/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.gef.commands.Command;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IProfile;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing a profile from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportProfileFromIdCommand extends Command implements IDBImportCommand {
	private static final DBLogger logger = new DBLogger(DBImportProfileFromIdCommand.class);

	private IProfile importedProfile= null; 

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private Exception exception = null;

	private DBArchimateModel model = null;

	private String id;
	private boolean mustCreateCopy;
	private boolean isNew;

	// new values that are retrieved from the database
	private HashMap<String, Object> newValues = null;

	// old values that need to be retain to allow undo
	private DBVersion oldInitialVersion;
	private DBVersion oldCurrentVersion;
	private DBVersion oldDatabaseVersion;
	private DBVersion oldLatestDatabaseVersion;
	private String oldName = null;
	private String oldConceptType = null;

	/**
	 * Imports a profile into the model<br>
	 * @param importConnection connection to the database
	 * @param archimateModel model into which the profile will be imported
	 * @param idToImport id of the profile to import
 	 * @param versionToImport version of the profile to import (0 if the latest version must be imported)
	 * @param importMode specifies if the profile must be copied or shared
	 */
	public DBImportProfileFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, String idToImport, int versionToImport, DBImportMode importMode) {
		this.model = archimateModel;
		this.id = idToImport;
		
		if ( logger.isDebugEnabled() )
			logger.debug("   Importing profile id " + idToImport + " version " + versionToImport + " in " + importMode.getLabel()+".");
		
		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObjectFromDatabase(idToImport, "IProfile", versionToImport);
			
			this.mustCreateCopy = importMode.shouldCreateCopy(null);
			
			if ( this.mustCreateCopy ) {
				this.newValues.put("id", DBPlugin.createID());
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}
			
			if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
				setLabel("import profile");
			} else {
				if ( ((String)this.newValues.get("name")).length() > 20 )
					setLabel("import \""+((String)this.newValues.get("name")).substring(0,16)+"...\"");
				else
					setLabel("import \""+(String)this.newValues.get("name")+"\"");
			}
		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
			this.importedProfile = null;
			this.exception = err;
		}
	}

	@Override
	public void execute() {
		if ( this.commandHasBeenExecuted )
			return;		// we do not execute it twice

		this.commandHasBeenExecuted = true;

		try {
			this.importedProfile = this.model.getAllProfiles().get(this.id);

			if ( this.importedProfile == null ) {
				this.importedProfile = IArchimateFactory.eINSTANCE.createProfile();
				this.model.getProfiles().add(this.importedProfile);

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedProfile);

				this.oldInitialVersion = dbMetadata.getInitialVersion();
				this.oldCurrentVersion = dbMetadata.getCurrentVersion();
				this.oldDatabaseVersion = dbMetadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = dbMetadata.getLatestDatabaseVersion();

				this.oldName = dbMetadata.getName();
				this.oldConceptType = dbMetadata.getConceptType();

				this.isNew = false;
			}

			DBMetadata dbMetadata = this.model.getDBMetadata(this.importedProfile);

			if ( this.mustCreateCopy )
				dbMetadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
			else
				dbMetadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));

			dbMetadata.setId((String)this.newValues.get("id"));
			dbMetadata.setName((String)this.newValues.get("name"));
			dbMetadata.setConceptType((String)this.newValues.get("conceptType"));
			
			dbMetadata.getCurrentVersion().set(dbMetadata.getInitialVersion());
			dbMetadata.getDatabaseVersion().set(dbMetadata.getInitialVersion());
			dbMetadata.getLatestDatabaseVersion().set(dbMetadata.getInitialVersion());
			
			if ( this.isNew )
				this.model.countObject(this.importedProfile, false);

		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
			this.exception = err;
		}
	}

	@Override
	public void undo() {
		if ( !this.commandHasBeenExecuted )
			return;

		if ( this.importedProfile != null ) {
			if ( this.isNew ) {
				// if the profile has been created by the execute() method, we just delete it
				this.model.getProfiles().remove(this.importedProfile);

				this.model.getAllProfiles().remove(this.importedProfile.getId());
			} else {
				// else, we need to restore the old properties
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedProfile);

				dbMetadata.getInitialVersion().set(this.oldInitialVersion);
				dbMetadata.getCurrentVersion().set(this.oldCurrentVersion);
				dbMetadata.getDatabaseVersion().set(this.oldDatabaseVersion);
				dbMetadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

				dbMetadata.setName(this.oldName);
				dbMetadata.setConceptType(this.oldConceptType);
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
	public IProfile getImported() {
		return this.importedProfile;
	}

	/**
	 * @return the exception that has been raised during the import process, if any.
	 */
	@Override
	public Exception getException() {
		return this.exception;
	}
}
