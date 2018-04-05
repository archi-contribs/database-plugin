/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.util.ArrayList;

import org.eclipse.gef.commands.Command;

import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;

/**
 * Command for deleting an Object from its parent container.<br>
 * It puts it back at the index position from where it was removed.<br>
 * <br>
 * This class is inspired from {@link com.archimatetool.editor.diagram.commands.DeleteDiagramObjectCommand} written by Phillip Beauvoir, but is not recursive (deleting a view object does not delete its children)
 * 
 * @author Herve Jouin
 */
public class DBDeleteDiagramObjectCommand extends Command {
    private IDiagramModelContainer viewObjectParent;
    private IDiagramModelObject viewObject;
    private int viewObjectIndex;
    private ArrayList<IDiagramModelObject> viewObjectChildren;
    
    public DBDeleteDiagramObjectCommand(IDiagramModelObject object) {
        this.viewObjectParent = (IDiagramModelContainer)object.eContainer();
        this.viewObject = object;
        this.viewObjectChildren = new ArrayList<IDiagramModelObject>();
    }

    @Override
    public boolean canExecute() {
        /*
         * Parent can be null when objects are selected (with marquee tool) and transferred from one container
         * to another and the Diagram Editor updates the enablement state of Actions.
         * Can also be null if already deleted as part of a Compound Command.
         */
        return this.viewObjectParent != null && this.viewObjectParent.getChildren().contains(this.viewObject);
    }
    
    @Override
    public void execute() {
        // Ensure viewObjectIndex is stored just before execute because if this is part of a composite delete action, then the index positions will have changed
        this.viewObjectIndex = this.viewObjectParent.getChildren().indexOf(this.viewObject); 
        if ( this.viewObjectIndex != -1 ) {        // might have already been deleted by another process
            // we move the viewObject children to the viewObjectParent
            if ( this.viewObject instanceof IDiagramModelContainer ) {
                for ( IDiagramModelObject child: ((IDiagramModelContainer)this.viewObject).getChildren() )
                    this.viewObjectChildren.add(child);
                
                for ( IDiagramModelObject child: this.viewObjectChildren )
                    this.viewObjectParent.getChildren().add(child);
            }
            this.viewObjectParent.getChildren().remove(this.viewObject);
        }
    }
    
    @Override
    public void undo() {
        // Add the Child at old index position
        if ( this.viewObjectIndex != -1 ) {        // might have already been deleted by another process
            this.viewObjectParent.getChildren().add(this.viewObjectIndex, this.viewObject);
            
            // we restore the children to the viewObject
            for ( IDiagramModelObject child: this.viewObjectChildren ) {
                this.viewObjectParent.getChildren().remove(child);
                ((IDiagramModelContainer)this.viewObject).getChildren().add(child);
            }
        }
    }

    @Override
    public void dispose() {
        this.viewObjectParent = null;
        this.viewObject = null;
        this.viewObjectChildren = null;
    }
}
