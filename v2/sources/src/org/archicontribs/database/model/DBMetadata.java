/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBScreenshot;
import org.archicontribs.database.data.DBVersion;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.ICanvasModelBlock;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IAssociationRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelImage;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFeatures;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProfile;
import com.archimatetool.model.IProfiles;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

import lombok.Getter;
import lombok.Setter;

/**
 * This class defines the metadata attached to every model components.
 * 
 * @author Herve Jouin 
 * @see org.archicontribs.database.model.IDBMetadata
 */
@SuppressWarnings("unused")
public class DBMetadata  {
	DBLogger logger = new DBLogger(DBMetadata.class);
	
    /**
     * Component that contains the DBMetadata<br>
     * This property is set during the component initialization and is used to calculate the component checksum
     */
    @Getter private EObject component;
    
    /**
     * Id of the object that is stored before the component has been imported from the database
     */
    private String id = null;

    /**
     * Version of the component as it was during last import/export, or zero if it was loaded from an archimate file
     */
    @Getter DBVersion initialVersion = new DBVersion();


    /**
     * Version of the component as it is now (recalculated on every import/export procedure)<br>
     * Will be copied to initialVersion after a successful export
     * 
     */
    @Getter DBVersion currentVersion = new DBVersion();

    /**
     * Version of the component as it is in the database version of the model
     */
    @Getter DBVersion databaseVersion = new DBVersion();

    /**
     * Latest version of the component in the database whichever the model that modified the component 
     */
    @Getter DBVersion latestDatabaseVersion = new DBVersion();
    
    // special variables and methods to handle screenshots if component is a IDiagramModel
    
    /**
     * Screenshot of the view (if the component is a view)
     */
    @Getter @Setter DBScreenshot screenshot = null;
    
    /**
     * Used to remember if the component has been exported
     */
    @Getter @Setter private boolean exported = false;

    /**
     * Used by views, set to false if some components are removed during the export process so their checksum needs to be recalculated
     */
    @Getter @Setter private boolean checksumValid = true;

    public DBMetadata(EObject componentObject) {
        assert ( componentObject instanceof IIdentifier );
        this.component = componentObject;
        
        if ( componentObject instanceof IDiagramModel )
        	this.screenshot = new DBScreenshot();
    }
    
    public DBMetadata(String componentId) {
        this.component = null;
        this.id = componentId;
    }
    
    public DBMetadata() {
        this.component = null;
        this.id = null;
    }

    /**
     * Gives a status of the component regarding it's database version:<br>
     * <li><b>isSynced</b></li> The component exists in both the database and the model, and they are in sync
     * <li><b>isNewInModel</b></li> The component exists in the model but not in the database<br>
     * <li><b>isUpdatedInDatabase</b></li> The component exists in both the model and the database but the database version has been updated since it has been imported
     * <li><b>isUpdatedInModel</b></li> The component exists in both the model and the database but the model version has been updated since it has been imported
     * <li><b>isDeletedInDatabase</b></li> The component exists in the database but is not associated to the model anymore
     * <li><b>isConflicting</b></li> The component has been updated in both the model and the database
     * <br><br>
     * <b><u>Limits:</u></b><br>
     * This status requires that the component exists in the model. Thus, it is not possible to calculate this way  
     * <li><b><strike>isNewInDatabase</strike></b></li> The component does not exist in the model but has been created in the database<br>
     * <li><b><strike>isDeletedInModel</strike></b></li> The component does not exist in the model because it has been deleted from the model
     */
    public enum DATABASE_STATUS {isSynced, isNewInModel, isNewInDatabase, isUpadtedInDatabase, isUpdatedInModel, isDeletedInDatabase, isConflicting}

