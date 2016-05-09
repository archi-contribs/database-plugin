# Archimate Tool Database-Plugin
Database export/import plugin that store models in a central repository.

The current version is able to :
   - Export and import models to a database (PostGreSQL and MySQL drivers are included)
   - version the models (keep an history on the model versions and allow to retreive a former version)
   - show up how many Archi components are exported and imported (kind od statistics)
   - filter the models listed on the import dialog (by element, relation or property)

The current limitations are :
   - not all the Archi components are managed (like sketches and images)
   - Text fields (model purpose, components documentation, ...) are limited in length (use of varchars)
   - models are independent (it is not possible create relations between models)

My roadmap includes the following functionalities :
   - of course, manage all Archi components (like sketches and images)
   - enable other database brands (at least relational ones like Oracle but eventually nosql ones),
   - Retreive the username rather that having a basic text field to manually fill-in,
   - allow relations between models,
   - manage locks (kind of check-in / check-out),
   - manage credentials (read/only, read/write, administrator tasks, ...),
   - map  objects to CMDB CIs (as my company uses ServiceNow as CMDB provider, I will reuse my ServiceNow plugin),
   - etc ...