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
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
