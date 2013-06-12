package de.yovi.chat.channel;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import de.yovi.chat.api.Message;
import de.yovi.chat.api.Message.Preset;
import de.yovi.chat.api.Segment;
import de.yovi.chat.api.User;
import de.yovi.chat.communication.InputHandler;
import de.yovi.chat.messaging.MediaSegment;
import de.yovi.chat.messaging.StringSegment;
import de.yovi.chat.messaging.SystemMessage;
import de.yovi.chat.processing.api.ContentType;
import de.yovi.chat.user.LocalUser;
import de.yovi.chat.user.UserProvider;

public class Room implements Channel {

	private final static Logger logger = Logger.getLogger(Room.class);
	
	private final AtomicLong ids;
	private final TreeSet<LocalUser> users = new TreeSet<LocalUser>();
	private final Deque<MediaSegment> media = new LinkedList<MediaSegment>();
	private final Set<String> invitations = new HashSet<String>();
	private final Set<String> bans = new HashSet<String>();
	private final List<Message> protocol = new LinkedList<Message>();
	private final MediaSegment protokollSegment;
	private String bgImage = null;
	private boolean open = false;
	private boolean anonymous = false;
	protected final String name;
	private String bgColor = "FFFFFF";
	private String fgColor = "000000";
	private LocalUser owner;
	private Message motd = null;
	
	public Room(String name, boolean individual) {
		this.name = name;
		if (individual) {
			String escapedName;
			try {
				escapedName = URLEncoder.encode(name, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				escapedName = name;
				logger.error(e);
			}
			protokollSegment = new MediaSegment(name, "data?protocol=" + escapedName, ContentType.PROTOCOL, null, null);
			protokollSegment.setAlternateName("$PROTOCOL{room=" + name + "}");
		} else {
			protokollSegment = null;
		}
		ids = new AtomicLong();
	}
	
	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}
	
