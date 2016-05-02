/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;


import static java.util.Arrays.asList;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.IRelationship;


/**
 * Database Model Exporter
 * 
 * @author Herve Jouin
 */
public class DBExporter implements IModelExporter {
	private Connection db;
	private int nbExported;	//TODO: calculate more detailed statistics and save them in the database
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
	public void export(IArchimateModel _model) throws IOException {
		DBModel dbModel = new DBModel(_model);
		String oldId = dbModel.getId();
		nbExported = 0;
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
		
		if ( (db = new DBSelectDatabase().open()) == null)
			return;

		HashMap<String, String> modelSelected;
		try {
			if ( (modelSelected = new DBSelectModel().open(db, dbModel)) == null ) {
				try { db.close(); } catch (SQLException ee) { };
				return;
			}
		} catch (SQLException e) {
			DBPlugin.popup(Level.Error, "Failed to get the model list from the database.", e);
			try { db.close(); } catch (SQLException ee) {}
			return;
		}

		try {
			// if the model already exists in the database, we ask the user to confirm the replacement
			ResultSet res = DBPlugin.select(db, "SELECT * FROM Model WHERE model = ? AND version = ?", modelSelected.get("id"), modelSelected.get("version"));
			if ( res.next() ) {
				if ( !MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, "You're about to replace the existing model "+modelSelected.get("name")+" ("+modelSelected.get("id")+") version "+modelSelected.get("version")+" in the database.\n\nAre you sure ?") ) {
					try { res.close(); } catch (SQLException ee) {}
					try { db.close(); } catch (SQLException ee) {}
					return;
				}
			}
			try { res.close(); } catch (SQLException ee) {}

			// we change the Id, version, name and purpose in case they've been changed in the ChooseModel window 
			dbModel.setId(modelSelected.get("id"), modelSelected.get("version"));
			dbModel.setName(modelSelected.get("name"));
			dbModel.setPurpose(modelSelected.get("purpose"));

			// we set (or change) the id and the version of all model components
			DBObject dbObject;
			for(Iterator<EObject> iter = _model.eAllContents(); iter.hasNext();) {
				dbObject = new DBObject(dbModel, iter.next());
				if ( dbObject.getModelId() == null || oldId.equals(dbObject.getModelId()) ) 
					dbObject.setId(dbObject.getId(), modelSelected.get("id"), modelSelected.get("version"));
			}

			// we remove the old components (if any) from the database
			for(String table: asList("Model", "ArchimateDiagramModel", "ArchimateElement", "DiagramModelArchimateConnection", "DiagramModelArchimateObject", /*"DiagramModelGroup", "DiagramModelNote", */"Relationship", "Point", "Property"))
				DBPlugin.sql(db, "DELETE FROM "+table+" WHERE model = ? AND version = ?", modelSelected.get("id"), modelSelected.get("version"));

			// we save the model itself
			DBPlugin.update(db, "INSERT INTO Model (model, version, name, purpose, owner, period, note)", modelSelected.get("id"), modelSelected.get("version"), modelSelected.get("name"), modelSelected.get("purpose"), modelSelected.get("owner"), modelSelected.get("period"), modelSelected.get("note"));
			exportProperties(dbModel);

			// we save the components
			Set<String> ignored = new HashSet<String>();
			EObject eObject;
			for(Iterator<EObject> iter = _model.eAllContents(); iter.hasNext();) {
				eObject = iter.next();
				dbObject = new DBObject(dbModel, eObject);
				// we save only the active model components 
				if ( modelSelected.get("id").equals(dbObject.getModelId()) ) {
					String type = eObject.eClass().getName();
					switch ( type ) {
					case "Property" :						break; //ignored as will be exported by their parents
					case "DiagramModelArchimateObject" :	break; //ignored as will be exported by ArchimateDiagramModel
					case "DiagramModelArchimateConnection" :break; //ignored as will be exported by ArchimateDiagramModel
					case "DiagramModelConnection" :			break; //ignored as will be exported by ArchimateDiagramModel
					case "DiagramModelGroup" :				break; //ignored as will be exported by ArchimateDiagramModel
					case "DiagramModelBendpoint" :			break; //ignored as will be exported by ArchimateDiagramModel
					case "DiagramModelNote" :				break; //ignored as will be exported by ArchimateDiagramModel
					case "Bounds" :							break; //ignored as will be exported by DiagramModelArchimateObject


					case "Folder" :




					case "SketchModel" :
					case "SketchModelSticky" :
					case "SketchModelActor" :

					case "CanvasModel" :
					case "CanvasModelImage" :
					case "CanvasModelConnection" :
					case "CanvasModelBlock" :
					case "CanvasModelSticky" :
						ignored.add(type);
						break ;

					case "ArchimateDiagramModel" :
						DBPlugin.update(db, "INSERT INTO ArchimateDiagramModel (id, model, version, name, documentation, connectionroutertype, viewpoint, type)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getDocumentation(), dbObject.getConnectionRouterType(), dbObject.getViewpoint(), dbObject.getClassSimpleName());
						nbExported++;
						nbDiagram++;
						exportProperties(dbObject);

						for ( int rank=0; rank < dbObject.getChildren().size(); rank++ ) {
							DBObject dbChild = dbObject.getChild(rank);
							switch ( dbChild.getEClassName() ) {
							case "DiagramModelArchimateConnection" :
								exportDiagramModelArchimateConnection(dbObject.getId(), dbChild, rank);
								break;
							case "DiagramModelArchimateObject" :
							case "DiagramModelGroup" :
							case "DiagramModelNote" :
								exportDiagramModelArchimateObject(dbObject.getId(), dbChild, rank, 0);
								break;
							default : //should never be here
								DBPlugin.popup(Level.Error,"Don't know how to save " + dbChild.getName() + " (" + dbChild.getEClassName() + ")");
							}
						}
						break;

					default:
						if ( eObject instanceof IArchimateElement ) {
							DBPlugin.update(db, "INSERT INTO ArchimateElement (id, model, version, name, type, documentation)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getClassSimpleName(), dbObject.getDocumentation()); 
							nbExported++;
							nbElement++;
							exportProperties(dbObject);
						}
						else {
							if ( eObject instanceof IRelationship ) {
								DBPlugin.update(db, "INSERT INTO Relationship (id, model, version, name, source, target, type, documentation)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getSourceId(), dbObject.getTargetId(), dbObject.getClassSimpleName(), dbObject.getDocumentation()); 
								nbExported++;
								nbRelation++;
								exportProperties(dbObject);
							}
							else {
								ignored.add(type);
							}
						}
					}
				}
			}

			String msg;
			if ( !ignored.isEmpty() ) {
				msg = "Model exported to the database, but not entirely.\n\nThis plugin is not able to export the following objects :";
				for(Object i : ignored)
					msg += "\n     - " + (String)i;
			} else {
				msg = "Model exported to the database ("+nbExported+" components exported).";	
			}
			msg += "\n\n"+nbExported+" components exported in total :";
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
			DBPlugin.popup(ignored.isEmpty() ? Level.Info : Level.Warning, msg);

			db.commit();
			// we remove the 'dirty' flag i.e. we consider the model as saved
			CommandStack stack = (CommandStack)_model.getAdapter(CommandStack.class);
			stack.markSaveLocation();
		} catch (Exception e) {
			DBPlugin.popup(Level.Error, "An error occured while exporting your model to the database.\n\nThe transaction has been rolled back and the database is left unmodified.", e);
			try { db.rollback(); } catch (Exception ee) {}
		}
		try { db.close(); } catch (SQLException e) {}
	}
	
	private void exportProperties(DBObject _dbObject) throws SQLException {
		if ( _dbObject.getProperties() != null ) {
			for(IProperty property: _dbObject.getProperties() ) {
				DBPlugin.update(db, "INSERT INTO property (parent, model, version, name, value)", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion(), property.getKey(), property.getValue());
				nbExported++;
				nbProperty++;
			}
		}
	}
	private void exportProperties(DBModel _dbModel) throws SQLException {
		if ( _dbModel.getProperties() != null ) {
			for(IProperty property: _dbModel.getProperties() ) {
				DBPlugin.update(db, "INSERT INTO property (parent, model, version, name, value)", _dbModel.getId(), _dbModel.getId(), _dbModel.getVersion(), property.getKey(), property.getValue());
				nbExported++;
				nbProperty++;
			}
		}
	}
	private void exportDiagramModelArchimateObject(String _parentId, DBObject _archimateObject, int _rank, int _indent) throws SQLException {
		String targetConnections = _archimateObject.getTargetConnectionsString();
		//we specify all the fields in the INSERT request as the DBObject return null values if not set (but does not trigger an exception)
		DBPlugin.update(db, "INSERT INTO DiagramModelArchimateObject (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, textAlignment, archimateelement, targetconnections, rank, indent, type, class, bordertype, content, documentation, name)",
				_archimateObject.getId(), _archimateObject.getModelId(), _archimateObject.getVersion(), _parentId, _archimateObject.getFillColor(), _archimateObject.getFont(), _archimateObject.getFontColor(), _archimateObject.getLineColor(), _archimateObject.getLineWidth(), _archimateObject.getTextAlignment(), _archimateObject.getArchimateElementId(), targetConnections, _rank, _indent, _archimateObject.getType(), _archimateObject.getEClassName(), _archimateObject.getBorderType(), _archimateObject.getContent(), _archimateObject.getDocumentation(), _archimateObject.getName());
		nbExported++;
		nbDiagramObject++;
		if ( targetConnections != null ) {
			int nb = targetConnections.split(",").length;
			nbExported += nb;
			nbConnection += nb;
		}
		if ( _archimateObject.getSourceConnections() != null ) {
			for ( int i=0; i < _archimateObject.getSourceConnections().size(); i++) {
				exportDiagramModelArchimateConnection(_archimateObject.getId(), _archimateObject.getSourceConnection(i), i);
			}
		}
		if ( _archimateObject.getChildren() != null ) {
			for ( int i=0; i < _archimateObject.getChildren().size(); i++) {
				exportDiagramModelArchimateObject(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1);
			}
		}
		exportBounds(_archimateObject);
		exportProperties(_archimateObject);
	}
	private void exportDiagramModelArchimateConnection(String _parentId, DBObject _connection, int _rank) throws SQLException {
		DBPlugin.update(db, "INSERT INTO DiagramModelArchimateConnection (id, model, version, parent, documentation, font, fontcolor, linecolor, linewidth, relationship, source, target, text, textposition, type, rank, class)", _connection.getId(), _connection.getModelId(), _connection.getVersion(), _parentId, _connection.getDocumentation(), _connection.getFont(), _connection.getFontColor(), _connection.getLineColor(), _connection.getLineWidth(), _connection.getRelationshipId(), _connection.getSourceId(), _connection.getTargetId(), _connection.getText(), _connection.getTextPosition(),	_connection.getType(), _rank, _connection.getEClassName());
		nbExported++;
		nbConnection++;
		exportBendpoints(_connection);
		exportProperties(_connection);
	}
	private void exportBounds(DBObject _dbObject) throws SQLException {
		IBounds bounds = _dbObject.getBounds();
		DBPlugin.update(db, "INSERT INTO Point (parent, model, version, x, y, w, h, rank)", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion(), bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 0);
		nbExported++;
		nbBound++;
	}
	private void exportBendpoints(DBObject _dbObject) throws SQLException {
		int rank=0;
		for ( IDiagramModelBendpoint point: _dbObject.getBendpoints() ) {
			DBPlugin.update(db, "INSERT INTO Point (parent, model, version, x, y, w, h, rank)", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion(), point.getStartX(), point.getStartY(), point.getEndX(), point.getEndY(), rank++);
			nbExported++;
			nbBendpoint++;
		}
	}
}
