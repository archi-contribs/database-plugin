/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.gui;

import java.sql.SQLException;

import org.apache.log4j.Level;
import org.archicontribs.database.connection.DBDatabaseExportConnection;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.ecore.EObject;
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
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IIdentifier;

/**
 * This class manages the GUI that shows debug information about an archimate component
 * 
 * @author Herve Jouin
 */
public class DBGuiShowDebug extends DBGui {
    //private static final DBLogger logger = new DBLogger(DBGuiExportModel.class);
    
    private EObject selectedObject;
    private DBMetadata selectedMetadata;
    private DBArchimateModel model;
    
    private Group grpDebug;
    
    private Label selectedComponentNameValueLbl;
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
     * @param obj the component which we want the debug information
     * @param title the title of the window
     */
    public DBGuiShowDebug(EObject obj, String title) {
        // We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
        super(title);
        
        this.selectedObject = obj;
        // in Archi 4.2 and previous, we need to separate the two cases
        if ( obj instanceof IArchimateModelObject ) {
        	this.model = (DBArchimateModel) ((IArchimateModelObject)obj).getArchimateModel();
        	this.selectedMetadata = DBMetadata.getDBMetadata(obj);
        } else if ( obj instanceof IDiagramModelArchimateObject ) {
        	this.model = (DBArchimateModel) ((IDiagramModelArchimateObject)obj).getArchimateConcept().getArchimateModel();
        	this.selectedMetadata = DBMetadata.getDBMetadata(obj);
        } else {
        	DBGuiUtils.popup(Level.ERROR, "Do not know how to get debugging information about a "+obj.getClass().getSimpleName());
        	return;
        }
        
        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for showing debug information about "+DBMetadata.getDBMetadata(obj).getDebugName());
        
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
        Label selectedComponentLbl = new Label(this.grpDebug, SWT.NONE);
        selectedComponentLbl.setBackground(GROUP_BACKGROUND_COLOR);
        selectedComponentLbl.setText("You selected the following component:");
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(0, 20);
        selectedComponentLbl.setLayoutData(fd);
        
        // Name label
        Label selectedComponentNameLbl = new Label(this.grpDebug, SWT.NONE);
        selectedComponentNameLbl.setBackground(GROUP_BACKGROUND_COLOR);
        selectedComponentNameLbl.setText("Name:");
        fd = new FormData();
        fd.top = new FormAttachment(selectedComponentLbl, 5);
        fd.left = new FormAttachment(0, 70);
        selectedComponentNameLbl.setLayoutData(fd);
        
        // Id label
        Label selectedComponentIdLbl = new Label(this.grpDebug, SWT.NONE);
        selectedComponentIdLbl.setBackground(GROUP_BACKGROUND_COLOR);
        selectedComponentIdLbl.setText("Id:");
        fd = new FormData();
        fd.top = new FormAttachment(selectedComponentNameLbl, 2);
        fd.left = new FormAttachment(selectedComponentNameLbl, 0, SWT.LEFT);
        selectedComponentIdLbl.setLayoutData(fd);
        
        // Class label
        this.selectedComponentClassLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentClassLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentClassLbl.setText("Class:");
        fd = new FormData();
        fd.top = new FormAttachment(selectedComponentIdLbl, 2);
        fd.left = new FormAttachment(selectedComponentNameLbl, 0, SWT.LEFT);
        this.selectedComponentClassLbl.setLayoutData(fd);
        
        // Image Path label
        this.selectedComponentImagePathLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentImagePathLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentImagePathLbl.setText("Image Path:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentClassLbl, 2);
        fd.left = new FormAttachment(selectedComponentNameLbl, 0, SWT.LEFT);
        this.selectedComponentImagePathLbl.setLayoutData(fd);
        
        // Database status label
        this.selectedComponentDatabaseStatusLbl = new Label(this.grpDebug, SWT.BOLD);
        this.selectedComponentDatabaseStatusLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.selectedComponentDatabaseStatusLbl.setText("Database status:");
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentImagePathLbl, 2);
        fd.left = new FormAttachment(0, 70);
        this.selectedComponentDatabaseStatusLbl.setLayoutData(fd);
        
        
        
        
        // name value
        this.selectedComponentNameValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentNameValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(selectedComponentNameLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentNameValueLbl.setLayoutData(fd);
        
        // ID value
        this.selectedComponentIdValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentIdValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(selectedComponentIdLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentIdValueLbl.setLayoutData(fd);
        
        // class value
        this.selectedComponentClassValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentClassValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentClassLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentClassValueLbl.setLayoutData(fd);
        
        // Image path value
        this.selectedComponentImagePathValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.selectedComponentImagePathValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentImagePathLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.selectedComponentImagePathValueLbl.setLayoutData(fd);
        
        // Database status value
        this.selectedComponentDatabaseStatusValueLbl = new Label(this.grpDebug, SWT.BOLD);
        this.selectedComponentDatabaseStatusValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.selectedComponentDatabaseStatusLbl, 10);
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
        column.setWidth(135);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("#");
        column.setWidth(50);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Container checksum");
        column.setWidth(340);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Checksum");
        column.setWidth(340);
        column = new TableColumn(this.selectedComponentDebugTable, SWT.CENTER);
        column.setText("Created on");
        column.setWidth(220);
        
        // corresponding concept
        this.correspondingConceptLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptLbl.setText("It is related to the following Archimate concept:");
        fd = new FormData();
        fd.top = new FormAttachment(50, 10);
        fd.left = new FormAttachment(0, 20);
        this.correspondingConceptLbl.setLayoutData(fd);
        
        // Name label
        this.correspondingConceptNameLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptNameLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptNameLbl.setText("Name:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptLbl, 5);
        fd.left = new FormAttachment(0, 70);
        this.correspondingConceptNameLbl.setLayoutData(fd);
        
        // Id label
        this.correspondingConceptIdLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptIdLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptIdLbl.setText("Id:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptNameLbl, 2);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 0, SWT.LEFT);
        this.correspondingConceptIdLbl.setLayoutData(fd);

        // Class label
        this.correspondingConceptClassLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptClassLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptClassLbl.setText("Class:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptIdLbl, 2);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 0, SWT.LEFT);
        this.correspondingConceptClassLbl.setLayoutData(fd);
        
        // Database status label
        this.correspondingConceptDatabaseStatusLbl = new Label(this.grpDebug, SWT.BOLD);
        this.correspondingConceptDatabaseStatusLbl.setBackground(GROUP_BACKGROUND_COLOR);
        this.correspondingConceptDatabaseStatusLbl.setText("Database status:");
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptClassLbl, 2);
        fd.left = new FormAttachment(this.correspondingConceptNameLbl, 0, SWT.LEFT);
        this.correspondingConceptDatabaseStatusLbl.setLayoutData(fd);
        
        // Name value
        this.correspondingConceptNameValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptNameValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptNameLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptNameValueLbl.setLayoutData(fd);

        // Id value
        this.correspondingConceptIdValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptIdValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptIdLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptIdValueLbl.setLayoutData(fd);
        
        // Class value
        this.correspondingConceptClassValueLbl = new Label(this.grpDebug, SWT.NONE);
        this.correspondingConceptClassValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptClassLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptDatabaseStatusLbl, 10);
        fd.right = new FormAttachment(100, -20);
        this.correspondingConceptClassValueLbl.setLayoutData(fd);
        
        // Database status value
        this.correspondingConceptDatabaseStatusValueLbl = new Label(this.grpDebug, SWT.BOLD);
        this.correspondingConceptDatabaseStatusValueLbl.setBackground(GROUP_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.correspondingConceptDatabaseStatusLbl, 0, SWT.TOP);
        fd.left = new FormAttachment(this.correspondingConceptDatabaseStatusLbl, 10);
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
        column.setWidth(135);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("#");
        column.setWidth(50);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Container checksum");
        column.setWidth(340);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Checksum");
        column.setWidth(340);
        column = new TableColumn(this.correspondingConceptDebugTable, SWT.CENTER);
        column.setText("Created on");
        column.setWidth(220);
    }
    
    @Override
    public void run() {
        super.run();
        
        try {
            this.model.countObject(this.selectedObject, true);
            if ( this.selectedObject instanceof IDiagramModelArchimateComponent )
                this.model.countObject(((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept(), true);
        } catch (Exception e) {
            DBGuiUtils.popup(Level.ERROR, "Failed to calculate checksum for selected component.", e);
            close();
            return;
        }
        
        this.selectedComponentNameValueLbl.setText(this.selectedMetadata.getName());
        this.selectedComponentIdValueLbl.setText(this.selectedMetadata.getId());
        this.selectedComponentClassValueLbl.setText(this.selectedObject.getClass().getSimpleName());
        String imagePath = this.selectedMetadata.getImagePath();
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
        
        if ( !(this.selectedObject instanceof IDiagramModelContainer) ) {
            TableColumn column = this.selectedComponentDebugTable.getColumn(2);
            column.setResizable(false);
            column.setText("");
            column.setWidth(0);

            column = this.correspondingConceptDebugTable.getColumn(2);
            column.setResizable(false);
            column.setText("");
            column.setWidth(0);
        }
            
        refreshDisplay();
        
        try {
            getDatabases(true);
        } catch (Exception err) {
            DBGuiUtils.popup(Level.ERROR, "Failed to get the databases.", err);
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
        
        // we get the version and checksum from the database
        try ( DBDatabaseExportConnection exportConnection = new DBDatabaseExportConnection(getDatabaseConnection()) ) {
        	if ( this.selectedObject instanceof IArchimateModelObject )
        		exportConnection.getModelVersionFromDatabase((DBArchimateModel) ((IArchimateModelObject)this.selectedObject).getArchimateModel());
        	else if ( this.selectedObject instanceof IDiagramModelObject )
        		exportConnection.getModelVersionFromDatabase((DBArchimateModel) ((IDiagramModelObject)this.selectedObject).getDiagramModel().getArchimateModel());
        	else if ( this.selectedObject instanceof IDiagramModelConnection )
        		exportConnection.getModelVersionFromDatabase((DBArchimateModel) ((IDiagramModelConnection)this.selectedObject).getDiagramModel().getArchimateModel());
        	
            if ( this.selectedObject instanceof IDiagramModelArchimateComponent ) {
            	exportConnection.getVersionFromDatabase(((IDiagramModelArchimateComponent)this.selectedObject).getDiagramModel());
                exportConnection.getVersionFromDatabase(((IDiagramModelArchimateComponent)this.selectedObject).getArchimateConcept());
            }
            
            exportConnection.getVersionFromDatabase((IIdentifier)this.selectedObject);
        } catch (SQLException err) {
            DBGuiUtils.popup(Level.ERROR, "Failed to get information about component from the database.", err);
            return;
        }
        
        // in case of a view, we check if we need to recreate the screenshot
        if ( this.selectedObject instanceof IDiagramModel ) {
            DBMetadata metadata = DBMetadata.getDBMetadata(this.selectedObject);
            if ( this.connection.getDatabaseEntry().isViewSnapshotRequired() ) {
                if ( (metadata.getScreenshot().getBytes() == null)
                        || (metadata.getScreenshot().getScaleFactor() != this.connection.getDatabaseEntry().getViewsImagesScaleFactor())
                        || (metadata.getScreenshot().getBodrderWidth() != this.connection.getDatabaseEntry().getViewsImagesBorderWidth())
                        ) {
                    logger.debug("Creating screenshot of view \""+((IDiagramModel)this.selectedObject).getName()+"\"");
                    createImage((IDiagramModel)this.selectedObject, this.connection.getDatabaseEntry().getViewsImagesScaleFactor(), this.connection.getDatabaseEntry().getViewsImagesBorderWidth());
                }
                metadata.getScreenshot().setScreenshotActive(true);
            } else
                metadata.getScreenshot().setScreenshotActive(false);
            
            // we recalculate the view checksum
            try {
            	// we force the presence of the view in the allView map in order to keep the screenshot in the checksum calculation
            	this.model.getAllViews().put(((IDiagramModel)this.selectedObject).getId(), (IDiagramModel)this.selectedObject);
				this.model.countObject(this.selectedObject, true);
			} catch (Exception err) {
				DBGuiUtils.popup(Level.ERROR, "Failed to recalculate view chekcsum.", err);
				return;
			}
        }
        
        DBMetadata metadata = DBMetadata.getDBMetadata(this.selectedObject);
        
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
            metadata = DBMetadata.getDBMetadata(concept);
            
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
    
    @Override
    public void notConnectedToDatabase() {
        this.selectedComponentDebugTable.removeAll();
        this.correspondingConceptDebugTable.removeAll();
    }
}
