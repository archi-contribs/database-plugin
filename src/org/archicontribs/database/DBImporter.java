/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.editor.model.ISelectedModelImporter;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.ArchimateModelUtils;

//
//vérifier si des vues ou des relations d'autres modèles référencent des objets disparus
//pour ça, utiliser la transaction :
//  1 - créer transaction
//  2 - sauvegarder le modèle
//  3 - demander à l'utilisateur
//				soit on modifie les autres projets pour que les vues et les relations pointent vers la nouvelle version des objets
//				soit on ne les modifie pas
//				soit on utilise une propriété pour le spécifier, objet par objet
//  4 - si des modèles sont modifiés par cette opération, alors il faut auto-générer une nouvelle version
//

/**
 * Import from Database
 * 
 * @author Hervé JOUIN
 */

public class DBImporter implements IModelImporter, ISelectedModelImporter {
	private Connection db;
	private int nbImported; //TODO: calculate more detailed statistics and check with values stored in the database
	private int nbElement;
	private int nbRelation;
	private int nbDiagram;
	private int nbProperty;
	private int nbBendpoint;
	private int nbDiagramObject;
	private int nbConnection;
	private int nbCanvas;
	private int nbCanvasModelBlock;
	private int nbCanvasModelSticky;
	private int nbFolder;

	DBList selectedModels;
	HashMap<String,String> modelSelected;

	@Override
	public void doImport() throws IOException {
		doImport(null);
	}

	@Override
	public void doImport(IArchimateModel _model) throws IOException {
		nbImported = 0;
		nbElement = 0;
		nbRelation = 0;
		nbDiagram = 0;
		nbProperty = 0;
		nbBendpoint = 0;
		nbDiagramObject = 0;
		nbConnection = 0;
		nbCanvas = 0;
		nbCanvasModelBlock = 0;
		nbCanvasModelSticky = 0;
		nbFolder = 0;

		if ( (db =new DBSelectDatabase().open()) == null)
			return;

		try {
			selectedModels = new DBSelectModel().open(db);
			if ( selectedModels == null || selectedModels.size() == 0) {
				try { db.close(); } catch (SQLException ee) { };
				return;
			}
			modelSelected = selectedModels.get(selectedModels.keySet().toArray()[0]);
		} catch (SQLException e) {
			DBPlugin.popup(Level.Error, "Failed to get the model list from the database.", e);
			try { db.close(); } catch (SQLException ee) {}
			return;
		}

		BusyIndicator.showWhile(Display.getDefault(), new Runnable(){
			public void run(){
				try {
					DBModel dbModel = new DBModel(null);
					
					//
					// Si on référence des objets d'autres modèles (dans une vue ou dans des relations)
					// alors, proposer à l'utilisateur
					//		soit charger les autres projets (en récursifs car ils peuvent dépendre les uns des autres)
					//		soit charger uniquement les objets dépendants dans un dossier spécial (mais attention à la sauvegarde)
					//		soit ne pas les charger mais ils devront être reconduits lors de la sauvegarde
					//
					
					if ( modelSelected.get("mode").equals("Shared") ) {
						// in shared mode, the database models will be loaded in folders in a generic model
						for (IArchimateModel m: IEditorModelManager.INSTANCE.getModels() ) {
							if ( m.getId().equals(DBPlugin.SharedModelId) ) {
								dbModel.setModel(m);
								break;
							}
						}
						if ( dbModel.getModel() == null ) {
							// if the shared model container does not yet exist, then we create it
							dbModel.setModel(IArchimateFactory.eINSTANCE.createArchimateModel());
							dbModel.getModel().setDefaults();
							dbModel.getModel().setId(DBPlugin.SharedModelId);
							dbModel.setName("Shared container");
							dbModel.setPurpose("This model is a container for all the models imported in shared mode.");
							IEditorModelManager.INSTANCE.registerModel(dbModel.getModel());
						} else {
							if ( dbModel.getAllModels() != null ) {
								// we deny to import a model twice as this will create ID conflicts
								for (IFolder f: dbModel.getAllModels() ) {
									if ( f.getId().split(DBPlugin.Separator)[0].equals(modelSelected.get("id")) ) {
										DBPlugin.popup(Level.Error, "You cannot import model \""+modelSelected.get("name")+"\" twice in shared mode.");
										//TODO : how to delete a model in shared mode ????? is delete button sufficient ? 
										try { db.close(); } catch (Exception ee) {}
										return;
									}
								}
							}
						}
						dbModel.addFolder(modelSelected.get("id"), modelSelected.get("version"), modelSelected.get("name"), modelSelected.get("purpose"));
					} else {
						// in standalone mode, we import the database model in a dedicated Archi model
						dbModel = new DBModel(IArchimateFactory.eINSTANCE.createArchimateModel());
						dbModel.getModel().setDefaults();
						dbModel.setModelId(modelSelected.get("id"), modelSelected.get("version"));
						dbModel.setName(modelSelected.get("name"));
						dbModel.setPurpose(modelSelected.get("purpose"));
						IEditorModelManager.INSTANCE.registerModel(dbModel.getModel());
					}

					// we import the model objects
					importProperties(dbModel);
					importFolders(dbModel);
					importArchimateElement(dbModel);
					importRelationship(dbModel);
					importArchimateDiagramModel(dbModel);
					importCanvasModel(dbModel);
					importCanvasModelBlock(dbModel);
					importCanvasModelSticky(dbModel);
					importDiagramModelArchimateObject(dbModel);
					importSourceConnections(dbModel);
					importTargetConnections(dbModel);

					//TODO: import all the other types folders, images, etc ...)
					String msg = "The model \""+dbModel.getName() + "\" has been imported.";
					nbImported = nbElement+nbRelation+nbDiagram+nbDiagramObject+nbConnection+nbCanvas+nbCanvasModelBlock+nbCanvasModelSticky+nbProperty+nbBendpoint+nbFolder;
					msg += "\n\n"+nbImported+" components imported in total :";
					msg += "\n     - "+nbElement+" elements";
					msg += "\n     - "+nbRelation+" relations";
					msg += "\n     - "+nbDiagram+" diagrams";
					msg += "\n     - "+nbDiagramObject+" diagram objects";
					msg += "\n     - "+nbConnection+" diagram connections";
					msg += "\n     - "+nbCanvas+" canvas";
					msg += "\n     - "+nbCanvasModelBlock+" canvas model blocks";
					msg += "\n     - "+nbCanvasModelSticky+" canvas model sticky";
					msg += "\n     - "+nbProperty+" properties";
					msg += "\n     - "+nbBendpoint+" bendpoints";
					msg += "\n     - "+nbFolder+" folders";
					DBPlugin.popup(Level.Info, msg);
				} catch (SQLException e) {
					DBPlugin.popup(Level.Error, "An error occured while importing the model from the database.\n\nThe model imported into Archi might be incomplete !!!", e);
					try { db.close(); } catch (Exception ee) {}
					return;
				}
			}
		});
		try { db.close(); } catch (Exception ee) {}
	}


