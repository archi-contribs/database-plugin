package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CommunicationNetwork<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.CommunicationNetwork
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CommunicationNetwork extends com.archimatetool.model.impl.CommunicationNetwork implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(CommunicationNetwork.class);
	private DBMetadata dbMetadata;
	
	public CommunicationNetwork() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new CommunicationNetwork");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
