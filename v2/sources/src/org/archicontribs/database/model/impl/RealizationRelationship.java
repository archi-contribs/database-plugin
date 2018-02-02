package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends RealizationRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.RealizationRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class RealizationRelationship extends com.archimatetool.model.impl.RealizationRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public RealizationRelationship() {
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
