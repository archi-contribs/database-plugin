package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Meaning<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Meaning
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Meaning extends com.archimatetool.model.impl.Meaning implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Meaning.class);
	private DBMetadata dbMetadata;
	
	public Meaning() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Meaning");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
