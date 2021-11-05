/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.archicontribs.database.GUI.DBGuiExportModel;
import org.archicontribs.database.model.DBArchimateModel;
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
     * Exports the model into the database.<br>
     * This method is called when the user clicks on the "Export / Export to database" menu entry of Archi
     */
    @Override
    public void export(IArchimateModel archimateModel) throws IOException {
        logger.info("Exporting model "+archimateModel.getName());

        try {
        	DBGuiExportModel exportModel = new DBGuiExportModel((DBArchimateModel)archimateModel, "Export model");
        	exportModel.run();
        } catch (Exception e) {
            DBGuiUtils.popup(Level.ERROR,"Cannot export model", e);
        }
    }
}
