package de.act.centralserver.applayer.core;

import de.act.centralserver.applayer.interfaces.IStateManager;
import de.act.centralserver.dblayer.interfaces.IDBStateManager;

/**
 * @author Martin Sachs & Henry
 * @since 1.0 - 02.11.2006
 * @update 02.11.2009
 */
public class StateManager implements IStateManager {
	private IDBStateManager dbStateManager;

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param dbStateManager
	 */
	public StateManager(IDBStateManager dbStateManager) {
		super();
		this.dbStateManager = dbStateManager;
	}

	/**
	 * @autor Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @return Returns the dbStateManager.
	 */
	public IDBStateManager getDbStateManager() {
		return dbStateManager;
	}

	/**
	 * @author Martin Sachs
	 * @since 1.0 - 20.11.2006
	 * @param dbStateManager The dbStateManager to set.
	 */
	public void setDbStateManager(IDBStateManager dbStateManager) {
		this.dbStateManager = dbStateManager;
	}
}
