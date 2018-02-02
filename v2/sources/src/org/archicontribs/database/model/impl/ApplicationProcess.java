package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationProcess<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationProcess
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationProcess extends com.archimatetool.model.impl.ApplicationProcess implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationProcess() {
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
