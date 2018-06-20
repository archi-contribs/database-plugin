/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.DBArchimateModel;
import org.archicontribs.database.model.DBMetadata;
import org.archicontribs.database.model.IDBMetadata;
import org.archicontribs.database.model.impl.Folder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

import lombok.Getter;

/**
 * This class holds the information required to connect to, to import from and export to a database
 * 
 * @author Herve Jouin
 */
public class DBDatabaseExportConnection extends DBDatabaseConnection {
	private static final DBLogger logger = new DBLogger(DBDatabaseExportConnection.class);

	private boolean isImportconnectionDuplicate = false;

	/**
	 * This class variable stores the last commit transaction
	 * It will be used in every insert and update calls<br>
	 * This way, all requests in a transaction will have the same timestamp.
	 */
	private Timestamp lastTransactionTimestamp = null;



	/**
	 * Opens a connection to a JDBC database using all the connection details
	 */
	public DBDatabaseExportConnection(DBDatabaseEntry databaseEntry) throws ClassNotFoundException, SQLException {
		super(databaseEntry);
	}

	/**
	 * duplicates a connection to a JDBC database to allow switching between importConnection and exportConnection
	 */
	public DBDatabaseExportConnection(DBDatabaseConnection databaseConnection) {
		super();
		assert(databaseConnection != null);
		super.databaseEntry = databaseConnection.databaseEntry;
		super.schema = databaseConnection.schema;
		super.connection = databaseConnection.connection;
		this.isImportconnectionDuplicate = true;
	}

	@Getter private HashMap<String, DBMetadata> elementsNotInModel = new HashMap<String, DBMetadata>();
	@Getter private HashMap<String, DBMetadata> relationshipsNotInModel = new HashMap<String, DBMetadata>();
	@Getter private HashMap<String, DBMetadata> foldersNotInModel = new LinkedHashMap<String, DBMetadata>();			// must keep the order
	@Getter private HashMap<String, DBMetadata> viewsNotInModel = new HashMap<String, DBMetadata>();
	@Getter private HashMap<String, DBMetadata> viewObjectsNotInModel = new LinkedHashMap<String, DBMetadata>();		// must keep the order
	@Getter private HashMap<String, DBMetadata> viewConnectionsNotInModel = new LinkedHashMap<String, DBMetadata>();	// must keep the order
	@Getter private HashMap<String, DBMetadata> imagesNotInModel = new HashMap<String, DBMetadata>();
	@Getter private HashMap<String, DBMetadata> imagesNotInDatabase = new HashMap<String, DBMetadata>();

	public void getModelVersionsFromDatabase(DBArchimateModel model) throws SQLException {
		// we reset the variables
		this.elementsNotInModel.clear();
		this.relationshipsNotInModel.clear();
		this.foldersNotInModel.clear();
		this.viewsNotInModel.clear();
		this.viewObjectsNotInModel.clear();
		this.viewConnectionsNotInModel.clear();
		this.imagesNotInModel.clear();
		this.imagesNotInDatabase.clear();

		String modelId = model.getId();

		// we reset all the versions
		Iterator<Map.Entry<String, IArchimateElement>> ite = model.getAllElements().entrySet().iterator();
		while (ite.hasNext()) {
			DBMetadata metadata = ((IDBMetadata)ite.next().getValue()).getDBMetadata();
			metadata.getCurrentVersion().setVersion(0);
			metadata.getInitialVersion().reset();
			metadata.getDatabaseVersion().reset();
			metadata.getLatestDatabaseVersion().reset();
		}

		Iterator<Map.Entry<String, IArchimateRelationship>> itr = model.getAllRelationships().entrySet().iterator();
		while (itr.hasNext()) {
			DBMetadata metadata = ((IDBMetadata)itr.next().getValue()).getDBMetadata();
			metadata.getCurrentVersion().setVersion(0);
			metadata.getInitialVersion().reset();
			metadata.getDatabaseVersion().reset();
			metadata.getLatestDatabaseVersion().reset();
		}

		Iterator<Map.Entry<String, IFolder>> itf = model.getAllFolders().entrySet().iterator();
		while (itf.hasNext()) {
			DBMetadata metadata = ((IDBMetadata)itf.next().getValue()).getDBMetadata();
			metadata.getCurrentVersion().setVersion(0);
			metadata.getInitialVersion().reset();
			metadata.getDatabaseVersion().reset();
			metadata.getLatestDatabaseVersion().reset();
		}

		Iterator<Map.Entry<String, IDiagramModel>> itv = model.getAllViews().entrySet().iterator();
		while (itv.hasNext()) {
			DBMetadata metadata = ((IDBMetadata)itv.next().getValue()).getDBMetadata();
			metadata.getCurrentVersion().setVersion(0);
			metadata.getInitialVersion().reset();
			metadata.getDatabaseVersion().reset();
			metadata.getLatestDatabaseVersion().reset();
		}

		if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the model from the database");
		model.getCurrentVersion().reset();
		try ( ResultSet resultLatestVersion = select("SELECT version, checksum, created_on FROM "+this.schema+"models WHERE id = ? AND version = (SELECT MAX(version) FROM "+this.schema+"models WHERE id = ?)", modelId, modelId) ) {
			// we get the latest model version from the database
			if ( resultLatestVersion.next() && (resultLatestVersion.getObject("version") != null) ) {
				// if the version is found, then the model exists in the database
				model.getDatabaseVersion().setVersion(resultLatestVersion.getInt("version"));
				model.getDatabaseVersion().setChecksum(resultLatestVersion.getString("checksum"));
				model.getDatabaseVersion().setTimestamp(resultLatestVersion.getTimestamp("created_on"));

				// we check if the model has been imported from (or last exported to) this database
				if ( !model.getInitialVersion().getTimestamp().equals(DBVersion.NEVER) ) {
					try ( ResultSet resultCurrentVersion = select("SELECT version, checksum FROM "+this.schema+"models WHERE id = ? AND created_on = ?", modelId, model.getInitialVersion().getTimestamp()) ) {
						if ( resultCurrentVersion.next() && resultCurrentVersion.getObject("version") != null ) {
							// if the version is found, then the model has been imported from or last exported to the database 
							model.getInitialVersion().setVersion(resultCurrentVersion.getInt("version"));
							model.getInitialVersion().setChecksum(resultCurrentVersion.getString("checksum"));
						}
					}
				}
				logger.debug("The model already exists in the database:");
			} else {
				model.getDatabaseVersion().setVersion(0);

				logger.debug("The model does not exist in the database");
			}


			model.getCurrentVersion().setVersion(model.getDatabaseVersion().getVersion() );

			if ( logger.isDebugEnabled() ) {
				logger.debug("   Initial version = "+model.getInitialVersion().getVersion());
				logger.debug("   Current version = "+model.getCurrentVersion().getVersion());
				logger.debug("   Database version = "+model.getDatabaseVersion().getVersion());
			}
		}
	}

