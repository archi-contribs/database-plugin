/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.menu;

import java.util.HashMap;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
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
import com.archimatetool.canvas.editparts.CanvasDiagramPart;
import com.archimatetool.canvas.editparts.CanvasStickyEditPart;
import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.editor.diagram.editparts.ArchimateDiagramPart;
import com.archimatetool.editor.diagram.editparts.ArchimateElementEditPart;
import com.archimatetool.editor.diagram.editparts.ArchimateRelationshipEditPart;
import com.archimatetool.editor.diagram.editparts.DiagramConnectionEditPart;
import com.archimatetool.editor.diagram.editparts.diagram.DiagramImageEditPart;
import com.archimatetool.editor.diagram.editparts.diagram.GroupEditPart;
import com.archimatetool.editor.diagram.editparts.diagram.NoteEditPart;
import com.archimatetool.editor.diagram.sketch.editparts.SketchActorEditPart;
import com.archimatetool.editor.diagram.sketch.editparts.SketchDiagramPart;
import com.archimatetool.editor.diagram.sketch.editparts.SketchGroupEditPart;
import com.archimatetool.editor.diagram.sketch.editparts.StickyEditPart;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.INameable;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.impl.ArchimateModel;

/**
 * This class is used when the user right-click on a graphical object to add entries to the contextual menu
 *
 * @author Herve Jouin
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
                        boolean showDebugInContextMenu = DBPlugin.INSTANCE.getPreferenceStore().getBoolean("showIdInContextMenu");
                        Object obj = selection.getFirstElement();

                        if ( logger.isDebugEnabled() ) logger.debug("Showing menu for class "+obj.getClass().getSimpleName());

                        switch ( obj.getClass().getSimpleName() ) {

                            // when a user right clicks on a model
                            case "DBArchimateModel":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((ArchimateModel)obj);
                                showImportComponent();
                                showExportModel();
                                break;

                                // when the user right clicks in a diagram background
                            case "ArchimateDiagramPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((ArchimateDiagramPart)obj).getModel());
                                showImportComponentIntoView();
                                break;

                                // when the user right clicks in a canvas background
                            case "CanvasDiagramPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((CanvasDiagramPart)obj).getModel());
                                // cannot import (yet) a component into a canvas, except an image ...
                                break;

                                // when the user right clicks in a sketch background
                            case "SketchDiagramPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((SketchDiagramPart)obj).getModel());
                                // cannot import (yet) a component into a sketch
                                break;

                                // when the user right clicks in a diagram and an element is selected
                            case "ArchimateElementEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((ArchimateElementEditPart)obj).getModel());
                                showGetHistory(((ArchimateElementEditPart)obj).getModel().getArchimateElement());
                                showReplaceElement(((ArchimateElementEditPart)obj).getModel().getArchimateElement());
                                break;

                                // when the user right clicks in a diagram and a relationship is selected
                            case "ArchimateRelationshipEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((ArchimateRelationshipEditPart)obj).getModel());
                                showGetHistory(((ArchimateRelationshipEditPart)obj).getModel().getArchimateRelationship());
                                break;

                                // when the user right clicks in a canvas' block
                            case "CanvasBlockEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((CanvasBlockEditPart)obj).getModel());
                                break;

                                // when the user right clicks in a canvas' sticky
                            case "CanvasStickyEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) { 
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((CanvasStickyEditPart)obj).getModel());
                                break;

                                // when the user right clicks on a connection
                            case "DiagramConnectionEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((DiagramConnectionEditPart)obj).getModel());
                                break;

                                // when the user right clicks on an image
                            case "DiagramImageEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((DiagramImageEditPart)obj).getModel());
                                break;

                                // when the user right clicks on a group
                            case "GroupEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((GroupEditPart)obj).getModel());
                                break;

                                // when the user right clicks on a note
                            case "NoteEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((NoteEditPart)obj).getModel());
                                break;

                                // when the user right clicks on a sketch actor
                            case "SketchActorEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((SketchActorEditPart)obj).getModel());
                                break;

                                // when the user right clicks on a sketch group
                            case "SketchGroupEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((SketchGroupEditPart)obj).getModel());
                                break;

                                // when the user right clicks on a sticky
                            case "StickyEditPart":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory(((StickyEditPart)obj).getModel());
                                break;	

                                // when the user right clicks on a diagram in the model tree
                            case "ArchimateDiagramModel":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((IArchimateDiagramModel)obj);
                                showImportComponentIntoView();
                                break;

                                // when the user right clicks on a canvas in the model tree
                            case "CanvasModel":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((ICanvasModel)obj);
                                showImportComponent();
                                break;

                                // when the user right clicks on a sketch in the model tree
                            case "SketchModel":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((ISketchModel)obj);
                                showImportComponent();
                                break;
                            case "Folder":
                                additions.addContributionItem(new Separator(), null);
                                if ( showDebugInContextMenu ) {
                                    showDebug();
                                    additions.addContributionItem(new Separator(), null);
                                }
                                showGetHistory((IFolder)obj);
                                showImportComponent();
                                break;
                            default:
                                if ( obj instanceof IArchimateElement || obj instanceof IArchimateRelationship ) {
                                    additions.addContributionItem(new Separator(), null);
                                    if ( showDebugInContextMenu ) {
                                        showDebug();
                                        additions.addContributionItem(new Separator(), null);
                                    }
                                    showGetHistory((IArchimateConcept)obj);
                                    if ( obj instanceof IArchimateElement )
                                        showReplaceElement((IArchimateElement)obj);
                                } else {
                                    if ( logger.isDebugEnabled() ) logger.debug("No specific menu to show for this class ...");
                                }
                        }
                        break;

                    default:
                }
            }
        }
    }


    private void showGetHistory(INameable component) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/history.png"), null));
        String label = "Get history of "+component.eClass().getName()+" \""+component.getName()+"\"";
        if ( label.length() > 100 )
            label = label.substring(0, 100);
        
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("mustConsiderConcept", (component instanceof IArchimateConcept ? "yes" : "no"));

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),		// serviceLocator
                "org.archicontribs.database.DBMenu",						// id
                "org.archicontribs.database.componentHistoryCommand",       // commandId
                parameters,													// parameters
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
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
        String label = "Import model from database";

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
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
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/export.png"), null));
        String label = "Export model to database";

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
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
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
        String label = "Import components from database";

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
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
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/import.png"), null));
        String label = "Import components from database into view";

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
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
    
    private void showReplaceElement(IArchimateElement element) {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/replace.png"), null));
        String label = "Replace "+element.eClass().getName()+" \""+element.getName()+"\"";
        if ( label.length() > 100 )
            label = label.substring(0, 100);

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),       // serviceLocator
                "org.archicontribs.database.DBMenu",                        // id
                "org.archicontribs.database.elementReplaceCommand",         // commandId
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
    
    private void showDebug() {
        ImageDescriptor menuIcon = ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle("org.archicontribs.database"), new Path("img/16x16/debug.png"), null));
        String label = "Show debugging information";

        if ( logger.isDebugEnabled() ) logger.debug("Adding menu label: "+label);
        CommandContributionItemParameter p = new CommandContributionItemParameter(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),       // serviceLocator
                "org.archicontribs.database.DBMenu",                        // id
                "org.archicontribs.database.showDebugCommand",              // commandId
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
