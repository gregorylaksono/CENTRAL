/**
 * 
 */
package de.act.centralserver.applayer.gateway;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import de.act.blackbox.applayer.interfaces.IChargesManager;
import de.act.blackbox.applayer.interfaces.IFormularManager;
import de.act.blackbox.applayer.interfaces.IHandlingAgentService;
import de.act.blackbox.applayer.interfaces.IManifestService;
import de.act.blackbox.applayer.interfaces.IRateManager;
import de.act.blackbox.applayer.interfaces.ISccHandlingManager;
import de.act.blackbox.applayer.interfaces.IStaticDataService;
import de.act.blackbox.applayer.interfaces.ITransferManager;
import de.act.blackbox.applayer.interfaces.IUldService;
import de.act.blackbox.applayer.interfaces.airline.IAircraftManager;
import de.act.blackbox.applayer.interfaces.airline.IAirlineManager;
import de.act.blackbox.applayer.interfaces.airline.INetRatesManager;
import de.act.blackbox.applayer.interfaces.airline.IPreferenceManager;
import de.act.blackbox.applayer.interfaces.airline.IScheduleManager;
import de.act.blackbox.applayer.interfaces.usermodule.ICommodityManager;
import de.act.blackbox.applayer.interfaces.usermodule.IUserPreferenceManager;
import de.act.blackbox.dblayer.interfaces.IDBManifestService;
import de.act.central.types.BookingFlight;
import de.act.central.types.PackageItemBKG;
import de.act.common.interfaces.IActernityGatewayServer;
import de.act.common.jmstypes.SerializableObject;
import de.act.common.jmstypes.beanwrapper.JMSAircraftObject;
import de.act.common.jmstypes.beanwrapper.JMSAirlineAreaObject;
import de.act.common.jmstypes.beanwrapper.JMSAirlineScc;
import de.act.common.jmstypes.beanwrapper.JMSAwbNo;
import de.act.common.jmstypes.beanwrapper.JMSManifest;
import de.act.common.jmstypes.beanwrapper.JMSNetRatesObject;
import de.act.common.jmstypes.beanwrapper.JMSNotoc;
import de.act.common.jmstypes.beanwrapper.JMSObjectWrapper;
import de.act.common.jmstypes.beanwrapper.JMSPreAdviceRespond;
import de.act.common.jmstypes.beanwrapper.JMSPreferenceAddressObject;
import de.act.common.jmstypes.beanwrapper.JMSProvisionsObject;
import de.act.common.jmstypes.beanwrapper.JMSSCCHandling;
import de.act.common.jmstypes.beanwrapper.JMSScheduleObject;
import de.act.common.types.agent.ContentData;
import de.act.common.types.handling.AManifest;
import de.act.common.types.handling.AManifestItem;
import de.act.common.types.nonstaticobjects.RCBkg;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.nonstaticobjects.ULDObject;
import de.act.common.types.packageitems.ItemHandlingBKG;
import de.act.common.types.staticobjects.CCommodityTree;
import de.act.common.types.staticobjects.Classes;
import de.act.common.types.staticobjects.RSAddBlackboxID;
import de.act.common.types.staticobjects.RSAddID;
import de.act.common.types.staticobjects.RSAnn;
import de.act.common.types.staticobjects.RSCharges;
import de.act.common.types.staticobjects.RSDbmWs;
import de.act.common.types.staticobjects.RSFlt;
import de.act.common.types.staticobjects.RSFuelChar;
import de.act.common.types.staticobjects.RSScc;
import de.act.common.types.staticobjects.SccPreference;
import de.act.common.types.staticobjects.ZHandlingAirport;

/**
 * @author Henry
 *
 */
@SuppressWarnings("unused")
public class SArea implements MessageListener{
	
	private static final String LOADING_LIST = "mmld";
	private static final String MANIFEST_LIST = "mmmn";
	private static final String ONBOARD_LIST = "mmvo";
	private static final String PREMANIFEST_LIST = "mmpr";
	
	private IActernityGatewayServer jmsGateway;
	private IStaticDataService staticDataService;
	private IAircraftManager aircraftManager;
	private IScheduleManager scheduleManager;
	private INetRatesManager netRatesManager;
	private IPreferenceManager preferenceManager;
	private IUserPreferenceManager preferenceUserManager;
	private IDBManifestService dbManifestService;
	private IManifestService manifestService;
	private IUldService uldService;
	private ITransferManager transferManager;
	
