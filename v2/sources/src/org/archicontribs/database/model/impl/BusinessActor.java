package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessActor<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessActor
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessActor extends com.archimatetool.model.impl.BusinessActor implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessActor() {
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
