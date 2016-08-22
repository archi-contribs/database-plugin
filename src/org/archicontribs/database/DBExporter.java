/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.IRelationship;

/**
 * Database Model Exporter
 * 
 * @author Herve Jouin
 */
public class DBExporter implements IModelExporter {
	private Connection db;
	private DBList selectedModels;
	private DBModel dbModel;
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

	@Override
	public void export(IArchimateModel _model) throws IOException {
		dbModel = new DBModel(_model);
		
		if ( (db = new DBSelectDatabase().open()) == null)
			return;

		try {
			selectedModels = new DBSelectModel().open(db, dbModel);
			if ( selectedModels == null || selectedModels.size() == 0 ) {
				// if the user clicked on cancel or did not selected any project to export, we give up 
				try { db.close(); } catch (SQLException ee) { };
				return;
			}
		} catch (SQLException e) {
			DBPlugin.popup(Level.Error, "Failed to get the model list from the database.", e);
			try { db.close(); } catch (SQLException ee) {}
			return;
		}

		// Preparing the DBProgress popup with one tab per model to export
		dbProgress = new DBProgress();
		for ( HashMap<String, String> modelSelected: selectedModels.values() ) {
			try {
				//we create the tab corresponding to the model to export
				dbTabItem = dbProgress.tabItem(modelSelected.get("name"));
				
				// if the model already exists in the database, we ask the user to confirm the replacement
				ResultSet res = DBPlugin.select(db, "SELECT * FROM model WHERE model = ? AND version = ?", modelSelected.get("id"), modelSelected.get("version"));
				if ( res.next() ) {
					if ( !MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, "You're about to replace the existing model "+modelSelected.get("name")+" ("+modelSelected.get("id")+") version "+modelSelected.get("version")+" in the database.\n\nAre you sure ?") ) {
						dbTabItem.setText("Export cancelled by user ...");
						modelSelected.put("_export_it", "no");
						try { res.close(); } catch (SQLException ee) {}
						continue;
					} else {
						// we use underscores to be sure it will never be an attibute name set by the DBSelectModel method ...
						modelSelected.put("_export_it", "yes");
					}
				}
				res.close();
				
				if ( dbModel.isShared() ) {
					if ( (dbModel.searchFolderById(modelSelected.get("id")) == null) && (dbModel.searchProjectFolderByName(modelSelected.get("name")) == null) ) {
						DBPlugin.popup(Level.Error, "Thats weird ...\n\nCannot find the model's folder for model \""+modelSelected.get("name")+"\".");
						dbTabItem.setText("Thats weird ...\n\nCannot find the model's folder !");
						selectedModels.remove(modelSelected.get("id"));
					}
				}
			} catch (SQLException e) {
				DBPlugin.popup(Level.Error, "An SQL Exception occured !!!", e);
				dbProgress.dismiss();
				return;
			}
		}

