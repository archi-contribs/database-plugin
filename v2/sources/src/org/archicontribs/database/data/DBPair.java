/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.data;

/**
 * Simple class to store a pair of key/value
 * 
 * @author Herve Jouin
 *
 * @param <K> class of the key
 * @param <V> class of the value
 */
public class DBPair<K, V>
{
    private K key;
    private V value;

    public DBPair(K key, V value)
    {
        this.key = key;
        this.value = value;
    }

    public K getKey()
    {
        return this.key;
    }

    public V getValue()
    {
        return this.value;
    }

    public K setKey(K key)
    {
        return this.key = key;
    }

    public V setValue(V value)
    {
        return this.value = value;
    }
}