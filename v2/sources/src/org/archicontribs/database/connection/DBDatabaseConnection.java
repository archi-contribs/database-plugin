/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.log4j.Level;
import org.archicontribs.database.DBColumn;
import org.archicontribs.database.DBColumnType;
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.DBTable;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
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
	public static final int databaseVersion = 490;

	/**
	 * the databaseEntry corresponding to the connection
	 */
	@Getter DBDatabaseEntry databaseEntry = null;
	
	/**
	 * the name of the schema
	 */
	@Getter protected String schema = "";
	
	/**
	 * the name of the schema suffixed by a '.' if not empty
	 */
	@Getter protected String schemaPrefix = "";

	/**
	 * Connection to the database
	 */
	@Getter protected Connection connection = null;


	/**
	 * Configuration of the database tables
	 */
	@Getter private List<DBColumn> databaseVersionColumns = null;
	@Getter private List<String> databaseVersionPrimaryKeys = null;

	@Getter private List<DBColumn> modelsColumns = null;
	@Getter private List<String> modelsPrimaryKeys = null;

	@Getter private List<DBColumn> foldersColumns = null;
	@Getter private List<String> foldersPrimaryKeys = null;
	@Getter private List<DBColumn> foldersInModelColumns = null;
	@Getter private List<String> foldersInModelPrimaryKeys = null;

	@Getter private List<DBColumn> elementsColumns = null;
	@Getter private List<String> elementsPrimaryKeys = null;
	@Getter private List<DBColumn> elementsInModelColumns = null;
	@Getter private List<String> elementsInModelPrimaryKeys = null;

	@Getter private List<DBColumn> relationshipsColumns = null;
	@Getter private List<String> relationshipsPrimaryKeys = null;
	@Getter private List<DBColumn> relationshipsInModelColumns = null;
	@Getter private List<String> relationshipsInModelPrimaryKeys = null;

	@Getter private List<DBColumn> viewsColumns = null;
	@Getter private List<String> viewsPrimaryKeys = null;
	@Getter private List<DBColumn> viewsInModelColumns = null;
	@Getter private List<String> viewsInModelPrimaryKeys = null;

	@Getter private List<DBColumn> viewsObjectsColumns = null;
	@Getter private List<String> viewsObjectsPrimaryKeys = null;
	@Getter private List<DBColumn> viewsObjectsInViewColumns = null;
    @Getter private List<String> viewsObjectsInViewPrimaryKeys = null;

    @Getter private List<DBColumn> viewsConnectionsColumns = null;
    @Getter private List<String> viewsConnectionsPrimaryKeys = null;
    @Getter private List<DBColumn> viewsConnectionsInViewColumns = null;
    @Getter private List<String> viewsConnectionsInViewPrimaryKeys = null;
	
	@Getter private List<DBColumn> propertiesColumns = null;
	@Getter private List<String> propertiesPrimaryKeys = null;

	@Getter private List<DBColumn> featuresColumns = null;
	@Getter private List<String> featuresPrimaryKeys = null;
	
	@Getter private List<DBColumn> profilesColumns = null;
	@Getter private List<String> profilesPrimaryKeys = null;
	
	@Getter private List<DBColumn> profilesInModelColumns = null;
	@Getter private List<String> profilesInModelPrimaryKeys = null;

	@Getter private List<DBColumn> bendpointsColumns = null;
	@Getter private List<String> bendpointsPrimaryKeys = null;
	
	@Getter private List<DBColumn> metadataColumns = null;
	@Getter private List<String> metadataPrimaryKeys = null;

	@Getter private List<DBColumn> imagesColumns = null;
	@Getter private List<String> imagesPrimaryKeys = null;

	@Getter private List<DBTable> databaseTables = null;

	/**
	 * Opens a connection to a JDBC database using all the connection details
	 * @param dbEntry class containing the details of the database to connect to
	 * @throws SQLException 
	 */
	protected DBDatabaseConnection(DBDatabaseEntry dbEntry) throws SQLException {
		assert(this.databaseEntry != null);
		this.databaseEntry = dbEntry;
		openConnection();
	}

	/**
	 * Used to switch between ImportConnection and ExportConnection.
	 */
	protected DBDatabaseConnection() {
		if ( this.databaseEntry != null ) {
			this.schema = this.databaseEntry.getSchema();
			this.schemaPrefix = this.databaseEntry.getSchemaPrefix();
		}
	}

	private void openConnection() throws SQLException {
		if ( isConnected() )
			close();

		if ( logger.isDebugEnabled() ) logger.debug("Opening connection to database "+this.databaseEntry.getName()+": driver="+this.databaseEntry.getDriver()+", server="+this.databaseEntry.getServer()+", port="+this.databaseEntry.getPort()+", database="+this.databaseEntry.getDatabase()+", schema="+this.schema+", username="+this.databaseEntry.getUsername());

		String clazz = this.databaseEntry.getDriverClass();
		if ( logger.isDebugEnabled() ) logger.debug("JDBC class = " + clazz);

		String connectionString = this.databaseEntry.getJdbcConnectionString();
		if ( logger.isDebugEnabled() ) logger.debug("JDBC connection string = " + connectionString);
		
		this.schema = this.databaseEntry.getSchema();
		this.schemaPrefix = this.databaseEntry.getSchemaPrefix();

		try {
			// we load the jdbc class
			Class.forName(clazz);
			String driver = this.databaseEntry.getDriver();
			String username = this.databaseEntry.getUsername();
			String encryptedPassword = this.databaseEntry.getEncryptedPassword();
			String password = "";

			if ( DBPlugin.isEmpty(username) ) {
				if ( !DBPlugin.isEmpty(encryptedPassword) )
					throw new SQLException("A password has been provided without a username.");
			} else {
				try {
					password = this.databaseEntry.getDecryptedPassword();
				} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException err) {
					DBGuiUtils.popup(Level.ERROR, "Failed to decrypt the password.", err);
				}

				// if the username is set but not the password, then we show a popup to ask for the password
				if ( DBPlugin.isEmpty(password) ) {
					password = DBGuiUtils.passwordDialog("Please provide the database password", "Database password:");
					if ( password == null ) {
						// password is null if the user clicked on cancel
						throw new SQLException("No password provided.");
					}
					// we register the new password for the current session
					try {
						this.databaseEntry.setDecryptedPassword(password);
					} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException err) {
						DBGuiUtils.popup(Level.ERROR, "Failed to decrypt the password.", err);
					}
				}
			}

			if ( DBPlugin.areEqual(driver, DBDatabase.MSSQL.getDriverName()) && DBPlugin.isEmpty(username) && DBPlugin.isEmpty(password) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Connecting with Windows integrated security");
				this.connection = DriverManager.getConnection(connectionString);
			} else {
				if ( logger.isDebugEnabled() ) logger.debug("Connecting with username = "+username);
				this.connection = DriverManager.getConnection(connectionString, username, password);
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
		} catch (ClassNotFoundException e) {
			throw new SQLException(e);
		}

		if ( logger.isDebugEnabled() ) {
			if ( DBPlugin.isEmpty(this.schema) )
				logger.debug("Will use default schema ");
			else
				logger.debug("Will use schema "+this.schema);
		}

		
	}

	/**
	 * Closes connection to the database
	 * <br>The current transaction must be committed or rolled back before the close. 
	 */
	@Override
	public void close() throws SQLException {
		if ( this.connection == null || this.connection.isClosed() ) {
			if ( logger.isDebugEnabled() ) logger.debug("The database connection is already closed.");
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Closing the database connection.");
			this.connection.close();
		}

		this.connection = null;
		this.databaseEntry = null;
		this.schema = "";
		this.schemaPrefix = "";
	}

	/**
	 * Gets the status of the database connection. You may also be interested in {@link #isConnected()}
	 * @return true if the connection is connected, false if the connection is closed
	 * @throws SQLException 
	 */
	public boolean isConnected() throws SQLException {
		return this.connection != null && !this.connection.isClosed();
	}

	/**
	 * Gets the status of the database connection. You may also be interested in {@link #isConnected()}
	 * @return true if the connection is closed, false if the connection is connected
	 * @throws SQLException 
	 */
	public boolean isClosed() throws SQLException {
		return (this.connection == null) || this.connection.isClosed();
	}
	
	/**
	 * Checks the content of the "database_version" table
	 * @param dbGui the dialog that holds the graphical interface
	 * @return 
	 * @throws Exception 
	 * @returns true if the database version is correct, generates an Exception if not
	 */
	public boolean checkDatabase(DBGui dbGui) throws Exception {
		// No tables to be checked in Neo4J databases
		if ( this.databaseEntry.getDriver().equals(DBDatabase.NEO4J.getDriverName()) )
			return true;
		
		if ( logger.isTraceEnabled() ) logger.trace("Checking \""+this.schemaPrefix+"database_version\" table");

		int currentVersion = 0;
		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT version FROM "+this.schemaPrefix+"database_version WHERE archi_plugin = ?", DBPlugin.pluginName) ) {
			result.next();
			currentVersion = result.getInt("version");
		} catch (@SuppressWarnings("unused") SQLException err) {
			// if the table does not exist
			if ( !DBGuiUtils.question("We successfully connected to the database but it seems that it has not been initialized.\n\nDo you wish to intialize the database ?") )
				throw new SQLException("Database not initialized.");

			createTables(dbGui);
			currentVersion = databaseVersion;
		}

		if ( (currentVersion < 200) || (currentVersion > databaseVersion) )
			throw new SQLException("The database has got an unknown model version (is "+currentVersion+" but should be between 200 and "+databaseVersion+")");

		if ( currentVersion != databaseVersion ) {
			if ( DBGuiUtils.question("The database needs to be upgraded. You will not loose any data during this operation.\n\nDo you wish to upgrade your database ?") ) {
				upgradeDatabase(currentVersion);
				DBGuiUtils.popup(Level.INFO, "Database successfully upgraded.");
			}
			else
				throw new SQLException("The database needs to be upgraded.");
		}
		
		return true;
	}

	/**
	 * Checks the database structure
	 * @param dbGui the dialog that holds the graphical interface
	 * @return 
	 * @throws Exception 
	 * @returns true if the database structure is correct, false if not
	 */
	public String checkDatabaseStructure(DBGui dbGui) throws Exception {
		StringBuilder message = new StringBuilder();
		boolean isDatabaseStructureCorrect = true;
		
		this.schema = this.databaseEntry.getSchema();
		this.schemaPrefix = this.databaseEntry.getSchemaPrefix();

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			// do not need to create tables
			// shouldn't be here anyway if Neo4J database
			return null;
		}
		
		try {
			if ( dbGui != null )
				dbGui.setMessage("Checking the database structure...");
			else
				DBGuiUtils.showPopupMessage("Checking the database structure...");

			if ( !isConnected() )
				openConnection();

			// checking if the database_version table exists
			checkDatabase(dbGui);


			// we check after the eventual database upgrade as database before version 212 did not have an ID
			String databaseId = "";
			try (DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id FROM "+this.schemaPrefix+"database_version WHERE archi_plugin = ?", DBPlugin.pluginName) ) {
				result.next();
				databaseId = result.getString("id");
			} // the "try" manages the result closure even in case of an exception

			if ( !this.databaseEntry.getId().equals(databaseId) ) {
				logger.info("The database ID is \""+databaseId+"\" whereas the Id we knew was \""+this.databaseEntry.getId()+"\"... Updating the databaseEntry.");
				this.databaseEntry.setId(databaseId);
				this.databaseEntry.persistIntoPreferenceStore();
			}
					
			logger.debug("Getting metadata from database connection.");
			DatabaseMetaData metadata = this.connection.getMetaData();
			boolean mustCheckNotNullConstraint = DBPlugin.INSTANCE.getPreferenceStore().getBoolean("checkNotNullConstraints");
			boolean hasGotNotNullErrorsOnly = true;
			
			initializeDatabaseTables();
			
			// we check that all the columns in the table are expected
			for (int t = 0; t < this.databaseTables.size() ; ++t ) {
				String tableName = this.databaseTables.get(t).getName();
				// oracle requires uppercase table names
				if ( this.databaseEntry.getDriver().equals("oracle") ) {
					tableName = tableName.toUpperCase();
					this.schema = this.schema.toUpperCase();
					this.schemaPrefix = this.schemaPrefix.toUpperCase();
				}
				List<DBColumn> expectedColumns = this.databaseTables.get(t).getColumns();
				
				try (ResultSet result = metadata.getColumns(null, DBPlugin.isEmpty(this.schema) ? null : this.schema.toUpperCase(), tableName, null)) {
					boolean isTableCorrect = true;
					
					logger.debug("Table "+this.schemaPrefix+tableName);
					
					// we reset the columns metadata
					for (Iterator<DBColumn> iterator = expectedColumns.iterator(); iterator.hasNext(); ) {
						DBColumn expectedColumn = iterator.next();
						expectedColumn.setMetadata(null);
					}
					
					while( result.next() ) {
						// we check that the table columns have got the right type
						String columnName = result.getString("COLUMN_NAME");
						for (Iterator<DBColumn> iterator = expectedColumns.iterator(); iterator.hasNext(); ) {
							DBColumn expectedColumn = iterator.next();
							if ( columnName.equalsIgnoreCase(expectedColumn.getName()) ) {
								DBColumn columnInTable = new DBColumn(columnName, result.getString("TYPE_NAME"), result.getInt("COLUMN_SIZE"), result.getInt("NULLABLE")==0);
								
								if ( columnInTable.equals(expectedColumn) )
									logger.debug("   Column "+columnInTable.toString()+" is correct");
								else {
									if ( columnInTable.getType().equalsIgnoreCase(expectedColumn.getType()) ) {
										if ( mustCheckNotNullConstraint ) {
											logger.debug("   Column "+columnInTable.getName()+": should be NOT NULL");
											if ( isTableCorrect ) {
												isTableCorrect = false;
												message.append("\nTable "+this.schemaPrefix+tableName);
											}
											message.append("\n   Column "+columnInTable.getName()+": should be NOT NULL");
										} else
											logger.debug("   Column "+columnInTable.getName()+": should be NOT NULL (ignored)");
									} else {
										logger.debug("   Column "+columnInTable.toString()+", but should be "+expectedColumn.getFullType());
										if ( isTableCorrect ) {
											isTableCorrect = false;
											message.append("\nTable "+this.schemaPrefix+tableName);
										}
										message.append("\n   Column "+columnInTable.toString()+", but should be "+expectedColumn.getFullType());
										hasGotNotNullErrorsOnly = false;
									}
								}
								expectedColumn.setMetadata(1);
								break;
							}
						}
					}
					
					// Now, we check that all the expected columns have been found
					for (Iterator<DBColumn> iterator = expectedColumns.iterator(); iterator.hasNext(); ) {
						DBColumn expectedColumn = iterator.next();
						if ( expectedColumn.getMetadata() == null ) {
							if ( isTableCorrect ) {
								isTableCorrect = false;
								message.append("\nTable "+this.schemaPrefix+tableName);
							}
							logger.debug("   Column "+expectedColumn.toString()+" is missing");
							message.append("\n   Column "+expectedColumn.toString()+" is missing");
							hasGotNotNullErrorsOnly = false;
						}
					}
					
					if ( !isTableCorrect )
						isDatabaseStructureCorrect = false;
				} catch (SQLException err) {
					throw err;
				}
				
				/* TODO; check primary keys
				logger.debug("   checking primary keys");
				try (ResultSet result = metadata.getPrimaryKeys(null, this.schema, tableName)) {
					while( result.next() ) {
						String indexName = result.getString("COLUMN_NAME");
						logger.debug("   found column "+indexName+" in primary key");
					}
				} catch (SQLException err) {
					DBGuiUtils.popup(Level.ERROR, "Failed to get table primary keys.", err);
					return;
				}
				*/
			}
			
			if ( hasGotNotNullErrorsOnly )
				message.append("\n\nYou may uncheck the \"Check for NOT NULL\" option in the plugin preferences pages should you wish to use this database.");

		} catch (Exception err) {
			rollback();
			throw err;
		} finally {
			if ( dbGui != null )
				dbGui.closeMessage();
			else
				DBGuiUtils.closePopupMessage();
		}
		
		if ( dbGui != null ) {
			if ( !isDatabaseStructureCorrect )
				DBGuiUtils.popup(Level.WARN, "You may have a look to the following items in your database:\n" + message.toString());
			else
				DBGuiUtils.popup(Level.INFO, "Tables name successfully checked.\nColumns name and type successfully checked");
		}
	
		if ( isDatabaseStructureCorrect )
			return null;
		return message.toString();
	}

	/**
	 * Creates the necessary tables in the database
	 * @param dbGui 
	 * @throws SQLException 
	 */
	private void createTables(DBGui dbGui) throws SQLException {
		//final String[] databaseVersionColumns = {"id", "archi_plugin", "version"};

		try {

			if ( dbGui != null )
				dbGui.setMessage("Creating necessary database tables ...");
			else
				DBGuiUtils.showPopupMessage("Creating necessary database tables ...");

			if ( !isConnected() )
				openConnection();

			setAutoCommit(false);
			
			initializeDatabaseTables();
			
			for ( int i = 0 ; i < this.databaseTables.size() ; ++i ) {
				DBTable table = this.databaseTables.get(i);
				if ( logger.isDebugEnabled() ) logger.debug("Creating table "+table.getFullName());
				executeRequest(table.generateCreateStatement());
			}
			
			// we fill in the database_version table 
			insert(this.schemaPrefix+"database_version", DBColumn.getColumnNames(this.databaseVersionColumns), DBPlugin.createID(null), DBPlugin.pluginName, databaseVersion);
			
			// Oracle do not implement AUTO_INCREMENT columns, so we have to manually create sequences and triggers
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_elements");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_elements_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_elements_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_elements_in_model");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_elements_in_model "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"elements_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_elements_in_model.NEXTVAL INTO :NEW.eim_id FROM DUAL;"
						+ "END;");
				
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_folders_in_model");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_folders_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_folders_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_folders_in_model");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_folders_in_model "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"folders_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_folders_in_model.NEXTVAL INTO :NEW.fim_id FROM DUAL;"
						+ "END;");
				
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_relationships");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_relationships_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_relationships_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_relationships_in_model");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_relationships_in_model "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"relationships_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_relationships_in_model.NEXTVAL INTO :NEW.rim_id FROM DUAL;"
						+ "END;");
				
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_connections_in_view");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_connections_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_connections_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_connections_in_view");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_connections_in_view "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"views_connections_in_view "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_connections_in_view.NEXTVAL INTO :NEW.civ_id FROM DUAL;"
						+ "END;");
				
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_views");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_views_in_model'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_views_in_model START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_views_in_model");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_views_in_model "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"views_in_model "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_views_in_model.NEXTVAL INTO :NEW.vim_id FROM DUAL;"
						+ "END;");
				
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_objects_in_view");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_objects_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_objects_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_objects_in_view");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_objects_in_view "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"views_objects_in_view "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_objects_in_view.NEXTVAL INTO :NEW.oiv_id FROM DUAL;"
						+ "END;");
			}

			commit();
			setAutoCommit(true);

			DBGuiUtils.popup(Level.INFO,"The database has been successfully initialized.");

		} catch (SQLException err) {
			rollback();
			setAutoCommit(true);
			throw err;
		} finally {
			if ( dbGui != null )
				dbGui.closeMessage();
			else
				DBGuiUtils.closePopupMessage();
		}

	}

	/**
	 * @param tableName
	 * @throws SQLException
	 */
	public void dropTableIfExists(String tableName) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Dropping table "+tableName+" if it exists");

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
			executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP TABLE "+tableName+"'; EXCEPTION WHEN OTHERS THEN NULL; END;");
		else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MSSQL.getDriverName()) )
			executeRequest("IF OBJECT_ID('"+tableName+"', 'U') IS NOT NULL DROP TABLE "+tableName);
		else
			executeRequest("DROP TABLE IF EXISTS "+tableName);
	}

	/**
	 * @param tableName
	 * @param columnName
	 * @throws SQLException
	 */
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
						createTableRequest.append(", PRIMARY KEY (");
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

	/**
	 * @param tableName
	 * @param columnName
	 * @param columnType
	 * @throws SQLException
	 */
	public void addColumn(String tableName, String columnName, String columnType) throws SQLException {
		addColumn(tableName, columnName, columnType, true, null);
	}

	/**
	 * @param <T>
	 * @param tableName
	 * @param columnName
	 * @param columnType
	 * @param canBeNull
	 * @param defaultValue
	 * @throws SQLException
	 */
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
	 * @param tableName
	 * @param oldColumnName
	 * @param newColumnName
	 * @param columnType
	 * @throws SQLException
	 */
	public void renameColumn(String tableName, String oldColumnName, String newColumnName, String columnType) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Altering table "+tableName+", renaming column "+oldColumnName+" to "+ newColumnName);

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.SQLITE.getDriverName()) )
			executeRequest("ALTER TABLE "+tableName+" RENAME COLUMN "+oldColumnName+" TO "+newColumnName);

		else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MSSQL.getDriverName()) )
			executeRequest("EXEC sp_rename '"+tableName+"."+oldColumnName+"','"+newColumnName+"','COLUMN'");

		else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.POSTGRESQL.getDriverName()) )
			executeRequest("ALTER TABLE "+tableName+" RENAME "+oldColumnName+" TO "+newColumnName);

		else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.MYSQL.getDriverName()) )
			executeRequest("ALTER TABLE "+tableName+" CHANGE COLUMN "+oldColumnName+" "+newColumnName+" "+columnType);

		else if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
			executeRequest("ALTER TABLE "+tableName+" RENAME COLUMN "+oldColumnName+" TO "+newColumnName);
	}

	/**
	 * Upgrades the database
	 * @param version 
	 * @throws Exception 
	 */
	private void upgradeDatabase(int version) throws Exception {
		int dbVersion = version;
		
		DBColumn booleanColumn = new DBColumn("", this.databaseEntry, DBColumnType.BOOLEAN, false);
		DBColumn integerColumn = new DBColumn("", this.databaseEntry, DBColumnType.INTEGER, false);
		DBColumn textColumn = new DBColumn("", this.databaseEntry, DBColumnType.TEXT, false);
		DBColumn imageColumn = new DBColumn("", this.databaseEntry, DBColumnType.IMAGE, false);
		DBColumn objectIDColumn = new DBColumn("", this.databaseEntry, DBColumnType.OBJECTID, false);
		DBColumn objNameColumn = new DBColumn("", this.databaseEntry, DBColumnType.OBJ_NAME, false);
		DBColumn userNameColumn = new DBColumn("", this.databaseEntry, DBColumnType.USERNAME, false);
		DBColumn datetimeColumn = new DBColumn("", this.databaseEntry, DBColumnType.DATETIME, false);
		DBColumn autoIncrementColumn = new DBColumn("", this.databaseEntry, DBColumnType.AUTO_INCREMENT, false);
		DBColumn strengthColumn = new DBColumn("", this.databaseEntry, DBColumnType.STRENGTH, false);
		
		setAutoCommit(false);
		
		initializeDatabaseTables();

		// convert from version 200 to 201:
		//      - add a blob column into the views table
		if ( dbVersion == 200 ) {
			addColumn(this.schemaPrefix+"views", "screenshot", imageColumn.getFullType());				// executeRequest("ALTER TABLE "+this.schemaPrefix+"views ADD "+COLUMN+" screenshot "+this.IMAGE);

			dbVersion = 201;
		}

		// convert from version 201 to 202:
		//      - add text_position column in the views_connections table
		//      - add source_connections and target_connections to views_objects and views_connections tables
		if ( dbVersion == 201 ) {
			
			addColumn(this.schemaPrefix+"views", "text_position", integerColumn.getType());                   // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_connections ADD "+COLUMN+" text_position "+this.INTEGER);
			addColumn(this.schemaPrefix+"views_objects", "source_connections", textColumn.getType());         // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_objects ADD "+COLUMN+" source_connections "+this.TEXT);
			addColumn(this.schemaPrefix+"views_objects", "target_connections", textColumn.getType());         // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_objects ADD "+COLUMN+" target_connections "+this.TEXT);
			addColumn(this.schemaPrefix+"views_connections", "source_connections", textColumn.getType());     // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_connections ADD "+COLUMN+" source_connections "+this.TEXT);
			addColumn(this.schemaPrefix+"views_connections", "target_connections", textColumn.getType());     // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_connections ADD "+COLUMN+" target_connections "+this.TEXT);

			dbVersion = 202;
		}

		// convert from version 202 to 203:
		//      - add element_version column to the views_objects table
		//      - add relationship_version column to the views_connections table
		if ( dbVersion == 202 ) {
			addColumn(this.schemaPrefix+"views_connections", "relationship_version", integerColumn.getType(), false, 1);     // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_connections ADD "+COLUMN+" relationship_version "+this.INTEGER);
			addColumn(this.schemaPrefix+"views_objects", "element_version", integerColumn.getType(), false, 1);             // executeRequest("ALTER TABLE "+this.schemaPrefix+"views_objects ADD "+COLUMN+" element_version "+this.INTEGER);

			dbVersion = 203;
		}

		// convert from version 203 to 204:
		//      - add a checksum to the model
		//
		if ( dbVersion == 203 ) {
			addColumn(this.schemaPrefix+"models", "checksum", objectIDColumn.getType(), false, "");

			if ( logger.isDebugEnabled() ) logger.debug("Calculating models checksum");
			try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version, name, note, purpose FROM "+this.schemaPrefix+"models") ) {
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
						DBGuiUtils.popup(Level.FATAL, "Failed to calculate models checksum.", err);
						rollback();
						return;
					}
					executeRequest("UPDATE "+this.schemaPrefix+"models SET checksum = ? WHERE id = ? AND version = ?", checksum, result.getString("id"), result.getInt("version"));
				}
			}

			dbVersion = 204;
		}

		// convert from version 204 to 205:
		//      - add a container_checksum column in the views table
		//
		if ( dbVersion == 204 ) {
			addColumn(this.schemaPrefix+"views", "container_checksum", objectIDColumn.getType(), false, "");

			DBGuiUtils.showPopupMessage("Please wait while calculating new checksum on views table.");

			DBArchimateModel tempModel = new DBArchimateModel();
			try ( DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(this) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Calculating containers checksum");
				try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version FROM "+this.schemaPrefix+"views") ) {
					while ( result.next() ) {
						IDiagramModel view;
						DBImportViewFromIdCommand command = new DBImportViewFromIdCommand(importConnection, tempModel, null, result.getString("id"), result.getInt("version"), DBImportMode.templateMode, false);
						if ( command.canExecute() )
							command.execute();
						if ( command.getException() != null )
							throw command.getException();

						view = command.getImported();

						executeRequest("UPDATE "+this.schemaPrefix+"views SET container_checksum = ? WHERE id = ? AND version = ?", DBChecksum.calculateChecksum(view), result.getString("id"), result.getInt("version"));
					}
				}
			}
			tempModel = null;

			DBGuiUtils.closePopupMessage();

			dbVersion = 205;
		}

		// convert from version 205 to 206:
		//      - add the created_by and created_on columns in the views_connections and views_objects tables
		//      - create tables views_connections_in_view and views_objects_in_view
		//      - remove the rank, view_id and view_version columns from the views_connections and views_objects tables
		//
		if ( dbVersion == 205 ) {
			Timestamp now = new Timestamp(0);

			addColumn(this.schemaPrefix+"views_connections", "created_by", userNameColumn.getType(), false, "databasePlugin");			// we set dummy value to satisfy the NOT NULL condition
			addColumn(this.schemaPrefix+"views_connections", "created_on", datetimeColumn.getType(), false, now);						// we set dummy value to satisfy the NOT NULL condition

			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
				executeRequest("UPDATE "+this.schemaPrefix+"views_connections c SET (created_on, created_by) = (SELECT created_on, created_by FROM "+this.schemaPrefix+"views WHERE id = c.view_id AND version = c.view_version)");
			else
				executeRequest("UPDATE "+this.schemaPrefix+"views_connections SET created_on = j.created_on, created_by = j.created_by FROM (SELECT c.id, v.created_on, v.created_by FROM "+this.schemaPrefix+"views_connections c JOIN "+this.schemaPrefix+"views v ON v.id = c.view_id AND v.version = c.view_version) j WHERE "+this.schemaPrefix+"views_connections.id = j.id");






			addColumn(this.schemaPrefix+"views_objects", "created_by", userNameColumn.getType(), false, "databasePlugin");				// we set dummy value to satisfy the NOT NULL condition
			addColumn(this.schemaPrefix+"views_objects", "created_on", datetimeColumn.getType(), false, now);							// we set dummy value to satisfy the NOT NULL condition

			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) )
				executeRequest("UPDATE "+this.schemaPrefix+"views_objects o SET (created_on, created_by) = (SELECT created_on, created_by FROM "+this.schemaPrefix+"views WHERE id = o.view_id AND version = o.view_version)");
			else
				executeRequest("UPDATE "+this.schemaPrefix+"views_objects SET created_on = j.created_on, created_by = j.created_by FROM (SELECT c.id, v.created_on, v.created_by FROM "+this.schemaPrefix+"views_objects c JOIN "+this.schemaPrefix+"views v ON v.id = c.view_id AND v.version = c.view_version) j WHERE "+this.schemaPrefix+"views_objects.id = j.id");



			if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schemaPrefix+"views_connections_in_view");
			executeRequest("CREATE TABLE "+this.schemaPrefix+"views_connections_in_view ("
					+ "civ_id " + autoIncrementColumn.getType()+", "
					+ "connection_id " + objectIDColumn.getType() +" NOT NULL, "
					+ "connection_version " + integerColumn.getType() +" NOT NULL, "
					+ "view_id " + objectIDColumn.getType() +" NOT NULL, "
					+ "view_version " + integerColumn.getType() +" NOT NULL, "
					+ "pos " + integerColumn.getType() +" NOT NULL"
					+ ", PRIMARY KEY (civ_id)"
					+ ")");
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_connections_in_view");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_connections_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_connections_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_connections_in_view");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_connections_in_view "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"views_connections_in_view "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_connections_in_view.NEXTVAL INTO :NEW.civ_id FROM DUAL;"
						+ "END;");
			}

			// we fill in the views_connections_in_view table
			if ( logger.isDebugEnabled() ) logger.debug("Copying data from "+this.schemaPrefix+"views_connections table to "+this.schemaPrefix+"views_connections_in_view table");
			executeRequest("INSERT INTO "+this.schemaPrefix+"views_connections_in_view "
					+"(connection_id, connection_version, view_id, view_version, pos) "
					+"SELECT id, version, view_id, view_version, pos FROM "+this.schemaPrefix+"views_connections"
					);

			if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schemaPrefix+"views_objects_in_view");
			executeRequest("CREATE TABLE "+this.schemaPrefix+"views_objects_in_view ("
					+ "oiv_id " + autoIncrementColumn.getType()+", "
					+ "object_id " + objectIDColumn.getType() +" NOT NULL, "
					+ "object_version " + integerColumn.getType() +" NOT NULL, "
					+ "view_id " + objectIDColumn.getType() +" NOT NULL, "
					+ "view_version " + integerColumn.getType() +" NOT NULL, "
					+ "pos " + integerColumn.getType() +" NOT NULL"
					+ ", PRIMARY KEY (oiv_id)"
					+ ")");
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.ORACLE.getDriverName()) ) {
				if ( logger.isDebugEnabled() ) logger.debug("Creating sequence "+this.schemaPrefix+"seq_objects_in_view");
				executeRequest("BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE "+this.schemaPrefix+"seq_objects_in_view'; EXCEPTION WHEN OTHERS THEN NULL; END;");
				executeRequest("CREATE SEQUENCE "+this.schemaPrefix+"seq_objects_in_view START WITH 1 INCREMENT BY 1 CACHE 100");

				if ( logger.isDebugEnabled() ) logger.debug("Creating trigger "+this.schemaPrefix+"trigger_objects_in_view");
				executeRequest("CREATE OR REPLACE TRIGGER "+this.schemaPrefix+"trigger_objects_in_view "
						+ "BEFORE INSERT ON "+this.schemaPrefix+"views_objects_in_view "
						+ "FOR EACH ROW "
						+ "BEGIN"
						+ "  SELECT "+this.schemaPrefix+"seq_objects_in_view.NEXTVAL INTO :NEW.oiv_id FROM DUAL;"
						+ "END;");
			}

			// we fill in the views_objects_in_view table
			if ( logger.isDebugEnabled() ) logger.debug("Copying data from "+this.schemaPrefix+"views_objects table to "+this.schemaPrefix+"views_objects_in_view table");
			if ( logger.isDebugEnabled() ) logger.debug("Copying data from "+this.schemaPrefix+"views_connections table to "+this.schemaPrefix+"views_connections_in_view table");
			executeRequest("INSERT INTO "+this.schemaPrefix+"views_objects_in_view "
					+"(object_id, object_version, view_id, view_version, pos) "
					+"SELECT id, version, view_id, view_version, pos FROM "+this.schemaPrefix+"views_objects"
					);

			dropColumn(this.schemaPrefix+"views_connections", "view_id");
			dropColumn(this.schemaPrefix+"views_connections", "view_version");
			dropColumn(this.schemaPrefix+"views_connections", "pos");

			dropColumn(this.schemaPrefix+"views_objects", "view_id");
			dropColumn(this.schemaPrefix+"views_objects", "view_version");
			dropColumn(this.schemaPrefix+"views_objects", "pos");

			dbVersion = 206;
		}

		// convert from version 206 to 207:
		//      - remove the checksum column from the images table
		//
		if ( dbVersion == 206 ) {
			dropColumn(this.schemaPrefix+"images", "checksum");

			dbVersion = 207;
		}

		// convert from version 207 to 208
		//      - create metadata table
		//      - drop columns source_connections and target_connections from views_objects and views_connections tables
		if ( dbVersion == 207 ) {
			DBGuiUtils.showPopupMessage("Please wait while converting data.");

			if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schemaPrefix+"metadata");
			executeRequest("CREATE TABLE "+this.schemaPrefix+"metadata ("
					+ "parent_id "+objectIDColumn.getType() +" NOT NULL, "
					+ "parent_version " + integerColumn.getType() +" NOT NULL, "
					+ "pos " + integerColumn.getType() +" NOT NULL, "
					+ "name " + objNameColumn.getType() + ", "
					+ "value " + textColumn.getType()+ ", "
					+ "PRIMARY KEY (parent_id, parent_version, pos)"
					+ ")");

			dropColumn(this.schemaPrefix+"views_objects", "source_connections");
			dropColumn(this.schemaPrefix+"views_objects", "target_connections");

			dropColumn(this.schemaPrefix+"views_connections", "source_connections");
			dropColumn(this.schemaPrefix+"views_connections", "target_connections");

			DBGuiUtils.closePopupMessage();

			dbVersion = 208;
		}

		// convert from version 208 to 209
		//      - add checkedin_by, checkedin_on, deleted_by and deleted_on columns in folders, views, views_connections and views_objects
		//             (they're not yet used, but they're created for uniformity purpose)
		//      - add alpha column in views_objects table
		if ( dbVersion == 208 ) {
			addColumn(this.schemaPrefix+"folders", "checkedin_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"folders", "checkedin_on", datetimeColumn.getType());
			addColumn(this.schemaPrefix+"folders", "deleted_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"folders", "deleted_on", datetimeColumn.getType());

			addColumn(this.schemaPrefix+"views", "checkedin_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"views", "checkedin_on", datetimeColumn.getType());
			addColumn(this.schemaPrefix+"views", "deleted_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"views", "deleted_on", datetimeColumn.getType());

			addColumn(this.schemaPrefix+"views_connections", "checkedin_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"views_connections", "checkedin_on", datetimeColumn.getType());
			addColumn(this.schemaPrefix+"views_connections", "deleted_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"views_connections", "deleted_on", datetimeColumn.getType());

			addColumn(this.schemaPrefix+"views_objects", "checkedin_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"views_objects", "checkedin_on", datetimeColumn.getType());
			addColumn(this.schemaPrefix+"views_objects", "deleted_by", userNameColumn.getType());
			addColumn(this.schemaPrefix+"views_objects", "deleted_on", datetimeColumn.getType());

			addColumn(this.schemaPrefix+"views_objects", "alpha", integerColumn.getType());

			dbVersion = 209;
		}

		// convert from version 209 to 210
		//      - add screenshot_scale_factor and screenshot_border_width in views table
		if ( dbVersion == 209 ) {
			addColumn(this.schemaPrefix+"views", "screenshot_scale_factor", integerColumn.getType());
			addColumn(this.schemaPrefix+"views", "screenshot_border_width", integerColumn.getType());

			dbVersion = 210;
		}

		// convert from version 210 to 211
		//      - remove hint_title and hint_content from the views and views_objects table
		if ( dbVersion == 210 ) {
			dropColumn(this.schemaPrefix+"views", "hint_title");
			dropColumn(this.schemaPrefix+"views", "hint_content");

			dropColumn(this.schemaPrefix+"views_objects", "hint_title");
			dropColumn(this.schemaPrefix+"views_objects", "hint_content");

			// we need to recalculate the checksums
			DBGuiUtils.showPopupMessage("Please wait while re-calculating checksums on views_objects and views tables.");

			DBArchimateModel tempModel = new DBArchimateModel();

			try ( DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(this) ) {
				try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, version FROM "+this.schemaPrefix+"views") ) {
					while ( result.next() ) {
						IDiagramModel view;

						DBImportViewFromIdCommand command = new DBImportViewFromIdCommand(importConnection, tempModel, null, result.getString("id"), result.getInt("version"), DBImportMode.forceSharedMode, false);
						if ( command.canExecute() )
							command.execute();
						if ( command.getException() != null )
							throw command.getException();

						view = command.getImported();
						DBMetadata dbMetadata = DBMetadata.getDBMetadata(view);

						tempModel.countObject(view, true);

						executeRequest("UPDATE "+this.schemaPrefix+"views SET checksum = ?, container_checksum = ? WHERE id = ? AND version = ?", dbMetadata.getCurrentVersion().getChecksum(), dbMetadata.getCurrentVersion().getContainerChecksum(), result.getString("id"), result.getInt("version"));

						for ( IDiagramModelObject obj: view.getChildren() ) {
							updateChecksum(obj);
						}
					}
				}
			}
			tempModel = null;

			DBGuiUtils.closePopupMessage();

			dbVersion = 211;
		}

		// convert from version 211 to 212
		//      - create table features
		//		- add columns "properties" and "features" to all component tables
		//		- add column "bendpoints" to "views_connections" table
		//		- add column "is_directed" to "relationships" table
		//		- add column "id" to "database_version table
		if ( dbVersion == 211 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Creating table "+this.schemaPrefix+"features");
			executeRequest("CREATE TABLE "+this.schemaPrefix+"features ("
					+ "parent_id "+objectIDColumn.getType()+" NOT NULL, "
					+ "parent_version " + integerColumn.getType() +" NOT NULL, "
					+ "pos " + integerColumn.getType() +" NOT NULL, "
					+ "name " + objNameColumn.getType() + ", "
					+ "value " + textColumn.getType() + ", "
					+ "PRIMARY KEY (parent_id, parent_version, pos)"
					+ ")");

			String[] tableNames = {"models", "folders", "elements", "relationships", "views", "views_objects", "views_connections"};   
			for (String tableName: tableNames) {
				// we initialise the value to true as we do not know if the components have got properties, so we reproduce the previous behaviour
				addColumn(this.schemaPrefix+tableName, "properties", integerColumn.getType(), true, 1);

				// we initialise the value to false as we're sure that the components do not have features yet
				addColumn(this.schemaPrefix+tableName, "features", integerColumn.getType(), true, 1);
			}

			// we initialise the value to true as we do not know if the connection have got bendpoints, so we reproduce the previous behaviour
			addColumn(this.schemaPrefix+"views_connections", "bendpoints", integerColumn.getType(), true, 1);

			// we do not initialise the value, as NULL will be treated as false which is the default value (association relationships are not directed by default)
			addColumn(this.schemaPrefix+"relationships", "is_directed", booleanColumn.getType(), true, 0);

			// we add the new id column with a generated ID and save this ID in the preferences file for later use
			addColumn(this.schemaPrefix+"database_version", "id", objectIDColumn.getType(), false, "");

			this.databaseEntry.setId(DBPlugin.createID(null));
			executeRequest("UPDATE "+this.schemaPrefix+"database_version SET id = '"+this.databaseEntry.getId()+"' WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
			// if the databaseEntry.index is different from -1, then the databaseEntry is persisted in the database, so we persist the new ID in the preference store
			if ( this.databaseEntry.getIndex() != -1 )
				this.databaseEntry.persistIntoPreferenceStore();

			dbVersion = 212;
		}
		
		// convert from version 212 to 213
		//      - rename "rank" column to "pos" in all tables where required
		//      - change "strength" column from varchar(20) to blob
		if ( dbVersion == 212 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Renaming \"rank\" column to \"pos\" in all tables.");
			try {
				renameColumn(this.schemaPrefix+"properties", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in properties table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   properties table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"metadata", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in metadata table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   metadata table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"features", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in features table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   features table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"elements_in_model", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in elements_in_model table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   elements_in_model table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"relationships_in_model", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in relationships_in_model table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   relationships_in_model table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"folders_in_model", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in folders_in_model table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   folders_in_model table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"views_in_model", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in views_in_model table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   views_in_model table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"views_objects_in_view", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in views_objects_in_view table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   views_objects_in_view table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"views_connections_in_view", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in views_connections_in_view table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   views_connections_in_view table unmodified");
			}
			try {
				renameColumn(this.schemaPrefix+"bendpoints", "rank", "pos", integerColumn.getType());
				if ( logger.isDebugEnabled() ) logger.debug("   column renamed in bendpoints table");
			} catch(@SuppressWarnings("unused") Exception ign) { 
				if ( logger.isDebugEnabled() ) logger.debug("   bendpoints table unmodified");
			}
			
			if ( logger.isDebugEnabled() ) logger.debug("Changing strength column of relationships table from varchr(20) to clob.");
			renameColumn(this.schemaPrefix+"relationships", "strength", "old_strength", strengthColumn.getType());
			addColumn(this.schemaPrefix+"relationships", "strength", textColumn.getType());
			executeRequest("UPDATE "+this.schemaPrefix+"relationships SET strength = old_strength");
			dropColumn(this.schemaPrefix+"relationships", "old_strength");
			
			dbVersion = 213;
		}
		
		// convert from version 213 to 490
		//      - create profiles and profiles_in_model tables
		//      - add profile column in elements and relationships tables
		if ( dbVersion == 213 ) {
			// create profiles and profiles_in_model tables
			for ( int i = 0 ; i < this.databaseTables.size() ; ++i ) {
				DBTable table = this.databaseTables.get(i);
				if ( table.getName().equals("profiles") ) {
					if ( logger.isDebugEnabled() ) logger.debug("Creating profiles table");
					executeRequest(table.generateCreateStatement());
				}
				if ( table.getName().equals("profiles_in_model") ) {
					if ( logger.isDebugEnabled() ) logger.debug("Creating profiles_in_model table");
					executeRequest(table.generateCreateStatement());
				}
			}
			
			// add profile column in elements and relationships tables
			addColumn(this.schemaPrefix+"elements",      "profile", objectIDColumn.getType());
			addColumn(this.schemaPrefix+"relationships", "profile", objectIDColumn.getType());
			
			dbVersion = 490;
		}

		if ( logger.isTraceEnabled() ) logger.trace("Updating database version to 490");
		executeRequest("UPDATE "+this.schemaPrefix+"database_version SET version = "+dbVersion+" WHERE archi_plugin = '"+DBPlugin.pluginName+"'");
		commit();

		setAutoCommit(true);
	}

	private void updateChecksum(IDiagramModelObject obj) throws SQLException {
		DBMetadata dbMetadata = DBMetadata.getDBMetadata(obj);

		executeRequest("UPDATE "+this.schemaPrefix+"views_object SET checksum = ? WHERE id = ? AND version = ?", dbMetadata.getCurrentVersion().getChecksum(), obj.getId(), dbMetadata.getInitialVersion().getVersion());

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
	 * @param table 
	 * @param columns 
	 * @param parameters 
	 * @param <T> 
	 * @return The number of lines inserted in the table
	 * @throws SQLException 
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
		
		this.schema = this.databaseEntry.getSchema();
		this.schemaPrefix = this.databaseEntry.getSchemaPrefix();

		DBSelect result = null;
		try {
			// We do not use a GROUP BY because it does not give the expected result on PostGresSQL ...   
			if ( filter==null || filter.length()==0 )
				result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, name, version, created_on FROM "+this.schemaPrefix+"models m WHERE version = (SELECT MAX(version) FROM "+this.schemaPrefix+"models WHERE id = m.id) ORDER BY name");
			else
				result = new DBSelect(this.databaseEntry.getName(), this.connection, "SELECT id, name, version, created_on FROM "+this.schemaPrefix+"models m WHERE version = (SELECT MAX(version) FROM "+this.schemaPrefix+"models WHERE id = m.id) AND UPPER(name) like UPPER(?) ORDER BY name", filter);

			while ( result.next() && result.getString("id") != null ) {
				if (logger.isTraceEnabled() ) logger.trace("Found model \""+result.getString("name")+"\"");
				Hashtable<String, Object> table = new Hashtable<String, Object>();
				table.put("name", result.getString("name"));
				table.put("id", result.getString("id"));
				table.put("created_on",  result.getDate("created_on"));
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

	/**
	 * @param modelName
	 * @param ignoreCase
	 * @return
	 * @throws Exception
	 */
	public String getModelId(String modelName, boolean ignoreCase) throws Exception {
		String whereClause = ignoreCase ? "UPPER(name) = UPPER(?)" : "name = ?";

		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT id FROM "+this.schemaPrefix+"models m WHERE "+whereClause, modelName) ) {  
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

		try ( DBSelect result = new DBSelect(this.databaseEntry.getName(), this.connection,"SELECT version, created_by, created_on, name, note, purpose, checksum FROM "+this.schemaPrefix+"models WHERE id = ? ORDER BY version DESC", id) ) {
			while ( result.next() ) {
				if (logger.isTraceEnabled() ) logger.trace("Found model \""+result.getString("name")+"\" version \""+result.getString("version")+"\" checksum=\""+result.getString("checksum")+"\"");
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
	 * @param autoCommit 
	 * @throws SQLException 
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Setting database auto commit to "+String.valueOf(autoCommit));
		this.connection.setAutoCommit(autoCommit);
	}

	/**
	 * Commits the current transaction
	 * @throws SQLException 
	 */
	public void commit() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Committing database transaction.");
		this.connection.commit();
	}

	/**
	 * Rollbacks the current transaction
	 * @param savepoint 
	 * @throws SQLException 
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

	/**
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		rollback(null);
	}

	/**
	 * @param <T>
	 * @param request
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	@SafeVarargs
	final public <T> int executeRequest(String request, T... parameters) throws SQLException {
		int rowCount = 0;

		@SuppressWarnings("resource")
		DBRequest dbRequest = new DBRequest(this.databaseEntry.getName(), this.connection, request, parameters);
		rowCount = dbRequest.getRowCount();
		dbRequest.close();

		return rowCount;
	}
	
	private void initializeDatabaseTables() throws SQLException {
		// DatabaseVersions table
		this.databaseVersionColumns = new ArrayList<DBColumn>();
		this.databaseVersionColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.databaseVersionColumns.add(new DBColumn("archi_plugin", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.databaseVersionColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));

		this.modelsColumns = new ArrayList<DBColumn>();
		this.modelsColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.modelsColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.modelsColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, true));
		this.modelsColumns.add(new DBColumn("note", this.databaseEntry, DBColumnType.TEXT, false));
		this.modelsColumns.add(new DBColumn("purpose", this.databaseEntry, DBColumnType.TEXT, false));
		this.modelsColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.modelsColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.modelsColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.modelsColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.modelsColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.modelsColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.modelsColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.modelsColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.modelsColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.modelsPrimaryKeys = new ArrayList<String>();
		this.modelsPrimaryKeys.add("id");
		this.modelsPrimaryKeys.add("version");

		// Folders table
		this.foldersColumns = new ArrayList<DBColumn>();
		this.foldersColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.foldersColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.foldersColumns.add(new DBColumn("type", this.databaseEntry, DBColumnType.INTEGER, true));
		this.foldersColumns.add(new DBColumn("root_type", this.databaseEntry, DBColumnType.INTEGER, true));
		this.foldersColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, true));
		this.foldersColumns.add(new DBColumn("documentation", this.databaseEntry, DBColumnType.TEXT, false));
		this.foldersColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.foldersColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.foldersColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.foldersColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.foldersColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.foldersColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.foldersColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.foldersColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.foldersColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.foldersPrimaryKeys = new ArrayList<String>();
		this.foldersPrimaryKeys.add("id");
		this.foldersPrimaryKeys.add("version");
		
		// FoldersInModel table
		this.foldersInModelColumns = new ArrayList<DBColumn>();
		this.foldersInModelColumns.add(new DBColumn("fim_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.foldersInModelColumns.add(new DBColumn("folder_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.foldersInModelColumns.add(new DBColumn("folder_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.foldersInModelColumns.add(new DBColumn("parent_folder_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.foldersInModelColumns.add(new DBColumn("model_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.foldersInModelColumns.add(new DBColumn("model_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.foldersInModelColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.foldersInModelPrimaryKeys = new ArrayList<String>();
		this.foldersInModelPrimaryKeys.add("fim_id");

		// Elements table
		this.elementsColumns = new ArrayList<DBColumn>();
		this.elementsColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.elementsColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.elementsColumns.add(new DBColumn("class", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.elementsColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.elementsColumns.add(new DBColumn("documentation", this.databaseEntry, DBColumnType.TEXT, false));
		this.elementsColumns.add(new DBColumn("type", this.databaseEntry, DBColumnType.TYPE, false));
		this.elementsColumns.add(new DBColumn("profile", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.elementsColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.elementsColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.elementsColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.elementsColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.elementsColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.elementsColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.elementsColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.elementsColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.elementsColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.elementsPrimaryKeys = new ArrayList<String>();
		this.elementsPrimaryKeys.add("id");
		this.elementsPrimaryKeys.add("version");

		// ElementsInModel table
		this.elementsInModelColumns = new ArrayList<DBColumn>();
		this.elementsInModelColumns.add(new DBColumn("eim_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.elementsInModelColumns.add(new DBColumn("element_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.elementsInModelColumns.add(new DBColumn("element_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.elementsInModelColumns.add(new DBColumn("parent_folder_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.elementsInModelColumns.add(new DBColumn("model_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.elementsInModelColumns.add(new DBColumn("model_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.elementsInModelColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.elementsInModelPrimaryKeys = new ArrayList<String>();
		this.elementsInModelPrimaryKeys.add("eim_id");

		// Relationships table
		this.relationshipsColumns = new ArrayList<DBColumn>();
		this.relationshipsColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.relationshipsColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.relationshipsColumns.add(new DBColumn("class", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.relationshipsColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.relationshipsColumns.add(new DBColumn("documentation", this.databaseEntry, DBColumnType.TEXT, false));
		this.relationshipsColumns.add(new DBColumn("source_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.relationshipsColumns.add(new DBColumn("target_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.relationshipsColumns.add(new DBColumn("strength", this.databaseEntry, DBColumnType.TEXT, false));
		this.relationshipsColumns.add(new DBColumn("access_type", this.databaseEntry, DBColumnType.INTEGER, false));
		this.relationshipsColumns.add(new DBColumn("is_directed", this.databaseEntry, DBColumnType.BOOLEAN, false));
		this.relationshipsColumns.add(new DBColumn("profile", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.relationshipsColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.relationshipsColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.relationshipsColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.relationshipsColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.relationshipsColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.relationshipsColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.relationshipsColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.relationshipsColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.relationshipsColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.relationshipsPrimaryKeys = new ArrayList<String>();
		this.relationshipsPrimaryKeys.add("id");
		this.relationshipsPrimaryKeys.add("version");

		// RelationshipsInModel table
		this.relationshipsInModelColumns = new ArrayList<DBColumn>();
		this.relationshipsInModelColumns.add(new DBColumn("rim_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.relationshipsInModelColumns.add(new DBColumn("relationship_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.relationshipsInModelColumns.add(new DBColumn("relationship_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.relationshipsInModelColumns.add(new DBColumn("parent_folder_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.relationshipsInModelColumns.add(new DBColumn("model_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.relationshipsInModelColumns.add(new DBColumn("model_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.relationshipsInModelColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.relationshipsInModelPrimaryKeys = new ArrayList<String>();
		this.relationshipsInModelPrimaryKeys.add("rim_id");

		// Views table
		this.viewsColumns = new ArrayList<DBColumn>();
		this.viewsColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsColumns.add(new DBColumn("class", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.viewsColumns.add(new DBColumn("documentation", this.databaseEntry, DBColumnType.TEXT, false));
		this.viewsColumns.add(new DBColumn("background", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsColumns.add(new DBColumn("connection_router_type", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsColumns.add(new DBColumn("viewpoint", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsColumns.add(new DBColumn("screenshot", this.databaseEntry, DBColumnType.IMAGE, false));
		this.viewsColumns.add(new DBColumn("screenshot_scale_factor", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsColumns.add(new DBColumn("screenshot_border_width", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.viewsColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.viewsColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.viewsColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.viewsColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.viewsColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.viewsColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsColumns.add(new DBColumn("container_checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.viewsPrimaryKeys = new ArrayList<String>();
		this.viewsPrimaryKeys.add("id");
		this.viewsPrimaryKeys.add("version");

		// ViewsInModel table
		this.viewsInModelColumns = new ArrayList<DBColumn>();
		this.viewsInModelColumns.add(new DBColumn("vim_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.viewsInModelColumns.add(new DBColumn("view_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsInModelColumns.add(new DBColumn("view_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsInModelColumns.add(new DBColumn("parent_folder_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsInModelColumns.add(new DBColumn("model_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsInModelColumns.add(new DBColumn("model_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsInModelColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.viewsInModelPrimaryKeys =  new ArrayList<String>();
		this.viewsInModelPrimaryKeys.add("vim_id");

		// ViewsObjects table
		this.viewsObjectsColumns = new ArrayList<DBColumn>();
		this.viewsObjectsColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsObjectsColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsObjectsColumns.add(new DBColumn("class", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsObjectsColumns.add(new DBColumn("container_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsObjectsColumns.add(new DBColumn("element_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsObjectsColumns.add(new DBColumn("element_version", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("diagram_ref_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsObjectsColumns.add(new DBColumn("border_color", this.databaseEntry, DBColumnType.COLOR, false));
		this.viewsObjectsColumns.add(new DBColumn("border_type", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("content", this.databaseEntry, DBColumnType.TEXT, false));
		this.viewsObjectsColumns.add(new DBColumn("documentation", this.databaseEntry, DBColumnType.TEXT, false));
		this.viewsObjectsColumns.add(new DBColumn("is_locked", this.databaseEntry, DBColumnType.BOOLEAN, false));
		this.viewsObjectsColumns.add(new DBColumn("image_path", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsObjectsColumns.add(new DBColumn("image_position", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("line_color", this.databaseEntry, DBColumnType.COLOR, false));
		this.viewsObjectsColumns.add(new DBColumn("line_width", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("fill_color", this.databaseEntry, DBColumnType.COLOR, false));
		this.viewsObjectsColumns.add(new DBColumn("alpha", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("font", this.databaseEntry, DBColumnType.FONT, false));
		this.viewsObjectsColumns.add(new DBColumn("font_color", this.databaseEntry, DBColumnType.COLOR, false));
		this.viewsObjectsColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.viewsObjectsColumns.add(new DBColumn("notes", this.databaseEntry, DBColumnType.TEXT, false));
		this.viewsObjectsColumns.add(new DBColumn("text_alignment", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("text_position", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("type", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("x", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("y", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("width", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("height", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.viewsObjectsColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.viewsObjectsColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.viewsObjectsColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.viewsObjectsColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.viewsObjectsColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.viewsObjectsColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsObjectsColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.viewsObjectsPrimaryKeys = new ArrayList<String>();
		this.viewsObjectsPrimaryKeys.add("id");
		this.viewsObjectsPrimaryKeys.add("version");
	    
		// ViewsConnections table
		this.viewsConnectionsColumns = new ArrayList<DBColumn>();
		this.viewsConnectionsColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsConnectionsColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsConnectionsColumns.add(new DBColumn("class", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsConnectionsColumns.add(new DBColumn("container_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsConnectionsColumns.add(new DBColumn("relationship_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsConnectionsColumns.add(new DBColumn("relationship_version", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsConnectionsColumns.add(new DBColumn("source_object_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsConnectionsColumns.add(new DBColumn("target_object_id", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.viewsConnectionsColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.viewsConnectionsColumns.add(new DBColumn("documentation", this.databaseEntry, DBColumnType.TEXT, false));
		this.viewsConnectionsColumns.add(new DBColumn("is_locked", this.databaseEntry, DBColumnType.BOOLEAN, false));
		this.viewsConnectionsColumns.add(new DBColumn("line_color", this.databaseEntry, DBColumnType.COLOR, false));
		this.viewsConnectionsColumns.add(new DBColumn("line_width", this.databaseEntry, DBColumnType.INTEGER, false));			
		this.viewsConnectionsColumns.add(new DBColumn("font", this.databaseEntry, DBColumnType.FONT, false));
		this.viewsConnectionsColumns.add(new DBColumn("font_color", this.databaseEntry, DBColumnType.COLOR, false));
		this.viewsConnectionsColumns.add(new DBColumn("text_position", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsConnectionsColumns.add(new DBColumn("type", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsConnectionsColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.viewsConnectionsColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.viewsConnectionsColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.viewsConnectionsColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.viewsConnectionsColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.viewsConnectionsColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.viewsConnectionsColumns.add(new DBColumn("bendpoints", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsConnectionsColumns.add(new DBColumn("properties", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsConnectionsColumns.add(new DBColumn("features", this.databaseEntry, DBColumnType.INTEGER, false));
		this.viewsConnectionsColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.viewsConnectionsPrimaryKeys = new ArrayList<String>();
		this.viewsConnectionsPrimaryKeys.add("id");
		this.viewsConnectionsPrimaryKeys.add("version");

		// Properties table
		this.propertiesColumns = new ArrayList<DBColumn>();
		this.propertiesColumns.add(new DBColumn("parent_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.propertiesColumns.add(new DBColumn("parent_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.propertiesColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));
		this.propertiesColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.propertiesColumns.add(new DBColumn("value", this.databaseEntry, DBColumnType.TEXT, false));

		this.propertiesPrimaryKeys = new ArrayList<String>();
		this.propertiesPrimaryKeys.add("parent_id");
		this.propertiesPrimaryKeys.add("parent_version");
		this.propertiesPrimaryKeys.add("pos");

		// Features table
		this.featuresColumns = new ArrayList<DBColumn>();
		this.featuresColumns.add(new DBColumn("parent_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.featuresColumns.add(new DBColumn("parent_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.featuresColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));
		this.featuresColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.featuresColumns.add(new DBColumn("value", this.databaseEntry, DBColumnType.TEXT, false));

		this.featuresPrimaryKeys = new ArrayList<String>();
		this.featuresPrimaryKeys.add("parent_id");
		this.featuresPrimaryKeys.add("parent_version");
		this.featuresPrimaryKeys.add("pos");
		
		// Profiles table
		this.profilesColumns = new ArrayList<DBColumn>();
		this.profilesColumns.add(new DBColumn("id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.profilesColumns.add(new DBColumn("version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.profilesColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.profilesColumns.add(new DBColumn("is_specialization", this.databaseEntry, DBColumnType.BOOLEAN, false));
		this.profilesColumns.add(new DBColumn("image_path", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.profilesColumns.add(new DBColumn("concept_type", this.databaseEntry, DBColumnType.OBJECTID, false));
		this.profilesColumns.add(new DBColumn("created_by", this.databaseEntry, DBColumnType.USERNAME, true));
		this.profilesColumns.add(new DBColumn("created_on", this.databaseEntry, DBColumnType.DATETIME, true));
		this.profilesColumns.add(new DBColumn("checkedin_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.profilesColumns.add(new DBColumn("checkedin_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.profilesColumns.add(new DBColumn("deleted_by", this.databaseEntry, DBColumnType.USERNAME, false));
		this.profilesColumns.add(new DBColumn("deleted_on", this.databaseEntry, DBColumnType.DATETIME, false));
		this.profilesColumns.add(new DBColumn("checksum", this.databaseEntry, DBColumnType.OBJECTID, true));

		this.profilesPrimaryKeys = new ArrayList<String>();
		this.profilesPrimaryKeys.add("id");
		this.profilesPrimaryKeys.add("version");
		
		// ProfilesInModel table
		this.profilesInModelColumns = new ArrayList<DBColumn>();
		this.profilesInModelColumns.add(new DBColumn("pim_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.profilesInModelColumns.add(new DBColumn("profile_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.profilesInModelColumns.add(new DBColumn("profile_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.profilesInModelColumns.add(new DBColumn("model_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.profilesInModelColumns.add(new DBColumn("model_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.profilesInModelColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.profilesInModelPrimaryKeys = new ArrayList<String>();
		this.profilesInModelPrimaryKeys.add("pim_id");
		
		// Bendpoints table
		this.bendpointsColumns = new ArrayList<DBColumn>();
		this.bendpointsColumns.add(new DBColumn("parent_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.bendpointsColumns.add(new DBColumn("parent_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.bendpointsColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));
		this.bendpointsColumns.add(new DBColumn("start_x", this.databaseEntry, DBColumnType.INTEGER, true));
		this.bendpointsColumns.add(new DBColumn("start_y", this.databaseEntry, DBColumnType.INTEGER, true));
		this.bendpointsColumns.add(new DBColumn("end_x", this.databaseEntry, DBColumnType.INTEGER, true));
		this.bendpointsColumns.add(new DBColumn("end_y", this.databaseEntry, DBColumnType.INTEGER, true));

		this.bendpointsPrimaryKeys = new ArrayList<String>();
		this.bendpointsPrimaryKeys.add("parent_id");
		this.bendpointsPrimaryKeys.add("parent_version");
		this.bendpointsPrimaryKeys.add("pos");
		
		// Metadata table
		this.metadataColumns = new ArrayList<DBColumn>();
		this.metadataColumns.add(new DBColumn("parent_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.metadataColumns.add(new DBColumn("parent_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.metadataColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));
		this.metadataColumns.add(new DBColumn("name", this.databaseEntry, DBColumnType.OBJ_NAME, false));
		this.metadataColumns.add(new DBColumn("value", this.databaseEntry, DBColumnType.TEXT, false));

		this.metadataPrimaryKeys = new ArrayList<String>();
		this.metadataPrimaryKeys.add("parent_id");
		this.metadataPrimaryKeys.add("parent_version");
		this.metadataPrimaryKeys.add("pos");

		// images table
		this.imagesColumns = new ArrayList<DBColumn>();
		this.imagesColumns.add(new DBColumn("path", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.imagesColumns.add(new DBColumn("image", this.databaseEntry, DBColumnType.IMAGE, true));
		
		// ViewsObjectsInView table
		this.viewsObjectsInViewColumns = new ArrayList<DBColumn>();
		this.viewsObjectsInViewColumns.add(new DBColumn("oiv_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.viewsObjectsInViewColumns.add(new DBColumn("object_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsObjectsInViewColumns.add(new DBColumn("object_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsObjectsInViewColumns.add(new DBColumn("view_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsObjectsInViewColumns.add(new DBColumn("view_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsObjectsInViewColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.viewsObjectsInViewPrimaryKeys = new ArrayList<String>();
		this.viewsObjectsInViewPrimaryKeys.add("oiv_id");
	    
		// ViewsConnectionsInView table
		this.viewsConnectionsInViewColumns = new ArrayList<DBColumn>();
		this.viewsConnectionsInViewColumns.add(new DBColumn("civ_id", this.databaseEntry, DBColumnType.AUTO_INCREMENT, true));
		this.viewsConnectionsInViewColumns.add(new DBColumn("connection_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsConnectionsInViewColumns.add(new DBColumn("connection_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsConnectionsInViewColumns.add(new DBColumn("view_id", this.databaseEntry, DBColumnType.OBJECTID, true));
		this.viewsConnectionsInViewColumns.add(new DBColumn("view_version", this.databaseEntry, DBColumnType.INTEGER, true));
		this.viewsConnectionsInViewColumns.add(new DBColumn("pos", this.databaseEntry, DBColumnType.INTEGER, true));

		this.viewsConnectionsInViewPrimaryKeys = new ArrayList<String>();
		this.viewsConnectionsInViewPrimaryKeys.add("civ_id");

		/* ****************************************************************************************************** */

		this.databaseTables = new ArrayList<DBTable>();
		this.databaseTables.add(new DBTable(this.schema, "database_version", this.databaseVersionColumns, this.databaseVersionPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "models", this.modelsColumns, this.modelsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "folders", this.foldersColumns, this.foldersPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "folders_in_model", this.foldersInModelColumns, this.foldersInModelPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "elements", this.elementsColumns, this.elementsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "elements_in_model", this.elementsInModelColumns, this.elementsInModelPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "relationships", this.relationshipsColumns, this.relationshipsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "relationships_in_model", this.relationshipsInModelColumns, this.relationshipsInModelPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "views", this.viewsColumns, this.viewsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "views_in_model", this.viewsInModelColumns, this.viewsInModelPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "views_objects", this.viewsObjectsColumns, this.viewsObjectsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "views_objects_in_view", this.viewsObjectsInViewColumns, this.viewsObjectsInViewPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "views_connections", this.viewsConnectionsColumns, this.viewsConnectionsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "views_connections_in_view", this.viewsConnectionsInViewColumns, this.viewsConnectionsInViewPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "properties", this.propertiesColumns, this.propertiesPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "features", this.featuresColumns, this.featuresPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "profiles", this.profilesColumns, this.profilesPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "profiles_in_model", this.profilesInModelColumns, this.profilesInModelPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "bendpoints", this.bendpointsColumns, this.bendpointsPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "metadata", this.metadataColumns, this.metadataPrimaryKeys));
		this.databaseTables.add(new DBTable(this.schema, "images", this.imagesColumns, this.imagesPrimaryKeys));
	}
}