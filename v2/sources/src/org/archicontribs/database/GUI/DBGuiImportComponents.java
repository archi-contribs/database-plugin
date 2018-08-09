/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.commands.DBImportElementFromIdCommand;
import org.archicontribs.database.model.commands.DBImportFolderFromIdCommand;
import org.archicontribs.database.model.commands.DBImportRelationshipFromIdCommand;
import org.archicontribs.database.model.commands.DBImportViewFromIdCommand;
import org.archicontribs.database.model.commands.DBResolveConnectionsCommand;
import org.archicontribs.database.model.commands.DBResolveRelationshipsCommand;
import org.archicontribs.database.model.commands.IDBImportFromIdCommand;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.services.ViewManager;
import com.archimatetool.editor.views.tree.ITreeModelView;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IFolder;

public class DBGuiImportComponents extends DBGui {
	@SuppressWarnings("hiding")
	protected static final DBLogger logger = new DBLogger(DBGuiImportComponents.class);

	protected DBArchimateModel importedModel;
	protected IArchimateDiagramModel selectedView;
	protected IFolder selectedFolder;

	private Group grpFilter;
	private Group grpComponent;

	Composite compoModels;
	Composite compoElements;
	Button radioOptionModel;
	Button radioOptionElement;
	Button radioOptionView;
	private Text filterName;
	private Button hideOption;             // to hide empty names for elements and relationships, top level folders and default views
	private Button hideAlreadyInModel;

	DBDatabaseImportConnection importConnection = null;

	//private Composite compoFolders;
	//private Button strategyFolders;
	//private Button applicationFolders;
	//private Button businessFolders;
	//private Button technologyFolders;
	//private Button otherFolders;
	//private Button motivationFolders;
	//private Button implementationFolders;

	Composite compoViews;
	private Button archimateViews;
	private Button canvasViews;
	private Button sketchViews;

	Label lblComponents;
	Table tblComponents;
	Label lblPreview;


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
	ComponentLabel groupingLabel;
	ComponentLabel locationLabel;
	ComponentLabel junctionLabel;

	private Label lblStrategy;
	private Label lblBusiness;
	private Label lblApplication;
	private Label lblTechnology;
	private Label lblPhysical;
	private Label lblImplementation;
	private Label lblMotivation;

	ComponentLabel[] allElementLabels;

	/**
	 * Creates the GUI to import components
	 * @throws Exception 
	 */
	public DBGuiImportComponents(DBArchimateModel model, IArchimateDiagramModel view, IFolder folder, String title) throws Exception {
		super(title);

		this.includeNeo4j = false;

		setMessage("Counting model's components");
		model.countAllObjects();
		if ( logger.isDebugEnabled() ) logger.debug("The model has got "+model.getAllElements().size()+" elements and "+model.getAllRelationships().size()+" relationships.");
		closeMessage();		

		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for importing a component (plugin version "+DBPlugin.pluginVersion+").");

		// model in which the component should be imported
		this.importedModel = model;

		// if specified, the imported element or relationship will be instantiated as an object or a connection in the view
		this.selectedView = view;

		// if specified, the imported view will be instantiated into the folder (if the root folder type is view)
		this.selectedFolder = folder;

		createAction(ACTION.One, "Choose component");
		setActiveAction(ACTION.One);

		// we show the option in the bottom
		setOption("Import type:",
				"Template mode",     DBPlugin.INSTANCE.getPreferenceStore().getString("defaultImportMode").equals("template"), "The components will be copied except if the \"template\" property's value is \"shared\".",
				"Force shared mode", DBPlugin.INSTANCE.getPreferenceStore().getString("defaultImportMode").equals("shared"),   "The components will be shared across models. All the modifications done on those components will be visible to other models.",
				"Force copy mode",   DBPlugin.INSTANCE.getPreferenceStore().getString("defaultImportMode").equals("copy"),     "A copy of the components will be created. All your modifications will remain private to your model and will not be visible by other models.");

		// We activate the btnDoAction button: if the user select the "Import" button --> call the doImport() method
		setBtnAction("Import", new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				DBGuiImportComponents.this.btnDoAction.setEnabled(false);
				try {
					doImport();
				} catch (Exception err) {
					DBGui.popup(Level.ERROR, "An exception has been raised during import.", err);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});

		// We activate the Eclipse Help framework
		setHelpHref("importComponent.html");

		enableOption();
		createGrpFilter();
		createGrpComponents();

		// we pre-select components depending on the folder selected
		if ( this.selectedFolder == null ) {
			// if the folder and the view are both null, then, the user selected the model
			if ( this.selectedView == null ) {
				this.radioOptionModel.setSelection(true);
				this.radioOptionElement.setSelection(false);
				this.radioOptionView.setSelection(false);
			}
		} else {
			this.radioOptionModel.setSelection(false);
			this.radioOptionElement.setSelection(true);
			this.radioOptionView.setSelection(false);

			switch ( ((IDBMetadata)this.selectedFolder).getDBMetadata().getRootFolderType() ) {
				case FolderType.STRATEGY_VALUE:
					this.lblStrategy.notifyListeners(SWT.MouseUp, null);
					break;

				case FolderType.BUSINESS_VALUE:
					this.lblBusiness.notifyListeners(SWT.MouseUp, null);
					break;

				case FolderType.APPLICATION_VALUE:
					this.lblApplication.notifyListeners(SWT.MouseUp, null);
					break;

				case FolderType.TECHNOLOGY_VALUE:
					this.lblTechnology.notifyListeners(SWT.MouseUp, null);      // the null event indicates that the labels must be selected but the elements list must not be gathered from the database
					this.lblPhysical.notifyListeners(SWT.MouseUp, null);
					break;

				case FolderType.IMPLEMENTATION_MIGRATION_VALUE:
					this.lblImplementation.notifyListeners(SWT.MouseUp, null);
					break;

				case FolderType.MOTIVATION_VALUE:
					this.lblMotivation.notifyListeners(SWT.MouseUp, null);
					break;

				case FolderType.OTHER_VALUE:
					// there is no "other" group so no MouseUp listener. The getElements needs to be called manually. 
					this.locationLabel.setSelected(true);
					this.locationLabel.redraw();

					this.groupingLabel.setSelected(true);
					this.groupingLabel.redraw();

					this.junctionLabel.setSelected(true);
					this.junctionLabel.redraw();
					break;

				case FolderType.DIAGRAMS_VALUE:
					if ( view == null ) {
						// if no view is selected, then we suppose we must import a view in the selected folder
						// if a view is selected, we suppose we must import a component into that view (but we cannot guess which one)
						this.radioOptionModel.setSelection(false);
						this.radioOptionElement.setSelection(false);
						this.radioOptionView.setSelection(true);
					}
					break;

				default:
					break;
			}
		}

		getDatabases(false);
		
		// we reset the location of the imported view objects if any
		DBImportElementFromIdCommand.resetCreatedViewObjectsLocation();
	}

	/**
	 * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
	 */
	@Override
	protected void connectedToDatabase(boolean ignore) {
		this.importConnection = new DBDatabaseImportConnection(getDatabaseConnection());
		this.compoRightBottom.setVisible(true);
		this.compoRightBottom.layout();
		try {
			this.tblComponents.clearAll();
			
			if ( this.radioOptionModel.getSelection() )
				getModels();
			else if ( this.radioOptionElement.getSelection() )
				getElements();
			else if ( this.radioOptionView.getSelection() )
				getViews();
		} catch (Exception err) {
			DBGui.popup(Level.ERROR, "An exception has been raised.", err);
		}
	}

