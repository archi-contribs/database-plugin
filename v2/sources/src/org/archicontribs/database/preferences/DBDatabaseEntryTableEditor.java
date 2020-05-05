/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.preferences;

import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.log4j.Level;
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiAdminDatabase;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBDatabase;
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
 * @author Herve Jouin
 */
public class DBDatabaseEntryTableEditor extends FieldEditor {
	Group grpDatabases;

	Table tblDatabases;
	Label lblName;
	Text txtName;
	Label lblDriver;
	Combo comboDriver;
	Label lblFile;
	Button btnBrowse;
	Text txtFile;

	Label lblExportViewsScreenshot;
	Composite compoExportViewsScreenshot;
	Button btnExportViewsScreenshot;
	Button btnDoNotExportViewsScreenshot;
	
	Label lblBorderWidth;
	Text txtBorderWidth;
	Label lblBorderWidthPixels;
	
	Label lblScaleFactor;
	Text txtScaleFactor;
	Label lblScaleFactorPercent;
	
	Label lblNeo4jMode;
	Composite compoNeo4jMode;
	Button btnNeo4jNativeMode;
	Button btnNeo4jExtendedMode;
	Label lblNeo4jEmpty;
	Composite compoNeo4jEmpty;
	Button btnNeo4jEmptyDB;
	Button btnNeo4jDoNotEmptyDB;
	Label lblNeo4jRelationships;
	Composite compoNeo4jRelationships;
	Button btnNeo4jStandardRelationships;
	Button btnNeo4jTypedRelationships;
	Label lblServer;
	Text txtServer;
	Label lblPort;
	Text txtPort;
	Label lblDatabase;
	Text txtDatabase;
	Label lblSchema;
	Text txtSchema;
	Label lblUsername;
	Text txtUsername;
	Label lblPassword;
	Button btnShowPassword;
	Text txtPassword;
	Label lblExpertMode;
	Button btnExpertMode;
	Label lblJdbc;
    Text txtJdbc;

	Button btnUp;
	Button btnNew;
	Button btnAdmin;
	Button btnRemove;
	Button btnEdit;
	Button btnDown;

	Button btnCheck;
	Button btnDiscard;
	Button btnSave;
	
    int defaultMargin = 10;
    int defaultLabelHeight;
    int defaultButtonHeight;

