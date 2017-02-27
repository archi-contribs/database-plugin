package org.archicontribs.database;

import java.sql.Timestamp;

import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;

/**
 * This class defines the metadata attached to every model components.
 * 
 * @author Herve Jouin 
 * @see org.archicontribs.database.IDBMetadata
 */
public class DBMetadata  {
	/**
	 * What to do with the component in case of conflict.<br>
	 * Beware, the preferences have precedence over this local variable
	 */
	public enum CONFLICT_CHOICE {askUser, doNotExport, exportToDatabase, importFromDatabase};
	private CONFLICT_CHOICE conflictChoice = CONFLICT_CHOICE.askUser;
	
	/**
	 * Current version of the component.<br>
	 * This property is set when the component is imported or after a successful export.
	 */
	private int currentVersion = 0;
	
	/**
	 * Exported version of the component.<br>
	 * This property is set during the export process and copied to the current version if the transaction is committed to the database .
	 */
	private int exportedVersion = 0;
	
	/**
	 * Version of the component in the database.<br>
	 * This property is retrieved from the database each time a connection is made to a database.
	 */
	private int databaseVersion = 0;
	
	/**
	 * Checksum of the component<br>
	 * This property is set when the component is imported or before an export.
	 */
	private String currentChecksum = "";
	
	/**
	 * Checksum of the component in the database<br>
	 * This property is retrieved from the database each time a connection is made to a database.
	 */
	private String databaseChecksum = "";
	
	/**
	 * Username of the last person who exported the component in the database
	 */
	private String databaseCreatedBy = null;
	
	/**
	 * Timestamp of the component in the database
	 */
	private Timestamp databaseCreatedOn = null;
	
	/**
	 * Indicates if the component needs to be exported to the database.
	 */
	//private boolean shouldExport = false;
	
	/**
	 * Indicates if the component needs to be imported from the database.
	 */
	//private boolean shouldImport = false;
	
	/**
	 * Component that contains the DBMetadata<br>
	 * This property is set during the component initialization and is used to calculate the component checksum
	 */
	private EObject component;
	
	public DBMetadata(EObject component) {
		assert ( component instanceof IIdentifier );
		this.component = component;
	}
	
	public int getCurrentVersion() {
		return currentVersion;
	}
	
	public void setCurrentVersion(int version) {
		currentVersion = version;
	}
	
	public int getExportedVersion() {
		return exportedVersion;
	}
	
	public void setExportedVersion(int version) {
		exportedVersion = version;
	}
	
	public int getDatabaseVersion() {
		return databaseVersion;
	}
	
	public void setDatabaseVersion(int version) {
		databaseVersion = version;
	}
	
	public String getDatabaseCreatedBy() {
		return databaseCreatedBy;
	}
	
	public void setDatabaseCreatedBy(String username) {
		databaseCreatedBy = username;
	}
	
	public Timestamp getDatabaseCreatedOn() {
		return databaseCreatedOn;
	}
	
	public void setDatabaseCreatedOn(Timestamp timestamp) {
		databaseCreatedOn = timestamp;
	}
	
	public CONFLICT_CHOICE getConflictChoice() {
		return conflictChoice;
	}
	
	public void setConflictChoice(CONFLICT_CHOICE choice) {
		conflictChoice = choice;
	}
	
	public String getCurrentChecksum() {
		if ( currentChecksum.isEmpty() )
			calculateChecksum();
		return currentChecksum;
	}
	
	public void setCurrentChecksum(String checksum) {
		currentChecksum = checksum;
	}
	
	public String getDatabaseChecksum() {
		return databaseChecksum;
	}
	
	public void setDatabaseChecksum(String checksum) {
		databaseChecksum = checksum;
	}
	
	public boolean isUpdated() {
		return !getCurrentChecksum().equals(databaseChecksum);
	}
	
	/**
	 * Calculates the checksum of the component<br>
	 * To ease the comparison of two components, the ID is not taken in account in the checksum.
	 * @return the checksum value
	 */
	public String calculateChecksum() {
		StringBuilder checksum = new StringBuilder();
		
			//TODO : the method should be extended to work for all classes !!!
		checksum.append(component.getClass().getSimpleName());
		if ( component instanceof INameable )				checksum.append(((INameable)component).getName());
		if ( component instanceof IDocumentable )			checksum.append(((IDocumentable)component).getDocumentation());
		if ( component instanceof IArchimateRelationship )	checksum.append(((IArchimateRelationship)component).getSource().getId());
		if ( component instanceof IArchimateRelationship )	checksum.append(((IArchimateRelationship)component).getTarget().getId());
		if ( component instanceof IInfluenceRelationship )	checksum.append(((IInfluenceRelationship)component).getStrength());
		if ( component instanceof IAccessRelationship )		checksum.append(((IAccessRelationship)component).getAccessType());
			// not used at the moment
		//if ( component instanceof IFolder )	{
		//	for ( EObject obj: ((IFolder)component).getElements() ) {	checksum.append(((IIdentifier)obj).getId()); }
		//	for ( IFolder sub: ((IFolder)component).getFolders() ) {	checksum.append(sub.getId()); }
		//}
		if ( component instanceof IProperties ) {
			for ( IProperty prop: ((IProperties)component).getProperties() ) {
				checksum.append(prop.getKey());
				checksum.append(prop.getValue());
			}
		}
		
		currentChecksum = DBPlugin.calculateChecksum(checksum.toString().getBytes());
		return currentChecksum;
	}
	
	private String fullName = null;
	/**
	 * Calculates the full name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\""
	 */
	public String getFullName() {
		if ( fullName == null ) {
			StringBuilder objName = new StringBuilder(component.getClass().getSimpleName());
			objName.append(":\""+((INameable)component).getName()+"\"");
			fullName = objName.toString();
		}
		return fullName;
	}
	
	private String debugName = null;
	/**
	 * Calculates the debug name of the component 
	 * @return getclass().getSimpleName()+":\""+getName()+"\"("+getId()+")"
	 */
	public String getDebugName() {
		if ( debugName == null ) {
			StringBuilder objName = new StringBuilder(getFullName());
			objName.append("("+((IIdentifier)component).getId()+")");
			debugName = objName.toString();
		}
		return debugName;
	}
}
