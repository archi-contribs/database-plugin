/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiImportModel;
import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.editor.model.ISelectedModelImporter;
import com.archimatetool.model.IArchimateModel;

/**
 * Import from Database
 * 
 * @author Herve Jouin
 */
public class DBImporter implements IModelImporter, ISelectedModelImporter {
	private static final DBLogger logger = new DBLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void doImport() throws IOException {
		doImport(null);
	}
	/**
	 * Exports the model into the database.
	 */
	@Override
	public void doImport(IArchimateModel notUsed) throws IOException {
		if ( logger.isDebugEnabled() ) logger.debug("Importing model.");
	
		DBGuiImportModel importDialog = null;
		try {
		    importDialog = new DBGuiImportModel("Import model");
		    importDialog.run();
		} catch (Exception e) {
		    DBGui.popup(Level.ERROR,"Cannot import model", e);
		}
		importDialog = null;
	}
}