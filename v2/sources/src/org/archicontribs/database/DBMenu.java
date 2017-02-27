package org.archicontribs.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archicontribs.database.model.ArchimateModel;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

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



public class DBMenu extends CompoundContributionItem {
	private static final DBLogger logger = new DBLogger(DBMenu.class);

	protected IContributionItem[] getContributionItems() {
		if ( logger.isTraceEnabled() ) logger.trace("Showing menu items");
		List<IContributionItem> contributionItems = new ArrayList<IContributionItem>();
		
		
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	    if (window != null)
	    {
	    	IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
	    	if ( selection.size() == 1 ) {
	    		Object obj = selection.getFirstElement();
	    		CommandContributionItem item1 = null;
	    		CommandContributionItem item2 = null;
	    		
	    		if ( logger.isDebugEnabled() ) logger.debug("Showing menu for class "+obj.getClass().getSimpleName());
	    		
	    		switch ( obj.getClass().getSimpleName() ) {
	    				// when a user right clicks on a model
	    			case "ArchimateModel" :
	    				item1 = exportModel((ArchimateModel)obj);
	    				break;
	    				// when the user right clicks in a diagram background and no object is selected
	    			case "ArchimateDiagramPart" :
	    				item1 = importComponent(((ArchimateDiagramPart)obj).getModel().getArchimateModel(), ((ArchimateDiagramPart)obj).getModel().getId());
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
		    			item1 = showId((IIdentifier)((ArchimateElementEditPart)obj).getModel().getArchimateElement());
		    			item2 = getHistory(((ArchimateElementEditPart)obj).getModel().getArchimateElement());
		    			break;
		    		case "ArchimateRelationshipEditPart" :
		    			item1 = showId((IIdentifier)((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
		    			item2 = getHistory(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
		    			break;
		    		case "CanvasBlockEditPart" :
		    			item1 = showPath(((CanvasBlockEditPart)obj).getModel().getImagePath());
		    			break;
		    		case "CanvasStickyEditPart" :
		    			item2 = showPath(((CanvasStickyEditPart)obj).getModel().getImagePath());
		    			break;
		    		case "DiagramConnectionEditPart" :
		    			item1 = showId((IIdentifier)((DiagramConnectionEditPart)obj).getModel());
		    			item2 = getHistory(((DiagramConnectionEditPart)obj).getModel());
		    			break;
		    		case "DiagramImageEditPart" :
		    			item1 = showPath(((DiagramImageEditPart)obj).getModel().getImagePath());
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
		    			item2 = importComponent(((IArchimateDiagramModel)obj).getArchimateModel(), ((IArchimateDiagramModel)obj).getId());
		    			break;
		    		case "CanvasModel" :
		    			item2 = importComponent(((ICanvasModel)obj).getArchimateModel(), null);
		    			break;
		    		case "SketchModel" :
		    			item2 = importComponent(((ISketchModel)obj).getArchimateModel(), null);
		    			break;
		    		case "Folder" :
		    			item2 = importComponent(((IFolder)obj).getArchimateModel(), null);
		    			break;
		    		default :
		    			if ( obj instanceof IArchimateElement || obj instanceof IArchimateRelationship ) {
		    				item1 = showId((IIdentifier)obj);
		    				item2 = getHistory((IArchimateConcept)obj);
		    			} else {
		    				if ( logger.isDebugEnabled() ) logger.debug("No specific menu to show for this class ...");
		    			}
	    		}
	    		if ( item1 != null )
		    		contributionItems.add(item1);
		    	if ( item2 != null )
		    		contributionItems.add(item2);
	    	} else {
	    			// If all the selected objects are models, we propose to merge them
	    		Iterator<?> itr = selection.iterator();
	    		boolean allArchimateModels = true;
	    		while ( itr.hasNext() ) {
	    			if ( !(itr.next() instanceof ArchimateModel) ) {
	    				allArchimateModels = false;
	    				break;
	    			}
	    		}
	    		if ( allArchimateModels ) {
	    			contributionItems.add(mergeModels());
	    		}
	    	}
	    }
		return contributionItems.toArray(new IContributionItem[contributionItems.size()]);
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
