/**
 * 
 */
package de.act.centralserver.applayer.gateway;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import de.act.central.types.CFlt;
import de.act.central.types.CentralBooking;
import de.act.central.types.CentralFlights;
import de.act.centralserver.applayer.interfaces.airline.IFlightManager;
import de.act.centralserver.applayer.interfaces.usermodule.IUserRegistrationManager;
import de.act.common.interfaces.IActernityGatewayServer;
import de.act.common.jmstypes.SerializableObject;
import de.act.common.jmstypes.beanwrapper.JMSFlightObject;
import de.act.common.types.Aflt;
import de.act.common.types.nonstaticobjects.RCBkg;
import de.act.common.types.staticobjects.RCAdd;

/**
 * @author Henry
 *
 */
public class CArea implements MessageListener{

	private IUserRegistrationManager userRegistration;
	private IFlightManager flightManager;
	private IActernityGatewayServer jmsGateway;
	
	public void onMessage(Message message){
		try{
			if (message instanceof ObjectMessage){
				ObjectMessage objMsg = (ObjectMessage) message;
				Serializable obj = objMsg.getObject();
				if (obj instanceof SerializableObject){
					SerializableObject serializableObject = (SerializableObject) obj;
					if(serializableObject.getObjectTemplate() instanceof RCAdd){
						RCAdd addressData = (RCAdd) serializableObject.getObjectTemplate();
						userRegistration.saveUserRegistration(addressData);
					}
					else if(serializableObject.getObjectTemplate() instanceof JMSFlightObject){
						JMSFlightObject data = (JMSFlightObject) serializableObject.getObjectTemplate();
						for(int i = 0; i < data.getListAflt().size(); i++){
							Aflt aFlt = data.getListAflt().get(i);
							if(data.isUseCurrentID()){
								CFlt cFlt = new CFlt();
								cFlt.setFltId(aFlt.getA_flt_id());
								cFlt.setFlt_group(aFlt.getFlt_group());
								cFlt.setSFltId(aFlt.getS_flt_id());
								cFlt.setDep(aFlt.getDep());
								cFlt.setArr(aFlt.getArr());
								cFlt.setAc(aFlt.getAc());
								cFlt = this.flightManager.saveFlight(cFlt);
								aFlt.setA_flt_id(cFlt.getFltId());
							}
							else{
								CentralFlights cFlt = new CentralFlights();
								cFlt.setFlt_group(aFlt.getFlt_group());
								cFlt.setSFltId(aFlt.getS_flt_id());
								cFlt.setDep(aFlt.getDep());
								cFlt.setArr(aFlt.getArr());
								cFlt.setAc(aFlt.getAc());
								cFlt = this.flightManager.saveFlight(cFlt);
								aFlt.setA_flt_id(cFlt.getFltId());
							}
						}
						serializableObject.setObjectTemplate(data);
						if(data.getSendToFArea()) jmsGateway.sendToClient("f_area_blackbox_" + data.getBlackboxID(), serializableObject);
						jmsGateway.sendToClient("ClientReceiveSArea", serializableObject);
					}
					else if(serializableObject.getObjectTemplate() instanceof RCBkg){
						RCBkg aBkg = (RCBkg) serializableObject.getObjectTemplate();
						CentralBooking cBkg = new CentralBooking();
						
						if(aBkg.getStatusSync() != null && aBkg.getStatusSync().equals("unactivating")){
							cBkg.setBkgIdId(aBkg.getId());
							cBkg.setIsActive(0);
							this.flightManager.unactivatingCBkg(cBkg);
						}
						else{
							cBkg.setBkgId(aBkg.getBkgId());
							CentralFlights cf = new CentralFlights();
							cf.setFltId(aBkg.getFltId());
							cBkg.setCFlt(cf);
							cBkg.setAwbNo(aBkg.getAwbNo());
							cBkg.setPart(aBkg.getPart());
							cBkg.setAddId(aBkg.getAddId());
							cBkg.setF_id(aBkg.getFid());
							cBkg.setPrio(aBkg.getPrio());
							cBkg.setBkgStatus(aBkg.getBkgStatus());
							cBkg.setComId(aBkg.getComId());
							cBkg.setAnnId(aBkg.getAnnId());
							cBkg.setDept(aBkg.getDept());
							cBkg.setDest(aBkg.getDest());
							cBkg.setScc3lc(aBkg.getScc3lc());
							cBkg.setPcs(aBkg.getPcs());
							cBkg.setWgt(aBkg.getWgt());
							cBkg.setVol(aBkg.getVol());
							cBkg.setAcComp(aBkg.getAcComp());
							cBkg.setUld3lc(aBkg.getUld3lc());
							cBkg.setIsActive(aBkg.getIsActive());
							this.flightManager.saveBooking(cBkg);
						}
					}
				}
			}
			else if(message instanceof TextMessage){
				TextMessage objMsg = (TextMessage) message;
				System.out.println(objMsg.getText());
			}
		}
		catch(JMSException e){
			
		}
	}

	public IActernityGatewayServer getJmsGateway() {
		return jmsGateway;
	}

	public void setJmsGateway(IActernityGatewayServer jmsGateway) {
		this.jmsGateway = jmsGateway;
	}

	public IUserRegistrationManager getUserRegistration() {
		return userRegistration;
	}

	public void setUserRegistration(IUserRegistrationManager userRegistration) {
		this.userRegistration = userRegistration;
	}

	public IFlightManager getFlightManager() {
		return flightManager;
	}

	public void setFlightManager(IFlightManager flightManager) {
		this.flightManager = flightManager;
	}
}
