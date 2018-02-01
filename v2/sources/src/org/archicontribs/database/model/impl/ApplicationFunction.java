package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationFunction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationFunction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationFunction extends com.archimatetool.model.impl.ApplicationFunction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationFunction() {
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
