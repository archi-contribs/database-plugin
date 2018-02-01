package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationCollaboration<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationCollaboration
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationCollaboration extends com.archimatetool.model.impl.ApplicationCollaboration implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationCollaboration() {
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
