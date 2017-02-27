package org.archicontribs.database;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.Folder;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
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
public class DBDatabase {
	private static final DBLogger logger = new DBLogger(DBDatabase.class);

	public static final String preferenceName = "databases";
	public static final String[] properties = {"name", "driver", "server", "port", "database", "username", "password"};
	public static final String[] driverList = {"Neo4j", "MsSQL", "MySQL", "Oracle", "PostGreSQL", "SQLite"};
	public static final String[] defaultPorts = {"7687", "1433", "3306", "1521", "5432", ""};

	private static enum COMPARE {NEW, IDENTICAL, UPDATED};

	private String name;
	private String driver;
	private String server;
	private String port;
	private String database;
	private String username;
	private String password;

	/**
	 * Connection to the database
	 */
	private Connection connection;

	/**
	 * ResultSet of the current transaction (used by import process to allow the loop to be managed outside the DBdatabase class)
	 */
	private ResultSet currentResultSet = null;

	public DBDatabase() {
		this.name = "";
		this.driver = "";
		this.server = "";
		this.port = "";
		this.database = "";
		this.username = "";
		this.password = "";
		this.connection = null;
	}

	public DBDatabase(String name) {
		this.name = name;
		this.driver = "";
		this.server = "";
		this.port = "";
		this.database = "";
		this.username = "";
		this.password = "";
		this.connection = null;
	}

	public DBDatabase(String name, String driver, String server, String port, String database, String username, String password) {
		this.name = name;
		this.driver = driver.toLowerCase();
		this.server = server;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		this.connection = null;
	}

	public DBDatabase(DBDatabase entry) {
		this.name = entry.name;
		this.driver = entry.driver.toLowerCase();
		this.server = entry.server;
		this.port = entry.port;
		this.database = entry.database;
		this.username = entry.username;
		this.password = entry.password;
		this.connection = null;
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

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
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

	public String getLanguage() {
		if ( driver.equals("neo4j") )
			return "CQL";
		else
			return "SQL";
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
		return value;
	}

	public void setProperty(String propName, String propValue) {
		switch (propName.toLowerCase()) {
		case "name"     : this.name = propValue; break;
		case "driver"   : this.driver = propValue.toLowerCase(); break;
		case "server"   : this.server = propValue; break;
		case "port"     : this.port = propValue; break;
		case "database" : this.database = propValue; break;
		case "username" : this.username = propValue; break;
		case "password" : this.password = propValue; break;
		}
	}

	public static List<DBDatabase> getAllDatabasesFromPreferenceStore() {
		if ( logger.isDebugEnabled() ) logger.debug("Getting databases preferences from preference store");
		List<DBDatabase> databaseEntries = new ArrayList<DBDatabase>();		
		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		int lines =	store.getInt(preferenceName);

		databaseEntries.clear();
		for (int line = 0; line <lines; line++) {
			DBDatabase entry = new DBDatabase();
			for (String prop: properties) {
				entry.setProperty(prop, store.getString(preferenceName+"_"+prop+"_"+String.valueOf(line)));
			}
			databaseEntries.add(entry);
		}
		return databaseEntries;
	}

	public static void setAllIntoPreferenceStore(List<DBDatabase> databaseEntries) {
		if ( logger.isDebugEnabled() ) logger.debug("Recording databases in preference store");

		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		int lines =	databaseEntries.size();
		store.setValue(preferenceName, lines);

		for (int line = 0; line < lines; line++) {
			for (String prop: properties) {
				store.setValue(preferenceName+"_"+prop+"_"+String.valueOf(line), databaseEntries.get(line).getProperty(prop));
			}
		}
	}

	public static DBDatabase getDBDatabase(String databaseName) {
		List<DBDatabase> databaseEntries = DBDatabase.getAllDatabasesFromPreferenceStore();

		for (DBDatabase databaseEntry : databaseEntries) {
			if ( databaseEntry.getName().equals(databaseName) ) {
				return databaseEntry;
			}
		}
		return null;
	}

	/**
	 * Opens a connection to a JDBC database using all the connection details
	 */
	public void openConnection() throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Opening connection to database "+name+" : driver="+driver+", server="+server+", port="+port+", database="+database+", username="+username+", password="+password);

		if ( connection != null ) {
			closeConnection();
		}

		String clazz = null;
		String connectionString = null;

		switch (driver) {
		case "postgresql" :
			clazz = "org.postgresql.Driver";
			connectionString = "jdbc:postgresql://" + server + ":" + port + "/" + database;
			break;
		case "mssql"      :
			clazz = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
			connectionString = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database;
			break;
		case "mysql"      :
			clazz = "com.mysql.jdbc.Driver";
			connectionString = "jdbc:mysql://" + server + ":" + port + "/" + database;
			break;
		case "neo4j"      :
			clazz = "org.neo4j.jdbc.Driver";
			connectionString = "jdbc:neo4j:bolt://" + server + ":" + port;
			break;
		case "oracle"     :
			clazz = "oracle.jdbc.driver.OracleDriver";
			connectionString = "jdbc:oracle:thin:@" + server + ":" + port+ ":" + database;
			break;
		case "sqlite"     :
			clazz = "org.sqlite.JDBC";
			connectionString = "jdbc:sqlite:"+server;
			break;
		default :
			throw new Exception("Unknonwn driver "+driver);
		}

		Class.forName(clazz);
		if ( logger.isTraceEnabled() ) logger.trace("JDBC connection string = "+connectionString);
		connection = DriverManager.getConnection(connectionString, username, password);
	}

	/**
	 * Closes connection to the database
	 */
	public void closeConnection() throws Exception {
		if ( connection != null ) {
			if ( logger.isDebugEnabled() ) logger.debug("Closing connection to the database");
			connection.close();
			connection = null;
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("connection to the databse is already closed.");
		}
	}

	public boolean isConnected() {
		return connection != null;
	}

	/**
	 * Checks the database structure 
	 */
	public void check() throws Exception {
		boolean wasConnected = isConnected();

		if ( !wasConnected )
			openConnection();

		if ( logger.isDebugEnabled() ) logger.debug("Checking database");
		//TODO : check database structure
		//TODO : throw exception in case of something wrong

		if ( !wasConnected ) {
			connection.close();
			connection = null;
		}
	}

	/**
	 * Add the parameters to the PreparedStatement and return a string with the complete request
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	private final <T> void constructStatement(PreparedStatement pstmt, String request, T... parameters) throws Exception {
		StringBuilder debugRequest = new StringBuilder();
		String[] splittedRequest = request.split("\\?");
		
		int requestRank = 0;
		int parameterRank = 0;
		while (requestRank < parameters.length) {
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
					if ( logger.isTraceEnabled() ) debugRequest.append("[image as stream]");
					pstmt.setBinaryStream(++requestRank, new ByteArrayInputStream((byte[])parameters[parameterRank]), ((byte[])parameters[parameterRank]).length);
				} catch (Exception err) {
					if ( logger.isTraceEnabled() ) debugRequest.append("[image as base64 string]");
					pstmt.setString(++requestRank, Base64.getEncoder().encodeToString((byte[])parameters[parameterRank]));
				}

			} else {
				if ( logger.isTraceEnabled() ) logger.trace(request);
				throw new Exception("Unknown "+parameters[parameterRank].getClass().getSimpleName()+" parameter in SQL select.");
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
	 * Wrapper to generate and execute a SELECT request in the database<br>
	 * One may use '?' in the request and provide the corresponding values as parameters (at the moment, only strings are accepted)<br>
	 * The connection to the database should already exist 
	 * @return the resultset with the data read from the database
	 */
	@SafeVarargs
	public final <T> ResultSet select(String request, T... parameters) throws Exception {
		assert (connection != null);

		PreparedStatement pstmt;
		try {
			pstmt = connection.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		} catch (SQLException err) {
			// in case of an SQLException, we log the raw request to ease the debug process
			if ( logger.isTraceEnabled() ) logger.trace("SQL Exception for database request : "+request);
			throw err;
		}
		//TODO : améliorer les requêtes les stockant dans une hashtable pour conserver les preparestatement
		constructStatement(pstmt, request, parameters);
		pstmt.closeOnCompletion();

		return pstmt.executeQuery();
	}

