/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.database.data;

import java.util.ArrayList;

import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;

import lombok.Getter;

/**
 * This enum specifies how individual components may be imported.
 * <ul>
 * <li><b>templateMode</b>: the components are imported in copy mode, except the concept that have the "template" property's value set to "shared" which will be imported in shared mode.</li>
 * <li><b>forceSharedMode</b>: the components are imported in shared mode (i.e. modifications done will be seen by other models).</li>
 * <li><b>forceCopyMode</b>: the components are imported in copy mode (i.e. modifications done are local to the model and are not seen by other models).</li>
 * </ul>
 * @author Herve Jouin
 */
public enum DBImportMode {
	/**
	 * In <b>templateMode</b>, the components are imported in copy mode, except the concept that have the "template" property's value set to "shared" which will be imported in shared mode.
	 */
	templateMode(DBImportMode.templateModeValue, "template mode"),
	/**
	 * In <b>forceSharedMode</b>, the components are imported in shared mode (i.e. modifications done will be seen by other models).
	 */
	forceSharedMode(DBImportMode.forceSharedModeValue, "force shared mode"),
	/**
	 * In <b>forceCopyMode</b>, the components are imported in copy mode (i.e. modifications done are local to the model and are not seen by other models).
	 */
	forceCopyMode(DBImportMode.forceCopyModeValue, "force copy mode");

	protected static final DBLogger logger = new DBLogger(DBImportMode.class);

	@Getter private int value;
	@Getter private String label;

	public static final int templateModeValue = 1;
	public static final int forceSharedModeValue = 2;
	public static final int forceCopyModeValue = 3;

	private DBImportMode(int v, String l) {
		this.value = v;
		this.label = l;
	}

	public static String getLabel(int value) {
		switch ( value ) {
			case DBImportMode.templateModeValue: return templateMode.getLabel();
			case DBImportMode.forceSharedModeValue: return forceSharedMode.getLabel();
			case DBImportMode.forceCopyModeValue: return forceCopyMode.getLabel();
			default: return null;
		}
	}
	
	public static DBImportMode get(int value) {
		switch ( value ) {
			case DBImportMode.templateModeValue: return templateMode;
			case DBImportMode.forceSharedModeValue: return forceSharedMode;
			case DBImportMode.forceCopyModeValue: return forceCopyMode;
			default: return null;
		}
	}

	/**
	 * @param properties Array with Archimate component properties
	 * @return true if import should be done in copy mode, false if import should be done in share mode
	 */
	public boolean shouldCreateCopy(ArrayList<DBProperty> properties) {
		switch ( this.value ) {
			case DBImportMode.forceSharedModeValue:
				logger.debug("   Import in forced shared mode.");
				return false;

			case DBImportMode.forceCopyModeValue:
				logger.debug("   Import in forced copy mode.");
				return true;

			case DBImportMode.templateModeValue:
				if ( properties != null ) {
					for ( DBProperty prop: properties) {
						if ( DBPlugin.areEqual(prop.getKey(), "template") && DBPlugin.areEqual(prop.getValue(), "copy") ) {
							logger.debug("   Import in copy mode (as specified by the \"template\" property)");
							return true;
						}
					}
				}
				logger.debug("   Import in shared mode (no \"template\" property or value different from \"copy\"");
				return false;

			default:
				logger.debug("   Unknown import mode: defaulting to shared mode.");
				return false;
		}

	}
}
