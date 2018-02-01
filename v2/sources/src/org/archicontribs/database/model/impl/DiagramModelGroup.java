package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelGroup<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelGroup
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelGroup extends com.archimatetool.model.impl.DiagramModelGroup implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelGroup() {
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
