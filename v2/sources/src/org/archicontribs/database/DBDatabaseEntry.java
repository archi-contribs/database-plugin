package org.archicontribs.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBDatabase;
import org.eclipse.jface.preference.IPreferenceStore;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains all the information required to connect to to a database 
 * 
 * @author Herve Jouin
 */
public class DBDatabaseEntry {
	private static final DBLogger logger = new DBLogger(DBDatabaseEntry.class);

	public static final String preferenceName = "databases";
	
    public DBDatabaseEntry() {
    }

    /**
     * Created a database entry
     * @param entryName : name of the entry (just a bulk of letters, can contain spaces)
     * @param driverName : name of the driver required to connect to the database (must be one of {@link DBDatabase}) 
     * @param serverName : IP address or DNS name of the server where the database stands
     * @param portValue : TCP port on which the database is listening to 
     * @param databaseName : name of the database
     * @param schemaName : name of the schema
     * @param userName : account used to connect to the database (on MS-SQL databases, an empty username and password allows to switch to the Windows Integrated Security) 
     * @param pass : password used to connect to the database
     * @param exportWholeModel : true if the whole model (including folders, views, objects and connections) should be exported, or false if only the elements and relationships must be exported. 
     * @param exportViewImages : true if screenshots of the views must be exported in the database, false if screenshots are not necessary
     * @param isNeo4jNativeMode : true if Archi relationships must be exported as Neo4J relationships (but relationships on relationships is not permitted), or false if Archi relationships are exported as nodes (relationships on relationships are supported, but the Neo4J requests are more complex)
     * @param neo4jEmptyDB : true if the Neo4J database must be emptied before the export, or false if Neo4J database content must be kept
     * @throws Exception
     */
    public DBDatabaseEntry(String entryName, String driverName, String serverName, int portValue, String databaseName, String schemaName, String userName, String pass, boolean exportWholeModel, boolean exportViewImages, boolean isNeo4jNativeMode, boolean neo4jEmptyDB) throws Exception {
        setName(entryName);
        setDriver(driverName);
        setServer(serverName);
        setPort(portValue);
        setDatabase(databaseName);
        setSchema(schemaName);
        setUsername(userName);
        setPassword(pass);

        //TODO: store this parameter once for all in the database as exporting the elements and relationships only in a database where the previous models have got views is equal to removing the views from the model
        setWholeModelExported(exportWholeModel);
        setViewSnapshotRequired(exportViewImages);

        setNeo4jNativeMode(isNeo4jNativeMode);
        setShouldEmptyNeo4jDB(neo4jEmptyDB);
    }

	/**
	 * Name of the database Entry 
	 */
	@Getter @Setter private String name = "";
	
	/**
	 * Driver to use to access the database<br>
	 * <br>
	 * must be one of @DBDatabase.VALUES
	 */
	@Getter private String driver = "";
	
	/**
     * Driver to use to access the database<br>
     * <br>
     * @param driverName must be one of @DBDatabase.VALUES
     */
    public void setDriver(String driverName) throws Exception {
        // we ensure that the driver is known
        this.driver = null;
        for ( DBDatabase db: DBDatabase.VALUES ) {
            if ( DBPlugin.areEqual(db.getDriverName(), driverName) ) {
                this.driver = driverName.toLowerCase();
                return;
            }
        }
        throw new Exception("Unknown driver "+driverName);
    }
	
	/**
	 * Name or IP address of the server hosting the database
	 */
	@Getter @Setter private String server = "";
	
	/**
	 * TCP port on which the database listens to<br>
	 * <br>
	 * port should be between 0 and 65535 
	 */
	@Getter private int port = 0;
	
	/**
     * TCP port on which the database listens to<br>
     * <br>
     * @value port should be between 0 and 65535 
     */
    public void setPort(int portValue) throws Exception {
        // we ensure that the port is > 0 and < 65536
        if ( (portValue < 0) || (portValue > 65535) )
            throw new Exception("Port should be between 0 and 65535");
        this.port = portValue;
    }
	
	/**
	 * Database name
	 */
	@Getter @Setter private String database = "";
	
	/**
	 * Schema name
	 */
	@Getter @Setter private String schema = "";
	
	/**
	 * Usename used to connect to the database
	 */
	@Getter @Setter private String username = "";
	
	/**
	 * Password used to connect to the database
	 */
	@Getter @Setter private String password = "";
	
