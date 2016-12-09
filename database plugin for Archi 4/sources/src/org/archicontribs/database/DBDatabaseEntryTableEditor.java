package org.archicontribs.database;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.archicontribs.database.DBPlugin.Level;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;

/**
 * A field editor that manages a table of input values. The editor
 * displays a table containing the rows of values, buttons for adding,
 * duplicating and removing rows and buttons to adjust the order of rows in the
 * table. The table also allows in-place editing of values.
 *
 * inspired by Sandip V. Chitale work (https://code.google.com/archive/p/pathtools/)
 * 
 * @author Herve jouin
 */
public class DBDatabaseEntryTableEditor extends FieldEditor {
	private TableViewer tableViewer;
	private List<DBDatabaseEntry> databaseEntries = new ArrayList<DBDatabaseEntry>();
	
	private static final Color HIGHLIGHTED_COLOR = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
	
	private Table table;
	private Composite buttonBox;
	private Button newButton;
	private Button duplicateButton;
	private Button removeButton;
	private Button upButton;
	private Button downButton;
	private Button checkButton;

	/**
	 * Creates a table field editor.
	 *
	 * @param name			: the name of the preference this field editor works on
	 * @param labelText 	: the label text of the field editor
	 * @param columnNames	: the names of columns
	 * @param columnWidths	: the widths of columns
	 * @param parent		: the parent of the field editor's control
	 *
	 */
	protected DBDatabaseEntryTableEditor(String name, String labelText, Composite parent) {
		init(name, labelText);
		DBPlugin.debug(DebugLevel.MainMethod, "new DBDatabaseEntryTableEditor(\""+name+"\",\""+labelText+"\")");
		createControl(parent);		// calls doFillIntoGrid
	}

