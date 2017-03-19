package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelArchimateObject<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelArchimateObject
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelArchimateObject extends com.archimatetool.model.impl.DiagramModelArchimateObject implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelArchimateObject.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelArchimateObject() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelArchimateObject");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
