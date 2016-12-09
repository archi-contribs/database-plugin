package org.archicontribs.database;

import java.lang.reflect.Field;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

/**
 * This class sets the perference page that will show up in Archi preference menu.
 * 
 * @author Herve Jouin
 */
public class DBPreferencePage extends FieldEditorPreferencePage	implements IWorkbenchPreferencePage {
	
	//TODO : implement help
	//private static String HELP_ID = "com.archicontribs.database.DBPreferencePage";
	
	private DBDatabaseEntryTableEditor table;
	private RadioGroupFieldEditor importMode;
	private RadioGroupFieldEditor showProgress;

	private BooleanFieldEditor debugMainMethods;
	private BooleanFieldEditor debugSecondaryMethods;
	private BooleanFieldEditor debugVariables;
	private BooleanFieldEditor debugSQL;
	
	private TabFolder tabFolder;
	
	public DBPreferencePage() {
		// DO NOT DEBUG as this constructor is used by the DBPlugin.debug method
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(DBPlugin.INSTANCE.getPreferenceStore());
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBPreferencePage.createFieldEditors()");
		
		// TODO: write the help (accessible with F1 key)
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
        
		tabFolder = new TabFolder(getFieldEditorParent(), SWT.NONE);
		
		// ********************************* */
		// * Behaviour tab  **************** */
		// ********************************* */
		
		Composite clientBehaviour = new Composite(tabFolder, SWT.NULL);
		clientBehaviour.setLayout(new GridLayout());
        
        TabItem tabItemBehaviour = new TabItem(tabFolder, SWT.NONE);
        tabItemBehaviour.setText("Behaviour");
        tabItemBehaviour.setControl(clientBehaviour);
		
		Group groupTable = new Group(clientBehaviour, SWT.NONE);
		groupTable.setFont(getFieldEditorParent().getFont());
		groupTable.setText("Databases : ");
		groupTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		table = new DBDatabaseEntryTableEditor("databases", "", groupTable);
		addField(table);
        
        importMode = new RadioGroupFieldEditor("importMode", "Default import mode : ", 1,
        		new String[][] {{"Standalone", "standalone"}, {"Shared", "shared"}},
        		clientBehaviour, true);
        importMode.setIndent(40);
        addField(importMode);
        
        showProgress = new RadioGroupFieldEditor("showProgress", "Progres window : ", 1,
        		new String[][] {{"Show up and wait", "showAndWait"}, {"Show up and automatically dismiss", "showAndDismiss"}, {"Do not show up", "doNotShow"}},
        		clientBehaviour, true);
        showProgress.setIndent(40);
    	addField(showProgress);
    	
    	//we add an empty line
    	new Label(clientBehaviour, SWT.NONE);

		// ********************************* */
		// * Debug tab  ******************** */
		// ********************************* */
        
        Composite clientDebug = new Composite(tabFolder, SWT.NULL);
        clientDebug.setLayout(new GridLayout());
        
        TabItem tabItemDebug = new TabItem(tabFolder, SWT.NONE);
        tabItemDebug.setText("Debug");
        tabItemDebug.setControl(clientDebug);  
            
		debugMainMethods = new BooleanFieldEditor("debugMainMethods", "debug main methods", clientDebug);
    	addField(debugMainMethods);
    	
    	debugSecondaryMethods = new BooleanFieldEditor("debugSecondaryMethods", "debug secondary methods", clientDebug);
    	addField(debugSecondaryMethods);
    	
    	debugVariables = new BooleanFieldEditor("debugVariables", "debug variables", clientDebug);
    	addField(debugVariables);
    	
    	debugSQL = new BooleanFieldEditor("debugSQL", "debug database requests", clientDebug);
    	addField(debugSQL);
    	
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBPreferencePage.createFieldEditors()");
	}
	
    @Override
    public boolean performOk() {
    	DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBPreferencePage.performOk()");
    	table.store();
    	
    	// the value is a private property, so we use reflection to access it
		try {
			Field field = RadioGroupFieldEditor.class.getDeclaredField("value");
			field.setAccessible(true);
			DBPlugin.debug(DebugLevel.Variable, "Saving importMode : "+(String)field.get(importMode));
			field.setAccessible(false);
		} catch (Exception err) {
	    	DBPlugin.debug(DebugLevel.Variable, "Saving importMode");
	    }
    	importMode.store();
    	
		try {
			Field field = RadioGroupFieldEditor.class.getDeclaredField("value");
			field.setAccessible(true);
			DBPlugin.debug(DebugLevel.Variable, "Saving showProgress : "+(String)field.get(showProgress));
			field.setAccessible(false);
		} catch (Exception err) {
	    	DBPlugin.debug(DebugLevel.Variable, "Saving showProgress");
		}
    	showProgress.store();
    	    	
    	DBPlugin.debug(DebugLevel.Variable, "Saving debugMainMethods : "+debugMainMethods.getBooleanValue());
    	debugMainMethods.store();
    	
    	DBPlugin.debug(DebugLevel.Variable, "Saving debugSecondaryMethods : "+debugSecondaryMethods.getBooleanValue());
    	debugSecondaryMethods.store();
    	
    	DBPlugin.debug(DebugLevel.Variable, "Saving debugVariables : "+debugVariables.getBooleanValue());
    	debugVariables.store();
    	
    	DBPlugin.debug(DebugLevel.Variable, "Saving debugSQL : "+debugSQL.getBooleanValue());
    	debugSQL.store();
    	
    	DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBPreferencePage.performOk()");
    	return true;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	// do not debug the following methods as they are called by the debug method
	public boolean shouldDebugMainMethods() {
		return getPreferenceStore().getBoolean("debugMainMethods");
	}
	
	public boolean shouldDebugSecondaryMethods() {
		return getPreferenceStore().getBoolean("debugSecondaryMethods");
	}
	
	public boolean shouldDebugVariables() {
		return getPreferenceStore().getBoolean("debugVariables");
	}
	
	public boolean shouldDebugSQL() {
		return getPreferenceStore().getBoolean("debugSQL");
	}
	
	public String getShowProgress() {
		return getPreferenceStore().getString("showProgress");
	}
	
	public String getImportMode() {
		return  getPreferenceStore().getString("importMode");
	}
	
	public boolean isImportModeStandalone() {
		return  getPreferenceStore().getString("importMode").equals("standalone");
	}
}