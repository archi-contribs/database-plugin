package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Artifact<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Artifact
 * @see org.archicontribs.database.IDBMetadata
 */
public class Artifact extends com.archimatetool.model.impl.Artifact implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Artifact.class);
	private DBMetadata dbMetadata;
	
	public Artifact() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Artifact");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
