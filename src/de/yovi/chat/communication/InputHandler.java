package de.yovi.chat.communication;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.activation.MimetypesFileTypeMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.api.Message;
import de.yovi.chat.api.Message.Preset;
import de.yovi.chat.api.Segment;
import de.yovi.chat.api.User;
import de.yovi.chat.channel.Channel;
import de.yovi.chat.channel.ChannelProvider;
import de.yovi.chat.channel.Room;
import de.yovi.chat.messaging.MediaSegment;
import de.yovi.chat.messaging.StringSegment;
import de.yovi.chat.messaging.SystemMessage;
import de.yovi.chat.persistence.PersistenceManager;
import de.yovi.chat.persistence.UserEntity;
import de.yovi.chat.processing.ImageThumbGenerator;
import de.yovi.chat.processing.VideoThumbGenerator;
import de.yovi.chat.processing.api.ContentType;
import de.yovi.chat.processing.api.ProcessorPlugin;
import de.yovi.chat.processing.api.ProcessorResult;
import de.yovi.chat.processing.api.ProcessorResult.ThumbGenerator;
import de.yovi.chat.processing.plugins.ImageProcessorPlugin;
import de.yovi.chat.processing.plugins.VideoProcessorPlugin;
import de.yovi.chat.processing.plugins.WebsiteProcessorPlugin;
import de.yovi.chat.processing.plugins.YoutubeProcessorPlugin;
import de.yovi.chat.system.FileStorage;
import de.yovi.chat.user.LocalUser;
import de.yovi.chat.user.UserProvider;
import de.yovi.chat.web.Configurator;

public class InputHandler implements InputHandlerRemote {

	private final static Logger logger = Logger.getLogger(InputHandler.class);

	private final static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
	private final static TreeMap<Integer, ProcessorPlugin> processorMap = new TreeMap<Integer, ProcessorPlugin>();
	
	static {
		mimeTypesMap.addMimeTypes("video/3gp 3gp 3GP");
		ImageProcessorPlugin imageProcessorPlugin = new ImageProcessorPlugin();
		processorMap.put(1, imageProcessorPlugin);
		processorMap.put(101, new YoutubeProcessorPlugin());
		int i = 102;
		for (String pluginClass : Configurator.getExtraPlugins()) {
			ProcessorPlugin plugin = null;
			try {
				
				Class<?> loadedClass = imageProcessorPlugin.getClass().getClassLoader().loadClass(pluginClass);
				Constructor<?>[] constructors = loadedClass.getConstructors();
				for (Constructor<?> constructor : constructors) {
					plugin = (ProcessorPlugin) constructor.newInstance();
					break;
				}
				logger.info("Adding Plugin: " + plugin.getClass().getName());
				processorMap.put(i++, plugin);
			} catch (Exception e) {
				logger.error("Error, while loading Plugins", e);
			}
		}
		processorMap.put(Integer.MAX_VALUE - 1, new VideoProcessorPlugin());
		processorMap.put(Integer.MAX_VALUE, new WebsiteProcessorPlugin());
	}

	private final ChannelProvider cp = ChannelProvider.getInstance();
	private final UserProvider userProvider;
	private final SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");
	private final SimpleDateFormat sdf_time =  new SimpleDateFormat("HH:mm");
	

	
	public InputHandler() {
//		EntityManagerFactory emf = Persistence.createEntityManagerFactory("YourChatWeb");
		EntityManagerFactory emf = PersistenceManager.getInstance().getFactory();
		EntityManager entityManager = emf.createEntityManager();
		userProvider = new UserProvider(entityManager);
	}
	
