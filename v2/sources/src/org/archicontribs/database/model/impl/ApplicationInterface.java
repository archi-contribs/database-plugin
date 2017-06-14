package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationInterface<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationInterface
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationInterface extends com.archimatetool.model.impl.ApplicationInterface implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(ApplicationInterface.class);
	private DBMetadata dbMetadata;
	
	public ApplicationInterface() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ApplicationInterface");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
