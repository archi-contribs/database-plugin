/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBVersion;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.INameable;

import lombok.Getter;
import lombok.Setter;

/**
 * This class defines the metadata attached to every model components.
 * 
 * @author Herve Jouin 
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DBMetadata  {
    /**
     * Component that contains the DBMetadata<br>
     * This property is set during the component initialization and is used to calculate the component checksum
     */
    private EObject component;
    
    /**
     * Version of the component as it was during last import/export, or zero if it was loaded from an archimate file
     */
	@Getter DBVersion initialVersion = new DBVersion();
	
	
	/**
	 * Version of the component as it is now (recalculated on every import/export procedure)<br>
	 * Will be copied to initialVersion after a successful export
	 * 
	 */
	@Getter DBVersion currentVersion = new DBVersion();
	
	/**
	 * Version of the component as it is in the database version of the model
	 */
	@Getter DBVersion databaseVersion = new DBVersion();
	
	/**
	 * Latest version of the component in the database whichever the model that modified the component 
	 */
	@Getter DBVersion latestDatabaseVersion = new DBVersion();
	
	/**
	 * Parent diagram 
	 */
	@Getter @Setter private IDiagramModelComponent parentDiagram = null;
	
	/**
	 * Used by views, set to false if some components are removed during the export process so their checksum needs to be recalculated
	 */
	@Getter @Setter private boolean checksumValid = true;
	
	/**
	 * If the component is a folder, stores the type of its root folder<br>
	 * This is needed to determine what class of eObjects it is able to store<br>
	 * Folders created by the user have got a type of 0 (zero) but the root folder they are in limit the kind of objects they can store   
	 */
	@Getter @Setter private int rootFolderType;
	
	public DBMetadata(EObject component) {
		assert ( component instanceof IIdentifier );
		this.component = component;
	}
	
	/**
	 * Choices available when a conflict is detected in the database<br>
	 * <li><b>askUser</b> Ask the user what he wishes to do</li>
	 * <li><b>doNotExport</b> Do not export to the database</li>
	 * <li><b>exportToDatabase</b> Export to the database</li>
	 * <li><b>importFromDatabase</b> Replace the component with the version in the database</li>
	 */
	public enum CONFLICT_CHOICE {askUser, doNotExport, exportToDatabase, importFromDatabase}
	/**
	 * Stores the action that need to be done in case of a database conflict
	 * @see CONFLICT_CHOICE
	 */
	@Getter @Setter private CONFLICT_CHOICE conflictChoice = CONFLICT_CHOICE.askUser;
	
	/**
     * Gives a status of the component regarding it's database version:<br>
     * <li><b>isSynced</b></li> The component exists in both the database and the model, and they are in sync
     * <li><b>isNewInModel</b></li> The component exists in the model but not in the database<br>
     * <li><b>isUpdatedInDatabase</b></li> The component exists in both the model and the database but the database version has been updated since it has been imported
     * <li><b>isUpdatedInModel</b></li> The component exists in both the model and the database but the model version has been updated since it has been imported
     * <li><b>isDeletedInDatabase</b></li> The component exists in the database but is not associated to the model anymore
     * <li><b>isConflicting</b></li> The component has been updated in both the model and the database
     * <br><br>
     * <b><u>Limits:</u></b><br>
     * This status requires that the component exists in the model. Thus, it is not possible to calculate this way  
     * <li><b><strike>isNewInDatabase</strike></b></li> The component does not exist in the model but has been created in the database<br>
     * <li><b><strike>isDeletedInModel</strike></b></li> The component does not exist in the model because it has been deleted from the model
     */
    public enum DATABASE_STATUS {isSynced, isNewInModel, isUpadtedInDatabase, isUpdatedInModel, isDeletedInDatabase, IsConflicting}
    
    /**
     * Gets the status of the component<br>
     * - if the database version is zero --> isNewInModel (the component does not exist in the database so it is new in the model)<br>
     * @see COMPONENT_STATUS
     */
    public DATABASE_STATUS getDatabaseStatus() {
        // if database version is zero, it means that the component does not exist in the database version of the model
        // so it is a new component in the model
        if ( this.databaseVersion.getVersion() == 0 )
            return DATABASE_STATUS.isNewInModel;
        
        // if latest database version is zero, it means that the component did exist in the database version of the model,
        // but it does not exist anymore in the latest version of the database model,
        // so it means that it has been deleted from the database version of the model by another user
        if ( this.latestDatabaseVersion.getVersion() == 0 )
            return DATABASE_STATUS.isDeletedInDatabase;
        
        // if the latest database checksum equals the current checksum,
        // this means that the component is synced between the model and the database
        if ( DBPlugin.areEqual(this.latestDatabaseVersion.getChecksum(), this.currentVersion.getChecksum()) )
            return DATABASE_STATUS.isSynced;
        
        String initialChecksum = (this.component instanceof IDiagramModelContainer ) ? this.initialVersion.getContainerChecksum() : this.initialVersion.getChecksum();
        String currentChecksum = (this.component instanceof IDiagramModelContainer ) ? this.currentVersion.getContainerChecksum() : this.currentVersion.getChecksum();
        String databaseChecksum = (this.component instanceof IDiagramModelContainer ) ? this.databaseVersion.getContainerChecksum() : this.databaseVersion.getChecksum();
        
        // if the components checksum in the model has been modified since the component has been imported
        // this means that the component has been updated in the model
        boolean modifiedInModel = !DBPlugin.areEqual(initialChecksum, currentChecksum);
        
        // if the components checksum in the database has been modified since the component has been imported
        // this means that the component has been updated in the database 
        boolean modifiedInDatabase = !DBPlugin.areEqual(initialChecksum, databaseChecksum);
            
        // if both versions of the component (in the model and in the database) have been updated
        // then they are conflicting ...
        // ... except if the modifications are the same
        if ( modifiedInModel && modifiedInDatabase ) {
            if ( DBPlugin.areEqual(currentChecksum, databaseChecksum) )
                return DATABASE_STATUS.isSynced;

            return DATABASE_STATUS.IsConflicting;
        }
        
        // if we're here, it means that the componant has been upadted either in the model, either in the database
        if ( modifiedInModel )
            return DATABASE_STATUS.isUpdatedInModel;
        
        if ( modifiedInDatabase )
        	return DATABASE_STATUS.isUpadtedInDatabase;
        
        return DATABASE_STATUS.isSynced;
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
