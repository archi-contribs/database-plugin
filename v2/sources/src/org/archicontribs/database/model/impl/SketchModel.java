package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends WorkPackage<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SketchModel
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SketchModel extends com.archimatetool.model.impl.SketchModel implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(SketchModel.class);
	private DBMetadata dbMetadata;
	
	public SketchModel() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new SketchModel");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
