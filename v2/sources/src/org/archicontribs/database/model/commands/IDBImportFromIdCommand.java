package org.archicontribs.database.model.commands;

import org.eclipse.emf.ecore.EObject;

public interface IDBImportFromIdCommand {
    public boolean canExecute();
    public void execute();
    
    public boolean canUndo();
    public void undo();
    
    public boolean canRedo();
    public void redo();
    
    public EObject getImported();
    
    public Exception getException();
    
    public void dispose();
}
