package de.yovi.chat.communication;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;

import de.yovi.chat.api.Message.Preset;
import de.yovi.chat.api.PrivateMessage;
import de.yovi.chat.api.User;
import de.yovi.chat.messaging.PrivateMessageProvider;
import de.yovi.chat.messaging.SystemMessage;
import de.yovi.chat.persistence.PersistenceManager;
import de.yovi.chat.persistence.UserEntity;
import de.yovi.chat.user.LocalUser;
import de.yovi.chat.user.UserProvider;

/**
 * Implementation for handling requests that interact with the PrivateMessaging-System
 * @author michi
 *
 */
public class PrivateMessageHandler implements PrivateMessageHandlerRemote {

	private final static Logger logger = Logger.getLogger(PrivateMessageHandler.class);

	private final UserProvider userProvider;
	private final PrivateMessageProvider pmProvider;
	
	public PrivateMessageHandler() {
//		EntityManagerFactory emf = Persistence.createEntityManagerFactory("YourChatWeb");
		EntityManagerFactory emf = PersistenceManager.getInstance().getFactory();
		EntityManager entityManager = emf.createEntityManager();
		userProvider = new UserProvider(entityManager);
		pmProvider = new PrivateMessageProvider(entityManager);
	}
	
	@Override
	public int[] countInbox(User user) {
		LocalUser luser = userProvider.getByUser(user);
		int[] result = null;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to get Outbox");
		} else {
			UserEntity recipientEntity = userProvider.getUserEntityByName(luser.getUserName());
			if (recipientEntity != null) {
				result = pmProvider.countInbox(recipientEntity);
				logger.debug("countInbox: " + result);
			}
		}
		return result;
	}
	
	@Override
	public int[] countOutbox(User user) {
		LocalUser luser = userProvider.getByUser(user);
		int[] result = null;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to get Outbox");
		} else {
			UserEntity senderEntity = userProvider.getUserEntityByName(luser.getUserName());
			if (senderEntity != null) {
				result = pmProvider.countOutbox(senderEntity);
			}
		}
		return result;
	}
	
	@Override
	public PrivateMessage[] readInbox(User user, int page, int limit) {
		LocalUser luser = userProvider.getByUser(user);
		List<PrivateMessage> result = null;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to get Outbox");
		} else {
			UserEntity recipientEntity = userProvider.getUserEntityByName(luser.getUserName());
			if (recipientEntity != null) {
				result = pmProvider.getInbox(recipientEntity, page, limit);
			}
		}
		return result.toArray(new PrivateMessage[result.size()]);
	}
	

	@Override
	public PrivateMessage[] readOutbox(User user, int page, int limit) {
		LocalUser luser = userProvider.getByUser(user);
		List<PrivateMessage> result = null;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to get Outbox");
		} else {
			UserEntity senderEntity = userProvider.getUserEntityByName(luser.getUserName());
			if (senderEntity != null) {
				result = pmProvider.getOutbox(senderEntity, page, limit);
			}
		}
		return result.toArray(new PrivateMessage[result.size()]);
	}
	
	@Override
	public PrivateMessage readMessage(User user, long messageId) {
		LocalUser luser = userProvider.getByUser(user);
		PrivateMessage result = null;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to read message  '" + messageId + "'");
		} else {
			UserEntity userEntity = userProvider.getUserEntityByName(user.getUserName());
			if (userEntity != null) {
				result = pmProvider.readMessage(userEntity, messageId);
			}
		}
		return result;
	}
	
	@Override
	public PrivateMessage sendMessage(User user, String recipient, String subject, String body) {
		LocalUser luser = userProvider.getByUser(user);
		PrivateMessage result = null;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to send message  '" + subject + "' to " + recipient);
		} else if (recipient == null || recipient.trim().isEmpty()) {
			logger.warn("User " + user + " tried to send message without recipient!");
		} else if (subject == null || subject.trim().isEmpty()) {
			logger.warn("User " + user + " tried to send message without subject!");
		} else if (body == null || body.trim().isEmpty()) {
			logger.warn("User " + user + " tried to send message without body!");
		} else {
			UserEntity senderEntity = userProvider.getUserEntityByName(luser.getUserName());
			UserEntity recipientEntity = userProvider.getUserEntityByName(recipient);
			if (senderEntity != null && recipientEntity != null) {
				result = pmProvider.sendMessage(senderEntity, recipientEntity, subject, body);
				if (result != null) {
					LocalUser rUser = userProvider.getByName(recipient);
					if (rUser != null) {
						logger.info("Notifying " + rUser + " about recieved message");
						rUser.push(new SystemMessage(luser, 0, Preset.MESSAGE, user.getUserName(), subject));
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public boolean deleteMessage(User user, long messageId) {
		LocalUser luser = userProvider.getByUser(user);
		boolean result = false;
		if (luser == null) {
			logger.warn("User without session! " + user);
		} else if (luser.isGuest()) {
			logger.info("Guest " + user + " tried to delete message  '" + messageId);
		} else {
			UserEntity userEntity = userProvider.getUserEntityByName(luser.getUserName());
			if (userEntity != null) {
				result = pmProvider.deleteMessage(messageId, userEntity);
			} else {
				logger.warn("Nonpersistent User " + user + " tried to delete message " + messageId);
			}
		}
		return result;
	}
	
}
