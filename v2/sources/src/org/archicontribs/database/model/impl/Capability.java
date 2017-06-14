package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Capability<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Capability
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Capability extends com.archimatetool.model.impl.Capability implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Capability.class);
	private DBMetadata dbMetadata;
	
	public Capability() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Capability");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
