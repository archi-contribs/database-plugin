package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationInteraction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationInteraction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationInteraction extends com.archimatetool.model.impl.ApplicationInteraction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(ApplicationInteraction.class);
	private DBMetadata dbMetadata;
	
	public ApplicationInteraction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ApplicationInteraction");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
