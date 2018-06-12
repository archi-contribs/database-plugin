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
import org.archicontribs.database.data.DBPair;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBCanvasFactory;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * Command for importing an view connection from it's ID.
 * 
 * @author Herve Jouin
 */
public class DBImportViewConnectionFromIdCommand extends CompoundCommand {
    private static final DBLogger logger = new DBLogger(DBImportViewConnectionFromIdCommand.class);
    
    private boolean commandHasBeenExecuted = false;		// to avoid being executed several times
    private Exception exception;
    
    private DBDatabaseImportConnection importConnection = null;
    private DBArchimateModel model = null;
    private IDiagramModelConnection importedViewConnection = null; 
    private String id = null;
    private int version = 0;
    private boolean mustCreateCopy = false;
    private boolean importedViewConnectionHasBeenCreated = false;
    
    // old values that need to be retain to allow undo
    private DBVersion oldInitialVersion;
    private DBVersion oldCurrentVersion;
    private DBVersion oldDatabaseVersion;
    private DBVersion oldLatestDatabaseVersion;
    
	private IArchimateConcept oldArchimateConcept = null;
	private String oldDocumentation =null;
	private Integer oldType = null;
	private String oldName = null;
	private Boolean oldIsLocked = null;
	private String oldLineColor = null;
	private Integer oldLineWidth = null;
	private String oldFont = null;
	private String oldFontColor = null;
	private Integer oldTextPosition = null;
	private ArrayList<DBPair<String, String>> oldProperties = null;
	private ArrayList<IDiagramModelBendpoint> oldBendpoints = null;
    
    /**
     * Imports a view connection into the model<br>
     * @param connection connection to the database
     * @param model model into which the view connection will be imported
     * @param id id of the view connection to import
     * @param version version of the view connection to import (0 if the latest version should be imported)
     * @param mustCreateCopy true if a copy must be imported (i.e. if a new id must be generated) or false if the view connection should be its original id
     */
    public DBImportViewConnectionFromIdCommand(DBDatabaseImportConnection connection, DBArchimateModel model, String id, int version, boolean mustCreateCopy) {
        this.importConnection = connection;
        this.model = model;
        this.id = id;
        this.version = version;
        this.mustCreateCopy = mustCreateCopy;
    }
    
    /**
     * @return the view connection that has been imported by the command (of course, the command must have been executed before)<br>
     * if the value is null, the exception that has been raised can be get using {@link getException}
     */
    public IDiagramModelConnection getImportedViewConnection() {
    	return this.importedViewConnection;
    }
    
    
    /**
     * @return the view object that has been imported by the command (of course, the command must have been executed before)
     */
    public Exception getException() {
        return this.exception;
    }

    @Override
    public boolean canExecute() {
        try {
            return (this.importConnection != null)
                    && (!this.importConnection.isClosed())
                    && (this.model != null)
                    && (this.id != null) ;
        } catch (SQLException err) {
            this.exception = err;
            return false;
        }
    }
    
