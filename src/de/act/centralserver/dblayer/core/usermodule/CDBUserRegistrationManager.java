/**
 * 
 */
package de.act.centralserver.dblayer.core.usermodule;


import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTemplate;

import de.act.centralserver.dblayer.core.CDBSessionManager;
import de.act.centralserver.dblayer.interfaces.usermodule.IDBUserRegistrationManager;
import de.act.common.types.staticobjects.RCAdd;

/**
 * @author Henry
 *
 */
public class CDBUserRegistrationManager implements IDBUserRegistrationManager{

	private static final Logger log = Logger.getLogger(CDBUserRegistrationManager.class);
	private HibernateTemplate ht;
	private CDBSessionManager dbSessionManager;

	public CDBUserRegistrationManager(){
		CDBUserRegistrationManager.log.info("starting CDBUserRegistrationManager");
	}

	public void setDbSessionManager(final CDBSessionManager dbSessionManager) {
		this.dbSessionManager = dbSessionManager;
		this.ht = dbSessionManager.getHibernateTemplate();
	}

	public CDBSessionManager getDbSessionManager() {
		return dbSessionManager;
	}

	public Integer saveAddress(RCAdd rca){
		Session session = ht.getSessionFactory().openSession();
		Transaction tr = session.beginTransaction();
		try {
			session.save(rca);
			tr.commit();
			return 0;
		} 
		catch (final DataAccessException e) {
			tr.rollback();
			return -1;
		}
		catch (RuntimeException e) {
			tr.rollback();
			e.printStackTrace();
			return -1;
		}
	}

	public Integer getNextAddressID() {
		int id = -1;
		String hql = "SELECT NEXTVAL ('c_add_seq');";
		final JdbcTemplate jt = this.dbSessionManager.getJdbcTemplate();
		if( jt != null ) {
			id = jt.queryForInt(hql);
		}
		return id;
	}
	
}
