/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.data;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * This class holds a pair of DBVersion : one current, one latest.
 * 
 * @author Herve Jouin
 */
public class DBVersionPair {
    private DBVersion currentVersion;
    private DBVersion latestVersion;
    
    public static Timestamp NEVER = Timestamp.from(Instant.EPOCH);
    
    public DBVersionPair(int version, String checksum, Timestamp timestamp, int latestVersion, String latestChecksum, Timestamp latestTimestamp) {
        this.currentVersion= new DBVersion(version, checksum, timestamp);
        this.latestVersion = new DBVersion(latestVersion, latestChecksum, latestTimestamp);
    }
    
    public DBVersionPair() {
        this.currentVersion= new DBVersion();
        this.latestVersion = new DBVersion();
    }
    
    public DBVersionPair(Timestamp timestamp) {
        this.currentVersion= new DBVersion(timestamp);
        this.latestVersion = new DBVersion();
    }
    
    public DBVersionPair(int version) {
        this.currentVersion= new DBVersion(version);
        this.latestVersion = new DBVersion();
    }
    
    public void reset() {
        this.currentVersion= new DBVersion();
        this.latestVersion = new DBVersion();
    }
    
    /**
     * @return the version of the component
     */
    public int getCurrentVersion() {
        return this.currentVersion.getVersion();
    }

    /**
     * @param the version of the component
     */
    public void setCurrentVersion(int version) {
        this.currentVersion.setVersion(version);
    }

    /**
     * @return the checksum of the component
     */
    public String getCurrentChecksum() {
        return this.currentVersion.getChecksum();
    }

    /**
     * @param the checksum of the component
     */
    public void setCurrentChecksum(String checksum) {
        this.currentVersion.setChecksum(checksum);
    }
    
    /**
     * @return the timestamp of the component
     */
    public Timestamp getCurrentTimestamp() {
        return this.currentVersion.getTimestamp();
    }

    /**
     * @param the timestamp of the component
     */
    public void setCurrentTimestamp(Timestamp timestamp) {
        this.currentVersion.setTimestamp(timestamp);
    }
    
    /**
     * @return the latest version of the component
     */
    public int getLatestVersion() {
        return this.latestVersion.getVersion();
    }

    /**
     * @param the latest version of the component
     */
    public void setLatestVersion(int version) {
        this.latestVersion.setVersion(version);
    }

    /**
     * @return the latest checksum of the component
     */
    public String getLatestChecksum() {
        return this.latestVersion.getChecksum();
    }

    /**
     * @param the latest checksum of the component
     */
    public void setLatestChecksum(String checksum) {
        this.latestVersion.setChecksum(checksum);
    }
    
    /**
     * @return the latest timestamp of the component
     */
    public Timestamp getLatestTimestamp() {
        return this.latestVersion.getTimestamp();
    }

    /**
     * @param the latest timestamp of the component
     */
    public void setLatestTimestamp(Timestamp timestamp) {
        this.latestVersion.setTimestamp(timestamp);
    }
}
