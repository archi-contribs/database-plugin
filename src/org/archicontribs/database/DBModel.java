package org.archicontribs.database;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperty;


//*
//*
//*  REFAIRE LE DBMODEL EN AJOUTANT UN PROJET
//*  IMPLEMENTE SOUS LA FORME D'UN DOSSIER
//*  
//*  SI PROJET POSITIONNE ALORS DBMODEL.GetId = ID projet
//*  SINON, GetId = Id model
//*
//*
//*


public class DBModel {
	private IArchimateModel model;
	private IFolder folder;

	public DBModel(IArchimateModel _model) {
		model = _model;
		folder = null;
	}
	public DBModel(IArchimateModel _model, IFolder _folder) {
		model = _model;
		folder = _folder;
	}
	public IArchimateModel getModel() {
		return model;
	}
	public IArchimateModel setModel(IArchimateModel _model) {
		return model=_model;
	}

	public IFolder getFolder() {
		return folder;
	}
	public IFolder setFolder(IFolder _folder) {
		return folder=_folder;
	}
	public IFolder setFolderById(String _modelId) {
		folder = null;
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
				for ( IFolder ff: f.getFolders() ) {
					if ( _modelId.equals(DBPlugin.getModelId(ff.getId())) ) {
						folder = ff;
						return ff;
					}
				}
			}
		}
		return null;
	}
	public IFolder setFolderByName(String _name) {
		folder = null;
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
				for ( IFolder ff: f.getFolders() ) {
					if ( _name.equals(ff.getName()) ) {
						folder = ff;
						return ff;
					}
				}
			}
		}
		return null;
	}
	public IFolder addFolder(String _modelId, String _version, String _name, String _purpose) {
		boolean sharedFolderExists = false;

		for ( IFolder f: model.getFolders() ) {
			// we create a sub-folder in all the model's folders
			boolean subFolderShouldBeCreated = true;
			for ( IFolder ff: f.getFolders() ) {
				if ( DBPlugin.getModelId(ff.getId()).equals(_modelId) )
					subFolderShouldBeCreated = false;	// the subfolder already exists, we do not need to create it
			}
			if ( subFolderShouldBeCreated ) {
				IFolder subFolder = addFolder(f.getFolders(), _name, _modelId, _version);
				if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
					subFolder.setDocumentation(_purpose);
					sharedFolderExists = true;
					folder = subFolder;
				}
			}
		}
		if ( !sharedFolderExists ) {
			IFolder sharedModelsFolder = addFolder(model.getFolders(), DBPlugin.SharedFolderName);
			folder = addFolder(sharedModelsFolder.getFolders(), _name, _modelId, _version);
			folder.setDocumentation(_purpose);
		}
		return folder;
	}
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name) {
		return addFolder(_parentFolder, _name, null, null);
	}
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name, String _modelId, String _version) {
		IFolder subFolder = IArchimateFactory.eINSTANCE.createFolder();
		subFolder.setId(DBPlugin.generateModelId(_modelId, _version));
		subFolder.setName(_name);
		_parentFolder.add(subFolder);
		return subFolder;
	}

	public Boolean isVersionned() {
		if ( folder != null )
			return DBPlugin.isVersionned(folder.getId());
		return DBPlugin.isVersionned(model.getId());
	}
	public String generateId(String _id) {
		return DBPlugin.generateId(_id, getModelId(), getVersion());
	}
	public String getModelId() {
		return folder!=null  ? DBPlugin.getModelId(folder.getId()) : DBPlugin.getModelId(model.getId()) ;
	}
	public String setModelId(String _modelId, String _version) {
		if ( folder == null )
			model.setId(DBPlugin.generateModelId(_modelId, _version));
		if ( isVersionned() ) {				
			String oldId = getModelId();
			for ( IFolder f: model.getFolders() ) {
				for ( IFolder ff: f.getFolders() ) {
					if ( oldId.equals(DBPlugin.getModelId(ff.getId())) ) {	// we do not check the version as we can only have one version at a time
						ff.setId(DBPlugin.generateId(ff.getId().split(DBPlugin.Separator)[0], _modelId, _version));
					}
				}
			}
		}
		return _modelId;
	}

	public String getVersion() {
		return DBPlugin.getVersion(folder!=null ? folder.getId() : model.getId());
	}

	public EList<IProperty> getProperties() {
		return folder!=null ? folder.getProperties() : model.getProperties();
	}

	public String getName() {
		return folder!=null ? folder.getName() : model.getName();
	}
	public String setName(String _name) {
		if ( _name == null || _name.trim().isEmpty() )
			return null;
		if ( folder == null )
			model.setName(_name);
		else for ( IFolder f: model.getFolders() ) {
			// we create a sub-folder in all the model's folders (if it does not exist yet)
			for ( IFolder ff: f.getFolders() ) {
				if ( DBPlugin.getModelId(folder.getId()).equals(DBPlugin.getModelId(ff.getId())) ) {	// we do not check the version as we can only have one version at a time
					ff.setName(_name);
				}
			}
		}
		return _name;
	}

	public String getPurpose() {
		return folder!= null ? folder.getDocumentation() : model.getPurpose();
	}
	public void setPurpose(String _purpose) {
		if ( _purpose == null )
			_purpose = "";

		if ( folder!= null )
			folder.setDocumentation(_purpose);
		else
			model.setPurpose(_purpose);
	}

	public EList<IFolder> getFolders() {
		if ( folder == null )
			return model.getFolders();

		EList<IFolder> folders = new BasicEList<IFolder>();
		for ( IFolder f: model.getFolders() ) {
			for ( IFolder ff: f.getFolders() ) {
				if ( (DBPlugin.isVersionned(ff.getId()) && DBPlugin.getModelId(ff.getId()).equals(DBPlugin.getModelId(folder.getId()))) || ff.getName().equals(folder.getName()) )
					folders.add(ff);
			}
		}
		return folders;
	}
	public EList<IFolder> getAllModels() {
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) return f.getFolders();
		}
		return null;
	}

	public IFolder getDefaultFolderForFolderType(FolderType _folderType ) {
		for ( IFolder f: model.getFolders() ) {
			if ( f.getType().equals(_folderType) ) {
				if ( folder == null ) return f;			// in standalone mode, we return the model's folder
				for ( IFolder ff: f.getFolders() ) {	// in shared mode, we look for the sub-folder that has got the correct ID
					if ( DBPlugin.getModelId(ff.getId()).equals(DBPlugin.getModelId(folder.getId())) )
						return ff;
				}
			}
		}
		return null;	//shouldn't be the case, but we never know ...
	}
	public IFolder getDefaultFolderForElement(EObject _eObject) {
		if ( folder == null )
			return model.getDefaultFolderForElement(_eObject);
		try {
			for ( IFolder f: model.getDefaultFolderForElement(_eObject).getFolders() ) {
				if ( DBPlugin.getModelId(f.getId()).equals(DBPlugin.getModelId(((IIdentifier)_eObject).getId())) ) {
					return f;
				}
			}
		} catch (Exception e) {}
		return null;	//shouldn't be the case, but we never know ...
	}

	public TreeIterator<EObject> eAllContents() {
		return folder != null ? folder.eAllContents() : model.eAllContents();
	}
	
	public boolean isShared() {
		return model.getId().equals(DBPlugin.SharedModelId);
	}
}