    @Override
    public void execute() {
    	if ( this.commandHasBeenExecuted )
    		return;		// we do not execute it twice
    	
        if ( logger.isDebugEnabled() ) {
            if ( this.mustCreateCopy )
                logger.debug("   Importing a copy of view connection id "+this.id+".");
            else
                logger.debug("   Importing view connection id "+this.id+".");
        }

        String versionString = (this.version==0) ? "(SELECT MAX(version) FROM "+this.importConnection.getSchema()+"views_connections WHERE id = v.id)" : String.valueOf(this.version);

        try ( ResultSet result = this.importConnection.select("SELECT DISTINCT id, version, class, container_id, name, documentation, is_locked, line_color, line_width, font, font_color, relationship_id, source_object_id, target_object_id, text_position, type, checksum, created_on FROM "+this.importConnection.getSchema()+"views_connections v WHERE id = ? AND version = "+versionString, this.id) ) {
            if ( !result.next() ) {
                if ( this.version == 0 )
                    throw new Exception("View connection with id="+this.id+" has not been found in the database.");
                throw new Exception("View connection with id="+this.id+" and version="+this.version+" has not been found in the database.");
            }
            
            DBMetadata metadata;

			this.importedViewConnection = this.model.getAllViewConnections().get(this.id);
			if ( this.importedViewConnection == null || this.mustCreateCopy ) {
				if ( result.getString("class").startsWith("Canvas") )
					this.importedViewConnection = (IDiagramModelConnection)DBCanvasFactory.eINSTANCE.create(result.getString("class"));
				else
					this.importedViewConnection = (IDiagramModelConnection)DBArchimateFactory.eINSTANCE.create(result.getString("class"));

				((IIdentifier)this.importedViewConnection).setId(this.mustCreateCopy ? this.model.getIDAdapter().getNewID() : this.id);
				
				metadata = ((IDBMetadata)this.importedViewConnection).getDBMetadata();
				
				metadata.getInitialVersion().setVersion(1);
				metadata.getInitialVersion().setChecksum(result.getString("checksum"));
				metadata.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
				metadata.getCurrentVersion().setVersion(1);
				metadata.getCurrentVersion().setChecksum(result.getString("checksum"));
				metadata.getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
			} else {
				metadata = ((IDBMetadata)this.importedViewConnection).getDBMetadata();
				
				if ( result.getObject("relationship_id") == null ) this.oldName = metadata.getName();
				this.oldArchimateConcept = metadata.getArchimateConcept();
				this.oldIsLocked = metadata.isLocked();
				this.oldDocumentation = metadata.getDocumentation();
				this.oldLineColor = metadata.getLineColor();
				this.oldLineWidth = metadata.getLineWidth();
				this.oldFont = metadata.getFont();
				this.oldFontColor = metadata.getFontColor();
				this.oldType = metadata.getType();
				this.oldTextPosition = metadata.getTextPosition();
				
				if ( result.getString("relationship_id")==null ) {
					this.oldProperties = new ArrayList<DBPair<String, String>>();
					for ( IProperty prop: ((IProperties)this.importedViewConnection).getProperties() )
						this.oldProperties.add(new DBPair<String, String>(prop.getKey(), prop.getValue()));
				}
				
				this.oldBendpoints = new ArrayList<IDiagramModelBendpoint>();
				for ( IDiagramModelBendpoint bendpoint: this.importedViewConnection.getBendpoints() ) {
					IDiagramModelBendpoint bendpointCopy = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
					bendpointCopy.setStartX(bendpoint.getStartX());
					bendpointCopy.setStartY(bendpoint.getStartY());
					bendpointCopy.setEndX(bendpoint.getEndX());
					bendpointCopy.setEndY(bendpoint.getEndY());
					this.oldBendpoints.add(bendpointCopy);
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

			if ( this.importedViewConnection instanceof IDiagramModelArchimateConnection && result.getString("relationship_id") != null) {
				// we check that the relationship already exists. If not, we import it in shared mode
				IArchimateRelationship relationship = this.model.getAllRelationships().get(result.getString("relationship_id"));
				if ( relationship == null )
					this.add(new DBImportRelationshipFromIdCommand(this.importConnection, this.model, result.getString("relationship_id"), 0));
			}

			if ( this.model.getAllRelationships().get(result.getString("relationship_id")) != null ) metadata.setName(result.getString("name"));
			metadata.setArchimateConcept(this.model.getAllRelationships().get(result.getString("relationship_id")));
			metadata.setLocked(result.getObject("is_locked"));
			metadata.setDocumentation(result.getString("documentation"));
			metadata.setLineColor(result.getString("line_color"));
			metadata.setLineWidth(result.getInt("line_width"));
			metadata.setFont(result.getString("font"));
			metadata.setFontColor(result.getString("font_color"));
			metadata.setType(result.getInt("type"));
			metadata.setTextPosition(result.getInt("text_position"));
			metadata.setType(result.getInt("type"));
			
            IConnectable source = this.model.getAllViewObjects().get(result.getString("source_object_id"));
            IConnectable target = this.model.getAllViewObjects().get(result.getString("target_object_id"));
            
            if ( source != null ) {
                // source is an object and is reputed already imported, so we can set it right away
                this.importedViewConnection.setSource(source);
                source.addConnection(this.importedViewConnection);
            } else {
                // source is another connection and may not be already loaded. So we register it for future resolution
                this.model.registerSourceConnection(this.importedViewConnection, result.getString("source_object_id"));
            }
            
            if ( target != null ) {
                // target is an object and is reputed already imported, so we can set it right away
                this.importedViewConnection.setTarget(target);
                target.addConnection(this.importedViewConnection);
            } else {
                // target is another connection and may not be already loaded. So we register it for future resolution
                this.model.registerTargetConnection(this.importedViewConnection, result.getString("target_object_id"));
            }

            this.importedViewConnection.getBendpoints().clear();
			try ( ResultSet resultBendpoints = this.importConnection.select("SELECT start_x, start_y, end_x, end_y FROM "+this.importConnection.getSchema()+"bendpoints WHERE parent_id = ? AND parent_version = "+versionString+" ORDER BY rank", ((IIdentifier)this.importedViewConnection).getId()) ) {
				while(resultBendpoints.next()) {
					IDiagramModelBendpoint bendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
					bendpoint.setStartX(resultBendpoints.getInt("start_x"));
					bendpoint.setStartY(resultBendpoints.getInt("start_y"));
					bendpoint.setEndX(resultBendpoints.getInt("end_x"));
					bendpoint.setEndY(resultBendpoints.getInt("end_y"));
					this.importedViewConnection.getBendpoints().add(bendpoint);
				}
			}

			if ( logger.isDebugEnabled() ) logger.debug("   imported version "+((IDBMetadata)this.importedViewConnection).getDBMetadata().getInitialVersion().getVersion()+" of "+((IDBMetadata)this.importedViewConnection).getDBMetadata().getDebugName());
			
	        this.commandHasBeenExecuted = true;
        } catch (Exception err) {
            this.importedViewConnection = null;
            this.exception = err;
        }
    }
    
    @Override
    public void undo() {
    	if ( !this.commandHasBeenExecuted )
    		return;
        
        if ( this.importedViewConnectionHasBeenCreated ) {
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
            
			// If the object has got properties but does not have a linked relationship, then it may have distinct properties
			if ( metadata.getArchimateConcept() == null ) {
	            ((IProperties)this.importedViewConnection).getProperties().clear();
	            for ( DBPair<String, String> pair: this.oldProperties ) {
	            	IProperty prop = DBArchimateFactory.eINSTANCE.createProperty();
	            	prop.setKey(pair.getKey());
	            	prop.setValue(pair.getValue());
	            	((IProperties)this.importedViewConnection).getProperties().add(prop);
	            }
			}
			
			this.importedViewConnection.getBendpoints().clear();
			for ( IDiagramModelBendpoint bendpointCopy: this.oldBendpoints ) {
				IDiagramModelBendpoint bendpoint = DBArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
				bendpoint.setStartX(bendpointCopy.getStartX());
				bendpoint.setStartY(bendpointCopy.getStartY());
				bendpoint.setEndX(bendpointCopy.getEndX());
				bendpoint.setEndY(bendpointCopy.getEndY());
				this.importedViewConnection.getBendpoints().add(bendpoint);
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
        
		this.oldName = null;
		this.oldArchimateConcept = null;
		this.oldIsLocked = null;
		this.oldDocumentation = null;
		this.oldLineColor = null;
		this.oldLineWidth = null;
		this.oldFont = null;
		this.oldFontColor = null;
		this.oldType = null;
		this.oldTextPosition = null;
        
        this.oldProperties = null;
        this.oldBendpoints = null;
        
        this.importedViewConnection = null;
        this.model = null;
        this.id = null;
        this.importConnection = null;
    }
}
