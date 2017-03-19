package org.archicontribs.database.model.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.archicontribs.database.DBChecksum;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.model.DBArchimateFactory;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
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
		metadata = DBArchimateFactory.eINSTANCE.createMetadata();
	}
	
	private int currentVersion = 0;
	private int exportedVersion = 0;
	
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
		
			// we use LinkedHashMap as order is important
		allElements = new LinkedHashMap<String, IArchimateElement>();
		allRelationships = new LinkedHashMap<String, IArchimateRelationship>();
		allViews = new LinkedHashMap<String, IDiagramModel>();
		allViewsObjects = new LinkedHashMap<String, EObject>();
		allViewsConnections = new LinkedHashMap<String, IConnectable>();
		allFolders = new LinkedHashMap<String, IFolder>();
		
		allRelations = new LinkedHashMap<IArchimateRelationship, Entry<String, String>>();
		allConnections = new LinkedHashMap<IDiagramModelConnection, Entry<String, String>>();
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
			//    - elements and relationships order is not important
			//    - but graphical objects order is important to know which one is over (or under) which others
		
		for (IFolder folder: getFolders() ) {
			countObject(folder, true);
		}
	}
	
	/**
	 * Adds a specific object in the corresponding counter<br>
	 * At the same time, we calculate the current checksums
	 * If if is a folder, we set its type that it is the same as its root parent
	 */
	public void countObject(EObject eObject, boolean checksumRequired) throws Exception {
		StringBuilder checksum = null;
			
		switch ( eObject.eClass().getName() ) {
			case "ArchimateDiagramModel" :
			case "CanvasModel" :
			case "SketchModel" :					allViews.put(((IIdentifier)eObject).getId(), (IDiagramModel)eObject);
													for ( EObject child: ((IDiagramModel)eObject).getChildren() ) countObject(child, checksumRequired);
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
														for ( EObject child: ((IDiagramModelContainer)eObject).getChildren() ) countObject(child, checksumRequired);
													}
													if ( eObject instanceof IConnectable) {
														for ( EObject source: ((IConnectable)eObject).getSourceConnections() ) countObject(source, checksumRequired);
														for ( EObject target: ((IConnectable)eObject).getTargetConnections() ) countObject(target, checksumRequired);
													}
													break;
	
			case "CanvasModelConnection" :
			case "DiagramModelArchimateConnection":
			case "DiagramModelConnection" :			allViewsConnections.put(((IIdentifier)eObject).getId(), (IConnectable)eObject);
													break;
													
			case "Folder" :							if ( checksumRequired ) checksum = new StringBuilder(((IFolder)eObject).getName());
													allFolders.put(((IFolder)eObject).getId(), (IFolder)eObject);
													for ( IFolder child: ((IFolder)eObject).getFolders() ) {
														if ( child.getType().getValue() == 0 ) {
															Display.getDefault().asyncExec(new Runnable() {
																@Override
																public void run() {
																	child.setType(((IFolder)eObject).getType());
																}
												    		});
														}
														countObject(child, checksumRequired);
														if ( checksumRequired ) checksum.append(((IDBMetadata)child).getDBMetadata().getCurrentChecksum());
													}
													for ( EObject child: ((IFolder)eObject).getElements() ) {
														countObject(child, checksumRequired);
														if ( checksumRequired ) checksum.append(((IDBMetadata)child).getDBMetadata().getCurrentChecksum());
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
					if ( checksumRequired ) {
						checksum = new StringBuilder(((IArchimateElement)eObject).getName());
						checksum.append(((IArchimateElement)eObject).getDocumentation());
					}
				} else if ( eObject instanceof IArchimateRelationship ) {
						allRelationships.put(((IIdentifier)eObject).getId(), (IArchimateRelationship)eObject);
						if ( checksumRequired ) {
							checksum = new StringBuilder(((IArchimateRelationship)eObject).getName());
							checksum.append(((IArchimateRelationship)eObject).getDocumentation());
							checksum.append(((IArchimateRelationship)eObject).getSource().getId());
							checksum.append(((IArchimateRelationship)eObject).getTarget().getId());
							if ( eObject instanceof IInfluenceRelationship )
								checksum.append(((IInfluenceRelationship)eObject).getStrength());
							if ( eObject instanceof IAccessRelationship )
								checksum.append(((IAccessRelationship)eObject).getAccessType());

						}
				} else { //we should never be there, but just in case ...
					throw new Exception("Unknown "+eObject.eClass().getName()+" object.");
				}
		}
		
		if ( checksum != null ) {
			if ( eObject instanceof IProperties ) {
				for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
					checksum.append(prop.getKey());
					checksum.append(prop.getValue());
				}
			}
			
			((IDBMetadata)eObject).getDBMetadata().setCurrentChecksum(DBChecksum.calculateChecksum(checksum.toString()));
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
	
	public void registerSourceAndTarget(IArchimateRelationship relationship, String sourceId, String targetId) throws Exception {
		assert (sourceId != null && targetId != null);
		
		allRelations.put(relationship, new SimpleEntry<String, String>(sourceId, targetId));
	}
	
	public void registerSourceAndTarget(IDiagramModelConnection connection, String sourceId, String targetId) throws Exception {
		assert (sourceId != null && targetId != null);
			
		allConnections.put(connection, new SimpleEntry<String, String>(sourceId, targetId));
	}
	
	public void resolveRelationshipsSourcesAndTargets() {
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
	
	public void resolveConnectionsSourcesAndTargets() {
		for ( IDiagramModelConnection connection: allConnections.keySet() ) {
			Entry<String, String> conn = allConnections.get(connection);

			IConnectable source = (IConnectable)getAllViewsObjects().get(conn.getKey());
			if ( source == null ) source = (IConnectable)getAllViewsConnections().get(conn.getKey());
	
			IConnectable target = (IConnectable)getAllViewsObjects().get(conn.getValue());
			if ( target == null ) target = (IConnectable)getAllViewsConnections().get(conn.getValue());

			logger.trace("   resolving connection for "+connection.getId()+"   (source="+source.getId()+"    target= "+target.getId()+")");
			connection.setSource(source);
			source.getSourceConnections().add(connection);
			
			connection.setTarget(target);
			target.getTargetConnections().add(connection);
			
			//TODO : in case of error, the exception is not well trapped because of the multi threading !!!
		}
	}
}
