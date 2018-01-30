/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * This class holds the table name, version, checksum and a timestamp
 * 
 * @author Herve Jouin
 */
public class DBVersion {
	private String id;
    private int version;
    private String checksum;
    private Timestamp timestamp;
    private int latestVersion;
    private String latestChecksum;
    private Timestamp latestTimestamp;
    
    public static Timestamp NEVER = Timestamp.from(Instant.EPOCH);
    
    public DBVersion(String id, int version, String checksum, Timestamp timestamp, int latestVersion, String latestChecksum, Timestamp latestTimestamp) {
    	setId(id);
        setVersion(version);
        setChecksum(checksum);
        setTimestamp(timestamp);
        setLatestVersion(latestVersion);
        setLatestChecksum(latestChecksum);
        setLatestTimestamp(latestTimestamp);
    }
    
    public DBVersion() {
    	this(null, 0, null, null,0, null, null);
    }
    
    public DBVersion(Timestamp timestamp) {
    	this(null, 0, null, timestamp, 0, null, null);
    }
    
    public void reset() {
    	setId(null);
        setVersion(0);
        setChecksum(null);
        setTimestamp(null);
        setLatestVersion(0);
        setLatestChecksum(null);
        setLatestTimestamp(null);
    }
    
    /**
     * @return the table name where the component can be imported/exported
     */
    public String getId() {
        return id;
    }

    /**
     * @param table name where the component can be imported/exported
     */
    public void setId(String id) {
        this.id = (id==null ? "" : id);
    }

    /**
     * @return the version of the component
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param the version of the component
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return the checksum of the component
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @param the checksum of the component
     */
    public void setChecksum(String checksum) {
        this.checksum = (checksum==null ? "" : checksum);
    }
    
    /**
     * @return the timestamp of the component
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * @param the timestamp of the component
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = (timestamp==null ? NEVER : timestamp);
    }
    
    /**
     * @return the latest version of the component
     */
    public int getLatestVersion() {
        return latestVersion;
    }

    /**
     * @param the latest version of the component
     */
    public void setLatestVersion(int version) {
        this.latestVersion = version;
    }

    /**
     * @return the latest checksum of the component
     */
    public String getLatestChecksum() {
        return latestChecksum;
    }

    /**
     * @param the latest checksum of the component
     */
    public void setLatestChecksum(String checksum) {
        this.latestChecksum = (checksum==null ? "" : checksum);
    }
    
    /**
     * @return the latest timestamp of the component
     */
    public Timestamp getLatestTimestamp() {
        return latestTimestamp;
    }

    /**
     * @param the latest timestamp of the component
     */
    public void setLatestTimestamp(Timestamp timestamp) {
        this.latestTimestamp = (timestamp==null ? NEVER : timestamp);
    }
}
