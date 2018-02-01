package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CommunicationNetwork<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.CommunicationNetwork
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CommunicationNetwork extends com.archimatetool.model.impl.CommunicationNetwork implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CommunicationNetwork() {
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
