package org.archicontribs.database.GUI;

import java.util.List;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;

public class DBGuiImportModel extends DBGui {
	protected static final DBLogger logger = new DBLogger(DBGuiImportModel.class);
	
	private Exception jobException = null;
	
	private Table tblModels;
	private Table tblModelVersions;
	private Text txtFilterModels;
	private Text txtName;
	private Text txtPurpose;
	private Text txtReleaseNote;
	
	private Group grpModels;
	private Group grpModelVersions;

	/**
	 * Creates the GUI to import a model
	 */
	public DBGuiImportModel(String title) {
		super(title);
		
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for importing a model.");
		
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
					if ( modelId.equals(existingModel.getId()) ) {
						popup(Level.ERROR, "A model with ID \""+modelId+"\" already exists. Cannot import it again ...");
						return;
					}
					if ( checkName && modelName.equals(existingModel.getName()) ) {
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
		
			// We connect to the database and call the connectedToDatabase() method
		getDatabases();
	}
	
	/**
	 * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
	 */
	@Override
	protected void connectedToDatabase() {	
		createGrpModel();
		compoRightBottom.setVisible(true);
		compoRightBottom.layout();
		try {
			database.getModels(txtFilterModels.getText(), tblModels);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
				try {
					if ( database.isConnected() )
						database.getModels("%"+txtFilterModels.getText()+"%", tblModels);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
			}
		});
		fd = new FormData();
		fd.top = new FormAttachment(lblListModels, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblListModels, 5);
		fd.right = new FormAttachment(100, -10);
		txtFilterModels.setLayoutData(fd);
		
		
						
		tblModels = new Table(grpModels, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		tblModels.setLinesVisible(true);
		tblModels.setBackground(GROUP_BACKGROUND_COLOR);
		tblModels.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				try {
					if ( (tblModels.getSelection() != null) && (tblModels.getSelection().length > 0) && (tblModels.getSelection()[0] != null) )
						database.getModelVersions((String) tblModels.getSelection()[0].getData("id"), tblModelVersions);
					else
						tblModelVersions.removeAll();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
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
					txtName.setText((String) tblModelVersions.getSelection()[0].getData("name"));
					btnDoAction.setEnabled(true);
				} else {
					btnDoAction.setEnabled(false);
				}
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
		colCreatedOn.setText("Created on");
		colCreatedOn.setWidth(120);
		
		TableColumn colCreatedBy = new TableColumn(tblModelVersions, SWT.NONE);
		colCreatedBy.setText("Created by");
		colCreatedBy.setWidth(125);
		
		Label lblName = new Label(grpModelVersions, SWT.NONE);
		lblName.setBackground(GROUP_BACKGROUND_COLOR);
		lblName.setText("Model name :");
		fd = new FormData();
		fd.top = new FormAttachment(tblModelVersions, 10);
		fd.left = new FormAttachment(0, 10);
		lblName.setLayoutData(fd);
		
		txtName = new Text(grpModelVersions, SWT.BORDER);
		txtName.setBackground(GROUP_BACKGROUND_COLOR);
		txtName.setEnabled(false);
		fd = new FormData();
		fd.top = new FormAttachment(lblName);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		txtName.setLayoutData(fd);
		
		Label lblPurpose = new Label(grpModelVersions, SWT.NONE);
		lblPurpose.setBackground(GROUP_BACKGROUND_COLOR);
		lblPurpose.setText("Purpose :");
		fd = new FormData();
		fd.top = new FormAttachment(txtName, 10);
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
		
		Label lblReleaseNote = new Label(grpModelVersions, SWT.NONE);
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
	 * Called when the user clicks on the "import" button 
	 */
	protected void doImport() {
		String modelName = tblModels.getSelection()[0].getText();
		String modelId = (String)tblModels.getSelection()[0].getData("id");
		
		hideGrpDatabase();
		createProgressBar();
		
		lblProgressBar.setText("Importing model \""+modelName+"\"");
		
		Job job;
		
		grpModels.setVisible(false);
		grpModelVersions.setVisible(false);
		setActiveAction(ACTION.Two);
		
			// we create the model (but do not create standard folder as they will be imported from the database)
		ArchimateModel model = (ArchimateModel)IArchimateFactory.eINSTANCE.createArchimateModel();
		model.setId(modelId);
		model.setName(modelName);
		model.setPurpose((String)tblModels.getSelection()[0].getData("purpose"));
		model.setCurrentVersion(Integer.valueOf(tblModelVersions.getSelection()[0].getText(0)));
		
			// we add the new model in the manager
		IEditorModelManager.INSTANCE.registerModel(model);
		
			// we import the model from the database in a separate thread
		job = new Job("importModel") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					//TODO : sortir les boucles de la classe database pour que le progressbar soit géré ici !!!
					if ( logger.isDebugEnabled() ) logger.debug("Importing the model metadata ...");
					int importSize = database.importModel(model);
					setProgressBar(0, importSize);
					
					if ( logger.isDebugEnabled() ) logger.debug("Importing the folders ...");
					database.prepareImportFolders(model);
					while (database.importFolders(model))
						increaseProgressBar();
					sync();
					
					if ( logger.isDebugEnabled() ) logger.debug("Importing the elements ...");
					database.prepareImportElements(model);
					while (database.importElements(model))
						increaseProgressBar();
					sync();
					
					if ( logger.isDebugEnabled() ) logger.debug("Importing the relationships ...");
					database.prepareImportRelationships(model);
					while (database.importRelationships(model))
						increaseProgressBar();
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							model.setSourceAndTarget();
						}
		    		});
					sync();
					
