package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Gap<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Gap
 * @see org.archicontribs.database.IDBMetadata
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
