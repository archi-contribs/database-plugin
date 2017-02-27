package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends WorkPackage<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Value
 * @see org.archicontribs.database.IDBMetadata
 */
public class Value extends com.archimatetool.model.impl.Value implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Value.class);
	private DBMetadata dbMetadata;
	
	public Value() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Value");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