    /**
     * Gets the status of the component<br>
     * @see COMPONENT_STATUS
     */
    public DATABASE_STATUS getDatabaseStatus() {
        if ( this.initialVersion.getVersion() == 0 ) {
            if ( getComponent() == null ) {
                // the component does not exist in the model, thus it is new in the database
                return DATABASE_STATUS.isNewInDatabase;
            }
            // The component does exist in the model but has not been found in the database, thus it is a new component in the model
            return DATABASE_STATUS.isNewInModel;
        }
        
        if ( this.databaseVersion.getVersion() == 0 ) {
            // if the databaseVersion is zero, then it means that the component does not exist in the database version of the model anymore
            return DATABASE_STATUS.isDeletedInDatabase;
        }
        
        if ( this.initialVersion.getVersion() == this.latestDatabaseVersion.getVersion() ) {
            // if the initialVersion equals the latestDatabaseVersion, then the component has not been updated in the database
            
            if ( DBPlugin.areEqual(this.currentVersion.getChecksum(), this.initialVersion.getChecksum()) ) {
                // if the checksum are identical, it means that the component has not been updated since it has been imported
                return DATABASE_STATUS.isSynced;
            }

            // else, it means that it has been updated since it has been imported
            return DATABASE_STATUS.isUpdatedInModel;
        }
        
        // else, it means that the component has been updated in the database 

        String initialChecksum = this.initialVersion.getChecksum();
        String currentChecksum = this.currentVersion.getChecksum();
        String databaseChecksum= this.latestDatabaseVersion.getChecksum();

        // if the components checksum in the model has been modified since the component has been imported
        // this means that the component has been updated in the model
        boolean modifiedInModel = !DBPlugin.areEqual(initialChecksum, currentChecksum);

        // if the components checksum in the database has been modified since the component has been imported
        // this means that the component has been updated in the database 
        boolean modifiedInDatabase = !DBPlugin.areEqual(initialChecksum, databaseChecksum);

        // if both versions of the component (in the model and in the database) have been updated
        // then they are conflicting ...
        // ... except if the modifications are the same
        // ... except if the component is a folder (there is no conflict on folders as the conflicts are managed using their content)
        if ( modifiedInModel && modifiedInDatabase ) {
            if ( DBPlugin.areEqual(currentChecksum, databaseChecksum) )
                return DATABASE_STATUS.isSynced;

            if ( getComponent() instanceof IFolder )
                return DATABASE_STATUS.isUpdatedInModel;
            
            return DATABASE_STATUS.isConflicting;
        }

        // if we're here, it means that the component has been updated either in the model, either in the database
        if ( modifiedInModel )
            return DATABASE_STATUS.isUpdatedInModel;

        if ( modifiedInDatabase )
            return DATABASE_STATUS.isUpadtedInDatabase;

        return DATABASE_STATUS.isSynced;
    }

    /**
     * This method checks all the views objects and connections in a view and return those that reference the current Archimate concept
     * @return the list of views objects or connections that reference the concept
     * @param view view in which the concept should be searched in
     */
    public List<IConnectable> findConnectables(IArchimateDiagramModel view) {
        List<IConnectable> connectables = new ArrayList<IConnectable>();

        if ( (this.component instanceof IArchimateConcept) && (view != null) ) {
            for ( IDiagramModelObject viewObject: view.getChildren() ) {
                connectables.addAll(findConnectables((IDiagramModelArchimateComponent)viewObject, (IArchimateConcept)this.component));
            }
        }
        return connectables;
    }

    /**
     * This method checks all the views objects and connections in a view and return those that reference a specific Archimate concept
     * @return the list of views objects or connections that reference the concept
     * @param view view in which the concept should be searched in
     * @param concept Archimate concept to search in the view
     */
    public List<IConnectable> findConnectables(IArchimateDiagramModel view, IArchimateConcept concept) {
        List<IConnectable> connectables = new ArrayList<IConnectable>();

        if ( (this.component instanceof IArchimateConcept) && (view != null) ) {
            for ( IDiagramModelObject viewObject: view.getChildren() ) {
                connectables.addAll(findConnectables((IDiagramModelArchimateComponent)viewObject, concept));
            }
        }
        return connectables;
    }

    /**
     * This method checks if the view object references the concept. If the view object is a container, the method chechs recusrsively all the included view objects.
     * @return the list of views objects that reference the concept
     * @param parentComponent View object in which the concept should be searched in
     * @param concept Archimate concept to search in the view object
     */
    private List<IConnectable> findConnectables(IDiagramModelArchimateComponent parentComponent, IArchimateConcept concept) {
        List<IConnectable> connectables = new ArrayList<IConnectable>();

        if ( concept instanceof IArchimateElement ) {
            if ( DBPlugin.areEqual(parentComponent.getArchimateConcept().getId(), concept.getId()) )
                connectables.add(parentComponent);
        } else if ( concept instanceof IArchimateRelationship ) {
            for ( IDiagramModelConnection conn: parentComponent.getSourceConnections() ) {
                if ( DBPlugin.areEqual(conn.getSource().getId(), concept.getId()) ) connectables.add(conn);
                if ( DBPlugin.areEqual(conn.getTarget().getId(), concept.getId()) ) connectables.add(conn);
            }
            for ( IDiagramModelConnection conn: parentComponent.getTargetConnections() ) {
                if ( DBPlugin.areEqual(conn.getSource().getId(), concept.getId()) ) connectables.add(conn);
                if ( DBPlugin.areEqual(conn.getTarget().getId(), concept.getId()) ) connectables.add(conn);
            }
        }

        if ( parentComponent instanceof IDiagramModelContainer ) {
            for ( IDiagramModelObject child: ((IDiagramModelContainer)parentComponent).getChildren() ) {
                connectables.addAll(findConnectables((IDiagramModelArchimateComponent)child, concept));
            }
        }
        return connectables;
    }


    /**
     * Calculates the full name of the component 
     * @return getclass().getSimpleName()+":\""+getName()+"\""
     */
    public String getFullName() {
        String name = getName(); 
        
        // if the component does not exist, this just return its name
        if ( this.component == null ) {
            if ( name == null )
                return "null:\"null\"";
            return name;
        }

        return new StringBuilder(this.component.getClass().getSimpleName()).append(":\""+name+"\"").toString();
    }

