/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.archicontribs.database.DBChecksum;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBVersion;
import org.eclipse.emf.ecore.EObject;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ModelVersion;

/**
 * This class extends the <b>ArchimateModel</b> class.<br>
 * It adds a version and various counters about the components included in the model.
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ArchimateModel
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class ArchimateModel extends com.archimatetool.model.impl.ArchimateModel {
	private static final DBLogger logger = new DBLogger(ArchimateModel.class);
		
	public ArchimateModel() {
		super();
		if ( logger.isTraceEnabled() ) logger.trace("Creating new ArchimateModel");
		super.setVersion(ModelVersion.VERSION);
		super.setMetadata(DBArchimateFactory.eINSTANCE.createMetadata());
	}
	
	private boolean importLatestVersion = false;			// specifies if we must import the latest version of the components or the version specified in the model
	private DBVersion currentVersion = new DBVersion(Timestamp.from(Instant.now()));
	private DBVersion databaseVersion = new DBVersion(Timestamp.from(Instant.MIN));
	
    // we use LinkedHashMap as order is important
	private Map<String, IArchimateElement> allElements = new LinkedHashMap<String, IArchimateElement>();
	private Map<String, IArchimateRelationship> allRelationships = new LinkedHashMap<String, IArchimateRelationship>();
	private Map<String, IDiagramModel> allViews = new LinkedHashMap<String, IDiagramModel>();
	private Map<String, IDiagramModelComponent> allViewsObjects = new LinkedHashMap<String, IDiagramModelComponent>();
	private Map<String, IDiagramModelConnection> allViewsConnections = new LinkedHashMap<String, IDiagramModelConnection>();
	private Map<String, IFolder> allFolders = new LinkedHashMap<String, IFolder>();
	private Map<IArchimateRelationship, Entry<String, String>> allRelationsSourceAndTarget = new LinkedHashMap<IArchimateRelationship, Entry<String, String>>();
	private Map<IDiagramModelObject, String> allSourceObjectsConnections = new LinkedHashMap<IDiagramModelObject, String>();
	private Map<IDiagramModelObject, String> allTargetObjectsConnections = new LinkedHashMap<IDiagramModelObject, String>();
	private Map<IDiagramModelConnection, String> allSourceConnectionsConnections = new LinkedHashMap<IDiagramModelConnection, String>();
	private Map<IDiagramModelConnection, String> allTargetConnectionsConnections = new LinkedHashMap<IDiagramModelConnection, String>();
	
	public void setImportLatestVersion(boolean latest) {
		importLatestVersion = latest;
	}
	
	public boolean getImportLatestVersion() {
		return importLatestVersion;
	}
	
	/**
	 * @return the current version of the model 
	 */
	public DBVersion getCurrentVersion() {
		return currentVersion;
	}
	
	/**
	 * @return the version of the model as it is in the database 
	 */
	public DBVersion getDatabaseVersion() {
		return databaseVersion;
	}
	
	/**
	 * Resets the counters of components in the model
	 */
	public void resetCounters() {
		if ( logger.isTraceEnabled() ) logger.trace("Reseting model's counters.");
		

		allElements.clear();
		allRelationships.clear();
		allViews.clear();
		allViewsObjects.clear();
		allViewsConnections.clear();
		allFolders.clear();
		
		allRelationsSourceAndTarget.clear();
		allSourceObjectsConnections.clear();
		allTargetObjectsConnections.clear();
	    allSourceConnectionsConnections.clear();
	    allTargetConnectionsConnections.clear();
	}
	
	/**
     * Resets the counters of Sources and Targets to be resolved in the model<br>
     * Used when importing unitary relationship or connection
     */
    public void resetSourceAndTargetCounters() {
        if ( logger.isTraceEnabled() ) logger.trace("Reseting source and target counters.");
        
        allRelationsSourceAndTarget.clear();
        allSourceObjectsConnections.clear();
        allTargetObjectsConnections.clear();
        allSourceConnectionsConnections.clear();
        allTargetConnectionsConnections.clear();
    }
	
	/**
	 * Counts the number of objects in the model.<br>
	 * At the same time, we calculate the current checksums
	 */
	public void countAllObjects() throws Exception {
		resetCounters();
		
		if ( logger.isTraceEnabled() ) logger.trace("Counting objects in selected model.");
			// we iterate over the model components and store them in hash tables in order to count them and retrieve them more easily
			// In addition, we calculate the current checksum on elements and relationships
		
			// we do not use eAllContents() but traverse manually all the components because we want to keep the order
			//    - elements and relationships order is not really important
			//    - but graphical objects order is important to know which one is over (or under) which others
		
			// we also ensure that the root folders are exported first
		for (IFolder folder: getFolders() ) {
		    ((IDBMetadata)folder).getDBMetadata().setRootFolderType(folder.getType().getValue());
		    allFolders.put(folder.getId(), folder);
		}
		
		for (IFolder folder: getFolders() ) {
			countObject(folder, true, null);
		}
	}
	
	/**
	 * Adds a specific object in the corresponding counter<br>
	 * At the same time, we calculate the current checksums
	 * If it is a folder, we set its type that it is the same as its root parent
	 * @return : the concatenation of the checksums of all the eObject components
	 */
	public String countObject(EObject eObject, boolean mustCalculateChecksum, IDiagramModel parentDiagram) throws Exception {
		StringBuilder checksumBuilder = null;
		int len = 0;

		if ( mustCalculateChecksum ) {
			//TODO: find a way to avoid to calculate the checksum twice for connections (are they are counted twice : as sources and targets) 
			checksumBuilder = new StringBuilder(DBChecksum.calculateChecksum(eObject));
			len = checksumBuilder.length();
		}
		
		//if ( ((IIdentifier)eObject).getId().equals("03366f6e-e1ab-46dd-b91a-4ac4a8453377") ) {
		//	System.out.println("got you in parent "+parentDiagram.getId());
		//}
		
		switch ( eObject.eClass().getName() ) {
			case "ArchimateDiagramModel" :
			case "CanvasModel" :
			case "SketchModel" :					allViews.put(((IIdentifier)eObject).getId(), (IDiagramModel)eObject);
													for ( EObject child: ((IDiagramModel)eObject).getChildren() ) {
														String subChecksum = countObject(child, mustCalculateChecksum, (IDiagramModel)eObject);
														if ( mustCalculateChecksum ) checksumBuilder.append(subChecksum);
													}
												    break;
												    
			case "SketchModelActor" :
			case "CanvasModelBlock" :
			case "CanvasModelImage" :
			case "DiagramModelArchimateObject":		
			case "DiagramModelGroup" :
			case "DiagramModelNote" :
			case "DiagramModelReference" :
			case "CanvasModelSticky" :
			case "SketchModelSticky" :				allViewsObjects.put(((IIdentifier)eObject).getId(), (IDiagramModelComponent)eObject);
			                                        ((IDBMetadata)eObject).getDBMetadata().setParentdiagram(parentDiagram);
													if ( eObject instanceof IDiagramModelContainer ) {
														for ( EObject child: ((IDiagramModelContainer)eObject).getChildren() ) {
															String subChecksum = countObject(child, mustCalculateChecksum, parentDiagram);
															if ( mustCalculateChecksum ) checksumBuilder.append(subChecksum);
														}
													}
													if ( eObject instanceof IConnectable) {
														for ( EObject source: ((IConnectable)eObject).getSourceConnections() ) {
															String subChecksum = countObject(source, mustCalculateChecksum, parentDiagram);
															if ( mustCalculateChecksum ) checksumBuilder.append(subChecksum); 
														}
														for ( EObject target: ((IConnectable)eObject).getTargetConnections() ) {
															String subChecksum = countObject(target, mustCalculateChecksum, parentDiagram);
															if ( mustCalculateChecksum ) checksumBuilder.append(subChecksum);
														}
													}
													break;
	
			case "CanvasModelConnection" :
			case "DiagramModelArchimateConnection":
			case "DiagramModelConnection" :			allViewsConnections.put(((IIdentifier)eObject).getId(), (IDiagramModelConnection)eObject);
			                                        ((IDBMetadata)eObject).getDBMetadata().setParentdiagram(parentDiagram);
													break;
													
			case "Folder" :							allFolders.put(((IFolder)eObject).getId(), (IFolder)eObject);
			
													// WARNING : SUB FOLDERS AND ELEMENTS ARE NOT SORTED AND MAY BE DIFFERENT FROM ONE ARCHI INSTANCE TO ANOTHER !!!
													// so we do not use sub folders or elements in the checksum calculation anymore
													// at the moment, this is not important as we do not allow to share folders between models
													// but a solution needs to be found !!!
			
													for ( IFolder subFolder: ((IFolder)eObject).getFolders() ) {
														((IDBMetadata)subFolder).getDBMetadata().setRootFolderType( ( ((IFolder)subFolder).getType().getValue() != 0 ) ? ((IFolder)subFolder).getType().getValue() : ((IDBMetadata)eObject).getDBMetadata().getRootFolderType());
														countObject(subFolder, mustCalculateChecksum, parentDiagram);
														//SEE WARNING -- if ( mustCalculateChecksum ) checksumBuilder.append(subFolder.getId());
													}
													
													for ( EObject child: ((IFolder)eObject).getElements() ) {
														countObject(child, mustCalculateChecksum, parentDiagram);
														//SEE WARNING -- if ( mustCalculateChecksum ) checksumBuilder.append(((IIdentifier)child).getId());
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
				} else if ( eObject instanceof IArchimateRelationship ) {
					allRelationships.put(((IIdentifier)eObject).getId(), (IArchimateRelationship)eObject);
				} else { //we should never be there, but just in case ...
					throw new Exception("Unknown "+eObject.eClass().getName()+" object.");
				}
		}
		
		if ( mustCalculateChecksum ) {
			String checksum = (checksumBuilder.length() != len) ? DBChecksum.calculateChecksum(checksumBuilder) : checksumBuilder.toString();
			((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setChecksum(checksum);
			return checksum;
		}
		
		return null;
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
	
	public Map<String, IDiagramModelComponent> getAllViewObjects() {
		return allViewsObjects;
	}
	
	public Map<String, IDiagramModelConnection> getAllViewConnections() {
		return allViewsConnections;
	}
	
	public Map<String, IFolder> getAllFolders() {
		return allFolders;
	}
	
	public List<String> getAllImagePaths() {
		return ((IArchiveManager)getAdapter(IArchiveManager.class)).getImagePaths();
	}
	
	public byte[] getImage(String path) {
		return ((IArchiveManager)getAdapter(IArchiveManager.class)).getBytesFromEntry(path);
	}
	
	public void registerSourceAndTarget(IArchimateRelationship relationship, String sourceId, String targetId) throws Exception {
		assert (sourceId != null && targetId != null);
		
		allRelationsSourceAndTarget.put(relationship, new SimpleEntry<String, String>(sourceId, targetId));
	}
	
	public void resolveRelationshipsSourcesAndTargets() {
	    if ( logger.isTraceEnabled() ) logger.trace("resolving sources and targets for relationships");
		for ( Map.Entry<IArchimateRelationship, Entry<String, String>> entry: allRelationsSourceAndTarget.entrySet() ) {
			IArchimateRelationship relationship = entry.getKey();
			Entry<String, String> rel = entry.getValue();

			IArchimateConcept source = getAllElements().get(rel.getKey());
			if ( source == null) source = getAllRelationships().get(rel.getKey());

			IArchimateConcept target = getAllElements().get(rel.getValue());
			if ( target == null) target = getAllRelationships().get(rel.getValue());
			
			if ( logger.isTraceEnabled() ) logger.trace("   resolving relationship for "+relationship.getId()+"   (source="+source.getId()+"    target= "+target.getId()+")");
	        relationship.setSource(source);
			relationship.setTarget(target);
		}
		
		allRelationsSourceAndTarget.clear();
	}
	
    public void registerSourceConnection(IDiagramModelObject object, String sourceId) throws Exception {
        if ( sourceId != null && sourceId.length()!=0 ) if ( sourceId != null && sourceId.length()!=0 ) allSourceObjectsConnections.put(object, sourceId);
    }
    
    public void registerTargetConnection(IDiagramModelObject object, String targetId) throws Exception {
        if ( targetId != null && targetId.length()!=0 ) allTargetObjectsConnections.put(object, targetId);
    }
    
    public void registerSourceConnection(IDiagramModelConnection connection, String sourceId) throws Exception {
        if ( sourceId != null && sourceId.length()!=0 ) allSourceConnectionsConnections.put(connection, sourceId);
    }
    
    public void registerTargetConnection(IDiagramModelConnection connection, String targetId) throws Exception {
        if ( targetId != null && targetId.length()!=0 ) allTargetConnectionsConnections.put(connection, targetId);
    }
	
    public void resolveConnectionsSourcesAndTargets() throws Exception {
        if ( logger.isTraceEnabled() ) logger.trace("resolving sources and targets for connections");

        for ( Map.Entry<IDiagramModelObject, String> entry: allSourceObjectsConnections.entrySet() ) {
            IDiagramModelObject object = entry.getKey();
            
            if ( logger.isTraceEnabled() ) logger.trace("   resolving source connection for "+((IDBMetadata)object).getDBMetadata().getDebugName());
            for ( String id: entry.getValue().split(",") ) {
                if ( logger.isTraceEnabled() ) logger.trace("      source = "+id);
                IDiagramModelConnection connection = getAllViewConnections().get(id);
                if ( connection == null ) throw new Exception("Cannot find connection "+entry.getValue());
                connection.setSource(object);
                object.getSourceConnections().add(connection);
            }
        }
        
        for ( Map.Entry<IDiagramModelConnection, String> entry: allSourceConnectionsConnections.entrySet() ) {
            IDiagramModelConnection object = entry.getKey();
            
            if ( logger.isTraceEnabled() ) logger.trace("   resolving source connection for "+((IDBMetadata)object).getDBMetadata().getDebugName());
            for ( String id: entry.getValue().split(",") ) {
                if ( logger.isTraceEnabled() ) logger.trace("      source = "+id);
                IDiagramModelConnection connection = getAllViewConnections().get(id);
                if ( connection == null ) throw new Exception("Cannot find connection "+entry.getValue());
                connection.setSource(object);
                object.getSourceConnections().add(connection);
            }
        }
        
        for ( Map.Entry<IDiagramModelObject, String> entry: allTargetObjectsConnections.entrySet() ) {
            IDiagramModelObject object = entry.getKey();
            
            if ( logger.isTraceEnabled() ) logger.trace("   resolving target connection for "+((IDBMetadata)object).getDBMetadata().getDebugName());
            for ( String id: entry.getValue().split(",") ) {
                if ( logger.isTraceEnabled() ) logger.trace("      source = "+id);
                IDiagramModelConnection connection = getAllViewConnections().get(id);
                if ( connection == null ) throw new Exception("Cannot find connection "+entry.getValue());
                connection.setTarget(object);
                object.getTargetConnections().add(connection);
            }
        }
        
        for ( Map.Entry<IDiagramModelConnection, String> entry: allTargetConnectionsConnections.entrySet() ) {
            IDiagramModelConnection object = entry.getKey();
            
            if ( logger.isTraceEnabled() ) logger.trace("   resolving target connection for "+((IDBMetadata)object).getDBMetadata().getDebugName());
            for ( String id: entry.getValue().split(",") ) {
                if ( logger.isTraceEnabled() ) logger.trace("      source = "+id);
                IDiagramModelConnection connection = getAllViewConnections().get(id);
                if ( connection == null ) throw new Exception("Cannot find connection "+entry.getValue());
                connection.setTarget(object);
                object.getTargetConnections().add(connection);
            }
        }

        allSourceObjectsConnections.clear();
        allSourceConnectionsConnections.clear();
        allSourceObjectsConnections.clear();
        allTargetConnectionsConnections.clear();
	}
}
