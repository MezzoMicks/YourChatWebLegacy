package de.yovi.chat.user;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;

import de.yovi.chat.api.Message;
import de.yovi.chat.api.Message.Preset;
import de.yovi.chat.api.User;
import de.yovi.chat.channel.ChannelProvider;
import de.yovi.chat.channel.Room;
import de.yovi.chat.messaging.SystemMessage;
import de.yovi.chat.persistence.ImageEntity;
import de.yovi.chat.persistence.PersistenceManager;
import de.yovi.chat.persistence.ProfileEntity;
import de.yovi.chat.persistence.UserEntity;
import de.yovi.chat.system.ActionHandler;
import de.yovi.chat.system.PasswordUtil;
import de.yovi.chat.web.Configurator;

public class UserProvider {

	private static final long MINUTE = 1000 * 60;
	private final static Logger logger = Logger.getLogger(UserProvider.class);
	private final static long A_DAY_IN_MILLIS = MINUTE * 60 * 24;

	private final static Map<String, LocalUser> sessions2users = new HashMap<String, LocalUser>();
	private final static Map<String, LocalUser> names2users = new TreeMap<String, LocalUser>();
	private final static Map<String, ChatInvitation> invitations = new HashMap<String, ChatInvitation>();

	private final static long TIMEOUT = MINUTE * 60;
	private final static long AWAY_TIMEOUT = 6 * TIMEOUT;

	private final static Thread timeout = new Thread() {
		public void run() {
			logger.info("Timeout-Thread is running");
			while (true) {
				try {
					Thread.sleep(MINUTE);
					long timeoutAgo = System.currentTimeMillis() - TIMEOUT;
					long awayTimeoutAgo = System.currentTimeMillis() - AWAY_TIMEOUT;
					ActionHandler actionHandler = null;
					Set<String> userNames = new HashSet<String>(names2users.keySet());
					for (String key : userNames) {
						LocalUser user = names2users.get(key);
						if (user != null) {
							boolean timeout = false;
							if (!user.isAway() && user.getLastActivity() < timeoutAgo) {
								timeout = true;
							} else if (user.isAway() && user.getLastActivity() < awayTimeoutAgo) {
								timeout = true;
							}
							if (timeout) {
								logger.info("Timeout for User " + user);
								user.push(new SystemMessage(null, 0l, Preset.TIMEOUT));
								Thread.sleep(5000); // Timing-Problem, want to make sure the Listenerthread will fetch the Message
								if (actionHandler == null) {
									actionHandler = new ActionHandler();
								}
								actionHandler.logout(user);
							}
						} else {
							names2users.remove(key);
						}
					}
					long dayAgo = System.currentTimeMillis() - A_DAY_IN_MILLIS;

					Set<String> inviKeys = new HashSet<String>(invitations.keySet());
					for (String key : inviKeys) {
						ChatInvitation invitation = invitations.get(key);
						if (invitation != null) {
							if (invitation.getCreation() <= dayAgo) {
								invitations.remove(key);
								logger.info("Clearing obsolete invitation");
							}
						} else {
							invitations.remove(key);
						}
					}
				} catch (InterruptedException ex) {
					logger.error("Error in Timeout-Thread: ", ex);
				}
			}
			
		};
		
	};
	
	static {
		timeout.start();
	}
	
	private final AtomicLong ids = new AtomicLong(System.currentTimeMillis());

	private final EntityManager em;

	public UserProvider(EntityManager em) {
		this.em = em;
	}

	public LocalUser login(String username, String pwHash, String sugar) {
		logger.info("User " + username + " tries to login");
		LocalUser result = validatePassword(username, pwHash, sugar);
		if (result == null) {
			logger.info("User[" + username + "] tried to login, invalid Username or Password supplied!");
		}
		return result;
	}
	

	public void broadcast(Message msg) {
		for (LocalUser user : getUsers()) {
			user.push(msg);
		}
	}

