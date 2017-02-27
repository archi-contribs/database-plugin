package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends WorkPackage<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.WorkPackage
 * @see org.archicontribs.database.IDBMetadata
 */
public class WorkPackage extends com.archimatetool.model.impl.WorkPackage implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(WorkPackage.class);
	private DBMetadata dbMetadata;
	
	public WorkPackage() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new WorkPackage");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
