/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.propertysections;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiImportImage;
import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.commands.EObjectFeatureCommand;
import com.archimatetool.editor.propertysections.ITabbedLayoutConstants;
import com.archimatetool.editor.propertysections.Messages;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelImage;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelObject;

/**
 * This class extends the com.archimatetool.editor.propertysections.AbstractECorePropertySection, adding the ability to import an image from a database
 * 
 * @author Herve Jouin
 */
public class DiagramModelImageSection extends AbstractPropertySection {
	private static final DBLogger logger = new DBLogger(DiagramModelImageSection.class);
	
	protected static final String HELP_ID = "com.archimatetool.help.elementPropertySection"; //$NON-NLS-1$
	private IDiagramModelImage fDiagramModelImage;
	Button btnImportImage;
	
    /**
     * Filter to show or reject this section depending on input value
     */
    public static class Filter extends ObjectFilter {
        @Override
        public boolean isRequiredType(Object object) {
            return object instanceof IDiagramModelImage;
        }

        @Override
        public Class<?> getAdaptableType() {
            return IDiagramModelImage.class;
        }
    }
    
    @Override
    protected void createControls(Composite parent) {
    	// we create an empty label to align the button to the others buttons on the page
    	createLabel(parent, "", ITabbedLayoutConstants.STANDARD_LABEL_WIDTH, SWT.CENTER);
    	
    	this.btnImportImage = new Button(parent, SWT.PUSH);
    	this.btnImportImage.setText(" Import image from database..."); //$NON-NLS-1$
        getWidgetFactory().adapt(this.btnImportImage, true, true); // Need to do it this way for Mac
        GridData gd = new GridData(SWT.NONE, SWT.NONE, true, false);
        gd.minimumWidth = ITabbedLayoutConstants.COMBO_WIDTH;
        this.btnImportImage.setLayoutData(gd);
        this.btnImportImage.setAlignment(SWT.LEFT);
        this.btnImportImage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                MenuManager menuManager = new MenuManager();
                
                IAction actionChoose = new Action(Messages.DiagramModelImageSection_2) {
                    @Override
                    public void run() {
                        chooseImage();
                    }
                };
                
                menuManager.add(actionChoose);
                
                IAction actionClear = new Action(Messages.DiagramModelImageSection_3) {
                    @Override
                    public void run() {
                        clearImage();
                    }
                };
                
                IDiagramModelImageProvider imageProvider = (IDiagramModelImageProvider)getFirstSelectedObject();
                if ( imageProvider != null )
                    actionClear.setEnabled(imageProvider.getImagePath() != null);
                
                menuManager.add(actionClear);
                
                Menu menu = menuManager.createContextMenu(DiagramModelImageSection.this.btnImportImage.getShell());
                Point p = DiagramModelImageSection.this.btnImportImage.getParent().toDisplay(DiagramModelImageSection.this.btnImportImage.getBounds().x, DiagramModelImageSection.this.btnImportImage.getBounds().y + DiagramModelImageSection.this.btnImportImage.getBounds().height);
                menu.setLocation(p);
                menu.setVisible(true);
            }
        });
    	this.btnImportImage.setBackground(DBGui.GROUP_BACKGROUND_COLOR);   // light grey
        
        // Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
    }
    
    @Override
    protected IObjectFilter getFilter() {
        return new Filter();
    }
    
    @Override
    protected void notifyChanged(Notification msg) {
        if(msg.getNotifier() == getFirstSelectedObject()) {
            Object feature = msg.getFeature();
            if(feature == IArchimatePackage.Literals.LOCKABLE__LOCKED) {
                refreshButtons();
            }
        }
    }
    
    @Override
    protected void update() {
        refreshButtons();
    }
    
    protected void refreshButtons() {
        this.btnImportImage.setEnabled(!isLocked(getFirstSelectedObject()));
    }
    
    protected void clearImage() {
        CompoundCommand result = new CompoundCommand();

        for(EObject dmo : getEObjects()) {
            if(isAlive(dmo)) {
                Command cmd = new EObjectFeatureCommand("Clear image", dmo, IArchimatePackage.Literals.DIAGRAM_MODEL_IMAGE_PROVIDER__IMAGE_PATH, null);

                if(cmd.canExecute()) {
                    result.add(cmd);
                }
            }
        }

        executeCommand(result.unwrap());
    }
    
    protected void chooseImage() {
        IDiagramModelObject dmo = (IDiagramModelObject)getFirstSelectedObject();
        
        if(isAlive(dmo)) {
            DBGuiImportImage guiImportImage;
            try {
                guiImportImage = new DBGuiImportImage((DBArchimateModel)dmo.getDiagramModel().getArchimateModel(), "Import image");
                guiImportImage.run();
                while ( !guiImportImage.isDisposed() )
                    DBGui.refreshDisplay();
                
                if ( guiImportImage.getImage() != null )
                    setImage(guiImportImage.getImage(), guiImportImage.getImagePath());
            } catch (Exception e) {
                DBGui.popup(Level.ERROR,"Cannot import image", e);
            }
        }
    }
    
    protected void setImage(Image image, String path) {
        try {
            CompoundCommand result = new CompoundCommand();
            
            for(EObject dmo : getEObjects()) {
                if(isAlive(dmo)) {
                	ByteArrayOutputStream baos = new ByteArrayOutputStream();
                	ImageIO.write(getBufferedImage(image), "PNG", baos);
                    ((IArchiveManager)((IDiagramModelObject)dmo).getAdapter(IArchiveManager.class)).addByteContentEntry(path, baos.toByteArray());
                    Command cmd = new EObjectFeatureCommand("Set image", dmo, IArchimatePackage.Literals.DIAGRAM_MODEL_IMAGE_PROVIDER__IMAGE_PATH, path);
                    if(cmd.canExecute()) {
                        result.add(cmd);
                    }
        	        logger.debug("Image path set to " + path);
                }
            }
            
            executeCommand(result.unwrap());
		} catch (IOException e) {
			logger.error("Failed to set image", e);
		}
    }

	static BufferedImage getBufferedImage(Image image) {
		// code from https://m4tx.pl/en/2013/01/java-swt-to-awt-and-vice-versa-image-conversion-with-transparency-support/
        ColorModel colorModel = null;
        ImageData data = image.getImageData();
        PaletteData palette = data.palette;
        BufferedImage bufferedImage = null;
        if (palette.isDirect) {
            bufferedImage = new BufferedImage(data.width, data.height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int pixel = data.getPixel(x, y);
                    RGB rgb = palette.getRGB(pixel);
                    bufferedImage.setRGB(x, y, data.getAlpha(x, y) << 24 | rgb.red << 16 | rgb.green << 8 | rgb.blue);
                }
            }
        } else {
            RGB[] rgbs = palette.getRGBs();
            byte[] red = new byte[rgbs.length];
            byte[] green = new byte[rgbs.length];
            byte[] blue = new byte[rgbs.length];
            for (int i = 0; i < rgbs.length; i++) {
                RGB rgb = rgbs[i];
                red[i] = (byte) rgb.red;
                green[i] = (byte) rgb.green;
                blue[i] = (byte) rgb.blue;
            }
            if (data.transparentPixel != -1)
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
            else
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
            bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int pixel = data.getPixel(x, y);
                    pixelArray[0] = pixel;
                    raster.setPixel(x, y, pixelArray);
                }
            }
        }
        return bufferedImage;
	}
	
    /**
     * @return The EObject for this Property Section (for 4.2 and prior compatibility)
     */
    protected EObject getEObject() {
        return this.fDiagramModelImage;
    }
    
    /**
     * sets the EObject for this Property Section (for 4.2 and prior compatibility)
     */
    protected void setElement(Object element) {
        this.fDiagramModelImage = (IDiagramModelImage)new Filter().adaptObject(element);
        if(this.fDiagramModelImage == null) {
            System.err.println(getClass() + " failed to get element for " + element); //$NON-NLS-1$
        }
    }
}
