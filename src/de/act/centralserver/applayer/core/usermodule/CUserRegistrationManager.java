/**
 * 
 */
package de.act.centralserver.applayer.core.usermodule;

import org.apache.log4j.Logger;

import de.act.blackbox.actions.airline.CBookingControlAction;
import de.act.centralserver.applayer.interfaces.usermodule.IUserRegistrationManager;
import de.act.centralserver.dblayer.interfaces.usermodule.IDBUserRegistrationManager;
import de.act.common.types.staticobjects.RCAdd;
import de.act.common.types.staticobjects.RCAddAdd;

/**
 * @author Henry
 *
 */
public class CUserRegistrationManager implements IUserRegistrationManager{

	private IDBUserRegistrationManager dbUserRegistration;
	private static final Logger log = Logger.getLogger(CBookingControlAction.class);
	
	public CUserRegistrationManager(){
		
	}
	
	public void saveUserRegistration(RCAdd rca){
//		rsa.setS_id(dbUserRegistration.);
		rca.setAdd_id(dbUserRegistration.getNextAddressID());
		
		log.info("Email = " + rca.getEmailString());
		log.info("Street = " + rca.getStreet());
		log.info("Contact Name = " + rca.getContactName());
		log.info("No = " + rca.getNo());
		log.info("Telp = " + rca.getTelpString());
		
		this.dbUserRegistration.saveAddress(rca);
	}
	
	public IDBUserRegistrationManager getDbUserRegistration() {
		return dbUserRegistration;
	}

	public void setDbUserRegistration(IDBUserRegistrationManager dbUserRegistration) {
		this.dbUserRegistration = dbUserRegistration;
	}
}

/*public void onMessage(Message arg0){
	try{
		//boolean ret = true;
		if (arg0 instanceof ObjectMessage){
			ObjectMessage objMsg = (ObjectMessage) arg0;
			//ret = false;
			Serializable obj = objMsg.getObject();
			if (obj instanceof SerializableObject){
				SerializableObject serializableObject = (SerializableObject) obj;
				if(serializableObject.getObjectTemplate() instanceof RSAdd){
					RSAdd addressData = (RSAdd) serializableObject.getObjectTemplate();
					addressData.setAdd_id(dbUserRegistration.getNextAddressID().toString());
					dbUserRegistration.saveAddress(addressData);
				}
				//agent.init(central, dbTransferManager, dbBookingManager, dbStateManager, this);
				//ret = agent.run(); // run agent and wait for completion -alternativ: run agent as threads
				//agent.destroy(); // destroy when finish agent
			}
		}
		else if (arg0 instanceof TextMessage){
			TextMessage txtMsg = (TextMessage) arg0;
			// forward message to all clients
			if (this.central){
				this.sendFromCentral(address, txtMsg);
			}else{
				this.sendToBox(address, txtMsg);
			}
			ret = true;
		}
		else{
			ret = true;
		}
		if (ret) arg0.acknowledge();
	}
	catch(JMSException e){
		
	}
}*/
