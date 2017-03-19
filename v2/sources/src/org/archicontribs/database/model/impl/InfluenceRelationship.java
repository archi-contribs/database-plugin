package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;;

/**
 * extends InfluenceRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.InfluenceRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class InfluenceRelationship extends com.archimatetool.model.impl.InfluenceRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(InfluenceRelationship.class);
	private DBMetadata dbMetadata;
	
	public InfluenceRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new InfluenceRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
