package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Goal<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Goal
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Goal extends com.archimatetool.model.impl.Goal implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Goal.class);
	private DBMetadata dbMetadata;
	
	public Goal() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Goal");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
