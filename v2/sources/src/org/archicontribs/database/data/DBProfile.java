/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Simple class to store a Profile
 * 
 * @author Herve Jouin
 */
@AllArgsConstructor
public class DBProfile implements java.lang.Comparable<DBProfile>
{
    @Getter @Setter private String name;
    @Getter @Setter private String conceptType;
    @Getter @Setter private boolean specialization;
    @Getter @Setter private String imagePath;

    
    @Override public int compareTo(DBProfile o) {
		int result = this.name.compareTo(o.getName());
		if ( result == 0 ) result = this.conceptType.compareTo(o.getConceptType());
		if ( result == 0 ) result = ((Boolean)this.specialization).compareTo(o.isSpecialization());
		if ( result == 0 ) result = this.getImagePath().compareTo(o.getImagePath());
		return result;
	}
}