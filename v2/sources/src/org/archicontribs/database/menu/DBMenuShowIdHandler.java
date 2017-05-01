/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.diagram.util.DiagramUtils;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

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
		
		ArchimateModel model = (ArchimateModel) ((IArchimateModelObject)selectedObject).getArchimateModel();
		
		for ( IDiagramModel view: model.getAllViews().values() ) {
			Image image = DiagramUtils.createImage(view, 1, 10);

			ImageLoader saver = new ImageLoader();
			saver.data = new ImageData[] { image.getImageData() };
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("d:\\"+view.getName()+".png");

				saver.save(fos, SWT.IMAGE_PNG);
				fos.flush();
				fos.close();
				image.dispose();
				
				System.out.println("Image of view \""+view.getName()+"\" created");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		return null;
	}
}
