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
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ISketchModel;


/**
 * This class is used when the user right-click on a graphical object
 */
public class DBMenu extends ExtensionContributionFactory {
    private static final DBLogger logger = new DBLogger(DBMenu.class);
    private IContributionRoot fAdditions = null;
    

    @Override
    public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
        if ( logger.isTraceEnabled() ) logger.trace("Showing menu items");
        this.fAdditions = additions;

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
            if ( selection != null ) {
                switch ( selection.size() ) {
                    case 0:
                        // if nothing is selected, we add an "import model" menu entry
                        showImportModel();
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
                                    showId("model", (DBArchimateModel)obj);
                                    showVersion((DBArchimateModel)obj);
                                    showChecksum("", (DBArchimateModel)obj);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showConvertIds();
                                showExportModel();
                                break;

                                // when the user right clicks in a diagram background
                            case "ArchimateDiagramPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("view ", ((ArchimateDiagramPart)obj).getModel());
                                    showVersion(((ArchimateDiagramPart)obj).getModel());
                                    showChecksum("", (IDBMetadata) ((ArchimateDiagramPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((ArchimateDiagramPart)obj).getModel());
                                showImportComponentIntoView();
                                break;

                                // when the user right clicks in a canvas background
                            case "CanvasDiagramPart" :
                                showGetHistory(((CanvasDiagramPart)obj).getModel());
                                // cannot import (yet) a component into a canvas, except an image ...
                                break;

                                // when the user right clicks in a sketch background
                            case "SketchDiagramPart" :
                                showGetHistory(((SketchDiagramPart)obj).getModel());
                                // cannot import (yet) a component into a sketch
                                break;

                                // when the user right clicks in a diagram and an element is selected
                            case "ArchimateElementEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("object ", ((ArchimateElementEditPart)obj).getModel());
                                    showVersion(((ArchimateElementEditPart)obj).getModel());
                                    showChecksum("", (IDBMetadata) ((ArchimateElementEditPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                    showId("element ", ((ArchimateElementEditPart)obj).getModel().getArchimateElement());
                                    showVersion(((ArchimateElementEditPart)obj).getModel().getArchimateElement());
                                    showChecksum("", (IDBMetadata) ((ArchimateElementEditPart)obj).getModel().getArchimateElement());
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((ArchimateElementEditPart)obj).getModel().getArchimateElement());
                                break;

                                // when the user right clicks in a diagram and a relationship is selected
                            case "ArchimateRelationshipEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("object ", ((ArchimateRelationshipEditPart)obj).getModel());
                                    showVersion(((ArchimateRelationshipEditPart)obj).getModel());
                                    showChecksum("", (IDBMetadata) ((ArchimateRelationshipEditPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                    showId("relationship ", ((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
                                    showVersion(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
                                    showChecksum("", (IDBMetadata) ((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
                                break;

                                // when the user right clicks in a canvas' block
                            case "CanvasBlockEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("block ", ((CanvasBlockEditPart)obj).getModel());
                                    showVersion(((CanvasBlockEditPart)obj).getModel());
                                    showImagePath(((CanvasBlockEditPart)obj).getModel().getImagePath());
                                    showChecksum("", (IDBMetadata) ((CanvasBlockEditPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                }	    				
                                break;

                                // when the user right clicks in a canvas' sticky
                            case "CanvasStickyEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) { 
                                    showId("sticky ", ((CanvasStickyEditPart)obj).getModel());
                                    showVersion(((CanvasStickyEditPart)obj).getModel());
                                    showImagePath(((CanvasStickyEditPart)obj).getModel().getImagePath());
                                    showChecksum("", (IDBMetadata) ((CanvasStickyEditPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                }
                                break;

                                // when the user right clicks on a connection
                            case "DiagramConnectionEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("connection ", ((DiagramConnectionEditPart)obj).getModel());
                                    showVersion(((DiagramConnectionEditPart)obj).getModel());
                                    showChecksum("", (IDBMetadata) ((DiagramConnectionEditPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((DiagramConnectionEditPart)obj).getModel());
                                break;

                                // when the user right clicks on an image
                            case "DiagramImageEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("image ", ((DiagramImageEditPart)obj).getModel());
                                    showVersion(((DiagramImageEditPart)obj).getModel());
                                    showImagePath(((DiagramImageEditPart)obj).getModel().getImagePath());
                                    showChecksum("", (IDBMetadata) ((DiagramImageEditPart)obj).getModel());
                                    additions.addContributionItem(new Separator(), null);
                                }
                                break;

                                // when the user right clicks on a group
                            case "GroupEditPart" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("group ", ((GroupEditPart)obj).getModel());
                                    showVersion(((GroupEditPart)obj).getModel());
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
                                    showId("view ", (IArchimateDiagramModel)obj);
                                    showVersion((IArchimateDiagramModel)obj);
                                    showChecksum("", (IDBMetadata)obj);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((IArchimateDiagramModel)obj);
                                showImportComponentIntoView();
                                break;

                                // when the user right clicks on a canvas in the model tree
                            case "CanvasModel" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("canvas ", (ICanvasModel)obj);
                                    showVersion((ICanvasModel)obj);
                                    showChecksum("", (IDBMetadata)obj);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((ICanvasModel)obj);
                                showImportComponent();
                                break;

                                // when the user right clicks on a sketch in the model tree
                            case "SketchModel" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("sketch ", (ISketchModel)obj);
                                    showVersion((ISketchModel)obj);
                                    showChecksum("", (IDBMetadata)obj);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((ISketchModel)obj);
                                showImportComponent();
                                break;
                            case "Folder" :
                                additions.addContributionItem(new Separator(), null);
                                if ( showIdInContextMenu ) {
                                    showId("folder ", (IFolder)obj);
                                    showVersion((IFolder)obj);
                                    showChecksum("", (IDBMetadata)obj);
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showImportComponent();
                                break;
                            default :
                                if ( obj instanceof IArchimateElement || obj instanceof IArchimateRelationship ) {
                                    additions.addContributionItem(new Separator(), null);
                                    if ( showIdInContextMenu ) {
                                        showId("", (IIdentifier)obj);
                                        showVersion((IIdentifier)obj);
                                        showChecksum("", (IDBMetadata)obj);
                                        additions.addContributionItem(new Separator(), null);
                                    }
                                    showGetHistory((IArchimateConcept)obj);
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
                            showMergeModels();
                        }
                }
            }
        }
    }


    private void showGetHistory(IArchimateModelObject component) {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showGetHistory(IDiagramModelConnection connection) {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showImportModel() {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showExportModel() {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showImportComponent() {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showImportComponentIntoView() {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showId(String prefix, IIdentifier component) {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showChecksum(String prefix, IDBMetadata component) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        
        String label = prefix+"Checksum:";
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        label = prefix+"     Initial = "+ component.getDBMetadata().getInitialVersion().getChecksum();
        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        p = new CommandContributionItemParameter(
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        label = prefix+"     Current = "+ component.getDBMetadata().getCurrentVersion().getChecksum();
        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        p = new CommandContributionItemParameter(
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        label = prefix+"     Database = " + component.getDBMetadata().getDatabaseVersion().getChecksum();
        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        p = new CommandContributionItemParameter(
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        label = prefix+"     Latest db = " + component.getDBMetadata().getLatestDatabaseVersion().getChecksum();
        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        p = new CommandContributionItemParameter(
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        if ( component instanceof IDiagramModel ) {
            label = prefix+"Container:";
            if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
            p = new CommandContributionItemParameter(
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
            this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
            
            label = prefix+"     Initial = "+ component.getDBMetadata().getInitialVersion().getContainerChecksum();
            if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
            p = new CommandContributionItemParameter(
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
            this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
            
            label = prefix+"     Current = "+ component.getDBMetadata().getCurrentVersion().getContainerChecksum();
            if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
            p = new CommandContributionItemParameter(
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
            this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
            
            label = prefix+"     Database = " + component.getDBMetadata().getDatabaseVersion().getContainerChecksum();
            if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
            p = new CommandContributionItemParameter(
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
            this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
            
            label = prefix+"     Latest db = " + component.getDBMetadata().getLatestDatabaseVersion().getContainerChecksum();
            if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
            p = new CommandContributionItemParameter(
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
            this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        }
    }
    
    private void showChecksum(String prefix, DBArchimateModel model) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        
        String label = prefix+"Checksum: Initial = " + model.getInitialVersion().getChecksum();
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        label = prefix+"          Current = " + model.getCurrentVersion().getChecksum();
        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        p = new CommandContributionItemParameter(
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
        
        label = prefix+"          Database = " + model.getDatabaseVersion().getChecksum();
        if ( logger.isDebugEnabled() ) logger.debug("adding menu label : "+label);
        p = new CommandContributionItemParameter(
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showVersion(IIdentifier component) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = "Version: Initial="+((IDBMetadata)component).getDBMetadata().getInitialVersion().getVersion() + " / Current="+((IDBMetadata)component).getDBMetadata().getCurrentVersion().getVersion()+ " / Database="+((IDBMetadata)component).getDBMetadata().getDatabaseVersion().getVersion()+ " / Latest db="+((IDBMetadata)component).getDBMetadata().getLatestDatabaseVersion().getVersion();

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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }
    
    private void showVersion(DBArchimateModel model) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("com.archimatetool.editor"), new Path("img/minus.png"), null));
        String label = "Version: Initial="+model.getInitialVersion().getVersion() + " / Current="+model.getCurrentVersion().getVersion()+ " / Latest db="+model.getDatabaseVersion().getVersion();

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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showConvertIds() {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showImagePath(String imagePath) {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }

    private void showMergeModels() {
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
        this.fAdditions.addContributionItem(new CommandContributionItem(p), null);
    }
}
