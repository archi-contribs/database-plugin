package org.archicontribs.database.GUI;

import java.awt.Toolkit;
import java.util.List;
import org.apache.log4j.Level;
import org.archicontribs.database.DBDatabase;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class DBGui {
	protected static final DBLogger logger = new DBLogger(DBGui.class);
	
	protected List<DBDatabase> databaseEntries;
	protected DBDatabase database;
	
	private String HELP_HREF = null;
	private boolean overHelp = false;
	
	protected enum ACTION {One, Two, Three, Four};
	protected enum STATUS {Empty, Selected, Running, Bypassed, Ok, Warn, Error};
	
	public static final Color LIGHT_GREEN_COLOR = new Color(null, 204, 255, 229);
	public static final Color LIGHT_RED_COLOR = new Color(null, 255, 230, 230);
	public static final Color RED_COLOR = new Color(null, 240, 0, 0);
	public static final Color WHITE_COLOR = new Color(null, 220,220,220);
	public static final Color GREY_COLOR = new Color(null, 100, 100, 100);
	public static final Color BLACK_COLOR = new Color(null, 0, 0, 0);
	
	public static final Color COMPO_LEFT_COLOR = new Color(null, 240, 248, 255);		// light blue
	public static final Color COMPO_BACKGROUND_COLOR = new Color(null, 250, 250, 250);	// light grey
	public static final Color GROUP_BACKGROUND_COLOR = new Color(null, 235, 235, 235);	// light grey (a bit darker than compo background)
	public static final Color TABLE_BACKGROUND_COLOR = GROUP_BACKGROUND_COLOR;
	public static final Color HIGHLIGHTED_COLOR = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
	
	public static final Color STRATEGY_COLOR = new Color(null, 255, 222, 170);
	public static final Color BUSINESS_COLOR = new Color(null, 255, 255, 181);
	public static final Color APPLICATION_COLOR = new Color(null, 181, 255, 255);
	public static final Color TECHNOLOGY_COLOR = new Color(null, 201, 231, 183);
	public static final Color PHYSICAL_COLOR = new Color(null, 201, 231, 183);
	public static final Color IMPLEMENTATION_COLOR = new Color(null, 255, 224, 224);
	public static final Color MOTIVATION_COLOR = new Color(null, 204, 204, 255);
	
	public static final Color PASSIVE_COLOR = new Color(null, 250, 250, 250);
	
	public static final Font GROUP_TITLE_FONT = new Font(null, "Segoe UI", 10, SWT.BOLD | SWT.ITALIC);
	public static final Font TITLE_FONT = new Font(null, "Segoe UI", 10, SWT.BOLD);
	public static final Font BOLD_FONT = new Font(null, "Segoe UI", 8, SWT.BOLD);

	public static final Image LOGO_IMAGE = new Image(null, DBGui.class.getResourceAsStream("/img/logo.png"));
	public static final Image EXPORT_TO_DATABASE_IMAGE = new Image(null, DBGui.class.getResourceAsStream("/img/22x22/export.png"));
	public static final Image IMPORT_FROM_DATABASE_IMAGE = new Image(null, DBGui.class.getResourceAsStream("/img/22x22/import.png"));
	
	public static final Image BYPASSED_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/bypassed.png"));
	public static final Image CLOCK_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/clock.png"));
	public static final Image ERROR_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/error.png"));
	public static final Image WARNING_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/warning.png"));
	public static final Image OK_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/ok.png"));
	public static final Image RIGHT_ARROW_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/right_arrow.png"));
	
	public static final Image LOCK_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/lock.png"));
	public static final Image UNLOCK_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/10x10/unlock.png"));
	
	public static final Image HELP_ICON = new Image(null, DBGui.class.getResourceAsStream("/img/28x28/help.png"));
	
	public static final Image GREY_BACKGROUND = new Image(null, DBGui.class.getResourceAsStream("/img/grey.png"));
	
	protected static Display display = Display.getDefault();
	protected Shell dialog;
	
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

		dialog = new Shell(display, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText(DBPlugin.pluginTitle + " - " + title);
		dialog.setMinimumSize(750, 550);
		dialog.setSize(800,600);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 4);
		dialog.setLayout(new FormLayout());
		
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
			public void widgetSelected(SelectionEvent e) { setPreferences(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
		});
		fd = new FormData();
		fd.top = new FormAttachment(lblRegisteredDatabases, 0, SWT.CENTER);
		fd.right = new FormAttachment(100, -10);
		btnSetPreferences.setLayoutData(fd);
		
		comboDatabases = new Combo(grpDatabase, SWT.NONE | SWT.READ_ONLY);
		comboDatabases.setBackground(GROUP_BACKGROUND_COLOR);
		comboDatabases.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { databaseSelected(); }
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
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
		btnHelp.addListener(SWT.MouseEnter, new Listener() { @Override public void handleEvent(Event event) { overHelp = true; btnHelp.redraw(); } });
		btnHelp.addListener(SWT.MouseExit, new Listener() { @Override public void handleEvent(Event event) { overHelp = false; btnHelp.redraw(); } });
		btnHelp.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e)
            {
                 if ( overHelp ) e.gc.drawRoundRectangle(0, 0, 29, 29, 10, 10);
                 e.gc.drawImage(HELP_ICON, 2, 2);
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
			public void widgetSelected(SelectionEvent e) {
				close();
			}
			public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
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
		
		radioOption1 = new Button(compoBottom, SWT.RADIO);
		radioOption1.setBackground(COMPO_BACKGROUND_COLOR);
		radioOption1.setVisible(false);
		fd = new FormData();
		fd.top = new FormAttachment(btnDoAction, 0, SWT.CENTER);
		fd.right = new FormAttachment(radioOption2, -10);
		radioOption1.setLayoutData(fd);
		
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
	 */
	protected void getDatabases() {
		databaseEntries = DBDatabase.getAllDatabasesFromPreferenceStore();
		if ( (databaseEntries == null) || (databaseEntries.size() == 0) ) {
			popup(Level.ERROR, "You haven't configure any database yet.\n\nPlease setup at least one database in the preferences.");
		} else {
			for (DBDatabase databaseEntry: databaseEntries) {
				display.asyncExec (new Runnable () {
					public void run () {
						comboDatabases.add(databaseEntry.getName());
					}
				});
			}
			display.asyncExec (new Runnable () {
				public void run () {
					comboDatabases.select(0);
					comboDatabases.notifyListeners(SWT.Selection, new Event());		// calls the databaseSelected() method
				}
			});
		}
	}
	
	/**
	 * Called when the user clicks on the "set preferences" button<br>
	 * This method opens up the database plugin preference page that the user can configure database details.
	 */
	protected void setPreferences() {
		if ( logger.isDebugEnabled() ) logger.debug("Openning preference page ...");
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "org.archicontribs.database.DBPreferencePage", null, null);
		dialog.setBlockOnOpen(true);
		if ( dialog.open() == 0 ) {
			if ( logger.isDebugEnabled() ) logger.debug("Resetting settings from preferences ...");
			
			comboDatabases.removeAll();
			
			databaseEntries = DBDatabase.getAllDatabasesFromPreferenceStore();
			if ( (databaseEntries == null) || (databaseEntries.size() == 0) ) {
				comboDatabases.select(0);
				popup(Level.ERROR, "You won't be able to export until a database is configured in the preferences.");
			} else {
				for (DBDatabase databaseEntry: databaseEntries) {
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
		database = databaseEntries.get(comboDatabases.getSelectionIndex());
		setOption(database.getExportWholeModel());
		if ( logger.isDebugEnabled() ) logger.debug("selected database = " + database.getName()+" ("+database.getDriver()+", "+database.getServer()+", "+database.getPort()+", "+database.getDatabase()+", "+database.getUsername()+", "+database.getPassword()+")");
		
			// then we connect to the database.
		try {
			database.openConnection();
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
			database.check();
		} catch (Exception err) {
			closePopup();
			notConnectedToDatabase();
			popup(Level.ERROR, "Cannot use this database.", err);
			return;
		}
		
		connectedToDatabase();
		closePopup();
	}
	
	protected void databaseSelectedCleanup() {
		//to be overriden
	}
	
	protected void connectedToDatabase() {
		// to be overriden
		btnDoAction.setEnabled(true);
	}
	
	protected void notConnectedToDatabase() {
		// to be overriden
	}
	
	protected void setHelpHref(String href) {
		HELP_HREF = href;
		btnHelp.setVisible(HELP_HREF != null);
	}
	
	private ACTION activeAction = null;
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
	
	protected void setActiveAction(STATUS status) {
		if ( activeAction == null )
			activeAction = ACTION.One;
		setActiveAction(activeAction, status);
	}
	
	protected void setActiveAction(ACTION action) {
		setActiveAction(action, STATUS.Selected);
	}
	
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
	}
	
	protected void hideOption() {
		lblOption.setVisible(false);
		radioOption1.setVisible(false);
		radioOption1.setVisible(false);
	}
	
	protected void disableOption() {
		radioOption1.setEnabled(false);
		radioOption2.setEnabled(false);
	}
	
	protected void showOption() {
		lblOption.setVisible(true);
		radioOption1.setVisible(true);
		radioOption1.setEnabled(true);
		radioOption2.setVisible(true);
		radioOption2.setEnabled(true);
	}
	
	/**
	 * Returns true if the first option is selected, false if the second option is selected
	 */
	private boolean _selection;	
	protected boolean getOptionValue() {
		display.syncExec(new Runnable() { @Override public void run() { _selection = radioOption1.getSelection(); } });
		return _selection;
	}
	
	
	
	
	/**
	 * shows up an on screen popup displaying the message but does not wait for any user input<br>
	 * it is the responsibility of the caller to dismiss the popup 
	 */
	private static Shell dialogShell = null;
	private static Label dialogLabel = null;
	public static Shell popup(String msg) {
		if ( dialogShell == null ) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					dialogShell = new Shell(display, SWT.BORDER | SWT.APPLICATION_MODAL);
					dialogShell.setSize(400, 50);
					dialogShell.setBackground(BLACK_COLOR);
					dialogShell.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialogShell.getSize().x) / 4, (Toolkit.getDefaultToolkit().getScreenSize().height - dialogShell.getSize().y) / 4);
					FillLayout layout = new FillLayout();
					layout.marginWidth = 2;
					layout.marginHeight = 2;
					dialogShell.setLayout(layout);
					
					Composite composite = new Composite(dialogShell, SWT.NONE);
					composite.setBackground(COMPO_LEFT_COLOR);
					composite.setLayout( new GridLayout( 1, false ) );
					
					dialogLabel = new Label(composite, SWT.NONE);
					dialogLabel.setBackground(COMPO_LEFT_COLOR);
					dialogLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, true, true ) );
					dialogLabel.setFont(TITLE_FONT);
				}
			});
		}
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				for ( Shell shell: display.getShells() ) {
					shell.setCursor(DBPlugin.CURSOR_WAIT);
				}
				dialogShell.setText(msg);
				dialogLabel.setText(msg);
				dialogShell.open();
			}
		});
		return dialogShell;
	}
	
	public static void closePopup() {
		if ( dialogShell != null ) {
    		Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					dialogShell.close();
					dialogShell = null;
					for ( Shell shell: display.getShells() ) {
						shell.setCursor(DBPlugin.CURSOR_ARROW);
					}
				}
    		});
		}
	}
	
	/**
	 * Shows up an on screen popup displaying the message and wait for the user to click on the "OK" button
	 */
	public static void popup(Level level, String msg) {
		popup(level,msg,null);
	}
	
	// the popupMessage is a class variable because it will be used in an asyncExec() method.
	private static String popupMessage;
	/**
	 * Shows up an on screen popup, displaying the message (and the exception message if any) and wait for the user to click on the "OK" button<br>
	 * The exception stacktrace is also printed on the standard error stream
	 */
	public static void popup(Level level, String msg, Exception e) {
		popupMessage = msg;
		logger.log(DBGui.class, level, msg, e);
		
		if ( e != null ) {
			if ( (e.getMessage()!=null) && !e.getMessage().equals(msg)) {
				popupMessage += "\n\n" + e.getMessage();
			} else {
				popupMessage += "\n\n" + e.getClass().getName();
			}
		}
		//TODO : in case a exception is provided : use multistatus instead
		/*
		        private static MultiStatus createMultiStatus(String msg, Throwable t) {

                List<Status> childStatuses = new ArrayList<>();
                StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();

                 for (StackTraceElement stackTrace: stackTraces) {
                        Status status = new Status(IStatus.ERROR,
                                        "com.example.e4.rcp.todo", stackTrace.toString());
                        childStatuses.add(status);
                }

                MultiStatus ms = new MultiStatus("com.example.e4.rcp.todo",
                                IStatus.ERROR, childStatuses.toArray(new Status[] {}),
                                t.toString(), t);
                return ms;
        }
		 */

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				switch ( level.toInt() ) {
					case Level.FATAL_INT :
					case Level.ERROR_INT :
						MessageDialog.openError(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, popupMessage);
						break;
					case Level.WARN_INT :
						MessageDialog.openWarning(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, popupMessage);
						break;
					default :
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, popupMessage);
						break;
				}
			}
		});
	}
	
	/**
	 * Shows up an on screen popup displaying the question (and the exception message if any)  and wait for the user to click on the "YES" or "NO" button<br>
	 * The exception stacktrace is also printed on the standard error stream
	 * 
	 * @param msg
	 * @return true or false
	 */
	public static boolean question(String msg) {
		if ( logger.isDebugEnabled() ) logger.debug("question : "+msg);
		boolean result = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), DBPlugin.pluginTitle, msg);
		if ( logger.isDebugEnabled() ) logger.debug("answer : "+result);
		return result;
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
	protected void createProgressBar() {
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
		lblProgressBar.setText("Exporting components ...");
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
		
		compoRightTop.layout();
	}
	
	/**
	 * Sets the min and max values of the progressBar and reset its selection to zero
	 */
	protected void setProgressBar(int min, int max) {
		display.asyncExec(new Runnable() { @Override public void run() { progressBar.setMinimum(min); progressBar.setMaximum(max); } });
		resetProgressBar();
	}
	
	/**
	 * Resets the progressBar to zero in the SWT thread (thread safe method)
	 */
	protected void resetProgressBar() {
		display.asyncExec(new Runnable() { @Override public void run() { progressBar.setSelection(0); } });
	}
	
	/**
	 * Increases the progressBar selection in the SWT thread (thread safe method)
	 */
	protected void increaseProgressBar() {
		display.asyncExec(new Runnable() {
			@Override public void run() {
				progressBar.setSelection(progressBar.getSelection()+1);
				if ( logger.isTraceEnabled() ) logger.trace("progressBar : "+(progressBar.getSelection()+1)+"/"+progressBar.getMaximum());
			}
		});
	}
	
	/**
	 * Method used to close graphical objects if needed
	 */
	protected void close() {
		display.syncExec(new Runnable() {
			@Override public void run() {
				if ( DBPlugin.areEqual(btnClose.getText(), "Cancel") )
					if ( logger.isDebugEnabled() ) logger.debug("Operation cancelled by user.");
				//TODO: create class property "cancelled" that will be checked by other threads to stop their processing
				//TODO: add loop to manage all remaining graphical events because of async execs created by other threads
				try { database.close(); } catch (Exception ignore) {}
				database = null;
				dialog.close();
				dialog = null;
			}
		});
	}
	
	/**
	 * Waits for all the asynchronous commands sent to SWT have finished
	 */
	public void sync() throws InterruptedException {
		if ( logger.isTraceEnabled() ) logger.trace("Synchronizing threads ...");
		if ( Display.getCurrent() != null ) {
			// if we are in the SWT thread, we dispatch all the events until there is no more
			while ( Display.getCurrent().readAndDispatch() );
		} else {
			// if we are in an async thread, we lock an object and ask SWT to unlock it.
			
			int timeout = 10000;	// timeout = 10 seconds
	        long before = 0L;
	
	        if ( logger.isTraceEnabled() ) before = System.nanoTime();
	        
			final Object waitObj = new Object();
			display.asyncExec(new Runnable() {
				public void run () {
					synchronized (waitObj) { waitObj.notify(); }
				}
			});
	
			synchronized (waitObj) { waitObj.wait(timeout); }
	        
	        if ( logger.isTraceEnabled() ) {
	        	long duration = (before - System.nanoTime())/1000000;		// we divide by 1000000 to convert nanoseconds to miliseconds
	        	if ( duration >= timeout)
	            	logger.trace("Timeout reached while waiting for thread synchronization ("+duration+") ...");
	            else
	            	logger.trace("Threads synchronized ...");
	        }
		}
	}
}
