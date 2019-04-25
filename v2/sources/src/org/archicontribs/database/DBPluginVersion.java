package org.archicontribs.database;

import lombok.Getter;

public class DBPluginVersion implements Comparable<DBPluginVersion> {
    @Getter private String version;
    
    public DBPluginVersion(String version) {
    	this.version = version;
    }

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