	/**
	 * Registers a user permanently at the system
	 * 
	 * @param username
	 * @param password
	 * @param invitationKey
	 *            (needed if invitation is required)
	 * @param sugar
	 *            (needed if invitationKey is hashed!)
	 * @return boolean
	 */
	public boolean register(String username, String password, String invitationKey, String sugar) {
		boolean ok = false;
		username = username != null ? username.trim() : null;
		if (username.length() < 4) {
			logger.info(username + "'s registration revoked, username : '"	+ (username) + "' must be at least 4 chars");
		} else if (getUserEntityByName(username) == null) {
			if (Configurator.isInvitationRequired()) {
				ChatInvitation invitation = getInvitation(username, invitationKey, sugar, false);
				if (invitation != null && !invitation.isTrial()) {
					ok = true;
				} else {
					logger.info(username + "'s registration revoked : " + (invitation == null ? "not invited" : "trial-invitation"));
				}
			} else {
				// no invitation required? no problem :)
				ok = true;
			}
		} else {
			logger.warn("Registration cancelled, username '" + username
					+ "' already given");
		}
		if (ok) {
			logger.info("Registering User " + username);
			UserEntity newUser = new UserEntity();
			newUser.setName(username);
			newUser.setPassword(password);

			logger.debug("Persisting!");
			try {
				UserTransaction transaction = (UserTransaction) new InitialContext()
						.lookup("java:comp/UserTransaction");
				transaction.begin();
				em.joinTransaction();
				em.persist(newUser);
				transaction.commit();
			} catch (Exception ex) {
				logger.error("Error while persisting UserEntity", ex);
			}
			return true;
		} else {
			return false;
		}
	}

	public void logout(LocalUser user) {
		sessions2users.remove(user.getSessionId());
		String lcUserName = user.getUserName().trim().toLowerCase();
		names2users.remove(lcUserName);
		Room room = user.getCurrentRoom();
		if (room != null) {
			room.leave(user);
		}
	}

	public LocalUser getByUser(User user) {
		if (user != null && user.getSessionId() != null) {
			return sessions2users.get(user.getSessionId());
		} else {
			return null;
		}
	}

	public LocalUser getByName(String name) {
		if (name != null) {
			String lcUserName = name.trim().toLowerCase();
			return names2users.get(lcUserName);
		} else {
			return null;
		}
	}

	public LocalUser getByPermanentByName(String name) {
		if (name != null) {
			String lcUserName = name.trim().toLowerCase();
			return names2users.get(lcUserName);
		} else {
			return null;
		}
	}
	
	public Collection<LocalUser> getUsers() {
		return names2users.values();
	}

	private String getSessionID() {
		String sessionId = Long.toHexString(ids.getAndIncrement());
		return sessionId;
	}

	private LocalUser validatePassword(String username, String pwhash, String sugar) {
		LocalUser result = null;
		UserEntity userEntity = getUserEntityByName(username);
		// Is it a persisted user?
		if (userEntity != null) {
			// then let's hash and check the password
			String tmpHash = userEntity.getPassword();
			if (sugar != null) {
				tmpHash = PasswordUtil.encrypt(userEntity.getPassword(), sugar);
			}
			if (tmpHash.equals(pwhash)) {
				// maybe the user is already logged in
				result = getByName(username);
				if (result == null) {

					// not logged in... then let's create the local user
					username = userEntity.getName();
					result = createLocalUser(username, false);
					result.setColor(userEntity.getColor());
					result.setFont(userEntity.getFont());
					result.setFavouriteRoom(userEntity.getRoom());
					result.setTrusted(userEntity.isTrusted());
					result.setAsyncmode(userEntity.isAsyncmode());
					ProfileEntity profile = userEntity.getProfile();
					ImageEntity avatar = profile != null ? profile.getAvatar() : null;
					result.setAvatarID(avatar != null ? avatar.getId() : null);
					userEntity.setLastlogin(new Timestamp(System.currentTimeMillis()));
					logger.info("User[" + username + "] is now logged in");
					try {
						UserTransaction transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
						transaction.begin();
						em.joinTransaction();
						em.merge(userEntity);
						transaction.commit();
					} catch (Exception ex) {
						logger.error("Error while persisting UserEntity", ex);
					}
					// // and persist their login time
				} else {
					// update the listener time and 'alive' them
					result.setListenerTime(System.currentTimeMillis());
					result.alive();
					logger.info("User[" + username + "]'s is now 'alived' due to login-attempt");
				}
			}
		} else {
			logger.info("The user doesn't seem to be a persistent user, let's check the invitations");
			ChatInvitation invitation = getInvitation(username, pwhash, sugar, true);
			if (invitation != null) {
				// maybe the user is already logged in
				result = invitation.getInvitee();
				if (result == null) {
					if ((System.currentTimeMillis() - invitation.getCreation()) < A_DAY_IN_MILLIS) {
						logger.info("Creating temporary User-Object for invitee " + username);
						result = createLocalUser(username, true);
						invitation.setInvitee(result);
					} else {
						invitations.remove(invitation.getKey());
						logger.info("Invitation with key " + invitation.getKey() + " from  " + invitation.getInviter().getUserName() + " expired!" + " Invitee: " + username + " won't get in!");
					}
				} else {
					logger.info(username + " reused their invitation");
					// update the listener time and 'alive' them
					result.setListenerTime(System.currentTimeMillis());
					result.alive();
					logger.info("User[" + username + "]'s is now 'alived' due to login-attempt");
				}
			}
		}
		return result;
	}

