/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseExportConnection;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.connection.DBSelect;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.model.commands.DBImportElementFromIdCommand;
import org.archicontribs.database.model.commands.DBImportFolderFromIdCommand;
import org.archicontribs.database.model.commands.DBImportRelationshipFromIdCommand;
import org.archicontribs.database.model.commands.DBImportViewFromIdCommand;
import org.archicontribs.database.model.commands.IDBImportCommand;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.impl.ArchimateModel;

/**
 * This class manages the GUI that shows a component history
 * 
 * @author Herve Jouin
 */
public class DBGuiComponentHistory extends DBGui {
	@SuppressWarnings("hiding")
	private static final DBLogger logger = new DBLogger(DBGuiComponentHistory.class);
	
	IArchimateModelObject selectedComponent = null;

	private Label lblVersions;
	
	Button btnImportDatabaseVersion;
	Button btnExportModelVersion;
	Label lblCompareComponents;
	
	Tree tblContent;
	Table tblVersions;
	
	/**
	 * Creates the GUI to show the differences between a component in the model and the database
	 * @param component 
	 * @throws Exception 
	 */
	public DBGuiComponentHistory(IArchimateModelObject component) throws Exception {
		super("Component history");
		this.selectedComponent = component;
		
		this.includeNeo4j = false;
		
		if ( logger.isDebugEnabled() ) logger.debug("Setting up GUI for showing history of "+DBMetadata.getDBMetadata(component).getDebugName()+" (plugin version "+DBPlugin.pluginVersion.toString()+").");		
		
		// we calculate the checksum of the component
		if ( component instanceof ArchimateModel )
			((DBArchimateModel)this.selectedComponent).getCurrentVersion().setChecksum(DBChecksum.calculateChecksum(this.selectedComponent));
		else
			((DBArchimateModel)this.selectedComponent.getArchimateModel()).countObject(component, true);

		
		setCompoRight();
		this.compoRightBottom.setVisible(true);
		this.compoRightBottom.layout();
		
		
		createAction(ACTION.One, "Component history");
		setActiveAction(ACTION.One);

		getDatabases(false);
	}
	
	/**
	 * creates the composites where the user can check the components to export
	 */
	protected void setCompoRight() {
		Group grpComponents = new Group(this.compoRightBottom, SWT.NONE);
		grpComponents.setBackground(GROUP_BACKGROUND_COLOR);
		grpComponents.setFont(GROUP_TITLE_FONT);
		grpComponents.setText("History of "+this.selectedComponent.getClass().getSimpleName() + "\"" + this.selectedComponent.getName() + "\"");
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100, 0);
		grpComponents.setLayoutData(fd);
		grpComponents.setLayout(new FormLayout());
		
