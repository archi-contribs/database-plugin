package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AssignmentRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AssignmentRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class AssignmentRelationship extends com.archimatetool.model.impl.AssignmentRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public AssignmentRelationship() {
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
