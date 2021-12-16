/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFeatures;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProfile;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.impl.Folder;

import lombok.Getter;

/**
 * This class holds the information required to connect to, to import from and export to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseExportConnection extends DBDatabaseConnection {
	private static final DBLogger logger = new DBLogger(DBDatabaseExportConnection.class);
	
	/**
	 * max number of IN values in a SQL select request
	 */
	private static int maxValuesInSQLRequest = 1000;

	private boolean isconnectionDuplicate = false;

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
		super.schemaPrefix = importConnection.schemaPrefix;
		super.connection = importConnection.connection;
		this.isconnectionDuplicate = true;
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
		super.schemaPrefix = databaseConnection.schemaPrefix;
		super.connection = databaseConnection.connection;
		this.isconnectionDuplicate = true;
	}

	@Getter private HashMap<String, DBMetadata> profilesNotInModel = new HashMap<String, DBMetadata>();
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
		try ( DBSelect resultLatestVersion = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT version, checksum, created_on FROM "+this.schemaPrefix+"models WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"models WHERE id = ?)", modelId, modelId) ) {
			// we get the latest model version from the database
			if ( resultLatestVersion.next() && (resultLatestVersion.getObject("version") != null) ) {
				// if the version is found, then the model exists in the database
				model.getDatabaseVersion().setVersion(resultLatestVersion.getInt("version"));
				model.getDatabaseVersion().setChecksum(resultLatestVersion.getString("checksum"));
				model.getDatabaseVersion().setTimestamp(resultLatestVersion.getTimestamp("created_on"));

				// we check if the model has been imported from (or last exported to) this database
				if ( !model.getInitialVersion().getTimestamp().equals(DBVersion.NEVER) ) {
					try ( DBSelect resultCurrentVersion = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT version, checksum FROM "+this.schemaPrefix+"models WHERE id = ? AND created_on = ?", modelId, model.getInitialVersion().getTimestamp()) ) {
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
		//int modelInitialVersion;
		int modelDatabaseVersion;

		DBMetadata metadata = DBMetadata.getDBMetadata(component);

		if ( logger.isTraceEnabled() ) {
			logger.trace("   Getting version of "+metadata.getDebugName()+" from the database.");
		}

		if ( component instanceof DBArchimateModel )  {
			request = "select id, name, version, checksum, created_on, created_by, id AS model_id, version AS model_version"
					+ " FROM "+this.schemaPrefix+" models"
					+ " WHERE id = ?"
					+ " ORDER BY version";
			DBArchimateModel model = (DBArchimateModel)component;
			modelId = model.getId();
			//modelInitialVersion = model.getInitialVersion().getVersion();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IArchimateElement )  {
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"elements"
					+ " LEFT JOIN "+this.schemaPrefix+"elements_in_model ON element_id = id AND element_version = version"
					+ " WHERE id = ?"
					+ " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IArchimateElement)component).getArchimateModel();
			modelId = model.getId();
			//modelInitialVersion = model.getInitialVersion().getVersion();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IArchimateRelationship ) {
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"relationships"
					+ " LEFT JOIN "+this.schemaPrefix+"relationships_in_model ON relationship_id = id AND relationship_version = version"
					+ " WHERE id = ?"
					+ " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IArchimateRelationship)component).getArchimateModel();
			modelId = model.getId();
			//modelInitialVersion = model.getInitialVersion().getVersion();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IFolder ) {
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"folders"
					+ " LEFT JOIN "+this.schemaPrefix+"folders_in_model ON folder_id = id AND folder_version = version"
					+ " WHERE id = ?"
					+ " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IFolder)component).getArchimateModel();
			modelId = model.getId();
			//modelInitialVersion = model.getInitialVersion().getVersion();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IDiagramModel ) {
			request = "SELECT id, name, version, checksum, container_checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"views"
					+ " LEFT JOIN "+this.schemaPrefix+"views_in_model ON view_id = id AND view_version = version"
					+ " WHERE id = ?"
					+ " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IDiagramModel)component).getArchimateModel();
			modelId = model.getId();
			//modelInitialVersion = model.getInitialVersion().getVersion();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IDiagramModelObject  ) {
			request = "SELECT id, name, version, checksum, created_on, created_by, view_id as model_id, view_version as model_version"		// for convenience, we rename view_id to model_id and view_version to model_version
					+ " FROM "+this.schemaPrefix+"views_objects"
					+ " LEFT JOIN "+this.schemaPrefix+"views_objects_in_view ON object_id = id AND object_version = version"
					+ " WHERE id = ?"
					+ " ORDER BY version, view_version";
			IDiagramModel diagram = ((IDiagramModelObject)component).getDiagramModel();
			modelId = diagram.getId();
			DBMetadata dbMetadata = DBMetadata.getDBMetadata(diagram);
			//modelInitialVersion = dbMetadata.getInitialVersion().getVersion();
			modelDatabaseVersion = dbMetadata.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IDiagramModelConnection  ) {
			request = "SELECT id, name, version, checksum, created_on, created_by, view_id as model_id, view_version as model_version"		// for convenience, we rename view_id to model_id and view_version to model_version
					+ " FROM "+this.schemaPrefix+"views_connections"
					+ " LEFT JOIN "+this.schemaPrefix+"views_connections_in_view ON connection_id = id AND connection_version = version"
					+ " WHERE id = ?"
					+ " ORDER BY version, view_version";
			IDiagramModel diagram = ((IDiagramModelConnection)component).getDiagramModel();
			modelId = diagram.getId();
			DBMetadata dbMetadata = DBMetadata.getDBMetadata(diagram);
			//modelInitialVersion = dbMetadata.getInitialVersion().getVersion();
			modelDatabaseVersion = dbMetadata.getDatabaseVersion().getVersion();
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
			String createdBy = null;
			while ( result.next() ) {
				version = result.getInt("version");
				checksum = result.getString("checksum");
				containerChecksum = (component instanceof IDiagramModel ? result.getString("container_checksum") : null);
				createdOn = result.getTimestamp("created_on");
				createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
					// if the component is part of the model, we compare with the model's version
					//if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
					if ( DBPlugin.areEqual(checksum, metadata.getCurrentVersion().getChecksum()) ) {
						metadata.getInitialVersion().set(version, containerChecksum, checksum, createdOn, createdBy);
						metadata.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						metadata.getDatabaseVersion().set(version, containerChecksum, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				metadata.getLatestDatabaseVersion().set(version, containerChecksum, checksum, createdOn, createdBy);
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
	 * Gets the version and checksum of a specific component from the database and fills its DBMetadata.<br>
	 * <br>
	 * Thus method is meant to be called during the export process for every component that is new in the model to check if it is shared with other models.
	 * <br>
	 * @param componentHashMap 
	 * @param gui 
	 * @param component
	 * @throws SQLException
	 */
	public void getHashMapVersionFromDatabase(HashMap<String, IIdentifier> componentHashMap, DBGui gui) throws SQLException {
		assert (componentHashMap != null);

		String request;
		String orderByRequest = "";
		String modelId;
		int modelDatabaseVersion;

		// we get the "first" (in fact any) component of the hashmap to determine its class
		IIdentifier component = componentHashMap.entrySet().iterator().next().getValue();

		// unused at this stage, but to avoid null warning
		DBMetadata dbMetadata = DBMetadata.getDBMetadata(component);

		if ( component instanceof IArchimateElement )  {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" elements from the database.");
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"elements"
					+ " LEFT JOIN "+this.schemaPrefix+"elements_in_model ON element_id = id AND element_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY id, version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IArchimateElement)component).getArchimateModel();
			modelId = model.getId();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IArchimateRelationship ) {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" relationships from the database.");
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"relationships"
					+ " LEFT JOIN "+this.schemaPrefix+"relationships_in_model ON relationship_id = id AND relationship_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IArchimateRelationship)component).getArchimateModel();
			modelId = model.getId();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IFolder ) {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" folders from the database.");
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"folders"
					+ " LEFT JOIN "+this.schemaPrefix+"folders_in_model ON folder_id = id AND folder_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IFolder)component).getArchimateModel();
			modelId = model.getId();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IDiagramModel ) {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" views from the database.");
			request = "SELECT id, name, version, checksum, container_checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"views"
					+ " LEFT JOIN "+this.schemaPrefix+"views_in_model ON view_id = id AND view_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IDiagramModel)component).getArchimateModel();
			modelId = model.getId();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IDiagramModelObject  ) {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" view objects from the database.");
			request = "SELECT id, name, version, checksum, created_on, created_by, view_id as model_id, view_version as model_version"		// for convenience, we rename view_id to model_id and view_version to model_version
					+ " FROM "+this.schemaPrefix+"views_objects"
					+ " LEFT JOIN "+this.schemaPrefix+"views_objects_in_view ON object_id = id AND object_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY version, view_version";
			IDiagramModel diagram = ((IDiagramModelObject)component).getDiagramModel();
			modelId = diagram.getId();
			dbMetadata = DBMetadata.getDBMetadata(diagram);
			modelDatabaseVersion = dbMetadata.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IDiagramModelConnection  ) {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" view connections from the database.");
			request = "SELECT id, name, version, checksum, created_on, created_by, view_id as model_id, view_version as model_version"		// for convenience, we rename view_id to model_id and view_version to model_version
					+ " FROM "+this.schemaPrefix+"views_connections"
					+ " LEFT JOIN "+this.schemaPrefix+"views_connections_in_view ON connection_id = id AND connection_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY version, view_version";
			IDiagramModel diagram = ((IDiagramModelConnection)component).getDiagramModel();
			modelId = diagram.getId();
			dbMetadata = DBMetadata.getDBMetadata(diagram);
			modelDatabaseVersion = dbMetadata.getDatabaseVersion().getVersion();
		}
		else if ( component instanceof IProfile ) {
			if ( logger.isTraceEnabled() )
				logger.trace("   Searching for "+componentHashMap.size()+" specializations from the database.");
			request = "SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
					+ " FROM "+this.schemaPrefix+"profiles"
					+ " LEFT JOIN "+this.schemaPrefix+"profiles_in_model ON profile_id = id AND profile_version = version"
					+ " WHERE id in (";
			orderByRequest = " ORDER BY version, model_version";
			DBArchimateModel model = (DBArchimateModel) ((IProfile)component).getArchimateModel();
			modelId = model.getId();
			modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		}
		else
			throw new SQLException("Do not know how to get a "+component.getClass().getSimpleName()+" from the database.");

		// we construct the IN string
		StringBuilder questionMarks = new StringBuilder();
		ArrayList<String> idList = new ArrayList<String>();

		Iterator<Entry<String, IIdentifier>> iterator = componentHashMap.entrySet().iterator();
		while (iterator.hasNext()) {
			component = iterator.next().getValue();

			dbMetadata = DBMetadata.getDBMetadata(component);

			dbMetadata.getCurrentVersion().setVersion(0);
			dbMetadata.getInitialVersion().reset();
			dbMetadata.getDatabaseVersion().reset();
			dbMetadata.getLatestDatabaseVersion().reset();

			if ( questionMarks.length() != 0 )
				questionMarks.append(", ");

			questionMarks.append("?");
			idList.add(component.getId());
		}

		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, request+questionMarks+")"+orderByRequest, idList) ) {
			int version = 0;
			String checksum = null;
			String containerChecksum = null;
			Timestamp createdOn = null;
			String createdBy = null;
			String oldId = null;
			String id;

			while ( result.next() ) {
				id = result.getString("id");

				if ( !id.equals(oldId) ) {
					if ( (oldId != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
						
						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + dbMetadata.getInitialVersion().getVersion());
							logger.trace("         Current version = " + dbMetadata.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ dbMetadata.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ dbMetadata.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + dbMetadata.getDatabaseStatus());
						}
					}

					component = componentHashMap.get(id);
					dbMetadata = DBMetadata.getDBMetadata(component);

					if ( logger.isTraceEnabled() )
						logger.trace("   Found component "+dbMetadata.getDebugName()+" in the database.");
				}

				version = result.getInt("version");
				checksum = result.getString("checksum");
				containerChecksum = (component instanceof IDiagramModel ? result.getString("container_checksum") : null);
				createdOn = result.getTimestamp("created_on");
				createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
					// if the component is part of the model, we compare with the model's version
					// if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
					if ( result.getInt("model_version") == 0 || checksum.equals(dbMetadata.getCurrentVersion().getChecksum()) ) {						
						dbMetadata.getInitialVersion().set(version, containerChecksum, checksum, createdOn, createdBy);
						dbMetadata.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						dbMetadata.getDatabaseVersion().set(version, containerChecksum, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				dbMetadata.getLatestDatabaseVersion().set(version, containerChecksum, checksum, createdOn, createdBy);

				oldId = id;
			}

			if ( (oldId != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
				
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + dbMetadata.getInitialVersion().getVersion());
					logger.trace("         Current version = " + dbMetadata.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ dbMetadata.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ dbMetadata.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + dbMetadata.getDatabaseStatus());
				}
			}
		}
	}

	/**
	 * Gets the version of all the model's components, and checks as well for the components that are in the latest model's version in the database but that are not in the model.
	 * Those components are stored in the << not in model >> hashmaps:
	 * <ul>
	 *    <li> profilesNotInModel
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
	 * @param gui 
	 * @throws SQLException
	 */
	public void getAllVersionFromDatabase(DBArchimateModel model, DBGui gui) throws SQLException {
		// we do not manage versions in a Neo4J database
		assert(!DBPlugin.areEqual(this.databaseEntry.getDriver().toLowerCase(), "neo4j"));

		// we reset the variables
		this.profilesNotInModel.clear();
		this.elementsNotInModel.clear();
		this.relationshipsNotInModel.clear();
		this.foldersNotInModel.clear();
		this.viewsNotInModel.clear();
		this.viewObjectsNotInModel.clear();
		this.viewConnectionsNotInModel.clear();
		this.imagesNotInModel.clear();
		this.imagesNotInDatabase.clear();

		getModelVersionFromDatabase(model);
		if ( gui != null ) gui.increaseProgressBar();

		String modelId = model.getId();
		int modelInitialVersion = model.getInitialVersion().getVersion();
		int modelDatabaseVersion = model.getDatabaseVersion().getVersion();
		HashMap<String, IIdentifier> componentHashMap = new HashMap<String, IIdentifier>();
		
		
		//////////////////// PROFILES
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the profiles from the database");

		// we reset the version of all the profiles in the model
		Iterator<Map.Entry<String, IProfile>> itp = model.getAllProfiles().entrySet().iterator();
		while (itp.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itp.next().getValue());
			dbMetadata.getCurrentVersion().setVersion(0);
			dbMetadata.getInitialVersion().reset();
			dbMetadata.getDatabaseVersion().reset();
			dbMetadata.getLatestDatabaseVersion().reset();
		}
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, created_on, created_by, model_id, model_version, pos"
						+ " FROM "+this.schemaPrefix+"profiles"
						+ " LEFT JOIN "+this.schemaPrefix+"profiles_in_model ON profile_id = id AND profile_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"profiles JOIN "+this.schemaPrefix+"profiles_in_model ON profile_id = id AND profile_version = version WHERE model_id = ? AND model_version = ?)"
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
				String createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();

						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					IArchimateModelObject object = model.getAllProfiles().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(currentId);
						this.profilesNotInModel.put(currentId, currentComponent);
						logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database, but not in the model)");
					} else
						logger.trace("   Getting version of "+currentComponent.getDebugName()+" (is in the database and in the model)");
				}

				if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
					// if the component is part of the model, we compare with the model's version
					if ( result.getInt("model_version") == modelInitialVersion || checksum.equals(currentComponent.getCurrentVersion().getChecksum()) ) {
						currentComponent.getInitialVersion().set(version, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						currentComponent.getDatabaseVersion().set(version, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn, createdBy);

				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();

				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}

		// if some components have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		itp = model.getAllProfiles().entrySet().iterator();
		while (itp.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itp.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}
		/* ***************************************************************** */


		//////////////////// ELEMENTS
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the elements from the database");

		// we reset the version of all the elements in the model
		Iterator<Map.Entry<String, IArchimateElement>> ite = model.getAllElements().entrySet().iterator();
		while (ite.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(ite.next().getValue());
			dbMetadata.getCurrentVersion().setVersion(0);
			dbMetadata.getInitialVersion().reset();
			dbMetadata.getDatabaseVersion().reset();
			dbMetadata.getLatestDatabaseVersion().reset();
		}

		// we get all the elements that are part of the latest version of the model in the database and compare them to the actual model
		// we do not use max(version) in the SQL request as all database brands do not support it
		// so we get all the version (sorted by the version) and determine the latest version of each element when the ID changes or when we read the latest element
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
						+ " FROM "+this.schemaPrefix+"elements"
						+ " LEFT JOIN "+this.schemaPrefix+"elements_in_model ON element_id = id AND element_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"elements JOIN "+this.schemaPrefix+"elements_in_model ON element_id = id AND element_version = version WHERE model_id = ? AND model_version = ?)"
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
				String createdBy = result.getString("created_by");

				// if the current element ID is equals to the previous element ID, it means that we got a newer version of the same element
				// so we just archive the current component as previous component for future reference 
				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					// If the current ID is different from the previous ID, it means we got a new component and not a new version of the same component
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
						
						if ( logger.isTraceEnabled() ) {
							// If there is a previous component, we print some trace information
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					// we check if the component is present in the actual model
					IArchimateModelObject object = model.getAllElements().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

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
					//if ( modelInitialVersion == 0 || result.getInt("model_version") == modelInitialVersion ) {
					if ( result.getInt("model_version") == modelInitialVersion || checksum.equals(currentComponent.getCurrentVersion().getChecksum()) ) {
						currentComponent.getInitialVersion().set(version, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						currentComponent.getDatabaseVersion().set(version, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn, createdBy);

				// we copy currentComponent to previous component to be able to retrieve it in next loop
				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
				
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}

		// if some components have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		ite = model.getAllElements().entrySet().iterator();
		while (ite.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(ite.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}



		//////////////////// RELATIONSHIPS
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the relationships from the database");

		// we reset the version of all the relationships in the model
		Iterator<Map.Entry<String, IArchimateRelationship>> itr = model.getAllRelationships().entrySet().iterator();
		while (itr.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itr.next().getValue());
			dbMetadata.getCurrentVersion().setVersion(0);
			dbMetadata.getInitialVersion().reset();
			dbMetadata.getDatabaseVersion().reset();
			dbMetadata.getLatestDatabaseVersion().reset();
		}

		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
						+ " FROM "+this.schemaPrefix+"relationships"
						+ " LEFT JOIN "+this.schemaPrefix+"relationships_in_model ON relationship_id = id AND relationship_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"relationships JOIN "+this.schemaPrefix+"relationships_in_model ON relationship_id = id AND relationship_version = version WHERE model_id = ? AND model_version = ?)"
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
				String createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
						
						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					IArchimateModelObject object = model.getAllRelationships().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

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
					if ( result.getInt("model_version") == modelInitialVersion || checksum.equals(currentComponent.getCurrentVersion().getChecksum()) ) {
						currentComponent.getInitialVersion().set(version, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						currentComponent.getDatabaseVersion().set(version, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn, createdBy);

				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
				
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}

		// if some components have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		itr = model.getAllRelationships().entrySet().iterator();
		while (itr.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itr.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}



		//////////////////// FOLDERS
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the folders from the database");

		// we reset the version of all the folders in the model
		Iterator<Map.Entry<String, IFolder>> itf = model.getAllFolders().entrySet().iterator();
		while (itf.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itf.next().getValue());
			dbMetadata.getCurrentVersion().setVersion(0);
			dbMetadata.getInitialVersion().reset();
			dbMetadata.getDatabaseVersion().reset();
			dbMetadata.getLatestDatabaseVersion().reset();
		}
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, created_on, created_by, model_id, model_version"
						+ " FROM "+this.schemaPrefix+"folders"
						+ " LEFT JOIN "+this.schemaPrefix+"folders_in_model ON folder_id = id AND folder_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"folders JOIN "+this.schemaPrefix+"folders_in_model ON folder_id = id AND folder_version = version WHERE model_id = ? AND model_version = ?)"
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
				String createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
						
						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					IArchimateModelObject object = model.getAllFolders().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

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
					if ( result.getInt("model_version") == modelInitialVersion || checksum.equals(currentComponent.getCurrentVersion().getChecksum()) ) {
						currentComponent.getInitialVersion().set(version, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						currentComponent.getDatabaseVersion().set(version, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn, createdBy);

				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
			
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}

		// if some components have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		itf = model.getAllFolders().entrySet().iterator();
		while (itf.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itf.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}




		//////////////////// VIEWS
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the views from the database");

		// we reset the version of all the views in the model
		Iterator<Map.Entry<String, IDiagramModel>> itv = model.getAllViews().entrySet().iterator();
		while (itv.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itv.next().getValue());
			dbMetadata.getCurrentVersion().setVersion(0);
			dbMetadata.getInitialVersion().reset();
			dbMetadata.getDatabaseVersion().reset();
			dbMetadata.getLatestDatabaseVersion().reset();
		}
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, container_checksum, created_on, created_by, model_id, model_version"
						+ " FROM "+this.schemaPrefix+"views"
						+ " LEFT JOIN "+this.schemaPrefix+"views_in_model ON view_id = id AND view_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"views JOIN "+this.schemaPrefix+"views_in_model ON view_id = id AND view_version = version WHERE model_id = ? AND model_version = ?)"
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
				String createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
					
						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					IArchimateModelObject object = model.getAllViews().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

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
					if ( result.getInt("model_version") == modelInitialVersion || checksum.equals(currentComponent.getCurrentVersion().getChecksum()) ) {
						currentComponent.getInitialVersion().set(version, containerChecksum, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("model_version") == modelDatabaseVersion )
						currentComponent.getDatabaseVersion().set(version,  containerChecksum, checksum, createdOn, createdBy);
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, containerChecksum, checksum, createdOn, createdBy);

				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
			
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}

		// if some components have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		itv = model.getAllViews().entrySet().iterator();
		while (itv.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itv.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}




		// we check if the latest version of the model has got images that are not in the model
		if ( logger.isDebugEnabled() ) logger.debug("Checking missing images from the database");
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT DISTINCT image_path FROM "+this.schemaPrefix+"views_objects "
				+ "JOIN "+this.schemaPrefix+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version "
				+ "JOIN "+this.schemaPrefix+"views_in_model ON views_in_model.view_id = views_objects_in_view.view_id AND views_in_model.view_version = views_objects_in_view.view_version "
				+ "WHERE image_path IS NOT NULL AND views_in_model.model_id = ? AND views_in_model.model_version = ?"
				,model.getId()
				,model.getDatabaseVersion().getVersion()
				) ) {
			while ( result.next() ) {
				if ( !model.getAllImagePaths().contains(result.getString("image_path")) ) {
					this.imagesNotInModel.put(result.getString("image_path"), new DBMetadata());
				}
				if ( gui != null ) gui.increaseProgressBar();
			}
		}

		// we compare the objects and connections of existing views
		Iterator<Entry<String, IDiagramModel>> viewsIterator = model.getAllViews().entrySet().iterator();
		while ( viewsIterator.hasNext() )
			getViewObjectsAndConnectionsVersionsFromDatabase(model, model.getDBMetadata(viewsIterator.next().getValue()), gui);

		// we also need to compare the objects and connections that are in the views that will be imported into the model
		Iterator<Entry<String, DBMetadata>> viewsNotInModelIterator = this.viewsNotInModel.entrySet().iterator();
		while ( viewsNotInModelIterator.hasNext() )
			getViewObjectsAndConnectionsVersionsFromDatabase(model, viewsNotInModelIterator.next().getValue(), gui);
		
		
		
		// if some view objects have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		Iterator<Map.Entry<String, IDiagramModelObject>> itvo = model.getAllViewObjects().entrySet().iterator();
		while (itvo.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itvo.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}
		
		// if some view connections have got an initialVersion equal to zero, it means that they're not part of the latest version of the model in the database
		// so we check if they are completely new or if they exist in another model
		Iterator<Map.Entry<String, IDiagramModelConnection>> itvc = model.getAllViewConnections().entrySet().iterator();
		while (itvc.hasNext()) {
			DBMetadata dbMetadata = model.getDBMetadata(itvc.next().getValue());
			if ( dbMetadata.getInitialVersion().getVersion() == 0 )
				componentHashMap.put(dbMetadata.getId(), (IIdentifier)dbMetadata.getComponent());

			if ( componentHashMap.size() == DBDatabaseExportConnection.maxValuesInSQLRequest ) {
				getHashMapVersionFromDatabase(componentHashMap, gui);
				componentHashMap.clear();
			}
		}
		if ( componentHashMap.size() != 0 ) {
			getHashMapVersionFromDatabase(componentHashMap, gui);
			componentHashMap.clear();
		}
		
		
		
		

		// even if the model does not exist in the database, the images can exist in the database
		// images do not have a version as they cannot be modified. Their path is a checksum and loading a new image creates a new path.

		// at last, we check if all the images in the model are in the database
		// we did not know them before now, so do not increase the progress bar !
		if ( logger.isDebugEnabled() ) logger.debug("Checking if the images exist in the database");
		for ( String path: model.getAllImagePaths() ) {
			try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT path from "+this.schemaPrefix+"images where path = ?", path) ) {
				if ( result.next() && result.getObject("path") != null ) {
					// the image is in the database
				} else {
					// the image is not in the database
					this.imagesNotInDatabase.put(path, new DBMetadata());
				}
			}
		}
	}

	private void getViewObjectsAndConnectionsVersionsFromDatabase(DBArchimateModel model, DBMetadata viewMetadata, DBGui gui) throws SQLException, RuntimeException {
		String viewId = viewMetadata.getId();

		int viewInitialVersion = viewMetadata.getInitialVersion().getVersion();
		int viewDatabaseVersion = viewMetadata.getLatestDatabaseVersion().getVersion();

		// view objects
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of view objects from the database for "+viewMetadata.getDebugName());
		Iterator<Map.Entry<String, IDiagramModelObject>> itvo = model.getAllViewObjects().entrySet().iterator();
		while (itvo.hasNext()) {
			IDiagramModelObject object = itvo.next().getValue();
			if ( object.getDiagramModel().getId().equals(viewMetadata.getId()) ) {
				DBMetadata dbMetadata = model.getDBMetadata(object);
				dbMetadata.getCurrentVersion().setVersion(0);
				dbMetadata.getInitialVersion().reset();
				dbMetadata.getDatabaseVersion().reset();
				dbMetadata.getLatestDatabaseVersion().reset();
			}
		}			
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, created_on, created_by, view_id, view_version"
						+ " FROM "+this.schemaPrefix+"views_objects"
						+ " LEFT JOIN "+this.schemaPrefix+"views_objects_in_view ON object_id = id AND object_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"views_objects JOIN "+this.schemaPrefix+"views_objects_in_view ON object_id = id AND object_version = version WHERE view_id = ? AND view_version = ?)"
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
				String createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
						
						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					IDiagramModelObject object = model.getAllViewObjects().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

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
						currentComponent.getInitialVersion().set(version, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("view_version") == viewDatabaseVersion ) {
						currentComponent.getDatabaseVersion().set(version, checksum, createdOn, createdBy);
					}
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn, createdBy);

				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
				
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}


		
		// view connections
		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of view connections from the database for "+viewMetadata.getDebugName());
		Iterator<Map.Entry<String, IDiagramModelConnection>> itvc = model.getAllViewConnections().entrySet().iterator();
		while (itvc.hasNext()) {
			IDiagramModelConnection cnct = itvc.next().getValue();
			if ( cnct.getDiagramModel().getId().equals(viewMetadata.getId()) ) {
				DBMetadata dbMetadata = model.getDBMetadata(cnct);
				dbMetadata.getCurrentVersion().setVersion(0);
				dbMetadata.getInitialVersion().reset();
				dbMetadata.getDatabaseVersion().reset();
				dbMetadata.getLatestDatabaseVersion().reset();
			}
		}
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, 
				"SELECT id, name, version, checksum, created_on, created_by, view_id, view_version"
						+ " FROM "+this.schemaPrefix+"views_connections"
						+ " LEFT JOIN "+this.schemaPrefix+"views_connections_in_view ON connection_id = id AND connection_version = version"
						+ " WHERE id IN (SELECT id FROM "+this.schemaPrefix+"views_connections JOIN "+this.schemaPrefix+"views_connections_in_view ON connection_id = id AND connection_version = version WHERE view_id = ? AND view_version = ?)"
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
				String createdBy = result.getString("created_by");

				if ( DBPlugin.areEqual(currentId, previousId) )
					currentComponent = previousComponent;
				else {
					if ( (previousComponent != null) ) {
						if ( gui != null ) gui.increaseProgressBar();
						
						if ( logger.isTraceEnabled() ) {
							logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
							logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
							logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
							logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
							logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
						}
					}

					IDiagramModelConnection object = model.getAllViewConnections().get(currentId);
					currentComponent = (object == null ) ? null : model.getDBMetadata(object);

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
						currentComponent.getInitialVersion().set(version, checksum, createdOn, createdBy);
						currentComponent.getCurrentVersion().setVersion(version);
					}
					if ( result.getInt("view_version") == viewDatabaseVersion ) {
						currentComponent.getDatabaseVersion().set(version, checksum, createdOn, createdBy);
					}
				}

				// components are sorted by version (so also by timestamp) so the latest found is the latest in time
				currentComponent.getLatestDatabaseVersion().set(version, checksum, createdOn, createdBy);

				previousComponent = currentComponent;
				previousId = currentId;
			}

			if ( (previousComponent != null) ) {
				if ( gui != null ) gui.increaseProgressBar();
				
				if ( logger.isTraceEnabled() ) {
					logger.trace("         Initial version = " + previousComponent.getInitialVersion().getVersion());
					logger.trace("         Current version = " + previousComponent.getCurrentVersion().getVersion());
					logger.trace("         Database version = "+ previousComponent.getDatabaseVersion().getVersion());
					logger.trace("         Latest db version = "+ previousComponent.getLatestDatabaseVersion().getVersion());
					logger.trace("         Database status = " + previousComponent.getDatabaseStatus());
				}
			}
		}
	}

	/**
	 * Empty a Neo4J database
	 * @throws Exception 
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
		final String[] modelsColumns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "properties", "features", "checksum"};

		if ( (model.getName() == null) || (model.getName().equals("")) )
			throw new RuntimeException("Model name cannot be empty.");

		model.getCurrentVersion().setVersion(model.getDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting model (initial version = "+model.getInitialVersion().getVersion()+", exported version = "+model.getCurrentVersion().getVersion()+", latest database version = "+model.getDatabaseVersion().getVersion()+")");

		if ( this.connection.getAutoCommit() )
			model.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
		else
			model.getCurrentVersion().setTimestamp(this.lastTransactionTimestamp);
		
		DBMetadata metadata = DBMetadata.getDBMetadata(model);

		int nbProperties = metadata.getNumberOfProperties();
		int nbFeatures = metadata.getNumberOfFeatures();

		insert(this.schemaPrefix+"models", modelsColumns
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,model.getName()
				,releaseNote
				,model.getPurpose()
				,System.getProperty("user.name")
				,model.getCurrentVersion().getTimestamp()
				,nbProperties
				,nbFeatures
				,model.getCurrentVersion().getChecksum()
				);

		if ( nbProperties != 0 )
			exportProperties(model);

		if ( nbFeatures != 0 )
			exportFeatures(model);

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
		else if ( eObject instanceof IProfile )					exportProfile((IProfile)eObject);
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
		else if ( eObject instanceof IProfile )					assignProfileToModel((IProfile)eObject);
		else
			throw new Exception("Do not know how to assign to the model: "+eObject.getClass().getSimpleName());
	}

	/**
	 * Export an element to the database
	 * @param element 
	 * @throws Exception 
	 */
	private void exportElement(IArchimateElement element) throws Exception {
		final String[] elementsColumns = {"id", "version", "class", "name", "type", "documentation", "profile", "created_by", "created_on", "properties", "features", "checksum"};
		DBArchimateModel model = (DBArchimateModel)element.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(element);

		// if the element is exported, the we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		int nbProperties = (element.getProperties() == null) ? 0 : element.getProperties().size();
		int nbFeatures = (element.getFeatures() == null) ? 0 : element.getFeatures().size();

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			// TODO: USE MERGE instead to replace existing nodes
			executeRequest("CREATE (new:elements {id:?, version:?, class:?, name:?, type:?, documentation:?, checksum:?})"
					,element.getId()
					,dbMetadata.getCurrentVersion().getVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,dbMetadata.getJunctionType()
					,element.getDocumentation()
					,dbMetadata.getCurrentVersion().getChecksum()
					);
		} else {
			insert(this.schemaPrefix+"elements", elementsColumns
					,element.getId()
					,dbMetadata.getCurrentVersion().getVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,dbMetadata.getJunctionType()
					,element.getDocumentation()
					,dbMetadata.getPrimaryProfileID()
					,System.getProperty("user.name")
					,model.getCurrentVersion().getTimestamp()
					,nbProperties
					,nbFeatures
					,dbMetadata.getCurrentVersion().getChecksum()
					);
		}

		if ( nbProperties != 0 )
			exportProperties(element);

		if ( nbFeatures != 0)
			exportFeatures(element);
	}

	/**
	 * This class variable allows to sort the exported elements that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int elementPos = 0;

	/**
	 * Assign an element to a model into the database
	 * @param element 
	 * @throws Exception 
	 */
	private void assignElementToModel(IArchimateElement element) throws Exception {
		final String[] elementsInModelColumns = {"element_id", "element_version", "parent_folder_id", "model_id", "model_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)element.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(element);

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning element to model");

		insert(this.schemaPrefix+"elements_in_model", elementsInModelColumns
				,element.getId()
				,dbMetadata.getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportElement()
				,((IFolder)element.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.elementPos
				);
	}

	/**
	 * Export a relationship to the database
	 * @param relationship 
	 * @throws Exception 
	 */
	private void exportRelationship(IArchimateRelationship relationship) throws Exception {
		final String[] relationshipsColumns = {"id", "version", "class", "name", "documentation", "source_id", "target_id", "strength", "access_type", "is_directed", "profile", "created_by", "created_on", "properties", "features", "checksum"};
		DBArchimateModel model = (DBArchimateModel)relationship.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(relationship);

		// if the relationship is exported, the we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		int nbProperties = (relationship.getProperties() == null) ? 0 : relationship.getProperties().size();
		int nbFeatures = (relationship.getFeatures() == null) ? 0 : relationship.getFeatures().size();

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			String relationshipType = (this.databaseEntry.isNeo4jTypedRelationship() ? (relationship.getClass().getSimpleName()+"s") : "relationships");
			// TODO: USE MERGE instead to replace existing nodes

			// we remove the "Relationship" suffix from the relationship name
			String relationshipName = relationship.getClass().getSimpleName();
			if ( relationshipName.endsWith("Relationship") )
				relationshipName = relationshipName.substring(0, relationshipName.length() - 12);

			if ( this.databaseEntry.isNeo4jNativeMode() ) {
				if ( (relationship.getSource() instanceof IArchimateElement) && (relationship.getTarget() instanceof IArchimateElement) ) {
					executeRequest("MATCH (source:elements {id:?, version:?}), (target:elements {id:?, version:?}) CREATE (source)-[relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}]->(target)"
							,relationship.getSource().getId()
							,model.getDBMetadata(relationship.getSource()).getCurrentVersion().getVersion()
							,relationship.getTarget().getId()
							,model.getDBMetadata(relationship.getTarget()).getCurrentVersion().getVersion()
							,relationship.getId()
							,dbMetadata.getCurrentVersion().getVersion()
							,relationshipName
							,relationship.getName()
							,relationship.getDocumentation()
							,dbMetadata.getStrength()
							,dbMetadata.getAccessType()
							,dbMetadata.getCurrentVersion().getChecksum()
							);
				}
			} else {
				executeRequest("MATCH (source {id:?, version:?}), (target {id:?, version:?}) CREATE (relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}), (source)-[rel1:relatedTo]->(relationship)-[rel2:relatedTo]->(target)"
						,relationship.getSource().getId()
						,model.getDBMetadata(relationship.getSource()).getCurrentVersion().getVersion()
						,relationship.getTarget().getId()
						,model.getDBMetadata(relationship.getTarget()).getCurrentVersion().getVersion()
						,relationship.getId()
						,dbMetadata.getCurrentVersion().getVersion()
						,relationship.getClass().getSimpleName()
						,relationship.getName()
						,relationship.getDocumentation()
						,dbMetadata.getStrength()
						,dbMetadata.getAccessType()
						,dbMetadata.getCurrentVersion().getChecksum()
						);
			}
		} else {
			insert(this.schemaPrefix+"relationships", relationshipsColumns
					,relationship.getId()
					,dbMetadata.getCurrentVersion().getVersion()
					,relationship.getClass().getSimpleName()
					,relationship.getName()
					,relationship.getDocumentation()
					,relationship.getSource().getId()
					,relationship.getTarget().getId()
					,dbMetadata.getStrength()
					,dbMetadata.getAccessType()
					,dbMetadata.isDirectedAsInteger()
					,dbMetadata.getPrimaryProfileID()
					,System.getProperty("user.name")
					,model.getCurrentVersion().getTimestamp()
					,nbProperties
					,nbFeatures
					,dbMetadata.getCurrentVersion().getChecksum()
					);
		}

		if ( nbProperties != 0 )
			exportProperties(relationship);

		if ( nbFeatures != 0 )
			exportFeatures(relationship);
	}

	/**
	 * This class variable allows to sort the exported relationships that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int relationshipPos = 0;

	/**
	 * Assign a relationship to a model into the database
	 * @param relationship 
	 * @throws Exception 
	 */
	private void assignRelationshipToModel(IArchimateRelationship relationship) throws Exception {
		final String[] relationshipsInModelColumns = {"relationship_id", "relationship_version", "parent_folder_id", "model_id", "model_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)relationship.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(relationship);

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning relationship to model");

		insert(this.schemaPrefix+"relationships_in_model", relationshipsInModelColumns
				,relationship.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,((IFolder)relationship.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.relationshipPos
				);
	}

	/**
	 * Export a folder into the database.
	 * @param folder 
	 * @throws Exception 
	 */
	private void exportFolder(IFolder folder) throws Exception {
		final String[] foldersColumns = {"id", "version", "type", "root_type", "name", "documentation", "created_by", "created_on", "properties", "features", "checksum"};
		DBArchimateModel model = (DBArchimateModel)folder.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(folder);

		// if the folder is exported, the we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		int nbProperties = (folder.getProperties() == null) ? 0 : folder.getProperties().size();
		int nbFeatures = (folder.getFeatures() == null) ? 0 : folder.getFeatures().size();

		insert(this.schemaPrefix+"folders", foldersColumns
				,folder.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,folder.getType().getValue()
				,dbMetadata.getRootFolderType()
				,folder.getName()
				,folder.getDocumentation()
				,System.getProperty("user.name")
				,model.getCurrentVersion().getTimestamp()
				,nbProperties
				,nbFeatures
				,dbMetadata.getCurrentVersion().getChecksum()
				);

		if ( nbProperties != 0 )
			exportProperties(folder);

		if ( nbFeatures != 0 )
			exportFeatures(folder);
	}

	/**
	 * This class variable allows to sort the exported folders that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int folderPos = 0;

	/**
	 * Assign a folder to a model into the database
	 * @param folder 
	 * @throws Exception 
	 */
	private void assignFolderToModel(IFolder folder) throws Exception {
		final String[] foldersInModelColumns = {"folder_id", "folder_version", "parent_folder_id", "model_id", "model_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)folder.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(folder);

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning folder to model");

		insert(this.schemaPrefix+"folders_in_model", foldersInModelColumns
				,folder.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,(((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId())
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.folderPos
				);
	}

	/**
	 * Export a view into the database.
	 * @param view 
	 * @throws Exception 
	 */
	private void exportView(IDiagramModel view) throws Exception {
		final String[] ViewsColumns = {"id", "version", "class", "created_by", "created_on", "name", "connection_router_type", "documentation", "viewpoint", "background", "screenshot", "screenshot_scale_factor", "screenshot_border_width", "properties", "features", "checksum", "container_checksum"};
		DBArchimateModel model = (DBArchimateModel)view.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(view);

		// if the view is exported, the we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		int nbProperties = (view.getProperties() == null) ? 0 : view.getProperties().size();
		int nbFeatures = (view.getFeatures() == null) ? 0 : view.getFeatures().size();

		insert(this.schemaPrefix+"views", ViewsColumns
				,view.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,view.getClass().getSimpleName()
				,System.getProperty("user.name")
				,model.getCurrentVersion().getTimestamp()
				,view.getName()
				,view.getConnectionRouterType()
				,view.getDocumentation()
				,dbMetadata.getViewpoint()
				,dbMetadata.getBackground()
				,dbMetadata.getScreenshot().getBytes()
				,dbMetadata.getScreenshot().getScaleFactor()
				,dbMetadata.getScreenshot().getBodrderWidth()
				,nbProperties
				,nbFeatures
				,dbMetadata.getCurrentVersion().getChecksum()
				,dbMetadata.getCurrentVersion().getContainerChecksum()
				);

		if ( nbProperties != 0 )
			exportProperties(view);

		if ( nbFeatures != 0 )
			exportFeatures(view);
	}

	/**
	 * This class variable allows to sort the exported views that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int viewPos = 0;

	/**
	 * Assign a view to a model into the database
	 * @param view 
	 * @throws Exception 
	 */
	private void assignViewToModel(IDiagramModel view) throws Exception {
		final String[] viewsInModelColumns = {"view_id", "view_version", "parent_folder_id", "model_id", "model_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)view.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(view);

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning view to model");

		insert(this.schemaPrefix+"views_in_model", viewsInModelColumns
				,view.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,((IFolder)view.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.viewPos
				);
	}

	/**
	 * This class variable allows to sort the exported views objects that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int viewObjectPos = 0;

	/**
	 * Export a view object into the database.<br>
	 * The pos allows to order the views during the import process.
	 * @param viewObject 
	 * @throws Exception 
	 */
	private void exportViewObject(IDiagramModelComponent viewObject) throws Exception {
		final String[] ViewsObjectsColumns = {"id", "version", "class", "container_id", "element_id", "diagram_ref_id", "type", "border_color", "border_type", "content", "documentation", "is_locked", "image_path", "image_position", "line_color", "line_width", "fill_color", "alpha", "font", "font_color", "name", "notes", "text_alignment", "text_position", "x", "y", "width", "height", "created_by", "created_on", "properties", "features", "checksum"};
		DBArchimateModel model = (DBArchimateModel)viewObject.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(viewObject);

		// if the viewObject is exported, the we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		int nbProperties = (!(viewObject instanceof IProperties) || !(viewObject instanceof IDiagramModelArchimateComponent) || (((IProperties)viewObject).getProperties() == null)) ? 0 : ((IProperties)viewObject).getProperties().size();
		int nbFeatures = (viewObject.getFeatures() == null) ? 0 : viewObject.getFeatures().size();

		insert(this.schemaPrefix+"views_objects", ViewsObjectsColumns
				,((IIdentifier)viewObject).getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,viewObject.getClass().getSimpleName()
				,((IIdentifier)viewObject.eContainer()).getId()
				,dbMetadata.getArchimateConceptId()
				,dbMetadata.getReferencedModelId()
				,dbMetadata.getType()
				,dbMetadata.getBorderColor()
				,dbMetadata.getBorderType()
				,dbMetadata.getContent()
				,dbMetadata.getDocumentation()
				,dbMetadata.isLockedAsInteger()
				,dbMetadata.getImagePath()
				,dbMetadata.getImagePosition()
				,dbMetadata.getLineColor()
				,dbMetadata.getLineWidth()
				,dbMetadata.getFillColor()
				,dbMetadata.getAlpha()
				,dbMetadata.getFont()
				,dbMetadata.getFontColor()
				,viewObject.getName()						// we export the name because it may be useful when parsing the database by hand
				,dbMetadata.getNotes()
				,dbMetadata.getTextAlignment()
				,dbMetadata.getTextPosition()
				,dbMetadata.getX()
				,dbMetadata.getY()
				,dbMetadata.getWidth()
				,dbMetadata.getHeight()
				,System.getProperty("user.name")
				,model.getCurrentVersion().getTimestamp()
				,nbProperties
				,nbFeatures
				,dbMetadata.getCurrentVersion().getChecksum()
				);

		if ( nbProperties != 0 )
			exportProperties((IProperties)viewObject);

		if ( nbFeatures != 0 )
			exportFeatures(viewObject);
	}

	/**
	 * Assign a view Object to a view into the database
	 * @param viewObject 
	 * @throws Exception 
	 */
	private void assignViewObjectToView(IDiagramModelComponent viewObject) throws Exception {
		final String[] viewObjectInViewColumns = {"object_id", "object_version", "view_id", "view_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)viewObject.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(viewObject);
		IDiagramModel viewContainer = viewObject.getDiagramModel();


		if ( logger.isTraceEnabled() ) logger.trace("   Assigning view object to view");

		insert(this.schemaPrefix+"views_objects_in_view", viewObjectInViewColumns
				,viewObject.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,viewContainer.getId()
				,model.getDBMetadata(viewContainer).getCurrentVersion().getVersion()
				,++this.viewObjectPos
				);


	}

	/**
	 * This class variable allows to sort the exported views objects that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int viewConnectionPos = 0;

	/**
	 * Export a view connection into the database.<br>
	 * The pos allows to order the views during the import process.
	 * @param viewConnection 
	 * @throws Exception 
	 */
	private void exportViewConnection(IDiagramModelConnection viewConnection) throws Exception {
		final String[] ViewsConnectionsColumns = {"id", "version", "class", "container_id", "name", "documentation", "is_locked", "line_color", "line_width", "font", "font_color", "relationship_id", "source_object_id", "target_object_id", "text_position", "type", "created_by", "created_on", "properties", "features", "bendpoints", "checksum"};
		DBArchimateModel model = (DBArchimateModel)viewConnection.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(viewConnection);

		// if the viewConnection is exported, then we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		int nbProperties = (viewConnection.getProperties() == null) ? 0 : viewConnection.getProperties().size();
		int nbFeatures = (viewConnection.getFeatures() == null) ? 0 : viewConnection.getFeatures().size();
		int nbBendpoints = (viewConnection.getBendpoints() == null) ? 0 : viewConnection.getBendpoints().size();

		insert(this.schemaPrefix+"views_connections", ViewsConnectionsColumns
				,((IIdentifier)viewConnection).getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,viewConnection.getClass().getSimpleName()
				,((IIdentifier)viewConnection.eContainer()).getId()
				,(!(viewConnection instanceof IDiagramModelArchimateConnection) ? ((INameable)viewConnection).getName() : null)                    // if there is a relationship behind, the name is the relationship name, so no need to store it.
				,(!(viewConnection instanceof IDiagramModelArchimateConnection) ? ((IDocumentable)viewConnection).getDocumentation() : null)       // if there is a relationship behind, the documentation is the relationship name, so no need to store it.
				,dbMetadata.isLockedAsInteger()  
				,viewConnection.getLineColor()
				,viewConnection.getLineWidth()
				,viewConnection.getFont()
				,viewConnection.getFontColor()
				,dbMetadata.getArchimateConceptId()
				,viewConnection.getSource().getId()
				,viewConnection.getTarget().getId()
				,viewConnection.getTextPosition()
				,dbMetadata.getType()
				,System.getProperty("user.name")
				,((DBArchimateModel)viewConnection.getDiagramModel().getArchimateModel()).getCurrentVersion().getTimestamp()
				,nbProperties
				,nbFeatures
				,nbBendpoints
				,dbMetadata.getCurrentVersion().getChecksum()
				);

		if ( nbProperties != 0 )
			exportProperties(viewConnection);

		if ( nbFeatures != 0 )
			exportFeatures(viewConnection);

		if ( nbBendpoints != 0 )
			exportBendpoints(viewConnection);
	}

	/**
	 * Assign a view Connection to a view into the database
	 * @param viewConnection 
	 * @throws SQLException 
	 */
	private void assignViewConnectionToView(IDiagramModelConnection viewConnection) throws SQLException {
		final String[] viewObjectInViewColumns = {"connection_id", "connection_version", "view_id", "view_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)viewConnection.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(viewConnection);
		IDiagramModel viewContainer = viewConnection.getDiagramModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning view connection to view");

		insert(this.schemaPrefix+"views_connections_in_view", viewObjectInViewColumns
				,viewConnection.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,viewContainer.getId()
				,model.getDBMetadata(viewContainer).getCurrentVersion().getVersion()
				,++this.viewConnectionPos
				);


	}

	/**
	 * Export properties to the database
	 * @param parent 
	 * @throws SQLException 
	 */
	private void exportProperties(IProperties parent) throws SQLException {
		final String[] propertiesColumns = {"parent_id", "parent_version", "pos", "name", "value"};

		if ( parent.getProperties() != null ) {
			logger.debug("   Exporting "+parent.getProperties().size()+" properties");

			String parentId = ((IIdentifier)parent).getId();
			int parentVersion = (parent instanceof DBArchimateModel) ? ((DBArchimateModel)parent).getCurrentVersion().getVersion() : DBMetadata.getDBMetadata(parent).getCurrentVersion().getVersion();

			int pos = 0;
			for ( int p = 0 ; p < parent.getProperties().size(); ++p) {
				IProperty prop = parent.getProperties().get(p);
				if ( prop != null) {
					String key = prop.getKey();
					String value = prop.getValue();
					if ( (key != null) && (value != null) ) {
						++pos;
						if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
							executeRequest("MATCH (parent {id:?, version:?}) CREATE (prop:property {pos:?, name:?, value:?}), (parent)-[:hasProperty]->(prop)"
									,parentId
									,parentVersion
									,pos
									,key
									,value
									);
						}
						else
							insert(this.schemaPrefix+"properties", propertiesColumns
									,parentId
									,parentVersion
									,pos
									,key
									,value
									);
					}
				}
			}
		}
	}

	/**
	 * Export features to the database
	 * @param parent 
	 * @throws SQLException 
	 */
	private void exportFeatures(IFeatures parent) throws SQLException {
		final String[] featuresColumns = {"parent_id", "parent_version", "pos", "name", "value"};

		if ( parent.getFeatures() != null ) {
			logger.debug("   Exporting "+parent.getFeatures().size()+" features");

			String parentId = ((IIdentifier)parent).getId();
			int parentVersion = (parent instanceof DBArchimateModel) ? ((DBArchimateModel)parent).getCurrentVersion().getVersion() : DBMetadata.getDBMetadata(parent).getCurrentVersion().getVersion();

			int pos = 0;
			for ( int p = 0 ; p < parent.getFeatures().size(); ++p) {
				IFeature feature = parent.getFeatures().get(pos);
				if ( feature != null ) {
					String name = feature.getName();
					String value = feature.getValue();
					if ( (name != null) && (value != null) ) {
						++pos;
						if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
							executeRequest("MATCH (parent {id:?, version:?}) CREATE (feat:feature {pos:?, name:?, value:?}), (parent)-[:hasFeature]->(feat)"
									,parentId
									,parentVersion
									,pos
									,name
									,value
									);
						}
						else
							insert(this.schemaPrefix+"features", featuresColumns
									,parentId
									,parentVersion
									,pos
									,name
									,value
									);
					}
				}
			}
		}
	}
	
	/**
	 * Export model profiles to the database. Please note that the profiles images are not exported using this method. 
	 * @param profile 
	 * @throws SQLException 
	 */
	private void exportProfile(IProfile profile) throws SQLException {
		final String[] profilesColumns = {"id", "version", "name", "is_specialization", "image_path", "concept_type", "created_by", "created_on", "checksum"};
		DBArchimateModel model = (DBArchimateModel)profile.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(profile);
		
		// if the profile is exported, the we increase its exportedVersion
		dbMetadata.getCurrentVersion().setVersion(dbMetadata.getLatestDatabaseVersion().getVersion() + 1);
		
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+dbMetadata.getDebugName()+" (initial version = "+dbMetadata.getInitialVersion().getVersion()+", exported version = "+dbMetadata.getCurrentVersion().getVersion()+", database_version = "+dbMetadata.getDatabaseVersion().getVersion()+", latest_database_version = "+dbMetadata.getLatestDatabaseVersion().getVersion()+")");

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			executeRequest("CREATE (new:profiles {id:?, version:?, name:?, is_specialization:?, image_path:?, concept_type:?})"
				,profile.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,profile.getName()
				,profile.isSpecialization()
				,profile.getImagePath()
				,profile.getConceptType()
				,System.getProperty("user.name")
				,dbMetadata.getCurrentVersion().getTimestamp()
				,dbMetadata.getCurrentVersion().getChecksum()
				);
		} else {
			insert(this.schemaPrefix+"profiles", profilesColumns
				,profile.getId()
				,dbMetadata.getCurrentVersion().getVersion()
				,profile.getName()
				,profile.isSpecialization()
				,profile.getImagePath()
				,profile.getConceptType()
				,System.getProperty("user.name")
				,dbMetadata.getCurrentVersion().getTimestamp()
				,dbMetadata.getCurrentVersion().getChecksum()
				);
		}
	}
	
	/**
	 * This class variable allows to sort the exported profiles that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int profilePos = 0;
	
	/**
	 * Assign a profile to a model into the database
	 * @param profile 
	 * @throws Exception 
	 */
	private void assignProfileToModel(IProfile profile) throws Exception {
		final String[] profilesInModelColumns = {"profile_id", "profile_version", "model_id", "model_version", "pos"};
		DBArchimateModel model = (DBArchimateModel)profile.getArchimateModel();
		DBMetadata dbMetadata = model.getDBMetadata(profile);

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning profile to model");

		insert(this.schemaPrefix+"profiles_in_model", profilesInModelColumns
				,profile.getId()
				,dbMetadata.getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportProfile()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.profilePos
				);
	}

	/**
	 * Export bendpoints of a DiagramModelConnection
	 * @param parent the diagramModelConnection
	 * @throws SQLException 
	 */
	private void exportBendpoints(IDiagramModelConnection parent) throws SQLException {
		final String[] bendpointsColumns = {"parent_id", "parent_version", "pos", "start_x", "start_y", "end_x", "end_y"};

		if ( (parent.getBendpoints() != null) && !DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			logger.debug("   Exporting "+parent.getBendpoints().size()+" bendpoints");

			String parentId = ((IIdentifier)parent).getId();
			int parentVersion = (parent instanceof DBArchimateModel) ? ((DBArchimateModel)parent).getCurrentVersion().getVersion() : DBMetadata.getDBMetadata(parent).getCurrentVersion().getVersion();

			for ( int pos = 0 ; pos < parent.getBendpoints().size(); ++pos) {
				IDiagramModelBendpoint bendpoint = parent.getBendpoints().get(pos);
				insert(this.schemaPrefix+"bendpoints", bendpointsColumns
						,parentId
						,parentVersion
						,pos
						,bendpoint.getStartX()
						,bendpoint.getStartY()
						,bendpoint.getEndX()
						,bendpoint.getEndY()
						);
			}
		}
	}

	/**
	 * Export model's metadata to the database
	 * @param parent 
	 * @throws Exception 
	 */
	private void exportMetadata(DBArchimateModel parent) throws Exception {
		final String[] metadataColumns = {"parent_id", "parent_version", "pos", "name", "value"};

		if ( (parent.getMetadata() != null) && (parent.getMetadata().getEntries() != null) ) {
			logger.debug("   Exporting "+parent.getMetadata().getEntries().size()+" metadata");

			if ( parent.getMetadata() != null ) {        
				for ( int propPos = 0 ; propPos < parent.getMetadata().getEntries().size(); ++propPos) {
					IProperty prop = parent.getMetadata().getEntries().get(propPos);
					if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
						executeRequest("MATCH (parent {id:?, version:?}) CREATE (prop:metadata {pos:?, name:?, value:?}), (parent)-[:hasMetadata]->(prop)"
								,parent.getId()
								,parent.getCurrentVersion().getVersion()
								,propPos
								,prop.getKey()
								,prop.getValue()
								);
					}
					else
						insert(this.schemaPrefix+"metadata", metadataColumns
								,parent.getId()
								,parent.getCurrentVersion().getVersion()
								,propPos
								,prop.getKey()
								,prop.getValue()
								);
				}
			}
		}
	}

	/**
	 * Exports an image to the database
	 * @param path
	 * @param image
	 * @return
	 * @throws SQLException
	 */
	public boolean exportImage(String path, byte[] image) throws SQLException {
		// we do not export null images (should never happen, but it sometimes does)
		if ( image == null ) 
			return false;

		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT path FROM "+this.schemaPrefix+"images WHERE path = ?", path) ) {
			if ( !result.next() ) {
				// if the image is not yet in the db, we insert it
				String[] databaseColumns = {"path", "image"};
				insert(this.schemaPrefix+"images", databaseColumns, path, image);
				return true;
			}
		}
		return false;
	}

	/**
	 * @param connections
	 * @return
	 */
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
	 * @throws SQLException
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
	 * @throws SQLException
	 */
	@Override
	public void commit() throws SQLException {
		super.commit();
		this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
	}

	/**
	 * Reset the counters
	 */
	public void reset() {
		// We reset all "pos" to zero
		this.elementPos = 0;
		this.relationshipPos = 0;
		this.folderPos = 0;
		this.viewPos = 0;
		this.viewObjectPos = 0;
		this.viewConnectionPos = 0;

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
	 * @throws SQLException
	 */
	@Override
	public void close() throws SQLException {
		reset();

		if ( !this.isconnectionDuplicate )
			super.close();
	}
}