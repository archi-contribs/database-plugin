package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends BusinessActor<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessActor
 * @see org.archicontribs.database.IDBMetadata
 */
public class BusinessActor extends com.archimatetool.model.impl.BusinessActor implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessActor.class);
	private DBMetadata dbMetadata;
	
	public BusinessActor() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessActor");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
