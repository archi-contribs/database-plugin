/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.model.FolderType;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;

@SuppressWarnings("unused")
public class DBMenuShowIdHandler extends AbstractHandler {
	//private static final DBLogger logger = new DBLogger(DBMenu.class);

	/*
	 * This method doesn't do anything but is used for debugging purpose :-)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selectedObject = ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
		
		if ( selectedObject instanceof IIdentifier ) {
			String id = ((IIdentifier)selectedObject).getId();
			int w = 12;
		}
		
		if ( selectedObject instanceof IFolder ) {
			FolderType type = ((IFolder)selectedObject).getType();
			int w = 12;
		}
		
		return null;
	}
}
