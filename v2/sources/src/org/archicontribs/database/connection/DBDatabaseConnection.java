/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import org.apache.log4j.Level;
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.commands.DBImportViewFromIdCommand;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;

import lombok.Getter;

/**
 * This class holds the information required to connect to, to import from and export to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseConnection implements AutoCloseable {
    private static final DBLogger logger = new DBLogger(DBDatabaseConnection.class);

    /**
     * Version of the expected database model.<br>
     * If the value found into the columns version of the table "database_version", then the plugin will try to upgrade the datamodel.
     */
    public static final int databaseVersion = 211;

    /**
     * the databaseEntry corresponding to the connection
     */
    @Getter protected DBDatabaseEntry databaseEntry = null;
    @Getter protected String schema = "";

    /**
     * Connection to the database
     */
    @Getter protected Connection connection = null;

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
     * Used to switch between ImportConnection and ExportConnection.
     */
    protected DBDatabaseConnection() {

    }

    private void openConnection() throws ClassNotFoundException, SQLException {
        if ( isConnected() )
            close();

        if ( logger.isDebugEnabled() ) logger.debug("Opening connection to database "+this.databaseEntry.getName()+": driver="+this.databaseEntry.getDriver()+", server="+this.databaseEntry.getServer()+", port="+this.databaseEntry.getPort()+", database="+this.databaseEntry.getDatabase()+", schema="+this.databaseEntry.getSchema()+", username="+this.databaseEntry.getUsername());

        String clazz = this.databaseEntry.getDriverClass();
        if ( logger.isDebugEnabled() ) logger.debug("JDBC class = " + clazz);
        
        String connectionString = this.databaseEntry.getJdbcConnectionString();
        if ( logger.isDebugEnabled() ) logger.debug("JDBC connection string = " + connectionString);

        try {
            // we load the jdbc class
            Class.forName(clazz);
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MSSQL.getDriverName()) && DBPlugin.isEmpty(this.databaseEntry.getUsername()) && DBPlugin.isEmpty(this.databaseEntry.getPassword()) ) {
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
                if ( this.databaseEntry.getDriver().equals(DBDatabase.MSSQL.getDriverName()) )	// if SQL Server, we update the message for integrated authentication
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
    @Override
    public void close() throws SQLException {
        if ( this.connection == null || this.connection.isClosed() ) {
            if ( logger.isDebugEnabled() ) logger.debug("The database connection is already closed.");
        } else {
            // if some transactions have not been committed before calling this close method, then they must be rolled back
            try {
                rollback();
            } catch (SQLException err) {
                logger.error("Failed to rollback the database transactions.", err);
            }
            if ( logger.isDebugEnabled() ) logger.debug("Closing database connection.");
            this.connection.close();
        }
        this.connection = null;
        this.databaseEntry = null;
    }

    public boolean isConnected() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch ( @SuppressWarnings("unused") SQLException ign ) {
            // nothing to do
        }
        return false;
    }
    
    /**
     * You may prefer {@link isConnected}
     */
    public boolean isClosed() throws SQLException {
        return this.connection.isClosed();
    }

    /**
     * Checks the database structure<br>
     * @throws SQLExcetion if the connection to the database failed
     * @returns true if the database structure is correct, false if not
     */
    public void checkDatabase(DBGui dbGui) throws Exception {
    	try {
			if ( dbGui != null )
				dbGui.setMessage("Checking the database structure...");
			else
				DBGui.popup("Checking the database structure...");
	
	        if ( !isConnected() )
	            openConnection();
	
	        if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
	        	// do not need to create tables
	            return;
	        }

	        if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.SQLITE.getDriverName()) ) {
	                this.AUTO_INCREMENT	= "integer NOT NULL";
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
	        } else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MYSQL.getDriverName()) ) {
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
	        } else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MSSQL.getDriverName()) ) {
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
	        } else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
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
	        } else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.POSTGRESQL.getDriverName()) ) {
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
	        } else {
	        	// should never be here
	            throw new SQLException("Unknown driver "+this.databaseEntry.getDriver());
	
	        }
	
	        // checking if the database_version table exists
	        if ( logger.isTraceEnabled() ) logger.trace("Checking \""+this.schema+"database_version\" table");
	
	        int currentVersion = 0;
	        try ( DBSelect resultVersion = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT version FROM "+this.schema+"database_version WHERE archi_plugin = ?", DBPlugin.pluginName) ) {
	            resultVersion.next();
	            currentVersion = resultVersion.getInt("version");
	        } catch (@SuppressWarnings("unused") SQLException err) {
	            // if the table does not exist
	            if ( !DBGui.question("We successfully connected to the database but the necessary tables have not be found.\n\nDo you wish to create them ?") )
	                throw new SQLException("Necessary tables not found.");
	
	            createTables(dbGui);
	            return;
	        }
	
	        if ( (currentVersion < 200) || (currentVersion > databaseVersion) )
	            throw new SQLException("The database has got an unknown model version (is "+currentVersion+" but should be between 200 and "+databaseVersion+")");
	
	        if ( currentVersion != databaseVersion ) {
	            if ( DBGui.question("The database needs to be upgraded. You will not loose any data during this operation.\n\nDo you wish to upgrade your database ?") ) {
	                upgradeDatabase(currentVersion);
	                DBGui.popup(Level.INFO, "Database successfully upgraded.");
	            }
	            else
	                throw new SQLException("The database needs to be upgraded.");
	        }
    	} catch (SQLException err) {
    		rollback();
    		throw err;
	    } finally {
			if ( dbGui != null )
				dbGui.closeMessage();
			else
				DBGui.closePopup();
	    }
    }

    /**
     * Creates the necessary tables in the database
     * @throws ClassNotFoundException 
     */
    private void createTables(DBGui dbGui) throws SQLException, ClassNotFoundException {
        final String[] databaseVersionColumns = {"archi_plugin", "version"};
        
        try {
            
            if ( dbGui != null )
            	dbGui.setMessage("Creating necessary database tables ...");
            else
            	DBGui.popup("Creating necessary database tables ...");
            
            if ( !isConnected() )
                openConnection();

            setAutoCommit(false);
            
            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"database_version");
            executeRequest("CREATE TABLE " + this.schema +"database_version ("
                    + "archi_plugin " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL"
                    + ")");

            insert(this.schema+"database_version", databaseVersionColumns, DBPlugin.pluginName, databaseVersion);

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"bendpoints");
            executeRequest("CREATE TABLE " + this.schema +"bendpoints ("
                    + "parent_id " + this.OBJECTID +" NOT NULL, "
                    + "parent_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL, "
                    + "start_x " + this.INTEGER +" NOT NULL, "
                    + "start_y " + this.INTEGER +" NOT NULL, "
                    + "end_x " + this.INTEGER +" NOT NULL, "
                    + "end_y " + this.INTEGER +" NOT NULL, "
                    + this.PRIMARY_KEY+" (parent_id, parent_version, rank)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"elements");
            executeRequest("CREATE TABLE " + this.schema +"elements ("
                    + "id " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "class " + this.OBJECTID +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "
                    + "documentation " + this.TEXT + ", "
                    + "type " + this.TYPE + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL,"
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"elements_in_model");
            executeRequest("CREATE TABLE "+this.schema+"elements_in_model ("
                    + "eim_id " + this.AUTO_INCREMENT + ", "
                    + "element_id " + this.OBJECTID +" NOT NULL, "
                    + "element_version " + this.INTEGER +" NOT NULL, "
                    + "parent_folder_id " + this.OBJECTID +" NOT NULL, "
                    + "model_id " + this.OBJECTID +" NOT NULL, "
                    + "model_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (eim_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_elements");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_elements_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_elements_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_elements_in_model");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_elements_in_model "
                        + "BEFORE INSERT ON "+this.schema+"elements_in_model "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_elements_in_model.NEXTVAL INTO :NEW.eim_id FROM DUAL;"
                        + "END;");
            }

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"folders");
            executeRequest("CREATE TABLE "+this.schema+"folders ("
                    + "id " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "type " + this.INTEGER +" NOT NULL, "
                    + "root_type " + this.INTEGER +" NOT NULL, "
                    + "name " + this.OBJ_NAME + " NOT NULL, "
                    + "documentation " + this.TEXT + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL, "
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"folders_in_model");
            executeRequest("CREATE TABLE "+this.schema+"folders_in_model ("
                    + "fim_id " + this.AUTO_INCREMENT+", "
                    + "folder_id " + this.OBJECTID +" NOT NULL, "
                    + "folder_version " + this.INTEGER +" NOT NULL, "
                    + "parent_folder_id " + this.OBJECTID + ", "
                    + "model_id " + this.OBJECTID +" NOT NULL, "
                    + "model_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (fim_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_folders_in_model");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_folders_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_folders_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_folders_in_model");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_folders_in_model "
                        + "BEFORE INSERT ON "+this.schema+"folders_in_model "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_folders_in_model.NEXTVAL INTO :NEW.fim_id FROM DUAL;"
                        + "END;");
            }

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"images");
            executeRequest("CREATE TABLE "+this.schema+"images ("
                    + "path " + this.OBJECTID +" NOT NULL, "
                    + "image " + this.IMAGE +" NOT NULL, "
                    + this.PRIMARY_KEY+" (path)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"models");
            executeRequest("CREATE TABLE "+this.schema+"models ("
                    + "id " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "name " + this.OBJ_NAME +" NOT NULL, "
                    + "note " + this.TEXT + ", "
                    + "purpose " + this.TEXT + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL, "
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"properties");
            executeRequest("CREATE TABLE "+this.schema+"properties ("
                    + "parent_id "+this.OBJECTID +" NOT NULL, "
                    + "parent_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "
                    + "value " + this.TEXT + ", "
                    + this.PRIMARY_KEY+" (parent_id, parent_version, rank)"
                    + ")");
            
            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"metadata");
            executeRequest("CREATE TABLE "+this.schema+"metadata ("
                    + "parent_id "+this.OBJECTID +" NOT NULL, "
                    + "parent_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "
                    + "value " + this.TEXT + ", "
                    + this.PRIMARY_KEY+" (parent_id, parent_version, rank)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"relationships");
            executeRequest("CREATE TABLE "+this.schema+"relationships ("
                    + "id "+this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "class " + this.OBJECTID +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "
                    + "documentation " + this.TEXT + ", "
                    + "source_id " + this.OBJECTID + ", "
                    + "target_id " + this.OBJECTID + ", "
                    + "strength " + this.STRENGTH + ", "
                    + "access_type " + this.INTEGER + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL, "
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"relationships_in_model");
            executeRequest("CREATE TABLE "+this.schema+"relationships_in_model ("
                    + "rim_id " + this.AUTO_INCREMENT+", "
                    + "relationship_id " + this.OBJECTID +" NOT NULL, "
                    + "relationship_version " + this.INTEGER +" NOT NULL, "
                    + "parent_folder_id " + this.OBJECTID +" NOT NULL, "
                    + "model_id " + this.OBJECTID +" NOT NULL, "
                    + "model_version " + this.INTEGER +" NOT NULL, "
                    + "rank "+this.INTEGER +" NOT NULL "
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (rim_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_relationships");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_relationships_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_relationships_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_relationships_in_model");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_relationships_in_model "
                        + "BEFORE INSERT ON "+this.schema+"relationships_in_model "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_relationships_in_model.NEXTVAL INTO :NEW.rim_id FROM DUAL;"
                        + "END;");
            }

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views");
            executeRequest("CREATE TABLE "+this.schema+"views ("
                    + "id " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "class " + this.OBJECTID +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "
                    + "documentation " + this.TEXT +" , "
                    + "background " + this.INTEGER + ", "
                    + "connection_router_type " + this.INTEGER +" NOT NULL, "
                    + "viewpoint " + this.OBJECTID + ", "
                    + "screenshot " + this.IMAGE + ", "
                    + "screenshot_scale_factor " + this.INTEGER + ", "
                    + "screenshot_border_width " + this.INTEGER + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL, "
                    + "container_checksum " + this.OBJECTID +" NOT NULL, "
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_connections");
            executeRequest("CREATE TABLE "+this.schema+"views_connections ("
                    + "id " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "class " + this.OBJECTID +" NOT NULL, "
                    + "container_id " + this.OBJECTID +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "					// connection must store a name because all of them are not linked to a relationship
                    + "documentation " + this.TEXT + ", "
                    + "is_locked " + this.BOOLEAN + ", "
                    + "line_color " + this.COLOR + ", "
                    + "line_width " + this.INTEGER + ", "
                    + "font " + this.FONT + ", "
                    + "font_color " + this.COLOR + ", "
                    + "relationship_id " + this.OBJECTID + ", "
                    + "relationship_version " + this.INTEGER + ", "
                    + "source_object_id " + this.OBJECTID + ", "
                    + "target_object_id " + this.OBJECTID + ", "
                    + "text_position " + this.INTEGER + ", "
                    + "type " + this.INTEGER + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL, "
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_connections_in_view");
            executeRequest("CREATE TABLE "+this.schema+"views_connections_in_view ("
                    + "civ_id " + this.AUTO_INCREMENT+", "
                    + "connection_id " + this.OBJECTID +" NOT NULL, "
                    + "connection_version " + this.INTEGER +" NOT NULL, "
                    + "view_id " + this.OBJECTID +" NOT NULL, "
                    + "view_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (civ_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_connections_in_view");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_connections_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_connections_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_connections_in_view");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_connections_in_view "
                        + "BEFORE INSERT ON "+this.schema+"views_connections_in_view "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_connections_in_view.NEXTVAL INTO :NEW.civ_id FROM DUAL;"
                        + "END;");
            }

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_in_model");
            executeRequest("CREATE TABLE "+this.schema+"views_in_model ("
                    + "vim_id " + this.AUTO_INCREMENT + ", "
                    + "view_id " + this.OBJECTID +" NOT NULL, "
                    + "view_version " + this.INTEGER +" NOT NULL, "
                    + "parent_folder_id " + this.OBJECTID +" NOT NULL, "
                    + "model_id " + this.OBJECTID +" NOT NULL, "
                    + "model_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (vim_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_views");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_views_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_views_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_views_in_model");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_views_in_model "
                        + "BEFORE INSERT ON "+this.schema+"views_in_model "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_views_in_model.NEXTVAL INTO :NEW.vim_id FROM DUAL;"
                        + "END;");
            }

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_objects");
            executeRequest("CREATE TABLE "+this.schema+"views_objects ("
                    + "id " + this.OBJECTID +" NOT NULL, "
                    + "version " + this.INTEGER +" NOT NULL, "
                    + "class " + this.OBJECTID +" NOT NULL, "
                    + "container_id " + this.OBJECTID +" NOT NULL, "
                    + "element_id " + this.OBJECTID + ", "
                    + "element_version " + this.INTEGER + ", "
                    + "diagram_ref_id " + this.OBJECTID + ", "
                    + "border_color " + this.COLOR + ", "
                    + "border_type " + this.INTEGER + ", "
                    + "content " + this.TEXT + ", "
                    + "documentation " + this.TEXT + ", "
                    + "is_locked " + this.BOOLEAN + ", "
                    + "image_path " + this.OBJECTID + ", "
                    + "image_position " + this.INTEGER + ", "
                    + "line_color " + this.COLOR + ", "
                    + "line_width " + this.INTEGER + ", "
                    + "fill_color " + this.COLOR + ", "
                    + "alpha " + this.INTEGER + ", "
                    + "font " + this.FONT + ", "
                    + "font_color " + this.COLOR + ", "
                    + "name " + this.OBJ_NAME + ", "
                    + "notes " + this.TEXT + ", "
                    + "text_alignment " + this.INTEGER + ", "
                    + "text_position " + this.INTEGER + ", "
                    + "type " + this.INTEGER + ", "
                    + "x " + this.INTEGER + ", "
                    + "y " + this.INTEGER + ", "
                    + "width " + this.INTEGER + ", "
                    + "height " + this.INTEGER + ", "
                    + "created_by " + this.USERNAME +" NOT NULL, "
                    + "created_on " + this.DATETIME +" NOT NULL, "
                    + "checkedin_by " + this.USERNAME + ", "
                    + "checkedin_on " + this.DATETIME + ", "
                    + "deleted_by " + this.USERNAME + ", "
                    + "deleted_on " + this.DATETIME + ", "
                    + "checksum " + this.OBJECTID +" NOT NULL, "
                    + this.PRIMARY_KEY+" (id, version)"
                    + ")");

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_objects_in_view");
            executeRequest("CREATE TABLE "+this.schema+"views_objects_in_view ("
                    + "oiv_id " + this.AUTO_INCREMENT+", "
                    + "object_id " + this.OBJECTID +" NOT NULL, "
                    + "object_version " + this.INTEGER +" NOT NULL, "
                    + "view_id " + this.OBJECTID +" NOT NULL, "
                    + "view_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (oiv_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_objects_in_view");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_objects_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_objects_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_objects_in_view");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_objects_in_view "
                        + "BEFORE INSERT ON "+this.schema+"views_objects_in_view "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_objects_in_view.NEXTVAL INTO :NEW.oiv_id FROM DUAL;"
                        + "END;");
            }

            commit();
            setAutoCommit(true);

            DBGui.popup(Level.INFO,"The database has been successfully initialized.");

        } catch (SQLException err) {
            if ( dbGui != null )
            	dbGui.closeMessage();
            else
            	DBGui.closePopup();
            
            rollback();
            setAutoCommit(true);
            throw err;
        }

    }

	public void dropTableIfExists(String tableName) throws SQLException {
    	if ( logger.isDebugEnabled() ) logger.debug("Dropping table "+tableName+" if it exists");
        
        if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
			executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP TABLE "+tableName+"'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MSSQL.getDriverName()) )
			executeRequest("IF OBJECT_ID('"+tableName+"', 'U') IS NOT NULL DROP TABLE "+tableName);
        else
			executeRequest("DROP TABLE IF EXISTS "+tableName);
    }

    public void dropColumn(String tableName, String columnName) throws SQLException {
        if ( logger.isDebugEnabled() ) logger.debug("Altering table "+tableName+", dropping column "+columnName);

        // sqlite has got very limited alter table support. Especially, it does not allow to drop a column
        if ( !DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.SQLITE.getDriverName()) ) {
            StringBuilder requestString = new StringBuilder();
            requestString.append("ALTER TABLE ");
            requestString.append(tableName);
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MYSQL.getDriverName()) )
                requestString.append(" DROP ");
            else
                requestString.append(" DROP COLUMN ");
            requestString.append(columnName);

            executeRequest(requestString.toString());
        } else {
            StringBuilder createTableRequest = new StringBuilder();
            StringBuilder columnNames = new StringBuilder();
            StringBuilder primaryKeys = new StringBuilder();

            // just in case
            dropTableIfExists(tableName+"_old");

            String tableInfoRequest = "PRAGMA TABLE_INFO("+tableName+")";
            try (PreparedStatement pstmt = this.connection.prepareStatement(tableInfoRequest, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY) ) {
                if ( logger.isTraceSQLEnabled() ) logger.trace("      --> "+tableInfoRequest);
                try (ResultSet result = pstmt.executeQuery() ) {
                    createTableRequest.append("CREATE TABLE "+tableName+" (");
                    boolean columnsNeedComma = false;
                    boolean primaryKeysNeedComma = false;
                    while ( result.next() ) {
                        // if the column is not the column to drop, then we create it
                        if ( !DBPlugin.areEqual(columnName, result.getString("name")) ) {
                            if ( columnsNeedComma ) {
                                createTableRequest.append(", ");
                                columnNames.append(", ");
                            }
                            createTableRequest.append(result.getString("name"));
                            createTableRequest.append(" ");
                            createTableRequest.append(result.getString("type"));

                            columnNames.append(result.getString("name"));

                            columnsNeedComma = true;
                        }

                        if ( result.getInt("pk") != 0 ) {
                            if ( primaryKeysNeedComma ) {
                                primaryKeys.append(", ");
                            }
                            primaryKeys.append(result.getString("name"));
                            primaryKeysNeedComma = true;
                        }
                    }
                    if ( primaryKeys.length() != 0 ) {
                        createTableRequest.append(", "+this.PRIMARY_KEY+" (");
                        createTableRequest.append(primaryKeys.toString());
                        createTableRequest.append(")");
                    }

                    createTableRequest.append(")");
                }
            }

            executeRequest("ALTER TABLE "+tableName+" RENAME TO "+tableName+"_old");
            executeRequest(createTableRequest.toString());
            executeRequest("INSERT INTO "+tableName+" SELECT "+columnNames+" FROM "+tableName+"_old");

            dropTableIfExists(tableName+"_old");
        }
    }

    public void addColumn(String tableName, String columnName, String columnType) throws SQLException {
        addColumn(tableName, columnName, columnType, true, null);
    }
    
    public <T> void addColumn(String tableName, String columnName, String columnType, boolean canBeNull, T defaultValue) throws SQLException {
        if ( logger.isDebugEnabled() ) logger.debug("Altering table "+tableName+", adding column "+columnName+" type "+ columnType);

        StringBuilder requestString = new StringBuilder();
        requestString.append("ALTER TABLE ");
        requestString.append(tableName);
        if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MYSQL.getDriverName()) || DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MSSQL.getDriverName()) ) 
            requestString.append(" ADD ");
        else if (  DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
        	requestString.append(" ADD ( ");
        else
            requestString.append(" ADD COLUMN ");
        requestString.append(columnName);
        requestString.append(" ");
        requestString.append(columnType);
        
        if ( defaultValue != null ) {
            requestString.append(" DEFAULT ");
            if ( defaultValue instanceof Integer )
                requestString.append(defaultValue);
            else if ( defaultValue instanceof Timestamp ) {
            	if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
            		requestString.append("TO_DATE(");
            	requestString.append("'");
            	requestString.append(defaultValue.toString().substring(0, 19));		// we remove the milliseconds
            	requestString.append("'");
            	if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
            		requestString.append(",'YYYY-MM-DD HH24:MI.SS')");
            } else {
                requestString.append("'");
                requestString.append(defaultValue);
                requestString.append("'");
            }
        }
        
        if ( !canBeNull )
            requestString.append(" NOT NULL");
        
        if (  DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
        	requestString.append(" )");
        
        executeRequest(requestString.toString());
    }

    /**
     * Upgrades the database
     * @throws Exception 
     */
    private void upgradeDatabase(int version) throws Exception {
        int dbVersion = version;

        setAutoCommit(false);

        // convert from version 200 to 201:
        //      - add a blob column into the views table
        if ( dbVersion == 200 ) {
            addColumn(this.schema+"views", "screenshot", this.IMAGE);			           // executeRequest("ALTER TABLE "+this.schema+"views ADD "+COLUMN+" screenshot "+this.IMAGE);

            dbVersion = 201;
        }

        // convert from version 201 to 202:
        //      - add text_position column in the views_connections table
        //      - add source_connections and target_connections to views_objects and views_connections tables
        if ( dbVersion == 201 ) {
            addColumn(this.schema+"views", "text_position", this.INTEGER);                   //executeRequest("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" text_position "+this.INTEGER);
            addColumn(this.schema+"views_objects", "source_connections", this.TEXT);         // executeRequest("ALTER TABLE "+this.schema+"views_objects ADD "+COLUMN+" source_connections "+this.TEXT);
            addColumn(this.schema+"views_objects", "target_connections", this.TEXT);         // executeRequest("ALTER TABLE "+this.schema+"views_objects ADD "+COLUMN+" target_connections "+this.TEXT);
            addColumn(this.schema+"views_connections", "source_connections", this.TEXT);     // executeRequest("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" source_connections "+this.TEXT);
            addColumn(this.schema+"views_connections", "target_connections", this.TEXT);     // executeRequest("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" target_connections "+this.TEXT);

            dbVersion = 202;
        }

        // convert from version 202 to 203:
        //      - add element_version column to the views_objects table
        //      - add relationship_version column to the views_connections table
        if ( dbVersion == 202 ) {
            addColumn(this.schema+"views_connections", "relationship_version", this.INTEGER, false, 1);     // executeRequest("ALTER TABLE "+this.schema+"views_connections ADD "+COLUMN+" relationship_version "+this.INTEGER);
            addColumn(this.schema+"views_objects", "element_version", this.INTEGER, false, 1);             // executeRequest("ALTER TABLE "+this.schema+"views_objects ADD "+COLUMN+" element_version "+this.INTEGER);

            dbVersion = 203;
        }

        // convert from version 203 to 204:
        //      - add a checksum to the model
        //
        if ( dbVersion == 203 ) {
            addColumn(this.schema+"models", "checksum", this.OBJECTID, false, "");

            if ( logger.isDebugEnabled() ) logger.debug("Calculating models checksum");
            try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, name, note, purpose FROM "+this.schema+"models") ) {
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
                    executeRequest("UPDATE "+this.schema+"models SET checksum = ? WHERE id = ? AND version = ?", checksum, result.getString("id"), result.getInt("version"));
                }
            }

            dbVersion = 204;
        }

        // convert from version 204 to 205:
        //      - add a container_checksum column in the views table
        //
        if ( dbVersion == 204 ) {
            addColumn(this.schema+"views", "container_checksum", this.OBJECTID, false, "");
            
            DBGui.popup("Please wait while calculating new checksum on views table.");
            
            DBArchimateModel tempModel = new DBArchimateModel();
            try ( DBDatabaseImportConnection importConnection = (DBDatabaseImportConnection)this ) {
                if ( logger.isDebugEnabled() ) logger.debug("Calculating containers checksum");
                try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version FROM "+this.schema+"views") ) {
                    while ( result.next() ) {
                        IDiagramModel view;
                        DBImportViewFromIdCommand command = new DBImportViewFromIdCommand(importConnection, tempModel, null, result.getString("id"), result.getInt("version"), DBImportMode.templateMode, false);
                        if ( command.canExecute() )
                            command.execute();
                        if ( command.getException() != null )
                            throw command.getException();
                        
                        view = command.getImported();
                        
                        executeRequest("UPDATE "+this.schema+"views SET container_checksum = ? WHERE id = ? AND version = ?", DBChecksum.calculateChecksum(view), result.getString("id"), result.getInt("version"));
                    }
                }
            }
            tempModel = null;
            
            DBGui.closePopup();

            dbVersion = 205;
        }

        // convert from version 205 to 206:
        //      - add the created_by and created_on columns in the views_connections and views_objects tables
        //      - create tables views_connections_in_view and views_objects_in_view
        //      - remove the rank, view_id and view_version columns from the views_connections and views_objects tables
        //
        if ( dbVersion == 205 ) {
        	Timestamp now = new Timestamp(0);
        	
            addColumn(this.schema+"views_connections", "created_by", this.USERNAME, false, "databasePlugin");			// we set dummy value to satisfy the NOT NULL condition
            addColumn(this.schema+"views_connections", "created_on", this.DATETIME, false, now);						// we set dummy value to satisfy the NOT NULL condition
        	
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
            	executeRequest("UPDATE "+this.schema+"views_connections c SET (created_on, created_by) = (SELECT created_on, created_by FROM "+this.schema+"views WHERE id = c.view_id AND version = c.view_version)");
            else
            	executeRequest("UPDATE "+this.schema+"views_connections SET created_on = j.created_on, created_by = j.created_by FROM (SELECT c.id, v.created_on, v.created_by FROM "+this.schema+"views_connections c JOIN "+this.schema+"views v ON v.id = c.view_id AND v.version = c.view_version) j WHERE "+this.schema+"views_connections.id = j.id");
            
            
            
            
            
            
            addColumn(this.schema+"views_objects", "created_by", this.USERNAME, false, "databasePlugin");				// we set dummy value to satisfy the NOT NULL condition
            addColumn(this.schema+"views_objects", "created_on", this.DATETIME, false, now);							// we set dummy value to satisfy the NOT NULL condition
            
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
            	executeRequest("UPDATE "+this.schema+"views_objects o SET (created_on, created_by) = (SELECT created_on, created_by FROM "+this.schema+"views WHERE id = o.view_id AND version = o.view_version)");
            else
            	executeRequest("UPDATE "+this.schema+"views_objects SET created_on = j.created_on, created_by = j.created_by FROM (SELECT c.id, v.created_on, v.created_by FROM "+this.schema+"views_objects c JOIN "+this.schema+"views v ON v.id = c.view_id AND v.version = c.view_version) j WHERE "+this.schema+"views_objects.id = j.id");

            
            
            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_connections_in_view");
            executeRequest("CREATE TABLE "+this.schema+"views_connections_in_view ("
                    + "civ_id " + this.AUTO_INCREMENT+", "
                    + "connection_id " + this.OBJECTID +" NOT NULL, "
                    + "connection_version " + this.INTEGER +" NOT NULL, "
                    + "view_id " + this.OBJECTID +" NOT NULL, "
                    + "view_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (civ_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_connections_in_view");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_connections_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_connections_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_connections_in_view");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_connections_in_view "
                        + "BEFORE INSERT ON "+this.schema+"views_connections_in_view "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_connections_in_view.NEXTVAL INTO :NEW.civ_id FROM DUAL;"
                        + "END;");
            }

            // we fill in the views_connections_in_view table
            if ( logger.isDebugEnabled() ) logger.debug("Copying data from "+this.schema+"views_connections table to "+this.schema+"views_connections_in_view table");
            executeRequest("INSERT INTO "+this.schema+"views_connections_in_view "
                    +"(connection_id, connection_version, view_id, view_version, rank) "
                    +"SELECT id, version, view_id, view_version, rank FROM "+this.schema+"views_connections"
                    );

            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"views_objects_in_view");
            executeRequest("CREATE TABLE "+this.schema+"views_objects_in_view ("
                    + "oiv_id " + this.AUTO_INCREMENT+", "
                    + "object_id " + this.OBJECTID +" NOT NULL, "
                    + "object_version " + this.INTEGER +" NOT NULL, "
                    + "view_id " + this.OBJECTID +" NOT NULL, "
                    + "view_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL"
                    + (this.AUTO_INCREMENT.endsWith("PRIMARY KEY") ? "" : (", "+this.PRIMARY_KEY+" (oiv_id)") )
                    + ")");
            if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
                if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schema+"seq_objects_in_view");
                executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schema+"seq_objects_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
                executeRequest("CREATE SEQUENCE "+this.schema+"seq_objects_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

                if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schema+"trigger_objects_in_view");
                executeRequest("CREATE OR REPLACE TRIGGER "+this.schema+"trigger_objects_in_view "
                        + "BEFORE INSERT ON "+this.schema+"views_objects_in_view "
                        + "FOR EACH ROW "
                        + "BEGIN"
                        + "  SELECT "+this.schema+"seq_objects_in_view.NEXTVAL INTO :NEW.oiv_id FROM DUAL;"
                        + "END;");
            }

            // we fill in the views_objects_in_view table
            if ( logger.isDebugEnabled() ) logger.debug("Copying data from "+this.schema+"views_objects table to "+this.schema+"views_objects_in_view table");
            if ( logger.isDebugEnabled() ) logger.debug("Copying data from "+this.schema+"views_connections table to "+this.schema+"views_connections_in_view table");
            executeRequest("INSERT INTO "+this.schema+"views_objects_in_view "
                    +"(object_id, object_version, view_id, view_version, rank) "
                    +"SELECT id, version, view_id, view_version, rank FROM "+this.schema+"views_objects"
                    );

            dropColumn(this.schema+"views_connections", "view_id");
            dropColumn(this.schema+"views_connections", "view_version");
            dropColumn(this.schema+"views_connections", "rank");
            
            dropColumn(this.schema+"views_objects", "view_id");
            dropColumn(this.schema+"views_objects", "view_version");
            dropColumn(this.schema+"views_objects", "rank");

            dbVersion = 206;
        }
        
        // convert from version 206 to 207:
        //      - remove the checksum column from the images table
        //
        if ( dbVersion == 206 ) {
            dropColumn(this.schema+"images", "checksum");
            
            dbVersion = 207;
        }
        
        // convert from version 207 to 208
        //      - create metadata table
        //      - drop columns source_connections and target_connections from views_objects and views_connections tables
        if ( dbVersion == 207 ) {
            DBGui.popup("Please wait while converting data.");
            
            if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schema+"metadata");
            executeRequest("CREATE TABLE "+this.schema+"metadata ("
                    + "parent_id "+this.OBJECTID +" NOT NULL, "
                    + "parent_version " + this.INTEGER +" NOT NULL, "
                    + "rank " + this.INTEGER +" NOT NULL, "
                    + "name " + this.OBJ_NAME + ", "
                    + "value " + this.TEXT + ", "
                    + this.PRIMARY_KEY+" (parent_id, parent_version, rank)"
                    + ")");
            
            dropColumn(this.schema+"views_objects", "source_connections");
            dropColumn(this.schema+"views_objects", "target_connections");
            
            dropColumn(this.schema+"views_connections", "source_connections");
            dropColumn(this.schema+"views_connections", "target_connections");
            
            DBGui.closePopup();
            
            dbVersion = 208;
        }
        
        // convert from version 208 to 209
        //      - add checkedin_by, checkedin_on, deleted_by and deleted_on columns in folders, views, views_connections and views_objects
        //             (they're not yet used, but they're created for uniformity purpose)
        //      - add alpha column in views_objects table
        if ( dbVersion == 208 ) {
        	addColumn(this.schema+"folders", "checkedin_by", this.USERNAME);
        	addColumn(this.schema+"folders", "checkedin_on", this.DATETIME);
        	addColumn(this.schema+"folders", "deleted_by", this.USERNAME);
        	addColumn(this.schema+"folders", "deleted_on", this.DATETIME);
        	
        	addColumn(this.schema+"views", "checkedin_by", this.USERNAME);
        	addColumn(this.schema+"views", "checkedin_on", this.DATETIME);
        	addColumn(this.schema+"views", "deleted_by", this.USERNAME);
        	addColumn(this.schema+"views", "deleted_on", this.DATETIME);
        	
        	addColumn(this.schema+"views_connections", "checkedin_by", this.USERNAME);
        	addColumn(this.schema+"views_connections", "checkedin_on", this.DATETIME);
        	addColumn(this.schema+"views_connections", "deleted_by", this.USERNAME);
        	addColumn(this.schema+"views_connections", "deleted_on", this.DATETIME);
        	
        	addColumn(this.schema+"views_objects", "checkedin_by", this.USERNAME);
        	addColumn(this.schema+"views_objects", "checkedin_on", this.DATETIME);
        	addColumn(this.schema+"views_objects", "deleted_by", this.USERNAME);
        	addColumn(this.schema+"views_objects", "deleted_on", this.DATETIME);
        	
        	addColumn(this.schema+"views_objects", "alpha", this.INTEGER);

        	dbVersion = 209;
        }
        
        // convert from version 209 to 210
        //      - add screenshot_scale_factor and screenshot_border_width in views table
        if ( dbVersion == 209 ) {
        	addColumn(this.schema+"views", "screenshot_scale_factor", this.INTEGER);
        	addColumn(this.schema+"views", "screenshot_border_width", this.INTEGER);
        	
        	dbVersion = 210;
        }
        
        // convert from version 210 to 211
        //      - remove hint_title and hint_content from the views and views_objects table
        if ( dbVersion == 210 ) {
            dropColumn(this.schema+"views", "hint_title");
            dropColumn(this.schema+"views", "hint_content");
            
            dropColumn(this.schema+"views_objects", "hint_title");
            dropColumn(this.schema+"views_objects", "hint_content");
            
            // we need to recalculate the checksums
            DBGui.popup("Please wait while re-calculating checksums on views_objects and views tables.");
            
            DBArchimateModel tempModel = new DBArchimateModel();
            
            try ( DBDatabaseImportConnection importConnection = (DBDatabaseImportConnection)this ) {
                try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version FROM "+this.schema+"views") ) {
                    while ( result.next() ) {
                        IDiagramModel view;
                        DBImportViewFromIdCommand command = new DBImportViewFromIdCommand(importConnection, tempModel, null, result.getString("id"), result.getInt("version"), DBImportMode.forceSharedMode, false);
                        if ( command.canExecute() )
                            command.execute();
                        if ( command.getException() != null )
                            throw command.getException();
                        
                        view = command.getImported();
                        
                        tempModel.countObject(view, true, null);
                        
                        executeRequest("UPDATE "+this.schema+"views SET checksum = ?, container_checksum = ? WHERE id = ? AND version = ?", ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getChecksum(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getContainerChecksum(), result.getString("id"), result.getInt("version"));
                        
                        for ( IDiagramModelObject obj: view.getChildren() ) {
                        	updateChecksum(obj);
                        }
                    }
                }
            }
            tempModel = null;
            
            DBGui.closePopup();
        	
        	dbVersion = 211;
        }

        executeRequest("UPDATE "+this.schema+"database_version SET version = "+dbVersion+" WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
        commit();

        setAutoCommit(true);
    }
    
    private void updateChecksum(IDiagramModelObject obj) throws SQLException {
        executeRequest("UPDATE "+this.schema+"views_object SET checksum = ? WHERE id = ? AND version = ?", ((IDBMetadata)obj).getDBMetadata().getCurrentVersion().getChecksum(), obj.getId(), ((IDBMetadata)obj).getDBMetadata().getInitialVersion().getVersion());
        
        if ( obj instanceof IDiagramModelContainer ) {
        	for ( IDiagramModelObject subObj: ((IDiagramModelContainer)obj).getChildren() ) {
        		updateChecksum(subObj);
        	}
        }
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

        @SuppressWarnings("resource")
		DBRequest request = new DBRequest(this.databaseEntry.getName(), this.connection, "INSERT INTO "+table+" ("+cols.toString()+") VALUES ("+values.toString()+")", newParameters.toArray());
        int rowCount = request.getRowCount();
        request.close();
        
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

        DBSelect result = null;
        try {
            // We do not use a GROUP BY because it does not give the expected result on PostGresSQL ...   
            if ( filter==null || filter.length()==0 )
                result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, name, version FROM "+this.schema+"models m WHERE version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = m.id) ORDER BY name");
            else
                result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, name, version FROM "+this.schema+"models m WHERE version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = m.id) AND UPPER(name) like UPPER(?) ORDER BY name", filter);

            while ( result.next() && result.getString("id") != null ) {
                if (logger.isTraceEnabled() ) logger.trace("Found model \""+result.getString("name")+"\"");
                Hashtable<String, Object> table = new Hashtable<String, Object>();
                table.put("name", result.getString("name"));
                table.put("id", result.getString("id"));
                list.add(table);
            }
        } finally {
            if ( result != null ) {
                result.close();
                result = null;
            }
        }

        return list;
    }

    public String getModelId(String modelName, boolean ignoreCase) throws Exception {
        String whereClause = ignoreCase ? "UPPER(name) = UPPER(?)" : "name = ?";
        
        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT id FROM "+this.schema+"models m WHERE "+whereClause, modelName) ) {  
            if ( result.next() ) 
                return (result.getString("id"));
        }
        
        return null;
    }

    /**
     * Gets the list of versions on a model in the current database
     * @param id the id of the model 
     * @throws Exception
     * @return a list of Hashtables, each containing the version, created_on, created_by, name, note and purpose of one version of the model
     */
    public ArrayList<Hashtable<String, Object>> getModelVersions(String id) throws Exception {
        ArrayList<Hashtable<String, Object>> list = new ArrayList<Hashtable<String, Object>>();

        try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT version, created_by, created_on, name, note, purpose FROM "+this.schema+"models WHERE id = ? ORDER BY version DESC", id) ) {
            while ( result.next() ) {
                if (logger.isTraceEnabled() ) logger.trace("Found model \""+result.getString("name")+"\" version \""+result.getString("version")+"\"");
                Hashtable<String, Object> table = new Hashtable<String, Object>();
                table.put("version", result.getString("version"));
                table.put("created_by", result.getString("created_by"));
                table.put("created_on", result.getTimestamp("created_on"));
                table.put("name", result.getString("name"));
                table.put("note", result.getString("note") == null ? "" : result.getString("note"));
                table.put("purpose", result.getString("purpose") == null ? "" : result.getString("purpose"));
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
    public void rollback(Savepoint savepoint) throws SQLException {
        if ( this.connection == null ) {
            logger.warn("Cannot rollback as there is no database connection opened.");
        } else {
            if ( this.connection.getAutoCommit() ) {
            	if ( logger.isDebugEnabled() ) logger.debug("Do not rollback as database is in auto commit mode.");
            } else {
            	if ( logger.isDebugEnabled() ) logger.debug("Rollbacking database transaction.");
            	if ( savepoint == null )
            		this.connection.rollback();
            	else
            		this.connection.rollback(savepoint);
            }
        }
    }
    
    public void rollback() throws SQLException {
    	rollback(null);
    }
    
    @SafeVarargs
	final public <T> int executeRequest(String request, T... parameters) throws SQLException {
    	int rowCount = 0;
    	
    	@SuppressWarnings("resource")
		DBRequest dbRequest = new DBRequest(this.databaseEntry.getName(), this.connection, request, parameters);
    	rowCount = dbRequest.getRowCount();
    	dbRequest.close();
    	
    	return rowCount;
    }
}