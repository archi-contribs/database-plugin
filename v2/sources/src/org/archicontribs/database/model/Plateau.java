package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Plateau<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Plateau
 * @see org.archicontribs.database.IDBMetadata
 */
public class Plateau extends com.archimatetool.model.impl.Plateau implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Plateau.class);
	private DBMetadata dbMetadata;
	
	public Plateau() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Plateau");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
