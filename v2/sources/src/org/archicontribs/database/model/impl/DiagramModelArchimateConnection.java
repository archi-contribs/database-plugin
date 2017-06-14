package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelArchimateConnection<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelArchimateConnection
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelArchimateConnection extends com.archimatetool.model.impl.DiagramModelArchimateConnection implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelArchimateConnection.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelArchimateConnection() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelArchimateConnection");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
