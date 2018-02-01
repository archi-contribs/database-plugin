package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ServingRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ServingRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ServingRelationship extends com.archimatetool.model.impl.ServingRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ServingRelationship() {
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
