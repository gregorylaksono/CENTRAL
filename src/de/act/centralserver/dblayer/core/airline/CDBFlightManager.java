/**
 * 
 */
package de.act.centralserver.dblayer.core.airline;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.HibernateTemplate;

import de.act.central.types.CFlt;
import de.act.central.types.CentralBooking;
import de.act.central.types.CentralFlights;
import de.act.centralserver.dblayer.core.CDBSessionManager;
import de.act.centralserver.dblayer.interfaces.airline.IDBFlightManager;
import de.act.common.types.staticobjects.RSAc;
import de.act.common.types.staticobjects.RSFlt;

/**
 * @author Henry
 *
 */
@SuppressWarnings("unchecked")
public class CDBFlightManager implements IDBFlightManager{

	private static final Logger log = Logger.getLogger(CDBFlightManager.class);
	private HibernateTemplate ht;
	private CDBSessionManager dbSessionManager;
	
	public CDBFlightManager(){
		CDBFlightManager.log.info("starting CDBFlightManager");
	}

	public void setDbSessionManager(final CDBSessionManager dbSessionManager) {
		this.dbSessionManager = dbSessionManager;
		this.ht = dbSessionManager.getHibernateTemplate();
	}

	public CDBSessionManager getDbSessionManager() {
		return dbSessionManager;
	}
	
	public CentralFlights saveFlight(CentralFlights cFlt){
		CentralFlights cf = getFlight(cFlt);
		if(cf != null) return cf;
		Session session = ht.getSessionFactory().openSession();
		Transaction tr = session.beginTransaction();
		try {
			session.save(cFlt);
			tr.commit();
			return cFlt;
		} 
		catch (final DataAccessException e) {
			tr.rollback();
			return null;
		}
		catch (RuntimeException e) {
			tr.rollback();
			e.printStackTrace();
			return null;
		}
	}
	
	public CFlt saveFlight(CFlt cFlt){
		CFlt cf = getFlight(cFlt);
		if(cf != null) return cf;
		Session session = ht.getSessionFactory().openSession();
		Transaction tr = session.beginTransaction();
		try {
			session.save(cFlt);
			tr.commit();
			return cFlt;
		} 
		catch (final DataAccessException e) {
			tr.rollback();
			return null;
		}
		catch (RuntimeException e) {
			tr.rollback();
			e.printStackTrace();
			return null;
		}
	}
	
	private CentralFlights getFlight(CentralFlights cFlt){
		Session session = null;
		try{
			session = dbSessionManager.getSession();
			String hql = "From CentralFlights cf where cf.SFltId = ? AND cf.dep = ?";
//			Object[] args = new Object[]{ cFlt.getSFltId(), cFlt.getDep() };
//			List<CentralFlights> li = ht.find(hql, args);
			Query query = session.createQuery(hql);
			query.setLong(0, cFlt.getSFltId());
			query.setTimestamp(1, cFlt.getDep());
			List<CentralFlights> li = query.list();
			if(li != null && li.size() > 0) return li.iterator().next();
			else return null;
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			return null;
		}
		finally
		{
			if(session != null && session.isOpen())
				session.close();
		}
	}
	
	private CFlt getFlight(CFlt cFlt){
		try{
			String hql = "From CentralFlights cf where cf.SFltId = ? AND cf.dep = ?";
			Object[] args = new Object[]{ cFlt.getSFltId(), cFlt.getDep() };
			List<CFlt> li = ht.find(hql, args);
			if(li != null && li.size() > 0) return li.iterator().next();
			else return null;
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Integer saveBooking(CentralBooking cBkg){
		Session session = ht.getSessionFactory().openSession();
		Transaction tr = session.beginTransaction();
		try {
			session.save(cBkg);
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
	
	public Integer unactivatingCBkg(CentralBooking bkg){
		try {
			String hql = "update c_bkg set is_active = 0 WHERE bkg_id_id = '" + bkg.getBkgIdId() +"'";
			final JdbcTemplate jt = this.dbSessionManager.getJdbcTemplate();
			if(jt != null){
				jt.execute(hql);
			}
			return 0;
		} 
		catch (DataAccessException e) {
			e.printStackTrace();
			return 1;
		}
	}

	@Override
	public RSFlt getFligt(Long flt_id, Date flt_date) {
		try{
			String hql = "From RSFlt cf where cf.flt_id = ? AND cf.dep = ?";
			Object[] args = new Object[]{ flt_id, flt_date };
			List<RSFlt> li = ht.find(hql, args);
			if(li != null && li.size() > 0) return li.iterator().next();
			else return null;
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public RSAc getSac(Long ac_id) {
		try{
			String hql = "From RSAc cf where cf.ac_id = ? ";
			Object[] args = new Object[]{ ac_id };
			List<RSAc> li = ht.find(hql, args);
			if(li != null && li.size() > 0) return li.iterator().next();
			else return null;
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
