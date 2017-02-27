package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends TechnologyInteraction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyInteraction
 * @see org.archicontribs.database.IDBMetadata
 */
public class TechnologyInteraction extends com.archimatetool.model.impl.TechnologyInteraction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(TechnologyInteraction.class);
	private DBMetadata dbMetadata;
	
	public TechnologyInteraction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new TechnologyInteraction");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
