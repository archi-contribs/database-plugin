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
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

	Table tblDatabases;
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
	
	Label lblBorderWidth;
	Text txtBorderWidth;
	Label lblBorderWidthPixels;
	
	Label lblScaleFactor;
	Text txtScaleFactor;
	Label lblScaleFactorPercent;
	
	private Label lblNeo4jMode;
	private Composite compoNeo4jMode;
	private Button btnNeo4jNativeMode;
	private Button btnNeo4jExtendedMode;
	private Label lblNeo4jEmpty;
	private Composite compoNeo4jEmpty;
	private Button btnNeo4jEmptyDB;
	private Button btnNeo4jDoNotEmptyDB;
	private Label lblNeo4jRelationships;
	private Composite compoNeo4jRelationships;
	private Button btnNeo4jStandardRelationships;
	private Button btnNeo4jTypedRelationships;
	private Label lblExportMode;
	private Composite compoExportMode;
	private Button btnStandaloneMode;
	private Button btnCollaborativeMode;
	private Label lblServer;
	private Text txtServer;
	private Label lblPort;
	private Text txtPort;
	private Label lblDatabase;
	private Text txtDatabase;
	private Label lblSchema;
	private Text txtSchema;
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
	@Override
    @SuppressWarnings("unused")
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		if ( logger.isTraceEnabled() ) logger.trace("doFillIntoGrid()");

		// we create a composite with layout as FormLayout
		this.grpDatabases = new Group(parent, SWT.NONE);
		this.grpDatabases.setFont(parent.getFont());
		this.grpDatabases.setLayout(new FormLayout());
		this.grpDatabases.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.grpDatabases.setText("Databases : ");

		this.btnUp = new Button(this.grpDatabases, SWT.NONE);
		this.btnUp.setText("^");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(100, -70);
		fd.right = new FormAttachment(100, -40);
		this.btnUp.setLayoutData(fd);
		this.btnUp.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { swapDatabaseEntries(-1); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnUp.setEnabled(false);

		this.btnDown = new Button(this.grpDatabases, SWT.NONE);
		this.btnDown.setText("v");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(100, -35);
		fd.right = new FormAttachment(100, -5);
		this.btnDown.setLayoutData(fd);
		this.btnDown.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { swapDatabaseEntries(1); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnDown.setEnabled(false);

		this.btnNew = new Button(this.grpDatabases, SWT.NONE);
		this.btnNew.setText("New");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnUp, 5);
		fd.left = new FormAttachment(100, -70);
		fd.right = new FormAttachment(100, -5);
		this.btnNew.setLayoutData(fd);
		this.btnNew.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { newCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});

		this.btnEdit = new Button(this.grpDatabases, SWT.NONE);
		this.btnEdit.setText("Edit");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnNew, 5);
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		this.btnEdit.setLayoutData(fd);
		this.btnEdit.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { setDatabaseDetails(true); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnEdit.setEnabled(false);

		this.btnCheck = new Button(this.grpDatabases, SWT.NONE);
		this.btnCheck.setText("Check");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnEdit, 5);
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		this.btnCheck.setLayoutData(fd);
		this.btnCheck.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { checkCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnCheck.setEnabled(false);

		this.btnRemove = new Button(this.grpDatabases, SWT.NONE);
		this.btnRemove.setText("Remove");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnCheck, 5);
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		this.btnRemove.setLayoutData(fd);
		this.btnRemove.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { removeCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnRemove.setEnabled(false);


		this.tblDatabases = new Table(this.grpDatabases, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.SINGLE);
		this.tblDatabases.setLinesVisible(true);
		fd = new FormData();
		fd.top = new FormAttachment(this.btnUp, 0, SWT.TOP);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(this.btnNew, -10, SWT.LEFT);
		fd.bottom = new FormAttachment(this.btnRemove, 0, SWT.BOTTOM);
		this.tblDatabases.setLayoutData(fd);
		this.tblDatabases.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				DBDatabaseEntryTableEditor.this.tblDatabases.getColumns()[0].setWidth(DBDatabaseEntryTableEditor.this.tblDatabases.getClientArea().width);
			}
		});
		this.tblDatabases.addListener(SWT.Selection, new Listener() {
			@Override
            public void handleEvent(Event e) {
				setDatabaseDetails(false);
			}
		});
		new TableColumn(this.tblDatabases, SWT.NONE);

		this.lblName = new Label(this.grpDatabases, SWT.NONE);
		this.lblName.setText("Name:");
		this.lblName.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.tblDatabases, 10);
		fd.left = new FormAttachment(this.tblDatabases, 30, SWT.LEFT);
		this.lblName.setLayoutData(fd);
		this.lblName.setVisible(false);

		this.txtName = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblName, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblName, 30);
		fd.right = new FormAttachment(this.tblDatabases, -20, SWT.RIGHT);
		this.txtName.setLayoutData(fd);
		this.txtName.setVisible(false);

		this.lblDriver = new Label(this.grpDatabases, SWT.NONE);
		this.lblDriver.setText("Driver:");
		this.lblDriver.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblName, 8);
		fd.left = new FormAttachment(this.lblName, 0 , SWT.LEFT);
		this.lblDriver.setLayoutData(fd);
		this.lblDriver.setVisible(false);

		this.comboDriver = new Combo(this.grpDatabases, SWT.READ_ONLY);
		this.comboDriver.setItems(DBDatabase.DRIVER_NAMES);
		this.comboDriver.setText(DBDatabase.DRIVER_NAMES[0]);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDriver, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		this.comboDriver.setLayoutData(fd);
		this.comboDriver.setVisible(false);
		this.comboDriver.addListener(SWT.Selection, new Listener() {
			@Override
            public void handleEvent(Event e) {
				driverChanged();		// when the database driver is changed, we call comboSelectionChanged()
				e.doit = true;
			}
		});

		this.lblFile = new Label(this.grpDatabases, SWT.NONE);
		this.lblFile.setText("File:");
		this.lblFile.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDriver, 8);
		fd.left = new FormAttachment(this.lblDriver, 0 , SWT.LEFT);
		this.lblFile.setLayoutData(fd);
		this.lblFile.setVisible(false);

		this.btnBrowse = new Button(this.grpDatabases, SWT.NONE);
		this.btnBrowse.setText("Browse");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblFile, 0, SWT.CENTER);
		fd.right = new FormAttachment(this.tblDatabases, -30, SWT.RIGHT);
		this.btnBrowse.setLayoutData(fd);
		this.btnBrowse.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { browseCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnBrowse.setVisible(false);

		this.txtFile = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblFile, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnBrowse, -10);
		this.txtFile.setLayoutData(fd);
		this.txtFile.setVisible(false);

		this.lblServer = new Label(this.grpDatabases, SWT.NONE);
		this.lblServer.setText("Server or IP:");
		this.lblServer.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDriver, 8);
		fd.left = new FormAttachment(this.lblDriver, 0 , SWT.LEFT);
		this.lblServer.setLayoutData(fd);
		this.lblServer.setVisible(false);

		this.txtServer = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblServer, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(45, -20);
		this.txtServer.setLayoutData(fd);
		this.txtServer.setVisible(false);

		this.lblPort = new Label(this.grpDatabases, SWT.NONE);
		this.lblPort.setText("Port:");
		this.lblPort.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblServer, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtServer, 30);
		this.lblPort.setLayoutData(fd);
		this.lblPort.setVisible(false);

		this.txtPort = new Text(this.grpDatabases, SWT.BORDER);
		this.txtPort.setTextLimit(5);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblPort, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblPort, 30);
		fd.width = 40;
		this.txtPort.setLayoutData(fd);
		this.txtPort.setVisible(false);
		this.txtPort.addModifyListener(this.setPortListener);
		this.txtPort.addVerifyListener(new VerifyListener() {
	        @Override
	        public void verifyText(VerifyEvent e) {
	            // get old text and create new text by using the VerifyEvent.text
	            final String oldString = ((Text)e.widget).getText();
	            String newString = oldString.substring(0, e.start) + e.text + oldString.substring(e.end);
	            try {
	                if ( DBPlugin.isEmpty(newString) )
	                	e.doit = true;
	                else {
	                	int port = Integer.parseInt(newString);
	                	e.doit = port > 0 && port < 65536;
	                }
	            } catch(NumberFormatException ign) {
	            	e.doit = false;
	            }
	        }
		});

		this.lblDatabase = new Label(this.grpDatabases, SWT.NONE);
		this.lblDatabase.setText("Database:");
		this.lblDatabase.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblServer, 8);
		fd.left = new FormAttachment(this.lblServer, 0 , SWT.LEFT);
		this.lblDatabase.setLayoutData(fd);
		this.lblDatabase.setVisible(false);

		this.txtDatabase = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(45, -20);
		this.txtDatabase.setLayoutData(fd);
		this.txtDatabase.setVisible(false);

		this.lblSchema = new Label(this.grpDatabases, SWT.NONE);
		this.lblSchema.setText("Schema:");
		this.lblSchema.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, 9, SWT.CENTER);
		fd.left = new FormAttachment(this.txtDatabase, 30);
		this.lblSchema.setLayoutData(fd);
		this.lblSchema.setVisible(false);

		this.txtSchema = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblSchema, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtPort, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.tblDatabases, -20, SWT.RIGHT);
		this.txtSchema.setLayoutData(fd);
		this.txtSchema.setVisible(false);

		this.lblUsername = new Label(this.grpDatabases, SWT.NONE);
		this.lblUsername.setText("Username:");
		this.lblUsername.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, 8);
		fd.left = new FormAttachment(this.lblDatabase, 0 , SWT.LEFT);
		this.lblUsername.setLayoutData(fd);
		this.lblUsername.setVisible(false);

		this.txtUsername = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblUsername, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(45, -20);
		this.txtUsername.setLayoutData(fd);
		this.txtUsername.setVisible(false);

		this.lblPassword = new Label(this.grpDatabases, SWT.NONE);
		this.lblPassword.setText("Password:");
		this.lblPassword.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblUsername, 9, SWT.CENTER);
		fd.left = new FormAttachment(this.lblPort, 0, SWT.LEFT);
		this.lblPassword.setLayoutData(fd);
		this.lblPassword.setVisible(false);

		this.txtPassword = new Text(this.grpDatabases, SWT.PASSWORD | SWT.BORDER);

		this.btnShowPassword = new Button(this.grpDatabases, SWT.TOGGLE);
		this.btnShowPassword.setImage(DBGui.LOCK_ICON);
		this.btnShowPassword.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblPassword, 0, SWT.CENTER);
		fd.right = new FormAttachment(this.tblDatabases, -20, SWT.RIGHT);
		this.btnShowPassword.setLayoutData(fd);
		this.btnShowPassword.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { showOrHidePasswordCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnShowPassword.setVisible(false);
		
		fd = new FormData();
		fd.top = new FormAttachment(this.lblPassword, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.txtPort, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnShowPassword);
		this.txtPassword.setLayoutData(fd);
		this.txtPassword.setVisible(false);

		this.lblExportType = new Label(this.grpDatabases, SWT.NONE);
		this.lblExportType.setText("Export type:");
		this.lblExportType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblUsername, 4);
		fd.left = new FormAttachment(this.lblUsername, 0 , SWT.LEFT);
		this.lblExportType.setLayoutData(fd);
		this.lblExportType.setVisible(false);
		this.lblExportType.setToolTipText("Please choose what information should be exported to the database.");


		this.compoExportType = new Composite(this.grpDatabases, SWT.NONE);
		this.compoExportType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoExportType.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportType, 0, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblExportType, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoExportType.setLayoutData(fd);
		RowLayout rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		this.compoExportType.setLayout(rl);

		this.btnWholeType = new Button(this.compoExportType, SWT.RADIO);
		this.btnWholeType.setText("Whole model");
		this.btnWholeType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.btnWholeType.addSelectionListener(new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) { driverChanged(); }
			@Override public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnWholeType.setToolTipText("The plugin will export the whole model content : elements, relationships, folders, views and images.\n   --> It will therefore be possible to import back your models from the database.");

		this.btnComponentsType = new Button(this.compoExportType, SWT.RADIO);
		this.btnComponentsType.setText("Elements and relationships only");
		this.btnComponentsType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.btnComponentsType.addSelectionListener(new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) { driverChanged(); }
			@Override public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnComponentsType.setToolTipText("The plugin will export the elements and relationships only (folders, views and images won't be exported).\n   --> This mode is useful for graph databases for instance, but please be careful, it won't be possible to import your models back from the database.");

		this.lblNeo4jMode = new Label(this.grpDatabases, SWT.NONE);
		this.lblNeo4jMode.setText("Export graph mode:");
		this.lblNeo4jMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportType, 4);
		fd.left = new FormAttachment(this.lblExportType, 0 , SWT.LEFT);
		this.lblNeo4jMode.setLayoutData(fd);
		this.lblNeo4jMode.setVisible(false);

		this.compoNeo4jMode = new Composite(this.grpDatabases, SWT.NONE);
		this.compoNeo4jMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoNeo4jMode.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblNeo4jMode, 0, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblNeo4jMode, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoNeo4jMode.setLayoutData(fd);
		rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		this.compoNeo4jMode.setLayout(rl);

		this.btnNeo4jNativeMode = new Button(this.compoNeo4jMode, SWT.RADIO);
		this.btnNeo4jNativeMode.setText("Native");
		this.btnNeo4jNativeMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);

		this.btnNeo4jExtendedMode = new Button(this.compoNeo4jMode, SWT.RADIO);
		this.btnNeo4jExtendedMode.setText("Extended");
		this.btnNeo4jExtendedMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		
		this.lblNeo4jEmpty = new Label(this.grpDatabases, SWT.NONE);
		this.lblNeo4jEmpty.setText("Empty database:");
		this.lblNeo4jEmpty.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblNeo4jMode, 4);
		fd.left = new FormAttachment(this.lblNeo4jMode, 0 , SWT.LEFT);
		this.lblNeo4jEmpty.setLayoutData(fd);
		this.lblNeo4jEmpty.setVisible(false);
		
		this.compoNeo4jEmpty = new Composite(this.grpDatabases, SWT.NONE);
		this.compoNeo4jEmpty.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoNeo4jEmpty.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblNeo4jEmpty, 0, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblNeo4jEmpty, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoNeo4jEmpty.setLayoutData(fd);
		rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		this.compoNeo4jEmpty.setLayout(rl);

		this.btnNeo4jEmptyDB = new Button(this.compoNeo4jEmpty, SWT.RADIO);
		this.btnNeo4jEmptyDB.setText("Empty database before every export");
		this.btnNeo4jEmptyDB.setBackground(DBGui.COMPO_BACKGROUND_COLOR);

		this.btnNeo4jDoNotEmptyDB = new Button(this.compoNeo4jEmpty, SWT.RADIO);
		this.btnNeo4jDoNotEmptyDB.setText("Leave database content");
		this.btnNeo4jDoNotEmptyDB.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		
		this.lblNeo4jRelationships = new Label(this.grpDatabases, SWT.NONE);
		this.lblNeo4jRelationships.setText("Relationships type:");
		this.lblNeo4jRelationships.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblNeo4jEmpty, 4);
		fd.left = new FormAttachment(this.lblNeo4jEmpty, 0 , SWT.LEFT);
		this.lblNeo4jRelationships.setLayoutData(fd);
		this.lblNeo4jRelationships.setVisible(false);
		
		this.compoNeo4jRelationships = new Composite(this.grpDatabases, SWT.NONE);
		this.compoNeo4jRelationships.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoNeo4jRelationships.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblNeo4jRelationships, 0, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblNeo4jRelationships, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoNeo4jRelationships.setLayoutData(fd);
		rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		this.compoNeo4jRelationships.setLayout(rl);

		this.btnNeo4jStandardRelationships = new Button(this.compoNeo4jRelationships, SWT.RADIO);
		this.btnNeo4jStandardRelationships.setText("Use unique \"relationships\" type");
		this.btnNeo4jStandardRelationships.setBackground(DBGui.COMPO_BACKGROUND_COLOR);

		this.btnNeo4jTypedRelationships = new Button(this.compoNeo4jRelationships, SWT.RADIO);
		this.btnNeo4jTypedRelationships.setText("Use typed relationships");
		this.btnNeo4jTypedRelationships.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		

		
		this.lblExportMode = new Label(this.grpDatabases, SWT.NONE);
		this.lblExportMode.setText("Export mode:");
		this.lblExportMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportType, 4);
		fd.left = new FormAttachment(this.lblExportType, 0 , SWT.LEFT);
		this.lblExportMode.setLayoutData(fd);
		this.lblExportMode.setVisible(false);
		this.lblExportMode.setToolTipText("Please choose how the plugin shoud export your data.");

		this.compoExportMode = new Composite(this.grpDatabases, SWT.NONE);
		this.compoExportMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoExportMode.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportMode, 0, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblExportMode, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoExportMode.setLayoutData(fd);
		rl = new RowLayout();
		rl.marginTop = 0;
		rl.marginLeft = 0;
		rl.spacing = 10;
		this.compoExportMode.setLayout(rl);

		this.btnCollaborativeMode = new Button(this.compoExportMode, SWT.RADIO);
		this.btnCollaborativeMode.setText("Collaborative mode");
		this.btnCollaborativeMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.btnCollaborativeMode.setToolTipText("The collaborative mode is a bit slower than the standalone mode but allows for several people to work on the same model at the same time."+
				"   --> While exporting your model, the plugin checks if components have been updated in the database while you were editing the model:\n"+
				"           - components updated in both your model and the database generate conflicts that need to be manually solved\n"+
				"           - components that have been created or updated in the database but not in the model are automatically imported without generating any conflict.");
		
		this.btnStandaloneMode = new Button(this.compoExportMode, SWT.RADIO);
		this.btnStandaloneMode.setText("Standalone mode");
		this.btnStandaloneMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.btnStandaloneMode.setToolTipText("The standalone mode is the quickest mode if only one person is working on a model at a time."+
				"   --> The plugin behaves as for archimate files : it exports your model as it is, without checking if components have been updated in the database while you were editing the model.");
		
		this.lblExportViewImages = new Label(this.grpDatabases, SWT.NONE);
		this.lblExportViewImages.setText("Export View Images:");
		this.lblExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportMode, 5);
		fd.left = new FormAttachment(this.lblExportMode, 0 , SWT.LEFT);
		this.lblExportViewImages.setLayoutData(fd);
		this.lblExportViewImages.setVisible(false);
		this.lblExportViewImages.setToolTipText("Please select if you wish to export a screenshot (jpg) of your views in the database.");

		this.compoExportViewImages = new Composite(this.grpDatabases, SWT.NONE);
		this.compoExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoExportViewImages.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportViewImages, -1, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblExportViewImages, 5, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 50, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoExportViewImages.setLayoutData(fd);
		this.compoExportViewImages.setLayout(new FormLayout());

		this.btnExportViewImages = new Button(this.compoExportViewImages, SWT.RADIO);
		this.btnExportViewImages.setText("Yes");
		this.btnExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(0);
		this.btnExportViewImages.setLayoutData(fd);
		this.btnExportViewImages.setToolTipText("The plugin will create views screenshots (jpg) and export them to the database.");
		this.btnExportViewImages.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    DBDatabaseEntryTableEditor.this.lblBorderWidth.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.txtBorderWidth.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.lblBorderWidthPixels.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.lblScaleFactor.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.txtScaleFactor.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.lblScaleFactorPercent.setEnabled(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);				
			}
		});

		this.btnDoNotExportViewImages = new Button(this.compoExportViewImages, SWT.RADIO);
		this.btnDoNotExportViewImages.setText("No");
		this.btnDoNotExportViewImages.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.btnExportViewImages, 10);
		this.btnDoNotExportViewImages.setLayoutData(fd);
		this.btnDoNotExportViewImages.setToolTipText("The plugin won't create any view screenshot.");
		this.btnDoNotExportViewImages.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    DBDatabaseEntryTableEditor.this.lblBorderWidth.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.txtBorderWidth.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.lblBorderWidthPixels.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.lblScaleFactor.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.txtScaleFactor.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.lblScaleFactorPercent.setEnabled(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);				
			}
		});

		this.lblBorderWidth = new Label(this.compoExportViewImages, SWT.NONE);
		this.lblBorderWidth.setText("Border width:");
		this.lblBorderWidth.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.btnDoNotExportViewImages, 40);
		this.lblBorderWidth.setLayoutData(fd);
		this.lblBorderWidth.setToolTipText("Please select the border width, in pixels, to add around the exported views images.");
		
		this.txtBorderWidth = new Text(this.compoExportViewImages, SWT.RIGHT | SWT.BORDER);
		this.txtBorderWidth.setText("10");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(this.lblBorderWidth, 3);
		fd.right = new FormAttachment(this.lblBorderWidth, 25, SWT.RIGHT);
		this.txtBorderWidth.setLayoutData(fd);
		this.txtBorderWidth.setToolTipText("Please choose the border width, in pixels, to add around the exported views images (between 0 and 50).");
		this.txtBorderWidth.addVerifyListener(new VerifyListener() {
	        @Override
	        public void verifyText(VerifyEvent e) {
	            // get old text and create new text by using the VerifyEvent.text
	            final String oldString = ((Text)e.widget).getText();
	            String newString = oldString.substring(0, e.start) + e.text + oldString.substring(e.end);
	            try {
	                if ( DBPlugin.isEmpty(newString) )
	                	e.doit = true;
	                else {
	                	int borderWidth = Integer.parseInt(newString);
	                	e.doit = borderWidth >= 0 && borderWidth <= 50;
	                }
	            } catch(NumberFormatException ign) {
	            	e.doit = false;
	            }
	        }
		});
		
		this.lblBorderWidthPixels = new Label(this.compoExportViewImages, SWT.NONE);
		this.lblBorderWidthPixels.setText("px");
		this.lblBorderWidthPixels.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.txtBorderWidth, 3);
		this.lblBorderWidthPixels.setLayoutData(fd);
		this.lblBorderWidthPixels.setToolTipText("Please choose the scale factor to resize the views images.");
		
		this.lblScaleFactor = new Label(this.compoExportViewImages, SWT.NONE);
		this.lblScaleFactor.setText("Scale factor:");
		this.lblScaleFactor.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.lblBorderWidthPixels, 40);
		this.lblScaleFactor.setLayoutData(fd);
		this.lblScaleFactor.setToolTipText("Please choose the scale factor to resize the views images.");
		
		this.txtScaleFactor = new Text(this.compoExportViewImages, SWT.RIGHT | SWT.BORDER);
		this.txtScaleFactor.setText("100");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(this.lblScaleFactor, 3);
		fd.right = new FormAttachment(this.lblScaleFactor, 30, SWT.RIGHT);
		this.txtScaleFactor.setLayoutData(fd);
		this.txtScaleFactor.setToolTipText("Please choose the scale factor to resize the views images (between 10% and 500%).");
		this.txtScaleFactor.addVerifyListener(new VerifyListener() {
	        @Override
	        public void verifyText(VerifyEvent e) {
	            // get old text and create new text by using the VerifyEvent.text
	            final String oldString = ((Text)e.widget).getText();
	            String newString = oldString.substring(0, e.start) + e.text + oldString.substring(e.end);
	            try {
	                if ( DBPlugin.isEmpty(newString) )
	                	e.doit = true;
	                else {
	                	int borderWidth = Integer.parseInt(newString);
	                	e.doit = borderWidth > 0 && borderWidth <= 500;
	                }
	            } catch(NumberFormatException ign) {
	            	e.doit = false;
	            }
	        }
		});
		
		this.lblScaleFactorPercent = new Label(this.compoExportViewImages, SWT.NONE);
		this.lblScaleFactorPercent.setText("%");
		this.lblScaleFactorPercent.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.txtScaleFactor, 3);
		this.lblScaleFactorPercent.setLayoutData(fd);
		this.lblScaleFactorPercent.setToolTipText("Please choose the scale factor to resize the views images.");
		
		this.compoExportViewImages.layout();
		
		this.btnSave = new Button(this.grpDatabases, SWT.NONE);
		this.btnSave.setText("Save");
		fd = new FormData();
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(100, -7);
		this.btnSave.setLayoutData(fd);
		this.btnSave.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { saveCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnSave.setVisible(false);

		this.btnDiscard = new Button(this.grpDatabases, SWT.NONE);
		this.btnDiscard.setText("Discard");
		fd = new FormData();
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(this.btnSave, -5, SWT.TOP);
		this.btnDiscard.setLayoutData(fd);
		this.btnDiscard.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { setDatabaseDetails(false); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnDiscard.setVisible(false);


		this.grpDatabases.setTabList(new Control[] {this.txtName, this.comboDriver, this.txtFile, this.btnBrowse, this.txtServer, this.txtPort, this.txtDatabase, this.txtSchema, this.txtUsername, this.txtPassword, this.compoExportType, this.compoExportViewImages, this.compoNeo4jMode, this.compoExportMode, this.btnDiscard, this.btnSave});

		this.grpDatabases.layout();

		GridData gd = new GridData();
		gd.heightHint = this.compoExportViewImages.getLocation().y + this.compoExportViewImages.getSize().y - 10;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		this.grpDatabases.setLayoutData(gd);
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
    protected void doLoad() {
		if ( logger.isTraceEnabled() ) logger.trace("doLoad()");

		this.tblDatabases.removeAll();

		for ( DBDatabaseEntry databaseEntry : DBDatabaseEntry.getAllDatabasesFromPreferenceStore(true) ) {
			TableItem tableItem = new TableItem(this.tblDatabases, SWT.NONE);
			tableItem.setText(databaseEntry.getName());
			tableItem.setData(databaseEntry);
		}

		if ( this.tblDatabases.getItemCount() != 0 ) {
			this.tblDatabases.setSelection(0);
			this.tblDatabases.notifyListeners(SWT.Selection, new Event());
		}
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
    protected void doStore() {
		if ( logger.isTraceEnabled() ) logger.trace("doStore()");

		List<DBDatabaseEntry> databaseEntries = new ArrayList<DBDatabaseEntry>();

		for ( TableItem tableItem : this.tblDatabases.getItems() )
			databaseEntries.add((DBDatabaseEntry)tableItem.getData());

		DBDatabaseEntry.setAllIntoPreferenceStore(databaseEntries);
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
    public int getNumberOfControls() {
		return 1;
	}

	/**
	 * Invoked when the selection in the driver combo has changed.
	 */
	protected void driverChanged() {
		boolean isFile = this.comboDriver.getText().equalsIgnoreCase("sqlite");
		boolean isNeo4j = this.comboDriver.getText().equalsIgnoreCase("neo4j");
		boolean hasDatabaseName = !isFile && !isNeo4j;
		boolean hasSchema = DBDatabase.get(this.comboDriver.getText()).hasSchema();
		
		this.lblFile.setVisible(isFile);
		this.txtFile.setVisible(isFile);		
		this.btnBrowse.setVisible(isFile);
		this.lblExportType.setVisible(true);
		this.compoExportType.setVisible(true);

		this.lblNeo4jMode.setVisible(isNeo4j);
		this.compoNeo4jMode.setVisible(isNeo4j);
		this.lblNeo4jEmpty.setVisible(isNeo4j);
		this.compoNeo4jEmpty.setVisible(isNeo4j);
		this.lblNeo4jRelationships.setVisible(isNeo4j);
		this.compoNeo4jRelationships.setVisible(isNeo4j);
		
		this.lblExportMode.setVisible(!isNeo4j);
		this.compoExportMode.setVisible(!isNeo4j);
		this.btnCollaborativeMode.setVisible(!isNeo4j);
		this.btnStandaloneMode.setVisible(!isNeo4j);
		
		this.lblServer.setVisible(!isFile);
		this.txtServer.setVisible(!isFile);
		this.lblPort.setVisible(!isFile);
		this.txtPort.setVisible(!isFile);
		this.lblDatabase.setVisible(hasDatabaseName);
		this.txtDatabase.setVisible(hasDatabaseName);
		if ( hasDatabaseName ) {
			FormData fd = new FormData();
			fd.top = new FormAttachment(this.lblDatabase, 8);
			fd.left = new FormAttachment(this.lblDatabase, 0 , SWT.LEFT);
			this.lblUsername.setLayoutData(fd);
		} else {
			FormData fd = new FormData();
			fd.top = new FormAttachment(this.lblServer, 8);
			fd.left = new FormAttachment(this.lblServer, 0 , SWT.LEFT);
			this.lblUsername.setLayoutData(fd);
		}
		this.grpDatabases.layout();
		this.lblSchema.setVisible(hasSchema);
		this.txtSchema.setVisible(hasSchema);
		this.lblUsername.setVisible(!isFile);
		this.txtUsername.setVisible(!isFile);
		this.lblPassword.setVisible(!isFile);
		this.txtPassword.setVisible(!isFile);
		this.btnShowPassword.setVisible(!isFile);
		
		this.lblExportViewImages.setVisible(this.btnWholeType.getSelection() && !isNeo4j);
		this.compoExportViewImages.setVisible(this.btnWholeType.getSelection() && !isNeo4j);

		FormData fd = new FormData();
		fd.top = new FormAttachment(isFile ? this.lblFile: this.lblUsername, 8);
		fd.left = new FormAttachment(this.lblUsername, 0 , SWT.LEFT);
		this.lblExportType.setLayoutData(fd);
		
		if ( this.comboDriver.getText().equalsIgnoreCase("ms-sql") ) {
			this.txtUsername.setToolTipText("Leave username and password empty to use Windows integrated security");
			this.txtPassword.setToolTipText("Leave username and password empty to use Windows integrated security");
			this.txtServer.setToolTipText("Specify \"server\\\\instance\" in case of named instance.");
		} else {
			this.txtUsername.setToolTipText(null);
			this.txtPassword.setToolTipText(null);
			this.txtServer.setToolTipText(null);
		}

		if ( DBPlugin.isEmpty((String)this.txtPort.getData("manualPort")) ) {
			this.txtPort.removeModifyListener(this.setPortListener);
			this.txtPort.setText(String.valueOf(DBDatabase.get(this.comboDriver.getText()).getDefaultPort()));
			this.txtPort.addModifyListener(this.setPortListener);
		}

		this.grpDatabases.layout();
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
    public void setFocus() {
		if ( this.tblDatabases != null )
			this.tblDatabases.setFocus();
	}

	/**
	 * Called when the "new" button has been pressed
	 */
	void newCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("newCallback()");

		// we unselect all the lines of the tblDatabases table
		this.tblDatabases.deselectAll();
		
		// we show up the edition widgets
		setDatabaseDetails(true);
	}

	/**
	 * Called when the "save" button has been pressed
	 */
	void saveCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("saveCallback()");

		if ( this.txtName.getText().isEmpty() ) {
		    DBGui.popup(Level.ERROR, "Please provide a name for your configuration.");
			return;
		}

		DBDatabaseEntry databaseEntry = null;
		TableItem tableItem;

		try {
			if ( this.tblDatabases.getSelectionIndex() >= 0 ) {
				databaseEntry = getDatabaseDetails((DBDatabaseEntry)this.tblDatabases.getItem(this.tblDatabases.getSelectionIndex()).getData());
				tableItem = this.tblDatabases.getSelection()[0];
			} else {
				databaseEntry = getDatabaseDetails(null);
				tableItem = new TableItem(this.tblDatabases, SWT.NONE);
			}
		} catch (Exception e) {
			DBGui.popup(Level.ERROR, "Please verify the information you provided", e);
			return;
		}
		tableItem.setText(this.txtName.getText());
        tableItem.setData(databaseEntry);
        this.tblDatabases.setSelection(tableItem);
        this.tblDatabases.notifyListeners(SWT.Selection, new Event());
        
		setDatabaseDetails(false);

		
	}

	private DBDatabaseEntry getDatabaseDetails(DBDatabaseEntry entry) throws NumberFormatException, Exception {
	    DBDatabaseEntry databaseEntry = entry==null ? new DBDatabaseEntry() : entry;

		databaseEntry.setName(this.txtName.getText());
		databaseEntry.setDriver(this.comboDriver.getText());
		databaseEntry.setServer(this.comboDriver.getText().equalsIgnoreCase("sqlite") ? this.txtFile.getText() : this.txtServer.getText());
		databaseEntry.setPort(this.txtPort.getText().isEmpty() ? 0 : Integer.valueOf(this.txtPort.getText()));
		databaseEntry.setDatabase(this.txtDatabase.getText());
		databaseEntry.setSchema(this.txtSchema.getText());
		databaseEntry.setUsername(this.txtUsername.getText());
		databaseEntry.setPassword(this.txtPassword.getText());
		databaseEntry.setWholeModelExported(this.btnWholeType.getSelection());
		databaseEntry.setViewSnapshotRequired(this.btnExportViewImages.getSelection());
		databaseEntry.setViewsImagesBorderWidth(Integer.valueOf(this.txtBorderWidth.getText()));
		databaseEntry.setViewsImagesScaleFactor(Integer.valueOf(this.txtScaleFactor.getText())<10 ? 10 : Integer.valueOf(this.txtScaleFactor.getText()));
		databaseEntry.setCollaborativeMode(this.btnCollaborativeMode.getSelection());
		databaseEntry.setNeo4jNativeMode(this.btnNeo4jNativeMode.getSelection());
		databaseEntry.setShouldEmptyNeo4jDB(this.btnNeo4jEmptyDB.getSelection());
		databaseEntry.setNeo4jTypedRelationship(this.btnNeo4jTypedRelationships.getSelection());

		return databaseEntry;
	}

	void setDatabaseDetails(boolean editMode) {
		DBDatabaseEntry databaseEntry = null;
		boolean shouldExportViewSnapshots = false;

		if ( this.tblDatabases.getSelectionIndex() == -1 ) {
		    this.txtName.setText("");
		    this.comboDriver.setText("");
		    this.txtFile.setText("");
	        this.txtServer.setText("");
	        this.txtPort.setText("");
	        this.txtDatabase.setText("");
	        this.txtSchema.setText("");
	        this.txtUsername.setText("");
	        this.txtPassword.setText("");
	        this.btnWholeType.setSelection(true);
	        this.btnComponentsType.setSelection(false);
	        this.btnNeo4jNativeMode.setSelection(false);
	        this.btnNeo4jExtendedMode.setSelection(true);
	        this.btnNeo4jEmptyDB.setSelection(false);
	        this.btnNeo4jDoNotEmptyDB.setSelection(true);
	        this.btnNeo4jStandardRelationships.setSelection(true);
	        this.btnNeo4jTypedRelationships.setSelection(false);
	        this.btnCollaborativeMode.setSelection(true);
	        this.btnStandaloneMode.setSelection(false);
	        this.btnExportViewImages.setSelection(false);
			this.txtBorderWidth.setText("10");
			this.txtScaleFactor.setText("100");
	        this.btnDoNotExportViewImages.setSelection(true);
		} else {
			databaseEntry = (DBDatabaseEntry)this.tblDatabases.getItem(this.tblDatabases.getSelectionIndex()).getData();

            this.txtName.setText(databaseEntry.getName());
            this.comboDriver.setText(databaseEntry.getDriver());
            this.txtFile.setText(databaseEntry.getServer());
            this.txtServer.setText(databaseEntry.getServer());
            this.txtPort.setText(String.valueOf(databaseEntry.getPort()));
            this.txtDatabase.setText(databaseEntry.getDatabase());
            this.txtSchema.setText(databaseEntry.getSchema());
            this.txtUsername.setText(databaseEntry.getUsername());
            this.txtPassword.setText(databaseEntry.getPassword());
            this.btnWholeType.setSelection(databaseEntry.isWholeModelExported());
            this.btnComponentsType.setSelection(!databaseEntry.isWholeModelExported());
            this.btnNeo4jNativeMode.setSelection(databaseEntry.isNeo4jNativeMode());
            this.btnNeo4jExtendedMode.setSelection(!databaseEntry.isNeo4jNativeMode());
            this.btnNeo4jEmptyDB.setSelection(databaseEntry.shouldEmptyNeo4jDB());
            this.btnNeo4jDoNotEmptyDB.setSelection(!databaseEntry.shouldEmptyNeo4jDB());
            this.btnCollaborativeMode.setSelection(databaseEntry.isCollaborativeMode());
            this.btnNeo4jStandardRelationships.setSelection(!databaseEntry.isNeo4jTypedRelationship());
            this.btnNeo4jTypedRelationships.setSelection(databaseEntry.isNeo4jTypedRelationship());
            this.btnStandaloneMode.setSelection(!databaseEntry.isCollaborativeMode());
            this.btnExportViewImages.setSelection(databaseEntry.isViewSnapshotRequired());
            this.btnDoNotExportViewImages.setSelection(!databaseEntry.isViewSnapshotRequired());
            this.txtBorderWidth.setText(String.valueOf(databaseEntry.getViewsImagesBorderWidth()));
            this.txtScaleFactor.setText(String.valueOf(databaseEntry.getViewsImagesScaleFactor()));
            
            shouldExportViewSnapshots = databaseEntry.isViewSnapshotRequired();
		}
		
		this.btnShowPassword.setSelection(!editMode);
		showOrHidePasswordCallback();

        this.lblName.setVisible(true);
        this.txtName.setVisible(true);
		this.txtName.setEnabled(editMode);
		
	    this.lblDriver.setVisible(true);
	    this.comboDriver.setVisible(true);
		this.comboDriver.setEnabled(editMode);				
		
		this.txtFile.setEnabled(editMode);
		this.txtServer.setEnabled(editMode);
		this.txtPort.setEnabled(editMode);
		this.txtDatabase.setEnabled(editMode);
		this.txtSchema.setEnabled(editMode);
		this.txtUsername.setEnabled(editMode);
		this.btnShowPassword.setEnabled(editMode);
		this.txtPassword.setEnabled(editMode);
		
		this.btnWholeType.setEnabled(editMode);
		this.btnComponentsType.setEnabled(editMode);
		
		this.btnNeo4jNativeMode.setEnabled(editMode);
		this.btnNeo4jExtendedMode.setEnabled(editMode);
		this.btnNeo4jEmptyDB.setEnabled(editMode);
		this.btnNeo4jDoNotEmptyDB.setEnabled(editMode);
		this.btnNeo4jStandardRelationships.setEnabled(editMode);
		this.btnNeo4jTypedRelationships.setEnabled(editMode);
		
	    this.btnCollaborativeMode.setEnabled(editMode);
	    this.btnStandaloneMode.setEnabled(editMode);
	    
	    this.btnExportViewImages.setEnabled(editMode);
	    this.btnDoNotExportViewImages.setEnabled(editMode);
	    this.lblBorderWidth.setEnabled(editMode && shouldExportViewSnapshots);
	    this.txtBorderWidth.setEnabled(editMode && shouldExportViewSnapshots);
	    this.lblBorderWidthPixels.setEnabled(editMode && shouldExportViewSnapshots);
	    this.lblScaleFactor.setEnabled(editMode && shouldExportViewSnapshots);
	    this.txtScaleFactor.setEnabled(editMode && shouldExportViewSnapshots);
	    this.lblScaleFactorPercent.setEnabled(editMode && shouldExportViewSnapshots);

		driverChanged();

		this.btnSave.setVisible(editMode);
		this.btnDiscard.setVisible(editMode);

		this.btnNew.setEnabled(!editMode);
		this.btnEdit.setEnabled(!editMode && (this.tblDatabases.getSelection()!=null) && (this.tblDatabases.getSelection().length!=0));
		this.btnRemove.setEnabled(!editMode && (this.tblDatabases.getSelection()!=null) && (this.tblDatabases.getSelection().length!=0));
		this.btnCheck.setEnabled(editMode || ((this.tblDatabases.getSelection()!=null) && (this.tblDatabases.getSelection().length!=0)));
		this.btnUp.setEnabled(!editMode && (this.tblDatabases.getSelectionIndex() > 0));
		this.btnDown.setEnabled(!editMode && (this.tblDatabases.getSelectionIndex() < this.tblDatabases.getItemCount()-1));
		this.tblDatabases.setEnabled(!editMode);

		this.grpDatabases.layout();
	}

	/**
	 * Called when the "check" button has been pressed
	 */
	void checkCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("checkCallback()");
		DBDatabaseEntry databaseEntry;
		try {
			databaseEntry = getDatabaseDetails(null);
		} catch (Exception e) {
			DBGui.popup(Level.ERROR, "Please verify the information you provided", e);
			return;
		}

		try ( DBDatabaseImportConnection connection = new DBDatabaseImportConnection(databaseEntry) ) {
			connection.checkDatabase();
			DBGui.popup(Level.INFO, "Database successfully checked.");
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
			return;
		}
	}

	/**
	 * Called when the "remove" button has been pressed
	 */
	void removeCallback() {
		if ( logger.isTraceEnabled() ) logger.trace("removeCallback()");
		// setPresentsDefaultValue(false);
		int index = this.tblDatabases.getSelectionIndex();

		this.tblDatabases.remove(index);

		if ( this.tblDatabases.getItemCount() > 0 ) {
			if ( index < this.tblDatabases.getItemCount() )
				this.tblDatabases.setSelection(index);
			else {
				if ( index > 0 )
					this.tblDatabases.setSelection(index-1);
			}
			setDatabaseDetails(false);
		} else {
			this.lblName.setVisible(false);
			this.txtName.setVisible(false);		
			this.lblDriver.setVisible(false);
			this.comboDriver.setVisible(false);
			this.lblFile.setVisible(false);
			this.txtFile.setVisible(false);		
			this.btnBrowse.setVisible(false);
			this.lblExportType.setVisible(false);
			this.compoExportType.setVisible(false);System.out.println("***************** compoExportType.setvisible(false) ********************");
			this.lblNeo4jMode.setVisible(false);
			this.compoNeo4jMode.setVisible(false);
			this.lblNeo4jEmpty.setVisible(false);
			this.compoNeo4jEmpty.setVisible(false);
			this.lblNeo4jRelationships.setVisible(false);
			this.compoNeo4jRelationships.setVisible(false);
			this.lblServer.setVisible(false);
			this.txtServer.setVisible(false);
			this.lblPort.setVisible(false);
			this.txtPort.setVisible(false);
			this.lblDatabase.setVisible(false);
			this.txtDatabase.setVisible(false);
			this.lblSchema.setVisible(false);
			this.txtSchema.setVisible(false);
			this.lblUsername.setVisible(false);
			this.txtUsername.setVisible(false);
			this.lblPassword.setVisible(false);
			this.txtPassword.setVisible(false);
			this.btnShowPassword.setVisible(false);
			this.lblExportViewImages.setVisible(false);
			this.compoExportViewImages.setVisible(false);
			this.lblExportMode.setVisible(false);
			this.compoExportMode.setVisible(false);

			this.btnSave.setVisible(false);
			this.btnDiscard.setVisible(false);

			this.btnNew.setEnabled(true);
			this.btnEdit.setEnabled(false);
			this.btnRemove.setEnabled(false);
			this.btnCheck.setEnabled(false);
			this.btnUp.setEnabled(false);
			this.btnDown.setEnabled(false);
			this.tblDatabases.setEnabled(true);

			this.grpDatabases.layout();
		}
	}

	/**
	 * Called when the "browse" button has been pressed
	 */
	void browseCallback() {
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SINGLE);
		dlg.setFileName(this.txtFile.getText());
		dlg.setFilterExtensions(new String[]{"*.sqlite", "*.sqlite2", "*.sqlite3", "*.db", "*.*"});
		if (dlg.open() != null) {
			StringBuffer buf = new StringBuffer(dlg.getFilterPath());
			if (buf.charAt(buf.length() - 1) != File.separatorChar)
				buf.append(File.separatorChar);
			buf.append(dlg.getFileName());
			this.txtFile.setText(buf.toString());
		}
	}

	/**
	 * Moves the currently selected item up or down.
	 *
	 * @param direction :
	 *            <code>true</code> if the item should move up, and
	 *            <code>false</code> if it should move down
	 */
	void swapDatabaseEntries(int direction) {
		if ( logger.isTraceEnabled() ) logger.trace("swap("+direction+")");

		int source = this.tblDatabases.getSelectionIndex();
		int target = this.tblDatabases.getSelectionIndex()+direction;

		if ( logger.isTraceEnabled() ) logger.trace("swapping entrie "+source+" and "+target+".");
		TableItem sourceItem = this.tblDatabases.getItem(source);
		String sourceText = sourceItem.getText();
		DBDatabaseEntry sourceData = (DBDatabaseEntry)sourceItem.getData();

		TableItem targetItem = this.tblDatabases.getItem(target);
		String targetText = targetItem.getText();
		DBDatabaseEntry targetData = (DBDatabaseEntry)targetItem.getData();

		sourceItem.setText(targetText);
		sourceItem.setData(targetData);
		targetItem.setText(sourceText);
		targetItem.setData(sourceData);

		this.tblDatabases.setSelection(target);
		this.tblDatabases.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Called when the "showPassword" button is pressed
	 */
	public void showOrHidePasswordCallback() {
		this.txtPassword.setEchoChar(this.btnShowPassword.getSelection() ? 0x25cf : '\0' );
		this.btnShowPassword.setImage(this.btnShowPassword.getSelection() ? DBGui.LOCK_ICON : DBGui.UNLOCK_ICON);
	}
	
	/**
	 * If we are in edit mode, then ask the user is if wants to save or discard
	 */
	public void close() {
		if ( this.txtName.isVisible() && this.txtName.isEnabled() ) {
			if ( DBGui.question("Do you wish to save or discard your currents updates ?", new String[] {"save", "discard"}) == 0 ) {
				saveCallback();
			}			
		}
	}
	
	private ModifyListener setPortListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			((Text)e.widget).setData("manualPort", ((Text)e.widget).getText());
		}
	};

    @Override
    protected void adjustForNumColumns(int numColumns) {
        // nothing to do
    }

    @Override
    protected void doLoadDefault() {
        // nothing to do
    }
}
