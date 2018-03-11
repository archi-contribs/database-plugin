/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.propertysections;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.GUI.DBGuiImportImage;
import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.emf.common.notify.Adapter;
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
import com.archimatetool.editor.propertysections.AbstractArchimatePropertySection;
import com.archimatetool.editor.propertysections.ITabbedLayoutConstants;
import com.archimatetool.editor.propertysections.Messages;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelImage;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelObject;

/**
 * This class extends the com.archimatetool.editor.propertysections.DiagramModelImageSection, adding the ability to import an image from a database
 * 
 * @author Herve Jouin
 */
public class DiagramModelImageSection extends AbstractArchimatePropertySection {
	private static final DBLogger logger = new DBLogger(DiagramModelImageSection.class);
	
	protected static final String HELP_ID = "com.archimatetool.help.elementPropertySection"; //$NON-NLS-1$
	private IDiagramModelImage fDiagramModelImage;
	Button btnImportImage;
	
    /**
     * Filter to show or reject this section depending on input value
     */
    public static class Filter extends ObjectFilter {
        @Override
        protected boolean isRequiredType(Object object) {
            return object instanceof IDiagramModelImage;
        }

        @Override
        protected Class<?> getAdaptableType() {
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
                
                actionClear.setEnabled(((IDiagramModelImageProvider)getEObject()).getImagePath() != null);
                
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
    
    protected void chooseImage() {
        if(!isAlive()) {
            return;
        }
        
        DBGuiImportImage guiImportImage;
        try {
            guiImportImage = new DBGuiImportImage((ArchimateModel)getEObject().getDiagramModel().getArchimateModel(), "Import image");
            guiImportImage.run();
            while ( !guiImportImage.isDisposed() )
                DBGui.refreshDisplay();
            
            if ( guiImportImage.getImage() != null )
                setImage(guiImportImage.getImage(), guiImportImage.getImagePath());
        } catch (Exception e) {
            DBGui.popup(Level.ERROR,"Cannot import image", e);
        }
    }
    
    protected void setImage(Image image, String path) {
        this.fIsExecutingCommand = true;
        try {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	ImageIO.write(getBufferedImage(image), "PNG", baos);
            ((IArchiveManager)getEObject().getAdapter(IArchiveManager.class)).addByteContentEntry(path, baos.toByteArray());
	        getCommandStack().execute(new EObjectFeatureCommand("Set image", getEObject(), IArchimatePackage.Literals.DIAGRAM_MODEL_IMAGE_PROVIDER__IMAGE_PATH, path));
	        logger.debug("Image path set to " + path);
		} catch (IOException e) {
			logger.error("Failed to set image", e);
		}
        this.fIsExecutingCommand = false;
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
    
    protected void clearImage() {
        if(isAlive()) {
            this.fIsExecutingCommand = true;
            getCommandStack().execute(new EObjectFeatureCommand("Clear image", getEObject(), IArchimatePackage.Literals.DIAGRAM_MODEL_IMAGE_PROVIDER__IMAGE_PATH, null));
            this.fIsExecutingCommand = false;
        }
    }

	@Override
	protected Adapter getECoreAdapter() {
		return null;
	}

	@Override
	protected IDiagramModelObject getEObject() {
		return this.fDiagramModelImage;
	}

	@Override
	protected void setElement(Object element) {
        this.fDiagramModelImage = (IDiagramModelImage)new Filter().adaptObject(element);
        if(this.fDiagramModelImage == null) {
            System.err.println(getClass() + " failed to get element for " + element); //$NON-NLS-1$
        }
	}
	
}
