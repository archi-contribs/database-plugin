package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AssociationRelationship<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AssociationRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class AssociationRelationship extends com.archimatetool.model.impl.AssociationRelationship implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public AssociationRelationship() {
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
