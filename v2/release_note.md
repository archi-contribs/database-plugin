### v2.0.1 :	01/05/2017
* Add the ability to export images of views in the database
* Add a preference to keep the imported model even in case of error
* Add SQL Server integrated security authentication mode
* Reduce memory leak
* Added back Neo4J support (elements and relationships export only)
* Solve NullPointerException while checking database

### v2.0.0 :	28/04/2017
* Export Model :
  * Solve bug where properties were not exported correctly
  * Solve bug where connections could be exported twice
  * Rewrite the conflict detection when exporting to make it more accurate
  * Create the status page on the export model window
  * Add a popup when nothing needs to be exported
* Import Model :
  * Create the status page on the export model window
*  Import individual component :
  * Add the ability to hide existing components in the import component module
  *  Add the ability to hide default views in the import component module
* Get component history :
  *  Solve bug where the component was not found in the database
* Preferences :
  *  Add a preference entry to automatically close import and export windows on success
  *  Add a preference entry to automatically start to export the model to the default database
  *  Add a preference entry to automatically download and install the plugin updates
  *  Add a preference entry to import individual components in shared or copy mode by default
* Miscellaneous :
  * Solve bug in the logger where some multi-lines messages were not printed correctly
  * From now on, folders and views have got their own version number
  * Increase performance by reusing compiled SQL requests
  * Check database version before using it
  * Automatically create database tables if they do not exist
  * Stop replacing the folders type
  * Replaced database table "archi_plugin" by new table "database_version"
  * Opens the model in the tree after import

### v2.0.0.beta4 :	29/03/2017
* Correct import folders properties
* Add version numbers of imported objects in debug mode
* Solve MySQL compatibility issue on elements import SQL request
* Detect folders and views changes using checksum

### v2.0.0.beta3 :	21/03/2017
* Correct preference page where some options are outside the window on some displays
* Solve SQL duplicate key error message on model export
* Solve bug where save button does not show up in preferences page

### v2.0.0.beta2 :	19/03/2017
* Importing an element now imports its relationships as well
* Add import folder functionality
*	Add import view functionality
* Change RCP methods to insert entries in menus in order to be more friendly with other plugins
* Solve a bug with MySQL databases for which aliases in SQL joins are mandatory
* Solve a bug in progressBar which did not represent 100%
* Launch the import process on double-click in the model list table
* The ID is now shown in right menu only in debug mode
*	Few java optimizations
*	Improve exceptions catching between threads
*	Replace boolean database columns by integer columns for better compatibility

### v2.0.0.beta1 :	26/02/2017
From v1 plugin :
* Added log4j support
* Version all the elements and relationships
* Reduce the quantity of data exported by exporting only updated components (use of checksums)
* Detect database conflicts and add a conflict resolution mechanism
* Reduce number of database tables
* Reduce database table names to be compliant will all database brands
* Add back Oracle JDBC driver
* Temporarily remove the Neo4j driver
* Accelerate import and export processes by using multi-threading
* Complete rework of the graphical interface
* Add the ability to import components from other models
* Add inline help