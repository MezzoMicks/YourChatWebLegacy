package de.yovi.chat.api;

import java.io.Serializable;
import java.util.Date;

/**
 * Representation of a user in it's role as a friend
 * @author michi
 *
 */
public interface Friend extends Serializable {

	/**
	 * The {@link Friend}'s username
	 * @return {@link String}
	 */
	public String getUserName();
	
	/**
	 * whether or not the {@link Friend} has confirmed the friendship
	 * @return boolean
	 */
	public boolean isConfirmed();
	
	/**
	 * whether or not the {@link Friend} is currently online
	 * @return boolean
	 */
	public boolean isOnline();
	
	/**
	 * The timestamp of the {@link Friend}'s last login
	 * @return {@link Date}
	 */
	public Date getLastLogin();
	
}
