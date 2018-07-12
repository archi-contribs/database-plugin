/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.text.Collator;
import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;


/**
 * Database Model Importer / Exporter
 * 
 * The DBPlugin class implements static methods and properties used everywhere else in the database plugin. 
 * 
 * @author Herve Jouin
 *
 * v0.1: 			25/03/2016		plug-in creation
 * 
 * v1.0.0:			10/12/2016		First production release
 * 
 * v2.0.0.beta1:	26/02/2017		Added log4j support
 * 									Version all the elements and relationships
 * 									Reduce the quantity of data exported by exporting only updated components (use of checksums)
 * 									Detect database conflicts and add a conflict resolution mechanism
 * 									Reduce number of database tables
 * 									Reduce database table names to be compliant will all database brands
 * 									Add back Oracle JDBC driver
 * 									Temporarily remove the Neo4j driver
 * 									Accelerate import and export processes by using multi-threading
 * 									Complete rework of the graphical interface
 * 									Add the ability to Import components from database from other models
 * 									Add inline help
 * 
 * v2.0.0.beta2:	19/03/2017		Importing an element now imports its relationships as well
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
 * v2.0.0.beta3:	21/03/2017		Correct preference page where some options are outside the window on some displays
 * 									Solve SQL duplicate key error message on model export
 * 
 * v2.0.0.beta4:	29/03/2017		Correct import folders properties
 * 									Add version numbers of imported objects in debug mode
 * 									Solve MySQL compatibility issue on elements import SQL request
 * 									Detect folders and views changes using checksum
 * 									Solve bug where save button does not show up in preferences page
 * 
 * v2.0.0:         28/04/2017		Export Model:
 *                                     Solve bug where properties were not exported correctly
 *                                     Solve bug where connections could be exported twice
 *                                     Rewrite the conflict detection when exporting to make it more accurate
 *                                     Create the status page on the export model window
 *                                     Add a popup when nothing needs to be exported
 *                                  Import Model:
 *                                     Create the status page on the export model window
 *                                  Import components from database:
 *                                     Add the ability to hide existing components in the import component module
 *                                     Add the ability to hide default views in the import component module
 *                                  Get component history:
 *                                     Solve bug where the component was not found in the database
 *                                  Preferences:
 *                                     Add a preference entry to automatically close import and export windows on success
 *                                     Add a preference entry to automatically start to export the model to the default database
 *                                     Add a preference entry to automatically download and install the plugin updates
 *                                     Add a preference entry to Import components from database in shared or copy mode by default
 *                                  Miscellaneous:
 *                                     Solve bug in the logger where some multi-lines messages were not printed correctly
 *                                     From now on, folders and views have got their own version number
 *                                     Increase performance by reusing compiled SQL requests
 *                                     Check database version before using it
 *                                     Automatically create database tables if they do not exist
 *                                     Stop replacing the folders type
 *                                     Replaced database table "archi_plugin" by new table "database_version"
 *                                     Opens the model in the tree after import
 *                                     
 * v2.0.1: 01/05/2017              Add the ability to export images of views in the database
 * 									Add a preference to keep the imported model even in case of error
									Add SQL Server integrated security authentication mode
 * 									Reduce memory leak
 * 									Added back Neo4J support (elements and relationships export only)
 * 									Solve NullPointerException while checking database
 * 
 * v2.0.2: 02/05/2017              Solve errors during table creation in PostGreSQL database
 *                                  Solve "Operation not allowed after ResultSet closed" error message on model export
 *                                  Add a menu entry to replace old fashion IDs to Archi 4 IDs (to ensure uniqueness of all components)
 *                                  
 * v2.0.3: 07/05/2017              Export model:
 *                                  	Make conflict management more reliable on PostGreSQL databases
 *                                 		Added a preference to remove the dirty flag on the model after a successful export
 *                                 		Solve bug where count of exported components could be erroneous
 *									Import components from database:
 *										Added missing "location" class
 *                                  	Add the ability to import several components at the same time
 *                                  	The components are now sorted alphabetically
 *										Solve bug where the same component could be imported several times
 *                                  Miscellaneous:
 * 										Allow to specify a database schema in the database configuration
 * 										It is now possible to check a database connection without the need to edit their details
 *                                	    Reduce memory consumption
 *                                      Remove the NOT NULL constraints on some columns because Oracle does not do any difference between an empty string and a null value
 *                                      Renamed mssql driver to ms-sql to be visually more distinctive from mysql
 * 
 * v2.0.4: 11/05/2017				Export model:
 * 										Solve bug where export conflicts were not detected correctly on MySQL databases
 *                                  Import component:
 *                                  	The import type (shared or copy) can now be changed directly on the import window
 *									Preference page:
 *									    Correct traversal order of fields on preference page
 *									    The default database port is automatically filled-in when port field is empty or equals 0
 *									    The default for new databases is to not export view snapshots
 *									    When saving preferences while editing database properties, the plugin now asks if the updates need to be saved or discarded
 *                                  Miscellaneous:
 *                                  	Rewrite of the checksum calculation procedure to ensure it is always the same length
 *                                  	Rewrite of the views connections import procedure to ensure that they are imported in the same order as in the original model
 *                                  	This leads to 2 new columns (source_connections and target_connections) in the view_objects and views_connections database tables
 *                                  
 * v2.0.5: 17/05/2017				Export model:
 * 										Change order of folders export (exporting all the root folders first)
 * 									Import model:
 * 										Solve bug in counting components to import prior the import itself which can cause false error messages even when the import is successful
 * 										Solve bug in folder import where the parent folder was not created yet before its content
 * 
 * v2.0.6: 30/05/2017				Import model:
 * 										Solve bug when importing a model which has got a shared view which has been updated by another model
 * 										The import SQL request have been rewritten because of Oracle specificity
 * 										A double click on a model's version now launches the import
 *									Import components from database:
 *										Solve bug where all the views versions were added in the table, resulting in several entries with the same name
 * 									Database model:
 * 										Added column "element_version" to table "views_objects"
 * 										Added column "relationship_version" to table "views_connections"
 * 
 * v2.0.7: 30/06/2017				Rollback to single thread as multi-threading causes to many side effects and does not accelerate the import and export duration as expected
 *                                  Improve checksum mechanism
 *                                  Add an option to show up ID and checksum in context menu rather than relying on the logger mode
 *                                  Import model:
 *                                      Solve bug where the filter field was not working as expected
 *                                      Change the filter request to be case insensitive
 *                                  Export model:
 *                                      Use of a Tree rather than a Table to show up conflicts
 *                                      Show up more information about conflicting components
 *                                      Increase the conflict detection and resolution
 * 										
 * v2.0.7b: 01/07/2017				Solve Neo4J errors
 * 
 * v2.1: 12/07/2018					Import components from database:
 * 										Rename "import individual component" to "import components"
 * 										Added documentation column to help distinguish components having the same name
 *                                      Added tooltip with properties to help distinguish components having the same name
 * 										Added message during the import as it may take some time
 * 										Use commands to allow undo/redo
 *                                      Added a label to explain that the icons can be selected
 *                                      The categories can now be clicked to select/unselect the whole category
 *                                      The component is imported by default in the selected folder
 *                                      The element class is pre-selected depending on the selected folder
 *                                      In case one view is selected for import, show view screenshot if available in the database
 *                                      Introduce new template mode that mixes shared and copy modes depending on each component properties
 *                                      Possibility to import a whole model into another one (merge models)
 *                                  Import model:
 *                                  	A context menu entry allowin got import a model has been added when no model is selected
 *                                      Automatically open the default view (if any) at the end of the import
 *                                      Fix number of images to import
 * 									Export model:
 *										For relational databases:
 *											The export is now in "collaborative mode", which syncs the model with the database:
 *												- It can be compared to a pull+push to GitHub.
 * 												- It is slower than the previous mode but allows several people to work on the same model at the same time
 * 											Allow to specify border width and scale factor for views screenshots
 *											To simplify, it is no more possible to choose between whole export or elements and relationships only
 *										For Neo4J databases:
 *											Create two export modes: native and extended
 *											New option to empty the database before the export
 *											New option to specialize relationships
 *                                      Rewrite version management (check timestamps in addition of the version number)
 *                                      Remove the name, the documentation and the properties from view objects and connections checksum calculation as they are related to the corresponding concept
 *                                  Plugin preferences:
 *                                      The default import mode (shared or copy) has been removed as the default import mode is now the template mode
 *                                      Add an option to compare the model from the database before exporting it
 *										Allow to specify the generated view screenshots border width and scale factor
 *										Allow to specify a suffix to add to components imported in copy mode
 *										Add an option to check for max memory at startup (Xmx should be set to 1g)
 *										Add an option to show or hide zero values in import and export windows
 *									Get history from database:
 *										Allows to get history for diagrams, canvas and sketches
 *										Allows to export/import component to/from the database directly from the history window
 *									Other:
 *										Bug fixes:
 *											Exporting blocks or images objects with no images set does not generate errors anymore
 * 											Fix plugin initialization failure that occurred some times
 * 											Fix progress bar during download new version of the plugin from GitHub
 * 											Increase compiler severity to maximum and resolve all the warnings to improve code resiliency
 * 											Reduce memory leak
 * 											Fix centering of GUI windows, especially on hiDPI displays
 * 											Fix calculation of numbers of images to import
 *											Better management of the cancel button during the import and export process
 *											Cleanup properties before import rather than update existing values as several properties can have the same name
 *											Fix centering of GUI windows especially on HiDPI displays
 *										Improvements:
 *											Fill in the online help pages
 *                                  		Rewrite debug and trace messages to be more useful
 *											Add the ability to import an image from the database (on the Image and Block objects in Canvas)
 *                                  		Some annoying popups have been replaced by messages directly in the import/export window
 *											Remove the name, the documentation and the properties from view objects and connections checksum as they are not related to the view objects and connections themselves, but to the related element or relationship
 * 											Add procedures that can be called by the script plugin
 * 											The inline help can be accessed using the interrogation mark on every plugin window.
 * 											Export and import back the model's "metadata" (may be used by other external tools)
 * 											Do not calculate checksum on images anymore as the path is already a kind of checksum
 * 											A new "show debug information" window has been created
 *                                  		Add "import model from database" menu entry on right click when no model has been loaded yet
 *                                  		Manage the objects transparency (introduced in Archi 4.3)
 *                                  		Check for max memory available at startup and suggest to increase it (Xmx parameter) if less than 1 GB
 *                                  		Add the ability to import an image from the database on Canvas Image and Block objects
 * 											Update JDBC drivers
 * 												- Neo4J to 3.1.0
 * 												- SQLite to 3.21.0
 *												- PostGreSQL to 42.2.1
 * 
 * Known bugs:
 * -----------
 *		Import components from database:
 *		     importing elements "in a view" create all the corresponding objects in the top left corner of the view
 *
 * TO-DO list:
 * ----------
 *		Import components from database:
 *			allow to import elements recursively (with depth limit)
 *		Get component history:
 *			show view screenshots to ease views comparison
 *		Miscellaneous:
 *			add an option to check for relationships that are in the database but would not be in the in memory model
 *				--> nearly done with "get history from database". Just needs to compare the list of relationships
 *			create a new windows that will show up detailed statistics about the model
 *			add more jdbc drivers (mongodb, odbc, etc ...)
 *          add an option to duplicate a model
 *			create database admin procedures
 */
