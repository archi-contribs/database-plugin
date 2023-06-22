package org.archicontribs.database;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.archicontribs.database.data.DBDatabase;
import org.eclipse.jface.preference.IPersistentPreferenceStore;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains all the information required to connect to to a database 
 * 
 * @author Herve Jouin
 */
public class DBDatabaseEntry {
	private static final DBLogger logger = new DBLogger(DBDatabaseEntry.class);
	
	/**
	 * prefix used in the preference store
	 */
	static final String preferenceName = "databases";

    /**
     * Creates a new DBDatabaseEntry.
     */
    public DBDatabaseEntry() {
    	this.index = -1;
    	this.name = "";
    	this.driver = null;
    }
    
	/**
	 * DBDatabaseEntries are sorted, so we keep its index 
	 */
	@Getter @Setter private int index = -1;		// -1 means not initialized; i.e. not persisted in the preference store
    
	/**
	 * ID of the database Entry 
	 */
	@Getter @Setter private String id = "";

	/**
	 * Name of the database Entry 
	 */
	@Getter @Setter private String name = "";
	
	/**
	 * Driver to use to access the database<br>
	 * <br>
	 * must be one of @DBDatabase.VALUES
	 */
	@Getter @Setter private DBDatabaseDriver driver = null;
	
	/**
	 * Name or IP address of the server hosting the database
	 */
	@Getter @Setter private String server = "";
	
	/**
	 * TCP port on which the database listens to<br>
	 * <br>
	 * port should be between 0 and 65535 
	 */
	@Getter private int port = 0;
	
	/**
     * TCP port on which the database listens to<br>
     * <br>
	 * @param portValue should be between 0 and 65535 
	 * @throws DBException if the port is negative or greater than 65535
     */
    public void setPort(int portValue) throws DBException {
        // we ensure that the port is > 0 and < 65536
        if ( (portValue < 0) || (portValue > 65535) )
            throw new DBException("Port should be between 0 and 65535");
        this.port = portValue;
    }
	
	/**
	 * Database name
	 */
	@Getter @Setter private String database = "";
	
	/**
	 * Schema name
	 */
	@Getter @Setter private String schema = "";
	
	/**
	 * Username used to connect to the database
	 */
	@Getter @Setter private String username = "";
	
	/**
	 * Encrypted password used to connect to the database
	 */
	@Getter @Setter private String encryptedPassword = "";
	
