/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database;

import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperty;


/**
 * The DBModel class is a proxy to the IArchimateModel class in standalone mode, and to the IFolder class in shared mode.
 * <br>
 * It allows to transparentely access methods of the right object  
 * 
 * @author Hervé
 *
 */
public class DBModel {
	/**
	 * The IArchimate object:
	 * <br>
	 * <li>In standalone mode : the whole model</li>
	 * <li>In shared mode : the container model</li>
	 */
	private IArchimateModel model = null;
	
	/**
	 * <li>In standalone mode : null</li>
	 * <li>In shared mode : the project folder (as a subfolder of the folder "Models")</li>
	 */
	private IFolder projectFolder = null;
	
	/**
	 * Hashtable containing searcheable model components (the container model in shared mode).
	 * <br>
	 * This Hashtable allows to easily retrieve an element knowing its ID.  
	 */
	private Hashtable<String, EObject> objects = new Hashtable<String, EObject>();
	
	public DBModel() {
		this(null, null);
	}
	public DBModel(IArchimateModel _model) {
		this(_model, null);
	}
	public DBModel(IArchimateModel _model, IFolder _folder) {
		EObject obj;
		
		if ( _model == null ) {
			// If no existing model is provided, then we set it to the shared container
			for (IArchimateModel m: IEditorModelManager.INSTANCE.getModels() ) {
				if ( m.getId().equals(DBPlugin.SharedModelId) ) {
					model = m;
					break;
				}
			}
			// if the shared container doesn't exist, we create it 
			if ( _model == null ) {
				model = IArchimateFactory.eINSTANCE.createArchimateModel();
				model.setDefaults();
				model.setId(DBPlugin.SharedModelId);
				model.setName("Shared container");
				model.setPurpose("This model is a container for all the models imported in shared mode.");
				IEditorModelManager.INSTANCE.registerModel(model);
			}
		} else {
			// if an existing model is provided, we memorize it and hashes its components to ease their retrieval
			model = _model;
			
			// we populate the objects hashtable with existing components
			for(Iterator<EObject> iter = _model.eAllContents(); iter.hasNext();) {
				obj = iter.next();
				if ( obj instanceof IIdentifier )
					objects.put(((IIdentifier)obj).getId(), obj);
			}
		}
		
		// In shared mode, the folder may be provided in the constructor, or later on using a setter
		projectFolder = _folder;
	}
	
	/**
	 * returns the model. In shared mode, the container model is returned.
	 * 
	 * @return IArchimateModel
	 */
	public IArchimateModel getModel() {
		return model;
	}
	
	/**
	 * returns the folder in shared mode.
	 * 
	 * @return
	 */
	public IFolder getProjectFolder() {
		return projectFolder;
	}
	
	/**
	 * Sets the project folder in shared mode.
	 * 
	 * @param _folder
	 * @return IFolder
	 */
	public IFolder setProjectFolder(IFolder _folder) {
		return projectFolder=_folder;
	}
	
