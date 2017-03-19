package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelConnection<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelConnection
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelConnection extends com.archimatetool.model.impl.DiagramModelConnection implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelConnection.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelConnection() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelConnection");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
