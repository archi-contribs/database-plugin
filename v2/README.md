# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

## Archi versions compatibility
* The v2 of the plugin as compatible with Archi 4 only.

## The current version is able to :
* Export and import models to a relational database (PostGreSQL, MySQL, Oracle and SQLite drivers are included)
* Version the models (keep an history on the model versions and allow to retrieve a former version)
* Version models components (keep an history on the components versions and allow to retrieve a former version)
* Share elements, relationships and views between models
* Update itself when a new release is available on GitHub
* Automatically create the database tables it needs

## Installation instructions :
* download the **org.archicontribs.database_v2.0.0.alpha1.jar** file to the Archi **plugins** folder
* start (or restart) Archi and the *import from database* and *export to database* menu entries should be visible ...

## Differences from v1 :
* Added log4j support
* Versionning at the element level
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

## Wiki
Please do not hesitate to have a look at the [Wiki](https://github.com/archi-contribs/database-plugin/wiki).