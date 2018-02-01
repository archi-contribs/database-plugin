package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationService<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationService
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationService extends com.archimatetool.model.impl.ApplicationService implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationService() {
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