    /**
     * Calculates the debug name of the component 
     * @return getclass().getSimpleName()+":\""+getName()+"\"("+getId()+")"
     */
    public String getDebugName() {
        return new StringBuilder(getFullName()).append("("+getId()+")").toString();
    }

    //   H E L P E R   M E T H O D S

    // ArchimateConcept
    public IArchimateConcept getArchimateConcept() {
        if ( this.component instanceof IDiagramModelArchimateComponent )
            return ((IDiagramModelArchimateComponent)this.component).getArchimateConcept();
        return null;
    }

    public void setArchimateConcept(IArchimateConcept concept) {
        if ( this.component instanceof IDiagramModelArchimateComponent && (concept != null))
            ((IDiagramModelArchimateComponent)this.component).setArchimateConcept(concept);
    }
    
    public String getArchimateConceptId() {
        if ( this.component instanceof IDiagramModelArchimateComponent )
            return ((IDiagramModelArchimateComponent)this.component).getArchimateConcept().getId();
        return null;
    }

    // ReferencedModel
    public IDiagramModel getReferencedModel() {
        if ( this.component instanceof IDiagramModelReference )
            return ((IDiagramModelReference)this.component).getReferencedModel();
        return null;
    }
    
    public String getReferencedModelId() {
        if ( this.component instanceof IDiagramModelReference )
            return ((IDiagramModelReference)this.component).getReferencedModel().getId();
        return null;
    }

    public void setReferencedModel(IDiagramModel view) {
        if ( (this.component instanceof IDiagramModelReference) && (view != null) )
            ((IDiagramModelReference)this.component).setReferencedModel(view);
    }

    // Type (Int)
    public Integer getType() {
        if ( this.component instanceof IDiagramModelArchimateObject )
            return ((IDiagramModelArchimateObject)this.component).getType();
        if ( this.component instanceof IDiagramModelArchimateConnection )
            return ((IDiagramModelArchimateConnection)this.component).getType();
        if ( this.component instanceof IDiagramModelConnection )
            return ((IDiagramModelConnection)this.component).getType();
        return null;
    }

    public void setType(Integer type) {
        if ( type != null ) {
            if ( (this.component instanceof IDiagramModelArchimateObject) && (((IDiagramModelArchimateObject)this.component).getType() != type.intValue()) )
                ((IDiagramModelArchimateObject)this.component).setType(type);
            else if ( (this.component instanceof IDiagramModelArchimateConnection) && ((IDiagramModelArchimateConnection)this.component).getType() != type.intValue())
                ((IDiagramModelArchimateConnection)this.component).setType(type);
            else if ( (this.component instanceof IDiagramModelConnection) && (((IDiagramModelConnection)this.component).getType() != type.intValue()) )
                ((IDiagramModelConnection)this.component).setType(type);
        }
    }

    // Type (String)
    public String getJunctionType() {
        if ( this.component instanceof IJunction )
            return ((IJunction)this.component).getType();
        return null;
    }

    public void setType(String type) {
        if ( (this.component instanceof IJunction) && (type != null) && !DBPlugin.areEqual(((IJunction)this.component).getType(), type) )
            ((IJunction)this.component).setType(type);
    }

    // FolderType
    public FolderType getFolderType() {
        if ( this.component instanceof IFolder )
            return ((IFolder)this.component).getType();
        return null;
    }

    public void setFolderType(FolderType type) {
        if ( (this.component instanceof IFolder) && (type != null) && (((IFolder)this.component).getType().getValue() != type.getValue()) )
            ((IFolder)this.component).setType(type);
    }
    
    public void setFolderType(Integer type) {
        if ( (this.component instanceof IFolder) && (type != null) && (((IFolder)this.component).getType().getValue() != type) )
            ((IFolder)this.component).setType(FolderType.get(type.intValue()));
    }
    
    // RootFolderType
    public Integer getRootFolderType() {
        if ( this.component instanceof IFolder ) {
            IFolder folder = (IFolder)this.component;
            while ( folder.getType().getValue() == FolderType.USER_VALUE ) {
                if ( folder.eContainer() instanceof IFolder )
                    folder = (IFolder)folder.eContainer();
                else break;
            }
            return folder.getType().getValue();
        }
        return null;
    }
    