	/**
	 * Should we export the whole model
	 */
	@Getter @Setter private boolean wholeModelExported = false;
	
	/**
	 * Should we export snapshots of the views
	 */
	@Getter @Setter private boolean viewSnapshotRequired = false;
	
	/**
	 * Border width to add around view snapshots
	 */
	@Getter @Setter private int viewsImagesBorderWidth = 10;
	
	/**
	 * Scale factor of view snapshots
	 */
	@Getter @Setter private int viewsImagesScaleFactor = 100;
	
	/**
	 * In case of Neo4J database, should we generate native relationships
	 */
	@Getter  @Setter private boolean neo4jNativeMode = false;
	
	/**
	 * In case of Neo4J database, should we empty the database before export
	 */
	@Setter private boolean shouldEmptyNeo4jDB = false;
	/**
     * In case of Neo4J database, should we empty the database before export
     */
	public boolean shouldEmptyNeo4jDB() {
	    return this.shouldEmptyNeo4jDB;
	}
	
	/**
	 * Should we use typed relationships in Neo4j databases
	 */
	@Getter @Setter private boolean neo4jTypedRelationship = false;


	/**
	 * @param driver
	 * @return the default TCP port for a given driver 
	 */
	public static int getDefaultPort(String driver) {
		for ( DBDatabase database: DBDatabase.VALUES ) {
			if ( DBPlugin.areEqual(database.getDriverName(), driver.toLowerCase()) ) return database.getDefaultPort();
		}
		return 0;
	}

	/**
	 * if the schema is set, returns the schema name followed by a dot, directly usable in SQL requests
	 */
	public String getSchemaPrefix() {
		if ( this.schema.isEmpty() )
			return "";
		return this.schema + ".";
	}
	
	/**
	 * @return CQL if Neo4j database selected, or SQL in all other cases
	 */
	public String getLanguage() {
		if ( DBPlugin.areEqual(this.driver, "neo4j") )
			return "CQL";
		return "SQL";
	}

	/**
	 * Gets all the database entries from the preference store
	 * 
	 * @param includeNeo4j : True if the Neo4J database must be included, false if the Neo4J databases must me excluded
	 * @return List of the database entries
	 */
	public static List<DBDatabaseEntry> getAllDatabasesFromPreferenceStore(boolean includeNeo4j) {
		if ( logger.isDebugEnabled() ) logger.debug("Getting databases preferences from preference store");
		List<DBDatabaseEntry> databaseEntries = new ArrayList<DBDatabaseEntry>();     
		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		int lines = store.getInt(preferenceName);

		databaseEntries.clear();
		for (int line = 0; line <lines; line++) {

			if ( includeNeo4j || !DBPlugin.areEqual(store.getString(preferenceName+"_driver_"+String.valueOf(line)).toLowerCase(), "neo4j") ) {
				DBDatabaseEntry databaseEntry = new DBDatabaseEntry();

				databaseEntry.setName(store.getString(preferenceName+"_name_"+String.valueOf(line)));

				try {
					// we have to manage the old name of MS SQL driver :-(
					if ( store.getString(preferenceName+"_driver_"+String.valueOf(line)).equalsIgnoreCase("mssql") )
						databaseEntry.setDriver(DBDatabase.MSSQL.getDriverName());
					else
						databaseEntry.setDriver(store.getString(preferenceName+"_driver_"+String.valueOf(line)));
					
					databaseEntry.setServer(store.getString(preferenceName+"_server_"+String.valueOf(line)));

					if ( !DBPlugin.areEqual(databaseEntry.getDriver(), "sqlite") ) {
						databaseEntry.setPort(store.getInt(preferenceName+"_port_"+String.valueOf(line)));
						databaseEntry.setDatabase(store.getString(preferenceName+"_database_"+String.valueOf(line)));
						if ( !DBPlugin.areEqual(databaseEntry.getDriver(), "neo4j") ) {
							databaseEntry.setSchema(store.getString(preferenceName+"_schema_"+String.valueOf(line)));
						}
						databaseEntry.setUsername(store.getString(preferenceName+"_username_"+String.valueOf(line)));
						databaseEntry.setPassword(store.getString(preferenceName+"_password_"+String.valueOf(line)));
					}
					databaseEntry.setWholeModelExported(store.getBoolean(preferenceName+"_export-whole-model_"+String.valueOf(line)));
					
					databaseEntry.setViewSnapshotRequired(store.getBoolean(preferenceName+"_export-views-images_"+String.valueOf(line)));
					store.setDefault(preferenceName+"_views-images-border-width_"+String.valueOf(line), 10);
					databaseEntry.setViewsImagesBorderWidth(store.getInt(preferenceName+"_views-images-border-width_"+String.valueOf(line)));
					store.setDefault(preferenceName+"_views-images-scale-factor_"+String.valueOf(line), 100);
					databaseEntry.setViewsImagesScaleFactor(store.getInt(preferenceName+"_views-images-scale-factor_"+String.valueOf(line)));
					
					databaseEntry.setNeo4jNativeMode(store.getBoolean(preferenceName+"_neo4j-native-mode_"+String.valueOf(line)));
					databaseEntry.setShouldEmptyNeo4jDB(store.getBoolean(preferenceName+"_neo4j-empty-database_"+String.valueOf(line)));
					databaseEntry.setNeo4jTypedRelationship(store.getBoolean(preferenceName+"_neo4j-typed-relationships_"+String.valueOf(line)));

					databaseEntries.add(databaseEntry);
				} catch (Exception e) {
					DBGui.popup(Level.ERROR, "Failed to get database entry \""+databaseEntry.getName()+"\" from preference store.", e);
				}
			}
		}
		return databaseEntries;
	}

