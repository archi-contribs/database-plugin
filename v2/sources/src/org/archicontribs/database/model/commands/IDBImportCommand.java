package org.archicontribs.database.model.commands;

import org.eclipse.emf.ecore.EObject;

public interface IDBImportCommand extends IDBCommand {
    public EObject getImported();
}
