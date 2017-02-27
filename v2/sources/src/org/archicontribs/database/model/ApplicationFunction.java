package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends ApplicationFunction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationFunction
 * @see org.archicontribs.database.IDBMetadata
 */
public class ApplicationFunction extends com.archimatetool.model.impl.ApplicationFunction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(ApplicationFunction.class);
	private DBMetadata dbMetadata;
	
	public ApplicationFunction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ApplicationFunction");
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
