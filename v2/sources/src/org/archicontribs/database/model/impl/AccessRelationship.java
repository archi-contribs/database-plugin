package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AccessRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AccessRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class AccessRelationship extends com.archimatetool.model.impl.AccessRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public AccessRelationship() {
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
