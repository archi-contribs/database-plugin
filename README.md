# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

The current version is able to :
   - Export and import models to a database (PostGreSQL, MySQL and Oracle drivers are included)
   - Version the models (keep an history on the model versions and allow to retreive a former version)
   - Show up how many Archi components are exported and imported (kind od statistics)
   - Filter the models listed on the import dialog (by element, relation or property)

The current limitations are :
   - Not all the Archi components are managed (like sketches and images)
   - Text fields (model purpose, components documentation, ...) are limited in length (use of varchars instead of blobs)
   - Models are independent (it is not possible create relations between models)

My roadmap includes the following functionalities :
   - Of course, manage all Archi components (like sketches and images)
   - Allow relations between models,
   - Manage locks (kind of check-in / check-out),
   - Manage credentials (read/only, read/write, administrator tasks, ...),
   - Map  objects to CMDB CIs (as my company uses ServiceNow as CMDB provider, I will rework my ServiceNow plugin),
   - etc ...
   
