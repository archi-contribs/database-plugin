package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends SketchModelSticky<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SketchModelSticky
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SketchModelSticky extends com.archimatetool.model.impl.SketchModelSticky implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public SketchModelSticky() {
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
