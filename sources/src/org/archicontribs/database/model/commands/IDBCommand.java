package org.archicontribs.database.model.commands;

import org.archicontribs.database.DBException;

public interface IDBCommand {
    public boolean canExecute();
    public void execute();
    
    public boolean canUndo();
    public void undo();
    
    public boolean canRedo();
    public void redo();
    
    public DBException getException();
    
    public void dispose();
}
