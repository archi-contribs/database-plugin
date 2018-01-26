/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

/**
 * This class holds the version and checksum for a component ID
 * 
 * @author Herve Jouin
 */
public class DBVersion {
    private int version = 0;
    private String checksum = null;
    
    public DBVersion() {
    }
    
    public DBVersion(int version, String checksum) {
        this.version = version;
        this.checksum = checksum;
    }

    /**
     * @return the version of the component
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version the version to set
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
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    
}
