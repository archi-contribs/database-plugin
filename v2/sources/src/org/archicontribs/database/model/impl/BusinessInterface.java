package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessInterface<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessInterface
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessInterface extends com.archimatetool.model.impl.BusinessInterface implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessInterface() {
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
