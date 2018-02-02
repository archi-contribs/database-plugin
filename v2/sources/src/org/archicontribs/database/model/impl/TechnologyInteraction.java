package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyInteraction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyInteraction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyInteraction extends com.archimatetool.model.impl.TechnologyInteraction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TechnologyInteraction() {
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
