package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Goal<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Goal
 * @see org.archicontribs.database.IDBMetadata
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
