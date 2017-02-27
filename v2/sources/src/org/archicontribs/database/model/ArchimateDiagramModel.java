package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBMetadata;
import org.archicontribs.database.IDBMetadata;

/**
 * extends AccessRelationship<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ArchimateDiagramModel
 * @see org.archicontribs.database.IDBMetadata
 */
public class ArchimateDiagramModel extends com.archimatetool.model.impl.ArchimateDiagramModel implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(ArchimateDiagramModel.class);
	private DBMetadata dbMetadata;
	
	public ArchimateDiagramModel() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ArchimateDiagramModel");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
