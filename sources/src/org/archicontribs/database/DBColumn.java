package org.archicontribs.database;

import java.sql.SQLException;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Herve Jouin
 *
 */
public class DBColumn {
	@Getter @Setter String name = "";
	@Getter @Setter String type = "";
	@Getter @Setter int length = 0;
	@Getter @Setter boolean notNull = true;
	@Getter @Setter Object metadata = null;
	@Getter int maxLength = 2000000000;	// do not know why, but it seems to be hard coded in JDBC
	
	private static final String VARCHAR = "VARCHAR";
	private static final String NVARCHAR = "NVARCHAR";
	private static final String INTEGER = "INTEGER";
	private static final String NUMBER = "NUMBER";
	private static final String TINYINT = "TINYINT";
	private static final String SERIAL = "SERIAL";
	private static final String TEXT = "TEXT";
	private static final String BLOB = "BLOB";
	private static final String CLOB = "CLOB";
	private static final String DATETIME = "DATETIME";
	
	/**
	 * 
	 * @param name
	 * @param type
	 * @param length
	 * @param isNotNull
	 */
	@SuppressWarnings("hiding")
	public DBColumn(String name, String type, int length, boolean isNotNull) {
		set(name, type, length, isNotNull);
	}
	
	/**
	 * 
	 * @param name 
	 * @param dbEntry
	 * @param columnType
	 * @param isNotNull 
	 * @throws SQLException 
	 */
	@SuppressWarnings("hiding")
	public DBColumn (String name, DBDatabaseEntry dbEntry, DBColumnType columnType, boolean isNotNull) throws SQLException {
		switch (dbEntry.getDriver()) {
	        case POSTGRESQL:
	        	switch ( columnType ) {
	        		case AUTO_INCREMENT : set(name, SERIAL,      0, isNotNull); break;
	        		case BOOLEAN :        set(name, "INT2",        0, isNotNull); break;
	        		case COLOR :          set(name, VARCHAR,     7, isNotNull); break;
	        		case DATETIME :       set(name, "TIMESTAMP",   0, isNotNull); break;
	        		case FONT :           set(name, VARCHAR,   150, isNotNull); break;
	        		case IMAGE :          set(name, "BYTEA",       0, isNotNull); break;
	        		case INTEGER :        set(name, "INT4",        0, isNotNull); break;
	        		case OBJECTID :       set(name, VARCHAR,    50, isNotNull); break;
	        		case OBJ_NAME :       set(name, VARCHAR,  1024, isNotNull); break;
	        		case STRENGTH :       set(name, VARCHAR,    20, isNotNull); break;
	        		case TEXT :           set(name, TEXT,        0, isNotNull); break;
	        		case TYPE :           set(name, VARCHAR,     3, isNotNull); break;
	        		case USERNAME :       set(name, VARCHAR,    30, isNotNull); break;
	        		default:              break;
	        	}
	        	break;
	        case MSSQL:
	        	switch ( columnType ) {
	        		case AUTO_INCREMENT : set(name, "INT IDENTITY",0, isNotNull); break;
	        		case BOOLEAN :        set(name, TINYINT,     0, isNotNull); break;
	        		case COLOR :          set(name, VARCHAR,     7, isNotNull); break;
	        		case DATETIME :       set(name, DATETIME,    0, isNotNull); break;
	        		case FONT :           set(name, VARCHAR,   150, isNotNull); break;
	        		case IMAGE :          set(name, "IMAGE",       0, isNotNull); break;
	        		case INTEGER :        set(name, "INT",         0, isNotNull); break;
	        		case OBJECTID :       set(name, VARCHAR,    50, isNotNull); break;
	        		case OBJ_NAME :       set(name, VARCHAR,  1024, isNotNull); break;
	        		case STRENGTH :       set(name, VARCHAR,    20, isNotNull); break;
	        		case TEXT :           set(name, NVARCHAR,   -1, isNotNull); break;
	        		case TYPE :           set(name, VARCHAR,     3, isNotNull); break;
	        		case USERNAME :       set(name, VARCHAR,    30, isNotNull); break;
	        		default:              break;
	        	}
	        	break;
	        case MYSQL:
	        	switch ( columnType ) {
	        		case AUTO_INCREMENT : set(name, "INT AUTO_INCREMENT", 0, isNotNull); break;
	        		case BOOLEAN :        set(name, TINYINT,     0, isNotNull); break;
	        		case COLOR :          set(name, VARCHAR,     7, isNotNull); break;
	        		case DATETIME :       set(name, DATETIME,    0, isNotNull); break;
	        		case FONT :           set(name, VARCHAR,   150, isNotNull); break;
	        		case IMAGE :          set(name, "LONGBLOB",    0, isNotNull); break;
	        		case INTEGER :        set(name, "INT",        10, isNotNull); break;
	        		case OBJECTID :       set(name, VARCHAR,    50, isNotNull); break;
	        		case OBJ_NAME :       set(name, VARCHAR,  1024, isNotNull); break;
	        		case STRENGTH :       set(name, VARCHAR,    20, isNotNull); break;
	        		case TEXT :           set(name, "MEDIUMTEXT",  0, isNotNull); break;
	        		case TYPE :           set(name, VARCHAR,     3, isNotNull); break;
	        		case USERNAME :       set(name, VARCHAR,    30, isNotNull); break;
	        		default:              break;
	        	}
	        	break;
	        case ORACLE:
	        	switch ( columnType ) {
	        		case AUTO_INCREMENT : set(name, INTEGER,     0, isNotNull); break;
	        		case BOOLEAN :        set(name, "CHAR",        1, isNotNull); break;
	        		case COLOR :          set(name, VARCHAR,     7, isNotNull); break;
	        		case DATETIME :       set(name, "DATE",        0, isNotNull); break;
	        		case FONT :           set(name, VARCHAR,   150, isNotNull); break;
	        		case IMAGE :          set(name, BLOB,        0, isNotNull); break;
	        		case INTEGER :        set(name, INTEGER,     0, isNotNull); break;
	        		case OBJECTID :       set(name, VARCHAR,    50, isNotNull); break;
	        		case OBJ_NAME :       set(name, VARCHAR,  1024, isNotNull); break;
	        		case STRENGTH :       set(name, VARCHAR,    20, isNotNull); break;
	        		case TEXT :           set(name, CLOB,        0, isNotNull); break;
	        		case TYPE :           set(name, VARCHAR,     3, isNotNull); break;
	        		case USERNAME :       set(name, VARCHAR,    30, isNotNull); break;
	        		default:              break;
	        	}
	        	break;
	        case SQLITE:
	        	switch ( columnType ) {
	        		case AUTO_INCREMENT : set(name, INTEGER,     0, isNotNull); break;
	        		case BOOLEAN :        set(name, TINYINT,     0, isNotNull); break;
	        		case COLOR :          set(name, VARCHAR,     7, isNotNull); break;
	        		case DATETIME :       set(name, "TIMESTAMP",   0, isNotNull); break;
	        		case FONT :           set(name, VARCHAR,   150, isNotNull); break;
	        		case IMAGE :          set(name, BLOB,        0, isNotNull); break;
	        		case INTEGER :        set(name, INTEGER,    10, isNotNull); break;
	        		case OBJECTID :       set(name, VARCHAR,    50, isNotNull); break;
	        		case OBJ_NAME :       set(name, VARCHAR,  1024, isNotNull); break;
	        		case STRENGTH :       set(name, VARCHAR,    20, isNotNull); break;
	        		case TEXT :           set(name, CLOB,        0, isNotNull); break;
	        		case TYPE :           set(name, VARCHAR,     3, isNotNull); break;
	        		case USERNAME :       set(name, VARCHAR,    30, isNotNull); break;
	        		default:              break;
	        	}
	        	break;
	        case NEO4J:
	        	break;
	        default:
	        	// just in case;
	        	throw new SQLException("Unknonwn driver " + dbEntry.getDriver());
		}
    }
	