	private void createGrpFilter() {
		this.grpFilter = new Group(this.compoRightBottom, SWT.NONE);
		this.grpFilter.setBackground(GROUP_BACKGROUND_COLOR);
		this.grpFilter.setFont(GROUP_TITLE_FONT);
		this.grpFilter.setText("Filter: ");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(50, -5);
		this.grpFilter.setLayoutData(fd);
		this.grpFilter.setLayout(new FormLayout());

		Label chooseCategory = new Label(this.grpFilter, SWT.NONE);
		chooseCategory.setBackground(GROUP_BACKGROUND_COLOR);
		chooseCategory.setFont(BOLD_FONT);
		chooseCategory.setText("Category:");
		fd = new FormData();
		fd.top = new FormAttachment(0, 20);
		fd.left = new FormAttachment(0, 10);
		chooseCategory.setLayoutData(fd);

		this.radioOptionModel = new Button(this.grpFilter, SWT.RADIO);
		this.radioOptionModel.setBackground(GROUP_BACKGROUND_COLOR);
		this.radioOptionModel.setText("Models");
		this.radioOptionModel.setSelection(false);
		this.radioOptionModel.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if ( DBGuiImportComponents.this.radioOptionModel.getSelection() ) {
					try {
						getModels();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(chooseCategory, 5);
		fd.left = new FormAttachment(0, 20);
		this.radioOptionModel.setLayoutData(fd);

		this.radioOptionElement = new Button(this.grpFilter, SWT.RADIO);
		this.radioOptionElement.setBackground(GROUP_BACKGROUND_COLOR);
		this.radioOptionElement.setText("Elements");
		this.radioOptionElement.setSelection(true);
		this.radioOptionElement.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if ( DBGuiImportComponents.this.radioOptionElement.getSelection() ) {
					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(this.radioOptionModel, 5);
		fd.left = new FormAttachment(0, 20);
		this.radioOptionElement.setLayoutData(fd);

		this.radioOptionView = new Button(this.grpFilter, SWT.RADIO);
		this.radioOptionView.setBackground(GROUP_BACKGROUND_COLOR);
		this.radioOptionView.setText("Views");
		this.radioOptionView.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if ( DBGuiImportComponents.this.radioOptionView.getSelection() ) {
					try {
						getViews();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(this.radioOptionElement, 5);
		fd.left = new FormAttachment(0, 20);
		this.radioOptionView.setLayoutData(fd);

		Label chooseName = new Label(this.grpFilter, SWT.NONE);
		chooseName.setBackground(GROUP_BACKGROUND_COLOR);
		chooseName.setFont(BOLD_FONT);
		chooseName.setText("Name filter:");
		fd = new FormData();
		fd.top = new FormAttachment(this.radioOptionView, 10);
		fd.left = new FormAttachment(0, 10);
		chooseName.setLayoutData(fd);

		this.filterName = new Text(this.grpFilter, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(chooseName, 5);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(0, 125);
		this.filterName.setLayoutData(fd);
		this.filterName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				try {
					if ( DBGuiImportComponents.this.importConnection.isConnected() ) {
						if ( DBGuiImportComponents.this.radioOptionModel.getSelection() )
							getModels();
						if ( DBGuiImportComponents.this.radioOptionElement.getSelection() )
							getElements();
						//else if ( compoFolders.isVisible() )
						//	getFolders();
						else if ( DBGuiImportComponents.this.radioOptionView.getSelection () )
							getViews();
					}
				} catch (Exception err) {
					DBGui.popup(Level.ERROR, "An exception has been raised.", err);
				} 
			}
		});

		createCompoModels();
		createCompoElements();
		//createCompoComposites();
		//createCompoFolders();
		createCompoViews();
	}
	
	private void createCompoModels() {
		this.compoModels = new Composite(this.grpFilter, SWT.NONE);
		this.compoModels.setBackground(GROUP_BACKGROUND_COLOR);
		this.compoModels.setVisible(false);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		this.compoModels.setLayoutData(fd);
		this.compoModels.setLayout(new FormLayout());
		
		Label title = new Label(this.compoModels, SWT.NONE);
		title.setText("Please select a model to merge it to your current model ...");
		title.setBackground(GROUP_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		title.setLayoutData(fd);
	}

	private void createCompoElements() {		
		this.compoElements = new Composite(this.grpFilter, SWT.NONE);
		this.compoElements.setBackground(GROUP_BACKGROUND_COLOR);
		this.compoElements.setVisible(false);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		this.compoElements.setLayoutData(fd);
		this.compoElements.setLayout(new FormLayout());

		Label title = new Label(this.compoElements, SWT.CENTER);
		title.setText("You may click on a label or an icon to (un)select the corresponding classes ...");
		title.setBackground(GROUP_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		title.setLayoutData(fd);

		Composite strategyActiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite strategyBehaviorCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite strategyPassiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT );

		Composite businessActiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite businessBehaviorCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite businessPassiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT );

		Composite applicationActiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite applicationBehaviorCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite applicationPassiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);

		Composite technologyActiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite technologyBehaviorCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite technologyPassiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);

		Composite physicalActiveCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite physicalBehaviorCompo = new Composite(this.compoElements, SWT.TRANSPARENT);
		Composite physicalPassive = new Composite(this.compoElements, SWT.TRANSPARENT);

		Composite implementationCompo = new Composite(this.compoElements, SWT.TRANSPARENT);


		Composite motivationCompo = new Composite(this.compoElements, SWT.TRANSPARENT);

		Composite otherCompo = new Composite(this.compoElements, SWT.TRANSPARENT);

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Strategy layer
		// Passive
		// Behavior
		this.capabilityLabel = new ComponentLabel(strategyBehaviorCompo,  "Capability");
		this.courseOfActionLabel = new ComponentLabel(strategyBehaviorCompo,  "Course Of Action");
		// Active
		this.resourceLabel = new ComponentLabel(strategyActiveCompo, "Resource");

		// Business layer
		// Passive
		this.productLabel = new ComponentLabel(businessPassiveCompo, "Product");
		// Behavior
		this.businessProcessLabel = new ComponentLabel(businessBehaviorCompo, "Business Process");
		this.businessFunctionLabel = new ComponentLabel(businessBehaviorCompo, "Business Function");
		this.businessInteractionLabel = new ComponentLabel(businessBehaviorCompo, "Business Interaction");
		this.businessEventLabel = new ComponentLabel(businessBehaviorCompo, "Business Event");
		this.businessServiceLabel = new ComponentLabel(businessBehaviorCompo, "Business Service");
		this.businessObjectLabel = new ComponentLabel(businessBehaviorCompo, "Business Object");
		this.contractLabel = new ComponentLabel(businessBehaviorCompo, "Contract");
		this.representationLabel = new ComponentLabel(businessBehaviorCompo, "Representation");
		// Active
		this.businessActorLabel = new ComponentLabel(businessActiveCompo, "Business Actor");
		this.businessRoleLabel = new ComponentLabel(businessActiveCompo, "Business Role");
		this.businessCollaborationLabel = new ComponentLabel(businessActiveCompo, "Business Collaboration");
		this.businessInterfaceLabel = new ComponentLabel(businessActiveCompo, "Business Interface");

		// Application layer
		//Passive
		this.dataObjectLabel = new ComponentLabel(applicationPassiveCompo, "Data Object");
		//Behavior
		this.applicationFunctionLabel = new ComponentLabel(applicationBehaviorCompo, "Application Function");
		this.applicationInteractionLabel = new ComponentLabel(applicationBehaviorCompo, "Application Interaction");
		this.applicationEventLabel = new ComponentLabel(applicationBehaviorCompo, "Application Event");
		this.applicationServiceLabel = new ComponentLabel(applicationBehaviorCompo, "Application Service");
		this.applicationProcessLabel = new ComponentLabel(applicationBehaviorCompo, "Application Process");
		//	Active		
		this.applicationComponentLabel = new ComponentLabel(applicationActiveCompo, "Application Component");
		this.applicationCollaborationLabel = new ComponentLabel(applicationActiveCompo, "Application Collaboration");
		this.applicationInterfaceLabel = new ComponentLabel(applicationActiveCompo, "Application Interface");

		// Technology layer
		// Passive
		this.artifactLabel = new ComponentLabel(technologyPassiveCompo, "Artifact");
		// Behavior
		this.technologyFunctionLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Function");
		this.technologyProcessLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Process");
		this.technologyInteractionLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Interaction");
		this.technologyEventLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Event");
		this.technologyServiceLabel = new ComponentLabel(technologyBehaviorCompo, "Technology Service");
		// Active
		this.nodeLabel = new ComponentLabel(technologyActiveCompo, "Node");
		this.deviceLabel = new ComponentLabel(technologyActiveCompo, "Device");
		this.systemSoftwareLabel = new ComponentLabel(technologyActiveCompo, "System Software");
		this.technologyCollaborationLabel = new ComponentLabel(technologyActiveCompo, "Technology Collaboration");
		this.technologyInterfaceLabel = new ComponentLabel(technologyActiveCompo, "Technology Interface");
		this.pathLabel = new ComponentLabel(technologyActiveCompo, "Path");
		this.communicationNetworkLabel = new ComponentLabel(technologyActiveCompo, "Communication Network");

		// Physical layer
		// Passive
		// Behavior
		this.materialLabel = new ComponentLabel(physicalBehaviorCompo, "Material");
		// Active
		this.equipmentLabel = new ComponentLabel(physicalActiveCompo, "Equipment");
		this.facilityLabel = new ComponentLabel(physicalActiveCompo, "Facility");
		this.distributionNetworkLabel = new ComponentLabel(physicalActiveCompo, "Distribution Network");

		// Implementation layer
		this.workpackageLabel = new ComponentLabel(implementationCompo, "Work Package");
		this.deliverableLabel = new ComponentLabel(implementationCompo, "Deliverable");
		this.implementationEventLabel = new ComponentLabel(implementationCompo, "Implementation Event");
		this.plateauLabel = new ComponentLabel(implementationCompo, "Plateau");
		this.gapLabel = new ComponentLabel(implementationCompo, "Gap");

		// Motivation layer
		this.stakeholderLabel = new ComponentLabel(motivationCompo, "Stakeholder");
		this.driverLabel = new ComponentLabel(motivationCompo, "Driver");
		this.assessmentLabel = new ComponentLabel(motivationCompo, "Assessment");
		this.goalLabel = new ComponentLabel(motivationCompo, "Goal");
		this.outcomeLabel = new ComponentLabel(motivationCompo, "Outcome");
		this.principleLabel = new ComponentLabel(motivationCompo, "Principle");
		this.requirementLabel = new ComponentLabel(motivationCompo, "Requirement");
		this.constaintLabel = new ComponentLabel(motivationCompo, "Constraint");
		this.smeaningLabel = new ComponentLabel(motivationCompo, "Meaning");
		this.valueLabel = new ComponentLabel(motivationCompo, "Value");

		// Containers !!!
		//
		this.groupingLabel = new ComponentLabel(otherCompo, "Grouping");
		this.locationLabel = new ComponentLabel(otherCompo, "Location");
		this.junctionLabel = new ComponentLabel(otherCompo, "Junction");

		this.allElementLabels = new ComponentLabel[]{ this.resourceLabel, this.capabilityLabel, this.courseOfActionLabel, this.applicationComponentLabel, this.applicationCollaborationLabel, this.applicationInterfaceLabel, this.applicationFunctionLabel, this.applicationInteractionLabel, this.applicationEventLabel, this.applicationServiceLabel, this.dataObjectLabel, this.applicationProcessLabel, this.businessActorLabel, this.businessRoleLabel, this.businessCollaborationLabel, this.businessInterfaceLabel, this.businessProcessLabel, this.businessFunctionLabel, this.businessInteractionLabel, this.businessEventLabel, this.businessServiceLabel, this.businessObjectLabel, this.contractLabel, this.representationLabel, this.nodeLabel, this.deviceLabel, this.systemSoftwareLabel, this.technologyCollaborationLabel, this.technologyInterfaceLabel, this.pathLabel, this.communicationNetworkLabel, this.technologyFunctionLabel, this.technologyProcessLabel, this.technologyInteractionLabel, this.technologyEventLabel, this.technologyServiceLabel, this.artifactLabel, this.equipmentLabel, this.facilityLabel, this.distributionNetworkLabel, this.materialLabel, this.workpackageLabel, this.deliverableLabel, this.implementationEventLabel, this.plateauLabel, this.gapLabel, this.stakeholderLabel, this.driverLabel, this.assessmentLabel, this.goalLabel, this.outcomeLabel, this.principleLabel, this.requirementLabel, this.constaintLabel, this.smeaningLabel, this.valueLabel, this.productLabel, this.locationLabel, this.groupingLabel, this.junctionLabel};

		Label passiveLabel = new Label(this.compoElements, SWT.TRANSPARENT | SWT.CENTER);
		Canvas passiveCanvas = new Canvas(this.compoElements, SWT.TRANSPARENT | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(title, 2);
		fd.bottom = new FormAttachment(implementationCompo, 1, SWT.TOP);
		fd.left = new FormAttachment(0, 70);
		fd.right = new FormAttachment(0, 110);
		passiveCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(passiveCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(0, 71);
		fd.right = new FormAttachment(0, 109);
		passiveLabel.setLayoutData(fd);
		passiveLabel.setText("Passive");
		passiveLabel.setBackground(PASSIVE_COLOR);
		passiveLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.productLabel);
				labelList.add(DBGuiImportComponents.this.dataObjectLabel);
				labelList.add(DBGuiImportComponents.this.artifactLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		Label behaviorLabel = new Label(this.compoElements, SWT.TRANSPARENT | SWT.CENTER);
		Canvas behaviorCanvas = new Canvas(this.compoElements, SWT.TRANSPARENT | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(title, 2);
		fd.bottom = new FormAttachment(implementationCompo, 1, SWT.TOP);
		fd.left = new FormAttachment(0, 115);
		fd.right = new FormAttachment(55);
		behaviorCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(behaviorCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(0, 116);
		fd.right = new FormAttachment(55, -1);
		behaviorLabel.setLayoutData(fd);
		behaviorLabel.setText("Behavior");
		behaviorLabel.setBackground(PASSIVE_COLOR);
		behaviorLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.capabilityLabel);
				labelList.add(DBGuiImportComponents.this.courseOfActionLabel);
				labelList.add(DBGuiImportComponents.this.businessProcessLabel);
				labelList.add(DBGuiImportComponents.this.businessFunctionLabel);
				labelList.add(DBGuiImportComponents.this.businessInteractionLabel);
				labelList.add(DBGuiImportComponents.this.businessEventLabel);
				labelList.add(DBGuiImportComponents.this.businessServiceLabel);
				labelList.add(DBGuiImportComponents.this.businessObjectLabel);
				labelList.add(DBGuiImportComponents.this.contractLabel);
				labelList.add(DBGuiImportComponents.this.representationLabel);
				labelList.add(DBGuiImportComponents.this.applicationFunctionLabel);
				labelList.add(DBGuiImportComponents.this.applicationInteractionLabel);
				labelList.add(DBGuiImportComponents.this.applicationEventLabel);
				labelList.add(DBGuiImportComponents.this.applicationServiceLabel);
				labelList.add(DBGuiImportComponents.this.applicationProcessLabel);
				labelList.add(DBGuiImportComponents.this.technologyFunctionLabel);
				labelList.add(DBGuiImportComponents.this.technologyProcessLabel);
				labelList.add(DBGuiImportComponents.this.technologyInteractionLabel);
				labelList.add(DBGuiImportComponents.this.technologyEventLabel);
				labelList.add(DBGuiImportComponents.this.technologyServiceLabel);
				labelList.add(DBGuiImportComponents.this.materialLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		Label activeLabel = new Label(this.compoElements, SWT.TRANSPARENT | SWT.CENTER);
		Canvas activeCanvas = new Canvas(this.compoElements, SWT.TRANSPARENT | SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(title, 2);
		fd.bottom = new FormAttachment(implementationCompo, 1, SWT.TOP);
		fd.left = new FormAttachment(55, 5);
		fd.right = new FormAttachment(100, -65);
		activeCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(activeCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(55, 6);
		fd.right = new FormAttachment(100, -66);
		activeLabel.setLayoutData(fd);
		activeLabel.setText("Active");
		activeLabel.setBackground(PASSIVE_COLOR);
		activeLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.resourceLabel);
				labelList.add(DBGuiImportComponents.this.businessActorLabel);
				labelList.add(DBGuiImportComponents.this.businessRoleLabel);
				labelList.add(DBGuiImportComponents.this.businessCollaborationLabel);
				labelList.add(DBGuiImportComponents.this.businessInterfaceLabel);
				labelList.add(DBGuiImportComponents.this.applicationComponentLabel);
				labelList.add(DBGuiImportComponents.this.applicationCollaborationLabel);
				labelList.add(DBGuiImportComponents.this.applicationInterfaceLabel);
				labelList.add(DBGuiImportComponents.this.nodeLabel);
				labelList.add(DBGuiImportComponents.this.deviceLabel);
				labelList.add(DBGuiImportComponents.this.systemSoftwareLabel);
				labelList.add(DBGuiImportComponents.this.technologyCollaborationLabel);
				labelList.add(DBGuiImportComponents.this.technologyInterfaceLabel);
				labelList.add(DBGuiImportComponents.this.pathLabel);
				labelList.add(DBGuiImportComponents.this.communicationNetworkLabel);
				labelList.add(DBGuiImportComponents.this.equipmentLabel);
				labelList.add(DBGuiImportComponents.this.facilityLabel);
				labelList.add(DBGuiImportComponents.this.distributionNetworkLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		this.lblMotivation = new Label(this.compoElements, SWT.TRANSPARENT | SWT.CENTER);
		Canvas motivationCanvas = new Canvas(this.compoElements, SWT.TRANSPARENT);
		fd = new FormData();
		fd.top = new FormAttachment(title, 2);
		fd.bottom = new FormAttachment(85, -2);
		fd.left = new FormAttachment(100, -60);
		fd.right = new FormAttachment(100);
		motivationCanvas.setLayoutData(fd);
		fd = new FormData();
		fd.top = new FormAttachment(motivationCanvas, 1, SWT.TOP);
		fd.left = new FormAttachment(100, -59);
		fd.right = new FormAttachment(100, -1);
		this.lblMotivation.setLayoutData(fd);
		this.lblMotivation.setText("Motivation");
		this.lblMotivation.setBackground(MOTIVATION_COLOR);
		this.lblMotivation.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.stakeholderLabel);
				labelList.add(DBGuiImportComponents.this.driverLabel);
				labelList.add(DBGuiImportComponents.this.assessmentLabel);
				labelList.add(DBGuiImportComponents.this.goalLabel);
				labelList.add(DBGuiImportComponents.this.outcomeLabel);
				labelList.add(DBGuiImportComponents.this.valueLabel);
				labelList.add(DBGuiImportComponents.this.principleLabel);
				labelList.add(DBGuiImportComponents.this.requirementLabel);
				labelList.add(DBGuiImportComponents.this.constaintLabel);
				labelList.add(DBGuiImportComponents.this.smeaningLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		PaintListener redraw = new PaintListener() {
			@Override
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







		this.lblStrategy = new Label(this.compoElements, SWT.NONE);
		Canvas strategyCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(15, 2);
		fd.bottom = new FormAttachment(29, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		strategyCanvas.setLayoutData(fd);
		strategyCanvas.setBackground(STRATEGY_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(strategyCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(strategyCanvas, 2, SWT.LEFT);
		this.lblStrategy.setLayoutData(fd);
		this.lblStrategy.setBackground(STRATEGY_COLOR);
		this.lblStrategy.setText("Strategy");
		this.lblStrategy.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.capabilityLabel);
				labelList.add(DBGuiImportComponents.this.courseOfActionLabel);
				labelList.add(DBGuiImportComponents.this.resourceLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		this.lblBusiness = new Label(this.compoElements, SWT.NONE);
		Canvas businessCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(29, 2);
		fd.bottom = new FormAttachment(43, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		businessCanvas.setLayoutData(fd);
		businessCanvas.setBackground(BUSINESS_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(businessCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(businessCanvas, 2, SWT.LEFT);
		this.lblBusiness.setLayoutData(fd);
		this.lblBusiness.setBackground(BUSINESS_COLOR);
		this.lblBusiness.setText("Business");
		this.lblBusiness.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.productLabel);
				labelList.add(DBGuiImportComponents.this.businessProcessLabel);
				labelList.add(DBGuiImportComponents.this.businessFunctionLabel);
				labelList.add(DBGuiImportComponents.this.businessInteractionLabel);
				labelList.add(DBGuiImportComponents.this.businessEventLabel);
				labelList.add(DBGuiImportComponents.this.businessServiceLabel);
				labelList.add(DBGuiImportComponents.this.businessObjectLabel);
				labelList.add(DBGuiImportComponents.this.contractLabel);
				labelList.add(DBGuiImportComponents.this.representationLabel);
				labelList.add(DBGuiImportComponents.this.businessActorLabel);
				labelList.add(DBGuiImportComponents.this.businessRoleLabel);
				labelList.add(DBGuiImportComponents.this.businessCollaborationLabel);
				labelList.add(DBGuiImportComponents.this.businessInterfaceLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		this.lblApplication = new Label(this.compoElements, SWT.NONE);
		Canvas applicationCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(43, 2);
		fd.bottom = new FormAttachment(57, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		applicationCanvas.setLayoutData(fd);
		applicationCanvas.setBackground(APPLICATION_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(applicationCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(applicationCanvas, 2, SWT.LEFT);
		this.lblApplication.setLayoutData(fd);
		this.lblApplication.setBackground(APPLICATION_COLOR);
		this.lblApplication.setText("Application");
		this.lblApplication.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.dataObjectLabel);
				labelList.add(DBGuiImportComponents.this.applicationFunctionLabel);
				labelList.add(DBGuiImportComponents.this.applicationInteractionLabel);
				labelList.add(DBGuiImportComponents.this.applicationEventLabel);
				labelList.add(DBGuiImportComponents.this.applicationServiceLabel);
				labelList.add(DBGuiImportComponents.this.applicationProcessLabel);
				labelList.add(DBGuiImportComponents.this.applicationComponentLabel);
				labelList.add(DBGuiImportComponents.this.applicationCollaborationLabel);
				labelList.add(DBGuiImportComponents.this.applicationInterfaceLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		this.lblTechnology = new Label(this.compoElements, SWT.NONE);
		Canvas technologyCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(57, 2);
		fd.bottom = new FormAttachment(71, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		technologyCanvas.setLayoutData(fd);
		technologyCanvas.setBackground(TECHNOLOGY_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(technologyCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(technologyCanvas, 2, SWT.LEFT);
		this.lblTechnology.setLayoutData(fd);
		this.lblTechnology.setBackground(TECHNOLOGY_COLOR);
		this.lblTechnology.setText("Technology");
		this.lblTechnology.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.artifactLabel);
				labelList.add(DBGuiImportComponents.this.technologyFunctionLabel);
				labelList.add(DBGuiImportComponents.this.technologyProcessLabel);
				labelList.add(DBGuiImportComponents.this.technologyInteractionLabel);
				labelList.add(DBGuiImportComponents.this.technologyEventLabel);
				labelList.add(DBGuiImportComponents.this.technologyServiceLabel);
				labelList.add(DBGuiImportComponents.this.nodeLabel);
				labelList.add(DBGuiImportComponents.this.deviceLabel);
				labelList.add(DBGuiImportComponents.this.systemSoftwareLabel);
				labelList.add(DBGuiImportComponents.this.technologyCollaborationLabel);
				labelList.add(DBGuiImportComponents.this.technologyInterfaceLabel);
				labelList.add(DBGuiImportComponents.this.pathLabel);
				labelList.add(DBGuiImportComponents.this.communicationNetworkLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		this.lblPhysical = new Label(this.compoElements, SWT.NONE);
		Canvas physicalCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(71, 2);
		fd.bottom = new FormAttachment(85, -2);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -60);
		physicalCanvas.setLayoutData(fd);
		physicalCanvas.setBackground(PHYSICAL_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(physicalCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(physicalCanvas, 2, SWT.LEFT);
		this.lblPhysical.setLayoutData(fd);
		this.lblPhysical.setBackground(PHYSICAL_COLOR);
		this.lblPhysical.setText("Physical");
		this.lblPhysical.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.materialLabel);
				labelList.add(DBGuiImportComponents.this.equipmentLabel);
				labelList.add(DBGuiImportComponents.this.facilityLabel);
				labelList.add(DBGuiImportComponents.this.distributionNetworkLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		this.lblImplementation = new Label(this.compoElements, SWT.NONE);
		Canvas implementationCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(85, 2);
		fd.bottom = new FormAttachment(100);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100, -65);
		implementationCanvas.setLayoutData(fd);
		implementationCanvas.setBackground(IMPLEMENTATION_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(implementationCanvas, 0, SWT.CENTER);
		fd.left = new FormAttachment(implementationCanvas, 2, SWT.LEFT);
		this.lblImplementation.setLayoutData(fd);
		this.lblImplementation.setBackground(IMPLEMENTATION_COLOR);
		this.lblImplementation.setText("Implementation");
		this.lblImplementation.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				ArrayList<ComponentLabel> labelList = new ArrayList<ComponentLabel>();

				labelList.add(DBGuiImportComponents.this.workpackageLabel);
				labelList.add(DBGuiImportComponents.this.deliverableLabel);
				labelList.add(DBGuiImportComponents.this.implementationEventLabel);
				labelList.add(DBGuiImportComponents.this.plateauLabel);
				labelList.add(DBGuiImportComponents.this.gapLabel);

				boolean areAllSet = true;
				for ( ComponentLabel label: labelList) {
					if ( !label.isSelected()) {
						areAllSet = false;
						break;
					}
				}

				for ( ComponentLabel label: labelList) {
					label.setSelected(!areAllSet);
					label.redraw();
				}

				if ( event != null ) {
					super.mouseUp(event);

					try {
						getElements();
					} catch (Exception err) {
						DBGui.popup(Level.ERROR, "An exception has been raised.", err);
					}
				}
			}
		});

		Canvas otherCanvas = new Canvas(this.compoElements, SWT.NONE);
		fd = new FormData();
		fd.top = new FormAttachment(85, 2);
		fd.bottom = new FormAttachment(100);
		fd.left = new FormAttachment(100, -60);
		fd.right = new FormAttachment(100);
		otherCanvas.setLayoutData(fd);

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

		// other
		fd = new FormData();
		fd.top = new FormAttachment(otherCanvas, 0, SWT.TOP);
		fd.bottom = new FormAttachment(otherCanvas, 0, SWT.BOTTOM);
		fd.left = new FormAttachment(otherCanvas, 0, SWT.LEFT);
		fd.right = new FormAttachment(otherCanvas, 0, SWT.RIGHT);
		otherCompo.setLayoutData(fd);
		rd = new RowLayout(SWT.HORIZONTAL);
		rd.center = true;
		rd.fill = true;
		rd.justify = true;
		rd.wrap = true;
		rd.marginBottom = 5;
		rd.marginTop = 5;
		rd.marginLeft = 5;
		rd.marginRight = 5;
		rd.spacing = 0;
		otherCompo.setLayout(rd);
	}

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
		folderTypeLabel.setText("Select folders type to display:");
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
		this.compoViews = new Composite(this.grpFilter, SWT.NONE);
		this.compoViews.setBackground(GROUP_BACKGROUND_COLOR);
		this.compoViews.setVisible(false);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 135);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -10);
		this.compoViews.setLayoutData(fd);
		this.compoViews.setLayout(new FormLayout());

		Label viewTypeLabel = new Label(this.compoViews, SWT.NONE);
		viewTypeLabel.setBackground(GROUP_BACKGROUND_COLOR);
		viewTypeLabel.setText("Select views type to display:");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 30);
		viewTypeLabel.setLayoutData(fd);

		this.archimateViews = new Button(this.compoViews, SWT.CHECK);
		this.archimateViews.setBackground(GROUP_BACKGROUND_COLOR);
		this.archimateViews.setText("Archimate views");
		this.archimateViews.setSelection(true);
		fd = new FormData();
		fd.top = new FormAttachment(viewTypeLabel, 5);
		fd.left = new FormAttachment(viewTypeLabel, 20, SWT.LEFT);
		this.archimateViews.setLayoutData(fd);
		this.archimateViews.addListener(SWT.MouseUp, this.getViewsListener); 

		this.canvasViews = new Button(this.compoViews, SWT.CHECK);
		this.canvasViews.setBackground(GROUP_BACKGROUND_COLOR);
		this.canvasViews.setText("Canvas");
		fd = new FormData();
		fd.top = new FormAttachment(this.archimateViews, 5);
		fd.left = new FormAttachment(viewTypeLabel, 20, SWT.LEFT);
		this.canvasViews.setLayoutData(fd);
		this.canvasViews.addListener(SWT.MouseUp, this.getViewsListener); 

		this.sketchViews = new Button(this.compoViews, SWT.CHECK);
		this.sketchViews.setBackground(GROUP_BACKGROUND_COLOR);
		this.sketchViews.setText("Sketch views");
		fd = new FormData();
		fd.top = new FormAttachment(this.canvasViews, 5);
		fd.left = new FormAttachment(viewTypeLabel, 20, SWT.LEFT);
		this.sketchViews.setLayoutData(fd);
		this.sketchViews.addListener(SWT.MouseUp, this.getViewsListener); 
	}

	private void createGrpComponents() {
		this.grpComponent = new Group(this.compoRightBottom, SWT.NONE);
		this.grpComponent.setBackground(GROUP_BACKGROUND_COLOR);
		this.grpComponent.setFont(GROUP_TITLE_FONT);
		this.grpComponent.setText("Select the component to import: ");
		FormData fd = new FormData();
		fd.top = new FormAttachment(this.grpFilter, 10);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100);
		this.grpComponent.setLayoutData(fd);
		this.grpComponent.setLayout(new FormLayout());

		this.lblComponents = new Label(this.grpComponent, SWT.CENTER);
		this.lblComponents.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblComponents.setText("0 component matches your criterias.");
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(70, -5);
		this.lblComponents.setLayoutData(fd);

		SelectionListener redrawTblComponents = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					if ( DBGuiImportComponents.this.importConnection.isConnected() ) {
						DBGuiImportComponents.this.tblComponents.clearAll();
						
						if ( DBGuiImportComponents.this.radioOptionModel.getSelection() )
							getModels();
						else if ( DBGuiImportComponents.this.radioOptionElement.getSelection() )
							getElements();
						else if ( DBGuiImportComponents.this.radioOptionView.getSelection() )
							getViews();
					}
				} catch (Exception err) {
					DBGui.popup(Level.ERROR, "An exception has been raised.", err);
				} 
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);                    
			}
		};

		this.hideAlreadyInModel = new Button(this.grpComponent, SWT.CHECK);
		this.hideAlreadyInModel.setBackground(GROUP_BACKGROUND_COLOR);
		this.hideAlreadyInModel.setText("Hide components already in model");
		this.hideAlreadyInModel.setSelection(true);
		this.hideAlreadyInModel.addSelectionListener(redrawTblComponents);

		this.hideOption = new Button(this.grpComponent, SWT.CHECK);
		this.hideOption.setBackground(GROUP_BACKGROUND_COLOR);
		this.hideOption.setText("Hide components with empty names");
		this.hideOption.setSelection(true);
		this.hideOption.addSelectionListener(redrawTblComponents);

		this.tblComponents = new Table(this.grpComponent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.MULTI);
		this.tblComponents.setLinesVisible(true);
		this.tblComponents.setHeaderVisible(true);
		this.tblComponents.setBackground(TABLE_BACKGROUND_COLOR);
		this.tblComponents.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if ( DBGuiImportComponents.this.tblComponents.getItemCount() < 2 ) {
					DBGuiImportComponents.this.lblComponents.setText(DBGuiImportComponents.this.tblComponents.getItemCount()+" component matches your criterias");
				} else {
					DBGuiImportComponents.this.lblComponents.setText(DBGuiImportComponents.this.tblComponents.getItemCount()+" components match your criterias");
				}

				if ( DBGuiImportComponents.this.tblComponents.getSelectionCount() == 0) {
					DBGuiImportComponents.this.lblComponents.setText(DBGuiImportComponents.this.lblComponents.getText()+".");
				} else {
					DBGuiImportComponents.this.lblComponents.setText(DBGuiImportComponents.this.lblComponents.getText()+" ("+DBGuiImportComponents.this.tblComponents.getSelectionCount()+" selected).");

					byte[] screenshot = null;
					if ( DBGuiImportComponents.this.compoViews.isVisible() && (DBGuiImportComponents.this.tblComponents.getSelectionCount() == 1) ) {
						try ( ResultSet resultViewScreenshot = DBGuiImportComponents.this.importConnection.select("SELECT screenshot FROM "+DBGuiImportComponents.this.selectedDatabase.getSchemaPrefix()+"views WHERE id = ? AND version = (SELECT MAX(version) FROM "+DBGuiImportComponents.this.selectedDatabase.getSchemaPrefix()+"views WHERE id = ?)", DBGuiImportComponents.this.tblComponents.getSelection()[0].getData("id"), DBGuiImportComponents.this.tblComponents.getSelection()[0].getData("id")) ) {
							if ( resultViewScreenshot.next() )
								screenshot = resultViewScreenshot.getBytes("screenshot");
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					DBGuiImportComponents.this.lblPreview.setData("screenshot", screenshot);
					DBGuiImportComponents.this.lblPreview.notifyListeners(SWT.Resize, new Event());
				}

				DBGuiImportComponents.this.btnDoAction.setEnabled(true);		// as soon a component is selected, we can import it
			}
		});

		this.tblComponents.addListener(SWT.Dispose, this.tooltipListener);
		this.tblComponents.addListener(SWT.KeyDown, this.tooltipListener);
		this.tblComponents.addListener(SWT.MouseMove, this.tooltipListener);
		this.tblComponents.addListener(SWT.MouseHover, this.tooltipListener);

		this.lblPreview = new Label(this.grpComponent, SWT.BORDER);
		fd = new FormData();
		fd.top = new FormAttachment(this.tblComponents, 0, SWT.TOP);
		fd.left = new FormAttachment(this.tblComponents, 5);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(this.tblComponents, 0, SWT.BOTTOM);
		this.lblPreview.setLayoutData(fd);

		this.lblPreview.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Image screenshot = DBGuiImportComponents.this.lblPreview.getImage();
				if ( screenshot != null ) {
					DBGuiImportComponents.this.lblPreview.setImage(null);
					screenshot.dispose();
				}

				byte[] screenshotBytes = (byte[]) DBGuiImportComponents.this.lblPreview.getData("screenshot");
				if ( screenshotBytes != null ) {
					screenshot = new Image(DBGuiImportComponents.this.lblPreview.getDisplay(), new ByteArrayInputStream(screenshotBytes));
					ImageData data = screenshot.getImageData();

					double scaleWidth = DBGuiImportComponents.this.lblPreview.getSize().x / (double)data.width;
					double scaleHeight = DBGuiImportComponents.this.lblPreview.getSize().y / (double)data.height;
					double scale = (scaleWidth < scaleHeight) ? scaleWidth : scaleHeight;

					int width = (int) (data.width * scale);
					int height = (int) (data.height * scale);

					DBGuiImportComponents.this.lblPreview.setImage(new Image(DBGuiImportComponents.this.lblPreview.getDisplay(), data.scaledTo(width, height)));
					screenshot.dispose();
				}
			}
		});

		TableColumn colName = new TableColumn(this.tblComponents, SWT.NONE);
		colName.setText("Name");
		colName.setWidth(150);
		colName.addListener(SWT.Selection, this.sortListener);
		
		
		TableColumn colDocumentation = new TableColumn(this.tblComponents, SWT.NONE);
		colDocumentation.setText("Documentation");
		colDocumentation.setWidth(300);
		colDocumentation.addListener(SWT.Selection, this.sortListener);

		this.tblComponents.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if ( DBGuiImportComponents.this.btnDoAction.getEnabled() )
					DBGuiImportComponents.this.btnDoAction.notifyListeners(SWT.Selection, new Event());
			}
		});		

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -5);
		fd.left = new FormAttachment(35, 5);
		this.hideOption.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -5);
		fd.right = new FormAttachment(35, -5);
		this.hideAlreadyInModel.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(this.lblComponents, 5);
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(70, -5);
		fd.bottom = new FormAttachment(this.hideAlreadyInModel, -5);
		this.tblComponents.setLayoutData(fd);
	}

