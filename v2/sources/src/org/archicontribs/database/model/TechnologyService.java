package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends TechnologyService<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyService
 * @see org.archicontribs.database.IDBMetadata
 */
public class TechnologyService extends com.archimatetool.model.impl.TechnologyService implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(TechnologyService.class);
	private DBMetadata dbMetadata;
	
	public TechnologyService() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new TechnologyService");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
