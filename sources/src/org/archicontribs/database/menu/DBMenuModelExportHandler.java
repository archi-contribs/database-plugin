/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.gui.DBGuiExportModel;
import org.archicontribs.database.gui.DBGuiUtils;
import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IDiagramModelArchimateComponent;

/**
 * Class that is called when the user selects the "export model to database" in the context menu
 * 
 * @author Herve Jouin
 */
public class DBMenuModelExportHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		DBArchimateModel exportedModel = null;
		
		// we check if a model is selected
		if ( selection instanceof DBArchimateModel )
			exportedModel = (DBArchimateModel)selection;
		else if ( selection instanceof AbstractEditPart ) {
            Object selectedObject = ((AbstractEditPart)selection).getModel();
            if ( selectedObject instanceof IDiagramModelArchimateComponent )
            	exportedModel = (DBArchimateModel) ((IDiagramModelArchimateComponent)selectedObject).getArchimateConcept().getArchimateModel();
		} else if ( selection instanceof IArchimateModelObject )
        	exportedModel = (DBArchimateModel) ((IArchimateModelObject) selection).getArchimateModel();
		
		if ( exportedModel != null ) {
			if ( logger.isDebugEnabled() )
				logger.debug("Exporting model "+exportedModel.getName());
	
	        try {
	        	DBGuiExportModel exportModel = new DBGuiExportModel(exportedModel, "Export model");
	        	exportModel.run();
	        } catch (Exception e) {
	            DBGuiUtils.popup(Level.ERROR,"Cannot export model", e);
	        }
		}
        
		return null;
	}
}
