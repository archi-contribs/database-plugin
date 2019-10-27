/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.log4j.Level;
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class DBGuiAdminDatabase extends DBGui {
    @SuppressWarnings("hiding")
	protected static final DBLogger logger = new DBLogger(DBGuiAdminDatabase.class);

    DBDatabaseImportConnection importConnection;
    
    Table tblModels;
    Table tblModelVersions;
    Text txtFilterModels;

    private Group grpModels;
    private Group grpModelVersions;
    private Group grpActions;
    
    private Label lblModelName;
    Text txtModelName;
    private Label lblPurpose;
    Text txtPurpose;
    private Label lblReleaseNote;
    Text txtReleaseNote;
    
    Button btnCheckStructure;
    Button btnCheckContent;
    Button btnDeleteModel;
    Button btnDeleteVersion;
    

    /**
     * Creates the GUI to import a model
     * @param title Title of the window
     * @throws Exception 
     */
    public DBGuiAdminDatabase(DBDatabaseImportConnection databaseImportconnection, String title) throws Exception {
        super(title);
        
        this.importConnection = databaseImportconnection;

        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for administering the \""+databaseImportconnection.getDatabaseEntry().getName()+"\" database (plugin version "+DBPlugin.pluginVersion.getVersion()+").");

        createAction(ACTION.One, "1 - Admin database");
        setActiveAction(ACTION.One);

        // We deactivate the btnDoAction button
        setBtnAction(null, null);

        // We activate the Eclipse Help framework
        //TODO: setHelpHref("adminDatabase.html");

        // we hide the compoRightTop that is no use here and move the compoRightBottom up
        this.compoRightTop.setVisible(false);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(100, -100);
        this.compoRightBottom.setLayoutData(fd);
        
        createGrpModel();
        createGrpActions();

        // we set the comboDatabase entries and select the database
        this.databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore(this.includeNeo4j);
        int index = 0;
        for (DBDatabaseEntry databaseEntry: this.databaseEntries) {
            this.comboDatabases.add(databaseEntry.getName());
            if ( databaseEntry.getName().equals(databaseImportconnection.getDatabaseEntry().getName()) )
            	this.comboDatabases.select(index);
            ++index;
        }
        databaseSelected();
    }

    @Override
    protected void databaseSelectedCleanup() {
        if ( this.tblModels != null ) {
            this.tblModels.removeAll();
        }
        if ( this.tblModelVersions != null ) {
            this.tblModelVersions.removeAll();
        }
    }
    
    /**
     * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
     */
    @Override
    protected void connectedToDatabase(boolean ignore) {
        this.importConnection = new DBDatabaseImportConnection(getDatabaseConnection());
        
        this.compoRightBottom.setVisible(true);
        this.compoRightBottom.layout();
        
        this.tblModels.removeAll();
        
        this.txtFilterModels.notifyListeners(SWT.Modify, new Event());		// refreshes the list of models in the database
        
        this.tblModels.layout();
        this.tblModels.setVisible(true);
        this.tblModels.setLinesVisible(true);
        this.tblModels.setRedraw(true);
        if (logger.isTraceEnabled() ) logger.trace("   found "+this.tblModels.getItemCount()+" model"+(this.tblModels.getItemCount()>1?"s":"")+" in total");
        
        if ( this.tblModels.getItemCount() != 0 ) {
            this.tblModels.setSelection(0);
            this.tblModels.notifyListeners(SWT.Selection, new Event());      // calls database.getModelVersions()
        }
    }


    protected void createGrpModel() {
        this.grpModels = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
        this.grpModels.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpModels.setFont(GROUP_TITLE_FONT);
        this.grpModels.setText("Models: ");
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(50, -5);
        fd.bottom = new FormAttachment(100);
        this.grpModels.setLayoutData(fd);
        this.grpModels.setLayout(new FormLayout());

        Label lblListModels = new Label(this.grpModels, SWT.NONE);
        lblListModels.setBackground(GROUP_BACKGROUND_COLOR);
        lblListModels.setText("Filter:");
        fd = new FormData();
        fd.top = new FormAttachment(0, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        lblListModels.setLayoutData(fd);

        this.txtFilterModels = new Text(this.grpModels, SWT.BORDER);
        this.txtFilterModels.setToolTipText("You may use '%' as wildcard.");
        this.txtFilterModels.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                DBGuiAdminDatabase.this.tblModels.removeAll();
                DBGuiAdminDatabase.this.tblModelVersions.removeAll();
                try {
                    for (Hashtable<String, Object> model : DBGuiAdminDatabase.this.importConnection.getModels("%"+DBGuiAdminDatabase.this.txtFilterModels.getText()+"%")) {
                        TableItem tableItem = new TableItem(DBGuiAdminDatabase.this.tblModels, SWT.BORDER);
                        tableItem.setText((String)model.get("name"));
                        tableItem.setData("id", model.get("id"));
                    }
                } catch (Exception err) {
                    DBGui.popup(Level.ERROR, "Failed to get the list of models in the database.", err);
                } 
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblListModels, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblListModels, 5);
        fd.right = new FormAttachment(100, -getDefaultMargin());
        this.txtFilterModels.setLayoutData(fd);



        this.tblModels = new Table(this.grpModels, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        this.tblModels.setLinesVisible(true);
        this.tblModels.setBackground(TABLE_BACKGROUND_COLOR);
        this.tblModels.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
               	DBGuiAdminDatabase.this.tblModelVersions.removeAll();
                	
            	try {
                    for (Hashtable<String, Object> version : DBGuiAdminDatabase.this.importConnection.getModelVersions((String) DBGuiAdminDatabase.this.tblModels.getSelection()[0].getData("id")) ) {
                    	if ( DBGuiAdminDatabase.this.tblModelVersions.getItemCount() == 0 ) {
	                    	// if the first line, then we add the "latest version"
	        				TableItem tableItem = new TableItem(DBGuiAdminDatabase.this.tblModelVersions, SWT.NULL);
	        				tableItem.setText(1, "(latest version)");
	        				tableItem.setData("name", version.get("name"));
	        				tableItem.setData("note", version.get("note"));
	        				tableItem.setData("purpose", version.get("purpose"));
        				}
        				
                    	TableItem tableItem = new TableItem(DBGuiAdminDatabase.this.tblModelVersions, SWT.NULL);
            			tableItem.setText(0, (String)version.get("version"));
            			tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format((Timestamp)version.get("created_on")));
            			tableItem.setText(2, (String)version.get("created_by"));
            			tableItem.setData("name", version.get("name"));
            			tableItem.setData("note", version.get("note"));
            			tableItem.setData("purpose", version.get("purpose"));
                    }
                } catch (Exception err) {
                    DBGui.popup(Level.ERROR, "Failed to get model's versions from the database", err);
                }
            	
	    		if ( DBGuiAdminDatabase.this.tblModelVersions.getItemCount() != 0 ) {
	    			DBGuiAdminDatabase.this.tblModelVersions.setSelection(0);
	    			DBGuiAdminDatabase.this.tblModelVersions.notifyListeners(SWT.Selection, new Event());       // calls database.getModelVersions()
	    		}
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblListModels, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(100, -getDefaultMargin());
        this.tblModels.setLayoutData(fd);

        TableColumn colModelName = new TableColumn(this.tblModels, SWT.NONE);
        colModelName.setText("Model name");
        colModelName.setWidth(265);

        this.grpModelVersions = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
        this.grpModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpModelVersions.setFont(GROUP_TITLE_FONT);
        this.grpModelVersions.setText("Versions of selected model: ");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(50, 5);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpModelVersions.setLayoutData(fd);
        this.grpModelVersions.setLayout(new FormLayout());

        this.tblModelVersions = new Table(this.grpModelVersions,  SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        this.tblModelVersions.setBackground(TABLE_BACKGROUND_COLOR);
        this.tblModelVersions.setLinesVisible(true);
        this.tblModelVersions.setHeaderVisible(true);
        this.tblModelVersions.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if ( (DBGuiAdminDatabase.this.tblModelVersions.getSelection() != null) && (DBGuiAdminDatabase.this.tblModelVersions.getSelection().length > 0) && (DBGuiAdminDatabase.this.tblModelVersions.getSelection()[0] != null) ) {
                    DBGuiAdminDatabase.this.txtReleaseNote.setText((String) DBGuiAdminDatabase.this.tblModelVersions.getSelection()[0].getData("note"));
                    DBGuiAdminDatabase.this.txtPurpose.setText((String) DBGuiAdminDatabase.this.tblModelVersions.getSelection()[0].getData("purpose"));
                    DBGuiAdminDatabase.this.txtModelName.setText((String) DBGuiAdminDatabase.this.tblModelVersions.getSelection()[0].getData("name"));
                    DBGuiAdminDatabase.this.btnDoAction.setEnabled(true);
                } else {
                    DBGuiAdminDatabase.this.btnDoAction.setEnabled(false);
                }
            }
        });
        this.tblModelVersions.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if ( DBGuiAdminDatabase.this.btnDoAction.getEnabled() )
                    DBGuiAdminDatabase.this.btnDoAction.notifyListeners(SWT.Selection, new Event());
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(0, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(50);
        this.tblModelVersions.setLayoutData(fd);

        TableColumn colVersion = new TableColumn(this.tblModelVersions, SWT.NONE);
        colVersion.setText("#");
        colVersion.setWidth(40);

        TableColumn colCreatedOn = new TableColumn(this.tblModelVersions, SWT.NONE);
        colCreatedOn.setText("Date");
        colCreatedOn.setWidth(120);

        TableColumn colCreatedBy = new TableColumn(this.tblModelVersions, SWT.NONE);
        colCreatedBy.setText("Author");
        colCreatedBy.setWidth(150);

        this.lblModelName = new Label(this.grpModelVersions, SWT.NONE);
        this.lblModelName.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModelName.setText("Model name:");
        fd = new FormData();
        fd.top = new FormAttachment(this.tblModelVersions, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        this.lblModelName.setLayoutData(fd);

        this.txtModelName = new Text(this.grpModelVersions, SWT.BORDER);
        this.txtModelName.setBackground(GROUP_BACKGROUND_COLOR);
        this.txtModelName.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblModelName);
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        this.txtModelName.setLayoutData(fd);

        this.lblPurpose = new Label(this.grpModelVersions, SWT.NONE);
        this.lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblPurpose.setText("Purpose:");
        fd = new FormData();
        fd.top = new FormAttachment(this.txtModelName, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        this.lblPurpose.setLayoutData(fd);

        this.txtPurpose = new Text(this.grpModelVersions, SWT.BORDER);
        this.txtPurpose.setBackground(GROUP_BACKGROUND_COLOR);
        this.txtPurpose.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblPurpose);
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(80, -5);
        this.txtPurpose.setLayoutData(fd);

        this.lblReleaseNote = new Label(this.grpModelVersions, SWT.NONE);
        this.lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblReleaseNote.setText("Release note:");
        fd = new FormData();
        fd.top = new FormAttachment(this.txtPurpose, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        this.lblReleaseNote.setLayoutData(fd);

        this.txtReleaseNote = new Text(this.grpModelVersions, SWT.BORDER);
        this.txtReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
        this.txtReleaseNote.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.lblReleaseNote);
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(100, -getDefaultMargin());
        this.txtReleaseNote.setLayoutData(fd);
    }

    protected void createGrpActions() {
        Composite compoBottom = new Composite(this.compoRight, SWT.NONE);
        compoBottom.setBackground(COMPO_BACKGROUND_COLOR);
        FormData fd = new FormData();
        fd.top = new FormAttachment(this.compoRightBottom, 10);
        fd.bottom = new FormAttachment(100, -10);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        compoBottom.setLayoutData(fd);
        compoBottom.setLayout(new FormLayout());
        
        this.grpActions = new Group(compoBottom, SWT.SHADOW_ETCHED_IN);
        this.grpActions.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpActions.setFont(GROUP_TITLE_FONT);
        this.grpActions.setText("Actions: ");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpActions.setLayoutData(fd);
        
        RowLayout rowLayout = new RowLayout();
        rowLayout.justify = true;
        this.grpActions.setLayout(rowLayout);
		
		this.btnCheckStructure = new Button(this.grpActions, SWT.NONE);
		this.btnCheckStructure.setText("Check structure");
		this.btnCheckStructure.setToolTipText("Checks that the database has got the right structure.");
		this.btnCheckStructure.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { checkStructureCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		this.btnCheckContent = new Button(this.grpActions, SWT.NONE);
		this.btnCheckContent.setText("Check content");
		this.btnCheckContent.setToolTipText("Checks the database content and show up all the errors found.");
		this.btnCheckContent.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { checkContentCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		this.btnDeleteModel = new Button(this.grpActions, SWT.NONE);
		this.btnDeleteModel.setText("Delete model");
		this.btnDeleteModel.setToolTipText("Completely delete a whole model and all the components that are not shared with another model\n(shared components will be kept)\n\nBeware, components' versions my be recalculated.");
		this.btnDeleteModel.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { deleteModelCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		this.btnDeleteVersion = new Button(this.grpActions, SWT.NONE);
		this.btnDeleteVersion.setText("Delete version");
		this.btnDeleteVersion.setToolTipText("Delete the selected version of the model and all the components that are specific to this version.\n(shared components will be kept)\n\nBeware, components' versions my be recalculated.");
		this.btnDeleteVersion.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { deleteVersionCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
    }
	
	/**
	 * Called when the "check structure" button has been pressed
	 */
	void checkStructureCallback() {
		try {
			this.importConnection.checkDatabase(null);
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
			return;
		}
		
		String schema = this.importConnection.getDatabaseEntry().getSchema().equals("") ? null : this.importConnection.getDatabaseEntry().getSchema();
		String schemaPrefix = this.importConnection.getDatabaseEntry().getSchemaPrefix();
		
		boolean isCorrect = true;
		StringBuilder message = new StringBuilder();
		
		logger.debug("Getting database structure ...");
		
		try {
			DatabaseMetaData metadata = this.importConnection.getConnection().getMetaData();
			
			String[][] databaseVersionColumns = {	{"archi_plugin", this.importConnection.getOBJECTID_COLUMN(), null},
													{"version", this.importConnection.getINTEGER_COLUMN(), null}
			};
			
			String[][] modelsColumns = {			{"id", this.importConnection.getOBJECTID_COLUMN(), null},
													{"name", this.importConnection.getOBJ_NAME_COLUMN(), null},
													{"version", this.importConnection.getINTEGER_COLUMN(), null},
													{"note", this.importConnection.getTEXT_COLUMN(), null},
													{"purpose", this.importConnection.getTEXT_COLUMN(), null},
													{"created_by", this.importConnection.getUSERNAME_COLUMN(), null},
													{"created_on", this.importConnection.getDATETIME_COLUMN(), null},
													{"checkedin_by", this.importConnection.getUSERNAME_COLUMN(), null},
													{"checkedin_on", this.importConnection.getDATETIME_COLUMN(), null},
													{"deleted_by", this.importConnection.getUSERNAME_COLUMN(), null},
													{"deleted_on", this.importConnection.getDATETIME_COLUMN(), null},
													{"checksum", this.importConnection.getOBJECTID_COLUMN(), null},
			};

			Map<String, String[][]> tables = new HashMap<String, String[][]>();
			tables.put("database_version", databaseVersionColumns);
			tables.put("models", modelsColumns);
			
			for (Map.Entry<String, String[][]> entry : tables.entrySet()) {
				String tableName = entry.getKey();
				String[][] tableColumns = entry.getValue();
				
				try (ResultSet result = metadata.getColumns(null, schema, tableName, null)) {
					logger.debug("Table "+schemaPrefix+"database_version:");
					message.append("Table "+schemaPrefix+"database_version:");
					
					boolean checkColumnsResult = checkColumns(result, message, tableColumns);
					isCorrect &= checkColumnsResult;
					
					message.append("\n");			
				} catch (SQLException err) {
					DBGui.popup(Level.ERROR, "Failed to get table columns.", err);
					return;
				}
			}
		} catch (SQLException err) {
			DBGui.popup(Level.ERROR, "Failed to get database connection's metadata.", err);
			return;
		}
		
		if ( isCorrect ) {
			DBGui.popup(Level.INFO, "Database structure successfully checked.");
		} else {
			DBGui.popup(Level.WARN, message.toString());
		}
	}
	
	@SuppressWarnings("static-method")
	boolean checkColumns(ResultSet result, StringBuilder message, String[][] columns) throws SQLException {
		boolean isCorrect = true;
		
		while( result.next() ) {
			String columnName = result.getString("COLUMN_NAME").toLowerCase();
			String columnType = result.getString("TYPE_NAME").toLowerCase();
			
			boolean columnFound = false;

			// if the column name is known, we check its type
			for ( int i=0; i < columns.length; ++i ) {
				if ( columnName.equalsIgnoreCase(columns[i][0]) ) {
					columnFound = true;
					columns[i][2] = "found";
					if ( columnType.equalsIgnoreCase(columns[i][1]) )
						logger.debug("   Column "+columnName+" is "+columnType);
					else {
						logger.debug("   Column "+columnName+" is "+columnType+", should be "+columns[i][1].toLowerCase());
						message.append("\n   Column "+columnName+" is "+columnType+", should be "+columns[i][1].toLowerCase());
						isCorrect = false;
					}
						
				}
			}
			
			// if the column name is not known, we add an error
			if ( !columnFound ) {
				logger.debug("   Column "+columnName+" has been found but shoud not exist.");
				message.append("\n   Column "+columnName+" has been found but shoud not exist.");
				isCorrect = false;
			}
		}
		
		// we now check that all the columns have been found
		for ( int i=0; i < columns.length; ++i ) {
			if ( columns[i][2] == null ) {
				logger.debug("   Column "+columns[i][0]+" has not been found but shoud exist.");
				message.append("\n   Column "+columns[i][0]+" has not been found but shoud exist.");
				isCorrect = false;
			}
		}
		
		return isCorrect;
	}
	
	/**
	 * Called when the "check content" button has been pressed
	 */
	void checkContentCallback() {
		try {
			this.importConnection.checkDatabase(null);
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "Failed to check the database.", err);
			return;
		}
		
		// we remove duplicate rows in view_objects_in_views and view_connections_in views to fix bug of plugin version 2.2
	
		try {
			// we start a new transaction
			this.importConnection.setAutoCommit(false);
		} catch (SQLException err) {
			DBGui.popup(Level.ERROR, "Failed to start a new transaction.", err);
			return;
		}
		
		String schemaPrefix = this.importConnection.getDatabaseEntry().getSchemaPrefix();
		String duplicateObjectsStatus = "";
		String duplicateConnectionsStatus = "";
		
		logger.info("Removing view_objects_in_views duplicates ...");
		try {
			String table = schemaPrefix+"views_objects_in_view";
			String request = "DELETE FROM "+table+" WHERE EXISTS (SELECT * FROM "+table+" t2 WHERE "+table+".object_id = t2.object_id AND "+table+".object_version = t2.object_version AND "+table+".view_id = t2.view_id AND "+table+".view_version = t2.view_version AND "+table+".oiv_id > t2.oiv_id)";
			if ( logger.isTraceSQLEnabled() ) logger.trace("      --> "+request);
			
			int deletedRows = this.importConnection.executeRequest(request);
			switch (deletedRows) {
			case 0: duplicateObjectsStatus = "No duplicate row has been found in the \"views_objects_in_view\" table."; break;
			case 1: duplicateObjectsStatus = "1 duplicate row has been removed from the \"views_objects_in_view\" table."; break;
			default: duplicateObjectsStatus = deletedRows+" duplicate row have been removed from the \"views_objects_in_view\" table.";
			}
		} catch (SQLException err) {
			try {
				this.importConnection.rollback();
				this.importConnection.setAutoCommit(true);
			} catch (SQLException err2) {
				DBGui.popup(Level.ERROR, "Failed to remove view_objects_in_views duplicates.", err);
				DBGui.popup(Level.FATAL, "Failed to roll back the transaction. We suggest you close Archi and verify your database manually.", err2);
				return;
			}
			
			DBGui.popup(Level.ERROR, "Failed to remove view_objects_in_views duplicates. The transaction has been rolled back.", err);
			return;
		}
		
		logger.info("Removing view_connections_in_views duplicates ...");
		try {
			String table = schemaPrefix+"views_connections_in_view";
			String request = "DELETE FROM "+table+" WHERE EXISTS (SELECT * FROM "+table+" t2 WHERE "+table+".connection_id = t2.connection_id AND "+table+".connection_version = t2.connection_version AND "+table+".view_id = t2.view_id AND "+table+".view_version = t2.view_version AND "+table+".civ_id > t2.civ_id)";
			
			if ( logger.isTraceSQLEnabled() ) logger.trace("      --> "+request);

			int deletedRows = this.importConnection.executeRequest(request);
			switch (deletedRows) {
				case 0: duplicateConnectionsStatus = "No duplicate row has been found in the \"views_connections_in_view\" table."; break;
				case 1: duplicateConnectionsStatus = "1 duplicate row has been removed from the \"views_connections_in_view\" table."; break;
				default: duplicateConnectionsStatus = deletedRows+" duplicate row have been removed from the \"views_connections_in_view\" table.";
			}
		} catch (SQLException err) {
			try {
				this.importConnection.rollback();
				this.importConnection.setAutoCommit(true);
			} catch (SQLException err2) {
				DBGui.popup(Level.ERROR, "Failed to remove view_connections_in_views duplicates.", err);
				DBGui.popup(Level.FATAL, "Failed to roll back the transaction. We suggest you close Archi and verify your database manually.", err2);
				return;
			}
			
			DBGui.popup(Level.ERROR, "Failed to remove view_connections_in_views duplicates. The transaction has been rolled back.", err);
			return;
		}
		
		try {
			this.importConnection.commit();
			this.importConnection.setAutoCommit(true);
		} catch (SQLException err) {
			DBGui.popup(Level.FATAL, "Failed to commit the transaction. We suggest you close Archi and verify your database manually.", err);
			return;
		}
		
		DBGui.popup(Level.INFO, duplicateObjectsStatus+"\n"+duplicateConnectionsStatus+"\n\nDatabase content successfully checked.");
	}
	
	/**
	 * Called when the "delete model" button has been pressed
	 */
	@SuppressWarnings("static-method")
	void deleteModelCallback() {
		DBGui.popup(Level.INFO, "Not yet implemented.");
	}
	
	/**
	 * Called when the "delete version" button has been pressed
	 */
	@SuppressWarnings("static-method")
	void deleteVersionCallback() {
		DBGui.popup(Level.INFO, "Not yet implemented.");
	}
}
