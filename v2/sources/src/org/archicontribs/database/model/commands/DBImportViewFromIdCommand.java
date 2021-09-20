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

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.editor.ui.services.EditorManager;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing a view from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewFromIdCommand extends Command implements IDBImportCommand {
	private static final DBLogger logger = new DBLogger(DBImportViewFromIdCommand.class);

	private IDiagramModel importedView= null; 

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private List<IDBImportCommand> importViewContentCommands = new ArrayList<IDBImportCommand>();
	private Exception exception;

	private DBArchimateModel model = null;

	private String id = null;
	private boolean mustCreateCopy;
	private boolean mustImportViewContent;
	private boolean isNew;

	// new values that are retrieved from the database
	private HashMap<String, Object> newValues = null;
	private IFolder newFolder = null;

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
	private IFolder oldFolder = null;
	private ArrayList<DBProperty> oldProperties = null;
	private ArrayList<DBProperty> oldFeatures = null;


	/**
	 * Imports a view into the model<br>
	 * @param importConnection connection to the database
	 * @param archimateModel model into which the view will be imported
	 * @param view if a view is provided, then an ArchimateObject will be automatically created
	 * @param folder if a folder is provided, the view will be created inside this folder. Else, we'll check in the database if the view has already been part of this model in order to import it in the same folder.
	 * @param idToImport id of the view to import
	 * @param versionToImport version of the view to import
	 * @param importMode mode of the import (template, shared or copy mode) 
	 * @param mustImportContent true if the view content must be imported as well

	 */
	public DBImportViewFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, IFolder folder, String idToImport, int versionToImport, DBImportMode importMode, boolean mustImportContent) {
		this.model = archimateModel;
		this.id = idToImport;
		this.mustImportViewContent = mustImportContent;
		
		if ( logger.isDebugEnabled() )
			logger.debug("   Importing view id " + idToImport + " version " + versionToImport + " in " + importMode.getLabel() + (mustImportContent ? " including its content" : "") + ".");
		
		importConnection.declareAsImported(idToImport);

		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObjectFromDatabase(idToImport, "IDiagramModel", versionToImport);

			this.mustCreateCopy = importMode.shouldCreateCopy((ArrayList<DBProperty>)this.newValues.get("properties"));
			
			if ( this.mustCreateCopy ) {
				String newId = DBPlugin.createID();
				this.model.registerCopiedView((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}

			if ( (folder != null) && (archimateModel.getDBMetadata(folder).getRootFolderType() == DBMetadata.getDefaultFolderType((String)this.newValues.get("class"))) )
			    this.newFolder = folder;
			else
			    this.newFolder = importConnection.getLastKnownFolder(this.model, "IDiagramModel", this.id);

			if ( this.mustImportViewContent ) {
				// we import the objects and create the corresponding elements if they do not exist yet
				//    we use the importFromId method in order to allow undo and redo
				try (DBSelect result = (versionToImport == 0)
						? new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT object_id, object_version, pos FROM "+importConnection.getSchemaPrefix()+"views_objects_in_view WHERE view_id = ? AND view_version = (SELECT MAX(view_version) FROM "+importConnection.getSchemaPrefix()+"views_objects_in_view WHERE view_id = ?) ORDER BY pos", idToImport, idToImport)
						: new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT DISTINCT object_id, object_version, pos FROM "+importConnection.getSchemaPrefix()+"views_objects_in_view WHERE view_id = ? AND view_version = ? ORDER BY pos", idToImport, versionToImport) ) {
					while ( result.next() ) {
					    DBImportViewObjectFromIdCommand command = new DBImportViewObjectFromIdCommand(importConnection, archimateModel, result.getString("object_id"), (versionToImport == 0) ? 0 : result.getInt("object_version"), this.mustCreateCopy, importMode);
					    if ( command.getException() != null )
					        throw command.getException();
						this.importViewContentCommands.add(command);
					}
				}

				// we import the connections and create the corresponding relationships if they do not exist yet
				//    we use the importFromId method in order to allow undo and redo
				try (DBSelect result = (versionToImport == 0)
						? new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT DISTINCT connection_id, connection_version, pos FROM "+importConnection.getSchemaPrefix()+"views_connections_in_view WHERE view_id = ? AND view_version = (SELECT MAX(view_version) FROM "+importConnection.getSchemaPrefix()+"views_connections_in_view WHERE view_id = ?) ORDER BY pos", idToImport, idToImport)
						: new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT DISTINCT connection_id, connection_version, pos FROM "+importConnection.getSchemaPrefix()+"views_connections_in_view WHERE view_id = ? AND view_version = ? ORDER BY pos", idToImport, versionToImport) ) {
					while ( result.next() ) {
					    DBImportViewConnectionFromIdCommand command = new DBImportViewConnectionFromIdCommand(importConnection, archimateModel, result.getString("connection_id"), (versionToImport == 0) ? 0 : result.getInt("connection_version"), this.mustCreateCopy, importMode);
					    if ( command.getException() != null )
					        throw command.getException();
						this.importViewContentCommands.add(command);
					}
				}
			}

			if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
				setLabel("import view");
			} else {
				if ( ((String)this.newValues.get("name")).length() > 20 )
					setLabel("import \""+((String)this.newValues.get("name")).substring(0,16)+"...\"");
				else
					setLabel("import \""+(String)this.newValues.get("name")+"\"");
			}
		} catch (Exception err) {
            Logger.logError("Got Exception "+err.getMessage());
			this.importedView = null;
			this.exception = err;
		}
	}

	@Override
	public void execute() {
		if ( this.commandHasBeenExecuted )
			return;		// we do not execute it twice

		this.commandHasBeenExecuted = true;

		try {
			this.importedView = this.model.getAllViews().get(this.id);

			if ( this.importedView == null ) {
				if ( DBPlugin.areEqual((String)this.newValues.get("class"), "CanvasModel") )
					this.importedView = (IDiagramModel) ICanvasFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier("com.archimatetool.canvas.model."+(String)this.newValues.get("class"))));
				else
					this.importedView = (IDiagramModel) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier((String)this.newValues.get("class"))));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedView);

				this.oldInitialVersion = dbMetadata.getInitialVersion();
				this.oldCurrentVersion = dbMetadata.getCurrentVersion();
				this.oldDatabaseVersion = dbMetadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = dbMetadata.getLatestDatabaseVersion();

				this.oldName = dbMetadata.getName();
				this.oldDocumentation = dbMetadata.getDocumentation();
				this.oldConnectionRouterType = dbMetadata.getConnectionRouterType();
				this.oldViewpoint = dbMetadata.getViewpoint();
				this.oldBackground = dbMetadata.getBackground();

				this.oldProperties = new ArrayList<DBProperty>();
				for ( IProperty prop: this.importedView.getProperties() ) {
					this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
				}
				
				this.oldFeatures = new ArrayList<DBProperty>();
				for ( IFeature feature: this.importedView.getFeatures() ) {
					this.oldFeatures.add(new DBProperty(feature.getName(), feature.getValue()));
				}

				this.oldFolder = dbMetadata.getParentFolder();

				this.isNew = false;
			}
			DBMetadata metadata = this.model.getDBMetadata(this.importedView);

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
			metadata.setConnectionRouterType((Integer)this.newValues.get("connection_router_type"));
			metadata.setViewpoint((String)this.newValues.get("viewpoint"));
			metadata.setBackground((Integer)this.newValues.get("background"));

			this.importedView.getProperties().clear();
			if ( this.newValues.get("properties") != null ) {
    			for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    				IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
    				prop.setKey(newProperty.getKey());
    				prop.setValue(newProperty.getValue());
    				this.importedView.getProperties().add(prop);
    			}
			}

			if ( this.newFolder == null )
				metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedView));
			else
				metadata.setParentFolder(this.newFolder);

			if ( this.isNew )
				this.model.countObject(this.importedView, false);

			// if some content must be imported
			for (IDBImportCommand childCommand: this.importViewContentCommands) {
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
	public boolean canUndo() {
		return this.commandHasBeenExecuted;
	}

	@Override
	public void undo() {
		if ( !this.commandHasBeenExecuted )
			return;

		// if some content has been imported
		for (int i = this.importViewContentCommands.size() - 1 ; i >= 0 ; --i) {
			if ( this.importViewContentCommands.get(i).canUndo() ) 
				this.importViewContentCommands.get(i).undo();
		}

		if ( this.importedView != null ) {
			if ( this.isNew ) {
				// if the view has been created by the execute method, we just delete it
				EditorManager.closeDiagramEditor(this.importedView);

				IFolder parentFolder = (IFolder)this.importedView.eContainer();
				if ( parentFolder != null )
					parentFolder.getElements().remove(this.importedView);

				this.model.getAllFolders().remove(this.importedView.getId());
			} else {
				// else, we need to restore the old properties
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedView);

				dbMetadata.getInitialVersion().set(this.oldInitialVersion);
				dbMetadata.getCurrentVersion().set(this.oldCurrentVersion);
				dbMetadata.getDatabaseVersion().set(this.oldDatabaseVersion);
				dbMetadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

				dbMetadata.setName(this.oldName);
				dbMetadata.setDocumentation(this.oldDocumentation);
				dbMetadata.setConnectionRouterType(this.oldConnectionRouterType);
				dbMetadata.setViewpoint(this.oldViewpoint);
				dbMetadata.setBackground(this.oldBackground);

				dbMetadata.setParentFolder(this.oldFolder);

				this.importedView.getProperties().clear();
				((IProperties)this.importedView).getProperties().clear();
				for ( DBProperty oldPropery: this.oldProperties ) {
					IProperty newProperty = IArchimateFactory.eINSTANCE.createProperty();
					newProperty.setKey(oldPropery.getKey());
					newProperty.setValue(oldPropery.getValue());
					((IProperties)this.importedView).getProperties().add(newProperty);
				}
				
				this.importedView.getFeatures().clear();
				for ( DBProperty oldFeature: this.oldFeatures ) {
					IFeature newFeature = IArchimateFactory.eINSTANCE.createFeature();
					newFeature.setName(oldFeature.getKey());
					newFeature.setValue(oldFeature.getValue());
					this.importedView.getFeatures().add(newFeature);
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
	public IDiagramModel getImported() {
		return this.importedView;
	}

	/**
	 * @return the exception that has been raised during the import process, if any.
	 */
	@Override
	public Exception getException() {
		return this.exception;
	}
}
