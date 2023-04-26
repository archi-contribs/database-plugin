/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.data;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Level;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.GUI.DBGuiUtils;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.canvas.model.INotesContent;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModel;
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
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFeature;
import com.archimatetool.model.IFeatures;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProfile;
import com.archimatetool.model.IProfiles;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

/**
 * Class to manage checksums
 * 
 * @author Herve Jouin
 */
public class DBChecksum {
	protected static final DBLogger logger = new DBLogger(DBChecksum.class);
	
	private static final char startOfText = (char)2;
	private static final char endOfText = (char)3;
	
	/**
	 * Calculate the checksum of a model.<br>
	 * Please note that this method is *NOT* recursive: the checksum only considers the information of the model itself.
	 * @param model 
	 * @param releaseNote 
	 * @return 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String calculateChecksum(IArchimateModel model, String releaseNote) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		StringBuilder checksumBuilder = new StringBuilder();
		
		append(checksumBuilder, "id", model.getId());
		append(checksumBuilder, "name", model.getName());
		append(checksumBuilder, "purpose", model.getPurpose());
		append(checksumBuilder, "note", releaseNote);
		
		if ( model.getProperties() != null )
			for ( IProperty prop: model.getProperties() ) {
				append(checksumBuilder, "property key", prop.getKey());
				append(checksumBuilder, "property value", prop.getValue());
			}
		
		if ( model.getFeatures() != null )
			for ( IFeature feature: model.getFeatures() ) {
				append(checksumBuilder, "feature name", feature.getName());
				append(checksumBuilder, "feature value", feature.getValue());
			}
		
		if ( model.getProfiles() != null )
			for ( IProfile profile: model.getProfiles() ) {
				append(checksumBuilder, "profile id", profile.getId());
				append(checksumBuilder, "profile name", profile.getName());
				append(checksumBuilder, "image path", profile.getImagePath());
				append(checksumBuilder, "concept type", profile.getConceptType());
			}
		
		return calculateChecksum(checksumBuilder);
	}
	
	/**
	 * Calculate the checksum of an object.<br>
	 * Please note that this method is *NOT* recursive: the recursion should be managed at a higher level for folders and views.
	 * @param eObject 
	 * @return the eObject's checksum, empty string ("") if the eObject is null
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException
	 * @throws NullPointerException if the EObject is missing information in case of an inconsistent model (like source or target missing for a relationship or a connection).   
	 */
	public static String calculateChecksum(EObject eObject) throws NoSuchAlgorithmException, UnsupportedEncodingException, NullPointerException {
		if ( eObject == null )
			return "";
		
		DBMetadata metadata = DBMetadata.getDBMetadata(eObject);
		
		// warning, this is VERY verbose
		//if ( logger.isTraceEnabled() ) {
		//	logger.trace("Calculating checksum of "+metadata.getDebugName());
		//}
		
		StringBuilder checksumBuilder = new StringBuilder();
		DBMetadata dbMetadata = DBMetadata.getDBMetadata(eObject);
		
		if ( eObject instanceof IIdentifier )						append(checksumBuilder, "id", ((IIdentifier)eObject).getId());
		
		if ( eObject instanceof INameable &&
		        !(eObject instanceof IDiagramModelArchimateObject) &&
		        !(eObject instanceof IDiagramModelConnection) )	    append(checksumBuilder, "name", ((INameable)eObject).getName());
		
		if ( eObject instanceof IDocumentable  &&
		        !(eObject instanceof IDiagramModelArchimateObject) &&
		        !(eObject instanceof IDiagramModelConnection) )		append(checksumBuilder, "documentation", ((IDocumentable)eObject).getDocumentation());
		
		if ( eObject instanceof IJunction )							append(checksumBuilder, "junction type", ((IJunction)eObject).getType());
		if ( eObject instanceof IArchimateRelationship ) {			IArchimateConcept source = ((IArchimateRelationship)eObject).getSource();
															        if ( source == null ) {
															        	logger.error("No source found on "+metadata.getDebugName());
															        	throw new NullPointerException("The relationship has got no source");
															        }
															        append(checksumBuilder, "source id", source.getId());
															        
															        IArchimateConcept target = ((IArchimateRelationship)eObject).getTarget();
															        if ( target == null ) {
															        	logger.error("No target found on "+metadata.getDebugName());
															        	throw new NullPointerException("The relationship has got no source");
															        }
																	append(checksumBuilder, "target id", target.getId());
			if ( eObject instanceof IInfluenceRelationship )		append(checksumBuilder, "strength", ((IInfluenceRelationship)eObject).getStrength());
			if ( eObject instanceof IAccessRelationship )			append(checksumBuilder, "access type", ((IAccessRelationship)eObject).getAccessType());
		}
		if ( eObject instanceof IFolder )							append(checksumBuilder, "folder type", ((IFolder)eObject).getType().getLiteral());
		if ( eObject instanceof IArchimateDiagramModel )			append(checksumBuilder, "viewpoint", ((IArchimateDiagramModel)eObject).getViewpoint());
		if ( eObject instanceof IDiagramModel )	{					append(checksumBuilder, "router type", ((IDiagramModel)eObject).getConnectionRouterType());
																	append(checksumBuilder, "screenshot", dbMetadata.getScreenshot().getBytes());
		}
		else if ( eObject instanceof IDiagramModelContainer ) {		EObject container = ((IDiagramModelContainer)eObject).eContainer();
																	if ( container == null ) {
																		logger.error("No container found on "+metadata.getDebugName());
																		throw new NullPointerException("Container not found");
																	}
																	append(checksumBuilder, "container", ((IIdentifier)container).getId());
		}
		if ( eObject instanceof IBorderObject )						append(checksumBuilder, "border color", ((IBorderObject)eObject).getBorderColor());
		if ( eObject instanceof IDiagramModelNote )					append(checksumBuilder, "border type", ((IDiagramModelNote)eObject).getBorderType());
		if ( eObject instanceof IDiagramModelArchimateObject )		append(checksumBuilder, "type", ((IDiagramModelArchimateObject)eObject).getType());
		if ( eObject instanceof IDiagramModelConnection ) {			append(checksumBuilder, "type", ((IDiagramModelConnection)eObject).getType());			// we do not use getText as it is deprecated
																	
																	IConnectable source = ((IDiagramModelConnection)eObject).getSource();
		                                                            if ( source == null ) {
		                                                            	logger.error("No source found on "+metadata.getDebugName());
		                                                            	throw new NullPointerException("The diagram connection has got no source");
		                                                            }
																	append(checksumBuilder, "source id", source.getId());
																	
																	IConnectable target = ((IDiagramModelConnection)eObject).getTarget();
																	if ( target == null ) {
																		logger.error("No target found on "+metadata.getDebugName());
																		throw new NullPointerException("The diagram connection has got no target");
																	}
		                                                            append(checksumBuilder, "target id", target.getId());
																	append(checksumBuilder, "text position", ((IDiagramModelConnection)eObject).getTextPosition());
																	if ( ((IDiagramModelConnection)eObject).getBendpoints() != null ) {
																		for (IDiagramModelBendpoint point: ((IDiagramModelConnection)eObject).getBendpoints()) {
																			append(checksumBuilder, "bendpoint start x", point.getStartX());
																			append(checksumBuilder, "bendpoint start y", point.getStartY());
																			append(checksumBuilder, "bendpoint end x", point.getEndX());
																			append(checksumBuilder, "bendpoint end y" ,point.getEndY());
																		}
																	}
		}
		if ( eObject instanceof IDiagramModelImageProvider )		append(checksumBuilder, "image path", ((IDiagramModelImageProvider)eObject).getImagePath());
		if ( eObject instanceof IDiagramModelObject ) {				append(checksumBuilder, "fill color", ((IDiagramModelObject)eObject).getFillColor());
																	append(checksumBuilder, "alpha", dbMetadata.getAlpha());		// from Archi 4.3
																	IBounds bounds = ((IDiagramModelObject)eObject).getBounds();
																	append(checksumBuilder, "bounds x", bounds.getX());
																	append(checksumBuilder, "bounds y", bounds.getY());
																	append(checksumBuilder, "bounds width" ,bounds.getWidth());
																	append(checksumBuilder, "bounds height", bounds.getHeight());
		}
		if ( eObject instanceof IDiagramModelArchimateComponent ) { IArchimateConcept concept = ((IDiagramModelArchimateComponent)eObject).getArchimateConcept();
																	if ( concept == null ) {
																		logger.error("No archimate concept linked to "+metadata.getDebugName());
																		throw new NullPointerException("No archimate concept linked to the diagram object");
																	}
																	append(checksumBuilder, "archimate concept", concept.getId());
		}
		if ( eObject instanceof IDiagramModelArchimateConnection ) {IArchimateRelationship relationship = ((IDiagramModelArchimateConnection)eObject).getArchimateConcept();
																	if ( relationship == null ) {
																		logger.error("No relationship linked to "+metadata.getDebugName());
																		throw new NullPointerException("No relationship linked to the diagram connection");
																	}
																	append(checksumBuilder, "archimate concept", ((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId());
		}
		if ( eObject instanceof IFontAttribute ) {					append(checksumBuilder, "font", ((IFontAttribute)eObject).getFont());
																	append(checksumBuilder, "font color", ((IFontAttribute)eObject).getFontColor());
		}
		if ( eObject instanceof ILineObject ) {						append(checksumBuilder, "line width", ((ILineObject)eObject).getLineWidth());
																	append(checksumBuilder, "line color", ((ILineObject)eObject).getLineColor());
		}
		if ( eObject instanceof ILockable )							append(checksumBuilder, "lockable", ((ILockable)eObject).isLocked());
		if ( eObject instanceof ISketchModel )						append(checksumBuilder, "background", ((ISketchModel)eObject).getBackground());
		if ( eObject instanceof ITextAlignment )					append(checksumBuilder, "text alignment", ((ITextAlignment)eObject).getTextAlignment());
        if ( eObject instanceof ITextPosition )						append(checksumBuilder, "text position", ((ITextPosition)eObject).getTextPosition());
		if ( eObject instanceof ITextContent )						append(checksumBuilder, "content", ((ITextContent)eObject).getContent());
		if ( dbMetadata.getImagePosition() != null ) 				append(checksumBuilder, "image position", dbMetadata.getImagePosition());
		if ( eObject instanceof INotesContent )						append(checksumBuilder, "notes", ((INotesContent)eObject).getNotes());
		if ( eObject instanceof IProperties &&
		        !(eObject instanceof IDiagramModelArchimateObject) &&
		        !(eObject instanceof IDiagramModelConnection) &&
		        (((IProperties)eObject).getProperties() != null)){	for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
																		append(checksumBuilder, "property key", prop.getKey());
																		append(checksumBuilder, "property value", prop.getValue());
		        													}
		}
		if ( (eObject instanceof IFeatures) &&
				(((IFeatures)eObject).getFeatures() != null) ) {	for ( IFeature feature: ((IFeatures)eObject).getFeatures() ) {
																		append(checksumBuilder, "feature name", feature.getName());
																		append(checksumBuilder, "feature value", feature.getValue());
		        													}
		}
		if ( eObject instanceof IProfile) {							append(checksumBuilder, "is specialization", ((IProfile)eObject).isSpecialization());
																	append(checksumBuilder, "image path", ((IProfile)eObject).getImagePath());
																	append(checksumBuilder, "concept type", ((IProfile)eObject).getConceptType());
			
		}
		if ( eObject instanceof IProfiles) {						for ( IProfile profile: ((IProfiles)eObject).getProfiles() )
																		append(checksumBuilder, "has profile", profile.getId());
		}
		
		return calculateChecksum(checksumBuilder);
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param name was used for log purpose but is not used anymore
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, String name, String value) {
		String sValue = (value == null ? "" : value);

	    sb.append(startOfText+sValue+endOfText);
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param name was used for log purpose but is not used anymore
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, String name, byte[] value) {
		String sValue = (value == null ? "" : new String(value));

	    append(sb, name, sValue);
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param name was used for log purpose but is not used anymore
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, String name, int value) {
		append(sb, name, String.valueOf(value));
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param name was used for log purpose but is not used anymore
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, String name, boolean value) {
		append(sb, name, String.valueOf(value));
	}
	
	/**
	 * Calculate a MD5 from a StringBuilder
	 * @param input 
	 * @return 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String calculateChecksum(StringBuilder input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return calculateChecksum(input.toString().getBytes("UTF-8"));
	}
	
	/**
	 * Calculate a MD5 from a String
	 * @param input 
	 * @return 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String calculateChecksum(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return calculateChecksum(input.getBytes("UTF-8"));
	}
	
	/**
	 * Calculate a MD5 from a byte array
	 * @param bytes 
	 * @return 
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
			DBGuiUtils.popup(Level.ERROR, "Failed to calculate checksum.", e);
			throw e;
		}

		return md5.toString();
	}
}
