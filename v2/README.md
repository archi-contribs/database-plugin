# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

## Archi versions compatibility
* The v2 of the plugin is compatible with Archi 4 only.

## The current version is able to :
* Export and import models to a relational database (PostGreSQL, MySQL, MS SQL Server, Oracle and SQLite drivers are included)
* Export elements and relationships to a graph database (Neo4J driver is included)
* Version the models (keep an history on the model versions and allow to retrieve a former version)
* Version models components (keep an history on the components versions and allow to retrieve a former version)
* Share elements, relationships and views between models
* Update itself when a new release is available on GitHub
* Automatically create and update the database tables when needed

## Installation instructions :
1 Download the archiplugin file from GitHub.

2 Run Archi, go to menu Help / Manage Plug-ins and then click on the "Install New ..." button

3 Select the archiplugin file you previously downloaded

Once installed, you may update the plugin to a newer version through the same procedure or click on the "check for update" button on the preference page.

## Key Differences from v1 :
* Added log4j support
* Versionning at the element level
* Reduce the quantity of data exported by exporting only updated components (use of checksums)
* Detect database conflicts and add a conflict resolution mechanism
* Reduce number of database tables
* Reduce database table name length to be compliant will all database brands
* Add back Oracle JDBC driver
* Complete rework of the graphical interface
* Add the ability to import components from other models
* Add inline help
* ...

## Wiki
Please do not hesitate to have a look at the [Wiki](https://github.com/archi-contribs/database-plugin/wiki).