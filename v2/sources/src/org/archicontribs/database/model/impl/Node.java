package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends Node<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.Node
 * @see org.archicontribs.database.model.IDBMetadata
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
