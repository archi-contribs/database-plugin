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
    
    public DBBendpoint(int startX, int startY, int endX, int endY) {
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
    }
}