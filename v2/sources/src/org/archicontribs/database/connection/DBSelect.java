package org.archicontribs.database.connection;
/**
 * Wrapper to generate and execute a SELECT request in the database<br>
 * One may use '?' in the request and provide the corresponding values as parameters (at the moment, only strings are accepted)<br>
 * The connection to the database should already exist 
 * @return the ResultSet with the data read from the database
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.archicontribs.database.DBLogger;

import lombok.Getter;

public class DBSelect extends DBStatement {
	private static final DBLogger logger = new DBLogger(DBSelect.class);
	
	@Getter ResultSet result = null;
	
	@SafeVarargs
	public <T> DBSelect(String theDriverName, Connection theConnection, String theRequest, T... theParameters) throws SQLException {
        super(theDriverName, theConnection, theRequest, theParameters);

        try {
            this.result = executeQuery();
        } catch (Exception err) {
            // in case of an SQLException, we log the raw request to ease the debug process
            if ( logger.isTraceEnabled() ) logger.trace("SQL Exception for database request: "+theRequest);
            throw err;
        }
	}
	
	public boolean next() throws SQLException {
		return this.result.next();
	}
	
	public int getInt(String columnLabel) throws SQLException {
		return this.result.getInt(columnLabel);
	}
	
	public String getString(String columnLabel) throws SQLException {
		return this.result.getString(columnLabel);
	}
	
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return this.result.getTimestamp(columnLabel);
	}
	
	public byte[] getBytes(String columnLabel) throws SQLException {
		return this.result.getBytes(columnLabel);
	}
	
	public Object getObject(String columnLabel) throws SQLException {
		return this.result.getObject(columnLabel);
	}
	
	@Override public void close() {
		try {
			if ( this.result != null && !this.result.isClosed() )
				this.result.close();
		} catch (SQLException err) {
			logger.error("Cannot close the ResultSet", err);
		}
		
		super.close();
	}
}
