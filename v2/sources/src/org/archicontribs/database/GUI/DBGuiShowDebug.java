/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.GUI;

import org.apache.log4j.Level;
import org.archicontribs.database.connection.DBDatabaseExportConnection;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelImage;
import com.archimatetool.model.IDiagramModelImageProvider;

public class DBGuiShowDebug extends DBGui {
    //private static final DBLogger logger = new DBLogger(DBGuiExportModel.class);
    
    private IArchimateModelObject selectedObject;
    
    private DBDatabaseExportConnection connection = null;
    
    private Group grpDebug;
    
    private Label selectedComponentLbl;
    private Label selectedComponentNameLbl;
    private Label selectedComponentNameValueLbl;
    private Label selectedComponentIdLbl;
    private Label selectedComponentIdValueLbl;
    private Label selectedComponentClassLbl;
    private Label selectedComponentClassValueLbl;
    private Label selectedComponentImagePathLbl;
    private Label selectedComponentImagePathValueLbl;
    private Label selectedComponentDatabaseStatusLbl;
    private Label selectedComponentDatabaseStatusValueLbl;
    private Table selectedComponentDebugTable;
    
    private Label correspondingConceptLbl;
    private Label correspondingConceptNameLbl;
    private Label correspondingConceptNameValueLbl;
    private Label correspondingConceptIdLbl;
    private Label correspondingConceptIdValueLbl;
    private Label correspondingConceptClassLbl;
    private Label correspondingConceptClassValueLbl;
    private Label correspondingConceptDatabaseStatusLbl;
    private Label correspondingConceptDatabaseStatusValueLbl;
    private Table correspondingConceptDebugTable;

    /**
     * Creates the GUI that shows the debug information
     * 
     * @param oj the component which we want the debug information
     * @param title the title of the window
     */
    public DBGuiShowDebug(IArchimateModelObject obj, String title) {
        // We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
        super(title);
        
        this.selectedObject = obj;
        
        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for showing debug information about "+((IDBMetadata)obj).getDBMetadata().getDebugName());
        
        createGrpDebug();
        
        createAction(ACTION.One, "1 - Show debug");
        
        // we show an arrow in front of the first action
        setActiveAction(ACTION.One);

        // We activate the Eclipse Help framework
        setHelpHref("showDebug.html");
    }
    
