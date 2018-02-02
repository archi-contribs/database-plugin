package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelArchimateObject<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelArchimateObject
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelArchimateObject extends com.archimatetool.model.impl.DiagramModelArchimateObject implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelArchimateObject() {
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
