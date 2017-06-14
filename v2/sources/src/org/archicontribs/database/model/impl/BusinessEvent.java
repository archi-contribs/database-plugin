package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessEvent<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessEvent
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessEvent extends com.archimatetool.model.impl.BusinessEvent implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessEvent.class);
	private DBMetadata dbMetadata;
	
	public BusinessEvent() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessEvent");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
