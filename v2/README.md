# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

## Archi versions compatibility
* The v2 of the plugin is compatible with Archi 4 only.

## The current version is able to :
* Export and import models to a relational database (PostGreSQL, MySQL, Oracle and SQLite drivers are included)
* Export elements and relationships to a graph database (Neo4J driver is included)
* Version the models (keep an history on the model versions and allow to retrieve a former version)
* Version models components (keep an history on the components versions and allow to retrieve a former version)
* Share elements, relationships and views between models
* Update itself when a new release is available on GitHub
* Automatically create and update the database tables when needed

## Installation instructions :
* download the **org.archicontribs.database_v2.0.1.alpha1.jar** file to the Archi **plugins** folder
* download the **sqljdbc_auth.dll** file to the Archi **JRE\bin** folder if you plan to use MSQ SQL integrated security mode (i.e. Windows authentication)
* start (or restart) Archi and the *import from database*, *export to database* menu entries and the *Database plugin* preferences page should be visible ...

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