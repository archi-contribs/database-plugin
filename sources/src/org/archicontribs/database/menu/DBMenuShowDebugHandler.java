/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.gui.DBGuiShowDebug;
import org.archicontribs.database.gui.DBGuiUtils;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.ISketchModel;

/**
 * Class that is called when the iser selects the "show debugging information" in the context menu
 * 
 * @author Herve Jouin
 */
public class DBMenuShowDebugHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenuShowDebugHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
        EObject component;
        
        if ( selection instanceof IArchimateConcept
                || selection instanceof IArchimateDiagramModel
                || selection instanceof ICanvasModel
                || selection instanceof ISketchModel
                || selection instanceof IFolder) {
            // if the user click on a component in the model tree
            component = (IArchimateModelObject)selection;
        } else if ( selection instanceof EditPart ) {
            // if the user clicked on a graphical object in a view
            component = (EObject) ((EditPart)selection).getModel();
        } else {
            DBGuiUtils.popup(Level.ERROR, "Do not know which component you selected.");
            return null;
        }
        
        DBMetadata dbMetadata = DBMetadata.getDBMetadata(component);
        if ( dbMetadata != null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Showing debbuging information for "+dbMetadata.getDebugName());
            }
            
            try {
                DBGuiShowDebug showDebug = new DBGuiShowDebug(component, "Debugging information");
                showDebug.run();
            } catch (Exception e) {
                DBGuiUtils.popup(Level.ERROR,"Failed to show debugging information.", e);
            }
        }
        
        return dbMetadata;
    }
}
