package org.archicontribs.database.canvas;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends AccessRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModel
 * @see org.archicontribs.database.IDBMetadata
 */
public class CanvasModel extends com.archimatetool.canvas.model.impl.CanvasModel implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(CanvasModel.class);
	private DBMetadata dbMetadata;
	
	public CanvasModel() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new CanvasModel");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
