package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelArchimateConnection<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelArchimateConnection
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelArchimateConnection extends com.archimatetool.model.impl.DiagramModelArchimateConnection implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelArchimateConnection() {
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
