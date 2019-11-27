package org.archicontribs.database;

import java.util.List;

import org.archicontribs.database.connection.DBDatabaseConnection;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.gef.commands.CommandStack;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;

/**
 * This class is an API for the script plugin 
 * 
 * @author Herve Jouin
 */
public class DBScript {
    private static final DBLogger logger = new DBLogger(DBDatabaseConnection.class);

    /**
     * Imports the latest version of a model
     * @param modelName name of the model top import
     * @param databaseName name of the database to import from, as stated in the list of databases on the plugin's preference page
     * @param force if true, allow to have another model with the same name, else, the model's name must be unique in Archi
     * @return the model
     * @throws Exception if the model cannot be imported
     */
    public static IArchimateModel importModel(String modelName, String databaseName, boolean force) throws Exception {

        // we get the databases list from the preferences
        List<DBDatabaseEntry> databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore();
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
        try ( DBDatabaseImportConnection connection = new DBDatabaseImportConnection(databaseEntry) ) {
	        // we check if we are connected to the database
	        if ( !connection.isConnected() )
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
	
	        // we create the model
	        DBArchimateModel modelToImport = (DBArchimateModel)DBArchimateFactory.eINSTANCE.createArchimateModel();
	        modelToImport.setId(modelId);
	        modelToImport.setName(modelName);
	        
	        // awe create the model's archive manager
	        modelToImport.setAdapter(IArchiveManager.class, IArchiveManager.FACTORY.createArchiveManager(modelToImport));
	        
	        try {
	        	// at last, we import the model
	            connection.importModel(modelToImport);
	        
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the folders ...");
	            connection.prepareImportFolders(modelToImport);
	            while ( connection.importFolders(modelToImport) ) {
	                // each loop imports a folder
	            }
	            
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the elements ...");
	            connection.prepareImportElements(modelToImport);
	            while ( connection.importElements(modelToImport) ) {
	                // each loop imports an element
	            }
	            
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the relationships ...");
	            connection.prepareImportRelationships(modelToImport);
	            while ( connection.importRelationships(modelToImport) ) {
	                // each loop imports a relationship
	            }
	            
	            modelToImport.resolveSourceAndTargetRelationships();
	            
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the views ...");
	            connection.prepareImportViews(modelToImport);
	            while ( connection.importViews(modelToImport) ) {
	                // each loop imports a view
	            }
	            
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the views objects ...");
	            for (IDiagramModel view: modelToImport.getAllViews().values()) {
	                connection.prepareImportViewsObjects(view.getId(), DBMetadata.getDBMetadata(view).getInitialVersion().getVersion());
	                while ( connection.importViewsObjects(modelToImport, view) ) {
	                    // each loop imports a view object
	                }
	            }
	            
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the views connections ...");
	            for (IDiagramModel view: modelToImport.getAllViews().values()) {
	                connection.prepareImportViewsConnections(view.getId(), DBMetadata.getDBMetadata(view).getInitialVersion().getVersion());
	                while ( connection.importViewsConnections(modelToImport) ) {
	                    // each loop imports a view connection
	                }
	            }
	            
	            modelToImport.resolveSourceAndTargetConnections();
	    
	            if ( logger.isDebugEnabled() ) logger.debug("Importing the images ...");
	            for (String path: connection.getAllImagePaths())
	                connection.importImage(modelToImport, path);
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
}
