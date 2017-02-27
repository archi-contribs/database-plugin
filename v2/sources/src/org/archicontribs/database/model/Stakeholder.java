package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Stakeholder<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Stakeholder
 * @see org.archicontribs.database.IDBMetadata
 */
public class Stakeholder extends com.archimatetool.model.impl.Stakeholder implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Stakeholder.class);
	private DBMetadata dbMetadata;
	
	public Stakeholder() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Stakeholder");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
