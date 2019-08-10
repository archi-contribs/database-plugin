package org.archicontribs.database.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.archicontribs.database.DBPlugin;

import lombok.Getter;

/**
 * This enum stores the information required to connect to a database
 * @author Herve Jouin
 */
public enum DBDatabase {
	MSSQL(0, "ms-sql", 1433, true),
	MYSQL(1, "mysql", 3306, false),
	NEO4J(2, "neo4j", 7687, false),
	ORACLE(3, "oracle", 1521, true),
	POSTGRESQL(4, "postgresql", 5432, true),
	SQLITE(5, "sqlite", 0, false);
	
	public static final int NEO4J_VALUE = 0;
	public static final int MSSQL_VALUE = 1;
	public static final int MYSQL_VALUE = 2;
	public static final int ORACLE_VALUE = 3;
	public static final int POSTGRESQL_VALUE = 4;
	public static final int SQLITE_VALUE = 5;
	
    private static final DBDatabase[] VALUES_ARRAY = new DBDatabase[] {MSSQL, MYSQL, NEO4J, ORACLE, POSTGRESQL, SQLITE};
    
    public static final List<DBDatabase> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));
    
    public static final String[] DRIVER_NAMES = new String[] {MSSQL.getDriverName(), MYSQL.getDriverName(), NEO4J.getDriverName(), ORACLE.getDriverName(), POSTGRESQL.getDriverName(), SQLITE.getDriverName()};
    
    /**
     * Numeric value of the entry
     */
	@Getter private final int value;
	
	/**
	 * driver name of the database
	 */
	@Getter private final String driverName;
	
	/**
	 * default port that can be used to connect to the database
	 */
	@Getter private final int defaultPort;
	
	/**
	 * true is the database can handle schemas, false if it can't
	 */
	private final boolean hasSchema;
    public boolean hasSchema() {
        return this.hasSchema;
    }
	
    @SuppressWarnings("hiding")
	private DBDatabase(int value, String driverName, int defaultPort, boolean hasSchema) {
        this.value = value;
        this.driverName = driverName.toLowerCase();
        this.defaultPort = defaultPort;
        this.hasSchema = hasSchema;
    }
    
    /**
     * Gets the database properties from its driver-name
     * @param driverName
     * @return the database properties
     */
    public static DBDatabase get(String driverName) {
    	for ( DBDatabase database: VALUES_ARRAY )
    		if ( DBPlugin.areEqual(database.getDriverName(), driverName) ) return database;
        return null;
    }
    
    /**
     * Gets the database properties from its numeric value
     * @param value numeric value
     * @return the database properties
     */
    public static DBDatabase get(int value) {
        switch (value) {
            case NEO4J_VALUE: return NEO4J;
            case MSSQL_VALUE: return MSSQL;
            case MYSQL_VALUE: return MYSQL;
            case ORACLE_VALUE: return ORACLE;
            case POSTGRESQL_VALUE: return POSTGRESQL;
            case SQLITE_VALUE: return SQLITE;
			default:
				break;
        }
        return null;
    }

    /**
     * Gets the database driver name
     * @return the driver name
     */
    @Override
    public String toString() {
        return this.driverName;
    }
}
