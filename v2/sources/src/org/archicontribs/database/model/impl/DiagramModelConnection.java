package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelConnection<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelConnection
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelConnection extends com.archimatetool.model.impl.DiagramModelConnection implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelConnection() {
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
