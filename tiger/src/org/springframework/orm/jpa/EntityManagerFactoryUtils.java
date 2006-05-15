/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class featuring methods for JPA EntityManager handling,
 * allowing for reuse of EntityManager instances within transactions.
 *
 * <p>Used by JpaTemplate, JpaInterceptor, and JpaTransactionManager.
 * Can also be used directly in application code, e.g. in combination
 * with JpaInterceptor.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see JpaTemplate
 * @see JpaInterceptor
 * @see JpaTransactionManager
 */
public abstract class EntityManagerFactoryUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up JPA
	 * EntityManagers. Return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100
	 * to execute EntityManager cleanup before JDBC Connection cleanup, if any.
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int ENTITY_MANAGER_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	private static final Log logger = LogFactory.getLog(EntityManagerFactoryUtils.class);


	/**
	 * Obtain a JPA EntityManager from the given factory. Is aware of a
	 * corresponding EntityManager bound to the current thread,
	 * for example when using JpaTransactionManager.
	 * <p>Note: Will return <code>null</code> if no thread-bound EntityManager found!
	 * @param emf EntityManagerFactory to create the EntityManager with
	 * @return the EntityManager, or <code>null</code> if none found
	 * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
	 * @see JpaTransactionManager
	 */
	public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf) throws DataAccessResourceFailureException {
		try {
			return doGetTransactionalEntityManager(emf);
		}
		catch (PersistenceException ex) {
			throw new DataAccessResourceFailureException("Could not obtain JPA EntityManager", ex);
		}
	}

	/**
	 * Obtain a JPA EntityManager from the given factory. Is aware of a
	 * corresponding EntityManager bound to the current thread,
	 * for example when using JpaTransactionManager.
	 * <p>Same as <code>getEntityManager</code>, but throwing the original PersistenceException.
	 * @param emf EntityManagerFactory to create the EntityManager with
	 * @return the EntityManager, or <code>null</code> if none found
	 * @throws javax.persistence.PersistenceException if the EntityManager couldn't be created
	 * @see #getTransactionalEntityManager(javax.persistence.EntityManagerFactory)
	 * @see JpaTransactionManager
	 */
	public static EntityManager doGetTransactionalEntityManager(EntityManagerFactory emf) throws PersistenceException {
		Assert.notNull(emf, "No EntityManagerFactory specified");

		EntityManagerHolder emHolder =
				(EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
		if (emHolder != null) {
			if (!emHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				// Try to explicitly synchronize the EntityManager itself
				// with an ongoing JTA transaction, if any.
				try {
					emHolder.getEntityManager().joinTransaction();
				}
				catch (TransactionRequiredException ex) {
					logger.debug("Could not join JTA transaction because none was active", ex);
				}
				emHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new EntityManagerSynchronization(emHolder, emf, false));
			}
			return emHolder.getEntityManager();
		}

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			// Indicate that we can't obtain a transactional EntityManager.
			return null;
		}

		// Create a new EntityManager with PersistenceContextType.EXTENDED,
		// as we are likely to execute outside of transactions as well
		// (for example, to enable lazy loading through OpenEntityManagerInView).
		logger.debug("Opening JPA EntityManager");
		EntityManager em = emf.createEntityManager();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for JPA EntityManager");
			// Use same EntityManager for further JPA actions within the transaction.
			// Thread object will get removed by synchronization at transaction completion.
			emHolder = new EntityManagerHolder(em);
			emHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(
					new EntityManagerSynchronization(emHolder, emf, true));
			TransactionSynchronizationManager.bindResource(emf, emHolder);
		}

		return em;
	}

	/**
	 * Convert the given PersistenceException to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy.
	 * <p>The most important cases like object not found or optimistic locking
	 * failure are covered here. For more fine-granular conversion, JpaAccessor and
	 * JpaTransactionManager support sophisticated translation of exceptions via a
	 * JpaDialect.
	 * @param ex PersistenceException that occured
	 * @return the corresponding DataAccessException instance
	 * @see JpaAccessor#convertJpaAccessException
	 * @see JpaTransactionManager#convertJpaAccessException
	 * @see JpaDialect#translateException
	 */
	public static DataAccessException convertJpaAccessException(PersistenceException ex) {
		if (ex instanceof EntityNotFoundException) {
			return new JpaObjectRetrievalFailureException((EntityNotFoundException) ex);
		}
		if (ex instanceof OptimisticLockException) {
			return new JpaOptimisticLockingFailureException((OptimisticLockException) ex);
		}
		if (ex instanceof EntityExistsException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof NoResultException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		// fallback
		return new JpaSystemException(ex);
	}


	/**
	 * Callback for resource cleanup at the end of a non-JPA transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class EntityManagerSynchronization extends TransactionSynchronizationAdapter {

		private final EntityManagerHolder entityManagerHolder;

		private final EntityManagerFactory entityManagerFactory;

		private final boolean newEntityManager;

		private boolean holderActive = true;

		public EntityManagerSynchronization(
				EntityManagerHolder emHolder, EntityManagerFactory emf, boolean newEntityManager) {
			this.entityManagerHolder = emHolder;
			this.entityManagerFactory = emf;
			this.newEntityManager = newEntityManager;
		}

		public int getOrder() {
			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER;
		}

		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
			}
		}

		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.entityManagerFactory, this.entityManagerHolder);
			}
		}

		public void beforeCompletion() {
			if (this.newEntityManager) {
				TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
				this.holderActive = false;
				this.entityManagerHolder.getEntityManager().close();
			}
		}

		public void afterCompletion(int status) {
			this.entityManagerHolder.setSynchronizedWithTransaction(false);
		}
	}

}
