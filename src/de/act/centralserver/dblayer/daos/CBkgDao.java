package de.act.centralserver.dblayer.daos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import de.act.centralserver.dblayer.interfaces.IBkgDao;
import de.act.common.system.Lock;
import de.act.common.system.LockSet;
import de.act.common.types.CentralBookingResult;
import de.act.common.types.WgtVol;
import de.act.common.types.nonstaticobjects.RCBkg;
import de.act.common.types.nonstaticobjects.RCFlt;
import de.act.common.types.views.CFltBkg;

@SuppressWarnings("unchecked")
public class CBkgDao extends HibernateDaoSupport implements IBkgDao {

	/** Our logger. */
	private final static Log LOG = LogFactory.getLog(IBkgDao.class.getName());

	private final static String ACT_BKG_ID = "ACT:";

	static class LockSetKey {
		final Long sFltId;
		final Date   date;
		final Long   cFltId;
		final Long   fltGroup;
		final int    hashCode;

		LockSetKey(Long sFltId, Date date, IBkgDao bkgDao) {
			this.sFltId   = sFltId;
			this.date     = date;

			RCFlt cFlt    = bkgDao.getCFlt(sFltId, date);

			this.cFltId   = cFlt.getId();
			this.fltGroup = cFlt.getFltGroup();

			this.hashCode = (this.fltGroup == null ? this.cFltId.hashCode() : this.fltGroup.hashCode());
		}

		public int hashCode() {
			return this.hashCode;
		}

		public boolean equals(Object o) {
			if (o instanceof LockSetKey) {
				LockSetKey other = (LockSetKey) o;
				return this.hashCode == other.hashCode &&
				(this.fltGroup == null ? this.cFltId.equals(other.cFltId) :
					this.fltGroup.equals(other.fltGroup));
			}

			return false;
		}
	} // class LockSetKey

	static class ConxFlt {
		private Long fltId;
		private String conxId;
		private Long sFltId;
		private Date   date;

		public String getConxId() {
			return conxId;
		}
		public void setConxId(String conxId) {
			this.conxId = conxId;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		public Long getFltId() {
			return fltId;
		}
		public void setFltId(Long fltId) {
			this.fltId = fltId;
		}
		public Long getsFltId() {
			return sFltId;
		}
		public void setsFltId(Long fltId) {
			sFltId = fltId;
		}
	}

	/** Used for locking in makeBooking(). */
	private final static LockSet<LockSetKey> bookingTransactions = new LockSet<LockSetKey>(128, true);

	private String long2string(Long id) {
		if (id == null) {
			return null;
		} 
		else {
			return ACT_BKG_ID + id;
		}
	}

	private Long string2long(String id) {
		if (id == null) {
			return null;
		} 
		else if (id.startsWith(ACT_BKG_ID)) {
			return Long.parseLong(id.substring(ACT_BKG_ID.length()));
		} 
		else if (id!=null){
			return Long.parseLong(id);
		}
		else {
			throw new IllegalArgumentException("Illegal booking ID: " +  id);
		}
	}

	private CFltBkg getCFltBkg(LockSetKey fltNdate) throws DataAccessException {
		Object[] args = { fltNdate.sFltId, fltNdate.date };
		String  hql = "from CFltBkg flt ";
		hql = hql + "where flt.sFltId = ? and flt.fltDate = ? ";
		hql = hql + "order by flt.depDate";

		List flts = this.getHibernateTemplate().find(hql, args);

		if (flts != null && flts.size() > 0) {
			return (CFltBkg) flts.get(0);
		}
		else {
			LOG.debug("AvailableCFltBkg: None");
			return null;
		}
	}

	private List<CFltBkg> getCFltBkgGroup(Long fltGroup, LockSetKey fltNdate) throws DataAccessException {
		Object[] args = { fltGroup, fltNdate.sFltId, fltNdate.date };

		String  hql = "from CFltBkg flt ";
		hql = hql + "where flt.fltGroup = ? ";
		hql = hql + "and flt.sFltId = ? and flt.fltDate = ? ";
		hql = hql + "order by flt.fltStops, flt.fltId";

		List<CFltBkg> ret = (List<CFltBkg>) this.getHibernateTemplate().find(hql, args);

		if (ret == null || ret.isEmpty()) {
			LOG.debug("CFltGroup: None");
			return Collections.emptyList();
		}
		else {
			LOG.debug("CFltGroup: " + ret.size());
			return ret;
		}
	}

