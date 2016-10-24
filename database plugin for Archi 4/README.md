# Archimate Tool Database-Plugin for Archi 4

## Install the plugin
Download the **_org.archicontribs.database_v0.11(archi4).jar_** file into Archi's plugins folder (usually **_C:\Program Files\Archi\Plugins_** on Windows).

## Create the database
1. Create a database that will be used to store your models
At the moment, the following brands are supported :
* MySQL
* Oracle
* PostGreSQL
* SQLite
* Neo4j
2. Create the database tables using one of the script provided (Neo4j doesn't need tables creation).

## Configure the plugin
Run archi, go to Edit/Preferences/database plugin and enter your database information.

## Export and import your models
You are now ready to export your models to your database using the new **_export to database_** menu item and import them back using the new **_import from database_** menu item.

## Wiki
Please do not hesitate to have a look at the [Wiki](https://github.com/archi-contribs/database-plugin/wiki) for more information.