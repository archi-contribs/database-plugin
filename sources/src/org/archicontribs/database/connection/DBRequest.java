package org.archicontribs.database.connection;

import java.sql.Connection;
import java.sql.SQLException;
//import org.archicontribs.database.DBLogger;

import lombok.Getter;

/**
 * Helper class that allows to execute a request upon a database
 * 
 * @author Herve Jouin
 */
public class DBRequest extends DBStatement {
	//private static final DBLogger logger = new DBLogger(DBRequest.class);
	
	@Getter private int rowCount = 0;
	
	/**
	 * wrapper to execute an INSERT or UPDATE request in the database<br>
	 * One may use '?' in the request and provide the corresponding values as parameters (strings, integers, booleans and byte[] are accepted)
	 * @return the number of lines impacted by the request
	 * @param <T>
	 * @param theDriverName
	 * @param theConnection
	 * @param theRequest
	 * @param theParameters
	 * @throws SQLException
	 */
	@SafeVarargs
	public <T> DBRequest(String theDriverName, Connection theConnection, String theRequest, T... theParameters) throws SQLException {
		super(theDriverName, theConnection, theRequest, theParameters);
		
		this.rowCount = executeUpdate();
	}
}