	private IAirlineManager airlineManager;
	private ISccHandlingManager sccHandlingManager;
	private IHandlingAgentService handlingAgentService;
	private ICommodityManager commodityManager;
	private IRateManager rateManager;
	private IChargesManager chargesManager;
	private IFormularManager formularManager;
	
	public void onMessage(Message message){
		try {
			if (message instanceof ObjectMessage){
				ObjectMessage objMsg = (ObjectMessage) message;
				Serializable obj = objMsg.getObject();
				if (obj instanceof SerializableObject){
					SerializableObject serializableObject = (SerializableObject) obj;
					if(serializableObject.getObjectTemplate() instanceof JMSAircraftObject){
						JMSAircraftObject data = (JMSAircraftObject) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("insert")){
								aircraftManager.saveAircraftandCompartments(data.getAc(), data.getRac(), data.getAddId());
							}
							else if(data.getState().equals("update")){
								aircraftManager.updateAircraftandCompartments(data.getAc(), data.getRac(), data.getAddId());
							}
							else if(data.getState().equals("delete")){
								aircraftManager.deleteAircraft(data.getAc());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSScheduleObject){
						JMSScheduleObject data = (JMSScheduleObject) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("insert")){
								scheduleManager.insertSchedules(data.getLrf(), data.getAddId());
							}
							else if(data.getState().equals("update")){
								scheduleManager.updateSchedules(data.getLrf(), data.getFlt_group(), false, data.getAddId());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSNetRatesObject){
						JMSNetRatesObject data = (JMSNetRatesObject) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("insertAutoRate")){
								netRatesManager.saveAutoRate(data.getRa(), data.getRaa());
							}
							else if(data.getState().equals("insertNetRate")){
								netRatesManager.saveNetRates(data.getLra());
							}
							else if(data.getState().equals("updateAutoRate")){
								netRatesManager.updateAutoRate(data.getConx_id(), data.getRa(), data.getRaa());
							}
							else if(data.getState().equals("updateNetRate")){
								netRatesManager.updateNetRates(data.getConx_id(), data.getLra());
							}
							else if(data.getState().equals("deleteRate")){
								netRatesManager.deleteRates(data.getConx_id(), data.getDate());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSProvisionsObject){
						JMSProvisionsObject data = (JMSProvisionsObject) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("insert")){
								netRatesManager.saveProvisions(data.getProvisions());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSPreferenceAddressObject){
						JMSPreferenceAddressObject data = (JMSPreferenceAddressObject) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("saveAgentAddress")){
								HashMap<Boolean, Integer> saved = preferenceManager.saveAgentAddress(data.getSadd(), false);
								Set<Boolean> key = saved.keySet();
								Iterator<Boolean> keyIterator = key.iterator();
								while(keyIterator.hasNext()){
									Boolean keyContain = (Boolean) keyIterator.next();
									if(keyContain == true){
										data.getUserObject().setAddId(saved.get(keyContain));
										preferenceUserManager.saveUser(data.getUserObject(), data.getListUserAuth());
									}
								}
								saveAddBlackboxID(data);
							}
							else if(data.getState().equals("deleteAddress")){
								preferenceManager.deleteAddress(data.getAdd_id());
							}
							else if(data.getState().equals("saveAddress")){
								preferenceManager.saveAddress(data.getSadd(), false, false);
								saveAddBlackboxID(data);
							}
							else if(data.getState().equals("updateAddressParent")){
								preferenceManager.updateAddressParent(data.getSadd());
							}
							else if(data.getState().equals("updateAddressBranch")){
								preferenceManager.updateAddressBranch(data.getSadd());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSAirlineAreaObject){
						JMSAirlineAreaObject data = (JMSAirlineAreaObject) serializableObject.getObjectTemplate();
						Integer id_db = this.staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("move2ULDOrBulk")){
								RFForm form = new RFForm();
								form.setF_id(data.getFID());
								ContentData contentData = transferManager.loadData(form);
								if(contentData != null){
									AManifest mani = getOrCreateManifest(data.getFltID(), data.getFltDate(), data.getAddID());
									List<AManifestItem> maniItemCentral = getManifestItem(mani.getManifestItems(), contentData.getSetBookingItem());
									AManifestItem newManiItem = data.getNewManifestItem();
									if(newManiItem != null){
										newManiItem.setId(null);
										newManiItem.setMani_id(mani);
										
										//HENRY, PARTSHIPMENT PROBLEM
										Integer highestSimilarity = 0;
										for(BookingFlight bkgItem : contentData.getLastFBkgFlts()){
											PackageItemBKG bookingItem = bkgItem.getFBkgItem();
											 Integer similarity = compare(bookingItem, newManiItem.getBooking());
											 if(highestSimilarity < similarity){
												 ItemHandlingBKG itemBKG = new ItemHandlingBKG();
												 itemBKG.setId(bookingItem.getBkgItemId());
												 newManiItem.setBooking(itemBKG);
											 }
										}
										Integer statusSave = dbManifestService.saveManiItem(newManiItem) == true ? 0 : 1;
										if(statusSave == 0){
											ULDObject uld = uldService.getUldById(newManiItem.getLl_uld().uld_id);
											uld.isUsed = 1;
											this.uldService.updateAULD(uld);
										}
									}
									if(data.getOldManifestItem() != null){
										for(AManifestItem centralOldManiItem : maniItemCentral){
											centralOldManiItem.setIs_active(0);
											Integer statusUpdate = dbManifestService.updateManiItem(centralOldManiItem) == true ? 0 : 1;
											if(statusUpdate == 0){
												ULDObject uld = uldService.getUldById(centralOldManiItem.getLl_uld().uld_id);
												uld.isUsed = 0;
												List<AManifestItem> usedBy = dbManifestService.getManifestItemByllUldID(uld);
												if(usedBy == null) this.uldService.updateAULD(uld);
											}
										}
									}
								}
							}
							else if(data.getState().equals("toManifest")){
								AManifest mani = this.getManifest(data.getFltID(), data.getFltDate());
								if(mani == null){
									mani = new AManifest();
									mani.setId(null);
									RSFlt flt = new RSFlt();
									flt.setFlt_Id(data.getFltID());
									mani.setFlt_id(flt);
									mani.setFlt_date(data.getFltDate());
									mani.setCreatorAddress(data.getAddID());
									mani.setStat_id(SArea.MANIFEST_LIST);
									mani.setManifestItems(null);
									mani.setV_o(new Date());
									this.dbManifestService.saveMani(mani);
								}
								else{
									mani.setV_o(new Date());
									mani.setStat_id(SArea.MANIFEST_LIST);
									this.dbManifestService.updateMani(mani);
								}
							}
							else if(data.getState().equals("acceptULD")){
								this.uldService.acceptULD(data.getUldObject().getUld_id(), data.getAddID(), data.getRsState(), data.getIsUsed());
							}
							else if(data.getState().equals("saveULDStock")){
								this.uldService.saveULDStock(data.getUldStock());
							}
							else if(data.getState().equals("saveULDStandart")){
								this.uldService.saveULDStandart(data.getUldObject(), false);
							}
							else if(data.getState().equals("saveULDAndULDTypeNonStandart")){
								this.uldService.saveULDAndULDTypeNonStandart(data.getUldObject(), data.getSULD(), false);
							}
							else if(data.getState().equals("updateULDStandart")){
								this.uldService.updateULDStandart(data.getUldObject());
							}
							else if(data.getState().equals("updateULDAndULDTypeNonStandart")){
								data.getUldObject().setS_uld_id(data.getOldSULD());
								this.uldService.updateULDAndULDTypeNonStandart(data.getUldObject(), data.getSULD(), false);
							}
							else if(data.getState().equals("deleteAULD")){
								this.uldService.deleteAULD(data.getUldObject());
							}
						}
						data.setCentralID(id_db);
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSAwbNo){
						JMSAwbNo data = (JMSAwbNo) serializableObject.getObjectTemplate();
						data.setCentralAddID(this.staticDataService.getID_DB());
						this.airlineManager.updateAwbStock(data.getCaID(), data.getBlackboxID(), data.getOwnerAddID(), true);
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSSCCHandling){
						JMSSCCHandling data = (JMSSCCHandling) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("savePreadvise")){
								//HENRY - need new function for save preadvice
								//this.sccHandlingManager.savePreadvise(data.getPreadvise());
							}
							else if(data.getState().equals("updateRespond")){
								this.sccHandlingManager.updateRespond(data.getId().longValue(), data.getRespond());
							}
						}
						data.setCentralID(id_db);
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSPreAdviceRespond){
						JMSPreAdviceRespond data = (JMSPreAdviceRespond) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("savePreadvise")){
								this.sccHandlingManager.savePreadvise(data.getF_id(), data.getRespond(), data.getAdd_id(), data.getAp_3lc(), data.getUser_id());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSNotoc){
						JMSNotoc data = (JMSNotoc) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("saveNotoc")){
								this.sccHandlingManager.insertANotocAndFormularNotocInfo(data.getNotoc(), data.getInfos());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSManifest){
						JMSManifest data = (JMSManifest) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(data.getBlackboxID())){
							if(data.getState().equals("saveRemark")){
								this.manifestService.saveManiItemRemark(data.getF_id(), data.getRemark(), data.getPart());
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSObjectWrapper){
						JMSObjectWrapper data = (JMSObjectWrapper) serializableObject.getObjectTemplate();
						if(data.getMainObject() instanceof SccPreference)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("insertSCCPreference")){
									this.sccHandlingManager.insertSccPreference((SccPreference)data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof ZHandlingAirport)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("insertHandlingAgentAirport")){
									this.handlingAgentService.insertHandlingAgentAirport((ZHandlingAirport)data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof CCommodityTree)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("createCommodityJoinTable")){
									this.commodityManager.createCommodityJoinTable((CCommodityTree)data.getMainObject());
								}
								else if(data.getState().equals("createCommodityJoinTableDefault")){
									this.commodityManager.createCommodityJoinTableDefault((CCommodityTree)data.getMainObject(), ((CCommodityTree)data.getMainObject()).getDefCom());
								}
								else if(data.getState().equals("createCommodity")){
									this.commodityManager.createCommodity((CCommodityTree)data.getMainObject());
								}
								else if(data.getState().equals("editCommodity")){
									this.commodityManager.editCommodity((CCommodityTree)data.getMainObject());
								}
								else if(data.getState().equals("deleteCommodity")){
									this.commodityManager.deleteCommodity((CCommodityTree)data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof RSAnn)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("createAnnotation")){
									this.commodityManager.createAnnotation((RSAnn)data.getMainObject());
								}
								else if(data.getState().equals("deleteAnnotation")){
									this.commodityManager.createAnnotation((RSAnn)data.getMainObject());
								}
								else if(data.getState().equals("editAnnotation")){
									this.commodityManager.editAnnotation((RSAnn)data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof RSScc)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("saveSCC")){
									this.commodityManager.saveSCC((RSScc)data.getMainObject());
								}
								else if(data.getState().equals("updateSCC")){
									this.commodityManager.updateSCC((RSScc)data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof Classes)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("saveClass")){
									this.commodityManager.saveClass((Classes)data.getMainObject());
								}
								else if(data.getState().equals("updateClass")){
									this.commodityManager.updateClass((Classes)data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof RSFuelChar)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("SaveFuelCharges")){
									this.rateManager.saveFuelCharges((RSFuelChar) data.getMainObject());
								}
								else if(data.getState().equals("UpdateFuelCharges")){
									this.rateManager.updateFuelCharges((RSFuelChar) data.getMainObject(), (Long)data.getObjectList().get(0));
								}
							}
						}
						else if(data.getMainObject() instanceof RSCharges)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("saveSecurityCharges")){
									this.rateManager.saveSecurityCharges((RSCharges) data.getMainObject());
								}
								else if(data.getState().equals("updateSecurityCharges")){
									this.rateManager.updateSecurityCharges((RSCharges) data.getMainObject(), (Long)data.getObjectList().get(0));
								}
							}
						}
						else if(data.getMainObject() instanceof Long)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("DeleteFuelCharges")){
									this.rateManager.deleteFuelCharges((Long) data.getMainObject());
								}
								if(data.getState().equals("deleteSecurityCharges")){
									this.rateManager.deleteSecurityCharges((Long) data.getMainObject());
								}
							}
						}
						else if(data.getMainObject() instanceof RCBkg)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("updateABkgPriority")){
									RCBkg temp = (RCBkg)data.getMainObject();
									this.manifestService.updateABkgPriority(temp.getFid(), temp.getPart(), temp.getDept(), temp.getDest(), temp.getPrio());
									this.formularManager.updateItemBKGPrio(temp.getFid(), temp.getPart(), temp.getPrio());
								}
							}
						}
						else if(data.getMainObject() instanceof RSDbmWs)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("saveDbmWs")){
									RSDbmWs temp = (RSDbmWs)data.getMainObject();
									this.manifestService.saveDbmWs(temp);
								}
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSAirlineScc)
					{
						JMSAirlineScc scc = (JMSAirlineScc) serializableObject.getObjectTemplate();
						Integer id_db = staticDataService.getID_DB();
						if(!id_db.equals(scc.getBlackboxID()))
						{
							if(JMSAirlineScc.TYPE_AIRLINE == scc.getType())
							{
								if(JMSAirlineScc.STATE_SAVE.equals(scc.getState()))
								{
									sccHandlingManager.saveAirlineScc(scc.getAirlineScc());
								}
							}
							else if(JMSAirlineScc.TYPE_AIRCRAFT == scc.getType())
							{
								if(JMSAirlineScc.STATE_SAVE.equals(scc.getState()))
								{
									aircraftManager.saveAircraftScc(scc.getAircraftScc());
								}
								else if(JMSAirlineScc.STATE_UPDATE.equals(scc.getState()))
								{
									aircraftManager.updateAircraftScc(scc.getAircraftScc());
								}
							}
							else if(JMSAirlineScc.TYPE_SCHEDULE == scc.getType())
							{
								if(JMSAirlineScc.STATE_SAVE.equals(scc.getState()))
								{
									scheduleManager.saveScheduleScc(scc.getScheduleScc());
								}
								else if(JMSAirlineScc.STATE_UPDATE.equals(scc.getState()))
								{
									scheduleManager.updateScheduleScc(scc.getScheduleScc());
								}
							}
							else if(JMSAirlineScc.TYPE_ASCC_LOAD == scc.getType())
							{
								if(JMSAirlineScc.STATE_SAVE.equals(scc.getState()))
								{
									sccHandlingManager.saveSccLoadable(scc.getLoadableList());
								}
							}
						}
					}
					jmsGateway.sendToClient("ClientReceiveSArea", serializableObject);
				}
			}
		} 
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private Integer saveAddBlackboxID(JMSPreferenceAddressObject data){
		/** save c blackbox add id **/
		RSAddBlackboxID add_blackbox_id = new RSAddBlackboxID();
		add_blackbox_id.setBlackbox_id(data.getBlackboxID());
		
		RSAddID rsaddID = new RSAddID();
		rsaddID.setAdd_id(data.getSadd().getAdd_id());								
		add_blackbox_id.setAdd(rsaddID);
		return staticDataService.storeAddBlackboxID(add_blackbox_id);
	}
	
	private AManifest getOrCreateManifest(Long flt_id, Date flt_date, Integer add_id){
		AManifest mani = this.getManifest(flt_id, flt_date);
		if(mani == null) {
			mani = new AManifest();
			RSFlt sFlight = new RSFlt();
			sFlight.flt_id = flt_id;
			mani.setFlt_id(sFlight);
			mani.setFlt_date(flt_date);
			mani.setUserAddress(add_id);
			mani.setCreatorAddress(add_id);
			mani.setStat_id(SArea.LOADING_LIST);

			boolean status = dbManifestService.saveMani(mani);
			if(status) return mani;
			return null;
		}
		return mani;
	}
	
	private List<AManifestItem> getManifestItem(List<AManifestItem> listManiItem, Set<PackageItemBKG> setItemBooking){
		List<AManifestItem> returnList = new ArrayList<AManifestItem>();
		for(AManifestItem maniItem : listManiItem){
			for(PackageItemBKG itemBooking : setItemBooking){
				if(maniItem.getBooking().getId().equals(itemBooking.getBkgItemId())){
					returnList.add(maniItem);
				}
			}
		}
		if(returnList.size() == 0) return null;
		return returnList;
	}
	
	private Integer compare(PackageItemBKG itemPackage, ItemHandlingBKG maniItem){
		Integer similarity = 0;
		if(itemPackage.getPcs().equals(maniItem.getPcs())) similarity++;
		if(itemPackage.getWgt().equals(maniItem.getWgt())) similarity++;
		if(itemPackage.getVol().equals(maniItem.getVol())) similarity++;
		if(itemPackage.getComId().equals(maniItem.getCommodity().getId())) similarity++;
		if(itemPackage.getReqDep().equals(maniItem.getRequestedDepartureDate())) similarity++;
		return similarity;
	}
	
	public AManifest getManifest(Long flt_id, Date flt_date){
		try{
			return this.dbManifestService.getManifest(flt_id, flt_date);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public IActernityGatewayServer getJmsGateway() {
		return jmsGateway;
	}

	public void setJmsGateway(IActernityGatewayServer jmsGateway) {
		this.jmsGateway = jmsGateway;
	}

	public IStaticDataService getStaticDataService() {
		return staticDataService;
	}

	public void setStaticDataService(IStaticDataService staticDataService) {
		this.staticDataService = staticDataService;
	}

	public IAircraftManager getAircraftManager() {
		return aircraftManager;
	}

	public void setAircraftManager(IAircraftManager aircraftManager) {
		this.aircraftManager = aircraftManager;
	}

	public IScheduleManager getScheduleManager() {
		return scheduleManager;
	}

	public void setScheduleManager(IScheduleManager scheduleManager) {
		this.scheduleManager = scheduleManager;
	}

	public INetRatesManager getNetRatesManager() {
		return netRatesManager;
	}

	public void setNetRatesManager(INetRatesManager netRatesManager) {
		this.netRatesManager = netRatesManager;
	}

	public IPreferenceManager getPreferenceManager() {
		return preferenceManager;
	}

	public void setPreferenceManager(IPreferenceManager preferenceManager) {
		this.preferenceManager = preferenceManager;
	}

	public IDBManifestService getDbManifestService() {
		return dbManifestService;
	}

	public void setDbManifestService(IDBManifestService dbManifestService) {
		this.dbManifestService = dbManifestService;
	}

	public IUldService getUldService() {
		return uldService;
	}

	public void setUldService(IUldService uldService) {
		this.uldService = uldService;
	}

	public ITransferManager getTransferManager() {
		return transferManager;
	}

	public void setTransferManager(ITransferManager transferManager) {
		this.transferManager = transferManager;
	}

	public IAirlineManager getAirlineManager() {
		return airlineManager;
	}

	public void setAirlineManager(IAirlineManager airlineManager) {
		this.airlineManager = airlineManager;
	}

	public ISccHandlingManager getSccHandlingManager() {
		return sccHandlingManager;
	}

	public void setSccHandlingManager(ISccHandlingManager sccHandlingManager) {
		this.sccHandlingManager = sccHandlingManager;
	}

	public IManifestService getManifestService()
	{
		return manifestService;
	}

	public void setManifestService(IManifestService manifestService)
	{
		this.manifestService = manifestService;
	}

	public IHandlingAgentService getHandlingAgentService()
	{
		return handlingAgentService;
	}

	public void setHandlingAgentService(IHandlingAgentService handlingAgentService)
	{
		this.handlingAgentService = handlingAgentService;
	}

	public ICommodityManager getCommodityManager()
	{
		return commodityManager;
	}

	public void setCommodityManager(ICommodityManager commodityManager)
	{
		this.commodityManager = commodityManager;
	}

	public IUserPreferenceManager getPreferenceUserManager() {
		return preferenceUserManager;
	}

	public void setPreferenceUserManager(
			IUserPreferenceManager preferenceUserManager) {
		this.preferenceUserManager = preferenceUserManager;
	}

	public IRateManager getRateManager()
	{
		return rateManager;
	}

	public void setRateManager(IRateManager rateManager)
	{
		this.rateManager = rateManager;
	}

	public IChargesManager getChargesManager()
	{
		return chargesManager;
	}

	public void setChargesManager(IChargesManager chargesManager)
	{
		this.chargesManager = chargesManager;
	}

	public IFormularManager getFormularManager()
	{
		return formularManager;
	}

	public void setFormularManager(IFormularManager formularManager)
	{
		this.formularManager = formularManager;
	}
	
}
