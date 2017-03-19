package org.archicontribs.database.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.archicontribs.database.DBDatabase;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * A field editor that manages the list of databases configurations
 * 
 * @author Herve jouin
 */
public class DBDatabaseEntryTableEditor extends FieldEditor {
	private static DBLogger logger = new DBLogger(DBDatabaseEntryTableEditor.class);
	
	private Group grpDatabases;
	
	private Table tblDatabases;
	private Label lblName;
	private Text txtName;
	private Label lblDriver;
	private Combo comboDriver;
	private Label lblFile;
	private Button btnBrowse;
	private Text txtFile;
	private Label lblExportType;
	private Button btnWholeType;
	private Button btnComponentsType;
	private Label lblServer;
	private Text txtServer;
	private Label lblPort;
	private Text txtPort;
	private Label lblDatabase;
	private Text txtDatabase;
	//private Label lblSchema;
	//private Text txtSchema;
	private Label lblUsername;
	private Text txtUsername;
	private Label lblPassword;
	private Button btnShowPassword;
	private Text txtPassword;
	
	private Button btnUp;
	private Button btnNew;
	private Button btnRemove;
	private Button btnEdit;
	private Button btnDown;
	
	private Button btnCheck;
	private Button btnDiscard;
	private Button btnSave;

	/**
	 * Creates a table field editor.
	 */
	public DBDatabaseEntryTableEditor(String name, String labelText, Composite parent) {
		init(name, labelText);
		if ( logger.isDebugEnabled() ) logger.debug("new DBDatabaseEntryTableEditor(\""+name+"\",\""+labelText+"\")");
		createControl(parent);		// calls doFillIntoGrid
	}

