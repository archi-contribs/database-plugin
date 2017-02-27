
package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends BusinessService<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessService
 * @see org.archicontribs.database.IDBMetadata
 */
public class BusinessService extends com.archimatetool.model.impl.BusinessService implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessService.class);
	private DBMetadata dbMetadata;
	
	public BusinessService() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessService");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
