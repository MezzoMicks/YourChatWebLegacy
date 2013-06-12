package de.yovi.chat.channel;

import de.yovi.chat.ChannelInfo;
import de.yovi.chat.api.User;
import de.yovi.chat.messaging.MediaSegment;

public class DefaultChannelInfo implements ChannelInfo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7428038028924932419L;
	private final String name;
	private final String bgColor;
	private final String fgColor;
	private final String bgImage;
	private final User[] users;
	private final MediaSegment[] media;
	private User[] otherUsers;
	
	public DefaultChannelInfo(String name, String bgColor, String fgColor, String bgImage, User[] users, MediaSegment[] media) {
		this.name = name;
		this.bgColor = bgColor;
		this.fgColor = fgColor;
		this.users = users;
		this.media = media;
		this.bgImage = bgImage != null ? "data/" + bgImage : null;
	}
	

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getBgColor() {
		return bgColor;
	}

	@Override
	public String getFgColor() {
		return fgColor;
	}
	
	@Override
	public String getBgImage() {
		return bgImage;
	}
	
	@Override
	public User[] getUsers() {
		return users;
	}
	
	@Override
	public MediaSegment[] getMedia() {
		return media;
	}
	
	@Override
	public User[] getOtherUsers() {
		return otherUsers;
	}

	public void setOtherUsers(User[] otherUsers) {
		this.otherUsers = otherUsers;
	}
	
}
