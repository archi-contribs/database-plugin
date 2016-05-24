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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IFolder;
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
	private int nbBendpoint;
	private int nbDiagramObject;
	private int nbConnection;
	private int nbCanvas;
	private int nbCanvasModelBlock;
	private int nbCanvasModelSticky;
	private int nbFolder;

	DBList selectedModels;
	DBModel dbModel;
	String oldId;

	@Override
	public void export(IArchimateModel _model) throws IOException {
		nbExported = 0;
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

		dbModel = new DBModel(_model);
		oldId = dbModel.getProjectId();

		//TODO : check if there are components outside projects folders as they will not be exported !!!

		if ( (db = new DBSelectDatabase().open()) == null)
			return;

		try {
			selectedModels = new DBSelectModel().open(db, dbModel);
			if ( selectedModels == null || selectedModels.size() == 0 ) {
				try { db.close(); } catch (SQLException ee) { };
				return;
			}
		} catch (SQLException e) {
			DBPlugin.popup(Level.Error, "Failed to get the model list from the database.", e);
			try { db.close(); } catch (SQLException ee) {}
			return;
		}

		BusyIndicator.showWhile(Display.getDefault(), new Runnable(){
			public void run(){
				try {
					for ( HashMap<String, String> modelSelected: selectedModels.values() ) {
						// if the model already exists in the database, we ask the user to confirm the replacement
						ResultSet res = DBPlugin.select(db, "SELECT * FROM model WHERE model = ? AND version = ?", modelSelected.get("id"), modelSelected.get("version"));
						if ( res.next() ) {
							if ( !MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, "You're about to replace the existing model "+modelSelected.get("name")+" ("+modelSelected.get("id")+") version "+modelSelected.get("version")+" in the database.\n\nAre you sure ?") ) {
								try { res.close(); } catch (SQLException ee) {}
								continue;
							}
						}
						try { res.close(); } catch (SQLException ee) {}

						// if the model is the shared one, then we search for the folder to export
						// TODO: ATTENTION aux folders créés à la main et qui n'ont pas de modelId
						// TODO:
						// TODO:
						// TODO:
						// TODO:
						// TODO:
						if ( dbModel.isShared() ) {
							DBPlugin.debug("Model is shared ... setting model's folder by ID = " + modelSelected.get("id"));
							if ( dbModel.setProjectFolder(dbModel.searchProjectFolderById(modelSelected.get("id"))) == null ) {
								DBPlugin.debug("   Not found !!! setting model's folder by name = " + modelSelected.get("name"));
								if ( dbModel.setProjectFolder(dbModel.searchProjectFolderByName(modelSelected.get("name"))) == null ) {
									DBPlugin.popup(Level.Error, "Thats weird ...\n\nI do not know how to export the model \""+modelSelected.get("name")+"\".");
									try { res.close(); } catch (SQLException ee) {}
									continue;
								}
							}
						}

						// we change the Id, version, name and purpose in case they've been changed in the ChooseModel window
						// TODO: ATTENTION aux folders créés à la main et qui n'ont pas de modelId
						// TODO:
						// TODO:
						// TODO:
						// TODO:
						// TODO:
						dbModel.setProjectId(modelSelected.get("id"), modelSelected.get("version"));
						dbModel.setName(modelSelected.get("name"));
						dbModel.setPurpose(modelSelected.get("purpose"));

						// we set (or change) the id and the version of all components inside the corresponding folders
						DBObject dbObject;
						DBPlugin.debug("Setting ID and version to all components inside project folders ...");
						for (IFolder f: dbModel.getFolders() ) {
							DBPlugin.debug("   folder " + f.getName());
							dbObject = new DBObject(dbModel, f);
							dbObject.setId(dbObject.getId(), modelSelected.get("id"), modelSelected.get("version"));
							for(Iterator<EObject> iter = f.eAllContents(); iter.hasNext();) {
								dbObject = new DBObject(dbModel, iter.next());
								dbObject.setId(dbObject.getId(), modelSelected.get("id"), modelSelected.get("version"));
							}
						}

						// we remove the old components (if any) from the database
						DBPlugin.debug("Removing old values ...");
						for(String table: DBPlugin.allTables ) {
							DBPlugin.debug("   table " + table);
							DBPlugin.sql(db, "DELETE FROM "+table+" WHERE model = ? AND version = ?", modelSelected.get("id"), modelSelected.get("version"));
						}
						
						// we save the model itself
						DBPlugin.debug("Exportint model metadata ...");
						DBPlugin.update(db, "INSERT INTO model (model, version, name, purpose, owner, period, note)", modelSelected.get("id"), modelSelected.get("version"), modelSelected.get("name"), modelSelected.get("purpose"), modelSelected.get("owner"), modelSelected.get("period"), modelSelected.get("note"));
						exportProperties(dbModel);

						// we save the folders
						DBPlugin.debug("Exporting folders ...");
						for (IFolder f: dbModel.getFolders() ) 
							exportFolders(new DBObject(dbModel, f), null, 0);

						// we save the components
						DBPlugin.debug("Exprorting components ...");
						Set<String> ignored = new HashSet<String>();
						EObject eObject;
						for (IFolder f: dbModel.getFolders() ) {
							for(Iterator<EObject> iter = f.eAllContents(); iter.hasNext();) {
								eObject = iter.next();
								dbObject = new DBObject(dbModel, eObject);
								// we save only the active model components
								if ( modelSelected.get("id").equals(dbObject.getModelId()) ) {
									String type = eObject.eClass().getName();
									switch ( type ) {
									case "Folder" :							break; // exported separately
									case "Property" :						break; // exported by their parents
									case "DiagramModelArchimateObject" :	break; // exported by ArchimateDiagramModel
									case "DiagramModelArchimateConnection" :break; // exported by ArchimateDiagramModel
									case "DiagramModelConnection" :			break; // exported by ArchimateDiagramModel
									case "DiagramModelGroup" :				break; // exported by ArchimateDiagramModel
									case "DiagramModelBendpoint" :			break; // exported by ArchimateDiagramModel
									case "DiagramModelNote" :				break; // exported by ArchimateDiagramModel
									case "Bounds" :							break; // exported by DiagramModelArchimateObject
									case "CanvasModelBlock" :				break; // exported by CanvasModel
									case "CanvasModelSticky" :				break; // exported by CanvasModel

									case "SketchModel" :
									case "SketchModelSticky" :
									case "SketchModelActor" :
									case "CanvasModelImage" :
									case "CanvasModelConnection" :
										ignored.add(type);
										break ;

									case "CanvasModel" :
										DBPlugin.update(db, "INSERT INTO canvasmodel (id, model, version, name, documentation, hinttitle, hintcontent, connectionroutertype, folder)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getDocumentation(), dbObject.getHintTitle(), dbObject.getHintContent(), dbObject.getConnectionRouterType(), dbObject.getFolder());
										++nbCanvas;
										exportProperties(dbObject);

										for ( int rank=0; rank < dbObject.getChildren().size(); ++rank ) {
											DBObject dbChild = dbObject.getChild(rank);
											switch ( dbChild.getEClassName() ) {
											case "CanvasModelBlock" :
												exportCanvasModelBlock(dbObject.getId(), dbChild, rank, 0);
												break;
											case "CanvasModelSticky" :
												exportCanvasModelSticky(dbObject.getId(), dbChild, rank, 0);
												break;
											default :  //should never be here
												DBPlugin.popup(Level.Error,"Don't know how to save " + dbChild.getName() + " (" + dbChild.getEClassName() + ")");
											}

										}
										break;

									case "ArchimateDiagramModel" :
										DBPlugin.update(db, "INSERT INTO archimatediagrammodel (id, model, version, name, documentation, connectionroutertype, viewpoint, type, folder)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getDocumentation(), dbObject.getConnectionRouterType(), dbObject.getViewpoint(), dbObject.getClassSimpleName(), dbObject.getFolder());
										++nbDiagram;
										exportProperties(dbObject);

										for ( int rank=0; rank < dbObject.getChildren().size(); ++rank ) {
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
											DBPlugin.update(db, "INSERT INTO archimateelement (id, model, version, name, type, documentation, folder)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getClassSimpleName(), dbObject.getDocumentation(), dbObject.getFolder()); 
											++nbElement;
											exportProperties(dbObject);
										}
										else {
											if ( eObject instanceof IRelationship ) {
												DBPlugin.update(db, "INSERT INTO relationship (id, model, version, name, source, target, type, documentation, folder)", dbObject.getId(), dbObject.getModelId(), dbObject.getVersion(), dbObject.getName(), dbObject.getSourceId(), dbObject.getTargetId(), dbObject.getClassSimpleName(), dbObject.getDocumentation(), dbObject.getFolder()); 
												++nbRelation;
												exportProperties(dbObject);
											}
											else {
												ignored.add(type);
											}
										}
									}
								}
							}
						}

						String msg;
						if ( !ignored.isEmpty() ) {
							msg = "The model \"" + dbModel.getName() + "\" has been exported to the database, but not entirely.\n\nThis plugin is not able to export the following objects :";
							for(Object i : ignored)
								msg += "\n     - " + (String)i;
						} else {
							msg = "The model \"" + dbModel.getName() + "\" has been successfully exported to the database.";	
						}
						nbExported = nbElement+nbRelation+nbDiagram+nbDiagramObject+nbConnection+nbCanvas+nbCanvasModelBlock+nbCanvasModelSticky+nbProperty+nbBendpoint+nbFolder;
						msg += "\n\n"+nbExported+" components exported in total :";
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
						DBPlugin.popup(ignored.isEmpty() ? Level.Info : Level.Warning, msg);
					}
					//TODO: commit each project ...
					db.commit();
					// we remove the 'dirty' flag i.e. we consider the model as saved
					CommandStack stack = (CommandStack)dbModel.getModel().getAdapter(CommandStack.class);
					stack.markSaveLocation();
				} catch (SQLException e) {
					DBPlugin.popup(Level.Error, "An error occured while exporting your model to the database.\n\nThe transaction has been rolled back and the database is left unmodified.", e);
					System.err.println(e.getStackTrace());
					try { db.rollback(); } catch (Exception ee) {}
				}
			}
		});
		try { db.close(); } catch (SQLException e) {}
	}

	private void exportProperties(DBObject _dbObject) throws SQLException {
		// exports IProperty objects
		if ( _dbObject.getProperties() != null ) {
			int id=0;
			for(IProperty property: _dbObject.getProperties() ) {
				DBPlugin.update(db, "INSERT INTO property (id, parent, model, version, name, value)", ++id, _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion(), property.getKey(), property.getValue());
				++nbProperty;
			}
		}
	}
	private void exportProperties(DBModel _dbModel) throws SQLException {
		// exports IProperty objects
		if ( _dbModel.getProperties() != null ) {
			int rank=0;
			for(IProperty property: _dbModel.getProperties() ) {
				DBPlugin.update(db, "INSERT INTO property (id, parent, model, version, name, value)", ++rank, _dbModel.getProjectId(), _dbModel.getProjectId(), _dbModel.getVersion(), property.getKey(), property.getValue());
				++nbProperty;
			}
		}
	}
	private void exportDiagramModelArchimateObject(String _parentId, DBObject _archimateObject, int _rank, int _indent) throws SQLException {
		//exports IDiagramModelArchimateObject + IDiagramModelObject + IDiagramModelGroup + IDiagramModelNote objects
		String targetConnections = _archimateObject.getTargetConnectionsString();
		//we specify all the fields in the INSERT request as the DBObject return null values if not set (but does not trigger an exception)
		DBPlugin.update(db, "INSERT INTO diagrammodelarchimateobject (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, textAlignment, archimateelementid, archimateelementid, archimateelementclass, targetconnections, rank, indent, type, class, bordertype, content, documentation, name, x, y, width, height)",
				_archimateObject.getId(), _archimateObject.getModelId(), _archimateObject.getVersion(), _parentId, _archimateObject.getFillColor(), _archimateObject.getFont(), _archimateObject.getFontColor(), _archimateObject.getLineColor(), _archimateObject.getLineWidth(), _archimateObject.getTextAlignment(),
				_archimateObject.getArchimateElementId(),
				_archimateObject.getArchimateElementName(),
				_archimateObject.getArchimateElementClass(),
				targetConnections, _rank, _indent, _archimateObject.getType(), _archimateObject.getEClassName(), _archimateObject.getBorderType(), _archimateObject.getContent(), _archimateObject.getDocumentation(), _archimateObject.getName(), _archimateObject.getBounds().getX(), _archimateObject.getBounds().getY(), _archimateObject.getBounds().getWidth(), _archimateObject.getBounds().getHeight());
		++nbDiagramObject;
		if ( targetConnections != null ) {
			int nb = targetConnections.split(",").length;
			nbExported += nb;
			nbConnection += nb;
		}
		if ( _archimateObject.getSourceConnections() != null ) {
			for ( int i=0; i < _archimateObject.getSourceConnections().size(); ++i) {
				exportDiagramModelArchimateConnection(_archimateObject.getId(), _archimateObject.getSourceConnection(i), i);
			}
		}
		if ( _archimateObject.getChildren() != null ) {
			for ( int i=0; i < _archimateObject.getChildren().size(); ++i) {
				exportDiagramModelArchimateObject(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1);
			}
		}
		exportProperties(_archimateObject);
	}
	private void exportDiagramModelArchimateConnection(String _parentId, DBObject _connection, int _rank) throws SQLException {
		//exports IDiagramModelArchimateConnection objects
		DBPlugin.update(db, "INSERT INTO diagrammodelarchimateconnection (id, model, version, parent, documentation, font, fontcolor, linecolor, linewidth, relationship, source, target, text, textposition, type, rank, class)", _connection.getId(), _connection.getModelId(), _connection.getVersion(), _parentId, _connection.getDocumentation(), _connection.getFont(), _connection.getFontColor(), _connection.getLineColor(), _connection.getLineWidth(), _connection.getRelationshipId(), _connection.getSourceId(), _connection.getTargetId(), _connection.getText(), _connection.getTextPosition(),	_connection.getType(), _rank, _connection.getEClassName());
		++nbConnection;
		exportBendpoints(_connection);
		exportProperties(_connection);
	}

	private void exportCanvasModelBlock(String _parentId, DBObject _block, int _rank, int _indent) throws SQLException {
		//export ICanvasModelBlock objects
		DBPlugin.update(db, "INSERT INTO canvasmodelblock (id, model, version, parent, bordercolor, content, fillcolor, font, fontcolor, hintcontent, hinttitle, imagepath, imageposition, linecolor, linewidth, islocked, name, textalignment, textposition, rank, indent, x, y, width, height)",
				_block.getId(), _block.getModelId(), _block.getVersion(), _parentId, _block.getBorderColor(), _block.getContent(), _block.getFillColor(), _block.getFont(), _block.getFontColor(),
				_block.getHintContent(), _block.getHintTitle(), _block.getImagePath(), _block.getImagePosition(), _block.getLineColor(), _block.getLineWidth(), _block.isLocked(), _block.getName(), _block.getTextAlignment(), _block.getTextPosition(), _rank, _indent, _block.getBounds().getX(), _block.getBounds().getY(), _block.getBounds().getWidth(), _block.getBounds().getHeight());
		++nbCanvasModelBlock;
		if ( _block.getChildren() != null ) {
			for ( int i=0; i < _block.getChildren().size(); ++i) {
				exportDiagramModelArchimateObject(_block.getId(), _block.getChild(i), i, _indent+1);
			}
		}
		exportProperties(_block);
	}
	private void exportCanvasModelSticky(String _parentId, DBObject _sticky, int _rank, int _indent) throws SQLException {
		//export ICanvasModelSticky objects
		DBPlugin.update(db, "INSERT INTO canvasmodelsticky (id, model, version, parent, bordercolor, content, fillcolor, font, fontcolor, imagepath, imageposition, linecolor, linewidth, notes, name, source, target, textalignment, textposition, rank, indent, x, y, width, height)",
				_sticky.getId(), _sticky.getModelId(), _sticky.getVersion(), _parentId, _sticky.getBorderColor(), _sticky.getContent(), _sticky.getFillColor(), _sticky.getFont(), _sticky.getFontColor(),
				_sticky.getImagePath(), _sticky.getImagePosition(), _sticky.getLineColor(), _sticky.getLineWidth(), _sticky.getNotes(), _sticky.getName(),  _sticky.getSourceId(), _sticky.getTargetId(), _sticky.getTextAlignment(), _sticky.getTextPosition(), _rank, _indent, _sticky.getBounds().getX(), _sticky.getBounds().getY(), _sticky.getBounds().getWidth(), _sticky.getBounds().getHeight());
		++nbCanvasModelSticky;
		exportProperties(_sticky);
	}
	private void exportBendpoints(DBObject _dbObject) throws SQLException {
		//export IDiagramModelBendpoint objects
		int rank=0;
		for ( IDiagramModelBendpoint point: _dbObject.getBendpoints() ) {
			DBPlugin.update(db, "INSERT INTO bendpoint (parent, model, version, startx, starty, endx, endy, rank)", _dbObject.getId(), _dbObject.getModelId(), _dbObject.getVersion(), point.getStartX(), point.getStartY(), point.getEndX(), point.getEndY(), ++rank);
			++nbBendpoint;
		}
	}
	private void exportFolders(DBObject _folder, String _parentId, int rank) throws SQLException {
		DBPlugin.update(db, "INSERT INTO folder (id, model, version, documentation, parent, name, type, rank)",
				_folder.getId(), _folder.getModelId(), _folder.getVersion(), _folder.getDocumentation(), _parentId , _folder.getName(), _folder.getFolderType(rank).getValue(), rank);
		++rank;
		++nbFolder;
		exportProperties(_folder);
		
		for ( IFolder f: _folder.getFolders() ) {
			exportFolders(new DBObject(_folder.getDBModel(), f), _folder.getId(), rank);
		}
	}
}
