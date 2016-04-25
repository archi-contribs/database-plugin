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
import org.eclipse.swt.graphics.Point;

public class ChooseModel extends Dialog {
	private ResultSet result = null;
	private Shell dialog;
	private IArchimateModel model;
	private Connection db;
	private Mode mode;
	private Table tblId;
	private Table tblVersion;
	private Button btnSame;
	private Button btnMinor;
	private Button btnMajor;
	private Text id;
	private Text name;
	private Text purpose;
	private Text user;
	private Text comment;
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
		
		/*******************/
		
		ScrolledComposite compositeId = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
		compositeId.setAlwaysShowScrollBars(true);
		compositeId.setBounds(10, 10, 400, 450);
		compositeId.setExpandHorizontal(true);
		compositeId.setExpandVertical(true);

		TableViewer tableViewerId = new TableViewer(compositeId, SWT.FULL_SELECTION);
		tblId = tableViewerId.getTable();
		tblId.setHeaderVisible(true);
		tblId.setLinesVisible(true);
		tblId.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				//TODO : show last version
				//TODO : populate version list if import, calculate next minor and major if export (replace current version by default)
				try {
					Statement stmt = db.createStatement();
					ResultSet models = stmt.executeQuery("SELECT * FROM Model WHERE id = '"+tblId.getSelection()[0].getText()+"'");
					if ( models.next() ) {
						id.setText(models.getString("id"));
						name.setText(models.getString("name"));
						if ( models.getString("purpose") != null ) purpose.setText(models.getString("purpose"));
						if ( models.getString("owner") != null ) user.setText(models.getString("owner"));
						if ( models.getDate("creation") != null ) comment.setText(models.getDate("creation").toString());
						okButton.setEnabled(true);
					}
					models.close();
					stmt.close();
				} catch (SQLException ee) {
					Logger.logError("Cannot retreive details about model " + tblId.getSelection()[0].getText(), ee);
				}
			}
		});
		tblId.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				//TODO : replace current version by default
				okButton.notifyListeners(SWT.Selection, new Event());
			}	
		});


		TableColumn columnId = new TableColumn(tblId, SWT.NONE);
		columnId.setResizable(false);
		columnId.setMoveable(true);
		columnId.setWidth(100);
		columnId.setText("ID");
		columnId.addListener(SWT.Selection, sortListener);

		TableColumn columnName = new TableColumn(tblId, SWT.NONE);
		columnName.setResizable(false);
		columnName.setWidth(280);
		columnName.setText("Name");
		columnName.addListener(SWT.Selection, sortListener);

		compositeId.setContent(tblId);
		compositeId.setMinSize(tblId.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		/*******************/
		
		Label lblId = new Label(dialog, SWT.NONE);
		lblId.setBounds(420, 45, 55, 15);
		lblId.setText("ID :");

		id = new Text(dialog, SWT.BORDER);
		id.setEditable(false);
		id.setBounds(521, 42, 132, 21);
		id.setTextLimit(5);
		
		/*******************/
		
		if ( mode == Mode.Import ) {
			ScrolledComposite compositeVersion = new ScrolledComposite(dialog, SWT.BORDER | SWT.V_SCROLL);
			compositeVersion.setExpandVertical(true);
			compositeVersion.setExpandHorizontal(true);
			compositeVersion.setAlwaysShowScrollBars(true);
			compositeVersion.setBounds(521, 81, 265, 56);
			compositeVersion.setMinSize(new Point(397, 41));
			
			TableViewer tableViewerVersion = new TableViewer(compositeVersion, SWT.FULL_SELECTION);
			tblVersion = tableViewerVersion.getTable();
			tblVersion.setEnabled(false);
			tblVersion.setHeaderVisible(true);
			tblVersion.setLinesVisible(true);
			tblVersion.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					//TODO : fill user, comment text fields
				}
			});
			tblVersion.addListener(SWT.MouseDoubleClick, new Listener() {
				public void handleEvent(Event event) {
					okButton.notifyListeners(SWT.Selection, new Event());
				}	
			});
	
	
			TableColumn columnVersion = new TableColumn(tblVersion, SWT.NONE);
			columnVersion.setResizable(false);
			columnVersion.setMoveable(true);
			columnVersion.setWidth(50);
			columnVersion.setText("Version");
			columnVersion.addListener(SWT.Selection, sortListener);
	
			TableColumn columnDate = new TableColumn(tblVersion, SWT.NONE);
			columnDate.setResizable(false);
			columnDate.setWidth(195);
			columnDate.setText("Date");
			columnDate.addListener(SWT.Selection, sortListener);
	
			compositeVersion.setContent(tblVersion);
			compositeVersion.setMinSize(tblVersion.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		} else {
			btnSame = new Button(dialog, SWT.CHECK);
			btnSame.setEnabled(false);
			btnSame.setBounds(521, 80, 55, 16);

			btnMinor = new Button(dialog, SWT.CHECK);
			btnMinor.setEnabled(false);
			btnMinor.setBounds(521, 124, 55, 16);

			btnMajor = new Button(dialog, SWT.CHECK);
			btnMajor.setEnabled(false);
			btnMajor.setBounds(521, 102, 55, 16);
		}

		/*******************/
		
		Label lblName = new Label(dialog, SWT.NONE);
		lblName.setBounds(420, 158, 55, 15);
		lblName.setText("Name :");

		name = new Text(dialog, SWT.BORDER);
		name.setEditable(false);
		name.setBounds(521, 155, 265, 21);

		Label lblPurpose = new Label(dialog, SWT.NONE);
		lblPurpose.setBounds(420, 198, 55, 15);
		lblPurpose.setText("Purpose :");

		purpose = new Text(dialog, SWT.BORDER | SWT.V_SCROLL);
		purpose.setEditable(false);
		purpose.setBounds(521, 195, 265, 155);

		Label lblUser = new Label(dialog, SWT.NONE);
		lblUser.setBounds(420, 373, 55, 15);
		lblUser.setText("User :");

		user = new Text(dialog, SWT.BORDER);
		user.setEditable(false);
		user.setBounds(521, 370, 134, 21);

		Label lblComment = new Label(dialog, SWT.NONE);
		lblComment.setBounds(420, 410, 89, 15);
		lblComment.setText("Comment :");

		comment = new Text(dialog, SWT.BORDER);
		comment.setEditable(false);
		comment.setBounds(521, 407, 263, 21);

		Label lblVersion = new Label(dialog, SWT.NONE);
		lblVersion.setBounds(420, 81, 55, 15);
		lblVersion.setText("Version :");
		
		btnNew = new Button(dialog, SWT.NONE);
		btnNew.setEnabled(false);
		btnNew.setBounds(326, 466, 75, 25);
		btnNew.setText("New ...");

		okButton = new Button(dialog, SWT.PUSH);
		okButton.setEnabled(false);
		okButton.setBounds(628, 466, 75, 25);
		okButton.setText("OK");
		okButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) { this.widgetDefaultSelected(e); }
			public void widgetDefaultSelected(SelectionEvent e) {
				//TODO : show last version
				try {
					Statement stmt = db.createStatement();
					ResultSet models = stmt.executeQuery("SELECT *, false as new FROM Model WHERE id = '"+tblId.getSelection()[0].getText()+"'");
					if ( models.next() ) {
						result = models;
						dialog.close();
					} else {
						// we do not close the ResultSet as we return it
						models.close();
						stmt.close();
					}
				} catch (SQLException ee) {
					Logger.logError("Cannot retreive details about model " + tblId.getSelection()[0].getText(), ee);
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
	}

	private void loadValues() throws SQLException {
		Statement stmt = db.createStatement();
		ResultSet models = stmt.executeQuery("SELECT id, name FROM Model");

		int index=0;
		while(models.next()) {
			TableItem tableItem = new TableItem(tblId, SWT.NONE);
			tableItem.setText(0, models.getString("id").trim());
			tableItem.setText(1, models.getString("name").trim());
			if ( model != null && model.getId().equals(models.getString("id").trim()+"-"+models.getString("id").trim()) ) {
				tblId.setSelection(index);
				tblId.notifyListeners(SWT.Selection, new Event());
			}
			index++;
		}
		models.close();
		stmt.close();
	}

	private Listener sortListener = new Listener() {
		public void handleEvent(Event e) {
			TableItem[] items = tblId.getItems();
			Collator collator = Collator.getInstance(Locale.getDefault());
			TableColumn column = (TableColumn) e.widget;

			if (column == tblId.getSortColumn()) {
				tblId.setSortDirection(tblId.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
			} else {
				tblId.setSortColumn(column);
				tblId.setSortDirection(SWT.UP);
			}

			int columnIndex = -1;
			for ( int c=0; c < tblId.getColumnCount(); c++) {
				if ( column == tblId.getColumn(c) ) {
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
						if ( tblId.getSortDirection() == SWT.DOWN)
							inf = ! inf;
						if (inf) {
							String[] values = { items[i].getText(0),items[i].getText(1) };
							items[i].dispose();
							TableItem item = new TableItem(tblId, SWT.NONE, j);
							item.setText(values);
							items = tblId.getItems();
							break;
						}
					}
				}
			}

		}
	};
}
