/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.archicontribs.database.GUI.DBGuiImportModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Class that is called when the user selects the "import model from database" in the context menu 
 * 
 * @author Herve Jouin
 */
public class DBMenuModelImportHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if ( logger.isDebugEnabled() ) logger.debug("Launching Import model window");

        try {
        	DBGuiImportModel importModel = new DBGuiImportModel("Import model");
        	importModel.run();
        } catch (Exception e) {
            DBGuiUtils.popup(Level.ERROR,"Cannot import model", e);
        }
        
		return null;
	}
}