	/*
	 * (non-Javadoc) Method declared in FieldEditor.
	 * 
	 * called by createControl(parent)
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		if ( logger.isDebugEnabled() ) logger.debug("doFillIntoGrid()");
		
			// we create a composite with layout as FormLayout
		grpDatabases = new Group(parent, SWT.NONE);
		grpDatabases.setFont(parent.getFont());
		GridData gd = new GridData();
		gd.heightHint = 260;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		grpDatabases.setLayoutData(gd);
		grpDatabases.setLayout(new FormLayout());
		
		btnUp = new Button(grpDatabases, SWT.NONE);
		btnUp.setText("^");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(100, -70);
		fd.right = new FormAttachment(100, -40);
		btnUp.setLayoutData(fd);
		btnUp.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { swapDatabaseEntries(-1); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnUp.setEnabled(false);
		
		btnDown = new Button(grpDatabases, SWT.NONE);
		btnDown.setText("v");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(100, -35);
		fd.right = new FormAttachment(100, -5);
		btnDown.setLayoutData(fd);
		btnDown.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { swapDatabaseEntries(1); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnDown.setEnabled(false);
		
		btnNew = new Button(grpDatabases, SWT.NONE);
		btnNew.setText("New");
		fd = new FormData();
		fd.top = new FormAttachment(btnUp, 5);
		fd.left = new FormAttachment(100, -70);
		fd.right = new FormAttachment(100, -5);
		btnNew.setLayoutData(fd);
		btnNew.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { newCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		btnEdit = new Button(grpDatabases, SWT.NONE);
		btnEdit.setText("Edit");
		fd = new FormData();
		fd.top = new FormAttachment(btnNew, 5);
		fd.left = new FormAttachment(btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnNew, 0, SWT.RIGHT);
		btnEdit.setLayoutData(fd);
		btnEdit.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { editCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnEdit.setEnabled(false);
		
		btnRemove = new Button(grpDatabases, SWT.NONE);
		btnRemove.setText("Remove");
		fd = new FormData();
		fd.top = new FormAttachment(btnEdit, 5);
		fd.left = new FormAttachment(btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnNew, 0, SWT.RIGHT);
		btnRemove.setLayoutData(fd);
		btnRemove.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { removeCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnRemove.setEnabled(false);
		
		tblDatabases = new Table(grpDatabases, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.SINGLE);
		tblDatabases.setLinesVisible(true);
		fd = new FormData();
		fd.top = new FormAttachment(btnUp, 0, SWT.TOP);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(btnNew, -10, SWT.LEFT);
		fd.bottom = new FormAttachment(btnRemove, 0, SWT.BOTTOM);
		tblDatabases.setLayoutData(fd);
		tblDatabases.addListener(SWT.Resize, new Listener() {
			@Override
	        public void handleEvent(Event event) {
				tblDatabases.getColumns()[0].setWidth(tblDatabases.getClientArea().width);
			}
		});
		tblDatabases.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				tableSelectionChanged();
			}
		});
		new TableColumn(tblDatabases, SWT.NONE);
		
		lblName = new Label(grpDatabases, SWT.NONE);
		lblName.setText("Name :");
		fd = new FormData();
		fd.top = new FormAttachment(tblDatabases, 10);
		fd.left = new FormAttachment(tblDatabases, 30, SWT.LEFT);
		lblName.setLayoutData(fd);
		lblName.setVisible(false);
		
		txtName = new Text(grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblName, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblName, 30);
		fd.right = new FormAttachment(tblDatabases, -30, SWT.RIGHT);
		txtName.setLayoutData(fd);
		txtName.setVisible(false);
		
		lblDriver = new Label(grpDatabases, SWT.NONE);
		lblDriver.setText("Driver :");
		fd = new FormData();
		fd.top = new FormAttachment(lblName, 10);
		fd.left = new FormAttachment(lblName, 0 , SWT.LEFT);
		lblDriver.setLayoutData(fd);
		lblDriver.setVisible(false);
		
		comboDriver = new Combo(grpDatabases, SWT.READ_ONLY);
		comboDriver.setItems(DBDatabase.driverList);
		fd = new FormData();
		fd.top = new FormAttachment(lblDriver, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtName, 0, SWT.LEFT);
		comboDriver.setLayoutData(fd);
		comboDriver.setVisible(false);
		comboDriver.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				comboSelectionChanged();		// when the database driver is changed, we call comboSelectionChanged()
				e.doit = true;
			}
		});
		
		lblFile = new Label(grpDatabases, SWT.NONE);
		lblFile.setText("File :");
		fd = new FormData();
		fd.top = new FormAttachment(lblDriver, 10);
		fd.left = new FormAttachment(lblDriver, 0 , SWT.LEFT);
		lblFile.setLayoutData(fd);
		lblFile.setVisible(false);
		
		btnBrowse = new Button(grpDatabases, SWT.NONE);
		btnBrowse.setText("Browse");
		fd = new FormData();
		fd.top = new FormAttachment(lblFile, 0, SWT.CENTER);
		fd.right = new FormAttachment(tblDatabases, -30, SWT.RIGHT);
		btnBrowse.setLayoutData(fd);
		btnBrowse.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { browseCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnBrowse.setVisible(false);
		
		txtFile = new Text(grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblFile, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnBrowse, -10);
		txtFile.setLayoutData(fd);
		txtFile.setVisible(false);
		
		lblServer = new Label(grpDatabases, SWT.NONE);
		lblServer.setText("Server :");
		fd = new FormData();
		fd.top = new FormAttachment(lblDriver, 10);
		fd.left = new FormAttachment(lblDriver, 0 , SWT.LEFT);
		lblServer.setLayoutData(fd);
		lblServer.setVisible(false);
		
		txtServer = new Text(grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblServer, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(45, -20);
		txtServer.setLayoutData(fd);
		txtServer.setVisible(false);
		
		lblPort = new Label(grpDatabases, SWT.NONE);
		lblPort.setText("Port :");
		fd = new FormData();
		fd.top = new FormAttachment(lblServer, 10, SWT.CENTER);
		fd.left = new FormAttachment(txtServer, 40);
		lblPort.setLayoutData(fd);
		lblPort.setVisible(false);
		
		txtPort = new Text(grpDatabases, SWT.BORDER);
		txtPort.setTextLimit(5);
		fd = new FormData();
		fd.top = new FormAttachment(lblPort, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblPort, 40);
		fd.width = 30;
		txtPort.setLayoutData(fd);
		txtPort.setVisible(false);
		
		lblDatabase = new Label(grpDatabases, SWT.NONE);
		lblDatabase.setText("Database :");
		fd = new FormData();
		fd.top = new FormAttachment(lblServer, 10);
		fd.left = new FormAttachment(lblServer, 0 , SWT.LEFT);
		lblDatabase.setLayoutData(fd);
		lblDatabase.setVisible(false);
		
		txtDatabase = new Text(grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblDatabase, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(tblDatabases, -20, SWT.RIGHT);
		txtDatabase.setLayoutData(fd);
		txtDatabase.setVisible(false);
		
		//lblSchema = new Label(parentComposite, SWT.NONE);
		//lblSchema.setText("Schema :");
		//fd = new FormData();
		//fd.top = new FormAttachment(lblDatabase, 10, SWT.CENTER);
		//fd.left = new FormAttachment(txtDatabase, 40);
		//lblSchema.setLayoutData(fd);
		//lblSchema.setVisible(false);
		
		//txtSchema = new Text(parentComposite, SWT.BORDER);
		//fd = new FormData();
		//fd.top = new FormAttachment(lblSchema, 0, SWT.CENTER);
		//fd.left = new FormAttachment(txtPort, 0, SWT.LEFT);
		//fd.right = new FormAttachment(50, -20);
		//txtSchema.setLayoutData(fd);
		//txtSchema.setVisible(false);
		
		lblUsername = new Label(grpDatabases, SWT.NONE);
		lblUsername.setText("Username :");
		fd = new FormData();
		fd.top = new FormAttachment(lblDatabase, 10);
		fd.left = new FormAttachment(lblDatabase, 0 , SWT.LEFT);
		lblUsername.setLayoutData(fd);
		lblUsername.setVisible(false);
		
		txtUsername = new Text(grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblUsername, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(45, -20);
		txtUsername.setLayoutData(fd);
		txtUsername.setVisible(false);
		
		lblPassword = new Label(grpDatabases, SWT.NONE);
		lblPassword.setText("Password :");
		fd = new FormData();
		fd.top = new FormAttachment(lblUsername, 10, SWT.CENTER);
		fd.left = new FormAttachment(txtUsername, 40);
		lblPassword.setLayoutData(fd);
		lblPassword.setVisible(false);
		
		txtPassword = new Text(grpDatabases, SWT.PASSWORD | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtPort, 0, SWT.LEFT);
		fd.right = new FormAttachment(tblDatabases, -20, SWT.RIGHT);
		txtPassword.setLayoutData(fd);
		txtPassword.setVisible(false);
		
		btnShowPassword = new Button(grpDatabases, SWT.TOGGLE);
		btnShowPassword.setImage(DBGui.LOCK_ICON);
		btnShowPassword.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblPassword, 0);
		btnShowPassword.setLayoutData(fd);
		btnShowPassword.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { showOrHidePasswordCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnShowPassword.setVisible(false);

		lblExportType = new Label(grpDatabases, SWT.NONE);
		lblExportType.setText("Export type : ");
		fd = new FormData();
		fd.top = new FormAttachment(lblUsername, 10);
		fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
		lblExportType.setLayoutData(fd);
		lblExportType.setVisible(false);
		
		btnWholeType = new Button(grpDatabases, SWT.RADIO);
		btnWholeType.setText("Whole model");
		fd = new FormData();
		fd.top = new FormAttachment(lblExportType, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtName, 0, SWT.LEFT);
		btnWholeType.setLayoutData(fd);
		btnWholeType.setVisible(false);
		
		btnComponentsType = new Button(grpDatabases, SWT.RADIO);
		btnComponentsType.setText("Components only");
		fd = new FormData();
		fd.top = new FormAttachment(lblExportType, 0, SWT.CENTER);
		fd.left = new FormAttachment(45, 20);
		btnComponentsType.setLayoutData(fd);
		btnComponentsType.setVisible(false);
		
		btnSave = new Button(grpDatabases, SWT.NONE);
		btnSave.setText("Save");
		fd = new FormData();
		fd.left = new FormAttachment(btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(100, -7);
		btnSave.setLayoutData(fd);
		btnSave.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { saveCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnSave.setVisible(false);
		
		btnDiscard = new Button(grpDatabases, SWT.NONE);
		btnDiscard.setText("Discard");
		fd = new FormData();
		fd.left = new FormAttachment(btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(btnSave, -5, SWT.TOP);
		btnDiscard.setLayoutData(fd);
		btnDiscard.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { discardCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnDiscard.setVisible(false);
		
		btnCheck = new Button(grpDatabases, SWT.NONE);
		btnCheck.setText("Check");
		fd = new FormData();
		fd.left = new FormAttachment(btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(btnDiscard, -5, SWT.TOP);
		btnCheck.setLayoutData(fd);
		btnCheck.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { checkCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnCheck.setVisible(false);
		
		grpDatabases.layout();
	}
	
	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void adjustForNumColumns(int numColumns) {
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void doLoadDefault() {
	}
	
	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void doLoad() {
		if ( logger.isTraceEnabled() ) logger.trace("doLoad()");
		
		tblDatabases.removeAll();
		for ( DBDatabase databaseEntry : DBDatabase.getAllDatabasesFromPreferenceStore() ) {
			TableItem tableItem = new TableItem(tblDatabases, SWT.NONE);
			tableItem.setText(databaseEntry.getName());
			tableItem.setData(databaseEntry);
		}
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void doStore() {
		if ( logger.isTraceEnabled() ) logger.trace("doStore()");
		
		List<DBDatabase> databaseEntries = new ArrayList<DBDatabase>();
		
		for ( TableItem tableItem : tblDatabases.getItems() )
			databaseEntries.add((DBDatabase)tableItem.getData());
		
		DBDatabase.setAllIntoPreferenceStore(databaseEntries);
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	public int getNumberOfControls() {
		return 1;
	}
	
	/**
	 * Invoked when the selection in the driver combo has changed.
	 */
	protected void tableSelectionChanged() {
		TableItem[] selection = tblDatabases.getSelection();
		
		if ( selection == null || selection.length == 0 ) {
			btnUp.setEnabled(false);
			btnDown.setEnabled(false);
			btnEdit.setEnabled(false);
			btnRemove.setEnabled(false);
		} else {
			btnUp.setEnabled(tblDatabases.getSelectionIndex() != 0);
			btnUp.setEnabled(tblDatabases.getSelectionIndex() != tblDatabases.getItemCount()-1);
			btnEdit.setEnabled(true);
			btnRemove.setEnabled(true);
		}
		
		discardCallback();
	}

