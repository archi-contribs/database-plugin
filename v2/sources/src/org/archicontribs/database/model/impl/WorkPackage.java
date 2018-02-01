package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends WorkPackage<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.WorkPackage
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class WorkPackage extends com.archimatetool.model.impl.WorkPackage implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public WorkPackage() {
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
