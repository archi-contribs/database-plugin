package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessObject<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessObject
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessObject extends com.archimatetool.model.impl.BusinessObject implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessObject.class);
	private DBMetadata dbMetadata;
	
	public BusinessObject() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessObject");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
