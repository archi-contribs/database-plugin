package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyEvent<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyEvent
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyEvent extends com.archimatetool.model.impl.TechnologyEvent implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(TechnologyEvent.class);
	private DBMetadata dbMetadata;
	
	public TechnologyEvent() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new TechnologyEvent");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