		this.lblVersions = new Label(grpComponents, SWT.NONE);
		this.lblVersions.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblVersions.setText("Versions:");		
		fd = new FormData();
		fd.top = new FormAttachment(0, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		this.lblVersions.setLayoutData(fd);
		
		this.tblVersions = new Table(grpComponents, SWT.BORDER | SWT.FULL_SELECTION);
		this.tblVersions.setBackground(TABLE_BACKGROUND_COLOR);
		this.tblVersions.setHeaderVisible(true);
		this.tblVersions.setLinesVisible(true);
		this.tblVersions.addListener(SWT.Selection, new Listener() {
		    @Override
            public void handleEvent(Event e) {
		        Boolean areIdentical = fillInCompareTable(DBGuiComponentHistory.this.tblContent, DBGuiComponentHistory.this.selectedComponent, Integer.valueOf(DBGuiComponentHistory.this.tblVersions.getSelection()[0].getText(0)));
		        if ( areIdentical == null )
		        	DBGuiComponentHistory.this.lblCompareComponents.setText("Versions:");
		        else {
		        	if ( areIdentical.booleanValue() )
		        		DBGuiComponentHistory.this.lblCompareComponents.setText("Versions are identical");
		        	else
		        		DBGuiComponentHistory.this.lblCompareComponents.setText("Versions are different (check highlighted lines):");
		        	
		        	// the export button is activated if the component is different from the latest version in the database
		        	//    so the latest database version must be selected to activate the export button
		        	DBGuiComponentHistory.this.btnExportModelVersion.setEnabled(!areIdentical.booleanValue() && DBGuiComponentHistory.this.tblVersions.getSelectionIndex() == 0);
		        	
		        	// the import button is activated if the component is different from the selected version of the database
		        	DBGuiComponentHistory.this.btnImportDatabaseVersion.setEnabled(!areIdentical.booleanValue());
		        }
		    }
		});
		fd = new FormData();
		fd.top = new FormAttachment(this.lblVersions, 10);
		fd.left = new FormAttachment(20, 0);
		fd.right = new FormAttachment(80, 0);
		fd.bottom = new FormAttachment(30, 0);
		this.tblVersions.setLayoutData(fd);
		
		TableColumn colVersion = new TableColumn(this.tblVersions, SWT.NONE);
		colVersion.setWidth(47);
		colVersion.setText("Version");
		
		TableColumn colCreatedBy = new TableColumn(this.tblVersions, SWT.NONE);
		colCreatedBy.setWidth(121);
		colCreatedBy.setText("Created by");
		
		TableColumn colCreatedOn = new TableColumn(this.tblVersions, SWT.NONE);
		colCreatedOn.setWidth(145);
		colCreatedOn.setText("Created on");
		
		this.lblCompareComponents = new Label(grpComponents, SWT.NONE);
		this.lblCompareComponents.setBackground(GROUP_BACKGROUND_COLOR);
		this.lblCompareComponents.setText("Versions:");
		fd = new FormData();
		fd.top = new FormAttachment(this.tblVersions, 20);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		this.lblCompareComponents.setLayoutData(fd);
		
		this.tblContent = new Tree(grpComponents, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		this.tblContent.setBackground(TABLE_BACKGROUND_COLOR);
		this.tblContent.setHeaderVisible(true);
		this.tblContent.setLinesVisible(true);
		fd = new FormData();
		fd.top = new FormAttachment(this.lblCompareComponents, 10);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(100, -10);
		fd.bottom = new FormAttachment(100, -50);
		this.tblContent.setLayoutData(fd);
		
		TreeColumn colItem = new TreeColumn(this.tblContent, SWT.NONE);
		colItem.setWidth(120);
		colItem.setText("Items");
		
		TreeColumn colYourVersion = new TreeColumn(this.tblContent, SWT.NONE);
		colYourVersion.setWidth(220);
		colYourVersion.setText("Your version");
		
		TreeColumn colDatabaseVersion = new TreeColumn(this.tblContent, SWT.NONE);
		colDatabaseVersion.setWidth(220);
		colDatabaseVersion.setText("Database version");
		
		this.btnImportDatabaseVersion = new Button(grpComponents, SWT.NONE);
		this.btnImportDatabaseVersion.setImage(IMPORT_FROM_DATABASE_IMAGE);
		this.btnImportDatabaseVersion.setText("Import database version");
		this.btnImportDatabaseVersion.setEnabled(false);
		this.btnImportDatabaseVersion.addSelectionListener(new SelectionListener() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				try (DBDatabaseImportConnection importConnection = new DBDatabaseImportConnection(getDatabaseConnection())) {
					IArchimateModelObject importedComponent = DBGuiComponentHistory.this.selectedComponent;
					DBArchimateModel importedModel = (DBArchimateModel)importedComponent.getArchimateModel();
					IFolder parentFolder = (IFolder)importedComponent.eContainer();
					String id = importedComponent.getId();
					int version = Integer.valueOf(DBGuiComponentHistory.this.tblVersions.getSelection()[0].getText(0)).intValue();
					IDBImportCommand command = null;
					
					if ( importedComponent instanceof IArchimateElement )
						command = new DBImportElementFromIdCommand(importConnection, importedModel, null, null, parentFolder, id, version, DBImportMode.forceSharedMode, true); 
					else if ( importedComponent instanceof IArchimateRelationship )
						command = new DBImportRelationshipFromIdCommand(importConnection, importedModel, null, null, parentFolder, id, version, DBImportMode.forceSharedMode);
					else if ( importedComponent instanceof IFolder )
						command = new DBImportFolderFromIdCommand(importConnection, importedModel, null, parentFolder, id, version, DBImportMode.forceSharedMode);
					else if ( importedComponent instanceof IArchimateDiagramModel || importedComponent instanceof ICanvasModel || importedComponent instanceof ISketchModel )
						command = new DBImportViewFromIdCommand(importConnection, importedModel, null, parentFolder, id, version, DBImportMode.forceSharedMode, true);
					else
					    throw new Exception("Cannot import components of class "+importedComponent.getClass().getSimpleName());

					if ( command.getException() != null )
						throw command.getException();
					command.execute();
					if ( command.getException() != null )
						throw command.getException();
					((CommandStack)importedModel.getAdapter(CommandStack.class)).execute((Command) command);
					
					DBGuiUtils.popup(Level.INFO, "The current version of the component has been replaced by the selected version from the database.");
					
					connectedToDatabase(true);
					
				} catch (Exception err) {
					DBGuiUtils.popup(Level.ERROR, "Failed to import component.", err);
				}
			}
			@Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(this.tblContent, 10);
		fd.right = new FormAttachment(100, -10);
		this.btnImportDatabaseVersion.setLayoutData(fd);
		
		this.btnExportModelVersion = new Button(grpComponents, SWT.NONE);
		this.btnExportModelVersion.setImage(EXPORT_TO_DATABASE_IMAGE);
		this.btnExportModelVersion.setText("Export your version to the database");
		this.btnExportModelVersion.setEnabled(false);
		this.btnExportModelVersion.addSelectionListener(new SelectionListener() {
		    @Override
            public void widgetSelected(SelectionEvent e) {
		    	try (DBDatabaseExportConnection exportConnection = new DBDatabaseExportConnection(getDatabaseConnection())) {
		    		((DBArchimateModel)DBGuiComponentHistory.this.selectedComponent.getArchimateModel()).getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
					exportConnection.exportEObject(DBGuiComponentHistory.this.selectedComponent);
					
					DBGuiUtils.popup(Level.INFO, "The component has been updated in the database.");
					connectedToDatabase(true);
				} catch (Exception err) {
					DBGuiUtils.popup(Level.ERROR, "Failed to export component.", err);
				}
		    }
		    @Override
            public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(this.tblContent, 10);
		fd.right = new FormAttachment(this.btnImportDatabaseVersion, -10);
		this.btnExportModelVersion.setLayoutData(fd);
	}
	
	/**
	 * Called when a database is selected in the comboDatabases and that the connection to this database succeeded.<br>
	 */
	@Override
	protected void connectedToDatabase(boolean forceCheck) {	
		this.dialog.setCursor(DBGuiUtils.CURSOR_ARROW);
		
		try (DBDatabaseExportConnection exportConnection = new DBDatabaseExportConnection(getDatabaseConnection()) ) {
            exportConnection.getVersionFromDatabase(this.selectedComponent);
		} catch (Exception e) {
		    DBGuiUtils.popup(Level.FATAL, "Cannot get version of selected component from the database.", e);
		    return ;
		}
		
			// if everything goes well, then we search for all the versions of the component
		if ( logger.isDebugEnabled() ) logger.debug("Searching for all versions of the component");

		this.tblVersions.removeAll();
		this.tblContent.removeAll();
		this.btnImportDatabaseVersion.setEnabled(false);
		this.btnExportModelVersion.setEnabled(false);
		
		String tableName = null;
		if ( this.selectedComponent instanceof ArchimateModel )
			tableName = "models";
		else if ( this.selectedComponent instanceof IArchimateElement ) 
		    tableName = "elements";
		else if ( this.selectedComponent instanceof IArchimateRelationship ) 
            tableName = "relationships";
        else if ( this.selectedComponent instanceof IArchimateDiagramModel || this.selectedComponent instanceof ICanvasModel || this.selectedComponent instanceof ISketchModel )
        	tableName = "views";
        else if ( this.selectedComponent instanceof IFolder )
        	tableName = "folders";
        else if ( this.selectedComponent instanceof IDiagramModelObject )
            tableName = "views_objects";
        else if ( this.selectedComponent instanceof IDiagramModelConnection )
            tableName = "views_connections";
        else {
		    DBGuiUtils.popup(Level.FATAL, "Cannot get history for components of class "+this.selectedComponent.getClass().getSimpleName());
		    return ;
		}
	
		try ( DBSelect result = new DBSelect(getDatabaseConnection().getDatabaseEntry().getName(), getDatabaseConnection().getConnection(),"SELECT version, created_by, created_on FROM "+this.selectedDatabase.getSchemaPrefix()+tableName+" where id = ? ORDER BY version DESC", this.selectedComponent.getId()) ) {
			while ( result.next() ) {
			    TableItem tableItem = new TableItem(this.tblVersions, SWT.NULL);
			    tableItem.setText(0, String.valueOf(result.getInt("version")));
			    tableItem.setText(1, result.getString("created_by"));
			    tableItem.setText(2, result.getTimestamp("created_on").toString());
			}
		} catch (Exception err) {
		    this.tblVersions.removeAll();
			DBGuiUtils.popup(Level.FATAL, "Failed to search component versions in the database.", err);
		}
		
		if ( this.tblVersions.getItemCount() > 1 ) {
		    this.lblVersions.setText(this.tblVersions.getItemCount()+" versions have been found in the database:");
		} else {
		    this.lblVersions.setText(this.tblVersions.getItemCount()+" version has been found in the database:");
		}
		
		if ( this.tblVersions.getItemCount() != 0 ) {
			this.tblVersions.select(0);
			this.tblVersions.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	@Override
    protected void notConnectedToDatabase() {
	    this.lblVersions.setText("");
	    this.tblContent.removeAll();
	    this.tblVersions.removeAll();
	}
}
