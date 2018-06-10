/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.Command;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * Command for importing an element from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewObjectFromIdCommand extends Command {
    private static final DBLogger logger = new DBLogger(DBImportViewObjectFromIdCommand.class);
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private EObject importedViewObject = null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    private boolean importedViewObjectHasBeenCreated = false;
    private Command importCorrespondingElementCommand = null;
    
    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    
	private IArchimateConcept oldArchimateConcept = null;
	private IDiagramModel oldReferencedModel = null;
	private Integer oldType = null;
	private String oldBorderColor = null;
	private Integer oldBorderType = null;
	private String oldContent = null;
	private String oldName = null;
	private String oldHintContent = null;
	private String oldHintTitle = null;
	private Boolean oldIsLocked = null;
	private String oldImagePath = null;
	private Integer oldImagePosition = null;
	private String oldLineColor = null;
	private Integer oldLineWidth = null;
	private String oldFillColor = null;
	private String oldFont = null;
	private String oldFontColor = null;
	private String oldNotes = null;
	private Integer oldTextAlignment = null;
	private Integer oldTextPosition = null;
	private Integer oldX = null;
	private Integer oldY = null;
	private Integer oldWidth = null;
	private Integer oldHeight = null;
	
    private ArrayList<IProperty> oldProperties;
    
    /**
     * Imports a view object into the model<br>
     * @param model model into which the element will be imported
     * @param id id of the element to import
     * @param version version of the element to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the element should be its original id
     */
    public DBImportViewObjectFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version, boolean mustCreateCopy) {
        this.importConnection = connection;
        this.model = model;
        this.id = id;
        this.version = version;
        this.mustCreateCopy = mustCreateCopy;
    }

    @Override
    public boolean canExecute() {
        try {
            return (this.importConnection != null)
                    && (!this.importConnection.isClosed())
                    && (this.model != null)
                    && (this.id != null) ;
        } catch (@SuppressWarnings("unused") SQLException ign) {
            return false;
        }
    }
    
    @Override
    public void execute() {
    	if ( this.commandHasBeenExecuted )
    		return;		// we do not execute it twice
    	
        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("*************************** Importing a copy of view object id "+this.id+".");
            else
                logger.debug("*************************** Importing view object id "+this.id+".");
        }

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"views_objects WHERE id = v.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT id, version, class, container_id, element_id, diagram_ref_id, border_color, border_type, content, documentation, hint_content, hint_title, is_locked, image_path, image_position, line_color, line_width, fill_color, font, font_color, name, notes, text_alignment, text_position, type, x, y, width, height, checksum, created_on FROM "+this.importConnection.getSchema()+"views_objects v WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("View object with id="+this.id+" has not been found in the database.");
                throw new Exception("View object with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }
            
            DBMetadata metadata;

			this.importedViewObject = this.model.getAllViewObjects().get(this.id);
			if ( this.importedViewObject == null || this.mustCreateCopy ) {
				if ( result.getString("class").startsWith("Canvas") )
					this.importedViewObject = DBCanvasFactory.eINSTANCE.create(result.getString("class"));
				else
					this.importedViewObject = DBArchimateFactory.eINSTANCE.create(result.getString("class"));

				((IIdentifier)this.importedViewObject).setId(this.mustCreateCopy ? this.model.getIDAdapter().getNewID() : this.id);
				
				metadata = ((IDBMetadata)this.importedViewObject).getDBMetadata();
				
				metadata.getInitialVersion().setVersion(1);
				metadata.getInitialVersion().setChecksum(result.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
				metadata.getCurrentVersion().setVersion(1);
				metadata.getCurrentVersion().setChecksum(result.getString("checksum"));
				metadata.getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
			} else {
				metadata = ((IDBMetadata)this.importedViewObject).getDBMetadata();
				
				if ( result.getObject("element_id") == null ) this.oldName = metadata.getName();
				this.oldArchimateConcept = metadata.getArchimateConcept();
				this.oldReferencedModel = metadata.getReferencedModel();
				this.oldType = metadata.getType();
				this.oldBorderColor = metadata.getBorderColor();
				this.oldBorderType = metadata.getBorderType();
				this.oldContent = metadata.getContent();
				this.oldHintContent = metadata.getHintContent();
				this.oldHintTitle = metadata.getHintTitle();
				this.oldIsLocked = metadata.isLocked();
				this.oldImagePath = metadata.getImagePath();
				this.oldImagePosition = metadata.getImagePosition();
				this.oldLineColor = metadata.getLineColor();
				this.oldLineWidth = metadata.getLineWidth();
				this.oldFillColor = metadata.getFillColor();
				this.oldFont = metadata.getFont();
				this.oldFontColor = metadata.getFontColor();
				this.oldNotes = metadata.getNotes();
				this.oldTextAlignment = metadata.getTextAlignment();
				this.oldTextPosition = metadata.getTextPosition();
				this.oldX = metadata.getX();
				this.oldY = metadata.getY();
				this.oldWidth = metadata.getWidth();
				this.oldHeight = metadata.getHeight();
				
				if ( this.importedViewObject instanceof IProperties && result.getString("element_id")==null ) {
					this.oldProperties = new ArrayList<IProperty>(((IProperties)this.importedViewObject).getProperties());
				}
				
				metadata.getInitialVersion().setVersion(result.getInt("version"));
				metadata.getInitialVersion().setChecksum(result.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
				metadata.getCurrentVersion().setVersion(result.getInt("version"));
				metadata.getCurrentVersion().setChecksum(result.getString("checksum"));
				metadata.getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
			}
			
			metadata.getDatabaseVersion().setVersion(result.getInt("version"));
			metadata.getDatabaseVersion().setChecksum(result.getString("checksum"));
			metadata.getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
			metadata.getLatestDatabaseVersion().setVersion(result.getInt("version"));
			metadata.getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
			metadata.getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));

			if ( this.importedViewObject instanceof IDiagramModelArchimateComponent && result.getString("element_id") != null) {
				// we check that the element already exists. If not, we import it in shared mode
				IArchimateElement element = this.model.getAllElements().get(result.getString("element_id"));
				if ( element == null ) {
					this.importCorrespondingElementCommand = new DBImportElementFromIdCommand(this.importConnection, this.model, result.getString("element_id"), 0);
					this.importCorrespondingElementCommand.execute();
				}
			}

			if ( this.model.getAllElements().get(result.getString("element_id")) != null ) metadata.setName(result.getString("name"));
			metadata.setArchimateConcept(this.model.getAllElements().get(result.getString("element_id")));
			metadata.setReferencedModel(this.model.getAllViews().get(result.getString("diagram_ref_id")));
			metadata.setType(result.getInt("type"));
			metadata.setBorderColor(result.getString("border_color"));
			metadata.setBorderType(result.getInt("border_type"));
			metadata.setContent(result.getString("content"));
			metadata.setDocumentation(result.getString("documentation"));
			metadata.setHintContent(result.getString("hint_content"));
			metadata.setHintTitle(result.getString("hint_title"));
			metadata.setLocked(result.getObject("is_locked"));
			metadata.setImagePath(result.getString("image_path"));
			metadata.setImagePosition(result.getInt("image_position"));
			metadata.setLineColor(result.getString("line_color"));
			metadata.setLineWidth(result.getInt("line_width"));
			metadata.setFillColor(result.getString("fill_color"));
			metadata.setFont(result.getString("font"));
			metadata.setFontColor(result.getString("font_color"));
			metadata.setNotes(result.getString("notes"));
			metadata.setTextAlignment(result.getInt("text_alignment"));
			metadata.setTextPosition(result.getInt("text_position"));
			metadata.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));
			
			// If the object has got properties but does not have a linked element, then it may have distinct properties
			if ( this.importedViewObject instanceof IProperties && result.getString("element_id")==null )
				this.importConnection.importProperties((IProperties)this.importedViewObject);

			// we check if the view object must be changed from container
			if ( this.importedViewObject instanceof IDiagramModelObject ) {
				IDiagramModelContainer newContainer = this.model.getAllViews().get(result.getString("container_id"));
				if ( newContainer == null )
					newContainer = (IDiagramModelContainer) this.model.getAllViewObjects().get(result.getString("container_id"));
				IDiagramModelContainer currentContainer = (IDiagramModelContainer) ((IDiagramModelObject)this.importedViewObject).eContainer();		

				if ( currentContainer != null ) {
					if ( newContainer != currentContainer ) {
						if ( logger.isTraceEnabled() ) logger.trace("   Removing from container "+((IDBMetadata)currentContainer).getDBMetadata().getDebugName());
						currentContainer.getChildren().remove(this.importedViewObject);
					} else
						newContainer = null;		// no need to assign it again to the same container
				}
				
				if ( newContainer != null ) {
					if ( logger.isTraceEnabled() ) logger.trace("   Assigning to container "+((IDBMetadata)newContainer).getDBMetadata().getDebugName());
					newContainer.getChildren().add((IDiagramModelObject)this.importedViewObject);
				}
			}
			
			EObject viewContainer = this.importedViewObject.eContainer();
			while ( (viewContainer!= null) && !(viewContainer instanceof IDiagramModel) ) {
				viewContainer = viewContainer.eContainer();
			}
			if ( viewContainer!= null ) ((IDBMetadata)viewContainer).getDBMetadata().setChecksumValid(false);

			this.model.countObject(this.importedViewObject, false, null);
			
			// if the object contains an image, we store its path to import it later
			if ( result.getString("image_path") != null )
				this.importConnection.getAllImagePaths().add(result.getString("image_path"));

			if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)this.importedViewObject).getDBMetadata().getInitialVersion().getVersion()+" of "+((IDBMetadata)this.importedViewObject).getDBMetadata().getDebugName());
        } catch (Exception e) {
            // TODO: find a way to advertise the user as exceptions cannot be thrown
            logger.error("Failed to import element !!!", e);
        }
        
        this.commandHasBeenExecuted = true;
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
    	
        // if a importedViewObject has been created, then we remove it from the view
        if ( this.importCorrespondingElementCommand != null ) {
            this.importCorrespondingElementCommand.undo();
        }
        
        if ( this.importedViewObjectHasBeenCreated ) {
            // if the element has been created by the execute() method, we just delete it
            IFolder parentFolder = (IFolder)this.importedViewObject.eContainer();
            parentFolder.getElements().remove(this.importedViewObject);
            
            this.model.getAllViewObjects().remove(((IIdentifier)this.importedViewObject).getId());
        } else {
            // else, we need to restore the old properties
            DBMetadata metadata = ((IDBMetadata)this.importedViewObject).getDBMetadata();
            
            metadata.getInitialVersion().set(this.oldInitialVersion);
            metadata.getCurrentVersion().set(this.oldCurrentVersion);
            metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
            metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

			if ( metadata.getArchimateConcept() != null ) metadata.setName(this.oldName);
			metadata.setArchimateConcept(this.oldArchimateConcept);
			metadata.setReferencedModel(this.oldReferencedModel);
			metadata.setType(this.oldType);
			metadata.setBorderColor(this.oldBorderColor);
			metadata.setBorderType(this.oldBorderType);
			metadata.setContent(this.oldContent);

			metadata.setHintContent(this.oldHintContent);
			metadata.setHintTitle(this.oldHintTitle);
			metadata.setLocked(this.oldIsLocked);
			metadata.setImagePath(this.oldImagePath);
			metadata.setImagePosition(this.oldImagePosition);
			metadata.setLineColor(this.oldLineColor);
			metadata.setLineWidth(this.oldLineWidth);
			metadata.setFillColor(this.oldFillColor);
			metadata.setFont(this.oldFont);
			metadata.setFontColor(this.oldFontColor);
			metadata.setNotes(this.oldNotes);
			metadata.setTextAlignment(this.oldTextAlignment);
			metadata.setTextPosition(this.oldTextPosition);
			metadata.setBounds(this.oldX,  this.oldY,  this.oldWidth, this.oldHeight);
            
			// If the object has got properties but does not have a linked element, then it may have distinct properties
			if ( this.importedViewObject instanceof IProperties && metadata.getArchimateConcept() == null ) {
	            ((IProperties)this.importedViewObject).getProperties().clear();
	            for ( IProperty prop: this.oldProperties )
	            	((IProperties)this.importedViewObject).getProperties().add(prop);
			}
        }
        
        this.commandHasBeenExecuted = false;
    }

    @Override
    public void dispose() {
        this.oldInitialVersion = null;
        this.oldCurrentVersion = null;
        this.oldDatabaseVersion = null;
        this.oldLatestDatabaseVersion = null;
        
        this.importCorrespondingElementCommand.dispose();
        this.importCorrespondingElementCommand = null;
        
		this.oldArchimateConcept = null;
		this.oldReferencedModel = null;
		this.oldType = null;
		this.oldBorderColor = null;
		this.oldBorderType = null;
		this.oldContent = null;
		this.oldName = null;
		this.oldHintContent = null;
		this.oldHintTitle = null;
		this.oldIsLocked = null;
		this.oldImagePath = null;
		this.oldImagePosition = null;
		this.oldLineColor = null;
		this.oldLineWidth = null;
		this.oldFillColor = null;
		this.oldFont = null;
		this.oldFontColor = null;
		this.oldNotes = null;
		this.oldTextAlignment = null;
		this.oldTextPosition = null;
		this.oldX = null;
		this.oldY = null;
		this.oldWidth = null;
		this.oldHeight = null;
        
        this.oldProperties = null;
        
        this.importedViewObject = null;
        this.model = null;
        this.id = null;
        this.importConnection = null;
    }
}
