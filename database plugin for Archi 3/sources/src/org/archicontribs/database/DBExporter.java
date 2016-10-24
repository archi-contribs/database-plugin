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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.canvas.model.ICanvasModel;
import com.archimatetool.canvas.model.ICanvasModelBlock;
import com.archimatetool.canvas.model.ICanvasModelConnection;
import com.archimatetool.canvas.model.ICanvasModelImage;
import com.archimatetool.canvas.model.ICanvasModelSticky;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInterfaceElement;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.IRelationship;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ISketchModelActor;
import com.archimatetool.model.ISketchModelSticky;

/**
 * Database Model Exporter
 * 
 * @author Herve Jouin
 */
public class DBExporter implements IModelExporter {
	private Connection db;
	
	private List<HashMap<String, String>> selectedModels;
	private DBModel dbModel;
	private DBSelectModel dbSelectModel;
	private DBProgress dbProgress;
	private DBProgressTabItem dbTabItem;

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
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.export("+_model.getName()+")");
		
		dbModel = new DBModel(_model);
		
		dbSelectModel = new DBSelectModel();
		try {
			db = dbSelectModel.selectModelToExport(dbModel);
		} catch (Exception err) {
			DBPlugin.popup(Level.Error, "An error occurred !!!", err);
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.export("+_model.getName()+")");
			return;
		}
		
		if ( db == null ) {
			// if there is no database connection, we cannot export
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.export("+_model.getName()+")");
			return;
		}
		
		selectedModels = dbSelectModel.getSelectedModels();
		if ( selectedModels == null || selectedModels.size() == 0 ) {
			// if the user clicked on cancel or did not selected any project to export, we give up 
			try { db.close(); } catch (SQLException ee) { };
			DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.export("+_model.getName()+")");
			return;
		}

		// Preparing the DBProgress popup with one tab per model to export
		dbProgress = new DBProgress();
		for ( HashMap<String, String> modelSelected: selectedModels ) {
			try {
				// we create the tab corresponding to the model to export
				dbTabItem = dbProgress.tabItem(modelSelected.get("name"));
				
				// by default, we export all the projects that the user selected
				modelSelected.put("_export_it", "yes");
				
				// if the model already exists in the db, we ask the user to confirm the replacement
				String request;
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					request = "SELECT name FROM model WHERE model = ? AND version = ?";
				} else {
					request = "MATCH (m:model) WHERE m.model = ? AND m.version = ? RETURN m.name";
				}
				ResultSet result = DBPlugin.select(db, request, modelSelected.get("id"), modelSelected.get("version"));
				if ( result.next() ) {
					if ( !MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, "You're about to replace the existing model "+modelSelected.get("name")+" ("+modelSelected.get("id")+") version "+modelSelected.get("version")+" in the database.\n\nAre you sure ?") ) {
						dbTabItem.setWarning("Export cancelled by user ...");
						modelSelected.put("_export_it", "no");
						try { result.close(); } catch (SQLException ee) {}
						continue;
					}
				}
				result.close();
				
