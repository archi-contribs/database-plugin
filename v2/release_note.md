### v2.0.6 : 30/05/2017
* Import model:
  * Solve bug when importing a model which has got a shared view with elements or relationships added by other models
  * The import SQL request have been rewritten because of Oracle specificity
  * A double click on a model's version now launches the import
* Import individual components:
  * Solve bug where all the views versions were added in the table, resulting in several entries with the same name
* Database model:
  * Added column "element_version" to table "views_objects"
  * Added column "relationship_version" to table "views_connections"
                                  
### Known bugs
* Import individual component:
  * images are not imported
  * view references are not imported correctly
  * importing elements "in a view" create all the corresponding objects in the top left corner of the view
  * clicking on the "cancel" button during the export or the import of a model is not well managed

### TODO list
* Import individual component:
  * allow to import elements recursively
  * allow to select all the classes of one group in a single click
  * when the user right clicks on a folder, automatically select the class corresponding to the folder (views, components, ...)
* Export model:
  * show up all the component properties in the conflict resolution table
* Get component history:
  * allow to export individual component, or update it from the database, directly from the history window
  * allow to get the database history
* Miscellaneous:
  * add a preference to show or hide the debug information on the right click menu rather than depend on the logging level
  * add an option to check for relationships that are in the database but would not be in the in memory model
  * add a progressbar on the "please wait while checking components to export" window
  * find a way to manage images from the database the same way it is done on disk
  * create a new windows that will show up detailed statistics about the model
  * add more jdbc drivers (mongodb, odbc, etc ...)

----------

### v2.0.5 : 17/05/2017
* Export model:
  * Change order of folders export (exporting all the root folders first)
* Import model:
  * Solve bug in counting components to import prior the import itself which can cause false error messages even when the import is successful
  * Solve bug in folder import where the parent folder was not created yet before its content

### v2.0.4 : 11/05/2017
* Export model:
  * Solve bug where export conflicts were not detected correctly on MySQL databases
* Import individual component:
  * The import type (shared or copy) can now be changed directly on the import window
* Preference page:
  * Correct traversal order of fields on preference page
  * The default database port is automatically filled-in when port field is empty or equals 0
  * The default for new databases is to not export view images
  * When saving preferences while editing database properties, the plugin now asks if the updates need to be saved or discarded
* Miscellaneous:
  * Rewrite of the checksum calculation procedure to ensure it is always the same length
  *	Rewrite of the views connections import procedure to ensure that they are imported in the same order as in the original model
  *	This leads to 2 new columns (source_connections and target_connections) in the view_objects and views_connections database tables

### v2.0.3 : 07/05/2017
* Export model :
  * Make conflict management more reliable on PostGreSQL databases
  * Added a preference to remove the dirty flag on the model after a successful export
  * Solve bug where count of exported components could be erroneous
* Import individual component :
  *	Added missing "location" in individual component import window
  * Add the ability to import several individual components at the same time
  * The component list in the individual component import window are now sorted alphabetically
  *	Solve bug where the same component could be imported several times
* Miscellanous :
  * Allow to specify a database schema in the database configuration
  * It is now possible to check a database connection without the need to edit their details
  * Reduce memory consumption
  * Remove the NOT NULL constraints on some columns because Oracle does not do any difference between an empty string and a null value
  * Renamed mssql driver to ms-sql to be visually more distinctive from mysql

### v2.0.2 :	02/05/2017
* Solve errors during table creation in PostGreSQL database
* Solve "Operation not allowed after ResultSet closed" error message on model export
* Add a menu entry to replace old fashion IDs to Archi 4 IDs (to ensure uniqueness of all components)

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