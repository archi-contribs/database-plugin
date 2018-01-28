/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
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
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.ui.services.ViewManager;
import com.archimatetool.editor.views.tree.ITreeModelView;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;

public class DBGuiImportModel extends DBGui {
    protected static final DBLogger logger = new DBLogger(DBGuiImportModel.class);

    private ArchimateModel modelToImport;
    private Table tblModels;
    private Table tblModelVersions;
    private Text txtFilterModels;

    private Group grpModels;
    private Group grpModelVersions;
    private Group grpComponents;
    
    private Label lblModelName;
    private Text txtModelName;
    private Label lblPurpose;
    private Text txtPurpose;
    private Label lblReleaseNote;
    private Text txtReleaseNote;

    private Text txtTotalElements;
    private Text txtTotalRelationships;
    private Text txtTotalFolders;
    private Text txtTotalViews;
    private Text txtTotalViewObjects;
    private Text txtTotalViewConnections;
    private Text txtTotalImages;

    private Text txtImportedElements;
    private Text txtImportedRelationships;
    private Text txtImportedFolders;
    private Text txtImportedViews;
    private Text txtImportedViewObjects;
    private Text txtImportedViewConnections;
    private Text txtImportedImages;

    /**
     * Creates the GUI to import a model
     * @throws Exception 
     */
    public DBGuiImportModel(String title) throws Exception {
        super(title);
        
        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for importing a model (plugin version "+DBPlugin.pluginVersion+").");

        createAction(ACTION.One, "1 - Choose model");
        createAction(ACTION.Two, "2 - Import model");
        createAction(ACTION.Three, "3 - Status");
        setActiveAction(ACTION.One);

        // We activate the btnDoAction button : if the user select the "Import" button --> call the doImport() method
        setBtnAction("Import", new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                String modelName = tblModels.getSelection()[0].getText();
                String modelId = (String)tblModels.getSelection()[0].getData("id");
                boolean checkName = true;

                // we check that the model is not already in memory
                List<IArchimateModel> allModels = IEditorModelManager.INSTANCE.getModels();
                for ( IArchimateModel existingModel: allModels ) {
                    if ( DBPlugin.areEqual(modelId, existingModel.getId()) ) {
                        popup(Level.ERROR, "A model with ID \""+modelId+"\" already exists. Cannot import it again ...");
                        return;
                    }
                    if ( checkName && DBPlugin.areEqual(modelName, existingModel.getName()) ) {
                        if ( !question("A model with name \""+modelName+"\" already exists.\n\nIt is possible to have two models with the same name as long as they've got distinct IDs but it is not recommended.\n\nDo you wish to force the import ?") ) {
                            return;
                        }
                        checkName = false;	// if a third model has got the same name, we do not ask again.
                    }
                }

                setActiveAction(STATUS.Ok);
                btnDoAction.setEnabled(false);
                doImport();
            }
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
        });

        // We rename the "close" button to "cancel"
        btnClose.setText("Cancel");

        // We activate the Eclipse Help framework
        setHelpHref("importModel.html");

        createGrpModel();
        createGrpComponents();

        // We connect to the database and call the databaseSelected() method
        includeNeo4j = false;
        getDatabases();
    }

    protected void databaseSelectedCleanup() {
        if ( logger.isTraceEnabled() ) logger.trace("Removing all lignes in model table");
        if ( tblModels != null ) {
            tblModels.removeAll();
        }
        if ( tblModelVersions != null ) {
            tblModelVersions.removeAll();
        }
    }

    /**
     * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
     */
    @Override
    protected void connectedToDatabase(boolean ignore) {	
        compoRightBottom.setVisible(true);
        compoRightBottom.layout();
        try {
            connection.getModels(txtFilterModels.getText(), tblModels);
        } catch (Exception err) {
            DBGui.popup(Level.ERROR, "Failed to get the list of models in the database.", err);
        }
    }

    protected void createGrpModel() {
        grpModels = new Group(compoRightBottom, SWT.SHADOW_ETCHED_IN);
        grpModels.setBackground(GROUP_BACKGROUND_COLOR);
        grpModels.setFont(GROUP_TITLE_FONT);
        grpModels.setText("Models : ");
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(50, -5);
        fd.bottom = new FormAttachment(100);
        grpModels.setLayoutData(fd);
        grpModels.setLayout(new FormLayout());

        Label lblListModels = new Label(grpModels, SWT.NONE);
        lblListModels.setBackground(GROUP_BACKGROUND_COLOR);
        lblListModels.setText("Filter :");
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(0, 10);
        lblListModels.setLayoutData(fd);

        txtFilterModels = new Text(grpModels, SWT.BORDER);
        txtFilterModels.setToolTipText("You may use '%' as wildcard.");
        txtFilterModels.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                tblModels.removeAll();
                tblModelVersions.removeAll();
                try {
                    if ( connection.isConnected() )
                        connection.getModels("%"+txtFilterModels.getText()+"%", tblModels);
                } catch (Exception err) {
                    DBGui.popup(Level.ERROR, "Failed to get the list of models in the database.", err);
                } 
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblListModels, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblListModels, 5);
        fd.right = new FormAttachment(100, -10);
        txtFilterModels.setLayoutData(fd);



        tblModels = new Table(grpModels, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        tblModels.setLinesVisible(true);
        tblModels.setBackground(GROUP_BACKGROUND_COLOR);
        tblModels.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                try {
                    if ( (tblModels.getSelection() != null) && (tblModels.getSelection().length > 0) && (tblModels.getSelection()[0] != null) )
                        connection.getModelVersions((String) tblModels.getSelection()[0].getData("id"), tblModelVersions);
                    else
                        tblModelVersions.removeAll();
                } catch (Exception err) {
                    DBGui.popup(Level.ERROR, "Failed to get models from the database", err);
                } 
            }
        });
        tblModels.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event e) {
                if ( btnDoAction.getEnabled() )
                    btnDoAction.notifyListeners(SWT.Selection, new Event());
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblListModels, 10);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(100, -10);
        tblModels.setLayoutData(fd);

        TableColumn colModelName = new TableColumn(tblModels, SWT.NONE);
        colModelName.setText("Model name");
        colModelName.setWidth(265);

        grpModelVersions = new Group(compoRightBottom, SWT.SHADOW_ETCHED_IN);
        grpModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
        grpModelVersions.setFont(GROUP_TITLE_FONT);
        grpModelVersions.setText("Versions of selected model : ");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(50, 5);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        grpModelVersions.setLayoutData(fd);
        grpModelVersions.setLayout(new FormLayout());

        tblModelVersions = new Table(grpModelVersions,  SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        tblModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
        tblModelVersions.setLinesVisible(true);
        tblModelVersions.setHeaderVisible(true);
        tblModelVersions.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                if ( (tblModelVersions.getSelection() != null) && (tblModelVersions.getSelection().length > 0) && (tblModelVersions.getSelection()[0] != null) ) {
                    txtReleaseNote.setText((String) tblModelVersions.getSelection()[0].getData("note"));
                    txtPurpose.setText((String) tblModelVersions.getSelection()[0].getData("purpose"));
                    txtModelName.setText((String) tblModelVersions.getSelection()[0].getData("name"));
                    btnDoAction.setEnabled(true);
                } else {
                    btnDoAction.setEnabled(false);
                }
            }
        });
        tblModelVersions.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event e) {
                if ( btnDoAction.getEnabled() )
                    btnDoAction.notifyListeners(SWT.Selection, new Event());
            }
        });
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(50);
        tblModelVersions.setLayoutData(fd);

        TableColumn colVersion = new TableColumn(tblModelVersions, SWT.NONE);
        colVersion.setText("#");
        colVersion.setWidth(20);

        TableColumn colCreatedOn = new TableColumn(tblModelVersions, SWT.NONE);
        colCreatedOn.setText("Date");
        colCreatedOn.setWidth(120);

        TableColumn colCreatedBy = new TableColumn(tblModelVersions, SWT.NONE);
        colCreatedBy.setText("Author");
        colCreatedBy.setWidth(125);

        lblModelName = new Label(grpModelVersions, SWT.NONE);
        lblModelName.setBackground(GROUP_BACKGROUND_COLOR);
        lblModelName.setText("Model name :");
        fd = new FormData();
        fd.top = new FormAttachment(tblModelVersions, 10);
        fd.left = new FormAttachment(0, 10);
        lblModelName.setLayoutData(fd);

        txtModelName = new Text(grpModelVersions, SWT.BORDER);
        txtModelName.setBackground(GROUP_BACKGROUND_COLOR);
        txtModelName.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(lblModelName);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        txtModelName.setLayoutData(fd);

        lblPurpose = new Label(grpModelVersions, SWT.NONE);
        lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
        lblPurpose.setText("Purpose :");
        fd = new FormData();
        fd.top = new FormAttachment(txtModelName, 10);
        fd.left = new FormAttachment(0, 10);
        lblPurpose.setLayoutData(fd);

        txtPurpose = new Text(grpModelVersions, SWT.BORDER);
        txtPurpose.setBackground(GROUP_BACKGROUND_COLOR);
        txtPurpose.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(lblPurpose);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(80, -5);
        txtPurpose.setLayoutData(fd);

        lblReleaseNote = new Label(grpModelVersions, SWT.NONE);
        lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
        lblReleaseNote.setText("Release note :");
        fd = new FormData();
        fd.top = new FormAttachment(txtPurpose, 10);
        fd.left = new FormAttachment(0, 10);
        lblReleaseNote.setLayoutData(fd);

        txtReleaseNote = new Text(grpModelVersions, SWT.BORDER);
        txtReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
        txtReleaseNote.setEnabled(false);
        fd = new FormData();
        fd.top = new FormAttachment(lblReleaseNote);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(100, -10);
        txtReleaseNote.setLayoutData(fd);
    }

    /**
     * Creates a group displaying details about the imported model's components
     */
    private void createGrpComponents() {
        grpComponents = new Group(compoRightBottom, SWT.SHADOW_ETCHED_IN);
        grpComponents.setVisible(false);
        grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
        grpComponents.setFont(GROUP_TITLE_FONT);
        grpComponents.setText("Your model's components : ");
        FormData fd = new FormData();
        fd.top = new FormAttachment(100, -220);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        grpComponents.setLayoutData(fd);
        grpComponents.setLayout(new FormLayout());

        Label lblElements = new Label(grpComponents, SWT.NONE);
        lblElements.setBackground(GROUP_BACKGROUND_COLOR);
        lblElements.setText("Elements :");
        fd = new FormData();
        fd.top = new FormAttachment(0, 25);
        fd.left = new FormAttachment(0, 30);
        lblElements.setLayoutData(fd);

        Label lblRelationships = new Label(grpComponents, SWT.NONE);
        lblRelationships.setBackground(GROUP_BACKGROUND_COLOR);
        lblRelationships.setText("Relationships :");
        fd = new FormData();
        fd.top = new FormAttachment(lblElements, 10);
        fd.left = new FormAttachment(0, 30);
        lblRelationships.setLayoutData(fd);

        Label lblFolders = new Label(grpComponents, SWT.NONE);
        lblFolders.setBackground(GROUP_BACKGROUND_COLOR);
        lblFolders.setText("Folders :");
        fd = new FormData();
        fd.top = new FormAttachment(lblRelationships, 10);
        fd.left = new FormAttachment(0, 30);
        lblFolders.setLayoutData(fd);

        Label lblViews = new Label(grpComponents, SWT.NONE);
        lblViews.setBackground(GROUP_BACKGROUND_COLOR);
        lblViews.setText("Views :");
        fd = new FormData();
        fd.top = new FormAttachment(lblFolders, 10);
        fd.left = new FormAttachment(0, 30);
        lblViews.setLayoutData(fd);

        Label lblViewObjects = new Label(grpComponents, SWT.NONE);
        lblViewObjects.setBackground(GROUP_BACKGROUND_COLOR);
        lblViewObjects.setText("Objects :");
        fd = new FormData();
        fd.top = new FormAttachment(lblViews, 10);
        fd.left = new FormAttachment(0, 30);
        lblViewObjects.setLayoutData(fd);

        Label lblViewConnections = new Label(grpComponents, SWT.NONE);
        lblViewConnections.setBackground(GROUP_BACKGROUND_COLOR);
        lblViewConnections.setText("Connections :");
        fd = new FormData();
        fd.top = new FormAttachment(lblViewObjects, 10);
        fd.left = new FormAttachment(0, 30);
        lblViewConnections.setLayoutData(fd);

        Label lblImages = new Label(grpComponents, SWT.NONE);
        lblImages.setBackground(GROUP_BACKGROUND_COLOR);
        lblImages.setText("Images :");
        fd = new FormData();
        fd.top = new FormAttachment(lblViewConnections, 10);
        fd.left = new FormAttachment(0, 30);
        lblImages.setLayoutData(fd);

        /* * * * * */

        Label lblTotal = new Label(grpComponents, SWT.CENTER);
        lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
        lblTotal.setText("Total");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(20, 10);
        fd.right = new FormAttachment(40, -10);
        lblTotal.setLayoutData(fd);

        Label lblImported = new Label(grpComponents, SWT.CENTER);
        lblImported.setBackground(GROUP_BACKGROUND_COLOR);
        lblImported.setText("Imported");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(40, 10);
        fd.right = new FormAttachment(60, -10);
        lblImported.setLayoutData(fd);

        /* * * * * */

        txtTotalElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalElements.setLayoutData(fd);

        txtImportedElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedElements.setLayoutData(fd);

        txtTotalRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalRelationships.setLayoutData(fd);

        txtImportedRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedRelationships.setLayoutData(fd);

        txtTotalFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalFolders.setLayoutData(fd);

        txtImportedFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedFolders.setLayoutData(fd);

        txtTotalViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViews.setLayoutData(fd);

        txtImportedViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedViews.setLayoutData(fd);

        txtTotalViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViewObjects.setLayoutData(fd);

        txtImportedViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedViewObjects.setLayoutData(fd);

        txtTotalViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViewConnections.setLayoutData(fd);

        txtImportedViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedViewConnections.setLayoutData(fd);

        txtTotalImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalImages.setLayoutData(fd);

        txtImportedImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtImportedImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImported, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImported, 0, SWT.RIGHT);
        txtImportedImages.setLayoutData(fd);
    }

    /**
     * Called when the user clicks on the "import" button 
     */
    protected void doImport() {
        String modelName = tblModels.getSelection()[0].getText();
        String modelId = (String)tblModels.getSelection()[0].getData("id");
        
        logger.info("Importing model \""+modelName+"\"");

        hideGrpDatabase();
        createProgressBar("Importing model \""+modelName+"\"", 0, 100);


        grpModels.setVisible(false);
        grpComponents.setVisible(true);
        
        // we reorganize the grpModelVersion widget
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(grpComponents, -10);
        grpModelVersions.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(0, 10);
        fd.right = new FormAttachment(40, -10);
        fd.bottom = new FormAttachment(100, -10);
        tblModelVersions.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(40, 0);
        lblModelName.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(lblModelName, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelName, 80, SWT.LEFT);
        fd.right = new FormAttachment(100, -10);
        txtModelName.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(txtModelName, 10);
        fd.left = new FormAttachment(40, 0);
        lblPurpose.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(txtModelName, 5);
        fd.left = new FormAttachment(txtModelName, 0, SWT.LEFT);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(55, -5);
        txtPurpose.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(txtPurpose, 10);
        fd.left = new FormAttachment(40, 0);
        lblReleaseNote.setLayoutData(fd);
        
        fd = new FormData();
        fd.top = new FormAttachment(txtPurpose, 5);
        fd.left = new FormAttachment(txtModelName, 0, SWT.LEFT);
        fd.right = new FormAttachment(100, -10);
        fd.bottom = new FormAttachment(100, -10);
        txtReleaseNote.setLayoutData(fd);
        
        compoRightBottom.layout();
        
        setActiveAction(ACTION.Two);
        
        // we create the model (but do not create standard folder as they will be imported from the database)
        modelToImport = (ArchimateModel)IArchimateFactory.eINSTANCE.createArchimateModel();
        modelToImport.setId(modelId);
        modelToImport.setName(modelName);
        modelToImport.setPurpose((String)tblModels.getSelection()[0].getData("purpose"));
        
        // we get the selected model version to import
        // if the value is empty, this means that the user selected the "Now" line, so wh must load the latest version of the views
        if ( !tblModelVersions.getSelection()[0].getText(0).isEmpty() ) {
        	modelToImport.getCurrentVersion().setVersion(Integer.valueOf(tblModelVersions.getSelection()[0].getText(0)));
        	modelToImport.setImportLatestVersion(false);
        } else {
        	modelToImport.getCurrentVersion().setVersion(Integer.valueOf(tblModelVersions.getItem(1).getText(0)));
        	modelToImport.setImportLatestVersion(true);
        }

        // we add the new model in the manager
        IEditorModelManager.INSTANCE.registerModel(modelToImport);

        // we import the model from the database in a separate thread
        try {
            if ( logger.isDebugEnabled() ) logger.debug("Importing the model metadata ...");
            popup("Please wait while getting the model metadata ...");
            int importSize = connection.importModelMetadata(modelToImport);
            closePopup();
            setProgressBarMinAndMax(0, importSize);

            txtTotalElements.setText(String.valueOf(connection.countElementsToImport()));
            txtTotalRelationships.setText(String.valueOf(connection.countRelationshipsToImport()));
            txtTotalFolders.setText(String.valueOf(connection.countFoldersToImport()));
            txtTotalViews.setText(String.valueOf(connection.countViewsToImport()));
            txtTotalViewObjects.setText(String.valueOf(connection.countViewObjectsToImport()));
            txtTotalViewConnections.setText(String.valueOf(connection.countViewConnectionsToImport()));
            txtTotalImages.setText(String.valueOf(connection.countImagesToImport()));

            txtImportedElements.setText(String.valueOf(connection.countElementsImported()));
            txtImportedRelationships.setText(String.valueOf(connection.countRelationshipsImported()));
            txtImportedFolders.setText(String.valueOf(connection.countFoldersImported()));
            txtImportedViews.setText(String.valueOf(connection.countViewsImported()));
            txtImportedViewObjects.setText(String.valueOf(connection.countViewObjectsImported()));
            txtImportedViewConnections.setText(String.valueOf(connection.countViewConnectionsImported()));
            txtImportedImages.setText(String.valueOf(connection.countImagesImported()));

            if ( logger.isDebugEnabled() ) logger.debug("Importing the folders ...");
            connection.prepareImportFolders(modelToImport);
            while ( connection.importFolders(modelToImport) ) {
            	txtImportedFolders.setText(String.valueOf(connection.countFoldersImported()));
                increaseProgressBar();
            }

            if ( logger.isDebugEnabled() ) logger.debug("Importing the elements ...");
            connection.prepareImportElements(modelToImport);
            while ( connection.importElements(modelToImport) ) {
            	txtImportedElements.setText(String.valueOf(connection.countElementsImported()));
                increaseProgressBar();
            }

            if ( logger.isDebugEnabled() ) logger.debug("Importing the relationships ...");
            connection.prepareImportRelationships(modelToImport);
            while ( connection.importRelationships(modelToImport) ) {
            	txtImportedRelationships.setText(String.valueOf(connection.countRelationshipsImported()));
                increaseProgressBar();
            }

            if ( logger.isDebugEnabled() ) logger.debug("Resolving relationships' sources and targets ...");
            modelToImport.resolveRelationshipsSourcesAndTargets();


            if ( logger.isDebugEnabled() ) logger.debug("Importing the views ...");
            connection.prepareImportViews(modelToImport);
            while ( connection.importViews(modelToImport) ) {
            	txtImportedViews.setText(String.valueOf(connection.countViewsImported()));
                increaseProgressBar();
            }

            if ( logger.isDebugEnabled() ) logger.debug("Importing the views objects ...");
            for (IDiagramModel view: modelToImport.getAllViews().values()) {
                connection.prepareImportViewsObjects(view.getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion());
                while ( connection.importViewsObjects(modelToImport, view) ) {
                	txtImportedViewObjects.setText(String.valueOf(connection.countViewObjectsImported()));
                    increaseProgressBar();
                }
            }
            txtImportedElements.setText(String.valueOf(connection.countElementsImported()));

            if ( logger.isDebugEnabled() ) logger.debug("Importing the views connections ...");
            for (IDiagramModel view: modelToImport.getAllViews().values()) {
                connection.prepareImportViewsConnections(view.getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion());
                while ( connection.importViewsConnections(modelToImport) ) {
                	txtImportedViewConnections.setText(String.valueOf(connection.countViewConnectionsImported()));
                    increaseProgressBar();
                }
            }
            txtImportedRelationships.setText(String.valueOf(connection.countRelationshipsImported()));

            if ( logger.isDebugEnabled() ) logger.debug("Resolving connections' sources and targets ...");
            modelToImport.resolveConnectionsSourcesAndTargets();

            if ( logger.isDebugEnabled() ) logger.debug("importing the images ...");
            for (String path: connection.getAllImagePaths()) {
                connection.importImage(modelToImport, path);
                txtImportedImages.setText(String.valueOf(connection.countImagesImported()));
                increaseProgressBar();
            }
        } catch (Exception err) {
        	closePopup();
            popup(Level.ERROR, "Failed to import model from database.", err);
            setActiveAction(STATUS.Error);
            doShowResult(err);
            return;
        }

        setActiveAction(STATUS.Ok);
        doShowResult(null);
        return;
    }

    protected void doShowResult(Exception err) {
        logger.debug("Showing result.");
        if ( grpProgressBar != null ) grpProgressBar.setVisible(false);

        setActiveAction(ACTION.Three);
        btnClose.setText("close");
        
        Color statusColor = GREEN_COLOR;
        
		if ( logger.isTraceEnabled() ) {
		    logger.trace(connection.countElementsImported()+"/"+connection.countElementsToImport()+" elements imported");
		    logger.trace(connection.countRelationshipsImported()+"/"+connection.countRelationshipsToImport()+" relationships imported");
		    logger.trace(connection.countFoldersImported()+"/"+connection.countFoldersToImport()+" folders imported");
		    logger.trace(connection.countViewsImported()+"/"+connection.countViewsToImport()+" views imported");
		    logger.trace(connection.countViewObjectsImported()+"/"+connection.countViewObjectsToImport()+" views objects imported");
		    logger.trace(connection.countViewConnectionsImported()+"/"+connection.countViewConnectionsToImport()+" views connections imported");
		    logger.trace(connection.countImagesImported()+"/"+connection.countImagesToImport()+" images imported");
		}

        txtImportedElements.setForeground( (connection.countElementsImported() == connection.countElementsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        txtImportedRelationships.setForeground( (connection.countRelationshipsImported() == connection.countRelationshipsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        txtImportedFolders.setForeground( (connection.countFoldersImported() == connection.countFoldersToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        txtImportedViews.setForeground( (connection.countViewsImported() == connection.countViewsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        txtImportedViewObjects.setForeground( (connection.countViewObjectsImported() == connection.countViewObjectsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        txtImportedViewConnections.setForeground( (connection.countViewConnectionsImported() == connection.countViewConnectionsToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
        txtImportedImages.setForeground( (connection.countImagesImported() == connection.countImagesToImport()) ? GREEN_COLOR : (statusColor=RED_COLOR) );

        if ( err == null ) {
        	// if all the counters are equals to the expected values
        	if ( statusColor == GREEN_COLOR ) {
        	   	setMessage("Import successful", statusColor);
            	
            	IEditorModelManager.INSTANCE.openModel(modelToImport);											// We open the Model in the Editor
            	ITreeModelView view = (ITreeModelView)ViewManager.showViewPart(ITreeModelView.ID, true);		// We select the model in the tree
	            if(view != null) {
	                List<Object> elements = new ArrayList<Object>();
	                elements.add(modelToImport.getDefaultFolderForObject(modelToImport.getAllViews().entrySet().iterator().next().getValue()));
	                view.getViewer().setSelection(new StructuredSelection(elements), true);
	                elements = new ArrayList<Object>();
	                elements.add(modelToImport);
	                view.getViewer().setSelection(new StructuredSelection(elements), true);
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
	                CommandStack stack = (CommandStack)modelToImport.getAdapter(CommandStack.class);
	                stack.markSaveLocation();
	    
	                IEditorModelManager.INSTANCE.closeModel(modelToImport);
	            } catch (IOException e) {
	                popup(Level.FATAL, "Failed to close the model partially imported.\n\nWe suggest you close and restart Archi.", e);
	            }
	        } else {
	            popup(Level.ERROR, "Please be warn that the model you just imported is not concistent.\n\nYou choosed to keep it in the preferences, but should you export it back to the database, you may loose data.\n\nDo it at your own risk !");
	        }
        }
    }
}
