package de.act.centralserver.dblayer.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import de.act.central.types.AccountingInformation;
import de.act.central.types.Addresses;
import de.act.central.types.Attachment;
import de.act.central.types.AwbState;
import de.act.central.types.BookingFlight;
import de.act.central.types.BookingFlightState;
import de.act.central.types.FomularDGR;
import de.act.central.types.FormularAVI;
import de.act.central.types.FormularAWB;
import de.act.central.types.FormularBKG;
import de.act.central.types.FormularInformation;
import de.act.central.types.FormularSLI;
import de.act.central.types.Manifest;
import de.act.central.types.ManifestItem;
import de.act.central.types.OtherCharges;
import de.act.central.types.PackageItem;
import de.act.central.types.PackageItemAVI;
import de.act.central.types.PackageItemAWB;
import de.act.central.types.PackageItemBKG;
import de.act.central.types.PackageItemDGR;
import de.act.central.types.PackageItemSLI;
import de.act.central.types.PackageState;
import de.act.central.types.Routing;
import de.act.central.types.Uld;
import de.act.centralserver.applayer.agent.ContentData;
import de.act.centralserver.dblayer.interfaces.IDBTransferManager;
import de.act.common.types.formulars.FormularType;
import de.act.common.types.handling.AManifestData;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.staticobjects.CCommodityTree;
import de.act.common.types.staticobjects.CommodityTree;
import de.act.common.types.staticobjects.ICommodityTree;
import de.act.common.types.staticobjects.RSAdd;
import de.act.common.types.staticobjects.RSAnn;
import de.act.common.types.staticobjects.RSFlt;
import de.act.common.types.staticobjects.RSFltConxCA;
import de.act.common.types.staticobjects.ZHandlingAirport;
import de.act.common.types.views.AddressView;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 01.11.2006
 * @update 02.11.2009
 */
