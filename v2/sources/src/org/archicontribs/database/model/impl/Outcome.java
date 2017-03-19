package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Outcome<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Outcome
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Outcome extends com.archimatetool.model.impl.Outcome implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Outcome.class);
	private DBMetadata dbMetadata;
	
	public Outcome() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Outcome");
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
