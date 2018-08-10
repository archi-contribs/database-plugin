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
import java.util.List;

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
import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing a relationship from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportRelationshipFromIdCommand extends Command implements IDBImportFromIdCommand {
	private static final DBLogger logger = new DBLogger(DBImportRelationshipFromIdCommand.class);

	private IArchimateRelationship importedRelationship = null;
	private List<IDiagramModelConnection> createdViewConnections = null;

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private Exception exception = null;

	private DBArchimateModel model = null;
	private IArchimateDiagramModel view = null;

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
	private String oldStrength = null;
	private Integer oldAccessType = null;
	private IFolder oldFolder = null;
	private IArchimateConcept oldSource = null;
	private IArchimateConcept oldTarget = null;
	private ArrayList<DBProperty> oldProperties = null;

	/**
	 * Imports a relationship into the model<br>
	 * @param model model into which the relationship will be imported
	 * @param view if a view is provided, then an ArchimateObject will be automatically created
	 * @param folder if a folder is provided, the relationship will be created inside this folder. Else, we'll check in the database if the view has already been part of this model in order to import it in the same folder.
	 * @param id id of the relationship to import
	 * @param importMode specifies if the relationship must be copied or shared
	 * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the relationship should be its original id
	 */
	@SuppressWarnings("unchecked")
	public DBImportRelationshipFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, IArchimateDiagramModel view, IFolder folder, String id, int version, DBImportMode importMode) {
		this.model = model;
		this.view = view;
		this.id = id;

		if ( logger.isDebugEnabled() )
			logger.debug("   Importing relationship id " + this.id + " version " + version + " in " + importMode.getLabel() + ((view != null) ? " into view."+view.getId() : "."));


		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObject(id, "IArchimateRelationship", version);
			
			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));
			
			if ( this.mustCreateCopy ) {
				String newId = this.model.getIDAdapter().getNewID();
				this.model.registerCopiedRelationship((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}

			if ( (folder != null) && (((IDBMetadata)folder).getDBMetadata().getRootFolderType() == DBMetadata.getDefaultFolderType((String)this.newValues.get("class"))) )
			    this.newFolder = folder;
			else
			    this.newFolder = importConnection.getLastKnownFolder(this.model, "IArchimateRelationship", this.id);

			if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
				setLabel("import relationship");
			} else {
				if ( ((String)this.newValues.get("name")).length() > 20 )
					setLabel("import \""+((String)this.newValues.get("name")).substring(0,16)+"...\"");
				else
					setLabel("import \""+(String)this.newValues.get("name")+"\"");
			}
		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
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
			this.importedRelationship = this.model.getAllRelationships().get(this.id);

			if ( (this.importedRelationship == null) || this.mustCreateCopy ) {
				this.importedRelationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create((String)this.newValues.get("class"));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();

				this.oldInitialVersion = metadata.getInitialVersion();
				this.oldCurrentVersion = metadata.getCurrentVersion();
				this.oldDatabaseVersion = metadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();

				this.oldName = metadata.getName();
				this.oldDocumentation = metadata.getDocumentation();
				this.oldStrength = metadata.getStrength();
				this.oldAccessType = metadata.getAccessType();

				this.oldSource = metadata.getRelationshipSource();
				this.oldTarget = metadata.getRelationshipTarget();

				this.oldProperties = new ArrayList<DBProperty>();
				for ( IProperty prop: this.importedRelationship.getProperties() ) {
					this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
				}

				this.oldFolder = metadata.getParentFolder();

				this.isNew = false;
			}

			DBMetadata metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();

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
			metadata.setStrength((String)this.newValues.get("strength"));
			metadata.setAccessType((Integer)this.newValues.get("access_type"));

			// The source of the relationship can be either an element or another relationship
			IArchimateConcept source = this.model.getAllElements().get(this.model.getNewElementId((String)this.newValues.get("source_id")));
			if ( source == null )
				source = this.model.getAllRelationships().get(this.model.getNewRelationshipId((String)this.newValues.get("source_id")));
			if ( source == null ) {
				// the source does not exist in the model, so we register it for later resolution
				this.model.registerSourceRelationship(this.importedRelationship, (String)this.newValues.get("source_id"));
			} else {
				// the source can be resolved right away
				metadata.setRelationshipSource(source);
			}

			// The target of the relationship can be either an element or another relationship
			IArchimateConcept target = this.model.getAllElements().get(this.model.getNewElementId((String)this.newValues.get("target_id")));
			if ( target == null )
				target = this.model.getAllRelationships().get(this.model.getNewRelationshipId((String)this.newValues.get("target_id")));
			if ( target == null ) {
				// the target does not exist in the model, so we register it for later resolution
				this.model.registerTargetRelationship(this.importedRelationship, (String)this.newValues.get("target_id"));
			} else {
				// the target can be resolved right away
				metadata.setRelationshipTarget(target);
			}

			this.importedRelationship.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedRelationship.getProperties().add(prop);
    			}
			}

			// During the import of an individual relationship from the database, we check if objects or connections exist for the source and the target
			// and create the corresponding connections
			// TODO: add an option that the user can choose if he wants the connections or not
			if ( this.view != null && metadata.findConnectables(this.view).isEmpty() ) {
				this.createdViewConnections = new ArrayList<IDiagramModelConnection>();
				List<IConnectable> sourceConnections = metadata.findConnectables(this.view, this.importedRelationship.getSource());
				List<IConnectable> targetConnections = metadata.findConnectables(this.view, this.importedRelationship.getTarget());

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

			if ( this.newFolder == null )
				metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedRelationship));
			else
				metadata.setParentFolder(this.newFolder);

			if ( this.isNew )
				this.model.countObject(this.importedRelationship, false, null);

		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
			this.exception = err;
		}
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

		if ( this.importedRelationship != null ) {
			if ( this.isNew ) {
				// if the relationship has been created by the execute() method, we just delete it
				IFolder parentFolder = (IFolder)this.importedRelationship.eContainer();
				if ( parentFolder != null )
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

				metadata.setRelationshipSource(this.oldSource);
				metadata.setRelationshipTarget(this.oldTarget);

				metadata.setParentFolder(this.oldFolder);

				this.importedRelationship.getProperties().clear();
				for ( DBProperty oldProperty: this.oldProperties ) {
					IProperty newProperty = DBArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldProperty.getKey());
					newProperty.setValue(oldProperty.getValue());
					this.importedRelationship.getProperties().add(newProperty);
				}
			}
		}

		// we allow the command to be executed again
		this.commandHasBeenExecuted = false;
	}

	/**
	 * @return the relationship that has been imported by the command (of course, the command must have been executed before)<br>
	 * if the value is null, the exception that has been raised can be get using {@link getException}
	 */
	@Override
	public IArchimateRelationship getImported() {
		return this.importedRelationship;
	}


	/**
	 * @return the exception that has been raised during the import process, if any.
	 */
	@Override
	public Exception getException() {
		return this.exception;
	}
}