	private List<LockSetKey> getFlights(String conxId, Date from, Date to) {
		Object[]      hqlArgs = { conxId, from, to };
		List<ConxFlt> conxFlts = (List<ConxFlt>) this.getHibernateTemplate().find("from ConxFlt cf" + " where cf.conxId = ? and cf.fltDate >= ? and cf.fltDate <= ?", hqlArgs);

		// handle degenerated case
		if (conxFlts == null || conxFlts.isEmpty()) {
			return Collections.emptyList();
		}

		// remap everything
		List<LockSetKey> ret = new ArrayList<LockSetKey>(conxFlts.size());
		for (ConxFlt cf : conxFlts) {
			try {
				ret.add(new LockSetKey(cf.getsFltId(), cf.getDate(), this));
			} 
			catch (DataRetrievalFailureException drfe) {
				LOG.error("Flight vanished.", drfe);
			}
		}
		return ret;
	}

	private LinkedList<CFltBkg> orderFlts(LinkedList<CFltBkg> flts) {
		LinkedList<CFltBkg> ret = new LinkedList<CFltBkg>();
		String dept;
		String dest;

		// initialize the variables
		ret.add(flts.pop());
		dept = ret.peek().getDeptAp();
		dest = ret.peek().getDestAp();

		while (!flts.isEmpty()) {
			for (Iterator<CFltBkg> it = flts.iterator(); it.hasNext(); ) {
				CFltBkg flt = it.next();
				if (flt.getDestAp().equals(dept)) {
					// flt fits before the other flights
					ret.addFirst(flt);
					dept = flt.getDeptAp();
					it.remove();
				} 
				else if (flt.getDeptAp().equals(dest)) {
					// flt fits after the other flights
					ret.addLast(flt);
					dest = flt.getDestAp();
					it.remove();
				}
			}
		}
		return ret;
	}

	private Map<String, Integer> analyzeFltGroup(List<CFltBkg> fltGroup) {
		int N = fltGroup.size();

		// first get the non-stop flights in an own list
		LinkedList<CFltBkg> flts = new LinkedList<CFltBkg>();
		for (int i = 0; i < N; i++) {
			CFltBkg flt = fltGroup.get(i);
			if (flt.getFltStops() > 0) {
				break; // only look at non-stop flights
			}
			flts.add(flt);
		}

		// get the flights in the right order
		flts = orderFlts(flts);

		// finally convert the list to a map
		Map<String, Integer> ret = new HashMap<String, Integer>((int) (flts.size() * 1.5) + 2);

		int i = 1;
		for (CFltBkg flt : flts) {
			ret.put(flt.getDestAp(), i);
			i++;
		}
		ret.put(flts.get(0).getDeptAp(), 0);
		return ret;
	}

	private void correctSums(CFltBkg avail, LockSetKey fltNdate) {
		if (avail.getFltGroup() == null) {
			return;
		}

		// Get the whole flight group ordered by the number of stops
		List<CFltBkg> fltGroup = getCFltBkgGroup(avail.getFltGroup(), fltNdate);
		Map<String, Integer> fltMap = analyzeFltGroup(fltGroup);

		// look for 'our' flight
		int i = Collections.binarySearch(fltGroup, avail, new Comparator<CFltBkg>() {
			public int compare(CFltBkg f1, CFltBkg f2) {
				// compare the stops first
				int i = f1.getFltStops() - f2.getFltStops();
				if (i != 0) {
					return i;
				}

				// now compare the ID
				long l = f1.getFltId() - f2.getFltId();

				if (l > 0L) {
					return 1;
				} 
				else if (l < 0L) {
					return -1;
				}
				else {
					return 0;
				}
			}});
		if (i < 0) {
			// Something ugly happened: Our flight vanished from the group!!!
			throw new RuntimeException("Wicked flight group '" +
					avail.getFltGroup() + "' is missing flight: " +
					avail.getFltId());
		}

		// initialize the variables
		WgtVol sumWgtVol = new WgtVol(0.0, 0.0);
		int deptI = fltMap.get(avail.getDeptAp());
		int destI = fltMap.get(avail.getDestAp());

		// sum up the weights and volumes of all flights that 'intersect' our flight
		for (CFltBkg flt : fltGroup) {
			if (deptI < fltMap.get(flt.getDestAp()) &&
					destI > fltMap.get(flt.getDeptAp())) {

				sumWgtVol.setWgt(sumWgtVol.getWgt() + flt.getSumWgt());
				sumWgtVol.setVol(sumWgtVol.getVol() + flt.getSumVol());
			}
		}

		// finally correct the sums
		avail.setSumWgt(sumWgtVol.getWgt());
		avail.setSumVol(sumWgtVol.getVol());
	}

