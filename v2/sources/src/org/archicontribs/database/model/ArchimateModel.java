package org.archicontribs.database.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map.Entry;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;

/**
 * This class extends the <b>ArchimateModel</b> class.<br>
 * It adds a version and various counters about the components included in the model.
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ArchimateModel
 * @see org.archicontribs.database.IDBMetadata
 */
public class ArchimateModel extends com.archimatetool.model.impl.ArchimateModel {
	private static final DBLogger logger = new DBLogger(ArchimateModel.class);
		
	public ArchimateModel() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ArchimateModel");
	}
	
	private int currentVersion = 0;
	private int exportedVersion = 0;
	//TODO : add databaseVersion as well !!!
	
	private Map<String, IArchimateElement> allElements = null;
	private Map<String, IArchimateRelationship> allRelationships = null;
	private Map<String, IDiagramModel> allViews = null;
	private Map<String, EObject> allViewsObjects = null;
	private Map<String, IConnectable> allViewsConnections = null;
	private Map<String, IFolder> allFolders = null;
	private Map<IArchimateRelationship, Entry<String, String>> allRelations = null;
	private Map<IDiagramModelConnection, Entry<String, String>> allConnections = null;
	
	/**
	 * @return the current version of the model 
	 */
	public int getCurrentVersion() {
		return currentVersion;
	}
	
	/**
	 * sets the current version of the model
	 */
	public void setCurrentVersion(int version) {
		currentVersion = version;
	}
	
	/**
	 * @return the version that was exported to the database
	 */
	public int getExportedVersion() {
		return exportedVersion;
	}
	
	/**
	 * Sets the version of the model as it is exported in the database<br>
	 * copied to current version once the database transaction is committed.
	 */
	public void setExportedVersion(int version) {
		exportedVersion = version;
	}
	
	/**
	 * Resets the counters of components in the model
	 */
	public void resetCounters() {
		if ( logger.isTraceEnabled() ) logger.trace("Reseting model's counters.");
		allElements = new LinkedHashMap<String, IArchimateElement>();						// order is important
		allRelationships = new LinkedHashMap<String, IArchimateRelationship>();				// order is important
		allViews = new LinkedHashMap<String, IDiagramModel>();								// order is important
		allViewsObjects = new LinkedHashMap<String, EObject>();								// order is important
		allViewsConnections = new LinkedHashMap<String, IConnectable>();					// order is important
		allFolders = new LinkedHashMap<String, IFolder>();									// order is important
		allRelations = new HashMap<IArchimateRelationship, Entry<String, String>>();		// order is not important
		allConnections = new HashMap<IDiagramModelConnection, Entry<String, String>>();		// order is not important
	}
	
	/**
	 * Counts the number of objects in the model.<br>
	 * At the same time, we calculate the current checksum of elements and relationships and the exportedVersion of composants
	 * @throws Exception
	 */
	public void countAllObjects() throws Exception {
		resetCounters();
		
		if ( logger.isTraceEnabled() ) logger.trace("Counting objects in selected model.");
			// we iterate over the model components and store them in hash tables in order to count them and retrieve them more easily
			// In addition, we calculate the current checksum on elements and relationships
		
			// we do not use eAllContents() but traverse manually all the components because we want to keep the order
			// elements and relationships order is not important
			// but graphical objects order is important to know which one is over (or under) which others
		
		for (IFolder folder: getFolders() ) {
			countObject(folder);
		}
		
			// we iterate over the images and calculate their checksum.
			// The images are common to all the models, so we get the first one.
		//IArchiveManager archiveMgr = (IArchiveManager)(IEditorModelManager.INSTANCE.getModels().get(0)).getAdapter(IArchiveManager.class);
		//for (String imagePath: archiveMgr.getImagePaths()) {
		//	getComponentsMetadata(imagePath).setCurrentChecksum(DBPlugin.calculateChecksum(archiveMgr.getBytesFromEntry(imagePath)));
		//}
	}
	
	/**
	 * Adds a specific object in the corresponding counter<br>
	 * At the same time, we calculate its current checksum if it is an element or a relationship and its exportedVersion<br>
	 * If if is a folder, we set its type that it is the same as its root parent
	 */
	public void countObject(EObject eObject) throws Exception {
		if ( eObject instanceof IDBMetadata ) {
			((IDBMetadata)eObject).getDBMetadata().setExportedVersion(((IDBMetadata)eObject).getDBMetadata().getCurrentVersion()+1);
		}
		switch ( eObject.eClass().getName() ) {
			case "ArchimateDiagramModel" :
			case "CanvasModel" :
			case "SketchModel" :					allViews.put(((IIdentifier)eObject).getId(), (IDiagramModel)eObject);
													for ( EObject child: ((IDiagramModel)eObject).getChildren() ) countObject(child);
												    break;
			case "SketchModelActor" :
			case "CanvasModelBlock" :
			case "CanvasModelImage" :
			case "DiagramModelArchimateObject":
			case "DiagramModelGroup" :
			case "DiagramModelNote" :
			case "DiagramModelReference" :
			case "CanvasModelSticky" :
			case "SketchModelSticky" :				allViewsObjects.put(((IIdentifier)eObject).getId(), eObject);
													if ( eObject instanceof IDiagramModelContainer ) {
														for ( EObject child: ((IDiagramModelContainer)eObject).getChildren() ) countObject(child);
													}
													if ( eObject instanceof IConnectable) {
														for ( EObject source: ((IConnectable)eObject).getSourceConnections() ) countObject(source);
														for ( EObject target: ((IConnectable)eObject).getTargetConnections() ) countObject(target);
													}
													break;
	
			case "CanvasModelConnection" :
			case "DiagramModelArchimateConnection":
			case "DiagramModelConnection" :			allViewsConnections.put(((IIdentifier)eObject).getId(), (IConnectable)eObject);
													break;
													
			case "Folder" :							allFolders.put(((IIdentifier)eObject).getId(), (IFolder)eObject);
													for ( IFolder child: ((IFolder)eObject).getFolders() ) {
														if ( child.getType().getValue() == 0 ) {
															Display.getDefault().asyncExec(new Runnable() {
																@Override
																public void run() {
																	child.setType(((IFolder)eObject).getType());
																}
												    		});
														}
														countObject(child);
													}
													for ( EObject child: ((IFolder)eObject).getElements() ) {
														countObject(child);
													}
													break;
			case "Property" :
			case "Bounds" :
			case "Metadata":
			case "DiagramModelBendpoint" :			// do nothing
													break;
													
			default :								// here, the class is too detailed (Node, Artefact, BusinessActor, etc ...), so we use "instanceof" to distinguish elements from relationships
				if ( eObject instanceof IArchimateElement ) {
					allElements.put(((IIdentifier)eObject).getId(), (IArchimateElement)eObject);
				} else {
					if ( eObject instanceof IArchimateRelationship ) {
						allRelationships.put(((IIdentifier)eObject).getId(), (IArchimateRelationship)eObject);
					} else { //we should never be there, but just in case ...
						throw new Exception("Unknown "+eObject.eClass().getName()+" object.");
					}
				}
		}
	}
	
	public Map<String, IArchimateElement> getAllElements() {
		return allElements;
	}
	
	public Map<String, IArchimateRelationship> getAllRelationships() {
		return allRelationships;
	}
	
	public Map<String, IDiagramModel> getAllViews() {
		return allViews;
	}
	
	public Map<String, EObject> getAllViewsObjects() {
		return allViewsObjects;
	}
	
	public Map<String, IConnectable> getAllViewsConnections() {
		return allViewsConnections;
	}
	
	public Map<String, IFolder> getAllFolders() {
		return allFolders;
	}
	
	public void registerSourceAndTarget(EObject obj, String sourceId, String targetId) throws Exception {
		assert (sourceId != null && targetId != null);
			
		if ( obj instanceof IArchimateRelationship ) {
			allRelations.put((IArchimateRelationship)obj, new SimpleEntry<String, String>(sourceId, targetId));
		} else if ( obj instanceof IDiagramModelConnection ) {
			allConnections.put((IDiagramModelConnection)obj, new SimpleEntry<String, String>(sourceId, targetId));
		} else
			throw new Exception("Invalid class : must be IArchimateRelationship or IDiagramModelConnection.");
	}
	
	public void setSourceAndTarget() {
		IArchimateConcept archimateConcept;
		for ( String id: getAllRelationships().keySet() ) {
			IArchimateRelationship relationship = getAllRelationships().get(id);
			Entry<String, String> rel = allRelations.get(relationship);

			archimateConcept = getAllElements().get(rel.getKey());
			if ( archimateConcept == null) archimateConcept = getAllRelationships().get(rel.getKey());
			relationship.setSource(archimateConcept);

			archimateConcept = getAllElements().get(rel.getValue());
			if ( archimateConcept == null) archimateConcept = getAllRelationships().get(rel.getValue());
			relationship.setTarget(archimateConcept);
		}
	}
	
	public void reconnectConnections() {
		for ( IDiagramModelConnection _parent: allConnections.keySet() ) {
			Entry<String, String> conn = allConnections.get(_parent);

			IConnectable source = (IConnectable)getAllViewsObjects().get(conn.getKey());
			if ( source == null ) source =  getAllViewsConnections().get(conn.getKey());
	
			IConnectable target = (IConnectable)getAllViewsObjects().get(conn.getValue());
			if ( target == null ) target =  getAllViewsConnections().get(conn.getValue());

			_parent.connect(source, target);
		}
	}
}
