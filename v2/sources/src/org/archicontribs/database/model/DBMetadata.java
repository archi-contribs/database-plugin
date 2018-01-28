/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.sql.Timestamp;

import org.archicontribs.database.DBLogger;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.INameable;

/**
 * This class defines the metadata attached to every model components.
 * 
 * @author Herve Jouin 
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DBMetadata  {
	private static final DBLogger logger = new DBLogger(ArchimateModel.class);
	
	/**
	 * Initial version of the component.<br>
	 * This property is set when the component is imported or after a successful export.<br>
	 * Is 0 when the model has been loaded from an archimate file.
	 */
	private int initialVersion = 0;
	
	/**
	 * Current version of the component.<br>
	 * This property is calculated during the export process: currentVersion = initialVersion +1 when no conflict is detected, but may be higher in case of a conflict<br>
	 * The value is copied to initialVersion if the export to the database transaction is committed.
	 */
	private int currentVersion = 0;
	
	/**
	 * Latest version of the component in the database.<br>
	 * This property is set during the export process when checking existing components and is used during the conflict resolution mechanism
	 */
	private int databaseVersion = 0;
	
	/**
	 * Initial checksum of the component<br><br>
	 * This property is set when the component is imported or after a successful export.<br><br>
	 * Is Empty when the model has been loaded from an archimate file.
	 */
	private String initialChecksum = "";
	
	/**
	 * Gets the {@link #initialChecksum} of the component
	 */
	public String getInitialChecksum() {
		return initialChecksum;
	}
	
	/**
	 * Gets the {@link #initialChecksum} of the component
	 */
	public void setInitialChecksum(String checksum) {
		if ( logger.isTraceEnabled() ) logger.trace("setting initial checksum for "+getDebugName()+" : "+checksum);
		initialChecksum = checksum;
	}
	
	/**
	 * Checksum of the component<br><br>
	 * This property is calculated before each export and compared to initialChecksum to know if the component has been modified since last import or export.<br><br>
	 * The value is copied to {@link #initialChecksum} if the export to the database transaction is committed.
	 */
	private String currentChecksum = "";
	
	/**
	 * Gets the {@link #currentChecksum} of the component
	 */
	public String getCurrentChecksum() {
		return currentChecksum;
	}
	
	/**
	 * Gets the {@link #currentChecksum} of the component
	 */
	public void setCurrentChecksum(String checksum) {
		if ( logger.isTraceEnabled() ) logger.trace("setting current checksum for "+getDebugName()+" : "+checksum);
		currentChecksum = checksum;
	}
	
	/**
	 * Checksum of the component in the database<br>
	 * This property is retrieved from the database in syncMode or when a conflict is detected.
	 */
	private String databaseChecksum = "";
	
	/**
	 * Username of the last person who exported the component in the database
	 */
	private String databaseCreatedBy = null;
	
	/**
	 * Timestamp of the component in the database
	 */
	private Timestamp databaseCreatedOn = null;
	
	/**
	 * diagram 
	 */
	private IDiagramModelComponent parentDiagram = null;
	
	/**
	 * Component that contains the DBMetadata<br>
	 * This property is set during the component initialization and is used to calculate the component checksum
	 */
	private EObject component;
	
	/**
	 * If the component is a folder, stores the type of its root folder<br>
	 * This is needed to determine what class of eObjects it is able to store<br>
	 * Folders created by the user have got a type of 0 (zero) but the root folder they are in limit the kind of objects they can store   
	 */
	private int rootFolderType;
	
	public DBMetadata(EObject component) {
		assert ( component instanceof IIdentifier );
		this.component = component;
	}
	
	public int getInitialVersion() {
		return initialVersion;
	}
	
	public void setInitialVersion(int version) {
		initialVersion = version;
	}
	
	public int getCurrentVersion() {
		// Version of viewObject and viewConnections is the version of their parent view
	    if ( component!=null && parentDiagram!=null && !(component instanceof IDiagramModel) && (component instanceof IDiagramModelComponent || component instanceof IDiagramModelConnection) ) {
	        return ((IDBMetadata)parentDiagram).getDBMetadata().getCurrentVersion();
	    }
    
	    return currentVersion;
	}
	
	public void setCurrentVersion(int version) {
		// Version of viewObject and viewConnections is the version of their parent view
		if ( component!=null && parentDiagram!=null && !(component instanceof IDiagramModel) && (component instanceof IDiagramModelComponent || component instanceof IDiagramModelConnection) ) {
	        ((IDBMetadata)parentDiagram).getDBMetadata().setCurrentVersion(version);
	    }
		
		currentVersion = version;
	}
	
	public int getDatabaseVersion() {
		// Version of viewObject and viewConnections is the version of their parent view
	    if ( component!=null && parentDiagram!=null && !(component instanceof IDiagramModel) && (component instanceof IDiagramModelComponent || component instanceof IDiagramModelConnection) ) {
	        return ((IDBMetadata)parentDiagram).getDBMetadata().getDatabaseVersion();
	    }
    
	    return databaseVersion;
	}
	
	public void setDatabaseVersion(int version) {
		// Version of viewObject and viewConnections is the version of their parent view
		if ( component!=null && parentDiagram!=null && !(component instanceof IDiagramModel) && (component instanceof IDiagramModelComponent || component instanceof IDiagramModelConnection) ) {
	        ((IDBMetadata)parentDiagram).getDBMetadata().setDatabaseVersion(version);
	    }
		
		databaseVersion = version;
	}
	
	/**
	 * Status that the component can have regarding the database
	 * <li><b>unknown</b> the database version is unknown</li>
	 * <li><b>identical</b> the component has got the same version than in the database</li>
	 * <li><b>newer</b> the component has changed since last import/export, but not the database version</li>
	 * <li><b>older</b> the current version has not changed since last import/export, but the database version has</li>
	 * <li><b>conflict</b> the component has changed since last import/export, and the database version as well</li>
	 */
	//public enum STATUS {unknown, identical, newer, older, conflict};
	
	/**
	 * Calculates the status of the component regarding its database version
	 * @see STATUS
	 */
	/*
	public STATUS getStatus() {
		if ( databaseVersion == 0 || databaseChecksum.isEmpty() )
			return STATUS.unknown;									// the database version is unknown
		
		if ( currentChecksum.equals(databaseChecksum) )
			return STATUS.identical;								// the component has got the same version than in the database
		
		if ( currentChecksum.equals(initialChecksum) )
			return STATUS.older;									// the current version has not changed since last import/export, but the database version has
		
		if ( initialChecksum.equals(databaseChecksum) )
			return STATUS.newer;									// the component has changed since last import/export, but not the database version
		
		return STATUS.conflict;										// the component has changed since last import/export, and the database version as well
	}
	*/
	
	/**
	 * Choices available when a conflict is detected in the database<br>
	 * <li><b>askUser</b> ask the user what he wishes to do</li>
	 * <li><b>doNotExport</b> do not export to the database</li>
	 * <li><b>exportToDatabase</b> export to the database</li>
	 * <li><b>importFromDatabase</b> replace the component with the version in the database</li>
	 */
	public enum CONFLICT_CHOICE {askUser, doNotExport, exportToDatabase, importFromDatabase};
	/**
	 * Stores the action that need to be done in case of a database conflict
	 * @see CONFLICT_CHOICE
	 */
	private CONFLICT_CHOICE conflictChoice = CONFLICT_CHOICE.askUser;
	
	public CONFLICT_CHOICE getConflictChoice() {
		return conflictChoice;
	}
	
	public void setConflictChoice(CONFLICT_CHOICE choice) {
		conflictChoice = choice;
	}
	
	public String getDatabaseCreatedBy() {
		return databaseCreatedBy;
	}
	
	public void setDatabaseCreatedBy(String username) {
		databaseCreatedBy = username;
	}
	
	public Timestamp getDatabaseCreatedOn() {
		return databaseCreatedOn;
	}
	
	public void setDatabaseCreatedOn(Timestamp timestamp) {
		databaseCreatedOn = timestamp;
	}
	

	
	public String getDatabaseChecksum() {
		return databaseChecksum;
	}
	
	public void setDatabaseChecksum(String checksum) {
		databaseChecksum = checksum;
	}
	
	public IDiagramModelComponent getParentDiagram() {
	    return parentDiagram;
	}
	
	public void setParentdiagram(IDiagramModelComponent parent) {
	    parentDiagram = parent;
	}
	
	public int getRootFolderType() {
		return rootFolderType;
	}
	
	public void setRootFolderType(int type) {
		rootFolderType = type;
	}
	
	private String fullName = null;
	/**
	 * Calculates the full name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\""
	 */
	public String getFullName() {
		if ( fullName == null ) {
			StringBuilder objName = new StringBuilder(component.getClass().getSimpleName());
			objName.append(":\""+((INameable)component).getName()+"\"");
			fullName = objName.toString();
		}
		return fullName;
	}
	
	private String debugName = null;
	/**
	 * Calculates the debug name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\"("+getId()+")"
	 */
	public String getDebugName() {
		if ( debugName == null ) {
			StringBuilder objName = new StringBuilder(getFullName());
			objName.append("("+((IIdentifier)component).getId()+")");
			debugName = objName.toString();
		}
		return debugName;
	}
}
