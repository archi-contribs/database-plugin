# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

The current version (v0.6) is able to :
   - Export and import models to a database (PostGreSQL, MySQL, Oracle and SQLite drivers are included)
   - Version the models (keep an history on the model versions and allow to retreive a former version)
   - Show up how many Archi components are exported and imported (kind of statistics)
   - Filter the models listed on the import dialog (by element, relation or property)
   - import models either in standalone mode (each in their own Archi model) or in shared mode (each in a separate subfolder of the same Archi model container, allwing to create relations between them). This part still needs improvements.

The current limitations are :
   - All the Archi features are not yet managed (like sketches and images)
   - Text fields (model purpose, components documentation, ...) are limited in length (use of varchars instead of blobs)

My roadmap includes the following functionalities :
   - Of course, manage all Archi features (like sketches and images)
   - Manage locks (kind of check-in / check-out),
   - Manage credentials (read/only, read/write, administrator tasks, ...),
   - Reduce import/export duration,
   - Autiomatically generate view including a set of elements with all their dependencies (recursively) in order to allow impact analysis,
   - Map objects to CMDB CIs (as my company uses ServiceNow as CMDB provider, I will rework my ServiceNow plugin),
   - etc ...
   
