/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.connection.DBDatabaseConnection;
import org.archicontribs.database.connection.DBDatabaseImportConnection;
import org.archicontribs.database.data.DBBendpoint;
import org.archicontribs.database.data.DBDatabase;
import org.archicontribs.database.data.DBProperty;
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.archimatetool.model.IIconic;
import com.archimatetool.canvas.model.INotesContent;
import com.archimatetool.editor.diagram.util.DiagramUtils;
import com.archimatetool.editor.diagram.util.ModelReferencedImage;
import com.archimatetool.editor.ui.ImageFactory;
import com.archimatetool.model.FolderType;
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
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFontAttribute;
import com.archimatetool.model.IInfluenceRelationship;
import com.archimatetool.model.IJunction;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.ISketchModel;
import com.archimatetool.model.ITextAlignment;
import com.archimatetool.model.ITextContent;
import com.archimatetool.model.ITextPosition;

import lombok.Getter;
import lombok.Setter;

/**
 * This class manages the GUI of the plugin.
 * 
 * @author Herve Jouin
 */
public class DBGui {
    protected static final DBLogger logger = new DBLogger(DBGui.class);

    @Getter @Setter private boolean closedByUser = false;

    protected List<DBDatabaseEntry> databaseEntries;
    protected List<DBDatabaseEntry> comboDatabaseEntries;
    protected DBDatabaseEntry selectedDatabase;
    DBDatabaseImportConnection connection;

    protected static final Display display = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
    protected Shell dialog;
    protected Shell parentDialog;

    protected boolean includeNeo4j = true;

    String HELP_HREF = null;
    boolean mouseOverHelpButton = false;

    protected enum ACTION {One, Two, Three, Four}
    protected enum STATUS {Empty, Selected, Running, Bypassed, Ok, Warn, Error}

    public static final Color LIGHT_GREEN_COLOR = new Color(display, 204, 255, 229);
    public static final Color LIGHT_RED_COLOR = new Color(display, 255, 230, 230);
    public static final Color RED_COLOR = new Color(display, 240, 0, 0);
    public static final Color GREEN_COLOR = new Color(display, 0, 180, 0);
    public static final Color WHITE_COLOR = new Color(display, 255, 255, 255);
    public static final Color GREY_COLOR = new Color(display, 100, 100, 100);
    public static final Color BLACK_COLOR = new Color(display, 0, 0, 0);
    public static final Color YELLOW_COLOR = new Color(display, 255, 255, 0);

    public static final Color COMPO_LEFT_COLOR = new Color(display, 240, 248, 255);			// light blue
    public static final Color COMPO_BACKGROUND_COLOR = new Color(display, 250, 250, 250);	// light grey
    public static final Color GROUP_BACKGROUND_COLOR = new Color(display, 235, 235, 235);	// light grey (a bit darker than compo background)
    public static final Color TABLE_BACKGROUND_COLOR = new Color(display, 225, 225, 225);	// light grey (a bit darker than group background)
    public static final Color HIGHLIGHTED_COLOR = display.getSystemColor(SWT.COLOR_GRAY);

    public static final Color STRATEGY_COLOR = new Color(display, 255, 222, 170);
    public static final Color BUSINESS_COLOR = new Color(display, 255, 255, 181);
    public static final Color APPLICATION_COLOR = new Color(display, 181, 255, 255);
    public static final Color TECHNOLOGY_COLOR = new Color(display, 201, 231, 183);
    public static final Color PHYSICAL_COLOR = new Color(display, 201, 231, 183);
    public static final Color IMPLEMENTATION_COLOR = new Color(display, 255, 224, 224);
    public static final Color MOTIVATION_COLOR = new Color(display, 204, 204, 255);

    public static final Color PASSIVE_COLOR = new Color(display, 250, 250, 250);

    public static final Cursor CURSOR_WAIT = new Cursor(null, SWT.CURSOR_WAIT);
    public static final Cursor CURSOR_ARROW = new Cursor(null, SWT.CURSOR_ARROW);

    public static final FontData SYSTEM_FONT = display.getSystemFont().getFontData()[0];
    public static final Font GROUP_TITLE_FONT = new Font(display, SYSTEM_FONT.getName(), SYSTEM_FONT.getHeight()+2, SWT.BOLD | SWT.ITALIC);
    public static final Font TITLE_FONT = new Font(display, SYSTEM_FONT.getName(), SYSTEM_FONT.getHeight()+2, SWT.BOLD);
    public static final Font BOLD_FONT = new Font(display, SYSTEM_FONT.getName(), SYSTEM_FONT.getHeight(), SWT.BOLD);

    public static final Image LOGO_IMAGE = new Image(display, DBGui.class.getResourceAsStream("/img/logo.png"));
    public static final Image EXPORT_TO_DATABASE_IMAGE = new Image(display, DBGui.class.getResourceAsStream("/img/22x22/export.png"));
    public static final Image IMPORT_FROM_DATABASE_IMAGE = new Image(display, DBGui.class.getResourceAsStream("/img/22x22/import.png"));

    public static final Image BYPASSED_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/bypassed.png"));
    public static final Image CLOCK_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/clock.png"));
    public static final Image ERROR_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/error.png"));
    public static final Image WARNING_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/warning.png"));
    public static final Image OK_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/ok.png"));
    public static final Image RIGHT_ARROW_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/right_arrow.png"));

    public static final Image LOCK_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/lock.png"));
    public static final Image UNLOCK_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/10x10/unlock.png"));

    public static final Image HELP_ICON = new Image(display, DBGui.class.getResourceAsStream("/img/28x28/help.png"));

    private Composite compoLeft;
    protected Composite compoRight;
    protected Composite compoRightTop;
    protected Composite compoRightBottom;
    private Composite compoBottom;

    private Label imgFirstAction;
    private Label imgSecondAction;
    private Label imgThirdAction;
    private Label imgFourthAction;

    private Label lblFirstAction;
    private Label lblSecondAction;
    private Label lblThirdAction;
    private Label lblFourthAction;

    private Label lblOption;
    Button radioOption1;
    Button radioOption2;
    Button radioOption3;

    @Getter protected Combo comboDatabases;
    protected Button btnSetPreferences;
    protected Button btnClose;
    protected Button btnDoAction;
    protected Label btnHelp;

    private Group grpDatabase;

    protected Group grpProgressBar = null;
    protected Label lblProgressBar;
    private ProgressBar progressBar;

    protected Group grpMessage = null;
    private CLabel lblMessage;

    /** Default height of a Label widget */
    @Getter private int defaultLabelHeight;

    /** Default margin between widgets */
    @Getter private int defaultMargin = 10;