@SuppressWarnings({"unused","unchecked"})
public class DBTransferManager extends HibernateDaoSupport implements IDBTransferManager {
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(DBTransferManager.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#loadData(de.act.common.types.nonstaticobjects.RFForm)
	 */
	public ContentData loadData(RFForm content) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			FormularInformation loadedForm = (FormularInformation) this.getHibernateTemplate().load(FormularInformation.class, content.getF_id());
			ContentData data = new ContentData(loadedForm);
			List<Attachment> loadedAttlist = this.getHibernateTemplate().find(
					"from Attachment a where a.fid = ? and (a.auth <> 'r' or a.attType = 'pkg' or a.attType = 'bkg_flt') and a.attType <> 'acc'", content.getF_id());
			// List<Attachment> tranportAttachments = new ArrayList<Attachment>();
			// for(Attachment a:loadedAttlist){
			// if (a!=null) {
			// if (!a.getAuth().equals("r")) {
			// tranportAttachments.add(a);
			// }
			// }
			// }
			// set to r
			for (Attachment a : loadedAttlist) {
				a.setAuth("r");
				s.saveOrUpdate(a);
			}

			data.setAttachments(loadedAttlist);
			tr.commit();
			return data;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.IDBTranferManager#storeData(de.act.central.applayer.ContentData)
	 */
	public synchronized boolean storeData(FormularInformation formularInfo, final List<Attachment> listAtt) {
		Session s = this.getSession();
		final Transaction tr = s.beginTransaction();
		try {
			FormularInformation f = formularInfo;
			log.info("updating formularinformations");
			FormularInformation existObj = null;
			try {
				List<FormularInformation> list = this.getHibernateTemplate().find("from FormularInformation f where f.FId = ?", f.getFId());
				if (list != null && list.size() > 0) {
					existObj = list.get(0);
				}
			} 
			catch (DataAccessException e) {
				// log.error(e.getMessage());
			}
			if (existObj != null) {
				s.update(f);
			} 
			else {
				s.save(f);
			}
			log.info("updating attachments");
			List<Attachment> dba = this.getHibernateTemplate().find("from Attachment a where  a.fid = ?", f.getFId());
			if (dba != null && dba.size() > 0) {
				log.info("found ids");
				for (Attachment a : listAtt) {
					if (dba.contains(a)) {
						log.info("found id for " + a.getAttType());
						int foundIndex = dba.indexOf(a);
						Attachment found = dba.get(foundIndex);
						a.setAttId(found.getAttId());
						if (a.getAttType().equals(FormularType.AWB_ITEM.getConstaint())) {
							this.injectIdForItemAWB(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.ADDRESS.getConstaint())) {
							this.injectIdForAddress(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.AWB_STATE.getConstaint())) {
							this.injectIdForAwbState(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.AWB.getConstaint())) {
							this.injectIdForOtherCharges(dba, a);
							this.injectIdForAwb(dba, a);
							this.injectIdForAccountingInformation(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.SLI.getConstaint())) {
							this.injectIdForSli(dba, a);
							this.injectIdForAvi(dba, a);
							this.injectIdForDgr(dba, a);
							this.injectIdForAccountingInformation(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.BKG.getConstaint())) {
							this.injectIdForBkg(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.AVI.getConstaint())) {
							this.injectIdForAvi(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.DGR.getConstaint())) {
							this.injectIdForDgr(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.ROUTING.getConstaint())) {
							this.injectIdForRouting(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.ACOUNING_INFORMATION.getConstaint())) {
							this.injectIdForAccountingInformation(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.AVI_ITEM.getConstaint())) {
							this.injectIdForItemAVI(dba, a);
						}
						else if (a.getAttType().equals(FormularType.DGR_ITEM.getConstaint())) {
							this.injectIdForItemDGR(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.PACKAGE.getConstaint())) {
							this.injectIdForItemPKG(dba, a);
							this.injectIdForItemSLI(dba, a);
							// maybe all packageinformation are in one attachment
							this.injectIdForItemBKG(dba, a);
							this.injectIdForItemAWB(dba, a);
							this.injectIdForItemAVI(dba, a);
							this.injectIdForItemDGR(dba, a);
							this.injectIdForPackageState(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.BKG_FLIGHT.getConstaint())) {
							this.injectIdForItemBKG(dba, a);
							this.injectIdForBookingFlight(dba, a);
							// ItemBKG darf kein transientes object sein.
							// ht.persist(itemBKG)??
							this.injectIdForBookingFlightState(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.BKG_FLIGHT_STATE.getConstaint())) {
							this.injectIdForItemBKG(dba, a);
							this.injectIdForBookingFlight(dba, a);
							this.injectIdForBookingFlightState(dba, a);
						} 
						else if (a.getAttType().equals(FormularType.PACKAGE_STATE.getConstaint())) {
							this.injectIdForPackageState(dba, a);
						} // end if PACKAGE_STATE
					} 
					else {
						if (a.getAttType().equals(FormularType.BKG_FLIGHT.getConstaint())) {
							this.injectIdForBookingFlight(dba, a);
						}
					}
				}// end foreach Attachment
			} else {
				log.info("no ids found");
			}

			// store sequence is needed: pkg, add, bgk_flt, bkg_flt_stat, formulars.
			log.info(listAtt);
			Collections.sort(listAtt, new Comparator<Attachment>() {
				public int compare(Attachment o1, Attachment o2) {
					return o1.getAttNo().compareTo(o2.getAttNo());
				}
			});
			log.info(listAtt);
			Collections.sort(listAtt, new Comparator<Attachment>() {
				public int compare(Attachment o1, Attachment o2) {
					if (o1.getAttType().equals("pkg")) {
						return o1.getAttNo().compareTo(o2.getAttNo());
					}
					if (o2.getAttType().equals("pkg")) {
						return 1;
					}
					if (o1.getAttType().equals("add")) {
						return -1;
					}
					if (o2.getAttType().equals("add")) {
						return 1;
					}
					if (o1.getAttType().equals("bkg_flt") && o2.getAttType().equals("pkg")) {
						return 1;
					}
					if (o2.getAttType().equals("bkg_flt") && o1.getAttType().equals("pkg")) {
						return -1;
					}
					if (o2.getAttType().equals("bkg_flt")) {
						return 1;
					}
					if (o1.getAttType().equals("bkg_flt")) {
						return -1;
					}
					return 0;
				}
			});
			log.info(listAtt);
			Collections.sort(listAtt, new Comparator<Attachment>() {
				public int compare(Attachment o1, Attachment o2) {
					return o1.getAttNo().compareTo(o2.getAttNo());
				}
			});
			log.info(listAtt);
			// this.getHibernateTemplate().execute(new HibernateCallback() {
			// public Object doInHibernate(Session session) throws HibernateException,
			// SQLException {
			// // session.setFlushMode(FlushMode.MANUAL);
			for (Attachment a : listAtt) {
				if (a.getAttType().equals("bkg_flt")) {
					log.info("look here attid= " + a.getAttId() + a.getAttNo());
					if (a.getAttId() == null && a.getAttNo() > 0) {
						log.warn("this object should be inserted --> ");
						List<Attachment> lista = this.getHibernateTemplate().findByExample(a);
						if (lista != null && lista.size() > 0) {
							// TODO find the error and remove this workaround
							log.error("found object with id == null, something is going wrong. should be updated -> workaround setting id ");
							a.setAttId(lista.get(0).getAttId());
						} 
						else {
							log.info("insert ok");
						}
					}
				}
				log.info("saveOrUpdate " + a.getAttType() + " - " + a.getAttNo());
				try {
					s.saveOrUpdate(a);
				} 
				catch (DataAccessException e) {
					log.error(e.getMessage(), e);
					throw e;
				} 
				catch (RuntimeException e) {
					log.error("Exception at: " + a.toString());
					log.error(e.getMessage());
					throw e;
				}
			}
			// session.flush();
			// return null;
			// }
			// }, true);
			// this.getHibernateTemplate().saveOrUpdate(listAtt);
			tr.commit();
			return true;
		} catch (DataAccessException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
		} 
		finally
		{
			if(s != null && s.isOpen())
				s.close();
		}
		int a;
		return false;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 05.12.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForBkg(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<FormularBKG> bkgFltStates = a.getFBkgs();
		Set<FormularBKG> oldStates = equal.getFBkgs();
		if (oldStates != null && bkgFltStates != null) {
			for (FormularBKG bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (FormularBKG old : oldStates) {
						if (old.equals(bs)) {
							bs.setAttId(old.getAttId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForSli(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<FormularSLI> bkgFltStates = a.getFSlis();
		Set<FormularSLI> oldStates = equal.getFSlis();
		if (oldStates != null && bkgFltStates != null) {
			for (FormularSLI bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (FormularSLI old : oldStates) {
						if (old.equals(bs)) {
							bs.setAttId(old.getAttId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForAwb(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<FormularAWB> bkgFltStates = a.getFAwbs();
		Set<FormularAWB> oldStates = equal.getFAwbs();
		if (oldStates != null && bkgFltStates != null) {
			for (FormularAWB bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (FormularAWB old : oldStates) {
						if (old.equals(bs)) {
							bs.setAttId(old.getAttId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForAvi(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<FormularAVI> bkgFltStates = a.getFAvis();
		Set<FormularAVI> oldStates = equal.getFAvis();
		if (oldStates != null && bkgFltStates != null) {
			for (FormularAVI bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (FormularAVI old : oldStates) {
						if (old.equals(bs)) {
							bs.setAttId(old.getAttId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForDgr(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<FomularDGR> bkgFltStates = a.getFDgrs();
		Set<FomularDGR> oldStates = equal.getFDgrs();
		if (oldStates != null && bkgFltStates != null) {
			for (FomularDGR bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (FomularDGR old : oldStates) {
						if (old.equals(bs)) {
							bs.setAttId(old.getAttId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForRouting(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<Routing> bkgFltStates = a.getFRts();
		Set<Routing> oldStates = equal.getFRts();
		if (oldStates != null && bkgFltStates != null) {
			for (Routing bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (Routing old : oldStates) {
						if (old.equals(bs)) {
							bs.setRtId(old.getRtId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForItemBKG(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageItemBKG> bkgFltStates = a.getFBkgItems();
		Set<PackageItemBKG> oldStates = equal.getFBkgItems();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageItemBKG bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageItemBKG old : oldStates) {
						if (old.equals(bs)) {
							bs.setBkgItemId(old.getBkgItemId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForOtherCharges(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		Set<OtherCharges> bkgFltStates = a.getFOts();
		Set<OtherCharges> oldStates = equal.getFOts();
		for (OtherCharges bs : bkgFltStates) {
			if (oldStates.contains(bs)) {
				for (OtherCharges old : oldStates) {
					if (old.equals(bs)) {
						bs.setOtId(old.getOtId());
						break;
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForAddress(List<Attachment> dba, Attachment a) {
		Set<Addresses> adds = a.getFAdds();
		if (adds != null && adds.size() > 0) {
			for (Addresses ad : adds) {
				ad.setAttId(a.getAttId());
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForAccountingInformation(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<AccountingInformation> bkgFltStates = a.getFAcis();
		Set<AccountingInformation> oldStates = equal.getFAcis();
		if (bkgFltStates != null && bkgFltStates.size() > 0) for (AccountingInformation bs : bkgFltStates) {
			if (oldStates.contains(bs)) {
				for (AccountingInformation old : oldStates) {
					if (old.equals(bs)) {
						bs.setId(old.getId());
						break;
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForAwbState(List<Attachment> dba, Attachment a) {
		Set<AwbState> adds = a.getFAwbStats();
		if (adds != null && adds.size() > 0) {
			for (AwbState ad : adds) {
				ad.setAttachment(a.getAttId());
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForBookingFlightState(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		Set<BookingFlightState> bkgFltStates = a.getFBkgFltStats();
		Set<BookingFlightState> oldStates = equal.getFBkgFltStats();
		for (BookingFlightState bs : bkgFltStates) {
			if (oldStates.contains(bs)) {
				for (BookingFlightState old : oldStates) {
					if (old.equals(bs)) {
						bs.setBkgFltStatId(old.getBkgFltStatId());
						break;
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return
	 * @return Attachment
	 */
	private Attachment getEqualAttachment(List<Attachment> dba, Attachment a) {
		for (Attachment b : dba) {
			if (b.equals(a)) return b;
		}
		return null;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForBookingFlight(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) {
			Set<BookingFlight> listBkgs = a.getFBkgFlts();
			for (BookingFlight b : listBkgs) {
				PackageItemBKG oldBkg = b.getFBkgItem();
				if (oldBkg.getBkgItemId() == null) {
					Attachment pkgOldAtt = this.getEqualAttachment(dba, oldBkg.getFAtt());
					Set<PackageItemBKG> list = pkgOldAtt.getFBkgItems();
					for (PackageItemBKG bkg : list) {
						if (oldBkg.getPart().equals(bkg.getPart())) {
							b.setFBkgItem(bkg);
						}
					}
				} else {
					// oldBkg.setBkgItemId(null);
				}
			}
			return;
		}
		Set<BookingFlight> bkgFltStates = a.getFBkgFlts();
		Set<BookingFlight> oldStates = equal.getFBkgFlts();
		if (oldStates != null && bkgFltStates != null) {
			for (BookingFlight bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (BookingFlight old : oldStates) {
						if (old.equals(bs)) {
							bs.setBkgFltId(old.getBkgFltId());
							if (bs.getFBkgItem() != null && old.getFBkgItem() != null)
								bs.getFBkgItem().setBkgItemId(old.getFBkgItem().getBkgItemId()); // set
							// transient
							// reference
							else {
								bs.setFBkgItem(old.getFBkgItem());
							}
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForPackageState(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageState> bkgFltStates = a.getFPkgStats();
		Set<PackageState> oldStates = equal.getFPkgStats();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageState bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageState old : oldStates) {
						if (old.equals(bs)) {
							bs.setPkgStatId(old.getPkgStatId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForItemPKG(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageItem> bkgFltStates = a.getFPkgs();
		Set<PackageItem> oldStates = equal.getFPkgs();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageItem bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageItem old : oldStates) {
						if (old.equals(bs)) {
							bs.setPkgId(old.getPkgId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForItemSLI(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageItemSLI> bkgFltStates = a.getFSliItems();
		Set<PackageItemSLI> oldStates = equal.getFSliItems();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageItemSLI bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageItemSLI old : oldStates) {
						if (old.equals(bs)) {
							bs.setPkgId(old.getPkgId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForItemDGR(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageItemDGR> bkgFltStates = a.getFDgrItems();
		Set<PackageItemDGR> oldStates = equal.getFDgrItems();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageItemDGR bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageItemDGR old : oldStates) {
						if (old.equals(bs)) {
							bs.setDgrItemId(old.getDgrItemId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForItemAVI(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageItemAVI> bkgFltStates = a.getFAviItems();
		Set<PackageItemAVI> oldStates = equal.getFAviItems();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageItemAVI bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageItemAVI old : oldStates) {
						if (old.equals(bs)) {
							bs.setAviItemId(old.getAviItemId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dba
	 * @param a
	 * @return void
	 */
	private void injectIdForItemAWB(List<Attachment> dba, Attachment a) {
		Attachment equal = this.getEqualAttachment(dba, a);
		if (equal == null) return;
		Set<PackageItemAWB> bkgFltStates = a.getFAwbItems();
		Set<PackageItemAWB> oldStates = equal.getFAwbItems();
		if (oldStates != null && bkgFltStates != null) {
			for (PackageItemAWB bs : bkgFltStates) {
				if (oldStates.contains(bs)) {
					for (PackageItemAWB old : oldStates) {
						if (old.equals(bs)) {
							bs.setAwbItemId(old.getAwbItemId());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param pkgs
	 * @param a
	 * @return void
	 */
	private void injectIdForItemPKG(Set<PackageItem> pkgs, Attachment a) {
		if (pkgs != null && pkgs.size() > 0) {
			for (PackageItem p : pkgs) {
				DetachedCriteria crit = DetachedCriteria.forClass(de.act.central.types.PackageItem.class).add(
						Restrictions.naturalId().set("rowNo", p.getRowNo()).set("FAtt", a));
				Long id = this.findIdByCriteria(crit);
				p.setPkgId(id);
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param dgrItems
	 * @param a
	 * @return void
	 */
	private void injectIdForItemDGR(Set<PackageItemDGR> pkgs, Attachment a) {
		if (pkgs != null && pkgs.size() > 0) {
			for (PackageItemDGR p : pkgs) {
				DetachedCriteria crit = DetachedCriteria.forClass(de.act.central.types.PackageItemDGR.class).add(
						Restrictions.naturalId().set("aviRowNo", p.getDgrRowNo()).set("FAtt", a));
				Long id = this.findIdByCriteria(crit);
				p.setDgrItemId(id);
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param pkgs
	 * @param a
	 * @return void
	 */
	private void injectIdForItemAVI(Set<PackageItemAVI> pkgs, Attachment a) {
		if (pkgs != null && pkgs.size() > 0) {
			for (PackageItemAVI p : pkgs) {
				DetachedCriteria crit = DetachedCriteria.forClass(de.act.central.types.PackageItemAVI.class).add(
						Restrictions.naturalId().set("aviRowNo", p.getAviRowNo()).set("FAtt", a));
				Long id = this.findIdByCriteria(crit);
				p.setAviItemId(id);
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param pkgs
	 * @param a
	 * @return void
	 */
	private void injectIdForItemAWB(Set<PackageItemAWB> pkgs, Attachment a) {
		if (pkgs != null && pkgs.size() > 0) {
			for (PackageItemAWB p : pkgs) {
				DetachedCriteria crit = DetachedCriteria.forClass(de.act.central.types.PackageItemAWB.class).add(
						Restrictions.naturalId().set("awbRow", p.getAwbRow()).set("FAtt", a));
				Long id = this.findIdByCriteria(crit);
				p.setAwbItemId(id);
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param pkgs
	 * @param a
	 * @return void
	 */
	private void injectIdForBookingFlight(Set<BookingFlight> pkgs, Attachment a) {
		if (pkgs != null && pkgs.size() > 0) {
			for (BookingFlight p : pkgs) {
				// DetachedCriteria crit =
				// DetachedCriteria.forClass(de.act.central.types.BookingFlight.class).add(
				// Restrictions.naturalId().set("fltRow", p.getFltRow()).set("FBkgItem",
				// p.getFBkgItem()).set("FAtt", a));
				Object[] objs = {a, p.getFBkgItem().getPart(), p.getFltRow()};
				List<BookingFlight> list = this.getHibernateTemplate().find(
						"from de.act.central.types.BookingFlight f where f.FAtt = ? and f.FBkgItem.part = ? and f.fltRow = ? ", objs);
				if (list != null && list.size() > 0) {
					BookingFlight bkg = list.get(0);
					p.setBkgFltId(bkg.getBkgFltId());
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param pkgs
	 * @return void
	 */
	private void injectIdForBookingFlightState(Set<BookingFlightState> pkgs, Attachment a) {
		if (pkgs != null && pkgs.size() > 0) {
			for (BookingFlightState p : pkgs) {
				Object[] objs = {a, p.getFBkgFlt().getFltRow(), p.getBkgStatus()};
				List<BookingFlightState> list = this.getHibernateTemplate().find(
						"from de.act.central.types.BookingFlightState f where f.FAtt = ? and f.FBkgFlt.fltRow = ? and f.bkgStatus = ? ", objs);
				if (list != null && list.size() > 0) {
					BookingFlightState bkg = list.get(0);
					p.setBkgFltStatId(bkg.getBkgFltStatId());
				}
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param pkgs4
	 * @return void
	 */
	private void injectIdForPackageState(Set<PackageState> pkgs4, Attachment a) {
		if (pkgs4 != null && pkgs4.size() > 0) {
			for (PackageState p : pkgs4) {
				DetachedCriteria crit = DetachedCriteria.forClass(de.act.central.types.PackageState.class).add(
						Restrictions.naturalId().set("FPkg", p.getFPkg()).set("pkgStatus", p.getPkgStatus()).set("FAtt", a));
				Long id = this.findIdByCriteria(crit);
				p.setPkgStatId(id);
			}
		}
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 16.11.2006
	 * @param crit
	 * @return
	 * @return Long
	 */
	private Long findIdByCriteria(DetachedCriteria crit) {
		List list = this.getHibernateTemplate().findByCriteria(crit);
		if (list != null && list.size() > 0) {
			Object obj = list.get(0);
			if (obj instanceof Long) {
				Long id = (Long) obj;
				return id;
			}
		}
		return -1L;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#storeData(de.act.common.types.handling.AManifestData)
	 */
	public boolean storeData(Manifest manifest) {
		Object[] keys = {manifest.getFltDate(), manifest.getFltId(), manifest.getUserAddId()};
		// search manifest in DB
		List<Manifest> manis = this.getHibernateTemplate().find("from Manifest where fltDate = ? and fltId = ? and userAddId = ?", keys);
		if (manis != null && manis.size() > 0) {
			Manifest manidb = manis.get(manis.size() - 1);
			// manifest allready exists
			log.error("manifest allready exists");
			return false;
		}
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			Set<ManifestItem> items = manifest.getAManiItems();
			if (items != null) {
				for (ManifestItem mi : items) {
					PackageItemBKG bkgItem = mi.getFBkgItem();
					if (bkgItem != null) {
						Attachment bkgAtt = bkgItem.getFAtt();
						Object[] objs = {bkgItem.getPart(), bkgAtt.getAddId(), bkgAtt.getAttNo(), bkgAtt.getAttType(), bkgAtt.getFid()};
						List<PackageItemBKG> list = this.getHibernateTemplate().find(
								"from PackageItemBKG b where b.part =? and b.FAtt.addId =? and b.FAtt.attNo =? and b.FAtt.attType =? and b.FAtt.fid = ?", objs);
						if (list != null && list.size() > 0) {
							PackageItemBKG bkgI = list.get(0);
							// bkgItem.setBkgItemId(bkgI.getBkgItemId()); // sett none
							// cascaded object as saved
							mi.setFBkgItem(bkgI);
							// Set<AcceptanceItem> shps = mi.getShpItems();
							mi.setShpItems(null);
							// if (shps!=null) {
							// for(AcceptanceItem a:shps) {
							// a.setBkgItemId(bkgI.getBkgItemId());
							// }
							// }

							// mi.getFAccItemByAwbAccItemId().setBkgItemId(bkgI.getBkgItemId());
							// mi.getFAccItemByShpAccItemId().setBkgItemId(bkgI.getBkgItemId());
						} 
						else {
							mi.setFBkgItem(null);
							log.fatal("bkgItem not found!!");
						}
					}
					Uld uld = mi.getAUld(); // change holder of uld
					Uld newUld = (Uld) setfindSingleObjectByExample(uld);
					if (newUld != null) {
						newUld.setHolderAddId(manifest.getUserAddId());
						mi.setAUld(newUld);
						s.saveOrUpdate(newUld);
					} 
					else {
						mi.setAUld(null);
					}

					// AcceptanceUld accUld = mi.getFAccUld();
					// if (accUld != null){
					// Uld acceptedUld = accUld.getAUld();
					// accUld.setAUld((Uld) setfindSingleObjectByExample(acceptedUld));
					// }

				}
			}
			s.saveOrUpdate(manifest);
			tr.commit();
			return true;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
			// mi.get
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
		}
		finally
		{
			if(s != null && s.isOpen())
				s.close();
		}
		return false;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 23.11.2006
	 * @param uld
	 * @return
	 * @return Uld
	 */
	private Object setfindSingleObjectByExample(Object uld) {
		if (uld != null) {
			if (uld instanceof Uld) {
				Uld u = (Uld) uld;
				Object[] objs = {u.getUldPrefix(), u.getUldNo(), u.getCaId()};
				List test = this.getHibernateTemplate().find("from Uld a where a.uldPrefix =? and a.uldNo =? and a.caId =?", objs);
				if (test != null && test.size() == 1) {
					return test.get(0);
				} 
				else if (test != null && test.size() > 1) {
					log.fatal("setfindSingleObjectByExample(" + uld + ") - more than one OBJECT found !");
					return test.get(0);
				}
				u.setUldId(null);
				return uld;
			}
		}
		return uld;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#getConsigneeImportAgent(java.lang.String)
	 */
	public Integer getConsigneeImportAgent(Integer con) {
		try {
			RSAdd conadd = (RSAdd) this.getHibernateTemplate().load(RSAdd.class, con);
			if (conadd != null) {
				return conadd.getImportAgent().getAdd_id();
			}
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#getHandlingAddressByAirport(java.lang.Long,
	 *      java.lang.String)
	 */
	public Integer getHandlingAddressByFlight(String bkgFltId, String service) {
		try {
			RSFlt fl = (RSFlt) this.getHibernateTemplate().load(RSFlt.class, bkgFltId);
			RSFltConxCA fltconxca = new RSFltConxCA(fl);
			String AP_3LC = fltconxca.conx.dept.ap_3lc;
			if (service.equals("i")) {
				AP_3LC = fltconxca.conx.dest.ap_3lc;
			}
			Object[] objs = {AP_3LC, fltconxca.conx.ca_id, service};
			List<ZHandlingAirport> list = this.getHibernateTemplate().find(
					"from ZHandlingAirport a where a.ap_3lc = ? and a.ca_id= ? and (a.service = ? OR a.service = 'a')", objs);
			if (list != null && list.size() > 0) {
				ZHandlingAirport ha = list.get(0);
				AddressView add = ha.getAddress();
				return add.getAdd_id();
			} 
			else {
				Object[] objs2 = {AP_3LC, service};
				list = this.getHibernateTemplate().find("from ZHandlingAirport a where a.ap_3lc = ? and a.ca_id= '000' and (a.service = ? OR a.service = 'a')", objs2);
				if (list != null && list.size() > 0) {
					ZHandlingAirport ha = list.get(0);
					AddressView add = ha.getAddress();
					return add.getAdd_id();
				}
				else if (list != null && list.size() > 1) {
					log.error("getHandlingAddressByAirport(" + AP_3LC + ") more than one address for airport: " + list.size());
				}
				else {
					if (list != null)
						log.error("getHandlingAddressByAirport(" + AP_3LC + ") " + list.size());
					else
						log.error("getHandlingAddressByAirport(" + AP_3LC + ") == null");
				}
			}
			return null;
		} 
		catch (DataAccessException e) {
			log.error(" ", e);
			return null;
		}
		catch (RuntimeException e) {
			log.error(" ", e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#getHandlingAddressByAirport(java.lang.String,
	 *      java.lang.String)
	 */
	public Integer getHandlingAddressByAirport(String ap, String service) {
		try {
			Object[] objs = {ap, service};
			List<ZHandlingAirport> list = this.getHibernateTemplate().find("from ZHandlingAirport a where a.ap_3lc = ? and (a.service = ? OR a.service = 'a')", objs);
			if (list != null && list.size() > 0) {
				ZHandlingAirport ha = list.get(0);
				AddressView add = ha.getAddress();
				return add.getAdd_id();
			} 
			else {
				if (list != null)
					log.error("getHandlingAddressByAirport(" + ap + ") " + list.size());
				else
					log.error("getHandlingAddressByAirport(" + ap + ") == null");
			}
			return null;
		} 
		catch (DataAccessException e) {
			log.error(" ", e);
			return null;
		} 
		catch (RuntimeException e) {
			log.error(" ", e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBTranferManager#storeFormularInformations(de.act.central.types.FormularInformation)
	 */
	public FormularInformation storeFormularInformations(FormularInformation dbf) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			this.getHibernateTemplate().merge(dbf);
			tr.commit();
			return dbf;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
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
		try {
			return (RFForm) this.getHibernateTemplate().load(RFForm.class, id);
		}
		catch (DataAccessException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#getCommodityAnnotationById(java.lang.String)
	 */
	public RSAnn getCommodityAnnotationById(String comId) {
		try {
			CommodityTree com = (CommodityTree) this.getHibernateTemplate().load(CommodityTree.class, comId);
			if (com != null)
				return com.getAnn();
			else
				return null;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#getFlightById(java.lang.String)
	 */
	public RSFltConxCA getFlightById(String fltId) {
		try {
			return (RSFltConxCA) this.getHibernateTemplate().load(RSFltConxCA.class, fltId);
		}
		catch (DataAccessException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#storeBookingData(java.util.Set,
	 *      java.util.Set)
	 */
	public void storeBookingData(Set<BookingFlight> flights, Set<BookingFlightState> flightStates) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			for (BookingFlightState state : flightStates) {
				s.saveOrUpdate(state);
			}
			tr.commit();
		}
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		}
		finally
		{
			if(s != null && s.isOpen())
				s.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#loadData(de.act.central.types.FormularInformation)
	 */
	public List<Attachment> loadData(FormularInformation formularInfo) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			List<Attachment> loadedAttlist = this.getHibernateTemplate().find("from Attachment a where a.fid = ? and a.attType <> 'acc'", formularInfo.getFId());
			tr.commit();
			return loadedAttlist;
		}
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#loadAllData(de.act.central.applayer.agent.ContentData)
	 */
	public ContentData loadAllData(ContentData contentData) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			FormularInformation loadedForm = (FormularInformation) this.getHibernateTemplate().load(FormularInformation.class,
					contentData.getFormularInformations().getFId());
			ContentData data = new ContentData(loadedForm);
			List<Attachment> loadedAttlist = this.loadData(loadedForm);
			data.setAttachments(loadedAttlist);
			tr.commit();
			return data;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#getFlightConx(java.lang.String)
	 */
	public RSFltConxCA getFlightConx(String flightId) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			RSFlt flt = (RSFlt) this.getHibernateTemplate().load(RSFlt.class, flightId);
			RSFltConxCA data = new RSFltConxCA(flt);
			tr.commit();
			return data;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#loadFormularInfo(java.lang.String)
	 */
	public FormularInformation loadFormularInfo(String fid) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			FormularInformation data = (FormularInformation) this.getHibernateTemplate().load(FormularInformation.class, fid);
			tr.commit();
			return data;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#loadData(de.act.common.types.handling.AManifestData)
	 */
	public ContentData loadData(AManifestData manifest) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			Object[] objs = {manifest.getFlt_id().flt_id, manifest.getFlt_date(), manifest.getUser_add_id().getAdd_id()};
			List<Manifest> list = this.getHibernateTemplate().find("from Manifest a where a.fltId =? and a.fltDate =? and a.userAddId =?", objs);
			if (list != null && list.size() > 0) {
				Manifest loadedForm = (Manifest) list.get(0);
				ContentData data = new ContentData(loadedForm);
				tr.commit();
				return data;
			}
			else
				return null;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#loadData(de.act.central.types.Manifest)
	 */
	public AManifestData loadData(Manifest manifest) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			Object[] objs = {manifest.getFltId(), manifest.getFltDate(), manifest.getUserAddId()};
			List<AManifestData> list = this.getHibernateTemplate().find(
					"from AManifestData a where a.flt_id.FLT_ID =? and a.flt_date =? and a.user_add_id.add_id =?", objs);
			if (list != null && list.size() > 0) {
				AManifestData loadedForm = (AManifestData) list.get(0);
				tr.commit();
				return loadedForm;
			}
			else
				return null;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		} 
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#reLoadData(de.act.central.types.FormularInformation)
	 */
	public ContentData reLoadData(FormularInformation ffrom, List<Attachment> att) {
		Session s = this.getSession();
		Transaction tr = s.beginTransaction();
		try {
			FormularInformation loadedForm = (FormularInformation) this.getHibernateTemplate().load(FormularInformation.class, ffrom.getFId());
			ContentData data = new ContentData(loadedForm);
			List<Attachment> loadedAttlist = this.getHibernateTemplate().find("from Attachment a where a.fid = ? and a.attType <> 'acc' and (a.auth <> 'r') ",
					ffrom.getFId());
			for (Attachment a : loadedAttlist) {
				a.setAuth("r");
				s.saveOrUpdate(a);
			}

			for (Attachment attachment : att) {
				if (!loadedAttlist.contains(attachment)) {
					loadedAttlist.add(attachment);// liste um attachments erweitern die
					// noch zum Client gesendet werden
					// sollen.
				}
			}
			data.setAttachments(loadedAttlist);
			tr.commit();
			return data;
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
			tr.rollback();
		}
		catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			tr.rollback();
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
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#getAddressBy(java.lang.String)
	 */
	public RSAdd getAddressBy(String addId) {
		try {
			RSAdd conadd = (RSAdd) this.getHibernateTemplate().load(RSAdd.class, addId);
			if (conadd != null) {
				return conadd;
			}
		} 
		catch (DataAccessException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#storeData(de.act.common.types.staticobjects.RSAdd)
	 */
	public Boolean storeData(RSAdd address) {

		Session session = this.getHibernateTemplate().getSessionFactory().openSession();
		Transaction tr = session.beginTransaction();
		try {
			Object obj = session.get(RSAdd.class, address.getAdd_id());
			if (obj!=null) {
				log.error("Address already exists on DB: "+address.getAdd_id());
				return false;
			}
			session.save(address);
			tr.commit();
		}
		catch (DataAccessException e) {
			log.error("storeData(Address) ",e);
			tr.rollback();
			return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see de.act.central.dblayer.interfaces.IDBTransferManager#storeData(de.act.common.types.staticobjects.ICommodityTree)
	 */
	public Boolean storeData(ICommodityTree com) {

		Session session = this.getHibernateTemplate().getSessionFactory().openSession();
		Transaction tr = session.beginTransaction();
		try {
			Object obj = session.get(CCommodityTree.class, com.getId());
			if (obj!=null) {
				log.error("Commodity already exists on DB: "+com.getAwbName());
				return false;
			}
			session.save(com);
			tr.commit();
		} 
		catch (DataAccessException e) {
			log.error("storeData(Commodity) ",e);
			tr.rollback();
			return false;
		}
		return true;
	}
}
