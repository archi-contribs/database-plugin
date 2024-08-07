/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.gui.DBGuiComponentHistory;
import org.archicontribs.database.gui.DBGuiUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IDiagramModelArchimateComponent;

/**
 * Class that is called when the user selects the "Component history" contexte menu.
 * 
 * @author Herve Jouin
 */
public class DBMenuComponentHistoryHandler extends AbstractHandler {
    private static final DBLogger logger = new DBLogger(DBMenu.class);

	@Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
        Object selectedObject;
        
        if ( selection instanceof AbstractEditPart ) {
            selectedObject = ((AbstractEditPart)selection).getModel();
            
            if ( DBPlugin.areEqual(event.getParameter("mustConsiderConcept"), "yes") && (selectedObject instanceof IDiagramModelArchimateComponent) )
               selectedObject = ((IDiagramModelArchimateComponent)selectedObject).getArchimateConcept();
        } else
            selectedObject = selection;
        
        if ( selectedObject instanceof IArchimateModelObject ) {
            IArchimateModelObject selectedComponent = (IArchimateModelObject) selectedObject;
            if ( logger.isDebugEnabled() ) logger.debug("Showing database history of component " + DBMetadata.getDBMetadata(selectedComponent).getDebugName());
    
            try {
                DBGuiComponentHistory componentHistory = new DBGuiComponentHistory(selectedComponent);
                componentHistory.run();
            } catch (Exception e) {
                DBGuiUtils.popup(Level.ERROR,"Cannot get history from database.", e);
            }
        }
        else {
            // in all other cases, we do not know how to get its history from the database
            DBGuiUtils.popup(Level.ERROR, "Cannot get history of a "+selectedObject.getClass().getSimpleName());
            return null;
        }


        return null;
    }
}
