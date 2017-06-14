/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;


/**
 * The BDException class is an Exception class that is thrown each time there is an impossibility to import or export a component to the datatabase
 * 
 * @author Herve jouin
 */
public class DBException extends Exception {
    private static final long serialVersionUID = 1L;

    public DBException (String message) {
        super (message);
    }

    public DBException (Throwable cause) {
        super (cause);
    }

    public DBException (String message, Throwable cause) {
        super (message, cause);
    }
}