    // defaultFolderType
    public static int getDefaultFolderType(String clazz) {
    	switch ( clazz.toLowerCase() ) {
	        case "capability":
	        case "courseofaction":
	    	case "resource":
	        	return FolderType.STRATEGY_VALUE;
	        	
	        case "product":
	        case "businessactor":
	        case "businessrole":
	        case "businesscollaboration":
	        case "businessinterface":
	        case "businessprocess":
	        case "businessfunction":
	        case "businessinteraction":
	        case "businessevent":
	        case "businessservice":
	        case "businessobject":
	        case "contract":
	        case "representation":
	        	return FolderType.BUSINESS_VALUE;

	        case "applicationcomponentlabel":
	        case "applicationcollaboration":
	        case "applicationinterface":
	        case "applicationfunction":
	        case "applicationinteraction":
	        case "applicationevent":
	        case "applicationservice":
	        case "applicationprocess":
	        case "dataObject":
	        	return FolderType.APPLICATION_VALUE;

	        case "artifact":
	        case "technologyfunction":
	        case "technologyprocess":
	        case "technologyinteraction":
	        case "technologyevent":
	        case "technologyservice":
	        case "technologycollaboration":
	        case "technologyinterface":
	        case "node":
	        case "device":
	        case "systemsoftware":
	        case "path":
	        case "communicationnetwork":
	        	
	        case "material":
	        case "equipment":
	        case "facility":
	        case "distributionnetwork":
	        case "workpackage":
	        case "deliverable":
	        case "implementationevent":
	        case "plateau":
	        case "gap":
	        	return FolderType.TECHNOLOGY_VALUE;

	        case "stakeholder":
	        case "driver":
	        case "assessment":
	        case "goal":
	        case "outcome":
	        case "principle":
	        case "requirement":
	        case "constaint":
	        case "smeaning":
	        case "value":
	        	return FolderType.MOTIVATION_VALUE;

	        case "grouping":
	        case "location":
	        case "junction":
	        	return FolderType.OTHER_VALUE;
	        
	        case "archimatediagrammodel":
	        case "canvasmodel":
	        case "sketchmodel":
	        	return FolderType.DIAGRAMS_VALUE;
	        	
	        default:
	        	if ( clazz.toLowerCase().endsWith("relationship") )
	        		return FolderType.RELATIONS_VALUE;
	        	return 0;
    	}
    }

    // BorderColor
    public String getBorderColor() {
        if ( this.component instanceof IBorderObject )
            return ((IBorderObject)this.component).getBorderColor();
        return null;
    }

    public void setBorderColor(String borderColor) {
        if ( (this.component instanceof IBorderObject) && (borderColor != null) && !DBPlugin.areEqual(((IBorderObject)this.component).getBorderColor(), borderColor) )
            ((IBorderObject)this.component).setBorderColor(borderColor);
    }

    // BorderType
    public Integer getBorderType() {
        if ( this.component instanceof IDiagramModelNote ) 
            return ((IDiagramModelNote)this.component).getBorderType();
        return null;
    }

    public void setBorderType(Integer borderType) {
        if ( (this.component instanceof IDiagramModelNote) && (borderType != null) && ((IDiagramModelNote)this.component).getBorderType() != borderType.intValue() ) 
            ((IDiagramModelNote)this.component).setBorderType(borderType);
    }

    // Content
    public String getContent() {
        if ( this.component instanceof ITextContent ) 
            return ((ITextContent)this.component).getContent();
        return null;
    }
    public void setContent(String content) {
        if ( (this.component instanceof ITextContent) && (content != null) && !DBPlugin.areEqual(((ITextContent)this.component).getContent(), content) ) 
            ((ITextContent)this.component).setContent(content);
    }

    // Documentation
    public String getDocumentation() {
        if ( (this.component instanceof IDocumentable) && !(this.component instanceof IDiagramModelArchimateComponent) )
            return ((IDocumentable)this.component).getDocumentation();
        return null;
    }

    public void setDocumentation(String documentation) {
        if ( (this.component instanceof IDocumentable) && !(this.component instanceof IDiagramModelArchimateComponent) && (documentation != null) && !DBPlugin.areEqual(((IDocumentable)this.component).getDocumentation(), documentation) )  
            ((IDocumentable)this.component).setDocumentation(documentation);
    }
    
    // Name
    public String getId() {
        // if the component has not be imported (yet) from the database, we return the id that has been previously stored
        if ( this.component == null )
            return this.id;
        
        if ( this.component instanceof IIdentifier )
            return ((IIdentifier)this.component).getId();
        
        return null;
    }

    /**
     * Sets the Id of the underlying archimate component
     * @param newId
     */
    public void setId(String newId) {
        // if the component has not be imported (yet) from the database, we store its id in a local variable
        if ( this.component == null )
            this.id = newId;
        else {
            if ( (this.component instanceof IIdentifier) && (newId != null) && !DBPlugin.areEqual(((IIdentifier)this.component).getId(), newId) )
                ((IIdentifier)this.component).setId(newId);
        }
    }

    // Name
    public String getName() {
        if ( this.component instanceof INameable )
            return ((INameable)this.component).getName();
        return null;
    }

    public void setName(String name) {
    	// we do not set the name if the component is a view object linked to an element or a relationship (as the name is set with the element or the relationship)
        if ( (this.component instanceof INameable && !(this.component instanceof IDiagramModelArchimateObject || this.component instanceof IDiagramModelArchimateConnection)) && (name != null) && !DBPlugin.areEqual(((INameable)this.component).getName(), name) )
            ((INameable)this.component).setName(name);
    }

    // Locked
    public Boolean isLocked() {
        if ( this.component instanceof ILockable )
            return ((ILockable)this.component).isLocked();
        return null;
    }
    public Integer isLockedAsInteger() {
        if ( this.component instanceof ILockable )
            return ((ILockable)this.component).isLocked() ? 1 : 0;
        return null;
    }
    
