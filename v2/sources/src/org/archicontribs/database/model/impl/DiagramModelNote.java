package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DiagramModelNote<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DiagramModelNote
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DiagramModelNote extends com.archimatetool.model.impl.DiagramModelNote implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DiagramModelNote.class);
	private DBMetadata dbMetadata;
	
	public DiagramModelNote() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DiagramModelNote");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
