package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Gap<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Gap
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Folder extends com.archimatetool.model.impl.Folder implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Folder.class);
	private DBMetadata dbMetadata;
	
	public Folder() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Folder");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
