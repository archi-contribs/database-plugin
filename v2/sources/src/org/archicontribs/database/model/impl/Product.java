package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Product<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Product
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class Product extends com.archimatetool.model.impl.Product implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Product.class);
	private DBMetadata dbMetadata;
	
	public Product() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Product");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
