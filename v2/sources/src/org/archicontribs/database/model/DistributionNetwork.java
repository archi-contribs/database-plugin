package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends DistributionNetwork<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DistributionNetwork
 * @see org.archicontribs.database.IDBMetadata
 */
public class DistributionNetwork extends com.archimatetool.model.impl.DistributionNetwork implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(DistributionNetwork.class);
	private DBMetadata dbMetadata;
	
	public DistributionNetwork() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new DistributionNetwork");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