	/**
	 * wrapper to generate and execute a INSERT request in the database
	 * One may just provide the column names and the corresponding values as parameters
	 * the wrapper automatically generates the VALUES part of the request 
	 * @return The number of lines inserted in the table
	 */
	@SafeVarargs
	public final <T> int insert(String request, T...parameters) throws Exception {
		assert (connection != null);

		StringBuilder fullRequest = new StringBuilder(request);
		for (int rank=0 ; rank < parameters.length ; rank++) {
			fullRequest.append(rank == 0 ? " VALUES (?" : ", ?");
		}
		fullRequest.append(")");

		return request(fullRequest.toString(), parameters);
	}

	/**
	 * wrapper to execute an INSERT or UPDATE request in the database
	 * One may use '?' in the request and provide the corresponding values as parameters (strings, integers, booleans and byte[] are accepted)
	 * @return the number of lines impacted by the request
	 */
	@SafeVarargs
	public final <T> int request(String request, T... parameters) throws Exception {
		assert (connection != null);

		PreparedStatement pstmt = connection.prepareStatement(request);
		constructStatement(pstmt, request, parameters);

		int rowCount = pstmt.executeUpdate();
		pstmt.close();

		return rowCount;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Setting database auto commit to "+String.valueOf(autoCommit));
		connection.setAutoCommit(autoCommit);
	}

