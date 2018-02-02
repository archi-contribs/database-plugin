package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessEvent<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessEvent
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessEvent extends com.archimatetool.model.impl.BusinessEvent implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessEvent() {
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
