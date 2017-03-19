package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiImportComponent;
import org.archicontribs.database.model.impl.ArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.diagram.editparts.ArchimateDiagramPart;
import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IFolder;

public class DBMenuComponentImportHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		ArchimateModel model;
		
		if ( selection instanceof IFolder ) {											// if the user clicked on a folder in the tree
			model = (ArchimateModel) ((IFolder)selection).getArchimateModel();
		} else if ( selection instanceof IArchimateConcept ) {							// if the user clicked on an element or a relationship in the tree
			model = (ArchimateModel) ((IArchimateConcept)selection).getArchimateModel();
		} else if ( selection instanceof ArchimateElementEditPart ) {					// if the user clicked on a component in a view
			model = (ArchimateModel) ((ArchimateElementEditPart)selection).getModel().getDiagramModel().getArchimateModel();
		} else if ( selection instanceof ArchimateDiagramPart ) {						// if the user clicked on a view in the tree
			model = (ArchimateModel)((ArchimateDiagramPart)selection).getModel().getArchimateModel();
		} else {
			DBGui.popup(Level.ERROR, "Do not know which component you selected : "+selection.getClass().getSimpleName());
			return null;
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Importing component in model "+model.getName());
		
		new DBGuiImportComponent(model, "Import a component");
		return null;
	}
}
