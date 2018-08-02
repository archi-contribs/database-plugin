# beta release

|`Please note that this version is for testing purpose. **It must not be used on a production database**`|  |
|------------------------------------------------|--|
|                                                |  |

`Please dho not forget to fill in an issue if you discover a misbehaviour.`

## Plugin installation
Download the *org.archicontribs.database_xxx.jar* file in Archi's plugins folder and restart Archi.

As the plugin is a beta release, it is not able to update itself when a new beta release is available. To update the plugin, you need to manually replace the old jar file by the new one.

## Known bugs or points of attention:
These points need to be addressed before the release of version 2.1:

* When a component is moved to another folder, it is not seen as a component change. If only folder moves are done, then the export plugin may say that the model is up to date even if it is not.

## Planned improvements
In addition, some improvements are planned but it's not guaranteed that they will be part of the version 2.1:

* General
  * Improve the inline help and the GitHub wiki that lack a lot of information (specifically, the algorithms should be more detailed)
  * Optimize the SQL requests
  * Improve import performance by gathering all the objects properties and connections bound points in hashmaps in order to reduce the number of SQL requests generated (but this will increase even more the memory consumption)
  * Develop a database admin interface with housekeeping (delete a model, delete a specific model's version, list and delete components that are not part of any model, vacuum database, backup database, restore database, creation of indexes for big databases, ...)
  * Allow to replace a component by another one (and moving all the relations to this new component)
  * Allow to change a component's class (verifying the relationships conformity)
  * Now that the sync works, allow the creation of branches rather than a linear version management
  * Add support for new database brands (DB2, Sybase, MongoDB, and more generally ODBC)
  * Create a window that provide detailed statistics about the model and its content

* ''For the "import individual component":''
  * Allow to recursively import elements that have got relations with the imported element (with depth limit)

## Changes from the version 2.0.7b:
* *Import components:*
  * Added documentation column in the table to help distinguish components having the same name
  * Added tootip with properties to help distinguish components having the same name
  * Add message during the import as it may take some time
  * Imports can now be undone / redone
  * A label now explains that the icons can be selected on the import element window
  * Classes to import are pre-selectionned depending on the folder where the components will be imported to
  * The categories can now be clicked to select/un-select the whole category
  * It is now possible to import (merge) models
  * Introduce new "template" import mode that mixes copy and shared mode

* *Import model:*
  * A context menu entry allowin got import a model has been added when no model is selected
  * Automatically open the default view (if any) at the end of a successful import
  * Fix number of images to import

* *Export model:*
  * Complete rewrite of the comparison management (use timestamps in addition of version number as it is possible to switch from a database to another)
  * In case of exception, the database lock is released before the error message is displayed
  * An option has been added to show / not show the model's comparison to the database before the export (showing comparison gives more information to the user but it takes some time on big models)
  * *For relational databases*:
    * The export is now in "collaborative mode", which syncs the model with the database:
      * It can be compared to a pull+push to GitHub.
      * It is slower than the previous mode but allows several people to work on the same model at the same time
    * Allow to specify border width and scale factor for views screenshots
	* To simplify, it is no more possible to choose between whole export or elements and relationships only
  * *For Neo4J databases*:
    * Create two export modes: native and extended:
      * *Native mode* means that Archi relationships are exported as Neo4J relationships (but this mode does not allow to export relationships on relationships)
      * *Extended mode* means that Archi relationships are exported as Neo4J nodes (this mode makes Neo4J diagrams more complex but allow relationships on relationships)
    * New option to empty the database before the export
    * New option to specialize relationships

* *Get history from database:*
  * It is now possible to get history for all the model components (including folders, diagrams, canvas and sketches)
  * It is now possible to import/export an individual component from the history window

* *Other:*
  * *bug fixes: *
    * Fix export blocks and images objects that have got no image set
    * Fix plugin initialization failure that occurred some times
    * Fix progress bar during download new version of the plugin from GitHub
    * Fix centering of GUI windows, especially on hiDPI displays
    * Fix calculation of numbers of images to import
    * Fix properties updates when several of them have got the same name during sync of existing components
    * Fix centering of GUI windows especially on HiDPI displays
  * *Improvements:*
    * Fill in the inline help pages
    * Rewrite debug and trace messages to be more useful
    * Increase compiler severity to maximum and resolve all the warnings to improve code resiliency
    * Reduce memory leak
    * Better management of the cancel button during the import and export process
    * Add the ability to import an image from the database (on the Image and Block objects in Canvas)
    * Remove the name, the documentation and the properties from view objects and connections checksum as they are not related to the view objects and connections themselves, but to the related element or relationship
    * Some annoying popups have been replaced by messages directly in the import/export window
    * Add procedures that can be called by the script plugin
    * The inline help can be accessed using the interrogation mark on every plugin window.
    * Export and import back the model's "metadata" (may be used by other external tools)
    * Do not calculate checksum on images anymore as the path is already a kind of checksum
    * A new "show debug information" window has been created
    * Add "import model from database" menu entry on right click when no model has been loaded yet
    * Manage the objects transparency (introduced in Archi 4.3)
    * Check for max memory available at startup and suggest to increase it (Xmx parameter) if less than 1 GB
    * Add the ability to import an image from the database on Canvas Image and Block objects
    * Update JDBC drivers
      * Neo4J to 3.1.0
      * SQLite to 3.21.0
      * PostGreSQL to 42.2.1