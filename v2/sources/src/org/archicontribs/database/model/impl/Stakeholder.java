package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Stakeholder<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Stakeholder
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Stakeholder extends com.archimatetool.model.impl.Stakeholder implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Stakeholder() {
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
