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
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.Command;

import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;

/**
 * Command for importing an element from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportElementFromIdCommand extends Command implements IDBImportFromIdCommand {
    private static final DBLogger logger = new DBLogger(DBImportElementFromIdCommand.class);

    private IArchimateElement importedElement = null;
    private IDiagramModelObject createdViewObject = null;

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
        this(importConnection, model, null, id, version, false, false);
    }

    /**
     * Imports an element into the model<br>
     * @param connection connection to the database
     * @param model model into which the element will be imported
     * @param view if a view is provided, then an ArchimateObject will be automatically created
     * @param id id of the element to import
     * @param version version of the element to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the element should be its original id
     * @param mustImportRelationships true if the relationships to and from  the newly created element must be imported as well  
     */
    public DBImportElementFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, IArchimateDiagramModel view, String id, int version, boolean mustCreateCopy, boolean mustImportRelationships) {
        this.model = model;
        this.view = view;
        this.id = id;
        this.mustCreateCopy = mustCreateCopy;
        this.mustImportRelationships = mustImportRelationships;

        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of element id "+this.id+".");
            else
                logger.debug("   Importing element id "+this.id+".");
        }

        try {
            if ( !this.mustCreateCopy ) {
                this.importedElement = model.getAllElements().get(this.id);
                if ( this.importedElement != null ) {
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
                }
            }

            // we get the new values from the database to allow execute and redo
            this.newValues = importConnection.getObject(id, "IArchimateElement", version);

            this.newFolder = importConnection.getLastKnownFolder(this.model, "IArchimateElement", this.id);

            if ( this.mustImportRelationships ) {
                if ( logger.isDebugEnabled() ) logger.debug("   Checking if we must import relationships");
                // We import the relationships that source or target the element
                try ( ResultSet resultrelationship = importConnection.select("SELECT id, source_id, target_id FROM "+importConnection.getSchema()+"relationships WHERE source_id = ? OR target_id = ?", this.id, this.id) ) {
                    while ( resultrelationship.next() && resultrelationship.getString("id") != null ) {
                        // we import only relationships that are not present in the model
                        // TODO: add an option to import the relationship to refresh its properties from the database
                        if ( this.model.getAllRelationships().get(resultrelationship.getString("id")) == null ) {
                            IDBImportFromIdCommand command = new DBImportRelationshipFromIdCommand(importConnection, this.model, this.view, resultrelationship.getString("id"), 0, false);
                            if ( command.getException() == null )
                            	this.importRelationshipCommands.add(command);
                            else
                            	throw command.getException();
                        }
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

            if ( this.importedElement == null ) {
                this.isNew = true;
                this.importedElement = (IArchimateElement) DBArchimateFactory.eINSTANCE.create((String)this.newValues.get("class"));
            } else
                this.isNew = false;

            DBMetadata metadata = ((IDBMetadata)this.importedElement).getDBMetadata();

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
            metadata.setType((String)this.newValues.get("type"));

            this.importedElement.getProperties().clear();
            for ( String[] newProperty: (String[][])this.newValues.get("properties")) {
                IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
                prop.setKey(newProperty[0]);
                prop.setValue(newProperty[1]);
                this.importedElement.getProperties().add(prop);
            }

            if ( this.newFolder == null )
                metadata.setParentFolder(this.model.getDefaultFolderForObject(this.importedElement));
            else
                metadata.setParentFolder(this.newFolder);

            if ( this.view != null && metadata.componentToConnectable(this.view).isEmpty() ) {
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