	@SuppressWarnings("hiding")
	private void set(String name, String type, int length, boolean isNotNull) {
		this.name = name;
		// if type has got parenthesis, the number enclosed takes precedence over the dbLenght parameter
		int parenthesis = type.indexOf('(');
		if ( parenthesis == -1 ) {
			this.type = type;
			this.length = length;
		} else {
			this.type = type.substring(0,parenthesis);
			this.length = Integer.parseInt(type.substring(parenthesis+1, type.length()-1));
		}
		this.notNull = isNotNull;
	}
	
	/**
	 * 
	 * @param columns
	 * @return
	 */
	public static String[] getColumnNames(List<DBColumn> columns) {
		String[] columnNames = new String[columns.size()];
		for ( int i = 0 ; i < columns.size() ; ++i ) {
			columnNames[i] = columns.get(i).getName();
		}
		return columnNames;
	}

	/**
	 * 
	 * @return
	 */
	@Override public String toString() {
		StringBuilder result = new StringBuilder(this.name);
		result.append(": ");
		result.append (getFullType());
		
		return result.toString();
	}
	
	/**
	 * @return
	 */
	public String getFullType() {
		StringBuilder fullType = new StringBuilder();
		
		fullType.append (this.type);
		
		if ( this.length != 0 && this.length != this.maxLength) {
			fullType.append("(");
			if ( this.length > 0 )
				fullType.append(this.length);
			else
				fullType.append("MAX");
			fullType.append(")");
		}
		
		if ( this.notNull ) {
			fullType.append(" NOT NULL");
		}
		
		return fullType.toString();
	}
	
	/**
	 * 
	 * @param compareTo
	 * @return
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (this.getClass() != obj.getClass())
			return false;
		
		DBColumn compareTo = (DBColumn)obj;
		
		if ( this.name.compareToIgnoreCase(compareTo.name) != 0 )
			return false;
		
		// we compare only the fist word in the type, to avoid checking for AUTO_INCREMENT 
		String thisType = this.type.split(" ")[0].toUpperCase();
		String compareToType = compareTo.type.split(" ")[0].toUpperCase();
		
		// we consider VARCHAR & VARCHAR2, and NUMBER & INTEGER being the same
		 if ( !(thisType.startsWith(VARCHAR) && compareToType.startsWith(VARCHAR))
				 && !((thisType.equals(NUMBER) || compareToType.equals(NUMBER)) && (thisType.equals(INTEGER) || compareToType.equals(INTEGER)))
				 && thisType.compareToIgnoreCase(compareToType) != 0
				 )
			 return false;

		// we consider NUMBER(38) and INTEGER(10) being the same
		if ( (this.length > 0 && compareTo.length > 0 && this.length != compareTo.length)
				&& !((thisType.equals(NUMBER) || thisType.equals(INTEGER)) && Math.abs(this.length-compareTo.length)==28)
				)
			return false;
		
		return this.notNull == compareTo.notNull;
	}
	
	@Override
	public int hashCode() {
		return (this.name+"\n"+this.type+"\n"+this.length).hashCode();
	}
}