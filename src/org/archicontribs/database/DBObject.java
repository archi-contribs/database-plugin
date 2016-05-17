package org.archicontribs.database;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.canvas.model.ICanvasModelBlock;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.canvas.model.INotesContent;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelGroup;
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
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.util.ArchimateModelUtils;

public class DBObject {
	private DBModel dbModel;
	private EObject object;

	public DBObject(DBModel _dbModel, EObject _object) {
		dbModel = _dbModel;
		object = _object;
	}
	public DBObject(DBModel _dbModel, String _id) {
		dbModel = _dbModel;
		object = ArchimateModelUtils.getObjectByID(_dbModel.getModel(),_id);
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
		} catch (Exception e) {}
		return null;
	}
	public void setId(String _id, String _modelId, String _version) {
		try {  
			((IIdentifier)object).setId(_id + DBPlugin.Separator + _modelId + DBPlugin.Separator + _version);
		} catch (Exception e) {}
	}
	public String getId() {
		try {
			if ( isVersionned() ) return ((IIdentifier)object).getId().split(DBPlugin.Separator)[0];
			return ((IIdentifier)object).getId();
		} catch (Exception e) {}
		return null;
	}
	public String getModelId() {
		try {
			if ( isVersionned() ) return ((IIdentifier)object).getId().split(DBPlugin.Separator)[1];
			return ((IIdentifier)object).getId();
		} catch (Exception e) {}
		return dbModel.getModelId();
	}
	public String getVersion() {
		try {
			if ( isVersionned() ) return ((IIdentifier)object).getId().split(DBPlugin.Separator)[2];
		} catch (Exception e) {}
		return dbModel.getVersion();
	}
	public Boolean isVersionned() {
		try {
			return ((IIdentifier)object).getId().contains(DBPlugin.Separator);
		} catch (Exception e) {}
		return false;
	}
	public String getName() {
		try {
			return ((INameable)object).getName();
		} catch (Exception e) {}
		return null;
	}
	public void setName(String _name) {
		try {
			if ( _name != null ) ((INameable)object).setName(_name);
		} catch (Exception e) {}
	}
	public String getDocumentation() {
		try {
			return ((IDocumentable)object).getDocumentation();
		} catch (Exception e) {}
		return null;
	}
	public void setDocumentation(String _documentation) {
		try {
			if ( _documentation != null ) ((IDocumentable)object).setDocumentation(_documentation);
		} catch (Exception e) {}
	}
	public EList<IProperty> getProperties() {
		try {
			return ((IProperties)object).getProperties();
		} catch (Exception e) {}
		return null;
	}
	public String getSourceId() {
		try {
			return (new DBObject(dbModel, ((IRelationship)object).getSource())).getId();
		} catch (Exception e) {}
		try {
			return (new DBObject(dbModel, ((IDiagramModelConnection)object).getSource())).getId();
		} catch (Exception e) {}
		return null;
	}
	public void setSource(String _source) {
		try {
			((IRelationship)object).setSource((IArchimateElement)ArchimateModelUtils.getObjectByID(dbModel.getModel(), dbModel.generateId(_source)));
		} catch (Exception e) {}
		try {
			((IDiagramModelConnection)object).setSource((IDiagramModelObject)ArchimateModelUtils.getObjectByID(dbModel.getModel(), dbModel.generateId(_source)));
		} catch (Exception e) {}
	}
	public String getTargetId() {
		try {
			return (new DBObject(dbModel, ((IRelationship)object).getTarget())).getId();
		} catch (Exception e) {}
		try {
			return (new DBObject(dbModel, ((IDiagramModelConnection)object).getTarget())).getId();
		} catch (Exception e) {}
		return null;
	}
	public void setTarget(String _target) {
		try {
			((IRelationship)object).setTarget((IArchimateElement)ArchimateModelUtils.getObjectByID(dbModel.getModel(), dbModel.generateId(_target)));
		} catch (Exception e) {}
		try {
			((IDiagramModelConnection)object).setTarget((IDiagramModelObject)ArchimateModelUtils.getObjectByID(dbModel.getModel(), dbModel.generateId(_target)));
		} catch (Exception e) {}
	}
	public String getRelationshipId() {
		try {
			return (new DBObject(dbModel, ((IDiagramModelArchimateConnection)object).getRelationship())).getId();
		} catch (Exception e) {}
		return null;
	}
	public void setRelationship(String _relationship) {
		try {
			((IDiagramModelArchimateConnection)object).setRelationship((IRelationship) ArchimateModelUtils.getObjectByID(dbModel.getModel(), dbModel.generateId(_relationship)));
		} catch (Exception e) {}
	}
	public EList<IDiagramModelObject> getChildren() {
		try {
			return ((IDiagramModelContainer)object).getChildren();
		} catch (Exception e) {}
		return null;
	}
	public DBObject getChild(int i) {
		try {
			return new DBObject(dbModel, ((IDiagramModelContainer)object).getChildren().get(i));
		} catch (Exception e) {}
		return null;
	}
	public void addChild(DBObject _dbObject) {
		try {
			((IDiagramModelArchimateObject)object).getChildren().add((IDiagramModelObject)_dbObject.getEObject());
			return;
		} catch (Exception e) {}
		try {
			((IArchimateDiagramModel)object).getChildren().add((IDiagramModelObject)_dbObject.getEObject());
			return;
		} catch (Exception e) {}
		try {
			((IDiagramModelGroup)object).getChildren().add((IDiagramModelObject)_dbObject.getEObject());
			return;
		} catch (Exception e) {}
		try {
			((ICanvasModel)object).getChildren().add((ICanvasModelBlock)_dbObject.getEObject());
			return;
		} catch (Exception e) {}
		try {
			((ICanvasModel)object).getChildren().add((ICanvasModelSticky)_dbObject.getEObject());
			return;
		} catch (Exception e) {}
	}
	public String getEClassName() {
		try {
			return object.eClass().getName();
		} catch (Exception e) {}
		return null;
	}
	public String getClassSimpleName() {
		try {
			return object.getClass().getSimpleName();
		} catch (Exception e) {}
		return null;
	}
	public EList<IDiagramModelConnection> getTargetConnections() {
		try {
			return ((IDiagramModelObject)object).getTargetConnections();
		} catch (Exception e) {}
		return null;
	}
	public String getTargetConnectionsString() {
		try {
			String target=null;
			for ( IDiagramModelConnection connection: ((IDiagramModelArchimateObject)object).getTargetConnections() ) {
				String id = (new DBObject(dbModel, connection)).getId();
				target = (target == null) ?  id : target+","+id;
			}
			return target;
		} catch (Exception e) {}
		return null;
	}
	public String getFont() {
		try {
			return ((IFontAttribute)object).getFont();
		} catch (Exception e) {}
		return null;
	}
	public void setFont(String _font) {
		try {
			((IFontAttribute)object).setFont(_font);
		} catch (Exception e) {}
	}
	public String getFontColor() {
		try {
			return ((IFontAttribute)object).getFontColor();
		} catch (Exception e) {}
		return null;
	}
	public void setFontColor(String _fontColor) {
		try {
			((IFontAttribute)object).setFontColor(_fontColor);
		} catch (Exception e) {}
	}
	public int getLineWidth() {
		try {
			return ((ILineObject)object).getLineWidth();
		} catch (Exception e) {}
		return -1;
	}
	public void setLineWidth(int _lineWidth) {
		try {
			((ILineObject)object).setLineWidth(_lineWidth);
		} catch (Exception e) {}
	}
	public String getLineColor() {
		try {
			return ((ILineObject)object).getLineColor();
		} catch (Exception e) {}
		return null;
	}
	public void setLineColor(String _lineColor) {
		try {
			((ILineObject)object).setLineColor(_lineColor);
		} catch (Exception e) {}
	}
	public int getTextAlignment() {
		try {
			return ((ITextAlignment)object).getTextAlignment();
		} catch (Exception e) {}
		return -1;
	}
	public void setTextAlignment(int _textAlignment) {
		try {
			((ITextAlignment)object).setTextAlignment(_textAlignment);
		} catch (Exception e) {}
	}
	public IBounds getBounds() {
		try {
			return ((IDiagramModelObject)object).getBounds();
		} catch (Exception e) {}
		return null;
	}
	public void setBounds(int _x, int _y, int _width, int _height) {
		try {
			((IDiagramModelObject)object).setBounds(_x, _y, _width, _height);
		} catch (Exception e) {}
	}
	public EList<IDiagramModelConnection> getSourceConnections() {
		try {
			return ((IDiagramModelObject)object).getSourceConnections();
		} catch (Exception e) {}
		return null;
	}
	public DBObject getSourceConnection(int i) {
		try {
			return new DBObject (dbModel, ((IDiagramModelObject)object).getSourceConnections().get(i));
		} catch (Exception e) {}
		return null;
	}
	public String getFillColor() {
		try {
			return ((IDiagramModelObject)object).getFillColor();
		} catch (Exception e) {}
		return null;
	}
	public void setFillColor(String _fillColor) {
		try {
			((IDiagramModelObject)object).setFillColor(_fillColor);
		} catch (Exception e) {}
	}
	public int getType() {
		try {
			return ((IDiagramModelConnection)object).getType();
		} catch (Exception e) {}
		try {
			return ((IDiagramModelArchimateObject)object).getType();
		} catch (Exception e) {}
		return -1;
	}
	public void setType(int _type) {
		try {
			((IDiagramModelConnection)object).setType(_type);
			return;
		} catch (Exception e) {}
		try {
			((IDiagramModelArchimateObject)object).setType(_type);
			return;
		} catch (Exception e) {}
	}
	public DBObject getArchimateElement() {
		try {
			return new DBObject (dbModel, ((IDiagramModelArchimateObject)object).getArchimateElement());
		} catch (Exception e) {}
		return null;
	}
	public String getArchimateElementId() {
		try {
			return (new DBObject (dbModel, ((IDiagramModelArchimateObject)object).getArchimateElement())).getId();
		} catch (Exception e) {}
		return null;
	}
	public void setArchimateElement(EObject _eObject) {
		try {
			if (_eObject != null)((IDiagramModelArchimateObject)object).setArchimateElement((IArchimateElement)_eObject);
		} catch (Exception e) {}
	}
	@SuppressWarnings("deprecation")
	public String getText() {
		try {
			return ((IDiagramModelConnection)object).getText();
		} catch (Exception e) {}
		return null;
	}
	@SuppressWarnings("deprecation")
	public void setText(String _text) {
		try {
			((IDiagramModelConnection)object).setText(_text);
		} catch (Exception e) {}
	}
	public EList<IDiagramModelBendpoint> getBendpoints() {
		try {
			return ((IDiagramModelConnection)object).getBendpoints();
		} catch (Exception e) {}
		return null;
	}
	public void setBendpoint(int _startX, int _startY, int _endX, int _endY) {
		try {
			IDiagramModelBendpoint bp = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
			bp.setStartX(_startX);
			bp.setStartY(_startY);
			bp.setEndX(_endX);
			bp.setEndY(_endY);
			((IDiagramModelConnection)object).getBendpoints().add(bp);
		} catch (Exception e) {}
	}
	public int getTextPosition() {
		try {
			return ((IDiagramModelConnection)object).getTextPosition();
		} catch (Exception e) {}
		return -1;
	}
	public void setTextPosition(int _textPosition) {
		try {
			((IDiagramModelConnection)object).setTextPosition(_textPosition);
		} catch (Exception e) {}
	}
	public int getConnectionRouterType() {
		try {
			return ((IDiagramModel)object).getConnectionRouterType();
		} catch (Exception e) {}
		return -1;
	}
	public void setConnectionRouterType(int _connectionRouterType) {
		try {
			((IDiagramModel)object).setConnectionRouterType(_connectionRouterType);
		} catch (Exception e) {}
	}
	public int getViewpoint() {
		try {
			return ((IArchimateDiagramModel)object).getViewpoint();
		} catch (Exception e) {}
		return -1;
	}
	public void setViewpoint(int _viewpoint) {
		try {
			((IArchimateDiagramModel)object).setViewpoint(_viewpoint);
		} catch (Exception e) {}
	}
	public int getBorderType() {
		try {
			return ((IDiagramModelNote)object).getBorderType();
		} catch (Exception e) {}
		return -1;
	}
	public void setBorderType(int _borderType) {
		try {
			((IDiagramModelNote)object).setBorderType(_borderType);
		} catch (Exception e) {}
	}
	public String getContent() {
		try {
			return ((ITextContent)object).getContent();
		} catch (Exception e) {}
		return null;
	}
	public void setContent(String _content) {
		try {
			((ITextContent)object).setContent(_content);
		} catch (Exception e) {}
	}
	public String getHintTitle() {
		try {
			return ((IHintProvider)object).getHintTitle();
		} catch (Exception e) {}
		return null;
	}
	public void setHintTitle(String _title) {
		try {
			((IHintProvider)object).setHintTitle(_title);
		} catch (Exception e) {}
	}
	public String getHintContent() {
		try {
			return ((ICanvasModel)object).getHintContent();
		} catch (Exception e) {}
		return null;
	}
	public void setHintContent(String _content) {
		try {
			((ICanvasModel)object).setHintContent(_content);
		} catch (Exception e) {}
	}
	public String getImagePath() {
		try {
			return ((IIconic)object).getImagePath();
		} catch (Exception e) {}
		return null;
	}
	public void setImagePath(String _path) {
		try {
			((IIconic)object).setImagePath(_path);
		} catch (Exception e) {}
	}
	public int getImagePosition() {
		try {
			return ((IIconic)object).getImagePosition();
		} catch (Exception e) {}
		return 0;
	}
	public void setImagePosition(int _position) {
		try {
			((IIconic)object).setImagePosition(_position);
		} catch (Exception e) {}
	}
	public boolean isLocked() {
		try {
			return ((ILockable)object).isLocked();
		} catch (Exception e) {}
		return false;
	}
	public void setLocked(boolean _locked) {
		try {
			((ILockable)object).setLocked(_locked);
		} catch (Exception e) {}
	}
	public String getBorderColor() {
		try {
			return ((IBorderObject)object).getBorderColor();
		} catch (Exception e) {}
		return null;
	}
	public void setBorderColor(String _color) {
		try {
			((IBorderObject)object).setBorderColor(_color);
		} catch (Exception e) {}
	}
	public String getNotes() {
		try {
			return ((INotesContent)object).getNotes();
		} catch (Exception e) {}
		return null;
	}
	public void setNotes(String _notes) {
		try {
			((INotesContent)object).setNotes(_notes);
		} catch (Exception e) {}
	}
	public EList<IFolder> getFolders() {
		try {
			return ((IFolderContainer)object).getFolders();
		} catch (Exception e) {}
		return null;
	}
	public String getFolder() {
		try {
			return DBPlugin.getId(((IIdentifier)object.eContainer()).getId());
		} catch (Exception e) {}
		return null;
	}
	public void setFolder(String _id) {
		String id = DBPlugin.isVersionned(_id) ? _id : DBPlugin.generateId(_id, getModelId(), getVersion());
		IFolder folder = getFolderById(dbModel.getFolders(), id);
		
		if ( folder != null ) {
			
			if ( object instanceof IFolder ) {
				folder.getFolders().add((IFolder)object);
			} else {
				folder.getElements().add(object);
			}
		}
	}
	public FolderType getFolderType() {
		try {
			return ((IFolder)object).getType();
		} catch (Exception e) {}
		return null;
	}
	private IFolder getFolderById(EList<IFolder> _folders, String _id) {
		if ( _folders == null ) return null;
		for ( IFolder f: _folders ) {
			if ( f.getId().equals(_id) ) {
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
