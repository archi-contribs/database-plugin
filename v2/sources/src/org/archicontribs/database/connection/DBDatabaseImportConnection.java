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
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.commands.DBImportElementFromIdCommand;
import org.archicontribs.database.model.commands.DBImportRelationshipFromIdCommand;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFeatures;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IMetadata;
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
	
	// clob in Oracle are a real nightmare
	private String toCharDocumentation;
	private String toCharDocumentationAsDocumentation;
	private String toCharContentAsContent;
	private String toCharNotesAsNotes;
	private String toCharStrength;
	private String toCharStrengthAsStrength;

	/**
	 * Opens a connection to a JDBC database using all the connection details
	 * @param dbEntry 
	 * @throws SQLException 
	 */
	public DBDatabaseImportConnection(DBDatabaseEntry dbEntry) throws SQLException {
		super(dbEntry);
	}

	/**
	 * duplicates a connection to a JDBC database to allow switching between DBDatabaseExportConnection and DBDatabaseImportConnection
	 * @param exportConnection 
	 */
	public DBDatabaseImportConnection(DBDatabaseExportConnection exportConnection) {
		super();
		assert(exportConnection != null);
		super.databaseEntry = exportConnection.databaseEntry;
		super.schema = exportConnection.schema;
		super.schemaPrefix = exportConnection.schemaPrefix;
		super.connection = exportConnection.connection;
		this.isExportConnectionDuplicate = true;
		
		this.toCharDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation)" : "documentation";
		this.toCharDocumentationAsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation) AS documentation" : "documentation";
		this.toCharContentAsContent = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(content) AS content" : "content";
		this.toCharNotesAsNotes = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(notes) AS notes" : "notes";
		this.toCharStrength = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(strength)" : "strength";
		this.toCharStrengthAsStrength = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(strength) AS strength" : "strength";
	}
	
	 /**
     * duplicates a connection to a JDBC database to allow switching between DBDatabaseConnection and DBDatabaseImportConnection
	 * @param databaseConnection 
     */
    public DBDatabaseImportConnection(DBDatabaseConnection databaseConnection) {
        super();
        assert(databaseConnection != null);
        super.databaseEntry = databaseConnection.databaseEntry;
        super.schema = databaseConnection.schema;
        super.schemaPrefix = databaseConnection.schemaPrefix;
        super.connection = databaseConnection.connection;
        this.isExportConnectionDuplicate = false;
        
        this.toCharDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation)" : "documentation";
        this.toCharDocumentationAsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation) AS documentation" : "documentation";
        this.toCharContentAsContent = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(content) AS content" : "content";
        this.toCharNotesAsNotes = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(notes) AS notes" : "notes";
        this.toCharStrength = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(strength)" : "strength";
		this.toCharStrengthAsStrength = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(strength) AS strength" : "strength";
    }

	/**
	 * ResultSet of the current transaction (used by import process to allow the loop to be managed outside the DBdatabase class)
	 */
	private DBSelect currentResultSetElements = null;
	private DBSelect currentResultSetRelationships = null;
	private DBSelect currentResultSetFolders = null;
	private DBSelect currentResultSetViews = null;
	private DBSelect currentResultSetViewsObjects = null;
	private DBSelect currentResultSetViewsConnections = null; 

	/**
	 * Gets a component from the database and convert the result into a HashMap<br>
	 * Mainly used in DBGui to compare a component to its database version.
	 * @param component component to get
	 * @param version version of the component to get (0 to get the latest version) 
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

		return getObjectFromDatabase(id, clazz, version);
	}

	/**
	 * Gets a component from the database and convert the result into a HashMap<br>
	 * Mainly used in DBGui to compare a component to its database version.
	 * @param id id of component to get
	 * @param clazz class of component to get
	 * @param objectVersion version of the component to get (0 to get the latest version) 
	 * @return HashMap containing the object data
	 * @throws Exception
	 */
	// remove warning about the non closure of "result" as it IS closed in the finally block
	@SuppressWarnings("resource")
	public HashMap<String, Object> getObjectFromDatabase(String id, String clazz, int objectVersion) throws Exception {
		DBSelect result = null;
		int version = objectVersion;
		HashMap<String, Object> hashResult = null;

		if ( logger.isDebugEnabled() ) logger.debug("   Getting "+clazz);

		try {
			if ( version == 0 ) {
				// because of PostGreSQL, we need to split the request in two
				if (      DBPlugin.areEqual(clazz,  "IArchimateElement") )       result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, name, documentation, type, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"elements e WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"elements WHERE id = e.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") )  result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"relationships r WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"relationships WHERE id = r.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IFolder") )                 result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, 'Folder' as class, type, root_type, name, documentation, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"folders f WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"folders WHERE id = f.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") )           result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, name, documentation, created_by, created_on, background, connection_router_type, viewpoint, properties, features, checksum, container_checksum FROM "+this.schemaPrefix+"views v WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"views WHERE id = v.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelObject") )     result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, is_locked, image_path, image_position, line_color, line_width, fill_color, alpha, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"views_objects v WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"views_objects WHERE id = v.id)", id);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, relationship_version, source_object_id, target_object_id, text_position, type, created_by, created_on, properties, features, bendpoints, checksum FROM "+this.schemaPrefix+"views_connections v WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schemaPrefix+"views_connections WHERE id = v.id)", id);
				else throw new Exception("Do not know how to get a "+clazz+" from the database.");
			} else {        
				if (      DBPlugin.areEqual(clazz,  "IArchimateElement") )       result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, name, documentation, type, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"elements WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") )  result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"relationships WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IFolder") )                 result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, 'Folder' as class, type, root_type, name, documentation, created_by, created_on, properties, features, checksum FROM "+this.schemaPrefix+"folders WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") )           result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, name, documentation, created_by, created_on, background, connection_router_type, viewpoint, properties, features, checksum, container_checksum FROM "+this.schemaPrefix+"views WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelObject") )     result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, is_locked, image_path, image_position, line_color, line_width, fill_color, alpha, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, properties, features, checksum FROM "+this.schemaPrefix+"views_objects WHERE id = ? AND version = ?", id, version);
				else if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, relationship_version, source_object_id, target_object_id, text_position, type, created_by, created_on, properties, features, bendpoints, checksum FROM "+this.schemaPrefix+"views_connections v WHERE id = ? AND version = ?", id, version);
				else throw new Exception("Do not know how to get a "+clazz+" from the database.");
			}

			if ( result.next() ) {
				version = result.getInt("version");

				hashResult = resultSetToHashMap(result.getResult());

				// properties
				ArrayList<DBProperty> databaseProperties = new ArrayList<DBProperty>();
				if ( result.getInt("properties") != 0 ) {
					try ( DBSelect resultProperties = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT name, value FROM "+this.schemaPrefix+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY POS", id, version) ) {
						while ( resultProperties.next() )
							databaseProperties.add(new DBProperty(resultProperties.getString("name"), resultProperties.getString("value")));
					}
				}
				hashResult.put("properties", databaseProperties);

				// features
				ArrayList<DBProperty> databaseFeatures = new ArrayList<DBProperty>();
				if ( result.getInt("features") != 0 ) {
					try ( DBSelect resultFeatures = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT name, value FROM "+this.schemaPrefix+"features WHERE parent_id = ? AND parent_version = ? ORDER BY POS", id, version) ) {
						while ( resultFeatures.next() )
							databaseFeatures.add(new DBProperty(resultFeatures.getString("name"), resultFeatures.getString("value")));
					}
				}
				hashResult.put("features", databaseFeatures);

				// bendpoints
				if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) {
					ArrayList<DBBendpoint> databaseBendpoints = new ArrayList<DBBendpoint>();
					if ( result.getInt("bendpoints") != 0 ) {
						try ( DBSelect resultBendpoints = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT start_x, start_y, end_x, end_y FROM "+this.schemaPrefix+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY POS", id, version ) ) {
							while ( resultBendpoints.next() )
								databaseBendpoints.add(new DBBendpoint(resultBendpoints.getInt("start_x"), resultBendpoints.getInt("start_y"), resultBendpoints.getInt("end_x"), resultBendpoints.getInt("end_y")));
						}
					}
					hashResult.put("bendpoints", databaseBendpoints);
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
	 * @param rs 
	 * @return 
	 * @throws SQLException 
	 */
	public static HashMap<String, Object> resultSetToHashMap(ResultSet rs) throws SQLException {
		HashMap<String, Object> map = new HashMap<String, Object>();

		for (int column = 1; column <= rs.getMetaData().getColumnCount(); column++) {
			if ( rs.getObject(column) != null ) {
				// we only listed the types that may be found by the database proxy and not the exhaustive types list
				String columnName = rs.getMetaData().getColumnName(column).toLowerCase();			// we need to convert to lowercase because of Oracle
				switch ( rs.getMetaData().getColumnType(column) ) {
					case Types.INTEGER:
					case Types.NUMERIC:
					case Types.SMALLINT:
					case Types.TINYINT:
					case Types.BIGINT:
					case Types.BOOLEAN:
					case Types.BIT:        map.put(columnName, rs.getInt(column));  break;

					case Types.TIMESTAMP:
					case Types.TIME:       map.put(columnName, rs.getTimestamp(column)); break;

					default:               map.put(columnName, rs.getString(column));
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
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public int importModel(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   importing model");
		// reseting the model's counters
		model.resetCounters();
		
		// we remember the database used to import the model
		model.setImportDatabaseId(this.databaseEntry.getId());
		
	    this.toCharDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation)" : "documentation";
	    this.toCharDocumentationAsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(documentation) AS documentation" : "documentation";
	    this.toCharContentAsContent = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(content) AS content" : "content";
	    this.toCharNotesAsNotes = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(notes) AS notes" : "notes";
	    this.toCharStrength = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(strength)" : "strength";
		this.toCharStrengthAsStrength = DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ? "TO_CHAR(strength) AS strength" : "strength";
	    
		if ( model.getInitialVersion().getVersion() == 0 ) {
			try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT MAX(version) AS version FROM "+this.schemaPrefix+"models WHERE id = ?", model.getId()) ) {
				result.next();
				model.getInitialVersion().setVersion(result.getInt("version"));
			}
		}

		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT name, purpose, created_on, properties, features, checksum FROM "+this.schemaPrefix+"models WHERE id = ? AND version = ?", model.getId(), model.getInitialVersion().getVersion()) ) {
			result.next();
			model.setPurpose(result.getString("purpose"));
			model.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
			model.getInitialVersion().setChecksum(result.getString("checksum"));
			
			if ( result.getInt("properties") != 0 )
				importProperties(model, model.getId(), model.getInitialVersion().getVersion());

			if ( result.getInt("features") != 0 )
				importFeatures(model, model.getId(), model.getInitialVersion().getVersion());
			
			importMetadata(model);
		}

		String versionToImport = model.isLatestVersionImported() ? "(SELECT MAX(version) FROM "+this.schemaPrefix+"elements WHERE id = element_id)" : "element_version";
		this.importElementsRequest = "SELECT DISTINCT element_id, parent_folder_id, version, class, name, type, "+this.toCharDocumentationAsDocumentation+", created_on, properties, features, checksum"
				+ " FROM "+this.schemaPrefix+"elements_in_model"
				+ " JOIN "+this.schemaPrefix+"elements ON elements.id = element_id AND version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?"
				+ " GROUP BY element_id, parent_folder_id, version, class, name, type, "+this.toCharDocumentation+", properties, features, created_on, checksum, pos";
		try (DBSelect resultElements = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(*) AS countElements FROM ("+this.importElementsRequest+") elts", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultElements.next();
			this.countElementsToImport = resultElements.getInt("countElements");
			this.countElementsImported = 0;
		}


		versionToImport = model.isLatestVersionImported() ? "(SELECT MAX(version) FROM "+this.schemaPrefix+"relationships WHERE id = relationship_id)" : "relationship_version";
		this.importRelationshipsRequest = "SELECT DISTINCT relationship_id, parent_folder_id, version, class, name, "+this.toCharDocumentationAsDocumentation+", source_id, target_id, "+this.toCharStrengthAsStrength+", access_type, is_directed, created_on, properties, features, checksum"
				+ " FROM "+this.schemaPrefix+"relationships_in_model"
				+ " INNER JOIN "+this.schemaPrefix+"relationships ON id = relationship_id AND version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?"
				+ " GROUP BY relationship_id, parent_folder_id, version, class, name, "+this.toCharDocumentation+", source_id, target_id, "+this.toCharStrength+", access_type, is_directed, properties, features, created_on, checksum";
		try ( DBSelect resultRelationships = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(*) AS countRelationships FROM ("+this.importRelationshipsRequest+") relts"
				,model.getId()
				,model.getInitialVersion().getVersion()
				) ) {
			resultRelationships.next();
			this.countRelationshipsToImport = resultRelationships.getInt("countRelationships");
			this.countRelationshipsImported = 0;
		}

		versionToImport = model.isLatestVersionImported() ? "(SELECT MAX(version) FROM "+this.schemaPrefix+"folders WHERE folders.id = folders_in_model.folder_id)" : "folders_in_model.folder_version";
		String selectFoldersRequest = "SELECT DISTINCT folder_id, folder_version, parent_folder_id, type, root_type, name, "+this.toCharDocumentationAsDocumentation+", created_on, properties, features, checksum, pos"
				+ " FROM "+this.schemaPrefix+"folders_in_model"
				+ " JOIN "+this.schemaPrefix+"folders ON folders.id = folders_in_model.folder_id AND folders.version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( DBSelect resultFolders = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(*) AS countFolders FROM ("+selectFoldersRequest+") fldrs", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultFolders.next();
			this.countFoldersToImport = resultFolders.getInt("countFolders");
			this.countFoldersImported = 0;
		}
		this.importFoldersRequest = selectFoldersRequest + " ORDER BY pos";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		versionToImport = model.isLatestVersionImported() ? "(select max(version) from "+this.schemaPrefix+"views where views.id = views_in_model.view_id)" : "views_in_model.view_version";
		String selectViewsRequest = "SELECT DISTINCT id, version, parent_folder_id, class, name, "+this.toCharDocumentationAsDocumentation+", background, connection_router_type, viewpoint, created_on, properties, features, checksum, container_checksum, pos"
				+ " FROM "+this.schemaPrefix+"views_in_model"
				+ " JOIN "+this.schemaPrefix+"views ON views.id = views_in_model.view_id AND views.version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( DBSelect resultViews = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(*) AS countViews FROM ("+selectViewsRequest+") vws", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViews.next();
			this.countViewsToImport = resultViews.getInt("countViews");
			this.countViewsImported = 0;
		}
		this.importViewsRequest = selectViewsRequest + " ORDER BY pos";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		// versionToImport is same as for views
		String selectViewsObjectsRequest = "SELECT DISTINCT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, "+this.toCharContentAsContent+", "+this.toCharDocumentationAsDocumentation+", is_locked, image_path, image_position, line_color, line_width, fill_color, alpha, font, font_color, name, "+this.toCharNotesAsNotes+", text_alignment, text_position, type, x, y, width, height, properties, features, checksum"
				+ " FROM "+this.schemaPrefix+"views_objects"
				+ " JOIN "+this.schemaPrefix+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version"
				+ " JOIN "+this.schemaPrefix+"views_in_model ON views_objects_in_view.view_id = views_in_model.view_id AND views_objects_in_view.view_version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( DBSelect resultViewObjects = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(*) AS countViewsObjects FROM ("+selectViewsObjectsRequest+") vobjs", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViewObjects.next();
			this.countViewObjectsToImport = resultViewObjects.getInt("countViewsObjects");
			this.countViewObjectsImported = 0;
		}
		// (unused) this.importViewsObjectsRequest = this.selectViewsObjectsRequest + " ORDER BY views_objects.pos";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		// versionToImport is same as for views
		String selectViewsConnectionsRequest = "SELECT DISTINCT id, version, class, container_id, name, "+this.toCharDocumentationAsDocumentation+", is_locked, line_color, line_width, font, font_color, relationship_id, source_object_id, target_object_id, text_position, type, properties, features, checksum "
				+ " FROM "+this.schemaPrefix+"views_connections"
				+ " JOIN "+this.schemaPrefix+"views_connections_in_view ON views_connections_in_view.connection_id = views_connections.id AND views_connections_in_view.connection_version = views_connections.version"
				+ " JOIN "+this.schemaPrefix+"views_in_model ON views_connections_in_view.view_id = views_in_model.view_id AND views_connections_in_view.view_version = "+versionToImport
				+ " WHERE model_id = ? AND model_version = ?";
		try ( DBSelect resultViewConnections = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(*) AS countViewsConnections FROM ("+selectViewsConnectionsRequest+") vcons", model.getId(), model.getInitialVersion().getVersion()) ) {
			resultViewConnections.next();
			this.countViewConnectionsToImport = resultViewConnections.getInt("countViewsConnections");
			this.countViewConnectionsImported = 0;
		}
		// (unused) this.importViewsConnectionsRequest = this.selectViewsConnectionsRequest + " ORDER BY views_connections.pos";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		try ( DBSelect resultImages = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT COUNT(DISTINCT image_path) AS countImages"+
				" FROM "+this.schemaPrefix+"views_in_model"+
				" INNER JOIN "+this.schemaPrefix+"views ON views_in_model.view_id = views.id AND views_in_model.view_version = views.version"+
				" INNER JOIN "+this.schemaPrefix+"views_objects_in_view ON views_objects_in_view.view_id = views.id AND views_objects_in_view.view_version = views.version"+
				" INNER JOIN "+this.schemaPrefix+"views_objects ON views_objects.id = views_objects_in_view.object_id AND views_objects.version = views_objects_in_view.object_version"+
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
	 * @param model 
	 * @throws Exception 
	 */
	public void prepareImportFolders(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import folders");
		this.currentResultSetFolders = new DBSelect(this.databaseEntry.getName(), this.connection, this.importFoldersRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);
	}

	/**
	 * Import the folders from the database
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public boolean importFolders(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetFolders != null ) {
			if ( this.currentResultSetFolders.next() ) {
				IFolder folder = IArchimateFactory.eINSTANCE.createFolder();
				
				folder.setId(this.currentResultSetFolders.getString("folder_id"));
				
				// the DBMetadata must be get AFTER the id is set 
				DBMetadata dbMetadata = model.getDBMetadata(folder);

				dbMetadata.getInitialVersion().setVersion(this.currentResultSetFolders.getInt("folder_version"));
				dbMetadata.getInitialVersion().setChecksum(this.currentResultSetFolders.getString("checksum"));
				dbMetadata.getInitialVersion().setTimestamp(this.currentResultSetFolders.getTimestamp("created_on"));

				dbMetadata.setName(this.currentResultSetFolders.getString("name"));
				dbMetadata.setDocumentation(this.currentResultSetFolders.getString("documentation"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+folder.getClass().getSimpleName()+" \""+folder.getName()+"\" version "+dbMetadata.getInitialVersion().getVersion());

				String parentId = this.currentResultSetFolders.getString("parent_folder_id");

				if ( parentId != null && !parentId.isEmpty() ) {
					dbMetadata.setFolderType(FolderType.get(0));                              		// non root folders have got the "USER" type

					IFolder parent = model.getAllFolders().get(parentId);
					if ( parent == null )
						parent=model.getFolder(FolderType.get(this.currentResultSetFolders.getInt("root_type")));
					if ( parent == null ) 
						throw new Exception("Don't know where to create folder "+this.currentResultSetFolders.getString("name")+" of type "+this.currentResultSetFolders.getInt("type")+" and root_type "+this.currentResultSetFolders.getInt("root_type")+" (unknown folder ID "+this.currentResultSetFolders.getString("parent_folder_id")+")");

					parent.getFolders().add(folder);
				} else {
					dbMetadata.setFolderType(FolderType.get(this.currentResultSetFolders.getInt("type")));        // root folders have got their own type
					model.getFolders().add(folder);
				}

				if ( this.currentResultSetFolders.getInt("properties") != 0 )
					importProperties(folder, folder.getId(), dbMetadata.getInitialVersion().getVersion());
				
				if ( this.currentResultSetFolders.getInt("features") != 0)
					importFeatures(folder, folder.getId(), dbMetadata.getInitialVersion().getVersion());

				// we reference this folder for future use (storing sub-folders or components into it ...)
				model.countObject(folder, false);
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
	 * @param model 
	 * @throws Exception 
	 */
	public void prepareImportElements(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import elements");
		this.currentResultSetElements = new DBSelect(this.databaseEntry.getName(), this.connection,this.importElementsRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);

	}

	/**
	 * import the elements from the database
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public boolean importElements(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetElements != null ) {
			if ( this.currentResultSetElements.next() ) {
				
				IArchimateElement element = (IArchimateElement) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetElements.getString("class"))));

				element.setId(this.currentResultSetElements.getString("element_id"));
				
				// the DBMetadata should be get AFTER the id is set 
				DBMetadata dbMetadata = model.getDBMetadata(element);

				dbMetadata.getInitialVersion().setVersion(this.currentResultSetElements.getInt("version"));
				dbMetadata.getInitialVersion().setChecksum(this.currentResultSetElements.getString("checksum"));
				dbMetadata.getInitialVersion().setTimestamp(this.currentResultSetElements.getTimestamp("created_on"));

				dbMetadata.setName(this.currentResultSetElements.getString("name"));
				dbMetadata.setDocumentation(this.currentResultSetElements.getString("documentation"));
				dbMetadata.setType(this.currentResultSetElements.getString("type"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+element.getClass().getSimpleName()+" \""+element.getName()+"\" version "+dbMetadata.getInitialVersion().getVersion());

				IFolder folder = null;
				if ( this.currentResultSetElements.getString("parent_folder_id") != null ) {
				    folder = model.getAllFolders().get(this.currentResultSetElements.getString("parent_folder_id"));
				}
				
				if ( folder == null ) {
				    folder = model.getDefaultFolderForObject(element);
				}
				folder.getElements().add(element);
				
				if ( this.currentResultSetElements.getInt("properties") != 0 )
					importProperties(element, element.getId(), dbMetadata.getInitialVersion().getVersion());
				
				if ( this.currentResultSetElements.getInt("features") != 0 )
					importFeatures(element, element.getId(), dbMetadata.getInitialVersion().getVersion());

				// we reference the element for future use (establishing relationships, creating views objects, ...)
				model.countObject(element, false);
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
	 * @param model 
	 * @throws Exception 
	 */
	public void prepareImportRelationships(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import relationships");
		this.currentResultSetRelationships = new DBSelect(this.databaseEntry.getName(), this.connection,this.importRelationshipsRequest
				,model.getId()
				,model.getInitialVersion().getVersion()
				);
	}

	/**
	 * import the relationships from the database
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public boolean importRelationships(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetRelationships != null ) {
			if ( this.currentResultSetRelationships.next() ) {
				IArchimateRelationship relationship = (IArchimateRelationship) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetRelationships.getString("class"))));

				relationship.setId(this.currentResultSetRelationships.getString("relationship_id"));
				
				// the DBMetadata should be get AFTER the id is set 
				DBMetadata dbMetadata = model.getDBMetadata(relationship);

				dbMetadata.getInitialVersion().setVersion(this.currentResultSetRelationships.getInt("version"));
				dbMetadata.getInitialVersion().setChecksum(this.currentResultSetRelationships.getString("checksum"));
				dbMetadata.getInitialVersion().setTimestamp(this.currentResultSetRelationships.getTimestamp("created_on"));

				dbMetadata.setName(this.currentResultSetRelationships.getString("name")==null ? "" : this.currentResultSetRelationships.getString("name"));
				dbMetadata.setDocumentation(this.currentResultSetRelationships.getString("documentation"));
				dbMetadata.setStrength(this.currentResultSetRelationships.getString("strength"));
				dbMetadata.setAccessType(this.currentResultSetRelationships.getInt("access_type"));
				dbMetadata.setDirected(this.currentResultSetRelationships.getInt("is_directed"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+relationship.getClass().getSimpleName()+" \""+relationship.getName()+"\" version "+dbMetadata.getInitialVersion().getVersion());

				IFolder folder = null;
				if ( this.currentResultSetRelationships.getString("parent_folder_id") == null ) {
				    folder = model.getAllFolders().get(this.currentResultSetRelationships.getString("parent_folder_id"));
				}
				
				if ( folder == null ) {
				    folder = model.getDefaultFolderForObject(relationship);
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

				
				if ( this.currentResultSetRelationships.getInt("properties") != 0 )
					importProperties(relationship, relationship.getId(), dbMetadata.getInitialVersion().getVersion());
				
				if ( this.currentResultSetRelationships.getInt("features") != 0 )
					importFeatures(relationship, relationship.getId(), dbMetadata.getInitialVersion().getVersion());

				model.countObject(relationship, false);

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
	 * @param model 
	 * @throws Exception 
	 */
	public void prepareImportViews(DBArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import views");
		this.currentResultSetViews = new DBSelect(this.databaseEntry.getName(), this.connection,this.importViewsRequest,
				model.getId(),
				model.getInitialVersion().getVersion()
				);
	}

	/**
	 * import the views from the database
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public boolean importViews(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetViews != null ) {
			if ( this.currentResultSetViews.next() ) {
				IDiagramModel view;
				if ( DBPlugin.areEqual(this.currentResultSetViews.getString("class"), "CanvasModel") )
					view = (IDiagramModel) ICanvasFactory.eINSTANCE.create((EClass)(ICanvasFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetViews.getString("class"))));
				else
					view = (IDiagramModel) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetViews.getString("class"))));

				view.setId(this.currentResultSetViews.getString("id"));
				
				// the DBMetadata should be get AFTER the id is set 
				DBMetadata dbMetadata = model.getDBMetadata(view);

				dbMetadata.getInitialVersion().setVersion(this.currentResultSetViews.getInt("version"));
				dbMetadata.getInitialVersion().setChecksum(this.currentResultSetViews.getString("checksum"));
				dbMetadata.getInitialVersion().setContainerChecksum(this.currentResultSetViews.getString("container_checksum"));
				dbMetadata.getInitialVersion().setTimestamp(this.currentResultSetViews.getTimestamp("created_on"));

				dbMetadata.setName(this.currentResultSetViews.getString("name"));
				dbMetadata.setDocumentation(this.currentResultSetViews.getString("documentation"));
				dbMetadata.setConnectionRouterType(this.currentResultSetViews.getInt("connection_router_type"));
				dbMetadata.setViewpoint(this.currentResultSetViews.getString("viewpoint"));
				dbMetadata.setBackground(this.currentResultSetViews.getInt("background"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+view.getClass().getSimpleName()+" \""+view.getName()+"\" version "+dbMetadata.getInitialVersion().getVersion());

				model.getAllFolders().get(this.currentResultSetViews.getString("parent_folder_id")).getElements().add(view);

				if ( this.currentResultSetViews.getInt("properties") != 0 )
					importProperties(view, view.getId(), dbMetadata.getInitialVersion().getVersion());
				
				if ( this.currentResultSetViews.getInt("features") != 0 )
					importFeatures(view, view.getId(), dbMetadata.getInitialVersion().getVersion());

				// we reference the view for future use
				model.countObject(view, false);
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
	 * @param id 
	 * @param version 
	 * @throws Exception 
	 */
	public void prepareImportViewsObjects(String id, int version) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import views objects for view "+id+" version "+version);
		this.currentResultSetViewsObjects = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT DISTINCT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, "+this.toCharContentAsContent+", "+this.toCharDocumentationAsDocumentation+", is_locked, image_path, image_position, line_color, line_width, fill_color, alpha, font, font_color, name, "+this.toCharNotesAsNotes+", text_alignment, text_position, type, x, y, width, height, properties, features, checksum, created_on, pos"
				+" FROM "+this.schemaPrefix+"views_objects"
				+" JOIN "+this.schemaPrefix+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version"
				+" WHERE view_id = ? AND view_version = ?"
				+" ORDER BY pos"
				,id
				,version
				);
	}

	/**
	 * import the views objects from the database
	 * @param model 
	 * @param view 
	 * @return 
	 * @throws Exception 
	 */
	public boolean importViewsObjects(DBArchimateModel model, IDiagramModel view) throws Exception {
		if ( this.currentResultSetViewsObjects != null ) {
			if ( this.currentResultSetViewsObjects.next() ) {
				EObject eObject;

				if ( this.currentResultSetViewsObjects.getString("class").startsWith("Canvas") )
					eObject = ICanvasFactory.eINSTANCE.create((EClass)(ICanvasFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetViewsObjects.getString("class"))));
				else
					eObject = IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetViewsObjects.getString("class"))));

				((IIdentifier)eObject).setId(this.currentResultSetViewsObjects.getString("id"));
				
				// the DBMetadata should be get AFTER the id is set 
				DBMetadata dbMetadata = model.getDBMetadata(eObject);

				dbMetadata.getInitialVersion().setVersion(this.currentResultSetViewsObjects.getInt("version"));
				dbMetadata.getInitialVersion().setChecksum(this.currentResultSetViewsObjects.getString("checksum"));
				dbMetadata.getInitialVersion().setTimestamp(this.currentResultSetViewsObjects.getTimestamp("created_on"));

				if ( eObject instanceof IDiagramModelArchimateComponent && this.currentResultSetViewsObjects.getString("element_id") != null) {
					// we check that the element already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateElement element = model.getAllElements().get(this.currentResultSetViewsObjects.getString("element_id"));
					if ( element == null ) {
						DBImportElementFromIdCommand command = new DBImportElementFromIdCommand(this, model, null, null, this.currentResultSetViewsObjects.getString("element_id"), 0, DBImportMode.templateMode, true);
						((CommandStack)model.getAdapter(CommandStack.class)).execute(command);

						element = command.getImported();

						if ( command.getException() != null )
							throw command.getException();
					}
				}

				dbMetadata.setArchimateConcept(model.getAllElements().get(this.currentResultSetViewsObjects.getString("element_id")));
				dbMetadata.setReferencedModel(model.getAllViews().get(this.currentResultSetViewsObjects.getString("diagram_ref_id")));
				
				dbMetadata.setBorderColor(this.currentResultSetViewsObjects.getString("border_color"));
				dbMetadata.setBorderType(this.currentResultSetViewsObjects.getInt("border_type"));
				dbMetadata.setContent(this.currentResultSetViewsObjects.getString("content"));
				dbMetadata.setDocumentation(this.currentResultSetViewsObjects.getString("documentation"));
				dbMetadata.setLocked(this.currentResultSetViewsObjects.getObject("is_locked"));
				dbMetadata.setImagePath(this.currentResultSetViewsObjects.getString("image_path"));
				dbMetadata.setImagePosition(this.currentResultSetViewsObjects.getInt("image_position"));
				dbMetadata.setLineColor(this.currentResultSetViewsObjects.getString("line_color"));
				dbMetadata.setLineWidth(this.currentResultSetViewsObjects.getInt("line_width"));
				dbMetadata.setFillColor(this.currentResultSetViewsObjects.getString("fill_color"));
				if ( this.currentResultSetViewsObjects.getObject("alpha") != null )
					dbMetadata.setAlpha(this.currentResultSetViewsObjects.getInt("alpha"));
				dbMetadata.setFont(this.currentResultSetViewsObjects.getString("font"));
				dbMetadata.setFontColor(this.currentResultSetViewsObjects.getString("font_color"));
				dbMetadata.setName(this.currentResultSetViewsObjects.getString("name"));
				dbMetadata.setNotes(this.currentResultSetViewsObjects.getString("notes"));
				dbMetadata.setTextAlignment(this.currentResultSetViewsObjects.getInt("text_alignment"));
				dbMetadata.setTextPosition(this.currentResultSetViewsObjects.getInt("text_position"));
				dbMetadata.setType(this.currentResultSetViewsObjects.getInt("type"));
				dbMetadata.setBounds(this.currentResultSetViewsObjects.getInt("x"), this.currentResultSetViewsObjects.getInt("y"), this.currentResultSetViewsObjects.getInt("width"), this.currentResultSetViewsObjects.getInt("height"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+eObject.getClass().getSimpleName()+" \""+((INameable)eObject).getName()+"\" version "+dbMetadata.getInitialVersion().getVersion());

				// The container is either the view, or a container in the view
				// if the container is not found, we create the object in the view as this is better than an NullPointerException
				IDiagramModelContainer container = (IDiagramModelContainer)model.getAllViewObjects().get(this.currentResultSetViewsObjects.getString("container_id"));
				if ( (container == null) || DBPlugin.areEqual(this.currentResultSetViewsObjects.getString("container_id"), view.getId()) )
					view.getChildren().add((IDiagramModelObject)eObject);
				else
					((IDiagramModelContainer)model.getAllViewObjects().get(this.currentResultSetViewsObjects.getString("container_id"))).getChildren().add((IDiagramModelObject)eObject);

				// If the object has got properties but does not have a linked element, then it may have distinct properties
				if ( eObject instanceof IProperties && this.currentResultSetViewsObjects.getString("element_id")==null && (this.currentResultSetViewsObjects.getInt("properties") != 0) )
					importProperties((IProperties)eObject, ((IIdentifier)eObject).getId(), dbMetadata.getInitialVersion().getVersion());
				
				if ( this.currentResultSetViewsObjects.getInt("features") != 0 )
					importFeatures((IFeatures)eObject, ((IIdentifier)eObject).getId(), dbMetadata.getInitialVersion().getVersion());

				// we reference the view for future use
				model.countObject(eObject, false);
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
	 * @param id 
	 * @param version 
	 * @throws Exception 
	 */
	public void prepareImportViewsConnections(String id, int version) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("   Preparing to import views connections for view "+id+" version "+version);
		this.currentResultSetViewsConnections = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT DISTINCT id, version, class, container_id, name, "+this.toCharDocumentationAsDocumentation+", is_locked, line_color, line_width, font, font_color, relationship_id, source_object_id, target_object_id, text_position, type, properties, features, bendpoints, checksum, pos"
				+" FROM "+this.schemaPrefix+"views_connections"
				+" JOIN "+this.schemaPrefix+"views_connections_in_view ON views_connections_in_view.connection_id = views_connections.id AND views_connections_in_view.connection_version = views_connections.version"
				+" WHERE view_id = ? AND view_version = ?"
				+" ORDER BY pos"
				,id
				,version
				);
	}

	/**
	 * import the views connections from the database
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public boolean importViewsConnections(DBArchimateModel model) throws Exception {
		if ( this.currentResultSetViewsConnections != null ) {
			if ( this.currentResultSetViewsConnections.next() ) {
				EObject eObject;

				if ( this.currentResultSetViewsConnections.getString("class").startsWith("Canvas") )
					eObject = ICanvasFactory.eINSTANCE.create((EClass)(ICanvasFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetViewsConnections.getString("class"))));
				else
					eObject = IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier(this.currentResultSetViewsConnections.getString("class"))));

				((IIdentifier)eObject).setId(this.currentResultSetViewsConnections.getString("id"));
				
				// the DBMetadata should be get AFTER the id is set 
				DBMetadata dbMetadata = model.getDBMetadata(eObject);


				dbMetadata.getInitialVersion().setVersion(this.currentResultSetViewsConnections.getInt("version"));
				dbMetadata.getInitialVersion().setChecksum(this.currentResultSetViewsConnections.getString("checksum"));

				if ( eObject instanceof IDiagramModelArchimateConnection && this.currentResultSetViewsConnections.getString("relationship_id") != null) {
					// we check that the relationship already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateRelationship relationship = model.getAllRelationships().get(this.currentResultSetViewsConnections.getString("relationship_id"));
					if ( relationship == null ) {
						DBImportRelationshipFromIdCommand command = new DBImportRelationshipFromIdCommand(this, model, null, null, this.currentResultSetViewsConnections.getString("relationship_id"), 0, DBImportMode.templateMode);
						((CommandStack)model.getAdapter(CommandStack.class)).execute(command);

						relationship = command.getImported();

						if ( command.getException() != null )
							throw command.getException();
					}
				}

				dbMetadata.setName(this.currentResultSetViewsConnections.getString("name"));
				dbMetadata.setLocked(this.currentResultSetViewsConnections.getObject("is_locked"));
				dbMetadata.setDocumentation(this.currentResultSetViewsConnections.getString("documentation"));
				dbMetadata.setLineColor(this.currentResultSetViewsConnections.getString("line_color"));
				dbMetadata.setLineWidth(this.currentResultSetViewsConnections.getInt("line_width"));
				dbMetadata.setFont(this.currentResultSetViewsConnections.getString("font"));
				dbMetadata.setFontColor(this.currentResultSetViewsConnections.getString("font_color"));
				dbMetadata.setType(this.currentResultSetViewsConnections.getInt("type"));
				dbMetadata.setTextPosition(this.currentResultSetViewsConnections.getInt("text_position"));
				dbMetadata.setArchimateConcept(model.getAllRelationships().get(this.currentResultSetViewsConnections.getString("relationship_id")));
				
				if ( logger.isDebugEnabled() ) logger.debug("   Importing "+eObject.getClass().getSimpleName()+" \""+((INameable)eObject).getName()+"\" version "+dbMetadata.getInitialVersion().getVersion());

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

				// If the connection has got properties but does not have a linked relationship, then it may have distinct properties
				if ( eObject instanceof IProperties && this.currentResultSetViewsConnections.getString("relationship_id")==null && (this.currentResultSetViewsConnections.getInt("properties") != 0) )
						importProperties((IProperties)eObject, ((IIdentifier)eObject).getId(), dbMetadata.getInitialVersion().getVersion());
				
				// connections have got their own features
				if ( this.currentResultSetViewsConnections.getInt("features") != 0 )
					importFeatures((IFeatures)eObject, ((IIdentifier)eObject).getId(), dbMetadata.getInitialVersion().getVersion());

				if ( (eObject instanceof IDiagramModelConnection) && (this.currentResultSetViewsConnections.getInt("bendpoints") != 0) ) {
					importBendpoints((IDiagramModelConnection)eObject, ((IIdentifier)eObject).getId(), dbMetadata.getInitialVersion().getVersion());
				}

				// we reference the connection for future use
				model.countObject(eObject, false);
				++this.countViewConnectionsImported;

				return true;
			}
			this.currentResultSetViewsConnections.close();
			this.currentResultSetViewsConnections = null;
		}
		return false;
	}

	/**
	 * import the views from the database
	 * @param model 
	 * @param path 
	 * @throws Exception 
	 */
	public void importImage(DBArchimateModel model, String path) throws Exception {
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT image FROM "+this.schemaPrefix+"images WHERE path = ?", path) ) {
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
				throw new Exception("Import of image failed: unkwnown image path "+path);
		} 
	}

	/**
	 * import an image from the database
	 * @param path 
	 * @return 
	 * @throws Exception 
	 */
	public Image getImageFromDatabase(String path) throws Exception {
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT image FROM "+this.schemaPrefix+"images WHERE path = ?", path) ) {
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
	 * @return 
	 * @throws Exception 
	 */
	public List<String> getImageListFromDatabase() throws Exception {
		List<String> list = new ArrayList<String>();
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT path FROM "+this.schemaPrefix+"images") ) {
			while ( result.next() ) {
				list.add(result.getString("path"));
			}
		}
		return list;
	}

	/**
	 * check if the number of imported images is equals to what is expected
	 * @throws Exception 
	 */
	public void checkImportedImagesCount() throws Exception {
		if ( this.countImagesImported != this.countImagesToImport )
			throw new Exception(this.countImagesImported+" images imported instead of the "+this.countImagesToImport+" that were expected.");

		if ( logger.isDebugEnabled() ) logger.debug(this.countImagesImported+" images imported.");
	}

	/**
	 * Imports the properties of an Archi component
	 * @param parent 
	 * @param id 
	 * @param version 
	 * @throws SQLException 
	 */
	public void importProperties(IProperties parent, String id, int version) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("      Importing properties");

		// first, we delete all existing properties
		parent.getProperties().clear();

		// then, we import the properties from the database 
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT name, value FROM "+this.schemaPrefix+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY pos", id, version)) {
			while ( result.next() ) {
				// if the property already exist, we update its value. If it doesn't, we create it
				IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
				prop.setKey(result.getString("name"));
				prop.setValue(result.getString("value"));
				parent.getProperties().add(prop);
			}
		}
	}
	
	/**
	 * Imports the features of an Archi component
	 * @param parent 
	 * @param id 
	 * @param version 
	 * @throws SQLException 
	 */
	public void importFeatures(IFeatures parent, String id, int version) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("      Importing features");

		// first, we delete all existing properties
		parent.getFeatures().clear();

		// then, we import the properties from the database 
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT name, value FROM "+this.schemaPrefix+"features WHERE parent_id = ? AND parent_version = ? ORDER BY pos", id, version)) {
			while ( result.next() ) {
				// if the property already exist, we update its value. If it doesn't, we create it
				IFeature feature = IArchimateFactory.eINSTANCE.createFeature();
				feature.setName(result.getString("name"));
				feature.setValue(result.getString("value"));
				parent.getFeatures().add(feature);
			}
		}
	}
	
	/**
	 * Imports the bendpoints of an Archi connection
	 * @param parent 
	 * @param id 
	 * @param version 
	 * @throws SQLException 
	 */
	public void importBendpoints(IDiagramModelConnection parent, String id, int version) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("      Importing bendpoints");
		
		// first, we delete all existing bendpoints
		if ( parent.getBendpoints() != null )
			parent.getBendpoints().clear();
		
		// then we import the bendpoints from the database
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT start_x, start_y, end_x, end_y FROM "+this.schemaPrefix+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY pos", id, version)) {
			while(result.next()) {
				IDiagramModelBendpoint bendpoint = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
				bendpoint.setStartX(result.getInt("start_x"));
				bendpoint.setStartY(result.getInt("start_y"));
				bendpoint.setEndX(result.getInt("end_x"));
				bendpoint.setEndY(result.getInt("end_y"));
				parent.getBendpoints().add(bendpoint);
			}
		}
	}

	/**
	 * Imports the metadata of a model<br>
	 * @param model 
	 * @throws SQLException 
	 */
	@SuppressWarnings("deprecation")
	private void importMetadata(DBArchimateModel model) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("      Importing metadata");

		// first, we delete all existing metadata
		if ( model.getMetadata() != null )
			model.getMetadata().getEntries().clear();
		else
			model.setMetadata(IArchimateFactory.eINSTANCE.createMetadata());

		// then, we import the metadata from the database 
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT name, value FROM "+this.schemaPrefix+"metadata WHERE parent_id = ? AND parent_version = ? ORDER BY pos", model.getId(), model.getInitialVersion().getVersion())) {
			while ( result.next() ) {
				// if the property already exist, we update its value. If it doesn't, we create it
				IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
				prop.setKey(result.getString("name"));
				prop.setValue(result.getString("value"));
				if ( model.getMetadata() == null ) {
					// if the model does not have metadata yet, we create them
					IMetadata metadata = IArchimateFactory.eINSTANCE.createMetadata();
					model.setMetadata(metadata);
				}
				model.getMetadata().getEntries().add(prop);
			}
		}
	}

	/**
	 * gets the latest model version in the database (0 if the model does not exist in the database)
	 * @param model 
	 * @return 
	 * @throws Exception 
	 */
	public int getLatestModelVersion(DBArchimateModel model) throws Exception {
		// we use COALESCE to guarantee that a value is returned, even if the model does not exist in the database
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT COALESCE(MAX(version),0) as version FROM "+this.schemaPrefix+"models WHERE id = ?", model.getId()) ) {
			result.next();
			return result.getInt("version");
		}
	}

	/**
	 * Reset the counters
	 */
	public void reset() {
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
	 * @param model 
	 * @param clazz 
	 * @param id 
	 * @return 
	 * @throws Exception 
	 */
	public IFolder getLastKnownFolder(DBArchimateModel model, String clazz, String id) throws Exception {
		IFolder parentFolder = null;

		if ( !model.isLatestVersionImported() ) {
			String table;
			String column;
			if ( logger.isDebugEnabled() ) logger.debug("   getting last known folder");

			if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) { table = this.schemaPrefix+"elements_in_model" ; column = "element_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) { table = this.schemaPrefix+"relationships_in_model" ; column = "relationship_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IFolder") ) { table = this.schemaPrefix+"folders_in_model" ; column = "folder_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) { table = this.schemaPrefix+"views_in_model" ; column = "view_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModelObject") ) { table = this.schemaPrefix+"views_ojects_in_model" ; column = "object_id"; }
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModelConnection") ) { table = this.schemaPrefix+"views_connections_in_model" ; column = "connection_id"; }
			else throw new Exception("Do not know how to get a "+clazz+" from the database.");

			try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT parent_folder_id, model_version"
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
	 * @param folder 
	 * @throws Exception
	 */
	public void setFolderToLastKnown(DBArchimateModel model, IFolder folder) throws Exception {
		if ( !model.isLatestVersionImported() ) {
			IFolder parentFolder = null;
			DBMetadata dbMetadta = model.getDBMetadata(folder);

			if ( logger.isDebugEnabled() ) logger.debug("   setting folder to last known");

			try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT parent_folder_id, model_version"
					+ " FROM "+this.schemaPrefix+"folders_in_model"
					+ " WHERE model_id = ? and folder_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.schemaPrefix+"folders_in_model WHERE model_id = ? AND folder_id = ?)"
					, model.getId()
					, folder.getId()
					, model.getId()
					, folder.getId()
					) ) {
				if ( result.next() )
					parentFolder = model.getAllFolders().get(result.getString("parent_folder_id"));
			}
			if (parentFolder == null )
				parentFolder = model.getFolder(dbMetadta.getFolderType());

			dbMetadta.setParentFolder(parentFolder);
		}


	}

	/**
	 * Check if the component has already been part of the model once, and sets it to the folder it was in.<br>
	 * <br>
	 * if the component has never been part of the model, then is it set in the default folder for this component
	 * @param model
	 * @param view 
	 * @throws Exception
	 */
	public void setFolderToLastKnown(DBArchimateModel model, IDiagramModel view) throws Exception {
		if ( !model.isLatestVersionImported() ) {
			IFolder parentFolder = null;

			if ( logger.isDebugEnabled() ) logger.debug("   setting folder to last known");

			try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT parent_folder_id, model_version"
					+ " FROM "+this.schemaPrefix+"views_in_model"
					+ " WHERE model_id = ? and view_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.schemaPrefix+"views_in_model WHERE model_id = ? AND view_id = ?)"
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

			model.getDBMetadata(view).setParentFolder(parentFolder);
		}
	}
	
	private HashSet<String> alreadyImported = new HashSet<String>();
	/**
	 * Declare that the element has been imported in order to avoid to import it twice
	 * @param id
	 */
	public void declareAsImported(String id) {
		this.alreadyImported.add(id);
	}
	/**
	 * checks if the element has already been imported
	 * @param id
	 * @return true if already imported, false is not
	 */
	public boolean isAlreadyImported(String id) {
		return this.alreadyImported.contains(id);
	}
}