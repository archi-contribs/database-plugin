package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelGroup<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelGroup
 * @see org.archicontribs.database.model.IDBMetadata
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
