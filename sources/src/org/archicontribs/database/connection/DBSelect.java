package org.archicontribs.database.connection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.archicontribs.database.DBLogger;

import lombok.Getter;

/**
 * Helper class that allows to execute a SQL select request upon a database
 * 
 * @author Herve Jouin
 */
public class DBSelect extends DBStatement {
	private static final DBLogger logger = new DBLogger(DBSelect.class);
	
	@Getter ResultSet result = null;
	
	/**
	 * Wrapper to generate and execute a SELECT request in the database<br>
	 * One may use '?' in the request and provide the corresponding values as parameters (at the moment, only strings are accepted)<br>
	 * The connection to the database should already exist 
	 * @param theDriverName 
	 * @param theConnection 
	 * @param theRequest 
	 * @param theParameters 
	 * @param <T> 
	 * @return the ResultSet with the data read from the database
	 * @throws SQLException 
	 */
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
	
	/**
	 * May be called after a DBselect call
	 * @return the next value
	 * @throws SQLException
	 */
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
	
	public Date getDate(String columnLabel) throws SQLException {
		return this.result.getDate(columnLabel);
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
