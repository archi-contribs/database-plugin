package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Material<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Material
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Material extends com.archimatetool.model.impl.Material implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Material.class);
	private DBMetadata dbMetadata;
	
	public Material() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Material");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