	/**
	 * Invoked when the selection in the driver combo has changed.
	 */
	protected void comboSelectionChanged() {
		if ( logger.isTraceEnabled() ) logger.trace("comboSelectionChanged()");
		boolean isFile = false;
		
		if ( comboDriver.getText().equalsIgnoreCase("sqlite") ) {
			isFile = true;
			FormData fd = new FormData();
			fd.top = new FormAttachment(lblFile, 10);
			fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
			lblExportType.setLayoutData(fd);
		} else {
			FormData fd = new FormData();
			fd.top = new FormAttachment(lblUsername, 10);
			fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
			lblExportType.setLayoutData(fd);
		}
		
		lblFile.setVisible(isFile);
		txtFile.setVisible(isFile);		
		btnBrowse.setVisible(isFile);
		lblExportType.setVisible(true);
		btnWholeType.setVisible(true);
		btnComponentsType.setVisible(true);
		lblServer.setVisible(!isFile);
		txtServer.setVisible(!isFile);
		lblPort.setVisible(!isFile);
		txtPort.setVisible(!isFile);
		lblDatabase.setVisible(!isFile);
		txtDatabase.setVisible(!isFile);
		//lblSchema.setVisible(!isFile);
		//txtSchema.setVisible(!isFile);
		lblUsername.setVisible(!isFile);
		txtUsername.setVisible(!isFile);
		lblPassword.setVisible(!isFile);
		txtPassword.setVisible(!isFile);
		btnShowPassword.setVisible(!isFile);
		
		grpDatabases.layout();
	}
	
	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	public void setFocus() {
		if ( tblDatabases != null )
			tblDatabases.setFocus();
	}
	
