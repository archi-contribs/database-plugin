/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.data;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * This class holds a version, a checksum and a timestamp
 * 
 * @author Herve Jouin
 */
public class DBVersion {
    private int version;
    private String checksum;
    private Timestamp timestamp;
    
    public static Timestamp NEVER = Timestamp.from(Instant.EPOCH);
    
    public DBVersion(int version, String checksum, Timestamp timestamp) {
        setVersion(version);
        setChecksum(checksum);
        setTimestamp(timestamp);
    }
    
    public DBVersion() {
    	this(0, null, null);
    }
    
    public DBVersion(Timestamp timestamp) {
    	this(0, null, timestamp);
    }
    
    public DBVersion(int version) {
    	this(version, null, null);
    }
    
    public void reset() {
        setVersion(0);
        setChecksum(null);
        setTimestamp(null);
    }
    
    /**
     * @return the version
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * @param the version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return this.checksum;
    }

    /**
     * @param the checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = (checksum==null ? "" : checksum);
    }
    
    /**
     * @return the timestamp
     */
    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    /**
     * @param the timestamp
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = (timestamp==null ? NEVER : timestamp);
    }
}
