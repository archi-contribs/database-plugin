/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.lang.reflect.Method;
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
import org.archicontribs.database.DBPlugin.CONFLICT_CHOICE;
import org.archicontribs.database.connection.DBDatabaseExportConnection;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBCompoundCommand;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.DBMetadata.DATABASE_STATUS;
import org.archicontribs.database.model.commands.DBDeleteDiagramConnectionCommand;
import org.archicontribs.database.model.commands.DBDeleteDiagramObjectCommand;
import org.archicontribs.database.model.commands.DBDeleteProfileCommand;
import org.archicontribs.database.model.commands.DBImportElementFromIdCommand;
import org.archicontribs.database.model.commands.DBImportFolderFromIdCommand;
import org.archicontribs.database.model.commands.DBImportProfileFromIdCommand;
import org.archicontribs.database.model.commands.DBImportRelationshipFromIdCommand;
import org.archicontribs.database.model.commands.DBImportViewConnectionFromIdCommand;
import org.archicontribs.database.model.commands.DBImportViewFromIdCommand;
import org.archicontribs.database.model.commands.DBImportViewObjectFromIdCommand;
import org.archicontribs.database.model.commands.DBResolveConnectionsCommand;
import org.archicontribs.database.model.commands.DBResolveRelationshipsCommand;
import org.archicontribs.database.model.commands.DBSetFolderToLastKnownCommand;
import org.archicontribs.database.model.commands.IDBCommand;
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
import com.archimatetool.editor.model.commands.DeleteArchimateElementCommand;
import com.archimatetool.editor.model.commands.DeleteArchimateRelationshipCommand;
import com.archimatetool.editor.model.commands.DeleteDiagramModelCommand;
import com.archimatetool.editor.model.commands.DeleteFolderCommand;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProfile;

