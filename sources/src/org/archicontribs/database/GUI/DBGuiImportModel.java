/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.ui.services.EditorManager;
import com.archimatetool.editor.ui.services.ViewManager;
import com.archimatetool.editor.views.tree.ITreeModelView;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IFolder;

import lombok.Getter;

/**
 * This class manages the GUI that allows to import a model from a database
 * 
 * @author Herve Jouin
 */
public class DBGuiImportModel extends DBGui {
    @SuppressWarnings("hiding")
	protected static final DBLogger logger = new DBLogger(DBGuiImportModel.class);

    private @Getter DBArchimateModel modelToImport = null;
    
    DBDatabaseImportConnection importConnection;
    
    @Getter Table tblModels;
    @Getter Table tblModelVersions;
    @Getter Text txtFilterModels;

    private Group grpModels;
    private Group grpModelVersions;
    private Group grpComponents;
    
    private Label lblModelName;
    Text txtModelName;
    private Label lblPurpose;
    Text txtPurpose;
    private Label lblReleaseNote;
    Text txtReleaseNote;

    private Text txtTotalModelItself;
    private Text txtTotalProfiles;
    private Text txtTotalElements;
    private Text txtTotalRelationships;
    private Text txtTotalFolders;
    private Text txtTotalViews;
    private Text txtTotalViewObjects;
    private Text txtTotalViewConnections;
    private Text txtTotalImages;

    private Text txtImportedModelItself;
    private Text txtImportedProfiles;
    private Text txtImportedElements;
    private Text txtImportedRelationships;
    private Text txtImportedFolders;
    private Text txtImportedViews;
    private Text txtImportedViewObjects;
    private Text txtImportedViewConnections;
    private Text txtImportedImages;
    
    private boolean showRealTimeNumbers = DBPlugin.INSTANCE.getPreferenceStore().getBoolean("showRealTimeNumbers");
    
    /**
     * Creates the GUI to import a model
     * @param title Title of the window
     * @throws Exception 
     */
    public DBGuiImportModel(String title) throws Exception {
    	this(title, null);
    }

    /**
     * Creates the GUI to import a model
     * @param title Title of the window
     * @param defaultDatabaseName Name of the default database name to connect to
     * @throws Exception 
     */
    public DBGuiImportModel(String title, String defaultDatabaseName) throws Exception {
        super(title);
        
        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for importing a model (plugin version "+DBPlugin.pluginVersion.toString()+").");

        createAction(ACTION.One, "1 - Choose model");
        createAction(ACTION.Two, "2 - Import model");
        createAction(ACTION.Three, "3 - Status");
        setActiveAction(ACTION.One);

        // We activate the btnDoAction button: if the user select the "Import" button --> call the doImport() method
        setBtnAction("Import", new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String modelName = DBGuiImportModel.this.tblModels.getSelection()[0].getText();
                String modelId = (String)DBGuiImportModel.this.tblModels.getSelection()[0].getData("id");
                boolean checkName = true;

                // we check that the model is not already in memory
                List<IArchimateModel> allModels = IEditorModelManager.INSTANCE.getModels();
                for ( IArchimateModel existingModel: allModels ) {
                    if ( DBPlugin.areEqual(modelId, existingModel.getId()) ) {
                        DBGuiUtils.popup(Level.ERROR, "A model with ID \""+modelId+"\" already exists. Cannot import it again ...");
                        return;
                    }
                    if ( checkName && DBPlugin.areEqual(modelName, existingModel.getName()) ) {
                        if ( !DBGuiUtils.question("A model with name \""+modelName+"\" already exists.\n\nIt is possible to have two models with the same name as long as they've got distinct IDs but it is not recommended.\n\nDo you wish to force the import ?") ) {
                            return;
                        }
                        checkName = false;	// if a third model has got the same name, we do not ask again.
                    }
                }

                setActiveAction(STATUS.Ok);
                DBGuiImportModel.this.btnDoAction.setEnabled(false);
                doImport();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
        });

        // We rename the "close" button to "cancel"
        this.btnClose.setText("Cancel");

        // We activate the Eclipse Help framework
        setHelpHref("importModel.html");

        createGrpModel();
        createGrpComponents();

