/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.swt.graphics.Image;

import com.archimatetool.canvas.CanvasEditorPlugin;
import com.archimatetool.canvas.ICanvasImages;
import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.canvas.model.ICanvasModelBlock;
import com.archimatetool.canvas.model.ICanvasModelConnection;
import com.archimatetool.canvas.model.ICanvasModelImage;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.ICanvasPackage;
import com.archimatetool.canvas.model.impl.CanvasFactory;
import com.archimatetool.editor.ui.ImageFactory;

/**
 * The <b>DBCanvasFactory</b> class overrides the com.archimatetool.canvas.model.impl.CanvasFactory class<br>
 * It creates DBxxx classes that extend the standard Archi classes by adding metadata.
 *
 * @author Herve JOUIN
 * @see com.archimatetool.canvas.model.impl.ACanvasFactory
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DBCanvasFactory extends CanvasFactory {
	final int PHYSICAL_SERVER = 240;
	static DBLogger logger = new DBLogger(DBCanvasFactory.class);
	static boolean ignoreNext = false;
	
	@SuppressWarnings("hiding")
	public static DBCanvasFactory eINSTANCE = init();
	
    public static DBCanvasFactory init() {
    	if ( logger.isDebugEnabled() ) logger.debug("initializing DBCanvasFactory");
        try {
        	DBCanvasFactory theCanvasFactory = (DBCanvasFactory)EPackage.Registry.INSTANCE.getEFactory(ICanvasPackage.eNS_URI);
            if (theCanvasFactory != null) {
                return theCanvasFactory;
            }
        }
        catch (Exception exception) {
            EcorePlugin.INSTANCE.log(exception);
        }
        return new DBCanvasFactory();
    }
	
    /**
     * Override of the original ArchimateFactory<br>
	 * Creates a DBxxxx instead of a xxxx objects that include DBMetadata properties 
     */
	public DBCanvasFactory() {
		super();
	}
	
	/**
	 * Creates a component by its class name
	 */
    public EObject create(String clazz) {
        switch (clazz.toUpperCase()) {
            case "CANVASMODEL": return createCanvasModel();
            case "CANVASMODELSTICKY": return createCanvasModelSticky();
            case "CANVASMODELBLOCK": return createCanvasModelBlock();
            case "CANVASMODELIMAGE": return createCanvasModelImage();
            case "CANVASMODELCONNECTION": return createCanvasModelConnection();
            default:
                throw new IllegalArgumentException("The class '" + clazz + "' is not a valid class"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    public static Image getImage(String clazz) {
		ImageFactory ImageFactory = new ImageFactory(CanvasEditorPlugin.INSTANCE);
        switch (clazz.toUpperCase()) {
            case "CANVASMODEL": return ImageFactory.getImage(ICanvasImages.ICON_CANVAS_MODEL);
            default:
                throw new IllegalArgumentException("The class '" + clazz + "' is not a valid class"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
	
	/**
	 * Override of the original createCanvasModel<br>
	 * Creates a DBCanvasModel instead of a CanvasModel 
	 */
    public ICanvasModel createCanvasModel() {
        return new org.archicontribs.database.model.impl.CanvasModel();
    }

	/**
	 * Override of the original createCanvasModelSticky<br>
	 * Creates a DBCanvasModelSticky instead of a CanvasModelSticky 
	 */
    public ICanvasModelSticky createCanvasModelSticky() {
        return new org.archicontribs.database.model.impl.CanvasModelSticky();
    }

	/**
	 * Override of the original createCanvasModelBlock<br>
	 * Creates a DBCanvasModelBlock instead of a CanvasModelBlock 
	 */
    public ICanvasModelBlock createCanvasModelBlock() {
        return new org.archicontribs.database.model.impl.CanvasModelBlock();
    }

	/**
	 * Override of the original createCanvasModelImage<br>
	 * Creates a DBCanvasModelImage instead of a CanvasModelImage 
	 */
    public ICanvasModelImage createCanvasModelImage() {
        return new org.archicontribs.database.model.impl.CanvasModelImage();
    }

	/**
	 * Override of the original createCanvasModelConnection<br>
	 * Creates a DBCanvasModelConnection instead of a CanvasModelConnection 
	 */
    public ICanvasModelConnection createCanvasModelConnection() {
        return new org.archicontribs.database.model.impl.CanvasModelConnection();
    }
}
