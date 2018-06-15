/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.commands;

import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.gef.commands.Command;

/**
 * Command for resolving source and target for relationships
 * 
 * @author Herve Jouin
 */
public class DBResolveRelationshipsCommand extends Command {
    private DBArchimateModel model = null;
    
    public DBResolveRelationshipsCommand(DBArchimateModel model) {
        this.model = model;
    }
    
    @Override
    public boolean canExecute() {
        return this.model != null;
    }
    
    @Override
    public void execute() {
        this.model.resolveSourceRelationships();
        this.model.resolveTargetRelationships();
    }
    
    // no need to undo
    
    // redo is same as execute
}
