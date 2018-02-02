package org.archicontribs.database.model.impl.canvas;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CanvasModel<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModel
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CanvasModel extends com.archimatetool.canvas.model.impl.CanvasModel implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CanvasModel() {
		super();
		
		this.dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	@Override
    public DBMetadata getDBMetadata() {
		return this.dbMetadata;
	}
}
