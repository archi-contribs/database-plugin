package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends SketchModelActor<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SketchModelActor
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SketchModelActor extends com.archimatetool.model.impl.SketchModelActor implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public SketchModelActor() {
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
