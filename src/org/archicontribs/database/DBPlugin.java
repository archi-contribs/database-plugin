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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.model.util.Logger;

/**
 * Database Model Exporter
 * 
 * @author Herve Jouin
 */
public class DBPlugin {
	public static String pluginVersion = "0.2";
	public static String pluginName = "DatabasePlugin";
	public static String pluginTitle = "Database import/export plugin v" + pluginVersion;
	public static String Separator = "-";

	public enum Level { Info, Warning, Error };

	public static void popup(Level level, String msg) {
		popup(level,msg,null);
	}
	public static void popup(Level level, String msg, Exception e) {
		String msg2 = msg;
		if ( e != null) msg2 += "\n\n" + e.getMessage();

		switch ( level ) {
		case Info :
			//Logger.logInfo(msg, e);
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), pluginTitle, msg2);
			break;
		case Warning :
			Logger.logWarning(msg, e);
			MessageDialog.openWarning(Display.getDefault().getActiveShell(), pluginTitle, msg2);
			break;
		case Error :
			Logger.logError(msg, e);
			MessageDialog.openError(Display.getDefault().getActiveShell(), pluginTitle, msg2);
			break;
		}
	}
	public static void sql(Connection db, String request, String... parameters) throws SQLException {
		PreparedStatement pstmt = db.prepareStatement(request);
		for (int rank=0 ; rank < parameters.length ; rank++)
			pstmt.setString(rank+1, parameters[rank]);
		pstmt.executeUpdate();
		pstmt.close();
	}
	public static ResultSet select(Connection db, String request, String... parameters) throws SQLException {
		PreparedStatement pstmt = db.prepareStatement(request, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_INSENSITIVE);
		for (int rank=0 ; rank < parameters.length ; rank++)
			pstmt.setString(rank+1, parameters[rank]);
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
				else
					pstmt.setInt(rank+1, (int)args[rank]);
			}
		}
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
}