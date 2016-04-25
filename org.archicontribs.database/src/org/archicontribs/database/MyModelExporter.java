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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.archicontribs.database.DatabasePlugin.Level;
import org.archicontribs.database.DatabasePlugin.Mode;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.impl.ArchimateDiagramModel;
import com.archimatetool.model.impl.ArchimateElement;
import com.archimatetool.model.impl.DiagramModelArchimateConnection;
import com.archimatetool.model.impl.DiagramModelArchimateObject;
import com.archimatetool.model.impl.DiagramModelGroup;
import com.archimatetool.model.impl.Relationship;

/**
 * Database Model Exporter
 * 
 * @author Herve Jouin
 */
//TODO: manage several languages
public class MyModelExporter implements IModelExporter {
	private Connection db = null;
	private String oldModelId = null;
	private String newModelId = null;
	//TODO: model id = id-version, objects id = id-xxxxx-version	
	//private String oldVersion = null;
	//private String newVersion = null;
	ResultSet result = null;

	@Override
	public void export(IArchimateModel model) throws IOException {

		if ( (db = new ChooseDatabase().open()) == null)
			return;

		String id[] = model.getId().split("-");
		if ( id.length == 2 ) {
			oldModelId = id[0];
			//oldVersion = id[1];
		}

		try {
			if ( (result = new ChooseModel().open(db, Mode.Export, model)) == null ) {
				db.close();
				return;
			}
			newModelId = result.getString("id").trim();
			//newVersion = result.getString("version").trim();

			if ( !newModelId.equals(oldModelId) && !result.getBoolean("new") ) {
				if ( !MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DatabasePlugin.pluginTitle, "You're about to replace model "+result.getString("name").trim()+" ("+result.getString("id").trim()+").\n\nAre you sure ?") ) {
					try { db.close(); } catch (SQLException ee) {}
					return;
				}
				model.setId(newModelId+"-"+newModelId);
			}
		} catch (SQLException e) {
			DatabasePlugin.popup(Level.Error, "Failed to get the model list from the database.", e);
			try { db.close(); } catch (SQLException ee) {}
			return;
		}

