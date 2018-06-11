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
import java.util.List;

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
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * Command for importing a relationship from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportRelationshipFromIdCommand extends Command {
    private static final DBLogger logger = new DBLogger(DBImportRelationshipFromIdCommand.class);
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private IArchimateDiagramModel view = null;
    private IArchimateRelationship importedRelationship = null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    private boolean hasBeenCreated = false;
    
    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    
    private String oldDocumentation = null;
    private String oldName = null;
    private String oldStrength = null;
    private Integer oldAccessType = null;
    private IFolder oldFolder = null;
    private IArchimateConcept oldSource = null;
    private IArchimateConcept oldTarget = null;
    private ArrayList<DBPair<String, String>> oldProperties = null;
    private List<IDiagramModelConnection> createdViewConnections = null;
    
    
    /**
     * Imports a relationship into the model<br>
     * @param connection connection to the database
     * @param model model into which the relationship will be imported
     * @param relationshipId id of the relationship to import
     * @param relationshipVersion version of the relationship to import (0 if the latest version should be imported)
     */
    public DBImportRelationshipFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version) {
        this.importConnection = connection;
        this.model = model;
        this.id = id;
        this.version = version;
    }
    
    /**
     * @return the relationship that has been imported by the command (of course, the command must have been executed before)
     */
    public IArchimateRelationship getImportedRelationship() {
    	return this.importedRelationship;
    }
    
    /**
     * Imports a relationship into the model<br>
     * @param model model into which the relationship will be imported
     * @param view if a view is provided, then an ArchimateObject will be automatically created
     * @param id id of the relationship to import
     * @param version version of the relationship to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the relationship should be its original id
     */
    public DBImportRelationshipFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, IArchimateDiagramModel view, String id, int version, boolean mustCreateCopy) {
        this.importConnection = connection;
        this.model = model;
        this.view = view;
        this.id = id;
        this.version = version;
        this.mustCreateCopy = mustCreateCopy;
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
                logger.debug("   Importing a copy of relationship id "+this.id+".");
            else
                logger.debug("   Importing relationship id "+this.id+".");
        }

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"relationships WHERE id = r.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT version, class, name, documentation, source_id, target_id, strength, access_type, checksum, created_on FROM "+this.importConnection.getSchema()+"relationships r WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("Relationship with id="+this.id+" has not been found in the database.");
                throw new Exception("Relationship with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }
            
            DBMetadata metadata;

            if ( this.mustCreateCopy ) {
                this.importedRelationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                this.importedRelationship.setId(this.model.getIDAdapter().getNewID());

                // as the relationship has just been created, the undo will just need to drop it
                // so we do not need to save its properties
                this.hasBeenCreated = true;
                metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();
                
                metadata.getInitialVersion().setVersion(0);
                metadata.getInitialVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
                metadata.getCurrentVersion().setVersion(0);
                metadata.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));

                this.importConnection.importProperties(this.importedRelationship, this.id, result.getInt("version"));
            } else {
                this.importedRelationship = this.model.getAllRelationships().get(this.id);
                
                if ( this.importedRelationship == null ) {
                    this.importedRelationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                    this.importedRelationship.setId(this.id);
                    
                    // as the relationship has just been created, the undo will just need to drop it
                    // so we do not need to save its properties
                    this.hasBeenCreated = true;
                    metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();
                } else {
                    // the relationship already exists in the model and will be updated with information from the database
                    // we need to keep a value of all its properties to allow undo
                    metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();
                    
                    this.oldInitialVersion = metadata.getInitialVersion();
                    this.oldCurrentVersion = metadata.getCurrentVersion();
                    this.oldDatabaseVersion = metadata.getDatabaseVersion();
                    this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();
                    
                    this.oldName = metadata.getName();
                    this.oldDocumentation = metadata.getDocumentation();
                    this.oldStrength = metadata.getStrength();
                    this.oldAccessType = metadata.getAccessType();
                    
                    this.oldSource = metadata.getSource();
                    this.oldTarget = metadata.getTarget();
                    
					this.oldProperties = new ArrayList<DBPair<String, String>>();
					for ( IProperty prop: ((IProperties)this.importedRelationship).getProperties() ) {
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

                this.importConnection.importProperties(this.importedRelationship);
            }

            metadata.setName(result.getString("name"));
            metadata.setDocumentation(result.getString("documentation"));
            metadata.setStrength(result.getString("strength"));
            metadata.setAccessType(result.getInt("access_type"));
            
            IArchimateConcept source = this.model.getAllElements().get(result.getString("source_id"));
            if ( source == null ) source = this.model.getAllRelationships().get(result.getString("source_id"));
            metadata.setSource(source);
            
            IArchimateConcept target = this.model.getAllElements().get(result.getString("target_id"));
            if ( target == null ) target = this.model.getAllRelationships().get(result.getString("target_id"));
            metadata.setTarget(target);
            
            this.importConnection.setFolderToLastKnown(this.model, this.importedRelationship);

            // During the import of an individual relationship from the database, we check if objects or connections exist for the source and the target
            // and create the corresponding connections
            // TODO : make an option with this functionality that the user can choose if he want the connections or not
            if ( this.view != null && metadata.componentToConnectable(this.view).isEmpty() ) {
                this.createdViewConnections = new ArrayList<IDiagramModelConnection>();
                List<IConnectable> sourceConnections = metadata.componentToConnectable(this.view, this.importedRelationship.getSource());
                List<IConnectable> targetConnections = metadata.componentToConnectable(this.view, this.importedRelationship.getTarget());

                for ( IConnectable sourceConnection: sourceConnections ) {
                    for ( IConnectable targetConnection: targetConnections ) {
                        IDiagramModelArchimateConnection connection = ArchimateDiagramModelFactory.createDiagramModelArchimateConnection(this.importedRelationship);
                        this.createdViewConnections.add(connection);
                        
                        connection.setSource(sourceConnection);
                        sourceConnection.getSourceConnections().add(connection);
                        
                        connection.setTarget(targetConnection);
                        targetConnection.getTargetConnections().add(connection);
                    }
                }
            }

            if ( this.hasBeenCreated )
                this.model.countObject(this.importedRelationship, false, null);
        } catch (Exception e) {
            // TODO: find a way to advertise the user as exceptions cannot be thrown
            logger.error("Failed to import relationship !!!", e);
        }
        
        this.commandHasBeenExecuted = true;
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
    	
        // if a viewObject has been created, then we remove it from the view
        if ( (this.view != null) && (this.createdViewConnections != null) ) {
            for ( IDiagramModelConnection connection: this.createdViewConnections ) {
                IConnectable sourceConnection = connection.getSource();
                connection.setSource(null);
                sourceConnection.getSourceConnections().remove(connection);
                
                IConnectable targetConnection = connection.getTarget();
                connection.setTarget(null);
                targetConnection.getTargetConnections().remove(connection);
            }
        }
        
        if ( this.hasBeenCreated ) {
            // if the relationship has been created by the execute() method, we just delete it
            IFolder parentFolder = (IFolder)this.importedRelationship.eContainer();
            parentFolder.getElements().remove(this.importedRelationship);
            
            this.model.getAllRelationships().remove(this.importedRelationship.getId());
        } else {
            // else, we need to restore the old properties
            DBMetadata metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();
            
            metadata.getInitialVersion().set(this.oldInitialVersion);
            metadata.getCurrentVersion().set(this.oldCurrentVersion);
            metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
            metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);
            
            metadata.setName(this.oldName);
            metadata.setDocumentation(this.oldDocumentation);
            metadata.setStrength(this.oldStrength);
            metadata.setAccessType(this.oldAccessType);
            
            metadata.setSource(this.oldSource);
            metadata.setTarget(this.oldTarget);
            
            metadata.setParentFolder(this.oldFolder);
            
            this.importedRelationship.getProperties().clear();
            for ( DBPair<String, String> pair: this.oldProperties ) {
            	IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
            	prop.setKey(pair.getKey());
            	prop.setValue(pair.getValue());
            	this.importedRelationship.getProperties().add(prop);
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
        this.oldStrength = null;
        this.oldAccessType = null;
        
        this.oldSource = null;
        this.oldTarget = null;
        
        this.oldProperties = null;
        this.createdViewConnections = null;
        
        this.importedRelationship = null;
        this.model = null;
        this.view = null;
        this.id = null;
        this.importConnection = null;
    }
}
