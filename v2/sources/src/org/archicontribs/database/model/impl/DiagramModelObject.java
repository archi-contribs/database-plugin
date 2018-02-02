package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelObject<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelObject
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelObject extends com.archimatetool.model.impl.DiagramModelObject implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelObject() {
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
