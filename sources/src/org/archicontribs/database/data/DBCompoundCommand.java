package org.archicontribs.database.data;

import org.archicontribs.database.model.commands.IDBCommand;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

public class DBCompoundCommand extends CompoundCommand {
	/**
	 * Constructs an empty CompoundCommand
	 */
	public DBCompoundCommand() {
		super();
	}
	
	/**
	 * Constructs an empty CompoundCommand with the specified label.
	 * 
	 * @param label
	 *            the label for the Command
	 */
	public DBCompoundCommand(String label) {
		super(label);
	}

	/**
	 * Adds the specified command if it is not <code>null</code>.
	 * 
	 * @param command
	 *            <code>null</code> or a IDBCommand
	 * @throws Exception
	 *            if any exception is raised during the initialization of the command
	 */
	public void checkAndAdd(IDBCommand command) throws Exception {
		if (command != null) {
			Exception exception = command.getException();
			if ( exception != null )
				throw exception;
			
			super.add((Command)command);
		}
	}
	
	/**
	 * Execute the specified command and adds it if it is not <code>null</code>.
	 * 
	 * @param command
	 *            <code>null</code> or a IDBCommand
	 * @throws Exception 
	 *            if any exception is raised during the initialization of the command
	 */
	public void checkAndExecute(IDBCommand command) throws Exception {
		if (command != null) {
			Exception exception = command.getException();
			if ( exception != null )
				throw exception;
			
			command.execute();
			exception = command.getException();
			if ( exception != null)
				throw exception;
			
			super.add((Command)command);
		}
	}
}
