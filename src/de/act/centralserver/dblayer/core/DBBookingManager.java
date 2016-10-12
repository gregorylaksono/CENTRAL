package de.act.centralserver.dblayer.core;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import de.act.central.types.BookingFlight;
import de.act.central.types.BookingFlightState;
import de.act.central.types.FormularInformation;
import de.act.centralserver.dblayer.interfaces.IDBBookingManager;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.staticobjects.CCommodityTree;
import de.act.common.types.staticobjects.RSAnn;
import de.act.common.types.staticobjects.RSFlt;
import de.act.common.types.staticobjects.RSFltConxCA;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 02.11.2006
 * @update 02.11.2009
 */
public class DBBookingManager extends HibernateDaoSupport implements IDBBookingManager {
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(DBBookingManager.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#storeFormularInformations(de.act.central.types.FormularInformation)
	 */
	public FormularInformation storeFormularInformations(FormularInformation dbf) {
		Session s = this.getSession();
		Transaction t = s.beginTransaction();
		try{
			this.getHibernateTemplate().merge(dbf);
			t.commit();
			return dbf;
		}
		catch(DataAccessException e){
			t.rollback();
			log.error(e.getMessage());
		}
		finally
		{
			if(s != null && s.isOpen())
				s.close();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#getFormularInformation(java.lang.String)
	 */
	public RFForm getFormularInformation(String id) {
		try{
			return (RFForm) this.getHibernateTemplate().load(RFForm.class, id);
		}
		catch(DataAccessException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#getCommodityAnnotationById(java.lang.String)
	 */
	public RSAnn getCommodityAnnotationById(Long comId) {
		try{
			CCommodityTree com = (CCommodityTree) this.getHibernateTemplate().load(CCommodityTree.class, comId);
			if (com != null)
				return com.getAnnotation();
			else return null;
		}
		catch(DataAccessException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#getFlightById(java.lang.String)
	 */
	public RSFltConxCA getFlightById(Long fltId) {
		try{
			RSFlt ret = (RSFlt) this.getHibernateTemplate().load(RSFlt.class, fltId);
			return new RSFltConxCA(ret);
		}
		catch(DataAccessException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#storeBookingData(java.util.Set, java.util.Set)
	 */
	@SuppressWarnings("unused")
	public void storeBookingData(Set<BookingFlight> flights, Set<BookingFlightState> flightStates) {
		Session s = this.getSession();
		Transaction t = s.beginTransaction();

		try{
			Set<BookingFlightState> save = new HashSet<BookingFlightState>();
			int i;
			// search objects on db, then store
			//		  for(BookingFlightState state:flightStates){
			//			 List<PackageItemBKG> dbStates = this.getHibernateTemplate().findByExample(state.getFBkgFlt().getFBkgItem());
			//			 if (dbStates != null && dbStates.size() > 0){
			//				PackageItemBKG dbState = dbStates.get(dbStates.size() - 1);
			//				state.getFBkgFlt().setFBkgItem(dbState);
			//				save.add(state);
			//			 }else{
			////				List<BookingFlight> flts = this.getHibernateTemplate().findByExample(state.getFBkgFlt());
			////				if (flts!=null && flts.size()>0) {
			////				    state.setFBkgFlt(flts.get(flts.size()-1));
			////				    state.setFAtt(flts.get(flts.size()-1).getFAtt());
			////				}
			////				save.add(state);
			//			 }
			//		  }
			for(BookingFlightState state:flightStates){
				//            Attachment att = state.getFAtt();
				//            this.getHibernateTemplate().saveOrUpdate(att);
				//            BookingFlight flt = state.getFBkgFlt();
				//            this.getHibernateTemplate().saveOrUpdate(flt);
				s.saveOrUpdate(state);
			}
			t.commit();
		}

		catch(DataAccessException e){
			t.rollback();
			log.error(e.getMessage());
		}
		finally
		{
			if(s != null && s.isOpen())
				s.close();
		}
	}
}