	/* (non-Javadoc)
	 * @see de.act.blackbox.dblayer.daos.IBkgDao#makeBooking(java.lang.String, java.util.Date, de.act.common.types.WgtVol)
	 */
	private CentralBookingResult makeBooking(LockSetKey fltNdate, Integer addId,
			String awbNo, String fId, long comId, Long annId, String scc3lc,
			int pcs, WgtVol wgtVol, String minStatus, String cBkgId) {

		//
		// here we have to be careful with parallel transactions:
		//
		Lock lock = null;
		try {
			// now lock bookings for our flight and date
			lock = bookingTransactions.lock(fltNdate);

			CFltBkg avail = getCFltBkg(fltNdate);
			if (avail == null) {
				return null;
			}
			correctSums(avail, fltNdate);

			String status = null;
			if (avail.getAvailableAutoWgt() >= wgtVol.getWgt() && avail.getAvailableAutoVol() >= wgtVol.getVol()) {
				status = STATUS_CONFIRMED;
			} 
			else if (avail.getAvailableWgt() < wgtVol.getWgt() || avail.getAvailableVol() < wgtVol.getVol()){
				return null;
			} 
			else if (avail.getAvailableManualWgt() >= wgtVol.getWgt() && avail.getAvailableManualVol() >= wgtVol.getVol() && (minStatus == null || minStatus.equals(STATUS_WAIT_LIST))) {
				//status = STATUS_MANUAL;
				status = STATUS_WAIT_LIST;
			} 
			else if (minStatus.equals(STATUS_WAIT_LIST)) {
				status = STATUS_WAIT_LIST;
			}

			Long fltId = avail.getFltId();

			// create the booking record
			RCBkg bkg = new RCBkg();
			bkg.setId(null);
			bkg.setBkgId(string2long(cBkgId));
			bkg.setCorr(new Date());
			bkg.setFltId(fltId);
			bkg.setAddId(addId);
			bkg.setAwbNo(awbNo);
			bkg.setFid(fId);
			bkg.setPart(0);
			bkg.setBkgStatus(status);
			bkg.setComId(comId);
			bkg.setAnnId(annId);
			bkg.setScc3lc(scc3lc);
			bkg.setPcs(pcs);
			bkg.setWgt(wgtVol.getWgt());
			bkg.setVol(wgtVol.getVol());

			this.getHibernateTemplate().save(bkg);

			return new CentralBookingResult(long2string(bkg.getBkgId()),
					fltNdate.sFltId, fltNdate.date, status);
		} 
		catch(InterruptedException e){
			LOG.error(e.getMessage());
			return null;
		} 
		finally {
			// finally release our lock or clean up the hash map
			if (lock != null) {
				lock.unlock();
			}
		}
	}

	public RCFlt getCFlt(Long cFltId) {
		if (cFltId == null) {
			return null;
		} 
		else {
			return (RCFlt) this.getHibernateTemplate().load(RCFlt.class, cFltId);
		}
	}

	public RCFlt getCFlt(Long sFltId, Date date) {
		Object[] args = { sFltId , date };
		String  hql = "from RCFlt flt ";
		hql = hql + "where flt.sFltId = ? and date(flt.dep) = ?";
		hql = hql + "order by flt.dep";
		List flts = this.getHibernateTemplate().find(hql, args);

		if (flts != null && flts.size() > 0) {
			return (RCFlt) flts.get(0);
		} 
		else {
			//throw new DataRetrievalFailureException("No flight with ID '" + sFltId + "' on " + date + ".");
			//insert a new a_flt -> s_ac_comp
			return null;
		}
	}

