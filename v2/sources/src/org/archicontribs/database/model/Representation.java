package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Representation<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Representation
 * @see org.archicontribs.database.IDBMetadata
 */
public class Representation extends com.archimatetool.model.impl.Representation implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Representation.class);
	private DBMetadata dbMetadata;
	
	public Representation() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Representation");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