	/**
	 * Called when the "new" button has been pressed
	 */
	private void newCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("newCallback()");

			// we unselect all the lines of the tblDatabases table
		tblDatabases.deselectAll();
		tblDatabases.notifyListeners(SWT.Selection, new Event());
		
			// we show up the edition widgets
		editCallback();
	}
	
	/**
	 * Called when the "edit" button has been pressed and by the newCallback() method
	 */
	private void editCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("editCallback()");

		DBDatabase dbDatabase = null;
		boolean isFile = false;
		boolean isServer = false;
		
		if ( tblDatabases.getSelectionIndex() >= 0 ) {
			dbDatabase = (DBDatabase)tblDatabases.getItem(tblDatabases.getSelectionIndex()).getData();
		}
		
			// we showUp the widgets that allow the user to fill in the database information
			// and set them with the DBDatabase information linked to the selected TableItem (if any) 
		if ( !comboDriver.getText().isEmpty() ) {
			if ( comboDriver.getText().equalsIgnoreCase("sqlite") ) {
				isFile = true;
				FormData fd = new FormData();
				fd.top = new FormAttachment(lblFile, 10);
				fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
				lblExportType.setLayoutData(fd);
			} else {
				isServer = true;
				FormData fd = new FormData();
				fd.top = new FormAttachment(lblUsername, 10);
				fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
				lblExportType.setLayoutData(fd);
			}
		}
				
		lblName.setVisible(true);
		txtName.setVisible(true);							txtName.setEnabled(true);			txtName.setText(dbDatabase != null ? dbDatabase.getName(): "");
		
		lblDriver.setVisible(true);
		comboDriver.setVisible(true);						comboDriver.setEnabled(true);		comboDriver.setText(dbDatabase != null ? dbDatabase.getDriver() : "");
		
		lblFile.setVisible(isFile);
		txtFile.setVisible(isFile);							txtFile.setEnabled(true);			txtFile.setText(isFile && dbDatabase != null ? dbDatabase.getServer() : "");
		
		btnBrowse.setVisible(false);
		
		lblServer.setVisible(isServer);
		txtServer.setVisible(isServer);						txtServer.setEnabled(true);			txtServer.setText(isServer && dbDatabase != null ? dbDatabase.getServer() : "");
		
		lblPort.setVisible(isServer);
		txtPort.setVisible(isServer);						txtPort.setEnabled(true);			txtPort.setText(isServer ? (dbDatabase != null ? dbDatabase.getPort() : DBDatabase.getDefaultPort(comboDriver.getText())) : "");
		
		lblDatabase.setVisible(isServer);
		txtDatabase.setVisible(isServer);					txtDatabase.setEnabled(true);		txtDatabase.setText(isServer && dbDatabase != null ? dbDatabase.getDatabase() : "");
		
		lblUsername.setVisible(isServer);
		txtUsername.setVisible(isServer);					txtUsername.setEnabled(true);		txtUsername.setText(isServer && dbDatabase != null ? dbDatabase.getUsername() : "");
		
		lblPassword.setVisible(isServer);
		if ( !btnShowPassword.getSelection() )
			showOrHidePasswordCallback();
		btnShowPassword.setVisible(isServer);
		txtPassword.setVisible(isServer);					txtPassword.setEnabled(true);		txtPassword.setText(isServer && dbDatabase != null ? dbDatabase.getPassword() : "");
		
		lblExportType.setVisible(isFile||isServer);
		btnWholeType.setVisible(isFile||isServer);			btnWholeType.setEnabled(true);		btnWholeType.setSelection(dbDatabase.getExportWholeModel());
		btnComponentsType.setVisible(isFile||isServer);		btnComponentsType.setEnabled(true);	btnComponentsType.setSelection(!dbDatabase.getExportWholeModel());
		
		btnSave.setVisible(true);
		btnDiscard.setVisible(true);
		btnCheck.setVisible(true);
		
		btnNew.setEnabled(false);
		btnEdit.setEnabled(false);
		btnRemove.setEnabled(false);
		btnUp.setEnabled(false);
		btnDown.setEnabled(false);
		tblDatabases.setEnabled(false);
		
		grpDatabases.layout();
	}
	
	/**
	 * Called when the "save" button has been pressed
	 */
	private void saveCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("saveCallback()");
		
		if ( txtName.getText().isEmpty() )
			return;

		DBDatabase dbDatabase = null;
		TableItem tableItem;
		
		if ( tblDatabases.getSelectionIndex() >= 0 ) {
			dbDatabase = (DBDatabase)tblDatabases.getItem(tblDatabases.getSelectionIndex()).getData();
			tableItem = tblDatabases.getSelection()[0];
		} else {
			dbDatabase = new DBDatabase();
			tableItem = new TableItem(tblDatabases, SWT.NONE);
		}
		
		tableItem.setText(txtName.getText());
		
		dbDatabase.setName(txtName.getText());
		dbDatabase.setDriver(comboDriver.getText());
		dbDatabase.setServer(comboDriver.getText().equalsIgnoreCase("sqlite") ? txtFile.getText() : txtServer.getText());
		dbDatabase.setPort(txtPort.getText());
		dbDatabase.setDatabase(txtDatabase.getText());
		dbDatabase.setUsername(txtUsername.getText());
		dbDatabase.setPassword(txtPassword.getText());
		dbDatabase.setExportWholeModel(btnWholeType.getSelection());
		
		discardCallback();

		tableItem.setData(dbDatabase);
		tblDatabases.setSelection(tableItem);
		tblDatabases.notifyListeners(SWT.Selection, new Event());
	}
	
	/**
	 * Called when the "discard" button has been pressed
	 */
	private void discardCallback() {
		if ( tblDatabases.getSelectionIndex() >= 0 ) {
				// 
			DBDatabase dbDatabase = (DBDatabase)tblDatabases.getItem(tblDatabases.getSelectionIndex()).getData();
			boolean isFile = false;
			boolean isServer = false;
			
			if ( !dbDatabase.getDriver().isEmpty() ) {
				if ( dbDatabase.getDriver().equalsIgnoreCase("sqlite") ) {
					isFile = true;
					FormData fd = new FormData();
					fd.top = new FormAttachment(lblFile, 10);
					fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
					lblExportType.setLayoutData(fd);
				} else {
					isServer = true;
					FormData fd = new FormData();
					fd.top = new FormAttachment(lblUsername, 10);
					fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
					lblExportType.setLayoutData(fd);
				}
			}
			
			lblName.setVisible(true);
			txtName.setVisible(true);							txtName.setEnabled(false);				txtName.setText(dbDatabase.getName());
			
			lblDriver.setVisible(true);
			comboDriver.setVisible(true);						comboDriver.setEnabled(false);			comboDriver.setText(dbDatabase.getDriver());
			
			lblFile.setVisible(isFile);
			txtFile.setVisible(isFile);							txtFile.setEnabled(false);				txtFile.setText(isFile ? dbDatabase.getServer() : "");
			
			btnBrowse.setVisible(false);
			
			lblServer.setVisible(isServer);
			txtServer.setVisible(isServer);						txtServer.setEnabled(false);			txtServer.setText(isServer ? dbDatabase.getServer() : "");
			
			lblPort.setVisible(isServer);
			txtPort.setVisible(isServer);						txtPort.setEnabled(false);				txtPort.setText(isServer ? dbDatabase.getPort() : "");
			
			lblDatabase.setVisible(isServer);
			txtDatabase.setVisible(isServer);					txtDatabase.setEnabled(false);			txtDatabase.setText(isServer ? dbDatabase.getDatabase() : "");
			
			lblUsername.setVisible(isServer);
			txtUsername.setVisible(isServer);					txtUsername.setEnabled(false);			txtUsername.setText(isServer ? dbDatabase.getUsername() : "");
			
			lblPassword.setVisible(isServer);
			if ( !btnShowPassword.getSelection() )
				showOrHidePasswordCallback();
			btnShowPassword.setVisible(isServer);
			txtPassword.setVisible(isServer);					txtPassword.setEnabled(false);			txtPassword.setText(isServer ? dbDatabase.getPassword() : "");
			
			lblExportType.setVisible(isFile||isServer);
			btnWholeType.setVisible(isFile||isServer);			btnWholeType.setEnabled(false);			btnWholeType.setSelection(dbDatabase.getExportWholeModel());
			btnComponentsType.setVisible(isFile||isServer);		btnComponentsType.setEnabled(false);	btnComponentsType.setSelection(!dbDatabase.getExportWholeModel());
			
			btnEdit.setEnabled(true);
			btnRemove.setEnabled(true);
			
			btnUp.setEnabled(tblDatabases.getSelectionIndex() > 0);
			btnDown.setEnabled(tblDatabases.getSelectionIndex() < tblDatabases.getItemCount()-1);
		} else {
			lblName.setVisible(false);
			txtName.setVisible(false);
			lblDriver.setVisible(false);
			comboDriver.setVisible(false);
			lblFile.setVisible(false);
			txtFile.setVisible(false);
			btnBrowse.setVisible(false);
			lblServer.setVisible(false);
			txtServer.setVisible(false);
			lblPort.setVisible(false);
			txtPort.setVisible(false);
			lblDatabase.setVisible(false);
			txtDatabase.setVisible(false);
			lblUsername.setVisible(false);
			txtUsername.setVisible(false);
			lblPassword.setVisible(false);
			txtPassword.setVisible(false);
			
			btnEdit.setEnabled(false);
			btnRemove.setEnabled(false);
			
			btnUp.setEnabled(false);
			btnDown.setEnabled(false);
		}
		
		btnNew.setEnabled(true);
		
		btnSave.setVisible(false);
		btnDiscard.setVisible(false);
		btnCheck.setVisible(false);
		
		tblDatabases.setEnabled(true);
		
		grpDatabases.layout();
	}
	
	/**
	 * Called when the "check" button has been pressed
	 */
	private void checkCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("checkCallback()");
		DBDatabase dbDatabase = new DBDatabase();
		
		dbDatabase.setName(txtName.getText());
		dbDatabase.setDriver(comboDriver.getText());
		dbDatabase.setServer(comboDriver.getText().equalsIgnoreCase("sqlite") ? txtFile.getText() : txtServer.getText());
		dbDatabase.setPort(txtPort.getText());
		dbDatabase.setDatabase(txtDatabase.getText());
		dbDatabase.setUsername(txtUsername.getText());
		dbDatabase.setPassword(txtPassword.getText());
		
		try {
			dbDatabase.check();
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
			//TODO : if the database is not initialized, propose to create tables
			return;
		}
		DBGui.popup(Level.INFO, "Connection successful to the database.");
	}

	/**
	 * Called when the "remove" button has been pressed
	 */
	private void removeCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("removeCallback()");
		// setPresentsDefaultValue(false);
		int index = tblDatabases.getSelectionIndex();
		
		tblDatabases.remove(index);
				
		if ( index < tblDatabases.getItemCount() )
			tblDatabases.setSelection(index);
		else if ( index > 0 )
			tblDatabases.setSelection(index-1);
	}
	
	/**
	 * Called when the "browse" button has been pressed
	 */
	private void browseCallback() {
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SINGLE);
		dlg.setFileName(txtFile.getText());
		dlg.setFilterExtensions(new String[]{"*.sqlite", "*.sqlite2", "*.sqlite3", "*.db", "*.*"});
	    if (dlg.open() != null) {
			StringBuffer buf = new StringBuffer(dlg.getFilterPath());
			if (buf.charAt(buf.length() - 1) != File.separatorChar)
				buf.append(File.separatorChar);
			buf.append(dlg.getFileName());
			txtFile.setText(buf.toString());
	    }
	}

	/**
	 * Moves the currently selected item up or down.
	 *
	 * @param up
	 *            <code>true</code> if the item should move up, and
	 *            <code>false</code> if it should move down
	 */
	private void swapDatabaseEntries(int direction) {
		if ( logger.isTraceEnabled() ) logger.trace("swap("+direction+")");
		
		int source = tblDatabases.getSelectionIndex();
		int target = tblDatabases.getSelectionIndex()+direction;
		
		if ( logger.isTraceEnabled() ) logger.trace("swapping entrie "+source+" and "+target+".");
		TableItem sourceItem = tblDatabases.getItem(source);
		String sourceText = sourceItem.getText();
		DBDatabase sourceData = (DBDatabase)sourceItem.getData();
		
		TableItem targetItem = tblDatabases.getItem(target);
		String targetText = targetItem.getText();
		DBDatabase targetData = (DBDatabase)targetItem.getData();
		
		sourceItem.setText(targetText);
		sourceItem.setData(targetData);
		targetItem.setText(sourceText);
		targetItem.setData(sourceData);
		
		tblDatabases.setSelection(target);
		tblDatabases.notifyListeners(SWT.Selection, new Event());
	}
	
	/**
	 * Called when the "showPassword" button has been pressed
	 */
	public void showOrHidePasswordCallback() {
		String pass = txtPassword.getText();
		txtPassword.dispose();
		
		if ( btnShowPassword.getSelection() ) {
			txtPassword = new Text(grpDatabases,  SWT.PASSWORD | SWT.BORDER);
			btnShowPassword.setImage(DBGui.LOCK_ICON);
		} else {
			txtPassword = new Text(grpDatabases, SWT.BORDER);
			btnShowPassword.setImage(DBGui.UNLOCK_ICON);
		}
		
		txtPassword.setText(pass);
		FormData fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtPort, 0, SWT.LEFT);
		fd.right = new FormAttachment(tblDatabases, -20, SWT.RIGHT);
		txtPassword.setLayoutData(fd);
		txtPassword.setVisible(true);
		
		grpDatabases.layout();
	}
}
