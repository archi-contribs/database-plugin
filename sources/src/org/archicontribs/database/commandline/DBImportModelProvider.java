package org.archicontribs.database.commandline;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGuiImportModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;

/**
 * Command Line interface to import a model from a database
 * 
 * Typical usage:
 *    Archi [-consoleLog] -application com.archimatetool.commandline.app --import.database "database name" --import.model.name "model name" [--import.model.version "model version"]
 * 
 * @author Herve Jouin
 */
public class DBImportModelProvider extends AbstractCommandLineProvider {
	static final String OPTION_IMPORT_DATABASE = "import.database";
	static final String OPTION_IMPORT_MODEL = "import.model";
	static final String OPTION_IMPORT_VERSION = "import.version";
	
	/**
	 * 
	 */
	public DBImportModelProvider() {
		
	}
	
	@Override
    public void run(CommandLine commandLine) throws Exception {		
        if( !commandLine.hasOption(OPTION_IMPORT_DATABASE) ) {
        	// if the option is not present on the CommandLine, we do not have anything to do
            return;
        }
        
        String databaseName = commandLine.getOptionValue(OPTION_IMPORT_DATABASE);
        String modelName = commandLine.getOptionValue(OPTION_IMPORT_MODEL);
        int modelVersion = 0;
        
        // We check the options provided on the CommandLine
        if ( commandLine.hasOption(OPTION_IMPORT_VERSION) ) {
        	try {
	        	modelVersion = Integer.valueOf(commandLine.getOptionValue(OPTION_IMPORT_VERSION));
	        } catch (NumberFormatException ign) {
	        	logError(getLogPrefix()+": parameter "+OPTION_IMPORT_MODEL+" should be a number");
	        	if ( commandLine.hasOption("abortOnException") )
	        		logError(getLogPrefix()+": parameter "+OPTION_IMPORT_MODEL+" defaulting to zero");
	        	else
	        		throw ign;
        	}
        }
    	
        // We activate the import GUI that is mandatory to import a model
        //    and set the database
    	DBGuiImportModel guiImportModel = new DBGuiImportModel("Import model", databaseName);
    	
    	// we check if the database has been found in Archi preferences
    	if ( (guiImportModel.getComboDatabases().getItemCount() == 0) || !(databaseName.equals(guiImportModel.getComboDatabases().getItem(guiImportModel.getComboDatabases().getSelectionIndex()))) )
    		throw new IOException(getLogPrefix()+": Database \""+databaseName+"\" is unknown. You must declare it in the plugin preferences before using the commandline interface.");
    	
    	// we check if the model exists in the database
    	boolean modelFound = false;
    	Table tblModels = guiImportModel.getTblModels();
    	for (int i = 0; i < tblModels.getItemCount(); ++i) {
    		TableItem item = tblModels.getItem(i);
    		if ( DBPlugin.areEqual(item.getText(0), modelName)) {
    			// we select the model
    			tblModels.select(i);
    			// we get the model versions from the database
    			tblModels.notifyListeners(SWT.Selection, new Event());
    			modelFound = true;
    			break;
    		}
    	}
    	if ( !modelFound )
    		throw new IOException(getLogPrefix()+": Model \""+modelName+"\" not found in the database \""+databaseName+"\"");
  	
    	// we check if the model version exists in the database
		Boolean versionFound = false;
    	if ( modelVersion == 0 ) {
    		// if the model does exist, there is a latest version
    		versionFound = true;
    	} else {
    		String modelVersionStr = String.valueOf(modelVersion);

        	Table tblVersions = guiImportModel.getTblModelVersions();
        	for (int i = 0; i < tblVersions.getItemCount(); ++i) {
        		TableItem item = tblVersions.getItem(i);
        		if ( DBPlugin.areEqual(item.getText(0), modelVersionStr)) {
        			// we select the version
        			tblVersions.select(i);
        			versionFound = true;
        			break;
        		}
        	}
    	}
    	
    	if ( !versionFound )
    		throw new IOException(getLogPrefix()+": Version \""+modelVersion+"\" of model \""+modelName+"\" not found in the database \""+databaseName+"\"");
    	
    	// we import the model
    	guiImportModel.doImport();
    	
    	// we set the imported model as the active model
    	CommandLineState.setModel(guiImportModel.getModelToImport());
        
        if ( modelVersion == 0 ) 
        	logMessage(getLogPrefix()+": Latest version of the model \""+modelName+"\" has been imported from the \""+databaseName+"\" database.");
        else
        	logMessage(getLogPrefix()+": Version "+modelVersion+" the model \""+modelName+"\" has been imported from the \""+databaseName+"\" database.");
	}

    @Override
    public Options getOptions() {
        Options options = new Options();
        
        Option option = Option.builder()
                .longOpt(OPTION_IMPORT_DATABASE)
                .hasArg().argName("importDatabase")
                .desc("Name of the database (should already be configured in Archi through the database plugin preferences page)")
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_IMPORT_MODEL)
                .hasArg().argName("importModel")
                .desc("Specifies the name of the model to import")
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_IMPORT_VERSION)
                .hasArg().argName("importVersion")
                .desc("Specifies the version of the model to import (if not specified, the latest version of the model will be imported)")
                .build();
        options.addOption(option);
        
        return options;
    }

    @Override
    public int getPriority() {
        return PRIORITY_IMPORT;
    }
    
    @Override
    protected String getLogPrefix() {
        return "DBPlugin Import CommandLine";
    }
}
