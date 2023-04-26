/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple class to store a pair of key/value
 * 
 * @author Herve Jouin
 */
public class DBProperty
{
    @Getter @Setter private String key;
    @Getter @Setter private String value;

    /**
     * Initialize the {@link DBProperty} with the provided key and value
     * @param theKey
     * @param theValue
     */
    public DBProperty(String theKey, String theValue)
    {
        this.key = theKey;
        this.value = theValue;
    }
}