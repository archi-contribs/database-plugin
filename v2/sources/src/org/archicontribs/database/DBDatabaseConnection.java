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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.impl.Folder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

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

		this.schema = databaseEntry.getSchemaPrefix();

		openConnection();
	}


	private void openConnection() throws ClassNotFoundException, SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Opening connection to database "+this.databaseEntry.getName()+" : driver="+this.databaseEntry.getDriver()+", server="+this.databaseEntry.getServer()+", port="+this.databaseEntry.getPort()+", database="+this.databaseEntry.getDatabase()+", schema="+this.databaseEntry.getSchema()+", username="+this.databaseEntry.getUsername());

		String clazz = null;
		String connectionString = null;

		switch (this.databaseEntry.getDriver()) {
			case "postgresql" :
				clazz = "org.postgresql.Driver";
				connectionString = "jdbc:postgresql://" + this.databaseEntry.getServer() + ":" + this.databaseEntry.getPort() + "/" + this.databaseEntry.getDatabase();
				break;
			case "ms-sql"      :
				clazz = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
				connectionString = "jdbc:sqlserver://" + this.databaseEntry.getServer() + ":" + this.databaseEntry.getPort() + ";databaseName=" + this.databaseEntry.getDatabase();
				if ( DBPlugin.isEmpty(this.databaseEntry.getUsername()) && DBPlugin.isEmpty(this.databaseEntry.getPassword()) )
					connectionString += ";integratedSecurity=true";
				break;
			case "mysql"      :
				clazz = "com.mysql.jdbc.Driver";
				connectionString = "jdbc:mysql://" + this.databaseEntry.getServer() + ":" + this.databaseEntry.getPort() + "/" + this.databaseEntry.getDatabase();
				break;
			case "neo4j"      :
				clazz = "org.neo4j.jdbc.Driver";
				connectionString = "jdbc:neo4j:bolt://" + this.databaseEntry.getServer() + ":" + this.databaseEntry.getPort();
				break;
			case "oracle"     :
				clazz = "oracle.jdbc.driver.OracleDriver";
				connectionString = "jdbc:oracle:thin:@" + this.databaseEntry.getServer() + ":" + this.databaseEntry.getPort() + ":" + this.databaseEntry.getDatabase();
				break;
			case "sqlite"     :
				clazz = "org.sqlite.JDBC";
				connectionString = "jdbc:sqlite:"+this.databaseEntry.getServer();
				break;
			default :
				throw new SQLException("Unknonwn driver " + this.databaseEntry.getDriver());        // just in case
		}

		if ( logger.isDebugEnabled() ) logger.debug("JDBC class = " + clazz);
		Class.forName(clazz);

		if ( logger.isDebugEnabled() ) logger.debug("JDBC connection string = " + connectionString);
		try {
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "ms-sql") && DBPlugin.isEmpty(this.databaseEntry.getUsername()) && DBPlugin.isEmpty(this.databaseEntry.getPassword()) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Connecting with Windows integrated security");
				this.connection = DriverManager.getConnection(connectionString);
			} else {
				if ( logger.isDebugEnabled() ) logger.debug("Connecting with username = "+this.databaseEntry.getUsername());
				this.connection = DriverManager.getConnection(connectionString, this.databaseEntry.getUsername(), this.databaseEntry.getPassword());
			}
		} catch (SQLException e) {
			// if the JDBC driver fails to connect to the database using the specified driver, the it tries with all the other drivers
			// and the exception is raised by the latest driver (log4j in our case)
			// so we need to trap this exception and change the error message
			// For JDBC people, this is not a bug but a functionality :( 
			if ( DBPlugin.areEqual(e.getMessage(), "JDBC URL is not correct.\nA valid URL format is: 'jdbc:neo4j:http://<host>:<port>'") ) {
				if ( this.databaseEntry.getDriver().equals("ms-sql") && DBPlugin.isEmpty(this.databaseEntry.getUsername()) && DBPlugin.isEmpty(this.databaseEntry.getPassword()) )	// integrated authentication
					throw new SQLException("Please verify the database configuration in the preferences.\n\nPlease also check that you installed the \"sqljdbc_auth.dll\" file in the JRE bin folder to enable the SQL Server integrated security mode.");
				throw new SQLException("Please verify the database configuration in the preferences.");
			}
			throw e;
		}

		if ( logger.isDebugEnabled() ) {
			if ( DBPlugin.isEmpty(this.schema) ) {
				logger.debug("Will use default schema ");
			}else {
				logger.debug("Will use schema "+this.schema);
			}
		}
	}

	/**
	 * Closes connection to the database
	 */
	public void close() throws SQLException {
		reset();

		if ( this.connection == null || this.connection.isClosed() ) {
			if ( logger.isDebugEnabled() ) logger.debug("The database connection is already closed.");
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Closing database connection.");
			this.connection.close();
		}
		this.connection = null;
		this.databaseEntry = null;
	}

	public boolean isConnected() {
		return this.connection != null;
	}

	public void reset() throws SQLException {
		for ( PreparedStatement pstmt: this.preparedStatementMap.values() ) {
			pstmt.close();
			pstmt=null;
		}
		this.preparedStatementMap = new HashMap<String, PreparedStatement>();

		if ( this.currentResultSet != null ) {
			this.currentResultSet.close();
			this.currentResultSet = null;
		}

		// We reset all "ranks" to zero
		this.elementRank = 0;
		this.relationshipRank = 0;
		this.folderRank = 0;
		this.viewRank = 0;
		this.viewObjectRank = 0;
		this.viewConnectionRank = 0;

		// we free up a bit the memory
		this.allImagePaths = null;
	}




	/**
	 * Checks the database structure<br>
	 * @throws SQLExcetion if the connection to the database failed
	 * @returns true if the database structure is correct, false if not
	 */
	public void checkDatabase() throws ClassNotFoundException, SQLException {
		boolean wasConnected = isConnected();

		DBGui.popup("Please wait while checking the "+this.databaseEntry.getDriver()+" database ...");

		try {
			if ( !wasConnected )
				openConnection();
			
			switch ( this.databaseEntry.getDriver() ) {
				case "neo4j" :
					DBGui.closePopup();		// no tables to check on neo4j databases
					return;
				case "sqlite" :
					this.AUTO_INCREMENT	= "integer PRIMARY KEY";
					this.BOOLEAN			= "tinyint(1)";          				// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "timestamp";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "blob";
					this.INTEGER			= "integer(10)";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY		= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "clob";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "mysql"  :
					this.AUTO_INCREMENT	= "int(10) NOT NULL AUTO_INCREMENT";
					this.BOOLEAN			= "tinyint(1)";							// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "datetime";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "longblob";
					this.INTEGER			= "int(10)";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY		= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "mediumtext";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "ms-sql"  :
					this.AUTO_INCREMENT	= "int IDENTITY NOT NULL" ;
					this.BOOLEAN			= "tinyint";          					// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "datetime";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "image";
					this.INTEGER			= "int";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY		= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "nvarchar(max)";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "oracle" :
					this.AUTO_INCREMENT	= "integer NOT NULL";
					this.BOOLEAN			= "char";          						// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "date";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "blob";
					this.INTEGER			= "integer";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY		= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "clob";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "postgresql" :
					this.AUTO_INCREMENT	= "serial NOT NULL" ;
					this.BOOLEAN			= "smallint";          					// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "timestamp";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "bytea";
					this.INTEGER			= "integer";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY		= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "text";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				default:		// should never be here, but just in case
					throw new SQLException("Unknown driver "+this.databaseEntry.getDriver());
					
			}

			// checking if the database_version table exists
			if ( logger.isTraceEnabled() ) logger.trace("Checking \""+this.schema+"database_version\" table");

			ResultSet result;
			try {
				result = select("SELECT version FROM "+this.schema+"database_version WHERE archi_plugin = ?", DBPlugin.pluginName);
			} catch (@SuppressWarnings("unused") SQLException err) {
				DBGui.closePopup();
				// if the table does not exist
				if ( !DBGui.question("We successfully connected to the database but the necessary tables have not be found.\n\nDo you wish to create them ?") ) {
					throw new SQLException("Necessary tables not found.");
				}

				createTables();
				return;
			}

			if ( result.next() ) {
				if ( databaseVersion != result.getInt("version") ) {
					if ( (result.getInt("version")<200) || (result.getInt("version")>databaseVersion) ) {
						result.close();
						throw new SQLException("The database has got an unknown model version (is "+result.getInt("version")+" but should be between 200 and "+databaseVersion+")");
					}
					
					result.close();
					
					if ( DBGui.question("The database needs to be upgraded. You will not loose any data during this operation.\n\nDo you wish to upgrade your database ?") ) {
						try {
						    upgradeDatabase(result.getInt("version"));
						} catch (Exception err) {
						    close();
						    throw err;
						}
					} else {
					    close();
						throw new SQLException("The database needs to be upgraded.");
					}
				} else
				    result.close();
			} else {
				result.close();
				result=null;
				throw new SQLException(DBPlugin.pluginName+" not found in "+this.schema+"database_version table");
				//TODO : call create tables and update createTables method to ignore error on database_version table (in case it already exists and is empty)
			}

			DBGui.closePopup();

		} finally {
			if ( !wasConnected && this.connection!=null) {
				this.connection.close();
				this.connection = null;
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
			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"database_version");
			request("CREATE TABLE "+ this.schema +"database_version ("
					+ "archi_plugin "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL"
					+ ")");

			insert(this.schema+"database_version", databaseVersionColumns, DBPlugin.pluginName, databaseVersion);

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"bendpoints");
			request("CREATE TABLE "+ this.schema +"bendpoints ("
					+ "parent_id "+ this.OBJECTID +" NOT NULL, "
					+ "parent_version "+ this.INTEGER +" NOT NULL, "
					+ "rank "+ this.INTEGER +" NOT NULL, "
					+ "start_x "+ this.INTEGER +" NOT NULL, "
					+ "start_y "+ this.INTEGER +" NOT NULL, "
					+ "end_x "+ this.INTEGER +" NOT NULL, "
					+ "end_y "+ this.INTEGER +" NOT NULL, "
					+ this.PRIMARY_KEY+" (parent_id, parent_version, rank)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"elements");
			request("CREATE TABLE "+ this.schema +"elements ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "class "+ this.OBJECTID +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +", "
					+ "documentation "+ this.TEXT +", "
					+ "type "+ this.TYPE +", "
					+ "created_by "+ this.USERNAME +" NOT NULL, "
					+ "created_on "+ this.DATETIME +" NOT NULL, "
					+ "checkedin_by "+ this.USERNAME +", "
					+ "checkedin_on "+ this.DATETIME +", "
					+ "deleted_by "+ this.USERNAME +", "
					+ "deleted_on "+ this.DATETIME +", "
					+ "checksum "+ this.OBJECTID +" NOT NULL,"
					+ this.PRIMARY_KEY+" (id, version)"
					+ ")");					

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"elements_in_model");
			request("CREATE TABLE "+this.schema+"elements_in_model ("
					+ "eim_id "+ this.AUTO_INCREMENT +", "
					+ "element_id "+ this.OBJECTID +" NOT NULL, "
					+ "element_version "+ this.INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_version "+ this.INTEGER +" NOT NULL, "
					+ "rank "+ this.INTEGER +" NOT NULL"
					+ (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (eim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+this.schema+"seq_elements");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_elements_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+this.schema+"seq_elements_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+this.schema+"trigger_elements_in_model");
				request("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_elements_in_model "
						+ "BEFORE INSERT ON "+this.schema+"elements_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schema+"seq_elements_in_model.NEXTVAL INTO :NEW.eim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"folders");
			request("CREATE TABLE "+this.schema+"folders ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "type "+ this.INTEGER +" NOT NULL, "
					+ "root_type "+ this.INTEGER +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +", "
					+ "documentation "+ this.TEXT +", "
					+ "created_by "+ this.USERNAME +", "
					+ "created_on "+ this.DATETIME +", "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"folders_in_model");
			request("CREATE TABLE "+this.schema+"folders_in_model ("
					+ "fim_id "+ this.AUTO_INCREMENT+", "
					+ "folder_id "+ this.OBJECTID +" NOT NULL, "
					+ "folder_version "+ this.INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ this.OBJECTID +", "
					+ "model_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_version "+ this.INTEGER +" NOT NULL, "
					+ "rank "+ this.INTEGER +" NOT NULL"
					+ (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (fim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+this.schema+"seq_folders_in_model");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_folders_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+this.schema+"seq_folders_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+this.schema+"trigger_folders_in_model");
				request("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_folders_in_model "
						+ "BEFORE INSERT ON "+this.schema+"folders_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schema+"seq_folders_in_model.NEXTVAL INTO :NEW.fim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"images");
			request("CREATE TABLE "+this.schema+"images ("
					+ "path "+ this.OBJECTID +" NOT NULL, "
					+ "image "+ this.IMAGE +" NOT NULL, "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (path)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"models");
			request("CREATE TABLE "+this.schema+"models ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +" NOT NULL, "
					+ "note "+ this.TEXT +", "
					+ "purpose "+ this.TEXT +", "
					+ "created_by "+ this.USERNAME +" NOT NULL, "
					+ "created_on "+ this.DATETIME +" NOT NULL, "
					+ "checkedin_by "+ this.USERNAME +", "
					+ "checkedin_on "+ this.DATETIME +", "
					+ "deleted_by "+ this.USERNAME +", "
					+ "deleted_on "+ this.DATETIME +", "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"properties");
			request("CREATE TABLE "+this.schema+"properties ("
					+ "parent_id "+this.OBJECTID +" NOT NULL, "
					+ "parent_version "+ this.INTEGER +" NOT NULL, "
					+ "rank "+ this.INTEGER +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +", "
					+ "value "+ this.TEXT +", "
					+ this.PRIMARY_KEY+" (parent_id, parent_version, rank)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"relationships");
			request("CREATE TABLE "+this.schema+"relationships ("
					+ "id "+this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "class "+ this.OBJECTID +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +", "
					+ "documentation "+ this.TEXT +", "
					+ "source_id "+ this.OBJECTID +", "
					+ "target_id "+ this.OBJECTID +", "
					+ "strength "+ this.STRENGTH +", "
					+ "access_type "+ this.INTEGER +", "
					+ "created_by "+ this.USERNAME +" NOT NULL, "
					+ "created_on "+ this.DATETIME +" NOT NULL, "
					+ "checkedin_by "+ this.USERNAME +", "
					+ "checkedin_on "+ this.DATETIME +", "
					+ "deleted_by "+ this.USERNAME +", "
					+ "deleted_on "+ this.DATETIME +", "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"relationships_in_model");
			request("CREATE TABLE "+this.schema+"relationships_in_model ("
					+ "rim_id "+ this.AUTO_INCREMENT+", "
					+ "relationship_id "+ this.OBJECTID +" NOT NULL, "
					+ "relationship_version "+ this.INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_version "+ this.INTEGER +" NOT NULL, "
					+ "rank "+this.INTEGER +" NOT NULL "
					+ (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (rim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+this.schema+"seq_relationships");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_relationships_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+this.schema+"seq_relationships_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+this.schema+"trigger_relationships_in_model");
				request("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_relationships_in_model "
						+ "BEFORE INSERT ON "+this.schema+"relationships_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schema+"seq_relationships_in_model.NEXTVAL INTO :NEW.rim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"views");
			request("CREATE TABLE "+this.schema+"views ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "class "+ this.OBJECTID +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +", "
					+ "documentation "+ this.TEXT +" , "
					+ "hint_content "+ this.TEXT +", "
					+ "hint_title "+ this.OBJ_NAME +", "
					+ "created_by "+ this.USERNAME +" NOT NULL, "
					+ "created_on "+ this.DATETIME +" NOT NULL, "
					+ "background "+ this.INTEGER +", "
					+ "connection_router_type "+ this.INTEGER +" NOT NULL, "
					+ "viewpoint "+ this.OBJECTID +", "
					+ "screenshot "+ this.IMAGE +", "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"views_connections");
			request("CREATE TABLE "+this.schema+"views_connections ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "container_id "+ this.OBJECTID +" NOT NULL, "
					+ "view_id "+ this.OBJECTID +" NOT NULL, "
					+ "view_version "+ this.INTEGER +" NOT NULL, "
					+ "class "+ this.OBJECTID +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +", "					// connection must store a name because all of them are not linked to a relationship
					+ "documentation "+ this.TEXT +", "
					+ "is_locked "+ this.BOOLEAN +", "
					+ "line_color "+ this.COLOR +", "
					+ "line_width "+ this.INTEGER +", "
					+ "font "+ this.FONT +", "
					+ "font_color "+ this.COLOR +", "
					+ "relationship_id "+ this.OBJECTID +", "
					+ "relationship_version "+ this.INTEGER +", "
					+ "source_connections "+ this.TEXT + ", "
					+ "target_connections "+ this.TEXT + ", "
					+ "source_object_id "+ this.OBJECTID +", "
					+ "target_object_id "+ this.OBJECTID +", "
					+ "text_position "+ this.INTEGER +", "
					+ "type "+ this.INTEGER +", "
					+ "rank "+ this.INTEGER +" NOT NULL, "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version, container_id)"
					+ ")");

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"views_in_model");
			request("CREATE TABLE "+this.schema+"views_in_model ("
					+ "vim_id "+ this.AUTO_INCREMENT +", "
					+ "view_id "+ this.OBJECTID +" NOT NULL, "
					+ "view_version "+ this.INTEGER +" NOT NULL, "
					+ "parent_folder_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_id "+ this.OBJECTID +" NOT NULL, "
					+ "model_version "+ this.INTEGER +" NOT NULL, "
					+ "rank "+ this.INTEGER
					+ (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (vim_id)") )
					+ ")");
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ) {
				if ( logger.isDebugEnabled() ) logger.debug("creating sequence "+this.schema+"seq_views");
				request("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_views_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				request("CREATE SEQUENCE "+this.schema+"seq_views_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("creating trigger "+this.schema+"trigger_views_in_model");
				request("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_views_in_model "
						+ "BEFORE INSERT ON "+this.schema+"views_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schema+"seq_views_in_model.NEXTVAL INTO :NEW.vim_id FROM DUAL;"
						+ "END;");
			}

			if ( logger.isDebugEnabled() ) logger.debug("creating table "+this.schema+"views_objects");
			request("CREATE TABLE "+this.schema+"views_objects ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "container_id "+ this.OBJECTID +" NOT NULL, "
					+ "view_id "+ this.OBJECTID +" NOT NULL, "
					+ "view_version "+ this.INTEGER +" NOT NULL, "
					+ "class "+ this.OBJECTID +" NOT NULL, "
					+ "element_id "+ this.OBJECTID +", "
					+ "element_version "+ this.INTEGER +", "
					+ "diagram_ref_id "+ this.OBJECTID +", "
					+ "border_color "+ this.COLOR +", "
					+ "border_type "+ this.INTEGER +", "
					+ "content "+ this.TEXT +", "
					+ "documentation "+ this.TEXT +", "
					+ "hint_content "+ this.TEXT +", "
					+ "hint_title "+ this.OBJ_NAME +", "
					+ "is_locked "+ this.BOOLEAN +", "
					+ "image_path "+ this.OBJECTID +", "
					+ "image_position "+ this.INTEGER +", "
					+ "line_color "+ this.COLOR +", "
					+ "line_width "+ this.INTEGER +", "
					+ "fill_color "+ this.COLOR +", "
					+ "font "+ this.FONT +", "
					+ "font_color "+ this.COLOR +", "
					+ "name "+ this.OBJ_NAME +", "
					+ "notes "+ this.TEXT +", "
					+ "source_connections "+ this.TEXT + ", "
					+ "target_connections "+ this.TEXT + ", "
					+ "text_alignment "+ this.INTEGER +", "
					+ "text_position "+ this.INTEGER +", "
					+ "type "+ this.INTEGER +", "
					+ "x "+ this.INTEGER +", "
					+ "y "+ this.INTEGER +", "
					+ "width "+ this.INTEGER +", "
					+ "height "+ this.INTEGER +", "
					+ "rank "+ this.INTEGER +" NOT NULL, "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version, container_id)"
					+ ")");

			commit();
			setAutoCommit(true);

			DBGui.popup(Level.INFO,"The database has been successfully initialized.");

		} catch (SQLException err) {
			rollback();
			setAutoCommit(true);
			// we delete the archi_plugin table because for some databases, DDL cannot be rolled back
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") )
				request("BEGIN EXECUTE IMMEDIATE 'DROP TABLE "+this.schema+"database_version'; EXCEPTION WHEN OTHERS THEN NULL; END;");
			else
				request("DROP TABLE IF EXISTS "+this.schema+"database_version");
			throw err;
		}

	}


	/**
	 * Upgrades the database
	 * @throws ClassNotFoundException 
	 */
	private void upgradeDatabase(int version) throws SQLException, ClassNotFoundException {
		String COLUMN = DBPlugin.areEqual(this.databaseEntry.getDriver(), "sqlite") ? "COLUMN" : "";
		
		int fromVersion = version;
		
		// convert from version 200 to 201 :
		//      - add a blob column into the views table
		if ( fromVersion == 200 ) {
			setAutoCommit(false);
			request("ALTER TABLE "+this.schema+"views ADD "+COLUMN+" screenshot "+this.IMAGE);
			
			request("UPDATE "+this.schema+"database_version SET version = 201 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			setAutoCommit(true);

			fromVersion = 201;
		}

		// convert from version 201 to 202 :
		//      - add a text_position column in the views_connections table
		//      - add source_connections and target_connections to views_objects and views_connections tables
		if ( fromVersion == 201 ) {
			setAutoCommit(false);
			request("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" text_position "+this.INTEGER);
			request("ALTER TABLE "+this.schema+"views_objects ADD "+COLUMN+" source_connections "+this.TEXT);
			request("ALTER TABLE "+this.schema+"views_objects ADD "+COLUMN+" target_connections "+this.TEXT);
			request("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" source_connections "+this.TEXT);
			request("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" target_connections "+this.TEXT);
			
			request("UPDATE "+this.schema+"database_version SET version = 202 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			setAutoCommit(true);

			fromVersion = 202;
		}
		
		// convert from version 202 to 203 :
		//      - add a text_position column in the views_connections table
		//      - add source_connections and target_connections to views_objects and views_connections tables
		if ( fromVersion == 202 ) {
			setAutoCommit(false);
			request("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" relationship_version "+this.INTEGER);
			request("UPDATE "+this.schema+"views_connections SET relationship_version = 1");
			
			request("ALTER TABLE "+this.schema+"views_objects ADD "+COLUMN+" element_version "+this.INTEGER);
			request("UPDATE "+this.schema+"views_objects SET element_version = 1");
			
			request("UPDATE "+this.schema+"database_version SET version = 203 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
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
			
			request("ALTER TABLE "+this.schema+"models RENAME TO "+this.schema+"models_old");
			
			request("CREATE TABLE "+this.schema+"models ("
					+ "id "+ this.OBJECTID +" NOT NULL, "
					+ "version "+ this.INTEGER +" NOT NULL, "
					+ "name "+ this.OBJ_NAME +" NOT NULL, "
					+ "note "+ this.TEXT +", "
					+ "purpose "+ this.TEXT +", "
					+ "created_by "+ this.USERNAME +" NOT NULL, "
					+ "created_on "+ this.DATETIME +" NOT NULL, "
					+ "checkedin_by "+ this.USERNAME +", "
					+ "checkedin_on "+ this.DATETIME +", "
					+ "deleted_by "+ this.USERNAME +", "
					+ "deleted_on "+ this.DATETIME +", "
					+ "checksum "+ this.OBJECTID +" NOT NULL, "
					+ this.PRIMARY_KEY+" (id, version)"
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
					result.close();
					DBGui.popup(Level.FATAL, "Failed to calculate models checksum.", err);
					rollback();
					return;
				}
				insert(this.schema+"models", columns,
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
			
			request("UPDATE "+this.schema+"database_version SET version = 204 WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			commit();
			
			// SQLite refuses to drop the table if we do not close the connection and reopen it
			this.connection.close();
			openConnection();
	        setAutoCommit(false);
			request("DROP TABLE "+this.schema+"models_old");
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
	private final static <T> void constructStatement(PreparedStatement pstmt, String request, T... parameters) throws SQLException {
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
				if ( logger.isTraceEnabled() ) debugRequest.append(String.valueOf(parameters[parameterRank]));
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
				} catch (@SuppressWarnings("unused") Exception err) {
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
	@SuppressWarnings("resource")
	public final <T> ResultSet select(String request, T... parameters) throws SQLException {
		assert (this.connection != null);

		ResultSet result = null;
		try {

			PreparedStatement pstmt = this.preparedStatementMap.get(request);
			if ( pstmt == null ) {
				pstmt = this.connection.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				this.preparedStatementMap.put(request, pstmt);
			} else
				pstmt.clearParameters();
			
			constructStatement(pstmt, request, parameters);

			result = pstmt.executeQuery();
		} catch (SQLException err) {
			// in case of an SQLException, we log the raw request to ease the debug process
			if ( logger.isTraceEnabled() ) logger.trace("SQL Exception for database request : "+request);
			throw err;
		}

		return result;
	}

	/**
	 * wrapper to generate and execute a INSERT request in the database
	 * One may just provide the column names and the corresponding values as parameters
	 * the wrapper automatically generates the VALUES part of the request 
	 * @return The number of lines inserted in the table
	 */
	@SafeVarargs
	public final <T> int insert(String table, String[] columns, T...parameters) throws SQLException {
		assert (this.connection != null);

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
	@SuppressWarnings("resource")
	public final <T> int request(String request, T... parameters) throws SQLException {
		assert (this.connection != null);
		int rowCount = 0;

		if ( parameters.length == 0 ) {		// no need to use a PreparedStatement
			if ( logger.isTraceEnabled() ) logger.trace(request);

			Statement stmt = this.connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			rowCount = stmt.executeUpdate(request);
			stmt.close();
			stmt=null;
		} else {
			PreparedStatement pstmt = this.preparedStatementMap.get(request);

			if ( pstmt == null || pstmt.isClosed() ) {
				pstmt = this.connection.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				this.preparedStatementMap.put(request, pstmt);
			} else {
				pstmt.clearParameters();
				pstmt.clearWarnings();
			}

			constructStatement(pstmt, request, parameters);

			// on PostGreSQL databases, we can only send new requests if we rollback the transaction that caused the exception
			Savepoint savepoint = null;
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "postgresql") ) savepoint = this.connection.setSavepoint();
			try {
				rowCount = pstmt.executeUpdate();
			} catch (SQLException e) {
				if ( savepoint != null ) {
					try {
						this.connection.rollback(savepoint);
						if ( logger.isTraceEnabled() ) logger.trace("Rolled back to savepoint");
					} catch (SQLException e2) { logger.error("Failed to rollback to savepoint", e2); }
				}
				
				// on sqlite databases, the prepared statement must be closed (then re-created) after the exception else we've got "statement is not executing" error messages
				if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "sqlite") ) {
					this.preparedStatementMap.remove(request);
					pstmt.close();
				}
				throw e;
			} finally {
				if ( savepoint != null ) this.connection.releaseSavepoint(savepoint);
			}
		}

		return rowCount;
	}

	/**
	 * Sets the auto-commit mode of the database
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Setting database auto commit to "+String.valueOf(autoCommit));
		this.connection.setAutoCommit(autoCommit);
		if ( autoCommit )
			this.lastTransactionTimestamp = null;                                                         // all the request will have their own timetamp
		else
			this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());    // all the requests will have the same timestamp
	}

	/**
	 * Commits the current transaction
	 */
	public void commit() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Commiting database transaction.");
		this.connection.commit();
		this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
	}

	/**
	 * Rollbacks the current transaction
	 */
	public void rollback() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Rollbacking database transaction.");
		this.connection.rollback();
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
	@SuppressWarnings("resource")
	public HashMap<String, Object> getObject(String id, String clazz, int objectVersion) throws Exception {
		ResultSet result = null;
		int version = objectVersion;
		
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

		HashMap<String, Object> hashResult;
		if ( result.next() ) {
			version = result.getInt("version");
			
			hashResult = resultSetToHashMap(result);
			if ( DBPlugin.areEqual(clazz,  "IFolder") ) hashResult.put("class", "Folder");                  // the folders table does not have a class column, so we add the property by hand

			// properties
			ResultSet resultProperties = select("SELECT count(*) as count_properties FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ?", id, version);
			resultProperties.next();
			String[][] databaseProperties = new String[resultProperties.getInt("count_properties")][2];
			resultProperties.close();
	
			resultProperties = select("SELECT name, value FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version );
			int i = 0;
			while ( resultProperties.next() ) {
				databaseProperties[i++] = new String[] { resultProperties.getString("name"), result.getString("value") };
			}
			hashResult.put("properties", databaseProperties);
			resultProperties.close();
			
			//TODO: gather the views objects if it is a view
			if ( DBPlugin.areEqual(clazz,  "IDiagramModel") ) {
				int countChildren;
				
				ResultSet resultChildren = select("select count(*) AS count_children FROM "+this.schema+"views_objects WHERE view_id = ? AND view_version = ?", id, version);
				resultChildren.next();
				countChildren = resultChildren.getInt("count_children");
				resultChildren.close();
				
				@SuppressWarnings("unchecked")
				HashMap<String, Object>[] children = new HashMap[countChildren];
				resultChildren = select("select id, version FROM "+this.schema+"views_objects WHERE view_id = ? AND view_version = ?", id, version);
				i=0;
				while ( resultChildren.next() ) {
					children[i++] = getObject(resultChildren.getString("id"), "IDiagramModelArchimateObject", result.getInt("version"));
				}
				resultChildren.close();
				hashResult.put("children", children);
			}
			
			// bendpoints
			ResultSet resultBendpoints = select("SELECT count(*) as count_bendpoints FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ?", id, version);
			resultBendpoints.next();
			Integer[][] databaseBendpoints = new Integer[resultBendpoints.getInt("count_bendpoints")][4];
			resultBendpoints.close();
			
			resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY RANK", id, version );
			int j = 0;
			while ( result.next() ) {
				databaseBendpoints[j++] = new Integer[] { resultBendpoints.getInt("start_x"), resultBendpoints.getInt("start_y"), resultBendpoints.getInt("end_x"), resultBendpoints.getInt("end_y") };
			}
			resultBendpoints.close();
			hashResult.put("bendpoints", databaseBendpoints);
		} else
			hashResult = new HashMap<String, Object>();

		result.close();
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

	/**
	 * Gets the list of models in the current database
	 * @param filter (use "%" as wildcard) 
	 * @throws Exception
	 * @return a list of Hashtables, each containing the name and the id of one model
	 */
	public ArrayList<Hashtable<String, Object>> getModels(String filter) throws Exception {
	    ArrayList<Hashtable<String, Object>> list = new ArrayList<Hashtable<String, Object>>();
	    
		ResultSet result;

		// We do not use a GROUP BY because it does not give the expected result on PostGresSQL ...   
		if ( filter==null || filter.length()==0 )
			result = select("SELECT id, name, version FROM "+this.schema+"models m WHERE version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = m.id) ORDER BY name");
		else
			result = select("SELECT id, name, version FROM "+this.schema+"models m WHERE version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = m.id) AND UPPER(name) like UPPER(?) ORDER BY name", filter);

		while ( result.next() && result.getString("id") != null ) {
			if (logger.isTraceEnabled() ) logger.trace("found model \""+result.getString("name")+"\"");
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			table.put("name", result.getString("name"));
			table.put("id", result.getString("id"));
			list.add(table);
		}
		result.close();
		
		return list;
	}
	
	public String getModelId(String modelName, boolean ignoreCase) throws Exception {
	    ResultSet result;
	    String id = null;
	    
	    // Using a GROUP BY on PostGresSQL does not give the expected result ...   
        if ( ignoreCase )
            result = select("SELECT id FROM "+this.schema+"models m WHERE UPPER(name) = UPPER(?)", modelName);
        else
            result = select("SELECT id FROM "+this.schema+"models m WHERE name = ?", modelName);

        if ( result.next() ) 
            id = result.getString("id");
        
        result.close();
        
        return id;
	}

	/**
	 * Gets the list of versions on a model in the current database
	 * @param id the id of the model 
	 * @throws Exception
	 * @return a list of Hashtables, each containing the version, created_on, created_by, name, note and purpose of one version of the model
	 */
	public ArrayList<Hashtable<String, Object>> getModelVersions(String id) throws Exception {
	    ArrayList<Hashtable<String, Object>> list = new ArrayList<Hashtable<String, Object>>();
	    
		ResultSet result = select("SELECT version, created_by, created_on, name, note, purpose FROM "+this.schema+"models WHERE id = ? ORDER BY version DESC", id);

		while ( result.next() ) {
			if (logger.isTraceEnabled() ) logger.trace("found version \""+result.getString("version")+"\"");
			
			if (logger.isTraceEnabled() ) logger.trace("found model \""+result.getString("name")+"\"");
			Hashtable<String, Object> table = new Hashtable<String, Object>();
			table.put("version", result.getString("version"));
			table.put("created_by", result.getString("created_by"));
			table.put("created_on", result.getTimestamp("created_on"));
			table.put("name", result.getString("name"));
			table.put("note", result.getString("note"));
			table.put("purpose", result.getString("purpose"));
			list.add(table);
		}
		result.close();
		
		return list;
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
	@SuppressWarnings("resource")
	public int importModelMetadata(ArchimateModel model) throws Exception {
		// reseting the model's counters
		model.resetCounters();

		if ( model.getCurrentVersion().getVersion() == 0 ) {
		    ResultSet result = select("SELECT MAX(version) AS version FROM "+this.schema+"models WHERE id = ?", model.getId());
		    if ( result.next() ) {
		        model.getCurrentVersion().setVersion(result.getInt("version"));
		    }
		    result.close();
		}

		//TODO : manage the "real" model metadata :-)
		ResultSet result = select("SELECT name, purpose FROM "+this.schema+"models WHERE id = ? AND version = ?", model.getId(), model.getCurrentVersion().getVersion());
		result.next();
		model.setPurpose(result.getString("purpose"));
		result.close();

		importProperties(model);

		String documentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ? "TO_CHAR(documentation)" : "documentation";
		
		String elementsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ? "TO_CHAR(elements.documentation) AS documentation" : "elements.documentation";
		if ( model.getImportLatestVersion() ) {
			this.importElementsRequest = "SELECT DISTINCT element_id, parent_folder_id, version, class, name, type, documentation, created_on"+
									" FROM ("+
									"	SELECT elements_in_model.element_id, elements_in_model.parent_folder_id, elements.version, elements.class, elements.name, elements.type, "+elementsDocumentation+", elements.created_on"+
									"	FROM "+this.schema+"elements_in_model"+
									"	JOIN "+this.schema+"elements ON elements.id = elements_in_model.element_id AND elements.version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE elements.id = elements_in_model.element_id)"+
									"	WHERE model_id = ? AND model_version = ?"+
									" UNION"+
									"	SELECT views_objects.element_id, null as parent_folder_id, elements.version, elements.class, elements.name, elements.type, "+elementsDocumentation+", elements.created_on"+
									"	FROM "+this.schema+"views_in_model"+
									"	JOIN "+this.schema+"views ON views.id = views_in_model.view_id"+
									"	JOIN "+this.schema+"views_objects ON views_objects.view_id = views.id AND views_objects.version = views.version"+
									"	JOIN "+this.schema+"elements ON elements.id = views_objects.element_id AND elements.version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE elements.id = views_objects.element_id)"+
									"	WHERE model_id = ? AND model_version = ? AND element_id IS NOT null"+
									"   AND views_objects.element_id NOT IN ("+
									"      SELECT elements_in_model.element_id"+
									"	   FROM "+this.schema+"elements_in_model"+
									"	   JOIN "+this.schema+"elements ON elements.id = elements_in_model.element_id AND elements.version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE elements.id = elements_in_model.element_id)"+
									"	   WHERE model_id = ? AND model_version = ?"+
									"   )"+
									" ) elements GROUP BY element_id, parent_folder_id, version, class, name, type, documentation, created_on";
			result = select("SELECT COUNT(*) AS countElements FROM ("+this.importElementsRequest+") elts"
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		} else {
			this.importElementsRequest = "SELECT DISTINCT elements_in_model.element_id, elements_in_model.parent_folder_id, elements.version, elements.class, elements.name, elements.type, "+elementsDocumentation+", elements.created_on"+
									" FROM "+this.schema+"elements_in_model"+
									" JOIN "+this.schema+"elements ON elements.id = elements_in_model.element_id AND elements.version = elements_in_model.element_version"+
									" WHERE model_id = ? AND model_version = ?"+
									" GROUP BY element_id, parent_folder_id, version, class, name, type, "+documentation+", created_on";
			result = select("SELECT COUNT(*) AS countElements FROM ("+this.importElementsRequest+") elts"
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		}

		result.next();
		this.countElementsToImport = result.getInt("countElements");
		this.countElementsImported = 0;
		result.close();
		result=null;

		String relationshipsDocumentation = DBPlugin.areEqual(this.databaseEntry.getDriver(), "oracle") ? "TO_CHAR(relationships.documentation) AS documentation" : "relationships.documentation";
		if ( model.getImportLatestVersion() ) {
			this.importRelationshipsRequest = "SELECT DISTINCT relationship_id, parent_folder_id, version, class, name, documentation, source_id, target_id, strength, access_type, created_on"+
										 " FROM ("+
					 					 "	SELECT relationships_in_model.relationship_id, relationships_in_model.parent_folder_id, relationships.version, relationships.class, relationships.name, "+relationshipsDocumentation+", relationships.source_id, relationships.target_id, relationships.strength, relationships.access_type, relationships.created_on"+
										 "	FROM "+this.schema+"relationships_in_model"+
										 "	JOIN "+this.schema+"relationships ON relationships.id = relationships_in_model.relationship_id AND relationships.version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE relationships.id = relationships_in_model.relationship_id)"+
										 "	WHERE model_id = ? AND model_version = ?"+
										 " UNION"+
										 "	SELECT views_connections.relationship_id, null as parent_folder_id, relationships.version, relationships.class, relationships.name, "+relationshipsDocumentation+", relationships.source_id, relationships.target_id, relationships.strength, relationships.access_type, relationships.created_on"+
										 "	FROM "+this.schema+"views_in_model"+
										 "	JOIN "+this.schema+"views ON views.id = views_in_model.view_id"+
										 "	JOIN "+this.schema+"views_connections ON views_connections.view_id = views.id AND views_connections.version = views.version"+
										 "	JOIN "+this.schema+"relationships ON relationships.id = views_connections.relationship_id AND relationships.version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE relationships.id = views_connections.relationship_id)"+
										 "	WHERE model_id = ? AND model_version = ? and relationship_id IS NOT null"+
										 "  AND views_connections.relationship_id NOT IN ("+
										 "     SELECT relationships_in_model.relationship_id"+
										 "	   FROM "+this.schema+"relationships_in_model"+
										 "	   JOIN "+this.schema+"relationships ON relationships.id = relationships_in_model.relationship_id AND relationships.version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE relationships.id = relationships_in_model.relationship_id)"+
										 "	   WHERE model_id = ? AND model_version = ?"+
										 "  )"+
										 " ) relationships GROUP BY relationship_id, parent_folder_id, version, class, name, documentation, source_id, target_id, strength, access_type, created_on";
			result = select("SELECT COUNT(*) AS countRelationships FROM ("+this.importRelationshipsRequest+") relts"
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		} else {
			this.importRelationshipsRequest = "SELECT relationships_in_model.relationship_id, relationships_in_model.parent_folder_id, relationships.version, relationships.class, relationships.name, "+relationshipsDocumentation+", relationships.source_id, relationships.target_id, relationships.strength, relationships.access_type, relationships.created_on"+
										 " FROM "+this.schema+"relationships_in_model"+
										 " INNER JOIN "+this.schema+"relationships ON relationships.id = relationships_in_model.relationship_id AND relationships.version = relationships_in_model.relationship_version"+
										 " WHERE model_id = ? AND model_version = ?"+
										 " GROUP BY relationship_id, parent_folder_id, version, class, name, "+documentation+", source_id, target_id, strength, access_type, created_on";
			result = select("SELECT COUNT(*) AS countRelationships FROM ("+this.importRelationshipsRequest+") relts"
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		}
		result.next();
		this.countRelationshipsToImport = result.getInt("countRelationships");
		this.countRelationshipsImported = 0;
		result.close();
		result=null;

		if ( model.getImportLatestVersion() ) {
			this.importFoldersRequest = "SELECT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on"+
									" FROM "+this.schema+"folders_in_model"+
									" JOIN "+this.schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = (SELECT MAX(version) FROM "+this.schema+"folders WHERE folders.id = folders_in_model.folder_id)"+
									" WHERE model_id = ? AND model_version = ?";
		} else {
			this.importFoldersRequest = "SELECT folder_id, folder_version, parent_folder_id, type, root_type, name, documentation, created_on"+
									" FROM "+this.schema+"folders_in_model"+
									" JOIN "+this.schema+"folders ON folders.id = folders_in_model.folder_id AND folders.version = folders_in_model.folder_version"+
									" WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countFolders FROM ("+this.importFoldersRequest+") fldrs"
				,model.getId()
				,model.getCurrentVersion().getVersion()
				);
		result.next();
		this.countFoldersToImport = result.getInt("countFolders");
		this.countFoldersImported = 0;
		result.close();
		result=null;
		this.importFoldersRequest += " ORDER BY folders_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			this.importViewsRequest = "SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on"+
								 " FROM "+this.schema+"views_in_model"+
								 " JOIN "+this.schema+"views ON views.id = views_in_model.view_id AND views.version = (select max(version) from "+this.schema+"views where views.id = views_in_model.view_id)"+
								 " WHERE model_id = ? AND model_version = ?";
		} else {
			this.importViewsRequest = "SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint, created_on"+
								 " FROM "+this.schema+"views_in_model"+
								 " JOIN "+this.schema+"views ON views.id = views_in_model.view_id AND views.version = views_in_model.view_version"+
								 " WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countViews FROM ("+this.importViewsRequest+") vws"
				,model.getId()
				,model.getCurrentVersion().getVersion()
				);
		result.next();
		this.countViewsToImport = result.getInt("countViews");
		this.countViewsImported = 0;
		result.close();
		result=null;
		this.importViewsRequest += " ORDER BY views_in_model.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			this.importViewsObjectsRequest = "SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height"+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_objects ON views_objects.view_id = views_in_model.view_id AND views_objects.view_version = (SELECT MAX(version) FROM "+this.schema+"views_objects WHERE views_objects.view_id = views_in_model.view_id)"+
										" WHERE model_id = ? AND model_version = ?";
		} else {
			this.importViewsObjectsRequest = "SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, source_connections, target_connections, text_alignment, text_position, type, x, y, width, height"+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_objects ON views_objects.view_id = views_in_model.view_id AND views_objects.view_version = views_in_model.view_version"+
										" WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countViewsObjects FROM ("+this.importViewsObjectsRequest+") vobjs"
				,model.getId()
				,model.getCurrentVersion().getVersion()
				);
		result.next();
		this.countViewObjectsToImport = result.getInt("countViewsObjects");
		this.countViewObjectsImported = 0;
		result.close();
		result=null;
		this.importViewsObjectsRequest += " ORDER BY views_objects.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		if ( model.getImportLatestVersion() ) {
			this.importViewsConnectionsRequest = "SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type "+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_connections ON views_connections.view_id = views_in_model.view_id AND views_connections.view_version = (SELECT MAX(version) FROM "+this.schema+"views_connections WHERE views_connections.view_id = views_in_model.view_id)"+
										" WHERE model_id = ? AND model_version = ?";
		} else {
			this.importViewsConnectionsRequest = "SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_connections, target_connections, source_object_id, target_object_id, text_position, type"+
										" FROM "+this.schema+"views_in_model"+
										" JOIN "+this.schema+"views_connections ON views_connections.view_id = views_in_model.view_id AND views_connections.view_version = views_in_model.view_version"+
										" WHERE model_id = ? AND model_version = ?";
		}
		result = select("SELECT COUNT(*) AS countViewsConnections FROM ("+this.importViewsConnectionsRequest+") vcons"
				,model.getId()
				,model.getCurrentVersion().getVersion()
				);
		result.next();
		this.countViewConnectionsToImport = result.getInt("countViewsConnections");
		this.countViewConnectionsImported = 0;
		result.close();
		result=null;
		this.importViewsConnectionsRequest += " ORDER BY views_connections.rank";				// we need to put aside the ORDER BY from the SELECT FROM SELECT because of SQL Server

		result = select("SELECT COUNT(DISTINCT image_path) AS countImages FROM "+this.schema+"views_in_model"+
						" INNER JOIN "+this.schema+"views ON "+this.schema+"views_in_model.view_id = views.id AND "+this.schema+"views_in_model.view_version = views.version"+
						" INNER JOIN "+this.schema+"views_objects ON views.id = "+this.schema+"views_objects.view_id AND views.version = "+this.schema+"views_objects.version"+
						" INNER JOIN "+this.schema+"images ON "+this.schema+"views_objects.image_path = images.path"+
						" WHERE model_id = ? AND model_version = ? AND path IS NOT NULL" 
				,model.getId()
				,model.getCurrentVersion().getVersion()
				);
		result.next();
		this.countImagesToImport = result.getInt("countImages");
		this.countImagesImported = 0;
		result.close();
		result=null;

		if ( logger.isDebugEnabled() ) logger.debug("Importing "+this.countElementsToImport+" elements, "+this.countRelationshipsToImport+" relationships, "+this.countFoldersToImport+" folders, "+this.countViewsToImport+" views, "+this.countViewObjectsToImport+" views objects, "+this.countViewConnectionsToImport+" views connections, and "+this.countImagesToImport+" images.");

		// initializing the HashMaps that will be used to reference imported objects
		this.allImagePaths = new HashSet<String>();

		return this.countElementsToImport + this.countRelationshipsToImport + this.countFoldersToImport + this.countViewsToImport + this.countViewObjectsToImport + this.countViewConnectionsToImport + this.countImagesToImport;
	}

	/**
	 * Prepare the import of the folders from the database
	 */
	public void prepareImportFolders(ArchimateModel model) throws Exception {
		this.currentResultSet = select(this.importFoldersRequest
				,model.getId()
				,model.getCurrentVersion().getVersion()
				);
	}

	/**
	 * Import the folders from the database
	 */
	public boolean importFolders(ArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IFolder folder = DBArchimateFactory.eINSTANCE.createFolder();

				folder.setId(this.currentResultSet.getString("folder_id"));
				((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("folder_version"));
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
	public void prepareImportElements(ArchimateModel model) throws Exception {
		if ( model.getImportLatestVersion() ) {
			this.currentResultSet = select(this.importElementsRequest
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		} else {
			this.currentResultSet = select(this.importElementsRequest
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		}
			
	}

	/**
	 * import the elements from the database
	 */
	public boolean importElements(ArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IArchimateElement element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				element.setId(this.currentResultSet.getString("element_id"));
				((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));
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
	public void prepareImportRelationships(ArchimateModel model) throws Exception {
		if ( model.getImportLatestVersion() ) {
			this.currentResultSet = select(this.importRelationshipsRequest
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		} else {
			this.currentResultSet = select(this.importRelationshipsRequest
					,model.getId()
					,model.getCurrentVersion().getVersion()
					);
		}
	}

	/**
	 * import the relationships from the database
	 */
	public boolean importRelationships(ArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IArchimateRelationship relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				relationship.setId(this.currentResultSet.getString("relationship_id"));
				((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));
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
	public void prepareImportViews(ArchimateModel model) throws Exception {
		this.currentResultSet = select(this.importViewsRequest,
				model.getId(),
				model.getCurrentVersion().getVersion()
				);
	}

	/**
	 * import the views from the database
	 */
	public boolean importViews(ArchimateModel model) throws Exception {
		if ( this.currentResultSet != null ) {
			if ( this.currentResultSet.next() ) {
				IDiagramModel view;
				if ( DBPlugin.areEqual(this.currentResultSet.getString("class"), "CanvasModel") )
					view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(this.currentResultSet.getString("class"));
				else
					view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(this.currentResultSet.getString("class"));

				view.setId(this.currentResultSet.getString("id"));
				((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(this.currentResultSet.getInt("version"));
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
	public boolean importViewsObjects(ArchimateModel model, IDiagramModel view) throws Exception {
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
	public boolean importViewsConnections(ArchimateModel model) throws Exception {
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
					ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM "+this.schema+"bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY rank"
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
	public void importImage(ArchimateModel model, String path) throws Exception {
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

		if ( parent instanceof IArchimateModel )
			currentVersion = ((ArchimateModel)parent).getCurrentVersion().getVersion();
		else
			currentVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

		ResultSet result = select("SELECT name, value FROM "+this.schema+"properties WHERE parent_id = ? AND parent_version = ? ORDER BY rank"
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
	public IArchimateElement importElementFromId(ArchimateModel model, IArchimateDiagramModel view, String elementId, int elementVersion, boolean mustCreateCopy) throws Exception {
		IArchimateElement element;
		List<Object> imported = new ArrayList<Object>();
		boolean newElement = false;

		// TODO add an option to import elements recursively

		ResultSet result;
		if ( elementVersion == 0 )
		    result = select("SELECT version, class, name, documentation, type, created_on FROM "+this.schema+"elements e WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"elements WHERE id = e.id)", elementId);
		else
		    result = select("SELECT version, class, name, documentation, type, created_on FROM "+this.schema+"elements e WHERE id = ? AND version = ?", elementId, elementVersion);

		if ( !result.next() ) {
			result.close();
			if ( elementVersion == 0 )
			    throw new Exception("Element with id="+elementId+" has not been found in the database.");
		    throw new Exception("Element with id="+elementId+" and version="+elementVersion+" has not been found in the database.");
		}

		if ( mustCreateCopy ) {
			if ( logger.isDebugEnabled() ) logger.debug("Importing a copy of element id "+elementId+".");
			element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
			element.setId(model.getIDAdapter().getNewID());
			newElement = true;

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
		++this.countElementsImported;

		// We import the relationships that source or target the element
		result = select("SELECT id, source_id, target_id FROM "+this.schema+"relationships WHERE source_id = ? OR target_id = ?", elementId, elementId);
		while ( result.next() && result.getString("id") != null ) {
			// we import only relationships that do not exist
			if ( model.getAllRelationships().get(result.getString("id")) == null ) {
				IArchimateElement sourceElement = model.getAllElements().get(result.getString("source_id"));
				IArchimateRelationship sourceRelationship = model.getAllRelationships().get(result.getString("source_id"));
				IArchimateElement targetElement = model.getAllElements().get(result.getString("target_id"));
				IArchimateRelationship targetRelationship = model.getAllRelationships().get(result.getString("target_id"));

				// we import only relations when both source and target are in the model
				if ( (sourceElement!=null || sourceRelationship!=null) && (targetElement!=null || targetRelationship!=null) ) {
					imported.add(importRelationshipFromId(model, view, result.getString("id"), 0, false));
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
	public IArchimateRelationship importRelationshipFromId(ArchimateModel model, IArchimateDiagramModel view, String relationshipId, int relationshipVersion, boolean mustCreateCopy) throws Exception {
		boolean newRelationship = false;

        ResultSet result;
        if ( relationshipVersion == 0 )
            result = select("SELECT version, class, name, documentation, source_id, target_id, strength, access_type, created_on FROM "+this.schema+"relationships r WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"relationships WHERE id = r.id)", relationshipId);
        else
            result = select("SELECT version, class, name, documentation, source_id, target_id, strength, access_type, created_on FROM "+this.schema+"relationships r WHERE id = ? AND version = ?", relationshipId, relationshipVersion);

        if ( !result.next() ) {
            result.close();
            if ( relationshipVersion == 0 )
                throw new Exception("Relationship with id="+relationshipId+" has not been found in the database.");
            throw new Exception("Relationship with id="+relationshipId+" and version="+relationshipVersion+" has not been found in the database.");
        }
	        
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
					IDiagramModelArchimateConnection cnct = ArchimateDiagramModelFactory.createDiagramModelArchimateConnection(relationship);
					cnct.setSource(sourceConnection);
					sourceConnection.getSourceConnections().add(cnct);
					cnct.setTarget(targetConnection);
					targetConnection.getTargetConnections().add(cnct);
				}
			}
		}

		result.close();
		result=null;
		++this.countRelationshipsImported;

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
		ResultSet result = select("SELECT version, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint FROM "+this.schema+"views v WHERE version = (SELECT MAX(version) FROM "+this.schema+"views WHERE id = v.id) AND id = ?", id);
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
	
		result.close();
		
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
	public int getLatestModelVersion(ArchimateModel model) throws Exception {
		// we use COALESCE to guarantee that a value is returned, even if the model does not exist in the database
		ResultSet result = select("SELECT COALESCE(MAX(version),0) as version FROM "+this.schema+"models WHERE id = ?", model.getId());
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

	private HashMap<String, DBVersion> elementsNotInModel = new HashMap<String, DBVersion>();			public HashMap<String, DBVersion> getElementsNotInModel() { return this.elementsNotInModel; }
	private HashMap<String, DBVersion> relationshipsNotInModel = new HashMap<String, DBVersion>();		public HashMap<String, DBVersion> getRelationshipsNotInModel() { return this.relationshipsNotInModel; }
	private HashMap<String, DBVersion> foldersNotInModel = new HashMap<String, DBVersion>();			public HashMap<String, DBVersion> getFoldersNotInModel() { return this.foldersNotInModel; }
	private HashMap<String, DBVersion> viewsNotInModel = new HashMap<String, DBVersion>();				public HashMap<String, DBVersion> getViewsNotInModel() { return this.viewsNotInModel; }
	
    /**
     * Gets the versions and checksum of one model's components from the database and fills elementsNotInModel, relationshipsNotInModel, foldersNotInModel and viewsNotInModel.<br>
     * <br>
     * This method is meant to be called during the import process.
     * @param modelId the id of the model
     * @param modelVersion the version of the model (0 to get the latest version of all the components)
     * @throws SQLException
     */
	public void getVersionsFromDatabase(String modelId, int version) throws SQLException, RuntimeException {
        if ( logger.isDebugEnabled() ) logger.debug("Getting versions from the database");
        
        boolean latestVersionOfComponents = false;
        int modelVersion = version;
        
        // if the modelVersion is zero we get the latest version of the database
        if ( modelVersion == 0 ) {
        	latestVersionOfComponents = true;
        	ResultSet result = select("SELECT MAX(version) AS version FROM "+this.schema+"models WHERE id = ?", modelId);
            if ( result.next() && result.getObject("version") != null ) {
            	modelVersion = result.getInt("version");
            }
            result.close();
            if ( modelVersion == 0 )
            	throw new RuntimeException("Cannot find model with id=\""+modelId+"\" in the database.");
        }
        
        // we reset the variables
    	this.elementsNotInModel = new HashMap<String, DBVersion>();
    	this.relationshipsNotInModel = new HashMap<String, DBVersion>();
    	this.foldersNotInModel = new HashMap<String, DBVersion>();
    	this.viewsNotInModel = new HashMap<String, DBVersion>();
        
        // we get the components
    	if ( latestVersionOfComponents ) {
    		ResultSet result;
    		
    		result = select("SELECT e2.id, max(e2version) FROM "+this.schema+"elements e1 JOIN "+this.schema+"elements e2 ON e2.id = e1.id AND e2.version >= e1.version JOIN elements_in_model m ON e2.id = m.element_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
        	while ( result.next() )
              	this.elementsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
        	result.close();
        	
    		result = select("SELECT e2.id, max(e2version) FROM "+this.schema+"relationships e1 JOIN "+this.schema+"relationships e2 ON e2.id = e1.id AND e2.version >= e1.version JOIN relationships_in_model m ON e2.id = m.relationship_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
        	while ( result.next() )
        		this.relationshipsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
        	result.close();
        	
    		result = select("SELECT e2.id, max(e2version) FROM "+this.schema+"folders e1 JOIN "+this.schema+"folders e2 ON e2.id = e1.id AND e2.version >= e1.version JOIN folders_in_model m ON e2.id = m.folder_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
        	while ( result.next() )
        		this.foldersNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
        	result.close();
        	
    		result = select("SELECT e2.id, max(e2version) FROM "+this.schema+"views e1 JOIN "+this.schema+"views e2 ON e2.id = e1.id AND e2.version >= e1.version JOIN views_in_model m ON e2.id = m.view_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
        	while ( result.next() )
        		this.viewsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
        	result.close();
        	
    		result = select("SELECT e2.id, max(e2version) FROM "+this.schema+"elements e1 JOIN "+this.schema+"elements e2 ON e2.id = e1.id AND e2.version >= e1.version JOIN elements_in_model m ON e2.id = m.element_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
        	while ( result.next() )
              	this.elementsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
        	result.close();
    	} else {
    		ResultSet result;
    		
    		result = select("SELECT id, version FROM "+this.schema+"elements e JOIN "+this.schema+"elements_in_model m ON e.id = m.element_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
	    	while ( result.next() )
	          	this.elementsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
	    	result.close();
	    	
    		result = select("SELECT id, version FROM "+this.schema+"relationships e JOIN "+this.schema+"relationships_in_model m ON e.id = m.relationship_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
	    	while ( result.next() )
	    		this.relationshipsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
	    	result.close();
	    	
    		result = select("SELECT id, version FROM "+this.schema+"folders e JOIN "+this.schema+"folders_in_model m ON e.id = m.folder_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
	    	while ( result.next() )
	    		this.foldersNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
	    	result.close();
	    	
    		result = select("SELECT id, version FROM "+this.schema+"views e JOIN "+this.schema+"views_in_model m ON e.id = m.view_id HAVING m.id = ? AND m.version = ?", modelId, modelVersion);
	    	while ( result.next() )
	    		this.viewsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version")));
	    	result.close();
    	}
    }
    
    /**
     * Gets the versions and checksum of one model's components from the database and fills their DBMetadata.<br>
     * <br>
     * Components that are not in the current model are set in elementsNotInModel, relationshipsNotInModel, foldersNotInModel and viewsNotInModel.<br>
     * <br>
     * This method is meant to be called during the export process.
     * @throws SQLException
     */
    @SuppressWarnings("resource")
	public void getVersionsFromDatabase(ArchimateModel model) throws SQLException, RuntimeException {
        if ( logger.isDebugEnabled() ) logger.debug("Getting versions from the database");

        // This method can retrieve versions only if the database contains the whole model tables
        assert(!this.databaseEntry.getExportWholeModel());
        
    	Iterator<Map.Entry<String, IArchimateElement>> ite = model.getAllElements().entrySet().iterator();
    	Iterator<Map.Entry<String, IArchimateRelationship>> itr = model.getAllRelationships().entrySet().iterator();
    	Iterator<Map.Entry<String, IFolder>> itf = model.getAllFolders().entrySet().iterator();
    	Iterator<Map.Entry<String, IDiagramModel>> itv = model.getAllViews().entrySet().iterator();
        
        // we reset the variables
    	this.elementsNotInModel = new HashMap<String, DBVersion>();
    	this.relationshipsNotInModel = new HashMap<String, DBVersion>();
    	this.foldersNotInModel = new HashMap<String, DBVersion>();
    	this.viewsNotInModel = new HashMap<String, DBVersion>();
        
        // we get the latest model version from the database
        model.getDatabaseVersion().reset();
        ResultSet result;
        result = select("SELECT version, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = ?)", model.getId(), model.getId());
        if ( result.next() && result.getObject("version") != null ) {
            // if the timestamp is found, then the model exists in the database
            model.getDatabaseVersion().setLatestVersion(result.getInt("version"));
            model.getDatabaseVersion().setLatestChecksum(result.getString("checksum"));
            model.getDatabaseVersion().setLatestTimestamp(result.getTimestamp("created_on"));
            result.close();
            
            // we check if the model has been imported from (or last exported to) this database
            result = select("SELECT version, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND created_on = ?", model.getId(), model.getCurrentVersion().getTimestamp());
            if ( result.next() && result.getObject("version") != null ) {
                // if the timestamp is found, then the model has been imported from or last exported to the database 
                model.getDatabaseVersion().setVersion(result.getInt("version"));
                model.getDatabaseVersion().setChecksum(result.getString("checksum"));
                model.getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
                
            }
            result.close();
            logger.debug("The model already exists in the database (current version = "+model.getDatabaseVersion().getVersion()+", latest version = "+model.getDatabaseVersion().getLatestVersion()+")");
            
            // we reset all the versions
            while (ite.hasNext()) ((IDBMetadata)ite.next().getValue()).getDBMetadata().getDatabaseVersion().reset();
            while (itr.hasNext()) ((IDBMetadata)itr.next().getValue()).getDBMetadata().getDatabaseVersion().reset();
            while (itf.hasNext()) ((IDBMetadata)itf.next().getValue()).getDBMetadata().getDatabaseVersion().reset();
            while (itv.hasNext()) ((IDBMetadata)itv.next().getValue()).getDBMetadata().getDatabaseVersion().reset();
            
            // we get the components versions from the database.
            // the big joint request allows to get in one go the version of the components as they are in the current model, plus the latest version of those components
            if ( model.getDatabaseVersion().getVersion() != 0 ) {
    	        result = select("SELECT t.id AS id, t.version AS version, t.checksum AS checksum, t.created_on AS created_on, tt.version AS latest_version, tt.checksum AS latest_checksum, tt.created_on AS latest_created_on FROM ("+
    	                "SELECT t1.id, t1.version, t1.checksum, t1.created_on, max(t2.version) AS latest_version FROM "+this.schema+"elements t1 "+
    	        		"JOIN "+this.schema+"elements t2 ON t2.id = t1.id AND t2.version >= t1.version "+
    	                "JOIN "+this.schema+"elements_in_model m ON t1.id = m.element_id AND t1.version = m.element_version "+
    	        		"GROUP BY t1.id, t1.version, t1.checksum, t1.created_on, m.model_id, m.model_version "+
    	                "HAVING  m.model_id = ? AND m.model_version = ? "+
    	        		") as t "+
    	        		"JOIN elements AS tt on tt.id = t.id AND tt.version = t.latest_version",
    	                model.getId(),
    	                model.getDatabaseVersion().getVersion());
    	        while ( result.next() ) {
    	        	IDBMetadata element = (IDBMetadata)model.getAllElements().get(result.getString("id"));
    	            if ( element != null ) {
    	            	element.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
    	            	element.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
    	            	element.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
    	            	element.getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("latest_version"));
    	            	element.getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("latest_checksum"));
    	            	element.getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("latest_created_on"));
    	            	
    	            	element.getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("latest_version"));
    	            } else
    	            	this.elementsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on"), result.getInt("latest_version"), result.getString("latest_checksum"),result.getTimestamp("latest_created_on")));
    	        }
    	        result.close();
            }

            if ( model.getDatabaseVersion().getVersion() != 0 ) {
    	        result = select("SELECT t.id AS id, t.version AS version, t.checksum AS checksum, t.created_on AS created_on, tt.version AS latest_version, tt.checksum AS latest_checksum, tt.created_on AS latest_created_on FROM ("+
    	                "SELECT t1.id, t1.version, t1.checksum, t1.created_on, max(t2.version) AS latest_version FROM "+this.schema+"relationships t1 "+
    	        		"JOIN "+this.schema+"relationships t2 ON t2.id = t1.id AND t2.version >= t1.version "+
    	                "JOIN "+this.schema+"relationships_in_model m ON t1.id = m.relationship_id AND t1.version = m.relationship_version "+
    	        		"GROUP BY t1.id, t1.version, t1.checksum, t1.created_on, m.model_id, m.model_version "+
    	                "HAVING  m.model_id = ? AND m.model_version = ? "+
    	        		") AS t "+
    	        		"JOIN relationships AS tt ON tt.id = t.id AND tt.version = t.latest_version",
    	                model.getId(),
    	                model.getDatabaseVersion().getVersion());
    	        while ( result.next() ) {
    	            IDBMetadata relationship = (IDBMetadata)model.getAllRelationships().get(result.getString("id"));
    	            if ( relationship != null ) {
    	            	relationship.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
    	            	relationship.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
    	            	relationship.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
    	            	relationship.getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("latest_version"));
    	            	relationship.getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("latest_checksum"));
    	            	relationship.getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("latest_created_on"));
    	            	
    	            	relationship.getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("latest_version"));
    	            } else
    	            	this.relationshipsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on"), result.getInt("latest_version"), result.getString("latest_checksum"),result.getTimestamp("latest_created_on")));
    	        }
    	        result.close();
            }
            
            if ( model.getDatabaseVersion().getVersion() != 0 ) {
    	        result = select("SELECT t.id AS id, t.version AS version, t.checksum AS checksum, t.created_on AS created_on, tt.version AS latest_version, tt.checksum AS latest_checksum, tt.created_on AS latest_created_on FROM ("+
    	                "SELECT t1.id, t1.version, t1.checksum, t1.created_on, max(t2.version) AS latest_version FROM "+this.schema+"folders t1 "+
    	        		"JOIN "+this.schema+"folders t2 ON t2.id = t1.id AND t2.version >= t1.version "+
    	                "JOIN "+this.schema+"folders_in_model m ON t1.id = m.folder_id AND t1.version = m.folder_version "+
    	        		"GROUP BY t1.id, t1.version, t1.checksum, t1.created_on, m.model_id, m.model_version "+
    	                "HAVING  m.model_id = ? AND m.model_version = ? "+
    	        		") AS t "+
    	        		"JOIN folders AS tt ON tt.id = t.id AND tt.version = t.latest_version",
    	                model.getId(),
    	                model.getDatabaseVersion().getVersion());
    	        while ( result.next() ) {
    	            IDBMetadata folder = (IDBMetadata)model.getAllFolders().get(result.getString("id"));
    	            if ( folder != null ) {
    	            	folder.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
    	            	folder.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
    	            	folder.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
    	            	folder.getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("latest_version"));
    	            	folder.getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("latest_checksum"));
    	            	folder.getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("latest_created_on"));
    	            	
    	            	folder.getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("latest_version"));
    	            } else
    	            	this.foldersNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on"), result.getInt("latest_version"), result.getString("latest_checksum"),result.getTimestamp("latest_created_on")));
    	        }
    	        result.close();
            }

            if ( model.getDatabaseVersion().getVersion() != 0 ) {
    	        result = select("SELECT t.id AS id, t.version AS version, t.checksum AS checksum, t.created_on AS created_on, tt.version AS latest_version, tt.checksum AS latest_checksum, tt.created_on AS latest_created_on FROM ("+
    	                "SELECT t1.id, t1.version, t1.checksum, t1.created_on, max(t2.version) AS latest_version FROM "+this.schema+"views t1 "+
    	        		"JOIN "+this.schema+"views t2 ON t2.id = t1.id AND t2.version >= t1.version "+
    	                "JOIN "+this.schema+"views_in_model m ON t1.id = m.view_id AND t1.version = m.view_version "+
    	        		"GROUP BY t1.id, t1.version, t1.checksum, t1.created_on, m.model_id, m.model_version "+
    	                "HAVING  m.model_id = ? AND m.model_version = ? "+
    	        		") AS t "+
    	        		"JOIN views AS tt ON tt.id = t.id AND tt.version = t.latest_version",
    	                model.getId(),
    	                model.getDatabaseVersion().getVersion());
    	        while ( result.next() ) {
    	            IDBMetadata view = (IDBMetadata)model.getAllViews().get(result.getString("id"));
    	            if ( view != null ) {
    	            	view.getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
    	            	view.getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
    	            	view.getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
    	            	view.getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("latest_version"));
    	            	view.getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("latest_checksum"));
    	            	view.getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("latest_created_on"));
    	            	
    	            	view.getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("latest_version"));
    	            } else
    	            	this.viewsNotInModel.put(result.getString("id"), new DBVersion(result.getInt("version"), result.getString("checksum"),result.getTimestamp("created_on"), result.getInt("latest_version"), result.getString("latest_checksum"),result.getTimestamp("latest_created_on")));
    	        }
    	        result.close();
            }
        } else {
        	result.close();
            logger.debug("The model does not (yet) exist in the database");
        
            // we get the latest version of all the model's components
            while (ite.hasNext()) {
            	IArchimateElement element = ite.next().getValue();
            	result = select("SELECT version, checksum, created_on FROM "+this.schema+"elements WHERE id = ? AND version = (SELECT MAX(version) FROM elements WHERE id = ?)", element.getId(), element.getId());
            	if ( result.next() ) {
	            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
	            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
	            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("version"));
	            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("checksum"));
	            	((IDBMetadata)element).getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("created_on"));
	            	
	            	((IDBMetadata)element).getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("version"));
            	}
            	 else
                     ((IDBMetadata)element).getDBMetadata().getCurrentVersion().setLatestVersion(0);
            	result.close();
            }
            
            while (itr.hasNext()) {
            	IArchimateRelationship relationship = itr.next().getValue();
            	result = select("SELECT version, checksum, created_on FROM "+this.schema+"relationships WHERE id = ? AND version = (SELECT MAX(version) FROM relationships WHERE id = ?)",relationship.getId(), relationship.getId());
            	if ( result.next() ) {
	            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
	            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
	            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("version"));
	            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("checksum"));
	            	((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("created_on"));
	            	
	            	((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("version"));
            	} else
                    ((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setLatestVersion(0);
            	result.close();
        	}
            
            while (itf.hasNext()) {
            	IFolder folder = itf.next().getValue();
            	if ( model.getDatabaseVersion().getVersion() == 0 ) {
            		// if the model is not in the database, then we get the values from the latest version in the database
                	result = select("SELECT version, checksum, created_on FROM "+this.schema+"folders WHERE id = ? AND version = (SELECT MAX(version) FROM folders WHERE id = ?)", folder.getId(), folder.getId());
                	if ( result.next() ) {
    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("version"));
    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("checksum"));
    	            	((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("created_on"));
    	            	
    	            	((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("version"));
                	} else
                	    ((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setLatestVersion(0);
                	result.close();
            	}
            	
                while (itv.hasNext()) {
                	IDiagramModel view = itv.next().getValue();
                	result = select("SELECT version, checksum, created_on FROM "+this.schema+"views WHERE id = ? AND version = (SELECT MAX(version) FROM views WHERE id = ?)", view.getId(), view.getId());
                	if ( result.next() ) {
    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setVersion(result.getInt("version"));
    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setChecksum(result.getString("checksum"));
    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setLatestVersion(result.getInt("version"));
    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setLatestChecksum(result.getString("checksum"));
    	            	((IDBMetadata)view).getDBMetadata().getDatabaseVersion().setLatestTimestamp(result.getTimestamp("created_on"));
    	            	
    	            	((IDBMetadata)view).getDBMetadata().getCurrentVersion().setLatestVersion(result.getInt("version"));
                	} else
                        ((IDBMetadata)view).getDBMetadata().getCurrentVersion().setLatestVersion(0);
                	result.close();
                }
            }
        }
        //TODO: images
    }
    
	/* **** Export methods  ************************************************************** */

	/**
	 * Exports the model metadata into the database
	 */
	public void exportModel(ArchimateModel model, String releaseNote) throws Exception {
		final String[] modelsColumns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "checksum"};
		
		if ( (model.getName() == null) || (model.getName().equals("")) )
			throw new RuntimeException("Model name cannot be empty.");
		
        if ( this.connection.getAutoCommit() )
            model.getCurrentVersion().setLatestTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
        else
            model.getCurrentVersion().setLatestTimestamp(this.lastTransactionTimestamp);
        
        model.getCurrentVersion().setLatestVersion(model.getDatabaseVersion().getLatestVersion()+1);
        logger.debug("Set model's version to "+model.getCurrentVersion().getLatestVersion());

		insert(this.schema+"models", modelsColumns
				,model.getId()
				,model.getCurrentVersion().getLatestVersion()
				,model.getName()
				,releaseNote
				,model.getPurpose()
				,System.getProperty("user.name")
				,model.getCurrentVersion().getLatestTimestamp()
				,model.getCurrentVersion().getLatestChecksum()
				);

		exportProperties(model);
	}

	/**
	 * Export a component to the database
	 */
	public void exportEObject(EObject eObject) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("version "+((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().getLatestVersion()+" of "+((IDBMetadata)eObject).getDBMetadata().getDebugName()+" is exported");

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
		else if ( !(eObject instanceof IDiagramModelObject) && !(eObject instanceof IDiagramModelConnection) )		// IDiagramModelObject and IDiagramModelConnection are assigned to views not to models
			throw new Exception("Do not know how to assign to the model : "+eObject.getClass().getSimpleName());
	}

	/**
	 * Export an element to the database
	 */
	private void exportElement(IArchimateConcept element) throws Exception {
		final String[] elementsColumns = {"id", "version", "class", "name", "type", "documentation", "created_by", "created_on", "checksum"};
		
		((IDBMetadata)element).getDBMetadata().getCurrentVersion().setLatestVersion(((IDBMetadata)element).getDBMetadata().getCurrentVersion().getLatestVersion()+1);

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "neo4j") ) {
			// USE MERGE instead to replace existing nodes
			request("CREATE (new:elements {id:?, version:?, class:?, name:?, type:?, documentation:?, checksum:?})"
					,element.getId()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getLatestVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
					,element.getDocumentation()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getLatestChecksum()
					);
		} else {
			insert(this.schema+"elements", elementsColumns
					,element.getId()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getLatestVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
					,element.getDocumentation()
					,System.getProperty("user.name")
					,((ArchimateModel)element.getArchimateModel()).getCurrentVersion().getLatestTimestamp()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getLatestChecksum()
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

		insert(this.schema+"elements_in_model", elementsInModelColumns
				,element.getId()
				,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getLatestVersion()   // we use currentVersion as it has been set in exportElement()
				,((IFolder)element.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getLatestVersion()
				,++this.elementRank
				);
	}

	/**
	 * Export a relationship to the database
	 */
	private void exportRelationship(IArchimateConcept relationship) throws Exception {
		final String[] relationshipsColumns = {"id", "version", "class", "name", "documentation", "source_id", "target_id", "strength", "access_type", "created_by", "created_on", "checksum"};
		
		((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setLatestVersion(((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestVersion()+1);

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), "neo4j") ) {
			// USE MERGE instead to replace existing nodes
			if ( this.databaseEntry.getNeo4jNativeMode() ) {
				if ( (((IArchimateRelationship)relationship).getSource() instanceof IArchimateElement) && (((IArchimateRelationship)relationship).getTarget() instanceof IArchimateElement) ) {
					request("MATCH (source:elements {id:?, version:?}), (target:elements {id:?, version:?}) CREATE (source)-[relationship:relationships {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}]->(target)"
							,((IArchimateRelationship)relationship).getSource().getId()
							,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion().getLatestVersion()
							,((IArchimateRelationship)relationship).getTarget().getId()
							,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion().getLatestVersion()
							,relationship.getId()
							,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestVersion()
							,relationship.getClass().getSimpleName()
							,relationship.getName()
							,relationship.getDocumentation()
							,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
							,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
							,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestChecksum()
							);
				}
			} else {
				request("MATCH (source {id:?, version:?}), (target {id:?, version:?}) CREATE (relationship:relationships {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}), (source)-[rel1:relatedTo]->(relationship)-[rel2:relatedTo]->(target)"
						,((IArchimateRelationship)relationship).getSource().getId()
						,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion().getLatestVersion()
						,((IArchimateRelationship)relationship).getTarget().getId()
						,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion().getLatestVersion()
						,relationship.getId()
						,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestVersion()
						,relationship.getClass().getSimpleName()
						,relationship.getName()
						,relationship.getDocumentation()
						,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
						,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
						,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestChecksum()
						);
			}
		} else {
			insert(this.schema+"relationships", relationshipsColumns
					,relationship.getId()
					,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestVersion()
					,relationship.getClass().getSimpleName()
					,relationship.getName()
					,relationship.getDocumentation()
					,((IArchimateRelationship)relationship).getSource().getId()
					,((IArchimateRelationship)relationship).getTarget().getId()
					,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
					,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
					,System.getProperty("user.name")
					,((ArchimateModel)relationship.getArchimateModel()).getCurrentVersion().getLatestTimestamp()
					,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestChecksum()
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

		insert(this.schema+"relationships_in_model", relationshipsInModelColumns
				,relationship.getId()
				,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getLatestVersion()
				,((IFolder)relationship.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getLatestVersion()
				,++this.relationshipRank
				);
	}

	/**
	 * Export a folder into the database.
	 */
	private void exportFolder(IFolder folder) throws Exception {
		final String[] foldersColumns = {"id", "version", "type", "root_type", "name", "documentation", "created_by", "created_on", "checksum"};
		
		((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setLatestVersion(((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getLatestVersion()+1);

		insert(this.schema+"folders", foldersColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getLatestVersion()
				,folder.getType().getValue()
				,((IDBMetadata)folder).getDBMetadata().getRootFolderType()
				,folder.getName()
				,folder.getDocumentation()
				,System.getProperty("user.name")
				,((ArchimateModel)folder.getArchimateModel()).getCurrentVersion().getLatestTimestamp()
				,((Folder)folder).getDBMetadata().getCurrentVersion().getLatestChecksum()
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

		insert(this.schema+"folders_in_model", foldersInModelColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getLatestVersion()
				,(((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId())
				,model.getId()
				,model.getCurrentVersion().getLatestVersion()
				,++this.folderRank
				);
	}

	/**
	 * Export a view into the database.
	 */
	private void exportView(IDiagramModel view) throws Exception {
		final String[] ViewsColumns = {"id", "version", "class", "created_by", "created_on", "name", "connection_router_type", "documentation", "hint_content", "hint_title", "viewpoint", "background", "screenshot", "checksum"};
		
		((IDBMetadata)view).getDBMetadata().getCurrentVersion().setLatestVersion(((IDBMetadata)view).getDBMetadata().getCurrentVersion().getLatestVersion()+1);

		byte[] viewImage = null;

		if ( this.databaseEntry.getExportViewsImages() ) {
			viewImage = DBGui.createImage(view, 1, 10);
		}

		insert(this.schema+"views", ViewsColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getLatestVersion()
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
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getLatestChecksum()
				);

		if ( this.databaseEntry.getExportViewsImages() ) {
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

		insert(this.schema+"views_in_model", viewsInModelColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getLatestVersion()
				,((IFolder)view.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getLatestVersion()
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
		final String[] ViewsObjectsColumns = {"id", "version", "container_id", "view_id", "view_version", "class", "element_id", "diagram_ref_id", "type", "border_color", "border_type", "content", "documentation", "hint_content", "hint_title", "is_locked", "image_path", "image_position", "line_color", "line_width", "fill_color", "font", "font_color", "name", "notes", "source_connections", "target_connections", "text_alignment", "text_position", "x", "y", "width", "height", "rank", "checksum"};

		EObject viewContainer = viewObject.eContainer();
		while ( !(viewContainer instanceof IDiagramModel) ) {
			viewContainer = viewContainer.eContainer();
		}

		insert(this.schema+"views_objects", ViewsObjectsColumns
				,((IIdentifier)viewObject).getId()
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getLatestVersion()
				,((IIdentifier)viewObject.eContainer()).getId()
				,((IIdentifier)viewContainer).getId()
				,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getLatestVersion()
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
				,++this.viewObjectRank
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getLatestChecksum()
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

		insert(this.schema+"views_connections", ViewsConnectionsColumns
				,((IIdentifier)viewConnection).getId()
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getLatestVersion()
				,((IIdentifier)viewConnection.eContainer()).getId()
				,((IIdentifier)viewContainer).getId()
				,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getLatestVersion()
				,viewConnection.getClass().getSimpleName()
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
				,++this.viewConnectionRank
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getLatestChecksum()
				);

		exportProperties(viewConnection);

		for ( int pos = 0 ; pos < viewConnection.getBendpoints().size(); ++pos) {
			IDiagramModelBendpoint bendpoint = viewConnection.getBendpoints().get(pos);
			insert(this.schema+"bendpoints", bendpointsColumns
					,((IIdentifier)viewConnection).getId()
					,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getLatestVersion()
					,pos
					,bendpoint.getStartX()
					,bendpoint.getStartY()
					,bendpoint.getEndX()
					,bendpoint.getEndY()
					);
		}
	}

	/**
	 * Export properties to the database
	 */
	private void exportProperties(IProperties parent) throws Exception {
		final String[] propertiesColumns = {"parent_id", "parent_version", "rank", "name", "value"};

		int exportedVersion;
		if ( parent instanceof ArchimateModel ) {
			exportedVersion = ((ArchimateModel)parent).getCurrentVersion().getLatestVersion();
		} else 
			exportedVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getLatestVersion();

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

	public void exportImage(String path, byte[] image) throws SQLException, NoSuchAlgorithmException {
		String checksum = DBChecksum.calculateChecksum(image);
		ResultSet result = null;

		try {
			result = select("SELECT checksum FROM "+this.schema+"images WHERE path = ?", path);

			if ( result.next() ) {
				// if the image exists in the database, we update it if the checkum differs
				if ( !DBPlugin.areEqual(checksum, result.getString("checksum")) ) {
					request("UPDATE "+this.schema+"images SET image = ?, checksum = ? WHERE path = ?"
							,image
							,checksum
							,path
							);
				}
			} else {
				// if the image is not yet in the db, we insert it
				String[] databaseColumns = {"path", "image", "checksum"};
				insert(this.schema+"images", databaseColumns
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
}