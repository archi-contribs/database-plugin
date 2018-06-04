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
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;

/**
 * Command for importing an element from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportElementFromIdCommand extends CompoundCommand {
    private static final DBLogger logger = new DBLogger(DBImportElementFromIdCommand.class);
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private IArchimateDiagramModel view = null;
    private IArchimateElement element = null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    private boolean mustImportRelationships = false;
    private boolean hasBeenCreated = false;
    
    /**
     * Imports an element into the model<br>
     * @param view if a view is provided, then an ArchimateObject will be automatically created
     * @param elementId id of the element to import
     * @param elementVersion version of the element to import (0 if the latest version should be imported)
     */
    public DBImportElementFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version) {
        this.importConnection = connection;
        this.model = model;
        this.view = this.view;
        this.id = id;
        this.version = version;
    }
    
    /**
     * Imports an element into the model<br>
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
        IArchimateElement element;

        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("Importing a copy of element id "+this.id+".");
            else
                logger.debug("Importing element id "+this.id+".");
        }

        // TODO add an option to import elements recursively
        
        //TODO: If existing element, we must save old values to enable undo (if new element, this is not necessary)

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"elements WHERE id = e.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT version, class, name, documentation, type, checksum, created_on FROM "+this.importConnection.getSchema()+"elements e WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("Element with id="+this.id+" has not been found in the database.");
                throw new Exception("Element with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }

            if ( this.mustCreateCopy ) {
                element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                element.setId(this.model.getIDAdapter().getNewID());
                this.hasBeenCreated = true;

                setName(result.getString("name"));
                ((IDBMetadata)element).getDBMetadata().getInitialVersion().setVersion(0);
                ((IDBMetadata)element).getDBMetadata().getInitialVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
                ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(0);
                ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));

                importProperties(element, this.id, result.getInt("version"));
            } else {
                element = this.model.getAllElements().get(this.id);
                if ( element == null ) {
                    element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                    element.setId(this.id);
                    this.hasBeenCreated = true;
                }

                setName(element, result.getString("name"));
                ((IDBMetadata)element).getDBMetadata().getInitialVersion().setVersion(result.getInt("version"));
                ((IDBMetadata)element).getDBMetadata().getInitialVersion().setChecksum(result.getString("checksum"));
                ((IDBMetadata)element).getDBMetadata().getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
                ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
                ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setChecksum(result.getString("checksum"));
                ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
                ((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
                ((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
                ((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
                ((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version"));
                ((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
                ((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));

                importProperties(element);
            }

            setDocumentation(element, result.getString("documentation"));
            setType(element, result.getString("type"));

            if ( this.view != null && componentToConnectable(this.view, element).isEmpty() ) {
                this.view.getChildren().add(ArchimateDiagramModelFactory.createDiagramModelArchimateObject(element));
            }

            if ( hasJustBeenCreated )
                this.model.countObject(element, false, null);

            ++this.countElementsImported;

        }

        if ( this.mustImportRelationships ) {
            // We import the relationships that source or target the element
            try ( ResultSet resultrelationship = select("SELECT id, source_id, target_id FROM "+this.schema+"relationships WHERE source_id = ? OR target_id = ?", this.id, this.id) ) {
                while ( resultrelationship.next() && resultrelationship.getString("id") != null ) {
                    // we import only relationships that do not exist
                    if ( this.model.getAllRelationships().get(resultrelationship.getString("id")) == null ) {
                        IArchimateElement sourceElement = this.model.getAllElements().get(resultrelationship.getString("source_id"));
                        IArchimateRelationship sourceRelationship = this.model.getAllRelationships().get(resultrelationship.getString("source_id"));
                        IArchimateElement targetElement = this.model.getAllElements().get(resultrelationship.getString("target_id"));
                        IArchimateRelationship targetRelationship = this.model.getAllRelationships().get(resultrelationship.getString("target_id"));

                        // we import only relations when both source and target are in the model
                        if ( (sourceElement!=null || sourceRelationship!=null) && (targetElement!=null || targetRelationship!=null) ) {
                            importRelationshipFromId(this.model, this.view, resultrelationship.getString("id"), 0, false);
                        }
                    }
                }
            }
            
            this.model.resolveSourceRelationships();
            this.model.resolveTargetRelationships();
        }
    }
    
    @Override
    public void undo() {
        // Add the Child at old index position
        if ( this.viewObjectIndex != -1 ) {        // might have already been deleted by another process
            this.viewObjectParent.getChildren().add(this.viewObjectIndex, this.viewObject);
            
            // we restore the children to the viewObject
            for ( IDiagramModelObject child: this.viewObjectChildren ) {
                this.viewObjectParent.getChildren().remove(child);
                ((IDiagramModelContainer)this.viewObject).getChildren().add(child);
            }
            ((DBArchimateModel)this.model).getAllViewObjects().put(this.viewObject.getId(), this.viewObject);
        }
    }

    @Override
    public void dispose() {
        this.viewObjectParent = null;
        this.viewObject = null;
        this.viewObjectChildren = null;
    }
}
