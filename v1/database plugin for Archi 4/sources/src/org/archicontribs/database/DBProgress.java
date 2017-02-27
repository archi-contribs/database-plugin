package org.archicontribs.database;

import java.awt.Toolkit;
import java.util.Hashtable;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;

/**
 * This class shows up a popup to follow up the import and export process. This window includes a tabitem that hosts DBProgressTabItem
 * 
 * @author Herve Jouin
 */
public class DBProgress extends Dialog {
	private Shell dialog;
	private Button button;
	private TabFolder tabFolder;
	private Hashtable<String, DBProgressTabItem> tabItems = new Hashtable<String, DBProgressTabItem>();

	/**
	 * Create the dialog.
	 */
	public DBProgress() {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBProgress.DBProgress()");
		Display.getCurrent().getActiveShell().addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				e.doit = false;
			}
		});
		createContents();
		if ( (new DBPreferencePage()).getShowProgress().substring(0,4).equals("show") ) {
			DBPlugin.debug(DebugLevel.Variable, "Showing up progress window ...");
			dialog.open();
			dialog.layout();
		} else
			DBPlugin.debug(DebugLevel.Variable, "Hiding progress window ...");
		
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBProgress.DBProgress()");
	}

	private void createContents() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBProgress.createContents()");
		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setSize(850, 380);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 4);
		dialog.setLayout(null);

		tabFolder = new TabFolder(dialog, SWT.NONE);
		tabFolder.setBounds(10, 10, 825, 300);

		button = new Button(dialog, SWT.NONE);
		button.setBounds(759, 316, 75, 25);
		button.setText("In progress ...");
		button.setEnabled(false);
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBProgress.createContents()");
	}

	public DBProgressTabItem tabItem(String _name) {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBProgress.tabItem()");
		DBProgressTabItem tab = tabItems.get(_name);
		if ( tab != null )
			return tab;
		tab = new DBProgressTabItem(tabFolder, _name);
		tabItems.put(_name, tab);
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBProgress.tabItem()");
		return tab;
	}
	
	public void finish() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBProgress.finish()");
		if ( (new DBPreferencePage()).getShowProgress().equals("showAndDismiss") ) {
			dialog.dispose();
		} else {
			button.setText("Done");
			button.setEnabled(true);
			button.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
				public void widgetDefaultSelected(SelectionEvent e) { dialog.dispose(); }
			});
		}
	}
	
	public void dismiss() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBProgress.dismiss()");
		dialog.dispose();
	}
}

