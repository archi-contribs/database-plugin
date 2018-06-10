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
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * Command for importing a folder from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportFolderFromIdCommand extends Command {
    private static final DBLogger logger = new DBLogger(DBImportFolderFromIdCommand.class);
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private IFolder importedFolder= null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    private boolean folderHasBeenCreated = false;
    
    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    
    private String oldDocumentation = null;
    private String oldName = null;
    private FolderType oldFolderType = null;
    private Integer oldRootFolderType = null;
    private IFolder oldFolder = null;
    private ArrayList<DBPair<String, String>> oldProperties = null;
    
    
    /**
     * Imports a folder into the model<br>
     * @param connection connection to the database
     * @param model model into which the folder will be imported
     * @param id id of the folder to import
     * @param version version of the folder to import (0 if the latest version should be imported)
     */
    public DBImportFolderFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version) {
        this.importConnection = connection;
        this.model = model;
        this.id = id;
        this.version = version;
    }
    
    /**
     * Imports a folder into the model<br>
     * @param connection connection to the database
     * @param model model into which the folder will be imported
     * @param id id of the folder to import
     * @param version version of the folder to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the folder should be its original id 
     */
    public DBImportFolderFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version, boolean mustCreateCopy) {
        this.importConnection = connection;
        this.model = model;
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
                logger.debug("*************************** Importing a copy of folder id "+this.id+".");
            else
                logger.debug("*************************** Importing folder id "+this.id+".");
        }

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"folders WHERE id = f.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT version, type, root_type, name, documentation, checksum, created_on FROM "+this.importConnection.getSchema()+"folders f WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("Folder with id="+this.id+" has not been found in the database.");
                throw new Exception("Folder with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }
            
            DBMetadata metadata;

            if ( this.mustCreateCopy ) {
                this.importedFolder = DBArchimateFactory.eINSTANCE.createFolder();
                this.importedFolder.setId(this.model.getIDAdapter().getNewID());

                // as the folder has just been created, the undo will just need to drop it
                // so we do not need to save its properties
                this.folderHasBeenCreated = true;
                metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();
                
                metadata.getInitialVersion().setVersion(0);
                metadata.getInitialVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
                metadata.getCurrentVersion().setVersion(0);
                metadata.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));

                this.importConnection.importProperties(this.importedFolder, this.id, result.getInt("version"));
            } else {
                this.importedFolder = this.model.getAllFolders().get(this.id);
                
                if ( this.importedFolder == null ) {
                    this.importedFolder = DBArchimateFactory.eINSTANCE.createFolder();
                    this.importedFolder.setId(this.id);
                    
                    // as the folder has just been created, the undo will just need to drop it
                    // so we do not need to save its properties
                    this.folderHasBeenCreated = true;
                    metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();
                } else {
                    // the folder already exists in the model and will be updated with information from the database
                    // we need to keep a value of all its properties to allow undo
                    metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();
                    
                    this.oldInitialVersion = metadata.getInitialVersion();
                    this.oldCurrentVersion = metadata.getCurrentVersion();
                    this.oldDatabaseVersion = metadata.getDatabaseVersion();
                    this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();
                    
                    this.oldName = metadata.getName();
                    this.oldDocumentation = metadata.getDocumentation();
                    this.oldFolderType = metadata.getFolderType();
                    this.oldRootFolderType = metadata.getRootFolderType();
                    
					this.oldProperties = new ArrayList<DBPair<String, String>>();
					for ( IProperty prop: ((IProperties)this.importedFolder).getProperties() ) {
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

                this.importConnection.importProperties(this.importedFolder);
            }

            metadata.setName(result.getString("name"));
            metadata.setDocumentation(result.getString("documentation"));
            metadata.setType(result.getString("type"));
            metadata.setRootFolderType(result.getInt("root_type"));
            
            this.importConnection.importProperties(this.importedFolder);
            
            this.importConnection.setFolderToLastKnown(this.model, this.importedFolder);

            if ( this.folderHasBeenCreated )
                this.model.countObject(this.importedFolder, false, null);
        } catch (Exception e) {
            // TODO: find a way to advertise the user as exceptions cannot be thrown
            logger.error("Failed to import folder !!!", e);
        }
        
        this.commandHasBeenExecuted = true;
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
    	
        if ( this.folderHasBeenCreated ) {
            // if the folder has been created by the execute() method, we just delete it
        	// we can safely assume that the parent is another folder (and not the mode) as root folders can be sync'ed but cannot be imported this way
            IFolder parentFolder = (IFolder)this.importedFolder.eContainer();
            parentFolder.getElements().remove(this.importedFolder);
            
            this.model.getAllFolders().remove(this.importedFolder.getId());
        } else {
            // else, we need to restore the old properties
            DBMetadata metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();
            
            metadata.getInitialVersion().set(this.oldInitialVersion);
            metadata.getCurrentVersion().set(this.oldCurrentVersion);
            metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
            metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);
            
            metadata.setName(this.oldName);
            metadata.setDocumentation(this.oldDocumentation);
            metadata.setFolderType(this.oldFolderType);
            metadata.setRootFolderType(this.oldRootFolderType);
            
            metadata.setParentFolder(this.oldFolder);
            
            this.importedFolder.getProperties().clear();
            for ( DBPair<String, String> pair: this.oldProperties ) {
            	IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
            	prop.setKey(pair.getKey());
            	prop.setValue(pair.getValue());
            	this.importedFolder.getProperties().add(prop);
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
        this.oldFolderType = null;
        
        this.oldProperties = null;
        
        this.importedFolder = null;
        this.model = null;
        this.id = null;
        this.importConnection = null;
    }
}