        // We connect to the database and call the databaseSelected() method
        this.includeNeo4j = false;
        getDatabases(false, null, defaultDatabaseName);
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
                DBGuiImportModel.this.tblModels.removeAll();
                DBGuiImportModel.this.tblModelVersions.removeAll();
                try {
                    for (Hashtable<String, Object> model : DBGuiImportModel.this.importConnection.getModels("%"+DBGuiImportModel.this.txtFilterModels.getText()+"%")) {
                        TableItem tableItem = new TableItem(DBGuiImportModel.this.tblModels, SWT.BORDER);
                        tableItem.setText(0, (String)model.get("name"));
                        tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy").format(((Date)model.get("created_on"))));
                        tableItem.setData("id", model.get("id"));
                        tableItem.setData("date", model.get("created_on"));
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



        this.tblModels = new Table(this.grpModels, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        this.tblModels.setLinesVisible(true);
        this.tblModels.setHeaderVisible(true);
        this.tblModels.setBackground(TABLE_BACKGROUND_COLOR);
        this.tblModels.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
               	DBGuiImportModel.this.tblModelVersions.removeAll();
                	
            	try {
                    for (Hashtable<String, Object> version : DBGuiImportModel.this.importConnection.getModelVersions((String) DBGuiImportModel.this.tblModels.getSelection()[0].getData("id")) ) {
                    	if ( DBGuiImportModel.this.tblModelVersions.getItemCount() == 0 ) {
	                    	// if the first line, then we add the "latest version"
	        				TableItem tableItem = new TableItem(DBGuiImportModel.this.tblModelVersions, SWT.NULL);
	        				tableItem.setText(1, "(latest version)");
	        				tableItem.setData("name", version.get("name"));
	        				tableItem.setData("note", version.get("note"));
	        				tableItem.setData("purpose", version.get("purpose"));
        				}
        				
                    	TableItem tableItem = new TableItem(DBGuiImportModel.this.tblModelVersions, SWT.NULL);
            			tableItem.setText(0, (String)version.get("version"));
            			tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy").format((Timestamp)version.get("created_on")));
            			tableItem.setText(2, (String)version.get("created_by"));
            			tableItem.setData("name", version.get("name"));
            			tableItem.setData("note", version.get("note"));
            			tableItem.setData("purpose", version.get("purpose"));
                    }
                } catch (Exception err) {
                    DBGuiUtils.popup(Level.ERROR, "Failed to get model's versions from the database", err);
                }
            	
	    		if ( DBGuiImportModel.this.tblModelVersions.getItemCount() != 0 ) {
	    			DBGuiImportModel.this.tblModelVersions.setSelection(0);
	    			DBGuiImportModel.this.tblModelVersions.notifyListeners(SWT.Selection, new Event());       // calls database.getModelVersions()
	    		}
            }
        });
        this.tblModels.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if ( DBGuiImportModel.this.btnDoAction.getEnabled() )
                    DBGuiImportModel.this.btnDoAction.notifyListeners(SWT.Selection, new Event());
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
        colModelName.setWidth(250);
        colModelName.setData("sortDirection", 1);
        colModelName.addListener(SWT.Selection, new Listener() {
        	@Override
        	public void handleEvent(Event e) {
        		// sort column 1
        		TableItem[] items = DBGuiImportModel.this.tblModels.getItems();
        		int sortOrder = (int)colModelName.getData("sortDirection");
        		colModelName.setData("sortDirection", -sortOrder);
        		Collator collator = Collator.getInstance(Locale.getDefault());
        		for (int i = 1; i < items.length; i++) {
        			String value1 = items[i].getText(0);
        			for (int j = 0; j < i; j++) {
        				String value2 = items[j].getText(0);
        				if (collator.compare(value1, value2)*sortOrder < 0) {
        					String col0 = items[i].getText(0);
        					String col1 = items[i].getText(1);
                            String id = (String)items[i].getData("id");
                            Date date = (Date)items[i].getData("date");
        					items[i].dispose();
        					TableItem newItem = new TableItem(DBGuiImportModel.this.tblModels, SWT.NONE, j);
        					newItem.setText(0, col0);
        					newItem.setText(1, col1);
        					newItem.setData("id", id);
        					newItem.setData("date", date);
        					items = DBGuiImportModel.this.tblModels.getItems();
        					break;
        				}
        			}
        		}
        	}
        });
        
