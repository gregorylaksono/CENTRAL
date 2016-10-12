package de.act.centralserver.applayer.core;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import de.act.central.types.Acceptance;
import de.act.central.types.AcceptanceItem;
import de.act.central.types.Attachment;
import de.act.central.types.FormularInformation;
import de.act.central.types.Manifest;
import de.act.central.types.ManifestItem;
import de.act.central.types.Uld;
import de.act.centralserver.applayer.agent.ContentData;
import de.act.centralserver.applayer.interfaces.ITransferManager;
import de.act.centralserver.dblayer.interfaces.IDBTransferManager;
import de.act.common.types.handling.AManifestData;
import de.act.common.types.nonstaticobjects.ProcessStates;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.staticobjects.ICommodityTree;
import de.act.common.types.staticobjects.RSAdd;
import de.act.common.types.staticobjects.RSAp;
import de.act.common.types.staticobjects.RSFltConxCA;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 01.11.2006
 * @update 02.11.2009
 */
public class TransferManager implements ITransferManager {
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(TransferManager.class);
	private IDBTransferManager  dbTransferManager;

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param dbTransferManager
	 */
	public TransferManager(IDBTransferManager dbTransferManager) {
		super();
		this.dbTransferManager = dbTransferManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#getConsigneeImportAgent(java.lang.String)
	 */
	public Integer getConsigneeImportAgent(Integer con) {
		log.debug("getConsigneeImportAgent(" + con + ")");
		try{
			return this.dbTransferManager.getConsigneeImportAgent(con);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#getFormularInformation(java.lang.String)
	 */
	public RFForm getFormularInformation(String id) {
		log.debug("getFormularInformation(" + id + ")");
		try{
			return this.dbTransferManager.getFormularInformation(id);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#getHandlingAddressByAirport(java.lang.Long, java.lang.String)
	 */
	public Integer getHandlingAddressByFlight(String bkgFltId, String string) {
		log.debug("getHandlingAddressByAirport(" + bkgFltId + "," + string + ")");
		try{
			return this.dbTransferManager.getHandlingAddressByFlight(bkgFltId, string);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#getHandlingAddressByAirport(java.lang.String, java.lang.String)
	 */
	public Integer getHandlingAddressByAirport(String ap, String string) {
		log.debug("getHandlingAddressByAirport(" + ap + "," + string + ")");
		try{
			return this.dbTransferManager.getHandlingAddressByAirport(ap, string);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#loadData(de.act.common.types.nonstaticobjects.RFForm)
	 */
	public ContentData loadData(RFForm content) {
		log.debug("loadData(" + content + ")");
		try{
			return this.dbTransferManager.loadData(content);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#storeData(de.act.central.types.FormularInformation, java.util.List)
	 */
	public synchronized boolean storeData(FormularInformation formularInfo, List<Attachment> listAtt) {
		log.debug("storeData(" + formularInfo + ")");
		try{
			boolean ret = this.dbTransferManager.storeData(formularInfo, listAtt);
			return ret;
		}
		catch(RuntimeException e){
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#storeData(de.act.central.types.Manifest)
	 */
	public boolean storeData(Manifest mani) {
		log.debug("storeData(" + mani + ")");
		try{
			if (mani == null) return false;
			Date d = new Date();
			String flightId = mani.getFltId();
			//int k = 0;
			// get flight id and destination aiport
			Integer address = this.dbTransferManager.getHandlingAddressByFlight(flightId, "i");
			RSFltConxCA conx = this.dbTransferManager.getFlightConx(flightId);
			if (conx == null || conx.conx == null || conx.conx.dest == null) return false;
			RSAp dest = conx.conx.dest;
			mani.setManiId(null);
			// create new Manifest with new keys
			mani.setUserAddId(address);
			mani.setCorr(d);
			Set<ManifestItem> maniitems = mani.getAManiItems();
			if (maniitems != null && maniitems.size() > 0){
				for(ManifestItem item:maniitems){
					if (item == null) continue;
					item.setManiItemId(null);

					// assign the uld to a new address
					Uld uldObject = item.getAUld();
					if (uldObject != null){
						uldObject.setUldId(null);
						uldObject.setHolderAddId(address);
					}

					// check if last airport
					Set<AcceptanceItem> shps = item.getShpItems();
					if (shps != null && shps.size() > 0) {
						for(AcceptanceItem shp:shps){
							if (shp != null){
								shp.setAccItemId(null);
								Acceptance acc = shp.getFAcc();
								if (acc != null){
									acc.setStatId(null);
									Attachment meta = acc.getFAtt();
									if (meta != null){
										meta.setNewCreatorAndId(address, dest.ap_3lc);
										if (acc != null && acc.getApDest() != null && mani != null && flightId != null && acc.getApDest().equals(dest.ap_3lc)){
											String fid = meta.getFid();
											FormularInformation form = this.dbTransferManager.loadFormularInfo(fid);
											form.setFormularName(ProcessStates.AWB_RECIEVED_AT_LASTAIRPORT.getFormularName());
											this.dbTransferManager.storeFormularInformations(form);
										}
									}
								}
							}
						}
					}
				}
			}
			boolean ret = this.dbTransferManager.storeData(mani);
			return ret;
		}
		catch(RuntimeException e){
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#storeFormularInformations(de.act.central.types.FormularInformation)
	 */
	public FormularInformation storeFormularInformations(FormularInformation dbf) {
		log.debug("storeFormularInformations(" + dbf + ")");
		try{
			return this.dbTransferManager.storeFormularInformations(dbf);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#loadAllData(de.act.central.applayer.agent.ContentData)
	 */
	public ContentData loadAllData(ContentData contentData) {
		log.debug("loadData(" + contentData + ")");
		try{
			return this.dbTransferManager.loadAllData(contentData);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#loadData(de.act.common.types.handling.AManifestData)
	 */
	public ContentData loadData(AManifestData manifest) {
		log.debug("loadData(" + manifest + ")");
		try{
			return this.dbTransferManager.loadData(manifest);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#loadManifestData(de.act.central.types.Manifest)
	 */
	public AManifestData loadManifestData(Manifest manifest) {
		log.debug("loadManifestData(" + manifest + ")");
		try{
			return this.dbTransferManager.loadData(manifest);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.act.central.applayer.interfaces.ITransferManager#reLoadData(de.act.central.types.FormularInformation)
	 */
	public ContentData reLoadData(FormularInformation ffrom, List<Attachment> att) {
		log.debug("reLoadData(" + ffrom + ")");
		try{
			return this.dbTransferManager.reLoadData(ffrom, att);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see de.act.central.applayer.interfaces.ITransferManager#getAddressBy(java.lang.String)
	 */
	public RSAdd getAddressBy(String addId) {
		log.debug("getAddressBy(" + addId + ")");
		try{
			return this.dbTransferManager.getAddressBy(addId);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see de.act.central.applayer.interfaces.ITransferManager#storeData(de.act.common.types.staticobjects.RSAdd)
	 */
	public Boolean storeData(RSAdd address) {
		log.debug("storeData(Address:" + address + ")");
		try{
			// checking data
			// is id set
			// is name set

			return this.dbTransferManager.storeData(address);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}      
		return null;
	}

	/* (non-Javadoc)
	 * @see de.act.central.applayer.interfaces.ITransferManager#storeData(de.act.common.types.staticobjects.ICommodityTree)
	 */
	public Boolean storeData(ICommodityTree com) {
		log.debug("storeData(Commodity:" + com + ")");
		try{

			// checking data
			// is id set
			// is name set
			// is parent set

			return this.dbTransferManager.storeData(com);
		}
		catch(RuntimeException e){
			log.error(e.getMessage());
		}
		return null;
	}
}
