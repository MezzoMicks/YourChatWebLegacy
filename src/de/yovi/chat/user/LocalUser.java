package de.yovi.chat.user;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import de.yovi.chat.api.Message;
import de.yovi.chat.api.User;
import de.yovi.chat.channel.Room;
import de.yovi.chat.messaging.AbstractMessage;

public class LocalUser implements User {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1974470192942471638L;
	

	private final static int MAX_QUEUE_SIZE = 1000;

	private final AtomicLong messageIds = new AtomicLong(0l);
	
	private final String username;
	private final String sessionId;
	private final boolean guest;
	private final transient Queue<Message> queue = new ConcurrentLinkedQueue<Message>();
	private transient Room currentRoom = null;
	private String listenId = "";
	
	private boolean away = false;
	private boolean trusted = false;
	private transient long lastActivity = 0l;
	
	private String lastInvite = null;
	private String color = "000000";
	private String font = null;
	private String room = null;
	private Long avatarID = null;

	private String alias = null;


	private boolean asyncmode;
	
	public LocalUser(String username, String sessionId, boolean guest) {
		this.username = username;
		this.sessionId = sessionId;
		this.guest = guest;
		alive();
	}
	
	public Room getCurrentRoom() {
		return currentRoom;
	}
	
	public void setCurrentRoom(Room room) {
		this.currentRoom = room;
	}
	
	@Override
	public int compareTo(User o) {
		if (o == null || o.getUserName() == null) {
			return 1;
		} else if (username == null) {
			return -1;
		} else {
			return username.compareTo(o.getUserName());
		}
	}

	@Override
	public String getUserName() {
		return username;
	}

	@Override
	public String toString() {
		return username;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}
	
	public void setAway(boolean away) {
		this.away = away;
	}
	
	@Override
	public boolean isAway() {
		return away;
	}

	public void alive() {
		lastActivity = System.currentTimeMillis();
	}
	
	public long getLastActivity() {
		return lastActivity;
	}
	
	public void setListenerTime(long listenerTime) {
		listenId = username + new Long(listenerTime).hashCode();
	}
	
	@Override
	public String getListenId() {
		return listenId;
	}
	
	public void setColor(String color) {
		this.color = color;
	}

	@Override
	public String getColor() {
		return color;
	}

	@Override
	public boolean isGuest() {
		return guest;
	}
	
	public void setFont(String font) {
		this.font = font;
	}

	@Override
	public String getFont() {
		return font;
	}

	public void setFavouriteRoom(String room) {
		this.room = room;
	}
	
	public String getFavouriteRoom() {
		return room;
	}
	
	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}
	
	@Override
	public boolean isTrusted() {
		return trusted;
	}
	
	public void setAvatarID(Long avatarID) {
		this.avatarID = avatarID;
	}
	
	@Override
	public Long getAvatarID() {
		return avatarID;
	}
	
	public synchronized boolean push(Message message) {
		if (queue.size() >= MAX_QUEUE_SIZE) {
			return false;
		} else {
			return queue.offer(new UserMessage(message, messageIds.getAndIncrement()));
		}
	}
	
	public Message read() {
		return queue.poll();
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}
	@Override
	public boolean hasAsyncMode() {
		return asyncmode;
	}
	public void setAsyncmode(boolean asyncmode) {
		this.asyncmode = asyncmode;
	}
	
	public String getLastInvite() {
		return lastInvite;
	}

	public void setLastInvite(String lastInvite) {
		this.lastInvite = lastInvite;
	}

	/**
	 * Private Implementation of a Message which can be supplied with an id and may acts duplicate of the original message
	 * @author Michi
	 *
	 */
	private class UserMessage extends AbstractMessage {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3182373724141502691L;
		private final Message original;
		private final long id;
		
		public UserMessage(Message original, long id) {
			super(original.getSegments());
			this.original = original;
			this.id = id;
		}
		
		@Override
		public long getID() {
			return id;
		}

		@Override
		public User getOrigin() {
			return original.getOrigin();
		}
		
		@Override
		public int getCode() {
			return original.getCode();
		}
		
	}
	
}