        TableColumn colModelDate = new TableColumn(this.tblModels, SWT.NONE);
        colModelDate.setText("Date");
        colModelDate.setWidth(120);
        colModelDate.setData("sortDirection", 1);
        colModelDate.addListener(SWT.Selection, new Listener() {
        	@Override
        	public void handleEvent(Event e) {
        		// sort column 1
        		TableItem[] items = DBGuiImportModel.this.tblModels.getItems();
        		int sortOrder = (int)colModelDate.getData("sortDirection");
        		colModelDate.setData("sortDirection", -sortOrder);
        		for (int i = 1; i < items.length; i++) {
        			Date date1 = (Date)items[i].getData("date");
        			for (int j = 0; j < i; j++) {
        				Date date2 = (Date)items[j].getData("date");
        				if (date1.compareTo(date2)*sortOrder < 0) {
        					String col0 = items[i].getText(0);
        					String col1 = items[i].getText(1);
                            String id = (String)items[i].getData("id");
                            Date date = (Date)items[i].getData("date");
        					items[i].dispose();
        					TableItem newItem = new TableItem(DBGuiImportModel.this.tblModels, SWT.NONE, j);
        					newItem.setText(0, col0);
        					newItem.setText(1, col1);
        					newItem.setData("id", id);
        					newItem.setData("date", date);
        					items = DBGuiImportModel.this.tblModels.getItems();
        					break;
        				}
        			}
        		}
        	}
        });

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
                if ( (DBGuiImportModel.this.tblModelVersions.getSelection() != null) && (DBGuiImportModel.this.tblModelVersions.getSelection().length > 0) && (DBGuiImportModel.this.tblModelVersions.getSelection()[0] != null) ) {
                    DBGuiImportModel.this.txtReleaseNote.setText((String) DBGuiImportModel.this.tblModelVersions.getSelection()[0].getData("note"));
                    DBGuiImportModel.this.txtPurpose.setText((String) DBGuiImportModel.this.tblModelVersions.getSelection()[0].getData("purpose"));
                    DBGuiImportModel.this.txtModelName.setText((String) DBGuiImportModel.this.tblModelVersions.getSelection()[0].getData("name"));
                    DBGuiImportModel.this.btnDoAction.setEnabled(true);
                } else {
                    DBGuiImportModel.this.btnDoAction.setEnabled(false);
                }
            }
        });
        this.tblModelVersions.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if ( DBGuiImportModel.this.btnDoAction.getEnabled() )
                    DBGuiImportModel.this.btnDoAction.notifyListeners(SWT.Selection, new Event());
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
        colVersion.setWidth(50);

        TableColumn colCreatedOn = new TableColumn(this.tblModelVersions, SWT.NONE);
        colCreatedOn.setText("Date");
        colCreatedOn.setWidth(130);

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

    /**
     * Creates a group displaying details about the imported model's components
     */
    private void createGrpComponents() {
        this.grpComponents = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
        this.grpComponents.setVisible(false);
        this.grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpComponents.setFont(GROUP_TITLE_FONT);
        this.grpComponents.setText("Your model components: ");
        
        // we calculate the required height of the grpComponents group
        int requiredHeight = 11 * (getDefaultLabelHeight() + getDefaultMargin());
        
        FormData fd = new FormData();
        fd.top = new FormAttachment(100, -requiredHeight);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpComponents.setLayoutData(fd);
        this.grpComponents.setLayout(new FormLayout());
        
        Label lblModelItself = new Label(this.grpComponents, SWT.NONE);
        lblModelItself.setBackground(GROUP_BACKGROUND_COLOR);
        lblModelItself.setText("Model itself:");
        fd = new FormData();
        fd.top = new FormAttachment(0, 25);
        fd.left = new FormAttachment(0, 30);
        lblModelItself.setLayoutData(fd);
        
        Label lblProfiles = new Label(this.grpComponents, SWT.NONE);
        lblProfiles.setBackground(GROUP_BACKGROUND_COLOR);
        lblProfiles.setText("Specializations:");
        fd = new FormData();
        fd.top = new FormAttachment(lblModelItself, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblProfiles.setLayoutData(fd);

        Label lblElements = new Label(this.grpComponents, SWT.NONE);
        lblElements.setBackground(GROUP_BACKGROUND_COLOR);
        lblElements.setText("Elements:");
        fd = new FormData();
        fd.top = new FormAttachment(lblProfiles, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblElements.setLayoutData(fd);

        Label lblRelationships = new Label(this.grpComponents, SWT.NONE);
        lblRelationships.setBackground(GROUP_BACKGROUND_COLOR);
        lblRelationships.setText("Relationships:");
        fd = new FormData();
        fd.top = new FormAttachment(lblElements, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblRelationships.setLayoutData(fd);

        Label lblFolders = new Label(this.grpComponents, SWT.NONE);
        lblFolders.setBackground(GROUP_BACKGROUND_COLOR);
        lblFolders.setText("Folders:");
        fd = new FormData();
        fd.top = new FormAttachment(lblRelationships, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblFolders.setLayoutData(fd);

        Label lblViews = new Label(this.grpComponents, SWT.NONE);
        lblViews.setBackground(GROUP_BACKGROUND_COLOR);
        lblViews.setText("Views:");
        fd = new FormData();
        fd.top = new FormAttachment(lblFolders, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblViews.setLayoutData(fd);

        Label lblViewObjects = new Label(this.grpComponents, SWT.NONE);
        lblViewObjects.setBackground(GROUP_BACKGROUND_COLOR);
        lblViewObjects.setText("Objects:");
        fd = new FormData();
        fd.top = new FormAttachment(lblViews, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblViewObjects.setLayoutData(fd);

        Label lblViewConnections = new Label(this.grpComponents, SWT.NONE);
        lblViewConnections.setBackground(GROUP_BACKGROUND_COLOR);
        lblViewConnections.setText("Connections:");
        fd = new FormData();
        fd.top = new FormAttachment(lblViewObjects, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblViewConnections.setLayoutData(fd);

        Label lblImages = new Label(this.grpComponents, SWT.NONE);
        lblImages.setBackground(GROUP_BACKGROUND_COLOR);
        lblImages.setText("Images:");
        fd = new FormData();
        fd.top = new FormAttachment(lblViewConnections, getDefaultMargin());
        fd.left = new FormAttachment(0, 30);
        lblImages.setLayoutData(fd);

        /* * * * * */

        Label lblTotal = new Label(this.grpComponents, SWT.CENTER);
        lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
        lblTotal.setText("Total");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(20, getDefaultMargin());
        fd.right = new FormAttachment(40, -getDefaultMargin());
        lblTotal.setLayoutData(fd);

        Label lblImported = new Label(this.grpComponents, SWT.CENTER);
        lblImported.setBackground(GROUP_BACKGROUND_COLOR);
        lblImported.setText("Imported");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(40, getDefaultMargin());
        fd.right = new FormAttachment(60, -getDefaultMargin());
        lblImported.setLayoutData(fd);

        /* * * * * */

        this.txtTotalModelItself = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalModelItself.setEditable(false);
        this.txtTotalModelItself.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalModelItself.setLayoutData(fd);
        
        this.txtImportedModelItself = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedModelItself.setEditable(false);
        this.txtImportedModelItself.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedModelItself.setLayoutData(fd);
        
        this.txtTotalProfiles = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalProfiles.setEditable(false);
        this.txtTotalProfiles.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalProfiles.setLayoutData(fd);
        
        this.txtImportedProfiles = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedProfiles.setEditable(false);
        this.txtImportedProfiles.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedProfiles.setLayoutData(fd);
        
        this.txtTotalElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalElements.setEditable(false);
        this.txtTotalElements.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalElements.setLayoutData(fd);

        this.txtImportedElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedElements.setEditable(false);
        this.txtImportedElements.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedElements.setLayoutData(fd);

        this.txtTotalRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalRelationships.setEditable(false);
        this.txtTotalRelationships.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalRelationships.setLayoutData(fd);

        this.txtImportedRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedRelationships.setEditable(false);
        this.txtImportedRelationships.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedRelationships.setLayoutData(fd);

        this.txtTotalFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalFolders.setEditable(false);
        this.txtTotalFolders.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalFolders.setLayoutData(fd);

        this.txtImportedFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedFolders.setEditable(false);
        this.txtImportedFolders.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedFolders.setLayoutData(fd);

        this.txtTotalViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViews.setEditable(false);
        this.txtTotalViews.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalViews.setLayoutData(fd);

        this.txtImportedViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedViews.setEditable(false);
        this.txtImportedViews.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedViews.setLayoutData(fd);

        this.txtTotalViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViewObjects.setEditable(false);
        this.txtTotalViewObjects.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalViewObjects.setLayoutData(fd);

        this.txtImportedViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedViewObjects.setEditable(false);
        this.txtImportedViewObjects.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedViewObjects.setLayoutData(fd);

        this.txtTotalViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViewConnections.setEditable(false);
        this.txtTotalViewConnections.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalViewConnections.setLayoutData(fd);

        this.txtImportedViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedViewConnections.setEditable(false);
        this.txtImportedViewConnections.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedViewConnections.setLayoutData(fd);

        this.txtTotalImages = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalImages.setEditable(false);
        this.txtTotalImages.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        this.txtTotalImages.setLayoutData(fd);

        this.txtImportedImages = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtImportedImages.setEditable(false);
        this.txtImportedImages.setEnabled(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        this.txtImportedImages.setLayoutData(fd);
    }

    /**
     * Called when the user clicks on the "import" button 
     */
    public void doImport() {
        String modelName = this.tblModels.getSelection()[0].getText();
        String modelId = (String)this.tblModels.getSelection()[0].getData("id");
        
        hideGrpDatabase();
        createProgressBar("Importing model \""+modelName+"\"", 0, 100);


        this.grpModels.setVisible(false);
        this.grpComponents.setVisible(true);
        
        // we reorganize the grpModelVersion widget
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(this.grpComponents, -getDefaultMargin());
        this.grpModelVersions.setLayoutData(fd);
        this.grpModelVersions.setText("Your model details: ");
        
        this.tblModelVersions.setVisible(false);
        
        Label lblModelVersion = new Label(this.grpModelVersions, SWT.NONE);
        
        fd = new FormData();
        fd.top = new FormAttachment(0, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        this.lblModelName.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(this.lblModelName, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelVersion, getDefaultMargin(), SWT.RIGHT);
        fd.right = new FormAttachment(100, -getDefaultMargin());
        this.txtModelName.setLayoutData(fd);
        
        lblModelVersion.setBackground(GROUP_BACKGROUND_COLOR);
        lblModelVersion.setText("Model version:");
        fd = new FormData();
        fd.top = new FormAttachment(this.lblModelName, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        lblModelVersion.setLayoutData(fd);

        Text txtModelVersion = new Text(this.grpModelVersions, SWT.BORDER);
        txtModelVersion.setBackground(GROUP_BACKGROUND_COLOR);
        txtModelVersion.setEnabled(false);
        TableItem selectedVersion = this.tblModelVersions.getSelection()[0];
        if ( selectedVersion.getText(0).isEmpty() )
        	txtModelVersion.setText("Latest version");
        else
        	txtModelVersion.setText(selectedVersion.getText(0)+" ("+selectedVersion.getText(1)+")");
        fd = new FormData();
        fd.top = new FormAttachment(lblModelVersion, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelVersion, getDefaultMargin(), SWT.RIGHT);
        fd.right = new FormAttachment(100, -getDefaultMargin());
        txtModelVersion.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(txtModelVersion, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        this.lblPurpose.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(txtModelVersion, 5);
        fd.left = new FormAttachment(lblModelVersion, getDefaultMargin(), SWT.RIGHT);
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(55, -5);
        this.txtPurpose.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(this.txtPurpose, getDefaultMargin());
        fd.left = new FormAttachment(0, getDefaultMargin());
        this.lblReleaseNote.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(this.txtPurpose, 5);
        fd.left = new FormAttachment(lblModelVersion, getDefaultMargin(), SWT.RIGHT);
        fd.right = new FormAttachment(100, -getDefaultMargin());
        fd.bottom = new FormAttachment(100, -getDefaultMargin());
        this.txtReleaseNote.setLayoutData(fd);
        
        this.compoRightBottom.layout();
        
        setActiveAction(ACTION.Two);
        
        // we create the model (but do not create standard folder as they will be imported from the database)
        this.modelToImport = (DBArchimateModel)DBArchimateFactory.eINSTANCE.createArchimateModel();
        this.modelToImport.setId(modelId);
        
        try {
        	// import properties, features and profiles from the database
        	this.importConnection.importModel(this.modelToImport);
        
	        // we get the selected model version to import
	        // if the value is empty, this means that the user selected the "Now" line, so we must load the latest version of the views
	        if ( !this.tblModelVersions.getSelection()[0].getText(0).isEmpty() ) {
	        	this.modelToImport.getInitialVersion().setVersion(Integer.valueOf(this.tblModelVersions.getSelection()[0].getText(0)));
	        	this.modelToImport.setLatestVersionImported(false);
	        } else {
	        	this.modelToImport.getInitialVersion().setVersion(Integer.valueOf(this.tblModelVersions.getItem(1).getText(0)));
	        	this.modelToImport.setLatestVersionImported(true);
	        }
	
	        // we add the new model in the manager
	        IEditorModelManager.INSTANCE.registerModel(this.modelToImport);
	
        	// count components to be imported from the database 
            int importSize = this.importConnection.countModelComponents(this.modelToImport);
            
            setProgressBarMinAndMax(0, importSize);

            this.txtTotalModelItself.setText("1");
            this.txtTotalProfiles.setText(toString(this.importConnection.getCountProfilesToImport()));
            this.txtTotalElements.setText(toString(this.importConnection.getCountElementsToImport()));
            this.txtTotalRelationships.setText(toString(this.importConnection.getCountRelationshipsToImport()));
            this.txtTotalFolders.setText(toString(this.importConnection.getCountFoldersToImport()));
            this.txtTotalViews.setText(toString(this.importConnection.getCountViewsToImport()));
            this.txtTotalViewObjects.setText(toString(this.importConnection.getCountViewObjectsToImport()));
            this.txtTotalViewConnections.setText(toString(this.importConnection.getCountViewConnectionsToImport()));
            this.txtTotalImages.setText(toString(this.importConnection.getCountImagesToImport()));

            this.txtImportedModelItself.setText("1");
            this.txtImportedProfiles.setText(toString(0));
            this.txtImportedElements.setText(toString(0));
            this.txtImportedRelationships.setText(toString(0));
            this.txtImportedFolders.setText(toString(0));
            this.txtImportedViews.setText(toString(0));
            this.txtImportedViewObjects.setText(toString(0));
            this.txtImportedViewConnections.setText(toString(0));
            this.txtImportedImages.setText(toString(0));
            
	        // Import the model components from the database

            logger.info("Importing specializations ...");
            int count = 0;
            this.importConnection.prepareImportProfiles(this.modelToImport);
            while ( this.importConnection.importProfiles(this.modelToImport) ) {
            	if ( this.showRealTimeNumbers ) {
            		this.txtImportedProfiles.setText(toString(this.importConnection.getCountProfilesImported()));
            		 increaseProgressBar();
            	}
            	++count;
            }
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedProfiles.setText(toString(count));
            	increaseProgressBar(count);
            }
            
            logger.info("Importing folders ...");
            count = 0;
            this.importConnection.prepareImportFolders(this.modelToImport);
            while ( this.importConnection.importFolders(this.modelToImport) ) {
            	if ( this.showRealTimeNumbers ) {
            		this.txtImportedFolders.setText(toString(this.importConnection.getCountFoldersImported()));
                    increaseProgressBar();
            	}
            	++count;
            }
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedFolders.setText(toString(count));
            	increaseProgressBar(count);
            }

            logger.info("Importing elements ...");
            count = 0;
            this.importConnection.prepareImportElements(this.modelToImport);
            while ( this.importConnection.importElements(this.modelToImport) ) {
            	if ( this.showRealTimeNumbers ) {
            		this.txtImportedElements.setText(toString(this.importConnection.getCountElementsImported()));
                	increaseProgressBar();
            	}
            	++count;
            }
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedElements.setText(toString(count));
            	increaseProgressBar(count);
            }

            logger.info("Importing relationships ...");
            count = 0;
            this.importConnection.prepareImportRelationships(this.modelToImport);
            while ( this.importConnection.importRelationships(this.modelToImport) ) {
            	if ( this.showRealTimeNumbers ) {
            		this.txtImportedRelationships.setText(toString(this.importConnection.getCountRelationshipsImported()));
                	increaseProgressBar();
            	}
            	++count;
            }
            this.modelToImport.resolveSourceAndTargetRelationships();
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedRelationships.setText(toString(count));
            	increaseProgressBar(count);
            }

            logger.info("Importing views ...");
            count = 0;
            this.importConnection.prepareImportViews(this.modelToImport);
            while ( this.importConnection.importViews(this.modelToImport) ) {
            	if ( this.showRealTimeNumbers ) {
            		this.txtImportedViews.setText(toString(this.importConnection.getCountViewsImported()));
                    increaseProgressBar();
            	}
            	++count;
            }
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedViews.setText(toString(count));
            	increaseProgressBar(count);
            }

            logger.info("Importing view objects ...");
            count = 0;
            for (IDiagramModel view: this.modelToImport.getAllViews().values()) {
                this.importConnection.prepareImportViewsObjects(view.getId(), this.modelToImport.getDBMetadata(view).getInitialVersion().getVersion());
                while ( this.importConnection.importViewsObjects(this.modelToImport, view) ) {
                	if ( this.showRealTimeNumbers ) {
                		this.txtImportedViewObjects.setText(toString(this.importConnection.getCountViewObjectsImported()));
                        increaseProgressBar(); 
                	}
                	++count;
                }
                this.txtImportedViewObjects.setText(toString(this.importConnection.getCountViewObjectsImported()));
            }
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedViewObjects.setText(toString(count));
            	increaseProgressBar(count);
            }
            // we refresh the numbers of elements imported as some may have been imported as part of view objects
            this.txtImportedElements.setText(toString(this.importConnection.getCountElementsImported()));

            logger.info("Importing view connections ...");
            count = 0;
            for (IDiagramModel view: this.modelToImport.getAllViews().values()) {
                this.importConnection.prepareImportViewsConnections(view.getId(), this.modelToImport.getDBMetadata(view).getInitialVersion().getVersion());
                while ( this.importConnection.importViewsConnections(this.modelToImport) ) {
                	if ( this.showRealTimeNumbers ) {
                		this.txtImportedViewConnections.setText(toString(this.importConnection.getCountViewConnectionsImported()));
                        increaseProgressBar();
                	}
                	++count;
                }
            }
            this.modelToImport.resolveSourceAndTargetConnections();
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedViewConnections.setText(toString(count));
            	increaseProgressBar(count);
            }
            // we refresh the numbers of elements imported as some may have been imported as part of view objects
            this.txtImportedRelationships.setText(toString(this.importConnection.getCountRelationshipsImported()));

            closeMessage();

            logger.info("Importing images ...");
            count = 0;
            for (String path: this.importConnection.getAllImagePaths()) {
                this.importConnection.importImage(this.modelToImport, path);
                if ( this.showRealTimeNumbers ) {
                	this.txtImportedImages.setText(toString(this.importConnection.getCountImagesImported()));
                    increaseProgressBar();
                }
                ++count;
            }
            if ( !this.showRealTimeNumbers ) {
            	this.txtImportedImages.setText(toString(count));
            	increaseProgressBar(count);
            }
            
            // If the model contains a view called "default view", we open it.
            for ( IDiagramModel view: this.modelToImport.getDiagramModels() ) {
                if ( DBPlugin.areEqual(view.getName().toLowerCase(), "default view") ) {
                    setMessage("Opening default view");
                    EditorManager.openDiagramEditor(view);
                    closeMessage();
                    break;
                }
            }
        } catch (Exception err) {
        	closeMessage();
            if ( isClosedByUser() ) {
                // we close the partially imported model
                CommandStack stack = (CommandStack)this.modelToImport.getAdapter(CommandStack.class);
                stack.markSaveLocation();
                try {
                    IEditorModelManager.INSTANCE.closeModel(this.modelToImport);
                } catch (@SuppressWarnings("unused") IOException ign) {
                    // there is nothing we can do
                }
                DBGuiUtils.popup(Level.WARN, "The import has been cancelled.");
            } else {
                DBGuiUtils.popup(Level.ERROR, "Failed to import model from database.", err);
                setActiveAction(STATUS.Error);
                doShowResult(err);
            }
            return;
        }

        if ( (this.dialog != null) && !this.dialog.isDisposed() ) {
            setActiveAction(STATUS.Ok);
            doShowResult(null);
        }
        return;
    }

    protected void doShowResult(Exception err) {
        logger.debug("Showing result.");
        if ( this.grpProgressBar != null ) this.grpProgressBar.setVisible(false);

        setActiveAction(ACTION.Three);
        this.btnClose.setText("Close");
        
        Color statusColor = GREEN_COLOR;
        
		if ( logger.isDebugEnabled() ) {
			logger.debug("   "+this.importConnection.getCountProfilesImported()+"/"+this.importConnection.getCountProfilesToImport()+" specializations imported");
		    logger.debug("   "+this.importConnection.getCountElementsImported()+"/"+this.importConnection.getCountElementsToImport()+" elements imported");
		    logger.debug("   "+this.importConnection.getCountRelationshipsImported()+"/"+this.importConnection.getCountRelationshipsToImport()+" relationships imported");
		    logger.debug("   "+this.importConnection.getCountFoldersImported()+"/"+this.importConnection.getCountFoldersToImport()+" folders imported");
		    logger.debug("   "+this.importConnection.getCountViewsImported()+"/"+this.importConnection.getCountViewsToImport()+" views imported");
		    logger.debug("   "+this.importConnection.getCountViewObjectsImported()+"/"+this.importConnection.getCountViewObjectsToImport()+" views objects imported");
		    logger.debug("   "+this.importConnection.getCountViewConnectionsImported()+"/"+this.importConnection.getCountViewConnectionsToImport()+" views connections imported");
		    logger.debug("   "+this.importConnection.getCountImagesImported()+"/"+this.importConnection.getCountImagesToImport()+" images imported");
		}

		this.txtImportedProfiles.setForeground( (this.importConnection.getCountProfilesImported() == this.importConnection.getCountProfilesToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedElements.setForeground( (this.importConnection.getCountElementsImported() == this.importConnection.getCountElementsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedRelationships.setForeground( (this.importConnection.getCountRelationshipsImported() == this.importConnection.getCountRelationshipsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedFolders.setForeground( (this.importConnection.getCountFoldersImported() == this.importConnection.getCountFoldersToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedViews.setForeground( (this.importConnection.getCountViewsImported() == this.importConnection.getCountViewsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedViewObjects.setForeground( (this.importConnection.getCountViewObjectsImported() == this.importConnection.getCountViewObjectsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedViewConnections.setForeground( (this.importConnection.getCountViewConnectionsImported() == this.importConnection.getCountViewConnectionsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        this.txtImportedImages.setForeground( (this.importConnection.getCountImagesImported() == this.importConnection.getCountImagesToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );

        if ( err == null ) {
        	// if all the counters are equals to the expected values
        	if ( statusColor == GREEN_COLOR ) {
        	   	setMessage("*** Import successful ***", statusColor);
            	
        	   	// We open the Model in the Editor
            	IEditorModelManager.INSTANCE.openModel(this.modelToImport);
            	
            	try {
            		ITreeModelView treeModelView = (ITreeModelView)ViewManager.showViewPart(ITreeModelView.ID, true);
		            if(treeModelView != null) {
		                List<Object> elements;
		                
		                // we select the view folder in order to show the model folders in the tree
		                elements = new ArrayList<Object>();
		                IFolder viewsFolder = this.modelToImport.getFolder(FolderType.DIAGRAMS);
		                if ( viewsFolder != null ) {
		                    elements.add(viewsFolder);
		                    treeModelView.getViewer().setSelection(new StructuredSelection(elements), true);
		                }
		        
		                // We select back the model in the tree
		                elements.clear();
		                elements.add(this.modelToImport);
		                treeModelView.getViewer().setSelection(new StructuredSelection(elements), true);
		            }
            	} catch (@SuppressWarnings("unused") IllegalStateException ign) {
            		// nothing to do as the workbench is not created when the import is done with ACLI
            	}
            
	            if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("closeIfSuccessful") ) {
	                if ( logger.isDebugEnabled() ) logger.debug("Automatically closing the window as set in preferences");
	                close();
	                return;
	            }
        	} else {
        		// if some counters are different from the expected values
        		statusColor = RED_COLOR;
        		setMessage("No error has been raised during the import process,\nbut the count of imported components is not correct !", RED_COLOR);
        	}
        } else {
        	statusColor = RED_COLOR;
            setMessage("Error while importing model:\n"+err.getMessage(), RED_COLOR);
        }

        if ( statusColor == RED_COLOR ) {
        	if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("deleteIfImportError") ) {
	        	try {
	                // we remove the 'dirty' flag (i.e. we consider the model as saved) because we do not want the closeModel() method ask to save it
	                CommandStack stack = (CommandStack)this.modelToImport.getAdapter(CommandStack.class);
	                stack.markSaveLocation();
	    
	                IEditorModelManager.INSTANCE.closeModel(this.modelToImport);
	            } catch (IOException e) {
	                DBGuiUtils.popup(Level.FATAL, "Failed to close the model partially imported.\n\nWe suggest you close and restart Archi.", e);
	            }
	        } else {
	            DBGuiUtils.popup(Level.ERROR, "Please be warn that the model you just imported is not concistent.\n\nYou choosed to keep it in the preferences, but should you export it back to the database, you may loose data.\n\nDo it at your own risk !");
	        }
        }
    }
}
