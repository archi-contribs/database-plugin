/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBChecksum;

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
	protected DBDatabaseConnection(DBDatabaseEntry databaseEntry) throws ClassNotFoundException, SQLException {
		assert(databaseEntry != null);
		this.databaseEntry = databaseEntry;
		this.schema = databaseEntry.getSchemaPrefix();
		openConnection();
	}
	
	/**
	 * Used to switch between ImportConnection and ExpoetConnection.
	 */
	protected DBDatabaseConnection() {
	    
	}

	private void openConnection() throws ClassNotFoundException, SQLException {
		if ( isConnected() )
			close();
		
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
			// if the JDBC driver fails to connect to the database using the specified driver, then it tries with all the other drivers
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
	}

	/**
	 * Checks the database structure<br>
	 * @throws SQLExcetion if the connection to the database failed
	 * @returns true if the database structure is correct, false if not
	 */
	public void checkDatabase() throws ClassNotFoundException, SQLException {
		DBGui.popup("Please wait while checking the "+this.databaseEntry.getDriver()+" database ...");

		try {
			if ( !isConnected() )
				openConnection();
			
			switch ( this.databaseEntry.getDriver() ) {
				case "neo4j" :
					DBGui.closePopup();		// no tables to check on neo4j databases
					return;
				case "sqlite" :
					this.AUTO_INCREMENT	= "integer PRIMARY KEY";
					this.BOOLEAN		= "tinyint(1)";          				// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "timestamp";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "blob";
					this.INTEGER		= "integer(10)";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY	= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "clob";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "mysql"  :
					this.AUTO_INCREMENT	= "int(10) NOT NULL AUTO_INCREMENT";
					this.BOOLEAN		= "tinyint(1)";							// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "datetime";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "longblob";
					this.INTEGER		= "int(10)";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY	= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "mediumtext";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "ms-sql"  :
					this.AUTO_INCREMENT	= "int IDENTITY NOT NULL" ;
					this.BOOLEAN		= "tinyint";          					// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "datetime";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "image";
					this.INTEGER		= "int";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY	= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "nvarchar(max)";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "oracle" :
					this.AUTO_INCREMENT	= "integer NOT NULL";
					this.BOOLEAN		= "char";          						// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "date";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "blob";
					this.INTEGER		= "integer";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY	= "PRIMARY KEY";
					this.STRENGTH		= "varchar(20)";
					this.TEXT			= "clob";
					this.TYPE			= "varchar(3)";
					this.USERNAME		= "varchar(30)";
					break;
				case "postgresql" :
					this.AUTO_INCREMENT	= "serial NOT NULL" ;
					this.BOOLEAN		= "smallint";          					// we do not use boolean SQL type as it is not supported by all databases, so we export and import integer instead (0 = false, 1 = true);
					this.COLOR			= "varchar(7)";
					this.DATETIME		= "timestamp";
					this.FONT			= "varchar(150)";
					this.IMAGE			= "bytea";
					this.INTEGER		= "integer";
					this.OBJECTID		= "varchar(50)";
					this.OBJ_NAME		= "varchar(1024)";
					this.PRIMARY_KEY	= "PRIMARY KEY";
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

			int currentVersion = 0;
			try ( ResultSet result = select("SELECT version FROM "+this.schema+"database_version WHERE archi_plugin = ?", DBPlugin.pluginName);){
				result.next();currentVersion = result.getInt("version");
			} catch (@SuppressWarnings("unused") SQLException err) {
				// if the table does not exist
				DBGui.closePopup();
				if ( !DBGui.question("We successfully connected to the database but the necessary tables have not be found.\n\nDo you wish to create them ?") )
					throw new SQLException("Necessary tables not found.");

				createTables();
				return;
			}
			
			if ( (currentVersion < 200) || (currentVersion > databaseVersion) )
				throw new SQLException("The database has got an unknown model version (is "+currentVersion+" but should be between 200 and "+databaseVersion+")");

			if ( currentVersion != databaseVersion ) {
				if ( DBGui.question("The database needs to be upgraded. You will not loose any data during this operation.\n\nDo you wish to upgrade your database ?") )
					upgradeDatabase(currentVersion);
				else
					throw new SQLException("The database needs to be upgraded.");
			}
		} finally {
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

		if ( !isConnected() )
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
			
			try ( ResultSet result = select("SELECT id, version, name, note, purpose, created_by, created_on, checkedin_by, checkedin_on, deleted_by, deleted_on FROM models_old"); ) {
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
			}
			
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
		assert ( isConnected() );

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
		assert ( isConnected() );

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
		assert ( isConnected() );
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
     * Gets the list of models in the current database
     * @param filter (use "%" as wildcard) 
     * @throws Exception
     * @return a list of Hashtables, each containing the name and the id of one model
     */
    public ArrayList<Hashtable<String, Object>> getModels(String filter) throws Exception {
        ArrayList<Hashtable<String, Object>> list = new ArrayList<Hashtable<String, Object>>();
        
        @SuppressWarnings("resource")
        ResultSet result = null;
        
        try {
            // We do not use a GROUP BY because it does not give the expected result on PostGresSQL ...   
            if ( filter==null || filter.length()==0 )
                result = select("SELECT id, name, version FROM "+this.schema+"models m WHERE version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = m.id) ORDER BY name");
            else
                result = select("SELECT id, name, version FROM "+this.schema+"models m WHERE version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = m.id) AND UPPER(name) like UPPER(?) ORDER BY name", filter);
    
            while ( result.next() && result.getString("id") != null ) {
                if (logger.isTraceEnabled() ) logger.trace("Found model \""+result.getString("name")+"\"");
                Hashtable<String, Object> table = new Hashtable<String, Object>();
                table.put("name", result.getString("name"));
                table.put("id", result.getString("id"));
                list.add(table);
            }
        } finally {
            if ( result != null )
                result.close();
        }
        
        return list;
    }
    
    public String getModelId(String modelName, boolean ignoreCase) throws Exception {
        @SuppressWarnings("resource")
        ResultSet result = null;
        String id = null;
        
        try {
            // Using a GROUP BY on PostGresSQL does not give the expected result ...   
            if ( ignoreCase )
                result = select("SELECT id FROM "+this.schema+"models m WHERE UPPER(name) = UPPER(?)", modelName);
            else
                result = select("SELECT id FROM "+this.schema+"models m WHERE name = ?", modelName);
    
            if ( result.next() ) 
                id = result.getString("id");
        } finally {
            if ( result != null )
                result.close();
        }
        
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
        
        try ( ResultSet result = select("SELECT version, created_by, created_on, name, note, purpose FROM "+this.schema+"models WHERE id = ? ORDER BY version DESC", id) ) {
            while ( result.next() ) {
                if (logger.isTraceEnabled() ) logger.trace("Found model \""+result.getString("name")+"\" version \""+result.getString("version")+"\"");
                Hashtable<String, Object> table = new Hashtable<String, Object>();
                table.put("version", result.getString("version"));
                table.put("created_by", result.getString("created_by"));
                table.put("created_on", result.getTimestamp("created_on"));
                table.put("name", result.getString("name"));
                table.put("note", result.getString("note"));
                table.put("purpose", result.getString("purpose"));
                list.add(table);
            }
        }       
        return list;
    }


	/**
	 * Sets the auto-commit mode of the database
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Setting database auto commit to "+String.valueOf(autoCommit));
		this.connection.setAutoCommit(autoCommit);
	}

	/**
	 * Commits the current transaction
	 */
	public void commit() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Committing database transaction.");
		this.connection.commit();
	}

	/**
	 * Rollbacks the current transaction
	 */
	public void rollback() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Rollbacking database transaction.");
		this.connection.rollback();
	}
}