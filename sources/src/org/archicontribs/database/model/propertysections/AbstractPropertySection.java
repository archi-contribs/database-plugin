package org.archicontribs.database.model.propertysections;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import com.archimatetool.model.IAdapter;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.ILockable;

public abstract class AbstractPropertySection extends org.eclipse.ui.views.properties.tabbed.AbstractPropertySection{
    /**
     * EObjects that are the subject of this Property Section
     */
    private List<EObject> fObjects;
    
    protected TabbedPropertySheetPage fPage;
    private static int V_SPACING = 10;
    
    /**
     * Set this flag to true when executing a command to stop unnecessary refreshing of controls
     */
    protected boolean fIsExecutingCommand;
    
    /**
     * Create Label control. Style is set to SWT.WRAP
     * @param parent Parent composite
     * @param text Text to display
     * @param width Width of label in pixels
     * @param v_position Vertical position. Should be SWT.CENTER or SWT.NONE
     * @return
     */
    protected Label createLabel(Composite parent, String text, int width, int verticalPosition) {
        Label label = getWidgetFactory().createLabel(parent, text, SWT.WRAP);
        GridData gd = new GridData(SWT.NONE, verticalPosition, false, false);
        gd.widthHint = width;
        label.setLayoutData(gd);
        return label;
    }
    
    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        handleSelection((IStructuredSelection)selection);
        super.setInput(part, selection);
    }
    
    @Override
    public void dispose() {
        removeEObjectAdapter();
    }
    
    protected void handleSelection(IStructuredSelection selection) {
        // stop double-firing
        if(selection != getSelection()) { 
            // Remove previous listener adapter
            removeEObjectAdapter();
            
            // Get the correct EObjects
            this.fObjects = getFilteredObjects(selection.toList());
            
            // Update section
            update();
            
            // Add ECore listener adapter
            addEObjectAdapter();
        }
    }
    
    /**
     * Adapter to listen to model element changes
     * Use a EContentAdapter to listen to child changes
     */
    private Adapter eAdapter = new EContentAdapter()  {
        @Override
        public void notifyChanged(Notification msg) {
            super.notifyChanged(msg);
            AbstractPropertySection.this.notifyChanged(msg);
        }
    };
    
    /**
     * Notify that an Ecore event happened which might affect the underlying object in this property section
     * @param msg
     */
    protected void notifyChanged(Notification msg) {
        // nothing to do
    }
    
    private void removeEObjectAdapter() {
        EObject object = getFirstSelectedObject();
        
        if(object != null) {
            object.eAdapters().remove(this.eAdapter);
        }
    }
    
    private void addEObjectAdapter() {
        EObject object = getFirstSelectedObject();
        
        if(object != null && !object.eAdapters().contains(this.eAdapter)) {
            object.eAdapters().add(this.eAdapter);
        }
    }
    
    /**
     * Filter selected objects.
     * Also ensure that selected objects are from only one model.
     * We don't support selections from more than one model due to each model having its own command stack.
     * 
     * @return A list of filtered adaptable objects according to type
     */
    private List<EObject> getFilteredObjects(List<?> objects) {
        ArrayList<EObject> list = new ArrayList<EObject>();
        
        IObjectFilter filter = getFilter();
        
        // Get underlying object if a Filter is applied
        for(Object object : objects) {
            if(filter != null) {
                object = filter.adaptObject(object);
            }
            
            if(object instanceof EObject) {
                list.add((EObject)object);
            }
        }
        
        // Only use the objects that are in *one* model - the model in the first selected object
        if(!list.isEmpty() && (list.get(0) instanceof IArchimateModelObject)) {
            IArchimateModel firstModel = ((IArchimateModelObject)list.get(0)).getArchimateModel();
            
            // Remove objects with different parent models
            for(int i = list.size() - 1; i >= 1; i--) {
                IArchimateModelObject eObject = (IArchimateModelObject)list.get(i);
                if(eObject.getArchimateModel() != firstModel) {
                    list.remove(eObject);
                }
            }
        }
        
        return list;
    }
    
    /**
     * @return The Filter for this section
     */
    protected abstract IObjectFilter getFilter();
    
    /**
     * Update the Property section
     */
    protected abstract void update();
    
    
    /**
     * @return The first selected object
     */
    protected EObject getFirstSelectedObject() {
        return (this.fObjects == null || this.fObjects.isEmpty()) ? null : this.fObjects.get(0);
    }
    
    /**
     * @param object
     * @return true if object is of type ILockable and is locked
     */
    protected static boolean isLocked(Object object) {
        return object instanceof ILockable && ((ILockable)object).isLocked();
    }
    
    /**
     * If the Property sheet was Active (or Pinned) and the Element deleted then the Element's
     * info could still be showing.
     * @return True if alive
     */
    protected static boolean isAlive(EObject eObject) {
        return (eObject != null) && (eObject instanceof IArchimateModel || eObject.eContainer() != null);
    }
    
    /**
     * @return The EObjects for this Property Section
     */
    protected List<EObject> getEObjects() {
        return this.fObjects;
    }
    
    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createControls(parent, tabbedPropertySheetPage);
        this.fPage = tabbedPropertySheetPage;
        setLayout(parent);
        createControls(parent);
    }
    
    /**
     * Create the controls 
     * @param parent
     */
    protected abstract void createControls(Composite parent);
    
    /**
     * Set the layout for the main parent composite
     * @param parent
     */
    protected void setLayout(Composite parent) {
        GridLayout layout = new GridLayout(2, false);
        layout.marginTop = V_SPACING;
        layout.marginHeight = 0;
        layout.marginLeft = 3;
        layout.marginBottom = 2; 
        layout.verticalSpacing = V_SPACING;
        parent.setLayout(layout);
        
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, shouldUseExtraSpace()));
    }
    
    /**
     * Execute a command on the selected objects' CommandStack
     * @param cmd
     */
    protected void executeCommand(Command command) {
        this.fIsExecutingCommand = true;
        
        EObject eObject = getFirstSelectedObject();
        
        if(eObject != null && eObject instanceof IAdapter) {
            CommandStack commandStack = (CommandStack)((IAdapter)eObject).getAdapter(CommandStack.class);
            if(commandStack != null) {
                commandStack.execute(command);
            }
        }
        
        this.fIsExecutingCommand = false;
    }
    
    // ========================== Mac workaround ==========================
    // Used for Mac bug - see https://bugs.eclipse.org/bugs/show_bug.cgi?id=383750 
    
    protected Text fHiddenText;
    
    /**
     * Add hidden text field to section on a Mac because of a bug.
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=383750
     * @param parent
     */
    protected void addHiddenTextFieldToForm(Composite parent) {
        // This fix applies only on Mac OS systems
        if(!Platform.getOS().equals(Platform.OS_MACOSX)) {
            return;
        }

        // The grid data used to reduce space of the fake Text field
        final GridData hiddenData = new GridData(0, 0);
        // It takes 2 columns spaces in the table
        hiddenData.horizontalSpan = 2;

        // The fake Text field
        this.fHiddenText = new Text(parent, SWT.READ_ONLY);
        this.fHiddenText.setLayoutData(hiddenData);

        // Here is the trick. To hide the fake Text field, we change top margin
        // value to move the content up, and bottom margin to prevent from
        // cropping the end of the table content. This is very bad, but it works.
        ((GridLayout)parent.getLayout()).marginHeight = -5; // default was 5
        ((GridLayout)parent.getLayout()).marginBottom = 10; // default was 0
    }
    
    @Override
    public void refresh() {
        // Workaround for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=383750
        if(this.fHiddenText != null && !this.fHiddenText.isDisposed()) {
            this.fHiddenText.setFocus();
        }
    }
}
