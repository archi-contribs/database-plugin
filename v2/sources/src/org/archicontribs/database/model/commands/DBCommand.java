package org.archicontribs.database.model.commands;

/**
 * Command allowing to execute and undo actions<br>
 * <br>
 * Differs from the GEF Command:<br>
 *    - the execute() method returns a status (0 -> success, != 0 -> failure)
 *    - the command maintains an execution state to avoid to run it twice
 * 
 * @author Herve Jouin
 */
public class DBCommand extends org.eclipse.gef.commands.Command {

    public DBCommand() {
        
    }
}
