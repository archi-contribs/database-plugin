package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CompositionRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.CompositionRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CompositionRelationship extends com.archimatetool.model.impl.CompositionRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CompositionRelationship() {
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
