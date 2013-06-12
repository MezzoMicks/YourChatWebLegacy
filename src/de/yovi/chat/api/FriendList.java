package de.yovi.chat.api;

import java.io.Serializable;

public interface FriendList extends Serializable {

	/**
	 * The name of this FriendList
	 * @return
	 */
	public String getName();
	
	/**
	 * The lists visibility
	 * @return
	 */
	public boolean isVisible();
	
	/**
	 * The {@link Friend}s contained in this list
	 * @return
	 */
	public Friend[] getFriends();
	
}
