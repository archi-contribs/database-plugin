/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database.GUI;

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Level;
import org.archicontribs.database.DBDatabaseConnection;
import org.archicontribs.database.DBDatabaseEntry;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.archicontribs.database.model.IDBMetadata;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.archimatetool.canvas.model.IHintProvider;
import com.archimatetool.canvas.model.IIconic;
import com.archimatetool.canvas.model.INotesContent;
import com.archimatetool.editor.diagram.util.DiagramUtils;
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
import com.archimatetool.model.IDiagramModelContainer;
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

public class DBGui {
	protected static final DBLogger logger = new DBLogger(DBGui.class);
	
	protected List<DBDatabaseEntry> databaseEntries;
	protected DBDatabaseEntry selectedDatabase;
	protected DBDatabaseConnection connection;
	
	protected static final Display display = Display.getCurrent();
	protected Shell dialog;
	
	protected boolean includeNeo4j = true;
	
	private String HELP_HREF = null;
	private boolean mouseOverHelpButton = false;
	
	protected enum ACTION {One, Two, Three, Four};
	protected enum STATUS {Empty, Selected, Running, Bypassed, Ok, Warn, Error};
	
	public static final Color LIGHT_GREEN_COLOR = new Color(display, 204, 255, 229);
	public static final Color LIGHT_RED_COLOR = new Color(display, 255, 230, 230);
	public static final Color RED_COLOR = new Color(display, 240, 0, 0);
	public static final Color GREEN_COLOR = new Color(display, 0, 180, 0);
	public static final Color WHITE_COLOR = new Color(display, 255, 255, 255);
	public static final Color GREY_COLOR = new Color(display, 100, 100, 100);
	public static final Color BLACK_COLOR = new Color(display, 0, 0, 0);
	
	public static final Color COMPO_LEFT_COLOR = new Color(display, 240, 248, 255);			// light blue
	public static final Color COMPO_BACKGROUND_COLOR = new Color(display, 250, 250, 250);	// light grey
	public static final Color GROUP_BACKGROUND_COLOR = new Color(display, 235, 235, 235);	// light grey (a bit darker than compo background)
	public static final Color TABLE_BACKGROUND_COLOR = GROUP_BACKGROUND_COLOR;
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
	private Button radioOption1;
	private Button radioOption2; 
	
	protected Combo comboDatabases;
	protected Button btnSetPreferences;
	protected Button btnClose;
	protected Button btnDoAction;
	protected Label btnHelp;
	
	private Group grpDatabase;
	
	protected Group grpProgressBar;
	protected Label lblProgressBar;
	private ProgressBar progressBar;

	/**
	 * Create the dialog with minimal graphical objects : 
	 * 		left composite : picture of a database with Archimate diagram inside, the plugin version, (my name of course) and 4 icons + texts to list actions 
	 * 		bottom composite : Close, doAction button at the right and help buton on the left
	 * 		right composite : database list in a combo and a button to set preferences
	 */
	protected DBGui(String title) {
		if ( logger.isDebugEnabled() ) logger.debug("Creating Form GUI.");
		
		setArrowCursor();

		dialog = new Shell(display, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText(DBPlugin.pluginTitle + " - " + title);
		dialog.setMinimumSize(750, 550);
		dialog.setSize(800,600);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 4);
		dialog.setLayout(new FormLayout());
		
	    dialog.addListener(SWT.Close, new Listener()
	    {
	        public void handleEvent(Event event)
	        {
	            close();
	            event.doit = true;         // TODO : manage stopping the import or export in the middle
	        }
	    });
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////// compoLeft ////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		compoLeft = new Composite(dialog, SWT.BORDER);
		compoLeft.setBackground(COMPO_LEFT_COLOR);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(0, 160);
		fd.bottom = new FormAttachment(100, -40);
		compoLeft.setLayoutData(fd);
		compoLeft.setLayout(new FormLayout());
		
		Composite compoTitle = new Composite(compoLeft, SWT.BORDER);
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
		lblPluginVersion.setText(DBPlugin.pluginVersion);
		fd = new FormData();
		fd.top = new FormAttachment(lblTitle, 5);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		lblPluginVersion.setLayoutData(fd);
		
		Label imgDatabase = new Label(compoLeft, SWT.CENTER);
		imgDatabase.setBackground(COMPO_LEFT_COLOR);
		imgDatabase.setImage(LOGO_IMAGE);
		fd = new FormData(135,115);
		fd.top = new FormAttachment(compoTitle, 30);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		imgDatabase.setLayoutData(fd);
		
		imgFirstAction = new Label(compoLeft, SWT.CENTER);
		imgFirstAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData(10,10);
		fd.top = new FormAttachment(imgDatabase, 50);
		fd.left = new FormAttachment(0, 10);
		imgFirstAction.setLayoutData(fd);
		
		lblFirstAction = new Label(compoLeft, SWT.NONE);
		lblFirstAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(imgFirstAction, 0, SWT.CENTER);
		fd.left = new FormAttachment(imgFirstAction, 10);
		fd.right = new FormAttachment(100, -10);
		lblFirstAction.setLayoutData(fd);
		
		imgSecondAction = new Label(compoLeft, SWT.CENTER);
		imgSecondAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData(10,10);
		fd.top = new FormAttachment(imgFirstAction, 10);
		fd.left = new FormAttachment(0, 10);
		imgSecondAction.setLayoutData(fd);
		
		lblSecondAction = new Label(compoLeft, SWT.NONE);
		lblSecondAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(imgSecondAction, 0, SWT.CENTER);
		fd.left = new FormAttachment(imgSecondAction, 10);
		fd.right = new FormAttachment(100, -10);
		lblSecondAction.setLayoutData(fd);
		
		imgThirdAction = new Label(compoLeft, SWT.CENTER);
		imgThirdAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData(10,10);
		fd.top = new FormAttachment(imgSecondAction, 10);
		fd.left = new FormAttachment(0, 10);
		imgThirdAction.setLayoutData(fd);
		
		lblThirdAction = new Label(compoLeft, SWT.NONE);
		lblThirdAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(imgThirdAction, 0, SWT.CENTER);
		fd.left = new FormAttachment(imgThirdAction, 10);
		fd.right = new FormAttachment(100, -10);
		lblThirdAction.setLayoutData(fd);
		
		imgFourthAction = new Label(compoLeft, SWT.CENTER);
		imgFourthAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData(10,10);
		fd.top = new FormAttachment(imgThirdAction, 10);
		fd.left = new FormAttachment(0, 10);
		imgFourthAction.setLayoutData(fd);
		
		lblFourthAction = new Label(compoLeft, SWT.NONE);
		lblFourthAction.setBackground(COMPO_LEFT_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(imgFourthAction, 0, SWT.CENTER);
		fd.left = new FormAttachment(imgFourthAction, 10);
		fd.right = new FormAttachment(100, -10);
		lblFourthAction.setLayoutData(fd);
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////// compoRight ///////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		compoRight = new Composite(dialog, SWT.BORDER);
		compoRight.setBackground(COMPO_BACKGROUND_COLOR);
		FormData fd_compoRight = new FormData();
		fd_compoRight.top = new FormAttachment(0);
		fd_compoRight.bottom = new FormAttachment(100, -40);
		fd_compoRight.left = new FormAttachment(compoLeft);
		fd_compoRight.right = new FormAttachment(100);
		compoRight.setLayoutData(fd_compoRight);
		compoRight.setLayout(new FormLayout());
		
		compoRightTop = new Composite(compoRight, SWT.NONE);
		compoRightTop.setBackground(COMPO_BACKGROUND_COLOR);
		FormData fd_compoRightUp = new FormData();
		fd_compoRightUp.top = new FormAttachment(0, 10);
		fd_compoRightUp.bottom = new FormAttachment(0, 60);
		fd_compoRightUp.left = new FormAttachment(0, 10);
		fd_compoRightUp.right = new FormAttachment(100, -10);
		compoRightTop.setLayoutData(fd_compoRightUp);
		compoRightTop.setLayout(new FormLayout());
		
		compoRightBottom = new Composite(compoRight, SWT.NONE);
		compoRightBottom.setBackground(COMPO_BACKGROUND_COLOR);
		FormData fd_compoRightBottom = new FormData();
		fd_compoRightBottom.top = new FormAttachment(compoRightTop, 10);
		fd_compoRightBottom.bottom = new FormAttachment(100, -10);
		fd_compoRightBottom.left = new FormAttachment(0, 10);
		fd_compoRightBottom.right = new FormAttachment(100, -10);
		compoRightBottom.setLayoutData(fd_compoRightBottom);
		compoRightBottom.setLayout(new FormLayout());
		
		grpDatabase = new Group(compoRightTop, SWT.SHADOW_ETCHED_IN);
		grpDatabase.setBackground(GROUP_BACKGROUND_COLOR);
		grpDatabase.setFont(GROUP_TITLE_FONT);
		grpDatabase.setText("Database : ");
		fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100);
		grpDatabase.setLayoutData(fd);
		grpDatabase.setLayout(new FormLayout());
		
		Label lblRegisteredDatabases = new Label(grpDatabase, SWT.NONE);
		lblRegisteredDatabases.setBackground(GROUP_BACKGROUND_COLOR);
		lblRegisteredDatabases.setText("Registered databases :");
		fd = new FormData();
		fd.top = new FormAttachment(25);
		fd.left = new FormAttachment(0, 10);
		lblRegisteredDatabases.setLayoutData(fd);
		
		btnSetPreferences = new Button(grpDatabase, SWT.NONE);
		btnSetPreferences.setText("Set preferences ...");
		btnSetPreferences.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) { try { setPreferences(); } catch (Exception e) { popup(Level.ERROR, "Failed to set preferences", e); } }
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(lblRegisteredDatabases, 0, SWT.CENTER);
		fd.right = new FormAttachment(100, -10);
		btnSetPreferences.setLayoutData(fd);
		
