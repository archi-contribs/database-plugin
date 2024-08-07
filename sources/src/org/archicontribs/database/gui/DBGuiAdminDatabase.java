/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.gui;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;
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

/**
 * This class manages the GUI for the database administration procedures
 * 
 * @author Herve Jouin
 */
public class DBGuiAdminDatabase extends DBGui {
    @SuppressWarnings("hiding")
	protected static final DBLogger logger = new DBLogger(DBGuiAdminDatabase.class);

    DBDatabaseImportConnection importConnection;
    
    Table tblModels;
    Table tblModelVersions;
    Text txtFilterModels;

    Text txtModelName;
    Text txtPurpose;
    Text txtReleaseNote;
    
    Button btnCheckStructure;
    Button btnCheckContent;
    Button btnDeleteModel;
    //Button btnDeleteVersion;
    

    /**
     * Creates the GUI to import a model
     * @param databaseImportconnection 
     * @param entries 
     * @param title Title of the window
     */
    public DBGuiAdminDatabase(DBDatabaseImportConnection databaseImportconnection, List<DBDatabaseEntry> entries, String title) {
        super(title);
        
        this.importConnection = databaseImportconnection;

        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for administering the \""+databaseImportconnection.getDatabaseEntry().getName()+"\" database (plugin version "+DBPlugin.PLUGIN_VERSION.toString()+").");

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
        this.comboDatabaseEntries = entries;
        int index = 0;
        for (DBDatabaseEntry databaseEntry: this.comboDatabaseEntries) {
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
     * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.
     * @param ignore (unused)
     */
    @SuppressWarnings("resource")
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
        	this.btnDeleteModel.setEnabled(true);
            this.tblModels.setSelection(0);
            this.tblModels.notifyListeners(SWT.Selection, new Event());      // calls database.getModelVersions()
        } else
        	this.btnDeleteModel.setEnabled(false);
    }


    protected void createGrpModel() {
        Group grpModels = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
        grpModels.setBackground(GROUP_BACKGROUND_COLOR);
        grpModels.setFont(GROUP_TITLE_FONT);
        grpModels.setText("Models: ");
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(50, -5);
        fd.bottom = new FormAttachment(100);
        grpModels.setLayoutData(fd);
        grpModels.setLayout(new FormLayout());

        Label lblListModels = new Label(grpModels, SWT.NONE);
        lblListModels.setBackground(GROUP_BACKGROUND_COLOR);
        lblListModels.setText("Filter:");
        fd = new FormData();
        fd.top = new FormAttachment(0, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        lblListModels.setLayoutData(fd);

        this.txtFilterModels = new Text(grpModels, SWT.BORDER);
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
                    DBGuiUtils.popup(Level.ERROR, "Failed to get the list of models in the database.", err);
                } 
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblListModels, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblListModels, 5);
        fd.right = new FormAttachment(100, -getDefaultMargin());
        this.txtFilterModels.setLayoutData(fd);



        this.tblModels = new Table(grpModels, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
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
                    DBGuiUtils.popup(Level.ERROR, "Failed to get model's versions from the database", err);
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

        Group grpModelVersions = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
        grpModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
        grpModelVersions.setFont(GROUP_TITLE_FONT);
        grpModelVersions.setText("Versions of selected model: ");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(50, 5);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        grpModelVersions.setLayoutData(fd);
        grpModelVersions.setLayout(new FormLayout());

        this.tblModelVersions = new Table(grpModelVersions,  SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
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

        Label lblModelName = new Label(grpModelVersions, SWT.NONE);
        lblModelName.setBackground(GROUP_BACKGROUND_COLOR);
        lblModelName.setText("Model name:");
        fd = new FormData();
        fd.top = new FormAttachment(this.tblModelVersions, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        lblModelName.setLayoutData(fd);

        this.txtModelName = new Text(grpModelVersions, SWT.BORDER);
        this.txtModelName.setBackground(GROUP_BACKGROUND_COLOR);
        this.txtModelName.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(lblModelName);
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        this.txtModelName.setLayoutData(fd);

        Label lblPurpose = new Label(grpModelVersions, SWT.NONE);
        lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
        lblPurpose.setText("Purpose:");
        fd = new FormData();
        fd.top = new FormAttachment(this.txtModelName, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        lblPurpose.setLayoutData(fd);

        this.txtPurpose = new Text(grpModelVersions, SWT.BORDER);
        this.txtPurpose.setBackground(GROUP_BACKGROUND_COLOR);
        this.txtPurpose.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(lblPurpose);
        fd.left = new FormAttachment(0, getDefaultMargin());
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(80, -5);
        this.txtPurpose.setLayoutData(fd);

        Label lblReleaseNote = new Label(grpModelVersions, SWT.NONE);
        lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
        lblReleaseNote.setText("Release note:");
        fd = new FormData();
        fd.top = new FormAttachment(this.txtPurpose, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        lblReleaseNote.setLayoutData(fd);

        this.txtReleaseNote = new Text(grpModelVersions, SWT.BORDER);
        this.txtReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
        this.txtReleaseNote.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(lblReleaseNote);
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
        
        Group grpActions = new Group(compoBottom, SWT.SHADOW_ETCHED_IN);
        grpActions.setBackground(GROUP_BACKGROUND_COLOR);
        grpActions.setFont(GROUP_TITLE_FONT);
        grpActions.setText("Actions: ");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        grpActions.setLayoutData(fd);
        
        RowLayout rowLayout = new RowLayout();
        rowLayout.justify = true;
        grpActions.setLayout(rowLayout);
		
		this.btnCheckStructure = new Button(grpActions, SWT.NONE);
		this.btnCheckStructure.setText("Check structure");
		this.btnCheckStructure.setToolTipText("Checks that the database has got the right structure.");
		this.btnCheckStructure.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { checkStructureCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		this.btnCheckContent = new Button(grpActions, SWT.NONE);
		this.btnCheckContent.setText("Check content");
		this.btnCheckContent.setToolTipText("Checks the database content and show up all the errors found.");
		this.btnCheckContent.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { checkContentCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		this.btnDeleteModel = new Button(grpActions, SWT.NONE);
		this.btnDeleteModel.setText("Delete model");
		this.btnDeleteModel.setToolTipText("Completely delete a whole model and all the components that are not shared with another model\n(shared components will be kept)\n\nBeware, components versions my be recalculated.");
		this.btnDeleteModel.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) { deleteModelCallback(); }
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		//this.btnDeleteVersion = new Button(this.grpActions, SWT.NONE);
		//this.btnDeleteVersion.setText("Delete version");
		//this.btnDeleteVersion.setToolTipText("Delete the selected version of the model and all the components that are specific to this version.\n(shared components will be kept)\n\nBeware, components versions my be recalculated.");
		//this.btnDeleteVersion.addSelectionListener(new SelectionListener() {
		//	@Override
        //    public void widgetSelected(SelectionEvent e) { deleteVersionCallback(); }
		//	@Override
        //    public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		//});
    }
	
	/**
	 * Called when the "check structure" button has been pressed
	 */
	void checkStructureCallback() {
		try {
			this.importConnection.checkDatabaseStructure(this);
		} catch (@SuppressWarnings("unused") Exception ign) {
			// messages are shown in the checkDatabase method
		}
	}
	
	
	/**
	 * Called when the "check content" button has been pressed
	 */
	void checkContentCallback() {
		// we remove duplicate rows in view_objects_in_views and view_connections_in views to fix bug of plugin version 2.2
	
		try {
			// we start a new transaction
			this.importConnection.setAutoCommit(false);
		} catch (SQLException err) {
			DBGuiUtils.popup(Level.ERROR, "Failed to start a new transaction.", err);
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
			case 0: duplicateObjectsStatus = "No duplicate row found in the \"views_objects_in_view\" table."; break;
			case 1: duplicateObjectsStatus = "1 duplicate row removed from the \"views_objects_in_view\" table."; break;
			default: duplicateObjectsStatus = deletedRows+" duplicate rows removed from the \"views_objects_in_view\" table.";
			}
		} catch (SQLException err) {
			try {
				this.importConnection.rollback();
				this.importConnection.setAutoCommit(true);
			} catch (SQLException err2) {
				DBGuiUtils.popup(Level.ERROR, "Failed to remove view_objects_in_views duplicates.", err);
				DBGuiUtils.popup(Level.FATAL, "Failed to roll back the transaction. We suggest you close Archi and verify your database manually.", err2);
				return;
			}
			
			DBGuiUtils.popup(Level.ERROR, "Failed to remove view_objects_in_views duplicates. The transaction has been rolled back.", err);
			return;
		}
		
		logger.info("Removing view_connections_in_views duplicates ...");
		try {
			String table = schemaPrefix+"views_connections_in_view";
			String request = "DELETE FROM "+table+" WHERE EXISTS (SELECT * FROM "+table+" t2 WHERE "+table+".connection_id = t2.connection_id AND "+table+".connection_version = t2.connection_version AND "+table+".view_id = t2.view_id AND "+table+".view_version = t2.view_version AND "+table+".civ_id > t2.civ_id)";
			
			if ( logger.isTraceSQLEnabled() ) logger.trace("      --> "+request);

			int deletedRows = this.importConnection.executeRequest(request);
			switch (deletedRows) {
				case 0: duplicateConnectionsStatus = "No duplicate row found in the \"views_connections_in_view\" table."; break;
				case 1: duplicateConnectionsStatus = "1 duplicate row removed from the \"views_connections_in_view\" table."; break;
				default: duplicateConnectionsStatus = deletedRows+" duplicate rows removed from the \"views_connections_in_view\" table.";
			}
		} catch (SQLException err) {
			try {
				this.importConnection.rollback();
				this.importConnection.setAutoCommit(true);
			} catch (SQLException err2) {
				DBGuiUtils.popup(Level.ERROR, "Failed to remove view_connections_in_views duplicates.", err);
				DBGuiUtils.popup(Level.FATAL, "Failed to roll back the transaction. We suggest you close Archi and verify your database manually.", err2);
				return;
			}
			
			DBGuiUtils.popup(Level.ERROR, "Failed to remove view_connections_in_views duplicates. The transaction has been rolled back.", err);
			return;
		}
		
		try {
			this.importConnection.commit();
			this.importConnection.setAutoCommit(true);
		} catch (SQLException err) {
			DBGuiUtils.popup(Level.FATAL, "Failed to commit the transaction. We suggest you close Archi and verify your database manually.", err);
			return;
		}
		
		DBGuiUtils.popup(Level.INFO, duplicateObjectsStatus+"\n"+duplicateConnectionsStatus+"\n\nDatabase content successfully checked.");
	}
	
	/**
	 * Called when the "delete model" button has been pressed
	 */
	void deleteModelCallback() {
		String modelId = (String) DBGuiAdminDatabase.this.tblModels.getSelection()[0].getData("id");
		String modelName = DBGuiAdminDatabase.this.tblModels.getSelection()[0].getText();
		
		if ( modelId == null ) {
			DBGuiUtils.popup(Level.ERROR, "Failed to get model ID.");
			return;
		}
		
		if (DBGuiUtils.question("You are about to delete the model \""+modelName+"\" from the database.\n\nThis will delete the model as a container. However, the model content (elements, relationships, views, ...) will remain in the database so they can be imported from the database into other models.\n\nPlease note that this action cannot be undone.\n\nDo you confirm the deletion ?") ) {
			try {
				int deletedRows = this.importConnection.executeRequest("DELETE FROM "+this.importConnection.getDatabaseEntry().getSchemaPrefix()+"models WHERE id = ?", modelId);
				
				if (deletedRows == 0)
					DBGuiUtils.popup(Level.WARN,"That's weird, no model with ID \""+modelId+"\" has been found in the database.");
				else {
					connectedToDatabase(false);
					DBGuiUtils.popup(Level.INFO, String.valueOf(deletedRows)+" version"+((deletedRows == 1)?"":"s")+" of the model \""+modelName+"\" "+((deletedRows == 1)?"has":"have")+" been deleted from the database.");
				}
			} catch (SQLException err) {
				try {
					this.importConnection.rollback();
					this.importConnection.setAutoCommit(true);
				} catch (SQLException err2) {
					DBGuiUtils.popup(Level.ERROR, "Failed to delete model from the database.", err);
					DBGuiUtils.popup(Level.FATAL, "Failed to roll back the transaction. We suggest you close Archi and verify your database manually.", err2);
					return;
				}
				DBGuiUtils.popup(Level.ERROR, "Failed to delete model from the database. The transaction has been rolled back.", err);
			}
			
		} else
			DBGuiUtils.popup(Level.INFO, "Delete canceled by user.");
	}
	
	///**
	// * Called when the "delete version" button has been pressed
	// */
	//@SuppressWarnings("static-method")
	//void deleteVersionCallback() {
	//	DBGuiUtils.popup(Level.INFO, "Not yet implemented.");
	//}
}
