package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Product<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Product
 * @see org.archicontribs.database.IDBMetadata
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
