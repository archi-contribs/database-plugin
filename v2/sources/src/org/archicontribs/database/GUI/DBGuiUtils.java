package org.archicontribs.database.GUI;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.archicontribs.database.DBLogger;
import org.archicontribs.database.DBPlugin;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * The DBGuiUtils class hosts the static methods that create simple popups on screeen to communicate with user.<wbr>
 * Methods are thread safe as they call display.suncExec
 * @author herve
 */
public class DBGuiUtils {
	protected static final DBLogger logger = new DBLogger(DBGuiUtils.class);
	protected static final Display display = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
    protected static final Color BLACK_COLOR = DBGui.BLACK_COLOR;
    protected static final Color BG_COLOR = DBGui.COMPO_LEFT_COLOR;			// light blue
    protected static final Font TITLE_FONT = DBGui.TITLE_FONT;
	protected static final Cursor CURSOR_WAIT = new Cursor(null, SWT.CURSOR_WAIT);
	protected static final Cursor CURSOR_ARROW = new Cursor(null, SWT.CURSOR_ARROW);
    
    
    /************** popup message ***********************/
    
    static Shell dialogShell = null;
    static Composite dialogComposite = null;
    static Label dialogLabel = null;
    /**
     * Shows up an on screen popup displaying a message to the user but does not wait for any user input<br>
     * The message can be changed in the popup window without closing the popup and opening a new one by calling several times the showPopupMessage method<br>
     * To dismiss the popup windows, it is the the responsibility of the caller to call the {@link #closePopupMessage()} method<br>
     * @param msg Message to show in the popup
     * @return 
     */
    public static Shell showPopupMessage(String msg) {
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
                    dialogComposite.setBackground(BG_COLOR);
                    dialogComposite.setLayout(new GridLayout( 1, false ) );

                    dialogLabel = new Label(dialogComposite, SWT.CENTER | SWT.WRAP);
                    dialogLabel.setBackground(BG_COLOR);
                    dialogLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, true, true ) );
                    dialogLabel.setFont(TITLE_FONT);
                } else {
                    restoreCursors();
                }

                dialogLabel.setText(msg);
                dialogShell.layout(true);
                dialogShell.open();

                dialogComposite.layout();

                setWaitCursor();
            }
        });

        return dialogShell;
    }

    /**
     * dismiss the popupMessage window if it is displayed (else, does nothing) 
     */
    public static void closePopupMessage() {
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
    
    /******************** popup ***********************/
	
    /**
     * Shows up an on screen popup displaying the message and wait for the user to click on the "OK" button
     * @param level Level of the message (INFO, ERROR, DEBUG, ...) 
     * @param msg Message to print in the popup
     */
    public static void popup(Level level, String msg) {
        popup(level,msg,null);
    }

    /**
     * Shows up an on screen popup, displaying the message (and the exception message if any) and wait for the user to click on the "OK" button<br>
     * The exception stacktrace is also printed on the standard error stream
     * @param level Level of the message (INFO, ERROR, DEBUG, ...) 
     * @param msg Message to print in the popup
     * @param e Exception to display in the popup
     */
    public static void popup(Level level, String msg, Exception e) {
        logger.log(DBGuiUtils.class, level, msg, e);

        display.syncExec(new Runnable() {
        	@Override
        	public void run() {
        		String popupMessage = msg;
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
        	}
        });

        //refreshDisplay();
    }
    
    
    /******************** question *********************/

    /** The choice made by the user in the question window (0 = first choice, 1 = 2nd choice, ...) */
	protected static int questionResult;
	
    /**
     * Shows up an on screen popup displaying a question to the user and defaults the possible answers to YRES or NO<br>
     * It is required to allows communication between threads
     * @param msg The question to ask to the user
     * @return true if the user selected YES and false if the user selected NO 
     */
    public static boolean question(String msg) {
        return question(msg, new String[] {"Yes", "No"}) == 0;
    }

    /**
     * Open up an on screen popup displaying a question to the user and allows to choose the possible answers<br>
     * @param msg The question to ask to the user
     * @param buttonLabels The list of choices to present to the user
     * @return 0 if the user selected the first choice, 1 if the user selected the second choice, and so on 
     */
    public static int question(String msg, String[] buttonLabels) {
        if ( logger.isDebugEnabled() ) logger.debug(DBGuiUtils.class, "Question: "+msg);

        display.syncExec(new Runnable() {
            @Override
            public void run() {
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
    

    /******************** passwordDialog **********************/
    
	/** The password entered by the user */
    protected static String answeredPassword;
    
    /**
     * Open up an input dialog and ask for a password
     * @param title the dialog title 
     * @param message the message on the password dialog
     * @return the password entered by the user
     */
    public static String passwordDialog(String title, String message) {
    	if ( logger.isDebugEnabled() ) logger.debug(DBGui.class, "Asking for password");
    	answeredPassword = "";
    	display.syncExec(new Runnable() {
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
    
    /********************* selectItemsDialog ******************/
    protected List<EObject> selectedItemsToReturn = new ArrayList<EObject>();
    
    /**
     * Open up an input dialog with a list of items and allow to select items in the list 
     * @param title the dialog title 
     * @param message the message on the dialog
     * @param map the list to display under the form of Map<EObject, String>
     * @param singleMode true if only a single item in the list can be selected, false if multiple items can be selected 
     * @return the password entered by the user
     */
    public static List<EObject> selectItemsDialog(String title, String message, Map<EObject, String> map, boolean singleMode) {
        logger.info(DBGui.class, message);

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                setWaitCursor();

                Shell shell = new Shell(display, SWT.APPLICATION_MODAL);
                shell.setSize(500, 300);
                shell.setBackground(BLACK_COLOR);

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
        	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width -  500) / 4;
        	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - 300) / 4;
                }    
                shell.setLocation(new Point(locationX, locationY));

                int borderWidth = (dialogShell.getBorderWidth()+1)*2;
                Composite composite = new Composite(dialogShell, SWT.NONE);
                composite.setSize(500-borderWidth, 300-borderWidth);
                composite.setLocation(1, 1);
                composite.setBackground(BG_COLOR);
                composite.setLayout(new GridLayout( 1, false ) );

                Label label = new Label(dialogComposite, SWT.CENTER | SWT.WRAP);
                label.setText(message);
                label.setBackground(BG_COLOR);
                label.setFont(TITLE_FONT);
                label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false ) );

                
                Table table = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL| SWT.H_SCROLL);
                TableColumn column = new TableColumn(table, SWT.NULL);
                column.setText("Component");
                column = new TableColumn(table, SWT.NULL);
                column.setText("Comment");
                table.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, true ) );
                
                Button okButton = new Button(shell, SWT.NONE);
                okButton.setText("OK");
                okButton.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false ) );
                
                okButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        okButton.dispose();
                    }
                });
                
                composite.layout(true);               
                shell.layout(true);
                shell.open();
                
                while (!okButton.isDisposed()) {		// the okButton is disposed in the selectionListener (i.e. when the user clicks on it) 
                    if (!display.readAndDispatch())
                        display.sleep();
                }
                restoreCursors();
            }
        });
        
        return null;
    }
    
    
    /*************** cursors ******************/
    protected static Stack<Map<Shell, Cursor>> cursorsStack = new Stack<Map<Shell, Cursor>>();
    
    /**
     * Sets the mouse cursor as WAIT
     */
    public static void setWaitCursor() {
    	display.syncExec(new Runnable() {
            @Override
            public void run() {
            	Map<Shell, Cursor> cursors = new HashMap<Shell, Cursor>();
            	for ( Shell shell: display.getShells() ) {
            		cursors.put(shell,  shell.getCursor());
            		shell.setCursor(CURSOR_WAIT);
            	}
            	cursorsStack.push(cursors);
            }
    	});
        //refreshDisplay();
    }

    /**
     * Restores the mouse cursor as it was before beeing set as WAIT
     */
    public static void restoreCursors() {
    	display.syncExec(new Runnable() {
            @Override
            public void run() {
		    	Map<Shell, Cursor> cursors = cursorsStack.pop();
		        for ( Shell shell: display.getShells() ) {
		            Cursor cursor = (cursors==null) ? null : cursors.get(shell);
		            shell.setCursor(cursor==null ? CURSOR_ARROW : cursor);
		        }
            }
    	});
        //refreshDisplay();
    }
}
