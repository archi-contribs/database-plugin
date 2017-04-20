### v1.0.1 (09/01/2017)
* correct bug preventing to export a canvas embeded in a sketch or another canvas

### v1.0 (09/12/2016)
* remove the support of Oracle databases that does not work
* Refuse to export a shared model when elements exist outside its model's subfolder

### v0.13.0 (23/11/2016)
* add Microsoft SQL Server support

### v0.12.2 (21/11/2016)
* fix folder count error when exporting project in shared mode
* correct bug causing release note being not saved in the database

### v0.12b (16/11/2016)
* solve library dependencies (Archi 4 plugin)
* correct an import request for Neo4j databases

### v0.12 (24/10/2016)
* Add column "diagrammodelid" to table "diagrammodelreference"
* Complete rewriting of the source and target connections import procedure
* Rewrite of the NetO4 relationships export request to allow self relationships
* Add missing column names in Neo4j import requests
* The model.xml inside the .archimate file is now identical as the original after the model has been exportted and imported back (even if not in the same order)

### v0.11 (13/10/2016)
* Add the capability to select several models on the import window
* Create 2 releases : one for Archi 3, one for Archi 4
* Change JDBC mode to auto-commit to avoid reset each time a select fails
* Correct a bug in the Neo4j model import (properties are case sensitive)
* Correct a bug in the Sketch import
* Add colored icons on tabItem titles to summarize their status
* Add table "archidatabaseplugin" to distinguish databases configured with Archi 3 or Archi 4 datamodels
* Use of CLOB for key text fields (name, description, ...)
* Use of longer field for ID
* Include model and version to referenced Ids to prepare inter-models relationships

### v0.10b (28/09/2016)
* Change the separator between the Id, the model Id and the version as the "-" could be found in Archi's IDs
* Correct a bug where the projects selected were not imported
* Correct a bug in the delete model procedure
* Check if a project with same ID or same name already exists before importing it
* Add a test to verify that we are not connected to a database configured for Archi 4

### v0.10 (26/09/2016)
add Neo4j driver
* Update all import/export methods to generate SQL and Cypher (CQL) requests
* Integration with Archi's preferences
* The folder where is stored the preferences file has changed  
* Standalone import mode is now the default until we can share components between models
* Rewrite of export and import classes to increase reliability
* Add exceptions to reduce the cases when the model is not complete
* Bug resolution : add bounds export/import for DiagramModelReference
* The Filter when selecting a model is (temporarily) deactivated for Neo4J databases

### v0.9 (22/08/2016)
* Images are now imported and exported correctly

### v0.8 (13/06/2016)
* All the Archi components are now managed
* Add a progressbar to follow the import and export processes

### v0.7 (24/05/2016)
* Use of hashtables to accelerate object searches
* Begin to add some Javadoc

### v0.6 (23/05/2016)
* Few optimizations
* Solve a dependency mistake in JAR
* Solve a bug in the folders loading that prevented the objects to be correctly included in the model

### v0.5 (15/05/2016)
* Allow to import several projects in a single model
* Add support for folders and canvas
* Add SQLite driver

### v0.4 (10/05/2016)
* Add Oracle driver and use lower case only table names to increase Windows/Linux portability

### v0.3 (08/05/2016)
* Add import filtering

### v0.2 (01/05/2016)
* Add models versionning

### v0.1 (25/03/2016)
* plugin creation
