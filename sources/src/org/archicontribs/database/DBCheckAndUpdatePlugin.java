/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.framework.Version;

/**
 * Checks if a new version of the plugin exists on GitHub and download it if required
 * 
 * @author Herve Jouin
 */
public class DBCheckAndUpdatePlugin {
    static final DBLogger logger = new DBLogger(DBCheckAndUpdatePlugin.class);
    
    private static final String githubLatestReleaseUrl = "https://api.github.com/repos/archi-contribs/database-plugin/releases/latest";
    
    private static final DropinsPluginHandler dropinsPluginHandler = new DropinsPluginHandler();
    
	static Display display = Display.getDefault();
	static FileSystem fileSystem = FileSystems.getDefault();
    
	static String latestVersion = null;
	static String releaseNotes = null;
	static String downloadUrl = null;
	
	static ProgressBar updateProgressbar = null;
	
	static int downloadedBytes = 0;
	
	static String newPluginFilename = null;
	
	static int answer = 0;
	
	/**
	 * Explicit constructor that forbids class instantiation as it only contains static methods
	 */
	private DBCheckAndUpdatePlugin() {
	    throw new IllegalStateException("This class is not meant to be instantiated.");
	}
	
	/**
	 * Establishes an https connection to GitHub, get a list of available jar files and get the versions from the jar filename.<br>
	 * If the greater version of the jar files is greater than the current plugin version, then a popup asks the user if it wishes to doanload it.<br>
	 * If yes, then the jar file is downloaded to a temporary file and a task is created to replace the existing jar file with the new one when Archi is stopped.
	 * @param showPopup is true show popup with error message, else only log the error messages in log file 
	 * @param pluginApiUrl
	 * @param releaseNoteUrl
	 */
	public static void checkAndUpdatePlugin(boolean showPopup) {
		// first, we check we can write to the dropins folder
		File dropinsFolder;
		String dropinsFolderName;
		try {
			dropinsFolder = dropinsPluginHandler.getDefaultDropinsFolder();
			dropinsFolderName = dropinsFolder.getCanonicalPath();
			
			if ( !dropinsFolder.canWrite() ) {
				if ( showPopup )
					DBGuiUtils.popup(Level.ERROR, "Can't write to \""+dropinsFolderName+"\" folder.");
				else
					logger.error("Can't write to \""+dropinsFolderName+"\" folder.");
				return;
			}
		} catch (IOException err) {
			if ( showPopup )
				DBGuiUtils.popup(Level.ERROR, "Failed to get dropins folder.", err);
			else
				logger.error("Failed to get dropins folder.", err);
			return;
		}
		
		if ( showPopup )
			DBGuiUtils.showPopupMessage("Checking for a new database plugin version on GitHub ...");
		else
			logger.debug("Checking for a new database plugin version on GitHub");

		try {
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					if ( logger.isTraceEnabled() ) logger.trace("   Requestor type = "+getRequestorType());
					if (getRequestorType() == RequestorType.PROXY) {
						String prot = getRequestingProtocol().toLowerCase();
						String host = System.getProperty(prot + ".proxyHost", "");
						String port = System.getProperty(prot + ".proxyPort", "80");
						String user = System.getProperty(prot + ".proxyUser", "");
						String pass = System.getProperty(prot + ".proxyPassword", "");

						if ( logger.isDebugEnabled() )
							logger.debug("Proxy request from "+getRequestingHost()+":"+getRequestingPort());
						if ( logger.isTraceEnabled() ) {
							logger.trace("   Proxy configuration:");
							logger.trace("   prot: "+prot);
							logger.trace("   host: "+host);
							logger.trace("   port: "+port);
							logger.trace("   user: "+user);
							logger.trace("   pass: (xxxxx)");
						}

						// we check if the request comes from the proxy (IP or hostname), else we do not send the password (for security reason)
						if ( (getRequestingSite().getHostAddress().equalsIgnoreCase(host) || getRequestingHost().equalsIgnoreCase(host)) && (getRequestingPort() == Integer.parseInt(port)) ) {
							// Seems to be OK.
							if ( logger.isDebugEnabled() ) logger.debug("Setting PasswordAuthenticator");
							return new PasswordAuthentication(user, pass.toCharArray());
						}
					}
					return null;
				}  
			});

			if ( logger.isDebugEnabled() ) logger.debug("Connecting to "+githubLatestReleaseUrl);
			HttpsURLConnection conn = (HttpsURLConnection)new URL(githubLatestReleaseUrl).openConnection();
			
			if ( logger.isDebugEnabled() ) logger.debug("Getting latest release information");
			try (InputStreamReader streamReader = new InputStreamReader(conn.getInputStream()) ) {
				JSONParser parser = new JSONParser();
				JSONObject latestReleaseJsonData = (JSONObject)parser.parse(streamReader);
				latestVersion = (String)latestReleaseJsonData.get("name");
				releaseNotes = ((String)latestReleaseJsonData.get("body")).replaceAll("\r\n", "\n");
				downloadUrl = (String)((JSONObject)((JSONArray)latestReleaseJsonData.get("assets")).get(0)).get("browser_download_url");
				logger.debug("latest version = " + latestVersion);
				logger.debug("release notes = " + releaseNotes);
				logger.debug("download url = " + downloadUrl);
			}
			conn.disconnect();
			
			DBGuiUtils.closePopupMessage();
	
			if ( DBPlugin.pluginVersion.compareTo(new Version(latestVersion)) >= 0 ) {
				if ( showPopup ) {
					DBGuiUtils.popup(Level.INFO, "You already have got the latest version: "+latestVersion);
				} else
					logger.info("You already have got the latest version: "+latestVersion);
				return;
			}

			boolean ask = true;
			while ( ask ) {
				display.syncExec(new Runnable() { @Override public void run() { DBCheckAndUpdatePlugin.answer = DBGuiUtils.question("A new version of the database plugin is available:\n     actual version: "+DBPlugin.pluginVersion.toString()+"\n     new version: "+ DBCheckAndUpdatePlugin.latestVersion+"\n\nDo you wish to download and install it ?", new String[] {"Yes", "No", "Check release note"}); }});
				switch ( answer ) {
					case 0: ask = false ; break;  // Yes
					case 1: return ;              // No
					case 2: ask = true ;          // release note
							DBGuiUtils.popup(Level.INFO, releaseNotes);
							break;
					default: // should never be here, but just in case
				}
			}
			
			if ( !DBPlugin.pluginsFilename.endsWith(".jar") ) {
				if ( showPopup )
					DBGuiUtils.popup(Level.ERROR,"Plugin cannot be updated as Archi is running inside Eclipse.");
				else
					logger.error("Plugin cannot be updated as Archi is running inside Eclipse.");
				return;
			}
	
			display.syncExec(new Runnable() { @Override public void run() { DBCheckAndUpdatePlugin.updateProgressbar = progressbarPopup("Downloading new version of the database plugin ..."); }});
	
			if ( logger.isDebugEnabled() ) logger.debug("Connecting to "+downloadUrl);
			conn = (HttpsURLConnection)new URL(downloadUrl).openConnection();
			
			int fileLength = conn.getContentLength();
			if (fileLength == -1)
				throw new IOException("Failed to get new plugin file size.");
			
			newPluginFilename = dropinsFolderName+File.separator+downloadUrl.substring(downloadUrl.lastIndexOf('/')+1, downloadUrl.length());
			// we delete the file in case a previous download failed
			Files.deleteIfExists(fileSystem.getPath(newPluginFilename));
			if ( logger.isDebugEnabled() ) logger.debug("Downloading latest plugin to " + newPluginFilename);
			try (InputStreamReader streamReader = new InputStreamReader(conn.getInputStream()) ) {
				display.syncExec(new Runnable() { @Override public void run() { DBCheckAndUpdatePlugin.updateProgressbar.setMaximum(fileLength); }});
	
				try ( InputStream in = conn.getInputStream() ) {
					try ( FileOutputStream fos = new FileOutputStream(new File(newPluginFilename)) ) {               
						byte[] buff = new byte[1024];
						int n;
						downloadedBytes = 0;
	
						if ( logger.isDebugEnabled() ) logger.debug("Downloading file ...");
						while ((n=in.read(buff)) !=-1) {
							fos.write(buff, 0, n);
							downloadedBytes +=n;
							display.syncExec(new Runnable() { @Override public void run() { DBCheckAndUpdatePlugin.updateProgressbar.setSelection(DBCheckAndUpdatePlugin.downloadedBytes); }});
						}
						fos.flush();
					}
				}
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Download finished");
		} catch (Exception e) {
			if( updateProgressbar != null ) display.syncExec(new Runnable() { @Override public void run() { DBCheckAndUpdatePlugin.updateProgressbar.getShell().dispose(); DBCheckAndUpdatePlugin.updateProgressbar = null; }});
			// we delete the file in case the download failed
			try {
				 Files.deleteIfExists(fileSystem.getPath(newPluginFilename));
			} catch (Exception err) {
				DBGuiUtils.popup(Level.ERROR, "Failed to delete partially downloaded file \""+newPluginFilename+"\".", err);
			}
			if ( showPopup ) {
				DBGuiUtils.closePopupMessage();
				DBGuiUtils.popup(Level.ERROR, "Failed to download the new version of the database plugin.", e);
			} else
				logger.error("Failed to download the new version of the database plugin.",e);
			return;
		}

		/*
		if( updateProgressbar != null ) display.syncExec(new Runnable() { @Override public void run() { DBCheckForPluginUpdate.updateProgressbar.getShell().dispose(); DBCheckForPluginUpdate.updateProgressbar = null;}});
		

		// we ask Archi to install it
		try {
			IStatus status = dropinsPluginHandler.installFile(new File(newPluginFilename));
			if ( !status.isOK() ) {
				if ( showPopup )
					DBGuiUtils.popup(Level.ERROR, "Failed to install new plugin version.");
				else
					logger.error("Failed to install new plugin version.");
				return;
			}
		} catch (IOException e) {
			if ( showPopup )
				DBGuiUtils.popup(Level.ERROR, "Failed to install new plugin version.", e);
			else
				logger.error("Failed to install new plugin version.",e);
			return;
		}
		

		if( DBGuiUtils.question("A new version on the database plugin has been downloaded. Archi needs to be restarted to install it.\n\nDo you wish to restart Archi now ?") ) {
			display.syncExec(new Runnable() {
				@Override public void run() {
					PlatformUI.getWorkbench().restart();
				}
			});
		}
		*/
	}
	
    /**
     * Shows up an on screen popup with a progressbar<br>
     * it is the responsibility of the caller to dismiss the popup
     * @param msg Message to show in the progressbar
     * @return 
     */
    static ProgressBar progressbarPopup(String msg) {
        if (logger.isDebugEnabled())
            logger.debug("New progressbarPopup(\"" + msg + "\")");
        
		final FontData SYSTEM_FONT = display.getSystemFont().getFontData()[0];
	    final Color    LIGHT_BLUE  = new Color(display, 240, 248, 255);
	    final Font     TITLE_FONT  = new Font(display, SYSTEM_FONT.getName(), SYSTEM_FONT.getHeight() + 2, SWT.BOLD);
        
        Shell shell = new Shell(display, SWT.SHELL_TRIM);
        shell.setSize(600, 100);
        shell.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - shell.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - shell.getSize().y) / 4);
        shell.setLayout(new FormLayout());
        
        Composite composite = new Composite(shell, SWT.NONE);
        composite.setBackground(LIGHT_BLUE);
        FormData fd = new FormData();
        fd.left= new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.top = new FormAttachment(0);
        fd.bottom = new FormAttachment(100);
        composite.setLayoutData(fd);
        composite.setLayout(new FormLayout());

        Label label = new Label(composite, SWT.CENTER);
        fd = new FormData();
        fd.left= new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.top = new FormAttachment(0, 10);
        label.setLayoutData(fd);
        label.setBackground(LIGHT_BLUE);
        label.setFont(TITLE_FONT);
        label.setText(msg);

        ProgressBar progressBar = new ProgressBar(composite, SWT.SMOOTH);
        fd = new FormData();
        fd.left= new FormAttachment(0, 20);
        fd.right = new FormAttachment(100, -20);
        fd.bottom = new FormAttachment(100, -20);
        progressBar.setLayoutData(fd);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

        shell.layout();
        shell.open();

        return progressBar;
    }
}
