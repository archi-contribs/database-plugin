package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Location<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Location
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Location extends com.archimatetool.model.impl.Location implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Location.class);
	private DBMetadata dbMetadata;
	
	public Location() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Location");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
