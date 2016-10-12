package de.act.centralserver.applayer.interfaces;

import java.util.List;

import de.act.central.types.Attachment;
import de.act.central.types.FormularInformation;
import de.act.centralserver.applayer.agent.ContentData;


/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 02.11.2006
 * @update 02.11.2009
 */
public interface IBookingManager {

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param contentData
	 * @return void
	 */
	boolean makeCentralBooking(ContentData contentData);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param ffrom
	 * @param att
	 * @return void
	 */
	boolean makeCentralBooking(FormularInformation ffrom, List<Attachment> att);

	/**
	 * @param addId TODO
	 * @param ffrom
	 * @param att
	 * @return
	 */
	boolean canBookCentral(String addId, FormularInformation ffrom, List<Attachment> att);

}
