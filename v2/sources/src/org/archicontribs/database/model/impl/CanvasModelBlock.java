package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends AccessRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModelBlock
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CanvasModelBlock extends com.archimatetool.canvas.model.impl.CanvasModelBlock implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(CanvasModelBlock.class);
	private DBMetadata dbMetadata;
	
	public CanvasModelBlock() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new CanvasModelBlock");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
