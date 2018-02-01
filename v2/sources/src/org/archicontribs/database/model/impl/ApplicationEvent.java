package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationEvent<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationEvent
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationEvent extends com.archimatetool.model.impl.ApplicationEvent implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationEvent() {
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
