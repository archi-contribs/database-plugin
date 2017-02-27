package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends WorkPackage<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SketchModelActor
 * @see org.archicontribs.database.IDBMetadata
 */
public class SketchModelActor extends com.archimatetool.model.impl.SketchModelActor implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(SketchModelActor.class);
	private DBMetadata dbMetadata;
	
	public SketchModelActor() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new SketchModelActor");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
