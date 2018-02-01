package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends DistributionNetwork<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.DistributionNetwork
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DistributionNetwork extends com.archimatetool.model.impl.DistributionNetwork implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public DistributionNetwork() {
		super();
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