	private void importArchimateElement(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, documentation, folder, name, type FROM archimateelement WHERE model = ? AND version = ?", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			++nbElement;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion()); 
			DBPlugin.debug("importing ArchimateElement " + result.getString("name") + " --> " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFolder(result.getString("folder"));
			importProperties(dbObject);
		}
	}

	private void importRelationship(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, documentation, name, source, target, type, folder FROM relationship WHERE model = ? AND version = ?", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			++nbRelation;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion());
			DBPlugin.debug("importing Relationship " + result.getString("name") + " --> " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));
			dbObject.setFolder(result.getString("folder"));

			importProperties(dbObject);
		}
	}

	private void importArchimateDiagramModel(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, connectionroutertype, documentation, folder, name, type, viewpoint FROM archimatediagrammodel WHERE model = ? AND version = ?", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			++nbDiagram;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion());
			DBPlugin.debug("importing ArchimateDiagramModel " + result.getString("name") + " --> " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFolder(result.getString("folder"));
			dbObject.setConnectionRouterType(result.getInt("connectionroutertype"));
			dbObject.setViewpoint(result.getInt("viewpoint"));
			
			importProperties(dbObject);
		}
	}
	private void importDiagramModelArchimateObject(DBModel _dbModel) throws SQLException {
		DBObject dbParent = null;
		String oldParent = null;

		ResultSet result = DBPlugin.select(db, "SELECT * FROM diagrammodelarchimateobject WHERE model = ? AND version = ? ORDER BY indent, rank, parent", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBPlugin.debug("importing DiagramModelArchimateObject " + result.getString("id") + " to parent " + DBPlugin.generateId(result.getString("parent"), _dbModel.getModelId(), _dbModel.getVersion()));
			if ( !result.getString("parent").equals(oldParent) ) {
				dbParent = new DBObject(_dbModel, DBPlugin.generateId(result.getString("parent"), _dbModel.getModelId(), _dbModel.getVersion()));
				if ( dbParent.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "Cannot import object ("+result.getString("id")+") as we do not know its parent ("+result.getString("parent")+")");
					break;
				}
			}

			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class"))));

			//setting common properties
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion());
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			
			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			//setting specific properties
			switch (result.getString("class")) {
			case "DiagramModelArchimateObject" :
				dbObject.setArchimateElement(result.getString("archimateelementid"),result.getString("archimateelementname"),result.getString("archimateelementclass"));
				dbObject.setType(result.getInt("type"));
				break;
			case "DiagramModelGroup" :
				dbObject.setName(result.getString("name"));
				dbObject.setDocumentation(result.getString("documentation"));
				importProperties(dbObject);

				break;
			case "DiagramModelNote" :
				dbObject.setName(result.getString("name"));
				dbObject.setBorderType(result.getInt("bordertype"));
				dbObject.setContent(result.getString("content"));
				break;
			default : //should never be here
				DBPlugin.popup(Level.Error,"Don't know how to import objects of type " + result.getString("class"));					
			}
			++nbDiagramObject;
			dbParent.addChild(dbObject);
		}
		result.close();
	}

	private void importSourceConnections(DBModel _dbModel) throws SQLException {
		DBObject dbParent = null;
		String oldParent = null;

		ResultSet result = DBPlugin.select(db, "SELECT * FROM diagrammodelarchimateconnection WHERE model = ? AND version = ? ORDER BY parent, rank", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			if ( !result.getString("parent").equals(oldParent) ) {
				dbParent = new DBObject(_dbModel, DBPlugin.generateId(result.getString("parent"), _dbModel.getModelId(), _dbModel.getVersion()));
				if ( dbParent.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "Cannot import connection ("+result.getString("id")+") as we do not know its parent ("+result.getString("parent")+")");
					break;
				}
			}
			DBObject dbSource = new DBObject(_dbModel, DBPlugin.generateId(result.getString("source"), _dbModel.getModelId(), _dbModel.getVersion()));
			if ( dbSource.getEObject() == null )
				DBPlugin.popup(Level.Error,  "Cannot found source "+result.getString("source"));
			DBObject dbTarget = new DBObject(_dbModel, DBPlugin.generateId(result.getString("target"), _dbModel.getModelId(), _dbModel.getVersion()));
			if ( dbTarget.getEObject() == null )
				DBPlugin.popup(Level.Error,  "Cannot found target "+result.getString("target"));
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class"))));
			++nbConnection;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion());
			DBPlugin.debug("importing DiagramModelArchimateConnection " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setText(result.getString("text"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setTextPosition(result.getInt("textposition"));
			dbObject.setType(result.getInt("type"));
			if ( "DiagramModelArchimateConnection".equals(result.getString("class")) )  dbObject.setRelationship(result.getString("relationship"));


			importBendpoints(dbObject);
			importProperties(dbObject);
			dbParent.getSourceConnections().add((IDiagramModelConnection)dbObject.getEObject());
		}
		result.close();
	}

	private void importTargetConnections(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, targetconnections FROM diagrammodelarchimateobject WHERE model = ? AND version = ? AND targetconnections IS NOT NULL", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, DBPlugin.generateId(result.getString("id"), _dbModel.getModelId(), _dbModel.getVersion()));
			for ( String target: result.getString("targetconnections").split(",")) {
				if ( ArchimateModelUtils.getObjectByID(_dbModel.getModel(), DBPlugin.generateId(target, _dbModel.getModelId(), _dbModel.getVersion())) == null )
					DBPlugin.popup(Level.Error, "Cannot found targetConnection "+target);
				else {
					dbObject.getTargetConnections().add((IDiagramModelConnection)ArchimateModelUtils.getObjectByID(_dbModel.getModel(), DBPlugin.generateId(target, _dbModel.getModelId(), _dbModel.getVersion())));
					++nbConnection;
				}
			}
		}
		result.close();
	}

	private void importCanvasModel(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodel WHERE model = ? AND version = ?", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModel());
			++nbCanvas;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion()); 
			DBPlugin.debug("importing CanvasModel " + result.getString("name") + " --> " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setHintContent(result.getString("hintcontent"));
			dbObject.setHintTitle(result.getString("hinttitle"));
			dbObject.setHintContent(result.getString("hintcontent"));
			dbObject.setConnectionRouterType(result.getInt("connectionroutertype"));
			dbObject.setFolder(result.getString("folder"));

			importProperties(dbObject);
		}
	}

	private void importCanvasModelBlock(DBModel _dbModel) throws SQLException {
		//import ICanvasModelBlock objects
		DBObject dbParent = null;
		String oldParent = null;

		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodelblock WHERE model = ? AND version = ? ORDER BY parent, rank, indent", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			if ( !result.getString("parent").equals(oldParent) ) {
				dbParent = new DBObject(_dbModel, DBPlugin.generateId(result.getString("parent"), _dbModel.getModelId(), _dbModel.getVersion()));
				if ( dbParent.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "Cannot import CanvasModelBlock ("+DBPlugin.generateId(result.getString("id"), _dbModel.getModelId(), _dbModel.getVersion())+") as we do not know its parent ("+DBPlugin.generateId(result.getString("parent"), _dbModel.getModelId(), _dbModel.getVersion())+")");
					break;
				}
			}
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModelBlock());
			++nbCanvasModelBlock;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion());
			DBPlugin.debug("importing CanvasModelBlock " + result.getString("name") + " --> " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setBorderColor(result.getString("bordercolor"));
			dbObject.setContent(result.getString("content"));
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setHintContent(result.getString("hintcontent"));
			dbObject.setHintTitle(result.getString("hinttitle"));
			dbObject.setImagePath(result.getString("imagepath"));
			dbObject.setImagePosition(result.getInt("imageposition"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setTextPosition(result.getInt("textposition"));
			dbObject.setLocked(result.getBoolean("islocked"));

			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));
			
			importProperties(dbObject);

			dbParent.addChild(dbObject);
		}
	}

	private void importCanvasModelSticky(DBModel _dbModel) throws SQLException {
		//import ICanvasModelSticky objects
		DBObject dbParent = null;
		String oldParent = null;

		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodelsticky WHERE model = ? AND version = ? ORDER BY parent, rank, indent", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			if ( !result.getString("parent").equals(oldParent) ) {
				dbParent = new DBObject(_dbModel, DBPlugin.generateId(result.getString("parent"), _dbModel.getModelId(), _dbModel.getVersion()));
				if ( dbParent.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "Cannot import CanvasModelBlock ("+result.getString("id")+") as we do not know its parent ("+result.getString("parent")+")");
					break;
				}
			}
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModelSticky());
			++nbCanvasModelSticky;
			dbObject.setId(result.getString("id"), _dbModel.getModelId(),_dbModel.getVersion());
			DBPlugin.debug("importing CanvasModelSticky " + result.getString("name") + " --> " + ((IIdentifier)dbObject.getEObject()).getId());
			dbObject.setBorderColor(result.getString("bordercolor"));
			dbObject.setContent(result.getString("content"));
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setImagePath(result.getString("imagepath"));
			dbObject.setImagePosition(result.getInt("imageposition"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setNotes(result.getString("notes"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setTextPosition(result.getInt("textposition"));

			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));

			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));
			
			importProperties(dbObject);

			dbParent.addChild(dbObject);
		}
	}

	private void importBendpoints(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT startx, starty, endx, endy FROM bendpoint WHERE parent = ? AND model = ? AND version = ? ORDER BY rank", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion());
		while(result.next()) {
			_dbObject.setBendpoint(result.getInt("startx"), result.getInt("starty"), result.getInt("endx"), result.getInt("endy"));
			++nbBendpoint;
		}
		result.close();
	}

	private void importProperties(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT name, value FROM property WHERE parent = ? AND model = ? AND version = ? ORDER BY id", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion());
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			++nbProperty;
			prop.setKey(result.getString("name"));
			prop.setValue(result.getString("value"));
			_dbObject.getProperties().add(prop);
		}
		result.close();
	}
	private void importProperties(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT name, value FROM property WHERE parent = ? AND model = ? AND version = ? ORDER BY id", _dbModel.getModelId(), _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			++nbProperty;
			prop.setKey(result.getString("name"));
			prop.setValue(result.getString("value"));
		}
		result.close();
	}
	private void importFolders(DBModel _dbModel) throws SQLException {
		DBObject folder;
		ResultSet result = DBPlugin.select(db, "SELECT id, model, version, documentation, parent, type, name FROM folder WHERE model = ? AND version = ? ORDER BY rank", _dbModel.getModelId(), _dbModel.getVersion());
		while(result.next()) {
			DBPlugin.debug("importing folder "+result.getString("name")+" (type = "+result.getInt("type")+")");
			if ( result.getInt("type") != 0 ) {		// folders are first level folders, they've already been created.
				folder = new DBObject(_dbModel, _dbModel.getDefaultFolderForFolderType(FolderType.get(result.getInt("type"))));
				if ( folder.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "I do not find default folder for type "+result.getInt("type")+" ("+FolderType.get(result.getInt("type")).name()+")");
					return;
				}
			} else {
				folder = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createFolder());
				folder.setName(result.getString("name"));
				folder.setFolder(result.getString("parent"));			
			}
			folder.setId(result.getString("id"), result.getString("model"), result.getString("version"));
			folder.setDocumentation(result.getString("documentation"));
			++nbFolder;
			importProperties(folder);
		}
		result.close();
	}
}