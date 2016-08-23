package org.archicontribs.database;

import java.awt.Toolkit;
import java.util.Hashtable;

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

public class DBProgress extends Dialog {
	private Shell dialog;
	private Button button;
	private TabFolder tabFolder;
	private Hashtable<String, DBTabItem> tabItems = new Hashtable<String, DBTabItem>();

	/**
	 * Create the dialog.
	 */
	public DBProgress() {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		Display.getCurrent().getActiveShell().addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				e.doit = false;
			}
		});
		createContents();
		dialog.open();
		dialog.layout();
	}

	private void createContents() {
		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setSize(850, 380);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 2);
		dialog.setLayout(null);

		tabFolder = new TabFolder(dialog, SWT.NONE);
		tabFolder.setBounds(10, 10, 825, 300);

		//removit before compile !!!
		//createTabItem("test",100);

		button = new Button(dialog, SWT.NONE);
		button.setBounds(759, 316, 75, 25);
		button.setText("In progress ...");
		button.setEnabled(false);
	}

	public DBTabItem tabItem(String _name) {
		DBTabItem tab = tabItems.get(_name);
		if ( tab != null )
			return tab;
		tab = new DBTabItem(tabFolder, _name);
		tabItems.put(_name, tab);
		return tab;
	}
	
	public void finish() {
		button.setText("Done");
		button.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		button.setEnabled(true);
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) { dialog.dispose(); }
		});
	}
	
	public void dismiss() {
		dialog.dispose();
	}
}