    public void setLocked(Object isLocked) {
        if ( this.component instanceof ILockable ) {
            boolean mustBeLocked = DBPlugin.getBooleanValue(isLocked);

            if ( ((ILockable)this.component).isLocked() != mustBeLocked )
                ((ILockable)this.component).setLocked(mustBeLocked);
        }
    }

    // ImagePath
    public String getImagePath() {   
        if ( this.component instanceof IDiagramModelImageProvider )  
            return ((IDiagramModelImageProvider)this.component).getImagePath();
        if ( this.component instanceof IDiagramModelImage )  
            return ((IDiagramModelImage)this.component).getImagePath();
        return null;
    }

    public void setImagePath(String imagePath) {   
        if ( (imagePath != null) && !DBPlugin.areEqual(((IDiagramModelImageProvider)this.component).getImagePath(), imagePath) ) {
            if ( this.component instanceof IDiagramModelImageProvider )  
                ((IDiagramModelImageProvider)this.component).setImagePath(imagePath);
            else if ( this.component instanceof IDiagramModel )  
                ((IDiagramModelImage)this.component).setImagePath(imagePath);
        }
    }

    // ImagePosition
    // until Archi 4.8, IIconic was defined in com.archimatetool.model.canvas.IIconic;
    // since Archi 4.9, IIconic is defined in  com.archimatetool.model.IIconic;
    public Integer getImagePosition() {
    	try {
    		Class<?> iIconicClass = Class.forName("com.archimatetool.model.canvas.IIconic");
    		Method getImagePositionMethod = iIconicClass.getDeclaredMethod("getImagePosition");

    		if ( iIconicClass.isInstance(this.component) )
    			return (Integer)getImagePositionMethod.invoke(this.component);
    		
    		return null;
        } catch ( NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException err) {
        	// com.archimatetool.model.canvas.IIconic class exists in Archi until version 4.8
        }
    	
    	try {
    		Class<?> iIconicClass = Class.forName("com.archimatetool.model.IIconic");
    		Method getImagePositionMethod = iIconicClass.getDeclaredMethod("getImagePosition");

    		if ( iIconicClass.isInstance(this.component) )
    			return (Integer)getImagePositionMethod.invoke(this.component);
    		
    		return null;
        } catch ( NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException err) {
        	// com.archimatetool.model.IIconic class exists in Archi from version 4.9
	    }
    	
    	this.logger.trace("IIconic is not com.archimatetool.model.canvas.IIconic nor com.archimatetool.model.IIconic");
    	
        return null;
    }

    public void setImagePosition(Integer imagePosition) {
    	try {
    		Class<?> iIconicClass = Class.forName("com.archimatetool.model.canvas.IIconic");
    		Method setImagePositionMethod = iIconicClass.getDeclaredMethod("setImagePosition", int.class);

    		if ( iIconicClass.isInstance(this.component) && (imagePosition != null) )
    			setImagePositionMethod.invoke(this.component, imagePosition);
    		
    		return;
        } catch ( NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException err) {
        	// com.archimatetool.model.canvas.IIconic class exists in Archi until version 4.8
        }
    	
    	try {
    		Class<?> iIconicClass = Class.forName("com.archimatetool.model.IIconic");
    		Method setImagePositionMethod = iIconicClass.getDeclaredMethod("setImagePosition", int.class);

    		if ( iIconicClass.isInstance(this.component) && (imagePosition != null) )
    			setImagePositionMethod.invoke(this.component, imagePosition);
    		
    		return;
        } catch ( NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException err) {
        	// com.archimatetool.model.IIconic class exists in Archi from version 4.9
        }

    	this.logger.trace("IIconic is not com.archimatetool.model.canvas.IIconic nor com.archimatetool.model.IIconic");
    }

    // LineColor
    public String getLineColor() {   
        if ( this.component instanceof ILineObject )  
            return ((ILineObject)this.component).getLineColor();
        return null;
    }

    public void setLineColor(String lineColor) {   
        if ( (this.component instanceof ILineObject) && (lineColor != null) && !DBPlugin.areEqual(((ILineObject)this.component).getLineColor(), lineColor) )  
            ((ILineObject)this.component).setLineColor(lineColor);
    }

    // FillColor
    public String getFillColor() {   
        if ( this.component instanceof IDiagramModelObject )  
            return ((IDiagramModelObject)this.component).getFillColor();
        return null;
    }

    public void setFillColor(String fillColor) {   
        if ( (this.component instanceof IDiagramModelObject) && (fillColor != null) && !DBPlugin.areEqual(((IDiagramModelObject)this.component).getFillColor(), fillColor) )  
            ((IDiagramModelObject)this.component).setFillColor(fillColor);
    }

    // LineWidth
    public Integer getLineWidth() {
        if ( this.component instanceof ILineObject ) 
            return ((ILineObject)this.component).getLineWidth();
        return null;
    }

