package de.act.centralserver.dblayer.interfaces;

import java.util.Set;

import de.act.central.types.BookingFlight;
import de.act.central.types.BookingFlightState;
import de.act.central.types.FormularInformation;
import de.act.common.types.staticobjects.RSAnn;
import de.act.common.types.staticobjects.RSFltConxCA;


/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 02.11.2006
 * @update 02.11.2009
 */
public interface IDBBookingManager {

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param dbf
	 * @return
	 * @return FormularInformation
	 */
	FormularInformation storeFormularInformations(FormularInformation dbf);


	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param comId
	 * @return
	 * @return RSAnn
	 */
	RSAnn getCommodityAnnotationById(Long comId);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param fltId
	 * @return
	 * @return RSFltConxCA
	 */
	RSFltConxCA getFlightById(Long fltId);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param flights
	 * @param flightStates
	 * @return void
	 */
	void storeBookingData(Set<BookingFlight> flights, Set<BookingFlightState> flightStates);
}
