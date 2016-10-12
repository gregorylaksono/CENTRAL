/**
 * 
 */
package de.act.centralserver.applayer.interfaces.airline;

import de.act.central.types.CFlt;
import de.act.central.types.CentralBooking;
import de.act.central.types.CentralFlights;

/**
 * @author Henry
 *
 */
public interface IFlightManager {

	public CentralFlights saveFlight(CentralFlights cFlt);
	public CFlt saveFlight(CFlt cFlt);
	public Integer saveBooking(CentralBooking cBkg);
	public Integer unactivatingCBkg(CentralBooking bkg);
}
