package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Grouping<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Grouping
 * @see org.archicontribs.database.IDBMetadata
 */
public class Grouping extends com.archimatetool.model.impl.Grouping implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Grouping.class);
	private DBMetadata dbMetadata;
	
	public Grouping() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Grouping");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