	void getModels() throws Exception {
		this.compoModels.setVisible(true);
		this.compoElements.setVisible(false);
		//compoFolders.setVisible(false);
		this.compoViews.setVisible(false);

		this.tblComponents.removeAll();
		Image image = this.lblPreview.getImage();
		if ( image != null ) {
			this.lblPreview.setImage(null);
			image.dispose();
		}
		this.lblPreview.setVisible(false);

		if ( this.selectedDatabase == null )
			return;

		setMessage("Getting list of models from database ...");

		if ( logger.isDebugEnabled() ) logger.debug("Getting models");

		this.hideAlreadyInModel.setVisible(false);
		this.hideOption.setVisible(false);

		this.tblComponents.getColumn(1).setText("Purpose");
		
		String filterRequest = "";
		if ( this.filterName.getText().length() != 0 )
			filterRequest = " AND UPPER(name) like '"+this.filterName.getText().toUpperCase()+"%'";
			
		try (ResultSet result = this.importConnection.select("SELECT id, version, name, purpose FROM "+this.selectedDatabase.getSchemaPrefix()+"models m WHERE version = (SELECT MAX(version) FROM "+this.selectedDatabase.getSchemaPrefix()+"models WHERE id = m.id)" + filterRequest) ) {
			while (result.next()) {
				if ( !DBPlugin.areEqual(result.getString("id"), this.importedModel.getId()) ) {
					StringBuilder tooltipBuilder = new StringBuilder();
					TableItem item = createTableItem(this.tblComponents, result.getString("id"), "model", result.getString("name"), result.getString("purpose"));
					logger.trace("Found model "+result.getString("name"));
					try ( ResultSet resultProperties = this.importConnection.select("SELECT name, value FROM "+this.selectedDatabase.getSchemaPrefix()+"properties WHERE parent_id = ? AND parent_version = ?", result.getString("id"), result.getInt("version")) ) {
						while ( resultProperties.next() ) {
							if ( tooltipBuilder.length() != 0 )
								tooltipBuilder.append("\n");
							tooltipBuilder.append("   - ");
							tooltipBuilder.append(resultProperties.getString("name"));
							tooltipBuilder.append(": ");
							String value = resultProperties.getString("value");
							if ( value.length() > 22 )
								tooltipBuilder.append(value.substring(0,19)+"...");
							else
								tooltipBuilder.append(value);
						}
					}
					if ( tooltipBuilder.length() != 0 )
						item.setData("tooltip", tooltipBuilder.toString());
				}
			}
		}
		closeMessage();

		if ( this.tblComponents.getItemCount() < 2 ) {
			this.lblComponents.setText(this.tblComponents.getItemCount()+" model matches your criterias");
		} else {
			this.lblComponents.setText(this.tblComponents.getItemCount()+" models match your criterias");
		}

		this.btnDoAction.setEnabled(false);
	}

