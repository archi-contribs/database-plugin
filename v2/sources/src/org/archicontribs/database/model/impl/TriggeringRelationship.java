package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TriggeringRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TriggeringRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TriggeringRelationship extends com.archimatetool.model.impl.TriggeringRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(TriggeringRelationship.class);
	private DBMetadata dbMetadata;
	
	public TriggeringRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new TriggeringRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