    public void setLineWidth(Integer lineWidth) {
        if ( (this.component instanceof ILineObject) && (lineWidth != null) && ((ILineObject)this.component).getLineWidth() != lineWidth.intValue() ) 
            ((ILineObject)this.component).setLineWidth(lineWidth);
    }

    // Font
    public String getFont() { 
        if ( this.component instanceof IFontAttribute )  
            return ((IFontAttribute)this.component).getFont();
        return null;
    }

    public void setFont(String font) { 
        if ( (this.component instanceof IFontAttribute) && (font != null) && !DBPlugin.areEqual(((IFontAttribute)this.component).getFont(), font) )  
            ((IFontAttribute)this.component).setFont(font);
    }

    // FontColor
    public String getFontColor() {   
        if ( this.component instanceof IFontAttribute )  
            return ((IFontAttribute)this.component).getFontColor();
        return null;
    }

    public void setFontColor(String fontColor) {   
        if ( (this.component instanceof IFontAttribute) && (fontColor != null) && !DBPlugin.areEqual(((IFontAttribute)this.component).getFontColor(), fontColor) )  
            ((IFontAttribute)this.component).setFontColor(fontColor);
    }

    // Notes
    public String getNotes() {   
        if ( this.component instanceof ICanvasModelSticky )  
            return ((ICanvasModelSticky)this.component).getNotes();
        return null;
    }

    public void setNotes(String notes) {   
        if ( (this.component instanceof ICanvasModelSticky) && (notes != null) && !DBPlugin.areEqual(((ICanvasModelSticky)this.component).getNotes(), notes) )  
            ((ICanvasModelSticky)this.component).setNotes(notes);
    }

    // TextAlignment
    public Integer getTextAlignment() {
        if ( this.component instanceof ITextAlignment ) 
            return ((ITextAlignment)this.component).getTextAlignment();
        return null;
    }

    public void setTextAlignment(Integer textAlignment) {
        if ( (this.component instanceof ITextAlignment) && (textAlignment != null) && ((ITextAlignment)this.component).getTextAlignment() != textAlignment.intValue() ) 
            ((ITextAlignment)this.component).setTextAlignment(textAlignment);
    }

    // TextPosition
    public Integer getTextPosition() {
        if ( this.component instanceof ITextPosition ) 
            return ((ITextPosition)this.component).getTextPosition();
        if ( this.component instanceof IDiagramModelConnection )
            return ((IDiagramModelConnection)this.component).getTextPosition();
        return null;
    }

    public void setTextPosition(Integer textPosition) {
        if ( textPosition != null  ) {
            if ( (this.component instanceof ITextPosition) && ((ITextPosition)this.component).getTextPosition() != textPosition.intValue() )
                ((ITextPosition)this.component).setTextPosition(textPosition);
            else if ( (this.component instanceof IDiagramModelConnection) && ((IDiagramModelConnection)this.component).getTextPosition() != textPosition.intValue())
                ((IDiagramModelConnection)this.component).setTextPosition(textPosition);
        }
    }

    // Bounds
    public Integer getX() {
        if ( this.component instanceof IDiagramModelObject)
            return ((IDiagramModelObject)this.component).getBounds().getX();
        return null;
    }

    public Integer getY() {
        if ( this.component instanceof IDiagramModelObject)
            return ((IDiagramModelObject)this.component).getBounds().getY();
        return null;
    }

    public Integer getWidth() {
        if ( this.component instanceof IDiagramModelObject)
            return ((IDiagramModelObject)this.component).getBounds().getWidth();
        return null;
    }

    public Integer getHeight() {
        if ( this.component instanceof IDiagramModelObject)
            return ((IDiagramModelObject)this.component).getBounds().getHeight();
        return null;
    }
    public void setBounds(Integer x, Integer y, Integer width, Integer height) {
        if ( (this.component instanceof IDiagramModelObject) && (x != null) && (y != null) && (width != null) && (height != null) ) {
            IBounds bounds = ((IDiagramModelObject)this.component).getBounds();
            if ( (bounds == null) || (bounds.getX() != x.intValue()) || (bounds.getY() != y.intValue()) || (bounds.getWidth() != width.intValue()) || (bounds.getHeight() != height.intValue()) )
                ((IDiagramModelObject)this.component).setBounds(x, y, width, height);
        }
    }

    // Strength
    public String getStrength() { 
        if ( this.component instanceof IInfluenceRelationship )  
            return ((IInfluenceRelationship)this.component).getStrength();
        return null;
    }

    public void setStrength(String strength) { 
        if ( (this.component instanceof IInfluenceRelationship) && (strength != null) && !DBPlugin.areEqual(((IInfluenceRelationship)this.component).getStrength(), strength) )  
            ((IInfluenceRelationship)this.component).setStrength(strength);
    }

    // AccessType
    public Integer getAccessType() {
        if ( this.component instanceof IAccessRelationship ) 
            return ((IAccessRelationship)this.component).getAccessType();
        return null;
    }

