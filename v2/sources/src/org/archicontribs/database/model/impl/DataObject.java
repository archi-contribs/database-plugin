package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DataObject<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DataObject
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DataObject extends com.archimatetool.model.impl.DataObject implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DataObject.class);
	private DBMetadata dbMetadata;
	
	public DataObject() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DataObject");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
