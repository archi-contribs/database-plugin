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
import org.archicontribs.database.GUI.DBGuiUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ModelVersion;

import lombok.Getter;


/**
 * Database Model Importer / Exporter
 * 
 * The DBPlugin class implements static methods and properties used everywhere else in the database plugin. 
 * 
 * @author Herve Jouin
 * 
 * v4.8.1   22/09/2021  fix coherence checking
 *                      change plugin numbering to follow Archi's and indicate more clearly that this version is compatible with Archi 4.8 and not Archi 4.9
 *                                  
 * v4.8.2	22/09/2021	fix exception raised when model purpose is null rather than an empty string
 * 
 * -----------------------------------------------------------------------------------------
 * 
 * v4.9.0a	22/09/2021	Change plugin version numbering to match Archi version
 * 						Adapt plugin to Archi 4.9, especially specialization
 * 
 * v4.9.0b	26/10/2021	Fix database model update
 * 						fix SQL Server exception caused by missing column in GROUP BY clause
 * 						Fix folder when import individual component
 * 						Fix import and export windows default location
 * 						Fix import relationships in right folder
 *						Fix database upgrade for Oracle engine
 * 						Improve specializations management during export to database
 * 						Add Options tab in preference page to reduce window size
 * 						Add model metadata in compare to database process
 * 
 * v4.9.1	01/12/2021	Fix parent folder of imported components when merging models
 * 
 * v4.9.2	01/12/2021	Fix nullPointerExceptions when model has got null properties or features
 * 
 * v4.9.3	08/12/2021	Fix created_on date for folders during export
 * 						Ignore difference between null and empty string during comparison
 * 						Fix created_by information on "get history" spreadsheet
 * 						Remove ID conversion from former size to new one as it is not needed anymore
 * 
 * -----------------------------------------------------------------------------------------
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
		logger.info("Initialising "+pluginTitle+" ...");
		
		logger.info("===============================================");
		// we force the class initialization by the SWT thread
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				DBGuiUtils.closePopupMessage();
			}
		});
		
		// we check if the database plugin has got 1 GB max memory (in fact, Xmx1g = 954 MB so in reality we check for 950 MB)
		int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1000000);
		
		if ( maxMemory < 950 ) {
			if ( getPreferenceStore().getBoolean("checkMaxMemory") )
				DBGuiUtils.popup(Level.WARN, "Archi is configured with "+maxMemory+" MB max memory.\n\n"
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
				DBGuiUtils.popup(Level.ERROR, "Failed to save your preferences.", e);
			}
			DBGuiUtils.popup(Level.INFO, welcomeMessage);
		}
		
		// we check if the plugin has been upgraded using the automatic procedure
		try {
			pluginsPackage = DBPlugin.class.getPackage().getName();
			pluginsFilename = new File(DBPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getCanonicalPath();
			pluginsFolder = (new File(pluginsFilename+File.separator+"..")).getCanonicalPath();

			if ( logger.isDebugEnabled() ) {
				logger.debug("Plugin's package  = "+pluginsPackage);
				logger.debug("Plugin's version  = "+pluginVersion.toString());
				logger.debug("Archi's version   = "+ModelVersion.VERSION);
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
			DBGuiUtils.popup(Level.ERROR, "Failed to get database plugin's folder.", e);
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
	 * @param str2 second string to compare
	 * @return true if the strings are both null or have the same content, false if they are different
	 */
	public static boolean areEqual(String str1, String str2) {
		if ( isEmpty(str1) )
			return isEmpty(str2);

		if ( isEmpty(str2) )
			return false;			// as str1 cannot be empty at this stage

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
		if ( isEmpty(str1) )
			return isEmpty(str2);

		if ( isEmpty(str2) )
			return false;			// as str1 cannot be empty at this stage

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