	@Override
	public boolean talk(User user, String message, InputStream uploadStream, String uploadname) {
		message = message != null ? message.trim() : "";
		if (!message.isEmpty()) {
			LocalUser luser = userProvider.getByUser(user);
			if (luser != null) {
				Channel channel = luser.getCurrentRoom();
				luser.alive();
				if (channel != null) {
					// first let the backend evaluate the message and stuff
					BackendResult beResult = process(luser, message, uploadStream, uploadname);
					// if there's something left for us to do
					if (!beResult.isProcessed()) {
						if (user.isAway()) {
							// Set to 'not away' if someone talks to the public
							setAway(user, false);
						}
						// do stuff!
						channel.talk(user, beResult.getSegments());
					}
					// It's done, in any case!
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public void join(User user, String room) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			localJoin(luser, room);
		}
	}


	public void localJoin(LocalUser luser, String roomName) {
		if (roomName == null) {
			roomName = luser.getFavouriteRoom();
		}
		Room room;
		if (roomName == null) {
			room = cp.getByName(luser.getLastInvite());
		} else {
			room = cp.getByName(roomName);
		}
		if (room != null) {
			logger.debug(luser + " tries to join room " + room.getName());
			if (room.isVisible() || room.isInvited(luser) || room.getOwner().equals(luser)) {
				room.join(luser);
				luser.alive();
				luser.push(new SystemMessage(null, 0l, Preset.SWITCH_CHANNEL, room.getName(), room.getColor()));
				userProvider.broadcast(new SystemMessage(null, 0l, Preset.REFRESH));
			} else {
				luser.push(new SystemMessage(null, 0l, Preset.CHANNEL_NOTALLOWED, room.getName()));
			}
		} else {
			luser.push(new SystemMessage(null, 0l, Preset.UNKNOWN_CHANNEL, roomName));
		}
	}
	
	@Override
	public void setAway(User user, boolean away) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null && !luser.isGuest()) {
			luser.setAway(away);
			luser.alive();
			Room room = luser.getCurrentRoom();
			if (room != null) {
				Message msg;
				if (luser.isAway()) {
					msg  = new SystemMessage(user, 0, Preset.USER_AWAY, user.getUserName());
				} else {
					msg  = new SystemMessage(user, 0, Preset.USER_BACK, user.getUserName());
				}
				room.shout(msg);
				userProvider.broadcast(new SystemMessage(null, 0l, Preset.REFRESH));
			}
		}
	}

	@Override
	public boolean whisper(User user, String target, String message, InputStream uploadStream, String uploadName) {
		LocalUser luser = userProvider.getByUser(user);
		LocalUser targetUser = userProvider.getByName(target);
		luser.alive();
		if (targetUser == null) {
			logger.warn("WHISPER with unknown user '" + target + "' supplied!");
			luser.push(new SystemMessage(null, 0l, Preset.UNKNOWN_USER, target));
		} else {
			// Perpare the SystemMessage
			SystemMessage msg4Target = new SystemMessage(luser, 0l, Preset.WHISPER, luser.getUserName());
			SystemMessage msg4Source = new SystemMessage(null, 0l, Preset.WHISPERTO, targetUser.getUserName());
			// parse the actual content
			List<Segment> msgSegments = parseSegments(luser.getUserName(), message, uploadStream, uploadName);
			// and append the Segments to the Message
			Segment[] extraSegments = new Segment[msgSegments.size()]; 
			msg4Target.append(msgSegments.toArray(extraSegments));
			msg4Source.append(msgSegments.toArray(extraSegments));
			targetUser.push(msg4Target);
			luser.push(msg4Source);
			if (logger.isDebugEnabled()) {
				logger.debug("Message[" + message + "] from user[" + luser + "] to user[" + targetUser + "]");
			}
		}
		return false;
	}
	