	void getElements() throws Exception {
		this.compoModels.setVisible(false);
		this.compoElements.setVisible(true);
		//compoFolders.setVisible(false);
		this.compoViews.setVisible(false);

		this.tblComponents.removeAll();
		Image image = this.lblPreview.getImage();
		if ( image != null ) {
			this.lblPreview.setImage(null);
			image.dispose();
		}
		this.lblPreview.setVisible(false);
		
		this.tblComponents.getColumn(1).setText("Documentation");

		if ( this.selectedDatabase == null )
			return;

		StringBuilder inList = new StringBuilder();
		ArrayList<String> classList = new ArrayList<String>();
		for (ComponentLabel label: this.allElementLabels) {
			if ( label.isSelected() ) {
				inList.append(inList.length()==0 ? "?" : ", ?");
				classList.add(label.getElementClassname());
			}
		}

		if ( inList.length() != 0 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Getting elements");

			setMessage("Getting list of elements from the database ...");

			this.hideAlreadyInModel.setVisible(true);
			this.hideOption.setVisible(true);
			this.hideOption.setText("Hide components with empty names");
			String addOn = "";
			if ( this.hideOption.getSelection() ) {
				if ( this.selectedDatabase.getDriver().equals("oracle") ) {
					addOn = " AND LENGTH(name) <> 0";
				} else {
					addOn = " AND name <> ''";
				}
			}

			addOn += " AND version = (SELECT MAX(version) FROM "+this.selectedDatabase.getSchemaPrefix()+"elements WHERE id = e.id)";
			addOn += " ORDER BY NAME";

			if ( inList.length() != 0 ) {
				String filterRequest = "";
				if ( this.filterName.getText().length() != 0 )
					filterRequest = " AND UPPER(name) like '"+this.filterName.getText().toUpperCase()+"%'";
				
				try (ResultSet result = this.importConnection.select("SELECT id, version, class, name, documentation FROM "+this.selectedDatabase.getSchemaPrefix()+"elements e WHERE class IN ("+inList.toString()+")" + addOn + filterRequest, classList) ) {
					while (result.next()) {
						if ( !this.hideAlreadyInModel.getSelection() || (this.importedModel.getAllElements().get(result.getString("id"))==null)) {
							StringBuilder tooltipBuilder = new StringBuilder();
							TableItem item = createTableItem(this.tblComponents, result.getString("id"), result.getString("Class"), result.getString("name"), result.getString("documentation"));

							try ( ResultSet resultProperties = this.importConnection.select("SELECT name, value FROM "+this.selectedDatabase.getSchemaPrefix()+"properties WHERE parent_id = ? AND parent_version = ?", result.getString("id"), result.getInt("version")) ) {
								while ( resultProperties.next() ) {
									if ( tooltipBuilder.length() != 0 )
										tooltipBuilder.append("\n");
									tooltipBuilder.append("   - ");
									tooltipBuilder.append(resultProperties.getString("name"));
									tooltipBuilder.append(": ");
									String value = resultProperties.getString("value");
									if ( value.length() > 22 )
										tooltipBuilder.append(value.substring(0,19)+"...");
									else
										tooltipBuilder.append(value);
								}
							}
							if ( tooltipBuilder.length() != 0 )
								item.setData("tooltip", tooltipBuilder.toString());
						}
					}
				}
			}

			closeMessage();
		}

		if ( this.tblComponents.getItemCount() < 2 ) {
			this.lblComponents.setText(this.tblComponents.getItemCount()+" component matches your criterias");
		} else {
			this.lblComponents.setText(this.tblComponents.getItemCount()+" components match your criterias");
		}

		this.btnDoAction.setEnabled(false);
	}

