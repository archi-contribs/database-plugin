# database-plugin
Database export/import plugin that store models in a central repository


The first release will only allow to store models in a central PostGreSQL repository.

But my roadmap includes the following functionalities :
   - enable other database brands (at least Oracle, MySQL, ...),
   - allow to search models (by name, by component, by owner, ...),
   - allow relations between models,
   - manage locks (kind of check-in / check-out),
   - manage versions (track who does what, show a previous version or even rollback to a previous version, ...),
   - provide statistics,
   - manage credentials (read/only, read/write, administrator tasks, ...),
   - map technical objects to CMDB CIs (as my company uses ServiceNow as CMDB provider, I will reuse my ServiceNow plugin),
   - etc ...
