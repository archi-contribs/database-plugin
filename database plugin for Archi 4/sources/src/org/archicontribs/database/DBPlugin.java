/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import java.util.Base64;

/**
 * Database Model Importer / Exporter
 * 
 * The DBPlugin class implements static methods used everywhere else in the plugin. 
 * 
 * @author Herve Jouin
 *
 * 
 * v0.1 : 25/03/2016		plug-in creation
 * v0.2 : 01/05/2016		Add models version
 * v0.3 : 08/05/2016		Add import filtering
 * v0.4 : 10/05/2016		Add Oracle driver and use lower case only table names to increase Windows/Linux portability
 * v0.5 : 15/05/2016		Allow to import several projects in a single model
 * 							Add support for folders and canvas
 * 							Add SQLite driver
 * v0.6 : 22/05/2016		bug corrections (especially regarding folders)
 * 							few optimizations
 * 							Import and export elements from other models is now possible even if it needs improvements
 * v0.6b: 23/05/2016		solve a dependency mistake in JAR
 * v0.6c: 23/05/2016		solve a bug in the folders loading that prevented the objects to be correctly included in the model
 * v0.7 : 24/05/2016		adding a hashtable of EObject in the DBModel class to accelerate the finding of an object by its ID  
 *							begin to add some Javadoc
 * v0.8 : 13/06/2016		All the Archi components are now managed
 *                          Added a progressbar to follow the import and export processes
 * v0.9 : 22/08/2016		Images are now imported and exported correctly
 * v0.10: 26/09/2016		add Neo4j driver
 *                          update all import/export methods to generate SQL and Cypher (CQL) requests
 *                          Use Archi's preferences
 *                          The folder where is stored the preferences file has changed  
 *                          Standalone import mode is now the default until we can share components between models
 *                          Rewrite of export and import classes to increase reliability
 *                          Add exceptions to reduce the cases when the model is not complete
 *                          Bug resolution : add bounds export/import for DiagramModelReference
 *                          The Filter when selecting a model is (temporarily) deactivated for Neo4J databases
 * v0.10b: 28/09/2016		Change the separator between the Id, the model Id and the version as the "-" could be found in Archi's IDs
 * 							Correct a bug where the projects selected were not imported
 * 							Correct a bug in the delete model procedure
 * 							Check if a project with same ID or same name already exists before importing it
 * 							Add a test to verify that we are not connected to a database configured for Archi 4
 * v0.11: 13/10/2016		Add the capability to select several models on the import window
 *							Create 2 releases : one for Archi 3, one for Archi 4
 *							Change JDBC mode to auto-commit to avoid reset each time a select fails
 * 							Correct a bug in the Neo4j model import (properties are case sensitive)
 * 							Correct a bug in the Sketch import
 * 							Column "parent" of database table "connection" removed as it was redundant with column "source"
 * 							Add colored icons on tabItem titles to summarize their status
 *							Add table "archidatabaseplugin" to distinguish databases configured with Archi 3 or Archi 4 datamodels
 *							Use of CLOB for key text fields (name, description, ...)
 *							Use of longer field for ID
 *							Include model and version to referenced Ids to prepare inter-models relationships
 */
