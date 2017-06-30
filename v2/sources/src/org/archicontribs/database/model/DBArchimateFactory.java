/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.model;

import org.archicontribs.database.DBLogger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.graphics.Image;

import com.archimatetool.editor.ArchiPlugin;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.ImageFactory;
import com.archimatetool.model.IAccessRelationship;
import com.archimatetool.model.IAggregationRelationship;
import com.archimatetool.model.IApplicationCollaboration;
import com.archimatetool.model.IApplicationComponent;
import com.archimatetool.model.IApplicationEvent;
import com.archimatetool.model.IApplicationFunction;
import com.archimatetool.model.IApplicationInteraction;
import com.archimatetool.model.IApplicationInterface;
import com.archimatetool.model.IApplicationProcess;
import com.archimatetool.model.IApplicationService;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArtifact;
import com.archimatetool.model.IAssessment;
import com.archimatetool.model.IAssignmentRelationship;
import com.archimatetool.model.IAssociationRelationship;
import com.archimatetool.model.IBusinessActor;
import com.archimatetool.model.IBusinessCollaboration;
import com.archimatetool.model.IBusinessEvent;
import com.archimatetool.model.IBusinessFunction;
import com.archimatetool.model.IBusinessInteraction;
import com.archimatetool.model.IBusinessInterface;
import com.archimatetool.model.IBusinessObject;
import com.archimatetool.model.IBusinessProcess;
import com.archimatetool.model.IBusinessRole;
import com.archimatetool.model.IBusinessService;
import com.archimatetool.model.ICapability;
import com.archimatetool.model.ICommunicationNetwork;
import com.archimatetool.model.ICompositionRelationship;
import com.archimatetool.model.IConstraint;
import com.archimatetool.model.IContract;
import com.archimatetool.model.ICourseOfAction;
import com.archimatetool.model.IDataObject;
import com.archimatetool.model.IDeliverable;
import com.archimatetool.model.IDevice;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelGroup;
import com.archimatetool.model.IDiagramModelImage;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IDistributionNetwork;
import com.archimatetool.model.IDriver;
import com.archimatetool.model.IEquipment;
import com.archimatetool.model.IFacility;
import com.archimatetool.model.IFlowRelationship;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IGap;
import com.archimatetool.model.IGoal;
import com.archimatetool.model.IGrouping;
import com.archimatetool.model.IImplementationEvent;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILocation;
import com.archimatetool.model.IMaterial;
import com.archimatetool.model.IMeaning;
import com.archimatetool.model.INode;
import com.archimatetool.model.IOutcome;
import com.archimatetool.model.IPath;
import com.archimatetool.model.IPlateau;
import com.archimatetool.model.IPrinciple;
import com.archimatetool.model.IProduct;
import com.archimatetool.model.IRealizationRelationship;
import com.archimatetool.model.IRepresentation;
import com.archimatetool.model.IRequirement;
import com.archimatetool.model.IResource;
import com.archimatetool.model.IServingRelationship;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ISketchModelActor;
import com.archimatetool.model.ISketchModelSticky;
import com.archimatetool.model.ISpecializationRelationship;
import com.archimatetool.model.IStakeholder;
import com.archimatetool.model.ISystemSoftware;
import com.archimatetool.model.ITechnologyCollaboration;
import com.archimatetool.model.ITechnologyEvent;
import com.archimatetool.model.ITechnologyFunction;
import com.archimatetool.model.ITechnologyInteraction;
import com.archimatetool.model.ITechnologyInterface;
import com.archimatetool.model.ITechnologyProcess;
import com.archimatetool.model.ITechnologyService;
import com.archimatetool.model.ITriggeringRelationship;
import com.archimatetool.model.IValue;
import com.archimatetool.model.IWorkPackage;
import com.archimatetool.model.impl.ArchimateFactory;

/**
 * The <b>DBArchimateFactory</b> class overrides the com.archimatetool.model.impl.ArchimateFactory class<br>
 * It creates DBxxx classes that extend the standard Archi classes by adding metadata.
 *
 * @author Herve JOUIN
 * @see com.archimatetool.model.impl.ArchimateFactory
 * @see org.archicontribs.database.model.IDBMetadata
 */
public class DBArchimateFactory extends ArchimateFactory {
	final int PHYSICAL_SERVER = 240;
	static DBLogger logger = new DBLogger(DBArchimateFactory.class);
	static boolean ignoreNext = false;
	
	public static DBArchimateFactory eINSTANCE = init();
	
    public static DBArchimateFactory init() {
    	if ( logger.isDebugEnabled() ) logger.debug("initializing DBArchimateFactory");
        return eINSTANCE==null ? new DBArchimateFactory() : eINSTANCE;
    }
	
    /**
     * Override of the original ArchimateFactory<br>
	 * Creates a DBxxxx instead of a xxxx objects that include DBMetadata properties 
     */
	public DBArchimateFactory() {
		super();
	}
	
