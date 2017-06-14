package org.archicontribs.database.model.impl;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CourseOfAction<br>
 * implements IHasDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.CourseOfAction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CourseOfAction extends com.archimatetool.model.impl.CourseOfAction implements IDBMetadata {
	private static final DBLogger logger = new DBLogger(CourseOfAction.class);
	private DBMetadata dbMetadata;
	
	public CourseOfAction() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new CourseOfAction");
		
		dbMetadata = new DBMetadata(this);
	}
	
	/**
	 * Gets the DBMetadata of the object
	 */
	public DBMetadata getDBMetadata() {
		return dbMetadata;
	}
}