public class DBPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.archicontribs.database";
	public static DBPlugin INSTANCE;
	
	public static final String pluginVersion = "0.10b";
	public static final String pluginName = "DatabasePlugin";
	public static final String pluginTitle = "Database import/export plugin v" + pluginVersion;
	public static final String Separator = ";";
	
	static StringBuilder margin = new StringBuilder();
	
	/**
	 * List of the implemented drivers
	 */
	public static final String[] driverList = {"Neo4j", "MySQL", "Oracle", "PostGreSQL", "SQLite"};
	
	/**
	 * List of the the default TCP ports
	 */
	public static final String[] defaultPorts = {"7687", "3306", "1521", "5432", ""};

	/**
	 * Name of all the table names in a SQL database
	 */
	public static String[] allSQLTables = { "archimatediagrammodel", "archimateelement", "bendpoint", "canvasmodel", "canvasmodelblock", "canvasmodelimage", "canvasmodelsticky", "connection", "diagrammodelarchimateobject", "diagrammodelreference", "folder", "model", "property", "relationship", "sketchmodel", "sketchmodelactor", "sketchmodelsticky"};

	/**
	 * level (Info, Warning, Error) for reporting  
	 */
	public enum Level { Info, Warning, Error };
	
	/**
	 * level (Info, Warning, Error) for reporting  
	 */
	public enum DebugLevel { MainMethod, SecondaryMethod, Variable, SQLRequest };

	/**
	 * Name of the model used as a container in shared mode. 
	 */
	public static String SharedModelName = "Shared model";
	
	/**
	 * ID of the model used as a container in shared mode.
	 */
	public static String SharedModelId = generateProjectId("Shared", "0.0");
	
	/**
	 * Name of the folder that contains projects subfolders in shared mode 
	 */
	public static String SharedFolderName = "Projects";

	/**
	 * ID of the folder that contains projects subfolders in shared mode 
	 */
	public static String SharedFolderId = generateProjectId("Projects", SharedModelId);

	public DBPlugin() {
        INSTANCE = this;
    }
    
	/**
	 * Prints the object on screen if in debug mode
	 * @param _level
	 * @param _message
	 */
	public static void debug(DebugLevel _level, String message){
		debug(_level, message, null);
	}
	
	/**
	 * Prints the object on screen if in debug mode
	 * @param _level
	 * @param _message
	 * @parame e
	 */
	public static void debug(DebugLevel _level, String message, Exception e) {
		DBPreferencePage pref = new DBPreferencePage();
		boolean doDebug = false;
		switch ( _level ) {
		case MainMethod : doDebug = pref.shouldDebugMainMethods(); break;
		case SecondaryMethod : doDebug = pref.shouldDebugSecondaryMethods(); break;
		case Variable : doDebug = pref.shouldDebugVariables(); break;
		case SQLRequest : doDebug = pref.shouldDebugSQL(); break;
		}	
		
		if ( doDebug ) {
			String msg;
			if ( message.substring(0, 1).equals("+") ) {
				msg = message.substring(1);
			} else if ( message.substring(0, 1).equals("-") ) {
				msg = message.substring(1);
				margin.setLength(Math.max(margin.length()-3,0));
			} else {
				msg = message;
			}
			
			System.out.println(margin.toString() + msg);
			if ( e != null )
				System.out.println(margin.toString() + e.getMessage());
			
			if ( message.substring(0, 1).equals("+") ) {
				margin.append("   ");
			}
		}
	}

	/**
	 * Shows up an on screen popup, displaying the message
	 * @param level
	 * @param msg
	 */
	public static void popup(Level level, String msg) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.popup("+level+","+msg+")");
		popup(level,msg,null);
	}
	
	/**
	 * Shows up an on screen popup, displaying the message and the exception message (if any).
	 * The exception stacktrace is also printed on the standard error stream
	 * @param level
	 * @param msg
	 * @param e
	 */
	public static void popup(Level level, String msg, Exception e) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.popup("+level+",\""+msg+"\","+(e==null?"null":e.getMessage())+")");
		String msg2 = msg;
		System.out.println(msg);
		if ( (e != null) && (e.getMessage()!=null)) {
			msg2 += "\n\n" + e.getMessage();
			e.printStackTrace(System.err);
		}
		switch ( level ) {
		case Info :
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), pluginTitle, msg2);
			break;
		case Warning :
			MessageDialog.openWarning(Display.getDefault().getActiveShell(), pluginTitle, msg2);
			break;
		case Error :
			MessageDialog.openError(Display.getDefault().getActiveShell(), pluginTitle, msg2);
			break;
		}
	}
	
	/**
	 * Shows up an on screen popup, displaying the question and the exception message (if any).
	 * The exception stacktrace is also printed on the standard error stream
	 * 
	 * @param msg
	 * @return true or false
	 */
	public static boolean question(String msg) {
		boolean result = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), pluginTitle, msg);
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.question(\""+msg+"\") -> "+result);
		return result;
	}
	
	/**
	 * @param _string
	 * @param _c
	 * @return the number of times a character is present in a string
	 */
	public static int count(String _string, char _c) {
		int count = 0;
		for (int i=0; i < _string.length(); i++)
			if (_string.charAt(i) == _c) count++;
		return count;
	}
	
	/**
	 * Wrapper to generate and execute a SELECT request in the database
	 * One may use '?' in the request and provide the corresponding values as parameters (at the moment, only strings are accepted)
	 * @param database
	 * @param request
	 * @param parameters
	 * @return the resultset with the data read from the database
	 * @throws Exception
	 */
	public static ResultSet select(Connection db, String request, String... parameters) throws Exception {
		PreparedStatement pstmt = db.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		
		StringBuilder debugRequest = new StringBuilder(request);
		for (int rank=0 ; rank < Math.min(parameters.length, count(request, '?')) ; rank++) {
			replaceFirst(debugRequest, "?", "\""+parameters[rank]+"\"");
			pstmt.setString(rank+1, parameters[rank]);
		}
		
		DBPlugin.debug(DebugLevel.SQLRequest, "DBPlugin.select(\""+debugRequest.toString()+"\")");
		ResultSet result = pstmt.executeQuery();
		return result;
	}
	
	/**
	 * wrapper to generate and execute a INSERT request in the database
	 * One may just provide the column names and the corresponding values as parameters
	 * the wrapper automatically generates the VALUES part of the request 
	 * @param database
	 * @param request
	 * @param parameters
	 * @throws Exception
	 */
	@SafeVarargs
	public static final <T> void insert(Connection db, String request, T...parameters) throws Exception {
		//DBPlugin.debug(DebugLevel.SQLRequest, "DBPlugin.insert(\""+request+"\")");
		for (int rank=0 ; rank < parameters.length ; rank++)
			request += rank == 0 ? " VALUES (?" : ",?";
		request += ")";
		request(db, request, parameters);
	}
	
	/**
	 * wrapper to execute an INSERT or UPDATE request in the database
	 * One may use '?' in the request and provide the corresponding values as parameters (strings, integers, booleans and byte[] are accepted)
	 * @param database
	 * @param request
	 * @param parameters
	 * @throws Exception
	 */
	@SafeVarargs
	public static final <T> void request(Connection db, String request, T... parameters) throws Exception {
		ByteArrayInputStream stream = null;
		
		PreparedStatement pstmt = db.prepareStatement(request);
		StringBuilder debugRequest = new StringBuilder(request);
		for (int rank=0 ; rank < parameters.length ; rank++) {
			if ( parameters[rank] == null ) {
				replaceFirst(debugRequest, "?", "null");
				pstmt.setString(rank+1, null);
			} else {
				if ( parameters[rank] instanceof String ) {
					replaceFirst(debugRequest, "?", "\""+parameters[rank]+"\"");
					pstmt.setString(rank+1, (String)parameters[rank]);
				} else if ( parameters[rank] instanceof Integer ) {
					replaceFirst(debugRequest, "?", String.valueOf((int)parameters[rank])+"");
					pstmt.setInt(rank+1, (int)parameters[rank]);
			    } else if ( parameters[rank] instanceof Boolean ) {
			    	replaceFirst(debugRequest, "?", String.valueOf((boolean)parameters[rank]));
					pstmt.setBoolean(rank+1, (boolean)parameters[rank]);
			    } else if ( parameters[rank] instanceof byte[] ) {
			    	try  {
			    		stream = new ByteArrayInputStream((byte[])parameters[rank]);
			    		pstmt.setBinaryStream(rank+1, stream, ((byte[])parameters[rank]).length);
			    		replaceFirst(debugRequest, "?", "<image as stream>");
			    	} catch (Exception err) {
			    		replaceFirst(debugRequest, "?", "<image as String>");
			    		pstmt.setString(rank+1, Base64.getEncoder().encodeToString((byte[])parameters[rank]));
			    	}
				}
				else {
					DBPlugin.popup(Level.Error, "DBPlugin.export : I do not understant what you want to export !!!");
					return;
				}
			}
		}
		
		debug(DebugLevel.SQLRequest, "DBPlugin.request(\""+debugRequest.toString()+"\")");
		
		pstmt.executeUpdate();
		pstmt.close();
		if ( stream != null )
			stream.close();
	}
	
	/**
	 * Increase the minor part of a version string.
	 * A version string is like 'major' dot 'minor'
	 * @param _version
	 * @return the new version string
	 */
	public static String incMinor(String _version) {
		String result;
		if ( _version != null ) {
			String version[] = _version.split("\\.");
			if ( version.length == 1 )
				result = version[0] + ".1";
			else
				result = version[0] + "." + String.valueOf(Integer.valueOf(version[1])+1);
		} else
			result = "0.1";
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.incMinor(\""+_version+"\") -> \""+result+"\"");
		return result;
	}
	
	/**
	 * Increase the major part of a version string.
	 * A version string is like 'major' dot 'minor'
	 * @param _version
	 * @return the new version string
	 */
	public static String incMajor(String _version) {
		String result;
		if ( _version != null ) {
			String version[] = _version.split("\\.");
			result = String.valueOf(Integer.valueOf(version[0])+1) + ".0";
		} else
			result = "1.0";
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.incMajor(\""+_version+"\") -> \""+result+"\"");
		return result;
		
	}
	
	/**
	 * Uppercase the first letter of all the words of a sentence (space separated) and concat them
	 * @param _phrase
	 * @return the generated word
	 */
	public static String capitalize(String _phrase) {
		if (_phrase.isEmpty()) return _phrase;
		StringBuilder result = new StringBuilder();
		for ( String s: _phrase.split(" ") )
			result.append(s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());
		return result.toString();
	}
	
	/**
	 * Checks if an ID contains the version of the object.
	 * 
	 * A standard internal Archi ID is compound uniquely of letters and numbers. 
	 * The database plugin changes the internal Archi IDs to store the former Archi ID, the project ID and the version of the Archi object, separating them with a dash "-".
	 * @param _id
	 * @return true if the ID contains a dash "-", else false
	 */
	public static boolean isVersionned(String _id ) {
		boolean result;
		if ( _id == null )
			result = false;
		else
			result = _id.contains(Separator);
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.isVersionned(\""+_id+"\") -> "+result);
		return result;
	}
	
	/**
	 * Generates a project ID by concatenating a projectID and a version, separating them with a dash "-"
	 * @param _projectId
	 * @param _version
	 * @return the generated ID
	 */
	public static String generateProjectId(String _projectId, String _version) {
		String result = _projectId + Separator + _version;
		
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.generateProjectId(\""+_projectId+"\",\""+_version+"\") -> \""+result+"\"");
		return result;
	}
	
	/**
	 * Generates an ID by concatenating an internal Archi ID, a projectID and a version, separating them with a dash "-"
	 * @param _projectId
	 * @param _version
	 * @return the generated ID
	 */
	public static String generateId(String _id, String _projectId, String _version) {
		String result;

		if ( _id == null ) 
			_id = UUID.randomUUID().toString().split(Separator)[0];
		if ( _projectId == null )
			result = _id;
		else {
			if ( _version == null )
				result = _id + Separator + _projectId;
			else
				result = _id + Separator + _projectId + Separator + _version;
		}
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.generateProjectId(\""+_id+"\",\""+_projectId+"\",\""+_version+"\") -> \""+result+"\"");
		return result;
	}
	
	/**
	 * Gets the internal Archi ID from a database plugin ID 
	 * @param _id
	 * @return the internal Archi ID
	 */
	public static String getId(String _id) {
		String result = null;
		if ( _id != null ) {
			if ( isVersionned(_id) )
				result = _id.split(Separator)[0];
			else
				result = _id;
		}
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.getId(\""+_id+"\") -> \""+result+"\"");
		return result;
	}
	
	/**
	 * Gets the project ID from a database plugin ID 
	 * @param _id
	 * @return the project ID
	 */
	public static String getProjectId(String _id) {
		String result;
		if ( isVersionned(_id) ) {
			String[] s = _id.split(Separator);
			result = s[s.length-2];
		} else
			result =_id;
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.getProjectId(\""+_id+"\") -> \""+result+"\"");
		return result;
	}
	
	/**
	 * Gets the version from a database plugin ID 
	 * @param _id
	 * @return the version
	 */
	public static String getVersion(String _id) {
		String result;
		if ( isVersionned(_id) ) {
			String[] s = _id.split(Separator);
			result = s[s.length-1];
		} else
			result = "0.0";
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.getVersion(\""+_id+"\") -> \""+result+"\"");
		return result;
	}
	
	/**
	 * Calculate a MD5 from a byte array
	 * 
	 * @param _input
	 * @return the calculated MD5
	 * @throws NoSuchAlgorithmException
	 */
	public static String getMD5(byte[] _input) throws NoSuchAlgorithmException {
	    String result = null;
	    if(_input != null) {
	    	MessageDigest md = MessageDigest.getInstance("MD5");
	    	md.update(_input);
	    	BigInteger hash = new BigInteger(1, md.digest());
	    	result = hash.toString(16);
	    	while(result.length() < 32) {
	    		result = "0" + result;
	    	}
	    }
	    DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.getMD5() -> \""+result+"\"");
	    return result;
	}
	
	/**
	 * Helper method to create a label widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param text
	 * @param style
	 * @return Label
	 */
	public static Label createLabel(Composite parent, int x, int y, int width, int heigth, String text, int style) {
		Label lbl = new Label(parent, style);
		if ( text != null )
			lbl.setText(text);
		lbl.setBounds(x, y, width, heigth);
		return lbl;
	}
	
	/**
	 * Helper method to create a text widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param text
	 * @param style
	 * @return Text
	 */
	public static Text createText(Composite parent, int x, int y, int width, int heigth, String text, int style) {
		Text txt = new Text(parent, style);
		if ( text != null )
			txt.setText(text);
		txt.setBounds(x, y, width, heigth);
		return txt;
	}
	
	/**
	 * Helper method to create a text widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param label
	 * @param textWidth
	 * @param text
	 * @param style
	 * @return Text
	 */
	public static Text createLabelledText(Composite parent, int x, int y, int width, int heigth, String label, int textWidth, int textHeight, String text, int style) {
		createLabel(parent, x, y, width, heigth, label, SWT.NONE);
		return createText(parent, x+width+5, y-3, textWidth, textHeight + 6, text, style);
	}
	
	/**
	 * Helper method to create a combo widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param values
	 * @param style
	 * @return Combo
	 */
	public static Combo createCombo(Composite parent, int x, int y, int width, int heigth, String[] values, int style) {
		Combo cmb = new Combo(parent, style);
		if ( values != null )
			cmb.setItems(values);
		cmb.setBounds(x, y, width, heigth);
		cmb.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
					// the only way to change the value is to choose it in the list
				if ( e.character != 0 )
					e.doit = false;
			}
		});
		return cmb;
	}
	

	/**
	 * Helper method to create a button widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param text
	 * @param style
	 * @param callback
	 * @return Button
	 */
	public static Button createButton(Composite parent, int x, int y, int width, int heigth, String text, int style, SelectionListener callback) {
		Button btn = new Button(parent, style);
		if ( text != null )
			btn.setText(text);
		btn.setBounds(x, y, width, heigth);
		if ( callback != null )
			btn.addSelectionListener(callback);
		return btn;
	}
	
	/**
	 * Helper method to create a group widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param style
	 * @return Group
	 */
	public static Group createGroup(Composite parent, int x, int y, int width, int heigth, int style) {
		Group grp = new Group(parent, style);
		grp.setBounds(x, y, width, heigth);
		grp.setLayout(null);
		return grp;
	}
	
	/**
	 * Helper method to create a composite widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param style
	 * @return Composite
	 */
	public static Composite createComposite(Composite parent, int x, int y, int width, int heigth, int style) {
		Composite compo = new Composite(parent, style);
		compo.setBounds(x, y, width, heigth);
		return compo;
	}
	
	/**
	 * Helper method to create a scrolledComposite widget
	 * 
	 * @param parent
	 * @param x
	 * @param y
	 * @param width
	 * @param heigth
	 * @param style
	 * @return Composite
	 */
	public static ScrolledComposite createScrolledComposite(Composite parent, int x, int y, int width, int heigth, int style) {
		ScrolledComposite compo = new ScrolledComposite(parent, style);
		compo.setBounds(x, y, width, heigth);
		compo.setAlwaysShowScrollBars(true);
		compo.setExpandHorizontal(true);
		compo.setExpandVertical(true);
		return compo;
	}
	
	public static Connection openConnection(String driver, String server, String port, String database, String username, String password) throws SQLException, ClassNotFoundException {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBPlugin.openConnection(\""+driver+"\",\""+server+"\",\""+port+"\",\""+database+"\",\""+username+"\",\""+password+"\")");
		Connection db = null;
		try {
			switch (driver.toLowerCase()) {
			case "neo4j"      : Class.forName("org.neo4j.jdbc.Driver");           db = DriverManager.getConnection("jdbc:neo4j:bolt://" + server + ":" + port, username, password); break;
			case "postgresql" : Class.forName("org.postgresql.Driver");           db = DriverManager.getConnection("jdbc:postgresql://" + server + ":" + port + "/" + database, username, password);  break;
			case "mysql"      : Class.forName("com.mysql.jdbc.Driver");           db = DriverManager.getConnection("jdbc:mysql://" + server + ":" + port + "/" + database+"?useSSL=false", username, password);  break;
			case "oracle"     : Class.forName("oracle.jdbc.driver.OracleDriver"); db = DriverManager.getConnection("jdbc:oracle:thin:@" + server + ":" + port+ ":" + database, username, password); break;
			case "sqlite"     : Class.forName("org.sqlite.JDBC");                 db = DriverManager.getConnection("jdbc:sqlite:"+server);
			}
		} catch (SQLException err) {
			// JDBC tries all the drivers until one succeeds to connect to the database
			// if the selected database cannot be joined, then the error message is the message sent by the last driver tested
			// which is "JDBC URL is not correct." as it does not recognize the URL ...
			if ( err.getMessage().startsWith("JDBC URL is not correct.") ) {
				throw new SQLException("Cannot connect to the database", (Exception)null);
			}
			throw err;
		}
		db.setAutoCommit(true);
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBPlugin.openConnection(\""+driver+"\",\""+server+"\",\""+port+"\",\""+database+"\",\""+username+"\",\""+password+"\") -> ok");
		return db;
		
	}
	
	public static String getDbLanguage(String driver) {
		String result;
		if ( driver.toLowerCase().equals("neo4j"))
			result = "CQL";
		else
			result = "SQL";
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBPlugin.getDbLanguage(\""+driver+"\") -> \""+result+"\"");
		return result;
	}
	
	public static String getEventName(int eventType) {
	    switch(eventType) {
	      case SWT.None:
	        return "null";
	      case SWT.KeyDown:
	        return "key down";
	      case SWT.KeyUp:
	        return "key up";
	      case SWT.MouseDown:
	        return "mouse down";
	      case SWT.MouseUp:
	        return "mouse up";
	      case SWT.MouseMove:
	        return "mouse move";
	      case SWT.MouseEnter:
	        return "mouse enter";
	      case SWT.MouseExit:
	        return "mouse exit";
	      case SWT.MouseDoubleClick:
	        return "mouse double click";
	      case SWT.Paint:
	        return "paint";
	      case SWT.Move:
	        return "move";
	      case SWT.Resize:
	        return "resize";
	      case SWT.Dispose:
	        return "dispose";
	      case SWT.Selection:
	        return "selection";
	      case SWT.DefaultSelection:
	        return "default selection";
	      case SWT.FocusIn:
	        return "focus in";
	      case SWT.FocusOut:
	        return "focus out";
	      case SWT.Expand:
	        return "expand";
	      case SWT.Collapse:
	        return "collapse";
	      case SWT.Iconify:
	        return "iconify";
	      case SWT.Deiconify:
	        return "deiconify";
	      case SWT.Close:
	        return "close";
	      case SWT.Show:
	        return "show";
	      case SWT.Hide:
	        return "hide";
	      case SWT.Modify:
	        return "modify";
	      case SWT.Verify:
	        return "verify";
	      case SWT.Activate:
	        return "activate";
	      case SWT.Deactivate:
	        return "deactivate";
	      case SWT.Help:
	        return "help";
	      case SWT.DragDetect:
	        return "drag detect";
	      case SWT.Arm:
	        return "arm";
	      case SWT.Traverse:
	        return "traverse";
	      case SWT.MouseHover:
	        return "mouse hover";
	      case SWT.HardKeyDown:
	        return "hard key down";
	      case SWT.HardKeyUp:
	        return "hard key up";
	      case SWT.MenuDetect:
	        return "menu detect";
	    }
	    return "unkown";
	}
	
	public static void replaceFirst(StringBuilder str, String searched, String replaced) {
		int index = str.indexOf(searched);
		if ( index != -1 ) {
			str.replace(index, index+searched.length(), replaced);
		}
	}
}