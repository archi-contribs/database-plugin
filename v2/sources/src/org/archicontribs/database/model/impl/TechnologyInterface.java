package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyInterface<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyInterface
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyInterface extends com.archimatetool.model.impl.TechnologyInterface implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TechnologyInterface() {
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
