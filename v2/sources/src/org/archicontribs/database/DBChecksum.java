/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IInfluenceRelationship;
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
	/**
	 * Calculate the checksum of an object.<br>
	 * Please note that this method is *NOT* recursive. the recursion should be managed at a higher level for folders and views.
	 */
	public static String calculateChecksum(EObject eObject) {
		StringBuilder checksum = new StringBuilder();
		
		if ( eObject instanceof INameable )
			checksum.append(((INameable)eObject).getName());
		
		if ( eObject instanceof IDocumentable )
			checksum.append(((IDocumentable)eObject).getDocumentation());
		
		if ( eObject instanceof IArchimateDiagramModel )
			checksum.append(((IArchimateDiagramModel)eObject).getViewpoint());
		
		if ( eObject instanceof IArchimateRelationship ) {
			checksum.append(((IArchimateRelationship)eObject).getSource().getId());
			checksum.append(((IArchimateRelationship)eObject).getTarget().getId());
			if ( eObject instanceof IInfluenceRelationship )
				checksum.append(((IInfluenceRelationship)eObject).getStrength());
			if ( eObject instanceof IAccessRelationship )
				checksum.append(((IAccessRelationship)eObject).getAccessType());
		}
		
		if ( eObject instanceof IBorderObject )
			checksum.append(((IBorderObject)eObject).getBorderColor());
		
		if ( eObject instanceof IConnectable ) {
			checksum.append(((IConnectable)eObject).getSourceConnections());
			checksum.append(((IConnectable)eObject).getTargetConnections());
		}
		
		if ( eObject instanceof IDiagramModel )
			checksum.append(((IDiagramModel)eObject).getConnectionRouterType());
			
		if ( eObject instanceof IDiagramModelConnection ) {
			checksum.append(((IDiagramModelConnection)eObject).getTextPosition());
			checksum.append(((IDiagramModelConnection)eObject).getType());
			for (IDiagramModelBendpoint point: ((IDiagramModelConnection)eObject).getBendpoints()) {
				checksum.append(point.getStartX());
				checksum.append(point.getStartY());
				checksum.append(point.getEndX());
				checksum.append(point.getEndY());
			}
		}
		
		if ( eObject instanceof IDiagramModelImageProvider )
			checksum.append(((IDiagramModelImageProvider)eObject).getImagePath());
		
		if ( eObject instanceof IDiagramModelNote )
			checksum.append(((IDiagramModelNote)eObject).getBorderType());
		
		if ( eObject instanceof IDiagramModelObject ) {
			checksum.append(((IDiagramModelObject)eObject).getFillColor());
			IBounds bounds = ((IDiagramModelObject)eObject).getBounds();
			checksum.append(bounds.getX());
			checksum.append(bounds.getY());
			checksum.append(bounds.getWidth());
			checksum.append(bounds.getHeight());
		}
		
		if ( eObject instanceof IFontAttribute ) {
			checksum.append(((IFontAttribute)eObject).getFont());
			checksum.append(((IFontAttribute)eObject).getFontColor());
		}
		
		// we include the rootfolderType in the checksum to force the export process to export them again
		if ( eObject instanceof IFolder ) {
		    checksum.append(((IDBMetadata)eObject).getDBMetadata().getRootFolderType());
		}
		
		if ( eObject instanceof ILineObject ) {
			checksum.append(((ILineObject)eObject).getLineWidth());
			checksum.append(((ILineObject)eObject).getLineColor());
		}
		
		if ( eObject instanceof ILockable )
			checksum.append(((ILockable)eObject).isLocked());
		
		if ( eObject instanceof ISketchModel )
			checksum.append(((ISketchModel)eObject).getBackground());
		
		if ( eObject instanceof ITextAlignment )
			checksum.append(((ITextAlignment)eObject).getTextAlignment());
		
		if ( eObject instanceof ITextContent )
			checksum.append(((ITextContent)eObject).getContent());

		if ( eObject instanceof ITextPosition )
			checksum.append(((ITextPosition)eObject).getTextPosition());

			
		if ( eObject instanceof IHintProvider )	{
				checksum.append(((IHintProvider)eObject).getHintTitle());
				checksum.append(((IHintProvider)eObject).getHintContent());
		}
		
		if ( eObject instanceof IHelpHintProvider ) {
				checksum.append(((IHelpHintProvider)eObject).getHelpHintTitle());
				checksum.append(((IHelpHintProvider)eObject).getHelpHintContent());
		}
				
		if ( eObject instanceof IIconic ) 
			checksum.append(((IIconic)eObject).getImagePosition());

		if ( eObject instanceof INotesContent ) 
			checksum.append(((INotesContent)eObject).getNotes());

		if ( eObject instanceof IProperties ) {
			for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
				checksum.append(prop.getKey());
				checksum.append(prop.getValue());
			}
		}
		
		return calculateChecksum(checksum);
	}
	
	/**
	 * Calculate a MD5 from a StringBuilder
	 */
	public static String calculateChecksum(StringBuilder input) {
		return calculateChecksum(input.toString().getBytes());
	}
	
	/**
	 * Calculate a MD5 from a String
	 */
	public static String calculateChecksum(String input) {
		return calculateChecksum(input.getBytes());
	}
	
	/**
	 * Calculate a MD5 from a byte array
	 */
	public static String calculateChecksum(byte[] bytes) {
	    if ( bytes == null )
	    	return null;
	    
		try {
	    	MessageDigest md;
			md = MessageDigest.getInstance("MD5");
	    	md.update(bytes);
	    	BigInteger hash = new BigInteger(1, md.digest());
	    	return hash.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return Integer.toString(bytes.hashCode());
		}
	}
}
