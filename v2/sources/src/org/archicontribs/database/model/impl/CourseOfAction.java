package org.archicontribs.database.model.impl;

import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;

/**
 * extends CourseOfAction<br>
 * implements IDBMetadata
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.CourseOfAction
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class CourseOfAction extends com.archimatetool.model.impl.CourseOfAction implements IDBMetadata {
	private DBMetadata dbMetadata;
	
	public CourseOfAction() {
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
