package org.archicontribs.database;

import lombok.Getter;

/**
 * Enumeration of all the database drivers managed by the database plugin.
 * 
 * @author Herve Jouin
 */
public enum DBDatabaseDriver {
    MSSQL("ms-sql", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
    NEO4J("neo4j", "org.neo4j.jdbc.Driver"),
    ORACLE("oracle", "oracle.jdbc.driver.OracleDriver"),
    POSTGRESQL("postgresql", "org.postgresql.Driver"),
    SQLITE("sqlite", "org.sqlite.JDBC");
	
	@Getter private String driverName = null;
	@Getter private String driverClass = null;
	
	/**
	 * Sets the database driver name
	 * @param dbName 
	 * @param dbClass 
	 */
	private DBDatabaseDriver(String dbName, String dbClass) {
		this.driverName = dbName;
		this.driverClass = dbClass;
	}
	
	/**
	 * Gets the DBDatabaseDriver by its name
	 * @param dbName
	 * @return the DBDatabaseDriver value
	 */
	public static DBDatabaseDriver fromName(String dbName) {
		switch (dbName.toLowerCase()) {
			case "ms-sql": return MSSQL;
			case "mysql": return MYSQL;
			case "neo4j": return NEO4J;
			case "oracle": return ORACLE;
			case "postgresql": return POSTGRESQL;
			case "sqlite": return SQLITE;
			default: // just in case
		}
		
		return null;
	}
}
