package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyFunction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyFunction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyFunction extends com.archimatetool.model.impl.TechnologyFunction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(TechnologyFunction.class);
	private DBMetadata dbMetadata;
	
	public TechnologyFunction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new TechnologyFunction");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
