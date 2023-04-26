/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.preferences;

import org.archicontribs.database.DBLogger;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * This class extends the FileFieldEditor. It allows invalid filenames.
 * 
 * @author Herve Jouin
 *
 */
public class DBFileFieldEditor extends FileFieldEditor {
	protected static final DBLogger logger = new DBLogger(DBFileFieldEditor.class);
	
	protected Color RED_COLOR = new Color(null, 255, 0 ,0);
	protected Color GREEN_COLOR = new Color(null, 0, 255 ,0);
	
	Color borderColor = null;
		
    /**
     * 
     */
    public DBFileFieldEditor() {
    	super();
    	setFileExtensions(new String[] {"*.log", "*.txt", "*.*"});
    	addColoredBorder(getTextControl());
    }
    
    /**
     * Creates a new file field editor
     * @param name
     * @param labelText
     * @param parent
     */
    public DBFileFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, false, parent);
        setFileExtensions(new String[] {"*.log", "*.txt", "*.*"});
        addColoredBorder(getTextControl());
    }
    
    /**
     * Creates a new file field editor
     * @param name
     * @param labelText
     * @param enforceAbsolute
     * @param parent
     */
    public DBFileFieldEditor(String name, String labelText, boolean enforceAbsolute, Composite parent) {
        super(name, labelText, enforceAbsolute, VALIDATE_ON_KEY_STROKE, parent);
        setFileExtensions(new String[] {"*.log", "*.txt", "*.*"});
        addColoredBorder(getTextControl());
    }
    
    /**
     * Creates a new file field editor
     * @param name
     * @param labelText
     * @param enforceAbsolute
     * @param validationStrategy
     * @param parent
     */
    public DBFileFieldEditor(String name, String labelText, boolean enforceAbsolute, int validationStrategy, Composite parent) {
    	super(name, labelText, enforceAbsolute, validationStrategy, parent);
    	setFileExtensions(new String[] {"*.log", "*.txt", "*.*"});
    	addColoredBorder(getTextControl());
    }
    
    @Override protected boolean checkState() {
    	boolean state = super.checkState();
    	DialogPage page = getPage();
    	Text text = getTextControl();
    	String path = text.getText();

    	
    	if ( state ) {
    		logger.debug("\""+path + "\" is a valid filename");
    		this.borderColor = this.GREEN_COLOR;
   			text.setToolTipText(null);
    	} else {
    		logger.debug("\""+path + "\" is an invalid filename");
    		this.borderColor = this.RED_COLOR;
    		if ( page != null ) 
    			text.setToolTipText(page.getErrorMessage());
    	}

    	// we force the redraw of the text control's parent to refresh the border color
    	getTextControl().getParent().redraw();
    	getTextControl().getParent().update();
        
    	// we always return true, else it may generate errors in Archi preferences
    	return true;
    }
    
    protected void addColoredBorder(Control cont){
        final Control control = cont;
        cont.getParent().addPaintListener(new PaintListener(){
            @Override public void paintControl(PaintEvent e){
            	if ( DBFileFieldEditor.this.borderColor != null ) {
	            	GC gc = e.gc;
	                gc.setBackground(DBFileFieldEditor.this.borderColor);
	                Rectangle rect = control.getBounds();
	                Rectangle rect1 = new Rectangle(rect.x - 2, rect.y - 2, rect.width + 4, rect.height + 4);
	                gc.setLineStyle(SWT.LINE_SOLID);
	                gc.fillRectangle(rect1);
                }
            }
        });
    }
}
