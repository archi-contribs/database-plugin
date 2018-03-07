/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.archicontribs.database.data.DBVersion;
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
	DBVersion currentVersion = new DBVersion();
	DBVersion exportedVersion = new DBVersion();
	DBVersion databaseVersion = new DBVersion();
	DBVersion latestDatabaseVersion = new DBVersion();
	
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
	
	/**
	 * Version of the current element, as it is in the memory<br>
	 * 0 if the model has been loaded from an archimate file<br>
	 * != 0 if the model has been imported from a database 
	 */
	public DBVersion getCurrentVersion() {
		// Version of viewObject and viewConnections is the version of their parent view
	    if ( this.component!=null && this.parentDiagram!=null && !(this.component instanceof IDiagramModel) && (this.component instanceof IDiagramModelComponent || this.component instanceof IDiagramModelConnection) ) {
	        return ((IDBMetadata)this.parentDiagram).getDBMetadata().getCurrentVersion();
	    }
    
	    return this.currentVersion;
	}
	
	/**
	 * Version of element, as calculated during the export process.<br>
	 * Will be copied to currentVersion if the export process succeeds.
	 */
    public DBVersion getExportedVersion() {
        // Version of viewObject and viewConnections is the version of their parent view
        if ( this.component!=null && this.parentDiagram!=null && !(this.component instanceof IDiagramModel) && (this.component instanceof IDiagramModelComponent || this.component instanceof IDiagramModelConnection) ) {
            return ((IDBMetadata)this.parentDiagram).getDBMetadata().getExportedVersion();
        }
    
        return this.exportedVersion;
    }
	
	/**
	 * Version of element, as it is in the database version.
	 * 0 if the element is new and not present in the database model.
	 */
	public DBVersion getDatabaseVersion() {
		// Version of viewObject and viewConnections is the version of their parent view
	    if ( this.component!=null && this.parentDiagram!=null && !(this.component instanceof IDiagramModel) && (this.component instanceof IDiagramModelComponent || this.component instanceof IDiagramModelConnection) ) {
	        return ((IDBMetadata)this.parentDiagram).getDBMetadata().getDatabaseVersion();
	    }
    
	    return this.databaseVersion;
	}
	
	/**
	 * latest version of the element in the database.<br>
	 * != databaseVersion if the element has been updated in the database by another user.
	 */
    public DBVersion getLatestDatabaseVersion() {
        // Version of viewObject and viewConnections is the version of their parent view
        if ( this.component!=null && this.parentDiagram!=null && !(this.component instanceof IDiagramModel) && (this.component instanceof IDiagramModelComponent || this.component instanceof IDiagramModelConnection) ) {
            return ((IDBMetadata)this.parentDiagram).getDBMetadata().getLatestDatabaseVersion();
        }
    
        return this.latestDatabaseVersion;
    }
	
	/**
	 * Choices available when a conflict is detected in the database<br>
	 * <li><b>askUser</b> ask the user what he wishes to do</li>
	 * <li><b>doNotExport</b> do not export to the database</li>
	 * <li><b>exportToDatabase</b> export to the database</li>
	 * <li><b>importFromDatabase</b> replace the component with the version in the database</li>
	 */
	public enum CONFLICT_CHOICE {askUser, doNotExport, exportToDatabase, importFromDatabase}
	/**
	 * Stores the action that need to be done in case of a database conflict
	 * @see CONFLICT_CHOICE
	 */
	private CONFLICT_CHOICE conflictChoice = CONFLICT_CHOICE.askUser;
	
	public CONFLICT_CHOICE getConflictChoice() {
		return this.conflictChoice;
	}
	
	public void setConflictChoice(CONFLICT_CHOICE choice) {
		this.conflictChoice = choice;
	}
	
	public IDiagramModelComponent getParentDiagram() {
	    return this.parentDiagram;
	}
	
	public void setParentdiagram(IDiagramModelComponent parent) {
	    this.parentDiagram = parent;
	}
	
	public int getRootFolderType() {
		return this.rootFolderType;
	}
	
	public void setRootFolderType(int type) {
		this.rootFolderType = type;
	}
	
	/**
	 * Calculates the full name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\""
	 */
	public String getFullName() {
		return new StringBuilder(this.component.getClass().getSimpleName()).append(":\""+((INameable)this.component).getName()+"\"").toString();
	}
	
	/**
	 * Calculates the debug name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\"("+getId()+")"
	 */
	public String getDebugName() {
		return new StringBuilder(getFullName()).append("("+((IIdentifier)this.component).getId()+")").toString();
	}
}
