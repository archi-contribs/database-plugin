/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import org.archicontribs.database.DBException;
import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.gef.commands.Command;

import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;

/**
 * Delete Diagram Connection Command<br>
 * <br>
 * This class is based on {@link com.archimatetool.editor.diagram.commands.DeleteDiagramConnectionCommand} written by Phillip Beauvoir
 * 
 * @author Herve Jouin
 */
public class DBDeleteDiagramConnectionCommand extends Command implements IDBCommand {
    private IDiagramModelConnection fConnection;
    private DBArchimateModel fModel;
    DBException exception = null;
    
    /** 
     * Create a command that will disconnect a connection from its endpoints.
     * @param model 
     * @param connection the connection instance to disconnect (non-null)
     */
    public DBDeleteDiagramConnectionCommand(DBArchimateModel model, IDiagramModelConnection connection) {
        this.fConnection = connection;
        this.fModel = model;
    }
    
    @Override
    public void execute() {
        try {
            IDiagramModel diagramModel = this.fConnection.getDiagramModel();
            if ( diagramModel != null )
                this.fModel.getDBMetadata(diagramModel).setChecksumValid(false);
            this.fModel.getAllViewConnections().remove(this.fConnection.getId());
            this.fConnection.disconnect();
        } catch ( Exception e ) {
            this.exception = new DBException("Failed to delete diagram connection");
            this.exception.initCause(e);
        }
    }
    
    @Override
    public void undo() {
        try {
            this.fConnection.reconnect();
            this.fModel.getAllViewConnections().put(this.fConnection.getId(), this.fConnection);
        } catch (Exception e) {
        	this.exception = new DBException("Failed to restore deleted diagram connection");
            this.exception.initCause(e);
        }
    }


    @Override
    public void dispose() {
        this.fConnection = null;
    }

    @Override
    public DBException getException() {
        return this.exception;
    }
}
