/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.IOException;
import org.archicontribs.database.GUI.DBGuiExportModel;
import org.archicontribs.database.model.ArchimateModel;

import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IArchimateModel;

/**
 * Database Model Exporter. This class exports the model components into a central repository (database).
 * 
 * @author Herve Jouin
 */
public class DBExporter implements IModelExporter {
	private static final DBLogger logger = new DBLogger(DBExporter.class);
	
	/**
	 * Exports the model into the database.
	 */
	@Override
	public void export(IArchimateModel archimateModel) throws IOException {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting model "+archimateModel.getName());

		new DBGuiExportModel((ArchimateModel)archimateModel, "Export model");
	}
	
	/*
			// First, we count all the model components and we calculate the MD5 on all elements and all relationships
		display.getActiveShell().setCursor(DBPlugin.CURSOR_WAIT);
		Shell popup = DBPlugin.popup("Please wait while counting model's components"); 
		Job job = new Job("countEObjects") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
				subMonitor.setTaskName("Please wait while counting model's components ...");
				subMonitor.split(1);
				try {
					model.countEObjects();
				} catch (Exception err) {
					exception = err;
					status = Status.CANCEL_STATUS;
				}
				return status;
			}
		};
	
		job.addJobChangeListener(new JobChangeAdapter() {
	    	public void done(IJobChangeEvent event) {
	    		display.asyncExec(new Runnable() {
					@Override
					public void run() {
	    				popup.close();
					}
	    		});				
	    		if (event.getResult().isOK()) {
	        		if ( logger.isDebugEnabled() ) logger.debug("the model has got "+model.getAllElements().size()+" elements and "+model.getAllRelationships().size()+" relationships.");
	        		new DBExportGUI(model).run();
	        		display.getActiveShell().setCursor(DBPlugin.CURSOR_ARROW);
	        	} else {
	    			DBPlugin.popup(Level.FATAL, "Error while counting model's components.", exception);
	        	}
	    		display.asyncExec(new Runnable() {
					@Override
					public void run() {
						display.getActiveShell().setCursor(DBPlugin.CURSOR_ARROW);
					}
	    		});
	    	}
	    });
		job.schedule();
	}	
	
	/*
	public void export(DBArchimateModel model) throws IOException {
		OldDBExporterGUI gui;
		Display display = Display.getCurrent();
		Shell shell = display.getActiveShell();
		boolean anErrorOccurred = false;
		
		shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));
		
		
			// First, we count all the model components and we calculate the MD5 on all elements and all relationships
		if ( logger.isDebugEnabled() ) logger.debug("Counting all components and calculating MD5");
		try {
			model.countEObjects();
			if ( logger.isDebugEnabled() ) logger.debug("the model has got "+model.getAllElements().size()+" elements.");
		} catch (Exception err) {
			DBPlugin.popup(Level.FATAL, "Error while counting components.", err);
			shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
			return;
		}
		
			// Then, we display the window that print out the number of elements and relationships that need to be exported.
			// The constructor returns only when the graphical interface is dismissed
		if ( logger.isDebugEnabled() ) logger.debug("Opening exporter GUI");
		gui = new OldDBExporterGUI(model);			
		
		if ( gui.getExportType() == OldDBExporterGUI.CANCEL ) {
			shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
			return;
		}
		
			// We get the DBDatabase class instance corresponding to the database name set in the GUI
		DBDatabase database = DBDatabase.getDBDatabase(gui.getDatabaseName());
		if ( database == null ) {
			DBPlugin.popup(Level.FATAL, "Cannot find database \""+gui.getDatabaseName()+"\" in the preferences store.\n\nPlease open the preference page and check your database configuration.");
			shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
			return;
		}
		
		String dbLanguage = database.getLanguage();
		if ( !dbLanguage.equals("SQL") ) {
			DBPlugin.popup(Level.FATAL, "At the moment, the plugin handles only relational databases.");
			shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
			return;
		}
		
		try {
			database.openConnection();
			database.check();
			database.setAutoCommit(false);
		} catch (Exception err) {
			DBPlugin.popup(Level.FATAL, "Error while connecting to the database.", err);
			shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
			return;
		}
		

		
		Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		String owner = System.getProperty("user.name");
		
			// we export the components, storing the conflicts in a list in order to resolve the conflicts later on.
		try {
			//TODO : create a progressbar
			Iterator<EObject> iteratorNew = model.getNewElements().values().iterator();
			Iterator<EObject> iteratorUpdated = model.getUpdatedElements().values().iterator();
			while ( iteratorNew.hasNext() || iteratorUpdated.hasNext() ) {
				IArchimateElement archimateElement = (IArchimateElement)(iteratorNew.hasNext() ? iteratorNew.next() : iteratorUpdated.next());
				String clazz = archimateElement.getClass().getSimpleName();
				
				boolean shouldInsertInDatabase = true;
				while ( shouldInsertInDatabase ) {				
					if ( logger.isDebugEnabled() ) logger.debug("Exporting "+clazz+" \""+archimateElement.getName()+"\" ("+archimateElement.getId()+")");
					String id = archimateElement.getId();
					DBMetadata componentMetadata = ((IHasDBMetadata)archimateElement).getDBMetadata();
					int newVersion = componentMetadata.getCurrentVersion()+1;
					
					try {		//TODO : use preparedstatement
							// 1 - create or update the component in the database
						if ( newVersion == 1 ) {
							database.insert("INSERT INTO components (component_id, class, latest_version, created_by, created_on)",id ,clazz ,newVersion ,owner, now);
						} else {
							database.request("UPDATE components set latest_version = ? WHERE component_id = ?",newVersion, id);
						}
						
							// 2 - create a new version of the component in the database
						database.insert("INSERT INTO components_versions (component_id, component_version, created_by, created_on, name, documentation, checksum)"
								,id
								,newVersion
								,owner
								,now
								,archimateElement.getName()
								,archimateElement.getDocumentation()
								,componentMetadata.getCurrentChecksum()
								);
						
							// 3 - create the component properties in the database
						for ( int rank = 0 ; rank < archimateElement.getProperties().size(); ++rank) {
								// TODO : use one insert with many values
							IProperty prop = archimateElement.getProperties().get(rank);
							database.insert("INSERT INTO properties (parent_id, parent_version, rank, name, value)"
									,id
									,newVersion
									,rank
									,prop.getKey()
									,prop.getValue()
									);
						}
						
						componentMetadata.setInitialChecksum(componentMetadata.getCurrentChecksum());
						componentMetadata.setCurrentVersion(newVersion);
						shouldInsertInDatabase = false;
					} catch (SQLException err) {
							// if the SQL exception is not linked to a primary key violation, then we escalate the exception
							// unfortunately, this is constructor dependent; worst, it may change from one driver version to another version
						if (	(err.getSQLState()!=null && !err.getSQLState().startsWith("23")) 				// generic error
								|| (database.getDriver().equals("sqlite") && err.getErrorCode() != 19)			// specific error from SQLite driver
								) {
								//TODO : ajouter preference pour permettre de continuer en cas d'erreur sur un composant
							anErrorOccurred = true;
							database.rollback();
							throw err;
						}
							// If we're here, that means that there is a conflict that has to be solved
						DBResolveConflict conflict = new DBResolveConflict(database, archimateElement);
						shouldInsertInDatabase = conflict.shouldInsertInDatabase();
						if ( !shouldInsertInDatabase ) {
							componentMetadata.setInitialChecksum(componentMetadata.getCurrentChecksum());
							componentMetadata.setCurrentVersion(newVersion);
						}
					}
				}
				//TODO : create a progressbar
				database.commit();
			}
		} catch (Exception err) {
			//TODO : db.rollback();
			DBPlugin.popup(Level.ERROR, "Failed to export in the database !!!", err);
			shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
			try {
				database.close();
			} catch (SQLException ignore) {	}
			return;
		}
		
		if ( anErrorOccurred ) {
			DBPlugin.popup(Level.ERROR, "Errors occured during the export, your data may be odd.\n\nPlease check your database with care.");
		} else {
			DBPlugin.popup(Level.INFO, "Export successful.");
		}
		
		shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
		
		/*
		dbSelectModel = new DBSelectModel();
		try {
			//TODO : db should be return in the newInfoAboutModel list !!!
			db = dbSelectModel.selectModelToExport(model);
		} catch (Exception err) {
			DBPlugin.popup(Level.ERROR, "An error occurred !!!", err);
			return;
		}

		if ( db == null ) {
			// if there is no database connection, we cannot export
			return;
		}

		List<HashMap<String, String>> modelList = dbSelectModel.getSelectedModels();
		if ( modelList == null || modelList.size() == 0 ) {
			// if the user clicked on cancel or did not selected any project to export, we give up 
			try { db.close(); } catch (SQLException ee) { };
			return;
		}

		HashMap<String, String> newInfoAboutModel = modelList.get(0);

		//TODO : check if the model is checkedin in the getSelectedModels class !!!!
		
		// Showing up the DBProgress window and create the tab corresponding to the model to export
		dbProgress = new DBProgress();
		dbTabItem = dbProgress.tabItem(newInfoAboutModel.get("name"));

		try {
			// we start a transaction
			db.setAutoCommit(false);

			// we check if the model version already exists in the database
			// TODO : do not delete anything, just do a conflict resolution
			// TODO : do not delete anything, just do a conflict resolution
			// TODO : do not delete anything, just do a conflict resolution
			// TODO : do not delete anything, just do a conflict resolution
			// TODO : do not delete anything, just do a conflict resolution
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!
			// TODO : shoudn't do it here ... the conflict should have been seen in the export popup !!!

			long startTime = System.currentTimeMillis();

			// we change the name and purpose in case they've been changed in the ChooseModel window
			//TODO : do not do it : the user can change it in the Archi window !!!
			//TODO : simplify the export popup
			/*
			 * if ( !model.getName().equals(newInfoAboutModel.get("name")) ) model.setName(newInfoAboutModel.get("name"));
			 * if ( !model.getPurpose().equals(newInfoAboutModel.get("purpose")) ) model.set)Purpose(newInfoAboutModel.get("purpose"));
			 * /
			//model.setOwner(modelSelected.get("owner"));
/*
			dbTabItem.setText("Please wait while counting elements to export ...");
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			

			//dbTabItem.setMaximum(countTotal);

			// we remove the old components (if any) from the database
			dbTabItem.setText("Please wait while cleaning up database ...");

			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			//TODO : REMOVE ONLY GRAPHICAL COMPONENTS, NOT ELEMENTS NOR RELATIONSHIPS !!!!!!!!!!!!!!!!!!!!!
			if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
				for(String table: DBPlugin.allSQLTables ) {
					DBPlugin.request(db, "DELETE FROM "+table+" WHERE model = ? AND version = ?"
							,model.getId()
							,newInfoAboutModel.get("version")
							);
				}
			} else {
				DBPlugin.request(db, "MATCH (node)-[rm:isInModel]->(model:model {model:?, version:?}) DETACH DELETE node, model"
						,model.getId()
						,newInfoAboutModel.get("version")
						);
				DBPlugin.request(db, "MATCH (prop:property {model:?, version:?}) DETACH DELETE prop"
						,model.getId()
						,newInfoAboutModel.get("version")
						);
			}

			dbTabItem.setText("Please wait while exporting model information ...");
			if ( logger.isDebugEnabled() ) logger.debug("Exporting model id="+model.getId()+" version="+newInfoAboutModel.get("version")+" name="+newInfoAboutModel.get("name"));
			if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
				DBPlugin.insert(db, "INSERT INTO model (model, version, name, purpose, owner, period, note)"
						,model.getId()
						,newInfoAboutModel.get("version")
						,newInfoAboutModel.get("name")
						,newInfoAboutModel.get("purpose")
						,newInfoAboutModel.get("owner")
						,new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())
						,newInfoAboutModel.get("note")
						);
			} else {
				DBPlugin.request(db, "CREATE (new:model {model:?, version:?, name:?, purpose:?, owner:?, period:?, note:?})"
						,model.getId()
						,newInfoAboutModel.get("version")
						,newInfoAboutModel.get("name")
						,newInfoAboutModel.get("purpose")
						,newInfoAboutModel.get("owner")
						,new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())
						,newInfoAboutModel.get("note")
						);
			}


			// we save the images
			dbTabItem.setText("Please wait while exporting images ...");
			//exportImages(model);

			// we save the folders
			dbTabItem.setText("Please wait while exporting folders ...");
			//for (IFolder folder: model.getFolders() ) {
			//	exportFolder(folder, null, 0);
			//}

			/*
			
			
			// we save the views
			dbTabItem.setText("Please wait while exporting views ...");
			for ( IArchimateDiagramModel archimateDiagramModel: allArchimateDiagramModels ) {
				if ( logger.isDebugEnabled() ) logger.debug("Exporting archimateDiagramModel id="+archimateDiagramModel.getId()+" name="+archimateDiagramModel.getName());
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO archimatediagrammodel (id, model, version, name, documentation, connectionroutertype, viewpoint, type, folder)"
							,archimateDiagramModel.getId()
							,projectId
							,version
							,archimateDiagramModel.getName()
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
							,archimateDiagramModel.getId()
							,archimateDiagramModel.getName()
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
			}

			for (IFolder f: getFolders() ) {
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
							if ( logger.isDebugEnabled() ) logger.debug("Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
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
							if ( logger.isDebugEnabled() ) logger.debug("Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
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
						case "ApplicationEvent" :			// Archi 4
						case "ApplicationFunction" :
						case "ApplicationInteraction" :
						case "ApplicationInterface" :
						case "ApplicationProcess" :			// Archi 4
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
						case "Capability" :					// Archi 4
						case "CommunicationNetwork" :		// Archi 4
						case "CommunicationPath" :
						case "Constraint" :
						case "Contract" :
						case "CourseOfAction" :				// Archi 4
						case "DataObject" :
						case "Deliverable" :
						case "Device" :
						case "DistributionNetwork" :		// Archi 4
						case "Driver" :
						case "Equipment" :					// Archi 4
						case "Facility" :					// Archi 4
						case "Gap" :
						case "Goal" :
						case "Grouping" :					// Archi 4
						case "ImplementationEvent" :		// Archi 4
						case "InfrastructureFunction" :
						case "InfrastructureInterface" :
						case "InfrastructureService" :
						case "Junction" :
						case "Location" :
						case "Material" :					// Archi 4
						case "Meaning" :
						case "Network" :
						case "Node" :
						case "OrJunction" :
						case "Outcome" :					// Archi 4
						case "Path" :						// Archi 4
						case "Plateau" :
						case "Principle" :
						case "Product" :
						case "Representation" :
						case "Requirement" :
						case "Resource" :					// Archi 4
						case "Stakeholder" :
						case "TechnologyCollaboration" :	// Archi 4
						case "TechnologyEvent" :			// Archi 4
						case "TechnologyFunction" :			// Archi 4
						case "TechnologyInteraction" :		// Archi 4
						case "TechnologyInterface" :		// Archi 4
						case "TechnologyProcess" :			// Archi 4
						case "TechnologyService" :			// Archi 4
						case "SystemSoftware" :
						case "Value" :
						case "WorkPackage" :
							IArchimateElement archimateElement = (IArchimateElement)objectToExport;
							if ( logger.isDebugEnabled() ) logger.debug("Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
							if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
								DBPlugin.insert(db, "INSERT INTO archimateelement (id, model, version, name, type, documentation, folder)"
										,id
										,projectId
										,version
										,name
										,archimateElement.getClass().getSimpleName()
										,archimateElement.getDocumentation()
										,((IIdentifier)archimateElement.eContainer()).getId()
										);
							} else {
								DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:archimateelement {id:?, name:?, type:?, documentation:?, folder:?}), (new)-[:isInModel]->(m)"
										,projectId
										,version
										,id
										,name
										,archimateElement.getClass().getSimpleName()
										,archimateElement.getDocumentation()
										,((IIdentifier)archimateElement.eContainer()).getId()
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
						case "DependencyRelationship" :				// Archi 4
						case "FlowRelationship" :
						case "InfluenceRelationship" :
							//case "RealisationRelationship" :			// Archi 3
						case "RealizationRelationship" :			// Archi 4
						case "ServingRelationship" :				// Archi 4
							//case "SpecialisationRelationship" :		// Archi 3
						case "SpecializationRelationship" :			// Archi 4
						case "TriggeringRelationship" :
							//case "UsedByRelationship" :				// Archi 3
							IArchimateRelationship relationship = (IArchimateRelationship)objectToExport;
							if ( logger.isDebugEnabled() ) logger.debug("Exporting "+objectToExport.eClass().getName()+" id="+id+" name="+name);
							if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
								DBPlugin.insert(db, "INSERT INTO relationship (id, model, version, name, source, target, type, documentation, folder, accesstype, strength)"
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
												,objectToExport.eClass().getName().equals("InfluenceRelationship") ? ((IInfluenceRelationship)objectToExport).getStrength() : ""
										);
							} else {
								DBPlugin.request(db, "MATCH (m:model {model:?, version:?}), (source {id:?})-[:isInModel]->(m) MATCH (target {id:?})-[:isInModel]->(m) CREATE (source)-[new:"+relationship.getClass().getSimpleName()+" {id:?, name:?, type:?, documentation:?, folder:?, accesstype:?, strength:?}]->(target)"
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
												,objectToExport.eClass().getName().equals("InfluenceRelationship") ? ((IInfluenceRelationship)objectToExport).getStrength() : ""
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
						model.getId(), dbModel.getVersion());
			} else {
				DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) SET m.countmetadatas=?, m.countfolders=?, m.countelements=?, m.countrelationships=?, m.countproperties=?, m.countarchimatediagrammodels=?, m.countdiagrammodelarchimateobjects=?, m.countdiagrammodelarchimateconnections=?, m.countdiagrammodelgroups=?, m.countdiagrammodelnotes=?, m.countcanvasmodels=?, m.countcanvasmodelblocks=?, m.countcanvasmodelstickys=?, m.countcanvasmodelconnections=?, m.countcanvasmodelimages=?, m.countsketchmodels=?, m.countsketchmodelactors=?, m.countsketchmodelstickys=?, m.countdiagrammodelconnections=?, m.countdiagrammodelbendpoints=?, m.countdiagrammodelreferences=?, m.countimages=?",
						model.getId(), dbModel.getVersion(),
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
			
					/*
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
		dbProgress.finish();
* /
			/*
		} catch (Exception e) {
			try {
				db.rollback();
				db.setAutoCommit(true);
			} catch (Exception ignore) {
				// do nothing as if the transaction has not been committed, it will be rolled back by the database anyway
			}
			DBPlugin.popup(Level.ERROR, "An error occured while exporting your model to the database.\n\nThe transaction has been rolled back and the database is left unmodified.", e);
			dbTabItem.setError("An error occured while exporting your model to the database.\nThe transaction has been rolled back and the database is left unmodified.\n" + e.getClass().getSimpleName() + " : " + e.getMessage());
		}

		try { db.close(); } catch (SQLException e) {}
		* /
	}





	/*************************************************************************************************************** /

	/**
	 * Export the object's properties to the database
	 * @param _dbObject
	 * @throws Exception
	 * /
	/*private void exportObjectProperties(EObject parent, EList<IProperty> properties) throws Exception {
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
	}
	* /

	/**
	 * Exports the model's properties and metadata to the database
	 * @param _dbModel
	 * @throws Exception
	 * /
	/*private void exportModelProperties(DBModel _dbModel) throws Exception {
		// exports IProperty objects
		if ( _dbModel.getProperties() != null ) {
			int rank=0;
			for(IProperty property: _dbModel.getProperties() ) {
				if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
					DBPlugin.insert(db, "INSERT INTO property (id, parent, model, version, name, value)"
							,++rank
							,_model.getId()
							,_model.getId()
							,_dbModel.getVersion()
							,property.getKey()
							,property.getValue()
							);
				} else {
					DBPlugin.request(db, "CREATE (prop:property {id:?, parent:?, model:?, version:?, name:?, value:?})"
							,++rank
							,_model.getId()
							,_model.getId()
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
							,model.getId()
							,_model.getId()
							,_dbModel.getVersion()
							,property.getKey()
							,property.getValue()
							);
				} else {
					DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (prop:property {id:?, name:?, value:?}), (m)-[:hasProperty]->(prop)"
							,model.getId()
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
	}
	* /

	/**
	 * Exports a diagramModelObject object to the database
	 * @param _parentId
	 * @param _object
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	//private void exportDiagramModelObject(String _parentId, EObject _object, int _rank, int _indent) throws Exception {
	/*private void exportDiagramModelObject(String _parentId, IDiagramModelObject diagramModelObject, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+diagramModelObject.eClass().getName()+" id="+diagramModelObject.getId()+" name="+diagramModelObject.getName());

		//IDiagramModelObject diagramModelObject = (IDiagramModelObject)_object;
		String archimateElementId;
		String archimateElementName;
		String archimateElementClass;
		int borderType;
		String content;
		String documentation;
		int type;
		int textPosition;						// Archi 4
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
			textPosition = diagraModelArchimateObject.getTextPosition();		// Archi 4

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
			textPosition = -1;												// Archi 4

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
			textPosition = diagraModelNote.getTextPosition();				// Archi 4

			dbTabItem.setCountDiagramModelNotes(++countDiagramModelNotes);
			dbTabItem.setProgressBar(++countTotal);
			break;

		default :
			throw new Exception("exportDiagramModelObject() : Don't know how to save " + diagramModelObject.getName() + " (" + diagramModelObject.eClass().getName() + ")");
		}

		if ( dbSelectModel.getDbLanguage().equals("SQL") ) {
			DBPlugin.insert(db, "INSERT INTO diagrammodelarchimateobject (id, model, version, parent, fillcolor, font, fontcolor, linecolor, linewidth, textalignment, textposition, targetconnections, archimateelementid, archimateelementname, archimateelementclass, rank, indent, type, class, bordertype, content, documentation, name, x, y, width, height)"
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
					,textPosition											// Archi 4
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
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:diagrammodelarchimateobject {id:?, parent:?, fillcolor:?, font:?, fontcolor:?, linecolor:?, linewidth:?, textalignment:?, textposition:?, targetconnections:?, archimateelementid:?, archimateelementname:?, archimateelementclass:?, rank:?, indent:?, type:?, class:?, bordertype:?, content:?, documentation:?, name:?, x:?, y:?, width:?, height:?}), (new)-[:isInModel]->(m)"
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
					,textPosition										// Archi 4
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
	}
	* /

	/**
	 * Exports an IDiagramModelConnection object in the database
	 * @param _parentId
	 * @param _connection
	 * @param _rank
	 * @throws Exception
	 * /
	//private void exportConnection(String _parentId, EObject _object, int _rank) throws Exception {
	/*@SuppressWarnings("deprecation")
	private void exportConnection(String _parentId, IDiagramModelConnection diagramModelConnection, int _rank) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+diagramModelConnection.eClass().getName()+" id="+diagramModelConnection.getId()+" source="+diagramModelConnection.getSource().getId()+" target="+diagramModelConnection.getTarget().getId());

		boolean isLocked;
		String relationshipId;

		switch ( diagramModelConnection.eClass().getName() ) {
		case "DiagramModelArchimateConnection" :
			isLocked = false;
			relationshipId = ((IDiagramModelArchimateConnection)diagramModelConnection).getArchimateRelationship().getId();

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
			DBPlugin.insert(db, "INSERT INTO connection (id, model, version, class, documentation, islocked, font, fontcolor, linecolor, linewidth, parent, relationship, source, target, targetconnections, text, textposition, type, rank)"
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
					,getTargetConnectionsString(diagramModelConnection.getTargetConnections())			// Archi 4
					,diagramModelConnection.getText()
					,diagramModelConnection.getTextPosition()
					,diagramModelConnection.getType()
					,_rank
					);
		} else {
			//TODO: convert this node to a relation
			DBPlugin.request(db, "MATCH (m:model {model:?, version:?}) CREATE (new:connection {id:?, class:?, documentation:?, islocked:?, font:?, fontcolor:?, linecolor:?, linewidth:?, parent:?, relationship:?, source:?, target:?, targetconnections:?, text:?, textposition:?, type:?, rank:?}), (new)-[:isInModel]->(m)"
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
					,getTargetConnectionsString(diagramModelConnection.getTargetConnections())			// Archi 4
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

		if ( diagramModelConnection.getSourceConnections() != null ) {							// Archi 4
			for ( int i=0; i < diagramModelConnection.getSourceConnections().size(); ++i) {
				exportConnection(diagramModelConnection.getId(), diagramModelConnection.getSourceConnections().get(i), i);
			}
		}

		exportObjectProperties(diagramModelConnection, diagramModelConnection.getProperties());
	}
	* /

	/**
	 * Exports a IDiagramModelReference object in the database
	 * @param _parentId
	 * @param _reference
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	/*private void exportDiagramModelReference(String _parentId, IDiagramModelReference _diagramModelReference, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+_diagramModelReference.eClass().getName()+" id="+_diagramModelReference.getId()+" name="+_diagramModelReference.getName());

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
	}
	* /

	/**
	 * Exports a ICanvasModelBlock object in the database
	 * @param _parentId
	 * @param _block
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	/*private void exportCanvasModelBlock(String _parentId, ICanvasModelBlock _canvasModelBlock, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+_canvasModelBlock.eClass().getName()+" id="+_canvasModelBlock.getId()+" name="+_canvasModelBlock.getName());

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
	}
	* /

	/**
	 * Exports a ICanvasModelSticky object in the database
	 * @param _parentId
	 * @param _sticky
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	/*private void exportCanvasModelSticky(String _parentId, ICanvasModelSticky _canvasModelSticky, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+_canvasModelSticky.eClass().getName()+" id="+_canvasModelSticky.getId()+" name="+_canvasModelSticky.getName());

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
	}
	* /

	/**
	 * Exports a ICanvasModelImage object in the database
	 * @param _parentId
	 * @param _image
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	/*private void exportCanvasModelImage(String _parentId, ICanvasModelImage _iCanvasModelImage, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+_iCanvasModelImage.eClass().getName()+" id="+_iCanvasModelImage.getId()+" name="+_iCanvasModelImage.getName());

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
	}
	* /

	/**
	 * exports a ISketchModelSticky object in the database
	 * @param _parentId
	 * @param _sticky
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	/*private void exportSketchModelSticky(String _parentId, ISketchModelSticky _sketchModelSticky, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+_sketchModelSticky.eClass().getName()+" id="+_sketchModelSticky.getId()+" name="+_sketchModelSticky.getName());

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
	}
	* /

	/**
	 * exports a ISketchModelActor object in the database
	 * @param _parentId
	 * @param _actor
	 * @param _rank
	 * @param _indent
	 * @throws Exception
	 * /
	/*private void exportSketchModelActor(String _parentId, ISketchModelActor _sketchModelActor, int _rank, int _indent) throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+_sketchModelActor.eClass().getName()+" id="+_sketchModelActor.getId()+" name="+_sketchModelActor.getName());

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
	}
	* /

	/**
	 * exports a IFolder object in the database
	 * @param _folder
	 * @param _parentId
	 * @param rank
	 * @throws Exception
	 * /
	/*private void exportFolder(IFolder folder, String parentId, int rank) throws Exception {
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

		if ( logger.isDebugEnabled() ) logger.debug("Exporting "+folder.eClass().getName()+" id="+folder.getId()+" name="+folder.getName()+" type="+folderType);

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
	}
	* /

	/**
	 * exports the model's images in the database
	 * @param _dbModel
	 * @throws Exception
	 * /
	/*private void exportImages(IArchimateModel model) throws Exception {
		IArchiveManager archiveMgr = (IArchiveManager)model.getAdapter(IArchiveManager.class);

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
	}
	* /

	/*public static String getTargetConnectionsString(EList<IDiagramModelConnection> connections) {
		StringBuilder target = new StringBuilder();
		for ( IDiagramModelConnection connection: connections ) {
			if ( target.length() > 0 )
				target.append(",");
			target.append(connection.getId());
		}
		return target.toString();
	}
	*/
}
