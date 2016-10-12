/**
 * 
 */
package de.act.centralserver.dblayer.interfaces.airline;

import java.util.Date;

import de.act.central.types.CFlt;
import de.act.central.types.CentralBooking;
import de.act.central.types.CentralFlights;
import de.act.common.types.staticobjects.RSAc;
import de.act.common.types.staticobjects.RSFlt;

/**
 * @author Henry
 *
 */
public interface IDBFlightManager {

	public CentralFlights saveFlight(CentralFlights cFlt);
	public CFlt saveFlight(CFlt cFlt);
	public Integer saveBooking(CentralBooking cBkg);
	public Integer unactivatingCBkg(CentralBooking bkg);
	public RSFlt getFligt(Long flt_id, Date flt_date);
	public RSAc getSac(Long ac_id);
}
