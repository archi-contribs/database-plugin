/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.File;
import java.io.IOException;
import org.osgi.framework.Version;
import java.util.Locale;
import java.util.UUID;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.archimatetool.model.IIdentifier;

import lombok.Getter;


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
 * v2.1: 25/09/2018					Import components from database:
 * 										Rename "import individual component" to "import components"
 * 										Added documentation column to help distinguish components having the same name
 *                                      Added tooltip with properties to help distinguish components having the same name
 * 										Added message during the import as it may take some time
 * 										Use commands to allow undo/redo
 *                                      Added a label to explain that the icons can be selected
 *                                      The categories can now be clicked to select/unselect the whole category
 *                                      The component is imported by default in the selected folder
 *                                      The element classes are pre-selected depending on the selected folder
 *                                      In case one view is selected for import, show view screenshot if available in the database
 *                                      Introduce new template mode that mixes shared and copy modes depending on on the "template" property of each component
 *                                      Possibility to import a whole model into another one (merge models)
 *                                  Import model:
 *                                  	A context menu entry allowing to import a model has been added when no model is selected
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
 *                                      Rewrite version management
 *                                      Remove the name, the documentation and the properties from view objects and connections checksum calculation as they are related to the corresponding concept
 *                                  Plugin preferences:
 *                                      Add an option to specify the default import mode for individual components (template mode, force shared mode or force copy mode)
 *                                      Add an option to compare the model from the database before exporting it
 *										Allow to specify the generated view screenshots border width and scale factor
 *										Allow to specify a suffix to add to components imported in copy mode
 *										Add an option to check for max memory at startup (xmx should be set to 1g)
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
 *											Fix display on HiDPI displays
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
 *                                  		Check for max memory available at startup and suggest to increase it (xmx parameter) if less than 1 GB
 *                                  		Add the ability to import an image from the database on Canvas Image and Block objects
 * 											Update JDBC drivers
 * 												- Neo4J to 3.1.0
 * 												- SQLite to 3.21.0
 *												- PostGreSQL to 42.2.1
 *
 * v2.1.1: 27/09/2018
 *                                  Fix import order from PostGreSQL databases
 *                                  
 * v2.1.2: 30/09/2018				Revert replace CREATE by MERGE in Neo4J requests
 * 
 * v2.1.3: 10/10/2018				Fix Oracle objects names were longer than the 30 character limit
 * 									Fix Oracle error ORA-00932 when using CLOB in joined requests
 * 									Update Oracle driver to 18c
 * 									Remove the hint_title and hint_content columns from views and views_objects tables as they do not need to be exported
 * 
 * v2.1.4: 20/10/2018				Fix version number
 * 
 * v2.1.5: 10/11/2018				Add key bindings to export model and import model commands
 * 									Fix screen scale calculation divide by zero exception on some environments
 * 									Fix model's checksum is reset during export
 * 									Check if auto commit mode before rollbacking transaction
 * 
 * v2.1.6: 13/11/2018				Fix savepoint name expected error during rollback
 * 									Fix import of recursive referenced views
 * 
 * v2.1.7: 21/11/2018               Import components from database:
 *                                     updating a view from the database now updates the elements and relationships referenced in the view
 *                                  Fixes:
 *                                     Fix version calculation during export
 *                                     Fix SQL requests when the database plugin is called by the script plugin
 *                                     Fix label position in debug window
 * 
 * v2.1.8: 12/02/2018				Fixes:
 * 										Fix import of images when initiated by script plugin
 * 
 * v2.1.9: 25/02/2019               Add expert mode where the jdbc connection string can be set manually
 * 
 * v2.1.10: 23/04/2019				Fix unclosed cursors issue on Oracle database
 * 
 * v2.1.11: 25/04/2019				Fix version comparison when a part of it is greater or equal to 10
 * 									Fix issues on SQL requests introduced in plugin version 2.1.10
 * 
 * v2.2: 10/08/2019					Rewrite of the export process
 * 									Rewrite of the conflict management process
 * 									Remove the "Relationship" suffix on the relationships names during Neo4J exports
 *									Fix number of model components during export when components are updated or deleted from the database
 *									Fix the export of an existing database model when opened from archimate file
 *									Remember the import database and set it as the default export database
 *									Ask the database password during connection if not provided in the preferences
 *									fix tablename case for SQL databases
 *									Add welcome message
 * 									Update the JDBC drivers
 * 										MySQL		--> 8.0.17
 * 										Neo4J		--> 3.4.0
 * 										PostGreSQL	--> 42.2.6
 * 										SQLite		--> 3.27.2.1
 * 
 * v2.2.1: 23/10/2019				Fix plugin version in windows title
 * 									Fix count of model's components during export
 * 									Fix unnecessary double export of view components 
 * 									Do not compare twice the model to the database if there have been no import
 * 									Create first admin procedures
 * 										check database structure
 * 										check database content
 * 									Update the JDBC drivers
 * 										MySQL		--> rollback to 5.1.48 because of timezone error (https://bugs.mysql.com/bug.php?id=90813)
 * 
 * v2.2.2: 23/11/2019				Rewrite code to remove a lot of class extends
 * 									Update database structure to be compliant with Archi 4.6
 * 									GUI improvement
 * 										Add last export date to the models table on the import window
 * 										Add a progress bar during a database comparison
 * 									Performance improvement
 * 										Do not try to import properties, features and bendpoints if not necessary
 * 										Drastically increase the performance when comparing a model against a database
 * 									Security improvement
 * 										The database password is not printed in clear text in preference window by default
 * 										The database password is not stored in clear text anymore in the preference store, even if the algorithm used is must be reversible
 * 
 * v2.2.3: 11/12/2019				Reduce the number of SQL request
 * 									Fix the get history from database
 * 									Fix the Neo4J JDBC connection string
 * 									Fix directed view connections export
 * 
 * v2.2.4: 12/12/2019				Get the plugin's version directly from the platform bundle 				
 * 									Fix username and password load from preference page for databases in expert mode
 * 
 * v2.2.5: 17/04/2020				This release is an update of the database drivers
 * 										Neo4J			--> 4.0.0
 * 										Oracle			--> 10
 * 										PostGreSQL		--> 42.2.12
 * 										SQLite			--> 3.30.1
 * 										MS SQL Server	--> 8.2.2
 * 										MySQL			--> stays in version 5.1.48 because of timezone error (https://bugs.mysql.com/bug.php?id=90813)
 * 
 * v2.2.6: 03/05/2020				Fix import of connections bendpoints when importing a view from another model
 * 									Fix automatic update from GitHub
 * 									Fix plugin version check at startup 
 * 
 * v2.2.7: 05/05/2020				Fix password storage in preferences when expert mode is activated
 * 									remove unnecessary password deciphering 
 * 
 * v2.2.8: 15/07/2020				Fix exception when accessing the database admin menu
 * 									Rewrite database structure check
 * 									Add a preference to enable/disable NOT NULL database constraints
 * 									Add a delete model action in the admin procedures
 * 									Add ACLI support to import a model from a database from command line
 * 
 * v2.2.9: 25/07/2020				Fix admin procedure
 * 
 * v2.2.10: 05/08/2020				Fix schema and columns type in SQL requests
 * 
 * v2.2.11: 13/08/2020				Fix structure check on Oracle database when schema is empty
 * 									Fix MySQL/MariaDB table creation SQL requests
 * 									Change password encryption key to avoid error messages when switching from LAN to WIFI 
 * 
 * v2.2.12: 20/08/2020				Add NLS support for Oracle databases
 * 									Fix deletion of a model from a SQL Server database
 * 									Fix Neo4J databases check
 *
 * v2.2.13: 27/11/2020				Update rank column in all tables to be compatible with MySQL 8.x
 * 									Rewrite automatic plugin update to use dropins folder
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
	/** ID of the plugin */
	public static final String PLUGIN_ID = "org.archicontribs.database";

	/** version of the plugin */
	public static Version pluginVersion = Platform.getBundle(PLUGIN_ID).getVersion(); 

	
	/** Name ofthe plugin */
	public static final String pluginName = "DatabasePlugin";
	
	/** Title od the plugin's windows */
	public static final String pluginTitle = "Database import/export plugin v" + pluginVersion.toString();

	/** Name of the plugin's package */
	public static String pluginsPackage;
	
	/** folder where the plugin is installed */
	public static String pluginsFolder;
	
	/** Name of the plugin's JAR file */
	public static String pluginsFilename;

	/**
	 * static instance that allow to keep information between calls of the plugin
	 */
	public static DBPlugin INSTANCE;

	/**
	 * PreferenceStore allowing to store the plugin configuration.
	 */
	private static IPersistentPreferenceStore preferenceStore = null;

	/**
	 * Name of all the table names in a SQL database
	 */
	public static String[] allSQLTables = { "archimatediagrammodel", "archimateelement", "bendpoint", "canvasmodel", "canvasmodelblock", "canvasmodelimage", "canvasmodelsticky", "connection", "diagrammodelarchimateobject", "diagrammodelreference", "folder", "model", "property", "relationship", "sketchmodel", "sketchmodelactor", "sketchmodelsticky"};

	static DBLogger logger;
	
    /**
     * Choices available when a conflict is detected in the database<br>
     * <li><b>askUser</b> Ask the user what he wishes to do</li>
     * <li><b>doNotExport</b> Do not export to the database</li>
     * <li><b>exportToDatabase</b> Export to the database</li>
     * <li><b>importFromDatabase</b> Replace the component with the version in the database</li>
     */
    public enum CONFLICT_CHOICE {
    	/** Ask the user what he wishes to do                      */	askUser,
    	/** Do not export to the database                          */	doNotExport,
    	/** Export to the database                                 */	exportToDatabase,
    	/** Replace the component with the version in the database */	importFromDatabase
    }
	
	/**
	 * Returns true is runs on Windows operating system, false for all other operating systems
	 */
	@Getter private static boolean WindowsOperatingSystem = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

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
		preferenceStore.setDefault("checkNotNullConstraints", true);
		preferenceStore.setDefault("copySuffix",              " (copy)");
		preferenceStore.setDefault("defaultImportMode",       "template");
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
		
		
		
		String welcomeMessage = null;
		String preferenceStorePluginVersion = preferenceStore.getString("pluginVersion");
		if ( (preferenceStorePluginVersion == null) || preferenceStorePluginVersion.isEmpty() ) {
			// if the "pluginVersion" preference is not set, this means that this is the first time the plugin is run
			// so we print out a welcome message
			welcomeMessage = "Welcome to the Archi Database Plugin.\n\nThis plugin allows you to centralize your models in a SQL database, and export them to a graph database for analysis purpose.\n\nThe next step is to configure your database(s) on the plugin's preference page.";
		} else {
			Version oldPluginVersion = new Version(preferenceStorePluginVersion);
			if ( oldPluginVersion.compareTo(pluginVersion) < 0 ) {
				// if the "pluginVersion" preference is older, then the plugin has been upgraded
				// so we print out a message confirming the upgrade
				welcomeMessage = "The Database plugin has been upgraded from version "+preferenceStorePluginVersion+" to version "+pluginVersion.toString()+".";
			} else if ( oldPluginVersion.compareTo(pluginVersion) > 0 ) {
				// if the "pluginVersion" preference is newer, then the plugin has been downgraded
				// so we print out a message confirming the downgrade
				welcomeMessage = "The Database plugin has been downgraded from version "+preferenceStorePluginVersion+" to version "+pluginVersion.toString()+".";
			}
		}

		preferenceStore.setValue("pluginVersion", pluginVersion.toString());
		if ( welcomeMessage != null ) {
			// we get all the DBDatabaseEntries in order to replace plain text passwords by encrypted passwords
			try {
				DBDatabaseEntry.getAllDatabasesFromPreferenceStore();
				preferenceStore.save();
			} catch (IOException e) {
				DBGui.popup(Level.ERROR, "Failed to save your preferences.", e);
			}
			DBGui.popup(Level.INFO, welcomeMessage);
		}
		
		// we check if the plugin has been upgraded using the automatic procedure
		try {
			pluginsPackage = DBPlugin.class.getPackage().getName();
			pluginsFilename = new File(DBPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getCanonicalPath();
			pluginsFolder = (new File(pluginsFilename+File.separator+"..")).getCanonicalPath();

			if ( logger.isDebugEnabled() ) {
				logger.debug("Plugin's package  = "+pluginsPackage);
				logger.debug("Plugin's version  = "+pluginVersion.toString());
				logger.debug("Plugin's folder   = "+pluginsFolder);
				logger.debug("Plugin's filename = "+pluginsFilename);
			}

			if ( !pluginsFilename.endsWith(".jar") ) {
				if ( logger.isTraceEnabled() ) logger.trace("   The plugin's filename is not a jar file, so we do not check for new plugin version on GitHub.");
			} else {
				if ( preferenceStore.getBoolean("checkForUpdateAtStartup") )
					checkForUpdate(false);
			}
		} catch ( IOException e ) {
			DBGui.popup(Level.ERROR, "Failed to get database plugin's folder.", e);
		}
	}

	@Override
	public IPersistentPreferenceStore getPreferenceStore() {
		if (preferenceStore == null) {
			preferenceStore = new ScopedPreferenceStore( InstanceScope.INSTANCE, PLUGIN_ID );
		}
		return preferenceStore;
	}

	/**
	 * Check if two strings are equals
	 * <br><br>
	 * Replaces string.equals() to avoid nullPointerException
	 * @param str1 first string to compare
	 * @param str2 secong string to compare
	 * @return true if the strings are both null or have the same content, false if they are different
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
	 * <br><br>
	 * Replaces string.equals() to avoid nullPointerException
	 * @param str1 first string to compare
	 * @param str2 secong string to compare
	 * @return true if the strings are both null or have the same content, false if they are different
	 */
	public static boolean areEqualIgnoreCase(String str1, String str2) {
		if ( str1 == null )
			return str2 == null;

		if ( str2 == null )
			return false;			// as str1 cannot be null at this stage

		return str1.equalsIgnoreCase(str2);
	}
	
	/**
	 * Check if a string  is null or empty
	 * <br><br>
	 * Replaces string.isEmpty() to avoid nullPointerException
	 * @param str string to check
	 * @return true if the string is null or empty, false if the string contains at least one char
	 */
	public static boolean isEmpty(String str) {
		return (str==null) || str.isEmpty();
	}
	
	/**
	 * Checks on GitHub if a new version of the plugin is available
	 * @param verbose
	 */
	public static void checkForUpdate(boolean verbose) {
		@SuppressWarnings("unused")
		DBCheckForPluginUpdate dbCheckForUpdate = new DBCheckForPluginUpdate(
				verbose,
				"https://api.github.com/repos/archi-contribs/database-plugin/contents/v2",
				"https://github.com/archi-contribs/database-plugin/blob/master/v2/release_note.md"
				);
	}
	
	/**
	 * Generates a new ID for any Archi component
	 * 
	 * @return Archi ID
	 */
	public static String createID() {
		return createID(null);
	}
	
	/**
	 * Generates a new ID for a given Archi component
	 * @param obj object for which the ID should be generated
	 * 
	 * @return Archi ID
	 */
	public static String createID(IIdentifier obj) {
		// until Archi 4.4: the ID was created using model.getIDAdapter().getNewID()
		// Archi 4.5 updated the ArchimateModel class to remove the getIDAdapter() method and introduces a new UUIDFactory class with a createID() method.
		
		// as I wish my plugin works for both versions, I decided to write my own method (based on UUIDFactory.createID())
		
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Gets the boolean value from a Boolean, an Integer or a String
	 * @param obj
	 * @return Boolean : true if the obj value is true<BR>Integer: true if the obj value is greater than 0<BR>String : true if the string can be converted to an Integer greater then 0<BR>false in all other cases
	 */
	static public boolean getBooleanValue(Object obj) {
		if ( obj == null )
			return false;
		
		if ( obj instanceof Boolean )
			return ((Boolean)obj).booleanValue();
		
		if ( obj instanceof Integer )
			return (Integer)obj != 0;
		
		if ( obj instanceof String ) {
			try {
				return Integer.valueOf((String)obj) != 0;
			} catch (@SuppressWarnings("unused") NumberFormatException ign) {
				// ignore
			}
			return Boolean.valueOf((String)obj);
		}
		
		return false;
	}
}