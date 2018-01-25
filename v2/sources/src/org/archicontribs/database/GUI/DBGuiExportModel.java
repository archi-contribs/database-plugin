/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.preferences.DBPreferencePage.EXPORT_BEHAVIOUR;
import org.archicontribs.database.model.DBMetadata.CONFLICT_CHOICE;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
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

public class DBGuiExportModel extends DBGui {
	private static final DBLogger logger = new DBLogger(DBGuiExportModel.class);

	private ArchimateModel exportedModel = null;

	private Group grpComponents;
	private Group grpModelVersions;
	
	@SuppressWarnings("unused")
	private EXPORT_BEHAVIOUR exportBehaviour;

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
                    boolean canChangeMetaData = (connection != null && connection.getExportWholeModel() && (tblModelVersions.getSelection()[0] == tblModelVersions.getItem(0)));
                    
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
		//txtModelName.setBackground(GROUP_BACKGROUND_COLOR);
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
		//txtPurpose.setBackground(GROUP_BACKGROUND_COLOR);
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

        Label lblModel = new Label(grpComponents, SWT.CENTER);
        lblModel.setBackground(GROUP_BACKGROUND_COLOR);
        lblModel.setText("Model");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(40, 10);
        fd.right = new FormAttachment(70, -10);
        lblModel.setLayoutData(fd);
        
        Label lblExportedNew = new Label(grpComponents, SWT.CENTER);
        lblExportedNew.setBackground(GROUP_BACKGROUND_COLOR);
        lblExportedNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(lblModel, 5);
        fd.left = new FormAttachment(40, 10);
        fd.right = new FormAttachment(55, -5);
        lblExportedNew.setLayoutData(fd);
        
        Label lblExportedUpdated = new Label(grpComponents, SWT.CENTER);
        lblExportedUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        lblExportedUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(lblModel, 5);
        fd.left = new FormAttachment(55, 5);
        fd.right = new FormAttachment(70, -10);
        lblExportedUpdated.setLayoutData(fd);

        Label lblDatabase = new Label(grpComponents, SWT.CENTER);
        lblDatabase.setBackground(GROUP_BACKGROUND_COLOR);
        lblDatabase.setText("Database");
        fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(70, 10);
        fd.right = new FormAttachment(100, -10);
        lblDatabase.setLayoutData(fd);
        
        Label lblImportedNew = new Label(grpComponents, SWT.CENTER);
        lblImportedNew.setBackground(GROUP_BACKGROUND_COLOR);
        lblImportedNew.setText("New");
        fd = new FormData();
        fd.top = new FormAttachment(lblDatabase, 5);
        fd.left = new FormAttachment(40, 10);
        fd.right = new FormAttachment(55, -5);
        lblImportedNew.setLayoutData(fd);
        
        Label lblImportedUpdated = new Label(grpComponents, SWT.CENTER);
        lblImportedUpdated.setBackground(GROUP_BACKGROUND_COLOR);
        lblImportedUpdated.setText("Updated");
        fd = new FormData();
        fd.top = new FormAttachment(lblDatabase, 5);
        fd.left = new FormAttachment(55, 5);
        fd.right = new FormAttachment(70, -10);
        lblImportedUpdated.setLayoutData(fd);
        
        Label lblTotal = new Label(grpComponents, SWT.CENTER);
        lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
        lblTotal.setText("Total");
        fd = new FormData();
        fd.left = new FormAttachment(20, 10);
        fd.right = new FormAttachment(40, -10);
        fd.bottom = new FormAttachment(lblExportedNew, 0, SWT.BOTTOM);
        lblTotal.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalElements.setLayoutData(fd);

        txtNewModelElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelElements.setLayoutData(fd);
        
        txtUpdatedModelElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedModelElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedUpdated, 0, SWT.RIGHT);
        txtUpdatedModelElements.setLayoutData(fd);
        
        txtNewDatabaseElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseElements.setLayoutData(fd);
        
        txtUpdatedDatabaseElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedDatabaseElements.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedUpdated, 0, SWT.RIGHT);
        txtUpdatedDatabaseElements.setLayoutData(fd);

        /* * * * * */
        
        txtTotalRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalRelationships.setLayoutData(fd);

        txtNewModelRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelRelationships.setLayoutData(fd);
        
        txtUpdatedModelRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedModelRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedUpdated, 0, SWT.RIGHT);
        txtUpdatedModelRelationships.setLayoutData(fd);
        
        txtNewDatabaseRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseRelationships.setLayoutData(fd);
        
        txtUpdatedDatabaseRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedDatabaseRelationships.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedUpdated, 0, SWT.RIGHT);
        txtUpdatedDatabaseRelationships.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalFolders.setLayoutData(fd);

        txtNewModelFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelFolders.setLayoutData(fd);
        
        txtUpdatedModelFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedModelFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedUpdated, 0, SWT.RIGHT);
        txtUpdatedModelFolders.setLayoutData(fd);
        
        txtNewDatabaseFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseFolders.setLayoutData(fd);
        
        txtUpdatedDatabaseFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedDatabaseFolders.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedUpdated, 0, SWT.RIGHT);
        txtUpdatedDatabaseFolders.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViews.setLayoutData(fd);

        txtNewModelViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelViews.setLayoutData(fd);
        
        txtUpdatedModelViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedModelViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedUpdated, 0, SWT.RIGHT);
        txtUpdatedModelViews.setLayoutData(fd);
        
        txtNewDatabaseViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseViews.setLayoutData(fd);
        
        txtUpdatedDatabaseViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedDatabaseViews.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedUpdated, 0, SWT.RIGHT);
        txtUpdatedDatabaseViews.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViewObjects.setLayoutData(fd);

        txtNewModelViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelViewObjects.setLayoutData(fd);
        
        txtUpdatedModelViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedModelViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedUpdated, 0, SWT.RIGHT);
        txtUpdatedModelViewObjects.setLayoutData(fd);
        
        txtNewDatabaseViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseViewObjects.setLayoutData(fd);
        
        txtUpdatedDatabaseViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedDatabaseViewObjects.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedUpdated, 0, SWT.RIGHT);
        txtUpdatedDatabaseViewObjects.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalViewConnections.setLayoutData(fd);

        txtNewModelViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelViewConnections.setLayoutData(fd);
        
        txtUpdatedModelViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedModelViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedUpdated, 0, SWT.RIGHT);
        txtUpdatedModelViewConnections.setLayoutData(fd);
        
        txtNewDatabaseViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseViewConnections.setLayoutData(fd);
        
        txtUpdatedDatabaseViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtUpdatedDatabaseViewConnections.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedUpdated, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedUpdated, 0, SWT.RIGHT);
        txtUpdatedDatabaseViewConnections.setLayoutData(fd);
        
        /* * * * * */
        
        txtTotalImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtTotalImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblTotal, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
        txtTotalImages.setLayoutData(fd);

        txtNewModelImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewModelImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblExportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblExportedNew, 0, SWT.RIGHT);
        txtNewModelImages.setLayoutData(fd);
        
        txtNewDatabaseImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
        txtNewDatabaseImages.setEditable(false);
        fd = new FormData(26,18);
        fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblImportedNew, 0, SWT.LEFT);
        fd.right = new FormAttachment(lblImportedNew, 0, SWT.RIGHT);
        txtNewDatabaseImages.setLayoutData(fd);
    }

	/**
	 * This method is called each time a database is selected and a connection has been established to it.<br>
	 * <br>
	 * It calls the database.checkComponentsToExport() method to compare exported model's components checksum with the one stored in the database and fill in their count in the corresponding labels.<br>
	 * If no exception is raised, then the "Export" button is enabled.
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheckDatabase) {
		// We get the preference's export behaviour
		switch ( DBPlugin.INSTANCE.getPreferenceStore().getString("exportBehaviour") ) {
			case "sync":   exportBehaviour = EXPORT_BEHAVIOUR.syncMode; break;
			case "master": exportBehaviour = EXPORT_BEHAVIOUR.masterMode; break;
			default:       exportBehaviour = EXPORT_BEHAVIOUR.collaborativeMode;
		}
		
		// We count the components to export and activate the export button
		txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
		txtNewModelElements.setText("");
		txtUpdatedModelElements.setText("");
		txtNewDatabaseElements.setText("");
		txtUpdatedDatabaseElements.setText("");

		txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
        txtNewModelRelationships.setText("");
        txtUpdatedModelRelationships.setText("");
        txtNewDatabaseRelationships.setText("");
        txtUpdatedDatabaseRelationships.setText("");

		txtTotalFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
        txtNewModelFolders.setText("");
        txtUpdatedModelFolders.setText("");
        txtNewDatabaseFolders.setText("");
        txtUpdatedDatabaseFolders.setText("");

		txtTotalViews.setText(String.valueOf(exportedModel.getAllViews().size()));
        txtNewModelViews.setText("");
        txtUpdatedModelViews.setText("");
        txtNewDatabaseViews.setText("");
        txtUpdatedDatabaseViews.setText("");

		txtTotalViewObjects.setText(String.valueOf(exportedModel.getAllViewObjects().size()));
        txtNewModelViewObjects.setText("");
        txtUpdatedModelViewObjects.setText("");
        txtNewDatabaseViewObjects.setText("");
        txtUpdatedDatabaseViewObjects.setText("");

		txtTotalViewConnections.setText(String.valueOf(exportedModel.getAllViewConnections().size()));
        txtNewModelViewConnections.setText("");
        txtUpdatedModelViewConnections.setText("");
        txtNewDatabaseViewConnections.setText("");
        txtUpdatedDatabaseViewConnections.setText("");

		txtTotalImages.setText(String.valueOf(((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()));
        txtNewModelImages.setText("");
        txtNewDatabaseImages.setText("");

		if ( forceCheckDatabase )
		    setOption(selectedDatabase.getExportWholeModel());
		
		if ( DBPlugin.areEqual(selectedDatabase.getDriver().toLowerCase(), "neo4j") )
			disableOption();
		else
			enableOption();

		if ( forceCheckDatabase ) {
			try {
				popup("Checking which components to export ...");

				// we save the name, note and purpose text fields values
				if ( (tblModelVersions.getItemCount() > 0) && (tblModelVersions.getItem(0).getData("new") != null) ) {
					tblModelVersions.setData("name", txtModelName.getText());
					tblModelVersions.setData("note", txtReleaseNote.getText());
					tblModelVersions.setData("purpose", txtPurpose.getText());
				}

				if ( !DBPlugin.areEqual(selectedDatabase.getDriver().toLowerCase(), "neo4j") )
				    connection.getModelVersions(exportedModel.getId(), tblModelVersions);
				connection.compareModelFromDatabase(exportedModel, connection.getExportWholeModel());

				closePopup();
			} catch (Exception err) {
				closePopup();
				popup(Level.FATAL, "Failed to check existing components in database", err);
				setActiveAction(STATUS.Error);
				return;
			}
		}

		if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllElements().size()+" elements in the model : "+connection.getCountIdenticalElements()+" identical, "+connection.getCountNewerElements()+" newer, "+connection.getCountOlderElements()+" older, "+connection.getCountConflictingElements()+" conflicting.");			
		txtNewModelElements.setText(String.valueOf(connection.getCountNewModelElements()));				  txtNewModelElements.setData("value", connection.getCountNewModelElements());
		txtUpdatedModelElements.setText(String.valueOf(connection.getCountUpdatedModelElements()));       txtUpdatedModelElements.setData("value", connection.getCountUpdatedModelElements());
	    txtNewDatabaseElements.setText(String.valueOf(connection.getCountNewDatabaseElements()));         txtNewDatabaseElements.setData("value", connection.getCountNewDatabaseElements());
	    txtUpdatedDatabaseElements.setText(String.valueOf(connection.getCountUpdatedDatabaseElements())); txtUpdatedDatabaseElements.setData("value", connection.getCountUpdatedDatabaseElements());

		if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllRelationships().size()+" Relationships in the model : "+connection.getCountIdenticalRelationships()+" identical, "+connection.getCountNewerRelationships()+" newer, "+connection.getCountOlderRelationships()+" older, "+connection.getCountConflictingRelationships()+" conflicting.");			
        txtNewModelRelationships.setText(String.valueOf(connection.getCountNewModelRelationships()));               txtNewModelRelationships.setData("value", connection.getCountNewModelRelationships());
        txtUpdatedModelRelationships.setText(String.valueOf(connection.getCountUpdatedModelRelationships()));       txtUpdatedModelRelationships.setData("value", connection.getCountUpdatedModelRelationships());
        txtNewDatabaseRelationships.setText(String.valueOf(connection.getCountNewDatabaseRelationships()));         txtNewDatabaseRelationships.setData("value", connection.getCountNewDatabaseRelationships());
        txtUpdatedDatabaseRelationships.setText(String.valueOf(connection.getCountUpdatedDatabaseRelationships())); txtUpdatedDatabaseRelationships.setData("value", connection.getCountUpdatedDatabaseRelationships());

		txtTotalFolders.setVisible(connection.getExportWholeModel());
		txtNewerFolders.setVisible(connection.getExportWholeModel());
		txtOlderFolders.setVisible(connection.getExportWholeModel());
		txtConflictingFolders.setVisible(connection.getExportWholeModel());

		txtTotalViews.setVisible(connection.getExportWholeModel());
		txtNewerViews.setVisible(connection.getExportWholeModel());
		txtOlderViews.setVisible(connection.getExportWholeModel());
		txtConflictingViews.setVisible(connection.getExportWholeModel());

		txtTotalViewObjects.setVisible(connection.getExportWholeModel());
		txtNewerViewObjects.setVisible(connection.getExportWholeModel());
		txtOlderViewObjects.setVisible(connection.getExportWholeModel());
		txtConflictingViewObjects.setVisible(connection.getExportWholeModel());
		
		txtTotalViewConnections.setVisible(connection.getExportWholeModel());
		txtIdenticalViewConnections.setVisible(connection.getExportWholeModel());
		txtNewerViewConnections.setVisible(connection.getExportWholeModel());
		txtOlderViewConnections.setVisible(connection.getExportWholeModel());
		txtConflictingViewConnections.setVisible(connection.getExportWholeModel());

		txtTotalImages.setVisible(connection.getExportWholeModel());
		txtIdenticalImages.setVisible(connection.getExportWholeModel());
		txtNewerImages.setVisible(connection.getExportWholeModel());
		txtOlderImages.setVisible(connection.getExportWholeModel());
		txtConflictingImages.setVisible(connection.getExportWholeModel());

		if ( connection.getExportWholeModel() ) {
			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllFolders().size()+" Folders in the model : "+connection.getCountIdenticalFolders()+" identical, "+connection.getCountNewerFolders()+" newer, "+connection.getCountOlderFolders()+" older, "+connection.getCountConflictingFolders()+" conflicting.");			
			txtIdenticalFolders.setText(String.valueOf(connection.getCountIdenticalFolders()));			txtIdenticalFolders.setData("value", connection.getCountIdenticalFolders());
			txtNewerFolders.setText(String.valueOf(connection.getCountNewerFolders()));					txtNewerFolders.setData("value", connection.getCountNewerFolders());
			txtOlderFolders.setText(String.valueOf(connection.getCountOlderFolders()));					txtOlderFolders.setData("value", connection.getCountOlderFolders());
			txtConflictingFolders.setText(String.valueOf(connection.getCountConflictingFolders()));		txtConflictingFolders.setData("value", connection.getCountConflictingFolders());

			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllViews().size()+" Views in the model : "+connection.getCountIdenticalViews()+" identical, "+connection.getCountNewerViews()+" newer, "+connection.getCountOlderViews()+" older, "+connection.getCountConflictingViews()+" conflicting.");			
			txtIdenticalViews.setText(String.valueOf(connection.getCountIdenticalViews()));			txtIdenticalViews.setData("value", connection.getCountIdenticalViews());
			txtNewerViews.setText(String.valueOf(connection.getCountNewerViews()));					txtNewerViews.setData("value", connection.getCountNewerViews());
			txtOlderViews.setText(String.valueOf(connection.getCountOlderViews()));					txtOlderViews.setData("value", connection.getCountOlderViews());
			txtConflictingViews.setText(String.valueOf(connection.getCountConflictingViews()));		txtConflictingViews.setData("value", connection.getCountConflictingViews());
			
			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllViewObjects().size()+" ViewObjects in the model : "+connection.getCountIdenticalViewObjects()+" identical, "+connection.getCountNewerViewObjects()+" newer, "+connection.getCountOlderViewObjects()+" older, "+connection.getCountConflictingViewObjects()+" conflicting.");			
			txtIdenticalViewObjects.setText(String.valueOf(connection.getCountIdenticalViewObjects()));			txtIdenticalViewObjects.setData("value", connection.getCountIdenticalViewObjects());
			txtNewerViewObjects.setText(String.valueOf(connection.getCountNewerViewObjects()));					txtNewerViewObjects.setData("value", connection.getCountNewerViewObjects());
			txtOlderViewObjects.setText(String.valueOf(connection.getCountOlderViewObjects()));					txtOlderViewObjects.setData("value", connection.getCountOlderViewObjects());
			txtConflictingViewObjects.setText(String.valueOf(connection.getCountConflictingViewObjects()));		txtConflictingViewObjects.setData("value", connection.getCountConflictingViewObjects());

			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllViewConnections().size()+" ViewConnections in the model : "+connection.getCountIdenticalViewConnections()+" identical, "+connection.getCountNewerViewConnections()+" newer, "+connection.getCountOlderViewConnections()+" older, "+connection.getCountConflictingViewConnections()+" conflicting.");			
			txtIdenticalViewConnections.setText(String.valueOf(connection.getCountIdenticalViewConnections()));			txtIdenticalViewConnections.setData("value", connection.getCountIdenticalViewConnections());
			txtNewerViewConnections.setText(String.valueOf(connection.getCountNewerViewConnections()));					txtNewerViewConnections.setData("value", connection.getCountNewerViewConnections());
			txtOlderViewConnections.setText(String.valueOf(connection.getCountOlderViewConnections()));					txtOlderViewConnections.setData("value", connection.getCountOlderViewConnections());
			txtConflictingViewConnections.setText(String.valueOf(connection.getCountConflictingViewConnections()));		txtConflictingViewConnections.setData("value", connection.getCountConflictingViewConnections());

			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllImagePaths().size()+" Images in the model : "+connection.getCountIdenticalImages()+" identical, "+connection.getCountNewerImages()+" newer, "+connection.getCountOlderImages()+" older, "+connection.getCountConflictingImages()+" conflicting.");			
			txtIdenticalImages.setText(String.valueOf(connection.getCountIdenticalImages()));			txtIdenticalImages.setData("value", connection.getCountIdenticalImages());
			txtNewerImages.setText(String.valueOf(connection.getCountNewerImages()));					txtNewerImages.setData("value", connection.getCountNewerImages());
			txtOlderImages.setText(String.valueOf(connection.getCountOlderImages()));					txtOlderImages.setData("value", connection.getCountOlderImages());
			txtConflictingImages.setText(String.valueOf(connection.getCountConflictingImages()));		txtConflictingImages.setData("value", connection.getCountConflictingImages());

			//TableItem tableItem = new TableItem(tblModelVersions, SWT.BOLD, 0);
			// we replace the line "Now" by "Not created yet"
			TableItem tableItem;
			if ( tblModelVersions.getItemCount() == 0 ) {
				tableItem = new TableItem(tblModelVersions, SWT.BOLD, 0);
				tableItem.setText(0, "1");
			} else {
				tableItem = tblModelVersions.getItem(0);
				tableItem.setFont(JFaceResources.getFontRegistry().getBold(tableItem.getFont(0).toString()));
				tableItem.setText(0, (tblModelVersions.getItemCount()>1 ? String.valueOf(Integer.valueOf(tblModelVersions.getItem(1).getText(0))+1) : "1"));
			}
			tableItem.setText(1, "(not created yet)");
			tableItem.setText(2, System.getProperty("user.name"));
			tableItem.setFont(JFaceResources.getFontRegistry().getBold(tableItem.getFont(0).toString()));
			tableItem.setData("new", true);
			tableItem.setData("name", (tblModelVersions.getData("name")!=null ? tblModelVersions.getData("name") : exportedModel.getName()));
			tableItem.setData("note", (tblModelVersions.getData("note")!=null ? tblModelVersions.getData("note") : ""));
			tableItem.setData("purpose", (tblModelVersions.getData("purpose")!=null ? tblModelVersions.getData("purpose") : exportedModel.getPurpose()));
		} else if ( (tblModelVersions.getItemCount() > 0) && (tblModelVersions.getItem(0).getData("new") != null) ) {
			tblModelVersions.setData("name", txtModelName.getText());
			tblModelVersions.setData("note", txtReleaseNote.getText());
			tblModelVersions.setData("purpose", txtPurpose.getText());			
			tblModelVersions.remove(0);
			
			txtModelName.setText("");
			txtModelName.setEnabled(false);
			
			txtReleaseNote.setText("");
			txtReleaseNote.setEnabled(false);
			
			txtPurpose.setText("");
			txtPurpose.setEnabled(false);
		}
		if ( tblModelVersions.getItemCount() > 0 ) {
			tblModelVersions.setSelection(0);
			tblModelVersions.notifyListeners(SWT.Selection, new Event());
		}

		if ( 	(connection.getCountIdenticalElements() == exportedModel.getAllElements().size()) &&
				(connection.getCountIdenticalRelationships() == exportedModel.getAllRelationships().size()) &&
				(connection.getCountIdenticalFolders() == exportedModel.getAllFolders().size()) &&
				(connection.getCountIdenticalViews() == exportedModel.getAllViews().size()) &&
				(connection.getCountIdenticalViewObjects() == exportedModel.getAllViewObjects().size()) &&
				(connection.getCountIdenticalViewConnections() == exportedModel.getAllViewConnections().size()) &&
				(connection.getCountIdenticalImages() == exportedModel.getAllImagePaths().size()) ) {
			popup(Level.INFO, "The model is already sync'ed to the database and doesn't need to be exported.");
			btnDoAction.setEnabled(false);
			return;
		}

		if ( logger.isDebugEnabled() ) logger.debug("Enabling the \"Export\" button.");;
		btnDoAction.setEnabled(true);
		
		if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("exportWithDefaultValues") ) {
			logger.debug("Automatically start export as specified in preferences.");
			btnDoAction.notifyListeners(SWT.Selection, new Event());
		}
	}

	/**
	 * Empties the labels showing the number of components equals or different from their database values.<br>
	 * <br>
	 * This method is called each time a database is selected and a connection failed to be established to it.
	 */
	@Override
	protected void notConnectedToDatabase() {
		txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
		txtIdenticalElements.setText("");
		txtNewerElements.setText("");
		txtOlderElements.setText("");
		txtConflictingElements.setText("");

		txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
		txtIdenticalRelationships.setText("");
		txtNewerRelationships.setText("");
		txtOlderRelationships.setText("");
		txtConflictingRelationships.setText("");

		txtTotalFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
		txtIdenticalFolders.setText("");
		txtNewerFolders.setText("");
		txtOlderFolders.setText("");
		txtConflictingFolders.setText("");

		txtTotalViews.setText(String.valueOf(exportedModel.getAllViews().size()));
		txtIdenticalViews.setText("");
		txtNewerViews.setText("");
		txtOlderViews.setText("");
		txtConflictingViews.setText("");

		txtTotalViewObjects.setText(String.valueOf(exportedModel.getAllViewObjects().size()));
		txtIdenticalViewObjects.setText("");
		txtNewerViewObjects.setText("");
		txtOlderViewObjects.setText("");
		txtConflictingViewObjects.setText("");

		txtTotalViewConnections.setText(String.valueOf(exportedModel.getAllViewConnections().size()));
		txtIdenticalViewConnections.setText("");
		txtNewerViewConnections.setText("");
		txtOlderViewConnections.setText("");
		txtConflictingViewConnections.setText("");

		txtTotalImages.setText(String.valueOf(((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()));
		txtIdenticalImages.setText("");
		txtNewerImages.setText("");
		txtOlderImages.setText("");
		txtConflictingImages.setText("");
	}
	
	/**
	 * Loop on model components and call doExportEObject to export them<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button
	 */
	protected void export() {
		int progressBarWidth;
		if ( connection.getExportWholeModel() ) {
			logger.info("Exporting model : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships, "+exportedModel.getAllFolders().size()+" folders, "+exportedModel.getAllViews().size()+" views, "+exportedModel.getAllViewObjects().size()+" views objects, "+exportedModel.getAllViewConnections().size()+" views connections, and "+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()+" images.");
			progressBarWidth = exportedModel.getAllFolders().size()+exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size()+exportedModel.getAllViews().size()+exportedModel.getAllViewObjects().size()+exportedModel.getAllViewConnections().size()+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size();
		} else {
			logger.info("Exporting components : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships.");
			progressBarWidth = exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size();
		}
		
		// we reset the counters because they may have been changed (in case of conflict for instance)
		connection.setCountIdenticalElements((int)txtIdenticalElements.getData("value"));					txtIdenticalElements.setText(String.valueOf((int)txtIdenticalElements.getData("value")));
		connection.setCountNewerElements((int)txtNewerElements.getData("value"));							txtNewerElements.setText(String.valueOf((int)txtNewerElements.getData("value")));
		connection.setCountOlderElements((int)txtOlderElements.getData("value"));							txtOlderElements.setText(String.valueOf((int)txtOlderElements.getData("value")));
		connection.setCountConflictingElements((int)txtConflictingElements.getData("value"));				txtOlderElements.setText(String.valueOf((int)txtConflictingElements.getData("value")));

		connection.setCountIdenticalRelationships((int)txtIdenticalRelationships.getData("value"));			txtIdenticalRelationships.setText(String.valueOf((int)txtIdenticalRelationships.getData("value")));
		connection.setCountNewerRelationships((int)txtNewerRelationships.getData("value"));					txtNewerRelationships.setText(String.valueOf((int)txtNewerRelationships.getData("value")));
		connection.setCountOlderRelationships((int)txtOlderRelationships.getData("value"));					txtOlderRelationships.setText(String.valueOf((int)txtOlderRelationships.getData("value")));
		connection.setCountConflictingRelationships((int)txtConflictingRelationships.getData("value"));		txtOlderRelationships.setText(String.valueOf((int)txtConflictingRelationships.getData("value")));

		if ( connection.getExportWholeModel() ) {
			connection.setCountIdenticalFolders((int)txtIdenticalFolders.getData("value"));					txtIdenticalFolders.setText(String.valueOf((int)txtIdenticalFolders.getData("value")));
			connection.setCountNewerFolders((int)txtNewerFolders.getData("value"));							txtNewerFolders.setText(String.valueOf((int)txtNewerFolders.getData("value")));
			connection.setCountOlderFolders((int)txtOlderFolders.getData("value"));							txtOlderFolders.setText(String.valueOf((int)txtOlderFolders.getData("value")));
			connection.setCountConflictingFolders((int)txtConflictingFolders.getData("value"));				txtOlderFolders.setText(String.valueOf((int)txtConflictingFolders.getData("value")));

			connection.setCountIdenticalViews((int)txtIdenticalViews.getData("value"));						txtIdenticalViews.setText(String.valueOf((int)txtIdenticalViews.getData("value")));
			connection.setCountNewerViews((int)txtNewerViews.getData("value"));								txtNewerViews.setText(String.valueOf((int)txtNewerViews.getData("value")));
			connection.setCountOlderViews((int)txtOlderViews.getData("value"));								txtOlderViews.setText(String.valueOf((int)txtOlderViews.getData("value")));
			connection.setCountConflictingViews((int)txtConflictingViews.getData("value"));					txtOlderViews.setText(String.valueOf((int)txtConflictingViews.getData("value")));

			connection.setCountIdenticalViewObjects((int)txtIdenticalViewObjects.getData("value"));				txtIdenticalViewObjects.setText(String.valueOf((int)txtIdenticalViewObjects.getData("value")));
			connection.setCountNewerViewObjects((int)txtNewerViewObjects.getData("value"));						txtNewerViewObjects.setText(String.valueOf((int)txtNewerViewObjects.getData("value")));
			connection.setCountOlderViewObjects((int)txtOlderViewObjects.getData("value"));						txtOlderViewObjects.setText(String.valueOf((int)txtOlderViewObjects.getData("value")));
			connection.setCountConflictingViewObjects((int)txtConflictingViewObjects.getData("value"));			txtOlderViewObjects.setText(String.valueOf((int)txtConflictingViewObjects.getData("value")));

			connection.setCountIdenticalViewConnections((int)txtIdenticalViewConnections.getData("value"));		txtIdenticalViewConnections.setText(String.valueOf((int)txtIdenticalViewConnections.getData("value")));
			connection.setCountNewerViewConnections((int)txtNewerViewConnections.getData("value"));				txtNewerViewConnections.setText(String.valueOf((int)txtNewerViewConnections.getData("value")));
			connection.setCountOlderViewConnections((int)txtOlderViewConnections.getData("value"));				txtOlderViewConnections.setText(String.valueOf((int)txtOlderViewConnections.getData("value")));
			connection.setCountConflictingViewConnections((int)txtConflictingViewConnections.getData("value"));	txtOlderViewConnections.setText(String.valueOf((int)txtConflictingViewConnections.getData("value")));

			connection.setCountIdenticalImages((int)txtIdenticalImages.getData("value"));						txtIdenticalImages.setText(String.valueOf((int)txtIdenticalImages.getData("value")));
			connection.setCountNewerImages((int)txtNewerImages.getData("value"));								txtNewerImages.setText(String.valueOf((int)txtNewerImages.getData("value")));
			connection.setCountOlderImages((int)txtOlderImages.getData("value"));								txtOlderImages.setText(String.valueOf((int)txtOlderImages.getData("value")));
			connection.setCountConflictingImages((int)txtConflictingImages.getData("value"));					txtOlderImages.setText(String.valueOf((int)txtConflictingImages.getData("value")));
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
		
		// we change the "identical" text fields to black
		txtIdenticalElements.setForeground(BLACK_COLOR);
		txtIdenticalRelationships.setForeground(BLACK_COLOR);
		txtIdenticalFolders.setForeground(BLACK_COLOR);
		txtIdenticalViews.setForeground(BLACK_COLOR);
		txtIdenticalViewObjects.setForeground(BLACK_COLOR);
		txtIdenticalViewConnections.setForeground(BLACK_COLOR);
		txtIdenticalImages.setForeground(BLACK_COLOR);

		// We show up a small arrow in front of the second action "export components"
        setActiveAction(STATUS.Ok);
        setActiveAction(ACTION.Two);

		// We hide the grpDatabase and create a progressBar instead 
		hideGrpDatabase();
		createProgressBar("Exporting components ...", 0, progressBarWidth);
		createGrpConflict();

		// then, we start a new database transaction
		try {
		    connection.setAutoCommit(false);
		} catch (SQLException err ) {
			popup(Level.FATAL, "Failed to create a transaction in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult(err);
			return;
		}

		// we export the components

			
		try {
			// if we need to save the model
			if ( connection.getExportWholeModel() ) {
				// we retrieve the latest version of the model in the database and increase the version number.
			    exportedModel.setExportedVersion(connection.getLatestModelVersion(exportedModel) + 1);
	
				// just in case
				if ( !DBPlugin.areEqual(exportedModel.getName(), txtModelName.getText()) )
					exportedModel.setName(txtModelName.getText());
	
				if ( !DBPlugin.areEqual(exportedModel.getPurpose(), txtPurpose.getText()) )
					exportedModel.setPurpose(txtPurpose.getText());
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting version "+exportedModel.getExportedVersion()+" of the model");
				connection.exportModel(exportedModel, txtReleaseNote.getText());
	
				if ( logger.isDebugEnabled() ) logger.debug("Exporting folders");
				Iterator<Entry<String, IFolder>> foldersIterator = exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) {
					doExportEObject(foldersIterator.next().getValue(), txtIdenticalFolders, txtNewerFolders, txtOlderFolders);
				}
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting elements");
			Iterator<Entry<String, IArchimateElement>> elementsIterator = exportedModel.getAllElements().entrySet().iterator();
			while ( elementsIterator.hasNext() ) {
				doExportEObject(elementsIterator.next().getValue(), txtIdenticalElements, txtNewerElements, txtOlderElements);
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting relationships");
			Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = exportedModel.getAllRelationships().entrySet().iterator();
			while ( relationshipsIterator.hasNext() ) {
				doExportEObject(relationshipsIterator.next().getValue(), txtIdenticalRelationships, txtNewerRelationships, txtOlderRelationships);
			}
	
			if ( connection.getExportWholeModel() ) {
				
				// we export first all the views in one go in order to check as quickly as possible if there are some conflicts
				List<IDiagramModel> exportedViews = new ArrayList<IDiagramModel>();
				
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views");
				Iterator<Entry<String, IDiagramModel>> viewsIterator = exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue(); 
					if ( doExportEObject(view, txtIdenticalViews, txtNewerViews, txtOlderViews) ) {
						exportedViews.add(view);
					}
				}
				
				for ( IDiagramModel view: exportedViews ) {
					connectionsAlreadyExported = new HashMap<String, IDiagramModelConnection>();		// we need to memorize exported connections as they can be get as sources AND as targets 
					for ( IDiagramModelObject viewObject: view.getChildren() ) {
						doExportViewObject(viewObject);
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
							txtIdenticalImages.setText(String.valueOf(Integer.valueOf(txtIdenticalImages.getText())+1));
							txtNewImages.setText(String.valueOf(Integer.valueOf(txtNewImages.getText())-1));
							break;
						case isUpdated :
							txtIdenticalImages.setText(String.valueOf(Integer.valueOf(txtIdenticalImages.getText())+1));
							txtOlderImages.setText(String.valueOf(Integer.valueOf(txtOlderImages.getText())-1));
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
				doShowResult(err);
				return;
			} catch (SQLException err2) {
				popup(Level.FATAL, "The transaction failed to rollback and the database is left in an inconsistent state.\n\nPlease check carrefully your database !", err2);
				doShowResult(err2);
				return;
			}
		}

		if ( logger.isDebugEnabled() ) logger.debug("Found "+tblListConflicts.getItemCount()+" components conflicting with database");
		if ( tblListConflicts.getItemCount() == 0 ) {
			try  {
			    connection.commit();
			    connection.setAutoCommit(true);
				setActiveAction(STATUS.Ok);
				doShowResult(null);
				return;
			} catch (Exception err) {
				popup(Level.FATAL, "Failed to commit the transaction. Please check carrefully your database !", err);
				setActiveAction(STATUS.Error);
				doShowResult(err);
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
				doShowResult(err);
				return;
			}
		
			tblListConflicts.setSelection(0);
			try {
				tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
			} catch (Exception err) {
				popup(Level.ERROR, "Failed to compare component with its database version.", err);
				setActiveAction(STATUS.Error);
				doShowResult(err);
				return;
			}
		}
	}

	Map<String, IDiagramModelConnection> connectionsAlreadyExported;
	private void doExportViewObject(IDiagramModelObject viewObject) throws Exception {
		if ( logger.isTraceEnabled() ) logger.trace("exporting view object "+((IDBMetadata)viewObject).getDBMetadata().getDebugName());
		doExportEObject(viewObject, txtIdenticalViewObjects, txtNewerViewObjects, txtOlderViewObjects);
		
		if ( viewObject instanceof IConnectable) {
			for ( IDiagramModelConnection source: ((IConnectable)viewObject).getSourceConnections() ) {
				if ( connectionsAlreadyExported.get(source.getId()) == null ) {
					doExportEObject(source, txtIdenticalViewConnections, txtNewerViewConnections, txtOlderViewConnections);
					connectionsAlreadyExported.put(source.getId(), source);
				}
			}
			for ( IDiagramModelConnection target: ((IConnectable)viewObject).getTargetConnections() ) {
				if ( connectionsAlreadyExported.get(target.getId()) == null ) {
					doExportEObject(target, txtIdenticalViewConnections, txtNewerViewConnections, txtOlderViewConnections);
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

	/**
	 * Effectively exports an EObject in the database<br>
	 * When a conflict is detected, the component ID is added to the tblListConflicts<br>
	 * <br>
	 * This method is called by the export() method
	 * @return true if the EObject has been exported, false if it is conflicting
	 */
	private boolean doExportEObject(EObject eObjectToExport, Text txtIdentical, Text txtNew, Text txtOlder) throws Exception {
		assert(eObjectToExport instanceof IDBMetadata);
		assert(connection != null);
		
		//TODO : select the text field concerned BEFORE calling this method
		
		boolean status = true;
		try {
			connection.exportEObject(eObjectToExport);
			if ( connection.getExportWholeModel() )
				connection.assignEObjectToModel(eObjectToExport);
			
			if ( txtIdentical != null ) {
				txtIdentical.setText(String.valueOf(Integer.valueOf(txtIdentical.getText())+1));
				switch ( ((IDBMetadata)eObjectToExport).getDBMetadata().getStatus() ) {
					case conflict:	// should not be here !!!
					case identical:	// should not be here !!!
					case newer:		txtNew.setText(String.valueOf(Integer.valueOf(txtNew.getText())-1)); break;
					case older:		txtOlder.setText(String.valueOf(Integer.valueOf(txtOlder.getText())-1)); break;
					case unknown:	// should not be here !!!
				}
			}
		} catch (SQLException err) {
			status = false;
			// if the SQL exception is not linked to a primary key violation, then we escalate the exception
			// unfortunately, this is constructor dependent
			// worst, it may change from one driver version to another version
			switch ( selectedDatabase.getDriver().toLowerCase() ) {
			    case "sqlite" :
			        if ( err.getErrorCode() != 19 )
			            throw err;
			        break;
			    case "postgresql" :
			        if ( !DBPlugin.areEqual(err.getSQLState(), "23505") )
                        throw err;
			        break;
			    case "mysql" :
			    	if ( err.getErrorCode() != 1062 )
			    		throw err;
			    	break;
			    case "oracle" :
			    	if ( err.getErrorCode() != 1 )
			    		throw err;
			    	break;
			    case "ms-sql" :
			    	if ( err.getErrorCode() != 2627 )
			    		throw err;
			    	break;
			    case "neo4j" :		// we do not use primary keys on neo4j databases
			    default :
			        throw err;
			        // TODO : we need to determine the right value for all the databases
			}
			
			// if we're here, it means that a conflict has been detected
			if ( logger.isDebugEnabled() ) logger.debug("The component conflicts with the version in the database.");
			switch ( ((IDBMetadata)eObjectToExport).getDBMetadata().getConflictChoice() ) {
				case askUser :
					if ( logger.isDebugEnabled() ) logger.debug("The conflict has to be manually resolved by user.");
					new TableItem(tblListConflicts, SWT.NONE).setText(((IIdentifier)eObjectToExport).getId());
					if ( tblListConflicts.getItemCount() < 2 )
						lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" component conflicts with newer version in the database :");
					else
						lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" components conflict with newer version in the database :");
					break;
				case doNotExport :
					if ( logger.isDebugEnabled() ) logger.debug("The component is tagged \"do not export\", so we keep it as it is.");
					break;
				case exportToDatabase :
					if ( logger.isDebugEnabled() ) logger.debug("The component is tagged to force export to the database. ");
					((IDBMetadata)eObjectToExport).getDBMetadata().setCurrentVersion(((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion() + 1);
					((IDBMetadata)eObjectToExport).getDBMetadata().setDatabaseVersion(((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion() + 1);
					((IDBMetadata)eObjectToExport).getDBMetadata().setConflictChoice(CONFLICT_CHOICE.askUser);	// just in case there is a new conflict
					status = doExportEObject(eObjectToExport, txtIdentical, txtNew, txtOlder);
					break;
				case importFromDatabase :
					if ( logger.isDebugEnabled() ) logger.debug("The component is tagged \"import the database version\".");
					// For the moment, we can import concepts only during an export !!!
			        if ( eObjectToExport instanceof IArchimateElement )
			            connection.importElementFromId(exportedModel, null, ((IIdentifier)eObjectToExport).getId(), false);
			        else
			            connection.importRelationshipFromId(exportedModel, null, ((IIdentifier)eObjectToExport).getId(), false);
					break;
			}
		}
		increaseProgressBar();
		return status;
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
	
						fillInCompareTable(tblCompareComponent, conflictingComponent, ((IDBMetadata)conflictingComponent).getDBMetadata().getDatabaseVersion(), null);
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

	protected void doShowResult(Exception err) {
		logger.debug("Showing result.");
		if ( grpProgressBar != null ) grpProgressBar.setVisible(false);
		if ( grpConflict != null ) grpConflict.setVisible(false);
		grpComponents.setVisible(true);
		grpModelVersions.setVisible(true);

		setActiveAction(ACTION.Three);
		
		if ( logger.isTraceEnabled() ) {
		    logger.trace("Model : "+txtTotalElements.getText()+" elements, "+txtTotalRelationships.getText()+" relationships, "+txtTotalFolders.getText()+" folders, "+txtTotalViews.getText()+" views, "+txtTotalViewObjects.getText()+" view objects, "+txtTotalViewConnections.getText()+" view connections.");
		    logger.trace("updated : "+txtOlderElements.getText()+" elements, "+txtOlderRelationships.getText()+" relationships, "+txtOlderFolders.getText()+" folders, "+txtOlderViews.getText()+" views, "+txtOlderViewObjects.getText()+" view objects, "+txtOlderViewConnections.getText()+" view connections.");
		    logger.trace("new : "+txtNewerElements.getText()+" elements, "+txtNewerRelationships.getText()+" relationships, "+txtNewerFolders.getText()+" folders, "+txtNewerViews.getText()+" views, "+txtNewerViewObjects.getText()+" view objects, "+txtNewerViewConnections.getText()+" view connections.");
		}

		Color statusColor = GREEN_COLOR;
		txtIdenticalElements.setForeground( DBPlugin.areEqual(txtIdenticalElements.getText(), txtTotalElements.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
		txtIdenticalRelationships.setForeground( DBPlugin.areEqual(txtIdenticalRelationships.getText(), txtTotalRelationships.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
		if ( connection.getExportWholeModel() ) {
			txtIdenticalFolders.setForeground( DBPlugin.areEqual(txtIdenticalFolders.getText(), txtTotalFolders.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtIdenticalViews.setForeground( DBPlugin.areEqual(txtIdenticalViews.getText(), txtTotalViews.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtIdenticalViewObjects.setForeground( DBPlugin.areEqual(txtIdenticalViewObjects.getText(), txtTotalViewObjects.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtIdenticalViewConnections.setForeground( DBPlugin.areEqual(txtIdenticalViewConnections.getText(), txtTotalViewConnections.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtIdenticalImages.setForeground( DBPlugin.areEqual(txtIdenticalImages.getText(), txtTotalImages.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
		}
		
		refreshDisplay();
		
		if ( err == null ) {
			if ( statusColor == GREEN_COLOR ) {
				setMessage("Export successful", statusColor);
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
				// if some counters are different from the expected values
				setMessage("No error has been raised but the number of exported components is not correct.\n\nPlease check thoroughly your database !", RED_COLOR);
			}
			
		} else {
            setMessage("Error while exporting model.\n"+ err.getMessage(), RED_COLOR);
		}
		
		btnClose.setText("close");
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

    private Text txtTotalElements;
    private Text txtNewModelElements;
    private Text txtUpdatedModelElements;
    private Text txtNewDatabaseElements;
    private Text txtUpdatedDatabaseElements;

    private Text txtTotalRelationships;
    private Text txtNewModelRelationships;
    private Text txtUpdatedModelRelationships;
    private Text txtNewDatabaseRelationships;
    private Text txtUpdatedDatabaseRelationships;

    private Text txtTotalFolders;
    private Text txtNewModelFolders;
    private Text txtUpdatedModelFolders;
    private Text txtNewDatabaseFolders;
    private Text txtUpdatedDatabaseFolders;

    private Text txtTotalViews;
    private Text txtNewModelViews;
    private Text txtUpdatedModelViews;
    private Text txtNewDatabaseViews;
    private Text txtUpdatedDatabaseViews;

    private Text txtTotalViewObjects;
    private Text txtNewModelViewObjects;
    private Text txtUpdatedModelViewObjects;
    private Text txtNewDatabaseViewObjects;
    private Text txtUpdatedDatabaseViewObjects;

    private Text txtTotalViewConnections;
    private Text txtNewModelViewConnections;
    private Text txtUpdatedModelViewConnections;
    private Text txtNewDatabaseViewConnections;
    private Text txtUpdatedDatabaseViewConnections;
    
    private Text txtTotalImages;
    private Text txtNewModelImages;
    private Text txtNewDatabaseImages;

	private Table tblModelVersions;
	private Text txtModelName;
	private Text txtPurpose;
}
