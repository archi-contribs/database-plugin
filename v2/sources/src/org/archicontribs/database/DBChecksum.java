/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.io.UnsupportedEncodingException;
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
	private static final boolean traceChecksum = false;
	private static final char startOfText = (char)2;
	private static final char endOfText = (char)3;

	/**
	 * Calculate the checksum of an object.<br>
	 * Please note that this method is *NOT* recursive. the recursion should be managed at a higher level for folders and views.
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String calculateChecksum(EObject eObject) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		StringBuilder checksum = new StringBuilder();

		
		if ( logger.isTraceEnabled() )
		    logger.trace("Calculating checksum of "+((IDBMetadata)eObject).getDBMetadata().getDebugName());
		
		if ( eObject instanceof IIdentifier )						append(checksum, "id", ((IIdentifier)eObject).getId());
		if ( eObject instanceof INameable )							append(checksum, "name", ((INameable)eObject).getName());		// shall we do a setName("") in case of null ?
		if ( eObject instanceof IDocumentable )						append(checksum, "documentation", ((IDocumentable)eObject).getDocumentation());
		if ( eObject instanceof IJunction )							append(checksum, "junction type", ((IJunction)eObject).getType());
		if ( eObject instanceof IArchimateRelationship ) {			append(checksum, "source id", ((IArchimateRelationship)eObject).getSource().getId());
																	append(checksum, "target id", ((IArchimateRelationship)eObject).getTarget().getId());
			if ( eObject instanceof IInfluenceRelationship )		append(checksum, "strength", ((IInfluenceRelationship)eObject).getStrength());
			if ( eObject instanceof IAccessRelationship )			append(checksum, "access type", ((IAccessRelationship)eObject).getAccessType());
		}
		if ( eObject instanceof IFolder )							append(checksum, "folder type", ((IFolder)eObject).getType().getLiteral());
		if ( eObject instanceof IArchimateDiagramModel )			append(checksum, "viewpoint", ((IArchimateDiagramModel)eObject).getViewpoint());
		if ( eObject instanceof IDiagramModel )						append(checksum, "router type", ((IDiagramModel)eObject).getConnectionRouterType());
		if ( eObject instanceof IBorderObject )						append(checksum, "border color", ((IBorderObject)eObject).getBorderColor());
		if ( eObject instanceof IDiagramModelNote )					append(checksum, "border type", ((IDiagramModelNote)eObject).getBorderType());
		if ( eObject instanceof IConnectable ) {					for ( IDiagramModelConnection conn: ((IConnectable)eObject).getSourceConnections() )
																		append(checksum, "source connections", conn.getId());
																	for ( IDiagramModelConnection conn: ((IConnectable)eObject).getTargetConnections() )
																		append(checksum, "target connections", conn.getId());
		}
		if ( eObject instanceof IDiagramModelArchimateObject )		append(checksum, "type", ((IDiagramModelArchimateObject)eObject).getType());
		if ( eObject instanceof IDiagramModelConnection ) {			append(checksum, "type", ((IDiagramModelConnection)eObject).getType());			// we do not use getText as it is deprecated
																	append(checksum, " text position : ", ((IDiagramModelConnection)eObject).getTextPosition());
																	for (IDiagramModelBendpoint point: ((IDiagramModelConnection)eObject).getBendpoints()) {
																		append(checksum, "bendpoint start x", point.getStartX());
																		append(checksum, "bendpoint start y", point.getStartY());
																		append(checksum, "bendpoint end x", point.getEndX());
																		append(checksum, "bendpoint end y" ,point.getEndY());
																	}
		}
		if ( eObject instanceof IDiagramModelImageProvider )		append(checksum, "image path", ((IDiagramModelImageProvider)eObject).getImagePath());
		if ( eObject instanceof IDiagramModelObject ) {				append(checksum, "fill color", ((IDiagramModelObject)eObject).getFillColor());
																	IBounds bounds = ((IDiagramModelObject)eObject).getBounds();
																	append(checksum, "bounds x", bounds.getX());
																	append(checksum, "bounds y", bounds.getY());
																	append(checksum, "bounds width" ,bounds.getWidth());
																	append(checksum, "bounds height", bounds.getHeight());
		}
		if ( eObject instanceof IDiagramModelArchimateComponent )	append(checksum, "archimate concept", ((IDiagramModelArchimateComponent)eObject).getArchimateConcept().getId());
		if ( eObject instanceof IDiagramModelArchimateConnection )	append(checksum, "archimate concept", ((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId());
		if ( eObject instanceof IFontAttribute ) {					append(checksum, "font", ((IFontAttribute)eObject).getFont());
																	append(checksum, "font color", ((IFontAttribute)eObject).getFontColor());
		}
		if ( eObject instanceof ILineObject ) {						append(checksum, "line width", ((ILineObject)eObject).getLineWidth());
																	append(checksum, "line color", ((ILineObject)eObject).getLineColor());
		}
		if ( eObject instanceof ILockable )							append(checksum, "lockable", ((ILockable)eObject).isLocked());
		if ( eObject instanceof ISketchModel )						append(checksum, "background", ((ISketchModel)eObject).getBackground());
		if ( eObject instanceof ITextAlignment )					append(checksum, "text alignment", ((ITextAlignment)eObject).getTextAlignment());
        if ( eObject instanceof ITextPosition )						append(checksum, "text position", ((ITextPosition)eObject).getTextPosition());
		if ( eObject instanceof ITextContent )						append(checksum, "content", ((ITextContent)eObject).getContent());
		if ( eObject instanceof IHintProvider )	{					append(checksum, "hint title", ((IHintProvider)eObject).getHintTitle());
																	append(checksum, "hint content", ((IHintProvider)eObject).getHintContent());
		}
		if ( eObject instanceof IHelpHintProvider ) {				append(checksum, "help hint title", ((IHelpHintProvider)eObject).getHelpHintTitle());
																	append(checksum, "help hint content", ((IHelpHintProvider)eObject).getHelpHintContent());
		}
		if ( eObject instanceof IIconic )							append(checksum, "image position", ((IIconic)eObject).getImagePosition());
		if ( eObject instanceof INotesContent )						append(checksum, "notes", ((INotesContent)eObject).getNotes());
		if ( eObject instanceof IProperties ) {						for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
																		append(checksum, "property key", prop.getKey());
																		append(checksum, "property value", prop.getValue());
																	}
		}
		
		return calculateChecksum(checksum);
	}
	
	private static void append(StringBuilder sb, String name, String value) {
		String sValue = (value == null ? "" : value);
		
		if ( traceChecksum ) logger.trace("   "+name+" : "+sValue);
	    sb.append(startOfText+sValue+endOfText);
	}
	
	private static void append(StringBuilder sb, String name, int value) {
		append(sb, name, String.valueOf(value));
	}
	
	private static void append(StringBuilder sb, String name, boolean value) {
		append(sb, name, String.valueOf(value));
	}
	
	/**
	 * Calculate a MD5 from a StringBuilder
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String calculateChecksum(StringBuilder input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return calculateChecksum(input.toString().getBytes("UTF-8"));
	}
	
	/**
	 * Calculate a MD5 from a String
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String calculateChecksum(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return calculateChecksum(input.getBytes("UTF-8"));
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