	/**
	 * Creates a component by its class name
	 */
	public EObject create(String clazz) {
        switch (clazz.toUpperCase()) {
            case "PROPERTY": return createProperty();												// standard -- no DBMetadata
            case "METADATA": return createMetadata();												// standard -- no DBMetadata
            case "FOLDER": return createFolder();													// with DBMetadata
            case "ARCHIMATEMODEL": return createArchimateModel();									// with DBMetadata
            case "JUNCTION": return createJunction();												// with DBMetadata
            case "APPLICATIONCOLLABORATION": return createApplicationCollaboration();				// with DBMetadata
            case "APPLICATIONCOMPONENT": return createApplicationComponent();						// with DBMetadata
            case "APPLICATIONEVENT": return createApplicationEvent();								// with DBMetadata
            case "APPLICATIONFUNCTION": return createApplicationFunction();							// with DBMetadata
            case "APPLICATIONINTERACTION": return createApplicationInteraction();					// with DBMetadata
            case "APPLICATIONINTERFACE": return createApplicationInterface();						// with DBMetadata
            case "APPLICATIONPROCESS": return createApplicationProcess();							// with DBMetadata
            case "APPLICATIONSERVICE": return createApplicationService();							// with DBMetadata
            case "ARTIFACT": return createArtifact();												// with DBMetadata
            case "ASSESSMENT": return createAssessment();											// with DBMetadata
            case "BUSINESSACTOR": return createBusinessActor();										// with DBMetadata
            case "BUSINESSCOLLABORATION": return createBusinessCollaboration();						// with DBMetadata
            case "BUSINESSEVENT": return createBusinessEvent();										// with DBMetadata
            case "BUSINESSFUNCTION": return createBusinessFunction();								// with DBMetadata
            case "BUSINESSINTERACTION": return createBusinessInteraction();							// with DBMetadata
            case "BUSINESSINTERFACE": return createBusinessInterface();								// with DBMetadata
            case "BUSINESSOBJECT": return createBusinessObject();									// with DBMetadata
            case "BUSINESSPROCESS": return createBusinessProcess();									// with DBMetadata
            case "BUSINESSROLE": return createBusinessRole();										// with DBMetadata
            case "BUSINESSSERVICE": return createBusinessService();									// with DBMetadata
            case "CAPABILITY": return createCapability();											// with DBMetadata
            case "COMMUNICATIONNETWORK": return createCommunicationNetwork();						// with DBMetadata
            case "CONTRACT": return createContract();												// with DBMetadata
            case "CONSTRAINT": return createConstraint();											// with DBMetadata
            case "COURSEOFACTION": return createCourseOfAction();									// with DBMetadata
            case "DATAOBJECT": return createDataObject();											// with DBMetadata
            case "DELIVERABLE": return createDeliverable();											// with DBMetadata
            case "DEVICE": return createDevice();													// with DBMetadata
            case "DISTRIBUTIONNETWORK": return createDistributionNetwork();							// with DBMetadata
            case "DRIVER": return createDriver();													// with DBMetadata
            case "EQUIPMENT": return createEquipment();												// with DBMetadata
            case "FACILITY": return createFacility();												// with DBMetadata
            case "GAP": return createGap();															// with DBMetadata
            case "GOAL": return createGoal();														// with DBMetadata
            case "GROUPING": return createGrouping();												// with DBMetadata
            case "IMPLEMENTATIONEVENT": return createImplementationEvent();							// with DBMetadata
            case "LOCATION": return createLocation();												// with DBMetadata
            case "MATERIAL": return createMaterial();												// with DBMetadata
            case "MEANING": return createMeaning();													// with DBMetadata
            case "NODE": return createNode();														// with DBMetadata
            case "OUTCOME": return createOutcome();													// with DBMetadata
            case "PATH": return createPath();														// with DBMetadata
            case "PLATEAU": return createPlateau();													// with DBMetadata
            case "PRINCIPLE": return createPrinciple();												// with DBMetadata
            case "PRODUCT": return createProduct();													// with DBMetadata
            case "REPRESENTATION": return createRepresentation();									// with DBMetadata
            case "RESOURCE": return createResource();												// with DBMetadata
            case "REQUIREMENT": return createRequirement();											// with DBMetadata
            case "STAKEHOLDER": return createStakeholder();											// with DBMetadata
            case "SYSTEMSOFTWARE": return createSystemSoftware();									// with DBMetadata
            case "TECHNOLOGYCOLLABORATION": return createTechnologyCollaboration();					// with DBMetadata
            case "TECHNOLOGYEVENT": return createTechnologyEvent();									// with DBMetadata
            case "TECHNOLOGYFUNCTION": return createTechnologyFunction();							// with DBMetadata
            case "TECHNOLOGYINTERFACE": return createTechnologyInterface();							// with DBMetadata
            case "TECHNOLOGYINTERACTION": return createTechnologyInteraction();						// with DBMetadata
            case "TECHNOLOGYPROCESS": return createTechnologyProcess();								// with DBMetadata
            case "TECHNOLOGYSERVICE": return createTechnologyService();								// with DBMetadata
            case "VALUE": return createValue();														// with DBMetadata
            case "WORKPACKAGE": return createWorkPackage();											// with DBMetadata
            case "ACCESSRELATIONSHIP": return createAccessRelationship();							// with DBMetadata
            case "AGGREGATIONRELATIONSHIP": return createAggregationRelationship();					// with DBMetadata
            case "ASSIGNMENTRELATIONSHIP": return createAssignmentRelationship();					// with DBMetadata
            case "ASSOCIATIONRELATIONSHIP": return createAssociationRelationship();					// with DBMetadata
            case "COMPOSITIONRELATIONSHIP": return createCompositionRelationship();					// with DBMetadata
            case "FLOWRELATIONSHIP": return createFlowRelationship();								// with DBMetadata
            case "INFLUENCERELATIONSHIP": return createInfluenceRelationship();						// with DBMetadata
            case "REALIZATIONRELATIONSHIP": return createRealizationRelationship();					// with DBMetadata
            case "SERVINGRELATIONSHIP": return createServingRelationship();							// with DBMetadata
            case "SPECIALIZATIONRELATIONSHIP": return createSpecializationRelationship();			// with DBMetadata
            case "TRIGGERINGRELATIONSHIP": return createTriggeringRelationship();					// with DBMetadata
            case "DIAGRAMMODELREFERENCE": return createDiagramModelReference();						// with DBMetadata
            case "DIAGRAMMODELGROUP": return createDiagramModelGroup();								// with DBMetadata
            case "DIAGRAMMODELNOTE": return createDiagramModelNote();								// with DBMetadata
            case "DIAGRAMMODELIMAGE": return createDiagramModelImage();								// with DBMetadata
            case "DIAGRAMMODELCONNECTION": return createDiagramModelConnection();					// with DBMetadata
            case "DIAGRAMMODELBENDPOINT": return createDiagramModelBendpoint();						// standard -- no DBMetadata
            case "BOUNDS": return createBounds();													// standard -- no DBMetadata
            case "ARCHIMATEDIAGRAMMODEL": return createArchimateDiagramModel();						// with DBMetadata
            case "DIAGRAMMODELARCHIMATEOBJECT": return createDiagramModelArchimateObject();			// with DBMetadata
            case "DIAGRAMMODELARCHIMATECONNECTION": return createDiagramModelArchimateConnection();	// with DBMetadata
            case "SKETCHMODEL": return createSketchModel();											// with DBMetadata
            case "SKETCHMODELSTICKY": return createSketchModelSticky();								// with DBMetadata
            case "SKETCHMODELACTOR": return createSketchModelActor();								// with DBMetadata
            default:
                throw new IllegalArgumentException("The class '" + clazz + "' is not a valid class"); //$NON-NLS-1$ //$NON-NLS-2$
        }
	}
	
