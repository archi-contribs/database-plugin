package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Driver<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Driver
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Driver extends com.archimatetool.model.impl.Driver implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Driver.class);
	private DBMetadata dbMetadata;
	
	public Driver() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Driver");
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
