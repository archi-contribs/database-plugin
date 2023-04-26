/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.model.commands;

import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.gef.commands.Command;

/**
 * Command for resolving source and target for connections
 * 
 * @author Herve Jouin
 */
public class DBResolveConnectionsCommand extends Command implements IDBCommand {
    private DBArchimateModel model = null;
    
    private Exception exception = null;
    private boolean commandHasBeenExecuted = false;
    
    public DBResolveConnectionsCommand(DBArchimateModel archimateModel) {
        this.model = archimateModel;
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
				this.model.resolveSourceAndTargetConnections();
			} catch (Exception e) {
				this.exception = e;
			}
    	}
    }
    
    @Override
	public void undo() {
    	this.commandHasBeenExecuted = false;
    }

	@Override
	public Exception getException() {
		return this.exception;
	}

}