	public UserEntity getUserEntityByName(String username) {
		// First we query the db for that user
		logger.debug("Fetching UserEntity by name " + username);
		TypedQuery<UserEntity> query = em.createNamedQuery("getUserByName", UserEntity.class);
		query.setParameter("name", username.toLowerCase());
		UserEntity userEntity = null;
		try {
			userEntity = query.getSingleResult();
			logger.debug("Found " + userEntity.getId() + " " + userEntity.getName());
		} catch (NoResultException nrex) {
			logger.debug("User " + username + " not found in Database");
		}
		return userEntity;
	}

	public boolean update(LocalUser luser, String color, String font, String room, boolean asyncmode) {
		boolean result = false;
		UserEntity userEntity = getUserEntityByName(luser.getUserName());
		if (userEntity != null) {
			try {
				if (isFontOk(font)) {
					userEntity.setFont(font);
					luser.setFont(font);
				}
				if (isColorOk(color)) {
					userEntity.setColor(color);
					luser.setColor(color);
				}
				if (isRoomOk(room)) {
					userEntity.setRoom(room);
					luser.setFavouriteRoom(room);
				}
				userEntity.setAsyncmode(asyncmode);
				luser.setAsyncmode(asyncmode);
				PersistenceManager.getInstance().persistOrMerge(em, userEntity, true);
				result = true;
			} catch (Exception e) {
				logger.error("Error, while updating " + luser, e);
			}
		} else {
			if (isFontOk(font)) {
				luser.setFont(font);
			}
			if (isColorOk(color)) {
				luser.setColor(color);
			}
			luser.setAsyncmode(asyncmode);
		}
		return result;

	}
	
	public String createInvitation(LocalUser user, boolean trial) {
		String key =  PasswordUtil.createKey();
		invitations.put(key, new ChatInvitation(trial, user, key));
		return key;
	}

	private LocalUser createLocalUser(String username, boolean guest) {
		LocalUser result;
		String sessionId = getSessionID();
		result = new LocalUser(username, sessionId, guest);
		sessions2users.put(sessionId, result);
		names2users.put(username.toLowerCase(), result);
		result.setListenerTime(System.currentTimeMillis());
		return result;
	}

	private ChatInvitation getInvitation(String username, String keyHash, String sugar, boolean asLogin) {
		ChatInvitation invitation = null;
		for (String tmpKey : invitations.keySet()) {
			String tmpHash;
			if (asLogin) {
				tmpHash = PasswordUtil.encrypt(tmpKey, null);
			} else {
				tmpHash = tmpKey;
			}
			tmpHash = PasswordUtil.encrypt(tmpHash, sugar);
			logger.debug("[" + tmpKey +"]" + tmpHash + " versus " + keyHash);
			if (tmpHash.equals(keyHash)) {
				invitation = invitations.get(tmpKey);
				logger.info("Invitation found for key " + tmpKey);
				if (invitation.getInvitee() != null && !username.equals(invitation.getInvitee().getUserName())) {
					logger.warn("Someone (" + username + ") tried to reuse " + invitation.getInvitee().getUserName() + "'s invitation!");
				} else {
					invitation = invitations.get(tmpKey);
				}
			}
		}
		return invitation;
	}

	private static boolean isRoomOk(String room) {
		if (room == null) {
			return true;
		} else {
			return ChannelProvider.getInstance().isMainRoom(room);
		}
	}

	private static boolean isFontOk(String font) {
		if (font == null) {
			return true;
		} else {
			for (char ch : font.toCharArray()) {
				if (!Character.isLetterOrDigit(ch)
						&& !Character.isWhitespace(ch)) {
					return false;
				}
			}
			return true;
		}
	}

	private static boolean isColorOk(String color) {
		if (color == null) {
			return true;
		} else {
			if (color.length() <= 8) {
				for (char ch : color.toCharArray()) {
					if (!Character.isLetterOrDigit(ch)) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}
	}
	
}
