package de.yovi.chat.api;

import java.io.Serializable;

public interface ProfileImage extends Serializable {

	public String getTitle();

	public String getDescription();
	
	public long getID();

}
