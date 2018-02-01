package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Grouping<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Grouping
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Grouping extends com.archimatetool.model.impl.Grouping implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public Grouping() {
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
