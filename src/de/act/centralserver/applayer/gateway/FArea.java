/**
 * 
 */
package de.act.centralserver.applayer.gateway;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import de.act.blackbox.applayer.interfaces.IStaticDataService;
import de.act.blackbox.applayer.interfaces.ITransferManager;
import de.act.blackbox.applayer.interfaces.IUldService;
import de.act.blackbox.applayer.interfaces.airline.IImportAcceptanceManager;
import de.act.blackbox.dblayer.interfaces.IBkgDao;
import de.act.blackbox.dblayer.interfaces.IDBManifestService;
import de.act.central.types.Attachment;
import de.act.central.types.AwbNo;
import de.act.central.types.BookingFlight;
import de.act.central.types.FormularInformation;
import de.act.central.types.PackageItem;
import de.act.central.types.PackageItemBKG;
import de.act.centralserver.dblayer.interfaces.airline.IDBFlightManager;
import de.act.common.interfaces.IActernityGatewayServer;
import de.act.common.jmstypes.SerializableObject;
import de.act.common.jmstypes.beanwrapper.JMSAuth;
import de.act.common.jmstypes.beanwrapper.JMSObjectWrapper;
import de.act.common.jmstypes.beanwrapper.JMSPreAdvice;
import de.act.common.jmstypes.beanwrapper.JMSRequestBookingObject;
import de.act.common.jmstypes.beanwrapper.JMSSyncDB;
import de.act.common.jmstypes.beanwrapper.JMSTrigger;
import de.act.common.jmstypes.beanwrapper.JMSUserRegistrationObject;
import de.act.common.types.agent.ContentData;
import de.act.common.types.handling.AManifest;
import de.act.common.types.handling.AManifestItem;
import de.act.common.types.handling.AULDTrans;
import de.act.common.types.nonstaticobjects.RCBkg;
import de.act.common.types.nonstaticobjects.RFForm;
import de.act.common.types.nonstaticobjects.RFStat;
import de.act.common.types.nonstaticobjects.ULDObject;
import de.act.common.types.packageitems.ItemHandlingBKG;
import de.act.common.types.staticobjects.RSAc;
import de.act.common.types.staticobjects.RSAdd;
import de.act.common.types.staticobjects.RSAp;
import de.act.common.types.staticobjects.RSFlt;

/**
 * @author Henry
 *
 */
public class FArea implements MessageListener{

