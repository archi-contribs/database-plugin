/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiExportModel;
import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class DBMenuModelExportHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		DBArchimateModel model = (DBArchimateModel)selection;
		
		if ( logger.isDebugEnabled() ) logger.debug("Exporting model "+model.getName());

        try {
        	DBGuiExportModel exportModel = new DBGuiExportModel(model, "Export model");
        	exportModel.run();
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot export model", e);
        }
        
		return null;
	}
}
