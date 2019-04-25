package org.archicontribs.database.connection;
/**
 * wrapper to execute an INSERT or UPDATE request in the database
 * One may use '?' in the request and provide the corresponding values as parameters (strings, integers, booleans and byte[] are accepted)
 * @return the number of lines impacted by the request
 */

import java.sql.Connection;
import java.sql.SQLException;
//import org.archicontribs.database.DBLogger;

import lombok.Getter;

public class DBRequest extends DBStatement {
	//private static final DBLogger logger = new DBLogger(DBRequest.class);
	
	@Getter private int rowCount = 0;
	
	@SafeVarargs
	public <T> DBRequest(String driverName, Connection connection, String request, T... parameters) throws SQLException {
		super(driverName, connection, request, parameters);
		
		this.rowCount = executeUpdate();
	}
}