	public void setCFlt(RCFlt cFlt) {
		this.getHibernateTemplate().update(cFlt);
	}

	public List<RCBkg> getBkgHistory(String bkgId) throws DataAccessException {
		List<RCBkg> bkgs  = (List<RCBkg>) this.getHibernateTemplate().find(
				"from RCBkg bkg" +
				" where bkg.bkgId = ?" +
				" order by bkg.corr desc", this.string2long(bkgId));

		if (bkgs == null || bkgs.isEmpty()) {
			LOG.debug("Bkgs: None");
			return Collections.emptyList();
		}
		else {
			LOG.debug("Bkgs: " + bkgs.size());
			return bkgs;
		}
	}

	public void setStatus(String bkgId, String status) {
		if (status == null) {
			throw new IllegalArgumentException("Illegal booking status: NULL");
		}
		List<RCBkg> bkgs = this.getBkgHistory(bkgId);

		if (bkgs.isEmpty()) {
			throw new IllegalArgumentException("Illegal booking ID: " +  bkgId);
		}

		RCBkg bkg = bkgs.get(0);
		if (status.equals(bkg.getBkgStatus())) {
			return;  // nothing to do
		}
		bkg.setId(null);
		bkg.setCorr(new Date());
		bkg.setBkgStatus(status);

		this.getHibernateTemplate().save(bkg);
	}

	public void cancel(String bkgId) {
		this.setStatus(bkgId, STATUS_CANCEL);
	}

	public CentralBookingResult makeAnyBooking(
			Long preferredSFltId, Date preferredDate,
			String conxId, Date from, Date to, Integer addId,
			String awbNo, String fId, long comId, Long annId, String scc3lc,
			int pcs, WgtVol wgtVol, String minStatus, String cBkgId){
		CentralBookingResult ret;
		// try the preferred flight first
		try {
			ret = this.makeBooking(
					new LockSetKey(preferredSFltId, preferredDate, this), addId,
					awbNo, fId, comId, annId, scc3lc, pcs, wgtVol, minStatus,
					cBkgId);
		} 
		catch (DataRetrievalFailureException drfe) {
			ret = null;
		}
		if (ret != null) {
			return ret;
		}

		List<LockSetKey> candidates = this.getFlights(conxId, from, to);
		for (LockSetKey fltNdate : candidates) {
			ret = this.makeBooking(fltNdate, addId, awbNo, fId, comId, annId,
					scc3lc, pcs, wgtVol, minStatus, cBkgId);
			if (ret != null) {
				return ret;
			}
		}

		return null;
	}

	public CentralBookingResult makeBooking(Long sFltId, Date date, Integer addId,
			String awbNo, String fId, long comId, Long annId, String scc3lc,
			int pcs, WgtVol wgtVol, String cBkgId){
		try {
			/** TO HERE **/
			return makeBooking(new LockSetKey(sFltId, date, this), addId, awbNo,
					fId, comId, annId, scc3lc, pcs, wgtVol, STATUS_WAIT_LIST,
					cBkgId);
		} 
		catch (DataRetrievalFailureException drfe) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see de.act.blackbox.dblayer.daos.IBkgDao#getAvailableWgtVol(java.lang.String, java.util.Date)
	 */
	public WgtVol getAvailableWgtVol(Long sFltId, Date date) {
		WgtVol  ret   = new WgtVol();
		try {
			CFltBkg avail = getCFltBkg(new LockSetKey(sFltId, date, this));

			if (avail == null) {
				ret.setWgt(0.0);
				ret.setVol(0.0);
			}
			else {
				ret.setWgt(avail.getAvailableWgt());
				ret.setVol(avail.getAvailableVol());
			}
		} 
		catch (DataRetrievalFailureException drfe) {
			ret.setWgt(0.0);
			ret.setVol(0.0);
		}
		return ret;
	}

	public boolean isAvailable(Long sFltId, Date date, WgtVol wgtVol) {
		WgtVol avail = this.getAvailableWgtVol(sFltId, date);

		if (avail.getWgt() >= wgtVol.getWgt() && avail.getVol() >= wgtVol.getVol()){
			return true;
		}
		return false;
	}
} // class CBkgDao
