package de.yovi.chat.channel;

import java.util.Collection;
import java.util.List;

import de.yovi.chat.ChannelInfo;
import de.yovi.chat.api.Message;
import de.yovi.chat.api.Segment;
import de.yovi.chat.api.User;
import de.yovi.chat.user.LocalUser;

public interface Channel {

	public void join(LocalUser user);
	
	public void leave(LocalUser user);
	
	public Collection<LocalUser> getUsers();
	
	public String getName();
	
	public String getColor();
	
	public boolean isAnonymous();
	
	public boolean isVisible();
	
	public boolean isMember(LocalUser user);
	
	public LocalUser getOwner();

	public boolean invite(LocalUser user);

	public boolean revokeInvitation(LocalUser user);

	public boolean ban(LocalUser user);

	public boolean revokeBan(LocalUser user);

	public boolean isInvited(LocalUser user);

	public void talk(User user, Segment[] segments);
	
	public List<Message> getProtocol();
	
}
