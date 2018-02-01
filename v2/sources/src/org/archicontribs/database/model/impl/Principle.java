package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Principle<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Principle
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Principle extends com.archimatetool.model.impl.Principle implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Principle() {
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
