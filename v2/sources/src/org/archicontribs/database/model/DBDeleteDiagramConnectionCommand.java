/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.eclipse.gef.commands.Command;

import com.archimatetool.model.IDiagramModelConnection;

/**
 * Delete Diagram Connection Command<br>
 * <br>
 * This class is a copy of the {@link com.archimatetool.editor.diagram.commands.DeleteDiagramConnectionCommand} written by Phillip Beauvoir, but his class is not accessible.
 * 
 * @author Herve Jouin
 */
public class DBDeleteDiagramConnectionCommand extends Command {
    private IDiagramModelConnection fConnection;
    
    /** 
     * Create a command that will disconnect a connection from its endpoints.
     * @param connection the connection instance to disconnect (non-null)
     */
    public DBDeleteDiagramConnectionCommand(IDiagramModelConnection connection){
        this.fConnection = connection;
    }
    
    @Override
    public void execute() {
        this.fConnection.disconnect();
        ((IDBMetadata)((IDBMetadata)this.fConnection).getDBMetadata().getParentDiagram()).getDBMetadata().setChecksumValid(false);
    }
    
    @Override
    public void undo() {
        this.fConnection.reconnect();
    }


    @Override
    public void dispose() {
        this.fConnection = null;
    }
}
