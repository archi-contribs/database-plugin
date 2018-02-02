package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelReference<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelReference
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelReference extends com.archimatetool.model.impl.DiagramModelReference implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelReference() {
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
