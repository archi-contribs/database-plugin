package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Contract<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Contract
 * @see org.archicontribs.database.IDBMetadata
 */
public class Contract extends com.archimatetool.model.impl.Contract implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Contract.class);
	private DBMetadata dbMetadata;
	
	public Contract() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Contract");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
