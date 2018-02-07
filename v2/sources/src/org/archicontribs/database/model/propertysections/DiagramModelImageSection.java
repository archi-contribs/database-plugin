/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.propertysections;

import org.archicontribs.database.GUI.DBGui;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.propertysections.AbstractArchimatePropertySection;
import com.archimatetool.editor.propertysections.ITabbedLayoutConstants;
import com.archimatetool.model.IDiagramModelImage;

/**
 * This class extends the com.archimatetool.editor.propertysections.DiagramModelImageSection, adding the ability to import an image from a database
 * 
 * @author Herve Jouin
 */
public class DiagramModelImageSection extends AbstractArchimatePropertySection {
	protected static final String HELP_ID = "com.archimatetool.help.elementPropertySection"; //$NON-NLS-1$
	private IDiagramModelImage fDiagramModelImage;
	
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
    	
    	Button btnImportImage = new Button(parent, SWT.PUSH);
    	btnImportImage.setText(" Import image from database..."); //$NON-NLS-1$
        getWidgetFactory().adapt(btnImportImage, true, true); // Need to do it this way for Mac
        GridData gd = new GridData(SWT.NONE, SWT.NONE, true, false);
        gd.minimumWidth = ITabbedLayoutConstants.COMBO_WIDTH;
        btnImportImage.setLayoutData(gd);
        btnImportImage.setAlignment(SWT.LEFT);
        btnImportImage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DBGuiImportImage importImage = new DBGuiImportImage(getPart().getSite().getShell(),
                        getEObject().getDiagramModel().getArchimateModel(),
                        ((IDiagramModelImageProvider)getEObject()).getImagePath());
                
                if(dialog.open() == Window.OK) {
                    setImage(dialog.getSelectedObject());
                }
            }
        });
    	btnImportImage.setBackground(DBGui.GROUP_BACKGROUND_COLOR);   // light grey
        
        // Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
    }

	@Override
	protected Adapter getECoreAdapter() {
		return null;
	}

	@Override
	protected EObject getEObject() {
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
