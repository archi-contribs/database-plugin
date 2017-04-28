/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.help;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

    protected void makeActions(IWorkbenchWindow window) {
            register(ActionFactory.HELP_SEARCH.create(window));
            register(ActionFactory.DYNAMIC_HELP.create(window));
    }

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
            super(configurer);
    }
}
