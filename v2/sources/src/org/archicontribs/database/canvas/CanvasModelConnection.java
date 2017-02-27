package org.archicontribs.database.canvas;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends AccessRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModelConnection
 * @see org.archicontribs.database.IDBMetadata
 */
public class CanvasModelConnection extends com.archimatetool.canvas.model.impl.CanvasModelConnection implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(CanvasModelConnection.class);
	private DBMetadata dbMetadata;
	
	public CanvasModelConnection() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new CanvasModelConnection");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