		for ( HashMap<String, String> modelSelected: selectedModels.values() ) {
			try {
				dbTabItem = dbProgress.tabItem(modelSelected.get("name"));
				
				if ( modelSelected.get("export").equals("yes") ) {
					// if the model is the shared one, then we search for the folder to export
					if ( dbModel.isShared() ) {
						if ( dbModel.setProjectFolder(dbModel.searchFolderById(modelSelected.get("id"))) == null )
							dbModel.setProjectFolder(dbModel.searchProjectFolderByName(modelSelected.get("name")));
					}
	
					long startTime = System.currentTimeMillis();
	
					// we change the Id, version, name and purpose in case they've been changed in the ChooseModel window
					dbModel.setProjectId(modelSelected.get("id"), modelSelected.get("version"));
					dbModel.setName(modelSelected.get("name"));
					dbModel.setPurpose(modelSelected.get("purpose"));
					dbModel.setOwner(modelSelected.get("owner"));
	
					// the countExistingEObject method changes the ID and version of all components in the model (standalone mode) or project (shared mode)
					dbTabItem.setText("Please wait while counting and versionning elements to export ...");
					dbModel.countExistingEObjects();
					
					dbTabItem.setMaximum(dbModel.countAllComponents());
	
					// we reset the counters values
					countMetadatas=0;
					countFolders=0;
					countElements=0;
					countRelationships=0;
					countProperties=0;
					countArchimateDiagramModels=0;
					countDiagramModelArchimateConnections=0;
					countDiagramModelConnections=0;
					countDiagramModelArchimateObjects=0;
					countDiagramModelGroups=0;
					countDiagramModelNotes=0;
					countCanvasModels=0;
					countCanvasModelBlocks=0;
					countCanvasModelStickys=0;
					countCanvasModelConnections=0;
					countCanvasModelImages=0;
					countImages=0;
					countSketchModels=0;
					countSketchModelActors=0;
					countSketchModelStickys=0;
					countDiagramModelBendpoints=0;
					countDiagramModelReferences=0;
					countTotal=0;
	
					// we remove the old components (if any) from the database
					dbTabItem.setText("Please wait while cleaning up database ...");
					for(String table: DBPlugin.allTables ) {
						DBPlugin.sql(db, "DELETE FROM "+table+" WHERE model = ? AND version = ?", modelSelected.get("id"), modelSelected.get("version"));
					}
	
					dbTabItem.setText("Please wait while exporting components ...");
					
					// we save the images
					exportImages(dbModel);
	
					// we save the folders
					for (IFolder f: dbModel.getFolders() ) {
						// in shared mode, we do not save the project folder itself ...
						if ( (dbModel.getProjectFolder() == null) || !f.getId().equals(dbModel.getProjectFolder().getId()) )
							exportFolder(new DBObject(dbModel, f), null, 0);
					}
	
					// we save the components
					DBObject dbObject;
					for (IFolder f: dbModel.getFolders() ) {
						// in shared mode, we do not save the project folder content ...
						if ( (dbModel.getProjectFolder()) != null && f.getId().equals(dbModel.getProjectFolder().getId()) )
							continue;
						Iterator<EObject> iter = f.eAllContents();
						while ( iter.hasNext() ) {
							EObject obj = iter.next();
							if ( obj instanceof IIdentifier ) {
								dbObject = new DBObject(dbModel, obj);
								switch ( dbObject.getEClassName() ) {
								case "SketchModel" :
									DBPlugin.update(db, "INSERT into sketchmodel (id, model, version, name, documentation, connectionroutertype, background, folder)", dbObject.getId(), dbObject.getProjectId(), dbObject.getVersion(), dbObject.getName(), dbObject.getDocumentation(), dbObject.getConnectionRouterType(), dbObject.getBackground(), dbObject.getFolder());
									dbTabItem.setCountSketchModels(++countSketchModels);
									dbTabItem.setProgressBar(++countTotal);
									exportProperties(dbObject);
	
									for ( int rank=0; rank < dbObject.getChildrenSize(); ++rank ) {
										DBObject dbChild = dbObject.getChild(rank);
										switch ( dbChild.getEClassName() ) {
										case "SketchModelSticky" :
											exportSketchModelSticky(dbObject.getId(), dbChild, rank, 0);
											break;
										case "SketchModelActor" :
											exportSketchModelActor(dbObject.getId(), dbChild, rank, 0);
											break;
										case "DiagramModelGroup" :
											exportDiagramModelArchimateObject(dbObject.getId(), dbChild, rank, 0);
											break;
										default :  //should not be here
											DBPlugin.popup(Level.Error,"Don't know how to save SketchModel child : " + dbChild.getName() + " (" + dbChild.getEClassName() + ")");
										}
									}
									break;
	
								case "CanvasModel" :
									DBPlugin.update(db, "INSERT INTO canvasmodel (id, model, version, name, documentation, hinttitle, hintcontent, connectionroutertype, folder)", dbObject.getId(), dbObject.getProjectId(), dbObject.getVersion(), dbObject.getName(), dbObject.getDocumentation(), dbObject.getHintTitle(), dbObject.getHintContent(), dbObject.getConnectionRouterType(), dbObject.getFolder());
									dbTabItem.setCountCanvasModels(++countCanvasModels);
									dbTabItem.setProgressBar(++countTotal);
									exportProperties(dbObject);
	
									for ( int rank=0; rank < dbObject.getChildrenSize(); ++rank ) {
										DBObject dbChild = dbObject.getChild(rank);
										switch ( dbChild.getEClassName() ) {
										case "CanvasModelBlock" :
											exportCanvasModelBlock(dbObject.getId(), dbChild, rank, 0);
											break;
										case "CanvasModelSticky" :
											exportCanvasModelSticky(dbObject.getId(), dbChild, rank, 0);
											break;
										case "CanvasModelImage" :
											exportCanvasModelImage(dbObject.getId(), dbChild, rank, 0);
											break;
										case "CanvasModelConnection" :
											exportConnection(dbObject.getId(), dbChild, rank);
											break;
										default :  //should not be here
											DBPlugin.popup(Level.Error,"Don't know how to save CanvasModel child : " + dbChild.getName() + " (" + dbChild.getEClassName() + ")");
										}
									}
									break;
	
								case "ArchimateDiagramModel" :
									DBPlugin.update(db, "INSERT INTO archimatediagrammodel (id, model, version, name, documentation, connectionroutertype, viewpoint, type, folder)", dbObject.getId(), dbObject.getProjectId(), dbObject.getVersion(), dbObject.getName(), dbObject.getDocumentation(), dbObject.getConnectionRouterType(), dbObject.getViewpoint(), dbObject.getClassSimpleName(), dbObject.getFolder());
									dbTabItem.setCountArchimateDiagramModels(++countArchimateDiagramModels);
									dbTabItem.setProgressBar(++countTotal);
									exportProperties(dbObject);
	
									for ( int rank=0; rank < dbObject.getChildrenSize(); ++rank ) {
										DBObject dbChild = dbObject.getChild(rank);
										switch ( dbChild.getEClassName() ) {
										case "DiagramModelArchimateConnection" :
											exportConnection(dbObject.getId(), dbChild, rank);
											break;
										case "DiagramModelArchimateObject" :
										case "DiagramModelGroup" :
										case "DiagramModelNote" :
											exportDiagramModelArchimateObject(dbObject.getId(), dbChild, rank, 0);
											break;
										case "DiagramModelReference" :
											exportDiagramModelReference(dbObject.getId(), dbChild, rank, 0);
											break;
										default : //should never be here
											DBPlugin.popup(Level.Error,"Don't know how to save DiagramModel child : " + dbChild.getName() + " (" + dbChild.getEClassName() + ")");
										}
									}
									break;
	
								default:
									// here, the class is too detailed (Node, Artefact, BisinessActor, etc ...)
									// so we use "instanceof" to determine if they are an element or a relationship, which is sufficient to export them
									if ( dbObject.getEObject() instanceof IArchimateElement ) {
										DBPlugin.update(db, "INSERT INTO archimateelement (id, model, version, name, type, documentation, folder)", dbObject.getId(), dbObject.getProjectId(), dbObject.getVersion(), dbObject.getName(), dbObject.getClassSimpleName(), dbObject.getDocumentation(), dbObject.getFolder());
										dbTabItem.setCountElements(++countElements);
										dbTabItem.setProgressBar(++countTotal);
										exportProperties(dbObject);
									}
									else if ( dbObject.getEObject() instanceof IRelationship ) {
										DBPlugin.update(db, "INSERT INTO relationship (id, model, version, name, source, target, type, documentation, folder)", dbObject.getId(), dbObject.getProjectId(), dbObject.getVersion(), dbObject.getName(), dbObject.getSourceId(), dbObject.getTargetId(), dbObject.getClassSimpleName(), dbObject.getDocumentation(), dbObject.getFolder());
										dbTabItem.setCountRelationships(++countRelationships);
										dbTabItem.setProgressBar(++countTotal);
										exportProperties(dbObject);
									}
								}
							}
						}
					}
	
					// at last, we export the model itself
					exportProperties(dbModel);
					DBPlugin.update(db, "INSERT INTO model (model, version, name, purpose, owner, period, note, countMetadatas, countFolders, countElements, countRelationships, countProperties, countArchimateDiagramModels, countDiagramModelArchimateObjects, countDiagramModelArchimateConnections, countDiagramModelGroups, countDiagramModelNotes, countCanvasModels, countCanvasModelBlocks, countCanvasModelStickys, countCanvasModelConnections, countCanvasModelImages, countSketchModels, countSketchModelActors, countSketchModelStickys, countDiagramModelConnections, countDiagramModelBendpoints, countDiagramModelReferences, countImages)", 
							dbModel.getProjectId(), dbModel.getVersion(), dbModel.getName(), dbModel.getPurpose(), dbModel.getOwner(), new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()), modelSelected.get("note"), countMetadatas, countFolders, countElements, countRelationships, countProperties, countArchimateDiagramModels, countDiagramModelArchimateObjects, countDiagramModelArchimateConnections, countDiagramModelGroups, countDiagramModelNotes, countCanvasModels, countCanvasModelBlocks, countCanvasModelStickys, countCanvasModelConnections, countCanvasModelImages, countSketchModels, countSketchModelActors, countSketchModelStickys, countDiagramModelConnections, countDiagramModelBendpoints, countDiagramModelReferences, countImages);
	
					long endTime = System.currentTimeMillis();
					String duration = String.format("%d'%02d", TimeUnit.MILLISECONDS.toMinutes(endTime-startTime), TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime)));
	
