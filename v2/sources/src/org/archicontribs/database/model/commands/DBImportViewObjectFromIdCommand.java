/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model.commands;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.connection.DBSelect;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CompoundCommand;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing a view object from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewObjectFromIdCommand extends CompoundCommand implements IDBImportCommand {
    private static final DBLogger logger = new DBLogger(DBImportViewObjectFromIdCommand.class);

    private EObject importedViewObject = null; 

    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    private Exception exception;

    private DBArchimateModel model = null;
    private DBImportElementFromIdCommand importElementCommand = null;
    private List<DBImportViewFromIdCommand> importLinkedViewCommands = new ArrayList<DBImportViewFromIdCommand>(); 

    private String id;
    private boolean mustCreateCopy;
    private boolean isNew;

    // new values that are retrieved from the database
    private HashMap<String, Object> newValues = null;
    private byte[]newImageContent = null;

    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    private IDiagramModelContainer oldContainer = null;
    private IArchimateConcept oldArchimateConcept = null;
    private IDiagramModel oldReferencedModel = null;
    private String oldDocumentation = null;
    private Integer oldType = null;
    private String oldBorderColor = null;
    private Integer oldBorderType = null;
    private String oldContent = null;
    private String oldName = null;
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
    private ArrayList<DBProperty> oldProperties = null;

    /**
     * Imports a view object into the model<br>
     * @param connection connection to the database
     * @param model model into which the view object will be imported
     * @param id id of the view object to import
     * @param version version of the view object to import
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the view object should be its original id
     * @param importMode specifies the mode to be used to import missing elements and relationships
     */
    public DBImportViewObjectFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, String id, int version, boolean mustCreateCopy, DBImportMode importMode) {
        this.model = model;
        this.id = id;
        this.mustCreateCopy = mustCreateCopy;

        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of view object id "+this.id+" version "+version+".");
            else
                logger.debug("   Importing view object id "+this.id+" version "+version+".");
        }

        try {
            // we get the new values from the database to allow execute and redo
            this.newValues = importConnection.getObject(id, "IDiagramModelObject", version);
            
			if ( this.mustCreateCopy ) {
				String newId = DBPlugin.createID();
				this.model.registerCopiedViewObject((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}

            // if the object contains an image
            if ( this.newValues.get("image_path") != null ) {
                IArchiveManager archiveMgr = (IArchiveManager)this.model.getAdapter(IArchiveManager.class);
                if ( !archiveMgr.getLoadedImagePaths().contains(this.newValues.get("image_path")) ) {
                    try ( DBSelect imageResult = new DBSelect(importConnection.getDatabaseEntry().getName(), importConnection.getConnection(), "SELECT image FROM "+importConnection.getSchema()+"images WHERE path = ?", (String)this.newValues.get("image_path")) ) {
                        if ( imageResult.next() ) {
                            this.newImageContent = imageResult.getBytes("image");
                            logger.debug("   Importing image "+this.newValues.get("image_path")+" (size = "+this.newImageContent.length+")");
                            // TODO: the image content should be part of a cross instance array in order to be store only once in memory
                        }
                    }
                } else {
                    logger.debug("   Image "+this.newValues.get("image_path")+" already exists in the model");
                }
            }
            
            // if the object references an element, then we import it
            if ( this.newValues.get("element_id") != null ) {
                this.importElementCommand = new DBImportElementFromIdCommand(importConnection, model, null, null, (String)this.newValues.get("element_id"), 0, importMode, true);
                if ( this.importElementCommand.getException() != null )
                    throw this.importElementCommand.getException();
            }

            // if the object is an embedded view and reference another view than itself and the referenced view does not exist in the model, then we import it
            if ( (this.newValues.get("diagram_ref_id") != null) && !this.newValues.get("diagram_ref_id").equals(this.newValues.get("element_id")) && (model.getAllViews().get(this.model.getNewViewId((String)this.newValues.get("diagram_ref_id"))) == null) ) {
            	if ( !importConnection.isAlreadyImported((String)this.newValues.get("diagram_ref_id")) ) {
	                importConnection.declareAsImported((String)this.newValues.get("diagram_ref_id"));
	                DBImportViewFromIdCommand importLinkedViewCommand = new DBImportViewFromIdCommand(importConnection, model, null, (String)this.newValues.get("diagram_ref_id"), 0, importMode, true);
	                if ( importLinkedViewCommand.getException() != null )
	                    throw importLinkedViewCommand.getException();
	                this.importLinkedViewCommands.add(importLinkedViewCommand);
            	} else
            		logger.trace("Referenced diagram has been previously imported. We do not re-import it.");
            }

            if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
                setLabel("import view object");
            } else {
                if ( ((String)this.newValues.get("name")).length() > 20 )
                    setLabel("import \""+((String)this.newValues.get("name")).substring(0,16)+"...\"");
                else
                    setLabel("import \""+(String)this.newValues.get("name")+"\"");
            }
        } catch (Exception err) {
            Logger.logError("Got Exception "+err.getMessage());
            this.exception = err;
        }
    }

    @Override
    public boolean canExecute() {
        return (this.model != null) && (this.id != null) && (this.exception == null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute() {
        if ( this.commandHasBeenExecuted )
            return;		// we do not execute it twice

        this.commandHasBeenExecuted = true;

        try {
            // if there are linked views to import
            for ( DBImportViewFromIdCommand importLinkedViewCommand: this.importLinkedViewCommands ) {
                importLinkedViewCommand.execute();
                if ( importLinkedViewCommand.getException() != null )
                    throw importLinkedViewCommand.getException();
            }
            
            // if the referenced element needs to be imported
            if ( this.importElementCommand != null )
                this.importElementCommand.execute();

            this.importedViewObject = this.model.getAllViewObjects().get(this.id);

            if ( this.importedViewObject == null ) {
                if ( ((String)this.newValues.get("class")).startsWith("Canvas") )
                    this.importedViewObject = DBCanvasFactory.eINSTANCE.create((String)this.newValues.get("class"));
                else
                    this.importedViewObject = DBArchimateFactory.eINSTANCE.create((String)this.newValues.get("class"));

                this.isNew = true;
            } else {
                // we must save the old values to allow undo
                DBMetadata metadata = ((IDBMetadata)this.importedViewObject).getDBMetadata();

                this.oldInitialVersion = metadata.getInitialVersion();
                this.oldCurrentVersion = metadata.getCurrentVersion();
                this.oldDatabaseVersion = metadata.getDatabaseVersion();
                this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();

                this.oldName = metadata.getName();
                this.oldArchimateConcept = metadata.getArchimateConcept();
                this.oldReferencedModel = metadata.getReferencedModel();
                this.oldDocumentation = metadata.getDocumentation();
                this.oldType = metadata.getType();
                this.oldBorderColor = metadata.getBorderColor();
                this.oldBorderType = metadata.getBorderType();
                this.oldContent = metadata.getContent();
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

                if ( (this.importedViewObject instanceof IProperties) && (metadata.getArchimateConcept() == null) ) {
                    this.oldProperties = new ArrayList<DBProperty>();
                    for ( IProperty prop: ((IProperties)this.importedViewObject).getProperties() ) {
                        this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
                    }
                }

                this.oldContainer = (IDiagramModelContainer)this.importedViewObject.eContainer();

                this.isNew = false;
            }

            DBMetadata metadata = ((IDBMetadata)this.importedViewObject).getDBMetadata();

            if ( this.mustCreateCopy )
                metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
            else
                metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));

            metadata.getCurrentVersion().set(metadata.getInitialVersion());
            metadata.getDatabaseVersion().set(metadata.getInitialVersion());
            metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

            metadata.setId((String)this.newValues.get("id"));
            if ( this.newValues.get("element_id") == null )
                metadata.setName((String)this.newValues.get("name"));
            else
                metadata.setArchimateConcept(this.model.getAllElements().get(this.model.getNewElementId((String)this.newValues.get("element_id"))));
            metadata.setReferencedModel(this.model.getAllViews().get(this.model.getNewViewId((String)this.newValues.get("diagram_ref_id"))));
            metadata.setType((Integer)this.newValues.get("type"));
            metadata.setBorderColor((String)this.newValues.get("border_color"));
            metadata.setBorderType((Integer)this.newValues.get("border_type"));
            metadata.setContent((String)this.newValues.get("content"));
            metadata.setDocumentation((String)this.newValues.get("documentation"));
            metadata.setLocked(this.newValues.get("is_locked"));
            metadata.setImagePosition((Integer)this.newValues.get("image_position"));
            metadata.setLineColor((String)this.newValues.get("line_color"));
            metadata.setLineWidth((Integer)this.newValues.get("line_width"));
            metadata.setFillColor((String)this.newValues.get("fill_color"));
            metadata.setFont((String)this.newValues.get("font"));
            metadata.setFontColor((String)this.newValues.get("font_color"));
            metadata.setNotes((String)this.newValues.get("notes"));
            metadata.setTextAlignment((Integer)this.newValues.get("text_alignment"));
            metadata.setTextPosition((Integer)this.newValues.get("text_position"));
            metadata.setBounds((Integer)this.newValues.get("x"), (Integer)this.newValues.get("y"), (Integer)this.newValues.get("width"), (Integer)this.newValues.get("height"));

            // we check if the view object must be changed from container
            if ( this.importedViewObject instanceof IDiagramModelObject ) {
                IDiagramModelContainer newContainer = this.model.getAllViews().get(this.model.getNewViewId((String)this.newValues.get("container_id")));
                if ( newContainer == null )
                    newContainer = (IDiagramModelContainer) this.model.getAllViewObjects().get(this.model.getNewViewObjectId((String)this.newValues.get("container_id")));

                if ( (newContainer != null) && (newContainer != this.oldContainer) ) {
                    if ( this.oldContainer != null ) {
                        if ( logger.isTraceEnabled() ) logger.trace("   Removing from container "+((IDBMetadata)this.oldContainer).getDBMetadata().getDebugName());
                        this.oldContainer.getChildren().remove(this.importedViewObject);
                    }

                    if ( logger.isTraceEnabled() ) logger.trace("   Assigning to container "+((IDBMetadata)newContainer).getDBMetadata().getDebugName());
                    newContainer.getChildren().add((IDiagramModelObject)this.importedViewObject);
                }
            }

            // If the object has got properties but does not have a linked element, then it may have distinct properties
            if ( (this.importedViewObject instanceof IProperties) && (this.newValues.get("element_id") == null) ) {
                ((IProperties)this.importedViewObject).getProperties().clear();
                if ( this.newValues.get("properties") != null ) {
                    for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
                        IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
                        prop.setKey(newProperty.getKey());
                        prop.setValue(newProperty.getValue());
                        ((IProperties)this.importedViewObject).getProperties().add(prop);
                    }
                }
            }

            // if the object contains an image
            if ( this.newImageContent != null ) {
                String imagePath = (String)this.newValues.get("image_path");
                IArchiveManager archiveMgr = (IArchiveManager)this.model.getAdapter(IArchiveManager.class);
                if ( !archiveMgr.getLoadedImagePaths().contains(imagePath) )
                    archiveMgr.addByteContentEntry(imagePath, this.newImageContent);
                metadata.setImagePath(imagePath);
            }

            // we determine the view that contains the view object
            EObject view = this.importedViewObject.eContainer();
            while ( (view!= null) && !(view instanceof IDiagramModel) ) {
                view = view.eContainer();
            }

            // we indicate that the checksum of the view is not valid anymore
            if ( view!= null )
                ((IDBMetadata)view).getDBMetadata().setChecksumValid(false);

            this.model.countObject(this.importedViewObject, false);

        } catch (Exception err) {
            Logger.logError("Got Exception "+err.getMessage());
            this.importedViewObject = null;
            this.exception = err;
        }
    }

    @Override
    public void undo() {
        if ( !this.commandHasBeenExecuted )
            return;

        if ( this.isNew ) {
            // if the view object has been created by the execute() method, we just delete it
            IDiagramModelContainer container = (IDiagramModelContainer)this.importedViewObject.eContainer();
            container.getChildren().remove(this.importedViewObject);

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
            metadata.setDocumentation(this.oldDocumentation);
            metadata.setType(this.oldType);
            metadata.setBorderColor(this.oldBorderColor);
            metadata.setBorderType(this.oldBorderType);
            metadata.setContent(this.oldContent);

            metadata.setLocked(this.oldIsLocked);
            metadata.setImagePath(this.oldImagePath);            // TODO: find a way to remove the image from the model if it is not used anymore
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
                for ( DBProperty oldProperty: this.oldProperties ) {
                    IProperty newProperty = DBArchimateFactory.eINSTANCE.createProperty();
                    newProperty.setKey(oldProperty.getKey());
                    newProperty.setValue(oldProperty.getValue());
                    ((IProperties)this.importedViewObject).getProperties().add(newProperty);
                }
            }
        }
        
        // if an element has been imported
        if ( this.importElementCommand != null )
            this.importElementCommand.undo();
        
        // if there are imported linked views
        for ( DBImportViewFromIdCommand importLinkedViewCommand: this.importLinkedViewCommands ) {
            importLinkedViewCommand.undo();
        }

        this.commandHasBeenExecuted = false;
    }

    @Override
    public EObject getImported() {
        return this.importedViewObject;
    }

    /**
     * @return the view object that has been imported by the command (of course, the command must have been executed before)
     */
    @Override
    public Exception getException() {
        return this.exception;
    }
}
