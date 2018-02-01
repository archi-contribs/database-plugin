package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationInterface<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationInterface
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationInterface extends com.archimatetool.model.impl.ApplicationInterface implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationInterface() {
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
