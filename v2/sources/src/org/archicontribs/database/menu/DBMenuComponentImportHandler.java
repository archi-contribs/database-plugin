/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiImportComponents;
import org.archicontribs.database.model.DBArchimateModel;
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
		DBArchimateModel model;
		IArchimateDiagramModel view = null;
		IFolder folder = null;
		
		if ( selection instanceof IFolder ) {											// if the user clicked on a folder in the tree
			model = (DBArchimateModel) ((IFolder)selection).getArchimateModel();
			folder = (IFolder)selection;
			
		} else if ( selection instanceof IArchimateConcept ) {							// if the user clicked on an element or a relationship in the tree
			model = (DBArchimateModel) ((IArchimateConcept)selection).getArchimateModel();
			folder = (IFolder) ((IArchimateConcept)selection).eContainer();
			
		} else if ( selection instanceof ArchimateElementEditPart ) {					// if the user clicked on a component in a view
			model = (DBArchimateModel) ((ArchimateElementEditPart)selection).getModel().getDiagramModel().getArchimateModel();
			
		} else if ( selection instanceof IArchimateDiagramModel ) {						// if the user clicked on a view in the tree
			model = (DBArchimateModel) ((IArchimateDiagramModel)selection).getArchimateModel();
			view = ((IArchimateDiagramModel)selection);
			folder = (IFolder)view.eContainer();
			
	    } else if ( selection instanceof ArchimateDiagramPart ) {                       // if the user clicked on a view background
	        model = (DBArchimateModel) ((ArchimateDiagramPart)selection).getModel().getArchimateModel();
			view = ((ArchimateDiagramPart)selection).getModel();
			
		} else if ( selection instanceof DBArchimateModel ) {                           // if the user clicked on the model
            model = (DBArchimateModel) selection;
            
		} else {
			logger.error("We do not know what the user selected ("+selection.getClass().getSimpleName()+")");
		    return null;                                                                // we do not know what the user selected
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Importing component in model "+model.getName());
		
        try {
        	DBGuiImportComponents importComponent = new DBGuiImportComponents(model, view, folder, "Import a component");
        	importComponent.run();
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot import model", e);
        }
		return null;
	}
}
