package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Outcome<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Outcome
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Outcome extends com.archimatetool.model.impl.Outcome implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Outcome() {
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