	/**
	 * Persist the database entries in the preference store
	 * 
	 * @param databaseEntries : List of the database entries to persist in the preference store
	 */
	public static void setAllIntoPreferenceStore(List<DBDatabaseEntry> databaseEntries) {
		if ( logger.isDebugEnabled() ) logger.debug("Recording databases in preference store");

		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		int lines = databaseEntries.size();
		store.setValue(preferenceName, lines);

		for (int line = 0; line < lines; line++) {
			DBDatabaseEntry databaseEntry = databaseEntries.get(line);
			store.setValue(preferenceName+"_name_"+String.valueOf(line), databaseEntry.getName());
			store.setValue(preferenceName+"_driver_"+String.valueOf(line), databaseEntry.getDriver());
			store.setValue(preferenceName+"_server_"+String.valueOf(line), databaseEntry.getServer());
			store.setValue(preferenceName+"_port_"+String.valueOf(line), databaseEntry.getPort());
			store.setValue(preferenceName+"_database_"+String.valueOf(line), databaseEntry.getDatabase());
			store.setValue(preferenceName+"_schema_"+String.valueOf(line), databaseEntry.getSchema());
			store.setValue(preferenceName+"_username_"+String.valueOf(line), databaseEntry.getUsername());
			store.setValue(preferenceName+"_password_"+String.valueOf(line), databaseEntry.getPassword());
			store.setValue(preferenceName+"_export-whole-model_"+String.valueOf(line), databaseEntry.isWholeModelExported());
			store.setValue(preferenceName+"_export-views-images_"+String.valueOf(line), databaseEntry.isViewSnapshotRequired());
			store.setValue(preferenceName+"_views-images-border-width_"+String.valueOf(line), databaseEntry.getViewsImagesBorderWidth());
			store.setValue(preferenceName+"_views-images-scale-factor_"+String.valueOf(line), databaseEntry.getViewsImagesScaleFactor());
			store.setValue(preferenceName+"_neo4j-native-mode_"+String.valueOf(line), databaseEntry.isNeo4jNativeMode());
			store.setValue(preferenceName+"_neo4j-empty-database_"+String.valueOf(line), databaseEntry.shouldEmptyNeo4jDB());
			store.setValue(preferenceName+"_neo4j-typed-relationships_"+String.valueOf(line), databaseEntry.isNeo4jTypedRelationship());
		}
	}

	/**
	 * Get a database entry from the database name
	 * 
	 * @param databaseName : database name of the database entry
	 * @return The database entry corresponding to the database name
	 * @throws Exception
	 */
	public static DBDatabaseEntry getDBDatabaseEntry(String databaseName) throws Exception {
		List<DBDatabaseEntry> databaseEntries = getAllDatabasesFromPreferenceStore(true);

		for (DBDatabaseEntry databaseEntry : databaseEntries) {
			if ( DBPlugin.areEqual(databaseEntry.getName(), databaseName) ) {
				return databaseEntry;
			}
		}
		return null;
	}
}
