package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Equipment<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Equipment
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Equipment extends com.archimatetool.model.impl.Equipment implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Equipment.class);
	private DBMetadata dbMetadata;
	
	public Equipment() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Equipment");
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
