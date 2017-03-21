/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.File;
import java.text.Collator;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Database Model Importer / Exporter
 * 
 * The DBPlugin class implements static methods and properties used everywhere else in the database plugin. 
 * 
 * @author Herve Jouin
 *
 * v0.1 : 			25/03/2016		plug-in creation
 * v1.0.0 :			10/12/2016		First production release
 * v2.0.0.beta1 :	26/02/2017		Added log4j support
 * 									Version all the elements and relationships
 * 									Reduce the quantity of data exported by exporting only updated components (use of checksums)
 * 									Detect database conflicts and add a conflict resolution mechanism
 * 									Reduce number of database tables
 * 									Reduce database table names to be compliant will all database brands
 * 									Add back Oracle JDBC driver
 * 									Temporarily remove the Neo4j driver
 * 									Accelerate import and export processes by using multi-threading
 * 									Complete rework of the graphical interface
 * 									Add the ability to import components from other models
 * 									Add inline help
 * v2.0.0.beta2 :	19/03/2017		Importing an element now imports its relationships as well
 * 									Add import folder functionality
 * 									Add import view functionality
 * 									Change RCP methods to insert entries in menus in order to be more friendly with other plugins
 * 									Solve a bug with MySQL databases for which aliases in SQL joins are mandatory
 * 									Solve a bug in progressBar which did not represent 100%
 * 									Launch the import process on double-click in the model list table
 * 									The ID is now shown in right menu only in debug mode
 *									Few java optimizations
 *									Improve exceptions catching between threads
 *									Replace boolean database columns by integer columns for better compatibility
 * 
 * 									// todo : dynamically load jdbc drivers
 * 									// todo : add datamodel management to preferences window
 * 									// todo : add an option to check if there is no missing relationship in the model comparing to the database (may be do this during import as well)
 * 									// todo : work with dates only (no more version numbers as they are useless)
 * 									// todo : add more jdbc drivers (odbc, mongodb, etc ...)
 * 									// todo : add an option "use model name as id" --> when changing model name, then change its id (kind of duplicate option)
 */
