package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelNote<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelNote
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelNote extends com.archimatetool.model.impl.DiagramModelNote implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DiagramModelNote() {
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
