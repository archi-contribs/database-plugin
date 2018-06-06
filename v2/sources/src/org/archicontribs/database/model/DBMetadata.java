/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import java.util.ArrayList;
import java.util.List;

import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.data.DBVersion;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
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

        if ( model.isTheLatestModelIntheDatabase() ) {
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
     * @return the list of views objects that reference the component
     */
    public List<IConnectable> componentToConnectable(IArchimateDiagramModel view) {
        List<IConnectable> connectables = new ArrayList<IConnectable>();

        if ( (this.component instanceof IArchimateConcept) && (view != null) ) {
            for ( IDiagramModelObject viewObject: view.getChildren() ) {
                connectables.addAll(toConnectable((IDiagramModelArchimateComponent)viewObject, (IArchimateConcept)this.component));
            }
        }
        return connectables;
    }

    /**
     * @return the list of views objects that reference the component
     * @param view view in which the concept should be searched in
     * @param concept Archimate concept to search in the view
     */
    public List<IConnectable> componentToConnectable(IArchimateDiagramModel view, IArchimateConcept concept) {
        List<IConnectable> connectables = new ArrayList<IConnectable>();

        if ( (this.component instanceof IArchimateConcept) && (view != null) ) {
            for ( IDiagramModelObject viewObject: view.getChildren() ) {
                connectables.addAll(toConnectable((IDiagramModelArchimateComponent)viewObject, concept));
            }
        }
        return connectables;
    }

    /**
     * @return the list of views objects that reference the component
     * @param parentComponent View object in which the concept should be searched in
     * @param concept Archimate concept to search in the view object
     */
    private List<IConnectable> toConnectable(IDiagramModelArchimateComponent parentComponent, IArchimateConcept concept) {
        List<IConnectable> connectables = new ArrayList<IConnectable>();

        if ( concept instanceof IArchimateElement ) {
            if ( DBPlugin.areEqual(parentComponent.getArchimateConcept().getId(), concept.getId()) ) connectables.add(parentComponent);
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
                connectables.addAll(toConnectable((IDiagramModelArchimateComponent)child, concept));
            }
        }
        return connectables;
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

    public void setType(FolderType type) {
        if ( (this.component instanceof IFolder) && (type != null) && (((IFolder)this.component).getType().getValue() != type.getValue()) )
            ((IFolder)this.component).setType(type);
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
    public Integer setBorderType() {
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
            ((ITextContent)this.component).getContent();
        return null;
    }
    public void setContent(String content) {
        if ( (this.component instanceof ITextContent) && (content != null) && !DBPlugin.areEqual(((ITextContent)this.component).getContent(), content) ) 
            ((ITextContent)this.component).setContent(content);
    }

    // Documentation
    public String getDocumentation() {
        if ( this.component instanceof IDocumentable )  
            return ((IDocumentable)this.component).getDocumentation();
        return null;
    }

    public void setDocumentation(String documentation) {
        if ( (this.component instanceof IDocumentable) && (documentation != null) && !DBPlugin.areEqual(((IDocumentable)this.component).getDocumentation(), documentation) )  
            ((IDocumentable)this.component).setDocumentation(documentation);
    }

    // Name
    public String getName() {
        if ( this.component instanceof INameable )
            ((INameable)this.component).getName();
        return null;
    }

    public void setName(String name) {
        if ( (this.component instanceof INameable) && (name != null) && !DBPlugin.areEqual(((INameable)this.component).getName(), name) )
            ((INameable)this.component).setName(name);
    }

    // HintContent
    public String getHintContent() {
        if ( this.component instanceof IHintProvider )   
            return ((IHintProvider)this.component).getHintContent();
        return null;
    }


    public void setHintContent(String hintContent) {
        if ( (this.component instanceof IHintProvider) && (hintContent != null) && !DBPlugin.areEqual(((IHintProvider)this.component).getHintContent(), hintContent) )   
            ((IHintProvider)this.component).setHintContent(hintContent);
    }

    // HintTitle
    public String getHintTitle() {
        if ( this.component instanceof IHintProvider )  
            return ((IHintProvider)this.component).getHintTitle();
        return null;
    }

    public void setHintTitle(String hintTitle) {
        if ( (this.component instanceof IHintProvider) && (hintTitle != null) && !DBPlugin.areEqual(((IHintProvider)this.component).getHintTitle(), hintTitle) )  
            ((IHintProvider)this.component).setHintTitle(hintTitle);
    }

    // Locked
    public Boolean isLocked() {
        if ( this.component instanceof ILockable )
            return ((ILockable)this.component).isLocked();
        return null;
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

    // ImagePath
    public String getImagePath() {   
        if ( this.component instanceof IDiagramModelImageProvider )  
            return ((IDiagramModelImageProvider)this.component).getImagePath();
        return null;
    }

    public void setImagePath(String imagePath) {   
        if ( (this.component instanceof IDiagramModelImageProvider) && (imagePath != null) && !DBPlugin.areEqual(((IDiagramModelImageProvider)this.component).getImagePath(), imagePath) )  
            ((IDiagramModelImageProvider)this.component).setImagePath(imagePath);
    }

    // ImagePosition
    public Integer getImagePosition() {
        if ( this.component instanceof IIconic ) 
            return ((IIconic)this.component).getImagePosition();
        return null;
    }

    public void setImagePosition(Integer imagePosition) {
        if ( this.component instanceof IIconic && (imagePosition != null) && (((IIconic)this.component).getImagePosition() != imagePosition) ) 
            ((IIconic)this.component).setImagePosition(imagePosition);
    }

    // LineColor
    public String getLineColor() {   
        if ( this.component instanceof ILineObject )  
            ((ILineObject)this.component).getLineColor();
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
        return null;
    }

    public void setTextPosition(Integer textPosition) {
        if ( (this.component instanceof ITextPosition) && (textPosition != null) && ((ITextPosition)this.component).getTextPosition() != textPosition.intValue() ) 
            ((ITextPosition)this.component).setTextPosition(textPosition);
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
                oldParentFolder.getFolders().remove(this.component);
            else
                oldParentFolder.getElements().remove(this.component);
        }
        
        if ( this.component instanceof IFolder )
            newParentFolder.getFolders().add((IFolder)this.component);
        else
            newParentFolder.getElements().add(this.component);
    }
    
    // Relationship Source
    public IArchimateConcept getSource() {
        if ( this.component instanceof IArchimateRelationship )
            return ((IArchimateRelationship)this.component).getSource();
        return null;
    }
    
    public void setSource(IArchimateConcept source) {
        if ( this.component instanceof IArchimateRelationship && (source != null) && (source != this.component) && (source != ((IArchimateRelationship)this.component).getSource()) ) {
            ((IArchimateRelationship)this.component).setSource(source);
            source.getSourceRelationships().add((IArchimateRelationship)this.component);
        }
    }
    
    // Relationship Target
    public IArchimateConcept getTarget() {
        if ( this.component instanceof IArchimateRelationship )
            return ((IArchimateRelationship)this.component).getTarget();
        return null;
    }
    
    public void setTarget(IArchimateConcept target) {
        if ( this.component instanceof IArchimateRelationship && (target != null) && (target != this.component) && (target != ((IArchimateRelationship)this.component).getTarget()) ) {
            ((IArchimateRelationship)this.component).setTarget(target);
            target.getTargetRelationships().add((IArchimateRelationship)this.component);
        }
    }
}