    public void setAccessType(Integer accessType) {
        if ( (this.component instanceof IAccessRelationship) && (accessType != null) && ((IAccessRelationship)this.component).getAccessType() != accessType.intValue() ) 
            ((IAccessRelationship)this.component).setAccessType(accessType);
    }

    // ViewPoint
    public String getViewpoint() {   
        if ( this.component instanceof IArchimateDiagramModel )  
            return ((IArchimateDiagramModel)this.component).getViewpoint();
        return null;
    }

    public void setViewpoint(String viewpoint) {   
        if ( (this.component instanceof IArchimateDiagramModel) && (viewpoint != null) && !DBPlugin.areEqual(((IArchimateDiagramModel)this.component).getViewpoint(), viewpoint) )  
            ((IArchimateDiagramModel)this.component).setViewpoint(viewpoint);
    }

    // Background
    public Integer getBackground() {
        if ( this.component instanceof ISketchModel ) 
            return ((ISketchModel)this.component).getBackground();
        return null;
    }

    public void setBackground(Integer background) {
        if ( (this.component instanceof ISketchModel) && (background != null) && ((ISketchModel)this.component).getBackground() != background.intValue() ) 
            ((ISketchModel)this.component).setBackground(background);
    }

    // ConnectionRouterType
    public Integer getConnectionRouterType() {
        if ( this.component instanceof IDiagramModel ) 
            return ((IDiagramModel)this.component).getConnectionRouterType();
        return null;
    }

    public void setConnectionRouterType(Integer routerType) {
        if ( (this.component instanceof IDiagramModel) && (routerType != null) && ((IDiagramModel)this.component).getConnectionRouterType() != routerType.intValue() ) 
            ((IDiagramModel)this.component).setConnectionRouterType(routerType);
    }

    // Folder
    public IFolder getParentFolder() {
        EObject container = this.component.eContainer();
        if ( container instanceof IFolder )
            return (IFolder) container;
        return null;
    }

    public void setParentFolder(IFolder newParentFolder) {
        if ( (newParentFolder == null) || (newParentFolder == this.component) )
            return;
        
        // if the component is already part of a folder, then we need to remove it from the old folder before setting it to the new one
        IFolder oldParentFolder = getParentFolder();
        if ( (oldParentFolder != null) && (oldParentFolder != newParentFolder) ) {
            if ( this.component instanceof IFolder )
                oldParentFolder.getFolders().remove((IFolder)this.component);
            else
                oldParentFolder.getElements().remove(this.component);
        }
        
        if ( this.component instanceof IFolder )
            newParentFolder.getFolders().add((IFolder)this.component);
        else
            newParentFolder.getElements().add(this.component);
    }
    
    // Relationship Source
    public IArchimateConcept getRelationshipSource() {
        if ( this.component instanceof IArchimateRelationship )
            return ((IArchimateRelationship)this.component).getSource();
        return null;
    }
    
    public void setRelationshipSource(IArchimateConcept source) {
        if ( this.component instanceof IArchimateRelationship && (source != null) && (source != this.component) && (source != ((IArchimateRelationship)this.component).getSource()) ) {
            ((IArchimateRelationship)this.component).setSource(source);
            source.getSourceRelationships().add((IArchimateRelationship)this.component);
        }
    }
    
    // Relationship Target
    public IArchimateConcept getRelationshipTarget() {
        if ( this.component instanceof IArchimateRelationship )
            return ((IArchimateRelationship)this.component).getTarget();
        return null;
    }
    
    public void setRelationshipTarget(IArchimateConcept target) {
        if ( this.component instanceof IArchimateRelationship && (target != null) && (target != this.component) && (target != ((IArchimateRelationship)this.component).getTarget()) ) {
            ((IArchimateRelationship)this.component).setTarget(target);
            target.getTargetRelationships().add((IArchimateRelationship)this.component);
        }
    }
    
    // View connection source
    public IConnectable getSourceConnection() {
        if ( this.component instanceof IDiagramModelConnection )
            return ((IDiagramModelConnection)this.component).getSource();
        return null;
    }
    
    public void setSourceConnection(IConnectable source) {
        if ( this.component instanceof IDiagramModelConnection && (source != null) && (source != this.component) && (source != ((IDiagramModelConnection)this.component).getSource()) ) {
            ((IDiagramModelConnection)this.component).setSource(source);
            source.getSourceConnections().add((IDiagramModelConnection)this.component);
        }
    }
    
    // View connection target
    public IConnectable getTargetConnection() {
        if ( this.component instanceof IDiagramModelConnection )
            return ((IDiagramModelConnection)this.component).getTarget();
        return null;
    }
    
    public void setTargetConnection(IConnectable target) {
        if ( this.component instanceof IDiagramModelConnection && (target != null) && (target != this.component) && (target != ((IDiagramModelConnection)this.component).getTarget()) ) {
            ((IDiagramModelConnection)this.component).setTarget(target);
            target.getTargetConnections().add((IDiagramModelConnection)this.component);
        }
    }
    
