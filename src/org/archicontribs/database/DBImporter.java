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
import java.util.concurrent.TimeUnit;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.ecore.EClass;

import com.archimatetool.canvas.model.ICanvasFactory;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.editor.model.ISelectedModelImporter;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IProperty;

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
	DBList selectedModels;
	DBModel dbModel;
	HashMap<String,String> modelSelected;
	private DBProgress dbProgress;
	private DBTabItem dbTabItem;
	
	private int countMetadatas;
	private int countFolders;
	private int countElements;
	private int countRelationships;
	private int countProperties;
	private int countArchimateDiagramModels;
	private int countDiagramModelArchimateConnections;
	private int countDiagramModelConnections;
	private int countDiagramModelReferences;
	private int countDiagramModelArchimateObjects;
	private int countDiagramModelGroups;
	private int countDiagramModelNotes;
	private int countCanvasModels;
	private int countCanvasModelBlocks;
	private int countCanvasModelStickys;
	private int countCanvasModelConnections;
	private int countCanvasModelImages;
	private int countImages;
	private int countSketchModels;
	private int countSketchModelActors;
	private int countSketchModelStickys;
	private int countDiagramModelBendpoints;
	private int countTotal;
	
	private int totalFolders;
	private int totalElements;
	private int totalRelationships;
	private int totalProperties;
	private int totalArchimateDiagramModels;
	private int totalDiagramModelArchimateConnections;
	private int totalDiagramModelConnections;
	private int totalDiagramModelArchimateObjects;
	private int totalDiagramModelGroups;
	private int totalDiagramModelNotes;
	private int totalCanvasModels;
	private int totalCanvasModelBlocks;
	private int totalCanvasModelStickys;
	private int totalCanvasModelConnections;
	private int totalCanvasModelImages;
	private int totalImages;
	private int totalSketchModels;
	private int totalSketchModelActors;
	private int totalSketchModelStickys;
	private int totalDiagramModelBendpoints;
	private int totalDiagramModelReferences;
	
	private int totalInDatabase;
	
	@Override
	public void doImport() throws IOException {
		doImport(null);
	}

	@Override
	public void doImport(IArchimateModel _model) throws IOException {

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
		
		dbProgress = new DBProgress();
		try {
			dbModel = null;

			//TODO
			// Si on référence des objets d'autres modèles (dans une vue ou dans des relations)
			// alors, proposer à l'utilisateur
			//		soit charger les autres projets (en récursifs car ils peuvent dépendre les uns des autres)
			//		soit charger uniquement les objets dépendants dans un dossier spécial (mais attention à la sauvegarde)
			//		soit ne pas les charger mais ils devront être reconduits lors de la sauvegarde
			//

			if ( modelSelected.get("mode").equals("Shared") ) {
				// in shared mode, the database models will be loaded in folders in a generic model
				dbModel = new DBModel();
				if ( dbModel.getProjectsFolders() != null ) {
					// we deny to import a model twice as this will create ID conflicts
					for (IFolder f: dbModel.getProjectsFolders() ) {
						if ( f.getId().split(DBPlugin.Separator)[0].equals(modelSelected.get("id")) ) {
							DBPlugin.popup(Level.Error, "You cannot import model \""+modelSelected.get("name")+"\" twice in shared mode.");
							//TODO : how to delete a model in shared mode ????? is delete button sufficient ? 
							try { db.close(); } catch (Exception ee) {}
							return;
						}
					}
				}
				dbModel.addFolder(modelSelected.get("id"), modelSelected.get("version"), modelSelected.get("name"), modelSelected.get("purpose"));
			} else {
				// in standalone mode, we import the database model in a dedicated Archi model
				dbModel = new DBModel(IArchimateFactory.eINSTANCE.createArchimateModel());
				dbModel.getModel().setDefaults();
				dbModel.setProjectId(modelSelected.get("id"), modelSelected.get("version"));
				dbModel.setName(modelSelected.get("name"));
				dbModel.setPurpose(modelSelected.get("purpose"));
				IEditorModelManager.INSTANCE.registerModel(dbModel.getModel());
			}

			long startTime = System.currentTimeMillis();
			
			//we show up the progressbar
			dbTabItem = dbProgress.tabItem(modelSelected.get("name"));
			dbTabItem.setText("Please wait while counting and versionning components ...");

			dbModel.initializeIndex();
			
			countMetadatas = 0;
			countFolders = 0;
			countElements = 0;
			countRelationships = 0;
			countProperties = 0;
			countArchimateDiagramModels = 0;
			countDiagramModelArchimateConnections = 0;
			countDiagramModelConnections = 0;
			countDiagramModelReferences = 0;
			countDiagramModelArchimateObjects = 0;
			countDiagramModelGroups = 0;
			countDiagramModelNotes = 0;
			countCanvasModels = 0;
			countCanvasModelBlocks = 0;
			countCanvasModelStickys = 0;
			countCanvasModelConnections = 0;
			countCanvasModelImages = 0;
			countImages = 0;
			countSketchModels = 0;
			countSketchModelActors = 0;
			countSketchModelStickys = 0;
			countDiagramModelBendpoints = 0;
			countTotal = 0;

			int totalMetadatas=Integer.parseInt(modelSelected.get("countMetadatas"));
			int totalFolders=Integer.parseInt(modelSelected.get("countFolders"));
			int totalElements=Integer.parseInt(modelSelected.get("countElements"));
			int totalRelationships=Integer.parseInt(modelSelected.get("countRelationships"));
			int totalProperties=Integer.parseInt(modelSelected.get("countProperties"));
			int totalArchimateDiagramModels=Integer.parseInt(modelSelected.get("countArchimateDiagramModels"));
			int totalDiagramModelArchimateConnections=Integer.parseInt(modelSelected.get("countDiagramModelArchimateConnections"));
			int totalDiagramModelConnections=Integer.parseInt(modelSelected.get("countDiagramModelConnections"));
			int totalDiagramModelArchimateObjects=Integer.parseInt(modelSelected.get("countDiagramModelArchimateObjects"));
			int totalDiagramModelGroups=Integer.parseInt(modelSelected.get("countDiagramModelGroups"));
			int totalDiagramModelNotes=Integer.parseInt(modelSelected.get("countDiagramModelNotes"));
			int totalCanvasModels=Integer.parseInt(modelSelected.get("countCanvasModels"));
			int totalCanvasModelBlocks=Integer.parseInt(modelSelected.get("countCanvasModelBlocks"));
			int totalCanvasModelStickys=Integer.parseInt(modelSelected.get("countCanvasModelStickys"));
			int totalCanvasModelConnections=Integer.parseInt(modelSelected.get("countCanvasModelConnections"));
			int totalCanvasModelImages=Integer.parseInt(modelSelected.get("countCanvasModelImages"));
			int totalImages=Integer.parseInt(modelSelected.get("countImages"));
			int totalSketchModels=Integer.parseInt(modelSelected.get("countSketchModels"));
			int totalSketchModelActors=Integer.parseInt(modelSelected.get("countSketchModelActors"));
			int totalSketchModelStickys=Integer.parseInt(modelSelected.get("countSketchModelStickys"));
			int totalDiagramModelBendpoints=Integer.parseInt(modelSelected.get("countDiagramModelBendpoints"));
			int totalDiagramModelReferences=Integer.parseInt(modelSelected.get("countDiagramModelReferences"));
			
			totalInDatabase = totalMetadatas + totalFolders + totalElements + totalRelationships + totalProperties +
					totalArchimateDiagramModels + totalDiagramModelArchimateObjects + totalDiagramModelArchimateConnections + totalDiagramModelGroups + totalDiagramModelNotes +  
					totalCanvasModels + totalCanvasModelBlocks + totalCanvasModelStickys + totalCanvasModelConnections + totalCanvasModelImages + 
					totalSketchModels + totalSketchModelActors + totalSketchModelStickys + totalDiagramModelConnections +
					totalDiagramModelBendpoints + totalDiagramModelReferences + totalImages;
			
			dbTabItem.setMaximum(totalInDatabase);
			dbTabItem.setText("Please wait while importing components ...");

			// we import the model objects
			importProperties(dbModel);
			importFolders(dbModel);
			importArchimateElement(dbModel);
			importRelationship(dbModel);

			importArchimateDiagramModel(dbModel);

			importCanvasModel(dbModel);
			importCanvasModelBlock(dbModel);
			importCanvasModelSticky(dbModel);
			importCanvasModelImage(dbModel);

			importSketchModel(dbModel);
			importSketchModelActor(dbModel);
			importSketchModelSticky(dbModel);

			importDiagramModelArchimateObject(dbModel);

			importDiagramModelReference(dbModel);

			importConnections(dbModel);
			importTargetConnections(dbModel);
			
			importImages(dbModel);

			dbTabItem.setText("Please wait while resolving dependencies ...");
			dbModel.resolveSourceConnections();
			dbModel.resolveChildren();

			long endTime = System.currentTimeMillis();
			
			String duration = String.format("%d'%02d", TimeUnit.MILLISECONDS.toMinutes(endTime-startTime), TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime)));
			
			String msg;
			
			if ( countTotal == totalInDatabase ) {
				msg = "The model \"" + dbModel.getName() + "\" has been successfully imported from the database in "+duration+"\n\n";
				msg += "--> " + countTotal + " components imported <--";
			} else {
				msg = "The model \"" + dbModel.getName() + "\" has been imported from the database in "+duration+", but with errors !\nPlesae check below :\n";
				msg += "--> "+ countTotal + "/" + totalInDatabase + " components imported <--";
			}
			
			dbTabItem.setText(msg);
			
			if ( countMetadatas != dbModel.countMetadatas() )										dbTabItem.setCountMetadatas(String.valueOf(countMetadatas) + " / " + String.valueOf(dbModel.countMetadatas()));
			
			if ( countFolders != totalFolders )														dbTabItem.setCountFolders(String.valueOf(countFolders) + " / " + String.valueOf(totalFolders));
			if ( countElements != totalElements )													dbTabItem.setCountElements(String.valueOf(countElements) + " / " + String.valueOf(totalElements));
			if ( countRelationships != totalRelationships )											dbTabItem.setCountRelationships(String.valueOf(countRelationships) + " / " + String.valueOf(totalRelationships));
			if ( countProperties != totalProperties )												dbTabItem.setCountProperties(String.valueOf(countProperties) + " / " + String.valueOf(totalProperties));

			if ( countArchimateDiagramModels != totalArchimateDiagramModels )						dbTabItem.setCountArchimateDiagramModels(String.valueOf(countArchimateDiagramModels) + " / " + String.valueOf(totalArchimateDiagramModels));
			if ( countDiagramModelArchimateObjects != totalDiagramModelArchimateObjects )			dbTabItem.setCountDiagramModelArchimateObjects(String.valueOf(countDiagramModelArchimateObjects) + " / " + String.valueOf(totalDiagramModelArchimateObjects));
			if ( countDiagramModelArchimateConnections != totalDiagramModelArchimateConnections )	dbTabItem.setCountDiagramModelArchimateConnections(String.valueOf(countDiagramModelArchimateConnections) + " / " + String.valueOf(totalDiagramModelArchimateConnections));
			if ( countDiagramModelConnections != totalDiagramModelConnections )						dbTabItem.setCountDiagramModelConnections(String.valueOf(countDiagramModelConnections) + " / " + String.valueOf(totalDiagramModelConnections));

			if ( countDiagramModelGroups !=+ totalDiagramModelGroups )								dbTabItem.setCountDiagramModelGroups(String.valueOf(countDiagramModelGroups) + " / " + String.valueOf(totalDiagramModelGroups));
			if ( countDiagramModelNotes != totalDiagramModelNotes )									dbTabItem.setCountDiagramModelNotes(String.valueOf(countDiagramModelNotes) + " / " + String.valueOf(totalDiagramModelNotes));

			if ( countCanvasModels != totalCanvasModels )											dbTabItem.setCountCanvasModels(String.valueOf(countCanvasModels) + " / " + String.valueOf(totalCanvasModels));
			if ( countCanvasModelBlocks != totalCanvasModelBlocks )									dbTabItem.setCountCanvasModelBlocks(String.valueOf(countCanvasModelBlocks) + " / " + String.valueOf(totalCanvasModelBlocks));
			if ( countCanvasModelStickys != totalCanvasModelStickys )								dbTabItem.setCountCanvasModelStickys(String.valueOf(countCanvasModelStickys) + " / " + String.valueOf(totalCanvasModelStickys));
			if ( countCanvasModelConnections != totalCanvasModelConnections )						dbTabItem.setCountCanvasModelConnections(String.valueOf(countCanvasModelConnections) + " / " + String.valueOf(totalCanvasModelConnections));
			if ( countCanvasModelImages != totalCanvasModelImages )									dbTabItem.setCountCanvasModelImages(String.valueOf(countCanvasModelImages) + " / " + String.valueOf(totalCanvasModelImages));

			if ( countSketchModels != totalSketchModels )											dbTabItem.setCountSketchModels(String.valueOf(countSketchModels) + " / " + String.valueOf(totalSketchModels));
			if ( countSketchModelActors != totalSketchModelActors )									dbTabItem.setCountSketchModelActors(String.valueOf(countSketchModelActors) + " / " + String.valueOf(totalSketchModelActors));
			if ( countSketchModelStickys != totalSketchModelStickys )								dbTabItem.setCountSketchModelStickys(String.valueOf(countSketchModelStickys) + " / " + String.valueOf(totalSketchModelStickys));

			if ( countDiagramModelBendpoints != totalDiagramModelBendpoints )						dbTabItem.setCountDiagramModelBendpoints(String.valueOf(countDiagramModelBendpoints) + " / " + String.valueOf(totalDiagramModelBendpoints));
			if ( countDiagramModelReferences != totalDiagramModelReferences )						dbTabItem.setCountDiagramModelReferences(String.valueOf(countDiagramModelReferences) + " / " + String.valueOf(totalDiagramModelReferences));

			if ( countImages != totalImages )														dbTabItem.setCountImages(String.valueOf(countImages) + " / " + String.valueOf(totalImages));
			
			dbTabItem.finish();			
		} catch (Exception e) {
			DBPlugin.popup(Level.Error, "An error occured while importing the model from the database.\n\nThe model imported into Archi might be incomplete !!!", e);
			
			dbTabItem.setText("An error occured while importing the model from the database.\nThe model imported into Archi might be incomplete, please check below : \n" + e.getClass().getSimpleName() + " : " + e.getMessage());
			
			if ( countMetadatas != dbModel.countMetadatas() )										dbTabItem.setCountMetadatas(String.valueOf(countMetadatas) + " / " + String.valueOf(dbModel.countMetadatas()));
			
			if ( countFolders != totalFolders )														dbTabItem.setCountFolders(String.valueOf(countFolders) + " / " + String.valueOf(totalFolders));
			if ( countElements != totalElements )													dbTabItem.setCountElements(String.valueOf(countElements) + " / " + String.valueOf(totalElements));
			if ( countRelationships != totalRelationships )											dbTabItem.setCountRelationships(String.valueOf(countRelationships) + " / " + String.valueOf(totalRelationships));
			if ( countProperties != totalProperties )												dbTabItem.setCountProperties(String.valueOf(countProperties) + " / " + String.valueOf(totalProperties));

			if ( countArchimateDiagramModels != totalArchimateDiagramModels )						dbTabItem.setCountArchimateDiagramModels(String.valueOf(countArchimateDiagramModels) + " / " + String.valueOf(totalArchimateDiagramModels));
			if ( countDiagramModelArchimateObjects != totalDiagramModelArchimateObjects )			dbTabItem.setCountDiagramModelArchimateObjects(String.valueOf(countDiagramModelArchimateObjects) + " / " + String.valueOf(totalDiagramModelArchimateObjects));
			if ( countDiagramModelArchimateConnections != totalDiagramModelArchimateConnections )	dbTabItem.setCountDiagramModelArchimateConnections(String.valueOf(countDiagramModelArchimateConnections) + " / " + String.valueOf(totalDiagramModelArchimateConnections));
			if ( countDiagramModelConnections != totalDiagramModelConnections )						dbTabItem.setCountDiagramModelConnections(String.valueOf(countDiagramModelConnections) + " / " + String.valueOf(totalDiagramModelConnections));

			if ( countDiagramModelGroups !=+ totalDiagramModelGroups )								dbTabItem.setCountDiagramModelGroups(String.valueOf(countDiagramModelGroups) + " / " + String.valueOf(totalDiagramModelGroups));
			if ( countDiagramModelNotes != totalDiagramModelNotes )									dbTabItem.setCountDiagramModelNotes(String.valueOf(countDiagramModelNotes) + " / " + String.valueOf(totalDiagramModelNotes));

			if ( countCanvasModels != totalCanvasModels )											dbTabItem.setCountCanvasModels(String.valueOf(countCanvasModels) + " / " + String.valueOf(totalCanvasModels));
			if ( countCanvasModelBlocks != totalCanvasModelBlocks )									dbTabItem.setCountCanvasModelBlocks(String.valueOf(countCanvasModelBlocks) + " / " + String.valueOf(totalCanvasModelBlocks));
			if ( countCanvasModelStickys != totalCanvasModelStickys )								dbTabItem.setCountCanvasModelStickys(String.valueOf(countCanvasModelStickys) + " / " + String.valueOf(totalCanvasModelStickys));
			if ( countCanvasModelConnections != totalCanvasModelConnections )						dbTabItem.setCountCanvasModelConnections(String.valueOf(countCanvasModelConnections) + " / " + String.valueOf(totalCanvasModelConnections));
			if ( countCanvasModelImages != totalCanvasModelImages )									dbTabItem.setCountCanvasModelImages(String.valueOf(countCanvasModelImages) + " / " + String.valueOf(totalCanvasModelImages));

			if ( countSketchModels != totalSketchModels )											dbTabItem.setCountSketchModels(String.valueOf(countSketchModels) + " / " + String.valueOf(totalSketchModels));
			if ( countSketchModelActors != totalSketchModelActors )									dbTabItem.setCountSketchModelActors(String.valueOf(countSketchModelActors) + " / " + String.valueOf(totalSketchModelActors));
			if ( countSketchModelStickys != totalSketchModelStickys )								dbTabItem.setCountSketchModelStickys(String.valueOf(countSketchModelStickys) + " / " + String.valueOf(totalSketchModelStickys));

			if ( countDiagramModelBendpoints != totalDiagramModelBendpoints )						dbTabItem.setCountDiagramModelBendpoints(String.valueOf(countDiagramModelBendpoints) + " / " + String.valueOf(totalDiagramModelBendpoints));
			if ( countDiagramModelReferences != totalDiagramModelReferences )						dbTabItem.setCountDiagramModelReferences(String.valueOf(countDiagramModelReferences) + " / " + String.valueOf(totalDiagramModelReferences));

			if ( countImages != totalImages )														dbTabItem.setCountImages(String.valueOf(countImages) + " / " + String.valueOf(totalImages));
			
			dbTabItem.finish();
		}
		dbProgress.finish();
		try { db.close(); } catch (Exception ee) {}
	}


	private void importArchimateElement(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, documentation, folder, name, type FROM archimateelement WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			dbTabItem.setCountElements(++countElements);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()); 
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFolder(result.getString("folder"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);
		}
		
		dbTabItem.setCountElements(countElements);
		dbTabItem.setProgressBar(countTotal);
	}

	private void importRelationship(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, documentation, name, source, target, type, folder FROM relationship WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			dbTabItem.setCountRelationships(++countRelationships);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));
			dbObject.setFolder(result.getString("folder"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);
		}
	}

	private void importArchimateDiagramModel(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, connectionroutertype, documentation, folder, name, type, viewpoint FROM archimatediagrammodel WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			dbTabItem.setCountArchimateDiagramModels(++countArchimateDiagramModels);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFolder(result.getString("folder"));
			dbObject.setConnectionRouterType(result.getInt("connectionroutertype"));
			dbObject.setViewpoint(result.getInt("viewpoint"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);
		}
	}
	private void importDiagramModelArchimateObject(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM diagrammodelarchimateobject WHERE model = ? AND version = ? ORDER BY indent, rank, parent", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class"))));

			//setting common properties
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
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
				dbTabItem.setCountDiagramModelArchimateObjects(++countDiagramModelArchimateObjects);
				dbTabItem.setProgressBar(++countTotal);
				break;
			case "DiagramModelGroup" :
				dbObject.setName(result.getString("name"));
				dbObject.setDocumentation(result.getString("documentation"));
				dbTabItem.setCountDiagramModelGroups(++countDiagramModelGroups);
				dbTabItem.setProgressBar(++countTotal);
				importProperties(dbObject);
				break;
			case "DiagramModelNote" :
				dbObject.setName(result.getString("name"));
				dbObject.setBorderType(result.getInt("bordertype"));
				dbObject.setContent(result.getString("content"));
				dbTabItem.setCountDiagramModelNotes(++countDiagramModelNotes);
				dbTabItem.setProgressBar(++countTotal);
				break;
			default : //should never be here
				DBPlugin.popup(Level.Error,"Don't know how to import objects of type " + result.getString("class"));					
			}

			_dbModel.indexDBObject(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);
		}
		result.close();
	}

	private void importConnections(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM connection WHERE model = ? AND version = ? ORDER BY parent, rank", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbSource = _dbModel.searchDBObjectById(DBPlugin.generateId(result.getString("source"), _dbModel.getProjectId(), _dbModel.getVersion()));
			if ( dbSource.getEObject() == null )
				System.out.println("Cannot found source "+DBPlugin.generateId(result.getString("source"), _dbModel.getProjectId(), _dbModel.getVersion()));

			DBObject dbTarget = _dbModel.searchDBObjectById(DBPlugin.generateId(result.getString("target"), _dbModel.getProjectId(), _dbModel.getVersion()));
			if ( dbTarget.getEObject() == null )
				System.out.println("Cannot found target "+DBPlugin.generateId(result.getString("target"), _dbModel.getProjectId(), _dbModel.getVersion()));

			DBObject dbObject;
			switch (result.getString("class")) {
			case "DiagramModelConnection" : 			dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createDiagramModelConnection());
														dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
														dbTabItem.setCountDiagramModelConnections(++countDiagramModelConnections);
														dbTabItem.setProgressBar(++countTotal);
														break;
			case "DiagramModelArchimateConnection" :	dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createDiagramModelArchimateConnection());
														dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
														dbObject.setRelationship(result.getString("relationship"));
														dbTabItem.setCountDiagramModelArchimateConnections(++countDiagramModelArchimateConnections);
														dbTabItem.setProgressBar(++countTotal);
														break;
			case "CanvasModelConnection" :				dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModelConnection());
														dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
														dbTabItem.setCountCanvasModelConnections(++countCanvasModelConnections);
														dbTabItem.setProgressBar(++countTotal);
														break;
			default :									DBPlugin.popup(Level.Error, "importConnection : do not know how to import " + result.getString("class"));
														return;
			}

			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));
			dbObject.setText(result.getString("text"));
			dbObject.setTextPosition(result.getInt("textposition"));
			dbObject.setType(result.getInt("type"));

			importBendpoints(dbObject);

			importProperties(dbObject);

			_dbModel.declareSourceConnection(result.getString("parent"), dbObject);

			_dbModel.indexDBObject(dbObject);
		}
		result.close();
	}

	private void importTargetConnections(DBModel _dbModel) throws SQLException {
		IDiagramModelConnection connection;
		ResultSet result = DBPlugin.select(db, "SELECT id, targetconnections FROM diagrammodelarchimateobject WHERE model = ? AND version = ? AND targetconnections IS NOT NULL", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = _dbModel.searchDBObjectById(DBPlugin.generateId(result.getString("id"), _dbModel.getProjectId(), _dbModel.getVersion()));
			for ( String target: result.getString("targetconnections").split(",")) {
				connection = (IDiagramModelConnection)_dbModel.searchEObjectById(DBPlugin.generateId(target, _dbModel.getProjectId(), _dbModel.getVersion()));
				if ( connection == null )
					System.out.println("Cannot found targetConnection "+DBPlugin.generateId(target, _dbModel.getProjectId(), _dbModel.getVersion()));
				else {
					dbObject.getTargetConnections().add(connection);
					//_dbModel.indexDBObject(dbObject);
				}
			}
		}
		result.close();
	}

	private void importCanvasModel(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodel WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModel());
			dbTabItem.setCountCanvasModels(++countCanvasModels);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()); 
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setHintContent(result.getString("hintcontent"));
			dbObject.setHintTitle(result.getString("hinttitle"));
			dbObject.setHintContent(result.getString("hintcontent"));
			dbObject.setConnectionRouterType(result.getInt("connectionroutertype"));
			dbObject.setFolder(result.getString("folder"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);
		}
	}

	private void importCanvasModelBlock(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodelblock WHERE model = ? AND version = ? ORDER BY parent, rank, indent", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModelBlock());
			dbTabItem.setCountCanvasModelBlocks(++countCanvasModelBlocks);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
			dbObject.setBorderColor(result.getString("bordercolor"));
			dbObject.setContent(result.getString("content"));
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setHintContent(result.getString("hintcontent"));
			dbObject.setHintTitle(result.getString("hinttitle"));
			dbObject.setImagePath(result.getString("imagepath"));			_dbModel.indexImagePath(result.getString("imagepath"));
			dbObject.setImagePosition(result.getInt("imageposition"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setTextPosition(result.getInt("textposition"));
			dbObject.setLocked(result.getBoolean("islocked"));

			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);
		}
	}

	private void importCanvasModelSticky(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodelsticky WHERE model = ? AND version = ? ORDER BY parent, rank, indent", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModelSticky());
			dbTabItem.setCountCanvasModelStickys(++countCanvasModelStickys);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
			dbObject.setBorderColor(result.getString("bordercolor"));
			dbObject.setContent(result.getString("content"));
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setImagePath(result.getString("imagepath"));		_dbModel.indexImagePath(result.getString("imagepath"));
			dbObject.setImagePosition(result.getInt("imageposition"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setNotes(result.getString("notes"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setTextPosition(result.getInt("textposition"));

			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);
		}
	}

	private void importCanvasModelImage(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM canvasmodelimage WHERE model = ? AND version = ? ORDER BY parent, rank, indent", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, ICanvasFactory.eINSTANCE.createCanvasModelImage());
			dbTabItem.setCountCanvasModelImages(++countCanvasModelImages);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
			dbObject.setBorderColor(result.getString("bordercolor"));
			dbObject.setLocked(result.getBoolean("islocked"));
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setImagePath(result.getString("imagepath"));				_dbModel.indexImagePath(result.getString("imagepath"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);
		}
	}

	private void importDiagramModelReference(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM DiagramModelReference WHERE model = ? AND version = ? ORDER BY parent, rank, indent", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createDiagramModelReference());
			dbTabItem.setCountDiagramModelReferences(++countDiagramModelReferences);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion());
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setTextAlignment(result.getInt("textalignment"));

			_dbModel.indexDBObject(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);
		}
	}


	private void importSketchModel(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM sketchmodel WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createSketchModel());
			dbTabItem.setCountSketchModels(++countSketchModels);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()); 
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setConnectionRouterType(result.getInt("connectionroutertype"));
			dbObject.setBackground(result.getInt("background"));

			dbObject.setFolder(result.getString("folder"));

			importProperties(dbObject);

			_dbModel.indexDBObject(dbObject);
		}
	}

	private void importSketchModelActor(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM sketchmodelactor WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createSketchModelActor());
			dbTabItem.setCountSketchModelActors(++countSketchModelActors);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()); 
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			importProperties(dbObject);

			_dbModel.indexDBObject(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);
		}
	}

	private void importSketchModelSticky(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM sketchmodelsticky WHERE model = ? AND version = ?", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createSketchModelSticky());
			dbTabItem.setCountSketchModelStickys(++countSketchModelStickys);
			dbTabItem.setProgressBar(++countTotal);
			
			dbObject.setId(result.getString("id"), _dbModel.getProjectId(),_dbModel.getVersion()); 
			dbObject.setContent(result.getString("content"));
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setName(result.getString("name"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("width"), result.getInt("height"));

			_dbModel.indexDBObject(dbObject);

			importProperties(dbObject);

			_dbModel.declareChild(result.getString("parent"), dbObject);

		}
	}

	private void importBendpoints(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT startx, starty, endx, endy FROM bendpoint WHERE parent = ? AND model = ? AND version = ? ORDER BY rank", _dbObject.getId(), _dbObject.getProjectId(), _dbObject.getVersion());
		while(result.next()) {
			IDiagramModelBendpoint bendpoint = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
			++countDiagramModelBendpoints;
			++countTotal;
			
			bendpoint.setStartX(result.getInt("startx"));
			bendpoint.setStartY(result.getInt("starty"));
			bendpoint.setEndX(result.getInt("endx"));
			bendpoint.setEndY(result.getInt("endy"));
			_dbObject.setBendpoint(bendpoint);
			_dbObject.getDBModel().indexEObject(bendpoint);
		}
		result.close();
		
		dbTabItem.setCountDiagramModelBendpoints(countDiagramModelBendpoints);
		dbTabItem.setProgressBar(countTotal);
	}

	private void importProperties(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT name, value FROM property WHERE parent = ? AND model = ? AND version = ? ORDER BY id", _dbObject.getId(), _dbObject.getProjectId(), _dbObject.getVersion());
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			prop.setKey(result.getString("name"));
			prop.setValue(result.getString("value"));
			_dbObject.getProperties().add(prop);
			_dbObject.getDBModel().indexEObject(prop);
			++countProperties;
			++countTotal;
		}
		result.close();
		
		if ( countProperties != 0 ) dbTabItem.setCountProperties(countProperties);
		dbTabItem.setProgressBar(countTotal);
	}
	
	private void importProperties(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT id, name, value FROM property WHERE parent = ? AND model = ? AND version = ? ORDER BY id", _dbModel.getProjectId(), _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			prop.setKey(result.getString("name"));
			prop.setValue(result.getString("value"));
			if ( result.getInt("id") < 0 ) {
				_dbModel.getMetadata().add(prop);
				++countMetadatas;
				_dbModel.indexMetadata();
			} else {
				_dbModel.getProperties().add(prop);
				++countProperties;
			}
			_dbModel.indexEObject(prop);
			++countTotal;
			
		}
		result.close();
		
		if ( countMetadatas != 0 ) dbTabItem.setCountMetadatas(countMetadatas);
		if ( countProperties != 0 ) dbTabItem.setCountProperties(countProperties);
		dbTabItem.setProgressBar(countTotal);
	}
	
	private void importFolders(DBModel _dbModel) throws SQLException {
		DBObject folder;
		ResultSet result = DBPlugin.select(db, "SELECT id, model, version, documentation, parent, type, name FROM folder WHERE model = ? AND version = ? ORDER BY rank", _dbModel.getProjectId(), _dbModel.getVersion());
		while(result.next()) {
			if ( result.getInt("type") != 0 ) {		// folders are first level folders, they've already been created.
				folder = new DBObject(_dbModel, _dbModel.getDefaultFolderForFolderType(FolderType.get(result.getInt("type"))));
				if ( folder.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "I do not find default folder for type "+result.getInt("type")+" ("+FolderType.get(result.getInt("type")).name()+")");
					return;
				}
			} else {
				folder = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.createFolder());
				folder.setName(result.getString("name"));
				//TODO: si parent == null, alors assigner à modele en standalone ou modelfolder en shared
				folder.setFolder(result.getString("parent"));
			}
			dbTabItem.setCountFolders(++countFolders);
			dbTabItem.setProgressBar(++countTotal);
			folder.setId(result.getString("id"), result.getString("model"), result.getString("version"));
			folder.setDocumentation(result.getString("documentation"));
			_dbModel.indexDBObject(folder);
			importProperties(folder);
		}
		result.close();
	}

	private void importImages(DBModel _dbModel) throws SQLException {
		// we import only required images
		for (String path: _dbModel.getImagePaths() ) {
			ResultSet result = DBPlugin.select(db, "SELECT path, image FROM images WHERE path = ?", path);
			if (result.next() ) {
				IArchiveManager archiveMgr = (IArchiveManager)_dbModel.getModel().getAdapter(IArchiveManager.class);
				try {
					String imagePath = archiveMgr.addByteContentEntry(result.getString("path"), result.getBytes("image"));
					if ( !imagePath.equals(result.getString("path")) ) {
						//TODO: the image was already in the cache but with a different path
						//TODO: we must search all the objects with "path" to replace it with "imagepath" 
					}
					dbTabItem.setCountImages(++countImages);
					dbTabItem.setProgressBar(++countTotal);
				} catch (Exception e) {
					DBPlugin.popup(Level.Error, "Failed to invoke reflection to set images !", e);
					return;
				}
			} else {
				DBPlugin.popup(Level.Error, "Unkwnown image path "+path);
			}
		}
	}
	
	static interface IByteArrayStorage {		
		void addByteContentEntry(String entryName, byte[] bytes);
	}
}