		// if the model is exported to a new one, we transfer the objects to the new model ID
		EObject eObject;
		if ( oldModelId != null && !newModelId.equals(oldModelId) ) {
			for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();) {
				if ( (eObject = iter.next()) instanceof IIdentifier ) {
					id = ((IIdentifier)eObject).getId().split("-");
					if ( id.length == 2 && id[0].equals(oldModelId) )
						((IIdentifier)eObject).setId(newModelId+"-"+id[0]);
				}
			}
		}
		
		// all the new objects (i.e. without a proper ID) are moved to the model ID
		for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();) {
			if ( (eObject = iter.next()) instanceof IIdentifier ) {
				if ( ((IIdentifier)eObject).getId().indexOf("-") == -1 )
					((IIdentifier)eObject).setId(newModelId+"-"+((IIdentifier)eObject).getId());
			}
		}

		try {
			// we remove the old objects from the database as they will be recreated later on
			DatabasePlugin.sql(db, "DELETE FROM Model WHERE id = ?", newModelId);
			for(String table: asList("ArchimateDiagramModel", "ArchimateElement", "DiagramModelArchimateConnection", "DiagramModelArchimateObject", "DiagramModelGroup", "Relationship"))
				DatabasePlugin.sql(db, "DELETE FROM "+table+" WHERE id LIKE ?", newModelId+"-%" );
			for(String table: asList("Point", "Property"))
				DatabasePlugin.sql(db, "DELETE FROM "+table+" WHERE parent LIKE ?", newModelId+"-%");

			// we save the project metadata
			DatabasePlugin.update(db, "INSERT INTO Model (id, name, purpose)", newModelId, model.getName(), model.getPurpose());
			exportProperties(newModelId, model.getProperties());

			// we save the objects
			//TODO: same images
			Set<String> ignored = new HashSet<String>();
			for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();) {
				eObject = iter.next();
				String type = eObject.eClass().getName();
				switch ( type ) {
				case "Property" :						break; //ignored as will be exported by their parents
				case "DiagramModelArchimateObject" :	break; //ignored as will be exported by ArchimateDiagramModel
				case "DiagramModelArchimateConnection" :break; //ignored as will be exported by ArchimateDiagramModel
				case "DiagramModelGroup" :				break; //ignored as will be exported by ArchimateDiagramModel
				case "DiagramModelBendpoint" :			break; //ignored as will be exported by ArchimateDiagramModel
				case "Bounds" :							break; //ignored as will be exported by DiagramModelArchimateObject

				
				
				case "Folder" :

				case "DiagramModelNote" :

				case "DiagramModelConnection" :
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
					exportArchimateDiagramModel((ArchimateDiagramModel)eObject);
					break;

				default:
					if ( eObject instanceof ArchimateElement )
						exportArchimateElement((ArchimateElement)eObject);
					else if ( eObject instanceof Relationship )
						exportRelationship((Relationship)eObject);
					else ignored.add(type);
				}
			}

			if ( !ignored.isEmpty() ) {
				String msg = "";
				for(Object i : ignored)
					msg += "\n     - " + (String)i;
				DatabasePlugin.popup(Level.Warning, "Model exported but not entirely.\n\nThis plugin is not able to export the following objects :"+msg);
			} else {
				DatabasePlugin.popup(Level.Info, "Model exported.");
			}

			db.commit();
			//TODO: how to remove the dirty flag ? 
		} catch (Exception e) {
			DatabasePlugin.popup(Level.Error, "An error occured while exporting your model to the database.\n\nThe transaction has been rolled back and the database is left unmodified.", e);
			try { db.rollback(); } catch (Exception ee) {}
		}
		finally {
			try { db.close(); } catch (SQLException e) {}
		}
	}


	private void exportArchimateElement(ArchimateElement element) throws SQLException {
		DatabasePlugin.update(db, "INSERT INTO ArchimateElement (id, name, type, documentation)", element.getId(), element.getName(), element.getClass().getSimpleName(), element.getDocumentation()); 
		exportProperties(element.getId(), element.getProperties());
	}
	private void exportRelationship(Relationship relation) throws SQLException {
		DatabasePlugin.update(db, "INSERT INTO Relationship (id, name, source, target, type, documentation)",  relation.getId(), relation.getName(), relation.getSource().getId(), relation.getTarget().getId(), relation.getClass().getSimpleName(), relation.getDocumentation()); 
		exportProperties(relation.getId(), relation.getProperties());
	}
	private void exportProperties(String parentId, EList<IProperty> properties) throws SQLException {
		if ( properties != null )
			for(IProperty property: properties )
				DatabasePlugin.update(db, "INSERT INTO property (parent, name, value)", parentId, property.getKey(), property.getValue());
	}

	private void exportArchimateDiagramModel(ArchimateDiagramModel diagram) throws SQLException {
		DatabasePlugin.update(db, "INSERT INTO ArchimateDiagramModel (id, name, documentation,type)", diagram.getId(), diagram.getName(), diagram.getDocumentation(), diagram.getClass().getSimpleName());
		exportProperties(diagram.getId(), diagram.getProperties());

		int rank = 1;
		for (IDiagramModelObject child: diagram.getChildren()) {
			switch ( child.eClass().getName() ) {
			case "DiagramModelArchimateObject" :
				exportDiagramModelArchimateObject(diagram.getId(), (DiagramModelArchimateObject)child, rank++, 0);
				break;
			case "DiagramModelArchimateConnection" :
				exportDiagramModelArchimateConnection(diagram.getId(), (DiagramModelArchimateConnection)child, rank++);
				break;
			case "DiagramModelGroup" :
				exportDiagramModelGroup(diagram.getId(), (DiagramModelGroup)child, rank++);
				break;
			default : //should never be here
				DatabasePlugin.popup(Level.Error,"Don't know how to save " + child.getName() + " (" + child.eClass().getName() + ")");
			}
		}
	}
	private void exportDiagramModelArchimateObject(String parent, DiagramModelArchimateObject obj, int rank, int indent) throws SQLException {
		String target=null;
		for (IDiagramModelConnection c: obj.getTargetConnections()) target = (target == null) ? c.getId() : target + "," + c.getId();

		DatabasePlugin.update(db, "INSERT INTO DiagramModelArchimateObject (id, parent, defaulttextalignment, fillcolor, font, fontcolor, linecolor, linewidth, name, textAlignment, element, targetconnections, rank, indent)",
				obj.getId(), parent, obj.getDefaultTextAlignment(), obj.getFillColor(), obj.getFont(), obj.getFontColor(), obj.getLineColor(), obj.getLineWidth(), obj.getName(), obj.getTextAlignment(), obj.getArchimateElement().getId(), target, rank, indent);
		exportPoint(obj.getId(), obj.getBounds(), 0);

		for (int i=0; i < obj.getSourceConnections().size(); i++) {
			exportDiagramModelArchimateConnection(obj.getId(), (DiagramModelArchimateConnection)(obj.getSourceConnections().get(i)), i);
		}
		for ( int i=0; i < obj.getChildren().size(); i++) {
			exportDiagramModelArchimateObject(obj.getId(), (DiagramModelArchimateObject)(obj.getChildren().get(i)), i, indent+1);
		}
	}
	@SuppressWarnings("deprecation")
	private void exportDiagramModelArchimateConnection(String parent, DiagramModelArchimateConnection conn, int rank) throws SQLException {
		DatabasePlugin.update(db, "INSERT INTO DiagramModelArchimateConnection (id, parent, documentation, font, fontcolor, linecolor, linewidth, name, relationship, source, target, text, textposition, type, rank)",
				conn.getId(), parent, conn.getDocumentation(), conn.getFont(), conn.getFontColor(), conn.getLineColor(), conn.getLineWidth(), conn.getName(),	conn.getRelationship().getId(), conn.getSource().getId(), conn.getTarget().getId(), conn.getText(), String.valueOf(conn.getTextPosition()),	conn.getType(), rank);

		int i=0;
		for ( IDiagramModelBendpoint point: conn.getBendpoints() ) {
			exportPoint(conn.getId(), point, i++);
		}

		exportProperties(conn.getId(), conn.getProperties());
	}
	private void exportDiagramModelGroup(String parent, DiagramModelGroup grp, int rank) throws SQLException {
		String target=null;
		for (IDiagramModelConnection c: grp.getTargetConnections()) target = (target == null) ? c.getId() : target + "," + c.getId();
		
		DatabasePlugin.update(db, "INSERT INTO DiagramModelGroup (id, parent, documentation, fillcolor, font, fontcolor, linecolor, linewidth, name, targetconnections, textalignment, rank)",
				grp.getId(), parent, grp.getDocumentation(), grp.getFillColor(), grp.getFont(), grp.getFontColor(), grp.getLineColor(), grp.getLineWidth(), grp.getName(), target, grp.getTextAlignment(), rank);

		exportPoint(grp.getId(), grp.getBounds(), 0);
		exportProperties(grp.getId(), grp.getProperties());
		
		for (int i=0; i < grp.getSourceConnections().size(); i++) {
			exportDiagramModelArchimateConnection(grp.getId(), (DiagramModelArchimateConnection)(grp.getSourceConnections().get(i)), i);
		}
		
		for ( int i=0; i < grp.getChildren().size(); i++) {
			exportDiagramModelArchimateObject(grp.getId(), (DiagramModelArchimateObject)(grp.getChildren().get(i)), i, 0);
		}
	}
	
	private void exportPoint(String parent, IBounds point, int rank) throws SQLException {
		DatabasePlugin.update(db, "INSERT INTO Point (parent, x, y, w, h, rank)", parent, point.getX(), point.getY(), point.getWidth(), point.getHeight(), rank);
	}
	private void exportPoint(String parent, IDiagramModelBendpoint bendpoint, int rank) throws SQLException {
		DatabasePlugin.update(db, "INSERT INTO Point (parent, x, y, w, h, rank)", parent, bendpoint.getStartX(), bendpoint.getStartY(), bendpoint.getEndX(), bendpoint.getEndY(), rank);
	}

	/**************************************************************************************/


}
