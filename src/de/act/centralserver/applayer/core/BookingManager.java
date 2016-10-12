package de.act.centralserver.applayer.core;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import de.act.blackbox.dblayer.interfaces.airline.IDBScheduleManager;
import de.act.central.types.Attachment;
import de.act.central.types.BookingFlight;
import de.act.central.types.BookingFlightState;
import de.act.central.types.FormularInformation;
import de.act.central.types.PackageItemBKG;
import de.act.centralserver.applayer.agent.ContentData;
import de.act.centralserver.applayer.interfaces.IBookingManager;
import de.act.centralserver.dblayer.interfaces.IBkgDao;
import de.act.centralserver.dblayer.interfaces.IDBBookingManager;
import de.act.centralserver.dblayer.interfaces.IDBTransferManager;
import de.act.common.types.CentralBookingResult;
import de.act.common.types.formulars.FormularType;
import de.act.common.types.nonstaticobjects.BookingStates;
import de.act.common.types.nonstaticobjects.ErrorState;
import de.act.common.types.nonstaticobjects.ProcessStates;
import de.act.common.types.staticobjects.RSCaConx;
import de.act.common.types.staticobjects.RSFltConxCA;

/**
 * Manage the bookingrequest for ActernityBox or ActernityCentral
 * @author Martin Sachs & Henry
 * @since 1.0 - 30.01.2007
 * @update 02.11.2009
 */
public class BookingManager implements IBookingManager { //NOT WORKING ANYMORE, BOOKING IS NOT DONE BY CENTRAL ANYMORE
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(BookingManager.class);
	private IDBBookingManager   dbBookingManager;
	private IDBTransferManager  dbTransferManager;
	private IBkgDao             bkgDao;