					int totalExported = countMetadatas + countFolders + countElements + countRelationships + countProperties +
							countArchimateDiagramModels + countDiagramModelArchimateObjects + countDiagramModelArchimateConnections + countDiagramModelGroups + countDiagramModelNotes +  
							countCanvasModels + countCanvasModelBlocks + countCanvasModelStickys + countCanvasModelConnections + countCanvasModelImages + 
							countSketchModels + countSketchModelActors + countSketchModelStickys + countDiagramModelConnections +
							countDiagramModelBendpoints + countDiagramModelReferences + countImages;
					String msg;
					if ( totalExported == dbModel.countAllComponents() ) {
						msg = "The model \"" + dbModel.getName() + "\" has been successfully exported to the database in "+duration+"\n\n";
						msg += "--> " + totalExported + " components exported <--";
					} else {
						msg = "The model \"" + dbModel.getName() + "\" has been exported to the database in "+duration+", but with errors !\nPlesae check below :\n";
						msg += "--> " + totalExported + " / " + dbModel.countAllComponents() + " components exported <--";
					}
					
					dbTabItem.setText(msg);
					
					if ( countMetadatas != dbModel.countMetadatas() )												dbTabItem.setCountMetadatas(String.valueOf(countMetadatas) + " / " + String.valueOf(dbModel.countMetadatas()));
					
					if ( countFolders != dbModel.countFolders() )													dbTabItem.setCountFolders(String.valueOf(countFolders) + " / " + String.valueOf(dbModel.countFolders()));
					if ( countElements != dbModel.countElements() )													dbTabItem.setCountElements(String.valueOf(countElements) + " / " + String.valueOf(dbModel.countElements()));
					if ( countRelationships != dbModel.countRelationships() )										dbTabItem.setCountRelationships(String.valueOf(countRelationships) + " / " + String.valueOf(dbModel.countRelationships()));
					if ( countProperties != dbModel.countProperties() )												dbTabItem.setCountProperties(String.valueOf(countProperties) + " / " + String.valueOf(dbModel.countProperties()));
	
					if ( countArchimateDiagramModels != dbModel.countArchimateDiagramModels() )						dbTabItem.setCountArchimateDiagramModels(String.valueOf(countArchimateDiagramModels) + " / " + String.valueOf(dbModel.countArchimateDiagramModels()));
					if ( countDiagramModelArchimateObjects != dbModel.countDiagramModelArchimateObjects() )			dbTabItem.setCountDiagramModelArchimateObjects(String.valueOf(countDiagramModelArchimateObjects) + " / " + String.valueOf(dbModel.countDiagramModelArchimateObjects()));
					if ( countDiagramModelArchimateConnections != dbModel.countDiagramModelArchimateConnections() )	dbTabItem.setCountDiagramModelArchimateConnections(String.valueOf(countDiagramModelArchimateConnections) + " / " + String.valueOf(dbModel.countDiagramModelArchimateConnections()));
					if ( countDiagramModelConnections != dbModel.countDiagramModelConnections() )					dbTabItem.setCountDiagramModelConnections(String.valueOf(countDiagramModelConnections) + " / " + String.valueOf(dbModel.countDiagramModelConnections()));
	