    // alpha (transparency) from Archi 4.3
    public Integer getAlpha() {
    	if ( (this.component instanceof IDiagramModelObject) || (this.component instanceof ICanvasModelSticky) || (this.component instanceof ICanvasModelBlock)) {
	    	try {
	    		return ((IDiagramModelObject)this.component).getAlpha();
	    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
	    		// prior to Archi 4.3, getAlpha() does not exist
	    	}
	    	return 255;
    	}
    	return null;
    }
    
    public void setAlpha(Integer alpha) {
    	if ( ((this.component instanceof IDiagramModelObject) || (this.component instanceof ICanvasModelSticky) || (this.component instanceof ICanvasModelBlock)) && (alpha != null) && (alpha.intValue() != getAlpha().intValue()) ) {
	    	try {
	    		((IDiagramModelObject)this.component).setAlpha(alpha.intValue());
	    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
	    		// prior to Archi 4.3, getAlpha() does not exist
	    	}
    	}
    }
    
    public Boolean isDirected() {
    	if ( this.component instanceof IAssociationRelationship )
    		return ((IAssociationRelationship)this.component).isDirected();
    	return null;
    }
    public Integer isDirectedAsInteger() {
    	if ( this.component instanceof IAssociationRelationship )
    		return ((IAssociationRelationship)this.component).isDirected() ? 1 : 0;
    	return null;
    }
    
    public void setDirected(boolean directed) {
    	if ( this.component instanceof IAssociationRelationship )
    		((IAssociationRelationship)this.component).setDirected(directed);
    }
    
    public void setDirected(Object directed) {
    	if ( this.component instanceof IAssociationRelationship )
    		((IAssociationRelationship)this.component).setDirected(DBPlugin.getBooleanValue(directed));
    }
    
    public EList<IProperty> getProperties() {
    	if ( this.component instanceof IProperties )
    		return ((IProperties)this.component).getProperties();
    	return null;
    }
    
    public int getNumberOfProperties() {
    	if ( this.component instanceof IProperties )
    		return ((IProperties)this.component).getProperties().size();
    	return 0;
    }
    
    public EList<IFeature> getFeatures() {
    	if ( this.component instanceof IFeatures )
    		return ((IFeatures)this.component).getFeatures();
    	return null;
    }
    
    public int getNumberOfFeatures() {
    	if ( this.component instanceof IFeatures )
    		return ((IFeatures)this.component).getFeatures().size();
    	return 0;
    }
    
    // specializations, from Archi 4.9
    public EList<IProfile> getProfiles() {
    	try {
    		if ( this.component instanceof IProfiles )
    			return ((IProfiles)this.component).getProfiles();
    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
    		// prior to Archi 4.9, getProfiles() does not exist
    	}
    	return null;
    }
    
    public IProfile getPrimaryProfile() {
    	try {
    		if ( this.component instanceof IProfiles )
    			return ((IProfiles)this.component).getPrimaryProfile();
    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
    		// prior to Archi 4.9, getPrimaryProfile() does not exist
    	}
    	return null;
    }
    
    public String getPrimaryProfileID() {
    	try {
    		if ( this.component instanceof IProfiles ) {
    			IProfile profile = ((IProfiles)this.component).getPrimaryProfile();
    			if ( profile != null )
    				return profile.getId();
    		}
    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
    		// prior to Archi 4.9, getPrimaryProfile() does not exist
    	}
    	return null;
    }
    
    public void addProfileId(String profileId) {
    	if ( profileId == null )
    		return;
    	
    	try {
    		if ( this.component instanceof IProfiles ) {
    			DBArchimateModel model = DBArchimateModel.getDBArchimateModel(this.component);
    			if ( model == null )
    				return;
    			EList<IProfile> profiles = ((IProfiles)this.component).getProfiles();
    			for ( IProfile profile: model.getProfiles() ) {
    				if ( profile.getId().equals(profileId) ) {
    					profiles.add(profile);
    					return;
    				}
    			}
    		}
    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
    		// prior to Archi 4.9, getPrimaryProfile() does not exist
    	}
    	return;
    }
    
    public int getNumberOfProfiles() {
    	try {
    		if ( this.component instanceof IProfiles )
    			return ((IProfiles)this.component).getProfiles().size();
    	} catch (@SuppressWarnings("unused") NoSuchMethodError ign) {
    		// prior to Archi 4.9, getProfiles() does not exist
    	}
    	return 0;
    }
    
    public String getConceptType() {
    	if ( this.component instanceof IProfile )
    		return ((IProfile)this.component).getConceptType();
    	return null;
    }
    
    public void setConceptType(String conceptType) {
    	if ( this.component instanceof IProfile )
    		((IProfile)this.component).setConceptType(conceptType);
    }
    
	/**
	 * Gets the DBMetadata associated with an archimate object.
	 * @param obj the archimate object
	 * @return the DBMetadata class associated with the archimate object
	 */
	public static DBMetadata getDBMetadata(EObject obj) {
		DBArchimateModel model = DBArchimateModel.getDBArchimateModel(obj);
		if ( model != null )
			return model.getDBMetadata(obj);	
		return null;
	}
}
