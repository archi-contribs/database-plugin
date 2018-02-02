package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends SystemSoftware<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SystemSoftware
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SystemSoftware extends com.archimatetool.model.impl.SystemSoftware implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public SystemSoftware() {
		super();
		
		this.dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	@Override
    public DBMetadata getDBMetadata() {
		return this.dbMetadata;
	}
}
