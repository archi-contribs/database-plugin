package org.archicontribs.database;

import lombok.Getter;

/**
 * Holds and manipulates a version under the form x.y.z[.t...] where x, y, z, t are integers. 
 * @author Herve Jouin *
 */
public class DBPluginVersion implements Comparable<DBPluginVersion> {
    @Getter private String version;
    
    /**
     * Initialize a {@link DBPluginVersion}
     * @param v value of the version
     */
    public DBPluginVersion(String v) {
    	this.version = v;
    }

    /**
     * Compares the current version to a new one
     * @param versionToCompare the version to compare to
     * @return 0 if identical<br>1 if current version is greater<br>-1 if current version is lower
     */
    @Override public int compareTo(DBPluginVersion versionToCompare) {
        if( versionToCompare == null )
            return 1;
        
        String[] subVersions1 = this.version.split("\\.");
        String[] subVersions2 = versionToCompare.getVersion().split("\\.");
        
        int length = Math.max(subVersions1.length, subVersions2.length);
        for(int i = 0; i < length; i++) {
            int v1 = (subVersions1.length > i) ? Integer.parseInt(subVersions1[i]) : 0;
            int v2 = (subVersions2.length > i) ? Integer.parseInt(subVersions2[i]) : 0;
            if(v1 < v2)
                return -1;
            if(v1 > v2)
                return 1;
        }
        return 0;
    }

    /**
     * Compares the current version to a new one
     * @param versionToCompare the version to compare to
     * @return true if identical, false if different
     */
    @Override public boolean equals(Object versionToCompare) {
        if(this == versionToCompare)
            return true;
        try {
        	return this.compareTo((DBPluginVersion) versionToCompare) == 0;
        } catch (@SuppressWarnings("unused") ClassCastException ign) {
        	return false;
        }
    }

	@Override public int hashCode() {
		return super.hashCode();
	}
}