	private static final Logger log = Logger.getLogger(FArea.class);
	private Integer ID_DB;
	private ArrayList<Integer> formularChecker = new ArrayList<Integer>();
	private ArrayList<Integer> temp = new ArrayList<Integer>();
	private IActernityGatewayServer jmsGateway;
	private IStaticDataService staticDataService;
	private ITransferManager transferManager;
	private IImportAcceptanceManager importAcceptanceManager;
	private IBkgDao bkgDao;
	private IUldService uldService;
	private IDBManifestService dbManifestService;
	private IDBFlightManager flightManager;
	private MailSender mailSender;
	 
	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	
	public void onMessage(Message message){
		try {
			if (message instanceof ObjectMessage){
				ObjectMessage objMsg = (ObjectMessage) message;
				Serializable obj = objMsg.getObject();
				if (obj instanceof SerializableObject){
					SerializableObject serializableObject = (SerializableObject) obj;
					if(serializableObject.getObjectTemplate() instanceof JMSRequestBookingObject){
						this.ID_DB = this.staticDataService.getID_DB();
						FArea.log.info("Central received RequestBookingObject");
						JMSRequestBookingObject data = (JMSRequestBookingObject) serializableObject.getObjectTemplate();
						HashMap<String, String> destinationList = data.getDestinationID();
						String ca_id = destinationList.get("airline");
						Set<Integer> setBlackboxAirline = new HashSet<Integer>(this.staticDataService.getAccountByCaID(ca_id));
						
						//HENRY -- UNBLOCK
						/*String ap3lc = destinationList.get("airportAirline");
						List<RSAdd> listAirportAddress = this.staticDataService.getAllAddressByAirport(ap3lc);
						Set<Integer> setAddAirport = new HashSet<Integer>();
						for(RSAdd add : listAirportAddress) setAddAirport.add(add.getAdd_id());
						setBlackboxAirline.removeAll(setAddAirport);*/
						//Integer blackboxAirline = this.staticDataService.getID_DBByAdd_ID(setAddAirport.iterator().next().toString());
						
						Integer blackboxAirline = this.staticDataService.getID_DBByAdd_ID(setBlackboxAirline.iterator().next().toString());
						temp = data.getBlackboxChecker();
						formularChecker = data.getBlackboxChecker();
						if(data.getState().equals("request")){
							FArea.log.info("Central distributing main form");
							//String DestAp3lc = destinationList.get("airportDest");
							//List<RSAdd> listDestAirportAddress = this.staticDataService.getAllAddressByAirport(DestAp3lc);
							//Set<Integer> setDestAddAirport = new HashSet<Integer>();
							//for(RSAdd add : listDestAirportAddress) setDestAddAirport.add(add.getAdd_id());
							//setDestAddAirport.removeAll(setAddAirport);
							//Set<Integer> setDestBlackbox = new HashSet<Integer>();
							//for(Integer add_id : setDestAddAirport) setDestBlackbox.add(this.staticDataService.getID_DBByAdd_ID(add_id.toString()));
							
							//Routing
							Set<Integer> setRouting = new HashSet<Integer>();
							Integer totalRouting = Integer.parseInt(destinationList.get("totalRouting"));
							for(int i = 0; i < totalRouting; i++){
								String routeAp3lc = destinationList.get("routing"+i);
								List<RSAdd> routingAddress = this.staticDataService.getAllAddressByAirport(routeAp3lc);
								for(RSAdd add : routingAddress) setRouting.add(add.getAdd_id());
							}
							
							//HENRY -- UNBLOCK
							//setRouting.removeAll(setAddAirport);
							
							setRouting.removeAll(setBlackboxAirline);
								
							this.saveMainformCentral(data.getContentCentral(), data.getRfStat());
							if(!formularChecker.contains(ID_DB)){
								formularChecker.add(ID_DB);
							}
							data.addBlackboxChecker(blackboxAirline);
							data.setState("airline");
							serializableObject.setObjectTemplate(data);
							jmsGateway.sendToClient("f_area_blackbox_" + blackboxAirline, serializableObject);

							if(!formularChecker.contains(data.getBlackboxID())){
								formularChecker.add(data.getBlackboxID());
								data.setState("mainForm");
								data.setBlackboxChecker(temp);
								serializableObject.setObjectTemplate(data);
								jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
							}
							if(destinationList.get("agent") != null){
								String add_id = destinationList.get("agent");
								Integer blackboxIDAgent = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(!formularChecker.contains(blackboxIDAgent)){
									formularChecker.add(blackboxIDAgent);
									data.setState("mainForm");
									data.setBlackboxChecker(temp);
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDAgent, serializableObject);
								}
							}
							if(destinationList.get("consignee") != null){ 
								String add_id = destinationList.get("consignee"); 
								Integer blackboxIDConsignee = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(!formularChecker.contains(blackboxIDConsignee)){
									formularChecker.add(blackboxIDConsignee);
									data.setState("mainForm");
									data.setBlackboxChecker(temp);
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDConsignee, serializableObject);
								}
							}
							if(destinationList.get("shipper") != null){
								String add_id = destinationList.get("shipper");
								Integer blackboxIDShipper = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(!formularChecker.contains(blackboxIDShipper)){
									formularChecker.add(blackboxIDShipper);
									data.setState("mainForm");
									data.setBlackboxChecker(temp);
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDShipper, serializableObject);
								}
							}
							if(setRouting.size() > 0){
								for(Integer blackboxID : setRouting){
									if(!formularChecker.contains(blackboxID)){
										formularChecker.add(blackboxID);
										data.setState("mainForm");
										data.setBlackboxChecker(temp);
										serializableObject.setObjectTemplate(data);
										jmsGateway.sendToClient("f_area_blackbox_" + blackboxID, serializableObject);
									}
								}
							}
							/*if(setDestBlackbox.size() > 0){
								for(Integer blackboxID : setDestBlackbox){
									if(!formularChecker.contains(blackboxID)){
										formularChecker.add(blackboxID);
										data.setState("mainForm");
										data.setBlackboxChecker(temp);
										serializableObject.setObjectTemplate(data);
										jmsGateway.sendToClient("f_area_blackbox_" + blackboxID, serializableObject);
									}
								}
							}*/
							data.setBlackboxChecker(temp);
						}
						else if(data.getState().equals("reply")){
							FArea.log.info("Central distributed the reply form");
							
							if(data.getBlackboxID().equals(ID_DB)){
								if(data.canSend(data.getBlackboxID())){
									data.addBlackboxChecker(data.getBlackboxID());
									data.setState("requestingBlackbox");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
								}
							}
							else if(!data.getBlackboxID().equals(ID_DB)){
								if(data.canSend(ID_DB)){
									data.addBlackboxChecker(ID_DB);
									this.saveConfirmation(data.getContentCentral(), data.getBkg());
									this.saveManifest(data.getRfForm(), data.getUser().getAddId());
								}
								if(data.canSend(data.getBlackboxID())){
									data.addBlackboxChecker(data.getBlackboxID());
									data.setState("requestingBlackbox");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
								}
							}
							/** HENRY **/
							if(data.canSend(ID_DB)){
								data.addBlackboxChecker(ID_DB);
								this.saveConfirmation(data.getContentCentral(), data.getBkg());
								this.saveManifest(data.getRfForm(), data.getUser().getAddId());
								this.staticDataService.saveStat(data.getRfStat());
							}
							if(data.canSend(data.getBlackboxID())){
								data.addBlackboxChecker(data.getBlackboxID());
								data.setState("requestingBlackbox");
								serializableObject.setObjectTemplate(data);
								jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
							}
							/** **/
							if(destinationList.get("agent") != null){
								String add_id = destinationList.get("agent");
								Integer blackboxIDAgent = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(data.canSend(blackboxIDAgent)){
									data.addBlackboxChecker(blackboxIDAgent);
									data.setState("agent");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDAgent, serializableObject);
								}
							}
							if(destinationList.get("consignee") != null){ 
								String add_id = destinationList.get("consignee"); 
								Integer blackboxIDConsignee = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(data.canSend(blackboxIDConsignee)){
									data.addBlackboxChecker(blackboxIDConsignee);
									data.setState("consignee");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDConsignee, serializableObject);
								}
							}
							if(destinationList.get("shipper") != null){
								String add_id = destinationList.get("shipper");
								Integer blackboxIDShipper = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(data.canSend(blackboxIDShipper)){
									data.addBlackboxChecker(blackboxIDShipper);
									data.setState("shipper");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDShipper, serializableObject);
								}
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSPreAdvice){
						this.ID_DB = this.staticDataService.getID_DB();
						FArea.log.info("Central received RequestBookingObject");
						JMSPreAdvice data = (JMSPreAdvice) serializableObject.getObjectTemplate();
						HashMap<String, String> destinationList = data.getDestinationID();
						String ca_id = destinationList.get("airline");
						Set<Integer> setBlackboxAirline = new HashSet<Integer>(this.staticDataService.getAccountByCaID(ca_id));
						Integer blackboxAirline = this.staticDataService.getID_DBByAdd_ID(setBlackboxAirline.iterator().next().toString());
						temp = data.getBlackboxChecker();
						formularChecker = data.getBlackboxChecker();
						if(data.getState().equals("request")){
							FArea.log.info("Central distributing main form");
							//Routing
							Set<Integer> setRouting = new HashSet<Integer>();
							Integer totalRouting = Integer.parseInt(destinationList.get("totalRouting"));
							for(int i = 0; i < totalRouting; i++){
								String routeAp3lc = destinationList.get("routing"+i);
								List<RSAdd> routingAddress = this.staticDataService.getAllAddressByAirport(routeAp3lc);
								for(RSAdd add : routingAddress) setRouting.add(add.getAdd_id());
							}
							setRouting.removeAll(setBlackboxAirline);
								
							this.saveMainformCentral(data.getContentCentral(), null);
							if(!formularChecker.contains(ID_DB)){
								formularChecker.add(ID_DB);
							}
							data.addBlackboxChecker(blackboxAirline);
							data.setState("airline");
							serializableObject.setObjectTemplate(data);
							jmsGateway.sendToClient("f_area_blackbox_" + blackboxAirline, serializableObject);

							if(!formularChecker.contains(data.getBlackboxID())){
								formularChecker.add(data.getBlackboxID());
								data.setState("mainForm");
								data.setBlackboxChecker(temp);
								serializableObject.setObjectTemplate(data);
								jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
							}
							if(destinationList.get("agent") != null){
								String add_id = destinationList.get("agent");
								Integer blackboxIDAgent = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(!formularChecker.contains(blackboxIDAgent)){
									formularChecker.add(blackboxIDAgent);
									data.setState("mainForm");
									data.setBlackboxChecker(temp);
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDAgent, serializableObject);
								}
							}
							if(destinationList.get("consignee") != null){ 
								String add_id = destinationList.get("consignee"); 
								Integer blackboxIDConsignee = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(!formularChecker.contains(blackboxIDConsignee)){
									formularChecker.add(blackboxIDConsignee);
									data.setState("mainForm");
									data.setBlackboxChecker(temp);
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDConsignee, serializableObject);
								}
							}
							if(destinationList.get("shipper") != null){
								String add_id = destinationList.get("shipper");
								Integer blackboxIDShipper = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(!formularChecker.contains(blackboxIDShipper)){
									formularChecker.add(blackboxIDShipper);
									data.setState("mainForm");
									data.setBlackboxChecker(temp);
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDShipper, serializableObject);
								}
							}
							if(setRouting.size() > 0){
								for(Integer blackboxID : setRouting){
									if(!formularChecker.contains(blackboxID)){
										formularChecker.add(blackboxID);
										data.setState("mainForm");
										data.setBlackboxChecker(temp);
										serializableObject.setObjectTemplate(data);
										jmsGateway.sendToClient("f_area_blackbox_" + blackboxID, serializableObject);
									}
								}
							}
							data.setBlackboxChecker(temp);
						}
						else if(data.getState().equals("reply")){
							FArea.log.info("Central distributed the reply form");
							
							if(data.getBlackboxID().equals(ID_DB)){
								if(data.canSend(data.getBlackboxID())){
									data.addBlackboxChecker(data.getBlackboxID());
									data.setState("requestingBlackbox");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
								}
							}
							else if(!data.getBlackboxID().equals(ID_DB)){
								if(data.canSend(ID_DB)){
									data.addBlackboxChecker(ID_DB);
									this.saveConfirmation(data.getContentCentral(), data.getBkg());
									this.saveManifest(data.getRfForm(), data.getUser().getAddId());
								}
								if(data.canSend(data.getBlackboxID())){
									data.addBlackboxChecker(data.getBlackboxID());
									data.setState("requestingBlackbox");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
								}
							}
							/** HENRY **/
							if(data.canSend(ID_DB)){
								data.addBlackboxChecker(ID_DB);
								this.saveConfirmation(data.getContentCentral(), data.getBkg());
								this.saveManifest(data.getRfForm(), data.getUser().getAddId());
							}
							if(data.canSend(data.getBlackboxID())){
								data.addBlackboxChecker(data.getBlackboxID());
								data.setState("requestingBlackbox");
								serializableObject.setObjectTemplate(data);
								jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
							}
							/** **/
							if(destinationList.get("agent") != null){
								String add_id = destinationList.get("agent");
								Integer blackboxIDAgent = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(data.canSend(blackboxIDAgent)){
									data.addBlackboxChecker(blackboxIDAgent);
									data.setState("agent");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDAgent, serializableObject);
								}
							}
							if(destinationList.get("consignee") != null){ 
								String add_id = destinationList.get("consignee"); 
								Integer blackboxIDConsignee = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(data.canSend(blackboxIDConsignee)){
									data.addBlackboxChecker(blackboxIDConsignee);
									data.setState("consignee");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDConsignee, serializableObject);
								}
							}
							if(destinationList.get("shipper") != null){
								String add_id = destinationList.get("shipper");
								Integer blackboxIDShipper = this.staticDataService.getID_DBByAdd_ID(add_id);
								if(data.canSend(blackboxIDShipper)){
									data.addBlackboxChecker(blackboxIDShipper);
									data.setState("shipper");
									serializableObject.setObjectTemplate(data);
									jmsGateway.sendToClient("f_area_blackbox_" + blackboxIDShipper, serializableObject);
								}
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSSyncDB){
						JMSSyncDB data = (JMSSyncDB) serializableObject.getObjectTemplate();
						HashMap<String, String> destinationList = data.getDestinationID();
						formularChecker = data.getBlackboxChecker();
						this.ID_DB = this.staticDataService.getID_DB();
						if(data.getState().equals("bookingControl")){
							formularChecker.add(data.getBlackboxID());
							if(!formularChecker.contains(this.ID_DB)){
								syncDB(data.getContentCentral());
								formularChecker.add(this.ID_DB);
							}
							if(destinationList.get("consignee") != null){ 
								this.sentToClient(serializableObject, data, destinationList.get("consignee"), "syncDB");
							}
							if(destinationList.get("shipper") != null){
								this.sentToClient(serializableObject, data, destinationList.get("shipper"), "syncDB");
							}
							if(destinationList.get("agent") != null){
								this.sentToClient(serializableObject, data, destinationList.get("agent"), "syncDB");
							}
						}
						else if(data.getState().equals("bookingItemControl")){
							formularChecker.add(data.getBlackboxID());
							if(!formularChecker.contains(this.ID_DB)){
								syncDBBookingItemControl(data.getContentCentral());
								formularChecker.add(this.ID_DB);
							}
							if(destinationList.get("consignee") != null){
								this.sentToClient(serializableObject, data, destinationList.get("consignee"), "syncDBbookingItemControl");
							}
							if(destinationList.get("shipper") != null){
								this.sentToClient(serializableObject, data, destinationList.get("shipper"), "syncDBbookingItemControl");
							}
							if(destinationList.get("agent") != null){
								this.sentToClient(serializableObject, data, destinationList.get("agent"), "syncDBbookingItemControl");
							}
						}
						else if(data.getState().equals("saveCucAndMrnNumber")){
							formularChecker.add(data.getBlackboxID());
							if(!formularChecker.contains(this.ID_DB)){
								syncDBBookingItemControl(data.getContentCentral());
								if(data.getNumbers() != null && data.getNumbers().size() > 0)
								{
									dbManifestService.saveMRNNumbers(data.getNumbers());
								}
								formularChecker.add(this.ID_DB);
							}
							if(destinationList.get("consignee") != null){
								this.sentToClient(serializableObject, data, destinationList.get("consignee"), "syncDBsaveCucAndMrnNumber");
							}
							if(destinationList.get("shipper") != null){
								this.sentToClient(serializableObject, data, destinationList.get("shipper"), "syncDBsaveCucAndMrnNumber");
							}
							if(destinationList.get("agent") != null){
								this.sentToClient(serializableObject, data, destinationList.get("agent"), "syncDBsaveCucAndMrnNumber");
							}
						}
						else if(data.getState().equals("importAcceptance")){
							formularChecker.add(data.getBlackboxID());
							if(!formularChecker.contains(this.ID_DB)){
								syncDB(data.getContentCentral());
								formularChecker.add(this.ID_DB);
							}
							if(destinationList.get("consignee") != null){
								this.sentToClient(serializableObject, data, destinationList.get("consignee"), "syncDBImportAcceptance");
							}
							if(destinationList.get("shipper") != null){
								this.sentToClient(serializableObject, data, destinationList.get("shipper"), "syncDBImportAcceptance");
							}
							if(destinationList.get("agent") != null){
								this.sentToClient(serializableObject, data, destinationList.get("agent"), "syncDBImportAcceptance");
							}
						}
						else if(data.getState().equals("airwayBill")){
							formularChecker.add(data.getBlackboxID());
							if(!formularChecker.contains(this.ID_DB)){
								syncDB(data.getContentCentral());
								formularChecker.add(this.ID_DB);
							}
							if(destinationList.get("consignee") != null){
								this.sentToClient(serializableObject, data, destinationList.get("consignee"), "syncDBAirwayBill");
							}
							if(destinationList.get("shipper") != null){
								this.sentToClient(serializableObject, data, destinationList.get("shipper"), "syncDBAirwayBill");
							}
							if(destinationList.get("agent") != null){
								this.sentToClient(serializableObject, data, destinationList.get("agent"), "syncDBAirwayBill");
							}
						}
						else if(data.getState().equals("returnShipment")){
							formularChecker.add(data.getBlackboxID());
							if(!formularChecker.contains(this.ID_DB)){
								syncDBBookingItemControl(data.getContentCentral());
								formularChecker.add(this.ID_DB);
							}
							if(destinationList.get("consignee") != null){
								this.sentToClient(serializableObject, data, destinationList.get("consignee"), "syncDBreturnShipment");
							}
							if(destinationList.get("shipper") != null){
								this.sentToClient(serializableObject, data, destinationList.get("shipper"), "syncDBreturnShipment");
							}
							if(destinationList.get("agent") != null){
								this.sentToClient(serializableObject, data, destinationList.get("agent"), "syncDBreturnShipment");
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSTrigger){
						JMSTrigger data = (JMSTrigger) serializableObject.getObjectTemplate();
						if(data.getState().equals("triggerAutoDropLyingListWhenLogin")){
							this.jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSUserRegistrationObject){
						JMSUserRegistrationObject data = (JMSUserRegistrationObject) serializableObject.getObjectTemplate();
						this.jmsGateway.sendToClient("f_area_blackbox_" + data.getDestinationBlackbox(), serializableObject);
						try{
							SimpleMailMessage msg = new SimpleMailMessage();
							msg.setFrom("support@acternity.com");
							msg.setTo(data.getEmailAdd());
							msg.setSubject("Welcome to Acternity.");
							msg.setText(data.getEmailText());
							mailSender.send(msg);
						}catch (MailSendException e) {
							System.out.print("error sending email : "+e.getMessage());
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSAuth){
						JMSAuth data = (JMSAuth) serializableObject.getObjectTemplate();
						Set<Integer> setAddress = new HashSet<Integer>();
						if(data.getState().equals("manifest")){
							List<RSAdd> listAddress = this.getStaticDataService().getAllAddressByAirport(data.getAirport().ap_3lc);
							for(RSAdd address : listAddress) setAddress.add(address.getAdd_id());
						}
						else if(data.getState().equals("partshipment")){
							List<RSAp> listAirport = data.getListAirport();
							for(RSAp airport : listAirport){
								List<RSAdd> listAddress = this.getStaticDataService().getAllAddressByAirport(airport.ap_3lc);
								for(RSAdd address : listAddress) setAddress.add(address.getAdd_id());
							}
						}
						List<Integer> blackboxSent = new ArrayList<Integer>();
						for(Integer addressID : setAddress){
							Integer blackboxID = this.getStaticDataService().getID_DBByAdd_ID(addressID.toString());
							if(!blackboxSent.contains(blackboxID)){
								this.jmsGateway.sendToClient("f_area_blackbox_" + blackboxID, serializableObject);
								blackboxSent.add(blackboxID);
							}
						}
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSObjectWrapper){
						JMSObjectWrapper data = (JMSObjectWrapper) serializableObject.getObjectTemplate();
						if(data.getMainObject() instanceof List)
						{
							Integer id_db = staticDataService.getID_DB();
							if(!id_db.equals(data.getBlackboxID())){
								if(data.getState().equals("saveTransferManifest")){
									this.importAcceptanceManager.saveTransferManifest((List)data.getMainObject());
								}
							}
						}
					}
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void sentToClient(SerializableObject serializableObject, JMSSyncDB data, String add_id, String status){
		Integer blackboxID = this.staticDataService.getID_DBByAdd_ID(add_id);
		if(!formularChecker.contains(blackboxID)){
			formularChecker.add(blackboxID);
			data.setState(status);
			data.setBlackboxChecker(formularChecker);
			serializableObject.setObjectTemplate(data);
			jmsGateway.sendToClient("f_area_blackbox_" + blackboxID, serializableObject);
		}
	}
	
	private void saveMainformCentral(ContentData contentData, RFStat stat){
		FArea.log.info("Central save main form");
		FormularInformation ffrom = this.transferManager.storeFormularInformations(contentData.getFormularInformations());
		AwbNo awb = ffrom.getAwbNo();
		awb = this.transferManager.storeAwbNo(awb);
		if (ffrom != null && awb != null) {
			List<Attachment> att = contentData.getAttachments();
			boolean stored = this.transferManager.storeData(ffrom, att);
			if (stored) {
				FArea.log.info("Form successfully saved");
				
				/*if(stat != null)
					this.staticDataService.saveStat(stat);*/
				FArea.log.info("RFStat " + stat.getfId());
			} 
			else {
				FArea.log.info("Store fail");
			}
		}
	}
	
	private void saveConfirmation(ContentData contentData, RCBkg data){
		FArea.log.info("Central save confirmation");
		FormularInformation ffrom = contentData.getFormularInformations();
		if (ffrom != null) {
			List<Attachment> att = contentData.getAttachments();
			boolean stored = this.transferManager.storeData(ffrom, att);
			ffrom.anyBooking(true);
			ffrom.anyBooking(false);
			if(stored){
				log.info("booked");
			} 
			else {
				log.error("not booked");
			}
		}
		if(data != null){
			RCBkg aBkg = new RCBkg();
			aBkg.setId(null);
			aBkg.setBkgId(data.getBkgId());
			aBkg.setFltId(data.getFltId());
			aBkg.setAwbNo(data.getAwbNo());
			aBkg.setPart(data.getPart());
			aBkg.setAddId(data.getAddId());
			aBkg.setFid(data.getFid());
			aBkg.setPrio(data.getPrio());
			aBkg.setBkgStatus(data.getBkgStatus());
			aBkg.setComId(data.getComId());
			aBkg.setAnnId(data.getAnnId());
			aBkg.setDept(data.getDept());
			aBkg.setDest(data.getDest());
			aBkg.setScc3lc(data.getScc3lc());
			aBkg.setPcs(data.getPcs());
			aBkg.setWgt(data.getWgt());
			aBkg.setVol(data.getVol());
			aBkg.setAcComp(data.getAcComp());
			aBkg.setUld3lc(data.getUld3lc());
			aBkg.setIsActive(data.getIsActive());
			bkgDao.saveBKG(aBkg);
		}
	}
	
	private void saveManifest(RFForm form, Integer add_id){
		ContentData content = this.transferManager.loadData(form);
		Set<BookingFlight> listBookingFlight = content.getLastFBkgFlts();
		for(BookingFlight bookingFlight : listBookingFlight){
			PackageItemBKG itemBooking = bookingFlight.getFBkgItem();
			Set<PackageItem> setItemPackage = itemBooking.getFPkgs();
			for(PackageItem itemPackage : setItemPackage){
				if(itemPackage.getUld_id() == null)
					itemPackage.setUld_id(Long.parseLong("10"));
				if(itemPackage.getUld_id() != null){
					Long fltID = bookingFlight.getFltId();
					Date fltDate = bookingFlight.getBkgFltDate();
					
					AManifest manifest = this.getOrCreateManifest(fltID, fltDate, add_id);
					AManifestItem manifestItem = new AManifestItem();
					manifestItem.setMani_id(manifest);
					manifestItem.setMani_row(0);
					manifestItem.setMani_sort(0);
					ItemHandlingBKG itemHandling = new ItemHandlingBKG();
					itemHandling.setId(itemBooking.getBkgItemId());
					manifestItem.setBooking(itemHandling);
					manifestItem.setPcs(itemBooking.getPcs());
					ULDObject uld = this.uldService.getUldById(itemPackage.getUld_id());
					manifestItem.setLl_uld(uld);
					manifestItem.setIs_active(1);
					Integer statusSave = this.dbManifestService.saveManiItem(manifestItem) == true ? 0 : 1;
					if(statusSave == 0){
						//ULDObject uld = uldService.getUldById(manifestItem.getLl_uld().uld_id);
						uld.isUsed = 1;
						this.uldService.updateAULD(uld);
						
						//FIRMAN
						if(!itemPackage.getUld_id().equals(10l)){
							AULDTrans transUld = new AULDTrans();
							transUld.setUldId(itemPackage.getUld_id());
							transUld.setUldTransMode("t");
							transUld.setUldDest(bookingFlight.getFBkgItem().getReqTo());
							transUld.setUldUser(add_id.longValue());
							
							this.uldService.saveUldTrans(transUld);
						}
					}
					break;
				}
			}
		}
	}
	
	private AManifest getOrCreateManifest(Long flt_id, Date flt_date, Integer add_id){
		String LOADING_LIST = "mmld";
		RSFlt flight = this.getFligt(flt_id, flt_date);
		RSAc sac = this.getRsac(flight.getAc_Id());
		AManifest mani = this.getManifest(flt_id, flt_date);
		if(mani == null) {
			mani = new AManifest();
			RSFlt sFlight = new RSFlt();
			sFlight.flt_id = flt_id;
			mani.setFlt_id(sFlight);
			mani.setFlt_date(flt_date);
			mani.setUserAddress(add_id);
			mani.setCreatorAddress(add_id);
			mani.setStat_id(LOADING_LIST);
			mani.setAc_reg(sac.getAc_reg());

			boolean status = this.dbManifestService.saveMani(mani);
			if(status) return mani;
			return null;
		}
		return mani;
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
	
	public RSFlt getFligt(Long flt_id, Date flt_date){
		try{
			return this.flightManager.getFligt(flt_id, flt_date);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public RSAc getRsac(Long ac_id){
		try{
			return this.flightManager.getSac(ac_id);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void syncDB(ContentData contentData){
		FormularInformation ffrom = this.transferManager.storeFormularInformations(contentData.getFormularInformations());
		if (ffrom != null) {
			List<Attachment> att = contentData.getAttachments();
			boolean stored = this.transferManager.storeData(ffrom, att);
			if(stored){
				log.info("Sync DB Successful");
			} 
			else {
				log.error("Sync DB failed");
			}
		}
	}
	
	private void syncDBBookingItemControl(ContentData contentData){
		FormularInformation ffrom = this.transferManager.storeFormularInformations(contentData.getFormularInformations());
		if (ffrom != null) {
			List<Attachment> att = contentData.getAttachments();
			boolean stored = this.transferManager.storeData(ffrom, att);
			if(stored){
				log.info("Sync DB Successful");
			} 
			else {
				log.error("Sync DB failed");
			}
		}
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

	public ITransferManager getTransferManager() {
		return transferManager;
	}

	public void setTransferManager(ITransferManager transferManager) {
		this.transferManager = transferManager;
	}

	public IBkgDao getBkgDao() {
		return bkgDao;
	}

	public void setBkgDao(IBkgDao bkgDao) {
		this.bkgDao = bkgDao;
	}

	public IUldService getUldService() {
		return uldService;
	}

	public void setUldService(IUldService uldService) {
		this.uldService = uldService;
	}

	public IDBManifestService getDbManifestService() {
		return dbManifestService;
	}

	public void setDbManifestService(IDBManifestService dbManifestService) {
		this.dbManifestService = dbManifestService;
	}


	public IImportAcceptanceManager getImportAcceptanceManager()
	{
		return importAcceptanceManager;
	}

	public void setImportAcceptanceManager(IImportAcceptanceManager importAcceptanceManager)
	{
		this.importAcceptanceManager = importAcceptanceManager;
	}
	
}