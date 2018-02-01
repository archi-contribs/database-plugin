package org.archicontribs.database.model.impl.canvas;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CanvasModelImage<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModelImage
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CanvasModelImage extends com.archimatetool.canvas.model.impl.CanvasModelImage implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CanvasModelImage() {
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
