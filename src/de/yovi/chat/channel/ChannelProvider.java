package de.yovi.chat.channel;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.yovi.chat.ChannelInfo;
import de.yovi.chat.web.Configurator;

public class ChannelProvider {

	private final static Logger logger = Logger.getLogger(Configurator.class);
	
	private static volatile ChannelProvider instance;
	
	private final TreeMap<String, Room> mains = new TreeMap<String, Room>();
	private final TreeMap<String, Room> subs = new TreeMap<String, Room>();
	
	private final Room defaultRoom;

	private ChannelProvider() {
		Room defaultRoom = null;
		logger.info("Setting up Channels");
		for (String channel : Configurator.getChannels()) {
			// hidden
			String name = channel;
			String color;
			int ixOfPipe = name.indexOf('|');
			if (ixOfPipe >= 0) {
				color = name.substring(ixOfPipe + 1);
				name = name.substring(0, ixOfPipe);
			} else {
				color = "FFFFFF";
			}
			logger.info("Room : " + name + " (" + color + ")");
			Room room = new Room(name, false);
			room.setColor(color);
			room.setOpen(true);
			room.setAnonymous(false);
			mains.put(name.toLowerCase(), room);
			if (defaultRoom == null) {
				defaultRoom = room;
			}
			String motd4room = Configurator.getMessageOfTheDay(name);
			if (motd4room != null) {
				room.setMotd(null, motd4room, null, null);
			}
		}
		this.defaultRoom = defaultRoom;
	}
	
	public static ChannelProvider getInstance() {
		if (instance == null) {
			createInstance();
		}
		return instance;
	}
	
	public synchronized static void createInstance() {
		if (instance == null) {
			instance = new ChannelProvider();
		}
	}
	
	public Room spawn(String name) {
		String lcName = name.toLowerCase();
		if (!mains.containsKey(lcName) && !subs.containsKey(lcName)) {
			Room result = new Room(name, true);
			subs.put(lcName, result);
			return result;
		} else {
			return null;
		}
	}
	
	public void remove(Room room) {
		String lcName = room.getName().toLowerCase();
		subs.remove(lcName);
	}
	
	public Room getByName(String name) {
		if (name != null) {
			String lcRoomName = name.trim().toLowerCase();
			Room result = mains.get(lcRoomName);
			if (result == null) {
				result = subs.get(lcRoomName);
			}
			return result;
		} else {
			return null;
		}
	}

	public boolean isMainRoom(String name) {
		if (name != null) {
			String lcRoomName = name.trim().toLowerCase();
			return mains.containsKey(lcRoomName);
		} else {
			return false;
		}
	}
	
	public Room getDefault() {
		return defaultRoom;
	}
	
	public List<ChannelInfo> getOpenRooms() {
		List<ChannelInfo> result = new LinkedList<ChannelInfo>();
		for (Room main : mains.values()) {
			result.add(main.getInfoForUser(null));
		}
		for (Room sub : subs.values()) {
			if (sub.isVisible()) {
				result.add(sub.getInfoForUser(null));
			}
		}
		return result;
	}
	
}
