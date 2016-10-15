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

/**
 * This class shows up a windows that allow the user to select the models to import or export
 * 
 * @author Hervé JOUIN
 */
public class DBSelectModel extends Dialog {
	private enum Action { Unknown, Import, Export };

	private String[] Relationships = {"", "Access", "Aggregation", "Assignment", "Association", "Composition", "Flow", "Grouping", "Influence", "Junction", "Realization", "Specialization", "Triggering", "Used by"};
	private String[] ElementLayers = {"", "Business", "Application", "Technology", "Motivation", "Implementation"};
	private String[] BusinessLayer = {"", "Business actor", "Business collaboration", "Business event", "Business function", "Business interaction", "Business interface", "Business object", "Business process", "Business role", "Business service", "Contract", "Location", "Meaning", "Product", "Representation", "Value"};
	private String[] ApplicationLayer = {"", "Application collaboration", "Application component", "Application function", "Application interaction" ,"Application interface", "Application service", "Data object"};
	private String[] TechnologyLayer = {"", "Artifact", "Communication path", "Device", "Infrastructure function", "Infrastructure interface", "Infrastructure service", "Network", "Node", "System software"};
	private String[] MotivationLayer = {"", "Assessment", "Constraint", "Driver", "Goal", "Principle", "Requirement", "Stakeholder"};
	private String[] ImplementationLayer = {"", "Deliverable", "Gap", "Plateau", "Work package"};

	private String filterModels = "";
	//private String filterVersions = "";

	private String dbLanguage;
	private Connection dbConnection;
	private List<DBDatabaseEntry> databaseEntries;
	
	static final Color LIGHT_GREEN_COLOR = new Color(null, 204, 255, 229);
	static final Color LIGHT_RED_COLOR = new Color(null, 255, 204, 204);
	static final Color GREY_COLOR = new Color(null, 220,220,220);
	static final Color WHITE_COLOR = new Color(null, 220,220,220);
	
	static final DBPreferencePage pref = new DBPreferencePage();

	String dbVersion;

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
		Connection cnct = openDialog();
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
		Connection cnct = openDialog();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.selectModelToImport()");
		return cnct;
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	private Connection openDialog() throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.open()");
		
