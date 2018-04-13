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
public class DBVersion {
    public static Timestamp NEVER = Timestamp.from(Instant.EPOCH);
    
    public DBVersion(int version, String checksum, Timestamp timestamp) {
        setVersion(version);
        setContainerChecksum(null);
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
        setContainerChecksum(null);
        setChecksum(null);
        setTimestamp(null);
    }
    
    @Getter private int version;
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
     * For containers, the checksum calculation must take the content in account 
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
}
