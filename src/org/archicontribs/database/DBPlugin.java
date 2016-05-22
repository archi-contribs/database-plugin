/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Database Model Exporter
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
 */
public class DBPlugin {
	public static String pluginVersion = "0.6";
	public static String pluginName = "DatabasePlugin";
	public static String pluginTitle = "Database import/export plugin v" + pluginVersion;
	public static String Separator = "-";
	
	public static String[] allTables = { "archimatediagrammodel", "archimateelement", "canvasmodel", "canvasmodelblock", "canvasmodelsticky", "diagrammodelarchimateconnection", "diagrammodelarchimateobject", "folder", "model",  "point", "property", "relationship" };

	public enum Level { Info, Warning, Error };
	
	public static String SharedModelId = "Shared-0.0";
	public static String SharedFolderName = "Models";
	public static String ExternalFolderName = "External Elements";

	private static boolean showDebug = false;
	
	public static void debug(Object _obj){
		if ( showDebug ) System.out.println(_obj);
	}
	
	public static void popup(Level level, String msg) {
		popup(level,msg,null);
	}
	public static void popup(Level level, String msg, Exception e) {
		String msg2 = msg;
		if ( e != null) msg2 += "\n\n" + e.getMessage();
		debug(msg2);
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
		if ( e != null ) 
			e.printStackTrace(System.err);
	}
	public static int count(String _string, char _c)
	{
	    int count = 0;
	    for (int i=0; i < _string.length(); i++)
	        if (_string.charAt(i) == _c) count++;
	    return count;
	}
	public static int max(int _a, int _b) {
		return _a > _b ? _a : _b;
	}
	public static int min(int _a, int _b) {
		return _a < _b ? _a : _b;
	}
	public static void sql(Connection db, String request, String... parameters) throws SQLException {
		PreparedStatement pstmt = db.prepareStatement(request);
		for (int rank=0 ; rank < min(parameters.length, count(request, '?')) ; rank++)
			pstmt.setString(rank+1, parameters[rank]);
		//debug(pstmt.toString());
		pstmt.executeUpdate();
		pstmt.close();
	}
	public static ResultSet select(Connection db, String request, String... parameters) throws SQLException {
		PreparedStatement pstmt = db.prepareStatement(request, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		for (int rank=0 ; rank < min(parameters.length, count(request, '?')) ; rank++)
			pstmt.setString(rank+1, parameters[rank]);
		//debug(pstmt.toString());
		return pstmt.executeQuery();
	}
	@SafeVarargs
	public static final <T> void update(Connection db, String request, T...args) throws SQLException {
		for (int rank=0 ; rank < args.length ; rank++) request += rank == 0 ? " VALUES (?" : ",?";
		request += ")";

		PreparedStatement pstmt = db.prepareStatement(request);
		for (int rank=0 ; rank < args.length ; rank++) {
			if ( args[rank] == null ) {
				pstmt.setString(rank+1, null);
			} else {
				if ( args[rank] instanceof String )
					pstmt.setString(rank+1, (String)args[rank]);
				else if ( args[rank] instanceof Integer )
					pstmt.setInt(rank+1, (int)args[rank]);
				else if ( args[rank] instanceof Boolean )
					pstmt.setBoolean(rank+1, (boolean)args[rank]);
				else 
					DBPlugin.popup(Level.Error, "heinnn ???");
			}
		}
		//debug(pstmt.toString());
		pstmt.executeUpdate();
		pstmt.close();
	}
	public static String incMinor(String _version) {
		if ( _version != null ) {
			String version[] = _version.split("\\.");
			return version[0] + "." + String.valueOf(Integer.valueOf(version[1])+1);
		}
		return "0.1";
	}
	public static String incMajor(String _version) {
		if ( _version != null ) {
			String version[] = _version.split("\\.");
			return String.valueOf(Integer.valueOf(version[0])+1) + ".0";
		}
		return "1.0";
	}
    public static String capitalize(String phrase) {
        if (phrase.isEmpty()) return phrase;
        StringBuilder result = new StringBuilder();
        for ( String s: phrase.split(" ") )
        	result.append(s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase());
        return result.toString();
    }
    public static boolean isVersionned(String _id ) {
    	if ( _id == null ) return false;
    	return _id.contains(Separator);
    }
    public static String generateModelId(String _modelId, String _version) {
    	return _modelId+Separator+_version;
    }
    public static String generateId(String _id, String _modelId, String _version) {
    	if ( _modelId == null ) return _id;
    	if ( _id == null ) return UUID.randomUUID().toString().split("-")[0]+Separator+_modelId+Separator+_version;
    	return _id+Separator+_modelId+Separator+_version;
    }
    public static String getId(String _id) {
    	if ( isVersionned(_id) )
    		return _id.split(Separator)[0];
    	return _id;
    }
    public static String getModelId(String _id) {
    	if ( isVersionned(_id) ) {
    		String[] s = _id.split(Separator);
    		return s[s.length-2];
    	}
    	return _id;
    }
    public static String getVersion(String _id) {
    	if ( isVersionned(_id) ) {
    		String[] s = _id.split(Separator);
    		return s[s.length-1];
    	}
    	return "0.0";
    }
}