package de.yovi.chat.messaging;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;

import de.yovi.chat.api.PrivateMessage;
import de.yovi.chat.persistence.PrivateMessageEntity;
import de.yovi.chat.persistence.UserEntity;

public class PrivateMessageProvider {

	private final static Logger logger = Logger.getLogger(PrivateMessageProvider.class);
	private final EntityManager em;
	
	public PrivateMessageProvider(EntityManager em) {
		this.em = em;
	}
	
	public PrivateMessage sendMessage(UserEntity sender, UserEntity recipient, String subject, String body) {
		PrivateMessageEntity messageEntity = new PrivateMessageEntity();
		messageEntity.setRead(false);
		messageEntity.setSender(sender);
		messageEntity.setSenderName(sender.getName());
		messageEntity.setRecipient(recipient);
		messageEntity.setRecipientName(recipient.getName());
		messageEntity.setSubject(subject);
		messageEntity.setBody(body);
		messageEntity.setDate(new Timestamp(System.currentTimeMillis()));
		try {
			UserTransaction transaction = (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
			transaction.begin();
			em.joinTransaction();
			em.persist(messageEntity);
			transaction.commit();
		} catch (Exception e) {
			logger.error("Error while persisting PrivateMessage", e);
		}
		return new PrivateMessageWrapper(messageEntity, true);
	}
	

	/**
	 * deletes a message
	 * @param id
	 * @return {@link PrivateMessage}
	 */
	public boolean deleteMessage(long id, UserEntity user) {
		try {
			PrivateMessageEntity messageEntity = em.find(PrivateMessageEntity.class, id);
			if (messageEntity != null) {
				UserEntity sender = messageEntity.getSender();
				UserEntity recipient = messageEntity.getRecipient();
				if (sender != null && user.getId() == sender.getId()) {
					messageEntity.setSender(null);
					sender = null;
				} else if (recipient != null && user.getId() == recipient.getId()) {
					messageEntity.setRecipient(null);
					recipient = null;
				}
				UserTransaction transaction = (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
				transaction.begin();
				em.joinTransaction();
				// If nobody's interested in this message anymore...
				if (recipient == null && sender == null) {
					// .. delete it
					em.remove(messageEntity);
				} else {
					// .. otherwise save changes
					em.merge(messageEntity);
				}
				transaction.commit();
			}
			return true;
		} catch (Exception e) {
			logger.error("Error while persisting PrivateMessage", e);
			return false;
		}
	}

	public int[] countOutbox(UserEntity sender) {
		TypedQuery<Long> queryAll = em.createNamedQuery("countOutboxBySender", Long.class);
		queryAll.setParameter("user", sender);
		return new int[] {queryAll.getSingleResult().intValue(), 0};
	}
	
	public int[] countInbox(UserEntity recipient) {
		TypedQuery<Long> queryAll = em.createNamedQuery("countInboxByRecipient", Long.class);
		queryAll.setParameter("user", recipient);
		Long countAll = queryAll.getSingleResult();
		logger.debug("Inbox count: " + countAll);
		TypedQuery<Long> queryUnread = em.createNamedQuery("countInboxUnreadByRecipient", Long.class);
		queryUnread.setParameter("user", recipient);
		Long countUnread = queryUnread.getSingleResult();
		logger.debug("Inbox unread: " + countUnread);
		return new int[] {countAll.intValue(), countUnread.intValue()};
	}
	
	/**
	 * Retrieves all messages in the inbox
	 * @param id
	 * @return List of {@link PrivateMessage} (without bodies!)
	 */
	public List<PrivateMessage> getInbox(UserEntity recipient, int page, int limit) {
		List<PrivateMessage> result = new LinkedList<PrivateMessage>();
		try {
			TypedQuery<PrivateMessageEntity> query = em.createNamedQuery("findByRecipient", PrivateMessageEntity.class);
			query.setParameter("recipient", recipient);
			if (page >= 0 && limit >= 0) {
				query.setFirstResult(page * limit);
				query.setMaxResults(limit);
			}
			for (PrivateMessageEntity entity : query.getResultList()) {
				result.add(new PrivateMessageWrapper(entity, false));
			}
		} catch (Exception e) {
			logger.error("Error while fetching PrivateMessages in inbox for user " + recipient.getName(), e);
		}
		return result;
	}
	

	/**
	 * Retrieves all messages in the outbox
	 * @param id
	 * @return List of {@link PrivateMessage} (without bodies!)
	 */
	public List<PrivateMessage> getOutbox(UserEntity sender, int page, int limit) {
		List<PrivateMessage> result = new LinkedList<PrivateMessage>();
		try {
			TypedQuery<PrivateMessageEntity> query = em.createNamedQuery("findBySender", PrivateMessageEntity.class);
			query.setParameter("sender", sender);
			if (page >= 0 && limit >= 0) {
				query.setFirstResult(page * limit);
				query.setMaxResults(limit);
			}
			for (PrivateMessageEntity entity : query.getResultList()) {
				result.add(new PrivateMessageWrapper(entity, false));
			}
		} catch (Exception e) {
			logger.error("Error while fetching PrivateMessages in outbox for user " + sender.getName(), e);
		}
		return result;
	}
	
	/**
	 * Retrieves the full message, including the Body
	 * @param id
	 * @return {@link PrivateMessage}
	 */
	public PrivateMessage readMessage(UserEntity user, long id) {
		try {
			PrivateMessageEntity messageEntity = em.find(PrivateMessageEntity.class, id);
			logger.debug("Found Message " + messageEntity + " for user " + user);
			if (messageEntity != null && 
					(user.equals(messageEntity.getSender()) 
							||
							user.equals(messageEntity.getRecipient())))  {
				if (user.equals(messageEntity.getRecipient())) {
					messageEntity.setRead(true);
				}
				UserTransaction transaction = (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
				transaction.begin();
				em.joinTransaction();
				em.merge(messageEntity);
				transaction.commit();
				return new PrivateMessageWrapper(messageEntity, true);
			} else {
				logger.warn("Found no message, or user fetched foreign message!");
				return null;
			}
		} catch (Exception e) {
			logger.error("Error while persisting PrivateMessage", e);
			return null;
		}
	}
	
	/**
	 * Simplified representation of a {@link PrivateMessageEntity}
	 * @author michi
	 *
	 */
	private class PrivateMessageWrapper implements PrivateMessage {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1717301428444951153L;
		private final long id;
		private final String sender;
		private final String recipient;
		private final String subject;
		private final String body;
		private final Date date;
		private final boolean read;
		
		public PrivateMessageWrapper(PrivateMessageEntity entity, boolean withBody) {
			id = entity.getId();
			sender = entity.getSenderName();
			recipient = entity.getRecipientName();
			subject = entity.getSubject();
			if (withBody) {
				body = entity.getBody();
			} else {
				body = null;
			}
			date = entity.getDate();
			read = entity.isRead();
		}
		
		@Override
		public long getId() {
			return id;
		}
		
		@Override
		public String getSubject() {
			return subject;
		}

		@Override
		public String getBody() {
			return body;
		}

		@Override
		public String getSender() {
			return sender;
		}

		@Override
		public String getRecipient() {
			return recipient;
		}

		@Override
		public boolean isRead() {
			return read;
		}

		@Override
		public Date getDate() {
			return date;
		}
		
	}
	
}
