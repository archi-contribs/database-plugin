/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGuiUtils;
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
	 * Imports the model from the database.<br>
     * This method is called when the user clicks on the "Import / Import from database" menu entry of Archi
	 */
	@Override
	public void doImport(IArchimateModel notUsed) throws IOException {
		logger.info("Importing model.");
	
		try {
			DBGuiImportModel importModel = new DBGuiImportModel("Import model");
			importModel.run();
		} catch (Exception e) {
		    DBGuiUtils.popup(Level.ERROR,"Cannot import model", e);
		}
	}
}