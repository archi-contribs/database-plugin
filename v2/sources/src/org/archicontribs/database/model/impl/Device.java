package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Device<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Device
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Device extends com.archimatetool.model.impl.Device implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Device.class);
	private DBMetadata dbMetadata;
	
	public Device() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Device");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
