package de.yovi.chat;

import java.io.Serializable;

import de.yovi.chat.api.User;
import de.yovi.chat.messaging.MediaSegment;

/**
 * Information about a channel
 * @author Michi
 *
 */
public interface ChannelInfo extends Serializable {

	/**
	 * Returns this channels name
	 * @return {@link String}
	 */
	public String getName();
	
	/**
	 * Returns this channels color in webformat
	 * @return Hex-String like #FFCCCC
	 */
	public String getBgColor();
	
	/**
	 * Returns this channels text color in webformat
	 * @return Hex-String like #FFCCCC
	 */
	public String getFgColor();
	
	public String getBgImage();
	
	/**
	 * Returns the members of this room
	 * @return Array of {@link String} representing the users
	 */
	public User[] getUsers();
	
	/**
	 * Returns the List of MediaSegments, that occured in this room
	 * @return Array of {@link MediaSegment}
	 */
	public MediaSegment[] getMedia();
	
	public User[] getOtherUsers();
	
}
