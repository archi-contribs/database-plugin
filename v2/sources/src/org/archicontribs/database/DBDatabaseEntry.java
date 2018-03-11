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
	private int viewsImagesBorderWidth = 10;
	private int viewsImagesScaleFactor = 100;
	private boolean neo4jNativeMode = false;
	private boolean neo4jEmptyDB = false;
	private boolean neo4jTypedRelationships = false;
	private boolean collaborativeMode = false;

	public DBDatabaseEntry() {
	}

	public DBDatabaseEntry(String name, String driver, String server, int port, String database, String schema, String username, String password, boolean exportWholeModel, boolean exportViewImages, boolean neo4jNativeMode, boolean neo4jEmptyDB, boolean collaborativeMode) throws Exception {
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
		setNeo4jEmptyDB(neo4jEmptyDB);
		
		setCollaborativeMode(collaborativeMode);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDriver() {
		return this.driver;
	}

	public void setDriver(String driver) throws Exception {
		// we ensure that the driver is known
		this.driver = null;
		for ( DBDatabase db: DBDatabase.VALUES ) {
			if ( DBPlugin.areEqual(db.getDriverName(), driver) ) {
				this.driver = driver.toLowerCase();
				return;
			}
		}

		throw new Exception("Unknown driver "+driver);
	}

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public static int getDefaultPort(String driver) {
		for ( DBDatabase database: DBDatabase.VALUES ) {
			if ( DBPlugin.areEqual(database.getDriverName(), driver.toLowerCase()) ) return database.getDefaultPort();
		}
		return 0;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) throws Exception {
		// we ensure that the port is > 0 and < 65536
		if ( (port < 0) || (port > 65535) )
			throw new Exception("Port should be between 0 and 65535");
		this.port = port;
	}

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getSchema() {
		return this.schema;
	}

	/**
	 * if the schema is set, returns the schema name followed by a dot, directly usable in SQL requests
	 */
	public String getSchemaPrefix() {
		if ( this.schema.isEmpty() )
			return "";
		return this.schema + ".";
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean getExportWholeModel() {
		return this.exportWholeModel;
	}

	public void setExportWholeModel(boolean exportWholeModel) {
		this.exportWholeModel = exportWholeModel;
	}

	public boolean getExportViewsImages()  {
		return this.exportViewImages;
	}

	public void setExportViewImages(boolean exportViewImages) {
		this.exportViewImages = exportViewImages;
	}
	
	public int getViewsImagesBorderWidth() {
		return this.viewsImagesBorderWidth;
	}
	
	public void setViewsImagesBorderWidth(int viewsImagesBorderWidth) {
		this.viewsImagesBorderWidth = viewsImagesBorderWidth;
	}
	
	public int getViewsImagesScaleFactor() {
		return this.viewsImagesScaleFactor;
	}
	
	public void setViewsImagesScaleFactor(int viewsImagesScaleFactor) {
		this.viewsImagesScaleFactor = viewsImagesScaleFactor;
	}
	
	public boolean getNeo4jNativeMode()  {
		return this.neo4jNativeMode;
	}

	public void setNeo4jNativeMode(boolean neo4jNativeMode) {
		this.neo4jNativeMode = neo4jNativeMode;
	}

	public boolean getNeo4jEmptyDB()  {
		return this.neo4jEmptyDB;
	}

	public void setNeo4jEmptyDB(boolean neo4jEmptyDB) {
		this.neo4jEmptyDB = neo4jEmptyDB;
	}
	
	public boolean getNeo4jTypedRelationships()  {
		return this.neo4jTypedRelationships;
	}

	public void setNeo4jTypedRelationships(boolean neo4jTypedRelationships) {
		this.neo4jTypedRelationships = neo4jTypedRelationships;
	}
	
	public boolean getCollaborativeMode()  {
		return this.collaborativeMode;
	}

	public void setCollaborativeMode(boolean collaborativeMode) {
		this.collaborativeMode = collaborativeMode;
	}
	
	public String getLanguage() {
		if ( DBPlugin.areEqual(this.driver, "neo4j") )
			return "CQL";
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
					store.setDefault(preferenceName+"_views-images-border-width_"+String.valueOf(line), 10);
					databaseEntry.setViewsImagesBorderWidth(store.getInt(preferenceName+"_views-images-border-width_"+String.valueOf(line)));
					store.setDefault(preferenceName+"_views-images-scale-factor_"+String.valueOf(line), 100);
					databaseEntry.setViewsImagesScaleFactor(store.getInt(preferenceName+"_views-images-scale-factor_"+String.valueOf(line)));
					
					databaseEntry.setNeo4jNativeMode(store.getBoolean(preferenceName+"_neo4j-native-mode_"+String.valueOf(line)));
					databaseEntry.setNeo4jEmptyDB(store.getBoolean(preferenceName+"_neo4j-empty-database_"+String.valueOf(line)));
					databaseEntry.setNeo4jTypedRelationships(store.getBoolean(preferenceName+"_neo4j-typed-relationships_"+String.valueOf(line)));
					
					databaseEntry.setCollaborativeMode(store.getBoolean(preferenceName+"_collaborative-mode_"+String.valueOf(line)));

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
			store.setValue(preferenceName+"_views-images-border-width_"+String.valueOf(line), databaseEntry.getViewsImagesBorderWidth());
			store.setValue(preferenceName+"_views-images-scale-factor_"+String.valueOf(line), databaseEntry.getViewsImagesScaleFactor());
			store.setValue(preferenceName+"_neo4j-native-mode_"+String.valueOf(line), databaseEntry.getNeo4jNativeMode());
			store.setValue(preferenceName+"_neo4j-empty-database_"+String.valueOf(line), databaseEntry.getNeo4jEmptyDB());
			store.setValue(preferenceName+"_neo4j-typed-relationships_"+String.valueOf(line), databaseEntry.getNeo4jTypedRelationships());
			store.setValue(preferenceName+"_collaborative-mode_"+String.valueOf(line), databaseEntry.getCollaborativeMode());
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
