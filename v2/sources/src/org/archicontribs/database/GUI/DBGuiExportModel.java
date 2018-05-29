/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseExportConnection;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBVersionPair;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBDeleteDiagramConnectionCommand;
import org.archicontribs.database.model.DBDeleteDiagramObjectCommand;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.DBMetadata.CONFLICT_CHOICE;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.commands.DeleteArchimateElementCommand;
import com.archimatetool.editor.model.commands.DeleteArchimateRelationshipCommand;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;

/**
 * This class holds the methods requires to export a model in a database
 * 
 * @author Herve Jouin
 */
public class DBGuiExportModel extends DBGui {
	@SuppressWarnings("hiding")
	private static final DBLogger logger = new DBLogger(DBGuiExportModel.class);

	DBArchimateModel exportedModel = null;

	Group grpComponents;
	Group grpModelVersions;
	
	HashMap<String, DBVersionPair> newDatabaseComponents;
	
	private CompoundCommand delayedCommands;
	DBDatabaseExportConnection exportConnection;
	private boolean forceExport;
	
	/**
	 * Creates the GUI to export components and model
	 */
	public DBGuiExportModel(DBArchimateModel model, String title) throws Exception {
		// We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
		super(title);
		// We reference the exported model 
        this.exportedModel = model;
		this.includeNeo4j = true;
		
        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for exporting model \""+this.exportedModel.getName()+"\" (plugin version "+DBPlugin.pluginVersion+").");

		createGrpComponents();
		createGrpModel();
		this.compoRightBottom.setVisible(true);
		this.compoRightBottom.layout();

		createAction(ACTION.One, "1 - Confirm export");
		createAction(ACTION.Two, "2 - Export components");
		createAction(ACTION.Three, "3 - Status");

		// we show an arrow in front of the first action
		setActiveAction(ACTION.One);

