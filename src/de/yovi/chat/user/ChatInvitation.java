package de.yovi.chat.user;

/**
 * An invitation to the chat
 * @author Michi
 *
 */
public class ChatInvitation {

	private final long creation;
	private final boolean trial;
	private final LocalUser inviter;
	private final String key;
	private LocalUser invitee;

	public ChatInvitation(boolean trial, LocalUser inviter, String key) {
		this.creation = System.currentTimeMillis();
		this.trial = trial;
		this.inviter = inviter;
		this.key = key;
	}

	/**
	 * Returns the LocalUser-Object to this invitation
	 * @return {@link LocalUser} if the user is present in chat
	 */
	public LocalUser getInvitee() {
		return invitee;
	}

	/**
	 * When an invitee logs in, his LocalUser-Instance will be stored here
	 * @param invitee
	 */
	public void setInvitee(LocalUser invitee) {
		this.invitee = invitee;
	}

	/**
	 * The person who invoked the Invitation
	 * @return {@link LocalUser}
	 */
	public LocalUser getInviter() {
		return inviter;
	}

	/**
	 * The invitation-Key
	 * @return {@link String}
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * The time when this invitation was invoked
	 * @return {@link Long}
	 */
	public long getCreation() {
		return creation;
	}
	
	/**
	 * Whether or not this is just a trial-invitation
	 * <br> if <b>true</b> the person may no register using this invitation!
	 * @return {@link Boolean}
	 */
	public boolean isTrial() {
		return trial;
	}
	
	
}
