/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import org.apache.log4j.Level;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.diagram.util.DiagramUtils;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * Class that is called by the "replace IDs" context menu. Convert old Archi 3 IDs to Archi 4 IDs
 * 
 * @author Herve Jouin
 */
@SuppressWarnings("unused")
public class DBMenuConvertIdsHandler extends AbstractHandler {
    //private static final DBLogger logger = new DBLogger(DBMenu.class);

    /*
     * This method doesn't do anything but is used for debugging purpose :-)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBArchimateModel model = (DBArchimateModel) ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
        int idsReplaced = 0;

        DBGuiUtils.showPopupMessage("Checking IDs ...");

        try {
            model.countAllObjects();
            
            if ( model.getId().length() != 36 ) {
                model.setId(DBPlugin.createID(model));
                ++idsReplaced;
            }

            for (IFolder folder: model.getAllFolders().values() ) {
                if ( folder.getId().length() != 36 ) {
                    folder.setId(DBPlugin.createID(folder));
                    ++idsReplaced;
                }
            }

            for (IArchimateElement element: model.getAllElements().values() ) {
                if ( element.getId().length() != 36 ) {
                    element.setId(DBPlugin.createID(element));
                    ++idsReplaced;
                }
            }

            for (IArchimateRelationship relationship: model.getAllRelationships().values() ) {
                if ( relationship.getId().length() != 36 ) {
                    relationship.setId(DBPlugin.createID(relationship));
                    ++idsReplaced;
                }
            }

            for (IDiagramModel view: model.getAllViews().values() ) {
                if ( view.getId().length() != 36 ) {
                    view.setId(DBPlugin.createID(view));
                    ++idsReplaced;
                }
            }
            
            for (IDiagramModelComponent viewObject: model.getAllViewObjects().values() ) {
                if ( viewObject.getId().length() != 36 ) {
                    viewObject.setId(DBPlugin.createID(viewObject));
                    ++idsReplaced;
                }
            }
            
            for (IDiagramModelConnection viewConnection: model.getAllViewConnections().values() ) {
                if ( viewConnection.getId().length() != 36 ) {
                    viewConnection.setId(DBPlugin.createID(viewConnection));
                    ++idsReplaced;
                }
            }
        } catch (Exception e) {
            DBGuiUtils.closePopupMessage();
            DBGuiUtils.popup(Level.ERROR, "Could not convert the IDs.\n\n"+idsReplaced+" IDs have been replaced.", e);
            return null;
        }

        DBGuiUtils.closePopupMessage();
        DBGuiUtils.popup(Level.INFO, idsReplaced+" IDs have been replaced.");

        return null;
    }
}
