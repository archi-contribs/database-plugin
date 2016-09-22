/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

/**
 * Database Model Exporter
 * 
 * @author Herve Jouin
 */
package org.archicontribs.database;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.archicontribs.database.DBPlugin.Level;

import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.model.IFolder;
import com.archimatetool.model.util.Logger;

import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;


//Sorry if the code is not optimized, but I used a generator :)
public class DBSelectModel extends Dialog {
	private enum Action { Unknown, Import, Export };
	private HashMap<String, HashMap<String, String>> selectedModels = null;

	private String[] Relationships = {"", "Access", "Aggregation", "Assignment", "Association", "Composition", "Flow", "Grouping", "Influence", "Junction", "Realization", "Specialization", "Triggering", "Used by"};
	private String[] ElementLayers = {"", "Business", "Application", "Technology", "Motivation", "Implementation"};
	private String[] BusinessLayer = {"", "Business actor", "Business collaboration", "Business event", "Business function", "Business interaction", "Business interface", "Business object", "Business process", "Business role", "Business service", "Contract", "Location", "Meaning", "Product", "Representation", "Value"};
	private String[] ApplicationLayer = {"", "Application collaboration", "Application component", "Application function", "Application interaction" ,"Application interface", "Application service", "Data object"};
	private String[] TechnologyLayer = {"", "Artifact", "Communication path", "Device", "Infrastructure function", "Infrastructure interface", "Infrastructure service", "Network", "Node", "System software"};
	private String[] MotivationLayer = {"", "Assessment", "Constraint", "Driver", "Goal", "Principle", "Requirement", "Stakeholder"};
	private String[] ImplementationLayer = {"", "Deliverable", "Gap", "Plateau", "Work package"};

	private String filterModels = "";
	private String filterVersions = "";

	private int oldSelectedItemIndex;
	
	private String dbLanguage;
	private Connection dbConnection;
	private List<DBDatabaseEntry> databaseEntries = new ArrayList<DBDatabaseEntry>();
	
	static final Color LIGHT_GREEN_COLOR = new Color(null, 204, 255, 229);
	static final Color LIGHT_RED_COLOR = new Color(null, 255, 204, 204);
	static final Color GREY_COLOR = new Color(null, 220,220,220);
	static final Color WHITE_COLOR = new Color(null, 220,220,220);
	
	static final DBPreferencePage pref = new DBPreferencePage();

	/**
	 * Creates the dialog.
	 */
	public DBSelectModel() {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		DBPlugin.debug(DebugLevel.MainMethod, "new DBSelectModel()");
	}

	/**
	 * Open the dialog for exporting.
	 * @param _dbModel
	 * @return a List of HashMap containing information about the models to import or export. The returned keys are :<br>
	 * <ul>
	 * <li>		id :		the id of the model</li>
	 * <li>		name :		the name of the model</li>
	 * <li>		purpose :	the purpose of the model</li>
	 * <li>		owner :		the owner of the model</li>
	 * <li>		version :	the version of the model</li>
	 * <li>		note :		the release note of the version</li>
	 * <li>     mode :		the model opening mode (shared or standalone)
	 * </ul>
	 */
	public Connection selectModelToExport(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.selectModelToExport(\""+_dbModel.getName()+"\")");
		dbModel = _dbModel;
		action = Action.Export;
		Connection cnct = open();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.selectModelToExport(\""+_dbModel.getName()+"\")");
		return cnct;
	}
	
	/**
	 * Open the dialog for importing.
	 * @return a List of HashMap containing information about the models to import or export. The returned keys are :<br>
	 * <ul>
	 * <li>		id :		the id of the model</li>
	 * <li>		name :		the name of the model</li>
	 * <li>		purpose :	the purpose of the model</li>
	 * <li>		owner :		the owner of the model</li>
	 * <li>		version :	the version of the model</li>
	 * <li>		note :		the release note of the version</li>
	 * <li>     mode :		the model opening mode (shared or standalone)
	 * </ul>
	 */
	public Connection selectModelToImport() throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.selectModelToImport()");
		dbModel = null;
		action = Action.Import;
		Connection cnct = open();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.selectModelToImport()");
		return cnct;
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	private Connection open() throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.open()");
		oldSelectedItemIndex = -1;
		
