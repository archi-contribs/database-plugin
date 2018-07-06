/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import java.sql.ResultSet;
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
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing an element from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportElementFromIdCommand extends Command implements IDBImportFromIdCommand {
	private static final DBLogger logger = new DBLogger(DBImportElementFromIdCommand.class);

	private IArchimateElement importedElement = null;
	private IDiagramModelArchimateObject createdViewObject = null;

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private List<IDBImportFromIdCommand> importRelationshipCommands = new ArrayList<IDBImportFromIdCommand>();
	private Exception exception = null;

	private DBArchimateModel model = null;
	private IArchimateDiagramModel view = null;

	private String id;
	private boolean mustCreateCopy;
	private boolean mustImportRelationships;
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
	private String oldType = null;
	private IFolder oldFolder = null;
	private ArrayList<DBProperty> oldProperties = null;


	/**
	 * Imports an element into the model<br>
	 * @param connection connection to the database
	 * @param model model into which the element will be imported
	 * @param elementId id of the element to import
	 * @param elementVersion version of the element to import (0 if the latest version should be imported)
	 */
	public DBImportElementFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, String id, int version) {
		this(importConnection, model, null, null, id, version, DBImportMode.templateMode, false);
	}

	/**
	 * Imports an element into the model<br>
	 * @param connection connection to the database
	 * @param model model into which the element will be imported
	 * @param view if a view is provided, then an ArchimateObject will be automatically created
	 * @param folder if a folder is provided, the element will be created inside this folder. Else, we'll check in the database if the view has already been part of this model in order to import it in the same folder.
	 * @param id id of the element to import
	 * @param version version of the element to import (0 if the latest version should be imported)
	 * @param importMode specifies if the element must be copied or shared
	 * @param mustImportRelationships true if the relationships to and from  the newly created element must be imported as well  
	 */
	@SuppressWarnings("unchecked")
	public DBImportElementFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, IArchimateDiagramModel view, IFolder folder, String id, int version, DBImportMode importMode, boolean mustImportRelationships) {
		this.model = model;
		this.view = view;
		this.id = id;
		this.mustImportRelationships = mustImportRelationships;

		if ( logger.isDebugEnabled() )
			logger.debug("   Importing element id " + this.id + "in " + importMode.getLabel() + ((view != null) ? " into view."+view.getId() : "."));

		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObject(id, "IArchimateElement", version);
			
			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));

			if ( (folder != null) && (((IDBMetadata)folder).getDBMetadata().getRootFolderType() == DBMetadata.getDefaultFolderType((String)this.newValues.get("class"))) )
			    this.newFolder = folder;
			else
			    this.newFolder = importConnection.getLastKnownFolder(this.model, "IArchimateElement", this.id);

			if ( this.mustImportRelationships ) {
				if ( logger.isDebugEnabled() ) logger.debug("   Checking if we must import relationships");
				// We import the relationships that source or target the element
				try ( ResultSet resultRelationship = importConnection.select("SELECT id, source_id, target_id FROM "+importConnection.getSchema()+"relationships WHERE source_id = ? OR target_id = ?", this.id, this.id) ) {
					while ( resultRelationship.next() ) {
					    IArchimateRelationship relationship = this.model.getAllRelationships().get(this.model.getNewRelationshipId(resultRelationship.getString("id")));
					    
					    IArchimateConcept source = this.model.getAllElements().get(this.model.getNewElementId(resultRelationship.getString("source_id")));
					    if ( source == null )
					    	source = this.model.getAllRelationships().get(this.model.getNewRelationshipId(resultRelationship.getString("source_id")));
					    
					    IArchimateConcept target = this.model.getAllElements().get(this.model.getNewElementId(resultRelationship.getString("target_id")));
					    if ( target == null )
					    	target = this.model.getAllRelationships().get(this.model.getNewRelationshipId(resultRelationship.getString("target_id")));
					    
                        // we import only relationships that are not present in the model and, on the opposite, if the source and target do exist in the model
						if ( (relationship  == null) && (DBPlugin.areEqual(resultRelationship.getString("source_id"), id) || source != null) && (DBPlugin.areEqual(resultRelationship.getString("target_id"), id) || target != null) ) {
							IDBImportFromIdCommand command = new DBImportRelationshipFromIdCommand(importConnection, this.model, this.view, null, resultRelationship.getString("id"), 0, importMode);
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

	@SuppressWarnings("unchecked")
	@Override
	public void execute() {
		if ( this.commandHasBeenExecuted )
			return;		// we do not execute it twice

		this.commandHasBeenExecuted = true;

		try {
			this.importedElement = this.model.getAllElements().get(this.id);

			if ( (this.importedElement == null) || this.mustCreateCopy ) {
				this.importedElement = (IArchimateElement) DBArchimateFactory.eINSTANCE.create((String)this.newValues.get("class"));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata metadata = ((IDBMetadata)this.importedElement).getDBMetadata();

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

				this.oldFolder = metadata.getParentFolder();

				this.isNew = false;
			}

			DBMetadata metadata = ((IDBMetadata)this.importedElement).getDBMetadata();

			if ( this.mustCreateCopy ) {
				metadata.setId(this.model.getIDAdapter().getNewID());
				metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
				this.model.registerCopiedElement((String)this.newValues.get("id"), metadata.getId());
				metadata.setName((String)this.newValues.get("name") + " (copy)");	//TODO: add a preference
			} else {
				metadata.setId((String)this.newValues.get("id"));
				metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));
				metadata.setName((String)this.newValues.get("name"));
			}

			metadata.getCurrentVersion().set(metadata.getInitialVersion());
			metadata.getDatabaseVersion().set(metadata.getInitialVersion());
			metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

			metadata.setDocumentation((String)this.newValues.get("documentation"));
			metadata.setType((String)this.newValues.get("type"));

			this.importedElement.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedElement.getProperties().add(prop);
    			}
			}

			if ( this.newFolder == null )
				metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedElement));
			else
				metadata.setParentFolder(this.newFolder);

			if ( this.view != null && metadata.findConnectables(this.view).isEmpty() ) {
				this.createdViewObject = ArchimateDiagramModelFactory.createDiagramModelArchimateObject(this.importedElement);
				this.view.getChildren().add(this.createdViewObject);
				this.model.countObject(this.createdViewObject, false, null);
			}

			if ( this.isNew )
				this.model.countObject(this.importedElement, false, null);

			// if some relationships must be imported
			for (IDBImportFromIdCommand childCommand: this.importRelationshipCommands) {
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
				DBMetadata metadata = ((IDBMetadata)this.importedElement).getDBMetadata();

				metadata.getInitialVersion().set(this.oldInitialVersion);
				metadata.getCurrentVersion().set(this.oldCurrentVersion);
				metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
				metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

				metadata.setName(this.oldName);
				metadata.setDocumentation(this.oldDocumentation);
				metadata.setType(this.oldType);

				metadata.setParentFolder(this.oldFolder);

				this.importedElement.getProperties().clear();
				for ( DBProperty oldProperty: this.oldProperties ) {
					IProperty newProperty = DBArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldProperty.getKey());
					newProperty.setValue(oldProperty.getValue());
					this.importedElement.getProperties().add(newProperty);
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
}
