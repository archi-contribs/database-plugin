package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Meaning<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Meaning
 * @see org.archicontribs.database.IDBMetadata
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
