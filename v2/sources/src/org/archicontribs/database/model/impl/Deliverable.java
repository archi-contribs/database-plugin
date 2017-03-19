package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Deliverable<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Deliverable
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Deliverable extends com.archimatetool.model.impl.Deliverable implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Deliverable.class);
	private DBMetadata dbMetadata;
	
	public Deliverable() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Deliverable");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
