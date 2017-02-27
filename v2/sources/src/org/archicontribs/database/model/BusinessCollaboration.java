package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends BusinessCollaboration<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessCollaboration
 * @see org.archicontribs.database.IDBMetadata
 */
public class BusinessCollaboration extends com.archimatetool.model.impl.BusinessCollaboration implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessCollaboration.class);
	private DBMetadata dbMetadata;
	
	public BusinessCollaboration() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessCollaboration");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
