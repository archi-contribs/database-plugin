package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CanvasModelSticky<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModelSticky
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CanvasModelSticky extends com.archimatetool.canvas.model.impl.CanvasModelSticky implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CanvasModelSticky() {
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
