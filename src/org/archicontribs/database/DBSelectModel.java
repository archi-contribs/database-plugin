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

import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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


//Sorry if the code is not optimized, but I used a generator :)
public class DBSelectModel extends Dialog {
	private enum Action { Unknown, Import, Export };
	private DBList selectedModels = null;

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

	/**
	 * Create the dialog.
	 */
	public DBSelectModel() {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/**
	 * Open the dialog.
	 * @param _db
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
	public DBList open(Connection _db, DBModel _dbModel) throws SQLException {
		dbModel = _dbModel;
		action = Action.Export;
		return open(_db);
	}
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public DBList open(Connection _db) throws SQLException {
		db = _db;
		oldSelectedItemIndex = -1;
		if ( action == Action.Unknown )
			action = Action.Import;
		Display display = getParent().getDisplay();
		createContents();
		setInitialValues();
		dialog.open();
		dialog.layout();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return selectedModels;
	}

	/*
	 * Create contents of the dialog.
	 */
	private void createContents() {
		Color grey = new Color(null, 220,220,220);
		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setSize(840, 700);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 2);
		dialog.setLayout(null);

		/**************************************************/
		
		Composite compositeId = new Composite(dialog, SWT.BORDER);
		compositeId.setBounds(10, 30, 103, 18);
		
		lblId = new Label(compositeId, SWT.NONE);
		lblId.setAlignment(SWT.CENTER);
		lblId.setBounds(0, 0, 103, 18);
		lblId.setText("ID");
		lblId.addMouseListener(sortColumns);

		id = new Text(dialog, SWT.BORDER);
		id.setBounds(10, 10, 103, 21);
		id.setEditable(action == Action.Export);
		id.setTextLimit(11);		// 5 digits before the dot, 5 digits after plus the dot itself
		id.addListener(SWT.Verify, new Listener() { public void handleEvent(Event e) { e.doit = !e.text.matches("^.*[^a-zA-Z0-9 ].*$"); }});
		
		Composite compositeName = new Composite(dialog, SWT.BORDER);
		compositeName.setBounds(112, 30, 298, 18);
		
		lblName = new Label(compositeName, SWT.NONE);
		lblName.setAlignment(SWT.CENTER);
		lblName.setBounds(0, 0, 298, 18);
		lblName.setText("Name");
		lblName.addMouseListener(sortColumns);

		name = new Text(dialog, SWT.BORDER);
		name.setTextLimit(255);
		name.setBounds(112, 10, 298, 21);
		name.setEditable(action == Action.Export);

		/**************************************************/

		ScrolledComposite compositeTblId = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
		compositeTblId.setBounds(10, 47, 400, 320);
		compositeTblId.setAlwaysShowScrollBars(true);
		compositeTblId.setExpandHorizontal(true);
		compositeTblId.setExpandVertical(true);

		TableViewer tableViewerId = new TableViewer(compositeTblId, SWT.FULL_SELECTION);
		tblId = tableViewerId.getTable();
		tblId.setLinesVisible(true);
		tblId.addListener(SWT.Selection, selectModelListener);
		// if Action is Import, then the double click is equivallent to the OK button
		if ( action == Action.Import )
			tblId.addListener(SWT.MouseDoubleClick, new Listener() { public void handleEvent(Event event) { btnOK.notifyListeners(SWT.Selection, new Event()); }});

		compositeTblId.setContent(tblId);
		compositeTblId.setMinSize(tblId.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		columnId = new TableColumn(tblId, SWT.NONE);
		columnId.setResizable(false);
		columnId.setMoveable(true);
		columnId.setWidth(100);
		columnId.setText("ID");
		columnId.setData( new Label[]{lblId, lblName} );

		columnName = new TableColumn(tblId, SWT.NONE);
		columnName.setResizable(false);
		columnName.setWidth(280);
		columnName.setText("Name");
		columnName.setData(new Label[]{lblName, lblId});

		/**************************************************/

		Label lblVersion = new Label(dialog, SWT.NONE);
		lblVersion.setBounds(420, 10, 55, 15);
		lblVersion.setText("Version :");

		compositeVersion = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
		compositeVersion.setExpandVertical(true);
		compositeVersion.setExpandHorizontal(true);
		compositeVersion.setAlwaysShowScrollBars(true);
		compositeVersion.setBounds(520, 10, 305, 92);

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

		versionGrp = new Group(dialog, SWT.NONE);
		versionGrp.setBounds(520, 5, 205, 102);
		versionGrp.setLayout(null);

		checkActual = new Button(versionGrp, SWT.RADIO);
		checkActual.setBounds(6, 18, 96, 16);
		checkActual.setEnabled(true);
		checkActual.setText("actual version:");

		checkMinor = new Button(versionGrp, SWT.RADIO);
		checkMinor.setBounds(6, 37, 101, 16);
		checkMinor.setEnabled(true);
		checkMinor.setSelection(true);
		checkMinor.setText("minor change:");


		checkMajor = new Button(versionGrp, SWT.RADIO);
		checkMajor.setBounds(6, 56, 100, 16);
		checkMajor.setEnabled(true);
		checkMajor.setText("major change:");


		checkCustom = new Button(versionGrp, SWT.RADIO);
		checkCustom.setBounds(6, 75, 111, 16);
		checkCustom.setEnabled(true);
		checkCustom.setText("custom version: ");

		actualVersion = new Text(versionGrp, SWT.NONE);
		actualVersion.setLocation(120, 18);
		actualVersion.setSize(68, 15);
		actualVersion.setEnabled(false);

		minorVersion = new Text(versionGrp, SWT.NONE);
		minorVersion.setLocation(120, 37);
		minorVersion.setSize(68, 15);
		minorVersion.setEnabled(false);

		majorVersion = new Text(versionGrp, SWT.NONE);
		majorVersion.setLocation(120, 56);
		majorVersion.setSize(68, 15);
		majorVersion.setEnabled(false);

		customVersion = new Text(versionGrp, SWT.NONE);
		customVersion.setLocation(120, 75);
		customVersion.setSize(70, 15);
		customVersion.setEnabled(true);
		customVersion.addListener(SWT.Verify, new Listener() {
			public void handleEvent(Event e) {
				try {
					String value = customVersion.getText().substring(0, e.start) + e.text + customVersion.getText().substring(e.end);
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
					e.doit = false;
				}
			}
		});

		/**************************************************/

		Label lblNote = new Label(dialog, SWT.NONE);
		lblNote.setBounds(426, 113, 89, 15);
		lblNote.setText("Release note :");

		note = new Text(dialog, SWT.BORDER);
		note.setBounds(520, 110, 305, 21);
		note.setEditable(action == Action.Export);

		/**************************************************/

		Label lblOwner = new Label(dialog, SWT.NONE);
		lblOwner.setBounds(426, 138, 55, 15);
		lblOwner.setText("Owner :");

		owner = new Text(dialog, SWT.BORDER);
		owner.setBounds(520, 135, 150, 21);
		if ( action == Action.Export ) {
			owner.setText(System.getProperty("user.name"));
		} else {
			owner.setEditable(false);
		}

		/**************************************************/

		Label lblPurpose = new Label(dialog, SWT.NONE);
		lblPurpose.setBounds(426, 163, 55, 15);
		lblPurpose.setText("Purpose :");

		purpose = new Text(dialog, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		purpose.setBounds(520, 160, 305, 135);
		purpose.setEditable(action == Action.Export);

		/**************************************************/

		lblMode = new Label(dialog, SWT.NONE);
		lblMode.setBounds(426, 305, 80, 15);
		lblMode.setText(action==Action.Import ? "Import mode :" : "Export :");

		grpMode = new Group(dialog, SWT.NONE);
		grpMode.setBounds(521, 295, 305, 30);
		grpMode.setLayout(null);

		btnShared = new Button(grpMode, SWT.RADIO);
		btnShared.setBounds(6, 10, 96, 16);
		btnShared.setSelection(true);
		btnShared.setText(action==Action.Import ? "Shared" : "Export");
		if ( action == Action.Import ) {
			btnShared.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					lblAutoDepth.setVisible(true);
					lblDepth.setVisible(true);
					scaleDepth.setVisible(true);
				}
			});
		}

