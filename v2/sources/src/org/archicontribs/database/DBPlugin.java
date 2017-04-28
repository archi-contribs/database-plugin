/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 * Database Model Importer / Exporter
 * 
 * The DBPlugin class implements static methods and properties used everywhere else in the database plugin. 
 * 
 * @author Herve Jouin
 *
 * v0.1 : 			25/03/2016		plug-in creation
 * 
 * v1.0.0 :			10/12/2016		First production release
 * 
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
 * 
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
 * v2.0.0.beta3 :	21/03/2017		Correct preference page where some options are outside the window on some displays
 * 									Solve SQL duplicate key error message on model export
 * 
 * v2.0.0.beta4 :	29/03/2017		Correct import folders properties
 * 									Add version numbers of imported objects in debug mode
 * 									Solve MySQL compatibility issue on elements import SQL request
 * 									Detect folders and views changes using checksum
 * 									Solve bug where save button does not show up in preferences page
 * 
 * v2.0.0 :         30/04/2017		Export Model :
 *                                     Solve bug where properties were not exported correctly
 *                                     Solve bug where connections could be exported twice
 *                                     Rewrite the conflict detection when exporting to make it more accurate
 *                                     Create the status page on the export model window
 *                                     Add a popup when nothing needs to be exported
 *                                  Import Model :
 *                                     Create the status page on the export model window
 *                                  Import individual component :
 *                                     Add the ability to hide existing components in the import component module
 *                                     Add the ability to hide default views in the import component module
 *                                  Get component history :
 *                                     Solve bug where the component was not found in the database
 *                                  Preferences :
 *                                     Add a preference entry to automatically close import and export windows on success
 *                                     Add a preference entry to automatically start to export the model to the default database
 *                                     Add a preference entry to automatically download and install the plugin updates
 *                                     Add a preference entry to import individual components in shared or copy mode by default
 *                                  Miscellaneous :
 *                                     Solve bug in the logger where some multi-lines messages were not printed correctly
 *                                     From now on, folders and views have got their own version number
 *                                     Increase performance by reusing compiled SQL requests
 *                                     Check database version before using it
 *                                     Automatically create database tables if they do not exist
 *                                     Stop replacing the folders type
 *                                     Replaced database table "archi_plugin" by new table "database_version"
 *                                     Opens the model in the tree after import
 *                                  
 *                                  // TODO : continue to check for exceptions where required
 *                                  // TODO : allow to import elements recursively
 *                                  // TODO : allow to import and export component from the history window
 *                                  // TODO : update component get history to search for history of folders and views
 *                                   
 *                                  // TODO : add a preference to choose what to do in case of error during import : delete or not
 *                                  // TODO : add a preference to regenerate Archi 4 IDs when the ID length is not correct in order to guarantee uniqueness
 *                                  // TODO : add a preference so reset the dirty flag on successful export
 *                                  
 *                                  // TODO : allow to import image from database (is it possible to superclass the window that opens image from file ?)
 *                                  
 *                                  // TODO : add a menu action : check for missing relationships (loop on all elements and check if some relationships are missing)
 *                                  // TODO : add a progressbar on import of individual components
 *                                  // TODO : add the children components in the resolve conflict table
 *                                  
 * 									// TODO : dynamically load jdbc drivers
 * 									// TODO : add more jdbc drivers (odbc, mongodb, etc ...)
 * 									// TODO : add an option to save an image of views in the database.
 * 									// TODO : do not calculate checksums on images anymore (the path is a checksum)
 *                                  // TODO : check if it is really useful to export the diagram_ref_id of views objects
 *                                  
 *                                  // BUG : get history is empty !!!
 *                                  // BUG : sometimes, the import of individual components gets an empty tblComponents table !!!
 *                                  // BUG : clicking on the "CANCEL" button during import or export is not managed well
 */
