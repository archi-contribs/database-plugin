package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Facility<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Facility
 * @see org.archicontribs.database.IDBMetadata
 */
public class Facility extends com.archimatetool.model.impl.Facility implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Facility.class);
	private DBMetadata dbMetadata;
	
	public Facility() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Facility");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
