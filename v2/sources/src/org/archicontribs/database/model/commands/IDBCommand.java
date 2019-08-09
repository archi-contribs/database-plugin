package org.archicontribs.database.model.commands;

public interface IDBCommand {
    public boolean canExecute();
    public void execute();
    
    public boolean canUndo();
    public void undo();
    
    public boolean canRedo();
    public void redo();
    
    public Exception getException();
    
    public void dispose();
}