	/*
	private void getFolders() throws Exception {
		this.compoModels.setVisible(false);
		compoElements.setVisible(false);
		compoFolders.setVisible(true);
		compoViews.setVisible(false);

		tblComponents.removeAll();
		Image image = this.lblPreview.getImage();
		if ( image != null ) {
			this.lblPreview.setImage(null);
			image.dispose();
		}
		this.lblPreview.setVisible(false);

		if ( logger.isTraceEnabled() ) logger.trace("Getting folders");

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
		addOn += " ORDER BY NAME";

		if ( inList.length() != 0 ) {
			String filterRequest = "";
			if ( this.filterName.getText().length() != 0 )
				filterRequest = " AND UPPER(name) like '"+this.filterName.getText().toUpperCase()+"%'";

			try ( ResultSet result = database.select("SELECT id, name, documentation FROM "+selectedDatabase.getSchemaPrefix()+"folders WHERE root_type IN ("+inList.toString()+")" + addOn + filterRequest, typeList)) {
				while (result.next()) {
				    if ( !hideAlreadyInModel.getSelection() || (importedModel.getAllFolders().get(result.getString("id"))==null))
				        createTableItem(tblComponents, result.getString("id"), "Folder", result.getString("name"), result.getString("documentation"));
				}
			}
		}

		if ( tblComponents.getItemCount() < 2 ) {
			lblComponents.setText(tblComponents.getItemCount()+" component matches your criterias");
		} else {
			lblComponents.setText(tblComponents.getItemCount()+" components match your criterias");
		}

		btnDoAction.setEnabled(false);
	}
	 */

