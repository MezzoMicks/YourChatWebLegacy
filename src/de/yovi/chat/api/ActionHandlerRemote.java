package de.yovi.chat.api;

import de.yovi.chat.ChannelInfo;


public interface ActionHandlerRemote {

	public Message[] listen(User user, String listenId);

	public ChannelInfo getChannelInfo(User user, String room);
	
	public User register(String name, String password, String key, String sugar);
	
	public void setSettings(User user, String color, String font, String room, boolean asyncmode);
	
	public User login(String name, String pwhash, String sugar, String initRoom);

	public void logout(User user);

	public User resetAndRejoin(User user);

	public String createInvitation(User user, boolean trial);
	
	public ChannelInfo[] getRoomList(User user);
	
	public Message[] getProtocol(User user, String room);

	public boolean isActive(User user);
	
}