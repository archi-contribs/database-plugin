package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Folder<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Folder
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Folder extends com.archimatetool.model.impl.Folder implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Folder() {
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
