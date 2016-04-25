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
import java.util.Iterator;

import org.archicontribs.database.DatabasePlugin.Level;
import org.archicontribs.database.DatabasePlugin.Mode;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.model.IModelImporter;
import com.archimatetool.editor.model.ISelectedModelImporter;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.IRelationship;
import com.archimatetool.model.impl.ArchimateDiagramModel;
import com.archimatetool.model.impl.DiagramModelArchimateConnection;
import com.archimatetool.model.impl.DiagramModelArchimateObject;
import com.archimatetool.model.impl.DiagramModelGroup;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Import from PostGreSQL
 * 
 * @author Hervé JOUIN
 * 
 * version 1.0 : 25/03/2016
 * 		plugin creation
 */

//TODO: manage roles
//	admin : allow to manage system tables
//	architect : allow to create or update models
//	viewer : allow to read models
//TODO: manage several languages
public class MyModelImporter implements IModelImporter, ISelectedModelImporter {

	@Override
	public void doImport() throws IOException {
		doImport(null);
	}

	@Override
	public void doImport(IArchimateModel model) throws IOException {
		if ( model != null && ! MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DatabasePlugin.pluginTitle, "Please be aware that, as you already have a model opened, you won't be able to save modifications done to this model.\n\rAre you sure you wish to import a new model ?") ) {
			return;
		}

		Connection db = new ChooseDatabase().open();
		if ( db == null) return;

		ResultSet result = null;
		try {
			result = new ChooseModel().open(db, Mode.Import);
		} catch (SQLException e) {
			DatabasePlugin.popup(Level.Error, "Cannot retreive the list of existing models from the database.", e);
			try { db.close(); } catch (Exception ee) {}
			return;
		}

