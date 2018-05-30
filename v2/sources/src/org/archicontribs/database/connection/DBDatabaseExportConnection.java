/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.data.DBVersionPair;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.impl.Folder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IConnectable;
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
	 */
	public DBDatabaseExportConnection(DBDatabaseEntry databaseEntry) throws ClassNotFoundException, SQLException {
		super(databaseEntry);
	}
	
	/**
     * duplicates a connection to a JDBC database to allow switching between importConnection and exportConnection
     */
    public DBDatabaseExportConnection(DBDatabaseConnection databaseConnection) {
        super();
        assert(databaseConnection != null);
        super.databaseEntry = databaseConnection.databaseEntry;
        super.schema = databaseConnection.schema;
        super.connection = databaseConnection.connection;
        this.isImportconnectionDuplicate = true;
    }

	@Getter private HashMap<String, DBVersionPair> elementsNotInModel = new HashMap<String, DBVersionPair>();
	@Getter private HashMap<String, DBVersionPair> relationshipsNotInModel = new HashMap<String, DBVersionPair>();
	@Getter private HashMap<String, DBVersionPair> foldersNotInModel = new LinkedHashMap<String, DBVersionPair>();			// must keep the order
	@Getter private HashMap<String, DBVersionPair> viewsNotInModel = new HashMap<String, DBVersionPair>();
	@Getter private HashMap<String, DBVersionPair> viewObjectsNotInModel = new LinkedHashMap<String, DBVersionPair>();		// must keep the order
	@Getter private HashMap<String, DBVersionPair> viewConnectionsNotInModel = new LinkedHashMap<String, DBVersionPair>();	// must keep the order
	@Getter private HashMap<String, DBVersionPair> imagesNotInModel = new HashMap<String, DBVersionPair>();
	@Getter private HashMap<String, DBVersionPair> imagesNotInDatabase = new HashMap<String, DBVersionPair>();
	
    /**
     * Gets the versions and checksum of one model's components from the database and fills their DBMetadata.<br>
     * <br>
     * Components that are not in the current model are set in elementsNotInModel, relationshipsNotInModel, foldersNotInModel and viewsNotInModel.<br>
     * <br>
     * This method is meant to be called during the export process, before the export of the components as it sets the DBVersion variables used during the export process.
     * @throws SQLException
     */
    public void getVersionsFromDatabase(DBArchimateModel model) throws SQLException, RuntimeException {
        if ( logger.isDebugEnabled() ) logger.debug("Getting versions from the database");

        // This method can retrieve versions only if the database contains the whole model tables
        assert(!this.databaseEntry.isWholeModelExported());
        
        // we reset the variables
    	this.elementsNotInModel.clear();
    	this.relationshipsNotInModel.clear();
    	this.foldersNotInModel.clear();
    	this.viewsNotInModel.clear();
    	this.viewObjectsNotInModel.clear();
    	this.viewConnectionsNotInModel.clear();
    	this.imagesNotInModel.clear();
    	this.imagesNotInDatabase.clear();
    	
        model.getExportedVersion().reset();
        try ( ResultSet resultLatestVersion = select("SELECT version, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = ?)", model.getId(), model.getId()) ) {
            // we get the latest model version from the database
	        if ( resultLatestVersion.next() && resultLatestVersion.getObject("version") != null ) {
	            // if the version is found, then the model exists in the database
	            model.getLatestDatabaseVersion().setVersion(resultLatestVersion.getInt("version"));
	            model.getLatestDatabaseVersion().setChecksum(resultLatestVersion.getString("checksum"));
	            model.getLatestDatabaseVersion().setTimestamp(resultLatestVersion.getTimestamp("created_on"));
	            
	            // we check if the model has been imported from (or last exported to) this database
	            if ( !model.getInitialVersion().getTimestamp().equals(DBVersion.NEVER) ) {
		            try ( ResultSet resultCurrentVersion = select("SELECT version, checksum FROM "+this.schema+"models WHERE id = ? AND created_on = ?", model.getId(), model.getInitialVersion().getTimestamp()) ) {
			            if ( resultCurrentVersion.next() && resultCurrentVersion.getObject("version") != null ) {
			                // if the version is found, then the model has been imported from or last exported to the database 
			                model.getInitialVersion().setVersion(resultCurrentVersion.getInt("version"));
			                model.getInitialVersion().setChecksum(resultCurrentVersion.getString("checksum"));
			            }
		            }
	            }
	            
	            model.getExportedVersion().setVersion(model.getLatestDatabaseVersion().getVersion() + 1);

	            logger.debug("The model already exists in the database:");
	            logger.debug("   - initial version = "+model.getInitialVersion().getVersion());
	            logger.debug("   - latest database version = "+model.getLatestDatabaseVersion().getVersion());
	            logger.debug("   - exported version = "+model.getExportedVersion().getVersion());
	            
	            // we reset all the versions
	        	Iterator<Map.Entry<String, IArchimateElement>> ite = model.getAllElements().entrySet().iterator();
	            while (ite.hasNext()) {
	            	DBMetadata metadata = ((IDBMetadata)ite.next().getValue()).getDBMetadata();
	            	metadata.getDatabaseVersion().reset();
	            	metadata.getLatestDatabaseVersion().reset();
	            }

	        	Iterator<Map.Entry<String, IArchimateRelationship>> itr = model.getAllRelationships().entrySet().iterator();
	            while (itr.hasNext()) {
	            	DBMetadata metadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
	            	metadata.getDatabaseVersion().reset();
	            	metadata.getLatestDatabaseVersion().reset();
	            }
	            
	        	Iterator<Map.Entry<String, IFolder>> itf = model.getAllFolders().entrySet().iterator();
	            while (itf.hasNext()) {
	            	DBMetadata metadata = ((IDBMetadata)itf.next().getValue()).getDBMetadata();
	            	metadata.getDatabaseVersion().reset();
	            	metadata.getLatestDatabaseVersion().reset();
	            }
	            
	        	Iterator<Map.Entry<String, IDiagramModel>> itv = model.getAllViews().entrySet().iterator();
	            while (itv.hasNext()) {
	            	DBMetadata metadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
	            	metadata.getDatabaseVersion().reset();
	            	metadata.getLatestDatabaseVersion().reset();
	            }
	            
	            // we get the components versions from the database.
	            
	            // the big requests allow to get in one go all the versions needed of the components :
	            //      - version_in_current_model     contains the version of the component as it in the database model
	            //                                             . as it was when the model was imported, or as it is in the latest database version if the model has been loaded from an archimate file
	            //                                             . or null if it is a new component
	            //      - version_in_latest_model      contains the version of the component as it is in the latest database model
	            //                                             . or null if it has been deleted from the latest database model
	            //      - latest_version               contains the latest version of the component
	            //                                             . whichever the model that modified it
	            // So:
	            //      - if version_in_current_model == null           --> the component exists in the memory but not in the database model
	            //                                                                 --> so this is a new component
	            //      - else if version_in_latest_model == null       --> the component does not exist in the latest version of the model in the database  
	            //                                                                 --> so the component has been removed by another user
	            // And:
	            //      - if version_in_current_model != latest_version --> the component has been updated in the database
                //                                                                  --> so we need to import the component's updates or manage a conflict
	            
	            // elements
	            try ( ResultSet result = select(
	                    "SELECT id,"
                        + "  MAX(version_in_current_model) AS version_in_current_model,"
                        + "  MAX(checksum_in_current_model) AS checksum_in_current_model,"
                        + "  MAX(timestamp_in_current_model) AS timestamp_in_current_model,"
                        + "  MAX(version_in_latest_model) AS version_in_latest_model,"
                        + "  MAX(checksum_in_latest_model) AS checksum_in_latest_model,"
                        + "  MAX(timestamp_in_latest_model) AS timestamp_in_latest_model,"
                        + "  MAX(latest_version) AS latest_version,"
                        + "  MAX(latest_checksum) AS latest_checksum,"
                        + "  MAX(latest_timestamp) AS latest_timestamp "
                        + "FROM ("
                        + "  SELECT e.id AS id,"
                        + "    e.version AS version_in_current_model,"
                        + "    e.checksum AS checksum_in_current_model,"
                        + "    e.created_on AS timestamp_in_current_model,"
                        + "    null AS version_in_latest_model,"
                        + "    null AS checksum_in_latest_model,"
                        + "    null AS timestamp_in_latest_model,"
                        + "    e_max.version AS latest_version,"
                        + "    e_max.checksum AS latest_checksum,"
                        + "    e_max.created_on AS latest_timestamp"
                        + "  FROM elements e"
                        + "  JOIN elements e_max ON e_max.id = e.id AND e_max.version >= e.version"
                        + "  JOIN elements_in_model m ON m.element_id = e.id AND m.element_version = e.version"
                        + "  WHERE m.model_id = ? AND m.model_version = ? "
                        + "UNION"
                        + "  SELECT e.id AS id,"
                        + "    null AS version_in_current_model,"
                        + "    null AS checksum_in_current_model,"
                        + "    null AS timestamp_in_current_model,"
                        + "    e.version AS version_in_latest_model,"
                        + "    e.checksum AS checksum_in_latest_model,"
                        + "    e.created_on AS timestamp_in_latest_model,"
                        + "    e_max.version AS latest_version,"
                        + "    e_max.checksum AS latest_checksum,"
                        + "    e_max.created_on AS latest_timestamp"
                        + "  FROM elements e"
                        + "  JOIN elements e_max ON e_max.id = e.id AND e_max.version >= e.version"
                        + "  JOIN elements_in_model m ON m.element_id = e.id AND m.element_version = e.version"
                        + "  WHERE m.model_id = ? AND m.model_version = (SELECT MAX(version) FROM models WHERE id = ?)"
                        + ") e "
                        + "GROUP BY id"
                        ,model.getId()
                        ,model.getInitialVersion().getVersion()
                        ,model.getId()
                        ,model.getId()
	                    ) ) {
	                while ( result.next() ) {
	                    IDBMetadata element = (IDBMetadata)model.getAllElements().get(result.getString("id"));
	                    if ( element != null ) {
	                        // if the element exists in memory
	                        element.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version_in_current_model"));
	                        element.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum_in_current_model"));
	                        element.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_current_model"));
	                        element.getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version_in_latest_model"));
	                        element.getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum_in_latest_model"));
	                        element.getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_latest_model"));

	                        element.getDBMetadata().getCurrentVersion().setVersion(result.getInt("latest_version"));
	                    } else {
                        	this.elementsNotInModel.put(
                                result.getString("id"),
                                new DBVersionPair(
                                        result.getInt("version_in_current_model"), result.getString("checksum_in_current_model"),result.getTimestamp("timestamp_in_current_model"),
                                        result.getInt("version_in_latest_model"), result.getString("checksum_in_latest_model"),result.getTimestamp("timestamp_in_latest_model")
                                        )
                                );
	                    }
	                }
	            }
	
	            // relationships
                try ( ResultSet result = select(
                            "SELECT id,"
                            + "  MAX(version_in_current_model) AS version_in_current_model,"
                            + "  MAX(checksum_in_current_model) AS checksum_in_current_model,"
                            + "  MAX(timestamp_in_current_model) AS timestamp_in_current_model,"
                            + "  MAX(version_in_latest_model) AS version_in_latest_model,"
                            + "  MAX(checksum_in_latest_model) AS checksum_in_latest_model,"
                            + "  MAX(timestamp_in_latest_model) AS timestamp_in_latest_model,"
                            + "  MAX(latest_version) AS latest_version,"
                            + "  MAX(latest_checksum) AS latest_checksum,"
                            + "  MAX(latest_timestamp) AS latest_timestamp "
                            + "FROM ("
                            + "  SELECT e.id AS id,"
                            + "    e.version AS version_in_current_model,"
                            + "    e.checksum AS checksum_in_current_model,"
                            + "    e.created_on AS timestamp_in_current_model,"
                            + "    null AS version_in_latest_model,"
                            + "    null AS checksum_in_latest_model,"
                            + "    null AS timestamp_in_latest_model,"
                            + "    e_max.version AS latest_version,"
                            + "    e_max.checksum AS latest_checksum,"
                            + "    e_max.created_on AS latest_timestamp"
                            + "  FROM relationships e"
                            + "  JOIN relationships e_max ON e_max.id = e.id AND e_max.version >= e.version"
                            + "  JOIN relationships_in_model m ON m.relationship_id = e.id AND m.relationship_version = e.version"
                            + "  WHERE m.model_id = ? AND m.model_version = ? "
                            + "UNION"
                            + "  SELECT e.id AS id,"
                            + "    null AS version_in_current_model,"
                            + "    null AS checksum_in_current_model,"
                            + "    null AS timestamp_in_current_model,"
                            + "    e.version AS version_in_latest_model,"
                            + "    e.checksum AS checksum_in_latest_model,"
                            + "    e.created_on AS timestamp_in_latest_model,"
                            + "    e_max.version AS latest_version,"
                            + "    e_max.checksum AS latest_checksum,"
                            + "    e_max.created_on AS latest_timestamp"
                            + "  FROM relationships e"
                            + "  JOIN relationships e_max ON e_max.id = e.id AND e_max.version >= e.version"
                            + "  JOIN relationships_in_model m ON m.relationship_id = e.id AND m.relationship_version = e.version"
                            + "  WHERE m.model_id = ? AND m.model_version = (SELECT MAX(version) FROM models WHERE id = ?)"
                            + ") e "
                            + "GROUP BY id"
                            ,model.getId()
                            ,model.getInitialVersion().getVersion()
                            ,model.getId()
                            ,model.getId()
                            ) ) {
                    while ( result.next() ) {
                        IDBMetadata relationship = (IDBMetadata)model.getAllRelationships().get(result.getString("id"));
                        if ( relationship != null ) {
                            // if the relationship exists in memory
                            relationship.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version_in_current_model"));
                            relationship.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum_in_current_model"));
                            relationship.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_current_model"));
                            relationship.getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version_in_latest_model"));
                            relationship.getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum_in_latest_model"));
                            relationship.getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_latest_model"));

                            relationship.getDBMetadata().getCurrentVersion().setVersion(result.getInt("latest_version"));
                        } else {
                            this.relationshipsNotInModel.put(
                                    result.getString("id"),
                                    new DBVersionPair(
                                            result.getInt("version_in_current_model"), result.getString("checksum_in_current_model"),result.getTimestamp("timestamp_in_current_model"),
                                            result.getInt("version_in_latest_model"), result.getString("checksum_in_latest_model"),result.getTimestamp("timestamp_in_latest_model")
                                            )
                                    );
                        }
                    }
                }
                
                // folders
                try ( ResultSet result = select(
                          "SELECT id,"
                          + "  MAX(version_in_current_model) AS version_in_current_model,"
                          + "  MAX(checksum_in_current_model) AS checksum_in_current_model,"
                          + "  MAX(timestamp_in_current_model) AS timestamp_in_current_model,"
                          + "  MAX(version_in_latest_model) AS version_in_latest_model,"
                          + "  MAX(checksum_in_latest_model) AS checksum_in_latest_model,"
                          + "  MAX(timestamp_in_latest_model) AS timestamp_in_latest_model,"
                          + "  MAX(latest_version) AS latest_version,"
                          + "  MAX(latest_checksum) AS latest_checksum,"
                          + "  MAX(latest_timestamp) AS latest_timestamp,"
                          + "  rank "
                          + "FROM ("
                          + "  SELECT e.id AS id,"
                          + "    e.version AS version_in_current_model,"
                          + "    e.checksum AS checksum_in_current_model,"
                          + "    e.created_on AS timestamp_in_current_model,"
                          + "    null AS version_in_latest_model,"
                          + "    null AS checksum_in_latest_model,"
                          + "    null AS timestamp_in_latest_model,"
                          + "    e_max.version AS latest_version,"
                          + "    e_max.checksum AS latest_checksum,"
                          + "    e_max.created_on AS latest_timestamp,"
                          + "    rank"
                          + "  FROM folders e"
                          + "  JOIN folders e_max ON e_max.id = e.id AND e_max.version >= e.version"
                          + "  JOIN folders_in_model m ON m.folder_id = e.id AND m.folder_version = e.version"
                          + "  WHERE m.model_id = ? AND m.model_version = ? "
                          + "UNION"
                          + "  SELECT e.id AS id,"
                          + "    null AS version_in_current_model,"
                          + "    null AS checksum_in_current_model,"
                          + "    null AS timestamp_in_current_model,"
                          + "    e.version AS version_in_latest_model,"
                          + "    e.checksum AS checksum_in_latest_model,"
                          + "    e.created_on AS timestamp_in_latest_model,"
                          + "    e_max.version AS latest_version,"
                          + "    e_max.checksum AS latest_checksum,"
                          + "    e_max.created_on AS latest_timestamp,"
                          + "    rank"
                          + "  FROM folders e"
                          + "  JOIN folders e_max ON e_max.id = e.id AND e_max.version >= e.version"
                          + "  JOIN folders_in_model m ON m.folder_id = e.id AND m.folder_version = e.version"
                          + "  WHERE m.model_id = ? AND m.model_version = (SELECT MAX(version) FROM models WHERE id = ?)"
                          + ") e "
                          + "GROUP BY id "
                          + "ORDER BY rank"
                          ,model.getId()
                          ,model.getInitialVersion().getVersion()
                          ,model.getId()
                          ,model.getId()
                          ) ) {
                    while ( result.next() ) {
                        IDBMetadata folder = (IDBMetadata)model.getAllFolders().get(result.getString("id"));
                        if ( folder != null ) {
                            // if the folder exists in memory
                            folder.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version_in_current_model"));
                            folder.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum_in_current_model"));
                            folder.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_current_model"));
                            folder.getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version_in_latest_model"));
                            folder.getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum_in_latest_model"));
                            folder.getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_latest_model"));

                            folder.getDBMetadata().getCurrentVersion().setVersion(result.getInt("latest_version"));
                        } else {
                            this.foldersNotInModel.put(
                                    result.getString("id"),
                                    new DBVersionPair(
                                            result.getInt("version_in_current_model"), result.getString("checksum_in_current_model"),result.getTimestamp("timestamp_in_current_model"),
                                            result.getInt("version_in_latest_model"), result.getString("checksum_in_latest_model"),result.getTimestamp("timestamp_in_latest_model")
                                            )
                                    );
                        }
                    }
                }
                
                // views
                try ( ResultSet result = select(
                        "SELECT id,"
                                + "  MAX(version_in_current_model) AS version_in_current_model,"
                                + "  MAX(checksum_in_current_model) AS checksum_in_current_model,"
                                + "  MAX(container_checksum_in_current_model) AS container_checksum_in_current_model,"
                                + "  MAX(timestamp_in_current_model) AS timestamp_in_current_model,"
                                + "  MAX(version_in_latest_model) AS version_in_latest_model,"
                                + "  MAX(checksum_in_latest_model) AS checksum_in_latest_model,"
                                + "  MAX(container_checksum_in_latest_model) AS container_checksum_in_latest_model,"
                                + "  MAX(timestamp_in_latest_model) AS timestamp_in_latest_model,"
                                + "  MAX(latest_version) AS latest_version,"
                                + "  MAX(latest_checksum) AS latest_checksum,"
                                + "  MAX(latest_container_checksum) AS latest_container_checksum,"
                                + "  MAX(latest_timestamp) AS latest_timestamp "
                                + "FROM ("
                                + "  SELECT e.id AS id,"
                                + "    e.version AS version_in_current_model,"
                                + "    e.checksum AS checksum_in_current_model,"
                                + "    e.container_checksum AS container_checksum_in_current_model,"
                                + "    e.created_on AS timestamp_in_current_model,"
                                + "    null AS version_in_latest_model,"
                                + "    null AS checksum_in_latest_model,"
                                + "    null AS container_checksum_in_latest_model,"
                                + "    null AS timestamp_in_latest_model,"
                                + "    e_max.version AS latest_version,"
                                + "    e_max.checksum AS latest_checksum,"
                                + "    e_max.container_checksum AS latest_container_checksum,"
                                + "    e_max.created_on AS latest_timestamp"
                                + "  FROM views e"
                                + "  JOIN views e_max ON e_max.id = e.id AND e_max.version >= e.version"
                                + "  JOIN views_in_model m ON m.view_id = e.id AND m.view_version = e.version"
                                + "  WHERE m.model_id = ? AND m.model_version = ? "
                                + "UNION"
                                + "  SELECT e.id AS id,"
                                + "    null AS version_in_current_model,"
                                + "    null AS checksum_in_current_model,"
                                + "    null AS container_checksum_in_current_model,"
                                + "    null AS timestamp_in_current_model,"
                                + "    e.version AS version_in_latest_model,"
                                + "    e.checksum AS checksum_in_latest_model,"
                                + "    e.container_checksum AS container_checksum_in_latest_model,"
                                + "    e.created_on AS timestamp_in_latest_model,"
                                + "    e_max.version AS latest_version,"
                                + "    e_max.checksum AS latest_checksum,"
                                + "    e_max.container_checksum AS latest_container_checksum,"
                                + "    e_max.created_on AS latest_timestamp"
                                + "  FROM views e"
                                + "  JOIN views e_max ON e_max.id = e.id AND e_max.version >= e.version"
                                + "  JOIN views_in_model m ON m.view_id = e.id AND m.view_version = e.version"
                                + "  WHERE m.model_id = ? AND m.model_version = (SELECT MAX(version) FROM models WHERE id = ?)"
                                + ") e "
                                + "GROUP BY id"
                                ,model.getId()
                                ,model.getInitialVersion().getVersion()
                                ,model.getId()
                                ,model.getId()
                        ) ) {
                    while ( result.next() ) {
                        IDiagramModel view = model.getAllViews().get(result.getString("id"));
                        IDBMetadata viewMetadata = (IDBMetadata)view;
                        if ( view != null ) {
                            // if the view exists in memory
                            viewMetadata.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version_in_current_model"));
                            viewMetadata.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum_in_current_model"));
                            viewMetadata.getDBMetadata().getDatabaseVersion().setContainerChecksum(result.getString("container_checksum_in_current_model"));
                            viewMetadata.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_current_model"));
                            viewMetadata.getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version_in_latest_model"));
                            viewMetadata.getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum_in_latest_model"));
                            viewMetadata.getDBMetadata().getLatestDatabaseVersion().setContainerChecksum(result.getString("container_checksum_in_latest_model"));
                            viewMetadata.getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_latest_model"));

                            viewMetadata.getDBMetadata().getCurrentVersion().setVersion(result.getInt("latest_version"));
                            
                            getViewObjectsAndConnectionsVersionsFromDatabase(model, view);
                        } else {
                            this.viewsNotInModel.put(
                                    result.getString("id"),
                                    new DBVersionPair(
                                            result.getInt("version_in_current_model"), result.getString("checksum_in_current_model"),result.getTimestamp("timestamp_in_current_model"),
                                            result.getInt("version_in_latest_model"), result.getString("checksum_in_latest_model"),result.getTimestamp("timestamp_in_latest_model")
                                            )
                                    );
                        }
                    }
                }
		        
				// then we check if the latest version of the model has got images that are not in the model
		        try ( ResultSet result = select ("SELECT DISTINCT image_path FROM views_objects "
		        		+ "JOIN views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version "
						+ "JOIN views_in_model ON views_in_model.view_id = views_objects_in_view.view_id AND views_in_model.view_version = views_objects_in_view.view_version "
						+ "WHERE image_path IS NOT NULL AND views_in_model.model_id = ? AND views_in_model.model_version = ?"
						,model.getId()
						,model.getLatestDatabaseVersion().getVersion()
						) ) {
					while ( result.next() ) {
						if ( !model.getAllImagePaths().contains(result.getString("image_path")) ) {
							this.imagesNotInModel.put(result.getString("image_path"), new DBVersionPair());
						}
					}
		        }
		    } else {
	            logger.debug("The model does not (yet) exist in the database");
	            
	            model.getExportedVersion().setVersion(1);
	        
	            // the model does not exist yet, but the components may exist from other models. So we get their latest version
	        	Iterator<Map.Entry<String, IArchimateElement>> ite = model.getAllElements().entrySet().iterator();
	            while (ite.hasNext()) {
	            	IArchimateElement element = ite.next().getValue();
	            	try ( ResultSet result = select("SELECT version, checksum, created_on FROM "+this.schema+"elements WHERE id = ? AND version = (SELECT MAX(version) FROM elements WHERE id = ?)", element.getId(), element.getId()) ) {
		            	if ( result.next() ) {
		            	    // if the component does exist in the database
			            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
			            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
			            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
			            	((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version"));
			            	((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
			            	((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
			            	
			            	((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
		            	}
		            	 else {
		            	  // if the component does not exist in the database
		                     ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(0);
		            	 }
	            	}
	            }
	            
	        	Iterator<Map.Entry<String, IArchimateRelationship>> itr = model.getAllRelationships().entrySet().iterator();
	            while (itr.hasNext()) {
	            	IArchimateRelationship relationship = itr.next().getValue();
	            	try ( ResultSet result = select("SELECT version, checksum, created_on FROM "+this.schema+"relationships WHERE id = ? AND version = (SELECT MAX(version) FROM relationships WHERE id = ?)",relationship.getId(), relationship.getId()) ) {
		            	if ( result.next() ) {
			            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
			            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
			            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
			            	((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version"));
			            	((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
			            	((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
			            	
			            	((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
		            	} else
		                    ((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(0);
	            	}
	        	}
	            
	        	Iterator<Map.Entry<String, IFolder>> itf = model.getAllFolders().entrySet().iterator();
	            while (itf.hasNext()) {
	            	IFolder folder = itf.next().getValue();
            		try ( ResultSet result = select("SELECT version, checksum, created_on FROM "+this.schema+"folders WHERE id = ? AND version = (SELECT MAX(version) FROM folders WHERE id = ?)", folder.getId(), folder.getId()) ) {
	                	if ( result.next() ) {
	    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
	    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
	    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	    	            	((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version"));
	    	            	((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
	    	            	((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	    	            	
	    	            	((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
	                	} else
	                	    ((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(0);
            		}
	            }
	        	Iterator<Map.Entry<String, IDiagramModel>> itv = model.getAllViews().entrySet().iterator();
                while (itv.hasNext()) {
                	IDiagramModel view = itv.next().getValue();
                	try ( ResultSet result = select("SELECT version, checksum, created_on FROM "+this.schema+"views WHERE id = ? AND version = (SELECT MAX(version) FROM views WHERE id = ?)", view.getId(), view.getId()) ) {
	                	if ( result.next() ) {
	    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
	    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
	    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	    	            	((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version"));
	    	            	((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
	    	            	((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	    	            	
	    	            	((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
	                	} else
	                        ((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(0);
                	}
                }
	        }
        }
        
        // even if the model does not exist in the database, the images can exist in the database
        // images do not have a version as they cannot be modified. Their path is a checksum and loading a new image creates a new path.
        
        // at last, we check if all the images in the model are in the database
    	for ( String path: model.getAllImagePaths() ) {
    		try ( ResultSet result = select("SELECT path from images where path = ?", path) ) {
				if ( result.next() && result.getObject("path") != null ) {
					// the image is in the database
				} else {
					// the image is not in the database
					this.imagesNotInDatabase.put(path, new DBVersionPair());
				}
    		}
		}
    }

    public void getViewObjectsAndConnectionsVersionsFromDatabase(DBArchimateModel model, IDiagramModel view) throws SQLException, RuntimeException {
    	// view connections
    	try ( ResultSet result = select(
    			"SELECT id,"
    					+ "  MAX(version_in_current_model) AS version_in_current_model,"
    					+ "  MAX(checksum_in_current_model) AS checksum_in_current_model,"
    					+ "  MAX(timestamp_in_current_model) AS timestamp_in_current_model,"
    					+ "  MAX(version_in_latest_model) AS version_in_latest_model,"
    					+ "  MAX(checksum_in_latest_model) AS checksum_in_latest_model,"
    					+ "  MAX(timestamp_in_latest_model) AS timestamp_in_latest_model,"
    					+ "  MAX(latest_version) AS latest_version,"
    					+ "  MAX(latest_checksum) AS latest_checksum,"
    					+ "  MAX(latest_timestamp) AS latest_timestamp,"
    					+ "  rank "
    					+ "FROM ("
    					+ "  SELECT e.id AS id,"
    					+ "    e.version AS version_in_current_model,"
    					+ "    e.checksum AS checksum_in_current_model,"
    					+ "    e.created_on AS timestamp_in_current_model,"
    					+ "    null AS version_in_latest_model,"
    					+ "    null AS checksum_in_latest_model,"
    					+ "    null AS timestamp_in_latest_model,"
    					+ "    e_max.version AS latest_version,"
    					+ "    e_max.checksum AS latest_checksum,"
    					+ "    e_max.created_on AS latest_timestamp,"
    					+ "    rank"
    					+ "  FROM views_connections e"
    					+ "  JOIN views_connections e_max ON e_max.id = e.id AND e_max.version >= e.version"
    					+ "  JOIN views_connections_in_view v ON v.connection_id = e.id AND v.connection_version = e.version"
    					+ "  WHERE v.view_id = ? AND v.view_version = ? "
    					+ "UNION"
    					+ "  SELECT e.id AS id,"
    					+ "    null AS version_in_current_model,"
    					+ "    null AS checksum_in_current_model,"
    					+ "    null AS timestamp_in_current_model,"
    					+ "    e.version AS version_in_latest_model,"
    					+ "    e.checksum AS checksum_in_latest_model,"
    					+ "    e.created_on AS timestamp_in_latest_model,"
    					+ "    e_max.version AS latest_version,"
    					+ "    e_max.checksum AS latest_checksum,"
    					+ "    e_max.created_on AS latest_timestamp,"
    					+ "    rank"
    					+ "  FROM views_connections e"
    					+ "  JOIN views_connections e_max ON e_max.id = e.id AND e_max.version >= e.version"
    					+ "  JOIN views_connections_in_view v ON v.connection_id = e.id AND v.connection_version = e.version"
    					+ "  WHERE v.view_id = ? AND v.view_version = (SELECT MAX(version) FROM views WHERE id = ?)"
    					+ ") e "
    					+ "GROUP BY id "
    					+ "ORDER BY rank"
    					,view.getId()
    					,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
    					,view.getId()
    					,view.getId()
    			) ) {
    		while ( result.next() ) {
    			IDiagramModelComponent viewConnection = model.getAllViewConnections().get(result.getString("id"));
    			IDBMetadata viewConnectionMetadata = (IDBMetadata)viewConnection;
    			if ( viewConnection != null ) {
    				// if the view exists in memory
    				viewConnectionMetadata.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version_in_current_model"));
    				viewConnectionMetadata.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum_in_current_model"));
    				viewConnectionMetadata.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_current_model"));
    				viewConnectionMetadata.getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version_in_latest_model"));
    				viewConnectionMetadata.getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum_in_latest_model"));
    				viewConnectionMetadata.getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_latest_model"));

    				viewConnectionMetadata.getDBMetadata().getCurrentVersion().setVersion(result.getInt("latest_version"));
    			} else {
    				this.viewConnectionsNotInModel.put(
    						result.getString("id"),
    						new DBVersionPair(
    								result.getInt("version_in_current_model"), result.getString("checksum_in_current_model"),result.getTimestamp("timestamp_in_current_model"),
    								result.getInt("version_in_latest_model"), result.getString("checksum_in_latest_model"),result.getTimestamp("timestamp_in_latest_model")
    								)
    						);
    			}
    		}
    	}

    	// view objects
    	try ( ResultSet result = select(
    			"SELECT id,"
    					+ "  MAX(version_in_current_model) AS version_in_current_model,"
    					+ "  MAX(checksum_in_current_model) AS checksum_in_current_model,"
    					+ "  MAX(timestamp_in_current_model) AS timestamp_in_current_model,"
    					+ "  MAX(version_in_latest_model) AS version_in_latest_model,"
    					+ "  MAX(checksum_in_latest_model) AS checksum_in_latest_model,"
    					+ "  MAX(timestamp_in_latest_model) AS timestamp_in_latest_model,"
    					+ "  MAX(latest_version) AS latest_version,"
    					+ "  MAX(latest_checksum) AS latest_checksum,"
    					+ "  MAX(latest_timestamp) AS latest_timestamp,"
    					+ "  rank "
    					+ "FROM ("
    					+ "  SELECT e.id AS id,"
    					+ "    e.version AS version_in_current_model,"
    					+ "    e.checksum AS checksum_in_current_model,"
    					+ "    e.created_on AS timestamp_in_current_model,"
    					+ "    null AS version_in_latest_model,"
    					+ "    null AS checksum_in_latest_model,"
    					+ "    null AS timestamp_in_latest_model,"
    					+ "    e_max.version AS latest_version,"
    					+ "    e_max.checksum AS latest_checksum,"
    					+ "    e_max.created_on AS latest_timestamp,"
    					+ "    rank"
    					+ "  FROM views_objects e"
    					+ "  JOIN views_objects e_max ON e_max.id = e.id AND e_max.version >= e.version"
    					+ "  JOIN views_objects_in_view v ON v.object_id = e.id AND v.object_version = e.version"
    					+ "  WHERE v.view_id = ? AND v.view_version = ? "
    					+ "UNION"
    					+ "  SELECT e.id AS id,"
    					+ "    null AS version_in_current_model,"
    					+ "    null AS checksum_in_current_model,"
    					+ "    null AS timestamp_in_current_model,"
    					+ "    e.version AS version_in_latest_model,"
    					+ "    e.checksum AS checksum_in_latest_model,"
    					+ "    e.created_on AS timestamp_in_latest_model,"
    					+ "    e_max.version AS latest_version,"
    					+ "    e_max.checksum AS latest_checksum,"
    					+ "    e_max.created_on AS latest_timestamp,"
    					+ "    rank"
    					+ "  FROM views_objects e"
    					+ "  JOIN views_objects e_max ON e_max.id = e.id AND e_max.version >= e.version"
    					+ "  JOIN views_objects_in_view v ON v.object_id = e.id AND v.object_version = e.version"
    					+ "  WHERE v.view_id = ? AND v.view_version = (SELECT MAX(version) FROM views WHERE id = ?)"
    					+ ") e "
    					+ "GROUP BY id "
    					+ "ORDER BY rank"
    					,view.getId()
    					,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
    					,view.getId()
    					,view.getId()
    			) ) {
    		while ( result.next() ) {
    			IDiagramModelComponent viewObject = model.getAllViewObjects().get(result.getString("id"));
    			IDBMetadata viewObjectMetadata = (IDBMetadata)viewObject;
    			if ( viewObject != null ) {
    				// if the view exists in memory
    				viewObjectMetadata.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version_in_current_model"));
    				viewObjectMetadata.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum_in_current_model"));
    				viewObjectMetadata.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_current_model"));
    				viewObjectMetadata.getDBMetadata().getLatestDatabaseVersion().setVersion(result.getInt("version_in_latest_model"));
    				viewObjectMetadata.getDBMetadata().getLatestDatabaseVersion().setChecksum(result.getString("checksum_in_latest_model"));
    				viewObjectMetadata.getDBMetadata().getLatestDatabaseVersion().setTimestamp(result.getTimestamp("timestamp_in_latest_model"));

    				viewObjectMetadata.getDBMetadata().getCurrentVersion().setVersion(result.getInt("latest_version"));
    			} else {
    				this.viewObjectsNotInModel.put(
    						result.getString("id"),
    						new DBVersionPair(
    								result.getInt("version_in_current_model"), result.getString("checksum_in_current_model"),result.getTimestamp("timestamp_in_current_model"),
    								result.getInt("version_in_latest_model"), result.getString("checksum_in_latest_model"),result.getTimestamp("timestamp_in_latest_model")
    								)
    						);
    			}
    		}
    	}
    }
    
	/**
	 * Empty a Neo4J database
	 */
	public void emptyNeo4jDB() throws Exception {
		request("MATCH (n) DETACH DELETE n");
	}
    
    
	/**
	 * Exports the model metadata into the database
	 */
	public void exportModel(DBArchimateModel model, String releaseNote) throws Exception {
		final String[] modelsColumns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "checksum"};
		
		if ( (model.getName() == null) || (model.getName().equals("")) )
			throw new RuntimeException("Model name cannot be empty.");
		
        if ( logger.isDebugEnabled() ) logger.debug("Exporting model (initial version = "+model.getInitialVersion().getVersion()+", exported version = "+model.getExportedVersion().getVersion()+", latest database version = "+model.getLatestDatabaseVersion().getVersion()+")");
		
        if ( this.connection.getAutoCommit() )
            model.getExportedVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
        else
            model.getExportedVersion().setTimestamp(this.lastTransactionTimestamp);
        
        insert(this.schema+"models", modelsColumns
				,model.getId()
				,model.getExportedVersion().getVersion()
				,model.getName()
				,releaseNote
				,model.getPurpose()
				,DBPlugin.getUserName()
				,model.getExportedVersion().getTimestamp()
				,model.getExportedVersion().getChecksum()
				);

		exportProperties(model);
		exportMetadata(model);
	}

	/**
	 * Export a component to the database
	 */
	public void exportEObject(EObject eObject, DBGui gui) throws Exception {
		if ( eObject instanceof IArchimateElement ) 			exportElement((IArchimateElement)eObject);
		else if ( eObject instanceof IArchimateRelationship ) 	exportRelationship((IArchimateRelationship)eObject);
		else if ( eObject instanceof IFolder ) 					exportFolder((IFolder)eObject);
		else if ( eObject instanceof IDiagramModel ) 			exportView((IDiagramModel)eObject, gui);
		else if ( eObject instanceof IDiagramModelObject )		exportViewObject((IDiagramModelComponent)eObject);
		else if ( eObject instanceof IDiagramModelConnection )	exportViewConnection((IDiagramModelConnection)eObject);
		else
			throw new Exception("Do not know how to export "+eObject.getClass().getSimpleName());
	}
	
	public void assignEObjectToModel(EObject eObject) throws Exception {
		if ( logger.isTraceEnabled() ) logger.trace("   assigning component to model");
		
		if ( eObject instanceof IArchimateElement )				assignElementToModel((IArchimateElement)eObject);
		else if ( eObject instanceof IArchimateRelationship )	assignRelationshipToModel((IArchimateRelationship)eObject);
		else if ( eObject instanceof IFolder )					assignFolderToModel((IFolder)eObject);
		else if ( eObject instanceof IDiagramModel )			assignViewToModel((IDiagramModel)eObject);
		else if ( eObject instanceof IDiagramModelObject )		assignViewObjectToView((IDiagramModelObject)eObject);
		else if ( eObject instanceof IDiagramModelConnection )	assignViewConnectionToView((IDiagramModelConnection)eObject);
		else
			throw new Exception("Do not know how to assign to the model : "+eObject.getClass().getSimpleName());
	}

	/**
	 * Export an element to the database
	 */
	private void exportElement(IArchimateConcept element) throws Exception {
		final String[] elementsColumns = {"id", "version", "class", "name", "type", "documentation", "created_by", "created_on", "checksum"};
		
		// if the element is exported, the we increase its exportedVersion
		((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion() + 1);
		
        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)element).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)element).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)element).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "neo4j") ) {
			// TODO : USE MERGE instead to replace existing nodes
			request("CREATE (new:elements {id:?, version:?, class:?, name:?, type:?, documentation:?, checksum:?})"
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
					,DBPlugin.getUserName()
					,((DBArchimateModel)element.getArchimateModel()).getExportedVersion().getTimestamp()
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

		insert(this.schema+"elements_in_model", elementsInModelColumns
				,element.getId()
				,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportElement()
				,((IFolder)element.eContainer()).getId()
				,model.getId()
				,model.getExportedVersion().getVersion()
				,++this.elementRank
				);
	}

	/**
	 * Export a relationship to the database
	 */
	private void exportRelationship(IArchimateConcept relationship) throws Exception {
		final String[] relationshipsColumns = {"id", "version", "class", "name", "documentation", "source_id", "target_id", "strength", "access_type", "created_by", "created_on", "checksum"};
		
	    // if the relationship is exported, the we increase its exportedVersion
        ((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion() + 1);
		
        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)relationship).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)relationship).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "neo4j") ) {
			String relationshipType = (this.databaseEntry.isNeo4jTypedRelationship() ? (relationship.getClass().getSimpleName()+"s") : "relationships");
			// TODO : USE MERGE instead to replace existing nodes
			if ( this.databaseEntry.isNeo4jNativeMode() ) {
				if ( (((IArchimateRelationship)relationship).getSource() instanceof IArchimateElement) && (((IArchimateRelationship)relationship).getTarget() instanceof IArchimateElement) ) {
					request("MATCH (source:elements {id:?, version:?}), (target:elements {id:?, version:?}) CREATE (source)-[relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}]->(target)"
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
				request("MATCH (source {id:?, version:?}), (target {id:?, version:?}) CREATE (relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}), (source)-[rel1:relatedTo]->(relationship)-[rel2:relatedTo]->(target)"
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
					,DBPlugin.getUserName()
					,((DBArchimateModel)relationship.getArchimateModel()).getExportedVersion().getTimestamp()
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

		insert(this.schema+"relationships_in_model", relationshipsInModelColumns
				,relationship.getId()
				,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
				,((IFolder)relationship.eContainer()).getId()
				,model.getId()
				,model.getExportedVersion().getVersion()
				,++this.relationshipRank
				);
	}

	/**
	 * Export a folder into the database.
	 */
	private void exportFolder(IFolder folder) throws Exception {
		final String[] foldersColumns = {"id", "version", "type", "root_type", "name", "documentation", "created_by", "created_on", "checksum"};
		
		// if the folder is exported, the we increase its exportedVersion
        ((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion() + 1);
		
        if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)folder).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)folder).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");
        
		insert(this.schema+"folders", foldersColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
				,folder.getType().getValue()
				,((IDBMetadata)folder).getDBMetadata().getRootFolderType()
				,folder.getName()
				,folder.getDocumentation()
				,DBPlugin.getUserName()
				,((DBArchimateModel)folder.getArchimateModel()).getExportedVersion().getTimestamp()
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

		insert(this.schema+"folders_in_model", foldersInModelColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
				,(((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId())
				,model.getId()
				,model.getExportedVersion().getVersion()
				,++this.folderRank
				);
	}

	/**
	 * Export a view into the database.
	 */
	private void exportView(IDiagramModel view, DBGui gui) throws Exception {
		final String[] ViewsColumns = {"id", "version", "class", "created_by", "created_on", "name", "connection_router_type", "documentation", "hint_content", "hint_title", "viewpoint", "background", "screenshot", "checksum", "container_checksum"};
		
		// if the view is exported, the we increase its exportedVersion
        ((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion() + 1);
		
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)view).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)view).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)view).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		byte[] viewImage = null;

		if ( this.databaseEntry.isViewSnapshotRequired() )
			viewImage = gui.createImage(view, this.databaseEntry.getViewsImagesScaleFactor()/100.0, this.databaseEntry.getViewsImagesBorderWidth());

		insert(this.schema+"views", ViewsColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
				,view.getClass().getSimpleName()
				,DBPlugin.getUserName()
				,((DBArchimateModel)view.getArchimateModel()).getExportedVersion().getTimestamp()
				,view.getName()
				,view.getConnectionRouterType()
				,view.getDocumentation()
				,((view instanceof IHintProvider) ? ((IHintProvider)view).getHintContent() : null)
				,((view instanceof IHintProvider) ? ((IHintProvider)view).getHintTitle() : null)
				,((view instanceof IArchimateDiagramModel) ? ((IArchimateDiagramModel)view).getViewpoint() : null)
				,((view instanceof ISketchModel) ? ((ISketchModel)view).getBackground() : null)
				,viewImage
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getChecksum()
                ,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getContainerChecksum()
				);
		
		viewImage = null;		// to force memory release

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

		insert(this.schema+"views_in_model", viewsInModelColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
				,((IFolder)view.eContainer()).getId()
				,model.getId()
				,model.getExportedVersion().getVersion()
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
		final String[] ViewsObjectsColumns = {"id", "version", "class", "container_id", "element_id", "diagram_ref_id", "type", "border_color", "border_type", "content", "documentation", "hint_content", "hint_title", "is_locked", "image_path", "image_position", "line_color", "line_width", "fill_color", "font", "font_color", "name", "notes", "source_connections", "target_connections", "text_alignment", "text_position", "x", "y", "width", "height", "created_by", "created_on", "checksum"};
		
	      // if the viewObject is exported, the we increase its exportedVersion
        ((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion() + 1);

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
				,((viewObject instanceof IHintProvider) ? ((IHintProvider)viewObject).getHintContent() : null)
				,((viewObject instanceof IHintProvider) ? ((IHintProvider)viewObject).getHintTitle() : null)
				//TODO : add helpHintcontent and helpHintTitle
				,((viewObject instanceof ILockable) ? (((ILockable)viewObject).isLocked()?1:0) : null)
				,((viewObject instanceof IDiagramModelImageProvider) ? ((IDiagramModelImageProvider)viewObject).getImagePath() : null)
				,((viewObject instanceof IIconic) ? ((IIconic)viewObject).getImagePosition() : null)
				,((viewObject instanceof ILineObject) ? ((ILineObject)viewObject).getLineColor() : null)
				,((viewObject instanceof ILineObject) ? ((ILineObject)viewObject).getLineWidth() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getFillColor() : null)
				,((viewObject instanceof IFontAttribute) ? ((IFontAttribute)viewObject).getFont() : null)
				,((viewObject instanceof IFontAttribute) ? ((IFontAttribute)viewObject).getFontColor() : null)
				,viewObject.getName()																						// we export the name because it will be used in case of conflict
				,((viewObject instanceof ICanvasModelSticky) ? ((ICanvasModelSticky)viewObject).getNotes() : null)
				,((viewObject instanceof IConnectable) ? encode(((IConnectable)viewObject).getSourceConnections()) : null)
				,((viewObject instanceof IConnectable) ? encode(((IConnectable)viewObject).getTargetConnections()) : null)
				,((viewObject instanceof ITextAlignment) ? ((ITextAlignment)viewObject).getTextAlignment() : null)
				,((viewObject instanceof ITextPosition) ? ((ITextPosition)viewObject).getTextPosition() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getX() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getY() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getWidth() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getHeight() : null)
				,DBPlugin.getUserName()
				,((DBArchimateModel)viewObject.getDiagramModel().getArchimateModel()).getExportedVersion().getTimestamp()
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

		EObject viewContainer = viewObject.eContainer();
		while ( !(viewContainer instanceof IDiagramModel) ) {
			viewContainer = viewContainer.eContainer();
		}

		insert(this.schema+"views_objects_in_view", viewObjectInViewColumns
				,((IIdentifier)viewObject).getId()
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()
				,((IIdentifier)viewContainer).getId()
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
		final String[] ViewsConnectionsColumns = {"id", "version", "class", "container_id", "name", "documentation", "is_locked", "line_color", "line_width", "font", "font_color", "relationship_id", "source_connections", "target_connections", "source_object_id", "target_object_id", "text_position", "type", "created_by", "created_on", "checksum"};
		final String[] bendpointsColumns = {"parent_id", "parent_version", "rank", "start_x", "start_y", "end_x", "end_y"};

	    // if the viewConnection is exported, the we increase its exportedVersion
        ((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion() + 1);

		EObject viewContainer = viewConnection.eContainer();
		while ( !(viewContainer instanceof IDiagramModel) ) {
			viewContainer = viewContainer.eContainer();
		}

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
				,encode(viewConnection.getSourceConnections())
				,encode(viewConnection.getTargetConnections())
				,viewConnection.getSource().getId()
				,viewConnection.getTarget().getId()
				,viewConnection.getTextPosition()
				,((viewConnection instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)viewConnection).getType() : viewConnection.getType())
				,DBPlugin.getUserName()
				,((DBArchimateModel)viewConnection.getDiagramModel().getArchimateModel()).getExportedVersion().getTimestamp()
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

		EObject viewContainer = viewConnection.eContainer();
		while ( !(viewContainer instanceof IDiagramModel) ) {
			viewContainer = viewContainer.eContainer();
		}

		insert(this.schema+"views_connections_in_view", viewObjectInViewColumns
				,((IIdentifier)viewConnection).getId()
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
				,((IIdentifier)viewContainer).getId()
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
			exportedVersion = ((DBArchimateModel)parent).getExportedVersion().getVersion();
		} else 
			exportedVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

		for ( int propRank = 0 ; propRank < parent.getProperties().size(); ++propRank) {
			IProperty prop = parent.getProperties().get(propRank);
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "neo4j") ) {
				request("MATCH (parent {id:?, version:?}) CREATE (prop:property {rank:?, name:?, value:?}), (parent)-[:hasProperty]->(prop)"
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
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "neo4j") ) {
				request("MATCH (parent {id:?, version:?}) CREATE (prop:metadata {rank:?, name:?, value:?}), (parent)-[:hasMetadata]->(prop)"
						,parent.getId()
						,parent.getExportedVersion().getVersion()
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
			}
			else
				insert(this.schema+"metadata", metadataColumns
						,parent.getId()
						,parent.getExportedVersion().getVersion()
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
		}
	}

	public boolean exportImage(String path, byte[] image) throws SQLException {
	    // we do not export null images (should never happen, but it sometimes does)
	    if ( image == null ) 
	        return true;

		boolean exported = false;

		try ( ResultSet result = select("SELECT path FROM "+this.schema+"images WHERE path = ?", path) ) {

			if ( result.next() ) {
				// if the image exists in the database, we update it
			    request("UPDATE "+this.schema+"images SET image = ? WHERE path = ?"
						,image
						,path
						);
					exported = true;
			} else {
				// if the image is not yet in the db, we insert it
				String[] databaseColumns = {"path", "image"};
				insert(this.schema+"images", databaseColumns
						,path
						,image							
						);
				exported = true;
			}
		}
		return exported;
	}

	private static String encode (EList<IDiagramModelConnection> connections) {
		StringBuilder result = new StringBuilder();
		for ( IDiagramModelConnection cnct: connections ) {
			if ( result.length() > 0 )
				result.append(",");
			result.append(cnct.getId());
		}
		return result.toString();
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