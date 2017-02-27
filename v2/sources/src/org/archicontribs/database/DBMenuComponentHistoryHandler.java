package org.archicontribs.database;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGuiComponentHistory;
import org.archicontribs.database.GUI.DBGui;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.model.IArchimateConcept;

public class DBMenuComponentHistoryHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selection = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		IArchimateConcept component;
		
		if ( selection instanceof IArchimateConcept ) {					// if the component is selected in the tree
			component = (IArchimateConcept)selection;
		} else if ( selection instanceof ArchimateElementEditPart ) {	// if the component is selected in a view
			component = (IArchimateConcept) ((ArchimateElementEditPart)selection).getModel().getArchimateConcept();
		} else {
			DBGui.popup(Level.ERROR, "Do not know which component you selected :(");
			return null;
		}
		
		if ( logger.isDebugEnabled() ) logger.debug("Showing history for component "+((IDBMetadata)component).getDBMetadata().getDebugName());
		
		new DBGuiComponentHistory(component);
		return null;
	}
}
