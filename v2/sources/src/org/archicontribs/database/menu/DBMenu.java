package org.archicontribs.database.menu;

import java.util.Iterator;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.impl.ArchimateModel;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

import com.archimatetool.canvas.editparts.CanvasBlockEditPart;
import com.archimatetool.canvas.editparts.CanvasStickyEditPart;
import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.editor.diagram.editparts.ArchimateDiagramPart;
import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.editor.diagram.editparts.ArchimateRelationshipEditPart;
import com.archimatetool.editor.diagram.editparts.DiagramConnectionEditPart;
import com.archimatetool.editor.diagram.editparts.diagram.DiagramImageEditPart;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ISketchModel;



public class DBMenu extends ExtensionContributionFactory {
	private static final DBLogger logger = new DBLogger(DBMenu.class);

	@Override
    public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
		if ( logger.isTraceEnabled() ) logger.trace("Showing menu items");
		
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	    if (window != null)
	    {
	    	IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
	    	if ( selection.size() == 1 ) {
	    		Object obj = selection.getFirstElement();
	    		
	    		if ( logger.isDebugEnabled() ) logger.debug("Showing menu for class "+obj.getClass().getSimpleName());
	    		
	    		switch ( obj.getClass().getSimpleName() ) {
	    				// when a user right clicks on a model
	    			case "ArchimateModel" :
	    				additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId((ArchimateModel)obj), null);
	    				additions.addContributionItem(exportModel((ArchimateModel)obj), null);
	    				break;
	    				// when the user right clicks in a diagram background and no object is selected
	    			case "ArchimateDiagramPart" :
	    				additions.addContributionItem(new Separator(), null);
	    				if ( logger.isDebugEnabled() ) additions.addContributionItem(showId(((ArchimateDiagramPart)obj).getModel()), null);
	    				//additions.addContributionItem(importComponent(((ArchimateDiagramPart)obj).getModel().getArchimateModel(), ((ArchimateDiagramPart)obj).getModel().getId()), null);
	    				//TODO: find a way to create a component from an element
	    				break;
	    			case "CanvasDiagramPart" :
	    				// cannot import (yet) a component into a canvas, except an image ...
	    				//TODO : add the import of an image
	    				break;
	    			case "SketchDiagramPart" :
	    				// cannot import (yet) a component into a sketch
	    				break;
	    				
	    				// when the user right clicks in a diagram and a unique component is selected
		    		case "ArchimateElementEditPart" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId((IIdentifier)((ArchimateElementEditPart)obj).getModel().getArchimateElement()), null);
	    				additions.addContributionItem(getHistory(((ArchimateElementEditPart)obj).getModel().getArchimateElement()), null);
		    			break;
		    		case "ArchimateRelationshipEditPart" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId((IIdentifier)((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship()), null);
	    				additions.addContributionItem(getHistory(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship()), null);
		    			break;
		    		case "CanvasBlockEditPart" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId(((CanvasBlockEditPart)obj).getModel()), null);
	    				additions.addContributionItem(showPath(((CanvasBlockEditPart)obj).getModel().getImagePath()), null);
		    			break;
		    		case "CanvasStickyEditPart" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId(((CanvasStickyEditPart)obj).getModel()), null);
	    				additions.addContributionItem(showPath(((CanvasStickyEditPart)obj).getModel().getImagePath()), null);
		    			break;
		    		case "DiagramConnectionEditPart" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId((IIdentifier)((DiagramConnectionEditPart)obj).getModel()), null);
	    				additions.addContributionItem(getHistory(((DiagramConnectionEditPart)obj).getModel()), null);
		    			break;
		    		case "DiagramImageEditPart" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId(((DiagramImageEditPart)obj).getModel()), null);
	    				additions.addContributionItem(showPath(((DiagramImageEditPart)obj).getModel().getImagePath()), null);
		    			break;
		    		case "GroupEditPart" :
		    			break;
		    		case "NoteEditPart" :
		    			break;
		    		case "SketchActorEditPart" :
		    			break;
		    		case "SketchGroupEditPart" :
		    			break;	
		    		case "StickyEditPart" :
		    			break;	
		    			
		    			//When the user right clicks in the model tree
		    		case "ArchimateDiagramModel" :
		    			additions.addContributionItem(new Separator(), null);
	    				additions.addContributionItem(importComponent(((IArchimateDiagramModel)obj).getArchimateModel(), ((IArchimateDiagramModel)obj).getId()), null);
		    			break;
		    		case "CanvasModel" :
		    			additions.addContributionItem(new Separator(), null);
	    				additions.addContributionItem(importComponent(((ICanvasModel)obj).getArchimateModel(), null), null);
		    			break;
		    		case "SketchModel" :
		    			additions.addContributionItem(new Separator(), null);
	    				additions.addContributionItem(importComponent(((ISketchModel)obj).getArchimateModel(), null), null);
		    			break;
		    		case "Folder" :
		    			additions.addContributionItem(new Separator(), null);
		    			if ( logger.isDebugEnabled() ) additions.addContributionItem(showId((IFolder)obj), null);
	    				additions.addContributionItem(importComponent(((IFolder)obj).getArchimateModel(), null), null);
		    			break;
		    		default :
		    			if ( obj instanceof IArchimateElement || obj instanceof IArchimateRelationship ) {
		    				additions.addContributionItem(new Separator(), null);
		    				if ( logger.isDebugEnabled() ) additions.addContributionItem(showId((IIdentifier)obj), null);
		    				additions.addContributionItem(getHistory((IArchimateConcept)obj), null);
		    			} else {
		    				if ( logger.isDebugEnabled() ) logger.debug("No specific menu to show for this class ...");
		    			}
	    		}
	    	} else {
	    			// If all the selected objects are models, we propose to merge them
	    		Iterator<?> itr = selection.iterator();
	    		boolean oneArchimateModel = false;
	    		boolean allArchimateModels = true;
	    		while ( itr.hasNext() ) {
	    			if ( (itr.next() instanceof ArchimateModel) ) {
	    				oneArchimateModel = true;
	    			} else {
	    				allArchimateModels = false;
	    				break;
	    			}
	    		}
	    		if ( oneArchimateModel && allArchimateModels ) {
	    			additions.addContributionItem(new Separator(), null);
    				additions.addContributionItem(mergeModels(), null);
	    		}
	    	}
	    }
	}

	
	CommandContributionItem getHistory(IArchimateConcept component) {
		String clazz = component.eClass().getName().replaceAll("(.)([A-Z])", "$1 $2").trim().toLowerCase().replace(" ", "-");	// we generate the class name, the same way than used in Archi icons names
		ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/archimate/"+clazz+".png"), null));
		String label = "get history for "+component.eClass().getName()+" \""+component.getName()+"\"";
		if ( label.length() > 100 )
			label = label.substring(0, 100);

		if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
				"org.archicontribs.database.DBMenu",						// id
				"org.archicontribs.database.componentHistoryCommand",       // commandId
				null,														// parameters
				menuIcon,													// icon
				null,														// disabledIcon
				null,														// hoverIcon
				label,														// label
				null,														// mnemonic
				null,														// tooltip 
				CommandContributionItem.STYLE_PUSH,							// style
				null,														// helpContextId
				true);
		return new CommandContributionItem(p);
	}
	
	CommandContributionItem getHistory(IDiagramModelConnection connection) {
		String clazz = connection.eClass().getName().replaceAll("(.)([A-Z])", "$1 $2").trim().toLowerCase().replace(" ", "-");	// we generate the class name, the same way than used in Archi icons names
		ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/archimate/"+clazz+".png"), null));
		String label = "get history for "+connection.eClass().getName()+" \""+connection.getName()+"\"";
		if ( label.length() > 100 )
			label = label.substring(0, 100);

		if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
				"org.archicontribs.database.DBMenu",						// id
				"org.archicontribs.database.componentHistoryCommand",       // commandId
				null,														// parameters
				menuIcon,													// icon
				null,														// disabledIcon
				null,														// hoverIcon
				label,														// label
				null,														// mnemonic
				null,														// tooltip 
				CommandContributionItem.STYLE_PUSH,							// style
				null,														// helpContextId
				true);
		return new CommandContributionItem(p);
	}
	
	CommandContributionItem exportModel(IArchimateModel model) {
		ImageDescriptor menuIcon;
		String label;

		menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/app-16.png"), null));
		label = "Export model to database";

		if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
				"org.archicontribs.database.DBMenu",						// id
				"org.archicontribs.database.modelExportCommand",          	// commandId
				null,														// parameters
				menuIcon,													// icon
				null,														// disabledIcon
				null,														// hoverIcon
				label,														// label
				null,														// mnemonic
				null,														// tooltip 
				CommandContributionItem.STYLE_PUSH,							// style
				null,														// helpContextId
				true);
		return new CommandContributionItem(p);
	}
	
	CommandContributionItem importComponent(IArchimateModel model, String viewId) {
		ImageDescriptor menuIcon;
		String label;
		
		menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
		if ( viewId == null ) {
			label = "Import element";
		} else {
			label = "Import component into view";
		}

		if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
				"org.archicontribs.database.DBMenu",						// id
				"org.archicontribs.database.componentImportCommand",      	// commandId
				null,														// parameters
				menuIcon,													// icon
				null,														// disabledIcon
				null,														// hoverIcon
				label,														// label
				null,														// mnemonic
				null,														// tooltip 
				CommandContributionItem.STYLE_PUSH,							// style
				null,														// helpContextId
				true);
		return new CommandContributionItem(p);
	}
	
	CommandContributionItem showId(IIdentifier component) {
		//TODO : add a preference to show or hide this ID
		ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
		String label = "ID : "+component.getId();

		if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
				"org.archicontribs.database.DBMenu",						// id
				"org.archicontribs.database.showIdCommand",		          	// commandId
				null,														// parameters
				menuIcon,													// icon
				null,														// disabledIcon
				null,														// hoverIcon
				label,														// label
				null,														// mnemonic
				null,														// tooltip 
				CommandContributionItem.STYLE_PUSH,							// style
				null,														// helpContextId
				true);
		return new CommandContributionItem(p);
	}
	
	CommandContributionItem showPath(String path) {
		//TODO : add a preference to show or hide this path
		if ( path != null ) {
			ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
			String label = "Path : "+path;
	
			if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
			CommandContributionItemParameter p = new CommandContributionItemParameter(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
					"org.archicontribs.database.DBMenu",						// id
					"org.archicontribs.database.void",				          	// commandId
					null,														// parameters
					menuIcon,													// icon
					null,														// disabledIcon
					null,														// hoverIcon
					label,														// label
					null,														// mnemonic
					null,														// tooltip 
					CommandContributionItem.STYLE_PUSH,							// style
					null,														// helpContextId
					true);
			return new CommandContributionItem(p);
		}
		return null;
	}
	
	CommandContributionItem mergeModels() {
		ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/app-16.png"), null));
		String label = "Merge models";

		if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
				"org.archicontribs.database.DBMenu",						// id
				"org.archicontribs.database.mergeModelsCommand",	       	// commandId
				null,														// parameters
				menuIcon,													// icon
				null,														// disabledIcon
				null,														// hoverIcon
				label,														// label
				null,														// mnemonic
				null,														// tooltip 
				CommandContributionItem.STYLE_PUSH,							// style
				null,														// helpContextId
				true);
		return new CommandContributionItem(p);
	}
}
