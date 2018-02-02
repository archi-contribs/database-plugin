package org.archicontribs.database.model.impl.canvas;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CanvasModelBlock<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.canvas.model.impl.CanvasModelBlock
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CanvasModelBlock extends com.archimatetool.canvas.model.impl.CanvasModelBlock implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CanvasModelBlock() {
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
