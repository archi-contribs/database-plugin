/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import java.util.Iterator;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
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
import com.archimatetool.canvas.editparts.CanvasDiagramPart;
import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.editor.diagram.editparts.ArchimateDiagramPart;
import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.editor.diagram.editparts.ArchimateRelationshipEditPart;
import com.archimatetool.editor.diagram.editparts.DiagramConnectionEditPart;
import com.archimatetool.editor.diagram.editparts.diagram.DiagramImageEditPart;
import com.archimatetool.editor.diagram.editparts.diagram.GroupEditPart;
import com.archimatetool.editor.diagram.sketch.editparts.SketchDiagramPart;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ISketchModel;


/**
 * This class is used when the user right-click on a graphical object
 */
public class DBMenu extends ExtensionContributionFactory {
    private static final DBLogger logger = new DBLogger(DBMenu.class);

    @Override
    public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
        if ( logger.isTraceEnabled() ) logger.trace("Showing menu items");

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
            if ( selection != null ) {
                switch ( selection.size() ) {
                    case 0:
                        // if nothing is selected, we add an "import model" menu entry
                        additions.addContributionItem(importModel(), null);
                        break;

                    case 1:
                        boolean showIdInContextMenu = DBPlugin.INSTANCE.getPreferenceStore().getBoolean("showIdInContextMenu");
                        Object obj = selection.getFirstElement();

                        if ( logger.isDebugEnabled() ) logger.debug("Showing menu for class "+obj.getClass().getSimpleName());

                        switch ( obj.getClass().getSimpleName() ) {

                            // when a user right clicks on a model
                            case "DBArchimateModel" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("model", (DBArchimateModel)obj), null);
                                    additions.addContributionItem(showVersion((DBArchimateModel)obj), null);
                                    additions.addContributionItem(showChecksum("", (DBArchimateModel)obj), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(convertIds(), null);
                                additions.addContributionItem(exportModel(), null);
                                break;

                                // when the user right clicks in a diagram background
                            case "ArchimateDiagramPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("view ", ((ArchimateDiagramPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((ArchimateDiagramPart)obj).getModel()), null);
                                    additions.addContributionItem(showChecksum("", ((ArchimateDiagramPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory(((ArchimateDiagramPart)obj).getModel()), null);
                                additions.addContributionItem(importComponentIntoView(), null);
                                break;

                                // when the user right clicks in a canvas background
                            case "CanvasDiagramPart" :
                                additions.addContributionItem(getHistory(((CanvasDiagramPart)obj).getModel()), null);
                                // cannot import (yet) a component into a canvas, except an image ...
                                break;

                                // when the user right clicks in a sketch background
                            case "SketchDiagramPart" :
                                additions.addContributionItem(getHistory(((SketchDiagramPart)obj).getModel()), null);
                                // cannot import (yet) a component into a sketch
                                break;

                                // when the user right clicks in a diagram and an element is selected
                            case "ArchimateElementEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("object ", ((ArchimateElementEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((ArchimateElementEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showChecksum("", ((ArchimateElementEditPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                    additions.addContributionItem(showId("element ", ((ArchimateElementEditPart)obj).getModel().getArchimateElement()), null);
                                    additions.addContributionItem(showVersion(((ArchimateElementEditPart)obj).getModel().getArchimateElement()), null);
                                    additions.addContributionItem(showChecksum("", ((ArchimateElementEditPart)obj).getModel().getArchimateElement()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory(((ArchimateElementEditPart)obj).getModel().getArchimateElement()), null);
                                break;

                                // when the user right clicks in a diagram and a relationship is selected
                            case "ArchimateRelationshipEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("object ", ((ArchimateRelationshipEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((ArchimateRelationshipEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showChecksum("", ((ArchimateRelationshipEditPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                    additions.addContributionItem(showId("relationship ", ((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship()), null);
                                    additions.addContributionItem(showVersion(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship()), null);
                                    additions.addContributionItem(showChecksum("", ((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship()), null);
                                break;

                                // when the user right clicks in a canvas' block
                            case "CanvasBlockEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("block ", ((CanvasBlockEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((CanvasBlockEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showImagePath(((CanvasBlockEditPart)obj).getModel().getImagePath()), null);
                                    additions.addContributionItem(showChecksum("", ((CanvasBlockEditPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }	    				
                                break;

                                // when the user right clicks in a canvas' sticky
                            case "CanvasStickyEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) { 
                                    additions.addContributionItem(showId("sticky ", ((CanvasStickyEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((CanvasStickyEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showImagePath(((CanvasStickyEditPart)obj).getModel().getImagePath()), null);
                                    additions.addContributionItem(showChecksum("", ((CanvasStickyEditPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                break;

                                // when the user right clicks on a connection
                            case "DiagramConnectionEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("connection ", ((DiagramConnectionEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((DiagramConnectionEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showChecksum("", ((DiagramConnectionEditPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory(((DiagramConnectionEditPart)obj).getModel()), null);
                                break;

                                // when the user right clicks on an image
                            case "DiagramImageEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("image ", ((DiagramImageEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((DiagramImageEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showImagePath(((DiagramImageEditPart)obj).getModel().getImagePath()), null);
                                    additions.addContributionItem(showChecksum("", ((DiagramImageEditPart)obj).getModel()), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                break;

                                // when the user right clicks on a group
                            case "GroupEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("group ", ((GroupEditPart)obj).getModel()), null);
                                    additions.addContributionItem(showVersion(((GroupEditPart)obj).getModel()), null);
                                }
                                break;

                                // when the user right clicks on a note
                            case "NoteEditPart" :
                                break;

                                // when the user right clicks on a sketch actor
                            case "SketchActorEditPart" :
                                break;

                                // when the user right clicks on a sketch group
                            case "SketchGroupEditPart" :
                                break;

                                // when the user right clicks on a sticky
                            case "StickyEditPart" :
                                break;	

                                // when the user right clicks on a diagram in the model tree
                            case "ArchimateDiagramModel" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("view ", (IArchimateDiagramModel)obj), null);
                                    additions.addContributionItem(showVersion((IArchimateDiagramModel)obj), null);
                                    additions.addContributionItem(showChecksum("", (IArchimateDiagramModel)obj), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory((IArchimateDiagramModel)obj), null);
                                additions.addContributionItem(importComponentIntoView(), null);
                                break;

                                // when the user right clicks on a canvas in the model tree
                            case "CanvasModel" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("canvas ", (ICanvasModel)obj), null);
                                    additions.addContributionItem(showVersion((ICanvasModel)obj), null);
                                    additions.addContributionItem(showChecksum("", (ICanvasModel)obj), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory((ICanvasModel)obj), null);
                                additions.addContributionItem(importComponent(), null);
                                break;

                                // when the user right clicks on a sketch in the model tree
                            case "SketchModel" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("sketch ", (ISketchModel)obj), null);
                                    additions.addContributionItem(showVersion((ISketchModel)obj), null);
                                    additions.addContributionItem(showChecksum("", (ISketchModel)obj), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(getHistory((ISketchModel)obj), null);
                                additions.addContributionItem(importComponent(), null);
                                break;
                            case "Folder" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    additions.addContributionItem(showId("folder ", (IFolder)obj), null);
                                    additions.addContributionItem(showVersion((IFolder)obj), null);
                                    additions.addContributionItem(showChecksum("", (IFolder)obj), null);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                additions.addContributionItem(importComponent(), null);
                                break;
                            default :
                                if ( obj instanceof IArchimateElement || obj instanceof IArchimateRelationship ) {
                                    additions.addContributionItem(new Separator(), null);
                                    if ( showIdInContextMenu ) {
                                        additions.addContributionItem(showId("", (IIdentifier)obj), null);
                                        additions.addContributionItem(showVersion((IIdentifier)obj), null);
                                        additions.addContributionItem(showChecksum("", (IIdentifier)obj), null);
                                        additions.addContributionItem(new Separator(), null);
                                    }
                                    additions.addContributionItem(getHistory((IArchimateConcept)obj), null);
                                } else {
                                    if ( logger.isDebugEnabled() ) logger.debug("No specific menu to show for this class ...");
                                }
                        }
                        break;

                    default:
                        // If all the selected objects are models, we propose to merge them
                        Iterator<?> itr = selection.iterator();
                        boolean oneArchimateModel = false;
                        boolean allArchimateModels = true;
                        while ( itr.hasNext() ) {
                            if ( (itr.next() instanceof DBArchimateModel) ) {
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
    }


    static CommandContributionItem getHistory(IArchimateModelObject component) {
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

    static CommandContributionItem getHistory(IDiagramModelConnection connection) {
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

    static CommandContributionItem importModel() {
        ImageDescriptor menuIcon;
        String label;

        menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
        label = "Import model from database";

        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),       // serviceLocator
                "org.archicontribs.database.DBMenu",                        // id
                "org.archicontribs.database.modelImportCommand",            // commandId
                null,                                                       // parameters
                menuIcon,                                                   // icon
                null,                                                       // disabledIcon
                null,                                                       // hoverIcon
                label,                                                      // label
                null,                                                       // mnemonic
                null,                                                       // tooltip 
                CommandContributionItem.STYLE_PUSH,                         // style
                null,                                                       // helpContextId
                true);
        return new CommandContributionItem(p);
    }

    static CommandContributionItem exportModel() {
        ImageDescriptor menuIcon;
        String label;

        menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/export.png"), null));
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

    static CommandContributionItem importComponent() {
        ImageDescriptor menuIcon;
        String label;

        menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
        label = "Import individual component";

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

    static CommandContributionItem importComponentIntoView() {
        ImageDescriptor menuIcon;
        String label;

        menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
        label = "Import individual component into view";

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

    static CommandContributionItem showId(String prefix, IIdentifier component) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = prefix+"ID : "+component.getId();

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

    static CommandContributionItem showChecksum(String prefix, IIdentifier component) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = prefix+"Checksum: Initial="+((IDBMetadata)component).getDBMetadata().getInitialVersion().getChecksum() + " / Current="+((IDBMetadata)component).getDBMetadata().getCurrentVersion().getChecksum()+ " / Database="+((IDBMetadata)component).getDBMetadata().getDatabaseVersion().getChecksum();

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
    
    static CommandContributionItem showChecksum(String prefix, DBArchimateModel model) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = prefix+"Checksum: Initial="+model.getInitialVersion().getChecksum() + " / Current="+model.getExportedVersion().getChecksum()+ " / Database="+model.getLatestDatabaseVersion().getChecksum();

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

    static CommandContributionItem showVersion(IIdentifier component) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = "Version: Initial="+((IDBMetadata)component).getDBMetadata().getInitialVersion().getVersion() + " / Current="+((IDBMetadata)component).getDBMetadata().getCurrentVersion().getVersion()+ " / Database="+((IDBMetadata)component).getDBMetadata().getDatabaseVersion().getVersion();

        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),       // serviceLocator
                "org.archicontribs.database.DBMenu",                        // id
                "org.archicontribs.database.showIdCommand",                 // commandId
                null,                                                       // parameters
                menuIcon,                                                   // icon
                null,                                                       // disabledIcon
                null,                                                       // hoverIcon
                label,                                                      // label
                null,                                                       // mnemonic
                null,                                                       // tooltip 
                CommandContributionItem.STYLE_PUSH,                         // style
                null,                                                       // helpContextId
                true);
        return new CommandContributionItem(p);
    }
    
    static CommandContributionItem showVersion(DBArchimateModel model) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = "Version: Initial="+model.getInitialVersion().getVersion() + " / Current="+model.getExportedVersion().getVersion()+ " / Database="+model.getLatestDatabaseVersion().getVersion();

        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),       // serviceLocator
                "org.archicontribs.database.DBMenu",                        // id
                "org.archicontribs.database.showIdCommand",                 // commandId
                null,                                                       // parameters
                menuIcon,                                                   // icon
                null,                                                       // disabledIcon
                null,                                                       // hoverIcon
                label,                                                      // label
                null,                                                       // mnemonic
                null,                                                       // tooltip 
                CommandContributionItem.STYLE_PUSH,                         // style
                null,                                                       // helpContextId
                true);
        return new CommandContributionItem(p);
    }

    static CommandContributionItem convertIds() {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/app-16.png"), null));
        String label = "Convert old fashion IDs to Archi4";

        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),       // serviceLocator
                "org.archicontribs.database.DBMenu",                        // id
                "org.archicontribs.database.convertIdsCommand",              // commandId
                null,                                                       // parameters
                menuIcon,                                                   // icon
                null,                                                       // disabledIcon
                null,                                                       // hoverIcon
                label,                                                      // label
                null,                                                       // mnemonic
                null,                                                       // tooltip 
                CommandContributionItem.STYLE_PUSH,                         // style
                null,                                                       // helpContextId
                true);
        return new CommandContributionItem(p);
    }

    static CommandContributionItem showImagePath(String imagePath) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String path = (imagePath==null) ? "null" : imagePath;

        String label = "Image : "+path;

        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
                "org.archicontribs.database.DBMenu",						// id
                "org.archicontribs.database.showIdCommand", 	          	// commandId
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

    static CommandContributionItem mergeModels() {
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
