package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Capability<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Capability
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Capability extends com.archimatetool.model.impl.Capability implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Capability() {
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
