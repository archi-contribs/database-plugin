package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Requirement<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Requirement
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Requirement extends com.archimatetool.model.impl.Requirement implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Requirement() {
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
