package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Plateau<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Plateau
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Plateau extends com.archimatetool.model.impl.Plateau implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Plateau() {
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
