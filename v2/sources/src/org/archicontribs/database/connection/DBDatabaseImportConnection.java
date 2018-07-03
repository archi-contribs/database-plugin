/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBBendpoint;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.commands.DBImportElementFromIdCommand;
import org.archicontribs.database.model.commands.DBImportRelationshipFromIdCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import lombok.Getter;

/**
 * This class holds the information required to connect to, to import from and export to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseImportConnection extends DBDatabaseConnection {
	private static final DBLogger logger = new DBLogger(DBDatabaseImportConnection.class);

	private boolean isExportConnectionDuplicate = false;

	/**
	 * Opens a connection to a JDBC database using all the connection details
	 */
	public DBDatabaseImportConnection(DBDatabaseEntry dbEntry) throws ClassNotFoundException, SQLException {
		super(dbEntry);
	}

	/**
	 * duplicates a connection to a JDBC database to allow switching between importConnection and exportConnection
	 */
	public DBDatabaseImportConnection(DBDatabaseConnection databaseConnection) {
		super();
		assert(databaseConnection != null);
		super.databaseEntry = databaseConnection.databaseEntry;
		super.schema = databaseConnection.schema;
		super.connection = databaseConnection.connection;
		this.isExportConnectionDuplicate = true;
	}

	/**
	 * ResultSet of the current transaction (used by import process to allow the loop to be managed outside the DBdatabase class)
	 */
	private ResultSet currentResultSetElements = null;
	private ResultSet currentResultSetRelationships = null;
	private ResultSet currentResultSetFolders = null;
	private ResultSet currentResultSetViews = null;
	private ResultSet currentResultSetViewsObjects = null;
	private ResultSet currentResultSetViewsConnections = null;

	/**
	 * Gets a component from the database and convert the result into a HashMap<br>
	 * Mainly used in DBGui to compare a component to its database version.
	 * @param component: component to get
	 * @param version: version of the component to get (0 to get the latest version) 
	 * @return HashMap containing the object data
	 * @throws Exception
	 */
	public HashMap<String, Object> getObjectFromDatabase(EObject component, int version) throws Exception {
		String id = ((IIdentifier)component).getId();
		String clazz;
		if ( component instanceof IArchimateElement ) clazz = "IArchimateElement";
		else if ( component instanceof IArchimateRelationship ) clazz = "IArchimateRelationship";
		else if ( component instanceof IFolder ) clazz = "IFolder";
		else if ( component instanceof IDiagramModel ) clazz = "IDiagramModel";
		else if ( component instanceof IDiagramModelObject ) clazz = "IDiagramModelObject";
		else if ( component instanceof IDiagramModelConnection ) clazz = "IDiagramModelConnection";
		else throw new Exception("Do not know how to get a "+component.getClass().getSimpleName()+" from the database.");

		return getObject(id, clazz, version);
	}

	/**
	 * Gets a component from the database and convert the result into a HashMap<br>
	 * Mainly used in DBGui to compare a component to its database version.
	 * @param id: id of component to get
	 * @param clazz: class of component to get
	 * @param version: version of the component to get (0 to get the latest version) 
	 * @return HashMap containing the object data
	 * @throws Exception
	 */
	public HashMap<String, Object> getObject(String id, String clazz, int objectVersion) throws Exception {
		ResultSet result = null;
		int version = objectVersion;
		HashMap<String, Object> hashResult = null;

		if ( logger.isDebugEnabled() ) logger.debug("   Getting "+clazz);

		try {
			if ( version == 0 ) {
				// because of PostGreSQL, we need to split the request in two
				if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) result = select("SELECT id, version, class, name, documentation, type, created_by, created_on, checksum FROM "+this.schema+"elements e WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE id = e.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) result = select("SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM "+this.schema+"relationships r WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE id = r.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IFolder") ) result = select("SELECT id, version, 'Folder' as class, type, name, documentation, created_by, created_on, checksum FROM "+this.schema+"folders f WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"folders WHERE id = f.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) result = select("SELECT id, version, class, name, documentation, hint_content, hint_title, created_by, created_on, background, connection_router_type, viewpoint, checksum, container_checksum FROM "+this.schema+"views v WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"views WHERE id = v.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelObject") ) result = select("SELECT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, created_by, created_on, checksum FROM "+this.schema+"views_objects v WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"views_objects WHERE id = v.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) result = select("SELECT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, relationship_version, source_object_id, target_object_id, text_position, type, created_by, created_on, checksum FROM "+this.schema+"views_connections v WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"views_connections WHERE id = v.id)", id);
				else throw new Exception("Do not know how to get a "+clazz+" from the database.");
			} else {        
				if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) result = select("SELECT id, version, class, name, documentation, type, created_by, created_on, checksum FROM "+this.schema+"elements WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) result = select("SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM "+this.schema+"relationships WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IFolder") ) result = select("SELECT id, version, 'Folder' as class, type, name, documentation, created_by, created_on, checksum FROM "+this.schema+"folders WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) result = select("SELECT id, version, class, name, documentation, hint_content, hint_title, created_by, created_on, background, connection_router_type, viewpoint, checksum, container_checksum FROM "+this.schema+"views WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelObject") ) result = select("SELECT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, checksum FROM "+this.schema+"views_objects WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) result = select("SELECT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, relationship_version, source_object_id, target_object_id, text_position, type, created_by, created_on, checksum FROM "+this.schema+"views_connections v WHERE id = ? AND version = ?", id, version);
				else throw new Exception("Do not know how to get a "+clazz+" from the database.");
			}

			if ( result.next() ) {
				version = result.getInt("version");

				hashResult = resultSetToHashMap(result);

				// properties
				ArrayList<DBProperty> databaseProperties = new ArrayList<DBProperty>();
				try ( ResultSet resultProperties = select("SELECT name, value FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version) ) {
					while ( resultProperties.next() )
						databaseProperties.add(new DBProperty(resultProperties.getString("name"), resultProperties.getString("value")));
					hashResult.put("properties", databaseProperties);
				}



				// bendpoints
				if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) {
					ArrayList<DBBendpoint> databaseBendpoints = new ArrayList<DBBendpoint>();
					try ( ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version ) ) {
						while ( resultBendpoints.next() )
							databaseBendpoints.add(new DBBendpoint(resultBendpoints.getInt("start_x"), resultBendpoints.getInt("start_y"), resultBendpoints.getInt("end_x"), resultBendpoints.getInt("end_y")));
						hashResult.put("bendpoints", databaseBendpoints);
					}
				}
				
                logger.debug("   Found "+hashResult.get("class")+" \""+hashResult.get("name")+"\" version "+hashResult.get("version"));
			} else
				hashResult = new HashMap<String, Object>();
		} finally {
			if ( result != null ) {
				result.close();
				result = null;
			}
		}

		return hashResult;
	}

	/**
	 * Creates a HashMap from a ResultSet
	 */
	public static HashMap<String, Object> resultSetToHashMap(ResultSet rs) throws SQLException {
		HashMap<String, Object> map = new HashMap<String, Object>();

		for (int column = 1; column <= rs.getMetaData().getColumnCount(); column++) {
			if ( rs.getObject(column) != null ) {
				// we only listed the types that may be found by the database proxy and not the exhaustive types list
				String columnName = rs.getMetaData().getColumnName(column).toLowerCase();			// we need to convert to lowercase because of Oracle
				switch ( rs.getMetaData().getColumnType(column) ) {
					case Types.INTEGER :
					case Types.NUMERIC :
					case Types.SMALLINT :
					case Types.TINYINT :
					case Types.BIGINT :
					case Types.BOOLEAN :
					case Types.BIT :        map.put(columnName, rs.getInt(column));  break;

					case Types.TIMESTAMP :
					case Types.TIME :       map.put(columnName, rs.getTimestamp(column)); break;

					default :               map.put(columnName, rs.getString(column));
				}
			}
		}

		return map;
	}

	@Getter private HashSet<String> allImagePaths = new HashSet<String>();

	@Getter private int countElementsToImport = 0;
	@Getter private int countElementsImported = 0;
	@Getter private int countRelationshipsToImport = 0;
	@Getter private int countRelationshipsImported = 0;
	@Getter private int countFoldersToImport = 0;
	@Getter private int countFoldersImported = 0;
	@Getter private int countViewsToImport = 0;
	@Getter private int countViewsImported = 0;
	@Getter private int countViewObjectsToImport = 0;
	@Getter private int countViewObjectsImported = 0;
	@Getter private int countViewConnectionsToImport = 0;
	@Getter private int countViewConnectionsImported = 0;
	@Getter private int countImagesToImport = 0;
	@Getter private int countImagesImported = 0;

	private String importElementsRequest;
	private String importRelationshipsRequest;
	private String importFoldersRequest;
	private String importViewsRequest;

	/**
	 * Import the model metadata from the database
	 */
	public int importModel(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   importing model");
		// reseting the model's counters
		model.resetCounters();

		if ( model.getInitialVersion().getVersion() == 0 ) {
			try ( ResultSet result = select("SELECT MAX(version) AS version FROM "+this.schema+"models WHERE id = ?", model.getId()) ) {
				if ( result.next() )
					model.getInitialVersion().setVersion(result.getInt("version"));
			}
		}

		try ( ResultSet result = select("SELECT name, purpose, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND version = ?", model.getId(), model.getInitialVersion().getVersion()) ) {
			result.next();
			model.setPurpose(result.getString("purpose"));
			model.getInitialVersion().setChecksum(result.getString("checksum"));
			model.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
		}

		importProperties(model);
		importMetadata(model);

		String toCharDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation)" : "documentation";
		String toCharDocumentationAsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation) AS documentation" : "documentation";


		String versionToImport = model.isLatestVersionImported() ? "(SELECT MAX(version) FROM "+this.schema+"elements WHERE id = element_id)" : "element_version";
		this.importElementsRequest = "SELECT DISTINCT element_id, parent_folder_id, version, class, name, type, "+toCharDocumentationAsDocumentation+", created_on, checksum"
				+ " FROM "+this.schema+"elements_in_model"
				+ " JOIN "+this.schema+"elements ON elements.id = element_id AND version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?"
				+ " GROUP BY element_id, parent_folder_id, version, class, name, type, "+toCharDocumentation+", created_on, checksum, rank";
		try (ResultSet resultElements = select("SELECT COUNT(*) AS countElements FROM ("+this.importElementsRequest+") elts", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultElements.next();
			this.countElementsToImport = resultElements.getInt("countElements");
			this.countElementsImported = 0;
		}


		versionToImport = model.isLatestVersionImported() ? "(SELECT MAX(version) FROM "+this.schema+"relationships WHERE id = relationship_id)" : "relationship_version";
		this.importRelationshipsRequest = "SELECT DISTINCT relationship_id, parent_folder_id, version, class, name, "+toCharDocumentationAsDocumentation+", source_id, target_id, strength, access_type, created_on, checksum"
				+ " FROM "+this.schema+"relationships_in_model"
				+ " INNER JOIN "+this.schema+"relationships ON id = relationship_id AND version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?"
				+ " GROUP BY relationship_id, parent_folder_id, version, class, name, "+toCharDocumentation+", source_id, target_id, strength, access_type, created_on, checksum";
		try ( ResultSet resultRelationships = select("SELECT COUNT(*) AS countRelationships FROM ("+this.importRelationshipsRequest+") relts"
				,model.getId()
				,model.getInitialVersion().getVersion()
				) ) {
			resultRelationships.next();
			this.countRelationshipsToImport = resultRelationships.getInt("countRelationships");
			this.countRelationshipsImported = 0;
		}

		versionToImport = model.isLatestVersionImported() ? "(SELECT MAX(version) FROM "+this.schema+"folders WHERE folders.id = folders_in_model.folder_id)" : "folders_in_model.folder_version";
		String selectFoldersRequest = "SELECT DISTINCT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on, checksum, rank"
				+ " FROM "+this.schema+"folders_in_model"
				+ " JOIN "+this.schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( ResultSet resultFolders = select("SELECT COUNT(*) AS countFolders FROM ("+selectFoldersRequest+") fldrs", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultFolders.next();
			this.countFoldersToImport = resultFolders.getInt("countFolders");
			this.countFoldersImported = 0;
		}
		this.importFoldersRequest = selectFoldersRequest + " ORDER BY folders_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		versionToImport = model.isLatestVersionImported() ? "(select max(version) from "+this.schema+"views where views.id = views_in_model.view_id)" : "views_in_model.view_version";
		String selectViewsRequest = "SELECT DISTINCT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on, checksum, container_checksum, rank"
				+ " FROM "+this.schema+"views_in_model"
				+ " JOIN "+this.schema+"views ON views.id = views_in_model.view_id AND views.version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( ResultSet resultViews = select("SELECT COUNT(*) AS countViews FROM ("+selectViewsRequest+") vws", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViews.next();
			this.countViewsToImport = resultViews.getInt("countViews");
			this.countViewsImported = 0;
		}
		this.importViewsRequest = selectViewsRequest + " ORDER BY views_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		// versionToImport is same as for views
		String selectViewsObjectsRequest = "SELECT DISTINCT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, checksum"
				+ " FROM "+this.schema+"views_objects"
				+ " JOIN "+this.schema+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version"
				+ " JOIN "+this.schema+"views_in_model ON views_objects_in_view.view_id = views_in_model.view_id AND views_objects_in_view.view_version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( ResultSet resultViewObjects = select("SELECT COUNT(*) AS countViewsObjects FROM ("+selectViewsObjectsRequest+") vobjs", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViewObjects.next();
			this.countViewObjectsToImport = resultViewObjects.getInt("countViewsObjects");
			this.countViewObjectsImported = 0;
		}
		// (unused) this.importViewsObjectsRequest = this.selectViewsObjectsRequest + " ORDER BY views_objects.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		// versionToImport is same as for views
		String selectViewsConnectionsRequest = "SELECT DISTINCT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_object_id, target_object_id, text_position, type, checksum "
				+ " FROM "+this.schema+"views_connections"
				+ " JOIN "+this.schema+"views_connections_in_view ON views_connections_in_view.connection_id = views_connections.id AND views_connections_in_view.connection_version = views_connections.version"
				+ " JOIN "+this.schema+"views_in_model ON views_connections_in_view.view_id = views_in_model.view_id AND views_connections_in_view.view_version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( ResultSet resultViewConnections = select("SELECT COUNT(*) AS countViewsConnections FROM ("+selectViewsConnectionsRequest+") vcons", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViewConnections.next();
			this.countViewConnectionsToImport = resultViewConnections.getInt("countViewsConnections");
			this.countViewConnectionsImported = 0;
		}
		// (unused) this.importViewsConnectionsRequest = this.selectViewsConnectionsRequest + " ORDER BY views_connections.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		try ( ResultSet resultImages = select("SELECT COUNT(DISTINCT image_path) AS countImages"+
				" FROM "+this.schema+"views_in_model"+
				" INNER JOIN "+this.schema+"views ON views_in_model.view_id = views.id AND views_in_model.view_version = views.version"+
				" INNER JOIN "+this.schema+"views_objects_in_view ON views_objects_in_view.view_id = views.id AND views_objects_in_view.view_version = views.version"+
				" INNER JOIN "+this.schema+"views_objects ON views_objects.id = views_objects_in_view.object_id AND views_objects.version = views_objects_in_view.object_version"+
				" WHERE model_id = ? AND model_version = ? AND image_path IS NOT NULL" 
				,model.getId()
				,model.getInitialVersion().getVersion()
				))
		{
			resultImages.next();
			this.countImagesToImport = resultImages.getInt("countImages");
			this.countImagesImported = 0;
		}

		if ( logger.isDebugEnabled() ) logger.debug("Importing "+this.countElementsToImport+" elements, "+this.countRelationshipsToImport+" relationships, "+this.countFoldersToImport+" folders, "+this.countViewsToImport+" views, "+this.countViewObjectsToImport+" views objects, "+this.countViewConnectionsToImport+" views connections, and "+this.countImagesToImport+" images.");

		// initializing the HashMaps that will be used to reference imported objects
		this.allImagePaths.clear();

		return this.countElementsToImport + this.countRelationshipsToImport + this.countFoldersToImport + this.countViewsToImport + this.countViewObjectsToImport + this.countViewConnectionsToImport + this.countImagesToImport;
	}

	/**
	 * Prepare the import of the folders from the database
	 */
	public void prepareImportFolders(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import folders");
		this.currentResultSetFolders = select(this.importFoldersRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);
	}

	/**
	 * Import the folders from the database
	 */
	public boolean importFolders(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetFolders != null ) {
			if ( this.currentResultSetFolders.next() ) {
				IFolder folder = DBArchimateFactory.eINSTANCE.createFolder();
				DBMetadata metadata = ((IDBMetadata)folder).getDBMetadata();

				folder.setId(this.currentResultSetFolders.getString("folder_id"));

				metadata.getInitialVersion().setVersion(this.currentResultSetFolders.getInt("folder_version"));
				metadata.getInitialVersion().setChecksum(this.currentResultSetFolders.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(this.currentResultSetFolders.getTimestamp("created_on"));

				metadata.setName(this.currentResultSetFolders.getString("name"));
				metadata.setDocumentation(this.currentResultSetFolders.getString("documentation"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+folder.getClass().getSimpleName()+" \""+folder.getName()+"\" version "+((IDBMetadata)folder).getDBMetadata().getInitialVersion().getVersion());

				String parentId = this.currentResultSetFolders.getString("parent_folder_id");

				if ( parentId != null && !parentId.isEmpty() ) {
					metadata.setFolderType(FolderType.get(0));                              		// non root folders have got the "USER" type

					IFolder parent = model.getAllFolders().get(parentId);
					if ( parent == null )
						parent=model.getFolder(FolderType.get(this.currentResultSetFolders.getInt("root_type")));
					if ( parent == null ) 
						throw new Exception("Don't know where to create folder "+this.currentResultSetFolders.getString("name")+" of type "+this.currentResultSetFolders.getInt("type")+" and root_type "+this.currentResultSetFolders.getInt("root_type")+" (unknown folder ID "+this.currentResultSetFolders.getString("parent_folder_id")+")");

					parent.getFolders().add(folder);
				} else {
					metadata.setFolderType(FolderType.get(this.currentResultSetFolders.getInt("type")));        // root folders have got their own type
					model.getFolders().add(folder);
				}

				importProperties(folder);

				// we reference this folder for future use (storing sub-folders or components into it ...)
				model.countObject(folder, false, null);
				++this.countFoldersImported;
				return true;
			}
			this.currentResultSetFolders.close();
			this.currentResultSetFolders = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the elements from the database
	 */
	public void prepareImportElements(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import elements");
		this.currentResultSetElements = select(this.importElementsRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);

	}

	/**
	 * import the elements from the database
	 */
	public boolean importElements(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetElements != null ) {
			if ( this.currentResultSetElements.next() ) {
				IArchimateElement element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(this.currentResultSetElements.getString("class"));
				DBMetadata metadata = ((IDBMetadata)element).getDBMetadata();

				element.setId(this.currentResultSetElements.getString("element_id"));

				metadata.getInitialVersion().setVersion(this.currentResultSetElements.getInt("version"));
				metadata.getInitialVersion().setChecksum(this.currentResultSetElements.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(this.currentResultSetElements.getTimestamp("created_on"));

				metadata.setName(this.currentResultSetElements.getString("name"));
				metadata.setDocumentation(this.currentResultSetElements.getString("documentation"));
				metadata.setType(this.currentResultSetElements.getString("type"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+element.getClass().getSimpleName()+" \""+element.getName()+"\" version "+((IDBMetadata)element).getDBMetadata().getInitialVersion().getVersion());

				IFolder folder;
				if ( this.currentResultSetElements.getString("parent_folder_id") == null ) {
					folder = model.getDefaultFolderForObject(element);
				} else {
					folder = model.getAllFolders().get(this.currentResultSetElements.getString("parent_folder_id"));
				}
				folder.getElements().add(element);

				importProperties(element);

				// we reference the element for future use (establishing relationships, creating views objects, ...)
				model.countObject(element, false, null);
				++this.countElementsImported;
				return true;
			}
			this.currentResultSetElements.close();
			this.currentResultSetElements = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the relationships from the database
	 */
	public void prepareImportRelationships(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import relationships");
		this.currentResultSetRelationships = select(this.importRelationshipsRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);
	}

	/**
	 * import the relationships from the database
	 */
	public boolean importRelationships(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetRelationships != null ) {
			if ( this.currentResultSetRelationships.next() ) {
				IArchimateRelationship relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(this.currentResultSetRelationships.getString("class"));
				DBMetadata metadata = ((IDBMetadata)relationship).getDBMetadata();

				relationship.setId(this.currentResultSetRelationships.getString("relationship_id"));

				metadata.getInitialVersion().setVersion(this.currentResultSetRelationships.getInt("version"));
				metadata.getInitialVersion().setChecksum(this.currentResultSetRelationships.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(this.currentResultSetRelationships.getTimestamp("created_on"));

				metadata.setName(this.currentResultSetRelationships.getString("name")==null ? "" : this.currentResultSetRelationships.getString("name"));
				metadata.setDocumentation(this.currentResultSetRelationships.getString("documentation"));
				metadata.setStrength(this.currentResultSetRelationships.getString("strength"));
				metadata.setAccessType(this.currentResultSetRelationships.getInt("access_type"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+relationship.getClass().getSimpleName()+" \""+relationship.getName()+"\" version "+((IDBMetadata)relationship).getDBMetadata().getInitialVersion().getVersion());

				IFolder folder;
				if ( this.currentResultSetRelationships.getString("parent_folder_id") == null ) {
					folder = model.getDefaultFolderForObject(relationship);
				} else {
					folder = model.getAllFolders().get(this.currentResultSetRelationships.getString("parent_folder_id"));
				}
				folder.getElements().add(relationship);

				IArchimateConcept source = model.getAllElements().get(this.currentResultSetRelationships.getString("source_id"));
				IArchimateConcept target = model.getAllElements().get(this.currentResultSetRelationships.getString("target_id"));

				if ( source != null ) {
					// source is an element and is reputed already imported, so we can set it right away
					relationship.setSource(source);
					source.getSourceRelationships().add(relationship);
				} else {
					// source is another connection and may not be already loaded. So we register it for future resolution
					model.registerSourceRelationship(relationship, this.currentResultSetRelationships.getString("source_id"));
				}

				if ( target != null ) {
					// target is an element and is reputed already imported, so we can set it right away
					relationship.setTarget(target);
					target.getTargetRelationships().add(relationship);
				} else {
					// target is another connection and may not be already loaded. So we register it for future resolution
					model.registerTargetRelationship(relationship, this.currentResultSetRelationships.getString("target_id"));
				}

				importProperties(relationship);

				model.countObject(relationship, false, null);

				++this.countRelationshipsImported;
				return true;
			}
			this.currentResultSetRelationships.close();
			this.currentResultSetRelationships = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views from the database
	 */
	public void prepareImportViews(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import views");
		this.currentResultSetViews = select(this.importViewsRequest,
				model.getId(),
				model.getInitialVersion().getVersion()
				);
	}

	/**
	 * import the views from the database
	 */
	public boolean importViews(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetViews != null ) {
			if ( this.currentResultSetViews.next() ) {
				IDiagramModel view;
				if ( DBPlugin.areEqual(this.currentResultSetViews.getString("class"), "CanvasModel") )
					view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(this.currentResultSetViews.getString("class"));
				else
					view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(this.currentResultSetViews.getString("class"));

				DBMetadata metadata = ((IDBMetadata)view).getDBMetadata();

				view.setId(this.currentResultSetViews.getString("id"));

				metadata.getInitialVersion().setVersion(this.currentResultSetViews.getInt("version"));
				metadata.getInitialVersion().setChecksum(this.currentResultSetViews.getString("checksum"));
				metadata.getInitialVersion().setContainerChecksum(this.currentResultSetViews.getString("container_checksum"));
				metadata.getInitialVersion().setTimestamp(this.currentResultSetViews.getTimestamp("created_on"));

				metadata.setName(this.currentResultSetViews.getString("name"));
				metadata.setDocumentation(this.currentResultSetViews.getString("documentation"));
				metadata.setConnectionRouterType(this.currentResultSetViews.getInt("connection_router_type"));
				metadata.setViewpoint(this.currentResultSetViews.getString("viewpoint"));
				metadata.setBackground(this.currentResultSetViews.getInt("background"));
				metadata.setHintContent(this.currentResultSetViews.getString("hint_content"));
				metadata.setHintTitle(this.currentResultSetViews.getString("hint_title"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+view.getClass().getSimpleName()+" \""+view.getName()+"\" version "+((IDBMetadata)view).getDBMetadata().getInitialVersion().getVersion());

				model.getAllFolders().get(this.currentResultSetViews.getString("parent_folder_id")).getElements().add(view);

				importProperties(view);

				// we reference the view for future use
				model.countObject(view, false, null);
				++this.countViewsImported;
				return true;
			}
			this.currentResultSetViews.close();
			this.currentResultSetViews = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views objects of a specific view from the database
	 */
	public void prepareImportViewsObjects(String id, int version) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import views objects");
		this.currentResultSetViewsObjects = select("SELECT DISTINCT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, checksum, created_on, rank"
				+" FROM "+this.schema+"views_objects"
				+" JOIN "+this.schema+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version"
				+" WHERE view_id = ? AND view_version = ?"
				+" ORDER BY rank"
				,id
				,version
				);
	}

	/**
	 * import the views objects from the database
	 */
	public boolean importViewsObjects(DBArchimateModel model, IDiagramModel view) throws Exception {
		if ( this.currentResultSetViewsObjects != null ) {
			if ( this.currentResultSetViewsObjects.next() ) {
				EObject eObject;

				if ( this.currentResultSetViewsObjects.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(this.currentResultSetViewsObjects.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(this.currentResultSetViewsObjects.getString("class"));

				DBMetadata metadata = ((IDBMetadata)eObject).getDBMetadata();

				((IIdentifier)eObject).setId(this.currentResultSetViewsObjects.getString("id"));

				metadata.getInitialVersion().setVersion(this.currentResultSetViewsObjects.getInt("version"));
				metadata.getInitialVersion().setChecksum(this.currentResultSetViewsObjects.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(this.currentResultSetViewsObjects.getTimestamp("created_on"));

				if ( eObject instanceof IDiagramModelArchimateComponent && this.currentResultSetViewsObjects.getString("element_id") != null) {
					// we check that the element already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateElement element = model.getAllElements().get(this.currentResultSetViewsObjects.getString("element_id"));
					if ( element == null ) {
						DBImportElementFromIdCommand command = new DBImportElementFromIdCommand(this, model, null, null, this.currentResultSetViewsObjects.getString("element_id"), 0, false, true);
						((CommandStack)model.getAdapter(CommandStack.class)).execute(command);

						element = command.getImported();

						if ( command.getException() != null )
							throw command.getException();
					}
				}

				metadata.setArchimateConcept(model.getAllElements().get(this.currentResultSetViewsObjects.getString("element_id")));
				metadata.setReferencedModel(model.getAllViews().get(this.currentResultSetViewsObjects.getString("diagram_ref_id")));
				
				metadata.setBorderColor(this.currentResultSetViewsObjects.getString("border_color"));
				metadata.setBorderType(this.currentResultSetViewsObjects.getInt("border_type"));
				metadata.setContent(this.currentResultSetViewsObjects.getString("content"));
				metadata.setDocumentation(this.currentResultSetViewsObjects.getString("documentation"));
				metadata.setHintContent(this.currentResultSetViewsObjects.getString("hint_content"));
				metadata.setHintTitle(this.currentResultSetViewsObjects.getString("hint_title"));
				metadata.setLocked(this.currentResultSetViewsObjects.getObject("is_locked"));
				metadata.setImagePath(this.currentResultSetViewsObjects.getString("image_path"));
				metadata.setImagePosition(this.currentResultSetViewsObjects.getInt("image_position"));
				metadata.setLineColor(this.currentResultSetViewsObjects.getString("line_color"));
				metadata.setLineWidth(this.currentResultSetViewsObjects.getInt("line_width"));
				metadata.setFillColor(this.currentResultSetViewsObjects.getString("fill_color"));
				metadata.setFont(this.currentResultSetViewsObjects.getString("font"));
				metadata.setFontColor(this.currentResultSetViewsObjects.getString("font_color"));
				metadata.setName(this.currentResultSetViewsObjects.getString("name"));
				metadata.setNotes(this.currentResultSetViewsObjects.getString("notes"));
				metadata.setTextAlignment(this.currentResultSetViewsObjects.getInt("text_alignment"));
				metadata.setTextPosition(this.currentResultSetViewsObjects.getInt("text_position"));
				metadata.setType(this.currentResultSetViewsObjects.getInt("type"));
				metadata.setBounds(this.currentResultSetViewsObjects.getInt("x"), this.currentResultSetViewsObjects.getInt("y"), this.currentResultSetViewsObjects.getInt("width"), this.currentResultSetViewsObjects.getInt("height"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+eObject.getClass().getSimpleName()+" \""+((INameable)eObject).getName()+"\" version "+((IDBMetadata)eObject).getDBMetadata().getInitialVersion().getVersion());

				// The container is either the view, or a container in the view
				// if the container is not found, we create the object in the view as this is better than an NullPointerException
				IDiagramModelContainer container = (IDiagramModelContainer)model.getAllViewObjects().get(this.currentResultSetViewsObjects.getString("container_id"));
				if ( (container == null) || DBPlugin.areEqual(this.currentResultSetViewsObjects.getString("container_id"), view.getId()) )
					view.getChildren().add((IDiagramModelObject)eObject);
				else
					((IDiagramModelContainer)model.getAllViewObjects().get(this.currentResultSetViewsObjects.getString("container_id"))).getChildren().add((IDiagramModelObject)eObject);

				// If the object has got properties but does not have a linked element, then it may have distinct properties
				if ( eObject instanceof IProperties && this.currentResultSetViewsObjects.getString("element_id")==null ) {
					importProperties((IProperties)eObject);
				}

				// we reference the view for future use
				model.countObject(eObject, false, null);
				++this.countViewObjectsImported;

				// if the object contains an image, we store its path to import it later
				if ( this.currentResultSetViewsObjects.getString("image_path") != null )
					this.allImagePaths.add(this.currentResultSetViewsObjects.getString("image_path"));

				return true;
			}
			this.currentResultSetViewsObjects.close();
			this.currentResultSetViewsObjects = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views connections of a specific view from the database
	 */
	public void prepareImportViewsConnections(String id, int version) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import views connections");
		this.currentResultSetViewsConnections = select("SELECT DISTINCT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_object_id, target_object_id, text_position, type, checksum, rank"
				+" FROM "+this.schema+"views_connections"
				+" JOIN "+this.schema+"views_connections_in_view ON views_connections_in_view.connection_id = views_connections.id AND views_connections_in_view.connection_version = views_connections.version"
				+" WHERE view_id = ? AND view_version = ?"
				+" ORDER BY rank"
				,id
				,version
				);
	}

	/**
	 * import the views connections from the database
	 */
	public boolean importViewsConnections(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetViewsConnections != null ) {
			if ( this.currentResultSetViewsConnections.next() ) {
				EObject eObject;

				if ( this.currentResultSetViewsConnections.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(this.currentResultSetViewsConnections.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(this.currentResultSetViewsConnections.getString("class"));

				DBMetadata metadata = ((IDBMetadata)eObject).getDBMetadata();

				((IIdentifier)eObject).setId(this.currentResultSetViewsConnections.getString("id"));

				metadata.getInitialVersion().setVersion(this.currentResultSetViewsConnections.getInt("version"));
				metadata.getInitialVersion().setChecksum(this.currentResultSetViewsConnections.getString("checksum"));

				if ( eObject instanceof IDiagramModelArchimateConnection && this.currentResultSetViewsConnections.getString("relationship_id") != null) {
					// we check that the relationship already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateRelationship relationship = model.getAllRelationships().get(this.currentResultSetViewsConnections.getString("relationship_id"));
					if ( relationship == null ) {
						DBImportRelationshipFromIdCommand command = new DBImportRelationshipFromIdCommand(this, model, null, null, this.currentResultSetViewsConnections.getString("relationship_id"), 0, false);
						((CommandStack)model.getAdapter(CommandStack.class)).execute(command);

						relationship = command.getImported();

						if ( command.getException() != null )
							throw command.getException();
					}
				}

				metadata.setName(this.currentResultSetViewsConnections.getString("name"));
				metadata.setLocked(this.currentResultSetViewsConnections.getObject("is_locked"));
				metadata.setDocumentation(this.currentResultSetViewsConnections.getString("documentation"));
				metadata.setLineColor(this.currentResultSetViewsConnections.getString("line_color"));
				metadata.setLineWidth(this.currentResultSetViewsConnections.getInt("line_width"));
				metadata.setFont(this.currentResultSetViewsConnections.getString("font"));
				metadata.setFontColor(this.currentResultSetViewsConnections.getString("font_color"));
				metadata.setType(this.currentResultSetViewsConnections.getInt("type"));
				metadata.setTextPosition(this.currentResultSetViewsConnections.getInt("text_position"));
				metadata.setArchimateConcept(model.getAllRelationships().get(this.currentResultSetViewsConnections.getString("relationship_id")));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+eObject.getClass().getSimpleName()+" \""+((INameable)eObject).getName()+"\" version "+((IDBMetadata)eObject).getDBMetadata().getInitialVersion().getVersion());

				if ( eObject instanceof IDiagramModelConnection ) {
					IConnectable source = model.getAllViewObjects().get(this.currentResultSetViewsConnections.getString("source_object_id"));
					IConnectable target = model.getAllViewObjects().get(this.currentResultSetViewsConnections.getString("target_object_id"));

					if ( source != null ) {
						// source is an object and is reputed already imported, so we can set it right away
						((IDiagramModelConnection)eObject).setSource(source);
						source.addConnection((IDiagramModelConnection)eObject);
					} else {
						// source is another connection and may not be already loaded. So we register it for future resolution
						model.registerSourceConnection((IDiagramModelConnection)eObject, this.currentResultSetViewsConnections.getString("source_object_id"));
					}

					if ( target != null ) {
						// target is an object and is reputed already imported, so we can set it right away
						((IDiagramModelConnection)eObject).setTarget(target);
						target.addConnection((IDiagramModelConnection)eObject);
					} else {
						// target is another connection and may not be already loaded. So we register it for future resolution
						model.registerTargetConnection((IDiagramModelConnection)eObject, this.currentResultSetViewsConnections.getString("target_object_id"));
					}
				}

				if ( eObject instanceof IDiagramModelConnection ) {
					try ( ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY rank", ((IIdentifier)eObject).getId(), ((IDBMetadata)eObject).getDBMetadata().getInitialVersion().getVersion()) ) {
						while(resultBendpoints.next()) {
							IDiagramModelBendpoint bendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
							bendpoint.setStartX(resultBendpoints.getInt("start_x"));
							bendpoint.setStartY(resultBendpoints.getInt("start_y"));
							bendpoint.setEndX(resultBendpoints.getInt("end_x"));
							bendpoint.setEndY(resultBendpoints.getInt("end_y"));
							((IDiagramModelConnection)eObject).getBendpoints().add(bendpoint);
						}
					}
				}

				// we reference the connection for future use
				model.countObject(eObject, false, null);
				++this.countViewConnectionsImported;

				// If the connection has got properties but does not have a linked relationship, then it may have distinct properties
				if ( eObject instanceof IProperties && this.currentResultSetViewsConnections.getString("relationship_id")==null ) {
					importProperties((IProperties)eObject);
				}

				return true;
			}
			this.currentResultSetViewsConnections.close();
			this.currentResultSetViewsConnections = null;
		}
		return false;
	}

	/**
	 * import the views from the database
	 */
	public void importImage(DBArchimateModel model, String path) throws Exception {
		try ( ResultSet result = select("SELECT image FROM "+this.schema+"images WHERE path = ?", path) ) {
			if (result.next() ) {
				IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);
				try {
					String imagePath;
					byte[] imageContent = result.getBytes("image");

					if ( logger.isDebugEnabled() ) {
						if ( (imageContent.length/1024)/2014 > 1 )
							logger.debug( "Importing "+path+" ("+(imageContent.length/1024)/1024+" Mo)");
						else
							logger.debug( "Importing "+path+" ("+imageContent.length/1024+" Ko)");
					}
					imagePath = archiveMgr.addByteContentEntry(path, imageContent);

					if ( logger.isDebugEnabled() && !DBPlugin.areEqual(imagePath, path) )
						logger.debug( "... image imported but with new path "+imagePath);

				} catch (Exception e) {
					throw new Exception("Import of image failed !", e.getCause()!=null ? e.getCause() : e);
				}
				++this.countImagesImported;
			}
			else
				throw new Exception("Import of image failed : unkwnown image path "+path);
		} 
	}

	/**
	 * import an image from the database
	 */
	public Image getImageFromDatabase(String path) throws Exception {
		try ( ResultSet result = select("SELECT image FROM "+this.schema+"images WHERE path = ?", path) ) {
			if ( result.next() ) {
				byte[] imageContent = result.getBytes("image");
				if ( logger.isDebugEnabled() ) logger.debug( "Importing "+path+" with "+imageContent.length/1024+" Ko of data");
				return new Image(Display.getDefault(), new ImageData(new ByteArrayInputStream(imageContent)));
			}
		}
		return null;
	}

	/**
	 * gets the list of all images in the database
	 */
	public List<String> getImageListFromDatabase() throws Exception {
		List<String> list = new ArrayList<String>();
		try ( ResultSet result = select("SELECT path FROM "+this.schema+"images") ) {
			while ( result.next() ) {
				list.add(result.getString("path"));
			}
		}
		return list;
	}

	/**
	 * check if the number of imported images is equals to what is expected
	 */
	public void checkImportedImagesCount() throws Exception {
		if ( this.countImagesImported != this.countImagesToImport )
			throw new Exception(this.countImagesImported+" images imported instead of the "+this.countImagesToImport+" that were expected.");

		if ( logger.isDebugEnabled() ) logger.debug(this.countImagesImported+" images imported.");
	}

	/**
	 * Imports the properties of an Archi component<br>
	 * - missing properties are created
	 * - existing properties are updated with correct values if needed
	 * - existing properties with correct values are left untouched 
	 */
	public void importProperties(IProperties parent) throws Exception {
		int version;
		if ( parent instanceof IArchimateModel )
			version = ((DBArchimateModel)parent).getInitialVersion().getVersion();
		else
			version = ((IDBMetadata)parent).getDBMetadata().getInitialVersion().getVersion();

		importProperties(parent, ((IIdentifier)parent).getId(), version);
	}

	/**
	 * Imports the properties of an Archi component<br>
	 * - missing properties are created
	 * - existing properties are updated with correct values if needed
	 * - existing properties with correct values are left untouched 
	 * @throws SQLException 
	 */
	public void importProperties(IProperties parent, String id, int version) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("   importing properties");

		// first, we delete all existing properties
		parent.getProperties().clear();

		// then, we import the properties from the database 
		try ( ResultSet result = select("SELECT name, value FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY rank", id, version)) {
			while ( result.next() ) {
				// if the property already exist, we update its value. If it doesn't, we create it
				IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
				prop.setKey(result.getString("name"));
				prop.setValue(result.getString("value"));
				parent.getProperties().add(prop);
			}
		}
	}

	/**
	 * Imports the metadata of a model<br>
	 * @throws SQLException 
	 */
	private void importMetadata(DBArchimateModel model) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("   importing metadata");

		// first, we delete all existing metadata
		model.getMetadata().getEntries().clear();

		// then, we import the metadata from the database 
		try ( ResultSet result = select("SELECT name, value FROM "+this.schema+"metadata WHERE parent_id = ? AND parent_version = ? ORDER BY rank", model.getId(), model.getInitialVersion().getVersion())) {
			while ( result.next() ) {
				// if the property already exist, we update its value. If it doesn't, we create it
				IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
				prop.setKey(result.getString("name"));
				prop.setValue(result.getString("value"));
				model.getMetadata().getEntries().add(prop);
			}
		}
	}

	/**
	 * gets the latest model version in the database (0 if the model does not exist in the database)
	 */
	public int getLatestModelVersion(DBArchimateModel model) throws Exception {
		// we use COALESCE to guarantee that a value is returned, even if the model does not exist in the database
		try ( ResultSet result = select("SELECT COALESCE(MAX(version),0) as version FROM "+this.schema+"models WHERE id = ?", model.getId()) ) {
			result.next();
			return result.getInt("version");
		}
	}

	public void reset() throws SQLException {
		if ( this.currentResultSetElements != null ) {
			this.currentResultSetElements.close();
			this.currentResultSetElements = null;
		}
		if ( this.currentResultSetRelationships != null ) {
			this.currentResultSetRelationships.close();
			this.currentResultSetRelationships = null;
		}
		if ( this.currentResultSetFolders != null ) {
			this.currentResultSetFolders.close();
			this.currentResultSetFolders = null;
		}
		if ( this.currentResultSetViews != null ) {
			this.currentResultSetViews.close();
			this.currentResultSetViews = null;
		}
		if ( this.currentResultSetViewsObjects != null ) {
			this.currentResultSetViewsObjects.close();
			this.currentResultSetViewsObjects = null;
		}
		if ( this.currentResultSetViewsConnections != null ) {
			this.currentResultSetViewsConnections.close();
			this.currentResultSetViewsConnections = null;
		}

		// we reset the counters
		this.countElementsToImport = 0;
		this.countElementsImported = 0;
		this.countRelationshipsToImport = 0;
		this.countRelationshipsImported = 0;
		this.countFoldersToImport = 0;
		this.countFoldersImported = 0;
		this.countViewsToImport = 0;
		this.countViewsImported = 0;
		this.countViewObjectsToImport = 0;
		this.countViewObjectsImported = 0;
		this.countViewConnectionsToImport = 0;
		this.countViewConnectionsImported = 0;
		this.countImagesToImport = 0;
		this.countImagesImported = 0;

		// we empty the hashmap
		this.allImagePaths.clear();
	}

	@Override
	public void close() throws SQLException {
		reset();

		if ( !this.isExportConnectionDuplicate )
			super.close();
	}

	/**
	 * Gets the latest folder where the elementId was in the model
	 */
	public IFolder getLastKnownFolder(DBArchimateModel model, String clazz, String id) throws Exception {
		IFolder parentFolder = null;

		if ( !model.isLatestVersionImported() ) {
			String table;
			String column;
			if ( logger.isDebugEnabled() ) logger.debug("   getting last known folder");

			if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) { table = this.schema+"elements_in_model" ; column = "element_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) { table = this.schema+"relationships_in_model" ; column = "relationship_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IFolder") ) { table = this.schema+"folders_in_model" ; column = "folder_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) { table = this.schema+"views_in_model" ; column = "view_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModelObject") ) { table = this.schema+"views_ojects_in_model" ; column = "object_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) { table = this.schema+"views_connections_in_model" ; column = "connection_id"; }
			else throw new Exception("Do not know how to get a "+clazz+" from the database.");

			try ( ResultSet result = select("SELECT parent_folder_id, model_version"
					+" FROM "+table
					+" WHERE model_id = ? AND "+column+" = ? AND model_version = (SELECT MAX(model_version) FROM "+table+" WHERE model_id = ? AND "+column+" = ?)"
					, model.getId()
					, id
					, model.getId()
					, id
					) ) {
				if ( result.next() )
					parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
			}
		}

		return parentFolder;
	}

	/**
	 * Check if the component has already been part of the model once, and sets it to the folder it was in.<br>
	 * <br>
	 * if the component has never been part of the model, then is it set in the default folder for this component
	 * @param model
	 * @throws Exception
	 */
	public void setFolderToLastKnown(DBArchimateModel model, IFolder folder) throws Exception {
		if ( !model.isLatestVersionImported() ) {
			IFolder parentFolder = null;

			if ( logger.isDebugEnabled() ) logger.debug("   setting folder to last known");

			try ( ResultSet result = select("SELECT parent_folder_id, model_version"
					+ " FROM "+this.schema+"folders_in_model"
					+ " WHERE model_id = ? and folder_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.schema+"folders_in_model WHERE model_id = ? AND folder_id = ?)"
					, model.getId()
					, folder.getId()
					, model.getId()
					, folder.getId()
					) ) {
				if ( result.next() )
					parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
			}
			if (parentFolder == null )
				parentFolder = model.getFolder(((IDBMetadata)folder).getDBMetadata().getFolderType());

			((IDBMetadata)folder).getDBMetadata().setParentFolder(parentFolder);
		}


	}

	/**
	 * Check if the component has already been part of the model once, and sets it to the folder it was in.<br>
	 * <br>
	 * if the component has never been part of the model, then is it set in the default folder for this component
	 * @param model
	 * @throws Exception
	 */
	public void setFolderToLastKnown(DBArchimateModel model, IDiagramModel view) throws Exception {
		if ( !model.isLatestVersionImported() ) {
			IFolder parentFolder = null;

			if ( logger.isDebugEnabled() ) logger.debug("   setting folder to last known");

			try ( ResultSet result = select("SELECT parent_folder_id, model_version"
					+ " FROM "+this.schema+"views_in_model"
					+ " WHERE model_id = ? and view_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.schema+"views_in_model WHERE model_id = ? AND view_id = ?)"
					, model.getId()
					, view.getId()
					, model.getId()
					, view.getId()
					) ) {
				if ( result.next() )
					parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
			}

			if (parentFolder == null )
				parentFolder = model.getDefaultFolderForObject(view);

			((IDBMetadata)view).getDBMetadata().setParentFolder(parentFolder);
		}
	}
}