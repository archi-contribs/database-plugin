/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBCanvasFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class DBGuiImportComponent extends DBGui {
	protected static final DBLogger logger = new DBLogger(DBGuiImportComponent.class);
	
	protected ArchimateModel importedModel;
	
	private Group grpFilter;
	private Group grpComponent;
	
	private Composite compoElements;
	private Button radioOptionElement;
	private Button radioOptionView;
	private Text filterName;
	private Button ignoreCase;
	private Button hideOption;             // to hide empty names for elements and relationships, top level folders and default views
	private Button hideAlreadyInModel;
		
	//private Composite compoContainers;
	
	//private Composite compoFolders;
	//private Button strategyFolders;
	//private Button applicationFolders;
	//private Button businessFolders;
	//private Button technologyFolders;
	//private Button otherFolders;
	//private Button motivationFolders;
	//private Button implementationFolders;
	
	private Composite compoViews;
	private Button archimateViews;
	private Button canvasViews;
	private Button sketchViews;
	
	private Table tblComponents;
	
	
	ComponentLabel resourceLabel;
	ComponentLabel capabilityLabel;
	ComponentLabel courseOfActionLabel;
	ComponentLabel applicationComponentLabel;
	ComponentLabel applicationCollaborationLabel;
	ComponentLabel applicationInterfaceLabel;
	ComponentLabel applicationFunctionLabel;
	ComponentLabel applicationInteractionLabel;
	ComponentLabel applicationEventLabel;
	ComponentLabel applicationServiceLabel;
	ComponentLabel dataObjectLabel;
	ComponentLabel applicationProcessLabel;
	ComponentLabel businessActorLabel;
	ComponentLabel businessRoleLabel;
	ComponentLabel businessCollaborationLabel;
	ComponentLabel businessInterfaceLabel;
	ComponentLabel businessProcessLabel;
	ComponentLabel businessFunctionLabel;
	ComponentLabel businessInteractionLabel;
	ComponentLabel businessEventLabel;
	ComponentLabel businessServiceLabel;
	ComponentLabel businessObjectLabel;
	ComponentLabel contractLabel;
	ComponentLabel representationLabel;
	ComponentLabel nodeLabel;
	ComponentLabel deviceLabel;
	ComponentLabel systemSoftwareLabel;
	ComponentLabel technologyCollaborationLabel;
	ComponentLabel technologyInterfaceLabel;
	ComponentLabel pathLabel;
	ComponentLabel communicationNetworkLabel;
	ComponentLabel technologyFunctionLabel;
	ComponentLabel technologyProcessLabel;
	ComponentLabel technologyInteractionLabel;
	ComponentLabel technologyEventLabel;
	ComponentLabel technologyServiceLabel;
	ComponentLabel artifactLabel;
	ComponentLabel equipmentLabel;
	ComponentLabel facilityLabel;
	ComponentLabel distributionNetworkLabel;
	ComponentLabel materialLabel;
	ComponentLabel workpackageLabel;
	ComponentLabel deliverableLabel;
	ComponentLabel implementationEventLabel;
	ComponentLabel plateauLabel;
	ComponentLabel gapLabel;
	ComponentLabel stakeholderLabel;
	ComponentLabel driverLabel;
	ComponentLabel assessmentLabel;
	ComponentLabel goalLabel;
	ComponentLabel outcomeLabel;
	ComponentLabel principleLabel;
	ComponentLabel requirementLabel;
	ComponentLabel constaintLabel;
	ComponentLabel smeaningLabel;
	ComponentLabel valueLabel;
	ComponentLabel productLabel;
	
	ComponentLabel[] allElementLabels;
	
	private Exception jobException = null;
	
	/**
	 * Creates the GUI to import components
	 */
	public DBGuiImportComponent(ArchimateModel model, String title) {
		super(title);
		
			// We count the imported model's components in a separate thread
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
		
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for importing a component.");

			// if a model has been specified, it means that the component should be imported in the selected model
		importedModel = model;
		
		createAction(ACTION.One, "1 - Choose component");
		createAction(ACTION.Two, "2 - Import component");
		createAction(ACTION.Three, "3 - Status");
		setActiveAction(ACTION.One);
		
			// we show the option in the bottom
		setOption("Import type :", "Shared", "The component will be shared between models. All your modifications will be visible by other models.", "Copy", "A copy of the component will be created. All your modifications will remain private to your model and will not be visible by other models.", DBPlugin.INSTANCE.getPreferenceStore().getBoolean("importShared"));
		
			// We activate the btnDoAction button : if the user select the "Import" button --> call the doImport() method
		setBtnAction("Import", new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				setActiveAction(STATUS.Ok);
				btnDoAction.setEnabled(false);
				try {
					doImport();
				} catch (Exception err) {
					DBGui.popup(Level.ERROR, "An exception has been raised during import.", err);
				}
			}
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		
			// We rename the "close" button to "cancel"
		btnClose.setText("Cancel");
		
			// We activate the Eclipse Help framework
		setHelpHref("importComponent.html");
	}

	/**
	 * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
	 */
	@Override
	protected void connectedToDatabase(boolean ignore) {	
		createGrpFilter();
		createGrpComponents();
		compoRightBottom.setVisible(true);
		compoRightBottom.layout();
	}
	
	private void createGrpFilter() {
		grpFilter = new Group(compoRightBottom, SWT.NONE);
		grpFilter.setBackground(GROUP_BACKGROUND_COLOR);
		grpFilter.setFont(GROUP_TITLE_FONT);
		grpFilter.setText("Filter : ");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(50, -5);
		grpFilter.setLayoutData(fd);
		grpFilter.setLayout(new FormLayout());
		
		Label chooseCategory = new Label(grpFilter, SWT.NONE);
		chooseCategory.setBackground(GROUP_BACKGROUND_COLOR);
		chooseCategory.setFont(BOLD_FONT);
		chooseCategory.setText("Category :");
		fd = new FormData();
		fd.top = new FormAttachment(0, 20);
		fd.left = new FormAttachment(0, 10);
		chooseCategory.setLayoutData(fd);
		
		radioOptionElement = new Button(grpFilter, SWT.RADIO);
		radioOptionElement.setBackground(GROUP_BACKGROUND_COLOR);
		radioOptionElement.setText("Elements");
		radioOptionElement.setSelection(true);
		radioOptionElement.addSelectionListener(new SelectionListener() {
		    @Override public void widgetSelected(SelectionEvent event) {
		        try {
		            getElements();
		        } catch (Exception err) {
		            DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
		        }
		    } @Override public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(chooseCategory, 5);
		fd.left = new FormAttachment(0, 20);
		radioOptionElement.setLayoutData(fd);
		
		radioOptionView = new Button(grpFilter, SWT.RADIO);
		radioOptionView.setBackground(GROUP_BACKGROUND_COLOR);
		radioOptionView.setText("Views");
		radioOptionView.addSelectionListener(new SelectionListener() {
		    @Override public void widgetSelected(SelectionEvent event) {
		        try {
		            getViews();
		        } catch (Exception err) {
		            DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
		        }
		    }
		    @Override public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(radioOptionElement, 5);
		fd.left = new FormAttachment(0, 20);
		radioOptionView.setLayoutData(fd);
		
		Label chooseName = new Label(grpFilter, SWT.NONE);
		chooseName.setBackground(GROUP_BACKGROUND_COLOR);
		chooseName.setFont(BOLD_FONT);
		chooseName.setText("Name filter :");
		fd = new FormData();
		fd.top = new FormAttachment(radioOptionView, 10);
		fd.left = new FormAttachment(0, 10);
		chooseName.setLayoutData(fd);
		
		filterName = new Text(grpFilter, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(chooseName, 5);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(0, 125);
		filterName.setLayoutData(fd);
		filterName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				try {
					if ( database.isConnected() ) {
						if ( compoElements.isVisible() )
							getElements();
						//else if ( compoContainers.isVisible() )
						//	getContainers();
						//else if ( compoFolders.isVisible() )
						//	getFolders();
						else if ( compoViews.isVisible() )
							getViews();
					}
				} catch (Exception err) {
				    DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
				} 
			}
		});
		
		ignoreCase = new Button(grpFilter, SWT.CHECK);
		ignoreCase.setBackground(GROUP_BACKGROUND_COLOR);
		ignoreCase.setText("Ignore case");
		ignoreCase.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(filterName, 5);
		fd.left = new FormAttachment(0, 10);
		ignoreCase.setLayoutData(fd);
		ignoreCase.addListener(SWT.MouseUp, new Listener() {
	    	@Override
	    	public void handleEvent(Event event) {
				try {
					if ( database.isConnected() ) {
						if ( compoElements.isVisible() )
							getElements();
						//else if ( compoContainers.isVisible() )
						//	getContainers();
						//else if ( compoFolders.isVisible() )
						//	getFolders();
						else if ( compoViews.isVisible() )
							getViews();
					}
				} catch (Exception err) {
				    DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
				} 
			}
		});
		
		createCompoElements();		compoElements.setVisible(true);
		//createCompoComposites();	compoContainers.setVisible(false);
		//createCompoFolders();		compoFolders.setVisible(false);
		createCompoViews();			compoViews.setVisible(false);
	}
	
	private void createCompoElements() {		
		compoElements = new Composite(grpFilter, SWT.NONE);
		compoElements.setBackground(GROUP_BACKGROUND_COLOR);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		compoElements.setLayoutData(fd);
		compoElements.setLayout(new FormLayout());
		
		Composite strategyActiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite strategyBehaviorCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite strategyPassiveCompo = new Composite(compoElements, SWT.TRANSPARENT );
		
		Composite businessActiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite businessBehaviorCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite businessPassiveCompo = new Composite(compoElements, SWT.TRANSPARENT );
		
		Composite applicationActiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite applicationBehaviorCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite applicationPassiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		
		Composite technologyActiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite technologyBehaviorCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite technologyPassiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		
		Composite physicalActiveCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite physicalBehaviorCompo = new Composite(compoElements, SWT.TRANSPARENT);
		Composite physicalPassive = new Composite(compoElements, SWT.TRANSPARENT);
		
		Composite implementationCompo = new Composite(compoElements, SWT.TRANSPARENT);

		
		Composite motivationCompo = new Composite(compoElements, SWT.TRANSPARENT);
		
		/********************************************************************************************************************************************************************************************************/
		// Strategy layer
			// Passive
			// Behavior
		capabilityLabel = new ComponentLabel(strategyBehaviorCompo,  "Capability");
		courseOfActionLabel = new ComponentLabel(strategyBehaviorCompo,  "Course Of Action");
			// Active
		resourceLabel = new ComponentLabel(strategyActiveCompo, "Resource");
		
		// Business layer
			// Passive
		productLabel = new ComponentLabel(businessPassiveCompo, "Product");
			// Behavior
		businessProcessLabel = new ComponentLabel(businessBehaviorCompo, "Business Process");
		businessFunctionLabel = new ComponentLabel(businessBehaviorCompo, "Business Function");
		businessInteractionLabel = new ComponentLabel(businessBehaviorCompo, "Business Interaction");
		businessEventLabel = new ComponentLabel(businessBehaviorCompo, "Business Event");
		businessServiceLabel = new ComponentLabel(businessBehaviorCompo, "Business Service");
		businessObjectLabel = new ComponentLabel(businessBehaviorCompo, "Business Object");
		contractLabel = new ComponentLabel(businessBehaviorCompo, "Contract");
		representationLabel = new ComponentLabel(businessBehaviorCompo, "Representation");
			// Active
		businessActorLabel = new ComponentLabel(businessActiveCompo, "Business Actor");
		businessRoleLabel = new ComponentLabel(businessActiveCompo, "Business Role");
		businessCollaborationLabel = new ComponentLabel(businessActiveCompo, "Business Collaboration");
		businessInterfaceLabel = new ComponentLabel(businessActiveCompo, "Business Interface");
	
		// Application layer
			//Passive
		dataObjectLabel = new ComponentLabel(applicationPassiveCompo, "Data Object");
			//Behavior
		applicationFunctionLabel = new ComponentLabel(applicationBehaviorCompo, "Application Function");
		applicationInteractionLabel = new ComponentLabel(applicationBehaviorCompo, "Application Interaction");
		applicationEventLabel = new ComponentLabel(applicationBehaviorCompo, "Application Event");
		applicationServiceLabel = new ComponentLabel(applicationBehaviorCompo, "Application Service");
		applicationProcessLabel = new ComponentLabel(applicationBehaviorCompo, "Application Process");
		//	Active		
		applicationComponentLabel = new ComponentLabel(applicationActiveCompo, "Application Component");
		applicationCollaborationLabel = new ComponentLabel(applicationActiveCompo, "Application Collaboration");
		applicationInterfaceLabel = new ComponentLabel(applicationActiveCompo, "Application Interface");
			
		// Technology layer
			// Passive
		artifactLabel = new ComponentLabel(technologyPassiveCompo, "Artifact");
			// Behavior
		technologyFunctionLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Function");
		technologyProcessLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Process");
		technologyInteractionLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Interaction");
		technologyEventLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Event");
		technologyServiceLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Service");
			// Active
		nodeLabel = new ComponentLabel(technologyActiveCompo, "Node");
		deviceLabel = new ComponentLabel(technologyActiveCompo, "Device");
		systemSoftwareLabel = new ComponentLabel(technologyActiveCompo, "System Software");
		technologyCollaborationLabel = new ComponentLabel(technologyActiveCompo, "Technology Collaboration");
		technologyInterfaceLabel = new ComponentLabel(technologyActiveCompo, "Technology Interface");
		pathLabel = new ComponentLabel(technologyActiveCompo, "Path");
		communicationNetworkLabel = new ComponentLabel(technologyActiveCompo, "Communication Network");

		// Physical layer
			// Passive
			// Behavior
		materialLabel = new ComponentLabel(physicalBehaviorCompo, "Material");
			// Active
		equipmentLabel = new ComponentLabel(physicalActiveCompo, "Equipment");
		facilityLabel = new ComponentLabel(physicalActiveCompo, "Facility");
		distributionNetworkLabel = new ComponentLabel(physicalActiveCompo, "Distribution Network");
		
		// Implementation layer
		workpackageLabel = new ComponentLabel(implementationCompo, "Work Package");
		deliverableLabel = new ComponentLabel(implementationCompo, "Deliverable");
		implementationEventLabel = new ComponentLabel(implementationCompo, "Implementation Event");
		plateauLabel = new ComponentLabel(implementationCompo, "Plateau");
		gapLabel = new ComponentLabel(implementationCompo, "Gap");
		
		// Motivation layer
		stakeholderLabel = new ComponentLabel(motivationCompo, "Stakeholder");
		driverLabel = new ComponentLabel(motivationCompo, "Driver");
		assessmentLabel = new ComponentLabel(motivationCompo, "Assessment");
		goalLabel = new ComponentLabel(motivationCompo, "Goal");
		outcomeLabel = new ComponentLabel(motivationCompo, "Outcome");
		principleLabel = new ComponentLabel(motivationCompo, "Principle");
		requirementLabel = new ComponentLabel(motivationCompo, "Requirement");
		constaintLabel = new ComponentLabel(motivationCompo, "Constraint");
		smeaningLabel = new ComponentLabel(motivationCompo, "Meaning");
		valueLabel = new ComponentLabel(motivationCompo, "Value");
		
		allElementLabels = new ComponentLabel[]{ resourceLabel, capabilityLabel, courseOfActionLabel, applicationComponentLabel, applicationCollaborationLabel, applicationInterfaceLabel, applicationFunctionLabel, applicationInteractionLabel, applicationEventLabel, applicationServiceLabel, dataObjectLabel, applicationProcessLabel, businessActorLabel, businessRoleLabel, businessCollaborationLabel, businessInterfaceLabel, businessProcessLabel, businessFunctionLabel, businessInteractionLabel, businessEventLabel, businessServiceLabel, businessObjectLabel, contractLabel, representationLabel, nodeLabel, deviceLabel, systemSoftwareLabel, technologyCollaborationLabel, technologyInterfaceLabel, pathLabel, communicationNetworkLabel, technologyFunctionLabel, technologyProcessLabel, technologyInteractionLabel, technologyEventLabel, technologyServiceLabel, artifactLabel, equipmentLabel, facilityLabel, distributionNetworkLabel, materialLabel, workpackageLabel, deliverableLabel, implementationEventLabel, plateauLabel, gapLabel, stakeholderLabel, driverLabel, assessmentLabel, goalLabel, outcomeLabel, principleLabel, requirementLabel, constaintLabel, smeaningLabel, valueLabel, productLabel};
		
		// Containers !!!
		//
		//createTableItem(tblClasses, "Grouping");
		//createTableItem(tblClasses, "Location");

		Label passiveLabel = new Label(compoElements, SWT.TRANSPARENT);
		Canvas passiveCanvas = new Canvas(compoElements, SWT.TRANSPARENT | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(implementationCompo, 1, SWT.TOP);
		fd.left = new FormAttachment(0, 60);
		fd.right = new FormAttachment(0, 100);
		passiveCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(passiveCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.CENTER);
		passiveLabel.setLayoutData(fd);
		passiveLabel.setText("Passive");
		passiveLabel.setBackground(PASSIVE_COLOR);

		Label behaviorLabel = new Label(compoElements, SWT.TRANSPARENT);
		Canvas behaviorCanvas = new Canvas(compoElements, SWT.TRANSPARENT | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(implementationCompo, 1, SWT.TOP);
		fd.left = new FormAttachment(0, 105);
		fd.right = new FormAttachment(57);
		behaviorCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(behaviorCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(behaviorCanvas, 0, SWT.CENTER);
		behaviorLabel.setLayoutData(fd);
		behaviorLabel.setText("Behavior");
		behaviorLabel.setBackground(PASSIVE_COLOR);
		
		Label activeLabel = new Label(compoElements, SWT.TRANSPARENT);
		Canvas activeCanvas = new Canvas(compoElements, SWT.TRANSPARENT | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(implementationCompo, 1, SWT.TOP);
		fd.left = new FormAttachment(57, 5);
		fd.right = new FormAttachment(100, -65);
		activeCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(activeCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(activeCanvas, 0, SWT.CENTER);
		activeLabel.setLayoutData(fd);
		activeLabel.setText("Active");
		activeLabel.setBackground(PASSIVE_COLOR);
		
		Label motivationLabel = new Label(compoElements, SWT.TRANSPARENT);
		Canvas motivationCanvas = new Canvas(compoElements, SWT.TRANSPARENT);
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(100);
		fd.left = new FormAttachment(100, -60);
		fd.right = new FormAttachment(100);
		motivationCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(motivationCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(motivationCanvas, 0, SWT.CENTER);
		motivationLabel.setLayoutData(fd);
		motivationLabel.setText("Motivation");
		motivationLabel.setBackground(MOTIVATION_COLOR);
		
		PaintListener redraw = new PaintListener() {
            public void paintControl(PaintEvent event) {
            	event.gc.setAlpha(100);
            	if ( event.widget == motivationCanvas ) 
            		event.gc.setBackground(MOTIVATION_COLOR);
            	else
            		event.gc.setBackground(PASSIVE_COLOR);
            	event.gc.fillRectangle(event.x, event.y, event.width, event.height);
            }
        };
		
        passiveCanvas.addPaintListener(redraw);
        behaviorCanvas.addPaintListener(redraw);
        activeCanvas.addPaintListener(redraw);
        motivationCanvas.addPaintListener(redraw);
		
		
		
		
		
		

		Label strategyLabel = new Label(compoElements, SWT.NONE);
		Canvas strategyCanvas = new Canvas(compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(10, 2);
		fd.bottom = new FormAttachment(25, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		strategyCanvas.setLayoutData(fd);
		strategyCanvas.setBackground(STRATEGY_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(strategyCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(strategyCanvas, 2, SWT.LEFT);
		strategyLabel.setLayoutData(fd);
		strategyLabel.setBackground(STRATEGY_COLOR);
		strategyLabel.setText("Strategy");
		
		Label businessLabel = new Label(compoElements, SWT.NONE);
		Canvas businessCanvas = new Canvas(compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(25, 2);
		fd.bottom = new FormAttachment(40, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		businessCanvas.setLayoutData(fd);
		businessCanvas.setBackground(BUSINESS_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(businessCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(businessCanvas, 2, SWT.LEFT);
		businessLabel.setLayoutData(fd);
		businessLabel.setBackground(BUSINESS_COLOR);
		businessLabel.setText("Business");
		
		Label applicationLabel = new Label(compoElements, SWT.NONE);
		Canvas applicationCanvas = new Canvas(compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(40, 2);
		fd.bottom = new FormAttachment(55, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		applicationCanvas.setLayoutData(fd);
		applicationCanvas.setBackground(APPLICATION_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(applicationCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(applicationCanvas, 2, SWT.LEFT);
		applicationLabel.setLayoutData(fd);
		applicationLabel.setBackground(APPLICATION_COLOR);
		applicationLabel.setText("Application");
		
		Label technologyLabel = new Label(compoElements, SWT.NONE);
		Canvas technologyCanvas = new Canvas(compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(55, 2);
		fd.bottom = new FormAttachment(70, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		technologyCanvas.setLayoutData(fd);
		technologyCanvas.setBackground(TECHNOLOGY_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(technologyCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(technologyCanvas, 2, SWT.LEFT);
		technologyLabel.setLayoutData(fd);
		technologyLabel.setBackground(TECHNOLOGY_COLOR);
		technologyLabel.setText("Technology");
		 
		Label physicalLabel = new Label(compoElements, SWT.NONE);
		Canvas physicalCanvas = new Canvas(compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(70, 2);
		fd.bottom = new FormAttachment(85, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		physicalCanvas.setLayoutData(fd);
		physicalCanvas.setBackground(PHYSICAL_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(physicalCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(physicalCanvas, 2, SWT.LEFT);
		physicalLabel.setLayoutData(fd);
		physicalLabel.setBackground(PHYSICAL_COLOR);
		physicalLabel.setText("Physical");
		
		Label implementationLabel = new Label(compoElements, SWT.NONE);
		Canvas implementationCanvas = new Canvas(compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(85, 2);
		fd.bottom = new FormAttachment(100);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		implementationCanvas.setLayoutData(fd);
		implementationCanvas.setBackground(IMPLEMENTATION_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(implementationCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(implementationCanvas, 2, SWT.LEFT);
		implementationLabel.setLayoutData(fd);
		implementationLabel.setBackground(IMPLEMENTATION_COLOR);
		implementationLabel.setText("Implementation");
		
			// strategy + active
		fd = new FormData();
		fd.top = new FormAttachment(strategyCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(strategyCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(activeCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(activeCanvas, 0, SWT.RIGHT);
		strategyActiveCompo.setLayoutData(fd);
		RowLayout rd = new RowLayout(SWT.HORIZONTAL);
		rd.center = true;
		rd.fill = true;
		rd.justify = true;
		rd.wrap = true;
		rd.marginBottom = 5;
		rd.marginTop = 5;
		rd.marginLeft = 5;
		rd.marginRight = 5;
		rd.spacing = 0;
		strategyActiveCompo.setLayout(rd);
		
			// strategy + behavior
		fd = new FormData();
		fd.top = new FormAttachment(strategyCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(strategyCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(behaviorCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(behaviorCanvas, 0, SWT.RIGHT);
		strategyBehaviorCompo.setLayoutData(fd);
		strategyBehaviorCompo.setLayout(rd);
		
			// strategy + passive
		fd = new FormData();
		fd.top = new FormAttachment(strategyCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(strategyCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(passiveCanvas, 0, SWT.RIGHT);
		strategyPassiveCompo.setLayoutData(fd);
		strategyPassiveCompo.setLayout(rd);	
		
			// business + active
		fd = new FormData();
		fd.top = new FormAttachment(businessCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(businessCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(activeCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(activeCanvas, 0, SWT.RIGHT);
		businessActiveCompo.setLayoutData(fd);
		businessActiveCompo.setLayout(rd);
		
			// business + behavior
		fd = new FormData();
		fd.top = new FormAttachment(businessCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(businessCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(behaviorCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(behaviorCanvas, 0, SWT.RIGHT);
		businessBehaviorCompo.setLayoutData(fd);
		businessBehaviorCompo.setLayout(rd);
		
			// Business + passive
		fd = new FormData();
		fd.top = new FormAttachment(businessCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(businessCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(passiveCanvas, 0, SWT.RIGHT);
		businessPassiveCompo.setLayoutData(fd);
		businessPassiveCompo.setLayout(rd);
		

		
			// application + active
		fd = new FormData();
		fd.top = new FormAttachment(applicationCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(applicationCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(activeCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(activeCanvas, 0, SWT.RIGHT);
		applicationActiveCompo.setLayoutData(fd);
		applicationActiveCompo.setLayout(rd);

		
			// application + behavior
		fd = new FormData();
		fd.top = new FormAttachment(applicationCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(applicationCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(behaviorCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(behaviorCanvas, 0, SWT.RIGHT);
		applicationBehaviorCompo.setLayoutData(fd);
		applicationBehaviorCompo.setLayout(rd);
		
			// application + passive
		fd = new FormData();
		fd.top = new FormAttachment(applicationCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(applicationCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(passiveCanvas, 0, SWT.RIGHT);
		applicationPassiveCompo.setLayoutData(fd);
		applicationPassiveCompo.setLayout(rd);
		
		
			// technology + active
		fd = new FormData();
		fd.top = new FormAttachment(technologyCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(technologyCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(activeCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(activeCanvas, 0, SWT.RIGHT);
		technologyActiveCompo.setLayoutData(fd);
		technologyActiveCompo.setLayout(rd);
		 
			// technology + behavior
		fd = new FormData();
		fd.top = new FormAttachment(technologyCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(technologyCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(behaviorCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(behaviorCanvas, 0, SWT.RIGHT);
		technologyBehaviorCompo.setLayoutData(fd);
		technologyBehaviorCompo.setLayout(rd);
		
			// technology + passive
		fd = new FormData();
		fd.top = new FormAttachment(technologyCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(technologyCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(passiveCanvas, 0, SWT.RIGHT);
		technologyPassiveCompo.setLayoutData(fd);
		technologyPassiveCompo.setLayout(rd);
		
			// physical + active
		fd = new FormData();
		fd.top = new FormAttachment(physicalCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(physicalCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(activeCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(activeCanvas, 0, SWT.RIGHT);
		physicalActiveCompo.setLayoutData(fd);
		physicalActiveCompo.setLayout(rd);
		 
			// physical + behavior
		fd = new FormData();
		fd.top = new FormAttachment(physicalCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(physicalCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(behaviorCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(behaviorCanvas, 0, SWT.RIGHT);
		physicalBehaviorCompo.setLayoutData(fd);
		physicalBehaviorCompo.setLayout(rd);
		
			// physical + passive
		fd = new FormData();
		fd.top = new FormAttachment(physicalCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(physicalCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(passiveCanvas, 0, SWT.RIGHT);
		physicalPassive.setLayoutData(fd);
		physicalPassive.setLayout(rd);
		 
			// implementation
		fd = new FormData();
		fd.top = new FormAttachment(implementationCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(implementationCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(passiveCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(activeCanvas, 0, SWT.RIGHT);
		implementationCompo.setLayoutData(fd);
		rd = new RowLayout(SWT.HORIZONTAL);
		rd.center = true;
		rd.fill = true;
		rd.justify = true;
		rd.wrap = true;
		rd.marginBottom = 5;
		rd.marginTop = 7;
		rd.marginLeft = 5;
		rd.marginRight = 5;
		rd.spacing = 0;
		implementationCompo.setLayout(rd);
		
			// motivation
		fd = new FormData();
		fd.top = new FormAttachment(motivationCanvas, 20, SWT.TOP);
		fd.bottom = new FormAttachment(motivationCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(motivationCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(motivationCanvas, 0, SWT.RIGHT);
		motivationCompo.setLayoutData(fd);
		rd = new RowLayout(SWT.VERTICAL);
		rd.center = true;
		rd.fill = true;
		rd.justify = true;
		rd.wrap = true;
		rd.marginBottom = 5;
		rd.marginTop = 5;
		rd.marginLeft = 20;
		rd.marginRight = 5;
		rd.spacing = 0;
		motivationCompo.setLayout(rd);
	}
	
	/*
	private void createCompoComposites() {
		compoContainers = new Composite(grpFilter, SWT.NONE);
		compoContainers.setBackground(GROUP_BACKGROUND_COLOR);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		compoContainers.setLayoutData(fd);
		compoContainers.setLayout(new FormLayout());
		
		Label sorryLabel = new Label(compoContainers, SWT.NONE);
		sorryLabel.setBackground(GROUP_BACKGROUND_COLOR);
		sorryLabel.setText("Not yet implemented, sorry ...");
		fd = new FormData();
		fd.top = new FormAttachment(35);
		fd.left = new FormAttachment(0, 30);
		sorryLabel.setLayoutData(fd);
	}
	*/
	
	/*
	private void createCompoFolders() {
		compoFolders = new Composite(grpFilter, SWT.NONE);
		compoFolders.setBackground(GROUP_BACKGROUND_COLOR);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		compoFolders.setLayoutData(fd);
		compoFolders.setLayout(new FormLayout());
		
		Label folderTypeLabel = new Label(compoFolders, SWT.NONE);
		folderTypeLabel.setBackground(GROUP_BACKGROUND_COLOR);
		folderTypeLabel.setText("Select folders type to display :");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 30);
		folderTypeLabel.setLayoutData(fd);
		
		strategyFolders = new Button(compoFolders, SWT.CHECK);
		strategyFolders.setBackground(GROUP_BACKGROUND_COLOR);
		strategyFolders.setText("Strategy");
		fd = new FormData();
		fd.top = new FormAttachment(folderTypeLabel, 5);
		fd.left = new FormAttachment(folderTypeLabel, 20, SWT.LEFT);
		strategyFolders.setLayoutData(fd);
		strategyFolders.addListener(SWT.MouseUp, getFoldersListener);
		
		businessFolders = new Button(compoFolders, SWT.CHECK);
		businessFolders.setBackground(GROUP_BACKGROUND_COLOR);
		businessFolders.setText("Business");
		fd = new FormData();
		fd.top = new FormAttachment(strategyFolders, 5);
		fd.left = new FormAttachment(strategyFolders, 0, SWT.LEFT);
		businessFolders.setLayoutData(fd);
		businessFolders.addListener(SWT.MouseUp, getFoldersListener);
		
		applicationFolders = new Button(compoFolders, SWT.CHECK);
		applicationFolders.setBackground(GROUP_BACKGROUND_COLOR);
		applicationFolders.setText("Application");
		fd = new FormData();
		fd.top = new FormAttachment(businessFolders, 5);
		fd.left = new FormAttachment(strategyFolders, 0, SWT.LEFT);
		applicationFolders.setLayoutData(fd);
		applicationFolders.addListener(SWT.MouseUp, getFoldersListener);
		
		technologyFolders = new Button(compoFolders, SWT.CHECK);
		technologyFolders.setBackground(GROUP_BACKGROUND_COLOR);
		technologyFolders.setText("Technology && Physical");
		fd = new FormData();
		fd.top = new FormAttachment(applicationFolders, 5);
		fd.left = new FormAttachment(strategyFolders, 0, SWT.LEFT);
		technologyFolders.setLayoutData(fd);
		technologyFolders.addListener(SWT.MouseUp, getFoldersListener);
		
		motivationFolders = new Button(compoFolders, SWT.CHECK);
		motivationFolders.setBackground(GROUP_BACKGROUND_COLOR);
		motivationFolders.setText("Motivation");
		fd = new FormData();
		fd.top = new FormAttachment(technologyFolders, 5);
		fd.left = new FormAttachment(strategyFolders, 0, SWT.LEFT);
		motivationFolders.setLayoutData(fd);
		motivationFolders.addListener(SWT.MouseUp, getFoldersListener);
		
		implementationFolders = new Button(compoFolders, SWT.CHECK);
		implementationFolders.setBackground(GROUP_BACKGROUND_COLOR);
		implementationFolders.setText("Implementation & Migration");
		fd = new FormData();
		fd.top = new FormAttachment(motivationFolders, 5);
		fd.left = new FormAttachment(strategyFolders, 0, SWT.LEFT);
		implementationFolders.setLayoutData(fd);
		implementationFolders.addListener(SWT.MouseUp, getFoldersListener);
		
		otherFolders = new Button(compoFolders, SWT.CHECK);
		otherFolders.setBackground(GROUP_BACKGROUND_COLOR);
		otherFolders.setText("Other");
		fd = new FormData();
		fd.top = new FormAttachment(implementationFolders, 5);
		fd.left = new FormAttachment(strategyFolders, 0, SWT.LEFT);
		otherFolders.setLayoutData(fd);
		otherFolders.addListener(SWT.MouseUp, getFoldersListener);
	}
	*/
	
	private void createCompoViews() {
		compoViews = new Composite(grpFilter, SWT.NONE);
		compoViews.setBackground(GROUP_BACKGROUND_COLOR);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		compoViews.setLayoutData(fd);
		compoViews.setLayout(new FormLayout());
		
		Label viewTypeLabel = new Label(compoViews, SWT.NONE);
		viewTypeLabel.setBackground(GROUP_BACKGROUND_COLOR);
		viewTypeLabel.setText("Select views type to display :");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 30);
		viewTypeLabel.setLayoutData(fd);
		
		archimateViews = new Button(compoViews, SWT.CHECK);
		archimateViews.setBackground(GROUP_BACKGROUND_COLOR);
		archimateViews.setText("Archimate views");
		fd = new FormData();
		fd.top = new FormAttachment(viewTypeLabel, 5);
		fd.left = new FormAttachment(viewTypeLabel, 20, SWT.LEFT);
		archimateViews.setLayoutData(fd);
		archimateViews.addListener(SWT.MouseUp, getViewsListener); 
		
		canvasViews = new Button(compoViews, SWT.CHECK);
		canvasViews.setBackground(GROUP_BACKGROUND_COLOR);
		canvasViews.setText("Canvas");
		fd = new FormData();
		fd.top = new FormAttachment(archimateViews, 5);
		fd.left = new FormAttachment(viewTypeLabel, 20, SWT.LEFT);
		canvasViews.setLayoutData(fd);
		canvasViews.addListener(SWT.MouseUp, getViewsListener); 
		
		sketchViews = new Button(compoViews, SWT.CHECK);
		sketchViews.setBackground(GROUP_BACKGROUND_COLOR);
		sketchViews.setText("Sketch views");
		fd = new FormData();
		fd.top = new FormAttachment(canvasViews, 5);
		fd.left = new FormAttachment(viewTypeLabel, 20, SWT.LEFT);
		sketchViews.setLayoutData(fd);
		sketchViews.addListener(SWT.MouseUp, getViewsListener); 
	}
	
	private void createGrpComponents() {
		grpComponent = new Group(compoRightBottom, SWT.NONE);
		grpComponent.setBackground(GROUP_BACKGROUND_COLOR);
		grpComponent.setFont(GROUP_TITLE_FONT);
		grpComponent.setText("Select the component to import : ");
		FormData fd = new FormData();
		fd.top = new FormAttachment(grpFilter, 10);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100);
		grpComponent.setLayoutData(fd);
		grpComponent.setLayout(new FormLayout());
		
		SelectionListener redrawTblComponents = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
	                    if ( database.isConnected() ) {
	                        if ( compoElements.isVisible() )
	                            getElements();
	                        //else if ( compoContainers.isVisible() )
	                        //  getContainers();
	                        //else if ( compoFolders.isVisible() )
	                        //    getFolders();
	                        else if ( compoViews.isVisible() )
	                            getViews();
	                    }
	                } catch (Exception err) {
	                    DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
	                } 
	            }

                @Override
                public void widgetDefaultSelected(SelectionEvent event) {
                    widgetSelected(event);                    
                }
	        };
	        
        hideAlreadyInModel = new Button(grpComponent, SWT.CHECK);
        hideAlreadyInModel.setBackground(GROUP_BACKGROUND_COLOR);
        hideAlreadyInModel.setText("Hide components already in model");
        hideAlreadyInModel.setSelection(true);
        hideAlreadyInModel.addSelectionListener(redrawTblComponents);
        
        hideOption = new Button(grpComponent, SWT.CHECK);
        hideOption.setBackground(GROUP_BACKGROUND_COLOR);
        hideOption.setText("Hide components with empty names");
        hideOption.setSelection(true);
        hideOption.addSelectionListener(redrawTblComponents);

		tblComponents = new Table(grpComponent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		tblComponents.setLinesVisible(true);
		tblComponents.setBackground(GROUP_BACKGROUND_COLOR);
		tblComponents.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				btnDoAction.setEnabled(true);		// as soon a component is selected, we can import it
			}
		});
		tblComponents.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				if ( btnDoAction.getEnabled() )
					btnDoAction.notifyListeners(SWT.Selection, new Event());
			}
		});
		
        fd = new FormData();
        fd.bottom = new FormAttachment(100, -5);
        fd.left = new FormAttachment(50, 5);
        hideOption.setLayoutData(fd);
		
		fd = new FormData();
		fd.bottom = new FormAttachment(100, -5);
		fd.right = new FormAttachment(50, -5);
		hideAlreadyInModel.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(30);
		fd.right = new FormAttachment(70);
		fd.bottom = new FormAttachment(hideAlreadyInModel, -5);
		tblComponents.setLayoutData(fd);
	}
	
	private void getElements() throws Exception {
		compoElements.setVisible(true);
		//compoContainers.setVisible(false);
		//compoFolders.setVisible(false);
		compoViews.setVisible(false);
		
		if ( logger.isTraceEnabled() ) logger.trace("emptying tblComponents");
		tblComponents.removeAll();
		
		StringBuilder inList = new StringBuilder();
		ArrayList<String> classList = new ArrayList<String>();
		for (ComponentLabel label: allElementLabels) {
			if ( label.isSelected ) {
				inList.append(inList.length()==0 ? "?" : ", ?");
				classList.add(label.getElementClassname());
			}
		}
		
	    hideOption.setText("Hide components with empty names");
	    String addOn = "";
	    if ( hideOption.getSelection() )
	        addOn = " AND name <> ''";
		
		if ( inList.length() != 0 ) {
			ResultSet result;
			
			if ( filterName.getText().length() == 0 )
				result = database.select("SELECT id, class, name FROM elements WHERE class IN ("+inList.toString()+")"+addOn, classList);
			else {
				if ( ignoreCase.getSelection() )
					result = database.select("SELECT id, class, name FROM elements WHERE class IN ("+inList.toString()+") AND UPPER(name) like ?"+addOn, classList, "%"+filterName.getText().toUpperCase()+"%");
				else
					result = database.select("SELECT id, class, name FROM elements WHERE class IN ("+inList.toString()+") AND name like ?"+addOn, classList, "%"+filterName.getText()+"%");
			}
			
			while (result.next()) {
			    if ( !hideAlreadyInModel.getSelection() || (importedModel.getAllElements().get(result.getString("id"))==null))
			        createTableItem(tblComponents, result.getString("id"), result.getString("Class"), result.getString("name"));
			}
			result.close();
		}
		
		btnDoAction.setEnabled(false);
	}
	
	/*
	private void getFolders() throws Exception {
		compoElements.setVisible(false);
		compoContainers.setVisible(false);
		compoFolders.setVisible(true);
		compoViews.setVisible(false);
		
		logger.if ( logger.isTraceEnabled() ) logger.trace("emptying tblComponents");
		tblComponents.removeAll();
		
		StringBuilder inList = new StringBuilder();
		ArrayList<String> typeList = new ArrayList<String>();
		if ( strategyFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.STRATEGY_VALUE));
		}
		if ( businessFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.BUSINESS_VALUE));
		}
		if ( applicationFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.APPLICATION_VALUE));
		}
		if ( technologyFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.TECHNOLOGY_VALUE));
		}
		if ( motivationFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.MOTIVATION_VALUE));
		}
		if ( implementationFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.IMPLEMENTATION_MIGRATION_VALUE));
		}
		if ( otherFolders.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			typeList.add(String.valueOf(FolderType.OTHER_VALUE));
		}
		
		hideOption.setText("Hide top level folders");
		String addOn = "";
		if ( hideOption.getSelection() )
		    addOn = " AND type = 0";
		
		if ( inList.length() != 0 ) {
			ResultSet result;
			
			if ( filterName.getText().length() == 0 )
				result = database.select("SELECT id, name FROM folders WHERE root_type IN ("+inList.toString()+")"+addOn, typeList);
			else
				result = database.select("SELECT id, name FROM folders WHERE root_type IN ("+inList.toString()+") AND name like ?"+addOn, typeList, "%"+filterName.getText()+"%");
			
			while (result.next()) {
			    if ( !hideAlreadyInModel.getSelection() || (importedModel.getAllFolders().get(result.getString("id"))==null))
			        createTableItem(tblComponents, result.getString("id"), "Folder", result.getString("name"));
			}
			result.close();
		}
		
		btnDoAction.setEnabled(false);
	}
	*/
	
	private void getViews() throws Exception {
		compoElements.setVisible(false);
		//compoContainers.setVisible(false);
		//compoFolders.setVisible(false);
		compoViews.setVisible(true);
		
		if ( logger.isTraceEnabled() ) logger.trace("emptying tblComponents");
		tblComponents.removeAll();
		
		StringBuilder inList = new StringBuilder();
		ArrayList<String> classList = new ArrayList<String>();
		if ( archimateViews.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			classList.add("ArchimateDiagramModel");
		}
		if ( canvasViews.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			classList.add("CanvasModel");
		}
		if ( sketchViews.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			classList.add("SketchModel");
		}
		
		hideOption.setText("Hide default views");
		String addOn = "";
		if ( hideOption.getSelection() )
		    addOn = " AND name <> 'Default View'";
		
		if ( inList.length() != 0 ) {
			ResultSet result;
			
			if ( filterName.getText().length() == 0 )
				result = database.select("SELECT id, class, name FROM views WHERE class IN ("+inList.toString()+")"+addOn, classList);
			else
				result = database.select("SELECT id, class, name FROM views WHERE class IN ("+inList.toString()+") AND name like ?"+addOn, classList, "%"+filterName.getText()+"%");
			
			while (result.next()) {
                if ( !hideAlreadyInModel.getSelection() || (importedModel.getAllViews().get(result.getString("id"))==null))
                    createTableItem(tblComponents, result.getString("id"), result.getString("Class"), result.getString("name"));
			}
			result.close();
		}
		
		btnDoAction.setEnabled(false);
	}
	
	
	
	private void createTableItem(Table table, String id, String className, String text) {
	    if ( logger.isTraceEnabled() ) logger.trace("adding "+text+"("+className+") to tblComponents");
		TableItem item = new TableItem(table, SWT.NONE);
		item.setData("id", id);
		item.setText("   "+text);
		if ( className.toUpperCase().startsWith("CANVAS") )
			item.setImage(DBCanvasFactory.getImage(className));
		else
			item.setImage(DBArchimateFactory.getImage(className));
	}

	
	private void doImport() throws Exception {
	    if ( logger.isTraceEnabled() ) logger.trace("tblComponents has got "+tblComponents.getItemCount()+" items");
		if ( getOptionValue() )
			logger.trace("Importing component "+tblComponents.getSelection()[0].getText());
		else
			logger.trace("Importing a copy of component "+tblComponents.getSelection()[0].getText());
		
		String id = (String)tblComponents.getSelection()[0].getData("id");
		
		if ( compoElements.getVisible() )
			database.importElementFromId(importedModel, id, !getOptionValue(), true);
		//else if ( compoContainers.getVisible() )
		//	database.importContainerFromId(importedModel, id, !getOptionValue());
		//else if ( compoFolders.getVisible() )
		//	database.importFolder(importedModel, id, !getOptionValue());
		else if ( compoViews.getVisible() )
			database.importViewFromId(importedModel, id, !getOptionValue());
		
		btnClose.setText("Close");		// to remove "cancel by user" trace message
		close();
	}
	
	private class ComponentLabel extends Label {
		public boolean isSelected = false;
		
		ComponentLabel(Composite parent, String toolTip) {
			super(parent, SWT.NONE);
			setSize(100,  100);
			setToolTipText(toolTip);
			setImage(DBArchimateFactory.getImage(getElementClassname()));
			addPaintListener(redraw);
			addListener(SWT.MouseUp, getElementsListener); 
		}
		
		private PaintListener redraw = new PaintListener() {
            @Override
            public void paintControl(PaintEvent event)
            {
                 if ( isSelected )
                	 setBackground(GREY_COLOR);
                	 //event.gc.drawRoundRectangle(0, 0, 16, 16, 2, 2);
                 else
                	 setBackground(null);
            }
        };
        
        public String getElementClassname() {
        	return getToolTipText().replaceAll(" ",  "");
        }
        
        @Override
        protected void checkSubclass() {
        }
	}
	
    private Listener getElementsListener = new Listener() {
    	@Override
    	public void handleEvent(Event event) {
    		ComponentLabel label = (ComponentLabel)event.widget;
    		label.isSelected = !label.isSelected;
    		label.redraw();
    		try {
				getElements();
			} catch (Exception err) {
			    DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
			}
    	}
    };
    
    /*
    private Listener getFoldersListener = new Listener() {
    	@Override
    	public void handleEvent(Event event) {
    		try {
				getFolders();
			} catch (Exception err) {
				DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
			}
    	}
    };
    */
	
    private Listener getViewsListener = new Listener() {
    	@Override
    	public void handleEvent(Event event) {
    		try {
				getViews();
			} catch (Exception err) {
			    DBGui.popup(Level.ERROR, "An exception has been raised during the import.", err);
			}
    	}
    };
}