		btnStandalone = new Button(grpMode, SWT.RADIO);
		btnStandalone.setBounds(150, 10, 96, 16);
		btnStandalone.setText(action==Action.Import ? "Standalone" : "Do not Export");
		if ( action == Action.Import ) {
			btnStandalone.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					lblAutoDepth.setVisible(false);
					lblDepth.setVisible(false);
					scaleDepth.setVisible(false);
				}
			});
		}

		/**************************************************/

		btnOK = new Button(dialog, SWT.PUSH);
		btnOK.setBounds(668, 379, 75, 25);
		btnOK.setText(action==Action.Import ? "Import" : "Export");
		btnOK.addSelectionListener(BtnOKListener);

		btnCancel = new Button(dialog, SWT.PUSH);
		btnCancel.setBounds(749, 379, 75, 25);
		btnCancel.setText("Cancel");

		btnDelete = new Button(dialog, SWT.NONE);
		btnDelete.setBounds(10, 379, 75, 25);
		btnDelete.setText("Delete");
		btnDelete.setEnabled(action==Action.Import);
		btnDelete.addSelectionListener(BtnDeleteListener);

		btnChangeId = new Button(dialog, SWT.NONE);
		btnChangeId.setText("Change ID");
		btnChangeId.setEnabled(false);
		btnChangeId.setBounds(100, 379, 75, 25);

		btnApplyFilter = new Button(dialog, SWT.NONE);
		btnApplyFilter.setText("Filter");
		btnApplyFilter.setBounds(190, 379, 75, 25);
		btnApplyFilter.addSelectionListener(BtnApplyFilterListener);

		/**************************************************/

		lblElements = new Label(dialog, SWT.NONE);
		lblElements.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		lblElements.setText("Elements :");
		lblElements.setBackground(grey);
		lblElements.setBounds(10, 416, 80, 15);

		lblRelations = new Label(dialog, SWT.NONE);
		lblRelations.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		lblRelations.setText("Relationships :");
		lblRelations.setBackground(grey);
		lblRelations.setBounds(10, 541, 80, 15);

		lblProperties = new Label(dialog, SWT.NONE);
		lblProperties.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		lblProperties.setText("Properties :");
		lblProperties.setBackground(grey);
		lblProperties.setBounds(485, 416, 80, 15);

		Group ElementsGrp = new Group(dialog, SWT.NONE);
		ElementsGrp.setBounds(10, 425, 465, 107);
		ElementsGrp.setBackground(grey);
		ElementsGrp.setLayout(null);

		lblEltLayer = new Label(ElementsGrp, SWT.NONE);
		lblEltLayer.setText("Layer :");
		lblEltLayer.setBounds(15, 5, 36, 15);
		lblEltLayer.setBackground(grey);		

		Label lblEltType = new Label(ElementsGrp, SWT.NONE);
		lblEltType.setText("Type :");
		lblEltType.setBounds(140, 5, 36, 15);
		lblEltType.setBackground(grey);

		lblEltName = new Label(ElementsGrp, SWT.NONE);
		lblEltName.setText("Name :");
		lblEltName.setBounds(285, 5, 44, 15);
		lblEltName.setBackground(grey);

		comboEltLayer1 = new Combo(ElementsGrp, SWT.NONE);
		comboEltLayer1.setBounds(5, 20, 130, 23);
		comboEltLayer1.setItems(ElementLayers);
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
		comboEltLayer2 = new Combo(ElementsGrp, SWT.NONE);
		comboEltLayer2.setBounds(5, 48, 130, 23);
		comboEltLayer2.setItems(ElementLayers);
		comboEltLayer2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltLayer3 = new Combo(ElementsGrp, SWT.NONE);
		comboEltLayer3.setBounds(5, 76, 130, 23);
		comboEltLayer3.setItems(ElementLayers);
		comboEltLayer3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltType1 = new Combo(ElementsGrp, SWT.NONE);
		comboEltType1.setBounds(140, 20, 140, 23);
		comboEltType1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltType2 = new Combo(ElementsGrp, SWT.NONE);
		comboEltType2.setBounds(140, 48, 140, 23);
		comboEltType2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		comboEltType3 = new Combo(ElementsGrp, SWT.NONE);
		comboEltType3.setBounds(140, 76, 140, 23);
		comboEltType3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if ( e.character != 0 )
					e.doit = false;
			}
		});

		txtEltName1 = new Text(ElementsGrp, SWT.BORDER);
		txtEltName1.setBounds(285, 21, 170, 21);

		txtEltName2 = new Text(ElementsGrp, SWT.BORDER);
		txtEltName2.setBounds(285, 49, 170, 21);

		txtEltName3 = new Text(ElementsGrp, SWT.BORDER);
		txtEltName3.setBounds(285, 77, 170, 21);



		grpProperties = new Group(dialog, SWT.NONE);
		grpProperties.setLayout(null);
		grpProperties.setBackground(grey);
		grpProperties.setBounds(485, 425, 338, 107);

		lblPropName = new Label(grpProperties, SWT.NONE);
		lblPropName.setText("Name :");
		lblPropName.setBounds(10, 5, 36, 15);
		lblPropName.setBackground(grey);

		lblPropValue = new Label(grpProperties, SWT.NONE);
		lblPropValue.setText("Value :");
		lblPropValue.setBounds(140, 5, 44, 15);
		lblPropValue.setBackground(grey);

		TxtPropName1 = new Text(grpProperties, SWT.BORDER);
		TxtPropName1.setBounds(10, 21, 125, 21);

		TxtPropName2 = new Text(grpProperties, SWT.BORDER);
		TxtPropName2.setBounds(10, 49, 125, 21);

		TxtPropName3 = new Text(grpProperties, SWT.BORDER);
		TxtPropName3.setBounds(10, 77, 125, 21);

		TxtPropValue1 = new Text(grpProperties, SWT.BORDER);
		TxtPropValue1.setBounds(140, 21, 188, 21);

		TxtPropValue2 = new Text(grpProperties, SWT.BORDER);
		TxtPropValue2.setBounds(140, 49, 188, 21);

		TxtPropValue3 = new Text(grpProperties, SWT.BORDER);
		TxtPropValue3.setBounds(140, 77, 188, 21);

		grpRelations = new Group(dialog, SWT.NONE);
		grpRelations.setLayout(null);
		grpRelations.setBounds(10, 550, 465, 111);
		grpRelations.setBackground(grey);

		lblRelType = new Label(grpRelations, SWT.NONE);
		lblRelType.setText("Type :");
		lblRelType.setBounds(5, 5, 36, 15);
		lblRelType.setBackground(grey);

		lblRelSource = new Label(grpRelations, SWT.NONE);
		lblRelSource.setText("Source :");
		lblRelSource.setBounds(180, 5, 44, 15);
		lblRelSource.setBackground(grey);

		lblRelTarget = new Label(grpRelations, SWT.NONE);
		lblRelTarget.setText("Target :");
		lblRelTarget.setBounds(320, 5, 44, 15);
		lblRelTarget.setBackground(grey);

		comboRelationship1 = new Combo(grpRelations, SWT.NONE);
		comboRelationship1.setBounds(5, 20, 170, 23);
		comboRelationship1.setItems(Relationships);

		comboRelationship2 = new Combo(grpRelations, SWT.NONE);
		comboRelationship2.setBounds(5, 48, 170, 23);
		comboRelationship2.setItems(Relationships);

		comboRelationship3 = new Combo(grpRelations, SWT.NONE);
		comboRelationship3.setBounds(5, 76, 170, 23);
		comboRelationship3.setItems(Relationships);

		TxtSource1 = new Text(grpRelations, SWT.BORDER);
		TxtSource1.setBounds(180, 21, 135, 21);

		TxtSource2 = new Text(grpRelations, SWT.BORDER);
		TxtSource2.setBounds(180, 49, 135, 21);

		TxtSource3 = new Text(grpRelations, SWT.BORDER);
		TxtSource3.setBounds(180, 77, 135, 21);

		TxtTarget1 = new Text(grpRelations, SWT.BORDER);
		TxtTarget1.setBounds(320, 21, 135, 21);

		TxtTarget2 = new Text(grpRelations, SWT.BORDER);
		TxtTarget2.setBounds(320, 49, 135, 21);

		TxtTarget3 = new Text(grpRelations, SWT.BORDER);
		TxtTarget3.setBounds(320, 77, 135, 21);

		Label lblHelp = new Label(dialog, SWT.WRAP);
		lblHelp.setText("The search is not case sensitive.\nYou may use the '%' character as wildcard.");
		lblHelp.setBounds(485, 550, 250, 50);

		btnCancelFilter = new Button(dialog, SWT.NONE);
		btnCancelFilter.setText("Cancel filter");
		btnCancelFilter.setBounds(749, 639, 75, 25);
		btnCancelFilter.setVisible(false);
		
		lblAutoDepth = new Label(dialog, SWT.NONE);
		lblAutoDepth.setBounds(426, 345, 89, 15);
		lblAutoDepth.setText("Auto depth :");
		lblAutoDepth.setVisible(action == Action.Import);
		
		lblDepth = new Label(dialog, SWT.NONE);
		lblDepth.setBounds(520, 345, 55, 15);
		lblDepth.setText("Infinite");
		lblDepth.setVisible(action == Action.Import);
		
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
		scaleDepth.setBounds(581, 331, 244, 40);
		scaleDepth.setVisible(action == Action.Import);
		
		btnCancelFilter.addSelectionListener(BtnCancelFilterListener);
		
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) { dialog.close(); }
		});
		//TODO: change the focus tab list
		//Control[] list = new Control[] { c1, b6, tb1, c4, c2, l2 }; shell.setTabList(list);
	}

	/*
	 * Fills in initial values.
	 * 
	 * This method is called once, right after the dialog is opened 
	 */
	private void setInitialValues() throws SQLException{
		dialog.setSize(840, 440);

		// in export mode, we populate the model list with the model exported only
		if ( action == Action.Export ) {
			// if the model selected is the shared one, then we export all the models
			// we get the list from subfolders of DBPlugin.SharedFolderName
			if ( dbModel.getModel().getId().equals(DBPlugin.SharedModelId) ) {
				if ( dbModel.getAllModels() != null ) {
					for ( IFolder f: dbModel.getAllModels() ) {
						DBObject dbObject = new DBObject(dbModel, f);
						TableItem tableItem = new TableItem(tblId, SWT.NONE);
						tableItem.setText(0, dbObject.getModelId());
						tableItem.setText(1, dbObject.getName());
						
						tableItem.setData("id", dbObject.getModelId());
						tableItem.setData("name", dbObject.getName());
						tableItem.setData("purpose", dbModel.getPurpose()!=null ? dbObject.getDocumentation() : "");
						tableItem.setData("owner", System.getProperty("user.name"));
						tableItem.setData("note", "");
						tableItem.setData("export", true);
						tableItem.setData("actual_version", dbObject.getVersion());
						tableItem.setData("version", DBPlugin.incMinor(dbObject.getVersion()));
					}
				}
			} else {
				// if the model selected is not the shared one, then we export the selected model only 
				TableItem tableItem = new TableItem(tblId, SWT.NONE);
				tableItem.setText(0, dbModel.getModelId());
				tableItem.setText(1, dbModel.getName());

/*				id.setText(dbModel.getModelId());
				name.setText(dbModel.getName());
				purpose.setText(dbModel.getPurpose()!=null ? dbModel.getPurpose() : "");
				actualVersion.setText(dbModel.getVersion());
				minorVersion.setText(DBPlugin.incMinor(dbModel.getVersion()));
				majorVersion.setText(DBPlugin.incMajor(dbModel.getVersion()));
				customVersion.setText("");
				checkMinor.setSelection(true);*/

				tableItem.setData("id", dbModel.getModelId());
				tableItem.setData("name", dbModel.getName());
				tableItem.setData("purpose", dbModel.getPurpose()!=null ? dbModel.getPurpose() : "");
				tableItem.setData("owner", System.getProperty("user.name"));
				tableItem.setData("note", "");
				tableItem.setData("export", btnShared.getSelection());
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
			loadValuesFromDatabase();
		}
		compositeVersion.setVisible(action==Action.Import);
		versionGrp.setVisible(action == Action.Export);
		btnApplyFilter.setEnabled(action==Action.Import);
	}

	/*
	 * load the models id, name, versions from the database.
	 * 
	 *  This method is called when the method is opened and each time a filter is applied or cancelled. 
	 */
	private void loadValuesFromDatabase() throws SQLException {
		//System.out.println("loadValuesFromDatabase : SELECT m1.model, m1.name, m1.version FROM model AS m1 JOIN (SELECT model, MAX(version) AS version FROM model GROUP BY model) AS m2 ON m1.model = m2.model and m1.version = m2.version" + filterModels);
		ResultSet result = DBPlugin.select(db, "SELECT m1.model, m1.name, m1.version FROM model AS m1 JOIN (SELECT model, MAX(version) AS version FROM model GROUP BY model) AS m2 ON m1.model = m2.model and m1.version = m2.version" + filterModels);
		tblId.setRedraw(false);
		tblId.removeAll();
		tblVersion.removeAll();
		while(result.next()) {
			TableItem tableItem = new TableItem(tblId, SWT.NONE);
			tableItem.setText(0, result.getString("model"));
			tableItem.setText(1, result.getString("name"));
			if ( action == Action.Export  && dbModel.getModelId().equals(result.getString("model")) ) {
				tblId.setSelection(tableItem);
				tblId.notifyListeners(SWT.Selection, new Event());
				checkActual.setEnabled(true);
				actualVersion.setText(result.getString("version"));
				minorVersion.setText(DBPlugin.incMinor(result.getString("version")));
				majorVersion.setText(DBPlugin.incMajor(result.getString("version")));
			}
		}
		if ( tblId.getItems().length > 0 ) {			
			tblId.setSelection(0);
			tblId.notifyListeners(SWT.Selection, new Event());
		}
		tblId.setRedraw(true);
		result.close();
	}

	/*
	 * Sorts the tableID columns
	 * 
	 * This callback is called each time there is a double click on the columns' header
	 */
	private MouseAdapter sortColumns = new MouseAdapter() {
		public void mouseUp(MouseEvent e) {
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
			try {
				ResultSet versions;

				if ( action == Action.Import ) {
					// In Import mode, we got the information from the database, including all the versions stored in the database
					//System.out.println("selectModelListener : SELECT * FROM model WHERE model = ? " + filterVersions + " ORDER BY version >>> " + tblId.getSelection()[0].getText() + "<<<");
					versions = DBPlugin.select(db, "SELECT * FROM model WHERE model = ? " + filterVersions + " ORDER BY version", tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText(), tblId.getSelection()[0].getText());

					tblVersion.removeAll();
					while ( versions.next() ) {
						id.setText(versions.getString("model") == null ? "" : versions.getString("model"));
						name.setText(versions.getString("name") == null ? "" : versions.getString("name"));
						purpose.setText(versions.getString("purpose") == null ? "" : versions.getString("purpose"));

						TableItem tableItem = new TableItem(tblVersion, SWT.NONE);
						tableItem.setText(0, versions.getString("version"));
						tableItem.setText(1, versions.getString("period"));
						tableItem.setData("name", versions.getString("name"));
						tableItem.setData("purpose", versions.getString("purpose"));
						tableItem.setData("owner", versions.getString("owner"));
						tableItem.setData("note", versions.getString("note"));
						tblVersion.setSelection(tableItem);
					}
					versions.close();
				} else {
					// in Export mode, we get the information from the data previously set in the tblId list
					if ( oldSelectedItemIndex >= 0 ) {
						TableItem tableItem = tblId.getItem(oldSelectedItemIndex);
						tableItem.setData("id", id.getText());
						tableItem.setData("name", name.getText());
						tableItem.setData("purpose", purpose.getText());
						tableItem.setData("owner", owner.getText());
						tableItem.setData("note", note.getText());
						tableItem.setData("export", btnShared.getSelection());
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

					if ( (boolean)tblId.getSelection()[0].getData("export") ) {
						btnShared.setSelection(true);
						btnStandalone.setSelection(false);
					} else {
						btnShared.setSelection(false);
						btnStandalone.setSelection(true);
					}
				}
			} catch (SQLException ee) {
				Logger.logError("Cannot retreive details about model " + tblId.getSelection()[0].getText(), ee);
			}
		}
	};
	private SelectionListener BtnApplyFilterListener = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			dialog.setSize(840, 700);
			btnCancelFilter.setVisible(true);
			btnApplyFilter.setText("Apply filter");

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
					clause1 = "type = '"+DBPlugin.capitalize(comboEltType1.getText())+"'";
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
					clause1 += (clause1.isEmpty() ? "" : " AND ") + "UPPER(name) LIKE UPPER('"+txtEltName1.getText()+"')";

				if ( !comboEltType2.getText().isEmpty() )
					clause2 = "type = '"+comboEltType2.getText()+"'";
				else if ( !comboEltLayer2.getText().isEmpty() ) {
					String[] layer = null;;
					clause2 = "type IN (";
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
					clause2 += (clause2.isEmpty() ? "" : " AND ") + "UPPER(name) LIKE UPPER('"+txtEltName2.getText()+"')";

				if ( !comboEltType3.getText().isEmpty() )
					clause3 = "type = '"+comboEltType3.getText()+"'";
				else if ( !comboEltLayer3.getText().isEmpty() ) {
					String[] layer = null;;
					clause3 = "type IN (";
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
					clause3 += (clause3.isEmpty() ? "" : " AND ") + "UPPER(name) LIKE UPPER('"+txtEltName3.getText()+"')";


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
					filterEltModel = "SELECT DISTINCT model FROM archimateelement";
					filterEltVersion = "SELECT DISTINCT version FROM archimateelement WHERE model = ?";
				} else {
					filterEltModel = "SELECT DISTINCT model FROM archimateelement " + clause;
					filterEltVersion = "SELECT DISTINCT version FROM archimateelement " + clause + " AND model = ?";
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
				filterVersions = " AND version IN ("+filterEltVersion;
			}
			if ( !filterRelModel.isEmpty() ) {
				if ( filterModels.isEmpty() ) {
					filterModels = " WHERE m1.model IN ("+filterRelModel;
					filterVersions = " AND version IN ("+filterRelVersion;
				} else {
					filterModels += " INTERSECT "+filterRelModel;
					filterVersions += " INTERSECT "+filterRelVersion;
				}
			}
			if ( !filterPropModel.isEmpty() ) {
				if ( filterModels.isEmpty() ) {
					filterModels = " WHERE m1.model IN ("+filterPropModel;
					filterVersions = " AND version IN ("+filterPropVersion;
				} else {
					filterModels += " INTERSECT "+filterPropModel;
					filterVersions += " INTERSECT "+filterPropVersion;
				}
			}
			if ( !filterModels.isEmpty() ) {
				filterModels += ")";
				filterVersions += ")";
			}

			try { tblId.removeAll(); loadValuesFromDatabase(); } catch (SQLException e) { Logger.logError("loadValuesFromDatabase : SQL Exception !!!", e); }
		}
		public void widgetDefaultSelected(SelectionEvent event) {}
	};
	private SelectionListener BtnCancelFilterListener = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			dialog.setSize(840, 440);
			btnCancelFilter.setVisible(false);
			btnApplyFilter.setText("Filter");
			filterModels = "";
			try { tblId.removeAll(); loadValuesFromDatabase(); } catch (SQLException e) { Logger.logError("loadValuesFromDatabase : SQL Exception !!!", e); }
		}
		public void widgetDefaultSelected(SelectionEvent event) {}
	};
	private SelectionListener BtnDeleteListener = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			MessageDialog confirm = new MessageDialog(dialog, DBPlugin.pluginTitle, null,
				    "Warning, you're going to remove the project "+name.getText() + " ("+id.getText()+"). Be aware that this operation cannot be undone !\n\nPlease confirm what you wish to remove exactly ...",
				    MessageDialog.QUESTION, new String[] { "Whole model", "version "+tblVersion.getSelection()[0].getText(0)+" only", "cancel" }, 0);
			String where = null;
			switch ( confirm.open() ) {
			case 0 :
				DBPlugin.debug("Removing whole model !!!");
				where = "WHERE model = ?";
				break;
			case 1 :
				DBPlugin.debug("removing one version only");
				where = "WHERE model = ? and VERSION = ?";
				break;
			case 2 :
				DBPlugin.debug("cancelled");
				return;
			}
			for(String table: DBPlugin.allTables ) {
				DBPlugin.debug("   table " + table);
				try {
					DBPlugin.sql(db, "DELETE FROM "+table+" "+where, id.getText(), tblVersion.getSelection()[0].getText(0));
					db.commit();
				} catch (SQLException e) { Logger.logError("delete from +"+table+" : SQL Exception !!!", e); }
			}
			try {
				loadValuesFromDatabase();
			} catch (SQLException e) { Logger.logError("loadValuesFromDatabase : SQL Exception !!!", e); }
		}
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	private SelectionListener BtnOKListener = new SelectionListener() {
		public void widgetSelected(SelectionEvent event) {
			if ( !id.getText().isEmpty() ) { 
				selectedModels = new DBList();

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
					selectedModels.put(id.getText(), hash);
				} else {
					// if Action is Export, then the selectedModels list must contain one entry per model to export

					// we force to register the text fields into the data map of the tableItem
					tblId.setSelection(tblId.getSelectionIndex());
					tblId.notifyListeners(SWT.Selection, new Event());
					for ( TableItem tableItem: tblId.getItems() ) {
						if ( (boolean)tableItem.getData("export") ) {
							HashMap<String,String> hash = new HashMap<String,String>();
							hash.put("id", (String)tableItem.getData("id"));
							hash.put("name", (String)tableItem.getData("name"));
							hash.put("purpose", (String)tableItem.getData("purpose"));
							hash.put("owner", (String)tableItem.getData("owner"));
							hash.put("note", (String)tableItem.getData("note"));
							hash.put("period", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
							hash.put("version", (String)tableItem.getData("version"));
							selectedModels.put((String)tableItem.getData("id"), hash);
						}
					}
				}
				dialog.close();
			}
		}
		public void widgetDefaultSelected(SelectionEvent e) {}
	};


	private Shell dialog;
	private DBModel dbModel;
	private Connection db;
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
	private Label lblMode;
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