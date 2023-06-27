package org.archicontribs.database.gui;

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
import org.archicontribs.database.model.DBMetadata;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

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
    
    static Shell popupShell = null;
    static Composite popupComposite = null;
    static Label popupLabel = null;
    
    /**
     * Shows up an on screen popup displaying a message to the user but does not wait for any user input<br>
     * The message can be changed in the popup window without closing the popup and opening a new one by calling several times the showPopupMessage method<br>
     * To dismiss the popup windows, it is the the responsibility of the caller to call the {@link #closePopupMessage()} method<br>
     * @param showPopup if true, shows the popop, else sends the message to the logger
     * @param msg Message to show in the popup
     * @return 
     */
    public static Shell showPopupMessage(boolean showPopup, String msg) {
    	if ( showPopup )
    		return showPopupMessage(msg);
    	
    	logger.debug(msg);
    	return null;
    }
    /**
     * Shows up an on screen popup displaying a message to the user but does not wait for any user input<br>
     * The message can be changed in the popup window without closing the popup and opening a new one by calling several times the showPopupMessage method<br>
     * To dismiss the popup windows, it is the the responsibility of the caller to call the {@link #closePopupMessage()} method<br>
     * @param msg Message to show in the popup
     * @return 
     */
    public static Shell showPopupMessage(String msg) {
        logger.info(msg);

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                if ( popupShell == null ) {
                	popupShell = new Shell(display, SWT.APPLICATION_MODAL);
                	popupShell.setSize(500, 70);
                	popupShell.setBackground(BLACK_COLOR);

                    // Use the active shell, if available, to determine the new shell placing
                    int locationX = 0;
                    int locationY = 0;
                    Rectangle shellSize = popupShell.getBounds();
                    Shell parent = display.getActiveShell();
                    if (parent!=null) { 
                    	Rectangle parentSize = parent.getBounds();
            	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
            	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                    } else {
            	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - 500) / 4;
            	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - 70) / 4;
                    }    
                    popupShell.setLocation(new Point(locationX, locationY));

                    int borderWidth = (popupShell.getBorderWidth()+1)*2;
                    popupComposite = new Composite(popupShell, SWT.NONE);
                    popupComposite.setSize(500-borderWidth, 70-borderWidth);
                    popupComposite.setLocation(1, 1);
                    popupComposite.setBackground(BG_COLOR);
                    popupComposite.setLayout(new GridLayout( 1, false ) );

                    popupLabel = new Label(popupComposite, SWT.CENTER | SWT.WRAP);
                    popupLabel.setBackground(BG_COLOR);
                    popupLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, true, true ) );
                    popupLabel.setFont(TITLE_FONT);
                } else {
                    restoreCursors();
                }

                popupLabel.setText(msg);
                popupShell.layout(true);
                popupShell.open();

                popupComposite.layout();

                setWaitCursor();
            }
        });

        return popupShell;
    }

    /**
     * dismiss the popupMessage window if it is displayed (else, does nothing) 
     */
    public static void closePopupMessage() {
        if ( popupShell != null ) {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    popupShell.close();
                    popupShell = null;

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
        logger.debug(level.toString()+": "+msg, e);

        display.syncExec(new Runnable() {
        	@Override
        	public void run() {
        		int statusSeverity;
        		int dialogKind;

        		switch ( level.toInt() ) {
        		case Priority.FATAL_INT:
        		case Priority.ERROR_INT:
        			statusSeverity = IStatus.ERROR;
        			dialogKind = MessageDialog.ERROR;
        			break;
        		case Priority.WARN_INT:
        			statusSeverity = IStatus.WARNING;
        			dialogKind = MessageDialog.WARNING;
        			break;
        		default:
        			statusSeverity = IStatus.INFO;
        			dialogKind = MessageDialog.INFORMATION;
        			break;
        		}
        		
    			if ( e != null ) {
    				List<Status> childStatuses = new ArrayList<>();
    		        Throwable err = e;
    		        while ( err != null ) {
	    				for (StackTraceElement stackTrace: e.getStackTrace())
	    		            childStatuses.add(new Status(statusSeverity, DBPlugin.PLUGIN_TITLE, stackTrace.toString()));
	    				err = err.getCause();
	    				if ( err != null ) {
	    					childStatuses.add(new Status(statusSeverity, DBPlugin.PLUGIN_TITLE, ""));
	    					childStatuses.add(new Status(statusSeverity, DBPlugin.PLUGIN_TITLE, "Caused by ..."));
	    					childStatuses.add(new Status(statusSeverity, DBPlugin.PLUGIN_TITLE, ""));
	    				}
    		        }
    		        MultiStatus multiStatus = new MultiStatus(DBPlugin.PLUGIN_TITLE, statusSeverity, childStatuses.toArray(new Status[] {}), e.toString(), e);
    				ErrorDialog.openError(display.getActiveShell(), DBPlugin.PLUGIN_TITLE, msg, multiStatus);
    			} else
    				MessageDialog.open(dialogKind, display.getActiveShell(), DBPlugin.PLUGIN_TITLE, msg, SWT.NONE);
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
        if ( logger.isDebugEnabled() ) logger.debug("Question: "+msg);

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                Shell questionShell = new Shell(display, SWT.SHELL_TRIM);
                questionShell.setSize(0, 0);
                questionShell.setBackground(BLACK_COLOR);

                // Use the active shell, if available, to determine the new shell placing
                int locationX = 0;
                int locationY = 0;
                Rectangle shellSize = questionShell.getBounds();
                Shell parent = display.getActiveShell();
                if (parent!=null) { 
                	Rectangle parentSize = parent.getBounds();
        	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
        	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                } else {
        	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - questionShell.getSize().x) / 4;
        	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - questionShell.getSize().y) / 4;
                }
                questionShell.setLocation(new Point(locationX, locationY));
                MessageDialog messageDialog = new MessageDialog(questionShell, DBPlugin.PLUGIN_TITLE, null, msg, MessageDialog.QUESTION, buttonLabels, 0);
                questionResult = messageDialog.open();
            }
        });

        if ( logger.isDebugEnabled() ) logger.debug("Answer: "+buttonLabels[questionResult]);
        return questionResult;
    }
    

    /******************** passwordDialog **********************/
    
	/** The password entered by the user */
    protected static String answeredPassword;
    
    /**
     * Open up an input dialog and ask for a password
     * @param title the dialog title 
     * @return the password entered by the user
     */
    public static String passwordDialog(String title) {
    	if ( logger.isDebugEnabled() ) logger.debug("Asking for password");
    	answeredPassword = "";
    	display.syncExec(new Runnable() {
            @Override
            public void run() {
                Shell passwordDialogShell = new Shell(display, SWT.SHELL_TRIM);
                passwordDialogShell.setText(title);
                passwordDialogShell.setSize(0, 0);
                passwordDialogShell.setBackground(BLACK_COLOR);

                // Use the active shell, if available, to determine the new shell placing
                int locationX = 0;
                int locationY = 0;
                Rectangle shellSize = passwordDialogShell.getBounds();
                Shell parent = display.getActiveShell();
                if (parent!=null) { 
                	Rectangle parentSize = parent.getBounds();
        	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
        	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                } else {
        	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width - passwordDialogShell.getSize().x) / 4;
        	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - passwordDialogShell.getSize().y) / 4;
                }
                passwordDialogShell.setLocation(new Point(locationX, locationY));
                
                DBGuiPasswordDialog passwordDialog = new DBGuiPasswordDialog(passwordDialogShell);
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
    protected List<EObject> selectedItemsToReturn = new ArrayList<>();
    
    /**
     * Open up an input dialog with a list of items and allow to select items in the list 
     * @param title the dialog title 
     * @param message the message on the dialog
     * @param map the list to display under the form of Map<EObject, String>
     * @return the password entered by the user
     */
    public static List<EObject> selectItemsDialog(String title, String message, Map<EObject, String> map) {
        logger.info(message);

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                setWaitCursor();

                Shell selectItemsShell = new Shell(display, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
                selectItemsShell.setText(title);
                selectItemsShell.setSize(1024, 768);
                selectItemsShell.setBackground(BLACK_COLOR);
                selectItemsShell.setLayout(new GridLayout(1, false));
                

                // Use the active shell, if available, to determine the new shell placing
                int locationX = 0;
                int locationY = 0;
                Rectangle shellSize = selectItemsShell.getBounds();
                Shell parent = display.getActiveShell();
                if (parent!=null) { 
                	Rectangle parentSize = parent.getBounds();
        	        locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
        	        locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
                } else {
        	        locationX = (Toolkit.getDefaultToolkit().getScreenSize().width -  shellSize.width) / 4;
        	        locationY = (Toolkit.getDefaultToolkit().getScreenSize().height - shellSize.height) / 4;
                }    
                selectItemsShell.setLocation(new Point(locationX, locationY));

                Composite selectItemsComposite = new Composite(selectItemsShell, SWT.NONE);
                selectItemsComposite.setBackground(BG_COLOR);
                selectItemsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                selectItemsComposite.setLayout(new FormLayout());

                Label label = new Label(selectItemsComposite, SWT.WRAP | SWT.BORDER);
                label.setText(message);
                label.setBackground(BG_COLOR);
                label.pack();
                FormData fd = new FormData();
                fd.top = new FormAttachment(0, 5);
                fd.left = new FormAttachment(0, 5);
                fd.right = new FormAttachment(100, -5);
                label.setLayoutData(fd);
                
                Button okButton = new Button(selectItemsComposite, SWT.NONE);
                okButton.setText("OK");
                fd = new FormData();
                fd.right = new FormAttachment(100, -5);
                fd.bottom = new FormAttachment(100, -5);
                okButton.setLayoutData(fd);

                
                Table table = new Table(selectItemsComposite, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL| SWT.H_SCROLL);
                table.setLinesVisible(true);
                table.setHeaderVisible(true);
                fd = new FormData();
                fd.top = new FormAttachment(label, 5);
                fd.left = new FormAttachment(0, 5);
                fd.right = new FormAttachment(100, -5);
                fd.bottom = new FormAttachment(okButton, -5);
                table.setLayoutData(fd);
                
                
                TableColumn column = new TableColumn(table, SWT.NULL);
                column.setWidth(10);
                column = new TableColumn(table, SWT.NULL);
                column.setText("Component");
                column.setWidth(100);
                column = new TableColumn(table, SWT.NULL);
                column.setText("Comment");
                column.setWidth(100);
                
                for (var entry : map.entrySet()) {
                    TableItem tableItem = new TableItem(table, SWT.NONE);
                    DBMetadata metadata = DBMetadata.getDBMetadata(entry.getKey());
                    tableItem.setText(1, metadata.getDebugName());
                    tableItem.setText(2, entry.getValue());
                }
                
                okButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                    	selectItemsShell.dispose();
                    }
                });
                
                selectItemsComposite.layout(true);               
                selectItemsShell.layout(true);
                selectItemsShell.open();
                
                while (!selectItemsShell.isDisposed()) {		// the shell window is disposed in the selectionListener (i.e. when the user clicks on it) 
                    if (!display.readAndDispatch())
                        display.sleep();
                }
                restoreCursors();
            }
        });
        
        return null;
    }
    
    
    /*************** cursors ******************/
    protected static Stack<Map<Shell, Cursor>> cursorsStack = new Stack<>();
    
    /**
     * Sets the mouse cursor as WAIT
     */
    public static void setWaitCursor() {
    	display.syncExec(new Runnable() {
            @Override
            public void run() {
            	Map<Shell, Cursor> cursors = new HashMap<>();
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
