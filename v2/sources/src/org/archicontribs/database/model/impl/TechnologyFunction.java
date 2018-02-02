package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends TechnologyFunction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.TechnologyFunction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class TechnologyFunction extends com.archimatetool.model.impl.TechnologyFunction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public TechnologyFunction() {
		super();
		
		this.dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	@Override
    public DBMetadata getDBMetadata() {
		return this.dbMetadata;
	}
}
