package org.archicontribs.database;

/**
 * This file is adapted from the com.archimatetool.editor.p2.DropinsPluginHandler @author Phillip Beauvoir
 */

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

import com.archimatetool.editor.ArchiPlugin;
import com.archimatetool.editor.Logger;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.editor.utils.PlatformUtils;
import com.archimatetool.editor.utils.ZipUtils;

/**
 * @author Herve Jouin
 *
 */
public class DropinsPluginHandler {
	File userDropinsFolder;
	File systemDropinsFolder;
	File instanceDropinsFolder;

	boolean success;
	boolean needsClose;

	static final int CONTINUE = 0;
	static final int RESTART = 1;
	static final int CLOSE = 2;

	static final String MAGIC_ENTRY = "archi-plugin"; //$NON-NLS-1$

	static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$

	/**
	 * 
	 */
	public DropinsPluginHandler() {
		// nothing to do
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws NoSuchElementException 
	 */
	public List<Bundle> getInstalledPlugins() throws IOException, NoSuchElementException {
		List<Bundle> list = new ArrayList<>();

		for(Bundle bundle : ArchiPlugin.INSTANCE.getBundle().getBundleContext().getBundles()) {
			File file = getDropinsBundleFile(bundle);
			if(file != null) {
				list.add(bundle);
			}
		}

		return list;
	}

	/**
	 * @param shell
	 * @return
	 * @throws IOException
	 */
	public int install(Shell shell) throws IOException {
		if(!checkCanWriteToFolder(shell, getDefaultDropinsFolder())) {
			return status();
		}

		List<File> files = askOpenFiles(shell);
		if(files.isEmpty()) {
			return status();
		}

		List<IStatus> stats = new ArrayList<>();

		Exception[] exception = new Exception[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {
				for(File file : files) {
					try {
						IStatus status = installFile(file);
						stats.add(status);
					}
					catch(IOException ex) {
						exception[0] = ex;
					}
				}
			}
		});

		if(exception[0] != null) {
			displayErrorDialog(shell, exception[0].getMessage());
			return status();
		}

		StringBuilder resultMessage = new StringBuilder();
		boolean hasError = false;

		for(int i = 0; i < stats.size(); i++) {
			IStatus status = stats.get(i);

			if(status.isOK()) {
				this.success = true;
				resultMessage.append(NLS.bind("{0} - Installed\n", files.get(i).getName())); //$NON-NLS-1$
			}
			else {
				hasError = true;

				if(status.getCode() == 666)
					resultMessage.append(NLS.bind("{0} - Is not an Archi plug-in.\n", files.get(i).getName())); //$NON-NLS-1$
				else
					resultMessage.append(NLS.bind("{0} - Not installed.\n", files.get(i).getName())); //$NON-NLS-1$
			}
		}

		if(hasError)
			MessageDialog.openInformation(shell, "Install status", resultMessage.toString());

		return status();
	}

	/**
	 * @param zipFile
	 * @return
	 * @throws IOException
	 */
	public IStatus installFile(File zipFile) throws IOException {
		if(!isPluginZipFile(zipFile)) {
			return new Status(IStatus.ERROR, "com.archimatetool.editor", 666, //$NON-NLS-1$
					NLS.bind("{0} is not a plug-in file", zipFile.getAbsolutePath()), null);
		}

		Path tmp = Files.createTempDirectory("archi"); //$NON-NLS-1$
		File tmpFolder = tmp.toFile();

		try {
			ZipUtils.unpackZip(zipFile, tmpFolder);

			File pluginsFolder = getDefaultDropinsFolder();
			pluginsFolder.mkdirs();

			for(File file : tmpFolder.listFiles()) {
				// Ignore the magic entry file
				if(MAGIC_ENTRY.equalsIgnoreCase(file.getName())) {
					continue;
				}

				// Delete old plugin on exit in target plugins folder
				deleteOlderPluginOnExit(file, pluginsFolder);

				// Copy new ones
				if(file.isDirectory()) {
					FileUtils.copyFolder(file, new File(pluginsFolder, file.getName()));
				}
				else {
					FileUtils.copyFile(file, new File(pluginsFolder, file.getName()), false);
				}
			}
		}
		finally {
			FileUtils.deleteFolder(tmpFolder);
		}

		return new Status(IStatus.OK, "com.archimatetool.editor", 777, NLS.bind("{0} installed", zipFile.getPath()), null); //$NON-NLS-1$
	}