	/**
	 * Gets the version and checksum of an individual component from the database and fills its DBMetadata.<br>
	 * <br>
	 * Thus method is meant to be called during the export process for every component that is new in the model to check if it is shared with other models.<br>
	 * 
	 */
	public void getVersionFromDatabase(IIdentifier component) throws Exception {
		assert (component != null);
		String tableName;

		if ( component instanceof IArchimateElement ) tableName = getSchema()+"elements";
		else if ( component instanceof IArchimateRelationship ) tableName = getSchema()+"relationships";
		else if ( component instanceof IFolder ) tableName = getSchema()+"folders";
		else if ( component instanceof IDiagramModel ) tableName = getSchema()+"views";
		else if ( component instanceof IDiagramModelObject  ) tableName = getSchema()+"views_objects";
		else if ( component instanceof IDiagramModelConnection  ) tableName = getSchema()+"views_connections";
		else throw new Exception("Do not know how to get a "+component.getClass().getSimpleName()+" from the database.");

		DBMetadata metadata = ((IDBMetadata)component).getDBMetadata();
		boolean isLatest = true;

		try ( ResultSet result = select("SELECT version, checksum, created_on FROM "+tableName+" WHERE id = ? ORDER BY version DESC", component.getId()) ) {
			while ( result.next() ) {
				if ( isLatest ) {
					metadata.getDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
					metadata.getLatestDatabaseVersion().set(metadata.getDatabaseVersion());
					isLatest = false;
				}
				if ( DBPlugin.areEqual(result.getString("checksum"), metadata.getCurrentVersion().getChecksum()) ) {
					metadata.getInitialVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
					metadata.getCurrentVersion().setVersion(result.getInt("version"));
					metadata.getCurrentVersion().setTimestamp(result.getTimestamp("created_on"));
					break;
				}   
			}
		}
	}

	/**
	 * Gets the versions and checksum of one model's components from the database and fills their DBMetadata.<br>
	 * <br>
	 * Components that are not in the current model are set in elementsNotInModel, relationshipsNotInModel, foldersNotInModel and viewsNotInModel.<br>
	 * <br>
	 * This method is meant to be called during the export process, before the export of the components as it sets the DBVersion variables used during the export process.
	 */
	public void getVersionsFromDatabase(DBArchimateModel model) throws SQLException, RuntimeException {
		// This method can retrieve versions only if the database contains the whole model tables
		assert(!this.databaseEntry.isWholeModelExported());

		int modelInitialVersion = model.getInitialVersion().getVersion();

		// if the model is brand new, there is no point to check for its content in the database
		if ( modelInitialVersion != 0 ) {
			String modelId = model.getId();
			int modelDatabaseVersion = model.getDatabaseVersion().getVersion();
			int modelCurrentVersion = model.getCurrentVersion().getVersion();
			
			// we get the components versions from the database.

			// elements
			if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the elements from the database");
			try ( ResultSet result = select(
					"SELECT id, name, version, checksum, created_on, model_id, model_version"
							+ " FROM "+this.schema+"elements"
							+ " LEFT JOIN "+this.schema+"elements_in_model ON element_id = id AND element_version = version"
							+ " WHERE id IN (SELECT id FROM "+this.schema+"elements JOIN "+this.schema+"elements_in_model ON element_id = id AND element_version = version WHERE model_id = ?)"
							+ " ORDER BY id, version, model_version"
							,modelId
					) ) {
				String previousId = null;
				DBMetadata previousComponent = null;
				while ( result.next() ) {
					DBMetadata currentComponent;
					String currentId = result.getString("id");

					if ( DBPlugin.areEqual(currentId, previousId) )
						currentComponent = previousComponent;
					else {
						IArchimateModelObject object = model.getAllElements().get(currentId);
						currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();
					}

					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(null);
						this.elementsNotInModel.put(result.getString("id"), currentComponent);
					}

					if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
						// if the component is part of the model, we compare with the model's version
						if ( result.getInt("model_version") == modelInitialVersion )
							currentComponent.getInitialVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
						if ( result.getInt("model_version") == modelDatabaseVersion )
							currentComponent.getDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
					}

					// components are sorted by version (so also by timestamp) so the latest found is the latest in time
					currentComponent.getLatestDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));

