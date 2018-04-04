/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
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

/**
 * This class holds the information required to connect to, to import from and export to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseImportConnection extends DBDatabaseConnection {
	private static final DBLogger logger = new DBLogger(DBDatabaseImportConnection.class);

	/**
	 * Opens a connection to a JDBC database using all the connection details
	 */
	public DBDatabaseImportConnection(DBDatabaseEntry databaseEntry) throws ClassNotFoundException, SQLException {
	    super(databaseEntry);
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
    }

	/**
	 * ResultSet of the current transaction (used by import process to allow the loop to be managed outside the DBdatabase class)
	 */
	private ResultSet currentResultSet = null;

	/**
	 * Gets a component from the database and convert the result into a HashMap<br>
	 * Mainly used in DBGuiExportModel to compare a component to its database version.
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
		else if ( component instanceof IDiagramModelArchimateObject	 ) clazz = "IDiagramModelArchimateObject";		
		else throw new Exception("Do not know how to get a "+component.getClass().getSimpleName()+" from the database.");

		return getObject(id, clazz, version);
	}
	
	/**
	 * Gets a component from the database and convert the result into a HashMap<br>
	 * Mainly used in DBGuiExportModel to compare a component to its database version.
	 * @param id: id of component to get
	 * @param clazz: class of component to get
	 * @param version: version of the component to get (0 to get the latest version) 
	 * @return HashMap containing the object data
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public HashMap<String, Object> getObject(String id, String clazz, int objectVersion) throws Exception {
		ResultSet result = null;
		int version = objectVersion;
		HashMap<String, Object> hashResult = null;
		
		try {
			if ( version == 0 ) {
				// because of PostGreSQL, we need to split the request in two
				if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) result = select("SELECT id, version, class, name, documentation, type, created_by, created_on, checksum FROM "+this.schema+"elements e WHERE version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE id = e.id) AND id = ?", id);
				else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) result = select("SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM "+this.schema+"relationships r WHERE version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE id = r.id) AND id = ?", id);
				else if ( DBPlugin.areEqual(clazz,  "IFolder") ) result = select("SELECT id, version, type, name, documentation, created_by, created_on, checksum FROM folders f WHERE version = (SELECT MAX(version) FROM "+this.schema+"folders WHERE id = f.id) AND id = ?", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) result = select("SELECT id, version, class, name, documentation, hint_content, hint_title, created_by, created_on, background, connection_router_type, viewpoint, checksum FROM "+this.schema+"views v WHERE version = (SELECT MAX(version) FROM "+this.schema+"views WHERE id = v.id) AND id = ?", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelArchimateObject") ) result = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height, checksum FROM "+this.schema+"views_objects v WHERE version = (SELECT MAX(version) FROM "+this.schema+"views_objects WHERE id = v.id) AND id = ?", id);		
				else throw new Exception("Do not know how to get a "+clazz+" from the database.");
			} else {        
				if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) result = select("SELECT id, version, class, name, documentation, type, created_by, created_on, checksum FROM "+this.schema+"elements WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) result = select("SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM "+this.schema+"relationships WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IFolder") ) result = select("SELECT id, version, type, name, documentation, created_by, created_on, checksum FROM "+this.schema+"folders WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) result = select("SELECT id, version, class, name, documentation, hint_content, hint_title, created_by, created_on, background, connection_router_type, viewpoint, checksum FROM "+this.schema+"views WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelArchimateObject") ) result = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height, checksum FROM "+this.schema+"views_objects WHERE id = ? AND version = ?", id, version);
				else throw new Exception("Do not know how to get a "+clazz+" from the database.");
			}
	
			if ( result.next() ) {
				version = result.getInt("version");
				
				hashResult = resultSetToHashMap(result);
				if ( DBPlugin.areEqual(clazz,  "IFolder") ) hashResult.put("class", "Folder");                  // the folders table does not have a class column, so we add the property by hand
	
				// properties
				String[][] databaseProperties;
				try ( ResultSet resultProperties = select("SELECT count(*) as count_properties FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ?", id, version) ) {
					resultProperties.next();
					databaseProperties = new String[resultProperties.getInt("count_properties")][2];
				}
		
				try ( ResultSet resultProperties = select("SELECT name, value FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version) ) {
					int i = 0;
					while ( resultProperties.next() ) {
						databaseProperties[i++] = new String[] { resultProperties.getString("name"), result.getString("value") };
					}
					hashResult.put("properties", databaseProperties);
				}
				
				// gather the views objects if we are getting a view
				if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) {
					int countChildren;
					
					try ( ResultSet resultChildren = select("select count(*) AS count_children FROM "+this.schema+"views_objects WHERE view_id = ? AND view_version = ?", id, version) ) {
						resultChildren.next();
						countChildren = resultChildren.getInt("count_children");
					}
					
					@SuppressWarnings("unchecked")
					HashMap<String, Object>[] children = new HashMap[countChildren];
					try ( ResultSet resultChildren = select("select id, version FROM "+this.schema+"views_objects WHERE view_id = ? AND view_version = ?", id, version) ) {
						int i=0;
						while ( resultChildren.next() ) {
							children[i++] = getObject(resultChildren.getString("id"), "IDiagramModelArchimateObject", result.getInt("version"));
						}
						resultChildren.close();
						hashResult.put("children", children);
					}
				}
				
				// bendpoints
				Integer[][] databaseBendpoints;
				try ( ResultSet resultBendpoints = select("SELECT count(*) as count_bendpoints FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ?", id, version) ) {
					result.next();
					databaseBendpoints = new Integer[resultBendpoints.getInt("count_bendpoints")][4];
				}
				
				try ( ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version ) ) {
					int j = 0;
					while ( result.next() ) {
						databaseBendpoints[j++] = new Integer[] { resultBendpoints.getInt("start_x"), resultBendpoints.getInt("start_y"), resultBendpoints.getInt("end_x"), resultBendpoints.getInt("end_y") };
					}
					hashResult.put("bendpoints", databaseBendpoints);
				}
			} else
				hashResult = new HashMap<String, Object>();
		} finally {
			if ( result != null )
				result.close();
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

	private HashSet<String> allImagePaths;

	private int countElementsToImport = 0;
	public int countElementsToImport() { return this.countElementsToImport; }

	private int countElementsImported = 0;
	public int countElementsImported() { return this.countElementsImported; }

	private int countRelationshipsToImport = 0;
	public int countRelationshipsToImport() { return this.countRelationshipsToImport; }

	private int countRelationshipsImported = 0;
	public int countRelationshipsImported() { return this.countRelationshipsImported; }

	private int countFoldersToImport = 0;
	public int countFoldersToImport() { return this.countFoldersToImport; }

	private int countFoldersImported = 0;
	public int countFoldersImported() { return this.countFoldersImported; }

	private int countViewsToImport = 0;
	public int countViewsToImport() { return this.countViewsToImport; }

	private int countViewsImported = 0;
	public int countViewsImported() { return this.countViewsImported; }

	private int countViewObjectsToImport = 0;
	public int countViewObjectsToImport() { return this.countViewObjectsToImport; }

	private int countViewObjectsImported = 0;
	public int countViewObjectsImported() { return this.countViewObjectsImported; }

	private int countViewConnectionsToImport = 0;
	public int countViewConnectionsToImport() { return this.countViewConnectionsToImport; }

	private int countViewConnectionsImported = 0;
	public int countViewConnectionsImported() { return this.countViewConnectionsImported; }

	private int countImagesToImport = 0;
	public int countImagesToImport() { return this.countImagesToImport; }

	private int countImagesImported = 0;
	public int countImagesImported() { return this.countImagesImported; }
	
	private String importFoldersRequest;
	private String importElementsRequest;
	private String importRelationshipsRequest;
	private String importViewsRequest;
	private String importViewsObjectsRequest;
	private String importViewsConnectionsRequest;

	/**
	 * Import the model metadata from the database
	 */
	public int importModelMetadata(DBArchimateModel model) throws Exception {
		// reseting the model's counters
		model.resetCounters();

		if ( model.getInitialVersion().getVersion() == 0 ) {
		    try ( ResultSet result = select("SELECT MAX(version) FROM "+this.schema+"models WHERE id = ?", model.getId()) ) {
			    if ( result.next() ) {
			        model.getInitialVersion().setVersion(result.getInt("version"));
			    }
		    }
		}

		//TODO : manage the "real" model metadata :-)
		try ( ResultSet result = select("SELECT name, purpose, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND version = ?", model.getId(), model.getInitialVersion().getVersion()) ) {
			result.next();
			model.setPurpose(result.getString("purpose"));
	        model.getInitialVersion().setChecksum(result.getString("checksum"));
	        model.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
		}

		importProperties(model);

		String toCharDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ? "TO_CHAR(documentation)" : "documentation";
		String toCharDocumentationAsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ? "TO_CHAR(documentation) AS documentation" : "documentation";
		
		if ( model.getImportLatestVersion() ) {
			this.importElementsRequest = "SELECT DISTINCT element_id, parent_folder_id, version, class, name, type, "+toCharDocumentationAsDocumentation+", created_on, checksum"+
					" FROM "+this.schema+"elements_in_model"+
					" JOIN "+this.schema+"elements ON elements.id = element_id AND version = (SELECT MAX(version) FROM elements WHERE id = element_id)"+
					" WHERE model_id = ? AND model_version = ?"+
					" GROUP BY element_id, parent_folder_id, version, class, name, type, "+toCharDocumentation+", created_on";
		} else {
			this.importElementsRequest = "SELECT DISTINCT element_id, parent_folder_id, version, class, name, type, "+toCharDocumentationAsDocumentation+", created_on, checksum"+
									" FROM "+this.schema+"elements_in_model"+
									" JOIN "+this.schema+"elements ON id = element_id AND version = element_version"+
									" WHERE model_id = ? AND model_version = ?"+
									" GROUP BY element_id, parent_folder_id, version, class, name, type, "+toCharDocumentation+", created_on";
		}
		try (ResultSet resultElements = select("SELECT COUNT(*) AS countElements FROM ("+this.importElementsRequest+") elts", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultElements.next();
			this.countElementsToImport = resultElements.getInt("countElements");
			this.countElementsImported = 0;
		}

		if ( model.getImportLatestVersion() ) {
			this.importRelationshipsRequest = "SELECT relationship_id, parent_folder_id, version, class, name, "+toCharDocumentationAsDocumentation+", source_id, target_id, strength, access_type, created_on, checksum"+
					 " FROM "+this.schema+"relationships_in_model"+
					 " INNER JOIN "+this.schema+"relationships ON id = relationship_id AND version = (SELECT MAX(version) FROM relationships WHERE id = relationship_id)"+
					 " WHERE model_id = ? AND model_version = ?"+
					 " GROUP BY relationship_id, parent_folder_id, version, class, name, "+toCharDocumentation+", source_id, target_id, strength, access_type, created_on";
		} else {
			this.importRelationshipsRequest = "SELECT relationship_id, parent_folder_id, version, class, name, "+toCharDocumentationAsDocumentation+", source_id, target_id, strength, access_type, created_on, checksum"+
										 " FROM "+this.schema+"relationships_in_model"+
										 " INNER JOIN "+this.schema+"relationships ON id = relationship_id AND version = relationship_version"+
										 " WHERE model_id = ? AND model_version = ?"+
										 " GROUP BY relationship_id, parent_folder_id, version, class, name, "+toCharDocumentation+", source_id, target_id, strength, access_type, created_on";
		}
		try ( ResultSet resultRelationships = select("SELECT COUNT(*) AS countRelationships FROM ("+this.importRelationshipsRequest+") relts"
				,model.getId()
				,model.getInitialVersion().getVersion()
				) ) {
			resultRelationships.next();
			this.countRelationshipsToImport = resultRelationships.getInt("countRelationships");
			this.countRelationshipsImported = 0;
		}
		
		if ( model.getImportLatestVersion() ) {
			this.importFoldersRequest = "SELECT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on, checksum"+
									" FROM "+this.schema+"folders_in_model"+
									" JOIN "+this.schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = (SELECT MAX(version) FROM "+this.schema+"folders WHERE folders.id = folders_in_model.folder_id)"+
									" WHERE model_id = ? AND model_version = ?";
		} else {
			this.importFoldersRequest = "SELECT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on, checksum"+
									" FROM "+this.schema+"folders_in_model"+
									" JOIN "+this.schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = folders_in_model.folder_version"+
									" WHERE model_id = ? AND model_version = ?";
		}
		try ( ResultSet resultFolders = select("SELECT COUNT(*) AS countFolders FROM ("+this.importFoldersRequest+") fldrs", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultFolders.next();
			this.countFoldersToImport = resultFolders.getInt("countFolders");
			this.countFoldersImported = 0;
		}

		this.importFoldersRequest += " ORDER BY folders_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			this.importViewsRequest = "SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on, checksum"+
								 " FROM "+this.schema+"views_in_model"+
								 " JOIN "+this.schema+"views ON views.id = views_in_model.view_id AND views.version = (select max(version) from "+this.schema+"views where views.id = views_in_model.view_id)"+
								 " WHERE model_id = ? AND model_version = ?";
		} else {
			this.importViewsRequest = "SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on, checksum"+
								 " FROM "+this.schema+"views_in_model"+
								 " JOIN "+this.schema+"views ON views.id = views_in_model.view_id AND views.version = views_in_model.view_version"+
								 " WHERE model_id = ? AND model_version = ?";
		}
		try ( ResultSet resultViews = select("SELECT COUNT(*) AS countViews FROM ("+this.importViewsRequest+") vws", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViews.next();
			this.countViewsToImport = resultViews.getInt("countViews");
			this.countViewsImported = 0;
		}

		this.importViewsRequest += " ORDER BY views_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			this.importViewsObjectsRequest = "SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height"+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_objects ON views_objects.view_id = views_in_model.view_id AND views_objects.view_version = (SELECT MAX(version) FROM "+this.schema+"views WHERE id = views_in_model.view_id)"+
										" WHERE model_id = ? AND model_version = ?";
		} else {
			this.importViewsObjectsRequest = "SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height"+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_objects ON views_objects.view_id = views_in_model.view_id AND views_objects.view_version = views_in_model.view_version"+
										" WHERE model_id = ? AND model_version = ?";
		}
		try ( ResultSet resultViewObjects = select("SELECT COUNT(*) AS countViewsObjects FROM ("+this.importViewsObjectsRequest+") vobjs", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViewObjects.next();
			this.countViewObjectsToImport = resultViewObjects.getInt("countViewsObjects");
			this.countViewObjectsImported = 0;
		}
		
		this.importViewsObjectsRequest += " ORDER BY views_objects.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			this.importViewsConnectionsRequest = "SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type "+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_connections ON views_connections.view_id = views_in_model.view_id AND views_connections.view_version = (SELECT MAX(version) FROM "+this.schema+"views WHERE id = views_in_model.view_id)"+
										" WHERE model_id = ? AND model_version = ?";
		} else {
			this.importViewsConnectionsRequest = "SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type"+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_connections ON views_connections.view_id = views_in_model.view_id AND views_connections.view_version = views_in_model.view_version"+
										" WHERE model_id = ? AND model_version = ?";
		}
		try ( ResultSet resultViewConnections = select("SELECT COUNT(*) AS countViewsConnections FROM ("+this.importViewsConnectionsRequest+") vcons", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViewConnections.next();
			this.countViewConnectionsToImport = resultViewConnections.getInt("countViewsConnections");
			this.countViewConnectionsImported = 0;
		}
		
		this.importViewsConnectionsRequest += " ORDER BY views_connections.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		try ( ResultSet resultImages = select("SELECT COUNT(DISTINCT image_path) AS countImages FROM "+this.schema+"views_in_model"+
						" INNER JOIN "+this.schema+"views ON "+this.schema+"views_in_model.view_id = views.id AND "+this.schema+"views_in_model.view_version = views.version"+
						" INNER JOIN "+this.schema+"views_objects ON views.id = "+this.schema+"views_objects.view_id AND views.version = "+this.schema+"views_objects.version"+
						" INNER JOIN "+this.schema+"images ON "+this.schema+"views_objects.image_path = images.path"+
						" WHERE model_id = ? AND model_version = ? AND path IS NOT NULL" 
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
		this.allImagePaths = new HashSet<String>();

		return this.countElementsToImport + this.countRelationshipsToImport + this.countFoldersToImport + this.countViewsToImport + this.countViewObjectsToImport + this.countViewConnectionsToImport + this.countImagesToImport;
	}

	/**
	 * Prepare the import of the folders from the database
	 */
	public void prepareImportFolders(DBArchimateModel model) throws Exception {
		this.currentResultSet = select(this.importFoldersRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);
	}

	/**
	 * Import the folders from the database
	 */
	public boolean importFolders(DBArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IFolder folder = DBArchimateFactory.eINSTANCE.createFolder();

				folder.setId(this.currentResultSet.getString("folder_id"));
				((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("folder_version"));
				((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setChecksum(this.currentResultSet.getString("checksum"));
				((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setTimestamp(this.currentResultSet.getTimestamp("created_on"));

				folder.setName(this.currentResultSet.getString("name")==null ? "" : this.currentResultSet.getString("name"));
				folder.setDocumentation(this.currentResultSet.getString("documentation"));

				String parentId = this.currentResultSet.getString("parent_folder_id");

				if ( parentId != null && !parentId.isEmpty() ) {
					folder.setType(FolderType.get(0));                              		// non root folders have got the "USER" type
					
					IFolder parent = model.getAllFolders().get(parentId);
					if ( parent == null )
						parent=model.getFolder(FolderType.get(this.currentResultSet.getInt("root_type")));
					if ( parent == null ) 
						throw new Exception("Don't know where to create folder "+this.currentResultSet.getString("name")+" of type "+this.currentResultSet.getInt("type")+" and root_type "+this.currentResultSet.getInt("root_type")+" (unknown folder ID "+this.currentResultSet.getString("parent_folder_id")+")");

					parent.getFolders().add(folder);
				} else {
					folder.setType(FolderType.get(this.currentResultSet.getInt("type")));        // root folders have got their own type
					model.getFolders().add(folder);
				}

				importProperties(folder);
				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()+" of folder "+this.currentResultSet.getString("name")+"("+this.currentResultSet.getString("folder_id")+")");

				// we reference this folder for future use (storing sub-folders or components into it ...)
				model.countObject(folder, false, null);
				++this.countFoldersImported;
				return true;
			}
			this.currentResultSet.close();
			this.currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the elements from the database
	 */
	// it is complex because we need to retrieve the elements that have been added in views by other models
	public void prepareImportElements(DBArchimateModel model) throws Exception {
//		if ( model.getImportLatestVersion() ) {
//			this.currentResultSet = select(this.importElementsRequest
//					,model.getId()
//					,model.getCurrentVersion().getVersion()
//					,model.getId()
//					,model.getCurrentVersion().getVersion()
//					,model.getId()
//					,model.getCurrentVersion().getVersion()
//					);
//		} else {
			this.currentResultSet = select(this.importElementsRequest
					,model.getId()
					,model.getInitialVersion().getVersion()
					);
//		}
			
	}

	/**
	 * import the elements from the database
	 */
	public boolean importElements(DBArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IArchimateElement element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				element.setId(this.currentResultSet.getString("element_id"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setChecksum(this.currentResultSet.getString("checksum"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(this.currentResultSet.getTimestamp("created_on"));

				element.setName(this.currentResultSet.getString("name")==null ? "" : this.currentResultSet.getString("name"));
				element.setDocumentation(this.currentResultSet.getString("documentation"));
				if ( element instanceof IJunction   && this.currentResultSet.getObject("type")!=null )  ((IJunction)element).setType(this.currentResultSet.getString("type"));

				IFolder folder;
				if ( this.currentResultSet.getString("parent_folder_id") == null ) {
					folder = model.getDefaultFolderForObject(element);
				} else {
					folder = model.getAllFolders().get(this.currentResultSet.getString("parent_folder_id"));
				}
				folder.getElements().add(element);

				importProperties(element);

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()+" of "+this.currentResultSet.getString("class")+":"+this.currentResultSet.getString("name")+"("+this.currentResultSet.getString("element_id")+")");

				// we reference the element for future use (establishing relationships, creating views objects, ...)
				model.countObject(element, false, null);
				++this.countElementsImported;
				return true;
			}
			this.currentResultSet.close();
			this.currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the relationships from the database
	 */
	public void prepareImportRelationships(DBArchimateModel model) throws Exception {
//		if ( model.getImportLatestVersion() ) {
//			this.currentResultSet = select(this.importRelationshipsRequest
//					,model.getId()
//					,model.getCurrentVersion().getVersion()
//					,model.getId()
//					,model.getCurrentVersion().getVersion()
//					,model.getId()
//					,model.getCurrentVersion().getVersion()
//					);
//		} else {
			this.currentResultSet = select(this.importRelationshipsRequest
					,model.getId()
					,model.getInitialVersion().getVersion()
					);
//		}
	}

	/**
	 * import the relationships from the database
	 */
	public boolean importRelationships(DBArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IArchimateRelationship relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				relationship.setId(this.currentResultSet.getString("relationship_id"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setChecksum(this.currentResultSet.getString("checksum"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setTimestamp(this.currentResultSet.getTimestamp("created_on"));

				relationship.setName(this.currentResultSet.getString("name")==null ? "" : this.currentResultSet.getString("name"));
				relationship.setDocumentation(this.currentResultSet.getString("documentation"));

				if ( relationship instanceof IInfluenceRelationship && this.currentResultSet.getObject("strength")!=null )      ((IInfluenceRelationship)relationship).setStrength(this.currentResultSet.getString("strength"));
				if ( relationship instanceof IAccessRelationship    && this.currentResultSet.getObject("access_type")!=null )   ((IAccessRelationship)relationship).setAccessType(this.currentResultSet.getInt("access_type"));

				IFolder folder;
				if ( this.currentResultSet.getString("parent_folder_id") == null ) {
					folder = model.getDefaultFolderForObject(relationship);
				} else {
					folder = model.getAllFolders().get(this.currentResultSet.getString("parent_folder_id"));
				}
				folder.getElements().add(relationship);

				importProperties(relationship);

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()+" of "+this.currentResultSet.getString("class")+":"+this.currentResultSet.getString("name")+"("+this.currentResultSet.getString("relationship_id")+")");

				// we reference the relationship for future use (establishing relationships, creating views connections, ...)
				model.countObject(relationship, false, null);
				model.registerSourceAndTarget(relationship, this.currentResultSet.getString("source_id"), this.currentResultSet.getString("target_id"));
				++this.countRelationshipsImported;
				return true;
			}
			this.currentResultSet.close();
			this.currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views from the database
	 */
	public void prepareImportViews(DBArchimateModel model) throws Exception {
		this.currentResultSet = select(this.importViewsRequest,
				model.getId(),
				model.getInitialVersion().getVersion()
				);
	}

	/**
	 * import the views from the database
	 */
	public boolean importViews(DBArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IDiagramModel view;
				if ( DBPlugin.areEqual(this.currentResultSet.getString("class"), "CanvasModel") )
					view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				else
					view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));

				view.setId(this.currentResultSet.getString("id"));
				((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));
				((IDBMetadata)view).getDBMetadata().getCurrentVersion().setChecksum(this.currentResultSet.getString("checksum"));
				((IDBMetadata)view).getDBMetadata().getCurrentVersion().setTimestamp(this.currentResultSet.getTimestamp("created_on"));

				view.setName(this.currentResultSet.getString("name")==null ? "" : this.currentResultSet.getString("name"));
				view.setDocumentation(this.currentResultSet.getString("documentation"));
				view.setConnectionRouterType(this.currentResultSet.getInt("connection_router_type"));
				if ( view instanceof IArchimateDiagramModel && this.currentResultSet.getObject("viewpoint")!=null )     ((IArchimateDiagramModel) view).setViewpoint(this.currentResultSet.getString("viewpoint"));
				if ( view instanceof ISketchModel           && this.currentResultSet.getObject("background")!=null )    ((ISketchModel)view).setBackground(this.currentResultSet.getInt("background"));
				if ( view instanceof IHintProvider          && this.currentResultSet.getObject("hint_content")!=null )  ((IHintProvider)view).setHintContent(this.currentResultSet.getString("hint_content"));
				if ( view instanceof IHintProvider          && this.currentResultSet.getObject("hint_title")!=null )    ((IHintProvider)view).setHintTitle(this.currentResultSet.getString("hint_title"));

				model.getAllFolders().get(this.currentResultSet.getString("parent_folder_id")).getElements().add(view);

				importProperties(view);

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()+" of "+this.currentResultSet.getString("name")+"("+this.currentResultSet.getString("id")+")");

				// we reference the view for future use
				model.countObject(view, false, null);
				++this.countViewsImported;
				return true;
			}
			this.currentResultSet.close();
			this.currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views objects of a specific view from the database
	 */
	public void prepareImportViewsObjects(String viewId, int version) throws Exception {
		this.currentResultSet = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height FROM "+this.schema+"views_objects WHERE view_id = ? AND view_version = ? ORDER BY rank"
				,viewId
				,version
				);
	}

	/**
	 * import the views objects from the database
	 */
	public boolean importViewsObjects(DBArchimateModel model, IDiagramModel view) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				EObject eObject;

				if ( this.currentResultSet.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));

				((IIdentifier)eObject).setId(this.currentResultSet.getString("id"));
				((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));

				if ( eObject instanceof IDiagramModelArchimateComponent && this.currentResultSet.getString("element_id") != null) {
					// we check that the element already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateElement element = model.getAllElements().get(this.currentResultSet.getString("element_id"));
					if ( element == null ) {
						if (logger.isTraceEnabled() ) logger.trace("importing individual element ...");
						importElementFromId(model, null, this.currentResultSet.getString("element_id"), 0, false);
					}
				}

				if ( eObject instanceof IDiagramModelArchimateComponent && this.currentResultSet.getObject("element_id")!=null )                            ((IDiagramModelArchimateComponent)eObject).setArchimateConcept(model.getAllElements().get(this.currentResultSet.getString("element_id")));
				if ( eObject instanceof IDiagramModelReference          && this.currentResultSet.getObject("diagram_ref_id")!=null )                        ((IDiagramModelReference)eObject).setReferencedModel(model.getAllViews().get(this.currentResultSet.getString("diagram_ref_id")));
				if ( eObject instanceof IDiagramModelArchimateObject    && this.currentResultSet.getObject("type")!=null )                                  ((IDiagramModelArchimateObject)eObject).setType(this.currentResultSet.getInt("type"));
				if ( eObject instanceof IBorderObject                   && this.currentResultSet.getObject("border_color")!=null )                          ((IBorderObject)eObject).setBorderColor(this.currentResultSet.getString("border_color"));
				if ( eObject instanceof IDiagramModelNote               && this.currentResultSet.getObject("border_type")!=null )                           ((IDiagramModelNote)eObject).setBorderType(this.currentResultSet.getInt("border_type"));
				if ( eObject instanceof ITextContent                    && this.currentResultSet.getObject("content")!=null )                               ((ITextContent)eObject).setContent(this.currentResultSet.getString("content"));
				if ( eObject instanceof IDocumentable                   && this.currentResultSet.getObject("documentation")!=null )                         ((IDocumentable)eObject).setDocumentation(this.currentResultSet.getString("documentation"));
				if ( eObject instanceof INameable                       && this.currentResultSet.getObject("name")!=null && this.currentResultSet.getObject("element_id")==null )   ((INameable)eObject).setName(this.currentResultSet.getString("name"));
				if ( eObject instanceof IHintProvider                   && this.currentResultSet.getObject("hint_content")!=null )                          ((IHintProvider)eObject).setHintContent(this.currentResultSet.getString("hint_content"));
				if ( eObject instanceof IHintProvider                   && this.currentResultSet.getObject("hint_title")!=null )                            ((IHintProvider)eObject).setHintTitle(this.currentResultSet.getString("hint_title"));
				if ( eObject instanceof ILockable                       && this.currentResultSet.getObject("is_locked")!=null )                             {int locked; if ( this.currentResultSet.getObject("is_locked") instanceof String ) locked = Integer.valueOf(this.currentResultSet.getString("is_locked")); else locked=this.currentResultSet.getInt("is_locked"); ((ILockable)eObject).setLocked(locked==0?false:true);}
				if ( eObject instanceof IDiagramModelImageProvider      && this.currentResultSet.getObject("image_path")!=null )                            ((IDiagramModelImageProvider)eObject).setImagePath(this.currentResultSet.getString("image_path"));
				if ( eObject instanceof IIconic                         && this.currentResultSet.getObject("image_position")!=null )                        ((IIconic)eObject).setImagePosition(this.currentResultSet.getInt("image_position"));
				if ( eObject instanceof ILineObject                     && this.currentResultSet.getObject("line_color")!=null )                            ((ILineObject)eObject).setLineColor(this.currentResultSet.getString("line_color"));
				if ( eObject instanceof ILineObject                     && this.currentResultSet.getObject("line_width")!=null )                            ((ILineObject)eObject).setLineWidth(this.currentResultSet.getInt("line_width"));
				if ( eObject instanceof IDiagramModelObject             && this.currentResultSet.getObject("fill_color")!=null )                            ((IDiagramModelObject)eObject).setFillColor(this.currentResultSet.getString("fill_color"));
				if ( eObject instanceof IFontAttribute                  && this.currentResultSet.getObject("font")!=null )                                  ((IFontAttribute)eObject).setFont(this.currentResultSet.getString("font"));
				if ( eObject instanceof IFontAttribute                  && this.currentResultSet.getObject("font_color")!=null )                            ((IFontAttribute)eObject).setFontColor(this.currentResultSet.getString("font_color"));
				if ( eObject instanceof ICanvasModelSticky              && this.currentResultSet.getObject("notes")!=null )                                 ((ICanvasModelSticky)eObject).setNotes(this.currentResultSet.getString("notes"));
				if ( eObject instanceof ITextAlignment                  && this.currentResultSet.getObject("text_alignment")!=null )                        ((ITextAlignment)eObject).setTextAlignment(this.currentResultSet.getInt("text_alignment"));
				if ( eObject instanceof ITextPosition                   && this.currentResultSet.getObject("text_position")!=null )                         ((ITextPosition)eObject).setTextPosition(this.currentResultSet.getInt("text_position"));
				if ( eObject instanceof IDiagramModelObject )                                                                       ((IDiagramModelObject)eObject).setBounds(this.currentResultSet.getInt("x"), this.currentResultSet.getInt("y"), this.currentResultSet.getInt("width"), this.currentResultSet.getInt("height"));

				// The container is either the view, or a container in the view
				if ( DBPlugin.areEqual(this.currentResultSet.getString("container_id"), view.getId()) )
					view.getChildren().add((IDiagramModelObject)eObject);
				else
					((IDiagramModelContainer)model.getAllViewObjects().get(this.currentResultSet.getString("container_id"))).getChildren().add((IDiagramModelObject)eObject);


				if ( eObject instanceof IConnectable ) {
					model.registerSourceConnection((IDiagramModelObject)eObject, this.currentResultSet.getString("source_connections"));
					model.registerTargetConnection((IDiagramModelObject)eObject, this.currentResultSet.getString("target_connections"));
				}

				// If the object has got properties but does not have a linked element, then it may have distinct properties
				if ( eObject instanceof IProperties && this.currentResultSet.getString("element_id")==null ) {
					importProperties((IProperties)eObject);
				}

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()+" of "+this.currentResultSet.getString("class")+"("+((IIdentifier)eObject).getId()+")");

				// we reference the view for future use
				model.countObject(eObject, false, null);
				++this.countViewObjectsImported;

				// if the object contains an image, we store its path to import it later
				if ( this.currentResultSet.getString("image_path") != null )
					this.allImagePaths.add(this.currentResultSet.getString("image_path"));

				return true;
			}
			this.currentResultSet.close();
			this.currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views connections of a specific view from the database
	 */
	public void prepareImportViewsConnections(String viewId, int version) throws Exception {
		this.currentResultSet = select("SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type FROM "+this.schema+"views_connections WHERE view_id = ? AND view_version = ? ORDER BY rank"
				,viewId
				,version
				);
	}

	/**
	 * import the views connections from the database
	 */
	public boolean importViewsConnections(DBArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				EObject eObject;

				if ( this.currentResultSet.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));

				((IIdentifier)eObject).setId(this.currentResultSet.getString("id"));
				((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));

				if ( eObject instanceof IDiagramModelArchimateConnection && this.currentResultSet.getString("relationship_id") != null) {
					// we check that the relationship already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateRelationship relationship = model.getAllRelationships().get(this.currentResultSet.getString("relationship_id"));
					if ( relationship == null ) {
						importRelationshipFromId(model, null, this.currentResultSet.getString("relationship_id"), 0, false);
					}
				}

				if ( eObject instanceof INameable                           && this.currentResultSet.getObject("name")!=null )              ((INameable)eObject).setName(this.currentResultSet.getString("name"));
				if ( eObject instanceof ILockable                           && this.currentResultSet.getObject("is_locked")!=null )         {int locked; if ( this.currentResultSet.getObject("is_locked") instanceof String ) locked = Integer.valueOf(this.currentResultSet.getString("is_locked")); else locked=this.currentResultSet.getInt("is_locked"); ((ILockable)eObject).setLocked(locked==0?false:true);}
				if ( eObject instanceof IDocumentable                       && this.currentResultSet.getObject("documentation")!=null )     ((IDocumentable)eObject).setDocumentation(this.currentResultSet.getString("documentation"));
				if ( eObject instanceof ILineObject                         && this.currentResultSet.getObject("line_color")!=null )        ((ILineObject)eObject).setLineColor(this.currentResultSet.getString("line_color"));
				if ( eObject instanceof ILineObject                         && this.currentResultSet.getObject("line_width")!=null )        ((ILineObject)eObject).setLineWidth(this.currentResultSet.getInt("line_width"));
				if ( eObject instanceof IFontAttribute                      && this.currentResultSet.getObject("font")!=null )              ((IFontAttribute)eObject).setFont(this.currentResultSet.getString("font"));
				if ( eObject instanceof IFontAttribute                      && this.currentResultSet.getObject("font_color")!=null )        ((IFontAttribute)eObject).setFontColor(this.currentResultSet.getString("font_color"));
				if ( eObject instanceof IDiagramModelConnection             && this.currentResultSet.getObject("type")!=null )              ((IDiagramModelConnection)eObject).setType(this.currentResultSet.getInt("type"));
				if ( eObject instanceof IDiagramModelConnection             && this.currentResultSet.getObject("text_position")!=null )     ((IDiagramModelConnection)eObject).setTextPosition(this.currentResultSet.getInt("text_position"));
				if ( eObject instanceof IDiagramModelArchimateConnection    && this.currentResultSet.getObject("type")!=null )              ((IDiagramModelArchimateConnection)eObject).setType(this.currentResultSet.getInt("type"));
				if ( eObject instanceof IDiagramModelArchimateConnection    && this.currentResultSet.getObject("relationship_id")!=null )   ((IDiagramModelArchimateConnection)eObject).setArchimateConcept(model.getAllRelationships().get(this.currentResultSet.getString("relationship_id")));

				if ( eObject instanceof IConnectable ) {
					model.registerSourceConnection((IDiagramModelConnection)eObject, this.currentResultSet.getString("source_connections"));
					model.registerTargetConnection((IDiagramModelConnection)eObject, this.currentResultSet.getString("target_connections"));
				}
				//model.registerSourceAndTarget((IDiagramModelConnection)eObject, currentResultSet.getString("source_object_id"), currentResultSet.getString("target_object_id"));

				if ( eObject instanceof IDiagramModelConnection ) {
					try ( ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY rank", ((IIdentifier)eObject).getId(), ((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()) ) {
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

				// we reference the view for future use
				model.countObject(eObject, false, null);
				++this.countViewConnectionsImported;

				// If the connection has got properties but does not have a linked relationship, then it may have distinct properties
				if ( eObject instanceof IProperties && this.currentResultSet.getString("relationship_id")==null ) {
					importProperties((IProperties)eObject);
				}

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()+" of "+this.currentResultSet.getString("class")+"("+((IIdentifier)eObject).getId()+")");

				return true;
			}
			this.currentResultSet.close();
			this.currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the images from the database
	 */
	public HashSet<String> getAllImagePaths() {
		return this.allImagePaths;
	}

	/**
	 * import the views from the database
	 */
	public void importImage(DBArchimateModel model, String path) throws Exception {
		this.currentResultSet = select("SELECT image FROM "+this.schema+"images WHERE path = ?", path);

		if (this.currentResultSet.next() ) {
			IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);
			try {
				String imagePath;
				byte[] imageContent = this.currentResultSet.getBytes("image");

				if ( logger.isDebugEnabled() ) logger.debug( "Importing "+path+" with "+imageContent.length/1024+" Ko of data");
				imagePath = archiveMgr.addByteContentEntry(path, imageContent);

				if ( DBPlugin.areEqual(imagePath, path) ) {
					if ( logger.isDebugEnabled() ) logger.debug( "... image imported");
				} else {
					if ( logger.isDebugEnabled() ) logger.debug( "... image imported but with new path "+imagePath);
					//TODO: the image was already in the cache but with a different path
					//TODO: we must search all the objects with "path" to replace it with "imagepath" 
				}

			} catch (Exception e) {
				this.currentResultSet.close();
				this.currentResultSet = null;
				throw new Exception("Import of image failed !", e.getCause()!=null ? e.getCause() : e);
			}
			if ( logger.isDebugEnabled() ) logger.debug("   imported "+path);
			++this.countImagesImported;
			this.currentResultSet.close();
			this.currentResultSet = null;
		} else {
			this.currentResultSet.close();
			this.currentResultSet = null;
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
	private void importProperties(IProperties parent) throws Exception {
		int currentVersion;
		int i = 0;
		
		if ( parent instanceof IArchimateModel )
			currentVersion = ((DBArchimateModel)parent).getInitialVersion().getVersion();
		else
			currentVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

		try ( ResultSet result = select("SELECT name, value FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY rank", ((IIdentifier)parent).getId(), currentVersion)) {
			boolean shouldAdd;
			while ( result.next() ) {
				// if the property already exist, we update its value. If it doesn't, we create it
				IProperty prop ;
	
				shouldAdd = (i >= parent.getProperties().size() );
				if ( shouldAdd )
					prop = DBArchimateFactory.eINSTANCE.createProperty();
				else
					prop = parent.getProperties().get(i);
	
				if ( !DBPlugin.areEqual(prop.getKey(), result.getString("name")) )
					prop.setKey(result.getString("name"));
				if ( !DBPlugin.areEqual(prop.getValue(), result.getString("value")) )
					prop.setValue(result.getString("value"));
	
				if ( shouldAdd )
					parent.getProperties().add(prop);
	
				++i;
			}
		}

		// if there are more properties in memory than in database, we delete them
		while ( i < parent.getProperties().size() )
			parent.getProperties().remove(i);
	}

	public static String getSqlTypeName(int type) {
		switch (type) {
			case Types.BIT:
				return "BIT";
			case Types.TINYINT:
				return "TINYINT";
			case Types.SMALLINT:
				return "SMALLINT";
			case Types.INTEGER:
				return "INTEGER";
			case Types.BIGINT:
				return "BIGINT";
			case Types.FLOAT:
				return "FLOAT";
			case Types.REAL:
				return "REAL";
			case Types.DOUBLE:
				return "DOUBLE";
			case Types.NUMERIC:
				return "NUMERIC";
			case Types.DECIMAL:
				return "DECIMAL";
			case Types.CHAR:
				return "CHAR";
			case Types.VARCHAR:
				return "VARCHAR";
			case Types.LONGVARCHAR:
				return "LONGVARCHAR";
			case Types.DATE:
				return "DATE";
			case Types.TIME:
				return "TIME";
			case Types.TIMESTAMP:
				return "TIMESTAMP";
			case Types.BINARY:
				return "BINARY";
			case Types.VARBINARY:
				return "VARBINARY";
			case Types.LONGVARBINARY:
				return "LONGVARBINARY";
			case Types.NULL:
				return "NULL";
			case Types.OTHER:
				return "OTHER";
			case Types.JAVA_OBJECT:
				return "JAVA_OBJECT";
			case Types.DISTINCT:
				return "DISTINCT";
			case Types.STRUCT:
				return "STRUCT";
			case Types.ARRAY:
				return "ARRAY";
			case Types.BLOB:
				return "BLOB";
			case Types.CLOB:
				return "CLOB";
			case Types.REF:
				return "REF";
			case Types.DATALINK:
				return "DATALINK";
			case Types.BOOLEAN:
				return "BOOLEAN";
			case Types.ROWID:
				return "ROWID";
			case Types.NCHAR:
				return "NCHAR";
			case Types.NVARCHAR:
				return "NVARCHAR";
			case Types.LONGNVARCHAR:
				return "LONGNVARCHAR";
			case Types.NCLOB:
				return "NCLOB";
			case Types.SQLXML:
				return "SQLXML";
			default:
		}

		return "?";
	}

	/**
	 * Imports an element into the model<br>
	 * @param model model into which the element will be imported
	 * @param view if a view is provided, then an ArchimateObject will be automatically created
	 * @param elementId id of the element to import
	 * @param elementVersion version of the element to import (0 if the latest version should be imported)
	 * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the element should kee its original id 
	 * @return the imported element
	 * @throws Exception
	 */
	public IArchimateElement importElementFromId(DBArchimateModel model, IArchimateDiagramModel view, String elementId, int elementVersion, boolean mustCreateCopy) throws Exception {
		IArchimateElement element;
		List<Object> imported = new ArrayList<Object>();
		boolean newElement = false;

		// TODO add an option to import elements recursively

		@SuppressWarnings("resource")
		ResultSet resultElement = null;
		try {
			if ( elementVersion == 0 )
				resultElement = select("SELECT version, class, name, documentation, type, created_on FROM "+this.schema+"elements e WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE id = e.id)", elementId);
			else
				resultElement = select("SELECT version, class, name, documentation, type, created_on FROM "+this.schema+"elements e WHERE id = ? AND version = ?", elementId, elementVersion);
	
			if ( !resultElement.next() ) {
				if ( elementVersion == 0 )
				    throw new Exception("Element with id="+elementId+" has not been found in the database.");
			    throw new Exception("Element with id="+elementId+" and version="+elementVersion+" has not been found in the database.");
			}

			if ( mustCreateCopy ) {
				if ( logger.isDebugEnabled() ) logger.debug("Importing a copy of element id "+elementId+".");
				element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(resultElement.getString("class"));
				element.setId(model.getIDAdapter().getNewID());
				newElement = true;
	
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(0);
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
			} else {
				element = model.getAllElements().get(elementId);
				if ( element == null ) {
					if ( logger.isDebugEnabled() ) logger.debug("Importing element id "+elementId+".");
					element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(resultElement.getString("class"));
					element.setId(elementId);
					newElement = true;
				} else {
					if ( logger.isDebugEnabled() ) logger.debug("Updating element id "+elementId+".");
					newElement = false;
				}
	
				if ( !DBPlugin.areEqual(element.getName(), resultElement.getString("name")) ) element.setName(resultElement.getString("name")==null ? "" : resultElement.getString("name"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(resultElement.getInt("version"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(resultElement.getTimestamp("created_on"));
			}
	
			if ( !DBPlugin.areEqual(element.getDocumentation(), resultElement.getString("documentation")) ) element.setDocumentation(resultElement.getString("documentation"));
			if ( element instanceof IJunction ) {
				if ( !DBPlugin.areEqual(((IJunction)element).getType(), resultElement.getString("type")) ) ((IJunction)element).setType(resultElement.getString("type"));
			}
	
			importProperties(element);
			
			boolean createViewObject = false;
			if( newElement ) {
				model.getDefaultFolderForObject(element).getElements().add(element);
				model.getAllElements().put(element.getId(), element);
				model.countObject(element, false, null);
				createViewObject = view!=null;
			} else {
				if ( view == null ) {
					createViewObject = false;
				} else {
					createViewObject = componentToConnectable(view, element).isEmpty();
				}
			}

			if ( view != null && createViewObject ) {
				view.getChildren().add(ArchimateDiagramModelFactory.createDiagramModelArchimateObject(element));
			}

			++this.countElementsImported;
			
		} finally {
			if ( resultElement != null )
				resultElement.close();
		}


		// We import the relationships that source or target the element
		try ( ResultSet resultrelationship = select("SELECT id, source_id, target_id FROM "+this.schema+"relationships WHERE source_id = ? OR target_id = ?", elementId, elementId) ) {
			while ( resultrelationship.next() && resultrelationship.getString("id") != null ) {
				// we import only relationships that do not exist
				if ( model.getAllRelationships().get(resultrelationship.getString("id")) == null ) {
					IArchimateElement sourceElement = model.getAllElements().get(resultrelationship.getString("source_id"));
					IArchimateRelationship sourceRelationship = model.getAllRelationships().get(resultrelationship.getString("source_id"));
					IArchimateElement targetElement = model.getAllElements().get(resultrelationship.getString("target_id"));
					IArchimateRelationship targetRelationship = model.getAllRelationships().get(resultrelationship.getString("target_id"));
	
					// we import only relations when both source and target are in the model
					if ( (sourceElement!=null || sourceRelationship!=null) && (targetElement!=null || targetRelationship!=null) ) {
						imported.add(importRelationshipFromId(model, view, resultrelationship.getString("id"), 0, false));
					}
				}
			}
		}

		if ( !imported.isEmpty() )
			model.resolveRelationshipsSourcesAndTargets();

		imported.add(0, element);
		//ITreeModelView treeView = (ITreeModelView)ViewManager.showViewPart(ITreeModelView.ID, true);
		//if(treeView != null) {
		//	logger.trace("selecting newly imported components");
		//	treeView.getViewer().setSelection(new StructuredSelection(element), true);
		//}

		return element;
	}

	private List<IConnectable> componentToConnectable(IArchimateDiagramModel view, IArchimateConcept concept) {
		List<IConnectable> connectables = new ArrayList<IConnectable>();
		for ( IDiagramModelObject viewObject: view.getChildren() ) {
			connectables.addAll(componentToConnectable((IDiagramModelArchimateComponent)viewObject, concept));
		}
		return connectables;
	}

	private List<IConnectable> componentToConnectable(IDiagramModelArchimateComponent component, IArchimateConcept concept) {
		List<IConnectable> connectables = new ArrayList<IConnectable>();

		if ( concept instanceof IArchimateElement ) {
			if ( DBPlugin.areEqual(component.getArchimateConcept().getId(), concept.getId()) ) connectables.add(component);
		} else if ( concept instanceof IArchimateRelationship ) {
			for ( IDiagramModelConnection conn: component.getSourceConnections() ) {
				if ( DBPlugin.areEqual(conn.getSource().getId(), concept.getId()) ) connectables.add(conn);
				if ( DBPlugin.areEqual(conn.getTarget().getId(), concept.getId()) ) connectables.add(conn);
			}
			for ( IDiagramModelConnection conn: component.getTargetConnections() ) {
				if ( DBPlugin.areEqual(conn.getSource().getId(), concept.getId()) ) connectables.add(conn);
				if ( DBPlugin.areEqual(conn.getTarget().getId(), concept.getId()) ) connectables.add(conn);
			}
		}

		if ( component instanceof IDiagramModelContainer ) {
			for ( IDiagramModelObject child: ((IDiagramModelContainer)component).getChildren() ) {
				connectables.addAll(componentToConnectable((IDiagramModelArchimateComponent)child, concept));
			}
		}
		return connectables;
	}

	/**
	 * Imports a relationship into the model
	 * @param model model into which the relationship will be imported
	 * @param view if a view is provided, then an ArchimateConnection will be automatically created
	 * @param relationshipId id of the relationship to import
	 * @param relationshipVersion version of the relationship to import (0 if the latest version should be imported)
	 * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the element should kee its original id
	 * @return the imported relationship
	 * @throws Exception
	 */
	public IArchimateRelationship importRelationshipFromId(DBArchimateModel model, IArchimateDiagramModel view, String relationshipId, int relationshipVersion, boolean mustCreateCopy) throws Exception {
		boolean newRelationship = false;
		IArchimateRelationship relationship;

        @SuppressWarnings("resource")
        ResultSet resultRelationship = null;
        try {
	        if ( relationshipVersion == 0 )
	        	resultRelationship = select("SELECT version, class, name, documentation, source_id, target_id, strength, access_type, created_on FROM "+this.schema+"relationships r WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE id = r.id)", relationshipId);
	        else
	        	resultRelationship = select("SELECT version, class, name, documentation, source_id, target_id, strength, access_type, created_on FROM "+this.schema+"relationships r WHERE id = ? AND version = ?", relationshipId, relationshipVersion);
	
	        if ( !resultRelationship.next() ) {
	            if ( relationshipVersion == 0 )
	                throw new Exception("Relationship with id="+relationshipId+" has not been found in the database.");
	            throw new Exception("Relationship with id="+relationshipId+" and version="+relationshipVersion+" has not been found in the database.");
	        }
		        
	
			if ( mustCreateCopy ) {
				if ( logger.isDebugEnabled() ) logger.debug("Importing a copy of relationship id "+relationshipId+".");
				relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(resultRelationship.getString("class"));
				relationship.setId(model.getIDAdapter().getNewID());
				newRelationship = true;
	
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(0);
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
			} else {
				relationship = model.getAllRelationships().get(relationshipId);
				if ( relationship == null ) {
					if ( logger.isDebugEnabled() ) logger.debug("Importing relationship id "+relationshipId+".");
					relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(resultRelationship.getString("class"));
					relationship.setId(relationshipId);
					newRelationship = true;
				} else {
					if ( logger.isDebugEnabled() ) logger.debug("Upgrading relationship id "+relationshipId+".");
					newRelationship = false;
				}
	
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(resultRelationship.getInt("version"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setTimestamp(resultRelationship.getTimestamp("created_on"));
			}
	
			if ( !DBPlugin.areEqual(relationship.getName(), resultRelationship.getString("name")) ) relationship.setName(resultRelationship.getString("name")==null ? "" : resultRelationship.getString("name"));
			if ( !DBPlugin.areEqual(relationship.getDocumentation(), resultRelationship.getString("documentation")) )relationship.setDocumentation(resultRelationship.getString("documentation"));
	
			if ( relationship instanceof IInfluenceRelationship && resultRelationship.getObject("strength")!=null    && !DBPlugin.areEqual(((IInfluenceRelationship)relationship).getStrength(), resultRelationship.getString("strength")) )  ((IInfluenceRelationship)relationship).setStrength(resultRelationship.getString("strength"));
			if ( relationship instanceof IAccessRelationship    && resultRelationship.getObject("access_type")!=null && ((IAccessRelationship)relationship).getAccessType() != resultRelationship.getInt("access_type") )  ((IAccessRelationship)relationship).setAccessType(resultRelationship.getInt("access_type"));
	
			IArchimateConcept source = model.getAllElements().get(resultRelationship.getString("source_id"));
			if ( source == null ) source = model.getAllRelationships().get(resultRelationship.getString("source_id"));
			relationship.setSource(source);
	
			IArchimateConcept target = model.getAllElements().get(resultRelationship.getString("target_id"));
			if ( source == null ) source = model.getAllRelationships().get(resultRelationship.getString("target_id"));
			relationship.setTarget(target);
	
	
			importProperties(relationship);
	
			boolean createViewConnection = false;
			if ( newRelationship ) {
				model.getDefaultFolderForObject(relationship).getElements().add(relationship);
				model.getAllRelationships().put(relationship.getId(), relationship);
				createViewConnection = view!=null;
			} else {
				if ( view == null ) {
					createViewConnection = false;
				} else {
					createViewConnection = componentToConnectable(view, relationship).isEmpty();
				}
			}
	
			if ( view != null && createViewConnection ) {
				List<IConnectable> sourceConnections = componentToConnectable(view, relationship.getSource());
				List<IConnectable> targetConnections = componentToConnectable(view, relationship.getTarget());
	
				for ( IConnectable sourceConnection: sourceConnections ) {
					for ( IConnectable targetConnection: targetConnections ) {
						IDiagramModelArchimateConnection cnct = ArchimateDiagramModelFactory.createDiagramModelArchimateConnection(relationship);
						cnct.setSource(sourceConnection);
						sourceConnection.getSourceConnections().add(cnct);
						cnct.setTarget(targetConnection);
						targetConnection.getTargetConnections().add(cnct);
					}
				}
			}
        } finally {
        	if ( resultRelationship != null )
        		resultRelationship.close();
        }

		++this.countRelationshipsImported;

		return relationship;
	}



	/**
	 * This method imports a view with all its components (graphical objects and connections) and requirements (elements and relationships)<br>
	 * elements and relationships that needed to be imported are located in a folder named by the view
	 */
	public IDiagramModel importViewFromId(DBArchimateModel model, IFolder parentFolder, String id, boolean mustCreateCopy) throws Exception {
		if ( model.getAllViews().get(id) != null ) {
			if ( mustCreateCopy )
				throw new RuntimeException("Re-importing a view is not supported.\n\nIf you wish to create a copy of an existing table, you may use a copy-paste operation.");
			throw new RuntimeException("Re-importing a view is not supported.\n\nIf you wish to refresh your view from the database, you may close your model and re-import it from the database.");
		}

		if ( logger.isDebugEnabled() ) {
			if ( mustCreateCopy )
				logger.debug("Importing a copy of view id "+id);
			else {
				logger.debug("Importing view id "+id);
			}
		}

		// 1 : we create the view
		IDiagramModel view;
		try ( ResultSet resultView = select("SELECT version, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint FROM "+this.schema+"views v WHERE version = (SELECT MAX(version) FROM "+this.schema+"views WHERE id = v.id) AND id = ?", id) ) {
			resultView.next();
			if ( DBPlugin.areEqual(resultView.getString("class"), "CanvasModel") )
				view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(resultView.getString("class"));
			else
				view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(resultView.getString("class"));
	
			if ( mustCreateCopy ) 
				view.setId(model.getIDAdapter().getNewID());
			else
				view.setId(id);
	
			((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(resultView.getInt("version"));
	
			view.setName(resultView.getString("name")==null ? "" : resultView.getString("name"));
			view.setDocumentation(resultView.getString("documentation"));
			view.setConnectionRouterType(resultView.getInt("connection_router_type"));
			if ( view instanceof IArchimateDiagramModel && resultView.getObject("viewpoint")!=null )     ((IArchimateDiagramModel) view).setViewpoint(resultView.getString("viewpoint"));
			if ( view instanceof ISketchModel           && resultView.getObject("background")!=null )    ((ISketchModel)view).setBackground(resultView.getInt("background"));
			if ( view instanceof IHintProvider          && resultView.getObject("hint_content")!=null )  ((IHintProvider)view).setHintContent(resultView.getString("hint_content"));
			if ( view instanceof IHintProvider          && resultView.getObject("hint_title")!=null )    ((IHintProvider)view).setHintTitle(resultView.getString("hint_title"));
		}
		
		if ( (parentFolder!=null) && (((IDBMetadata)parentFolder).getDBMetadata().getRootFolderType() == FolderType.DIAGRAMS_VALUE) )
			parentFolder.getElements().add(view);
		else
			model.getDefaultFolderForObject(view).getElements().add(view);

		importProperties(view);

		model.getAllViews().put(((IIdentifier)view).getId(), view);

		if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()+" of "+((INameable)view).getName()+"("+((IIdentifier)view).getId()+")");

		model.resetSourceAndTargetCounters();

		// 2 : we import the objects and create the corresponding elements if they do not exist yet
		//        importing an element will automatically import the relationships to and from this element
		prepareImportViewsObjects(((IIdentifier)view).getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion());
		while ( importViewsObjects(model, view) ) {
		    // each loop imports an object
		}

		// 3 : we import the connections and create the corresponding relationships if they do not exist yet
		prepareImportViewsConnections(((IIdentifier)view).getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion());
		while ( importViewsConnections(model) ) {
		    // each loop imports a connection
		}
		

		model.resolveRelationshipsSourcesAndTargets();
		model.resolveConnectionsSourcesAndTargets();

		//TODO : import missing images
		//for (String path: getAllImagePaths()) {
		//    importImage(model, path);
		//}

		return view;
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
	
	@Override
    public void reset() throws SQLException {
	    super.reset();
	    
       if ( this.currentResultSet != null ) {
            this.currentResultSet.close();
            this.currentResultSet = null;
        }

        // we free up a bit the memory
        this.allImagePaths = null;
	}
}