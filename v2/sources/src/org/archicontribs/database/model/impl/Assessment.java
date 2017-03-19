package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Assessment<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Assessment
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Assessment extends com.archimatetool.model.impl.Assessment implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Assessment.class);
	private DBMetadata dbMetadata;
	
	public Assessment() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Assessment");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