	private IDBScheduleManager dbScheduleManager;

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param dbBookingManager
	 * @param dbTransferManager
	 * @param bkgDao
	 */
	public BookingManager(IDBBookingManager dbBookingManager, IDBTransferManager dbTransferManager, IBkgDao bkgDao) {
		super();
		this.dbBookingManager = dbBookingManager;
		this.dbTransferManager = dbTransferManager;
		this.bkgDao = bkgDao;
	}

	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @return Returns the dbBookingManager.
	 */
	public IDBBookingManager getDbBookingManager() {
		return dbBookingManager;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param dbBookingManager
	 *             The dbBookingManager to set.
	 */
	public void setDbBookingManager(IDBBookingManager dbBookingManager) {
		this.dbBookingManager = dbBookingManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.dblayer.IDBBookingManager#makeCentralBooking(de.act.central.applayer.ContentData)
	 */
	public boolean makeCentralBooking(ContentData contentData) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.IBookingManager#makeCentralBooking(de.act.central.types.FormularInformation, java.util.List)
	 */
	public boolean makeCentralBooking(FormularInformation formularInfo, List<Attachment> listAtt) {
		Attachment bkgFlightsAtt = null;
		Attachment pkgAtt = null;


		listAtt = this.dbTransferManager.loadData(formularInfo);
		short bkgAttNo = -1;
		short pkgAttNo = -1;
		// try booking
		for(Attachment a:listAtt){
			if (a.getAttType().equals(FormularType.BKG_FLIGHT.getConstaint())){
				Short attNo = a.getAttNo();
				if (attNo > bkgAttNo){
					bkgFlightsAtt = a;
					bkgAttNo = attNo;
				}
			}
			else if (a.getAttType().equals(FormularType.PACKAGE.getConstaint())){
				//int d;
				Short attNo = a.getAttNo();
				if (attNo > pkgAttNo){
					pkgAtt = a;
					pkgAttNo = attNo;
				}
			}
		}
		if (bkgFlightsAtt != null && pkgAtt != null){
			Set<BookingFlight> flights = bkgFlightsAtt.getFBkgFlts();
			Set<BookingFlightState> flightStates1 = bkgFlightsAtt.getFBkgFltStats();
			Set<BookingFlightState> flightStates = new LinkedHashSet<BookingFlightState>();
			for(BookingFlightState b:flightStates1){
				if (b.getBkgStatus().startsWith("xa") || formularInfo.anyBooking()) {
					flightStates.add(b);
				} 
			}
			Set<PackageItemBKG> bkgItems = pkgAtt.getFBkgItems();
			if (bkgItems != null && bkgItems.size() > 0){
				PackageItemBKG bkgItem = bkgItems.iterator().next();
				String state = bkgItem.getReqStat();
				log.info("requested State: "+state);
				log.info("state in bkgFltStat: "+flightStates);
				if (state == null) return false;
				BookingStates bks = BookingStates.getStateForActCode(state);
				//String fname = formularInfo.getFormularName();
				ProcessStates ps = ProcessStates.getProcessState(state);
				if (bks != null && flightStates!=null && flightStates.size()>0 &&
						(ps == ProcessStates.BKG || 
								bks == BookingStates.AGENT_REQUEST ||
								bks == BookingStates.AGENT_ALTERNATIVE_REQUEST || 
								bks == BookingStates.AGENT_CANCEL)){
					// do the bookingaction
					switch(bks){
					case AGENT_REQUEST:
					case AGENT_ALTERNATIVE_REQUEST: {
						doBooking(formularInfo, bkgItem,bkgFlightsAtt,flights, flightStates);
						break;
					}
					case AGENT_CANCEL: {
						log.info("booking canceled by agent");
						doBookingCancelation(formularInfo, bkgItem, bkgFlightsAtt,flights, flightStates);
						break;
					}
					default: return false;
					}
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param formularInfo
	 * @param bkgItem
	 * @param bkgFlightsAtt 
	 * @param flights
	 * @param flightStates
	 * @return void
	 */
	private void doBookingCancelation(FormularInformation formularInfo, PackageItemBKG bkgItem, Attachment bkgFlightsAtt, Set<BookingFlight> flights, Set<BookingFlightState> flightStates) {
		log.info("try to cancel a flight flight");
		bkgFlightsAtt.setNewId();
		BookingWorker d = new BookingWorker(formularInfo, bkgItem, flights, flightStates, this.dbBookingManager, this.bkgDao);
		try{
			boolean bkgId = d.cancel();
			if (bkgId){
				String st = bkgItem.getReqStat();
				//BookingStates state = BookingStates.getStateForActCode(st);
				for(BookingFlightState f:flightStates){
					f.setBkgStatus(st);
				}
				formularInfo.setWorkState(ErrorState.OK.getError());
				st = BookingStates.BOOKING_CANCELED.getActCode();
				for(BookingFlightState f:flightStates){
					f.setBkgStatus(st);
				}
				formularInfo.setCaId(st);
			}
			else{
				log.warn("Unable to cancel boooking");
				formularInfo.setWorkState(ErrorState.ERROR.getError());
				//			 formularInfo.setCaId(st);
			}
			this.dbBookingManager.storeFormularInformations(formularInfo);
			this.dbBookingManager.storeBookingData(flights, flightStates);
		}
		catch(RuntimeException e1){
			log.error("runtimeexception " + e1.getMessage(), e1);
		}
	}

	/**
	 * @author Martin Sachs
	 * @param form
	 *             TODO
	 * @param bkgItem
	 * @param bkgFlightsAtt 
	 * @param flights
	 * @param flightStates
	 * @since 1.0 - 20.11.2006
	 * @return void
	 */
	private void doBooking(FormularInformation info, PackageItemBKG bkgItem, Attachment bkgFlightsAtt, Set<BookingFlight> flights, Set<BookingFlightState> flightStates) {
		try{
			BookingWorker d = new BookingWorker(info, bkgItem, flights, flightStates, this.dbBookingManager, this.bkgDao);
			//  set id to null, and insert a new row. Att_no is increased
			bkgFlightsAtt.setNewId();
			log.info("book on central system");
			// try to book with the BookingWorker 
			CentralBookingResult bkgId = d.book();
			if (bkgId != null){
				if (bkgItem != null){
					String st = bkgItem.getReqStat();
					//String fltId = "00";
					Long fltId = 0l;
					for(BookingFlightState f:flightStates){
						f.setBkgStatus(st);
					}
					info.setWorkState(ErrorState.OK.getError());
					if (flightStates != null && flightStates.size() > 0){
						fltId = flightStates.iterator().next().getFBkgFlt().getFltId();
						String actCode = bkgId.getStatus();
						for(BookingFlightState f:flightStates){
							f.setBkgStatus(actCode);
							f.setBkgFltStatId(null); // create new flightrows
							BookingFlight bkgFlt = f.getFBkgFlt();
							bkgFlt.setBkgFltId(null);
							bkgFlt.setCBkgId(bkgId.getBkgId());
							Attachment bkgFltAtt = bkgFlt.getFAtt();
							bkgFltAtt.setAttId(null);
							Short attn = bkgFltAtt.getAttNo();
							attn++;
							bkgFltAtt.setAttNo(attn);
							f.getFAtt().setAttId(null);
							Short attNo = f.getFAtt().getAttNo();
							attNo++;
							f.getFAtt().setAttNo(attNo);
							f.getFAtt().setAuth("s");
						}
						RSFltConxCA fltconxca = this.dbBookingManager.getFlightById(fltId);
						//RSCaConx ca = fltconxca.conx.carrier;
						RSCaConx ca = this.dbScheduleManager.getCarrierById(fltconxca.conx.ca_id);
						if (actCode.equals(BookingStates.BOOKING_CONFIRMED.getActCode())) {
							info.setCaId(ca.ca_id);
						}else {
							info.setWorkState(ErrorState.ERROR.getError());
							//info.setCaId(actCode);
						}
						log.info("booking confirmed with "+info.getCaId());
						this.dbBookingManager.storeFormularInformations(info);
						this.dbBookingManager.storeBookingData(flights, flightStates);
					}
					else{}
				}
			}
			else{
				log.warn("Unable to confirm boooking");
				String actCode = BookingStates.UTC_NO_CAPACITY.getActCode();
				for(BookingFlightState f:flightStates){
					f.setBkgStatus(actCode);
					f.setBkgFltStatId(null); // create new flightrows
					BookingFlight bkgFlt = f.getFBkgFlt();
					bkgFlt.setBkgFltId(null);
					Attachment bkgFltAtt = bkgFlt.getFAtt();
					bkgFltAtt.setAttId(null);
					Short attn = bkgFltAtt.getAttNo();
					attn++;
					bkgFltAtt.setAttNo(attn);
					f.getFAtt().setAttId(null);
					Short attNo = f.getFAtt().getAttNo();
					attNo++;
					f.getFAtt().setAttNo(attNo);
					f.getFAtt().setAuth("s");

				}
				info.setWorkState(ErrorState.ERROR.getError());
				//info.setCaId(actCode);
				this.dbBookingManager.storeFormularInformations(info);
				this.dbBookingManager.storeBookingData(flights, flightStates);
			}
		}
		catch(RuntimeException e1){
			log.error("runtimeexception " + e1.getMessage(), e1);
		}
	}

	/* (non-Javadoc)
	 * @see de.act.central.applayer.interfaces.IBookingManager#canBookCentral(de.act.central.types.FormularInformation, java.util.List)
	 */
	public boolean canBookCentral(String addId, FormularInformation ffrom, List<Attachment> listAtt) {

		// only miat first
		Attachment bkgFlightsAtt = null;
		Attachment pkgAtt = null;
		listAtt = this.dbTransferManager.loadData(ffrom);
		short bkgAttNo = -1;
		short pkgAttNo = -1;
		// try booking
		for(Attachment a:listAtt){
			if (a.getAttType().equals(FormularType.BKG_FLIGHT.getConstaint())){
				Short attNo = a.getAttNo();
				if (attNo > bkgAttNo){
					bkgFlightsAtt = a;
					bkgAttNo = attNo;
				}
			}
			else if (a.getAttType().equals(FormularType.PACKAGE.getConstaint())){
				//int d;
				Short attNo = a.getAttNo();
				if (attNo > pkgAttNo){
					pkgAtt = a;
					pkgAttNo = attNo;
				}
			}
		}
		if (bkgFlightsAtt != null && pkgAtt != null){
			Set<BookingFlight> flights = bkgFlightsAtt.getFBkgFlts();
			if (flights!=null && flights.size()>0) {
				Long fltID = flights.iterator().next().getFltId();
				RSFltConxCA flt = this.dbBookingManager.getFlightById(fltID);
				if (flt!=null && flt.carrier.ca_2lc.equals("OM")) {
					return false;
				}
			}
		}

		return true;
	}
}