    private void createGrpDebug() {
        this.grpDebug = new Group(this.compoRightBottom, SWT.NONE);
        this.grpDebug.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpDebug.setText("Debug information: ");
        this.grpDebug.setFont(GROUP_TITLE_FONT);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpDebug.setLayoutData(fd);
        this.grpDebug.setLayout(new FormLayout());
        
        // selected component
        this.selectedComponentLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentLbl.setText("You selected the following component:");
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(0, 20);
        this.selectedComponentLbl.setLayoutData(fd);
        
        // Name
        this.selectedComponentNameLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentNameLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentNameLbl.setText("Name:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentLbl, 5);
        fd.left = new FormAttachment(0, 70);
        this.selectedComponentNameLbl.setLayoutData(fd);
        
        this.selectedComponentNameValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentNameValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentNameLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentNameValueLbl.setLayoutData(fd);
        
        // Id
        this.selectedComponentIdLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentIdLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentIdLbl.setText("Id:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentNameLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.selectedComponentIdLbl.setLayoutData(fd);
        
        this.selectedComponentIdValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentIdValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentIdLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentIdValueLbl.setLayoutData(fd);
        
        // Class
        this.selectedComponentClassLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentClassLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentClassLbl.setText("Class:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentIdLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.selectedComponentClassLbl.setLayoutData(fd);
        
        this.selectedComponentClassValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentClassValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentClassLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentClassValueLbl.setLayoutData(fd);
        
        // Image Path
        this.selectedComponentImagePathLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentImagePathLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentImagePathLbl.setText("Image Path:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentClassLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.selectedComponentImagePathLbl.setLayoutData(fd);
        
        this.selectedComponentImagePathValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentImagePathValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentImagePathLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentImagePathValueLbl.setLayoutData(fd);
        
        // Database status
        this.selectedComponentDatabaseStatusLbl = new Label(this.grpDebug, SWT.BOLD);
        this.selectedComponentDatabaseStatusLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentDatabaseStatusLbl.setText("Database status:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentImagePathLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.selectedComponentDatabaseStatusLbl.setLayoutData(fd);
        
        this.selectedComponentDatabaseStatusValueLbl = new Label(this.grpDebug, SWT.BOLD);
        this.selectedComponentDatabaseStatusValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentDatabaseStatusValueLbl.setLayoutData(fd);
        
        // Table
        this.selectedComponentDebugTable = new Table(this.grpDebug, SWT.BORDER | SWT.FULL_SELECTION);
        this.selectedComponentDebugTable.setLinesVisible(true);
        this.selectedComponentDebugTable.setHeaderVisible(true);
        this.selectedComponentDebugTable.setBackground(TABLE_BACKGROUND_COLOR);
        this.selectedComponentDebugTable.setHeaderBackground(COMPO_LEFT_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentDatabaseStatusValueLbl, 10);
        fd.left = new FormAttachment(0, 20);
        fd.right = new FormAttachment(100, -20);
        fd.bottom = new FormAttachment(50, -10);
        this.selectedComponentDebugTable.setLayoutData(fd);
        
        TableColumn column = new TableColumn(this.selectedComponentDebugTable, SWT.NONE);
        column.setWidth(100);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Version");
        column.setWidth(50);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Container checksum");
        column.setWidth(230);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Checksum");
        column.setWidth(230);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Created on");
        column.setWidth(140);
        
        // corresponding concept
        this.correspondingConceptLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptLbl.setText("It is related to the following concept:");
        fd = new FormData();
        fd.top = new FormAttachment(50, 10);
        fd.left = new FormAttachment(0, 20);
        this.correspondingConceptLbl.setLayoutData(fd);
        
        // Name
        this.correspondingConceptNameLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptNameLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptNameLbl.setText("Name:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptLbl, 5);
        fd.left = new FormAttachment(0, 70);
        this.correspondingConceptNameLbl.setLayoutData(fd);
        
        this.correspondingConceptNameValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptNameValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptNameLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptNameValueLbl.setLayoutData(fd);
        
        // Id
        this.correspondingConceptIdLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptIdLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptIdLbl.setText("Id:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptNameLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.correspondingConceptIdLbl.setLayoutData(fd);
        
        this.correspondingConceptIdValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptIdValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptIdLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptIdValueLbl.setLayoutData(fd);
        
        // Class
        this.correspondingConceptClassLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptClassLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptClassLbl.setText("Class:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptIdLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.correspondingConceptClassLbl.setLayoutData(fd);
        
        this.correspondingConceptClassValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptClassValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptClassLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptClassValueLbl.setLayoutData(fd);
        
        this.correspondingConceptDatabaseStatusLbl = new Label(this.grpDebug, SWT.BOLD);
        this.correspondingConceptDatabaseStatusLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptDatabaseStatusLbl.setText("Database status:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptClassLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.correspondingConceptDatabaseStatusLbl.setLayoutData(fd);
        
        this.correspondingConceptDatabaseStatusValueLbl = new Label(this.grpDebug, SWT.BOLD);
        this.correspondingConceptDatabaseStatusValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptDatabaseStatusLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 60);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptDatabaseStatusValueLbl.setLayoutData(fd);
        
        // Table
        this.correspondingConceptDebugTable = new Table(this.grpDebug, SWT.BORDER | SWT.FULL_SELECTION);
        this.correspondingConceptDebugTable.setLinesVisible(true);
        this.correspondingConceptDebugTable.setHeaderVisible(true);
        this.correspondingConceptDebugTable.setBackground(TABLE_BACKGROUND_COLOR);
        this.correspondingConceptDebugTable.setHeaderBackground(COMPO_LEFT_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptDatabaseStatusValueLbl, 10);
        fd.left = new FormAttachment(0, 20);
        fd.right = new FormAttachment(100, -20);
        fd.bottom = new FormAttachment(100, -10);
        this.correspondingConceptDebugTable.setLayoutData(fd);
        
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.NONE);
        column.setWidth(100);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Version");
        column.setWidth(50);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Container checksum");
        column.setWidth(230);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Checksum");
        column.setWidth(230);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Created on");
        column.setWidth(140);
    }
    
    @Override
    public void run() {
        super.run();
        
        try {
            ((DBArchimateModel)this.selectedObject.getArchimateModel()).countObject(this.selectedObject, true, null);
            if ( this.selectedObject instanceof IDiagramModelArchimateComponent )
                ((DBArchimateModel)this.selectedObject.getArchimateModel()).countObject(((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept(), true, null);
        } catch (Exception e) {
            popup(Level.ERROR, "Failed to calculate checksum for selected component.", e);
            close();
            return;
        }
        
        this.selectedComponentNameValueLbl.setText(this.selectedObject.getName());
        this.selectedComponentIdValueLbl.setText(this.selectedObject.getId());
        this.selectedComponentClassValueLbl.setText(this.selectedObject.getClass().getSimpleName());
        String imagePath = null;
        if ( this.selectedObject instanceof IDiagramModelImageProvider )  
            imagePath = ((IDiagramModelImageProvider)this.selectedObject).getImagePath() == null ? "(null)" : ((IDiagramModelImageProvider)this.selectedObject).getImagePath();
        else if ( this.selectedObject instanceof IDiagramModelImage )  
            imagePath = ((IDiagramModelImage)this.selectedObject).getImagePath() == null ? "(null)" : ((IDiagramModelImage)this.selectedObject).getImagePath();
        
        if ( imagePath == null ) {
            this.selectedComponentImagePathLbl.setVisible(false);
            this.selectedComponentImagePathValueLbl.setVisible(false);
            
            FormData fd = new FormData();
            fd.top = new FormAttachment(this.selectedComponentClassLbl, 2);
            fd.left = new FormAttachment(0, 70);
            this.selectedComponentDatabaseStatusLbl.setLayoutData(fd);
        } else {
            this.selectedComponentImagePathLbl.setVisible(true);
            this.selectedComponentImagePathValueLbl.setVisible(true);
            
            FormData fd = new FormData();
            fd.top = new FormAttachment(this.selectedComponentImagePathLbl, 2);
            fd.left = new FormAttachment(0, 70);
            this.selectedComponentDatabaseStatusLbl.setLayoutData(fd);
            
            this.selectedComponentImagePathValueLbl.setText(imagePath);
        }
        
        this.grpDebug.layout();
        
        this.selectedComponentDebugTable.removeAll();
        this.correspondingConceptDebugTable.removeAll();
        
        if ( this.selectedObject instanceof IDiagramModelArchimateComponent ) {
            this.correspondingConceptLbl.setVisible(true);
            this.correspondingConceptNameLbl.setVisible(true);
            this.correspondingConceptNameValueLbl.setVisible(true);
            this.correspondingConceptIdLbl.setVisible(true);
            this.correspondingConceptIdValueLbl.setVisible(true);
            this.correspondingConceptClassLbl.setVisible(true);
            this.correspondingConceptClassValueLbl.setVisible(true);
            this.correspondingConceptDatabaseStatusLbl.setVisible(true);
            this.correspondingConceptDatabaseStatusValueLbl.setVisible(true);
            this.correspondingConceptDebugTable.setVisible(true);
            
            IArchimateConcept concept = ((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept();
            this.correspondingConceptNameValueLbl.setText(concept.getName());
            this.correspondingConceptIdValueLbl.setText(concept.getId());
            this.correspondingConceptClassValueLbl.setText(concept.getClass().getSimpleName());
        } else {
            this.correspondingConceptLbl.setVisible(false);
            this.correspondingConceptNameLbl.setVisible(false);
            this.correspondingConceptNameValueLbl.setVisible(false);
            this.correspondingConceptIdLbl.setVisible(false);
            this.correspondingConceptIdValueLbl.setVisible(false);
            this.correspondingConceptClassLbl.setVisible(false);
            this.correspondingConceptClassValueLbl.setVisible(false);
            this.correspondingConceptDatabaseStatusLbl.setVisible(false);
            this.correspondingConceptDatabaseStatusValueLbl.setVisible(false);
            this.correspondingConceptDebugTable.setVisible(false);
        }
        
        if ( !(this.selectedObject instanceof IDiagramModel) ) {
            TableColumn column = this.selectedComponentDebugTable.getColumn(2);
            column.setResizable(false);
            column.setWidth(0);

            column = this.correspondingConceptDebugTable.getColumn(2);
            column.setResizable(false);
            column.setWidth(0);
        }
            
        refreshDisplay();
        
        try {
            getDatabases(true);
        } catch (Exception err) {
            popup(Level.ERROR, "Failed to get the databases.", err);
            return;
        }
    }
    
    /**
     * This method is called each time a database is selected and a connection has been established to it.<br>
     * <br>
     * It enables the export button and starts the export if the "automatic start" is specified in the plugin preferences
     */
    @Override
    protected void connectedToDatabase(boolean forceCheckDatabase) {
        this.selectedComponentDebugTable.removeAll();
        this.correspondingConceptDebugTable.removeAll();
        
        this.connection = new DBDatabaseExportConnection(getDatabaseConnection());
        
        // we get the version and checksum from the database
        try {
            if ( this.selectedObject instanceof DBArchimateModel )
                this.connection.getVersionsFromDatabase((DBArchimateModel)this.selectedObject);
            else {
                DBMetadata metadata = ((IDBMetadata)this.selectedObject).getDBMetadata();
                
                metadata.getCurrentVersion().setVersion(0);
                metadata.getInitialVersion().reset();
                metadata.getDatabaseVersion().reset();
                metadata.getLatestDatabaseVersion().reset();
                this.connection.getVersionFromDatabase(this.selectedObject);
                
                if ( this.selectedObject instanceof IDiagramModelArchimateComponent ) {
                    IArchimateConcept concept = ((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept();
                    metadata = ((IDBMetadata)concept).getDBMetadata();
                    metadata.getCurrentVersion().setVersion(0);
                    metadata.getInitialVersion().reset();
                    metadata.getDatabaseVersion().reset();
                    metadata.getLatestDatabaseVersion().reset();
                    this.connection.getVersionFromDatabase(((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept());
                }
            }
        } catch (Exception err) {
            popup(Level.ERROR, "Failed to get information about component from the database.", err);
            return;
        }
        
        if ( this.selectedObject instanceof DBArchimateModel ) {
            DBArchimateModel model = (DBArchimateModel)this.selectedObject;
            
            TableItem item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Initial");
            item.setText(1, String.valueOf(model.getInitialVersion().getVersion()));
            item.setText(2, model.getInitialVersion().getContainerChecksum());
            item.setText(3, "");
            item.setText(4, model.getInitialVersion().getTimestamp() == DBVersion.NEVER ? "" : model.getInitialVersion().getTimestamp().toString());
            
            item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Current");
            item.setText(1, String.valueOf(model.getCurrentVersion().getVersion()));
            item.setText(2, model.getCurrentVersion().getContainerChecksum());
            item.setText(3, "");
            item.setText(4, model.getCurrentVersion().getTimestamp() == DBVersion.NEVER ? "" : model.getCurrentVersion().getTimestamp().toString());
            
            item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Database");
            item.setText(1, String.valueOf(model.getDatabaseVersion().getVersion()));
            item.setText(2, model.getDatabaseVersion().getContainerChecksum());
            item.setText(3, "");
            item.setText(4, model.getDatabaseVersion().getTimestamp() == DBVersion.NEVER ? "" : model.getDatabaseVersion().getTimestamp().toString());
        } else {
            DBMetadata metadata = ((IDBMetadata)this.selectedObject).getDBMetadata();
            
            this.selectedComponentDatabaseStatusValueLbl.setText(metadata.getDatabaseStatus().toString());
            
            TableItem item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Initial");
            item.setText(1, String.valueOf(metadata.getInitialVersion().getVersion()));
            item.setText(2, metadata.getInitialVersion().getContainerChecksum());
            item.setText(3, metadata.getInitialVersion().getChecksum());
            item.setText(4, metadata.getInitialVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getInitialVersion().getTimestamp().toString());
            
            item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Current");
            item.setText(1, String.valueOf(metadata.getCurrentVersion().getVersion()));
            item.setText(2, metadata.getCurrentVersion().getContainerChecksum());
            item.setText(3, metadata.getCurrentVersion().getChecksum());
            item.setText(4, metadata.getCurrentVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getCurrentVersion().getTimestamp().toString());
            
            item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Database");
            item.setText(1, String.valueOf(metadata.getDatabaseVersion().getVersion()));
            item.setText(2, metadata.getDatabaseVersion().getContainerChecksum());
            item.setText(3, metadata.getDatabaseVersion().getChecksum());
            item.setText(4, metadata.getDatabaseVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getLatestDatabaseVersion().getTimestamp().toString());
            
            item = new TableItem(this.selectedComponentDebugTable, SWT.NONE);
            item.setText(0, "Latest database");
            item.setText(1, String.valueOf(metadata.getLatestDatabaseVersion().getVersion()));
            item.setText(2, metadata.getLatestDatabaseVersion().getContainerChecksum());
            item.setText(3, metadata.getLatestDatabaseVersion().getChecksum());
            item.setText(4, metadata.getLatestDatabaseVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getLatestDatabaseVersion().getTimestamp().toString());
            
            if ( this.selectedObject instanceof IDiagramModelArchimateComponent ) {
                IArchimateConcept concept = ((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept();
                metadata = ((IDBMetadata)concept).getDBMetadata();
                
                this.correspondingConceptDatabaseStatusValueLbl.setText(metadata.getDatabaseStatus().toString());
                
                item = new TableItem(this.correspondingConceptDebugTable, SWT.NONE);
                item.setText(0, "Initial");
                item.setText(1, String.valueOf(metadata.getInitialVersion().getVersion()));
                item.setText(2, metadata.getInitialVersion().getContainerChecksum());
                item.setText(3, metadata.getInitialVersion().getChecksum());
                item.setText(4, metadata.getInitialVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getInitialVersion().getTimestamp().toString());
                
                item = new TableItem(this.correspondingConceptDebugTable, SWT.NONE);
                item.setText(0, "Current");
                item.setText(1, String.valueOf(metadata.getCurrentVersion().getVersion()));
                item.setText(2, metadata.getCurrentVersion().getContainerChecksum());
                item.setText(3, metadata.getCurrentVersion().getChecksum());
                item.setText(4, metadata.getCurrentVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getCurrentVersion().getTimestamp().toString());
                
                item = new TableItem(this.correspondingConceptDebugTable, SWT.NONE);
                item.setText(0, "Database");
                item.setText(1, String.valueOf(metadata.getDatabaseVersion().getVersion()));
                item.setText(2, metadata.getDatabaseVersion().getContainerChecksum());
                item.setText(3, metadata.getDatabaseVersion().getChecksum());
                item.setText(4, metadata.getDatabaseVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getDatabaseVersion().getTimestamp().toString());
                
                item = new TableItem(this.correspondingConceptDebugTable, SWT.NONE);
                item.setText(0, "Latest database");
                item.setText(1, String.valueOf(metadata.getLatestDatabaseVersion().getVersion()));
                item.setText(2, metadata.getLatestDatabaseVersion().getContainerChecksum());
                item.setText(3, metadata.getLatestDatabaseVersion().getChecksum());
                item.setText(4, metadata.getLatestDatabaseVersion().getTimestamp() == DBVersion.NEVER ? "" : metadata.getLatestDatabaseVersion().getTimestamp().toString());
            }
        }
    }
    
    @Override
    public void notConnectedToDatabase() {
        this.selectedComponentDebugTable.removeAll();
        this.correspondingConceptDebugTable.removeAll();
    }
}
