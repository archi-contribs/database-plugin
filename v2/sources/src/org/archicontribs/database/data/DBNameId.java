package org.archicontribs.database.data;

public class DBNameId {
    private String name;
    private String id;

    public DBNameId() {
        this.name = null;
        this.id = null;
    }
    
    /**
     * @param name
     * @param id
     */
    public DBNameId(String name, String id) {
        this.name = name;
        this.id = id;
    }
    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the id
     */
    public String getId() {
        return this.id;
    }
    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
    
    
}
