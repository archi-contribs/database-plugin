package org.archicontribs.database.connection;


import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBDatabase;

/**
 * Helper class that allows to construct a preparedStatement
 */
public class DBStatement implements AutoCloseable {
	private static final DBLogger logger = new DBLogger(DBStatement.class);

	String driverName = null;
	Connection connection = null;
	Statement statement = null;
	PreparedStatement preparedStatement = null;
	String request = null;

	/**
	 * HelperMethod to construct the PreparedStatement from the specified request and all its parameters
	 * @param <T>
	 * @param theDriverName
	 * @param theConnection
	 * @param theRequest
	 * @param theParameters
	 * @throws SQLException
	 */
	@SafeVarargs
	public <T> DBStatement(String theDriverName, Connection theConnection, String theRequest, T... theParameters) throws SQLException {
		this.driverName = theDriverName;
		this.connection = theConnection;
		this.request = theRequest;
		try {
			if ( theParameters.length == 0 )
				this.statement = theConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			else {
				this.preparedStatement = theConnection.prepareStatement(theRequest, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				constructStatement(theParameters);
			}
		} catch (SQLException err) {
			// in case of an SQLException, we log the raw request to ease the debug process
			if ( logger.isTraceEnabled() ) logger.trace("SQL Exception for database request: "+theRequest);
			throw err;
		}

	}

	/**
	 * Executes the preparedStatement where no answer is awaited
	 * @return
	 * @throws SQLException
	 */
	public ResultSet executeQuery() throws SQLException {
		if ( this.statement != null && ! this.statement.isClosed() ) 
			return this.statement.executeQuery(this.request);

		if ( this.preparedStatement != null && ! this.preparedStatement.isClosed() ) 
			return this.preparedStatement.executeQuery();

		return null;
	}

	/**
	 * executes the preparedStement when it is related to an UPDATE request
	 * @return the number or updated rows
	 * @throws SQLException
	 */
	public int executeUpdate() throws SQLException {
		Savepoint savepoint = null;
		int rowCount = 0;
		
		if ( logger.isTraceEnabled() && !this.request.toLowerCase().startsWith("match") && !this.request.toLowerCase().startsWith("create") && !this.request.toLowerCase().startsWith("insert") && !this.request.toLowerCase().startsWith("update")) {
			// MATCH, CREATE, INSERT and UPDATE request are going through constructStatement that has got its own trace
			logger.trace("      --> "+this.request);
		}

		try {
			// on PostGreSQL databases, we can only send new requests if we rollback the transaction that caused the exception
			if ( DBPlugin.areEqual(this.driverName, DBDatabase.POSTGRESQL.getDriverName()) )
				savepoint = this.connection.setSavepoint();
			
			if ( this.statement != null && !this.statement.isClosed() )
				rowCount = this.statement.executeUpdate(this.request);
			else
				if ( this.preparedStatement != null && !this.preparedStatement.isClosed() )
					rowCount = this.preparedStatement.executeUpdate();
		} catch (SQLException err) {
			if ( savepoint != null ) {
				try {
					rollback(savepoint);
					if ( logger.isTraceEnabled() ) logger.trace("Rolled back to savepoint");
				} catch (SQLException e2) { logger.error("Failed to rollback to savepoint", e2); }
			}
			throw err;
		} finally {
			if ( savepoint != null )
				this.connection.releaseSavepoint(savepoint);
		}
		
		return rowCount;
	}

	/**
	 * Rollbacks the current database transactions from a specified savePoint
	 * @param savepoint 
	 * @throws SQLException 
	 */
	public void rollback(Savepoint savepoint) throws SQLException {
		if ( this.connection.getAutoCommit() ) {
			if ( logger.isDebugEnabled() ) logger.debug("Do not rollback as database is in auto commit mode.");
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Rollbacking database transaction.");
			if ( savepoint == null )
				this.connection.rollback();
			else
				this.connection.rollback(savepoint);
		}
	}

	/**
	 * Rollbacks the current database transactions
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		rollback(null);
	}

	@SuppressWarnings("unchecked")
	<T> void constructStatement(T... parameters) throws SQLException {
		StringBuilder debugRequest = new StringBuilder();
		String[] splittedRequest = this.request.split("\\?");

		int requestRank = 0;
		int parameterRank = 0;
		while (parameterRank < parameters.length) {
			if ( logger.isTraceEnabled() ) debugRequest.append(splittedRequest[requestRank]);

			if ( parameters[parameterRank] == null ) {
				if ( logger.isTraceEnabled() ) debugRequest.append("null");
				this.preparedStatement.setString(++requestRank, null);
			} else if ( parameters[parameterRank] instanceof String ) {
				if ( logger.isTraceEnabled() ) debugRequest.append("'"+parameters[parameterRank]+"'");
				this.preparedStatement.setString(++requestRank, (String)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof Integer ) {
				if ( logger.isTraceEnabled() ) debugRequest.append(parameters[parameterRank]);
				this.preparedStatement.setInt(++requestRank, (int)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof Timestamp ) {
				if ( logger.isTraceEnabled() ) debugRequest.append(String.valueOf(parameters[parameterRank]));
				this.preparedStatement.setTimestamp(++requestRank, (Timestamp)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof Boolean ) {
				if ( logger.isTraceEnabled() ) debugRequest.append(String.valueOf((boolean)parameters[parameterRank]));
				this.preparedStatement.setBoolean(++requestRank, (Boolean)parameters[parameterRank]);

			} else if ( parameters[parameterRank] instanceof ArrayList<?> ){
				for(int i = 0; i < ((ArrayList<String>)parameters[parameterRank]).size(); ++i) {
					if ( logger.isTraceEnabled() ) {
						if ( i != 0 )
							debugRequest.append(",");
						debugRequest.append("'"+((ArrayList<String>)parameters[parameterRank]).get(i)+"'");
					}
					this.preparedStatement.setString(++requestRank, ((ArrayList<String>)parameters[parameterRank]).get(i));
				}
			} else if ( parameters[parameterRank] instanceof byte[] ) {
				try  {
					this.preparedStatement.setBinaryStream(++requestRank, new ByteArrayInputStream((byte[])parameters[parameterRank]), ((byte[])parameters[parameterRank]).length);
					if ( logger.isTraceEnabled() ) debugRequest.append("[image as stream ("+((byte[])parameters[parameterRank]).length+" bytes)]");
				} catch (@SuppressWarnings("unused") Exception err) {
					this.preparedStatement.setString(++requestRank, Base64.getEncoder().encodeToString((byte[])parameters[parameterRank]));
					if ( logger.isTraceEnabled() ) debugRequest.append("[image as base64 string ("+((byte[])parameters[parameterRank]).length+" bytes)]");
				}

			} else {
				if ( logger.isTraceEnabled() ) logger.trace("   "+this.request);
				throw new SQLException("Unknown "+parameters[parameterRank].getClass().getSimpleName()+" parameter in SQL select.");
			}
			++parameterRank;
		}
		if ( logger.isTraceSQLEnabled() ) {
			if ( requestRank < splittedRequest.length )
				debugRequest.append(splittedRequest[requestRank]);
			logger.trace("      --> "+debugRequest.toString());
		}
	}

	@Override public void close() {
		try {
			if ( this.statement != null && !this.statement.isClosed() ) {
				this.statement.close();
				this.statement = null;
			}
		} catch (SQLException err) {
			logger.error("Cannot close the Statement", err);
		}

		try {
			if ( this.preparedStatement != null && !this.preparedStatement.isClosed() ) {
				this.preparedStatement.close();
				this.preparedStatement = null;
			}
		} catch (SQLException err) {
			logger.error("Cannot close the PreparedStatement", err);
		}
	}
}
