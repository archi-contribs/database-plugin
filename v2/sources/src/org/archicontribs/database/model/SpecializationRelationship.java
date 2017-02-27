package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends TriggeringRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TriggeringRelationship
 * @see org.archicontribs.database.IDBMetadata
 */
public class SpecializationRelationship extends com.archimatetool.model.impl.SpecializationRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(SpecializationRelationship.class);
	private DBMetadata dbMetadata;
	
	public SpecializationRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new SpecializationRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
