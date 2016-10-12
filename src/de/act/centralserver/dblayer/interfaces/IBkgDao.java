package de.act.centralserver.dblayer.interfaces;

import java.util.Date;
import java.util.List;

import org.springframework.dao.DataAccessException;

import de.act.common.types.CentralBookingResult;
import de.act.common.types.WgtVol;
import de.act.common.types.nonstaticobjects.RCBkg;
import de.act.common.types.nonstaticobjects.RCFlt;

public interface IBkgDao {
	final static String STATUS_CANCEL      = "xxxx";
	final static String STATUS_CONFIRMED   = "xxkk";
	final static String STATUS_MANUAL      = "xxnn";
	final static String STATUS_WAIT_LIST   = "xxll";
	final static String STATUS_NO_CAPACITY = "xxuu";

	/**
	 * Get all booking entries for the given booking ID.
	 * The result is ordered descending by 'corr'.
	 *
	 * @param bkgId the ID of a previous successful booking.
	 *
	 * @return the history of booking entries with the latest enty first.
	 *
	 * @throws DataAccessException if a Hibernate error occurs.
	 */
	List<RCBkg> getBkgHistory(String bkgId) throws DataAccessException;

	/**
	 * Set the status of the booking request with the given ID.
	 * A new row is inserted into the c_bkg table with the given status.
	 *
	 * @param bkgId  the ID of a previous successful booking.
	 * @param status the new status of the booking request.
	 */
	void setStatus(String bkgId, String status);

	/**
	 * Cancel the booking request with the given ID.
	 * A new row is inserted into the c_bkg table with the status 'xx'.
	 *
	 * @param bkgId the ID of a previous successful booking.
	 */
	void cancel(String bkgId);

	/**
	 * Make a booking.
	 *
	 * @param sFltId the ID of the flight to book in the 's_flt' table.
	 * @param date   the date of the flight to book.
	 * @param addId  the address ID of the company that initiated the booking.
	 * @param awbNo  the Air Way Bill number.
	 * @param comId  the commodity ID of the goods to transport.
	 * @param annId  the annotation ID of the goods to transport.
	 * @param scc3lc the three letter code
	 * @param pcs    the number of pieces.
	 * @param wgtVol the weight and volume to book.
	 * @param cBkgID
	 *
	 * @return a booking result if the booking was successful and
	 *         <code>null</code> otherwise.
	 */
	CentralBookingResult makeBooking(Long sFltId, Date date, Integer addId,
			String awbNo, String fId, long comId, Long annId, String scc3lc,
			int pcs, WgtVol wgtVol, String cBkgID);

	/**
	 * Make a booking.
	 *
	 * @param preferredSFltId ID of the preferred flight.
	 * @param preferredDate   the preferred flight date.
	 * @param conxId          the ID of the connection to book.
	 * @param from            earliest possible date.
	 * @param to              last possible date.
	 * @param addId           the address ID of the company that initiated
	 *                        the booking.
	 * @param awbNo           the Air Way Bill number.
	 * @param comId           the commodity ID of the goods to transport.
	 * @param annId           the annotation ID of the goods to transport.
	 * @param scc3lc          the three letter code
	 * @param pcs             the number of pieces.
	 * @param wgtVol          the weight and volume to book.
	 * @param minStatus       the minimal status required for a successful
	 *                        booking.
	 * @param cBkgID TODO
	 * @return a booking result if the booking was successful and
	 *         <code>null</code> otherwise.
	 */
	CentralBookingResult makeAnyBooking(
			Long preferredSFltId, Date preferredDate,
			String conxId, Date from, Date to, Integer addId,
			String awbNo, String fId, long comId, Long annId, String scc3lc,
			int pcs, WgtVol wgtVol, String minStatus, String cBkgID);

	/**
	 * Get the weight and volume still available on the given flight.
	 *
	 * @param sFltId the ID of the flight in the 's_flt' table.
	 * @param date   the date of the flight.
	 *
	 * @return an object containing the weight and volume still available.
	 */
	WgtVol getAvailableWgtVol(Long sFltId, Date date);

	/**
	 * Check if the given weight and volume are still available
	 * on the given flight.
	 *
	 * @param sFltId the ID of the flight in the 's_flt' table.
	 * @param date   the date of the flight.
	 * @param wgtVol the weight and volume to check.
	 *
	 * @return <code>true</code> if the weight and volume are still available
	 *         and <code>false</code> otherwise.
	 */
	boolean isAvailable(Long sFltId, Date date, WgtVol wgtVol);

	/**
	 * Get a central flight simply by its primary key.
	 *
	 * @param cFltId primary key of the flight to get.
	 *
	 * @return the central flight with the given primary key.
	 */
	RCFlt getCFlt(Long cFltId);

	/**
	 * Get a central flight for the given static flight-ID and date.
	 *
	 * @param sFltId the static flight ID (s_flt_id) of the central flight to get.
	 * @param date   the departure date of the flight to get.
	 *
	 * @return a central flight for the given static flight-ID and date.
	 */
	RCFlt getCFlt(Long sFltId, Date date);

	/**
	 * Save the given central flight object to the database.
	 *
	 * @param cFlt the central flight to save (update).
	 */
	void setCFlt(RCFlt cFlt);
}