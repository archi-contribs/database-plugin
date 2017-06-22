/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.sql.ResultSet;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.archicontribs.database.model.ArchimateModel;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;

public class DBGuiComponentHistory extends DBGui {
	private static final DBLogger logger = new DBLogger(DBGuiComponentHistory.class);
	
	private IArchimateConcept selectedComponent = null;

	private Label lblVersions;
	
	private Button btnImportDatabaseVersion;
	private Button btnExportModelVersion;
	
	private Table tblContent;
	private Table tblVersions;
	
	/**
	 * Creates the GUI to export components and model
	 * @throws Exception 
	 */
	public DBGuiComponentHistory(IArchimateConcept component) throws Exception {
		super("Component history");
		selectedComponent = component;
		
		includeNeo4j = false;
		
		popup("Please wait while counting model's components");
		((ArchimateModel)selectedComponent.getArchimateModel()).countAllObjects();
		if ( logger.isDebugEnabled() ) logger.debug("the model has got "+((ArchimateModel)selectedComponent.getArchimateModel()).getAllElements().size()+" elements and "+((ArchimateModel)selectedComponent.getArchimateModel()).getAllRelationships().size()+" relationships.");
		closePopup();
		
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for showing history of "+((IDBMetadata)component).getDBMetadata().getDebugName()+" (plugin version "+DBPlugin.pluginVersion+").");		
		
		setCompoRight();
		compoRightBottom.setVisible(true);
		compoRightBottom.layout();
		
		
		createAction(ACTION.One, "Component history");
		setActiveAction(ACTION.One);

		getDatabases();
	}
	
	/**
	 * creates the composites where the user can check the components to export
	 */
	protected void setCompoRight() {
		Group grpComponents = new Group(compoRightBottom, SWT.NONE);
		grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
		grpComponents.setFont(GROUP_TITLE_FONT);
		grpComponents.setText("Component history : ");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100, 0);
		grpComponents.setLayoutData(fd);
		grpComponents.setLayout(new FormLayout());
		
		lblVersions = new Label(grpComponents, SWT.NONE);
		lblVersions.setBackground(GROUP_BACKGROUND_COLOR);
		lblVersions.setText("Versions :");		
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		lblVersions.setLayoutData(fd);
		
		tblVersions = new Table(grpComponents, SWT.BORDER | SWT.FULL_SELECTION);
		tblVersions.setHeaderVisible(true);
		tblVersions.setLinesVisible(true);
		tblVersions.addListener(SWT.Selection, new Listener() {
		    public void handleEvent(Event e) {
		        fillInCompareTable(tblContent, 0, selectedComponent, Integer.valueOf(tblVersions.getSelection()[0].getText(0)), null);
		    }
		});
		fd = new FormData();
		fd.top = new FormAttachment(lblVersions, 10);
		fd.left = new FormAttachment(20, 0);
		fd.right = new FormAttachment(80, 0);
		fd.bottom = new FormAttachment(30, 0);
		tblVersions.setLayoutData(fd);
		
		TableColumn colVersion = new TableColumn(tblVersions, SWT.NONE);
		colVersion.setWidth(47);
		colVersion.setText("Version");
		
		TableColumn colCreatedBy = new TableColumn(tblVersions, SWT.NONE);
		colCreatedBy.setWidth(121);
		colCreatedBy.setText("Created by");
		
		TableColumn colCreatedOn = new TableColumn(tblVersions, SWT.NONE);
		colCreatedOn.setWidth(145);
		colCreatedOn.setText("Created on");
		
		Label lblContent = new Label(grpComponents, SWT.NONE);
		lblContent.setBackground(GROUP_BACKGROUND_COLOR);
		lblContent.setText("Content :");
		fd = new FormData();
		fd.top = new FormAttachment(tblVersions, 20);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		lblContent.setLayoutData(fd);
		
		tblContent = new Table(grpComponents, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		tblContent.setHeaderVisible(true);
		tblContent.setLinesVisible(true);
		fd = new FormData();
		fd.top = new FormAttachment(lblContent, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -50);
		tblContent.setLayoutData(fd);
		
		TableColumn colItem = new TableColumn(tblContent, SWT.NONE);
		colItem.setWidth(100);
		colItem.setText("Items");
		
		TableColumn colYourVersion = new TableColumn(tblContent, SWT.NONE);
		colYourVersion.setWidth(150);
		colYourVersion.setText("Your version");
		
		TableColumn colDatabaseVersion = new TableColumn(tblContent, SWT.NONE);
		colDatabaseVersion.setWidth(150);
		colDatabaseVersion.setText("Database version");
		
		btnImportDatabaseVersion = new Button(grpComponents, SWT.NONE);
		btnImportDatabaseVersion.setImage(IMPORT_FROM_DATABASE_IMAGE);
		btnImportDatabaseVersion.setText("Import database version");
		btnImportDatabaseVersion.setEnabled(false);
		btnImportDatabaseVersion.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { /*importFromDatabase();*/ }		//TODO
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(tblContent, 10);
		fd.right = new FormAttachment(100, -10);
		btnImportDatabaseVersion.setLayoutData(fd);
		
		btnExportModelVersion = new Button(grpComponents, SWT.NONE);
		btnExportModelVersion.setImage(EXPORT_TO_DATABASE_IMAGE);
		btnExportModelVersion.setText("Export to the database");
		btnExportModelVersion.setEnabled(false);
		btnExportModelVersion.addSelectionListener(new SelectionListener() {
		    public void widgetSelected(SelectionEvent e) { /*exportToDatabase();*/ }      //TODO
		    public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(tblContent, 10);
		fd.right = new FormAttachment(btnImportDatabaseVersion, -10);
		btnExportModelVersion.setLayoutData(fd);
	}
	
	/**
	 * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheck) {	
		dialog.setCursor(CURSOR_ARROW);
			// if everything goes well, then we search for all the versions of the component
		if ( logger.isDebugEnabled() ) logger.debug("Searching for all versions of the component");
		try {
			tblVersions.removeAll();
			tblContent.removeAll();
			btnImportDatabaseVersion.setEnabled(false);
			
			String tableName = null;
			if ( selectedComponent instanceof IArchimateElement ) 
			    tableName = "elements";
			else if ( selectedComponent instanceof IArchimateRelationship ) 
                tableName = "relationships";
			else {
			    popup(Level.FATAL, "Cannot get history for components of class "+selectedComponent.getClass().getSimpleName());
			    return ;
			}
			    
			ResultSet result = connection.select("SELECT version, created_by, created_on FROM "+selectedDatabase.getSchemaPrefix()+tableName+" where id = ? ORDER BY version DESC", selectedComponent.getId());
				
			while ( result.next() ) {
			    TableItem tableItem = new TableItem(tblVersions, SWT.NULL);
			    tableItem.setText(0, String.valueOf(result.getInt("version")));
			    tableItem.setText(1, result.getString("created_by"));
			    tableItem.setText(2, result.getTimestamp("created_on").toString());
			}
		} catch (Exception err) {
		    tblVersions.removeAll();
			popup(Level.FATAL, "Failed to search component versions in the database.", err);
		}
		
		if ( tblVersions.getItemCount() > 1 ) {
		    lblVersions.setText(tblVersions.getItemCount()+" versions have been found in the database :");
		} else {
		    lblVersions.setText(tblVersions.getItemCount()+" version has been found in the database :");
		}
		
		if ( tblVersions.getItemCount() != 0 ) {
			tblVersions.select(0);
			tblVersions.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	protected void notConnectedToDatabase() {
	    lblVersions.setText("");
	    tblContent.removeAll();
	    tblVersions.removeAll();
	}
}
