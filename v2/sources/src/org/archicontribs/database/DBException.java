/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;


/**
 * The BDException class is an Exception class that is thrown each time there is an impossibility to import or export a component to the datatabase
 * 
 * @author Herve Jouin
 */
public class DBException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception
     * @param message describes the error that is raised
     */
    public DBException (String message) {
        super (message);
    }

    /**
     * Creates a new exception
     * @param cause describes the primary exception that causes this exception
     */
    public DBException (Throwable cause) {
        super (cause);
    }

    /**
     * Creates a new exception
     * @param message describes the error that is raised
     * @param cause describes the primary exception that causes this exception
     */
    public DBException (String message, Throwable cause) {
        super (message, cause);
    }
}
