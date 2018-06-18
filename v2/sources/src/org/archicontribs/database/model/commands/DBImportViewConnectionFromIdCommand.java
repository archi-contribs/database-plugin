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
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * Command for importing an view connection from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewConnectionFromIdCommand extends CompoundCommand implements IDBImportFromIdCommand{
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
	private ArrayList<DBBendpoint> oldBendpoints = null;

	/**
	 * Imports a view connection into the model<br>
	 * @param connection connection to the database
	 * @param model model into which the view connection will be imported
	 * @param id id of the view connection to import
	 * @param version version of the view connection to import (0 if the latest version should be imported)
	 * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the view connection should be its original id
	 */
	public DBImportViewConnectionFromIdCommand(DBDatabaseImportConnection importConnection, DBArchimateModel model, String id, int version, boolean mustCreateCopy) {
		this.model = model;
		this.id = id;
		this.mustCreateCopy = mustCreateCopy;

		if ( logger.isDebugEnabled() ) {
			if ( this.mustCreateCopy )
				logger.debug("   Importing a copy of view connection id "+this.id+".");
			else
				logger.debug("   Importing view connection id "+this.id+".");
		}

		try {
			// we get the new values from the database to allow execute and redo
			this.newValues = importConnection.getObject(id, "IDiagramModelArchimateConnection", version);
			
            // if the object references a relationship that is not referenced in the model, then we import it
            if ( (this.newValues.get("relationship_id") != null) && (this.model.getAllRelationships().get(this.newValues.get("relationship_id")) == null) ) {
                this.importRelationshipCommand = new DBImportRelationshipFromIdCommand(importConnection, model, null, (String)this.newValues.get("relationship_id"), 0, mustCreateCopy);
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
					this.importedViewConnection = (IDiagramModelConnection)DBCanvasFactory.eINSTANCE.create((String)this.newValues.get("class"));
				else
					this.importedViewConnection = (IDiagramModelConnection)DBArchimateFactory.eINSTANCE.create((String)this.newValues.get("class"));

				this.isNew = true;
			} else {
				// we must save the old values to allow undo
				DBMetadata metadata = ((IDBMetadata)this.importedViewConnection).getDBMetadata();
				
				this.oldInitialVersion = metadata.getInitialVersion();
				this.oldCurrentVersion = metadata.getCurrentVersion();
				this.oldDatabaseVersion = metadata.getDatabaseVersion();
				this.oldLatestDatabaseVersion = metadata.getLatestDatabaseVersion();

				this.oldName = metadata.getName();
				this.oldArchimateConcept = metadata.getArchimateConcept();
				this.oldIsLocked = metadata.isLocked();
				this.oldDocumentation = metadata.getDocumentation();
				this.oldLineColor = metadata.getLineColor();
				this.oldLineWidth = metadata.getLineWidth();
				this.oldFont = metadata.getFont();
				this.oldFontColor = metadata.getFontColor();
				this.oldType = metadata.getType();
				this.oldTextPosition = metadata.getTextPosition();
				this.oldSource = metadata.getSourceConnection();
				this.oldTarget = metadata.getTargetConnection();

				if ( metadata.getArchimateConcept()==null ) {
					this.oldProperties = new ArrayList<DBProperty>();
					for ( IProperty prop: ((IProperties)this.importedViewConnection).getProperties() ) {
						this.oldProperties.add(new DBProperty(prop.getKey(), prop.getValue()));
					}
				}

				this.oldBendpoints = new ArrayList<DBBendpoint>();
				for ( IDiagramModelBendpoint bendpoint: this.importedViewConnection.getBendpoints() ) {
					this.oldBendpoints.add(new DBBendpoint(bendpoint.getStartX(), bendpoint.getStartY(), bendpoint.getEndX(), bendpoint.getEndY()));
				}

				this.isNew = false;                
			}

			DBMetadata metadata = ((IDBMetadata)this.importedViewConnection).getDBMetadata();

			if ( this.mustCreateCopy ) {
				metadata.setId(this.model.getIDAdapter().getNewID());
				metadata.getInitialVersion().set(0, null, new Timestamp(Calendar.getInstance().getTime().getTime()));
			} else {
				metadata.setId((String)this.newValues.get("id"));
				metadata.getInitialVersion().set((int)this.newValues.get("version"), (String)this.newValues.get("checksum"), (Timestamp)this.newValues.get("created_on"));
			}

			metadata.getCurrentVersion().set(metadata.getInitialVersion());
			metadata.getDatabaseVersion().set(metadata.getInitialVersion());
			metadata.getLatestDatabaseVersion().set(metadata.getInitialVersion());

			if ( this.newValues.get("relationship_id") == null )
			    metadata.setName((String)this.newValues.get("name"));
			else
			    metadata.setArchimateConcept(this.model.getAllRelationships().get(this.newValues.get("relationship_id")));
			metadata.setLocked(this.newValues.get("is_locked"));
			metadata.setDocumentation((String)this.newValues.get("documentation"));
			metadata.setLineColor((String)this.newValues.get("line_color"));
			metadata.setLineWidth((Integer)this.newValues.get("line_width"));
			metadata.setFont((String)this.newValues.get("font"));
			metadata.setFontColor((String)this.newValues.get("font_color"));
			metadata.setType((Integer)this.newValues.get("type"));
			metadata.setTextPosition((Integer)this.newValues.get("text_position"));

			IConnectable source = this.model.getAllViewObjects().get(this.newValues.get("source_object_id"));
			if ( source == null ) source = this.model.getAllViewConnections().get(this.newValues.get("source_object_id"));
			if ( source == null ) {
				// source is another connection and may not be already loaded. So we register it for future resolution
				this.model.registerSourceConnection(this.importedViewConnection, (String)this.newValues.get("source_object_id"));
			} else {
				// source is an object and is reputed already imported, so we can set it right away
				metadata.setSourceConnection(source);
			}

			IConnectable target = this.model.getAllViewObjects().get(this.newValues.get("target_object_id"));
			if ( target == null ) target = this.model.getAllViewConnections().get(this.newValues.get("target_object_id"));
			if ( target == null ) {
				// target is another connection and may not be already loaded. So we register it for future resolution
				this.model.registerTargetConnection(this.importedViewConnection, (String)this.newValues.get("target_object_id"));
			} else {
				// target is an object and is reputed already imported, so we can set it right away
				metadata.setTargetConnection(target);
			}

			// If the connection has got properties but does not have a linked element, then it may have distinct properties
			if ( metadata.getArchimateConcept() == null ) {
	            this.importedViewConnection.getProperties().clear();
	            if ( this.newValues.get("properties") != null ) {
    	            for ( DBProperty newProperty: (ArrayList<DBProperty>)this.newValues.get("properties")) {
    	                IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
    	                prop.setKey(newProperty.getKey());
    	                prop.setValue(newProperty.getValue());
    	                this.importedViewConnection.getProperties().add(prop);
    	            }
	            }
			}

            this.importedViewConnection.getBendpoints().clear();
            if ( this.newValues.get("bendpoints") != null ) {
                for ( DBBendpoint newBendpoint: (ArrayList<DBBendpoint>)this.newValues.get("bendpoints")) {
                    IDiagramModelBendpoint bendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
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
				((IDBMetadata)view).getDBMetadata().setChecksumValid(false);

			this.model.countObject(this.importedViewConnection, false, null);
		} catch (Exception err) {
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
			DBMetadata metadata = ((IDBMetadata)this.importedViewConnection).getDBMetadata();

			metadata.getInitialVersion().set(this.oldInitialVersion);
			metadata.getCurrentVersion().set(this.oldCurrentVersion);
			metadata.getDatabaseVersion().set(this.oldDatabaseVersion);
			metadata.getLatestDatabaseVersion().set(this.oldLatestDatabaseVersion);

			if ( metadata.getArchimateConcept() != null ) metadata.setName(this.oldName);
			metadata.setArchimateConcept(this.oldArchimateConcept);
			metadata.setLocked(this.oldIsLocked);
			metadata.setDocumentation(this.oldDocumentation);
			metadata.setLineColor(this.oldLineColor);
			metadata.setLineWidth(this.oldLineWidth);
			metadata.setFont(this.oldFont);
			metadata.setFontColor(this.oldFontColor);
			metadata.setType(this.oldType);
			metadata.setTextPosition(this.oldTextPosition);

			metadata.setSourceConnection(this.oldSource);
			metadata.setTargetConnection(this.oldTarget);

			// If the object has got properties but does not have a linked relationship, then it may have distinct properties
			if ( this.oldProperties == null ) {
				((IProperties)this.importedViewConnection).getProperties().clear();
				for ( DBProperty pair: this.oldProperties ) {
					IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
					prop.setKey(pair.getKey());
					prop.setValue(pair.getValue());
					((IProperties)this.importedViewConnection).getProperties().add(prop);
				}
			}

			this.importedViewConnection.getBendpoints().clear();
			for ( DBBendpoint oldBendpoint: this.oldBendpoints ) {
				IDiagramModelBendpoint newBendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
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