					if ( logger.isDebugEnabled() ) logger.debug("Importing the views ...");
					database.prepareImportViews(model);
					while (database.importViews(model))
						increaseProgressBar();
					sync();
					
					if ( logger.isDebugEnabled() ) logger.debug("Importing the views objects ...");
					for (String viewId: model.getAllViews().keySet()) {
						database.prepareImportViewsObjects(model, viewId);
						while (database.importViewsObjects(model, viewId))
							increaseProgressBar();
					}
					sync();
					
					if ( logger.isDebugEnabled() ) logger.debug("Importing the views connections ...");
					for (String viewId: model.getAllViews().keySet()) {
						database.prepareImportViewsConnections(model, viewId);
						while (database.importViewsConnections(model))
							increaseProgressBar();
					}
					sync();
					
					if ( logger.isDebugEnabled() ) logger.debug("Reconnecting connections ...");
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							model.reconnectConnections();
						}
		    		});
					
					if ( logger.isDebugEnabled() ) logger.debug("importing the images ...");
					for (String path: database.getAllImagePaths()) {
						database.importImage(model, path);
						increaseProgressBar();
					}
					sync();
					
				} catch (Exception err) {
					jobException = err;
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		
		job.addJobChangeListener(new JobChangeAdapter() {
	    	public void done(IJobChangeEvent event) {
	    		//TODO : fill in the status page !!!
	    		if (event.getResult().isOK()) {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							setActiveAction(STATUS.Ok);
							setActiveAction(ACTION.Three);
			    			btnClose.setText("close");
						}
		    		});
	    			popup(Level.INFO, "The model has been successfully imported.");
	    			close();
	        	} else {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							setActiveAction(STATUS.Error);
							setActiveAction(ACTION.Three);
			    			btnClose.setText("close");
						}
		    		});
	    			popup(Level.FATAL, "Error while importing model.", jobException);
	        	}
	    	}
	    });
		
			// We schedule the import
		job.schedule();
	}
	
	/**
	 * Waits for all the asynchronous commands sent to SWT have finished
	 */
	private void sync() throws InterruptedException {
		Object objSync = new Object();
		synchronized(objSync) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if ( logger.isTraceEnabled() ) logger.trace("Synchronizing threads ...");
					synchronized(objSync) { objSync.notify(); }
				}
			});
			objSync.wait();
		}
	}
}
