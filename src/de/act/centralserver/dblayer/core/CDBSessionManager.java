/**
 * @file CDBSessionManager.java
 * @package de.act.batch.com.dblayer.core
 * @since 1.0 09:09:11
 * @author Martin Sachs
 */
package de.act.centralserver.dblayer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * CDBSessionManager.java:
 *
 * @since 1.0
 * @author Martin Sachs
 */
public class CDBSessionManager {
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		hibernateTemplate.flush();
		hibernateTemplate.clear();        
	}

	private JdbcTemplate      jdbcTemplate      = null;
	private HibernateTemplate hibernateTemplate = null;
	private SessionFactory    sessionFactory    = null;
	private Log               myLog;

	public CDBSessionManager() {
		this.myLog = LogFactory.getLog(CDBSessionManager.class);
		this.myLog.info("starting CDBSessionManager");
	}

	/**
	 *
	 */
	public void setSessionFactory(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		// TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(session));
	}

	/**
	 * @return Returns the hibernateTemplate.
	 */
	public HibernateTemplate getHibernateTemplate() {
		return this.hibernateTemplate;
	}

	/**
	 * @param hibernateTemplate
	 *            The hibernateTemplate to set.
	 */
	public void setHibernateTemplate(final HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
		this.hibernateTemplate.setCacheQueries(true);
	}

	/**
	 * @return Returns the jdbcTemplate.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * @param jdbcTemplate
	 *            The jdbcTemplate to set.
	 */
	public void setJdbcTemplate(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * @return Returns the sessionFactory.
	 */
	@SuppressWarnings("unused")
	private SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 23.06.2006
	 * @return Returns the session.
	 */
	public Session getSession() {
		return SessionFactoryUtils.getSession(this.sessionFactory, true);
		//        return session;
	}
}
