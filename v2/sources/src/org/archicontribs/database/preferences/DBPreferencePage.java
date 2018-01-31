/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.preferences;

import java.io.IOException;
import java.lang.reflect.Field;
import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbench;

/**
 * This class sets the preference page that will show up in Archi preference menu.
 * 
 * @author Herve Jouin
 */
public class DBPreferencePage extends FieldEditorPreferencePage	implements IWorkbenchPreferencePage {
	private static String[][] LOGGER_MODES = {{"Disabled", "disabled"}, {"Simple mode", "simple"}, {"Expert mode", "expert"}};
	private static String[][] LOGGER_LEVELS = {{"Fatal", "fatal"}, {"Error", "error"}, {"Warn", "warn"}, {"Info", "info"}, {"Debug", "debug"}, {"Trace", "trace"}};
	
	private static String HELP_ID = "org.archicontribs.database.preferences.configurePlugin";
	
	private static final IPreferenceStore preferenceStore = DBPlugin.INSTANCE.getPreferenceStore();
	
	private Composite loggerComposite;
	
	private DBDatabaseEntryTableEditor table = null;
	
	private RadioGroupFieldEditor loggerModeRadioGroupEditor;
	private FileFieldEditor filenameFileFieldEditor;
	private RadioGroupFieldEditor loggerLevelRadioGroupEditor;
	private DBTextFieldEditor expertTextFieldEditor;
	private Group simpleModeGroup;
	private Group expertModeGroup;
	private Button btnCheckForUpdateAtStartupButton;
	private Button btnExportWithDefaultValues;
	private Button btnCloseIfSuccessful;
	private Button btnRemoveDirtyFlag;
	private Button btnDeleteIfImportError;
	private Button btnImportShared;
	private Button btnShowIdInContextMenu;
	
	private DBLogger logger = new DBLogger(DBPreferencePage.class);
	
	private TabFolder tabFolder;
	
	private boolean mouseOverHelpButton = false;
	
