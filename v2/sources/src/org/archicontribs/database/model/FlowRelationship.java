package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends FlowRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.FlowRelationship
 * @see org.archicontribs.database.IDBMetadata
 */
public class FlowRelationship extends com.archimatetool.model.impl.FlowRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(FlowRelationship.class);
	private DBMetadata dbMetadata;
	
	public FlowRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new FlowRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
