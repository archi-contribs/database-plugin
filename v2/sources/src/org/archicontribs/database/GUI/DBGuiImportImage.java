/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.GUI;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui.ACTION;
import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.propertysections.Messages;
import com.archimatetool.editor.utils.PlatformUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * This class holds the methods requires to import an image from the database
 * 
 * @author Herve Jouin
 */
public class DBGuiImportImage extends DBGui {
    @SuppressWarnings("hiding")
    private static final DBLogger logger = new DBLogger(DBGuiImportImage.class);
    
    private int DEFAULT_GALLERY_ITEM_SIZE = 128;
    private int MIN_GALLERY_ITEM_SIZE = 64;
    private int MAX_GALLERY_ITEM_SIZE = 256;
    
    String imagePath = null;
    
    private Group grpImages;
    Gallery gallery;
    GalleryItem rootGalleryItem;
    Scale scale;
    
    public DBGuiImportImage(ArchimateModel model, String title) throws Exception {
        // We call the DBGui constructor that will create the underlying form and expose the compoRight, compoRightUp and compoRightBottom composites
        super(title);
        
        if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for importing an image \""+model.getName()+"\" (plugin version "+DBPlugin.pluginVersion+").");
        
        createGrpImages();
        
        this.compoRightBottom.setVisible(true);
        this.compoRightBottom.layout();
        
        createAction(ACTION.One, "1 - select image");
        
        // we show an arrow in front of the first action
        setActiveAction(ACTION.One);
        
        // if the user select the "Import" button --> import the image, set the importedImagePath variable and close the window
        setBtnAction("Export", new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                //importImage();
                //DBGuiImportImage.this.importedImagePath = "xxx";
                close();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
        });
        
        // We rename the "close" button to "Import"
        this.btnClose.setText("Import");
        
        // We activate the Eclipse Help framework
        setHelpHref("importImage.html");
    }
    
    /**
     * Creates a group displaying the images from the database
     */
    private void createGrpImages() {
        this.grpImages = new Group(this.compoRightBottom, SWT.NONE);
        this.grpImages.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpImages.setText("Images in the database : ");
        this.grpImages.setFont(GROUP_TITLE_FONT);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpImages.setLayoutData(fd);
        this.grpImages.setLayout(new GridLayout(2, false));
        
        Composite galleryComposite = new Composite(this.grpImages, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        galleryComposite.setLayout(layout);
        
        this.gallery = new Gallery(galleryComposite, SWT.V_SCROLL | SWT.BORDER);
        this.gallery.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // Renderers
        final NoGroupRenderer groupRenderer = new NoGroupRenderer();
        groupRenderer.setItemSize(this.DEFAULT_GALLERY_ITEM_SIZE, this.DEFAULT_GALLERY_ITEM_SIZE);
        groupRenderer.setAutoMargin(true);
        groupRenderer.setMinMargin(10);
        this.gallery.setGroupRenderer(groupRenderer);
        
        final DefaultGalleryItemRenderer itemRenderer = new DefaultGalleryItemRenderer();
        itemRenderer.setDropShadows(true);
        itemRenderer.setDropShadowsSize(7);
        itemRenderer.setShowRoundedSelectionCorners(false);
        this.gallery.setItemRenderer(itemRenderer);
        
        // Root Group
        this.rootGalleryItem = new GalleryItem(this.gallery, SWT.NONE);
        
        // Slider
        this.scale = new Scale(galleryComposite, SWT.HORIZONTAL);
        GridData gd = new GridData(SWT.END, SWT.NONE, false, false);
        gd.widthHint = 120;
        if(PlatformUtils.isMac()) { // Mac clips height of slider
            gd.heightHint = 18;
        }
        this.scale.setLayoutData(gd);
        this.scale.setMinimum(this.MIN_GALLERY_ITEM_SIZE);
        this.scale.setMaximum(this.MAX_GALLERY_ITEM_SIZE);
        this.scale.setIncrement(8);
        this.scale.setPageIncrement(32);
        this.scale.setSelection(this.DEFAULT_GALLERY_ITEM_SIZE);
        this.scale.setEnabled(false);
        this.scale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int inc = DBGuiImportImage.this.scale.getSelection();
                itemRenderer.setDropShadows(inc >= 96);
                groupRenderer.setItemSize(inc, inc);
            }
        });

        // Gallery selections
        this.gallery.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(e.item instanceof GalleryItem)
                    DBGuiImportImage.this.imagePath = (String)((GalleryItem)e.item).getData("imagePath");
                else
                    DBGuiImportImage.this.imagePath = null;
             }
        });
        
        // Double-clicks
        this.gallery.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                GalleryItem item = DBGuiImportImage.this.gallery.getItem(new Point(event.x, event.y));
                if(item != null) {
                    close();
                }
            }
        });
        
        // Dispose of the images here not in the main dispose() method because if the help system is showing then 
        // the TrayDialog is resized and this control is asked to relayout.
        this.gallery.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                notConnectedToDatabase();       // we dispose the images
            }
        });
    }
    
    /**
     * This method is called each time a database is selected and a connection has been established to it.<br>
     */
    @Override
    protected void connectedToDatabase(boolean forceCheckDatabase) {
        notConnectedToDatabase();       // we dispose the images
        
        try {
            for ( String path: this.connection.getImageListFromDatabase() ) {
                GalleryItem item = new GalleryItem(this.rootGalleryItem, SWT.NONE);
                item.setImage(this.connection.getImageFromDatabase(path));
                item.setData("imagePath", path);
            }
        } catch (Exception err) {
            popup(Level.ERROR, "Failed to get images from the database", err);
        }
    }
    
    /**
     * 
     */
    
    /**
     * This method is called each time a connection to the database fails.<br>
     */
    @Override
    protected void notConnectedToDatabase() {
        if( this.rootGalleryItem != null && !this.rootGalleryItem.isDisposed()) {
            while(this.rootGalleryItem.getItemCount() > 0) {
                GalleryItem item = this.rootGalleryItem.getItem(0);
                this.rootGalleryItem.remove(item);
            }
        }
    }
    
    public String getImagePath() {
        return this.imagePath;
    }
}
