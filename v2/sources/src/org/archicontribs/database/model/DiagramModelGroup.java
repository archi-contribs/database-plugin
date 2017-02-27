package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends DiagramModelGroup<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelGroup
 * @see org.archicontribs.database.IDBMetadata
 */
public class DiagramModelGroup extends com.archimatetool.model.impl.DiagramModelGroup implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelGroup.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelGroup() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelGroup");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
