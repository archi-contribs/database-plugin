
# beta release
Please note that this version is for testing purpose and should not be used on a production database.

**Please do not forget to fill in an issue if you discover a misbehaviour.**


## Release note
* The online help is finally beeing written. It can be accessed using the interrogation mark on every plugin window.
* Bug fixes:
  * Fix plugin initialization failure that occured some times
  * Fix progress bar during download new version of the plugin from GitHub
  * Increase compiler severity to maximum and resolve all the warnings
  * Reduce memory leak
* Export model:
  * Complete rewrite of the comparison management (use of timestamps in addition of version number)
  * Add an option to compare the model from the database before exporting it
  * For relational databases:
    * Create two export modes: standalone and collaborative modes
      * standalone mode means the export procedure only export components (it is quicker and adapted when only one person work on a model at one time)
      * collaborative mode means the export procedure syncs the model with the database (it is slower butadapted when several people are working on the same model at one time)
    * Allow to specify border width and scale factor for views screenshots
  * For Neo4J databases:
    * Create two export modes: native and extended
      * native mode means that Archi relationships are exported as Neo4J relationships (but this mode does not allow to export relationships on relationships)
      * extended mode means that Archi relationships are exported as Neo4J nodes (this mode makes Neo4J diagrams more complex but allow relationships on relationships)
    * New option to empty the database before the export
    * New option to specialize relationships
* Import individual component:
  * Added documentation column in the array to help distinguish components having the same name
  * Add popup message during the import as it may take some time
* Get history from database:
  * Allow to get history for diagrams, canvas and sketches
* Other:
  * Check for null images before export 
  * Add the ability to import an image from the database (on the Image and Block objects in Canvas)
  * Remove the name, the documentation and the properties from view objects and connections checksum as they are not relatiod to the view objects and connections themselves, but to the related element or relationship
  * Add procedures that can be called by the script plugin
  * Update JDBC drivers
    * Neo4J to 3.1.0
    * SQLite to 3.21.0
    * PostGreSQL to 42.2.1


## Known bugs
* Export views
The views checksum are calculated at the begining of the export process but their content may be updated when the elements and relationships are exported.
Specifically, elements and relationships may have new name or may be removed, so the checksum should be calculated after the elements and relationships have been exported.