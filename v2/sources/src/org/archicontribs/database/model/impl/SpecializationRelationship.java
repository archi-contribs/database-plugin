package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends SpecializationRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SpecializationRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SpecializationRelationship extends com.archimatetool.model.impl.SpecializationRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public SpecializationRelationship() {
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
