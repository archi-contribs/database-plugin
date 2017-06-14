package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessProcess<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessProcess
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessProcess extends com.archimatetool.model.impl.BusinessProcess implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessProcess.class);
	private DBMetadata dbMetadata;
	
	public BusinessProcess() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessProcess");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
