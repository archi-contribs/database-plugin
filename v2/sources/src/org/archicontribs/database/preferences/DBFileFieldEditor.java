/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.preferences;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * This class extends the FileFieldEditor. It allows invalid filenames.
 * 
 * @author Herve Jouin
 *
 */
public class DBFileFieldEditor extends FileFieldEditor {
    public DBFileFieldEditor() {
    	super();
    }
    
    public DBFileFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, false, parent);
    }
    
    public DBFileFieldEditor(String name, String labelText, boolean enforceAbsolute, Composite parent) {
        super(name, labelText, enforceAbsolute, VALIDATE_ON_FOCUS_LOST, parent);
    }
    
    public DBFileFieldEditor(String name, String labelText, boolean enforceAbsolute, int validationStrategy, Composite parent) {
    	super(name, labelText, enforceAbsolute, validationStrategy, parent);
    }
    
    @Override
	protected String changePressed() {
        return getTextControl().getText();
    }
    
    @Override
	protected boolean checkState() {
    	return true;
    }
}