	public DBPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		if ( logger.isDebugEnabled() ) logger.debug("Setting preference store");
		setPreferenceStore(preferenceStore);
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	protected void createFieldEditors() {
		if ( logger.isDebugEnabled() ) logger.debug("Creating field editors on preference page");
        
		tabFolder = new TabFolder(getFieldEditorParent(), SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tabFolder.setBackground(DBGui.GROUP_BACKGROUND_COLOR);
		
		// ******************************** */
		// * Behaviour tab **************** */
		// ******************************** */
		Composite behaviourComposite = new Composite(tabFolder, SWT.NULL);
        RowLayout rowLayout = new RowLayout();
        rowLayout.type = SWT.VERTICAL;
        rowLayout.pack = true;
        rowLayout.marginTop = 5;
        rowLayout.marginBottom = 5;
        rowLayout.justify = false;
        rowLayout.fill = false;
        behaviourComposite.setLayoutData(rowLayout);
        behaviourComposite.setBackground(DBGui.GROUP_BACKGROUND_COLOR);
        
		TabItem behaviourTabItem = new TabItem(tabFolder, SWT.NONE);
        behaviourTabItem.setText("  Behaviour  ");
        behaviourTabItem.setControl(behaviourComposite);
        
		Group grpVersion = new Group(behaviourComposite, SWT.NONE);
		grpVersion.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		grpVersion.setLayout(new FormLayout());
		grpVersion.setText("Version : ");
		
		Label versionLbl = new Label(grpVersion, SWT.NONE);
		versionLbl.setText("Actual version :");
		versionLbl.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 10);
		versionLbl.setLayoutData(fd);
		
		Label versionValue = new Label(grpVersion, SWT.NONE);
		versionValue.setText(DBPlugin.pluginVersion);
		versionValue.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		versionValue.setFont(DBGui.BOLD_FONT);
		fd = new FormData();
		fd.top = new FormAttachment(versionLbl, 0, SWT.TOP);
		fd.left = new FormAttachment(versionLbl, 5);
		versionValue.setLayoutData(fd);
		
		Button checkUpdateButton = new Button(grpVersion, SWT.NONE);
		checkUpdateButton.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		checkUpdateButton.setText("Check for update");
		fd = new FormData();
		fd.top = new FormAttachment(versionValue, 0, SWT.CENTER);
		fd.left = new FormAttachment(versionValue, 100);
		checkUpdateButton.setLayoutData(fd);
		checkUpdateButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { DBPlugin.checkForUpdate(true); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		
		btnCheckForUpdateAtStartupButton = new Button(grpVersion, SWT.CHECK);
		btnCheckForUpdateAtStartupButton.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnCheckForUpdateAtStartupButton.setText("Automatically check for update at startup");
		fd = new FormData();
		fd.top = new FormAttachment(versionLbl, 5);
		fd.left = new FormAttachment(0, 10);
		btnCheckForUpdateAtStartupButton.setLayoutData(fd);
		btnCheckForUpdateAtStartupButton.setSelection(preferenceStore.getBoolean("checkForUpdateAtStartup"));
		
		GridData gd = new GridData();
		//gd.heightHint = 45;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		grpVersion.setLayoutData(gd);
        
		table = new DBDatabaseEntryTableEditor("databases", "", behaviourComposite);
		addField(table);
		
		Group grpMiscellaneous = new Group(behaviourComposite, SWT.NONE);
		grpMiscellaneous.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		grpMiscellaneous.setText("Miscellaneous :");
		grpMiscellaneous.setLayout(new FormLayout());
		
		gd = new GridData();
		//gd.heightHint = 80;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		grpMiscellaneous.setLayoutData(gd);


		btnExportWithDefaultValues = new Button(grpMiscellaneous, SWT.CHECK);
		btnExportWithDefaultValues.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnExportWithDefaultValues.setText("Automatically start to export to default database");
		btnExportWithDefaultValues.setSelection(preferenceStore.getBoolean("exportWithDefaultValues"));
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 10);
		btnExportWithDefaultValues.setLayoutData(fd);		
		
		btnCloseIfSuccessful = new Button(grpMiscellaneous, SWT.CHECK);
		btnCloseIfSuccessful.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnCloseIfSuccessful.setText("Automatically close import and export windows on success");
		btnCloseIfSuccessful.setSelection(preferenceStore.getBoolean("closeIfSuccessful"));
		fd = new FormData();
		fd.top = new FormAttachment(btnExportWithDefaultValues, 5);
		fd.left = new FormAttachment(0, 10);
		btnCloseIfSuccessful.setLayoutData(fd);
		
		btnRemoveDirtyFlag = new Button(grpMiscellaneous, SWT.CHECK);
		btnRemoveDirtyFlag.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnRemoveDirtyFlag.setText("Remove model's dirty flag after successful export");
		btnRemoveDirtyFlag.setSelection(preferenceStore.getBoolean("removeDirtyFlag"));
		fd = new FormData();
		fd.top = new FormAttachment(btnCloseIfSuccessful, 5);
		fd.left = new FormAttachment(0, 10);
		btnRemoveDirtyFlag.setLayoutData(fd);
		
		btnDeleteIfImportError = new Button(grpMiscellaneous, SWT.CHECK);
		btnDeleteIfImportError.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnDeleteIfImportError.setText("Delete model in case of import error");
		btnDeleteIfImportError.setSelection(preferenceStore.getBoolean("deleteIfImportError"));
		fd = new FormData();
		fd.top = new FormAttachment(btnRemoveDirtyFlag, 5);
		fd.left = new FormAttachment(0, 10);
		btnDeleteIfImportError.setLayoutData(fd);
		
		btnShowIdInContextMenu = new Button(grpMiscellaneous, SWT.CHECK);
		btnShowIdInContextMenu.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		btnShowIdInContextMenu.setText("Show debuging information in context menu");
		btnShowIdInContextMenu.setSelection(preferenceStore.getBoolean("showIdInContextMenu"));
		fd = new FormData();
		fd.top = new FormAttachment(btnDeleteIfImportError, 5);
		fd.left = new FormAttachment(0, 10);
		btnShowIdInContextMenu.setLayoutData(fd);
		
		Label lblDefaultImportType = new Label(grpMiscellaneous, SWT.NONE);
		lblDefaultImportType.setText("Default component import type :");
		lblDefaultImportType.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(btnShowIdInContextMenu, 5);
		fd.left = new FormAttachment(0, 10);
		lblDefaultImportType.setLayoutData(fd);
		
		btnImportShared = new Button(grpMiscellaneous, SWT.RADIO);
		btnImportShared.setText("Shared");
		btnImportShared.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblDefaultImportType, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblDefaultImportType, 10);
		btnImportShared.setLayoutData(fd);
		
		Button btnImportCopy = new Button(grpMiscellaneous, SWT.RADIO);
		btnImportCopy.setText("Copy");
		btnImportCopy.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(lblDefaultImportType, 0, SWT.CENTER);
		fd.left = new FormAttachment(btnImportShared, 10);
		btnImportCopy.setLayoutData(fd);
		
		btnImportShared.setSelection(preferenceStore.getBoolean("importShared"));
		btnImportCopy.setSelection(!preferenceStore.getBoolean("importShared"));
		
		Group grpHelp = new Group(behaviourComposite, SWT.NONE);
        grpHelp.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
        grpHelp.setLayout(new FormLayout());
        grpHelp.setText("Online help : ");
        
        gd = new GridData();
        //gd.heightHint = 40;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        grpHelp.setLayoutData(gd);
        
