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
 * Simple class to store a pair of key/value
 * 
 * @author Herve Jouin
 */
@AllArgsConstructor
public class DBProperty implements java.lang.Comparable<DBProperty>
{
    @Getter @Setter private String key;
    @Getter @Setter private String value;

    @Override public int compareTo(DBProperty o) {
		int result = this.key.compareTo(o.getKey());
		if ( result == 0 ) result = this.value.compareTo(o.getValue());
		return result;
	}
}