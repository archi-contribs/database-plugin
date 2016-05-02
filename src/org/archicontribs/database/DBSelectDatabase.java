/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.awt.Toolkit;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DBSelectDatabase extends Dialog {
	private Connection db;
	private Shell dialog;
	private Combo driver;
	private Text server;
	private Text port;
	private Text database;
	private Text username;
	private Text password;
	private Button remember;
	private Button doNotAskAgain;
	private Button btnOk;
	private Button btnCancel;


	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public DBSelectDatabase(/*Shell parent, int style*/) {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/**
	 * Open the dialog.
	 * @return the connection to the database
	 */
	public Connection open() {
		createContents();
		loadValues();
		if ( remember.getSelection() && doNotAskAgain.getSelection() && !driver.getText().isEmpty() ) {
			connectToDatabase();
		}
		//if we cannot connect to the database, then we force the window opening
		if ( db == null ) {
			dialog.open();
			dialog.layout();
			Display display = getParent().getDisplay();
			while (!dialog.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
		return db;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		dialog = new Shell(getParent(), getStyle());
		dialog.setSize(400, 310);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 2);

		Label lblDatabase = new Label(dialog, SWT.NONE);
		lblDatabase.setBounds(10, 25, 65, 15);
		lblDatabase.setText("Driver :");

		driver = new Combo(dialog, SWT.NONE);
		driver.setToolTipText("Choose database brand");
		driver.setItems(new String[] {"MySQL", "Oracle", "PostGreSQL"});
		driver.setBounds(80, 17, 169, 400);
		driver.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		Label lblServer = new Label(dialog, SWT.NONE);
		lblServer.setBounds(10, 60, 65, 15);
		lblServer.setText("Server :");

		server = new Text(dialog, SWT.BORDER);
		server.setBounds(80, 57, 300, 21);

		Label lblPort = new Label(dialog, SWT.NONE);
		lblPort.setBounds(10, 95, 65, 15);
		lblPort.setText("Port :");

		port = new Text(dialog, SWT.BORDER);
		port.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if (e.character != 0 && !Character.isDigit(e.character))
					e.doit = false;
			}
		});
		port.setBounds(80, 92, 75, 21);

		Label lblDatabase_1 = new Label(dialog, SWT.NONE);
		lblDatabase_1.setBounds(10, 130, 65, 15);
		lblDatabase_1.setText("Database :");

		database = new Text(dialog, SWT.BORDER);
		database.setBounds(80, 127, 169, 21);

		Label lblUsername = new Label(dialog, SWT.NONE);
		lblUsername.setBounds(10, 165, 65, 15);
		lblUsername.setText("Username :");

		username = new Text(dialog, SWT.BORDER);
		username.setBounds(80, 162, 169, 21);

		Label lblPassword = new Label(dialog, SWT.NONE);
		lblPassword.setBounds(10, 200, 65, 15);
		lblPassword.setText("Password :");

		password = new Text(dialog, SWT.BORDER | SWT.PASSWORD);
		password.setBounds(80, 197, 169, 21);

		remember = new Button(dialog, SWT.CHECK);
		remember.setText("Remember");
		remember.setSelection(false);
		remember.setBounds(10, 250, 79, 16);

		doNotAskAgain = new Button(dialog, SWT.CHECK);
		doNotAskAgain.setText("Do not ask me again");
		doNotAskAgain.setSelection(false);
		doNotAskAgain.setBounds(90, 250, 125, 16);

		btnOk = new Button(dialog, SWT.NONE);
		btnOk.setBounds(226, 246, 75, 25);
		btnOk.setText("Ok");
		btnOk.setEnabled(false);

		btnCancel = new Button(dialog, SWT.NONE);
		btnCancel.setBounds(307, 246, 75, 25);
		btnCancel.setText("Cancel");

		ModifyListener check = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				btnOk.setEnabled( !driver.getText().isEmpty() && !server.getText().isEmpty() && !port.getText().isEmpty() && !database.getText().isEmpty() && !username.getText().isEmpty() && !password.getText().isEmpty() );
			}
		};
		driver.addModifyListener(check);
		server.addModifyListener(check);
		port.addModifyListener(check);
		database.addModifyListener(check);
		username.addModifyListener(check);
		password.addModifyListener(check);

		driver.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if ( port.getText().isEmpty() ) {
					switch(driver.getText()) {
					case "mySQL" : port.setText("13306"); break;
					case "Oracle" : port.setText("1521"); break;
					case "PostGreSQL" : port.setText("5432"); break;
					default : port.setText("");
					}
				}
			}
		});

		btnOk.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) {
				connectToDatabase();
				if ( db != null ) {
					saveValues();
					((Button) e.getSource()).getShell().close(); 
				}
			}
		});

		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) { db = null ; ((Button) e.getSource()).getShell().close(); }
		});
	}

	private void connectToDatabase() {
		try {
			Class.forName("org."+driver.getText().toLowerCase()+".Driver");
		} catch (ClassNotFoundException ee) {
			DBPlugin.popup(Level.Error, "Cannot load 'org."+driver.getText().toLowerCase()+".Driver' driver.", ee);
			return;
		}

		try {
			db = DriverManager.getConnection("jdbc:" + driver.getText().toLowerCase() + "://" + server.getText() + ":" + port.getText() + "/" + database.getText(), username.getText(), password.getText());
			db.setAutoCommit(false);
		} catch (SQLException ee) {
			DBPlugin.popup(Level.Error, "Cannot connect to the database.", ee);
			return;
		} 
	}

	private void loadValues() {
		PreferenceStore store = new PreferenceStore("org.archicontribs.database");

		//if preferences are not set, that's not an error
		try { store.load(); } catch (IOException e) { return; }

		driver.setText(store.getString("driver"));
		server.setText(store.getString("server"));
		port.setText(store.getString("port"));
		database.setText(store.getString("database"));
		remember.setSelection(store.getBoolean("remember"));
		doNotAskAgain.setSelection(store.getBoolean("doNotAskAgain"));
		username.setText(store.getString("username"));
		password.setText(store.getString("password"));
	}

	private void saveValues() {
		PreferenceStore store = new PreferenceStore("org.archicontribs.database");

		if ( remember.getSelection() ) {
			store.setValue("driver", driver.getText());
			store.setValue("server", server.getText());
			store.setValue("port", port.getText());
			store.setValue("database", database.getText());
			store.setValue("username", username.getText());
			store.setValue("password", password.getText());
			store.setValue("remember", remember.getSelection());
			store.setValue("doNotAskAgain", doNotAskAgain.getSelection());
		} else {
			store.setValue("driver","");
			store.setValue("server", "");
			store.setValue("port", "");
			store.setValue("database", "");
			store.setValue("username", "");
			store.setValue("password", "");
			store.setValue("remember", false);
			store.setValue("doNotAskAgain", false);
		}
		try {
			store.save();
		} catch (IOException e) {
			DBPlugin.popup(Level.Error, "Cannot save preferences.", e);
			return;
		}
	}
}
