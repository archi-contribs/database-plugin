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
import org.archicontribs.database.data.DBPair;
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
    private Exception exception;

    private DBArchimateModel model = null;
    private IArchimateDiagramModel view = null;

    private String id = null;
    private boolean mustCreateCopy = false;
    private boolean isNew = false;

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
    private ArrayList<DBPair<String, String>> oldProperties = null;


    /**
     * Imports a relationship into the model<br>
     * @param connection connection to the database
     * @param model model into which the relationship will be imported
     * @param relationshipId id of the relationship to import
     * @param relationshipVersion version of the relationship to import (0 if the latest version should be imported)
     */
    public DBImportRelationshipFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version) {
        this(connection, model, null, id, version, false);
    }

    /**
     * Imports a relationship into the model<br>
     * @param model model into which the relationship will be imported
     * @param view if a view is provided, then an ArchimateObject will be automatically created
     * @param id id of the relationship to import
     * @param version version of the relationship to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the relationship should be its original id
     */
    public DBImportRelationshipFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, IArchimateDiagramModel view, String id, int version, boolean mustCreateCopy) {
        this.model = model;
        this.view = view;
        this.id = id;
        this.mustCreateCopy = mustCreateCopy;

        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of relationship id "+this.id+".");
            else
                logger.debug("   Importing relationship id "+this.id+".");
        }

        try {
            if ( !this.mustCreateCopy ) {
                this.importedRelationship = model.getAllRelationships().get(this.id);
                if ( this.importedRelationship != null ) {
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

                    this.oldSource = metadata.getSource();
                    this.oldTarget = metadata.getTarget();

                    this.oldProperties = new ArrayList<DBPair<String, String>>();
                    for ( IProperty prop: this.importedRelationship.getProperties() ) {
                        this.oldProperties.add(new DBPair<String, String>(prop.getKey(), prop.getValue()));
                    }

                    this.oldFolder = metadata.getParentFolder();
                }
            }

            // we get the new values from the database to allow execute and redo
            this.newValues = importConnection.getObject(id, "IArchimateRelationship", version);

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
            this.importedRelationship = null;
            this.exception = err;
        }
    }

    @Override
    public boolean canExecute() {
        return (this.model != null) && (this.id != null) ;
    }

    @Override
    public void execute() {
        if ( this.commandHasBeenExecuted )
            return;		// we do not execute it twice
        
        IArchimateConcept source = this.model.getAllElements().get(this.newValues.get("source_id"));
        if ( source == null ) source = this.model.getAllRelationships().get(this.newValues.get("source_id"));
        
        IArchimateConcept target = this.model.getAllElements().get(this.newValues.get("target_id"));
        if ( target == null ) target = this.model.getAllRelationships().get(this.newValues.get("target_id"));

        // we import the relationship if and only if the source and target exist
        if ( (source == null) || (target == null) )
            return;
        
        try {
            this.importedRelationship = this.model.getAllRelationships().get(this.id);

            if ( this.importedRelationship == null ) {
                this.isNew = true;
                this.importedRelationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create((String)this.newValues.get("class"));
            } else
                this.isNew = false;

            DBMetadata metadata = ((IDBMetadata)this.importedRelationship).getDBMetadata();

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
            metadata.setStrength((String)this.newValues.get("strength"));
            metadata.setAccessType((Integer)this.newValues.get("access_type"));

            metadata.setSource(source);
            metadata.setTarget(target);

            this.importedRelationship.getProperties().clear();
            for ( String[] newProperty: (String[][])this.newValues.get("properties")) {
                IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
                prop.setKey(newProperty[0]);
                prop.setValue(newProperty[1]);
                this.importedRelationship.getProperties().add(prop);
            }

            // During the import of an individual relationship from the database, we check if objects or connections exist for the source and the target
            // and create the corresponding connections
            // TODO : add an option that the user can choose if he wants the connections or not
            if ( this.view != null && metadata.componentToConnectable(this.view).isEmpty() ) {
                this.createdViewConnections = new ArrayList<IDiagramModelConnection>();
                List<IConnectable> sourceConnections = metadata.componentToConnectable(this.view, this.importedRelationship.getSource());
                List<IConnectable> targetConnections = metadata.componentToConnectable(this.view, this.importedRelationship.getTarget());

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

            this.commandHasBeenExecuted = true;
        } catch (Exception err) {
            this.importedRelationship = null;
            this.exception = err;
        }
    }

    @Override
    public boolean canUndo() {
        return this.commandHasBeenExecuted && (this.importedRelationship != null);
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

        if ( this.isNew ) {
            // if the relationship has been created by the execute() method, we just delete it
            IFolder parentFolder = (IFolder)this.importedRelationship.eContainer();
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

            metadata.setSource(this.oldSource);
            metadata.setTarget(this.oldTarget);

            metadata.setParentFolder(this.oldFolder);

            this.importedRelationship.getProperties().clear();
            for ( DBPair<String, String> pair: this.oldProperties ) {
                IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
                prop.setKey(pair.getKey());
                prop.setValue(pair.getValue());
                this.importedRelationship.getProperties().add(prop);
            }
        }

        this.commandHasBeenExecuted = false;
    }


    @Override
    public boolean canRedo() {
        return canExecute();
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

    @Override
    public void dispose() {
        this.importedRelationship = null;
        this.createdViewConnections = null;

        this.oldInitialVersion = null;
        this.oldCurrentVersion = null;
        this.oldDatabaseVersion = null;
        this.oldLatestDatabaseVersion = null;

        this.newValues = null;
        this.newFolder = null;

        this.oldName = null;
        this.oldDocumentation = null;
        this.oldStrength = null;
        this.oldAccessType = null;
        this.oldSource = null;
        this.oldTarget = null;
        this.oldProperties = null;

        this.model = null;
        this.view = null;
        this.id = null;
    }
}
