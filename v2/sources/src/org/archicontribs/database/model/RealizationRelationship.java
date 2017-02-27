package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends RealizationRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.RealizationRelationship
 * @see org.archicontribs.database.IDBMetadata
 */
public class RealizationRelationship extends com.archimatetool.model.impl.RealizationRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(RealizationRelationship.class);
	private DBMetadata dbMetadata;
	
	public RealizationRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new RealizationRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
