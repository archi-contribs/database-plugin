package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelReference<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelReference
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelReference extends com.archimatetool.model.impl.DiagramModelReference implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelReference.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelReference() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelReference");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
