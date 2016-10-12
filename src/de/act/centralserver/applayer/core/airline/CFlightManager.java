/**
 * 
 */
package de.act.centralserver.applayer.core.airline;

import de.act.central.types.CFlt;
import de.act.central.types.CentralBooking;
import de.act.central.types.CentralFlights;
import de.act.centralserver.applayer.interfaces.airline.IFlightManager;
import de.act.centralserver.dblayer.interfaces.airline.IDBFlightManager;

/**
 * @author Henry
 *
 */
public class CFlightManager implements IFlightManager{

	private IDBFlightManager dbFlightManager;

	public IDBFlightManager getDbFlightManager() {
		return dbFlightManager;
	}

	public void setDbFlightManager(IDBFlightManager dbFlightManager) {
		this.dbFlightManager = dbFlightManager;
	}
	
	public CentralFlights saveFlight(CentralFlights cFlt){
		return this.dbFlightManager.saveFlight(cFlt);
	}
	
	public CFlt saveFlight(CFlt cFlt){
		return this.dbFlightManager.saveFlight(cFlt);
	}
	
	public Integer saveBooking(CentralBooking cBkg){
		return this.dbFlightManager.saveBooking(cBkg);
	}
	
	public Integer unactivatingCBkg(CentralBooking bkg){
		return this.dbFlightManager.unactivatingCBkg(bkg);
	}
}
