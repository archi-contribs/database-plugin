package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AggregationRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AggregationRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class AggregationRelationship extends com.archimatetool.model.impl.AggregationRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(AggregationRelationship.class);
	private DBMetadata dbMetadata;
	
	public AggregationRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new AggregationRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
