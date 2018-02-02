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

import javax.net.ssl.HttpsURLConnection;

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
 * v2.0.0 :         28/04/2017		Export Model:
 *                                     Solve bug where properties were not exported correctly
 *                                     Solve bug where connections could be exported twice
 *                                     Rewrite the conflict detection when exporting to make it more accurate
 *                                     Create the status page on the export model window
 *                                     Add a popup when nothing needs to be exported
 *                                  Import Model:
 *                                     Create the status page on the export model window
 *                                  Import individual component :
 *                                     Add the ability to hide existing components in the import component module
 *                                     Add the ability to hide default views in the import component module
 *                                  Get component history :
 *                                     Solve bug where the component was not found in the database
 *                                  Preferences:
 *                                     Add a preference entry to automatically close import and export windows on success
 *                                     Add a preference entry to automatically start to export the model to the default database
 *                                     Add a preference entry to automatically download and install the plugin updates
 *                                     Add a preference entry to import individual components in shared or copy mode by default
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
 * v2.0.1 : 01/05/2017              Add the ability to export images of views in the database
 * 									Add a preference to keep the imported model even in case of error
									Add SQL Server integrated security authentication mode
 * 									Reduce memory leak
 * 									Added back Neo4J support (elements and relationships export only)
 * 									Solve NullPointerException while checking database
 * 
 * v2.0.2 : 02/05/2017              Solve errors during table creation in PostGreSQL database
 *                                  Solve "Operation not allowed after ResultSet closed" error message on model export
 *                                  Add a menu entry to replace old fashion IDs to Archi 4 IDs (to ensure uniqueness of all components)
 *                                  
 * v2.0.3 : 07/05/2017              Export model:
 *                                  	Make conflict management more reliable on PostGreSQL databases
 *                                 		Added a preference to remove the dirty flag on the model after a successful export
 *                                 		Solve bug where count of exported components could be erroneous
 *									Import individual component:
 *										Added missing "location" in individual component import window
 *                                  	Add the ability to import several individual components at the same time
 *                                  	The component list in the individual component import window are now sorted alphabetically
 *										Solve bug where the same component could be imported several times
 *                                  Miscellaneous:
 * 										Allow to specify a database schema in the database configuration
 * 										It is now possible to check a database connection without the need to edit their details
 *                                	    Reduce memory consumption
 *                                      Remove the NOT NULL constraints on some columns because Oracle does not do any difference between an empty string and a null value
 *                                      Renamed mssql driver to ms-sql to be visually more distinctive from mysql
 * 
 * v2.0.4 : 11/05/2017				Export model:
 * 										Solve bug where export conflicts were not detected correctly on MySQL databases
 *                                  Import individual component:
 *                                  	The import type (shared or copy) can now be changed directly on the import window
 *									Preference page:
 *									    Correct traversal order of fields on preference page
 *									    The default database port is automatically filled-in when port field is empty or equals 0
 *									    The default for new databases is to not export view images
 *									    When saving preferences while editing database properties, the plugin now asks if the updates need to be saved or discarded
 *                                  Miscellaneous:
 *                                  	Rewrite of the checksum calculation procedure to ensure it is always the same length
 *                                  	Rewrite of the views connections import procedure to ensure that they are imported in the same order as in the original model
 *                                  	This leads to 2 new columns (source_connections and target_connections) in the view_objects and views_connections database tables
 *                                  
 * v2.0.5 : 17/05/2017				Export model:
 * 										Change order of folders export (exporting all the root folders first)
 * 									Import model:
 * 										Solve bug in counting components to import prior the import itself which can cause false error messages even when the import is successful
 * 										Solve bug in folder import where the parent folder was not created yet before its content
 * 
 * v2.0.6 : 30/05/2017				Import model:
 * 										Solve bug when importing a model which has got a shared view which has been updated by another model
 * 										The import SQL request have been rewritten because of Oracle specificity
 * 										A double click on a model's version now launches the import
 *									Import individual components:
 *										Solve bug where all the views versions were added in the table, resulting in several entries with the same name
 * 									Database model:
 * 										Added column "element_version" to table "views_objects"
 * 										Added column "relationship_version" to table "views_connections"
 * 
 * v2.0.7 : 30/06/2017				Rollback to single thread as multi-threading causes to many side effects and does not accelerate the import and export duration as expected
 *                                  Improve checksum mechanism
 *                                  Add an option to show up ID and checksum in context menu rather than relying on the logger mode
 *                                  Import model:
 *                                      Solve bug where the filter field was not working as expected
 *                                      Change the filter request to be case insensitive
 *                                  Export model:
 *                                      Use of a Tree rather than a Table to show up conflicts
 *                                      show up more information about conflicting components
 *                                      The conflict detection and resolution is now more reliable
 * 										
 * v2.0.7b : 01/07/2017				Solve Neo4J errors
 * 
 * v2.1 : 28/01/2018				Solve plugin initialization failure
 * 									Import individual component:
 * 										added documentation column
 * 										added popup message during the import
 * 									Export model:
 *										change the export algorithm to manage standalone and collaborative modes
 *									Get history from database:
 *										allows to get history for diagrams, canvas and sketches
 *                                  Add procedures that can be called by the script plugin
 *                                  reduce memory leak
 * 
 *                                  Known bugs:
 *                                  -----------
 *										Import individual component:
 *											images are not imported
 *											view references are not imported correctly
 *											importing elements "in a view" create all the corresponding objects in the top left corner of the view
 *											clicking on the "cancel" button during the export or the import of a model is not well managed
 *
 *									TODO list:
 *									----------
 *										Import model:
 *											Add an indication when a component has been updated after the last export, through another model 
 *										Import individual component:
 *											allow to import elements recursively
 *											allow to select all the classes of one group in a single click
 *											when the user right clicks on a folder, automatically select the class corresponding to the folder (views, components, ...)
 *										Get component history:
 *											allow to export individual component, or update it from the database, directly from the history window
 *											allow to get the database history
 *										Miscellaneous:
 *											add a preference to show or hide the debug information on the right click menu rather than depend on the logging level
 *											add an option to check for relationships that are in the database but would not be in the in memory model
 *											add a progressbar on the "please wait while checking components to export" window
 *											find a way to manage images from the database the same way it is done on disk
 *											create a new windows that will show up detailed statistics about the model
 *											add more jdbc drivers (mongodb, odbc, etc ...)				
 *
 * 									technical TODOs :
 *                                  	// TODO : update component get history to search for history of folders and views
 * 										// TODO : do not calculate checksums on images anymore (the path is a checksum)
 *                                  	// TODO : check if it is really useful to export the diagram_ref_id of views objects
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

	private static DBLogger logger;

	public DBPlugin() {
		INSTANCE = this;
		preferenceStore = this.getPreferenceStore();
		preferenceStore.setDefault("exportWithDefaultValues", false);
		preferenceStore.setDefault("checkForUpdateAtStartup", false);
		preferenceStore.setDefault("closeIfSuccessful",       false);
		preferenceStore.setDefault("deleteIfImportError",     true);
		preferenceStore.setDefault("importShared",            false);
		preferenceStore.setDefault("removeDirtyFlag",         false);
		preferenceStore.setDefault("showIdInContextMenu",     false);
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
		
		logger.info("===============================================");
		// we force the class initialization by the SWT thread
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				DBGui.closePopup();
			}
		});
		
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
				
				// We connect to GitHub and get the latest plugin file version
				// Do not forget the "-Djdk.http.auth.tunneling.disabledSchemes=" in the ini file if you connect through a proxy
				String PLUGIN_API_URL = "https://api.github.com/repos/archi-contribs/database-plugin/contents/v2";
				String RELEASENOTE_URL = "https://github.com/archi-contribs/database-plugin/blob/master/v2/release_note.md";

				Map<String, String> versions = new TreeMap<String, String>(Collections.reverseOrder());

				try {
					JSONParser parser = new JSONParser();
					Authenticator.setDefault(new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
						    logger.debug("requestor type = "+getRequestorType());
							if (getRequestorType() == RequestorType.PROXY) {
								String prot = getRequestingProtocol().toLowerCase();
								String host = System.getProperty(prot + ".proxyHost", "");
								String port = System.getProperty(prot + ".proxyPort", "80");
								String user = System.getProperty(prot + ".proxyUser", "");
								String pass = System.getProperty(prot + ".proxyPassword", "");
								
								if ( logger.isDebugEnabled() ) {
								    logger.debug("proxy request from "+getRequestingHost()+":"+getRequestingPort());
								    logger.debug("proxy configuration:");
								    logger.debug("   prot : "+prot);
								    logger.debug("   host : "+host);
								    logger.debug("   port : "+port);
								    logger.debug("   user : "+user);
								    logger.debug("   pass : xxxxx");
								}

								// we check if the request comes from the proxy, else we do not send the password (for security reason)
								// TODO: check IP address in addition of the FQDN
								if ( getRequestingHost().equalsIgnoreCase(host) && (Integer.parseInt(port) == getRequestingPort()) ) {
									// Seems to be OK.
									logger.debug("Setting PasswordAuthenticator");
									return new PasswordAuthentication(user, pass.toCharArray());
								}
								logger.debug("Not setting PasswordAuthenticator as the request does not come from the proxy (host + port)");
							}
							return null;
						}  
					});
					
					
                    if ( logger.isDebugEnabled() ) logger.debug("connecting to "+PLUGIN_API_URL);
                    HttpsURLConnection conn = (HttpsURLConnection)new URL(PLUGIN_API_URL).openConnection();

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
					// treemap is sorted in descending order, so first entry should have the "bigger" key value, i.e. the latest version
					Entry<String, String> entry = versions.entrySet().iterator().next();

					if ( pluginVersion.compareTo(entry.getKey()) >= 0 ) {
						if ( verbose )
							DBGui.popup(Level.INFO, "You already have got the latest version : "+pluginVersion);
						else
							logger.info("You already have got the latest version : "+pluginVersion);
						return;
					}
					
					if ( !pluginsFilename.endsWith(".jar") ) {
						if ( verbose )
							DBGui.popup(Level.ERROR,"A new version of the database plugin is available:\n     actual version: "+pluginVersion+"\n     new version: "+entry.getKey()+"\n\nUnfortunately, it cannot be downloaded while Archi is running inside Eclipse.");
						else
							logger.error("A new version of the database plugin is available:\n     actual version: "+pluginVersion+"\n     new version: "+entry.getKey()+"\n\nUnfortunately, it cannot be downloaded while Archi is running inside Eclipse.");
						return;
					}

					boolean ask = true;
					while ( ask ) {
					    switch ( DBGui.question("A new version of the database plugin is available:\n     actual version: "+pluginVersion+"\n     new version: "+entry.getKey()+"\n\nDo you wish to download and install it ?", new String[] {"Yes", "No", "Check release note"}) ) {
					        case 0 : ask = false ; break;  // Yes
					        case 1 : return ;              // No
					        case 2 : ask = true ;          // release note
        					         Program.launch(RELEASENOTE_URL);
        					         break;
							default:
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
					in.close();

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
			}
		}.start();
	}
}