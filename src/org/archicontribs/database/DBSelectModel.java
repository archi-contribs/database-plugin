/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */

package org.archicontribs.database;

import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.model.util.Logger;

public class DBSelectModel extends Dialog {
	private enum Mode { Unknown, Import, Export };
	private HashMap<String,String> modelSelected = null;
	private Shell dialog;
	private DBModel dbModel;
	private Connection db;
	private Mode mode = Mode.Unknown;
	private Text lblId;
	private Text id;
	private Text lblName;
	private Text name;
	private Table tblId;
	private TableColumn columnId;
	private TableColumn columnName;
	private Table tblVersion;
	private Button checkActual;
	private Text actualVersion;
	private Button checkMinor;
	private Text minorVersion;
	private Button checkMajor;
	private Text majorVersion;
	private Button checkCustom;
	private Text customVersion;

	private Text purpose;
	private Text owner;
	private Text note;
	private Button okButton;
	private Button cancelButton;

	/**
	 * Create the dialog.
	 */
	public DBSelectModel(/*Shell parent, int style*/) {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/**
	 * Open the dialog.
	 * @param db
	 * @param mode
	 * @param model
	 * @return a ResultSet containing the database row corresponding to the model to import or export
	 */
	public HashMap<String,String>  open(Connection _db, DBModel _dbModel) throws SQLException {
		dbModel = _dbModel;
		mode = Mode.Export;
		return open(_db);
	}
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public HashMap<String,String> open(Connection _db) throws SQLException {
		db = _db;
		if ( mode == Mode.Unknown )
			mode = Mode.Import;
		Display display = getParent().getDisplay();
		createContents();
		setValues();
		dialog.open();
		dialog.layout();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return modelSelected;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		dialog = new Shell(getParent(), getStyle());
		dialog.setText(DBPlugin.pluginTitle);
		if ( mode == Mode.Import )
			dialog.setText("Please choose the model to import ...");
		else
			dialog.setText("Please choose the model to export ...");
		dialog.setSize(800, 525);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 2);
		dialog.setLayout(null);

		ScrolledComposite compositeId = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
		compositeId.setBounds(10, 68, 400, 392);
		compositeId.setAlwaysShowScrollBars(true);
		compositeId.setExpandHorizontal(true);
		compositeId.setExpandVertical(true);

		TableViewer tableViewerId = new TableViewer(compositeId, SWT.FULL_SELECTION);
		tblId = tableViewerId.getTable();
		tblId.setLinesVisible(true);
		tblId.addListener(SWT.Selection, selectModelListener);
		tblId.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				okButton.notifyListeners(SWT.Selection, new Event());
			}	
		});


		columnId = new TableColumn(tblId, SWT.NONE);
		columnId.setResizable(false);
		columnId.setMoveable(true);
		columnId.setWidth(100);
		columnId.setText("ID");

		columnName = new TableColumn(tblId, SWT.NONE);
		columnName.setResizable(false);
		columnName.setWidth(280);
		columnName.setText("Name");

		compositeId.setContent(tblId);
		compositeId.setMinSize(tblId.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		lblId = new Text(dialog, SWT.BORDER);
		lblId.setBounds(10, 10, 103, 21);
		lblId.addMouseListener(sortColumns);
		lblId.setEditable(false);
		lblId.setText("             ID");


		id = new Text(dialog, SWT.BORDER);
		id.setBounds(10, 37, 103, 21);
		id.setEditable(mode == Mode.Export);
		id.setTextLimit(50);
		id.addListener(SWT.Verify, new Listener() {
			public void handleEvent(Event e) {
				//TODO:  allow only alphanum digits and, very important, refuse the '-' char
				check();
			}
		});

		lblName = new Text(dialog, SWT.BORDER);
		lblName.setBounds(112, 10, 275, 21);
		lblName.addMouseListener(sortColumns);
		lblName.setText("                                      Name");
		lblName.setEditable(false);

		name = new Text(dialog, SWT.BORDER);
		name.setTextLimit(255);
		name.setBounds(112, 37, 275, 21);
		name.setEditable(mode == Mode.Export);
		id.addListener(SWT.Verify, new Listener() {
			public void handleEvent(Event e) {
				check();
			}
		});

		/*******************/

		if ( mode == Mode.Import ) {
			ScrolledComposite compositeVersion = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
			compositeVersion.setExpandVertical(true);
			compositeVersion.setExpandHorizontal(true);
			compositeVersion.setAlwaysShowScrollBars(true);
			compositeVersion.setBounds(521, 81, 265, 98);
			compositeVersion.setMinSize(new Point(397, 41));

			TableViewer tableViewerVersion = new TableViewer(compositeVersion, SWT.FULL_SELECTION);
			tblVersion = tableViewerVersion.getTable();
			tblVersion.setLinesVisible(true);
			tblVersion.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TableItem tableItem = tblVersion.getSelection()[0];
					name.setText(tableItem.getData("name") == null ? "" : (String)tableItem.getData("name"));
					purpose.setText(tableItem.getData("purpose") == null ? "" : (String)tableItem.getData("purpose"));
					owner.setText(tableItem.getData("owner") == null ? "" : (String)tableItem.getData("owner"));
					note.setText(tableItem.getData("note") == null ? "" : (String)tableItem.getData("note"));
					check();
				}
			});


			TableColumn columnVersion = new TableColumn(tblVersion, SWT.NONE);
			columnVersion.setResizable(false);
			columnVersion.setWidth(75);
			columnVersion.setText("Version");

			TableColumn columnDate = new TableColumn(tblVersion, SWT.NONE);
			columnDate.setResizable(false);
			columnDate.setWidth(170);
			columnDate.setText("Date");

			compositeVersion.setContent(tblVersion);
			compositeVersion.setMinSize(tblVersion.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		} else {
			Group grp = new Group(dialog, SWT.NONE);
			grp.setBounds(521, 62, 116, 102);
			grp.setLayout(null);

			checkActual = new Button(grp, SWT.RADIO);
			checkActual.setBounds(6, 18, 96, 16);
			checkActual.setEnabled(true);
			checkActual.setText("actual version:");
			actualVersion = new Text(dialog, SWT.NONE);
			actualVersion.setBounds(643, 81, 68, 15);
			actualVersion.setEnabled(false);
			actualVersion.setText(DBPlugin.incMinor(dbModel.getVersion()));


			checkMinor = new Button(grp, SWT.RADIO);
			checkMinor.setBounds(6, 37, 101, 16);
			checkMinor.setEnabled(true);
			checkMinor.setSelection(true);
			checkMinor.setText("minor change:");
			minorVersion = new Text(dialog, SWT.NONE);
			minorVersion.setBounds(643, 98, 68, 15);
			minorVersion.setEnabled(false);


			checkMajor = new Button(grp, SWT.RADIO);
			checkMajor.setBounds(6, 56, 100, 16);
			checkMajor.setEnabled(true);
			checkMajor.setText("major change:");

			majorVersion = new Text(dialog, SWT.NONE);
			majorVersion.setBounds(643, 117, 68, 15);
			majorVersion.setEnabled(false);


			checkCustom = new Button(grp, SWT.RADIO);
			checkCustom.setBounds(6, 75, 111, 16);
			checkCustom.setEnabled(true);
			checkCustom.setText("custom version: ");

			customVersion = new Text(dialog, SWT.NONE);
			customVersion.setBounds(643, 138, 70, 15);
			customVersion.setEnabled(true);
			id.setTextLimit(11);		// 5 digits before the dot, 5 digits after
			customVersion.addListener(SWT.Verify, new Listener() {
				public void handleEvent(Event e) {
					try {
						String value = customVersion.getText().substring(0, e.start) + e.text + customVersion.getText().substring(e.end);
						if ( !value.isEmpty() ) {
							Float.valueOf(value);
							String values[] = value.split("\\.");
							if ( values[0].length() > 5 ) throw new Exception();
							if ( values.length == 2 && values[1].length() > 5 ) throw new Exception();

							checkActual.setSelection(false);
							checkMinor.setSelection(false);
							checkMajor.setSelection(false);
							checkCustom.setSelection(true);
						}
					} catch (Exception ee) {
						e.doit = false;
					}
					check();
				}
			});
		}


		Label lblPurpose = new Label(dialog, SWT.NONE);
		lblPurpose.setBounds(426, 284, 55, 15);
		lblPurpose.setText("Purpose :");

		purpose = new Text(dialog, SWT.BORDER | SWT.V_SCROLL);
		purpose.setBounds(519, 281, 265, 155);
		purpose.setEditable(mode == Mode.Export);

		Label lblowner = new Label(dialog, SWT.NONE);
		lblowner.setBounds(426, 241, 55, 15);
		lblowner.setText("owner :");

		owner = new Text(dialog, SWT.BORDER);
		owner.setBounds(521, 236, 134, 21);
		owner.setEditable(mode == Mode.Export);

		Label lblNote = new Label(dialog, SWT.NONE);
		lblNote.setBounds(426, 205, 89, 15);
		lblNote.setText("Release note :");

		note = new Text(dialog, SWT.BORDER);
		note.setBounds(521, 196, 263, 21);
		note.setEditable(mode == Mode.Export);

		Label lblVersion = new Label(dialog, SWT.NONE);
		lblVersion.setBounds(420, 81, 55, 15);
		lblVersion.setText("Version :");

		okButton = new Button(dialog, SWT.PUSH);
		okButton.setBounds(628, 466, 75, 25);
		okButton.setText("OK");
		okButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) {
				modelSelected = new HashMap<String,String>();
				modelSelected.put("id", id.getText());
				modelSelected.put("name", name.getText());
				modelSelected.put("purpose", purpose.getText());
				modelSelected.put("owner", owner.getText());
				modelSelected.put("period", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
				if ( mode == Mode.Import ) {
					modelSelected.put("version", tblVersion.getSelection()[0].getText(0));
				} else {
					if ( checkActual.getSelection() ) {
						modelSelected.put("version", actualVersion.getText());
					} else if ( checkMinor.getSelection() ) {
						modelSelected.put("version", minorVersion.getText());
					} else if ( checkMajor.getSelection() ) {
						modelSelected.put("version", majorVersion.getText());
					} else {
						modelSelected.put("version", customVersion.getText());
					}
				}
				dialog.close();
			}
		});

		cancelButton = new Button(dialog, SWT.PUSH);
		cancelButton.setBounds(709, 466, 75, 25);
		cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) { dialog.close(); }
		});
	}

	/**
	 * Fill-in the table with ID and name of the models 
	 */
	private void setValues() throws SQLException {
		if ( mode == Mode.Export ) {
			id.setText(dbModel.getId());
			name.setText(dbModel.getName());
			purpose.setText(dbModel.getPurpose()!=null ? dbModel.getPurpose() : "");
			minorVersion.setText(DBPlugin.incMinor(dbModel.getVersion()));
			majorVersion.setText(DBPlugin.incMajor(dbModel.getVersion()));
			customVersion.setText("");
			checkMinor.setSelection(true);
		}

		ResultSet result = DBPlugin.select(db, "SELECT m1.model, m1.name, m1.version FROM Model AS m1 JOIN (SELECT model, MAX(version) AS version FROM Model GROUP BY model) AS m2 ON m1.model = m2.model and m1.version = m2.version");
		while(result.next()) {
			TableItem tableItem = new TableItem(tblId, SWT.NONE);
			tableItem.setText(0, result.getString("model"));
			tableItem.setText(1, result.getString("name"));
			if ( mode == Mode.Export  && dbModel.getId().equals(result.getString("model")) ) {
				tblId.setSelection(tableItem);
				tblId.notifyListeners(SWT.Selection, new Event());
				checkActual.setEnabled(true);
				actualVersion.setText(result.getString("version"));
				minorVersion.setText(DBPlugin.incMinor(result.getString("version")));
				majorVersion.setText(DBPlugin.incMajor(result.getString("version")));
			}
		}
		result.close();
		check();
	}

	/**
	 * Sorts the table columns 
	 */
	private MouseAdapter sortColumns = new MouseAdapter() {
		public void mouseDoubleClick(MouseEvent e) {
			Collator collator = Collator.getInstance(Locale.getDefault());
			Table table;
			TableItem[] items;
			TableColumn column;

			if ( e.widget.equals(lblId)) {
				table = tblId;
				column = columnId;
			} else if ( e.widget.equals(lblName)) {
				table = tblId;
				column = columnName;
			} else
				return;

			items = table.getItems();

			if (column == table.getSortColumn()) {
				table.setSortDirection(table.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
			} else {
				table.setSortColumn(column);
				table.setSortDirection(SWT.UP);
			}

			int columnIndex = -1;
			for ( int c=0; c < table.getColumnCount(); c++) {
				if ( column == table.getColumn(c) ) {
					columnIndex = c;
					break;
				}
			}
			if ( columnIndex != -1 ) {
				for (int i = 1; i < items.length; i++) {
					String value1 = items[i].getText(columnIndex);
					for (int j = 0; j < i; j++) {
						String value2 = items[j].getText(columnIndex);
						boolean inf = collator.compare(value1, value2) < 0;
						if ( table.getSortDirection() == SWT.DOWN)
							inf = ! inf;
						if (inf) {
							String[] values = { items[i].getText(0),items[i].getText(1) };
							items[i].dispose();
							TableItem item = new TableItem(table, SWT.NONE, j);
							item.setText(values);
							items = table.getItems();
							break;
						}
					}
				}
			}

		}
	};

	/**
	 * Selects a model in the table
	 */
	private Listener selectModelListener = new Listener() {
		public void handleEvent(Event e) {
			try {
				ResultSet models = DBPlugin.select(db, "SELECT * FROM Model WHERE model = ? ORDER BY version", tblId.getSelection()[0].getText());
				if ( mode == Mode.Import ) tblVersion.removeAll();
				while ( models.next() ) {
					id.setText(models.getString("model") == null ? "" : models.getString("model"));
					name.setText(models.getString("name") == null ? "" : models.getString("name"));
					purpose.setText(models.getString("purpose") == null ? "" : models.getString("purpose"));
					if ( mode == Mode.Export ) {
						owner.setText(models.getString("owner") == null ? "" : models.getString("owner"));
						if ( models.getString("version") == null ) {
							actualVersion.setText(models.getString("version"));
							actualVersion.setEnabled(false);
						}
						minorVersion.setText(DBPlugin.incMinor(models.getString("version")));
						majorVersion.setText(DBPlugin.incMajor(models.getString("version")));
						customVersion.setText("");
						checkMinor.setSelection(true);
					}
					else {

						TableItem tableItem = new TableItem(tblVersion, SWT.NONE);
						tableItem.setText(0, models.getString("version"));
						tableItem.setText(1, models.getString("period"));
						tableItem.setData("name", models.getString("name"));
						tableItem.setData("purpose", models.getString("purpose"));
						tableItem.setData("owner", models.getString("owner"));
						tableItem.setData("note", models.getString("note"));
						tblVersion.setSelection(tableItem);
					}
				}
				models.close();
				check();
			} catch (SQLException ee) {
				Logger.logError("Cannot retreive details about model " + tblId.getSelection()[0].getText(), ee);
			}
		}
	};

	private void check() {
		if ( mode == Mode.Export )
			okButton.setEnabled(!id.getText().isEmpty() && !name.getText().isEmpty() && (!checkCustom.getSelection() || !customVersion.getText().isEmpty()) );
		else
			okButton.setEnabled(!id.getText().isEmpty() && !name.getText().isEmpty());
	}
}