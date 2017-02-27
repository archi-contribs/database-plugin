package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends DiagramModelReference<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelReference
 * @see org.archicontribs.database.IDBMetadata
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