public class DBPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.archicontribs.database";
	
	public static final String pluginVersion = "2.0.0.beta3";
	public static final String pluginName = "DatabasePlugin";
	public static final String pluginTitle = "Database import/export plugin v" + pluginVersion;
	public static final String Separator = ";";

	/**
	 * static instance that allow to keep information between calls of the plugin
	 */
	public static DBPlugin INSTANCE;
	public static Collator collator = Collator.getInstance();
	
	/**
	 * PreferenceStore allowing to store the plugin configuration.
	 */
	private static IPreferenceStore preferenceStore = null;

	/**
	 * Name of all the table names in a SQL database
	 */
	public static String[] allSQLTables = { "archimatediagrammodel", "archimateelement", "bendpoint", "canvasmodel", "canvasmodelblock", "canvasmodelimage", "canvasmodelsticky", "connection", "diagrammodelarchimateobject", "diagrammodelreference", "folder", "model", "property", "relationship", "sketchmodel", "sketchmodelactor", "sketchmodelsticky"};

	private DBLogger logger;
	
	public static final Cursor CURSOR_WAIT = new Cursor(null, SWT.CURSOR_WAIT);
	public static final Cursor CURSOR_ARROW = new Cursor(null, SWT.CURSOR_ARROW);
	
	
	public DBPlugin() {
        INSTANCE = this;
        preferenceStore = this.getPreferenceStore();
		preferenceStore.setDefault("progressWindow",	"showAndWait");
		preferenceStore.setDefault("loggerMode",		"disabled");
		preferenceStore.setDefault("loggerLevel",		"INFO");
		preferenceStore.setDefault("loggerFilename",	System.getProperty("user.home")+File.separator+pluginName+".log");
		preferenceStore.setDefault("loggerExpert",		"log4j.rootLogger                               = INFO, stdout, file\n"+
														"\n"+
														"log4j.appender.stdout                          = org.apache.log4j.ConsoleAppender\n"+
														"log4j.appender.stdout.Target                   = System.out\n"+
														"log4j.appender.stdout.layout                   = org.apache.log4j.PatternLayout\n"+
														"log4j.appender.stdout.layout.ConversionPattern = %d{yyyy-MM-dd HH:mm:ss} %-5p %4L:%-30.30C{1} %m%n\n"+
														"\n"+
														"log4j.appender.file                            = org.apache.log4j.FileAppender\n"+
														"log4j.appender.file.ImmediateFlush             = true\n"+
														"log4j.appender.file.Append                     = false\n"+
														"log4j.appender.file.Encoding                   = UTF-8\n"+
														"log4j.appender.file.File                       = "+(System.getProperty("user.home")+File.separator+pluginName+".log").replace("\\", "\\\\")+"\n"+
														"log4j.appender.file.layout                     = org.apache.log4j.PatternLayout\n"+
														"log4j.appender.file.layout.ConversionPattern   = %d{yyyy-MM-dd HH:mm:ss} %-5p %4L:%-30.30C{1} %m%n");
		logger = new DBLogger(DBPlugin.class);
		logger.info("Initialising "+pluginName+" plugin ...");
    }
	
	@Override
	public IPreferenceStore getPreferenceStore() {
	    if (preferenceStore == null) {
	        preferenceStore = new ScopedPreferenceStore( InstanceScope.INSTANCE, PLUGIN_ID );
	    }
	    return preferenceStore;
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
	
	public static void replaceFirst(StringBuilder str, String searched, String replaced) {
		int index = str.indexOf(searched);
		if ( index != -1 ) {
			str.replace(index, index+searched.length(), replaced);
		}
	}
	
	/**
	 * Helper method to create a Label widget
	 */
	public static Label createLabel(Composite parent, int style, String txt, Object top, int topMargin, int topStyle, Object left, int leftMargin, int leftStyle) {
		FormData formData = new FormData();
		
		if ( top instanceof Control )
			formData.top = new FormAttachment((Control)top, topMargin, topStyle);
		else
			if ( (int)top != -1 ) formData.top = new FormAttachment((int)top, topMargin);
		
		if ( left instanceof Control )
			formData.left = new FormAttachment((Control)left, leftMargin, leftStyle);
		else
			if ( (int)left != -1 ) formData.left = new FormAttachment((int)left, leftMargin);

		Label label = new Label(parent, style);
		label.setLayoutData(formData);
		if ( txt != null ) label.setText(txt);
		
		return label;
	}
	
	/**
	 * Helper method to create a Label widget
	 */
	public static Label createLabel(Composite parent, int style, String txt, Object top, int topMargin, int topStyle, Object bottom, int bottomMargin, int bottomStyle, Object left, int leftMargin, int leftStyle, Object right, int rightMargin, int rightStyle) {
		FormData formData = new FormData();
		
		if ( top instanceof Control )
			formData.top = new FormAttachment((Control)top, topMargin, topStyle);
		else
			if ( (int)top != -1 ) formData.top = new FormAttachment((int)top, topMargin);
		
		if ( bottom instanceof Control )
			formData.bottom = new FormAttachment((Control)bottom, bottomMargin, bottomStyle);
		else
			if ( (int)bottom != -1 ) formData.bottom = new FormAttachment((int)bottom, bottomMargin);
		
		if ( left instanceof Control )
			formData.left = new FormAttachment((Control)left, leftMargin, leftStyle);
		else
			if ( (int)left != -1 ) formData.left = new FormAttachment((int)left, leftMargin);
		
		if ( right instanceof Control )
			formData.right = new FormAttachment((Control)right, rightMargin, rightStyle);
		else
			if ( (int)right != -1 ) formData.right = new FormAttachment((int)right, rightMargin);
		
		Label label = new Label(parent, style);
		label.setLayoutData(formData);
		if ( txt != null ) label.setText(txt);
		
		return label;
	}
	
	/**
	 * Helper method to create a Text widget
	 */
	public static Text createText(Composite parent, int style, String txt, Object top, int topMargin, int topStyle, Object left, int leftMargin, int leftStyle, boolean enabled) {
		FormData formData = new FormData();
		
		if ( top instanceof Control )
			formData.top = new FormAttachment((Control)top, topMargin, topStyle);
		else
			if ( (int)top != -1 ) formData.top = new FormAttachment((int)top, topMargin);
		
		if ( left instanceof Control )
			formData.left = new FormAttachment((Control)left, leftMargin, leftStyle);
		else
			if ( (int)left != -1 ) formData.left = new FormAttachment((int)left, leftMargin);
		
		Text text = new Text(parent, style);
		if ( (style & SWT.V_SCROLL) != 0 ) {
			text.addListener(SWT.Resize, scrollBarListener);
			text.addListener(SWT.Modify, scrollBarListener);
			text.setFocus();
		}
		text.setEnabled(enabled);
		text.setLayoutData(formData);
		if ( txt != null ) text.setText(txt);
		return text;
	}

	/**
	 * Helper method to create a Text widget
	 */
	public static Text createText(Composite parent, int style, String txt, Object top, int topMargin, int topStyle, Object bottom, int bottomMargin, int bottomStyle, Object left, int leftMargin, int leftStyle, Object right, int rightMargin, int rightStyle, boolean enabled) {
		FormData formData = new FormData();
		
		if ( top instanceof Control )
			formData.top = new FormAttachment((Control)top, topMargin, topStyle);
		else
			if ( (int)top != -1 ) formData.top = new FormAttachment((int)top, topMargin);
		
		if ( bottom instanceof Control )
			formData.bottom = new FormAttachment((Control)bottom, bottomMargin, bottomStyle);
		else
			if ( (int)bottom != -1 ) formData.bottom = new FormAttachment((int)bottom, bottomMargin);
		
		if ( left instanceof Control )
			formData.left = new FormAttachment((Control)left, leftMargin, leftStyle);
		else
			if ( (int)left != -1 ) formData.left = new FormAttachment((int)left, leftMargin);
		
		if ( right instanceof Control )
			formData.right = new FormAttachment((Control)right, rightMargin, rightStyle);
		else
			if ( (int)right != -1 ) formData.right = new FormAttachment((int)right, rightMargin);
		
		Text text = new Text(parent, style);
		if ( (style & SWT.V_SCROLL) != 0 ) {
			text.addListener(SWT.Resize, scrollBarListener);
			text.addListener(SWT.Modify, scrollBarListener);
			text.setFocus();
		}
		text.setEnabled(enabled);
		text.setLayoutData(formData);
		if ( txt != null ) text.setText(txt);
		return text;
	}
	
	/**
	 * Listener called each time the release note text widget is modified or resized in order to show or hide the scrollbars.
	 */
	private static Listener scrollBarListener = new Listener() {
		public void handleEvent(Event event) {
			Text t = (Text)event.widget;
			Rectangle r1 = t.getClientArea();
			Rectangle r2 = t.computeTrim(r1.x, r1.y, r1.width, r1.height);
			Point p = t.computeSize(SWT.DEFAULT,  SWT.DEFAULT,  true);
			t.getHorizontalBar().setVisible(r2.width <= p.x);
			t.getVerticalBar().setVisible(r2.height <= p.y);
			if (event.type == SWT.Modify) {
				t.getParent().layout(true);
				t.showSelection();
			}
		}
	};
	
	/**
	 * Check if two strings are equals
	 */
	public static boolean areEqual(String str1, String str2) {
		if ( str1 == null )
			return str2 == null;
		
		if ( str2 == null )
			return false;			// as str1 cannot be null at this stage
		
		return str1.equals(str2);
	}
	
	/**
	 * Check if two strings are equals (ignore case)
	 */
	public static boolean areEqualIgnoreCase(String str1, String str2) {
		if ( str1 == null )
			return str2 == null;
		
		if ( str2 == null )
			return false;			// as str1 cannot be null at this stage
		
		return str1.equalsIgnoreCase(str2);
	}
	
	/**
	 * Exception raised during an asynchronous thread
	 */
	private static Exception asyncException = null;
	
	/**
	 * Gets the latest exception raised during an asynchronous thread
	 */
	public static Exception getAsyncException() {
		return asyncException;
	}
	
	/**
	 * Set an exception during an asynchronous thread
	 */
	public static void setAsyncException(Exception e) {
		asyncException = e;
	}
}