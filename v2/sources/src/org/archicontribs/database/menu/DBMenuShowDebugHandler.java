/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiShowDebug;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.EditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.ISketchModel;

public class DBMenuShowDebugHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenuShowDebugHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
        IArchimateModelObject component;
        
        if ( selection instanceof IArchimateConcept
                || selection instanceof IArchimateDiagramModel
                || selection instanceof ICanvasModel
                || selection instanceof ISketchModel
                || selection instanceof IFolder) {
            // if the user click on a component in the model tree
            component = (IArchimateModelObject)selection;
        } else if ( selection instanceof EditPart ) {
            // if the user clicked on a graphical object in a view
            component = (IArchimateModelObject) ((EditPart)selection).getModel();
        } else {
            DBGui.popup(Level.ERROR, "Do not know which component you selected.");
            return null;
        }
        
        if ( logger.isDebugEnabled() ) {
            if ( component instanceof DBArchimateModel )
                logger.debug("Showing debbuging information for model "+((DBArchimateModel)component).getName());
            else
                logger.debug("Showing debbuging information for "+((IDBMetadata)component).getDBMetadata().getDebugName());
        }

        try {
            DBGuiShowDebug showDebug = new DBGuiShowDebug(component, "Debugging information");
            showDebug.run();
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Failed to show debugging information.", e);
        }
        
        return null;
    }
}
