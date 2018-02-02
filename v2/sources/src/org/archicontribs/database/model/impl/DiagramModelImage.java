package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelImage<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelImage
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelImage extends com.archimatetool.model.impl.DiagramModelImage implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelImage() {
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
