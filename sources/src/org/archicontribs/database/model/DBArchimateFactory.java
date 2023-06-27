/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.impl.ArchimateFactory;

/**
 * The <b>DBArchimateFactory</b> class overrides the com.archimatetool.model.impl.ArchimateFactory class<br>
 * It allows to create DBArchimateModel instances instead of standard ArchimaeModel instances.
 *
 * @author Herve JOUIN
 * @see com.archimatetool.model.impl.ArchimateFactory
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DBArchimateFactory extends ArchimateFactory {
	static DBLogger logger = new DBLogger(DBArchimateFactory.class);
	static boolean ignoreNext = false;
	
	/**
	 * Instance of the DBArchimateFactory class
	 */
	@SuppressWarnings("hiding")
	public static final DBArchimateFactory eINSTANCE = init();
	
    public static DBArchimateFactory init() {
    	if ( logger.isDebugEnabled() )
    		logger.debug("Initializing DBArchimateFactory");
        
    	if ( eINSTANCE==null )
        	return new DBArchimateFactory();
        return eINSTANCE;
    }
	
    /**
     * Override of the original ArchimateFactory<br>
	 * Creates a DBxxxx instead of a xxxx objects that include DBMetadata properties 
     */
	public DBArchimateFactory() {
		super();
	}
	
	
	/**
	 * Override of the original createArchimateModel<br>
	 * Creates a DBArchimateModel instead of a ArchimateModel 
	 */
    @Override
    public IArchimateModel createArchimateModel() {
        return new org.archicontribs.database.model.DBArchimateModel();
    }
}
