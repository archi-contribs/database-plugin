package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessRole<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessRole
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessRole extends com.archimatetool.model.impl.BusinessRole implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessRole() {
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
