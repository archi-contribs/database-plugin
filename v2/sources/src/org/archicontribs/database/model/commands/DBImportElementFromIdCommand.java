/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBPair;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.Command;
import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;

/**
 * Command for importing an element from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportElementFromIdCommand extends Command {
    private static final DBLogger logger = new DBLogger(DBImportElementFromIdCommand.class);
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private IArchimateDiagramModel view = null;
    private IArchimateElement importedElement = null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    @SuppressWarnings("unused")
    private boolean mustImportRelationships = false;
    private boolean elementHasBeenCreated = false;
    
    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    
    private String oldDocumentation = null;
    private String oldName = null;
    private String oldType = null;
    private IFolder oldFolder = null;
    private ArrayList<DBPair<String, String>> oldProperties = null;
    private IDiagramModelObject createdViewObject = null;
    
    
    /**
     * Imports an element into the model<br>
     * @param connection connection to the database
     * @param model model into which the element will be imported
     * @param elementId id of the element to import
     * @param elementVersion version of the element to import (0 if the latest version should be imported)
     */
    public DBImportElementFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version) {
        this.importConnection = connection;
        this.model = model;
        this.id = id;
        this.version = version;
    }
    
    /**
     * Imports an element into the model<br>
     * @param connection connection to the database
     * @param model model into which the element will be imported
     * @param view if a view is provided, then an ArchimateObject will be automatically created
     * @param id id of the element to import
     * @param version version of the element to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the element should be its original id
     * @param mustImportRelationships true if the relationships to and from  the newly created element must be imported as well  
     */
    public DBImportElementFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, IArchimateDiagramModel view, String id, int version, boolean mustCreateCopy, boolean mustImportRelationships) {
        this.importConnection = connection;
        this.model = model;
        this.view = view;
        this.id = id;
        this.version = version;
        this.mustCreateCopy = mustCreateCopy;
        this.mustImportRelationships = mustImportRelationships;
    }
    
    /**
     * @return the relationship that has been imported by the command (of course, the command must have been executed before)
     */
    public IArchimateElement getImportedElement() {
    	return this.importedElement;
    }

    @Override
    public boolean canExecute() {
        try {
            return (this.importConnection != null)
                    && (!this.importConnection.isClosed())
                    && (this.model != null)
                    && (this.id != null) ;
        } catch (@SuppressWarnings("unused") SQLException ign) {
            return false;
        }
    }
    
    @Override
    public void execute() {
    	if ( this.commandHasBeenExecuted )
    		return;		// we do not execute it twice
    	
        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of element id "+this.id+".");
            else
                logger.debug("   Importing element id "+this.id+".");
        }

        // TODO add an option to import elements recursively

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"elements WHERE id = e.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT DISTINCT version, class, name, documentation, type, checksum, created_on FROM "+this.importConnection.getSchema()+"elements e WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("Element with id="+this.id+" has not been found in the database.");
                throw new Exception("Element with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }
            
            DBMetadata metadata;

            if ( this.mustCreateCopy ) {
                this.importedElement = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                this.importedElement.setId(this.model.getIDAdapter().getNewID());

                // as the element has just been created, the undo will just need to drop it
                // so we do not need to save its properties
                this.elementHasBeenCreated = true;
                metadata = ((IDBMetadata)this.importedElement).getDBMetadata();
                
                metadata.getInitialVersion().setVersion(0);
                metadata.getInitialVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
                metadata.getCurrentVersion().setVersion(0);
                metadata.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));

                this.importConnection.importProperties(this.importedElement, this.id, result.getInt("version"));
            } else {
                this.importedElement = this.model.getAllElements().get(this.id);
                
                if ( this.importedElement == null ) {
                    this.importedElement = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                    this.importedElement.setId(this.id);
                    
                    // as the element has just been created, the undo will just need to drop it
                    // so we do not need to save its properties
                    this.elementHasBeenCreated = true;
                    metadata = ((IDBMetadata)this.importedElement).getDBMetadata();
                } else {
                    // the element already exists in the model and will be updated with information from the database
                    // we need to keep a value of all its properties to allow undo
                    metadata = ((IDBMetadata)this.importedElement).getDBMetadata();
                    
                    this.oldInitialVersion = metadata.getInitialVersion();
                    this.oldCurrentVersion = metadata.getCurrentVersion();
                    this.oldDatabaseVersion = metadata.getDatabaseVersion();
                    this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();
                    
                    this.oldName = metadata.getName();
                    this.oldDocumentation = metadata.getDocumentation();
                    this.oldType = metadata.getJunctionType();
                    
					this.oldProperties = new ArrayList<DBPair<String, String>>();
					for ( IProperty prop: this.importedElement.getProperties() ) {
						this.oldProperties.add(new DBPair<String, String>(prop.getKey(), prop.getValue()));
					}
					
                    this.oldFolder = metadata.getParentFolder();
                }


                
                metadata.getInitialVersion().setVersion(result.getInt("version"));
                metadata.getInitialVersion().setChecksum(result.getString("checksum"));
                metadata.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
                
                metadata.getCurrentVersion().set(metadata.getInitialVersion());
                metadata.getDatabaseVersion().set(metadata.getInitialVersion());
                metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

                this.importConnection.importProperties(this.importedElement);
            }

            metadata.setName(result.getString("name"));
            metadata.setDocumentation(result.getString("documentation"));
            metadata.setType(result.getString("type"));
            
            
            this.importConnection.setFolderToLastKnown(this.model, this.importedElement);

            if ( this.view != null && metadata.componentToConnectable(this.view).isEmpty() ) {
                    this.createdViewObject = ArchimateDiagramModelFactory.createDiagramModelArchimateObject(this.importedElement);
                    this.view.getChildren().add(this.createdViewObject);
                    this.model.countObject(this.createdViewObject, false, null);
            }

            if ( this.elementHasBeenCreated )
                this.model.countObject(this.importedElement, false, null);

            // this.importConnection.setCountElementsImported(this.importConnection.getCountElementsImported() + 1);
            /*
            if ( this.mustImportRelationships ) {
                // We import the relationships that source or target the element
                try ( ResultSet resultrelationship = this.importConnection.select("SELECT id, source_id, target_id FROM "+this.importConnection.getSchema()+"relationships WHERE source_id = ? OR target_id = ?", this.id, this.id) ) {
                    while ( resultrelationship.next() && resultrelationship.getString("id") != null ) {
                        // we import only relationships that do not exist
                        if ( this.model.getAllRelationships().get(resultrelationship.getString("id")) == null ) {
                            IArchimateElement sourceElement = this.model.getAllElements().get(resultrelationship.getString("source_id"));
                            IArchimateRelationship sourceRelationship = this.model.getAllRelationships().get(resultrelationship.getString("source_id"));
                            IArchimateElement targetElement = this.model.getAllElements().get(resultrelationship.getString("target_id"));
                            IArchimateRelationship targetRelationship = this.model.getAllRelationships().get(resultrelationship.getString("target_id"));

                            // we import only relations when both source and target are in the model
                            if ( (sourceElement!=null || sourceRelationship!=null) && (targetElement!=null || targetRelationship!=null) ) {
                                chain(new importRelationshipFromIdCommand(this.importConnection, this.model, this.view, resultrelationship.getString("id"), 0, false));
                            }
                        }
                    }
                }
                
                this.model.resolveSourceRelationships();
                this.model.resolveTargetRelationships();
            }
            */
        } catch (Exception e) {
            // TODO: find a way to advertise the user as exceptions cannot be thrown
            logger.error("Failed to import element !!!", e);
        }
        
        this.commandHasBeenExecuted = true;
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
    	
        // if a viewObject has been created, then we remove it from the view
        if ( (this.view != null) && (this.createdViewObject != null) ) {
            this.view.getChildren().remove(this.createdViewObject);
            this.model.getAllViewObjects().remove(this.createdViewObject.getId());
        }
        
        if ( this.elementHasBeenCreated ) {
            // if the element has been created by the execute() method, we just delete it
            IFolder parentFolder = (IFolder)this.importedElement.eContainer();
            parentFolder.getElements().remove(this.importedElement);
            
            this.model.getAllElements().remove(this.importedElement.getId());
        } else {
            // else, we need to restore the old properties
            DBMetadata metadata = ((IDBMetadata)this.importedElement).getDBMetadata();
            
            metadata.getInitialVersion().set(this.oldInitialVersion);
            metadata.getCurrentVersion().set(this.oldCurrentVersion);
            metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
            metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);
            
            metadata.setName(this.oldName);
            metadata.setDocumentation(this.oldDocumentation);
            metadata.setType(this.oldType);
            
            metadata.setParentFolder(this.oldFolder);
            
            this.importedElement.getProperties().clear();
            for ( DBPair<String, String> pair: this.oldProperties ) {
            	IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
            	prop.setKey(pair.getKey());
            	prop.setValue(pair.getValue());
            	this.importedElement.getProperties().add(prop);
            }
        }
        
        this.commandHasBeenExecuted = false;
    }

    @Override
    public void dispose() {
        this.oldInitialVersion = null;
        this.oldCurrentVersion = null;
        this.oldDatabaseVersion = null;
        this.oldLatestDatabaseVersion = null;
        
        this.oldName = null;
        this.oldDocumentation = null;
        this.oldType = null;
        
        this.oldProperties = null;
        this.createdViewObject = null;
        
        this.importedElement = null;
        this.model = null;
        this.view = null;
        this.id = null;
        this.importConnection = null;
    }
}
