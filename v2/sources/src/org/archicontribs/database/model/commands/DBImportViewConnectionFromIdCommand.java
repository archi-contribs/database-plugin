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

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBBendpoint;
import org.archicontribs.database.data.DBImportMode;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.Logger;

/**
 * Command for importing an view connection from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewConnectionFromIdCommand extends CompoundCommand implements IDBImportCommand {
	private static final DBLogger logger = new DBLogger(DBImportViewConnectionFromIdCommand.class);

	private IDiagramModelConnection importedViewConnection = null; 

	private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
	private Exception exception;

	private DBArchimateModel model = null;
    private DBImportRelationshipFromIdCommand importRelationshipCommand = null;

	private String id = null;
	private boolean mustCreateCopy = false;
	private boolean isNew = false;

	// new values that are retrieved from the database
	private HashMap<String, Object> newValues = null;

	// old values that need to be retain to allow undo
	private DBVersion oldInitialVersion;
	private DBVersion oldCurrentVersion;
	private DBVersion oldDatabaseVersion;
	private DBVersion oldLatestDatabaseVersion;
	private String oldName = null;
	private IArchimateConcept oldArchimateConcept = null;
	private String oldDocumentation =null;
	private Integer oldType = null;
	private Boolean oldIsLocked = null;
	private String oldLineColor = null;
	private Integer oldLineWidth = null;
	private String oldFont = null;
	private String oldFontColor = null;
	private Integer oldTextPosition = null;
	private IConnectable oldSource = null;
	private IConnectable oldTarget = null;
	private ArrayList<DBProperty> oldProperties = null;
	private ArrayList<DBProperty> oldFeatures = null;
	private ArrayList<DBBendpoint> oldBendpoints = null;

	/**
	 * Imports a view connection into the model<br>
	 * @param importConnection connection to the database
	 * @param archimateModel model into which the view connection will be imported
	 * @param mergedModelId ID of the model merged in the actual model, to search for its parent folder 
	 * @param idToImport id of the view connection to import
	 * @param versionToImport version of the view connection to import (0 if the latest version should be imported)
     * @param mustCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the view object should be its original id
	 * @param importMode specifies if the view must be copied or shared
	 */
	public DBImportViewConnectionFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel archimateModel, String mergedModelId, String idToImport, int versionToImport, boolean mustCopy, DBImportMode importMode) {
		this.model = archimateModel;
		this.id = idToImport;
        this.mustCreateCopy = mustCopy;

		if ( logger.isDebugEnabled() ) {
			if ( this.mustCreateCopy )
				logger.debug("   Importing a copy of view connection id "+this.id + " version " + versionToImport +".");
			else
				logger.debug("   Importing view connection id "+this.id + " version " + versionToImport +".");
		}

		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObjectFromDatabase(idToImport, "IDiagramModelConnection", versionToImport);
			
			if ( this.mustCreateCopy ) {
				String newId = DBPlugin.createID();
				this.model.registerCopiedViewConnection((String)this.newValues.get("id"), newId);
				this.newValues.put("id", newId);
				this.newValues.put("name", (String)this.newValues.get("name") + DBPlugin.INSTANCE.getPreferenceStore().getString("copySuffix"));
			}
			
            // if the object references a relationship, then we import it
            if ( this.newValues.get("relationship_id") != null ) {
                this.importRelationshipCommand = new DBImportRelationshipFromIdCommand(importConnection, archimateModel, null, mergedModelId, null, (String)this.newValues.get("relationship_id"), 0, importMode);
                if ( this.importRelationshipCommand.getException() != null )
                    throw this.importRelationshipCommand.getException();
            }

			if ( DBPlugin.isEmpty((String)this.newValues.get("name")) ) {
				setLabel("import view connection");
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
		
        // if the referenced relationship needs to be imported
        if ( this.importRelationshipCommand != null )
            this.importRelationshipCommand.execute();

		try {
			this.importedViewConnection = this.model.getAllViewConnections().get(this.id);

			if ( this.importedViewConnection == null ) {
				if ( ((String)this.newValues.get("class")).startsWith("Canvas") )
					this.importedViewConnection = (IDiagramModelConnection) ICanvasFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier("com.archimatetool.canvas.model."+(String)this.newValues.get("class"))));
				else
					this.importedViewConnection = (IDiagramModelConnection) IArchimateFactory.eINSTANCE.create((EClass)(IArchimateFactory.eINSTANCE.getEPackage().getEClassifier((String)this.newValues.get("class"))));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata dbMetadata = this.model.getDBMetadata(this.importedViewConnection);
				
				this.oldInitialVersion = dbMetadata.getInitialVersion();
				this.oldCurrentVersion = dbMetadata.getCurrentVersion();
				this.oldDatabaseVersion = dbMetadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = dbMetadata.getLatestDatabaseVersion();

				this.oldName = dbMetadata.getName();
				this.oldArchimateConcept = dbMetadata.getArchimateConcept();
				this.oldIsLocked = dbMetadata.isLocked();
				this.oldDocumentation = dbMetadata.getDocumentation();
				this.oldLineColor = dbMetadata.getLineColor();
				this.oldLineWidth = dbMetadata.getLineWidth();
				this.oldFont = dbMetadata.getFont();
				this.oldFontColor = dbMetadata.getFontColor();
				this.oldType = dbMetadata.getType();
				this.oldTextPosition = dbMetadata.getTextPosition();
				this.oldSource = dbMetadata.getSourceConnection();
				this.oldTarget = dbMetadata.getTargetConnection();

				if ( dbMetadata.getArchimateConcept()==null ) {
					this.oldProperties = new ArrayList<DBProperty>();
					for ( IProperty prop: ((IProperties)this.importedViewConnection).getProperties() ) {
						this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
					}
				}
				
				this.oldFeatures = new ArrayList<DBProperty>();
				for ( IFeature feature: this.importedViewConnection.getFeatures() ) {
					this.oldFeatures.add(new DBProperty(feature.getName(), feature.getValue()));
				}

				this.oldBendpoints = new ArrayList<DBBendpoint>();
				for ( IDiagramModelBendpoint bendpoint: this.importedViewConnection.getBendpoints() ) {
					this.oldBendpoints.add(new DBBendpoint(bendpoint.getStartX(), bendpoint.getStartY(), bendpoint.getEndX(), bendpoint.getEndY()));
				}

				this.isNew = false;                
			}

			DBMetadata metadata = this.model.getDBMetadata(this.importedViewConnection);

			if ( this.mustCreateCopy )
				metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
			else
				metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));

			metadata.getCurrentVersion().set(metadata.getInitialVersion());
			metadata.getDatabaseVersion().set(metadata.getInitialVersion());
			metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

			metadata.setId((String)this.newValues.get("id"));
			if ( this.newValues.get("relationship_id") == null )
			    metadata.setName((String)this.newValues.get("name"));
			else
			    metadata.setArchimateConcept(this.model.getAllRelationships().get(this.model.getNewRelationshipId((String)this.newValues.get("relationship_id"))));
			metadata.setLocked(this.newValues.get("is_locked"));
			metadata.setDocumentation((String)this.newValues.get("documentation"));
			metadata.setLineColor((String)this.newValues.get("line_color"));
			metadata.setLineWidth((Integer)this.newValues.get("line_width"));
			metadata.setFont((String)this.newValues.get("font"));
			metadata.setFontColor((String)this.newValues.get("font_color"));
			metadata.setType((Integer)this.newValues.get("type"));
			metadata.setTextPosition((Integer)this.newValues.get("text_position"));

			// The source of the view connection can be either a view object or another view connection
			IConnectable source = this.model.getAllViewObjects().get(this.model.getNewViewObjectId((String)this.newValues.get("source_object_id")));
			if ( source == null )
				source = this.model.getAllViewConnections().get(this.model.getNewViewConnectionId((String)this.newValues.get("source_object_id")));
			if ( source == null ) {
				// the source does not exist in the model, so we register it for later resolution
				this.model.registerSourceConnection(this.importedViewConnection, (String)this.newValues.get("source_object_id"));
			} else {
				// the source can be resolved right away
				metadata.setSourceConnection(source);
			}

			// The target of the view connection can be either a view object or another view connection
			IConnectable target = this.model.getAllViewObjects().get(this.model.getNewViewObjectId((String)this.newValues.get("target_object_id")));
			if ( target == null )
				target = this.model.getAllViewConnections().get(this.model.getNewViewConnectionId((String)this.newValues.get("target_object_id")));
			if ( target == null ) {
				// the target does not exist in the model, so we register it for later resolution
				this.model.registerTargetConnection(this.importedViewConnection, (String)this.newValues.get("target_object_id"));
			} else {
				// the target can be resolved right away
				metadata.setTargetConnection(target);
			}

			// If the connection has got properties but does not have a linked element, then it may have distinct properties
			if ( this.newValues.get("relationship_id") == null ) {
	            this.importedViewConnection.getProperties().clear();
	            if ( this.newValues.get("properties") != null ) {
    	            for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    	                IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
    	                prop.setKey(newProperty.getKey());
    	                prop.setValue(newProperty.getValue());
    	                this.importedViewConnection.getProperties().add(prop);
    	            }
	            }
			}

            this.importedViewConnection.getBendpoints().clear();
            if ( this.newValues.get("bendpoints") != null ) {
                for ( DBBendpoint newBendpoint: (ArrayList<DBBendpoint>)this.newValues.get("bendpoints")) {
                    IDiagramModelBendpoint bendpoint = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
                    bendpoint.setStartX(newBendpoint.getStartX());
                    bendpoint.setStartY(newBendpoint.getStartY());
                    bendpoint.setEndX(newBendpoint.getEndX());
                    bendpoint.setEndY(newBendpoint.getEndY());
                    this.importedViewConnection.getBendpoints().add(bendpoint);
                }
            }

			// we determine the view that contains the view object
			EObject view = this.importedViewConnection.eContainer();
			while ( (view!= null) && !(view instanceof IDiagramModel) ) {
				view = view.eContainer();
			}

			// we indicate that the checksum of the view is not valid anymore
			if ( view!= null )
				this.model.getDBMetadata(view).setChecksumValid(false);

			this.model.countObject(this.importedViewConnection, false);
		} catch (Exception err) {
		    Logger.logError("Got Exception "+err.getMessage());
			this.importedViewConnection = null;
			this.exception = err;
		}
	}

	@Override
	public void undo() {
		if ( !this.commandHasBeenExecuted )
			return;

		if ( this.isNew ) {
			// if the view connection has been created by the execute() method, we just delete it
			this.importedViewConnection.disconnect();

			this.model.getAllViewConnections().remove(this.importedViewConnection.getId());
		} else {
			// else, we need to restore the old properties
			DBMetadata dbMetadata = this.model.getDBMetadata(this.importedViewConnection);

			dbMetadata.getInitialVersion().set(this.oldInitialVersion);
			dbMetadata.getCurrentVersion().set(this.oldCurrentVersion);
			dbMetadata.getDatabaseVersion().set(this.oldDatabaseVersion);
			dbMetadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

			if ( dbMetadata.getArchimateConcept() != null ) dbMetadata.setName(this.oldName);
			dbMetadata.setArchimateConcept(this.oldArchimateConcept);
			dbMetadata.setLocked(this.oldIsLocked);
			dbMetadata.setDocumentation(this.oldDocumentation);
			dbMetadata.setLineColor(this.oldLineColor);
			dbMetadata.setLineWidth(this.oldLineWidth);
			dbMetadata.setFont(this.oldFont);
			dbMetadata.setFontColor(this.oldFontColor);
			dbMetadata.setType(this.oldType);
			dbMetadata.setTextPosition(this.oldTextPosition);

			dbMetadata.setSourceConnection(this.oldSource);
			dbMetadata.setTargetConnection(this.oldTarget);

			// If the object has got properties but does not have a linked relationship, then it may have distinct properties
			if ( this.oldProperties == null ) {
				((IProperties)this.importedViewConnection).getProperties().clear();
				for ( DBProperty pair: this.oldProperties ) {
					IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
					prop.setKey(pair.getKey());
					prop.setValue(pair.getValue());
					((IProperties)this.importedViewConnection).getProperties().add(prop);
				}
			}
			
			this.importedViewConnection.getFeatures().clear();
			for ( DBProperty oldFeature: this.oldFeatures ) {
				IFeature newFeature = IArchimateFactory.eINSTANCE.createFeature();
				newFeature.setName(oldFeature.getKey());
				newFeature.setValue(oldFeature.getValue());
				this.importedViewConnection.getFeatures().add(newFeature);
			}

			this.importedViewConnection.getBendpoints().clear();
			for ( DBBendpoint oldBendpoint: this.oldBendpoints ) {
				IDiagramModelBendpoint newBendpoint = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
				newBendpoint.setStartX(oldBendpoint.getStartX());
				newBendpoint.setStartY(oldBendpoint.getStartY());
				newBendpoint.setEndX(oldBendpoint.getEndX());
				newBendpoint.setEndY(oldBendpoint.getEndY());
				this.importedViewConnection.getBendpoints().add(newBendpoint);
			}
		}
		
		// if a relationship has been imported
        if ( this.importRelationshipCommand != null )
            this.importRelationshipCommand.undo();

		this.commandHasBeenExecuted = false;
	}

	/**
	 * @return the view connection that has been imported by the command (of course, the command must have been executed before)<br>
	 * if the value is null, the exception that has been raised can be get using {@link getException}
	 */
	@Override
	public IDiagramModelConnection getImported() {
		return this.importedViewConnection;
	}


	/**
	 * @return the view object that has been imported by the command (of course, the command must have been executed before)
	 */
	@Override
	public Exception getException() {
		return this.exception;
	}
}
