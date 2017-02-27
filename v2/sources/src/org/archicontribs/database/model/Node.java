package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends Node<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Node
 * @see org.archicontribs.database.IDBMetadata
 */
public class Node extends com.archimatetool.model.impl.Node implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(Node.class);
	private DBMetadata dbMetadata;
	
	public Node() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new Node");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
