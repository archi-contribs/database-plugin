package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Path<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Path
 * @see org.archicontribs.database.IDBMetadata
 */
public class Path extends com.archimatetool.model.impl.Path implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Path.class);
	private DBMetadata dbMetadata;
	
	public Path() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Path");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
