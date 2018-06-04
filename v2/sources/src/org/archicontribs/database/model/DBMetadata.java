/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBVersion;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
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
public class DBMetadata  {
    /**
     * Component that contains the DBMetadata<br>
     * This property is set during the component initialization and is used to calculate the component checksum
     */
    private EObject component;
    
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
	
	/**
	 * Used by remember is the component has been exported
	 */
	@Getter @Setter private boolean hasBeenExported = false;
	
	/**
	 * Used by views, set to false if some components are removed during the export process so their checksum needs to be recalculated
	 */
	@Getter @Setter private boolean checksumValid = true;
	
	/**
	 * If the component is a folder, stores the type of its root folder<br>
	 * This is needed to determine what class of eObjects it is able to store<br>
	 * Folders created by the user have got a type of 0 (zero) but the root folder they are in limit the kind of objects they can store   
	 */
	@Getter @Setter private int rootFolderType;
	
	public DBMetadata(EObject component) {
		assert ( component instanceof IIdentifier );
		this.component = component;
	}
	
	/**
	 * Choices available when a conflict is detected in the database<br>
	 * <li><b>askUser</b> Ask the user what he wishes to do</li>
	 * <li><b>doNotExport</b> Do not export to the database</li>
	 * <li><b>exportToDatabase</b> Export to the database</li>
	 * <li><b>importFromDatabase</b> Replace the component with the version in the database</li>
	 */
	public enum CONFLICT_CHOICE {askUser, doNotExport, exportToDatabase, importFromDatabase}
	/**
	 * Stores the action that need to be done in case of a database conflict
	 * @see CONFLICT_CHOICE
	 */
	@Getter @Setter private CONFLICT_CHOICE conflictChoice = CONFLICT_CHOICE.askUser;
	
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
    public enum DATABASE_STATUS {isSynced, isNewInModel, isUpadtedInDatabase, isUpdatedInModel, isDeletedInDatabase, IsConflicting}
    
    /**
     * Gets the status of the component<br>
     * @see COMPONENT_STATUS
     */
    public DATABASE_STATUS getDatabaseStatus() {
    	DBArchimateModel model;
    	
    	if ( this.component instanceof IArchimateModelObject )
    		model = (DBArchimateModel) ((IArchimateModelObject)this.component).getArchimateModel();
    	else
    		model = (DBArchimateModel) ((IDiagramModelComponent)this.component).getDiagramModel().getArchimateModel();
    	
        // if the initialVersion is zero, it means that the component does not exist in the database version of the model
    	// therefore, it couldn't be imported
        // thus it is a new component in the model
        if ( this.initialVersion.getVersion() == 0 )
            return DATABASE_STATUS.isNewInModel;
        
        // The tests are not the same if there is a new version of the model in the database or not ...
        
        if ( (model.getCurrentVersion().getVersion() - model.getInitialVersion().getVersion()) == 1 ) {
        	// if nobody created a new version of the database since it has been imported, then only the initialChecksum and currentChecksum matter
        	
        	// if the checksum are identical, it means that the component has not been updated since it has been imported
        	if ( DBPlugin.areEqual(this.currentVersion.getChecksum(), this.initialVersion.getChecksum()) )
                return DATABASE_STATUS.isSynced;
        	
        	// else, it means that it has been updated since it has been imported
        	return DATABASE_STATUS.isUpdatedInModel;
        }
        
        // if someone created a new version of the model in the database since we imported it, then we need to determine its status compared to the database
        	
        // if the databaseVersion is zero, then it means that the component did not exist in the latest version of the model anymore
        if ( this.databaseVersion.getVersion() == 0 )
        	return DATABASE_STATUS.isDeletedInDatabase;

        // if the currentChecksum equals the latestDatabasechecksum, this means that the component is synced between the model and the database
        if ( DBPlugin.areEqual(this.currentVersion.getChecksum(), this.latestDatabaseVersion.getChecksum()) )
            return DATABASE_STATUS.isSynced;
        
        String initialChecksum = this.initialVersion.getChecksum();
        String currentChecksum = this.currentVersion.getChecksum();
        String databaseChecksum= this.databaseVersion.getChecksum();
        
        // if the components checksum in the model has been modified since the component has been imported
        // this means that the component has been updated in the model
        boolean modifiedInModel = !DBPlugin.areEqual(initialChecksum, currentChecksum);
        
        // if the components checksum in the database has been modified since the component has been imported
        // this means that the component has been updated in the database 
        boolean modifiedInDatabase = !DBPlugin.areEqual(initialChecksum, databaseChecksum);
            
        // if both versions of the component (in the model and in the database) have been updated
        // then they are conflicting ...
        // ... except if the modifications are the same
        if ( modifiedInModel && modifiedInDatabase ) {
            if ( DBPlugin.areEqual(currentChecksum, databaseChecksum) )
                return DATABASE_STATUS.isSynced;

            return DATABASE_STATUS.IsConflicting;
        }
        
        // if we're here, it means that the component has been updated either in the model, either in the database
        if ( modifiedInModel )
            return DATABASE_STATUS.isUpdatedInModel;
        
        if ( modifiedInDatabase )
        	return DATABASE_STATUS.isUpadtedInDatabase;
        
        return DATABASE_STATUS.isSynced;
    }
    
	
	/**
	 * Calculates the full name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\""
	 */
	public String getFullName() {
		return new StringBuilder(this.component.getClass().getSimpleName()).append(":\""+((INameable)this.component).getName()+"\"").toString();
	}
	
