/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.commands;

import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.Command;

/**
 * Command for resolving source and target for relationships
 * 
 * @author Herve Jouin
 */
public class DBResolveRelationshipsCommand extends Command implements IDBImportFromIdCommand {
    private DBArchimateModel model = null;
    
    private Exception exception = null;
    private boolean commandHasBeenExecuted = false;
    
    public DBResolveRelationshipsCommand(DBArchimateModel model) {
        this.model = model;
    }
    
    @Override
    public boolean canExecute() {
        return this.model != null;
    }
    
    @Override
    public void execute() {
    	if ( ! this.commandHasBeenExecuted ) {
	    	this.commandHasBeenExecuted = true;
	        try {
				this.model.resolveSourceAndTargetRelationships();
			} catch (Exception e) {
				this.exception = e;
			}
    	}
    }
    @Override
	public void undo() {
    	this.commandHasBeenExecuted = false;
    }
    
    // redo is same as execute

	@Override
	public EObject getImported() {
		return null;
	}

	@Override
	public Exception getException() {
		return this.exception;
	}
}
