/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiReplaceElement;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.model.IArchimateElement;

public class DBMenuElementReplaceHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		IArchimateElement element;
		
		if ( selection instanceof IArchimateElement ) {											// if the user clicked on an element
		    element = (IArchimateElement)selection;
			
		} else if ( selection instanceof ArchimateElementEditPart ) {					        // if the user clicked on an element in a view
		    element = ((ArchimateElementEditPart)selection).getModel().getArchimateElement();

		} else {
			logger.error("We do not know how to replace a ("+selection.getClass().getSimpleName()+")");
		    return null;                                                                        // we can only replace elements here
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Replacing element "+((IDBMetadata)element).getDBMetadata().getDebugName());
		
        try {
            DBGuiReplaceElement replaceElement = new DBGuiReplaceElement((DBArchimateModel)element.getArchimateModel(), element, "Replace element");
            replaceElement.run();
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot import model", e);
        }
		return null;
	}
}
