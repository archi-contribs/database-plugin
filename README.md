# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

The current version is able to :
   - Export and import models to a database (PostGreSQL, MySQL, Oracle and SQLite drivers are included)
   - Version the models (keep an history on the model versions and allow to retreive a former version)
   - Show up how many Archi components are exported and imported (kind od statistics)
   - Filter the models listed on the import dialog (by element, relation or property)
   - import models either in standalone mode (each in their own Archi model) or in shared mode (all in the same Archi model, each in a separate subfolder)

The current limitations are :
   - Text fields (model purpose, components names, documentation, ...) are limited in length
   - Models are independent (it is not yet possible to create relations between models)

My roadmap includes the following functionalities :
   - Allow relations between models,
   - Manage locks (kind of check-in / check-out),
   - Manage credentials (read/only, read/write, administrator tasks, ...),
   - Map  objects to CMDB CIs (as my company uses ServiceNow as CMDB provider, I will rework my ServiceNow plugin),
   - etc ...
   
