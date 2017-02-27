package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends BusinessRole<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessRole
 * @see org.archicontribs.database.IDBMetadata
 */
public class BusinessRole extends com.archimatetool.model.impl.BusinessRole implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessRole.class);
	private DBMetadata dbMetadata;
	
	public BusinessRole() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessRole");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