	@Override
	public void search(User user, String wanted) {
		LocalUser luser = userProvider.getByUser(user);
		luser.alive();
		if (luser != null) {
			LocalUser wantedUser = userProvider.getByName(wanted);
			if (wantedUser == null) {
				UserEntity wantedEntity = userProvider.getUserEntityByName(wanted);
				if (wantedEntity != null) {
					String date = sdf_date.format(wantedEntity.getLastlogin());
					String time = sdf_time.format(wantedEntity.getLastlogin());
					luser.push(new SystemMessage(null, 0l, Preset.SEARCH_LAST, wantedEntity.getName(), date, time));
				} else {
	 				logger.info("SEARCH with unknown user '" + wanted + "' supplied!");
					luser.push(new SystemMessage(null, 0l, Preset.UNKNOWN_USER, wanted));
				}
			} else {
				Channel channel = wantedUser.getCurrentRoom();
				if (channel == null) {
					luser.push(new SystemMessage(null, 0l, Preset.SEARCH_NOWHERE, wantedUser.getUserName()));
				} else if (!channel.isVisible()) {
					luser.push(new SystemMessage(null, 0l, Preset.SEARCH_PRIVATE, wantedUser.getUserName()));
				} else {
					luser.push(new SystemMessage(null, 0l, Preset.SEARCH_CHANNEL, wantedUser.getUserName(), channel.getName()));
				}
			}
		}
	}

