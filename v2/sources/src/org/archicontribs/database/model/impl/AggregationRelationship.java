package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AggregationRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AggregationRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class AggregationRelationship extends com.archimatetool.model.impl.AggregationRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public AggregationRelationship() {
		super();
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
