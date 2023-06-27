/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Level;
import org.archicontribs.database.DBException;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.gui.DBGuiUtils;
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
	
	private static final char START_OF_TEXT = (char)2;
	private static final char END_OF_TEXT = (char)3;
	
	/**
	 * Calculate the checksum of a model.<br>
	 * Please note that this method is *NOT* recursive: the checksum only considers the information of the model itself.
	 * @param model 
	 * @param releaseNote 
	 * @return 
	 * @throws DBException
	 */
	public static String calculateChecksum(IArchimateModel model, String releaseNote) throws DBException {
		StringBuilder checksumBuilder = new StringBuilder();
		
		append(checksumBuilder, model.getId());
		append(checksumBuilder, model.getName());
		append(checksumBuilder, model.getPurpose());
		append(checksumBuilder, releaseNote);
		
		if ( model.getProperties() != null )
			for ( IProperty prop: model.getProperties() ) {
				append(checksumBuilder, prop.getKey());
				append(checksumBuilder, prop.getValue());
			}
		
		if ( model.getFeatures() != null )
			for ( IFeature feature: model.getFeatures() ) {
				append(checksumBuilder, feature.getName());
				append(checksumBuilder, feature.getValue());
			}
		
		if ( model.getProfiles() != null )
			for ( IProfile profile: model.getProfiles() ) {
				append(checksumBuilder, profile.getId());
				append(checksumBuilder, profile.getName());
				append(checksumBuilder, profile.getImagePath());
				append(checksumBuilder, profile.getConceptType());
			}
		
		return calculateChecksum(checksumBuilder);
	}
	
	/**
	 * Calculate the checksum of an object.<br>
	 * Please note that this method is *NOT* recursive: the recursion should be managed at a higher level for folders and views.
	 * @param eObject 
	 * @return the eObject's checksum, empty string ("") if the eObject is null
	 * @throws DBException
	 */
	public static String calculateChecksum(EObject eObject) throws DBException {
		if ( eObject == null )
			return "";
		
		DBMetadata metadata = DBMetadata.getDBMetadata(eObject);
		
		// warning, this is VERY verbose
		//if ( logger.isTraceEnabled() ) {
		//	logger.trace("Calculating checksum of "+metadata.getDebugName());
		//}
		
		StringBuilder checksumBuilder = new StringBuilder();
		DBMetadata dbMetadata = DBMetadata.getDBMetadata(eObject);
		
		if ( eObject instanceof IIdentifier )						append(checksumBuilder, ((IIdentifier)eObject).getId());
		
		if ( eObject instanceof INameable &&
		        !(eObject instanceof IDiagramModelArchimateObject) &&
		        !(eObject instanceof IDiagramModelConnection) )	    append(checksumBuilder, ((INameable)eObject).getName());
		
		if ( eObject instanceof IDocumentable  &&
		        !(eObject instanceof IDiagramModelArchimateObject) &&
		        !(eObject instanceof IDiagramModelConnection) )		append(checksumBuilder, ((IDocumentable)eObject).getDocumentation());
		
		if ( eObject instanceof IJunction )							append(checksumBuilder, ((IJunction)eObject).getType());
		if ( eObject instanceof IArchimateRelationship ) {			IArchimateConcept source = ((IArchimateRelationship)eObject).getSource();
															        if ( source == null ) {
															        	logger.error("No source found on "+metadata.getDebugName());
															        	throw new NullPointerException("The relationship has got no source");
															        }
															        append(checksumBuilder, source.getId());
															        
															        IArchimateConcept target = ((IArchimateRelationship)eObject).getTarget();
															        if ( target == null ) {
															        	logger.error("No target found on "+metadata.getDebugName());
															        	throw new NullPointerException("The relationship has got no source");
															        }
																	append(checksumBuilder, target.getId());
			if ( eObject instanceof IInfluenceRelationship )		append(checksumBuilder, ((IInfluenceRelationship)eObject).getStrength());
			if ( eObject instanceof IAccessRelationship )			append(checksumBuilder, ((IAccessRelationship)eObject).getAccessType());
		}
		if ( eObject instanceof IFolder )							append(checksumBuilder, ((IFolder)eObject).getType().getLiteral());
		if ( eObject instanceof IArchimateDiagramModel )			append(checksumBuilder, ((IArchimateDiagramModel)eObject).getViewpoint());
		if ( eObject instanceof IDiagramModel )	{					append(checksumBuilder, ((IDiagramModel)eObject).getConnectionRouterType());
																	append(checksumBuilder, dbMetadata.getScreenshot().getBytes());
		}
		else if ( eObject instanceof IDiagramModelContainer ) {		EObject container = ((IDiagramModelContainer)eObject).eContainer();
																	if ( container == null ) {
																		logger.error("No container found on "+metadata.getDebugName());
																		throw new NullPointerException("Container not found");
																	}
																	append(checksumBuilder, ((IIdentifier)container).getId());
		}
		if ( eObject instanceof IBorderObject )						append(checksumBuilder, ((IBorderObject)eObject).getBorderColor());
		if ( eObject instanceof IDiagramModelNote )					append(checksumBuilder, ((IDiagramModelNote)eObject).getBorderType());
		if ( eObject instanceof IDiagramModelArchimateObject )		append(checksumBuilder, ((IDiagramModelArchimateObject)eObject).getType());
		if ( eObject instanceof IDiagramModelConnection ) {			append(checksumBuilder, ((IDiagramModelConnection)eObject).getType());			// we do not use getText as it is deprecated
																	
																	IConnectable source = ((IDiagramModelConnection)eObject).getSource();
		                                                            if ( source == null ) {
		                                                            	logger.error("No source found on "+metadata.getDebugName());
		                                                            	throw new NullPointerException("The diagram connection has got no source");
		                                                            }
																	append(checksumBuilder, source.getId());
																	
																	IConnectable target = ((IDiagramModelConnection)eObject).getTarget();
																	if ( target == null ) {
																		logger.error("No target found on "+metadata.getDebugName());
																		throw new NullPointerException("The diagram connection has got no target");
																	}
		                                                            append(checksumBuilder, target.getId());
																	append(checksumBuilder, ((IDiagramModelConnection)eObject).getTextPosition());
																	if ( ((IDiagramModelConnection)eObject).getBendpoints() != null ) {
																		for (IDiagramModelBendpoint point: ((IDiagramModelConnection)eObject).getBendpoints()) {
																			append(checksumBuilder, point.getStartX());
																			append(checksumBuilder, point.getStartY());
																			append(checksumBuilder, point.getEndX());
																			append(checksumBuilder, point.getEndY());
																		}
																	}
		}
		if ( eObject instanceof IDiagramModelImageProvider )		append(checksumBuilder, ((IDiagramModelImageProvider)eObject).getImagePath());
		if ( eObject instanceof IDiagramModelObject ) {				append(checksumBuilder, ((IDiagramModelObject)eObject).getFillColor());
																	append(checksumBuilder, dbMetadata.getAlpha());		// from Archi 4.3
																	IBounds bounds = ((IDiagramModelObject)eObject).getBounds();
																	append(checksumBuilder, bounds.getX());
																	append(checksumBuilder, bounds.getY());
																	append(checksumBuilder, bounds.getWidth());
																	append(checksumBuilder, bounds.getHeight());
		}
		if ( eObject instanceof IDiagramModelArchimateComponent ) { IArchimateConcept concept = ((IDiagramModelArchimateComponent)eObject).getArchimateConcept();
																	if ( concept == null ) {
																		logger.error("No archimate concept linked to "+metadata.getDebugName());
																		throw new NullPointerException("No archimate concept linked to the diagram object");
																	}
																	append(checksumBuilder, concept.getId());
		}
		if ( eObject instanceof IDiagramModelArchimateConnection ) {IArchimateRelationship relationship = ((IDiagramModelArchimateConnection)eObject).getArchimateConcept();
																	if ( relationship == null ) {
																		logger.error("No relationship linked to "+metadata.getDebugName());
																		throw new NullPointerException("No relationship linked to the diagram connection");
																	}
																	append(checksumBuilder, ((IDiagramModelArchimateConnection)eObject).getArchimateConcept().getId());
		}
		if ( eObject instanceof IFontAttribute ) {					append(checksumBuilder, ((IFontAttribute)eObject).getFont());
																	append(checksumBuilder, ((IFontAttribute)eObject).getFontColor());
		}
		if ( eObject instanceof ILineObject ) {						append(checksumBuilder, ((ILineObject)eObject).getLineWidth());
																	append(checksumBuilder, ((ILineObject)eObject).getLineColor());
		}
		if ( eObject instanceof ILockable )							append(checksumBuilder, ((ILockable)eObject).isLocked());
		if ( eObject instanceof ISketchModel )						append(checksumBuilder, ((ISketchModel)eObject).getBackground());
		if ( eObject instanceof ITextAlignment )					append(checksumBuilder, ((ITextAlignment)eObject).getTextAlignment());
        if ( eObject instanceof ITextPosition )						append(checksumBuilder, ((ITextPosition)eObject).getTextPosition());
		if ( eObject instanceof ITextContent )						append(checksumBuilder, ((ITextContent)eObject).getContent());
		if ( dbMetadata.getImagePosition() != null ) 				append(checksumBuilder, dbMetadata.getImagePosition());
		if ( eObject instanceof INotesContent )						append(checksumBuilder, ((INotesContent)eObject).getNotes());
		if ( eObject instanceof IProperties &&
		        !(eObject instanceof IDiagramModelArchimateObject) &&
		        !(eObject instanceof IDiagramModelConnection) &&
		        (((IProperties)eObject).getProperties() != null)){	for ( IProperty prop: ((IProperties)eObject).getProperties() ) {
																		append(checksumBuilder, prop.getKey());
																		append(checksumBuilder, prop.getValue());
		        													}
		}
		if ( (eObject instanceof IFeatures) &&
				(((IFeatures)eObject).getFeatures() != null) ) {	for ( IFeature feature: ((IFeatures)eObject).getFeatures() ) {
																		append(checksumBuilder, feature.getName());
																		append(checksumBuilder, feature.getValue());
		        													}
		}
		if ( eObject instanceof IProfile) {							append(checksumBuilder, ((IProfile)eObject).isSpecialization());
																	append(checksumBuilder, ((IProfile)eObject).getImagePath());
																	append(checksumBuilder, ((IProfile)eObject).getConceptType());
			
		}
		if ( eObject instanceof IProfiles) {						for ( IProfile profile: ((IProfiles)eObject).getProfiles() )
																		append(checksumBuilder, profile.getId());
		}
		
		return calculateChecksum(checksumBuilder);
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, String value) {
		String sValue = (value == null ? "" : value);

	    sb.append(START_OF_TEXT+sValue+END_OF_TEXT);
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, byte[] value) {
		String sValue = (value == null ? "" : new String(value));

	    append(sb, sValue);
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, int value) {
		append(sb, String.valueOf(value));
	}
	
	/**
	 * Adds the value to the StringBuilder that will be used to calculate the checksum.
	 * @param sb StringBuilder that will be used to calculate the checksum
	 * @param value value to add
	 */
	public static void append(StringBuilder sb, boolean value) {
		append(sb, String.valueOf(value));
	}
	
	/**
	 * Calculate a MD5 from a StringBuilder
	 * @param input 
	 * @return 
	 * @throws DBException
	 */
	public static String calculateChecksum(StringBuilder input) throws DBException {
		return calculateChecksum(input.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Calculate a MD5 from a String
	 * @param input 
	 * @return 
	 * @throws DBException
	 */
	public static String calculateChecksum(String input) throws DBException {
		return calculateChecksum(input.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Calculate a MD5 from a byte array
	 * @param bytes 
	 * @return 
	 * @throws DBException 
	 */
	public static String calculateChecksum(byte[] bytes) throws DBException {
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
			DBException exception = new DBException("Failed to calculate checksum");
			exception.initCause(e);
			throw exception;
		}

		return md5.toString();
	}
}
