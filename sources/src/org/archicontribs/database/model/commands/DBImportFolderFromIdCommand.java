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

import org.archicontribs.database.DBException;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.gef.commands.Command;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;
import com.archimatetool.model.util.UUIDFactory;

/**
 * Command for importing a folder from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportFolderFromIdCommand extends Command implements IDBImportCommand {
	private static final DBLogger logger = new DBLogger(DBImportFolderFromIdCommand.class);

	private IFolder importedFolder= null; 

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private DBException exception = null;

	private DBArchimateModel model = null;

	private String id;
	private boolean mustCreateCopy;
	private boolean isNew;

	// new values that are retrieved from the database
	private HashMap<String, Object> newValues = null;
	private IFolder newParentFolder = null;

	// old values that need to be retain to allow undo
	private DBVersion oldInitialVersion;
	private DBVersion oldCurrentVersion;
	private DBVersion oldDatabaseVersion;
	private DBVersion oldLatestDatabaseVersion;
	private String oldDocumentation = null;
	private String oldName = null;
	private FolderType oldFolderType = null;
	//private Integer oldRootFolderType = null;
	private IFolder oldParentFolder = null;
	private ArrayList<DBProperty> oldProperties = null;
	private ArrayList<DBProperty> oldFeatures = null;

	/**
	 * Imports a folder into the model<br>
	 * @param importConnection connection to the database
	 * @param archimateModel model into which the folder will be imported
	 * @param mergedModelId ID of the model merged in the actual model, to search for its parent folder 
	 * @param parentFolder if a folder is provided, the folder will be created inside this parent folder. Else, we'll check in the database if the folder has already been part of this model in order to import it in the same parent folder.
	 * @param idToImport id of the folder to import
 	 * @param versionToImport version of the folder to import
	 * @param importMode specifies if the folder must be copied or shared
	 */
	public DBImportFolderFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, String mergedModelId, IFolder parentFolder, String idToImport, int versionToImport, DBImportMode importMode) {
		this.model = archimateModel;
		this.id = idToImport;
		
		if ( logger.isDebugEnabled() )
			logger.debug("   Importing folder id " + idToImport + " version " + versionToImport + " in " + importMode.getLabel()+".");
		
		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObjectFromDatabase(idToImport, "IFolder", versionToImport);
			
			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));
			
			if ( this.mustCreateCopy ) {
				this.newValues.put("id", UUIDFactory.createID(null));
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}
			
			if ( (parentFolder != null) && (archimateModel.getDBMetadata(parentFolder).getRootFolderType() == (int)this.newValues.get("root_type")) )
			    this.newParentFolder = parentFolder;
			else
				this.newParentFolder = importConnection.getLastKnownFolder(archimateModel, mergedModelId, "IFolder", this.id);

			if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
				setLabel("import folder");
			} else {
				if ( ((String)this.newValues.get("name")).length() > 20 )
					setLabel("import \""+((String)this.newValues.get("name")).substring(0,16)+"...\"");
				else
					setLabel("import \""+(String)this.newValues.get("name")+"\"");
			}
		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
			this.importedFolder = null;
			this.exception = new DBException("Failed to import folder from its ID");
            this.exception.initCause(err);
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
				this.importedFolder = IArchimateFactory.eINSTANCE.createFolder();

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedFolder);

				this.oldInitialVersion = dbMetadata.getInitialVersion();
				this.oldCurrentVersion = dbMetadata.getCurrentVersion();
				this.oldDatabaseVersion = dbMetadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = dbMetadata.getLatestDatabaseVersion();

				this.oldName = dbMetadata.getName();
				this.oldDocumentation = dbMetadata.getDocumentation();
				this.oldFolderType = dbMetadata.getFolderType();
				//this.oldRootFolderType = metadata.getRootFolderType();

				this.oldProperties = new ArrayList<DBProperty>();
				for ( IProperty prop: this.importedFolder.getProperties() ) {
					this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
				}
				
				this.oldFeatures = new ArrayList<DBProperty>();
				for ( IFeature feature: this.importedFolder.getFeatures() ) {
					this.oldFeatures.add(new DBProperty(feature.getName(), feature.getValue()));
				}

				this.oldParentFolder = dbMetadata.getParentFolder();

				this.isNew = false;
			}

			DBMetadata dbMetadata = this.model.getDBMetadata(this.importedFolder);

			if ( this.mustCreateCopy )
				dbMetadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()), null);
			else
				dbMetadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"), (String)this.newValues.get("created_by"));

			dbMetadata.setId((String)this.newValues.get("id"));
			dbMetadata.setName((String)this.newValues.get("name"));
			dbMetadata.getCurrentVersion().set(dbMetadata.getInitialVersion());
			dbMetadata.getDatabaseVersion().set(dbMetadata.getInitialVersion());
			dbMetadata.getLatestDatabaseVersion().set(dbMetadata.getInitialVersion());

			dbMetadata.setDocumentation((String)this.newValues.get("documentation"));
			dbMetadata.setFolderType((Integer)this.newValues.get("type"));
			//dbMetadata.setRootFolderType((Integer)this.newValues.get("root_type"));

			this.importedFolder.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedFolder.getProperties().add(prop);
    			}
			}

			if ( this.newParentFolder == null )
				dbMetadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedFolder));
			else
				dbMetadata.setParentFolder(this.newParentFolder);

			if ( this.isNew )
				this.model.countObject(this.importedFolder, false);

		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
		    this.exception = new DBException("Failed to import folder from its ID");
            this.exception.initCause(err);
		}
	}

	@Override
	public void undo() {
		if ( !this.commandHasBeenExecuted )
			return;

		if ( this.importedFolder != null ) {
			if ( this.isNew ) {
				// if the folder has been created by the execute() method, we just delete it
				// we can safely assume that the parent is another folder (and not the model) as root folders can be sync'ed but cannot be imported this way
				IFolder parentFolder = (IFolder)this.importedFolder.eContainer();
				if ( parentFolder != null )
					parentFolder.getFolders().remove(this.importedFolder);

				this.model.getAllFolders().remove(this.importedFolder.getId());
			} else {
				// else, we need to restore the old properties
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedFolder);

				dbMetadata.getInitialVersion().set(this.oldInitialVersion);
				dbMetadata.getCurrentVersion().set(this.oldCurrentVersion);
				dbMetadata.getDatabaseVersion().set(this.oldDatabaseVersion);
				dbMetadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

				dbMetadata.setName(this.oldName);
				dbMetadata.setDocumentation(this.oldDocumentation);
				dbMetadata.setFolderType(this.oldFolderType);
				//metadata.setRootFolderType(this.oldRootFolderType);

				dbMetadata.setParentFolder(this.oldParentFolder);

				this.importedFolder.getProperties().clear();
				for ( DBProperty oldProperty: this.oldProperties ) {
					IProperty newProperty = IArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldProperty.getKey());
					newProperty.setValue(oldProperty.getValue());
					this.importedFolder.getProperties().add(newProperty);
				}
				
				this.importedFolder.getFeatures().clear();
				for ( DBProperty oldFeature: this.oldFeatures ) {
					IFeature newFeature = IArchimateFactory.eINSTANCE.createFeature();
					newFeature.setName(oldFeature.getKey());
					newFeature.setValue(oldFeature.getValue());
					this.importedFolder.getFeatures().add(newFeature);
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
	public DBException getException() {
		return this.exception;
	}
}
