package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends DiagramModelObject<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelObject
 * @see org.archicontribs.database.IDBMetadata
 */
public class DiagramModelObject extends com.archimatetool.model.impl.DiagramModelObject implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelObject.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelObject() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelObject");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
