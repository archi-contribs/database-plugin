# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

## Archi versions compatibility
* The plugin is available for Archi 3
* It is also provided for Archi 4 which is under development

## The current version is able to :
* Export and import models to a relational database (PostGreSQL, MySQL, Oracle and SQLite drivers are included)
* Export and import models to a graph database (Neo4j driver is included)
* Version the models (keep an history on the model versions and allow to retrieve a former version)
* Show up how many Archi components are exported and imported (kind of statistics)
* Filter the models listed on the import dialog (by element, relation or property)
* import models either in standalone mode (each in their own Archi model) or in shared mode (all in the same Archi model, each in a separate subfolder)

## The current limitations are :
* The use of elements from one model into another or the use of relationships between models is not yet fully supported

## My roadmap includes the following functionalities :
* Allow relations between models,
* Manage locks (kind of check-in / check-out),
* Manage credentials (read/only, read/write, administrator tasks, ...),
* Map  objects to CMDB CIs (as my company uses ServiceNow as CMDB provider, I will rework my ServiceNow plugin),
* etc ...
   
## Wiki
Please do not hesitate to have a look at the [Wiki](https://github.com/archi-contribs/database-plugin/wiki).