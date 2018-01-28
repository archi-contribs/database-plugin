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
	public enum Type {element, relationship, folder, view };
	private Type type;
    private int version = 0;
    private String checksum = null;
    
    public DBVersion(Type type, int version, String checksum) {
    	this.type = type;
        this.version = version;
        this.checksum = checksum;
    }
    
    /**
     * @return the type of the component
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type of the component to set
     */
    public void setVersion(Type type) {
        this.type = type;
    }

    /**
     * @return the version of the component
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version of the component
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
     * @param checksum of the component
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    
}