	public boolean setOpen(boolean open) {
		if (this.open != open) {
			if (owner != null) {
				if (open) {
					broadcast(new SystemMessage(owner, 0l, Preset.OPEN_CHANNEL, owner.getUserName()));
				} else {
					broadcast(new SystemMessage(owner, 0l, Preset.CLOSE_CHANNEL, owner.getUserName()));
				}
			}
			this.open = open;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Collection<LocalUser> getUsers() {
		return users;
	}
	
	@Override
	public boolean isVisible() {
		return open;
	}

	@Override
	public boolean isMember(LocalUser user) {
		if (user == null) {
			return false; 
		} else {
			return users.contains(user);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getColor() {
		return bgColor;
	}
	
	public void setBgImage(String filename) {
		this.bgImage = filename;
	}

	/**
	 * Sets this rooms color (Background)
	 * @param color
	 */
	public void setColor(String color) {
		this.bgColor = color;
	}
	

	/**
	 * Sets this rooms textcolor (Background)
	 * @param fontColor
	 */
	public void setFontColor(String fontColor) {
		this.fgColor = fontColor;
	}
	
	public void clearLog(String username) {
		if (this.protocol != null) {
			this.protocol.clear();
		}
	}
	
	public void clearMedia(String username) {
		if (this.media != null) {
			this.media.clear();
		}
	}

	@Override
	public void leave(LocalUser user) {
		users.remove(user);
		if (owner != null) {
			// if the owner left
			if (owner.equals(user)) {
				if (!users.isEmpty()) {
					// .. pass the ownership on
					owner = users.first();
				} else {
					// or leave it empty and remove the room!
					owner = null;
					ChannelProvider.getInstance().remove(this);
				}
			}
		}
		user.setCurrentRoom(null);
		broadcast(new SystemMessage(user, nextId(), Preset.LEFT_CHANNEL, user.getUserName()));
	}

	@Override
	public void join(LocalUser user) {
		Room oldRoom = user.getCurrentRoom();
		broadcast(new SystemMessage(user, nextId(), Preset.JOIN_CHANNEL, user.getUserName()));
		// if this isn't the users previous room
		if (oldRoom != this) {
			// leave it
			if (oldRoom != null) {
				oldRoom.leave(user);
			}
			// and set this as his room
			user.setCurrentRoom(this);
			users.add(user);
			if (motd != null) {
				user.push(motd);
			}
		}
	}

	@Override
	public boolean isAnonymous() {
		return anonymous;
	}

	public DefaultChannelInfo getInfoForUser(LocalUser user) {
		String name;
		String bgColor;
		String fgColor;
		String bgImage;
		User[] usersArray;
		MediaSegment[] mediaArray;
		boolean member = isMember(user);
		if (isVisible() || member) {
			if (member) {
				bgImage = this.bgImage;
			} else {
				bgImage = null;
			}
			name = this.name;
			bgColor = this.bgColor;
			fgColor = this.fgColor;
			if (!isAnonymous() || member) {
				usersArray = new User[users.size()];
				int i = 0;
				for (User tmp : getUsers()) {
					usersArray[i++] = tmp;
				}
			} else {
				usersArray = null;
			}
			if (member && protokollSegment != null) {
				mediaArray = new MediaSegment[media.size() + 1];
				int i = 0;
				mediaArray[i++] = protokollSegment;
				for (MediaSegment e : media) {
					mediaArray[i++] = e;
				}
			} else {
				mediaArray = null;
			}
		} else {
			name = null;
			bgColor = null;
			fgColor = null;
			bgImage = null;
			usersArray = null;
			mediaArray = null;
		}
		return new DefaultChannelInfo(name, bgColor, fgColor,bgImage, usersArray, mediaArray);
	}
	
	@Override
	public void talk(User user, Segment[] segments) {
		// look for nice stuff (like media)
		for (Segment segment : segments) {
			if (segment instanceof MediaSegment) {
				// and remember it!
				media.addFirst((MediaSegment) segment);
			}
		}
		// build a message consisting of the segments...
		SystemMessage sm = new SystemMessage(user, nextId(), segments);
		if (logger.isDebugEnabled()) {
			logger.debug(name + "> " + (user != null ? user.getUserName() : "null") + ": " + sm.toString());
		}
		// ..and tell everyone about it :)
		broadcast(sm);
	}
	
	public void shout(Message msg) {
		broadcast(new SystemMessage(nextId(), msg));
	}
	
	public void setOwner(LocalUser owner) {
		this.owner = owner;
	}
	
	@Override
	public LocalUser getOwner() {
		return owner;
	}
	
	@Override
	public boolean isInvited(LocalUser user) {
		return invitations.contains(user.getUserName());
	}
	
	@Override
	public boolean invite(LocalUser user) {
		user.setLastInvite(name);
		return invitations.add(user.getUserName());
	}
	
	@Override
	public boolean revokeInvitation(LocalUser user) {
		return invitations.remove(user.getUserName());
	}
	
	@Override
	public boolean ban(LocalUser user) {
		return bans.add(user.getUserName());
	}
	
	@Override 
	public boolean revokeBan(LocalUser user) {
		return bans.remove(user.getUserName());
	}
	
	public void setMotd(String username, String motd, InputStream uploadStream, String uploadName) {
		if (motd == null) {
			this.motd = null;
		} else {
			List<Segment> segments = InputHandler.parseSegments(username, motd, uploadStream, uploadName);
			segments.add(new StringSegment("\n"));
			SystemMessage motdMessage = new SystemMessage(null, 0, Preset.MOTD);
			motdMessage.append(segments.toArray(new Segment[segments.size()]));
			this.motd = motdMessage;
		}
	}
	
	@Override
	public List<Message> getProtocol() {
		if (protokollSegment != null) {
			return protocol;
		} else {
			return null;
		}
	}
	
	private void broadcast(Message msg) {
		for (LocalUser listener : getUsers()) {
			listener.push(msg);
		}
		if (protokollSegment != null) {
			protocol.add(msg);
		}
	}

	private long nextId() {
		return ids.getAndIncrement();
	}
}
