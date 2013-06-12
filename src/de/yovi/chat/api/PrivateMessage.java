package de.yovi.chat.api;

import java.io.Serializable;
import java.util.Date;

/**
 * Representation of a private message, send between users
 * @author Michi
 *
 */
public interface PrivateMessage extends Serializable {

	/**
	 * Unique messageId
	 * @return {@link String}
	 */
	public long getId();
	
	/**
	 * The subject or title of the message
	 * @return {@link String}
	 */
	public String getSubject();
	
	/**
	 * The body or content of the message
	 * @return {@link String}
	 */
	public String getBody();
	
	/**
	 * Username of the person who send this message
	 * @return {@link String}
	 */
	public String getSender();
	
	/**
	 * Username of the person who recieved the message
	 * @return {@link String}
	 */
	public String getRecipient();
	
	/**
	 * Whether or not a message was read by the recipient
	 * @return {@link String}
	 */
	public boolean isRead();
	
	/**
	 * The Date, when the message was sent
	 * @return {@link Date}
	 */
	public Date getDate();
	
}