				if ( dbModel.isShared() ) {
					if ( (dbModel.searchFolderById(modelSelected.get("id")) == null) && (dbModel.searchProjectFolderByName(modelSelected.get("name")) == null) ) {
						DBPlugin.popup(Level.Error, "Thats weird, I cannot find the model's folder for model \""+modelSelected.get("name")+"\".");
						dbTabItem.setError("Thats weird, I cannot find the model's folder for model \""+modelSelected.get("name")+"\".");
						modelSelected.put("_export_it", "no");
					}
				}
			} catch (Exception e) {
				DBPlugin.popup(Level.Error, "An exception occured !!!", e);
				dbProgress.dismiss();
				DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.export("+_model.getName()+")");
				return;
			}
		}

		for ( HashMap<String, String> modelSelected: selectedModels ) {
			try {
				dbTabItem = dbProgress.tabItem(modelSelected.get("name"));
				
				if ( modelSelected.get("_export_it").equals("no") ) {
					dbTabItem.setWarning("cancelled by user");
					continue;
				}
				
				// we start a transaction
				db.setAutoCommit(false);
				
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
				
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					for(String table: DBPlugin.allSQLTables ) {
						DBPlugin.request(db, "DELETE FROM "+table+" WHERE model = ? AND version = ?"
							,modelSelected.get("id")
							,modelSelected.get("version")
							);
					}
				} else {
					DBPlugin.request(db, "MATCH (node)-[rm:isInModel]->(model:model {model:?, version:?}) DETACH DELETE node, model"
							,modelSelected.get("id")
							,modelSelected.get("version")
							);
					DBPlugin.request(db, "MATCH (prop:property {model:?, version:?}) DETACH DELETE prop"
							,modelSelected.get("id")
							,modelSelected.get("version")
							);
				}

				dbTabItem.setText("Please wait while exporting model ...");
				DBPlugin.debug(DebugLevel.Variable, "Exporting model id="+dbModel.getProjectId()+" version="+dbModel.getVersion()+" name="+dbModel.getName());
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO model (model, version, name, purpose, owner, period, note)"
							,dbModel.getProjectId()
							,dbModel.getVersion()
							,dbModel.getName()
							,dbModel.getPurpose()
							,dbModel.getOwner()
							,new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())
							,modelSelected.get("note")
							);
				} else {
					DBPlugin.request(db, "CREATE (new:model {model:?, version:?, name:?, purpose:?, owner:?, period:?, note:?})"
							,dbModel.getProjectId()
							,dbModel.getVersion()
							,dbModel.getName()
							,dbModel.getPurpose()
							,dbModel.getOwner()
							,new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())
							,modelSelected.get("note")
							);
				}
				
				dbTabItem.setText("Please wait while exporting images ...");
				
				// we save the images
				exportImages(dbModel);
				
				dbTabItem.setText("Please wait while exporting components ...");

				// we save the folders
				for (IFolder folder: dbModel.getFolders() ) {
					exportFolder(folder, null, 0);
				}

				// we save the components
				for (IFolder f: dbModel.getFolders() ) {
					// in shared mode, we do not save the "project" folder
					if ( (dbModel.getProjectFolder()) != null && f.getId().equals(dbModel.getProjectFolder().getId()) )
						continue;
					Iterator<EObject> iter = f.eAllContents();
					while ( iter.hasNext() ) {
						EObject objectToExport = iter.next();
						if ( (objectToExport instanceof IIdentifier) && (objectToExport instanceof INameable) ) {	// we only export components that have got an ID and a name
							String fullId = ((IIdentifier)objectToExport).getId();
							String id = DBPlugin.getId(fullId);
							String projectId = DBPlugin.getProjectId(fullId);
							String version = DBPlugin.getVersion(fullId);
							String name = ((INameable)objectToExport).getName();
							
							switch ( objectToExport.eClass().getName() ) {
							case "SketchModel" :
								ISketchModel sketchModel = (ISketchModel)objectToExport;
								DBPlugin.debug(DebugLevel.Variable, "Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
								if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
									DBPlugin.insert(db, "INSERT INTO sketchmodel (id, model, version, name, documentation, connectionroutertype, background, folder)"
											,id
											,projectId
											,version
											,name
											,sketchModel.getDocumentation()
											,sketchModel.getConnectionRouterType()
											,sketchModel.getBackground()
											,((IIdentifier)sketchModel.eContainer()).getId()
											);
								} else {
									DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:sketchmodel {id:?, name:?, documentation:?, connectionroutertype:?, background:?, folder:?}), (new)-[:isInModel]->(m)"
											,projectId
											,version
											,id
											,name
											,sketchModel.getDocumentation()
											,sketchModel.getConnectionRouterType()
											,sketchModel.getBackground()
											,((IIdentifier)sketchModel.eContainer()).getId()
											);
								}
								dbTabItem.setCountSketchModels(++countSketchModels);
								dbTabItem.setProgressBar(++countTotal);
								exportObjectProperties(sketchModel, sketchModel.getProperties());

								for ( int rank=0; rank < sketchModel.getChildren().size(); ++rank ) {
									EObject child = sketchModel.getChildren().get(rank);
									switch ( child.eClass().getName() ) {
									case "SketchModelSticky" : 
										exportSketchModelSticky(fullId, (ISketchModelSticky)child, rank, 0);
										break;
									case "SketchModelActor" :
										exportSketchModelActor(fullId, (ISketchModelActor)child, rank, 0);
										break;
									case "DiagramModelGroup" :
										exportDiagramModelObject(fullId, (IDiagramModelObject)child, rank, 0);
										break;
									default :  //should not be here
										throw new Exception("Don't know how to save SketchModel child : " + ((INameable)child).getName() + " (" + child.eClass().getName() + ")");
									}
								}
								break;

							case "CanvasModel" :
								ICanvasModel canvasModel = (ICanvasModel)objectToExport;
								DBPlugin.debug(DebugLevel.Variable, "Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
								if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
									DBPlugin.insert(db, "INSERT INTO canvasmodel (id, model, version, name, documentation, hinttitle, hintcontent, connectionroutertype, folder)"
											,id
											,projectId
											,version
											,name
											,canvasModel.getDocumentation()
											,canvasModel.getHintTitle()
											,canvasModel.getHintContent()
											,canvasModel.getConnectionRouterType()
											,((IIdentifier)canvasModel.eContainer()).getId()
											);
								} else {
									DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:canvasmodel {id:?, name:?, documentation:?, hinttitle:?, hintcontent:?, connectionroutertype:?, folder:?}), (new)-[:isInModel]->(m)"
											,projectId
											,version
											,id
											,name
											,canvasModel.getDocumentation()
											,canvasModel.getHintTitle()
											,canvasModel.getHintContent()
											,canvasModel.getConnectionRouterType()
											,((IIdentifier)canvasModel.eContainer()).getId()
											);
								}
								dbTabItem.setCountCanvasModels(++countCanvasModels);
								dbTabItem.setProgressBar(++countTotal);
								exportObjectProperties(canvasModel, canvasModel.getProperties());

								for ( int rank=0; rank < canvasModel.getChildren().size(); ++rank ) {
									EObject child = canvasModel.getChildren().get(rank);
									switch ( child.eClass().getName() ) {
									case "CanvasModelBlock" :
										exportCanvasModelBlock(fullId, (ICanvasModelBlock)child, rank, 0);
										break;
									case "CanvasModelSticky" :
										exportCanvasModelSticky(fullId, (ICanvasModelSticky)child, rank, 0);
										break;
									case "CanvasModelImage" :
										exportCanvasModelImage(fullId, (ICanvasModelImage)child, rank, 0);
										break;
									case "CanvasModelConnection" :
										exportConnection(fullId, (ICanvasModelConnection)child, rank);
										break;
									default :  //should not be here
										throw new Exception("Don't know how to save CanvasModel child : " + ((INameable)child).getName() + " (" + child.eClass().getName() + ")");
									}
								}
								break;

							case "ArchimateDiagramModel" :
								IArchimateDiagramModel archimateDiagramModel = (IArchimateDiagramModel)objectToExport;
								DBPlugin.debug(DebugLevel.Variable, "Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
								if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
									DBPlugin.insert(db, "INSERT INTO archimatediagrammodel (id, model, version, name, documentation, connectionroutertype, viewpoint, type, folder)"
											,id
											,projectId
											,version
											,name
											,archimateDiagramModel.getDocumentation()
											,archimateDiagramModel.getConnectionRouterType()
											,archimateDiagramModel.getViewpoint()
											,archimateDiagramModel.getClass().getSimpleName()
											,((IIdentifier)archimateDiagramModel.eContainer()).getId()
											);
								} else {
									DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:archimatediagrammodel {id:?, name:?, documentation:?, connectionroutertype:?, viewpoint:?, type:?, folder:?})-[:isInModel]->(m)"
											,projectId
											,version
											,id
											,name
											,archimateDiagramModel.getDocumentation()
											,archimateDiagramModel.getConnectionRouterType()
											,archimateDiagramModel.getViewpoint()
											,archimateDiagramModel.getClass().getSimpleName()
											,((IIdentifier)archimateDiagramModel.eContainer()).getId()
											);
								}
								dbTabItem.setCountArchimateDiagramModels(++countArchimateDiagramModels);
								dbTabItem.setProgressBar(++countTotal);
								exportObjectProperties(archimateDiagramModel, archimateDiagramModel.getProperties());

								for ( int rank=0; rank < archimateDiagramModel.getChildren().size(); ++rank ) {
									EObject child = archimateDiagramModel.getChildren().get(rank);
									switch ( child.eClass().getName() ) {
									case "DiagramModelArchimateConnection" :
										exportConnection(fullId, (IDiagramModelArchimateConnection)child, rank);
										break;
									case "DiagramModelArchimateObject" :
										exportDiagramModelObject(fullId, (IDiagramModelArchimateObject)child, rank, 0);
										break;
									case "DiagramModelGroup" :
										exportDiagramModelObject(fullId, (IDiagramModelGroup)child, rank, 0);
										break;
									case "DiagramModelNote" :
										exportDiagramModelObject(fullId, (IDiagramModelNote)child, rank, 0);
										break;
									case "DiagramModelReference" :
										exportDiagramModelReference(fullId, (IDiagramModelReference)child, rank, 0);
										break;
									default : //should never be here
										throw new Exception("Don't know how to save DiagramModel child : " + ((INameable)child).getName() + " (" + child.eClass().getName() + ")");
									}
								}
								break;

							case "AndJunction" :
							case "ApplicationCollaboration" :
							case "ApplicationComponent" :
							case "ApplicationFunction" :
							case "ApplicationInteraction" :
							case "ApplicationInterface" :
							case "ApplicationService" :
							case "Artifact" :
							case "Assessment" :
							case "BusinessActor" :
							case "BusinessCollaboration" :
							case "BusinessEvent" :
							case "BusinessFunction" :
							case "BusinessInteraction" :
							case "BusinessInterface" :
							case "BusinessObject" :
							case "BusinessProcess" :
							case "BusinessRole" :
							case "BusinessService" :
							case "CommunicationPath" :
							case "Constraint" :
							case "Contract" :
							case "DataObject" :
							case "Deliverable" :
							case "Device" :
							case "Driver" :
							case "Gap" :
							case "Goal" :
							case "InfrastructureFunction" :
							case "InfrastructureInterface" :
							case "InfrastructureService" :
							case "Junction" :
							case "Location" :
							case "Meaning" :
							case "Network" :
							case "Node" :
							case "OrJunction" :
							case "Plateau" :
							case "Principle" :
							case "Product" :
							case "Representation" :
							case "Requirement" :
							case "Stakeholder" :
							case "SystemSoftware" :
							case "Value" :
							case "WorkPackage" :
								IArchimateElement archimateElement = (IArchimateElement)objectToExport;
								DBPlugin.debug(DebugLevel.Variable, "Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
								if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
									DBPlugin.insert(db, "INSERT INTO archimateelement (id, model, version, name, type, documentation, folder, interfacetype)"
											,id
											,projectId
											,version
											,name
											,archimateElement.getClass().getSimpleName()
											,archimateElement.getDocumentation()
											,((IIdentifier)archimateElement.eContainer()).getId()
											,objectToExport instanceof IInterfaceElement ? ((IInterfaceElement)objectToExport).getInterfaceType() : -1				// Archi 3 only
											);
								} else {
									DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:archimateelement {id:?, name:?, type:?, documentation:?, folder:?, interfacetype:?}), (new)-[:isInModel]->(m)"
											,projectId
											,version
											,id
											,name
											,archimateElement.getClass().getSimpleName()
											,archimateElement.getDocumentation()
											,((IIdentifier)archimateElement.eContainer()).getId()
											,objectToExport instanceof IInterfaceElement ? ((IInterfaceElement)objectToExport).getInterfaceType() : -1				// Archi 3 only
											);
								}
								dbTabItem.setCountElements(++countElements);
								dbTabItem.setProgressBar(++countTotal);
								exportObjectProperties(archimateElement,archimateElement.getProperties());
								break;
								
							case "AccessRelationship" :
							case "AggregationRelationship" :
							case "AssignmentRelationship" :
							case "AssociationRelationship" :
							case "CompositionRelationship" :
							case "FlowRelationship" :
							case "InfluenceRelationship" :
							case "RealisationRelationship" :
							case "SpecialisationRelationship" :
							case "TriggeringRelationship" :
							case "UsedByRelationship" :
								IRelationship relationship = (IRelationship)objectToExport;
								DBPlugin.debug(DebugLevel.Variable, "Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
								if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
									DBPlugin.insert(db, "INSERT INTO relationship (id, model, version, name, source, target, type, documentation, folder, accesstype)"
											,id
											,projectId
											,version
											,name
											,relationship.getSource().getId()
											,relationship.getTarget().getId()
											,relationship.getClass().getSimpleName()
											,relationship.getDocumentation()
											,((IIdentifier)relationship.eContainer()).getId()
											,objectToExport.eClass().getName().equals("AccessRelationship") ? ((IAccessRelationship)objectToExport).getAccessType() : -1
											);
								} else {
									DBPlugin.request(db, "MATCH (m:model {model:?, version:?}), (source {id:?})-[:isInModel]->(m) MATCH (target {id:?})-[:isInModel]->(m) CREATE (source)-[new:"+relationship.getClass().getSimpleName()+" {id:?, name:?, type:?, documentation:?, folder:?, accesstype:?}]->(target)"
											,projectId
											,version
											,DBPlugin.getId(relationship.getSource().getId())		// TODO: watchup when they will be in separate models !!!
											,DBPlugin.getId(relationship.getTarget().getId())
											,id
											,name
											,relationship.getClass().getSimpleName()
											,relationship.getDocumentation()
											,((IIdentifier)relationship.eContainer()).getId()
											,objectToExport.eClass().getName().equals("AccessRelationship") ? ((IAccessRelationship)objectToExport).getAccessType() : -1
											);
								}
								
								dbTabItem.setCountRelationships(++countRelationships);
								dbTabItem.setProgressBar(++countTotal);
								exportObjectProperties(relationship, relationship.getProperties());
								break;
								
							case "Folder" :
							case "SketchModelSticky" : 
							case "SketchModelActor" :
							case "CanvasModelBlock" :
							case "CanvasModelSticky" :
							case "CanvasModelImage" :
							case "CanvasModelConnection" :
							case "DiagramModelArchimateConnection" :
							case "DiagramModelArchimateObject" :
							case "DiagramModelGroup" :
							case "DiagramModelNote" :
							case "DiagramModelReference" :
							case "DiagramModelConnection" :
								// do nothing as they are exported with their parent
								break;
								
							default :
								throw new Exception("Do not know how to export object of class \""+objectToExport.eClass().getName()+"\"");
							}
						}
					}
				}

				// last but not least, we update the counters of the model
				exportModelProperties(dbModel);
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.request(db, "UPDATE model set countmetadatas=?, countfolders=?, countelements=?, countrelationships=?, countproperties=?, countarchimatediagrammodels=?, countdiagrammodelarchimateobjects=?, countdiagrammodelarchimateconnections=?, countdiagrammodelgroups=?, countdiagrammodelnotes=?, countcanvasmodels=?, countcanvasmodelblocks=?, countcanvasmodelstickys=?, countcanvasmodelconnections=?, countcanvasmodelimages=?, countsketchmodels=?, countsketchmodelactors=?, countsketchmodelstickys=?, countdiagrammodelconnections=?, countdiagrammodelbendpoints=?, countdiagrammodelreferences=?, countimages=? WHERE model=? AND version=?",
							countMetadatas, countFolders, countElements, countRelationships, countProperties, countArchimateDiagramModels, countDiagramModelArchimateObjects, countDiagramModelArchimateConnections, countDiagramModelGroups, countDiagramModelNotes, countCanvasModels, countCanvasModelBlocks, countCanvasModelStickys, countCanvasModelConnections, countCanvasModelImages, countSketchModels, countSketchModelActors, countSketchModelStickys, countDiagramModelConnections, countDiagramModelBendpoints, countDiagramModelReferences, countImages,
							dbModel.getProjectId(), dbModel.getVersion());
				} else {
					DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) SET m.countmetadatas=?, m.countfolders=?, m.countelements=?, m.countrelationships=?, m.countproperties=?, m.countarchimatediagrammodels=?, m.countdiagrammodelarchimateobjects=?, m.countdiagrammodelarchimateconnections=?, m.countdiagrammodelgroups=?, m.countdiagrammodelnotes=?, m.countcanvasmodels=?, m.countcanvasmodelblocks=?, m.countcanvasmodelstickys=?, m.countcanvasmodelconnections=?, m.countcanvasmodelimages=?, m.countsketchmodels=?, m.countsketchmodelactors=?, m.countsketchmodelstickys=?, m.countdiagrammodelconnections=?, m.countdiagrammodelbendpoints=?, m.countdiagrammodelreferences=?, m.countimages=?",
							dbModel.getProjectId(), dbModel.getVersion(),
							countMetadatas, countFolders, countElements, countRelationships, countProperties, countArchimateDiagramModels, countDiagramModelArchimateObjects, countDiagramModelArchimateConnections, countDiagramModelGroups, countDiagramModelNotes, countCanvasModels, countCanvasModelBlocks, countCanvasModelStickys, countCanvasModelConnections, countCanvasModelImages, countSketchModels, countSketchModelActors, countSketchModelStickys, countDiagramModelConnections, countDiagramModelBendpoints, countDiagramModelReferences, countImages);
				}

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
					dbTabItem.setSuccess(msg);
				} else {
					msg = "The model \"" + dbModel.getName() + "\" has been exported to the database in "+duration+", but with errors !\nPlease check below :\n";
					msg += "--> " + totalExported + " / " + dbModel.countAllComponents() + " components exported <--";
					dbTabItem.setError(msg);
				}

				db.commit();
				db.setAutoCommit(true);
				
				// we remove the 'dirty' flag i.e. we consider the model as saved
				CommandStack stack = (CommandStack)dbModel.getModel().getAdapter(CommandStack.class);
				stack.markSaveLocation();
				
			} catch (Exception e) {
				try {
					db.rollback();
				} catch (Exception ee) {
					// do nothing as if the transaction has not been committed, it will be rolled back by the database anyway
				}
				DBPlugin.popup(Level.Error, "An error occured while exporting your model to the database.\n\nThe transaction has been rolled back and the database is left unmodified.", e);
				dbTabItem.setError("An error occured while exporting your model to the database.\nThe transaction has been rolled back and the database is left unmodified.\n" + e.getClass().getSimpleName() + " : " + e.getMessage());
				countMetadatas = 0;
				countFolders = 0;
				countElements = 0;
				countRelationships = 0;
				countProperties = 0;
				countArchimateDiagramModels = 0;
				countDiagramModelArchimateObjects = 0;
				countDiagramModelArchimateConnections = 0;
				countDiagramModelConnections = 0;
				countDiagramModelGroups = 0;
				countDiagramModelNotes = 0;
				countCanvasModels = 0;
				countCanvasModelBlocks = 0;
				countCanvasModelStickys = 0;
				countCanvasModelConnections = 0;
				countCanvasModelImages = 0;
				countSketchModels = 0;
				countSketchModelActors = 0;
				countSketchModelStickys = 0;
				countDiagramModelBendpoints = 0;
				countDiagramModelReferences = 0;
				countImages = 0;
			}
			dbTabItem.setCountMetadatas(countMetadatas, dbModel.countMetadatas());
			dbTabItem.setCountFolders(countFolders, dbModel.countFolders());
			dbTabItem.setCountElements(countElements, dbModel.countElements());
			dbTabItem.setCountRelationships(countRelationships, dbModel.countRelationships());
			dbTabItem.setCountProperties(countProperties, dbModel.countProperties());
			dbTabItem.setCountArchimateDiagramModels(countArchimateDiagramModels, dbModel.countArchimateDiagramModels());
			dbTabItem.setCountDiagramModelArchimateObjects(countDiagramModelArchimateObjects, dbModel.countDiagramModelArchimateObjects());
			dbTabItem.setCountDiagramModelArchimateConnections(countDiagramModelArchimateConnections, dbModel.countDiagramModelArchimateConnections());
			dbTabItem.setCountDiagramModelConnections(countDiagramModelConnections, dbModel.countDiagramModelConnections());
			dbTabItem.setCountDiagramModelGroups(countDiagramModelGroups, dbModel.countDiagramModelGroups());
			dbTabItem.setCountDiagramModelNotes(countDiagramModelNotes, dbModel.countDiagramModelNotes());
			dbTabItem.setCountCanvasModels(countCanvasModels, dbModel.countCanvasModels());
			dbTabItem.setCountCanvasModelBlocks(countCanvasModelBlocks, dbModel.countCanvasModelBlocks());
			dbTabItem.setCountCanvasModelStickys(countCanvasModelStickys, dbModel.countCanvasModelStickys());
			dbTabItem.setCountCanvasModelConnections(countCanvasModelConnections, dbModel.countCanvasModelConnections());
			dbTabItem.setCountCanvasModelImages(countCanvasModelImages, dbModel.countCanvasModelImages());
			dbTabItem.setCountSketchModels(countSketchModels, dbModel.countSketchModels());
			dbTabItem.setCountSketchModelActors(countSketchModelActors, dbModel.countSketchModelActors());
			dbTabItem.setCountSketchModelStickys(countSketchModelStickys, dbModel.countSketchModelStickys());
			dbTabItem.setCountDiagramModelBendpoints(countDiagramModelBendpoints, dbModel.countDiagramModelBendpoints());
			dbTabItem.setCountDiagramModelReferences(countDiagramModelReferences, dbModel.countDiagramModelReferences());
			dbTabItem.setCountImages(countImages, dbModel.countImages());
			
			dbTabItem.finish();
		}
		dbProgress.finish();
		try { db.close(); } catch (SQLException e) {}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.export("+_model.getName()+")");
	}

	/***************************************************************************************************************/

	/**
	 * Export the object's properties to the database
	 * @param _dbObject
	 * @throws Exception
	 */
	private void exportObjectProperties(EObject parent, EList<IProperty> properties) throws Exception {
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBExporter.exportObjectProperties()");
		
		assert(parent instanceof IIdentifier);

		if ( properties.size() > 0) {
			int rank=0;
			String parentId = DBPlugin.getId(((IIdentifier)parent).getId());
			String projectId = DBPlugin.getProjectId(((IIdentifier)parent).getId());
			String version = DBPlugin.getVersion(((IIdentifier)parent).getId());
		
			for(IProperty property: properties) {
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO property (id, parent, model, version, name, value)"
							,++rank
							,parentId
							,projectId
							,version
							,property.getKey()
							,property.getValue()
							);
				} else {
					DBPlugin.request(db, "CREATE (prop:property {id:?, parent:?, model:?, version:?, name:?, value:?})"
							,++rank
							,parentId
							,projectId
							,version
							,property.getKey()
							,property.getValue()
							);
				}
				++countProperties;
				++countTotal;
			}
			if ( rank != 0 ) {
				dbTabItem.setCountProperties(countProperties);
				dbTabItem.setProgressBar(countTotal);
			}
		}
		//DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBExporter.exportObjectProperties()");
	}
	
	/**
	 * * Exports the model's properties and metadata to the database
	 * @param _dbModel
	 * @throws Exception
	 */
	private void exportModelProperties(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBExporter.exportModelProperties()");
		
		// exports IProperty objects
		if ( _dbModel.getProperties() != null ) {
			int rank=0;
			for(IProperty property: _dbModel.getProperties() ) {
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO property (id, parent, model, version, name, value)"
							,++rank
							,_dbModel.getProjectId()
							,_dbModel.getProjectId()
							,_dbModel.getVersion()
							,property.getKey()
							,property.getValue()
							);
				} else {
					DBPlugin.request(db, "CREATE (prop:property {id:?, parent:?, model:?, version:?, name:?, value:?})"
							,++rank
							,_dbModel.getProjectId()
							,_dbModel.getProjectId()
							,_dbModel.getVersion()
							,property.getKey()
							,property.getValue()
							);
				}
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
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO property (id, parent, model, version, name, value)"
							,++rank
							,_dbModel.getProjectId()
							,_dbModel.getProjectId()
							,_dbModel.getVersion()
							,property.getKey()
							,property.getValue()
							);
				} else {
					DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (prop:property {id:?, name:?, value:?}), (m)-[:hasProperty]->(prop)"
							 ,_dbModel.getProjectId()
							 ,_dbModel.getVersion()
							 ,++rank
							 ,property.getKey()
							 ,property.getValue()
							 );
				}
				++countMetadatas;
				++countTotal;
			}
			if ( rank != -1000 ) {
				dbTabItem.setCountMetadatas(countMetadatas);
				dbTabItem.setProgressBar(countTotal);
			}
		}
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBExporter.exportModelProperties()");
	}

	/**
	 * Exports a diagramModelObject object to the database
	 * @param _parentId
	 * @param _object
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	//private void exportDiagramModelObject(String _parentId, EObject _object, int _rank, int _indent) throws Exception {
	private void exportDiagramModelObject(String _parentId, IDiagramModelObject diagramModelObject, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportDiagramModelArchimateObject()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+diagramModelObject.eClass().getName()+" id="+diagramModelObject.getId()+" name="+diagramModelObject.getName());
		
		//IDiagramModelObject diagramModelObject = (IDiagramModelObject)_object;
		String archimateElementId;
		String archimateElementName;
		String archimateElementClass;
		int borderType;
		String content;
		String documentation;
		int type;
		EList<IDiagramModelObject> children;
		
		//switch ( _object.eClass().getName() ) {
		switch ( diagramModelObject.eClass().getName() ) {
		case "DiagramModelArchimateObject" :
			//IDiagramModelArchimateObject diagraModelArchimateObject = (IDiagramModelArchimateObject)_object;
			IDiagramModelArchimateObject diagraModelArchimateObject = (IDiagramModelArchimateObject)diagramModelObject;
			
			children = diagraModelArchimateObject.getChildren();
			archimateElementId = diagraModelArchimateObject.getArchimateElement().getId();	// the element can be located in another project
			archimateElementName = diagraModelArchimateObject.getArchimateElement().getName();
			archimateElementClass = diagraModelArchimateObject.getArchimateElement().getClass().getSimpleName();
			borderType = -1;
			content = null;
			documentation = null;
			type = diagraModelArchimateObject.getType();
			
			dbTabItem.setCountDiagramModelArchimateObjects(++countDiagramModelArchimateObjects);
			dbTabItem.setProgressBar(++countTotal);
			break;
			
		case "DiagramModelGroup" :
			//IDiagramModelGroup diagraModelModelGroup = (DiagramModelGroup)_object;
			IDiagramModelGroup diagraModelModelGroup = (IDiagramModelGroup)diagramModelObject;
			
			children = diagraModelModelGroup.getChildren();
			archimateElementId = null;
			archimateElementName = null;
			archimateElementClass = null;
			borderType = -1;
			content = null;
			documentation = diagraModelModelGroup.getDocumentation();
			type = -1;

			dbTabItem.setCountDiagramModelGroups(++countDiagramModelGroups);
			dbTabItem.setProgressBar(++countTotal);
			break;
			
		case "DiagramModelNote" :
			//IDiagramModelNote diagraModelNote = (IDiagramModelNote)_object;
			IDiagramModelNote diagraModelNote = (IDiagramModelNote)diagramModelObject;
			
			children = null;
			archimateElementId = null;
			archimateElementName = null;
			archimateElementClass = null;
			borderType = diagraModelNote.getBorderType();
			content = diagraModelNote.getContent();
			documentation = null;
			type = -1;
			
			dbTabItem.setCountDiagramModelNotes(++countDiagramModelNotes);
			dbTabItem.setProgressBar(++countTotal);
			break;
			
		default :
			throw new Exception("exportDiagramModelObject() : Don't know how to save " + diagramModelObject.getName() + " (" + diagramModelObject.eClass().getName() + ")");
		}
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO diagrammodelarchimateobject (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, textalignment, targetconnections, archimateelementid, archimateelementname, archimateelementclass, rank, indent, type, class, bordertype, content, documentation, name, x, y, width, height)"
					,DBPlugin.getId(diagramModelObject.getId())
					,DBPlugin.getProjectId(diagramModelObject.getId())
					,DBPlugin.getVersion(diagramModelObject.getId())
					,_parentId
					,diagramModelObject.getFillColor()
					,diagramModelObject.getFont()
					,diagramModelObject.getFontColor()
					,diagramModelObject.getLineColor()
					,diagramModelObject.getLineWidth()
					,diagramModelObject.getTextAlignment()
					,getTargetConnectionsString(diagramModelObject.getTargetConnections())
					,archimateElementId
					,archimateElementName
					,archimateElementClass
					,_rank
					,_indent
					,type
					,diagramModelObject.eClass().getName()
					,borderType
					,content
					,documentation
					,diagramModelObject.getName()
					,diagramModelObject.getBounds().getX()
					,diagramModelObject.getBounds().getY()
					,diagramModelObject.getBounds().getWidth()
					,diagramModelObject.getBounds().getHeight()
					);
		} else {
			//TODO: CQL : replace property "archimateelementid" by "represents" relation to the corresponding archimate element, but before, once must ensure that the archimate element has been exported first
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:diagrammodelarchimateobject {id:?, parent:?, fillcolor:?, font:?, fontcolor:?, linecolor:?, linewidth:?, textalignment:?, targetconnections:?, archimateelementid:?, archimateelementname:?, archimateelementclass:?, rank:?, indent:?, type:?, class:?, bordertype:?, content:?, documentation:?, name:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(diagramModelObject.getId())
					,DBPlugin.getVersion(diagramModelObject.getId())
					,DBPlugin.getId(diagramModelObject.getId())
					,_parentId
					,diagramModelObject.getFillColor()
					,diagramModelObject.getFont()
					,diagramModelObject.getFontColor()
					,diagramModelObject.getLineColor()
					,diagramModelObject.getLineWidth()
					,diagramModelObject.getTextAlignment()
					,getTargetConnectionsString(diagramModelObject.getTargetConnections())
					,archimateElementId
					,archimateElementName
					,archimateElementClass
					,_rank
					,_indent
					,type
					,diagramModelObject.eClass().getName()
					,borderType
					,content
					,documentation
					,diagramModelObject.getName()
					,diagramModelObject.getBounds().getX()
					,diagramModelObject.getBounds().getY()
					,diagramModelObject.getBounds().getWidth()
					,diagramModelObject.getBounds().getHeight()
					);
		}
		
		if ( diagramModelObject.eClass().getName().equals("DiagramModelGroup") ) {
			exportObjectProperties((IDiagramModelGroup)diagramModelObject, ((IDiagramModelGroup)diagramModelObject).getProperties());
		}

		if ( diagramModelObject.getSourceConnections() != null ) {
			for ( int i=0; i < diagramModelObject.getSourceConnections().size(); ++i) {
				exportConnection(diagramModelObject.getId(), diagramModelObject.getSourceConnections().get(i), i);
			}
		}
		
		if ( children != null ) {
			for ( int i=0; i < children.size(); ++i) {
				switch ( children.get(i).eClass().getName() ) {
				case "CanvasModelImage" :
					exportCanvasModelImage(diagramModelObject.getId(), (ICanvasModelImage)children.get(i), i, _indent+1);
					break;
				case "DiagramModelArchimateObject":
				case "DiagramModelGroup" :
				case "DiagramModelNote" :
					exportDiagramModelObject(diagramModelObject.getId(), (IDiagramModelObject)children.get(i), i, _indent+1);
					break;
				case "DiagramModelReference" :
					exportDiagramModelReference(diagramModelObject.getId(), (IDiagramModelReference)children.get(i), i, _indent+1);
					break;
				case "SketchModelActor" :
					exportSketchModelActor(diagramModelObject.getId(), (ISketchModelActor)children.get(i), i, _indent+1);
					break;
				case "SketchModelSticky" : 
					exportSketchModelSticky(diagramModelObject.getId(), (ISketchModelSticky)children.get(i), i, _indent+1);
					break;
				default : 
					throw new Exception("exportDiagramModelArchimateObject : Do not know how to save child "+children.get(i).eClass().getName());
				}
			}
		}

		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportDiagramModelArchimateObject()");
	}

	/**
	 * Exports an IDiagramModelConnection object in the database
	 * @param _parentId
	 * @param _connection
	 * @param _rank
	 * @throws Exception
	 */
	//private void exportConnection(String _parentId, EObject _object, int _rank) throws Exception {
	@SuppressWarnings("deprecation")
	private void exportConnection(String _parentId, IDiagramModelConnection diagramModelConnection, int _rank) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportConnection()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+diagramModelConnection.eClass().getName()+" id="+diagramModelConnection.getId()+" source="+diagramModelConnection.getSource().getId()+" target="+diagramModelConnection.getTarget().getId());
		
		boolean isLocked;
		String relationshipId;
		
		switch ( diagramModelConnection.eClass().getName() ) {
		case "DiagramModelArchimateConnection" :
			isLocked = false;
			relationshipId = ((IDiagramModelArchimateConnection)diagramModelConnection).getRelationship().getId();
			
			dbTabItem.setCountDiagramModelConnections(++countDiagramModelArchimateConnections);
			dbTabItem.setProgressBar(++countTotal);
			
			break;
		case "DiagramModelConnection" :
			isLocked = false;
			relationshipId = null;
			
			dbTabItem.setCountDiagramModelArchimateConnections(++countDiagramModelConnections);
			dbTabItem.setProgressBar(++countTotal);
			break;
		case "CanvasModelConnection" :
			isLocked = ((ICanvasModelConnection)diagramModelConnection).isLocked();
			relationshipId = null;
			
			dbTabItem.setCountCanvasModelConnections(++countCanvasModelConnections);
			dbTabItem.setProgressBar(++countTotal);
			break;
		default : 
			throw new Exception("exportConnection() : Don't know how to save " + diagramModelConnection.getName() + " (" + diagramModelConnection.eClass().getName() + ")");
		}
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO connection (id, model, version, class, documentation, islocked, font, fontcolor, linecolor, linewidth, parent, relationship, source, target, text, textposition, type, rank)"
					,DBPlugin.getId(diagramModelConnection.getId())
					,DBPlugin.getProjectId(diagramModelConnection.getId())
					,DBPlugin.getVersion(diagramModelConnection.getId())
					,diagramModelConnection.eClass().getName()
					,diagramModelConnection.getDocumentation()
					,isLocked
					,diagramModelConnection.getFont()
					,diagramModelConnection.getFontColor()
					,diagramModelConnection.getLineColor()
					,diagramModelConnection.getLineWidth()
					,_parentId
					,relationshipId
					,diagramModelConnection.getSource().getId()
					,diagramModelConnection.getTarget().getId()
					,diagramModelConnection.getText()
					,diagramModelConnection.getTextPosition()
					,diagramModelConnection.getType()
					,_rank
					);
		} else {
			//TODO: convert this node to a relation
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:connection {id:?, class:?, documentation:?, islocked:?, font:?, fontcolor:?, linecolor:?, linewidth:?, parent:?, relationship:?, source:?, target:?, text:?, textposition:?, type:?, rank:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(diagramModelConnection.getId())
					,DBPlugin.getVersion(diagramModelConnection.getId())
					,DBPlugin.getId(diagramModelConnection.getId())
					,diagramModelConnection.eClass().getName()
					,diagramModelConnection.getDocumentation()
					,isLocked
					,diagramModelConnection.getFont()
					,diagramModelConnection.getFontColor()
					,diagramModelConnection.getLineColor()
					,diagramModelConnection.getLineWidth()
					,_parentId
					,relationshipId
					,diagramModelConnection.getSource().getId()
					,diagramModelConnection.getTarget().getId()
					,diagramModelConnection.getText()
					,diagramModelConnection.getTextPosition()
					,diagramModelConnection.getType()
					,_rank
					);
		}
		
		if ( diagramModelConnection.getBendpoints() != null ) {
			int rank=0;
			for ( IDiagramModelBendpoint bendpoint: diagramModelConnection.getBendpoints() ) {
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO bendpoint (parent, model, version, startx, starty, endx, endy, rank)"
							,DBPlugin.getId(diagramModelConnection.getId())
							,DBPlugin.getProjectId(diagramModelConnection.getId())
							,DBPlugin.getVersion(diagramModelConnection.getId())
							,bendpoint.getStartX()
							,bendpoint.getStartY()
							,bendpoint.getEndX()
							,bendpoint.getEndY()
							,++rank
							);
				} else {
					DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:bendpoint {parent:?, startx:?, starty:?, endx:?, endy:?, rank:?}), (new)-[:isInModel]->(m)"
							,DBPlugin.getProjectId(diagramModelConnection.getId())
							,DBPlugin.getVersion(diagramModelConnection.getId())
							,DBPlugin.getId(diagramModelConnection.getId())
							,bendpoint.getStartX()
							,bendpoint.getStartY()
							,bendpoint.getEndX()
							,bendpoint.getEndY()
							,++rank
							);
				}
				++countDiagramModelBendpoints;
				++countTotal;
			}
			dbTabItem.setCountDiagramModelBendpoints(countDiagramModelBendpoints);
			dbTabItem.setProgressBar(countTotal);
		}

		exportObjectProperties(diagramModelConnection, diagramModelConnection.getProperties());
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportConnection()");
	}
	
	/**
	 * Exports a IDiagramModelReference object in the database
	 * @param _parentId
	 * @param _reference
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	private void exportDiagramModelReference(String _parentId, IDiagramModelReference _diagramModelReference, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportDiagramModelReference()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+_diagramModelReference.eClass().getName()+" id="+_diagramModelReference.getId()+" name="+_diagramModelReference.getName());
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO diagrammodelreference (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, textalignment, targetconnections, diagrammodelid, x, y, width, height, rank, indent)"
					,DBPlugin.getId(_diagramModelReference.getId())
					,DBPlugin.getProjectId(_diagramModelReference.getId())
					,DBPlugin.getVersion(_diagramModelReference.getId())
					,_parentId
					,_diagramModelReference.getFillColor()
					,_diagramModelReference.getFont()
					,_diagramModelReference.getFontColor()
					,_diagramModelReference.getLineColor()
					,_diagramModelReference.getLineWidth()
					,_diagramModelReference.getTextAlignment()
					,getTargetConnectionsString(_diagramModelReference.getTargetConnections())
					,_diagramModelReference.getReferencedModel().getId()
					,_diagramModelReference.getBounds().getX()
					,_diagramModelReference.getBounds().getY()
					,_diagramModelReference.getBounds().getWidth()
					,_diagramModelReference.getBounds().getHeight()
					,_rank
					,_indent
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:diagrammodelreference {id:?, parent:?, fillcolor:?, font:?, fontcolor:?, linecolor:?, linewidth:?, textalignment:?, targetconnections:?, diagrammodelid:?, x:?, y:?, width:?, height:?, rank:?, indent:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(_diagramModelReference.getId())
					,DBPlugin.getVersion(_diagramModelReference.getId())
					,DBPlugin.getId(_diagramModelReference.getId())
					,_parentId
					,_diagramModelReference.getFillColor()
					,_diagramModelReference.getFont()
					,_diagramModelReference.getFontColor()
					,_diagramModelReference.getLineColor()
					,_diagramModelReference.getLineWidth()
					,_diagramModelReference.getTextAlignment()
					,getTargetConnectionsString(_diagramModelReference.getTargetConnections())
					,_diagramModelReference.getReferencedModel().getId()
					,_diagramModelReference.getBounds().getX()
					,_diagramModelReference.getBounds().getY()
					,_diagramModelReference.getBounds().getWidth()
					,_diagramModelReference.getBounds().getHeight()
					,_rank
					,_indent
					);
		}
		
		dbTabItem.setCountDiagramModelReferences(++countDiagramModelReferences);
		dbTabItem.setProgressBar(++countTotal);

		if ( _diagramModelReference.getSourceConnections() != null ) {
			for ( int i=0; i < _diagramModelReference.getSourceConnections().size(); ++i) {
				exportConnection(_diagramModelReference.getId(), _diagramModelReference.getSourceConnections().get(i), i);
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportDiagramModelReference()");
	}

	/**
	 * Exports a ICanvasModelBlock object in the database
	 * @param _parentId
	 * @param _block
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	private void exportCanvasModelBlock(String _parentId, ICanvasModelBlock _canvasModelBlock, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportCanvasModelBlock()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+_canvasModelBlock.eClass().getName()+" id="+_canvasModelBlock.getId()+" name="+_canvasModelBlock.getName());
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO canvasmodelblock (id, model, version, parent, bordercolor, content, fillcolor, font, fontcolor, hintcontent, hinttitle, imagepath, imageposition, linecolor, linewidth, islocked, name, textalignment, textposition, targetconnections, rank, indent, x, y, width, height)"
					,DBPlugin.getId(_canvasModelBlock.getId())
					,DBPlugin.getProjectId(_canvasModelBlock.getId())
					,DBPlugin.getVersion(_canvasModelBlock.getId())
					,_parentId
					,_canvasModelBlock.getBorderColor()
					,_canvasModelBlock.getContent()
					,_canvasModelBlock.getFillColor()
					,_canvasModelBlock.getFont()
					,_canvasModelBlock.getFontColor()
					,_canvasModelBlock.getHintContent()
					,_canvasModelBlock.getHintTitle()
					,_canvasModelBlock.getImagePath()
					,_canvasModelBlock.getImagePosition()
					,_canvasModelBlock.getLineColor()
					,_canvasModelBlock.getLineWidth()
					,_canvasModelBlock.isLocked()
					,_canvasModelBlock.getName()
					,_canvasModelBlock.getTextAlignment()
					,_canvasModelBlock.getTextPosition()
					,getTargetConnectionsString(_canvasModelBlock.getTargetConnections())
					,_rank
					,_indent
					,_canvasModelBlock.getBounds().getX()
					,_canvasModelBlock.getBounds().getY()
					,_canvasModelBlock.getBounds().getWidth()
					,_canvasModelBlock.getBounds().getHeight()
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:canvasmodelblock {id:?, parent:?, bordercolor:?, content:?, fillcolor:?, font:?, fontcolor:?, hintcontent:?, hinttitle:?, imagepath:?, imageposition:?, linecolor:?, linewidth:?, islocked:?, name:?, textalignment:?, textposition:?, targetconnections:?, rank:?, indent:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(_canvasModelBlock.getId())
					,DBPlugin.getVersion(_canvasModelBlock.getId())
					,DBPlugin.getId(_canvasModelBlock.getId())
					,_parentId
					,_canvasModelBlock.getBorderColor()
					,_canvasModelBlock.getContent()
					,_canvasModelBlock.getFillColor()
					,_canvasModelBlock.getFont()
					,_canvasModelBlock.getFontColor()
					,_canvasModelBlock.getHintContent()
					,_canvasModelBlock.getHintTitle()
					,_canvasModelBlock.getImagePath()
					,_canvasModelBlock.getImagePosition()
					,_canvasModelBlock.getLineColor()
					,_canvasModelBlock.getLineWidth()
					,_canvasModelBlock.isLocked()
					,_canvasModelBlock.getName()
					,_canvasModelBlock.getTextAlignment()
					,_canvasModelBlock.getTextPosition()
					,getTargetConnectionsString(_canvasModelBlock.getTargetConnections())
					,_rank
					,_indent
					,_canvasModelBlock.getBounds().getX()
					,_canvasModelBlock.getBounds().getY()
					,_canvasModelBlock.getBounds().getWidth()
					,_canvasModelBlock.getBounds().getHeight()
					);
		}
		
		dbTabItem.setCountCanvasModelBlocks(++countCanvasModelBlocks);
		dbTabItem.setProgressBar(++countTotal);
		
		for ( int i=0; i < _canvasModelBlock.getChildren().size(); ++i) {
			EObject child = _canvasModelBlock.getChildren().get(i);
			switch ( child.eClass().getName() ) {
			case "DiagramModelArchimateObject":
				exportDiagramModelObject(_canvasModelBlock.getId(), (IDiagramModelObject)child, i, _indent+1);
				break;
			case "CanvasModelImage" :
				exportCanvasModelImage(_canvasModelBlock.getId(), (ICanvasModelImage)child, i, _indent+1);
				break;
			case "CanvasModelBlock" :
				exportCanvasModelBlock(_canvasModelBlock.getId(), (ICanvasModelBlock)child, i, _indent+1);
				break;
			case "CanvasModelSticky" :
				exportCanvasModelSticky(_canvasModelBlock.getId(), (ICanvasModelSticky)child, i, _indent+1);
				break;
			case "DiagramModelReference" :
				exportDiagramModelReference(_canvasModelBlock.getId(), (IDiagramModelReference)child, i, _indent+1);
				break;
			default :
				throw new Exception("exportCanvasModelBlock : do not know how to export child " + child.eClass().getName());
			}
		}
		if ( _canvasModelBlock.getSourceConnections() != null ) {
			for ( int i=0; i < _canvasModelBlock.getSourceConnections().size(); ++i) {
				exportConnection(_canvasModelBlock.getId(), _canvasModelBlock.getSourceConnections().get(i), i);
			}
		}		
		exportObjectProperties(_canvasModelBlock, _canvasModelBlock.getProperties());
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportCanvasModelBlock()");
	}
	
	/**
	 * Exports a ICanvasModelSticky object in the database
	 * @param _parentId
	 * @param _sticky
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	private void exportCanvasModelSticky(String _parentId, ICanvasModelSticky _canvasModelSticky, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportCanvasModelSticky()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+_canvasModelSticky.eClass().getName()+" id="+_canvasModelSticky.getId()+" name="+_canvasModelSticky.getName());
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO canvasmodelsticky (id, model, version, parent, bordercolor, content, fillcolor, font, fontcolor, imagepath, imageposition, islocked, linecolor, linewidth, notes, name, textalignment, textposition, targetconnections, rank, indent, x, y, width, height)" 
					,DBPlugin.getId(_canvasModelSticky.getId())
					,DBPlugin.getProjectId(_canvasModelSticky.getId())
					,DBPlugin.getVersion(_canvasModelSticky.getId())
					,_parentId
					,_canvasModelSticky.getBorderColor()
					,_canvasModelSticky.getContent()
					,_canvasModelSticky.getFillColor()
					,_canvasModelSticky.getFont()
					,_canvasModelSticky.getFontColor()
					,_canvasModelSticky.getImagePath()
					,_canvasModelSticky.getImagePosition()
					,_canvasModelSticky.isLocked()
					,_canvasModelSticky.getLineColor()
					,_canvasModelSticky.getLineWidth()
					,_canvasModelSticky.getNotes()
					,_canvasModelSticky.getName()
					,_canvasModelSticky.getTextAlignment()
					,_canvasModelSticky.getTextPosition()
					,getTargetConnectionsString(_canvasModelSticky.getTargetConnections())
					,_rank
					,_indent
					,_canvasModelSticky.getBounds().getX()
					,_canvasModelSticky.getBounds().getY()
					,_canvasModelSticky.getBounds().getWidth()
					,_canvasModelSticky.getBounds().getHeight()
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:canvasmodelsticky {id:?, parent:?, bordercolor:?, content:?, fillcolor:?, font:?, fontcolor:?, imagepath:?, imageposition:?, islocked:?, linecolor:?, linewidth:?, notes:?, name:?, textalignment:?, textposition:?, targetconnections:?, rank:?, indent:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)" 
					,DBPlugin.getProjectId(_canvasModelSticky.getId())
					,DBPlugin.getVersion(_canvasModelSticky.getId())
					,DBPlugin.getId(_canvasModelSticky.getId())
					,_parentId
					,_canvasModelSticky.getBorderColor()
					,_canvasModelSticky.getContent()
					,_canvasModelSticky.getFillColor()
					,_canvasModelSticky.getFont()
					,_canvasModelSticky.getFontColor()
					,_canvasModelSticky.getImagePath()
					,_canvasModelSticky.getImagePosition()
					,_canvasModelSticky.isLocked()
					,_canvasModelSticky.getLineColor()
					,_canvasModelSticky.getLineWidth()
					,_canvasModelSticky.getNotes()
					,_canvasModelSticky.getName()
					,_canvasModelSticky.getTextAlignment()
					,_canvasModelSticky.getTextPosition()
					,getTargetConnectionsString(_canvasModelSticky.getTargetConnections())
					,_rank
					,_indent
					,_canvasModelSticky.getBounds().getX()
					,_canvasModelSticky.getBounds().getY()
					,_canvasModelSticky.getBounds().getWidth()
					,_canvasModelSticky.getBounds().getHeight()
					);
		}
		
		dbTabItem.setCountCanvasModelStickys(++countCanvasModelStickys);
		dbTabItem.setProgressBar(++countTotal);
		exportObjectProperties(_canvasModelSticky, _canvasModelSticky.getProperties());

		if ( _canvasModelSticky.getSourceConnections() != null ) {
			for ( int i=0; i < _canvasModelSticky.getSourceConnections().size(); ++i) {
				exportConnection(_canvasModelSticky.getId(), _canvasModelSticky.getSourceConnections().get(i), i);
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportCanvasModelSticky()");
	}
	
	/**
	 * Exports a ICanvasModelImage object in the database
	 * @param _parentId
	 * @param _image
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	private void exportCanvasModelImage(String _parentId, ICanvasModelImage _iCanvasModelImage, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportCanvasModelImage()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+_iCanvasModelImage.eClass().getName()+" id="+_iCanvasModelImage.getId()+" name="+_iCanvasModelImage.getName());
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO canvasmodelimage (id, model, version, parent, bordercolor, islocked, fillcolor, font, fontcolor, imagepath, linecolor, linewidth, name, textalignment, targetconnections, rank, indent, x, y, width, height)" 
					,DBPlugin.getId(_iCanvasModelImage.getId())
					,DBPlugin.getProjectId(_iCanvasModelImage.getId())
					,DBPlugin.getVersion(_iCanvasModelImage.getId())
					,_parentId
					,_iCanvasModelImage.getBorderColor()
					,_iCanvasModelImage.isLocked()
					,_iCanvasModelImage.getFillColor()
					,_iCanvasModelImage.getFont()
					,_iCanvasModelImage.getFontColor()
					,_iCanvasModelImage.getImagePath()
					,_iCanvasModelImage.getLineColor()
					,_iCanvasModelImage.getLineWidth()
					,_iCanvasModelImage.getName()
					,_iCanvasModelImage.getTextAlignment()
					,getTargetConnectionsString(_iCanvasModelImage.getTargetConnections())
					,_rank
					,_indent
					,_iCanvasModelImage.getBounds().getX()
					,_iCanvasModelImage.getBounds().getY()
					,_iCanvasModelImage.getBounds().getWidth()
					,_iCanvasModelImage.getBounds().getHeight()
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:canvasmodelimage {id:?, parent:?, bordercolor:?, islocked:?, fillcolor:?, font:?, fontcolor:?, imagepath:?, linecolor:?, linewidth:?, name:?, textalignment:?, targetconnections:?, rank:?, indent:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)" 
					,DBPlugin.getProjectId(_iCanvasModelImage.getId())
					,DBPlugin.getVersion(_iCanvasModelImage.getId())
					,DBPlugin.getId(_iCanvasModelImage.getId())
					,_parentId
					,_iCanvasModelImage.getBorderColor()
					,_iCanvasModelImage.isLocked()
					,_iCanvasModelImage.getFillColor()
					,_iCanvasModelImage.getFont()
					,_iCanvasModelImage.getFontColor()
					,_iCanvasModelImage.getImagePath()
					,_iCanvasModelImage.getLineColor()
					,_iCanvasModelImage.getLineWidth()
					,_iCanvasModelImage.getName()
					,_iCanvasModelImage.getTextAlignment()
					,getTargetConnectionsString(_iCanvasModelImage.getTargetConnections())
					,_rank
					,_indent
					,_iCanvasModelImage.getBounds().getX()
					,_iCanvasModelImage.getBounds().getY()
					,_iCanvasModelImage.getBounds().getWidth()
					,_iCanvasModelImage.getBounds().getHeight()
					);
		}
		
		dbTabItem.setCountCanvasModelImages(++countCanvasModelImages);
		dbTabItem.setProgressBar(++countTotal);

		if ( _iCanvasModelImage.getSourceConnections() != null ) {
			for ( int i=0; i < _iCanvasModelImage.getSourceConnections().size(); ++i) {
				exportConnection(_iCanvasModelImage.getId(), _iCanvasModelImage.getSourceConnections().get(i), i);
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportCanvasModelImage()");
	}

	/**
	 * exports a ISketchModelSticky object in the database
	 * @param _parentId
	 * @param _sticky
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	private void exportSketchModelSticky(String _parentId, ISketchModelSticky _sketchModelSticky, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportSketchModelSticky()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+_sketchModelSticky.eClass().getName()+" id="+_sketchModelSticky.getId()+" name="+_sketchModelSticky.getName());
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO sketchmodelsticky (id, model, version, parent, content, fillcolor, font, fontcolor, linecolor, linewidth, name, textalignment, targetconnections, rank, indent, x, y, width, height)"
					,DBPlugin.getId(_sketchModelSticky.getId())
					,DBPlugin.getProjectId(_sketchModelSticky.getId())
					,DBPlugin.getVersion(_sketchModelSticky.getId())
					,_parentId
					,_sketchModelSticky.getContent()
					,_sketchModelSticky.getFillColor()
					,_sketchModelSticky.getFont()
					,_sketchModelSticky.getFontColor()
					,_sketchModelSticky.getLineColor()
					,_sketchModelSticky.getLineWidth()
					,_sketchModelSticky.getName()
					,_sketchModelSticky.getTextAlignment()
					,getTargetConnectionsString(_sketchModelSticky.getTargetConnections())
					,_rank
					,_indent
					,_sketchModelSticky.getBounds().getX()
					,_sketchModelSticky.getBounds().getY()
					,_sketchModelSticky.getBounds().getWidth()
					,_sketchModelSticky.getBounds().getHeight()
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:sketchmodelsticky {id:?, parent:?, content:?, fillcolor:?, font:?, fontcolor:?, linecolor:?, linewidth:?, name:?, textalignment:?, targetconnections:?, rank:?, indent:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(_sketchModelSticky.getId())
					,DBPlugin.getVersion(_sketchModelSticky.getId())
					,DBPlugin.getId(_sketchModelSticky.getId())
					,_parentId
					,_sketchModelSticky.getContent()
					,_sketchModelSticky.getFillColor()
					,_sketchModelSticky.getFont()
					,_sketchModelSticky.getFontColor()
					,_sketchModelSticky.getLineColor()
					,_sketchModelSticky.getLineWidth()
					,_sketchModelSticky.getName()
					,_sketchModelSticky.getTextAlignment()
					,getTargetConnectionsString(_sketchModelSticky.getTargetConnections())
					,_rank
					,_indent
					,_sketchModelSticky.getBounds().getX()
					,_sketchModelSticky.getBounds().getY()
					,_sketchModelSticky.getBounds().getWidth()
					,_sketchModelSticky.getBounds().getHeight()
					);
		}
		
		dbTabItem.setCountSketchModelStickys(++countSketchModelStickys);
		dbTabItem.setProgressBar(++countTotal);
		exportObjectProperties(_sketchModelSticky, _sketchModelSticky.getProperties());

		if ( _sketchModelSticky.getSourceConnections() != null ) {
			for ( int i=0; i < _sketchModelSticky.getSourceConnections().size(); ++i) {
				exportConnection(_sketchModelSticky.getId(), _sketchModelSticky.getSourceConnections().get(i), i);
			}
		}

		for ( int i=0; i < _sketchModelSticky.getChildren().size(); ++i) {
			EObject child = _sketchModelSticky.getChildren().get(i);
			switch ( child.eClass().getName() ) {
			case "DiagramModelArchimateObject":
			case "DiagramModelGroup" :
			case "DiagramModelNote" :
				exportDiagramModelObject(_sketchModelSticky.getId(), (IDiagramModelObject)child, i, _indent+1);
				break;
			case "DiagramModelReference" :
				exportDiagramModelReference(_sketchModelSticky.getId(), (IDiagramModelReference)child, i, _indent+1);
				break;
			case "SketchModelSticky" :
				exportSketchModelSticky(_sketchModelSticky.getId(), (ISketchModelSticky)child, i, _indent+1);
				break;
			case "SketchModelActor" :
				exportSketchModelActor(_sketchModelSticky.getId(), (ISketchModelActor)child, i, _indent+1);
				break;
			default :
				throw new Exception("exportSketchSticky : do not know how to export child " + child.eClass().getName());
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportSketchModelSticky()");
	}
	
	/**
	 * exports a ISketchModelActor object in the database
	 * @param _parentId
	 * @param _actor
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 */
	private void exportSketchModelActor(String _parentId, ISketchModelActor _sketchModelActor, int _rank, int _indent) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportSketchModelActor()");
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+_sketchModelActor.eClass().getName()+" id="+_sketchModelActor.getId()+" name="+_sketchModelActor.getName());
				
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO sketchmodelactor (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, name, textalignment, targetconnections, rank, indent, x, y, width, height)"
					,DBPlugin.getId(_sketchModelActor.getId())
					,DBPlugin.getProjectId(_sketchModelActor.getId())
					,DBPlugin.getVersion(_sketchModelActor.getId())
					,_parentId
					,_sketchModelActor.getFillColor()
					,_sketchModelActor.getFont()
					,_sketchModelActor.getFontColor()
					,_sketchModelActor.getLineColor()
					,_sketchModelActor.getLineWidth()
					,_sketchModelActor.getName()
					,_sketchModelActor.getTextAlignment()
					,getTargetConnectionsString(_sketchModelActor.getTargetConnections())
					,_rank
					,_indent
					,_sketchModelActor.getBounds().getX()
					,_sketchModelActor.getBounds().getY()
					,_sketchModelActor.getBounds().getWidth()
					,_sketchModelActor.getBounds().getHeight()
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:sketchmodelactor {id:?, parent:?, fillcolor:?, font:?, fontcolor:?, linecolor:?, linewidth:?, name:?, textalignment:?, targetconnections:?, rank:?, indent:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(_sketchModelActor.getId())
					,DBPlugin.getVersion(_sketchModelActor.getId())
					,DBPlugin.getId(_sketchModelActor.getId())
					,_parentId
					,_sketchModelActor.getFillColor()
					,_sketchModelActor.getFont()
					,_sketchModelActor.getFontColor()
					,_sketchModelActor.getLineColor()
					,_sketchModelActor.getLineWidth()
					,_sketchModelActor.getName()
					,_sketchModelActor.getTextAlignment()
					,getTargetConnectionsString(_sketchModelActor.getTargetConnections())
					,_rank
					,_indent
					,_sketchModelActor.getBounds().getX()
					,_sketchModelActor.getBounds().getY()
					,_sketchModelActor.getBounds().getWidth()
					,_sketchModelActor.getBounds().getHeight()
					);
		}
	
		dbTabItem.setCountSketchModelActors(++countSketchModelActors);
		dbTabItem.setProgressBar(++countTotal);
		exportObjectProperties(_sketchModelActor, _sketchModelActor.getProperties());

		if ( _sketchModelActor.getSourceConnections() != null ) {
			for ( int i=0; i < _sketchModelActor.getSourceConnections().size(); ++i) {
				exportConnection(_sketchModelActor.getId(), _sketchModelActor.getSourceConnections().get(i), i);
			}
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportSketchModelActor()");
	}

	/**
	 * exports a IFolder object in the database
	 * @param _folder
	 * @param _parentId
	 * @param rank
	 * @throws Exception
	 */
	private void exportFolder(IFolder folder, String parentId, int rank) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportFolder()");
		
		// if we are in shared mode, the first level of folder is the name of the project. In this case, we use the parent folder's name and type.
		String folderName = null;
		int folderType = -1;
		try {
			if ( (rank == 0) && (folder.getType().getValue() != 0) ) {
				folderName = ((IFolder)folder.eContainer()).getName();
				folderType = ((IFolder)folder.eContainer()).getType().getValue();
			}
		} catch (ClassCastException e) { } // the parent is not a folder, may be the model ?
		if ( folderName == null )
			folderName = folder.getName();
		if ( folderType == -1 )
			folderType = folder.getType().getValue();
		
		DBPlugin.debug(DebugLevel.Variable, "Exporting "+folder.eClass().getName()+" id="+folder.getId()+" name="+folder.getName()+" type="+folderType);
		
		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO folder (id, model, version, documentation, parent, name, type, rank)"
					,DBPlugin.getId(folder.getId())
					,DBPlugin.getProjectId(folder.getId())
					,DBPlugin.getVersion(folder.getId())
					,folder.getDocumentation()
					,parentId
					,folderName
					,folderType
					,rank
					);
		} else {
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:folder {id:?, documentation:?, parent:?, name:?, type:?, rank:?}), (new)-[:isInModel]->(m)"
					,DBPlugin.getProjectId(folder.getId())
					,DBPlugin.getVersion(folder.getId())
					,DBPlugin.getId(folder.getId())
					,folder.getDocumentation()
					,parentId
					,folderName
					,folderType
					,rank
					);
		}
		
		
		dbTabItem.setCountFolders(++countFolders);
		dbTabItem.setProgressBar(++countTotal);
		exportObjectProperties(folder, folder.getProperties());
		
		++rank;
		for ( IFolder subFolder: folder.getFolders() ) {
			exportFolder(subFolder, folder.getId(), rank);
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportFolder()");
	}

	/**
	 * exports the model's images in the database
	 * @param _dbModel
	 * @throws Exception
	 */
	private void exportImages(DBModel _dbModel) throws Exception {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBExporter.exportImages()");
		
		IArchiveManager archiveMgr = (IArchiveManager)dbModel.getModel().getAdapter(IArchiveManager.class);
		
		for ( String path: archiveMgr.getImagePaths() ) {
			byte[] image = archiveMgr.getBytesFromEntry(path);
			String md5 = DBPlugin.getMD5(image);
			
			ResultSet result;
			if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
				result = DBPlugin.select(db, "SELECT count(*) as nb FROM images WHERE path = ?", path);
			} else {
				result = DBPlugin.select(db, "MATCH (i:images {path:?}) RETURN count(i) as nb", path);
			}
			
			result.next();
			if ( result.getInt("nb") == 0 ) {
				// if the image is not yet in the db, we insert it
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO images (path, md5, image)", path, md5, image);
				} else {
					DBPlugin.request(db, "CREATE (new:images {path:?, md5:?, image:?})", path, md5, image);
				}
				
			} else {
				ResultSet resultMd5;
				// we check if the calculated md5 is different than in the db, then we update it
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					resultMd5 = DBPlugin.select(db, "SELECT md5 FROM images WHERE path = ?", path);
				} else {
					resultMd5 = DBPlugin.select(db, "MATCH (i:images {path:?}) RETURN i.md5 AS md5", path);
				}
				resultMd5.next();
				if ( !md5.equals(resultMd5.getString("md5")) ) {
					if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
						DBPlugin.request(db, "UPDATE images SET md5 = ?, image = ? WHERE path = ?", md5, image, path);
					} else {
						DBPlugin.request(db, "MATCH (i:images {path:?}) set i.md5 = ?, i.image = ?", path, md5, image);
					}
				}
				resultMd5.close();
			}
			result.close();
			dbTabItem.setCountImages(++countImages);
			dbTabItem.setProgressBar(++countTotal);
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBExporter.exportImages()");
	}

	public static String getTargetConnectionsString(EList<IDiagramModelConnection> connections) {
		StringBuilder target = new StringBuilder();
		for ( IDiagramModelConnection connection: connections ) {
			if ( target.length() > 0 )
				target.append(",");
			target.append(connection.getId());
		}
		return target.toString();
	}
}
