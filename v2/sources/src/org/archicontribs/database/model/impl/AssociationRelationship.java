package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AssociationRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AssociationRelationship
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class AssociationRelationship extends com.archimatetool.model.impl.AssociationRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(AssociationRelationship.class);
	private DBMetadata dbMetadata;
	
	public AssociationRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new AssociationRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