	@Override
	public void newRoom(User user, String roomName, String color) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			ChannelProvider cp = ChannelProvider.getInstance();
			logger.debug(user + " wants to create " + roomName + " with color " + color);
			if (luser.isGuest()) {
				luser.push(new SystemMessage(null, 0l, Preset.CREATE_NOGUEST));
			} else {
				Room room = cp.spawn(roomName);
				if (room == null) {
					logger.debug(user + " wanted to create " + roomName + " which name is already in use");
					room = cp.getByName(roomName);
					luser.push(new SystemMessage(null, 0l, Preset.CREATE_NAMEGIVEN, room.getName()));
				} else {
					logger.info(user + " created " + roomName + " with color " + color);
					if (color != null) {
						room.setColor(color);
					}
					room.setOwner(luser);
					localJoin(luser, roomName);
				}
			}
		}
	}
	
	@Override
	public void invite(User user, String invitee) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			logger.debug(user + " invites " + invitee);
			if (luser.isGuest()) {
				luser.push(new SystemMessage(null, 0l, Preset.INVITE_NOGUEST));
			} else {
				LocalUser inviteeUser = userProvider.getByName(invitee);
				if (inviteeUser == null) {
					luser.push(new SystemMessage(null, 0l, Preset.UNKNOWN_USER, invitee));
				} else {
					Channel channel = luser.getCurrentRoom();
					if (!channel.invite(inviteeUser)) {
						luser.push(new SystemMessage(null, 0l, Preset.INVITETO_USER_ALREADY, inviteeUser.getUserName()));
					} else {
						inviteeUser.push(new SystemMessage(luser, 0l, Preset.INVITE_USER, luser.getUserName(), channel.getName()));
						luser.push(new SystemMessage(null, 0l, Preset.INVITETO_USER, inviteeUser.getUserName()));
					}
				}
			}
		}
	}
	
	@Override
	public void closeRoom(User user, String roomName) {
		LocalUser luser = userProvider.getByUser(user);
		logger.debug(user + " closes room " + roomName);
		Room room = roomName == null ? luser.getCurrentRoom() : cp.getByName(roomName);
		if (room == null) {
			luser.push(new SystemMessage(null, 0l, Preset.UNKNOWN_CHANNEL, roomName));
		} else if (room.getOwner().equals(luser)) {
			if (!room.setOpen(false)) {
				luser.push(new SystemMessage(null, 0l, Preset.CLOSE_CHANNEL_ALREADY));
			}
		}
	}
	
	@Override
	public void openRoom(User user, String roomName) {
		LocalUser luser = userProvider.getByUser(user);
		logger.debug(user + " opens room " + roomName);
		Room room = roomName == null ? luser.getCurrentRoom() : cp.getByName(roomName);
		if (room == null) {
			logger.debug("no room found");
			luser.push(new SystemMessage(null, 0l, Preset.UNKNOWN_CHANNEL, roomName));
		} else {
			logger.debug("checking owner");
			if (luser.equals(room.getOwner())) {
				if (!room.setOpen(true)) {
					luser.push(new SystemMessage(null, 0l, Preset.OPEN_CHANNEL_ALREADY));
				}	
			}
		}
	}
	
	


	/**
	 * Processes a user's message and the appended file-uploads
	 * @param user
	 * @param message
	 * @param upload
	 * @return {@link BackendResult}
	 */
	public BackendResult process(LocalUser user, String message, InputStream uploadStream, String uploadName) {
		boolean processed = false;
		// no system-message-injections!
		if (message.startsWith("$")) {
			message = "\\" + message;
		// commands start with "/"
		} else if (message.startsWith("/")) {
			int whitespace = message.indexOf(' ');
			String cmdprefix;
			if (whitespace > 0) {
				cmdprefix = message.substring(1, whitespace);
			} else {
				cmdprefix = message.substring(1);
			}
			// Now is it a command?
			MessageCommand cmd = MessageCommand.getByCmd(cmdprefix);
			// if not
			if (cmd == null) {
				// ... tell someone about it
				logger.info("Unknown command[" + cmdprefix + "] from user[" + user + "]");
				user.push(new SystemMessage(null, 0l, Preset.UNKNOWN_COMMAND, cmdprefix));
			} else {
				// ... else if yes... 
				// get the commands payload
				String payload = null;
				if (whitespace > 0) {
					payload = message.substring(whitespace);
				}
				logger.info("Command[" + cmdprefix + "] from user[" + user + "]");
				// and process it
				processCommand(user, cmd, payload, uploadStream, uploadName);
			}
			processed = true;
		}
		Segment[] segmentsArray = null;
		// now is there stuff left to do?
		if (!processed) {
			// then it's about to parse the message for it's actual content
			List<Segment> segments = parseSegments(user.getUserName(), message, uploadStream, uploadName);
			segmentsArray = segments.toArray(new Segment[segments.size()]);
		}
		return new BackendResult(processed, segmentsArray);
	}
	
	/**
	 * Parses and splits an input into it's segments
	 * @param input
	 * @return
	 */
	public static List<Segment> parseSegments(String username, String input, InputStream uploadStream, String uploadName) {
		LinkedList<Segment> result = new LinkedList<Segment>();
		// Split over every whitespacey character
		String [] parts = input.split("\\s");
        // Attempt to convert each item into an URL.   
        for( String item : parts ) {
        	try {
        		// if it's an url
	            URL url = new URL(item);
	            // process the media-stuff 'out of it'
	            Segment segment = processLink(username, url);
	            if (segment != null) {
	            	result.add(segment);
	            }
	        } catch (MalformedURLException e) {
	            // If there was something that wasn't an URL....
	        	// if the previous segment was already a String
	        	Segment previous = result.isEmpty() ? null : result.getLast();
				if (previous != null && previous instanceof StringSegment) {
					//.. append
	        		((StringSegment) previous).append(item);
	        	} else {
	        		//.. create a new Segment
	        		result.add(new StringSegment(item));
	        	}
	        } catch (Exception e) {
	        	logger.error("Error while parsing message \"" + input + "\" on segment \"" + item + "\"", e);
			}
        }
        // now take care of the upload!
        Segment uploadSegment = processUpload(username, uploadStream, uploadName);
		if (uploadSegment != null) {
			result.add(uploadSegment);
		}
		return result;
	}
	
	private static Segment processUpload(String username, InputStream uploadStream, String uploadname) {
		MediaSegment result = null;
		if (uploadStream != null) {
			// We only do images!
			try {
				// First copy the Stream
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				IOUtils.copy(uploadStream, bos);
				// Load retrieved data into new ByteInputStream
				ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
				String filename = FileStorage.getInstance().store(bis, uploadname);
				bis.reset();
				String contentType = mimeTypesMap.getContentType(uploadname);
				logger.info("Upload from " + username + " stored to " + filename + "[" + contentType +"]" );
				ContentType type;
				String preview = null;
				String pinky = null;
				if (contentType.startsWith("video")) {
					type = ContentType.VIDEO;
					// Create Thumbnail
					Map<ImageSize, String> map = new VideoThumbGenerator(uploadname, bis).generate(ImageSize.PREVIEW, ImageSize.PINKY);
					preview = map.get(ImageSize.PREVIEW);
					pinky = map.get(ImageSize.PINKY);
				} else {
					type = ContentType.IMAGE;
					// Create Thumbnail
					Map<ImageSize, String> map = new ImageThumbGenerator(uploadname, bis).generate(ImageSize.PREVIEW, ImageSize.PINKY);
					preview = map.get(ImageSize.PREVIEW);
					pinky = map.get(ImageSize.PINKY);
				}
				bis.close();
				result = new MediaSegment(username, "data/" + filename, type, "data/" + preview, "data/" + pinky);
				result.setAlternateName(uploadname);
				uploadStream.close();
			} catch (IOException e) {
				logger.error("Couldn't buffer upload " + uploadname, e);
			}
		}
		return result;
	}
	
	private static Segment processLink(String username, URL url) {
		MediaSegment result = null;
		URLConnection connection;
		try {
			connection = url.openConnection();
			connection.setConnectTimeout(2000);
			ProcessorResult tmpResult = null;
			String title = null;
			String preview = null;
			String pinky = null;
			ContentType type = ContentType.UNKNOWN;
			logger.debug("protocol :" + url.getProtocol());
			for (Integer key : processorMap.keySet()) {
				tmpResult = processorMap.get(key).process(connection); 
				if (tmpResult != null) {
					type = tmpResult.getType();
					title = tmpResult.getTitle();
					ThumbGenerator generator = tmpResult.getThumbGenerator();
					if (generator != null) {
						Map<ImageSize, String> generated = generator.generate(ImageSize.PREVIEW, ImageSize.PINKY);
						preview = generated.get(ImageSize.PREVIEW);
						pinky = generated.get(ImageSize.PINKY);
					}
					break;
				}
			}
			logger.debug("URL of type " + type + " : " + url);
			result = new MediaSegment(username, url.toString(), type, preview, pinky);
			result.setAlternateName(title);
		} catch (IOException e) {
			logger.error("Error while processing posted Link", e);
		}
         return result;
	}
	

	private boolean processCommand(LocalUser user, MessageCommand cmd, String payload, InputStream uploadStream, String uploadName) {
		Room userRoom = null;
		switch (cmd) {
		case ALIAS:
			String alias = payload != null ? payload.trim() : "";
			userRoom = user.getCurrentRoom();
			if (alias.isEmpty()) {
				user.setAlias(null);
				userRoom.shout(new SystemMessage(null, 0l, Preset.USER_ALIAS_CLEARED, user.getUserName()));
			} else {
				user.setAlias(alias);
				userRoom.shout(new SystemMessage(null, 0l, Preset.USER_ALIAS_SET, user.getUserName(), alias));
			}
			return true;
		case WHO:
			String whoRoomString = payload != null ? payload.trim() : "";
			Room whoRoom;
			if (!whoRoomString.isEmpty()) {
				whoRoom	= ChannelProvider.getInstance().getByName(whoRoomString);
				if (whoRoom == null) {
					user.push(new SystemMessage(null, 0, Preset.UNKNOWN_CHANNEL, whoRoomString));
				}
			} else {
				whoRoom = user.getCurrentRoom();
			}
			if (whoRoom != null) {
				if (whoRoom.isMember(user) || (whoRoom.isVisible() && !whoRoom.isAnonymous())) {
					StringBuffer output = new StringBuffer();
					output.append('*');
					output.append(whoRoom.getName());
					output.append(": ");
					boolean first = true;
					for (User roomUser : whoRoom.getUsers()) {
						if (!first) {
							output.append(", ");
						} else {
							first = false;
						}
						output.append(roomUser);
					}
					user.push(new SystemMessage(null, 0, new StringSegment(output.toString())));
				} else {
					user.push(new SystemMessage(null, 0, Preset.CHANNEL_PRIVATE, whoRoom.getName()));
				}
			}
			return true;
		case FOREGROUND:
			userRoom = user.getCurrentRoom();
			if (user.isTrusted() || user.equals(userRoom.getOwner())) {
				userRoom.setFontColor(payload != null ? payload.trim() : "000000");
				userRoom.shout(new SystemMessage(user, 0, Preset.CHANNEL_FG_CHANGED));
			}
			return true;
		case BACKGROUND:
			userRoom = user.getCurrentRoom();
			// if the user is this rooms owner
			if (user.isTrusted() || user.equals(userRoom.getOwner())) {
				// parse color and operations out of the payload
				String newColor = null;
				boolean resize = false;
				boolean clear = false;
				if (payload != null) {
					for (String load : payload.split(" ")) {
						String trimmedLoad = load.trim();
						if (trimmedLoad.equalsIgnoreCase("resize")) {
							resize = true;
						} else if (trimmedLoad.equalsIgnoreCase("clear")) {
							clear = true;
						} else {
							if (!trimmedLoad.isEmpty()) {
								newColor = trimmedLoad;
							}
						}
					}
				}
				if (newColor != null) {
					userRoom.setColor(newColor);
				}
				if (uploadStream != null) {
					try {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						Color backdrop = Color.WHITE;

						if (newColor != null) {
							if (newColor.length() == 3) {
								char[] bytes = newColor.toCharArray();
								newColor = new String(new char[] { bytes[0], bytes[0], bytes[1], bytes[1], bytes[2], bytes[2]});
							}
							int hex = Integer.parseInt(newColor, 16);
							int r = (hex & 0xFF0000) >> 16;
							int g = (hex & 0xFF00) >> 8;
							int b = (hex & 0xFF);
							logger.debug("parsed big hex to " + r + " " + g + " " + b);
							backdrop = new Color(r, g, b);
						}
						ChatUtils.makeImageMoreTransparent(uploadStream, bos, backdrop);
						ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
						String bgImage = "data/" + ChatUtils.createAndStoreResized("bg_", bis, uploadName, 1240, (resize ? 1024 : -1), backdrop);
						userRoom.setBgImage(bgImage);
					} catch (Exception e) {
						logger.error("Error, processing background for user " + user + " file: " + uploadName, e);
						userRoom.setBgImage(null);
					}
				} else if (clear) {
					userRoom.setBgImage(null);
				}
				
				userRoom.shout(new SystemMessage(user, 0, Preset.CHANNEL_BG_CHANGED));
			}
			return true;
		case AWAY:
			setAway(user, true);
			return true;
		case LOGOUT:
			userProvider.logout(user);
			return true;
		case CLOSEROOM:
			closeRoom(user, payload);
			return true;
		case OPENROOM:
			openRoom(user, payload);
			return true;
		case INVITE:
			if (payload == null || (payload = payload.trim()).isEmpty()) {
				logger.warn("INVITE with no user supplied!");
			} else {
				invite(user, payload);
			}
			return true;
		case NEWROOM:
			if (payload == null || (payload = payload.trim()).isEmpty()) {
				logger.warn("NEWROOM with no room supplied!");
			} else {
				String color;
				String room;
				int whitespace = payload.indexOf(' ');
				if (whitespace != -1) {
					room = payload.substring(0, whitespace).trim();
					color = payload.substring(whitespace).trim();
				} else {
					color = null;
					room = payload;
				}
				newRoom(user, room, color);
			}
			return true;
		case SEARCH:
			if (payload == null || (payload = payload.trim()).isEmpty()) {
				logger.warn("SEARCH with no user supplied!");
			} else {
				search(user, payload);
			}
			return true;
		case IGNORE:
			// TODO
			return true;
		case JOIN:
			localJoin(user, payload != null ? payload.trim() : null);
			return true;
		case WHISPER:
			int whitespace;
			if (payload == null) {
				whitespace = -1;
			} else {
				payload = payload.trim();
				whitespace = payload.indexOf(' ');
			}
			if (whitespace == -1) {
				logger.warn("WHISPER with no message supplied!");
			} else {
				String targetUsername = payload.substring(0, whitespace).trim();
				String message = payload.substring(whitespace).trim();
				whisper(user, targetUsername, message, uploadStream, uploadName);
			}
			return true;
		case PROFILE:
			String profileUser = payload != null ? payload.trim() : "";
			UserEntity targetUser = userProvider.getUserEntityByName(profileUser);
			if (targetUser == null) {
				logger.warn("PROFILE with unknown user '" + profileUser + "' supplied!");
				user.push(new SystemMessage(null, 0l, Preset.UNKNOWN_USER, profileUser));
			} else {
				user.push(new SystemMessage(null, 0l, Preset.PROFILE_OPEN, targetUser.getName()));
			}
			return true;
		case CLEAR:
			userRoom = user.getCurrentRoom();
			if (user.isTrusted() || user.equals(userRoom.getOwner())) {
				String target = payload != null ? payload.trim() : "";
				if (target.equals("all") || target.equals("log")) {
					userRoom.clearLog(user.getUserName());
					logger.debug(user + " clears Log for Room '" + userRoom + "'");
					userRoom.shout(new SystemMessage(null, 0l, Preset.CLEAR_LOG, user.getUserName()));
				}
				if (target.equals("all") || target.equals("media")) {
					userRoom.clearMedia(user.getUserName());
					userRoom.shout(new SystemMessage(null, 0l, Preset.CLEAR_MEDIA, user.getUserName()));
					logger.debug(user + " clears Media for Room '" + userRoom + "'");
				}
			} else {
				logger.warn("MotD from unprivileged user '" + user + "'");
				user.push(new SystemMessage(null, 0l, Preset.MOTD_NOTALLOWED));
			}
			return true;
		case MOTD:
			userRoom = user.getCurrentRoom();
			if (user.isTrusted() || user.equals(userRoom.getOwner())) {
				String motd = payload != null ? payload.trim() : "";
				if (logger.isDebugEnabled()) {
					logger.debug(user + " sets motd for Room '" + userRoom + "' to '" + motd + "'");
				}
				if (!motd.isEmpty()) {
					userRoom.setMotd(user.getUserName(), motd, uploadStream, uploadName);
				} else {
					userRoom.setMotd(user.getUserName(), null, null, null);
				}
				userRoom.shout(new SystemMessage(null, 0l, Preset.MOTD_SET, user.getUserName()));
			} else {
				logger.warn("MotD from unprivileged user '" + user + "'");
				user.push(new SystemMessage(null, 0l, Preset.MOTD_NOTALLOWED));
			}
			return true;
		default:
			return false;
		}
	}
	
	private static class BackendResult {
		
		private boolean processed = false;
		private Segment[] segments = null;
		public BackendResult(boolean processed, Segment[] segments) {
			super();
			this.processed = processed;
			this.segments = segments;
		}
		
		public Segment[] getSegments() {
			return segments;
		}
		
		public boolean isProcessed() {
			return processed;
		}
		
	}
	
	public static void main(String[] args) {
//		new Configurator().debug();
//		for (Segment seg : parseSegments(null, "test http://blogs.taz.de foo ")) {
//			System.out.println(seg.getContent());
//		}
//		for (Segment seg : parseSegments(null, "test http://blogs.taz.de/arabesken/files/2012/05/testbild-sendepause.jpg foo ")) {
//			System.out.println(seg.getContent());
//		}
//
//		for (Segment seg : parseSegments(null, "Foo  <Klammäär aüf>& http://blogs.taz.de/arabesken/files/2012/05/testbild-sendepause.jpg foo ")) {
//			System.out.println(seg.getContent());
//		}
		String colorCode = "ff0";
		if (colorCode.length() == 3) {
			char[] bytes = colorCode.toCharArray();
			colorCode = new String(new char[] { bytes[0], bytes[0], bytes[1], bytes[1], bytes[2], bytes[2]});
		}
		int hex = Integer.parseInt(colorCode, 16);
		int r = (hex & 0xFF0000) >> 16;
		int g = (hex & 0xFF00) >> 8;
		int b = (hex & 0xFF);
		logger.debug("parsed big hex to " + r + " " + g + " " + b);
	}
}
