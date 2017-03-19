package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyCollaboration<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyCollaboration
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyCollaboration extends com.archimatetool.model.impl.TechnologyCollaboration implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(TechnologyCollaboration.class);
	private DBMetadata dbMetadata;
	
	public TechnologyCollaboration() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new TechnologyCollaboration");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
