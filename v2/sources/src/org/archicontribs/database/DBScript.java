package org.archicontribs.database;

import java.util.List;

import org.archicontribs.database.model.ArchimateModel;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.CommandStack;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;

public class DBScript {
    private static final DBLogger logger = new DBLogger(DBDatabaseConnection.class);

    public IArchimateModel importModel(String modelName, String databaseName, boolean force) throws Exception {

        // we get the databases list from the preferences
        List<DBDatabaseEntry> databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore(true);
        if ( (databaseEntries == null) || (databaseEntries.size() == 0) )
            throw new RuntimeException("Cannot find any database.");
          
        // we get the database by its name
        DBDatabaseEntry databaseEntry = null;
        for ( DBDatabaseEntry entry: databaseEntries ) {
            if ( entry.getName().equals(databaseName) ) {
                databaseEntry = entry;
                break;
            }
            
        }
        
        // we check that the database exists
        if ( databaseEntry == null )
            throw new RuntimeException("Cannot find database \""+databaseName+"\"");
        
        // we connect to the database
        DBDatabaseConnection connection = new DBDatabaseConnection(databaseEntry);
        
        // we check if we are connected to the database
        if ( connection == null || !connection.isConnected() )
            throw new RuntimeException("Cannot connect to the database \""+databaseName+"\"");
        
        // we check if the model exists in the database
        String modelId = connection.getModelId(modelName, false);
        
        if ( modelId == null )
            throw new RuntimeException("Cannot find model \""+ modelName +"\" in the database \""+databaseName+"\"");
        
        // we check that the model is not already in memory
        List<IArchimateModel> allModels = IEditorModelManager.INSTANCE.getModels();
        for ( IArchimateModel existingModel: allModels ) {
            if ( DBPlugin.areEqual(modelId, existingModel.getId()) )
                throw new RuntimeException("A model with ID \""+modelId+"\" is already opened.");
            if ( DBPlugin.areEqual(modelName, existingModel.getName()) && !force )
                throw new RuntimeException("A model with name \""+modelName+"\" is already opened.");
        }

        // at last, we import the model
        ArchimateModel modelToImport = (ArchimateModel)IArchimateFactory.eINSTANCE.createArchimateModel();
        modelToImport.setId(modelId);
        modelToImport.setName(modelName);
        
        try {
            connection.importModelMetadata(modelToImport);
        
            if ( logger.isDebugEnabled() ) logger.debug("Importing the folders ...");
            connection.prepareImportFolders(modelToImport);
            while ( connection.importFolders(modelToImport) );
            
            if ( logger.isDebugEnabled() ) logger.debug("Importing the elements ...");
            connection.prepareImportElements(modelToImport);
            while ( connection.importElements(modelToImport) );
            
            if ( logger.isDebugEnabled() ) logger.debug("Importing the relationships ...");
            connection.prepareImportRelationships(modelToImport);
            while ( connection.importRelationships(modelToImport) );
            
            if ( logger.isDebugEnabled() ) logger.debug("Resolving relationships' sources and targets ...");
            modelToImport.resolveRelationshipsSourcesAndTargets();
            
            if ( logger.isDebugEnabled() ) logger.debug("Importing the views ...");
            connection.prepareImportViews(modelToImport);
            while ( connection.importViews(modelToImport) );
            
            if ( logger.isDebugEnabled() ) logger.debug("Importing the views objects ...");
            for (IDiagramModel view: modelToImport.getAllViews().values()) {
                connection.prepareImportViewsObjects(view.getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion());
                while ( connection.importViewsObjects(modelToImport, view) );
            }
            
            if ( logger.isDebugEnabled() ) logger.debug("Importing the views connections ...");
            for (IDiagramModel view: modelToImport.getAllViews().values()) {
                connection.prepareImportViewsConnections(view.getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion());
                while ( connection.importViewsConnections(modelToImport) );
            }
            
            if ( logger.isDebugEnabled() ) logger.debug("Resolving connections' sources and targets ...");
            modelToImport.resolveConnectionsSourcesAndTargets();
    
            if ( logger.isDebugEnabled() ) logger.debug("importing the images ...");
            for (String path: connection.getAllImagePaths())
                connection.importImage(modelToImport, path);
            
            if ( logger.isDebugEnabled() ) logger.debug("Importing the views connections ...");
            for (IDiagramModel view: modelToImport.getAllViews().values()) {
                connection.prepareImportViewsConnections(view.getId(), ((IDBMetadata)view).getDBMetadata().getCurrentVersion());
                while ( connection.importViewsConnections(modelToImport) );
            }
        } catch ( Exception e) {
            // in case of an import error, we remove the newly created model, except if we are in force mode
            if ( !force ) {
                // we remove the 'dirty' flag (i.e. we consider the model as saved) because we do not want the closeModel() method ask to save it
                CommandStack stack = (CommandStack)modelToImport.getAdapter(CommandStack.class);
                if ( stack != null ) stack.markSaveLocation();
    
                IEditorModelManager.INSTANCE.closeModel(modelToImport);
            }
            throw e;
        }
        // we add the new model in the manager
        IEditorModelManager.INSTANCE.registerModel(modelToImport);
        
        return modelToImport;
    }
}
