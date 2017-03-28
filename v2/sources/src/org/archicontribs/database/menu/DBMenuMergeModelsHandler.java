package org.archicontribs.database.menu;

import java.util.Iterator;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class DBMenuMergeModelsHandler extends AbstractHandler {
	private static final DBLogger logger = new DBLogger(DBMenu.class);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Iterator<?> itr = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).iterator();
		StringBuilder modelNames = new StringBuilder();
		
		while ( itr.hasNext() ) {
			modelNames.append("+");
			modelNames.append(((ArchimateModel)itr.next()).getName());
		}

		if ( logger.isDebugEnabled() ) logger.debug("Merging models "+modelNames);
		
		//TODO : to be developped
		DBGui.popup(Level.INFO, "Not yet implemented.");
		
		return null;
	}
}