	void getViews() throws Exception {
		this.compoModels.setVisible(false);
		this.compoElements.setVisible(false);
		//compoFolders.setVisible(false);
		this.compoViews.setVisible(true);

		this.tblComponents.removeAll();
		Image image = this.lblPreview.getImage();
		if ( image != null ) {
			this.lblPreview.setImage(null);
			image.dispose();
		}
		this.lblPreview.setVisible(true);
		
		this.tblComponents.getColumn(1).setText("Documentation");

		if ( this.selectedDatabase == null )
			return;

		StringBuilder inList = new StringBuilder();
		ArrayList<String> classList = new ArrayList<String>();
		if ( this.archimateViews.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			classList.add("ArchimateDiagramModel");
		}
		if ( this.canvasViews.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			classList.add("CanvasModel");
		}
		if ( this.sketchViews.getSelection() ) {
			inList.append(inList.length()==0 ? "?" : ", ?");
			classList.add("SketchModel");
		}

		if ( inList.length() != 0 ) {
			setMessage("Getting list of views from database ...");

			if ( logger.isDebugEnabled() ) logger.debug("Getting views");

			this.hideAlreadyInModel.setVisible(true);
			this.hideOption.setVisible(true);
			this.hideOption.setText("Hide default views");
			String addOn = "";
			if ( this.hideOption.getSelection() )
				addOn = " AND name <> 'Default View'";

			addOn += " AND version = (SELECT MAX(version) FROM "+this.selectedDatabase.getSchemaPrefix()+"views WHERE id = v.id)";
			addOn += " ORDER BY NAME";

			if ( inList.length() != 0 ) {
				String filterRequest = "";
				if ( this.filterName.getText().length() != 0 )
					filterRequest = " AND UPPER(name) like '"+this.filterName.getText().toUpperCase()+"%'";
				
				try (ResultSet result = this.importConnection.select("SELECT id, version, class, name, documentation FROM "+this.selectedDatabase.getSchemaPrefix()+"views v WHERE class IN ("+inList.toString()+")" + addOn + filterRequest, classList)) {
					while (result.next()) {
						if ( !this.hideAlreadyInModel.getSelection() || (this.importedModel.getAllViews().get(result.getString("id"))==null)) {
							StringBuilder tooltipBuilder = new StringBuilder();
							TableItem item = createTableItem(this.tblComponents, result.getString("id"), result.getString("Class"), result.getString("name"), result.getString("documentation"));

							try ( ResultSet resultProperties = this.importConnection.select("SELECT name, value FROM "+this.selectedDatabase.getSchemaPrefix()+"properties WHERE parent_id = ? AND parent_version = ?", result.getString("id"), result.getInt("version")) ) {
								while ( resultProperties.next() ) {
									if ( tooltipBuilder.length() != 0 )
										tooltipBuilder.append("\n");
									tooltipBuilder.append("   - ");
									tooltipBuilder.append(resultProperties.getString("name"));
									tooltipBuilder.append(": ");
									String value = resultProperties.getString("value");
									if ( value.length() > 22 )
										tooltipBuilder.append(value.substring(0,19)+"...");
									else
										tooltipBuilder.append(value);
								}
							}
							if ( tooltipBuilder.length() != 0 )
								item.setData("tooltip", tooltipBuilder.toString());
						}
					}
				}
			}

			closeMessage();
		}

		if ( this.tblComponents.getItemCount() < 2 ) {
			this.lblComponents.setText(this.tblComponents.getItemCount()+" component matches your criterias");
		} else {
			this.lblComponents.setText(this.tblComponents.getItemCount()+" components match your criterias");
		}

		this.btnDoAction.setEnabled(false);
	}



