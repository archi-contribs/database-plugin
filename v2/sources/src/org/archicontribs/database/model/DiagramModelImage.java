package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends DiagramModelImage<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.WorkPackage
 * @see org.archicontribs.database.IDBMetadata
 */
public class DiagramModelImage extends com.archimatetool.model.impl.DiagramModelImage implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelImage.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelImage() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelImage");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
