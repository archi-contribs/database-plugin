package org.archicontribs.database;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;

public class DBModel {
	private IArchimateModel model;

	public DBModel(IArchimateModel _model) {
		model = _model;
	}
	public IArchimateModel getModel() {
		return model;
	}
	public Boolean isVersionned() {
		return model.getId().contains(DBPlugin.Separator);
	}
	public String getId() {
		if ( isVersionned() ) return model.getId().split(DBPlugin.Separator)[0];
		return model.getId();
	}
	public String getVersionnedId(String _id) {
		return getId() + DBPlugin.Separator + _id + DBPlugin.Separator + getVersion();
	}
	public void setId(String _id, String _version) {
		model.setId(_id + DBPlugin.Separator + _version);
	}
	public String getVersion() {
		if ( isVersionned() ) return model.getId().split(DBPlugin.Separator)[1];
		return null;
	}
	public EList<IProperty> getProperties() {
		return model.getProperties();
	}
	public String getName() {
		return model.getName();
	}
	public void setName(String _name) {
		if ( _name != null ) model.setName(_name);
	}
	public String getPurpose() {
		return model.getPurpose();
	}
	public void setPurpose(String _purpose) {
		if ( _purpose != null ) model.setPurpose(_purpose);
	}
	public IFolder getDefaultFolderForElement(EObject _eObject) {
		return model.getDefaultFolderForElement(_eObject);
	}
}
