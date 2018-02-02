/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.archicontribs.database.DBChecksum;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.DBVersion;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.DBMetadata.CONFLICT_CHOICE;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
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
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
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

	ArchimateModel exportedModel = null;

	Group grpComponents;
	Group grpModelVersions;
	
	HashMap<String, DBVersion> newDatabaseComponents;
	
	private boolean forceExport; 
	
	/**
	 * Creates the GUI to export components and model
	 */
	public DBGuiExportModel(ArchimateModel model, String title) throws Exception {
		// We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
		super(title);
		// We reference the exported model 
        this.exportedModel = model;
		
		this.includeNeo4j = true;

		popup("Please wait while counting model's components");
		this.exportedModel.countAllObjects();
		if ( logger.isDebugEnabled() ) logger.debug("the model has got "+model.getAllElements().size()+" elements and "+model.getAllRelationships().size()+" relationships.");
		closePopup();
		
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for exporting model \""+model.getName()+"\" (plugin version "+DBPlugin.pluginVersion+").");
		
		createGrpComponents();
		createGrpModel();
		this.compoRightBottom.setVisible(true);
		this.compoRightBottom.layout();

		createAction(ACTION.One, "1 - Confirm export");
		createAction(ACTION.Two, "2 - Export components");
		createAction(ACTION.Three, "3 - Status");

		// we show an arrow in front of the first action
		setActiveAction(ACTION.One);

		// We activate the btnDoAction button : if the user select the "Export" button --> call the exportComponents() method
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
		
	    getDatabases();
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
				
				if ( canExport ) {
                    boolean canChangeMetaData = (DBGuiExportModel.this.connection != null && DBGuiExportModel.this.selectedDatabase.getExportWholeModel() && (DBGuiExportModel.this.tblModelVersions.getSelection()[0] == DBGuiExportModel.this.tblModelVersions.getItem(0)));
                    
                    DBGuiExportModel.this.txtReleaseNote.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.txtPurpose.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.txtModelName.setEnabled(canChangeMetaData);
                    DBGuiExportModel.this.btnDoAction.setEnabled(canChangeMetaData);
                    
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
        fd.top = new FormAttachment(100, -220);
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
        fd.right = new FormAttachment(30, 0);
        this.lblTotal.setLayoutData(fd);
        
        this.lblModel = new Label(this.grpComponents, SWT.CENTER);
        this.lblModel.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModel.setText("Model");
        fd = new FormData();
        fd.top = new FormAttachment(0, -8);
        fd.left = new FormAttachment(35, 0);
        fd.right = new FormAttachment(55, 0);
        this.lblModel.setLayoutData(fd);
        
        this.lblModelNew = new Label(this.grpComponents, SWT.CENTER);
        this.lblModelNew.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModelNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(35, 0);
        fd.right = new FormAttachment(45, -3);
        this.lblModelNew.setLayoutData(fd);
        
        this.lblModelUpdated = new Label(this.grpComponents, SWT.CENTER);
        this.lblModelUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblModelUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(45, 2);
        fd.right = new FormAttachment(55, 0);
        this.lblModelUpdated.setLayoutData(fd);
        
        this.lblDatabase = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabase.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabase.setText("Database");
        fd = new FormData();
        fd.top = new FormAttachment(0, -8);
        fd.left = new FormAttachment(60, 0);
        fd.right = new FormAttachment(80, 0);
        this.lblDatabase.setLayoutData(fd);
        
        this.lblDatabaseNew = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabaseNew.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabaseNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(60, 0);
        fd.right = new FormAttachment(70, -3);
        this.lblDatabaseNew.setLayoutData(fd);
        
        this.lblDatabaseUpdated = new Label(this.grpComponents, SWT.CENTER);
        this.lblDatabaseUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblDatabaseUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(70, 2);
        fd.right = new FormAttachment(80, 0);
        this.lblDatabaseUpdated.setLayoutData(fd);
        
        this.lblConflict = new Label(this.grpComponents, SWT.CENTER);
        this.lblConflict.setBackground(GROUP_BACKGROUND_COLOR);
        this.lblConflict.setText("Conflict");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(85, 0);
        fd.right = new FormAttachment(95, 0);
        this.lblConflict.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalElements.setText(String.valueOf(this.exportedModel.getAllElements().size()));
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
        
        this.txtConflictingElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflict, 0, SWT.RIGHT);
        this.txtConflictingElements.setLayoutData(fd);

        /* * * * * */
        
        this.txtTotalRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalRelationships.setText(String.valueOf(this.exportedModel.getAllRelationships().size()));
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
        
        this.txtConflictingRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflict, 0, SWT.RIGHT);
        this.txtConflictingRelationships.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalFolders.setText(String.valueOf(this.exportedModel.getAllFolders().size()));
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
        
        this.txtConflictingFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflict, 0, SWT.RIGHT);
        this.txtConflictingFolders.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViews.setText(String.valueOf(this.exportedModel.getAllViews().size()));
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
        
        this.txtConflictingViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtConflictingViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblConflict, 0, SWT.RIGHT);
        this.txtConflictingViews.setLayoutData(fd);
        
        /* * * * * */
        
        this.txtTotalViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViewObjects.setText(String.valueOf(this.exportedModel.getAllViewObjects().size()));
        this.txtTotalViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalViewObjects.setLayoutData(fd);
  
        /* * * * * */
        
        this.txtTotalViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalViewConnections.setText(String.valueOf(this.exportedModel.getAllViewConnections().size()));
        this.txtTotalViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalViewConnections.setLayoutData(fd);

        /* * * * * */
        
        this.txtTotalImages = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
        this.txtTotalImages.setText(String.valueOf(this.exportedModel.getAllImagePaths().size()));
        this.txtTotalImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
        this.txtTotalImages.setLayoutData(fd);
    }

	/**
	 * This method is called each time a database is selected and a connection has been established to it.<br>
	 * <br>
	 * It enables the export button and starts the export if the "automatic start" is specified in the plugin preferences
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheckDatabase) {
		// we hide the database and conflict columns in standalone mode, and show them in collaborative mode
		this.lblDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.lblDatabaseNew.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.lblDatabaseUpdated.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.lblConflict.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewElementsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedElementsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingElements.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewRelationshipsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedRelationshipsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingRelationships.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewFoldersInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedFoldersInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingFolders.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewViewsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedViewsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingViews.setVisible(this.selectedDatabase.getCollaborativeMode());

		
		// if we're not in a Neo4J database, then we get the latest version and checksum of the model's components in the database
		try {
			if ( !DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") )
				this.connection.getModelVersions(this.exportedModel.getId(), this.tblModelVersions);
		} catch (Exception err) {
			closePopup();
			popup(Level.FATAL, "Failed to check existing components in database", err);
			setActiveAction(STATUS.Error);
			return;
		}
		
		// if the exportWithDefaultValues preference is set, then we automatically start the export
		if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("exportWithDefaultValues") ) {
			logger.debug("Automatically start export as specified in preferences.");
			this.btnDoAction.notifyListeners(SWT.Selection, new Event());
		} else {
    		// else, we check what needs to be exported to the database
            this.txtNewElementsInModel.setText("0");         this.txtUpdatedElementsInModel.setText("0");         this.txtNewElementsInDatabase.setText("0");          this.txtUpdatedElementsInDatabase.setText("0");          this.txtConflictingElements.setText("0");
            this.txtNewRelationshipsInModel.setText("0");    this.txtUpdatedRelationshipsInModel.setText("0");    this.txtNewRelationshipsInDatabase.setText("0");     this.txtUpdatedRelationshipsInDatabase.setText("0");     this.txtConflictingRelationships.setText("0");
            this.txtNewFoldersInModel.setText("0");          this.txtUpdatedFoldersInModel.setText("0");          this.txtNewFoldersInDatabase.setText("0");           this.txtUpdatedFoldersInDatabase.setText("0");           this.txtConflictingFolders.setText("0");
            this.txtNewViewsInModel.setText("0");            this.txtUpdatedViewsInModel.setText("0");            this.txtNewViewsInDatabase.setText("0");             this.txtUpdatedViewsInDatabase.setText("0");             this.txtConflictingViews.setText("0");
            this.txtNewElementsInModel.setText("0");         this.txtUpdatedElementsInModel.setText("0");         this.txtNewElementsInDatabase.setText("0");          this.txtUpdatedElementsInDatabase.setText("0");          this.txtConflictingElements.setText("0");
    
            try {
                popup("Please wait while comparing model from the database");
                this.connection.getVersionsFromDatabase(this.exportedModel);
            } catch (SQLException err ) {
                closePopup();
                popup(Level.FATAL, "Failed to get latest version of components in the database.", err);
                setActiveAction(STATUS.Error);
                doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
                return;
            }
            
            // we export the components without checking for conflicts in 3 cases:
            //    - the model is not in the database
            //    - the current model is the latest model in the database
            //    - we are in standalone mode
            this.forceExport = this.exportedModel.getCurrentVersion().getVersion() == 0
                    || this.exportedModel.getCurrentVersion().getLatestVersion() == this.exportedModel.getCurrentVersion().getVersion()
                    || !this.selectedDatabase.getCollaborativeMode();
    
            int nbNews = 0;
            int nbUpdated = 0;
            int nbUpdatedDb = 0;
            int nbConflict = 0;
            Iterator<Map.Entry<String, IArchimateElement>> ite = this.exportedModel.getAllElements().entrySet().iterator();
            while (ite.hasNext()) {
                DBMetadata metadata = ((IDBMetadata)ite.next().getValue()).getDBMetadata();
                if (  metadata.getDatabaseVersion().getLatestVersion() == 0 ) {
                    // if the database version is zero, then the component is not in the database (therefore, new in the model)
                    ++nbNews;
                } else {
                    if ( !DBPlugin.areEqual(metadata.getDatabaseVersion().getLatestChecksum(), metadata.getCurrentVersion().getLatestChecksum()) ) {
                        boolean modifiedInModel = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getCurrentVersion().getLatestChecksum());
                        boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getDatabaseVersion().getLatestChecksum());
                        
                        if ( modifiedInModel && modifiedInDatabase ) {
                            if ( this.forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            this.txtNewElementsInModel.setText(String.valueOf(nbNews));
            this.txtUpdatedElementsInModel.setText(String.valueOf(nbUpdated));
            this.txtUpdatedElementsInDatabase.setText(String.valueOf(nbUpdatedDb));
            this.txtNewElementsInDatabase.setText(String.valueOf(this.connection.getElementsNotInModel().size()));
            this.txtConflictingElements.setText(String.valueOf(nbConflict));
            
            nbNews = 0;
            nbUpdated = 0;
            nbUpdatedDb = 0;
            nbConflict = 0;
            Iterator<Map.Entry<String, IArchimateRelationship>> itr = this.exportedModel.getAllRelationships().entrySet().iterator();
            while (itr.hasNext()) {
                DBMetadata metadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
                if (  metadata.getDatabaseVersion().getLatestVersion() == 0 ) {
                    // if the database version is zero, then the component is not in the database (therefore, new in the model)
                    ++nbNews;
                } else {
                    if ( !DBPlugin.areEqual(metadata.getDatabaseVersion().getLatestChecksum(), metadata.getCurrentVersion().getLatestChecksum()) ) {
                        boolean modifiedInModel = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getCurrentVersion().getLatestChecksum());
                        boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getDatabaseVersion().getLatestChecksum());
                        
                        if ( modifiedInModel && modifiedInDatabase ) {
                            if ( this.forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            this.txtNewRelationshipsInModel.setText(String.valueOf(nbNews));
            this.txtUpdatedRelationshipsInModel.setText(String.valueOf(nbUpdated));
            this.txtUpdatedRelationshipsInDatabase.setText(String.valueOf(nbUpdatedDb));
            this.txtNewRelationshipsInDatabase.setText(String.valueOf(this.connection.getRelationshipsNotInModel().size()));
            this.txtConflictingRelationships.setText(String.valueOf(nbConflict));
            
            nbNews = 0;
            nbUpdated = 0;
            nbUpdatedDb = 0;
            nbConflict = 0;
            Iterator<Map.Entry<String, IFolder>> itf = this.exportedModel.getAllFolders().entrySet().iterator();
            while (itf.hasNext()) {
                IFolder tmp = itf.next().getValue();
                DBMetadata metadata = ((IDBMetadata)tmp).getDBMetadata();
                if (  metadata.getDatabaseVersion().getLatestVersion() == 0 ) {
                    // if the database version is zero, then the component is not in the database (therefore, new in the model)
                    ++nbNews;
                } else {
                    if ( !DBPlugin.areEqual(metadata.getDatabaseVersion().getLatestChecksum(), metadata.getCurrentVersion().getLatestChecksum()) ) {
                        boolean modifiedInModel = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getCurrentVersion().getLatestChecksum());
                        boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getDatabaseVersion().getLatestChecksum());
                        
                        if ( modifiedInModel && modifiedInDatabase ) {
                            if ( this.forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            this.txtNewFoldersInModel.setText(String.valueOf(nbNews));
            this.txtUpdatedFoldersInModel.setText(String.valueOf(nbUpdated));
            this.txtUpdatedFoldersInDatabase.setText(String.valueOf(nbUpdatedDb));
            this.txtNewFoldersInDatabase.setText(String.valueOf(this.connection.getFoldersNotInModel().size()));
            this.txtConflictingFolders.setText(String.valueOf(nbConflict));
            
            nbNews = 0;
            nbUpdated = 0;
            nbUpdatedDb = 0;
            nbConflict = 0;
            Iterator<Map.Entry<String, IDiagramModel>> itv = this.exportedModel.getAllViews().entrySet().iterator();
            while (itv.hasNext()) {
                DBMetadata metadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
                if (  metadata.getDatabaseVersion().getLatestVersion() == 0 ) {
                    // if the database version is zero, then the component is not in the database (therefore, new in the model)
                    ++nbNews;
                } else {
                    if ( !DBPlugin.areEqual(metadata.getDatabaseVersion().getLatestChecksum(), metadata.getCurrentVersion().getLatestChecksum()) ) {
                        boolean modifiedInModel = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getCurrentVersion().getLatestChecksum());
                        boolean modifiedInDatabase = !DBPlugin.areEqual(metadata.getCurrentVersion().getChecksum(), metadata.getDatabaseVersion().getLatestChecksum());
                        
                        if ( modifiedInModel && modifiedInDatabase ) {
                            if ( this.forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            this.txtNewViewsInModel.setText(String.valueOf(nbNews));
            this.txtUpdatedViewsInModel.setText(String.valueOf(nbUpdated));
            this.txtUpdatedViewsInDatabase.setText(String.valueOf(nbUpdatedDb));
            this.txtNewViewsInDatabase.setText(String.valueOf(this.connection.getViewsNotInModel().size()));
            this.txtConflictingViews.setText(String.valueOf(nbConflict));
            
            closePopup();
    		
            if ( this.txtNewElementsInModel.getText().equals("0") && this.txtNewRelationshipsInModel.getText().equals("0") && this.txtNewFoldersInModel.getText().equals("0") && this.txtNewViewsInModel.getText().equals("0") &&
                    this.txtUpdatedElementsInModel.getText().equals("0") && this.txtUpdatedRelationshipsInModel.getText().equals("0") && this.txtUpdatedFoldersInModel.getText().equals("0") && this.txtUpdatedViewsInModel.getText().equals("0") && 
                    this.txtNewElementsInDatabase.getText().equals("0") && this.txtNewRelationshipsInDatabase.getText().equals("0") && this.txtNewFoldersInDatabase.getText().equals("0") && this.txtNewViewsInDatabase.getText().equals("0") &&
                    this.txtUpdatedElementsInDatabase.getText().equals("0") && this.txtUpdatedRelationshipsInDatabase.getText().equals("0") && this.txtUpdatedFoldersInDatabase.getText().equals("0") && this.txtUpdatedViewsInDatabase.getText().equals("0") &&
                    this.txtConflictingElements.getText().equals("0") && this.txtConflictingRelationships.getText().equals("0") && this.txtConflictingFolders.getText().equals("0") && this.txtConflictingViews.getText().equals("0") ) {
                popup(Level.INFO, "Your database is already up to date.");
                if ( logger.isDebugEnabled() ) logger.debug("Disabling the \"Export\" button.");
                this.btnDoAction.setEnabled(false);
                this.btnDoAction.setText("Export");
                
                this.exportedModel.getCurrentVersion().setTimestamp(this.exportedModel.getDatabaseVersion().getLatestTimestamp());
            } else {
                if ( logger.isDebugEnabled() ) logger.debug("Enabling the \"Export\" button.");
                this.btnDoAction.setEnabled(true);
                
                if ( this.txtUpdatedElementsInDatabase.getText().equals("0") && this.txtUpdatedRelationshipsInDatabase.getText().equals("0") && this.txtUpdatedFoldersInDatabase.getText().equals("0") && this.txtUpdatedViewsInDatabase.getText().equals("0") &&
                this.txtConflictingElements.getText().equals("0") && this.txtConflictingRelationships.getText().equals("0") && this.txtConflictingFolders.getText().equals("0") && this.txtConflictingViews.getText().equals("0") )
                    this.btnDoAction.setText("Export");
                else
                    this.btnDoAction.setText("Sync");
            }
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
		
		// we hide the database and conflict columns in standalone mode, and show them in collaborative mode
		this.lblDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.lblDatabaseNew.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.lblDatabaseUpdated.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.lblConflict.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewElementsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedElementsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingElements.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewRelationshipsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedRelationshipsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingRelationships.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewFoldersInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedFoldersInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingFolders.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtNewViewsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtUpdatedViewsInDatabase.setVisible(this.selectedDatabase.getCollaborativeMode());
		this.txtConflictingViews.setVisible(this.selectedDatabase.getCollaborativeMode());
		
		this.txtNewElementsInModel.setText("");			this.txtUpdatedElementsInModel.setText("");			this.txtNewElementsInDatabase.setText("");			this.txtUpdatedElementsInDatabase.setText("");			this.txtConflictingElements.setText("");
		this.txtNewRelationshipsInModel.setText("");		this.txtUpdatedRelationshipsInModel.setText("");		this.txtNewRelationshipsInDatabase.setText("");		this.txtUpdatedRelationshipsInDatabase.setText("");		this.txtConflictingRelationships.setText("");
		this.txtNewFoldersInModel.setText("");			this.txtUpdatedFoldersInModel.setText("");			this.txtNewFoldersInDatabase.setText("");			this.txtUpdatedFoldersInDatabase.setText("");			this.txtConflictingFolders.setText("");
		this.txtNewViewsInModel.setText("");				this.txtUpdatedViewsInModel.setText("");				this.txtNewViewsInDatabase.setText("");				this.txtUpdatedViewsInDatabase.setText("");				this.txtConflictingViews.setText("");
		this.txtNewElementsInModel.setText("");			this.txtUpdatedElementsInModel.setText("");			this.txtNewElementsInDatabase.setText("");			this.txtUpdatedElementsInDatabase.setText("");			this.txtConflictingElements.setText("");
		
		this.btnDoAction.setText("Export");
	}

	/**
	 * Loop on model components and call doExportEObject to export them<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button
	 */
	protected void export() {
		int progressBarWidth;
		if ( this.selectedDatabase.getExportWholeModel() ) {
			logger.info("Exporting model : "+this.exportedModel.getAllElements().size()+" elements, "+this.exportedModel.getAllRelationships().size()+" relationships, "+this.exportedModel.getAllFolders().size()+" folders, "+this.exportedModel.getAllViews().size()+" views, "+this.exportedModel.getAllViewObjects().size()+" views objects, "+this.exportedModel.getAllViewConnections().size()+" views connections, and "+((IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()+" images.");
			progressBarWidth = this.exportedModel.getAllFolders().size()+this.exportedModel.getAllElements().size()+this.exportedModel.getAllRelationships().size()+this.exportedModel.getAllViews().size()+this.exportedModel.getAllViewObjects().size()+this.exportedModel.getAllViewConnections().size()+((IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size();
		} else {
			logger.info("Exporting components : "+this.exportedModel.getAllElements().size()+" elements, "+this.exportedModel.getAllRelationships().size()+" relationships.");
			progressBarWidth = this.exportedModel.getAllElements().size()+this.exportedModel.getAllRelationships().size();
		}
		
		// we disable the export button to avoid a second click
		this.btnDoAction.setEnabled(false);

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
            this.exportedModel.getCurrentVersion().setLatestChecksum(DBChecksum.calculateChecksum(this.exportedModel, this.txtReleaseNote.getText()));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException err) {
            popup(Level.FATAL, "Failed to calculate the model's checksum.", err);
            setActiveAction(STATUS.Error);
            doShowResult(STATUS.Error, "Failed to calculate the model's checksum.\n"+err.getMessage());
            return;
        }

		// then, we start a new database transaction
		try {
		    this.connection.setAutoCommit(false);
		} catch (SQLException err ) {
			popup(Level.FATAL, "Failed to create a transaction in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return;
		}

		// we reset the counters as they will be updated by the doExportEObject method
	    this.txtNewElementsInModel.setText("0");         this.txtUpdatedElementsInModel.setText("0");         this.txtNewElementsInDatabase.setText("0");          this.txtUpdatedElementsInDatabase.setText("0");          this.txtConflictingElements.setText("0");
        this.txtNewRelationshipsInModel.setText("0");    this.txtUpdatedRelationshipsInModel.setText("0");    this.txtNewRelationshipsInDatabase.setText("0");     this.txtUpdatedRelationshipsInDatabase.setText("0");     this.txtConflictingRelationships.setText("0");
        this.txtNewFoldersInModel.setText("0");          this.txtUpdatedFoldersInModel.setText("0");          this.txtNewFoldersInDatabase.setText("0");           this.txtUpdatedFoldersInDatabase.setText("0");           this.txtConflictingFolders.setText("0");
        this.txtNewViewsInModel.setText("0");            this.txtUpdatedViewsInModel.setText("0");            this.txtNewViewsInDatabase.setText("0");             this.txtUpdatedViewsInDatabase.setText("0");             this.txtConflictingViews.setText("0");
        this.txtNewElementsInModel.setText("0");         this.txtUpdatedElementsInModel.setText("0");         this.txtNewElementsInDatabase.setText("0");          this.txtUpdatedElementsInDatabase.setText("0");          this.txtConflictingElements.setText("0");

		
		try {
			// we need to recalculate the latest versions in the database in case someone updated the database since the last check
			// TODO : add a transaction number that will speed up the export process in case nobody updated the database 
			this.connection.getVersionsFromDatabase(this.exportedModel);
		} catch (SQLException err ) {
			popup(Level.FATAL, "Failed to get latest version of components in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return;
		}
		
	    // we export the components without checking for conflicts in 3 cases:
        //    - the model is not in the database
        //    - the current model is the latest model in the database
        //    - we are in standalone mode
        this.forceExport = this.exportedModel.getCurrentVersion().getVersion() == 0
                || this.exportedModel.getCurrentVersion().getLatestVersion() == this.exportedModel.getCurrentVersion().getVersion()
                || !this.selectedDatabase.getCollaborativeMode();
		
		// we export the components
		try {
			// if we need to save the whole model (i.e. not only the elements and the relationships) 
			if ( this.selectedDatabase.getExportWholeModel() ) {
				// We update the model name and purpose in case they've been changed in the export windows
				if ( !DBPlugin.areEqual(this.exportedModel.getName(), this.txtModelName.getText()) )
					this.exportedModel.setName(this.txtModelName.getText());

				if ( !DBPlugin.areEqual(this.exportedModel.getPurpose(), this.txtPurpose.getText()) )
					this.exportedModel.setPurpose(this.txtPurpose.getText());

				if ( logger.isDebugEnabled() ) logger.debug("Exporting version "+this.exportedModel.getCurrentVersion().getLatestVersion()+" of the model ("+this.exportedModel.getCurrentVersion().getTimestamp().toString()+")");
				this.connection.exportModel(this.exportedModel, this.txtReleaseNote.getText());
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting folders");
				Iterator<Entry<String, IFolder>> foldersIterator = this.exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) {
					doExportEObject(foldersIterator.next().getValue(), this.txtNewFoldersInModel, this.txtUpdatedFoldersInModel, this.txtNewFoldersInDatabase, this.txtUpdatedFoldersInDatabase, this.txtConflictingFolders);
				}
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting elements");
			Iterator<Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
			while ( elementsIterator.hasNext() ) {
				doExportEObject(elementsIterator.next().getValue(), this.txtNewElementsInModel, this.txtUpdatedElementsInModel, this.txtNewElementsInDatabase, this.txtUpdatedElementsInDatabase, this.txtConflictingElements);
			}
			
			if ( logger.isDebugEnabled() ) logger.debug("Must import "+this.connection.getElementsNotInModel().size()+" elements");
			for (String id : this.connection.getElementsNotInModel().keySet() ) {
			    DBVersion versionToImport = this.connection.getElementsNotInModel().get(id);
			    this.connection.importElementFromId(this.exportedModel, null, id, versionToImport.getLatestVersion(), false);
			    incrementText(this.txtNewElementsInDatabase);
			}
			    
			if ( logger.isDebugEnabled() ) logger.debug("Exporting relationships");
			Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
			while ( relationshipsIterator.hasNext() ) {
				doExportEObject(relationshipsIterator.next().getValue(), this.txtNewRelationshipsInModel, this.txtUpdatedRelationshipsInModel, this.txtNewRelationshipsInDatabase, this.txtUpdatedRelationshipsInDatabase, this.txtConflictingRelationships);
			}
			
	        if ( logger.isDebugEnabled() ) logger.debug("Must import "+this.connection.getRelationshipsNotInModel().size()+" relationships");
	        for (String id : this.connection.getRelationshipsNotInModel().keySet() ) {
	            DBVersion versionToImport = this.connection.getRelationshipsNotInModel().get(id);
	            this.connection.importRelationshipFromId(this.exportedModel, null, id, versionToImport.getLatestVersion(), false);
	            incrementText(this.txtNewRelationshipsInDatabase);
	        }
	
			if ( this.selectedDatabase.getExportWholeModel() ) {
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views");
				Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue(); 
					if ( doExportEObject(view, this.txtNewViewsInModel, this.txtUpdatedViewsInModel, this.txtNewViewsInDatabase, this.txtUpdatedViewsInDatabase, this.txtConflictingViews) ) {
					    this.connectionsAlreadyExported = new HashMap<String, IDiagramModelConnection>();      // we need to memorize exported connections as they can be get as sources AND as targets 
	                    for ( IDiagramModelObject viewObject: view.getChildren() ) {
	                        doExportViewObject(viewObject);
	                    }
					}
				}
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting images");
		    	IArchiveManager archiveMgr = (IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class);
				for ( String path: this.exportedModel.getAllImagePaths() ) {
					this.connection.exportImage(path, archiveMgr.getBytesFromEntry(path));
					//TODO : the imagePath is a checksum so an image cannot be modified without changing its imagePath;
					/*
					switch ( connection.exportImage(path, archiveMgr.getBytesFromEntry(path)) ) {
						case isNew :
							incrementText(txtIdenticalImages);
							decrementText(txtNewImages);
							break;
						case isUpdated :
							incrementText(txtIdenticalImages);
							decrementText(txtOlderImages);
							break;
						case isSynced :
							// do nothing
							break;
					}
					*/
				}
			}
		} catch (Exception err) {
			setActiveAction(STATUS.Error);
			popup(Level.FATAL, "An error occured while exporting the components.\n\nThe transaction will be rolled back to leave the database in a coherent state. You may solve the issue and export again your components.", err);
			try  {
			    this.connection.rollback();
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
				return;
			} catch (SQLException err2) {
				popup(Level.FATAL, "The transaction failed to rollback and the database is left in an inconsistent state.\n\nPlease check carrefully your database !", err2);
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err2.getMessage());
				return;
			}
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Found "+this.tblListConflicts.getItemCount()+" components conflicting with database");
		if ( this.tblListConflicts.getItemCount() == 0 ) {
			// the export is successfull
			try  {
				// we check if something has been really exported				
				if ( this.selectedDatabase.getExportWholeModel() ) {
			        if ( this.txtNewElementsInModel.getText().equals("0") && this.txtNewRelationshipsInModel.getText().equals("0") && this.txtNewFoldersInModel.getText().equals("0") && this.txtNewViewsInModel.getText().equals("0") &&
			                this.txtUpdatedElementsInModel.getText().equals("0") && this.txtUpdatedRelationshipsInModel.getText().equals("0") && this.txtUpdatedFoldersInModel.getText().equals("0") && this.txtUpdatedViewsInModel.getText().equals("0") && 
			                this.txtNewElementsInDatabase.getText().equals("0") && this.txtNewRelationshipsInDatabase.getText().equals("0") && this.txtNewFoldersInDatabase.getText().equals("0") && this.txtNewViewsInDatabase.getText().equals("0") &&
			                this.txtUpdatedElementsInDatabase.getText().equals("0") && this.txtUpdatedRelationshipsInDatabase.getText().equals("0") && this.txtUpdatedFoldersInDatabase.getText().equals("0") && this.txtUpdatedViewsInDatabase.getText().equals("0") &&
			                this.txtConflictingElements.getText().equals("0") && this.txtConflictingRelationships.getText().equals("0") && this.txtConflictingFolders.getText().equals("0") && this.txtConflictingViews.getText().equals("0") &&   
							this.exportedModel.getCurrentVersion().getLatestChecksum().equals(this.exportedModel.getDatabaseVersion().getChecksum()) ) {
						this.connection.rollback();
					    this.connection.setAutoCommit(true);
						setActiveAction(STATUS.Ok);
						setComponentVersion();
						doShowResult(STATUS.Ok, "The database is already up to date.");
						return;
					}
				}
			    this.connection.commit();
			    this.connection.setAutoCommit(true);
				setActiveAction(STATUS.Ok);
				setComponentVersion();
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
		    this.connection.rollback();
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
	
	/**
	 * this method should be called after the model has been successfully exported to the database.<br>
	 * <br>
	 * it copies the latestVestion to the currentVersion, latestTimestamp to CurrentTimestamp and latestChecksum to currentChecksum
	 */
	private void setComponentVersion() {
	    this.exportedModel.getCurrentVersion().setChecksum(this.exportedModel.getCurrentVersion().getLatestChecksum());
	    this.exportedModel.getCurrentVersion().setVersion(this.exportedModel.getCurrentVersion().getLatestVersion());
	    this.exportedModel.getCurrentVersion().setTimestamp(this.exportedModel.getCurrentVersion().getLatestTimestamp());
	    
	    Iterator<Map.Entry<String, IArchimateElement>> ite = this.exportedModel.getAllElements().entrySet().iterator();
        while (ite.hasNext()) {
            DBVersion version = ((IDBMetadata)ite.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(this.exportedModel.getCurrentVersion().getLatestTimestamp());
        }
        
        Iterator<Map.Entry<String, IArchimateRelationship>> itr = this.exportedModel.getAllRelationships().entrySet().iterator();
        while (itr.hasNext()) {
            DBVersion version = ((IDBMetadata)itr.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(this.exportedModel.getCurrentVersion().getLatestTimestamp());
        }
        
        Iterator<Map.Entry<String, IFolder>> itf = this.exportedModel.getAllFolders().entrySet().iterator();
        while (itf.hasNext()) {
            DBVersion version = ((IDBMetadata)itf.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(this.exportedModel.getCurrentVersion().getLatestTimestamp());
        }
        
        Iterator<Map.Entry<String, IDiagramModel>> itv = this.exportedModel.getAllViews().entrySet().iterator();
        while (itv.hasNext()) {
            DBVersion version = ((IDBMetadata)itv.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(this.exportedModel.getCurrentVersion().getLatestTimestamp());
        }
	}

	Map<String, IDiagramModelConnection> connectionsAlreadyExported;
	private void doExportViewObject(IDiagramModelObject viewObject) throws Exception {
		if ( logger.isTraceEnabled() ) logger.trace("exporting view object "+((IDBMetadata)viewObject).getDBMetadata().getDebugName());
		boolean exported = doExportEObject(viewObject, null, null, null, null, null);
		
		if ( exported ) {
			for ( IDiagramModelConnection source: ((IConnectable)viewObject).getSourceConnections() ) {
				if ( this.connectionsAlreadyExported.get(source.getId()) == null ) {
					doExportEObject(source, null, null, null, null, null);
					this.connectionsAlreadyExported.put(source.getId(), source);
				}
			}
			for ( IDiagramModelConnection target: ((IConnectable)viewObject).getTargetConnections() ) {
				if ( this.connectionsAlreadyExported.get(target.getId()) == null ) {
					doExportEObject(target, null, null, null, null, null);
					this.connectionsAlreadyExported.put(target.getId(), target);
				}
			}
			
			if ( viewObject instanceof IDiagramModelContainer ) {
				for ( IDiagramModelObject child: ((IDiagramModelContainer)viewObject).getChildren() ) {
					doExportViewObject(child);
				}
			}
		}
	}

	/**
	 * Effectively exports an EObject in the database<br>
	 * When a conflict is detected, the component ID is added to the tblListConflicts<br>
	 * <br>
	 * This method is called by the export() method
	 * @return true if the EObject has been exported, false if it is conflicting
	 */
	private boolean doExportEObject(EObject eObject, Text txtNewInModel, Text txtUpdatedInModel, Text txtNewInDatabase, Text txtUpdatedInDatabase, Text txtConflicting) throws Exception {
		assert(eObject instanceof IDBMetadata);
		assert(this.connection != null);
		
		boolean mustExport = false;
		boolean mustImport = false;
		boolean exported = false;
		
		EObject eObjectToExport = eObject;
		
		if ( DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
		    // in Neo4J databases, we do not manage versions so we export all the elements and all the relationships
		    mustExport = true;
		} else {
		    // but in SQL databases, we need to calculate the component version and check if there is no conflict in the database

		    if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestVersion() == 0 ) {
                // if the database version is zero then the component is not in the database, therefore, it is new in the model and we must export it
                mustExport = true;
            } else {
                if ( DBPlugin.areEqual(((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestChecksum(), ((IDBMetadata)eObjectToExport).getDBMetadata().getCurrentVersion().getLatestChecksum()) ) {
                    // if the checksum of the latest version in the database equals the latest checksum
                    // then the database is up to date and the component does not need to be exported
                } else {
                    // else, the component is different between the model and the database
        			boolean modifiedInModel = !DBPlugin.areEqual(((IDBMetadata)eObjectToExport).getDBMetadata().getCurrentVersion().getChecksum(), ((IDBMetadata)eObjectToExport).getDBMetadata().getCurrentVersion().getLatestChecksum());
        			boolean modifiedInDatabase = !DBPlugin.areEqual(((IDBMetadata)eObjectToExport).getDBMetadata().getCurrentVersion().getChecksum(), ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestChecksum());
        			
        			if ( modifiedInModel && modifiedInDatabase ) {
        				// if the component has been updated in both the model and the database, there is a conflict
        				// except if we force the export
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
        			} else {
        				if ( modifiedInModel )
        					mustExport = true;
        				else
        					mustImport = true;
        			}
                }
            }
		}
	            
		if ( mustExport ) {
			this.connection.exportEObject(eObjectToExport);
            if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestVersion() == 0 )
            	incrementText(txtNewInModel);
            else
                incrementText(txtUpdatedInModel);
            exported = true;
		}
		
		if ( mustImport ) {
            // For the moment, we can import elements and relationships only during an export !!!
            if ( eObjectToExport instanceof IArchimateElement ) {
                eObjectToExport = this.connection.importElementFromId(this.exportedModel, null, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestVersion(), false);
            } else if ( eObjectToExport instanceof IArchimateRelationship ) {
                eObjectToExport = this.connection.importRelationshipFromId(this.exportedModel, null, ((IIdentifier)eObjectToExport).getId(), ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestVersion(), false);
                incrementText(this.connection.getRelationshipsNotInModel().get(((IIdentifier)eObjectToExport).getId()) != null ? txtNewInDatabase : txtUpdatedInDatabase);
            } else
            	throw new Exception ("At the moment, we cannot import a "+eObjectToExport.getClass().getSimpleName()+" during the export process :(");
		}
		
		
		// even if the eObject is not exported, it has to be referenced as being part of the model
		if ( this.selectedDatabase.getExportWholeModel() )
			this.connection.assignEObjectToModel(eObjectToExport);
		
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
	
						fillInCompareTable(DBGuiExportModel.this.tblCompareComponent, conflictingComponent, ((IDBMetadata)conflictingComponent).getDBMetadata().getDatabaseVersion().getLatestVersion());
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
			colYourVersion.setWidth(170);
	
			TreeColumn colDatabaseVersion = new TreeColumn(this.tblCompareComponent, SWT.NONE);
			colDatabaseVersion.setText("Database version");
			colDatabaseVersion.setWidth(170);
	
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
		try {
			this.connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	Button btnDoNotExport;
	Button btnExportMyVersion;
	Button btnImportDatabaseVersion;
	
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
	private Label lblDatabase;
	private Label lblDatabaseNew;
	private Label lblDatabaseUpdated;
	private Label lblConflict;

    private Text txtTotalElements;
    private Text txtNewElementsInModel;
    private Text txtUpdatedElementsInModel;
    private Text txtNewElementsInDatabase;
    private Text txtUpdatedElementsInDatabase;
    private Text txtConflictingElements;

    private Text txtTotalRelationships;
    private Text txtNewRelationshipsInModel;
    private Text txtUpdatedRelationshipsInModel;
    private Text txtNewRelationshipsInDatabase;
    private Text txtUpdatedRelationshipsInDatabase;
    private Text txtConflictingRelationships;

    private Text txtTotalFolders;
    private Text txtNewFoldersInModel;
    private Text txtUpdatedFoldersInModel;
    private Text txtNewFoldersInDatabase;
    private Text txtUpdatedFoldersInDatabase;
    private Text txtConflictingFolders;

    private Text txtTotalViews;
    private Text txtNewViewsInModel;
    private Text txtUpdatedViewsInModel;
    private Text txtNewViewsInDatabase;
    private Text txtUpdatedViewsInDatabase;
    private Text txtConflictingViews;

    private Text txtTotalViewObjects;

    private Text txtTotalViewConnections;
    
    private Text txtTotalImages;


	Table tblModelVersions;
	Text txtModelName;
	Text txtPurpose;
}
