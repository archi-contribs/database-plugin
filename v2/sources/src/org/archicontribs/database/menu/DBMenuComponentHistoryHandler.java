/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGuiComponentHistory;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.ISketchModel;

public class DBMenuComponentHistoryHandler extends AbstractHandler {
    private static final DBLogger logger = new DBLogger(DBMenu.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
        IArchimateModelObject component;

        if ( selection instanceof IArchimateConcept ) {										// if the component is selected in the tree
            component = (IArchimateConcept)selection;
        } else if ( selection instanceof ArchimateElementEditPart ) {						// if the component is selected in a view
            component = (IArchimateConcept) ((ArchimateElementEditPart)selection).getModel().getArchimateConcept();
        } else if ( selection instanceof IArchimateDiagramModel ) {							// if the user clicked on a view in the tree
        	component = (IArchimateDiagramModel)selection;
        } else if ( selection instanceof ICanvasModel ) {									// if the user clicked on a view in the tree
        	component = (ICanvasModel)selection;
        } else if ( selection instanceof ISketchModel ) {									// if the user clicked on a view in the tree
        	component = (ISketchModel)selection;
        } else {
            DBGui.popup(Level.ERROR, "Do not know which component you selected :(");
            return null;
        }

        if ( logger.isDebugEnabled() ) logger.debug("Showing history for component "+((IDBMetadata)component).getDBMetadata().getDebugName());

        try {
            new DBGuiComponentHistory(component);
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot import model", e);
        }
        return null;
    }
}
