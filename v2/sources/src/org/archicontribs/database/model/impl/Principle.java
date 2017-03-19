package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Principle<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Principle
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Principle extends com.archimatetool.model.impl.Principle implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Principle.class);
	private DBMetadata dbMetadata;
	
	public Principle() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Principle");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
