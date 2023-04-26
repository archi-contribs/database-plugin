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
import org.archicontribs.database.connection.DBSelect;
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
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing an element from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportElementFromIdCommand extends Command implements IDBImportCommand {
	private static final DBLogger logger = new DBLogger(DBImportElementFromIdCommand.class);
	
	private static int createdViewObjectXLocation = 0;
	private static int createdViewObjectYLocation = 0;

	private IArchimateElement importedElement = null;
	private IDiagramModelArchimateObject createdViewObject = null;

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private List<IDBImportCommand> importRelationshipCommands = new ArrayList<IDBImportCommand>();
	private Exception exception = null;

	private DBArchimateModel model = null;
	private IArchimateDiagramModel view = null;

	private String id;
	private boolean mustCreateCopy;
	private boolean mustImportTheRelationships;
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
	private String oldType = null;
	private IFolder oldParentFolder = null;
	private ArrayList<DBProperty> oldProperties = null;
	private ArrayList<DBProperty> oldFeatures = null;


	/**
	 * Imports an element into the model<br>
	 * @param importConnection connection to the database
	 * @param archimateModel model into which the element will be imported
	 * @param mergedModelId ID of the model merged in the actual model, to search for its parent folder 
	 * @param archimateDiagramModel if a view is provided, then an ArchimateObject will be automatically created
	 * @param parentFolder if a folder is provided, the element will be created inside this parent folder. Else, we'll check in the database if the element has already been part of this model in order to import it in the same parent folder.
	 * @param idToImport id of the element to import
	 * @param versionToImport version of the element to import (0 if the latest version should be imported)
	 * @param importMode specifies if the element must be copied or shared
	 * @param mustImportRelationships true if the relationships to and from  the newly created element must be imported as well  
	 */
	public DBImportElementFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, String mergedModelId, IArchimateDiagramModel archimateDiagramModel, IFolder parentFolder, String idToImport, int versionToImport, DBImportMode importMode, boolean mustImportRelationships) {
		this.model = archimateModel;
		this.view = archimateDiagramModel;
		this.id = idToImport;
		this.mustImportTheRelationships = mustImportRelationships;

		if ( logger.isDebugEnabled() )
			logger.debug("   Importing element id " + this.id + " version " + versionToImport +" in " + importMode.getLabel() + ((archimateDiagramModel != null) ? " into view."+archimateDiagramModel.getId() : "."));

		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObjectFromDatabase(idToImport, "IArchimateElement", versionToImport);
			
			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));
			
			if ( this.mustCreateCopy ) {
				String newId = DBPlugin.createID();
				this.model.registerCopiedElement((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}

			if ( (parentFolder != null) && (archimateModel.getDBMetadata(parentFolder).getRootFolderType().intValue() == DBMetadata.getDefaultFolderType((String)this.newValues.get("class"))) )
			    this.newParentFolder = parentFolder;
			else
			    this.newParentFolder = importConnection.getLastKnownFolder(this.model, mergedModelId, "IArchimateElement", this.id);

			if ( this.mustImportTheRelationships ) {
				if ( logger.isDebugEnabled() ) logger.debug("   Checking if we must import relationships");
				// We import the relationships that source or target the element
				try ( DBSelect resultRelationship = new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT id, source_id, target_id FROM "+importConnection.getSchemaPrefix()+"relationships WHERE source_id = ? OR target_id = ?", this.id, this.id) ) {
					while ( resultRelationship.next() ) {
					    IArchimateRelationship relationship = this.model.getAllRelationships().get(this.model.getNewRelationshipId(resultRelationship.getString("id")));
					    
					    IArchimateConcept source = this.model.getAllElements().get(this.model.getNewElementId(resultRelationship.getString("source_id")));
					    if ( source == null )
					    	source = this.model.getAllRelationships().get(this.model.getNewRelationshipId(resultRelationship.getString("source_id")));
					    
					    IArchimateConcept target = this.model.getAllElements().get(this.model.getNewElementId(resultRelationship.getString("target_id")));
					    if ( target == null )
					    	target = this.model.getAllRelationships().get(this.model.getNewRelationshipId(resultRelationship.getString("target_id")));
					    
                        // we import only relationships that are not present in the model and, on the opposite, if the source and target do exist in the model
						if ( (relationship  == null) && (DBPlugin.areEqual(resultRelationship.getString("source_id"), idToImport) || source != null) && (DBPlugin.areEqual(resultRelationship.getString("target_id"), idToImport) || target != null) ) {
							IDBImportCommand command = new DBImportRelationshipFromIdCommand(importConnection, this.model, this.view, mergedModelId, null, resultRelationship.getString("id"), 0, importMode);
							if ( command.getException() == null )
								this.importRelationshipCommands.add(command);
							else
								throw command.getException();
						}
                        // TODO: add an option to import the relationship to refresh its properties from the database
					}
				}
			}

			if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
				setLabel("import element");
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

	@Override
	public boolean canExecute() {
		return (this.model != null) && (this.id != null) && (this.exception == null);
	}

	@Override
	public void execute() {
		if ( this.commandHasBeenExecuted )
			return;		// we do not execute it twice

		this.commandHasBeenExecuted = true;

		try {
			this.importedElement = this.model.getAllElements().get(this.id);

			if ( (this.importedElement == null) || this.mustCreateCopy ) {
				this.importedElement = (IArchimateElement) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier((String)this.newValues.get("class"))));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata metadata = this.model.getDBMetadata(this.importedElement);

				this.oldInitialVersion = metadata.getInitialVersion();
				this.oldCurrentVersion = metadata.getCurrentVersion();
				this.oldDatabaseVersion = metadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();

				this.oldName = metadata.getName();
				this.oldDocumentation = metadata.getDocumentation();
				this.oldType = metadata.getJunctionType();

				this.oldProperties = new ArrayList<DBProperty>();
				for ( IProperty prop: this.importedElement.getProperties() ) {
					this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
				}
				
				this.oldFeatures = new ArrayList<DBProperty>();
				for ( IFeature feature: this.importedElement.getFeatures() ) {
					this.oldFeatures.add(new DBProperty(feature.getName(), feature.getValue()));
				}

				this.oldParentFolder = metadata.getParentFolder();

				this.isNew = false;
			}

			DBMetadata metadata = this.model.getDBMetadata(this.importedElement);

			if ( this.mustCreateCopy )
				metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()), null);
			else
				metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"), (String)this.newValues.get("created_by"));

			metadata.setId((String)this.newValues.get("id"));
			metadata.setName((String)this.newValues.get("name"));
			metadata.getCurrentVersion().set(metadata.getInitialVersion());
			metadata.getDatabaseVersion().set(metadata.getInitialVersion());
			metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

			metadata.setDocumentation((String)this.newValues.get("documentation"));
			metadata.setType((String)this.newValues.get("type"));

			this.importedElement.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedElement.getProperties().add(prop);
    			}
			}

			if ( this.newParentFolder == null ) {
				if ( this.oldParentFolder == null)
					metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedElement));
				// else we keep the existing folder 
			} else
				metadata.setParentFolder(this.newParentFolder);

			if ( this.view != null && metadata.findConnectables(this.view).isEmpty() ) {
				this.createdViewObject = ArchimateDiagramModelFactory.createDiagramModelArchimateObject(this.importedElement);
				this.createdViewObject.getBounds().setLocation(createdViewObjectXLocation, createdViewObjectYLocation);
				createdViewObjectXLocation += this.createdViewObject.getBounds().getWidth() + 10;
				if ( createdViewObjectXLocation > 800 ) {
				    createdViewObjectYLocation += this.createdViewObject.getBounds().getHeight() + 10;
				    createdViewObjectXLocation = 0;
				}
				this.view.getChildren().add(this.createdViewObject);
				this.model.countObject(this.createdViewObject, false);
			}

			if ( this.isNew )
				this.model.countObject(this.importedElement, false);

			// if some relationships must be imported
			for (IDBImportCommand childCommand: this.importRelationshipCommands) {
				if ( childCommand.canExecute() )
					childCommand.execute();

				if ( childCommand.getException() != null )
					throw childCommand.getException();
			}

		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
			this.exception = err;
		}
	}

	@Override
	public void undo() {
		// if some relationships have been imported
		for (int i = this.importRelationshipCommands.size() - 1 ; i >= 0 ; --i) {
			if ( this.importRelationshipCommands.get(i).canUndo() )
				this.importRelationshipCommands.get(i).undo();
		}

		// if a viewObject has been created, then we remove it from the view
		if ( (this.view != null) && (this.createdViewObject != null) ) {
			this.view.getChildren().remove(this.createdViewObject);
			this.model.getAllViewObjects().remove(this.createdViewObject.getId());
		}

		if ( this.importedElement != null ) {
			if ( this.isNew ) {
				// if the element has been created by the execute method, we just delete it
				IFolder parentFolder = (IFolder)this.importedElement.eContainer();
				if ( parentFolder != null )
					parentFolder.getElements().remove(this.importedElement);

				this.model.getAllElements().remove(this.importedElement.getId());
			} else {
				// else, we need to restore the old properties
				DBMetadata metadata = this.model.getDBMetadata(this.importedElement);

				metadata.getInitialVersion().set(this.oldInitialVersion);
				metadata.getCurrentVersion().set(this.oldCurrentVersion);
				metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
				metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

				metadata.setName(this.oldName);
				metadata.setDocumentation(this.oldDocumentation);
				metadata.setType(this.oldType);

				metadata.setParentFolder(this.oldParentFolder);

				this.importedElement.getProperties().clear();
				for ( DBProperty oldProperty: this.oldProperties ) {
					IProperty newProperty = IArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldProperty.getKey());
					newProperty.setValue(oldProperty.getValue());
					this.importedElement.getProperties().add(newProperty);
				}
				
				this.importedElement.getFeatures().clear();
				for ( DBProperty oldFeature: this.oldFeatures ) {
					IFeature newFeature = IArchimateFactory.eINSTANCE.createFeature();
					newFeature.setName(oldFeature.getKey());
					newFeature.setValue(oldFeature.getValue());
					this.importedElement.getFeatures().add(newFeature);
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
	public IArchimateElement getImported() {
		return this.importedElement;
	}

	/**
	 * @return the exception that has been raised during the import process, if any.
	 */
	@Override
	public Exception getException() {
		return this.exception;
	}
	
	/**
	 * Resets the X and Y location of the created viewObject
	 */
	public static void resetCreatedViewObjectsLocation() {
	    createdViewObjectXLocation = 0;
	    createdViewObjectYLocation = 0;
	}
}
