/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;

import org.archicontribs.database.GUI.DBGuiExportModel;
import org.archicontribs.database.model.ArchimateModel;
import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IArchimateModel;

/**
 * Database Model Exporter. This class exports the model components into a central repository (database).
 * 
 * @author Herve Jouin
 */
public class DBExporter implements IModelExporter {
	private static final DBLogger logger = new DBLogger(DBExporter.class);
	
	/**
	 * Exports the model into the database.
	 */
	@Override
	public void export(IArchimateModel archimateModel) throws IOException {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting model "+archimateModel.getName());

		DBGuiExportModel exportDialog = new DBGuiExportModel((ArchimateModel)archimateModel, "Export model");
		exportDialog.run();
		exportDialog = null;
	}
}
