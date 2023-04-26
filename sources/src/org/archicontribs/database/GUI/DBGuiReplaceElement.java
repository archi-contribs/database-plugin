/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import com.archimatetool.model.IArchimateElement;

/**
 * This class manages the GUI that allows to replace a component by another one
 * 
 * @author Herve Jouin
 */
public class DBGuiReplaceElement extends DBGuiImportComponents {
	@SuppressWarnings("hiding")
    protected static final DBLogger logger = new DBLogger(DBGuiReplaceElement.class);
	
	IArchimateElement selectedElement; 

	public DBGuiReplaceElement(DBArchimateModel model, IArchimateElement element, String title) throws Exception {
	   super(model, null, null, title);
	   
	   this.selectedElement = element;
	   
	   // we ensure that the element tab is selected and the only one shown
	   
	   
	   // we replace the "import" button by a "replace" button
       setBtnAction("Replace", new SelectionListener() {
           @Override
           public void widgetSelected(SelectionEvent event) {
               DBGuiReplaceElement.this.btnDoAction.setEnabled(false);
               try {
                   doReplace();
               } catch (Exception err) {
                   DBGuiUtils.popup(Level.ERROR, "An exception has been raised during import.", err);
               }
           }
           @Override
           public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
       });
	}
	
	void doReplace() {
	    logger.info("Replacing "+DBMetadata.getDBMetadata(this.selectedElement).getDebugName());
	}
}
