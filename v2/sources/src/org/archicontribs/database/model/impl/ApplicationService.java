package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationService<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationService
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationService extends com.archimatetool.model.impl.ApplicationService implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(ApplicationService.class);
	private DBMetadata dbMetadata;
	
	public ApplicationService() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ApplicationService");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
