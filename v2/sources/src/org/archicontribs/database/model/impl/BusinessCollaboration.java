package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessCollaboration<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessCollaboration
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessCollaboration extends com.archimatetool.model.impl.BusinessCollaboration implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessCollaboration() {
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
