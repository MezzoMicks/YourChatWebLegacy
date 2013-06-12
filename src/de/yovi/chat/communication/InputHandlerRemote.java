package de.yovi.chat.communication;

import java.io.File;
import java.io.InputStream;

import de.yovi.chat.api.User;

public interface InputHandlerRemote {

	/**
	 * Enum of commands which can be triggered from within normal messages
	 * @author Michi
	 *
	 */
	public enum MessageCommand {

		WHISPER("whisper", "w", "fluester", "f"),
		SEARCH("search", "suche"),
		JOIN("join", "j"),
		IGNORE("ignore", "ig"),
		INVITE("invite", "si"),
		NEWROOM("new", "sepnew", "sn"),
		OPENROOM("open", "sepopen"),
		CLOSEROOM("lock", "seplock"),
		AWAY("away"),
		BACKGROUND("background", "bg"),
		FOREGROUND("foreground", "fg"), 
		WHO("wer", "who", "werc", "whoc"),
		PROFILE("profile", "id", "about"),
		MOTD("motd"),
		CLEAR("clear"),
		ALIAS("alias", "aka"),
		LOGOUT("logout", "bye"),
		;
		
		private final String[] cmds;
		
		private MessageCommand(String... cmds) {
			this.cmds = cmds;
		}
		
		/**
		 * Retrieves a {@link MessageCommand} for the supplied String
		 * @param cmd
		 * @return {@link MessageCommand}
		 */
		public static MessageCommand getByCmd(String cmd) {
			for (MessageCommand value : values()) {
				for (String tmp : value.cmds) {
					if (tmp.equals(cmd)) {
						return value;
					}
				}
			}
			return null;
		}
		
	}
	
	
	/**
	 * Talks to the chat
	 * @param user
	 * @param message
	 * @param uploads
	 * @return
	 */
	public boolean talk(User user, String message, InputStream uploadStream, String uploadName);

	/**
	 * Whispers a message to a target
	 * @param user
	 * @param target
	 * @param message
	 * @param uploads
	 * @return
	 */
	public boolean whisper(User user, String target, String message, InputStream uploadStream, String uploadName);

	/**
	 * Joins a room
	 * @param user
	 * @param room
	 */
	public void join(User user, String room);

	/**
	 * Sets the user's status to 'away'
	 * @param user
	 * @param away
	 */
	public void setAway(User user, boolean away);
	
	/**
	 * 'simple' Searches for another User
	 * @param user
	 * @param wanted
	 */
	public void search(User user, String wanted);
	
	/**
	 * creates a new room
	 * @param user
	 * @param room
	 * @param color
	 */
	public void newRoom(User user, String room, String color);
	
	/**
	 * opens a room
	 * @param user
	 * @param room
	 */
	public void openRoom(User user, String room);
	
	/**
	 * locks a room
	 * @param user
	 * @param room
	 */
	public void closeRoom(User user, String room);
	

	/**
	 * Invite a user to a room
	 * @param user
	 * @param invitee
	 */
	public void invite(User user, String invitee);
	
}
