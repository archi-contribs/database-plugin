package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Deliverable<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Deliverable
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Deliverable extends com.archimatetool.model.impl.Deliverable implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Deliverable() {
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