	public void commit() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Commiting database transaction.");
		connection.commit();
	}

	public void rollback() throws SQLException {
		if ( logger.isDebugEnabled() ) logger.debug("Rollbacking database transaction.");
		connection.rollback();
	}

	public void close() throws SQLException {
			// we free up a bit the memory
		allImagePaths = null;
		
		if ( connection == null || connection.isClosed() == true ) {
			if ( logger.isDebugEnabled() ) logger.debug("The database connection is already closed.");
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Closing database connection.");
			connection.close();
		}
	}

	/**
	 * Called when the user selects a line in the tblListConflicts or tblVersions table<br>
	 * The method fills in the tblContent with the values from the component in the model and the required version of the component in the database 
	 */
	public void fillInCompareTable(Table table, IArchimateConcept component, String version) {
		TableItem tableItem;
		ResultSet result;

		try {
			if ( version == null ) {
				if ( component instanceof IArchimateElement ) 
					result = select("SELECT max(version) AS version, class, name, documentation, type, created_by, created_on, checksum FROM elements WHERE id = ? GROUP BY id HAVING MAX(version) is not null", component.getId());
				else
					result = select("SELECT max(version) AS version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM relationships WHERE id = ? GROUP BY id HAVING MAX(version) is not null", component.getId());
			} else {
				if ( component instanceof IArchimateElement ) 
					result = select("SELECT class, name, documentation, type, created_by, created_on, checksum FROM elements WHERE id = ? AND version = ?", component.getId(), version);
				else
					result = select("SELECT class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum FROM relationships WHERE id = ? AND version = ?", component.getId(), version);
			}

			if ( !result.next() ) {
				DBGui.popup(Level.ERROR, "Cannot find "+(version==null?"latest version":"version \""+version+"\"")+" of component with id \""+component.getId()+"\" in the database.");
				return;
			}

			table.removeAll();
			if ( version == null ) {
				version = result.getString("version");
			}

			if ( ((IDBMetadata)component).getDBMetadata().getCurrentVersion() != 0 ) {
				tableItem = new TableItem(table, SWT.NULL);
				tableItem.setText(0, "Version");
				tableItem.setText(1, String.valueOf(((IDBMetadata)component).getDBMetadata().getCurrentVersion()));
				tableItem.setText(2, version);
			}

			if ( ((IDBMetadata)component).getDBMetadata().getDatabaseCreatedBy() != null ) {
				tableItem = new TableItem(table, SWT.NULL);
				tableItem.setText(0, "Created by");
				tableItem.setText(1, ((IDBMetadata)component).getDBMetadata().getDatabaseCreatedBy());
				tableItem.setText(2, result.getString("created_by"));
			}

			if ( ((IDBMetadata)component).getDBMetadata().getDatabaseCreatedOn() != null ) {
				tableItem = new TableItem(table, SWT.NULL);
				tableItem.setText(0, "Created on");
				tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(((IDBMetadata)component).getDBMetadata().getDatabaseCreatedOn().getTime()));
				tableItem.setText(2, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(result.getTimestamp("created_on")));
			}

			tableItem = new TableItem(table, SWT.NULL);
			tableItem.setText(0, "Class");
			tableItem.setText(1, component.getClass().getSimpleName());
			tableItem.setText(2, result.getString("class"));
			if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
				tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
			}

			tableItem = new TableItem(table, SWT.NULL);
			tableItem.setText(0, "Name");
			tableItem.setText(1, component.getName());
			tableItem.setText(2, result.getString("name"));
			if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
				tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
			}

			if ( component instanceof IJunction ) {
				tableItem = new TableItem(table, SWT.NULL);
				tableItem.setText(0, "Type");
				tableItem.setText(1, ((IJunction)component).getType());
				tableItem.setText(2, result.getString("type"));
				if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
					tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
				}
			}

			//TODO : show multiline : http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet231.java
			//TODO : show multiline : http://www.java2s.com/Tutorial/Java/0280__SWT/MultilineTablecell.htm
			tableItem = new TableItem(table, SWT.NULL);
			tableItem.setText(0, "Documentation");
			tableItem.setText(1, component.getDocumentation());
			tableItem.setText(2, result.getString("documentation"));
			if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
				tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
			}

			if ( component instanceof IArchimateRelationship ) {
				tableItem = new TableItem(table, SWT.NULL);
				tableItem.setText(0, "Source ID");
				tableItem.setText(1, ((IArchimateRelationship)component).getSource().getId());
				tableItem.setText(2, result.getString("source_id"));
				if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
					tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
				}

				tableItem = new TableItem(table, SWT.NULL);
				tableItem.setText(0, "Target ID");
				tableItem.setText(1, ((IArchimateRelationship)component).getTarget().getId());
				tableItem.setText(2, result.getString("target_id"));
				if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
					tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
				}

				if ( component instanceof IInfluenceRelationship ) {
					tableItem = new TableItem(table, SWT.NULL);
					tableItem.setText(0, "Strength");
					tableItem.setText(1, ((IInfluenceRelationship)component).getStrength());
					tableItem.setText(2, result.getString("strength"));
					if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
						tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
					}
				}

				if ( component instanceof IAccessRelationship ) {
					tableItem = new TableItem(table, SWT.NULL);
					tableItem.setText(0, "Access type");
					tableItem.setText(1, String.valueOf(((IAccessRelationship)component).getAccessType()));
					tableItem.setText(2, result.getString("access_type"));
					if ( !tableItem.getText(1).equals(tableItem.getText(2)) ) {
						tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
					}
				}
			}

			result.close();

			String[][] componentProperties = new String[component.getProperties().size()][2];
			for (int i = 0; i < component.getProperties().size(); ++i) {
				componentProperties[i] = new String[] { component.getProperties().get(i).getKey(), component.getProperties().get(i).getValue() };
			}
			Arrays.sort(componentProperties, new Comparator<String[]>() { public int compare(final String[] row1, final String[] row2) { return DBPlugin.collator.compare(row1[0],row2[0]); } });

			result = select("SELECT count(*) as count_properties FROM properties WHERE parent_id = ? AND parent_version = ?"
					,component.getId()
					,version				// ,((IDBMetadata)component).getDBMetadata().getDatabaseVersion()
					);

			result.next();

			String[][] databaseProperties = new String[result.getInt("count_properties")][2];
			result.close();

			result = select("SELECT name, value FROM properties WHERE parent_id = ? AND parent_version = ?"
					,component.getId()
					,version				// ,((IDBMetadata)component).getDBMetadata().getDatabaseVersion()
					);
			int j = 0;
			while ( result.next() ) {
				databaseProperties[j++] = new String[] { result.getString("name"), result.getString("value") };
			}
			Arrays.sort(databaseProperties, new Comparator<String[]>() { public int compare(final String[] row1, final String[] row2) { return DBPlugin.collator.compare(row1[0],row2[0]); } });
			result.close();

			int indexComponent = 0;
			int indexDatabase = 0;
			int compare;
			while ( (indexComponent < componentProperties.length) || (indexDatabase < databaseProperties.length) ) {
				if ( indexComponent >= componentProperties.length )
					compare = 1;
				else if ( indexDatabase >= databaseProperties.length )
					compare = -1;
				else
					compare = DBPlugin.collator.compare(componentProperties[indexComponent][0], databaseProperties[indexDatabase][0]);

				tableItem = new TableItem(table, SWT.NULL);

				if ( compare == 0 ) {				// both have got the same property
					tableItem.setText(0, "Prop: "+componentProperties[indexComponent][0]);
					tableItem.setText(1, componentProperties[indexComponent][1]);
					tableItem.setText(2, databaseProperties[indexDatabase][1]);
					if ( !tableItem.getText(1).equals(tableItem.getText(2)) )
						tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
					++indexComponent;
					++indexDatabase;
				} else if ( compare < 0 ) {			// only the component has got the property
					tableItem.setText(0, "Prop: "+componentProperties[indexComponent][0]);
					tableItem.setText(1, componentProperties[indexComponent][1]);
					tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
					++indexComponent;
				} else {							// only the database has got the property
					tableItem.setText(0, "Prop: "+databaseProperties[indexDatabase][0]);
					tableItem.setText(2, databaseProperties[indexDatabase][1]);
					tableItem.setBackground(DBGui.LIGHT_RED_COLOR);
					++indexDatabase;
				}
			}
			//TODO : move this line into caller !!!
			//btnImportDatabaseVersion.setEnabled(true);
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to get component from database.", err);
		}
	}

	/**
	 * timestamp to record in insert and update SQL requests
	 */
	Timestamp timestamp = new Timestamp(Calendar.getInstance().getTime().getTime());

	public void setTimestamp() {
		timestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
	}

	private int countNewElements = 0;
	private int countIdenticalElements = 0;
	private int countUpdatedElements = 0;

	private int countNewRelationships = 0;
	private int countIdenticalRelationships = 0;
	private int countUpdatedRelationships = 0;

	public void checkComponentsToExport(ArchimateModel model) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Checking in database which components need to be exported.");

		countNewElements = 0;
		countIdenticalElements = 0;
		countUpdatedElements = 0;

		countNewRelationships = 0;
		countIdenticalRelationships = 0;
		countUpdatedRelationships = 0;

		for ( Entry<String, IArchimateElement> entry: model.getAllElements().entrySet() ) {
			switch (checkComponent("elements", entry.getValue())) {
			case NEW :		 ++countNewElements; break;
			case IDENTICAL : ++countIdenticalElements; break;
			case UPDATED :   ++countUpdatedElements; break;
			}
		}

		for ( Entry<String, IArchimateRelationship> entry: model.getAllRelationships().entrySet() ) {
			switch (checkComponent("relationships", entry.getValue())) {
			case NEW :		 ++countNewRelationships; break;
			case IDENTICAL : ++countIdenticalRelationships; break;
			case UPDATED :   ++countUpdatedRelationships; break;
			}
		}

		/*
		for ( Entry<String, IDiagramModel> entry: model.getAllViews().entrySet() ) {
			switch (checkComponent("views", entry.getValue())) {
			case NEW :		 break;
			case IDENTICAL : break;
			case UPDATED :   break;
			}
		}

		for ( Entry<String, EObject> entry: model.getAllViewsObjects().entrySet() ) {
			switch (checkComponent("views_objects", entry.getValue())) {
			case NEW :		 break;
			case IDENTICAL : break;
			case UPDATED :   break;
			}
		}

		for ( Entry<String, EObject> entry: model.getAllViewsConnections().entrySet() ) {
			switch (checkComponent("views_connections", entry.getValue())) {
			case NEW :		 break;
			case IDENTICAL : break;
			case UPDATED :   break;
			}
		}

		for ( Entry<String, IFolder> entry: model.getAllFolders().entrySet() ) {
			switch (checkComponent("folders", entry.getValue())) {
			case NEW :		 break;
			case IDENTICAL : break;
			case UPDATED :   break;
			}
		}
		*/		
	}

	/**
	 * Retrieves the version and the checksum of the component from the database
	 */
	private COMPARE checkComponent(String tableName, EObject eObject) throws Exception {
		int currentVersion = 0;
		int databaseVersion = 0;
		String databaseChecksum = null;
		String databaseCreatedBy = null;
		Timestamp databaseCreatedOn = null;

		// the tables views_objects and views_connections do not have created_by and created_on columns
		boolean hasCreatedByColumn = !tableName.contains("_");

		// first, we force the checksum to be re-calculated
		((IDBMetadata)eObject).getDBMetadata().calculateChecksum();

		// we check the checksum in the database
		ResultSet result = select("SELECT version, checksum"+(hasCreatedByColumn ? ", created_by, created_on" : "")+" FROM "+tableName+" WHERE id = ? ORDER BY version DESC", ((IIdentifier)eObject).getId());

		while ( result.next() ) {
			// The first result gives the latest version 
			if ( databaseVersion == 0 ) {
				databaseVersion = result.getInt("version");
				databaseChecksum = result.getString("checksum");
				if ( hasCreatedByColumn ) {
					databaseCreatedBy = result.getString("created_by");
					databaseCreatedOn = result.getTimestamp("created_on");
				}
			}
			// We check every version in the database to retrieve the version of the current object
			if ( ((IDBMetadata)eObject).getDBMetadata().getCurrentChecksum().equals(result.getString("checksum")) ) {
				currentVersion = result.getInt("version");
				break;
			}
		}
		result.close();

		// Then, we store the values in the DBMetadata
		((IDBMetadata)eObject).getDBMetadata().setCurrentVersion(currentVersion);
		((IDBMetadata)eObject).getDBMetadata().setDatabaseVersion(databaseVersion);
		if ( ((IDBMetadata)eObject).getDBMetadata().getExportedVersion() == 0 )
			((IDBMetadata)eObject).getDBMetadata().setExportedVersion(databaseVersion+1);
		((IDBMetadata)eObject).getDBMetadata().setDatabaseChecksum(databaseChecksum);
		((IDBMetadata)eObject).getDBMetadata().setDatabaseCreatedBy(databaseCreatedBy);
		((IDBMetadata)eObject).getDBMetadata().setDatabaseCreatedOn(databaseCreatedOn);
		
		if ( databaseVersion == 0 ) {
			if ( logger.isTraceEnabled() ) logger.trace("   does not exist in the database. Current version  : "+currentVersion+", checksum : "+((IDBMetadata)eObject).getDBMetadata().getCurrentChecksum());
			return COMPARE.NEW;
		}
		
		if ( logger.isTraceEnabled() ) logger.trace("   Database version : "+databaseVersion+", checksum : "+databaseChecksum+(hasCreatedByColumn ? ", created by : "+databaseCreatedBy+", created on : "+databaseCreatedOn : ""));
		if ( logger.isTraceEnabled() ) logger.trace("   Current version  : "+currentVersion+", checksum : "+((IDBMetadata)eObject).getDBMetadata().getCurrentChecksum());

		if ( databaseVersion == currentVersion )
			return COMPARE.IDENTICAL;

		return COMPARE.UPDATED; 
	}


	public int getCountNewElements() {
		return countNewElements;
	}

	public int getCountIdenticalElements() {
		return countIdenticalElements;
	}

	public int getCountUpdatedElements() {
		return countUpdatedElements;
	}

	public int getCountNewRelationships() {
		return countNewRelationships;
	}

	public int getCountIdenticalRelationships() {
		return countIdenticalRelationships;
	}

	public int getCountUpdatedRelationships() {
		return countUpdatedRelationships;
	}

	public void exportModel(ArchimateModel model, String releaseNote) throws Exception {
		insert("INSERT INTO models (id, version, name, note, purpose, created_by, created_on)"
				,model.getId()
				,model.getExportedVersion()
				,model.getName()
				,releaseNote
				,model.getPurpose()
				,System.getProperty("user.name")
				,timestamp
				);

		for ( int rank = 0 ; rank < model.getProperties().size(); ++rank) {
			IProperty prop = model.getProperties().get(rank);
			insert("INSERT INTO properties (parent_id, parent_version, rank, name, value)"
					,model.getId()
					,model.getExportedVersion()
					,rank
					,prop.getKey()
					,prop.getValue()
					);
		}
	}

	/**
	 * Export a folder into the database.<br>
	 * The rank allows to order the folders during the import process, thus guaranteeing that parents folders are created before sub-folders.
	 */
	public void exportFolder(IFolder folder, int rank) throws Exception {
		// At the moment, folders do not have their own version because we need to implement conflict for them as well
		//checkComponent("folders", folder);
		ArchimateModel model = (ArchimateModel)folder.getArchimateModel();

		insert("INSERT INTO folders_in_model (folder_id, folder_version, parent_folder_id, model_id, model_version, rank)"
				,folder.getId()
				,model.getExportedVersion()																			// at the moment, the folder version is the model version (until we manage folders conflicts)
				,((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId()
						,model.getId()
						,model.getExportedVersion()
						,rank
				);

		//TODO : if ( ((Folder)folder).getDBMetadata().isUpdated() ) {
		insert("INSERT INTO folders (id, version, type, name, documentation, created_by, created_on, checksum)"
				,folder.getId()
				,model.getExportedVersion()											// TODO: at the moment, the folder version is the model version (until we manage folders conflicts)
				,folder.getType().getValue()
				,folder.getName()
				,folder.getDocumentation()
				,System.getProperty("user.name")
				,timestamp
				,((Folder)folder).getDBMetadata().getCurrentChecksum()
				);

		for ( int i = 0 ; i < folder.getProperties().size(); ++i) {
			IProperty prop = folder.getProperties().get(i);
			insert("INSERT INTO properties (parent_id, parent_version, rank, name, value)"
					,folder.getId()
					,model.getExportedVersion()																		// at the moment, the folder version is the model version (until we manage folders conflicts)
					,i
					,prop.getKey()
					,prop.getValue()
					);
		}
		//}
	}

	/**
	 * Export a component to the database
	 */
	public void exportComponent(IArchimateConcept component) throws Exception {
		if ( component instanceof IArchimateElement )
			exportElement(component);
		else
			exportRelationship(component);
	}
	
	/**
	 * Reference a component to a model into the database
	 */
	public void exportComponentInModel(IArchimateConcept component) throws Exception {
		if ( component instanceof IArchimateElement )
			exportElementInModel(component);
		else
			exportRelationshipInModel(component);
	}

	/**
	 * Export an element to the database
	 */
	public void exportElement(IArchimateConcept element) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting version "+((IDBMetadata)element).getDBMetadata().getExportedVersion()+" of "+((IDBMetadata)element).getDBMetadata().getDebugName());

		insert("INSERT INTO elements (id, version, class, name, type, documentation, created_by, created_on, checksum)"
				,element.getId()
				,((IDBMetadata)element).getDBMetadata().getExportedVersion()
				,element.getClass().getSimpleName()
				,element.getName()
				,(element instanceof IJunction) ?
						((IJunction)element).getType() : null
						,element.getDocumentation()
						,System.getProperty("user.name")
						,timestamp
						,((IDBMetadata)element).getDBMetadata().getCurrentChecksum()
				);

		exportProperties(element);
		
		// the element has been exported to the database
		((IDBMetadata)element).getDBMetadata().setCurrentVersion(((IDBMetadata)element).getDBMetadata().getExportedVersion());
	}
	
	/**
	 * Reference an element to a model into the database
	 */
	public void exportElementInModel(IArchimateConcept element) throws Exception {
		ArchimateModel model = (ArchimateModel)element.getArchimateModel();
		
		insert("INSERT INTO elements_in_model (element_id, element_version, parent_folder_id, model_id, model_version)"
				,element.getId()
				,((IDBMetadata)element).getDBMetadata().getCurrentVersion()			// we use currentVersion as it has been set in exportElement()
				,((IFolder)((IArchimateConcept)element).eContainer()).getId()
				,model.getId()
				,model.getExportedVersion()
				);
	}

	/**
	 * Export a relationship to the database
	 */
	public void exportRelationship(IArchimateConcept relationship) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting version "+((IDBMetadata)relationship).getDBMetadata().getExportedVersion()+" of "+((IDBMetadata)relationship).getDBMetadata().getDebugName());

		insert("INSERT INTO relationships (id, version, class, name, documentation, source_id, target_id, strength, access_type, created_by, created_on, checksum)"
				,relationship.getId()
				,((IDBMetadata)relationship).getDBMetadata().getExportedVersion()
				,relationship.getClass().getSimpleName()
				,relationship.getName()
				,relationship.getDocumentation()
				,(relationship instanceof IArchimateRelationship) ? ((IArchimateRelationship)relationship).getSource().getId() : null
						,(relationship instanceof IArchimateRelationship) ? ((IArchimateRelationship)relationship).getTarget().getId() : null
								,(relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null
										,(relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null
												,System.getProperty("user.name")
												,timestamp
												,((IDBMetadata)relationship).getDBMetadata().getCurrentChecksum()
				);

		exportProperties(relationship);
		
		// the relationship has been exported to the database
		((IDBMetadata)relationship).getDBMetadata().setCurrentVersion(((IDBMetadata)relationship).getDBMetadata().getExportedVersion());
	}
	
	/**
	 * Reference a relationship to a model into the database
	 */
	public void exportRelationshipInModel(IArchimateConcept relationship) throws Exception {
		ArchimateModel model = (ArchimateModel)relationship.getArchimateModel();

		insert("INSERT INTO relationships_in_model (relationship_id, relationship_version, parent_folder_id, model_id, model_version)"
				,relationship.getId()
				,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion()	// we use currentVersion as it has been set in exportRelationship()
				,((IFolder)((IArchimateConcept)relationship).eContainer()).getId()
				,model.getId()
				,model.getExportedVersion()
				);
	}

	/**
	 * Export properties to the database
	 */
	public void exportProperties(IProperties parent) throws Exception {
		for ( int i = 0 ; i < parent.getProperties().size(); ++i) {
			IProperty prop = parent.getProperties().get(i);
			insert("INSERT INTO properties (parent_id, parent_version, rank, name, value)"
					,((IIdentifier)parent).getId()
					,((IDBMetadata)parent).getDBMetadata().getExportedVersion()
					,i
					,prop.getKey()
					,prop.getValue()
					);
		}
	}

	/**
	 * import a component from the database
	 */
	public void importComponent(IArchimateConcept component, int version) throws Exception {
		if ( component instanceof IArchimateElement )
			importElement(component, version);
		else
			importRelationship(component, version);
	}

	/**
	 * import an element from the database
	 */
	public IArchimateConcept importElement(IArchimateConcept element, int version) throws Exception {
		ResultSet result;

		if ( version != 0 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Importing version "+version+" of "+((IDBMetadata)element).getDBMetadata().getDebugName());
			result = select("SELECT version, class, name, type, documentation FROM elements WHERE id = ? AND version = ?"
					,element.getId()
					,version
					);
		} else { 
			if ( logger.isDebugEnabled() ) logger.debug("Importing latest version of "+((IDBMetadata)element).getDBMetadata().getDebugName());
			result = select("SELECT max(version) as version, class, name, type, documentation FROM elements WHERE id = ?"
					,element.getId()
					);
		}

		if ( result.next() ) {
			if ( version == 0 )
				version = result.getInt("version");

			((IDBMetadata)element).getDBMetadata().setCurrentVersion(version);
			
			_fifo.add(resultSetCopy(result));
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					HashMap<String, Object> map = _fifo.removeFirst();
					
					// we only update the values that differ 
					if ( !element.getName().equals((String)map.get("name")) )
						element.setName((String)map.get("name"));

					if ( !element.getDocumentation().equals((String)map.get("documentation")) )
						element.setDocumentation((String)map.get("documentation"));
	
					if ( element instanceof IJunction ) {
						if ( !((IJunction)element).getType().equals((String)map.get("type")) )
							((IJunction)element).setType((String)map.get("type"));
					}
				}
			});
			result.close();

			importProperties(element);
		} else {
			throw new Exception("Cannot find component "+((IDBMetadata)element).getDBMetadata().getDebugName()+" in the database.");
		}

		return element;
	}

	/**
	 * import a relationship from the database
	 */
	public IArchimateConcept importRelationship(IArchimateConcept relationship, int version) throws Exception {
		ResultSet result;
		ArchimateModel model = (ArchimateModel)relationship.getArchimateModel();

		if ( version != 0 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Importing version "+version+" of "+((IDBMetadata)relationship).getDBMetadata().getDebugName());
			result = select("SELECT class, name, documentation, source_id, target_id, strength, access_type FROM components WHERE id = ? AND version = ?"
					,relationship.getId()
					,version
					);
		} else { 
			if ( logger.isDebugEnabled() ) logger.debug("Importing latest version of "+((IDBMetadata)relationship).getDBMetadata().getDebugName());
			result = select("SELECT max(version) as version, class, name, documentation, source_id, target_id, strength, access_type FROM components WHERE id = ?"
					,relationship.getId()
					);
		}

		if ( result.next() ) {
			if ( version == 0 )
				version = result.getInt("version");

			((IDBMetadata)relationship).getDBMetadata().setCurrentVersion(version);

			// we only update the values that differ 
			if ( !relationship.getName().equals(result.getString("name")) )
				relationship.setName(result.getString("name"));
			if ( !relationship.getDocumentation().equals(result.getString("documentation")) )
				relationship.setDocumentation(result.getString("documentation"));
			if ( !((IArchimateRelationship)relationship).getSource().getId().equals(result.getString("source_id")) )
				((IArchimateRelationship)relationship).setSource(model.getAllRelationships().get(result.getString("source_id")));
			if ( !((IArchimateRelationship)relationship).getTarget().getId().equals(result.getString("target_id")) )
				((IArchimateRelationship)relationship).setTarget(model.getAllRelationships().get(result.getString("target_id")));
			if ( relationship instanceof IInfluenceRelationship && !((IInfluenceRelationship)relationship).getStrength().equals(result.getString("Strength")) ) 
				((IInfluenceRelationship)relationship).setStrength(result.getString("Strength"));
			if ( relationship instanceof IAccessRelationship && ((IAccessRelationship)relationship).getAccessType()!=result.getInt("access_type") )
				((IAccessRelationship)relationship).setAccessType(result.getInt("access_type"));

			result.close();

			importProperties(relationship);
		} else {
			throw new Exception("Cannot find component "+((IDBMetadata)relationship).getDBMetadata().getDebugName()+" in the database.");
		}

		return relationship;
	}

	public void getModels(String filter, Table tblModels) throws Exception {
		ResultSet result;
		
		if ( filter==null || filter.length()==0 )
			result = select("SELECT id, name, max(version) AS version FROM models GROUP BY id ORDER BY id");
		else
			result = select("SELECT id, name, max(version) AS version FROM models WHERE name like ? GROUP BY id ORDER BY id", filter);
		
		TableItem tableItem;

		tblModels.removeAll();
		while ( result.next() ) {
			if ( result.getString("id") != null ) {		// in case the table is empty
				tableItem = new TableItem(tblModels, SWT.NULL);
				tableItem.setText(result.getString("name"));
				tableItem.setData("id", result.getString("id"));
			}
		}

		result.close();

		if ( tblModels.getItemCount() != 0 ) {
			tblModels.setSelection(0);
			tblModels.notifyListeners(SWT.Selection, new Event());		// calls database.getModelVersions()
		}
	}

	public void getModelVersions(String id, Table tblModelVersions) throws Exception {
		ResultSet result = select("SELECT version, created_by, created_on, name, note, purpose FROM models WHERE id = ? ORDER BY version DESC", id);
		TableItem tableItem;

		tblModelVersions.removeAll();
		while ( result.next() ) {
			tableItem = new TableItem(tblModelVersions, SWT.NULL);
			tableItem.setText(0, result.getString("version"));
			tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(result.getTimestamp("created_on")));
			tableItem.setText(2, result.getString("created_by"));
			tableItem.setData("name", result.getString("name"));
			tableItem.setData("note", result.getString("note")!=null ? result.getString("note") : "");
			tableItem.setData("purpose", result.getString("purpose")!=null ? result.getString("purpose") : "");
		}

		result.close();

		if ( tblModelVersions.getItemCount() != 0 ) {
			tblModelVersions.setSelection(0);
			tblModelVersions.notifyListeners(SWT.Selection, new Event());		// calls database.getModelVersions()
		}
	}

	private LinkedList<HashMap<String, Object>> _fifo = new LinkedList<HashMap<String, Object>>();
	private HashSet<String> allImagePaths;


	/**
	 * Import the model metadata from the database
	 */
	public int importModel(ArchimateModel model) throws Exception {
			// reseting the model's counters
		model.resetCounters();
		
		ResultSet result = select("SELECT name, purpose FROM models WHERE id = ? AND version = ?"
				,model.getId()
				,model.getCurrentVersion()
				);

		//TODO : manage the "real" model metadata :-)

		result.next();

		_fifo.add(resultSetCopy(result));
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				HashMap<String, Object> map = _fifo.removeFirst();
				
				model.setPurpose((String)map.get("purpose"));
			}
		});
		result.close();

		importProperties(model);
		
		result = select("SELECT COUNT(*) AS countElements FROM (SELECT DISTINCT element_id FROM elements_in_model INNER JOIN elements ON elements_in_model.element_id = elements.id WHERE model_id = ? AND model_version = ? UNION SELECT DISTINCT element_id FROM views_in_model INNER JOIN views ON views_in_model.view_id = views.id INNER JOIN views_objects ON views.id = views_objects.view_id AND views.version = views_objects.version WHERE model_id = ? AND model_version = ? and element_id IS NOT NULL)"
				,model.getId()
				,model.getCurrentVersion()
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countElements = result.getInt("countElements");
		result.close();
		
		result = select("SELECT COUNT(*) AS countRelationships FROM (SELECT DISTINCT relationship_id FROM relationships_in_model INNER JOIN relationships ON relationships_in_model.relationship_id = relationships.id WHERE model_id = ? AND model_version = ? UNION SELECT DISTINCT relationship_id FROM views_in_model INNER JOIN views ON views_in_model.view_id = views.id INNER JOIN views_connections ON views.id = views_connections.view_id AND views.version = views_connections.version WHERE model_id = ? AND model_version = ? and relationship_id IS NOT NULL)"
				,model.getId()
				,model.getCurrentVersion()
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countRelationships = result.getInt("countRelationships");
		result.close();
		
		result = select("SELECT COUNT(*) AS countFolders FROM folders_in_model WHERE model_id = ? AND model_version = ?"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countFolders = result.getInt("countFolders");
		result.close();
		
		result = select("SELECT COUNT(*) AS countViews FROM views_in_model WHERE model_id = ? AND model_version = ?"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countViews = result.getInt("countViews");
		result.close();
		
		result = select("SELECT COUNT(*) AS countViewsObjects FROM views_in_model INNER JOIN views ON views_in_model.view_id = views.id INNER JOIN views_objects ON views.id = views_objects.view_id AND views.version = views_objects.version WHERE model_id = ? AND model_version = ?"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countViewsObjects = result.getInt("countViewsObjects");
		result.close();
		
		result = select("SELECT COUNT(*) AS countViewsConnections FROM views_in_model INNER JOIN views ON views_in_model.view_id = views.id INNER JOIN views_connections ON views.id = views_connections.view_id AND views.version = views_connections.version WHERE model_id = ? AND model_version = ?"
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countViewsConnections = result.getInt("countViewsConnections");
		result.close();
		
		result = select("SELECT COUNT(DISTINCT image_path) AS countImages FROM views_in_model INNER JOIN views ON views_in_model.view_id = views.id INNER JOIN views_objects ON views.id = views_objects.view_id AND views.version = views_objects.version INNER JOIN images ON views_objects.image_path = images.path WHERE model_id = ? AND model_version = ? AND path IS NOT NULL" 
				,model.getId()
				,model.getCurrentVersion()
				);
		result.next();
		int countImages = result.getInt("countImages");
		result.close();
		
		if ( logger.isDebugEnabled() ) logger.debug("Importing "+countElements+" elements, "+countRelationships+" relationships, "+countFolders+" folders, "+countViews+" views, "+countViewsObjects+" views objects, "+countViewsConnections+" views connections, and "+countImages+" images.");

		// initializing the HashMaps that will be used to reference imported objects
		allImagePaths = new HashSet<String>();

		return countElements + countRelationships + countFolders + countViews + countViewsObjects + countViewsConnections + countImages;
	}

	/**
	 * Prepare the import of the folders from the database
	 */
	public void prepareImportFolders(ArchimateModel model) throws Exception {
		currentResultSet = select("SELECT folder_id, folder_version, parent_folder_id, type, name, documentation FROM folders_in_model INNER JOIN folders ON folders_in_model.folder_id = folders.id AND folders_in_model.folder_version = folders.version WHERE model_id = ? AND model_version = ? ORDER BY rank"
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

				if ( currentResultSet.getInt("type") != 0 )
					folder.setType(FolderType.get(currentResultSet.getInt("type")));
				folder.setId(currentResultSet.getString("folder_id"));

				_fifo.add(resultSetCopy(currentResultSet));
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						HashMap<String, Object> map = _fifo.removeFirst();
						
						folder.setName((String)map.get("name"));
						folder.setDocumentation((String)map.get("documentation"));
						
						if ( map.get("parent_folder_id") == null ) {
							model.getFolders().add(folder);
						} else {
							model.getAllFolders().get(map.get("parent_folder_id")).getFolders().add(folder);
						}
					}
				});
				importProperties(folder);
				if ( logger.isDebugEnabled() ) logger.debug("   imported "+currentResultSet.getString("name")+"("+currentResultSet.getString("folder_id")+")");

					// we reference this folder for future use (storing sub-folders or components into it ...)
				model.countObject(folder);
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
	public void prepareImportElements(ArchimateModel model) throws Exception {
		currentResultSet = select("SELECT element_id, max(version) as version, parent_folder_id, class, name, type, documentation FROM elements_in_model INNER JOIN elements ON elements_in_model.element_id = elements.id WHERE model_id = ? AND model_version = ? GROUP BY element_id"
				,model.getId()
				,model.getCurrentVersion()
				);
		//TODO : referencer tous les elements qui one été modifiés depuis le dernier exper : ie tous ceux dont la version != elements_in_model.element_version
	}
	
	/**
	 * import the elements from the database
	 */
	public boolean importElements(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IArchimateElement element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));
				element.setId(currentResultSet.getString("element_id"));
				((IDBMetadata)element).getDBMetadata().setCurrentVersion(currentResultSet.getInt("version"));

				_fifo.add(resultSetCopy(currentResultSet));
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						HashMap<String, Object> map = _fifo.removeFirst();
						
						element.setName((String)map.get("name"));
						element.setDocumentation((String)map.get("documentation"));
						if ( element instanceof IJunction	&& map.get("type")!=null )	((IJunction)element).setType((String)map.get("type"));

						model.getAllFolders().get(map.get("parent_folder_id")).getElements().add(element);
					}
				});
				importProperties(element);

				// we reference the element for future use (establishing relationships, creating views objects, ...)
				model.countObject(element);
				
				if ( logger.isDebugEnabled() ) logger.debug("   imported "+currentResultSet.getString("class")+":"+currentResultSet.getString("name")+"("+currentResultSet.getString("element_id")+")");
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
		currentResultSet = select("SELECT relationship_id, relationship_version, parent_folder_id, class, name, documentation, source_id, target_id, strength, access_type FROM relationships_in_model INNER JOIN relationships ON relationships_in_model.relationship_id = relationships.id AND relationships_in_model.relationship_version = relationships.version WHERE model_id = ? AND model_version = ?"
				,model.getId()
				,model.getCurrentVersion()
				);
	}

	/**
	 * import the relationships from the database
	 */
	public boolean importRelationships(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IArchimateRelationship relationship = (IArchimateRelationship) DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));
				relationship.setId(currentResultSet.getString("relationship_id"));
				((IDBMetadata)relationship).getDBMetadata().setCurrentVersion(currentResultSet.getInt("relationship_version"));

				_fifo.add(resultSetCopy(currentResultSet));
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						HashMap<String, Object> map = _fifo.removeFirst();
						
						relationship.setName((String)map.get("name"));
						relationship.setDocumentation((String)map.get("documentation"));

						if ( relationship instanceof IInfluenceRelationship	&& map.get("strength")!=null )		((IInfluenceRelationship)relationship).setStrength((String)map.get("strength"));
						if ( relationship instanceof IAccessRelationship	&& map.get("access_type")!=null )	((IAccessRelationship)relationship).setAccessType((Integer)map.get("access_type"));

						model.getAllFolders().get(map.get("parent_folder_id")).getElements().add(relationship);
					}
				});
				importProperties(relationship);

				// we reference the relationship for future use (establishing relationships, creating views connections, ...)
				model.countObject(relationship);
				model.registerSourceAndTarget(relationship, currentResultSet.getString("source_id"), currentResultSet.getString("target_id"));
				
				if ( logger.isDebugEnabled() ) logger.debug("   imported "+currentResultSet.getString("class")+":"+currentResultSet.getString("name")+"("+currentResultSet.getString("relationship_id")+")");
				return true;
			}
			currentResultSet.close();
			currentResultSet = null;
			
			//model.reconnectRelationships();
		}
		return false;
	}

	/**
	 * Prepare the import of the views from the database
	 */
	public void prepareImportViews(ArchimateModel model) throws Exception {
		currentResultSet = select("SELECT id, version, parent_folder_id, class, name, documentation, background, connection_router_type, hint_content, hint_title, viewpoint FROM views_in_model INNER JOIN views ON views_in_model.view_id = views.id AND views_in_model.view_version = views.version WHERE model_id = ? AND model_version = ? ORDER BY rank"
				,model.getId()
				,model.getCurrentVersion()
				);
	}

	/**
	 * import the views from the database
	 */
	public boolean importViews(ArchimateModel model) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				IDiagramModel view;
				if ( currentResultSet.getString("class").equals("CanvasModel") )
					view = (IDiagramModel) DBCanvasFactory.eINSTANCE.create(currentResultSet.getString("class"));
				else
					view = (IDiagramModel) DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));

				view.setId(currentResultSet.getString("id"));
				((IDBMetadata)view).getDBMetadata().setCurrentVersion(currentResultSet.getInt("version"));

				_fifo.add(resultSetCopy(currentResultSet));
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						HashMap<String, Object> map = _fifo.removeFirst();
						
						view.setName((String)map.get("name"));
						view.setDocumentation((String)map.get("documentation"));
						view.setConnectionRouterType((Integer)map.get("connection_router_type"));
						if ( view instanceof IArchimateDiagramModel && map.get("viewpoint")!=null )		((IArchimateDiagramModel) view).setViewpoint((String)map.get("viewpoint"));
						if ( view instanceof ISketchModel			&& map.get("background")!=null )	((ISketchModel)view).setBackground((Integer)map.get("background"));
						if ( view instanceof IHintProvider			&& map.get("hint_content")!=null )	((IHintProvider)view).setHintContent((String)map.get("hint_content"));
						if ( view instanceof IHintProvider			&& map.get("hint_title")!=null )	((IHintProvider)view).setHintTitle((String)map.get("hint_title"));

						model.getAllFolders().get(map.get("parent_folder_id")).getElements().add(view);
					}
				});
				importProperties(view);
				if ( logger.isDebugEnabled() ) logger.debug("   imported "+currentResultSet.getString("name")+"("+currentResultSet.getString("id")+")");

				// we reference the view for future use
				model.countObject(view);
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
	public void prepareImportViewsObjects(ArchimateModel model, String viewId) throws Exception {
		IDiagramModel view = model.getAllViews().get(viewId);

		currentResultSet = select("SELECT id, version, container_id, class, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height FROM views_objects WHERE view_id = ? AND view_version = ? ORDER BY rank"
				,viewId
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion()
				);
	}

	/**
	 * import the views objects from the database
	 */
	public boolean importViewsObjects(ArchimateModel model, String viewId) throws Exception {
		if ( currentResultSet != null ) {
			if ( currentResultSet.next() ) {
				EObject eObject;
				IDiagramModel view = model.getAllViews().get(viewId);

				if ( currentResultSet.getString("class").startsWith("Canvas") )
					eObject = DBCanvasFactory.eINSTANCE.create(currentResultSet.getString("class"));
				else
					eObject = DBArchimateFactory.eINSTANCE.create(currentResultSet.getString("class"));

				((IIdentifier)eObject).setId(currentResultSet.getString("id"));
				((IDBMetadata)eObject).getDBMetadata().setCurrentVersion(currentResultSet.getInt("version"));
				
				_fifo.add(resultSetCopy(currentResultSet));
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						HashMap<String, Object> map = _fifo.removeFirst();
						if ( (eObject instanceof IDiagramModelArchimateComponent) && ! (model.getAllElements().get(map.get("element_id")) instanceof IArchimateElement))
							logger.error("ERREUR");
						if ( eObject instanceof IDiagramModelArchimateComponent && map.get("element_id")!=null ) 							((IDiagramModelArchimateComponent)eObject).setArchimateConcept(model.getAllElements().get(map.get("element_id")));
						if ( eObject instanceof IDiagramModelReference			&& map.get("diagram_ref_id")!=null )						((IDiagramModelReference)eObject).setReferencedModel(model.getAllViews().get(map.get("diagram_ref_id")));
						if ( eObject instanceof IDiagramModelArchimateObject	&& map.get("type")!=null )									((IDiagramModelArchimateObject)eObject).setType((Integer)map.get("type"));
						if ( eObject instanceof IBorderObject					&& map.get("border_color")!=null )							((IBorderObject)eObject).setBorderColor((String)map.get("border_color"));
						if ( eObject instanceof IDiagramModelNote				&& map.get("border_type")!=null )							((IDiagramModelNote)eObject).setBorderType((Integer)map.get("border_type"));
						if ( eObject instanceof ITextContent 					&& map.get("content")!=null )								((ITextContent)eObject).setContent((String)map.get("content"));
						if ( eObject instanceof IDocumentable 					&& map.get("documentation")!=null )							((IDocumentable)eObject).setDocumentation((String)map.get("documentation"));
						if ( eObject instanceof INameable	 					&& map.get("name")!=null && map.get("element_id")==null )	((INameable)eObject).setName((String)map.get("name"));
						if ( eObject instanceof IHintProvider					&& map.get("hint_content")!=null )							((IHintProvider)eObject).setHintContent((String)map.get("hint_content"));
						if ( eObject instanceof IHintProvider					&& map.get("hint_title")!=null )							((IHintProvider)eObject).setHintTitle((String)map.get("hint_title"));
						if ( eObject instanceof ILockable						&& map.get("is_locked")!=null )								((ILockable)eObject).setLocked((Boolean)map.get("is_locked"));
						if ( eObject instanceof IDiagramModelImageProvider		&& map.get("image_path")!=null )							((IDiagramModelImageProvider)eObject).setImagePath((String)map.get("image_path"));
						if ( eObject instanceof IIconic							&& map.get("image_position")!=null )						((IIconic)eObject).setImagePosition((Integer)map.get("image_position"));
						if ( eObject instanceof ILineObject 					&& map.get("line_color")!=null )							((ILineObject)eObject).setLineColor((String)map.get("line_color"));
						if ( eObject instanceof ILineObject						&& map.get("line_width")!=null )							((ILineObject)eObject).setLineWidth((Integer)map.get("line_width"));
						if ( eObject instanceof IDiagramModelObject 			&& map.get("fill_color")!=null )							((IDiagramModelObject)eObject).setFillColor((String)map.get("fill_color"));
						if ( eObject instanceof IFontAttribute 					&& map.get("font")!=null )									((IFontAttribute)eObject).setFont((String)map.get("font"));
						if ( eObject instanceof IFontAttribute 					&& map.get("font_color")!=null )							((IFontAttribute)eObject).setFontColor((String)map.get("font_color"));
						if ( eObject instanceof ICanvasModelSticky				&& map.get("notes")!=null )									((ICanvasModelSticky)eObject).setNotes((String)map.get("notes"));
						if ( eObject instanceof ITextAlignment					&& map.get("text_alignment")!=null )						((ITextAlignment)eObject).setTextAlignment((Integer)map.get("text_alignment"));
						if ( eObject instanceof ITextPosition 					&& map.get("text_position")!=null )							((ITextPosition)eObject).setTextPosition((Integer)map.get("text_position"));
						if ( eObject instanceof IDiagramModelObject )																		((IDiagramModelObject)eObject).setBounds((Integer)map.get("x"), (Integer)map.get("y"), (Integer)map.get("width"), (Integer)map.get("height"));

							// The container is either the view, or a container in the view
						IDiagramModelContainer container = (map.get("container_id").equals(view.getId())) ? view : ((IDiagramModelContainer)model.getAllViewsObjects().get(map.get("container_id")));
						container.getChildren().add((IDiagramModelObject)eObject);
					}
				});

				// If the object has got properties but does not have a linked element, then it may have distinct properties
				if ( eObject instanceof IProperties && currentResultSet.getString("element_id")==null ) {
					importProperties((IProperties)eObject);
				}
				
				if ( logger.isDebugEnabled() ) logger.debug("   imported "+currentResultSet.getString("class")+"("+((IIdentifier)eObject).getId()+")");

					// we reference the view for future use
				model.countObject(eObject);

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
	public void prepareImportViewsConnections(ArchimateModel model, String viewId) throws Exception {
		IDiagramModel view = model.getAllViews().get(viewId);

		currentResultSet = select("SELECT id, version, container_id, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_object_id, target_object_id, type FROM views_connections WHERE view_id = ? AND view_version = ? ORDER BY rank"
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion()
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
				((IDBMetadata)eObject).getDBMetadata().setCurrentVersion(currentResultSet.getInt("version"));
				
				_fifo.add(resultSetCopy(currentResultSet));
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						HashMap<String, Object> map = _fifo.removeFirst();
						
						if ( eObject instanceof ILockable							&& map.get("is_locked")!=null )			((ILockable)eObject).setLocked((Boolean)map.get("is_locked"));
						if ( eObject instanceof IDocumentable						&& map.get("documentation")!=null )		((IDocumentable)eObject).setDocumentation((String)map.get("documentation"));
						if ( eObject instanceof ILineObject 						&& map.get("line_color")!=null )		((ILineObject)eObject).setLineColor((String)map.get("line_color"));
						if ( eObject instanceof ILineObject 						&& map.get("line_width")!=null )		((ILineObject)eObject).setLineWidth((Integer)map.get("line_width"));
						if ( eObject instanceof IFontAttribute 						&& map.get("font")!=null )				((IFontAttribute)eObject).setFont((String)map.get("font"));
						if ( eObject instanceof IFontAttribute						&& map.get("font_color")!=null )		((IFontAttribute)eObject).setFontColor((String)map.get("font_color"));
						if ( eObject instanceof IDiagramModelConnection				&& map.get("name")!=null )				((IDiagramModelConnection)eObject).setName((String)map.get("name"));
						if ( eObject instanceof IDiagramModelConnection				&& map.get("type")!=null )				((IDiagramModelConnection)eObject).setType((Integer)map.get("type"));
						if ( eObject instanceof IDiagramModelArchimateConnection	&& map.get("type")!=null )				((IDiagramModelArchimateConnection)eObject).setType((Integer)map.get("type"));
						if ( eObject instanceof IDiagramModelArchimateConnection	&& map.get("relationship_id")!=null )	((IDiagramModelArchimateConnection)eObject).setArchimateConcept(model.getAllRelationships().get(map.get("relationship_id")));
					}
				});

				model.registerSourceAndTarget(eObject, currentResultSet.getString("source_object_id"), currentResultSet.getString("target_object_id"));

				if ( eObject instanceof IDiagramModelConnection ) {
					ResultSet resultBendpoints = select("SELECT start_x, start_y, end_x, end_y FROM bendpoints WHERE parent_id = ? AND parent_version = ? ORDER BY rank"
							,((IIdentifier)eObject).getId()
							,((IDBMetadata)eObject).getDBMetadata().getCurrentVersion()
							);
					while(resultBendpoints.next()) {
						IDiagramModelBendpoint bendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
						bendpoint.setStartX(resultBendpoints.getInt("start_x"));
						bendpoint.setStartY(resultBendpoints.getInt("start_y"));
						bendpoint.setEndX(resultBendpoints.getInt("end_x"));
						bendpoint.setEndY(resultBendpoints.getInt("end_y"));
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								((IDiagramModelConnection)eObject).getBendpoints().add(bendpoint);
							}
						});
					}
					resultBendpoints.close();
				}

					// we reference the view for future use
				model.countObject(eObject);
				
					// If the connection has got properties but does not have a linked relationship, then it may have distinct properties
				if ( eObject instanceof IProperties && currentResultSet.getString("relationship_id")==null ) {
					importProperties((IProperties)eObject);
				}

				if ( logger.isDebugEnabled() ) logger.debug("   imported "+currentResultSet.getString("class")+"("+((IIdentifier)eObject).getId()+")");

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
		currentResultSet = select("SELECT image FROM images WHERE path = ?", path);
		
		if (currentResultSet.next() ) {
			IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);
			try {
				String imagePath;
				byte[] imageContent = currentResultSet.getBytes("image");

				if ( logger.isDebugEnabled() ) logger.debug( "Importing "+path+" with "+imageContent.length/1024+" Ko of data");
				imagePath = archiveMgr.addByteContentEntry(path, imageContent);

				if ( imagePath.equals(path) ) {
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
			currentResultSet.close();
			currentResultSet = null;
		} else {
			currentResultSet.close();
			currentResultSet = null;
			throw new Exception("Import of image failed : unkwnown image path "+path);
		}
	}

	private void importProperties(IProperties parent) throws Exception {
		int currentVersion;

		if ( parent instanceof IArchimateModel )
			currentVersion = ((ArchimateModel)parent).getCurrentVersion();
		else
			currentVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion();

		ResultSet result = select("SELECT name, value FROM properties WHERE parent_id = ? AND parent_version = ? ORDER BY rank"
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

			if ( !prop.getKey().equals(result.getString("name")) )
				prop.setKey(result.getString("name"));
			if ( !prop.getValue().equals(result.getString("value")) )
				prop.setValue(result.getString("value"));

			if ( shouldAdd ) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						parent.getProperties().add(prop);
					}
				});
			}

			++i;
		}
		result.close();

		// if there are more properties in memory than in database, we delete them
		while ( i < parent.getProperties().size() )
			parent.getProperties().remove(i);
	}
	
	private HashMap<String, Object> resultSetCopy(ResultSet rs) throws SQLException {
		HashMap<String, Object> map = new HashMap<String, Object>();
		
	    for (int column = 1; column <= rs.getMetaData().getColumnCount(); column++) {
	    	if ( rs.getObject(column) != null ) {
	    			// we only listed the types that may be found by the database proxy and not the exhaustive types list
	    		switch ( rs.getMetaData().getColumnType(column) ) {
	    			case Types.INTEGER :
	    			case Types.NUMERIC :
	    			case Types.SMALLINT :
	    			case Types.TINYINT :	map.put(rs.getMetaData().getColumnName(column), rs.getInt(column));	 break;
	    			
	    			case Types.BOOLEAN :	map.put(rs.getMetaData().getColumnName(column), rs.getBoolean(column)); break;
	    			
	    			case Types.TIMESTAMP :	map.put(rs.getMetaData().getColumnName(column), rs.getTimestamp(column)); break;
	    			
	    			default :				map.put(rs.getMetaData().getColumnName(column), rs.getString(column));
	    		}
	    	}
	    }
		
		return map;
	}

	public void importElementFromId(ArchimateModel model, String id, boolean mustCreateCopy) throws Exception {
		if ( logger.isDebugEnabled() ) {
			if ( mustCreateCopy )
				logger.debug("Importing a copy of element id "+id);
			else
				logger.debug("Importing elment id "+id);
		}
		
		ResultSet result = select("SELECT id, max(version) as version, class, name, documentation, type FROM elements WHERE id = ?", id);
		
		if ( result.next() ) {
			IArchimateElement element = (IArchimateElement) DBArchimateFactory.eINSTANCE.create(result.getString("class"));
			if ( mustCreateCopy )
				element.setId(EcoreUtil.generateUUID());
			else
				element.setId(result.getString("id"));
			((IDBMetadata)element).getDBMetadata().setCurrentVersion(result.getInt("version"));
	
			element.setName(result.getString("name"));
			element.setDocumentation(result.getString("documentation"));
			if ( element instanceof IJunction	&& result.getString("type")!=null )	((IJunction)element).setType(result.getString("type"));
	
			model.getDefaultFolderForObject(element).getElements().add(element);
	
			importProperties(element);
	
			// we reference the element for future use (establishing relationships, creating views objects, ...)
			model.countObject(element);
			
			if ( logger.isDebugEnabled() ) logger.debug("   imported "+((IDBMetadata)element).getDBMetadata().getDebugName());
		} else {
			throw new Exception("Failed to import element id "+id+" from database.");
		}
		
		//TODO : import relationships from and to this element !!!
	}
	
}