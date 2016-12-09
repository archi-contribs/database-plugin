package org.archicontribs.database;

import java.util.ArrayList;
import java.util.List;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class holds the information required to connect to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseEntry {
	public static String preferenceName = "databases";
	public static String[] properties = {"name", "driver", "server", "port", "database", "username", "password"}; 
	
	private String name;
	private String driver;
	private String server;
	private String port;
	private String database;
	private String username;
	private String password;
	
	public DBDatabaseEntry() {
		DBPlugin.debug(DebugLevel.MainMethod, "new DBDatabaseEntry.DBDatabaseEntry()");
		this.name = "";
		this.driver = "";
		this.server = "";
		this.port = "";
		this.database = "";
		this.username = "";
		this.password = "";
	}
	
	public DBDatabaseEntry(String name) {
		DBPlugin.debug(DebugLevel.MainMethod, "new DBDatabaseEntry.DBDatabaseEntry(\""+name+"\")");
		this.name = name;
		this.driver = "";
		this.server = "";
		this.port = "";
		this.database = "";
		this.username = "";
		this.password = "";
	}
	
	public DBDatabaseEntry(String name, String driver, String server, String port, String database, String username, String password) {
		DBPlugin.debug(DebugLevel.MainMethod, "new DBDatabaseEntry.DBDatabaseEntry(\""+name+"\",\""+driver+"\",\""+server+"\",\""+port+"\",\""+database+"\",\""+username+"\",\""+password+"\")");
		this.name = name;
		this.driver = driver;
		this.server = server;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
	}
	
	public DBDatabaseEntry(DBDatabaseEntry entry) {
		DBPlugin.debug(DebugLevel.MainMethod, "new DBDatabaseEntry.DBDatabaseEntry(databaseEntry={\""+name+"\",\""+driver+"\",\" "+server+"\",\""+port+"\",\" "+database+"\",\""+username+"\",\""+password+"\")");
		this.name = entry.name;
		this.driver = entry.driver;
		this.server = entry.server;
		this.port = entry.port;
		this.database = entry.database;
		this.username = entry.username;
		this.password = entry.password;
	}
	
	public String getName() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getName() -> \""+name+"\"");
		return name;
	}

	public void setName(String name) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setName(\""+name+"\")");
		this.name = name;
	}

	public String getDriver() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getDriver() -> \""+driver+"\"");
		return driver;
	}

	public void setDriver(String driver) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setDriver(\""+driver+"\")");
		this.driver = driver;
	}

	public String getServer() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getServer() -> \""+server+"\"");
		return server;
	}

	public void setServer(String server) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setServer(\""+server+"\")");
		this.server = server;
	}

	public String getPort() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getPort() -> \""+port+"\"");
		return port;
	}

	public void setPort(String port) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setPort(\""+port+"\")");
		this.port = port;
	}

	public String getDatabase() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getDatabase() -> \""+database+"\"");
		return database;
	}

	public void setDatabase(String database) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setDatabase(\""+database+"\")");
		this.database = database;
	}

	public String getUsername() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getUsername() -> \""+username+"\"");
		return username;
	}

	public void setUsername(String username) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setUsername(\""+username+"\")");
		this.username = username;
	}

	public String getPassword() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getPassword() -> \""+password+"\"");
		return password;
	}

	public void setPassword(String password) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setPassword(\""+password+"\")");
		this.password = password;
	}
	
	public String getProperty(String propName) {
		String value = null;
		switch (propName.toLowerCase()) {
		case "name"     : value = name; break;
		case "driver"   : value = driver; break;
		case "server"   : value = server; break;
		case "port"     : value = port; break;
		case "database" : value = database; break;
		case "username" : value = username; break;
		case "password" : value = password; break;
		}
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.getProperty(\""+propName+"\") -> \""+value+"\"");
		return value;
	}
	
	public void setProperty(String propName, String propValue) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntry.setProperty(\""+propName+"\",\""+propValue+"\")");
		switch (propName.toLowerCase()) {
		case "name"     : this.name = propValue; break;
		case "driver"   : this.driver = propValue; break;
		case "server"   : this.server = propValue; break;
		case "port"     : this.port = propValue; break;
		case "database" : this.database = propValue; break;
		case "username" : this.username = propValue; break;
		case "password" : this.password = propValue; break;
		}
	}

	public static List<DBDatabaseEntry> getAllFromPreferenceStore() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBDatabaseEntry.getAllFromPreferenceStore()");
		
		List<DBDatabaseEntry> databaseEntries = new ArrayList<DBDatabaseEntry>();		
		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		int lines =	store.getInt(preferenceName);
		DBPlugin.debug(DebugLevel.Variable, "Loading "+lines+" databases from store");
		
		databaseEntries.clear();
		for (int line = 0; line <lines; line++) {
			DBDatabaseEntry entry = new DBDatabaseEntry();
			for (String prop: properties) {
				entry.setProperty(prop, store.getString(preferenceName+"_"+prop+"_"+String.valueOf(line)));
			}
			databaseEntries.add(entry);
		}
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBDatabaseEntry.getAllFromPreferenceStore()");
		return databaseEntries;
	}
	
	public static void setAllIntoPreferenceStore(List<DBDatabaseEntry> databaseEntries) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBDatabaseEntry.setAllIntoPreferenceStore("+String.valueOf(databaseEntries.size())+" databaseEntries)");
		
		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		int lines =	databaseEntries.size();
		store.setValue(preferenceName, lines);
		DBPlugin.debug(DebugLevel.Variable, "Saving "+lines+" databases into store");

		DBPlugin.debug(DebugLevel.Variable, "-----");
		for (int line = 0; line < lines; line++) {
			for (String prop: properties) {
				store.setValue(preferenceName+"_"+prop+"_"+String.valueOf(line), databaseEntries.get(line).getProperty(prop));
			}
			if ( line < lines )
				DBPlugin.debug(DebugLevel.Variable, "-----");
		}
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBDatabaseEntry.setAllIntoPreferenceStore("+String.valueOf(databaseEntries.size())+" databaseEntries)");
	}
}