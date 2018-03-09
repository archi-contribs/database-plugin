/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.eclipse.gef.commands.Command;

import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;

/**
 * Command for deleting an Object from its parent container.<br>
 * It puts it back at the index position from where it was removed.<br>
 * <br>
 * This class is a copy of the {@link com.archimatetool.editor.diagram.commands.DeleteDiagramObjectCommand} written by Phillip Beauvoir, but his class is not accessible.
 * 
 * @author Herve Jouin
 */
public class DBDeleteDiagramObjectCommand extends Command {
    private IDiagramModelContainer fParent;
    private IDiagramModelObject fObject;
    private int fIndex;
    
    public DBDeleteDiagramObjectCommand(IDiagramModelObject object) {
        this.fParent = (IDiagramModelContainer)object.eContainer();
        this.fObject = object;
    }

    @Override
    public boolean canExecute() {
        /*
         * Parent can be null when objects are selected (with marquee tool) and transferred from one container
         * to another and the Diagram Editor updates the enablement state of Actions.
         * Can also be null if already deleted as part of a Compound Command.
         */
        return this.fParent != null && this.fParent.getChildren().contains(this.fObject);
    }
    
    @Override
    public void execute() {
        // Ensure fIndex is stored just before execute because if this is part of a composite delete action
        // then the index positions will have changed
        this.fIndex = this.fParent.getChildren().indexOf(this.fObject); 
        if(this.fIndex != -1) { // might have already been deleted by another process
            this.fParent.getChildren().remove(this.fObject);
        }
    }
    
    @Override
    public void undo() {
        // Add the Child at old index position
        if(this.fIndex != -1) { // might have already been deleted by another process
            this.fParent.getChildren().add(this.fIndex, this.fObject);
        }
    }

    @Override
    public void dispose() {
        this.fParent = null;
        this.fObject = null;
    }
}
