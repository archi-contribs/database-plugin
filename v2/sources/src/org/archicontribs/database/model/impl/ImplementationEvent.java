package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ImplementationEvent<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ImplementationEvent
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ImplementationEvent extends com.archimatetool.model.impl.ImplementationEvent implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(ImplementationEvent.class);
	private DBMetadata dbMetadata;
	
	public ImplementationEvent() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ImplementationEvent");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
