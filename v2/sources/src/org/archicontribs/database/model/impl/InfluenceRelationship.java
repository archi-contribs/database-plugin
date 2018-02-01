package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends InfluenceRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.InfluenceRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class InfluenceRelationship extends com.archimatetool.model.impl.InfluenceRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public InfluenceRelationship() {
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
