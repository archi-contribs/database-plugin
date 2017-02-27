package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends BusinessInterface<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessInterface
 * @see org.archicontribs.database.IDBMetadata
 */
public class BusinessInterface extends com.archimatetool.model.impl.BusinessInterface implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessInterface.class);
	private DBMetadata dbMetadata;
	
	public BusinessInterface() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessInterface");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
