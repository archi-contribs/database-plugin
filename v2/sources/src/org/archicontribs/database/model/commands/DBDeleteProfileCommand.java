package org.archicontribs.database.model.commands;

import java.util.List;

import org.archicontribs.database.model.DBArchimateModel;
import org.eclipse.gef.commands.CompoundCommand;

import com.archimatetool.editor.model.commands.RemoveListMemberCommand;
import com.archimatetool.model.IProfile;
import com.archimatetool.model.IProfiles;

/**
 * This class is base upon Archi class deleteProfileCommand. It calls Archi's RemoveListMemberCommand to delete a profile (ie specialization) in the model.<br>
 * <br>
 * It is necessary as Archi declares its own class as private so we cannot instantiate it :(
 */
public class DBDeleteProfileCommand extends CompoundCommand {
	/**
	 * Deletes a profile in a model, including all the profile usages
	 * @param model
	 * @param profile
	 */
	public DBDeleteProfileCommand(DBArchimateModel model, IProfile profile) {
		
		// Delete profile from Model
		add(new RemoveListMemberCommand<IProfile>(profile.getArchimateModel().getProfiles(), profile));

		// Delete profile usages
        List<IProfiles> usages = model.getAllProfilesUsages().get(profile);
		if(usages != null) {
			for(IProfiles owner: usages) {
				add(new RemoveListMemberCommand<IProfile>(owner.getProfiles(), profile));
			}
		}
	}
}
