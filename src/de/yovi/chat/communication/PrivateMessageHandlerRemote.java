package de.yovi.chat.communication;

import de.yovi.chat.api.PrivateMessage;
import de.yovi.chat.api.User;

/**
 * Interface for invocations on the PrivateMessaging-System
 * @author michi
 *
 */
public interface PrivateMessageHandlerRemote {

	/**
	 * Sends a Message to a user
	 * @param user (sender, invoker)
	 * @param recipient
	 * @param subject
	 * @param body
	 * @return {@link PrivateMessage} the result
	 */
	public PrivateMessage sendMessage(User user, String recipient, String subject, String body);
	
	/**
	 * Reads a user's outbox
	 * @param user
	 * @param page
	 * @param limit
	 * @return Array of {@link PrivateMessage} without body
	 */
	public PrivateMessage[] readOutbox(User user, int page, int limit);
	
	/**
	 * Reads a user's inbox
	 * @param user
	 * @param page
	 * @param limit
	 * @return Array of {@link PrivateMessage} without body
	 */
	public PrivateMessage[] readInbox(User user, int page, int limit);
	
	/**
	 * Counts the messages inside a user's outbox
	 * @param user
	 * @return array of int 0 = max 1 = unread
	 */
	public int[] countOutbox(User user);

	/**
	 * Counts the messages inside a user's inbox
	 * @param user
	 * @return array of int 0 = max 1 = unread
	 */
	public int[] countInbox(User user);
	
	/**
	 * explicitly reads a message, including the Body, marking it as read
	 * @param user
	 * @param messageId
	 * @return {@link PrivateMessage}
	 */
	public PrivateMessage readMessage(User user, long messageId);
	
	/**
	 * Deletes a Message from the users box
	 * @param user
	 * @param messageId
	 * @return success
	 */
	public boolean deleteMessage(User user, long messageId);
	
}