	/**
	 * Calculates the debug name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\"("+getId()+")"
	 */
	public String getDebugName() {
		return new StringBuilder(getFullName()).append("("+((IIdentifier)this.component).getId()+")").toString();
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

	// ReferencedModel
    public IDiagramModel getReferencedModel() {
        if ( this.component instanceof IDiagramModelReference )
            return ((IDiagramModelReference)this.component).getReferencedModel();
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
        else if ( this.component instanceof IDiagramModelArchimateConnection )
            return ((IDiagramModelArchimateConnection)this.component).getType();
        else if ( this.component instanceof IDiagramModelConnection )
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
    public String getTypeString() {
        if ( this.component instanceof IJunction )
            return ((IJunction)this.component).getType();
        return null;
    }
    
    public void setType(String type) {
        if ( (this.component instanceof IJunction) && (type != null) && !DBPlugin.areEqual(((IJunction)this.component).getType(), type) )
            ((IJunction)this.component).setType(type);
    }

    xxxxxxxxxxxxxxx
    public void setBorderColor(String borderColor) {
        if ( (this.component instanceof IBorderObject) && (borderColor != null) && !DBPlugin.areEqual(((IBorderObject)this.component).getBorderColor(), borderColor) )
            ((IBorderObject)this.component).setBorderColor(borderColor);
    }

    public void setBorderType(Integer borderType) {
        if ( (this.component instanceof IDiagramModelNote) && (borderType != null) && ((IDiagramModelNote)this.component).getBorderType() != borderType.intValue() ) 
            ((IDiagramModelNote)this.component).setBorderType(borderType);
    }

    public void setContent(String content) {
        if ( (this.component instanceof ITextContent) && (content != null) && !DBPlugin.areEqual(((ITextContent)this.component).getContent(), content) ) 
            ((ITextContent)this.component).setContent(content);
    }

    public void setDocumentation(String documentation) {
        if ( (this.component instanceof IDocumentable) && (documentation != null) && !DBPlugin.areEqual(((IDocumentable)this.component).getDocumentation(), documentation) )  
            ((IDocumentable)this.component).setDocumentation(documentation);
    }

    public void setName(String name) {
        if ( (this.component instanceof INameable) && (name != null) && !DBPlugin.areEqual(((INameable)this.component).getName(), name) )
            ((INameable)this.component).setName(name);
    }

    public void setHintContent(String hintContent) {
        if ( (this.component instanceof IHintProvider) && (hintContent != null) && !DBPlugin.areEqual(((IHintProvider)this.component).getHintContent(), hintContent) )   
            ((IHintProvider)this.component).setHintContent(hintContent);
    }

    public void setHintTitle(String hintTitle) {
        if ( (this.component instanceof IHintProvider) && (hintTitle != null) && !DBPlugin.areEqual(((IHintProvider)this.component).getHintTitle(), hintTitle) )  
            ((IHintProvider)this.component).setHintTitle(hintTitle);
    }

    public void setLocked(Object isLocked) {
        if ( (this.component instanceof ILockable) && (isLocked !=null) ) {
            Boolean mustBeLocked = null;
            if ( isLocked instanceof Boolean )
                mustBeLocked = (Boolean)isLocked;
            else if ( isLocked instanceof Integer)
                mustBeLocked = (Integer)isLocked!=0;
            else if ( isLocked instanceof String)
                mustBeLocked = Integer.valueOf((String)isLocked)!=0;

            if ( mustBeLocked != null && ((ILockable)this.component).isLocked() != mustBeLocked )
                ((ILockable)this.component).setLocked(mustBeLocked);
        }
    }

    public void setImagePath(String imagePath) {   
        if ( (this.component instanceof IDiagramModelImageProvider) && (imagePath != null) && !DBPlugin.areEqual(((IDiagramModelImageProvider)this.component).getImagePath(), imagePath) )  
            ((IDiagramModelImageProvider)this.component).setImagePath(imagePath);
    }

    public void setImagePosition(Integer imagePosition) {
        if ( (this.component instanceof IIconic) && (imagePosition != null) && ((IIconic)this.component).getImagePosition() != imagePosition.intValue() ) 
            ((IIconic)this.component).setImagePosition(imagePosition);
    }

    public void setLineColor(String lineColor) {   
        if ( (this.component instanceof ILineObject) && (lineColor != null) && !DBPlugin.areEqual(((ILineObject)this.component).getLineColor(), lineColor) )  
            ((ILineObject)this.component).setLineColor(lineColor);
    }

    public void setFillColor(String fillColor) {   
        if ( (this.component instanceof IDiagramModelObject) && (fillColor != null) && !DBPlugin.areEqual(((IDiagramModelObject)this.component).getFillColor(), fillColor) )  
            ((IDiagramModelObject)this.component).setFillColor(fillColor);
    }

    public void setLineWidth(Integer lineWidth) {
        if ( (this.component instanceof ILineObject) && (lineWidth != null) && ((ILineObject)this.component).getLineWidth() != lineWidth.intValue() ) 
            ((ILineObject)this.component).setLineWidth(lineWidth);
    }

    public void setFont(String font) { 
        if ( (this.component instanceof IFontAttribute) && (font != null) && !DBPlugin.areEqual(((IFontAttribute)this.component).getFont(), font) )  
            ((IFontAttribute)this.component).setFont(font);
    }

    public void setFontColor(String fontColor) {   
        if ( (this.component instanceof IFontAttribute) && (fontColor != null) && !DBPlugin.areEqual(((IFontAttribute)this.component).getFontColor(), fontColor) )  
            ((IFontAttribute)this.component).setFontColor(fontColor);
    }

    public void setNotes(String notes) {   
        if ( (this.component instanceof ICanvasModelSticky) && (notes != null) && !DBPlugin.areEqual(((ICanvasModelSticky)this.component).getNotes(), notes) )  
            ((ICanvasModelSticky)this.component).setNotes(notes);
    }

    public void setTextAlignment(Integer textAlignment) {
        if ( (this.component instanceof ITextAlignment) && (textAlignment != null) && ((ITextAlignment)this.component).getTextAlignment() != textAlignment.intValue() ) 
            ((ITextAlignment)this.component).setTextAlignment(textAlignment);
    }

    public void setTextPosition(Integer textPosition) {
        if ( (this.component instanceof ITextPosition) && (textPosition != null) && ((ITextPosition)this.component).getTextPosition() != textPosition.intValue() ) 
            ((ITextPosition)this.component).setTextPosition(textPosition);
    }

    public void setBounds(Integer x, Integer y, Integer width, Integer height) {
        if ( eObject instanceof IDiagramModelObject && (x != null) && (y != null) && (width != null) && (height != null) ) {
            IBounds bounds = ((IDiagramModelObject)this.component).getBounds();
            if ( (bounds == null) || (bounds.getX() != x.intValue()) || (bounds.getY() != y.intValue()) || (bounds.getWidth() != width.intValue()) || (bounds.getHeight() != height.intValue()) )
                ((IDiagramModelObject)this.component).setBounds(x, y, width, height);
        }
    }

    public void setStrength(String strength) { 
        if ( (this.component instanceof IInfluenceRelationship) && (strength != null) && !DBPlugin.areEqual(((IInfluenceRelationship)this.component).getStrength(), strength) )  
            ((IInfluenceRelationship)this.component).setStrength(strength);
    }

    public void setAccessType(Integer accessType) {
        if ( (this.component instanceof IAccessRelationship) && (accessType != null) && ((IAccessRelationship)this.component).getAccessType() != accessType.intValue() ) 
            ((IAccessRelationship)this.component).setAccessType(accessType);
    }

    public void setViewpoint(String viewpoint) {   
        if ( (this.component instanceof IArchimateDiagramModel) && (viewpoint != null) && !DBPlugin.areEqual(((IArchimateDiagramModel)this.component).getViewpoint(), viewpoint) )  
            ((IArchimateDiagramModel)this.component).setViewpoint(viewpoint);
    }

    public void setBackground(Integer background) {
        if ( (this.component instanceof ISketchModel) && (background != null) && ((ISketchModel)this.component).getBackground() != background.intValue() ) 
            ((ISketchModel)this.component).setBackground(background);
    }

    public void setConnectionRouterType(Integer routerType) {
        if ( (this.component instanceof IDiagramModel) && (routerType != null) && ((IDiagramModel)this.component).getConnectionRouterType() != routerType.intValue() ) 
            ((IDiagramModel)this.component).setConnectionRouterType(routerType);
    }
}
