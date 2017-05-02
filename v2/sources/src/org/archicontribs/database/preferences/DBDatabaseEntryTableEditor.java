/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

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
import org.eclipse.swt.layout.RowLayout;
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
	private Composite compoExportType;
	private Button btnWholeType;
	private Button btnComponentsType;
	private Label lblExportViewImages;
	private Composite compoExportViewImages;
	private Button btnExportViewImages;
	private Button btnDoNotExportViewImages;
	private Label lblNeo4jMode;
	private Composite compoNeo4jMode;
	private Button btnNeo4jNativeMode;
	private Button btnNeo4jExtendedMode;
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
		if ( logger.isTraceEnabled() ) logger.trace("new DBDatabaseEntryTableEditor(\""+name+"\",\""+labelText+"\")");
		createControl(parent);		// calls doFillIntoGrid
	}

	/*
	 * (non-Javadoc) Method declared in FieldEditor.
	 * 
	 * called by createControl(parent)
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		if ( logger.isTraceEnabled() ) logger.trace("doFillIntoGrid()");
		
			// we create a composite with layout as FormLayout
		grpDatabases = new Group(parent, SWT.NONE);
		grpDatabases.setFont(parent.getFont());
		grpDatabases.setLayout(new FormLayout());
		grpDatabases.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		grpDatabases.setText("Databases : ");
		
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
			public void widgetSelected(SelectionEvent e) { showDatabaseDetails(true); }
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
				showDatabaseDetails(false);
			}
		});
		new TableColumn(tblDatabases, SWT.NONE);
		
		lblName = new Label(grpDatabases, SWT.NONE);
		lblName.setText("Name :");
		lblName.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
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
		lblDriver.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
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
		lblFile.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
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
		lblServer.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
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
		lblPort.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblServer, 10, SWT.CENTER);
		fd.left = new FormAttachment(txtServer, 30);
		lblPort.setLayoutData(fd);
		lblPort.setVisible(false);
		
		txtPort = new Text(grpDatabases, SWT.BORDER);
		txtPort.setTextLimit(5);
		fd = new FormData();
		fd.top = new FormAttachment(lblPort, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblPort, 30);
		fd.width = 40;
		txtPort.setLayoutData(fd);
		txtPort.setVisible(false);
		
		lblDatabase = new Label(grpDatabases, SWT.NONE);
		lblDatabase.setText("Database :");
		lblDatabase.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
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
		lblUsername.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
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
		lblPassword.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblUsername, 10, SWT.CENTER);
		fd.left = new FormAttachment(lblPort, 0, SWT.LEFT);
		lblPassword.setLayoutData(fd);
		lblPassword.setVisible(false);
		
		btnShowPassword = new Button(grpDatabases, SWT.TOGGLE);
		btnShowPassword.setImage(DBGui.LOCK_ICON);
		btnShowPassword.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.right = new FormAttachment(tblDatabases, -20, SWT.RIGHT);
		btnShowPassword.setLayoutData(fd);
		btnShowPassword.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { showOrHidePasswordCallback(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		btnShowPassword.setVisible(false);
		
		txtPassword = new Text(grpDatabases, SWT.PASSWORD | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(lblPassword, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtPort, 0, SWT.LEFT);
		fd.right = new FormAttachment(btnShowPassword,0, SWT.LEFT);
		txtPassword.setLayoutData(fd);
		txtPassword.setVisible(false);
		


		lblExportType = new Label(grpDatabases, SWT.NONE);
		lblExportType.setText("Default export type :");
		lblExportType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblUsername, 10);
		fd.left = new FormAttachment(lblUsername, 0 , SWT.LEFT);
		lblExportType.setLayoutData(fd);
		lblExportType.setVisible(false);
		
		compoExportType = new Composite(grpDatabases, SWT.NONE);
		compoExportType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		compoExportType.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblExportType, 0, SWT.TOP);
		fd.bottom = new FormAttachment(lblExportType, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(txtName, 0, SWT.RIGHT);
		compoExportType.setLayoutData(fd);
		RowLayout rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		compoExportType.setLayout(rl);
		
		btnWholeType = new Button(compoExportType, SWT.RADIO);
		btnWholeType.setText("Whole model");
		btnWholeType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnWholeType.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { comboSelectionChanged(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		btnComponentsType = new Button(compoExportType, SWT.RADIO);
		btnComponentsType.setText("Components only");
		btnComponentsType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnComponentsType.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { comboSelectionChanged(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		lblExportViewImages = new Label(grpDatabases, SWT.NONE);
		lblExportViewImages.setText("Export View Images :");
		lblExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblExportType, 10);
		fd.left = new FormAttachment(lblExportType, 0 , SWT.LEFT);
		lblExportViewImages.setLayoutData(fd);
		lblExportViewImages.setVisible(false);
		
		compoExportViewImages = new Composite(grpDatabases, SWT.NONE);
		compoExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		compoExportViewImages.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblExportViewImages, 0, SWT.TOP);
		fd.bottom = new FormAttachment(lblExportViewImages, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(txtName, 0, SWT.RIGHT);
		compoExportViewImages.setLayoutData(fd);
		rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		compoExportViewImages.setLayout(rl);
		
		btnExportViewImages = new Button(compoExportViewImages, SWT.RADIO);
		btnExportViewImages.setText("Yes");
		btnExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		
		btnDoNotExportViewImages = new Button(compoExportViewImages, SWT.RADIO);
		btnDoNotExportViewImages.setText("No");
		btnDoNotExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		
		lblNeo4jMode = new Label(grpDatabases, SWT.NONE);
		lblNeo4jMode.setText("Export graph mode :");
		lblNeo4jMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblExportType, 10);
		fd.left = new FormAttachment(lblExportType, 0 , SWT.LEFT);
		lblNeo4jMode.setLayoutData(fd);
		lblNeo4jMode.setVisible(false);
		
		compoNeo4jMode = new Composite(grpDatabases, SWT.NONE);
		compoNeo4jMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		compoNeo4jMode.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblNeo4jMode, 0, SWT.TOP);
		fd.bottom = new FormAttachment(lblNeo4jMode, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(txtName, 0, SWT.RIGHT);
		compoNeo4jMode.setLayoutData(fd);
		rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		compoNeo4jMode.setLayout(rl);
		
		btnNeo4jNativeMode = new Button(compoNeo4jMode, SWT.RADIO);
		btnNeo4jNativeMode.setText("Native");
		btnNeo4jNativeMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
				
		btnNeo4jExtendedMode = new Button(compoNeo4jMode, SWT.RADIO);
		btnNeo4jExtendedMode.setText("Extended");
		btnNeo4jExtendedMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
				
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
			public void widgetSelected(SelectionEvent e) { showDatabaseDetails(false); }
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
		
		GridData gd = new GridData();
		gd.heightHint = lblExportViewImages.getLocation().y + 10;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		grpDatabases.setLayoutData(gd);
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
		for ( DBDatabase databaseEntry : DBDatabase.getAllDatabasesFromPreferenceStore(true) ) {
			TableItem tableItem = new TableItem(tblDatabases, SWT.NONE);
			tableItem.setText(databaseEntry.getName());
			tableItem.setData(databaseEntry);
		}
		
		if ( tblDatabases.getItemCount() != 0 ) {
			tblDatabases.setSelection(0);
			tblDatabases.notifyListeners(SWT.Selection, new Event());
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

		if ( comboDriver.getText().equalsIgnoreCase("mssql") ) {
			txtUsername.setToolTipText("Leave username and password empty to use Windows integrated security");
			txtPassword.setToolTipText("Leave username and password empty to use Windows integrated security");
			txtServer.setToolTipText("Specify \"server\\\\instance\" in case of named instance.");
		} else {
			txtUsername.setToolTipText(null);
			txtPassword.setToolTipText(null);
			txtServer.setToolTipText(null);
		}
		
		if ( txtPort.getText().isEmpty() ) {
			for ( int i = 0; i < DBDatabase.driverList.length; ++i) {
				if ( comboDriver.getText().equals(DBDatabase.driverList[i]) )
					txtPort.setText(DBDatabase.defaultPorts[i]);
			}
		}
		
		lblFile.setVisible(isFile);
		txtFile.setVisible(isFile);		
		btnBrowse.setVisible(isFile);
		lblExportType.setVisible(true);
		compoExportType.setVisible(true);
		if ( comboDriver.getText().equalsIgnoreCase("neo4j") ) {
			//TODO : if neo4j ; pas d'export full
			btnWholeType.setEnabled(false);
			btnWholeType.setSelection(false);
			btnComponentsType.setEnabled(false);
			btnComponentsType.setSelection(true);
			btnExportViewImages.setEnabled(false);
			btnDoNotExportViewImages.setEnabled(false);
			lblNeo4jMode.setVisible(true);
			compoNeo4jMode.setVisible(true);
		}
		else {
			btnWholeType.setEnabled(txtName.getEnabled());
			btnComponentsType.setEnabled(txtName.getEnabled());
			btnExportViewImages.setEnabled(txtName.getEnabled());
			btnDoNotExportViewImages.setEnabled(txtName.getEnabled());
			lblNeo4jMode.setVisible(false);
			compoNeo4jMode.setVisible(false);
		}
		
		lblExportViewImages.setVisible(btnWholeType.getSelection());
		compoExportViewImages.setVisible(btnWholeType.getSelection());

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
		
			// we show up the edition widgets
		showDatabaseDetails(true);
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
		dbDatabase.setExportViewImages(btnExportViewImages.getSelection());
		dbDatabase.setNeo4jNativeMode(btnNeo4jNativeMode.getSelection());
		
		showDatabaseDetails(false);

		tableItem.setData(dbDatabase);
		tblDatabases.setSelection(tableItem);
		tblDatabases.notifyListeners(SWT.Selection, new Event());
	}
	
	private void showDatabaseDetails(boolean editMode) {
		DBDatabase dbDatabase = null;
		
		if ( tblDatabases.getSelectionIndex() != -1 )
			dbDatabase = (DBDatabase)tblDatabases.getItem(tblDatabases.getSelectionIndex()).getData();
	
		if ( !editMode )
			btnShowPassword.setSelection(true);
		showOrHidePasswordCallback();
				
		txtName.setEnabled(editMode);					txtName.setText(dbDatabase != null ? dbDatabase.getName(): "");
		comboDriver.setEnabled(editMode);				comboDriver.setText(dbDatabase != null ? dbDatabase.getDriver() : "");
		txtFile.setEnabled(editMode);					txtFile.setText(dbDatabase != null ? dbDatabase.getServer() : "");
		txtServer.setEnabled(editMode);					txtServer.setText(dbDatabase != null ? dbDatabase.getServer() : "");
		txtPort.setEnabled(editMode);					txtPort.setText(dbDatabase != null ? dbDatabase.getPort() : DBDatabase.getDefaultPort(comboDriver.getText()));
		txtDatabase.setEnabled(editMode);				txtDatabase.setText(dbDatabase != null ? dbDatabase.getDatabase() : "");
		txtUsername.setEnabled(editMode);				txtUsername.setText(dbDatabase != null ? dbDatabase.getUsername() : "");
		btnShowPassword.setEnabled(editMode);
		txtPassword.setEnabled(editMode);				txtPassword.setText(dbDatabase != null ? dbDatabase.getPassword() : "");
		btnWholeType.setEnabled(editMode);				btnWholeType.setSelection(dbDatabase != null ? dbDatabase.getExportWholeModel() : true);
		btnComponentsType.setEnabled(editMode);			btnComponentsType.setSelection(dbDatabase != null ? !dbDatabase.getExportWholeModel() : false);
		btnExportViewImages.setEnabled(editMode);		btnExportViewImages.setSelection(dbDatabase != null ? dbDatabase.getExportViewsImages() : true);
		btnDoNotExportViewImages.setEnabled(editMode);	btnDoNotExportViewImages.setSelection(dbDatabase != null ? !dbDatabase.getExportViewsImages() : false);
		btnNeo4jNativeMode.setEnabled(editMode);		btnNeo4jNativeMode.setSelection(dbDatabase != null ? dbDatabase.getNeo4jNativeMode() : true);
		btnNeo4jExtendedMode.setEnabled(editMode);		btnNeo4jExtendedMode.setSelection(dbDatabase != null ? !dbDatabase.getNeo4jNativeMode() : false);
		
		lblName.setVisible(true);
		txtName.setVisible(true);
		lblDriver.setVisible(true);
		comboDriver.setVisible(true);
		
		comboSelectionChanged();

		btnSave.setVisible(editMode);
		btnDiscard.setVisible(editMode);
		btnCheck.setVisible(editMode);
		
		btnNew.setEnabled(!editMode);
		btnEdit.setEnabled(!editMode && (tblDatabases.getSelection()!=null) && (tblDatabases.getSelection().length!=0));
		btnRemove.setEnabled(!editMode && (tblDatabases.getSelection()!=null) && (tblDatabases.getSelection().length!=0));
		btnUp.setEnabled(!editMode && (tblDatabases.getSelectionIndex() > 0));
		btnDown.setEnabled(!editMode && (tblDatabases.getSelectionIndex() < tblDatabases.getItemCount()-1));
		tblDatabases.setEnabled(!editMode);
		
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
			dbDatabase.check(true);
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
			return;
		}
		
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
		else {
			if ( index > 0 )
				tblDatabases.setSelection(index-1);
		}
		
		showDatabaseDetails(false);
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