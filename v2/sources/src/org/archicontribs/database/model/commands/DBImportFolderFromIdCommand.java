/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
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
import com.archimatetool.model.IProperty;

/**
 * Command for importing a folder from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportFolderFromIdCommand extends Command implements IDBImportFromIdCommand {
    private static final DBLogger logger = new DBLogger(DBImportFolderFromIdCommand.class);
    
    private IFolder importedFolder= null; 
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    private Exception exception = null;
    
    private DBArchimateModel model = null;

    private String id;
    private boolean mustCreateCopy;
    private boolean isNew;
    
    // new values that are retrieved from the database
    private HashMap<String, Object> newValues = null;
    private IFolder newFolder = null;
    
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
        this(connection, model, id, version, false);
    }
    
    /**
     * Imports a folder into the model<br>
     * @param connection connection to the database
     * @param model model into which the folder will be imported
     * @param id id of the folder to import
     * @param version version of the folder to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the folder should be its original id 
     */
    public DBImportFolderFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, String id, int version, boolean mustCreateCopy) {
        this.model = model;
        this.id = id;
        this.mustCreateCopy = mustCreateCopy;
        
        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of folder id "+this.id+".");
            else
                logger.debug("   Importing folder id "+this.id+".");
        }
        
        try {
            if ( !this.mustCreateCopy ) {
            	this.importedFolder = model.getAllFolders().get(this.id);
            	if ( this.importedFolder != null ) {
            		DBMetadata metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();
            		
                    this.oldInitialVersion = metadata.getInitialVersion();
                    this.oldCurrentVersion = metadata.getCurrentVersion();
                    this.oldDatabaseVersion = metadata.getDatabaseVersion();
                    this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();
                    
                    this.oldName = metadata.getName();
                    this.oldDocumentation = metadata.getDocumentation();
                    this.oldFolderType = metadata.getFolderType();
                    this.oldRootFolderType = metadata.getRootFolderType();
                    
                    this.oldProperties = new ArrayList<DBPair<String, String>>();
                    for ( IProperty prop: this.importedFolder.getProperties() ) {
                        this.oldProperties.add(new DBPair<String, String>(prop.getKey(), prop.getValue()));
                    }
                    
                    this.oldFolder = metadata.getParentFolder();
            	}
            }
            
            // we get the new values from the database to allow execute and redo
            this.newValues = importConnection.getObject(id, "IFolder", version);

            this.newFolder = importConnection.getLastKnownFolder(this.model, "IFolder", this.id);

            if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
                setLabel("import folder");
            } else {
                if ( ((String)this.newValues.get("name")).length() > 20 )
                    setLabel("import \""+((String)this.newValues.get("name")).substring(0,16)+"...\"");
                else
                    setLabel("import \""+(String)this.newValues.get("name")+"\"");
            }
        } catch (Exception err) {
            this.importedFolder = null;
            this.exception = err;
        }
    }

    @Override
    public void execute() {
    	if ( this.commandHasBeenExecuted )
    		return;		// we do not execute it twice
    	
    	this.commandHasBeenExecuted = true;
    	
    	try {
    		this.importedFolder = this.model.getAllFolders().get(this.id);
    		
    		if ( this.importedFolder == null ) {
    			this.isNew = true;
                this.importedFolder = DBArchimateFactory.eINSTANCE.createFolder();
    		} else
    			this.isNew = false;
    		
    		DBMetadata metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();
    		
    		if ( this.mustCreateCopy ) {
                metadata.setId(this.model.getIDAdapter().getNewID());
                metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
    		} else {
                metadata.setId((String)this.newValues.get("id"));
                metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));
    		}
    		
            metadata.getCurrentVersion().set(metadata.getInitialVersion());
            metadata.getDatabaseVersion().set(metadata.getInitialVersion());
            metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

            metadata.setName((String)this.newValues.get("name"));
            metadata.setDocumentation((String)this.newValues.get("documentation"));
            metadata.setFolderType((Integer)this.newValues.get("type"));
            metadata.setRootFolderType((Integer)this.newValues.get("root_type"));
            
            this.importedFolder.getProperties().clear();
            for ( String[] newProperty: (String[][])this.newValues.get("properties")) {
                IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
                prop.setKey(newProperty[0]);
                prop.setValue(newProperty[1]);
                this.importedFolder.getProperties().add(prop);
            }
            
            if ( this.newFolder == null )
                metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedFolder));
            else
                metadata.setParentFolder(this.newFolder);
            
            if ( this.isNew )
                this.model.countObject(this.importedFolder, false, null);
    		
        } catch (Exception err) {
            this.exception = err;
        }
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
    	
    	if ( this.importedFolder != null ) {
	        if ( this.isNew ) {
	            // if the folder has been created by the execute() method, we just delete it
	        	// we can safely assume that the parent is another folder (and not the mode) as root folders can be sync'ed but cannot be imported this way
	            IFolder parentFolder = (IFolder)this.importedFolder.eContainer();
	            if ( parentFolder != null )
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
    	}

        // we allow the command to be executed again
        this.commandHasBeenExecuted = false;
    }

    /**
     * @return the element that has been imported by the command (of course, the command must have been executed before)<br>
     * if the value is null, the exception that has been raised can be get using {@link getException}
     */
    @Override
    public IFolder getImported() {
        return this.importedFolder;
    }

    /**
     * @return the exception that has been raised during the import process, if any.
     */
    @Override
    public Exception getException() {
        return this.exception;
    }
}
