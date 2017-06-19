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
import com.archimatetool.model.IIdentifier;
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
	/**
	 * Calculate the checksum of an object.<br>
	 * Please note that this method is *NOT* recursive. the recursion should be managed at a higher level for folders and views.
	 * @throws NoSuchAlgorithmException 
	 */
	public static String calculateChecksum(EObject eObject) throws NoSuchAlgorithmException {
		StringBuilder checksum = new StringBuilder();
		char startOfText = (char)2;
		char endOfText = (char)3;
		boolean traceChecksum = false;
		
		if ( logger.isTraceEnabled() )
		    logger.trace("Calculating checksum of "+((IDBMetadata)eObject).getDBMetadata().getDebugName());
		
		if ( eObject instanceof IIdentifier ) {
	          if ( traceChecksum ) logger.trace("   id            : "+((IIdentifier)eObject).getId());
	          checksum.append(startOfText+((IIdentifier)eObject).getId()+endOfText);
		}
		
		if ( eObject instanceof INameable ) {
			// we refuse null names, so if we got one, we replace it with an empty string
			if ( ((INameable)eObject).getName() == null ) ((INameable)eObject).setName("");
		    if ( traceChecksum ) logger.trace("   name          : "+((INameable)eObject).getName());
		    checksum.append(startOfText+((INameable)eObject).getName()+endOfText);
		}
		
		if ( eObject instanceof IDocumentable ) {
            if ( traceChecksum ) logger.trace("   documentation : "+((IDocumentable)eObject).getDocumentation());
			checksum.append(startOfText+((IDocumentable)eObject).getDocumentation()+endOfText);
		}
		
		if ( eObject instanceof IJunction ) {
            if ( traceChecksum ) logger.trace("   junction type : "+((IJunction)eObject).getType());
			checksum.append(startOfText+((IJunction)eObject).getType()+endOfText);
		}
		
		if ( eObject instanceof IArchimateRelationship ) {
		    if ( traceChecksum ) logger.trace("   rel source id : "+((IArchimateRelationship)eObject).getSource().getId());
			checksum.append(startOfText+((IArchimateRelationship)eObject).getSource().getId()+endOfText);
			if ( traceChecksum ) logger.trace("   rel target id : "+((IArchimateRelationship)eObject).getTarget().getId());
			checksum.append(startOfText+((IArchimateRelationship)eObject).getTarget().getId()+endOfText);
			
			if ( eObject instanceof IInfluenceRelationship ) {
			    if ( traceChecksum ) logger.trace("   rel strength  : "+((IArchimateRelationship)eObject).getTarget().getId());
				checksum.append(startOfText+((IInfluenceRelationship)eObject).getStrength()+endOfText);
			}
			if ( eObject instanceof IAccessRelationship ) {
	            if ( traceChecksum ) logger.trace("   rel acc. type : "+((IAccessRelationship)eObject).getAccessType());
				checksum.append(startOfText+((IAccessRelationship)eObject).getAccessType()+endOfText);
			}
		}
		
		if ( eObject instanceof IFolder ) {
            if ( traceChecksum ) logger.trace("   folder type   : "+((IFolder)eObject).getType().getLiteral());
            checksum.append(startOfText+((IFolder)eObject).getType().getLiteral()+endOfText);
		}
		
		if ( eObject instanceof IArchimateDiagramModel ) {
		    if ( traceChecksum ) logger.trace("   viewpoint     : "+((IArchimateDiagramModel)eObject).getViewpoint());
		    checksum.append(startOfText+((IArchimateDiagramModel)eObject).getViewpoint()+endOfText);
		}
		
		if ( eObject instanceof IDiagramModel ) {
		    if ( traceChecksum ) logger.trace("   router type   : "+((IDiagramModel)eObject).getConnectionRouterType());
		    checksum.append(startOfText+((IDiagramModel)eObject).getConnectionRouterType()+endOfText);
		}
			
		if ( eObject instanceof IBorderObject ) {
		    if ( traceChecksum ) logger.trace("   border color  : "+((IBorderObject)eObject).getBorderColor());
		    checksum.append(startOfText+((IBorderObject)eObject).getBorderColor()+endOfText);
		}
		
		if ( eObject instanceof IDiagramModelNote ) {
		    if ( traceChecksum ) logger.trace("   border type   : "+((IDiagramModelNote)eObject).getBorderType());
		    checksum.append(startOfText+((IDiagramModelNote)eObject).getBorderType()+endOfText);
		}
		
		if ( eObject instanceof IConnectable ) {
			for ( IDiagramModelConnection conn: ((IConnectable)eObject).getSourceConnections() ) {
			    if ( traceChecksum ) logger.trace("   source conn   : "+conn.getId());
				checksum.append(startOfText+conn.getId()+endOfText);
			}
			for ( IDiagramModelConnection conn: ((IConnectable)eObject).getTargetConnections() ) {
			    if ( traceChecksum ) logger.trace("   target conn   : "+conn.getId());
			    checksum.append(startOfText+conn.getId()+endOfText);
			}
		}
		
		if ( eObject instanceof IDiagramModelArchimateObject ) {
		    if ( traceChecksum ) logger.trace("   type          : "+((IDiagramModelArchimateObject)eObject).getType());
		    checksum.append(startOfText+((IDiagramModelArchimateObject)eObject).getType()+endOfText);
		}
		
		if ( eObject instanceof IDiagramModelConnection ) {
		    if ( traceChecksum ) logger.trace("   type          : "+((IDiagramModelConnection)eObject).getType());
	        checksum.append(startOfText+((IDiagramModelConnection)eObject).getType()+endOfText);
		    if ( traceChecksum ) logger.trace("   text position : "+((IDiagramModelConnection)eObject).getTextPosition());
		    checksum.append(startOfText+((IDiagramModelConnection)eObject).getTextPosition()+endOfText);
		    
			for (IDiagramModelBendpoint point: ((IDiagramModelConnection)eObject).getBendpoints()) {
			    if ( traceChecksum ) logger.trace("   bendpoint s.x : "+point.getStartX());
				checksum.append(startOfText+point.getStartX()+endOfText);
				if ( traceChecksum ) logger.trace("   bendpoint s.y : "+point.getStartY());
				checksum.append(startOfText+point.getStartY()+endOfText);
				if ( traceChecksum ) logger.trace("   bendpoint e.x : "+point.getEndX());
				checksum.append(startOfText+point.getEndX()+endOfText);
				if ( traceChecksum ) logger.trace("   bendpoint e.y : "+point.getEndY());
				checksum.append(startOfText+point.getEndY()+endOfText);
			}
		}
		
		if ( eObject instanceof IDiagramModelImageProvider ) {
		    if ( traceChecksum ) logger.trace("   image path    : "+((IDiagramModelImageProvider)eObject).getImagePath());
			checksum.append(startOfText+((IDiagramModelImageProvider)eObject).getImagePath()+endOfText);
		}
		

		
		if ( eObject instanceof IDiagramModelObject ) {
		    if ( traceChecksum ) logger.trace("   fill color    : "+((IDiagramModelObject)eObject).getFillColor());
		    checksum.append(((IDiagramModelObject)eObject).getFillColor()+endOfText);
			IBounds bounds = ((IDiagramModelObject)eObject).getBounds();
			if ( traceChecksum ) logger.trace("   bounds x      : "+bounds.getX());
			checksum.append(startOfText+bounds.getX()+endOfText);
			if ( traceChecksum ) logger.trace("   bounds y      : "+bounds.getY());
			checksum.append(startOfText+bounds.getY()+endOfText);
			if ( traceChecksum ) logger.trace("   bounds width  : "+bounds.getWidth());
			checksum.append(startOfText+bounds.getWidth()+endOfText);
			if ( traceChecksum ) logger.trace("   bounds height : "+bounds.getHeight());
			checksum.append(startOfText+bounds.getHeight()+endOfText);
		}
		
		if ( eObject instanceof IDiagramModelArchimateComponent ) {
		    if ( traceChecksum ) logger.trace("   object element: "+((IDiagramModelArchimateComponent)eObject).getArchimateConcept().getId());
		    checksum.append(startOfText+((IDiagramModelArchimateComponent)eObject).getArchimateConcept().getId()+endOfText);
		}
		
		if ( eObject instanceof IDiagramModelArchimateConnection ) {
		    if ( traceChecksum ) logger.trace("   connection rel: "+((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId());
		    checksum.append(startOfText+((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId()+endOfText);
		}
		
		if ( eObject instanceof IFontAttribute ) {
		    if ( traceChecksum ) logger.trace("   font          : "+((IFontAttribute)eObject).getFont());
		    checksum.append(startOfText+((IFontAttribute)eObject).getFont()+endOfText);
		    if ( traceChecksum ) logger.trace("   font color    : "+((IFontAttribute)eObject).getFontColor());
			checksum.append(startOfText+((IFontAttribute)eObject).getFontColor()+endOfText);
		}
		
		if ( eObject instanceof ILineObject ) {
		    if ( traceChecksum ) logger.trace("   line width    : "+((ILineObject)eObject).getLineWidth());
		    checksum.append(startOfText+((ILineObject)eObject).getLineWidth()+endOfText);
		    if ( traceChecksum ) logger.trace("   line color    : "+((ILineObject)eObject).getLineColor());
			checksum.append(startOfText+((ILineObject)eObject).getLineColor()+endOfText);
		}
		
		if ( eObject instanceof ILockable ) {
		    if ( traceChecksum ) logger.trace("   lockable      : "+((ILockable)eObject).isLocked());
		    checksum.append(startOfText+String.valueOf(((ILockable)eObject).isLocked())+endOfText);
		}
		
		if ( eObject instanceof ISketchModel ) {
		    if ( traceChecksum ) logger.trace("   background    : "+((ISketchModel)eObject).getBackground());
		    checksum.append(startOfText+((ISketchModel)eObject).getBackground()+endOfText);
		}
		
		if ( eObject instanceof ITextAlignment ) {
		    if ( traceChecksum ) logger.trace("   text alignment: "+((ITextAlignment)eObject).getTextAlignment());
		    checksum.append(startOfText+((ITextAlignment)eObject).getTextAlignment()+endOfText);
		}
		

        if ( eObject instanceof ITextPosition ) {
            if ( traceChecksum ) logger.trace("   text position : "+((ITextPosition)eObject).getTextPosition());
            checksum.append(startOfText+((ITextPosition)eObject).getTextPosition()+endOfText);
        }
		
		if ( eObject instanceof ITextContent ) {
		    if ( traceChecksum ) logger.trace("   content       : "+((ITextContent)eObject).getContent());
		    checksum.append(startOfText+((ITextContent)eObject).getContent()+endOfText);
		}

			
		if ( eObject instanceof IHintProvider )	{
		    if ( traceChecksum ) logger.trace("   hint title    : "+((IHintProvider)eObject).getHintTitle());
		    checksum.append(startOfText+((IHintProvider)eObject).getHintTitle()+endOfText);
		    if ( traceChecksum ) logger.trace("   hint content  : "+((IHintProvider)eObject).getHintContent());
		    checksum.append(startOfText+((IHintProvider)eObject).getHintContent()+endOfText);
		}
		
		if ( eObject instanceof IHelpHintProvider ) {
		    if ( traceChecksum ) logger.trace("   help hint titl: "+((IHelpHintProvider)eObject).getHelpHintTitle());
		    checksum.append(startOfText+((IHelpHintProvider)eObject).getHelpHintTitle()+endOfText);
		    if ( traceChecksum ) logger.trace("   help hint cont: "+((IHelpHintProvider)eObject).getHelpHintContent());
		    checksum.append(startOfText+((IHelpHintProvider)eObject).getHelpHintContent()+endOfText);
		}
				
		if ( eObject instanceof IIconic ) {
		    if ( traceChecksum ) logger.trace("   image position: "+((IIconic)eObject).getImagePosition());
		    checksum.append(startOfText+((IIconic)eObject).getImagePosition()+endOfText);
		}

		if ( eObject instanceof INotesContent ) {
		    if ( traceChecksum ) logger.trace("   notes         : "+((INotesContent)eObject).getNotes());
		    checksum.append(startOfText+((INotesContent)eObject).getNotes()+endOfText);
		}

		if ( eObject instanceof IProperties ) {
			for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
			    if ( traceChecksum ) logger.trace("   property key  : "+prop.getKey());
			    checksum.append(startOfText+prop.getKey()+endOfText);
			    if ( traceChecksum ) logger.trace("   property value: "+prop.getValue());
			    checksum.append(startOfText+prop.getValue()+endOfText);
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
		
		if ( logger.isTraceEnabled() ) {
		    logger.trace("checksum is "+md5.toString()+" ("+md5.length()+") from "+bytes.length+" bytes : "+new String(bytes, 0, Math.min(bytes.length, 200)));
		}
		return md5.toString();
	}
}
