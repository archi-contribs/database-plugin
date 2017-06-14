package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Requirement<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Requirement
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Requirement extends com.archimatetool.model.impl.Requirement implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Requirement.class);
	private DBMetadata dbMetadata;
	
	public Requirement() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Requirement");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