	private static TableItem createTableItem(Table table, String id, String className, String name, String documentation) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setData("id", id);
		item.setText(0, "   "+name);
		item.setText(1, "   "+documentation);
		if ( className.toUpperCase().startsWith("CANVAS") )
			item.setImage(DBCanvasFactory.getImage(className));
		else
			item.setImage(DBArchimateFactory.getImage(className));
		return item; 
	}


	void doImport() throws Exception {
		if ( logger.isDebugEnabled() )
			logger.debug("Importing "+this.tblComponents.getSelectionCount()+" component" + ((this.tblComponents.getSelectionCount()>1) ? "s" : "") + " in " + DBImportMode.getLabel(getOptionValue()) + ".");
		
		CompoundCommand undoRedoCommands = new CompoundCommand();
		int done = 0;
		try {
			for ( TableItem tableItem: this.tblComponents.getSelection() ) {
				String id = (String)tableItem.getData("id");
				String name = tableItem.getText(0).trim();

				if ( this.radioOptionModel.getSelection() ) {
					setMessage("("+(++done)+"/"+this.tblComponents.getSelectionCount()+") Importing model \""+name+"\".");
					
					// import folders
					setMessage("("+done+"/"+this.tblComponents.getSelectionCount()+") Importing folders from model \""+name+"\".");
					Map<String, IFolder> foldersConversionMap = new HashMap<String, IFolder>();
					try ( ResultSet result = this.importConnection.select("SELECT fim.folder_id, fim.folder_version, fim.parent_folder_id, f.type, f.root_type, f.name FROM "+this.selectedDatabase.getSchemaPrefix()+"folders_in_model fim JOIN "+this.selectedDatabase.getSchemaPrefix()+"folders f ON fim.folder_id = f.id and fim.folder_version = f.version WHERE fim.model_id = ? AND fim.model_version = (SELECT MAX(model_version) FROM "+this.selectedDatabase.getSchemaPrefix()+"folders_in_model WHERE model_id = ?) ORDER BY fim.rank", id, id) ) {
						while ( result.next() ) {
							if ( result.getInt("type") != 0 ) {
								// root folders are not imported as they must not be duplicated so we need to keep a conversion table
								IFolder rootFolder = this.importedModel.getFolder(FolderType.get(result.getInt("type")));
								logger.debug("   Mapping root folder \""+result.getString("name")+"\"("+result.getString("folder_id")+") to folder \""+rootFolder.getName()+"\"("+rootFolder.getId()+")");
								foldersConversionMap.put(result.getString("folder_id"), rootFolder);
							} else {
								// we check if the parent folder needs to be translated
								IFolder parentFolder = foldersConversionMap.get(result.getString("parent_folder_id"));
								if ( parentFolder != null )
									logger.debug("   Translating parent folder to \""+parentFolder.getName()+"\"("+parentFolder.getId()+")");
								else
									parentFolder = this.importedModel.getAllFolders().get(result.getString("parent_folder_id"));
								IDBImportFromIdCommand command = new DBImportFolderFromIdCommand(this.importConnection, this.importedModel, parentFolder, result.getString("folder_id"), result.getInt("folder_version"), DBImportMode.get(getOptionValue())); 
								if ( command.getException() != null )
									throw command.getException();
								command.execute();
								if ( command.getException() != null )
									throw command.getException();
								undoRedoCommands.add((Command)command);
								// if the folder has been copied (thus has got a different id), then we add it to the conversion map
								IFolder importedFolder = (IFolder)command.getImported();
								if ( !DBPlugin.areEqual(result.getString("folder_id"), importedFolder.getId()) ) {
									logger.debug("   Mapping imported folder \""+result.getString("name")+"\"("+result.getString("folder_id")+") to folder \""+importedFolder.getName()+"\"("+importedFolder.getId()+")");
									foldersConversionMap.put(result.getString("folder_id"), importedFolder);
								}
							}
						}
					}
					
					// import elements
					setMessage("("+done+"/"+this.tblComponents.getSelectionCount()+") Importing elements from model \""+name+"\".");
					try ( ResultSet result = this.importConnection.select("SELECT element_id, element_version, parent_folder_id FROM "+this.selectedDatabase.getSchemaPrefix()+"elements_in_model WHERE model_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.selectedDatabase.getSchemaPrefix()+"elements_in_model WHERE model_id = ?) ORDER BY rank", id, id) ) {
						while ( result.next() ) {
							// we check if the parent folder needs to be translated
							IFolder parentFolder = foldersConversionMap.get(result.getString("parent_folder_id"));
							if ( parentFolder != null )
								logger.debug("   Translating parent folder to \""+parentFolder.getName()+"\"("+parentFolder.getId()+")");
							else
								parentFolder = this.importedModel.getAllFolders().get(result.getString("parent_folder_id"));
							IDBImportFromIdCommand command = new DBImportElementFromIdCommand(this.importConnection, this.importedModel, null, parentFolder, result.getString("element_id"), result.getInt("element_version"), DBImportMode.get(getOptionValue()), false); 
							if ( command.getException() != null )
								throw command.getException();
							command.execute();
							if ( command.getException() != null )
								throw command.getException();
							undoRedoCommands.add((Command)command);
						}
					}
					
					// import relationships
					setMessage("("+done+"/"+this.tblComponents.getSelectionCount()+") Importing relationships from model \""+name+"\".");
					try ( ResultSet result = this.importConnection.select("SELECT relationship_id, relationship_version, parent_folder_id FROM "+this.selectedDatabase.getSchemaPrefix()+"relationships_in_model WHERE model_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.selectedDatabase.getSchemaPrefix()+"relationships_in_model WHERE model_id = ?) ORDER BY rank", id, id) ) {
						while ( result.next() ) {
							// we check if the parent folder needs to be translated
							IFolder parentFolder = foldersConversionMap.get(result.getString("parent_folder_id"));
							if ( parentFolder != null )
								logger.debug("   Translating parent folder to \""+parentFolder.getName()+"\"("+parentFolder.getId()+")");
							else
								parentFolder = this.importedModel.getAllFolders().get(result.getString("parent_folder_id"));
							IDBImportFromIdCommand command = new DBImportRelationshipFromIdCommand(this.importConnection, this.importedModel, null, parentFolder, result.getString("relationship_id"), result.getInt("relationship_version"), DBImportMode.get(getOptionValue())); 
							if ( command.getException() != null )
								throw command.getException();
							command.execute();
							if ( command.getException() != null )
								throw command.getException();
							undoRedoCommands.add((Command)command);
						}
					}
					
					setMessage("("+done+"/"+this.tblComponents.getSelectionCount()+") Resolving relationships from model \""+name+"\".");
			        if ( (this.importedModel.getAllSourceRelationshipsToResolve().size() != 0) || (this.importedModel.getAllTargetRelationshipsToResolve().size() != 0) ) {
			            DBResolveRelationshipsCommand resolveRelationshipsCommand = new DBResolveRelationshipsCommand(this.importedModel);
			            resolveRelationshipsCommand.execute();
			            if ( resolveRelationshipsCommand.getException() != null )
			            	throw resolveRelationshipsCommand.getException();
			            undoRedoCommands.add(resolveRelationshipsCommand);
			        }
					
					// import views
					setMessage("("+done+"/"+this.tblComponents.getSelectionCount()+") Importing views from model \""+name+"\".");
					try ( ResultSet result = this.importConnection.select("SELECT view_id, view_version, parent_folder_id FROM "+this.selectedDatabase.getSchemaPrefix()+"views_in_model WHERE model_id = ? AND model_version = (SELECT MAX(model_version) FROM "+this.selectedDatabase.getSchemaPrefix()+"views_in_model WHERE model_id = ?) ORDER BY rank", id, id) ) {
						while ( result.next() ) {
							// we check if the parent folder needs to be translated
							IFolder parentFolder = foldersConversionMap.get(result.getString("parent_folder_id"));
							if ( parentFolder != null )
								logger.debug("   Translating parent folder to \""+parentFolder.getName()+"\"("+parentFolder.getId()+")");
							else
								parentFolder = this.importedModel.getAllFolders().get(result.getString("parent_folder_id"));
							IDBImportFromIdCommand command = new DBImportViewFromIdCommand(this.importConnection, this.importedModel, parentFolder, result.getString("view_id"), result.getInt("view_version"), DBImportMode.get(getOptionValue()), true); 
							if ( command.getException() != null )
								throw command.getException();
							command.execute();
							if ( command.getException() != null )
								throw command.getException();
							undoRedoCommands.add((Command)command);
						}
					}
					
					setMessage("("+done+"/"+this.tblComponents.getSelectionCount()+") Resolving connections from model \""+name+"\".");
			        if ( (this.importedModel.getAllSourceConnectionsToResolve().size() != 0) || (this.importedModel.getAllTargetConnectionsToResolve().size() != 0) ) {
			        	DBResolveConnectionsCommand resolveConnectionsCommand = new DBResolveConnectionsCommand(this.importedModel);
			            resolveConnectionsCommand.execute();
			            if ( resolveConnectionsCommand.getException() != null )
			            	throw resolveConnectionsCommand.getException();
			            undoRedoCommands.add(resolveConnectionsCommand);
			        }
				} else if ( this.radioOptionElement.getSelection() ) {
					setMessage("("+(++done)+"/"+this.tblComponents.getSelectionCount()+") Importing element \""+name+"\".");
					IDBImportFromIdCommand command = new DBImportElementFromIdCommand(this.importConnection, this.importedModel, this.selectedView, this.selectedFolder, id, 0, DBImportMode.get(getOptionValue()), true); 
					if ( command.getException() != null )
						throw command.getException();
					command.execute();
					if ( command.getException() != null )
						throw command.getException();
					undoRedoCommands.add((Command)command);
				}

				else if ( this.radioOptionView.getSelection() ) {
					setMessage("("+(++done)+"/"+this.tblComponents.getSelectionCount()+") Importing view \""+name+"\".");
					IDBImportFromIdCommand command = new DBImportViewFromIdCommand(this.importConnection, this.importedModel, this.selectedFolder, id, 0, DBImportMode.get(getOptionValue()), true);
					if ( command.getException() != null )
						throw command.getException();
					command.execute();
					if ( command.getException() != null )
						throw command.getException();
					undoRedoCommands.add((Command)command);
				}
			}

			DBResolveRelationshipsCommand resolveRelationshipsCommand = new DBResolveRelationshipsCommand(this.importedModel);
			resolveRelationshipsCommand.execute();
			if ( resolveRelationshipsCommand.getException() != null )
				throw resolveRelationshipsCommand.getException();
			undoRedoCommands.add(resolveRelationshipsCommand);

			DBResolveConnectionsCommand resolveConnectionsCommand = new DBResolveConnectionsCommand(this.importedModel);
			resolveConnectionsCommand.execute();
			if ( resolveConnectionsCommand.getException() != null )
				throw resolveConnectionsCommand.getException();
			undoRedoCommands.add(resolveConnectionsCommand);

			((CommandStack)this.importedModel.getAdapter(CommandStack.class)).execute(undoRedoCommands);

			// we select the imported components in the model tree 
			List<Object> imported = new ArrayList<Object>();

			Iterator<IDBImportFromIdCommand> iterator = undoRedoCommands.getCommands().iterator();
			while ( iterator.hasNext() ) {
				IDBImportFromIdCommand command = iterator.next();

				if ( command.getImported() != null )
					imported.add(command.getImported());
			}

			if ( !imported.isEmpty() ) {
				// We select the element in the model tree
				ITreeModelView treeView = (ITreeModelView)ViewManager.showViewPart(ITreeModelView.ID, true);
				if(treeView != null)
					treeView.getViewer().setSelection(new StructuredSelection(imported));
			}

			// we redraw the tblComponents table to unselect the items (and hide the newly imported components if the option is selected)
			this.hideAlreadyInModel.notifyListeners(SWT.Selection, new Event());
		} catch(RuntimeException e) {
			popup(Level.ERROR, "Couldn't import component.", e);
		} finally {
			// we do not catch the exception if any, but we need to close the popup
			closeMessage();
		}
	}

	private class ComponentLabel {
		Label label;

		ComponentLabel(Composite parent, String toolTip) {
			this.label = new Label(parent, SWT.NONE);
			this.label.setSize(100,  100);
			this.label.setToolTipText(toolTip);
			this.label.setImage(DBArchimateFactory.getImage(getElementClassname()));
			this.label.addPaintListener(this.redraw);
			this.label.addListener(SWT.MouseUp, DBGuiImportComponents.this.getElementsListener);
			setSelected(false);
		}

		private PaintListener redraw = new PaintListener() {
			@Override
			public void paintControl(PaintEvent event)
			{
				if ( ComponentLabel.this.isSelected() )
					ComponentLabel.this.label.setBackground(GREY_COLOR);
				//event.gc.drawRoundRectangle(0, 0, 16, 16, 2, 2);
				else
					ComponentLabel.this.label.setBackground(null);
			}
		};

		public String getElementClassname() {
			return this.label.getToolTipText().replaceAll(" ",  "");
		}

		public void setSelected(boolean selected) {
			this.label.setData("isSelected", selected);
		}

		public boolean isSelected() {
			Boolean selected = (Boolean)this.label.getData("isSelected");
			if ( selected == null )
				return false;
			return selected.booleanValue();
		}

		public void redraw() {
			this.label.redraw();
		}
	}

	Listener getElementsListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			Label label = (Label)event.widget;
			label.setData("isSelected", !(Boolean)label.getData("isSelected"));
			label.redraw();
			try {
				getElements();
			} catch (Exception err) {
				DBGui.popup(Level.ERROR, "An exception has been raised.", err);
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
				DBGui.popup(Level.ERROR, "An exception has been raised.", err);
			}
    	}
    };
	 */

	final private Listener getViewsListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			try {
				getViews();
			} catch (Exception err) {
				DBGui.popup(Level.ERROR, "An exception has been raised.", err);
			}
		}
	};

	final private Listener tooltipListener = new Listener() {
		Shell tip = null;
		StyledText label = null;

		@Override
		public void handleEvent (Event event) {
			switch (event.type) {
				case SWT.Dispose:
				case SWT.KeyDown:
				case SWT.MouseMove:
					if (this.tip == null) break;
					this.tip.dispose ();
					this.tip = null;
					this.label = null;
					break;

				case SWT.MouseHover:
					TableItem item = DBGuiImportComponents.this.tblComponents.getItem (new Point (event.x, event.y));

					if (item != null) {
						Table table = item.getParent();

						if ( (this.tip != null)  && !this.tip.isDisposed () )
							this.tip.dispose ();

						if ( item.getData("tooltip") != null ) {
							this.tip = new Shell (table.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
							FillLayout layout = new FillLayout ();
							layout.marginWidth = 2;
							this.tip.setLayout (layout);
							this.label = new StyledText (this.tip, SWT.NONE);
							this.label.setEditable(false);
							this.label.setText("Properties:\n"+(String)item.getData("tooltip"));
							StyleRange style = new StyleRange( 0, 11, null, null, SWT.BOLD);
							style.underline = true;
							this.label.setStyleRange(style);
							Point size = this.tip.computeSize (SWT.DEFAULT, SWT.DEFAULT);
							Point pt = table.toDisplay (event.x, event.y);
							this.tip.setBounds (pt.x, pt.y, size.x, size.y);
							this.tip.setVisible (true);
						}
					}
					break;

				default:
					break;
			}
		}
	};

	@Override
	public void close() {
		if ( this.lblPreview.getImage() != null )
			this.lblPreview.getImage().dispose();

		super.close();
	}
	
	private Listener sortListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			TableColumn sortedColumn = (TableColumn) e.widget;
			Table table = sortedColumn.getParent();
			table.setSortColumn(sortedColumn);
			Integer sortDirection = (Integer)sortedColumn.getData("sortDirection");
        	if ( sortDirection == null || sortDirection == SWT.DOWN )
        		sortDirection=SWT.UP;
        	else
        		sortDirection=SWT.DOWN;
        	sortedColumn.setData("sortDirection",sortDirection);
        	table.setSortDirection(sortDirection);
        	
			int columnIndex = 0;
			for ( int i = 0; i < table.getColumns().length; ++i ) {
				if ( table.getColumn(i).equals(sortedColumn) ) {
					columnIndex = i;
					break;
				}
			}
			
			TableItem[] items = table.getItems();
			Collator collator = Collator.getInstance(Locale.getDefault());
			
			for (int i = 1; i < items.length; i++) {
				String value1 = items[i].getText(columnIndex);
				for (int j = 0; j < i; j++) {
					String value2 = items[j].getText(columnIndex);
					if ( (sortDirection == SWT.UP && collator.compare(value1, value2) < 0) || (sortDirection == SWT.DOWN && collator.compare(value1, value2) > 0)) {
						// we save the old values
						String name = items[i].getText(0);
						String documentation = items[i].getText(1);
						String tooltip = (String) items[i].getData("tooltip");
						Image image = items[i].getImage();
						
						// we delete the old row
						items[i].dispose();
						
						// we create the new one
						TableItem item = new TableItem(table, SWT.NONE, j);
						
						// and restore the saved values
						item.setText(0, name);
						item.setText(1, documentation);
						item.setData("tooltip", tooltip);
						item.setImage(image);
						
						// at last, we refresh the items list
						items = table.getItems();
						break;
					}
				}
			}
		}
	};
}
