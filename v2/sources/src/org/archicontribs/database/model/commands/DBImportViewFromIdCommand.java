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
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBPair;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.Command;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * Command for importing a view from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewFromIdCommand extends Command {
    private static final DBLogger logger = new DBLogger(DBImportViewFromIdCommand.class);
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private IDiagramModel importedView= null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    private boolean mustImportViewContent = false;
    private boolean viewHasBeenCreated = false;
    
    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    
    private String oldName = null;
    private String oldDocumentation = null;
    private Integer oldConnectionRouterType = null;
    private String oldViewpoint = null;
    private Integer oldBackground = null;
    private String oldHintContent = null;
    private String oldHintTitle = null;
    private IFolder oldFolder = null;
    private ArrayList<DBPair<String, String>> oldProperties = null;
    
    
    /**
     * Imports a view into the model<br>
     * @param connection connection to the database
     * @param model model into which the view will be imported
     * @param view if a view is provided, then an ArchimateObject will be automatically created
     * @param id id of the view to import
     * @param version version of the view to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the view should be its original id 
     */
    public DBImportViewFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version, boolean mustCreateCopy, boolean mustImportViewContent) {
        this.importConnection = connection;
        this.model = model;
        this.id = id;
        this.version = version;
        this.mustCreateCopy = mustCreateCopy;
        this.mustImportViewContent = mustImportViewContent;
    }
    
    /**
     * @return the view that has been imported by the command (of course, the command must have been executed before)
     */
    public IDiagramModel getImportedView() {
    	return this.importedView;
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
                logger.debug("   Importing a copy of view id "+this.id+".");
            else
                logger.debug("   Importing view id "+this.id+".");
        }

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"views WHERE id = v.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT version, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, checksum, container_checksum, created_on FROM "+this.importConnection.getSchema()+"views v WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("View with id="+this.id+" has not been found in the database.");
                throw new Exception("View with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }
            
            DBMetadata metadata;

            if ( this.mustCreateCopy ) {
				if ( DBPlugin.areEqual(result.getString("class"), "CanvasModel") )
					this.importedView = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(result.getString("class"));
				else
					this.importedView = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                this.importedView.setId(this.model.getIDAdapter().getNewID());

                // as the view has just been created, the undo will just need to drop it
                // so we do not need to save its properties
                this.viewHasBeenCreated = true;
                metadata = ((IDBMetadata)this.importedView).getDBMetadata();
                
                metadata.getInitialVersion().setVersion(0);
                metadata.getInitialVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
                metadata.getCurrentVersion().setVersion(0);
                metadata.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));

                this.importConnection.importProperties(this.importedView, this.id, result.getInt("version"));
            } else {
                this.importedView = this.model.getAllViews().get(this.id);
                
                if ( this.importedView == null ) {
    				if ( DBPlugin.areEqual(result.getString("class"), "CanvasModel") )
    					this.importedView = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(result.getString("class"));
    				else
    					this.importedView = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
                    this.importedView.setId(this.id);
                    
                    // as the view has just been created, the undo will just need to drop it
                    // so we do not need to save its properties
                    this.viewHasBeenCreated = true;
                    metadata = ((IDBMetadata)this.importedView).getDBMetadata();
                } else {
                    // the view already exists in the model and will be updated with information from the database
                    // we need to keep a value of all its properties to allow undo
                    metadata = ((IDBMetadata)this.importedView).getDBMetadata();
                    
                    this.oldInitialVersion = metadata.getInitialVersion();
                    this.oldCurrentVersion = metadata.getCurrentVersion();
                    this.oldDatabaseVersion = metadata.getDatabaseVersion();
                    this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();
                    
                    this.oldName = metadata.getName();
                    this.oldDocumentation = metadata.getDocumentation();
                    this.oldConnectionRouterType = metadata.getConnectionRouterType();
                    this.oldViewpoint = metadata.getViewpoint();
                    this.oldBackground = metadata.getBackground();
                    this.oldHintContent = metadata.getHintContent();
                    this.oldHintTitle = metadata.getHintTitle();
                    
					for ( IProperty prop: ((IProperties)this.importedView).getProperties() ) {
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

                this.importConnection.importProperties(this.importedView);
            }

			metadata.setName(result.getString("name"));
			metadata.setDocumentation(result.getString("documentation"));
			metadata.setConnectionRouterType(result.getInt("connection_router_type"));

			metadata.setViewpoint(result.getString("viewpoint"));
			metadata.setBackground(result.getInt("background"));
			metadata.setHintContent(result.getString("hint_content"));
			metadata.setHintTitle(result.getString("hint_title"));
            
            this.importConnection.setFolderToLastKnown(this.model, this.importedView);
            
    		if ( this.mustImportViewContent ) {
    			// 2 : we import the objects and create the corresponding elements if they do not exist yet
    			//        importing an element will automatically import the relationships to and from this element
    			this.importConnection.prepareImportViewsObjects(((IIdentifier)this.importedView).getId(), ((IDBMetadata)this.importedView).getDBMetadata().getInitialVersion().getVersion());
    			while ( this.importConnection.importViewsObjects(this.model, this.importedView) ) {
    				// each loop imports an object
    			}

    			// 3 : we import the connections and create the corresponding relationships if they do not exist yet
    			this.importConnection.prepareImportViewsConnections(((IIdentifier)this.importedView).getId(), ((IDBMetadata)this.importedView).getDBMetadata().getInitialVersion().getVersion());
    			while ( this.importConnection.importViewsConnections(this.model) ) {
    				// each loop imports a connection
    			}

    			this.model.resolveSourceConnections();
    			this.model.resolveTargetConnections();
    		}

            if ( this.viewHasBeenCreated )
                this.model.countObject(this.importedView, false, null);
        } catch (Exception e) {
            // TODO: find a way to advertise the user as exceptions cannot be thrown
            logger.error("Failed to import view !!!", e);
        }
        
        this.commandHasBeenExecuted = true;
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
    	
        if ( this.viewHasBeenCreated ) {
            // TODO: removing the view is not sufficient. We must delete all the elements and relationships imported while the view has been imported
            IFolder parentFolder = (IFolder)this.importedView.eContainer();
            parentFolder.getElements().remove(this.importedView);
            
            this.model.getAllFolders().remove(this.importedView.getId());
        } else {
            // else, we need to restore the old properties
            DBMetadata metadata = ((IDBMetadata)this.importedView).getDBMetadata();
            
            metadata.getInitialVersion().set(this.oldInitialVersion);
            metadata.getCurrentVersion().set(this.oldCurrentVersion);
            metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
            metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);
            
			metadata.setName(this.oldName);
			metadata.setDocumentation(this.oldDocumentation);
			metadata.setConnectionRouterType(this.oldConnectionRouterType);

			metadata.setViewpoint(this.oldViewpoint);
			metadata.setBackground(this.oldBackground);
			metadata.setHintContent(this.oldHintContent);
			metadata.setHintTitle(this.oldHintTitle);
            
            metadata.setParentFolder(this.oldFolder);
            
            this.importedView.getProperties().clear();
            ((IProperties)this.importedView).getProperties().clear();
            for ( DBPair<String, String> pair: this.oldProperties ) {
            	IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
            	prop.setKey(pair.getKey());
            	prop.setValue(pair.getValue());
            	((IProperties)this.importedView).getProperties().add(prop);
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
        this.oldConnectionRouterType = null;
        this.oldViewpoint = null;
        this.oldBackground = null;
        this.oldHintContent = null;
        this.oldHintTitle = null;
        
        this.oldProperties = null;
        
        this.importedView = null;
        this.model = null;
        this.id = null;
        this.importConnection = null;
    }
}