					currentComponent.getCurrentVersion().setVersion(result.getInt("version"));
				}
			}

			// relationships
			if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the relationships from the database");
			try ( ResultSet result = select(
					"SELECT id, name, version, checksum, created_on, model_id, model_version"
							+ " FROM "+this.schema+"relationships"
							+ " LEFT JOIN "+this.schema+"relationships_in_model ON relationship_id = id AND relationship_version = version"
							+ " WHERE id IN (SELECT id FROM "+this.schema+"relationships JOIN "+this.schema+"relationships_in_model ON relationship_id = id AND relationship_version = version WHERE model_id = ?)"
							+ " ORDER BY id, version, model_version"
							,modelId
					) ) {
				String previousId = null;
				DBMetadata previousComponent = null;
				while ( result.next() ) {
					DBMetadata currentComponent;
					String currentId = result.getString("id");

					if ( DBPlugin.areEqual(currentId, previousId) )
						currentComponent = previousComponent;
					else {
						IArchimateModelObject object = model.getAllRelationships().get(currentId);
						currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();
					}

					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(null);
						this.relationshipsNotInModel.put(result.getString("id"), currentComponent);
					}

					if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
						// if the component is part of the model, we compare with the model's version
						if ( result.getInt("model_version") == modelInitialVersion )
							currentComponent.getInitialVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
						if ( result.getInt("model_version") == modelDatabaseVersion )
							currentComponent.getDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
					}

					// components are sorted by version (so also by timestamp) so the latest found is the latest in time
					currentComponent.getLatestDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));

					currentComponent.getCurrentVersion().setVersion(result.getInt("version"));
				}
			}

			// folders
			if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the folders from the database");
			try ( ResultSet result = select(
					"SELECT id, name, version, checksum, created_on, model_id, model_version"
							+ " FROM "+this.schema+"folders"
							+ " LEFT JOIN "+this.schema+"folders_in_model ON folder_id = id AND folder_version = version"
							+ " WHERE id IN (SELECT id FROM "+this.schema+"folders JOIN "+this.schema+"folders_in_model ON folder_id = id AND folder_version = version WHERE model_id = ?)"
							+ " ORDER BY id, version, model_version"
							,modelId
					) ) {
				String previousId = null;
				DBMetadata previousComponent = null;
				while ( result.next() ) {
					DBMetadata currentComponent;
					String currentId = result.getString("id");

					if ( DBPlugin.areEqual(currentId, previousId) )
						currentComponent = previousComponent;
					else {
						IArchimateModelObject object = model.getAllFolders().get(currentId);
						currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();
					}

					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(null);
						this.foldersNotInModel.put(result.getString("id"), currentComponent);
					}

					if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
						// if the component is part of the model, we compare with the model's version
						if ( result.getInt("model_version") == modelInitialVersion )
							currentComponent.getInitialVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
						if ( result.getInt("model_version") == modelDatabaseVersion )
							currentComponent.getDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));
					}

					// components are sorted by version (so also by timestamp) so the latest found is the latest in time
					currentComponent.getLatestDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getTimestamp("created_on"));

					currentComponent.getCurrentVersion().setVersion(result.getInt("version"));
				}
			}

			if ( logger.isDebugEnabled() ) logger.debug("Getting versions of the views from the database");
			try ( ResultSet result = select(
					"SELECT id, name, version, checksum, container_checksum, created_on, model_id, model_version"
							+ " FROM "+this.schema+"views"
							+ " LEFT JOIN "+this.schema+"views_in_model ON view_id = id AND view_version = version"
							+ " WHERE id IN (SELECT id FROM "+this.schema+"views JOIN "+this.schema+"views_in_model ON view_id = id AND view_version = version WHERE model_id = ?)"
							+ " ORDER BY id, version, model_version"
							,modelId
					) ) {
				String previousId = null;
				DBMetadata previousComponent = null;
				while ( result.next() ) {
					DBMetadata currentComponent;
					String currentId = result.getString("id");

					if ( DBPlugin.areEqual(currentId, previousId) )
						currentComponent = previousComponent;
					else {
						IArchimateModelObject object = model.getAllViews().get(currentId);
						currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();
					}

					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(null);
						this.viewsNotInModel.put(result.getString("id"), currentComponent);
					}

					if ( DBPlugin.areEqual(result.getString("model_id"), modelId) ) {
						// if the component is part of the model, we compare with the model's version
						if ( result.getInt("model_version") == modelInitialVersion )
							currentComponent.getInitialVersion().set(result.getInt("version"), result.getString("checksum"), result.getString("container_checksum"), result.getTimestamp("created_on"));
						if ( result.getInt("model_version") == modelDatabaseVersion )
							currentComponent.getDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getString("container_checksum"), result.getTimestamp("created_on"));
					}

					// components are sorted by version (so also by timestamp) so the latest found is the latest in time
					currentComponent.getLatestDatabaseVersion().set(result.getInt("version"), result.getString("checksum"), result.getString("container_checksum"), result.getTimestamp("created_on"));

					currentComponent.getCurrentVersion().setVersion(result.getInt("version"));
				}
			}

			if ( modelCurrentVersion == 1 ) {
				// even if the model does not exist in the database, the images can exist in the database
				// images do not have a version as they cannot be modified. Their path is a checksum and loading a new image creates a new path.

				// at last, we check if all the images in the model are in the database
				if ( logger.isDebugEnabled() ) logger.debug("Checking if the images exist in the database");
				for ( String path: model.getAllImagePaths() ) {
					try ( ResultSet result = select("SELECT path from "+this.schema+"images where path = ?", path) ) {
						if ( result.next() && result.getObject("path") != null ) {
							// the image is in the database
						} else {
							// the image is not in the database
							this.imagesNotInDatabase.put(path, new DBMetadata(null));
						}
					}
				}
			} else {
				// we check if the latest version of the model has got images that are not in the model
				if ( logger.isDebugEnabled() ) logger.debug("Checking missing images from the database");
				try ( ResultSet result = select ("SELECT DISTINCT image_path FROM "+this.schema+"views_objects "
						+ "JOIN "+this.schema+"views_objects_in_view ON views_objects_in_view.object_id = views_objects.id AND views_objects_in_view.object_version = views_objects.version "
						+ "JOIN "+this.schema+"views_in_model ON views_in_model.view_id = views_objects_in_view.view_id AND views_in_model.view_version = views_objects_in_view.view_version "
						+ "WHERE image_path IS NOT NULL AND views_in_model.model_id = ? AND views_in_model.model_version = ?"
						,model.getId()
						,model.getDatabaseVersion().getVersion()
						) ) {
					while ( result.next() ) {
						if ( !model.getAllImagePaths().contains(result.getString("image_path")) ) {
							this.imagesNotInModel.put(result.getString("image_path"), new DBMetadata(null));
						}
					}
				}
			}
		}
	}

	public void getViewObjectsAndConnectionsVersionsFromDatabase(DBArchimateModel model, IDiagramModel view) throws SQLException, RuntimeException {
		// if the model is brand new, there is no point to check for its content in the database
		if ( model.getInitialVersion().getVersion() != 0 ) {
			String viewId = view.getId();
			
			Iterator<Map.Entry<String, IDiagramModelObject>> itvo = model.getAllViewObjects().entrySet().iterator();
			while (itvo.hasNext()) {
				IDiagramModelObject object = itvo.next().getValue();
				if ( object.getDiagramModel() == view ) {
					DBMetadata metadata = ((IDBMetadata)object).getDBMetadata();
					metadata.getCurrentVersion().setVersion(0);
					metadata.getInitialVersion().reset();
					metadata.getDatabaseVersion().reset();
					metadata.getLatestDatabaseVersion().reset();
				}
			}
	
			Iterator<Map.Entry<String, IDiagramModelConnection>> itvc = model.getAllViewConnections().entrySet().iterator();
			while (itvc.hasNext()) {
				IDiagramModelConnection cnct = itvc.next().getValue();
				if ( cnct.getDiagramModel() == view ) {
					DBMetadata metadata = ((IDBMetadata)cnct).getDBMetadata();
					metadata.getCurrentVersion().setVersion(0);
					metadata.getInitialVersion().reset();
					metadata.getDatabaseVersion().reset();
					metadata.getLatestDatabaseVersion().reset();
				}
			}
	
			int viewInitialVersion = ((IDBMetadata)view).getDBMetadata().getInitialVersion().getVersion();
			int viewDatabaseVersion = ((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().getVersion();
	
			// view objects
			if ( logger.isDebugEnabled() ) logger.debug("Getting versions of view objects from the database for "+((IDBMetadata)view).getDBMetadata().getDebugName());
			try ( ResultSet result = select(
					"SELECT id, name, version, checksum, created_on, view_id, view_version"
							+ " FROM "+this.schema+"views_objects"
							+ " LEFT JOIN "+this.schema+"views_objects_in_view ON object_id = id AND object_version = version"
							+ " WHERE id IN (SELECT id FROM "+this.schema+"views_objects JOIN "+this.schema+"views_objects_in_view ON object_id = id AND object_version = version WHERE view_id = ?)"
							+ " ORDER BY id, version, view_version"
							,viewId
					) ) {
				String previousId = null;
				DBMetadata previousComponent = null;
				while ( result.next() ) {
					DBMetadata currentComponent;
					String currentId = result.getString("id");
	
					if ( DBPlugin.areEqual(currentId, previousId) )
						currentComponent = previousComponent;
					else {
						IDiagramModelObject object = model.getAllViewObjects().get(currentId);
						currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();
					}
	
					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(null);
						this.viewObjectsNotInModel.put(result.getString("id"), currentComponent);
					}
	
					if ( DBPlugin.areEqual(result.getString("view_id"), viewId) ) {
						// if the component is part of the model, we compare with the model's version
						if ( result.getInt("view_version") == viewInitialVersion ) {
							currentComponent.getInitialVersion().setVersion(result.getInt("version"));
							currentComponent.getInitialVersion().setChecksum(result.getString("checksum"));
							currentComponent.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
						}
						if ( result.getInt("view_version") == viewDatabaseVersion ) {
							currentComponent.getDatabaseVersion().setVersion(result.getInt("version"));
							currentComponent.getDatabaseVersion().setChecksum(result.getString("checksum"));
							currentComponent.getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
						}
					}
	
					// components are sorted by version (so also by timestamp) so the latest found is the latest in time
					currentComponent.getLatestDatabaseVersion().setVersion(result.getInt("version"));
					currentComponent.getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
					currentComponent.getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	
					currentComponent.getCurrentVersion().setVersion(result.getInt("version"));
				}
			}
	
			// view connections
			if ( logger.isDebugEnabled() ) logger.debug("Getting versions of view connections from the database for "+((IDBMetadata)view).getDBMetadata().getDebugName());
			try ( ResultSet result = select(
					"SELECT id, name, version, checksum, created_on, view_id, view_version"
							+ " FROM "+this.schema+"views_connections"
							+ " LEFT JOIN "+this.schema+"views_connections_in_view ON connection_id = id AND connection_version = version"
							+ " WHERE id IN (SELECT id FROM "+this.schema+"views_connections JOIN "+this.schema+"views_connections_in_view ON connection_id = id AND connection_version = version WHERE view_id = ?)"
							+ " ORDER BY id, version, view_version"
							,viewId
					) ) {
				String previousId = null;
				DBMetadata previousComponent = null;
				while ( result.next() ) {
					DBMetadata currentComponent;
					String currentId = result.getString("id");
	
					if ( DBPlugin.areEqual(currentId, previousId) )
						currentComponent = previousComponent;
					else {
						IDiagramModelConnection object = model.getAllViewConnections().get(currentId);
						currentComponent = (object == null ) ? null : ((IDBMetadata)object).getDBMetadata();
					}
	
					// the loop returns all the versions of all the model components
					if ( currentComponent == null ) {
						currentComponent = new DBMetadata(null);
						this.viewConnectionsNotInModel.put(result.getString("id"), currentComponent);
					}
	
					if ( DBPlugin.areEqual(result.getString("view_id"), viewId) ) {
						// if the component is part of the model, we compare with the model's version
						if ( result.getInt("view_version") == viewInitialVersion ) {
							currentComponent.getInitialVersion().setVersion(result.getInt("version"));
							currentComponent.getInitialVersion().setChecksum(result.getString("checksum"));
							currentComponent.getInitialVersion().setTimestamp(result.getTimestamp("created_on"));
						}
						if ( result.getInt("view_version") == viewDatabaseVersion ) {
							currentComponent.getDatabaseVersion().setVersion(result.getInt("version"));
							currentComponent.getDatabaseVersion().setChecksum(result.getString("checksum"));
							currentComponent.getDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
						}
					}
	
					// components are sorted by version (so also by timestamp) so the latest found is the latest in time
					currentComponent.getLatestDatabaseVersion().setVersion(result.getInt("version"));
					currentComponent.getLatestDatabaseVersion().setChecksum(result.getString("checksum"));
					currentComponent.getLatestDatabaseVersion().setTimestamp(result.getTimestamp("created_on"));
	
					currentComponent.getCurrentVersion().setVersion(result.getInt("version"));
				}
			}
		}
	}

	/**
	 * Empty a Neo4J database
	 */
	public void emptyNeo4jDB() throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Emptying Neo4J database.");
		request("MATCH (n) DETACH DELETE n");
	}


	/**
	 * Exports the model metadata into the database
	 */
	public void exportModel(DBArchimateModel model, String releaseNote) throws Exception {
		final String[] modelsColumns = {"id", "version", "name", "note", "purpose", "created_by", "created_on", "checksum"};

		if ( (model.getName() == null) || (model.getName().equals("")) )
			throw new RuntimeException("Model name cannot be empty.");
		
		model.getCurrentVersion().setVersion(model.getDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting model (initial version = "+model.getInitialVersion().getVersion()+", exported version = "+model.getCurrentVersion().getVersion()+", latest database version = "+model.getDatabaseVersion().getVersion()+")");

		if ( this.connection.getAutoCommit() )
			model.getCurrentVersion().setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()));
		else
			model.getCurrentVersion().setTimestamp(this.lastTransactionTimestamp);

		insert(this.schema+"models", modelsColumns
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,model.getName()
				,releaseNote
				,model.getPurpose()
				,DBPlugin.getUserName()
				,model.getCurrentVersion().getTimestamp()
				,model.getCurrentVersion().getChecksum()
				);

		exportProperties(model);
		exportMetadata(model);
	}

	/**
	 * Export a component to the database
	 */
	public void exportEObject(EObject eObject, DBGui gui) throws Exception {
		if ( eObject instanceof IArchimateElement ) 			exportElement((IArchimateElement)eObject);
		else if ( eObject instanceof IArchimateRelationship ) 	exportRelationship((IArchimateRelationship)eObject);
		else if ( eObject instanceof IFolder ) 					exportFolder((IFolder)eObject);
		else if ( eObject instanceof IDiagramModel ) 			exportView((IDiagramModel)eObject, gui);
		else if ( eObject instanceof IDiagramModelObject )		exportViewObject((IDiagramModelComponent)eObject);
		else if ( eObject instanceof IDiagramModelConnection )	exportViewConnection((IDiagramModelConnection)eObject);
		else
			throw new Exception("Do not know how to export "+eObject.getClass().getSimpleName());
	}

	public void assignEObjectToModel(EObject eObject) throws Exception {
		if ( eObject instanceof IArchimateElement )				assignElementToModel((IArchimateElement)eObject);
		else if ( eObject instanceof IArchimateRelationship )	assignRelationshipToModel((IArchimateRelationship)eObject);
		else if ( eObject instanceof IFolder )					assignFolderToModel((IFolder)eObject);
		else if ( eObject instanceof IDiagramModel )			assignViewToModel((IDiagramModel)eObject);
		else if ( eObject instanceof IDiagramModelObject )		assignViewObjectToView((IDiagramModelObject)eObject);
		else if ( eObject instanceof IDiagramModelConnection )	assignViewConnectionToView((IDiagramModelConnection)eObject);
		else
			throw new Exception("Do not know how to assign to the model : "+eObject.getClass().getSimpleName());
	}

	/**
	 * Export an element to the database
	 */
	private void exportElement(IArchimateConcept element) throws Exception {
		final String[] elementsColumns = {"id", "version", "class", "name", "type", "documentation", "created_by", "created_on", "checksum"};

		// if the element is exported, the we increase its exportedVersion
		((IDBMetadata)element).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)element).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)element).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)element).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)element).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			// TODO : USE MERGE instead to replace existing nodes
			request("CREATE (new:elements {id:?, version:?, class:?, name:?, type:?, documentation:?, checksum:?})"
					,element.getId()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
					,element.getDocumentation()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getChecksum()
					);
		} else {
			insert(this.schema+"elements", elementsColumns
					,element.getId()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()
					,element.getClass().getSimpleName()
					,element.getName()
					,((element instanceof IJunction) ? ((IJunction)element).getType() : null)
					,element.getDocumentation()
					,DBPlugin.getUserName()
					,((DBArchimateModel)element.getArchimateModel()).getCurrentVersion().getTimestamp()
					,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getChecksum()
					);
		}

		exportProperties(element);
	}

	/**
	 * This class variable allows to sort the exported elements that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int elementRank = 0;

	/**
	 * Assign an element to a model into the database
	 */
	private void assignElementToModel(IArchimateConcept element) throws Exception {
		final String[] elementsInModelColumns = {"element_id", "element_version", "parent_folder_id", "model_id", "model_version", "rank"};
		DBArchimateModel model = (DBArchimateModel)element.getArchimateModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning element to model");

		insert(this.schema+"elements_in_model", elementsInModelColumns
				,element.getId()
				,((IDBMetadata)element).getDBMetadata().getCurrentVersion().getVersion()   // we use currentVersion as it has been set in exportElement()
				,((IFolder)element.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.elementRank
				);
	}

	/**
	 * Export a relationship to the database
	 */
	private void exportRelationship(IArchimateConcept relationship) throws Exception {
		final String[] relationshipsColumns = {"id", "version", "class", "name", "documentation", "source_id", "target_id", "strength", "access_type", "created_by", "created_on", "checksum"};

		// if the relationship is exported, the we increase its exportedVersion
		((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)relationship).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)relationship).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)relationship).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)relationship).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
			String relationshipType = (this.databaseEntry.isNeo4jTypedRelationship() ? (relationship.getClass().getSimpleName()+"s") : "relationships");
			// TODO : USE MERGE instead to replace existing nodes
			if ( this.databaseEntry.isNeo4jNativeMode() ) {
				if ( (((IArchimateRelationship)relationship).getSource() instanceof IArchimateElement) && (((IArchimateRelationship)relationship).getTarget() instanceof IArchimateElement) ) {
					request("MATCH (source:elements {id:?, version:?}), (target:elements {id:?, version:?}) CREATE (source)-[relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}]->(target)"
							,((IArchimateRelationship)relationship).getSource().getId()
							,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion().getVersion()
							,((IArchimateRelationship)relationship).getTarget().getId()
							,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion().getVersion()
							,relationship.getId()
							,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
							,relationship.getClass().getSimpleName()
							,relationship.getName()
							,relationship.getDocumentation()
							,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
							,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
							,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
							);
				}
			} else {
				request("MATCH (source {id:?, version:?}), (target {id:?, version:?}) CREATE (relationship:"+relationshipType+" {id:?, version:?, class:?, name:?, documentation:?, strength:?, access_type:?, checksum:?}), (source)-[rel1:relatedTo]->(relationship)-[rel2:relatedTo]->(target)"
						,((IArchimateRelationship)relationship).getSource().getId()
						,((IDBMetadata)((IArchimateRelationship)relationship).getSource()).getDBMetadata().getCurrentVersion().getVersion()
						,((IArchimateRelationship)relationship).getTarget().getId()
						,((IDBMetadata)((IArchimateRelationship)relationship).getTarget()).getDBMetadata().getCurrentVersion().getVersion()
						,relationship.getId()
						,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
						,relationship.getClass().getSimpleName()
						,relationship.getName()
						,relationship.getDocumentation()
						,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
						,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
						,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
						);
			}
		} else {
			insert(this.schema+"relationships", relationshipsColumns
					,relationship.getId()
					,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
					,relationship.getClass().getSimpleName()
					,relationship.getName()
					,relationship.getDocumentation()
					,((IArchimateRelationship)relationship).getSource().getId()
					,((IArchimateRelationship)relationship).getTarget().getId()
					,((relationship instanceof IInfluenceRelationship) ? ((IInfluenceRelationship)relationship).getStrength() : null)
					,((relationship instanceof IAccessRelationship) ? ((IAccessRelationship)relationship).getAccessType() : null)
					,DBPlugin.getUserName()
					,((DBArchimateModel)relationship.getArchimateModel()).getCurrentVersion().getTimestamp()
					,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getChecksum()
					);
		}

		exportProperties(relationship);
	}

	/**
	 * This class variable allows to sort the exported relationships that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int relationshipRank = 0;

	/**
	 * Assign a relationship to a model into the database
	 */
	private void assignRelationshipToModel(IArchimateConcept relationship) throws Exception {
		final String[] relationshipsInModelColumns = {"relationship_id", "relationship_version", "parent_folder_id", "model_id", "model_version", "rank"};

		DBArchimateModel model = (DBArchimateModel)relationship.getArchimateModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning relationship to model");

		insert(this.schema+"relationships_in_model", relationshipsInModelColumns
				,relationship.getId()
				,((IDBMetadata)relationship).getDBMetadata().getCurrentVersion().getVersion()
				,((IFolder)relationship.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.relationshipRank
				);
	}

	/**
	 * Export a folder into the database.
	 */
	private void exportFolder(IFolder folder) throws Exception {
		final String[] foldersColumns = {"id", "version", "type", "root_type", "name", "documentation", "created_by", "created_on", "checksum"};

		// if the folder is exported, the we increase its exportedVersion
		((IDBMetadata)folder).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)folder).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)folder).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)folder).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)folder).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		insert(this.schema+"folders", foldersColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
				,folder.getType().getValue()
				,((IDBMetadata)folder).getDBMetadata().getRootFolderType()
				,folder.getName()
				,folder.getDocumentation()
				,DBPlugin.getUserName()
				,((DBArchimateModel)folder.getArchimateModel()).getCurrentVersion().getTimestamp()
				,((Folder)folder).getDBMetadata().getCurrentVersion().getChecksum()
				);

		exportProperties(folder);
	}


	/**
	 * This class variable allows to sort the exported folders that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int folderRank = 0;

	/**
	 * Assign a folder to a model into the database
	 */
	private void assignFolderToModel(IFolder folder) throws Exception {
		final String[] foldersInModelColumns = {"folder_id", "folder_version", "parent_folder_id", "model_id", "model_version", "rank"};
		DBArchimateModel model = (DBArchimateModel)folder.getArchimateModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning folder to model");

		insert(this.schema+"folders_in_model", foldersInModelColumns
				,folder.getId()
				,((IDBMetadata)folder).getDBMetadata().getCurrentVersion().getVersion()
				,(((IIdentifier)((Folder)folder).eContainer()).getId() == model.getId() ? null : ((IIdentifier)((Folder)folder).eContainer()).getId())
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.folderRank
				);
	}

	/**
	 * Export a view into the database.
	 */
	private void exportView(IDiagramModel view, DBGui gui) throws Exception {
		final String[] ViewsColumns = {"id", "version", "class", "created_by", "created_on", "name", "connection_router_type", "documentation", "hint_content", "hint_title", "viewpoint", "background", "screenshot", "checksum", "container_checksum"};

		// if the view is exported, the we increase its exportedVersion
		((IDBMetadata)view).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)view).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)view).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)view).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)view).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		byte[] viewImage = null;

		if ( this.databaseEntry.isViewSnapshotRequired() )
			viewImage = gui.createImage(view, this.databaseEntry.getViewsImagesScaleFactor()/100.0, this.databaseEntry.getViewsImagesBorderWidth());

		insert(this.schema+"views", ViewsColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
				,view.getClass().getSimpleName()
				,DBPlugin.getUserName()
				,((DBArchimateModel)view.getArchimateModel()).getCurrentVersion().getTimestamp()
				,view.getName()
				,view.getConnectionRouterType()
				,view.getDocumentation()
				,((view instanceof IHintProvider) ? ((IHintProvider)view).getHintContent() : null)
				,((view instanceof IHintProvider) ? ((IHintProvider)view).getHintTitle() : null)
				,((view instanceof IArchimateDiagramModel) ? ((IArchimateDiagramModel)view).getViewpoint() : null)
				,((view instanceof ISketchModel) ? ((ISketchModel)view).getBackground() : null)
				,viewImage
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getChecksum()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getContainerChecksum()
				);

		viewImage = null;		// to force memory release

		exportProperties(view);
	}

	/**
	 * This class variable allows to sort the exported views that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int viewRank = 0;

	/**
	 * Assign a view to a model into the database
	 */
	private void assignViewToModel(IDiagramModel view) throws Exception {
		final String[] viewsInModelColumns = {"view_id", "view_version", "parent_folder_id", "model_id", "model_version", "rank"};
		DBArchimateModel model = (DBArchimateModel)view.getArchimateModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning view to model");

		insert(this.schema+"views_in_model", viewsInModelColumns
				,view.getId()
				,((IDBMetadata)view).getDBMetadata().getCurrentVersion().getVersion()
				,((IFolder)view.eContainer()).getId()
				,model.getId()
				,model.getCurrentVersion().getVersion()
				,++this.viewRank
				);
	}

	/**
	 * This class variable allows to sort the exported views objects that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int viewObjectRank = 0;

	/**
	 * Export a view object into the database.<br>
	 * The rank allows to order the views during the import process.
	 */
	private void exportViewObject(IDiagramModelComponent viewObject) throws Exception {
		final String[] ViewsObjectsColumns = {"id", "version", "class", "container_id", "element_id", "diagram_ref_id", "type", "border_color", "border_type", "content", "documentation", "hint_content", "hint_title", "is_locked", "image_path", "image_position", "line_color", "line_width", "fill_color", "font", "font_color", "name", "notes", "text_alignment", "text_position", "x", "y", "width", "height", "created_by", "created_on", "checksum"};

		// if the viewObject is exported, the we increase its exportedVersion
		((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)viewObject).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)viewObject).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)viewObject).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)viewObject).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)viewObject).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		insert(this.schema+"views_objects", ViewsObjectsColumns
				,((IIdentifier)viewObject).getId()
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()
				,viewObject.getClass().getSimpleName()
				,((IIdentifier)viewObject.eContainer()).getId()
				,((viewObject instanceof IDiagramModelArchimateComponent) ? ((IDiagramModelArchimateComponent)viewObject).getArchimateConcept().getId() : null)
				,((viewObject instanceof IDiagramModelReference) ? ((IDiagramModelReference)viewObject).getReferencedModel().getId() : null)
				,((viewObject instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)viewObject).getType() : null)
				,((viewObject instanceof IBorderObject) ? ((IBorderObject)viewObject).getBorderColor() : null)
				,((viewObject instanceof IDiagramModelNote) ? ((IDiagramModelNote)viewObject).getBorderType() : null)
				,((viewObject instanceof ITextContent) ? ((ITextContent)viewObject).getContent() : null)
				,((viewObject instanceof IDocumentable && !(viewObject instanceof IDiagramModelArchimateComponent)) ? ((IDocumentable)viewObject).getDocumentation() : null)        // They have got there own documentation. The others use the documentation of the corresponding ArchimateConcept
				,((viewObject instanceof IHintProvider) ? ((IHintProvider)viewObject).getHintContent() : null)
				,((viewObject instanceof IHintProvider) ? ((IHintProvider)viewObject).getHintTitle() : null)
				//TODO : add helpHintcontent and helpHintTitle
				,((viewObject instanceof ILockable) ? (((ILockable)viewObject).isLocked()?1:0) : null)
				,((viewObject instanceof IDiagramModelImageProvider) ? ((IDiagramModelImageProvider)viewObject).getImagePath() : null)
				,((viewObject instanceof IIconic) ? ((IIconic)viewObject).getImagePosition() : null)
				,((viewObject instanceof ILineObject) ? ((ILineObject)viewObject).getLineColor() : null)
				,((viewObject instanceof ILineObject) ? ((ILineObject)viewObject).getLineWidth() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getFillColor() : null)
				,((viewObject instanceof IFontAttribute) ? ((IFontAttribute)viewObject).getFont() : null)
				,((viewObject instanceof IFontAttribute) ? ((IFontAttribute)viewObject).getFontColor() : null)
				,viewObject.getName()																						// we export the name because it will be used in case of conflict
				,((viewObject instanceof ICanvasModelSticky) ? ((ICanvasModelSticky)viewObject).getNotes() : null)
				,((viewObject instanceof ITextAlignment) ? ((ITextAlignment)viewObject).getTextAlignment() : null)
				,((viewObject instanceof ITextPosition) ? ((ITextPosition)viewObject).getTextPosition() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getX() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getY() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getWidth() : null)
				,((viewObject instanceof IDiagramModelObject) ? ((IDiagramModelObject)viewObject).getBounds().getHeight() : null)
				,DBPlugin.getUserName()
				,((DBArchimateModel)viewObject.getDiagramModel().getArchimateModel()).getCurrentVersion().getTimestamp()
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getChecksum()
				);

		if ( viewObject instanceof IProperties && !(viewObject instanceof IDiagramModelArchimateComponent))
			exportProperties((IProperties)viewObject);
	}

	/**
	 * Assign a view Object to a view into the database
	 */
	private void assignViewObjectToView(IDiagramModelComponent viewObject) throws Exception {
		final String[] viewObjectInViewColumns = {"object_id", "object_version", "view_id", "view_version", "rank"};
		IDiagramModel viewContainer = viewObject.getDiagramModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning view object to view");

		insert(this.schema+"views_objects_in_view", viewObjectInViewColumns
				,viewObject.getId()
				,((IDBMetadata)viewObject).getDBMetadata().getCurrentVersion().getVersion()
				,viewContainer.getId()
				,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getVersion()
				,++this.viewObjectRank
				);


	}

	/**
	 * This class variable allows to sort the exported views objects that they are imported in the same order<br>
	 * It is reset to zero each time a connection to a new database is done (connection() method).
	 */
	private int viewConnectionRank = 0;

	/**
	 * Export a view connection into the database.<br>
	 * The rank allows to order the views during the import process.
	 */
	private void exportViewConnection(IDiagramModelConnection viewConnection) throws Exception {
		final String[] ViewsConnectionsColumns = {"id", "version", "class", "container_id", "name", "documentation", "is_locked", "line_color", "line_width", "font", "font_color", "relationship_id", "source_object_id", "target_object_id", "text_position", "type", "created_by", "created_on", "checksum"};
		final String[] bendpointsColumns = {"parent_id", "parent_version", "rank", "start_x", "start_y", "end_x", "end_y"};

		// if the viewConnection is exported, the we increase its exportedVersion
		((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().setVersion(((IDBMetadata)viewConnection).getDBMetadata().getLatestDatabaseVersion().getVersion() + 1);

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+((IDBMetadata)viewConnection).getDBMetadata().getDebugName()+" (initial version = "+((IDBMetadata)viewConnection).getDBMetadata().getInitialVersion().getVersion()+", exported version = "+((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()+", database_version = "+((IDBMetadata)viewConnection).getDBMetadata().getDatabaseVersion().getVersion()+", latest_database_version = "+((IDBMetadata)viewConnection).getDBMetadata().getLatestDatabaseVersion().getVersion()+")");

		insert(this.schema+"views_connections", ViewsConnectionsColumns
				,((IIdentifier)viewConnection).getId()
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
				,viewConnection.getClass().getSimpleName()
				,((IIdentifier)viewConnection.eContainer()).getId()
				,(!(viewConnection instanceof IDiagramModelArchimateConnection) ? ((INameable)viewConnection).getName() : null)                    // if there is a relationship behind, the name is the relationship name, so no need to store it.
				,(!(viewConnection instanceof IDiagramModelArchimateConnection) ? ((IDocumentable)viewConnection).getDocumentation() : null)       // if there is a relationship behind, the documentation is the relationship name, so no need to store it.
				,((viewConnection instanceof ILockable) ? (((ILockable)viewConnection).isLocked()?1:0) : null)  
				,viewConnection.getLineColor()
				,viewConnection.getLineWidth()
				,viewConnection.getFont()
				,viewConnection.getFontColor()
				,((viewConnection instanceof IDiagramModelArchimateConnection) ? ((IDiagramModelArchimateConnection)viewConnection).getArchimateConcept().getId() : null)
				,viewConnection.getSource().getId()
				,viewConnection.getTarget().getId()
				,viewConnection.getTextPosition()
				,((viewConnection instanceof IDiagramModelArchimateObject) ? ((IDiagramModelArchimateObject)viewConnection).getType() : viewConnection.getType())
				,DBPlugin.getUserName()
				,((DBArchimateModel)viewConnection.getDiagramModel().getArchimateModel()).getCurrentVersion().getTimestamp()
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getChecksum()
				);

		exportProperties(viewConnection);

		for ( int pos = 0 ; pos < viewConnection.getBendpoints().size(); ++pos) {
			IDiagramModelBendpoint bendpoint = viewConnection.getBendpoints().get(pos);
			insert(this.schema+"bendpoints", bendpointsColumns
					,((IIdentifier)viewConnection).getId()
					,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
					,pos
					,bendpoint.getStartX()
					,bendpoint.getStartY()
					,bendpoint.getEndX()
					,bendpoint.getEndY()
					);
		}
	}

	/**
	 * Assign a view Connection to a view into the database
	 */
	private void assignViewConnectionToView(IDiagramModelConnection viewConnection) throws Exception {
		final String[] viewObjectInViewColumns = {"connection_id", "connection_version", "view_id", "view_version", "rank"};
		IDiagramModel viewContainer = viewConnection.getDiagramModel();

		if ( logger.isTraceEnabled() ) logger.trace("   Assigning view connection to view");

		insert(this.schema+"views_connections_in_view", viewObjectInViewColumns
				,viewConnection.getId()
				,((IDBMetadata)viewConnection).getDBMetadata().getCurrentVersion().getVersion()
				,viewContainer.getId()
				,((IDBMetadata)viewContainer).getDBMetadata().getCurrentVersion().getVersion()
				,++this.viewConnectionRank
				);


	}

	/**
	 * Export properties to the database
	 */
	private void exportProperties(IProperties parent) throws Exception {
		final String[] propertiesColumns = {"parent_id", "parent_version", "rank", "name", "value"};

		int exportedVersion;
		if ( parent instanceof DBArchimateModel ) {
			exportedVersion = ((DBArchimateModel)parent).getCurrentVersion().getVersion();
		} else 
			exportedVersion = ((IDBMetadata)parent).getDBMetadata().getCurrentVersion().getVersion();

		for ( int propRank = 0 ; propRank < parent.getProperties().size(); ++propRank) {
			IProperty prop = parent.getProperties().get(propRank);
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
				request("MATCH (parent {id:?, version:?}) CREATE (prop:property {rank:?, name:?, value:?}), (parent)-[:hasProperty]->(prop)"
						,((IIdentifier)parent).getId()
						,exportedVersion
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
			}
			else
				insert(this.schema+"properties", propertiesColumns
						,((IIdentifier)parent).getId()
						,exportedVersion
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
		}
	}

	/**
	 * Export properties to the database
	 */
	private void exportMetadata(DBArchimateModel parent) throws Exception {
		final String[] metadataColumns = {"parent_id", "parent_version", "rank", "name", "value"};

		for ( int propRank = 0 ; propRank < parent.getMetadata().getEntries().size(); ++propRank) {
			IProperty prop = parent.getMetadata().getEntries().get(propRank);
			if ( DBPlugin.areEqual(this.databaseEntry.getDriver(), DBDatabase.NEO4J.getDriverName()) ) {
				request("MATCH (parent {id:?, version:?}) CREATE (prop:metadata {rank:?, name:?, value:?}), (parent)-[:hasMetadata]->(prop)"
						,parent.getId()
						,parent.getCurrentVersion().getVersion()
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
			}
			else
				insert(this.schema+"metadata", metadataColumns
						,parent.getId()
						,parent.getCurrentVersion().getVersion()
						,propRank
						,prop.getKey()
						,prop.getValue()
						);
		}
	}

	public boolean exportImage(String path, byte[] image) throws SQLException {
		// we do not export null images (should never happen, but it sometimes does)
		if ( image == null ) 
			return true;

		boolean exported = false;

		try ( ResultSet result = select("SELECT path FROM "+this.schema+"images WHERE path = ?", path) ) {

			if ( result.next() ) {
				// if the image exists in the database, we update it
				request("UPDATE "+this.schema+"images SET image = ? WHERE path = ?"
						,image
						,path
						);
				exported = true;
			} else {
				// if the image is not yet in the db, we insert it
				String[] databaseColumns = {"path", "image"};
				insert(this.schema+"images", databaseColumns
						,path
						,image							
						);
				exported = true;
			}
		}
		return exported;
	}

	public static String getTargetConnectionsString(EList<IDiagramModelConnection> connections) {
		StringBuilder target = new StringBuilder();
		for ( IDiagramModelConnection connection: connections ) {
			if ( target.length() > 0 )
				target.append(",");
			target.append(connection.getId());
		}
		return target.toString();
	}

	/**
	 * Sets the auto-commit mode of the database
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		super.setAutoCommit(autoCommit);

		if ( autoCommit )
			this.lastTransactionTimestamp = null;                                                         // all the request will have their own timestamp
		else
			this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());    // all the requests will have the same timestamp
	}

	/**
	 * Commits the current transaction
	 */
	@Override
	public void commit() throws SQLException {
		super.commit();
		this.lastTransactionTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
	}

	public void reset() {
		// We reset all "ranks" to zero
		this.elementRank = 0;
		this.relationshipRank = 0;
		this.folderRank = 0;
		this.viewRank = 0;
		this.viewObjectRank = 0;
		this.viewConnectionRank = 0;

		// we empty the hashmaps
		this.elementsNotInModel.clear();
		this.relationshipsNotInModel.clear();
		this.foldersNotInModel.clear();
		this.viewsNotInModel.clear();
		this.imagesNotInModel.clear();
		this.imagesNotInDatabase.clear();
	}

	/**
	 * Closes connection to the database
	 */
	@Override
	public void close() throws SQLException {
		reset();

		if ( !this.isImportconnectionDuplicate )
			super.close();
	}
}