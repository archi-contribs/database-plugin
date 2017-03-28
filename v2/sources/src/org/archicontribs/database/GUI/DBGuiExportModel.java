package org.archicontribs.database.GUI;

import java.sql.ResultSet;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.archicontribs.database.DBChecksum;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.DBMetadata.CONFLICT_CHOICE;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
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

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

public class DBGuiExportModel extends DBGui {
	private static final DBLogger logger = new DBLogger(DBGuiExportModel.class);
	
	private ArchimateModel exportedModel = null;
	private Exception jobException = null;
	
	private Group grpComponents;
	private Group grpModel;
	
	//TODO : add a preference to immediately export the model on the first database with a standard release note or no release note at all
	//TODO : add an option to deactivate the check before the export if all the components have got a currentVersion
	//TODO : do not separate the export of components and graphical objects as now folders and views are shared
	
	
	/**
	 * Creates the GUI to export components and model
	 */
	public DBGuiExportModel(ArchimateModel model, String title) {
			// We call the DBGui constructor that will create the underlaying form and expose the compoRight, compoRightUp and compoRightBottom composites
		super(title);
		
			// We count the exported model's components and calculate their checksum in a separate thread
		Job job = new Job("countEObjects") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					popup("Please wait while counting model's components");
					model.countAllObjects();
				} catch (Exception err) {
					jobException = err;
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		
			// When the components count is finished, we fill in the corresponding labels
			//    ... and connect to the first database listed in the preferences by calling the DBFormGUI.getDatabases() method
			//    ... In return, this method calls  the ConnectedToDatabase() or NotConnectedToDatabase() methods
		job.addJobChangeListener(new JobChangeAdapter() {
	    	public void done(IJobChangeEvent event) {
	    		closePopup();			
	    		if (event.getResult().isOK()) {
	        		if ( logger.isDebugEnabled() ) logger.debug("the model has got "+model.getAllElements().size()+" elements and "+model.getAllRelationships().size()+" relationships.");
	        		getDatabases();
	        	} else {
	    			popup(Level.FATAL, "Error while counting model's components.", jobException);
		    		display.syncExec(new Runnable() {
						@Override
						public void run() {
							dialog.close();
						}
		    		});
	        	}
	    	}
	    });
			// We schedule the count job
		job.schedule();
		
			// We reference the exported model 
		exportedModel = model;

			// Create the graphical objects specific to the export
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI to export model \""+model.getName()+"\"");
		createGrpComponents();
		createGrpModel();
		compoRightBottom.setVisible(true);
		compoRightBottom.layout();
		
		createAction(ACTION.One, "1 - Confirm export");
		createAction(ACTION.Two, "2 - Export components");
		createAction(ACTION.Three, "3 - Export views");
		createAction(ACTION.Four, "4 - Statut");
		
			// we show an arrow in front of the first action
		setActiveAction(ACTION.One);
		
			// we show the option in the bottom
		setOption("Export type :", "Whole model", "The whole model will be exported, including the views and graphical components.", "Elements and relationships only", "Only the elements and relationships will be exported. This may be useful in case of a graph database for instance.", true);
		
			// We activate the btnDoAction button : if the user select the "Export" button --> call the exportComponents() method
		setBtnAction("Export", new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setActiveAction(STATUS.Ok);
				exportComponents();
			}
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
			// We rename the "close" button to "cancel"
		btnClose.setText("Cancel");
		
