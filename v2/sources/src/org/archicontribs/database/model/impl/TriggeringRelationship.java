package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TriggeringRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TriggeringRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TriggeringRelationship extends com.archimatetool.model.impl.TriggeringRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TriggeringRelationship() {
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
