package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyProcess<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyProcess
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyProcess extends com.archimatetool.model.impl.TechnologyProcess implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TechnologyProcess() {
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
