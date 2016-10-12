package de.act.centralserver.applayer.interfaces;


import java.util.List;

import de.act.central.types.Attachment;
import de.act.central.types.FormularInformation;
import de.act.central.types.Manifest;
import de.act.centralserver.applayer.agent.ContentData;
import de.act.common.types.handling.AManifestData;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.staticobjects.ICommodityTree;
import de.act.common.types.staticobjects.RSAdd;

/**
 * Transfermanager manages the loading and storing of all dataContents in the central server
 * and acternity boxes.
 * @author Martin Sachs & Henry
 * @since 1.0 - 02.11.2006
 * @update 02.11.2009
 */
public interface ITransferManager {

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
	 * Store a changed FormularInformation-Object. Save the last state of the 
	 * hole process (F_id)
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param dbf
	 * @return FormularInformation instance if succeed, otherwise null
	 */
	FormularInformation storeFormularInformations(FormularInformation dbf);

	/**
	 * get addressId for a concret bkgFltId
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param bkgFltId
	 * @param string
	 * @return RSAdd
	 */
	Integer getHandlingAddressByFlight(String bkgFltId, String string);

	/**
	 * get addressId for a concret airport-3lc
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param ap
	 * @param string
	 * @return RSAdd
	 */
	Integer getHandlingAddressByAirport(String ap, String string);

	/**
	 * get the Import Agents, specified at AddressObject of the Consignee.
	 * @author Martin Sachs
	 * @since 1.0 - 15.11.2006
	 * @param con
	 * @return
	 * @return String
	 */
	Integer getConsigneeImportAgent(Integer con);

	/**
	 * get the ActernityBox FormularInformation-Object
	 * @author Martin Sachs
	 * @since 1.0 - 17.11.2006
	 * @param id
	 * @return RFForm
	 */
	RFForm getFormularInformation(String id);

	/**
	 * load all Data associated to and old dataobject. 
	 * Use this method to reload changes from the database
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param contentData
	 * @return ContentData
	 */
	ContentData loadAllData(ContentData contentData);

	/**
	 * load manifestcontent from ABox manifest-Object
	 * @author Martin Sachs
	 * @since 1.0 - 21.11.2006
	 * @param manifest
	 * @return ContentData
	 */
	ContentData loadData(AManifestData manifest);

	/**
	 * load ABox Manifest-Object from ACentral-Object
	 * @author Martin Sachs
	 * @param mani 
	 * @since 1.0 - 21.11.2006
	 * @return AManifestData
	 */
	AManifestData loadManifestData(Manifest mani);

	/**
	 * reload all changed informations. Changed informations are marked in DB with att_auth <> 'r'
	 * this method stores att_auth = 'r' after loading the datas 
	 * @author Martin Sachs
	 * @since 1.0 - 05.12.2006
	 * @param ffrom
	 * @param att 
	 * @return ContentData
	 */
	ContentData reLoadData(FormularInformation ffrom, List<Attachment> att);

	/**
	 * get the AddressObject by AddId
	 * @author Martin Sachs
	 * @since 1.0 - 26.01.2007
	 * @param addId
	 * @return RSAdd
	 */
	RSAdd getAddressBy(String addId);

	/**
	 * store Address data
	 * @param address
	 */
	Boolean storeData(RSAdd address);

	/**
	 * store Commodity data
	 * @param com
	 */
	Boolean storeData(ICommodityTree com);

}