	/**
	 * Get the decrypted the password used to connect to the database
	 * @return decrypted password
	 * @throws InvalidKeyException 
	 * @throws IllegalBlockSizeException 
	 * @throws BadPaddingException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException 
	 */
	public String getDecryptedPassword() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
		if ( this.encryptedPassword.equals("") )
			return "";
		return new String(getCipher(Cipher.DECRYPT_MODE).doFinal(Base64.getDecoder().decode(this.encryptedPassword)));
	}
	
	/**
	 * Set the decrypted password used to connect to the database
	 * @param password the decrypted password
	 * @throws InvalidKeyException 
	 * @throws IllegalBlockSizeException 
	 * @throws BadPaddingException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException 
	 */
	public void setDecryptedPassword(String password) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
		this.encryptedPassword = DBPlugin.isEmpty(password) ? "" : Base64.getEncoder().encodeToString(getCipher(Cipher.ENCRYPT_MODE).doFinal(password.getBytes()));
	}
	
	/**
	 * Should we export snapshots of the views
	 */
	@Getter @Setter private boolean viewSnapshotRequired = false;
	
	/**
	 * Border width to add around view snapshots
	 */
	@Getter @Setter private int viewsImagesBorderWidth = 10;
	
	/**
	 * Scale factor of view snapshots
	 */
	@Getter @Setter private int viewsImagesScaleFactor = 100;
	
	/**
	 * In case of Neo4J database, should we generate native relationships
	 */
	@Getter  @Setter private boolean neo4jNativeMode = false;
	
	/**
	 * In case of Neo4J database, should we empty the database before export
	 */
	@Setter private boolean shouldEmptyNeo4jDB = false;
	
	/**
     * In case of a Neo4J database
     * @return true if the plugin should empty the database before exporting the Archi components
     */
	public boolean shouldEmptyNeo4jDB() {
	    return this.shouldEmptyNeo4jDB;
	}
	
	/**
	 * Should we use typed relationships in Neo4j databases
	 */
	@Getter @Setter private boolean neo4jTypedRelationship = false;

	/**
	 * Should we use SSL Encryption in Neo4j databases
	 */
	@Getter @Setter private boolean neo4jSSLEncrypted = false;

	/**
	 * @param driver
	 * @return the default TCP port for a given driver 
	 */
	public static int getDefaultPort(String driver) {
		for ( DBDatabase database: DBDatabase.VALUES ) {
			if ( DBPlugin.areEqual(database.getDriverName(), driver.toLowerCase()) ) return database.getDefaultPort();
		}
		return 0;
	}

	/**
	 * @return if the schema is set, returns the schema name followed by a dot, directly usable in SQL requests
	 */
	public String getSchemaPrefix() {
		if ( this.schema.isEmpty() )
			return "";
		return this.schema + ".";
	}
	
	/*
	 * @return CQL if Neo4j database selected, or SQL in all other cases
	 * /
	public String getLanguage() {
		if ( DBPlugin.areEqual(this.driver, "neo4j") )
			return "CQL";
		return "SQL";
	}*/
	
    
    /**
     * Standard mode (in which the JDBC connection string is calculated from fields)
     * Expert mode (in which the JDBC connection string is provided by the user)
     */
    @Getter @Setter private boolean expertMode = false;
    
    /**
     * in Standard mode, the JDBC string is calculated from the individual strings
     * in Export mode, the JDBC string is provided as is
     */
    @Setter private String jdbcConnectionString;
    
    /**
     * in Standard mode, the JDBC string is calculated from the individual strings
     * in Export mode, the JDBC string is provided as is
     * @return the JDBC connection string, "" if the driver is unknown
     */
    public String getJdbcConnectionString() {
        if ( ! isExpertMode() ) {
            switch (this.driver) {
                case POSTGRESQL:
                    this.jdbcConnectionString = "jdbc:postgresql://" + this.getServer() + ":" + this.getPort() + "/" + this.getDatabase();
                    break;
                case MSSQL:
                    this.jdbcConnectionString = "jdbc:sqlserver://" + this.getServer() + ":" + this.getPort() + ";databaseName=" + this.getDatabase();
					try {
						if ( DBPlugin.isEmpty(this.getUsername()) && DBPlugin.isEmpty(this.getDecryptedPassword()) )
	                        this.jdbcConnectionString += ";integratedSecurity=true";
						// we enable SSL encryption
						this.jdbcConnectionString+=";encrypt=true;trustServerCertificate=true";
					} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
						DBGuiUtils.popup(Level.ERROR, "Database: "+this.database+"\n\nFailed to decrypt the password. Did you change your network configuration since passwords have been registered ?", e);
						this.jdbcConnectionString = "";
					}
                    break;
                case MYSQL:
                    this.jdbcConnectionString = "jdbc:mysql://" + this.getServer() + ":" + this.getPort() + "/" + this.getDatabase();
                    break;
                case NEO4J:
                	this.jdbcConnectionString = "jdbc:neo4j:bolt"+(this.isNeo4jSSLEncrypted()?"+s":"")+"://" + this.getServer() + ":" + this.getPort();
                    break;
                case ORACLE:
                    this.jdbcConnectionString = "jdbc:oracle:thin:@" + this.getServer() + ":" + this.getPort() + ":" + this.getDatabase();
                    break;
                case SQLITE:
                    this.jdbcConnectionString = "jdbc:sqlite:"+this.getServer();
                    break;
                default:
                	// just in case ...
                    this.jdbcConnectionString = "";
            }
        }
        return this.jdbcConnectionString;
    }
    
    /**
     * Calculates a JDBC connection string 
     * @param driverName
     * @param serverName
     * @param port
     * @param databaseName
     * @param username
     * @param password
     * @param ssl
     * @return the JDBC connection string
     * @throws SQLException if the JDBC driver is unknown
     */
    public static String getJdbcConnectionString(String driverName, String serverName, int port, String databaseName, String username, String password, boolean ssl) throws SQLException {
        String jdbcString = "";

        switch (driverName) {
            case "postgresql":
                jdbcString = "jdbc:postgresql://" + serverName + ":" + port + "/" + databaseName;
                break;
            case "ms-sql":
                jdbcString = "jdbc:sqlserver://" + serverName + ":" + port + ";databaseName=" + databaseName;
                if ( DBPlugin.isEmpty(username) && DBPlugin.isEmpty(password) )
                    jdbcString += ";integratedSecurity=true";
                break;
            case "mysql":
                jdbcString = "jdbc:mysql://" + serverName + ":" + port + "/" + databaseName;
                break;
            case "neo4j":
            	jdbcString = "jdbc:neo4j:bolt"+(ssl?"+s":"")+"://" + serverName + ":" + port;
                break;
            case "oracle":
                jdbcString = "jdbc:oracle:thin:@" + serverName + ":" + port + ":" + databaseName;
                break;
            case "sqlite":
                jdbcString = "jdbc:sqlite:"+serverName;
                break;
            default:
                throw new SQLException("Unknonwn driver " + driverName);        // just in case
        }

        return jdbcString;
    }
    
	/**
	 * Gets all the database entries from the preference store
	 * 
	 * @param includeNeo4j True if the Neo4J database must be included, false if the Neo4J databases must me excluded
	 * @return List of the database entries
	 */
	public static List<DBDatabaseEntry> getAllDatabasesFromPreferenceStore() {
		if ( logger.isDebugEnabled() ) logger.debug("Getting databases preferences from preference store");
		List<DBDatabaseEntry> databaseEntries = new ArrayList<>();     
		IPersistentPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		
		int lines = store.getInt(preferenceName);
		
		for (int line = 0; line <lines; line++) {
			DBDatabaseEntry databaseEntry = new DBDatabaseEntry();
			try {
				databaseEntry.setIndex(line);
				
				databaseEntry.setId(store.getString(preferenceName+"_id_"+line));
				if ( databaseEntry.getId() == null ) 
					databaseEntry.setId("");
				
				databaseEntry.setName(store.getString(preferenceName+"_name_"+line));
				
				if ( logger.isDebugEnabled() ) logger.debug("Getting database entry \""+databaseEntry.getName()+"\" from the preference store");
				
				Boolean isExpertMode = store.getBoolean(preferenceName+"_isExpertMode_"+line);
				
				databaseEntry.setExpertMode(isExpertMode);
				databaseEntry.setDriver(DBDatabaseDriver.fromName(store.getString(preferenceName+"_driver_"+line)));
				databaseEntry.setServer(store.getString(preferenceName+"_server_"+line));
				
				databaseEntry.setJdbcConnectionString(store.getString(preferenceName+"_jdbcConnectionString_"+line));

				databaseEntry.setNeo4jNativeMode(store.getBoolean(preferenceName+"_neo4j-native-mode_"+line));
				databaseEntry.setShouldEmptyNeo4jDB(store.getBoolean(preferenceName+"_neo4j-empty-database_"+line));
				databaseEntry.setNeo4jTypedRelationship(store.getBoolean(preferenceName+"_neo4j-typed-relationships_"+line));
				databaseEntry.setNeo4jSSLEncrypted(store.getBoolean(preferenceName+"_neo4j-ssl-encrypted_"+line));
				
				databaseEntry.setSchema(store.getString(preferenceName+"_schema_"+line));
				
				databaseEntry.setViewSnapshotRequired(store.getBoolean(preferenceName+"_export-views-images_"+line));
				databaseEntry.setViewsImagesBorderWidth(store.getInt(preferenceName+"_views-images-border-width_"+line));
				databaseEntry.setViewsImagesScaleFactor(store.getInt(preferenceName+"_views-images-scale-factor_"+line));
					
				databaseEntry.setPort(store.getInt(preferenceName+"_port_"+line));
				databaseEntry.setDatabase(store.getString(preferenceName+"_database_"+line));
				
				if ( !databaseEntry.getDriver().equals(DBDatabaseDriver.SQLITE) ) {
					databaseEntry.setUsername(store.getString(preferenceName+"_username_"+line));
						
					String password = store.getString(preferenceName+"_password_"+line);
					if ( DBPlugin.isEmpty(password) )
						databaseEntry.setEncryptedPassword(store.getString(preferenceName+"_encrypted_password_"+line));
					else {
						logger.debug("*********** Got plain text password ... replacing by encrypted one in preference store.");
						try {
							databaseEntry.setDecryptedPassword(password);
							store.setValue(preferenceName+"_encrypted_password_"+line, databaseEntry.getEncryptedPassword());
							store.setValue(preferenceName+"_password_"+line, "");
						} catch (InvalidKeyException|IllegalBlockSizeException|BadPaddingException|InvalidAlgorithmParameterException|NoSuchAlgorithmException|NoSuchPaddingException e) {
							DBGuiUtils.popup(Level.ERROR, "Failed to encrypt password for database entry \""+databaseEntry.getName()+"\".\n\nYour password will be left unencrypted in your preference store.", e);
						}
					}
				}

				databaseEntries.add(databaseEntry);
			} catch (Exception e) {
				DBGuiUtils.popup(Level.ERROR, "Failed to get database entry \""+databaseEntry.getName()+"\" from preference store.", e);
			}
		}
		return databaseEntries;
	}
	
	/**
	 * Persist the database entry in the preference store
	 * 
	 * @throws SQLException 
	 */
	private static void cleanupDatabaseEntriesFromPreferenceStore() {
		if ( logger.isDebugEnabled() ) logger.debug("Cleaning up database entries from the preference store");

		IPersistentPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		store.setValue(preferenceName, 0);
		
		// it is unlikely that user has got more than 100 configured databases
		for ( int i = -1; i < 100 ; ++i ) {
			String indexString = String.valueOf(i);
			
			store.setValue(DBDatabaseEntry.preferenceName + "_id_"							+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_name_"						+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_driver_"						+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_server_"						+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_port_"						+ indexString, 0);
			store.setValue(DBDatabaseEntry.preferenceName + "_database_"					+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_schema_"						+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_username_"					+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_password_"					+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_encrypted_password_"			+ indexString, "");
			store.setValue(DBDatabaseEntry.preferenceName + "_export-views-images_"			+ indexString, false);
			store.setValue(DBDatabaseEntry.preferenceName + "_views-images-border-width_"	+ indexString, 0);
			store.setValue(DBDatabaseEntry.preferenceName + "_views-images-scale-factor_"	+ indexString, 0);
			store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-native-mode_"			+ indexString, false);
			store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-empty-database_"		+ indexString, false);
			store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-typed-relationships_"	+ indexString, false);
			store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-ssl-encrypted_"	        + indexString, false);
			store.setValue(DBDatabaseEntry.preferenceName + "_isExpertMode_"				+ indexString, false);
			store.setValue(DBDatabaseEntry.preferenceName + "_jdbcConnectionString_"		+ indexString, "");
		}
	}
	
	/**
	 * Persist the database entry in the preference store
	 * 
	 * @throws SQLException 
	 */
	public void persistIntoPreferenceStore() {
		if ( logger.isDebugEnabled() ) logger.debug("Persisting database entry \""+getName()+"\" in the preference store");
		
		IPersistentPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		String indexString = String.valueOf(getIndex());
		boolean isNeo4j = getDriver().equals(DBDatabaseDriver.NEO4J);
		boolean isSqlite = getDriver().equals(DBDatabaseDriver.SQLITE);
		
		store.setValue(DBDatabaseEntry.preferenceName + "_id_" +                        indexString, getId());
		store.setValue(DBDatabaseEntry.preferenceName + "_name_" +                      indexString, getName());
		store.setValue(DBDatabaseEntry.preferenceName + "_driver_" +                    indexString, getDriver().getDriverName());
		store.setValue(DBDatabaseEntry.preferenceName + "_server_" +                    indexString, getServer());
		
		store.setValue(DBDatabaseEntry.preferenceName + "_isExpertMode_" +              indexString, isExpertMode());
		
		store.setValue(DBDatabaseEntry.preferenceName + "_jdbcConnectionString_" +      indexString, isExpertMode() ? getJdbcConnectionString() : "");
		
		store.setValue(DBDatabaseEntry.preferenceName + "_port_" +                      indexString, (isExpertMode() || isSqlite) ? 0     : getPort());
		store.setValue(DBDatabaseEntry.preferenceName + "_database_" +                  indexString, (isExpertMode() || isSqlite) ? ""    : getDatabase());

		store.setValue(DBDatabaseEntry.preferenceName + "_username_" +              	indexString, isSqlite ? ""    : getUsername());
		store.setValue(DBDatabaseEntry.preferenceName + "_encrypted_password_" +    	indexString, isSqlite ? ""    : getEncryptedPassword());
		store.setValue(DBDatabaseEntry.preferenceName + "_password_" +    				indexString, "");

		store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-native-mode_" +         indexString, isNeo4j ? isNeo4jNativeMode() : false);
		store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-empty-database_" +      indexString, isNeo4j ? shouldEmptyNeo4jDB() : false);
		store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-typed-relationships_" + indexString, isNeo4j ? isNeo4jTypedRelationship() : false);
		store.setValue(DBDatabaseEntry.preferenceName + "_neo4j-ssl-encrypted_" +       indexString, isNeo4j ? isNeo4jSSLEncrypted() : false);
		
		store.setValue(DBDatabaseEntry.preferenceName + "_schema_" +                    indexString, isNeo4j ? ""    : getSchema());
		store.setValue(DBDatabaseEntry.preferenceName + "_export-views-images_" +       indexString, isNeo4j ? false : isViewSnapshotRequired());
		store.setValue(DBDatabaseEntry.preferenceName + "_views-images-border-width_" + indexString, isNeo4j ? 0     : getViewsImagesBorderWidth());
		store.setValue(DBDatabaseEntry.preferenceName + "_views-images-scale-factor_" + indexString, isNeo4j ? 0     : getViewsImagesScaleFactor());
	}

	/**
	 * Persist all the database entries in the preference store
	 * 
	 * @param databaseEntries List of the database entries to persist in the preference store
	 * @throws SQLException 
	 */
	public static void persistDatabaseEntryListIntoPreferenceStore(List<DBDatabaseEntry> databaseEntries) {
		cleanupDatabaseEntriesFromPreferenceStore();
		
		if ( logger.isDebugEnabled() ) logger.debug("Persisting all database entries in the preference store");

		IPersistentPreferenceStore store = DBPlugin.INSTANCE.getPreferenceStore();
		
		int nbDatabases = databaseEntries.size();
		store.setValue(preferenceName, nbDatabases);

		for (int i = 0; i < nbDatabases; ++i) {
			DBDatabaseEntry databaseEntry = databaseEntries.get(i);
			databaseEntry.setIndex(i);		// just in case, we force the index value
			databaseEntry.persistIntoPreferenceStore();
		}
		
		try {
			store.save();
		} catch (IOException e) {
			DBGuiUtils.popup(Level.ERROR, "Failed to save your preferences.", e);
		}
	}

	/**
	 * Get a database entry from the database name
	 * 
	 * @param databaseName database name of the database entry
	 * @return The database entry corresponding to the database name
	 * @throws Exception
	 */
	public static DBDatabaseEntry getDBDatabaseEntryFromDatabaseName(String databaseName) throws Exception {
		List<DBDatabaseEntry> databaseEntries = getAllDatabasesFromPreferenceStore();

		for (DBDatabaseEntry databaseEntry : databaseEntries) {
			if ( DBPlugin.areEqual(databaseEntry.getName(), databaseName) ) {
				return databaseEntry;
			}
		}
		return null;
	}
	
	/**
	 * Get a database entry from the database ID
	 * 
	 * @param databaseId database ID of the database entry
	 * @return The database entry corresponding to the database ID
	 * @throws Exception
	 */
	public static DBDatabaseEntry getDBDatabaseEntryFromDatabaseID(String databaseId) throws Exception {
		List<DBDatabaseEntry> databaseEntries = getAllDatabasesFromPreferenceStore();

		for (DBDatabaseEntry databaseEntry : databaseEntries) {
			if ( DBPlugin.areEqual(databaseEntry.getId(), databaseId) ) {
				return databaseEntry;
			}
		}
		return null;
	}
	
	private static Cipher getCipher(int cipherMode) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
		String passwordEncryptionKey = null;	
		boolean errorMessageAlreadyPrinted = false;
		final String ciferTransformation = "Blowfish";
		
		// the default key is the MAC address of the first physical network interface
		try {
		    Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
		    while (nics.hasMoreElements()) {
		    	NetworkInterface net = nics.nextElement();
		        byte[] mac = net.getHardwareAddress();
			
				if ( mac != null ) {
					StringBuilder key = new StringBuilder();
		            for (int i = 0; i < mac.length; i++) {
		            	key.append(String.format("%x", mac[i]));
		                if (i < mac.length - 1)
		                	key.append(":");
		             }
		            
		            passwordEncryptionKey = key.toString();
		            break;
				}
		    }
		} catch (SocketException e) {
			logger.error("Failed to calculate encryption key, defaulting to default one.", e);
			errorMessageAlreadyPrinted = true;
		}
		
		if ( passwordEncryptionKey == null ) {
			passwordEncryptionKey = "VerySimpleKey.";		// default key in case we cannot calculate a unique one for the running machine
			if ( !errorMessageAlreadyPrinted )
				logger.error("Failed to calculate encryption key, defaulting to default one.");
		}
		
		int maxKeyLength = Cipher.getMaxAllowedKeyLength(ciferTransformation);
		if ( passwordEncryptionKey.length() > maxKeyLength )
			passwordEncryptionKey = passwordEncryptionKey.substring(0, maxKeyLength-1);
		
		int minKeyLength = 32;
		if ( passwordEncryptionKey.length() < minKeyLength ) {
			StringBuilder sb = new StringBuilder(passwordEncryptionKey);
			while ( sb.length() < minKeyLength )
				sb.append(passwordEncryptionKey.substring(0,Math.min(passwordEncryptionKey.length(),  minKeyLength-sb.length())));
			passwordEncryptionKey = sb.toString();
		} 

		SecretKeySpec secretKeySpec = new SecretKeySpec(passwordEncryptionKey.getBytes(), ciferTransformation);
		Cipher cipher = Cipher.getInstance(ciferTransformation);
		cipher.init(cipherMode, secretKeySpec, cipher.getParameters());
		
		return cipher;
	}
}
