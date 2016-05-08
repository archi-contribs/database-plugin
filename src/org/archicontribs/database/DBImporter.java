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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.editor.model.ISelectedModelImporter;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Import from Database
 * 
 * @author Hervé JOUIN
 */

//TODO: manage roles
//	admin : allow to manage system tables
//	architect : allow to create or update models
//	viewer : allow to read models
//TODO: manage several languages
public class DBImporter implements IModelImporter, ISelectedModelImporter {
	private Connection db;
	private int nbImported; //TODO: calculate more detailed statistics and check with values stored in the database
	private int nbElement;
	private int nbRelation;
	private int nbDiagram;
	private int nbProperty;
	private int nbBound;
	private int nbBendpoint;
	private int nbDiagramObject;
	private int nbConnection;
	private int nbDiagramGroup;
	private int nbDiagramNote;

	@Override
	public void doImport() throws IOException {
		doImport(null);
	}

	@Override
	public void doImport(IArchimateModel _model) throws IOException {
		if ( _model != null && ! MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, "Please be aware that, as you already have a model opened, you won't be able to save modifications done to this model.\n\rAre you sure you wish to import a new model ?") ) {
			return;
		}

		nbImported = 0; //TODO: calculate more detailed statistics and check with values stored in the database
		nbElement = 0;
		nbRelation = 0;
		nbDiagram = 0;
		nbProperty = 0;
		nbBound = 0;
		nbBendpoint = 0;
		nbDiagramObject = 0;
		nbConnection = 0;
		nbDiagramGroup = 0;
		nbDiagramNote = 0;

		db = new DBSelectDatabase().open();
		if ( db == null) return;

		HashMap<String, String> modelSelected;
		try {
			if ( (modelSelected = new DBSelectModel().open(db)) == null ) {
				try { db.close(); } catch (SQLException ee) { };
				return;
			}
		} catch (SQLException e) {
			DBPlugin.popup(Level.Error, "Failed to get the model list from the database.", e);
			try { db.close(); } catch (SQLException ee) {}
			return;
		}

