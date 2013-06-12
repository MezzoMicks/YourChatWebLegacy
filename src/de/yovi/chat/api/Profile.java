package de.yovi.chat.api;

import java.io.Serializable;
import java.util.Date;

public interface Profile extends Serializable {

	public String getName();
	
	public Date getLastLogin();
	
	public int getGender();
	
	public String getAbout();
	
	public Date getDateOfBirth();
	
	public String getLocation();
	
	public ProfileImage getImage();
	
	public ProfileImage[] getCollage();
	
	public FriendList[] getFriendLists();
	
}
