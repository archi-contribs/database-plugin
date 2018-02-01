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
	private static final DBLogger logger = new DBLogger(DBGuiExportModel.class);

	private ArchimateModel exportedModel = null;

	private Group grpComponents;
	private Group grpModelVersions;
	
	HashMap<String, DBVersion> newDatabaseComponents;
	
	private boolean forceExport; 
	
	/**
	 * Creates the GUI to export components and model
	 */
	public DBGuiExportModel(ArchimateModel model, String title) throws Exception {
		// We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
		super(title);
		// We reference the exported model 
        exportedModel = model;
		
		includeNeo4j = true;

		popup("Please wait while counting model's components");
		exportedModel.countAllObjects();
		if ( logger.isDebugEnabled() ) logger.debug("the model has got "+model.getAllElements().size()+" elements and "+model.getAllRelationships().size()+" relationships.");
		closePopup();
		
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for exporting model \""+model.getName()+"\" (plugin version "+DBPlugin.pluginVersion+").");
		
		createGrpComponents();
		createGrpModel();
		compoRightBottom.setVisible(true);
		compoRightBottom.layout();

		createAction(ACTION.One, "1 - Confirm export");
		createAction(ACTION.Two, "2 - Export components");
		createAction(ACTION.Three, "3 - Status");

		// we show an arrow in front of the first action
		setActiveAction(ACTION.One);

		// We activate the btnDoAction button : if the user select the "Export" button --> call the exportComponents() method
		setBtnAction("Export", new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				export();
			}
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});

		// We rename the "close" button to "cancel"
		btnClose.setText("Cancel");

		// We activate the Eclipse Help framework
		setHelpHref("exportModel.html");
		
	    getDatabases();
	}

	/**
	 * Creates a group displaying details about the the model in the database (list of existing versions)
	 */
	private void createGrpModel() {
		grpModelVersions = new Group(compoRightBottom, SWT.NONE);
		grpModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
		grpModelVersions.setText("Your model versions : ");
		grpModelVersions.setFont(GROUP_TITLE_FONT);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(grpComponents, -10);
		grpModelVersions.setLayoutData(fd);
		grpModelVersions.setLayout(new FormLayout());

		tblModelVersions = new Table(grpModelVersions,  SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		tblModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
		tblModelVersions.setLinesVisible(true);
		tblModelVersions.setHeaderVisible(true);
		tblModelVersions.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				boolean canExport = (tblModelVersions.getSelection() != null) && (tblModelVersions.getSelection().length > 0) && (tblModelVersions.getSelection()[0] != null);
				
				btnDoAction.setEnabled(canExport);
				
				if ( canExport ) {
                    boolean canChangeMetaData = (connection != null && selectedDatabase.getExportWholeModel() && (tblModelVersions.getSelection()[0] == tblModelVersions.getItem(0)));
                    
                    txtReleaseNote.setEnabled(canChangeMetaData);
                    txtPurpose.setEnabled(canChangeMetaData);
                    txtModelName.setEnabled(canChangeMetaData);
                    btnDoAction.setEnabled(canChangeMetaData);
                    
					txtReleaseNote.setText((String) tblModelVersions.getSelection()[0].getData("note"));
					txtPurpose.setText((String) tblModelVersions.getSelection()[0].getData("purpose"));
					txtModelName.setText((String) tblModelVersions.getSelection()[0].getData("name"));
				}
			}
		});
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(40, -10);
		fd.bottom = new FormAttachment(100, -10);
		tblModelVersions.setLayoutData(fd);

		TableColumn colVersion = new TableColumn(tblModelVersions, SWT.NONE);
		colVersion.setText("#");
		colVersion.setWidth(20);

		TableColumn colCreatedOn = new TableColumn(tblModelVersions, SWT.NONE);
		colCreatedOn.setText("Created on");
		colCreatedOn.setWidth(120);

		TableColumn colCreatedBy = new TableColumn(tblModelVersions, SWT.NONE);
		colCreatedBy.setText("Created by");
		colCreatedBy.setWidth(125);

		Label lblModelName = new Label(grpModelVersions, SWT.NONE);
		lblModelName.setBackground(GROUP_BACKGROUND_COLOR);
		lblModelName.setText("Model name :");
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(40, 0);
		lblModelName.setLayoutData(fd);

		txtModelName = new Text(grpModelVersions, SWT.BORDER);
		txtModelName.setText(exportedModel.getName());
		txtModelName.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblModelName, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblModelName, 80, SWT.LEFT);
		fd.right = new FormAttachment(100, -10);
		txtModelName.setLayoutData(fd);

		Label lblPurpose = new Label(grpModelVersions, SWT.NONE);
		lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
		lblPurpose.setText("Purpose :");
		fd = new FormData();
		fd.top = new FormAttachment(txtModelName, 10);
		fd.left = new FormAttachment(lblModelName, 0, SWT.LEFT);
		lblPurpose.setLayoutData(fd);

		txtPurpose = new Text(grpModelVersions, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		txtPurpose.setText(exportedModel.getPurpose());
		txtPurpose.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(txtModelName, 5);
		fd.left = new FormAttachment(txtModelName, 0, SWT.LEFT);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(55, -5);
		txtPurpose.setLayoutData(fd);

		Label lblReleaseNote = new Label(grpModelVersions, SWT.NONE);
		lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		lblReleaseNote.setText("Release note :");
		fd = new FormData();
		fd.top = new FormAttachment(txtPurpose, 10);
		fd.left = new FormAttachment(lblPurpose, 0, SWT.LEFT);
		lblReleaseNote.setLayoutData(fd);

		txtReleaseNote = new Text(grpModelVersions, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		//txtReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		txtReleaseNote.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(txtPurpose, 5);
		fd.left = new FormAttachment(txtModelName, 0, SWT.LEFT);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		txtReleaseNote.setLayoutData(fd);
	}

	/**
	 * Creates a group displaying details about the exported model's components :<br>
	 * - total number<br>
	 * - number sync'ed with the database<br>
	 * - number that do not exist in the database<br>
	 * - number that exist in the database but with different values. 
	 */
    private void createGrpComponents() {
        grpComponents = new Group(compoRightBottom, SWT.SHADOW_ETCHED_IN);
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

        lblTotal = new Label(grpComponents, SWT.CENTER);
        lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
        lblTotal.setText("Total");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(20, 0);
        fd.right = new FormAttachment(30, 0);
        lblTotal.setLayoutData(fd);
        
        lblModel = new Label(grpComponents, SWT.CENTER);
        lblModel.setBackground(GROUP_BACKGROUND_COLOR);
        lblModel.setText("Model");
        fd = new FormData();
        fd.top = new FormAttachment(0, -8);
        fd.left = new FormAttachment(35, 0);
        fd.right = new FormAttachment(55, 0);
        lblModel.setLayoutData(fd);
        
        lblModelNew = new Label(grpComponents, SWT.CENTER);
        lblModelNew.setBackground(GROUP_BACKGROUND_COLOR);
        lblModelNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(35, 0);
        fd.right = new FormAttachment(45, -3);
        lblModelNew.setLayoutData(fd);
        
        lblModelUpdated = new Label(grpComponents, SWT.CENTER);
        lblModelUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        lblModelUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(45, 2);
        fd.right = new FormAttachment(55, 0);
        lblModelUpdated.setLayoutData(fd);
        
        lblDatabase = new Label(grpComponents, SWT.CENTER);
        lblDatabase.setBackground(GROUP_BACKGROUND_COLOR);
        lblDatabase.setText("Database");
        fd = new FormData();
        fd.top = new FormAttachment(0, -8);
        fd.left = new FormAttachment(60, 0);
        fd.right = new FormAttachment(80, 0);
        lblDatabase.setLayoutData(fd);
        
        lblDatabaseNew = new Label(grpComponents, SWT.CENTER);
        lblDatabaseNew.setBackground(GROUP_BACKGROUND_COLOR);
        lblDatabaseNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(60, 0);
        fd.right = new FormAttachment(70, -3);
        lblDatabaseNew.setLayoutData(fd);
        
        lblDatabaseUpdated = new Label(grpComponents, SWT.CENTER);
        lblDatabaseUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        lblDatabaseUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(70, 2);
        fd.right = new FormAttachment(80, 0);
        lblDatabaseUpdated.setLayoutData(fd);
        
        lblConflict = new Label(grpComponents, SWT.CENTER);
        lblConflict.setBackground(GROUP_BACKGROUND_COLOR);
        lblConflict.setText("Conflict");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(85, 0);
        fd.right = new FormAttachment(95, 0);
        lblConflict.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
        txtTotalElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalElements.setLayoutData(fd);

        txtNewElementsInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewElementsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelNew, 0, SWT.RIGHT);
        txtNewElementsInModel.setLayoutData(fd);
        
        txtUpdatedElementsInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedElementsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelUpdated, 0, SWT.RIGHT);
        txtUpdatedElementsInModel.setLayoutData(fd);
        
        txtNewElementsInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewElementsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseNew, 0, SWT.RIGHT);
        txtNewElementsInDatabase.setLayoutData(fd);
        
        txtUpdatedElementsInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedElementsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseUpdated, 0, SWT.RIGHT);
        txtUpdatedElementsInDatabase.setLayoutData(fd);
        
        txtConflictingElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtConflictingElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblConflict, 0, SWT.RIGHT);
        txtConflictingElements.setLayoutData(fd);

        /* * * * * */
        
        txtTotalRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
        txtTotalRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalRelationships.setLayoutData(fd);

        txtNewRelationshipsInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewRelationshipsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelNew, 0, SWT.RIGHT);
        txtNewRelationshipsInModel.setLayoutData(fd);
        
        txtUpdatedRelationshipsInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedRelationshipsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelUpdated, 0, SWT.RIGHT);
        txtUpdatedRelationshipsInModel.setLayoutData(fd);
        
        txtNewRelationshipsInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewRelationshipsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseNew, 0, SWT.RIGHT);
        txtNewRelationshipsInDatabase.setLayoutData(fd);
        
        txtUpdatedRelationshipsInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedRelationshipsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseUpdated, 0, SWT.RIGHT);
        txtUpdatedRelationshipsInDatabase.setLayoutData(fd);
        
        txtConflictingRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtConflictingRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblConflict, 0, SWT.RIGHT);
        txtConflictingRelationships.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
        txtTotalFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalFolders.setLayoutData(fd);

        txtNewFoldersInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewFoldersInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelNew, 0, SWT.RIGHT);
        txtNewFoldersInModel.setLayoutData(fd);
        
        txtUpdatedFoldersInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedFoldersInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelUpdated, 0, SWT.RIGHT);
        txtUpdatedFoldersInModel.setLayoutData(fd);
        
        txtNewFoldersInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewFoldersInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseNew, 0, SWT.RIGHT);
        txtNewFoldersInDatabase.setLayoutData(fd);
        
        txtUpdatedFoldersInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedFoldersInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseUpdated, 0, SWT.RIGHT);
        txtUpdatedFoldersInDatabase.setLayoutData(fd);
        
        txtConflictingFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtConflictingFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblConflict, 0, SWT.RIGHT);
        txtConflictingFolders.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViews.setText(String.valueOf(exportedModel.getAllViews().size()));
        txtTotalViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViews.setLayoutData(fd);

        txtNewViewsInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewViewsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelNew, 0, SWT.RIGHT);
        txtNewViewsInModel.setLayoutData(fd);
        
        txtUpdatedViewsInModel = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedViewsInModel.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblModelUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblModelUpdated, 0, SWT.RIGHT);
        txtUpdatedViewsInModel.setLayoutData(fd);
        
        txtNewViewsInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewViewsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseNew, 0, SWT.RIGHT);
        txtNewViewsInDatabase.setLayoutData(fd);
        
        txtUpdatedViewsInDatabase = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedViewsInDatabase.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblDatabaseUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblDatabaseUpdated, 0, SWT.RIGHT);
        txtUpdatedViewsInDatabase.setLayoutData(fd);
        
        txtConflictingViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtConflictingViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblConflict, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblConflict, 0, SWT.RIGHT);
        txtConflictingViews.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViewObjects.setText(String.valueOf(exportedModel.getAllViewObjects().size()));
        txtTotalViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViewObjects.setLayoutData(fd);
  
        /* * * * * */
        
        txtTotalViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViewConnections.setText(String.valueOf(exportedModel.getAllViewConnections().size()));
        txtTotalViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViewConnections.setLayoutData(fd);

        /* * * * * */
        
        txtTotalImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalImages.setText(String.valueOf(exportedModel.getAllImagePaths().size()));
        txtTotalImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalImages.setLayoutData(fd);
    }

	/**
	 * This method is called each time a database is selected and a connection has been established to it.<br>
	 * <br>
	 * It enables the export button and starts the export if the "automatic start" is specified in the plugin preferences
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheckDatabase) {
		// we hide the database and conflict columns in standalone mode, and show them in collaborative mode
		lblDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		lblDatabaseNew.setVisible(selectedDatabase.getCollaborativeMode());
		lblDatabaseUpdated.setVisible(selectedDatabase.getCollaborativeMode());
		lblConflict.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewElementsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedElementsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingElements.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewRelationshipsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedRelationshipsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingRelationships.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewFoldersInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedFoldersInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingFolders.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewViewsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedViewsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingViews.setVisible(selectedDatabase.getCollaborativeMode());

		
		// if we're not in a Neo4J database, then we get the latest version and checksum of the model's components in the database
		try {
			if ( !DBPlugin.areEqual(selectedDatabase.getDriver().toLowerCase(), "neo4j") )
				connection.getModelVersions(exportedModel.getId(), tblModelVersions);
		} catch (Exception err) {
			closePopup();
			popup(Level.FATAL, "Failed to check existing components in database", err);
			setActiveAction(STATUS.Error);
			return;
		}
		
		// if the exportWithDefaultValues preference is set, then we automatically start the export
		if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("exportWithDefaultValues") ) {
			logger.debug("Automatically start export as specified in preferences.");
			btnDoAction.notifyListeners(SWT.Selection, new Event());
		} else {
    		// else, we check what needs to be exported to the database
            txtNewElementsInModel.setText("0");         txtUpdatedElementsInModel.setText("0");         txtNewElementsInDatabase.setText("0");          txtUpdatedElementsInDatabase.setText("0");          txtConflictingElements.setText("0");
            txtNewRelationshipsInModel.setText("0");    txtUpdatedRelationshipsInModel.setText("0");    txtNewRelationshipsInDatabase.setText("0");     txtUpdatedRelationshipsInDatabase.setText("0");     txtConflictingRelationships.setText("0");
            txtNewFoldersInModel.setText("0");          txtUpdatedFoldersInModel.setText("0");          txtNewFoldersInDatabase.setText("0");           txtUpdatedFoldersInDatabase.setText("0");           txtConflictingFolders.setText("0");
            txtNewViewsInModel.setText("0");            txtUpdatedViewsInModel.setText("0");            txtNewViewsInDatabase.setText("0");             txtUpdatedViewsInDatabase.setText("0");             txtConflictingViews.setText("0");
            txtNewElementsInModel.setText("0");         txtUpdatedElementsInModel.setText("0");         txtNewElementsInDatabase.setText("0");          txtUpdatedElementsInDatabase.setText("0");          txtConflictingElements.setText("0");
    
            try {
                popup("Please wait while comparing model from the database");
                connection.getVersionsFromDatabase(exportedModel);
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
            forceExport = exportedModel.getCurrentVersion().getVersion() == 0
                    || exportedModel.getCurrentVersion().getLatestVersion() == exportedModel.getCurrentVersion().getVersion()
                    || !selectedDatabase.getCollaborativeMode();
    
            int nbNews = 0;
            int nbUpdated = 0;
            int nbUpdatedDb = 0;
            int nbConflict = 0;
            Iterator<Map.Entry<String, IArchimateElement>> ite = exportedModel.getAllElements().entrySet().iterator();
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
                            if ( forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            txtNewElementsInModel.setText(String.valueOf(nbNews));
            txtUpdatedElementsInModel.setText(String.valueOf(nbUpdated));
            txtUpdatedElementsInDatabase.setText(String.valueOf(nbUpdatedDb));
            txtNewElementsInDatabase.setText(String.valueOf(connection.getElementsNotInModel().size()));
            txtConflictingElements.setText(String.valueOf(nbConflict));
            
            nbNews = 0;
            nbUpdated = 0;
            nbUpdatedDb = 0;
            nbConflict = 0;
            Iterator<Map.Entry<String, IArchimateRelationship>> itr = exportedModel.getAllRelationships().entrySet().iterator();
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
                            if ( forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            txtNewRelationshipsInModel.setText(String.valueOf(nbNews));
            txtUpdatedRelationshipsInModel.setText(String.valueOf(nbUpdated));
            txtUpdatedRelationshipsInDatabase.setText(String.valueOf(nbUpdatedDb));
            txtNewRelationshipsInDatabase.setText(String.valueOf(connection.getRelationshipsNotInModel().size()));
            txtConflictingRelationships.setText(String.valueOf(nbConflict));
            
            nbNews = 0;
            nbUpdated = 0;
            nbUpdatedDb = 0;
            nbConflict = 0;
            Iterator<Map.Entry<String, IFolder>> itf = exportedModel.getAllFolders().entrySet().iterator();
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
                            if ( forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            txtNewFoldersInModel.setText(String.valueOf(nbNews));
            txtUpdatedFoldersInModel.setText(String.valueOf(nbUpdated));
            txtUpdatedFoldersInDatabase.setText(String.valueOf(nbUpdatedDb));
            txtNewFoldersInDatabase.setText(String.valueOf(connection.getFoldersNotInModel().size()));
            txtConflictingFolders.setText(String.valueOf(nbConflict));
            
            nbNews = 0;
            nbUpdated = 0;
            nbUpdatedDb = 0;
            nbConflict = 0;
            Iterator<Map.Entry<String, IDiagramModel>> itv = exportedModel.getAllViews().entrySet().iterator();
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
                            if ( forceExport )          ++nbUpdated;
                            else {                      ++nbConflict; metadata.setConflictChoice(CONFLICT_CHOICE.askUser); }
                        } else {
                            if ( modifiedInModel )      ++nbUpdated;
                            if ( modifiedInDatabase )   ++nbUpdatedDb;
                        }
                    }
                }
            }
            txtNewViewsInModel.setText(String.valueOf(nbNews));
            txtUpdatedViewsInModel.setText(String.valueOf(nbUpdated));
            txtUpdatedViewsInDatabase.setText(String.valueOf(nbUpdatedDb));
            txtNewViewsInDatabase.setText(String.valueOf(connection.getViewsNotInModel().size()));
            txtConflictingViews.setText(String.valueOf(nbConflict));
            
            closePopup();
    		
            if ( txtNewElementsInModel.getText().equals("0") && txtNewRelationshipsInModel.getText().equals("0") && txtNewFoldersInModel.getText().equals("0") && txtNewViewsInModel.getText().equals("0") &&
                    txtUpdatedElementsInModel.getText().equals("0") && txtUpdatedRelationshipsInModel.getText().equals("0") && txtUpdatedFoldersInModel.getText().equals("0") && txtUpdatedViewsInModel.getText().equals("0") && 
                    txtNewElementsInDatabase.getText().equals("0") && txtNewRelationshipsInDatabase.getText().equals("0") && txtNewFoldersInDatabase.getText().equals("0") && txtNewViewsInDatabase.getText().equals("0") &&
                    txtUpdatedElementsInDatabase.getText().equals("0") && txtUpdatedRelationshipsInDatabase.getText().equals("0") && txtUpdatedFoldersInDatabase.getText().equals("0") && txtUpdatedViewsInDatabase.getText().equals("0") &&
                    txtConflictingElements.getText().equals("0") && txtConflictingRelationships.getText().equals("0") && txtConflictingFolders.getText().equals("0") && txtConflictingViews.getText().equals("0") ) {
                popup(Level.INFO, "Your database is already up to date.");
                if ( logger.isDebugEnabled() ) logger.debug("Disabling the \"Export\" button.");
                btnDoAction.setEnabled(false);
                
                exportedModel.getDatabaseVersion().setTimestamp(exportedModel.getDatabaseVersion().getLatestTimestamp());
            } else {
                if ( logger.isDebugEnabled() ) logger.debug("Enabling the \"Export\" button.");
                btnDoAction.setEnabled(true);
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
		btnDoAction.setEnabled(false);
		
		// we hide the database and conflict columns in standalone mode, and show them in collaborative mode
		lblDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		lblDatabaseNew.setVisible(selectedDatabase.getCollaborativeMode());
		lblDatabaseUpdated.setVisible(selectedDatabase.getCollaborativeMode());
		lblConflict.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewElementsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedElementsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingElements.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewRelationshipsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedRelationshipsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingRelationships.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewFoldersInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedFoldersInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingFolders.setVisible(selectedDatabase.getCollaborativeMode());
		txtNewViewsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtUpdatedViewsInDatabase.setVisible(selectedDatabase.getCollaborativeMode());
		txtConflictingViews.setVisible(selectedDatabase.getCollaborativeMode());
		
		txtNewElementsInModel.setText("");			txtUpdatedElementsInModel.setText("");			txtNewElementsInDatabase.setText("");			txtUpdatedElementsInDatabase.setText("");			txtConflictingElements.setText("");
		txtNewRelationshipsInModel.setText("");		txtUpdatedRelationshipsInModel.setText("");		txtNewRelationshipsInDatabase.setText("");		txtUpdatedRelationshipsInDatabase.setText("");		txtConflictingRelationships.setText("");
		txtNewFoldersInModel.setText("");			txtUpdatedFoldersInModel.setText("");			txtNewFoldersInDatabase.setText("");			txtUpdatedFoldersInDatabase.setText("");			txtConflictingFolders.setText("");
		txtNewViewsInModel.setText("");				txtUpdatedViewsInModel.setText("");				txtNewViewsInDatabase.setText("");				txtUpdatedViewsInDatabase.setText("");				txtConflictingViews.setText("");
		txtNewElementsInModel.setText("");			txtUpdatedElementsInModel.setText("");			txtNewElementsInDatabase.setText("");			txtUpdatedElementsInDatabase.setText("");			txtConflictingElements.setText("");
	}

	/**
	 * Loop on model components and call doExportEObject to export them<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button
	 */
	protected void export() {
		int progressBarWidth;
		if ( selectedDatabase.getExportWholeModel() ) {
			logger.info("Exporting model : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships, "+exportedModel.getAllFolders().size()+" folders, "+exportedModel.getAllViews().size()+" views, "+exportedModel.getAllViewObjects().size()+" views objects, "+exportedModel.getAllViewConnections().size()+" views connections, and "+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()+" images.");
			progressBarWidth = exportedModel.getAllFolders().size()+exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size()+exportedModel.getAllViews().size()+exportedModel.getAllViewObjects().size()+exportedModel.getAllViewConnections().size()+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size();
		} else {
			logger.info("Exporting components : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships.");
			progressBarWidth = exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size();
		}
		
		// we disable the export button to avoid a second click
		btnDoAction.setEnabled(false);

		// we disable the option between an whole model export or a components only export
		disableOption();

		// the we disable the name, purpose and release note text fields
		txtModelName.setEnabled(false);
		txtPurpose.setEnabled(false);
		txtReleaseNote.setEnabled(false);

		// we force the modelVersion and component groups to be visible (in case we come from the conflict resolution)
		grpComponents.setVisible(true);
		grpModelVersions.setVisible(true);
		
		// We show up a small arrow in front of the second action "export components"
        setActiveAction(STATUS.Ok);
        setActiveAction(ACTION.Two);

		// We hide the grpDatabase and create a progressBar instead 
		hideGrpDatabase();
		createProgressBar("Exporting components ...", 0, progressBarWidth);
		createGrpConflict();
		
		// we calculate the new model checksum
		try {
            exportedModel.getCurrentVersion().setLatestChecksum(DBChecksum.calculateChecksum(exportedModel, txtReleaseNote.getText()));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException err) {
            popup(Level.FATAL, "Failed to calculate the model's checksum.", err);
            setActiveAction(STATUS.Error);
            doShowResult(STATUS.Error, "Failed to calculate the model's checksum.\n"+err.getMessage());
            return;
        }

		// then, we start a new database transaction
		try {
		    connection.setAutoCommit(false);
		} catch (SQLException err ) {
			popup(Level.FATAL, "Failed to create a transaction in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return;
		}

		// we reset the counters as they will be updated by the doExportEObject method
	    txtNewElementsInModel.setText("0");         txtUpdatedElementsInModel.setText("0");         txtNewElementsInDatabase.setText("0");          txtUpdatedElementsInDatabase.setText("0");          txtConflictingElements.setText("0");
        txtNewRelationshipsInModel.setText("0");    txtUpdatedRelationshipsInModel.setText("0");    txtNewRelationshipsInDatabase.setText("0");     txtUpdatedRelationshipsInDatabase.setText("0");     txtConflictingRelationships.setText("0");
        txtNewFoldersInModel.setText("0");          txtUpdatedFoldersInModel.setText("0");          txtNewFoldersInDatabase.setText("0");           txtUpdatedFoldersInDatabase.setText("0");           txtConflictingFolders.setText("0");
        txtNewViewsInModel.setText("0");            txtUpdatedViewsInModel.setText("0");            txtNewViewsInDatabase.setText("0");             txtUpdatedViewsInDatabase.setText("0");             txtConflictingViews.setText("0");
        txtNewElementsInModel.setText("0");         txtUpdatedElementsInModel.setText("0");         txtNewElementsInDatabase.setText("0");          txtUpdatedElementsInDatabase.setText("0");          txtConflictingElements.setText("0");

		
		try {
			// we need to recalculate the latest versions in the database in case someone updated the database since the last check
			// TODO : add a transaction number that will speed up the export process in case nobody updated the database 
			connection.getVersionsFromDatabase(exportedModel);
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
        forceExport = exportedModel.getCurrentVersion().getVersion() == 0
                || exportedModel.getCurrentVersion().getLatestVersion() == exportedModel.getCurrentVersion().getVersion()
                || !selectedDatabase.getCollaborativeMode();
		
		// we export the components
		try {
			// if we need to save the whole model (i.e. not only the elements and the relationships) 
			if ( selectedDatabase.getExportWholeModel() ) {
				// We update the model name and purpose in case they've been changed in the export windows
				if ( !DBPlugin.areEqual(exportedModel.getName(), txtModelName.getText()) )
					exportedModel.setName(txtModelName.getText());

				if ( !DBPlugin.areEqual(exportedModel.getPurpose(), txtPurpose.getText()) )
					exportedModel.setPurpose(txtPurpose.getText());

				if ( logger.isDebugEnabled() ) logger.debug("Exporting version "+exportedModel.getCurrentVersion().getLatestVersion()+" of the model ("+exportedModel.getCurrentVersion().getTimestamp().toString()+")");
				connection.exportModel(exportedModel, txtReleaseNote.getText());
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting folders");
				Iterator<Entry<String, IFolder>> foldersIterator = exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) {
					doExportEObject(foldersIterator.next().getValue(), txtNewFoldersInModel, txtUpdatedFoldersInModel, txtNewFoldersInDatabase, txtUpdatedFoldersInDatabase, txtConflictingFolders);
				}
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting elements");
			Iterator<Entry<String, IArchimateElement>> elementsIterator = exportedModel.getAllElements().entrySet().iterator();
			while ( elementsIterator.hasNext() ) {
				doExportEObject(elementsIterator.next().getValue(), txtNewElementsInModel, txtUpdatedElementsInModel, txtNewElementsInDatabase, txtUpdatedElementsInDatabase, txtConflictingElements);
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting relationships");
			Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = exportedModel.getAllRelationships().entrySet().iterator();
			while ( relationshipsIterator.hasNext() ) {
				doExportEObject(relationshipsIterator.next().getValue(), txtNewRelationshipsInModel, txtUpdatedRelationshipsInModel, txtNewRelationshipsInDatabase, txtUpdatedRelationshipsInDatabase, txtConflictingRelationships);
			}
	
			if ( selectedDatabase.getExportWholeModel() ) {
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views");
				Iterator<Entry<String, IDiagramModel>> viewsIterator = exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue(); 
					if ( doExportEObject(view, txtNewViewsInModel, txtUpdatedViewsInModel, txtNewViewsInDatabase, txtUpdatedViewsInDatabase, txtConflictingViews) ) {
					    connectionsAlreadyExported = new HashMap<String, IDiagramModelConnection>();      // we need to memorize exported connections as they can be get as sources AND as targets 
	                    for ( IDiagramModelObject viewObject: view.getChildren() ) {
	                        doExportViewObject(viewObject);
	                    }
					}
				}
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting images");
		    	IArchiveManager archiveMgr = (IArchiveManager)exportedModel.getAdapter(IArchiveManager.class);
				for ( String path: exportedModel.getAllImagePaths() ) {
					connection.exportImage(path, archiveMgr.getBytesFromEntry(path));
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
			    connection.rollback();
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
				return;
			} catch (SQLException err2) {
				popup(Level.FATAL, "The transaction failed to rollback and the database is left in an inconsistent state.\n\nPlease check carrefully your database !", err2);
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err2.getMessage());
				return;
			}
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Found "+tblListConflicts.getItemCount()+" components conflicting with database");
		if ( tblListConflicts.getItemCount() == 0 ) {
			// the export is successfull
			try  {
				// we check if something has been really exported				
				if ( selectedDatabase.getExportWholeModel() ) {
			        if ( txtNewElementsInModel.getText().equals("0") && txtNewRelationshipsInModel.getText().equals("0") && txtNewFoldersInModel.getText().equals("0") && txtNewViewsInModel.getText().equals("0") &&
			                txtUpdatedElementsInModel.getText().equals("0") && txtUpdatedRelationshipsInModel.getText().equals("0") && txtUpdatedFoldersInModel.getText().equals("0") && txtUpdatedViewsInModel.getText().equals("0") && 
			                txtNewElementsInDatabase.getText().equals("0") && txtNewRelationshipsInDatabase.getText().equals("0") && txtNewFoldersInDatabase.getText().equals("0") && txtNewViewsInDatabase.getText().equals("0") &&
			                txtUpdatedElementsInDatabase.getText().equals("0") && txtUpdatedRelationshipsInDatabase.getText().equals("0") && txtUpdatedFoldersInDatabase.getText().equals("0") && txtUpdatedViewsInDatabase.getText().equals("0") &&
			                txtConflictingElements.getText().equals("0") && txtConflictingRelationships.getText().equals("0") && txtConflictingFolders.getText().equals("0") && txtConflictingViews.getText().equals("0") &&   
							exportedModel.getCurrentVersion().getLatestChecksum().equals(exportedModel.getDatabaseVersion().getChecksum()) ) {
						connection.rollback();
					    connection.setAutoCommit(true);
						setActiveAction(STATUS.Ok);
						setComponentVersion();
						doShowResult(STATUS.Ok, "The database is already up to date.");
				        //TODO: if the model is not in the database, we force its export even if all the components are the same version !!!!!
						//TODO:
						//TODO:
						//TODO:
						return;
					}
				}
			    connection.commit();
			    connection.setAutoCommit(true);
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
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Export of components incomplete. Conflicts need to be manually resolved.");
			resetProgressBar();
			try  {
			    connection.rollback();
			} catch (Exception err) {
				popup(Level.FATAL, "Failed to rollback the transaction. Please check carrefully your database !", err);
				setActiveAction(STATUS.Error);
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
				return;
			}
		
			tblListConflicts.setSelection(0);
			try {
				tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
			} catch (Exception err) {
				popup(Level.ERROR, "Failed to compare component with its database version.", err);
				setActiveAction(STATUS.Error);
				doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
				return;
			}
		}
	}
	
	/**
	 * this method should be called after the model has been successfully exported to the database.<br>
	 * <br>
	 * it copies the latestVestion to the currentVersion, latestTimestamp to CurrentTimestamp and latestChecksum to currentChecksum
	 */
	private void setComponentVersion() {
	    exportedModel.getCurrentVersion().setChecksum(exportedModel.getCurrentVersion().getLatestChecksum());
	    exportedModel.getCurrentVersion().setVersion(exportedModel.getCurrentVersion().getLatestVersion());
	    exportedModel.getCurrentVersion().setTimestamp(exportedModel.getCurrentVersion().getLatestTimestamp());
	    
	    Iterator<Map.Entry<String, IArchimateElement>> ite = exportedModel.getAllElements().entrySet().iterator();
        while (ite.hasNext()) {
            DBVersion version = ((IDBMetadata)ite.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(exportedModel.getCurrentVersion().getLatestTimestamp());
        }
        
        Iterator<Map.Entry<String, IArchimateRelationship>> itr = exportedModel.getAllRelationships().entrySet().iterator();
        while (itr.hasNext()) {
            DBVersion version = ((IDBMetadata)itr.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(exportedModel.getCurrentVersion().getLatestTimestamp());
        }
        
        Iterator<Map.Entry<String, IFolder>> itf = exportedModel.getAllFolders().entrySet().iterator();
        while (itf.hasNext()) {
            DBVersion version = ((IDBMetadata)itf.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(exportedModel.getCurrentVersion().getLatestTimestamp());
        }
        
        Iterator<Map.Entry<String, IDiagramModel>> itv = exportedModel.getAllViews().entrySet().iterator();
        while (itv.hasNext()) {
            DBVersion version = ((IDBMetadata)itv.next().getValue()).getDBMetadata().getCurrentVersion();
            version.setVersion(version.getLatestVersion());
            version.setChecksum(version.getLatestChecksum());
            version.setTimestamp(exportedModel.getCurrentVersion().getLatestTimestamp());
        }
	}

	Map<String, IDiagramModelConnection> connectionsAlreadyExported;
	private void doExportViewObject(IDiagramModelObject viewObject) throws Exception {
		if ( logger.isTraceEnabled() ) logger.trace("exporting view object "+((IDBMetadata)viewObject).getDBMetadata().getDebugName());
		boolean exported = doExportEObject(viewObject, null, null, null, null, null);
		
		if ( exported ) {
			if ( viewObject instanceof IConnectable) {
				for ( IDiagramModelConnection source: ((IConnectable)viewObject).getSourceConnections() ) {
					if ( connectionsAlreadyExported.get(source.getId()) == null ) {
						doExportEObject(source, null, null, null, null, null);
						connectionsAlreadyExported.put(source.getId(), source);
					}
				}
				for ( IDiagramModelConnection target: ((IConnectable)viewObject).getTargetConnections() ) {
					if ( connectionsAlreadyExported.get(target.getId()) == null ) {
						doExportEObject(target, null, null, null, null, null);
						connectionsAlreadyExported.put(target.getId(), target);
					}
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
	private boolean doExportEObject(EObject eObjectToExport, Text txtNewInModel, Text txtUpdatedInModel, Text txtNewInDatabase, Text txtUpdatedInDatabase, Text txtConflicting) throws Exception {
		assert(eObjectToExport instanceof IDBMetadata);
		assert(connection != null);
		
		boolean mustExport = false;
		boolean mustImport = false;
		boolean exported = false;
		
		if ( DBPlugin.areEqual(selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
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
        				if ( forceExport )
            		        mustExport = true;
        				else {
        					if ( logger.isDebugEnabled() ) logger.debug("The component conflicts with the version in the database.");
        					switch ( ((IDBMetadata)eObjectToExport).getDBMetadata().getConflictChoice() ) {
        						case askUser :
        							if ( logger.isDebugEnabled() ) logger.debug("The conflict has to be manually resolved by user.");
        	                    	new TableItem(tblListConflicts, SWT.NONE).setText(((IIdentifier)eObjectToExport).getId());
        	                    	if ( tblListConflicts.getItemCount() < 2 )
        	                    		lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" component conflicts with newer version in the database :");
        	                    	else
        	                    		lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" components conflict with newer version in the database :");
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
			connection.exportEObject(eObjectToExport);
            if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion().getLatestVersion() == 0 )
            	incrementText(txtNewInModel);
            else
                incrementText(txtUpdatedInModel);
            exported = true;
		}
		
		if ( mustImport ) {
            // For the moment, we can import elements and relationships only during an export !!!
            if ( eObjectToExport instanceof IArchimateElement ) {
                eObjectToExport = connection.importElementFromId(exportedModel, null, ((IIdentifier)eObjectToExport).getId(), false);
            } else if ( eObjectToExport instanceof IArchimateRelationship ) {
                eObjectToExport = connection.importRelationshipFromId(exportedModel, null, ((IIdentifier)eObjectToExport).getId(), false);
                incrementText(connection.getRelationshipsNotInModel().get(((IIdentifier)eObjectToExport).getId()) != null ? txtNewInDatabase : txtUpdatedInDatabase);
            } else
            	throw new Exception ("Cannot import a "+eObjectToExport.getClass().getSimpleName()+" during an export :(");
		}
		
		
		// even if the eObject is not exported, it has to be referenced as beeing part of the model
		if ( selectedDatabase.getExportWholeModel() )
			connection.assignEObjectToModel(eObjectToExport);
		
		increaseProgressBar();
		return exported;
	}

	/**
	 * Creates a group that will display the conflicts raised during the export process
	 */
	protected void createGrpConflict() {		
		if ( grpConflict == null ) {
			grpConflict = new Group(compoRightBottom, SWT.NONE);
			grpConflict.setBackground(GROUP_BACKGROUND_COLOR);
			grpConflict.setFont(TITLE_FONT);
			grpConflict.setText("Conflict : ");
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			fd.bottom = new FormAttachment(100);
			grpConflict.setLayoutData(fd);
			grpConflict.setLayout(new FormLayout());
	
			lblCantExport = new Label(grpConflict, SWT.NONE);
			lblCantExport.setBackground(GROUP_BACKGROUND_COLOR);
			lblCantExport.setText("Can't export because some components conflict with newer version in the database :");
			fd = new FormData();
			fd.top = new FormAttachment(0, 10);
			fd.left = new FormAttachment(0, 10);
			lblCantExport.setLayoutData(fd);
	
			tblListConflicts = new Table(grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
			tblListConflicts.setLinesVisible(true);
			tblListConflicts.setBackground(GROUP_BACKGROUND_COLOR);
			tblListConflicts.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					// we search for the component that is conflicting
					String id = tblListConflicts.getSelection()[0].getText();
	
					EObject conflictingComponent = exportedModel.getAllElements().get(id);
					if ( conflictingComponent == null ) conflictingComponent = exportedModel.getAllRelationships().get(id);
					if ( conflictingComponent == null ) conflictingComponent = exportedModel.getAllFolders().get(id);
					if ( conflictingComponent == null ) conflictingComponent = exportedModel.getAllViews().get(id);
	
					if ( conflictingComponent == null ) {
						btnExportMyVersion.setEnabled(false);
						btnDoNotExport.setEnabled(false);
						btnImportDatabaseVersion.setEnabled(false);
						tblCompareComponent.removeAll();
						popup(Level.ERROR, "Do not know which component is conflicting !!! That's weird !!!");
					} else {				
						btnExportMyVersion.setEnabled(true);
						btnDoNotExport.setEnabled(true);
						btnImportDatabaseVersion.setEnabled( (conflictingComponent instanceof IArchimateElement) || (conflictingComponent instanceof IArchimateRelationship) );
	
						fillInCompareTable(tblCompareComponent, conflictingComponent, ((IDBMetadata)conflictingComponent).getDBMetadata().getDatabaseVersion().getLatestVersion(), null);
					}
					grpComponents.setVisible(false);
					grpModelVersions.setVisible(false);
					grpConflict.setVisible(true);
					compoRightBottom.layout();
				}
			});
			fd = new FormData();
			fd.top = new FormAttachment(lblCantExport, 10);
			fd.left = new FormAttachment(25);
			fd.right = new FormAttachment(75);
			fd.bottom = new FormAttachment(40);
			tblListConflicts.setLayoutData(fd);
	
			Label lblCompare = new Label(grpConflict, SWT.NONE);
			lblCompare.setBackground(GROUP_BACKGROUND_COLOR);
			lblCompare.setText("Please verify your version against the latest version in the database :");
			fd = new FormData();
			fd.top = new FormAttachment(tblListConflicts, 20);
			fd.left = new FormAttachment(0, 10);
			lblCompare.setLayoutData(fd);
	
			tblCompareComponent = new Tree(grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.V_SCROLL);
			tblCompareComponent.setBackground(GROUP_BACKGROUND_COLOR);
			tblCompareComponent.setHeaderVisible(true);
			tblCompareComponent.setLinesVisible(true);
			fd = new FormData();
			fd.top = new FormAttachment(lblCompare, 10);
			fd.left = new FormAttachment(0,10);
			fd.right = new FormAttachment(100, -10);
			fd.bottom = new FormAttachment(100, -40);
			tblCompareComponent.setLayoutData(fd);
	
			TreeColumn colItems = new TreeColumn(tblCompareComponent, SWT.NONE);
			colItems.setText("Items");
			colItems.setWidth(119);
	
			TreeColumn colYourVersion = new TreeColumn(tblCompareComponent, SWT.NONE);
			colYourVersion.setText("Your version");
			colYourVersion.setWidth(170);
	
			TreeColumn colDatabaseVersion = new TreeColumn(tblCompareComponent, SWT.NONE);
			colDatabaseVersion.setText("Database version");
			colDatabaseVersion.setWidth(170);
	
			btnImportDatabaseVersion = new Button(grpConflict, SWT.NONE);
			btnImportDatabaseVersion.setImage(IMPORT_FROM_DATABASE_IMAGE);
			btnImportDatabaseVersion.setText("Import");
			btnImportDatabaseVersion.setEnabled(false);
			btnImportDatabaseVersion.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) { 
					if ( checkRememberChoice.getSelection() ) {
						// if the button checkRememberChoice is checked, then we apply the choice for all the conflicting components.
						// at the end, only those with errors will stay
						tblListConflicts.setSelection(0);
						for ( int i=0; i<tblListConflicts.getItemCount(); ++i)
							tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.importFromDatabase);
					} else {
						// we only apply the choice to the selected component
						tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.importFromDatabase);
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
			});
			fd = new FormData(80,25);
			fd.right = new FormAttachment(100, -10);
			fd.bottom = new FormAttachment(100, -10);
			btnImportDatabaseVersion.setLayoutData(fd);
	
			btnExportMyVersion = new Button(grpConflict, SWT.NONE);
			btnExportMyVersion.setImage(EXPORT_TO_DATABASE_IMAGE);
			btnExportMyVersion.setText("Export");
			btnExportMyVersion.setEnabled(false);
			btnExportMyVersion.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) { 
					if ( checkRememberChoice.getSelection() ) {
						// if the button checkRememberChoice is checked, then we apply the choice for all the conflicting components.
						// at the end, only those with errors will stay
						tblListConflicts.setSelection(0);
						for ( int i=0; i<tblListConflicts.getItemCount(); ++i)
							tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.exportToDatabase);
					} else {
						// we only apply the choice to the selected component
						tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.exportToDatabase);
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
			});
			fd = new FormData(80,25);
			fd.right = new FormAttachment(btnImportDatabaseVersion, -10);
			fd.bottom = new FormAttachment(100, -10);
			btnExportMyVersion.setLayoutData(fd);
	
			btnDoNotExport = new Button(grpConflict, SWT.NONE);
			btnDoNotExport.setText("Do not export");
			btnDoNotExport.setEnabled(false);
			btnDoNotExport.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) { 
					if ( checkRememberChoice.getSelection() ) {
						// if the button checkRememberChoice is checked, then we apply the choice for all the conflicting components.
						// at the end, only those with errors will stay
						tblListConflicts.setSelection(0);
						for ( int i=0; i<tblListConflicts.getItemCount(); ++i)
							tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.doNotExport);
					} else {
						// we only apply the choice to the selected component
						tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.doNotExport);
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
			});
			fd = new FormData(80,25);
			fd.right = new FormAttachment(btnExportMyVersion, -10);
			fd.bottom = new FormAttachment(100, -10);
			btnDoNotExport.setLayoutData(fd);
	
			checkRememberChoice = new Button(grpConflict, SWT.CHECK);
			checkRememberChoice.setText("Remember my choice");
			fd = new FormData();
			fd.right = new FormAttachment(btnDoNotExport, -20);
			fd.top = new FormAttachment(btnDoNotExport, 0, SWT.CENTER);
			checkRememberChoice.setLayoutData(fd);
	
			grpConflict.layout();
		} else {
			grpConflict.setVisible(true);
			tblListConflicts.removeAll();
			tblCompareComponent.removeAll();
		}
	}
	
	/**
	 * called when the user click on the btnExportMyVersion button<br>
	 * Sets the exportChoice on the component's DBmetadata and removes the component from the tblListconflicts table<br>
	 * If no conflict remain, then it relaunch the doExportComponents method 
	 */
	protected void tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE requiredChoice) {
		CONFLICT_CHOICE effectiveChoice = requiredChoice;
		EObject component = exportedModel.getAllElements().get(tblListConflicts.getSelection()[0].getText());
		if ( component == null ) component = exportedModel.getAllRelationships().get(tblListConflicts.getSelection()[0].getText());
		if ( component == null ) {
			component = exportedModel.getAllFolders().get(tblListConflicts.getSelection()[0].getText());
			if ( component == null ) component = exportedModel.getAllViews().get(tblListConflicts.getSelection()[0].getText());
			if ( component == null ) {
				popup(Level.ERROR, "Can't get conflicting component \""+tblListConflicts.getSelection()[0].getText()+"\"");
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
		}

		int index = tblListConflicts.getSelectionIndex();
		tblListConflicts.remove(index);
		if ( logger.isDebugEnabled() ) logger.debug("Remaining " + tblListConflicts.getItemCount() + " conflicts");
		if ( tblListConflicts.getItemCount() == 0 ) {
			grpComponents.setVisible(true);
			grpModelVersions.setVisible(true);
			grpConflict.setVisible(false);
			export();
		} else {
			if ( tblListConflicts.getItemCount() < 2 )
				lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" component conflicts with newer version in the database :");
			else
				lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" components conflict with newer version in the database :");

			if ( index < tblListConflicts.getItemCount() )
				tblListConflicts.setSelection(index);
			else
				tblListConflicts.setSelection(index-1);
			tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
		}
	}

	protected void doShowResult(STATUS status, String message) {
		logger.debug("Showing result.");
		if ( grpProgressBar != null ) grpProgressBar.setVisible(false);
		if ( grpConflict != null ) grpConflict.setVisible(false);
		grpComponents.setVisible(true);
		grpModelVersions.setVisible(true);

		setActiveAction(ACTION.Three);
		
		if ( logger.isTraceEnabled() ) logger.trace("Model : "+txtTotalElements.getText()+" elements, "+txtTotalRelationships.getText()+" relationships, "+txtTotalFolders.getText()+" folders, "+txtTotalViews.getText()+" views, "+txtTotalViewObjects.getText()+" view objects, "+txtTotalViewConnections.getText()+" view connections.");
		
		if ( status == STATUS.Ok ) {
			setMessage(message, GREEN_COLOR);
			if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("closeIfSuccessful") ) {
				if ( logger.isDebugEnabled() ) logger.debug("Automatically closing the window as set in preferences");
			    close();
			    return;
			}
			if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("removeDirtyFlag") ) {
			    if ( logger.isDebugEnabled() ) logger.debug("Removing model's dirty flag");
			    CommandStack stack = (CommandStack)exportedModel.getAdapter(CommandStack.class);
			    stack.markSaveLocation();
			}
		} else {
			setMessage(message, RED_COLOR);
		}
		
		btnClose.setText("close");
		try {
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Button btnDoNotExport;
	private Button btnExportMyVersion;
	private Button btnImportDatabaseVersion;
	
	private Button checkRememberChoice;

	private Group grpConflict;

	private Tree tblCompareComponent;
	private Table tblListConflicts;
	private Label lblCantExport;

	private Text txtReleaseNote;
	
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


	private Table tblModelVersions;
	private Text txtModelName;
	private Text txtPurpose;
}