public class DBPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.archicontribs.database";

	public static final String pluginVersion = "2.0.0";
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

	private static DBLogger logger;

	public DBPlugin() {
		INSTANCE = this;
		preferenceStore = this.getPreferenceStore();
		preferenceStore.setDefault("exportWithDefaultValues", false);
		preferenceStore.setDefault("checkForUpdateAtStartup", false);
		preferenceStore.setDefault("closeIfSuccessful",       false);
		preferenceStore.setDefault("importShared",            false);
		preferenceStore.setDefault("loggerMode",		      "disabled");
		preferenceStore.setDefault("loggerLevel",		      "INFO");
		preferenceStore.setDefault("loggerFilename",	      System.getProperty("user.home")+File.separator+pluginName+".log");
		preferenceStore.setDefault("loggerExpert",		      "log4j.rootLogger                               = INFO, stdout, file\n"+
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

		// we check if the plugin has been upgraded using the automatic procedure
		try {
			pluginsPackage = DBPlugin.class.getPackage().getName();
			pluginsFilename = new File(DBPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getCanonicalPath();
			pluginsFolder = (new File(pluginsFilename+File.separator+"..")).getCanonicalPath();

			if ( logger.isDebugEnabled() ) {
				logger.debug("plugin's package  = "+pluginsPackage);
				logger.debug("plugin's folder   = "+pluginsFolder);
				logger.debug("plugin's filename = "+pluginsFilename);
				if ( !pluginsFilename.endsWith(".jar") )
					logger.debug("(the plugin's filename is not a jar file, so Archi is running inside Eclipse)");
			}

			if ( Files.exists(FileSystems.getDefault().getPath(pluginsFolder+File.separator+"databasePlugin.new"), LinkOption.NOFOLLOW_LINKS) ) {
				if ( logger.isDebugEnabled() ) logger.debug("found file \""+pluginsFolder+File.separator+"databasePlugin.new\"");
				
				try {
					String installedPluginsFilename = Files.readAllBytes(Paths.get(pluginsFolder+File.separator+"databasePlugin.new")).toString();
					
					if ( areEqual(pluginsFilename, installedPluginsFilename) ) 
						DBGui.popup(Level.INFO, "The database plugin has been correctly updated to version "+pluginVersion);
					else
						DBGui.popup(Level.ERROR, "The database plugin has been correctly downloaded to \""+installedPluginsFilename+"\" but you are still using the database plugin version "+pluginVersion+".\n\nPlease check the plugin files located in the \""+pluginsFolder+"\" folder.");
				} catch (IOException e1) {
					DBGui.popup(Level.WARN, "A new version of the database plugin has been downloaded but we failed to check if you are using the latest version.\n\nPlease check the plugin files located in the \""+pluginsFolder+"\" folder.");
				}
				
				try {
					if ( logger.isDebugEnabled() ) logger.debug("deleting file "+pluginsFolder+File.separator+"databasePlugin.new");
					Files.delete(FileSystems.getDefault().getPath(pluginsFolder+File.separator+"databasePlugin.new"));
				} catch ( IOException e ) {
					DBGui.popup(Level.ERROR, "Failed to delete file \""+pluginsFolder+File.separator+"databasePlugin.new\"\n\nYou need to delete it manually.");
				}
			} else if ( preferenceStore.getBoolean("checkForUpdateAtStartup") ) {
				checkForUpdate(false);
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
	 * Replaces String.equals() to avoid nullPointerException
	 */
	public static boolean areEqual(String str1, String str2) {
		if ( str1 == null )
			return str2 == null;

		if ( str2 == null )
			return false;			// as str1 cannot be null at this stage

		return str1.equals(str2);
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
	 * Checks if an exception has been raised during an asynchronous thread, and throw it in the current thread if any
	 * @throws Exception 
	 */
	public static void checkAsyncException() throws Exception {
		if ( asyncException != null ) throw asyncException;
	}

	/**
	 * Set an exception during an asynchronous thread
	 */
	public static void setAsyncException(Exception e) {
		asyncException = e;
	}

	private static ProgressBar updateProgressbar = null;
	private static int updateDownloaded = 0;
	public static void checkForUpdate(boolean verbose) {
		new Thread("checkForUpdate") {
			@Override
			public void run() {
				if ( verbose )
					DBGui.popup("Please wait while checking for new database plugin ...");
				else
					logger.debug("Checking for a new plugin version on GitHub");

				// we connect to GitHub and get the latest plugin file version
				String PLUGIN_API_URL = "https://api.github.com/repos/archi-contribs/database-plugin/contents/v2";
				String RELEASENOTE_URL = "https://github.com/archi-contribs/database-plugin/blob/master/v2/release_note.md";

				Map<String, String> versions = new TreeMap<String, String>(Collections.reverseOrder());

				try {
					JSONParser parser = new JSONParser();
					Authenticator.setDefault(new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							if (getRequestorType() == RequestorType.PROXY) {
								String prot = getRequestingProtocol().toLowerCase();
								String host = System.getProperty(prot + ".proxyHost", "");
								String port = System.getProperty(prot + ".proxyPort", "80");
								String user = System.getProperty(prot + ".proxyUser", "");
								String password = System.getProperty(prot + ".proxyPassword", "");

								if (getRequestingHost().equalsIgnoreCase(host)) {
									if (Integer.parseInt(port) == getRequestingPort()) {
										// Seems to be OK.
										logger.debug("Setting PasswordAuthenticator");
										return new PasswordAuthentication(user, password.toCharArray());
									}
								}
							}
							return null;
						}  
					});

					if ( logger.isDebugEnabled() ) logger.debug("connecting to "+PLUGIN_API_URL);
					URLConnection conn = new URL(PLUGIN_API_URL).openConnection();

					if ( logger.isDebugEnabled() ) logger.debug("getting file list");
					JSONArray result = (JSONArray)parser.parse(new InputStreamReader(conn.getInputStream()));

					if ( result == null ) {
						if ( verbose ) {
							DBGui.closePopup();
							DBGui.popup(Level.ERROR, "Failed to check for new database plugin version.\n\nParsing error.");
						} else
							logger.error("Failed to check for new database plugin version.\n\nParsing error.");
						return;
					}

					if ( logger.isDebugEnabled() ) logger.debug("searching for plugins jar files");
					Pattern p = Pattern.compile(pluginsPackage+"_v(.*).jar") ;

					@SuppressWarnings("unchecked")
					Iterator<JSONObject> iterator = result.iterator();
					while (iterator.hasNext()) {
						JSONObject file = iterator.next();
						Matcher m = p.matcher((String)file.get("name")) ;
						if ( m.matches() ) {
							if ( logger.isDebugEnabled() ) logger.debug("found version "+m.group(1)+" ("+(String)file.get("download_url")+")");
							versions.put(m.group(1), (String)file.get("download_url"));
						}
					}

					if ( verbose ) DBGui.closePopup();

					if ( versions.isEmpty() ) {
						if ( verbose )
							DBGui.popup(Level.ERROR, "Failed to check for new database plugin version.\n\nDid not find any "+pluginsPackage+" JAR file.");
						else
							logger.error("Failed to check for new database plugin version.\n\nDid not find any "+pluginsPackage+" JAR file.");
						return;
					}
				} catch (Exception e) {
					if ( verbose ) {
						DBGui.closePopup();
						DBGui.popup(Level.ERROR, "Failed to check for new version on GitHub.", e);
					} else {
						logger.error("Failed to check for new version on GitHub.", e);
					}
					return;
				}

				String newPluginFilename = null;
				String tmpFilename = null;
				try {
					// treemap is sorted in descending order, so first entry should have the "bigger" key value, so the latest version
					Entry<String, String> entry = versions.entrySet().iterator().next();

					if ( pluginVersion.compareTo((String)entry.getKey()) >= 0 ) {
						if ( verbose )
							DBGui.popup(Level.INFO, "You already have got the latest version : "+pluginVersion);
						else
							logger.info("You already have got the latest version : "+pluginVersion);
						return;
					}
					
					if ( !pluginsFilename.endsWith(".jar") ) {
						if ( verbose )
							DBGui.popup(Level.ERROR,"A new version of the database plugin is available:\n     actual version: "+pluginVersion+"\n     new version: "+(String)entry.getKey()+"\n\nUnfortunately, it cannot be downloaded while Archi is running inside Eclipse.");
						else
							logger.error("A new version of the database plugin is available:\n     actual version: "+pluginVersion+"\n     new version: "+(String)entry.getKey()+"\n\nUnfortunately, it cannot be downloaded while Archi is running inside Eclipse.");
						return;
					}

					boolean ask = true;
					while ( ask ) {
					    switch ( DBGui.question("A new version of the database plugin is available:\n     actual version: "+pluginVersion+"\n     new version: "+(String)entry.getKey()+"\n\nDo you wish to download and install it ?", new String[] {"Yes", "No", "Check release note"}) ) {
					        case 0 : ask = false ; break;  // Yes
					        case 1 : return ;              // No
					        case 2 : ask = true ;          // release note
        					         Program.launch(RELEASENOTE_URL);
        					         break;
					    }
					}

					Display.getDefault().syncExec(new Runnable() { @Override public void run() { updateProgressbar = DBGui.progressbarPopup("Downloading new version of database plugin ..."); }});

					URLConnection conn = new URL(entry.getValue()).openConnection();
					String FileType = conn.getContentType();
					int fileLength = conn.getContentLength();

					newPluginFilename = pluginsFolder+File.separator+entry.getValue().substring(entry.getValue().lastIndexOf('/')+1, entry.getValue().length());
					tmpFilename = newPluginFilename+".tmp";

					if ( logger.isTraceEnabled() ) {
						logger.trace("   File URL : " + entry.getValue());
						logger.trace("   File type : " + FileType);
						logger.trace("   File length : "+fileLength);
						logger.trace("   Tmp download file path : " + tmpFilename);
						logger.trace("   New Plugin file path : " + newPluginFilename);
					}

					if (fileLength == -1)
						throw new IOException("Failed to get file size.");
					else
						Display.getDefault().syncExec(new Runnable() { @Override public void run() { updateProgressbar.setMaximum(fileLength); }});

					InputStream in = conn.getInputStream();
					FileOutputStream fos = new FileOutputStream(new File(tmpFilename));	                
					byte[] buff = new byte[1024];
					int n;
					updateDownloaded = 0;

					if ( logger.isDebugEnabled() ) logger.debug("downloading file ...");
					while ((n=in.read(buff)) !=-1) {
						fos.write(buff, 0, n);
						updateDownloaded +=n;
						Display.getDefault().syncExec(new Runnable() { @Override public void run() { updateProgressbar.setSelection(updateDownloaded); }});
						//if ( logger.isTraceEnabled() ) logger.trace(updateDownloaded+"/"+fileLength);
					}
					fos.flush();
					fos.close();

					if ( logger.isDebugEnabled() ) logger.debug("download finished");

				} catch (Exception e) {
					logger.info("here");
					if( updateProgressbar != null ) Display.getDefault().syncExec(new Runnable() { @Override public void run() { updateProgressbar.getShell().dispose(); updateProgressbar = null; }});
					try {
						if ( tmpFilename != null ) Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFilename));
					} catch (IOException e1) {
						logger.error("cannot delete file \""+tmpFilename+"\"", e1);
					}
					if ( verbose )
						DBGui.popup(Level.ERROR, "Failed to download new version of database plugin.", e);
					else
						logger.error("Failed to download new version of database plugin.",e);
					return;
				}

				if( updateProgressbar != null ) Display.getDefault().syncExec(new Runnable() { @Override public void run() { updateProgressbar.getShell().dispose(); updateProgressbar = null;}});

				//install new plugin

				// we rename the tmpFilename to its definitive filename
				if ( logger.isDebugEnabled() ) logger.debug("renaming \""+tmpFilename+"\" to \""+newPluginFilename+"\"");
				try {
					Files.move(FileSystems.getDefault().getPath(tmpFilename), FileSystems.getDefault().getPath(newPluginFilename), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					if ( verbose )
						DBGui.popup(Level.ERROR, "Failed to rename \""+tmpFilename+"\" to \""+newPluginFilename+"\"");
					else
						logger.error("Failed to rename \""+tmpFilename+"\" to \""+newPluginFilename+"\"");
					return;
				}

				try {
					Files.write(Paths.get(pluginsFolder+File.separator+"databasePlugin.new"), newPluginFilename.getBytes());
				} catch(IOException ign) {
					// not a big deal, just that there will be no message after Archi is restarted
					logger.error("Cannot create file \""+pluginsFolder+File.separator+"databasePlugin.new\"", ign);
				}

				// we delete the actual plugin file on Archi exit (can't do it here because the plugin is in use).
				(new File(pluginsFilename)).deleteOnExit();

				if( DBGui.question("A new version on the database plugin has been downloaded. Archi needs to be restarted to install it.\n\nDo you wish to restart Archi now ?") ) {
					Display.getDefault().syncExec(new Runnable() { @Override public void run() { PlatformUI.getWorkbench().restart(); }});
				}
			};
		}.start();
	}
}