/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.data;

import org.eclipse.draw2d.geometry.Rectangle;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple class to store a view screenshot
 * 
 * @author Herve Jouin
 */
public class DBScreenshot {
	@Setter private Integer scaleFactor;
	@Setter private Integer borderWidth;
	@Setter private Rectangle bounds;
	@Setter private byte[] screenshotBytes;
	
	@Getter @Setter private boolean screenshotActive = true;
	
	public DBScreenshot() {
		this.screenshotBytes = null;
		this.scaleFactor = null;
		this.borderWidth = null;
		this.bounds = null;
		this.screenshotActive = false;
	}
	
	public DBScreenshot(byte[] screenshot, int scale, int margin) {
		this.screenshotBytes = screenshot;
		this.scaleFactor = scale;
		this.borderWidth = margin;
		this.bounds = null;
		this.screenshotActive = true;
	}

	public byte[] getBytes() {
		return this.screenshotActive ? this.screenshotBytes : null;
	}
	
	public Integer getScaleFactor() {
		return this.screenshotActive ? this.scaleFactor : null;
	}
	
	public Integer getX() {
		return this.screenshotActive ? this.bounds.x : null;
	}
	public Integer getY() {
		return this.screenshotActive ? this.bounds.y : null;
	}
	
	public Integer getWidth() {
		return this.screenshotActive ? this.bounds.width : null;
	}
	
	public Integer getHeight() {
		return this.screenshotActive ? this.bounds.height : null;
	}
	
	public Integer getBodrderWidth() {
		return this.screenshotActive ? this.borderWidth : null;
	}
	
	public void dispose() {
		this.screenshotBytes = null;
		this.scaleFactor = null;
		this.borderWidth = null;
		this.bounds = null;
		this.screenshotActive = false;
	}
}