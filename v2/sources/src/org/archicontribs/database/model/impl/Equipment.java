package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Equipment<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Equipment
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Equipment extends com.archimatetool.model.impl.Equipment implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Equipment() {
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
