/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.commands;

import java.util.HashMap;
import java.util.Map;

import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.connection.DBSelect;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.gef.commands.Command;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.util.Logger;

/**
 *  Check all the components in the database that have been move to a new folder and set them in the new folder<br>
 * <br>
 * This methods does nothing if the model is the latest in the database
 */
public class DBSetFolderToLastKnownCommand extends Command  implements IDBCommand {
    private Exception exception = null;
    
    private Map<EObject, IFolder> oldObjectsFolders = new HashMap<EObject, IFolder>();
    private Map<EObject, IFolder> newObjectsFolders = new HashMap<EObject, IFolder>();
    
    private DBArchimateModel dbModel;
    
    /**
     * @param model
     * @param importConnection
     */
    public DBSetFolderToLastKnownCommand(DBArchimateModel model, DBDatabaseImportConnection importConnection) {
    	this.dbModel = model;
    	
        try {
            try ( DBSelect result = new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT m2.element_id AS element_id, m2.parent_folder_id AS parent_folder_id"
                    + " FROM "+importConnection.getSchemaPrefix()+"elements_in_model m1"
                    + " JOIN "+importConnection.getSchemaPrefix()+"elements_in_model m2 ON m1.element_id = m2.element_id AND m1.model_id = m2.model_id"
                    + " WHERE m1.model_id = ? AND m1.model_version = ? AND m2.model_version = ? AND m1.parent_folder_id <> m2.parent_folder_id"
                    , model.getId()
                    , model.getInitialVersion().getVersion()
                    , model.getDatabaseVersion().getVersion()
                    ) ) {
                while (result.next() ) {
                    IArchimateElement element = model.getAllElements().get(result.getString("element_id"));
                	DBMetadata elementDbMetadata = model.getDBMetadata(element);
                    if ( element != null ) {
                        IFolder parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
                        if ( (parentFolder != null) && (parentFolder != elementDbMetadata.getParentFolder()) ) {
                            this.oldObjectsFolders.put(element, elementDbMetadata.getParentFolder());
                            this.newObjectsFolders.put(element, parentFolder);
                        }
                    }
                }
            }
    
            // relationships
            try ( DBSelect result = new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT m2.relationship_id AS relationship_id, m2.parent_folder_id AS parent_folder_id"
                    + " FROM "+importConnection.getSchema()+"relationships_in_model m1"
                    + " JOIN "+importConnection.getSchema()+"relationships_in_model m2 ON m1.relationship_id = m2.relationship_id AND m1.model_id = m2.model_id"
                    + " WHERE m1.model_id = ? AND m1.model_version = ? AND m2.model_version = ? AND m1.parent_folder_id <> m2.parent_folder_id"
                    , model.getId()
                    , model.getInitialVersion().getVersion()
                    , model.getDatabaseVersion().getVersion()
                    ) ) {
                while (result.next() ) {
                    IArchimateRelationship relationship = model.getAllRelationships().get(result.getString("relationship_id"));
                    DBMetadata relationshipDbMetadata = model.getDBMetadata(relationship);
                    if ( relationship != null ) {
                        IFolder parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
                        if ( (parentFolder != null) && (parentFolder != relationshipDbMetadata.getParentFolder()) ) {
                            this.oldObjectsFolders.put(relationship, relationshipDbMetadata.getParentFolder());
                            this.newObjectsFolders.put(relationship, parentFolder);
                        }
                    }
                }
            }
    
            // folders
            try ( DBSelect result = new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT m2.folder_id AS folder_id, m2.parent_folder_id AS parent_folder_id"
                    + " FROM "+importConnection.getSchema()+"folders_in_model m1"
                    + " JOIN "+importConnection.getSchema()+"folders_in_model m2 ON m1.folder_id = m2.folder_id AND m1.model_id = m2.model_id"
                    + " WHERE m1.model_id = ? AND m1.model_version = ? AND m2.model_version = ? AND m1.parent_folder_id <> m2.parent_folder_id"
                    , model.getId()
                    , model.getInitialVersion().getVersion()
                    , model.getDatabaseVersion().getVersion()
                    ) ) {
                while (result.next() ) {
                    IFolder folder = model.getAllFolders().get(result.getString("view_id"));
                    if ( (folder != null) ) {
                        IFolder parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
                        DBMetadata folderDbMetadata = model.getDBMetadata(folder);
                        if ( parentFolder != null && (parentFolder != folderDbMetadata.getParentFolder()) ) {
                            this.oldObjectsFolders.put(folder, folderDbMetadata.getParentFolder());
                            this.newObjectsFolders.put(folder, parentFolder);
                        }
                    }
                }
            }
    
            // views
            try ( DBSelect result = new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT m2.view_id AS view_id, m2.parent_folder_id AS parent_folder_id"
                    + " FROM "+importConnection.getSchema()+"views_in_model m1"
                    + " JOIN "+importConnection.getSchema()+"views_in_model m2 ON m1.view_id = m2.view_id AND m1.model_id = m2.model_id"
                    + " WHERE m1.model_id = ? AND m1.model_version = ? AND m2.model_version = ? AND m1.parent_folder_id <> m2.parent_folder_id"
                    , model.getId()
                    , model.getInitialVersion().getVersion()
                    , model.getDatabaseVersion().getVersion()
                    ) ) {
                while (result.next() ) {
                    IDiagramModel view = model.getAllViews().get(result.getString("view_id"));
                    if ( (view != null) ) {
                        IFolder parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
                        DBMetadata viewDbMetadata = model.getDBMetadata(view);
                        if ( parentFolder != null && (parentFolder != viewDbMetadata.getParentFolder()) ) {
                            this.oldObjectsFolders.put(view, viewDbMetadata.getParentFolder());
                            this.newObjectsFolders.put(view, parentFolder);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logError("Got Exception "+e.getMessage());
            this.exception = e;
        }
    }
    
    @Override
    public void execute() {
        for (Map.Entry<EObject, IFolder> newObjectEntry : this.newObjectsFolders.entrySet()) {
            this.dbModel.getDBMetadata(newObjectEntry.getKey()).setParentFolder(newObjectEntry.getValue());
        }
    }
    
    @Override
    public void undo() {
        for (Map.Entry<EObject, IFolder> oldObjectEntry : this.oldObjectsFolders.entrySet()) {
           this.dbModel.getDBMetadata(oldObjectEntry.getKey()).setParentFolder(oldObjectEntry.getValue());
        }
    }
    
    public boolean needsToBeExecuted() {
        return (this.newObjectsFolders.size() != 0) || (this.oldObjectsFolders.size() != 0);
    }
    
    @Override
    public Exception getException() {
        return this.exception;
    }
}