	/**
	 * Creates a table field editor.
	 * @param name 
	 * @param labelText 
	 * @param parent 
	 */
	public DBDatabaseEntryTableEditor(String name, String labelText, Composite parent) {
		init(name, labelText);
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
		// we create a composite with layout as FormLayout
		this.grpDatabases = new Group(parent, SWT.NONE);
		this.grpDatabases.setFont(parent.getFont());
		this.grpDatabases.setLayout(new FormLayout());
		this.grpDatabases.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.grpDatabases.setText("Databases: ");
		
	    /*
         * We calculate the default height of a Label widget
         */
        Label label = new Label(this.grpDatabases, SWT.NONE);
        label.setText("Test");
        this.defaultLabelHeight = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        label.dispose();
        
        /*
         * We calculate the default height of a Text widget
         */
        Button button = new Button(this.grpDatabases, SWT.NONE);
        button.setText("Test");
        this.defaultButtonHeight = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        button.dispose();
		
		// we calculate the required height of the group:
		//    height of the databases table  = height of 5 text widgets
		//    height of the database details = height of 8 text widgets
		int requiredHeight = 5 * (this.defaultButtonHeight + this.defaultMargin/2) + 8 * (this.defaultLabelHeight + this.defaultMargin);
		
		GridData gd = new GridData();
		gd.heightHint = requiredHeight;
		gd.minimumHeight = requiredHeight;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		this.grpDatabases.setLayoutData(gd);

		this.btnUp = new Button(this.grpDatabases, SWT.NONE);
		this.btnUp.setText("^");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, this.defaultMargin);
		fd.left = new FormAttachment(100, -80);
		fd.right = new FormAttachment(100, -45);
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
		fd.top = new FormAttachment(this.btnUp, 0, SWT.TOP);
		fd.left = new FormAttachment(100, -40);
		fd.right = new FormAttachment(100, -this.defaultMargin);
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
		fd.top = new FormAttachment(this.btnUp, this.defaultMargin/2);
		fd.left = new FormAttachment(100, -80);
		fd.right = new FormAttachment(100, -this.defaultMargin);
		this.btnNew.setLayoutData(fd);
		this.btnNew.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { try { newCallback(); } catch (SQLException ign) { /* */ } }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});

		this.btnEdit = new Button(this.grpDatabases, SWT.NONE);
		this.btnEdit.setText("Edit");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnNew, this.defaultMargin/2);
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

		this.btnAdmin = new Button(this.grpDatabases, SWT.NONE);
		this.btnAdmin.setText("Admin");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnEdit, this.defaultMargin/2);
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		this.btnAdmin.setLayoutData(fd);
		this.btnAdmin.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { adminCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnAdmin.setEnabled(false);

		this.btnRemove = new Button(this.grpDatabases, SWT.NONE);
		this.btnRemove.setText("Remove");
		fd = new FormData();
		fd.top = new FormAttachment(this.btnAdmin, this.defaultMargin/2);
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		this.btnRemove.setLayoutData(fd);
		this.btnRemove.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { try { removeCallback(); } catch (SQLException ign) { /* */ } }
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
            public void handleEvent(Event e) { setDatabaseDetails(false); }
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
		fd.top = new FormAttachment(this.lblName, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblName, 3, SWT.BOTTOM);
		fd.left = new FormAttachment(this.lblName, 45);
		fd.right = new FormAttachment(this.tblDatabases, -20, SWT.RIGHT);
		this.txtName.setLayoutData(fd);
		this.txtName.setVisible(false);

		this.lblDriver = new Label(this.grpDatabases, SWT.NONE);
		this.lblDriver.setText("Driver:");
		this.lblDriver.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblName, this.defaultMargin);
		fd.left = new FormAttachment(this.lblName, 0 , SWT.LEFT);
		this.lblDriver.setLayoutData(fd);
		this.lblDriver.setVisible(false);

		this.comboDriver = new Combo(this.grpDatabases, SWT.READ_ONLY);
		this.comboDriver.setItems(DBDatabase.DRIVER_NAMES);
		this.comboDriver.setText(DBDatabase.DRIVER_NAMES[0]);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDriver, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblDriver, 3, SWT.BOTTOM);
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
		
		this.lblExpertMode = new Label(this.grpDatabases, SWT.NONE);
		this.lblExpertMode.setText("Expert mode:");
        this.lblExpertMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblDriver, 0, SWT.CENTER);
        fd.left = new FormAttachment(45, 10);
        this.lblExpertMode.setLayoutData(fd);
        this.lblExpertMode.setVisible(false);

        this.btnExpertMode = new Button(this.grpDatabases, SWT.CHECK);
        this.btnExpertMode.setSelection(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblExpertMode, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblExpertMode, 5);
        this.btnExpertMode.setLayoutData(fd);
        this.btnExpertMode.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isExpertMode = DBDatabaseEntryTableEditor.this.btnExpertMode.getSelection();
                boolean isFile = DBDatabaseEntryTableEditor.this.comboDriver.getText().equalsIgnoreCase("sqlite");
                boolean isNeo4j = DBDatabaseEntryTableEditor.this.comboDriver.getText().equalsIgnoreCase("neo4j");
                boolean hasDatabaseName = !isFile && !isNeo4j;
                boolean hasSchema = DBDatabase.get(DBDatabaseEntryTableEditor.this.comboDriver.getText()).hasSchema();
                
                DBDatabaseEntryTableEditor.this.lblJdbc.setVisible(isExpertMode);
                DBDatabaseEntryTableEditor.this.txtJdbc.setVisible(isExpertMode);
                
                DBDatabaseEntryTableEditor.this.lblFile.setVisible(isFile && !isExpertMode);
                DBDatabaseEntryTableEditor.this.txtFile.setVisible(isFile && !isExpertMode);       
                DBDatabaseEntryTableEditor.this.btnBrowse.setVisible(isFile && !isExpertMode);
                
                DBDatabaseEntryTableEditor.this.lblServer.setVisible(!isFile && !isExpertMode);
                DBDatabaseEntryTableEditor.this.txtServer.setVisible(!isFile && !isExpertMode);
                DBDatabaseEntryTableEditor.this.lblPort.setVisible(!isFile && !isExpertMode);
                DBDatabaseEntryTableEditor.this.txtPort.setVisible(!isFile && !isExpertMode);
                DBDatabaseEntryTableEditor.this.lblDatabase.setVisible(hasDatabaseName && !isExpertMode);
                DBDatabaseEntryTableEditor.this.txtDatabase.setVisible(hasDatabaseName && !isExpertMode);
                
                DBDatabaseEntryTableEditor.this.lblSchema.setVisible(hasSchema && !isExpertMode);
                DBDatabaseEntryTableEditor.this.txtSchema.setVisible(hasSchema && !isExpertMode);
                DBDatabaseEntryTableEditor.this.lblUsername.setVisible(!isFile);
                DBDatabaseEntryTableEditor.this.txtUsername.setVisible(!isFile);
                DBDatabaseEntryTableEditor.this.lblPassword.setVisible(!isFile);
                DBDatabaseEntryTableEditor.this.txtPassword.setVisible(!isFile);
                DBDatabaseEntryTableEditor.this.btnShowPassword.setVisible(!isFile);
                
                // if the txtJdbc is empty, we generate it
                if ( DBDatabaseEntryTableEditor.this.txtJdbc.getText().isEmpty() ) {
                    try {
                        DBDatabaseEntryTableEditor.this.txtJdbc.setText(DBDatabaseEntry.getJdbcConnectionString(DBDatabaseEntryTableEditor.this.comboDriver.getText(), DBDatabaseEntryTableEditor.this.txtServer.getText(), Integer.parseInt(DBDatabaseEntryTableEditor.this.txtPort.getText()), DBDatabaseEntryTableEditor.this.txtDatabase.getText(), DBDatabaseEntryTableEditor.this.txtUsername.getText(), DBDatabaseEntryTableEditor.this.txtPassword.getText()));
                    } catch (NumberFormatException | SQLException ign) {
                        /* */
                    }
                }
                
                DBDatabaseEntryTableEditor.this.grpDatabases.layout();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
        });
        this.btnExpertMode.setVisible(false);
        
		this.lblFile = new Label(this.grpDatabases, SWT.NONE);
		this.lblFile.setText("File:");
		this.lblFile.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDriver, this.defaultMargin);
		fd.left = new FormAttachment(this.lblDriver, 0 , SWT.LEFT);
		this.lblFile.setLayoutData(fd);
		this.lblFile.setVisible(false);

		this.btnBrowse = new Button(this.grpDatabases, SWT.NONE);
		this.btnBrowse.setText("Browse");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblFile, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblFile, 3, SWT.BOTTOM);
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
		fd.top = new FormAttachment(this.lblFile, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblFile, 3, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnBrowse, -10);
		this.txtFile.setLayoutData(fd);
		this.txtFile.setVisible(false);
		
		this.lblJdbc = new Label(this.grpDatabases, SWT.NONE);
		this.lblJdbc.setText("JDBC string:");
        this.lblJdbc.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblDriver, this.defaultMargin);
        fd.left = new FormAttachment(this.lblDriver, 0 , SWT.LEFT);
        this.lblJdbc.setLayoutData(fd);
        this.lblJdbc.setVisible(false);
        
        this.txtJdbc = new Text(this.grpDatabases, SWT.BORDER);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblJdbc, -3, SWT.TOP);
        fd.bottom = new FormAttachment(this.lblJdbc, 3, SWT.BOTTOM);
        fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
        this.txtJdbc.setLayoutData(fd);
        this.txtJdbc.setVisible(false);

		this.lblServer = new Label(this.grpDatabases, SWT.NONE);
		this.lblServer.setText("Server or IP:");
		this.lblServer.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.comboDriver, this.defaultMargin);
		fd.left = new FormAttachment(this.lblFile, 0 , SWT.LEFT);
		this.lblServer.setLayoutData(fd);
		this.lblServer.setVisible(false);

		this.txtServer = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblServer, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblServer, 3, SWT.BOTTOM);
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
		fd.top = new FormAttachment(this.lblPort, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblPort, 3, SWT.BOTTOM);
		fd.left = new FormAttachment(this.btnExpertMode, 0, SWT.LEFT);
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
		fd.top = new FormAttachment(this.lblServer, this.defaultMargin);
		fd.left = new FormAttachment(this.lblServer, 0 , SWT.LEFT);
		this.lblDatabase.setLayoutData(fd);
		this.lblDatabase.setVisible(false);

		this.txtDatabase = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblDatabase, 3, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 0, SWT.LEFT);
		fd.right = new FormAttachment(45, -20);
		this.txtDatabase.setLayoutData(fd);
		this.txtDatabase.setVisible(false);

		this.lblSchema = new Label(this.grpDatabases, SWT.NONE);
		this.lblSchema.setText("Schema:");
		this.lblSchema.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, 9, SWT.CENTER);
		fd.left = new FormAttachment(this.lblExpertMode, 0, SWT.LEFT);
		this.lblSchema.setLayoutData(fd);
		this.lblSchema.setVisible(false);

		this.txtSchema = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblSchema, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblSchema, 3, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtPort, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.tblDatabases, -20, SWT.RIGHT);
		this.txtSchema.setLayoutData(fd);
		this.txtSchema.setVisible(false);

		this.lblUsername = new Label(this.grpDatabases, SWT.NONE);
		this.lblUsername.setText("Username:");
		this.lblUsername.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, this.defaultMargin);
		fd.left = new FormAttachment(this.lblDatabase, 0 , SWT.LEFT);
		this.lblUsername.setLayoutData(fd);
		this.lblUsername.setVisible(false);

		this.txtUsername = new Text(this.grpDatabases, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblUsername, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblUsername, 3, SWT.BOTTOM);
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
		this.txtPassword.setEchoChar((char)0x25cf);

		this.btnShowPassword = new Button(this.grpDatabases, SWT.TOGGLE);
		this.btnShowPassword.setImage(DBGui.LOCK_ICON);
		this.btnShowPassword.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblPassword, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblPassword, 3, SWT.BOTTOM);
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
		fd.top = new FormAttachment(this.lblPassword, -3, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblPassword, 3, SWT.BOTTOM);
		fd.left = new FormAttachment(this.btnExpertMode, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnShowPassword);
		this.txtPassword.setLayoutData(fd);
		this.txtPassword.setVisible(false);
		        
		this.lblNeo4jMode = new Label(this.grpDatabases, SWT.NONE);
		this.lblNeo4jMode.setText("Export graph mode:");
		this.lblNeo4jMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblUsername, 4);
		fd.left = new FormAttachment(this.lblUsername, 0 , SWT.LEFT);
		this.lblNeo4jMode.setLayoutData(fd);
		this.lblNeo4jMode.setVisible(false);

		this.compoNeo4jMode = new Composite(this.grpDatabases, SWT.NONE);
		this.compoNeo4jMode.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoNeo4jMode.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblNeo4jMode, 0, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblNeo4jMode, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(this.txtName, 65, SWT.LEFT);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoNeo4jMode.setLayoutData(fd);
		RowLayout rl = new RowLayout();
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
		fd.left = new FormAttachment(this.txtName, 65, SWT.LEFT);
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
		fd.left = new FormAttachment(this.txtName, 65, SWT.LEFT);
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
		

		this.lblExportViewsScreenshot = new Label(this.grpDatabases, SWT.NONE);
		this.lblExportViewsScreenshot.setText("Export views screenshot:");
		this.lblExportViewsScreenshot.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblUsername, 5);
		fd.left = new FormAttachment(this.lblUsername, 0 , SWT.LEFT);
		this.lblExportViewsScreenshot.setLayoutData(fd);
		this.lblExportViewsScreenshot.setVisible(false);
		this.lblExportViewsScreenshot.setToolTipText("Please select if you wish to export a screenshot (png) of your views in the database.");

		this.compoExportViewsScreenshot = new Composite(this.grpDatabases, SWT.NONE);
		this.compoExportViewsScreenshot.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		this.compoExportViewsScreenshot.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblExportViewsScreenshot, -1, SWT.TOP);
		fd.bottom = new FormAttachment(this.lblExportViewsScreenshot, 5, SWT.BOTTOM);
		fd.left = new FormAttachment(this.lblExportViewsScreenshot, 20);
		fd.right = new FormAttachment(this.txtName, 0, SWT.RIGHT);
		this.compoExportViewsScreenshot.setLayoutData(fd);
		this.compoExportViewsScreenshot.setLayout(new FormLayout());

		this.btnExportViewsScreenshot = new Button(this.compoExportViewsScreenshot, SWT.RADIO);
		this.btnExportViewsScreenshot.setText("Yes");
		this.btnExportViewsScreenshot.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(0);
		this.btnExportViewsScreenshot.setLayoutData(fd);
		this.btnExportViewsScreenshot.setToolTipText("The plugin will create views screenshots (jpg) and export them to the database.");
		this.btnExportViewsScreenshot.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    DBDatabaseEntryTableEditor.this.lblBorderWidth.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.txtBorderWidth.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.lblBorderWidthPixels.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.lblScaleFactor.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.txtScaleFactor.setEnabled(true);
			    DBDatabaseEntryTableEditor.this.lblScaleFactorPercent.setEnabled(true);
			    
                DBDatabaseEntryTableEditor.this.lblBorderWidth.setVisible(true);
                DBDatabaseEntryTableEditor.this.txtBorderWidth.setVisible(true);
                DBDatabaseEntryTableEditor.this.lblBorderWidthPixels.setVisible(true);
                DBDatabaseEntryTableEditor.this.lblScaleFactor.setVisible(true);
                DBDatabaseEntryTableEditor.this.txtScaleFactor.setVisible(true);
                DBDatabaseEntryTableEditor.this.lblScaleFactorPercent.setVisible(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);				
			}
		});

		this.btnDoNotExportViewsScreenshot = new Button(this.compoExportViewsScreenshot, SWT.RADIO);
		this.btnDoNotExportViewsScreenshot.setText("No");
		this.btnDoNotExportViewsScreenshot.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.btnExportViewsScreenshot, 10);
		this.btnDoNotExportViewsScreenshot.setLayoutData(fd);
		this.btnDoNotExportViewsScreenshot.setToolTipText("The plugin won't create any view screenshot.");
		this.btnDoNotExportViewsScreenshot.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    DBDatabaseEntryTableEditor.this.lblBorderWidth.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.txtBorderWidth.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.lblBorderWidthPixels.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.lblScaleFactor.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.txtScaleFactor.setEnabled(false);
			    DBDatabaseEntryTableEditor.this.lblScaleFactorPercent.setEnabled(false);
			    
                DBDatabaseEntryTableEditor.this.lblBorderWidth.setVisible(false);
                DBDatabaseEntryTableEditor.this.txtBorderWidth.setVisible(false);
                DBDatabaseEntryTableEditor.this.lblBorderWidthPixels.setVisible(false);
                DBDatabaseEntryTableEditor.this.lblScaleFactor.setVisible(false);
                DBDatabaseEntryTableEditor.this.txtScaleFactor.setVisible(false);
                DBDatabaseEntryTableEditor.this.lblScaleFactorPercent.setVisible(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);				
			}
		});

		this.lblBorderWidth = new Label(this.compoExportViewsScreenshot, SWT.NONE);
		this.lblBorderWidth.setText("Border width:");
		this.lblBorderWidth.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.btnDoNotExportViewsScreenshot, 34);
		this.lblBorderWidth.setLayoutData(fd);
		this.lblBorderWidth.setToolTipText("Please select the border width, in pixels, to add around the exported views images.");
		
		this.txtBorderWidth = new Text(this.compoExportViewsScreenshot, SWT.BORDER);
		this.txtBorderWidth.setText("10");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(this.lblBorderWidth, 5);
		fd.right = new FormAttachment(this.lblBorderWidth, 35, SWT.RIGHT);
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
		
		this.lblBorderWidthPixels = new Label(this.compoExportViewsScreenshot, SWT.NONE);
		this.lblBorderWidthPixels.setText("px");
		this.lblBorderWidthPixels.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblBorderWidth, 0, SWT.TOP);
		fd.left = new FormAttachment(this.txtBorderWidth, 5);
		this.lblBorderWidthPixels.setLayoutData(fd);
		this.lblBorderWidthPixels.setToolTipText("Please choose the scale factor to resize the views images.");
		
		this.lblScaleFactor = new Label(this.compoExportViewsScreenshot, SWT.NONE);
		this.lblScaleFactor.setText("Scale factor:");
		this.lblScaleFactor.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(5);
		fd.left = new FormAttachment(this.lblBorderWidthPixels, 25);
		this.lblScaleFactor.setLayoutData(fd);
		this.lblScaleFactor.setToolTipText("Please choose the scale factor to resize the views images.");
		
		this.txtScaleFactor = new Text(this.compoExportViewsScreenshot, SWT.BORDER);
		this.txtScaleFactor.setText("100");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(this.lblScaleFactor, 5);
		fd.right = new FormAttachment(this.lblScaleFactor, 50, SWT.RIGHT);
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
		
		this.lblScaleFactorPercent = new Label(this.compoExportViewsScreenshot, SWT.NONE);
		this.lblScaleFactorPercent.setText("%");
		this.lblScaleFactorPercent.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblScaleFactor, 0, SWT.TOP);
		fd.left = new FormAttachment(this.txtScaleFactor, 5);
		this.lblScaleFactorPercent.setLayoutData(fd);
		this.lblScaleFactorPercent.setToolTipText("Please choose the scale factor to resize the views images.");
		
		this.compoExportViewsScreenshot.layout();
		
		this.btnSave = new Button(this.grpDatabases, SWT.NONE);
		this.btnSave.setText("Save");
		fd = new FormData();
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(100, -7);
		this.btnSave.setLayoutData(fd);
		this.btnSave.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { try { saveCallback(); } catch (SQLException ign) { /* */ } }
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
		
		this.btnCheck = new Button(this.grpDatabases, SWT.NONE);
		this.btnCheck.setText("Check");
		fd = new FormData();
		fd.left = new FormAttachment(this.btnNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.btnNew, 0, SWT.RIGHT);
		fd.bottom = new FormAttachment(this.btnDiscard, -5, SWT.TOP);
		this.btnCheck.setLayoutData(fd);
		this.btnCheck.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { checkCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		this.btnCheck.setVisible(false);


		this.grpDatabases.setTabList(new Control[] {this.txtName, this.comboDriver, this.txtFile, this.btnBrowse, this.txtServer, this.txtPort, this.txtDatabase, this.txtSchema, this.txtUsername, this.txtPassword, this.compoExportViewsScreenshot, this.compoNeo4jMode, this.btnCheck, this.btnDiscard, this.btnSave});

		this.grpDatabases.layout();
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	@Override
    protected void doLoad() {

		this.tblDatabases.removeAll();

		for ( DBDatabaseEntry databaseEntry : DBDatabaseEntry.getAllDatabasesFromPreferenceStore() ) {
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
		List<DBDatabaseEntry> databaseEntries = new ArrayList<DBDatabaseEntry>();

		for ( TableItem tableItem : this.tblDatabases.getItems() )
			databaseEntries.add((DBDatabaseEntry)tableItem.getData());

		DBDatabaseEntry.persistDatabaseEntryListIntoPreferenceStore(databaseEntries);
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
	    boolean isExpertMode = this.btnExpertMode.getSelection();
		boolean isFile = this.comboDriver.getText().equalsIgnoreCase("sqlite");
		boolean isNeo4j = this.comboDriver.getText().equalsIgnoreCase("neo4j");
		boolean hasDatabaseName = !isFile && !isNeo4j;
		boolean hasSchema = DBDatabase.get(this.comboDriver.getText()).hasSchema();
		
		this.lblExpertMode.setVisible(true);
		this.btnExpertMode.setVisible(true);
		
		this.lblJdbc.setVisible(isExpertMode);
		this.txtJdbc.setVisible(isExpertMode);
		
		this.lblFile.setVisible(isFile && !isExpertMode);
		this.txtFile.setVisible(isFile && !isExpertMode);		
		this.btnBrowse.setVisible(isFile && !isExpertMode);

		this.lblNeo4jMode.setVisible(isNeo4j);
		this.compoNeo4jMode.setVisible(isNeo4j);
		this.lblNeo4jEmpty.setVisible(isNeo4j);
		this.compoNeo4jEmpty.setVisible(isNeo4j);
		this.lblNeo4jRelationships.setVisible(isNeo4j);
		this.compoNeo4jRelationships.setVisible(isNeo4j);
		
		this.lblServer.setVisible(!isFile && !isExpertMode);
		this.txtServer.setVisible(!isFile && !isExpertMode);
		this.lblPort.setVisible(!isFile && !isExpertMode);
		this.txtPort.setVisible(!isFile && !isExpertMode);
		this.lblDatabase.setVisible(hasDatabaseName && !isExpertMode);
		this.txtDatabase.setVisible(hasDatabaseName && !isExpertMode);
		
		if ( hasDatabaseName ) {
			FormData fd = new FormData();
			fd.top = new FormAttachment(this.lblDatabase, this.defaultMargin);
			fd.left = new FormAttachment(this.lblDatabase, 0 , SWT.LEFT);
			this.lblUsername.setLayoutData(fd);
		} else {
			FormData fd = new FormData();
			fd.top = new FormAttachment(this.lblServer, this.defaultMargin);
			fd.left = new FormAttachment(this.lblServer, 0 , SWT.LEFT);
			this.lblUsername.setLayoutData(fd);
		}
		
		this.lblSchema.setVisible(hasSchema);
		this.txtSchema.setVisible(hasSchema);
		this.lblUsername.setVisible(!isFile);
		this.txtUsername.setVisible(!isFile);
		this.lblPassword.setVisible(!isFile);
		this.txtPassword.setVisible(!isFile);
		this.btnShowPassword.setVisible(!isFile);
	    
		this.lblExportViewsScreenshot.setVisible(!isNeo4j);
		this.compoExportViewsScreenshot.setVisible(!isNeo4j);
		
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

        if ( isFile ) {
            FormData fd = new FormData();
            fd.top = new FormAttachment(this.lblFile, this.defaultMargin);
            fd.left = new FormAttachment(this.lblFile, 0 , SWT.LEFT);
            this.lblExportViewsScreenshot.setLayoutData(fd);
        } else {
            FormData fd = new FormData();
            fd.top = new FormAttachment(this.lblUsername, this.defaultMargin);
            fd.left = new FormAttachment(this.lblUsername, 0 , SWT.LEFT);
            this.lblExportViewsScreenshot.setLayoutData(fd);
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
	 * @throws SQLException 
	 */
	void newCallback() throws SQLException {
		// we unselect all the lines of the tblDatabases table
		this.tblDatabases.deselectAll();
		
		// we show up the edition widgets
		setDatabaseDetails(true);
	}

	/**
	 * Called when the "save" button has been pressed
	 * @throws SQLException 
	 */
	void saveCallback() throws SQLException {
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

	DBDatabaseEntry getDatabaseDetails(DBDatabaseEntry entry) throws NumberFormatException, Exception {
	    DBDatabaseEntry databaseEntry = entry==null ? new DBDatabaseEntry() : entry;

		databaseEntry.setName(this.txtName.getText());
		databaseEntry.setDriver(this.comboDriver.getText());
		databaseEntry.setServer(this.comboDriver.getText().equalsIgnoreCase("sqlite") ? this.txtFile.getText() : this.txtServer.getText());
		databaseEntry.setPort(this.txtPort.getText().isEmpty() ? 0 : Integer.valueOf(this.txtPort.getText()));
		databaseEntry.setDatabase(this.txtDatabase.getText());
		databaseEntry.setSchema(this.txtSchema.getText());
		databaseEntry.setUsername(this.txtUsername.getText());
		databaseEntry.setDecryptedPassword(this.txtPassword.getText());
		databaseEntry.setViewSnapshotRequired(this.btnExportViewsScreenshot.getSelection());
		databaseEntry.setViewsImagesBorderWidth(Integer.valueOf(this.txtBorderWidth.getText()));
		databaseEntry.setViewsImagesScaleFactor(Integer.valueOf(this.txtScaleFactor.getText())<10 ? 10 : Integer.valueOf(this.txtScaleFactor.getText()));
		databaseEntry.setNeo4jNativeMode(this.btnNeo4jNativeMode.getSelection());
		databaseEntry.setShouldEmptyNeo4jDB(this.btnNeo4jEmptyDB.getSelection());
		databaseEntry.setNeo4jTypedRelationship(this.btnNeo4jTypedRelationships.getSelection());
		databaseEntry.setExpertMode(this.btnExpertMode.getSelection());
		databaseEntry.setJdbcConnectionString(this.txtJdbc.getText());

		return databaseEntry;
	}

	void setDatabaseDetails(boolean editMode) {
		DBDatabaseEntry databaseEntry = null;
		boolean shouldExportViewSnapshots = false;
		boolean shouldActivateAllowButton = false;

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
	        this.btnExpertMode.setSelection(false);
	        this.txtJdbc.setText("");
	        this.btnNeo4jNativeMode.setSelection(false);
	        this.btnNeo4jExtendedMode.setSelection(true);
	        this.btnNeo4jEmptyDB.setSelection(false);
	        this.btnNeo4jDoNotEmptyDB.setSelection(true);
	        this.btnNeo4jStandardRelationships.setSelection(true);
	        this.btnNeo4jTypedRelationships.setSelection(false);
	        this.btnExportViewsScreenshot.setSelection(false);
			this.txtBorderWidth.setText("10");
			this.txtScaleFactor.setText("100");
	        this.btnDoNotExportViewsScreenshot.setSelection(true);
		} else {
			databaseEntry = (DBDatabaseEntry)this.tblDatabases.getItem(this.tblDatabases.getSelectionIndex()).getData();

            this.txtName.setText(databaseEntry.getName());
            this.comboDriver.setText(databaseEntry.getDriver());
            if ( !databaseEntry.getDriver().equalsIgnoreCase("neo4j") )
            	shouldActivateAllowButton = true;
            this.txtFile.setText(databaseEntry.getServer());
            this.txtServer.setText(databaseEntry.getServer());
            this.txtPort.setText(String.valueOf(databaseEntry.getPort()));
            this.txtDatabase.setText(databaseEntry.getDatabase());
            this.txtSchema.setText(databaseEntry.getSchema());
            this.txtUsername.setText(databaseEntry.getUsername());
            try {
				this.txtPassword.setText(databaseEntry.getDecryptedPassword());
			} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException err) {
				DBGui.popup(Level.ERROR, "Failed to decrypt the password.", err);
				this.txtPassword.setText("");
			}
            this.btnExpertMode.setSelection(databaseEntry.isExpertMode());
            this.txtJdbc.setText(databaseEntry.getJdbcConnectionString());
            this.btnNeo4jNativeMode.setSelection(databaseEntry.isNeo4jNativeMode());
            this.btnNeo4jExtendedMode.setSelection(!databaseEntry.isNeo4jNativeMode());
            this.btnNeo4jEmptyDB.setSelection(databaseEntry.shouldEmptyNeo4jDB());
            this.btnNeo4jDoNotEmptyDB.setSelection(!databaseEntry.shouldEmptyNeo4jDB());
            this.btnNeo4jStandardRelationships.setSelection(!databaseEntry.isNeo4jTypedRelationship());
            this.btnNeo4jTypedRelationships.setSelection(databaseEntry.isNeo4jTypedRelationship());
            this.btnExportViewsScreenshot.setSelection(databaseEntry.isViewSnapshotRequired());
            this.btnDoNotExportViewsScreenshot.setSelection(!databaseEntry.isViewSnapshotRequired());
            this.txtBorderWidth.setText(String.valueOf(databaseEntry.getViewsImagesBorderWidth()));
            this.txtScaleFactor.setText(String.valueOf(databaseEntry.getViewsImagesScaleFactor()));
            
            shouldExportViewSnapshots = databaseEntry.isViewSnapshotRequired();
		}
		
		showOrHidePasswordCallback();

        this.lblName.setVisible(true);
        this.txtName.setVisible(true);
		this.txtName.setEnabled(editMode);
		
	    this.lblDriver.setVisible(true);
	    this.comboDriver.setVisible(true);
		this.comboDriver.setEnabled(editMode);
		
		this.lblExpertMode.setVisible(true);
		this.btnExpertMode.setVisible(true);
		this.btnExpertMode.setEnabled(editMode);
		
		this.txtFile.setEnabled(editMode);
		this.txtJdbc.setEnabled(editMode);
		this.txtServer.setEnabled(editMode);
		this.txtPort.setEnabled(editMode);
		this.txtDatabase.setEnabled(editMode);
		this.txtSchema.setEnabled(editMode);
		this.txtUsername.setEnabled(editMode);
		this.btnShowPassword.setEnabled(editMode);
		this.txtPassword.setEnabled(editMode);
		
		this.btnNeo4jNativeMode.setEnabled(editMode);
		this.btnNeo4jExtendedMode.setEnabled(editMode);
		this.btnNeo4jEmptyDB.setEnabled(editMode);
		this.btnNeo4jDoNotEmptyDB.setEnabled(editMode);
		this.btnNeo4jStandardRelationships.setEnabled(editMode);
		this.btnNeo4jTypedRelationships.setEnabled(editMode);
	    
	    this.btnExportViewsScreenshot.setEnabled(editMode);
	    this.btnDoNotExportViewsScreenshot.setEnabled(editMode);
	    this.lblBorderWidth.setEnabled(editMode);
	    this.lblBorderWidth.setVisible(shouldExportViewSnapshots);
	    this.txtBorderWidth.setEnabled(editMode);
	    this.txtBorderWidth.setVisible(shouldExportViewSnapshots);
	    this.lblBorderWidthPixels.setEnabled(editMode);
	    this.lblBorderWidthPixels.setVisible(shouldExportViewSnapshots);
	    this.lblScaleFactor.setEnabled(editMode);
	    this.lblScaleFactor.setVisible(shouldExportViewSnapshots);
	    this.txtScaleFactor.setEnabled(editMode);
	    this.txtScaleFactor.setVisible(shouldExportViewSnapshots);
	    this.lblScaleFactorPercent.setEnabled(editMode);
	    this.lblScaleFactorPercent.setVisible(shouldExportViewSnapshots);

		driverChanged();

		this.btnSave.setVisible(editMode);
		this.btnDiscard.setVisible(editMode);
		this.btnCheck.setVisible(editMode);

		this.btnNew.setEnabled(!editMode);
		this.btnEdit.setEnabled(!editMode && (this.tblDatabases.getSelection()!=null) && (this.tblDatabases.getSelection().length!=0));
		this.btnRemove.setEnabled(!editMode && (this.tblDatabases.getSelection()!=null) && (this.tblDatabases.getSelection().length!=0));
		this.btnAdmin.setEnabled(!editMode && (this.tblDatabases.getSelection()!=null) && (this.tblDatabases.getSelection().length!=0) && shouldActivateAllowButton);
		this.btnUp.setEnabled(!editMode && (this.tblDatabases.getSelectionIndex() > 0));
		this.btnDown.setEnabled(!editMode && (this.tblDatabases.getSelectionIndex() < this.tblDatabases.getItemCount()-1));
		this.tblDatabases.setEnabled(!editMode);

		this.grpDatabases.layout();
	}

	/**
	 * Called when the "check" button has been pressed
	 */
	void checkCallback() {
		DBDatabaseEntry databaseEntry;
		try {
			databaseEntry = getDatabaseDetails(null);
		} catch (Exception e) {
			DBGui.popup(Level.ERROR, "Please verify the information you provided", e);
			return;
		}

		try ( DBDatabaseImportConnection connection = new DBDatabaseImportConnection(databaseEntry) ) {
			connection.checkDatabase(null);
			DBGui.popup(Level.INFO, "Database successfully checked.");
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
		}
	}
	
	/**
	 * Called when the "admin" button has been pressed
	 */
	void adminCallback() {
		DBDatabaseEntry databaseEntry;
		try {
			databaseEntry = getDatabaseDetails(null);
		} catch (Exception e) {
			DBGui.popup(Level.ERROR, "Please verify the information you provided", e);
			return;
		}
		
		try ( DBDatabaseImportConnection connection = new DBDatabaseImportConnection(databaseEntry) ) {
			connection.checkDatabase(null);
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
			return;
		}

		try ( DBDatabaseImportConnection connection = new DBDatabaseImportConnection(databaseEntry) ) {
			List<DBDatabaseEntry> entries = new ArrayList<DBDatabaseEntry>(); 
	        for ( int i = 0 ; i < this.tblDatabases.getItemCount() ; ++i ) {
	        	entries.add((DBDatabaseEntry)this.tblDatabases.getItem(i).getData());
	        }
			
        	DBGuiAdminDatabase adminDatabase = new DBGuiAdminDatabase(connection, entries, "Administer database \""+databaseEntry.getName()+"\"");
        	adminDatabase.run();
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot admin the database", e);
        }
	}

	/**
	 * Called when the "remove" button has been pressed
	 * @throws SQLException 
	 */
	void removeCallback() throws SQLException {
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
			this.lblExportViewsScreenshot.setVisible(false);
			this.compoExportViewsScreenshot.setVisible(false);

			this.btnSave.setVisible(false);
			this.btnDiscard.setVisible(false);
			this.btnCheck.setVisible(false);

			this.btnNew.setEnabled(true);
			this.btnEdit.setEnabled(false);
			this.btnRemove.setEnabled(false);
			this.btnAdmin.setEnabled(false);
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
		FileDialog dlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.SINGLE | SWT.SAVE);
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
	 * @param direction
	 *            <code>true</code> if the item should move up, and
	 *            <code>false</code> if it should move down
	 */
	void swapDatabaseEntries(int direction) {
		int source = this.tblDatabases.getSelectionIndex();
		int target = this.tblDatabases.getSelectionIndex()+direction;

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
	 * @throws SQLException 
	 */
	public void close() throws SQLException {
		if ( this.txtName.isVisible() && this.txtName.isEnabled() ) {
			if ( DBGui.question("Do you wish to save or discard your currents updates ?", new String[] {"save", "discard"}) == 0 ) {
				saveCallback();
			}			
		}
	}
	
	ModifyListener setPortListener = new ModifyListener() {
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