	/**
	 * Search a subfolder in the "Projects" folder by its model ID.
	 *  
	 * @param _id
	 * @return IFolder
	 */
	public IFolder searchProjectFolderById(String _id) {
		projectFolder = null;
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
				for ( IFolder ff: f.getFolders() ) {
					if ( _id.equals(DBPlugin.getProjectId(ff.getId())) ) {
						return ff;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Search a subfolder in the "Projects" folder by its name.
	 *  
	 * @param _name
	 * @return IFolder
	 */
	public IFolder searchProjectFolderByName(String _name) {
		projectFolder = null;
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
				for ( IFolder ff: f.getFolders() ) {
					if ( _name.equals(ff.getName()) ) {
						return ff;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Create the project folders structure : one project subfolder per first-level model folder (Business, Application, ...)
	 * 
	 * @param _projectId
	 * @param _version
	 * @param _name
	 * @param _purpose
	 * @return IFolder
	 */
	public IFolder addFolder(String _projectId, String _version, String _name, String _purpose) {
		boolean sharedFolderExists = false;

		for ( IFolder f: model.getFolders() ) {
			// we create a sub-folder in all first-level model folders
			boolean exists = false;
			for ( IFolder ff: f.getFolders() ) {
				if ( DBPlugin.getProjectId(ff.getId()).equals(_projectId) ) {
					exists = true;	// the subfolder already exists, we do not need to create it
					break;
				}
			}
			if ( !exists ) {
				IFolder subFolder = addFolder(f.getFolders(), _name, _projectId, _version);
				if ( f.getName().equals(DBPlugin.SharedFolderName) ) {
					subFolder.setDocumentation(_purpose);
					sharedFolderExists = true;
					projectFolder = subFolder;
				}
			}
		}
		if ( !sharedFolderExists ) {
			IFolder sharedModelsFolder = addFolder(model.getFolders(), DBPlugin.SharedFolderName);
			addFolder(sharedModelsFolder.getFolders(), DBPlugin.ExternalFolderName);
			projectFolder = addFolder(sharedModelsFolder.getFolders(), _name, _projectId, _version);
			projectFolder.setDocumentation(_purpose);
		}
		return projectFolder;
	}
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name) {
		return addFolder(_parentFolder, _name, null, null);
	}
	private IFolder addFolder(EList<IFolder> _parentFolder, String _name, String _projectId, String _version) {
		IFolder subFolder = IArchimateFactory.eINSTANCE.createFolder();
		subFolder.setId(DBPlugin.generateId(null, _projectId, _version));
		subFolder.setName(_name);
		_parentFolder.add(subFolder);
		return subFolder;
	}

	/**
	 * Determines if the model's ID (standalone mode) or project folder's ID (shared mode) contains a version number.
	 * @return true of false
	 */
	public Boolean isVersionned() {
		return DBPlugin.isVersionned(projectFolder == null ? model.getId() : projectFolder.getId()); 
	}

	/**
	 * Decodes the project's ID from the ID of the model (standalone mode) or the project folder (shared mode)
	 * 
	 * @return
	 */
	public String getProjectId() {
		return DBPlugin.getProjectId(projectFolder!=null ? projectFolder.getId() : model.getId());
	}
	
	/**
	 * Encodes the project ID and the version in the model's ID (standalone mode) or in the project's folder ID (shared mode) 
	 * @param _projectId
	 * @param _version
	 * @return
	 */
	public String setProjectId(String _projectId, String _version) {
		if ( projectFolder == null )
			model.setId(DBPlugin.generateProjectId(_projectId, _version));
		if ( isVersionned() ) {				
			String oldId = getProjectId();
			for ( IFolder f: model.getFolders() ) {
				for ( IFolder ff: f.getFolders() ) {
					if ( oldId.equals(DBPlugin.getProjectId(ff.getId())) ) {	// we do not check the version as we can only have one version at a time
						ff.setId(DBPlugin.generateId(ff.getId().split(DBPlugin.Separator)[0], _projectId, _version));
					}
				}
			}
		}
		return _projectId;
	}

	/**
	 * Decodes the version from the ID of the model (standalone mode) or the project folder (shared mode)
	 * 
	 * @return
	 */
	public String getVersion() {
		return DBPlugin.getVersion(projectFolder!=null ? projectFolder.getId() : model.getId());
	}

	/**
	 * gets the properties of the model (standalone mode) or the project folder (shared mode)
	 * @return
	 */
	public EList<IProperty> getProperties() {
		return projectFolder!=null ? projectFolder.getProperties() : model.getProperties();
	}

	/**
	 * Gets the name of the model (standalone mode) or the project folder (shared mode)
	 * @return
	 */
	public String getName() {
		return projectFolder!=null ? projectFolder.getName() : model.getName();
	}
	
	/**
	 * Sets the name of the model (standalone mode) or the project folder (shared mode)
	 * @param _name
	 * @return
	 */
	public String setName(String _name) {
		if ( _name == null || _name.trim().isEmpty() )
			return null;
		if ( projectFolder == null )
			model.setName(_name);
		else for ( IFolder f: model.getFolders() ) {
			// we create a sub-folder in all the model's folders (if it does not exist yet)
			for ( IFolder ff: f.getFolders() ) {
				if ( DBPlugin.getProjectId(projectFolder.getId()).equals(DBPlugin.getProjectId(ff.getId())) ) {	// we do not check the version as we can only have one version at a time
					ff.setName(_name);
				}
			}
		}
		return _name;
	}

	/**
	 * Gets the purpose of the model (standalone mode) or thedocumentation of the project folder (shared mode)
	 * @return
	 */
	public String getPurpose() {
		return projectFolder!= null ? projectFolder.getDocumentation() : model.getPurpose();
	}
	
	/**
	 * Sets the purpose of the model (standalone mode) or the documentation of the project folder (shared mode)
	 * @param _purpose
	 */
	public void setPurpose(String _purpose) {
		if ( _purpose == null )
			_purpose = "";

		if ( projectFolder!= null )
			projectFolder.setDocumentation(_purpose);
		else
			model.setPurpose(_purpose);
	}

	/**
	 * Gets the subfolders of the model (standalone mode) or the project folder (shared mode)
	 * @return
	 */
	public EList<IFolder> getFolders() {
		if ( projectFolder == null )
			return model.getFolders();

		EList<IFolder> folders = new BasicEList<IFolder>();
		for ( IFolder f: model.getFolders() ) {
			for ( IFolder ff: f.getFolders() ) {
				if ( (DBPlugin.isVersionned(ff.getId()) && DBPlugin.getProjectId(ff.getId()).equals(DBPlugin.getProjectId(projectFolder.getId()))) || ff.getName().equals(projectFolder.getName()) )
					folders.add(ff);
			}
		}
		return folders;
	}
	
	/**
	 * Gets a list of projects folders (shared mode only).
	 * @return
	 */
	public EList<IFolder> getAllModels() {
		for ( IFolder f: model.getFolders() ) {
			if ( f.getName().equals(DBPlugin.SharedFolderName) ) return f.getFolders();
		}
		return null;
	}

	/**
	 * Gets the default folder for a given folder type.
	 * @param _folderType
	 * @return
	 * <li>In standalone mode, the standard "Business", "Application", ... folder</li>
	 * <li>In shared mode, the project subfolder of "Business", "Application", ...</li>
	 */
	public IFolder getDefaultFolderForFolderType(FolderType _folderType ) {
		for ( IFolder f: model.getFolders() ) {
			if ( f.getType().equals(_folderType) ) {
				if ( projectFolder == null ) return f;			// in standalone mode, we return the model's folder
				for ( IFolder ff: f.getFolders() ) {	// in shared mode, we look for the sub-folder that has got the correct ID
					if ( DBPlugin.getProjectId(ff.getId()).equals(DBPlugin.getProjectId(projectFolder.getId())) )
						return ff;
				}
			}
		}
		return null;	//shouldn't be the case, but we never know ...
	}
	
	/**
	 *  Gets the default folder for a given element
	 * @param _eObject
	 * @return
	 * <li>In standalone mode, the standard "Business", "Application", ... folder</li>
	 * <li>In shared mode, the project subfolder of "Business", "Application", ...</li>
	 */
	public IFolder getDefaultFolderForElement(EObject _eObject) {
		if ( projectFolder == null )
			return model.getDefaultFolderForElement(_eObject);
		try {
			for ( IFolder f: model.getDefaultFolderForElement(_eObject).getFolders() ) {
				if ( DBPlugin.getProjectId(f.getId()).equals(DBPlugin.getProjectId(((IIdentifier)_eObject).getId())) ) {
					return f;
				}
			}
		} catch (Exception e) {}
		return null;	//shouldn't be the case, but we never know ...
	}

	/**
	 * Determines if we are in standalone or shared mode.
	 * @return
	 * <li>false : standalone mode</li>
	 * <li>true : shared mode</li>
	 */
	public boolean isShared() {
		return model.getId().equals(DBPlugin.SharedModelId);
	}
	
	/**
	 * Puts the EObject in a hashtable to allow future retrieval
	 * @param _element
	 */
	public void registerEObject(EObject _obj) {
		objects.put(((IIdentifier)_obj).getId(), _obj);
	}
	
	/**
	 * register the EObject attribute of the DBObject in a hashtable to allow future retrieval
	 * @param _element
	 */
	public void registerDBObject(DBObject _obj) {
		//DBPlugin.debug("registering DBObject " + _obj.getId());
		registerEObject(_obj.getEObject());
	}
	
	/**
	 * Retrieve the EObject from the its ID
	 * @return IArchimateElement
	 */
	public EObject searchEObjectById(String _id) {
		return objects.get(_id);
	}
	
	/**
	 * Retrieve the DBObject from the its ID
	 * @return IArchimateElement
	 */
	public DBObject searchDBObjectById(String _id) {
		//DBPlugin.debug("Searching for DBObject " + _id);
		return new DBObject(this, searchEObjectById(_id));
	}
}