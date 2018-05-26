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
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Checks if a new version of the plugin exists on GitHub and download it if required
 * 
 * @author Herve Jouin
 */
public class DBCheckForUpdate {
    static final DBLogger logger = new DBLogger(DBCheckForUpdate.class);
    
    private Display  display        	      = Display.getDefault();    
    
	ProgressBar updateProgressbar = null;
	int updateDownloaded = 0;
	int answer;
	
	/**
	 * Establishes an https connection to GitHub, get a list of available jar files and get the versions from the jar filename.<br>
	 * If the greater version of the jar files is greater than the current plugin version, then a popup asks the user if it wishes to doanload it.<br>
	 * If yes, then the jar file is downloaded to a temporary file and a task is created to replace the existing jar file with the new one when Archi is stopped.
	 * @param verbose
	 * @param pluginApiUrl
	 * @param releaseNoteUrl
	 */
	public DBCheckForUpdate(boolean verbose, String pluginApiUrl, String releaseNoteUrl) {
		if ( verbose )
			DBGui.popup("Please wait while checking for new database plugin ...");
		else
			logger.debug("Checking for a new database plugin version on GitHub");

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

						// we check if the request comes from the proxy (IP or hostname), else we do not send the password (for security reason)
						if ( (getRequestingSite().getHostAddress().equalsIgnoreCase(host) || getRequestingHost().equalsIgnoreCase(host)) && (getRequestingPort() == Integer.parseInt(port)) ) {
							// Seems to be OK.
							logger.debug("Setting PasswordAuthenticator");
							return new PasswordAuthentication(user, pass.toCharArray());
						}
						logger.debug("Not setting PasswordAuthenticator as the request does not come from the proxy (host + port)");
					}
					return null;
				}  
			});


			if ( logger.isDebugEnabled() ) logger.debug("connecting to "+pluginApiUrl);
			HttpsURLConnection conn = (HttpsURLConnection)new URL(pluginApiUrl).openConnection();

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
			Pattern p = Pattern.compile(DBPlugin.pluginsPackage+"_v(.*).jar") ;

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
					DBGui.popup(Level.ERROR, "Failed to check for new database plugin version.\n\nDid not find any "+DBPlugin.pluginsPackage+" JAR file.");
				else
					logger.error("Failed to check for new database plugin version.\n\nDid not find any "+DBPlugin.pluginsPackage+" JAR file.");
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

			if ( DBPlugin.pluginVersion.compareTo(entry.getKey()) >= 0 ) {
				if ( verbose )
					DBGui.popup(Level.INFO, "You already have got the latest version : "+DBPlugin.pluginVersion);
				else
					logger.info("You already have got the latest version : "+DBPlugin.pluginVersion);
				return;
			}

			if ( !DBPlugin.pluginsFilename.endsWith(".jar") ) {
				if ( verbose )
					DBGui.popup(Level.ERROR,"A new version of the database plugin is available:\n     actual version: "+DBPlugin.pluginVersion+"\n     new version: "+entry.getKey()+"\n\nUnfortunately, it cannot be downloaded while Archi is running inside Eclipse.");
				else
					logger.error("A new version of the database plugin is available:\n     actual version: "+DBPlugin.pluginVersion+"\n     new version: "+entry.getKey()+"\n\nUnfortunately, it cannot be downloaded while Archi is running inside Eclipse.");
				return;
			}

			boolean ask = true;
			while ( ask ) {
				this.display.syncExec(new Runnable() { @Override public void run() { DBCheckForUpdate.this.answer = DBGui.question("A new version of the database plugin is available:\n     actual version: "+DBPlugin.pluginVersion+"\n     new version: "+entry.getKey()+"\n\nDo you wish to download and install it ?", new String[] {"Yes", "No", "Check release note"}); }});
				switch ( this.answer ) {
					case 0 : ask = false ; break;  // Yes
					case 1 : return ;              // No
					case 2 : ask = true ;          // release note
							 Program.launch(releaseNoteUrl);
							 break;
					default: break;					// should never be here, but just in case
				}
			}

			this.display.syncExec(new Runnable() { @Override public void run() { DBCheckForUpdate.this.updateProgressbar = progressbarPopup("Downloading new version of the database plugin ..."); }});

			URLConnection conn = new URL(entry.getValue()).openConnection();
			String FileType = conn.getContentType();
			int fileLength = conn.getContentLength();

			newPluginFilename = DBPlugin.pluginsFolder+File.separator+entry.getValue().substring(entry.getValue().lastIndexOf('/')+1, entry.getValue().length());
			tmpFilename = newPluginFilename+".tmp";

			if ( logger.isTraceEnabled() ) {
				logger.trace("   File URL : " + entry.getValue());
				logger.trace("   File type : " + FileType);
				logger.trace("   File length : "+fileLength);
				logger.trace("   Tmp download file path : " + tmpFilename);
				logger.trace("   New Plugin file path : " + newPluginFilename);
			}
			
			// we delete the temp file in case a previous download failed
			try {
				 Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFilename));
			} catch (Exception e) {
				DBGui.popup(Level.ERROR, "Failed to delete temporary file.", e);
			}

			if (fileLength == -1)
				throw new IOException("Failed to get file size.");

			this.display.syncExec(new Runnable() { @Override public void run() { DBCheckForUpdate.this.updateProgressbar.setMaximum(fileLength); }});

			try ( InputStream in = conn.getInputStream() ) {
				try ( FileOutputStream fos = new FileOutputStream(new File(tmpFilename)) ) {               
					byte[] buff = new byte[1024];
					int n;
					this.updateDownloaded = 0;

					if ( logger.isDebugEnabled() ) logger.debug("downloading file ...");
					while ((n=in.read(buff)) !=-1) {
						fos.write(buff, 0, n);
						this.updateDownloaded +=n;
						this.display.syncExec(new Runnable() { @Override public void run() { DBCheckForUpdate.this.updateProgressbar.setSelection(DBCheckForUpdate.this.updateDownloaded); }});
						//if ( logger.isTraceEnabled() ) logger.trace(updateDownloaded+"/"+fileLength);
					}
					fos.flush();
				}
			}

			if ( logger.isDebugEnabled() ) logger.debug("download finished");

		} catch (Exception e) {
			logger.info("here");
			if( this.updateProgressbar != null ) this.display.syncExec(new Runnable() { @Override public void run() { DBCheckForUpdate.this.updateProgressbar.getShell().dispose(); DBCheckForUpdate.this.updateProgressbar = null; }});
			try {
				if ( tmpFilename != null ) Files.deleteIfExists(FileSystems.getDefault().getPath(tmpFilename));
			} catch (IOException e1) {
				logger.error("cannot delete file \""+tmpFilename+"\"", e1);
			}
			if ( verbose )
				DBGui.popup(Level.ERROR, "Failed to download the new version of the database plugin.", e);
			else
				logger.error("Failed to download the new version of the database plugin.",e);
			return;
		}

		if( this.updateProgressbar != null ) this.display.syncExec(new Runnable() { @Override public void run() { DBCheckForUpdate.this.updateProgressbar.getShell().dispose(); DBCheckForUpdate.this.updateProgressbar = null;}});

		//install new plugin

		// we rename the tmpFilename to its definitive filename
		if ( logger.isDebugEnabled() ) logger.debug("renaming \""+tmpFilename+"\" to \""+newPluginFilename+"\"");
		try {
			Files.move(FileSystems.getDefault().getPath(tmpFilename), FileSystems.getDefault().getPath(newPluginFilename), StandardCopyOption.REPLACE_EXISTING);
		} catch (@SuppressWarnings("unused") IOException ign) {
			if ( verbose )
				DBGui.popup(Level.ERROR, "Failed to rename \""+tmpFilename+"\" to \""+newPluginFilename+"\"");
			else
				logger.error("Failed to rename \""+tmpFilename+"\" to \""+newPluginFilename+"\"");
			return;
		}

		try ( PrintWriter out = new PrintWriter(DBPlugin.pluginsFolder+File.separator+"databasePlugin.new") ) {
			out.println(newPluginFilename);
		} catch(IOException ign) {
			// not a big deal, just that there will be no message after Archi is restarted
			logger.error("Cannot create file \""+DBPlugin.pluginsFolder+File.separator+"databasePlugin.new\"", ign);
		}

		// we delete the actual plugin file on Archi exit (can't do it here because the plugin is in use).
		(new File(DBPlugin.pluginsFilename)).deleteOnExit();

		if( DBGui.question("A new version on the database plugin has been downloaded. Archi needs to be restarted to install it.\n\nDo you wish to restart Archi now ?") ) {
			this.display.syncExec(new Runnable() { @Override public void run() { PlatformUI.getWorkbench().restart(); }});
		}
	}
	
    /**
     * Shows up an on screen popup with a progressbar<br>
     * it is the responsibility of the caller to dismiss the popup
     */
    ProgressBar progressbarPopup(String msg) {
        if (logger.isDebugEnabled())
            logger.debug("new progressbarPopup(\"" + msg + "\")");
        
		final FontData SYSTEM_FONT = this.display.getSystemFont().getFontData()[0];
	    final Color    LIGHT_BLUE  = new Color(this.display, 240, 248, 255);
	    final Font     TITLE_FONT  = new Font(this.display, SYSTEM_FONT.getName(), SYSTEM_FONT.getHeight() + 2, SWT.BOLD);
        
        Shell shell = new Shell(this.display, SWT.SHELL_TRIM);
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
