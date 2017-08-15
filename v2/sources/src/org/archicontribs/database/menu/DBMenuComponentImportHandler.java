/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiImportComponent;
import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.diagram.editparts.ArchimateDiagramPart;
import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IFolder;

public class DBMenuComponentImportHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		ArchimateModel model;
		IArchimateDiagramModel view = null;
		IFolder folder = null;
		
		if ( selection instanceof IFolder ) {											// if the user clicked on a folder in the tree
			model = (ArchimateModel) ((IFolder)selection).getArchimateModel();
			folder = (IFolder)selection;
		} else if ( selection instanceof IArchimateConcept ) {							// if the user clicked on an element or a relationship in the tree
			model = (ArchimateModel) ((IArchimateConcept)selection).getArchimateModel();
		} else if ( selection instanceof ArchimateElementEditPart ) {					// if the user clicked on a component in a view
			model = (ArchimateModel) ((ArchimateElementEditPart)selection).getModel().getDiagramModel().getArchimateModel();
		} else if ( selection instanceof IArchimateDiagramModel ) {						// if the user clicked on a view in the tree
			model = (ArchimateModel)((IArchimateDiagramModel)selection).getArchimateModel();
			view = ((IArchimateDiagramModel)selection);
	    } else if ( selection instanceof ArchimateDiagramPart ) {                     // if the user clicked on a view background
	        model = (ArchimateModel)((ArchimateDiagramPart)selection).getModel().getArchimateModel();
			view = ((ArchimateDiagramPart)selection).getModel();
		} else {
			DBGui.popup(Level.ERROR, "Do not know which component you selected : "+selection.getClass().getSimpleName());
			return null;
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Importing component in model "+model.getName());
		
        try {
            new DBGuiImportComponent(model, view, folder, "Import a component");
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot import model", e);
        }
		return null;
	}
}
