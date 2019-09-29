package org.archicontribs.database.model.propertysections;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.IFilter;

import com.archimatetool.editor.ui.factory.IObjectUIProvider;
import com.archimatetool.editor.ui.factory.ObjectUIFactory;

/**
 * Object Filter class to show or reject this section depending on input value
 */
public abstract class ObjectFilter implements IFilter, IObjectFilter {
    
    @Override
    public boolean select(Object object) {
        return adaptObject(object) != null;
    }
    
    @Override
    public Object adaptObject(Object object) {
        if(isRequiredType(object)) {
            return object;
        }
        
        if(object instanceof IAdaptable) {
            Object obj = ((IAdaptable)object).getAdapter(getAdaptableType());
            return isRequiredType(obj) ? obj : null;
        }
        
        return null;
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public boolean shouldExposeFeature(EObject eObject, EAttribute feature) {
        IObjectUIProvider provider = ObjectUIFactory.INSTANCE.getProvider(eObject);
        
        if(provider != null) {
            return provider.shouldExposeFeature(feature);
        }
        
        return true;
    }
}