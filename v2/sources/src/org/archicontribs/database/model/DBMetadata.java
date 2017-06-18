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
	
	/**
	 * Status of the component regarding the database:
     * <li><b>isSynced</b> the component is sync'ed with the database (version identical as the one in the database</li>
     * <li><b>isUpdated</b> the component exists in the database but the version in memory has got updated values</li>
     * <li><b>isNew</b> the component does not exist in the database</li>
	 */
	public enum DATABASE_STATUS {isSynced, isUpdated, isNew};
	/**
     * Status of the component regarding the database
     * @see DATABASE_STATUS
     */
	private DATABASE_STATUS databaseStatus = DATABASE_STATUS.isNew;
	
	/**
	 * Current version of the component.<br>
	 * This property is set when the component is imported or after a successful export.
	 */
	private int currentVersion = 0;
	
	/**
	 * Exported version of the component.<br>
	 * This property is set during the export process and copied to the current version if the transaction is committed to the database.<br>
	 * When there is no conflict during export, exportedVersion = currentVersion +1, but is higher in case a conflict is detected
	 */
	private int exportedVersion = 0;
	
	/**
	 * Latest version of the component in the database.<br>
	 * This property is set during the export process when checking existing components and is used during the conflict resolution mechanism<br>
	 * When there is no conflict during export, exportedVersion = currentVersion +1, but is higher in case a conflict is detected
	 */
	private int databaseVersion = 0;
	
	/**
	 * Checksum of the component<br>
	 * This property is set when the component is imported or before an export.
	 */
	private String currentChecksum = "";
	
	/**
	 * Checksum of the component in the database<br>
	 * This property is retrieved from the database each time a connection is made to a database.
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
	
	public int getCurrentVersion() {
		return currentVersion;
	}
	
	public void setCurrentVersion(int version) {
		currentVersion = version;
	}
	
	public int getExportedVersion() {
	    if ( component!=null && parentDiagram!=null && !(component instanceof IDiagramModel) && (component instanceof IDiagramModelComponent || component instanceof IDiagramModelConnection) ) {
	        return ((IDBMetadata)parentDiagram).getDBMetadata().getExportedVersion();
	    }
    
	    return exportedVersion;
	}
	
	public void setExportedVersion(int version) {
		exportedVersion = version;
	}
	
	public int getDatabaseVersion() {
	    if ( component!=null && parentDiagram!=null && !(component instanceof IDiagramModel) && (component instanceof IDiagramModelComponent || component instanceof IDiagramModelConnection) ) {
	        return ((IDBMetadata)parentDiagram).getDBMetadata().getDatabaseVersion();
	    }
    
	    return databaseVersion;
	}
	
	public void setDatabaseVersion(int version) {
		databaseVersion = version;
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
	
	public CONFLICT_CHOICE getConflictChoice() {
		return conflictChoice;
	}
	
	public void setConflictChoice(CONFLICT_CHOICE choice) {
		conflictChoice = choice;
	}
	
	public void setDatabaseStatus(DATABASE_STATUS status) {
		databaseStatus = status;
	}
	
	public DATABASE_STATUS getDatabaseStatus() {
		return databaseStatus;
	}
	
	public boolean needsToBeExported() {
		return databaseStatus != DATABASE_STATUS.isSynced;
	}
	
	public String getCurrentChecksum() {
		return currentChecksum;
	}
	
	public void setCurrentChecksum(String checksum) {
		if ( logger.isTraceEnabled() ) logger.trace("setting checksum for "+getDebugName()+" : "+checksum);
		currentChecksum = checksum;
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