		Display display = getParent().getDisplay();
		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setSize(840, 470);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 4);
		dialog.setLayout(null);

		/* ***************************** */
		/* * Selection of the database * */
		/* ***************************** */
		
		DBPlugin.createLabel(dialog, 10,10,60,20, "Database :", SWT.NONE);
		
		database = new CCombo(dialog, SWT.BORDER);
		database.setBounds(70, 8, 340, 20);
		database.addSelectionListener(databaseSelected);

		// Normally, the textArea of the combo widget is not accessible (private property)
		//       nevertheless, we want to access it anyway as we wish to change its background color
		//       we could use a sub class but preferred to use reflection
		//       if we fail to get the text widget, then the only impact will be that the background color will stay white, so not a big deal ...
		try {
			Field field = CCombo.class.getDeclaredField("text");
			field.setAccessible(true);
			databaseTextArea = (Text)field.get(database);
			field.setAccessible(false);
		} catch (Exception err) {
			databaseTextArea = null;
		}

		
		DBPlugin.createButton(dialog, 420, 5, 130, 25, "Set Preferences ...", SWT.PUSH, setPreferencesSelected);
		
		/* ***************************** */
		/* * Table with ids and names  * */
		/* ***************************** */

		Composite compositeId = DBPlugin.createComposite(dialog, 10, 60, 103, 18, SWT.BORDER);
		lblId = DBPlugin.createLabel(compositeId, 0, 0, 103, 18, "ID :", SWT.CENTER);
		lblId.addMouseListener(sortColumnsAdapter);
		id = DBPlugin.createText(dialog, 10, 40, 103, 21, "", SWT.BORDER);
		id.setTextLimit(8);
		id.addListener(SWT.Verify, verifyId);	//in export mode, the ID may be updated, so we check its validity

		Composite compositeName = DBPlugin.createComposite(dialog, 112, 60, 298, 18, SWT.BORDER);
		lblName = DBPlugin.createLabel(compositeName, 0, 0, 298, 18, "Name :", SWT.CENTER);
		lblName.addMouseListener(sortColumnsAdapter);
		name = DBPlugin.createText(dialog, 112, 40, 298, 21, "", SWT.BORDER);
		name.setTextLimit(255);

		ScrolledComposite compositeTblModels = DBPlugin.createScrolledComposite(dialog, 10, 77, 400, 320, SWT.BORDER | SWT.V_SCROLL);

		tblModels = new Table(compositeTblModels, SWT.CHECK | SWT.FULL_SELECTION);
		tblModels.setLinesVisible(true);
		tblModels.addListener(SWT.Selection, modelSelected);
		//TODO double click should select or unselect the item
		// if Action is Import, then the double click is equivalent to the OK button
		//if ( action == Action.Import )
		//	tblModels.addListener(SWT.MouseDoubleClick, new Listener() { public void handleEvent(Event event) { btnOK.notifyListeners(SWT.Selection, new Event()); }});

		compositeTblModels.setContent(tblModels);
		compositeTblModels.setMinSize(tblModels.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		TableColumn columnChecked = new TableColumn(tblModels, SWT.NONE);
		columnChecked.setResizable(false);
		columnChecked.setMoveable(false);
		columnChecked.setWidth(20);
		columnChecked.setText(" ");
		
		columnId = new TableColumn(tblModels, SWT.NONE);
		columnId.setResizable(false);
		columnId.setMoveable(false);
		columnId.setWidth(100);
		columnId.setText("ID");

		columnName = new TableColumn(tblModels, SWT.NONE);
		columnName.setResizable(false);
		columnName.setMoveable(false);
		columnName.setWidth(280);
		columnName.setText("Name");
		
		/* ********************************** */
		/* * versions :                     * */
		/* *    if import --> table         * */
		/* *    if export --> radio buttons * */
		/* ********************************** */
		DBPlugin.createLabel(dialog, 420, 40, 55, 15, "Version :", SWT.NONE);
		if ( action == Action.Import ) {
			compositeVersion = DBPlugin.createScrolledComposite(dialog, 520, 40, 305, 92, SWT.BORDER | SWT.V_SCROLL);
	
			tblVersions = new Table(compositeVersion, SWT.FULL_SELECTION);
			tblVersions.setLinesVisible(true);
			tblVersions.addListener(SWT.Selection, versionSelected);

			TableColumn columnVersion = new TableColumn(tblVersions, SWT.NONE);
			columnVersion.setResizable(false);
			columnVersion.setWidth(95);
			columnVersion.setText("Version");
	
			TableColumn columnDate = new TableColumn(tblVersions, SWT.NONE);
			columnDate.setResizable(false);
			columnDate.setWidth(190);
			columnDate.setText("Date");
			compositeVersion.setContent(tblVersions);
			compositeVersion.setMinSize(tblVersions.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		} else { // if action == Action.Export
			versionGrp = DBPlugin.createGroup(dialog, 520, 35, 205, 102, SWT.NONE);
	
			checkActualVersion = DBPlugin.createButton(versionGrp, 6, 18, 96, 16, "actual version :", SWT.RADIO, null);
			checkMinorVersion = DBPlugin.createButton(versionGrp, 6, 37, 101, 16, "minor change :", SWT.RADIO, null);
			checkMajorVersion = DBPlugin.createButton(versionGrp, 6, 56, 100, 16, "major change :", SWT.RADIO, null);
			checkCustomVersion = DBPlugin.createButton(versionGrp, 6, 75, 111, 16, "custom version :", SWT.RADIO, null);
	
			actualVersionValue = DBPlugin.createText(versionGrp, 120, 18, 68, 15, "", SWT.None);
			minorVersionValue = DBPlugin.createText(versionGrp, 120, 37, 68, 15, "", SWT.None);
			majorVersionValue = DBPlugin.createText(versionGrp, 120, 56, 68, 15, "", SWT.None);
			customVersionValue = DBPlugin.createText(versionGrp, 120, 75, 68, 15, "", SWT.None);
			customVersionValue.addListener(SWT.Verify, validateCustomVersion);
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
		}
		
		/* ******************************** */
		/* * action buttons               * */
		/* ******************************** */

		btnDelete = DBPlugin.createButton(dialog, 10, 409, 75, 25, "Delete", SWT.NONE, deleteButtonSelected);
		btnChangeId = DBPlugin.createButton(dialog, 100, 409, 75, 25, "Change ID", SWT.NONE, null);
		btnApplyFilter = DBPlugin.createButton(dialog, 190, 409, 75, 25, "Filter", SWT.NONE, applyFilterSelected);
		btnOK = DBPlugin.createButton(dialog, 668, 409, 75, 25, action==Action.Import ? "Import" : "Export", SWT.PUSH, okButtonSelected);
		btnCancel = DBPlugin.createButton(dialog, 749, 409, 75, 25, "Cancel", SWT.NONE, cancelButtonSelected);

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

		btnResetFilter = DBPlugin.createButton(dialog, 669, 669, 75, 25, "Reset filter", SWT.NONE, resetFilterSelected);
		
		btnCancelFilter = DBPlugin.createButton(dialog, 749, 669, 75, 25, "Cancel filter", SWT.NONE, cancelFilterSelected);

		//TODO : rework the tab list : the cancelbuttonfilter is not accessible when the filter is hidden !!!!!
		if ( action == Action.Import ) {
			dialog.getShell().setTabList(new Control[] { id, name, compositeTblModels, compositeVersion, note, owner, purpose, grpMode, scaleDepth,
					btnDelete, btnChangeId, btnApplyFilter,
					ElementsGrp, grpProperties, grpRelations, btnResetFilter, btnCancelFilter,
					btnOK, btnCancel});
		} else {	// action == Action.Export
			dialog.getShell().setTabList(new Control[] { id, name, compositeTblModels, versionGrp, note, owner, purpose,
					btnDelete, btnChangeId,
					ElementsGrp, grpProperties, grpRelations,
					btnOK, btnCancel});
		}
		
		if ( action == Action.Export ) {
			//TODO : /!\ the accents are not get properly !!!
			owner.setText(System.getProperty("user.name"));
			
			checkActualVersion.setEnabled(true);	checkActualVersion.setSelection(false);	actualVersionValue.setEnabled(false);
			checkMinorVersion.setEnabled(true);	checkMinorVersion.setSelection(true);		minorVersionValue.setEnabled(false);
			checkMajorVersion.setEnabled(true);	checkMajorVersion.setSelection(false);		majorVersionValue.setEnabled(false);
			checkCustomVersion.setEnabled(true);	checkCustomVersion.setSelection(false);	customVersionValue.setEnabled(true);
		} else { // action == Action.IMPORT
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
		
		// we get the databases list from the preferences
		databaseEntries = DBDatabaseEntry.getAllFromPreferenceStore();
		database.removeAll();
		for (DBDatabaseEntry databaseEntry: databaseEntries) {
			database.add(databaseEntry.getName());
		}

		// we fill in the models from the default database
		if ( databaseEntries.size() > 0 ) {
			database.select(0);
			database.notifyListeners(SWT.Selection, new Event());
		}
				
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
	
	/**
	 * Listener called when the user selects a database in the combo
	 * 
	 * we connect to the corresponding database
	 */
	SelectionListener databaseSelected = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.databaseSelected.widgetSelected()");
			btnOK.setEnabled(false);
			btnCancel.setEnabled(true);
			btnDelete.setEnabled(false);
			btnApplyFilter.setEnabled(false);
			btnChangeId.setEnabled(false);
			
			//when we change the database, there is no filter by default. The use can always open back the filter window
			filterModels = "";
			//filterVersions = "";
			dialog.setSize(840, 470);

			// if we are in import mode, we empty the models and versions tables
			if ( action == Action.Import ) {
				tblModels.removeAll();
				tblVersions.removeAll();
				formerModelItem = null;
			}

			// we get the databaseEntry corresponding to the selected combo entry
			DBDatabaseEntry databaseEntry = databaseEntries.get(database.getSelectionIndex());
			DBPlugin.debug(DebugLevel.Variable, "selected database = " + database.getText());
			
			// if we succeeded to get the combo text area widget, then we change its background color
			if ( databaseTextArea != null )
				databaseTextArea.setBackground(WHITE_COLOR);
			
			// then we connect to the database.
			try {
				dbLanguage=DBPlugin.getDbLanguage(databaseEntry.getDriver());
				//TODO : replace standard cursor by BUSY_CURSOR
				DBPlugin.debug(DebugLevel.Variable, "calling openconnection("+databaseEntry.getDriver()+", "+databaseEntry.getServer()+", "+databaseEntry.getPort()+", "+databaseEntry.getDatabase()+", "+databaseEntry.getUsername()+", "+databaseEntry.getPassword()+")");
				dbConnection = DBPlugin.openConnection(databaseEntry.getDriver(), databaseEntry.getServer(), databaseEntry.getPort(), databaseEntry.getDatabase(), databaseEntry.getUsername(), databaseEntry.getPassword());
				//if the database connection failed, then an exception is raised, meaning that we get here only if the database connection succeeded
				DBPlugin.debug(DebugLevel.Variable, "connected :)");
				if ( databaseTextArea != null )
					databaseTextArea.setBackground(LIGHT_GREEN_COLOR);
			} catch (Exception err) {
				if ( databaseTextArea != null )
					databaseTextArea.setBackground(LIGHT_RED_COLOR);
				if ( err.getMessage().equals("Cannot connect to the database") )
					DBPlugin.popup(Level.Error, "Cannot connect to the database.");
				else
					DBPlugin.popup(Level.Error, "Cannot connect to the database.", err);
				dbConnection = null;
				if ( databaseTextArea != null )
					databaseTextArea.setBackground(LIGHT_RED_COLOR);
			}
			
			if ( dbConnection != null ) {
				//TODO: separate the dbVersion test in import and export blocs as the choices are not the same
			
				String requestVersion;
				// We check the database version
				if ( dbLanguage.equals("SQL") ) {
					requestVersion = "SELECT p.version FROM archidatabaseplugin p";
				} else {
					requestVersion = "MATCH (p:archidatabaseplugin) RETURN p.version as version";
				}
			
				dbVersion = null;
				try {
					ResultSet resultVersion = DBPlugin.select(dbConnection, requestVersion);
					
					if ( resultVersion.next() ) {
						dbVersion = resultVersion.getString("version");
					}
					
					switch ( dbVersion ) {
					case "3" :
						// Archi 4
						if ( action == Action.Import ) {
							if ( !DBPlugin.question("Warning\n\nThe models in this database have been created with Archi 3. They can be imported to Archi 4 but can't be exported back to this database.\n\nDo you wish to continue ?") ) {
								throw new Exception("cancelByUser");
							}
						} else {
							throw new Exception("I'm sorry, but I can't export to this database as it is configured for Archi version "+dbVersion);
						}
						break;
					case "4.0" :
						break;
					default :
						if ( action == Action.Import ) {
							throw new Exception("I'm sorry, but I can't import from this database as it is configured for Archi version "+dbVersion);
						} else {
							throw new Exception("I'm sorry, but I can't export to this database as it is configured for Archi version "+dbVersion);
						}
					}
				} catch (Exception err) {
					if ( databaseTextArea != null )
						databaseTextArea.setBackground(LIGHT_RED_COLOR);
					if ( !"cancelByUser".equals(err.getMessage()) )
						DBPlugin.popup(Level.Error, err.getMessage());
					DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.databaseSelected.widgetSelected()");
					return;
				}
				
				if ( action == Action.Export ) {
					if ( tblModels.getItemCount() == 0 ) {
						// we fill-in the models table only once
						if ( dbModel.getModel().getId().equals(DBPlugin.SharedModelId) ) {
							DBPlugin.debug(DebugLevel.Variable, "Filling-in models tables with shared model projects");
							// if the model selected is the shared one, then we export all the projects. We get the list from <DBPlugin.SharedFolderName> subfolders
							if ( dbModel.getProjectsFolders() != null ) {
								for ( IFolder folder: dbModel.getProjectsFolders() ) {
									TableItem modelItem = new TableItem(tblModels, SWT.NONE);
									modelItem.setChecked(true);
									modelItem.setText(1, DBPlugin.getProjectId(folder.getId()));
									modelItem.setText(2, folder.getName());
			
									modelItem.setData("id", DBPlugin.getProjectId(folder.getId()));
									modelItem.setData("name", folder.getName());
									modelItem.setData("purpose", dbModel.getPurpose()!=null ? folder.getDocumentation() : "");
									modelItem.setData("owner", System.getProperty("user.name"));
									modelItem.setData("note", "");
									modelItem.setData("version:actual", DBPlugin.getVersion(folder.getId()));
									modelItem.setData("version:new", DBPlugin.incMinor(DBPlugin.getVersion(folder.getId())));
								}
							}
						} else {		// !dbModel.getModel().getId().equals(DBPlugin.SharedModelId)
							// if the model selected is not the shared one, then we export the selected model only 
							DBPlugin.debug(DebugLevel.Variable, "Filling-in models tables with \""+dbModel.getName()+"\" model");
							TableItem modelItem = new TableItem(tblModels, SWT.NONE);
							modelItem.setChecked(true);
							modelItem.setText(1, dbModel.getProjectId());
							modelItem.setText(2, dbModel.getName());
			
							modelItem.setData("id", dbModel.getProjectId());
							modelItem.setData("name", dbModel.getName());
							modelItem.setData("purpose", dbModel.getPurpose()==null ? "" : dbModel.getPurpose());
							modelItem.setData("owner", System.getProperty("user.name"));
							modelItem.setData("note", "");
							modelItem.setData("version:actual", dbModel.getVersion());
							modelItem.setData("version:new", DBPlugin.incMinor(DBPlugin.getVersion(dbModel.getVersion())));
						}
						if ( tblModels.getItemCount() > 0 ) {			
							tblModels.setSelection(0);
							tblModels.notifyListeners(SWT.Selection, new Event());		 // calls modelSelected listener
						}
					}
					// if there is at least one item selected, the we activate the btnOk
					// else, we deactivate it
					boolean mayValidate = false;
					for ( TableItem modelItem: tblModels.getItems() ) {
						if ( modelItem.getChecked() ) {
							mayValidate = true;
							break;
						}
					}
					btnOK.setEnabled(mayValidate);
				} else {		// if action == Action.Import
					try {
						if ( dbConnection != null )
							loadModelList();
					} catch (Exception err) {
						if ( databaseTextArea != null )
							databaseTextArea.setBackground(LIGHT_RED_COLOR);
						DBPlugin.popup(Level.Error, "Cannot import model list from database.", err);
					}
				}
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.databaseSelected.widgetSelected()");
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	};
	
	/**
	 * load the models id, name, versions from the database.
	 * 
	 *  This method is called when the method is opened and each time a filter is applied or cancelled. 
	 */
	private void loadModelList() throws Exception {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.loadModelList()");
		
		// when exporting, the tblModels is directly filled in by the databaseModifyListener
		assert(action == Action.Import);
		
		// we empty the tables as they will be filled-in with fresh data coming from the database 
		tblModels.removeAll();
		tblVersions.removeAll();
		formerModelItem = null;
		
		// if we're not connected to any database, then we cannot get any model list, can we ?
		if ( dbConnection == null )
			return;
		
		String request;
			// We get the latest version of all models
		if ( dbLanguage.equals("SQL") ) {
			request = "SELECT DISTINCT m.model FROM model m "+filterModels+" ORDER BY m.model";
		} else {
			request = "MATCH (m:model) " + filterModels + " RETURN DISTINCT m.model as model ORDER BY m.model";
		}
		
		try {
			ResultSet resultModels = DBPlugin.select(dbConnection, request);
			tblModels.setRedraw(false);
			while(resultModels.next()) {
				DBPlugin.debug(DebugLevel.Variable, "   Found project "+resultModels.getString("model"));
				TableItem modelItem = new TableItem(tblModels, SWT.NONE);
				modelItem.setText(1, resultModels.getString("model"));
				
				// for each model, we get the list of the versions (each version having it's own owner, release note, purpose, ... but the project name is the name of the latest version)
				if ( dbLanguage.equals("SQL") ) {
					request = "SELECT m.version, m.name, m.note, m.owner, m.period, m.purpose, m.countmetadatas, m.countFolders, m.countElements, m.countrelationships, m.countproperties, m.countarchimatediagrammodels, m.countdiagrammodelarchimateconnections, m.countdiagrammodelconnections, m.countdiagrammodelarchimateobjects, m.countdiagrammodelgroups, m.countdiagrammodelnotes, m.countcanvasmodels, m.countcanvasmodelblocks, m.countcanvasmodelstickys, m.countcanvasmodelconnections, m.countcanvasmodelimages, m.countimages, m.countsketchmodels, m.countsketchmodelactors, m.countsketchmodelstickys, m.countdiagrammodelbendpoints, m.countdiagrammodelreferences FROM model m WHERE m.model = ? ORDER BY m.version";
				} else {
					request = "MATCH (m:model {model:?}) RETURN m.version as version, m.name as name, m.note as note, m.owner as owner, m.period as period, m.purpose as purpose, m.countmetadatas as countmetadatas, m.countfolders as countfolders, m.countelements as countelements, m.countrelationships as countrelationships, m.countproperties as countproperties, m.countarchimatediagrammodels as countarchimatediagrammodels, m.countdiagrammodelarchimateconnections as countdiagrammodelarchimateconnections, m.countdiagrammodelconnections as countdiagrammodelconnections, m.countdiagrammodelarchimateobjects as countdiagrammodelarchimateobjects, m.countdiagrammodelGroups as countdiagrammodelgroups, m.countdiagrammodelnotes as countdiagrammodelnotes, m.countcanvasmodels as countcanvasmodels, m.countcanvasmodelblocks as countcanvasmodelblocks, m.countcanvasmodelstickys as countcanvasmodelstickys, m.countcanvasmodelconnections as countcanvasmodelconnections, m.countcanvasmodelimages as countcanvasmodelimages, m.countimages as countimages, m.countsketchmodels as countsketchmodels, m.countsketchmodelactors as countsketchmodelactors, m.countsketchmodelstickys as countsketchmodelstickys, m.countdiagrammodelbendpoints as countdiagrammodelbendpoints, m.countdiagrammodelreferences as countdiagrammodelreferences ORDER BY m.version";
				}
				List<HashMap<String, String>> versions = new ArrayList<HashMap<String, String>>();
				String name = "";
				
				ResultSet resultVersions = DBPlugin.select(dbConnection, request, resultModels.getString("model"));
				while(resultVersions.next()) {
					DBPlugin.debug(DebugLevel.Variable, "   Found version "+resultVersions.getString("version"));
					HashMap<String, String> version = new HashMap<String, String>();
					version.put("name", resultVersions.getString("name"));
					version.put("version", resultVersions.getString("version"));
					version.put("note", resultVersions.getString("note"));
					version.put("owner", resultVersions.getString("owner"));
					version.put("period", resultVersions.getString("period"));
					version.put("purpose", resultVersions.getString("purpose"));
					
					version.put("countMetadatas", resultVersions.getString("countmetadatas"));
					version.put("countFolders", resultVersions.getString("countfolders"));
					version.put("countElements", resultVersions.getString("countelements"));
					version.put("countRelationships", resultVersions.getString("countrelationships"));
					version.put("countProperties", resultVersions.getString("countproperties"));
					version.put("countArchimateDiagramModels", resultVersions.getString("countarchimatediagrammodels"));
					version.put("countDiagramModelArchimateConnections", resultVersions.getString("countdiagrammodelarchimateconnections"));
					version.put("countDiagramModelConnections", resultVersions.getString("countdiagrammodelconnections"));
					version.put("countDiagramModelArchimateObjects", resultVersions.getString("countdiagrammodelarchimateobjects"));
					version.put("countDiagramModelGroups", resultVersions.getString("countdiagrammodelgroups"));
					version.put("countDiagramModelNotes", resultVersions.getString("countdiagrammodelnotes"));
					version.put("countCanvasModels", resultVersions.getString("countcanvasmodels"));
					version.put("countCanvasModelBlocks", resultVersions.getString("countcanvasmodelblocks"));
					version.put("countCanvasModelStickys", resultVersions.getString("countcanvasmodelstickys"));
					version.put("countCanvasModelConnections", resultVersions.getString("countcanvasmodelconnections"));
					version.put("countCanvasModelImages", resultVersions.getString("countcanvasmodelimages"));
					version.put("countImages", resultVersions.getString("countimages"));
					version.put("countSketchModels", resultVersions.getString("countsketchmodels"));
					version.put("countSketchModelActors", resultVersions.getString("countsketchmodelactors"));
					version.put("countSketchModelStickys", resultVersions.getString("countsketchmodelstickys"));
					version.put("countDiagramModelBendpoints", resultVersions.getString("countdiagrammodelbendpoints"));
					version.put("countDiagramModelReferences", resultVersions.getString("countdiagrammodelreferences"));
					
					versions.add(version);
					// only the last one will be kept
					name = resultVersions.getString("name");
				}
				modelItem.setText(2, name);
				modelItem.setData("importVersions", versions);
				modelItem.setData("version:selected", -1);
				resultVersions.close();
			}
			resultModels.close();
		} catch (Exception err) {
			if ( databaseTextArea != null )
				databaseTextArea.setBackground(LIGHT_RED_COLOR);
			DBPlugin.popup(Level.Error, "Failed to get model list", err);
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.loadModelList()");
			throw err;
		} finally {
			tblModels.setRedraw(true);
			
			if ( tblModels.getItemCount() > 0 ) {			
				tblModels.setSelection(0);
				tblModels.notifyListeners(SWT.Selection, new Event());		 // calls modelSelected listener
			}
			btnApplyFilter.setEnabled(dbLanguage.equals("SQL"));
		}

		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.loadModelList()");
	}

	/**
	 * Sorts the tableID columns
	 * 
	 * This callback is called each time there is a double click on the columns' header
	 */
	private MouseAdapter sortColumnsAdapter = new MouseAdapter() {
		public void mouseUp(MouseEvent e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.sortColumnsAdapter.mouseUp()");
			Collator collator = Collator.getInstance(Locale.getDefault());

			// we get the table items to sort
			TableItem[] items = tblModels.getItems();
			
			// we calculate the sorted column from the label clicked
			TableColumn sortedColumn;
			Label sortedLabel;
			if ( e.widget.equals(lblId) ) {
				sortedColumn = columnId;
				sortedLabel = lblId;
			} else {
				sortedColumn = columnName;
				sortedLabel = lblName;
			}
			
			// we remove the sorted symbol ( ^ for UP, v for DOWN ) if any from the labels
			lblId.setText(lblId.getText().split(" ")[0]);
			lblName.setText(lblName.getText().split(" ")[0]);
			
			// we calculate the sort direction
			if ( tblModels.getSortColumn() == sortedColumn ) {
				// if the sorted column is already sorted, then we change the sort direction
				tblModels.setSortDirection(tblModels.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
			} else {
				// else the default direction is UP
				tblModels.setSortColumn(sortedColumn);
				tblModels.setSortDirection(SWT.UP);
			}
			
			// we add the sorted symbol ( ^ for UP, v for DOWN ) to the label corresponding to the sorted column
			sortedLabel.setText(sortedLabel.getText()+" "+(tblModels.getSortDirection()==SWT.UP?"^":"v"));
			
			// we calculate the index of the sorted column
			int sortedColumnIndex = -1;
			for ( int col=0; col < tblModels.getColumnCount(); col++) {
				if ( sortedColumn == tblModels.getColumn(col) ) {
					sortedColumnIndex = col;
					break;
				}
			}
			// finally, we sort the table items (bubble algorithm is good enough regarding the number of lines)
			if ( sortedColumnIndex != -1 ) {
				for (int i = 1; i < items.length; i++) {
					String value1 = items[i].getText(sortedColumnIndex);
					for (int j = 0; j < i; j++) {
						String value2 = items[j].getText(sortedColumnIndex);
						if ( (collator.compare(value1, value2) < 0) == (tblModels.getSortDirection() == SWT.UP) ) {
							String[] values = { items[i].getText(0),items[i].getText(1),items[i].getText(2) };
							items[i].dispose();
							new TableItem(tblModels, SWT.NONE, j).setText(values);
							//items = tblModels.getItems();
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
	TableItem formerModelItem = null;
	private Listener modelSelected = new Listener() {
		public void handleEvent(Event e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.modelSelected.handleEvent()");
			
			// If the event is on a checkbox, then we select the corresponding item 
			if ( e.detail == SWT.CHECK ) {
				tblModels.setSelection((TableItem) e.item);
			}
			
			DBPlugin.debug(DebugLevel.Variable, "*** formerModelItem = "+formerModelItem);
			// when a new table item is selected, then we save the information (version, purpose, ...) back to it
			if ( formerModelItem != null ) {
				formerModelItem.setData("name", name.getText());
				formerModelItem.setData("note", note.getText());
				formerModelItem.setData("owner", owner.getText());
				formerModelItem.setData("purpose", purpose.getText());
				
				if ( action == Action.Import ) {
					formerModelItem.setData("version:selected", tblVersions.getSelectionIndex());
				} else {	// action == Action.Export
					formerModelItem.setData("version:actual", actualVersionValue.getText());
					formerModelItem.setData("version:new", getSelectedVersion());
				}
			}
			
			TableItem newVersionItem = tblModels.getSelection()[0];
			id.setText(newVersionItem.getText(1));
			name.setText(newVersionItem.getText(2));
			if ( action == Action.Import ) {
				tblVersions.removeAll();
				@SuppressWarnings("unchecked")
				List<HashMap<String, String>> versions = (List<HashMap<String, String>>)newVersionItem.getData("importVersions");
				if ( versions.size() > 0 ) {
					for ( int i = 0 ; i < versions.size(); ++i ) {
						HashMap<String, String> version = versions.get(i);
						TableItem versionItem = new TableItem(tblVersions, SWT.NONE);
						versionItem.setText(0, version.get("version"));
						versionItem.setText(1, version.get("period"));
					}
					tblVersions.setSelection(versions.size()-1);
					tblVersions.notifyListeners(SWT.Selection, new Event());		 // calls versionSelected listener
				}
			} else {	// action == Action.Export
				setSelectedVersion((String)newVersionItem.getData("version:actual"), (String)newVersionItem.getData("version:new"));
				note.setText((String)newVersionItem.getData("note"));
				owner.setText((String)newVersionItem.getData("owner"));
				purpose.setText((String)newVersionItem.getData("purpose"));
			}
			
			formerModelItem = newVersionItem;
			
			// if there is at least one item selected, the we activate the btnOk
			// else, we deactivate it
			boolean mayValidate = false;
			for ( TableItem modelItem: tblModels.getItems() ) {
				if ( modelItem.getChecked() ) {
					mayValidate = true;
					break;
				}
			}
			btnOK.setEnabled(mayValidate);
			btnChangeId.setEnabled(mayValidate && (action==Action.Import));
			btnDelete.setEnabled(mayValidate && (action==Action.Import));
			
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.modelSelected.handleEvent()");
		}
	};
	
	private Listener versionSelected = new Listener() {
		public void handleEvent(Event e) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.versionSelected.handleEvent()");
			TableItem modelItem = tblModels.getSelection()[0];
			@SuppressWarnings("unchecked")
			HashMap<String, String> version = ((List<HashMap<String, String>>)modelItem.getData("importVersions")).get(tblVersions.getSelectionIndex());		// we are in import mode as the tblVersion deos not exist in export mode
			
			name.setText(version.get("name"));
			purpose.setText(version.get("purpose"));
			owner.setText(version.get("owner"));
			note.setText(version.get("note"));
			
			modelItem.setData("version:selected", tblVersions.getSelectionIndex());
			
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.versionSelected.handleEvent()");
		}
	};
	
	private SelectionListener applyFilterSelected = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.applyFilterSelected.widgetSelected()");
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

			//String filterEltVersion = "";
			//String filterRelVersion = "";
			//String filterPropVersion = "";


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
					//filterEltVersion = "SELECT DISTINCT version FROM archimateelement a WHERE model = ? " + clause;
				} else {
					filterEltModel = "MATCH (a:archimateelement) "+clause+" OPTIONAL MATCH (m:model) WHERE m.model = a.model RETURN DISTINCT m.model as model";
					//filterEltVersion = "MATCH (a:archimateelement) "+clause+" OPTIONAL MATCH (m:model) WHERE m.model = a.model RETURN DISTINCT m.version as version";
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
					//filterRelVersion = "SELECT DISTINCT relationship.version FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version and source.model = ? JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version WHERE target.model = ?";
				} else {
					filterRelModel = "SELECT DISTINCT relationship.model FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version " + clause;
					//filterRelVersion = "SELECT DISTINCT relationship.version FROM relationship JOIN archimateelement source ON relationship.source = source.id AND relationship.model = source.model AND relationship.version = source.version and source.model = ? JOIN archimateelement target ON relationship.target = target.id AND relationship.model = target.model AND relationship.version = target.version " + clause + " AND target.model = ?";
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
					//filterPropVersion = "SELECT DISTINCT version FROM property WHERE model = ?";
				} else {
					filterPropModel = "SELECT DISTINCT model FROM property " + clause;
					//filterPropVersion = "SELECT DISTINCT version FROM property " + clause + " AND model = ?";
				}
			}

			filterModels = "";
			//filterVersions = "";
			if ( !filterEltModel.isEmpty() ) {
				filterModels = " WHERE m1.model IN ("+filterEltModel;
				//filterVersions = " AND m.version IN ("+filterEltVersion;
			}
			if ( !filterRelModel.isEmpty() ) {
				if ( filterModels.isEmpty() ) {
					filterModels = " WHERE m1.model IN ("+filterRelModel;
					//filterVersions = " AND m.version IN ("+filterRelVersion;
				} else {
					filterModels += " INTERSECT "+filterRelModel;
					//filterVersions += " INTERSECT "+filterRelVersion;
				}
			}
			if ( !filterPropModel.isEmpty() ) {
				if ( filterModels.isEmpty() ) {
					filterModels = " WHERE m1.model IN ("+filterPropModel;
					//filterVersions = " AND m.version IN ("+filterPropVersion;
				} else {
					filterModels += " INTERSECT "+filterPropModel;
					//filterVersions += " INTERSECT "+filterPropVersion;
				}
			}
			if ( !filterModels.isEmpty() ) {
				filterModels += ")";
				//filterVersions += ")";
			}

			try {
				loadModelList();
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "Cannot import model list from database.", e);
			}
			
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.applyFilterSelected.widgetSelected()");
		}
		
		public void widgetDefaultSelected(SelectionEvent event) {
			// do nothing
		}
	};
	
	private SelectionListener resetFilterSelected = new SelectionListener() {
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
	
	private SelectionListener cancelFilterSelected = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.CancelFilterListenerButtonCallback.widgetSelected()");
			dialog.setSize(840, 470);
			btnApplyFilter.setText("Filter");
			filterModels = "";
			try {
				loadModelList();
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "Cannot import model list from database.", e);
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.CancelFilterListenerButtonCallback.widgetSelected()");
		}
		
		public void widgetDefaultSelected(SelectionEvent event) {
			// do nothing
		}
	};
	
	private SelectionListener deleteButtonSelected = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.deleteButtonCallback.widgetSelected()");
			MessageDialog confirm = new MessageDialog(dialog, DBPlugin.pluginTitle, null,
					"Warning, you're going to remove the project "+name.getText() + " ("+id.getText()+"). Be aware that this operation cannot be undone !\n\nPlease confirm what you wish to remove exactly ...",
					MessageDialog.QUESTION, new String[] { "Whole model", "version "+tblVersions.getSelection()[0].getText(0)+" only", "cancel" }, 0);
			String msg = "Deleted.";
			try {
				switch ( confirm.open() ) {
				case 0 :
					DBPlugin.debug(DebugLevel.Variable, "Removing whole model !!!");
					dbConnection.setAutoCommit(false);
					if ( dbLanguage.equals("SQL") )
						for(String table: DBPlugin.allSQLTables )
							DBPlugin.request(dbConnection, "DELETE FROM " + table + " WHERE model = ?", id.getText());
					else
						DBPlugin.request(dbConnection, "MATCH (node)-[rm:isInModel]->(model:model {model:?}) DETACH DELETE node, model", id.getText());
					msg = "Model deleted.";
					dbConnection.commit();
					dbConnection.setAutoCommit(true);
					break;
				case 1 :
					DBPlugin.debug(DebugLevel.Variable, "removing one version only");
					dbConnection.setAutoCommit(false);
					if ( dbLanguage.equals("SQL") )
						for(String table: DBPlugin.allSQLTables )
							DBPlugin.request(dbConnection, "DELETE FROM " + table + " WHERE model = ? AND version = ?", id.getText(), tblVersions.getSelection()[0].getText(0));
					else
						DBPlugin.request(dbConnection, "MATCH  (node)-[rm:isInModel]->(model:model {model:?, version:?}) DETACH DELETE node, model", id.getText(), tblVersions.getSelection()[0].getText(0));
					msg = "Version deleted.";
					dbConnection.commit();
					dbConnection.setAutoCommit(true);
					break;
				case 2 :
					DBPlugin.debug(DebugLevel.Variable, "cancelled");
					return;
				}
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "Deleting data : SQL Exception !!!", e);
				DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.deleteButtonCallback.widgetSelected()");
				return;
			}

			DBPlugin.popup(Level.Info, msg);
			try {
				loadModelList();
			} catch (Exception e) {
				Logger.logError("Cannot import model list from database.", e);
			}
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.deleteButtonCallback.widgetSelected()");
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	};
	
	private SelectionListener okButtonSelected = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBSelectModel.okButtonCallback.widgetSelected()");

			// just in case the information just changed, we register
			//if ( action == Action.Export ) {
			//   enregistrer les chmaps name, purpose, etc ...
			//}
			
			selectedModels = new ArrayList<HashMap<String, String>>();

			for ( TableItem modelItem: tblModels.getItems()) {
				if ( modelItem.getChecked() ){
					HashMap<String,String> hash = new HashMap<String,String>();
					
					hash.put("id", modelItem.getText(1));
					
					if ( action == Action.Import ) {
						@SuppressWarnings("unchecked")
						HashMap<String, String> version = ((List<HashMap<String, String>>)modelItem.getData("importVersions")).get((int)modelItem.getData("version:selected"));
						
						hash.put("name", version.get("name"));
						hash.put("purpose", version.get("purpose"));
						hash.put("owner", version.get("owner"));
						hash.put("note", version.get("note"));
						hash.put("period", version.get("period"));
						hash.put("version", version.get("version"));

						hash.put("dbVersion", dbVersion==null?"":dbVersion);
						
						hash.put("mode", btnShared.getSelection() ? "Shared" : "Standalone");
						
						hash.put("countMetadatas", version.get("countMetadatas"));
						hash.put("countFolders", version.get("countFolders"));
						hash.put("countElements", version.get("countElements"));
						hash.put("countRelationships", version.get("countRelationships"));
						hash.put("countProperties", version.get("countProperties"));
						hash.put("countArchimateDiagramModels", version.get("countArchimateDiagramModels"));
						hash.put("countDiagramModelArchimateConnections", version.get("countDiagramModelArchimateConnections"));
						hash.put("countDiagramModelConnections", version.get("countDiagramModelConnections"));
						hash.put("countDiagramModelArchimateObjects", version.get("countDiagramModelArchimateObjects"));
						hash.put("countDiagramModelGroups", version.get("countDiagramModelGroups"));
						hash.put("countDiagramModelNotes", version.get("countDiagramModelNotes"));
						hash.put("countCanvasModels", version.get("countCanvasModels"));
						hash.put("countCanvasModelBlocks", version.get("countCanvasModelBlocks"));
						hash.put("countCanvasModelStickys", version.get("countCanvasModelStickys"));
						hash.put("countCanvasModelConnections", version.get("countCanvasModelConnections"));
						hash.put("countCanvasModelImages", version.get("countCanvasModelImages"));
						hash.put("countImages", version.get("countImages"));
						hash.put("countSketchModels", version.get("countSketchModels"));
						hash.put("countSketchModelActors", version.get("countSketchModelActors"));
						hash.put("countSketchModelStickys", version.get("countSketchModelStickys"));
						hash.put("countDiagramModelBendpoints", version.get("countDiagramModelBendpoints"));
						hash.put("countDiagramModelReferences", version.get("countDiagramModelReferences"));
						//TODO:
						//TODO:
						//TODO:
						//TODO:
						//TODO: check for dependencies
						//TODO:   if the dependency is in the tblModels, then, just check it and add it in the HashMap
						//TODO:   else, we have to request the database
						//TODO:      /!\ what to do if the relationship does not point to the latest model version ???
					} else {	//if ( action == Action.Export )
						hash.put("name", (String)modelItem.getData("name"));
						hash.put("purpose", (String)modelItem.getData("purpose"));
						hash.put("owner", (String)modelItem.getData("owner"));
						hash.put("note", (String)modelItem.getData("note"));
						hash.put("version", (String)modelItem.getData("version:new"));
						hash.put("period", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
					}
					selectedModels.add(hash);
				}
			}
			dialog.close();
			DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBSelectModel.okButtonCallback.widgetSelected()");
		}
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}
	};
	
	SelectionListener cancelButtonSelected = new SelectionListener() {
		public void widgetSelected(SelectionEvent e) { 
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.cancelButtonCallback.widgetSelected()");
			dialog.close();
		}
		public void widgetDefaultSelected(SelectionEvent e) { 
			// do nothing
		}
	};
	
	SelectionListener setPreferencesSelected = new SelectionListener() {
		public void widgetSelected(SelectionEvent e) {
			DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.setPreferencesButtonCallback.widgetSelected()");
			DBPlugin.debug(DebugLevel.Variable, "Openning preference page ...");
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "org.archicontribs.database.DBPreferencePage", null, null);
			dialog.setBlockOnOpen(true);
			if ( dialog.open() == 0 ) {
				DBPlugin.debug(DebugLevel.Variable, "Resetting settings from preferences ...");
				databaseEntries = DBDatabaseEntry.getAllFromPreferenceStore();
				database.removeAll();
				for (DBDatabaseEntry databaseEntry: databaseEntries) {
					database.add(databaseEntry.getName());
				}
				// we fill in the models from the default database
				if ( databaseEntries.size() > 0 ) {
					database.select(0);
					database.notifyListeners(SWT.Selection, new Event());
				}
			} else {
				DBPlugin.debug(DebugLevel.Variable, "Preferences cancelled ...");
			}
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.setPreferencesButtonCallback.widgetSelected()");
		}
		public void widgetDefaultSelected(SelectionEvent e) { 
			// do nothing
		}
	};
	
	private Listener verifyId = new Listener() {
		public void handleEvent(Event event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.verifyIdListener.handleEvent("+DBPlugin.getEventName(event.type)+")");
			String value = id.getText().substring(0, event.start) + event.text + id.getText().substring(event.end);
			event.doit = value.matches("^%?[a-zA-Z0-9]+$");
		}
	};
	
	private Listener validateCustomVersion = new Listener() {
		public void handleEvent(Event event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBSelectModel.validateCustomVersionListener.handleEvent("+DBPlugin.getEventName(event.type)+")");
			try {
				String value = customVersionValue.getText().substring(0, event.start) + event.text + customVersionValue.getText().substring(event.end);
				if ( !value.isEmpty() ) {
					Float.valueOf(value);
					String values[] = value.split("\\.");
					if ( values[0].length() > 5 ) throw new Exception();
					if ( values.length == 2 && values[1].length() > 5 ) throw new Exception();

					checkActualVersion.setSelection(false);
					checkMinorVersion.setSelection(false);
					checkMajorVersion.setSelection(false);
					checkCustomVersion.setSelection(true);
				}
			} catch (Exception ee) {
				event.doit = false;
			}
		}
	};

	private List<HashMap<String, String>> selectedModels = null;
	public List<HashMap<String, String>> getSelectedModels() {
		// The selectedModels property is :
		//     null if the user clicked on the cancel button
		//     filled in by the okButtonCallback if the user clicked on the OK button
		return selectedModels;
	}
	
	public String getDbLanguage() {
		return dbLanguage;
	}
	
	/**
	 * @return the value of the text widget next to the selected radio button in the versionGrp
	 */
    private String getSelectedVersion() {
        if ( action == Action.Export ) {
            if ( checkActualVersion.getSelection() )
                return actualVersionValue.getText();
            if ( checkMinorVersion.getSelection() )
                return minorVersionValue.getText();
            if ( checkMajorVersion.getSelection() )
                return majorVersionValue.getText();
            return customVersionValue.getText();
        }
        return "0.0";
    }
    
	/**
	 * set the value of the text widget next to the selected radio button in the versionGrp
	 */
    private void setSelectedVersion(String actualVersion, String newVersion) {
    	DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBSelectModel.open()");
    	
    	assert (action == Action.Export );
    	
    	DBPlugin.debug(DebugLevel.Variable, "actualVersion = \""+actualVersion+ "\"   , new Version = \""+newVersion+"\"");
    	String minor = DBPlugin.incMinor(actualVersion);
		String major = DBPlugin.incMajor(actualVersion);
		
		checkActualVersion.setSelection(false);
		checkMinorVersion.setSelection(false);
		checkMajorVersion.setSelection(false);
		checkCustomVersion.setSelection(false);
		
		actualVersionValue.setText(actualVersion);
		if ( newVersion.equals(actualVersion) )
			checkActualVersion.setSelection(true);
		
		minorVersionValue.setText(minor);
		if ( newVersion.equals(minor) )
			checkMinorVersion.setSelection(true);
		
    	majorVersionValue.setText(major);
    	if ( newVersion.equals(major) )
    		checkMajorVersion.setSelection(true);
    	
    	if ( !checkActualVersion.getSelection() && ! checkMinorVersion.getSelection() && !checkMajorVersion.getSelection() ) {
    		checkCustomVersion.setSelection(true);
    		customVersionValue.setText(newVersion);
    	}
    
    	DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBSelectModel.open()");
    }
	
	private Shell dialog;
	private DBModel dbModel;
	private CCombo database;
	private Text databaseTextArea;
	private Action action = Action.Unknown;
	private Label lblId;
	private Text id;
	private Label lblName;
	private Text name;
	private Table tblModels;
	private TableColumn columnId;
	private TableColumn columnName;
	private ScrolledComposite compositeVersion;
	private Group versionGrp;
	private Table tblVersions;
	private Button checkActualVersion;
	private Text actualVersionValue;
	private Button checkMinorVersion;
	private Text minorVersionValue;
	private Button checkMajorVersion;
	private Text majorVersionValue;
	private Button checkCustomVersion;
	private Text customVersionValue;
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
	private Scale scaleDepth;
	private Label lblDepth;
	private Label lblAutoDepth;

	private Text purpose;
	private Text owner;
	private Text note;
	private Button btnOK;
	private Button btnCancel;
}