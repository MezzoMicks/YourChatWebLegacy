package de.yovi.chat.system;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;

import de.yovi.chat.ChannelInfo;
import de.yovi.chat.api.ActionHandlerRemote;
import de.yovi.chat.api.Message;
import de.yovi.chat.api.Message.Preset;
import de.yovi.chat.api.User;
import de.yovi.chat.channel.ChannelProvider;
import de.yovi.chat.channel.DefaultChannelInfo;
import de.yovi.chat.channel.Room;
import de.yovi.chat.communication.InputHandler;
import de.yovi.chat.messaging.SystemMessage;
import de.yovi.chat.persistence.PersistenceManager;
import de.yovi.chat.user.LocalUser;
import de.yovi.chat.user.UserProvider;

public class ActionHandler implements ActionHandlerRemote {
	
	private static final Message[] NO_MESSAGES = new Message[0];
	private final static Logger logger = Logger.getLogger(ActionHandler.class);
	private final ChannelProvider cp = ChannelProvider.getInstance();
	private final UserProvider userProvider;
	
	public ActionHandler() {
//		EntityManagerFactory emf = Persistence.createEntityManagerFactory("YourChatWeb");
		EntityManagerFactory emf = PersistenceManager.getInstance().getFactory();
		EntityManager entityManager = emf.createEntityManager();
		userProvider = new UserProvider(entityManager);
	}
	
	@Override
	public boolean isActive(User user) {
		LocalUser luser = userProvider.getByUser(user);
		return luser != null;
	}
	
	@Override
	public void setSettings(User user, String color, String font, String room, boolean asyncmode) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			if (userProvider.update(luser, color, font, room, asyncmode)) {
				luser.push(new SystemMessage(null, 0, Preset.SETTINGS));
			}
		}
	}
	
	@Override
	public ChannelInfo[] getRoomList(User user) {
		ChannelInfo[] rooms;
		if (user != null) {
			List<ChannelInfo> roomList = cp.getOpenRooms();
			rooms = roomList.toArray(new ChannelInfo[roomList.size()]);
		} else {
			rooms = new ChannelInfo[0];
		}
		return rooms;
	}
	
	@Override
	public User resetAndRejoin(User user) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			luser.setListenerTime(System.currentTimeMillis());
			Room currentRoom = luser.getCurrentRoom();
			new InputHandler().localJoin(luser, currentRoom != null ? currentRoom.getName() : null);
		}
		return luser;
	}
	
	@Override
	public User register(String name, String password, String key, String sugar) {
		if (userProvider.register(name, password, key, sugar)) {
			return login(name, password, null, null);
		} else {
			return null;
		}
	}
	
	@Override
	public Message[] listen(User user, String listenId) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser == null) {
			return null;
		} else if (listenId != null) {
			List<Message> result = new LinkedList<Message>();
			if (!listenId.equals(luser.getListenId())) {
				SystemMessage message = new SystemMessage(null, 0l, Preset.DUPLICATESESSION);
				result.add(message);
			} else {		
				Message message;
				while ((message = luser.read()) != null) {
					result.add(message);
				}
			}
			return result.toArray(new Message[result.size()]);
		} else {
			return NO_MESSAGES;
		}
	}
	
	@Override
	public String createInvitation(User user, boolean trial) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null && !luser.isGuest() && luser.isTrusted()) {
			return userProvider.createInvitation(luser, trial);
		} else {
			return null;
		}
	}
	
	@Override
	public User login(String name, String pwhash, String sugar, String initRoom) {
		LocalUser luser = userProvider.login(name, pwhash, sugar);
		if (luser != null) {
			luser.push(new SystemMessage(null, 0l, Preset.WELCOME, luser.getUserName()));
			if (initRoom == null || initRoom.trim().isEmpty()) {
				initRoom = cp.getDefault().getName();
			}
			new InputHandler().localJoin(luser, initRoom);
		}
		return luser;
	}
	
	@Override
	public void logout(User user) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null)  {
			userProvider.logout(luser);
			userProvider.broadcast(new SystemMessage(null, 0l, Preset.REFRESH));
		}
	}
	@Override
	public ChannelInfo getChannelInfo(User user, String room) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			DefaultChannelInfo info = localGetChannelInfo(luser, room);
			List<User> otherUsers = new LinkedList<User>();
			String myRoomName = luser.getCurrentRoom().getName();
			for (LocalUser other : userProvider.getUsers()) {
				Room theirRoom = other.getCurrentRoom();
				if (theirRoom == null) {
					logger.warn("User " + other + " without room, skipping for listing!");
				} else {
					String theirRoomName = theirRoom.getName();
					if (!myRoomName.equalsIgnoreCase(theirRoomName)) {
						otherUsers.add(other);
					}
				}
			}
			info.setOtherUsers(otherUsers.toArray(new User[otherUsers.size()]));
			return info;
		} else {
			return null;
		}
	}
	
	public DefaultChannelInfo localGetChannelInfo(LocalUser user, String roomname) {
		Room room;
		if (roomname == null) {
			room = user.getCurrentRoom();
		} else {
			room = cp.getByName(roomname);
		}
		return room.getInfoForUser(user);
	}
	
	@Override
	public Message[] getProtocol(User user, String roomName) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			ChannelProvider cp = ChannelProvider.getInstance();
			Room room = cp.getByName(roomName);
			if (room != null && room.isMember(luser)) {
				List<Message> protocol = room.getProtocol();
				return protocol.toArray(new Message[protocol.size()]);
			}
		}
		return null;
	}
	
}
