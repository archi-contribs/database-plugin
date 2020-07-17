package org.archicontribs.database.commandline;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGuiAdminDatabase;
import org.archicontribs.database.GUI.DBGuiImportModel;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.archimatetool.commandline.AbstractCommandLineProvider;

/**
 * Command Line interface for importing a model from a database
 * 
 * Typical usage:
 *    Archi [-consoleLog] -application com.archimatetool.commandline.app --import.database "database name" --import.model.name "model name" [--import.model.version "model version"]
 * 
 * @author Herve Jouin
 */
public class DBImportModelProvider extends AbstractCommandLineProvider {
	protected static final DBLogger logger = new DBLogger(DBGuiAdminDatabase.class);
	
	static final String PREFIX = "DBPlugin";
	static final String OPTION_IMPORT_DATABASE = "import.database";
	static final String OPTION_IMPORT_MODEL = "import.model.name";
	static final String OPTION_IMPORT_VERSION = "import.model.version";
	
	/**
	 * 
	 */
	public DBImportModelProvider() {
		
	}
	
	@Override
    public void run(CommandLine commandLine) throws Exception {
        if(!hasCorrectOptions(commandLine)) {
        	logError("Bad options provided.");
            return;
        }
        
        String databaseName = commandLine.getOptionValue(OPTION_IMPORT_DATABASE);
        String modelName = commandLine.getOptionValue(OPTION_IMPORT_MODEL);
        int modelVersion = 0;
        
        try {
        	modelVersion = Integer.valueOf(commandLine.getOptionValue(OPTION_IMPORT_VERSION));
        } catch (@SuppressWarnings("unused") NumberFormatException ign) {
        	if ( commandLine.hasOption("abortOnException") ) {
        		logError("Option "+OPTION_IMPORT_MODEL+" should be a number.");
        		return;
        	}
        	logMessage("Option "+OPTION_IMPORT_MODEL+" should be a number. Defaulting to zero (latest version of the model will be imported)");
        }
        
    	if ( commandLine.hasOption(OPTION_IMPORT_VERSION) )
    		logMessage("*** Importing version "+modelVersion+" of model \""+modelName+"\" from the database \""+databaseName+"\"");
    	else
    		logMessage("*** Importing latest version "+modelVersion+" of model \""+modelName+"\" from the database \""+databaseName+"\"");
    	
    	
    	logMessage("Creating DBGuiImportModel window ...");
    	DBGuiImportModel guiImportModel = new DBGuiImportModel("Import model", databaseName);
    	
    	logMessage("Checking if database \""+databaseName+"\" is selected");
    	int idx = guiImportModel.getComboDatabases().getSelectionIndex();
    	if ( idx == -1 ) {
    		guiImportModel.close();
    		logError("Unknown database \""+databaseName+"\"");
    		return;
    	}
    	
    	String databaseSelected = guiImportModel.getComboDatabases().getItem(idx);
    	if ( !DBPlugin.areEqual(databaseSelected, databaseName) ) {
    		guiImportModel.close();
    		logError("Database \""+databaseSelected+"\" is selected instead of \""+databaseName+"\"");
    		return;
    	}
    	
    	logMessage("Selecting model \""+modelName+"\"");
    	boolean modelSelected = false;
    	Table tblModels = guiImportModel.getTblModels();
    	for (int i = 0; i < tblModels.getItemCount(); ++i) {
    		TableItem item = tblModels.getItem(i);
    		if ( DBPlugin.areEqual(item.getText(0), modelName)) {
    			tblModels.select(i);
    			modelSelected = true;
    			break;
    		}
    	}
    	if ( !modelSelected ) {
    		guiImportModel.close();
    		logError("Model \""+modelName+"\" not found in the database.");
    		return;
    	}
  	
    	if ( modelVersion != 0 ) {
    		logMessage("Selecting model version \""+modelVersion+"\"");
    		String modelVersionStr = String.valueOf(modelVersion);
    		Boolean versionSelected = false;
        	Table tblVersions = guiImportModel.getTblModelVersions();
        	for (int i = 0; i < tblVersions.getItemCount(); ++i) {
        		TableItem item = tblVersions.getItem(i);
        		if ( DBPlugin.areEqual(item.getText(0), modelVersionStr)) {
        			tblVersions.select(i);
        			versionSelected = true;
        			break;
        		}
        	}
        	if ( !versionSelected ) {
        		guiImportModel.close();
        		logError("Model version\""+modelVersion+"\" not found in the database.");
        		return;
        	}
    	}
    	
    	logMessage("Importing model");
    	guiImportModel.doImport();

        logMessage("Closing GUI");
        guiImportModel.close();
        
        if ( modelVersion != 0 ) 
        	logMessage("*** Model \""+modelName+"\" imported.");
        else
        	logMessage("*** Model \""+modelName+"\" version \""+modelVersion+"\" imported.");
	}
	
    @Override
    public Options getOptions() {
        Options options = new Options();
        
        Option option = Option.builder()
                .longOpt(OPTION_IMPORT_DATABASE)
                .hasArg().argName("database")
                .desc("Name of the database (should already be configured in Archi through the database plugin preferences page)")
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_IMPORT_MODEL)
                .hasArg().argName("modelName")
                .desc("Specifies the name of the model to import")
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_IMPORT_VERSION)
                .hasArg().argName("modelVersion")
                .desc("Specifies the version of the model to import (if not specified, the latest version of the model will be imported)")
                .build();
        options.addOption(option);
        
        return options;
    }
	
    @SuppressWarnings("static-method")
	private boolean hasCorrectOptions(CommandLine commandLine) {
        return commandLine.hasOption(OPTION_IMPORT_DATABASE) && !DBPlugin.isEmpty(commandLine.getOptionValue(OPTION_IMPORT_DATABASE))
        		&& commandLine.hasOption(OPTION_IMPORT_MODEL) && !DBPlugin.isEmpty(commandLine.getOptionValue(OPTION_IMPORT_MODEL));
    }
    
    @Override
    public int getPriority() {
        return PRIORITY_IMPORT;
    }
    
    @Override
    protected String getLogPrefix() {
        return PREFIX;
    }
}
