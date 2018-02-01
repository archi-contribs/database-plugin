package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Meaning<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Meaning
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Meaning extends com.archimatetool.model.impl.Meaning implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Meaning() {
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
