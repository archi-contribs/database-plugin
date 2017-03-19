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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
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
	public enum EXPORT_TYPE {EXPORT_MODEL, EXPORT_COMPONENTS};
	
	private static String HELP_ID = "com.archimatetool.help.DBPreferencePage";
	
	private Composite loggerComposite;
	
	private DBDatabaseEntryTableEditor table;
	
	private RadioGroupFieldEditor loggerModeRadioGroupEditor;
	private FileFieldEditor filenameFileFieldEditor;
	private RadioGroupFieldEditor loggerLevelRadioGroupEditor;
	private DBTextFieldEditor expertTextFieldEditor;
	private Group simpleModeGroup;
	private Group expertModeGroup;
	
	private DBLogger logger = new DBLogger(DBPreferencePage.class);
	
	private TabFolder tabFolder;
	
	public DBPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		if ( logger.isDebugEnabled() ) logger.debug("Setting preference store");
		setPreferenceStore(DBPlugin.INSTANCE.getPreferenceStore());
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	protected void createFieldEditors() {
		if ( logger.isDebugEnabled() ) logger.debug("Creating field editors on preference page");
		
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getFieldEditorParent().getParent(), HELP_ID);
        
		tabFolder = new TabFolder(getFieldEditorParent(), SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		// ******************************** */
		// * Database tab  **************** */
		// ******************************** */
		Composite databaseComposite = new Composite(tabFolder, SWT.NULL);
        RowLayout rowLayout = new RowLayout();
        rowLayout.type = SWT.VERTICAL;
        rowLayout.pack = true;
        rowLayout.marginTop = 5;
        rowLayout.marginBottom = 5;
        rowLayout.justify = false;
        rowLayout.fill = false;
        databaseComposite.setLayoutData(rowLayout);
		
        TabItem behaviourTabItem = new TabItem(tabFolder, SWT.NONE);
        behaviourTabItem.setText("Behaviour");
        behaviourTabItem.setControl(databaseComposite);
        
		table = new DBDatabaseEntryTableEditor("databases", "", databaseComposite);
		addField(table);

		// ********************************* */
		// * Logging tab  ****************** */
		// ********************************* */
        loggerComposite = new Composite(tabFolder, SWT.NULL);
        rowLayout = new RowLayout();
        rowLayout.type = SWT.VERTICAL;
        rowLayout.pack = true;
        rowLayout.marginTop = 5;
        rowLayout.marginBottom = 5;
        rowLayout.justify = false;
        rowLayout.fill = false;
        loggerComposite.setLayoutData(rowLayout);
        
        Label note = new Label(loggerComposite, SWT.NONE);
        note = new Label(loggerComposite, SWT.NONE);
        note.setText(" Please be aware that enabling debug or, even more, trace level has got important impact on performances!\n Activate only if required.");
        note.setForeground(DBGui.RED_COLOR);
        
        TabItem loggerTabItem = new TabItem(tabFolder, SWT.NONE);
        loggerTabItem.setText("Logger");
        loggerTabItem.setControl(loggerComposite);
        
    	loggerModeRadioGroupEditor = new RadioGroupFieldEditor("loggerMode", "", 1, LOGGER_MODES, loggerComposite, true);
    	addField(loggerModeRadioGroupEditor);
    	
    	simpleModeGroup = new Group(loggerComposite, SWT.NONE);
    	simpleModeGroup.setLayout(new GridLayout());
    	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        //gd.widthHint = 300;
        simpleModeGroup.setLayoutData(gd);
        
        
        loggerLevelRadioGroupEditor = new RadioGroupFieldEditor("loggerLevel", "", 6, LOGGER_LEVELS, simpleModeGroup, false);
        addField(loggerLevelRadioGroupEditor);
        
        filenameFileFieldEditor = new DBFileFieldEditor("loggerFilename", "Log filename : ", false, FileFieldEditor.VALIDATE_ON_KEY_STROKE, simpleModeGroup);
        addField(filenameFileFieldEditor);
        
        
        
    	expertModeGroup = new Group(loggerComposite, SWT.NONE);
    	expertModeGroup.setLayout(new GridLayout());
    	gd = new GridData(GridData.FILL_BOTH);
    	//gd.widthHint = 350;
    	expertModeGroup.setLayoutData(gd);
        
        expertTextFieldEditor = new DBTextFieldEditor("loggerExpert", "", expertModeGroup);
        expertTextFieldEditor.getTextControl().setLayoutData(gd);
        expertTextFieldEditor.getTextControl().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        addField(expertTextFieldEditor);

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
			mode = DBPlugin.INSTANCE.getPreferenceStore().getString("loggerMode");
    		if ( mode == null ) {
    			mode = DBPlugin.INSTANCE.getPreferenceStore().getDefaultString("loggerMode");
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
    	if ( logger.isTraceEnabled() ) logger.trace("Saving preferences in preference store");
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
            ((IPersistentPreferenceStore)DBPlugin.INSTANCE.getPreferenceStore()).save();
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

    /**
     * @return the default export type preference. Values can be EXPORT_COMPONENTS or EXPORT_MODEL.
     */
	public EXPORT_TYPE getDefaultExportType() {
		if ( DBPlugin.areEqual(DBPlugin.INSTANCE.getPreferenceStore().getString("defaultExportType"), "components") )
			return  EXPORT_TYPE.EXPORT_COMPONENTS;
		else
			return EXPORT_TYPE.EXPORT_MODEL;
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}