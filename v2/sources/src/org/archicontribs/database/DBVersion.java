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
	private String tableName;
    private int version = 0;
    private String checksum = null;
    
    public DBVersion(String tableName, int version, String checksum) {
    	this.tableName = tableName;
        this.version = version;
        this.checksum = checksum;
    }
    
    /**
     * @return the table name where the component can be imported/exported
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @param table name where the component can be imported/exported
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
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
        this.checksum = checksum;
    }
    
    
}