        Label btnHelp = new Label(grpHelp, SWT.NONE);
        btnHelp.addListener(SWT.MouseEnter, new Listener() { @Override public void handleEvent(Event event) { mouseOverHelpButton = true; btnHelp.redraw(); } });
        btnHelp.addListener(SWT.MouseExit, new Listener() { @Override public void handleEvent(Event event) { mouseOverHelpButton = false; btnHelp.redraw(); } });
        btnHelp.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e)
            {
                 if ( mouseOverHelpButton ) e.gc.drawRoundRectangle(0, 0, 29, 29, 10, 10);
                 e.gc.drawImage(DBGui.HELP_ICON, 2, 2);
            }
        });
        btnHelp.addListener(SWT.MouseUp, new Listener() { @Override public void handleEvent(Event event) { if ( logger.isDebugEnabled() ) logger.debug("Showing help : /"+DBPlugin.PLUGIN_ID+"/help/html/configurePlugin.html"); PlatformUI.getWorkbench().getHelpSystem().displayHelpResource("/"+DBPlugin.PLUGIN_ID+"/help/html/configurePlugin.html"); } });
        fd = new FormData(30,30);
        fd.top = new FormAttachment(0, 11);
        fd.left = new FormAttachment(0, 10);
        btnHelp.setLayoutData(fd);
        
        Label helpLbl1 = new Label(grpHelp, SWT.NONE);
        helpLbl1.setText("Please be informed that a help button like this one is available on every plugin window.");
        helpLbl1.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(0, 10);
        fd.left = new FormAttachment(btnHelp, 10);
        helpLbl1.setLayoutData(fd);
        
        Label helpLbl2 = new Label(grpHelp, SWT.NONE);
        helpLbl2.setText("The online help is also available at any time using the menu Help / Help content.");
        helpLbl2.setBackground(DBGui.COMPO_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(helpLbl1, 5);
        fd.left = new FormAttachment(btnHelp, 10);
        helpLbl2.setLayoutData(fd);
		
		// ********************************* */
		// * Logger tab  ******************* */
		// ********************************* */
        loggerComposite = new Composite(tabFolder, SWT.NONE);
        rowLayout = new RowLayout();
        rowLayout.type = SWT.VERTICAL;
        rowLayout.pack = true;
        rowLayout.marginTop = 5;
        rowLayout.marginBottom = 5;
        rowLayout.justify = false;
        rowLayout.fill = false;
        loggerComposite.setLayoutData(rowLayout);
        loggerComposite.setBackground(DBGui.GROUP_BACKGROUND_COLOR);
        
        TabItem loggerTabItem = new TabItem(tabFolder, SWT.NONE);
        loggerTabItem.setText("  Logger  ");
        loggerTabItem.setControl(loggerComposite);
        
        Label note = new Label(loggerComposite, SWT.NONE);
        note.setText(" Please be aware that enabling debug or, even more, trace level has got important impact on performances!\n Activate only if required.");
        note.setBackground(DBGui.GROUP_BACKGROUND_COLOR);
        note.setForeground(DBGui.RED_COLOR);
        
    	loggerModeRadioGroupEditor = new RadioGroupFieldEditor("loggerMode", "", 1, LOGGER_MODES, loggerComposite, true);
      	addField(loggerModeRadioGroupEditor);
    	
    	simpleModeGroup = new Group(loggerComposite, SWT.NONE);
    	simpleModeGroup.setLayout(new GridLayout());
    	gd = new GridData(GridData.FILL_HORIZONTAL);
        //gd.widthHint = 300;
        simpleModeGroup.setLayoutData(gd);
        simpleModeGroup.setBackground(DBGui.GROUP_BACKGROUND_COLOR);
        
        
        loggerLevelRadioGroupEditor = new RadioGroupFieldEditor("loggerLevel", "", 6, LOGGER_LEVELS, simpleModeGroup, false);
        addField(loggerLevelRadioGroupEditor);
        
        filenameFileFieldEditor = new DBFileFieldEditor("loggerFilename", "Log filename : ", false, FileFieldEditor.VALIDATE_ON_KEY_STROKE, simpleModeGroup);
        addField(filenameFileFieldEditor);
        
        
        
    	expertModeGroup = new Group(loggerComposite, SWT.NONE);
    	expertModeGroup.setLayout(new GridLayout());
    	gd = new GridData(GridData.FILL_BOTH);
    	//gd.widthHint = 350;
    	expertModeGroup.setLayoutData(gd);
    	expertModeGroup.setBackground(DBGui.GROUP_BACKGROUND_COLOR);
        
        expertTextFieldEditor = new DBTextFieldEditor("loggerExpert", "", expertModeGroup);
        expertTextFieldEditor.getTextControl().setLayoutData(gd);
        expertTextFieldEditor.getTextControl().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        addField(expertTextFieldEditor);
        
        // We activate the Eclipse Help framework
       PlatformUI.getWorkbench().getHelpSystem().setHelp(getFieldEditorParent().getParent(), HELP_ID);
       PlatformUI.getWorkbench().getHelpSystem().setHelp(behaviourComposite, HELP_ID);
       PlatformUI.getWorkbench().getHelpSystem().setHelp(loggerComposite, HELP_ID);
       

        showLogger();
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		
		if ( event.getSource() == loggerModeRadioGroupEditor )
			showLogger();
		
		 if( event.getSource() == filenameFileFieldEditor )
			 setValid(true);
	}
	
	private void showLogger() {
		String mode = null;
		
		// If the user changes the value, we get it
		for ( Control control: loggerModeRadioGroupEditor.getRadioBoxControl(loggerComposite).getChildren() ) {
			if (((Button)control).getSelection())
				mode = (String)((Button)control).getData();
		}
		
		// when the preference page initialize, the radioButton selection is not (yet) made.
		// so we get the value from the preferenceStore
		if ( mode == null ) {
			mode = preferenceStore.getString("loggerMode");
    		if ( mode == null ) {
    			mode = preferenceStore.getDefaultString("loggerMode");
    		}
		}
		
		// Defining of the user's choice, we show up the simple or expert parameters or none of them
		switch ( mode ) {
		case "disabled" :
			expertModeGroup.setVisible(false);
			simpleModeGroup.setVisible(false);
			break;
		case "simple" :
			expertModeGroup.setVisible(false);
			simpleModeGroup.setVisible(true);
			break;
		case "expert" :
			expertModeGroup.setVisible(true);
			simpleModeGroup.setVisible(false);
			break;
		default : 
			expertModeGroup.setVisible(false);
			simpleModeGroup.setVisible(false);
			logger.error("Unknown value \""+mode+"\" in loggerModeRadioGroupEditor.");
		}
	}
	
    @Override
    public boolean performOk() {
    	if ( table != null )
    	    table.close();
    	
    	if ( logger.isTraceEnabled() ) logger.trace("Saving preferences in preference store");
    	
    	preferenceStore.setValue("exportWithDefaultValues", btnExportWithDefaultValues.getSelection());
    	preferenceStore.setValue("closeIfSuccessful", btnCloseIfSuccessful.getSelection());
    	preferenceStore.setValue("checkForUpdateAtStartup", btnCheckForUpdateAtStartupButton.getSelection());
    	preferenceStore.setValue("removeDirtyFlag", btnRemoveDirtyFlag.getSelection());
    	preferenceStore.setValue("deleteIfImportError", btnDeleteIfImportError.getSelection());
    	preferenceStore.setValue("showIdInContextMenu", btnShowIdInContextMenu.getSelection());
    	preferenceStore.setValue("importShared", btnImportShared.getSelection());
    	
    	if ( table != null )
    	    table.store();
    	
    	// the loggerMode is a private property, so we use reflection to access it
		try {
			Field field = RadioGroupFieldEditor.class.getDeclaredField("value");
			field.setAccessible(true);
			if ( logger.isTraceEnabled() ) logger.trace("loggerMode = "+(String)field.get(loggerModeRadioGroupEditor));
			field.setAccessible(false);
		} catch (Exception err) {
			logger.error("Failed to retrieve the \"loggerMode\" value from the preference page", err);
		}
		loggerModeRadioGroupEditor.store();
    	
    	// the loggerLevel is a private property, so we use reflection to access it
		try {
			Field field = RadioGroupFieldEditor.class.getDeclaredField("value");
			field.setAccessible(true);
			if ( logger.isTraceEnabled() ) logger.trace("loggerLevel = "+(String)field.get(loggerLevelRadioGroupEditor));
			field.setAccessible(false);
		} catch (Exception err) {
			logger.error("Failed to retrieve the \"loggerLevel\" value from the preference page", err);
		}
		loggerLevelRadioGroupEditor.store();
		
		//TODO : if we are in simple mode, check that is is a valid writable filename
		if ( logger.isTraceEnabled() ) logger.trace("loggerFilename = "+filenameFileFieldEditor.getStringValue());
		filenameFileFieldEditor.store();
		
		if ( logger.isTraceEnabled() ) logger.trace("loggerExpert = "+expertTextFieldEditor.getStringValue());
		expertTextFieldEditor.store();
		
        try {
        	if ( logger.isDebugEnabled() ) logger.debug("Saving the preference store to disk.");
            ((IPersistentPreferenceStore)preferenceStore).save();
        } catch (IOException err) {
        	DBGui.popup(Level.ERROR, "Failed to save the preference store to disk.", err);
        }
		
		try {
			logger.configure();
		} catch (Exception e) {
			DBGui.popup(Level.ERROR, "Faied to configure logger", e);
		}
		
    	return true;
    }

	@Override
	public void init(IWorkbench workbench) {
	}
}