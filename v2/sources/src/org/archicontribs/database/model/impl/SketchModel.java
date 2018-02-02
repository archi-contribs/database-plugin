package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends SketchModel<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SketchModel
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SketchModel extends com.archimatetool.model.impl.SketchModel implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public SketchModel() {
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
