package de.yovi.chat.persistence;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;

public class PersistenceManager {

	private final static Logger logger = Logger.getLogger(PersistenceManager.class);

	private final static PersistenceManager instance = new PersistenceManager();
	private volatile EntityManagerFactory emf = null;

	// = Persistence.createEntityManagerFactory("YourChatWeb")

	public static PersistenceManager getInstance() {
		return instance;
	}

	public EntityManagerFactory getFactory() {
		if (emf == null) {
			createFactory();
		}
		return emf;
	}

	public void closeEntityManagerFactory() {
		if (emf != null) {
			emf.close();
			emf = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Persistence finished");
			}
		}
	}

	public void persistOrMerge(EntityManager em, Object entity, boolean create) {
		UserTransaction transaction;
		try {
			transaction = createTransaction();
			transaction.begin();
			em.joinTransaction();
			logger.debug("persisting entity " + entity);
			if (create) {
				em.persist(entity);
			} else {
				em.merge(entity);
			}
			transaction.commit();
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	public void remove(EntityManager em, Object entity) {
		UserTransaction transaction;
		try {
			transaction = createTransaction();
			transaction.begin();
			em.joinTransaction();
			logger.debug("removing entity " + entity);
			em.remove(entity);
			transaction.commit();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private static UserTransaction createTransaction() throws NamingException {
		return (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
	}
	
	
	private synchronized void createFactory() {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("YourChatWeb");
		}
	}

}
