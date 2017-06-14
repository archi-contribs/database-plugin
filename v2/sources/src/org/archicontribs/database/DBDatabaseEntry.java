package org.archicontribs.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.jface.preference.IPreferenceStore;


public class DBDatabaseEntry {
	private static final DBLogger logger = new DBLogger(DBDatabaseEntry.class);

	public static final String preferenceName = "databases";

	private String name = "";
	private String driver = "";
	private String server = "";
	private int port = 0;
	private String database = "";
	private String schema = "";
	private String username = "";
	private String password = "";
	private boolean exportWholeModel = false;
	private boolean exportViewImages = false;
	private boolean neo4jNativeMode = false;

	public DBDatabaseEntry() {
	}

	public DBDatabaseEntry(String name, String driver, String server, int port, String database, String schema, String username, String password, boolean exportWholeModel, boolean exportViewImages, boolean neo4jNativeMode) throws Exception {
		setName(name);
		setDriver(driver);
		setServer(server);
		setPort(port);
		setDatabase(database);
		setSchema(schema);
		setUsername(username);
		setPassword(password);

		setExportWholeModel(exportWholeModel);
		setExportViewImages(exportViewImages);

		setNeo4jNativeMode(neo4jNativeMode);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) throws Exception {
		// we ensure that the driver is known
		this.driver = null;
		driver = driver.toLowerCase();		// just in case
		for ( DBDatabase database: DBDatabase.VALUES ) {
			if ( DBPlugin.areEqual(database.getDriverName(), driver) ) {
				this.driver = driver;
				return;
			}
		}

		throw new Exception("Unknown driver "+driver);
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public static int getDefaultPort(String driver) {
		driver = driver.toLowerCase();			// just in case
		for ( DBDatabase database: DBDatabase.VALUES ) {
			if ( DBPlugin.areEqual(database.getDriverName(), driver) ) return database.getDefaultPort();
		}
		return 0;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) throws Exception {
		// we ensure that the port is > 0 and < 65536
		if ( (port < 0) || (port > 65535) )
			throw new Exception("Port should be between 0 and 65535");
		this.port = port;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getSchema() {
		return schema;
	}

	/**
	 * if the schema is set, returns the schema name followed by a dot, directly usable in SQL requests
	 */
	public String getSchemaPrefix() {
		if ( schema.isEmpty() )
			return "";
		return schema + ".";
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean getExportWholeModel() {
		return exportWholeModel;
	}

	public void setExportWholeModel(boolean exportWholeModel) {
		this.exportWholeModel = exportWholeModel;
	}

	public boolean getExportViewsImages()  {
		return exportViewImages;
	}

	public void setExportViewImages(boolean exportViewImages) {
		this.exportViewImages = exportViewImages;
	}

	public boolean getNeo4jNativeMode()  {
		return neo4jNativeMode;
	}

	public void setNeo4jNativeMode(boolean neo4jNativeMode) {
		this.neo4jNativeMode = neo4jNativeMode;
	}

	public String getLanguage() {
		if ( DBPlugin.areEqual(driver, "neo4j") )
			return "CQL";
		else
			return "SQL";
	}

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
						databaseEntry.setDriver("ms-sql");
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
					databaseEntry.setExportWholeModel(store.getBoolean(preferenceName+"_export-whole-model_"+String.valueOf(line)));
					databaseEntry.setExportViewImages(store.getBoolean(preferenceName+"_export-views-images_"+String.valueOf(line)));

					if ( DBPlugin.areEqual(databaseEntry.getDriver(), "neo4j") ) {
						databaseEntry.setNeo4jNativeMode(store.getBoolean(preferenceName+"_neo4j-native-mode_"+String.valueOf(line)));
					}

					databaseEntries.add(databaseEntry);
				} catch (Exception e) {
					DBGui.popup(Level.ERROR, "Failed to get database entry \""+databaseEntry.getName()+"\" from preference store.", e);
				}
			}
		}
		return databaseEntries;
	}

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
			store.setValue(preferenceName+"_export-whole-model_"+String.valueOf(line), databaseEntry.getExportWholeModel());
			store.setValue(preferenceName+"_export-views-images_"+String.valueOf(line), databaseEntry.getExportViewsImages());
			store.setValue(preferenceName+"_neo4j-native-mode_"+String.valueOf(line), databaseEntry.getNeo4jNativeMode());

		}
	}

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
