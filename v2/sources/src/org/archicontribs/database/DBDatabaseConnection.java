/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.impl.Folder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

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
import com.archimatetool.model.IDiagramModelComponent;
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
public class DBDatabaseConnection {
	private static final DBLogger logger = new DBLogger(DBDatabaseConnection.class);

	/**
	 * Version of the expected database model.<br>
	 * If the value found into the columns version of the table "database_version", then the plugin will try to upgrade the datamodel.
	 */
	public static final int databaseVersion = 204;

	/**
	 * This class variable stores the last commit transaction
	 * It will be used in every insert and update calls<br>
	 * This way, all requests in a transaction will have the same timestamp.
	 */
	private Timestamp lastTransactionTimestamp = null;

	/**
	 * the databaseEntry corresponding to the connection
	 */
	protected DBDatabaseEntry databaseEntry = null;
	protected String schema = "";



	/**
	 * Connection to the database
	 */
	protected Connection connection = null;

	// This variables allows to store the columns type. They will be calculated for all the database brands.
	private String AUTO_INCREMENT;
	private String BOOLEAN;
	private String COLOR;
	private String DATETIME;
	private String FONT;
	private String IMAGE;
	private String INTEGER;
	private String OBJECTID;
	private String OBJ_NAME;
	private String PRIMARY_KEY;
	private String STRENGTH;
	private String TEXT;
	private String TYPE;
	private String USERNAME;


	/**
	 * Opens a connection to a JDBC database using all the connection details
	 */
	public DBDatabaseConnection(DBDatabaseEntry databaseEntry) throws ClassNotFoundException, SQLException {

		assert(databaseEntry != null);

		this.databaseEntry = databaseEntry;

		schema = databaseEntry.getSchemaPrefix();

		openConnection();
	}


	private void openConnection() throws ClassNotFoundException, SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Opening connection to database "+databaseEntry.getName()+" : driver="+databaseEntry.getDriver()+", server="+databaseEntry.getServer()+", port="+databaseEntry.getPort()+", database="+databaseEntry.getDatabase()+", schema="+databaseEntry.getSchema()+", username="+databaseEntry.getUsername());

		String clazz = null;
		String connectionString = null;

		switch (databaseEntry.getDriver()) {
			case "postgresql" :
				clazz = "org.postgresql.Driver";
				connectionString = "jdbc:postgresql://" + databaseEntry.getServer() + ":" + databaseEntry.getPort() + "/" + databaseEntry.getDatabase();
				break;
			case "ms-sql"      :
				clazz = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
				connectionString = "jdbc:sqlserver://" + databaseEntry.getServer() + ":" + databaseEntry.getPort() + ";databaseName=" + databaseEntry.getDatabase();
				if ( DBPlugin.isEmpty(databaseEntry.getUsername()) && DBPlugin.isEmpty(databaseEntry.getPassword()) )
					connectionString += ";integratedSecurity=true";
				break;
			case "mysql"      :
				clazz = "com.mysql.jdbc.Driver";
				connectionString = "jdbc:mysql://" + databaseEntry.getServer() + ":" + databaseEntry.getPort() + "/" + databaseEntry.getDatabase();
				break;
			case "neo4j"      :
				clazz = "org.neo4j.jdbc.Driver";
				connectionString = "jdbc:neo4j:bolt://" + databaseEntry.getServer() + ":" + databaseEntry.getPort();
				break;
			case "oracle"     :
				clazz = "oracle.jdbc.driver.OracleDriver";
				connectionString = "jdbc:oracle:thin:@" + databaseEntry.getServer() + ":" + databaseEntry.getPort() + ":" + databaseEntry.getDatabase();
				break;
			case "sqlite"     :
				clazz = "org.sqlite.JDBC";
				connectionString = "jdbc:sqlite:"+databaseEntry.getServer();
				break;
			default :
				throw new SQLException("Unknonwn driver " + databaseEntry.getDriver());        // just in case
		}

		if ( logger.isDebugEnabled() ) logger.debug("JDBC class = " + clazz);
		Class.forName(clazz);

