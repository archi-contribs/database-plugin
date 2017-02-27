package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends CompositionRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.CompositionRelationship
 * @see org.archicontribs.database.IDBMetadata
 */
public class CompositionRelationship extends com.archimatetool.model.impl.CompositionRelationship implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(CompositionRelationship.class);
	private DBMetadata dbMetadata;
	
	public CompositionRelationship() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new CompositionRelationship");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
