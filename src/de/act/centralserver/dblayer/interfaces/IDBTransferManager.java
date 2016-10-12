package de.act.centralserver.dblayer.interfaces;


import java.util.List;

import de.act.central.types.Attachment;
import de.act.central.types.FormularInformation;
import de.act.central.types.Manifest;
import de.act.centralserver.applayer.agent.ContentData;
import de.act.common.types.handling.AManifestData;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.staticobjects.ICommodityTree;
import de.act.common.types.staticobjects.RSAdd;
import de.act.common.types.staticobjects.RSFltConxCA;

/**
 * Transfermanager manages the loading and storing of all dataContents in the central server
 * and acternity boxes.
 * @author Martin Sachs
 * @since 1.0 - 02.11.2006
 */
public interface IDBTransferManager {

	/**
	 * load the data by f_id -
	 * this function loads only the data, which have changed.
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param content
	 * @return ContentData
	 */
	ContentData loadData(RFForm content);

	/**
	 * Store all changed data to the database
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param formularInfo
	 * @param listAtt TODO
	 * @return void
	 */
	boolean storeData(FormularInformation formularInfo, List<Attachment> listAtt);

	/**
	 * Store manifestdata to the Database.
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param manifest
	 * @return void
	 */
	boolean storeData(Manifest manifest);

	/**
	 * load the manifesttables with all attached datas
	 * @author Martin Sachs
	 * @since 1.0 - 07.11.2006
	 * @param manifest
	 * @return AManifestData
	 */
	AManifestData loadData(Manifest manifest);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param dbf
	 * @return
	 * @return FormularInformation
	 */
	FormularInformation storeFormularInformations(FormularInformation dbf);

	/**
	 * get addressId for a concret bkgFltId
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param bkgFltId
	 * @param service
	 * @return RSAdd
	 */
	Integer getHandlingAddressByFlight(String bkgFltId, String service);

	/**
	 * get addressId for a concret airport-3lc
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param ap
	 * @param service - i for import, e for export, a for all
	 * @return RSAdd
	 */
	Integer getHandlingAddressByAirport(String ap, String service);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param con
	 * @return
	 * @return String
	 */
	Integer getConsigneeImportAgent(Integer con);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param id
	 * @return
	 * @return RFForm
	 */
	RFForm getFormularInformation(String id);

	/**
	 * Load all assoziated datas per FId. 
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param formularInfo
	 * @return List<Attachment> -  list of the hole process data
	 */
	List<Attachment> loadData(FormularInformation formularInfo);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param contentData
	 * @return
	 * @return ContentData
	 */
	ContentData loadAllData(ContentData contentData);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 21.11.2006
	 * @param flightId
	 * @return
	 * @return RSFltConxCA
	 */
	RSFltConxCA getFlightConx(String flightId);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 21.11.2006
	 * @param fid
	 * @return
	 * @return FormularInformation
	 */
	FormularInformation loadFormularInfo(String fid);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 23.11.2006
	 * @param manifest
	 * @return
	 * @return ContentData
	 */
	ContentData loadData(AManifestData manifest);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 05.12.2006
	 * @param ffrom
	 * @param att TODO
	 * @return
	 * @return ContentData
	 */
	ContentData reLoadData(FormularInformation ffrom, List<Attachment> att);

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 26.01.2007
	 * @param addId
	 * @return
	 * @return RSAdd
	 */
	RSAdd getAddressBy(String addId);

	/**
	 * @param address
	 * @return
	 */
	Boolean storeData(RSAdd address);

	/**
	 * @param com
	 * @return
	 */
	Boolean storeData(ICommodityTree com);

}