		if ( logger.isDebugEnabled() ) logger.debug("JDBC connection string = " + connectionString);
		try {
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "ms-sql") && DBPlugin.isEmpty(databaseEntry.getUsername()) && DBPlugin.isEmpty(databaseEntry.getPassword()) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Connecting with Windows integrated security");
				connection = DriverManager.getConnection(connectionString);
			} else {
				if ( logger.isDebugEnabled() ) logger.debug("Connecting with username = "+databaseEntry.getUsername());
				connection = DriverManager.getConnection(connectionString, databaseEntry.getUsername(), databaseEntry.getPassword());
			}
		} catch (SQLException e) {
			// if the JDBC driver fails to connect to the database using the specified driver, the it tries with all the other drivers
			// and the exception is raised by the latest driver (log4j in our case)
			// so we need to trap this exception and change the error message
			// For JDBC people, this is not a bug but a functionality :( 
			if ( DBPlugin.areEqual(e.getMessage(), "JDBC URL is not correct.\nA valid URL format is: 'jdbc:neo4j:http://<host>:<port>'") )
				if ( databaseEntry.getDriver().equals("ms-sql") && DBPlugin.isEmpty(databaseEntry.getUsername()) && DBPlugin.isEmpty(databaseEntry.getPassword()) )	// integrated authentication
					throw new SQLException("Please verify the database configuration in the preferences.\n\nPlease also check that you installed the \"sqljdbc_auth.dll\" file in the JRE bin folder to enable the SQL Server integrated security mode.");
				else
					throw new SQLException("Please verify the database configuration in the preferences.");
			else
				throw e;
		}

		if ( logger.isDebugEnabled() ) {
			if ( DBPlugin.isEmpty(schema) ) {
				logger.debug("Will use default schema ");
			}else {
				logger.debug("Will use schema "+schema);
			}
		}
	}

	/**
	 * Closes connection to the database
	 */
	public void close() throws SQLException {
		reset();

		if ( connection == null || connection.isClosed() ) {
			if ( logger.isDebugEnabled() ) logger.debug("The database connection is already closed.");
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Closing database connection.");
			connection.close();
		}
		connection = null;
		databaseEntry = null;
	}

	public boolean isConnected() {
		return connection != null;
	}

	public void reset() throws SQLException {
		for ( PreparedStatement pstmt: preparedStatementMap.values() ) {
			pstmt.close();
			pstmt=null;
		}
		preparedStatementMap = new HashMap<String, PreparedStatement>();

		if ( currentResultSet != null ) {
			currentResultSet.close();
			currentResultSet = null;
		}

		// We reset all "ranks" to zero
		elementRank = 0;
		relationshipRank = 0;
		folderRank = 0;
		viewRank = 0;
		viewObjectRank = 0;
		viewConnectionRank = 0;

		// we free up a bit the memory
		allImagePaths = null;
	}




	/**
	 * Checks the database structure<br>
	 * @throws SQLExcetion if the connection to the database failed
	 * @returns true if the database structure is correct, false if not
	 */
	public void checkDatabase() throws ClassNotFoundException, SQLException {
		boolean wasConnected = isConnected();
		ResultSet result = null;

		DBGui.popup("Please wait while checking the "+databaseEntry.getDriver()+" database ...");

		try {
			if ( !wasConnected )
				openConnection();
			
			switch ( databaseEntry.getDriver() ) {
				case "neo4j" :
					DBGui.closePopup();		// no tables to check on neo4j databases
					return;
				case "sqlite" :
					AUTO_INCREMENT	= "integer PRIMARY KEY";
					BOOLEAN			= "tinyint(1)";          				// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					COLOR			= "varchar(7)";
					DATETIME		= "timestamp";
					FONT			= "varchar(150)";
					IMAGE			= "blob";
					INTEGER			= "integer(10)";
					OBJECTID		= "varchar(50)";
					OBJ_NAME		= "varchar(1024)";
					PRIMARY_KEY		= "PRIMARY KEY";
					STRENGTH		= "varchar(20)";
					TEXT			= "clob";
					TYPE			= "varchar(3)";
					USERNAME		= "varchar(30)";
					break;
				case "mysql"  :
					AUTO_INCREMENT	= "int(10) NOT NULL AUTO_INCREMENT";
					BOOLEAN			= "tinyint(1)";							// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					COLOR			= "varchar(7)";
					DATETIME		= "datetime";
					FONT			= "varchar(150)";
					IMAGE			= "longblob";
					INTEGER			= "int(10)";
					OBJECTID		= "varchar(50)";
					OBJ_NAME		= "varchar(1024)";
					PRIMARY_KEY		= "PRIMARY KEY";
					STRENGTH		= "varchar(20)";
					TEXT			= "mediumtext";
					TYPE			= "varchar(3)";
					USERNAME		= "varchar(30)";
					break;
				case "ms-sql"  :
					AUTO_INCREMENT	= "int IDENTITY NOT NULL" ;
					BOOLEAN			= "tinyint";          					// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					COLOR			= "varchar(7)";
					DATETIME		= "datetime";
					FONT			= "varchar(150)";
					IMAGE			= "image";
					INTEGER			= "int";
					OBJECTID		= "varchar(50)";
					OBJ_NAME		= "varchar(1024)";
					PRIMARY_KEY		= "PRIMARY KEY";
					STRENGTH		= "varchar(20)";
					TEXT			= "nvarchar(max)";
					TYPE			= "varchar(3)";
					USERNAME		= "varchar(30)";
					break;
				case "oracle" :
					AUTO_INCREMENT	= "integer NOT NULL";
					BOOLEAN			= "char";          						// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					COLOR			= "varchar(7)";
					DATETIME		= "date";
					FONT			= "varchar(150)";
					IMAGE			= "blob";
					INTEGER			= "integer";
					OBJECTID		= "varchar(50)";
					OBJ_NAME		= "varchar(1024)";
					PRIMARY_KEY		= "PRIMARY KEY";
					STRENGTH		= "varchar(20)";
					TEXT			= "clob";
					TYPE			= "varchar(3)";
					USERNAME		= "varchar(30)";
					break;
				case "postgresql" :
					AUTO_INCREMENT	= "serial NOT NULL" ;
					BOOLEAN			= "smallint";          					// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					COLOR			= "varchar(7)";
					DATETIME		= "timestamp";
					FONT			= "varchar(150)";
					IMAGE			= "bytea";
					INTEGER			= "integer";
					OBJECTID		= "varchar(50)";
					OBJ_NAME		= "varchar(1024)";
					PRIMARY_KEY		= "PRIMARY KEY";
					STRENGTH		= "varchar(20)";
					TEXT			= "text";
					TYPE			= "varchar(3)";
					USERNAME		= "varchar(30)";
					break;
			}

			// checking if the database_version table exists
			if ( logger.isTraceEnabled() ) logger.trace("Checking \""+schema+"database_version\" table");

			try {
				result = select("SELECT version FROM "+schema+"database_version WHERE archi_plugin = ?", DBPlugin.pluginName);
			} catch (SQLException err) {
				DBGui.closePopup();
				// if the table does not exist
				if ( !DBGui.question("We successfully connected to the database but the necessary tables have not be found.\n\nDo you wish to create them ?") ) {
					throw new SQLException("Necessary tables not found.");
				}

				createTables();
				return;
			}

			if ( result.next() ) {
				switch ( result.getInt("version")) {
					case databaseVersion : break;		// good, nothing to do
					case 200 : 
				}
				if ( databaseVersion != result.getInt("version") ) {
					if ( (result.getInt("version")<200) || (result.getInt("version")>databaseVersion) )
						throw new SQLException("The database has got an unknown model version (is "+result.getInt("version")+" but should be between 200 and "+databaseVersion+")");

					if ( DBGui.question("The database needs to be upgraded. You will not loose any data during this operation.\n\nDo you wish to upgrade your database ?") ) {
						upgradeDatabase(result.getInt("version"));
					} else
						throw new SQLException("The database needs to be upgraded.");
				}
			} else {
				result.close();
				result=null;
				throw new SQLException(DBPlugin.pluginName+" not found in "+schema+"database_version table");
				//TODO : call create tables and update createTables method to ignore error on database_version table (in case it already exists and is empty)
			}
			result.close();
			result=null;

			DBGui.closePopup();

		} finally {
			if ( !wasConnected && connection!=null) {
				connection.close();
				connection = null;
			}

			DBGui.closePopup();
		}
	}

	/**
	 * Creates the necessary tables in the database
	 * @throws ClassNotFoundException 
	 */
	private void createTables() throws SQLException, ClassNotFoundException {
		final String[] databaseVersionColumns = {"archi_plugin", "version"};

		DBGui.popup("Please wait while creating necessary database tables ...");

		boolean wasConnected = isConnected();

		if ( !wasConnected )
			openConnection();

		setAutoCommit(false);

		try {
			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"database_version");
			request("CREATE TABLE "+ schema +"database_version ("
					+ "archi_plugin "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL"
					+ ")");

			insert(schema+"database_version", databaseVersionColumns, DBPlugin.pluginName, databaseVersion);

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"bendpoints");
			request("CREATE TABLE "+ schema +"bendpoints ("
					+ "parent_id "+ OBJECTID +" NOT NULL, "
					+ "parent_version "+ INTEGER +" NOT NULL, "
					+ "rank "+ INTEGER +" NOT NULL, "
					+ "start_x "+ INTEGER +" NOT NULL, "
					+ "start_y "+ INTEGER +" NOT NULL, "
					+ "end_x "+ INTEGER +" NOT NULL, "
					+ "end_y "+ INTEGER +" NOT NULL, "
					+ PRIMARY_KEY+" (parent_id, parent_version, rank)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"elements");
			request("CREATE TABLE "+ schema +"elements ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "class "+ OBJECTID +" NOT NULL, "
					+ "name "+ OBJ_NAME +", "
					+ "documentation "+ TEXT +", "
					+ "type "+ TYPE +", "
					+ "created_by "+ USERNAME +" NOT NULL, "
					+ "created_on "+ DATETIME +" NOT NULL, "
					+ "checkedin_by "+ USERNAME +", "
					+ "checkedin_on "+ DATETIME +", "
					+ "deleted_by "+ USERNAME +", "
					+ "deleted_on "+ DATETIME +", "
					+ "checksum "+ OBJECTID +" NOT NULL,"
					+ PRIMARY_KEY+" (id, version)"
					+ ")");					

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"elements_in_model");
			request("CREATE TABLE "+schema+"elements_in_model ("
					+ "eim_id "+ AUTO_INCREMENT +", "
					+ "element_id "+ OBJECTID +" NOT NULL, "
					+ "element_version "+ INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ OBJECTID +" NOT NULL, "
					+ "model_id "+ OBJECTID +" NOT NULL, "
					+ "model_version "+ INTEGER +" NOT NULL, "
					+ "rank "+ INTEGER +" NOT NULL"
					+ (AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+PRIMARY_KEY+" (eim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+schema+"seq_elements");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+schema+"seq_elements_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+schema+"seq_elements_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+schema+"trigger_elements_in_model");
				request("CREATE OR REPLACE TRIGGER "+schema+"trigger_elements_in_model "
						+ "BEFORE INSERT ON "+schema+"elements_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+schema+"seq_elements_in_model.NEXTVAL INTO :NEW.eim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"folders");
			request("CREATE TABLE "+schema+"folders ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "type "+ INTEGER +" NOT NULL, "
					+ "root_type "+ INTEGER +" NOT NULL, "
					+ "name "+ OBJ_NAME +", "
					+ "documentation "+ TEXT +", "
					+ "created_by "+ USERNAME +", "
					+ "created_on "+ DATETIME +", "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"folders_in_model");
			request("CREATE TABLE "+schema+"folders_in_model ("
					+ "fim_id "+ AUTO_INCREMENT+", "
					+ "folder_id "+ OBJECTID +" NOT NULL, "
					+ "folder_version "+ INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ OBJECTID +", "
					+ "model_id "+ OBJECTID +" NOT NULL, "
					+ "model_version "+ INTEGER +" NOT NULL, "
					+ "rank "+ INTEGER +" NOT NULL"
					+ (AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+PRIMARY_KEY+" (fim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+schema+"seq_folders_in_model");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+schema+"seq_folders_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+schema+"seq_folders_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+schema+"trigger_folders_in_model");
				request("CREATE OR REPLACE TRIGGER "+schema+"trigger_folders_in_model "
						+ "BEFORE INSERT ON "+schema+"folders_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+schema+"seq_folders_in_model.NEXTVAL INTO :NEW.fim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"images");
			request("CREATE TABLE "+schema+"images ("
					+ "path "+ OBJECTID +" NOT NULL, "
					+ "image "+ IMAGE +" NOT NULL, "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (path)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"models");
			request("CREATE TABLE "+schema+"models ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "name "+ OBJ_NAME +" NOT NULL, "
					+ "note "+ TEXT +", "
					+ "purpose "+ TEXT +", "
					+ "created_by "+ USERNAME +" NOT NULL, "
					+ "created_on "+ DATETIME +" NOT NULL, "
					+ "checkedin_by "+ USERNAME +", "
					+ "checkedin_on "+ DATETIME +", "
					+ "deleted_by "+ USERNAME +", "
					+ "deleted_on "+ DATETIME +", "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"properties");
			request("CREATE TABLE "+schema+"properties ("
					+ "parent_id "+OBJECTID +" NOT NULL, "
					+ "parent_version "+ INTEGER +" NOT NULL, "
					+ "rank "+ INTEGER +" NOT NULL, "
					+ "name "+ OBJ_NAME +", "
					+ "value "+ TEXT +", "
					+ PRIMARY_KEY+" (parent_id, parent_version, rank)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"relationships");
			request("CREATE TABLE "+schema+"relationships ("
					+ "id "+OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "class "+ OBJECTID +" NOT NULL, "
					+ "name "+ OBJ_NAME +", "
					+ "documentation "+ TEXT +", "
					+ "source_id "+ OBJECTID +", "
					+ "target_id "+ OBJECTID +", "
					+ "strength "+ STRENGTH +", "
					+ "access_type "+ INTEGER +", "
					+ "created_by "+ USERNAME +" NOT NULL, "
					+ "created_on "+ DATETIME +" NOT NULL, "
					+ "checkedin_by "+ USERNAME +", "
					+ "checkedin_on "+ DATETIME +", "
					+ "deleted_by "+ USERNAME +", "
					+ "deleted_on "+ DATETIME +", "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"relationships_in_model");
			request("CREATE TABLE "+schema+"relationships_in_model ("
					+ "rim_id "+ AUTO_INCREMENT+", "
					+ "relationship_id "+ OBJECTID +" NOT NULL, "
					+ "relationship_version "+ INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ OBJECTID +" NOT NULL, "
					+ "model_id "+ OBJECTID +" NOT NULL, "
					+ "model_version "+ INTEGER +" NOT NULL, "
					+ "rank "+INTEGER +" NOT NULL "
					+ (AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+PRIMARY_KEY+" (rim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+schema+"seq_relationships");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+schema+"seq_relationships_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+schema+"seq_relationships_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+schema+"trigger_relationships_in_model");
				request("CREATE OR REPLACE TRIGGER "+schema+"trigger_relationships_in_model "
						+ "BEFORE INSERT ON "+schema+"relationships_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+schema+"seq_relationships_in_model.NEXTVAL INTO :NEW.rim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"views");
			request("CREATE TABLE "+schema+"views ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "class "+ OBJECTID +" NOT NULL, "
					+ "name "+ OBJ_NAME +", "
					+ "documentation "+ TEXT +" , "
					+ "hint_content "+ TEXT +", "
					+ "hint_title "+ OBJ_NAME +", "
					+ "created_by "+ USERNAME +" NOT NULL, "
					+ "created_on "+ DATETIME +" NOT NULL, "
					+ "background "+ INTEGER +", "
					+ "connection_router_type "+ INTEGER +" NOT NULL, "
					+ "viewpoint "+ OBJECTID +", "
					+ "screenshot "+ IMAGE +", "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"views_connections");
			request("CREATE TABLE "+schema+"views_connections ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "container_id "+ OBJECTID +" NOT NULL, "
					+ "view_id "+ OBJECTID +" NOT NULL, "
					+ "view_version "+ INTEGER +" NOT NULL, "
					+ "class "+ OBJECTID +" NOT NULL, "
					+ "name "+ OBJ_NAME +", "					// connection must store a name because all of them are not linked to a relationship
					+ "documentation "+ TEXT +", "
					+ "is_locked "+ BOOLEAN +", "
					+ "line_color "+ COLOR +", "
					+ "line_width "+ INTEGER +", "
					+ "font "+ FONT +", "
					+ "font_color "+ COLOR +", "
					+ "relationship_id "+ OBJECTID +", "
					+ "relationship_version "+ INTEGER +", "
					+ "source_connections "+ TEXT + ", "
					+ "target_connections "+ TEXT + ", "
					+ "source_object_id "+ OBJECTID +", "
					+ "target_object_id "+ OBJECTID +", "
					+ "text_position "+ INTEGER +", "
					+ "type "+ INTEGER +", "
					+ "rank "+ INTEGER +" NOT NULL, "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version, container_id)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"views_in_model");
			request("CREATE TABLE "+schema+"views_in_model ("
					+ "vim_id "+ AUTO_INCREMENT +", "
					+ "view_id "+ OBJECTID +" NOT NULL, "
					+ "view_version "+ INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ OBJECTID +" NOT NULL, "
					+ "model_id "+ OBJECTID +" NOT NULL, "
					+ "model_version "+ INTEGER +" NOT NULL, "
					+ "rank "+ INTEGER
					+ (AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+PRIMARY_KEY+" (vim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+schema+"seq_views");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+schema+"seq_views_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+schema+"seq_views_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+schema+"trigger_views_in_model");
				request("CREATE OR REPLACE TRIGGER "+schema+"trigger_views_in_model "
						+ "BEFORE INSERT ON "+schema+"views_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+schema+"seq_views_in_model.NEXTVAL INTO :NEW.vim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+schema+"views_objects");
			request("CREATE TABLE "+schema+"views_objects ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "container_id "+ OBJECTID +" NOT NULL, "
					+ "view_id "+ OBJECTID +" NOT NULL, "
					+ "view_version "+ INTEGER +" NOT NULL, "
					+ "class "+ OBJECTID +" NOT NULL, "
					+ "element_id "+ OBJECTID +", "
					+ "element_version "+ INTEGER +", "
					+ "diagram_ref_id "+ OBJECTID +", "
					+ "border_color "+ COLOR +", "
					+ "border_type "+ INTEGER +", "
					+ "content "+ TEXT +", "
					+ "documentation "+ TEXT +", "
					+ "hint_content "+ TEXT +", "
					+ "hint_title "+ OBJ_NAME +", "
					+ "is_locked "+ BOOLEAN +", "
					+ "image_path "+ OBJECTID +", "
					+ "image_position "+ INTEGER +", "
					+ "line_color "+ COLOR +", "
					+ "line_width "+ INTEGER +", "
					+ "fill_color "+ COLOR +", "
					+ "font "+ FONT +", "
					+ "font_color "+ COLOR +", "
					+ "name "+ OBJ_NAME +", "
					+ "notes "+ TEXT +", "
					+ "source_connections "+ TEXT + ", "
					+ "target_connections "+ TEXT + ", "
					+ "text_alignment "+ INTEGER +", "
					+ "text_position "+ INTEGER +", "
					+ "type "+ INTEGER +", "
					+ "x "+ INTEGER +", "
					+ "y "+ INTEGER +", "
					+ "width "+ INTEGER +", "
					+ "height "+ INTEGER +", "
					+ "rank "+ INTEGER +" NOT NULL, "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version, container_id)"
					+ ")");

			commit();
			setAutoCommit(true);

			DBGui.popup(Level.INFO,"The database has been successfully initialized.");

		} catch (SQLException err) {
			rollback();
			setAutoCommit(true);
			// we delete the archi_plugin table because for some databases, DDL cannot be rolled back
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") )
				request("BEGIN EXECUTE IMMEDIATE 'DROP TABLE "+schema+"database_version'; EXCEPTION WHEN OTHERS THEN NULL; END;");
			else
				request("DROP TABLE IF EXISTS "+schema+"database_version");
			throw err;
		}

	}


	/**
	 * Upgrades the database
	 * @throws ClassNotFoundException 
	 */
	private void upgradeDatabase(int fromVersion) throws SQLException, ClassNotFoundException {
		String COLUMN = DBPlugin.areEqual(databaseEntry.getDriver(), "sqlite") ? "COLUMN" : "";

		// convert from version 200 to 201 :
		//      - add a blob column into the views table
		if ( fromVersion == 200 ) {
			setAutoCommit(false);
			request("ALTER TABLE "+schema+"views ADD "+COLUMN+" screenshot "+IMAGE);
			
			request("UPDATE "+schema+"database_version SET version = 201 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			setAutoCommit(true);

			fromVersion = 201;
		}

		// convert from version 201 to 202 :
		//      - add a text_position column in the views_connections table
		//      - add source_connections and target_connections to views_objects and views_connections tables
		if ( fromVersion == 201 ) {
			setAutoCommit(false);
			request("ALTER TABLE "+schema+"views_connections ADD "+COLUMN+" text_position "+INTEGER);
			request("ALTER TABLE "+schema+"views_objects ADD "+COLUMN+" source_connections "+TEXT);
			request("ALTER TABLE "+schema+"views_objects ADD "+COLUMN+" target_connections "+TEXT);
			request("ALTER TABLE "+schema+"views_connections ADD "+COLUMN+" source_connections "+TEXT);
			request("ALTER TABLE "+schema+"views_connections ADD "+COLUMN+" target_connections "+TEXT);
			
			request("UPDATE "+schema+"database_version SET version = 202 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			setAutoCommit(true);

			fromVersion = 202;
		}
		
		// convert from version 202 to 203 :
		//      - add a text_position column in the views_connections table
		//      - add source_connections and target_connections to views_objects and views_connections tables
		if ( fromVersion == 202 ) {
			setAutoCommit(false);
			request("ALTER TABLE "+schema+"views_connections ADD "+COLUMN+" relationship_version "+INTEGER);
			request("UPDATE "+schema+"views_connections SET relationship_version = 1");
			
			request("ALTER TABLE "+schema+"views_objects ADD "+COLUMN+" element_version "+INTEGER);
			request("UPDATE "+schema+"views_objects SET element_version = 1");
			
			request("UPDATE "+schema+"database_version SET version = 203 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			setAutoCommit(true);

			fromVersion = 203;
		}
		
		// convert from version 203 to 204 :
		//      - add a checksum to the model
		//
		// unfortunately, some databases do not support to alter an existing column
		// so we create a new table
		if ( fromVersion == 203 ) {
			setAutoCommit(false);
			
			String[] columns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "checkedin_by", "checkedin_on", "deleted_by", "deleted_on", "checksum"};
			
			request("ALTER TABLE "+schema+"models RENAME TO "+schema+"models_old");
			
			request("CREATE TABLE "+schema+"models ("
					+ "id "+ OBJECTID +" NOT NULL, "
					+ "version "+ INTEGER +" NOT NULL, "
					+ "name "+ OBJ_NAME +" NOT NULL, "
					+ "note "+ TEXT +", "
					+ "purpose "+ TEXT +", "
					+ "created_by "+ USERNAME +" NOT NULL, "
					+ "created_on "+ DATETIME +" NOT NULL, "
					+ "checkedin_by "+ USERNAME +", "
					+ "checkedin_on "+ DATETIME +", "
					+ "deleted_by "+ USERNAME +", "
					+ "deleted_on "+ DATETIME +", "
					+ "checksum "+ OBJECTID +" NOT NULL, "
					+ PRIMARY_KEY+" (id, version)"
					+ ")");
			
			ResultSet result = select("SELECT id, version, name, note, purpose, created_by, created_on, checkedin_by, checkedin_on, deleted_by, deleted_on FROM models_old");
			while ( result.next() ) {
				StringBuilder checksumBuilder = new StringBuilder();
				DBChecksum.append(checksumBuilder, "id", result.getString("id"));
				DBChecksum.append(checksumBuilder, "name", result.getString("name"));
				DBChecksum.append(checksumBuilder, "purpose", result.getString("purpose"));
				DBChecksum.append(checksumBuilder, "note", result.getString("note"));
				String checksum;
				try {
					checksum = DBChecksum.calculateChecksum(checksumBuilder);
				} catch (Exception err) {
					DBGui.popup(Level.FATAL, "Failed to calculate models checksum.", err);
					rollback();
					return;
				}
				insert(schema+"models", columns,
						result.getString("id"),
						result.getInt("version"),
						result.getString("name"),
						result.getString("note"),
						result.getString("purpose"),
						result.getString("created_by"),
						result.getTimestamp("created_on"),
						result.getString("checkedin_by"),
						result.getTimestamp("checkedin_on"),
						result.getString("deleted_by"),
						result.getTimestamp("deleted_on"),
						checksum
						);
			}
			result.close();
			
			request("UPDATE "+schema+"database_version SET version = 204 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			
			// SQLite refuses to drop the table if we do not close the connection and reopen it
			connection.close();
			openConnection();
			request("DROP TABLE "+schema+"models_old");
			commit();

			setAutoCommit(true);

			fromVersion = 204;
		}
	}

	/**
	 * HelperMethod to construct the PreparedStatement from the specified request and all its parameters
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	private final <T> void constructStatement(PreparedStatement pstmt, String request, T... parameters) throws SQLException {
		StringBuilder debugRequest = new StringBuilder();
		String[] splittedRequest = request.split("\\?");

		int requestRank = 0;
		int parameterRank = 0;
		while (parameterRank < parameters.length) {
			if ( logger.isTraceEnabled() ) debugRequest.append(splittedRequest[requestRank]);

			if ( parameters[parameterRank] == null ) {
				if ( logger.isTraceEnabled() ) debugRequest.append("null");
				pstmt.setString(++requestRank, null);
			} else if ( parameters[parameterRank] instanceof String ) {
				if ( logger.isTraceEnabled() ) debugRequest.append("'"+parameters[parameterRank]+"'");
				pstmt.setString(++requestRank, (String)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof Integer ) {
				if ( logger.isTraceEnabled() ) debugRequest.append(parameters[parameterRank]);
				pstmt.setInt(++requestRank, (int)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof Timestamp ) {
				if ( logger.isTraceEnabled() ) debugRequest.append(String.valueOf((Timestamp)parameters[parameterRank]));
				pstmt.setTimestamp(++requestRank, (Timestamp)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof Boolean ) {
				if ( logger.isTraceEnabled() ) debugRequest.append(String.valueOf((boolean)parameters[parameterRank]));
				pstmt.setBoolean(++requestRank, (Boolean)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof ArrayList<?> ){
				for(int i = 0; i < ((ArrayList<String>)parameters[parameterRank]).size(); ++i) {
					if ( logger.isTraceEnabled() ) {
						if ( i != 0 )
							debugRequest.append(",");
						debugRequest.append("'"+((ArrayList<String>)parameters[parameterRank]).get(i)+"'");
					}
					pstmt.setString(++requestRank, ((ArrayList<String>)parameters[parameterRank]).get(i));
				}
			} else if ( parameters[parameterRank] instanceof byte[] ) {
				try  {
					pstmt.setBinaryStream(++requestRank, new ByteArrayInputStream((byte[])parameters[parameterRank]), ((byte[])parameters[parameterRank]).length);
					if ( logger.isTraceEnabled() ) debugRequest.append("[image as stream ("+((byte[])parameters[parameterRank]).length+" bytes)]");
				} catch (Exception err) {
					pstmt.setString(++requestRank, Base64.getEncoder().encodeToString((byte[])parameters[parameterRank]));
					if ( logger.isTraceEnabled() ) debugRequest.append("[image as base64 string ("+((byte[])parameters[parameterRank]).length+" bytes)]");
				}

			} else {
				if ( logger.isTraceEnabled() ) logger.trace(request);
				throw new SQLException("Unknown "+parameters[parameterRank].getClass().getSimpleName()+" parameter in SQL select.");
			}
			++parameterRank;
		}
		if ( logger.isTraceEnabled() ) {
			if ( requestRank < splittedRequest.length )
				debugRequest.append(splittedRequest[requestRank]);
			logger.trace("database request : "+debugRequest.toString());
		}
	}

	/**
	 * HashMap to store the JDBC preparedStatements
	 */
	private Map<String, PreparedStatement> preparedStatementMap = new HashMap<String, PreparedStatement>();

	/**
	 * Wrapper to generate and execute a SELECT request in the database<br>
	 * One may use '?' in the request and provide the corresponding values as parameters (at the moment, only strings are accepted)<br>
	 * The connection to the database should already exist 
	 * @return the ResultSet with the data read from the database
	 */
	@SafeVarargs
	public final <T> ResultSet select(String request, T... parameters) throws SQLException {
		assert (connection != null);

		PreparedStatement pstmt;
		try {
			pstmt = preparedStatementMap.get(request);
			if ( pstmt == null ) {
				pstmt = connection.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				preparedStatementMap.put(request, pstmt);
			} else {
				pstmt.clearParameters();
			}
		} catch (SQLException err) {
			// in case of an SQLException, we log the raw request to ease the debug process
			if ( logger.isTraceEnabled() ) logger.trace("SQL Exception for database request : "+request);
			throw err;
		}

		constructStatement(pstmt, request, parameters);

		return pstmt.executeQuery();
	}

	/**
	 * wrapper to generate and execute a INSERT request in the database
	 * One may just provide the column names and the corresponding values as parameters
	 * the wrapper automatically generates the VALUES part of the request 
	 * @return The number of lines inserted in the table
	 */
	@SafeVarargs
	public final <T> int insert(String table, String[] columns, T...parameters) throws SQLException {
		assert (connection != null);

		StringBuilder cols = new StringBuilder();
		StringBuilder values = new StringBuilder();
		ArrayList<T> newParameters = new ArrayList<T>();

		for (int i=0 ; i < columns.length ; ++i) {
			if ( parameters[i] != null ) {
				if ( cols.length() != 0 ) {
					cols.append(", ");
					values.append(", ");
				}
				cols.append(columns[i]);
				values.append("?");
				newParameters.add(parameters[i]);
			}
		}

		if ( (cols.length() == 0) || (values.length() == 0) )
			throw new SQLException("SQL request cannot have all its parameters null.");

		return request("INSERT INTO "+table+" ("+cols.toString()+") VALUES ("+values.toString()+")", newParameters.toArray());
	}

	/**
	 * wrapper to execute an INSERT or UPDATE request in the database
	 * One may use '?' in the request and provide the corresponding values as parameters (strings, integers, booleans and byte[] are accepted)
	 * @return the number of lines impacted by the request
	 */
	@SafeVarargs
	public final <T> int request(String request, T... parameters) throws SQLException {
		assert (connection != null);
		int rowCount = 0;

		if ( parameters.length == 0 ) {		// no need to use a PreparedStatement
			if ( logger.isTraceEnabled() ) logger.trace(request);

			Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			rowCount = stmt.executeUpdate(request);
			stmt.close();
			stmt=null;
		} else {
			PreparedStatement pstmt = preparedStatementMap.get(request);

			if ( pstmt == null || pstmt.isClosed() ) {
				pstmt = connection.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				preparedStatementMap.put(request, pstmt);
			} else {
				pstmt.clearParameters();
				pstmt.clearWarnings();
			}

			constructStatement(pstmt, request, parameters);

			// on PostGreSQL databases, we can only send new requests if we rollback the transaction that caused the exception
			Savepoint savepoint = null;
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "postgresql") ) savepoint = connection.setSavepoint();
			try {
				rowCount = pstmt.executeUpdate();
			} catch (SQLException e) {
				if ( savepoint != null ) {
					try {
						connection.rollback(savepoint);
						if ( logger.isTraceEnabled() ) logger.trace("Rolled back to savepoint");
					} catch (SQLException e2) { logger.error("Failed to rollback to savepoint", e2); };
				}
				
				// on sqlite databases, the prepared statement must be closed (then re-created) after the exception else we've got "statement is not executing" error messages
				if ( DBPlugin.areEqual(databaseEntry.getDriver(), "sqlite") ) {
					preparedStatementMap.remove((Object)pstmt);
					pstmt.close();
				}
				throw e;
			} finally {
				if ( savepoint != null ) connection.releaseSavepoint(savepoint);
			}
		}

		return rowCount;
	}

	/**
	 * Gets the timestamp of the beginning of the current transaction<br>
	 * if the database is in auto-commit mode, then it returns the current time. 
	 */
	public Timestamp getLastTransactionTimestamp() throws SQLException {
		// if autocommit is on, then each call will return the current time
		if ( connection.getAutoCommit() )
			return new Timestamp(Calendar.getInstance().getTime().getTime());

		// if autoCommit is off, then we return the timestamp of the last commit (and therefore the timestamp of the beginning of the current transaction)
		return lastTransactionTimestamp;
	}

	/**
	 * Sets the auto-commit mode of the database
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Setting database auto commit to "+String.valueOf(autoCommit));
		connection.setAutoCommit(autoCommit);
		if ( autoCommit )
			lastTransactionTimestamp = null;                                                         // all the request will have their own timetamp
		else
			lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());    // all the requests will have the same timestamp
	}

	/**
	 * Commits the current transaction
	 */
	public void commit() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Commiting database transaction.");
		connection.commit();
		lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
	}

	/**
	 * Rollbacks the current transaction
	 */
	public void rollback() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Rollbacking database transaction.");
		connection.rollback();
	}

	///////////////////////////////////////////////////////////////////////////////////
	//                                                                               //
	//   I M P O R T   S P E C I F I C   V A R I A B L E S   A N D   M E T H O D S   //
	//                                                                               //
	///////////////////////////////////////////////////////////////////////////////////

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
	public HashMap<String, Object> getObject(String id, String clazz, int version) throws Exception {
		ResultSet result = null;
		if ( version == 0 ) {
			// because of PostGreSQL, we need to split the request in two
			if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) result = select("SELECT id, version, class, name, documentation, type, created_by, created_on, checksum FROM "+schema+"elements e WHERE version = (SELECT MAX(version) FROM "+schema+"elements WHERE id = e.id) AND id = ?", id);
			else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) result = select("SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM "+schema+"relationships r WHERE version = (SELECT MAX(version) FROM "+schema+"relationships WHERE id = r.id) AND id = ?", id);
			else if ( DBPlugin.areEqual(clazz,  "IFolder") ) result = select("SELECT id, version, type, name, documentation, created_by, created_on, checksum FROM folders f WHERE version = (SELECT MAX(version) FROM "+schema+"folders WHERE id = f.id) AND id = ?", id);
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) result = select("SELECT id, version, class, name, documentation, hint_content, hint_title, created_by, created_on, background, connection_router_type, viewpoint, checksum FROM "+schema+"views v WHERE version = (SELECT MAX(version) FROM "+schema+"views WHERE id = v.id) AND id = ?", id);
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModelArchimateObject") ) result = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height, checksum FROM "+schema+"views_objects v WHERE version = (SELECT MAX(version) FROM "+schema+"views_objects WHERE id = v.id) AND id = ?", id);		
			else throw new Exception("Do not know how to get a "+clazz+" from the database.");
		} else {        
			if ( DBPlugin.areEqual(clazz,  "IArchimateElement") ) result = select("SELECT id, version, class, name, documentation, type, created_by, created_on, checksum FROM "+schema+"elements WHERE id = ? AND version = ?", id, version);
			else if ( DBPlugin.areEqual(clazz,  "IArchimateRelationship") ) result = select("SELECT id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM "+schema+"relationships WHERE id = ? AND version = ?", id, version);
			else if ( DBPlugin.areEqual(clazz,  "IFolder") ) result = select("SELECT id, version, type, name, documentation, created_by, created_on, checksum FROM "+schema+"folders WHERE id = ? AND version = ?", id, version);
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) result = select("SELECT id, version, class, name, documentation, hint_content, hint_title, created_by, created_on, background, connection_router_type, viewpoint, checksum FROM "+schema+"views WHERE id = ? AND version = ?", id, version);
			else if ( DBPlugin.areEqual(clazz,  "IDiagramModelArchimateObject") ) result = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height, checksum FROM "+schema+"views_objects WHERE id = ? AND version = ?", id, version);
			else throw new Exception("Do not know how to get a "+clazz+" from the database.");
		}

		HashMap<String, Object> hashResult;
		if ( result.next() ) {
			version = result.getInt("version");
			hashResult = resultSetToHashMap(result);

			if ( DBPlugin.areEqual(clazz,  "IFolder") ) hashResult.put("class", "Folder");                  // the folders table does not have a class column, so we add the property by hand

			result.close();
			result=null;


			// properties
			result = select("SELECT count(*) as count_properties FROM "+schema+"properties WHERE parent_id = ? AND parent_version = ?", id, version);
			result.next();
			String[][] databaseProperties = new String[result.getInt("count_properties")][2];
			result.close();
			result=null;
	
			result = select("SELECT name, value FROM "+schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version );
			int i = 0;
			while ( result.next() ) {
				databaseProperties[i++] = new String[] { result.getString("name"), result.getString("value") };
			}
			hashResult.put("properties", databaseProperties);
			result.close();
			result=null;
			
			//TODO: gather the views objects if it is a view
			if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) {
				int countChildren;
				
				result = select("select count(*) AS count_children FROM "+schema+"views_objects WHERE view_id = ? AND view_version = ?", id, version);
				result.next();
				countChildren = result.getInt("count_children");
				result.close();
				result = null;
				
				
				@SuppressWarnings("unchecked")
				HashMap<String, Object>[] children = new HashMap[countChildren];
				result = select("select id, version FROM "+schema+"views_objects WHERE view_id = ? AND view_version = ?", id, version);
				i=0;
				while ( result.next() ) {
					children[i++] = getObject(result.getString("id"), "IDiagramModelArchimateObject", result.getInt("version"));
				}
				hashResult.put("children", children);
			}
			
			// bendpoints
			result = select("SELECT count(*) as count_bendpoints FROM "+schema+"bendpoints WHERE parent_id = ? AND parent_version = ?", id, version);
			result.next();
			Integer[][] databaseBendpoints = new Integer[result.getInt("count_bendpoints")][4];
			result.close();
			result=null;
			
			result = select("SELECT start_x, start_y, end_x, end_y FROM "+schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version );
			int j = 0;
			while ( result.next() ) {
				databaseBendpoints[j++] = new Integer[] { result.getInt("start_x"), result.getInt("start_y"), result.getInt("end_x"), result.getInt("end_y") };
			}
			hashResult.put("bendpoints", databaseBendpoints);
		} else {
			hashResult = new HashMap<String, Object>();
		}
		
		result.close();
		result=null;

		return hashResult;
	}

	/**
	 * Creates a HashMap from a ResultSet
	 */
	public HashMap<String, Object> resultSetToHashMap(ResultSet rs) throws SQLException {
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

	/**
	 * Gets the list of models and fills-in the tblModels table
	 * @param filter
	 * @param tblModels
	 * @throws Exception
	 */
	public void getModels(String filter, Table tblModels) throws Exception {
		//TODO: separate the data and GUI functions : do not fill in the tblModels table in this method
		ResultSet result;

		// We do not use a GROUP BY because it does not give the expected result on PostGresSQL ...   
		if ( filter==null || filter.length()==0 )
			result = select("SELECT id, name, version FROM "+schema+"models m WHERE version = (SELECT MAX(version) FROM "+schema+"models WHERE id = m.id) ORDER BY name");
		else
			result = select("SELECT id, name, version FROM "+schema+"models m WHERE version = (SELECT MAX(version) FROM "+schema+"models WHERE id = m.id) AND UPPER(name) like UPPER(?) ORDER BY name", filter);


		while ( result.next() && result.getString("id") != null ) {
			if (logger.isTraceEnabled() ) logger.trace("found model \""+result.getString("name")+"\"");
			TableItem tableItem = new TableItem(tblModels, SWT.BORDER);
			tableItem.setText(result.getString("name"));
			tableItem.setData("id", result.getString("id"));
		}
		tblModels.layout();
		tblModels.setVisible(true);
		tblModels.setLinesVisible(true);
		tblModels.setRedraw(true);
		if (logger.isTraceEnabled() ) logger.trace("found "+tblModels.getItemCount()+" model"+(tblModels.getItemCount()>1?"s":"")+" in total");

		result.close();
		result=null;

		if ( tblModels.getItemCount() != 0 ) {
			tblModels.setSelection(0);
			tblModels.notifyListeners(SWT.Selection, new Event());      // calls database.getModelVersions()
		}
	}
	
	public String getModelId(String modelName, boolean ignoreCase) throws Exception {
	    ResultSet result;
	    
	    // Using a GROUP BY on PostGresSQL does not give the expected result ...   
        if ( ignoreCase )
            result = select("SELECT id FROM "+schema+"models m WHERE UPPER(name) = UPPER(?)", modelName);
        else
            result = select("SELECT id FROM "+schema+"models m WHERE name = ?", modelName);

        if ( result.next() ) 
            return result.getString("id");
        
        return null;
	}

	/**
	 * Gets the versions of the selected model and fills-in the tblVersions table
	 * @param filter
	 * @param tblModels
	 * @throws Exception
	 */
	public void getModelVersions(String id, Table tblModelVersions) throws Exception {
		//TODO: separate the data and GUI functions : do not fill in the tblVersions table in this method
		ResultSet result = select("SELECT version, created_by, created_on, name, note, purpose FROM "+schema+"models WHERE id = ? ORDER BY version DESC", id);
		TableItem tableItem = null;

		tblModelVersions.removeAll();
		while ( result.next() ) {
			if (logger.isTraceEnabled() ) logger.trace("found version \""+result.getString("version")+"\"");
			
			if ( tableItem == null ) {
				// if the first line, then we add the "latest version"
				tableItem = new TableItem(tblModelVersions, SWT.NULL);
				tableItem.setText(0, "");
				tableItem.setText(1, "Now");
				tableItem.setText(2, "");
				tableItem.setData("name", result.getString("name"));
				tableItem.setData("note", result.getString("note")!=null ? result.getString("note") : "");
				tableItem.setData("purpose", result.getString("purpose")!=null ? result.getString("purpose") : "");
			}
			tableItem = new TableItem(tblModelVersions, SWT.NULL);
			tableItem.setText(0, result.getString("version"));
			tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(result.getTimestamp("created_on")));
			tableItem.setText(2, result.getString("created_by"));
			tableItem.setData("name", result.getString("name"));
			tableItem.setData("note", result.getString("note")!=null ? result.getString("note") : "");
			tableItem.setData("purpose", result.getString("purpose")!=null ? result.getString("purpose") : "");
		}

		result.close();
		result=null;

		if ( tblModelVersions.getItemCount() != 0 ) {
			tblModelVersions.setSelection(0);
			tblModelVersions.notifyListeners(SWT.Selection, new Event());       // calls database.getModelVersions()
		}
	}



	private HashSet<String> allImagePaths;

	private int countElementsToImport = 0;
	public int countElementsToImport() { return countElementsToImport; }

	private int countElementsImported = 0;
	public int countElementsImported() { return countElementsImported; }

	private int countRelationshipsToImport = 0;
	public int countRelationshipsToImport() { return countRelationshipsToImport; }

	private int countRelationshipsImported = 0;
	public int countRelationshipsImported() { return countRelationshipsImported; }

	private int countFoldersToImport = 0;
	public int countFoldersToImport() { return countFoldersToImport; }

	private int countFoldersImported = 0;
	public int countFoldersImported() { return countFoldersImported; }

	private int countViewsToImport = 0;
	public int countViewsToImport() { return countViewsToImport; }

	private int countViewsImported = 0;
	public int countViewsImported() { return countViewsImported; }

	private int countViewObjectsToImport = 0;
	public int countViewObjectsToImport() { return countViewObjectsToImport; }

	private int countViewObjectsImported = 0;
	public int countViewObjectsImported() { return countViewObjectsImported; }

	private int countViewConnectionsToImport = 0;
	public int countViewConnectionsToImport() { return countViewConnectionsToImport; }

	private int countViewConnectionsImported = 0;
	public int countViewConnectionsImported() { return countViewConnectionsImported; }

	private int countImagesToImport = 0;
	public int countImagesToImport() { return countImagesToImport; }

	private int countImagesImported = 0;
	public int countImagesImported() { return countImagesImported; }
	
	private String importFoldersRequest;
	private String importElementsRequest;
	private String importRelationshipsRequest;
	private String importViewsRequest;
	private String importViewsObjectsRequest;
	private String importViewsConnectionsRequest;

	/**
	 * Import the model metadata from the database
	 */
	public int importModelMetadata(ArchimateModel model) throws Exception {
		// reseting the model's counters
		model.resetCounters();

		ResultSet result;
		
		if ( model.getCurrentVersion().getVersion() == 0 ) {
		    result = select("SELECT MAX(version) AS version FROM "+schema+"models WHERE id = ?", model.getId());
		    if ( result.next() ) {
		        model.getCurrentVersion().setVersion(result.getInt("version"));
		    }
		    result.close();
		}

	    result = select("SELECT name, purpose FROM "+schema+"models WHERE id = ? AND version = ?", model.getId(), model.getCurrentVersion());
		
		//TODO : manage the "real" model metadata :-)

		result.next();

		model.setPurpose((String)result.getString("purpose"));

		result.close();
		result=null;

		importProperties(model);

		String documentation = DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ? "TO_CHAR(documentation)" : "documentation";
		
		String elementsDocumentation = DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ? "TO_CHAR(elements.documentation) AS documentation" : "elements.documentation";
		if ( model.getImportLatestVersion() ) {
			importElementsRequest = "SELECT DISTINCT element_id, parent_folder_id, version, class, name, type, documentation, created_on"+
									" FROM ("+
									"	SELECT elements_in_model.element_id, elements_in_model.parent_folder_id, elements.version, elements.class, elements.name, elements.type, "+elementsDocumentation+", elements.created_on"+
									"	FROM "+schema+"elements_in_model"+
									"	JOIN "+schema+"elements ON elements.id = elements_in_model.element_id AND elements.version = (SELECT MAX(version) FROM "+schema+"elements WHERE elements.id = elements_in_model.element_id)"+
									"	WHERE model_id = ? AND model_version = ?"+
									" UNION"+
									"	SELECT views_objects.element_id, null as parent_folder_id, elements.version, elements.class, elements.name, elements.type, "+elementsDocumentation+", elements.created_on"+
									"	FROM "+schema+"views_in_model"+
									"	JOIN "+schema+"views ON views.id = views_in_model.view_id"+
									"	JOIN "+schema+"views_objects ON views_objects.view_id = views.id AND views_objects.version = views.version"+
									"	JOIN "+schema+"elements ON elements.id = views_objects.element_id AND elements.version = (SELECT MAX(version) FROM "+schema+"elements WHERE elements.id = views_objects.element_id)"+
									"	WHERE model_id = ? AND model_version = ? AND element_id IS NOT null"+
									"   AND views_objects.element_id NOT IN ("+
									"      SELECT elements_in_model.element_id"+
									"	   FROM "+schema+"elements_in_model"+
									"	   JOIN "+schema+"elements ON elements.id = elements_in_model.element_id AND elements.version = (SELECT MAX(version) FROM "+schema+"elements WHERE elements.id = elements_in_model.element_id)"+
									"	   WHERE model_id = ? AND model_version = ?"+
									"   )"+
									" ) elements GROUP BY element_id, parent_folder_id, version, class, name, type, documentation, created_on";
			result = select("SELECT COUNT(*) AS countElements FROM ("+importElementsRequest+") elts"
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					);
		} else {
			importElementsRequest = "SELECT DISTINCT elements_in_model.element_id, elements_in_model.parent_folder_id, elements.version, elements.class, elements.name, elements.type, "+elementsDocumentation+", elements.created_on"+
									" FROM "+schema+"elements_in_model"+
									" JOIN "+schema+"elements ON elements.id = elements_in_model.element_id AND elements.version = elements_in_model.element_version"+
									" WHERE model_id = ? AND model_version = ?"+
									" GROUP BY element_id, parent_folder_id, version, class, name, type, "+documentation+", created_on";
			result = select("SELECT COUNT(*) AS countElements FROM ("+importElementsRequest+") elts"
					,model.getId()
					,model.getCurrentVersion()
					);
		}

		result.next();
		countElementsToImport = result.getInt("countElements");
		countElementsImported = 0;
		result.close();
		result=null;

		String relationshipsDocumentation = DBPlugin.areEqual(databaseEntry.getDriver(), "oracle") ? "TO_CHAR(relationships.documentation) AS documentation" : "relationships.documentation";
		if ( model.getImportLatestVersion() ) {
			importRelationshipsRequest = "SELECT DISTINCT relationship_id, parent_folder_id, version, class, name, documentation, source_id, target_id, strength, access_type, created_on"+
										 " FROM ("+
					 					 "	SELECT relationships_in_model.relationship_id, relationships_in_model.parent_folder_id, relationships.version, relationships.class, relationships.name, "+relationshipsDocumentation+", relationships.source_id, relationships.target_id, relationships.strength, relationships.access_type, relationships.created_on"+
										 "	FROM "+schema+"relationships_in_model"+
										 "	JOIN "+schema+"relationships ON relationships.id = relationships_in_model.relationship_id AND relationships.version = (SELECT MAX(version) FROM "+schema+"relationships WHERE relationships.id = relationships_in_model.relationship_id)"+
										 "	WHERE model_id = ? AND model_version = ?"+
										 " UNION"+
										 "	SELECT views_connections.relationship_id, null as parent_folder_id, relationships.version, relationships.class, relationships.name, "+relationshipsDocumentation+", relationships.source_id, relationships.target_id, relationships.strength, relationships.access_type, relationships.created_on"+
										 "	FROM "+schema+"views_in_model"+
										 "	JOIN "+schema+"views ON views.id = views_in_model.view_id"+
										 "	JOIN "+schema+"views_connections ON views_connections.view_id = views.id AND views_connections.version = views.version"+
										 "	JOIN "+schema+"relationships ON relationships.id = views_connections.relationship_id AND relationships.version = (SELECT MAX(version) FROM "+schema+"relationships WHERE relationships.id = views_connections.relationship_id)"+
										 "	WHERE model_id = ? AND model_version = ? and relationship_id IS NOT null"+
										 "  AND views_connections.relationship_id NOT IN ("+
										 "     SELECT relationships_in_model.relationship_id"+
										 "	   FROM "+schema+"relationships_in_model"+
										 "	   JOIN "+schema+"relationships ON relationships.id = relationships_in_model.relationship_id AND relationships.version = (SELECT MAX(version) FROM "+schema+"relationships WHERE relationships.id = relationships_in_model.relationship_id)"+
										 "	   WHERE model_id = ? AND model_version = ?"+
										 "  )"+
										 " ) relationships GROUP BY relationship_id, parent_folder_id, version, class, name, documentation, source_id, target_id, strength, access_type, created_on";
			result = select("SELECT COUNT(*) AS countRelationships FROM ("+importRelationshipsRequest+") relts"
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					);
		} else {
			importRelationshipsRequest = "SELECT relationships_in_model.relationship_id, relationships_in_model.parent_folder_id, relationships.version, relationships.class, relationships.name, "+relationshipsDocumentation+", relationships.source_id, relationships.target_id, relationships.strength, relationships.access_type, relationships.created_on"+
										 " FROM "+schema+"relationships_in_model"+
										 " INNER JOIN "+schema+"relationships ON relationships.id = relationships_in_model.relationship_id AND relationships.version = relationships_in_model.relationship_version"+
										 " WHERE model_id = ? AND model_version = ?"+
										 " GROUP BY relationship_id, parent_folder_id, version, class, name, "+documentation+", source_id, target_id, strength, access_type, created_on";
			result = select("SELECT COUNT(*) AS countRelationships FROM ("+importRelationshipsRequest+") relts"
					,model.getId()
					,model.getCurrentVersion()
					);
		}
		result.next();
		countRelationshipsToImport = result.getInt("countRelationships");
		countRelationshipsImported = 0;
		result.close();
		result=null;

		if ( model.getImportLatestVersion() ) {
			importFoldersRequest = "SELECT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on"+
									" FROM "+schema+"folders_in_model"+
									" JOIN "+schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = (SELECT MAX(version) FROM "+schema+"folders WHERE folders.id = folders_in_model.folder_id)"+
									" WHERE model_id = ? AND model_version = ?";
		} else {
			importFoldersRequest = "SELECT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on"+
									" FROM "+schema+"folders_in_model"+
									" JOIN "+schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = folders_in_model.folder_version"+
									" WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countFolders FROM ("+importFoldersRequest+") fldrs"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		countFoldersToImport = result.getInt("countFolders");
		countFoldersImported = 0;
		result.close();
		result=null;
		importFoldersRequest += " ORDER BY folders_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			importViewsRequest = "SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on"+
								 " FROM "+schema+"views_in_model"+
								 " JOIN "+schema+"views ON views.id = views_in_model.view_id AND views.version = (select max(version) from "+schema+"views where views.id = views_in_model.view_id)"+
								 " WHERE model_id = ? AND model_version = ?";
		} else {
			importViewsRequest = "SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on"+
								 " FROM "+schema+"views_in_model"+
								 " JOIN "+schema+"views ON views.id = views_in_model.view_id AND views.version = views_in_model.view_version"+
								 " WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countViews FROM ("+importViewsRequest+") vws"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		countViewsToImport = result.getInt("countViews");
		countViewsImported = 0;
		result.close();
		result=null;
		importViewsRequest += " ORDER BY views_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			importViewsObjectsRequest = "SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height"+
										" FROM "+schema+"views_in_model"+
										" JOIN "+schema+"views_objects ON views_objects.view_id = views_in_model.view_id AND views_objects.view_version = (SELECT MAX(version) FROM "+schema+"views_objects WHERE views_objects.view_id = views_in_model.view_id)"+
										" WHERE model_id = ? AND model_version = ?";
		} else {
			importViewsObjectsRequest = "SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height"+
										" FROM "+schema+"views_in_model"+
										" JOIN "+schema+"views_objects ON views_objects.view_id = views_in_model.view_id AND views_objects.view_version = views_in_model.view_version"+
										" WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countViewsObjects FROM ("+importViewsObjectsRequest+") vobjs"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		countViewObjectsToImport = result.getInt("countViewsObjects");
		countViewObjectsImported = 0;
		result.close();
		result=null;
		importViewsObjectsRequest += " ORDER BY views_objects.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			importViewsConnectionsRequest = "SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type "+
										" FROM "+schema+"views_in_model"+
										" JOIN "+schema+"views_connections ON views_connections.view_id = views_in_model.view_id AND views_connections.view_version = (SELECT MAX(version) FROM "+schema+"views_connections WHERE views_connections.view_id = views_in_model.view_id)"+
										" WHERE model_id = ? AND model_version = ?";
		} else {
			importViewsConnectionsRequest = "SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type"+
										" FROM "+schema+"views_in_model"+
										" JOIN "+schema+"views_connections ON views_connections.view_id = views_in_model.view_id AND views_connections.view_version = views_in_model.view_version"+
										" WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countViewsConnections FROM ("+importViewsConnectionsRequest+") vcons"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		countViewConnectionsToImport = result.getInt("countViewsConnections");
		countViewConnectionsImported = 0;
		result.close();
		result=null;
		importViewsConnectionsRequest += " ORDER BY views_connections.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		result = select("SELECT COUNT(DISTINCT image_path) AS countImages FROM "+schema+"views_in_model"+
						" INNER JOIN "+schema+"views ON "+schema+"views_in_model.view_id = views.id AND "+schema+"views_in_model.view_version = views.version"+
						" INNER JOIN "+schema+"views_objects ON views.id = "+schema+"views_objects.view_id AND views.version = "+schema+"views_objects.version"+
						" INNER JOIN "+schema+"images ON "+schema+"views_objects.image_path = images.path"+
						" WHERE model_id = ? AND model_version = ? AND path IS NOT NULL" 
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		countImagesToImport = result.getInt("countImages");
		countImagesImported = 0;
		result.close();
		result=null;

		if ( logger.isDebugEnabled() ) logger.debug("Importing "+countElementsToImport+" elements, "+countRelationshipsToImport+" relationships, "+countFoldersToImport+" folders, "+countViewsToImport+" views, "+countViewObjectsToImport+" views objects, "+countViewConnectionsToImport+" views connections, and "+countImagesToImport+" images.");

		// initializing the HashMaps that will be used to reference imported objects
		allImagePaths = new HashSet<String>();

		return countElementsToImport + countRelationshipsToImport + countFoldersToImport + countViewsToImport + countViewObjectsToImport + countViewConnectionsToImport + countImagesToImport;
	}

	/**
	 * Prepare the import of the folders from the database
	 */
	public void prepareImportFolders(ArchimateModel model) throws Exception {
		currentResultSet = select(importFoldersRequest
				,model.getId()
				,model.getCurrentVersion()
				);
	}

	/**
	 * Import the folders from the database
	 */
	public boolean importFolders(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IFolder folder = DBArchimateFactory.eINSTANCE.createFolder();

				folder.setId(currentResultSet.getString("folder_id"));
				((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(currentResultSet.getInt("folder_version"));
				((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setTimestamp(currentResultSet.getTimestamp("created_on"));

				folder.setName(currentResultSet.getString("name")==null ? "" : currentResultSet.getString("name"));
				folder.setDocumentation(currentResultSet.getString("documentation"));

				String parentId = currentResultSet.getString("parent_folder_id");

				if ( parentId != null && !parentId.isEmpty() ) {
					folder.setType(FolderType.get(0));                              		// non root folders have got the "USER" type
					
					IFolder parent = model.getAllFolders().get(parentId);
					if ( parent == null )
						parent=model.getFolder(FolderType.get(currentResultSet.getInt("root_type")));
					if ( parent == null ) 
						throw new Exception("Don't know where to create folder "+currentResultSet.getString("name")+" of type "+currentResultSet.getInt("type")+" and root_type "+currentResultSet.getInt("root_type")+" (unknown folder ID "+currentResultSet.getString("parent_folder_id")+")");

					parent.getFolders().add(folder);
				} else {
					folder.setType(FolderType.get(currentResultSet.getInt("type")));        // root folders have got their own type
					model.getFolders().add(folder);
				}

				importProperties(folder);
				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()+" of folder "+currentResultSet.getString("name")+"("+currentResultSet.getString("folder_id")+")");

				// we reference this folder for future use (storing sub-folders or components into it ...)
				model.countObject(folder, false, null);
				++countFoldersImported;
				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the elements from the database
	 */
	// it is complex because we need to retrieve the elements that have been added in views by other models
	public void prepareImportElements(ArchimateModel model) throws Exception {
		if ( model.getImportLatestVersion() ) {
			currentResultSet = select(importElementsRequest
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					);
		} else {
			currentResultSet = select(importElementsRequest
					,model.getId()
					,model.getCurrentVersion()
					);
		}
			
	}

	/**
	 * import the elements from the database
	 */
	public boolean importElements(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IArchimateElement element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));
				element.setId(currentResultSet.getString("element_id"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(currentResultSet.getInt("version"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(currentResultSet.getTimestamp("created_on"));

				element.setName(currentResultSet.getString("name")==null ? "" : currentResultSet.getString("name"));
				element.setDocumentation(currentResultSet.getString("documentation"));
				if ( element instanceof IJunction   && currentResultSet.getObject("type")!=null )  ((IJunction)element).setType(currentResultSet.getString("type"));

				IFolder folder;
				if ( currentResultSet.getString("parent_folder_id") == null ) {
					folder = model.getDefaultFolderForObject(element);
				} else {
					folder = model.getAllFolders().get(currentResultSet.getString("parent_folder_id"));
				}
				folder.getElements().add(element);

				importProperties(element);

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()+" of "+currentResultSet.getString("class")+":"+currentResultSet.getString("name")+"("+currentResultSet.getString("element_id")+")");

				// we reference the element for future use (establishing relationships, creating views objects, ...)
				model.countObject(element, false, null);
				++countElementsImported;
				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the relationships from the database
	 */
	public void prepareImportRelationships(ArchimateModel model) throws Exception {
		if ( model.getImportLatestVersion() ) {
			currentResultSet = select(importRelationshipsRequest
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					,model.getId()
					,model.getCurrentVersion()
					);
		} else {
			currentResultSet = select(importRelationshipsRequest
					,model.getId()
					,model.getCurrentVersion()
					);
		}
	}

	/**
	 * import the relationships from the database
	 */
	public boolean importRelationships(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IArchimateRelationship relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));
				relationship.setId(currentResultSet.getString("relationship_id"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(currentResultSet.getInt("version"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setTimestamp(currentResultSet.getTimestamp("created_on"));

				relationship.setName(currentResultSet.getString("name")==null ? "" : currentResultSet.getString("name"));
				relationship.setDocumentation(currentResultSet.getString("documentation"));

				if ( relationship instanceof IInfluenceRelationship && currentResultSet.getObject("strength")!=null )      ((IInfluenceRelationship)relationship).setStrength(currentResultSet.getString("strength"));
				if ( relationship instanceof IAccessRelationship    && currentResultSet.getObject("access_type")!=null )   ((IAccessRelationship)relationship).setAccessType(currentResultSet.getInt("access_type"));

				IFolder folder;
				if ( currentResultSet.getString("parent_folder_id") == null ) {
					folder = model.getDefaultFolderForObject(relationship);
				} else {
					folder = model.getAllFolders().get(currentResultSet.getString("parent_folder_id"));
				}
				folder.getElements().add(relationship);

				importProperties(relationship);

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()+" of "+currentResultSet.getString("class")+":"+currentResultSet.getString("name")+"("+currentResultSet.getString("relationship_id")+")");

				// we reference the relationship for future use (establishing relationships, creating views connections, ...)
				model.countObject(relationship, false, null);
				model.registerSourceAndTarget(relationship, currentResultSet.getString("source_id"), currentResultSet.getString("target_id"));
				++countRelationshipsImported;
				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views from the database
	 */
	public void prepareImportViews(ArchimateModel model) throws Exception {
		currentResultSet = select(importViewsRequest,
				model.getId(),
				model.getCurrentVersion()
				);
	}

	/**
	 * import the views from the database
	 */
	public boolean importViews(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IDiagramModel view;
				if ( DBPlugin.areEqual(currentResultSet.getString("class"), "CanvasModel") )
					view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(currentResultSet.getString("class"));
				else
					view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));

				view.setId(currentResultSet.getString("id"));
				((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(currentResultSet.getInt("version"));
				((IDBMetadata)view).getDBMetadata().getCurrentVersion().setTimestamp(currentResultSet.getTimestamp("created_on"));

				view.setName(currentResultSet.getString("name")==null ? "" : currentResultSet.getString("name"));
				view.setDocumentation(currentResultSet.getString("documentation"));
				view.setConnectionRouterType(currentResultSet.getInt("connection_router_type"));
				if ( view instanceof IArchimateDiagramModel && currentResultSet.getObject("viewpoint")!=null )     ((IArchimateDiagramModel) view).setViewpoint(currentResultSet.getString("viewpoint"));
				if ( view instanceof ISketchModel           && currentResultSet.getObject("background")!=null )    ((ISketchModel)view).setBackground(currentResultSet.getInt("background"));
				if ( view instanceof IHintProvider          && currentResultSet.getObject("hint_content")!=null )  ((IHintProvider)view).setHintContent(currentResultSet.getString("hint_content"));
				if ( view instanceof IHintProvider          && currentResultSet.getObject("hint_title")!=null )    ((IHintProvider)view).setHintTitle(currentResultSet.getString("hint_title"));

				model.getAllFolders().get(currentResultSet.getString("parent_folder_id")).getElements().add(view);

				importProperties(view);

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()+" of "+currentResultSet.getString("name")+"("+currentResultSet.getString("id")+")");

				// we reference the view for future use
				model.countObject(view, false, null);
				++countViewsImported;
				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views objects of a specific view from the database
	 */
	public void prepareImportViewsObjects(String viewId, int version) throws Exception {
		currentResultSet = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height FROM "+schema+"views_objects WHERE view_id = ? AND view_version = ? ORDER BY rank"
				,viewId
				,version
				);
	}

	/**
	 * import the views objects from the database
	 */
	public boolean importViewsObjects(ArchimateModel model, IDiagramModel view) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				EObject eObject;

				if ( currentResultSet.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(currentResultSet.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));

				((IIdentifier)eObject).setId(currentResultSet.getString("id"));
				((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setVersion(currentResultSet.getInt("version"));

				if ( eObject instanceof IDiagramModelArchimateComponent && currentResultSet.getString("element_id") != null) {
					// we check that the element already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateElement element = model.getAllElements().get(currentResultSet.getString("element_id"));
					if ( element == null ) {
						if (logger.isTraceEnabled() ) logger.trace("importing individual element ...");
						importElementFromId(model, null, currentResultSet.getString("element_id"), false);
					}
				}

				if ( eObject instanceof IDiagramModelArchimateComponent && currentResultSet.getObject("element_id")!=null )                            ((IDiagramModelArchimateComponent)eObject).setArchimateConcept(model.getAllElements().get(currentResultSet.getString("element_id")));
				if ( eObject instanceof IDiagramModelReference          && currentResultSet.getObject("diagram_ref_id")!=null )                        ((IDiagramModelReference)eObject).setReferencedModel(model.getAllViews().get(currentResultSet.getString("diagram_ref_id")));
				if ( eObject instanceof IDiagramModelArchimateObject    && currentResultSet.getObject("type")!=null )                                  ((IDiagramModelArchimateObject)eObject).setType(currentResultSet.getInt("type"));
				if ( eObject instanceof IBorderObject                   && currentResultSet.getObject("border_color")!=null )                          ((IBorderObject)eObject).setBorderColor(currentResultSet.getString("border_color"));
				if ( eObject instanceof IDiagramModelNote               && currentResultSet.getObject("border_type")!=null )                           ((IDiagramModelNote)eObject).setBorderType(currentResultSet.getInt("border_type"));
				if ( eObject instanceof ITextContent                    && currentResultSet.getObject("content")!=null )                               ((ITextContent)eObject).setContent(currentResultSet.getString("content"));
				if ( eObject instanceof IDocumentable                   && currentResultSet.getObject("documentation")!=null )                         ((IDocumentable)eObject).setDocumentation(currentResultSet.getString("documentation"));
				if ( eObject instanceof INameable                       && currentResultSet.getObject("name")!=null && currentResultSet.getObject("element_id")==null )   ((INameable)eObject).setName(currentResultSet.getString("name"));
				if ( eObject instanceof IHintProvider                   && currentResultSet.getObject("hint_content")!=null )                          ((IHintProvider)eObject).setHintContent(currentResultSet.getString("hint_content"));
				if ( eObject instanceof IHintProvider                   && currentResultSet.getObject("hint_title")!=null )                            ((IHintProvider)eObject).setHintTitle(currentResultSet.getString("hint_title"));
				if ( eObject instanceof ILockable                       && currentResultSet.getObject("is_locked")!=null )                             {int locked; if ( currentResultSet.getObject("is_locked") instanceof String ) locked = Integer.valueOf(currentResultSet.getString("is_locked")); else locked=currentResultSet.getInt("is_locked"); ((ILockable)eObject).setLocked(locked==0?false:true);}
				if ( eObject instanceof IDiagramModelImageProvider      && currentResultSet.getObject("image_path")!=null )                            ((IDiagramModelImageProvider)eObject).setImagePath(currentResultSet.getString("image_path"));
				if ( eObject instanceof IIconic                         && currentResultSet.getObject("image_position")!=null )                        ((IIconic)eObject).setImagePosition(currentResultSet.getInt("image_position"));
				if ( eObject instanceof ILineObject                     && currentResultSet.getObject("line_color")!=null )                            ((ILineObject)eObject).setLineColor(currentResultSet.getString("line_color"));
				if ( eObject instanceof ILineObject                     && currentResultSet.getObject("line_width")!=null )                            ((ILineObject)eObject).setLineWidth(currentResultSet.getInt("line_width"));
				if ( eObject instanceof IDiagramModelObject             && currentResultSet.getObject("fill_color")!=null )                            ((IDiagramModelObject)eObject).setFillColor(currentResultSet.getString("fill_color"));
				if ( eObject instanceof IFontAttribute                  && currentResultSet.getObject("font")!=null )                                  ((IFontAttribute)eObject).setFont(currentResultSet.getString("font"));
				if ( eObject instanceof IFontAttribute                  && currentResultSet.getObject("font_color")!=null )                            ((IFontAttribute)eObject).setFontColor(currentResultSet.getString("font_color"));
				if ( eObject instanceof ICanvasModelSticky              && currentResultSet.getObject("notes")!=null )                                 ((ICanvasModelSticky)eObject).setNotes(currentResultSet.getString("notes"));
				if ( eObject instanceof ITextAlignment                  && currentResultSet.getObject("text_alignment")!=null )                        ((ITextAlignment)eObject).setTextAlignment(currentResultSet.getInt("text_alignment"));
				if ( eObject instanceof ITextPosition                   && currentResultSet.getObject("text_position")!=null )                         ((ITextPosition)eObject).setTextPosition(currentResultSet.getInt("text_position"));
				if ( eObject instanceof IDiagramModelObject )                                                                       ((IDiagramModelObject)eObject).setBounds(currentResultSet.getInt("x"), currentResultSet.getInt("y"), currentResultSet.getInt("width"), currentResultSet.getInt("height"));

				// The container is either the view, or a container in the view
				if ( DBPlugin.areEqual(currentResultSet.getString("container_id"), view.getId()) )
					view.getChildren().add((IDiagramModelObject)eObject);
				else
					((IDiagramModelContainer)model.getAllViewObjects().get(currentResultSet.getString("container_id"))).getChildren().add((IDiagramModelObject)eObject);;


				if ( eObject instanceof IConnectable ) {
					model.registerSourceConnection((IDiagramModelObject)eObject, currentResultSet.getString("source_connections"));
					model.registerTargetConnection((IDiagramModelObject)eObject, currentResultSet.getString("target_connections"));
				}

				// If the object has got properties but does not have a linked element, then it may have distinct properties
				if ( eObject instanceof IProperties && currentResultSet.getString("element_id")==null ) {
					importProperties((IProperties)eObject);
				}

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()+" of "+currentResultSet.getString("class")+"("+((IIdentifier)eObject).getId()+")");

				// we reference the view for future use
				model.countObject(eObject, false, null);
				++countViewObjectsImported;

				// if the object contains an image, we store its path to import it later
				if ( currentResultSet.getString("image_path") != null )
					allImagePaths.add(currentResultSet.getString("image_path"));

				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the views connections of a specific view from the database
	 */
	public void prepareImportViewsConnections(String viewId, int version) throws Exception {
		currentResultSet = select("SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type FROM "+schema+"views_connections WHERE view_id = ? AND view_version = ? ORDER BY rank"
				,viewId
				,version
				);
	}

	/**
	 * import the views connections from the database
	 */
	public boolean importViewsConnections(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				EObject eObject;

				if ( currentResultSet.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(currentResultSet.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));

				((IIdentifier)eObject).setId(currentResultSet.getString("id"));
				((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setVersion(currentResultSet.getInt("version"));

				if ( eObject instanceof IDiagramModelArchimateConnection && currentResultSet.getString("relationship_id") != null) {
					// we check that the relationship already exists. If not, we import it (this may be the case during an individual view import.
					IArchimateRelationship relationship = model.getAllRelationships().get(currentResultSet.getString("relationship_id"));
					if ( relationship == null ) {
						importRelationshipFromId(model, null, currentResultSet.getString("relationship_id"), false);
					}
				}

				if ( eObject instanceof INameable                           && currentResultSet.getObject("name")!=null )              ((INameable)eObject).setName(currentResultSet.getString("name"));
				if ( eObject instanceof ILockable                           && currentResultSet.getObject("is_locked")!=null )         {int locked; if ( currentResultSet.getObject("is_locked") instanceof String ) locked = Integer.valueOf(currentResultSet.getString("is_locked")); else locked=currentResultSet.getInt("is_locked"); ((ILockable)eObject).setLocked(locked==0?false:true);}
				if ( eObject instanceof IDocumentable                       && currentResultSet.getObject("documentation")!=null )     ((IDocumentable)eObject).setDocumentation(currentResultSet.getString("documentation"));
				if ( eObject instanceof ILineObject                         && currentResultSet.getObject("line_color")!=null )        ((ILineObject)eObject).setLineColor(currentResultSet.getString("line_color"));
				if ( eObject instanceof ILineObject                         && currentResultSet.getObject("line_width")!=null )        ((ILineObject)eObject).setLineWidth(currentResultSet.getInt("line_width"));
				if ( eObject instanceof IFontAttribute                      && currentResultSet.getObject("font")!=null )              ((IFontAttribute)eObject).setFont(currentResultSet.getString("font"));
				if ( eObject instanceof IFontAttribute                      && currentResultSet.getObject("font_color")!=null )        ((IFontAttribute)eObject).setFontColor(currentResultSet.getString("font_color"));
				if ( eObject instanceof IDiagramModelConnection             && currentResultSet.getObject("type")!=null )              ((IDiagramModelConnection)eObject).setType(currentResultSet.getInt("type"));
				if ( eObject instanceof IDiagramModelConnection             && currentResultSet.getObject("text_position")!=null )     ((IDiagramModelConnection)eObject).setTextPosition(currentResultSet.getInt("text_position"));
				if ( eObject instanceof IDiagramModelArchimateConnection    && currentResultSet.getObject("type")!=null )              ((IDiagramModelArchimateConnection)eObject).setType(currentResultSet.getInt("type"));
				if ( eObject instanceof IDiagramModelArchimateConnection    && currentResultSet.getObject("relationship_id")!=null )   ((IDiagramModelArchimateConnection)eObject).setArchimateConcept(model.getAllRelationships().get(currentResultSet.getString("relationship_id")));

				if ( eObject instanceof IConnectable ) {
					model.registerSourceConnection((IDiagramModelConnection)eObject, currentResultSet.getString("source_connections"));
					model.registerTargetConnection((IDiagramModelConnection)eObject, currentResultSet.getString("target_connections"));
				}
				//model.registerSourceAndTarget((IDiagramModelConnection)eObject, currentResultSet.getString("source_object_id"), currentResultSet.getString("target_object_id"));

				if ( eObject instanceof IDiagramModelConnection ) {
					ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY rank"
							,((IIdentifier)eObject).getId()
							,((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()
							);
					while(resultBendpoints.next()) {
						IDiagramModelBendpoint bendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
						bendpoint.setStartX(resultBendpoints.getInt("start_x"));
						bendpoint.setStartY(resultBendpoints.getInt("start_y"));
						bendpoint.setEndX(resultBendpoints.getInt("end_x"));
						bendpoint.setEndY(resultBendpoints.getInt("end_y"));
						((IDiagramModelConnection)eObject).getBendpoints().add(bendpoint);
					}
					resultBendpoints.close();
				}

				// we reference the view for future use
				model.countObject(eObject, false, null);
				++countViewConnectionsImported;

				// If the connection has got properties but does not have a linked relationship, then it may have distinct properties
				if ( eObject instanceof IProperties && currentResultSet.getString("relationship_id")==null ) {
					importProperties((IProperties)eObject);
				}

				if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()+" of "+currentResultSet.getString("class")+"("+((IIdentifier)eObject).getId()+")");

				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
		}
		return false;
	}

	/**
	 * Prepare the import of the images from the database
	 */
	public HashSet<String> getAllImagePaths() {
		return allImagePaths;
	}

	/**
	 * import the views from the database
	 */
	public void importImage(ArchimateModel model, String path) throws Exception {
		currentResultSet = select("SELECT image FROM "+schema+"images WHERE path = ?", path);

		if (currentResultSet.next() ) {
			IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);
			try {
				String imagePath;
				byte[] imageContent = currentResultSet.getBytes("image");

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
				currentResultSet.close();
				currentResultSet = null;
				throw new Exception("Import of image failed !", e.getCause()!=null ? e.getCause() : e);
			}
			if ( logger.isDebugEnabled() ) logger.debug("   imported "+path);
			++countImagesImported;
			currentResultSet.close();
			currentResultSet = null;
		} else {
			currentResultSet.close();
			currentResultSet = null;
			throw new Exception("Import of image failed : unkwnown image path "+path);
		}
	}

	/**
	 * check if the number of imported images is equals to what is expected
	 */
	public void checkImportedImagesCount() throws Exception {
		if ( countImagesImported != countImagesToImport )
			throw new Exception(countImagesImported+" images imported instead of the "+countImagesToImport+" that were expected.");

		if ( logger.isDebugEnabled() ) logger.debug(countImagesImported+" images imported.");
	}

	/**
	 * Imports the properties of an Archi component<br>
	 * - missing properties are created
	 * - existing properties are updated with correct values if needed
	 * - existing properties with correct values are left untouched 
	 */
	private void importProperties(IProperties parent) throws Exception {
		int currentVersion;

		if ( parent instanceof IArchimateModel )
			currentVersion = ((ArchimateModel)parent).getCurrentVersion().getVersion();
		else
			currentVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

		ResultSet result = select("SELECT name, value FROM "+schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY rank"
				,((IIdentifier)parent).getId()
				,currentVersion
				);

		int i = 0;
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

			if ( shouldAdd ) {
				parent.getProperties().add(prop);
			}

			++i;
		}
		result.close();
		result=null;

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
		}

		return "?";
	}

	public List<Object> importElementFromId(ArchimateModel model, IArchimateDiagramModel view, String elementId, boolean mustCreateCopy) throws Exception {
		IArchimateElement element;
		List<Object> imported = new ArrayList<Object>();
		boolean newElement = false;

		// TODO add an option to import elements recursively

		//TODO : add try catch block !!!

		// We import the element
		// PostGreSQL obliges to complexify the request
		ResultSet result = select("SELECT version, class, name, documentation, type, created_on FROM "+schema+"elements e WHERE version = (SELECT MAX(version) FROM "+schema+"elements WHERE id = e.id) AND id = ?", elementId);
		if ( !result.next() ) {
			result.close();
			result=null;
			throw new Exception("Element with id="+elementId+" has not been found in the database.");
		}

		if ( mustCreateCopy ) {
			if ( logger.isDebugEnabled() ) logger.debug("Importing a copy of element id "+elementId+".");
			element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
			element.setId(model.getIDAdapter().getNewID());
			newElement = true;

			element.setName((currentResultSet.getString("name")==null ? "" : currentResultSet.getString("name"))+" (copy)");
			((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(0);
			((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));

		} else {
			element = model.getAllElements().get(elementId);
			if ( element == null ) {
				if ( logger.isDebugEnabled() ) logger.debug("Importing element id "+elementId+".");
				element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
				element.setId(elementId);
				newElement = true;
			} else {
				if ( logger.isDebugEnabled() ) logger.debug("Updating element id "+elementId+".");
				newElement = false;
			}

			if ( !DBPlugin.areEqual(element.getName(), result.getString("name")) ) element.setName(result.getString("name")==null ? "" : result.getString("name"));
			((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
			((IDBMetadata)element).getDBMetadata().getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
		}

		if ( !DBPlugin.areEqual(element.getDocumentation(), result.getString("documentation")) ) element.setDocumentation(result.getString("documentation"));
		if ( element instanceof IJunction ) {
			if ( !DBPlugin.areEqual(((IJunction)element).getType(), result.getString("type")) ) ((IJunction)element).setType(result.getString("type"));
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

		result.close();
		result=null;
		++countElementsImported;

		// We import the relationships that source or target the element
		result = select("SELECT id, source_id, target_id FROM "+schema+"relationships WHERE source_id = ? OR target_id = ?", elementId, elementId);
		while ( result.next() && result.getString("id") != null ) {
			// we import only relationships that do not exist
			if ( model.getAllRelationships().get(result.getString("id")) == null ) {
				IArchimateElement sourceElement = model.getAllElements().get(result.getString("source_id"));
				IArchimateRelationship sourceRelationship = model.getAllRelationships().get(result.getString("source_id"));
				IArchimateElement targetElement = model.getAllElements().get(result.getString("target_id"));
				IArchimateRelationship targetRelationship = model.getAllRelationships().get(result.getString("target_id"));

				// we import only relations when both source and target are in the model
				if ( (sourceElement!=null || sourceRelationship!=null) && (targetElement!=null || targetRelationship!=null) ) {
					imported.add(importRelationshipFromId(model, view, result.getString("id"), false));
				}
			}
		}
		result.close();
		result=null;

		if ( !imported.isEmpty() )
			model.resolveRelationshipsSourcesAndTargets();

		imported.add(0, element);
		//ITreeModelView treeView = (ITreeModelView)ViewManager.showViewPart(ITreeModelView.ID, true);
		//if(treeView != null) {
		//	logger.trace("selecting newly imported components");
		//	treeView.getViewer().setSelection(new StructuredSelection(element), true);
		//}

		return imported;
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

	public IArchimateRelationship importRelationshipFromId(ArchimateModel model, IArchimateDiagramModel view, String relationshipId, boolean mustCreateCopy) throws Exception {
		boolean newRelationship = false;

		ResultSet result = select("SELECT version, class, name, documentation, source_id, target_id, strength, access_type, created_on FROM "+schema+"relationships r WHERE version = (SELECT MAX(version) FROM "+schema+"relationships WHERE id = r.id) AND id = ?", relationshipId);
		if ( !result.next() ) {
			result.close();
			result=null;
			throw new Exception("relationship with id="+relationshipId+" has not been found in the database.");
		}
		// TODO check that the element does not exist yet ...

		IArchimateRelationship relationship;

		if ( mustCreateCopy ) {
			if ( logger.isDebugEnabled() ) logger.debug("Importing a copy of relationship id "+relationshipId+".");
			relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
			relationship.setId(model.getIDAdapter().getNewID());
			newRelationship = true;

			((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(0);
			((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
		} else {
			relationship = model.getAllRelationships().get(relationshipId);
			if ( relationship == null ) {
				if ( logger.isDebugEnabled() ) logger.debug("Importing relationship id "+relationshipId+".");
				relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
				relationship.setId(relationshipId);
				newRelationship = true;
			} else {
				if ( logger.isDebugEnabled() ) logger.debug("Upgrading relationship id "+relationshipId+".");
				newRelationship = false;
			}

			((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));
			((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
		}

		if ( !DBPlugin.areEqual(relationship.getName(), result.getString("name")) ) relationship.setName(result.getString("name")==null ? "" : result.getString("name"));
		if ( !DBPlugin.areEqual(relationship.getDocumentation(), result.getString("documentation")) )relationship.setDocumentation(result.getString("documentation"));

		if ( relationship instanceof IInfluenceRelationship && result.getObject("strength")!=null    && !DBPlugin.areEqual(((IInfluenceRelationship)relationship).getStrength(), result.getString("strength")) )  ((IInfluenceRelationship)relationship).setStrength(result.getString("strength"));
		if ( relationship instanceof IAccessRelationship    && result.getObject("access_type")!=null && ((IAccessRelationship)relationship).getAccessType() != result.getInt("access_type") )  ((IAccessRelationship)relationship).setAccessType(result.getInt("access_type"));

		IArchimateConcept source = model.getAllElements().get(result.getString("source_id"));
		if ( source == null ) source = model.getAllRelationships().get(result.getString("source_id"));
		relationship.setSource(source);

		IArchimateConcept target = model.getAllElements().get(result.getString("target_id"));
		if ( source == null ) source = model.getAllRelationships().get(result.getString("target_id"));
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
					IDiagramModelArchimateConnection connection = ArchimateDiagramModelFactory.createDiagramModelArchimateConnection(relationship);
					connection.setSource(sourceConnection);
					sourceConnection.getSourceConnections().add(connection);
					connection.setTarget(targetConnection);
					targetConnection.getTargetConnections().add(connection);
				}
			}
		}

		result.close();
		result=null;
		++countRelationshipsImported;

		return relationship;
	}



	/**
	 * This method imports a view with all its components (graphical objects and connections) and requirements (elements and relationships)<br>
	 * elements and relationships that needed to be imported are located in a folder named by the view
	 */
	public IDiagramModel importViewFromId(ArchimateModel model, IFolder parentFolder, String id, boolean mustCreateCopy) throws Exception {
		if ( model.getAllViews().get(id) != null ) {
			if ( mustCreateCopy )
				throw new RuntimeException("Re-importing a view is not supported.\n\nIf you wish to create a copy of an existing table, you may use a copy-paste operation.");
			else
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
		ResultSet result = select("SELECT version, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint FROM "+schema+"views v WHERE version = (SELECT MAX(version) FROM "+schema+"views WHERE id = v.id) AND id = ?", id);
		result.next();
		IDiagramModel view;
		if ( DBPlugin.areEqual(result.getString("class"), "CanvasModel") )
			view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(result.getString("class"));
		else
			view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(result.getString("class"));

		if ( mustCreateCopy ) 
			view.setId(model.getIDAdapter().getNewID());
		else
			view.setId(id);

		((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(result.getInt("version"));

		view.setName(result.getString("name")==null ? "" : result.getString("name"));
		view.setDocumentation(result.getString("documentation"));
		view.setConnectionRouterType(result.getInt("connection_router_type"));
		if ( view instanceof IArchimateDiagramModel && result.getObject("viewpoint")!=null )     ((IArchimateDiagramModel) view).setViewpoint(result.getString("viewpoint"));
		if ( view instanceof ISketchModel           && result.getObject("background")!=null )    ((ISketchModel)view).setBackground(result.getInt("background"));
		if ( view instanceof IHintProvider          && result.getObject("hint_content")!=null )  ((IHintProvider)view).setHintContent(result.getString("hint_content"));
		if ( view instanceof IHintProvider          && result.getObject("hint_title")!=null )    ((IHintProvider)view).setHintTitle(result.getString("hint_title"));
	
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
			;
		}

		// 3 : we import the connections and create the corresponding relationships if they do not exist yet
		prepareImportViewsConnections(((IIdentifier)view).getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion());
		while ( importViewsConnections(model) ) {
			;
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
	public int getLatestModelVersion(ArchimateModel model) throws Exception {
		// we use COALESCE to guarantee that a value is returned, even if the model does not exist in the database
		ResultSet result = select("SELECT COALESCE(MAX(version),0) as version FROM "+schema+"models WHERE id = ?", model.getId());
		result.next();

		int version = result.getInt("version");
		result.close();
		result=null;

		return version;
	}

	///////////////////////////////////////////////////////////////////////////////////
	//                                                                               //
	//   E X P O R T   S P E C I F I C   V A R I A B L E S   A N D   M E T H O D S   //
	//                                                                               //
	///////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the versions and checksum of one model's components from the database and fills their DBMetadata. Components that are not in the current model are returned in a HashMap.
     * @param modelId the id of the model
     * @param modelVersion the versions of the model (0 to get the latest version)
     * @param getWholeModel if true, fills in the folders and view variables as well
     * @return HashMap with versions and checksum of components that are not in the model.
     * @throws SQLException
     */
    public HashMap<String, DBVersion> getVersionsFromDatabase(ArchimateModel model) throws SQLException, RuntimeException {
        if ( logger.isDebugEnabled() ) logger.debug("Getting latest version of the model from the database");
        
    	HashMap<String, DBVersion> componentsNotInModel = new HashMap<String, DBVersion>();
    	
        // This method can retrieve versions if the database contains the whole model tables
        if ( !databaseEntry.getExportWholeModel() ) {
        	return componentsNotInModel;
        }
        
        ResultSet result;
        int modelVersion = -1;
        
        // we get the model version from the database
        result = select("SELECT MAX(version) version from "+schema+"models WHERE id = ?", model.getId());
        if ( result.next() && result.getObject("version") != null ) {
        	modelVersion = result.getInt("version");
            model.getDatabaseVersion().setVersion(modelVersion);
        }
        result.close();
        
        if ( modelVersion == -1 )
            return componentsNotInModel;         // if the model is not not found in the database, this is not an error. this just means that the model is new. 

        result = select("SELECT checksum, created_on from "+schema+"models WHERE id = ? AND version = ?", model.getId(), modelVersion);
        result.next();
        if ( result.getString("checksum") != null ) {
        	model.getDatabaseVersion().setChecksum(result.getString("checksum"));
        	model.getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
        }
        result.close();
        
        // we get the list of elements
        result = select("SELECT id, version, checksum, created_on from "+schema+"elements elements "+
                "JOIN "+schema+"elements_in_model elements_in_model on elements.id = elements_in_model.element_id and elements.version = elements_in_model.element_version "+
                "WHERE elements_in_model.model_id = ? and elements_in_model.model_version = ?",
                model.getId(),
                modelVersion);
        while ( result.next() ) {
            IDBMetadata element = (IDBMetadata)model.getAllElements().get(result.getString("id"));
            if ( element != null ) {
            	if ( logger.isTraceEnabled() )
            		logger.trace("   element "+result.getString("id")+" : created on "+result.getTimestamp("created_on"));
            	element.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
            	element.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
            	element.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
            } else
            	componentsNotInModel.put(result.getString("id"), new DBVersion("elements", result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on")));
        }
        result.close();
        
        // we get the list of relationships
        result = select("SELECT id, version, checksum, created_on from "+schema+"relationships relationships "+
                "JOIN "+schema+"relationships_in_model relationships_in_model on relationships.id = relationships_in_model.relationship_id and relationships.version = relationships_in_model.relationship_version "+
                "WHERE relationships_in_model.model_id = ? and relationships_in_model.model_version = ?",
                model.getId(),
                modelVersion);
        while ( result.next() ) {
            IDBMetadata relationship = (IDBMetadata)model.getAllRelationships().get(result.getString("id"));
            if ( relationship != null ) {
            	if ( logger.isTraceEnabled() )
            		logger.trace("   relationship "+result.getString("id")+" : created on "+result.getTimestamp("created_on"));
            	relationship.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
            	relationship.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
            	relationship.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
            } else
            	componentsNotInModel.put(result.getString("id"), new DBVersion("relationships", result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on")));
        }
        result.close();
        
        // we get the list of folders
        result = select("SELECT id, version, checksum, created_on from "+schema+"folders folders "+
                "JOIN "+schema+"folders_in_model folders_in_model on folders.id = folders_in_model.folder_id and folders.version = folders_in_model.folder_version "+
                "WHERE folders_in_model.model_id = ? and folders_in_model.model_version = ?",
                model.getId(),
                modelVersion);
        while ( result.next() ) {
            IDBMetadata folder = (IDBMetadata)model.getAllFolders().get(result.getString("id"));
            if ( folder != null ) {
            	if ( logger.isTraceEnabled() )
            		logger.trace("   folder "+result.getString("id")+" : created on "+result.getTimestamp("created_on"));
            	folder.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
            	folder.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
            	folder.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
            } else
            	componentsNotInModel.put(result.getString("id"), new DBVersion("folders", result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on")));
        }
        result.close();
        
        // we get the list of views
        result = select("SELECT id, version, checksum, created_on from "+schema+"views views "+
                "JOIN "+schema+"views_in_model views_in_model on views.id = views_in_model.view_id and views.version = views_in_model.view_version "+
                "WHERE views_in_model.model_id = ? and views_in_model.model_version = ?",
                model.getId(),
                modelVersion);
        while ( result.next() ) {
            IDBMetadata view = (IDBMetadata)model.getAllViews().get(result.getString("id"));
            if ( view != null ) {
            	if ( logger.isTraceEnabled() )
            		logger.trace("   view "+result.getString("id")+" : created on "+result.getTimestamp("created_on"));
            	view.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
            	view.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
            	view.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
            } else
            	componentsNotInModel.put(result.getString("id"), new DBVersion("views", result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on")));
        }
        result.close();
        
        //TODO: images
        return componentsNotInModel;
    }
    
	/* **** Export methods  ************************************************************** */

	/**
	 * Exports the model metadata into the database
	 */
	public void exportModel(ArchimateModel model, String releaseNote) throws Exception {
		final String[] modelsColumns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "checksum"};
		
		if ( (model.getName() == null) || (model.getName().equals("")) )
			throw new RuntimeException("Model name cannot be empty.");

		insert(schema+"models", modelsColumns
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,model.getName()
				,releaseNote
				,model.getPurpose()
				,System.getProperty("user.name")
				,getLastTransactionTimestamp()
				,model.getCurrentVersion().getChecksum()
				);

		exportProperties(model);
	}

	/**
	 * Export a component to the database
	 */
	public void exportEObject(EObject eObject) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("version "+((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getVersion()+" of "+((IDBMetadata)eObject).getDBMetadata().getDebugName()+" is exported");

		if ( eObject instanceof IArchimateElement ) 			exportElement((IArchimateElement)eObject);
		else if ( eObject instanceof IArchimateRelationship ) 	exportRelationship((IArchimateRelationship)eObject);
		else if ( eObject instanceof IFolder ) 					exportFolder((IFolder)eObject);
		else if ( eObject instanceof IDiagramModel ) 			exportView((IDiagramModel)eObject);
		else if ( eObject instanceof IDiagramModelObject )		exportViewObject((IDiagramModelComponent)eObject);
		else if ( eObject instanceof IDiagramModelConnection )	exportViewConnection((IDiagramModelConnection)eObject);
		else
			throw new Exception("Do not know how to export "+eObject.getClass().getSimpleName());
	}
	
	public void assignEObjectToModel(EObject eObject) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("assigning component to model");
		
		if ( eObject instanceof IArchimateElement )				assignElementToModel((IArchimateElement)eObject);
		else if ( eObject instanceof IArchimateRelationship )	assignRelationshipToModel((IArchimateRelationship)eObject);
		else if ( eObject instanceof IFolder )					assignFolderToModel((IFolder)eObject);
		else if ( eObject instanceof IDiagramModel )			assignViewToModel((IDiagramModel)eObject);
		else if ( eObject instanceof IDiagramModelObject )		;// IDiagramModelObject and IDiagramModelConnection are assigned to views not to models
		else if ( eObject instanceof IDiagramModelConnection )	;
		else
			throw new Exception("Do not know how to assign to the model : "+eObject.getClass().getSimpleName());
	}

	/**
	 * Export an element to the database
	 */
	private void exportElement(IArchimateConcept element) throws Exception {
		final String[] elementsColumns = {"id", "version", "class", "name", "type", "documentation", "created_by", "created_on", "checksum"};

		if ( DBPlugin.areEqual(databaseEntry.getDriver(), "neo4j") ) {
			// USE MERGE instead to replace existing nodes
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
			insert(schema+"elements", elementsColumns
					,element.getId()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
					,element.getDocumentation()
					,System.getProperty("user.name")
					,getLastTransactionTimestamp()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getChecksum()
					);
		}

		exportProperties(element);

		// the element has been exported to the database
		//TODO: --countElementsToExport;
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
		ArchimateModel model = (ArchimateModel)element.getArchimateModel();

		insert(schema+"elements_in_model", elementsInModelColumns
				,element.getId()
				,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportElement()
				,((IFolder)((IArchimateConcept)element).eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++elementRank
				);
	}

	/**
	 * Export a relationship to the database
	 */
	private void exportRelationship(IArchimateConcept relationship) throws Exception {
		final String[] relationshipsColumns = {"id", "version", "class", "name", "documentation", "source_id", "target_id", "strength", "access_type", "created_by", "created_on", "checksum"};

		if ( DBPlugin.areEqual(databaseEntry.getDriver(), "neo4j") ) {
			// USE MERGE instead to replace existing nodes
			if ( databaseEntry.getNeo4jNativeMode() ) {
				if ( (((IArchimateRelationship)relationship).getSource() instanceof IArchimateElement) && (((IArchimateRelationship)relationship).getTarget() instanceof IArchimateElement) ) {
					request("MATCH (source:elements {id:?, version:?}), (target:elements {id:?, version:?}) CREATE (source)-[relationship:relationships {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}]->(target)"
							,((IArchimateRelationship)relationship).getSource().getId()
							,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion()
							,((IArchimateRelationship)relationship).getTarget().getId()
							,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion()
							,relationship.getId()
							,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion()
							,relationship.getClass().getSimpleName()
							,relationship.getName()
							,relationship.getDocumentation()
							,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
							,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
							,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
							);
				}
			} else {
				request("MATCH (source {id:?, version:?}), (target {id:?, version:?}) CREATE (relationship:relationships {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}), (source)-[rel1:relatedTo]->(relationship)-[rel2:relatedTo]->(target)"
						,((IArchimateRelationship)relationship).getSource().getId()
						,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion()
						,((IArchimateRelationship)relationship).getTarget().getId()
						,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion()
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
			insert(schema+"relationships", relationshipsColumns
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
					,getLastTransactionTimestamp()
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

		ArchimateModel model = (ArchimateModel)relationship.getArchimateModel();

		insert(schema+"relationships_in_model", relationshipsInModelColumns
				,relationship.getId()
				,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportRelationship()
				,((IFolder)((IArchimateConcept)relationship).eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++relationshipRank
				);
	}

	/**
	 * Export a folder into the database.
	 */
	private void exportFolder(IFolder folder) throws Exception {
		final String[] foldersColumns = {"id", "version", "type", "root_type", "name", "documentation", "created_by", "created_on", "checksum"};

		insert(schema+"folders", foldersColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
				,folder.getType().getValue()
				,((IDBMetadata)folder).getDBMetadata().getRootFolderType()
				,folder.getName()
				,folder.getDocumentation()
				,System.getProperty("user.name")
				,getLastTransactionTimestamp()
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

		ArchimateModel model = (ArchimateModel)folder.getArchimateModel();

		insert(schema+"folders_in_model", foldersInModelColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportFolder()
				,(((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId())
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++folderRank
				);
	}

	/**
	 * Export a view into the database.
	 */
	private void exportView(IDiagramModel view) throws Exception {
		final String[] ViewsColumns = {"id", "version", "class", "created_by", "created_on", "name", "connection_router_type", "documentation", "hint_content", "hint_title", "viewpoint", "background", "screenshot", "checksum"};

		byte[] viewImage = null;

		if ( databaseEntry.getExportViewsImages() ) {
			viewImage = DBGui.createImage(view, 1, 10);
		}

		insert(schema+"views", ViewsColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
				,view.getClass().getSimpleName()
				,System.getProperty("user.name")
				,new Timestamp(Calendar.getInstance().getTime().getTime())
				,view.getName()
				,view.getConnectionRouterType()
				,view.getDocumentation()
				,((view instanceof IHintProvider) ? ((IHintProvider)view).getHintContent() : null)
				,((view instanceof IHintProvider) ? ((IHintProvider)view).getHintTitle() : null)
				,((view instanceof IArchimateDiagramModel) ? ((IArchimateDiagramModel)view).getViewpoint() : null)
				,((view instanceof ISketchModel) ? ((ISketchModel)view).getBackground() : null)
				,viewImage
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getChecksum()
				);

		if ( databaseEntry.getExportViewsImages() ) {
			DBGui.disposeImage();
		}

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

		ArchimateModel model = (ArchimateModel)view.getArchimateModel();

		insert(schema+"views_in_model", viewsInModelColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportView()
				,((IFolder)view.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++viewRank
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
		final String[] ViewsObjectsColumns = {"id", "version", "container_id", "view_id", "view_version", "class", "element_id", "diagram_ref_id", "type", "border_color", "border_type", "content", "documentation", "hint_content", "hint_title", "is_locked", "image_path", "image_position", "line_color", "line_width", "fill_color", "font", "font_color", "name", "notes", "source_connections", "target_connections", "text_alignment", "text_position", "x", "y", "width", "height", "rank", "checksum"};

		EObject viewContainer = viewObject.eContainer();
		while ( !(viewContainer instanceof IDiagramModel) ) {
			viewContainer = viewContainer.eContainer();
		}

		insert(schema+"views_objects", ViewsObjectsColumns
				,((IIdentifier)viewObject).getId()
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()
				,((IIdentifier)viewObject.eContainer()).getId()
				,((IIdentifier)viewContainer).getId()
				,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getVersion()
				,viewObject.getClass().getSimpleName()
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
				,((viewObject instanceof INameable ) ? ((INameable)viewObject).getName() : null)					// we export the name because it will be used in case of conflict
				,((viewObject instanceof ICanvasModelSticky) ? ((ICanvasModelSticky)viewObject).getNotes() : null)
				,((viewObject instanceof IConnectable) ? encode(((IConnectable)viewObject).getSourceConnections()) : null)
				,((viewObject instanceof IConnectable) ? encode(((IConnectable)viewObject).getTargetConnections()) : null)
				,((viewObject instanceof ITextAlignment) ? ((ITextAlignment)viewObject).getTextAlignment() : null)
				,((viewObject instanceof ITextPosition) ? ((ITextPosition)viewObject).getTextPosition() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getX() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getY() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getWidth() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getHeight() : null)
				,++viewObjectRank
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getChecksum()
				);

		if ( viewObject instanceof IProperties && !(viewObject instanceof IDiagramModelArchimateComponent))
			exportProperties((IProperties)viewObject);
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
		final String[] ViewsConnectionsColumns = {"id", "version", "container_id", "view_id", "view_version", "class", "name", "documentation", "is_locked", "line_color", "line_width", "font", "font_color", "relationship_id", "source_connections", "target_connections", "source_object_id", "target_object_id", "text_position", "type", "rank", "checksum"};
		final String[] bendpointsColumns = {"parent_id", "parent_version", "rank", "start_x", "start_y", "end_x", "end_y"};


		EObject viewContainer = viewConnection.eContainer();
		while ( !(viewContainer instanceof IDiagramModel) ) {
			viewContainer = viewContainer.eContainer();
		}

		insert(schema+"views_connections", ViewsConnectionsColumns
				,((IIdentifier)viewConnection).getId()
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
				,((IIdentifier)viewConnection.eContainer()).getId()
				,((IIdentifier)viewContainer).getId()
				,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getVersion()
				,viewConnection.getClass().getSimpleName()
				,((viewConnection instanceof INameable     && !(viewConnection instanceof IDiagramModelArchimateConnection)) ? ((INameable)viewConnection).getName() : null)                    // if there is a relationship behind, the name is the relationship name, so no need to store it.
				,((viewConnection instanceof IDocumentable && !(viewConnection instanceof IDiagramModelArchimateConnection)) ? ((IDocumentable)viewConnection).getDocumentation() : null)       // if there is a relationship behind, the documentation is the relationship name, so no need to store it.
				,((viewConnection instanceof ILockable) ? (((ILockable)viewConnection).isLocked()?1:0) : null)  
				,((viewConnection instanceof ILineObject) ? ((ILineObject)viewConnection).getLineColor() : null)
				,((viewConnection instanceof ILineObject) ? ((ILineObject)viewConnection).getLineWidth() : null)
				,((viewConnection instanceof IFontAttribute) ? ((IFontAttribute)viewConnection).getFont() : null)
				,((viewConnection instanceof IFontAttribute) ? ((IFontAttribute)viewConnection).getFontColor() : null)
				,((viewConnection instanceof IDiagramModelArchimateConnection) ? ((IDiagramModelArchimateConnection)viewConnection).getArchimateConcept().getId() : null)
				,((viewConnection instanceof IConnectable) ? encode(((IConnectable)viewConnection).getSourceConnections()) : null)
				,((viewConnection instanceof IConnectable) ? encode(((IConnectable)viewConnection).getTargetConnections()) : null)
				,((viewConnection instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)viewConnection).getSource().getId() : null)
				,((viewConnection instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)viewConnection).getTarget().getId() : null)
				,((viewConnection instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)viewConnection).getTextPosition() : null)
				,((viewConnection instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)viewConnection).getType() : (viewConnection instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)viewConnection).getType() : null)
				,++viewConnectionRank
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getChecksum()
				);

		if ( viewConnection instanceof IProperties )
			exportProperties((IProperties)viewConnection);

		if ( viewConnection instanceof IDiagramModelConnection ) {
			for ( int pos = 0 ; pos < ((IDiagramModelConnection)viewConnection).getBendpoints().size(); ++pos) {
				IDiagramModelBendpoint bendpoint = ((IDiagramModelConnection)viewConnection).getBendpoints().get(pos);
				insert(schema+"bendpoints", bendpointsColumns
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
	}

	/**
	 * Export properties to the database
	 */
	private void exportProperties(IProperties parent) throws Exception {
		final String[] propertiesColumns = {"parent_id", "parent_version", "rank", "name", "value"};

		int exportedVersion;
		if ( parent instanceof ArchimateModel ) {
			exportedVersion = ((ArchimateModel)parent).getCurrentVersion().getVersion();
		} else 
			exportedVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

		for ( int propRank = 0 ; propRank < parent.getProperties().size(); ++propRank) {
			IProperty prop = parent.getProperties().get(propRank);
			if ( DBPlugin.areEqual(databaseEntry.getDriver(), "neo4j") ) {
				request("MATCH (parent {id:?, version:?}) CREATE (prop:property {rank:?, name:?, value:?}), (parent)-[:hasProperty]->(prop)"
						,((IIdentifier)parent).getId()
						,exportedVersion
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
			}
			else
				insert(schema+"properties", propertiesColumns
						,((IIdentifier)parent).getId()
						,exportedVersion
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
		}
	}

	public void exportImage(String path, byte[] image) throws SQLException, NoSuchAlgorithmException {
		String checksum = DBChecksum.calculateChecksum(image);
		ResultSet result = null;

		try {
			result = select("SELECT checksum FROM "+schema+"images WHERE path = ?", path);

			if ( result.next() ) {
				// if the image exists in the database, we update it if the checkum differs
				if ( !DBPlugin.areEqual(checksum, result.getString("checksum")) ) {
					request("UPDATE "+schema+"images SET image = ?, checksum = ? WHERE path = ?"
							,image
							,checksum
							,path
							);
				}
			} else {
				// if the image is not yet in the db, we insert it
				String[] databaseColumns = {"path", "image", "checksum"};
				insert(schema+"images", databaseColumns
						,path
						,image
						,checksum								
						);
			}
		} finally {    
			if ( result != null ) result.close();
			result = null;
		}
	}

	private String encode (EList<IDiagramModelConnection> connections) {
		StringBuilder result = new StringBuilder();
		for ( IDiagramModelConnection connection: connections ) {
			if ( result.length() > 0 )
				result.append(",");
			result.append(connection.getId());
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
}