    /**
     * Create the dialog with minimal graphical objects: 
     * 		left composite: picture of a database with Archimate diagram inside, the plugin version, (my name of course) and 4 icons + texts to list actions 
     * 		bottom composite: Close, doAction button at the right and help buton on the left
     * 		right composite: database list in a combo and a button to set preferences
     */
    protected DBGui(String title) {
        if ( logger.isDebugEnabled() ) logger.debug("Creating Form GUI.");

        setArrowCursor();

        this.parentDialog = display.getActiveShell();
        this.dialog = new Shell(display, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.RESIZE);
        this.dialog.setText(DBPlugin.pluginTitle + " - " + title);
        this.dialog.setMinimumSize(800, 700);
        this.dialog.setSize(1024, 700);
        
        int scaleFactor = 1;
        try {
        	if ( (Toolkit.getDefaultToolkit().getScreenResolution() != 0) && (this.dialog.getDisplay().getDPI() != null) && (this.dialog.getDisplay().getDPI().x != 0) )
        		scaleFactor = Toolkit.getDefaultToolkit().getScreenResolution() / this.dialog.getDisplay().getDPI().x;
        } catch ( @SuppressWarnings("unused") HeadlessException ign) {
        	// nothing to do
        }
        if ( scaleFactor == 0 )
        	scaleFactor = 1;		// just in case

        // Use the active shell, if available, to determine the new shell placing
        int locationX = 0;
        int locationY = 0;
        Rectangle shellSize = this.dialog.getBounds();
        if (this.parentDialog!=null) { 
	        Rectangle parentSize = this.parentDialog.getBounds();
	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
        } else {
	        locationX = ((Toolkit.getDefaultToolkit().getScreenSize().width / scaleFactor) - this.dialog.getSize().x) / 2;
	        //locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - this.dialog.getSize().x) / 2;
	        locationY = ((Toolkit.getDefaultToolkit().getScreenSize().height / scaleFactor) - this.dialog.getSize().y) / 2;
	        //locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - this.dialog.getSize().y) / 2;
        }
        this.dialog.setLocation(new Point(locationX, locationY));
		        
        this.dialog.setLayout(new FormLayout());

        /**
         * Calculate the default height of a Label widget
         */
        Label label = new Label(this.dialog, SWT.NONE);
        label.setText("Test");
        this.defaultLabelHeight = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        label.dispose();

        this.dialog.addListener(SWT.Close, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                boolean doIt = true;
                if ( DBGui.this.btnClose.getText().equals("Cancel") ) {
                    doIt = question("Are you sure you wish to cancel ?");
                }
                
                if ( doIt ) {
                    setClosedByUser(true);
                    try {
                        rollbackAndCloseConnection();
                    } catch (SQLException e) {
                        popup(Level.ERROR, "Failed to rollback and close the database connection.", e);
                    }
                    close();
                    event.doit = true;
                }
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////// compoLeft ////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        this.compoLeft = new Composite(this.dialog, SWT.BORDER);
        this.compoLeft.setBackground(COMPO_LEFT_COLOR);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(0, 160);
        fd.bottom = new FormAttachment(100, -40);
        this.compoLeft.setLayoutData(fd);
        this.compoLeft.setLayout(new FormLayout());

        Composite compoTitle = new Composite(this.compoLeft, SWT.BORDER);
        compoTitle.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData(140,50);
        fd.top = new FormAttachment(0, 40);
        fd.left = new FormAttachment(5);
        fd.right = new FormAttachment(100, -5);
        compoTitle.setLayoutData(fd);
        compoTitle.setLayout(new FormLayout());

        Label lblTitle = new Label(compoTitle, SWT.CENTER);
        lblTitle.setBackground(COMPO_LEFT_COLOR);
        lblTitle.setText("Archi database plugin");
        lblTitle.setFont(TITLE_FONT);
        fd = new FormData();
        fd.top = new FormAttachment(10);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        lblTitle.setLayoutData(fd);

        Label lblPluginVersion = new Label(compoTitle, SWT.CENTER);
        lblPluginVersion.setBackground(COMPO_LEFT_COLOR);
        lblPluginVersion.setText(DBPlugin.pluginVersion.toString());
        fd = new FormData();
        fd.top = new FormAttachment(lblTitle, 5);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        lblPluginVersion.setLayoutData(fd);

        Label imgDatabase = new Label(this.compoLeft, SWT.CENTER);
        imgDatabase.setBackground(COMPO_LEFT_COLOR);
        imgDatabase.setImage(LOGO_IMAGE);
        fd = new FormData(135,115);
        fd.top = new FormAttachment(compoTitle, 30);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        imgDatabase.setLayoutData(fd);

        this.imgFirstAction = new Label(this.compoLeft, SWT.CENTER);
        this.imgFirstAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData(10,10);
        fd.top = new FormAttachment(imgDatabase, 50);
        fd.left = new FormAttachment(0, 10);
        this.imgFirstAction.setLayoutData(fd);

        this.lblFirstAction = new Label(this.compoLeft, SWT.NONE);
        this.lblFirstAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.imgFirstAction, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.imgFirstAction, 10);
        fd.right = new FormAttachment(100, -10);
        this.lblFirstAction.setLayoutData(fd);

        this.imgSecondAction = new Label(this.compoLeft, SWT.CENTER);
        this.imgSecondAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData(10,10);
        fd.top = new FormAttachment(this.imgFirstAction, 10);
        fd.left = new FormAttachment(0, 10);
        this.imgSecondAction.setLayoutData(fd);

        this.lblSecondAction = new Label(this.compoLeft, SWT.NONE);
        this.lblSecondAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.imgSecondAction, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.imgSecondAction, 10);
        fd.right = new FormAttachment(100, -10);
        this.lblSecondAction.setLayoutData(fd);

        this.imgThirdAction = new Label(this.compoLeft, SWT.CENTER);
        this.imgThirdAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData(10,10);
        fd.top = new FormAttachment(this.imgSecondAction, 10);
        fd.left = new FormAttachment(0, 10);
        this.imgThirdAction.setLayoutData(fd);