	/**
	 * @param shell
	 * @param selected
	 * @return
	 * @throws IOException,NoSuchElementException
	 */
	public int uninstall(Shell shell, List<Bundle> selected) throws IOException, NoSuchElementException {
		if(selected.isEmpty()) {
			return status();
		}

		boolean ok = MessageDialog.openQuestion(shell,
				"Uninstall Archi Plug-ins",
				"Are you sure you want to uninstall these plug-ins?");

		if(!ok) {
			return status();
		}

		for(Bundle bundle : selected) {
			File file = getDropinsBundleFile(bundle);
			if(file != null) {
				deleteOnExit(file);
			}
			else {
				Logger.logError(NLS.bind("Could not create file location to uninstall plugin: {0}", bundle.getLocation()));
			}
		}

		this.success = true;

		return status();
	}

	int status() {
		if(this.success && this.needsClose) {
			return CLOSE;
		}
		if(this.success) {
			return RESTART;
		}

		return CONTINUE;
	}

	// Delete matching older plugin
	void deleteOlderPluginOnExit(File newPlugin, File pluginsFolder) throws IOException {
		for(File file : findMatchingPlugins(pluginsFolder, newPlugin)) {
			deleteOnExit(file);
		}
	}

	static File[] findMatchingPlugins(File pluginsFolder, File newPlugin) {
		String pluginName = getPluginName(newPlugin.getName());

		return pluginsFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				String targetPluginName = getPluginName(file.getName());
				return targetPluginName.equals(pluginName) && !newPlugin.getName().equals(file.getName());
			}
		});
	}

	static String getPluginName(String string) {
		String result;
		int index = string.indexOf("_"); //$NON-NLS-1$
		if(index == -1)
			result = string;
		else
			result = string.substring(0, index);

		return result;
	}

	static String getPluginVersion(String string) {
		String result;
		int index = string.lastIndexOf(".jar"); //$NON-NLS-1$
		if(index == -1)
			result = string;
		else
			result = string.substring(0, index);

		index = string.lastIndexOf("_"); //$NON-NLS-1$
		if(index != -1)
			result = string.substring(index + 1);

		return result;
	}

	static boolean checkCanWriteToFolder(Shell shell, File folder) {
		folder.mkdirs();

		if(!Files.isWritable(folder.toPath())) {
			String message = "Archi does not have write access to the installation folder. "; //$NON-NLS-1$

			if(PlatformUtils.isWindows()) {
				message += "Please run Archi as Administrator and try again.";
			}
			else {
				message += "Please quit and move the Archi app to a different location and try again.";
			}

			displayErrorDialog(shell, message);

			return false;
		}

		return true;
	}

	static boolean isPluginZipFile(File file) throws IOException {
		return ZipUtils.isZipFile(file) && ZipUtils.hasZipEntry(file, MAGIC_ENTRY);
	}

	File getDefaultDropinsFolder() throws IOException {
		// Get user dropins folder as set in Archi.ini
		File dropinsFolder = getUserDropinsFolder();

		// Else get the instance dropins folder as set in osgi.instance.area
		if(dropinsFolder == null) {
			dropinsFolder = getInstanceDropinsFolder();
		}

		// Else get the dropins folder as the "dropins" folder in the app installation directory
		if(dropinsFolder == null) {
			dropinsFolder = getSystemDropinsFolder();
		}

		return dropinsFolder;
	}

	File getUserDropinsFolder() {
		if(this.userDropinsFolder == null) {
			// If the dropins dir is set in Archi.ini
			String dropinsDirProperty = ArchiPlugin.INSTANCE.getBundle().getBundleContext().getProperty(DROPINS_DIRECTORY);
			if(dropinsDirProperty != null) {
				// Perform a variable substitution if necessary of %% tokens
				dropinsDirProperty = substituteVariables(dropinsDirProperty);
				this.userDropinsFolder = new File(dropinsDirProperty);
			}
		}

		return this.userDropinsFolder;
	}

	File getInstanceDropinsFolder() throws IOException {
		if(this.instanceDropinsFolder == null) {
			URL url = Platform.getInstanceLocation().getURL();
			url = FileLocator.resolve(url);
			this.instanceDropinsFolder = new File(url.getPath(), "dropins"); //$NON-NLS-1$
		}

		return this.instanceDropinsFolder;
	}

	File getSystemDropinsFolder() throws IOException {
		if(this.systemDropinsFolder == null) {
			URL url = Platform.getInstallLocation().getURL();
			url = FileLocator.resolve(url);
			this.systemDropinsFolder = new File(url.getPath(), "dropins"); //$NON-NLS-1$
		}

		return this.systemDropinsFolder;
	}

	/**
	 * This is taken From org.eclipse.equinox.internal.p2.reconciler.dropins.Activator
	 * When the dropins folder contains %% tokens, treat this as a system property.
	 * Example - %user.home%
	 * @param path 
	 * @return 
	 */
	static String substituteVariables(String path) {
		if(path == null) {
			return path;
		}

		int beginIndex = path.indexOf('%');

		// no variable
		if(beginIndex == -1) {
			return path;
		}

		beginIndex++;

		int endIndex = path.indexOf('%', beginIndex);
		// no matching end % to indicate variable
		if(endIndex == -1) {
			return path;
		}

		// get the variable name and do a lookup
		String variable = path.substring(beginIndex, endIndex);
		if(variable.length() == 0 || variable.indexOf(File.pathSeparatorChar) != -1) {
			return path;
		}

		variable = ArchiPlugin.INSTANCE.getBundle().getBundleContext().getProperty(variable);
		if(variable == null) {
			return path;
		}

		return path.substring(0, beginIndex - 1) + variable + path.substring(endIndex + 1);
	}

	/**
	 * If the bundle is in one of the "dropins" folders return its file (jar or folder), else return null
	 * @param bundle 
	 * @return 
	 * @throws IOException 
	 * @throws NoSuchElementException 
	 */
	File getDropinsBundleFile(Bundle bundle) throws IOException, NoSuchElementException {
		File bundleFile = FileLocator.getBundleFileLocation(bundle).get();
		File parentFolder = bundleFile.getParentFile();
		return (parentFolder.equals(getUserDropinsFolder())
				|| parentFolder.equals(getInstanceDropinsFolder()) 
				|| parentFolder.equals(getSystemDropinsFolder())) ? bundleFile : null;
	}

	static List<File> askOpenFiles(Shell shell) {
		FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] { "*.archiplugin", "*.zip", "*.*" } ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String path = dialog.open();

		// TODO: Bug on Mac 10.12 and newer - Open dialog does not close straight away
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=527306
		if(path != null && PlatformUtils.isMac()) {
			while(Display.getCurrent().readAndDispatch()) {
				// nothing to do;
			}
		}

		List<File> files = new ArrayList<>();

		if(path != null) {
			for(String name : dialog.getFileNames()) {
				String filterPath = dialog.getFilterPath();
				filterPath += File.separator; // Issue on OpenJDK if path is like C: or D: - no slash is added when creating File
				files.add(new File(filterPath, name));
			}
		}

		return files;
	}

	void deleteOnExit(File file) throws IOException {
		if(file.isDirectory()) {
			recursiveDeleteOnExit(file.toPath());
		}
		else {
			file.deleteOnExit();
		}

		// Mac won't delete files with File.deleteOnExit() if workbench is restarted
		this.needsClose = PlatformUtils.isMac();
	}

	static void recursiveDeleteOnExit(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				file.toFile().deleteOnExit();
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				dir.toFile().deleteOnExit();
				return FileVisitResult.CONTINUE;
			}
		});
	}

	static void displayErrorDialog(Shell shell, String message) {
		MessageDialog.openError(shell,
				"Install Archi Plug-ins",
				message);
	}
}
