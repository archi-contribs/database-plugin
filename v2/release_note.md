v2.2.7: 05/05/2020
* Fix password storage in preferences when expert mode is activated
* Remove unnecessary password deciphering  

v2.2.6: 03/05/2020
* Fix import of connections bendpoints when importing a view from another model
* Fix automatic update from GitHub
* Fix plugin version check at startup 

v2.2.5: 17/04/2020
This release is an update of the database drivers
* Neo4J --> 4.0.0
* Oracle --> 10
* PostGreSQL --> 42.2.12
* SQLite --> 3.30.1
* MS SQL Server --> 8.2.2
* MySQL --> stays in version 5.1.48 because of timezone error (https://bugs.mysql.com/bug.php?id=90813)

v2.2.4: 14/01/2020
* Fix the version numbershown in the plugin windows
* Fix username and password load from preference page for databases in expert mode

v2.2.3: 11/12/2019
* Reduce the number of SQL request
* Fix the get history from database
* Fix the Neo4J JDBC connection string
* Fix directed view connections export

v2.2.2: 02/12/2019
* Rewrite code to remove a lot of class extends
* Update database structure to be compliant with Archi 4.6
* GUI improvement
  * Add last export date to the models table on the import window
  * Add a progress bar during a database comparison
* Performance improvement
  * Do not try to import properties, features and bendpoints if not necessary
  * Drastically increase the performance when comparing a model against a database
* Security improvement
  * The database password is not printed in clear text in preference window by default
  * The database password is not stored in clear text anymore in the preference store, even if the algorithm used is must be reversible

v2.2.1: 28/10/2019
* Fix plugin version in windows title
* Fix count of model's components during export
* Fix unnecessary double export of view components 
* Do not compare twice the model to the database if there have been no import
* Create first admin procedures
  * check database structure
  * check database content
* Update the JDBC drivers
  * MySQL		--> rollback to 5.1.48 because of timezone error (https://bugs.mysql.com/bug.php?id=90813)

v2.2.0: 12/10/2019
* Rewrite of the export process
* Rewrite of the conflict management process
* Remove the "Relationship" suffix on the relationships names during Neo4J exports
* Fix number of model components during export when components are updated or deleted from the database
* Fix the export of an existing database model when opened from archimate file
* Remember the import database and set it as the default export database
* Ask the database password during connection if not provided in the preferences
* fix tablename case for SQL databases
* Add welcome message
* Update the JDBC drivers
  * MySQL		--> 8.0.17
  * Neo4J		--> 3.4.0
  * PostGreSQL	--> 42.2.6
  * SQLite		--> 3.27.2.1
  
v2.1.11: 26/04/2019
* Fix version comparison when a part of it is greater or equal to 10
* Fix issues on SQL requests introduced in plugin version 2.1.10

v2.1.10: 23/04/2019
* Fix unclosed cursors on Oracle databases

v2.1.9: 27/02/2018
* Add expert mode where the jdbc connection string can be manually edited

v2.1.8: 12/02/2018
* Fix import of images when initiated by script plugin

v2.1.7: 23/11/2018
* Import components from database:
  * updating a view from the database now updates the elements and relationships referenced in the view
* Fixes:
  * Fix version calculation during export
  * Fix SQL requests when the database plugin is called by the script plugin
  * Fix label position in debug window

v2.1.6: 13/11/2018
* Fix savepoint name expected error during rollback
* Fix import of recursive referenced views

v2.1.5: 10/11/2018
* Add key bindings to export model and import model commands
* Fix screen scale calculation divide by zero exception on some environments
* Fix model's checksum is reset during export
* Check if auto commit mode before rollbacking transaction

v2.1.4: 20/10/2018
* Fix version number

v2.1.3: 30/09/2018
* Fix Oracle objects names to be less than 30 characters long
* Fix Oracle error ORA-00932 when using CLOB in joined requests
* Update Oracle driver to 18c
* Remove the hint_title and hint_content columns from views and views_objects tables as they do not need to be exported

v2.1.2: 30/09/2018
* Revert "merge" instead of "create" for Neo4j databases

v2.1.1: 27/09/2018
* Fix import order from PostGreSQL databases

v2.1: 25/09/2018
* Import components from database:
  * Rename "import individual component" to "import components"
  * Added documentation column to help distinguish components having the same name
  * Added tooltip with properties to help distinguish components having the same name
  * Added message during the import as it may take some time
  * Use commands to allow undo/redo
  * Added a label to explain that the icons can be selected
  * The categories can now be clicked to select/unselect the whole category
  * The component is imported by default in the selected folder
  * The element classes are pre-selected depending on the selected folder
  * In case one view is selected for import, show view screenshot if available in the database
  * Introduce new template mode that mixes shared and copy modes depending on on the "template" property of each component
  * Possibility to import a whole model into another one (merge models)
* Import model:
  * A context menu entry allowing to import a model has been added when no model is selected
  * Automatically open the default view (if any) at the end of the import
  * Fix number of images to import
* Export model:
  * For relational databases:
     * The export is now in "collaborative mode", which syncs the model with the database:
       * It can be compared to a pull+push to GitHub.
       * It is slower than the previous mode but allows several people to work on the same model at the same time
     * Allow to specify border width and scale factor for views screenshots
     * To simplify, it is no more possible to choose between whole export or elements and relationships only
   * For Neo4J databases:
     * Create two export modes: native and extended
     * New option to empty the database before the export
     * New option to specialize relationships
  * Rewrite version management
  * Remove the name, the documentation and the properties from view objects and connections checksum calculation as they are related to the corresponding concept
* Plugin preferences:
  * Add an option to specify the default import mode for individual components (template mode, force shared mode or force copy mode)
  * Add an option to compare the model from the database before exporting it
  * Allow to specify the generated view screenshots border width and scale factor
  * Allow to specify a suffix to add to components imported in copy mode
  * Add an option to check for max memory at startup (xmx should be set to 1g)
  * Add an option to show or hide zero values in import and export windows
* Get history from database:
  * Allows to get history for diagrams, canvas and sketches
  * Allows to export/import component to/from the database directly from the history window
* Other:
  * Bug fixes:
    * Exporting blocks or images objects with no images set does not generate errors anymore
    * Fix plugin initialization failure that occurred some times
    * Fix progress bar during download new version of the plugin from GitHub
    * Increase compiler severity to maximum and resolve all the warnings to improve code resiliency
    * Reduce memory leak
    * Fix centering of GUI windows, especially on hiDPI displays
    * Fix calculation of numbers of images to import
    * Better management of the cancel button during the import and export process
    * Cleanup properties before import rather than update existing values as several properties can have the same name
    * Fix display on HiDPI displays
  * Improvements:
    * Fill in the online help pages
    * Rewrite debug and trace messages to be more useful
    * Add the ability to import an image from the database (on the Image and Block objects in Canvas)
    * Some annoying popups have been replaced by messages directly in the import/export window
    * Remove the name, the documentation and the properties from view objects and connections checksum as they are not related to the view objects and connections themselves, but to the related element or relationship
    * Add procedures that can be called by the script plugin
    * The inline help can be accessed using the interrogation mark on every plugin window.
    * Export and import back the model's "metadata" (may be used by other external tools)
    * Do not calculate checksum on images anymore as the path is already a kind of checksum
    * A new "show debug information" window has been created
    * Add "import model from database" menu entry on right click when no model has been loaded yet
    * Manage the objects transparency (introduced in Archi 4.3)
    * Check for max memory available at startup and suggest to increase it (xmx parameter) if less than 1 GB
    * Add the ability to import an image from the database on Canvas Image and Block objects
  * Update JDBC drivers
    * Neo4J to 3.1.0
    * SQLite to 3.21.0
    * PostGreSQL to 42.2.1
	
### v2.0.7b: 01/07/2017
* Fix Neo4J errors

### v2.0.7: 30/06/2017
* Rollback to single thread as multi-threading causes to many side effects and does not accelerate the import and export duration as expected
* Improve checksum mechanism
* Add an option to show up ID and checksum in context menu rather than relying on the logger mode
* Import model:
  * Solve bug where the filter field was not working as expected
  * Change the filter request to be case insensitive
* Export model:
  * Use of a Tree rather than a Table to show up conflicts
  * Show up more information about conflicting components
  * Improve the conflict detection and resolution

### v2.0.6: 30/05/2017
* Import model:
  * Solve bug when importing a model which has got a shared view with elements or relationships added by other models
  * The import SQL request have been rewritten because of Oracle specificity
  * A double click on a model's version now launches the import
* Import individual components:
  * Solve bug where all the views versions were added in the table, resulting in several entries with the same name
* Database model:
  * Added column "element_version" to table "views_objects"
  * Added column "relationship_version" to table "views_connections"

### v2.0.5: 17/05/2017
* Export model:
  * Change order of folders export (exporting all the root folders first)
* Import model:
  * Solve bug in counting components to import prior the import itself which can cause false error messages even when the import is successful
  * Solve bug in folder import where the parent folder was not created yet before its content

### v2.0.4: 11/05/2017
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
  *    Rewrite of the views connections import procedure to ensure that they are imported in the same order as in the original model
  *    This leads to 2 new columns (source_connections and target_connections) in the view_objects and views_connections database tables

### v2.0.3: 07/05/2017
* Export model:
  * Make conflict management more reliable on PostGreSQL databases
  * Added a preference to remove the dirty flag on the model after a successful export
  * Solve bug where count of exported components could be erroneous
* Import individual component:
  *    Added missing "location" in individual component import window
  * Add the ability to import several individual components at the same time
  * The component list in the individual component import window are now sorted alphabetically
  *    Solve bug where the same component could be imported several times
* Miscellanous:
  * Allow to specify a database schema in the database configuration
  * It is now possible to check a database connection without the need to edit their details
  * Reduce memory consumption
  * Remove the NOT NULL constraints on some columns because Oracle does not do any difference between an empty string and a null value
  * Renamed mssql driver to ms-sql to be visually more distinctive from mysql

### v2.0.2:    02/05/2017
* Solve errors during table creation in PostGreSQL database
* Solve "Operation not allowed after ResultSet closed" error message on model export
* Add a menu entry to replace old fashion IDs to Archi 4 IDs (to ensure uniqueness of all components)

### v2.0.1:    01/05/2017
* Add the ability to export images of views in the database
* Add a preference to keep the imported model even in case of error
* Add SQL Server integrated security authentication mode
* Reduce memory leak
* Added back Neo4J support (elements and relationships export only)
* Solve NullPointerException while checking database

### v2.0.0:    28/04/2017
* Export Model:
  * Solve bug where properties were not exported correctly
  * Solve bug where connections could be exported twice
  * Rewrite the conflict detection when exporting to make it more accurate
  * Create the status page on the export model window
  * Add a popup when nothing needs to be exported
* Import Model:
  * Create the status page on the export model window
*  Import individual component:
  * Add the ability to hide existing components in the import component module
  *  Add the ability to hide default views in the import component module
* Get component history:
  *  Solve bug where the component was not found in the database
* Preferences:
  *  Add a preference entry to automatically close import and export windows on success
  *  Add a preference entry to automatically start to export the model to the default database
  *  Add a preference entry to automatically download and install the plugin updates
  *  Add a preference entry to import individual components in shared or copy mode by default
* Miscellaneous:
  * Solve bug in the logger where some multi-lines messages were not printed correctly
  * From now on, folders and views have got their own version number
  * Increase performance by reusing compiled SQL requests
  * Check database version before using it
  * Automatically create database tables if they do not exist
  * Stop replacing the folders type
  * Replaced database table "archi_plugin" by new table "database_version"
  * Opens the model in the tree after import

### v2.0.0.beta4:    29/03/2017
* Correct import folders properties
* Add version numbers of imported objects in debug mode
* Solve MySQL compatibility issue on elements import SQL request
* Detect folders and views changes using checksum

### v2.0.0.beta3:    21/03/2017
* Correct preference page where some options are outside the window on some displays
* Solve SQL duplicate key error message on model export
* Solve bug where save button does not show up in preferences page

### v2.0.0.beta2:    19/03/2017
* Importing an element now imports its relationships as well
* Add import folder functionality
*    Add import view functionality
* Change RCP methods to insert entries in menus in order to be more friendly with other plugins
* Solve a bug with MySQL databases for which aliases in SQL joins are mandatory
* Solve a bug in progressBar which did not represent 100%
* Launch the import process on double-click in the model list table
* The ID is now shown in right menu only in debug mode
*    Few java optimizations
*    Improve exceptions catching between threads
*    Replace boolean database columns by integer columns for better compatibility

### v2.0.0.beta1:    26/02/2017
From v1 plugin:
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