		// if the user select the "Export" button --> call the exportComponents() method
		setBtnAction("Export", new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				export();
			}
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});

		// We rename the "close" button to "cancel"
		this.btnClose.setText("Cancel");

		// We activate the Eclipse Help framework
		setHelpHref("exportModel.html");
	}
	
	@Override
	public void run() {
	    super.run();
	    
	    refreshDisplay();
	    
        try {
    	    setMessage("Counting model's components");
            this.exportedModel.countAllObjects();
        } catch (Exception err) {
            popup(Level.ERROR, "Failed to count model's components", err);
            return;
        } finally {
            closeMessage();
        }
        
        if ( logger.isDebugEnabled() ) logger.debug("the model has got "+this.exportedModel.getAllElements().size()+" elements and "+this.exportedModel.getAllRelationships().size()+" relationships.");
        
        this.txtTotalElements.setText(String.valueOf(this.exportedModel.getAllElements().size()));
        this.txtTotalRelationships.setText(String.valueOf(this.exportedModel.getAllRelationships().size()));
        this.txtTotalFolders.setText(String.valueOf(this.exportedModel.getAllFolders().size()));
        this.txtTotalViews.setText(String.valueOf(this.exportedModel.getAllViews().size()));
        this.txtTotalViewObjects.setText(String.valueOf(this.exportedModel.getAllViewObjects().size()));
        this.txtTotalViewConnections.setText(String.valueOf(this.exportedModel.getAllViewConnections().size()));
        this.txtTotalImages.setText(String.valueOf(this.exportedModel.getAllImagePaths().size()));
        
        try {
            getDatabases(true);
        } catch (Exception err) {
            popup(Level.ERROR, "Failed to get the databases.", err);
            return;
        }
	}

	/**
	 * Creates a group displaying details about the the model in the database (list of existing versions)
	 */
	private void createGrpModel() {
		this.grpModelVersions = new Group(this.compoRightBottom, SWT.NONE);
		this.grpModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
		this.grpModelVersions.setText("Your model versions : ");
		this.grpModelVersions.setFont(GROUP_TITLE_FONT);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(this.grpComponents, -10);
		this.grpModelVersions.setLayoutData(fd);
		this.grpModelVersions.setLayout(new FormLayout());

		this.tblModelVersions = new Table(this.grpModelVersions,  SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		this.tblModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
		this.tblModelVersions.setLinesVisible(true);
		this.tblModelVersions.setHeaderVisible(true);
		this.tblModelVersions.addListener(SWT.Selection, new Listener() {
			@Override
            public void handleEvent(Event e) {
				boolean canExport = (DBGuiExportModel.this.tblModelVersions.getSelection() != null) && (DBGuiExportModel.this.tblModelVersions.getSelection().length > 0) && (DBGuiExportModel.this.tblModelVersions.getSelection()[0] != null);
				
				DBGuiExportModel.this.btnDoAction.setEnabled(canExport);
				DBGuiExportModel.this.btnCompareModelToDatabase.setEnabled(canExport);
				
				if ( canExport ) {
                    boolean canChangeMetaData = (DBGuiExportModel.this.exportConnection != null && DBGuiExportModel.this.selectedDatabase.isWholeModelExported() && (DBGuiExportModel.this.tblModelVersions.getSelection()[0] == DBGuiExportModel.this.tblModelVersions.getItem(0)));
                    
                    DBGuiExportModel.this.txtReleaseNote.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.txtPurpose.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.txtModelName.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.btnDoAction.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.btnCompareModelToDatabase.setEnabled(canChangeMetaData);
                    
					DBGuiExportModel.this.txtReleaseNote.setText((String) DBGuiExportModel.this.tblModelVersions.getSelection()[0].getData("note"));
					DBGuiExportModel.this.txtPurpose.setText((String) DBGuiExportModel.this.tblModelVersions.getSelection()[0].getData("purpose"));
					DBGuiExportModel.this.txtModelName.setText((String) DBGuiExportModel.this.tblModelVersions.getSelection()[0].getData("name"));
				}
			}
		});
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(40, -10);
		fd.bottom = new FormAttachment(100, -10);
		this.tblModelVersions.setLayoutData(fd);

		TableColumn colVersion = new TableColumn(this.tblModelVersions, SWT.NONE);
		colVersion.setText("#");
		colVersion.setWidth(20);

		TableColumn colCreatedOn = new TableColumn(this.tblModelVersions, SWT.NONE);
		colCreatedOn.setText("Created on");
		colCreatedOn.setWidth(120);

		TableColumn colCreatedBy = new TableColumn(this.tblModelVersions, SWT.NONE);
		colCreatedBy.setText("Created by");
		colCreatedBy.setWidth(125);

		Label lblModelName = new Label(this.grpModelVersions, SWT.NONE);
		lblModelName.setBackground(GROUP_BACKGROUND_COLOR);
		lblModelName.setText("Model name :");
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(40, 0);
		lblModelName.setLayoutData(fd);

		this.txtModelName = new Text(this.grpModelVersions, SWT.BORDER);
		this.txtModelName.setText(this.exportedModel.getName());
		this.txtModelName.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblModelName, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblModelName, 80, SWT.LEFT);
		fd.right = new FormAttachment(100, -10);
		this.txtModelName.setLayoutData(fd);

		Label lblPurpose = new Label(this.grpModelVersions, SWT.NONE);
		lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
		lblPurpose.setText("Purpose :");
		fd = new FormData();
		fd.top = new FormAttachment(this.txtModelName, 10);
		fd.left = new FormAttachment(lblModelName, 0, SWT.LEFT);
		lblPurpose.setLayoutData(fd);

		this.txtPurpose = new Text(this.grpModelVersions, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.txtPurpose.setText(this.exportedModel.getPurpose());
		this.txtPurpose.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.txtModelName, 5);
		fd.left = new FormAttachment(this.txtModelName, 0, SWT.LEFT);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(55, -5);
		this.txtPurpose.setLayoutData(fd);

		Label lblReleaseNote = new Label(this.grpModelVersions, SWT.NONE);
		lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		lblReleaseNote.setText("Release note :");
		fd = new FormData();
		fd.top = new FormAttachment(this.txtPurpose, 10);
		fd.left = new FormAttachment(lblPurpose, 0, SWT.LEFT);
		lblReleaseNote.setLayoutData(fd);

		this.txtReleaseNote = new Text(this.grpModelVersions, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		//txtReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		this.txtReleaseNote.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.txtPurpose, 5);
		fd.left = new FormAttachment(this.txtModelName, 0, SWT.LEFT);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		this.txtReleaseNote.setLayoutData(fd);
	}

	/**
	 * Creates a group displaying details about the exported model's components :<br>
	 * - total number<br>
	 * - number sync'ed with the database<br>
	 * - number that do not exist in the database<br>
	 * - number that exist in the database but with different values. 
	 */
    private void createGrpComponents() {
        this.grpComponents = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
        this.grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpComponents.setFont(GROUP_TITLE_FONT);
        this.grpComponents.setText("Your model's components : ");
        FormData fd = new FormData();
        fd.top = new FormAttachment(100, -240);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpComponents.setLayoutData(fd);
        this.grpComponents.setLayout(new FormLayout());

        Label lblElements = new Label(this.grpComponents, SWT.NONE);
        lblElements.setBackground(GROUP_BACKGROUND_COLOR);
        lblElements.setText("Elements :");
        fd = new FormData();
        fd.top = new FormAttachment(0, 25);
        fd.left = new FormAttachment(0, 30);
        lblElements.setLayoutData(fd);

        Label lblRelationships = new Label(this.grpComponents, SWT.NONE);
        lblRelationships.setBackground(GROUP_BACKGROUND_COLOR);
        lblRelationships.setText("Relationships :");
        fd = new FormData();
        fd.top = new FormAttachment(lblElements, 10);
        fd.left = new FormAttachment(0, 30);
        lblRelationships.setLayoutData(fd);

        Label lblFolders = new Label(this.grpComponents, SWT.NONE);
        lblFolders.setBackground(GROUP_BACKGROUND_COLOR);
        lblFolders.setText("Folders :");
        fd = new FormData();
        fd.top = new FormAttachment(lblRelationships, 10);
        fd.left = new FormAttachment(0, 30);
        lblFolders.setLayoutData(fd);

        Label lblViews = new Label(this.grpComponents, SWT.NONE);
        lblViews.setBackground(GROUP_BACKGROUND_COLOR);
        lblViews.setText("Views :");
        fd = new FormData();
        fd.top = new FormAttachment(lblFolders, 10);
        fd.left = new FormAttachment(0, 30);
        lblViews.setLayoutData(fd);

        Label lblViewObjects = new Label(this.grpComponents, SWT.NONE);
        lblViewObjects.setBackground(GROUP_BACKGROUND_COLOR);
        lblViewObjects.setText("Objects :");
        fd = new FormData();
        fd.top = new FormAttachment(lblViews, 10);
        fd.left = new FormAttachment(0, 30);
        lblViewObjects.setLayoutData(fd);

        Label lblViewConnections = new Label(this.grpComponents, SWT.NONE);
        lblViewConnections.setBackground(GROUP_BACKGROUND_COLOR);
        lblViewConnections.setText("Connections :");
        fd = new FormData();
        fd.top = new FormAttachment(lblViewObjects, 10);
        fd.left = new FormAttachment(0, 30);
        lblViewConnections.setLayoutData(fd);

        Label lblImages = new Label(this.grpComponents, SWT.NONE);
        lblImages.setBackground(GROUP_BACKGROUND_COLOR);
        lblImages.setText("Images :");
        fd = new FormData();
        fd.top = new FormAttachment(lblViewConnections, 10);
        fd.left = new FormAttachment(0, 30);
        lblImages.setLayoutData(fd);

        /* * * * * */

        this.lblTotal = new Label(this.grpComponents, SWT.CENTER);
        this.lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblTotal.setText("Total");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(20, 0);
        fd.right = new FormAttachment(28, 0);
        this.lblTotal.setLayoutData(fd);
        
        this.lblModel = new Label(this.grpComponents, SWT.CENTER);
        this.lblModel.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModel.setText("Archi");
        fd = new FormData();
        fd.top = new FormAttachment(0, -8);
        fd.left = new FormAttachment(33, 0);
        fd.right = new FormAttachment(57, 0);
        this.lblModel.setLayoutData(fd);
        
        this.lblModelNew = new Label(this.grpComponents, SWT.CENTER);
        this.lblModelNew.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModelNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(33, 0);
        fd.right = new FormAttachment(41, -2);
        this.lblModelNew.setLayoutData(fd);
        
        this.lblModelUpdated = new Label(this.grpComponents, SWT.CENTER);
        this.lblModelUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModelUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(41, 1);
        fd.right = new FormAttachment(49, -1);
        this.lblModelUpdated.setLayoutData(fd);
        
        this.lblModelDeleted = new Label(this.grpComponents, SWT.CENTER);
        this.lblModelDeleted.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModelDeleted.setText("Deleted");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(49, 2);
        fd.right = new FormAttachment(57, 0);
        this.lblModelDeleted.setLayoutData(fd);
        
        this.lblDatabase = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabase.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabase.setText("Database");
        fd = new FormData();
        fd.top = new FormAttachment(0, -8);
        fd.left = new FormAttachment(62, 0);
        fd.right = new FormAttachment(86, 0);
        this.lblDatabase.setLayoutData(fd);
        
        this.lblDatabaseNew = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabaseNew.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabaseNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(62, 0);
        fd.right = new FormAttachment(70, -2);
        this.lblDatabaseNew.setLayoutData(fd);
        
        this.lblDatabaseUpdated = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabaseUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabaseUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(70, 1);
        fd.right = new FormAttachment(78, -2);
        this.lblDatabaseUpdated.setLayoutData(fd);
        
        this.lblDatabaseDeleted = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabaseDeleted.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabaseDeleted.setText("Deleted");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(78, 1);
        fd.right = new FormAttachment(86, 0);
        this.lblDatabaseDeleted.setLayoutData(fd);
        
        this.lblConflicts = new Label(this.grpComponents, SWT.CENTER);
        this.lblConflicts.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblConflicts.setText("Conflicts");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(91, 0);
        fd.right = new FormAttachment(99, 0);
        this.lblConflicts.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalElements.setLayoutData(fd);

        this.txtNewElementsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewElementsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewElementsInModel.setLayoutData(fd);
        
        this.txtUpdatedElementsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedElementsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
        this.txtUpdatedElementsInModel.setLayoutData(fd);
        
        this.txtDeletedElementsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedElementsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
        this.txtDeletedElementsInModel.setLayoutData(fd);
        
        this.txtNewElementsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewElementsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewElementsInDatabase.setLayoutData(fd);
        
        this.txtUpdatedElementsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedElementsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
        this.txtUpdatedElementsInDatabase.setLayoutData(fd);
        
        this.txtDeletedElementsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedElementsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
        this.txtDeletedElementsInDatabase.setLayoutData(fd);
        
        this.txtConflictingElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
        this.txtConflictingElements.setLayoutData(fd);

        /* * * * * */
        
        this.txtTotalRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalRelationships.setLayoutData(fd);

        this.txtNewRelationshipsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewRelationshipsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewRelationshipsInModel.setLayoutData(fd);
        
        this.txtUpdatedRelationshipsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedRelationshipsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
        this.txtUpdatedRelationshipsInModel.setLayoutData(fd);
        
        this.txtDeletedRelationshipsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedRelationshipsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
        this.txtDeletedRelationshipsInModel.setLayoutData(fd);
        
        this.txtNewRelationshipsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewRelationshipsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewRelationshipsInDatabase.setLayoutData(fd);
        
        this.txtUpdatedRelationshipsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedRelationshipsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
        this.txtUpdatedRelationshipsInDatabase.setLayoutData(fd);
        
        this.txtDeletedRelationshipsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedRelationshipsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
        this.txtDeletedRelationshipsInDatabase.setLayoutData(fd);
        
        this.txtConflictingRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
        this.txtConflictingRelationships.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalFolders.setLayoutData(fd);

        this.txtNewFoldersInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewFoldersInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewFoldersInModel.setLayoutData(fd);
        
        this.txtUpdatedFoldersInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedFoldersInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
        this.txtUpdatedFoldersInModel.setLayoutData(fd);
        
        this.txtDeletedFoldersInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedFoldersInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
        this.txtDeletedFoldersInModel.setLayoutData(fd);
        
        this.txtNewFoldersInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewFoldersInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewFoldersInDatabase.setLayoutData(fd);
        
        this.txtUpdatedFoldersInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedFoldersInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
        this.txtUpdatedFoldersInDatabase.setLayoutData(fd);
        
        this.txtDeletedFoldersInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedFoldersInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
        this.txtDeletedFoldersInDatabase.setLayoutData(fd);
        
        this.txtConflictingFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
        this.txtConflictingFolders.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalViews.setLayoutData(fd);

        this.txtNewViewsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewViewsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewViewsInModel.setLayoutData(fd);
        
        this.txtUpdatedViewsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedViewsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
        this.txtUpdatedViewsInModel.setLayoutData(fd);
        
        this.txtDeletedViewsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedViewsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
        this.txtDeletedViewsInModel.setLayoutData(fd);
        
        this.txtNewViewsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewViewsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewViewsInDatabase.setLayoutData(fd);
        
        this.txtUpdatedViewsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedViewsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
        this.txtUpdatedViewsInDatabase.setLayoutData(fd);
        
        this.txtDeletedViewsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedViewsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
        this.txtDeletedViewsInDatabase.setLayoutData(fd);
        
        this.txtConflictingViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
        this.txtConflictingViews.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalViewObjects.setLayoutData(fd);
        
        this.txtNewViewObjectsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewViewObjectsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewViewObjectsInModel.setLayoutData(fd);
        
        this.txtUpdatedViewObjectsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedViewObjectsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
        this.txtUpdatedViewObjectsInModel.setLayoutData(fd);
        
        this.txtDeletedViewObjectsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedViewObjectsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
        this.txtDeletedViewObjectsInModel.setLayoutData(fd);
        
        this.txtNewViewObjectsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewViewObjectsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewViewObjectsInDatabase.setLayoutData(fd);
        
        this.txtUpdatedViewObjectsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedViewObjectsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
        this.txtUpdatedViewObjectsInDatabase.setLayoutData(fd);
        
        this.txtDeletedViewObjectsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedViewObjectsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
        this.txtDeletedViewObjectsInDatabase.setLayoutData(fd);
        
        this.txtConflictingViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
        this.txtConflictingViewObjects.setLayoutData(fd);
  
        /* * * * * */
        
        this.txtTotalViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalViewConnections.setLayoutData(fd);
        
        this.txtNewViewConnectionsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewViewConnectionsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewViewConnectionsInModel.setLayoutData(fd);
        
        this.txtUpdatedViewConnectionsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedViewConnectionsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
        this.txtUpdatedViewConnectionsInModel.setLayoutData(fd);
        
        this.txtDeletedViewConnectionsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedViewConnectionsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
        this.txtDeletedViewConnectionsInModel.setLayoutData(fd);
        
        this.txtNewViewConnectionsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewViewConnectionsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewViewConnectionsInDatabase.setLayoutData(fd);
        
        this.txtUpdatedViewConnectionsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtUpdatedViewConnectionsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
        this.txtUpdatedViewConnectionsInDatabase.setLayoutData(fd);
        
        this.txtDeletedViewConnectionsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtDeletedViewConnectionsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
        this.txtDeletedViewConnectionsInDatabase.setLayoutData(fd);
        
        this.txtConflictingViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
        this.txtConflictingViewConnections.setLayoutData(fd);

        /* * * * * */
        
        this.txtTotalImages = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalImages.setLayoutData(fd);
        
        this.txtNewImagesInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewImagesInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
        this.txtNewImagesInModel.setLayoutData(fd);
        
        this.txtNewImagesInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtNewImagesInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
        this.txtNewImagesInDatabase.setLayoutData(fd);
        
        /* * * * * */
        this.btnCompareModelToDatabase = new Button(this.grpComponents, SWT.WRAP);
        this.btnCompareModelToDatabase.setText("Compare model to\nthe database");
        fd = new FormData();
        fd.top = new FormAttachment(100, -40);
        fd.left = new FormAttachment(100, -120);
        fd.right = new FormAttachment(100, -5);
        fd.bottom = new FormAttachment(100, -5);
        this.btnCompareModelToDatabase.setLayoutData(fd);
        this.btnCompareModelToDatabase.addSelectionListener(new SelectionListener() {
            @Override public void widgetSelected(SelectionEvent e) {
                setMessage("Comparing model from the database...");
            	boolean upToDate = DBGuiExportModel.this.compareModelToDatabase();
            	closeMessage();
            	if ( upToDate ) {
            		popup(Level.INFO, "Your database is already up to date.");
                    DBGuiExportModel.this.btnClose.setText("Close");
            	}
            }
            @Override public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
        });
    }

	/**
	 * This method is called each time a database is selected and a connection has been established to it.<br>
	 * <br>
	 * It enables the export button and starts the export if the "automatic start" is specified in the plugin preferences
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheckDatabase) {
	    this.exportConnection = new DBDatabaseExportConnection(getDatabaseConnection());

		// we hide the comparison between the model and the database in case of a neo4j database
		boolean isNeo4j = DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j");
		this.lblModel.setVisible(!isNeo4j);
		this.lblModelNew.setVisible(!isNeo4j);
		this.lblModelDeleted.setVisible(!isNeo4j);
		this.lblModelUpdated.setVisible(!isNeo4j);
		this.txtNewElementsInModel.setVisible(!isNeo4j);
		this.txtUpdatedElementsInModel.setVisible(!isNeo4j);
		this.txtDeletedElementsInModel.setVisible(!isNeo4j);
		this.txtNewRelationshipsInModel.setVisible(!isNeo4j);
		this.txtUpdatedRelationshipsInModel.setVisible(!isNeo4j);
		this.txtDeletedRelationshipsInModel.setVisible(!isNeo4j);
		this.txtNewFoldersInModel.setVisible(!isNeo4j);
		this.txtUpdatedFoldersInModel.setVisible(!isNeo4j);
		this.txtDeletedFoldersInModel.setVisible(!isNeo4j);
		this.txtNewViewsInModel.setVisible(!isNeo4j);
		this.txtUpdatedViewsInModel.setVisible(!isNeo4j);
		this.txtDeletedViewsInModel.setVisible(!isNeo4j);
		this.txtNewViewObjectsInModel.setVisible(!isNeo4j);
        this.txtUpdatedViewObjectsInModel.setVisible(!isNeo4j);
        this.txtDeletedViewObjectsInModel.setVisible(!isNeo4j);
        this.txtNewViewConnectionsInModel.setVisible(!isNeo4j);
        this.txtUpdatedViewConnectionsInModel.setVisible(!isNeo4j);
        this.txtDeletedViewConnectionsInModel.setVisible(!isNeo4j);
		this.txtNewImagesInModel.setVisible(!isNeo4j);
		
        
        if ( this.exportedModel.getInitialVersion().getVersion() == 0 && this.selectedDatabase.isCollaborativeMode() && !DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
        	popup(Level.WARN, "You selected the collaborative mode in the preferences, but the model has not been imported from a database.\n\nWe temporarily switch to standalone mode.");
        	this.selectedDatabase.setCollaborativeMode(false);
        }
	      
        // we hide the database and conflict columns in standalone mode or Neo4J, and show them in collaborative mode
        this.lblDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblDatabaseNew.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblDatabaseDeleted.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblDatabaseUpdated.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblConflicts.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewElementsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedElementsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedElementsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingElements.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewRelationshipsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedRelationshipsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedRelationshipsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingRelationships.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewFoldersInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedFoldersInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedFoldersInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingFolders.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewViewsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedViewsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedViewsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingViews.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewViewObjectsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedViewObjectsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedViewObjectsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingViewObjects.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewViewConnectionsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedViewConnectionsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedViewConnectionsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingViewConnections.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewImagesInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
		
		// if we're not in a Neo4J database, then we get the latest version and checksum of the model's components in the database
		try {
			if ( !isNeo4j ) {
				DBGuiExportModel.this.tblModelVersions.removeAll();
				
               	// if the first line, then we add the "latest version"
				TableItem tableItem = new TableItem(DBGuiExportModel.this.tblModelVersions, SWT.NULL);
				tableItem.setText(1, "Now");
				tableItem.setData("name", this.exportedModel.getName());
				tableItem.setData("note", "");
				tableItem.setData("purpose", this.exportedModel.getPurpose());
				DBGuiExportModel.this.tblModelVersions.setSelection(tableItem);
				DBGuiExportModel.this.tblModelVersions.notifyListeners(SWT.Selection, new Event());		// activates the name, note and purpose texts
            	
				for (Hashtable<String, Object> version : DBGuiExportModel.this.exportConnection.getModelVersions(this.exportedModel.getId()) ) {
                	tableItem = new TableItem(DBGuiExportModel.this.tblModelVersions, SWT.NULL);
        			tableItem.setText(0, (String)version.get("version"));
        			tableItem.setText(1, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format((Timestamp)version.get("created_on")));
        			tableItem.setText(2, (String)version.get("created_by"));
        			tableItem.setData("name", version.get("name"));
        			tableItem.setData("note", version.get("note"));
        			tableItem.setData("purpose", version.get("purpose"));
                }
			}
		} catch (Exception err) {
			popup(Level.FATAL, "Failed to check existing components in database", err);
			setActiveAction(STATUS.Error);
			return;
		}
		
		if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("exportWithDefaultValues") ) {
		    // if the exportWithDefaultValues preference is set, then we automatically start the export
			logger.debug("Automatically start export as specified in preferences.");
			this.btnDoAction.notifyListeners(SWT.Selection, new Event());
			return;
		}
		
        this.btnCompareModelToDatabase.setVisible(!isNeo4j);
        
		if ( !isNeo4j && DBPlugin.INSTANCE.getPreferenceStore().getBoolean("compareBeforeExport") ) {
		    // if the compareBeforeExport is set
            setMessage("Comparing model from the database...");
        	boolean upToDate = DBGuiExportModel.this.compareModelToDatabase();
        	closeMessage();
        	if ( upToDate ) {
        		popup(Level.INFO, "Your database is already up to date.");
        		this.btnClose.setText("Close");
        	}
        } else {
            if ( logger.isDebugEnabled() ) logger.debug("Enabling the \"Export\" button.");
            this.btnDoAction.setEnabled(true);
            this.btnCompareModelToDatabase.setEnabled(true);
        }
	}
	
	/**
	 * This method is called each time a connection to the database fails.<br>
	 * <br>
	 * It disables the export button
	 */
	@Override
	protected void notConnectedToDatabase() {
		if ( logger.isDebugEnabled() ) logger.debug("Disabling the \"Export\" button.");
		this.btnDoAction.setEnabled(false);
		this.btnCompareModelToDatabase.setEnabled(false);
		
		// we hide the comparison between the model and the database in case of a neo4j database
        boolean isNeo4j = DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j");
        this.lblModel.setVisible(!isNeo4j);
        this.lblModelNew.setVisible(!isNeo4j);
        this.lblModelDeleted.setVisible(!isNeo4j);
        this.lblModelUpdated.setVisible(!isNeo4j);
        this.txtNewElementsInModel.setVisible(!isNeo4j);
        this.txtUpdatedElementsInModel.setVisible(!isNeo4j);
        this.txtDeletedElementsInModel.setVisible(!isNeo4j);
        this.txtNewRelationshipsInModel.setVisible(!isNeo4j);
        this.txtUpdatedRelationshipsInModel.setVisible(!isNeo4j);
        this.txtDeletedRelationshipsInModel.setVisible(!isNeo4j);
        this.txtNewFoldersInModel.setVisible(!isNeo4j);
        this.txtUpdatedFoldersInModel.setVisible(!isNeo4j);
        this.txtDeletedFoldersInModel.setVisible(!isNeo4j);
        this.txtNewViewsInModel.setVisible(!isNeo4j);
        this.txtUpdatedViewsInModel.setVisible(!isNeo4j);
        this.txtDeletedViewsInModel.setVisible(!isNeo4j);
        this.txtNewViewObjectsInModel.setVisible(!isNeo4j);
        this.txtUpdatedViewObjectsInModel.setVisible(!isNeo4j);
        this.txtDeletedViewObjectsInModel.setVisible(!isNeo4j);
        this.txtNewViewConnectionsInModel.setVisible(!isNeo4j);
        this.txtUpdatedViewConnectionsInModel.setVisible(!isNeo4j);
        this.txtDeletedViewConnectionsInModel.setVisible(!isNeo4j);
        this.txtNewImagesInModel.setVisible(!isNeo4j);

          
        // we hide the database and conflict columns in standalone mode or Neo4J, and show them in collaborative mode
        this.lblDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblDatabaseNew.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblDatabaseDeleted.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblDatabaseUpdated.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.lblConflicts.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewElementsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedElementsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedElementsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingElements.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewRelationshipsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedRelationshipsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedRelationshipsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingRelationships.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewFoldersInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedFoldersInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedFoldersInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingFolders.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewViewsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedViewsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedViewsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingViews.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewViewObjectsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedViewObjectsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedViewObjectsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingViewObjects.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewViewConnectionsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtUpdatedViewConnectionsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtDeletedViewConnectionsInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtConflictingViewConnections.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
        this.txtNewImagesInDatabase.setVisible(this.selectedDatabase.isCollaborativeMode() && !isNeo4j);
		
		this.txtNewElementsInModel.setText("");			this.txtUpdatedElementsInModel.setText("");			this.txtNewElementsInDatabase.setText("");			this.txtUpdatedElementsInDatabase.setText("");			this.txtConflictingElements.setText("");
		this.txtNewRelationshipsInModel.setText("");	this.txtUpdatedRelationshipsInModel.setText("");	this.txtNewRelationshipsInDatabase.setText("");		this.txtUpdatedRelationshipsInDatabase.setText("");		this.txtConflictingRelationships.setText("");
		this.txtNewFoldersInModel.setText("");			this.txtUpdatedFoldersInModel.setText("");			this.txtNewFoldersInDatabase.setText("");			this.txtUpdatedFoldersInDatabase.setText("");			this.txtConflictingFolders.setText("");
		this.txtNewViewsInModel.setText("");			this.txtUpdatedViewsInModel.setText("");			this.txtNewViewsInDatabase.setText("");				this.txtUpdatedViewsInDatabase.setText("");				this.txtConflictingViews.setText("");
		this.txtNewViewObjectsInModel.setText("");      this.txtUpdatedViewObjectsInModel.setText("");      this.txtNewViewObjectsInDatabase.setText("");       this.txtUpdatedViewObjectsInDatabase.setText("");       this.txtConflictingViewObjects.setText("");
		this.txtNewViewConnectionsInModel.setText("");  this.txtUpdatedViewConnectionsInModel.setText("");  this.txtNewViewConnectionsInDatabase.setText("");   this.txtUpdatedViewConnectionsInDatabase.setText("");   this.txtConflictingViewConnections.setText("");
		
		this.btnDoAction.setText("Export");
	}
	
	/**
	 * Update the text widgets that shows up the new, updated and deleted components
	 * 
	 * @return true is the database is up to date, false if the model needs to be exported
	 */
	protected boolean compareModelToDatabase() {
		// We do not verify the content of neo4j database, we just export the components
		if ( DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") )
			return true;
		
        try {
        	 this.exportConnection.getVersionsFromDatabase(this.exportedModel);
        } catch (SQLException err ) {
            popup(Level.FATAL, "Failed to get latest version of components in the database.", err);
            setActiveAction(STATUS.Error);
            doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
            return false;
        }
        
	    // we export the components without checking for conflicts in 4 cases:
        //    - the model is not in the database
        //    - the current model is the latest model in the database
        //    - we are in standalone mode
		//	  - we export to a Neo4j database
        this.forceExport = this.exportedModel.getInitialVersion().getVersion() == 0
                || this.exportedModel.getExportedVersion().getVersion() == this.exportedModel.getInitialVersion().getVersion()
                || !this.selectedDatabase.isCollaborativeMode()
                || DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j");
        
        if ( logger.isDebugEnabled() ) logger.debug("Calculating number of new, updated and deleted components (forceExport = "+this.forceExport+").");
        
        int nbNew = 0;
        int nbNewInDb = 0;
        int nbUpdated = 0;
        int nbUpdatedInDb = 0;
        int nbConflict = 0;
        int nbDeleted = 0;
        int nbDeletedInDb = 0;
        Iterator<Map.Entry<String, IArchimateElement>> ite = this.exportedModel.getAllElements().entrySet().iterator();
        while (ite.hasNext()) {
            IArchimateElement element = ite.next().getValue();
            DBMetadata metadata = ((IDBMetadata)element).getDBMetadata();
            if ( metadata.getDatabaseVersion().getVersion() == 0 )
                ++nbNew;                // if the component does not exist in the current database model, then it is a new component
            else {
                if ( metadata.getLatestDatabaseVersion().getVersion() == 0 )
                    ++nbDeletedInDb;    // if the component did exist, but does not exist anymore in the latest model, then it has been deleted by another user
                else if ( !DBPlugin.areEqual(metadata.getLatestDatabaseVersion().getChecksum(), metadata.getCurrentVersion().getChecksum()) ) {
                    boolean modifiedInModel = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getCurrentVersion().getChecksum());
                    boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getLatestDatabaseVersion().getChecksum());
                    
                    //TODO if ( DBPlugin.areEqual(((IDBMetadata)eObjectToExport).getDBMetadata().getCurrentVersion().getChecksum(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getChecksum()) ) {
		            //TODO if the modifications done on the component are the same between the model and the database, then we do not generate a conflict and simply ignore it
                    
                    if ( modifiedInModel && modifiedInDatabase ) {
                        if ( this.forceExport )     ++nbUpdated;
                        else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                    } else {
                        if ( modifiedInModel )      ++nbUpdated;
                        if ( modifiedInDatabase )   ++nbUpdatedInDb;
                    }
                }
                // else the component does not need to be exported
            }
        }
        // we distinguish the elements new in the database from those deleted from memory
        for ( DBVersionPair versionPair: this.exportConnection.getElementsNotInModel().values() ) {
            if ( versionPair.getCurrentVersion() == 0 )
                ++nbNewInDb;        // if the component does not exist in the database model, then it is a new one
            else
                ++nbDeleted;        // else, the component did exist in the model, but does not exist anymore, so it has been deleted
        }
        this.txtNewElementsInModel.setText(String.valueOf(nbNew));
        this.txtNewElementsInDatabase.setText(String.valueOf(nbNewInDb));
        this.txtUpdatedElementsInModel.setText(String.valueOf(nbUpdated));
        this.txtUpdatedElementsInDatabase.setText(String.valueOf(nbUpdatedInDb));
        this.txtConflictingElements.setText(String.valueOf(nbConflict));
        this.txtDeletedElementsInModel.setText(String.valueOf(nbDeleted));
        this.txtDeletedElementsInDatabase.setText(String.valueOf(nbDeletedInDb));
        
        
        nbNew = 0;
        nbNewInDb = 0;
        nbUpdated = 0;
        nbUpdatedInDb = 0;
        nbConflict = 0;
        nbDeleted = 0;
        nbDeletedInDb = 0;
        Iterator<Map.Entry<String, IArchimateRelationship>> itr = this.exportedModel.getAllRelationships().entrySet().iterator();
        while (itr.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
            if ( metadata.getDatabaseVersion().getVersion() == 0 ) {
                ++nbNew;                // if the component does not exist in the current database model, then it is a new component
            } else {
                if ( metadata.getLatestDatabaseVersion().getVersion() == 0 )
                    ++nbDeletedInDb;    // if the component did exist, but does not exist anymore, then it has been deleted by another user
                else if ( !DBPlugin.areEqual(metadata.getLatestDatabaseVersion().getChecksum(), metadata.getCurrentVersion().getChecksum()) ) {
                    boolean modifiedInModel = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getCurrentVersion().getChecksum());
                    boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getLatestDatabaseVersion().getChecksum());
                    
                    if ( modifiedInModel && modifiedInDatabase ) {
                        if ( this.forceExport )     ++nbUpdated;
                        else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                    } else {
                        if ( modifiedInModel )      ++nbUpdated;
                        if ( modifiedInDatabase )   ++nbUpdatedInDb;
                    }
                }
            }
        }
        // we distinguish the relationships new in the database from those deleted from memory
        for ( DBVersionPair versionPair: this.exportConnection.getRelationshipsNotInModel().values() ) {
            if ( versionPair.getCurrentVersion() == 0 )
                ++nbNewInDb;        // if the component does not exist in the database model, then it is a new one
            else
                ++nbDeleted;        // else, the component did exist in the model, but does not exist anymore, so it has been deleted
        }
        this.txtNewRelationshipsInModel.setText(String.valueOf(nbNew));
        this.txtNewRelationshipsInDatabase.setText(String.valueOf(nbNewInDb));
        this.txtUpdatedRelationshipsInModel.setText(String.valueOf(nbUpdated));
        this.txtUpdatedRelationshipsInDatabase.setText(String.valueOf(nbUpdatedInDb));
        this.txtConflictingRelationships.setText(String.valueOf(nbConflict));
        this.txtDeletedRelationshipsInModel.setText(String.valueOf(nbDeleted));
        this.txtDeletedRelationshipsInDatabase.setText(String.valueOf(nbDeletedInDb));
        
        nbNew = 0;
        nbNewInDb = 0;
        nbUpdated = 0;
        nbUpdatedInDb = 0;
        nbConflict = 0;
        nbDeleted = 0;
        nbDeletedInDb = 0;
        Iterator<Map.Entry<String, IFolder>> itf = this.exportedModel.getAllFolders().entrySet().iterator();
        while (itf.hasNext()) {
            IFolder tmp = itf.next().getValue();
            DBMetadata metadata = ((IDBMetadata)tmp).getDBMetadata();
            if ( metadata.getDatabaseVersion().getVersion() == 0 ) {
                ++nbNew;                // if the database version is zero, then the component is not in the database (therefore, new in the model)
            } else {
                if ( metadata.getLatestDatabaseVersion().getVersion() == 0 )
                    ++nbDeletedInDb;    // if the component did exist, but does not exist anymore, then it has been deleted by another user
                else if ( !DBPlugin.areEqual(metadata.getLatestDatabaseVersion().getChecksum(), metadata.getCurrentVersion().getChecksum()) ) {
                    boolean modifiedInModel = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getCurrentVersion().getChecksum());
                    boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getLatestDatabaseVersion().getChecksum());
                    
                    if ( modifiedInModel && modifiedInDatabase ) {
                        if ( this.forceExport )     ++nbUpdated;
                        else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                    } else {
                        if ( modifiedInModel )      ++nbUpdated;
                        if ( modifiedInDatabase )   ++nbUpdatedInDb;
                    }
                }
            }
        }
        // we distinguish the folders new in the database from those deleted from memory
        for ( DBVersionPair versionPair: this.exportConnection.getFoldersNotInModel().values() ) {
            if ( versionPair.getCurrentVersion() == 0 )
                ++nbNewInDb;        // if the component does not exist in the database model, then it is a new one
            else
                ++nbDeleted;        // else, the component did exist in the model, but does not exist anymore, so it has been deleted
        }
        this.txtNewFoldersInModel.setText(String.valueOf(nbNew));
        this.txtNewFoldersInDatabase.setText(String.valueOf(nbNewInDb));
        this.txtUpdatedFoldersInModel.setText(String.valueOf(nbUpdated));
        this.txtUpdatedFoldersInDatabase.setText(String.valueOf(nbUpdatedInDb));
        this.txtConflictingFolders.setText(String.valueOf(nbConflict));
        this.txtDeletedFoldersInModel.setText(String.valueOf(nbDeleted));
        this.txtDeletedFoldersInDatabase.setText(String.valueOf(nbDeletedInDb));
        
        nbNew = 0;
        nbNewInDb = 0;
        nbUpdated = 0;
        nbUpdatedInDb = 0;
        nbConflict = 0;
        nbDeleted = 0;
        nbDeletedInDb = 0;
        Iterator<Map.Entry<String, IDiagramModel>> itv = this.exportedModel.getAllViews().entrySet().iterator();
        while (itv.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
            if ( metadata.getDatabaseVersion().getVersion() == 0 ) {
                ++nbNew;                // if the database version is zero, then the component is not in the database (therefore, new in the model)
            } else {
                if ( metadata.getLatestDatabaseVersion().getVersion() == 0 )
                    ++nbDeletedInDb;    // if the component did exist, but does not exist anymore, then it has been deleted by another user
                else if ( !DBPlugin.areEqual(metadata.getLatestDatabaseVersion().getChecksum(), metadata.getCurrentVersion().getChecksum()) ) {
                    boolean modifiedInModel = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getCurrentVersion().getChecksum());
                    boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getLatestDatabaseVersion().getChecksum());
                    
                    if ( modifiedInModel && modifiedInDatabase ) {
                    	// if only the content has changed, then there is no conflict
                        if ( this.forceExport || DBPlugin.areEqual(metadata.getInitialVersion().getContainerChecksum(), metadata.getCurrentVersion().getContainerChecksum()) || DBPlugin.areEqual(metadata.getInitialVersion().getContainerChecksum(), metadata.getLatestDatabaseVersion().getContainerChecksum()) )
                        		                    ++nbUpdated;
                        else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                    } else {
                        if ( modifiedInModel )      ++nbUpdated;
                        if ( modifiedInDatabase )   ++nbUpdatedInDb;
                    }
                }
            }
        }
        // we distinguish the views new in the database from those deleted from memory
        for ( DBVersionPair versionPair: this.exportConnection.getViewsNotInModel().values() ) {
            if ( versionPair.getCurrentVersion() == 0 )
                ++nbNewInDb;        // if the component does not exist in the database model, then it is a new one
            else
                ++nbDeleted;        // else, the component did exist in the model, but does not exist anymore, so it has been deleted
        }
        this.txtNewViewsInModel.setText(String.valueOf(nbNew));
        this.txtNewViewsInDatabase.setText(String.valueOf(nbNewInDb));
        this.txtUpdatedViewsInModel.setText(String.valueOf(nbUpdated));
        this.txtUpdatedViewsInDatabase.setText(String.valueOf(nbUpdatedInDb));
        this.txtConflictingViews.setText(String.valueOf(nbConflict));
        this.txtDeletedViewsInModel.setText(String.valueOf(nbDeleted));
        this.txtDeletedViewsInDatabase.setText(String.valueOf(nbDeletedInDb));
        
        nbNew = 0;
        nbNewInDb = 0;
        nbUpdated = 0;
        nbUpdatedInDb = 0;
        nbConflict = 0;
        nbDeleted = 0;
        nbDeletedInDb = 0;
        Iterator<Map.Entry<String, IDiagramModelComponent>> ito = this.exportedModel.getAllViewObjects().entrySet().iterator();
        while (ito.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)ito.next().getValue()).getDBMetadata();
            if ( metadata.getDatabaseVersion().getVersion() == 0 ) {
                ++nbNew;                // if the database version is zero, then the component is not in the database (therefore, new in the model)
            } else {
                if ( metadata.getLatestDatabaseVersion().getVersion() == 0 )
                    ++nbDeletedInDb;    // if the component did exist, but does not exist anymore, then it has been deleted by another user
                else if ( !DBPlugin.areEqual(metadata.getLatestDatabaseVersion().getChecksum(), metadata.getCurrentVersion().getChecksum()) ) {
                    boolean modifiedInModel = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getCurrentVersion().getChecksum());
                    boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getLatestDatabaseVersion().getChecksum());
                    
                    if ( modifiedInModel && modifiedInDatabase ) {
                        if ( this.forceExport )     ++nbUpdated;
                        else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                    } else {
                        if ( modifiedInModel )      ++nbUpdated;
                        if ( modifiedInDatabase )   ++nbUpdatedInDb;
                    }
                }
            }
        }
        // we distinguish the viewObjects new in the database from those deleted from memory
        for ( DBVersionPair versionPair: this.exportConnection.getViewObjectsNotInModel().values() ) {
            if ( versionPair.getCurrentVersion() == 0 )
                ++nbNewInDb;        // if the component does not exist in the database model, then it is a new one
            else
                ++nbDeleted;        // else, the component did exist in the model, but does not exist anymore, so it has been deleted
        }
        this.txtNewViewObjectsInModel.setText(String.valueOf(nbNew));
        this.txtNewViewObjectsInDatabase.setText(String.valueOf(nbNewInDb));
        this.txtUpdatedViewObjectsInModel.setText(String.valueOf(nbUpdated));
        this.txtUpdatedViewObjectsInDatabase.setText(String.valueOf(nbUpdatedInDb));
        this.txtConflictingViewObjects.setText(String.valueOf(nbConflict));
        this.txtDeletedViewObjectsInModel.setText(String.valueOf(nbDeleted));
        this.txtDeletedViewObjectsInDatabase.setText(String.valueOf(nbDeletedInDb));
        
        nbNew = 0;
        nbNewInDb = 0;
        nbUpdated = 0;
        nbUpdatedInDb = 0;
        nbConflict = 0;
        nbDeleted = 0;
        nbDeletedInDb = 0;
        Iterator<Map.Entry<String, IDiagramModelConnection>> itc = this.exportedModel.getAllViewConnections().entrySet().iterator();
        while (itc.hasNext()) {
            DBMetadata metadata = ((IDBMetadata)itc.next().getValue()).getDBMetadata();
            if ( metadata.getDatabaseVersion().getVersion() == 0 ) {
                ++nbNew;                // if the database version is zero, then the component is not in the database (therefore, new in the model)
            } else {
                if ( metadata.getLatestDatabaseVersion().getVersion() == 0 )
                    ++nbDeletedInDb;    // if the component did exist, but does not exist anymore, then it has been deleted by another user
                else if ( !DBPlugin.areEqual(metadata.getLatestDatabaseVersion().getChecksum(), metadata.getCurrentVersion().getChecksum()) ) {
                    boolean modifiedInModel = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getCurrentVersion().getChecksum());
                    boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getInitialVersion().getChecksum(), metadata.getLatestDatabaseVersion().getChecksum());
                    
                    if ( modifiedInModel && modifiedInDatabase ) {
                        if ( this.forceExport )     ++nbUpdated;
                        else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                    } else {
                        if ( modifiedInModel )      ++nbUpdated;
                        if ( modifiedInDatabase )   ++nbUpdatedInDb;
                    }
                }
            }
        }
        // we distinguish the ViewConnections new in the database from those deleted from memory
        for ( DBVersionPair versionPair: this.exportConnection.getViewConnectionsNotInModel().values() ) {
            if ( versionPair.getCurrentVersion() == 0 )
                ++nbNewInDb;        // if the component does not exist in the database model, then it is a new one
            else
                ++nbDeleted;        // else, the component did exist in the model, but does not exist anymore, so it has been deleted
        }
        this.txtNewViewConnectionsInModel.setText(String.valueOf(nbNew));
        this.txtNewViewConnectionsInDatabase.setText(String.valueOf(nbNewInDb));
        this.txtUpdatedViewConnectionsInModel.setText(String.valueOf(nbUpdated));
        this.txtUpdatedViewConnectionsInDatabase.setText(String.valueOf(nbUpdatedInDb));
        this.txtConflictingViewConnections.setText(String.valueOf(nbConflict));
        this.txtDeletedViewConnectionsInModel.setText(String.valueOf(nbDeleted));
        this.txtDeletedViewConnectionsInDatabase.setText(String.valueOf(nbDeletedInDb));
        
        
        this.txtNewImagesInModel.setText(String.valueOf(this.exportConnection.getImagesNotInDatabase().size()));
        this.txtNewImagesInDatabase.setText(String.valueOf(this.exportConnection.getImagesNotInModel().size()));
        
        if ( this.txtNewElementsInModel.getText().equals("0") && this.txtNewRelationshipsInModel.getText().equals("0") && this.txtNewFoldersInModel.getText().equals("0") && this.txtNewViewsInModel.getText().equals("0") && this.txtNewViewObjectsInModel.getText().equals("0") && this.txtNewViewConnectionsInModel.getText().equals("0") &&
                this.txtUpdatedElementsInModel.getText().equals("0") && this.txtUpdatedRelationshipsInModel.getText().equals("0") && this.txtUpdatedFoldersInModel.getText().equals("0") && this.txtUpdatedViewsInModel.getText().equals("0") &&  this.txtUpdatedViewObjectsInModel.getText().equals("0") && this.txtUpdatedViewConnectionsInModel.getText().equals("0") &&
                this.txtNewElementsInDatabase.getText().equals("0") && this.txtNewRelationshipsInDatabase.getText().equals("0") && this.txtNewFoldersInDatabase.getText().equals("0") && this.txtNewViewsInDatabase.getText().equals("0") &&  this.txtNewViewObjectsInDatabase.getText().equals("0") && this.txtNewViewConnectionsInDatabase.getText().equals("0") &&
                this.txtUpdatedElementsInDatabase.getText().equals("0") && this.txtUpdatedRelationshipsInDatabase.getText().equals("0") && this.txtUpdatedFoldersInDatabase.getText().equals("0") && this.txtUpdatedViewsInDatabase.getText().equals("0") &&  this.txtUpdatedViewObjectsInDatabase.getText().equals("0") && this.txtUpdatedViewConnectionsInDatabase.getText().equals("0") &&
                this.txtConflictingElements.getText().equals("0") && this.txtConflictingRelationships.getText().equals("0") && this.txtConflictingFolders.getText().equals("0") && this.txtConflictingViews.getText().equals("0") && this.txtConflictingViewObjects.getText().equals("0") && this.txtConflictingViewConnections.getText().equals("0") ) {
            this.btnDoAction.setEnabled(false);
            this.btnDoAction.setText("Export");
            
            return true;
        }
        
        return false;
	}

	/**
	 * Loop on model components and call doExportEObject to export them<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button
	 */
	protected void export() {
		int progressBarWidth;
		if ( this.selectedDatabase.isWholeModelExported() ) {
			logger.info("Exporting model : "+this.exportedModel.getAllElements().size()+" elements, "+this.exportedModel.getAllRelationships().size()+" relationships, "+this.exportedModel.getAllFolders().size()+" folders, "+this.exportedModel.getAllViews().size()+" views, "+this.exportedModel.getAllViewObjects().size()+" views objects, "+this.exportedModel.getAllViewConnections().size()+" views connections, and "+((IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()+" images.");
			progressBarWidth = this.exportedModel.getAllFolders().size()+this.exportedModel.getAllElements().size()+this.exportedModel.getAllRelationships().size()+this.exportedModel.getAllViews().size()+this.exportedModel.getAllViewObjects().size()+this.exportedModel.getAllViewConnections().size()+((IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size();
		} else {
			logger.info("Exporting components : "+this.exportedModel.getAllElements().size()+" elements, "+this.exportedModel.getAllRelationships().size()+" relationships.");
			progressBarWidth = this.exportedModel.getAllElements().size()+this.exportedModel.getAllRelationships().size();
		}
		
		// we disable the export button to avoid a second click
		this.btnDoAction.setEnabled(false);
		this.btnCompareModelToDatabase.setEnabled(false);

		// we disable the option between an whole model export or a components only export
		disableOption();

		// the we disable the name, purpose and release note text fields
		this.txtModelName.setEnabled(false);
		this.txtPurpose.setEnabled(false);
		this.txtReleaseNote.setEnabled(false);

		// we force the modelVersion and component groups to be visible (in case we come from the conflict resolution)
		this.grpComponents.setVisible(true);
		this.grpModelVersions.setVisible(true);
		
		// We show up a small arrow in front of the second action "export components"
        setActiveAction(STATUS.Ok);
        setActiveAction(ACTION.Two);

		// We hide the grpDatabase and create a progressBar instead 
		hideGrpDatabase();
		createProgressBar("Exporting components ...", 0, progressBarWidth);
		createGrpConflict();
		
		// we calculate the new model checksum
		try {
            this.exportedModel.getExportedVersion().setChecksum(DBChecksum.calculateChecksum(this.exportedModel, this.txtReleaseNote.getText()));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException err) {
            popup(Level.FATAL, "Failed to calculate the model's checksum.", err);
            setActiveAction(STATUS.Error);
            doShowResult(STATUS.Error, "Failed to calculate the model's checksum.\n"+err.getMessage());
            return;
        }

		// then, we start a new database transaction
		try {
		    this.exportConnection.setAutoCommit(false);
		} catch (SQLException err ) {
			popup(Level.FATAL, "Failed to create a transaction in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return;
		}
		
		if ( !DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
			// we reset the counters as they will be updated by the doExportEObject method
		    this.txtNewElementsInModel.setText("0");         this.txtUpdatedElementsInModel.setText("0");         this.txtNewElementsInDatabase.setText("0");          this.txtUpdatedElementsInDatabase.setText("0");          this.txtConflictingElements.setText("0");
	        this.txtNewRelationshipsInModel.setText("0");    this.txtUpdatedRelationshipsInModel.setText("0");    this.txtNewRelationshipsInDatabase.setText("0");     this.txtUpdatedRelationshipsInDatabase.setText("0");     this.txtConflictingRelationships.setText("0");
	        this.txtNewFoldersInModel.setText("0");          this.txtUpdatedFoldersInModel.setText("0");          this.txtNewFoldersInDatabase.setText("0");           this.txtUpdatedFoldersInDatabase.setText("0");           this.txtConflictingFolders.setText("0");
	        this.txtNewViewsInModel.setText("0");            this.txtUpdatedViewsInModel.setText("0");            this.txtNewViewsInDatabase.setText("0");             this.txtUpdatedViewsInDatabase.setText("0");             this.txtConflictingViews.setText("0");
	        this.txtNewViewObjectsInModel.setText("0");      this.txtUpdatedViewObjectsInModel.setText("0");      this.txtNewViewObjectsInDatabase.setText("0");       this.txtUpdatedViewObjectsInDatabase.setText("0");       this.txtConflictingViewObjects.setText("0");
	        this.txtNewViewConnectionsInModel.setText("0");  this.txtUpdatedViewConnectionsInModel.setText("0");  this.txtNewViewConnectionsInDatabase.setText("0");   this.txtUpdatedViewConnectionsInDatabase.setText("0");   this.txtConflictingViewConnections.setText("0");
	        this.txtNewImagesInModel.setText("0");			 this.txtNewImagesInDatabase.setText("0");
	
			
			try {
				// we need to recalculate the latest versions in the database in case someone updated the database since the last check
				setMessage("Comparing model from the database...");
				this.exportConnection.getVersionsFromDatabase(this.exportedModel);
				closeMessage();
			} catch (SQLException err ) {
				closeMessage();
				popup(Level.FATAL, "Failed to get latest version of components in the database.", err);
				setActiveAction(STATUS.Error);
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
				return;
			}
		}
		
		// we initialize the deleteCommand
		this.delayedCommands = new CompoundCommand();
		
		// we export the components
		try ( DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(this.exportConnection) ) {
			// if we need to save the whole model (i.e. not only the elements and the relationships) 
			if ( this.selectedDatabase.isWholeModelExported() ) {
				// We update the model name and purpose in case they've been changed in the export windows
				if ( !DBPlugin.areEqual(this.exportedModel.getName(), this.txtModelName.getText()) )
					this.exportedModel.setName(this.txtModelName.getText());

				if ( !DBPlugin.areEqual(this.exportedModel.getPurpose(), this.txtPurpose.getText()) )
					this.exportedModel.setPurpose(this.txtPurpose.getText());

				this.exportConnection.exportModel(this.exportedModel, this.txtReleaseNote.getText());
	
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO : put the imported components in a compound Command to allow rollback
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				if ( logger.isDebugEnabled() ) logger.debug("Syncing/importing folders");
				for (String id : this.exportConnection.getFoldersNotInModel().keySet() ) {
				    DBVersionPair versionToImport = this.exportConnection.getFoldersNotInModel().get(id);
				    logger.trace("Must import folder "+id);
                    importConnection.importFolderFromId(this.exportedModel, id, versionToImport.getLatestVersion());
                    incrementText(this.txtNewFoldersInDatabase);
                    incrementText(this.txtTotalFolders);
				}
				if ( logger.isDebugEnabled() ) logger.debug("Exporting folders");
				Iterator<Entry<String, IFolder>> foldersIterator = this.exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) {
					doExportEObject(foldersIterator.next().getValue(), this.txtNewFoldersInModel, this.txtUpdatedFoldersInModel, this.txtUpdatedFoldersInDatabase, this.txtConflictingFolders);
				}
			}
			
			if ( DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") && this.selectedDatabase.shouldEmptyNeo4jDB() ) {
				this.exportConnection.emptyNeo4jDB();
			}
	
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO : put the imported components in a compound Command to allow rollback
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			if ( logger.isDebugEnabled() ) logger.debug("Syncing/importing elements");
			for (String id : this.exportConnection.getElementsNotInModel().keySet() ) {
			    DBVersionPair versionToImport = this.exportConnection.getElementsNotInModel().get(id);
		        if ( versionToImport.getCurrentVersion() == 0 ) {
		        	// if the element does not exist in the database model, then it is a new one, else, it has been deleted
		        	// TODO : if the element has been updated in the database, then generate a conflict
		        	logger.trace("Must import element "+id);
		        	importConnection.importElementFromId(this.exportedModel, id, versionToImport.getLatestVersion());
		        	incrementText(this.txtNewElementsInDatabase);
		        	incrementText(this.txtTotalElements);
		         }
			}
			if ( logger.isDebugEnabled() ) logger.debug("Exporting elements");
			Iterator<Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
			while ( elementsIterator.hasNext() ) {
				doExportEObject(elementsIterator.next().getValue(), this.txtNewElementsInModel, this.txtUpdatedElementsInModel, this.txtUpdatedElementsInDatabase, this.txtConflictingElements);
			}

			
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO : put the imported components in a compound Command to allow rollback
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			//TODO :
			if ( logger.isDebugEnabled() ) logger.debug("Syncing/importing relationships");
	        for (String id : this.exportConnection.getRelationshipsNotInModel().keySet() ) {
	            DBVersionPair versionToImport = this.exportConnection.getRelationshipsNotInModel().get(id);
	            if ( versionToImport.getCurrentVersion() == 0 ) {
		        	// if the relationship does not exist in the database model, then it is a new one, else, it has been deleted
		        	// TODO : if the relationship has been updated in the database, then generate a conflict
		        	logger.trace("Must import element "+id);
		        	importConnection.importRelationshipFromId(this.exportedModel, null, id, versionToImport.getLatestVersion(), false);
		        	incrementText(this.txtNewRelationshipsInDatabase);
		        	incrementText(this.txtTotalRelationships);
	            }
	        }
			if ( logger.isDebugEnabled() ) logger.debug("Exporting relationships");
			Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
			while ( relationshipsIterator.hasNext() ) {
				doExportEObject(relationshipsIterator.next().getValue(), this.txtNewRelationshipsInModel, this.txtUpdatedRelationshipsInModel, this.txtUpdatedRelationshipsInDatabase, this.txtConflictingRelationships);
			}
	        
            // if some elements or relationships must be removed from the model, we do it before exporting the views
			// as it will remove the view objects and connections at the same time
            if ( !this.delayedCommands.isEmpty() ) {
                // we remove the objects that have been removed by other users in the database
                CommandStack stack = (CommandStack) this.exportedModel.getAdapter(CommandStack.class);
                stack.execute(this.delayedCommands);
                
                // if some views are impacted by the elements and relationships removal, then we recalculate their checksum
                Iterator<Map.Entry<String, IDiagramModel>> itv = this.exportedModel.getAllViews().entrySet().iterator();
                while (itv.hasNext()) {
                    IDiagramModel view = itv.next().getValue();
                    if ( !((IDBMetadata)view).getDBMetadata().isChecksumValid() ) {
                        this.exportedModel.countObject(view, true, view);
                        this.exportConnection.getViewObjectsAndConnectionsVersionsFromDatabase(this.exportedModel, view);
                    }
                }
                
                // we refresh the text widgets
                compareModelToDatabase();
            }
	
			if ( this.selectedDatabase.isWholeModelExported() ) {
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO : put the imported components in a compound Command to allow rollback
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				if ( logger.isDebugEnabled() ) logger.debug("Syncing/importing views");
		        for (String id : this.exportConnection.getViewsNotInModel().keySet() ) {
		            DBVersionPair versionToImport = this.exportConnection.getViewsNotInModel().get(id);
		            if ( versionToImport.getCurrentVersion() == 0 ) {
			        	// if the relationship does not exist in the database model, then it is a new one, else, it has been deleted
			        	// TODO : if the relationship has been updated in the database, then generate a conflict
			        	logger.trace("Must import view "+id);
			        	importConnection.importViewFromId(this.exportedModel, id, versionToImport.getLatestVersion(), false, false);
			        	incrementText(this.txtNewViewsInDatabase);
			        	incrementText(this.txtTotalViews);
		            }
		        }
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views");
				Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue();
					doExportEObject(view, this.txtNewViewsInModel, this.txtUpdatedViewsInModel, this.txtUpdatedViewsInDatabase, this.txtConflictingViews);
				}
				
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO : put the imported components in a compound Command to allow rollback
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				if ( logger.isDebugEnabled() ) logger.debug("Syncing/importing views objects");
		        for (String id : this.exportConnection.getViewObjectsNotInModel().keySet() ) {
		            DBVersionPair versionToImport = this.exportConnection.getViewObjectsNotInModel().get(id);
		            if ( versionToImport.getCurrentVersion() == 0 ) {
			        	// if the relationship does not exist in the database model, then it is a new one, else, it has been deleted
			        	// TODO : if the relationship has been updated in the database, then generate a conflict
			        	logger.trace("Must import view object "+id);
			        	importConnection.importViewObjectFromId(this.exportedModel, id, versionToImport.getLatestVersion(), false);
			        	incrementText(this.txtNewViewObjectsInDatabase);
			        	incrementText(this.txtTotalViewObjects);
		            }
		        }
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views objects");
				Iterator<Entry<String, IDiagramModelComponent>> viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
				while ( viewObjectsIterator.hasNext() ) {
					IDiagramModelComponent viewObject = viewObjectsIterator.next().getValue();
					doExportEObject(viewObject, this.txtNewViewObjectsInModel, this.txtUpdatedViewObjectsInModel, this.txtUpdatedViewObjectsInDatabase, this.txtConflictingViewObjects);
				}
				
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO : put the imported components in a compound Command to allow rollback
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				//TODO :
				if ( logger.isDebugEnabled() ) logger.debug("Syncing/importing views connections");
		        for (String id : this.exportConnection.getViewConnectionsNotInModel().keySet() ) {
		            DBVersionPair versionToImport = this.exportConnection.getViewConnectionsNotInModel().get(id);
		            if ( versionToImport.getCurrentVersion() == 0 ) {
			        	// if the relationship does not exist in the database model, then it is a new one, else, it has been deleted
			        	// TODO : if the relationship has been updated in the database, then generate a conflict
			        	logger.trace("Must import view connection "+id);
			        	importConnection.importViewConnectionFromId(this.exportedModel, id, versionToImport.getLatestVersion(), false);
			        	incrementText(this.txtNewViewConnectionsInDatabase);
			        	incrementText(this.txtTotalViewConnections);
		            }
		        }
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views connections");
				Iterator<Entry<String, IDiagramModelConnection>> viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
				while ( viewConnectionsIterator.hasNext() ) {
					IDiagramModelConnection viewConnection = viewConnectionsIterator.next().getValue();
					doExportEObject(viewConnection, this.txtNewViewConnectionsInModel, this.txtUpdatedViewConnectionsInModel, this.txtUpdatedViewConnectionsInDatabase, this.txtConflictingViewConnections);
				}
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting images");
				
				// no need to use imagesNotInModel as the requested images have been imported at the same time as their view object
				
		    	IArchiveManager archiveMgr = (IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class);
				for ( String path: this.exportedModel.getAllImagePaths() ) {
					if ( this.exportConnection.exportImage(path, archiveMgr.getBytesFromEntry(path)) )
						incrementText(this.txtNewImagesInModel);
				}
			}
		} catch (Exception err) {
            if ( hasBeenClosed() )
                popup(Level.WARN, "The export has been cancelled.");
            else { 
    			setActiveAction(STATUS.Error);
    			popup(Level.FATAL, "An error occurred while exporting the components.\n\nThe transaction will be rolled back to leave the database in a coherent state. You may solve the issue and export again your components.", err);
    			try  {
    			    this.exportConnection.rollback();
    				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
    			} catch (SQLException err2) {
    				popup(Level.FATAL, "The transaction failed to rollback and the database is left in an inconsistent state.\n\nPlease check carrefully your database !", err2);
    				doShowResult(STATUS.Error, "Error while exporting model.\n"+err2.getMessage());
    			}
            }
            return;
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Found "+this.tblListConflicts.getItemCount()+" components conflicting with database");
		if ( this.tblListConflicts.getItemCount() == 0 ) {
			// the export is successful
			try  {
				// we check if something has been really exported				
				if ( this.selectedDatabase.isWholeModelExported() ) {
			        if ( this.txtNewElementsInModel.getText().equals("0") && this.txtNewRelationshipsInModel.getText().equals("0") && this.txtNewFoldersInModel.getText().equals("0") && this.txtNewViewsInModel.getText().equals("0") && this.txtNewViewObjectsInModel.getText().equals("0") && this.txtNewViewConnectionsInModel.getText().equals("0") &&
			                this.txtUpdatedElementsInModel.getText().equals("0") && this.txtUpdatedRelationshipsInModel.getText().equals("0") && this.txtUpdatedFoldersInModel.getText().equals("0") && this.txtUpdatedViewsInModel.getText().equals("0") && this.txtUpdatedViewObjectsInModel.getText().equals("0") && this.txtUpdatedViewConnectionsInModel.getText().equals("0") &&
			                this.txtNewElementsInDatabase.getText().equals("0") && this.txtNewRelationshipsInDatabase.getText().equals("0") && this.txtNewFoldersInDatabase.getText().equals("0") && this.txtNewViewsInDatabase.getText().equals("0") && this.txtNewViewObjectsInDatabase.getText().equals("0") && this.txtNewViewConnectionsInDatabase.getText().equals("0") &&
			                this.txtUpdatedElementsInDatabase.getText().equals("0") && this.txtUpdatedRelationshipsInDatabase.getText().equals("0") && this.txtUpdatedFoldersInDatabase.getText().equals("0") && this.txtUpdatedViewsInDatabase.getText().equals("0") && this.txtUpdatedViewObjectsInDatabase.getText().equals("0") && this.txtUpdatedViewConnectionsInDatabase.getText().equals("0") &&
			                this.txtConflictingElements.getText().equals("0") && this.txtConflictingRelationships.getText().equals("0") && this.txtConflictingFolders.getText().equals("0") && this.txtConflictingViews.getText().equals("0") && this.txtConflictingViewObjects.getText().equals("0") && this.txtConflictingViewConnections.getText().equals("0") &&
							this.exportedModel.getExportedVersion().getChecksum().equals(this.exportedModel.getInitialVersion().getChecksum()) ) {
						this.exportConnection.rollback();
					    this.exportConnection.setAutoCommit(true);
						setActiveAction(STATUS.Ok);
						doShowResult(STATUS.Ok, "Nothing has been exported as the database is already up to date.");
						return;
					}
				}
				
			    this.exportConnection.commit();
			    this.exportConnection.setAutoCommit(true);
				setActiveAction(STATUS.Ok);
				
				// Once the export is finished, we copy the exportedVersion to the currentVersion
				copyExportedVersionToCurrentVersion();
				
				doShowResult(STATUS.Ok, "Export successful");
				
				return;
			} catch (Exception err) {
				popup(Level.FATAL, "Failed to commit the transaction. Please check carrefully your database !", err);
				setActiveAction(STATUS.Error);
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
				return;
			}
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Export of components incomplete. Conflicts need to be manually resolved.");
		resetProgressBar();
		try  {
		    this.exportConnection.rollback();
		} catch (Exception err) {
			popup(Level.FATAL, "Failed to rollback the transaction. Please check carrefully your database !", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return;
		}
	
		this.tblListConflicts.setSelection(0);
		try {
			this.tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
		} catch (Exception err) {
			popup(Level.ERROR, "Failed to compare component with its database version.", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return;
		}
	}
	
	void copyExportedVersionToCurrentVersion() {
		if ( logger.isDebugEnabled() ) logger.debug("updating current versions from exported versions");
		
	    this.exportedModel.getInitialVersion().setVersion(this.exportedModel.getExportedVersion().getVersion());
        this.exportedModel.getInitialVersion().setChecksum(this.exportedModel.getExportedVersion().getChecksum());
        this.exportedModel.getInitialVersion().setTimestamp(this.exportedModel.getExportedVersion().getTimestamp());
        
        Iterator<Map.Entry<String, IArchimateElement>> ite = this.exportedModel.getAllElements().entrySet().iterator();
        while (ite.hasNext()) {
            DBMetadata dbMetadata = ((IDBMetadata)ite.next().getValue()).getDBMetadata();
            dbMetadata.getInitialVersion().setVersion(dbMetadata.getCurrentVersion().getVersion());
            dbMetadata.getInitialVersion().setChecksum(dbMetadata.getCurrentVersion().getChecksum());
            dbMetadata.getInitialVersion().setTimestamp(this.exportedModel.getExportedVersion().getTimestamp());
        }
        
        Iterator<Map.Entry<String, IArchimateRelationship>> itr = this.exportedModel.getAllRelationships().entrySet().iterator();
        while (itr.hasNext()) {
            DBMetadata dbMetadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
            dbMetadata.getInitialVersion().setVersion(dbMetadata.getCurrentVersion().getVersion());
            dbMetadata.getInitialVersion().setChecksum(dbMetadata.getCurrentVersion().getChecksum());
            dbMetadata.getInitialVersion().setTimestamp(this.exportedModel.getExportedVersion().getTimestamp());
        }
        
        Iterator<Map.Entry<String, IFolder>> itf = this.exportedModel.getAllFolders().entrySet().iterator();
        while (itf.hasNext()) {
            DBMetadata dbMetadata = ((IDBMetadata)itf.next().getValue()).getDBMetadata();
            dbMetadata.getInitialVersion().setVersion(dbMetadata.getCurrentVersion().getVersion());
            dbMetadata.getInitialVersion().setChecksum(dbMetadata.getCurrentVersion().getChecksum());
            dbMetadata.getInitialVersion().setTimestamp(this.exportedModel.getExportedVersion().getTimestamp());
        }
        
        Iterator<Map.Entry<String, IDiagramModel>> itv = this.exportedModel.getAllViews().entrySet().iterator();
        while (itv.hasNext()) {
            DBMetadata dbMetadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
            dbMetadata.getInitialVersion().setVersion(dbMetadata.getCurrentVersion().getVersion());
            dbMetadata.getInitialVersion().setChecksum(dbMetadata.getCurrentVersion().getChecksum());
            dbMetadata.getInitialVersion().setTimestamp(this.exportedModel.getExportedVersion().getTimestamp());
        }
        
        Iterator<Map.Entry<String, IDiagramModelComponent>> ito = this.exportedModel.getAllViewObjects().entrySet().iterator();
        while (ito.hasNext()) {
            DBMetadata dbMetadata = ((IDBMetadata)ito.next().getValue()).getDBMetadata();
            dbMetadata.getInitialVersion().setVersion(dbMetadata.getCurrentVersion().getVersion());
            dbMetadata.getInitialVersion().setChecksum(dbMetadata.getCurrentVersion().getChecksum());
        }
        
        Iterator<Map.Entry<String, IDiagramModelConnection>> itc = this.exportedModel.getAllViewConnections().entrySet().iterator();
        while (itc.hasNext()) {
            DBMetadata dbMetadata = ((IDBMetadata)itc.next().getValue()).getDBMetadata();
            dbMetadata.getInitialVersion().setVersion(dbMetadata.getCurrentVersion().getVersion());
            dbMetadata.getInitialVersion().setChecksum(dbMetadata.getCurrentVersion().getChecksum());
        }
	}
	
	/**
	 * Effectively exports an EObject in the database<br>
	 * When a conflict is detected, the component ID is added to the tblListConflicts<br>
	 * <br>
	 * This method is called by the export() method
	 * @return true if the EObject has been exported, false if it is conflicting
	 */
	private boolean doExportEObject(EObject eObject, Text txtNewInModel, Text txtUpdatedInModel, Text txtUpdatedInDatabase, Text txtConflicting) throws Exception {
		assert(eObject instanceof IDBMetadata);
		assert(this.exportConnection != null);
		
		boolean mustExport = false;
		boolean mustImport = false;
		boolean mustDelete = false;
		boolean exported = false;
		
		EObject eObjectToExport = eObject;
		if ( DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
		    // in Neo4J databases, we do not manage versions so we export all the elements and all the relationships
		    mustExport = true;
		} else {
		    // but in SQL databases, we need to calculate the component version and check if there is no conflict in the database

		    if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion() == 0 ) {
                // if the component is not present in the latest version of the model in the database
		        if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getVersion() != 0 ) {
		            // but was present in the current database model, the the element has been deleted in the database
		            mustDelete = true;
		        } else {
		            // else, it is a new component that needs to be exported
	                mustExport = true;
		        }
            } else {
            	String initialChecksum = ((IDBMetadata)eObjectToExport).getDBMetadata().getInitialVersion().getChecksum();
            	String currentChecksum = ((IDBMetadata)eObjectToExport).getDBMetadata().getCurrentVersion().getChecksum();
            	String latestDatabaseChecksum = ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getChecksum();

                if ( DBPlugin.areEqual(latestDatabaseChecksum, currentChecksum) ) {
                    // if the checksum of the latest version in the database equals the latest checksum
                    // then the database is up to date and the component does not need to be exported
                } else {
                    // else, the component is different between the model and the database
        			boolean modifiedInModel = !DBPlugin.areEqual(initialChecksum, currentChecksum);
        			boolean modifiedInDatabase = !DBPlugin.areEqual(initialChecksum, latestDatabaseChecksum);
        			
        			if ( modifiedInModel ) {
        			    if ( modifiedInDatabase ) {
            				// if the component has been updated in both the model and the database, there might be a conflict
        			        if ( DBPlugin.areEqual(currentChecksum, latestDatabaseChecksum) ) {
        			            // the modifications done on the component are the same between the model and the database, then we do not generate a conflict and simply ignore it
        			            mustExport = false;
            			    } else {
                				// We must manage the conflict, except if we are force the export
                				if ( this.forceExport )
                    		        mustExport = true;
                				else {
                					if ( logger.isDebugEnabled() ) logger.debug("The component conflicts with the version in the database.");
                					switch ( ((IDBMetadata)eObjectToExport).getDBMetadata().getConflictChoice() ) {
                						case askUser :
                							if ( logger.isDebugEnabled() ) logger.debug("The conflict has to be manually resolved by user.");
                	                    	new TableItem(this.tblListConflicts, SWT.NONE).setText(((IIdentifier)eObjectToExport).getId());
                	                    	if ( this.tblListConflicts.getItemCount() < 2 )
                	                    		this.lblCantExport.setText("Can't export because "+this.tblListConflicts.getItemCount()+" component conflicts with newer version in the database :");
                	                    	else
                	                    		this.lblCantExport.setText("Can't export because "+this.tblListConflicts.getItemCount()+" components conflict with newer version in the database :");
                	                    	incrementText(txtConflicting);
                	                    	break;
                						case exportToDatabase :
                		                    if ( logger.isDebugEnabled() ) logger.debug("The component is tagged to force export to the database. ");
                		                    mustExport = true;
                		                    break;
                						case importFromDatabase :
                		                    if ( logger.isDebugEnabled() ) logger.debug("The component is tagged \"import the database version\".");
                		                    mustImport = true;
                		                    break;
                						case doNotExport :
                		                    if ( logger.isDebugEnabled() ) logger.debug("The component is tagged \"do not export\", so we keep it as it is.");
                		                    break;
        								default:
        									break;
                					}
                				}
            			    }
        			    } else {
        			        // if the component has been modified in the model but not in the database, then we must export it
                            mustExport = true;
        			    }
        			} else {
        			    if ( modifiedInDatabase ) {
        			        // if the component has been modified in the database but not in the model, then we must import it
        			        mustImport = true;
                        } else {
                            // if the component has not been modified in the model nor in the database, then we do not export nor import it
                            mustExport = false;
                        }
        			}
                }
            }
		}
	            
		if ( mustExport ) {
		    if ( logger.isDebugEnabled() ) {
		    	String objectClass = "Unknown";
		        if ( eObjectToExport instanceof IArchimateElement )
		            objectClass = "Element";
		        else if ( eObjectToExport instanceof IArchimateRelationship )
		        	objectClass = "Relationship";
		        else if ( eObjectToExport instanceof IFolder )
		        	objectClass = "Folder";
		        else if ( eObjectToExport instanceof IDiagramModel )
		        	objectClass = "View";
		        else if ( eObjectToExport instanceof IDiagramModelComponent )
		        	objectClass = "View Object";
		        else if ( eObjectToExport instanceof IDiagramModelConnection )
		        	objectClass = "View Connection";
                logger.debug(objectClass+" id "+((IIdentifier)eObjectToExport).getId()+" has been updated in Archi, we must export it");
		    }
			this.exportConnection.exportEObject(eObjectToExport, this);
            if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion() == 0 )
            	incrementText(txtNewInModel);
            else
                incrementText(txtUpdatedInModel);
            exported = true;
		}
		
		if ( mustImport ) {
            // For the moment, we can import elements and relationships only during an export !!!
			try ( DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(this.exportConnection) ) {
	            if ( eObjectToExport instanceof IArchimateElement ) {
	                if ( logger.isTraceEnabled() ) logger.trace("Element id "+((IIdentifier)eObjectToExport).getId()+" has been updated in the database, we must import it");
	                importConnection.importElementFromId(this.exportedModel, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion());
	                incrementText(txtUpdatedInDatabase);
	            } else if ( eObjectToExport instanceof IArchimateRelationship ) {
	                if ( logger.isTraceEnabled() ) logger.trace("Relationshipd id "+((IIdentifier)eObjectToExport).getId()+" has been updated in the database, we must import it");
	                importConnection.importRelationshipFromId(this.exportedModel, null, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion(), false);
	                incrementText(txtUpdatedInDatabase);
	            } else if ( eObjectToExport instanceof IFolder ) {
	                if ( logger.isTraceEnabled() ) logger.trace("Folder id "+((IIdentifier)eObjectToExport).getId()+" has been updated in the database, we must import it");
	                importConnection.importFolderFromId(this.exportedModel, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion(), false);
	                incrementText(txtUpdatedInDatabase);
	            } else if ( eObjectToExport instanceof IDiagramModel ) {
	                if ( logger.isTraceEnabled() ) logger.trace("View id "+((IIdentifier)eObjectToExport).getId()+" has been updated in the database, we must import it");
	                importConnection.importViewFromId(this.exportedModel, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion(), false, false);
	                incrementText(txtUpdatedInDatabase);
	            } else if ( eObjectToExport instanceof IDiagramModelComponent ) {
	                if ( logger.isTraceEnabled() ) logger.trace("View Object id "+((IIdentifier)eObjectToExport).getId()+" has been updated in the database, we must import it");
	                importConnection.importViewObjectFromId(this.exportedModel, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion(), false);
	                incrementText(txtUpdatedInDatabase);
	            } else if ( eObjectToExport instanceof IDiagramModelConnection ) {
	                if ( logger.isTraceEnabled() ) logger.trace("View Connection id "+((IIdentifier)eObjectToExport).getId()+" has been updated in the database, we must import it");
	                importConnection.importViewConnectionFromId(this.exportedModel, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getLatestDatabaseVersion().getVersion(), false);
	                incrementText(txtUpdatedInDatabase);
	            } else {
	            	// should not be here !
	            	throw new Exception("At the moment, we cannot import a "+eObjectToExport.getClass().getSimpleName()+" during the export process :(");
	            }
	            exported = true;
			}
		}
		
		if ( mustDelete ) {
		    if ( eObjectToExport instanceof IArchimateElement ) {
		        if ( logger.isTraceEnabled() ) logger.trace("Element id "+((IIdentifier)eObjectToExport).getId()+" has been deleted in the database. We delete it in the model.");
		        for ( IDiagramModelArchimateObject obj : ((IArchimateElement)eObjectToExport).getReferencingDiagramObjects() )
		            this.delayedCommands.add(new DBDeleteDiagramObjectCommand(obj));
		        this.delayedCommands.add(new DeleteArchimateElementCommand((IArchimateElement)eObjectToExport));
		    } else if ( eObjectToExport instanceof IArchimateRelationship ) {
                if ( logger.isTraceEnabled() ) logger.trace("Element id "+((IIdentifier)eObjectToExport).getId()+" has been deleted in the database. We delete it in the model.");
                for ( IDiagramModelArchimateConnection obj : ((IArchimateRelationship)eObjectToExport).getReferencingDiagramConnections() )
                    this.delayedCommands.add(new DBDeleteDiagramConnectionCommand(obj));
                this.delayedCommands.add(new DeleteArchimateRelationshipCommand((IArchimateRelationship)eObjectToExport));
            } else if ( eObjectToExport instanceof IDiagramModelArchimateObject ) {
		        if ( logger.isTraceEnabled() ) logger.trace("view object id "+((IIdentifier)eObjectToExport).getId()+" has been deleted in the database. We delete it in the model.");
		        new DBDeleteDiagramObjectCommand((IDiagramModelArchimateObject)eObjectToExport).execute();
            } else if ( eObjectToExport instanceof IDiagramModelArchimateConnection ) {
		        if ( logger.isTraceEnabled() ) logger.trace("view object id "+((IIdentifier)eObjectToExport).getId()+" has been deleted in the database. We delete it in the model.");
		        new DBDeleteDiagramConnectionCommand((IDiagramModelArchimateConnection)eObjectToExport).execute();
		    } else
                logger.error("At the moment, we cannot delete a "+eObjectToExport.getClass().getSimpleName()+" during the export process :(");
		    // TODO : delete views (including content)
		    // TODO : delete folders (including sub folders, but check before if they are empty) 
            exported = true;
		} else {
		    // even if the eObject is not exported, it has to be referenced as being part of the model
		    if ( this.selectedDatabase.isWholeModelExported() )
		        this.exportConnection.assignEObjectToModel(eObjectToExport);
		}
		
		increaseProgressBar();
		return exported;
	}

	/**
	 * Creates a group that will display the conflicts raised during the export process
	 */
	protected void createGrpConflict() {		
		if ( this.grpConflict == null ) {
			this.grpConflict = new Group(this.compoRightBottom, SWT.NONE);
			this.grpConflict.setBackground(GROUP_BACKGROUND_COLOR);
			this.grpConflict.setFont(TITLE_FONT);
			this.grpConflict.setText("Conflict : ");
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			fd.bottom = new FormAttachment(100);
			this.grpConflict.setLayoutData(fd);
			this.grpConflict.setLayout(new FormLayout());
	
			this.lblCantExport = new Label(this.grpConflict, SWT.NONE);
			this.lblCantExport.setBackground(GROUP_BACKGROUND_COLOR);
			this.lblCantExport.setText("Can't export because some components conflict with newer version in the database :");
			fd = new FormData();
			fd.top = new FormAttachment(0, 10);
			fd.left = new FormAttachment(0, 10);
			this.lblCantExport.setLayoutData(fd);
	
			this.tblListConflicts = new Table(this.grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
			this.tblListConflicts.setLinesVisible(true);
			this.tblListConflicts.setBackground(GROUP_BACKGROUND_COLOR);
			this.tblListConflicts.addListener(SWT.Selection, new Listener() {
				@Override
                public void handleEvent(Event event) {
					// we search for the component that is conflicting
					String id = DBGuiExportModel.this.tblListConflicts.getSelection()[0].getText();
	
					EObject conflictingComponent = DBGuiExportModel.this.exportedModel.getAllElements().get(id);
					if ( conflictingComponent == null ) conflictingComponent = DBGuiExportModel.this.exportedModel.getAllRelationships().get(id);
					if ( conflictingComponent == null ) conflictingComponent = DBGuiExportModel.this.exportedModel.getAllFolders().get(id);
					if ( conflictingComponent == null ) conflictingComponent = DBGuiExportModel.this.exportedModel.getAllViews().get(id);
					if ( conflictingComponent == null ) conflictingComponent = DBGuiExportModel.this.exportedModel.getAllViewObjects().get(id);
					if ( conflictingComponent == null ) conflictingComponent = DBGuiExportModel.this.exportedModel.getAllViewConnections().get(id);
	
					if ( conflictingComponent == null ) {
						DBGuiExportModel.this.btnExportMyVersion.setEnabled(false);
						DBGuiExportModel.this.btnDoNotExport.setEnabled(false);
						DBGuiExportModel.this.btnImportDatabaseVersion.setEnabled(false);
						DBGuiExportModel.this.tblCompareComponent.removeAll();
						popup(Level.ERROR, "Do not know which component is conflicting !!! That's weird !!!");
					} else {				
						DBGuiExportModel.this.btnExportMyVersion.setEnabled(true);
						DBGuiExportModel.this.btnDoNotExport.setEnabled(true);
						DBGuiExportModel.this.btnImportDatabaseVersion.setEnabled( (conflictingComponent instanceof IArchimateElement) || (conflictingComponent instanceof IArchimateRelationship) );
	
						fillInCompareTable(DBGuiExportModel.this.tblCompareComponent, conflictingComponent, ((IDBMetadata)conflictingComponent).getDBMetadata().getLatestDatabaseVersion().getVersion());
					}
					DBGuiExportModel.this.grpComponents.setVisible(false);
					DBGuiExportModel.this.grpModelVersions.setVisible(false);
					DBGuiExportModel.this.grpConflict.setVisible(true);
					DBGuiExportModel.this.compoRightBottom.layout();
				}
			});
			fd = new FormData();
			fd.top = new FormAttachment(this.lblCantExport, 10);
			fd.left = new FormAttachment(25);
			fd.right = new FormAttachment(75);
			fd.bottom = new FormAttachment(40);
			this.tblListConflicts.setLayoutData(fd);
	
			Label lblCompare = new Label(this.grpConflict, SWT.NONE);
			lblCompare.setBackground(GROUP_BACKGROUND_COLOR);
			lblCompare.setText("Please verify your version against the latest version in the database :");
			fd = new FormData();
			fd.top = new FormAttachment(this.tblListConflicts, 20);
			fd.left = new FormAttachment(0, 10);
			lblCompare.setLayoutData(fd);
	
			this.tblCompareComponent = new Tree(this.grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.V_SCROLL);
			this.tblCompareComponent.setBackground(GROUP_BACKGROUND_COLOR);
			this.tblCompareComponent.setHeaderVisible(true);
			this.tblCompareComponent.setLinesVisible(true);
			fd = new FormData();
			fd.top = new FormAttachment(lblCompare, 10);
			fd.left = new FormAttachment(0,10);
			fd.right = new FormAttachment(100, -10);
			fd.bottom = new FormAttachment(100, -40);
			this.tblCompareComponent.setLayoutData(fd);
	
			TreeColumn colItems = new TreeColumn(this.tblCompareComponent, SWT.NONE);
			colItems.setText("Items");
			colItems.setWidth(119);
	
			TreeColumn colYourVersion = new TreeColumn(this.tblCompareComponent, SWT.NONE);
			colYourVersion.setText("Your version");
			colYourVersion.setWidth(220);
	
			TreeColumn colDatabaseVersion = new TreeColumn(this.tblCompareComponent, SWT.NONE);
			colDatabaseVersion.setText("Database version");
			colDatabaseVersion.setWidth(220);
	
			this.btnImportDatabaseVersion = new Button(this.grpConflict, SWT.NONE);
			this.btnImportDatabaseVersion.setImage(IMPORT_FROM_DATABASE_IMAGE);
			this.btnImportDatabaseVersion.setText("Import");
			this.btnImportDatabaseVersion.setEnabled(false);
			this.btnImportDatabaseVersion.addSelectionListener(new SelectionListener() {
				@Override
                public void widgetSelected(SelectionEvent e) { 
					if ( DBGuiExportModel.this.checkRememberChoice.getSelection() ) {
						// if the button checkRememberChoice is checked, then we apply the choice for all the conflicting components.
						// at the end, only those with errors will stay
						DBGuiExportModel.this.tblListConflicts.setSelection(0);
						for ( int i=0; i<DBGuiExportModel.this.tblListConflicts.getItemCount(); ++i)
							tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.importFromDatabase);
					} else {
						// we only apply the choice to the selected component
						tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.importFromDatabase);
					}
				}
				@Override
                public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
			});
			fd = new FormData(80,25);
			fd.right = new FormAttachment(100, -10);
			fd.bottom = new FormAttachment(100, -10);
			this.btnImportDatabaseVersion.setLayoutData(fd);
	
			this.btnExportMyVersion = new Button(this.grpConflict, SWT.NONE);
			this.btnExportMyVersion.setImage(EXPORT_TO_DATABASE_IMAGE);
			this.btnExportMyVersion.setText("Export");
			this.btnExportMyVersion.setEnabled(false);
			this.btnExportMyVersion.addSelectionListener(new SelectionListener() {
				@Override
                public void widgetSelected(SelectionEvent e) { 
					if ( DBGuiExportModel.this.checkRememberChoice.getSelection() ) {
						// if the button checkRememberChoice is checked, then we apply the choice for all the conflicting components.
						// at the end, only those with errors will stay
						DBGuiExportModel.this.tblListConflicts.setSelection(0);
						for ( int i=0; i<DBGuiExportModel.this.tblListConflicts.getItemCount(); ++i)
							tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.exportToDatabase);
					} else {
						// we only apply the choice to the selected component
						tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.exportToDatabase);
					}
				}
				@Override
                public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
			});
			fd = new FormData(80,25);
			fd.right = new FormAttachment(this.btnImportDatabaseVersion, -10);
			fd.bottom = new FormAttachment(100, -10);
			this.btnExportMyVersion.setLayoutData(fd);
	
			this.btnDoNotExport = new Button(this.grpConflict, SWT.NONE);
			this.btnDoNotExport.setText("Do not export");
			this.btnDoNotExport.setEnabled(false);
			this.btnDoNotExport.addSelectionListener(new SelectionListener() {
				@Override
                public void widgetSelected(SelectionEvent e) { 
					if ( DBGuiExportModel.this.checkRememberChoice.getSelection() ) {
						// if the button checkRememberChoice is checked, then we apply the choice for all the conflicting components.
						// at the end, only those with errors will stay
						DBGuiExportModel.this.tblListConflicts.setSelection(0);
						for ( int i=0; i<DBGuiExportModel.this.tblListConflicts.getItemCount(); ++i)
							tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.doNotExport);
					} else {
						// we only apply the choice to the selected component
						tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.doNotExport);
					}
				}
				@Override
                public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
			});
			fd = new FormData(80,25);
			fd.right = new FormAttachment(this.btnExportMyVersion, -10);
			fd.bottom = new FormAttachment(100, -10);
			this.btnDoNotExport.setLayoutData(fd);
	
			this.checkRememberChoice = new Button(this.grpConflict, SWT.CHECK);
			this.checkRememberChoice.setText("Remember my choice");
			fd = new FormData();
			fd.right = new FormAttachment(this.btnDoNotExport, -20);
			fd.top = new FormAttachment(this.btnDoNotExport, 0, SWT.CENTER);
			this.checkRememberChoice.setLayoutData(fd);
	
			this.grpConflict.layout();
		} else {
			this.grpConflict.setVisible(true);
			this.tblListConflicts.removeAll();
			this.tblCompareComponent.removeAll();
		}
	}
	
	/**
	 * called when the user click on the btnExportMyVersion button<br>
	 * Sets the exportChoice on the component's DBmetadata and removes the component from the tblListconflicts table<br>
	 * If no conflict remain, then it relaunch the doExportComponents method 
	 */
	protected void tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE requiredChoice) {
		CONFLICT_CHOICE effectiveChoice = requiredChoice;
		EObject component = this.exportedModel.getAllElements().get(this.tblListConflicts.getSelection()[0].getText());
		if ( component == null ) component = this.exportedModel.getAllRelationships().get(this.tblListConflicts.getSelection()[0].getText());
		if ( component == null ) {
			component = this.exportedModel.getAllFolders().get(this.tblListConflicts.getSelection()[0].getText());
			if ( component == null ) component = this.exportedModel.getAllViews().get(this.tblListConflicts.getSelection()[0].getText());
			if ( component == null ) {
				popup(Level.ERROR, "Can't get conflicting component \""+this.tblListConflicts.getSelection()[0].getText()+"\"");
				return;
			}

			if ( effectiveChoice == CONFLICT_CHOICE.importFromDatabase ) {
				logger.debug("Importing from database is not allowed for "+component.getClass().getSimpleName());
				effectiveChoice = CONFLICT_CHOICE.askUser;
			}
		}

		((IDBMetadata)component).getDBMetadata().setConflictChoice(effectiveChoice);
		switch (effectiveChoice) {
			case doNotExport:        if ( logger.isDebugEnabled() ) logger.debug("Tagging component to do not export");                      break;
			case exportToDatabase:   if ( logger.isDebugEnabled() ) logger.debug("Tagging component to export current version to database"); break;
			case importFromDatabase: if ( logger.isDebugEnabled() ) logger.debug("Tagging component to import database version");            break;
			case askUser:            if ( logger.isDebugEnabled() ) logger.debug("Tagging component to ask user");                           break;
			default:
		}

		int index = this.tblListConflicts.getSelectionIndex();
		this.tblListConflicts.remove(index);
		if ( logger.isDebugEnabled() ) logger.debug("Remaining " + this.tblListConflicts.getItemCount() + " conflicts");
		if ( this.tblListConflicts.getItemCount() == 0 ) {
			this.grpComponents.setVisible(true);
			this.grpModelVersions.setVisible(true);
			this.grpConflict.setVisible(false);
			export();
		} else {
			if ( this.tblListConflicts.getItemCount() < 2 )
				this.lblCantExport.setText("Can't export because "+this.tblListConflicts.getItemCount()+" component conflicts with newer version in the database :");
			else
				this.lblCantExport.setText("Can't export because "+this.tblListConflicts.getItemCount()+" components conflict with newer version in the database :");

			if ( index < this.tblListConflicts.getItemCount() )
				this.tblListConflicts.setSelection(index);
			else
				this.tblListConflicts.setSelection(index-1);
			this.tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
		}
	}

	protected void doShowResult(STATUS status, String message) {
		logger.debug("Showing result.");
		if ( this.grpProgressBar != null ) this.grpProgressBar.setVisible(false);
		if ( this.grpConflict != null ) this.grpConflict.setVisible(false);
		this.grpComponents.setVisible(true);
		this.grpModelVersions.setVisible(true);

		setActiveAction(ACTION.Three);
		
		if ( logger.isTraceEnabled() ) logger.trace("Model : "+this.txtTotalElements.getText()+" elements, "+this.txtTotalRelationships.getText()+" relationships, "+this.txtTotalFolders.getText()+" folders, "+this.txtTotalViews.getText()+" views, "+this.txtTotalViewObjects.getText()+" view objects, "+this.txtTotalViewConnections.getText()+" view connections.");
		
		if ( status == STATUS.Ok ) {
			setMessage(message, GREEN_COLOR);
			if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("closeIfSuccessful") ) {
				if ( logger.isDebugEnabled() ) logger.debug("Automatically closing the window as set in preferences");
			    close();
			    return;
			}
			if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("removeDirtyFlag") ) {
			    if ( logger.isDebugEnabled() ) logger.debug("Removing model's dirty flag");
			    CommandStack stack = (CommandStack)this.exportedModel.getAdapter(CommandStack.class);
			    stack.markSaveLocation();
			}
		} else {
			setMessage(message, RED_COLOR);
		}
		
		this.btnClose.setText("close");
	}

	Button btnDoNotExport;
	Button btnExportMyVersion;
	Button btnImportDatabaseVersion;
	Button btnCompareModelToDatabase;
	
	Button checkRememberChoice;

	Group grpConflict;

	Tree tblCompareComponent;
	Table tblListConflicts;
	private Label lblCantExport;

	Text txtReleaseNote;
	
	private Label lblTotal;
	private Label lblModel;
	private Label lblModelNew;
	private Label lblModelUpdated;
	private Label lblModelDeleted;
	private Label lblDatabase;
	private Label lblDatabaseNew;
	private Label lblDatabaseUpdated;
	private Label lblDatabaseDeleted;
	private Label lblConflicts;

    private Text txtTotalElements;
    private Text txtNewElementsInModel;
    private Text txtUpdatedElementsInModel;
    private Text txtDeletedElementsInModel;
    private Text txtNewElementsInDatabase;
    private Text txtUpdatedElementsInDatabase;
    private Text txtDeletedElementsInDatabase;
    private Text txtConflictingElements;

    private Text txtTotalRelationships;
    private Text txtNewRelationshipsInModel;
    private Text txtUpdatedRelationshipsInModel;
    private Text txtDeletedRelationshipsInModel;
    private Text txtNewRelationshipsInDatabase;
    private Text txtUpdatedRelationshipsInDatabase;
    private Text txtDeletedRelationshipsInDatabase;
    private Text txtConflictingRelationships;

    private Text txtTotalFolders;
    private Text txtNewFoldersInModel;
    private Text txtUpdatedFoldersInModel;
    private Text txtDeletedFoldersInModel;
    private Text txtNewFoldersInDatabase;
    private Text txtUpdatedFoldersInDatabase;
    private Text txtDeletedFoldersInDatabase;
    private Text txtConflictingFolders;

    private Text txtTotalViews;
    private Text txtNewViewsInModel;
    private Text txtUpdatedViewsInModel;
    private Text txtDeletedViewsInModel;
    private Text txtNewViewsInDatabase;
    private Text txtUpdatedViewsInDatabase;
    private Text txtDeletedViewsInDatabase;
    private Text txtConflictingViews;

    private Text txtTotalViewObjects;
    private Text txtNewViewObjectsInModel;
    private Text txtUpdatedViewObjectsInModel;
    private Text txtDeletedViewObjectsInModel;
    private Text txtNewViewObjectsInDatabase;
    private Text txtUpdatedViewObjectsInDatabase;
    private Text txtDeletedViewObjectsInDatabase;
    private Text txtConflictingViewObjects;

    private Text txtTotalViewConnections;
    private Text txtNewViewConnectionsInModel;
    private Text txtUpdatedViewConnectionsInModel;
    private Text txtDeletedViewConnectionsInModel;
    private Text txtNewViewConnectionsInDatabase;
    private Text txtUpdatedViewConnectionsInDatabase;
    private Text txtDeletedViewConnectionsInDatabase;
    private Text txtConflictingViewConnections;
    
    private Text txtTotalImages;
    private Text txtNewImagesInModel;
    private Text txtNewImagesInDatabase;


	Table tblModelVersions;
	Text txtModelName;
	Text txtPurpose;
}
