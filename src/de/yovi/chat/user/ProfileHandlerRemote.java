package de.yovi.chat.user;

import java.io.InputStream;
import java.util.Date;

import de.yovi.chat.api.Profile;
import de.yovi.chat.api.User;

public interface ProfileHandlerRemote {

	public boolean setAbout(User user, String about);
	
	public boolean setGender(User user, int gender);
	
	public Long addProfileImage(User user, InputStream upStream, String uploadname, String title, String description, boolean asAvatar);

	public boolean setLocation(User user, String location);

	public boolean setBirthday(User user, Date birthday);

	public Profile getProfile(User user, String profileUser);
	
	public boolean deleteImage(User user, long id);
	
}
