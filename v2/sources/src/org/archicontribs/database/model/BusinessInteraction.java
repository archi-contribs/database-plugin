package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends BusinessInteraction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessInteraction
 * @see org.archicontribs.database.IDBMetadata
 */
public class BusinessInteraction extends com.archimatetool.model.impl.BusinessInteraction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(BusinessInteraction.class);
	private DBMetadata dbMetadata;
	
	public BusinessInteraction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new BusinessInteraction");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