		comboDatabases = new Combo(grpDatabase, SWT.NONE | SWT.READ_ONLY);
		comboDatabases.setBackground(GROUP_BACKGROUND_COLOR);
		comboDatabases.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) { databaseSelected(); }
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(lblRegisteredDatabases, 0, SWT.CENTER);
		fd.left = new FormAttachment(lblRegisteredDatabases, 10);
		fd.right = new FormAttachment(btnSetPreferences, -40);
		comboDatabases.setLayoutData(fd);
		

		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////// compoBottom //////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		compoBottom = new Composite(dialog, SWT.NONE);
		compoBottom.setBackground(COMPO_BACKGROUND_COLOR);
		fd = new FormData();
		fd.top = new FormAttachment(100, -40);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		fd.bottom = new FormAttachment(100);
		compoBottom.setLayoutData(fd);
		compoBottom.setLayout(new FormLayout());
		
		btnHelp = new Label(compoBottom, SWT.NONE);
		btnHelp.setVisible(false);
		btnHelp.addListener(SWT.MouseEnter, new Listener() { @Override public void handleEvent(Event event) { mouseOverHelpButton = true; btnHelp.redraw(); } });
		btnHelp.addListener(SWT.MouseExit, new Listener() { @Override public void handleEvent(Event event) { mouseOverHelpButton = false; btnHelp.redraw(); } });
		btnHelp.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent event)
            {
                 if ( mouseOverHelpButton ) event.gc.drawRoundRectangle(0, 0, 29, 29, 10, 10);
                 event.gc.drawImage(HELP_ICON, 2, 2);
            }
        });
		btnHelp.addListener(SWT.MouseUp, new Listener() { @Override public void handleEvent(Event event) { if ( HELP_HREF != null ) { if ( logger.isDebugEnabled() ) logger.debug("Showing help : /"+DBPlugin.PLUGIN_ID+"/help/html/"+HELP_HREF); PlatformUI.getWorkbench().getHelpSystem().displayHelpResource("/"+DBPlugin.PLUGIN_ID+"/help/html/"+HELP_HREF); } } });
		fd = new FormData(30,30);
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 5);
		btnHelp.setLayoutData(fd);
		

		btnClose = new Button(compoBottom, SWT.NONE);
		btnClose.setText("Close");
		btnClose.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				close();
			}
			public void widgetDefaultSelected(SelectionEvent event) { widgetSelected(event); }
		});
		fd = new FormData(100,25);
		fd.top = new FormAttachment(0, 8);
		fd.right = new FormAttachment(100, -10);
		btnClose.setLayoutData(fd);
		
		btnDoAction = new Button(compoBottom, SWT.NONE);
		btnDoAction.setEnabled(false);
		btnDoAction.setVisible(false);
		fd = new FormData(100,25);
		fd.top = new FormAttachment(0, 8);
		fd.right = new FormAttachment(btnClose, -10);
		btnDoAction.setLayoutData(fd);
		
		radioOption2 = new Button(compoBottom, SWT.RADIO);
		radioOption2.setBackground(COMPO_BACKGROUND_COLOR);
		radioOption2.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(btnDoAction, 0, SWT.CENTER);
		fd.right = new FormAttachment(btnDoAction, -20);
		radioOption2.setLayoutData(fd);
		radioOption2.addListener(SWT.Selection, new Listener() { public void handleEvent(Event event) { if ( connection.isConnected() && radioOption1.getSelection() ) connectedToDatabase(false); } });
		
		radioOption1 = new Button(compoBottom, SWT.RADIO);
		radioOption1.setBackground(COMPO_BACKGROUND_COLOR);
		radioOption1.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(btnDoAction, 0, SWT.CENTER);
		fd.right = new FormAttachment(radioOption2, -10);
		radioOption1.setLayoutData(fd);
		radioOption1.addListener(SWT.Selection, new Listener() { public void handleEvent(Event event) { if ( connection.isConnected() && radioOption2.getSelection() ) connectedToDatabase(false); } });
		
		lblOption = new Label(compoBottom, SWT.NONE);
		lblOption.setBackground(COMPO_BACKGROUND_COLOR);
		lblOption.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(btnDoAction, 0, SWT.CENTER);
		fd.right = new FormAttachment(radioOption1, -10);
		lblOption.setLayoutData(fd);

		dialog.open();
		dialog.layout();
	}
	
	/**
	 * Gets the list of configured databases, fill-in the comboDatabases and select the first-one
	 * @throws Exception 
	 */
	protected void getDatabases() throws Exception {
		databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore(includeNeo4j);
		if ( (databaseEntries == null) || (databaseEntries.size() == 0) ) {
			popup(Level.ERROR, "You haven't configure any database yet.\n\nPlease setup at least one database in the preferences.");
		} else {
			for (DBDatabaseEntry databaseEntry: databaseEntries) {
				comboDatabases.add(databaseEntry.getName());
			}
			comboDatabases.select(0);
			comboDatabases.notifyListeners(SWT.Selection, new Event());		// calls the databaseSelected() method
		}
	}
	
	/**
	 * Called when the user clicks on the "set preferences" button<br>
	 * This method opens up the database plugin preference page that the user can configure database details.
	 * @throws Exception 
	 */
	protected void setPreferences() throws Exception {
		if ( logger.isDebugEnabled() ) logger.debug("Openning preference page ...");
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "org.archicontribs.database.DBPreferencePage", null, null);
		dialog.setBlockOnOpen(true);
		if ( dialog.open() == 0 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Resetting settings from preferences ...");
			
			comboDatabases.removeAll();
			
			databaseEntries = DBDatabaseEntry.getAllDatabasesFromPreferenceStore(includeNeo4j);
			if ( (databaseEntries == null) || (databaseEntries.size() == 0) ) {
				comboDatabases.select(0);
				popup(Level.ERROR, "You won't be able to export until a database is configured in the preferences.");
			} else {
				for (DBDatabaseEntry databaseEntry: databaseEntries) {
					comboDatabases.add(databaseEntry.getName());
				}
				comboDatabases.select(0);
				comboDatabases.notifyListeners(SWT.Selection, new Event());
			}
		} else {
			if ( logger.isDebugEnabled() ) logger.debug("Preferences cancelled ...");
			if ( comboDatabases.getItemCount() == 0 )
				popup(Level.ERROR, "You won't be able to export until a database is configured in the preferences.");
		}
		comboDatabases.setFocus();
	}
	
	/**
	 * Listener called when a database is selected in the comboDatabases<br>
	 * This method retrieve the database name from the comboDatabases and reads the preferences to get the connection details. A connection is then established to the database.
	 */
	protected void databaseSelected() {
		popup("Please wait while connecting to the database ...");
		
		databaseSelectedCleanup();
		
		btnDoAction.setEnabled(false);
		
			// we get the databaseEntry corresponding to the selected combo entry
		selectedDatabase = databaseEntries.get(comboDatabases.getSelectionIndex());
		if ( logger.isDebugEnabled() ) logger.debug("selected database = " + selectedDatabase.getName()+" ("+selectedDatabase.getDriver()+", "+selectedDatabase.getServer()+", "+selectedDatabase.getPort()+", "+selectedDatabase.getDatabase()+", "+selectedDatabase.getUsername()+", "+selectedDatabase.getPassword()+")");
		
			// then we connect to the database.
		try {
			connection = new DBDatabaseConnection(selectedDatabase);
			//if the database connection failed, then an exception is raised, meaning that we get here only if the database connection succeeded
			if ( logger.isDebugEnabled() ) logger.debug("We are connected to the database.");
		} catch (Exception err) {
			closePopup();
			notConnectedToDatabase();
			popup(Level.ERROR, "Cannot connect to the database.", err);
			return;
		}
		
			// then, we check if the database has got the right pre-requisites
		try {
			connection.checkDatabase();
		} catch (Exception err) {
			closePopup();
			popup(Level.ERROR, "Cannot use this database.", err);
			notConnectedToDatabase();
			return;
		}
		
		connectedToDatabase(true);
		closePopup();
	}
	
	/** 
	 * This method is called by the databaseSelected method. It allows to do some actions when a new database is selected. 
	 */
	protected void databaseSelectedCleanup() {
		//to be overriden
	}
	
	/** 
	 * This method is called by the databaseSelected method. It allows to do some actions when the connection to a new database is successful.
	 * @param forceCheckDatabase true when the database should be checked. 
	 */
	protected void connectedToDatabase(boolean forceCheckDatabase) {
		// to be overriden
		enableOption();
		btnDoAction.setEnabled(true);
	}
	
	/** 
	 * This method is called by the databaseSelected method. It allows to do some actions when the connection to a new database is failed.
	 */
	protected void notConnectedToDatabase() {
		// to be overriden
		disableOption();
	}
	
	/** 
	 * Sets the reference of the online help
	 */
	protected void setHelpHref(String href) {
		HELP_HREF = href;
		btnHelp.setVisible(HELP_HREF != null);
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
			case Selected : icon = RIGHT_ARROW_ICON; break;
			case Running : icon = CLOCK_ICON; break;
			case Bypassed : icon = BYPASSED_ICON; break;
			case Ok : icon = OK_ICON; break;
			case Warn : icon = WARNING_ICON; break;
			case Error : icon = ERROR_ICON; break;
			default : icon = null;
		}
		switch ( action ) {
			case One : activeAction = ACTION.One; imgFirstAction.setImage(icon); break;
			case Two : activeAction = ACTION.Two; imgSecondAction.setImage(icon); break;
			case Three : activeAction = ACTION.Three; imgThirdAction.setImage(icon); break;
			case Four : activeAction = ACTION.Four; imgFourthAction.setImage(icon); break;
		}
	}
	
	/**
	 * Changes the status of the current action (on the left handside panel)
	 * @param status status of the action
	 */
	protected void setActiveAction(STATUS status) {
		if ( activeAction == null )
			activeAction = ACTION.One;
		setActiveAction(activeAction, status);
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
			case One : lblFirstAction.setText(label); break;
			case Two : lblSecondAction.setText(label); break;
			case Three : lblThirdAction.setText(label); break;
			case Four : lblFourthAction.setText(label); break;
		}
	}
	
	protected void setOption(boolean firstSelected) {
		radioOption1.setSelection(firstSelected == true);
		radioOption2.setSelection(firstSelected == false);
	}
	
	protected void setOption(String label, String option1, String toolTip1, String option2, String toolTip2, boolean firstSelected ) {
		if ( label != null ) lblOption.setText(label);
				
		if ( option1 != null ) radioOption1.setText(option1);
		if ( toolTip1 != null ) radioOption1.setToolTipText(toolTip1);
		radioOption1.setSelection(firstSelected == true);
		
		if ( option2 != null ) radioOption2.setText(option2);
		if ( toolTip2 != null ) radioOption2.setToolTipText(toolTip2);
		radioOption2.setSelection(firstSelected == false);
		
		compoBottom.layout();
		
		showOption();
		disableOption();
	}
	
	protected void enableOption() {
		lblOption.setEnabled(true);
		radioOption1.setEnabled(true);
		radioOption2.setEnabled(true);
	}
	
	protected void disableOption() {
		lblOption.setEnabled(false);
		radioOption1.setEnabled(false);
		radioOption2.setEnabled(false);
	}
	
	protected void hideOption() {
		lblOption.setVisible(false);
		radioOption1.setVisible(false);
		radioOption1.setVisible(false);
	}
	
	protected void showOption() {
		lblOption.setVisible(true);
		radioOption1.setVisible(true);
		radioOption2.setVisible(true);
	}
	
	/**
	 * Returns true if the first option is selected, false if the second option is selected
	 */
	protected boolean getOptionValue() {
		return radioOption1.getSelection();
	}
	
	
	
	
	private static Shell dialogShell = null;
	private static Label dialogLabel = null;
	/**
	 * shows up an on screen popup displaying the message but does not wait for any user input<br>
	 * it is the responsibility of the caller to dismiss the popup 
	 */
	public static Shell popup(String msg) {
	    logger.info(msg);

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if ( dialogShell == null ) {
					dialogShell = new Shell(display, SWT.BORDER | SWT.APPLICATION_MODAL);
					dialogShell.setSize(500, 70);
					dialogShell.setBackground(COMPO_LEFT_COLOR);
					dialogShell.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialogShell.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialogShell.getSize().y) / 4);
					dialogShell.setLayout(new GridLayout( 1, false ) );
					
					dialogLabel = new Label(dialogShell, SWT.CENTER | SWT.WRAP);
					dialogLabel.setBackground(COMPO_LEFT_COLOR);
					dialogLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, true, true ) );
					dialogLabel.setFont(TITLE_FONT);
				} else {
					restoreCursors();
				}
				
				dialogLabel.setText(msg);
				dialogShell.layout(true);
				dialogShell.open();
				
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
		if ( logger.isDebugEnabled() ) logger.debug("Setting arrow cursor");
		Map<Shell, Cursor> cursors = new HashMap<Shell, Cursor>();
		for ( Shell shell: display.getShells() ) {
			cursors.put(shell,  shell.getCursor());
			shell.setCursor(CURSOR_WAIT);
		}
		cursorsStack.push(cursors);
		refreshDisplay();
	}
	
	public static void restoreCursors() {
		if ( logger.isDebugEnabled() ) logger.debug("Restoring cursors");
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
		
		if ( e != null ) {
			if ( (e.getMessage()!=null) && !DBPlugin.areEqual(e.getMessage(), msg)) {
				popupMessage += "\n\n" + e.getMessage();
			} else {
				popupMessage += "\n\n" + e.getClass().getName();
			}
		}

		switch ( level.toInt() ) {
			case Level.FATAL_INT :
			case Level.ERROR_INT :
				MessageDialog.openError(display.getActiveShell(), DBPlugin.pluginTitle, popupMessage);
				break;
			case Level.WARN_INT :
				MessageDialog.openWarning(display.getActiveShell(), DBPlugin.pluginTitle, popupMessage);
				break;
			default :
				MessageDialog.openInformation(display.getActiveShell(), DBPlugin.pluginTitle, popupMessage);
				break;
		}
		
		refreshDisplay();
	}
	
	private static int questionResult;
	
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
		if ( logger.isDebugEnabled() ) logger.debug("question : "+msg);
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				//questionResult = MessageDialog.openQuestion(display.getActiveShell(), DBPlugin.pluginTitle, msg);
				MessageDialog dialog = new MessageDialog(display.getActiveShell(), DBPlugin.pluginTitle, null, msg, MessageDialog.QUESTION, buttonLabels, 0);
				questionResult = dialog.open();
			}
		});

		if ( logger.isDebugEnabled() ) logger.debug("answer : "+buttonLabels[questionResult]);
		return questionResult;
	}
	
	/**
	 * shows up an on screen popup with a progressbar<br>
	 * it is the responsibility of the caller to dismiss the popup 
	 */
	public static ProgressBar progressbarPopup(String msg) {
		if ( logger.isDebugEnabled() ) logger.debug("new progressbarPopup(\""+msg+"\")");
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setSize(600, 100);
		shell.setBackground(BLACK_COLOR);
		shell.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - shell.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - shell.getSize().y) / 4);
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setBackground(COMPO_LEFT_COLOR);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		label.setBackground(COMPO_LEFT_COLOR);
		label.setFont(TITLE_FONT);
		label.setText(msg);
		
		ProgressBar progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));
    	progressBar.setMinimum(0);
    	progressBar.setMaximum(100);
    	
    	shell.layout();
    	shell.open();
    	
    	refreshDisplay();
    	
    	return progressBar;
	}
	
	
	protected void hideGrpDatabase() {
		grpDatabase.setVisible(false);
	}
	
	protected void setBtnAction(String label, SelectionListener listener) {
		btnDoAction.setText(label);
		btnDoAction.addSelectionListener(listener);
		btnDoAction.setVisible(true);
	}
	
	/**
	 * Creates the progress bar that will allow to follow the export process
	 */
	protected void createProgressBar(String label, int min, int max) {
		if ( grpProgressBar == null ) {
			grpProgressBar = new Group(compoRightTop, SWT.NONE);
			grpProgressBar.setBackground(GROUP_BACKGROUND_COLOR);
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			fd.bottom = new FormAttachment(100);
			grpProgressBar.setLayoutData(fd);
			grpProgressBar.setLayout(new FormLayout());
			
			
			lblProgressBar = new Label(grpProgressBar, SWT.CENTER);
			lblProgressBar.setBackground(GROUP_BACKGROUND_COLOR);
			lblProgressBar.setFont(TITLE_FONT);
			lblProgressBar.setText(label);
			fd = new FormData();
			fd.top = new FormAttachment(0, -5);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			lblProgressBar.setLayoutData(fd);
			
			progressBar = new ProgressBar(grpProgressBar, SWT.NONE);
			fd = new FormData();
			fd.top = new FormAttachment(lblProgressBar);
			fd.left = new FormAttachment(25);
			fd.right = new FormAttachment(75);
			fd.height = 15;
			progressBar.setLayoutData(fd);
			progressBar.setMinimum(min);
			progressBar.setMaximum(max);
			
			compoRightTop.layout();
		} else {
			grpProgressBar.setVisible(true);
			resetProgressBar();
		}
	}
	
	/**
	 * Sets the min and max values of the progressBar and reset its selection to zero
	 */
	protected void setProgressBarMinAndMax(int min, int max) {
	    if ( logger.isTraceEnabled() ) logger.trace("Setting progressbar from "+min+" to "+max);
		progressBar.setMinimum(min); progressBar.setMaximum(max);
		resetProgressBar();
	}
	
	/**
	 * Resets the progressBar to zero in the SWT thread (thread safe method)
	 */
	protected void resetProgressBar() {
		progressBar.setSelection(0);
		refreshDisplay();
	}
	
	/**
	 * Increases the progressBar selection in the SWT thread (thread safe method)
	 */
	protected void increaseProgressBar() {
		progressBar.setSelection(progressBar.getSelection()+1);
		if ( logger.isTraceEnabled() ) logger.trace("progressBar : "+(progressBar.getSelection()+1)+"/"+progressBar.getMaximum());
		refreshDisplay();
	}
	
	/**
	 * Method used to close graphical objects if needed
	 */
	public void close() {
		dialog.dispose();
		dialog = null;
		
		if ( connection != null ) {
		    try {
		        connection.close();
		    } catch (SQLException e) { logger.error("Failed to close database connection", e); }
		    connection = null;
		}
		
		restoreCursors();
	}
	
	protected void fillInCompareTable(Tree tree, EObject memoryObject, int memoryObjectversion, HashMap<String, Object> databaseObject) {
	    fillInCompareTable(tree, null, memoryObject, memoryObjectversion, databaseObject);
	}
	
    protected void fillInCompareTable(Tree tree, TreeItem treeItem, EObject memoryObject, int memoryObjectversion, HashMap<String, Object> databaseObject) {
        assert ( memoryObject!=null || databaseObject!=null );

        if ( memoryObject == null )
            logger.debug("showing up database version of component "+(String)databaseObject.get("class")+":\""+(String)databaseObject.get("name")+"\"("+(String)databaseObject.get("id")+")");
        else if ( databaseObject == null )
            logger.debug("showing up memory version of component "+((IDBMetadata)memoryObject).getDBMetadata().getDebugName());
        else
            logger.debug("showing up memory and database versions of component "+((IDBMetadata)memoryObject).getDBMetadata().getDebugName());
        
		// we get the database version of the component
	    if ( databaseObject == null ) {
			try {
				databaseObject = connection.getObjectFromDatabase(memoryObject, memoryObjectversion);
			} catch (Exception err) {
				DBGui.popup(Level.ERROR, "Failed to get component "+((IDBMetadata)memoryObject).getDBMetadata().getDebugName()+" from the database.", err);
				//TODO: shall we exit to the status page with status=error ???
				return;
			}
	    }
	    
	    if ( treeItem == null ) {          // the root component
	        tree.removeAll();
	        refreshDisplay();
        
	        addItemToCompareTable(tree, treeItem, "Version", String.valueOf(((IDBMetadata)memoryObject).getDBMetadata().getInitialVersion()), String.valueOf(databaseObject.get("version")));
        
            if ( (String)databaseObject.get("created_by") != null ) {
                addItemToCompareTable(tree, treeItem, "Created by", ((IDBMetadata)memoryObject).getDBMetadata().getDatabaseCreatedBy(), (String)databaseObject.get("created_by"));
            }
        
            if ( databaseObject.get("created_on") != null ) {
                if ( ((IDBMetadata)memoryObject).getDBMetadata().getDatabaseCreatedOn() != null )
                    addItemToCompareTable(tree, treeItem, "Created on", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(((IDBMetadata)memoryObject).getDBMetadata().getDatabaseCreatedOn().getTime()), new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(databaseObject.get("created_on")));
                else
                    addItemToCompareTable(tree, treeItem, "Created on", "", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(databaseObject.get("created_on")));
            }
	    }
	    
	    addItemToCompareTable(tree, treeItem, "Class", memoryObject==null ? null : memoryObject.getClass().getSimpleName(), (String)databaseObject.get("class"));
	    addItemToCompareTable(tree, treeItem, "Name", memoryObject==null ? null : ((INameable)memoryObject).getName(), (String)databaseObject.get("name"));
	    
	    if ( memoryObject instanceof IDocumentable )					addItemToCompareTable(tree, treeItem, "Documentation", memoryObject==null ? null : ((IDocumentable)memoryObject).getDocumentation(), (String)databaseObject.get("documentation"));
	    
	    if ( memoryObject instanceof IJunction )						addItemToCompareTable(tree, treeItem, "Type", memoryObject==null ? null : ((IJunction)memoryObject).getType(), (String)databaseObject.get("type"));
		if ( memoryObject instanceof IArchimateRelationship ) {		    addItemToCompareTable(tree, treeItem, "Source id", memoryObject==null ? null : ((IArchimateRelationship)memoryObject).getSource().getId(), (String)databaseObject.get("source_id"));
																	    addItemToCompareTable(tree, treeItem, "Target id", memoryObject==null ? null : ((IArchimateRelationship)memoryObject).getTarget().getId(), (String)databaseObject.get("target_id"));
			if ( memoryObject instanceof IInfluenceRelationship )		addItemToCompareTable(tree, treeItem, "Strength", memoryObject==null ? null : ((IInfluenceRelationship)memoryObject).getStrength(), (String)databaseObject.get("strength"));
			if ( memoryObject instanceof IAccessRelationship )			addItemToCompareTable(tree, treeItem, "Access type", memoryObject==null ? null : String.valueOf(((IAccessRelationship)memoryObject).getAccessType()), String.valueOf((int)databaseObject.get("access_type")));
		}
		if ( memoryObject instanceof IFolder )							addItemToCompareTable(tree, treeItem, "Folder type", memoryObject==null ? null : ((IFolder)memoryObject).getType().getLiteral(), FolderType.get((int)databaseObject.get("type")).getLiteral());
		if ( memoryObject instanceof IArchimateDiagramModel )			addItemToCompareTable(tree, treeItem, "Viewpoint", memoryObject==null ? null : ((IArchimateDiagramModel)memoryObject).getViewpoint(), (String)databaseObject.get("viewpoint"));
		if ( memoryObject instanceof IDiagramModel )					addItemToCompareTable(tree, treeItem, "Router type", memoryObject==null ? null : String.valueOf(((IDiagramModel)memoryObject).getConnectionRouterType()), databaseObject.get("connection_router_type")==null ? null : String.valueOf((int)databaseObject.get("connection_router_type")));
		if ( memoryObject instanceof IBorderObject )					addItemToCompareTable(tree, treeItem, "Border color", memoryObject==null ? null : ((IBorderObject)memoryObject).getBorderColor(), (String)databaseObject.get("border_color"));
		if ( memoryObject instanceof IDiagramModelNote )				addItemToCompareTable(tree, treeItem, "Border type", memoryObject==null ? null : String.valueOf(((IDiagramModelNote)memoryObject).getBorderType()), databaseObject.get("border_type")==null ? null : String.valueOf((int)databaseObject.get("border_type")));
		if ( memoryObject instanceof IConnectable ) {
			// TODO: get source and target connections from database and compare then to component's ones
			/*
			for ( IDiagramModelConnection conn: ((IConnectable)component).getSourceConnections() )
				addItemToCompareTable(table, level, "source connections", conn.getId(), (String)hashResult.get("xxxxx"));
			for ( IDiagramModelConnection conn: ((IConnectable)component).getTargetConnections() )
				addItemToCompareTable(table, level, "target connections", conn.getId(), (String)hashResult.get("xxxxx"));
			*/
		}
		if ( memoryObject instanceof IDiagramModelArchimateObject )		addItemToCompareTable(tree, treeItem, "Type", memoryObject==null ? null : String.valueOf(((IDiagramModelArchimateObject)memoryObject).getType()), databaseObject.get("type")==null ? null : String.valueOf((int)databaseObject.get("type")));
		if ( memoryObject instanceof IDiagramModelImageProvider )		addItemToCompareTable(tree, treeItem, "Image path", memoryObject==null ? null : ((IDiagramModelImageProvider)memoryObject).getImagePath(), (String)databaseObject.get("image_path"));
		if ( memoryObject instanceof IDiagramModelObject ) {			addItemToCompareTable(tree, treeItem, "Fill color", memoryObject==null ? null : ((IDiagramModelObject)memoryObject).getFillColor(), (String)databaseObject.get("fill_color"));
																		IBounds bounds = memoryObject==null ? null : ((IDiagramModelObject)memoryObject).getBounds();
																		addItemToCompareTable(tree, treeItem, "X", memoryObject==null ? null : String.valueOf(bounds.getX()), databaseObject.get("x") == null ? null : String.valueOf((int)databaseObject.get("x")));
																		addItemToCompareTable(tree, treeItem, "Y", memoryObject==null ? null : String.valueOf(bounds.getY()), databaseObject.get("y")==null ? null : String.valueOf((int)databaseObject.get("y")));
																		addItemToCompareTable(tree, treeItem, "Width" ,memoryObject==null ? null : String.valueOf(bounds.getWidth()), databaseObject.get("width")==null ? null : String.valueOf((int)databaseObject.get("width")));
																		addItemToCompareTable(tree, treeItem, "Height", memoryObject==null ? null : String.valueOf(bounds.getHeight()), databaseObject.get("height")==null ? null : String.valueOf((int)databaseObject.get("height")));
		}
		if ( memoryObject instanceof IDiagramModelArchimateComponent )	addItemToCompareTable(tree, treeItem, "Archimate concept", memoryObject==null ? null : ((IDiagramModelArchimateComponent)memoryObject).getArchimateConcept().getId(), (String)databaseObject.get("element_id"));
		if ( memoryObject instanceof IDiagramModelArchimateConnection )	addItemToCompareTable(tree, treeItem, "Archimate concept", memoryObject==null ? null : ((IDiagramModelArchimateConnection)memoryObject).getArchimateConcept().getId(), (String)databaseObject.get("element_id"));
		if ( memoryObject instanceof IFontAttribute ) {					addItemToCompareTable(tree, treeItem, "Font", memoryObject==null ? null : ((IFontAttribute)memoryObject).getFont(), (String)databaseObject.get("font"));
																		addItemToCompareTable(tree, treeItem, "Font color", memoryObject==null ? null : ((IFontAttribute)memoryObject).getFontColor(), (String)databaseObject.get("font color"));
		}
		if ( memoryObject instanceof ILineObject ) {					addItemToCompareTable(tree, treeItem, "Line width", memoryObject==null ? null : String.valueOf(((ILineObject)memoryObject).getLineWidth()), databaseObject.get("line_width")==null ? null : String.valueOf((int)databaseObject.get("line_width")));
																		addItemToCompareTable(tree, treeItem, "Line color", memoryObject==null ? null : ((ILineObject)memoryObject).getLineColor(), (String)databaseObject.get("line_color"));
		}
		if ( memoryObject instanceof ILockable )						addItemToCompareTable(tree, treeItem, "Lockable", memoryObject==null ? null : String.valueOf(((ILockable)memoryObject).isLocked()), databaseObject.get("lockable")==null ? null : String.valueOf((boolean)databaseObject.get("lockable")));
		if ( memoryObject instanceof ISketchModel )						addItemToCompareTable(tree, treeItem, "Background", memoryObject==null ? null : String.valueOf(((ISketchModel)memoryObject).getBackground()), databaseObject.get("background")==null ? null : String.valueOf((int)databaseObject.get("background")));
		if ( memoryObject instanceof ITextAlignment )					addItemToCompareTable(tree, treeItem, "Text alignment", memoryObject==null ? null : String.valueOf(((ITextAlignment)memoryObject).getTextAlignment()), databaseObject.get("text_alignment")==null ? null : String.valueOf((int)databaseObject.get("text_alignment")));
        if ( memoryObject instanceof ITextPosition )					addItemToCompareTable(tree, treeItem, "Text position", memoryObject==null ? null : String.valueOf(((ITextPosition)memoryObject).getTextPosition()), databaseObject.get("text_position")==null ? null : String.valueOf((int)databaseObject.get("text_position")));
		if ( memoryObject instanceof ITextContent )						addItemToCompareTable(tree, treeItem, "Content", memoryObject==null ? null : ((ITextContent)memoryObject).getContent(), (String)databaseObject.get("content"));
		if ( memoryObject instanceof IHintProvider )	{				addItemToCompareTable(tree, treeItem, "Hint title", memoryObject==null ? null : ((IHintProvider)memoryObject).getHintTitle(), (String)databaseObject.get("hint_title"));
																		addItemToCompareTable(tree, treeItem, "Hint content", memoryObject==null ? null : ((IHintProvider)memoryObject).getHintContent(), (String)databaseObject.get("hint_content"));
		}
		/* NOT EXPORTED YET
		if ( component instanceof IHelpHintProvider ) {					addItemToCompareTable(table, treeItem, "help hint title", component==null ? null : ((IHelpHintProvider)component).getHelpHintTitle(), (String)hashResult.get("xxxxx"));
																		addItemToCompareTable(table, treeItem, "help hint content", component==null ? null : ((IHelpHintProvider)component).getHelpHintContent(), (String)hashResult.get("xxxxx"));
		}
		*/
		if ( memoryObject instanceof IIconic )							addItemToCompareTable(tree, treeItem, "Image position", memoryObject==null ? null : String.valueOf(((IIconic)memoryObject).getImagePosition()), databaseObject.get("image_position")==null ? null : String.valueOf((int)databaseObject.get("image_position")));
		if ( memoryObject instanceof INotesContent )					addItemToCompareTable(tree, treeItem, "Notes", memoryObject==null ? null : ((INotesContent)memoryObject).getNotes(), (String)databaseObject.get("notes"));
	    refreshDisplay();
		if ( memoryObject instanceof IDiagramModelConnection ) {
			addItemToCompareTable(tree, treeItem, "Type", memoryObject==null ? null : String.valueOf(((IDiagramModelConnection)memoryObject).getType()), databaseObject.get("type")==null ? null : String.valueOf((int)databaseObject.get("type")));			// we do not use getText as it is deprecated
			addItemToCompareTable(tree, treeItem, "Text position : ", memoryObject==null ? null : String.valueOf(((IDiagramModelConnection)memoryObject).getTextPosition()), databaseObject.get("text_position")==null ? null : String.valueOf((int)databaseObject.get("text_position")));
			
			// we show up the bendpoints only if they both exist
			if ( memoryObject != null && databaseObject.containsKey("bendpoints") ) {
				if ( (((IDiagramModelConnection)memoryObject).getBendpoints().size() != 0) || (((Integer[][])databaseObject.get("bendpoints")).length != 0) ) {
				    TreeItem bendpointsTreeItem;
	                if ( treeItem == null )
	                    bendpointsTreeItem = new TreeItem(tree, SWT.NONE);
	                else
	                    bendpointsTreeItem = new TreeItem(treeItem, SWT.NONE);
				    bendpointsTreeItem.setText("Bendpoints");
				    bendpointsTreeItem.setExpanded(false);
					// we get a sorted list of component's bendpoints
					Integer[][] componentBendpoints = new Integer[((IDiagramModelConnection)memoryObject).getBendpoints().size()][4];
					for (int i = 0; i < ((IProperties)memoryObject).getProperties().size(); ++i) {
						componentBendpoints[i] = new Integer[] { ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getStartX(), ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getStartY(), ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getEndX(), ((IDiagramModelConnection)memoryObject).getBendpoints().get(i).getEndY() };
					}
					Arrays.sort(componentBendpoints, integerComparator);
			
					// we get a sorted list of properties from the database
					Integer[][] databaseBendpoints = (Integer[][])databaseObject.get("bendpoints");
					if ( databaseBendpoints == null ) databaseBendpoints = new Integer[0][0];			// just because it must not be null
					Arrays.sort(databaseBendpoints, integerComparator);
			
					int indexComponent = 0;
					int indexDatabase = 0;
					while ( (indexComponent < componentBendpoints.length) || (indexDatabase < databaseBendpoints.length) ) {
					    TreeItem subTreeItem = new TreeItem(bendpointsTreeItem, SWT.NONE);
					    subTreeItem.setText("Bendpoint "+Math.max(indexComponent, indexDatabase)+1);
					    subTreeItem.setExpanded(false);
						if ( indexComponent >= componentBendpoints.length ) {			// only the database has got the property
							addItemToCompareTable(tree, subTreeItem, "Start X", null, String.valueOf(databaseBendpoints[indexDatabase][0]));
							addItemToCompareTable(tree, subTreeItem, "Start Y", null, String.valueOf(databaseBendpoints[indexDatabase][1]));
							addItemToCompareTable(tree, subTreeItem, "End X", null, String.valueOf(databaseBendpoints[indexDatabase][2]));
							addItemToCompareTable(tree, subTreeItem, "End Y", null, String.valueOf(databaseBendpoints[indexDatabase][3]));
							++indexDatabase;
						} else if ( indexDatabase >= databaseBendpoints.length ) {		// only the component has got the property
							addItemToCompareTable(tree, subTreeItem, "Start X", String.valueOf(componentBendpoints[indexComponent][0]), null);
							addItemToCompareTable(tree, subTreeItem, "Start Y", String.valueOf(componentBendpoints[indexComponent][1]), null);
							addItemToCompareTable(tree, subTreeItem, "End X", String.valueOf(componentBendpoints[indexComponent][2]), null);
							addItemToCompareTable(tree, subTreeItem, "End Y", String.valueOf(componentBendpoints[indexComponent][3]), null);
							++indexComponent;
						} else {
							addItemToCompareTable(tree, subTreeItem, "Start X", String.valueOf(componentBendpoints[indexComponent][0]), String.valueOf(databaseBendpoints[indexDatabase][0]));
							addItemToCompareTable(tree, subTreeItem, "Start Y", String.valueOf(componentBendpoints[indexComponent][1]), String.valueOf(databaseBendpoints[indexDatabase][1]));
							addItemToCompareTable(tree, subTreeItem, "End X", String.valueOf(componentBendpoints[indexComponent][2]), String.valueOf(databaseBendpoints[indexDatabase][2]));
							addItemToCompareTable(tree, subTreeItem, "End Y", String.valueOf(componentBendpoints[indexComponent][3]), String.valueOf(databaseBendpoints[indexDatabase][3]));
							++indexComponent;
							++indexDatabase;
						}
					}
				}
			}
		}
		refreshDisplay();
		
		// we show up the properties if both exist
		if ( memoryObject != null && databaseObject.containsKey("properties") ) {
			if ( memoryObject instanceof IProperties && ((IProperties)memoryObject).getProperties().size() != 0) {
                TreeItem propertiesTreeItem;
                if ( treeItem == null )
                    propertiesTreeItem = new TreeItem(tree, SWT.NONE);
                else
                    propertiesTreeItem = new TreeItem(treeItem, SWT.NONE);
                propertiesTreeItem.setText("Properties");
                propertiesTreeItem.setExpanded(false);
				
				// we get a sorted list of component's properties
				String[][] componentProperties = new String[((IProperties)memoryObject).getProperties().size()][2];
				for (int i = 0; i < ((IProperties)memoryObject).getProperties().size(); ++i) {
					componentProperties[i] = new String[] { ((IProperties)memoryObject).getProperties().get(i).getKey(), ((IProperties)memoryObject).getProperties().get(i).getValue() };
				}
				Arrays.sort(componentProperties, stringComparator);
		
				// we get a sorted list of properties from the database
				String[][] databaseProperties = (String[][])databaseObject.get("properties");
				Arrays.sort(databaseProperties, stringComparator);
		
				int indexComponent = 0;
				int indexDatabase = 0;
				int compare;
				while ( (indexComponent < componentProperties.length) || (indexDatabase < databaseProperties.length) ) {
					if ( indexComponent >= componentProperties.length )
						compare = 1;
					else {
						if ( indexDatabase >= databaseProperties.length )
							compare = -1;
						else
							compare = DBPlugin.collator.compare(componentProperties[indexComponent][0], databaseProperties[indexDatabase][0]);
					}
		
					if ( compare == 0 ) {				// both have got the same property
						addItemToCompareTable(tree, propertiesTreeItem, componentProperties[indexComponent][0], componentProperties[indexComponent][1], databaseProperties[indexDatabase][1]);
						++indexComponent;
						++indexDatabase;
					} else if ( compare < 0 ) {			// only the component has got the property
						addItemToCompareTable(tree, propertiesTreeItem, componentProperties[indexComponent][0], componentProperties[indexComponent][1], null);
						++indexComponent;
					} else {							// only the database has got the property
						addItemToCompareTable(tree, propertiesTreeItem, componentProperties[indexDatabase][0], null, databaseProperties[indexDatabase][1]);
						++indexDatabase;
					}
				}
			}
		}
		
	    addItemToCompareTable(tree, treeItem, "Checksum", memoryObject==null ? null : ((IDBMetadata)memoryObject).getDBMetadata().getCurrentChecksum(), (String)databaseObject.get("checksum"));
		
		// we show up the children if both exist
		if ( memoryObject != null && databaseObject.containsKey("children") ) {
			if ( memoryObject instanceof IDiagramModelContainer ) {
                TreeItem childrenTreeItem;
                if ( treeItem == null )
                    childrenTreeItem = new TreeItem(tree, SWT.NONE);
                else
                    childrenTreeItem = new TreeItem(treeItem, SWT.NONE);
                childrenTreeItem.setText("Children");
                childrenTreeItem.setExpanded(false);
				
				//TODO : sort the arrays, as it is done for the properties.
				//TODO : then we can explain that if a child checksum is different, then it comes from the connections and if all the children are the same and the checksum is different, then it comes from the objects 
				
				int i = 0;
				while ( (i < ((IDiagramModelContainer)memoryObject).getChildren().size()) || (i < ((HashMap[])databaseObject.get("children")).length) ) {
					IDiagramModelObject child = (i<((IDiagramModelContainer)memoryObject).getChildren().size() ? ((IDiagramModelContainer)memoryObject).getChildren().get(i) : null);
                    TreeItem childTreeItem = new TreeItem(childrenTreeItem, SWT.NONE);
                    childTreeItem.setText("Child "+i+1);
                    childTreeItem.setExpanded(false);
					@SuppressWarnings("unchecked")
					HashMap<String, Object> result = ((HashMap<String, Object>[])databaseObject.get("children"))[i];
					fillInCompareTable(tree, childTreeItem, child, memoryObjectversion, result);
					if ( childTreeItem.getBackground().equals(DBGui.LIGHT_RED_COLOR))
					    childrenTreeItem.setBackground(DBGui.LIGHT_RED_COLOR);
					++i;
				}
			}
		}
	    refreshDisplay();
	}
	
	Comparator<String[]> stringComparator = new Comparator<String[]>() {
		public int compare(final String[] row1, final String[] row2) {
			return DBPlugin.collator.compare(row1[0],row2[0]);
		}
	};
	
	Comparator<Integer[]> integerComparator = new Comparator<Integer[]>() {
		public int compare(final Integer[] row1, final Integer[] row2) {
			return DBPlugin.collator.compare(row1[0],row2[0]);
		}
	};
	
	
	/**
	 * Helper function to fill in the compareTable
	 * @return true if col2 and col3 are equals, false if they differ 
	 */
    private void addItemToCompareTable(Tree tree, TreeItem treeItem, String col1, String col2, String col3) {
    	TreeItem subTreeItem;
    	
    	if ( treeItem != null ) 
    	    subTreeItem = new TreeItem(treeItem, SWT.NULL);
    	else
    	    subTreeItem = new TreeItem(tree, SWT.NONE);
    	
    	subTreeItem.setText(new String[] {col1, col2, col3 });
		if ( !DBPlugin.areEqual(col2, col3) ) {
		    if ( treeItem != null )
		        treeItem.setBackground(DBGui.LIGHT_RED_COLOR);
			subTreeItem.setBackground(DBGui.LIGHT_RED_COLOR);
		}
		
		refreshDisplay();
    }
    
    protected void setMessage(String message, Color foreground) {
    	Label label = new Label(compoRightTop, SWT.VERTICAL | SWT.CENTER);
        label.setFont(GROUP_TITLE_FONT);
        label.setBackground(foreground);
        message = message.replace("\n\n", "\n");
        
        if ( foreground == GREEN_COLOR )
        	logger.info(message);
        else
        	logger.error(message);
        
        if ( message.split("\n").length == 1 )
            message = "\n" + message;               // we try to vertically center it, more or less ...
        label.setText(message);

        FormData fd = new FormData();
        fd.top = new FormAttachment(0, 0);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        label.setLayoutData(fd);
        
        compoRightTop.layout();
        refreshDisplay();
    }
    
    
    private static byte[] viewImage = null;
    public static byte[] createImage(IDiagramModel view, double scale, int margin) {
    	if ( logger.isDebugEnabled() ) logger.debug(DBGui.class, "Creating image from view");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream writeOut = new DataOutputStream(out);
		ImageLoader saver = new ImageLoader();
		Image image = DiagramUtils.createImage(view, scale, margin);
		saver.data = new ImageData[] { image.getImageData() };
		saver.save(writeOut, SWT.IMAGE_PNG);
		image.dispose();
		try {
			writeOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		viewImage = out.toByteArray();

		return viewImage;
    }
    
    public static void disposeImage() {
    	viewImage = null;
    }
	
	/**
	 * Refreshes the display
	 */
	public static void refreshDisplay() {
		while ( Display.getCurrent().readAndDispatch() ) 
			;
	}
}
