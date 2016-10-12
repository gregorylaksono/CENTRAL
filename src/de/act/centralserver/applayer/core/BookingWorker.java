package de.act.centralserver.applayer.core;

import java.util.Date;
import java.util.Set;

import org.apache.log4j.Logger;

import de.act.central.types.AwbNo;
import de.act.central.types.BookingFlight;
import de.act.central.types.BookingFlightState;
import de.act.central.types.FormularInformation;
import de.act.central.types.PackageItemBKG;
import de.act.centralserver.dblayer.interfaces.IBkgDao;
import de.act.centralserver.dblayer.interfaces.IDBBookingManager;
import de.act.common.types.CentralBookingResult;
import de.act.common.types.WgtVol;
import de.act.common.types.nonstaticobjects.RCFlt;
import de.act.common.types.staticobjects.RSAnn;
import de.act.common.types.staticobjects.RSFltConxCA;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 11.10.2006
 * @update 02.11.2009
 */
@SuppressWarnings({"unused", "deprecation"})
public class BookingWorker implements Runnable {
	/**
	 * Logger for this class
	 */
	private static final Logger           log = Logger.getLogger(BookingWorker.class);
	private final IBkgDao                 bkgDao;
	private final IDBBookingManager       dbBookingManager;
	private final Set<BookingFlightState> flightStates;
	private final Set<BookingFlight>      flights;
	private final PackageItemBKG          bkgItem;
	private FormularInformation           info;

	/**
	 * @author Martin Sachs
	 * @param info
	 * @since 1.0 - 20.11.2006
	 * @param bkgItem
	 * @param flights
	 * @param flightStates
	 * @param dbTransferManager
	 * @param bkgDao2
	 */
	public BookingWorker(FormularInformation info, PackageItemBKG bkgItem, Set<BookingFlight> flights, Set<BookingFlightState> flightStates,
			IDBBookingManager dbTransferManager, IBkgDao bkgDao2) {
		this.info = info;
		this.bkgItem = bkgItem;
		this.flights = flights;
		this.flightStates = flightStates;
		this.bkgDao = bkgDao2;
		this.dbBookingManager = dbTransferManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 11.10.2006
	 * @return void
	 */
	public CentralBookingResult book() {
		if (this.bkgItem == null) return null;
		PackageItemBKG bkg = this.bkgItem;
		if (bkg != null) {
			if (flights != null && flights.size() > 0) {
				Integer addId = this.bkgItem.getFAtt().getAddId();
				AwbNo awbno = this.info.getAwbNo();
				String fid = this.info.getFId();
				if (awbno != null) {
					String awbNo = awbno.getCa3dg() + awbno.getAwbStock().toString() + awbno.getAwbNo().toString();
					Long comId = bkg.getComId();
					Long annId = null;
					if (comId != null) {
						RSAnn ann = this.dbBookingManager.getCommodityAnnotationById(comId);
					}

					String scc3lc = bkg.getScc3lc();
					Integer pcs = bkg.getPcs();
					WgtVol wgtVol = new WgtVol(bkg.getWgt(), bkg.getVol());
					for (BookingFlight flight : flights) {
						if (flight != null && flight.getFltId() != null) {
							RSFltConxCA flig = this.dbBookingManager.getFlightById(flight.getFltId());

							Date date = flight.getBkgFltDate();
							Date d = flig.flight.dep;

							date.setHours(d.getHours());
							date.setMinutes(d.getMinutes());
							date.setSeconds(d.getSeconds());

							//String sFltId = flight.getFltId();
							Long sFltId = flight.getFltId();
							RCFlt cFlt2 = bkgDao.getCFlt(sFltId, date);
							if (cFlt2 != null) {
								boolean avail = bkgDao.isAvailable(sFltId, date, wgtVol);
								if (avail) {
									log.info("booking available");
									CentralBookingResult bkgId = bkgDao.makeBooking(sFltId, date, addId, awbNo, fid, comId, annId, scc3lc, pcs, wgtVol,flight.getCBkgId());
									if (bkgId != null) {
										log.info("booked with bkgID: " + bkgId);
										flight.setCBkgId(bkgId.getBkgId());
										return bkgId;
									} 
									else {
										log.warn("could not book ");
										flight.setCBkgId(null);
									}
								} 
								else {
									log.warn("booking for a flight not available -  maybe full");
								}
							} 
							else {
								log.error("flight is not set");
							}
						} 
						else {
							log.error("C-flight is not set");
						}
					} // foreach 
				} 
				else {
					log.error("awb no not set");
				}
			} 
			else {
				log.error("no flight available");
			}
		} 
		else {
			log.error("no bkg item");
		}
		return null;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 18.10.2006
	 * @return
	 * @return boolean
	 */
	public boolean cancel() {
		if (this.bkgItem == null) return false;
		PackageItemBKG bkg = this.bkgItem;
		if (bkg != null) {
			if (flights != null && flights.size() > 0) {
				Integer addId = this.bkgItem.getFAtt().getAddId();
				AwbNo awbno = this.info.getAwbNo();
				if (awbno != null) {
					String awbNo = awbno.getCa3dg() + awbno.getAwbStock().toString() + awbno.getAwbNo().toString();
					Long comId = bkg.getComId();
					Long annId = null;
					if (comId != null) {
						RSAnn ann = this.dbBookingManager.getCommodityAnnotationById(comId);

					}

					String scc3lc = bkg.getScc3lc();
					Integer pcs = bkg.getPcs();
					WgtVol wgtVol = new WgtVol(bkg.getWgt(), bkg.getVol());
					for (BookingFlight flight : flights) {

						String cbkgId = flight.getCBkgId();
						if (flight != null && flight.getFltId() != null && cbkgId != null) {
							Date date = flight.getBkgFltDate();
							Long sFltId = flight.getFltId();
							try {
								this.bkgDao.cancel(cbkgId);
								return true;
							} catch (RuntimeException e) {
								log.error(e.getMessage());
								return false;
							}
						} 
						else {
							log.error("flight is not set");
						}
					}
				} 
				else {
					log.error("awb no not set");
				}
			} 
			else {
				log.error("no flight available");
			}
		} 
		else {
			log.error("no bkg item");
		}
		return false;
	}
}