public class DBPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.archicontribs.database";

	public static final String pluginVersion = "2.1";
	public static final String pluginName = "DatabasePlugin";
	public static final String pluginTitle = "Database import/export plugin v" + pluginVersion;

	public static String pluginsFolder;
	public static String pluginsPackage;
	public static String pluginsFilename;

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

	static DBLogger logger;

	/**
	 * The DBPlugin class is instantiated when Archi starts<b>
	 * It:<br>
	 *    1- configures the preference store,<br>
	 *    2- defines default values for standard options (in case they've not be defined in the preference store)
	 *    3- checks if a new version of the plugin is available on GitHub
	 */
	public DBPlugin() {
		INSTANCE = this;
		
		// forcing UTF-8
		System.setProperty("client.encoding.override", "UTF-8");
		System.setProperty("file.encoding", "UTF-8");
		
		preferenceStore = this.getPreferenceStore();
		preferenceStore.setDefault("exportWithDefaultValues", false);
		preferenceStore.setDefault("checkForUpdateAtStartup", false);
		preferenceStore.setDefault("closeIfSuccessful",       false);
		preferenceStore.setDefault("showZeroValues",          false);
		preferenceStore.setDefault("compareBeforeExport",     true);
		preferenceStore.setDefault("deleteIfImportError",     true);
		preferenceStore.setDefault("removeDirtyFlag",         false);
		preferenceStore.setDefault("showIdInContextMenu",     false);
		preferenceStore.setDefault("traceSQL",                true);
		preferenceStore.setDefault("checkMaxMemory",          true);
		preferenceStore.setDefault("copySuffix",              " (copy)");
		preferenceStore.setDefault("loggerMode",		      "disabled");
		preferenceStore.setDefault("loggerLevel",		      "INFO");
		preferenceStore.setDefault("loggerFilename",	      System.getProperty("user.home")+File.separator+pluginName+".log");
		preferenceStore.setDefault("loggerExpert",
		        "log4j.rootLogger                               = INFO, stdout, file\n"+
				"\n"+
				"log4j.appender.stdout                          = org.apache.log4j.ConsoleAppender\n"+
				"log4j.appender.stdout.Target                   = System.out\n"+
				"log4j.appender.stdout.layout                   = org.apache.log4j.PatternLayout\n"+
				"log4j.appender.stdout.layout.ConversionPattern = %d{yyyy-MM-dd HH:mm:ss} %-5p %4L:%-40.40C{1} %m%n\n"+
				"\n"+
				"log4j.appender.file                            = org.apache.log4j.FileAppender\n"+
				"log4j.appender.file.ImmediateFlush             = true\n"+
				"log4j.appender.file.Append                     = false\n"+
				"log4j.appender.file.Encoding                   = UTF-8\n"+
				"log4j.appender.file.File                       = "+(System.getProperty("user.home")+File.separator+pluginName+".log").replace("\\", "\\\\")+"\n"+
				"log4j.appender.file.layout                     = org.apache.log4j.PatternLayout\n"+
				"log4j.appender.file.layout.ConversionPattern   = %d{yyyy-MM-dd HH:mm:ss} %-5p %4L:%-40.40C{1} %m%n");
		logger = new DBLogger(DBPlugin.class);
		logger.info("Initialising "+pluginName+" plugin ...");
		
		logger.info("===============================================");
		// we force the class initialization by the SWT thread
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				DBGui.closePopup();
			}
		});
		
		// we check if the database plugin has got 1 GB max memory (in fact, Xmx1g = 954 MB so in reality we check for 950 MB)
		int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1000000);
		
		if ( maxMemory < 950 ) {
			if ( getPreferenceStore().getBoolean("checkMaxMemory") )
				DBGui.popup(Level.WARN, "Archi is configured with "+maxMemory+" MB max memory.\n\n"
						+ "If you plan to use the database plugin with huge models, we recommand to configure Archi\n"
						+ "with 1 GB of memory (you may add or update the \"-Xmx\" parameter in the Archi.ini file).\n\n"
						+ "You may deactivate the memory check in the database plugin preference page.");
			else
				logger.warn("Archi is configured with "+maxMemory+" MB max memory instead of the 1 GB recommanded.");
		} else
			logger.debug("Archi is setup with "+maxMemory+" MB of memory.");
		
		// we check if the plugin has been upgraded using the automatic procedure
		try {
			pluginsPackage = DBPlugin.class.getPackage().getName();
			pluginsFilename = new File(DBPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getCanonicalPath();
			pluginsFolder = (new File(pluginsFilename+File.separator+"..")).getCanonicalPath();

			if ( logger.isDebugEnabled() ) {
				logger.debug("plugin's package  = "+pluginsPackage);
				logger.debug("plugin's folder   = "+pluginsFolder);
				logger.debug("plugin's filename = "+pluginsFilename);
			}

			if ( !pluginsFilename.endsWith(".jar") ) {
				if ( logger.isTraceEnabled() ) logger.trace("   The plugin's filename is not a jar file, so we do not check for new plugin version on GitHub.");
			} else {
				if ( Files.exists(FileSystems.getDefault().getPath(pluginsFolder+File.separator+"databasePlugin.new"), LinkOption.NOFOLLOW_LINKS) ) {
					if ( logger.isDebugEnabled() ) logger.debug("Found file \""+pluginsFolder+File.separator+"databasePlugin.new\"");
					
					try {
						String installedPluginsFilename = new String(Files.readAllBytes(Paths.get(pluginsFolder+File.separator+"databasePlugin.new")));
						
						if ( areEqual(pluginsFilename, installedPluginsFilename) ) 
							DBGui.popup(Level.INFO, "The database plugin has been correctly updated to version "+pluginVersion);
						else
							DBGui.popup(Level.ERROR, "The database plugin has been correctly downloaded to \""+installedPluginsFilename+"\" but you are still using the database plugin version "+pluginVersion+".\n\nPlease check the plugin files located in the \""+pluginsFolder+"\" folder.");
					} catch (@SuppressWarnings("unused") IOException e) {
						DBGui.popup(Level.WARN, "A new version of the database plugin has been downloaded but we failed to check if you are using the latest version.\n\nPlease check the plugin files located in the \""+pluginsFolder+"\" folder.");
					}
					
					try {
						if ( logger.isDebugEnabled() ) logger.debug("deleting file "+pluginsFolder+File.separator+"databasePlugin.new");
						Files.delete(FileSystems.getDefault().getPath(pluginsFolder+File.separator+"databasePlugin.new"));
					} catch ( @SuppressWarnings("unused") IOException e ) {
						DBGui.popup(Level.ERROR, "Failed to delete file \""+pluginsFolder+File.separator+"databasePlugin.new\"\n\nYou need to delete it manually.");
					}
				} else if ( preferenceStore.getBoolean("checkForUpdateAtStartup") ) {
					checkForUpdate(false);
				}
			}
		} catch ( IOException e ) {
			DBGui.popup(Level.ERROR, "Failed to get database plugin's folder.", e);
		}
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		if (preferenceStore == null) {
			preferenceStore = new ScopedPreferenceStore( InstanceScope.INSTANCE, PLUGIN_ID );
		}
		return preferenceStore;
	}

	/**
	 * Check if two strings are equals<br>
	 * Replaces string.equals() to avoid nullPointerException
	 */
	public static boolean areEqual(String str1, String str2) {
		if ( str1 == null )
			return str2 == null;

		if ( str2 == null )
			return false;			// as str1 cannot be null at this stage

		return str1.equals(str2);
	}
	
	/**
	 * Check if a string  is null or empty<b>
	 * Replaces string.isEmpty() to avoid nullPointerException
	 */
	public static boolean isEmpty(String str) {
		return (str==null) || str.isEmpty();
	}
	
	public static void checkForUpdate(boolean verbose) {
		@SuppressWarnings("unused")
		DBCheckForUpdate dbCheckForUpdate = new DBCheckForUpdate(
				verbose,
				"https://api.github.com/repos/archi-contribs/database-plugin/contents/v2",
				"https://github.com/archi-contribs/database-plugin/blob/master/v2/release_note.md"
				);
	}
	
	// return user name in UTF 8 when possible
	public static String getUserName() {
		try {
			return new String(System.getProperty("user.name").getBytes("ISO-8859-1"), "UTF-8");
		} catch (@SuppressWarnings("unused") UnsupportedEncodingException ign) {
			return System.getProperty("user.name");
		}
	}
}