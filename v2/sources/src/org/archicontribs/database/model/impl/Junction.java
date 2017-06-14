package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Junction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Junction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Junction extends com.archimatetool.model.impl.Junction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Junction.class);
	private DBMetadata dbMetadata;
	
	public Junction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Junction");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
