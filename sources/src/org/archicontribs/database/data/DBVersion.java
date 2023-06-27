/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.data;

import java.sql.Timestamp;
import java.time.Instant;

import lombok.Getter;

/**
 * This class holds a version, a checksum and a timestamp
 * 
 * @author Herve Jouin
 */
@SuppressWarnings("hiding")
public class DBVersion {
    /**
     * value used to represent the "NEVER" concept
     */
    public static final Timestamp NEVER = Timestamp.from(Instant.EPOCH);
    
    @Getter private int version;
    
    /**
     * Initialize the {@link DBVersion} with another DBVersion that will be copied
     * @param versionToCopy
     */
    public DBVersion(DBVersion versionToCopy) {
        set(versionToCopy);
    }
    
    /**
     * Initialize the {@link DBVersion} with another DBVersion that will be copied
     * @param versionToCopy
     */
	public DBVersion(int version, String containerChecksum, String checksum, Timestamp timestamp, String username) {
        set(version, containerChecksum, checksum, timestamp, username);
    }
    
    public DBVersion(int version, String checksum, Timestamp timestamp, String username) {
        set(version, null, checksum, timestamp, username);
    }
    
    public DBVersion() {
    	this(0, null, null, null, null);
    }
    
    public DBVersion(Timestamp timestamp) {
    	this(0, null, null, timestamp, null);
    }
    
    public DBVersion(int version) {
    	this(version, null, null, null, null);
    }
    
    public void reset() {
        setVersion(0);
        setContainerChecksum(null);
        setChecksum(null);
        setTimestamp(null);
        setUsername(null);
    }
    
    public void set(DBVersion version) {
        if  (version == null ) {
	        setVersion(0);
	        setContainerChecksum(null);
	        setChecksum(null);
	        setTimestamp(null);
	        setUsername(null);
        } else {
	        setVersion(version.getVersion());
	        setContainerChecksum(version.getContainerChecksum());
	        setChecksum(version.getChecksum());
	        setTimestamp(version.getTimestamp());
	        setUsername(version.getUsername());
        }
    }
    
    public void set(int version, String checksum, Timestamp timestamp, String username) {
        setVersion(version);
        setContainerChecksum(null);
        setChecksum(checksum);
        setTimestamp(timestamp);
        setUsername(username);
    }
    
    public void set(int version, String containerChecksum, String checksum, Timestamp timestamp, String username) {
        setVersion(version);
        setContainerChecksum(containerChecksum);
        setChecksum(checksum);
        setTimestamp(timestamp);
        setUsername(username);
    }
    
    public void setVersion(int version) {
        this.version = (version<0 ? 0 : version);
    }
    
    /**
     * For containers, the checksum calculation must take the content in account 
     */
    @Getter private String checksum;
    /**
     * For containers, the checksum calculation must take the content in account 
     */
    public void setChecksum(String checksum) {
        this.checksum = (checksum==null ? "" : checksum);
    }
    
    /**
     * For containers, the checksum calculation must <b>not</b> take the content in account 
     */
    @Getter private String containerChecksum;
    /**
     * For containers, the checksum calculation must <b>not</b> take the content in account 
     */
    public void setContainerChecksum(String checksum) {
        this.containerChecksum = (checksum==null ? "" : checksum);
    }

    @Getter private Timestamp timestamp;
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = (timestamp==null ? NEVER : timestamp);
    }
    
    private String username;
    public String getUsername() {
    	if (this.username == null)
    		return System.getProperty("user.name");
    	return this.username;
    }
    
	public void setUsername(String name) {
		this.username = name;
	}
}
