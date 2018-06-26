/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBVersion;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ModelVersion;

import lombok.Getter;
import lombok.Setter;

/**
 * This class extends the <b>ArchimateModel</b> class.<br>
 * It adds a version and various counters about the components included in the model.
 * 
 * @author Herve Jouin 
 * @see com.archimatetool.model.impl.ArchimateModel
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DBArchimateModel extends com.archimatetool.model.impl.ArchimateModel {
    private static final DBLogger logger = new DBLogger(DBArchimateModel.class);

    public DBArchimateModel() {
        super();
        if ( logger.isDebugEnabled() ) logger.debug("Creating new ArchimateModel");
        super.setVersion(ModelVersion.VERSION);
        super.setMetadata(DBArchimateFactory.eINSTANCE.createMetadata());
    }

    /**
     * Specifies if we must import the latest version of the components or the version specified in the model.
     * 
     * @param importLatestVersion true if the import procedure should import the latest version of all components (even if updated by other models), or false if the import procedure should import the model version as it was exported.
     * @return true if the import procedure should import the latest version of all components (even if updated by other models), or false if the import procedure should import the model version as it was exported.
     */
    @Getter @Setter private boolean latestVersionImported = false;

    /**
     * Initial version of the model, i.e. version when it has been imported or exported, or zero if loaded from an archimate file)  
     */
    @Getter private DBVersion initialVersion = new DBVersion();

    /**
     * Version of the model as it will be exported to the database. Usually @initialVersion + 1.
     */
    @Getter private DBVersion currentVersion = new DBVersion();

    /**
     * Latest version of the model in the database.
     */
    @Getter private DBVersion databaseVersion = new DBVersion();
    
    /**
     * Determines it the model is the latest one in the database by comparing its currentVersion to the initialVersion
     */
    public boolean isTheLatestModelIntheDatabase() {
        return (this.currentVersion.getVersion() - this.initialVersion.getVersion()) == 1;
    }

    /**
     * List of all elements in the model.<br>
     * <br>
     * Set by the @countAllObjects method.
     * <br>
     * We use LinkedHashMap as the order is important
     */
    @Getter private Map<String, IArchimateElement> allElements = new LinkedHashMap<String, IArchimateElement>();

    /**
     * List of all relationships in the model.<br>
     * <br>
     * Set by the @countAllObjects method.
     * <br>
     * We use LinkedHashMap as the order is important
     */
    @Getter private Map<String, IArchimateRelationship> allRelationships = new LinkedHashMap<String, IArchimateRelationship>();

    /**
     * List of all views in the model.<br>
     * <br>
     * Set by the @countAllObjects method.
     * <br>
     * We use LinkedHashMap as the order is important
     */
    @Getter private Map<String, IDiagramModel> allViews = new LinkedHashMap<String, IDiagramModel>();

    /**
     * List of all objects in the model views.<br>
     * <br>
     * Set by the @countAllObjects method.
     * <br>
     * We use LinkedHashMap as the order is important
     */
    @Getter private Map<String, IDiagramModelObject> allViewObjects = new LinkedHashMap<String, IDiagramModelObject>();

    /**
     * List of all connections in the model views.<br>
     * <br>
     * Set by the @countAllObjects method.
     * <br>
     * We use LinkedHashMap as the order is important
     */
    @Getter private Map<String, IDiagramModelConnection> allViewConnections = new LinkedHashMap<String, IDiagramModelConnection>();

    /**
     * List of all folders in the model.<br>
     * <br>
     * Set by the @countAllObjects method.
     * <br>
     * We use LinkedHashMap as the order is important
     */
    @Getter private Map<String, IFolder> allFolders = new LinkedHashMap<String, IFolder>();

    /**
     * List of the source relationships that have been imported but not yet created.
     */
    @Getter private Map<IArchimateRelationship, String> allSourceRelationshipsToResolve = new LinkedHashMap<IArchimateRelationship, String>();

    /**
     * List of the target relationships that have been imported but not yet created.
     */
    @Getter private Map<IArchimateRelationship, String> allTargetRelationshipsToResolve = new LinkedHashMap<IArchimateRelationship, String>();

    /**
     * List of all the source connections that have been imported but not yet created.
     */
    @Getter private Map<IDiagramModelConnection, String> allSourceConnectionsToResolve = new LinkedHashMap<IDiagramModelConnection, String>();

    /**
     * List of all the target connections that have been imported but not yet created.
     */
    @Getter private Map<IDiagramModelConnection, String> allTargetConnectionsToResolve = new LinkedHashMap<IDiagramModelConnection, String>();

    /**
     * List of all the image paths in the model.
     */
    public List<String> getAllImagePaths() {
        return ((IArchiveManager)getAdapter(IArchiveManager.class)).getLoadedImagePaths();
    }

    /**
     * Gets the image content.
     * 
     * @param path path of the image
     * @return the byte array containing the image corresponding to the provided path
     */
    public byte[] getImage(String path) {
        return ((IArchiveManager)getAdapter(IArchiveManager.class)).getBytesFromEntry(path);
    }

    /**
     * Resets the counters of components in the model
     */
    public void resetCounters() {
        if ( logger.isDebugEnabled() ) logger.debug("Reseting model's counters.");

        this.allSourceRelationshipsToResolve.clear();
        this.allTargetRelationshipsToResolve.clear();
        this.allSourceConnectionsToResolve.clear();
        this.allTargetConnectionsToResolve.clear();
        this.allElements.clear();
        this.allRelationships.clear();
        this.allViews.clear();
        this.allViewObjects.clear();
        this.allViewConnections.clear();
        this.allFolders.clear();
    }

    /**
     * Gets the folder that contains the component
     * @return the folder that contains the component, null if no folder contains it.
     */
    public IFolder getFolder(EObject eObject) {
        return getFolder(eObject, getDefaultFolderForObject(eObject));
    }

    /**
     * check if the eObject is part of the folder, on calls itself recursively for every sub-folder
     * @return the folder that contains the component, null if no folder contains it.
     */
    private IFolder getFolder(EObject eObject, IFolder folder) {
        if( folder == null )
            return null;

        if ( eObject == folder )
            return folder;

        for ( EObject folderElement: folder.getElements() )
            if ( eObject == folderElement ) return folder;

        for ( IFolder subFolder: folder.getFolders() ) {
            IFolder folderThatContainsEObject = getFolder(eObject, subFolder);
            if ( folderThatContainsEObject != null ) return folderThatContainsEObject;
        }

        return null;
    }

    /**
     * Counts the number of objects in the model.<br>
     * At the same time, we calculate the current checksums
     */
    public void countAllObjects() throws Exception {
        resetCounters();

        if ( logger.isDebugEnabled() ) logger.debug("Counting objects in selected model.");
        // we iterate over the model components and store them in hash tables in order to count them and retrieve them more easily
        // In addition, we calculate the current checksum on elements and relationships

        // we do not use eAllContents() but traverse manually all the components because we want to keep the order
        //    - elements and relationships order is not really important
        //    - but graphical objects order is important to know which one is over (or under) which others

        // we also ensure that the root folders are exported first
        for (IFolder folder: getFolders() ) {
            //((IDBMetadata)folder).getDBMetadata().setRootFolderType(folder.getType().getValue());
            this.allFolders.put(folder.getId(), folder);
        }

        for (IFolder folder: getFolders() ) {
            countObject(folder, true, null);
        }
    }

    // the viewChecksum variable is a trick to include the connections checksums in the view checksum
    private StringBuilder viewChecksum = null;
    /**
     * Adds a specific object in the corresponding counter<br>
     * At the same time, we calculate the current checksums
     * @return the concatenation of the checksums of all the eObject components
     */
    @SuppressWarnings("null")
    public String countObject(EObject eObject, boolean mustCalculateChecksum, IDiagramModel parentDiagram) throws Exception {
        StringBuilder checksumBuilder = null;
        int len = 0;

        if ( mustCalculateChecksum ) {
            //TODO: find a way to avoid to calculate the checksum twice for connections (they are counted twice : as sources and targets) 
            checksumBuilder = new StringBuilder(DBChecksum.calculateChecksum(eObject));
            len = checksumBuilder.length();
        }

        switch ( eObject.eClass().getName() ) {
            case "ArchimateDiagramModel":
            case "CanvasModel":
            case "SketchModel":                     this.allViews.put(((IIdentifier)eObject).getId(), (IDiagramModel)eObject);
            
										            if ( mustCalculateChecksum ) {
										            	String checksum = checksumBuilder.toString();
										            	((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setContainerChecksum(checksum);
										                ((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setChecksum(checksum);
										                ((IDBMetadata)eObject).getDBMetadata().setChecksumValid(true);
										                this.viewChecksum = new StringBuilder(checksumBuilder.toString());
										            }
										            
										            for ( EObject child: ((IDiagramModel)eObject).getChildren() )
										                countObject(child, mustCalculateChecksum, (IDiagramModel)eObject);
										            
										            if ( mustCalculateChecksum ) {
										                checksumBuilder = new StringBuilder(this.viewChecksum.toString());
										                this.viewChecksum = null;
										            }
										            break;

            case "SketchModelActor":
            case "CanvasModelBlock":
            case "CanvasModelImage":
            case "DiagramModelArchimateObject":		
            case "DiagramModelGroup":
            case "DiagramModelNote":
            case "DiagramModelReference":
            case "CanvasModelSticky":
            case "SketchModelSticky":				this.allViewObjects.put(((IIdentifier)eObject).getId(), (IDiagramModelObject)eObject);
										            
									                if ( mustCalculateChecksum ) {
									                	((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setContainerChecksum(checksumBuilder.toString());
									                	if ( this.viewChecksum != null )
									                	    this.viewChecksum.append(checksumBuilder.toString());
									                }
									                
										            if ( eObject instanceof IDiagramModelContainer ) {
									                	for ( EObject child: ((IDiagramModelContainer)eObject).getChildren() ) {
									                	    String subChecksum = countObject(child, mustCalculateChecksum, parentDiagram);
									                	    if ( mustCalculateChecksum ) {
									                	        checksumBuilder.append(subChecksum);
									                	        if ( this.viewChecksum != null )
									                	            this.viewChecksum.append(subChecksum);
									                	    }
									                	}
										            }
										            
										            if ( eObject instanceof IConnectable) {
										                for ( EObject source: ((IConnectable)eObject).getSourceConnections() ) {
										                    String subChecksum = countObject(source, mustCalculateChecksum, parentDiagram);
										                    if ( mustCalculateChecksum && (this.viewChecksum != null) )
										                        this.viewChecksum.append(subChecksum);
										                }
										                
										                // we count only the source connections in order to wount them only once
										                //for ( EObject target: ((IConnectable)eObject).getTargetConnections() ) {
										                //    String subChecksum = countObject(target, mustCalculateChecksum, parentDiagram);
										                //    if ( mustCalculateChecksum )
										                //        this.viewChecksum.append(subChecksum);
										                //}
										            }
										            break;

            case "CanvasModelConnection":
            case "DiagramModelArchimateConnection":
            case "DiagramModelConnection":			this.allViewConnections.put(((IIdentifier)eObject).getId(), (IDiagramModelConnection)eObject);
            
                                                    if ( mustCalculateChecksum && (this.viewChecksum != null) ) {
                                                        this.viewChecksum.append(checksumBuilder.toString());
                                                    }
            
                                                    for ( EObject source: ((IDiagramModelConnection)eObject).getSourceConnections() ) {
                                                        String subChecksum = countObject(source, mustCalculateChecksum, parentDiagram);
                                                        if ( mustCalculateChecksum && (this.viewChecksum != null) )
                                                            this.viewChecksum.append(subChecksum);
                                                    }
            
										            break;
										
			case "Folder":							this.allFolders.put(((IFolder)eObject).getId(), (IFolder)eObject);
			
										            // TODO : SUB FOLDERS AND ELEMENTS ARE NOT SORTED AND MAY BE DIFFERENT FROM ONE ARCHI INSTANCE TO ANOTHER !!!
										            // so we do not use sub folders or elements in the checksum calculation anymore
										            // at the moment, this is not important as we do not allow to share folders between models
										            // but a solution needs to be found !!!
										
										            for ( IFolder subFolder: ((IFolder)eObject).getFolders() ) {
														// we fix the folder type
														subFolder.setType(FolderType.USER);
										                countObject(subFolder, mustCalculateChecksum, parentDiagram);
										                //SEE WARNING -- if ( mustCalculateChecksum ) checksumBuilder.append(subFolder.getId());
										            }
										
										            for ( EObject child: ((IFolder)eObject).getElements() ) {
										                countObject(child, mustCalculateChecksum, parentDiagram);
										                //SEE WARNING -- if ( mustCalculateChecksum ) checksumBuilder.append(((IIdentifier)child).getId());
										            }
										            break;
            case "Property":
            case "Bounds":
            case "Metadata":
            case "DiagramModelBendpoint":			// do nothing
                									break;

            default:								// here, the class is too detailed (Node, Artefact, BusinessActor, etc ...), so we use "instanceof" to distinguish elements from relationships
									                if ( eObject instanceof IArchimateElement ) {
									                    this.allElements.put(((IIdentifier)eObject).getId(), (IArchimateElement)eObject);
									                } else if ( eObject instanceof IArchimateRelationship ) {
									                    this.allRelationships.put(((IIdentifier)eObject).getId(), (IArchimateRelationship)eObject);
									                } else { //we should never be there, but just in case ...
									                    throw new Exception("Unknown "+eObject.eClass().getName()+" object.");
									                }
        }

        if ( mustCalculateChecksum ) {
            // if the checksumBuilder contains a single checksum, then we get it
            // else, we calculate a new checksum from the list of checksums
            String checksum = (checksumBuilder.length() != len) ? DBChecksum.calculateChecksum(checksumBuilder) : checksumBuilder.toString();
            ((IDBMetadata)eObject).getDBMetadata().getCurrentVersion().setChecksum(checksum);
            return checksum;
        }

        return null;
    }

    /**
     * register that the source of the relationship the concept with ID = sourceId<br>
     * It is registered as it may not be imported in the model when the relationship is created, so it will need to be resolved later (see {@link resolveSourceRelationships})<br>
     * <br>
     * As all the elements are imported before the relationships, the source of the relationship is another relationship (else, the element would have been existing in the model) 
     * @param relationship
     * @param sourceId
     * @throws Exception
     */
    public void registerSourceRelationship(IArchimateRelationship relationship, String sourceId) throws Exception {
        if ( sourceId != null && sourceId.length()!=0 ) this.allSourceRelationshipsToResolve.put(relationship, sourceId);
    }

    /**
     * register that the target of the relationship the concept with ID = sourceId<br>
     * It is registered as it may not be imported in the model when the relationship is created, so it will need to be resolved later (see {@link resolveTargetRelationships})<br>
     * <br>
     * As all the elements are imported before the relationships, the target of the relationship is another relationship (else, the element would have been existing in the model) 
     * @param relationship
     * @param sourceId
     * @throws Exception
     */
    public void registerTargetRelationship(IArchimateRelationship relationship, String targetId) throws Exception {
        if ( targetId != null && targetId.length()!=0 ) this.allTargetRelationshipsToResolve.put(relationship, targetId);
    }
    
    /**
     * resolves the source relationships (see {@link registerSourceRelationship})
     */
    public void resolveSourceRelationships() {
        logger.info("Resolving source relationships.");

        for ( Map.Entry<IArchimateRelationship, String> entry: this.allSourceRelationshipsToResolve.entrySet() ) {
            IArchimateRelationship relationship = entry.getKey();

            IArchimateRelationship source = this.getAllRelationships().get(entry.getValue());
            if ( source != null ) {
                relationship.setSource(source);
                source.getSourceRelationships().add(relationship);
            }
        }

        this.allSourceRelationshipsToResolve.clear();
    }

    /**
     * resolves the target relationships (see {@link registerTargetRelationship})
     */
    public void resolveTargetRelationships() {
        logger.info("Resolving target relationships.");

        for ( Map.Entry<IArchimateRelationship, String> entry: this.allTargetRelationshipsToResolve.entrySet() ) {
            IArchimateRelationship relationship = entry.getKey();

            IArchimateRelationship target = this.getAllRelationships().get(entry.getValue());
            if ( target != null ) {
                relationship.setTarget(target);
                target.getTargetRelationships().add(relationship);
            }
        }

        this.allTargetRelationshipsToResolve.clear();
    }

    public void registerSourceConnection(IDiagramModelConnection connection, String sourceId) throws Exception {
        if ( sourceId != null && sourceId.length()!=0 ) this.allSourceConnectionsToResolve.put(connection, sourceId);
    }

    public void registerTargetConnection(IDiagramModelConnection connection, String targetId) throws Exception {
        if ( targetId != null && targetId.length()!=0 ) this.allTargetConnectionsToResolve.put(connection, targetId);
    }

    public void resolveSourceConnections() {
        logger.info("Resolving source connections.");

        for ( Map.Entry<IDiagramModelConnection, String> entry: this.allSourceConnectionsToResolve.entrySet() ) {
            IDiagramModelConnection connection = entry.getKey();

            IConnectable source = this.getAllViewConnections().get(entry.getValue());
            connection.setSource(source);
            source.addConnection(connection);
        }

        this.allSourceConnectionsToResolve.clear();
    }

    public void resolveTargetConnections() {
        logger.info("Resolving target connections.");

        for ( Map.Entry<IDiagramModelConnection, String> entry: this.allTargetConnectionsToResolve.entrySet() ) {
            IDiagramModelConnection connection = entry.getKey();

            IConnectable target = this.getAllViewConnections().get(entry.getValue());
            connection.setSource(target);
            target.addConnection(connection);
        }

        this.allTargetConnectionsToResolve.clear();
    }
}