					if ( countDiagramModelGroups !=+ dbModel.countDiagramModelGroups() )							dbTabItem.setCountDiagramModelGroups(String.valueOf(countDiagramModelGroups) + " / " + String.valueOf(dbModel.countDiagramModelGroups()));
					if ( countDiagramModelNotes != dbModel.countDiagramModelNotes() )								dbTabItem.setCountDiagramModelNotes(String.valueOf(countDiagramModelNotes) + " / " + String.valueOf(dbModel.countDiagramModelNotes()));
	
					if ( countCanvasModels != dbModel.countCanvasModels() )											dbTabItem.setCountCanvasModels(String.valueOf(countCanvasModels) + " / " + String.valueOf(dbModel.countCanvasModels()));
					if ( countCanvasModelBlocks != dbModel.countCanvasModelBlocks() )								dbTabItem.setCountCanvasModelBlocks(String.valueOf(countCanvasModelBlocks) + " / " + String.valueOf(dbModel.countCanvasModelBlocks()));
					if ( countCanvasModelStickys != dbModel.countCanvasModelStickys() )								dbTabItem.setCountCanvasModelStickys(String.valueOf(countCanvasModelStickys) + " / " + String.valueOf(dbModel.countCanvasModelStickys()));
					if ( countCanvasModelConnections != dbModel.countCanvasModelConnections() )						dbTabItem.setCountCanvasModelConnections(String.valueOf(countCanvasModelConnections) + " / " + String.valueOf(dbModel.countCanvasModelConnections()));
					if ( countCanvasModelImages != dbModel.countCanvasModelImages() )								dbTabItem.setCountCanvasModelImages(String.valueOf(countCanvasModelImages) + " / " + String.valueOf(dbModel.countCanvasModelImages()));
	
					if ( countSketchModels != dbModel.countSketchModels() )											dbTabItem.setCountSketchModels(String.valueOf(countSketchModels) + " / " + String.valueOf(dbModel.countSketchModels()));
					if ( countSketchModelActors != dbModel.countSketchModelActors() )								dbTabItem.setCountSketchModelActors(String.valueOf(countSketchModelActors) + " / " + String.valueOf(dbModel.countSketchModelActors()));
					if ( countSketchModelStickys != dbModel.countSketchModelStickys() )								dbTabItem.setCountSketchModelStickys(String.valueOf(countSketchModelStickys) + " / " + String.valueOf(dbModel.countSketchModelStickys()));
	
					if ( countDiagramModelBendpoints != dbModel.countDiagramModelBendpoints() )						dbTabItem.setCountDiagramModelBendpoints(String.valueOf(countDiagramModelBendpoints) + " / " + String.valueOf(dbModel.countDiagramModelBendpoints()));
					if ( countDiagramModelReferences != dbModel.countDiagramModelReferences() )						dbTabItem.setCountDiagramModelReferences(String.valueOf(countDiagramModelReferences) + " / " + String.valueOf(dbModel.countDiagramModelReferences()));
	