import lombok.Getter;

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

	HashMap<String, DBMetadata> newDatabaseComponents;

	private CommandStack stack;
	DBDatabaseExportConnection exportConnection;

	private final String ZERO = toString(0);

	/**
	 * Creates the GUI to export components and model
	 * @param model to export
	 * @param title of the dialog
	 * @throws Exception 
	 */
	public DBGuiExportModel(DBArchimateModel model, String title) throws Exception {
		// We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
		super(title);
		// We reference the exported model 
		this.exportedModel = model;
		this.includeNeo4j = true;

		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for exporting model \""+this.exportedModel.getName()+"\" (plugin version "+DBPlugin.pluginVersion.toString()+").");

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
		this.btnDoAction.setEnabled(false);

		// We rename the close button to "Cancel"
		this.btnClose.setText("Cancel");

		// We activate the Eclipse Help framework
		setHelpHref("exportModel.html");

		// we get the model's stack that is used for undo / redo
		this.stack = (CommandStack)this.exportedModel.getAdapter(CommandStack.class);
		
        this.includeNeo4j = true;
	}

	@Override
	public void run() {
		super.run();

		try {
			this.exportedModel.countAllObjects();
		} catch (Exception err) {
			DBGuiUtils.popup(Level.ERROR, "Failed to count model's components", err);
			return;
		} finally {
			closeMessage();
		}

		if ( logger.isDebugEnabled() ) logger.debug("The model has got "+this.exportedModel.getAllProfiles().size()+" specializations and "+this.exportedModel.getAllElements().size()+" elements and "+this.exportedModel.getAllRelationships().size()+" relationships and "+this.exportedModel.getAllFolders().size()+" folders and "+this.exportedModel.getAllViews().size()+" views and "+this.exportedModel.getAllViewObjects().size()+" objects and "+this.exportedModel.getAllViewConnections().size()+" connections.");

		this.txtTotalModelItself.setText("1");
		this.txtTotalProfiles.setText(toString(this.exportedModel.getProfiles().size()));
		this.txtTotalElements.setText(toString(this.exportedModel.getAllElements().size()));
		this.txtTotalRelationships.setText(toString(this.exportedModel.getAllRelationships().size()));
		this.txtTotalFolders.setText(toString(this.exportedModel.getAllFolders().size()));
		this.txtTotalViews.setText(toString(this.exportedModel.getAllViews().size()));
		this.txtTotalViewObjects.setText(toString(this.exportedModel.getAllViewObjects().size()));
		this.txtTotalViewConnections.setText(toString(this.exportedModel.getAllViewConnections().size()));
		this.txtTotalImages.setText(toString(this.exportedModel.getAllImagePaths().size()));

		try {
			getDatabases(this.includeNeo4j, this.exportedModel.getImportDatabaseId(), null);
		} catch (Exception err) {
			DBGuiUtils.popup(Level.ERROR, "Failed to get the databases.", err);
			return;
		}
	}

	/**
	 * Creates a group displaying details about the the model in the database (list of existing versions)
	 */
	private void createGrpModel() {
		this.grpModelVersions = new Group(this.compoRightBottom, SWT.NONE);
		this.grpModelVersions.setBackground(GROUP_BACKGROUND_COLOR);
		this.grpModelVersions.setText("Your model versions: ");
		this.grpModelVersions.setFont(GROUP_TITLE_FONT);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(this.grpComponents, -getDefaultMargin());
		this.grpModelVersions.setLayoutData(fd);
		this.grpModelVersions.setLayout(new FormLayout());

		this.tblModelVersions = new Table(this.grpModelVersions,  SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		this.tblModelVersions.setBackground(TABLE_BACKGROUND_COLOR);
		this.tblModelVersions.setLinesVisible(true);
		this.tblModelVersions.setHeaderVisible(true);
		this.tblModelVersions.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				boolean canExport = (DBGuiExportModel.this.tblModelVersions.getSelection() != null) && (DBGuiExportModel.this.tblModelVersions.getSelection().length > 0) && (DBGuiExportModel.this.tblModelVersions.getSelection()[0] != null);

				DBGuiExportModel.this.btnDoAction.setEnabled(canExport);
				DBGuiExportModel.this.btnCompareModelToDatabase.setEnabled(canExport);

				if ( canExport ) {
					boolean canChangeMetaData = ((DBGuiExportModel.this.exportConnection != null) && (DBGuiExportModel.this.tblModelVersions.getSelection()[0] == DBGuiExportModel.this.tblModelVersions.getItem(0)));

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
		fd.top = new FormAttachment(0, getDefaultMargin());
		fd.left = new FormAttachment(0, getDefaultMargin());
		fd.right = new FormAttachment(40, -getDefaultMargin());
		fd.bottom = new FormAttachment(100, -getDefaultMargin());
		this.tblModelVersions.setLayoutData(fd);

		TableColumn colVersion = new TableColumn(this.tblModelVersions, SWT.NONE);
		colVersion.setText("#");
		colVersion.setWidth(40);

		TableColumn colCreatedOn = new TableColumn(this.tblModelVersions, SWT.NONE);
		colCreatedOn.setText("Created on");
		colCreatedOn.setWidth(120);

		TableColumn colCreatedBy = new TableColumn(this.tblModelVersions, SWT.NONE);
		colCreatedBy.setText("Created by");
		colCreatedBy.setWidth(125);

		Label lblModelName = new Label(this.grpModelVersions, SWT.NONE);
		lblModelName.setBackground(GROUP_BACKGROUND_COLOR);
		lblModelName.setText("Model name:");
		fd = new FormData();
		fd.top = new FormAttachment(0, getDefaultMargin());
		fd.left = new FormAttachment(40, 0);
		lblModelName.setLayoutData(fd);

		this.txtModelName = new Text(this.grpModelVersions, SWT.BORDER);
		this.txtModelName.setText(this.exportedModel.getName());
		this.txtModelName.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblModelName, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblModelName, getDefaultMargin());
		fd.right = new FormAttachment(100, -getDefaultMargin());
		this.txtModelName.setLayoutData(fd);

		Label lblPurpose = new Label(this.grpModelVersions, SWT.NONE);
		lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
		lblPurpose.setText("Purpose:");
		fd = new FormData();
		fd.top = new FormAttachment(this.txtModelName, getDefaultMargin());
		fd.left = new FormAttachment(lblModelName, 0, SWT.LEFT);
		lblPurpose.setLayoutData(fd);

		this.txtPurpose = new Text(this.grpModelVersions, (DBPlugin.isWindowsOperatingSystem() ? SWT.WRAP : SWT.MULTI) | SWT.BORDER | SWT.V_SCROLL);
		this.txtPurpose.setText(this.exportedModel.getPurpose());
		this.txtPurpose.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.txtModelName, 5);
		fd.left = new FormAttachment(this.txtModelName, 0, SWT.LEFT);
		fd.right = new FormAttachment(100, -getDefaultMargin());
		fd.bottom = new FormAttachment(55, -5);
		this.txtPurpose.setLayoutData(fd);

		Label lblReleaseNote = new Label(this.grpModelVersions, SWT.NONE);
		lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		lblReleaseNote.setText("Release note:");
		fd = new FormData();
		fd.top = new FormAttachment(this.txtPurpose, getDefaultMargin());
		fd.left = new FormAttachment(lblPurpose, 0, SWT.LEFT);
		lblReleaseNote.setLayoutData(fd);

		this.txtReleaseNote = new Text(this.grpModelVersions, (DBPlugin.isWindowsOperatingSystem() ? SWT.WRAP : SWT.MULTI) | SWT.BORDER | SWT.V_SCROLL);
		//txtReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		this.txtReleaseNote.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(this.txtPurpose, 5);
		fd.left = new FormAttachment(this.txtModelName, 0, SWT.LEFT);
		fd.right = new FormAttachment(100, -getDefaultMargin());
		fd.bottom = new FormAttachment(100, -getDefaultMargin());
		this.txtReleaseNote.setLayoutData(fd);
	}

	/**
	 * Creates a group displaying details about the exported model's components:<br>
	 * - total number<br>
	 * - number sync'ed with the database<br>
	 * - number that do not exist in the database<br>
	 * - number that exist in the database but with different values. 
	 */
	private void createGrpComponents() {
		this.grpComponents = new Group(this.compoRightBottom, SWT.SHADOW_ETCHED_IN);
		this.grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
		this.grpComponents.setFont(GROUP_TITLE_FONT);
		this.grpComponents.setText("Your model's components: ");

		// we calculate the required height
		int requiredHeight = 13 * (getDefaultLabelHeight() + getDefaultMargin()) + 2 * getDefaultMargin();

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
		fd.top = new FormAttachment(0, 2*getDefaultLabelHeight()+getDefaultMargin());
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

		this.lblTotal = new Label(this.grpComponents, SWT.CENTER);
		this.lblTotal.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblTotal.setText("Total");
		fd = new FormData();
		fd.top = new FormAttachment(0, getDefaultLabelHeight());
		fd.left = new FormAttachment(20, 0);
		fd.right = new FormAttachment(28, 0);
		this.lblTotal.setLayoutData(fd);

		this.lblModel = new Label(this.grpComponents, SWT.CENTER);
		this.lblModel.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblModel.setText("Archi");
		fd = new FormData();
		fd.top = new FormAttachment(0, -5);
		fd.left = new FormAttachment(33, 0);
		fd.right = new FormAttachment(57, 0);
		this.lblModel.setLayoutData(fd);

		this.lblModelNew = new Label(this.grpComponents, SWT.CENTER);
		this.lblModelNew.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblModelNew.setText("New");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(33, 0);
		fd.right = new FormAttachment(41, -2);
		this.lblModelNew.setLayoutData(fd);

		this.lblModelUpdated = new Label(this.grpComponents, SWT.CENTER);
		this.lblModelUpdated.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblModelUpdated.setText("Updated");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(41, 1);
		fd.right = new FormAttachment(49, -1);
		this.lblModelUpdated.setLayoutData(fd);

		this.lblModelDeleted = new Label(this.grpComponents, SWT.CENTER);
		this.lblModelDeleted.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblModelDeleted.setText("Deleted");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(49, 2);
		fd.right = new FormAttachment(57, 0);
		this.lblModelDeleted.setLayoutData(fd);

		this.modelHorizontalSeparator = new Label(this.grpComponents, SWT.HORIZONTAL | SWT.SEPARATOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblModel, 2);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.modelHorizontalSeparator.setLayoutData(fd);

		this.modelVerticalSeparatorLeft = new Label(this.grpComponents, SWT.VERTICAL | SWT.SEPARATOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.modelHorizontalSeparator, -4, SWT.TOP);
		fd.bottom = new FormAttachment(this.modelHorizontalSeparator, 4, SWT.BOTTOM);
		fd.left = new FormAttachment(this.modelHorizontalSeparator);
		this.modelVerticalSeparatorLeft.setLayoutData(fd);

		this.modelVerticalSeparatorRight = new Label(this.grpComponents, SWT.VERTICAL | SWT.SEPARATOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.modelHorizontalSeparator, -4, SWT.TOP);
		fd.bottom = new FormAttachment(this.modelHorizontalSeparator, 4, SWT.BOTTOM);
		fd.right = new FormAttachment(this.modelHorizontalSeparator);
		this.modelVerticalSeparatorRight.setLayoutData(fd);

		this.lblDatabase = new Label(this.grpComponents, SWT.CENTER);
		this.lblDatabase.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblDatabase.setText("Database");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblModel, 0, SWT.TOP);
		fd.left = new FormAttachment(62, 0);
		fd.right = new FormAttachment(86, 0);
		this.lblDatabase.setLayoutData(fd);

		this.lblDatabaseNew = new Label(this.grpComponents, SWT.CENTER);
		this.lblDatabaseNew.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblDatabaseNew.setText("New");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(62, 0);
		fd.right = new FormAttachment(70, -2);
		this.lblDatabaseNew.setLayoutData(fd);

		this.lblDatabaseUpdated = new Label(this.grpComponents, SWT.CENTER);
		this.lblDatabaseUpdated.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblDatabaseUpdated.setText("Updated");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(70, 1);
		fd.right = new FormAttachment(78, -2);
		this.lblDatabaseUpdated.setLayoutData(fd);

		this.lblDatabaseDeleted = new Label(this.grpComponents, SWT.CENTER);
		this.lblDatabaseDeleted.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblDatabaseDeleted.setText("Deleted");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(78, 1);
		fd.right = new FormAttachment(86, 0);
		this.lblDatabaseDeleted.setLayoutData(fd);

		this.databaseHorizontalSeparator = new Label(this.grpComponents, SWT.HORIZONTAL | SWT.SEPARATOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblDatabase, 2);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.databaseHorizontalSeparator.setLayoutData(fd);

		this.databaseVerticalSeparatorLeft = new Label(this.grpComponents, SWT.VERTICAL | SWT.SEPARATOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.databaseHorizontalSeparator, -4, SWT.TOP);
		fd.bottom = new FormAttachment(this.databaseHorizontalSeparator, 4, SWT.BOTTOM);
		fd.left = new FormAttachment(this.databaseHorizontalSeparator);
		this.databaseVerticalSeparatorLeft.setLayoutData(fd);

		this.databaseVerticalSeparatorRight = new Label(this.grpComponents, SWT.VERTICAL | SWT.SEPARATOR);
		fd = new FormData();
		fd.top = new FormAttachment(this.databaseHorizontalSeparator, -4, SWT.TOP);
		fd.bottom = new FormAttachment(this.databaseHorizontalSeparator, 4, SWT.BOTTOM);
		fd.right = new FormAttachment(this.databaseHorizontalSeparator);
		this.databaseVerticalSeparatorRight.setLayoutData(fd);

		this.lblConflicts = new Label(this.grpComponents, SWT.CENTER);
		this.lblConflicts.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblConflicts.setText("Conflicts");
		fd = new FormData();
		fd.top = new FormAttachment(this.lblTotal, 0, SWT.TOP);
		fd.left = new FormAttachment(91, 0);
		fd.right = new FormAttachment(99, 0);
		this.lblConflicts.setLayoutData(fd);

		/* * * * * */
		
		this.txtTotalModelItself = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalModelItself.setEditable(false);
		this.txtTotalModelItself.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalModelItself.setLayoutData(fd);
		
		this.txtNewModelItselfInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewModelItselfInModel.setEditable(false);
		this.txtNewModelItselfInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewModelItselfInModel.setLayoutData(fd);
		
		this.txtUpdatedModelItselfInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedModelItselfInModel.setEditable(false);
		this.txtUpdatedModelItselfInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedModelItselfInModel.setLayoutData(fd);
		
		//this.txtUpdatedModelItselfInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		//this.txtUpdatedModelItselfInDatabase.setEditable(false);
		//this.txtUpdatedModelItselfInDatabase.setEnabled(false);
		//fd = new FormData(26,18);
		//fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
		//fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		//fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		//this.txtUpdatedModelItselfInDatabase.setLayoutData(fd);
		
		//this.txtConflictingModelItself = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		//this.txtConflictingModelItself.setEditable(false);
		//this.txtConflictingModelItself.setEnabled(false);
		//fd = new FormData(26,18);
		//fd.top = new FormAttachment(lblModelItself, 0, SWT.CENTER);
		//fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		//fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		//this.txtConflictingModelItself.setLayoutData(fd);
		
		/* * * * * */
		
		this.txtTotalProfiles = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalProfiles.setEditable(false);
		this.txtTotalProfiles.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalProfiles.setLayoutData(fd);
		
		this.txtNewProfilesInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewProfilesInModel.setEditable(false);
		this.txtNewProfilesInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewProfilesInModel.setLayoutData(fd);
		
		this.txtUpdatedProfilesInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedProfilesInModel.setEditable(false);
		this.txtUpdatedProfilesInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedProfilesInModel.setLayoutData(fd);
		
		this.txtDeletedProfilesInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedProfilesInModel.setEditable(false);
		this.txtDeletedProfilesInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedProfilesInModel.setLayoutData(fd);

		this.txtNewProfilesInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewProfilesInDatabase.setEditable(false);
		this.txtNewProfilesInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewProfilesInDatabase.setLayoutData(fd);

		this.txtUpdatedProfilesInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedProfilesInDatabase.setEditable(false);
		this.txtUpdatedProfilesInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedProfilesInDatabase.setLayoutData(fd);

		this.txtDeletedProfilesInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedProfilesInDatabase.setEditable(false);
		this.txtDeletedProfilesInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedProfilesInDatabase.setLayoutData(fd);

		this.txtConflictingProfiles = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingProfiles.setEditable(false);
		this.txtConflictingProfiles.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblProfiles, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingProfiles.setLayoutData(fd);
		
		/* * * * * */

		this.txtTotalElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalElements.setEditable(false);
		this.txtTotalElements.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalElements.setLayoutData(fd);

		this.txtNewElementsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewElementsInModel.setEditable(false);
		this.txtNewElementsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewElementsInModel.setLayoutData(fd);

		this.txtUpdatedElementsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedElementsInModel.setEditable(false);
		this.txtUpdatedElementsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedElementsInModel.setLayoutData(fd);

		this.txtDeletedElementsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedElementsInModel.setEditable(false);
		this.txtDeletedElementsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedElementsInModel.setLayoutData(fd);

		this.txtNewElementsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewElementsInDatabase.setEditable(false);
		this.txtNewElementsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewElementsInDatabase.setLayoutData(fd);

		this.txtUpdatedElementsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedElementsInDatabase.setEditable(false);
		this.txtUpdatedElementsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedElementsInDatabase.setLayoutData(fd);

		this.txtDeletedElementsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedElementsInDatabase.setEditable(false);
		this.txtDeletedElementsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedElementsInDatabase.setLayoutData(fd);

		this.txtConflictingElements = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingElements.setEditable(false);
		this.txtConflictingElements.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingElements.setLayoutData(fd);

		/* * * * * */

		this.txtTotalRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalRelationships.setEditable(false);
		this.txtTotalRelationships.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalRelationships.setLayoutData(fd);

		this.txtNewRelationshipsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewRelationshipsInModel.setEditable(false);
		this.txtNewRelationshipsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewRelationshipsInModel.setLayoutData(fd);

		this.txtUpdatedRelationshipsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedRelationshipsInModel.setEditable(false);
		this.txtUpdatedRelationshipsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedRelationshipsInModel.setLayoutData(fd);

		this.txtDeletedRelationshipsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedRelationshipsInModel.setEditable(false);
		this.txtDeletedRelationshipsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedRelationshipsInModel.setLayoutData(fd);

		this.txtNewRelationshipsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewRelationshipsInDatabase.setEditable(false);
		this.txtNewRelationshipsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewRelationshipsInDatabase.setLayoutData(fd);

		this.txtUpdatedRelationshipsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedRelationshipsInDatabase.setEditable(false);
		this.txtUpdatedRelationshipsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedRelationshipsInDatabase.setLayoutData(fd);

		this.txtDeletedRelationshipsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedRelationshipsInDatabase.setEditable(false);
		this.txtDeletedRelationshipsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedRelationshipsInDatabase.setLayoutData(fd);

		this.txtConflictingRelationships = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingRelationships.setEditable(false);
		this.txtConflictingRelationships.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingRelationships.setLayoutData(fd);

		/* * * * * */

		this.txtTotalFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalFolders.setEditable(false);
		this.txtTotalFolders.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalFolders.setLayoutData(fd);

		this.txtNewFoldersInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewFoldersInModel.setEditable(false);
		this.txtNewFoldersInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewFoldersInModel.setLayoutData(fd);

		this.txtUpdatedFoldersInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedFoldersInModel.setEditable(false);
		this.txtUpdatedFoldersInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedFoldersInModel.setLayoutData(fd);

		this.txtDeletedFoldersInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedFoldersInModel.setEditable(false);
		this.txtDeletedFoldersInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedFoldersInModel.setLayoutData(fd);

		this.txtNewFoldersInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewFoldersInDatabase.setEditable(false);
		this.txtNewFoldersInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewFoldersInDatabase.setLayoutData(fd);

		this.txtUpdatedFoldersInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedFoldersInDatabase.setEditable(false);
		this.txtUpdatedFoldersInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedFoldersInDatabase.setLayoutData(fd);

		this.txtDeletedFoldersInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedFoldersInDatabase.setEditable(false);
		this.txtDeletedFoldersInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedFoldersInDatabase.setLayoutData(fd);

		this.txtConflictingFolders = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingFolders.setEditable(false);
		this.txtConflictingFolders.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingFolders.setLayoutData(fd);

		/* * * * * */

		this.txtTotalViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalViews.setEditable(false);
		this.txtTotalViews.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalViews.setLayoutData(fd);

		this.txtNewViewsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewViewsInModel.setEditable(false);
		this.txtNewViewsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewViewsInModel.setLayoutData(fd);

		this.txtUpdatedViewsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedViewsInModel.setEditable(false);
		this.txtUpdatedViewsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedViewsInModel.setLayoutData(fd);

		this.txtDeletedViewsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedViewsInModel.setEditable(false);
		this.txtDeletedViewsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedViewsInModel.setLayoutData(fd);

		this.txtNewViewsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewViewsInDatabase.setEditable(false);
		this.txtNewViewsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewViewsInDatabase.setLayoutData(fd);

		this.txtUpdatedViewsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedViewsInDatabase.setEditable(false);
		this.txtUpdatedViewsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedViewsInDatabase.setLayoutData(fd);

		this.txtDeletedViewsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedViewsInDatabase.setEditable(false);
		this.txtDeletedViewsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedViewsInDatabase.setLayoutData(fd);

		this.txtConflictingViews = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingViews.setEditable(false);
		this.txtConflictingViews.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingViews.setLayoutData(fd);

		/* * * * * */

		this.txtTotalViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalViewObjects.setEditable(false);
		this.txtTotalViewObjects.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalViewObjects.setLayoutData(fd);

		this.txtNewViewObjectsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewViewObjectsInModel.setEditable(false);
		this.txtNewViewObjectsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewViewObjectsInModel.setLayoutData(fd);

		this.txtUpdatedViewObjectsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedViewObjectsInModel.setEditable(false);
		this.txtUpdatedViewObjectsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedViewObjectsInModel.setLayoutData(fd);

		this.txtDeletedViewObjectsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedViewObjectsInModel.setEditable(false);
		this.txtDeletedViewObjectsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedViewObjectsInModel.setLayoutData(fd);

		this.txtNewViewObjectsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewViewObjectsInDatabase.setEditable(false);
		this.txtNewViewObjectsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewViewObjectsInDatabase.setLayoutData(fd);

		this.txtUpdatedViewObjectsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedViewObjectsInDatabase.setEditable(false);
		this.txtUpdatedViewObjectsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedViewObjectsInDatabase.setLayoutData(fd);

		this.txtDeletedViewObjectsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedViewObjectsInDatabase.setEditable(false);
		this.txtDeletedViewObjectsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedViewObjectsInDatabase.setLayoutData(fd);

		this.txtConflictingViewObjects = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingViewObjects.setEditable(false);
		this.txtConflictingViewObjects.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingViewObjects.setLayoutData(fd);

		/* * * * * */

		this.txtTotalViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalViewConnections.setEditable(false);
		this.txtTotalViewConnections.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalViewConnections.setLayoutData(fd);

		this.txtNewViewConnectionsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewViewConnectionsInModel.setEditable(false);
		this.txtNewViewConnectionsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewViewConnectionsInModel.setLayoutData(fd);

		this.txtUpdatedViewConnectionsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedViewConnectionsInModel.setEditable(false);
		this.txtUpdatedViewConnectionsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelUpdated, 0, SWT.RIGHT);
		this.txtUpdatedViewConnectionsInModel.setLayoutData(fd);

		this.txtDeletedViewConnectionsInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedViewConnectionsInModel.setEditable(false);
		this.txtDeletedViewConnectionsInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelDeleted, 0, SWT.RIGHT);
		this.txtDeletedViewConnectionsInModel.setLayoutData(fd);

		this.txtNewViewConnectionsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewViewConnectionsInDatabase.setEditable(false);
		this.txtNewViewConnectionsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewViewConnectionsInDatabase.setLayoutData(fd);

		this.txtUpdatedViewConnectionsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtUpdatedViewConnectionsInDatabase.setEditable(false);
		this.txtUpdatedViewConnectionsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseUpdated, 0, SWT.RIGHT);
		this.txtUpdatedViewConnectionsInDatabase.setLayoutData(fd);

		this.txtDeletedViewConnectionsInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtDeletedViewConnectionsInDatabase.setEditable(false);
		this.txtDeletedViewConnectionsInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseDeleted, 0, SWT.RIGHT);
		this.txtDeletedViewConnectionsInDatabase.setLayoutData(fd);

		this.txtConflictingViewConnections = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtConflictingViewConnections.setEditable(false);
		this.txtConflictingViewConnections.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblViewConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblConflicts, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblConflicts, 0, SWT.RIGHT);
		this.txtConflictingViewConnections.setLayoutData(fd);

		/* * * * * */

		this.txtTotalImages = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtTotalImages.setEditable(false);
		this.txtTotalImages.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblTotal, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblTotal, 0, SWT.RIGHT);
		this.txtTotalImages.setLayoutData(fd);

		this.txtNewImagesInModel = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewImagesInModel.setEditable(false);
		this.txtNewImagesInModel.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblModelNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblModelNew, 0, SWT.RIGHT);
		this.txtNewImagesInModel.setLayoutData(fd);

		this.txtNewImagesInDatabase = new Text(this.grpComponents, SWT.BORDER | SWT.CENTER);
		this.txtNewImagesInDatabase.setEditable(false);
		this.txtNewImagesInDatabase.setEnabled(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(lblImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(this.lblDatabaseNew, 0, SWT.LEFT);
		fd.right = new FormAttachment(this.lblDatabaseNew, 0, SWT.RIGHT);
		this.txtNewImagesInDatabase.setLayoutData(fd);

		/* * * * * */
		this.btnCompareModelToDatabase = new Button(this.grpComponents, SWT.PUSH);
		this.btnCompareModelToDatabase.setText("Compare model to the database");
		fd = new FormData();
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(100, -5);
		this.btnCompareModelToDatabase.setLayoutData(fd);
		this.btnCompareModelToDatabase.addSelectionListener(new SelectionListener() {
			@Override public void widgetSelected(SelectionEvent e) {
				boolean upToDate = false;
				DBGuiExportModel.this.btnDoAction.setEnabled(false);
				DBGuiExportModel.this.btnCompareModelToDatabase.setEnabled(false);
				try {
					upToDate = DBGuiExportModel.this.compareModelToDatabase(true);
				} catch (Exception err) {
					DBGuiUtils.popup(Level.ERROR, "Failed to compare the model to the database.", err);
				}
				DBGuiExportModel.this.btnCompareModelToDatabase.setEnabled(true);
				if ( upToDate ) {
					DBGuiUtils.popup(Level.INFO, "Your database is already up to date.");
					DBGuiExportModel.this.btnClose.setText("Close");
					DBGuiExportModel.this.btnDoAction.setEnabled(false);
				} else
					DBGuiExportModel.this.btnDoAction.setEnabled(true);
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

		boolean isNeo4j = DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j");

		this.lblModelNew.setText(isNeo4j ? "Exported" : "New");

		this.modelHorizontalSeparator.setVisible(!isNeo4j);
		this.modelVerticalSeparatorRight.setVisible(!isNeo4j);
		this.modelVerticalSeparatorRight.setVisible(!isNeo4j);

		this.databaseHorizontalSeparator.setVisible(!isNeo4j);
		this.databaseVerticalSeparatorRight.setVisible(!isNeo4j);
		this.databaseVerticalSeparatorRight.setVisible(!isNeo4j);
		
		// we hide the comparison between the model and the database in case of a neo4j database
		this.lblModel.setVisible(!isNeo4j);
		this.lblModelDeleted.setVisible(!isNeo4j);
		this.lblModelUpdated.setVisible(!isNeo4j);
		this.txtTotalModelItself.setVisible(!isNeo4j);
		
		this.txtNewModelItselfInModel.setVisible(!isNeo4j);         this.txtNewModelItselfInModel.setText(this.ZERO);
		                                                            this.txtNewProfilesInModel.setText(this.ZERO);
		                                                            this.txtNewElementsInModel.setText(this.ZERO);
		                                                            this.txtNewRelationshipsInModel.setText(this.ZERO);
		this.txtUpdatedModelItselfInModel.setVisible(!isNeo4j);     this.txtUpdatedModelItselfInModel.setText(this.ZERO);
		this.txtUpdatedProfilesInModel.setVisible(!isNeo4j);        this.txtUpdatedProfilesInModel.setText(this.ZERO);
		this.txtDeletedProfilesInModel.setVisible(!isNeo4j);        this.txtDeletedProfilesInModel.setText(this.ZERO);
		this.txtUpdatedElementsInModel.setVisible(!isNeo4j);        this.txtUpdatedElementsInModel.setText(this.ZERO);
		this.txtDeletedElementsInModel.setVisible(!isNeo4j);        this.txtDeletedElementsInModel.setText(this.ZERO);
		this.txtUpdatedRelationshipsInModel.setVisible(!isNeo4j);   this.txtUpdatedRelationshipsInModel.setText(this.ZERO);
		this.txtDeletedRelationshipsInModel.setVisible(!isNeo4j);   this.txtDeletedRelationshipsInModel.setText(this.ZERO);
		this.txtNewFoldersInModel.setVisible(!isNeo4j);             this.txtNewFoldersInModel.setText(this.ZERO);
		this.txtUpdatedFoldersInModel.setVisible(!isNeo4j);         this.txtUpdatedFoldersInModel.setText(this.ZERO);
		this.txtDeletedFoldersInModel.setVisible(!isNeo4j);         this.txtDeletedFoldersInModel.setText(this.ZERO);
		this.txtNewViewsInModel.setVisible(!isNeo4j);               this.txtNewViewsInModel.setText(this.ZERO);
		this.txtUpdatedViewsInModel.setVisible(!isNeo4j);           this.txtUpdatedViewsInModel.setText(this.ZERO);
		this.txtDeletedViewsInModel.setVisible(!isNeo4j);           this.txtDeletedViewsInModel.setText(this.ZERO);
		this.txtNewViewObjectsInModel.setVisible(!isNeo4j);         this.txtNewViewObjectsInModel.setText(this.ZERO);
		this.txtUpdatedViewObjectsInModel.setVisible(!isNeo4j);     this.txtUpdatedViewObjectsInModel.setText(this.ZERO);
		this.txtDeletedViewObjectsInModel.setVisible(!isNeo4j);     this.txtDeletedViewObjectsInModel.setText(this.ZERO);
		this.txtNewViewConnectionsInModel.setVisible(!isNeo4j);     this.txtNewViewConnectionsInModel.setText(this.ZERO);
		this.txtUpdatedViewConnectionsInModel.setVisible(!isNeo4j); this.txtUpdatedViewConnectionsInModel.setText(this.ZERO);
		this.txtDeletedViewConnectionsInModel.setVisible(!isNeo4j); this.txtDeletedViewConnectionsInModel.setText(this.ZERO);
		this.txtNewImagesInModel.setVisible(!isNeo4j);              this.txtNewImagesInModel.setText(this.ZERO);

		this.lblDatabase.setVisible(!isNeo4j);
		this.lblDatabaseNew.setVisible(!isNeo4j);
		this.lblDatabaseDeleted.setVisible(!isNeo4j);
		this.lblDatabaseUpdated.setVisible(!isNeo4j);
		this.lblConflicts.setVisible(!isNeo4j);
		this.txtNewProfilesInDatabase.setVisible(!isNeo4j);            this.txtNewProfilesInDatabase.setText(this.ZERO);
		this.txtUpdatedProfilesInDatabase.setVisible(!isNeo4j);        this.txtUpdatedProfilesInDatabase.setText(this.ZERO);
		this.txtDeletedProfilesInDatabase.setVisible(!isNeo4j);        this.txtDeletedProfilesInDatabase.setText(this.ZERO);
		this.txtConflictingProfiles.setVisible(!isNeo4j);              this.txtConflictingProfiles.setText(this.ZERO);
		this.txtNewElementsInDatabase.setVisible(!isNeo4j);            this.txtNewElementsInDatabase.setText(this.ZERO);
		this.txtUpdatedElementsInDatabase.setVisible(!isNeo4j);        this.txtUpdatedElementsInDatabase.setText(this.ZERO);
		this.txtDeletedElementsInDatabase.setVisible(!isNeo4j);        this.txtDeletedElementsInDatabase.setText(this.ZERO);
		this.txtConflictingElements.setVisible(!isNeo4j);              this.txtConflictingElements.setText(this.ZERO);
		this.txtNewRelationshipsInDatabase.setVisible(!isNeo4j);       this.txtNewRelationshipsInDatabase.setText(this.ZERO);
		this.txtUpdatedRelationshipsInDatabase.setVisible(!isNeo4j);   this.txtUpdatedRelationshipsInDatabase.setText(this.ZERO);
		this.txtDeletedRelationshipsInDatabase.setVisible(!isNeo4j);   this.txtDeletedRelationshipsInDatabase.setText(this.ZERO);
		this.txtConflictingRelationships.setVisible(!isNeo4j);         this.txtConflictingRelationships.setText(this.ZERO);
		this.txtNewFoldersInDatabase.setVisible(!isNeo4j);             this.txtNewFoldersInDatabase.setText(this.ZERO);
		this.txtUpdatedFoldersInDatabase.setVisible(!isNeo4j);         this.txtUpdatedFoldersInDatabase.setText(this.ZERO);
		this.txtDeletedFoldersInDatabase.setVisible(!isNeo4j);         this.txtDeletedFoldersInDatabase.setText(this.ZERO);
		this.txtConflictingFolders.setVisible(!isNeo4j);               this.txtConflictingFolders.setText(this.ZERO);
		this.txtNewViewsInDatabase.setVisible(!isNeo4j);               this.txtNewViewsInDatabase.setText(this.ZERO);
		this.txtUpdatedViewsInDatabase.setVisible(!isNeo4j);           this.txtUpdatedViewsInDatabase.setText(this.ZERO);
		this.txtDeletedViewsInDatabase.setVisible(!isNeo4j);           this.txtDeletedViewsInDatabase.setText(this.ZERO);
		this.txtConflictingViews.setVisible(!isNeo4j);                 this.txtConflictingViews.setText(this.ZERO);
		this.txtNewViewObjectsInDatabase.setVisible(!isNeo4j);         this.txtNewViewObjectsInDatabase.setText(this.ZERO);
		this.txtUpdatedViewObjectsInDatabase.setVisible(!isNeo4j);     this.txtUpdatedViewObjectsInDatabase.setText(this.ZERO);
		this.txtDeletedViewObjectsInDatabase.setVisible(!isNeo4j);     this.txtDeletedViewObjectsInDatabase.setText(this.ZERO);
		this.txtConflictingViewObjects.setVisible(!isNeo4j);           this.txtConflictingViewObjects.setText(this.ZERO);
		this.txtNewViewConnectionsInDatabase.setVisible(!isNeo4j);     this.txtNewViewConnectionsInDatabase.setText(this.ZERO);
		this.txtUpdatedViewConnectionsInDatabase.setVisible(!isNeo4j); this.txtUpdatedViewConnectionsInDatabase.setText(this.ZERO);
		this.txtDeletedViewConnectionsInDatabase.setVisible(!isNeo4j); this.txtDeletedViewConnectionsInDatabase.setText(this.ZERO);
		this.txtConflictingViewConnections.setVisible(!isNeo4j);       this.txtConflictingViewConnections.setText(this.ZERO);
		this.txtNewImagesInDatabase.setVisible(!isNeo4j);              this.txtNewImagesInDatabase.setText(this.ZERO);

		DBGuiExportModel.this.tblModelVersions.removeAll();

		// if the first line, then we add the "latest version"
		TableItem tableItem = new TableItem(DBGuiExportModel.this.tblModelVersions, SWT.NULL);
		tableItem.setText(1, "Now");
		tableItem.setData("name", this.exportedModel.getName());
		tableItem.setData("note", "");
		tableItem.setData("purpose", this.exportedModel.getPurpose());
		DBGuiExportModel.this.tblModelVersions.setSelection(tableItem);
		DBGuiExportModel.this.tblModelVersions.notifyListeners(SWT.Selection, new Event());		// activates the name, note and purpose texts

		// if we're not in a Neo4J database, then we get the latest version and checksum of the model's components in the database
		try {
			if ( !isNeo4j ) {
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
			DBGuiUtils.popup(Level.FATAL, "Failed to check existing components in database", err);
			setActiveAction(STATUS.Error);
			return;
		}

		this.btnCompareModelToDatabase.setVisible(!isNeo4j);

		if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("exportWithDefaultValues") ) {
			// if the exportWithDefaultValues preference is set, then we automatically start the export
			logger.debug("Automatically start export as specified in preferences.");
			this.btnDoAction.setEnabled(true);
			this.btnDoAction.notifyListeners(SWT.Selection, new Event());
			return;
		}

		if ( !isNeo4j && DBPlugin.INSTANCE.getPreferenceStore().getBoolean("compareBeforeExport") ) {
			// if the compareBeforeExport is set
			boolean upToDate = false;
			this.btnDoAction.setEnabled(false);
			this.btnCompareModelToDatabase.setEnabled(false);
			try {
				upToDate = compareModelToDatabase(true);
			} catch (Exception err) {
				DBGuiUtils.popup(Level.ERROR, "Failed to compare the model to the database.", err);
			}

			this.btnCompareModelToDatabase.setEnabled(true);
			closeMessage();
			if ( upToDate ) {
				DBGuiUtils.popup(Level.INFO, "Your database is already up to date.");
				DBGuiExportModel.this.btnClose.setText("Close");
				this.btnDoAction.setEnabled(false);
			} else
				this.btnDoAction.setEnabled(true);
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
		this.txtNewProfilesInModel.setVisible(!isNeo4j);
		this.txtUpdatedModelItselfInModel.setVisible(!isNeo4j);
		this.txtUpdatedProfilesInModel.setVisible(!isNeo4j);
		this.txtDeletedProfilesInModel.setVisible(!isNeo4j);
		this.txtNewRelationshipsInModel.setVisible(!isNeo4j);
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


		// we hide the database and conflict columns if Neo4J
		this.lblDatabase.setVisible(!isNeo4j);
		this.lblDatabaseNew.setVisible(!isNeo4j);
		this.lblDatabaseDeleted.setVisible(!isNeo4j);
		this.lblDatabaseUpdated.setVisible(!isNeo4j);
		this.lblConflicts.setVisible(!isNeo4j);
		//this.txtUpdatedModelItselfInDatabase.setVisible(!isNeo4j);
		this.txtNewProfilesInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedProfilesInDatabase.setVisible(!isNeo4j);
		this.txtDeletedProfilesInDatabase.setVisible(!isNeo4j);
		this.txtConflictingProfiles.setVisible(!isNeo4j);
		this.txtNewElementsInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedElementsInDatabase.setVisible(!isNeo4j);
		this.txtDeletedElementsInDatabase.setVisible(!isNeo4j);
		this.txtConflictingElements.setVisible(!isNeo4j);
		this.txtNewRelationshipsInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedRelationshipsInDatabase.setVisible(!isNeo4j);
		this.txtDeletedRelationshipsInDatabase.setVisible(!isNeo4j);
		this.txtConflictingRelationships.setVisible(!isNeo4j);
		this.txtNewFoldersInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedFoldersInDatabase.setVisible(!isNeo4j);
		this.txtDeletedFoldersInDatabase.setVisible(!isNeo4j);
		this.txtConflictingFolders.setVisible(!isNeo4j);
		this.txtNewViewsInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedViewsInDatabase.setVisible(!isNeo4j);
		this.txtDeletedViewsInDatabase.setVisible(!isNeo4j);
		this.txtConflictingViews.setVisible(!isNeo4j);
		this.txtNewViewObjectsInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedViewObjectsInDatabase.setVisible(!isNeo4j);
		this.txtDeletedViewObjectsInDatabase.setVisible(!isNeo4j);
		this.txtConflictingViewObjects.setVisible(!isNeo4j);
		this.txtNewViewConnectionsInDatabase.setVisible(!isNeo4j);
		this.txtUpdatedViewConnectionsInDatabase.setVisible(!isNeo4j);
		this.txtDeletedViewConnectionsInDatabase.setVisible(!isNeo4j);
		this.txtConflictingViewConnections.setVisible(!isNeo4j);
		this.txtNewImagesInDatabase.setVisible(!isNeo4j);

		                                             		  this.txtUpdatedModelItselfInModel.setText(this.ZERO);		                                                			/*this.txtUpdatedModelItselfInDatabase.setText(this.ZERO);			this.txtConflictingModelItself.setText(this.ZERO);*/
		this.txtNewProfilesInModel.setText(this.ZERO);		  this.txtUpdatedProfilesInModel.setText(this.ZERO);		this.txtNewProfilesInDatabase.setText(this.ZERO);			this.txtUpdatedProfilesInDatabase.setText(this.ZERO);			this.txtConflictingProfiles.setText(this.ZERO);
		this.txtNewElementsInModel.setText(this.ZERO);		  this.txtUpdatedElementsInModel.setText(this.ZERO);		this.txtNewElementsInDatabase.setText(this.ZERO);			this.txtUpdatedElementsInDatabase.setText(this.ZERO);			this.txtConflictingElements.setText(this.ZERO);
		this.txtNewRelationshipsInModel.setText(this.ZERO);	  this.txtUpdatedRelationshipsInModel.setText(this.ZERO);	this.txtNewRelationshipsInDatabase.setText(this.ZERO);		this.txtUpdatedRelationshipsInDatabase.setText(this.ZERO);		this.txtConflictingRelationships.setText(this.ZERO);
		this.txtNewFoldersInModel.setText(this.ZERO);		  this.txtUpdatedFoldersInModel.setText(this.ZERO);			this.txtNewFoldersInDatabase.setText(this.ZERO);			this.txtUpdatedFoldersInDatabase.setText(this.ZERO);			this.txtConflictingFolders.setText(this.ZERO);
		this.txtNewViewsInModel.setText(this.ZERO);			  this.txtUpdatedViewsInModel.setText(this.ZERO);			this.txtNewViewsInDatabase.setText(this.ZERO);				this.txtUpdatedViewsInDatabase.setText(this.ZERO);				this.txtConflictingViews.setText(this.ZERO);
		this.txtNewViewObjectsInModel.setText(this.ZERO);     this.txtUpdatedViewObjectsInModel.setText(this.ZERO);     this.txtNewViewObjectsInDatabase.setText(this.ZERO);     	this.txtUpdatedViewObjectsInDatabase.setText(this.ZERO);		this.txtConflictingViewObjects.setText(this.ZERO);
		this.txtNewViewConnectionsInModel.setText(this.ZERO); this.txtUpdatedViewConnectionsInModel.setText(this.ZERO); this.txtNewViewConnectionsInDatabase.setText(this.ZERO); 	this.txtUpdatedViewConnectionsInDatabase.setText(this.ZERO);	this.txtConflictingViewConnections.setText(this.ZERO);
	}

	/**
	 * Update the text widgets that shows up the new, updated and deleted components
	 * 
	 * @return true is the database is up to date, false if the model needs to be exported
	 */
	protected boolean compareModelToDatabase(boolean updateTextFields) throws Exception {
		// We do not verify the content of neo4j database, we just export the components
		if ( DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") )
			return true;
		
		// calculating model's checksum
		this.exportedModel.getCurrentVersion().setChecksum(DBChecksum.calculateChecksum(this.exportedModel, this.txtReleaseNote.getText()));
		
		int progressBarWidth = this.exportedModel.getAllElements().size() + this.exportedModel.getAllRelationships().size() + this.exportedModel.getAllFolders().size() + this.exportedModel.getAllViews().size() + this.exportedModel.getAllViewObjects().size() + this.exportedModel.getAllViewConnections().size();
		createProgressBar("Comparing the model to the database ...", 1, progressBarWidth);
		
		try {
			// we compare the elements, relationships, folders and views
			this.exportConnection.getAllVersionFromDatabase(this.exportedModel, this);
		} catch (SQLException err ) {
			hideProgressBar();
			DBGuiUtils.popup(Level.FATAL, "Failed to get latest version of components in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Error while exporting model.\n"+err.getMessage());
			return false;
		}

		// we create the view screenshots if the database is configured to export them
		hideProgressBar();
		hideGrpDatabase();
		createProgressBar("Checking if view screenshots are required", 1, this.exportedModel.getAllViews().size());
		Iterator<Entry<String, IDiagramModel>> screenshotsIterator = this.exportedModel.getAllViews().entrySet().iterator();
		while ( screenshotsIterator.hasNext() ) {
			increaseProgressBar();
			IDiagramModel view = screenshotsIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(view);
			if ( this.exportConnection.getDatabaseEntry().isViewSnapshotRequired() ) {
				if ( (metadata.getScreenshot().getBytes() == null)
						|| (metadata.getScreenshot().getScaleFactor() != this.exportConnection.getDatabaseEntry().getViewsImagesScaleFactor())
						|| (metadata.getScreenshot().getBodrderWidth() != this.exportConnection.getDatabaseEntry().getViewsImagesBorderWidth())
						) {
					setProgressBarLabel("Creating screenshot of view \""+view.getName()+"\"");
					createImage(view, this.exportConnection.getDatabaseEntry().getViewsImagesScaleFactor(), this.exportConnection.getDatabaseEntry().getViewsImagesBorderWidth());
				}
				metadata.getScreenshot().setScreenshotActive(true);
			} else
				metadata.getScreenshot().setScreenshotActive(false);

			// we recalculate the view checksum using the screenshot (or not)
			this.exportedModel.countObject(view, true);
		}
		hideProgressBar();
		showGrpDatabase();
		setMessage("Calculating number of new, updated and deleted components.");

		int total = 0;
		
		/**
		 * if the model's database version equals zero, the the model does not exist in the database
		 * else if the model's checksum differs from the database, then it has been updated 
		 */
		if ( this.exportedModel.getDatabaseVersion().getVersion() == 0 ) {
			this.txtNewModelItselfInModel.setText("1");
			++total;
		} else if ( ! this.exportedModel.getCurrentVersion().getChecksum().toString().equals(this.exportedModel.getDatabaseVersion().getChecksum().toString())) {
			this.txtUpdatedModelItselfInModel.setText("1");
			++total ;
		}
		
		// TODO: check if model has been updated in database and manage conflict as every other component   
		
		int nbNew = 0;
		int nbNewInDb = 0;
		int nbUpdated = 0;
		int nbUpdatedInDb = 0;
		int nbConflict = 0;
		int nbDeleted = 0;
		int nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IProfile>> profilesIterator = this.exportedModel.getAllProfiles().entrySet().iterator();
		while (profilesIterator.hasNext()) {
			IProfile profile = profilesIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(profile);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					if ( this.exportedModel.getAllConflicts().get(profile) == null )
						this.exportedModel.getAllConflicts().put(profile, CONFLICT_CHOICE.askUser);
					switch ( this.exportedModel.getAllConflicts().get(profile) ) {
						case doNotExport:   // nothing to do
							break;
						case exportToDatabase:
							++nbUpdated;
							break;
						case importFromDatabase:
							++nbUpdatedInDb;
							break;
						case askUser:
						default:
							++nbConflict;
					}
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the Profiles new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getProfilesNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded

		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if ( updateTextFields ) {
			this.txtNewProfilesInModel.setText(toString(nbNew));
			this.txtNewProfilesInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedProfilesInModel.setText(toString(nbUpdated));
			this.txtUpdatedProfilesInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingProfiles.setText(toString(nbConflict));
			this.txtDeletedProfilesInModel.setText(toString(nbDeleted));
			this.txtDeletedProfilesInDatabase.setText(toString(nbDeletedInDb));
		}

		nbNew = 0;
		nbNewInDb = 0;
		nbUpdated = 0;
		nbUpdatedInDb = 0;
		nbConflict = 0;
		nbDeleted = 0;
		nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
		while (elementsIterator.hasNext()) {
			IArchimateElement element = elementsIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(element);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					if ( this.exportedModel.getAllConflicts().get(element) == null )
						this.exportedModel.getAllConflicts().put(element, CONFLICT_CHOICE.askUser);
					switch ( this.exportedModel.getAllConflicts().get(element) ) {
						case doNotExport:   // nothing to do
							break;
						case exportToDatabase:
							++nbUpdated;
							break;
						case importFromDatabase:
							++nbUpdatedInDb;
							break;
						case askUser:
						default:
							++nbConflict;
					}
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the elements new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getElementsNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded

		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if ( updateTextFields ) {
			this.txtNewElementsInModel.setText(toString(nbNew));
			this.txtNewElementsInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedElementsInModel.setText(toString(nbUpdated));
			this.txtUpdatedElementsInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingElements.setText(toString(nbConflict));
			this.txtDeletedElementsInModel.setText(toString(nbDeleted));
			this.txtDeletedElementsInDatabase.setText(toString(nbDeletedInDb));
		}

		nbNew = 0;
		nbNewInDb = 0;
		nbUpdated = 0;
		nbUpdatedInDb = 0;
		nbConflict = 0;
		nbDeleted = 0;
		nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
		while (relationshipsIterator.hasNext()) {
			IArchimateRelationship relationship = relationshipsIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(relationship);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					if ( this.exportedModel.getAllConflicts().get(relationship) == null )
						this.exportedModel.getAllConflicts().put(relationship, CONFLICT_CHOICE.askUser);
					switch ( this.exportedModel.getAllConflicts().get(relationship) ) {
						case doNotExport:   // nothing to do
							break;
						case exportToDatabase:
							++nbUpdated;
							break;
						case importFromDatabase:
							++nbUpdatedInDb;
							break;
						case askUser:
						default:    // askUSer
							++nbConflict;
					}
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the relationships new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getRelationshipsNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded
		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if( updateTextFields ) {
			this.txtNewRelationshipsInModel.setText(toString(nbNew));
			this.txtNewRelationshipsInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedRelationshipsInModel.setText(toString(nbUpdated));
			this.txtUpdatedRelationshipsInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingRelationships.setText(toString(nbConflict));
			this.txtDeletedRelationshipsInModel.setText(toString(nbDeleted));
			this.txtDeletedRelationshipsInDatabase.setText(toString(nbDeletedInDb));
		}

		nbNew = 0;
		nbNewInDb = 0;
		nbUpdated = 0;
		nbUpdatedInDb = 0;
		nbConflict = 0;
		nbDeleted = 0;
		nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IFolder>> folderIterator = this.exportedModel.getAllFolders().entrySet().iterator();
		while (folderIterator.hasNext()) {
			IFolder folder = folderIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(folder);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					// There is no conflict for folders: conflicts are managed with their content
					// If a folder has been updated both in the model and the database, then we export a new version
					++nbUpdated;
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the folders new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getFoldersNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded
		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if ( updateTextFields ) {
			this.txtNewFoldersInModel.setText(toString(nbNew));
			this.txtNewFoldersInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedFoldersInModel.setText(toString(nbUpdated));
			this.txtUpdatedFoldersInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingFolders.setText(toString(nbConflict));
			this.txtDeletedFoldersInModel.setText(toString(nbDeleted));
			this.txtDeletedFoldersInDatabase.setText(toString(nbDeletedInDb));
		}

		nbNew = 0;
		nbNewInDb = 0;
		nbUpdated = 0;
		nbUpdatedInDb = 0;
		nbConflict = 0;
		nbDeleted = 0;
		nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
		while (viewsIterator.hasNext()) {
			IDiagramModel view = viewsIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(view);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					if ( this.exportedModel.getAllConflicts().get(view) == null )
						this.exportedModel.getAllConflicts().put(view, CONFLICT_CHOICE.askUser);
					switch ( this.exportedModel.getAllConflicts().get(view) ) {
						case doNotExport:   // nothing to do
							break;
						case exportToDatabase:
							++nbUpdated;
							break;
						case importFromDatabase:
							++nbUpdatedInDb;
							break;
						case askUser:
						default:    // askUSer
							++nbConflict;
					}
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the views new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getViewsNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded
		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if ( updateTextFields ) {
			this.txtNewViewsInModel.setText(toString(nbNew));
			this.txtNewViewsInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedViewsInModel.setText(toString(nbUpdated));
			this.txtUpdatedViewsInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingViews.setText(toString(nbConflict));
			this.txtDeletedViewsInModel.setText(toString(nbDeleted));
			this.txtDeletedViewsInDatabase.setText(toString(nbDeletedInDb));
		}

		nbNew = 0;
		nbNewInDb = 0;
		nbUpdated = 0;
		nbUpdatedInDb = 0;
		nbConflict = 0;
		nbDeleted = 0;
		nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IDiagramModelObject>> viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
		while (viewObjectsIterator.hasNext()) {
			IDiagramModelObject viewObject = viewObjectsIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(viewObject);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					if ( this.exportedModel.getAllConflicts().get(viewObject) == null )
						this.exportedModel.getAllConflicts().put(viewObject, CONFLICT_CHOICE.askUser);
					switch ( this.exportedModel.getAllConflicts().get(viewObject) ) {
						case doNotExport:   // nothing to do
							break;
						case exportToDatabase:
							++nbUpdated;
							break;
						case importFromDatabase:
							++nbUpdatedInDb;
							break;
						case askUser:
						default:    // askUSer
							++nbConflict;
					}
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the viewObjects new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getViewObjectsNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded
		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if ( updateTextFields ) {
			this.txtNewViewObjectsInModel.setText(toString(nbNew));
			this.txtNewViewObjectsInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedViewObjectsInModel.setText(toString(nbUpdated));
			this.txtUpdatedViewObjectsInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingViewObjects.setText(toString(nbConflict));
			this.txtDeletedViewObjectsInModel.setText(toString(nbDeleted));
			this.txtDeletedViewObjectsInDatabase.setText(toString(nbDeletedInDb));
		}

		nbNew = 0;
		nbNewInDb = 0;
		nbUpdated = 0;
		nbUpdatedInDb = 0;
		nbConflict = 0;
		nbDeleted = 0;
		nbDeletedInDb = 0;
		Iterator<Map.Entry<String, IDiagramModelConnection>> viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
		while (viewConnectionsIterator.hasNext()) {
			IDiagramModelConnection viewConnection = viewConnectionsIterator.next().getValue();
			DBMetadata metadata = this.exportedModel.getDBMetadata(viewConnection);
			switch ( metadata.getDatabaseStatus() ) {
				case isNewInModel:
					++nbNew;
					break;
				case isUpadtedInDatabase:
					++nbUpdatedInDb;
					break;
				case isUpdatedInModel:
					++nbUpdated;
					break;
				case isDeletedInDatabase:
					++nbDeletedInDb;
					break;
				case isConflicting:
					if ( this.exportedModel.getAllConflicts().get(viewConnection) == null )
						this.exportedModel.getAllConflicts().put(viewConnection, CONFLICT_CHOICE.askUser);
					switch ( this.exportedModel.getAllConflicts().get(viewConnection) ) {
						case doNotExport:   // nothing to do
							break;
						case exportToDatabase:
							++nbUpdated;
							break;
						case importFromDatabase:
							++nbUpdatedInDb;
							break;
						case askUser:
						default:    // askUSer
							++nbConflict;
					}
					break;
				case isSynced:
					// nothing to do
					break;
				case isNewInDatabase:
				default:
					// should never be here
			}
		}
		// we distinguish the ViewConnections new in the database from those deleted from memory
		for ( DBMetadata metadata: this.exportConnection.getViewConnectionsNotInModel().values() ) {
			if ( metadata.getInitialVersion().getVersion() != 0 )
				++nbDeleted;        // if the component did exist in the InitialVersion of the model, then it has been deleted from the model
			else
				++nbNewInDb;        // else, the component has been created in the database since the model has been loaded
		}
		total += nbNew + nbNewInDb + nbUpdated + nbUpdatedInDb + nbDeleted + nbDeletedInDb + nbConflict;
		if ( updateTextFields ) {
			this.txtNewViewConnectionsInModel.setText(toString(nbNew));
			this.txtNewViewConnectionsInDatabase.setText(toString(nbNewInDb));
			this.txtUpdatedViewConnectionsInModel.setText(toString(nbUpdated));
			this.txtUpdatedViewConnectionsInDatabase.setText(toString(nbUpdatedInDb));
			this.txtConflictingViewConnections.setText(toString(nbConflict));
			this.txtDeletedViewConnectionsInModel.setText(toString(nbDeleted));
			this.txtDeletedViewConnectionsInDatabase.setText(toString(nbDeletedInDb));
		}

		if ( updateTextFields ) {
			this.txtNewImagesInModel.setText(toString(this.exportConnection.getImagesNotInDatabase().size()));
			this.txtNewImagesInDatabase.setText(toString(this.exportConnection.getImagesNotInModel().size()));

			// we log the values uniquely if the updateTextFields has been requested, else the values are zero
			logger.info(String.format("                            <------ In model ------>   <----- In database ---->"));
			logger.info(String.format("                     Total      New  Updated  Deleted      New  Updated  Deleted Conflict"));                 
			logger.info(String.format("   Model itself:     %6d   %6d   %6d                     %6d            %6d",1, toInt(this.txtNewModelItselfInModel.getText()), toInt(this.txtUpdatedModelItselfInModel.getText()),0,0));
			logger.info(String.format("   Specializations:  %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllProfiles().size(), toInt(this.txtNewProfilesInModel.getText()), toInt(this.txtUpdatedProfilesInModel.getText()), toInt(this.txtDeletedProfilesInModel.getText()), toInt(this.txtNewProfilesInDatabase.getText()), toInt(this.txtUpdatedProfilesInDatabase.getText()), toInt(this.txtDeletedProfilesInDatabase.getText()), toInt(this.txtConflictingProfiles.getText())) );  
			logger.info(String.format("   Elements:         %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllElements().size(), toInt(this.txtNewElementsInModel.getText()), toInt(this.txtUpdatedElementsInModel.getText()), toInt(this.txtDeletedElementsInModel.getText()), toInt(this.txtNewElementsInDatabase.getText()), toInt(this.txtUpdatedElementsInDatabase.getText()), toInt(this.txtDeletedElementsInDatabase.getText()), toInt(this.txtConflictingElements.getText())) );  
			logger.info(String.format("   Relationships:    %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllRelationships().size(), toInt(this.txtNewRelationshipsInModel.getText()), toInt(this.txtUpdatedRelationshipsInModel.getText()), toInt(this.txtDeletedRelationshipsInModel.getText()), toInt(this.txtNewRelationshipsInDatabase.getText()), toInt(this.txtUpdatedRelationshipsInDatabase.getText()), toInt(this.txtDeletedRelationshipsInDatabase.getText()), toInt(this.txtConflictingRelationships.getText())) );
			if ( !DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
				logger.info(String.format("   Folders:          %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllFolders().size(), toInt(this.txtNewFoldersInModel.getText()), toInt(this.txtUpdatedFoldersInModel.getText()), toInt(this.txtDeletedFoldersInModel.getText()), toInt(this.txtNewFoldersInDatabase.getText()), toInt(this.txtUpdatedFoldersInDatabase.getText()), toInt(this.txtDeletedFoldersInDatabase.getText()), toInt(this.txtConflictingFolders.getText())) );
				logger.info(String.format("   views:            %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllViews().size(), toInt(this.txtNewViewsInModel.getText()), toInt(this.txtUpdatedViewsInModel.getText()), toInt(this.txtDeletedViewsInModel.getText()), toInt(this.txtNewViewsInDatabase.getText()), toInt(this.txtUpdatedViewsInDatabase.getText()), toInt(this.txtDeletedViewsInDatabase.getText()), toInt(this.txtConflictingViews.getText())) );
				logger.info(String.format("   Objects:          %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllViewObjects().size(), toInt(this.txtNewViewObjectsInModel.getText()), toInt(this.txtUpdatedViewObjectsInModel.getText()), toInt(this.txtDeletedViewObjectsInModel.getText()), toInt(this.txtNewViewObjectsInDatabase.getText()), toInt(this.txtUpdatedViewObjectsInDatabase.getText()), toInt(this.txtDeletedViewObjectsInDatabase.getText()), toInt(this.txtConflictingViewObjects.getText())) );
				logger.info(String.format("   Connections:      %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllViewConnections().size(), toInt(this.txtNewViewConnectionsInModel.getText()), toInt(this.txtUpdatedViewConnectionsInModel.getText()), toInt(this.txtDeletedViewConnectionsInModel.getText()), toInt(this.txtNewViewConnectionsInDatabase.getText()), toInt(this.txtUpdatedViewConnectionsInDatabase.getText()), toInt(this.txtDeletedViewConnectionsInDatabase.getText()), toInt(this.txtConflictingViewConnections.getText())) );
				logger.info(String.format("   images:           %6d   %6d   %16s  %6d", ((IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class)).getLoadedImagePaths().size(), toInt(this.txtNewImagesInModel.getText()), "", toInt(this.txtNewImagesInDatabase.getText())) );
			}
		}
		
		
		closeMessage();

		if ( total == 0 ) { 
			logger.info("The model does not need to be exported to the database.");
			return true;
		}

		logger.info("The model needs to be exported to the database.");

		return false;
	}

	/**
	 * Loop on model components and call doExportEObject to export them<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button
	 * @return 1 if the model has been successfully exported, 0 if the model does not need to be exported, -1 in case of error
	 */
	public int export() {
		logger.info("Exporting model: ");

		// we disable the export button to avoid a second click
		this.btnDoAction.setEnabled(false);
		this.btnCompareModelToDatabase.setEnabled(false);

		// We show up a small arrow in front of the second action "export components"
		setActiveAction(STATUS.Ok);
		setActiveAction(ACTION.Two);
		disableOption();

		// then we disable the name, purpose and release note text fields
		this.txtModelName.setEnabled(false);
		this.txtPurpose.setEnabled(false);
		this.txtReleaseNote.setEnabled(false);

		// we force the modelVersion and component groups to be visible (in case we come from the conflict resolution)
		this.grpComponents.setVisible(true);
		this.grpModelVersions.setVisible(true);

		// commands that can be undone in case the export fails or is cancelled by the user
		DBCompoundCommand undoableCommands = new DBCompoundCommand("Sync model with database");

		boolean isNeo4JDatabase = DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j");

		String errorMessage = "Exporting model to the database";		// the error message that will be printed in case an exception is raised.
		try {
			//////////////////////////// PREPARATION PHASE : we calculate the model's checksum
			logger.info("Calculating model's checksum.");
			errorMessage = "Failed to calculate the model's checksum.";

			if ( !DBPlugin.areEqual(this.exportedModel.getName(), this.txtModelName.getText()) )
				this.exportedModel.setName(this.txtModelName.getText());

			if ( !DBPlugin.areEqual(this.exportedModel.getPurpose(), this.txtPurpose.getText()) )
				this.exportedModel.setPurpose(this.txtPurpose.getText());

			this.exportedModel.getCurrentVersion().setChecksum(DBChecksum.calculateChecksum(this.exportedModel, this.txtReleaseNote.getText()));

			// we reset the counters as they will be updated during the import and export process
			this.txtTotalModelItself.setText(this.ZERO);      this.txtNewModelItselfInModel.setText(this.ZERO);      this.txtUpdatedModelItselfInModel.setText(this.ZERO);                                                                                                                            /* this.txtUpdatedModelItselfInDatabase.setText(this.ZERO);                                                                       this.txtConflictingModelItself.setText(this.ZERO); */
			this.txtTotalProfiles.setText(this.ZERO);         this.txtNewProfilesInModel.setText(this.ZERO);         this.txtUpdatedProfilesInModel.setText(this.ZERO);         this.txtDeletedProfilesInModel.setText(this.ZERO);         this.txtNewProfilesInDatabase.setText(this.ZERO);          this.txtUpdatedProfilesInDatabase.setText(this.ZERO);          this.txtDeletedProfilesInDatabase.setText(this.ZERO);        this.txtConflictingProfiles.setText(this.ZERO);
			this.txtTotalElements.setText(this.ZERO);         this.txtNewElementsInModel.setText(this.ZERO);         this.txtUpdatedElementsInModel.setText(this.ZERO);         this.txtDeletedElementsInModel.setText(this.ZERO);         this.txtNewElementsInDatabase.setText(this.ZERO);          this.txtUpdatedElementsInDatabase.setText(this.ZERO);          this.txtDeletedElementsInDatabase.setText(this.ZERO);        this.txtConflictingElements.setText(this.ZERO);
			this.txtTotalRelationships.setText(this.ZERO);    this.txtNewRelationshipsInModel.setText(this.ZERO);    this.txtUpdatedRelationshipsInModel.setText(this.ZERO);    this.txtDeletedRelationshipsInModel.setText(this.ZERO);    this.txtNewRelationshipsInDatabase.setText(this.ZERO);     this.txtUpdatedRelationshipsInDatabase.setText(this.ZERO);     this.txtDeletedRelationshipsInDatabase.setText(this.ZERO);   this.txtConflictingRelationships.setText(this.ZERO);
			this.txtTotalFolders.setText(this.ZERO);          this.txtNewFoldersInModel.setText(this.ZERO);          this.txtUpdatedFoldersInModel.setText(this.ZERO);          this.txtDeletedFoldersInModel.setText(this.ZERO);          this.txtNewFoldersInDatabase.setText(this.ZERO);           this.txtUpdatedFoldersInDatabase.setText(this.ZERO);           this.txtDeletedFoldersInDatabase.setText(this.ZERO);         this.txtConflictingFolders.setText(this.ZERO);
			this.txtTotalViews.setText(this.ZERO);            this.txtNewViewsInModel.setText(this.ZERO);            this.txtUpdatedViewsInModel.setText(this.ZERO);            this.txtDeletedViewsInModel.setText(this.ZERO);            this.txtNewViewsInDatabase.setText(this.ZERO);             this.txtUpdatedViewsInDatabase.setText(this.ZERO);             this.txtDeletedViewsInDatabase.setText(this.ZERO);           this.txtConflictingViews.setText(this.ZERO);
			this.txtTotalViewObjects.setText(this.ZERO);      this.txtNewViewObjectsInModel.setText(this.ZERO);      this.txtUpdatedViewObjectsInModel.setText(this.ZERO);      this.txtDeletedViewObjectsInModel.setText(this.ZERO);      this.txtNewViewObjectsInDatabase.setText(this.ZERO);       this.txtUpdatedViewObjectsInDatabase.setText(this.ZERO);       this.txtDeletedViewObjectsInDatabase.setText(this.ZERO);     this.txtConflictingViewObjects.setText(this.ZERO);
			this.txtTotalViewConnections.setText(this.ZERO);  this.txtNewViewConnectionsInModel.setText(this.ZERO);  this.txtUpdatedViewConnectionsInModel.setText(this.ZERO);  this.txtDeletedViewConnectionsInModel.setText(this.ZERO);  this.txtNewViewConnectionsInDatabase.setText(this.ZERO);   this.txtUpdatedViewConnectionsInDatabase.setText(this.ZERO);   this.txtDeletedViewConnectionsInDatabase.setText(this.ZERO); this.txtConflictingViewConnections.setText(this.ZERO);
			this.txtTotalImages.setText(this.ZERO);           this.txtNewImagesInModel.setText(this.ZERO);           this.txtNewImagesInDatabase.setText(this.ZERO);


			//////////////////////////// PHASE 1 : we compare the model to the database

			if ( !isNeo4JDatabase ) {
				if ( compareModelToDatabase(false) ) {
					setActiveAction(STATUS.Ok);
					doShowResult(STATUS.Ok, "Nothing has been exported as the database is already up to date.");
					return 0;
				}
			}

			//////////////////////////// PHASE 2 : we detect the conflicts and ask the user to resolve them

			if ( !isNeo4JDatabase ) {
				setMessage("Checking for conflicts ...");
				errorMessage = "Can't show up the list of conflicting components.";

				hideGrpDatabase();
				createGrpConflict();
				
				Iterator<Entry<String, IProfile>> profilesIterator = this.exportedModel.getAllProfiles().entrySet().iterator();
				while ( profilesIterator.hasNext() ) {
					IProfile profile = profilesIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(profile).getDatabaseStatus() == DATABASE_STATUS.isConflicting ) {
						if ( this.exportedModel.getAllConflicts().get(profile) != null ) {
							this.exportedModel.getAllConflicts().put(profile, CONFLICT_CHOICE.askUser);
							TableItem item = new TableItem(this.tblListConflicts, SWT.NONE);
							item.setText(profile.getId());
							item.setData(profile);
						}
					}
				}

				Iterator<Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
				while ( elementsIterator.hasNext() ) {
					IArchimateElement element = elementsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(element).getDatabaseStatus() == DATABASE_STATUS.isConflicting ) {
						if ( this.exportedModel.getAllConflicts().get(element) != null ) {
							this.exportedModel.getAllConflicts().put(element, CONFLICT_CHOICE.askUser);
							TableItem item = new TableItem(this.tblListConflicts, SWT.NONE);
							item.setText(element.getId());
							item.setData(element);
						}
					}
				}

				Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
				while ( relationshipsIterator.hasNext() ) {
					IArchimateRelationship relationship = relationshipsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(relationship).getDatabaseStatus() == DATABASE_STATUS.isConflicting ) {
						if ( this.exportedModel.getAllConflicts().get(relationship) != null ) {
							this.exportedModel.getAllConflicts().put(relationship, CONFLICT_CHOICE.askUser);
							TableItem item = new TableItem(this.tblListConflicts, SWT.NONE);
							item.setText(relationship.getId());
							item.setData(relationship);
						}
					}
				}

				Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(view).getDatabaseStatus() == DATABASE_STATUS.isConflicting ) {
						if ( this.exportedModel.getAllConflicts().get(view) != null ) {
							this.exportedModel.getAllConflicts().put(view, CONFLICT_CHOICE.askUser);
							TableItem item = new TableItem(this.tblListConflicts, SWT.NONE);
							item.setText(view.getId());
							item.setData(view);
						}
					}
				}

				Iterator<Entry<String, IDiagramModelObject>> viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
				while ( viewObjectsIterator.hasNext() ) {
					IDiagramModelObject viewObject = viewObjectsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(viewObject).getDatabaseStatus() == DATABASE_STATUS.isConflicting ) {
						if ( this.exportedModel.getAllConflicts().get(viewObject) != null ) {
							this.exportedModel.getAllConflicts().put(viewObject, CONFLICT_CHOICE.askUser);
							TableItem item = new TableItem(this.tblListConflicts, SWT.NONE);
							item.setText(viewObject.getId());
							item.setData(viewObject);
						}
					}
				}

				Iterator<Entry<String, IDiagramModelConnection>> viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
				while ( viewConnectionsIterator.hasNext() ) {
					IDiagramModelConnection viewConnection = viewConnectionsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(viewConnection).getDatabaseStatus() == DATABASE_STATUS.isConflicting ) {
						if ( this.exportedModel.getAllConflicts().get(viewConnection) != null ) {
							this.exportedModel.getAllConflicts().put(viewConnection, CONFLICT_CHOICE.askUser);
							TableItem item = new TableItem(this.tblListConflicts, SWT.NONE);
							item.setText(viewConnection.getId());
							item.setData(viewConnection);
						}
					}
				}

				// If there are some conflicts to resolve, then we show the grpConflict group
				//TODO: ameliorate the conflict resolution by keeping all the conflicts in the tblListConflict but add a column with the resolution choosen by the user
				//TODO: in that case, the export could be effectively done when all the tableItems have got a solution distinct from askUser
				if ( this.tblListConflicts.getItemCount() > 0 ) {
					this.tblListConflicts.setSelection(0);

					this.tblListConflicts.notifyListeners(SWT.Selection, new Event());      // shows up the tblListConflicts table and calls fillInCompareTable()

					// when the conflicts are resolved, the export() method will be called again
					return 0;
				}
			}

			// if we're here, this means that there is no conflict or that all the conflicts have been resolved

			//////////////////////////// PHASE 3 : we remove from the model the components that have been deleted in the database 
			if ( !isNeo4JDatabase ) {
				setMessage("Removing from the model the components that have been deleted in the database ...");
				errorMessage = "Failed to remove from the model the components that have been deleted in the database.";

				// please be aware that the commands put in the undoableCommand are single operations
				// ie. when an element is deleted, the command does not delete at the same time the relationships connected to it, not the views objets that references it.

				// we do not use getException() method as Archi commands do not implement it
				
				Iterator<Entry<String, IProfile>> profilesIterator = this.exportedModel.getAllProfiles().entrySet().iterator();
				while ( profilesIterator.hasNext() ) {
					IProfile profile = profilesIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(profile).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.add(new DBDeleteProfileCommand(this.exportedModel, profile));
						incrementText(this.txtDeletedProfilesInDatabase);
						decrementText(this.txtTotalProfiles);
					}
				}

				Iterator<Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
				while ( elementsIterator.hasNext() ) {
					IArchimateElement element = elementsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(element).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.add(new DeleteArchimateElementCommand(element));
						incrementText(this.txtDeletedElementsInDatabase);
						decrementText(this.txtTotalElements);
					}
				}

				Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
				while ( relationshipsIterator.hasNext() ) {
					IArchimateRelationship relationship = relationshipsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(relationship).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.add(new DeleteArchimateRelationshipCommand(relationship));
						incrementText(this.txtDeletedRelationshipsInDatabase);
						decrementText(this.txtTotalRelationships);
					}
				}

				Iterator<Entry<String, IFolder>> foldersIterator = this.exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) {
					IFolder folder = foldersIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(folder).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.add(new DeleteFolderCommand(folder));
						incrementText(this.txtDeletedFoldersInDatabase);
						decrementText(this.txtTotalFolders);
					}
				}

				Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					IDiagramModel view = viewsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(view).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.add(new DeleteDiagramModelCommand(view));
						incrementText(this.txtDeletedViewsInDatabase);
						decrementText(this.txtTotalViews);
					}
				}

				Iterator<Entry<String, IDiagramModelObject>> viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
				while ( viewObjectsIterator.hasNext() ) {
					IDiagramModelObject viewObject = viewObjectsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(viewObject).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.checkAndAdd(new DBDeleteDiagramObjectCommand(this.exportedModel, viewObject));
						incrementText(this.txtDeletedViewObjectsInDatabase);
						decrementText(this.txtTotalViewObjects);
					}
				}

				Iterator<Entry<String, IDiagramModelConnection>> viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
				while ( viewConnectionsIterator.hasNext() ) {
					IDiagramModelConnection viewConnection = viewConnectionsIterator.next().getValue();
					if ( this.exportedModel.getDBMetadata(viewConnection).getDatabaseStatus() == DATABASE_STATUS.isDeletedInDatabase ) {
						undoableCommands.checkAndAdd(new DBDeleteDiagramConnectionCommand(this.exportedModel, viewConnection));
						incrementText(this.txtDeletedViewConnectionsInDatabase);
						decrementText(this.txtTotalViewConnections);
					}
				}

				if ( !undoableCommands.isEmpty() )
					undoableCommands.execute();
			}

			//////////////////////////// PHASE 4 : we import new and updated components from the database
			if ( !isNeo4JDatabase ) {
				// the commands are run in real time, but they are also added in the undoableCommands compound command because we want to be able to undo them
				// they all implement the a getException() method that allow to check if an exception has been raised during the import

				// we count the number of new components to import from the database 
				int progressBarWidth = this.exportConnection.getFoldersNotInModel().size() + this.exportConnection.getElementsNotInModel().size() + this.exportConnection.getRelationshipsNotInModel().size() + this.exportConnection.getViewsNotInModel().size() + this.exportConnection.getViewObjectsNotInModel().size() + this.exportConnection.getViewConnectionsNotInModel().size();
				
				// we add the number of updated components to import from the database
				Iterator<Entry<String, IProfile>> profilesIterator = this.exportedModel.getAllProfiles().entrySet().iterator();
				while ( profilesIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(profilesIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				Iterator<Entry<String, IFolder>> foldersIterator = this.exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(foldersIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				Iterator<Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
				while ( elementsIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(elementsIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
				while ( relationshipsIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(relationshipsIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(viewsIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				Iterator<Entry<String, IDiagramModelObject>> viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
				while ( viewObjectsIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(viewObjectsIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				Iterator<Entry<String, IDiagramModelConnection>> viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
				while ( viewConnectionsIterator.hasNext() ) if ( this.exportedModel.getDBMetadata(viewConnectionsIterator.next().getValue()).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) ++progressBarWidth;
				
				// we count the number of conflict that have been resolved by an import from the database
				Iterator<Entry<EObject, CONFLICT_CHOICE>> conflictsIterator = this.exportedModel.getAllConflicts().entrySet().iterator();
				while ( conflictsIterator.hasNext() ) {
					CONFLICT_CHOICE choice = conflictsIterator.next().getValue();
					if ( choice == CONFLICT_CHOICE.importFromDatabase )
						++progressBarWidth;
				}

				if ( progressBarWidth == 0 )
					logger.info("There is no component to import from the database.");
				else {
					createProgressBar("Importing components from the database ...", 0, progressBarWidth);
					errorMessage = "Failed to import components from the database.";

					try ( DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(this.exportConnection) ) {
						// IMPORT PROFILES (we import the profiles BEFORE the elements, relationships and views because they must exist when the elements, relationships are imported)
						if ( this.exportConnection.getProfilesNotInModel().size() == 0 )
							logger.info("There is no specialization to import.");
						else {
							logger.info("Importing new specializations ...");
							setProgressBarLabel("Importing new specializations ...");
							for (String id : this.exportConnection.getProfilesNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getProfilesNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The specialization id "+id+" has been created in the database. We import it from the database.");
									undoableCommands.checkAndExecute(new DBImportProfileFromIdCommand(importConnection, this.exportedModel, id, versionToImport.getLatestDatabaseVersion().getVersion(), DBImportMode.forceSharedMode));
									incrementText(this.txtNewProfilesInDatabase);
									incrementText(this.txtTotalProfiles);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The specialization id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedProfilesInModel);
								}
							}
						}
						
						// UPDATE PROFILES
						profilesIterator = this.exportedModel.getAllProfiles().entrySet().iterator();
						while ( profilesIterator.hasNext() ) {
							IProfile profile = profilesIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(profile).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The specialization id "+profile.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportProfileFromIdCommand(importConnection, this.exportedModel, profile.getId(), 0, DBImportMode.forceSharedMode));
								incrementText(this.txtUpdatedProfilesInDatabase);
							}
						}
						
						// IMPORT FOLDERS (we import the folders BEFORE the elements, relationships and views because they must exist when the elements, relationships and views are imported)
						if ( this.exportConnection.getFoldersNotInModel().size() == 0 )
							logger.info("There is no folder to import.");
						else {
							logger.info("Importing new folders ...");
							setProgressBarLabel("Importing new folders ...");
							for (String id : this.exportConnection.getFoldersNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getFoldersNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The folder id "+id+" has been created in the database. We import it from the database.");
									undoableCommands.checkAndExecute(new DBImportFolderFromIdCommand(importConnection, this.exportedModel, null, null, id, versionToImport.getLatestDatabaseVersion().getVersion(), DBImportMode.forceSharedMode));
									incrementText(this.txtNewFoldersInDatabase);
									incrementText(this.txtTotalFolders);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The folder id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedFoldersInModel);
								}
							}
						}
						
						// UPDATE FOLDERS
						foldersIterator = this.exportedModel.getAllFolders().entrySet().iterator();
						while ( foldersIterator.hasNext() ) {
							IFolder folder = foldersIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(folder).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The folder id "+folder.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportFolderFromIdCommand(importConnection, this.exportedModel, null, null, folder.getId(), 0, DBImportMode.forceSharedMode));
								incrementText(this.txtUpdatedFoldersInDatabase);
							}
						}
						
	
						// IMPORT ELEMENTS
						if ( this.exportConnection.getElementsNotInModel().size() == 0 )
							logger.info("There is no element to import.");
						else {
							logger.info("Importing new elements ...");
							setProgressBarLabel("Importing new elements ...");
							for (String id : this.exportConnection.getElementsNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getElementsNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The element id "+id+" has been created in the database. We import it from the database.");
									undoableCommands.checkAndExecute(new DBImportElementFromIdCommand(importConnection, this.exportedModel, null, null, null, id, versionToImport.getLatestDatabaseVersion().getVersion(), DBImportMode.forceSharedMode, false));
									incrementText(this.txtNewElementsInDatabase);
									incrementText(this.txtTotalElements);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The element id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedElementsInModel);
								}
							}
						}
						
						// UPDATE ELEMENTS
						elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
						while ( elementsIterator.hasNext() ) {
							IArchimateElement element = elementsIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(element).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The element id "+element.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportElementFromIdCommand(importConnection, this.exportedModel, null, null, null, element.getId(), 0, DBImportMode.forceSharedMode, false));
								incrementText(this.txtUpdatedElementsInDatabase);
							}
						}
	
						// IMPORT RELATIONSHIPS
						if ( this.exportConnection.getRelationshipsNotInModel().size() == 0 )
							logger.info("There is no relationship to import.");
						else {
							logger.info("Importing new relationships ...");
							setProgressBarLabel("Importing new relationships ...");
							for (String id : this.exportConnection.getRelationshipsNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getRelationshipsNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The relationship id "+id+" has been created in the database. We import it from the database.");
									undoableCommands.checkAndExecute(new DBImportRelationshipFromIdCommand(importConnection, this.exportedModel, null, null, null, id, versionToImport.getLatestDatabaseVersion().getVersion(), DBImportMode.forceSharedMode));
									incrementText(this.txtNewRelationshipsInDatabase);
									incrementText(this.txtTotalRelationships);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The relationship id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedRelationshipsInModel);
								}
							}
						}
						
						// UPDATE RELATIONSHIPS
						relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
						while ( relationshipsIterator.hasNext() ) {
							IArchimateRelationship relationship = relationshipsIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(relationship).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The relationship id "+relationship.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportRelationshipFromIdCommand(importConnection, this.exportedModel, null, null, null, relationship.getId(), 0, DBImportMode.forceSharedMode));
								incrementText(this.txtUpdatedRelationshipsInDatabase);
							}
						}
	
						// IMPORT VIEWS
						if ( this.exportConnection.getViewsNotInModel().size() == 0 )
							logger.info("There is no view to import.");
						else {
							logger.info("Importing new views ...");
							setProgressBarLabel("Importing new views ...");
							for (String id : this.exportConnection.getViewsNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getViewsNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The view id "+id+" has been created in the database. We import it in the model.");
									undoableCommands.checkAndExecute(new DBImportViewFromIdCommand(importConnection, this.exportedModel, null, null, id, versionToImport.getLatestDatabaseVersion().getVersion(), DBImportMode.forceSharedMode, false));
									incrementText(this.txtNewViewsInDatabase);
									incrementText(this.txtTotalViews);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The view id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedViewsInModel);
								}
							}
						}
						
						// UPDATE VIEWS
						viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
						while ( viewsIterator.hasNext() ) {
							IDiagramModel view = viewsIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(view).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The view id "+view.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportViewFromIdCommand(importConnection, this.exportedModel, null, null, view.getId(), 0, DBImportMode.forceSharedMode, false));
								incrementText(this.txtUpdatedViewsInDatabase);
							}
						}
	
						// IMPORT VIEW OBJECTS
						if ( this.exportConnection.getViewObjectsNotInModel().size() == 0 )
							logger.info("There is no view object to import.");
						else {
							logger.info("Importing new views objects ...");
							setProgressBarLabel("Importing new views objects ...");
							for (String id : this.exportConnection.getViewObjectsNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getViewObjectsNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The view object id "+id+" has been created in the database. We import it in the model.");
									undoableCommands.checkAndExecute(new DBImportViewObjectFromIdCommand(importConnection, this.exportedModel, null, id, versionToImport.getLatestDatabaseVersion().getVersion(), false, DBImportMode.forceSharedMode));
									incrementText(this.txtNewViewObjectsInDatabase);
									incrementText(this.txtTotalViewObjects);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The view object id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedViewObjectsInModel);
								}
							}
						}
						
						// UPDATE VIEW OBJECTS
						viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
						while ( viewObjectsIterator.hasNext() ) {
							IDiagramModelObject viewObject = viewObjectsIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(viewObject).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The view object id "+viewObject.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportViewObjectFromIdCommand(importConnection, this.exportedModel, null, viewObject.getId(), 0, false, DBImportMode.forceSharedMode));
								incrementText(this.txtUpdatedViewObjectsInDatabase);
							}
						}
	
						// IMPORT VIEW CONNECTIONS
						if ( this.exportConnection.getViewConnectionsNotInModel().size() == 0 )
							logger.info("There is no view connection to import.");
						else {
							logger.info("Importing new views connections ...");
							setProgressBarLabel("Importing new views connections ...");
							for (String id : this.exportConnection.getViewConnectionsNotInModel().keySet() ) {
								DBMetadata versionToImport = this.exportConnection.getViewConnectionsNotInModel().get(id);
								if ( versionToImport.getInitialVersion().getVersion() == 0 ) {
									if ( logger.isDebugEnabled() ) logger.debug("The view connection id "+id+" has been created in the database. We import it in the model.");
									undoableCommands.checkAndExecute(new DBImportViewConnectionFromIdCommand(importConnection, this.exportedModel, null, id, versionToImport.getLatestDatabaseVersion().getVersion(), false, DBImportMode.forceSharedMode));
									incrementText(this.txtNewViewConnectionsInDatabase);
									incrementText(this.txtTotalViewConnections);
								} else {
									if ( logger.isDebugEnabled() ) logger.debug("The view connection id "+id+" is not imported as it has been deleted from the model.");
									incrementText(this.txtDeletedViewConnectionsInModel);
								}
							}
						}
						
						// UPDATE VIEW CONNECTIONS
						viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
						while ( viewConnectionsIterator.hasNext() ) {
							IDiagramModelConnection viewConnection = viewConnectionsIterator.next().getValue();
							if ( this.exportedModel.getDBMetadata(viewConnection).getDatabaseStatus() == DATABASE_STATUS.isUpadtedInDatabase ) {
								if ( logger.isDebugEnabled() ) logger.debug("The view connection id "+viewConnection.getId()+" has been updated in the database. We import the new version from the database.");
								undoableCommands.checkAndExecute(new DBImportViewConnectionFromIdCommand(importConnection, this.exportedModel, null, viewConnection.getId(), 0, false, DBImportMode.forceSharedMode));
								incrementText(this.txtUpdatedViewConnectionsInDatabase);
							}
						}
						
						conflictsIterator = this.exportedModel.getAllConflicts().entrySet().iterator();
						while ( conflictsIterator.hasNext() ) {
							Entry<EObject, CONFLICT_CHOICE> entry = conflictsIterator.next();
							EObject componentToImport = entry.getKey();
							CONFLICT_CHOICE choice = entry.getValue();
							
							if ( choice == CONFLICT_CHOICE.importFromDatabase ) {
								IDBCommand command = null;
								String id = ((IIdentifier)componentToImport).getId();
								DBMetadata metadata = this.exportedModel.getDBMetadata(componentToImport);
								int latestDatabaseVersion = metadata.getLatestDatabaseVersion().getVersion();
								
								if ( componentToImport instanceof IArchimateElement ) {
									command = new DBImportElementFromIdCommand(importConnection, this.exportedModel, null, null, null, id, latestDatabaseVersion, DBImportMode.forceSharedMode, false);
									incrementText(this.txtUpdatedElementsInDatabase);
								} else if ( componentToImport instanceof IArchimateRelationship ) {
									command = new DBImportRelationshipFromIdCommand(importConnection, this.exportedModel, null, null, null, id, latestDatabaseVersion, DBImportMode.forceSharedMode);
									incrementText(this.txtUpdatedRelationshipsInDatabase);
								} else if ( componentToImport instanceof IDiagramModel) {
									command = new DBImportViewFromIdCommand(importConnection, this.exportedModel, null, null, id, latestDatabaseVersion, DBImportMode.forceSharedMode, false);
									incrementText(this.txtUpdatedViewsInDatabase);
								} else if ( componentToImport instanceof IDiagramModelObject ) {
									command = new DBImportViewObjectFromIdCommand(importConnection, this.exportedModel, null, id, latestDatabaseVersion, false, DBImportMode.forceSharedMode);
									incrementText(this.txtUpdatedViewObjectsInDatabase);
								} else if ( componentToImport instanceof IDiagramModelConnection ) {
									command = new DBImportViewConnectionFromIdCommand(importConnection, this.exportedModel, null, id, latestDatabaseVersion, false, DBImportMode.forceSharedMode);
									incrementText(this.txtUpdatedViewConnectionsInDatabase);
								}
								
								if ( logger.isDebugEnabled() ) logger.debug("The conflicting "+metadata.getDebugName()+" conflicts with the database, but the conflict resolution has been set to "+CONFLICT_CHOICE.importFromDatabase);
								undoableCommands.checkAndExecute(command);
							}
						}
	
						// RESOLVE RELATIONSHIPS
						if ( (this.exportedModel.getAllSourceRelationshipsToResolve().size() != 0) || (this.exportedModel.getAllTargetRelationshipsToResolve().size() != 0) ) {
							setProgressBarLabel("Resolving relationships ...");
							undoableCommands.checkAndExecute(new DBResolveRelationshipsCommand(this.exportedModel));
						}
	
						// RESOLVE CONNECTIONS
						if ( (this.exportedModel.getAllSourceConnectionsToResolve().size() != 0) || (this.exportedModel.getAllTargetConnectionsToResolve().size() != 0) ) {
							setProgressBarLabel("Resolving views connections ...");
							undoableCommands.checkAndExecute(new DBResolveConnectionsCommand(this.exportedModel));
						}
					} finally {
						hideProgressBar();
					}
				}
			}

			//////////////////////////// PHASE 5 : we move components to new folders if they've been moved in the database
			if ( !isNeo4JDatabase ) {
				setMessage("Checking if components have been moved to new folder ...");
				errorMessage = "Failed to move components to a new folder.";

				try ( DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(this.exportConnection) ) {
					DBSetFolderToLastKnownCommand setFolderCommand = new DBSetFolderToLastKnownCommand(this.exportedModel, importConnection);
					if ( setFolderCommand.getException() != null )
						throw setFolderCommand.getException();
					if ( setFolderCommand.needsToBeExecuted() ) {
						logger.info("Moving components to new folders");
						undoableCommands.checkAndExecute(setFolderCommand);
					} else
						logger.info("There is no component to move to a new folder.");
				}
			}

			//////////////////////////// PHASE 6 : we recalculate all the checksums and the "getAll..."  maps as all the containers may have changed because of imported and deleted components
			if ( !isNeo4JDatabase && !undoableCommands.isEmpty() ) {
				setMessage("Recalculating checksums ");
				errorMessage = "Failed to recalculate checksums.";

				// recalculate the checksum. This does not update the versions, so the database status remains.
				this.exportedModel.countAllObjects();
			}
			
			////////////////////////////PHASE 7 : we re-compare the model to the database as the imports may have been sufficient
			if ( !isNeo4JDatabase && !undoableCommands.isEmpty() ) {
				if ( compareModelToDatabase(false) ) {
					setActiveAction(STATUS.Ok);
					doShowResult(STATUS.Ok, "Your model is now in sync with the database.");
					// TODO: loop on getAllXXX to fill in the txtTotalXXXX fields
					return 0;
				}
			}

			//////////////////////////// PHASE 8 : at last, we export a new version of the model to the database
			int progressBarWidth = this.exportedModel.getAllElements().size() + this.exportedModel.getAllRelationships().size();
			if ( !isNeo4JDatabase )
				progressBarWidth += this.exportedModel.getAllFolders().size() + this.exportedModel.getAllViews().size() + this.exportedModel.getAllViewObjects().size() + this.exportedModel.getAllViewConnections().size();

			createProgressBar("Exporting model to the database ...", 0, progressBarWidth);
			errorMessage = "Failed to export the model to the database";

			// we start a new database transaction
			this.exportConnection.setAutoCommit(false);

			if ( !isNeo4JDatabase ) {
				logger.info("Exporting the model itself ...");
				this.txtTotalModelItself.setText("1");
				this.exportConnection.exportModel(this.exportedModel, this.txtReleaseNote.getText());
			} else {
				if ( this.selectedDatabase.shouldEmptyNeo4jDB() ) {
					errorMessage = "Failed to empty the Neo4J database.";
					this.exportConnection.emptyNeo4jDB();
				}
			}
			
			// EXPORT SPECIALIZATIONS (ie profiles)
			int countNew = 0;
			int countUpdated = 0;
			int countNotExported = 0;
			setProgressBarLabel("Exporting specializations ...");
			Iterator<IProfile> profilesIterator = this.exportedModel.getProfiles().iterator();
			while ( profilesIterator.hasNext() ) {
				EObject componentToExport = profilesIterator.next();				
				DATABASE_STATUS dbStatus = isNeo4JDatabase ? DATABASE_STATUS.isNewInModel : this.exportedModel.getDBMetadata(componentToExport).getDatabaseStatus();
				
				if ( dbStatus == DATABASE_STATUS.isNewInModel ) {
					this.exportConnection.exportEObject(componentToExport);
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtNewProfilesInModel);
						incrementText(this.txtTotalProfiles);
						increaseProgressBar();
					}
					++countNew;
				} else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel ) {
					this.exportConnection.exportEObject(componentToExport);
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtUpdatedProfilesInModel);
						incrementText(this.txtTotalProfiles);
						increaseProgressBar();
					}
					++countUpdated;
				} else
					++countNotExported;

				this.exportConnection.assignEObjectToModel(componentToExport);
			}
            if ( !this.showRealTimeNumbers ) {
            	incrementText(this.txtNewProfilesInModel, countNew);
            	incrementText(this.txtUpdatedProfilesInModel, countUpdated);
            	incrementText(this.txtTotalProfiles, countNew+countUpdated+countNotExported);
            	increaseProgressBar(countNew+countUpdated+countNotExported);
            }
			
			// EXPORT ELEMENTS
			countNew = 0;
			countUpdated = 0;
			countNotExported = 0;
			setProgressBarLabel("Exporting elements ...");
			Iterator<Entry<String, IArchimateElement>> elementsIterator = this.exportedModel.getAllElements().entrySet().iterator();
			while ( elementsIterator.hasNext() ) {
				EObject componentToExport = elementsIterator.next().getValue();
				if ( isNeo4JDatabase ) {
					this.exportConnection.exportEObject(componentToExport);
					if ( this.showRealTimeNumbers )
						incrementText(this.txtNewElementsInModel);
					++countNew;
				} else {
					DATABASE_STATUS dbStatus = this.exportedModel.getDBMetadata(componentToExport).getDatabaseStatus();
					if ( dbStatus == DATABASE_STATUS.isNewInModel ) {
						this.exportConnection.exportEObject(componentToExport);
						if ( this.showRealTimeNumbers )
							incrementText(this.txtNewElementsInModel);
						++countNew;
					} else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel ) {
						this.exportConnection.exportEObject(componentToExport);
						if ( this.showRealTimeNumbers )
							incrementText(this.txtUpdatedElementsInModel);
						++countUpdated;
					} else
						++countNotExported;
					
					this.exportConnection.assignEObjectToModel(componentToExport);
				} 
				
				if ( this.showRealTimeNumbers ) {
					incrementText(this.txtTotalElements);
					increaseProgressBar();
				}
			}
            if ( !this.showRealTimeNumbers ) {
            	incrementText(this.txtNewElementsInModel, countNew);
            	incrementText(this.txtUpdatedElementsInModel, countUpdated);
            	incrementText(this.txtTotalElements, countNew+countUpdated+countNotExported);
            	increaseProgressBar(countNew+countUpdated+countNotExported);
            }

			// EXPORT RELATIONSHIPS
			countNew = 0;
			countUpdated = 0;
			countNotExported = 0;
			setProgressBarLabel("Exporting relationships ...");
			Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = this.exportedModel.getAllRelationships().entrySet().iterator();
			while ( relationshipsIterator.hasNext() ) {
				EObject componentToExport = relationshipsIterator.next().getValue();
				if ( isNeo4JDatabase ) {
					this.exportConnection.exportEObject(componentToExport);
					if ( this.showRealTimeNumbers )
						incrementText(this.txtNewRelationshipsInModel);
					++countNew;
				} else {
					DATABASE_STATUS dbStatus = this.exportedModel.getDBMetadata(componentToExport).getDatabaseStatus();
					if ( dbStatus == DATABASE_STATUS.isNewInModel ) {
						this.exportConnection.exportEObject(componentToExport);
						if ( this.showRealTimeNumbers )
							incrementText(this.txtNewRelationshipsInModel);
						++countNew;
					} else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel ) {
						this.exportConnection.exportEObject(componentToExport);
						if ( this.showRealTimeNumbers )
							incrementText(this.txtUpdatedRelationshipsInModel);
						++countUpdated;
					} else
						++countNotExported;
					
					this.exportConnection.assignEObjectToModel(componentToExport);
				}
				if ( this.showRealTimeNumbers ) {
					incrementText(this.txtTotalRelationships);
					increaseProgressBar();
				}
			}
			if ( !this.showRealTimeNumbers ) {
            	incrementText(this.txtNewRelationshipsInModel, countNew);
            	incrementText(this.txtUpdatedRelationshipsInModel, countUpdated);
            	incrementText(this.txtTotalRelationships, countNew+countUpdated+countNotExported);
            	increaseProgressBar(countNew+countUpdated+countNotExported);
            }

			if ( !isNeo4JDatabase ) {
				countNew = 0;
				countUpdated = 0;
				countNotExported = 0;
				setProgressBarLabel("Exporting folders ...");
				Iterator<Entry<String, IFolder>> foldersIterator = this.exportedModel.getAllFolders().entrySet().iterator();
				while ( foldersIterator.hasNext() ) {
					EObject componentToExport = foldersIterator.next().getValue();
					DATABASE_STATUS dbStatus = this.exportedModel.getDBMetadata(componentToExport).getDatabaseStatus();
					if ( dbStatus == DATABASE_STATUS.isNewInModel ) {
						this.exportConnection.exportEObject(componentToExport);
						if ( this.showRealTimeNumbers )
							incrementText(this.txtNewFoldersInModel);
						++countNew;
					} else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel ) {
						this.exportConnection.exportEObject(componentToExport);
						if ( this.showRealTimeNumbers )
							incrementText(this.txtUpdatedFoldersInModel);
						++countUpdated;
					} else
						++countNotExported;
					
					this.exportConnection.assignEObjectToModel(componentToExport);
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtTotalFolders);
						increaseProgressBar();
					}
				}
				if ( !this.showRealTimeNumbers ) {
	            	incrementText(this.txtNewFoldersInModel, countNew);
	            	incrementText(this.txtUpdatedFoldersInModel, countUpdated);
	            	incrementText(this.txtTotalFolders, countNew+countUpdated+countNotExported);
	            	increaseProgressBar(countNew+countUpdated+countNotExported);
	            }

				setProgressBarLabel("Exporting views ...");
				countNew = 0;
				countUpdated = 0;
				countNotExported = 0;
				Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
				while ( viewsIterator.hasNext() ) {
					EObject componentToExport = viewsIterator.next().getValue();
					DBMetadata metadata = this.exportedModel.getDBMetadata(componentToExport);
					DATABASE_STATUS dbStatus = metadata.getDatabaseStatus();
					Text txtFieldToIncrement = null;
					if ( dbStatus == DATABASE_STATUS.isNewInModel )
						txtFieldToIncrement = this.txtNewViewsInModel;
					else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel )
						txtFieldToIncrement = this.txtUpdatedViewsInModel;
					
					if ( txtFieldToIncrement != null ) {
						if ( metadata.getScreenshot().isScreenshotActive() ) {
							if ( this.showRealTimeNumbers )
								setProgressBarLabel("Creating screenshot of view \""+metadata.getName()+"\"");
							createImage((IDiagramModel)componentToExport, this.exportConnection.getDatabaseEntry().getViewsImagesScaleFactor(), this.exportConnection.getDatabaseEntry().getViewsImagesBorderWidth());
							if ( this.showRealTimeNumbers )
								setProgressBarLabel("Exporting views ...");
						}
						this.exportConnection.exportEObject(componentToExport);
						metadata.setExported(true);
						
						if ( this.showRealTimeNumbers ) {
							incrementText(txtFieldToIncrement);
							if ( txtFieldToIncrement == this.txtNewViewsInModel )
								++countNew;
							else
								++countUpdated;
						}
					} else {
						++countNotExported;
						metadata.setExported(false);
					}

					this.exportConnection.assignEObjectToModel(componentToExport);
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtTotalViews);
						increaseProgressBar();
					}
				}
				if ( !this.showRealTimeNumbers ) {
	            	incrementText(this.txtNewViewsInModel, countNew);
	            	incrementText(this.txtUpdatedViewsInModel, countUpdated);
	            	incrementText(this.txtTotalViews, countNew+countUpdated+countNotExported);
	            	increaseProgressBar(countNew+countUpdated+countNotExported);
	            }

				setProgressBarLabel("Exporting view objects ...");
				countNew = 0;
				countUpdated = 0;
				countNotExported = 0;
				Iterator<Entry<String, IDiagramModelObject>> viewObjectsIterator = this.exportedModel.getAllViewObjects().entrySet().iterator();
				while ( viewObjectsIterator.hasNext() ) {
					IDiagramModelObject componentToExport = viewObjectsIterator.next().getValue();

					if ( this.exportedModel.getDBMetadata(componentToExport.getDiagramModel()).isExported() ) {
						DATABASE_STATUS dbStatus = this.exportedModel.getDBMetadata(componentToExport).getDatabaseStatus();
						if ( dbStatus == DATABASE_STATUS.isNewInModel ) {
							this.exportConnection.exportEObject(componentToExport);
							if ( this.showRealTimeNumbers )
								incrementText(this.txtNewViewObjectsInModel);
							++countNew;
						} else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel ) {
							this.exportConnection.exportEObject(componentToExport);
							if ( this.showRealTimeNumbers )
								incrementText(this.txtUpdatedViewObjectsInModel);
							++countUpdated;
						} else
							++countNotExported;
						
						this.exportConnection.assignEObjectToModel(componentToExport);
					}
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtTotalViewObjects);
						increaseProgressBar();
					}
				}
				if ( !this.showRealTimeNumbers ) {
	            	incrementText(this.txtNewViewObjectsInModel, countNew);
	            	incrementText(this.txtUpdatedViewObjectsInModel, countUpdated);
	            	incrementText(this.txtTotalViewObjects, countNew+countUpdated+countNotExported);
	            	increaseProgressBar(countNew+countUpdated+countNotExported);
	            }

				setProgressBarLabel("Exporting view connections ...");
				countNew = 0;
				countUpdated = 0;
				countNotExported = 0;
				Iterator<Entry<String, IDiagramModelConnection>> viewConnectionsIterator = this.exportedModel.getAllViewConnections().entrySet().iterator();
				while ( viewConnectionsIterator.hasNext() ) {
					IDiagramModelConnection componentToExport = viewConnectionsIterator.next().getValue();
					
					if ( this.exportedModel.getDBMetadata(componentToExport.getDiagramModel()).isExported() ) {
						DATABASE_STATUS dbStatus = this.exportedModel.getDBMetadata(componentToExport).getDatabaseStatus();
						if ( dbStatus == DATABASE_STATUS.isNewInModel ) {
							this.exportConnection.exportEObject(componentToExport);
							if ( this.showRealTimeNumbers )
								incrementText(this.txtNewViewConnectionsInModel);
							++countNew;
						} else if ( dbStatus == DATABASE_STATUS.isUpdatedInModel ) {
							this.exportConnection.exportEObject(componentToExport);
							if ( this.showRealTimeNumbers )
								incrementText(this.txtUpdatedViewConnectionsInModel);
							++countUpdated;
						} else
							++countNotExported;
						this.exportConnection.assignEObjectToModel(componentToExport);
					}
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtTotalViewConnections);
						increaseProgressBar();
					}
				}
				if ( !this.showRealTimeNumbers ) {
	            	incrementText(this.txtNewViewConnectionsInModel, countNew);
	            	incrementText(this.txtUpdatedViewConnectionsInModel, countUpdated);
	            	incrementText(this.txtTotalViewConnections, countNew+countUpdated+countNotExported);
	            	increaseProgressBar(countNew+countUpdated+countNotExported);
	            }

				setProgressBarLabel("Exporting images ...");
				countNew = 0;
				countNotExported = 0;
				// no need to use imagesNotInModel as the requested images have been imported at the same time as their view object
				IArchiveManager archiveMgr = (IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class);
				for ( String path: this.exportedModel.getAllImagePaths() ) {
					if ( this.exportConnection.exportImage(path, archiveMgr.getBytesFromEntry(path)) ) {
						if ( this.showRealTimeNumbers )
							incrementText(this.txtNewImagesInModel);
						++countNew;
					} else
						++countNotExported;
					
					if ( this.showRealTimeNumbers ) {
						incrementText(this.txtTotalImages);
						increaseProgressBar();
					}
				}
				if ( !this.showRealTimeNumbers ) {
					incrementText(this.txtNewImagesInModel, countNew);
					incrementText(this.txtTotalImages, countNew+countNotExported);
	            	increaseProgressBar(countNew+countNotExported);
	            }

				// we register the undoableCommands on the model's stack, this way, the user will be able to manually undo them
				this.stack.execute(undoableCommands);
			}
		} catch (Exception exportError) {
			// if the exception is not raised because the user clicked on the cancel button, then we rollback and close the database connection
			if ( !isClosedByUser() ) {
				// TODO: find a better way to manage the cancel button
				setActiveAction(STATUS.Error);

				// if the user clicked on the "cancel" button, then the database connection is closed, which generates an exception when a SQL request is executed
				try {
					rollbackAndCloseConnection();

					doShowResult(STATUS.Error, errorMessage + "\n"+exportError.getMessage());
					DBGuiUtils.popup(Level.ERROR, errorMessage + "\n\nThe transaction has been rolled back to leave the database in a coherent state. You may solve the issue and export again your components.", exportError);
				} catch (SQLException closeDBError) {
					doShowResult(STATUS.Error, "Error while exporting model.\n"+exportError.getMessage()+"\nThe transaction failed to rollback, please check your database carrefully !");

					DBGuiUtils.popup(Level.FATAL, "An error occurred while exporting the components."+exportError);
					DBGuiUtils.popup(Level.FATAL, "An exception has been detected during the rollback and closure of the database transaction.\n\nThe database is left in an unknown state.\n\nPlease check carrefully your database !", closeDBError);
				}
			}

			// we rollback any update done on the model
			if ( !undoableCommands.isEmpty() ) {
				this.stack.undo();
				// this.undoableCommands.undo();
				for ( Object cmd: undoableCommands.getCommands() ) {
					try {
						Method getException = IDBCommand.class.getMethod("getException()");
						Exception e = (Exception) getException.invoke(cmd);
						if ( e != null ) {
							DBGuiUtils.popup(Level.FATAL, "Failed to restore the model as it was before the export. Please verify it carefully.", e);
							// a single message is sufficient to alert the user
							break;
						}
					} catch (@SuppressWarnings("unused") Exception ign) {
						// nothing to do if the command does not have manage exceptions
					}
				}
			}

			return -1;
		}

		// if we're here, it means that no exception has been raised during the export process
		try  {
			commitAndCloseConnection();
			setActiveAction(STATUS.Ok);
			// Once the export is finished, we copy the exportedVersion to the currentVersion for all the model's components
			copyCurrentVersionToInitialVersion();

			doShowResult(STATUS.Ok, "*** Export successful ***");
			return 1;
		} catch (Exception err) {
			setActiveAction(STATUS.Error);
			doShowResult(STATUS.Error, "Failed to commit the database transaction.\n"+err.getMessage()+"\nPlease check your database carrefully.");
			DBGuiUtils.popup(Level.FATAL, "The model has been exported to the database, but an exception has been raised during the database connection commit and closure, thus your dabase may be left in an incoherent state.\n\nPlease check carrefully your database !", err);
			return -1;
		}
	}
	
	//private void doExport(EObject objToExport, Text txtFieldToIncrement) throws Exception {
	//	this.exportConnection.exportEObject(objToExport);
	//	if ( txtFieldToIncrement != null )
	//		incrementText(txtFieldToIncrement);
	//}

	void copyCurrentVersionToInitialVersion() {
		if ( logger.isDebugEnabled() ) logger.debug("Copying current version to initial version.");

		this.exportedModel.getInitialVersion().set(this.exportedModel.getCurrentVersion());

		Iterator<Map.Entry<String, IArchimateElement>> ite = this.exportedModel.getAllElements().entrySet().iterator();
		while (ite.hasNext()) {
			DBMetadata dbMetadata = this.exportedModel.getDBMetadata(ite.next().getValue());
			dbMetadata.getInitialVersion().set(dbMetadata.getCurrentVersion());
		}

		Iterator<Map.Entry<String, IArchimateRelationship>> itr = this.exportedModel.getAllRelationships().entrySet().iterator();
		while (itr.hasNext()) {
			DBMetadata dbMetadata = this.exportedModel.getDBMetadata(itr.next().getValue());
			dbMetadata.getInitialVersion().set(dbMetadata.getCurrentVersion());
		}

		Iterator<Map.Entry<String, IFolder>> itf = this.exportedModel.getAllFolders().entrySet().iterator();
		while (itf.hasNext()) {
			DBMetadata dbMetadata = this.exportedModel.getDBMetadata(itf.next().getValue());
			dbMetadata.getInitialVersion().set(dbMetadata.getCurrentVersion());
		}

		Iterator<Map.Entry<String, IDiagramModel>> itv = this.exportedModel.getAllViews().entrySet().iterator();
		while (itv.hasNext()) {
			DBMetadata dbMetadata = this.exportedModel.getDBMetadata(itv.next().getValue());
			dbMetadata.getInitialVersion().set(dbMetadata.getCurrentVersion());
		}

		Iterator<Map.Entry<String, IDiagramModelObject>> ito = this.exportedModel.getAllViewObjects().entrySet().iterator();
		while (ito.hasNext()) {
			DBMetadata dbMetadata = this.exportedModel.getDBMetadata(ito.next().getValue());
			dbMetadata.getInitialVersion().set(dbMetadata.getCurrentVersion());
		}

		Iterator<Map.Entry<String, IDiagramModelConnection>> itc = this.exportedModel.getAllViewConnections().entrySet().iterator();
		while (itc.hasNext()) {
			DBMetadata dbMetadata = this.exportedModel.getDBMetadata(itc.next().getValue());
			dbMetadata.getInitialVersion().set(dbMetadata.getCurrentVersion());
		}
	}

	/**
	 * Creates a group that will display the conflicts raised during the export process
	 */
	protected void createGrpConflict() {		
		if ( this.grpConflict == null ) {
			this.grpConflict = new Group(this.compoRightBottom, SWT.NONE);
			this.grpConflict.setBackground(GROUP_BACKGROUND_COLOR);
			this.grpConflict.setFont(TITLE_FONT);
			this.grpConflict.setText("Conflict: ");
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			fd.bottom = new FormAttachment(100);
			this.grpConflict.setLayoutData(fd);
			this.grpConflict.setLayout(new FormLayout());

			this.lblCantExport = new Label(this.grpConflict, SWT.NONE);
			this.lblCantExport.setBackground(GROUP_BACKGROUND_COLOR);
			this.lblCantExport.setText("Can't export because some components conflict with newer version in the database:");
			fd = new FormData();
			fd.top = new FormAttachment(0, getDefaultMargin());
			fd.left = new FormAttachment(0, getDefaultMargin());
			this.lblCantExport.setLayoutData(fd);

			this.tblListConflicts = new Table(this.grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
			this.tblListConflicts.setLinesVisible(true);
			this.tblListConflicts.setBackground(TABLE_BACKGROUND_COLOR);
			this.tblListConflicts.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					// we search for the component that is conflicting
					String id = DBGuiExportModel.this.tblListConflicts.getSelection()[0].getText();

					EObject conflictingComponent = DBGuiExportModel.this.exportedModel.searchComponentFromId(id);

					if ( conflictingComponent == null ) {
						DBGuiExportModel.this.btnExportMyVersion.setEnabled(false);
						DBGuiExportModel.this.btnDoNotExport.setEnabled(false);
						DBGuiExportModel.this.btnImportDatabaseVersion.setEnabled(false);
						DBGuiExportModel.this.tblCompareComponent.removeAll();
						DBGuiUtils.popup(Level.ERROR, "Do not know which component is conflicting !!! That's weird !!!");
					} else {				
						DBGuiExportModel.this.btnExportMyVersion.setEnabled(true);
						DBGuiExportModel.this.btnDoNotExport.setEnabled(true);
						DBGuiExportModel.this.btnImportDatabaseVersion.setEnabled( (conflictingComponent instanceof IArchimateElement) || (conflictingComponent instanceof IArchimateRelationship) );

						fillInCompareTable(DBGuiExportModel.this.tblCompareComponent, conflictingComponent, DBGuiExportModel.this.exportedModel.getDBMetadata(conflictingComponent).getLatestDatabaseVersion().getVersion());
					}
					DBGuiExportModel.this.grpComponents.setVisible(false);
					DBGuiExportModel.this.grpModelVersions.setVisible(false);
					DBGuiExportModel.this.grpConflict.setVisible(true);
					DBGuiExportModel.this.compoRightBottom.layout();
				}
			});
			fd = new FormData();
			fd.top = new FormAttachment(this.lblCantExport, getDefaultMargin());
			fd.left = new FormAttachment(25);
			fd.right = new FormAttachment(75);
			fd.bottom = new FormAttachment(40);
			this.tblListConflicts.setLayoutData(fd);

			Label lblCompare = new Label(this.grpConflict, SWT.NONE);
			lblCompare.setBackground(GROUP_BACKGROUND_COLOR);
			lblCompare.setText("Please verify your version against the latest version in the database:");
			fd = new FormData();
			fd.top = new FormAttachment(this.tblListConflicts, 20);
			fd.left = new FormAttachment(0, getDefaultMargin());
			lblCompare.setLayoutData(fd);

			this.tblCompareComponent = new Tree(this.grpConflict, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.V_SCROLL);
			this.tblCompareComponent.setBackground(TABLE_BACKGROUND_COLOR);
			this.tblCompareComponent.setHeaderVisible(true);
			this.tblCompareComponent.setLinesVisible(true);
			fd = new FormData();
			fd.top = new FormAttachment(lblCompare, getDefaultMargin());
			fd.left = new FormAttachment(0, getDefaultMargin());
			fd.right = new FormAttachment(100, -getDefaultMargin());
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
			this.btnImportDatabaseVersion.setText("Import database version");
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
			fd = new FormData();
			fd.right = new FormAttachment(100, -getDefaultMargin());
			fd.bottom = new FormAttachment(100, -getDefaultMargin());
			this.btnImportDatabaseVersion.setLayoutData(fd);

			this.btnExportMyVersion = new Button(this.grpConflict, SWT.NONE);
			this.btnExportMyVersion.setImage(EXPORT_TO_DATABASE_IMAGE);
			this.btnExportMyVersion.setText("Export model's version to database");
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
			fd = new FormData();
			fd.right = new FormAttachment(this.btnImportDatabaseVersion, -getDefaultMargin());
			fd.bottom = new FormAttachment(100, -getDefaultMargin());
			this.btnExportMyVersion.setLayoutData(fd);

			this.btnDoNotExport = new Button(this.grpConflict, SWT.NONE);
			this.btnDoNotExport.setText("Do nothing");
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
			fd.right = new FormAttachment(this.btnExportMyVersion, -getDefaultMargin());
			fd.bottom = new FormAttachment(100, -getDefaultMargin());
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
	 * called when the user click on one of the btnExportMyVersion, btnImportDatabaseVersion, or btnDoNotExport button<br>
	 * Sets the exportChoice and removes the component from the tblListconflicts table<br>
	 * If no conflict remain, then it relaunch the doExportComponents method 
	 * @param requiredChoice 
	 */
	protected void tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE requiredChoice) {
		CONFLICT_CHOICE effectiveChoice = requiredChoice;
		
		EObject component = (EObject)this.tblListConflicts.getSelection()[0].getData();
		if ( component == null ) {
			DBGuiUtils.popup(Level.ERROR, "Can't get conflicting component \""+this.tblListConflicts.getSelection()[0].getText()+"\"");
			return;
		}

		this.exportedModel.getAllConflicts().put(component, effectiveChoice);
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
				this.lblCantExport.setText("Can't export because "+this.tblListConflicts.getItemCount()+" component conflicts with newer version in the database:");
			else
				this.lblCantExport.setText("Can't export because "+this.tblListConflicts.getItemCount()+" components conflict with newer version in the database:");

			if ( index < this.tblListConflicts.getItemCount() )
				this.tblListConflicts.setSelection(index);
			else
				this.tblListConflicts.setSelection(index-1);
			this.tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
		}
	}

	protected void doShowResult(STATUS status, String message) {
		logger.debug("Showing result.");
		hideProgressBar();
		if ( this.grpConflict != null ) this.grpConflict.setVisible(false);
		this.grpComponents.setVisible(true);
		this.grpModelVersions.setVisible(true);

		setActiveAction(ACTION.Three);

		if ( status == STATUS.Ok ) {
			logger.info(String.format("                            <------ In model ------>   <----- In database ---->"));
			logger.info(String.format("                    Total      New  Updated  Deleted      New  Updated  Deleted Conflict"));                 
			logger.info(String.format("   Elements:       %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllElements().size(), toInt(this.txtNewElementsInModel.getText()), toInt(this.txtUpdatedElementsInModel.getText()), toInt(this.txtDeletedElementsInModel.getText()), toInt(this.txtNewElementsInDatabase.getText()), toInt(this.txtUpdatedElementsInDatabase.getText()), toInt(this.txtDeletedElementsInDatabase.getText()), toInt(this.txtConflictingElements.getText())) );  
			logger.info(String.format("   Relationships:  %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllRelationships().size(), toInt(this.txtNewRelationshipsInModel.getText()), toInt(this.txtUpdatedRelationshipsInModel.getText()), toInt(this.txtDeletedRelationshipsInModel.getText()), toInt(this.txtNewRelationshipsInDatabase.getText()), toInt(this.txtUpdatedRelationshipsInDatabase.getText()), toInt(this.txtDeletedRelationshipsInDatabase.getText()), toInt(this.txtConflictingRelationships.getText())) );
			if ( !DBPlugin.areEqual(this.selectedDatabase.getDriver().toLowerCase(), "neo4j") ) {
				logger.info(String.format("   Folders:        %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllFolders().size(), toInt(this.txtNewFoldersInModel.getText()), toInt(this.txtUpdatedFoldersInModel.getText()), toInt(this.txtDeletedFoldersInModel.getText()), toInt(this.txtNewFoldersInDatabase.getText()), toInt(this.txtUpdatedFoldersInDatabase.getText()), toInt(this.txtDeletedFoldersInDatabase.getText()), toInt(this.txtConflictingFolders.getText())) );
				logger.info(String.format("   views:          %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllViews().size(), toInt(this.txtNewViewsInModel.getText()), toInt(this.txtUpdatedViewsInModel.getText()), toInt(this.txtDeletedViewsInModel.getText()), toInt(this.txtNewViewsInDatabase.getText()), toInt(this.txtUpdatedViewsInDatabase.getText()), toInt(this.txtDeletedViewsInDatabase.getText()), toInt(this.txtConflictingViews.getText())) );
				logger.info(String.format("   Objects:        %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllViewObjects().size(), toInt(this.txtNewViewObjectsInModel.getText()), toInt(this.txtUpdatedViewObjectsInModel.getText()), toInt(this.txtDeletedViewObjectsInModel.getText()), toInt(this.txtNewViewObjectsInDatabase.getText()), toInt(this.txtUpdatedViewObjectsInDatabase.getText()), toInt(this.txtDeletedViewObjectsInDatabase.getText()), toInt(this.txtConflictingViewObjects.getText())) );
				logger.info(String.format("   Connections:    %6d   %6d   %6d   %6d   %6d   %6d   %6d   %6d", this.exportedModel.getAllViewConnections().size(), toInt(this.txtNewViewConnectionsInModel.getText()), toInt(this.txtUpdatedViewConnectionsInModel.getText()), toInt(this.txtDeletedViewConnectionsInModel.getText()), toInt(this.txtNewViewConnectionsInDatabase.getText()), toInt(this.txtUpdatedViewConnectionsInDatabase.getText()), toInt(this.txtDeletedViewConnectionsInDatabase.getText()), toInt(this.txtConflictingViewConnections.getText())) );
				logger.info(String.format("   images:         %6d   %6d   %16s  %6d", ((IArchiveManager)this.exportedModel.getAdapter(IArchiveManager.class)).getLoadedImagePaths().size(), toInt(this.txtNewImagesInModel.getText()), "", toInt(this.txtNewImagesInDatabase.getText())) );
			}

			setMessage(message, GREEN_COLOR);
			if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("removeDirtyFlag") ) {
				if ( logger.isDebugEnabled() ) logger.debug("Removing model's dirty flag");
				this.stack.markSaveLocation();
			}
			if ( DBPlugin.INSTANCE.getPreferenceStore().getBoolean("closeIfSuccessful") ) {
				if ( logger.isDebugEnabled() ) logger.debug("Automatically closing the window as set in preferences");
				close();
				return;
			}
		} else {
			setMessage(message, RED_COLOR);
		}

		this.btnDoAction.setEnabled(false);
		this.btnClose.setText("Close");

		try {
			if ( this.btnClose.getText().equals("Cancel") && (this.connection != null) && this.connection.isConnected() )
				this.connection.rollback();
		} catch (SQLException e) {
			logger.error("Failed to check if database connection is closed.", e);
		}
	}

	@Override
	public void close() {
		// we remove the view screenshots to same memory
		Iterator<Entry<String, IDiagramModel>> viewsIterator = this.exportedModel.getAllViews().entrySet().iterator();
		while ( viewsIterator.hasNext() ) {
			increaseProgressBar();
			IDiagramModel view = viewsIterator.next().getValue();
			this.exportedModel.getDBMetadata(view).getScreenshot().dispose();
		}

		super.close();
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

	@Getter Text txtReleaseNote;

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

	private Label modelHorizontalSeparator;
	private Label modelVerticalSeparatorLeft;
	private Label modelVerticalSeparatorRight;
	private Label databaseHorizontalSeparator;
	private Label databaseVerticalSeparatorLeft;
	private Label databaseVerticalSeparatorRight;

	private Text txtTotalModelItself;
	private Text txtNewModelItselfInModel;
	private Text txtUpdatedModelItselfInModel;
	//private Text txtUpdatedModelItselfInDatabase;
	//private Text txtConflictingModelItself;
	
	private Text txtTotalProfiles;
	private Text txtNewProfilesInModel;
	private Text txtUpdatedProfilesInModel;
	private Text txtDeletedProfilesInModel;
	private Text txtNewProfilesInDatabase;
	private Text txtUpdatedProfilesInDatabase;
	private Text txtDeletedProfilesInDatabase;
	private Text txtConflictingProfiles;
	
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

    private boolean showRealTimeNumbers = DBPlugin.INSTANCE.getPreferenceStore().getBoolean("showRealTimeNumbers");

	Table tblModelVersions;
	Text txtModelName;
	Text txtPurpose;
}