		if ( result != null ) {
			Boolean importProperties;
			try {
				if (model != null ) {
					// we deny to import a model twice because we don't know which components have been modified and which not
					if ( result.getString("id").trim().equals(model.getId()) ) {
						//TODO: check for all the models previously imported (not only the active one) ...
						DatabasePlugin.popup(Level.Error, "You cannot import again model.\n\nIn case you wish to discard your work and reimport the model from the database, you need to close the model and import it in a new one.");
						try { db.close(); } catch (Exception ee) {}
						return;
					}
					// we do not import the project properties as the project is not the active one
					importProperties = false;
				} else {
					//Utils.message(Level.INFO, "Creating new model ["+item[0]+"] "+item[1]);
					model = IEditorModelManager.INSTANCE.createNewModel();
					// we remove the "default view" diagram
					model.getFolder(FolderType.DIAGRAMS).getElements().clear();
					importProperties = true;
				}

				// we import the model objects
				importModel(db, model, result.getString("id").trim(), importProperties);
			} catch (SQLException e) {
				DatabasePlugin.popup(Level.Error, "An error occured while importing the model from the database.\n\nThe model imported into Archi might be incomplete !!!", e);
				try { db.close(); } catch (Exception ee) {}
				return;
			}
		}
		try { db.close(); } catch (Exception ee) {}
	}


	private void importModel(Connection db, IArchimateModel model, String modelId, Boolean importProperties) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT count(*) FROM Model where id = ?", modelId);
		result.next();
		int nb = result.getInt(1);
		if ( nb == 0 ) { result.close(); throw new SQLException("Model '"+modelId+"' not found in the database !"); }
		if ( nb  > 1 ) { result.close(); throw new SQLException("Several models '"+modelId+"' found in the database "); }
		result.close();

		if ( importProperties ) {
			// in order to remember that the model is imported and therefore that it may be exported, the model ID is codified to 'id'-'id'

			model.setId(modelId+"-"+modelId);

			result = DatabasePlugin.select(db, "SELECT * FROM Model where id = ?", modelId);
			result.next();
			model.setName(result.getString("name").trim());
			if ( result.getString("purpose") != null ) model.setPurpose(result.getString("purpose").trim());
			result.close();

			importProperties(db, model.getId(), model.getProperties());
			//TODO: manage lock, owner, checkin, creation and modification dates, etc ...
		}

		importArchimateElement(db, model, modelId);
		importRelationship(db,model, modelId);
		importArchimateDiagramModel(db, model, modelId);
		importDiagramModelGroup(db, model, modelId);
		importDiagramModelArchimateObject(db, model, modelId);
		for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();) {
			EObject eObject = iter.next();
			if ( eObject instanceof DiagramModelArchimateObject && ((DiagramModelArchimateObject)eObject).getId().matches(modelId+"-.*") ) {
				importSourceConnections(db, model, (DiagramModelArchimateObject)eObject);
			}
		}
		importTargetConnections(db, model, modelId);
		//TODO: import all the other types modelgroup, folders, images, etc ...)
		DatabasePlugin.popup(Level.Info, "Import done.");
	}
	private void importArchimateElement(Connection db, IArchimateModel model, String modelId) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT * FROM ArchimateElement WHERE id like ?", modelId+"-%");
		while(result.next()) {
			IArchimateElement element = (IArchimateElement)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type").trim()));
			element.setId(result.getString("id").trim());
			element.setName(result.getString("name").trim());
			if ( result.getString("documentation") != null ) element.setDocumentation(result.getString("documentation").trim());

			importProperties(db, element.getId(), element.getProperties());
			model.getDefaultFolderForElement(element).getElements().add(element);
		}
	}
	private void importRelationship(Connection db, IArchimateModel model, String modelId) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT * FROM Relationship WHERE id like ?", modelId+"-%");
		while(result.next()) {
			IRelationship relation = (IRelationship)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type")));
			relation.setId(result.getString("id").trim());
			relation.setSource((IArchimateElement)ArchimateModelUtils.getObjectByID(model, result.getString("source").trim()));
			relation.setTarget((IArchimateElement)ArchimateModelUtils.getObjectByID(model, result.getString("target").trim()));
			relation.setName(result.getString("name").trim());
			if ( result.getString("documentation")!= null ) relation.setDocumentation(result.getString("documentation").trim());

			importProperties(db, relation.getId(), relation.getProperties());
			model.getDefaultFolderForElement(relation).getElements().add(relation);
		}
	}
	private void importArchimateDiagramModel(Connection db, IArchimateModel model, String modelId) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT * FROM ArchimateDiagramModel WHERE id like ?", modelId+"-%");
		while(result.next()) {
			IDiagramModel diagram = (IDiagramModel)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier(result.getString("type")));
			diagram.setId(result.getString("id").trim());
			diagram.setName(result.getString("name").trim());
			if ( result.getString("documentation") != null ) diagram.setDocumentation(result.getString("documentation").trim());
			importProperties(db, diagram.getId(), diagram.getProperties());
			model.getDefaultFolderForElement(diagram).getElements().add(diagram);
		}
	}
	private void importDiagramModelGroup(Connection db, IArchimateModel model, String modelId) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT * FROM DiagramModelGroup WHERE id like ?", modelId+"-%");
		while(result.next()) {
			IDiagramModelGroup grp = (IDiagramModelGroup)IArchimateFactory.eINSTANCE.create((EClass)IArchimatePackage.eINSTANCE.getEClassifier("DiagramModelGroup"));
			ArchimateDiagramModel parent = (ArchimateDiagramModel)ArchimateModelUtils.getObjectByID(model,result.getString("parent").trim());
			if ( parent == null ) {
				DatabasePlugin.popup(Level.Error, "Cannot import model group ("+result.getString("id").trim()+") as we do not know its parent ("+result.getString("parent").trim()+")");
				break;
			}
			grp.setId(result.getString("id").trim());
			grp.setName(result.getString("name").trim());
			if ( result.getString("documentation") != null ) grp.setDocumentation(result.getString("documentation"));
			if ( result.getString("fillcolor") != null )     grp.setDocumentation(result.getString("fillcolor"));
			if ( result.getString("font") != null )          grp.setDocumentation(result.getString("font"));
			if ( result.getString("fontcolor") != null )     grp.setDocumentation(result.getString("fontcolor"));
			if ( result.getString("linecolor") != null )     grp.setDocumentation(result.getString("linecolor"));
			grp.setLineWidth(result.getInt("linewidth"));
			grp.setTextAlignment(result.getInt("textalignment"));
			
			importPoint(db, grp);
			importProperties(db, grp.getId(), grp.getProperties());

			//TODO: importDiagramModelArchimateConnection(db, grp.getSourceConnections());
			//TODO: import target connections
			parent.getChildren().add(grp);
		}
	}
	private void importDiagramModelArchimateObject(Connection db, IArchimateModel model, String modelId) throws SQLException {
		EObject parent = null;
		String oldParent = null;

		ResultSet result = DatabasePlugin.select(db, "SELECT * FROM DiagramModelArchimateObject WHERE id LIKE ? ORDER BY indent, rank, parent", modelId+"-%");
		while(result.next()) {
			if ( (oldParent == null) || !result.getString("parent").trim().equals(oldParent) ) {
				parent = ArchimateModelUtils.getObjectByID(model,result.getString("parent").trim());
				if ( parent == null ) {
					DatabasePlugin.popup(Level.Error, "Cannot import object ("+result.getString("id").trim()+") as we do not know its parent ("+result.getString("parent").trim()+")");
					break;
				}
			}
			DiagramModelArchimateObject obj = (DiagramModelArchimateObject) IArchimateFactory.eINSTANCE.createDiagramModelArchimateObject();
			obj.setId(result.getString("id").trim());
			//obj.setName(result.getString("name").trim());
			obj.setArchimateElement((IArchimateElement) ArchimateModelUtils.getObjectByID(model,result.getString("element").trim()));
			if ( result.getString("fillcolor") != null ) obj.setFillColor(result.getString("fillcolor").trim());
			if ( result.getString("font") != null)       obj.setFont(result.getString("font").trim());
			if ( result.getString("fontcolor") != null ) obj.setFontColor(result.getString("fontcolor").trim());
			if ( result.getString("linecolor") != null ) obj.setLineColor(result.getString("linecolor").trim());
			obj.setLineWidth(result.getInt("linewidth"));
			obj.setTextAlignment(result.getInt("textalignment"));
			obj.setType(result.getInt("type"));

			importPoint(db, obj);

			if ( parent instanceof DiagramModelArchimateObject )
				((DiagramModelArchimateObject)parent).getChildren().add(obj);
			else if ( parent instanceof ArchimateDiagramModel )
				((ArchimateDiagramModel)parent).getChildren().add(obj);
			else if ( parent instanceof IDiagramModelGroup )
				((DiagramModelGroup)parent).getChildren().add(obj);
			else
				DatabasePlugin.popup(Level.Error, "Parent is of unknown type !");
		}
		result.close();
	}
	private void importTargetConnections(Connection db, IArchimateModel model, String modelId) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT id, targetconnections FROM DiagramModelArchimateObject WHERE id LIKE ? AND targetconnections IS NOT NULL", modelId+"-%");
		while(result.next()) {
			DiagramModelArchimateObject parent = (DiagramModelArchimateObject) ArchimateModelUtils.getObjectByID(model,result.getString("id").trim());
			if ( result.getString("targetconnections") != null ) {
				for ( String id: result.getString("targetconnections").trim().split(",")) {
					if ( ArchimateModelUtils.getObjectByID(model,id) == null )
						DatabasePlugin.popup(Level.Error, "Cannot found targetConnection "+id);
					else
						parent.getTargetConnections().add((DiagramModelArchimateConnection) ArchimateModelUtils.getObjectByID(model,id));
				}
			}
		}
		result.close();
	}
	@SuppressWarnings("deprecation")
	private void importSourceConnections(Connection db, IArchimateModel model, DiagramModelArchimateObject parent) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT * FROM DiagramModelArchimateConnection WHERE parent = ? ORDER BY rank", parent.getId());
		while(result.next()) {
			IDiagramModelObject source = (IDiagramModelObject) ArchimateModelUtils.getObjectByID(model,result.getString("source").trim());
			if ( source == null )
				DatabasePlugin.popup(Level.Error,  "Cannot found source "+result.getString("source"));
			IDiagramModelObject target = (IDiagramModelObject) ArchimateModelUtils.getObjectByID(model,result.getString("target").trim());
			if ( target == null )
				DatabasePlugin.popup(Level.Error,  "Cannot found target "+result.getString("target").trim());
			DiagramModelArchimateConnection conn = (DiagramModelArchimateConnection) IArchimateFactory.eINSTANCE.createDiagramModelArchimateConnection();
			conn.setId(result.getString("id").trim());
			//conn.setName(result.getString("name").trim());
			if ( result.getString("documentation") != null ) conn.setDocumentation(result.getString("documentation").trim());
			if ( result.getString("font") != null )          conn.setFont(result.getString("font").trim());
			if ( result.getString("fontcolor") != null )     conn.setFontColor(result.getString("fontcolor").trim());
			if ( result.getString("linecolor") != null )     conn.setLineColor(result.getString("linecolor").trim());
			if ( result.getString("relationship") != null )  conn.setRelationship((IRelationship) ArchimateModelUtils.getObjectByID(model, result.getString("relationship").trim()));
			if ( result.getString("source") != null )        conn.setSource((IDiagramModelObject) ArchimateModelUtils.getObjectByID(model,result.getString("source").trim()));
			if ( result.getString("target") != null )        conn.setTarget((IDiagramModelObject) ArchimateModelUtils.getObjectByID(model,result.getString("target").trim()));
			if ( result.getString("text") != null )          conn.setText(result.getString("text").trim());
			conn.setLineWidth(result.getInt("linewidth"));
			conn.setTextPosition(result.getInt("textposition"));
			conn.setType(result.getInt("type"));
			importBendpoint(db, conn.getId(), conn.getBendpoints());
			importProperties(db, conn.getId(), conn.getProperties());
			parent.getSourceConnections().add(conn);
		}
		result.close();
	}
	
	private void importProperties(Connection db, String id, EList<IProperty> properties) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT name, value FROM Property WHERE parent = ?", id);
		while(result.next()) {
			IProperty prop = IArchimateFactory.eINSTANCE.createProperty();
			prop.setKey(result.getString("name").trim());
			prop.setValue(result.getString("value").trim());
			properties.add(prop);
		}
		result.close();
	}
	private void importPoint(Connection db, IDiagramModelObject parent) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT x, y, w, h FROM Point where parent = ? ORDER BY rank", parent.getId());
		while(result.next()) {
			parent.setBounds(result.getInt("x"), result.getInt("y"), result.getInt("w"), result.getInt("h"));
		}
		result.close();
	}
	private void importBendpoint(Connection db, String id, EList<IDiagramModelBendpoint> bendpoints) throws SQLException {
		ResultSet result = DatabasePlugin.select(db, "SELECT x, y, w, h FROM Point where parent = ? ORDER BY rank", id);
		while(result.next()) {
			IDiagramModelBendpoint bp = IArchimateFactory.eINSTANCE.createDiagramModelBendpoint();
			bp.setStartX(result.getInt("x"));
			bp.setStartY(result.getInt("y"));
			bp.setEndX(result.getInt("w"));
			bp.setEndY(result.getInt("h"));
			bendpoints.add(bp);
		}
		result.close();
	}
}
