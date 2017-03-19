package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Constraint<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Constraint
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Constraint extends com.archimatetool.model.impl.Constraint implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Constraint.class);
	private DBMetadata dbMetadata;
	
	public Constraint() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Constraint");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
