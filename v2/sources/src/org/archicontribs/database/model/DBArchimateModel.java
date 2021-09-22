/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin.CONFLICT_CHOICE;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.data.DBChecksum;
import org.archicontribs.database.data.DBVersion;
import org.archicontribs.database.model.commands.DBDeleteDiagramConnectionCommand;
import org.archicontribs.database.model.commands.DBDeleteDiagramObjectCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.commands.DeleteArchimateElementCommand;
import com.archimatetool.editor.model.commands.DeleteArchimateRelationshipCommand;
import com.archimatetool.editor.model.commands.NonNotifyingCompoundCommand;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.INameable;
import com.archimatetool.model.ModelVersion;
import lombok.Getter;
import lombok.Setter;

/**
 * This class extends the {@link com.archimatetool.model.impl.ArchimateModel ArchimateModel} class.
 * <br>
 * The following features are implemented:
 * </p>
 * @see #latestVersionImported
 * @author Herve Jouin 
 */
public class DBArchimateModel extends com.archimatetool.model.impl.ArchimateModel {
    private static final DBLogger logger = new DBLogger(DBArchimateModel.class);

    /**
     * Creates a new {@link DBArchimateModel} that extends an {@link com.archimatetool.model.impl.ArchimateModel ArchimateModel}. 
     */
    public DBArchimateModel() {
        super();
        if ( logger.isDebugEnabled() ) logger.debug("Creating new ArchimateModel");
        super.setVersion(ModelVersion.VERSION);
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
     * ID of the database used to import the model. Will be set to the default database during export.
     */
    @Getter @Setter String importDatabaseId = null;
    
    /**
     * @return true if the model is the latest one in the database by comparing its {@link #currentVersion} to its {@link #initialVersion}
     */
    public boolean isTheLatestModelIntheDatabase() {
        return (this.currentVersion.getVersion() - this.initialVersion.getVersion()) == 1;
    }
    
    /**
     * Search a component in the getAllElements(), getAllRelationships(), getAllFolders(), getAllViews(), getAllViewObjects(), getAllViewConnections()
     * 
     * @param idOfTheComponent
     * @return the component
     */
    public EObject searchComponentFromId(String idOfTheComponent) {
		EObject obj = getAllElements().get(idOfTheComponent);
		if ( obj == null )
			obj = getAllRelationships().get(idOfTheComponent);
		if ( obj == null )
			obj = getAllFolders().get(idOfTheComponent);
		if ( obj == null )
			obj = getAllViews().get(idOfTheComponent);
		if ( obj == null )
			obj = getAllViewObjects().get(idOfTheComponent);
		if ( obj == null )
			obj = getAllViewConnections().get(idOfTheComponent);
		return obj;
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
     * List all the elements IDs that have been changed during the import process in copy mode
     */
    @Getter private Map<String, String> allCopiedElements = new LinkedHashMap<String, String>();
    
    /**
     * List all the relationship IDs that have been changed during the import process in copy mode
     */
    @Getter private Map<String, String> allCopiedRelationships = new LinkedHashMap<String, String>();
    
    /**
     * List all the views IDs that have been changed during the import process in copy mode
     */
    @Getter private Map<String, String> allCopiedView = new LinkedHashMap<String, String>();

    /**
     * List all the view objects IDs that have been changed during the import process in copy mode
     */
    @Getter private Map<String, String> allCopiedViewObjects = new LinkedHashMap<String, String>();
    
    /**
     * List all the view connections IDs that have been changed during the import process in copy mode
     */
    @Getter private Map<String, String> allCopiedViewConnections = new LinkedHashMap<String, String>();
    
    /**
     * List of all conflicts in the model and the option chosen by the user.<br>
     */
    @Getter private Map<EObject, CONFLICT_CHOICE> allConflicts = new LinkedHashMap<EObject, CONFLICT_CHOICE>();
    
    /**
     * @return the list of all the image paths in the model.
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
        this.allConflicts.clear();
    }

    /**
     * Checks if a component is part of one of the model's folders.
     * @param eObject the component to search for
     * @return the folder that contains the component, null if no folder contains it.
     */
    public IFolder getFolder(EObject eObject) {
        return getFolder(eObject, getDefaultFolderForObject(eObject));
    }

    /**
     * Check if a component is part of a folder or one of its sub folders.
     * @param eObject the component to search for
     * @param folder root folder to recursively search in
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
     * @throws NoSuchAlgorithmException 
     * @throws UnsupportedEncodingException 
     * @throws Exception 
     */
    public void countAllObjects() throws NoSuchAlgorithmException, UnsupportedEncodingException, Exception {
        // First, we reset all the counters as they can be be re-populated
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

        try {
        	for (IFolder folder: getFolders())
        		countObject(folder, true);
        } catch (@SuppressWarnings("unused") NullPointerException err)  {
        	// in case the NullPointerException is propagated here, then it means that an object has been deleted from the model, so we need to re-count all the objects
        	countAllObjects();
        }
    }

    // the viewChecksum variable is a trick to include the connections checksums in the view checksum
    private StringBuilder viewChecksum = null;
    /**
     * Adds a specific object in the corresponding counter<br>
     * At the same time, we calculate the current checksums
     * @param eObject 
     * @param mustCalculateChecksum 
     * @return the concatenation of the checksums of all the eObject components
     * @throws Exception when the eObject class is unknown (should never happen, but who knows ...)
     * @throws UnsupportedEncodingException 
     * @throws NoSuchAlgorithmException 
     * @throws NullPointerException when the calculateChecksum method raised a NullPointerException and the corresponding eObject has been removed from the model
     */
    @SuppressWarnings("null")
    public String countObject(EObject eObject, boolean mustCalculateChecksum) throws NoSuchAlgorithmException, UnsupportedEncodingException, NullPointerException, Exception {
        StringBuilder checksumBuilder = null;
        DBMetadata objectMetadata = getDBMetadata(eObject);
        int len = 0;

        if ( mustCalculateChecksum ) {
        	// if the eObject is a view and if it is not yet in the allViews map, then we empty the existing screenshot
        	if ( eObject instanceof IDiagramModel && (this.allViews.get(((IIdentifier)eObject).getId()) == null) )
        		objectMetadata.getScreenshot().dispose();
            try {
            	checksumBuilder = new StringBuilder(DBChecksum.calculateChecksum(eObject));
            } catch (NullPointerException err)  {
            	if ( DBGui.question("Hummm, that's weird !\n\nAn inconsistency has been detected on "+objectMetadata.getDebugName()+":\n   "+err.getMessage()+"\n\nWhat do you wish to do ?\n   - Ignore -> Keep the faulty component untouched and try to fix the issue manually\n   - Delete -> Delete the faulty component to fix the model consistency", new String[] {"Ignore","Delete"}) == 0 ) {
            		// We must ignore the error, so we just set the checksum to empty (as we couldn't calculate it)
            		checksumBuilder = new StringBuilder();
            	} else {
            		// We delete the object from the model, using a command that can be rolled-back
            		
            		// Stores the list of objects that needs to be removed from the model
            		Set<EObject> toBeDeleted = new HashSet<EObject>();
            		
            		// if the eObject is an archimate element or relationship, we need to delete the corresponding graphical objects and connections in all the views
                    if (eObject instanceof IArchimateElement) {
                		for (IDiagramModelObject obj : ((IArchimateElement)eObject).getReferencingDiagramObjects())
                			toBeDeleted.add(obj);
                	} else if ( eObject instanceof IArchimateRelationship) {
                		for (IDiagramModelArchimateConnection conn : ((IArchimateRelationship)eObject).getReferencingDiagramConnections())
                			toBeDeleted.add(conn);
                	}
                    // then we delete the eObject itself
            		toBeDeleted.add(eObject);
            		
            		// loop that effectively delete things
            		CompoundCommand deleteCommand = new NonNotifyingCompoundCommand("inconsistant object");
            		for (EObject objectToDelete: toBeDeleted) {
            			if (objectToDelete instanceof IArchimateElement)
            				deleteCommand.add(new DeleteArchimateElementCommand((IArchimateElement)objectToDelete));
            			else if ( objectToDelete instanceof IArchimateRelationship)
            				deleteCommand.add(new DeleteArchimateRelationshipCommand((IArchimateRelationship)objectToDelete));
            			else if (objectToDelete instanceof IDiagramModelObject)
            	            deleteCommand.add(new DBDeleteDiagramObjectCommand(this, (IDiagramModelObject)objectToDelete));
           	            else if (objectToDelete instanceof IDiagramModelConnection)
            	            deleteCommand.add(new DBDeleteDiagramConnectionCommand(this, (IDiagramModelConnection)objectToDelete));
            		}
            		
            		// execute the command to effectively delete the component from the model
        	        if ( deleteCommand.canExecute() )
        	        	deleteCommand.execute();
            		
            		// we forward the exception that the countAllObject can re-count
        			throw new NullPointerException("Components have been removed from the model.");
            	}
            }
            len = checksumBuilder.length();
        }

        switch ( eObject.eClass().getName() ) {
            case "ArchimateDiagramModel":
            case "CanvasModel":
            case "SketchModel":                     this.allViews.put(((IIdentifier)eObject).getId(), (IDiagramModel)eObject);
            
										            if ( mustCalculateChecksum ) {
										            	String checksum = checksumBuilder.toString();
										            	objectMetadata.getCurrentVersion().setContainerChecksum(checksum);
										            	objectMetadata.getCurrentVersion().setChecksum(checksum);
										            	objectMetadata.setChecksumValid(true);
										                this.viewChecksum = new StringBuilder(checksumBuilder.toString());
										            }
										            
										            for ( EObject child: ((IDiagramModel)eObject).getChildren() )
										                countObject(child, mustCalculateChecksum);
										            
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
									                	objectMetadata.getCurrentVersion().setContainerChecksum(checksumBuilder.toString());
									                	if ( this.viewChecksum != null )
									                	    this.viewChecksum.append(checksumBuilder.toString());
									                }
									                
										            if ( eObject instanceof IDiagramModelContainer ) {
									                	for ( EObject child: ((IDiagramModelContainer)eObject).getChildren() ) {
									                	    String subChecksum = countObject(child, mustCalculateChecksum);
									                	    if ( mustCalculateChecksum ) {
									                	        checksumBuilder.append(subChecksum);
									                	        if ( this.viewChecksum != null )
									                	            this.viewChecksum.append(subChecksum);
									                	    }
									                	}
										            }
										            
										            if ( eObject instanceof IConnectable) {
										                for ( EObject source: ((IConnectable)eObject).getSourceConnections() ) {
										                    String subChecksum = countObject(source, mustCalculateChecksum);
										                    if ( mustCalculateChecksum && (this.viewChecksum != null) )
										                        this.viewChecksum.append(subChecksum);
										                }
										                
										                // we count only the source connections in order to count them only once
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
                                                        String subChecksum = countObject(source, mustCalculateChecksum);
                                                        if ( mustCalculateChecksum && (this.viewChecksum != null) )
                                                            this.viewChecksum.append(subChecksum);
                                                    }
            
										            break;
										
			case "Folder":							this.allFolders.put(((IFolder)eObject).getId(), (IFolder)eObject);
			
										            // We sort the sub folders in order to guarantee the checksum changes when a sub-folder changes
			                                        List<IFolder> sortedFolder = new ArrayList<IFolder>(((IFolder)eObject).getFolders());
			                                        sortedFolder.sort(this.folderComparator);
										            for ( IFolder subFolder: sortedFolder ) {
														// we fix the folder type
														subFolder.setType(FolderType.USER);
										                countObject(subFolder, mustCalculateChecksum);
										                if ( mustCalculateChecksum ) checksumBuilder.append(subFolder.getId());
										            }
										
	                                                  // We sort the sub folders in order to guarantee the checksum changes when an element changes
                                                    List<EObject> sortedChild = new ArrayList<EObject>(((IFolder)eObject).getElements());
                                                    sortedChild.sort(this.objectComparator);
										            for ( EObject child: sortedChild ) {
										                countObject(child, mustCalculateChecksum);
										                if ( mustCalculateChecksum ) checksumBuilder.append(((IIdentifier)child).getId());
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
            objectMetadata.getCurrentVersion().setChecksum(checksum);
            return checksum;
        }

        return null;
    }
    
    private Comparator<IFolder> folderComparator = new Comparator<IFolder>() {
        @Override
        public int compare(IFolder f1, IFolder f2) {
            return f1.getName().compareTo(f2.getName());
        }
    };
    
    private Comparator<EObject> objectComparator = new Comparator<EObject>() {
        @Override
        public int compare(EObject o1, EObject o2) {
            return ((INameable)o1).getName().compareTo(((INameable)o2).getName());
        }
    };
    
    /**
     * register that an element has been copied during the import process. So we keep the its old and new ID.
     */
    public void registerCopiedElement(String oldId, String newId) {
   		this.allCopiedElements.put(oldId, newId);
    }
    
    /**
     * Get the ID of the copied element
     * @return the Id of the copied element, or the Id of the element itself if it has not been copied
     */
    public String getNewElementId(String oldId) {
    	return getNewId(this.allCopiedElements,oldId);
    }
    
    /**
     * register that a relationship has been copied during the import process. So we keep the its old and new ID.
     */
    public void registerCopiedRelationship(String oldId, String newId) {
   		this.allCopiedRelationships.put(oldId, newId);
    }
    
    /**
     * Get the ID of the copied relationship
     * @return the Id of the copied relationship, or the Id of the relationship itself if it has not been copied
     */
    public String getNewRelationshipId(String oldId) {
    	return getNewId(this.allCopiedRelationships,oldId);
    }
    
    /**
     * register that a view has been copied during the import process. So we keep the its old and new ID.
     */
    public void registerCopiedView(String oldId, String newId) {
  		this.allCopiedView.put(oldId, newId);
    }
    
    /**
     * Get the ID of the copied view object
     * @return the Id of the copied view object, or the Id of the view object itself if it has not been copied
     */
    public String getNewViewId(String oldId) {
    	return getNewId(this.allCopiedView,oldId);
    }
    
    /**
     * register that a view object has been copied during the import process. So we keep the its old and new ID.
     */
    public void registerCopiedViewObject(String oldId, String newId) {
  		this.allCopiedViewObjects.put(oldId, newId);
    }
    
    /**
     * Get the ID of the copied view object
     * @return the Id of the copied view object, or the Id of the view object itself if it has not been copied
     */
    public String getNewViewObjectId(String oldId) {
    	return getNewId(this.allCopiedViewObjects,oldId);
    }
    
    /**
     * register that a view connection has been copied during the import process. So we keep the its old and new ID.
     */
    public void registerCopiedViewConnection(String oldId, String newId) {
   		this.allCopiedViewConnections.put(oldId, newId);
    }
    
    /**
     * Get the ID of the copied view object
     * @return the Id of the copied view object, or the Id of the view object itself if it has not been copied
     */
    public String getNewViewConnectionId(String oldId) {
    	return getNewId(this.allCopiedViewConnections,oldId);
    }
    
    private static String getNewId(Map<String, String> map, String oldId) {
    	String newId = map.get(oldId);
    	
    	if ( newId == null )
    		return oldId;
    	return newId;
    }
    
    /**
     * register that the source of the relationship the concept with ID = sourceId<br>
     * It is registered as it may not be imported in the model when the relationship is created, so it will need to be resolved later (see {@link #resolveSourceAndTargetRelationships()})<br>
     * <br>
     * As all the elements are imported before the relationships, the source of the relationship is another relationship (else, the element would have been existing in the model) 
     * @param relationship
     * @param sourceId
     * @throws Exception
     */
    public void registerSourceRelationship(IArchimateRelationship relationship, String sourceId) throws Exception {
        if ( (sourceId != null) && (sourceId.length() != 0) )
        	this.allSourceRelationshipsToResolve.put(relationship, sourceId);
    }

    /**
     * register that the target of the relationship the concept with ID = sourceId<br>
     * It is registered as it may not be imported in the model when the relationship is created, so it will need to be resolved later (see {@link #resolveSourceAndTargetRelationships()})<br>
     * <br>
     * As all the elements are imported before the relationships, the target of the relationship is another relationship (else, the element would have been existing in the model) 
     * @param relationship
     * @param targetId
     * @throws Exception
     */
    public void registerTargetRelationship(IArchimateRelationship relationship, String targetId) throws Exception {
        if ( (targetId != null) && (targetId.length() != 0) )
        	this.allTargetRelationshipsToResolve.put(relationship, targetId);
    }
    
    /**
     * resolves the source and target relationships (see {@link #registerSourceRelationship(IArchimateRelationship relationship, String sourceId)} and @{link registerTargetRelationship(IArchimateRelationship relationship, String targetId)})
     * @throws Exception 
     */
    public void resolveSourceAndTargetRelationships() throws Exception {
        logger.info("Resolving source relationships.");
        for ( Map.Entry<IArchimateRelationship, String> entry: this.allSourceRelationshipsToResolve.entrySet() ) {
            IArchimateRelationship relationship = entry.getKey();

            // The source can be either a relationship or an element
            IArchimateConcept source = this.getAllRelationships().get(getNewRelationshipId(entry.getValue()));
            if ( source == null )
                source = this.getAllElements().get(getNewElementId(entry.getValue()));
        
            if ( source == null )
            	throw new Exception("Failed to resolve source relationship for "+getDBMetadata(relationship).getDebugName());

            relationship.setSource(source);
            source.getSourceRelationships().add(relationship);
        }
        
        logger.info("Resolving target relationships.");
        for ( Map.Entry<IArchimateRelationship, String> entry: this.allTargetRelationshipsToResolve.entrySet() ) {
            IArchimateRelationship relationship = entry.getKey();
            
            // The target can be either a relationship or an element
            IArchimateConcept target = this.getAllRelationships().get(getNewRelationshipId(entry.getValue()));
            if ( target == null )
                target = this.getAllElements().get(getNewElementId(entry.getValue()));
        
            if ( target == null )
            	throw new Exception("Failed to resolve target relationship for "+getDBMetadata(relationship).getDebugName());

            relationship.setTarget(target);
            target.getTargetRelationships().add(relationship);
        }

        this.allSourceRelationshipsToResolve.clear();
        this.allTargetRelationshipsToResolve.clear();
        this.allCopiedElements.clear();
        this.allCopiedRelationships.clear();
    }
    


    public void registerSourceConnection(IDiagramModelConnection connection, String sourceId) throws Exception {
        if ( (sourceId != null) && (sourceId.length() != 0) )
        	this.allSourceConnectionsToResolve.put(connection, sourceId);
    }

    public void registerTargetConnection(IDiagramModelConnection connection, String targetId) throws Exception {
        if ( (targetId != null) && (targetId.length() != 0) )
        	this.allTargetConnectionsToResolve.put(connection, targetId);
    }

    public void resolveSourceAndTargetConnections() throws Exception {
        logger.info("Resolving source connections.");
        for ( Map.Entry<IDiagramModelConnection, String> entry: this.allSourceConnectionsToResolve.entrySet() ) {
            IDiagramModelConnection connection = entry.getKey();
            
            // The source can be either a view connection or a view object
            IConnectable source = this.getAllViewConnections().get(getNewViewConnectionId(entry.getValue()));
            if ( source == null )
                source = this.getAllViewObjects().get(getNewViewObjectId(entry.getValue()));
        
            if ( source == null )
            	throw new Exception("Failed to resolve source connection for "+getDBMetadata(connection).getDebugName());

            connection.setSource(source);
            source.addConnection(connection);
        }

        logger.info("Resolving target connections.");
        for ( Map.Entry<IDiagramModelConnection, String> entry: this.allTargetConnectionsToResolve.entrySet() ) {
            IDiagramModelConnection connection = entry.getKey();
            
            // The source can be either a view connection or a view object
            IConnectable target = this.getAllViewConnections().get(getNewViewConnectionId(entry.getValue()));
            if ( target == null )
            	target = this.getAllViewObjects().get(getNewViewObjectId(entry.getValue()));
        
            if ( target == null )
            	throw new Exception("Failed to resolve target connection for "+getDBMetadata(connection).getDebugName());

            connection.setSource(target);
            target.addConnection(connection);
        }

        this.allSourceConnectionsToResolve.clear();
        this.allTargetConnectionsToResolve.clear();
        this.allCopiedViewObjects.clear();
        this.allCopiedViewConnections.clear();
    }
    
	/**
	 * HashMap containing the DBMetadata classes of model's components
	 */
	ConcurrentHashMap<String, DBMetadata> DBMetadataHashMap = new ConcurrentHashMap<String, DBMetadata>();
    
    /**
     * Gets the DBMetadata associated with an Archi component. If it does not exist yet, it is automatically created.
     * @param obj : the Archi component
     * @return the associated DBMetadata class. The returned value can be null if the object hasn't got an ID (is not of IIdentifier class) 
     */
    public DBMetadata getDBMetadata(EObject obj) {
    	if ( obj instanceof IIdentifier ) {
    		String objId = ((IIdentifier)obj).getId();
	    	DBMetadata dbMetadata = this.DBMetadataHashMap.get(objId);
	    	if ( dbMetadata == null ) {
	    		// in case the object metadata does not exist, we create it
	    		dbMetadata = new DBMetadata(obj);
	    		this.DBMetadataHashMap.put(objId, dbMetadata);
	    	}
	    	return dbMetadata;
    	}
    	return null;
    }
    
    /**
     * Gets the model the archimate object belongs to
     * @param obj the archimate object
	 * @return the DBArchimatemodel object, null if not found
	 */
     public static DBArchimateModel getDBArchimateModel(EObject obj) {
		if ( (obj != null) && (obj instanceof IArchimateModelObject) ) {
			EObject container = ((IArchimateModelObject)obj).eContainer();
			DBArchimateModel model = (DBArchimateModel) ((IArchimateModelObject)obj).getArchimateModel();
			// in some weird occasions, the getArchimateModel() method returns null
			// in this case, we loop on the eContainer until we found a model
			while ( (container != null) && (model == null) ) {
				if ( container instanceof IArchimateModelObject ) {
					model = (DBArchimateModel) ((IArchimateModelObject)container).getArchimateModel();
					container = ((IArchimateModelObject)container).eContainer();
				} else
					return null;
			}
			return model;
		}
		return null;
	}
    
    @Override
	public String getPurpose() {
    	 String lPurpose = super.getPurpose();
    	 if ( lPurpose == null )
    		 return "";
    	 return lPurpose;
     }
}
