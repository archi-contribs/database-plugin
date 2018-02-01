package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CanvasModelConnection<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModelConnection
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CanvasModelConnection extends com.archimatetool.canvas.model.impl.CanvasModelConnection implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CanvasModelConnection() {
		super();
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
