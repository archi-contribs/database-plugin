/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.impl.Folder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

import lombok.Getter;

/**
 * This class holds the information required to connect to, to import from and export to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseExportConnection extends DBDatabaseConnection {
    private static final DBLogger logger = new DBLogger(DBDatabaseExportConnection.class);

    private boolean isImportconnectionDuplicate = false;

    /**
     * This class variable stores the last commit transaction
     * It will be used in every insert and update calls<br>
     * This way, all requests in a transaction will have the same timestamp.
     */
    private Timestamp lastTransactionTimestamp = null;



    /**
     * Opens a connection to a JDBC database using all the connection details
     * @param dbEntry 
     * @throws ClassNotFoundException 
     * @throws SQLException 
     */
    public DBDatabaseExportConnection(DBDatabaseEntry dbEntry) throws ClassNotFoundException, SQLException {
        super(dbEntry);
    }

    /**
     * duplicates a connection to a JDBC database to allow switching between DBDatabaseImportConnection and DBDatabaseExportConnection
     * @param importConnection 
     */
    public DBDatabaseExportConnection(DBDatabaseImportConnection importConnection) {
        super();
        assert(importConnection != null);
        super.databaseEntry = importConnection.databaseEntry;
        super.schema = importConnection.schema;
        super.connection = importConnection.connection;
        this.isImportconnectionDuplicate = true;
    }

    /**
     * duplicates a connection to a JDBC database to allow switching between DBDatabaseConnection and DBDatabaseExportConnection
     * @param databaseConnection 
     */
    public DBDatabaseExportConnection(DBDatabaseConnection databaseConnection) {
        super();
        assert(databaseConnection != null);
        super.databaseEntry = databaseConnection.databaseEntry;
        super.schema = databaseConnection.schema;
        super.connection = databaseConnection.connection;
        this.isImportconnectionDuplicate = false;
    }

    @Getter private HashMap<String, DBMetadata> elementsNotInModel = new HashMap<String, DBMetadata>();
    @Getter private HashMap<String, DBMetadata> relationshipsNotInModel = new HashMap<String, DBMetadata>();
    @Getter private HashMap<String, DBMetadata> foldersNotInModel = new LinkedHashMap<String, DBMetadata>();			// must keep the order
    @Getter private HashMap<String, DBMetadata> viewsNotInModel = new HashMap<String, DBMetadata>();
    @Getter private HashMap<String, DBMetadata> viewObjectsNotInModel = new LinkedHashMap<String, DBMetadata>();		// must keep the order
    @Getter private HashMap<String, DBMetadata> viewConnectionsNotInModel = new LinkedHashMap<String, DBMetadata>();	// must keep the order
    @Getter private HashMap<String, DBMetadata> imagesNotInModel = new HashMap<String, DBMetadata>();
    @Getter private HashMap<String, DBMetadata> imagesNotInDatabase = new HashMap<String, DBMetadata>();

    /**
     * Gets the version of the model from the database
     * 
     * @param model
     * @throws SQLException
     */
    public void getModelVersionFromDatabase(DBArchimateModel model) throws SQLException {
        String modelId = model.getId();

        if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the model from the database");
        // model.getCurrentVersion().reset();
        try ( DBSelect resultLatestVersion = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT version, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = ?)", modelId, modelId) ) {
            // we get the latest model version from the database
            if ( resultLatestVersion.next() && (resultLatestVersion.getObject("version") != null) ) {
                // if the version is found, then the model exists in the database
                model.getDatabaseVersion().setVersion(resultLatestVersion.getInt("version"));
                model.getDatabaseVersion().setChecksum(resultLatestVersion.getString("checksum"));
                model.getDatabaseVersion().setTimestamp(resultLatestVersion.getTimestamp("created_on"));

                // we check if the model has been imported from (or last exported to) this database
                if ( !model.getInitialVersion().getTimestamp().equals(DBVersion.NEVER) ) {
                    try ( DBSelect resultCurrentVersion = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT version, checksum FROM "+this.schema+"models WHERE id = ? AND created_on = ?", modelId, model.getInitialVersion().getTimestamp()) ) {
                        if ( resultCurrentVersion.next() && resultCurrentVersion.getObject("version") != null ) {
                            // if the version is found, then the model has been imported from or last exported to the database 
                            model.getInitialVersion().setVersion(resultCurrentVersion.getInt("version"));
                            model.getInitialVersion().setChecksum(resultCurrentVersion.getString("checksum"));
                        }
                    }
                }
                logger.debug("The model already exists in the database:");
            } else {
                model.getDatabaseVersion().setVersion(0);

                logger.debug("The model does not exist in the database");
            }

            model.getCurrentVersion().setVersion(model.getDatabaseVersion().getVersion());

            if ( logger.isTraceEnabled() ) {
                logger.trace("         Initial version = " + model.getInitialVersion().getVersion());
                logger.trace("         Current version = " + model.getCurrentVersion().getVersion());
                logger.trace("         Database version = "+ model.getDatabaseVersion().getVersion());
            }
        }
    }


    /**
     * Gets the version and checksum of a specific component from the database and fills its DBMetadata.<br>
     * <br>
     * Thus method is meant to be called during the export process for every component that is new in the model to check if it is shared with other models.
     * <br>
     * @param component
     * @throws SQLException
     */
    public void getVersionFromDatabase(IIdentifier component) throws SQLException {
        assert (component != null);

        String request;
        String modelId;
        int modelInitialVersion;
        int modelDatabaseVersion;

        DBMetadata metadata = ((IDBMetadata)component).getDBMetadata();

        if ( logger.isTraceEnabled() ) {
            logger.trace("   Getting version of "+metadata.getDebugName()+" from the database.");
        }

        if ( component instanceof IArchimateElement )  {
            request = "SELECT id, name, version, checksum, created_on, model_id, model_version"
                    + " FROM "+this.schema+"elements"
                    + " LEFT JOIN "+this.schema+"elements_in_model ON element_id = id AND element_version = version"
                    + " WHERE id = ?"
                    + " ORDER BY version, model_version";
            DBArchimateModel model = (DBArchimateModel) ((IArchimateElement)component).getArchimateModel();
            modelId = model.getId();
            modelInitialVersion = model.getInitialVersion().getVersion();
            modelDatabaseVersion = model.getDatabaseVersion().getVersion();
        }
        else if ( component instanceof IArchimateRelationship ) {
            request = "SELECT id, name, version, checksum, created_on, model_id, model_version"
                    + " FROM "+this.schema+"relationships"
                    + " LEFT JOIN "+this.schema+"relationships_in_model ON relationship_id = id AND relationship_version = version"
                    + " WHERE id = ?"
                    + " ORDER BY version, model_version";
            DBArchimateModel model = (DBArchimateModel) ((IArchimateRelationship)component).getArchimateModel();
            modelId = model.getId();
            modelInitialVersion = model.getInitialVersion().getVersion();
            modelDatabaseVersion = model.getDatabaseVersion().getVersion();
        }
        else if ( component instanceof IFolder ) {
            request = "SELECT id, name, version, checksum, created_on, model_id, model_version"
                    + " FROM "+this.schema+"folders"
                    + " LEFT JOIN "+this.schema+"folders_in_model ON folder_id = id AND folder_version = version"
                    + " WHERE id = ?"
                    + " ORDER BY version, model_version";
            DBArchimateModel model = (DBArchimateModel) ((IFolder)component).getArchimateModel();
            modelId = model.getId();
            modelInitialVersion = model.getInitialVersion().getVersion();
            modelDatabaseVersion = model.getDatabaseVersion().getVersion();
        }
        else if ( component instanceof IDiagramModel ) {
            request = "SELECT id, name, version, checksum, container_checksum, created_on, model_id, model_version"
                    + " FROM "+this.schema+"views"
                    + " LEFT JOIN "+this.schema+"views_in_model ON view_id = id AND view_version = version"
                    + " WHERE id = ?"
                    + " ORDER BY version, model_version";
            DBArchimateModel model = (DBArchimateModel) ((IDiagramModel)component).getArchimateModel();
            modelId = model.getId();
            modelInitialVersion = model.getInitialVersion().getVersion();
            modelDatabaseVersion = model.getDatabaseVersion().getVersion();
        }
        else if ( component instanceof IDiagramModelObject  ) {
            request = "SELECT id, name, version, checksum, created_on, view_id as model_id, view_version as model_version"		// for convenience, we rename view_id to model_id and view_version to model_version
                    + " FROM "+this.schema+"views_objects"
                    + " LEFT JOIN "+this.schema+"views_objects_in_view ON object_id = id AND object_version = version"
                    + " WHERE id = ?"
                    + " ORDER BY version, view_version";
            IDiagramModel diagram = ((IDiagramModelObject)component).getDiagramModel();
            modelId = diagram.getId();
            modelInitialVersion = ((IDBMetadata)diagram).getDBMetadata().getInitialVersion().getVersion();
            modelDatabaseVersion = ((IDBMetadata)diagram).getDBMetadata().getDatabaseVersion().getVersion();
        }
        else if ( component instanceof IDiagramModelConnection  ) {
            request = "SELECT id, name, version, checksum, created_on, view_id as model_id, view_version as model_version"		// for convenience, we rename view_id to model_id and view_version to model_version
                    + " FROM "+this.schema+"views_connections"
                    + " LEFT JOIN "+this.schema+"views_connections_in_view ON connection_id = id AND connection_version = version"
                    + " WHERE id = ?"
                    + " ORDER BY version, view_version";
            IDiagramModel diagram = ((IDiagramModelConnection)component).getDiagramModel();
            modelId = diagram.getId();
            modelInitialVersion = ((IDBMetadata)diagram).getDBMetadata().getInitialVersion().getVersion();
            modelDatabaseVersion = ((IDBMetadata)diagram).getDBMetadata().getDatabaseVersion().getVersion();
        }
        else
            throw new SQLException("Do not know how to get a "+component.getClass().getSimpleName()+" from the database.");

        metadata.getCurrentVersion().setVersion(0);
        metadata.getInitialVersion().reset();
        metadata.getDatabaseVersion().reset();
        metadata.getLatestDatabaseVersion().reset();

        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, request, component.getId()) ) {
            int version = 0;
            String checksum = null;
            String containerChecksum = null;
            Timestamp createdOn = null;
            while ( result.next() ) {
                version = result.getInt("version");
                checksum = result.getString("checksum");
                containerChecksum = (component instanceof IDiagramModel ? result.getString("container_checksum") : null);
                createdOn = result.getTimestamp("created_on");

                if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
                    // if the component is part of the model, we compare with the model's version
                    if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
                        metadata.getInitialVersion().set(version, containerChecksum, checksum, createdOn);
                        metadata.getCurrentVersion().setVersion(version);
                    }
                    if ( result.getInt("model_version") == modelDatabaseVersion )
                        metadata.getDatabaseVersion().set(version, containerChecksum, checksum, createdOn);
                }

                // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                metadata.getLatestDatabaseVersion().set(version, containerChecksum, checksum, createdOn);
            }
        }
        if ( logger.isTraceEnabled() ) {
            logger.trace("         Initial version = " + metadata.getInitialVersion().getVersion());
            logger.trace("         Current version = " + metadata.getCurrentVersion().getVersion());
            logger.trace("         Database version = "+ metadata.getDatabaseVersion().getVersion());
            logger.trace("         Latest db version = "+ metadata.getLatestDatabaseVersion().getVersion());
            logger.trace("         Database status = " + metadata.getDatabaseStatus());
        }
    }

    /**
     * Gets the version of all the model's components, and checks as well for the components that are in the latest model's version in the database but that are not in the model.
     * Those components are stored in the << not in model >> hashmaps:
     * <ul>
     *    <li> elementsNotInModel
     *    <li> relationshipsNotInModel
     *    <li> foldersNotInModel
     *    <li> viewsNotInModel
     *    <li> viewObjectsNotInModel
     *    <li> viewConnectionsNotInModel
     *    <li> imagesNotInModel
     *    <li> imagesNotInDatabase
     * </ul>
     * @param model
     * @throws SQLException
     */
    public void getAllVersionFromDatabase(DBArchimateModel model) throws SQLException {
        // we do not manage versions in a Neo4J database
        assert(!DBPlugin.areEqual(this.databaseEntry.getDriver().toLowerCase(), "neo4j"));

        // we reset the variables
        this.elementsNotInModel.clear();
        this.relationshipsNotInModel.clear();
        this.foldersNotInModel.clear();
        this.viewsNotInModel.clear();
        this.viewObjectsNotInModel.clear();
        this.viewConnectionsNotInModel.clear();
        this.imagesNotInModel.clear();
        this.imagesNotInDatabase.clear();

        getModelVersionFromDatabase(model);

        String modelId = model.getId();
        int modelInitialVersion = model.getInitialVersion().getVersion();
        int modelDatabaseVersion = model.getDatabaseVersion().getVersion();

        //////////////////// ELEMENTS
        // we reset the version of all the elements in the model
        if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the elements from the database");
        Iterator<Map.Entry<String, IArchimateElement>> ite = model.getAllElements().entrySet().iterator();
        while (ite.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)ite.next().getValue()).getDBMetadata();
            metadata.getCurrentVersion().setVersion(0);
            metadata.getInitialVersion().reset();
            metadata.getDatabaseVersion().reset();
            metadata.getLatestDatabaseVersion().reset();
        }
        // we get all the elements that are part of the latest version of the model in the database and compare them to the actual model
        // we do not use max(version) in the SQL request as all database brands do not support it
        // so we get all the version (sorted by the version) and determine the latest version of each element when the ID changes or when we reah the latest element
        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
                "SELECT id, name, version, checksum, created_on, model_id, model_version"
                        + " FROM "+this.schema+"elements"
                        + " LEFT JOIN "+this.schema+"elements_in_model ON element_id = id AND element_version = version"
                        + " WHERE id IN (SELECT id FROM "+this.schema+"elements JOIN "+this.schema+"elements_in_model ON element_id = id AND element_version = version WHERE model_id = ? AND model_version = ?)"
                        + " ORDER BY id, version, model_version"
                        ,modelId
                        ,modelDatabaseVersion
                ) ) {
        	
            DBMetadata currentComponent = null;
            DBMetadata previousComponent = null;
            String currentId = null;
            String previousId = null;
            //for each element in the database's model
            while ( result.next() ) {
                currentId = result.getString("id");
                int version = result.getInt("version");
                String checksum = result.getString("checksum");
                Timestamp createdOn = result.getTimestamp("created_on");

                // if the current element ID is equals to the previous element ID, it means that we got a newer version of the same element
                // so we just archive the current component as previous component for future reference 
                if ( DBPlugin.areEqual(currentId, previousId) )
                    currentComponent = previousComponent;
                else {
                	// If the current ID is different from the previous ID, it means we got a new component and not a new version of the same component
                    if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                    	// If there is a previous component, we print some trace information
                        logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                        logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                        logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                        logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                        logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                    }
                    
                    // we check if the component is present in the actual model
                    IArchimateModelObject object = model.getAllElements().get(currentId);
                    currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();

                    // if the component is not in the model, we create a DBMetadata class and store it in the elementsNotInModel list
                    if ( currentComponent == null ) {
                        currentComponent = new DBMetadata(currentId);
                        this.elementsNotInModel.put(currentId, currentComponent);
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
                    } else
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
                }

                // we set the component versions
                if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
                    // if the component is part of the model, we compare with the model's version
                    if ( modelInitialVersion == 0 || modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
                        currentComponent.getInitialVersion().set(version, checksum, createdOn);
                        currentComponent.getCurrentVersion().setVersion(version);
                    }
                    if ( result.getInt("model_version") == modelDatabaseVersion )
                        currentComponent.getDatabaseVersion().set(version, checksum, createdOn);
                }

                // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn);

                // we copy currentComponent to previous component to be able to retrieve it in next loop
                previousComponent = currentComponent;
                previousId = currentId;
            }
            
            if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
            }
        }

        // If some elements have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
        // so we check one by one if they are completely new or if they exist, and may be in another model
        ite = model.getAllElements().entrySet().iterator();
        while (ite.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)ite.next().getValue()).getDBMetadata();
            if ( metadata.getInitialVersion().getVersion() == 0 )
                getVersionFromDatabase((IIdentifier)metadata.getComponent());
        }

        // relationships
        if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the relationships from the database");
        Iterator<Map.Entry<String, IArchimateRelationship>> itr = model.getAllRelationships().entrySet().iterator();
        while (itr.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
            metadata.getCurrentVersion().setVersion(0);
            metadata.getInitialVersion().reset();
            metadata.getDatabaseVersion().reset();
            metadata.getLatestDatabaseVersion().reset();
        }
        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
                "SELECT id, name, version, checksum, created_on, model_id, model_version"
                        + " FROM "+this.schema+"relationships"
                        + " LEFT JOIN "+this.schema+"relationships_in_model ON relationship_id = id AND relationship_version = version"
                        + " WHERE id IN (SELECT id FROM "+this.schema+"relationships JOIN "+this.schema+"relationships_in_model ON relationship_id = id AND relationship_version = version WHERE model_id = ? AND model_version = ?)"
                        + " ORDER BY id, version, model_version"
                        ,modelId
                        ,modelDatabaseVersion
                ) ) {
            String previousId = null;
            DBMetadata previousComponent = null;
            while ( result.next() ) {
                DBMetadata currentComponent;
                String currentId = result.getString("id");
                int version = result.getInt("version");
                String checksum = result.getString("checksum");
                Timestamp createdOn = result.getTimestamp("created_on");

                if ( DBPlugin.areEqual(currentId, previousId) )
                    currentComponent = previousComponent;
                else {
                    if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                        logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                        logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                        logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                        logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                        logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                    }
                    
                    IArchimateModelObject object = model.getAllRelationships().get(currentId);
                    currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();

                    // the loop returns all the versions of all the model components
                    if ( currentComponent == null ) {
                        currentComponent = new DBMetadata(currentId);
                        this.relationshipsNotInModel.put(currentId, currentComponent);
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
                    } else
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
                }

                if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
                    // if the component is part of the model, we compare with the model's version
                    if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
                        currentComponent.getInitialVersion().set(version, checksum, createdOn);
                        currentComponent.getCurrentVersion().setVersion(version);
                    }
                    if ( result.getInt("model_version") == modelDatabaseVersion )
                        currentComponent.getDatabaseVersion().set(version, checksum, createdOn);
                }

                // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn);

                previousComponent = currentComponent;
                previousId = currentId;
            }
            
            if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
            }
        }

        // If some relationships have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
        // so we check one by one if they are completely new or if they exist, and may be in another model
        itr = model.getAllRelationships().entrySet().iterator();
        while (itr.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
            if ( metadata.getInitialVersion().getVersion() == 0 )
                getVersionFromDatabase((IIdentifier)metadata.getComponent());
        }

        // folders
        if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the folders from the database");
        Iterator<Map.Entry<String, IFolder>> itf = model.getAllFolders().entrySet().iterator();
        while (itf.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itf.next().getValue()).getDBMetadata();
            metadata.getCurrentVersion().setVersion(0);
            metadata.getInitialVersion().reset();
            metadata.getDatabaseVersion().reset();
            metadata.getLatestDatabaseVersion().reset();
        }
        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
                "SELECT id, name, version, checksum, created_on, model_id, model_version"
                        + " FROM "+this.schema+"folders"
                        + " LEFT JOIN "+this.schema+"folders_in_model ON folder_id = id AND folder_version = version"
                        + " WHERE id IN (SELECT id FROM "+this.schema+"folders JOIN "+this.schema+"folders_in_model ON folder_id = id AND folder_version = version WHERE model_id = ? AND model_version = ?)"
                        + " ORDER BY id, version, model_version"
                        ,modelId
                        ,modelDatabaseVersion
                ) ) {
            String previousId = null;
            DBMetadata previousComponent = null;
            while ( result.next() ) {
                DBMetadata currentComponent;
                String currentId = result.getString("id");
                int version = result.getInt("version");
                String checksum = result.getString("checksum");
                Timestamp createdOn = result.getTimestamp("created_on");

                if ( DBPlugin.areEqual(currentId, previousId) )
                    currentComponent = previousComponent;
                else {
                    if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                        logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                        logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                        logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                        logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                        logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                    }
                    
                    IArchimateModelObject object = model.getAllFolders().get(currentId);
                    currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();

                    // the loop returns all the versions of all the model components
                    if ( currentComponent == null ) {
                        currentComponent = new DBMetadata(currentId);
                        this.foldersNotInModel.put(currentId, currentComponent);
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
                    } else
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
                }

                if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
                    // if the component is part of the model, we compare with the model's version
                    if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
                        currentComponent.getInitialVersion().set(version, checksum, createdOn);
                        currentComponent.getCurrentVersion().setVersion(version);
                    }
                    if ( result.getInt("model_version") == modelDatabaseVersion )
                        currentComponent.getDatabaseVersion().set(version, checksum, createdOn);
                }

                // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn);

                previousComponent = currentComponent;
                previousId = currentId;
            }
            
            if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
            }
        }

        // If some folders have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
        // so we check one by one if they are completely new or if they exist, and may be in another model
        itf = model.getAllFolders().entrySet().iterator();
        while (itf.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itf.next().getValue()).getDBMetadata();
            if ( metadata.getInitialVersion().getVersion() == 0 )
                getVersionFromDatabase((IIdentifier)metadata.getComponent());
        }

        if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the views from the database");
        Iterator<Map.Entry<String, IDiagramModel>> itv = model.getAllViews().entrySet().iterator();
        while (itv.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
            metadata.getCurrentVersion().setVersion(0);
            metadata.getInitialVersion().reset();
            metadata.getDatabaseVersion().reset();
            metadata.getLatestDatabaseVersion().reset();
        }
        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
                "SELECT id, name, version, checksum, container_checksum, created_on, model_id, model_version"
                        + " FROM "+this.schema+"views"
                        + " LEFT JOIN "+this.schema+"views_in_model ON view_id = id AND view_version = version"
                        + " WHERE id IN (SELECT id FROM "+this.schema+"views JOIN "+this.schema+"views_in_model ON view_id = id AND view_version = version WHERE model_id = ? AND model_version = ?)"
                        + " ORDER BY id, version, model_version"
                        ,modelId
                        ,modelDatabaseVersion
                ) ) {
            String previousId = null;
            DBMetadata previousComponent = null;
            while ( result.next() ) {
                DBMetadata currentComponent;
                String currentId = result.getString("id");
                int version = result.getInt("version");
                String checksum = result.getString("checksum");
                String containerChecksum = result.getString("container_checksum");
                Timestamp createdOn = result.getTimestamp("created_on");

                if ( DBPlugin.areEqual(currentId, previousId) )
                    currentComponent = previousComponent;
                else {
                    if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                        logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                        logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                        logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                        logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                        logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                    }
                    
                    IArchimateModelObject object = model.getAllViews().get(currentId);
                    currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();

                    // the loop returns all the versions of all the model components
                    if ( currentComponent == null ) {
                        currentComponent = new DBMetadata(currentId);
                        this.viewsNotInModel.put(currentId, currentComponent);
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
                    } else
                        logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
                }

                if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
                    // if the component is part of the model, we compare with the model's version
                    if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
                        currentComponent.getInitialVersion().set(version, containerChecksum, checksum, createdOn);
                        currentComponent.getCurrentVersion().setVersion(version);
                    }
                    if ( result.getInt("model_version") == modelDatabaseVersion )
                        currentComponent.getDatabaseVersion().set(version,  containerChecksum, checksum, createdOn);
                }

                // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                currentComponent.getLatestDatabaseVersion().set(version, containerChecksum, checksum, createdOn);

                previousComponent = currentComponent;
                previousId = currentId;
            }
            
            if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
            }
        }

        // If some views have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
        // so we check one by one if they are completely new or if they exist, and may be in another model
        itv = model.getAllViews().entrySet().iterator();
        while (itv.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
            if ( metadata.getInitialVersion().getVersion() == 0 )
                getVersionFromDatabase((IIdentifier)metadata.getComponent());
        }

        // we check if the latest version of the model has got images that are not in the model
        if ( logger.isDebugEnabled() ) logger.debug("Checking missing images from the database");
        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT DISTINCT image_path FROM "+this.schema+"views_objects "
                + "JOIN "+this.schema+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version "
                + "JOIN "+this.schema+"views_in_model ON views_in_model.view_id = views_objects_in_view.view_id AND views_in_model.view_version = views_objects_in_view.view_version "
                + "WHERE image_path IS NOT NULL AND views_in_model.model_id = ? AND views_in_model.model_version = ?"
                ,model.getId()
                ,model.getDatabaseVersion().getVersion()
                ) ) {
            while ( result.next() ) {
                if ( !model.getAllImagePaths().contains(result.getString("image_path")) ) {
                    this.imagesNotInModel.put(result.getString("image_path"), new DBMetadata());
                }
            }
        }
        
        // we compare the objects and connections of existing views
        Iterator<Entry<String, IDiagramModel>> viewsIterator = model.getAllViews().entrySet().iterator();
        while ( viewsIterator.hasNext() )
            getViewObjectsAndConnectionsVersionsFromDatabase(model, ((IDBMetadata)viewsIterator.next().getValue()).getDBMetadata());

        // we also need to compare the objects and connections that are in the views that will be imported into the model
        Iterator<Entry<String, DBMetadata>> viewsNotInModelIterator = this.viewsNotInModel.entrySet().iterator();
        while ( viewsNotInModelIterator.hasNext() )
            getViewObjectsAndConnectionsVersionsFromDatabase(model, viewsNotInModelIterator.next().getValue());

        // even if the model does not exist in the database, the images can exist in the database
        // images do not have a version as they cannot be modified. Their path is a checksum and loading a new image creates a new path.

        // at last, we check if all the images in the model are in the database
        if ( logger.isDebugEnabled() ) logger.debug("Checking if the images exist in the database");
        for ( String path: model.getAllImagePaths() ) {
            try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT path from "+this.schema+"images where path = ?", path) ) {
                if ( result.next() && result.getObject("path") != null ) {
                    // the image is in the database
                } else {
                    // the image is not in the database
                    this.imagesNotInDatabase.put(path, new DBMetadata());
                }
            }
        }
    }

    private void getViewObjectsAndConnectionsVersionsFromDatabase(DBArchimateModel model, DBMetadata viewMetadata) throws SQLException, RuntimeException {
        // if the model is brand new, there is no point to check for its content in the database
        //if ( model.getInitialVersion().getVersion() != 0 ) {
            String viewId = viewMetadata.getId();


            int viewInitialVersion = viewMetadata.getInitialVersion().getVersion();
            int viewDatabaseVersion = viewMetadata.getLatestDatabaseVersion().getVersion();

            // view objects
            if ( logger.isDebugEnabled() ) logger.debug("Getting versions of view objects from the database for "+viewMetadata.getDebugName());
            Iterator<Map.Entry<String, IDiagramModelObject>> itvo = model.getAllViewObjects().entrySet().iterator();
            while (itvo.hasNext()) {
                IDiagramModelObject object = itvo.next().getValue();
                if ( object.getDiagramModel().getId().equals(viewMetadata.getId()) ) {
                    DBMetadata metadata = ((IDBMetadata)object).getDBMetadata();
                    metadata.getCurrentVersion().setVersion(0);
                    metadata.getInitialVersion().reset();
                    metadata.getDatabaseVersion().reset();
                    metadata.getLatestDatabaseVersion().reset();
                }
            }			
            try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
                    "SELECT id, name, version, checksum, created_on, view_id, view_version"
                            + " FROM "+this.schema+"views_objects"
                            + " LEFT JOIN "+this.schema+"views_objects_in_view ON object_id = id AND object_version = version"
                            + " WHERE id IN (SELECT id FROM "+this.schema+"views_objects JOIN "+this.schema+"views_objects_in_view ON object_id = id AND object_version = version WHERE view_id = ? AND view_version = ?)"
                            + " ORDER BY id, version, view_version"
                            ,viewId
                            ,viewDatabaseVersion
                    ) ) {
                String previousId = null;
                DBMetadata previousComponent = null;
                while ( result.next() ) {
                    DBMetadata currentComponent;
                    String currentId = result.getString("id");
                    int version = result.getInt("version");
                    String checksum = result.getString("checksum");
                    Timestamp createdOn = result.getTimestamp("created_on");

                    if ( DBPlugin.areEqual(currentId, previousId) )
                        currentComponent = previousComponent;
                    else {
                        if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                            logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                            logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                            logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                            logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                            logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                        }
                        
                        IDiagramModelObject object = model.getAllViewObjects().get(currentId);
                        currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();

                        // the loop returns all the versions of all the model components
                        if ( currentComponent == null ) {
                            currentComponent = new DBMetadata(currentId);
                            this.viewObjectsNotInModel.put(currentId, currentComponent);
                            logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
                        } else
                            logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
                    }

                    if ( DBPlugin.areEqual(result.getString("view_id"), viewId) ) {
                        // if the component is part of the model, we compare with the model's version
                        if ( result.getInt("view_version") == viewInitialVersion ) {
                            currentComponent.getInitialVersion().set(version, checksum, createdOn);
                            currentComponent.getCurrentVersion().setVersion(version);
                        }
                        if ( result.getInt("view_version") == viewDatabaseVersion ) {
                            currentComponent.getDatabaseVersion().set(version, checksum, createdOn);
                        }
                    }

                    // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                    currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn);

                    previousComponent = currentComponent;
                    previousId = currentId;
                }
                
                if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                    logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                    logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                    logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                    logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                    logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                }
            }

            // If some view objects have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
            // so we check one by one if they are completely new or if they exist, and may be in another model
            itvo = model.getAllViewObjects().entrySet().iterator();
            while (itvo.hasNext()) {
                DBMetadata metadata = ((IDBMetadata)itvo.next().getValue()).getDBMetadata();
                if ( metadata.getInitialVersion().getVersion() == 0 )
                    getVersionFromDatabase((IIdentifier)metadata.getComponent());
            }

            // view connections
            if ( logger.isDebugEnabled() ) logger.debug("Getting versions of view connections from the database for "+viewMetadata.getDebugName());
            Iterator<Map.Entry<String, IDiagramModelConnection>> itvc = model.getAllViewConnections().entrySet().iterator();
            while (itvc.hasNext()) {
                IDiagramModelConnection cnct = itvc.next().getValue();
                if ( cnct.getDiagramModel().getId().equals(viewMetadata.getId()) ) {
                    DBMetadata metadata = ((IDBMetadata)cnct).getDBMetadata();
                    metadata.getCurrentVersion().setVersion(0);
                    metadata.getInitialVersion().reset();
                    metadata.getDatabaseVersion().reset();
                    metadata.getLatestDatabaseVersion().reset();
                }
            }
            try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
                    "SELECT id, name, version, checksum, created_on, view_id, view_version"
                            + " FROM "+this.schema+"views_connections"
                            + " LEFT JOIN "+this.schema+"views_connections_in_view ON connection_id = id AND connection_version = version"
                            + " WHERE id IN (SELECT id FROM "+this.schema+"views_connections JOIN "+this.schema+"views_connections_in_view ON connection_id = id AND connection_version = version WHERE view_id = ? AND view_version = ?)"
                            + " ORDER BY id, version, view_version"
                            ,viewId
                            ,viewDatabaseVersion
                    ) ) {
                String previousId = null;
                DBMetadata previousComponent = null;
                while ( result.next() ) {
                    DBMetadata currentComponent;
                    String currentId = result.getString("id");
                    int version = result.getInt("version");
                    String checksum = result.getString("checksum");
                    Timestamp createdOn = result.getTimestamp("created_on");

                    if ( DBPlugin.areEqual(currentId, previousId) )
                        currentComponent = previousComponent;
                    else {
                        if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                            logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                            logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                            logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                            logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                            logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                        }
                        
                        IDiagramModelConnection object = model.getAllViewConnections().get(currentId);
                        currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();

                        // the loop returns all the versions of all the model components
                        if ( currentComponent == null ) {
                            currentComponent = new DBMetadata(currentId);
                            this.viewConnectionsNotInModel.put(currentId, currentComponent);
                            logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
                        } else
                            logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
                    }

                    if ( DBPlugin.areEqual(result.getString("view_id"), viewId) ) {
                        // if the component is part of the model, we compare with the model's version
                        if ( result.getInt("view_version") == viewInitialVersion ) {
                            currentComponent.getInitialVersion().set(version, checksum, createdOn);
                            currentComponent.getCurrentVersion().setVersion(version);
                        }
                        if ( result.getInt("view_version") == viewDatabaseVersion ) {
                            currentComponent.getDatabaseVersion().set(version, checksum, createdOn);
                        }
                    }

                    // components are sorted by version (so also by timestamp) so the latest found is the latest in time
                    currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn);

                    previousComponent = currentComponent;
                    previousId = currentId;
                }
                
                if ( (previousComponent != null) && logger.isTraceEnabled() ) {
                    logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
                    logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
                    logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
                    logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
                    logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
                }
            }

            // If some view connections have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
            // so we check one by one if they are completely new or if they exist, and may be in another model
            itvc = model.getAllViewConnections().entrySet().iterator();
            while (itvc.hasNext()) {
                DBMetadata metadata = ((IDBMetadata)itvc.next().getValue()).getDBMetadata();
                if ( metadata.getInitialVersion().getVersion() == 0 )
                    getVersionFromDatabase((IIdentifier)metadata.getComponent());
            }
        //}
    }

    /**
     * Empty a Neo4J database
     */
    public void emptyNeo4jDB() throws Exception {
        if ( logger.isDebugEnabled() ) logger.debug("Emptying Neo4J database.");
        executeRequest("MATCH (n) DETACH DELETE n");
    }


    /**
     * Exports the model metadata into the database
     * @param model 
     * @param releaseNote 
     * @throws Exception 
     */
    public void exportModel(DBArchimateModel model, String releaseNote) throws Exception {
        final String[] modelsColumns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "checksum"};

        if ( (model.getName() == null) || (model.getName().equals("")) )
            throw new RuntimeException("Model name cannot be empty.");

        model.getCurrentVersion().setVersion(model.getDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting model (initial version = "+model.getInitialVersion().getVersion()+", exported version = "+model.getCurrentVersion().getVersion()+", latest database version = "+model.getDatabaseVersion().getVersion()+")");

        if ( this.connection.getAutoCommit() )
            model.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
        else
            model.getCurrentVersion().setTimestamp(this.lastTransactionTimestamp);

        insert(this.schema+"models", modelsColumns
                ,model.getId()
                ,model.getCurrentVersion().getVersion()
                ,model.getName()
                ,releaseNote
                ,model.getPurpose()
                ,System.getProperty("user.name")
                ,model.getCurrentVersion().getTimestamp()
                ,model.getCurrentVersion().getChecksum()
                );

        exportProperties(model);
        exportMetadata(model);
    }

    /**
     * Export a component to the database
     * @param eObject 
     * @throws Exception 
     */
    public void exportEObject(EObject eObject) throws Exception {
        if ( eObject instanceof IArchimateElement ) 			exportElement((IArchimateElement)eObject);
        else if ( eObject instanceof IArchimateRelationship ) 	exportRelationship((IArchimateRelationship)eObject);
        else if ( eObject instanceof IFolder ) 					exportFolder((IFolder)eObject);
        else if ( eObject instanceof IDiagramModel ) 			exportView((IDiagramModel)eObject);
        else if ( eObject instanceof IDiagramModelObject )		exportViewObject((IDiagramModelComponent)eObject);
        else if ( eObject instanceof IDiagramModelConnection )	exportViewConnection((IDiagramModelConnection)eObject);
        else
            throw new Exception("Do not know how to export "+eObject.getClass().getSimpleName());
    }

    /**
     * @param eObject
     * @throws Exception
     */
    public void assignEObjectToModel(EObject eObject) throws Exception {
        if ( eObject instanceof IArchimateElement )				assignElementToModel((IArchimateElement)eObject);
        else if ( eObject instanceof IArchimateRelationship )	assignRelationshipToModel((IArchimateRelationship)eObject);
        else if ( eObject instanceof IFolder )					assignFolderToModel((IFolder)eObject);
        else if ( eObject instanceof IDiagramModel )			assignViewToModel((IDiagramModel)eObject);
        else if ( eObject instanceof IDiagramModelObject )		assignViewObjectToView((IDiagramModelObject)eObject);
        else if ( eObject instanceof IDiagramModelConnection )	assignViewConnectionToView((IDiagramModelConnection)eObject);
        else
            throw new Exception("Do not know how to assign to the model: "+eObject.getClass().getSimpleName());
    }

    /**
     * Export an element to the database
     */
    private void exportElement(IArchimateConcept element) throws Exception {
        final String[] elementsColumns = {"id", "version", "class", "name", "type", "documentation", "created_by", "created_on", "checksum"};

        // if the element is exported, the we increase its exportedVersion
        ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)element).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)element).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)element).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

        if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
            // TODO: USE MERGE instead to replace existing nodes
            executeRequest("CREATE (new:elements {id:?, version:?, class:?, name:?, type:?, documentation:?, checksum:?})"
                    ,element.getId()
                    ,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()
                    ,element.getClass().getSimpleName()
                    ,element.getName()
                    ,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
                    ,element.getDocumentation()
                    ,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getChecksum()
                    );
        } else {
            insert(this.schema+"elements", elementsColumns
                    ,element.getId()
                    ,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()
                    ,element.getClass().getSimpleName()
                    ,element.getName()
                    ,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
                    ,element.getDocumentation()
                    ,System.getProperty("user.name")
                    ,((DBArchimateModel)element.getArchimateModel()).getCurrentVersion().getTimestamp()
                    ,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getChecksum()
                    );
        }

        exportProperties(element);
    }

    /**
     * This class variable allows to sort the exported elements that they are imported in the same order<br>
     * It is reset to zero each time a connection to a new database is done (connection() method).
     */
    private int elementRank = 0;

    /**
     * Assign an element to a model into the database
     */
    private void assignElementToModel(IArchimateConcept element) throws Exception {
        final String[] elementsInModelColumns = {"element_id", "element_version", "parent_folder_id", "model_id", "model_version", "rank"};
        DBArchimateModel model = (DBArchimateModel)element.getArchimateModel();

        if ( logger.isTraceEnabled() ) logger.trace("   Assigning element to model");

        insert(this.schema+"elements_in_model", elementsInModelColumns
                ,element.getId()
                ,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportElement()
                ,((IFolder)element.eContainer()).getId()
                ,model.getId()
                ,model.getCurrentVersion().getVersion()
                ,++this.elementRank
                );
    }

    /**
     * Export a relationship to the database
     */
    private void exportRelationship(IArchimateConcept relationship) throws Exception {
        final String[] relationshipsColumns = {"id", "version", "class", "name", "documentation", "source_id", "target_id", "strength", "access_type", "created_by", "created_on", "checksum"};

        // if the relationship is exported, the we increase its exportedVersion
        ((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)relationship).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)relationship).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

        if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
            String relationshipType = (this.databaseEntry.isNeo4jTypedRelationship() ? (relationship.getClass().getSimpleName()+"s") : "relationships");
            // TODO: USE MERGE instead to replace existing nodes
            
            // we remove the "Relationship" suffix from the relationship name
            String relationshipName = relationship.getClass().getSimpleName();
            if ( relationshipName.endsWith("Relationship") )
            	relationshipName = relationshipName.substring(0, relationshipName.length() - 12);
            
            if ( this.databaseEntry.isNeo4jNativeMode() ) {
                if ( (((IArchimateRelationship)relationship).getSource() instanceof IArchimateElement) && (((IArchimateRelationship)relationship).getTarget() instanceof IArchimateElement) ) {
                    executeRequest("MATCH (source:elements {id:?, version:?}), (target:elements {id:?, version:?}) CREATE (source)-[relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}]->(target)"
                            ,((IArchimateRelationship)relationship).getSource().getId()
                            ,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion().getVersion()
                            ,((IArchimateRelationship)relationship).getTarget().getId()
                            ,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion().getVersion()
                            ,relationship.getId()
                            ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
                            ,relationshipName
                            ,relationship.getName()
                            ,relationship.getDocumentation()
                            ,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
                            ,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
                            ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
                            );
                }
            } else {
                executeRequest("MATCH (source {id:?, version:?}), (target {id:?, version:?}) CREATE (relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}), (source)-[rel1:relatedTo]->(relationship)-[rel2:relatedTo]->(target)"
                        ,((IArchimateRelationship)relationship).getSource().getId()
                        ,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion().getVersion()
                        ,((IArchimateRelationship)relationship).getTarget().getId()
                        ,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion().getVersion()
                        ,relationship.getId()
                        ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
                        ,relationship.getClass().getSimpleName()
                        ,relationship.getName()
                        ,relationship.getDocumentation()
                        ,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
                        ,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
                        ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
                        );
            }
        } else {
            insert(this.schema+"relationships", relationshipsColumns
                    ,relationship.getId()
                    ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
                    ,relationship.getClass().getSimpleName()
                    ,relationship.getName()
                    ,relationship.getDocumentation()
                    ,((IArchimateRelationship)relationship).getSource().getId()
                    ,((IArchimateRelationship)relationship).getTarget().getId()
                    ,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
                    ,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
                    ,System.getProperty("user.name")
                    ,((DBArchimateModel)relationship.getArchimateModel()).getCurrentVersion().getTimestamp()
                    ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
                    );
        }

        exportProperties(relationship);
    }

    /**
     * This class variable allows to sort the exported relationships that they are imported in the same order<br>
     * It is reset to zero each time a connection to a new database is done (connection() method).
     */
    private int relationshipRank = 0;

    /**
     * Assign a relationship to a model into the database
     */
    private void assignRelationshipToModel(IArchimateConcept relationship) throws Exception {
        final String[] relationshipsInModelColumns = {"relationship_id", "relationship_version", "parent_folder_id", "model_id", "model_version", "rank"};

        DBArchimateModel model = (DBArchimateModel)relationship.getArchimateModel();

        if ( logger.isTraceEnabled() ) logger.trace("   Assigning relationship to model");

        insert(this.schema+"relationships_in_model", relationshipsInModelColumns
                ,relationship.getId()
                ,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
                ,((IFolder)relationship.eContainer()).getId()
                ,model.getId()
                ,model.getCurrentVersion().getVersion()
                ,++this.relationshipRank
                );
    }

    /**
     * Export a folder into the database.
     */
    private void exportFolder(IFolder folder) throws Exception {
        final String[] foldersColumns = {"id", "version", "type", "root_type", "name", "documentation", "created_by", "created_on", "checksum"};

        // if the folder is exported, the we increase its exportedVersion
        ((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)folder).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)folder).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

        insert(this.schema+"folders", foldersColumns
                ,folder.getId()
                ,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
                ,folder.getType().getValue()
                ,((IDBMetadata)folder).getDBMetadata().getRootFolderType()
                ,folder.getName()
                ,folder.getDocumentation()
                ,System.getProperty("user.name")
                ,((DBArchimateModel)folder.getArchimateModel()).getCurrentVersion().getTimestamp()
                ,((Folder)folder).getDBMetadata().getCurrentVersion().getChecksum()
                );

        exportProperties(folder);
    }


    /**
     * This class variable allows to sort the exported folders that they are imported in the same order<br>
     * It is reset to zero each time a connection to a new database is done (connection() method).
     */
    private int folderRank = 0;

    /**
     * Assign a folder to a model into the database
     */
    private void assignFolderToModel(IFolder folder) throws Exception {
        final String[] foldersInModelColumns = {"folder_id", "folder_version", "parent_folder_id", "model_id", "model_version", "rank"};
        DBArchimateModel model = (DBArchimateModel)folder.getArchimateModel();

        if ( logger.isTraceEnabled() ) logger.trace("   Assigning folder to model");

        insert(this.schema+"folders_in_model", foldersInModelColumns
                ,folder.getId()
                ,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
                ,(((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId())
                ,model.getId()
                ,model.getCurrentVersion().getVersion()
                ,++this.folderRank
                );
    }

    /**
     * Export a view into the database.
     */
    private void exportView(IDiagramModel view) throws Exception {
        final String[] ViewsColumns = {"id", "version", "class", "created_by", "created_on", "name", "connection_router_type", "documentation", "viewpoint", "background", "screenshot", "screenshot_scale_factor", "screenshot_border_width", "checksum", "container_checksum"};

        // if the view is exported, the we increase its exportedVersion
        ((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)view).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)view).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)view).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

        insert(this.schema+"views", ViewsColumns
                ,view.getId()
                ,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
                ,view.getClass().getSimpleName()
                ,System.getProperty("user.name")
                ,((DBArchimateModel)view.getArchimateModel()).getCurrentVersion().getTimestamp()
                ,view.getName()
                ,view.getConnectionRouterType()
                ,view.getDocumentation()
                ,((view instanceof IArchimateDiagramModel) ? ((IArchimateDiagramModel)view).getViewpoint() : null)
                ,((view instanceof ISketchModel) ? ((ISketchModel)view).getBackground() : null)
                ,((IDBMetadata)view).getDBMetadata().getScreenshot().getBytes()
                ,((IDBMetadata)view).getDBMetadata().getScreenshot().getScaleFactor()
                ,((IDBMetadata)view).getDBMetadata().getScreenshot().getBodrderWidth()
                ,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getChecksum()
                ,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getContainerChecksum()
                );

        exportProperties(view);
    }

    /**
     * This class variable allows to sort the exported views that they are imported in the same order<br>
     * It is reset to zero each time a connection to a new database is done (connection() method).
     */
    private int viewRank = 0;

    /**
     * Assign a view to a model into the database
     */
    private void assignViewToModel(IDiagramModel view) throws Exception {
        final String[] viewsInModelColumns = {"view_id", "view_version", "parent_folder_id", "model_id", "model_version", "rank"};
        DBArchimateModel model = (DBArchimateModel)view.getArchimateModel();

        if ( logger.isTraceEnabled() ) logger.trace("   Assigning view to model");

        insert(this.schema+"views_in_model", viewsInModelColumns
                ,view.getId()
                ,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
                ,((IFolder)view.eContainer()).getId()
                ,model.getId()
                ,model.getCurrentVersion().getVersion()
                ,++this.viewRank
                );
    }

    /**
     * This class variable allows to sort the exported views objects that they are imported in the same order<br>
     * It is reset to zero each time a connection to a new database is done (connection() method).
     */
    private int viewObjectRank = 0;

    /**
     * Export a view object into the database.<br>
     * The rank allows to order the views during the import process.
     */
    private void exportViewObject(IDiagramModelComponent viewObject) throws Exception {
        final String[] ViewsObjectsColumns = {"id", "version", "class", "container_id", "element_id", "diagram_ref_id", "type", "border_color", "border_type", "content", "documentation", "is_locked", "image_path", "image_position", "line_color", "line_width", "fill_color", "alpha", "font", "font_color", "name", "notes", "text_alignment", "text_position", "x", "y", "width", "height", "created_by", "created_on", "checksum"};

        // if the viewObject is exported, the we increase its exportedVersion
        ((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)viewObject).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)viewObject).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)viewObject).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)viewObject).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)viewObject).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

        insert(this.schema+"views_objects", ViewsObjectsColumns
                ,((IIdentifier)viewObject).getId()
                ,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()
                ,viewObject.getClass().getSimpleName()
                ,((IIdentifier)viewObject.eContainer()).getId()
                ,((viewObject instanceof IDiagramModelArchimateComponent) ? ((IDiagramModelArchimateComponent)viewObject).getArchimateConcept().getId() : null)
                ,((viewObject instanceof IDiagramModelReference) ? ((IDiagramModelReference)viewObject).getReferencedModel().getId() : null)
                ,((viewObject instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)viewObject).getType() : null)
                ,((viewObject instanceof IBorderObject) ? ((IBorderObject)viewObject).getBorderColor() : null)
                ,((viewObject instanceof IDiagramModelNote) ? ((IDiagramModelNote)viewObject).getBorderType() : null)
                ,((viewObject instanceof ITextContent) ? ((ITextContent)viewObject).getContent() : null)
                ,((viewObject instanceof IDocumentable && !(viewObject instanceof IDiagramModelArchimateComponent)) ? ((IDocumentable)viewObject).getDocumentation() : null)        // They have got there own documentation. The others use the documentation of the corresponding ArchimateConcept
                ,((viewObject instanceof ILockable) ? (((ILockable)viewObject).isLocked()?1:0) : null)
                ,((viewObject instanceof IDiagramModelImageProvider) ? ((IDiagramModelImageProvider)viewObject).getImagePath() : null)
                ,((viewObject instanceof IIconic) ? ((IIconic)viewObject).getImagePosition() : null)
                ,((viewObject instanceof ILineObject) ? ((ILineObject)viewObject).getLineColor() : null)
                ,((viewObject instanceof ILineObject) ? ((ILineObject)viewObject).getLineWidth() : null)
                ,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getFillColor() : null)
                ,((IDBMetadata)viewObject).getDBMetadata().getAlpha()
                ,((viewObject instanceof IFontAttribute) ? ((IFontAttribute)viewObject).getFont() : null)
                ,((viewObject instanceof IFontAttribute) ? ((IFontAttribute)viewObject).getFontColor() : null)
                ,viewObject.getName()																						// we export the name because it will be used in case of conflict
                ,((viewObject instanceof ICanvasModelSticky) ? ((ICanvasModelSticky)viewObject).getNotes() : null)
                ,((viewObject instanceof ITextAlignment) ? ((ITextAlignment)viewObject).getTextAlignment() : null)
                ,((viewObject instanceof ITextPosition) ? ((ITextPosition)viewObject).getTextPosition() : null)
                ,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getX() : null)
                ,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getY() : null)
                ,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getWidth() : null)
                ,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getHeight() : null)
                ,System.getProperty("user.name")
                ,((DBArchimateModel)viewObject.getDiagramModel().getArchimateModel()).getCurrentVersion().getTimestamp()
                ,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getChecksum()
                );

        if ( viewObject instanceof IProperties && !(viewObject instanceof IDiagramModelArchimateComponent))
            exportProperties((IProperties)viewObject);
    }

    /**
     * Assign a view Object to a view into the database
     */
    private void assignViewObjectToView(IDiagramModelComponent viewObject) throws Exception {
        final String[] viewObjectInViewColumns = {"object_id", "object_version", "view_id", "view_version", "rank"};
        IDiagramModel viewContainer = viewObject.getDiagramModel();

        if ( logger.isTraceEnabled() ) logger.trace("   Assigning view object to view");

        insert(this.schema+"views_objects_in_view", viewObjectInViewColumns
                ,viewObject.getId()
                ,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()
                ,viewContainer.getId()
                ,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getVersion()
                ,++this.viewObjectRank
                );


    }

    /**
     * This class variable allows to sort the exported views objects that they are imported in the same order<br>
     * It is reset to zero each time a connection to a new database is done (connection() method).
     */
    private int viewConnectionRank = 0;

    /**
     * Export a view connection into the database.<br>
     * The rank allows to order the views during the import process.
     */
    private void exportViewConnection(IDiagramModelConnection viewConnection) throws Exception {
        final String[] ViewsConnectionsColumns = {"id", "version", "class", "container_id", "name", "documentation", "is_locked", "line_color", "line_width", "font", "font_color", "relationship_id", "source_object_id", "target_object_id", "text_position", "type", "created_by", "created_on", "checksum"};
        final String[] bendpointsColumns = {"parent_id", "parent_version", "rank", "start_x", "start_y", "end_x", "end_y"};

        // if the viewConnection is exported, then we increase its exportedVersion
        ((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)viewConnection).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)viewConnection).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)viewConnection).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)viewConnection).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)viewConnection).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

        insert(this.schema+"views_connections", ViewsConnectionsColumns
                ,((IIdentifier)viewConnection).getId()
                ,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
                ,viewConnection.getClass().getSimpleName()
                ,((IIdentifier)viewConnection.eContainer()).getId()
                ,(!(viewConnection instanceof IDiagramModelArchimateConnection) ? ((INameable)viewConnection).getName() : null)                    // if there is a relationship behind, the name is the relationship name, so no need to store it.
                ,(!(viewConnection instanceof IDiagramModelArchimateConnection) ? ((IDocumentable)viewConnection).getDocumentation() : null)       // if there is a relationship behind, the documentation is the relationship name, so no need to store it.
                ,((viewConnection instanceof ILockable) ? (((ILockable)viewConnection).isLocked()?1:0) : null)  
                ,viewConnection.getLineColor()
                ,viewConnection.getLineWidth()
                ,viewConnection.getFont()
                ,viewConnection.getFontColor()
                ,((viewConnection instanceof IDiagramModelArchimateConnection) ? ((IDiagramModelArchimateConnection)viewConnection).getArchimateConcept().getId() : null)
                ,viewConnection.getSource().getId()
                ,viewConnection.getTarget().getId()
                ,viewConnection.getTextPosition()
                ,((viewConnection instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)viewConnection).getType() : viewConnection.getType())
                ,System.getProperty("user.name")
                ,((DBArchimateModel)viewConnection.getDiagramModel().getArchimateModel()).getCurrentVersion().getTimestamp()
                ,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getChecksum()
                );

        exportProperties(viewConnection);

        for ( int pos = 0 ; pos < viewConnection.getBendpoints().size(); ++pos) {
            IDiagramModelBendpoint bendpoint = viewConnection.getBendpoints().get(pos);
            insert(this.schema+"bendpoints", bendpointsColumns
                    ,((IIdentifier)viewConnection).getId()
                    ,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
                    ,pos
                    ,bendpoint.getStartX()
                    ,bendpoint.getStartY()
                    ,bendpoint.getEndX()
                    ,bendpoint.getEndY()
                    );
        }
    }

    /**
     * Assign a view Connection to a view into the database
     */
    private void assignViewConnectionToView(IDiagramModelConnection viewConnection) throws Exception {
        final String[] viewObjectInViewColumns = {"connection_id", "connection_version", "view_id", "view_version", "rank"};
        IDiagramModel viewContainer = viewConnection.getDiagramModel();

        if ( logger.isTraceEnabled() ) logger.trace("   Assigning view connection to view");

        insert(this.schema+"views_connections_in_view", viewObjectInViewColumns
                ,viewConnection.getId()
                ,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
                ,viewContainer.getId()
                ,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getVersion()
                ,++this.viewConnectionRank
                );


    }

    /**
     * Export properties to the database
     */
    private void exportProperties(IProperties parent) throws Exception {
        final String[] propertiesColumns = {"parent_id", "parent_version", "rank", "name", "value"};

        int exportedVersion;
        if ( parent instanceof DBArchimateModel ) {
            exportedVersion = ((DBArchimateModel)parent).getCurrentVersion().getVersion();
        } else 
            exportedVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

        for ( int propRank = 0 ; propRank < parent.getProperties().size(); ++propRank) {
            IProperty prop = parent.getProperties().get(propRank);
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
                executeRequest("MATCH (parent {id:?, version:?}) CREATE (prop:property {rank:?, name:?, value:?}), (parent)-[:hasProperty]->(prop)"
                        ,((IIdentifier)parent).getId()
                        ,exportedVersion
                        ,propRank
                        ,prop.getKey()
                        ,prop.getValue()
                        );
            }
            else
                insert(this.schema+"properties", propertiesColumns
                        ,((IIdentifier)parent).getId()
                        ,exportedVersion
                        ,propRank
                        ,prop.getKey()
                        ,prop.getValue()
                        );
        }
    }

    /**
     * Export properties to the database
     */
    private void exportMetadata(DBArchimateModel parent) throws Exception {
        final String[] metadataColumns = {"parent_id", "parent_version", "rank", "name", "value"};

        for ( int propRank = 0 ; propRank < parent.getMetadata().getEntries().size(); ++propRank) {
            IProperty prop = parent.getMetadata().getEntries().get(propRank);
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
                executeRequest("MATCH (parent {id:?, version:?}) CREATE (prop:metadata {rank:?, name:?, value:?}), (parent)-[:hasMetadata]->(prop)"
                        ,parent.getId()
                        ,parent.getCurrentVersion().getVersion()
                        ,propRank
                        ,prop.getKey()
                        ,prop.getValue()
                        );
            }
            else
                insert(this.schema+"metadata", metadataColumns
                        ,parent.getId()
                        ,parent.getCurrentVersion().getVersion()
                        ,propRank
                        ,prop.getKey()
                        ,prop.getValue()
                        );
        }
    }

    public boolean exportImage(String path, byte[] image) throws SQLException {
        // we do not export null images (should never happen, but it sometimes does)
        if ( image == null ) 
            return false;

        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT path FROM "+this.schema+"images WHERE path = ?", path) ) {
            if ( !result.next() ) {
                // if the image is not yet in the db, we insert it
                String[] databaseColumns = {"path", "image"};
                insert(this.schema+"images", databaseColumns, path, image);
                return true;
            }
        }
        return false;
    }

    public static String getTargetConnectionsString(EList<IDiagramModelConnection> connections) {
        StringBuilder target = new StringBuilder();
        for ( IDiagramModelConnection connection: connections ) {
            if ( target.length() > 0 )
                target.append(",");
            target.append(connection.getId());
        }
        return target.toString();
    }

    /**
     * Sets the auto-commit mode of the database
     */
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        super.setAutoCommit(autoCommit);

        if ( autoCommit )
            this.lastTransactionTimestamp = null;                                                         // all the request will have their own timestamp
        else
            this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());    // all the requests will have the same timestamp
    }

    /**
     * Commits the current transaction
     */
    @Override
    public void commit() throws SQLException {
        super.commit();
        this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
    }

    public void reset() {
        // We reset all "ranks" to zero
        this.elementRank = 0;
        this.relationshipRank = 0;
        this.folderRank = 0;
        this.viewRank = 0;
        this.viewObjectRank = 0;
        this.viewConnectionRank = 0;

        // we empty the hashmaps
        this.elementsNotInModel.clear();
        this.relationshipsNotInModel.clear();
        this.foldersNotInModel.clear();
        this.viewsNotInModel.clear();
        this.imagesNotInModel.clear();
        this.imagesNotInDatabase.clear();
    }

    /**
     * Closes connection to the database
     */
    @Override
    public void close() throws SQLException {
        reset();

        if ( !this.isImportconnectionDuplicate )
            super.close();
    }
}