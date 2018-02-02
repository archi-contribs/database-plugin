package org.archicontribs.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    
	private final int value;
	private final String driverName;
	private final int defaultPort;
	private final boolean hasSchema;
	
    private DBDatabase(int value, String driverName, int defaultPort, boolean hasSchema) {
        this.value = value;
        this.driverName = driverName;
        this.defaultPort = defaultPort;
        this.hasSchema = hasSchema;
    }
    
    public static DBDatabase get(String driverName) {
    	for ( DBDatabase database: VALUES_ARRAY )
    		if ( DBPlugin.areEqual(database.getDriverName(), driverName) ) return database;
        return null;
    }
    
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

	public String getDriverName() {
		return this.driverName.toLowerCase();		// just in case
	}

	public int getDefaultPort() {
		return this.defaultPort;
	}

	public boolean hasSchema() {
		return this.hasSchema;
	}

	public int getValue() {
		return this.value;
	}
	
    @Override
    public String toString() {
        return this.driverName;
    }
}
