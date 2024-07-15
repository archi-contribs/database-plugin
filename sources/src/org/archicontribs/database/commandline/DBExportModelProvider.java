package org.archicontribs.database.commandline;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.gui.DBGuiExportModel;
import org.archicontribs.database.model.DBArchimateModel;
import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;

/**
 * Command Line interface to export a model to a database
 * 
 * Typical usage:
 *    Archi [-consoleLog] -application com.archimatetool.commandline.app --export.database "database name" [--export.releasenote "release note"]
 * 
 * @author Herve Jouin
 */
public class DBExportModelProvider extends AbstractCommandLineProvider {
	static final String OPTION_EXPORT_DATABASE = "export.database";
	static final String OPTION_EXPORT_RELEASENOTE = "export.releasenote";
	
	/**
	 * 
	 */
	public DBExportModelProvider() {
		// nothing to do
	}
	
	@Override
    public void run(CommandLine commandLine) throws Exception {
		
        if( !commandLine.hasOption(OPTION_EXPORT_DATABASE) ) {
        	// if the option is not present on the CommandLine, we do not have anything to do
            return;
        }
        
        // We check there is an active model
        DBArchimateModel modelToExport = DBArchimateModel.getDBArchimateModel(CommandLineState.getModel());
        
        if(modelToExport == null)
            throw new IOException(getLogPrefix()+": There is no active model. You must load a model before trying to export it in a database.");
        
        String modelName = modelToExport.getName();
        String databaseName = commandLine.getOptionValue(OPTION_EXPORT_DATABASE);
        String releaseNote = commandLine.getOptionValue(OPTION_EXPORT_RELEASENOTE);
        
        // we deactivate the database comparison at GUI startup
        DBPlugin.INSTANCE.getPreferenceStore().setValue("compareBeforeExport", false);
        
        // We activate the export GUI that is mandatory to export a model 
    	DBGuiExportModel guiExportModel = new DBGuiExportModel(modelToExport, "Export model");
    	
    	// We set the database
    	guiExportModel.getDatabases(true, null, databaseName);
    	
    	// we check if the database has been declared in Archi
    	if ( (guiExportModel.getComboDatabases().getItemCount() == 0) || !(databaseName.equals(guiExportModel.getComboDatabases().getItem(guiExportModel.getComboDatabases().getSelectionIndex()))) )
    		throw new IOException(getLogPrefix()+": Database \""+databaseName+"\" is unknown. You must declare it in the plugin preferences before using the commandline interface.");
    	
    	// we count the model's components
    	guiExportModel.getExportedModel().countAllObjects();
    	
    	// we set the release note if provided
    	if ( releaseNote != null )
    		guiExportModel.getTxtReleaseNote().setText(releaseNote);
    	
    	// we export the model
    	switch ( guiExportModel.export() ) {
	    	case 1 : logMessage(getLogPrefix()+": The model \""+modelName+"\" has been exported to the \""+databaseName+"\" database."); break;
	    	case 0 : logMessage(getLogPrefix()+": The model \""+modelName+"\" does not need to be exported to the \""+databaseName+"\" database."); break;
	    	default : logError(getLogPrefix()+": Failed to export the model \""+modelName+"\" to the \""+databaseName+"\" database.");
    	}
	}
	
    @Override
    public Options getOptions() {
        Options options = new Options();
        
        Option option = Option.builder()
                .longOpt(OPTION_EXPORT_DATABASE)
                .hasArg().argName("exportDatabase")
                .desc("Name of the database (should already be configured in Archi through the database plugin preferences page)")
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_EXPORT_RELEASENOTE)
                .hasArg().argName("exportReleaseNote")
                .desc("Specifies the release note of the exported model")
                .build();
        options.addOption(option);
        
        return options;
    }
	
    @Override
    public int getPriority() {
        return PRIORITY_REPORT_OR_EXPORT;
    }
    
    @Override
    protected String getLogPrefix() {
        return "DBPlugin Export CommandLine";
    }
}
