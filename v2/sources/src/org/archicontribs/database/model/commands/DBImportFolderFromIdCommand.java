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
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.Command;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;
import com.archimatetool.model.util.UUIDFactory;

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
	//private Integer oldRootFolderType = null;
	private IFolder oldFolder = null;
	private ArrayList<DBProperty> oldProperties = null;

	/**
	 * Imports a folder into the model<br>
	 * @param connection connection to the database
	 * @param model model into which the folder will be imported
	 * @param folder if a folder is provided, the folder will be created inside this folder. Else, we'll check in the database if the view has already been part of this model in order to import it in the same folder.
	 * @param id id of the folder to import
 	 * @param version version of the folder to import
	 * @param importMode specifies if the folder must be copied or shared
	 */
	@SuppressWarnings("unchecked")
	public DBImportFolderFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, IFolder folder, String id, int version, DBImportMode importMode) {
		this.model = model;
		this.id = id;
		
		if ( logger.isDebugEnabled() )
			logger.debug("   Importing folder id " + " version " + version + " in " + importMode.getLabel()+".");
		
		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObject(id, "IFolder", version);
			
			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));
			
			if ( this.mustCreateCopy ) {
				this.newValues.put("id", UUIDFactory.createID(null));
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}
			
			if ( (folder != null) && (((IDBMetadata)folder).getDBMetadata().getRootFolderType() == (int)this.newValues.get("root_type")) )
			    this.newFolder = folder;
			else
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
		    Logger.logError("Got Exception "+err.getMessage());
			this.importedFolder = null;
			this.exception = err;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute() {
		if ( this.commandHasBeenExecuted )
			return;		// we do not execute it twice

		this.commandHasBeenExecuted = true;

		try {
			this.importedFolder = this.model.getAllFolders().get(this.id);

			if ( this.importedFolder == null ) {
				this.importedFolder = DBArchimateFactory.eINSTANCE.createFolder();

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();

				this.oldInitialVersion = metadata.getInitialVersion();
				this.oldCurrentVersion = metadata.getCurrentVersion();
				this.oldDatabaseVersion = metadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();

				this.oldName = metadata.getName();
				this.oldDocumentation = metadata.getDocumentation();
				this.oldFolderType = metadata.getFolderType();
				//this.oldRootFolderType = metadata.getRootFolderType();

				this.oldProperties = new ArrayList<DBProperty>();
				for ( IProperty prop: this.importedFolder.getProperties() ) {
					this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
				}

				this.oldFolder = metadata.getParentFolder();

				this.isNew = false;
			}

			DBMetadata metadata = ((IDBMetadata)this.importedFolder).getDBMetadata();

			if ( this.mustCreateCopy )
				metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
			else
				metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));

			metadata.setId((String)this.newValues.get("id"));
			metadata.setName((String)this.newValues.get("name"));
			metadata.getCurrentVersion().set(metadata.getInitialVersion());
			metadata.getDatabaseVersion().set(metadata.getInitialVersion());
			metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

			metadata.setDocumentation((String)this.newValues.get("documentation"));
			metadata.setFolderType((Integer)this.newValues.get("type"));
			//metadata.setRootFolderType((Integer)this.newValues.get("root_type"));

			this.importedFolder.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedFolder.getProperties().add(prop);
    			}
			}

			if ( this.newFolder == null )
				metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedFolder));
			else
				metadata.setParentFolder(this.newFolder);

			if ( this.isNew )
				this.model.countObject(this.importedFolder, false, null);

		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
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
				// we can safely assume that the parent is another folder (and not the model) as root folders can be sync'ed but cannot be imported this way
				IFolder parentFolder = (IFolder)this.importedFolder.eContainer();
				if ( parentFolder != null )
					parentFolder.getFolders().remove(this.importedFolder);

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
				//metadata.setRootFolderType(this.oldRootFolderType);

				metadata.setParentFolder(this.oldFolder);

				this.importedFolder.getProperties().clear();
				for ( DBProperty oldProperty: this.oldProperties ) {
					IProperty newProperty = DBArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldProperty.getKey());
					newProperty.setValue(oldProperty.getValue());
					this.importedFolder.getProperties().add(newProperty);
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
