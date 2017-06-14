/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Level;
import org.archicontribs.database.GUI.DBGui;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.canvas.model.INotesContent;
import com.archimatetool.help.hints.IHelpHintProvider;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBorderObject;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IConnectable;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

public class DBChecksum {
	private static final DBLogger logger = new DBLogger(DBChecksum.class);
	private static final boolean debugChecksum = false;
	/**
	 * Calculate the checksum of an object.<br>
	 * Please note that this method is *NOT* recursive. the recursion should be managed at a higher level for folders and views.
	 * @throws NoSuchAlgorithmException 
	 */
	public static String calculateChecksum(EObject eObject) throws NoSuchAlgorithmException {
		StringBuilder checksum = new StringBuilder();
		
		if ( debugChecksum )
		    logger.trace("Calculating checksum of "+((IDBMetadata)eObject).getDBMetadata().getDebugName());
		
		if ( eObject instanceof INameable ) {
		    if ( debugChecksum ) logger.trace("   name          : "+((INameable)eObject).getName());
		    checksum.append(((INameable)eObject).getName());
		}
		
		if ( eObject instanceof IDocumentable ) {
            if ( debugChecksum ) logger.trace("   documentation : "+((IDocumentable)eObject).getDocumentation().substring(0, Math.min(((IDocumentable)eObject).getDocumentation().length(), 200)));
			checksum.append(((IDocumentable)eObject).getDocumentation());
		}
		
		if ( eObject instanceof IJunction ) {
            if ( debugChecksum ) logger.trace("   junction type : "+((IJunction)eObject).getType());
			checksum.append(((IJunction)eObject).getType());
		}
		
		if ( eObject instanceof IArchimateRelationship ) {
		    if ( debugChecksum ) logger.trace("   rel source id : "+((IArchimateRelationship)eObject).getSource().getId());
			checksum.append(((IArchimateRelationship)eObject).getSource().getId());
			if ( debugChecksum ) logger.trace("   rel target id : "+((IArchimateRelationship)eObject).getTarget().getId());
			checksum.append(((IArchimateRelationship)eObject).getTarget().getId());
			
			if ( eObject instanceof IInfluenceRelationship ) {
			    if ( debugChecksum ) logger.trace("   rel strength  : "+((IArchimateRelationship)eObject).getTarget().getId());
				checksum.append(((IInfluenceRelationship)eObject).getStrength());
			}
			if ( eObject instanceof IAccessRelationship ) {
	             if ( debugChecksum ) logger.trace("   rel acc. type : "+((IAccessRelationship)eObject).getAccessType());
				checksum.append(((IAccessRelationship)eObject).getAccessType());
			}
		}
		
		if ( eObject instanceof IFolder ) {
            if ( debugChecksum ) logger.trace("   folder type   : "+((IFolder)eObject).getType().getLiteral());
            checksum.append(((IFolder)eObject).getType().getLiteral());
		}
		
		if ( eObject instanceof IArchimateDiagramModel ) {
		    if ( debugChecksum ) logger.trace("   viewpoint     : "+((IArchimateDiagramModel)eObject).getViewpoint());
		    checksum.append(((IArchimateDiagramModel)eObject).getViewpoint());
		}
		
		if ( eObject instanceof IDiagramModel ) {
		    if ( debugChecksum ) logger.trace("   router type   : "+((IDiagramModel)eObject).getConnectionRouterType());
		    checksum.append(((IDiagramModel)eObject).getConnectionRouterType());
		}
			
		if ( eObject instanceof IBorderObject ) {
		    if ( debugChecksum ) logger.trace("   border color  : "+((IBorderObject)eObject).getBorderColor());
		    checksum.append(((IBorderObject)eObject).getBorderColor());
		}
		
		if ( eObject instanceof IDiagramModelNote ) {
		    if ( debugChecksum ) logger.trace("   border type   : "+((IDiagramModelNote)eObject).getBorderType());
		    checksum.append(((IDiagramModelNote)eObject).getBorderType());
		}
		
		if ( eObject instanceof IConnectable ) {
			for ( IDiagramModelConnection conn: ((IConnectable)eObject).getSourceConnections() ) {
			    if ( debugChecksum ) logger.trace("   source conn   : "+conn.getId());
				checksum.append(conn.getId());
			}
			for ( IDiagramModelConnection conn: ((IConnectable)eObject).getTargetConnections() ) {
			    if ( debugChecksum ) logger.trace("   target conn   : "+conn.getId());
			    checksum.append(conn.getId());
			}
		}
		
		if ( eObject instanceof IDiagramModelArchimateObject ) {
		    if ( debugChecksum ) logger.trace("   type          : "+((IDiagramModelArchimateObject)eObject).getType());
		    checksum.append(((IDiagramModelArchimateObject)eObject).getType());
		}
		
		if ( eObject instanceof IDiagramModelConnection ) {
		    if ( debugChecksum ) logger.trace("   type          : "+((IDiagramModelConnection)eObject).getType());
	        checksum.append(((IDiagramModelConnection)eObject).getType());
		    if ( debugChecksum ) logger.trace("   text position : "+((IDiagramModelConnection)eObject).getTextPosition());
		    checksum.append(((IDiagramModelConnection)eObject).getTextPosition());
		    
			for (IDiagramModelBendpoint point: ((IDiagramModelConnection)eObject).getBendpoints()) {
			    if ( debugChecksum ) logger.trace("   bendpoint s.x : "+point.getStartX());
				checksum.append(point.getStartX());
				if ( debugChecksum ) logger.trace("   bendpoint s.y : "+point.getStartY());
				checksum.append(point.getStartY());
				if ( debugChecksum ) logger.trace("   bendpoint e.x : "+point.getEndX());
				checksum.append(point.getEndX());
				if ( debugChecksum ) logger.trace("   bendpoint e.y : "+point.getEndY());
				checksum.append(point.getEndY());
			}
		}
		
		if ( eObject instanceof IDiagramModelImageProvider ) {
		    if ( debugChecksum ) logger.trace("   image path    : "+((IDiagramModelImageProvider)eObject).getImagePath());
			checksum.append(((IDiagramModelImageProvider)eObject).getImagePath());
		}
		

		
		if ( eObject instanceof IDiagramModelObject ) {
		    if ( debugChecksum ) logger.trace("   fill color    : "+((IDiagramModelObject)eObject).getFillColor());
		    checksum.append(((IDiagramModelObject)eObject).getFillColor());
			IBounds bounds = ((IDiagramModelObject)eObject).getBounds();
			if ( debugChecksum ) logger.trace("   bounds x      : "+bounds.getX());
			checksum.append(bounds.getX());
			if ( debugChecksum ) logger.trace("   bounds y      : "+bounds.getY());
			checksum.append(bounds.getY());
			if ( debugChecksum ) logger.trace("   bounds width  : "+bounds.getWidth());
			checksum.append(bounds.getWidth());
			if ( debugChecksum ) logger.trace("   bounds height : "+bounds.getHeight());
			checksum.append(bounds.getHeight());
		}
		
		if ( eObject instanceof IDiagramModelArchimateComponent ) {
		    if ( debugChecksum ) logger.trace("   object element: "+((IDiagramModelArchimateComponent)eObject).getArchimateConcept().getId());
		    checksum.append(((IDiagramModelArchimateComponent)eObject).getArchimateConcept().getId());
		}
		
		if ( eObject instanceof IDiagramModelArchimateConnection ) {
		    if ( debugChecksum ) logger.trace("   connection rel: "+((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId());
		    checksum.append(((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId());
		}
		
		if ( eObject instanceof IFontAttribute ) {
		    if ( debugChecksum ) logger.trace("   font          : "+((IFontAttribute)eObject).getFont());
		    checksum.append(((IFontAttribute)eObject).getFont());
		    if ( debugChecksum ) logger.trace("   font color    : "+((IFontAttribute)eObject).getFontColor());
			checksum.append(((IFontAttribute)eObject).getFontColor());
		}
		
		if ( eObject instanceof ILineObject ) {
		    if ( debugChecksum ) logger.trace("   line width    : "+((ILineObject)eObject).getLineWidth());
		    checksum.append(((ILineObject)eObject).getLineWidth());
		    if ( debugChecksum ) logger.trace("   line color    : "+((ILineObject)eObject).getLineColor());
			checksum.append(((ILineObject)eObject).getLineColor());
		}
		
		if ( eObject instanceof ILockable ) {
		    if ( debugChecksum ) logger.trace("   lockable      : "+((ILockable)eObject).isLocked());
		    checksum.append(((ILockable)eObject).isLocked());
		}
		
		if ( eObject instanceof ISketchModel ) {
		    if ( debugChecksum ) logger.trace("   background    : "+((ISketchModel)eObject).getBackground());
		    checksum.append(((ISketchModel)eObject).getBackground());
		}
		
		if ( eObject instanceof ITextAlignment ) {
		    if ( debugChecksum ) logger.trace("   text alignment: "+((ITextAlignment)eObject).getTextAlignment());
		    checksum.append(((ITextAlignment)eObject).getTextAlignment());
		}
		

        if ( eObject instanceof ITextPosition ) {
            if ( debugChecksum ) logger.trace("   text position : "+((ITextPosition)eObject).getTextPosition());
            checksum.append(((ITextPosition)eObject).getTextPosition());
        }
		
		if ( eObject instanceof ITextContent ) {
		    if ( debugChecksum ) logger.trace("   content       : "+((ITextContent)eObject).getContent());
		    checksum.append(((ITextContent)eObject).getContent());
		}

			
		if ( eObject instanceof IHintProvider )	{
		    if ( debugChecksum ) logger.trace("   hint title    : "+((IHintProvider)eObject).getHintTitle());
		    checksum.append(((IHintProvider)eObject).getHintTitle());
		    if ( debugChecksum ) logger.trace("   hint content  : "+((IHintProvider)eObject).getHintContent());
		    checksum.append(((IHintProvider)eObject).getHintContent());
		}
		
		if ( eObject instanceof IHelpHintProvider ) {
		    if ( debugChecksum ) logger.trace("   help hint titl: "+((IHelpHintProvider)eObject).getHelpHintTitle());
		    checksum.append(((IHelpHintProvider)eObject).getHelpHintTitle());
		    if ( debugChecksum ) logger.trace("   help hint cont: "+((IHelpHintProvider)eObject).getHelpHintContent());
		    checksum.append(((IHelpHintProvider)eObject).getHelpHintContent());
		}
				
		if ( eObject instanceof IIconic ) {
		    if ( debugChecksum ) logger.trace("   image position: "+((IIconic)eObject).getImagePosition());
		    checksum.append(((IIconic)eObject).getImagePosition());
		}

		if ( eObject instanceof INotesContent ) {
		    if ( debugChecksum ) logger.trace("   notes         : "+((INotesContent)eObject).getNotes().substring(0, Math.min(((INotesContent)eObject).getNotes().length(), 200)));
		    checksum.append(((INotesContent)eObject).getNotes());
		}

		if ( eObject instanceof IProperties ) {
			for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
			    if ( debugChecksum ) logger.trace("   property key  : "+prop.getKey());
			    checksum.append(prop.getKey());
			    if ( debugChecksum ) logger.trace("   property value: "+prop.getValue());
			    checksum.append(prop.getValue());
			}
		}
		
		return calculateChecksum(checksum);
	}
	
	/**
	 * Calculate a MD5 from a StringBuilder
	 * @throws NoSuchAlgorithmException 
	 */
	public static String calculateChecksum(StringBuilder input) throws NoSuchAlgorithmException {
		return calculateChecksum(input.toString().getBytes());
	}
	
	/**
	 * Calculate a MD5 from a String
	 * @throws NoSuchAlgorithmException 
	 */
	public static String calculateChecksum(String input) throws NoSuchAlgorithmException {
		return calculateChecksum(input.getBytes());
	}
	
	/**
	 * Calculate a MD5 from a byte array
	 * @throws NoSuchAlgorithmException 
	 */
	public static String calculateChecksum(byte[] bytes) throws NoSuchAlgorithmException {
	    if ( bytes == null )
	    	return null;
	    
        MessageDigest md;
        StringBuilder md5 = new StringBuilder();
	    
		try {

			md = MessageDigest.getInstance("MD5");
	    	md.update(bytes);
	    	
	    	byte[] digest = md.digest();

	    	for (int i = 0; i < digest.length; i++) {
	    	    if ((0xff & digest[i]) < 0x10) {
	    	    	md5.append("0").append(Integer.toHexString((0xFF & digest[i])));
	    	    } else {
	    	    	md5.append(Integer.toHexString(0xFF & digest[i]));
	    	    }
	    	}
		} catch (NoSuchAlgorithmException e) {
			DBGui.popup(Level.ERROR, "Failed to calculate checksum.", e);
			throw e;
		}
		
		if ( debugChecksum ) {
		    logger.trace("checksum is "+md5.toString()+" ("+md5.length()+") from "+bytes.length+" bytes : "+new String(bytes, 0, Math.min(bytes.length, 200)));
		}
		return md5.toString();
	}
}
