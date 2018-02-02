package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends BusinessInteraction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.BusinessInteraction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class BusinessInteraction extends com.archimatetool.model.impl.BusinessInteraction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public BusinessInteraction() {
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
