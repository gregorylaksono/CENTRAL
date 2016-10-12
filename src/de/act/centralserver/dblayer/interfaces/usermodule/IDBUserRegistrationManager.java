package de.act.centralserver.dblayer.interfaces.usermodule;


import de.act.common.types.staticobjects.RCAdd;

/**
 * @author Henry
 *
 */
public interface IDBUserRegistrationManager {

	Integer saveAddress(RCAdd rca);
	Integer getNextAddressID();
}