        this.lblThirdAction = new Label(this.compoLeft, SWT.NONE);
        this.lblThirdAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.imgThirdAction, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.imgThirdAction, 10);
        fd.right = new FormAttachment(100, -10);
        this.lblThirdAction.setLayoutData(fd);

        this.imgFourthAction = new Label(this.compoLeft, SWT.CENTER);
        this.imgFourthAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData(10,10);
        fd.top = new FormAttachment(this.imgThirdAction, 10);
        fd.left = new FormAttachment(0, 10);
        this.imgFourthAction.setLayoutData(fd);

        this.lblFourthAction = new Label(this.compoLeft, SWT.NONE);
        this.lblFourthAction.setBackground(COMPO_LEFT_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(this.imgFourthAction, 0, SWT.CENTER);
        fd.left = new FormAttachment(this.imgFourthAction, 10);
        fd.right = new FormAttachment(100, -10);
        this.lblFourthAction.setLayoutData(fd);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////// compoRight ///////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        this.compoRight = new Composite(this.dialog, SWT.BORDER);
        this.compoRight.setBackground(COMPO_BACKGROUND_COLOR);
        FormData fd_compoRight = new FormData();
        fd_compoRight.top = new FormAttachment(0);
        fd_compoRight.bottom = new FormAttachment(100, -40);
        fd_compoRight.left = new FormAttachment(this.compoLeft);
        fd_compoRight.right = new FormAttachment(100);
        this.compoRight.setLayoutData(fd_compoRight);
        this.compoRight.setLayout(new FormLayout());

        this.compoRightTop = new Composite(this.compoRight, SWT.NONE);
        this.compoRightTop.setBackground(COMPO_BACKGROUND_COLOR);
        FormData fd_compoRightUp = new FormData();
        fd_compoRightUp.top = new FormAttachment(0, 10);
        fd_compoRightUp.bottom = new FormAttachment(0, 70);
        fd_compoRightUp.left = new FormAttachment(0, 10);
        fd_compoRightUp.right = new FormAttachment(100, -10);
        this.compoRightTop.setLayoutData(fd_compoRightUp);
        this.compoRightTop.setLayout(new FormLayout());

        this.compoRightBottom = new Composite(this.compoRight, SWT.NONE);
        this.compoRightBottom.setBackground(COMPO_BACKGROUND_COLOR);
        FormData fd_compoRightBottom = new FormData();
        fd_compoRightBottom.top = new FormAttachment(this.compoRightTop, 10);
        fd_compoRightBottom.bottom = new FormAttachment(100, -10);
        fd_compoRightBottom.left = new FormAttachment(0, 10);
        fd_compoRightBottom.right = new FormAttachment(100, -10);
        this.compoRightBottom.setLayoutData(fd_compoRightBottom);
        this.compoRightBottom.setLayout(new FormLayout());

        this.grpDatabase = new Group(this.compoRightTop, SWT.SHADOW_ETCHED_IN);
        this.grpDatabase.setVisible(true);
        this.grpDatabase.setData("visible", true);
        this.grpDatabase.setBackground(GROUP_BACKGROUND_COLOR);
        this.grpDatabase.setFont(GROUP_TITLE_FONT);
        this.grpDatabase.setText("Database: ");
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.grpDatabase.setLayoutData(fd);
        this.grpDatabase.setLayout(new FormLayout());

        Label lblRegisteredDatabases = new Label(this.grpDatabase, SWT.NONE);
        lblRegisteredDatabases.setBackground(GROUP_BACKGROUND_COLOR);
        lblRegisteredDatabases.setText("Registered databases:");
        fd = new FormData();
        fd.top = new FormAttachment(25);
        fd.left = new FormAttachment(0, 10);
        lblRegisteredDatabases.setLayoutData(fd);

        this.btnSetPreferences = new Button(this.grpDatabase, SWT.NONE);
        this.btnSetPreferences.setText("Set preferences ...");
        this.btnSetPreferences.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent event) { try { setPreferences(); } catch (Exception e) { popup(Level.ERROR, "Failed to set preferences", e); } }
            @Override
            public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblRegisteredDatabases, 0, SWT.CENTER);
        fd.right = new FormAttachment(100, -10);
        this.btnSetPreferences.setLayoutData(fd);

        this.comboDatabases = new Combo(this.grpDatabase, SWT.NONE | SWT.READ_ONLY);
        this.comboDatabases.setBackground(GROUP_BACKGROUND_COLOR);
        this.comboDatabases.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent event) { databaseSelected(); }
            @Override
            public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
        });
        fd = new FormData();
        fd.top = new FormAttachment(lblRegisteredDatabases, 0, SWT.CENTER);
        fd.left = new FormAttachment(lblRegisteredDatabases, 10);
        fd.right = new FormAttachment(this.btnSetPreferences, -40);
        this.comboDatabases.setLayoutData(fd);



        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////// compoBottom //////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        this.compoBottom = new Composite(this.dialog, SWT.NONE);
        this.compoBottom.setBackground(COMPO_BACKGROUND_COLOR);
        fd = new FormData();
        fd.top = new FormAttachment(100, -40);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.compoBottom.setLayoutData(fd);
        this.compoBottom.setLayout(new FormLayout());

        this.btnHelp = new Label(this.compoBottom, SWT.NONE);
        this.btnHelp.setVisible(false);
        this.btnHelp.addListener(SWT.MouseEnter, new Listener() { @Override public void handleEvent(Event event) { DBGui.this.mouseOverHelpButton = true; DBGui.this.btnHelp.redraw(); } });
        this.btnHelp.addListener(SWT.MouseExit, new Listener() { @Override public void handleEvent(Event event) { DBGui.this.mouseOverHelpButton = false; DBGui.this.btnHelp.redraw(); } });
        this.btnHelp.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent event)
            {
                if ( DBGui.this.mouseOverHelpButton ) event.gc.drawRoundRectangle(0, 0, 29, 29, 10, 10);
                event.gc.drawImage(HELP_ICON, 2, 2);
            }
        });
        this.btnHelp.addListener(SWT.MouseUp, new Listener() { @Override public void handleEvent(Event event) { if ( DBGui.this.HELP_HREF != null ) { if ( logger.isDebugEnabled() ) logger.debug("Showing help: /"+DBPlugin.PLUGIN_ID+"/help/html/"+DBGui.this.HELP_HREF); PlatformUI.getWorkbench().getHelpSystem().displayHelpResource("/"+DBPlugin.PLUGIN_ID+"/help/html/"+DBGui.this.HELP_HREF); } } });
        fd = new FormData(30,30);
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(0, 5);
        this.btnHelp.setLayoutData(fd);


        this.btnClose = new Button(this.compoBottom, SWT.NONE);
        this.btnClose.setText("Close");
        this.btnClose.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean doIt = true;
                if ( DBGui.this.btnClose.getText().equals("Cancel") ) {
                    doIt = question("Are you sure you wish to cancel ?");
                }
                
                if ( doIt ) {
                    setClosedByUser(true);
                    try {
                        rollbackAndCloseConnection();
                    } catch (SQLException e) {
                        popup(Level.ERROR, "Failed to rollback and close the database connection.", e);
                    }
                    close();
                    event.doit = true;
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
        });
        fd = new FormData(100,25);
        fd.top = new FormAttachment(0, 8);
        fd.right = new FormAttachment(100, -10);
        this.btnClose.setLayoutData(fd);

        this.btnDoAction = new Button(this.compoBottom, SWT.NONE);
        this.btnDoAction.setEnabled(false);
        this.btnDoAction.setVisible(false);
        fd = new FormData(100,25);
        fd.top = new FormAttachment(0, 8);
        fd.right = new FormAttachment(this.btnClose, -10);
        this.btnDoAction.setLayoutData(fd);

        this.radioOption3 = new Button(this.compoBottom, SWT.RADIO);
        this.radioOption3.setBackground(COMPO_BACKGROUND_COLOR);
        this.radioOption3.setVisible(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.btnDoAction, 0, SWT.CENTER);
        fd.right = new FormAttachment(this.btnDoAction, -20);
        this.radioOption3.setLayoutData(fd);

        this.radioOption2 = new Button(this.compoBottom, SWT.RADIO);
        this.radioOption2.setBackground(COMPO_BACKGROUND_COLOR);
        this.radioOption2.setVisible(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.btnDoAction, 0, SWT.CENTER);
        fd.right = new FormAttachment(this.radioOption3, -10);
        this.radioOption2.setLayoutData(fd);

        this.radioOption1 = new Button(this.compoBottom, SWT.RADIO);
        this.radioOption1.setBackground(COMPO_BACKGROUND_COLOR);
        this.radioOption1.setVisible(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.btnDoAction, 0, SWT.CENTER);
        fd.right = new FormAttachment(this.radioOption2, -10);
        this.radioOption1.setLayoutData(fd);

        this.lblOption = new Label(this.compoBottom, SWT.NONE);
        this.lblOption.setBackground(COMPO_BACKGROUND_COLOR);
        this.lblOption.setVisible(false);
        fd = new FormData();
        fd.top = new FormAttachment(this.btnDoAction, 0, SWT.CENTER);
        fd.right = new FormAttachment(this.radioOption1, -10);
        this.lblOption.setLayoutData(fd);
    }

    public void run() {
        this.dialog.open();
        this.dialog.layout();
        refreshDisplay();
    }
    
    /**
     * Gets the list of configured databases, fill-in the comboDatabases and select the database provided
     * @param mustIncludeNeo4j if true, include the Neo4J databases in the list, if false, do not include them in the list
     * @param defaultDatabaseId Indicated the ID of the default database (the first database will be selected, if the database is not found or if null)
     * @param defaultDatabaseName Indicated the name of the default database (the first database will be selected, if the database is not found or if null) - if both ID and name are provided, the ID has got higher priority
     * @throws Exception 
     */
    protected void getDatabases(boolean mustIncludeNeo4j, String defaultDatabaseId, String defaultDatabaseName) throws Exception {
        refreshDisplay();

        this.databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore();
        this.comboDatabaseEntries = new ArrayList<DBDatabaseEntry>();
        if ( (this.databaseEntries == null) || (this.databaseEntries.size() == 0) ) {
            popup(Level.ERROR, "You haven't configure any database yet.\n\nPlease setup at least one database in Archi preferences.");
        } else {
        	int databaseToSelect = -1;
        	int line = 0;
            for (DBDatabaseEntry databaseEntry: this.databaseEntries) {
            	if ( mustIncludeNeo4j || !databaseEntry.getDriver().equals(DBDatabase.NEO4J.getDriverName()) ) {
            		this.comboDatabases.add(databaseEntry.getName());
            		this.comboDatabaseEntries.add(databaseEntry);
            		if ( defaultDatabaseId != null && databaseEntry.getId().equals(defaultDatabaseId) )
            			databaseToSelect = line;
            		if ( defaultDatabaseName != null && databaseToSelect != 0 && databaseEntry.getName().equals(defaultDatabaseName) )
            			databaseToSelect = line;
            		++line;
            	}
            }
            if ( line == 0 ) 
            	popup(Level.ERROR, "You haven't configure any SQL database yet.\n\nPlease setup at least one SQL database in Archi preferences.");
            else {
            	// if no default database is provided, then we select the first database in the combo
            	if ( defaultDatabaseId == null && defaultDatabaseName == null )
            		databaseToSelect = 0;
            	if ( databaseToSelect != -1 ) {
            		this.comboDatabases.select(databaseToSelect);
            		this.comboDatabases.notifyListeners(SWT.Selection, new Event());		// calls the databaseSelected() method
            	}
            }
        }
    }

    /**
     * Gets the list of configured databases, fill-in the comboDatabases and select the first-one
     * @param mustIncludeNeo4j if true, include the Neo4J databases in the list, if false, do not include them in the list
     * @throws Exception 
     */
    protected void getDatabases(boolean mustIncludeNeo4j) throws Exception {
        getDatabases(mustIncludeNeo4j, null, null);
    }

    /**
     * Called when the user clicks on the "set preferences" button<br>
     * This method opens up the database plugin preference page that the user can configure database details.
     * @throws Exception 
     */
    protected void setPreferences() throws Exception {
        if ( logger.isDebugEnabled() ) logger.debug("Openning preference page ...");
        PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "org.archicontribs.database.DBPreferencePage", null, null);
        prefDialog.setBlockOnOpen(true);
        if ( prefDialog.open() == 0 ) {
            if ( logger.isDebugEnabled() ) logger.debug("Resetting settings from preferences ...");

            this.comboDatabases.removeAll();

            this.databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore();
            if ( (this.databaseEntries == null) || (this.databaseEntries.size() == 0) ) {
                this.comboDatabases.select(0);
                popup(Level.ERROR, "You haven't configure any database yet.\n\nPlease setup at least one database in Archi preferences.");
            } else {
            	int line = 0;
                for (DBDatabaseEntry databaseEntry: this.databaseEntries) {
                	if ( this.includeNeo4j || !databaseEntry.getDriver().equals(DBDatabase.NEO4J.getDriverName()) ) {
                		this.comboDatabases.add(databaseEntry.getName());
                		this.comboDatabaseEntries.add(databaseEntry);
                		++line;
                	}
                }
                if ( line == 0 ) 
                	popup(Level.ERROR, "You haven't configure any SQL database yet.\n\nPlease setup at least one SQL database in Archi preferences.");
                else {
                	this.comboDatabases.select(0);
                	this.comboDatabases.notifyListeners(SWT.Selection, new Event());
                }
            }
        } else {
            if ( logger.isDebugEnabled() ) logger.debug("Preferences cancelled ...");
            if ( this.comboDatabases.getItemCount() == 0 )
                popup(Level.ERROR, "You won't be able to export until a database is configured in the preferences.");
        }
        this.comboDatabases.setFocus();
    }

    /**
     * Listener called when a database is selected in the comboDatabases<br>
     * This method retrieve the database name from the comboDatabases and reads the preferences to get the connection details. A connection is then established to the database.
     */
    protected void databaseSelected() {
        setMessage("Connecting to the database ...");
        refreshDisplay();

        databaseSelectedCleanup();

        this.btnDoAction.setEnabled(false);

        // we get the databaseEntry corresponding to the selected combo entry
        this.selectedDatabase = this.comboDatabaseEntries.get(this.comboDatabases.getSelectionIndex());
        if ( logger.isDebugEnabled() ) logger.debug("Selected database = " + this.selectedDatabase.getName()+" ("+this.selectedDatabase.getDriver()+", "+this.selectedDatabase.getServer()+", "+this.selectedDatabase.getPort()+", "+this.selectedDatabase.getDatabase()+", "+this.selectedDatabase.getUsername());

        // then we connect to the database.
        try {
            this.connection = new DBDatabaseImportConnection(this.selectedDatabase);
            //if the database connection failed, then an exception is raised, meaning that we get here only if the database connection succeeded
            if ( logger.isDebugEnabled() ) logger.debug(DBGui.class, "We are connected to the database.");
        } catch (Exception err) {
            closeMessage();
            notConnectedToDatabase();
            popup(Level.ERROR, "Cannot connect to the database.", err);
            return;
        }

        // then, we check if the database has got the right pre-requisites
        try {
            this.connection.checkDatabase(null);
        } catch (Exception err) {
            closeMessage();
            popup(Level.ERROR, "Cannot use this database.", err);
            notConnectedToDatabase();
            return;
        } finally {
            closeMessage();
        }

        connectedToDatabase(true);
    }

    /** 
     * This method is called by the databaseSelected method. It allows to do some actions when a new database is selected. 
     */
    protected void databaseSelectedCleanup() {
        //to be overridden
    }

    /** 
     * This method is called by the databaseSelected method. It allows to do some actions when the connection to a new database is successful.
     * @param forceCheckDatabase true when the database should be checked. 
     */
    protected void connectedToDatabase(boolean forceCheckDatabase) {
        // to be overridden
        enableOption();
        this.btnDoAction.setEnabled(true);
    }

    /** 
     * This method is called by the databaseSelected method. It allows to do some actions when the connection to a new database is failed.
     */
    protected void notConnectedToDatabase() {
        // to be overridden
        disableOption();
        this.btnDoAction.setEnabled(false);
    }

    /** 
     * Sets the reference of the online help
     */
    protected void setHelpHref(String href) {
        this.HELP_HREF = href;
        this.btnHelp.setVisible(this.HELP_HREF != null);
    }

    private ACTION activeAction = null;
    /**
     * Activate an action (on the left handside panel)
     * @param action Reference of the action
     * @param status status of the action
     */
    protected void setActiveAction(ACTION action, STATUS status) {
        Image icon;
        switch ( status ) {
            case Selected: icon = RIGHT_ARROW_ICON; break;
            case Running: icon = CLOCK_ICON; break;
            case Bypassed: icon = BYPASSED_ICON; break;
            case Ok: icon = OK_ICON; break;
            case Warn: icon = WARNING_ICON; break;
            case Error: icon = ERROR_ICON; break;
            case Empty: icon = null; break;
            default: icon = null;
        }
        switch ( action ) {
            case One: this.activeAction = ACTION.One; this.imgFirstAction.setImage(icon); break;
            case Two: this.activeAction = ACTION.Two; this.imgSecondAction.setImage(icon); break;
            case Three: this.activeAction = ACTION.Three; this.imgThirdAction.setImage(icon); break;
            case Four: this.activeAction = ACTION.Four; this.imgFourthAction.setImage(icon); break;
            default:
        }
    }

    /**
     * Changes the status of the current action (on the left handside panel)
     * @param status status of the action
     */
    protected void setActiveAction(STATUS status) {
        if ( this.activeAction == null )
            this.activeAction = ACTION.One;
        setActiveAction(this.activeAction, status);
    }

    /**
     * Changes the active action (on the left handside panel)
     * @param action Reference of the action
     */
    protected void setActiveAction(ACTION action) {
        setActiveAction(action, STATUS.Selected);
    }

    /**
     * Creates an action (on the left handside panel)
     * @param action Reference of the action
     * @param label Label of the action
     */
    protected void createAction(ACTION action, String label) {
        switch ( action ) {
            case One: this.lblFirstAction.setText(label); break;
            case Two: this.lblSecondAction.setText(label); break;
            case Three: this.lblThirdAction.setText(label); break;
            case Four: this.lblFourthAction.setText(label); break;
            default:
        }
    }

    protected void setOption(int selectedOption) {
        int realSelectedOption = selectedOption % 3;
        this.radioOption1.setSelection(realSelectedOption == 0);
        this.radioOption2.setSelection(realSelectedOption == 1);
        this.radioOption3.setSelection(realSelectedOption == 2);
    }

    protected void setOption(String label, String option1, boolean option1Selected, String toolTip1, String option2, boolean option2Selected, String toolTip2, String option3, boolean option3Selected, String toolTip3) {
        if ( label != null ) this.lblOption.setText(label);

        if ( option1 == null ) {
            this.radioOption1.setText("");
            this.radioOption1.setToolTipText("");
            this.radioOption1.setVisible(false);
        } else {
            this.radioOption1.setText(option1);
            this.radioOption1.setSelection(option1Selected);
            this.radioOption1.setVisible(true);
            if ( toolTip1 != null ) this.radioOption1.setToolTipText(toolTip1);
        }

        if ( option2 == null ) {
            this.radioOption2.setText("");
            this.radioOption2.setToolTipText("");
            this.radioOption2.setVisible(true);
        } else {
            this.radioOption2.setText(option2);
            this.radioOption2.setSelection(option2Selected);
            this.radioOption2.setVisible(true);
            if ( toolTip2 != null ) this.radioOption2.setToolTipText(toolTip2);
        }

        if ( option3 == null ) {
            this.radioOption3.setText("");
            this.radioOption3.setToolTipText("");
            this.radioOption3.setVisible(false);
        } else {
            this.radioOption3.setText(option3);
            this.radioOption3.setSelection(option3Selected);
            this.radioOption3.setVisible(true);
            if ( toolTip3 != null ) this.radioOption3.setToolTipText(toolTip3);
        }

        this.compoBottom.layout();

        showOption();
        disableOption();
    }

    protected void enableOption() {
        this.lblOption.setEnabled(true);
        this.radioOption1.setEnabled(true);
        this.radioOption2.setEnabled(true);
        this.radioOption3.setEnabled(true);
    }

    protected void disableOption() {
        this.lblOption.setEnabled(false);
        this.radioOption1.setEnabled(false);
        this.radioOption2.setEnabled(false);
        this.radioOption3.setEnabled(false);
    }

    protected void hideOption() {
        this.lblOption.setVisible(false);
        this.radioOption1.setVisible(false);
        this.radioOption2.setVisible(false);
        this.radioOption3.setVisible(false);
    }

    protected void showOption() {
        this.lblOption.setVisible(true);
        this.radioOption1.setVisible(this.radioOption1.getText().length() != 0);
        this.radioOption2.setVisible(this.radioOption2.getText().length() != 0);
        this.radioOption3.setVisible(this.radioOption3.getText().length() != 0);
    }

    /**
     * Returns the value of the selected option:<br>
     * 1 for the first option<br>
     * 2 for the second option<br>
     * 3 for the third option
     */
    protected int getOptionValue() {
        if ( this.radioOption1.getSelection() )
            return 1;
        if ( this.radioOption2.getSelection() )
            return 2;
        if ( this.radioOption3.getSelection() )
            return 3;
        return 0;
    }



    static Shell dialogShell = null;
    static Composite dialogComposite = null;
    static Label dialogLabel = null;
    /**
     * shows up an on screen popup displaying the message but does not wait for any user input<br>
     * it is the responsibility of the caller to dismiss the popup 
     */
    public static Shell popup(String msg) {
        logger.info(DBGui.class, msg);

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                if ( dialogShell == null ) {
                    dialogShell = new Shell(display, SWT.APPLICATION_MODAL);
                    dialogShell.setSize(500, 70);
                    dialogShell.setBackground(BLACK_COLOR);

                    // Use the active shell, if available, to determine the new shell placing
                    int locationX = 0;
                    int locationY = 0;
                    Rectangle shellSize = dialogShell.getBounds();
                    Shell parent = display.getActiveShell();
                    if (parent!=null) { 
                    	Rectangle parentSize = parent.getBounds();
            	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
            	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                    } else {
            	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - 500) / 4;
            	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - 70) / 4;
                    }    
                    dialogShell.setLocation(new Point(locationX, locationY));

                    int borderWidth = (dialogShell.getBorderWidth()+1)*2;
                    dialogComposite = new Composite(dialogShell, SWT.NONE);
                    dialogComposite.setSize(500-borderWidth, 70-borderWidth);
                    dialogComposite.setLocation(1, 1);
                    dialogComposite.setBackground(COMPO_LEFT_COLOR);
                    dialogComposite.setLayout(new GridLayout( 1, false ) );

                    dialogLabel = new Label(dialogComposite, SWT.CENTER | SWT.WRAP);
                    dialogLabel.setBackground(COMPO_LEFT_COLOR);
                    dialogLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, true, true ) );
                    dialogLabel.setFont(TITLE_FONT);
                } else {
                    restoreCursors();
                }

                dialogLabel.setText(msg);
                dialogShell.layout(true);
                dialogShell.open();

                dialogComposite.layout();

                setArrowCursor();
            }
        });

        return dialogShell;
    }

    /**
     * dismiss the popup if it is displayed (else, does nothing) 
     */
    public static void closePopup() {
        if ( dialogShell != null ) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    dialogShell.close();
                    dialogShell = null;

                    restoreCursors();
                }
            });
        }
    }

    private static Stack<Map<Shell, Cursor>> cursorsStack = new Stack<Map<Shell, Cursor>>();
    public static void setArrowCursor() {
        Map<Shell, Cursor> cursors = new HashMap<Shell, Cursor>();
        for ( Shell shell: display.getShells() ) {
            cursors.put(shell,  shell.getCursor());
            shell.setCursor(CURSOR_WAIT);
        }
        cursorsStack.push(cursors);
        refreshDisplay();
    }

    public static void restoreCursors() {
        Map<Shell, Cursor> cursors = cursorsStack.pop();
        for ( Shell shell: display.getShells() ) {
            Cursor cursor = (cursors==null) ? null : cursors.get(shell);
            shell.setCursor(cursor==null ? CURSOR_ARROW : cursor);
        }
        refreshDisplay();
    }

    /**
     * Shows up an on screen popup displaying the message and wait for the user to click on the "OK" button
     */
    public static void popup(Level level, String msg) {
        popup(level,msg,null);
    }

    /**
     * Shows up an on screen popup, displaying the message (and the exception message if any) and wait for the user to click on the "OK" button<br>
     * The exception stacktrace is also printed on the standard error stream
     */
    public static void popup(Level level, String msg, Exception e) {
        String popupMessage = msg;
        logger.log(DBGui.class, level, msg, e);

        Throwable cause = e;
        while ( cause != null ) {
            if ( cause.getMessage() != null ) {
                if ( !popupMessage.endsWith(cause.getMessage()) )
                    popupMessage += "\n\n" + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            } else 
                popupMessage += "\n\n" + cause.getClass().getSimpleName();
            cause = cause.getCause();
        }

        switch ( level.toInt() ) {
            case Priority.FATAL_INT:
            case Priority.ERROR_INT:
                MessageDialog.openError(display.getActiveShell(), DBPlugin.pluginTitle, popupMessage);
                break;
            case Priority.WARN_INT:
                MessageDialog.openWarning(display.getActiveShell(), DBPlugin.pluginTitle, popupMessage);
                break;
            default:
                MessageDialog.openInformation(display.getActiveShell(), DBPlugin.pluginTitle, popupMessage);
                break;
        }

        refreshDisplay();
    }

    static int questionResult;

    /**
     * Shows up an on screen popup displaying the question (and the exception message if any)  and wait for the user to click on the "YES" or "NO" button<br>
     * The exception stacktrace is also printed on the standard error stream
     */
    public static boolean question(String msg) {
        return question(msg, new String[] {"Yes", "No"}) == 0;
    }

    /**
     * Shows up an on screen popup displaying the question (and the exception message if any)  and wait for the user to click on the "YES" or "NO" button<br>
     * The exception stacktrace is also printed on the standard error stream
     */
    public static int question(String msg, String[] buttonLabels) {
        if ( logger.isDebugEnabled() ) logger.debug(DBGui.class, "Question: "+msg);

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                //questionResult = MessageDialog.openQuestion(display.getActiveShell(), DBPlugin.pluginTitle, msg);
                Shell shell = new Shell(display, SWT.SHELL_TRIM);
                shell.setSize(0, 0);
                shell.setBackground(BLACK_COLOR);

                // Use the active shell, if available, to determine the new shell placing
                int locationX = 0;
                int locationY = 0;
                Rectangle shellSize = shell.getBounds();
                Shell parent = display.getActiveShell();
                if (parent!=null) { 
                	Rectangle parentSize = parent.getBounds();
        	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
        	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                } else {
        	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - shell.getSize().x) / 4;
        	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - shell.getSize().y) / 4;
                }
                shell.setLocation(new Point(locationX, locationY));
                MessageDialog messageDialog = new MessageDialog(shell, DBPlugin.pluginTitle, null, msg, MessageDialog.QUESTION, buttonLabels, 0);
                questionResult = messageDialog.open();
            }
        });

        if ( logger.isDebugEnabled() ) logger.debug(DBGui.class, "Answer: "+buttonLabels[questionResult]);
        return questionResult;
    }
    
    static String answeredPassword;
    
    /**
     * open up an input dialog and ask for a password
     * @param message the message on the password dialog
     * @return the typed password
     */
    public static String passwordDialog(String title, String message) {
    	if ( logger.isDebugEnabled() ) logger.debug(DBGui.class, "Asking for password");
    	answeredPassword = "";
    	Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = new Shell(display, SWT.SHELL_TRIM);
        		shell.setText(title);
                shell.setSize(0, 0);
                shell.setBackground(BLACK_COLOR);

                // Use the active shell, if available, to determine the new shell placing
                int locationX = 0;
                int locationY = 0;
                Rectangle shellSize = shell.getBounds();
                Shell parent = display.getActiveShell();
                if (parent!=null) { 
                	Rectangle parentSize = parent.getBounds();
        	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
        	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                } else {
        	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - shell.getSize().x) / 4;
        	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - shell.getSize().y) / 4;
                }
                shell.setLocation(new Point(locationX, locationY));
                
                DBGuiPasswordDialog passwordDialog = new DBGuiPasswordDialog(shell);
                if ( passwordDialog.open() == 0 )
                	answeredPassword = passwordDialog.getPassword();
                else
                	answeredPassword = null;
                passwordDialog.close();
            }
        });
    	
    	return answeredPassword;
    }

    protected void hideGrpDatabase() {
        this.grpDatabase.setVisible(false);
        this.grpDatabase.setData("visible", false);
    }

    protected void showGrpDatabase() {
        this.grpDatabase.setVisible(true);
        this.grpDatabase.setData("visible", true);
    }

    private SelectionListener actionListener = null;
    protected void setBtnAction(String label, SelectionListener listener) {
    	if ( label == null ) {
    		this.btnDoAction.setVisible(false);
    	} else {
	        this.btnDoAction.setText(label);
	        this.btnDoAction.setVisible(true);
	
	        if ( this.actionListener != null ) {
	            this.btnDoAction.removeSelectionListener(this.actionListener);
	            this.actionListener = null;
	        }
	
	        if ( listener != null ) {
	            this.actionListener = listener;
	            this.btnDoAction.addSelectionListener(this.actionListener);
	        }
    	}
    }

    /**
     * Creates the progress bar that will allow to follow the export process
     */
    protected void createProgressBar(String label, int min, int max) {
        if ( this.grpProgressBar == null ) {
            this.grpProgressBar = new Group(this.compoRightTop, SWT.NONE);
            this.grpProgressBar.setBackground(GROUP_BACKGROUND_COLOR);
            FormData fd = new FormData();
            fd.top = new FormAttachment(0);
            fd.left = new FormAttachment(0);
            fd.right = new FormAttachment(100);
            fd.bottom = new FormAttachment(100);
            this.grpProgressBar.setLayoutData(fd);
            this.grpProgressBar.setLayout(new FormLayout());


            this.lblProgressBar = new Label(this.grpProgressBar, SWT.CENTER);
            this.lblProgressBar.setBackground(GROUP_BACKGROUND_COLOR);
            this.lblProgressBar.setFont(TITLE_FONT);
            fd = new FormData();
            fd.top = new FormAttachment(0, -5);
            fd.left = new FormAttachment(0);
            fd.right = new FormAttachment(100);
            this.lblProgressBar.setLayoutData(fd);

            this.progressBar = new ProgressBar(this.grpProgressBar, SWT.NONE);
            fd = new FormData();
            fd.top = new FormAttachment(this.lblProgressBar);
            fd.left = new FormAttachment(25);
            fd.right = new FormAttachment(75);
            fd.height = 15;
            this.progressBar.setLayoutData(fd);
        }

        this.grpProgressBar.setVisible(true);
        this.grpProgressBar.setData("visible", true);

        this.grpProgressBar.moveAbove(null);

        this.lblProgressBar.setText(label);
        logger.info(DBGui.class, label);

        this.progressBar.setMinimum(min);
        this.progressBar.setMaximum(max);


        this.compoRightTop.layout();
        refreshDisplay();

        resetProgressBar();
    }

    public void hideProgressBar() {
        if ( this.progressBar != null ) {
            this.grpProgressBar.setVisible(false);
            this.grpProgressBar.setData("visible", false);
            refreshDisplay();
        }
    }

    public void setProgressBarLabel(String label) {
        if ( this.lblProgressBar == null )
            createProgressBar(label, 0, 100);
        else {
            this.lblProgressBar.setText(label);
            logger.info(DBGui.class, label);
        }
        refreshDisplay();
    }

    public String getProgressBarLabel() {
        if ( this.lblProgressBar == null )
            return "";

        return this.lblProgressBar.getText();
    }

    /**
     * Sets the min and max values of the progressBar and reset its selection to zero
     */
    public void setProgressBarMinAndMax(int min, int max) {
        if ( this.lblProgressBar != null ) {
            this.progressBar.setMinimum(min);
            this.progressBar.setMaximum(max);
        }
        resetProgressBar();
    }

    /**
     * Resets the progressBar to zero in the SWT thread (thread safe method)
     */
    public void resetProgressBar() {
        if ( this.lblProgressBar != null )
            this.progressBar.setSelection(0);
        refreshDisplay();
    }

    /**
     * Increases the progressBar selection in the SWT thread (thread safe method)
     */
    public void increaseProgressBar() {
        if ( this.lblProgressBar != null )
            this.progressBar.setSelection(this.progressBar.getSelection()+1);
        refreshDisplay();
    }

    public void setMessage(String message) {
        setMessage(message, GROUP_BACKGROUND_COLOR);
    }

    protected void setMessage(String message, Color background) {
        if ( this.grpMessage == null ) {
            this.grpMessage = new Group(this.compoRightTop, SWT.NONE);
            this.grpMessage.setBackground(GROUP_BACKGROUND_COLOR);
            this.grpMessage.setFont(GROUP_TITLE_FONT);
            this.grpMessage.setText("Please wait ... ");
            FormData fd = new FormData();
            fd.top = new FormAttachment(0);
            fd.left = new FormAttachment(0);
            fd.right = new FormAttachment(100);
            fd.bottom = new FormAttachment(100);
            this.grpMessage.setLayoutData(fd);
            this.grpMessage.setLayout(new FormLayout());

            this.lblMessage = new CLabel(this.grpMessage, SWT.CENTER);
            this.lblMessage.setAlignment(SWT.CENTER); 
            this.lblMessage.setBackground(GROUP_BACKGROUND_COLOR);
            this.lblMessage.setFont(TITLE_FONT);
            fd = new FormData();
            fd.top = new FormAttachment(0);
            fd.left = new FormAttachment(0);
            fd.right = new FormAttachment(100);
            fd.bottom = new FormAttachment(100);
            this.lblMessage.setLayoutData(fd);
            refreshDisplay();
        }

        this.grpMessage.setVisible(true);

        if (this.grpProgressBar != null )
            this.grpProgressBar.setVisible(false);

        if ( this.grpDatabase != null )
            this.grpDatabase.setVisible(false);

        this.compoRightTop.layout();

        this.lblMessage.setBackground(background);

        String msg = message.replace("\n\n", "\n");
        if ( background == RED_COLOR )
            logger.error(DBGui.class, msg);
        else
            logger.info(DBGui.class, msg);

        this.lblMessage.setText(msg);
        
        this.grpMessage.moveAbove(null);

        refreshDisplay();
    }


    public void closeMessage() {
        if ( (this.grpMessage != null) && !this.grpMessage.isDisposed() ) {
            this.grpMessage.setVisible(false);

            if (this.grpProgressBar != null && (this.grpProgressBar.getData("visible") != null) )
                this.grpProgressBar.setVisible((boolean)this.grpProgressBar.getData("visible"));

            if ( this.grpDatabase != null && (this.grpDatabase.getData("visible") != null) )
                this.grpDatabase.setVisible((boolean)this.grpDatabase.getData("visible"));

            this.compoRightTop.layout();
            refreshDisplay();
        }
    }

    /**
     * Method used to close graphical objects if needed
     */
    public void close() {
        this.dialog.dispose();
        this.dialog = null;

        restoreCursors();
    }
    
    public void commitAndCloseConnection() throws SQLException {
        if ( this.connection != null ) {
            // in case some transactions have been started, we commit them
            this.connection.commit();
            
             this.connection.close();
             this.connection = null;
        }
    }
    
    public void rollbackAndCloseConnection() throws SQLException {
        if ( this.connection != null ) {
            // in case some transactions have been started, we roll them back
            this.connection.rollback();

             this.connection.close();
             this.connection = null;
        }
    }

    public boolean isDisposed() {
        return this.dialog==null ? true : this.dialog.isDisposed();
    }

    protected Boolean fillInCompareTable(Tree tree, EObject memoryObject, int memoryObjectversion) {
        return fillInCompareTable(tree, null, memoryObject, memoryObjectversion);
    }

    @SuppressWarnings("unchecked")
    protected Boolean fillInCompareTable(Tree tree, TreeItem treeItem, EObject memoryObject, int memoryObjectversion) {
        assert ( memoryObject!=null );
        DBMetadata dbMetadata = DBMetadata.getDBMetadata(memoryObject);

        logger.debug(DBGui.class, "Showing up memory and database versions of component "+dbMetadata.getDebugName());

        // we get the database version of the component
        HashMap<String, Object> databaseObject;
        try {
            databaseObject = this.connection.getObjectFromDatabase(memoryObject, memoryObjectversion);
        } catch (Exception err) {
            DBGui.popup(Level.ERROR, "Failed to get component "+dbMetadata.getDebugName()+" from the database.", err);
            return null;
        }

        boolean areIdentical = true;

        if ( treeItem == null ) {          // the root component
            tree.removeAll();
            refreshDisplay();

            TreeItem item = new TreeItem(tree, SWT.NONE);
            item.setText(new String[] {"Version", String.valueOf(dbMetadata.getInitialVersion().getVersion()), String.valueOf(databaseObject.get("version"))});

            if ( (String)databaseObject.get("created_by") != null ) {
                item = new TreeItem(tree, SWT.NONE);
                item.setText(new String[] {"Created by", System.getProperty("user.name"), (String)databaseObject.get("created_by")}); 
            }

            if ( databaseObject.get("created_on") != null ) {
                item = new TreeItem(tree, SWT.NONE);
                if ( dbMetadata.getDatabaseVersion().getTimestamp() != null )
                    item.setText(new String[] {"Created on", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dbMetadata.getInitialVersion().getTimestamp().getTime()), new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(databaseObject.get("created_on"))});
                else
                    item.setText(new String[] {"Created on", "", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(databaseObject.get("created_on"))});
            }
        }

        areIdentical &= areIdentical &= addItemToCompareTable(tree, treeItem, "Class", memoryObject.getClass().getSimpleName(), (String)databaseObject.get("class"));
        areIdentical &= addItemToCompareTable(tree, treeItem, "Name", ((INameable)memoryObject).getName(), (String)databaseObject.get("name"));

        if ( memoryObject instanceof IDocumentable )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Documentation", ((IDocumentable)memoryObject).getDocumentation(), (String)databaseObject.get("documentation"));

        if ( memoryObject instanceof IJunction )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Type", ((IJunction)memoryObject).getType(), (String)databaseObject.get("type"));

        if ( memoryObject instanceof IArchimateRelationship ) {
            areIdentical &= addItemToCompareTable(tree, treeItem, "Source id", ((IArchimateRelationship)memoryObject).getSource().getId(), (String)databaseObject.get("source_id"));
            areIdentical &= addItemToCompareTable(tree, treeItem, "Target id", ((IArchimateRelationship)memoryObject).getTarget().getId(), (String)databaseObject.get("target_id"));
            if ( memoryObject instanceof IInfluenceRelationship )
                areIdentical &= addItemToCompareTable(tree, treeItem, "Strength", ((IInfluenceRelationship)memoryObject).getStrength(), (String)databaseObject.get("strength"));
            if ( memoryObject instanceof IAccessRelationship )
                areIdentical &= addItemToCompareTable(tree, treeItem, "Access type", String.valueOf(((IAccessRelationship)memoryObject).getAccessType()), String.valueOf((int)databaseObject.get("access_type")));
        }

        // TODO: get folders subfolders and elements from the database in order to compare them
        if ( memoryObject instanceof IFolder )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Folder type", ((IFolder)memoryObject).getType().getLiteral(), FolderType.get((int)databaseObject.get("type")).getLiteral());

        if ( memoryObject instanceof IArchimateDiagramModel )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Viewpoint", ((IArchimateDiagramModel)memoryObject).getViewpoint(), (String)databaseObject.get("viewpoint"));

        if ( memoryObject instanceof IDiagramModel )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Router type", String.valueOf(((IDiagramModel)memoryObject).getConnectionRouterType()), databaseObject.get("connection_router_type")==null ? null : String.valueOf((int)databaseObject.get("connection_router_type")));

        if ( memoryObject instanceof IBorderObject )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Border color", ((IBorderObject)memoryObject).getBorderColor(), (String)databaseObject.get("border_color"));

        if ( memoryObject instanceof IDiagramModelNote )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Border type", String.valueOf(((IDiagramModelNote)memoryObject).getBorderType()), databaseObject.get("border_type")==null ? null : String.valueOf((int)databaseObject.get("border_type")));

        if ( memoryObject instanceof IConnectable ) {
            // TODO: get source and target connections from database and compare then to component's ones
            /*
			for ( IDiagramModelConnection conn: ((IConnectable)component).getSourceConnections() )
				areIdentical &= addItemToCompareTable(table, level, "source connections", conn.getId(), (String)hashResult.get("xxxxx"));
			for ( IDiagramModelConnection conn: ((IConnectable)component).getTargetConnections() )
				areIdentical &= addItemToCompareTable(table, level, "target connections", conn.getId(), (String)hashResult.get("xxxxx"));
             */
        }

        if ( memoryObject instanceof IDiagramModelArchimateObject )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Type", String.valueOf(((IDiagramModelArchimateObject)memoryObject).getType()), databaseObject.get("type")==null ? null : String.valueOf((int)databaseObject.get("type")));

        if ( memoryObject instanceof IDiagramModelImageProvider )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Image path", ((IDiagramModelImageProvider)memoryObject).getImagePath(), (String)databaseObject.get("image_path"));

        if ( memoryObject instanceof IDiagramModelObject ) {
            areIdentical &= addItemToCompareTable(tree, treeItem, "Fill color", ((IDiagramModelObject)memoryObject).getFillColor(), (String)databaseObject.get("fill_color"));
            IBounds bounds = ((IDiagramModelObject)memoryObject).getBounds();
            areIdentical &= addItemToCompareTable(tree, treeItem, "X", String.valueOf(bounds.getX()), databaseObject.get("x") == null ? null : String.valueOf((int)databaseObject.get("x")));
            areIdentical &= addItemToCompareTable(tree, treeItem, "Y", String.valueOf(bounds.getY()), databaseObject.get("y")==null ? null : String.valueOf((int)databaseObject.get("y")));
            areIdentical &= addItemToCompareTable(tree, treeItem, "Width" ,String.valueOf(bounds.getWidth()), databaseObject.get("width")==null ? null : String.valueOf((int)databaseObject.get("width")));
            areIdentical &= addItemToCompareTable(tree, treeItem, "Height", String.valueOf(bounds.getHeight()), databaseObject.get("height")==null ? null : String.valueOf((int)databaseObject.get("height")));
        }

        if ( memoryObject instanceof IDiagramModelArchimateComponent )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Archimate concept", ((IDiagramModelArchimateComponent)memoryObject).getArchimateConcept().getId(), (String)databaseObject.get("element_id"));

        if ( memoryObject instanceof IDiagramModelArchimateConnection )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Archimate concept", ((IDiagramModelArchimateConnection)memoryObject).getArchimateConcept().getId(), (String)databaseObject.get("element_id"));

        if ( memoryObject instanceof IFontAttribute ) {
            areIdentical &= addItemToCompareTable(tree, treeItem, "Font", ((IFontAttribute)memoryObject).getFont(), (String)databaseObject.get("font"));
            areIdentical &= addItemToCompareTable(tree, treeItem, "Font color", ((IFontAttribute)memoryObject).getFontColor(), (String)databaseObject.get("font_color"));
        }

        if ( memoryObject instanceof ILineObject ) {
            areIdentical &= addItemToCompareTable(tree, treeItem, "Line width", String.valueOf(((ILineObject)memoryObject).getLineWidth()), databaseObject.get("line_width")==null ? null : String.valueOf((int)databaseObject.get("line_width")));
            areIdentical &= addItemToCompareTable(tree, treeItem, "Line color", ((ILineObject)memoryObject).getLineColor(), (String)databaseObject.get("line_color"));
        }

        if ( memoryObject instanceof ILockable ) {
            // the database can contain a boolean, a char (oracle) or an integer (sqlite, mysql, mssql, postgres) depending on the database brand
            String isLockedInDatabase = null;
            if ( databaseObject.get("is_locked") != null ) {
                if ( databaseObject.get("is_locked") instanceof Boolean )
                    isLockedInDatabase = String.valueOf((boolean)databaseObject.get("is_locked"));
                
                if ( databaseObject.get("is_locked") instanceof Integer )
                    isLockedInDatabase = ((int)databaseObject.get("is_locked") == 0) ? "false" : "true";
                
                if ( databaseObject.get("is_locked") instanceof String )
                    isLockedInDatabase = (Integer.valueOf((String)databaseObject.get("is_locked")) == 0) ? "false" : "true";
            }
            areIdentical &= addItemToCompareTable(tree, treeItem, "Locked", String.valueOf(((ILockable)memoryObject).isLocked()), isLockedInDatabase);
        }

        if ( memoryObject instanceof ISketchModel )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Background", String.valueOf(((ISketchModel)memoryObject).getBackground()), databaseObject.get("background")==null ? null : String.valueOf((int)databaseObject.get("background")));

        if ( memoryObject instanceof ITextAlignment )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Text alignment", String.valueOf(((ITextAlignment)memoryObject).getTextAlignment()), databaseObject.get("text_alignment")==null ? null : String.valueOf((int)databaseObject.get("text_alignment")));

        if ( memoryObject instanceof ITextPosition )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Text position", String.valueOf(((ITextPosition)memoryObject).getTextPosition()), databaseObject.get("text_position")==null ? null : String.valueOf((int)databaseObject.get("text_position")));

        if ( memoryObject instanceof ITextContent )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Content", ((ITextContent)memoryObject).getContent(), (String)databaseObject.get("content"));
        
        if ( memoryObject instanceof IIconic )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Image position", String.valueOf(((IIconic)memoryObject).getImagePosition()), databaseObject.get("image_position")==null ? null : String.valueOf((int)databaseObject.get("image_position")));
        
        if ( memoryObject instanceof INotesContent )
            areIdentical &= addItemToCompareTable(tree, treeItem, "Notes", ((INotesContent)memoryObject).getNotes(), (String)databaseObject.get("notes"));

        if ( memoryObject instanceof IDiagramModelConnection ) {
            areIdentical &= addItemToCompareTable(tree, treeItem, "Type", String.valueOf(((IDiagramModelConnection)memoryObject).getType()), databaseObject.get("type")==null ? null : String.valueOf((int)databaseObject.get("type")));			// we do not use getText as it is deprecated
            areIdentical &= addItemToCompareTable(tree, treeItem, "Text position", String.valueOf(((IDiagramModelConnection)memoryObject).getTextPosition()), databaseObject.get("text_position")==null ? null : String.valueOf((int)databaseObject.get("text_position")));

            // we show up the bendpoints only if they both exist
            if ( databaseObject.containsKey("bendpoints") ) {
                if ( (((IDiagramModelConnection)memoryObject).getBendpoints().size() != 0) || (((ArrayList<DBBendpoint>)databaseObject.get("bendpoints")).size() != 0) ) {
                    TreeItem bendpointsTreeItem;
                    if ( treeItem == null )
                        bendpointsTreeItem = new TreeItem(tree, SWT.NONE);
                    else
                        bendpointsTreeItem = new TreeItem(treeItem, SWT.NONE);
                    bendpointsTreeItem.setText("Bendpoints");
                    bendpointsTreeItem.setExpanded(false);

                    // we get a list of component's bendpoints
                    Integer[][] componentBendpoints = new Integer[((IDiagramModelConnection)memoryObject).getBendpoints().size()][4];
                    for (int i = 0; i < ((IDiagramModelConnection)memoryObject).getBendpoints().size(); ++i) {
                        componentBendpoints[i] = new Integer[] { ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getStartX(), ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getStartY(), ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getEndX(), ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getEndY() };
                    }
                    //Arrays.sort(componentBendpoints, this.integerComparator);www

                    // we get a list of properties from the database
                    Integer[][] databaseBendpoints = new Integer[((ArrayList<DBBendpoint>)databaseObject.get("bendpoints")).size()][4];
                    int i = 0;
                    for (DBBendpoint bp: (ArrayList<DBBendpoint>)databaseObject.get("bendpoints") ) {
                        componentBendpoints[i] = new Integer[] { bp.getStartX(), bp.getStartY(), bp.getEndX(), bp.getEndY() };
                        ++i;
                    }
                    //Arrays.sort(databaseBendpoints, this.integerComparator);

                    int indexComponent = 0;
                    int indexDatabase = 0;
                    while ( (indexComponent < componentBendpoints.length) || (indexDatabase < databaseBendpoints.length) ) {
                        TreeItem subTreeItem = new TreeItem(bendpointsTreeItem, SWT.NONE);
                        subTreeItem.setText("Bendpoint "+Math.max(indexComponent, indexDatabase)+1);
                        subTreeItem.setExpanded(false);
                        if ( indexComponent >= componentBendpoints.length ) {			// only the database has got the property
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "Start X", null, String.valueOf(databaseBendpoints[indexDatabase][0]));
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "Start Y", null, String.valueOf(databaseBendpoints[indexDatabase][1]));
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "End X", null, String.valueOf(databaseBendpoints[indexDatabase][2]));
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "End Y", null, String.valueOf(databaseBendpoints[indexDatabase][3]));
                            ++indexDatabase;
                        } else if ( indexDatabase >= databaseBendpoints.length ) {		// only the component has got the property
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "Start X", String.valueOf(componentBendpoints[indexComponent][0]), null);
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "Start Y", String.valueOf(componentBendpoints[indexComponent][1]), null);
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "End X", String.valueOf(componentBendpoints[indexComponent][2]), null);
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "End Y", String.valueOf(componentBendpoints[indexComponent][3]), null);
                            ++indexComponent;
                        } else {
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "Start X", String.valueOf(componentBendpoints[indexComponent][0]), String.valueOf(databaseBendpoints[indexDatabase][0]));
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "Start Y", String.valueOf(componentBendpoints[indexComponent][1]), String.valueOf(databaseBendpoints[indexDatabase][1]));
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "End X", String.valueOf(componentBendpoints[indexComponent][2]), String.valueOf(databaseBendpoints[indexDatabase][2]));
                            areIdentical &= addItemToCompareTable(tree, subTreeItem, "End Y", String.valueOf(componentBendpoints[indexComponent][3]), String.valueOf(databaseBendpoints[indexDatabase][3]));
                            ++indexComponent;
                            ++indexDatabase;
                        }
                    }
                }
            }
        }

        // we show up the properties if both exist
        if ( databaseObject.containsKey("properties") ) {
            if ( memoryObject instanceof IProperties && ((IProperties)memoryObject).getProperties().size() != 0) {
                TreeItem propertiesTreeItem;
                if ( treeItem == null )
                    propertiesTreeItem = new TreeItem(tree, SWT.NONE);
                else
                    propertiesTreeItem = new TreeItem(treeItem, SWT.NONE);
                propertiesTreeItem.setText("Properties");
                propertiesTreeItem.setExpanded(true);

                // we get a sorted list of component's properties
                ArrayList<DBProperty> componentProperties = new ArrayList<DBProperty>();
                for (int i = 0; i < ((IProperties)memoryObject).getProperties().size(); ++i) {
                    componentProperties.add(new DBProperty(((IProperties)memoryObject).getProperties().get(i).getKey(), ((IProperties)memoryObject).getProperties().get(i).getValue()));
                }
                Collections.sort(componentProperties, this.propertyComparator);

                // we get a sorted list of properties from the database
                ArrayList<DBProperty> databaseProperties = (ArrayList<DBProperty>)databaseObject.get("properties");
                Collections.sort(databaseProperties, this.propertyComparator);

                Collator collator = Collator.getInstance();
                int indexComponent = 0;
                int indexDatabase = 0;
                int compare;
                while ( (indexComponent < componentProperties.size()) || (indexDatabase < databaseProperties.size()) ) {
                    if ( indexComponent >= componentProperties.size() )
                        compare = 1;
                    else {
                        if ( indexDatabase >= databaseProperties.size() )
                            compare = -1;
                        else
                            compare = collator.compare(componentProperties.get(indexComponent).getKey(), databaseProperties.get(indexDatabase).getKey());
                    }

                    if ( compare == 0 ) {				// both have got the same property
                        areIdentical &= addItemToCompareTable(tree, propertiesTreeItem, componentProperties.get(indexComponent).getKey(), componentProperties.get(indexComponent).getValue(), databaseProperties.get(indexDatabase).getValue());
                        ++indexComponent;
                        ++indexDatabase;
                    } else if ( compare < 0 ) {			// only the component has got the property
                        areIdentical &= addItemToCompareTable(tree, propertiesTreeItem, componentProperties.get(indexComponent).getKey(), componentProperties.get(indexComponent).getValue(), null);
                        ++indexComponent;
                    } else {							// only the database has got the property
                        areIdentical &= addItemToCompareTable(tree, propertiesTreeItem, componentProperties.get(indexDatabase).getKey(), null, databaseProperties.get(indexDatabase).getValue());
                        ++indexDatabase;
                    }
                }
            }
        }

        refreshDisplay();
        return areIdentical;
    }

    Comparator<DBProperty> propertyComparator = new Comparator<DBProperty>() {
        @Override
        public int compare(final DBProperty row1, final DBProperty row2) {
            return Collator.getInstance().compare(row1.getKey(),row2.getKey());
        }
    };

    Comparator<Integer[]> integerComparator = new Comparator<Integer[]>() {
        @Override
        public int compare(final Integer[] row1, final Integer[] row2) {
            return Collator.getInstance().compare(row1[0],row2[0]);
        }
    };


    /**
     * Helper function to fill in the compareTable
     * @return true if col2 and col3 are equals, false if they differ 
     */
    private static boolean addItemToCompareTable(Tree tree, TreeItem treeItem, String col1, String col2, String col3) {
        TreeItem subTreeItem;
        boolean isIdentical;

        if ( treeItem != null ) 
            subTreeItem = new TreeItem(treeItem, SWT.NULL);
        else
            subTreeItem = new TreeItem(tree, SWT.NONE);

        subTreeItem.setText(new String[] {col1, col2, col3 });
        if ( !DBPlugin.areEqual(col2, col3) ) {
            if ( treeItem != null )
                treeItem.setBackground(DBGui.LIGHT_RED_COLOR);
            subTreeItem.setBackground(DBGui.LIGHT_RED_COLOR);
            isIdentical = false;
        } else
            isIdentical = true;

        refreshDisplay();
        return isIdentical;
    }

    public byte[] createImage(IDiagramModel view, int scalePercent, int margin) {
        byte[] imageContent = null;
        DBMetadata dbMetadata = DBMetadata.getDBMetadata(view); 

        String oldLabel = getProgressBarLabel();
        logger.debug(DBGui.class, "Creating screenshot of view \""+view.getName()+"\"");

        try ( ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
            try ( DataOutputStream writeOut = new DataOutputStream(out) ) {
                ImageLoader saver = new ImageLoader();
                ModelReferencedImage viewImage = DiagramUtils.createModelReferencedImage(view, scalePercent/100.0, margin);
                Image image = viewImage.getImage();

                saver.data = new ImageData[] { image.getImageData(ImageFactory.getDeviceZoom()) };
                image.dispose();

                saver.save(writeOut, SWT.IMAGE_PNG);
                imageContent = out.toByteArray();

                org.eclipse.draw2d.geometry.Rectangle bounds = viewImage.getBounds();
                bounds.performScale(ImageFactory.getDeviceZoom() / 100); // Account for device zoom level

                dbMetadata.getScreenshot().setScreenshotBytes(imageContent);
                dbMetadata.getScreenshot().setScaleFactor(scalePercent);
                dbMetadata.getScreenshot().setBorderWidth(margin);
                dbMetadata.getScreenshot().setBounds(bounds);
            } catch (IOException err) {
                logger.error(DBGui.class, "Failed to close DataOutputStream", err);
            }
        } catch (IOException err) {
            logger.error(DBGui.class, "Failed to close ByteArrayOutputStream", err);
        }

        setProgressBarLabel(oldLabel);

        return imageContent;
    }

    /**
     * Refreshes the display
     */
    public static void refreshDisplay() {
        while ( DBGui.display.readAndDispatch() ) {
            // nothing to do
        }
    }

    public static void incrementText(Text txt) {
        if ( txt != null ) {
            try {
                txt.setText(toString(toInt(txt.getText())+1));
            } catch (@SuppressWarnings("unused") Exception ign) {
                // ignore
            }
        }
    }

    public static void decrementText(Text txt) {
        if ( txt != null ) {
            try {
                txt.setText(toString(toInt(txt.getText())-1));
            } catch (@SuppressWarnings("unused") Exception ign) {
                // ignore
            }
        }
    }

    public static String toString(int value) {
        if ( (value == 0) && !DBPlugin.INSTANCE.getPreferenceStore().getBoolean("showZeroValues") )
            return "";
        return String.valueOf(value);
    }

    public static int toInt(String value) {
        if ( DBPlugin.isEmpty(value) )
            return 0;
        return Integer.valueOf(value);
    }

    protected DBDatabaseConnection getDatabaseConnection() {
        return this.connection;
    }
}