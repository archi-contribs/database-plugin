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
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.gef.commands.Command;
import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing a relationship from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportRelationshipFromIdCommand extends Command implements IDBImportCommand {
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
	private IFolder newParentFolder = null;

	// old values that need to be retain to allow undo
	private DBVersion oldInitialVersion;
	private DBVersion oldCurrentVersion;
	private DBVersion oldDatabaseVersion;
	private DBVersion oldLatestDatabaseVersion;
	private String oldDocumentation = null;
	private String oldName = null;
	private String oldStrength = null;
	private Integer oldAccessType = null;
	private IFolder oldParentFolder = null;
	private IArchimateConcept oldSource = null;
	private IArchimateConcept oldTarget = null;
	private ArrayList<DBProperty> oldProperties = null;
	private ArrayList<DBProperty> oldFeatures = null;

	/**
	 * Imports a relationship into the model<br>
	 * @param importConnection connection to the database
	 * @param archimateModel model into which the relationship will be imported
	 * @param mergedModelId ID of the model merged in the actual model, to search for its parent folder 
	 * @param archimateDiagramModel if a view is provided, then an ArchimateObject will be automatically created
	 * @param parentFolder if a folder is provided, the relationship will be created inside this parent folder. Else, we'll check in the database if the relationship has already been part of this model in order to import it in the same parent folder.
	 * @param idToImport id of the relationship to import
	 * @param versionToImport version of the relationship to import (0 if the latest found in the database should be imported) 
	 * @param importMode specifies if the relationship must be copied or shared
	 */
	public DBImportRelationshipFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, IArchimateDiagramModel archimateDiagramModel, String mergedModelId, IFolder parentFolder, String idToImport, int versionToImport, DBImportMode importMode) {
		this.model = archimateModel;
		this.view = archimateDiagramModel;
		this.id = idToImport;
		
		DBMetadata dbMetadata = archimateModel.getDBMetadata(archimateDiagramModel);

		if ( logger.isDebugEnabled() )
			logger.debug("   Importing relationship id " + idToImport + " version " + versionToImport + " in " + importMode.getLabel() + ((archimateDiagramModel != null) ? " into view."+dbMetadata.getDebugName() : "."));


		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObjectFromDatabase(idToImport, "IArchimateRelationship", versionToImport);
			
			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));
			
			if ( this.mustCreateCopy ) {
				String newId = DBPlugin.createID();
				this.model.registerCopiedRelationship((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}

			if ( (parentFolder != null) && (archimateModel.getDBMetadata(parentFolder).getRootFolderType() == DBMetadata.getDefaultFolderType((String)this.newValues.get("class"))) )
			    this.newParentFolder = parentFolder;
			else
			    this.newParentFolder = importConnection.getLastKnownFolder(this.model, mergedModelId, "IArchimateRelationship", this.id);

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
				this.importedRelationship = (IArchimateRelationship) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier((String)this.newValues.get("class"))));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedRelationship);

				this.oldInitialVersion = dbMetadata.getInitialVersion();
				this.oldCurrentVersion = dbMetadata.getCurrentVersion();
				this.oldDatabaseVersion = dbMetadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = dbMetadata.getLatestDatabaseVersion();

				this.oldName = dbMetadata.getName();
				this.oldDocumentation = dbMetadata.getDocumentation();
				this.oldStrength = dbMetadata.getStrength();
				this.oldAccessType = dbMetadata.getAccessType();

				this.oldSource = dbMetadata.getRelationshipSource();
				this.oldTarget = dbMetadata.getRelationshipTarget();

				this.oldProperties = new ArrayList<DBProperty>();
				for ( IProperty prop: this.importedRelationship.getProperties() ) {
					this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
				}
				
				this.oldFeatures = new ArrayList<DBProperty>();
				for ( IFeature feature: this.importedRelationship.getFeatures() ) {
					this.oldFeatures.add(new DBProperty(feature.getName(), feature.getValue()));
				}

				this.oldParentFolder = dbMetadata.getParentFolder();

				this.isNew = false;
			}

			DBMetadata dbMetadata = this.model.getDBMetadata(this.importedRelationship);

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
			dbMetadata.setStrength((String)this.newValues.get("strength"));
			dbMetadata.setAccessType((Integer)this.newValues.get("access_type"));

			// The source of the relationship can be either an element or another relationship
			IArchimateConcept source = this.model.getAllElements().get(this.model.getNewElementId((String)this.newValues.get("source_id")));
			if ( source == null )
				source = this.model.getAllRelationships().get(this.model.getNewRelationshipId((String)this.newValues.get("source_id")));
			if ( source == null ) {
				// the source does not exist in the model, so we register it for later resolution
				this.model.registerSourceRelationship(this.importedRelationship, (String)this.newValues.get("source_id"));
			} else {
				// the source can be resolved right away
				dbMetadata.setRelationshipSource(source);
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
				dbMetadata.setRelationshipTarget(target);
			}

			this.importedRelationship.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedRelationship.getProperties().add(prop);
    			}
			}
			
			this.importedRelationship.getFeatures().clear();
			if ( this.newValues.get("features") != null ) {
    			for ( DBProperty newFeature: (ArrayList<DBProperty>)this.newValues.get("features")) {
    				IFeature feature = IArchimateFactory.eINSTANCE.createFeature();
    				feature.setName(newFeature.getKey());
    				feature.setValue(newFeature.getValue());
    				this.importedRelationship.getFeatures().add(feature);
    			}
			}

			// During the import of an individual relationship from the database, we check if objects or connections exist for the source and the target
			// and create the corresponding connections
			// TODO: add an option that the user can choose if he wants the connections or not
			if ( this.view != null && dbMetadata.findConnectables(this.view).isEmpty() ) {
				this.createdViewConnections = new ArrayList<IDiagramModelConnection>();
				List<IConnectable> sourceConnections = dbMetadata.findConnectables(this.view, this.importedRelationship.getSource());
				List<IConnectable> targetConnections = dbMetadata.findConnectables(this.view, this.importedRelationship.getTarget());

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

			if ( this.newParentFolder == null )
				dbMetadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedRelationship));
			else
				dbMetadata.setParentFolder(this.newParentFolder);

			if ( this.isNew )
				this.model.countObject(this.importedRelationship, false);

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
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedRelationship);

				dbMetadata.getInitialVersion().set(this.oldInitialVersion);
				dbMetadata.getCurrentVersion().set(this.oldCurrentVersion);
				dbMetadata.getDatabaseVersion().set(this.oldDatabaseVersion);
				dbMetadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

				dbMetadata.setName(this.oldName);
				dbMetadata.setDocumentation(this.oldDocumentation);
				dbMetadata.setStrength(this.oldStrength);
				dbMetadata.setAccessType(this.oldAccessType);

				dbMetadata.setRelationshipSource(this.oldSource);
				dbMetadata.setRelationshipTarget(this.oldTarget);

				dbMetadata.setParentFolder(this.oldParentFolder);

				this.importedRelationship.getProperties().clear();
				for ( DBProperty oldProperty: this.oldProperties ) {
					IProperty newProperty = IArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldProperty.getKey());
					newProperty.setValue(oldProperty.getValue());
					this.importedRelationship.getProperties().add(newProperty);
				}
				
				this.importedRelationship.getFeatures().clear();
				for ( DBProperty oldFeature: this.oldFeatures ) {
					IFeature newFeature = IArchimateFactory.eINSTANCE.createFeature();
					newFeature.setName(oldFeature.getKey());
					newFeature.setValue(oldFeature.getValue());
					this.importedRelationship.getFeatures().add(newFeature);
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
