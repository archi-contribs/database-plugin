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
import org.archicontribs.database.model.DBMetadata.CONFLICT_CHOICE;
import org.archicontribs.database.model.DBMetadata.DATABASE_STATUS;
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

		// we show the option in the bottom
		setOption("Export type :", "Whole model", "The whole model will be exported, including the views and graphical components.", "Elements and relationships only", "Only the elements and relationships will be exported.", true);

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
				if ( (tblModelVersions.getSelection() != null) && (tblModelVersions.getSelection().length > 0) && (tblModelVersions.getSelection()[0] != null) ) {
					txtReleaseNote.setText((String) tblModelVersions.getSelection()[0].getData("note"));
					txtPurpose.setText((String) tblModelVersions.getSelection()[0].getData("purpose"));
					txtModelName.setText((String) tblModelVersions.getSelection()[0].getData("name"));

					if ( getOptionValue() && (tblModelVersions.getSelection()[0] == tblModelVersions.getItem(0)) ) {
						txtReleaseNote.setEnabled(true);
						txtPurpose.setEnabled(true);
						txtModelName.setEnabled(true);
						btnDoAction.setEnabled(true);
					} else {
						txtReleaseNote.setEnabled(false);
						txtPurpose.setEnabled(false);
						txtModelName.setEnabled(false);
						btnDoAction.setEnabled(false);
					}
				} else {
					btnDoAction.setEnabled(false);
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

		Label lblTotal = new Label(grpComponents, SWT.CENTER);
		lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
		lblTotal.setText("Total");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(20, 10);
		fd.right = new FormAttachment(40, -10);
		lblTotal.setLayoutData(fd);

		Label lblSynced = new Label(grpComponents, SWT.CENTER);
		lblSynced.setBackground(GROUP_BACKGROUND_COLOR);
		lblSynced.setText("Sync'ed");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(40, 10);
		fd.right = new FormAttachment(60, -10);
		lblSynced.setLayoutData(fd);

		Label lblUpdated = new Label(grpComponents, SWT.CENTER);
		lblUpdated.setBackground(GROUP_BACKGROUND_COLOR);
		lblUpdated.setText("Updated");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(60, 10);
		fd.right = new FormAttachment(80, -10);
		lblUpdated.setLayoutData(fd);

		Label lblNew = new Label(grpComponents, SWT.CENTER);
		lblNew.setBackground(GROUP_BACKGROUND_COLOR);
		lblNew.setText("New");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(80, 10);
		fd.right = new FormAttachment(100, -10);
		lblNew.setLayoutData(fd);

		/* * * * * */

		txtTotalElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalElements.setLayoutData(fd);

		txtSyncedElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedElements.setLayoutData(fd);

		txtUpdatedElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedElements.setLayoutData(fd);

		txtNewElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewElements.setLayoutData(fd);



		txtTotalRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalRelationships.setLayoutData(fd);

		txtSyncedRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedRelationships.setLayoutData(fd);

		txtUpdatedRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedRelationships.setLayoutData(fd);

		txtNewRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewRelationships.setLayoutData(fd);




		txtTotalFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalFolders.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalFolders.setLayoutData(fd);

		txtSyncedFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedFolders.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedFolders.setLayoutData(fd);

		txtUpdatedFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedFolders.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedFolders.setLayoutData(fd);

		txtNewFolders = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewFolders.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewFolders.setLayoutData(fd);



		txtTotalViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalViews.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalViews.setLayoutData(fd);

		txtSyncedViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedViews.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedViews.setLayoutData(fd);

		txtUpdatedViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedViews.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedViews.setLayoutData(fd);

		txtNewViews = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewViews.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewViews.setLayoutData(fd);



		txtTotalViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalViewObjects.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalViewObjects.setLayoutData(fd);

		txtSyncedViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedViewObjects.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedViewObjects.setLayoutData(fd);

		txtUpdatedViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedViewObjects.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedViewObjects.setLayoutData(fd);

		txtNewViewObjects = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewViewObjects.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewViewObjects.setLayoutData(fd);
		

		
		txtTotalViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalViewConnections.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalViewConnections.setLayoutData(fd);

		txtSyncedViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedViewConnections.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedViewConnections.setLayoutData(fd);

		txtUpdatedViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedViewConnections.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedViewConnections.setLayoutData(fd);

		txtNewViewConnections = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewViewConnections.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewViewConnections.setLayoutData(fd);

		

		txtTotalImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalImages.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblTotal, 0, SWT.RIGHT);
		txtTotalImages.setLayoutData(fd);

		txtSyncedImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtSyncedImages.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblSynced, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblSynced, 0, SWT.RIGHT);
		txtSyncedImages.setLayoutData(fd);
		
		txtUpdatedImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedImages.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblUpdated, 0, SWT.RIGHT);
		txtUpdatedImages.setLayoutData(fd);

		txtNewImages = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewImages.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(lblNew, 0, SWT.RIGHT);
		txtNewImages.setLayoutData(fd);
	}

	/**
	 * This method is called each time a database is selected and a connection has been established to it.<br>
	 * <br>
	 * It calls the database.checkComponentsToExport() method to compare exported model's components checksum with the one stored in the database and fill in their count in the corresponding labels.<br>
	 * If no exception is raised, then the "Export" button is enabled.
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheckDatabase) {	
		// We count the components to export and activate the export button
		txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
		txtNewElements.setText("");
		txtUpdatedElements.setText("");
		txtSyncedElements.setText("");

		txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
		txtNewRelationships.setText("");
		txtUpdatedRelationships.setText("");
		txtSyncedRelationships.setText("");

		txtTotalFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
		txtNewFolders.setText("");
		txtUpdatedFolders.setText("");
		txtSyncedFolders.setText("");

		txtTotalViews.setText(String.valueOf(exportedModel.getAllViews().size()));
		txtNewViews.setText("");
		txtUpdatedViews.setText("");
		txtSyncedViews.setText("");

		txtTotalViewObjects.setText(String.valueOf(exportedModel.getAllViewObjects().size()));
		txtSyncedViewObjects.setText("");
		txtUpdatedViewObjects.setText("");
		txtNewViewObjects.setText("");

		txtTotalViewConnections.setText(String.valueOf(exportedModel.getAllViewConnections().size()));
		txtSyncedViewConnections.setText("");
		txtUpdatedViewConnections.setText("");
		txtNewViewConnections.setText("");

		txtTotalImages.setText(String.valueOf(((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()));
		txtSyncedImages.setText("");
		txtUpdatedImages.setText("");
		txtNewImages.setText("");

		if ( forceCheckDatabase )
		    setOption(selectedDatabase.getExportWholeModel());
		
		if ( DBPlugin.areEqual(selectedDatabase.getDriver().toLowerCase(), "neo4j") )
			disableOption();
		else
			enableOption();

		if ( getOptionValue() ) {
			forceCheckDatabase = forceCheckDatabase || (connection.countNewViews()+connection.countUpdatedViews()+connection.countSyncedViews() != exportedModel.getAllViews().size());
		} else {
			forceCheckDatabase = forceCheckDatabase || (connection.countNewElements()+connection.countUpdatedElements()+connection.countSyncedElements() != exportedModel.getAllElements().size());
		}

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
				connection.checkComponentsToExport(exportedModel, getOptionValue());

				closePopup();
			} catch (Exception err) {
				closePopup();
				popup(Level.FATAL, "Failed to check existing components in database", err);
				setActiveAction(STATUS.Error);
				return;
			}
		}

		if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllElements().size()+" elements in the model : "+connection.countSyncedElements()+" synced, "+connection.countUpdatedElements()+" updated, "+connection.countNewElements()+"new.");			
		txtNewElements.setText(String.valueOf(connection.countNewElements()));							txtNewElements.setData("value", connection.countNewElements());
		txtUpdatedElements.setText(String.valueOf(connection.countUpdatedElements()));					txtUpdatedElements.setData("value", connection.countUpdatedElements());
		txtSyncedElements.setText(String.valueOf(connection.countSyncedElements()));					txtSyncedElements.setData("value", connection.countSyncedElements());

		if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllRelationships().size()+" relationships in the model: "+connection.countSyncedRelationships()+" synced, "+connection.countUpdatedRelationships()+" updated, "+connection.countNewRelationships()+" new.");
		txtNewRelationships.setText(String.valueOf(connection.countNewRelationships()));				txtNewRelationships.setData("value", connection.countNewRelationships());
		txtUpdatedRelationships.setText(String.valueOf(connection.countUpdatedRelationships()));		txtUpdatedRelationships.setData("value", connection.countUpdatedRelationships());
		txtSyncedRelationships.setText(String.valueOf(connection.countSyncedRelationships()));			txtSyncedRelationships.setData("value", connection.countSyncedRelationships());

		txtTotalFolders.setVisible(getOptionValue());
		txtNewFolders.setVisible(getOptionValue());
		txtUpdatedFolders.setVisible(getOptionValue());
		txtSyncedFolders.setVisible(getOptionValue());

		txtTotalViews.setVisible(getOptionValue());
		txtNewViews.setVisible(getOptionValue());
		txtUpdatedViews.setVisible(getOptionValue());
		txtSyncedViews.setVisible(getOptionValue());

		txtTotalViewObjects.setVisible(getOptionValue());
		txtSyncedViewObjects.setVisible(getOptionValue());
		txtUpdatedViewObjects.setVisible(getOptionValue());
		txtNewViewObjects.setVisible(getOptionValue());
		
		txtTotalViewConnections.setVisible(getOptionValue());
		txtSyncedViewConnections.setVisible(getOptionValue());
		txtUpdatedViewConnections.setVisible(getOptionValue());
		txtNewViewConnections.setVisible(getOptionValue());

		txtTotalImages.setVisible(getOptionValue());
		txtSyncedImages.setVisible(getOptionValue());
		txtUpdatedImages.setVisible(getOptionValue());
		txtNewImages.setVisible(getOptionValue());

		if ( getOptionValue() ) {
			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllFolders().size()+" folders in the model: "+connection.countSyncedFolders()+" synced, "+connection.countUpdatedFolders()+" updated, "+connection.countNewFolders()+" new.");			
			txtNewFolders.setText(String.valueOf(connection.countNewFolders()));							txtNewFolders.setData("value", connection.countNewFolders());
			txtUpdatedFolders.setText(String.valueOf(connection.countUpdatedFolders()));					txtUpdatedFolders.setData("value", connection.countUpdatedFolders());
			txtSyncedFolders.setText(String.valueOf(connection.countSyncedFolders()));						txtSyncedFolders.setData("value", connection.countSyncedFolders());
			//txtSyncedFolders.setForeground( (connection.countSyncedFolders() == exportedModel.getAllFolders().size()) ? GREEN_COLOR : (statusColor=RED_COLOR) );

			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllViews().size()+" views in the model: "+connection.countSyncedViews()+" synced, "+connection.countUpdatedViews()+" updated, "+connection.countNewViews()+" new.");			
			txtNewViews.setText(String.valueOf(connection.countNewViews()));								txtNewViews.setData("value", connection.countNewViews());
			txtUpdatedViews.setText(String.valueOf(connection.countUpdatedViews()));						txtUpdatedViews.setData("value", connection.countUpdatedViews());
			txtSyncedViews.setText(String.valueOf(connection.countSyncedViews()));							txtSyncedViews.setData("value", connection.countSyncedViews());
			//txtSyncedViews.setForeground( (connection.countSyncedViews() == exportedModel.getAllViews().size()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			
			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllViewObjects().size()+" view objects in the model: "+connection.countSyncedViewObjects()+" synced, "+connection.countUpdatedViewObjects()+" updated, "+connection.countNewViewObjects()+" new.");
			txtNewViewObjects.setText(String.valueOf(connection.countNewViewObjects()));					txtNewViewObjects.setData("value", connection.countNewViewObjects());
			txtUpdatedViewObjects.setText(String.valueOf(connection.countUpdatedViewObjects()));			txtUpdatedViewObjects.setData("value", connection.countUpdatedViewObjects());
			txtSyncedViewObjects.setText(String.valueOf(connection.countSyncedViewObjects()));				txtSyncedViewObjects.setData("value", connection.countSyncedViewObjects());
			//txtSyncedViewObjects.setForeground( (connection.countSyncedViewObjects() == exportedModel.getAllViewObjects().size()) ? GREEN_COLOR : (statusColor=RED_COLOR) );

			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllViewConnections().size()+" view connections in the model: "+connection.countSyncedViewConnections()+" synced, "+connection.countUpdatedViewConnections()+" updated, "+connection.countNewViewConnections()+" new.");
			txtNewViewConnections.setText(String.valueOf(connection.countNewViewConnections()));			txtNewViewConnections.setData("value", connection.countNewViewConnections());
			txtUpdatedViewConnections.setText(String.valueOf(connection.countUpdatedViewConnections()));	txtUpdatedViewConnections.setData("value", connection.countUpdatedViewConnections());
			txtSyncedViewConnections.setText(String.valueOf(connection.countSyncedViewConnections()));		txtSyncedViewConnections.setData("value", connection.countSyncedViewConnections());
			//txtSyncedViewConnections.setForeground( (connection.countSyncedViewConnections() == exportedModel.getAllViewConnections().size()) ? GREEN_COLOR : (statusColor=RED_COLOR) );

			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllImagePaths().size()+" ViewsImages in the model: "+connection.countSyncedImages()+" synced, "+connection.countUpdatedImages()+" updated, "+connection.countNewImages()+" new.");
			txtNewImages.setText(String.valueOf(connection.countNewImages()));								txtNewImages.setData("value", connection.countNewImages());
			txtUpdatedImages.setText(String.valueOf(connection.countUpdatedImages()));						txtUpdatedImages.setData("value", connection.countUpdatedImages());
			txtSyncedImages.setText(String.valueOf(connection.countSyncedImages()));						txtSyncedImages.setData("value", connection.countSyncedImages());
			//txtSyncedImages.setForeground( (connection.countSyncedImages() == exportedModel.getAllImagePaths().size()) ? GREEN_COLOR : (statusColor=RED_COLOR) );

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

		if ( 	(connection.countSyncedElements() == exportedModel.getAllElements().size()) &&
				(connection.countSyncedRelationships() == exportedModel.getAllRelationships().size()) &&
				(connection.countSyncedFolders() == exportedModel.getAllFolders().size()) &&
				(connection.countSyncedViews() == exportedModel.getAllViews().size()) &&
				(connection.countSyncedViewObjects() == exportedModel.getAllViewObjects().size()) &&
				(connection.countSyncedViewConnections() == exportedModel.getAllViewConnections().size()) &&
				(connection.countSyncedImages() == exportedModel.getAllImagePaths().size()) ) {
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
		txtNewElements.setText("");
		txtUpdatedElements.setText("");
		txtSyncedElements.setText("");
		
		txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
		txtNewRelationships.setText("");
		txtUpdatedRelationships.setText("");
		txtSyncedRelationships.setText("");
		
		txtTotalFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
		txtNewFolders.setText("");
		txtUpdatedFolders.setText("");
		txtSyncedFolders.setText("");
		
		txtTotalViews.setText(String.valueOf(exportedModel.getAllViews().size()));
		txtNewViews.setText("");
		txtUpdatedViews.setText("");
		txtSyncedViews.setText("");
		
		txtTotalViewObjects.setText(String.valueOf(exportedModel.getAllViewObjects().size()));
		txtNewViewObjects.setText("");
		txtUpdatedViewObjects.setText("");
		txtSyncedViewObjects.setText("");
		
		txtTotalViewConnections.setText(String.valueOf(exportedModel.getAllViewConnections().size()));
		txtNewViewConnections.setText("");
		txtUpdatedViewConnections.setText("");
		txtSyncedViewConnections.setText("");
		
		txtTotalImages.setText(String.valueOf(((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()));
		txtNewImages.setText("");
		txtUpdatedImages.setText("");
		txtSyncedImages.setText("");
	}
	
	/**
	 * Loop on model components and call doExportEObject to export them<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button
	 */
	protected void export() {
		int progressBarWidth;
		if ( getOptionValue() ) {
			logger.info("Exporting model : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships, "+exportedModel.getAllFolders().size()+" folders, "+exportedModel.getAllViews().size()+" views, "+exportedModel.getAllViewObjects().size()+" views objects, "+exportedModel.getAllViewConnections().size()+" views connections, and "+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()+" images.");
			progressBarWidth = exportedModel.getAllFolders().size()+exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size()+exportedModel.getAllViews().size()+exportedModel.getAllViewObjects().size()+exportedModel.getAllViewConnections().size()+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size();
		} else {
			logger.info("Exporting components : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships.");
			progressBarWidth = exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size();
		}
		
		// we reset the counters because they may have been changed (in case of conflict for instance)
		connection.setCountNewElements((int)txtNewElements.getData("value"));							txtNewElements.setText(String.valueOf(connection.countNewElements()));
		connection.setCountUpdatedElements((int)txtUpdatedElements.getData("value"));					txtUpdatedElements.setText(String.valueOf(connection.countUpdatedElements()));
		connection.setCountSyncedElements((int)txtSyncedElements.getData("value"));						txtSyncedElements.setText(String.valueOf(connection.countSyncedElements()));

		connection.setCountNewRelationships((int)txtNewRelationships.getData("value"));					txtNewRelationships.setText(String.valueOf(connection.countNewRelationships()));
		connection.setCountUpdatedRelationships((int)txtUpdatedRelationships.getData("value"));			txtUpdatedRelationships.setText(String.valueOf(connection.countUpdatedRelationships()));
		connection.setCountSyncedRelationships((int)txtSyncedRelationships.getData("value"));			txtSyncedRelationships.setText(String.valueOf(connection.countSyncedRelationships()));

		if ( getOptionValue() ) {
			connection.setCountNewFolders((int)txtNewFolders.getData("value"));							txtNewFolders.setText(String.valueOf(connection.countNewFolders()));
			connection.setCountUpdatedFolders((int)txtUpdatedFolders.getData("value"));					txtUpdatedFolders.setText(String.valueOf(connection.countUpdatedFolders()));
			connection.setCountSyncedFolders((int)txtSyncedFolders.getData("value"));					txtSyncedFolders.setText(String.valueOf(connection.countSyncedFolders()));

			connection.setCountNewViews((int)txtNewViews.getData("value"));								txtNewViews.setText(String.valueOf(connection.countNewViews()));
			connection.setCountUpdatedViews((int)txtUpdatedViews.getData("value"));						txtUpdatedViews.setText(String.valueOf(connection.countUpdatedViews()));
			connection.setCountSyncedViews((int)txtSyncedViews.getData("value"));						txtSyncedViews.setText(String.valueOf(connection.countSyncedViews()));

			connection.setCountNewViewObjects((int)txtNewViewObjects.getData("value"));					txtNewViewObjects.setText(String.valueOf(connection.countNewViewObjects()));
			connection.setCountUpdatedViewObjects((int)txtUpdatedViewObjects.getData("value"));			txtUpdatedViewObjects.setText(String.valueOf(connection.countUpdatedViewObjects()));
			connection.setCountSyncedViewObjects((int)txtSyncedViewObjects.getData("value"));			txtSyncedViewObjects.setText(String.valueOf(connection.countSyncedViewObjects()));

			connection.setCountNewViewConnections((int)txtNewViewConnections.getData("value"));			txtNewViewConnections.setText(String.valueOf(connection.countNewViewConnections()));
			connection.setCountUpdatedViewConnections((int)txtUpdatedViewConnections.getData("value"));	txtUpdatedViewConnections.setText(String.valueOf(connection.countUpdatedViewConnections()));
			connection.setCountSyncedViewConnections((int)txtSyncedViewConnections.getData("value"));	txtSyncedViewConnections.setText(String.valueOf(connection.countSyncedViewConnections()));

			connection.setCountNewImages((int)txtNewImages.getData("value"));							txtNewImages.setText(String.valueOf(connection.countNewImages()));
			connection.setCountUpdatedImages((int)txtUpdatedImages.getData("value"));					txtUpdatedImages.setText(String.valueOf(connection.countUpdatedImages()));
			connection.setCountSyncedImages((int)txtSyncedImages.getData("value"));						txtSyncedImages.setText(String.valueOf(connection.countSyncedImages()));
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
		
		// we change the synced text fields to black
		txtSyncedElements.setForeground(BLACK_COLOR);
		txtSyncedRelationships.setForeground(BLACK_COLOR);
		txtSyncedFolders.setForeground(BLACK_COLOR);
		txtSyncedViews.setForeground(BLACK_COLOR);
		txtSyncedViewObjects.setForeground(BLACK_COLOR);
		txtSyncedViewConnections.setForeground(BLACK_COLOR);
		txtSyncedImages.setForeground(BLACK_COLOR);

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
			if ( getOptionValue() ) {
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
					doExportEObject(foldersIterator.next().getValue(), txtSyncedFolders, txtNewFolders, txtUpdatedFolders);
				}
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting elements");
			Iterator<Entry<String, IArchimateElement>> elementsIterator = exportedModel.getAllElements().entrySet().iterator();
			while ( elementsIterator.hasNext() ) {
				doExportEObject(elementsIterator.next().getValue(), txtSyncedElements, txtNewElements, txtUpdatedElements);
			}
	
			if ( logger.isDebugEnabled() ) logger.debug("Exporting relationships");
			Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = exportedModel.getAllRelationships().entrySet().iterator();
			while ( relationshipsIterator.hasNext() ) {
				doExportEObject(relationshipsIterator.next().getValue(), txtSyncedRelationships, txtNewRelationships, txtUpdatedRelationships);
			}
	
			if ( getOptionValue() ) {
				
				// we export first all the views in one go in order to check as quickly as possible if there are some conflicts
				List<IDiagramModel> exportedViews = new ArrayList<IDiagramModel>();
				
				if ( logger.isDebugEnabled() ) logger.debug("Exporting views");
				Iterator<Entry<String, IDiagramModel>> viewsIterator = exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue(); 
					if ( doExportEObject(view, txtSyncedViews, txtNewViews, txtUpdatedViews) ) {
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
					switch ( connection.exportImage(path, archiveMgr.getBytesFromEntry(path)) ) {
						case isNew :
							txtSyncedImages.setText(String.valueOf(Integer.valueOf(txtSyncedImages.getText())+1));
							txtNewImages.setText(String.valueOf(Integer.valueOf(txtNewImages.getText())-1));
							break;
						case isUpdated :
							txtSyncedImages.setText(String.valueOf(Integer.valueOf(txtSyncedImages.getText())+1));
							txtUpdatedImages.setText(String.valueOf(Integer.valueOf(txtUpdatedImages.getText())-1));
							break;
						case isSynced :
							// do nothing
							break;
					}
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
				popup(Level.FATAL, "Failed to rollback the transaction. Please check carrefullsy your database !", err);
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
		doExportEObject(viewObject, txtSyncedViewObjects, txtNewViewObjects, txtUpdatedViewObjects);
		
		if ( viewObject instanceof IConnectable) {
			for ( IDiagramModelConnection source: ((IConnectable)viewObject).getSourceConnections() ) {
				if ( connectionsAlreadyExported.get(source.getId()) == null ) {
					doExportEObject(source, txtSyncedViewConnections, txtNewViewConnections, txtUpdatedViewConnections);
					connectionsAlreadyExported.put(source.getId(), source);
				}
			}
			for ( IDiagramModelConnection target: ((IConnectable)viewObject).getTargetConnections() ) {
				if ( connectionsAlreadyExported.get(target.getId()) == null ) {
					doExportEObject(target, txtSyncedViewConnections, txtNewViewConnections, txtUpdatedViewConnections);
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
	private boolean doExportEObject(EObject eObjectToExport, Text txtSynced, Text txtNew, Text txtUpdated) throws Exception {
		assert(eObjectToExport instanceof IDBMetadata);
		assert(connection != null);
		
		boolean status = true;
		try {
			if ( connection.exportEObject(eObjectToExport, getOptionValue()) && txtSynced != null ) {
				txtSynced.setText(String.valueOf(Integer.valueOf(txtSynced.getText())+1));
				if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseStatus() == DATABASE_STATUS.isNew ) {
					if ( txtNew != null ) txtNew.setText(String.valueOf(Integer.valueOf(txtNew.getText())-1));
				} else if ( ((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseStatus() == DATABASE_STATUS.isUpdated ) {
					if ( txtUpdated != null ) txtUpdated.setText(String.valueOf(Integer.valueOf(txtUpdated.getText())-1));
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
					((IDBMetadata)eObjectToExport).getDBMetadata().setExportedVersion(((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion() + 1);
					((IDBMetadata)eObjectToExport).getDBMetadata().setDatabaseVersion(((IDBMetadata)eObjectToExport).getDBMetadata().getDatabaseVersion() + 1);
					((IDBMetadata)eObjectToExport).getDBMetadata().setConflictChoice(CONFLICT_CHOICE.askUser);	// just in case there is a new conflict
					status = doExportEObject(eObjectToExport, txtSynced, txtNew, txtUpdated);
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
	
						fillInCompareTable(tblCompareComponent, 0, conflictingComponent, null);
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
	
			tblCompareComponent = new Table(grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.V_SCROLL);
			tblCompareComponent.setBackground(GROUP_BACKGROUND_COLOR);
			tblCompareComponent.setHeaderVisible(true);
			tblCompareComponent.setLinesVisible(true);
			fd = new FormData();
			fd.top = new FormAttachment(lblCompare, 10);
			fd.left = new FormAttachment(0,10);
			fd.right = new FormAttachment(100, -10);
			fd.bottom = new FormAttachment(100, -40);
			tblCompareComponent.setLayoutData(fd);
	
			TableColumn colItems = new TableColumn(tblCompareComponent, SWT.NONE);
			colItems.setText("Items");
			colItems.setWidth(119);
	
			TableColumn colYourVersion = new TableColumn(tblCompareComponent, SWT.NONE);
			colYourVersion.setText("Your version");
			colYourVersion.setWidth(170);
	
			TableColumn colDatabaseVersion = new TableColumn(tblCompareComponent, SWT.NONE);
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
		    logger.trace("synced : "+txtSyncedElements.getText()+" elements, "+txtSyncedRelationships.getText()+" relationships, "+txtSyncedFolders.getText()+" folders, "+txtSyncedViews.getText()+" views, "+txtSyncedViewObjects.getText()+" view objects, "+txtSyncedViewConnections.getText()+" view connections.");
		    logger.trace("updated : "+txtUpdatedElements.getText()+" elements, "+txtUpdatedRelationships.getText()+" relationships, "+txtUpdatedFolders.getText()+" folders, "+txtUpdatedViews.getText()+" views, "+txtUpdatedViewObjects.getText()+" view objects, "+txtUpdatedViewConnections.getText()+" view connections.");
		    logger.trace("new : "+txtNewElements.getText()+" elements, "+txtNewRelationships.getText()+" relationships, "+txtNewFolders.getText()+" folders, "+txtNewViews.getText()+" views, "+txtNewViewObjects.getText()+" view objects, "+txtNewViewConnections.getText()+" view connections.");
		}

		Color statusColor = GREEN_COLOR;
		txtSyncedElements.setForeground( DBPlugin.areEqual(txtSyncedElements.getText(), txtTotalElements.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
		txtSyncedRelationships.setForeground( DBPlugin.areEqual(txtSyncedRelationships.getText(), txtTotalRelationships.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
		if ( getOptionValue() ) {
			txtSyncedFolders.setForeground( DBPlugin.areEqual(txtSyncedFolders.getText(), txtTotalFolders.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtSyncedViews.setForeground( DBPlugin.areEqual(txtSyncedViews.getText(), txtTotalViews.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtSyncedViewObjects.setForeground( DBPlugin.areEqual(txtSyncedViewObjects.getText(), txtTotalViewObjects.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtSyncedViewConnections.setForeground( DBPlugin.areEqual(txtSyncedViewConnections.getText(), txtTotalViewConnections.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
			txtSyncedImages.setForeground( DBPlugin.areEqual(txtSyncedImages.getText(), txtTotalImages.getText()) ? GREEN_COLOR : (statusColor=RED_COLOR) );
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

	private Table tblCompareComponent;
	private Table tblListConflicts;
	private Label lblCantExport;

	private Text txtReleaseNote;

	private Text txtTotalElements;
	private Text txtSyncedElements;
	private Text txtUpdatedElements;
	private Text txtNewElements;

	private Text txtTotalRelationships;
	private Text txtSyncedRelationships;
	private Text txtUpdatedRelationships;
	private Text txtNewRelationships;

	private Text txtTotalFolders;
	private Text txtSyncedFolders;
	private Text txtUpdatedFolders;
	private Text txtNewFolders;

	private Text txtTotalViews;
	private Text txtSyncedViews;
	private Text txtUpdatedViews;
	private Text txtNewViews;

	private Text txtTotalViewObjects;
	private Text txtSyncedViewObjects;
	private Text txtUpdatedViewObjects;
	private Text txtNewViewObjects;

	private Text txtTotalViewConnections;
	private Text txtSyncedViewConnections;
	private Text txtUpdatedViewConnections;
	private Text txtNewViewConnections;
	
	private Text txtTotalImages;
	private Text txtSyncedImages;
	private Text txtUpdatedImages;
	private Text txtNewImages;

	private Table tblModelVersions;
	private Text txtModelName;
	private Text txtPurpose;
}
