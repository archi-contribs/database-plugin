package org.archicontribs.database;

import java.awt.Toolkit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Button;

public class DBProgress extends Dialog {
	private Shell dialog;
	private Text text;
	
	/**
	 * Create the dialog.
	 */
	public DBProgress(/*Shell parent, int style*/) {
		super(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}
	
	public void open(Thread longRunningOperation) {
		Display display = getParent().getDisplay();
		createContents();
		longRunningOperation.start();
		dialog.open();
		dialog.layout();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return;
	}
	
	private void createContents() {
		dialog = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText(DBPlugin.pluginTitle);
		dialog.setSize(850, 380);
		dialog.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - dialog.getSize().x) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - dialog.getSize().y) / 2);
		dialog.setLayout(null);
		
		TabFolder tabFolder = new TabFolder(dialog, SWT.NONE);
		tabFolder.setBounds(10, 10, 825, 300);
		
		createTab(tabFolder);
		
		Button btnNewButton = new Button(dialog, SWT.NONE);
		btnNewButton.setBounds(759, 316, 75, 25);
		btnNewButton.setText("Close");
	}
	
	private void createTab(TabFolder tabFolder) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText("New Item");
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(composite);
		
		text = new Text(composite, SWT.BORDER | SWT.CENTER | SWT.MULTI);
		text.setBounds(10, 10, 787, 45);
		
		Label lblNewLabel = new Label(composite, SWT.CENTER);
		lblNewLabel.setBounds(10, 80, 85, 15);
		lblNewLabel.setText("0 / 0");
		
		Label lblNewLabel_1 = new Label(composite, SWT.CENTER);
		lblNewLabel_1.setBounds(10, 100, 85, 15);
		lblNewLabel_1.setText("0 / 0");
		
		Label lblNewLabel_2 = new Label(composite, SWT.CENTER);
		lblNewLabel_2.setBounds(10, 120, 85, 15);
		lblNewLabel_2.setText("0 / 0");
		
		Label lblNewLabel_3 = new Label(composite, SWT.NONE);
		lblNewLabel_3.setBounds(90, 80, 115, 15);
		lblNewLabel_3.setText("Metadata");
		
		Label lblFolders = new Label(composite, SWT.NONE);
		lblFolders.setText("Folders");
		lblFolders.setBounds(90, 100, 115, 15);
		
		Label lblImages = new Label(composite, SWT.NONE);
		lblImages.setText("Images");
		lblImages.setBounds(90, 120, 115, 15);
		
		Label label = new Label(composite, SWT.CENTER);
		label.setText("0 / 0");
		label.setBounds(190, 80, 85, 15);
		
		Label lblElements = new Label(composite, SWT.NONE);
		lblElements.setText("Diagrams");
		lblElements.setBounds(270, 80, 115, 15);
		
		Label label_2 = new Label(composite, SWT.CENTER);
		label_2.setText("0 / 0");
		label_2.setBounds(190, 100, 85, 15);
		
		Label lblDiagramGroups = new Label(composite, SWT.NONE);
		lblDiagramGroups.setText("Diagram groups");
		lblDiagramGroups.setBounds(270, 100, 115, 15);
		
		Label label_4 = new Label(composite, SWT.CENTER);
		label_4.setText("0 / 0");
		label_4.setBounds(190, 120, 85, 15);
		
		Label lblDiagramObjects = new Label(composite, SWT.NONE);
		lblDiagramObjects.setText("Diagram notes");
		lblDiagramObjects.setBounds(270, 120, 115, 15);
		
		Label label_6 = new Label(composite, SWT.CENTER);
		label_6.setText("0 / 0");
		label_6.setBounds(10, 140, 85, 15);
		
		Label lblElements_1 = new Label(composite, SWT.NONE);
		lblElements_1.setText("Elements");
		lblElements_1.setBounds(90, 141, 115, 15);
		
		Label label_8 = new Label(composite, SWT.CENTER);
		label_8.setText("0 / 0");
		label_8.setBounds(10, 160, 85, 15);
		
		Label lblRelationships = new Label(composite, SWT.NONE);
		lblRelationships.setText("Relationships");
		lblRelationships.setBounds(90, 160, 115, 15);
		
		Label label_10 = new Label(composite, SWT.CENTER);
		label_10.setText("0 / 0");
		label_10.setBounds(10, 180, 85, 15);
		
		Label lblProperties = new Label(composite, SWT.NONE);
		lblProperties.setText("Properties");
		lblProperties.setBounds(90, 180, 115, 15);
		
		Label label_12 = new Label(composite, SWT.CENTER);
		label_12.setText("0 / 0");
		label_12.setBounds(190, 140, 85, 15);
		
		Label lblDiagramObjetcs = new Label(composite, SWT.NONE);
		lblDiagramObjetcs.setText("Diagram objects");
		lblDiagramObjetcs.setBounds(270, 140, 115, 15);
		
		Label label_14 = new Label(composite, SWT.CENTER);
		label_14.setText("0 / 0");
		label_14.setBounds(190, 160, 85, 15);
		
		Label lblDiagramConnections = new Label(composite, SWT.NONE);
		lblDiagramConnections.setText("Diagram connections");
		lblDiagramConnections.setBounds(270, 160, 115, 15);
		
		Label label_16 = new Label(composite, SWT.CENTER);
		label_16.setText("0 / 0");
		label_16.setBounds(190, 180, 85, 15);
		
		Label lblDiagramReferences = new Label(composite, SWT.NONE);
		lblDiagramReferences.setText("Diagram references");
		lblDiagramReferences.setBounds(270, 180, 115, 15);
		
		Label label_18 = new Label(composite, SWT.CENTER);
		label_18.setText("0 / 0");
		label_18.setBounds(410, 80, 85, 15);
		
		Label lblCanvas = new Label(composite, SWT.NONE);
		lblCanvas.setText("Canvas");
		lblCanvas.setBounds(490, 80, 115, 15);
		
		Label label_20 = new Label(composite, SWT.CENTER);
		label_20.setText("0 / 0");
		label_20.setBounds(410, 100, 85, 15);
		
		Label lblCanvasBlocks = new Label(composite, SWT.NONE);
		lblCanvasBlocks.setText("Canvas blocks");
		lblCanvasBlocks.setBounds(490, 100, 115, 15);
		
		Label label_22 = new Label(composite, SWT.CENTER);
		label_22.setText("0 / 0");
		label_22.setBounds(410, 120, 85, 15);
		
		Label lblCanvasSticky = new Label(composite, SWT.NONE);
		lblCanvasSticky.setText("Canvas sticky");
		lblCanvasSticky.setBounds(490, 120, 115, 15);
		
		Label label_24 = new Label(composite, SWT.CENTER);
		label_24.setText("0 / 0");
		label_24.setBounds(410, 140, 85, 15);
		
		Label lblCanvasImages = new Label(composite, SWT.NONE);
		lblCanvasImages.setText("Canvas images");
		lblCanvasImages.setBounds(490, 140, 115, 15);
		
		Label label_26 = new Label(composite, SWT.CENTER);
		label_26.setText("0 / 0");
		label_26.setBounds(410, 160, 85, 15);
		
		Label lblCanvasConnections = new Label(composite, SWT.NONE);
		lblCanvasConnections.setText("Canvas connections");
		lblCanvasConnections.setBounds(490, 160, 115, 15);
		
		Label label_28 = new Label(composite, SWT.CENTER);
		label_28.setText("0 / 0");
		label_28.setBounds(410, 180, 85, 15);
		
		Label label_29 = new Label(composite, SWT.NONE);
		label_29.setText("Images");
		label_29.setBounds(490, 180, 115, 15);
		
		Label label_1 = new Label(composite, SWT.CENTER);
		label_1.setText("0 / 0");
		label_1.setBounds(612, 80, 85, 15);
		
		Label label_3 = new Label(composite, SWT.CENTER);
		label_3.setText("0 / 0");
		label_3.setBounds(612, 100, 85, 15);
		
		Label label_5 = new Label(composite, SWT.CENTER);
		label_5.setText("0 / 0");
		label_5.setBounds(612, 120, 85, 15);
		
		Label label_7 = new Label(composite, SWT.CENTER);
		label_7.setText("0 / 0");
		label_7.setBounds(612, 140, 85, 15);
		
		Label lblSketchs = new Label(composite, SWT.NONE);
		lblSketchs.setText("Sketchs");
		lblSketchs.setBounds(692, 80, 115, 15);
		
		Label lblSketchActors = new Label(composite, SWT.NONE);
		lblSketchActors.setText("Sketch actors");
		lblSketchActors.setBounds(692, 100, 115, 15);
		
		Label lblSketchSticky = new Label(composite, SWT.NONE);
		lblSketchSticky.setText("Sketch sticky");
		lblSketchSticky.setBounds(692, 120, 115, 15);
		
		Label lblSketchConnections = new Label(composite, SWT.NONE);
		lblSketchConnections.setText("Sketch connections");
		lblSketchConnections.setBounds(692, 140, 115, 15);
		
		Label label_9 = new Label(composite, SWT.CENTER);
		label_9.setText("0 / 0");
		label_9.setBounds(10, 200, 85, 15);
		
		Label lblBendpoints = new Label(composite, SWT.NONE);
		lblBendpoints.setText("Bendpoints");
		lblBendpoints.setBounds(90, 200, 115, 15);
		
		ProgressBar progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setBounds(50, 237, 710, 17);
	}
}