			// We activate the Eclipse Help framework
		setHelpHref("exportModel.html");
	}
	
	/**
	 * Creates a group displaying details about the exported model's components (elements and relationships) :<br>
	 * - total number<br>
	 * - number sync'ed with the database<br>
	 * - number that do not exist in the database<br>
	 * - number that exist in the database but with different values. 
	 */
	private void createGrpComponents() {
		grpComponents = new Group(compoRightBottom, SWT.SHADOW_ETCHED_IN);
		grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
		grpComponents.setFont(GROUP_TITLE_FONT);
		grpComponents.setText("Elements and relationships : ");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(0, 200);
		grpComponents.setLayoutData(fd);
		grpComponents.setLayout(new FormLayout());
		
		Label lblContains = new Label(grpComponents, SWT.CENTER);
		lblContains.setBackground(GROUP_BACKGROUND_COLOR);
		lblContains.setText("Your model contains :");
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		lblContains.setLayoutData(fd);
		
		Label sep1 = new Label(grpComponents, SWT.SEPARATOR | SWT.HORIZONTAL);
		fd = new FormData();
		fd.top = new FormAttachment(lblContains);
		fd.left = new FormAttachment(50, -60);
		fd.right = new FormAttachment(50, 60);
		sep1.setLayoutData(fd);
		
		txtTotalElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(sep1, 10);
		fd.left = new FormAttachment(0, 10);
		txtTotalElements.setLayoutData(fd);
		
		Label lblTotalElements = new Label(grpComponents, SWT.NONE);
		lblTotalElements.setBackground(GROUP_BACKGROUND_COLOR);
		lblTotalElements.setText("Elements in total, from those :");
		fd = new FormData();
		fd.top = new FormAttachment(txtTotalElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtTotalElements, 10);
		lblTotalElements.setLayoutData(fd);
		
		txtElementsInSync = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtElementsInSync.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtTotalElements, 5);
		fd.left = new FormAttachment(0, 30);
		txtElementsInSync.setLayoutData(fd);
		
		Label lblElementsInSync = new Label(grpComponents, SWT.NONE);
		lblElementsInSync.setBackground(GROUP_BACKGROUND_COLOR);
		lblElementsInSync.setText("are in sync with the database");
		fd = new FormData();
		fd.top = new FormAttachment(txtElementsInSync, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtElementsInSync, 10);
		lblElementsInSync.setLayoutData(fd);
		
		txtElementsNeedExport = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtElementsNeedExport.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtElementsInSync, 5);
		fd.left = new FormAttachment(0, 30);
		txtElementsNeedExport.setLayoutData(fd);
		
		Label lblElementsNeedExport = new Label(grpComponents, SWT.NONE);
		lblElementsNeedExport.setBackground(GROUP_BACKGROUND_COLOR);
		lblElementsNeedExport.setText("need to be exported, from those :");
		fd = new FormData();
		fd.top = new FormAttachment(txtElementsNeedExport, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtElementsNeedExport, 10);
		lblElementsNeedExport.setLayoutData(fd);
		
		txtNewElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtElementsNeedExport, 5);
		fd.left = new FormAttachment(0, 50);
		txtNewElements.setLayoutData(fd);
		
		Label lblNewElements = new Label(grpComponents, SWT.NONE);
		lblNewElements.setText("do not exist in the database");
		lblNewElements.setBackground(GROUP_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(txtNewElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtNewElements, 10);
		lblNewElements.setLayoutData(fd);
		
		txtUpdatedElements = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedElements.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtNewElements, 5);
		fd.left = new FormAttachment(0, 50);
		txtUpdatedElements.setLayoutData(fd);
		
		Label lblUpdatedElements = new Label(grpComponents, SWT.NONE);
		lblUpdatedElements.setText("have got updated values");
		lblUpdatedElements.setBackground(GROUP_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(txtUpdatedElements, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtUpdatedElements, 10);
		lblUpdatedElements.setLayoutData(fd);
		
		Label sep2 = new Label(grpComponents, SWT.SEPARATOR | SWT.VERTICAL);
		fd = new FormData();
		fd.top = new FormAttachment(sep1);
		fd.left = new FormAttachment(50);
		fd.bottom = new FormAttachment(100, -10);
		sep2.setLayoutData(fd);
		
		txtTotalRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtTotalRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(sep1, 10);
		fd.left = new FormAttachment(50, 10);
		txtTotalRelationships.setLayoutData(fd);
		
		Label lblTotalRelationships = new Label(grpComponents, SWT.NONE);
		lblTotalRelationships.setBackground(GROUP_BACKGROUND_COLOR);
		lblTotalRelationships.setText("Relationships in total, from those :");
		fd = new FormData();
		fd.top = new FormAttachment(txtTotalRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtTotalRelationships, 10);
		lblTotalRelationships.setLayoutData(fd);
		
		txtRelationshipsInSync = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtRelationshipsInSync.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtTotalRelationships, 5);
		fd.left = new FormAttachment(50, 30);
		txtRelationshipsInSync.setLayoutData(fd);
		
		Label lblRelationshipsInSync = new Label(grpComponents, SWT.NONE);
		lblRelationshipsInSync.setBackground(GROUP_BACKGROUND_COLOR);
		lblRelationshipsInSync.setText("are in sync with the database");
		fd = new FormData();
		fd.top = new FormAttachment(txtRelationshipsInSync, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtRelationshipsInSync, 10);
		lblRelationshipsInSync.setLayoutData(fd);
		
		txtRelationshipsNeedExport = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtRelationshipsNeedExport.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtRelationshipsInSync, 5);
		fd.left = new FormAttachment(50, 30);
		txtRelationshipsNeedExport.setLayoutData(fd);
		
		Label lbRelationshipsNeedExport = new Label(grpComponents, SWT.NONE);
		lbRelationshipsNeedExport.setBackground(GROUP_BACKGROUND_COLOR);
		lbRelationshipsNeedExport.setText("need to be exported, from those :");
		fd = new FormData();
		fd.top = new FormAttachment(txtRelationshipsNeedExport, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtRelationshipsNeedExport, 10);
		lbRelationshipsNeedExport.setLayoutData(fd);
		
		txtNewRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtNewRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtRelationshipsNeedExport, 5);
		fd.left = new FormAttachment(50, 50);
		txtNewRelationships.setLayoutData(fd);
		
		Label lblNewRelationships = new Label(grpComponents, SWT.NONE);
		lblNewRelationships.setText("do not exist in the database");
		lblNewRelationships.setBackground(GROUP_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(txtNewRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtNewRelationships, 10);
		lblNewRelationships.setLayoutData(fd);
		
		txtUpdatedRelationships = new Text(grpComponents, SWT.BORDER | SWT.CENTER);
		txtUpdatedRelationships.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtNewRelationships, 5);
		fd.left = new FormAttachment(50, 50);
		txtUpdatedRelationships.setLayoutData(fd);
		
		Label lblUpdatedRelationships = new Label(grpComponents, SWT.NONE);
		lblUpdatedRelationships.setText("have got updated values");
		lblUpdatedRelationships.setBackground(GROUP_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(txtUpdatedRelationships, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtUpdatedRelationships, 10);
		lblUpdatedRelationships.setLayoutData(fd);
	}
	
	/**
	 * Creates a group displaying details about the exported model's objects (graphical objects, folders, views, ...) :<br>
	 * - total number<br>
	 * - number sync'ed with the database<br>
	 * - number that do not exist in the database<br>
	 * - number that exist in the database but with different values. 
	 */
	private void createGrpModel() {
		grpModel = new Group(compoRightBottom, SWT.NONE);
		grpModel.setBackground(GROUP_BACKGROUND_COLOR);
		grpModel.setText("Model : ");
		grpModel.setFont(GROUP_TITLE_FONT);
		FormData fd = new FormData();
		fd.top = new FormAttachment(grpComponents, 10);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100);
		grpModel.setLayoutData(fd);
		grpModel.setLayout(new FormLayout());
		
		Label lblReleaseNote = new Label(grpModel, SWT.NONE);
		lblReleaseNote.setBackground(GROUP_BACKGROUND_COLOR);
		lblReleaseNote.setText("Release note :");
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		lblReleaseNote.setLayoutData(fd);
		
		txtConnections = new Text(grpModel, SWT.BORDER);
		txtConnections.setEditable(false);
		txtConnections.setText("");
		fd = new FormData(26,18);
		fd.left = new FormAttachment(lblReleaseNote, 10);
		fd.bottom = new FormAttachment(100, -10);
		txtConnections.setLayoutData(fd);
		
		Label lblConnections = new Label(grpModel, SWT.NONE);
		lblConnections.setBackground(GROUP_BACKGROUND_COLOR);
		lblConnections.setText("Connections");
		fd = new FormData();
		fd.top = new FormAttachment(txtConnections, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtConnections, 5);
		lblConnections.setLayoutData(fd);
		
		txtViewsObjects = new Text(grpModel, SWT.BORDER);
		txtViewsObjects.setEditable(false);
		txtViewsObjects.setText("");
		fd = new FormData(26,18);
		fd.left = new FormAttachment(lblReleaseNote, 10);
		fd.bottom = new FormAttachment(txtConnections, -5);
		txtViewsObjects.setLayoutData(fd);
		
		Label lblViewsObjects = new Label(grpModel, SWT.NONE);
		lblViewsObjects.setBackground(GROUP_BACKGROUND_COLOR);
		lblViewsObjects.setText("Views objects");
		fd = new FormData();
		fd.top = new FormAttachment(txtViewsObjects, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtViewsObjects, 5);
		lblViewsObjects.setLayoutData(fd);
		
		txtViews = new Text(grpModel, SWT.BORDER | SWT.CENTER);
		txtViews.setEditable(false);
		txtViews.setText("");
		fd = new FormData(26,18);
		fd.left = new FormAttachment(lblReleaseNote, 10);
		fd.bottom = new FormAttachment(txtViewsObjects, -5);
		txtViews.setLayoutData(fd);
		
		Label lblViews = new Label(grpModel, SWT.NONE);
		lblViews.setBackground(GROUP_BACKGROUND_COLOR);
		lblViews.setText("Views");
		fd = new FormData();
		fd.top = new FormAttachment(txtViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtViews, 5);
		lblViews.setLayoutData(fd);
		
		Label lblContent = new Label(grpModel, SWT.NONE);
		lblContent.setBackground(GROUP_BACKGROUND_COLOR);
		lblContent.setText("Content :");
		fd = new FormData();
		fd.top = new FormAttachment(txtViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(0, 10);
		lblContent.setLayoutData(fd);
		
		txtFolders = new Text(grpModel, SWT.BORDER);
		txtFolders.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtViews, 0, SWT.CENTER);
		fd.left = new FormAttachment(0, 300);
		txtFolders.setLayoutData(fd);
		
		Label lblFolders = new Label(grpModel, SWT.NONE);
		lblFolders.setBackground(GROUP_BACKGROUND_COLOR);
		lblFolders.setText("Folders");
		fd = new FormData();
		fd.top = new FormAttachment(txtFolders, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtFolders, 5);
		lblFolders.setLayoutData(fd);
		
		txtImages = new Text(grpModel, SWT.BORDER);
		txtImages.setEditable(false);
		fd = new FormData(26,18);
		fd.top = new FormAttachment(txtFolders, 5);
		fd.left = new FormAttachment(txtFolders, 0, SWT.LEFT);
		txtImages.setLayoutData(fd);
		
		Label lblImages = new Label(grpModel, SWT.NONE);
		lblImages.setBackground(GROUP_BACKGROUND_COLOR);
		lblImages.setText("Images");
		fd = new FormData();
		fd.top = new FormAttachment(txtImages, 0, SWT.CENTER);
		fd.left = new FormAttachment(txtImages, 5);
		lblImages.setLayoutData(fd);
		
		
		txtReleaseNote = new Text(grpModel, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		fd = new FormData();
		fd.top = new FormAttachment(lblReleaseNote, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblReleaseNote, 10);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(txtViews, -10);
		txtReleaseNote.setLayoutData(fd);
		
		
		Button btnGetDetails = new Button(grpModel, SWT.NONE);
		btnGetDetails.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		btnGetDetails.setText("Show more details");
		fd = new FormData();
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		btnGetDetails.setLayoutData(fd);
	}
	
	/**
	 * This method is called each time a database is selected and a connection has been established to it.<br>
	 * <br>
	 * It calls the database.checkComponentsToExport() method to compare exported model's components checksum with the one stored in the database and fill in their count in the corresponding labels.<br>
	 * If no exception is raised, then the "Export" button is enabled.
	 */
	@Override
	protected void connectedToDatabase() {	
			// We count the components to export and activate the export button
		if ( logger.isDebugEnabled() ) logger.debug("Checking which components to export.");
		
		txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
		txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
		
		txtImages.setText(String.valueOf(((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()));
		txtFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
		txtViews.setText(String.valueOf(exportedModel.getAllViews().size()));
		txtViewsObjects.setText(String.valueOf(exportedModel.getAllViewsObjects().size()));
		txtConnections.setText(String.valueOf(exportedModel.getAllViewsConnections().size()));
		
		try {
			database.checkComponentsToExport(exportedModel);
			
			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllElements().size()+" elements in the model, from those "+database.getCountIdenticalElements()+" in sync with the database and "+(database.getCountNewElements()+database.getCountUpdatedElements())+" need to be exported ("+database.getCountNewElements()+" new, "+database.getCountUpdatedElements()+" updated)");			
			txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
			txtNewElements.setText(String.valueOf(database.getCountNewElements()));
			txtUpdatedElements.setText(String.valueOf(database.getCountUpdatedElements()));
			txtElementsInSync.setText(String.valueOf(database.getCountIdenticalElements()));
			txtElementsNeedExport.setText(String.valueOf(database.getCountNewElements()+database.getCountUpdatedElements()));
			
			if ( logger.isDebugEnabled() ) logger.debug(exportedModel.getAllRelationships().size()+" Relationships in the model, from those "+database.getCountIdenticalRelationships()+" in sync with the database and "+(database.getCountNewRelationships()+database.getCountUpdatedRelationships())+" need to be exported ("+database.getCountNewRelationships()+" new, "+database.getCountUpdatedRelationships()+" updated)");			
			txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
			txtNewRelationships.setText(String.valueOf(database.getCountNewRelationships()));
			txtUpdatedRelationships.setText(String.valueOf(database.getCountUpdatedRelationships()));
			txtRelationshipsInSync.setText(String.valueOf(database.getCountIdenticalRelationships()));
			txtRelationshipsNeedExport.setText(String.valueOf(database.getCountNewRelationships()+database.getCountUpdatedRelationships()));
		} catch (Exception err) {
			popup(Level.FATAL, "Failed to check existing components in database", err);
			setActiveAction(STATUS.Error);
			return;
		}
		
			// we check if the model already exists in the database
		try {
			//TODO : just do it ...
		} catch (Exception err) {
			popup(Level.FATAL, "Failed to check existing models in database", err);
			setActiveAction(STATUS.Error);
			return;
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Enabling the \"Export\" button.");;
		btnDoAction.setEnabled(true);
	}
	
	/**
	 * Empties the labels showing the number of components equals or different from their database values.<br>
	 * <br>
	 * This method is called each time a database is selected and a connection failed to be established to it.
	 */
	@Override
	protected void notConnectedToDatabase() {
		txtTotalElements.setText(String.valueOf(exportedModel.getAllElements().size()));
		txtTotalRelationships.setText(String.valueOf(exportedModel.getAllRelationships().size()));
		txtNewElements.setText("");
		txtUpdatedElements.setText("");
		txtElementsInSync.setText("");
		txtElementsNeedExport.setText("");
		txtNewRelationships.setText("");
		txtUpdatedRelationships.setText("");
		txtRelationshipsInSync.setText("");
		txtRelationshipsNeedExport.setText("");
		
		txtImages.setText(String.valueOf(((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()));
		txtFolders.setText(String.valueOf(exportedModel.getAllFolders().size()));
		txtViews.setText(String.valueOf(exportedModel.getAllViews().size()));
		txtViewsObjects.setText(String.valueOf(exportedModel.getAllViewsObjects().size()));
		txtConnections.setText(String.valueOf(exportedModel.getAllViewsConnections().size()));
	}
	
	/**
	 * Exports the elements and relationships<br>
	 * When a conflict is detected, the component ID is added to the tblListConflicts<br>
	 * <br>
	 * This method is called when the user clicks on the "Export" button<br>
	 * <br>
	 * When the export of the components is finished, it calls the exportViews() method
	 */
	protected void exportComponents() {
		int exportSize;
		if ( getOptionValue() ) {
			if ( logger.isDebugEnabled() ) logger.debug("Exporting model : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships, "+exportedModel.getAllRelationships().size()+" folders, "+exportedModel.getAllViews().size()+" views, "+exportedModel.getAllViewsObjects().size()+" views objects, "+exportedModel.getAllViewsConnections().size()+" views connections, and "+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size()+" images.");
			exportSize = exportedModel.getAllFolders().size()+exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size()+exportedModel.getAllViews().size()+exportedModel.getAllViewsObjects().size()+exportedModel.getAllViewsConnections().size()+((IArchiveManager)exportedModel.getAdapter(IArchiveManager.class)).getImagePaths().size();
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Exporting components only : "+exportedModel.getAllElements().size()+" elements, "+exportedModel.getAllRelationships().size()+" relationships.");
			exportSize = exportedModel.getAllElements().size()+exportedModel.getAllRelationships().size();
		}
		
		
			// First, we disable the export button to avoid a second click
		btnDoAction.setEnabled(false);
		
			// Then we disable the option between an whole model export or a components only export
		disableOption();
		
			// Then we hide the grpComponents and grpModel groups to free up the compoRightBottom composite
		grpComponents.setVisible(false);
		grpModel.setVisible(false);
		
			// We show up a small arrow in front of the second action "export components"
		setActiveAction(ACTION.Two);
		
			// We hide the grpDatabase and create a progressBar instead 
		hideGrpDatabase();
		createProgressBar();
		createGrpConflict();
		
		
			// We configure the progress bar
		lblProgressBar.setText("Exporting components ...");
		setProgressBar(0, exportSize);
		
			// then, we start a new database transaction
		try {
			database.setAutoCommit(false);
		} catch (SQLException err ) {
			popup(Level.FATAL, "Failed to create a transaction in the database.", err);
			setActiveAction(STATUS.Error);
			doShowResult();
		}
		
		String releaseNote = txtReleaseNote.getText();
		
			// we start a new thread to export the components
		Job job = new Job("exportComponents") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
						// if we need to save the model
					if ( getOptionValue() ) {
							// we retrieve the latest version of the model in the database and increase the version number.
							// we use COALESCE to guarantee that a value is returned, even if the model id does not exist in the database
						//TODO : mode to DBdatabase class
						ResultSet result = database.select("SELECT COALESCE(MAX(version),0) as version FROM models WHERE id = ?", exportedModel.getId());
						result.next();
						exportedModel.setExportedVersion(result.getInt("version") + 1);
						result.close();
	
						if ( logger.isDebugEnabled() ) logger.debug("Exporting version "+exportedModel.getExportedVersion()+" of the model");
						database.exportModel(exportedModel, releaseNote);
						
						if ( logger.isDebugEnabled() ) logger.debug("Exporting folders");
						for (IFolder folder: exportedModel.getFolders())
							exportFolder(folder);
					}
					
					if ( logger.isDebugEnabled() ) logger.debug("Exporting elements");
					Iterator<Entry<String, IArchimateElement>> elementsIterator = exportedModel.getAllElements().entrySet().iterator();
					while ( elementsIterator.hasNext() ) {
						doExportComponent(elementsIterator.next().getValue());
					}
					
					if ( logger.isDebugEnabled() ) logger.debug("Exporting relationships");
					Iterator<Entry<String, IArchimateRelationship>> relationshipsIterator = exportedModel.getAllRelationships().entrySet().iterator();
					while ( relationshipsIterator.hasNext() ) {
						doExportComponent(relationshipsIterator.next().getValue());
					}
				} catch (Exception err) {
					DBPlugin.setAsyncException(err);
					try  {
						database.rollback();
						popup(Level.FATAL, "An error occured while exporting the components.\n\nThe transaction has been rolled back to leave the database in a coherent state. You may solve the issue and export again your components.", err);
					} catch (SQLException e) {
						popup(Level.FATAL, "An error occured while exporting the components.\n\nThe transaction rollbacking failed and the database is left in an inconsistent state. Please check carrefully your database !", e);
					}
					jobException = err;
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		
			// if the user has to manually solve some conflicts, then we rollback the transaction in order to free up the database locks.
		job.addJobChangeListener(new JobChangeAdapter() {
	    	public void done(IJobChangeEvent event) {
		    	if (event.getResult().isOK()) {
		    		display.syncExec(new Runnable() {
						@Override
						public void run() {
							if ( logger.isDebugEnabled() ) logger.debug("Found "+tblListConflicts.getItemCount()+" components conflicting with database");
							if ( tblListConflicts.getItemCount() == 0 ) {
								setActiveAction(STATUS.Ok);
								exportViews();
							} else {
								if ( logger.isDebugEnabled() ) logger.debug("Export of components incomplete. Conflicts need to be manually resolved.");
								resetProgressBar();
								try  {
									database.rollback();
								} catch (Exception err) {
									popup(Level.FATAL, "Failed to rollback the transaction. Please check carrefully your database !", err);
									setActiveAction(STATUS.Error);
									doShowResult();
									return;
								}
								tblListConflicts.setSelection(0);
								tblListConflicts.notifyListeners(SWT.Selection, new Event());		// shows up the tblListConflicts table and calls fillInCompareTable()
							}
						}
					});
		    	} else {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							setActiveAction(STATUS.Error);
							doShowResult();
						}
		    		});
		    	}
	    	}
	    });
			// We schedule the export job
		job.schedule();
	}
	
	private void doExportComponent(IArchimateConcept componentToExport) throws Exception {
		if ( ((IDBMetadata)componentToExport).getDBMetadata().isUpdated() ) {
			try {
				database.exportComponent(componentToExport);
				if ( getOptionValue() )
					database.exportComponentInModel(componentToExport);
			} catch (SQLException err) {
					// if the SQL exception is not linked to a primary key violation, then we escalate the exception
					// unfortunately, this is constructor dependent; worst, it may change from one driver version to another version
				if (   (database.getDriver().equals("sqlite") && err.getErrorCode() != 19)				// specific error from SQLite driver
					|| (err.getSQLState()!=null && !err.getSQLState().startsWith("23")) ) {				// generic error
					throw err;
				}
					// if we're here, it means that a conflict has been detected
				if ( logger.isDebugEnabled() ) logger.debug("The component conflicts with the version in the database.");
				switch ( ((IDBMetadata)componentToExport).getDBMetadata().getConflictChoice() ) {
					case askUser :
						if ( logger.isDebugEnabled() ) logger.debug("The conflict has to be manually resolved by user.");
						display.syncExec(new Runnable() {
							@Override
							public void run() {
								new TableItem(tblListConflicts, SWT.NONE).setText(componentToExport.getId());
								if ( tblListConflicts.getItemCount() < 2 )
									lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" component conflicts with newer version in the database :");
								else
									lblCantExport.setText("Can't export because "+tblListConflicts.getItemCount()+" components conflict with newer version in the database :");
							}
			    		});
						break;
					case doNotExport :
						if ( logger.isDebugEnabled() ) logger.debug("The component is tagged \"do not export\", so we keep it as it is.");
						break;
					case exportToDatabase :
						if ( logger.isDebugEnabled() ) logger.debug("The component is tagged to force export to the database. ");
						((IDBMetadata)componentToExport).getDBMetadata().setCurrentVersion(0);
						database.exportComponent(componentToExport);
						if ( getOptionValue() )
							database.exportComponentInModel(componentToExport);
						break;
					case importFromDatabase :
						if ( logger.isDebugEnabled() ) logger.debug("The component is tagged \"import the database version\".");
						database.importComponent(componentToExport, 0);
						break;
				}
			}
		} else {
			if ( getOptionValue() )
				database.exportComponentInModel(componentToExport);
			if ( logger.isTraceEnabled() ) logger.trace(((IDBMetadata)componentToExport).getDBMetadata().getDebugName() + " does not need to be exported.");
		}
		increaseProgressBar();
	}
	
	/**
	 * Creates a group that will display the conflicts raised during the export process
	 */
	protected void createGrpConflict() {		
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
			public void handleEvent(Event e) {
				btnExportMyVersion.setEnabled(true);
				btnDoNotExport.setEnabled(true);
				btnImportDatabaseVersion.setEnabled(true);
				grpConflict.setVisible(true);
				compoRightBottom.layout();
				
					// if the conflicting component is not an element, then it is a relationship
				IArchimateConcept conflictingComponent =  exportedModel.getAllElements().get(tblListConflicts.getSelection()[0].getText());
				if ( conflictingComponent == null )
					conflictingComponent =  exportedModel.getAllRelationships().get(tblListConflicts.getSelection()[0].getText());
				database.fillInCompareTable(tblCompareComponent, conflictingComponent, null);
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
			public void widgetSelected(SelectionEvent e) { tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.importFromDatabase); }
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
			public void widgetSelected(SelectionEvent e) { tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.exportToDatabase); }
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
			public void widgetSelected(SelectionEvent e) { tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE.doNotExport); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		fd = new FormData(80,25);
		fd.right = new FormAttachment(btnExportMyVersion, -10);
		fd.bottom = new FormAttachment(100, -10);
		btnDoNotExport.setLayoutData(fd);
		
		Button checkRememberChoice = new Button(grpConflict, SWT.CHECK);
		checkRememberChoice.setText("Remember my choice");
		fd = new FormData();
		fd.right = new FormAttachment(btnDoNotExport, -20);
		fd.top = new FormAttachment(btnDoNotExport, 0, SWT.CENTER);
		checkRememberChoice.setLayoutData(fd);
		
		grpConflict.layout();
	}
	
	/**
	 * creates the group that will display a summary of the export process
	 */
	protected void createGrpStatus() {		
		grpStatus = new Group(compoRightBottom, SWT.NONE);
		grpStatus.setBackground(GROUP_BACKGROUND_COLOR);
		FormData fd_grpStatus = new FormData();
		fd_grpStatus.top = new FormAttachment(0);
		fd_grpStatus.bottom = new FormAttachment(100);
		fd_grpStatus.left = new FormAttachment(0);
		fd_grpStatus.right = new FormAttachment(100);
		grpStatus.setLayoutData(fd_grpStatus);
	}


	
	/**
	 * called when the user click on the btnExportMyVersion button<br>
	 * Sets the exportChoice on the component's DBmetadata and removes the component from the tblListconflicts table<br>
	 * If no conflict remain, then it relaunch the doExportComponents method 
	 */
	protected void tagComponentWithConflictResolutionChoice(CONFLICT_CHOICE choice) {
			// if the component is not an element, then it is a relationship
		IArchimateConcept componentToImport = exportedModel.getAllElements().get(tblListConflicts.getSelection()[0].getText());
		if ( componentToImport == null )
			componentToImport = exportedModel.getAllRelationships().get(tblListConflicts.getSelection()[0].getText());
		
		((IDBMetadata)componentToImport).getDBMetadata().setConflictChoice(choice);
		switch (choice) {
			case doNotExport:        if ( logger.isDebugEnabled() ) logger.debug("Tagging component to do not export");                      break;
			case exportToDatabase:   if ( logger.isDebugEnabled() ) logger.debug("Tagging component to export current version to database"); break;
			case importFromDatabase: if ( logger.isDebugEnabled() ) logger.debug("Tagging component to import database version");            break;
			case askUser:            if ( logger.isDebugEnabled() ) logger.debug("Tagging component to ask user");                           break;
		}
		
		int index = tblListConflicts.getSelectionIndex();
		tblListConflicts.remove(index);
		if ( logger.isDebugEnabled() ) logger.debug("Remaining " + tblListConflicts.getItemCount() + " conflicts");
		if ( tblListConflicts.getItemCount() == 0 ) {
			grpConflict.setVisible(false);
			exportComponents();
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


	
	protected void exportViews() {
		int rank;
		
		//TODO : move SQL code into DBdatabase class as it has been done for import
			// We show up a small arrow in front of the second action "export components"
		setActiveAction(ACTION.Three);
		
			// We hide the grpConflit, it will be visible if conflicts are detected later on
		grpConflict.setVisible(false);
		
			// if the user selected to export the whole model
		if ( getOptionValue() ) {
			setActiveAction(ACTION.Three);
				// We configure the progress bar
			lblProgressBar.setText("Exporting model ...");
			
			try {
				/////////////////////////////
				// We export the views and views_in_model
				/////////////////////////////
			if ( logger.isDebugEnabled() ) logger.debug("Exporting the views");
			rank = 0;
			for ( IDiagramModel view: exportedModel.getAllViews().values() ) {
				// TODO : manage conflicts
				((IDBMetadata)view).getDBMetadata().setExportedVersion(exportedModel.getExportedVersion());		// until conflicts are managed

				database.insert("INSERT INTO views_in_model (view_id, view_version, parent_folder_id, model_id, model_version, rank)"
						,view.getId()
						,((IDBMetadata)view).getDBMetadata().getExportedVersion()
						,((IFolder)view.eContainer()).getId()
						,exportedModel.getId()
						,exportedModel.getExportedVersion()
						,++rank
						);
				
				database.insert("INSERT INTO views (id, version, class, created_by, created_on, name, connection_router_type, documentation, hint_content, hint_title, viewpoint, background, checksum)"
						,view.getId()
						,((IDBMetadata)view).getDBMetadata().getExportedVersion()
						,view.getClass().getSimpleName()
						,System.getProperty("user.name")
						,new Timestamp(Calendar.getInstance().getTime().getTime())
						,view.getName()
						,view.getConnectionRouterType()
						,view.getDocumentation()
						,(view instanceof IHintProvider) ? ((IHintProvider)view).getHintContent() : null
						,(view instanceof IHintProvider) ? ((IHintProvider)view).getHintTitle() : null
						,(view instanceof IArchimateDiagramModel) ? ((IArchimateDiagramModel)view).getViewpoint() : null
						,(view instanceof ISketchModel) ? ((ISketchModel)view).getBackground() : null
						,((IDBMetadata)view).getDBMetadata().getCurrentChecksum()
						);
				
				database.exportProperties(view);
				
				// the view is exported
				((IDBMetadata)view).getDBMetadata().setCurrentVersion(((IDBMetadata)view).getDBMetadata().getExportedVersion());
				
				increaseProgressBar();
			}
			sync();
			
				/////////////////////////////
				// We export the views objects
				/////////////////////////////
			if ( logger.isDebugEnabled() ) logger.debug("Exporting the views objects");
			rank = 0;
			for ( EObject eObject: exportedModel.getAllViewsObjects().values() ) {
				EObject viewContainer = eObject.eContainer();
				while ( !((viewContainer instanceof IArchimateDiagramModel) || (viewContainer instanceof ICanvasModel) || (viewContainer instanceof ISketchModel)) ) {
					viewContainer = viewContainer.eContainer();
				}
				((IDBMetadata)eObject).getDBMetadata().setExportedVersion(((IDBMetadata)viewContainer).getDBMetadata().getExportedVersion());		// the object version is the version of its view container
				
				database.insert("INSERT INTO views_objects (id, version, container_id, view_id, view_version, class, element_id, diagram_ref_id, type, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, x, y, width, height, rank, checksum)"
						,((IIdentifier)eObject).getId()
						,((IDBMetadata)eObject).getDBMetadata().getExportedVersion()
						,((IIdentifier)eObject.eContainer()).getId()
						,((IIdentifier)viewContainer).getId()
						,((IDBMetadata)viewContainer).getDBMetadata().getExportedVersion()
						,eObject.getClass().getSimpleName()
						,(eObject instanceof IDiagramModelArchimateComponent) ? ((IDiagramModelArchimateComponent)eObject).getArchimateConcept().getId() : null
						,(eObject instanceof IDiagramModelReference) ? ((IDiagramModelReference)eObject).getReferencedModel().getId() : null
						,(eObject instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)eObject).getType() : null
						,(eObject instanceof IBorderObject) ? ((IBorderObject)eObject).getBorderColor() : null
						,(eObject instanceof IDiagramModelNote) ? ((IDiagramModelNote)eObject).getBorderType() : null
						,(eObject instanceof ITextContent) ? ((ITextContent)eObject).getContent() : null
						,(eObject instanceof IDocumentable && !(eObject instanceof IDiagramModelArchimateComponent)) ? ((IDocumentable)eObject).getDocumentation() : null		// They have got there own documentation. The others use the documentation of the corresponding ArchimateConcept
						,(eObject instanceof IHintProvider) ? ((IHintProvider)eObject).getHintContent() : null
						,(eObject instanceof IHintProvider) ? ((IHintProvider)eObject).getHintTitle() : null
								//TODO : add helpHintcontent and helpHintTitle
						,(eObject instanceof ILockable) ? (((ILockable)eObject).isLocked()?1:0) : null
						,(eObject instanceof IDiagramModelImageProvider) ? ((IDiagramModelImageProvider)eObject).getImagePath() : null
						,(eObject instanceof IIconic) ? ((IIconic)eObject).getImagePosition() : null
						,(eObject instanceof ILineObject) ? ((ILineObject)eObject).getLineColor() : null
						,(eObject instanceof ILineObject) ? ((ILineObject)eObject).getLineWidth() : null
						,(eObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)eObject).getFillColor() : null
						,(eObject instanceof IFontAttribute) ? ((IFontAttribute)eObject).getFont() : null
						,(eObject instanceof IFontAttribute) ? ((IFontAttribute)eObject).getFontColor() : null
						,(eObject instanceof INameable && !(eObject instanceof IDiagramModelArchimateComponent) && !(eObject instanceof IDiagramModelReference)) ? ((INameable)eObject).getName() : null		// They have got there own name. The others use the name of the corresponding ArchimateConcept
						,(eObject instanceof ICanvasModelSticky) ? ((ICanvasModelSticky)eObject).getNotes() : null
						,(eObject instanceof ITextAlignment) ? ((ITextAlignment)eObject).getTextAlignment() : null
						,(eObject instanceof ITextPosition) ? ((ITextPosition)eObject).getTextPosition() : null
						,(eObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)eObject).getBounds().getX() : null
						,(eObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)eObject).getBounds().getY() : null
						,(eObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)eObject).getBounds().getWidth() : null
						,(eObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)eObject).getBounds().getHeight() : null
						,++rank
						,((IDBMetadata)eObject).getDBMetadata().getCurrentChecksum()
						);
					
					if ( eObject instanceof IProperties && !(eObject instanceof IDiagramModelArchimateComponent))
						database.exportProperties((IProperties)eObject);
					
					// The viewObject is exported
					((IDBMetadata)eObject).getDBMetadata().setCurrentVersion(((IDBMetadata)eObject).getDBMetadata().getExportedVersion());
					increaseProgressBar();
				}
				sync();
					
					/////////////////////////////
					// We export the views connections
					/////////////////////////////
				if ( logger.isDebugEnabled() ) logger.debug("Exporting the views connections");
				rank = 0;
				for ( IConnectable eObject: exportedModel.getAllViewsConnections().values() ) {
					EObject viewContainer = eObject.eContainer();
					while ( !((viewContainer instanceof IArchimateDiagramModel) || (viewContainer instanceof ICanvasModel) || (viewContainer instanceof ISketchModel)) ) {
						viewContainer = viewContainer.eContainer();
					}
					
					((IDBMetadata)eObject).getDBMetadata().setExportedVersion(((IDBMetadata)viewContainer).getDBMetadata().getExportedVersion());		// the connection version is the version of its view container
					
					database.insert("INSERT INTO views_connections (id, version, container_id, view_id, view_version, class, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id,source_object_id, target_object_id, type, rank, checksum)"
							,((IIdentifier)eObject).getId()
							,((IDBMetadata)eObject).getDBMetadata().getExportedVersion()
							,((IIdentifier)eObject.eContainer()).getId()
							,((IIdentifier)viewContainer).getId()
							,((IDBMetadata)viewContainer).getDBMetadata().getExportedVersion()
							,eObject.getClass().getSimpleName()
							,(eObject instanceof INameable     && !(eObject instanceof IDiagramModelArchimateConnection)) ? ((INameable)eObject).getName() : null					// if there is a relationship behind, the name is the relationship name, so no need to store it.
							,(eObject instanceof IDocumentable && !(eObject instanceof IDiagramModelArchimateConnection)) ? ((IDocumentable)eObject).getDocumentation() : null		// if there is a relationship behind, the documentation is the relationship name, so no need to store it.
							,(eObject instanceof ILockable) ? (((ILockable)eObject).isLocked()?1:0) : null	
							,(eObject instanceof ILineObject) ? ((ILineObject)eObject).getLineColor() : null
							,(eObject instanceof ILineObject) ? ((ILineObject)eObject).getLineWidth() : null		
							,(eObject instanceof IFontAttribute) ? ((IFontAttribute)eObject).getFont() : null
							,(eObject instanceof IFontAttribute) ? ((IFontAttribute)eObject).getFontColor() : null
							,(eObject instanceof IDiagramModelArchimateConnection) ? ((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId() : null
							,(eObject instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)eObject).getSource().getId() : null
							,(eObject instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)eObject).getTarget().getId() : null
							,(eObject instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)eObject).getType() : (eObject instanceof IDiagramModelConnection) ? ((IDiagramModelConnection)eObject).getType() : null
							,++rank
							,((IDBMetadata)eObject).getDBMetadata().getCurrentChecksum()
							);
					
					if ( eObject instanceof IProperties )
						database.exportProperties((IProperties)eObject);
					
					if ( eObject instanceof IDiagramModelConnection ) {
						for ( int pos = 0 ; pos < ((IDiagramModelConnection)eObject).getBendpoints().size(); ++pos) {
							IDiagramModelBendpoint bendpoint = ((IDiagramModelConnection)eObject).getBendpoints().get(pos);
							database.insert("INSERT INTO bendpoints (parent_id, parent_version, rank, start_x, start_y, end_x, end_y)"
									,((IIdentifier)eObject).getId()
									,((IDBMetadata)eObject).getDBMetadata().getExportedVersion()
									,pos
									,bendpoint.getStartX()
									,bendpoint.getStartY()
									,bendpoint.getEndX()
									,bendpoint.getEndY()
									);
						}
					}
					// The viewConnection is exported
					((IDBMetadata)eObject).getDBMetadata().setCurrentVersion(((IDBMetadata)eObject).getDBMetadata().getExportedVersion());
					increaseProgressBar();
				}
				sync();
				
					/////////////////////////////
					// We export the images
					/////////////////////////////
				if ( logger.isDebugEnabled() ) logger.debug("Exporting the images");
				IArchiveManager archiveMgr = (IArchiveManager)exportedModel.getAdapter(IArchiveManager.class);
				for ( String path: archiveMgr.getImagePaths() ) {
					byte[] image = archiveMgr.getBytesFromEntry(path);
					String checksum = DBChecksum.calculateChecksum(image);

					ResultSet result = database.select("SELECT checksum FROM images WHERE path = ?", path);

						// if the image is not yet in the db, we insert it
					if ( !result.next() ) {
						database.insert("INSERT INTO images (path, image, checksum)"
								,path
								,image
								,checksum								
								);
					} else {		// if the image checksum is different from the one in the database, then we replace the image in the database
						if ( !checksum.equals(result.getString("checksum")) ) {
							database.request("UPDATE images SET image = ?, checksum = ? WHERE path = ?"
									,image
									,checksum
									,path
									);
						}
					}
					result.close();
					increaseProgressBar();
				}
				
				sync();

			} catch (Exception err) {
				try  {
					database.rollback();
					popup(Level.FATAL, "Failed to export components. The transaction has been rolled back.", err);
				} catch (SQLException e) {
					popup(Level.FATAL, "Failed to rollback the transaction. Please check carrefully your database !", e);
				}
				setActiveAction(STATUS.Error);
				doShowResult();
				return;
			}
		} else {
			setActiveAction(STATUS.Bypassed);
		}
		
		try  {
			database.commit();
			doShowResult();
		} catch (SQLException e) {
			setActiveAction(STATUS.Error);
			popup(Level.FATAL, "Failed to commit the transaction. Please check carrefully your database !", e);
		}
	}
	
	protected void exportFolder(IFolder folder) throws Exception {
		if ( logger.isTraceEnabled() ) logger.trace("Exporting folder "+folder.getName());
		database.exportFolder(folder);
		
		increaseProgressBar();
		
		for (IFolder subFolder: folder.getFolders()) {
			exportFolder(subFolder);
		}
	}
	
	protected void doShowResult() {
		//grpProgressBar.setVisible(false);
		grpConflict.setVisible(false);
		setActiveAction(ACTION.Four);
		btnClose.setText("close");
		
		close();
		
		//TODO: créer page de status
	}
	
	private Group grpStatus;
	
	private Button btnDoNotExport;
	private Button btnExportMyVersion;
	private Button btnImportDatabaseVersion;
	
	private Group grpConflict;
	
	private Table tblCompareComponent;
	private Table tblListConflicts;
	private Label lblCantExport;
	
	private Text txtReleaseNote;
	private Text txtTotalElements;
	private Text txtElementsInSync;
	private Text txtElementsNeedExport;
	private Text txtNewElements;
	private Text txtUpdatedElements;
	private Text txtTotalRelationships;
	private Text txtRelationshipsInSync;
	private Text txtRelationshipsNeedExport;
	private Text txtNewRelationships;
	private Text txtUpdatedRelationships;
	private Text txtViews;
	private Text txtViewsObjects;
	private Text txtConnections;
	private Text txtFolders;
	private Text txtImages;
}