		try {
			DBModel dbModel;
			if (_model != null ) {
				// we deny to import a model twice because we don't know which components have been modified and which not
				if ( modelSelected.get("id").equals(_model.getId()) ) {
					//TODO: check for all the models previously imported (not only the active one) ...
					DBPlugin.popup(Level.Error, "You cannot import again model.\n\nIn case you wish to discard your work and reimport the model from the database, you need to close the model and import it in a new one.");
					try { db.close(); } catch (Exception ee) {}
					return;
				}
				dbModel = new DBModel(_model);
			} else {
				dbModel = new DBModel(IEditorModelManager.INSTANCE.createNewModel());
				dbModel.getModel().getFolder(FolderType.DIAGRAMS).getElements().clear();
				dbModel.setId(modelSelected.get("id"), modelSelected.get("version"));
				dbModel.setName(modelSelected.get("name"));
				dbModel.setPurpose(modelSelected.get("purpose"));
				importProperties(dbModel);
			}

			// we import the model objects
			importArchimateElement(dbModel);
			importRelationship(dbModel);
			importArchimateDiagramModel(dbModel);
			importDiagramModelArchimateObject(dbModel);
			importSourceConnections(dbModel);
			importTargetConnections(dbModel);

			//TODO: import all the other types folders, images, etc ...)
			String msg = "Import done.";
			msg += "\n\n"+nbImported+" components imported in total :";
			msg += "\n     - "+nbElement+" elements";
			msg += "\n     - "+nbRelation+" relations";
			msg += "\n     - "+nbDiagram+" diagrams";
			msg += "\n     - "+nbDiagramGroup+" diagrams groups";
			msg += "\n     - "+nbDiagramObject+" diagram objects";
			msg += "\n     - "+nbDiagramNote+" diagram notes";
			msg += "\n     - "+nbConnection+" diagram connections";
			msg += "\n     - "+nbProperty+" properties";
			msg += "\n     - "+nbBound+" bounds";
			msg += "\n     - "+nbBendpoint+" bendpoints";
			DBPlugin.popup(Level.Info, msg);
		} catch (SQLException e) {
			DBPlugin.popup(Level.Error, "An error occured while importing the model from the database.\n\nThe model imported into Archi might be incomplete !!!", e);
			try { db.close(); } catch (Exception ee) {}
			return;
		}
	}


	private void importArchimateElement(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM ArchimateElement WHERE model = ? AND version = ?", _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			nbImported++;
			nbElement++;
			dbObject.setId(result.getString("id"), _dbModel.getId(),_dbModel.getVersion()); 
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			importProperties(dbObject);
			_dbModel.getDefaultFolderForElement(dbObject.getEObject()).getElements().add(dbObject.getEObject());
		}
	}

	private void importRelationship(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM Relationship WHERE model = ? AND version = ?", _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			nbImported++;
			nbRelation++;
			dbObject.setId(result.getString("id"), _dbModel.getId(),_dbModel.getVersion());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));

			importProperties(dbObject);
			_dbModel.getDefaultFolderForElement(dbObject.getEObject()).getElements().add(dbObject.getEObject());
		}
	}

	private void importArchimateDiagramModel(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT * FROM ArchimateDiagramModel WHERE model = ? AND version = ?", _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type"))));
			nbImported++;
			nbDiagram++;
			dbObject.setId(result.getString("id"), _dbModel.getId(),_dbModel.getVersion());
			dbObject.setName(result.getString("name"));
			dbObject.setDocumentation(result.getString("documentation"));

			importProperties(dbObject);
			_dbModel.getDefaultFolderForElement(dbObject.getEObject()).getElements().add(dbObject.getEObject());
		}
	}
	private void importDiagramModelArchimateObject(DBModel _dbModel) throws SQLException {
		DBObject dbParent = null;
		String oldParent = null;

		ResultSet result = DBPlugin.select(db, "SELECT * FROM DiagramModelArchimateObject WHERE model = ? AND version = ? ORDER BY indent, rank, parent", _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			if ( !result.getString("parent").equals(oldParent) ) {
				dbParent = new DBObject(_dbModel, ArchimateModelUtils.getObjectByID(_dbModel.getModel(),_dbModel.getVersionnedId(result.getString("parent"))));
				if ( dbParent.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "Cannot import object ("+result.getString("id").trim()+") as we do not know its parent ("+result.getString("parent").trim()+")");
					break;
				}
			}

			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class"))));
			nbImported++;

			//setting common properties
			dbObject.setId(result.getString("id"), _dbModel.getId(),_dbModel.getVersion());
			dbObject.setFillColor(result.getString("fillcolor"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setTextAlignment(result.getInt("textalignment"));
			importBounds(dbObject);

			//setting specific properties
			switch (result.getString("class")) {
			case "DiagramModelArchimateObject" :
				nbDiagramObject++;
				dbObject.setArchimateElement(ArchimateModelUtils.getObjectByID(_dbModel.getModel(), _dbModel.getVersionnedId(result.getString("archimateelement"))));
				dbObject.setType(result.getInt("type"));
				break;
			case "DiagramModelGroup" :
				dbObject.setName(result.getString("name"));
				dbObject.setDocumentation(result.getString("documentation"));
				importProperties(dbObject);
				nbDiagramGroup++;
				break;
			case "DiagramModelNote" :
				nbDiagramNote++;
				dbObject.setName(result.getString("name"));
				dbObject.setBorderType(result.getInt("bordertype"));
				dbObject.setContent(result.getString("content"));
				break;
			default : //should never be here
				DBPlugin.popup(Level.Error,"Don't know how to import objects of type " + result.getString("class"));					
			}
			dbParent.addChild(dbObject);
		}
		result.close();
	}

	private void importSourceConnections(DBModel _dbModel) throws SQLException {
		DBObject dbParent = null;
		String oldParent = null;

		ResultSet result = DBPlugin.select(db, "SELECT * FROM DiagramModelArchimateConnection WHERE model = ? AND version = ? ORDER BY parent, rank", _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			if ( !result.getString("parent").equals(oldParent) ) {
				dbParent = new DBObject(_dbModel, ArchimateModelUtils.getObjectByID(_dbModel.getModel(),_dbModel.getVersionnedId(result.getString("parent"))));
				if ( dbParent.getEObject() == null ) {
					DBPlugin.popup(Level.Error, "Cannot import connection ("+result.getString("id").trim()+") as we do not know its parent ("+result.getString("parent").trim()+")");
					break;
				}
			}
			DBObject dbSource = new DBObject(_dbModel, ArchimateModelUtils.getObjectByID(_dbModel.getModel(), _dbModel.getVersionnedId(result.getString("source"))));
			if ( dbSource.getEObject() == null )
				DBPlugin.popup(Level.Error,  "Cannot found source "+result.getString("source"));
			DBObject dbTarget = new DBObject(_dbModel, ArchimateModelUtils.getObjectByID(_dbModel.getModel(), _dbModel.getVersionnedId(result.getString("target"))));
			if ( dbTarget.getEObject() == null )
				DBPlugin.popup(Level.Error,  "Cannot found target "+result.getString("target"));
			DBObject dbObject = new DBObject(_dbModel, IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("class"))));
			nbImported++;
			nbConnection++;
			dbObject.setId(result.getString("id"), _dbModel.getId(),_dbModel.getVersion());
			dbObject.setDocumentation(result.getString("documentation"));
			dbObject.setFont(result.getString("font"));
			dbObject.setFontColor(result.getString("fontcolor"));
			dbObject.setLineColor(result.getString("linecolor"));
			dbObject.setSource(result.getString("source"));
			dbObject.setTarget(result.getString("target"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setText(result.getString("text"));
			dbObject.setLineWidth(result.getInt("linewidth"));
			dbObject.setText(result.getString("text"));
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
		ResultSet result = DBPlugin.select(db, "SELECT id, targetconnections FROM DiagramModelArchimateObject WHERE model = ? AND version = ? AND targetconnections IS NOT NULL", _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			DBObject dbObject = new DBObject(_dbModel, ArchimateModelUtils.getObjectByID(_dbModel.getModel(), _dbModel.getVersionnedId(result.getString("id"))));
			for ( String target: result.getString("targetconnections").split(",")) {
				if ( ArchimateModelUtils.getObjectByID(_dbModel.getModel(), _dbModel.getVersionnedId(target)) == null )
					DBPlugin.popup(Level.Error, "Cannot found targetConnection "+target);
				else {
					dbObject.getTargetConnections().add((IDiagramModelConnection)ArchimateModelUtils.getObjectByID(_dbModel.getModel(), _dbModel.getVersionnedId(target)));
					nbImported++;
					nbConnection++;
				}
			}
		}
		result.close();
	}

	private void importBendpoints(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT x, y, w, h FROM Point WHERE parent = ? AND model = ? AND version = ? ORDER BY rank", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion());
		while(result.next()) {
			_dbObject.setBendpoint(result.getInt("x"), result.getInt("y"), result.getInt("w"), result.getInt("h"));
			nbImported++;
			nbBendpoint++;
		}
		result.close();
	}

	private void importBounds(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT x, y, w, h FROM Point WHERE parent = ? AND model = ? AND version = ? ORDER BY rank", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion());
		while(result.next()) {
			_dbObject.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("w"), result.getInt("h"));
			nbImported++;
			nbBound++;
		}
		result.close();
	}

	private void importProperties(DBObject _dbObject) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT name, value FROM Property WHERE parent = ? AND model = ? AND version = ?", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion());
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			nbImported++;
			nbProperty++;
			prop.setKey(result.getString("name").trim());
			prop.setValue(result.getString("value").trim());
			_dbObject.getProperties().add(prop);
		}
		result.close();
	}
	private void importProperties(DBModel _dbModel) throws SQLException {
		ResultSet result = DBPlugin.select(db, "SELECT name, value FROM Property WHERE parent = ? AND model = ? AND version = ?", _dbModel.getId(), _dbModel.getId(), _dbModel.getVersion());
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			nbImported++;
			nbProperty++;
			prop.setKey(result.getString("name").trim());
			prop.setValue(result.getString("value").trim());
			_dbModel.getProperties().add(prop);
		}
		result.close();
	}
}
