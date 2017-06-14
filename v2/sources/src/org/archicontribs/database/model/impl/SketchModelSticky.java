package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends WorkPackage<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.SketchModelSticky
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class SketchModelSticky extends com.archimatetool.model.impl.SketchModelSticky implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(SketchModelSticky.class);
	private DBMetadata dbMetadata;
	
	public SketchModelSticky() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new SketchModelSticky");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
