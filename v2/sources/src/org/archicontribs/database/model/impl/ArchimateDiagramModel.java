package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends ArchimateDiagramModel<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ArchimateDiagramModel
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ArchimateDiagramModel extends com.archimatetool.model.impl.ArchimateDiagramModel implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public ArchimateDiagramModel() {
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