	public static Image getImage(String clazz) {
		ImageFactory ImageFactory = new ImageFactory(ArchiPlugin.INSTANCE);
        switch (clazz.toUpperCase()) {
            case "FOLDER": return ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_FOLDER);
            case "JUNCTION": return ImageFactory.getImage(IArchiImages.ICON_AND_JUNCTION);
            case "APPLICATIONCOLLABORATION": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_COLLABORATION);
            case "APPLICATIONCOMPONENT": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_COMPONENT);
            case "APPLICATIONEVENT": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_EVENT);
            case "APPLICATIONFUNCTION": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_FUNCTION);
            case "APPLICATIONINTERACTION": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_INTERACTION);
            case "APPLICATIONINTERFACE": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_INTERFACE);
            case "APPLICATIONPROCESS": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_PROCESS);
            case "APPLICATIONSERVICE": return ImageFactory.getImage(IArchiImages.ICON_APPLICATION_SERVICE);
            case "ARTIFACT": return ImageFactory.getImage(IArchiImages.ICON_ARTIFACT);
            case "ASSESSMENT": return ImageFactory.getImage(IArchiImages.ICON_ASSESSMENT);
            case "BUSINESSACTOR": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_ACTOR);
            case "BUSINESSCOLLABORATION": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_COLLABORATION);
            case "BUSINESSEVENT": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_EVENT);
            case "BUSINESSFUNCTION": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_FUNCTION);
            case "BUSINESSINTERACTION": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_INTERACTION);
            case "BUSINESSINTERFACE": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_INTERFACE);
            case "BUSINESSOBJECT": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_OBJECT);
            case "BUSINESSPROCESS": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_PROCESS);
            case "BUSINESSROLE": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_ROLE);
            case "BUSINESSSERVICE": return ImageFactory.getImage(IArchiImages.ICON_BUSINESS_SERVICE);
            case "CAPABILITY": return ImageFactory.getImage(IArchiImages.ICON_CAPABILITY);
            case "COMMUNICATIONNETWORK": return ImageFactory.getImage(IArchiImages.ICON_COMMUNICATION_NETWORK);
            case "CONTRACT": return ImageFactory.getImage(IArchiImages.ICON_CONTRACT);
            case "CONSTRAINT": return ImageFactory.getImage(IArchiImages.ICON_CONSTRAINT);
            case "COURSEOFACTION": return ImageFactory.getImage(IArchiImages.ICON_COURSE_OF_ACTION);
            case "DATAOBJECT": return ImageFactory.getImage(IArchiImages.ICON_DATA_OBJECT);
            case "DELIVERABLE": return ImageFactory.getImage(IArchiImages.ICON_DELIVERABLE);
            case "DEVICE": return ImageFactory.getImage(IArchiImages.ICON_DEVICE);
            case "DISTRIBUTIONNETWORK": return ImageFactory.getImage(IArchiImages.ICON_DISTRIBUTION_NETWORK);
            case "DRIVER": return ImageFactory.getImage(IArchiImages.ICON_DRIVER);
            case "EQUIPMENT": return ImageFactory.getImage(IArchiImages.ICON_EQUIPMENT);
            case "FACILITY": return ImageFactory.getImage(IArchiImages.ICON_FACILITY);
            case "GAP": return ImageFactory.getImage(IArchiImages.ICON_GAP);
            case "GOAL": return ImageFactory.getImage(IArchiImages.ICON_GOAL);
            case "GROUPING": return ImageFactory.getImage(IArchiImages.ICON_GROUPING);
            case "IMPLEMENTATIONEVENT": return ImageFactory.getImage(IArchiImages.ICON_IMPLEMENTATION_EVENT);
            case "LOCATION": return ImageFactory.getImage(IArchiImages.ICON_LOCATION);
            case "MATERIAL": return ImageFactory.getImage(IArchiImages.ICON_MATERIAL);
            case "MEANING": return ImageFactory.getImage(IArchiImages.ICON_MEANING);
            case "NODE": return ImageFactory.getImage(IArchiImages.ICON_NODE);
            case "OUTCOME": return ImageFactory.getImage(IArchiImages.ICON_OUTCOME);
            case "PATH": return ImageFactory.getImage(IArchiImages.ICON_PATH);
            case "PLATEAU": return ImageFactory.getImage(IArchiImages.ICON_PLATEAU);
            case "PRINCIPLE": return ImageFactory.getImage(IArchiImages.ICON_PRINCIPLE);
            case "PRODUCT": return ImageFactory.getImage(IArchiImages.ICON_PRODUCT);
            case "REPRESENTATION": return ImageFactory.getImage(IArchiImages.ICON_REPRESENTATION);
            case "RESOURCE": return ImageFactory.getImage(IArchiImages.ICON_RESOURCE);
            case "REQUIREMENT": return ImageFactory.getImage(IArchiImages.ICON_REQUIREMENT);
            case "STAKEHOLDER": return ImageFactory.getImage(IArchiImages.ICON_STAKEHOLDER);
            case "SYSTEMSOFTWARE": return ImageFactory.getImage(IArchiImages.ICON_SYSTEM_SOFTWARE);
            case "TECHNOLOGYCOLLABORATION": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_COLLABORATION);
            case "TECHNOLOGYEVENT": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_EVENT);
            case "TECHNOLOGYFUNCTION": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_FUNCTION);
            case "TECHNOLOGYINTERFACE": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_INTERFACE);
            case "TECHNOLOGYINTERACTION": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_INTERACTION);
            case "TECHNOLOGYPROCESS": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_PROCESS);
            case "TECHNOLOGYSERVICE": return ImageFactory.getImage(IArchiImages.ICON_TECHNOLOGY_SERVICE);
            case "VALUE": return ImageFactory.getImage(IArchiImages.ICON_VALUE);
            case "WORKPACKAGE": return ImageFactory.getImage(IArchiImages.ICON_WORKPACKAGE);
            case "ACCESSRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_ACESS_RELATION);
            case "AGGREGATIONRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_AGGREGATION_RELATION);
            case "ASSIGNMENTRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_ASSIGNMENT_RELATION);
            case "ASSOCIATIONRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_ASSOCIATION_RELATION);
            case "COMPOSITIONRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_COMPOSITION_RELATION);
            case "FLOWRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_FLOW_RELATION);
            case "INFLUENCERELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_INFLUENCE_RELATION);
            case "REALIZATIONRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_REALIZATION_RELATION);
            case "SERVINGRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_SERVING_RELATION);
            case "SPECIALIZATIONRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_SPECIALIZATION_RELATION);
            case "TRIGGERINGRELATIONSHIP": return ImageFactory.getImage(IArchiImages.ICON_TRIGGERING_RELATION);
            case "DIAGRAMMODELGROUP": return ImageFactory.getImage(IArchiImages.ICON_GROUP);
            case "DIAGRAMMODELNOTE": return ImageFactory.getImage(IArchiImages.ICON_NOTE);
            case "ARCHIMATEDIAGRAMMODEL": return ImageFactory.getImage(IArchiImages.ICON_DIAGRAM);
            case "SKETCHMODEL": return ImageFactory.getImage(IArchiImages.ICON_SKETCH);
            case "SKETCHMODELSTICKY": return ImageFactory.getImage(IArchiImages.ICON_STICKY);
            case "SKETCHMODELACTOR": return ImageFactory.getImage(IArchiImages.ICON_ACTOR);
            default:
                throw new IllegalArgumentException("The class '" + clazz + "' is not a valid class"); //$NON-NLS-1$ //$NON-NLS-2$
        }
	}
	
	/**
	 * Override of the original createArchimateDiagramModel<br>
	 * Creates a DBArchimateDiagramModel instead of a ArchimateDiagramModel 
	 */
    public IArchimateDiagramModel createArchimateDiagramModel() {
        return new org.archicontribs.database.model.impl.ArchimateDiagramModel();
    }
	
	/**
	 * Override of the original createArchimateModel<br>
	 * Creates a DBArchimateModel instead of a ArchimateModel 
	 */
    public IArchimateModel createArchimateModel() {
        return new org.archicontribs.database.model.ArchimateModel();
    }
    
	/**
	 * Override of the original createNode<br>
	 * Creates a DBArchimateElement instead of a Node
	 */
    public INode createNode() {
        return new org.archicontribs.database.model.impl.Node();
    }
    
	/**
	 * Override of the original createAccessRelationship<br>
	 * Creates a DBAccessRelationship instead of a AccessRelationship
	 */
    public IAccessRelationship createAccessRelationship() {
        return new org.archicontribs.database.model.impl.AccessRelationship();
    }
    
	/**
	 * Override of the original createAggregationRelationship<br>
	 * Creates a DBAggregationRelationship instead of a AggregationRelationship
	 */
    public IAggregationRelationship createAggregationRelationship() {
        return new org.archicontribs.database.model.impl.AggregationRelationship();
    }
    
	/**
	 * Override of the original createApplicationCollaboration<br>
	 * Creates a DBApplicationCollaboration instead of a ApplicationCollaboration
	 */
    public IApplicationCollaboration createApplicationCollaboration() {
        return new org.archicontribs.database.model.impl.ApplicationCollaboration();
    }
    
	/**
	 * Override of the original createApplicationComponent<br>
	 * Creates a DBApplicationComponent instead of a ApplicationComponent
	 */
    public IApplicationComponent createApplicationComponent() {
        return new org.archicontribs.database.model.impl.ApplicationComponent();
    }
    
	/**
	 * Override of the original createApplicationEvent<br>
	 * Creates a DBApplicationEvent instead of a ApplicationEvent
	 */
    public IApplicationEvent createApplicationEvent() {
        return new org.archicontribs.database.model.impl.ApplicationEvent();
    }
    
	/**
	 * Override of the original createApplicationFunction<br>
	 * Creates a DBApplicationFunction instead of a ApplicationFunction
	 */
    public IApplicationFunction createApplicationFunction() {
        return new org.archicontribs.database.model.impl.ApplicationFunction();
    }
    
	/**
	 * Override of the original createApplicationInteraction<br>
	 * Creates a DBApplicationInteraction instead of a ApplicationInteraction
	 */
    public IApplicationInteraction createApplicationInteraction() {
        return new org.archicontribs.database.model.impl.ApplicationInteraction();
    }
    
	/**
	 * Override of the original createApplicationInterface<br>
	 * Creates a DBApplicationInterface instead of a ApplicationInterface
	 */
    public IApplicationInterface createApplicationInterface() {
        return new org.archicontribs.database.model.impl.ApplicationInterface();
    }
    
	/**
	 * Override of the original createApplicationProcess<br>
	 * Creates a DBApplicationProcess instead of a ApplicationProcess
	 */
    public IApplicationProcess createApplicationProcess() {
        return new org.archicontribs.database.model.impl.ApplicationProcess();
    }
    
	/**
	 * Override of the original createApplicationService<br>
	 * Creates a DBApplicationService instead of a ApplicationService
	 */
    public IApplicationService createApplicationService() {
        return new org.archicontribs.database.model.impl.ApplicationService();
    }
    
	/**
	 * Override of the original createArtifact<br>
	 * Creates a DBArtifact instead of a Artifact
	 */
    public IArtifact createArtifact() {
        return new org.archicontribs.database.model.impl.Artifact();
    }
    
	/**
	 * Override of the original createAssessment<br>
	 * Creates a DBAssessment instead of a Assessment
	 */
    public IAssessment createAssessment() {
        return new org.archicontribs.database.model.impl.Assessment();
    }
    
	/**
	 * Override of the original createAssignmentRelationship<br>
	 * Creates a DBAssignmentRelationship instead of a AssignmentRelationship
	 */
    public IAssignmentRelationship createAssignmentRelationship() {
        return new org.archicontribs.database.model.impl.AssignmentRelationship();
    }
    
	/**
	 * Override of the original createAssociationRelationship<br>
	 * Creates a DBAssociationRelationship instead of a AssociationRelationship
	 */
    public IAssociationRelationship createAssociationRelationship() {
        return new org.archicontribs.database.model.impl.AssociationRelationship();
    }
    
	/**
	 * Override of the original createBusinessActor<br>
	 * Creates a DBBusinessActor instead of a BusinessActor
	 */
    public IBusinessActor createBusinessActor() {
        return new org.archicontribs.database.model.impl.BusinessActor();
    }
    
	/**
	 * Override of the original createBusinessCollaboration<br>
	 * Creates a DBBusinessCollaboration instead of a BusinessCollaboration
	 */
    public IBusinessCollaboration createBusinessCollaboration() {
        return new org.archicontribs.database.model.impl.BusinessCollaboration();
    }
    
	/**
	 * Override of the original createBusinessEvent<br>
	 * Creates a DBBusinessEvent instead of a BusinessEvent
	 */
    public IBusinessEvent createBusinessEvent() {
        return new org.archicontribs.database.model.impl.BusinessEvent();
    }
    
	/**
	 * Override of the original createBusinessRole<br>
	 * Creates a DBBusinessRole instead of a BusinessRole
	 */
    public IBusinessRole createBusinessRole() {
        return new org.archicontribs.database.model.impl.BusinessRole();
    }
    
	/**
	 * Override of the original createBusinessProcess<br>
	 * Creates a DBBusinessProcess instead of a BusinessProcess
	 */
    public IBusinessProcess createBusinessProcess() {
        return new org.archicontribs.database.model.impl.BusinessProcess();
    }
    
	/**
	 * Override of the original createBusinessObject<br>
	 * Creates a DBBusinessObject instead of a BusinessObject
	 */
    public IBusinessObject createBusinessObject() {
        return new org.archicontribs.database.model.impl.BusinessObject();
    }
    
	/**
	 * Override of the original createBusinessInterface<br>
	 * Creates a DBBusinessInterface instead of a BusinessInterface
	 */
    public IBusinessInterface createBusinessInterface() {
        return new org.archicontribs.database.model.impl.BusinessInterface();
    }
    
	/**
	 * Override of the original createBusinessInteraction<br>
	 * Creates a DBBusinessInteraction instead of a BusinessInteraction
	 */
    public IBusinessInteraction createBusinessInteraction() {
        return new org.archicontribs.database.model.impl.BusinessInteraction();
    }
    
	/**
	 * Override of the original createBusinessFunction<br>
	 * Creates a DBBusinessFunction instead of a BusinessFunction
	 */
    public IBusinessFunction createBusinessFunction() {
        return new org.archicontribs.database.model.impl.BusinessFunction();
    }
    
	/**
	 * Override of the original createDataObject<br>
	 * Creates a DBDataObject instead of a DataObject
	 */
    public IDataObject createDataObject() {
        return new org.archicontribs.database.model.impl.DataObject();
    }
    
	/**
	 * Override of the original createCourseOfAction<br>
	 * Creates a DBCourseOfAction instead of a CourseOfAction
	 */
    public ICourseOfAction createCourseOfAction() {
        return new org.archicontribs.database.model.impl.CourseOfAction();
    }
    
	/**
	 * Override of the original createContract<br>
	 * Creates a DBContract instead of a Contract
	 */
    public IContract createContract() {
        return new org.archicontribs.database.model.impl.Contract();
    }
    
	/**
	 * Override of the original createConstraint<br>
	 * Creates a DBConstraint instead of a Constraint
	 */
    public IConstraint createConstraint() {
        return new org.archicontribs.database.model.impl.Constraint();
    }
    
	/**
	 * Override of the original createCompositionRelationship<br>
	 * Creates a DBCompositionRelationship instead of a CompositionRelationship
	 */
    public ICompositionRelationship createCompositionRelationship() {
        return new org.archicontribs.database.model.impl.CompositionRelationship();
    }
    
	/**
	 * Override of the original createCapability<br>
	 * Creates a DBCapability instead of a Capability
	 */
    public ICapability createCapability() {
        return new org.archicontribs.database.model.impl.Capability();
    }
    
	/**
	 * Override of the original createCommunicationNetwork<br>
	 * Creates a DBCommunicationNetwork instead of a CommunicationNetwork
	 */
    public ICommunicationNetwork createCommunicationNetwork() {
        return new org.archicontribs.database.model.impl.CommunicationNetwork();
    }
    
	/**
	 * Override of the original createBusinessService<br>
	 * Creates a DBBusinessService instead of a BusinessService
	 */
    public IBusinessService createBusinessService() {
        return new org.archicontribs.database.model.impl.BusinessService();
    }
    
	/**
	 * Override of the original createDeliverable<br>
	 * Creates a DBDeliverable instead of a Deliverable
	 */
    public IDeliverable createDeliverable() {
        return new org.archicontribs.database.model.impl.Deliverable();
    }
    
	/**
	 * Override of the original createDevice<br>
	 * Creates a DBDevice instead of a Device
	 */
    public IDevice createDevice() {
        return new org.archicontribs.database.model.impl.Device();
    }
    
	/**
	 * Override of the original createDiagramModelArchimateConnection<br>
	 * Creates a DBDiagramModelArchimateConnection instead of a DiagramModelArchimateConnection
	 */
    public IDiagramModelArchimateConnection createDiagramModelArchimateConnection() {
        return new org.archicontribs.database.model.impl.DiagramModelArchimateConnection();
    }
    
	/**
	 * Override of the original createDiagramModelArchimateObject<br>
	 * Creates a DBDiagramModelArchimateObject instead of a DiagramModelArchimateObject
	 */
    public IDiagramModelArchimateObject createDiagramModelArchimateObject() {
        return new org.archicontribs.database.model.impl.DiagramModelArchimateObject();
    }
    
	/**
	 * Override of the original createDiagramModelConnection<br>
	 * Creates a DBDiagramModelArchimateObject instead of a DiagramModelConnection
	 */
    public IDiagramModelConnection createDiagramModelConnection() {
        return new org.archicontribs.database.model.impl.DiagramModelConnection();
    }
    
	/**
	 * Override of the original createDiagramModelGroup<br>
	 * Creates a DBDiagramModelGroup instead of a DiagramModelGroup
	 */
    public IDiagramModelGroup createDiagramModelGroup() {
        return new org.archicontribs.database.model.impl.DiagramModelGroup();
    }
    
	/**
	 * Override of the original createDiagramModelImage<br>
	 * Creates a DBDiagramModelImage instead of a DiagramModelImage
	 */
    public IDiagramModelImage createDiagramModelImage() {
        return new org.archicontribs.database.model.impl.DiagramModelImage();
    }
    
	/**
	 * Override of the original createDiagramModelNote<br>
	 * Creates a DBDiagramModelNote instead of a DiagramModelNote
	 */
    public IDiagramModelNote createDiagramModelNote() {
        return new org.archicontribs.database.model.impl.DiagramModelNote();
    }
    
	/**
	 * Override of the original createDiagramModelObject<br>
	 * Creates a DBDiagramModelObject instead of a DiagramModelObject
	 */
    public IDiagramModelObject createDiagramModelObject() {
        return new org.archicontribs.database.model.impl.DiagramModelObject();
    }
    
	/**
	 * Override of the original createDiagramModelReference<br>
	 * Creates a DBDiagramModelReference instead of a DiagramModelReference
	 */
    public IDiagramModelReference createDiagramModelReference() {
        return new org.archicontribs.database.model.impl.DiagramModelReference();
    }
    
	/**
	 * Override of the original createDistributionNetwork<br>
	 * Creates a DBDistributionNetwork instead of a DistributionNetwork
	 */
    public IDistributionNetwork createDistributionNetwork() {
        return new org.archicontribs.database.model.impl.DistributionNetwork();
    }
    
	/**
	 * Override of the original createDriver<br>
	 * Creates a DBDriver instead of a Driver
	 */
    public IDriver createDriver() {
        return new org.archicontribs.database.model.impl.Driver();
    }
    
	/**
	 * Override of the original createEquipment<br>
	 * Creates a DBEquipment instead of a Equipment
	 */
    public IEquipment createEquipment() {
        return new org.archicontribs.database.model.impl.Equipment();
    }
    
	/**
	 * Override of the original createFacility<br>
	 * Creates a DBFacility instead of a Facility
	 */
    public IFacility createFacility() {
        return new org.archicontribs.database.model.impl.Facility();
    }
    
	/**
	 * Override of the original createFolder<br>
	 * Creates a DBFolder instead of a Folder
	 */
    public IFolder createFolder() {
        return new org.archicontribs.database.model.impl.Folder();
    }
    
	/**
	 * Override of the original createFlowRelationship<br>
	 * Creates a DBFlowRelationship instead of a FlowRelationship
	 */
    public IFlowRelationship createFlowRelationship() {
        return new org.archicontribs.database.model.impl.FlowRelationship();
    }
    
	/**
	 * Override of the original createGap<br>
	 * Creates a DBGap instead of a Gap
	 */
    public IGap createGap() {
        return new org.archicontribs.database.model.impl.Gap();
    }
    
	/**
	 * Override of the original createGoal<br>
	 * Creates a DBGoal instead of a Goal
	 */
    public IGoal createGoal() {
        return new org.archicontribs.database.model.impl.Goal();
    }
    
	/**
	 * Override of the original createGrouping<br>
	 * Creates a DBGrouping instead of a Grouping
	 */
    public IGrouping createGrouping() {
        return new org.archicontribs.database.model.impl.Grouping();
    }
    
	/**
	 * Override of the original createImplementationEvent<br>
	 * Creates a DBImplementationEvent instead of a ImplementationEvent
	 */
    public IImplementationEvent createImplementationEvent() {
        return new org.archicontribs.database.model.impl.ImplementationEvent();
    }
    
	/**
	 * Override of the original createInfluenceRelationship<br>
	 * Creates a DBInfluenceRelationship instead of a InfluenceRelationship
	 */
    public IInfluenceRelationship createInfluenceRelationship() {
        return new org.archicontribs.database.model.impl.InfluenceRelationship();
    }
    
	/**
	 * Override of the original createJunction<br>
	 * Creates a DBJunction instead of a Junction
	 */
    public IJunction createJunction() {
        return new org.archicontribs.database.model.impl.Junction();
    }
    
	/**
	 * Override of the original createLocation<br>
	 * Creates a DBLocation instead of a Location
	 */
    public ILocation createLocation() {
        return new org.archicontribs.database.model.impl.Location();
    }
    
	/**
	 * Override of the original createMaterial<br>
	 * Creates a DBMaterial instead of a Material
	 */
    public IMaterial createMaterial() {
        return new org.archicontribs.database.model.impl.Material();
    }
    
	/**
	 * Override of the original createMeaning<br>
	 * Creates a DBMeaning instead of a Meaning
	 */
    public IMeaning createMeaning() {
        return new org.archicontribs.database.model.impl.Meaning();
    }
    
	/**
	 * Override of the original createOutcome<br>
	 * Creates a DBOutcome instead of a Outcome
	 */
    public IOutcome createOutcome() {
        return new org.archicontribs.database.model.impl.Outcome();
    }
    
	/**
	 * Override of the original createPath<br>
	 * Creates a DBPath instead of a Path
	 */
    public IPath createPath() {
        return new org.archicontribs.database.model.impl.Path();
    }
    
	/**
	 * Override of the original createPlateau<br>
	 * Creates a DBPlateau instead of a Plateau
	 */
    public IPlateau createPlateau() {
        return new org.archicontribs.database.model.impl.Plateau();
    }
    
	/**
	 * Override of the original createPrinciple<br>
	 * Creates a DBPrinciple instead of a Principle
	 */
    public IPrinciple createPrinciple() {
        return new org.archicontribs.database.model.impl.Principle();
    }
    
	/**
	 * Override of the original createProduct<br>
	 * Creates a DBProduct instead of a Product
	 */
    public IProduct createProduct() {
        return new org.archicontribs.database.model.impl.Product();
    }
    
	/**
	 * Override of the original createRealizationRelationship<br>
	 * Creates a DBRealizationRelationship instead of a RealizationRelationship
	 */
    public IRealizationRelationship createRealizationRelationship() {
        return new org.archicontribs.database.model.impl.RealizationRelationship();
    }
    
	/**
	 * Override of the original createRepresentation<br>
	 * Creates a DBRepresentation instead of a Representation
	 */
    public IRepresentation createRepresentation() {
        return new org.archicontribs.database.model.impl.Representation();
    }
    
	/**
	 * Override of the original createRequirement<br>
	 * Creates a DBRequirement instead of a Requirement
	 */
    public IRequirement createRequirement() {
        return new org.archicontribs.database.model.impl.Requirement();
    }
    
	/**
	 * Override of the original createResource<br>
	 * Creates a DBResource instead of a Resource
	 */
    public IResource createResource() {
        return new org.archicontribs.database.model.impl.Resource();
    }
    
	/**
	 * Override of the original createServingRelationship<br>
	 * Creates a DBServingRelationship instead of a ServingRelationship
	 */
    public IServingRelationship createServingRelationship() {
        return new org.archicontribs.database.model.impl.ServingRelationship();
    }
    
	/**
	 * Override of the original createSketchModel<br>
	 * Creates a DBSketchModel instead of a SketchModel
	 */
    public ISketchModel createSketchModel() {
        return new org.archicontribs.database.model.impl.SketchModel();
    }
    
	/**
	 * Override of the original createSketchModelActor<br>
	 * Creates a DBSketchModelActor instead of a SketchModelActor
	 */
    public ISketchModelActor createSketchModelActor() {
        return new org.archicontribs.database.model.impl.SketchModelActor();
    }
    
	/**
	 * Override of the original createSketchModelSticky<br>
	 * Creates a DBSketchModelSticky instead of a SketchModelSticky
	 */
    public ISketchModelSticky createSketchModelSticky() {
        return new org.archicontribs.database.model.impl.SketchModelSticky();
    }
    
	/**
	 * Override of the original createSpecializationRelationship<br>
	 * Creates a DBSpecializationRelationship instead of a SpecializationRelationship
	 */
    public ISpecializationRelationship createSpecializationRelationship() {
        return new org.archicontribs.database.model.impl.SpecializationRelationship();
    }
    
    
	/**
	 * Override of the original createStakeholder<br>
	 * Creates a DBStakeholder instead of a Stakeholder
	 */
    public IStakeholder createStakeholder() {
        return new org.archicontribs.database.model.impl.Stakeholder();
    }
    
	/**
	 * Override of the original createSystemSoftware<br>
	 * Creates a DBSystemSoftware instead of a SystemSoftware
	 */
    public ISystemSoftware createSystemSoftware() {
        return new org.archicontribs.database.model.impl.SystemSoftware();
    }
    
	/**
	 * Override of the original createTechnologyCollaboration<br>
	 * Creates a DBTechnologyCollaboration instead of a TechnologyCollaboration
	 */
    public ITechnologyCollaboration createTechnologyCollaboration() {
        return new org.archicontribs.database.model.impl.TechnologyCollaboration();
    }
    
	/**
	 * Override of the original createTechnologyEvent<br>
	 * Creates a DBTechnologyEvent instead of a TechnologyEvent
	 */
    public ITechnologyEvent createTechnologyEvent() {
        return new org.archicontribs.database.model.impl.TechnologyEvent();
    }
    
	/**
	 * Override of the original createTechnologyFunction<br>
	 * Creates a DBTechnologyFunction instead of a TechnologyFunction
	 */
    public ITechnologyFunction createTechnologyFunction() {
        return new org.archicontribs.database.model.impl.TechnologyFunction();
    }
    
	/**
	 * Override of the original createTechnologyInteraction<br>
	 * Creates a DBTechnologyInteraction instead of a TechnologyInteraction
	 */
    public ITechnologyInteraction createTechnologyInteraction() {
        return new org.archicontribs.database.model.impl.TechnologyInteraction();
    }
    
	/**
	 * Override of the original createTechnologyInterface<br>
	 * Creates a DBTechnologyInterface instead of a TechnologyInterface
	 */
    public ITechnologyInterface createTechnologyInterface() {
        return new org.archicontribs.database.model.impl.TechnologyInterface();
    }
    
	/**
	 * Override of the original createTechnologyProcess<br>
	 * Creates a DBTechnologyProcess instead of a TechnologyProcess
	 */
    public ITechnologyProcess createTechnologyProcess() {
        return new org.archicontribs.database.model.impl.TechnologyProcess();
    }
    
	/**
	 * Override of the original createTechnologyService<br>
	 * Creates a DBTechnologyService instead of a TechnologyService
	 */
    public ITechnologyService createTechnologyService() {
        return new org.archicontribs.database.model.impl.TechnologyService();
    }
    
	/**
	 * Override of the original createTriggeringRelationship<br>
	 * Creates a DBTriggeringRelationship instead of a TriggeringRelationship
	 */
    public ITriggeringRelationship createTriggeringRelationship() {
        return new org.archicontribs.database.model.impl.TriggeringRelationship();
    }
    
	/**
	 * Override of the original createValue<br>
	 * Creates a DBValue instead of a Value
	 */
    public IValue createValue() {
        return new org.archicontribs.database.model.impl.Value();
    }
    
	/**
	 * Override of the original createWorkPackage<br>
	 * Creates a DBWorkPackage instead of a WorkPackage
	 */
    public IWorkPackage createWorkPackage() {
        return new org.archicontribs.database.model.impl.WorkPackage();
    }
}
