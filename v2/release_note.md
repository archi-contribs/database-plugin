### v2.0.0.beta5 :	11/04/2017
* Add auto update functionality
* Solve bug in export model properties
* Rewrite the conflict detection when exporting to make it more accurate
* Fill in the status page on exports and imports
* Solve bug where connections could be exported twice
* Folders and views have now got their own version number 

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