	/*
	 * (non-Javadoc) Method declared in FieldEditor.
	 * 
	 * called by createControl(parent)
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.doFillIntoGrid()");

		tableViewer = new TableViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		tableViewer.setUseHashlookup(true);
		tableViewer.setContentProvider(new DBTableContentProvider());
		tableViewer.setLabelProvider(new DBTableLabelProvider());
		tableViewer.setInput(databaseEntries);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
	        @Override
	        public void selectionChanged(SelectionChangedEvent event) {
	        	tableSelectionChanged();
	        }
	        });
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tableViewer, new FocusCellOwnerDrawHighlighter(tableViewer));

		ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(tableViewer) {
		    protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
		        // Enable editor only with mouse double click (left button)
		        if (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION) {
		            EventObject source = event.sourceEvent;
		            if (source instanceof MouseEvent && ((MouseEvent)source).button == 1)
		                return true;
		        }
		        return false;
		    }
		};

		TableViewerEditor.create(tableViewer, focusCellManager, activationSupport, ColumnViewerEditor.DEFAULT);
		
		table = tableViewer.getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setFont(parent.getFont());
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		CellEditor[] editors = new CellEditor[DBDatabaseEntry.properties.length];
		for (int i = 0 ; i < DBDatabaseEntry.properties.length ; ++i) {
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setText(DBPlugin.capitalize(DBDatabaseEntry.properties[i]));
			col.setWidth(75);
			if ( DBDatabaseEntry.properties[i].toLowerCase().equals("driver") )
				editors[i] = new ComboBoxCellEditor(table, DBPlugin.driverList, SWT.READ_ONLY);
			else
				editors[i] = new TextCellEditor(table, DBDatabaseEntry.properties[i].toLowerCase().equals("password")?SWT.PASSWORD:SWT.NONE);
		}
		tableViewer.setColumnProperties(DBDatabaseEntry.properties);
		tableViewer.setCellModifier(new DBCellModifier(tableViewer));
		tableViewer.setCellEditors(editors);
		
		buttonBox = getButtonBoxControl(parent);
		GridData gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		buttonBox.setLayoutData(gd);
		
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.doFillIntoGrid()");
	}
	
	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void adjustForNumColumns(int numColumns) {
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void doLoadDefault() {
	}
	
	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void doLoad() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.doLoad()");
		databaseEntries = DBDatabaseEntry.getAllFromPreferenceStore();
		tableViewer.setInput(databaseEntries);
		tableViewer.refresh();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.doLoad()");
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	protected void doStore() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.doStore()");
		DBDatabaseEntry.setAllIntoPreferenceStore(databaseEntries);
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.doStore()");
	}

	/**
	 * Returns this field editor's button box containing the Add, Remove, Up,
	 * and Down button.
	 *
	 * @param parent
	 *            the parent control
	 * @return the button box
	 */
	public Composite getButtonBoxControl(Composite parent) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "+Entering DBDatabaseEntryTableEditor.getButtonBoxControl()");
		if (buttonBox == null) {
			buttonBox = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.marginWidth = 0;
			buttonBox.setLayout(layout);
			newButton = createPushButton(buttonBox, "New");
			duplicateButton = createPushButton(buttonBox, "Duplicate");
			removeButton = createPushButton(buttonBox, "Remove");
			upButton = createPushButton(buttonBox, "Up");
			downButton = createPushButton(buttonBox, "Down");
			checkButton = createPushButton(buttonBox, "Check");
			buttonBox.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					newButton = null;
					duplicateButton = null;
					removeButton = null;
					upButton = null;
					downButton = null;
					checkButton = null;
					buttonBox = null;
				}
			});

		} else {
			checkParent(buttonBox, parent);
		}

		tableSelectionChanged();
		DBPlugin.debug(DebugLevel.SecondaryMethod, "-Leaving DBDatabaseEntryTableEditor.getButtonBoxControl()");
		return buttonBox;
	}

	/**
	 * Returns this field editor's table control.
	 *
	 * @param parent
	 *            the parent control
	 * @return the table control
	 */
	public Table getTableControl(Composite parent) {
		if ( table != null )
			checkParent(table, parent);
		return table;
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	public int getNumberOfControls() {
		return 2;
	}

	/**
	 * Invoked when the selection in the list has changed.
	 */
	protected void tableSelectionChanged() {
		DBPlugin.debug(DebugLevel.MainMethod, "DBDatabaseEntryTableEditor.tableSelectionChanged()");
		int index = table.getSelectionIndex();
		int size = table.getItemCount();

		duplicateButton.setEnabled(index >= 0);
		removeButton.setEnabled(index >= 0);
		upButton.setEnabled(size > 1 && index > 0);
		downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
		checkButton.setEnabled(index >= 0);
		
		for (int i=0; i<size; ++i) {
			table.getItem(i).setBackground(i==index?HIGHLIGHTED_COLOR:table.getBackground());
		}
	}

	/*
	 * (non-Javadoc) Method declared on FieldEditor.
	 */
	public void setFocus() {
		if ( table != null )
			table.setFocus();
	}
	
	/**
	 * Notifies that the Add button has been pressed.
	 */
	private void newCallback() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.newCallback()");
		
		int index = table.getItemCount()-1;
		for (int i=0; i < table.getItemCount(); ++i) {
			if ( table.getItem(i).getBackground().equals(HIGHLIGHTED_COLOR) ) {
				index = i;
				break;
			}
		}
		databaseEntries.add(index+1, new DBDatabaseEntry("new"));
		tableViewer.refresh();
		table.setSelection(index+1);
		tableSelectionChanged();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.newCallback()");
	}
	
	/**
	 * Notifies that the Duplicate button has been pressed.
	 */
	private void duplicateCallback() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.duplicateCallback()");
		
		int index = table.getSelectionIndex();
		databaseEntries.add(index+1, new DBDatabaseEntry(databaseEntries.get(index)));
		tableViewer.refresh();
		tableSelectionChanged();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.duplicateCallback()");
	}
	
	/**
	 * Notifies that the check button has been pressed.
	 */
	private void checkCallback() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.checkCallback()");
		setPresentsDefaultValue(false);
		int index = table.getSelectionIndex();

		if (index >= 0) {
			DBDatabaseEntry entry = databaseEntries.get(index);
			try {
				// we check if we can connect to the database
	            checkButton.getShell().setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_WAIT));
				DBPlugin.openConnection(entry.getDriver(), entry.getServer(), entry.getPort(), entry.getDatabase(), entry.getUsername(), entry.getPassword()).close();
				DBPlugin.popup(Level.Info, "Congratulations !\n\nconnection to the database succeeded.");
			} catch (Exception err) {
				DBPlugin.popup(Level.Error, "Cannot connect to the database.\n\n"+err.getMessage());
			}
			checkButton.getShell().setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
		}
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.checkCallback()");
	}

	/**
	 * Notifies that the Remove button has been pressed.
	 */
	private void removeCallback() {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.removeCallback()");
		setPresentsDefaultValue(false);
		int index = table.getSelectionIndex();
		databaseEntries.remove(index);
		tableViewer.refresh();
		
		if ( index < table.getItemCount() )
			table.setSelection(index);
		else if ( index > 0 )
			table.setSelection(index-1);
			
		tableSelectionChanged();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.removeCallback()");
	}

	/**
	 * Moves the currently selected item up or down.
	 *
	 * @param up
	 *            <code>true</code> if the item should move up, and
	 *            <code>false</code> if it should move down
	 */
	private void swap(int direction) {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBDatabaseEntryTableEditor.swap("+direction+")");
		setPresentsDefaultValue(false);
		
		int source = table.getSelectionIndex();
		DBDatabaseEntry sourceEntry = databaseEntries.get(source);
		
		int target = table.getSelectionIndex()+direction;
		DBDatabaseEntry targetEntry = databaseEntries.get(target);
		
		DBPlugin.debug(DebugLevel.Variable,"swapping entrie "+source+" and "+target+".");
		
		databaseEntries.set(source, targetEntry);
		databaseEntries.set(target, sourceEntry);

		tableViewer.refresh();
		tableSelectionChanged();
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBDatabaseEntryTableEditor.swap("+direction+")");
	}

	/*
	 * @see FieldEditor.setEnabled(boolean,Composite).
	 */
	public void setEnabled(boolean enabled, Composite parent) {
		DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntryTableEditor.setEnabled()");
		super.setEnabled(enabled, parent);
		table.setEnabled(enabled);
		newButton.setEnabled(enabled);
		duplicateButton.setEnabled(enabled);
		removeButton.setEnabled(enabled);
		upButton.setEnabled(enabled);
		downButton.setEnabled(enabled);
	}

	
	/**
	 * Helper method to create a push button.
	 *
	 * @param parent	: the parent control
	 * @param key		: the resource name used to supply the button's label text
	 * @return Button
	 */
	private Button createPushButton(Composite parent, String key) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(key);
		button.setFont(parent.getFont());
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = Math.max(convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH), button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
		button.addSelectionListener(selectionListener);
		return button;
	}
	
	public TableViewer getTableViewer() {
		return tableViewer;
	}
	
	/**
	 * selection listener called when a pushed button is pressed
	 */
	private SelectionListener selectionListener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent event) {
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBDatabaseEntryTableEditor.selectionListener.widgetSelected()");
			Widget widget = event.widget;
			if (widget == newButton) {
				newCallback();
			} else if (widget == duplicateButton) {
				duplicateCallback();
			} else if (widget == removeButton) {
				removeCallback();
			} else if (widget == upButton) {
				swap(-1);
			} else if (widget == downButton) {
				swap(1);
			} else if (widget == checkButton) {
				checkCallback();
			} else if (widget == table) {
				tableSelectionChanged();
			}
		}
	};
	
	class DBTableLabelProvider implements ITableLabelProvider {
		@Override
		public void addListener(ILabelProviderListener listener) {
			// do nothing
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// do nothing
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String txt;
			if ( DBDatabaseEntry.properties[columnIndex].equals("password") && ((DBDatabaseEntry) element).getProperty(DBDatabaseEntry.properties[columnIndex])!=null ) {
				txt = ((DBDatabaseEntry) element).getProperty(DBDatabaseEntry.properties[columnIndex]).replaceAll(".", "*");
			} else {
				txt = ((DBDatabaseEntry) element).getProperty(DBDatabaseEntry.properties[columnIndex]);
			}
			return txt;
		}		
	};
	
	class DBTableContentProvider implements IStructuredContentProvider {
		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {
			return ((List<DBDatabaseEntry>) inputElement).toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	class DBCellModifier implements ICellModifier {
		private TableViewer tableViewer;
		
		/**
		 * Constructor 
		 * @param TableViewerExample an instance of a TableViewerExample 
		 */
		public DBCellModifier(TableViewer tableViewer) {
			super();
			DBPlugin.debug(DebugLevel.MainMethod, "DBCellModifier()");
			this.tableViewer = tableViewer;
		}

		/**
		 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
		 */
		public boolean canModify(Object element, String property) {
			return true;
		}

		/**
		 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
		 */
		public Object getValue(Object element, String property) {
			DBDatabaseEntry entry = (DBDatabaseEntry) element;
			Object result = new Integer(0);

			if ( property.toLowerCase().equals("driver") ) {
				// must return the index of the element in the combo
				for (int i = 0; i < DBPlugin.driverList.length ; ++i) {
					if ( DBPlugin.driverList[i].equals(entry.getDriver()) ) {
						result = new Integer(i);
						break;
					}
				}
			} else {
				result =  entry.getProperty(property);
			}

			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBCellModifier.getValue("+property+") -> "+result);
			return result;
		}

		/**
		 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
		 */
		public void modify(Object element, String property, Object value) {	
			DBPlugin.debug(DebugLevel.SecondaryMethod, "DBCellModifier.modify("+property+","+value+")");
			
			if (element instanceof Item) element = ((Item) element).getData();
			
			DBDatabaseEntry entry = (DBDatabaseEntry) element;
			
			if ( property.toLowerCase().equals("driver") ) {
				entry.setDriver(DBPlugin.driverList[((Integer)value).intValue()]);
				entry.setPort(DBPlugin.defaultPorts[((Integer)value).intValue()]);
			} else {
				entry.setProperty(property, ((String) value).trim());
			}

			tableViewer.refresh();
		}
	}
}
