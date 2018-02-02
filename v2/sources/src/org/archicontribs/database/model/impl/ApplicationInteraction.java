package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ApplicationInteraction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ApplicationInteraction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ApplicationInteraction extends com.archimatetool.model.impl.ApplicationInteraction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ApplicationInteraction() {
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
