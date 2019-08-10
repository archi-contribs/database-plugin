/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple class to store a bendpoint
 * 
 * @author Herve Jouin
 */
public class DBBendpoint
{
    @Getter @Setter private int startX;
    @Getter @Setter private int startY;
    @Getter @Setter private int endX;
    @Getter @Setter private int endY;
    
    public DBBendpoint(int theStartX, int theStartY, int theEndX, int theEndY) {
        this.startX = theStartX;
        this.endX = theEndX;
        this.startY = theStartY;
        this.endY = theEndY;
    }
}