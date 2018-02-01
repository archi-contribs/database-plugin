package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessObject<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessObject
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessObject extends com.archimatetool.model.impl.BusinessObject implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessObject() {
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