					if ( countImages != dbModel.countImages() )														dbTabItem.setCountImages(String.valueOf(countImages) + " / " + String.valueOf(dbModel.countImages()));
				}
				dbTabItem.finish();
			} catch (Exception e) {
				try { db.rollback(); } catch (Exception ee) {}
				DBPlugin.popup(Level.Error, "An error occured while exporting your model to the database.\n\nThe transaction has been rolled back and the database is left unmodified.", e);
				dbTabItem.setText("An error occured while exporting your model to the database.\nThe transaction has been rolled back and the database is left unmodified.\n" + e.getClass().getSimpleName() + " : " + e.getMessage());
				dbTabItem.setCountMetadatas("0");
				dbTabItem.setCountFolders("0");
				dbTabItem.setCountElements("0");
				dbTabItem.setCountRelationships("0");
				dbTabItem.setCountProperties("0");
				dbTabItem.setCountArchimateDiagramModels("0");
				dbTabItem.setCountDiagramModelArchimateObjects("0");
				dbTabItem.setCountDiagramModelArchimateConnections("0");
				dbTabItem.setCountDiagramModelConnections("0");
				dbTabItem.setCountDiagramModelGroups("0");
				dbTabItem.setCountDiagramModelNotes("0");
				dbTabItem.setCountCanvasModels("0");
				dbTabItem.setCountCanvasModelBlocks("0");
				dbTabItem.setCountCanvasModelStickys("0");
				dbTabItem.setCountCanvasModelConnections("0");
				dbTabItem.setCountCanvasModelImages("0");
				dbTabItem.setCountSketchModels("0");
				dbTabItem.setCountSketchModelActors("0");
				dbTabItem.setCountSketchModelStickys("0");
				dbTabItem.setCountDiagramModelBendpoints("0");
				dbTabItem.setCountDiagramModelReferences("0");
				dbTabItem.setCountImages("0");
				dbTabItem.finish();
			} finally {
				try {
					dbTabItem.finish();
					db.commit();
					// we remove the 'dirty' flag i.e. we consider the model as saved
					CommandStack stack = (CommandStack)dbModel.getModel().getAdapter(CommandStack.class);
					stack.markSaveLocation();
				} catch ( SQLException ee) {
					DBPlugin.popup(Level.Error, "Failed to commit model in database !!!", ee);
				}
			}
		}
		dbProgress.finish();
		try { db.close(); } catch (SQLException e) {}
	}

	/***************************************************************************************************************/

	private void exportProperties(DBObject _dbObject) throws SQLException {
		// exports IProperty objects
		if ( _dbObject.getProperties() != null ) {
			int rank=0;
			for(IProperty property: _dbObject.getProperties() ) {
				DBPlugin.update(db, "INSERT INTO property (id, parent, model, version, name, value)", ++rank, _dbObject.getId(), _dbObject.getProjectId(), _dbObject.getVersion(), property.getKey(), property.getValue());
				++countProperties;
				++countTotal;
			}
			if ( rank != 0 ) {
				dbTabItem.setCountProperties(countProperties);
				dbTabItem.setProgressBar(countTotal);
			}
		}
	}
	private void exportProperties(DBModel _dbModel) throws SQLException {
		// exports IProperty objects
		if ( _dbModel.getProperties() != null ) {
			int rank=0;
			for(IProperty property: _dbModel.getProperties() ) {
				DBPlugin.update(db, "INSERT INTO property (id, parent, model, version, name, value)", ++rank, _dbModel.getProjectId(), _dbModel.getProjectId(), _dbModel.getVersion(), property.getKey(), property.getValue());
				++countProperties;
				++countTotal;
			}
			if ( rank != 0 ) {
				dbTabItem.setCountProperties(countProperties);
				dbTabItem.setProgressBar(countTotal);
			}
		}

		// metadata properties are saved with negative IDs
		if ( _dbModel.getMetadata() != null ) {
			int rank=-1000;
			for(IProperty property: _dbModel.getMetadata() ) {
				DBPlugin.update(db, "INSERT INTO property (id, parent, model, version, name, value)", ++rank, _dbModel.getProjectId(), _dbModel.getProjectId(), _dbModel.getVersion(), property.getKey(), property.getValue());
				++countMetadatas;
				++countTotal;
			}
			if ( rank != -1000 ) {
				dbTabItem.setCountMetadatas(countMetadatas);
				dbTabItem.setProgressBar(countTotal);
			}
		}
	}

	/***************************************************************************************************************/

	private void exportDiagramModelArchimateObject(String _parentId, DBObject _archimateObject, int _rank, int _indent) throws SQLException {
		//exports IDiagramModelArchimateObject + IDiagramModelObject + IDiagramModelGroup + IDiagramModelNote objects
		String targetConnections = _archimateObject.getTargetConnectionsString();
		//we specify all the fields in the INSERT request as the DBObject return null values if not set (but does not trigger an exception)
		DBPlugin.update(db, "INSERT INTO diagrammodelarchimateobject (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, textAlignment, archimateelementid, archimateelementname, archimateelementclass, targetconnections, rank, indent, type, class, bordertype, content, documentation, name, x, y, width, height)",
				_archimateObject.getId(), _archimateObject.getProjectId(), _archimateObject.getVersion(), _parentId, _archimateObject.getFillColor(), _archimateObject.getFont(), _archimateObject.getFontColor(), _archimateObject.getLineColor(), _archimateObject.getLineWidth(), _archimateObject.getTextAlignment(),
				_archimateObject.getArchimateElementId(),
				_archimateObject.getArchimateElementName(),
				_archimateObject.getArchimateElementClass(),
				targetConnections, _rank, _indent, _archimateObject.getType(), _archimateObject.getEClassName(), _archimateObject.getBorderType(), _archimateObject.getContent(), _archimateObject.getDocumentation(), _archimateObject.getName(), _archimateObject.getBounds().getX(), _archimateObject.getBounds().getY(), _archimateObject.getBounds().getWidth(), _archimateObject.getBounds().getHeight());
		switch ( _archimateObject.getEClassName() ) {
		case "DiagramModelArchimateObject": dbTabItem.setCountDiagramModelArchimateObjects(++countDiagramModelArchimateObjects);
											dbTabItem.setProgressBar(++countTotal);
											break;
		case "DiagramModelGroup" :			dbTabItem.setCountDiagramModelGroups(++countDiagramModelGroups);
											dbTabItem.setProgressBar(++countTotal);
											break;
		case "DiagramModelNote" :			dbTabItem.setCountDiagramModelNotes(++countDiagramModelNotes);
											dbTabItem.setProgressBar(++countTotal);
											break;
		default : 							DBPlugin.popup(Level.Error, "exportDiagramModelArchimateObject : Do not know how to save "+_archimateObject.getEClassName());
		}

		if ( _archimateObject.getSourceConnections() != null ) {
			for ( int i=0; i < _archimateObject.getSourceConnections().size(); ++i) {
				exportConnection(_archimateObject.getId(), _archimateObject.getSourceConnection(i), i);
			}
		}
		for ( int i=0; i < _archimateObject.getChildrenSize(); ++i) {
			switch ( _archimateObject.getChild(i).getEClassName() ) {
			case "CanvasModelImage" :			exportCanvasModelImage(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1); break;
			case "DiagramModelArchimateObject":
			case "DiagramModelGroup" :
			case "DiagramModelNote" :			exportDiagramModelArchimateObject(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1); break;
			case "DiagramModelReference" :		exportDiagramModelReference(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1); break;
			case "SketchModelActor" :			exportSketchModelActor(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1); break;
			case "SketchModelSticky" : 			exportSketchModelSticky(_archimateObject.getId(), _archimateObject.getChild(i), i, _indent+1); break;
			default : 							DBPlugin.popup(Level.Error, "exportDiagramModelArchimateObject : Do not know how to save child "+_archimateObject.getChild(i).getEClassName());
			}
		}
		exportProperties(_archimateObject);
	}
	private void exportConnection(String _parentId, DBObject _connection, int _rank) throws SQLException {
		//exports IDiagramModelArchimateConnection, IDiagramModelconnection and ICanvasModelConnection objects
		//IDiagramModelArchimateConnection c; c.get
		//IDiagramModelConnection c; c.get
		//ICanvasModelConnection c; c.get
		DBPlugin.update(db, "INSERT INTO connection (id, model, version, class, documentation, islocked, font, fontcolor, linecolor, linewidth, parent, relationship, source, target, text, textposition, type, rank)",
				_connection.getId(), _connection.getProjectId(), _connection.getVersion(), _connection.getEClassName(), _connection.getDocumentation(), _connection.isLocked(), _connection.getFont(), _connection.getFontColor(), _connection.getLineColor(), _connection.getLineWidth(), _parentId, _connection.getRelationshipId(), _connection.getSourceId(), _connection.getTargetId(), _connection.getText(), _connection.getTextPosition(),	_connection.getType(), _rank);
		switch (_connection.getEClassName()) {
		case "DiagramModelConnection" : 			dbTabItem.setCountDiagramModelConnections(++countDiagramModelConnections);
													dbTabItem.setProgressBar(++countTotal);
													break;
		case "DiagramModelArchimateConnection" :	dbTabItem.setCountDiagramModelArchimateConnections(++countDiagramModelArchimateConnections);
													dbTabItem.setProgressBar(++countTotal);
													break;
		case "CanvasModelConnection" :				dbTabItem.setCountCanvasModelConnections(++countCanvasModelConnections);
													dbTabItem.setProgressBar(++countTotal);
													break;
		default :									DBPlugin.popup(Level.Error, "exportConnection : do not know how to export " + _connection.getEClassName());
		}

		exportBendpoints(_connection);
		exportProperties(_connection);
	}	
	private void exportDiagramModelReference(String _parentId, DBObject _reference, int _rank, int _indent) throws SQLException {
		//exports IDiagramModelReference objects
		//IDiagramModelReference r; r.get
		DBPlugin.update(db, "INSERT INTO diagrammodelreference (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, targetconnections, textalignment, rank, indent)", _reference.getId(), _reference.getProjectId(), _reference.getVersion(), _parentId, _reference.getFillColor(), _reference.getFont(), _reference.getFontColor(), _reference.getLineColor(), _reference.getLineWidth(), _reference.getTargetConnectionsString(), _reference.getTextAlignment(), _rank, _indent);
		dbTabItem.setCountDiagramModelReferences(++countDiagramModelReferences);
		dbTabItem.setProgressBar(++countTotal);

		if ( _reference.getSourceConnections() != null ) {
			for ( int i=0; i < _reference.getSourceConnections().size(); ++i) {
				exportConnection(_reference.getId(), _reference.getSourceConnection(i), i);
			}
		}
	}

	/***************************************************************************************************************/

	private void exportCanvasModelBlock(String _parentId, DBObject _block, int _rank, int _indent) throws SQLException {
		//export ICanvasModelBlock objects
		//ICanvasModelBlock b; b.get
		DBPlugin.update(db, "INSERT INTO canvasmodelblock (id, model, version, parent, bordercolor, content, fillcolor, font, fontcolor, hintcontent, hinttitle, imagepath, imageposition, linecolor, linewidth, islocked, name, textalignment, textposition, rank, indent, x, y, width, height)",
				_block.getId(), _block.getProjectId(), _block.getVersion(), _parentId, _block.getBorderColor(), _block.getContent(), _block.getFillColor(), _block.getFont(), _block.getFontColor(),
				_block.getHintContent(), _block.getHintTitle(), _block.getImagePath(), _block.getImagePosition(), _block.getLineColor(), _block.getLineWidth(), _block.isLocked(), _block.getName(), _block.getTextAlignment(), _block.getTextPosition(), _rank, _indent, _block.getBounds().getX(), _block.getBounds().getY(), _block.getBounds().getWidth(), _block.getBounds().getHeight());
		dbTabItem.setCountCanvasModelBlocks(++countCanvasModelBlocks);
		dbTabItem.setProgressBar(++countTotal);
		for ( int i=0; i < _block.getChildrenSize(); ++i) {
			switch ( _block.getChild(i).getEClassName() ) {
			case "DiagramModelArchimateObject":	exportDiagramModelArchimateObject(_block.getId(), _block.getChild(i), i, _indent+1); break;
			case "CanvasModelImage" : 			exportCanvasModelImage(_block.getId(), _block.getChild(i), i, _indent+1); break;
			case "CanvasModelBlock" :			exportCanvasModelBlock(_block.getId(), _block.getChild(i), i, _indent+1); break;
			case "CanvasModelSticky" :			exportCanvasModelSticky(_block.getId(), _block.getChild(i), i, _indent+1); break;
			case "DiagramModelReference" :		exportDiagramModelReference(_block.getId(), _block.getChild(i), i, _indent+1); break;
			default : 							DBPlugin.popup(Level.Error, "exportCanvasModelBlock : do not know how to export child " + _block.getChild(i).getEClassName());
			}
		}
		if ( _block.getSourceConnections() != null ) {
			for ( int i=0; i < _block.getSourceConnections().size(); ++i) {
				exportConnection(_block.getId(), _block.getSourceConnection(i), i);
			}
		}		
		exportProperties(_block);
	}
	private void exportCanvasModelSticky(String _parentId, DBObject _sticky, int _rank, int _indent) throws SQLException {
		//export ICanvasModelSticky objects
		//ICanvasModelSticky s; s.get
		DBPlugin.update(db, "INSERT INTO canvasmodelsticky (id, model, version, parent, bordercolor, content, fillcolor, font, fontcolor, imagepath, imageposition, islocked, linecolor, linewidth, notes, name, targetconnections, textalignment, textposition, rank, indent, x, y, width, height)",
				_sticky.getId(), _sticky.getProjectId(), _sticky.getVersion(), _parentId, _sticky.getBorderColor(), _sticky.getContent(), _sticky.getFillColor(), _sticky.getFont(), _sticky.getFontColor(),
				_sticky.getImagePath(), _sticky.getImagePosition(), _sticky.isLocked(), _sticky.getLineColor(), _sticky.getLineWidth(), _sticky.getNotes(), _sticky.getName(),  _sticky.getTargetConnectionsString(), _sticky.getTextAlignment(), _sticky.getTextPosition(), _rank, _indent, _sticky.getBounds().getX(), _sticky.getBounds().getY(), _sticky.getBounds().getWidth(), _sticky.getBounds().getHeight());
		dbTabItem.setCountCanvasModelStickys(++countCanvasModelStickys);
		dbTabItem.setProgressBar(++countTotal);
		exportProperties(_sticky);

		if ( _sticky.getSourceConnections() != null ) {
			for ( int i=0; i < _sticky.getSourceConnections().size(); ++i) {
				exportConnection(_sticky.getId(), _sticky.getSourceConnection(i), i);
			}
		}
	}
	private void exportCanvasModelImage(String _parentId, DBObject _image, int _rank, int _indent) throws SQLException {
		//export ICanvasModelImage objects
		//ICanvasModelImage i ; i.get
		DBPlugin.update(db, "INSERT INTO canvasmodelimage (id, model, version, parent, bordercolor, islocked, fillcolor, font, fontcolor, imagepath, linecolor, linewidth, name, targetconnections, textalignment, rank, indent, x, y, width, height)",
				_image.getId(), _image.getProjectId(), _image.getVersion(), _parentId, _image.getBorderColor(), _image.isLocked(), _image.getFillColor(), _image.getFont(), _image.getFontColor(),
				_image.getImagePath(), _image.getLineColor(), _image.getLineWidth(), _image.getName(),  _image.getTargetConnectionsString(), _image.getTextAlignment(), _rank, _indent, _image.getBounds().getX(), _image.getBounds().getY(), _image.getBounds().getWidth(), _image.getBounds().getHeight());
		dbTabItem.setCountCanvasModelImages(++countCanvasModelImages);
		dbTabItem.setProgressBar(++countTotal);

		if ( _image.getSourceConnections() != null ) {
			for ( int i=0; i < _image.getSourceConnections().size(); ++i) {
				exportConnection(_image.getId(), _image.getSourceConnection(i), i);
			}
		}
	}

	/***************************************************************************************************************/

	private void exportSketchModelSticky(String _parentId, DBObject _sticky, int _rank, int _indent) throws SQLException {
		//export ISketchModelSticky objects
		//ISketchModelSticky s; s.get
		DBPlugin.update(db, "INSERT INTO sketchmodelsticky (id, model, version, parent, content, fillcolor, font, fontcolor, linecolor, linewidth, name, targetconnections, textalignment, rank, indent, x, y, width, height)",
				_sticky.getId(), _sticky.getProjectId(), _sticky.getVersion(), _parentId, _sticky.getContent(), _sticky.getFillColor(), _sticky.getFont(), _sticky.getFontColor(),
				_sticky.getLineColor(), _sticky.getLineWidth(), _sticky.getName(), _sticky.getTargetConnectionsString(), _sticky.getTextAlignment(), _rank, _indent, _sticky.getBounds().getX(), _sticky.getBounds().getY(), _sticky.getBounds().getWidth(), _sticky.getBounds().getHeight());
		dbTabItem.setCountSketchModelStickys(++countSketchModelStickys);
		dbTabItem.setProgressBar(++countTotal);
		exportProperties(_sticky);

		if ( _sticky.getSourceConnections() != null ) {
			for ( int i=0; i < _sticky.getSourceConnections().size(); ++i) {
				exportConnection(_sticky.getId(), _sticky.getSourceConnection(i), i);
			}
		}

		for ( int i=0; i < _sticky.getChildrenSize(); ++i) {
			switch ( _sticky.getChild(i).getEClassName() ) {
			case "DiagramModelArchimateObject":
			case "DiagramModelGroup" :
			case "DiagramModelNote" :		exportDiagramModelArchimateObject(_sticky.getId(), _sticky.getChild(i), i, _indent+1); break;
			case "DiagramModelReference" :	exportDiagramModelReference(_sticky.getId(), _sticky.getChild(i), i, _indent+1); break;
			case "SketchModelSticky" :		exportSketchModelSticky(_sticky.getId(), _sticky.getChild(i), i, _indent+1); break;
			case "SketchModelActor" :		exportSketchModelActor(_sticky.getId(), _sticky.getChild(i), i, _indent+1); break;
			default : 						DBPlugin.popup(Level.Error, "exportSketchSticky : do not know how to export child " + _sticky.getChild(i).getEClassName());
			}
		}
	}
	private void exportSketchModelActor(String _parentId, DBObject _actor, int _rank, int _indent) throws SQLException {
		//export ISketchModelActor objects
		//ISketchModelActor a; a.get
		DBPlugin.update(db, "INSERT INTO sketchmodelactor (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, name, targetconnections, textalignment, rank, indent, x, y, width, height)",
				_actor.getId(), _actor.getProjectId(), _actor.getVersion(), _parentId, _actor.getFillColor(), _actor.getFont(), _actor.getFontColor(),
				_actor.getLineColor(), _actor.getLineWidth(), _actor.getName(), _actor.getTargetConnectionsString(), _actor.getTextAlignment(), _rank, _indent, _actor.getBounds().getX(), _actor.getBounds().getY(), _actor.getBounds().getWidth(), _actor.getBounds().getHeight());
		dbTabItem.setCountSketchModelActors(++countSketchModelActors);
		dbTabItem.setProgressBar(++countTotal);
		exportProperties(_actor);

		if ( _actor.getSourceConnections() != null ) {
			for ( int i=0; i < _actor.getSourceConnections().size(); ++i) {
				exportConnection(_actor.getId(), _actor.getSourceConnection(i), i);
			}
		}
	}

	/***************************************************************************************************************/

	private void exportBendpoints(DBObject _dbObject) throws SQLException {
		//export IDiagramModelBendpoint objects
		int rank=0;
		for ( IDiagramModelBendpoint point: _dbObject.getBendpoints() ) {
			DBPlugin.update(db, "INSERT INTO bendpoint (parent, model, version, startx, starty, endx, endy, rank)", _dbObject.getId(), _dbObject.getProjectId(), _dbObject.getVersion(), point.getStartX(), point.getStartY(), point.getEndX(), point.getEndY(), ++rank);
			++countDiagramModelBendpoints;
			++countTotal;
		}
		
		if ( rank != 0 ) {
			dbTabItem.setCountDiagramModelBendpoints(countDiagramModelBendpoints);
			dbTabItem.setProgressBar(countTotal);
		}
	}

	/***************************************************************************************************************/

	private void exportFolder(DBObject _folder, String _parentId, int rank) throws SQLException {
		//IFolder f; f.
		DBPlugin.update(db, "INSERT INTO folder (id, model, version, documentation, parent, name, type, rank)",
				_folder.getId(), _folder.getProjectId(), _folder.getVersion(), _folder.getDocumentation(), _parentId , _folder.getFolderName(rank), _folder.getFolderType(rank), rank);
		++rank;
		dbTabItem.setCountFolders(++countFolders);
		dbTabItem.setProgressBar(++countTotal);
		exportProperties(_folder);

		for ( IFolder f: _folder.getFolders() ) {
			exportFolder(new DBObject(_folder.getDBModel(), f), _folder.getId(), rank);
		}
	}

	/***************************************************************************************************************/

	private void exportImages(DBModel _dbModel) throws SQLException {
		IArchiveManager archiveMgr = (IArchiveManager)dbModel.getModel().getAdapter(IArchiveManager.class);

		for ( String path: archiveMgr.getImagePaths() ) {
			try {
				byte[] image = archiveMgr.getBytesFromEntry(path);
				String md5 = getMD5(image);
				
				try {
					ResultSet result = DBPlugin.select(db, "SELECT count(*) from images where path = ?", path);
					result.next();
					if ( result.getInt(1) == 0 ) {
						// if the image is not yet in the database, we insert it
						DBPlugin.update(db, "INSERT INTO images (path, md5, image)", path, md5, image);
					} else {
						// we check if the calculated md5 is different than in the database, then we update it
						result = DBPlugin.select(db, "SELECT md5 from images where path = ?", path);
						result.next();
						if ( !md5.equals(result.getString("md5")) ) {
							DBPlugin.update(db, "UPDATE images SET md5 = ?, image = ? WHERE path = ?", md5, image, path);
						}
					}
				} catch (SQLException e) {
					throw e;
				}
				dbTabItem.setCountImages(++countImages);
				dbTabItem.setProgressBar(++countTotal);
			} catch (NoSuchAlgorithmException e) {
				DBPlugin.popup(Level.Error, "Failed to calculate MD5 for image "+path+" !", e);
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "Failed to invoke reflection to get images !", e);
				return;
			}
		}
	}
	public static String getMD5(byte[] _input) throws NoSuchAlgorithmException {
	    String result = null;
	    if(_input != null) {
	    	MessageDigest md = MessageDigest.getInstance("MD5");
	    	md.update(_input);
	    	BigInteger hash = new BigInteger(1, md.digest());
	    	result = hash.toString(16);
	    	while(result.length() < 32) {
	    		result = "0" + result;
	    	}
	    }
	    return result;
	}
}
