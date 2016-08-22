/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.canvas.model.INotesContent;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFolderContainer;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.IRelationship;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ISketchModelSticky;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

public class DBObject {
	private DBModel dbModel;
	private EObject object;

	public DBObject(DBModel _dbModel, EObject _object) {
		dbModel = _dbModel;
		object = _object;
	}
	public EObject getEObject() {
		return object;
	}
	public DBModel getDBModel() {
		return dbModel;
	}
	public IArchimateModel getModel() {
		return dbModel.getModel();
	}
	public String setId(String _id) {
		try { 
			((IIdentifier)object).setId(_id);
			return _id;
		} catch (ClassCastException e) {}
		return null;
	}
	public void setId(String _id, String _modelId, String _version) { 
		((IIdentifier)object).setId(DBPlugin.generateId(_id, _modelId, _version));
	}
	public String getId() {
		return DBPlugin.getId(((IIdentifier)object).getId());
	}
	public String getProjectId() {
		return DBPlugin.getProjectId(((IIdentifier)object).getId());
	}
	public String getVersion() {
		return DBPlugin.getVersion(((IIdentifier)object).getId());
	}
	public Boolean isVersionned() {
		return DBPlugin.isVersionned(((IIdentifier)object).getId());
	}
	public String getName() {
		return ((INameable)object).getName();
	}
	public void setName(String _name) {
		if ( _name != null ) ((INameable)object).setName(_name);
	}
	public String getDocumentation() {
		try {
			return ((IDocumentable)object).getDocumentation();
		} catch (ClassCastException e) {}
		return null;
	}
	public void setDocumentation(String _documentation) {
		try {
			if ( _documentation != null ) ((IDocumentable)object).setDocumentation(_documentation);
		} catch (ClassCastException e) {}
	}
	public EList<IProperty> getProperties() {
		try {
			return ((IProperties)object).getProperties();
		} catch (ClassCastException e) {}
		return null;
	}
	public String getSourceId() {
		try {
			return DBPlugin.getId(((IRelationship)object).getSource().getId());
		} catch (ClassCastException e) {}
		return DBPlugin.getId(((IDiagramModelConnection)object).getSource().getId());
	}
	public void setSource(String _source) {
		try {
			((IRelationship)object).setSource((IArchimateElement)dbModel.searchEObjectById(DBPlugin.generateId(_source, getProjectId(), getVersion())));
			return;
		} catch (ClassCastException e) {}
		((IDiagramModelConnection)object).setSource((IDiagramModelObject)dbModel.searchEObjectById(DBPlugin.generateId(_source, getProjectId(), getVersion())));
	}
	public String getTargetId() {
		try {
			return (new DBObject(dbModel, ((IRelationship)object).getTarget())).getId();
		} catch (ClassCastException e) {}
		return (new DBObject(dbModel, ((IDiagramModelConnection)object).getTarget())).getId();
	}
	public void setTarget(String _target) {
		try {
			((IRelationship)object).setTarget((IArchimateElement)dbModel.searchEObjectById(DBPlugin.generateId(_target, getProjectId(), getVersion())));
			return;
		} catch (ClassCastException e) {}
		((IDiagramModelConnection)object).setTarget((IDiagramModelObject)dbModel.searchEObjectById(DBPlugin.generateId(_target, getProjectId(), getVersion())));
	}
	public String getRelationshipId() {
		try {
			return (new DBObject(dbModel, ((IDiagramModelArchimateConnection)object).getRelationship())).getId();
		} catch (ClassCastException e) {}
		return null;
	}
	public void setRelationship(String _relationship) {
		((IDiagramModelArchimateConnection)object).setRelationship((IRelationship)dbModel.searchEObjectById(DBPlugin.generateId(_relationship, getProjectId(), getVersion())));
	}
	public int getBackground() {
		return ((ISketchModel)object).getBackground();
	}
	public void setBackground(int _background) {
		((ISketchModel)object).setBackground(_background);
	}
	public int getChildrenSize() {
		try {
			return ((IDiagramModelContainer)object).getChildren().size();
		} catch (Exception e) {}
		return 0;
	}
	public EList<IDiagramModelObject> getChildren() {
		try {
			return ((IDiagramModelContainer)object).getChildren();
		} catch (ClassCastException e) {}
		return null;
	}
	public DBObject getChild(int i) {
		return new DBObject(dbModel, ((IDiagramModelContainer)object).getChildren().get(i));
	}
	public void addChild(DBObject _dbObject) {
		try {
			((IDiagramModelContainer)object).getChildren().add((IDiagramModelObject)_dbObject.getEObject());
			return;
		} catch (ClassCastException e) {}
		((ISketchModelSticky)object).getChildren().add((IDiagramModelObject)_dbObject.getEObject());
	}
	public String getEClassName() {
		return object.eClass().getName();
	}
	public String getClassSimpleName() {
		return object.getClass().getSimpleName();
	}
	public EList<IDiagramModelConnection> getTargetConnections() {
		return ((IDiagramModelObject)object).getTargetConnections();
	}
	public String getTargetConnectionsString() {
		EList<IDiagramModelConnection> connections = null;

		try {
			connections = ((IDiagramModelObject)object).getTargetConnections();
		} catch (ClassCastException e) {}
		if ( connections == null )
			connections = ((IDiagramModelGroup)object).getTargetConnections();

		String target=null;
		for ( IDiagramModelConnection connection: connections ) {
			String id = (new DBObject(dbModel, connection)).getId();
			target = (target == null) ?  id : target+","+id;
		}
		return target;
	}
	public String getFont() {
		return ((IFontAttribute)object).getFont();
	}
	public void setFont(String _font) {
		((IFontAttribute)object).setFont(_font);
	}
	public String getFontColor() {
		return ((IFontAttribute)object).getFontColor();
	}
	public void setFontColor(String _fontColor) {
		((IFontAttribute)object).setFontColor(_fontColor);
	}
	public int getLineWidth() {
		return ((ILineObject)object).getLineWidth();
	}
	public void setLineWidth(int _lineWidth) {
		((ILineObject)object).setLineWidth(_lineWidth);
	}
	public String getLineColor() {
		return ((ILineObject)object).getLineColor();
	}
	public void setLineColor(String _lineColor) {
		((ILineObject)object).setLineColor(_lineColor);
	}
	public int getTextAlignment() {
		return ((ITextAlignment)object).getTextAlignment();
	}
	public void setTextAlignment(int _textAlignment) {
		((ITextAlignment)object).setTextAlignment(_textAlignment);
	}
	public IBounds getBounds() {
		return ((IDiagramModelObject)object).getBounds();
	}
	public void setBounds(int _x, int _y, int _width, int _height) {
		((IDiagramModelObject)object).setBounds(_x, _y, _width, _height);
	}
	public EList<IDiagramModelConnection> getSourceConnections() {
		return ((IDiagramModelObject)object).getSourceConnections();
	}
	public DBObject getSourceConnection(int i) {
		return new DBObject (dbModel, ((IDiagramModelObject)object).getSourceConnections().get(i));
	}
	public String getFillColor() {
		return ((IDiagramModelObject)object).getFillColor();
	}
	public void setFillColor(String _fillColor) {
		((IDiagramModelObject)object).setFillColor(_fillColor);
	}
	public int getType() {
		try {
			return ((IDiagramModelConnection)object).getType();
		} catch (ClassCastException e) {}
		try {
			return ((IDiagramModelArchimateObject)object).getType();
		} catch (ClassCastException e) {}
		return -1;
	}
	public void setType(int _type) {
		try {
			((IDiagramModelConnection)object).setType(_type);
			return;
		} catch (ClassCastException e) {}
		try {
			((IDiagramModelArchimateObject)object).setType(_type);
			return ;
		} catch (ClassCastException e) {}
		return;
	}
	public DBObject getArchimateElement() {
		return new DBObject (dbModel, ((IDiagramModelArchimateObject)object).getArchimateElement());
	}
	public String getArchimateElementId() {
		try {
			return ((IDiagramModelArchimateObject)object).getArchimateElement().getId();
		} catch (ClassCastException e) {}
		return null;
	}
	public String getArchimateElementName() {
		try {
			return ((IDiagramModelArchimateObject)object).getArchimateElement().getName();
		} catch (ClassCastException e) {}
		return null;
	}
	public String getArchimateElementClass() {
		try {
			return ((IDiagramModelArchimateObject)object).getArchimateElement().getClass().getSimpleName();
		} catch (ClassCastException e) {}
		return null;
	}
	public void setArchimateElement(String _id, String _name, String _class) {
		if ( getProjectId().equals(DBPlugin.getProjectId(_id)) ) { 
			IArchimateElement child = (IArchimateElement)dbModel.searchEObjectById(_id);
			if ( child == null ) {
				DBPlugin.popup(Level.Error,"Unknown ArchimateElement " + _id);
			} else {
				((IDiagramModelArchimateObject)object).setArchimateElement(child);
			}
		} else {
			IArchimateElement child = (IArchimateElement)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(_class));
			((EObjectImpl)child).eSetProxyURI(URI.createURI(_id));
			child.setId(_id);
			child.setName(DBPlugin.getProjectId(_id)+":"+_name);
			child.setDocumentation("Load model ID '"+DBPlugin.getProjectId(_id)+"' to import element.");
			DBPlugin.popup(Level.Error,"   setting proxy to ArchimateElement "  + child.getName() + " ("+child.getId()+")");
			((IDiagramModelArchimateObject)object).setArchimateElement(child);
			//we add it to the "external elements" folder
			//for ( IFolder f: dbModel.getFolders() ) {
			//	if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
			//		for ( IFolder ff: f.getFolders() ) {
			//			if ( ff.getName().equals(DBPlugin.ExternalFolderName) ) {
			//				ff.getElements().add(child);
			//				return;
			//			}
			//		}
			//	}
			//}
		}
	}
	@SuppressWarnings("deprecation")
	public String getText() {
		return ((IDiagramModelConnection)object).getText();
	}
	@SuppressWarnings("deprecation")
	public void setText(String _text) {
		((IDiagramModelConnection)object).setText(_text);
	}
	public EList<IDiagramModelBendpoint> getBendpoints() {
		return ((IDiagramModelConnection)object).getBendpoints();
	}
	public void setBendpoint(IDiagramModelBendpoint _bendpoint) {
		((IDiagramModelConnection)object).getBendpoints().add(_bendpoint);
	}
	public int getTextPosition() {
		try {
			return ((IDiagramModelConnection)object).getTextPosition();
		} catch (ClassCastException e) {}
		return ((ITextPosition)object).getTextPosition();
	}
	public void setTextPosition(int _textPosition) {
		try {
			((IDiagramModelConnection)object).setTextPosition(_textPosition);
			return;
		} catch (ClassCastException e) {}
		((ITextPosition)object).setTextPosition(_textPosition);
	}
	public int getConnectionRouterType() {
		//try {
			return ((IDiagramModel)object).getConnectionRouterType();
		//} catch (ClassCastException e) {}
		//try {
		//	return ((IArchimateDiagramModel)object).getConnectionRouterType();
		//} catch (ClassCastException e) {}
		//return ((ICanvasModel)object).getConnectionRouterType();
	}
	public void setConnectionRouterType(int _connectionRouterType) {
		//try {
			((IDiagramModel)object).setConnectionRouterType(_connectionRouterType);
		//	return;
		//} catch (ClassCastException e) {}
		//try {
		//	((IArchimateDiagramModel)object).setConnectionRouterType(_connectionRouterType);
		//	return;
		//} catch (ClassCastException e) {}
		//((ICanvasModel)object).setConnectionRouterType(_connectionRouterType);
	}
	public int getViewpoint() {
		return ((IArchimateDiagramModel)object).getViewpoint();
	}
	public void setViewpoint(int _viewpoint) {
		((IArchimateDiagramModel)object).setViewpoint(_viewpoint);
	}
	public int getBorderType() {
		try {
			return ((IDiagramModelNote)object).getBorderType();
		} catch (ClassCastException e) {}
		return -1;
	}
	public void setBorderType(int _borderType) {
		try {
			((IDiagramModelNote)object).setBorderType(_borderType);
		} catch (ClassCastException e) {}
	}
	public String getContent() {
		try {
			return ((ITextContent)object).getContent();
		} catch (ClassCastException e) {}
		return null;
	}
	public void setContent(String _content) {
		try {
			((ITextContent)object).setContent(_content);
		} catch (ClassCastException e) {}
	}
	public String getHintTitle() {
		return ((IHintProvider)object).getHintTitle();
	}
	public void setHintTitle(String _title) {
		((IHintProvider)object).setHintTitle(_title);
	}
	public String getHintContent() {
		return ((IHintProvider)object).getHintContent();
	}
	public void setHintContent(String _content) {
		((IHintProvider)object).setHintContent(_content);
	}
	public String getImagePath() {
		return ((IDiagramModelImageProvider)object).getImagePath();		
	}
	public void setImagePath(String _path) {
		((IDiagramModelImageProvider)object).setImagePath(_path);
	}
	public int getImagePosition() {
		return ((IIconic)object).getImagePosition();
	}
	public void setImagePosition(int _position) {
		((IIconic)object).setImagePosition(_position);
	}
	public boolean isLocked() {
		try {
			return ((ILockable)object).isLocked();
		} catch (ClassCastException e) {}
		return false;
	}
	public void setLocked(boolean _locked) {
		try {
			((ILockable)object).setLocked(_locked);
		} catch (ClassCastException e) {}
	}
	public String getBorderColor() {
		return ((IBorderObject)object).getBorderColor();
	}
	public void setBorderColor(String _color) {
		((IBorderObject)object).setBorderColor(_color);
	}
	public String getNotes() {
		return ((INotesContent)object).getNotes();
	}
	public void setNotes(String _notes) {
		((INotesContent)object).setNotes(_notes);
	}
	public EList<IFolder> getFolders() {
		return ((IFolderContainer)object).getFolders();
	}
	public String getFolder() {
		return DBPlugin.getId(((IIdentifier)object.eContainer()).getId());
	}
	public void setFolder(String _id) {
		IFolder folder = getFolderById(dbModel.getFolders(), _id);

		if ( folder != null ) {
			if ( object instanceof IFolder ) {
				folder.getFolders().add((IFolder)object);
			} else {
				folder.getElements().add(object);
			}
		} else {
			//TODO: should exist !!! create it
			DBPlugin.debug("      setFolder : unknown folder id " + _id);;
		}
	}
	
	public int getFolderType(int _rank) {
		try {
			IFolder f = (IFolder)object;
			// if _rank == 0 and the getType()==0, then we are in shared mode and need to retrieve the type of the parent folder
			if ( (_rank == 0) && (f.getType().getValue() == 0) )
				f = (IFolder)f.eContainer();
			return f.getType().getValue();
		} catch (ClassCastException e) {}
		return 0;
	}
	
	public String getFolderName(int _rank) {
		try {
			IFolder f = (IFolder)object;
			// if _rank == 0 and the getType()==0, then we are in shared mode and need to retrieve the name of the parent folder
			if ( (_rank == 0) && (f.getType().getValue() == 0) )
				f = (IFolder)f.eContainer();
			return f.getName();
		} catch (ClassCastException e) {}
		return null;
	}
	
	public IFolder getFolderById(EList<IFolder> _folders, String _id) {
		if ( _folders == null ) return null;
		for ( IFolder f: _folders ) {
			if ( DBPlugin.getId(f.getId()).equals(_id) ) {
				return f;
			}
			IFolder folder = getFolderById(f.getFolders(), _id);
			if ( folder != null ) {
				return folder;
			}
		}
		return null;		
	}
}