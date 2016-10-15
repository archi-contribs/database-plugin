package org.archicontribs.database;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class DBPreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		DBPlugin.debug(DebugLevel.MainMethod, "DBPreferenceInitializer.initializeDefaultPreferences()");
		IPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		store.setDefault("debugMainMethods", false);
		store.setDefault("debugSecondaryMethods", false);
		store.setDefault("debugVariables", false);
		store.setDefault("debugSQL", false);
		store.setDefault("importMode", "standalone");
		store.setDefault("progressWindow", "showAndWait");
	}
}
