package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Capability<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Capability
 * @see org.archicontribs.database.IDBMetadata
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
