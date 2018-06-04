/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.Command;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModelConnection;

/**
 * Delete Diagram Connection Command<br>
 * <br>
 * This class is a copy of the {@link com.archimatetool.editor.diagram.commands.DeleteDiagramConnectionCommand} written by Phillip Beauvoir
 * 
 * @author Herve Jouin
 */
public class DBDeleteDiagramConnectionCommand extends Command {
    private IDiagramModelConnection fConnection;
    private IArchimateModel fModel;
    
    /** 
     * Create a command that will disconnect a connection from its endpoints.
     * @param connection the connection instance to disconnect (non-null)
     */
    public DBDeleteDiagramConnectionCommand(IArchimateModel model, IDiagramModelConnection connection){
        this.fConnection = connection;
        this.fModel = model;
    }
    
    @Override
    public void execute() {
        ((DBArchimateModel)this.fModel).getAllViewObjects().remove(this.fConnection.getId());
        ((IDBMetadata)(this.fConnection).getDiagramModel()).getDBMetadata().setChecksumValid(false);
        this.fConnection.disconnect();
    }
    
    @Override
    public void undo() {
        this.fConnection.reconnect();
        ((DBArchimateModel)this.fModel).getAllViewConnections().put(this.fConnection.getId(), this.fConnection);
    }


    @Override
    public void dispose() {
        this.fConnection = null;
    }
}
