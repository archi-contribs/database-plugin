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
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateFactory;
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
     * @param importConnection connection to the database
     * @param archimateModel model into which the view object will be imported
     * @param idToImport id of the view object to import
     * @param version version of the view object to import
     * @param mustCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the view object should be its original id
     * @param importMode specifies the mode to be used to import missing elements and relationships
     */
    public DBImportViewObjectFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, String idToImport, int version, boolean mustCopy, DBImportMode importMode) {
        this.model = archimateModel;
        this.id = idToImport;
        this.mustCreateCopy = mustCopy;

        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of view object id "+this.id+" version "+version+".");
            else
                logger.debug("   Importing view object id "+this.id+" version "+version+".");
        }

        try {
            // we get the new values from the database to allow execute and redo
            this.newValues = importConnection.getObjectFromDatabase(idToImport, "IDiagramModelObject", version);
            
			if ( this.mustCreateCopy ) {
				String newId = DBPlugin.createID();
				this.model.registerCopiedViewObject((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}

            // if the object contains an image
            if ( this.newValues.get("image_path") != null ) {
                IArchiveManager archiveMgr = (IArchiveManager)archimateModel.getAdapter(IArchiveManager.class);
                if ( !archiveMgr.getLoadedImagePaths().contains((String)this.newValues.get("image_path")) ) {
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
                this.importElementCommand = new DBImportElementFromIdCommand(importConnection, archimateModel, null, null, (String)this.newValues.get("element_id"), 0, importMode, true);
                if ( this.importElementCommand.getException() != null )
                    throw this.importElementCommand.getException();
            }

            // if the object is an embedded view and reference another view than itself and the referenced view does not exist in the model, then we import it
            if ( (this.newValues.get("diagram_ref_id") != null) && !this.newValues.get("diagram_ref_id").equals(this.newValues.get("element_id")) && (archimateModel.getAllViews().get(this.model.getNewViewId((String)this.newValues.get("diagram_ref_id"))) == null) ) {
            	if ( !importConnection.isAlreadyImported((String)this.newValues.get("diagram_ref_id")) ) {
	                importConnection.declareAsImported((String)this.newValues.get("diagram_ref_id"));
	                DBImportViewFromIdCommand importLinkedViewCommand = new DBImportViewFromIdCommand(importConnection, archimateModel, null, (String)this.newValues.get("diagram_ref_id"), 0, importMode, true);
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
                	this.importedViewObject = ICanvasFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier("com.archimatetool.canvas.model."+(String)this.newValues.get("class"))));
                	else
                		this.importedViewObject = IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier((String)this.newValues.get("class"))));

                this.isNew = true;
            } else {
                // we must save the old values to allow undo
                DBMetadata dbMetadata = this.model.getDBMetadata(this.importedViewObject);

                this.oldInitialVersion = dbMetadata.getInitialVersion();
                this.oldCurrentVersion = dbMetadata.getCurrentVersion();
                this.oldDatabaseVersion = dbMetadata.getDatabaseVersion();
                this.oldLatestDatabaseVersion = dbMetadata.getLatestDatabaseVersion();

                this.oldName = dbMetadata.getName();
                this.oldArchimateConcept = dbMetadata.getArchimateConcept();
                this.oldReferencedModel = dbMetadata.getReferencedModel();
                this.oldDocumentation = dbMetadata.getDocumentation();
                this.oldType = dbMetadata.getType();
                this.oldBorderColor = dbMetadata.getBorderColor();
                this.oldBorderType = dbMetadata.getBorderType();
                this.oldContent = dbMetadata.getContent();
                this.oldIsLocked = dbMetadata.isLocked();
                this.oldImagePath = dbMetadata.getImagePath();
                this.oldImagePosition = dbMetadata.getImagePosition();
                this.oldLineColor = dbMetadata.getLineColor();
                this.oldLineWidth = dbMetadata.getLineWidth();
                this.oldFillColor = dbMetadata.getFillColor();
                this.oldFont = dbMetadata.getFont();
                this.oldFontColor = dbMetadata.getFontColor();
                this.oldNotes = dbMetadata.getNotes();
                this.oldTextAlignment = dbMetadata.getTextAlignment();
                this.oldTextPosition = dbMetadata.getTextPosition();
                this.oldX = dbMetadata.getX();
                this.oldY = dbMetadata.getY();
                this.oldWidth = dbMetadata.getWidth();
                this.oldHeight = dbMetadata.getHeight();

                if ( (this.importedViewObject instanceof IProperties) && (dbMetadata.getArchimateConcept() == null) ) {
                    this.oldProperties = new ArrayList<DBProperty>();
                    for ( IProperty prop: ((IProperties)this.importedViewObject).getProperties() ) {
                        this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
                    }
                }

                this.oldContainer = (IDiagramModelContainer)this.importedViewObject.eContainer();

                this.isNew = false;
            }

            DBMetadata dbMetadata = this.model.getDBMetadata(this.importedViewObject);

            if ( this.mustCreateCopy )
                dbMetadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
            else
                dbMetadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));

            dbMetadata.getCurrentVersion().set(dbMetadata.getInitialVersion());
            dbMetadata.getDatabaseVersion().set(dbMetadata.getInitialVersion());
            dbMetadata.getLatestDatabaseVersion().set(dbMetadata.getInitialVersion());

            dbMetadata.setId((String)this.newValues.get("id"));
            if ( this.newValues.get("element_id") == null )
                dbMetadata.setName((String)this.newValues.get("name"));
            else
                dbMetadata.setArchimateConcept(this.model.getAllElements().get(this.model.getNewElementId((String)this.newValues.get("element_id"))));
            dbMetadata.setReferencedModel(this.model.getAllViews().get(this.model.getNewViewId((String)this.newValues.get("diagram_ref_id"))));
            dbMetadata.setType((Integer)this.newValues.get("type"));
            dbMetadata.setBorderColor((String)this.newValues.get("border_color"));
            dbMetadata.setBorderType((Integer)this.newValues.get("border_type"));
            dbMetadata.setContent((String)this.newValues.get("content"));
            dbMetadata.setDocumentation((String)this.newValues.get("documentation"));
            dbMetadata.setLocked(this.newValues.get("is_locked"));
            dbMetadata.setImagePosition((Integer)this.newValues.get("image_position"));
            dbMetadata.setLineColor((String)this.newValues.get("line_color"));
            dbMetadata.setLineWidth((Integer)this.newValues.get("line_width"));
            dbMetadata.setFillColor((String)this.newValues.get("fill_color"));
            dbMetadata.setFont((String)this.newValues.get("font"));
            dbMetadata.setFontColor((String)this.newValues.get("font_color"));
            dbMetadata.setNotes((String)this.newValues.get("notes"));
            dbMetadata.setTextAlignment((Integer)this.newValues.get("text_alignment"));
            dbMetadata.setTextPosition((Integer)this.newValues.get("text_position"));
            dbMetadata.setBounds((Integer)this.newValues.get("x"), (Integer)this.newValues.get("y"), (Integer)this.newValues.get("width"), (Integer)this.newValues.get("height"));

            // we check if the view object must be changed from container
            if ( this.importedViewObject instanceof IDiagramModelObject ) {
                IDiagramModelContainer newContainer = this.model.getAllViews().get(this.model.getNewViewId((String)this.newValues.get("container_id")));
                if ( newContainer == null )
                    newContainer = (IDiagramModelContainer) this.model.getAllViewObjects().get(this.model.getNewViewObjectId((String)this.newValues.get("container_id")));

                if ( (newContainer != null) && (newContainer != this.oldContainer) ) {
                    if ( this.oldContainer != null ) {
                        if ( logger.isTraceEnabled() ) logger.trace("   Removing from container "+this.model.getDBMetadata(this.oldContainer).getDebugName());
                        this.oldContainer.getChildren().remove((IDiagramModelObject)this.importedViewObject);
                    }

                    if ( logger.isTraceEnabled() ) logger.trace("   Assigning to container "+this.model.getDBMetadata(newContainer).getDebugName());
                    newContainer.getChildren().add((IDiagramModelObject)this.importedViewObject);
                }
            }

            // If the object has got properties but does not have a linked element, then it may have distinct properties
            if ( (this.importedViewObject instanceof IProperties) && (this.newValues.get("element_id") == null) ) {
                ((IProperties)this.importedViewObject).getProperties().clear();
                if ( this.newValues.get("properties") != null ) {
                    for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
                        IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
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
                dbMetadata.setImagePath(imagePath);
            }

            // we determine the view that contains the view object
            EObject view = this.importedViewObject.eContainer();
            while ( (view!= null) && !(view instanceof IDiagramModel) ) {
                view = view.eContainer();
            }

            // we indicate that the checksum of the view is not valid anymore
            if ( view!= null )
                this.model.getDBMetadata(view).setChecksumValid(false);

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
            container.getChildren().remove((IDiagramModelObject)this.importedViewObject);

            this.model.getAllViewObjects().remove(((IIdentifier)this.importedViewObject).getId());
        } else {
            // else, we need to restore the old properties
            DBMetadata dbMetadata = this.model.getDBMetadata(this.importedViewObject);

            dbMetadata.getInitialVersion().set(this.oldInitialVersion);
            dbMetadata.getCurrentVersion().set(this.oldCurrentVersion);
            dbMetadata.getDatabaseVersion().set(this.oldDatabaseVersion);
            dbMetadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

            if ( dbMetadata.getArchimateConcept() != null ) dbMetadata.setName(this.oldName);
            dbMetadata.setArchimateConcept(this.oldArchimateConcept);
            dbMetadata.setReferencedModel(this.oldReferencedModel);
            dbMetadata.setDocumentation(this.oldDocumentation);
            dbMetadata.setType(this.oldType);
            dbMetadata.setBorderColor(this.oldBorderColor);
            dbMetadata.setBorderType(this.oldBorderType);
            dbMetadata.setContent(this.oldContent);

            dbMetadata.setLocked(this.oldIsLocked);
            dbMetadata.setImagePath(this.oldImagePath);            // TODO: find a way to remove the image from the model if it is not used anymore
            dbMetadata.setImagePosition(this.oldImagePosition);
            dbMetadata.setLineColor(this.oldLineColor);
            dbMetadata.setLineWidth(this.oldLineWidth);
            dbMetadata.setFillColor(this.oldFillColor);
            dbMetadata.setFont(this.oldFont);
            dbMetadata.setFontColor(this.oldFontColor);
            dbMetadata.setNotes(this.oldNotes);
            dbMetadata.setTextAlignment(this.oldTextAlignment);
            dbMetadata.setTextPosition(this.oldTextPosition);
            dbMetadata.setBounds(this.oldX,  this.oldY,  this.oldWidth, this.oldHeight);

            // If the object has got properties but does not have a linked element, then it may have distinct properties
            if ( this.importedViewObject instanceof IProperties && dbMetadata.getArchimateConcept() == null ) {
                ((IProperties)this.importedViewObject).getProperties().clear();
                for ( DBProperty oldProperty: this.oldProperties ) {
                    IProperty newProperty = IArchimateFactory.eINSTANCE.createProperty();
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
