package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyCollaboration<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyCollaboration
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyCollaboration extends com.archimatetool.model.impl.TechnologyCollaboration implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TechnologyCollaboration() {
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
