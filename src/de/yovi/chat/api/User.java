package de.yovi.chat.api;

import java.io.Serializable;

public interface User extends Comparable<User>, Serializable {

	public String getUserName();
	
	public String getColor();
	
	public String getFavouriteRoom();
	
	public String getFont();
	
	public Long getAvatarID();
	
	public String getSessionId();
	
	public String getListenId();
	
	public boolean isAway();
	
	public boolean isGuest();
	
	public boolean isTrusted();
	
	public String getAlias();
	
	public boolean hasAsyncMode();
	
}
