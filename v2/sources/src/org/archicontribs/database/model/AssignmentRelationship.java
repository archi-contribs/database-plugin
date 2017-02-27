package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends AssignmentRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.AssignmentRelationship
 * @see org.archicontribs.database.IDBMetadata
 */
public class AssignmentRelationship extends com.archimatetool.model.impl.AssignmentRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(AssignmentRelationship.class);
	private DBMetadata dbMetadata;
	
	public AssignmentRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new AssignmentRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
