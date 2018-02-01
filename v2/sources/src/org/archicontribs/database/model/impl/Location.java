package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Location<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Location
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Location extends com.archimatetool.model.impl.Location implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Location() {
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
