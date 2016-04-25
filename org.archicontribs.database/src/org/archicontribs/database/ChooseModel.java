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
import java.sql.Statement;
import java.text.Collator;
import java.util.Locale;

import org.archicontribs.database.DatabasePlugin.Mode;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.util.Logger;
import org.eclipse.swt.widgets.Combo;

public class ChooseModel extends Dialog {
	private ResultSet result = null;
	private Shell dialog;
	private IArchimateModel model;
	private Connection db;
	private Mode mode;
	private Table table;
	private Text id;
	private Text name;
	private Text purpose;
	private Text owner;
	private Text created;
	private Button checkIn;
	private Button btnNew;
	private Button okButton;
	private Button cancelButton;
	

	//TODO: allow to create a new project (ie use of the "new" button")

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ChooseModel(/*Shell parent, int style*/) {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public ResultSet open(Connection _db, Mode _mode, IArchimateModel _model) throws SQLException {
		model = _model;
		return open(_db, _mode);
	}
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public ResultSet open(Connection _db, Mode _mode) throws SQLException {
		db = _db;
		mode = _mode;
		Display display = getParent().getDisplay();
		createContents();
		loadValues();
		dialog.open();
		dialog.layout();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		dialog = new Shell(getParent(), getStyle());
		dialog.setText(DatabasePlugin.pluginTitle);
		if ( mode == Mode.Import )
			dialog.setText("Please choose the model to import ...");
		else
			dialog.setText("Please choose the model to export ...");
		dialog.setSize(800, 525);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 2);
		dialog.setLayout(null);

		okButton = new Button(dialog, SWT.PUSH);
		okButton.setEnabled(false);
		okButton.setBounds(628, 466, 75, 25);
		okButton.setText("OK");
		okButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) {
				try {
					Statement stmt = db.createStatement();
					ResultSet models = stmt.executeQuery("SELECT *, false as new FROM Model WHERE id = '"+table.getSelection()[0].getText()+"'");
					if ( models.next() ) {
						result = models;
						dialog.close();
					} else {
						// we do not close the ResultSet as we return it
						models.close();
						stmt.close();
					}
				} catch (SQLException ee) {
					Logger.logError("Cannot retreive details about model " + table.getSelection()[0].getText(), ee);
				}
			}
		});

		cancelButton = new Button(dialog, SWT.PUSH);
		cancelButton.setBounds(709, 466, 75, 25);
		cancelButton.setText("Cancel");
		cancelButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) { dialog.close(); }
		});

		FormData cancelButtonFormData = new FormData();
		cancelButtonFormData.right = new FormAttachment(100,-2);
		cancelButtonFormData.bottom = new FormAttachment(100,-2);

		FormData okButtonFormData = new FormData();
		okButtonFormData.right = new FormAttachment(cancelButton,-10);
		okButtonFormData.bottom = new FormAttachment(100,-2);

		FormData tableCompositeFormData = new FormData();
		tableCompositeFormData.right = new FormAttachment(100,-2);
		tableCompositeFormData.left = new FormAttachment(0,2);
		tableCompositeFormData.top = new FormAttachment(0,2);
		tableCompositeFormData.bottom = new FormAttachment(cancelButton,-5);

		Label lblId = new Label(dialog, SWT.NONE);
		lblId.setBounds(420, 45, 55, 15);
		lblId.setText("ID :");

		id = new Text(dialog, SWT.BORDER);
		id.setEditable(false);
		id.setBounds(521, 42, 132, 21);

		Label lblName = new Label(dialog, SWT.NONE);
		lblName.setBounds(420, 80, 55, 15);
		lblName.setText("Name :");

		name = new Text(dialog, SWT.BORDER);
		name.setEditable(false);
		name.setBounds(519, 80, 265, 21);

		Label lblPurpose = new Label(dialog, SWT.NONE);
		lblPurpose.setBounds(420, 115, 55, 15);
		lblPurpose.setText("Purpose :");

		purpose = new Text(dialog, SWT.BORDER | SWT.V_SCROLL);
		purpose.setEditable(false);
		purpose.setBounds(521, 112, 265, 155);

		Label lblOwner = new Label(dialog, SWT.NONE);
		lblOwner.setBounds(420, 345, 55, 15);
		lblOwner.setText("Owner :");

		owner = new Text(dialog, SWT.BORDER);
		owner.setEditable(false);
		owner.setBounds(521, 339, 134, 21);

		Label lblCreated = new Label(dialog, SWT.NONE);
		lblCreated.setBounds(420, 380, 89, 15);
		lblCreated.setText("Created :");

		created = new Text(dialog, SWT.BORDER);
		created.setEditable(false);
		created.setBounds(521, 377, 134, 21);

		Label lblCheckIn = new Label(dialog, SWT.NONE);
		lblCheckIn.setBounds(420, 415, 55, 15);
		lblCheckIn.setText("Check in :");

		checkIn = new Button(dialog, SWT.CHECK);
		checkIn.setEnabled(false);
		checkIn.setBounds(521, 414, 13, 16);

		btnNew = new Button(dialog, SWT.NONE);
		btnNew.setEnabled(false);
		btnNew.setBounds(326, 466, 75, 25);
		btnNew.setText("New ...");

		ScrolledComposite scrolledComposite = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
		scrolledComposite.setAlwaysShowScrollBars(true);
		scrolledComposite.setBounds(10, 10, 400, 450);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		TableViewer tableViewer = new TableViewer(scrolledComposite, SWT.FULL_SELECTION);
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				try {
					Statement stmt = db.createStatement();
					ResultSet models = stmt.executeQuery("SELECT * FROM Model WHERE id = '"+table.getSelection()[0].getText()+"'");
					if ( models.next() ) {
						id.setText(models.getString("id"));
						name.setText(models.getString("name"));
						if ( models.getString("purpose") != null ) purpose.setText(models.getString("purpose"));
						if ( models.getString("owner") != null ) owner.setText(models.getString("owner"));
						if ( models.getDate("creation") != null ) created.setText(models.getDate("creation").toString());
						if ( models.getString("checkin") != null ) checkIn.setSelection(!models.getString("checkin").isEmpty());
						okButton.setEnabled(true);
					}
					models.close();
					stmt.close();
				} catch (SQLException ee) {
					Logger.logError("Cannot retreive details about model " + table.getSelection()[0].getText(), ee);
				}
			}
		});
		table.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				okButton.notifyListeners(SWT.Selection, new Event());
			}	
		});


		TableColumn columnId = new TableColumn(table, SWT.NONE);
		columnId.setResizable(false);
		columnId.setMoveable(true);
		columnId.setWidth(100);
		columnId.setText("ID");
		columnId.addListener(SWT.Selection, sortListener);

		TableColumn columnName = new TableColumn(table, SWT.NONE);
		columnName.setResizable(false);
		columnName.setWidth(280);
		columnName.setText("Name");
		columnName.addListener(SWT.Selection, sortListener);

		scrolledComposite.setContent(table);
		scrolledComposite.setMinSize(table.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Label lblVersion = new Label(dialog, SWT.NONE);
		lblVersion.setBounds(420, 296, 55, 15);
		lblVersion.setText("Version :");
		
		if ( mode == Mode.Import ) {
			Combo version = new Combo(dialog, SWT.NONE);
			version.setEnabled(false);
			version.setBounds(520, 302, 91, 23);
		} else {
			Button btnSame = new Button(dialog, SWT.CHECK);
			btnSame.setEnabled(false);
			btnSame.setBounds(521, 280, 55, 16);
			
			Button btnMinor = new Button(dialog, SWT.CHECK);
			btnMinor.setEnabled(false);
			btnMinor.setBounds(521, 295, 55, 16);
			
			Button btnMajor = new Button(dialog, SWT.CHECK);
			btnMajor.setEnabled(false);
			btnMajor.setBounds(521, 310, 55, 16);
		}

	}

	private void loadValues() throws SQLException {
		Statement stmt = db.createStatement();
		ResultSet models = stmt.executeQuery("SELECT id, name FROM Model");

		int index=0;
		while(models.next()) {
			TableItem tableItem = new TableItem(table, SWT.NONE);
			tableItem.setText(0, models.getString("id").trim());
			tableItem.setText(1, models.getString("name").trim());
			if ( model != null && model.getId().equals(models.getString("id").trim()+"-"+models.getString("id").trim()) ) {
				table.setSelection(index);
				table.notifyListeners(SWT.Selection, new Event());
			}
			index++;
		}
		models.close();
		stmt.close();
	}

	private Listener sortListener = new Listener() {
		public void handleEvent(Event e) {
			TableItem[] items = table.getItems();
			Collator collator = Collator.getInstance(Locale.getDefault());
			TableColumn column = (TableColumn) e.widget;

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
}
