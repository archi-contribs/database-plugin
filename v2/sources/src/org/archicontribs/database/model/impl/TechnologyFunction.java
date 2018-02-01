package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyFunction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyFunction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyFunction extends com.archimatetool.model.impl.TechnologyFunction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TechnologyFunction() {
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