		Display display = getParent().getDisplay();
		createContents();
		dialog.open();
		dialog.layout();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.open()");
		return dbConnection;
	}
	
	public HashMap<String, HashMap<String, String>> getSelectedModels() {
		return selectedModels;
	}
	
	public String getDbLanguage() {
		return dbLanguage;
	}

	/*
	 * Create contents of the dialog.
	 */
	private void createContents() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.createContents()");
		
		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setSize(840, 470);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 4);
		dialog.setLayout(null);
		dialog.addListener(SWT.Paint, setInitialValuesListener);

		/* ***************************** */
		/* * Selection of the database * */
		/* ***************************** */
		
		DBPlugin.createLabel(dialog, 10,10,60,20, "Database :", SWT.NONE);
		
		database = new CCombo(dialog, SWT.BORDER);
		database.setBounds(70, 8, 340, 20);
		database.addSelectionListener(databaseModifyListener);
		try {
			Field field = CCombo.class.getDeclaredField("text");
			field.setAccessible(true);
			databaseTextArea = (Text)field.get(database);
			field.setAccessible(false);
		} catch (Exception err) {
			databaseTextArea = null;
		}

		
		DBPlugin.createButton(dialog, 420, 5, 130, 25, "Set Preferences ...", SWT.PUSH, setPreferencesButtonCallback);
		
		/* ***************************** */
		/* * Table with ids and names  * */
		/* ***************************** */

		Composite compositeId = DBPlugin.createComposite(dialog, 10, 60, 103, 18, SWT.BORDER);
		lblId = DBPlugin.createLabel(compositeId, 0, 0, 103, 18, "ID :", SWT.CENTER);
		lblId.addMouseListener(sortColumnsAdapter);
		id = DBPlugin.createText(dialog, 10, 40, 103, 21, "", SWT.BORDER);
		id.setTextLimit(8);
		id.addListener(SWT.Verify, verifyIdListener);

		Composite compositeName = DBPlugin.createComposite(dialog, 112, 60, 298, 18, SWT.BORDER);
		lblName = DBPlugin.createLabel(compositeName, 0, 0, 298, 18, "Name :", SWT.CENTER);
		lblName.addMouseListener(sortColumnsAdapter);
		name = DBPlugin.createText(dialog, 112, 40, 298, 21, "", SWT.BORDER);
		name.setTextLimit(255);

		ScrolledComposite compositeTblId = DBPlugin.createScrolledComposite(dialog, 10, 77, 400, 320, SWT.BORDER | SWT.V_SCROLL);

		TableViewer tableViewerId = new TableViewer(compositeTblId, SWT.FULL_SELECTION);
		tblId = tableViewerId.getTable();
		tblId.setLinesVisible(true);
		tblId.addListener(SWT.Selection, selectModelListener);
		// if Action is Import, then the double click is equivalent to the OK button
		if ( action == Action.Import )
			tblId.addListener(SWT.MouseDoubleClick, new Listener() { public void handleEvent(Event event) { btnOK.notifyListeners(SWT.Selection, new Event()); }});

		compositeTblId.setContent(tblId);
		compositeTblId.setMinSize(tblId.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		columnId = new TableColumn(tblId, SWT.NONE);
		columnId.setResizable(false);
		columnId.setMoveable(true);
		columnId.setWidth(100);
		columnId.setText("ID");
		columnId.setData( new Label[]{lblId, lblName} );	// used by the sort callback

		columnName = new TableColumn(tblId, SWT.NONE);
		columnName.setResizable(false);
		columnName.setWidth(280);
		columnName.setText("Name");
		columnName.setData(new Label[]{lblName, lblId});	// used by the scroll callback
		
		/* ********************************** */
		/* * versions :                     * */
		/* *    if import --> table         * */
		/* *    if export --> radio buttons * */
		/* ********************************** */
		DBPlugin.createLabel(dialog, 420, 40, 55, 15, "Version :", SWT.NONE);
		if ( action == Action.Import ) {
			compositeVersion = DBPlugin.createScrolledComposite(dialog, 520, 40, 305, 92, SWT.BORDER | SWT.V_SCROLL);
	
			TableViewer tableViewerVersion = new TableViewer(compositeVersion, SWT.FULL_SELECTION);
			tblVersion = tableViewerVersion.getTable();
			tblVersion.setLinesVisible(true);
			tblVersion.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TableItem tableItem = tblVersion.getSelection()[0];
					name.setText(tableItem.getData("name") == null ? "" : (String)tableItem.getData("name"));
					purpose.setText(tableItem.getData("purpose") == null ? "" : (String)tableItem.getData("purpose"));
					owner.setText(tableItem.getData("owner") == null ? "" : (String)tableItem.getData("owner"));
					note.setText(tableItem.getData("note") == null ? "" : (String)tableItem.getData("note"));
				}
			});

			TableColumn columnVersion = new TableColumn(tblVersion, SWT.NONE);
			columnVersion.setResizable(false);
			columnVersion.setWidth(95);
			columnVersion.setText("Version");
	
			TableColumn columnDate = new TableColumn(tblVersion, SWT.NONE);
			columnDate.setResizable(false);
			columnDate.setWidth(190);
			columnDate.setText("Date");
			compositeVersion.setContent(tblVersion);
			compositeVersion.setMinSize(tblVersion.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		} else { // if action == Action.Export
			versionGrp = DBPlugin.createGroup(dialog, 520, 35, 205, 102, SWT.NONE);
	
			checkActual = DBPlugin.createButton(versionGrp, 6, 18, 96, 16, "actual version :", SWT.RADIO, null);
			checkMinor = DBPlugin.createButton(versionGrp, 6, 37, 101, 16, "minor change :", SWT.RADIO, null);
			checkMajor = DBPlugin.createButton(versionGrp, 6, 56, 100, 16, "major change :", SWT.RADIO, null);
			checkCustom = DBPlugin.createButton(versionGrp, 6, 75, 111, 16, "custom version :", SWT.RADIO, null);
	
			actualVersion = DBPlugin.createText(versionGrp, 120, 18, 68, 15, "", SWT.None);
			minorVersion = DBPlugin.createText(versionGrp, 120, 37, 68, 15, "", SWT.None);
			majorVersion = DBPlugin.createText(versionGrp, 120, 56, 68, 15, "", SWT.None);
			customVersion = DBPlugin.createText(versionGrp, 120, 75, 68, 15, "", SWT.None);
			customVersion.addListener(SWT.Verify, validateCustomVersionListener);
		}
		
		/* ******************************** */
		/* * Release note, owner, purpose * */
		/* ******************************** */

		note = DBPlugin.createLabelledText(dialog, 426, 143, 89, 15, "Release note :", 305, 15, "", SWT.BORDER);
		owner = DBPlugin.createLabelledText(dialog, 426, 168, 89, 15, "Owner :", 150, 15, "", SWT.BORDER);
		purpose = DBPlugin.createLabelledText(dialog, 426, 193, 89, 15, "Purpose :", 305, 130, "", SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		
		/* ******************************** */
		/* * Import and Export groups     * */
		/* ******************************** */
		
		if ( action == Action.Import ) {
			DBPlugin.createLabel(dialog, 426, 335, 80, 15, "Import mode :", SWT.NONE);
			grpMode = DBPlugin.createGroup(dialog, 521, 325, 305, 30, SWT.NONE);
			
			btnStandalone = DBPlugin.createButton(grpMode, 6, 10, 96, 16, "Standalone", SWT.RADIO, null);
			btnStandalone.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					lblAutoDepth.setVisible(false);
					lblDepth.setVisible(false);
					scaleDepth.setVisible(false);
				}
			});
			
			btnShared = DBPlugin.createButton(grpMode, 150, 10, 96, 16, "Shared", SWT.RADIO, null);
			btnShared.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					lblAutoDepth.setVisible(true);
					lblDepth.setVisible(true);
					scaleDepth.setVisible(true);
				}
			});
			
			lblAutoDepth = DBPlugin.createLabel(dialog, 426, 375, 89, 15, "Auto depth :", SWT.NONE);
			lblDepth = DBPlugin.createLabel(dialog, 520, 375, 55, 15, "Infinite", SWT.NONE);
			scaleDepth = new Scale(dialog, SWT.NONE);
			scaleDepth.setSelection(100);
			scaleDepth.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if ( scaleDepth.getSelection() == 0 ) lblDepth.setText("None");
					else if ( scaleDepth.getSelection() == 100 ) lblDepth.setText("Infinite");
					else lblDepth.setText(Integer.toString(scaleDepth.getSelection()));
				}
			});
			scaleDepth.setBounds(581, 361, 244, 40);
		} else {
			DBPlugin.createLabel(dialog, 426, 335, 80, 15, "Export :", SWT.NONE);
			grpMode = DBPlugin.createGroup(dialog, 521, 325, 305, 30, SWT.NONE);
			
			btnExport = DBPlugin.createButton(grpMode, 6, 10, 96, 16, "Export", SWT.RADIO, null);
			btnDoNotExport = DBPlugin.createButton(grpMode, 150, 10, 96, 16, "Do not export", SWT.RADIO, null);
			btnExport.setSelection(true);
		}
		
		/* ******************************** */
		/* * action buttons               * */
		/* ******************************** */

		btnDelete = DBPlugin.createButton(dialog, 10, 409, 75, 25, "Delete", SWT.NONE, deleteButtonCallback);
		btnChangeId = DBPlugin.createButton(dialog, 100, 409, 75, 25, "Change ID", SWT.NONE, null);
		btnApplyFilter = DBPlugin.createButton(dialog, 190, 409, 75, 25, "Filter", SWT.NONE, applyFilterButtonCallback);
		btnOK = DBPlugin.createButton(dialog, 668, 409, 75, 25, action==Action.Import ? "Import" : "Export", SWT.PUSH, okButtonCallback);
		btnCancel = DBPlugin.createButton(dialog, 749, 409, 75, 25, "Cancel", SWT.NONE, cancelButtonCallback);

		/* ******************************** */
		/* * filters                      * */
		/* ******************************** */

		lblElements = DBPlugin.createLabel(dialog, 10, 446, 80, 15, "Elements :", SWT.NONE);
		lblElements.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		lblElements.setBackground(GREY_COLOR);

		lblRelations = DBPlugin.createLabel(dialog, 10, 571, 80, 15, "Relationships :", SWT.NONE);
		lblRelations.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		lblRelations.setBackground(GREY_COLOR);

		lblProperties = DBPlugin.createLabel(dialog, 485, 446, 80, 15, "Properties :", SWT.NONE);
		lblProperties.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		lblProperties.setBackground(GREY_COLOR);

		Group ElementsGrp = DBPlugin.createGroup(dialog, 10, 455, 465, 107, SWT.NONE);
		ElementsGrp.setBackground(GREY_COLOR);

		lblEltLayer = DBPlugin.createLabel(ElementsGrp, 10, 5, 36, 15, "Layer :", SWT.NONE);
		lblEltLayer.setBackground(GREY_COLOR);		

		DBPlugin.createLabel(ElementsGrp, 140, 5, 36, 15, "Type :", SWT.NONE).setBackground(GREY_COLOR);

		lblEltName = DBPlugin.createLabel(ElementsGrp, 285, 5, 44, 15, "Name :", SWT.NONE);
		lblEltName.setBackground(GREY_COLOR);

		comboEltLayer1 = DBPlugin.createCombo(ElementsGrp, 5, 20, 130, 23, ElementLayers, SWT.NONE);
		comboEltLayer1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 ) e.doit = false;
				if ( ! e.text.equals(oldComboEltLayer1Text) ) {
					oldComboEltLayer1Text = e.text;
					comboEltType1.setText("");
					switch ( e.text ) {
					case "" :
					case "Model" : comboEltType1.removeAll(); break;
					case "Business" : comboEltType1.setItems(BusinessLayer); break;
					case "Application" : comboEltType1.setItems(ApplicationLayer); break;
					case "Technology" : comboEltType1.setItems(TechnologyLayer); break;
					case "Motivation" : comboEltType1.setItems(MotivationLayer); break;
					case "Implementation" : comboEltType1.setItems(ImplementationLayer); break;
					}
				}
			}
		});
		comboEltLayer2 = DBPlugin.createCombo(ElementsGrp, 5, 48, 130, 23, ElementLayers, SWT.NONE);
		comboEltLayer2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});
		comboEltLayer3 = DBPlugin.createCombo(ElementsGrp, 5, 76, 130, 23, ElementLayers, SWT.NONE);
		comboEltLayer3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltType1 = DBPlugin.createCombo(ElementsGrp, 140, 20, 140, 23, null, SWT.NONE);
		comboEltType1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltType2 = DBPlugin.createCombo(ElementsGrp, 140, 48, 140, 23, null, SWT.NONE);
		comboEltType2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltType3 = DBPlugin.createCombo(ElementsGrp, 140, 76, 140, 23, null, SWT.NONE);
		comboEltType3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		txtEltName1 = DBPlugin.createText(ElementsGrp, 285, 21, 170, 21, "", SWT.BORDER);
		txtEltName2 = DBPlugin.createText(ElementsGrp, 285, 49, 170, 21, "", SWT.BORDER);
		txtEltName3 = DBPlugin.createText(ElementsGrp, 285, 77, 170, 21, "", SWT.BORDER);

		grpProperties = DBPlugin.createGroup(dialog, 485, 455, 338, 107, SWT.NONE);				grpProperties.setBackground(GREY_COLOR);
		lblPropName = DBPlugin.createLabel(grpProperties, 10, 5, 36, 15, "Name :", SWT.NONE);	lblPropName.setBackground(GREY_COLOR);
		lblPropValue = DBPlugin.createLabel(grpProperties, 140, 5, 44, 15, "Value :", SWT.NONE);lblPropValue.setBackground(GREY_COLOR);

		TxtPropName1 = DBPlugin.createText(grpProperties, 10, 21, 125, 21, "", SWT.BORDER);
		TxtPropName2 = DBPlugin.createText(grpProperties, 10, 49, 125, 21, "", SWT.BORDER);
		TxtPropName3 = DBPlugin.createText(grpProperties, 10, 77, 125, 21, "", SWT.BORDER);

		TxtPropValue1 = DBPlugin.createText(grpProperties, 140, 21, 188, 21, "", SWT.BORDER);
		TxtPropValue2 = DBPlugin.createText(grpProperties, 140, 49, 188, 21, "", SWT.BORDER);
		TxtPropValue3 = DBPlugin.createText(grpProperties, 140, 77, 188, 21, "", SWT.BORDER);

		grpRelations = DBPlugin.createGroup(dialog, 10, 580, 465, 111, SWT.NONE);
		grpRelations.setBackground(GREY_COLOR);

		lblRelType = DBPlugin.createLabel(grpRelations, 5, 5, 36, 15, "Type :", SWT.NONE);		lblRelType.setBackground(GREY_COLOR);

		lblRelSource = DBPlugin.createLabel(grpRelations, 180, 5, 44, 15, "Source :", SWT.NONE);lblRelSource.setBackground(GREY_COLOR);
		lblRelTarget = DBPlugin.createLabel(grpRelations, 320, 5, 44, 15, "Target :", SWT.NONE);lblRelTarget.setBackground(GREY_COLOR);

		comboRelationship1 = DBPlugin.createCombo(grpRelations, 5, 20, 170, 23, Relationships, SWT.NONE);
		comboRelationship2 = DBPlugin.createCombo(grpRelations, 5, 48, 170, 23, Relationships, SWT.NONE);
		comboRelationship3 = DBPlugin.createCombo(grpRelations, 5, 76, 170, 23, Relationships, SWT.NONE);

		TxtSource1 = DBPlugin.createText(grpRelations, 180, 21, 135, 21, "", SWT.BORDER);
		TxtSource2 = DBPlugin.createText(grpRelations, 180, 49, 135, 21, "", SWT.BORDER);
		TxtSource3 = DBPlugin.createText(grpRelations, 180, 77, 135, 21, "", SWT.BORDER);

		TxtTarget1 = DBPlugin.createText(grpRelations, 320, 21, 135, 21, "", SWT.BORDER);
		TxtTarget2 = DBPlugin.createText(grpRelations, 320, 49, 135, 21, "", SWT.BORDER);
		TxtTarget3 = DBPlugin.createText(grpRelations, 320, 77, 135, 21, "", SWT.BORDER);

		DBPlugin.createLabel(dialog, 485, 550, 250, 50, "The search is not case sensitive.\nYou may use the '%' character as wildcard.", SWT.WRAP);

		btnResetFilter = DBPlugin.createButton(dialog, 669, 669, 75, 25, "Reset filter", SWT.NONE, ResetFilterListenerButtonCallback);
		
		btnCancelFilter = DBPlugin.createButton(dialog, 749, 669, 75, 25, "Cancel filter", SWT.NONE, CancelFilterListenerButtonCallback);

		//TODO : rework the tab list : the cancellbuttonfilter is not accessible when the filter is hidden !!!!!
		if ( action == Action.Import ) {
			dialog.getShell().setTabList(new Control[] { id, name, compositeTblId, compositeVersion, note, owner, purpose, grpMode, scaleDepth,
					btnDelete, btnChangeId, btnApplyFilter,
					ElementsGrp, grpProperties, grpRelations, btnResetFilter, btnCancelFilter,
					btnOK, btnCancel});
		} else {
			dialog.getShell().setTabList(new Control[] { id, name, compositeTblId, versionGrp, note, owner, purpose, grpMode,
					btnDelete, btnChangeId,
					ElementsGrp, grpProperties, grpRelations,
					btnOK, btnCancel});
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.createContents()");
	}


	 Listener setInitialValuesListener = new Listener() {
		public void handleEvent(Event event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.setInitialValuesListener.handleEvent("+DBPlugin.getEventName(event.type)+")");
			dialog.removeListener(SWT.Paint, setInitialValuesListener);
			setInitialValues();
			
		}
	};
	
	/*
	 * Fills in initial values.
	 * 
	 * This method is called once, right after the dialog is opened 
	 */
	private void setInitialValues() {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.setInitialValues()");
		if ( action == Action.Export ) {
			//TODO : /!\ the accents are not get properly !!!
			owner.setText(System.getProperty("user.name"));
			
			checkActual.setEnabled(true);
			checkMinor.setEnabled(true);
			checkMinor.setSelection(true);
			checkMajor.setEnabled(true);
			checkCustom.setEnabled(true);
			actualVersion.setEnabled(false);
			minorVersion.setEnabled(false);
			majorVersion.setEnabled(false);
			customVersion.setEnabled(true);
		} else {
			// in import mode, the text fields will be filled in by values from the database and are not editable
			id.setEditable(false);
			name.setEditable(false);
			owner.setEditable(false);
			note.setEditable(false);
			purpose.setEditable(false);
			
			boolean isStandalone = pref.isImportModeStandalone();
			btnStandalone.setSelection(isStandalone);
			btnShared.setSelection(!isStandalone);
			lblAutoDepth.setVisible(!isStandalone);
			lblDepth.setVisible(!isStandalone);
			scaleDepth.setVisible(!isStandalone);
		}
		
		//TODO: disable buttons (except cancel if not connected to any database
		DBDatabaseEntry.getAllFromPreferenceStore(databaseEntries);
		database.removeAll();
		for (DBDatabaseEntry databaseEntry: databaseEntries) {
			database.add(databaseEntry.getName());
		}
		
		selectedDatabase = -1;
		database.select(0);
		database.notifyListeners(SWT.Selection, new Event());
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.setInitialValues()");
	}
	
	/**
	 * Listener called when the database is modified
	 * 
	 * we connect to the corresponding database
	 */
	SelectionListener databaseModifyListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.databaseModifyListener.modifyText()");
			btnOK.setEnabled(action == Action.Export);
			btnCancel.setEnabled(true);
			btnDelete.setEnabled(false);
			btnApplyFilter.setEnabled(false);
			btnChangeId.setEnabled(false);
			
			//when we change the database, there is no filter by default. The use can always open back the filter window
			filterModels = "";
			filterVersions = "";
			dialog.setSize(840, 470);
			
			DBPlugin.debug(DebugLevel.Variable, "database.getSelectionIndex() = "+String.valueOf(database.getSelectionIndex()));
			DBPlugin.debug(DebugLevel.Variable, "selectedDatabase = "+String.valueOf(selectedDatabase));
			if ( database.getSelectionIndex() != selectedDatabase ) {
				DBPlugin.debug(DebugLevel.Variable, "database = " + database.getText() );
				
				for (DBDatabaseEntry databaseEntry: databaseEntries) {
					if ( database.getText().equals(databaseEntry.getName()) ) {
						tblId.removeAll();
						if ( databaseTextArea != null )
							databaseTextArea.setBackground(WHITE_COLOR);
						try {
							dbLanguage=DBPlugin.getDbLanguage(databaseEntry.getDriver());
							//TODO : replace standard cursor by BUSY_CURSOR
							DBPlugin.debug(DebugLevel.Variable, "calling openconnection("+databaseEntry.getDriver()+", "+databaseEntry.getServer()+", "+databaseEntry.getPort()+", "+databaseEntry.getDatabase()+", "+databaseEntry.getUsername()+", "+databaseEntry.getPassword()+")");
							dbConnection = DBPlugin.openConnection(databaseEntry.getDriver(), databaseEntry.getServer(), databaseEntry.getPort(), databaseEntry.getDatabase(), databaseEntry.getUsername(), databaseEntry.getPassword());
							DBPlugin.debug(DebugLevel.Variable, "connected :)");
							if ( action == Action.Export ) {
								// if the model selected is the shared one, then we export all the models
								// we get the list from subfolders of DBPlugin.SharedFolderName
								if ( dbModel.getModel().getId().equals(DBPlugin.SharedModelId) ) {
									checkActual.setSelection(false);
									checkMinor.setSelection(true);
									checkMajor.setSelection(false);
									checkCustom.setSelection(false);
									if ( dbModel.getProjectsFolders() != null ) {
										for ( IFolder folder: dbModel.getProjectsFolders() ) {
											TableItem tableItem = new TableItem(tblId, SWT.NONE);
											tableItem.setText(0, DBPlugin.getProjectId(folder.getId()));
											tableItem.setText(1, folder.getName());
					
											tableItem.setData("id", DBPlugin.getProjectId(folder.getId()));
											tableItem.setData("name", folder.getName());
											tableItem.setData("purpose", dbModel.getPurpose()!=null ? folder.getDocumentation() : "");
											tableItem.setData("owner", System.getProperty("user.name"));
											tableItem.setData("note", "");
											tableItem.setData("export", "yes");
											tableItem.setData("actual_version", DBPlugin.getVersion(folder.getId()));
											tableItem.setData("version", DBPlugin.incMinor(DBPlugin.getVersion(folder.getId())));
											
										}
									}
								} else {
									// if the model selected is not the shared one, then we export the selected model only 
									TableItem tableItem = new TableItem(tblId, SWT.NONE);
									tableItem.setText(0, dbModel.getProjectId());
									tableItem.setText(1, dbModel.getName());
					
									tableItem.setData("id", dbModel.getProjectId());
									tableItem.setData("name", dbModel.getName());
									tableItem.setData("purpose", dbModel.getPurpose()!=null ? dbModel.getPurpose() : "");
									tableItem.setData("owner", System.getProperty("user.name"));
									tableItem.setData("note", "");
									tableItem.setData("export", btnExport.getSelection()?"yes":"no");
									tableItem.setData("actual_version", dbModel.getVersion());
									if ( checkActual.getSelection() ) tableItem.setData("version", dbModel.getVersion()); else
										if ( checkMinor.getSelection() )  tableItem.setData("version", dbModel.getVersion());  else
											if ( checkMajor.getSelection() )  tableItem.setData("version", dbModel.getVersion());  else
												tableItem.setData("version", "");
								}
								if ( tblId.getItems().length > 0 ) {			
									tblId.setSelection(0);
									tblId.notifyListeners(SWT.Selection, new Event());
								}
							} else {
								// if action == Action.Import
								loadModelList();
							}
							if ( databaseTextArea != null )
								databaseTextArea.setBackground(LIGHT_GREEN_COLOR);
						} catch (Exception err) {
							if ( databaseTextArea != null )
								databaseTextArea.setBackground(LIGHT_RED_COLOR);
							DBPlugin.popup(Level.Error, "Cannot connect to the database.", err);
							dbConnection = null;
						}
					}
				}
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.databaseModifyListener.modifyText()");
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	};
	

	/*
	 * load the models id, name, versions from the database.
	 * 
	 *  This method is called when the method is opened and each time a filter is applied or cancelled. 
	 */
	private void loadModelList() throws Exception {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.loadModelList()");
		String request;
			// We get the last version of all models
		if ( dbLanguage.equals("SQL") ) {
			// request = "SELECT m1.model, m1.name, m1.version FROM model AS m1 JOIN (SELECT model, MAX(version) AS version FROM model GROUP BY model) AS m2 ON m1.model = m2.model and m1.version = m2.version" + filterModels);
			request = "SELECT m1.model, m1.name, max(m1.version) FROM model m1 "+filterModels+" GROUP BY m1.model, m1.name ORDER BY m1.name";
		} else {
			request = "MATCH (m1:model) " + filterModels + " RETURN m1.model as model, m1.name as name, max(m1.version) as version";
		}
		
		ResultSet result;
		try {
			result = DBPlugin.select(dbConnection, request);
			tblId.setRedraw(false);
			tblId.removeAll();
			tblVersion.removeAll();
			while(result.next()) {
				TableItem tableItem = new TableItem(tblId, SWT.NONE);
				tableItem.setText(0, result.getString("model"));
				tableItem.setText(1, result.getString("name"));
				if ( action == Action.Export  && dbModel.getProjectId().equals(result.getString("model")) ) {
					tblId.setSelection(tableItem);
					tblId.notifyListeners(SWT.Selection, new Event());
					actualVersion.setText(result.getString("version"));
					minorVersion.setText(DBPlugin.incMinor(result.getString("version")));
					majorVersion.setText(DBPlugin.incMajor(result.getString("version")));
					checkMinor.setEnabled(true);
				}
			}
			if ( tblId.getItems().length > 0 ) {			
				tblId.setSelection(0);
				tblId.notifyListeners(SWT.Selection, new Event());
				btnOK.setEnabled(true);
				btnChangeId.setEnabled(action == Action.Import);
				btnDelete.setEnabled(action == Action.Import);
				btnApplyFilter.setEnabled((action == Action.Import) && (dbLanguage.equals("SQL")));
			} else {
				btnOK.setEnabled(false);
			}
			tblId.setRedraw(true);
			result.close();
			if ( databaseTextArea != null )
				databaseTextArea.setBackground(LIGHT_GREEN_COLOR);
		} catch (Exception err) {
			if ( databaseTextArea != null )
				databaseTextArea.setBackground(LIGHT_RED_COLOR);
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.loadModelList()");
			throw err;
		}
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.loadModelList()");
	}

	/*
	 * Sorts the tableID columns
	 * 
	 * This callback is called each time there is a double click on the columns' header
	 */
	private MouseAdapter sortColumnsAdapter = new MouseAdapter() {
		public void mouseUp(MouseEvent e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.sortColumnsAdapter.mouseUp()");
			Collator collator = Collator.getInstance(Locale.getDefault());
			TableItem[] items;
			TableColumn column;

			if ( e.widget.equals(lblId))
				column = columnId;
			else 
				column = columnName;

			items = tblId.getItems();

			if (column == tblId.getSortColumn()) {
				tblId.setSortDirection(tblId.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
			} else {
				tblId.setSortColumn(column);
				tblId.setSortDirection(SWT.UP);
			}

			Label oldLabel = ((Label[])tblId.getSortColumn().getData())[1];
			String txt = oldLabel.getText().contains(" ") ? oldLabel.getText().split(" ")[0] : oldLabel.getText();
			oldLabel.setText(txt);

			Label newLabel = ((Label[])tblId.getSortColumn().getData())[0];
			txt = newLabel.getText().contains(" ") ? newLabel.getText().split(" ")[0] : newLabel.getText();
			txt += " ";
			txt += tblId.getSortDirection() == SWT.DOWN ? "(v)" : "(^)";
			newLabel.setText(txt);

			int columnIndex = -1;
			for ( int c=0; c < tblId.getColumnCount(); c++) {
				if ( column == tblId.getColumn(c) ) {
					columnIndex = c;
					break;
				}
			}
			if ( columnIndex != -1 ) {
				for (int i = 1; i < items.length; i++) {
					String value1 = items[i].getText(columnIndex);
					for (int j = 0; j < i; j++) {
						String value2 = items[j].getText(columnIndex);
						boolean inf = collator.compare(value1, value2) < 0;
						if ( tblId.getSortDirection() == SWT.DOWN)
							inf = ! inf;
						if (inf) {
							String[] values = { items[i].getText(0),items[i].getText(1) };
							items[i].dispose();
							TableItem item = new TableItem(tblId, SWT.NONE, j);
							item.setText(values);
							items = tblId.getItems();
							break;
						}
					}
				}
			}
		}
	};

	/*
	 * Retrieve the versions associated with a model
	 * 
	 * This callback is called each time a model is selected in the tableId table 
	 */
	private Listener selectModelListener = new Listener() {
		public void handleEvent(Event e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.selectModelListener.handleEvent()");
			try {
				ResultSet version;
				String request;

				if ( action == Action.Import ) {
					TableItem tableItem = null;
					// In Import mode, we got the information from the database, including all the versions stored in the database
					//System.out.println("selectModelListener : SELECT * FROM model WHERE model = ? " + filterVersions + " ORDER BY version >>> " + tblId.getSelection()[0].getText() + "<<<");
					if ( dbLanguage.equals("SQL") ) {
						request = "SELECT * FROM model m WHERE m.model = ? " + filterVersions + " ORDER BY m.version";
					} else {
						request = "MATCH (m:model) WHERE m.model = ? " + filterVersions + " RETURN m.version as version, m.period as period, m.model as model, m.name as name, m.purpose as purpose, m.owner as owner, m.note as note,"
								+ "m.countMetadatas as countMetadatas, m.countFolders as countFolders, m.countElements as countElements, m.countRelationships as countRelationships,"
								+ "m.countProperties as countProperties, m.countArchimateDiagramModels as countArchimateDiagramModels, m.countDiagramModelArchimateConnections as countDiagramModelArchimateConnections,"
								+ "m.countDiagramModelConnections as countDiagramModelConnections, m.countDiagramModelArchimateObjects as countDiagramModelArchimateObjects,"
								+ "m.countDiagramModelGroups as countDiagramModelGroups, m.countDiagramModelNotes as countDiagramModelNotes, m.countCanvasModels as countCanvasModels,"
								+ "m.countCanvasModelBlocks as countCanvasModelBlocks, m.countCanvasModelStickys as countCanvasModelStickys, m.countCanvasModelConnections as countCanvasModelConnections,"
								+ "m.countCanvasModelImages as countCanvasModelImages, m.countImages as countImages, m.countSketchModels as countSketchModels, m.countSketchModelActors as countSketchModelActors,"
								+ "m.countSketchModelStickys as countSketchModelStickys, m.countDiagramModelBendpoints as countDiagramModelBendpoints, m.countDiagramModelReferences as countDiagramModelReferences ORDER BY m.version";
					}
					version = DBPlugin.select(dbConnection, request, tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText());

					tblVersion.removeAll();
					while ( version.next() ) {
						tableItem = new TableItem(tblVersion, SWT.NONE);
						tableItem.setText(0, version.getString("version"));
						tableItem.setText(1, version.getString("period"));
						tableItem.setData("id", version.getString("model") == null ? "" : version.getString("model"));
						tableItem.setData("name", version.getString("name") == null ? "" : version.getString("name"));
						tableItem.setData("purpose", version.getString("purpose") == null ? "" : version.getString("purpose"));
						tableItem.setData("owner", version.getString("owner") == null ? "" : version.getString("owner"));
						tableItem.setData("note", version.getString("note") == null ? "" : version.getString("note"));

						tableItem.setData("countMetadatas", Integer.toString(version.getInt("countMetadatas")));
						tableItem.setData("countFolders", Integer.toString(version.getInt("countFolders")));
						tableItem.setData("countElements", Integer.toString(version.getInt("countElements")));
						tableItem.setData("countRelationships", Integer.toString(version.getInt("countRelationships")));
						tableItem.setData("countProperties", Integer.toString(version.getInt("countProperties")));
						tableItem.setData("countArchimateDiagramModels", Integer.toString(version.getInt("countArchimateDiagramModels")));
						tableItem.setData("countDiagramModelArchimateConnections", Integer.toString(version.getInt("countDiagramModelArchimateConnections")));
						tableItem.setData("countDiagramModelConnections", Integer.toString(version.getInt("countDiagramModelConnections")));
						tableItem.setData("countDiagramModelArchimateObjects", Integer.toString(version.getInt("countDiagramModelArchimateObjects")));
						tableItem.setData("countDiagramModelGroups", Integer.toString(version.getInt("countDiagramModelGroups")));
						tableItem.setData("countDiagramModelNotes", Integer.toString(version.getInt("countDiagramModelNotes")));
						tableItem.setData("countCanvasModels", Integer.toString(version.getInt("countCanvasModels")));
						tableItem.setData("countCanvasModelBlocks", Integer.toString(version.getInt("countCanvasModelBlocks")));
						tableItem.setData("countCanvasModelStickys", Integer.toString(version.getInt("countCanvasModelStickys")));
						tableItem.setData("countCanvasModelConnections", Integer.toString(version.getInt("countCanvasModelConnections")));
						tableItem.setData("countCanvasModelImages", Integer.toString(version.getInt("countCanvasModelImages")));
						tableItem.setData("countImages", Integer.toString(version.getInt("countImages")));
						tableItem.setData("countSketchModels", Integer.toString(version.getInt("countSketchModels")));
						tableItem.setData("countSketchModelActors", Integer.toString(version.getInt("countSketchModelActors")));
						tableItem.setData("countSketchModelStickys", Integer.toString(version.getInt("countSketchModelStickys")));
						tableItem.setData("countDiagramModelBendpoints", Integer.toString(version.getInt("countDiagramModelBendpoints")));
						tableItem.setData("countDiagramModelReferences", Integer.toString(version.getInt("countDiagramModelReferences")));
					}
					if ( tableItem != null ) {
						tblVersion.setSelection(0);
						tblVersion.notifyListeners(SWT.Selection, new Event());
						tblVersion.setSelection(tableItem);
						id.setText((String)tableItem.getData("id"));
						name.setText((String)tableItem.getData("name"));
						purpose.setText((String)tableItem.getData("purpose"));
					}
					version.close();
				} else { // if action == Action.Export
					// in Export mode, we get the information from the data previously set in the tblId list
					if ( oldSelectedItemIndex >= 0 ) {
						TableItem tableItem = tblId.getItem(oldSelectedItemIndex);
						tableItem.setData("id", id.getText());
						tableItem.setData("name", name.getText());
						tableItem.setData("purpose", purpose.getText());
						tableItem.setData("owner", owner.getText());
						tableItem.setData("note", note.getText());
						tableItem.setData("export", btnExport.getSelection()?"yes":"no");
						tableItem.setData("actual_version", actualVersion.getText());
						if ( checkActual.getSelection() ) tableItem.setData("version", actualVersion.getText()); else
							if ( checkMinor.getSelection() )  tableItem.setData("version", minorVersion.getText());  else
								if ( checkMajor.getSelection() )  tableItem.setData("version", majorVersion.getText());  else
									tableItem.setData("version", customVersion.getText());
					}

					oldSelectedItemIndex = tblId.getSelectionIndex();

					id.setText((String)tblId.getSelection()[0].getData("id"));
					name.setText((String)tblId.getSelection()[0].getData("name"));
					purpose.setText((String)tblId.getSelection()[0].getData("purpose"));
					owner.setText((String)tblId.getSelection()[0].getData("owner"));
					note.setText((String)tblId.getSelection()[0].getData("note"));
					actualVersion.setText((String)tblId.getSelection()[0].getData("actual_version"));
					minorVersion.setText(DBPlugin.incMinor((String)tblId.getSelection()[0].getData("actual_version")));
					majorVersion.setText(DBPlugin.incMajor((String)tblId.getSelection()[0].getData("actual_version")));
					checkActual.setSelection(false);
					checkMinor.setSelection(false);
					checkMajor.setSelection(false);
					checkCustom.setSelection(false);
					customVersion.setText("");

					if ( actualVersion.getText().equals((String)tblId.getSelection()[0].getData("version")))
						checkActual.setSelection(true);
					else if ( minorVersion.getText().equals((String)tblId.getSelection()[0].getData("version")))
						checkMinor.setSelection(true);
					else if ( majorVersion.getText().equals((String)tblId.getSelection()[0].getData("version")))
						checkMajor.setSelection(true);
					else {
						checkCustom.setSelection(true);
						customVersion.setText((String)tblId.getSelection()[0].getData("version"));
					}

					boolean doExport = tblId.getSelection()[0].getData("export").equals("yes");
					btnExport.setSelection(doExport);
					btnDoNotExport.setSelection(!doExport);
				}
			} catch (Exception ee) {
				Logger.logError("Cannot retreive details about model " + tblId.getSelection()[0].getText(), ee);
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.selectModelListener.handleEvent()");
		}
	};
	private SelectionListener applyFilterButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.applyFilterButtonCallback.widgetSelected()");
			dialog.setSize(840, 725);
			btnApplyFilter.setText("Apply filter");
			
			String likeOperator;
			if ( dbLanguage.equals("SQL") ) {
				likeOperator = "LIKE";
			} else {
				likeOperator = "=~";
			}

			//on vide la liste des models
			// on vide la liste des versions
			String filterEltModel = "";
			String filterRelModel = "";
			String filterPropModel = "";

			String filterEltVersion = "";
			String filterRelVersion = "";
			String filterPropVersion = "";


			//if we have to filter elements
			String clause = "";
			String clause1 = "";
			String clause2 = "";
			String clause3 = "";
			if ( !comboEltLayer1.getText().isEmpty() || !comboEltLayer2.getText().isEmpty() || !comboEltLayer3.getText().isEmpty() ||
					!comboEltType1.getText().isEmpty() || !comboEltType2.getText().isEmpty() || !comboEltType3.getText().isEmpty() ||
					!txtEltName1.getText().isEmpty() || !txtEltName2.getText().isEmpty() || !txtEltName3.getText().isEmpty() ) {

				if ( !comboEltType1.getText().isEmpty() )
					clause1 = "a.type = '"+DBPlugin.capitalize(comboEltType1.getText())+"'";
				else if ( !comboEltLayer1.getText().isEmpty() ) {
					String[] layer = null;;
					clause1 = "type IN (";
					switch ( comboEltLayer1.getText() ) {
					case "Business" : layer = BusinessLayer; break;
					case "Application" : layer = ApplicationLayer; break;
					case "Technology" : layer = TechnologyLayer; break;
					case "Motivation" : layer = MotivationLayer; break;
					case "Implementation" : layer = ImplementationLayer; break;
					}
					String sep = "";
					for (String s : layer) {
						if ( !s.isEmpty() ) {
							clause1 += sep + "'"+DBPlugin.capitalize(s)+"'" ;
							sep=",";
						}
					}
					clause1 += ")";
				}
				if ( !txtEltName1.getText().isEmpty() )
					clause1 += (clause1.isEmpty() ? "" : " AND ") + "UPPER(a.name) "+likeOperator+" UPPER('"+txtEltName1.getText()+"')";

				if ( !comboEltType2.getText().isEmpty() )
					clause2 = "a.type = '"+comboEltType2.getText()+"'";
				else if ( !comboEltLayer2.getText().isEmpty() ) {
					String[] layer = null;;
					clause2 = "a.type IN (";
					switch ( comboEltLayer2.getText() ) {
					case "Business" : layer = BusinessLayer; break;
					case "Application" : layer = ApplicationLayer; break;
					case "Technology" : layer = TechnologyLayer; break;
					case "Motivation" : layer = MotivationLayer; break;
					case "Implementation" : layer = ImplementationLayer; break;
					}
					String sep = "";
					for (String s : layer) {
						if ( !s.isEmpty() ) {
							clause2 += sep + "'"+DBPlugin.capitalize(s)+"'" ;
							sep=",";
						}
					}
					clause2 += ")";
				}
				if ( !txtEltName2.getText().isEmpty() )
					clause2 += (clause2.isEmpty() ? "" : " AND ") + "UPPER(a.name) "+likeOperator+" UPPER('"+txtEltName2.getText()+"')";

				if ( !comboEltType3.getText().isEmpty() )
					clause3 = "a.type = '"+comboEltType3.getText()+"'";
				else if ( !comboEltLayer3.getText().isEmpty() ) {
					String[] layer = null;;
					clause3 = "a.type IN (";
					switch ( comboEltLayer3.getText() ) {
					case "Business" : layer = BusinessLayer; break;
					case "Application" : layer = ApplicationLayer; break;
					case "Technology" : layer = TechnologyLayer; break;
					case "Motivation" : layer = MotivationLayer; break;
					case "Implementation" : layer = ImplementationLayer; break;
					}
					String sep = "";
					for (String s : layer) {
						if ( !s.isEmpty() ) {
							clause3 += sep + "'"+DBPlugin.capitalize(s)+"'" ;
							sep=",";
						}
					}
					clause3 += ")";
				}
				if ( !txtEltName3.getText().isEmpty() )
					clause3 += (clause3.isEmpty() ? "" : " AND ") + "UPPER(a.name) "+likeOperator+" UPPER('"+txtEltName3.getText()+"')";


				String sep = " WHERE ";
				if ( ! clause1.isEmpty() ) {
					clause += sep + "("+clause1+")";
					sep = " OR ";
				}
				if ( ! clause2.isEmpty() ) {
					clause += sep + "("+clause2+")";
					sep = " OR ";
				}
				if ( ! clause3.isEmpty() ) {
					clause += sep + "("+clause3+")";
					sep = " OR ";
				}

				if ( dbLanguage.equals("SQL") ) {
					filterEltModel = "SELECT DISTINCT model FROM archimateelement a " + clause;
					filterEltVersion = "SELECT DISTINCT version FROM archimateelement a WHERE model = ? " + clause;
				} else {
					filterEltModel = "MATCH (a:archimateelement) "+clause+" OPTIONAL MATCH (m:model) WHERE m.model = a.model RETURN DISTINCT m.model as model";
					filterEltVersion = "MATCH (a:archimateelement) "+clause+" OPTIONAL MATCH (m:model) WHERE m.model = a.model RETURN DISTINCT m.version as version";
				}
			}

			//if we have to filter relations
			clause = "";
			clause1 = "";
			clause2 = "";
			clause3 = "";
			if ( !comboRelationship1.getText().isEmpty() || !comboRelationship2.getText().isEmpty() || !comboRelationship3.getText().isEmpty() ||
					!TxtSource1.getText().isEmpty() || !TxtSource2.getText().isEmpty() || !TxtSource3.getText().isEmpty() ||
					!TxtTarget1.getText().isEmpty() || !TxtTarget2.getText().isEmpty() || !TxtTarget3.getText().isEmpty() ) {

				if ( !comboRelationship1.getText().isEmpty() )
					clause1 =  "relationship.type = '"+DBPlugin.capitalize(comboRelationship1.getText())+"Relationship'";
				if ( !TxtSource1.getText().isEmpty() )
					clause1 += (clause1.isEmpty() ? "" : " AND ") + "UPPER(source.name) LIKE UPPER('"+TxtSource1.getText()+"')";
				if ( !TxtTarget1.getText().isEmpty() )
					clause1 += (clause1.isEmpty() ? "" : " AND ") + "UPPER(target.name) LIKE UPPER('"+TxtTarget1.getText()+"')";

				if ( !comboRelationship2.getText().isEmpty() )
					clause2 =  "relationship.type = '"+DBPlugin.capitalize(comboRelationship2.getText())+"Relationship'";
				if ( !TxtSource2.getText().isEmpty() )
					clause2 += (clause2.isEmpty() ? "" : " AND ") + "UPPER(source.name) LIKE UPPER('"+TxtSource2.getText()+"')";
				if ( !TxtTarget2.getText().isEmpty() )
					clause2 += (clause2.isEmpty() ? "" : " AND ") + "UPPER(target.name) LIKE UPPER('"+TxtTarget2.getText()+"')";

				if ( !comboRelationship3.getText().isEmpty() )
					clause3 =  "relationship.type = '"+DBPlugin.capitalize(comboRelationship3.getText())+"Relationship'";
				if ( !TxtSource3.getText().isEmpty() )
					clause3 += (clause3.isEmpty() ? "" : " AND ") + "UPPER(source.name) LIKE UPPER('"+TxtSource3.getText()+"')";
				if ( !TxtTarget3.getText().isEmpty() )
					clause3 += (clause3.isEmpty() ? "" : " AND ") + "UPPER(target.name) LIKE UPPER('"+TxtTarget3.getText()+"')";

				String sep = " WHERE ";
				if ( ! clause1.isEmpty() ) {
					clause += sep + "("+clause1+")";
					sep = " OR ";
				}
				if ( ! clause2.isEmpty() ) {
					clause += sep + "("+clause2+")";
					sep = " OR ";
				}
				if ( ! clause3.isEmpty() ) {
					clause += sep + "("+clause3+")";
					sep = " OR ";
				}

				if ( clause.isEmpty() ) {
					filterRelModel = "SELECT DISTINCT relationship.model FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version";
					filterRelVersion = "SELECT DISTINCT relationship.version FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version and source.model = ? JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version WHERE target.model = ?";
				} else {
					filterRelModel = "SELECT DISTINCT relationship.model FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version " + clause;
					filterRelVersion = "SELECT DISTINCT relationship.version FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version and source.model = ? JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version " + clause + " AND target.model = ?";
				}
			}

			//if we have to filter properties
			clause = "";
			clause1 = "";
			clause2 = "";
			clause3 = "";

			if ( 	!TxtPropName1.getText().isEmpty() || !TxtPropName2.getText().isEmpty() || !TxtPropName3.getText().isEmpty() ||
					!TxtPropValue1.getText().isEmpty() || !TxtPropValue2.getText().isEmpty() || !TxtPropValue3.getText().isEmpty() ) {

				if ( !TxtPropName1.getText().isEmpty() )
					clause1 =  "UPPER(name) LIKE UPPER('"+TxtPropName1.getText()+"')";
				if ( !TxtPropValue1.getText().isEmpty() )
					clause1 += (clause1.isEmpty() ? "" : " AND ") + "UPPER(value) LIKE UPPER('"+TxtPropValue1.getText()+"')";

				if ( !TxtPropName2.getText().isEmpty() )
					clause2 =  "UPPER(name LIKE UPPER('"+TxtPropName2.getText()+"')";
				if ( !TxtPropValue2.getText().isEmpty() )
					clause2 += (clause2.isEmpty() ? "" : " AND ") + "UPPER(value) LIKE UPPER('"+TxtPropValue2.getText()+"')";

				if ( !TxtPropName3.getText().isEmpty() )
					clause3 =  "UPPER(name LIKE UPPER('"+TxtPropName3.getText()+"')";
				if ( !TxtPropValue3.getText().isEmpty() )
					clause3 += (clause3.isEmpty() ? "" : " AND ") + "UPPER(value) LIKE UPPER('"+TxtPropValue3.getText()+"')";

				String sep = " WHERE ";
				if ( ! clause1.isEmpty() ) {
					clause += sep + "("+clause1+")";
					sep = " OR ";
				}
				if ( ! clause2.isEmpty() ) {
					clause += sep + "("+clause2+")";
					sep = " OR ";
				}
				if ( ! clause3.isEmpty() ) {
					clause += sep + "("+clause3+")";
					sep = " OR ";
				}

				if ( clause.isEmpty() ) {
					filterPropModel = "SELECT DISTINCT model FROM property";
					filterPropVersion = "SELECT DISTINCT version FROM property WHERE model = ?";
				} else {
					filterPropModel = "SELECT DISTINCT model FROM property " + clause;
					filterPropVersion = "SELECT DISTINCT version FROM property " + clause + " AND model = ?";
				}
			}

			filterModels = "";
			filterVersions = "";
			if ( !filterEltModel.isEmpty() ) {
				filterModels = " WHERE m1.model IN ("+filterEltModel;
				filterVersions = " AND m.version IN ("+filterEltVersion;
			}
			if ( !filterRelModel.isEmpty() ) {
				if ( filterModels.isEmpty() ) {
					filterModels = " WHERE m1.model IN ("+filterRelModel;
					filterVersions = " AND m.version IN ("+filterRelVersion;
				} else {
					filterModels += " INTERSECT "+filterRelModel;
					filterVersions += " INTERSECT "+filterRelVersion;
				}
			}
			if ( !filterPropModel.isEmpty() ) {
				if ( filterModels.isEmpty() ) {
					filterModels = " WHERE m1.model IN ("+filterPropModel;
					filterVersions = " AND m.version IN ("+filterPropVersion;
				} else {
					filterModels += " INTERSECT "+filterPropModel;
					filterVersions += " INTERSECT "+filterPropVersion;
				}
			}
			if ( !filterModels.isEmpty() ) {
				filterModels += ")";
				filterVersions += ")";
			}

			try {
				tblId.removeAll();
				loadModelList();
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "An error occured while loading values from the database.", e);
			}
			
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.applyFilterButtonCallback.widgetSelected()");
		}
		
		public void widgetDefaultSelected(SelectionEvent event) {
			// do nothing
		}
	};
	
	private SelectionListener ResetFilterListenerButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			comboEltLayer1.setText("");
			comboEltLayer2.setText("");
			comboEltLayer3.setText("");
			
			comboEltType1.setText("");
			comboEltType2.setText("");
			comboEltType3.setText("");
			
			txtEltName1.setText("");
			txtEltName2.setText("");
			txtEltName3.setText("");
		
			grpProperties.setText("");
			lblPropName.setText("");
			lblPropValue.setText("");
		
			comboRelationship1.setText("");
			comboRelationship2.setText("");
			comboRelationship3.setText("");
		
			TxtSource1.setText("");
			TxtSource2.setText("");
			TxtSource3.setText("");
		
			TxtTarget1.setText("");
			TxtTarget2.setText("");
			TxtTarget3.setText("");
			
			TxtPropName1.setText("");
			TxtPropName2.setText("");
			TxtPropName3.setText("");
			
			TxtPropValue1.setText("");
			TxtPropValue2.setText("");
			TxtPropValue3.setText("");
			
			btnApplyFilter.notifyListeners(SWT.Selection, new Event());
		}
		
		public void widgetDefaultSelected(SelectionEvent event) {
			// do nothing
		}
	};
	
	private SelectionListener CancelFilterListenerButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.CancelFilterListenerButtonCallback.widgetSelected()");
			dialog.setSize(840, 470);
			btnApplyFilter.setText("Filter");
			filterModels = "";
			try {
				tblId.removeAll();
				loadModelList();
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "An error occured while loading values from the database.", e);
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.CancelFilterListenerButtonCallback.widgetSelected()");
		}
		
		public void widgetDefaultSelected(SelectionEvent event) {
			// do nothing
		}
	};
	private SelectionListener deleteButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.deleteButtonCallback.widgetSelected()");
			MessageDialog confirm = new MessageDialog(dialog, DBPlugin.pluginTitle, null,
					"Warning, you're going to remove the project "+name.getText() + " ("+id.getText()+"). Be aware that this operation cannot be undone !\n\nPlease confirm what you wish to remove exactly ...",
					MessageDialog.QUESTION, new String[] { "Whole model", "version "+tblVersion.getSelection()[0].getText(0)+" only", "cancel" }, 0);
			String msg = "Deleted.";
			try {
				switch ( confirm.open() ) {
				case 0 :
					DBPlugin.debug(DebugLevel.Variable, "Removing whole model !!!");
					if ( dbLanguage.equals("SQL") ) {
						for(String table: DBPlugin.allSQLTables ) {
							DBPlugin.request(dbConnection, "DELETE FROM " + table + " WHERE model = ?",
									id.getText());
						}
					} else {
						DBPlugin.request(dbConnection, "MATCH (node)-[rm:isInModel]->(model:model {model:?}) DETACH DELETE node, model",
								id.getText());
					}
					msg = "Model deleted.";
					break;
				case 1 :
					DBPlugin.debug(DebugLevel.Variable, "removing one version only");
					if ( dbLanguage.equals("SQL") ) {
						for(String table: DBPlugin.allSQLTables ) {
							DBPlugin.request(dbConnection, "DELETE FROM " + table + " WHERE model = ? AND version = ?",
									id.getText(), tblVersion.getSelection()[0].getText(0));
						}
					} else {
						DBPlugin.request(dbConnection, "MATCH  (node)-[rm:isInModel]->(model:model {model:?, version:?}) DETACH DELETE node, model",
									id.getText(), tblVersion.getSelection()[0].getText(0));
					}
					msg = "Version deleted.";
					break;
				case 2 :
					DBPlugin.debug(DebugLevel.Variable, "cancelled");
					return;
				}
				dbConnection.commit();
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "Deleting data : SQL Exception !!!", e);
				DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.deleteButtonCallback.widgetSelected()");
				return;
			}

			DBPlugin.popup(Level.Info, msg);
			try {
				loadModelList();
			} catch (Exception e) {
				Logger.logError("loadValuesFromDatabase : SQL Exception !!!", e);
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.deleteButtonCallback.widgetSelected()");
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	};
	
	private SelectionListener okButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.okButtonCallback.widgetSelected()");
			if ( !id.getText().isEmpty() ) { 
				selectedModels = new HashMap<String, HashMap<String, String>>();

				if ( action == Action.Import ) {
					// If action is Import, then the selectedModels list must contain one single entry with the model to import
					HashMap<String,String> hash = new HashMap<String,String>();
					hash.put("id", id.getText());
					hash.put("name", name.getText());
					hash.put("purpose", purpose.getText());
					hash.put("owner", owner.getText());
					hash.put("note", note.getText());
					hash.put("period", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
					hash.put("mode", btnShared.getSelection() ? "Shared" : "Standalone");
					hash.put("version", tblVersion.getSelection()[0].getText(0));
					
					hash.put("countMetadatas", (String)tblVersion.getSelection()[0].getData("countMetadatas"));
					hash.put("countFolders", (String)tblVersion.getSelection()[0].getData("countFolders"));
					hash.put("countElements", (String)tblVersion.getSelection()[0].getData("countElements"));
					hash.put("countRelationships", (String)tblVersion.getSelection()[0].getData("countRelationships"));
					hash.put("countProperties", (String)tblVersion.getSelection()[0].getData("countProperties"));
					hash.put("countArchimateDiagramModels", (String)tblVersion.getSelection()[0].getData("countArchimateDiagramModels"));
					hash.put("countDiagramModelArchimateConnections", (String)tblVersion.getSelection()[0].getData("countDiagramModelArchimateConnections"));
					hash.put("countDiagramModelConnections", (String)tblVersion.getSelection()[0].getData("countDiagramModelConnections"));
					hash.put("countDiagramModelArchimateObjects", (String)tblVersion.getSelection()[0].getData("countDiagramModelArchimateObjects"));
					hash.put("countDiagramModelGroups", (String)tblVersion.getSelection()[0].getData("countDiagramModelGroups"));
					hash.put("countDiagramModelNotes", (String)tblVersion.getSelection()[0].getData("countDiagramModelNotes"));
					hash.put("countCanvasModels", (String)tblVersion.getSelection()[0].getData("countCanvasModels"));
					hash.put("countCanvasModelBlocks", (String)tblVersion.getSelection()[0].getData("countCanvasModelBlocks"));
					hash.put("countCanvasModelStickys", (String)tblVersion.getSelection()[0].getData("countCanvasModelStickys"));
					hash.put("countCanvasModelConnections", (String)tblVersion.getSelection()[0].getData("countCanvasModelConnections"));
					hash.put("countCanvasModelImages", (String)tblVersion.getSelection()[0].getData("countCanvasModelImages"));
					hash.put("countImages", (String)tblVersion.getSelection()[0].getData("countImages"));
					hash.put("countSketchModels", (String)tblVersion.getSelection()[0].getData("countSketchModels"));
					hash.put("countSketchModelActors", (String)tblVersion.getSelection()[0].getData("countSketchModelActors"));
					hash.put("countSketchModelStickys", (String)tblVersion.getSelection()[0].getData("countSketchModelStickys"));
					hash.put("countDiagramModelBendpoints", (String)tblVersion.getSelection()[0].getData("countDiagramModelBendpoints"));
					hash.put("countDiagramModelReferences", (String)tblVersion.getSelection()[0].getData("countDiagramModelReferences"));
					
					selectedModels.put(id.getText(), hash);
				} else {		// if Action is Export, then the selectedModels list must contain one entry per model to export

					// we force to register the text fields into the data map of the tableItem
					tblId.setSelection(tblId.getSelectionIndex());
					tblId.notifyListeners(SWT.Selection, new Event());
					for ( TableItem tableItem: tblId.getItems() ) {
						if ( tableItem.getData("export").equals("yes") ) {
							HashMap<String,String> hash = new HashMap<String,String>();
							hash.put("id", (String)tableItem.getData("id"));
							hash.put("name", (String)tableItem.getData("name"));
							hash.put("purpose", (String)tableItem.getData("purpose"));
							hash.put("owner", (String)tableItem.getData("owner"));
							hash.put("note", (String)tableItem.getData("note"));
							hash.put("period", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
							hash.put("version", (String)tableItem.getData("version"));
							hash.put("export", (String)tableItem.getData("export"));
							selectedModels.put((String)tableItem.getData("id"), hash);
						}
					}
				}
				dialog.close();
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.okButtonCallback.widgetSelected()");
		}
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	};
	
	SelectionListener cancelButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent e) { 
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.cancelButtonCallback.widgetSelected()");
			dialog.close();
		}
		public void widgetDefaultSelected(SelectionEvent e) { 
			// do nothing
		}
	};
	
	SelectionListener setPreferencesButtonCallback = new SelectionListener() {
		public void widgetSelected(SelectionEvent e) {
			DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.setPreferencesButtonCallback.widgetSelected()");
			DBPlugin.debug(DebugLevel.Variable, "Openning preference page ...");
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "org.archicontribs.database.DBPreferencePage", null, null);
			dialog.setBlockOnOpen(true);
			if ( dialog.open() == 0 ) {
				DBPlugin.debug(DebugLevel.Variable, "Resetting settings from preferences ...");
				setInitialValues();
			} else {
				DBPlugin.debug(DebugLevel.Variable, "Preferences cancelled ...");
			}
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.setPreferencesButtonCallback.widgetSelected()");
		}
		public void widgetDefaultSelected(SelectionEvent e) { 
			// do nothing
		}
	};
	
	private Listener verifyIdListener = new Listener() {
		public void handleEvent(Event event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.verifyIdListener.handleEvent("+DBPlugin.getEventName(event.type)+")");
			String value = id.getText().substring(0, event.start) + event.text + id.getText().substring(event.end);
			event.doit = value.matches("^%?[a-zA-Z0-9]+$");
		}
	};
	
	private Listener validateCustomVersionListener = new Listener() {
		public void handleEvent(Event event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.validateCustomVersionListener.handleEvent("+DBPlugin.getEventName(event.type)+")");
			try {
				String value = customVersion.getText().substring(0, event.start) + event.text + customVersion.getText().substring(event.end);
				if ( !value.isEmpty() ) {
					Float.valueOf(value);
					String values[] = value.split("\\.");
					if ( values[0].length() > 5 ) throw new Exception();
					if ( values.length == 2 && values[1].length() > 5 ) throw new Exception();

					checkActual.setSelection(false);
					checkMinor.setSelection(false);
					checkMajor.setSelection(false);
					checkCustom.setSelection(true);
				}
			} catch (Exception ee) {
				event.doit = false;
			}
		}
	};

	private Shell dialog;
	private DBModel dbModel;
	private CCombo database;
	private Text databaseTextArea;
	private int selectedDatabase;
	private Action action = Action.Unknown;
	private Label lblId;
	private Text id;
	private Label lblName;
	private Text name;
	private Table tblId;
	private TableColumn columnId;
	private TableColumn columnName;
	private ScrolledComposite compositeVersion;
	private Group versionGrp;
	private Table tblVersion;
	private Button checkActual;
	private Text actualVersion;
	private Button checkMinor;
	private Text minorVersion;
	private Button checkMajor;
	private Text majorVersion;
	private Button checkCustom;
	private Text customVersion;
	private Button btnDelete;
	private Button btnChangeId;
	private Button btnApplyFilter;
	private Text txtEltName1;
	private Label lblEltLayer;
	private Label lblEltName;
	private Text txtEltName2;
	private Text txtEltName3;
	private Group grpProperties;
	private Text TxtPropValue1;
	private Label lblPropName;
	private Label lblPropValue;
	private Text TxtPropValue2;
	private Text TxtPropValue3;
	private Group grpRelations;
	private Text TxtSource1;
	private Combo comboRelationship1;
	private Label lblRelType;
	private Label lblRelSource;
	private Combo comboRelationship2;
	private Combo comboRelationship3;
	private Text TxtSource2;
	private Text TxtSource3;
	private Label lblElements;
	private Label lblRelations;
	private Text TxtPropName1;
	private Text TxtPropName2;
	private Text TxtPropName3;
	private Label lblProperties;
	private Button btnResetFilter;
	private Button btnCancelFilter;
	private Text TxtTarget1;
	private Label lblRelTarget;
	private Text TxtTarget2;
	private Text TxtTarget3;
	private Combo comboEltLayer1;
	private String oldComboEltLayer1Text = "";
	private Combo comboEltLayer2;
	private Combo comboEltLayer3;
	private Combo comboEltType1;
	private Combo comboEltType2;
	private Combo comboEltType3;
	private Group grpMode;
	private Button btnStandalone;
	private Button btnShared;
	private Button btnExport;
	private Button btnDoNotExport;
	private Scale scaleDepth;
	private Label lblDepth;
	private Label lblAutoDepth;

	private Text purpose;
	private Text owner;
	private Text note;
	private Button btnOK;
	private Button btnCancel;
}