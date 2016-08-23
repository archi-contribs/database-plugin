package org.archicontribs.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class DBTabItem {
	private static Display display = Display.getCurrent();
	private static Color RED = display.getSystemColor(SWT.COLOR_RED);
	private Text text;
	private Label countMetadata;
	private Label countFolders;
	private Label countImages;
	private Label countDiagrams;
	private Label countDiagramGroups;
	private Label countDiagramNotes;
	private Label countElements;
	private Label countRelationships;
	private Label countProperties;
	private Label countDiagramObjects;
	private Label countDiagramConnections;
	private Label countDiagramArchimateConnections;
	private Label countDiagramReferences;
	private Label countCanvas;
	private Label countCanvasBlocks;
	private Label countCanvasSticky;
	private Label countCanvasImages;
	private Label countCanvasModelConnections;
	private Label countSketchs;
	private Label countSketchActors;
	private Label countSketchSticky;
	private Label countSketchConnections;
	private Label countBendpoints;
	private ProgressBar progressBar;
	
	DBTabItem (TabFolder tabFolder, String _title) {
		TabItem tabItem = new TabItem(tabFolder, SWT.MULTI);
		tabItem.setText(_title);
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(composite);
		
		text = new Text(composite, SWT.BORDER | SWT.CENTER | SWT.MULTI);
		text.setBounds(10, 10, 787, 60);
		text.setText("Please wait ...");
		text.setEditable(false);

		countMetadata = new Label(composite, SWT.RIGHT);
		countMetadata.setBounds(10, 80, 75, 15);
		countMetadata.setText("");
		Label lblMetadata = new Label(composite, SWT.NONE);
		lblMetadata.setBounds(90, 80, 115, 15);
		lblMetadata.setText("Metadata");
		
		countFolders = new Label(composite, SWT.RIGHT);
		countFolders.setBounds(10, 100, 75, 15);
		countFolders.setText("");
		Label lblFolders = new Label(composite, SWT.NONE);
		lblFolders.setText("Folders");
		lblFolders.setBounds(90, 100, 115, 15);

		countImages = new Label(composite, SWT.RIGHT);
		countImages.setBounds(10, 120, 75, 15);
		countImages.setText("");
		Label lblImages = new Label(composite, SWT.NONE);
		lblImages.setText("Images");
		lblImages.setBounds(90, 120, 115, 15);
		
		countDiagrams = new Label(composite, SWT.RIGHT);
		countDiagrams.setText("");
		countDiagrams.setBounds(190, 80, 75, 15);
		Label lblDiagrams = new Label(composite, SWT.NONE);
		lblDiagrams.setText("Diagrams");
		lblDiagrams.setBounds(270, 80, 115, 15);
		
		countDiagramGroups = new Label(composite, SWT.RIGHT);
		countDiagramGroups.setText("");
		countDiagramGroups.setBounds(190, 100, 75, 15);
		Label lblDiagramGroups = new Label(composite, SWT.NONE);
		lblDiagramGroups.setText("Diagram groups");
		lblDiagramGroups.setBounds(270, 100, 115, 15);
		
		countDiagramNotes = new Label(composite, SWT.RIGHT);
		countDiagramNotes.setText("");
		countDiagramNotes.setBounds(190, 120, 75, 15);
		Label lblDiagramObjects = new Label(composite, SWT.NONE);
		lblDiagramObjects.setText("Diagram notes");
		lblDiagramObjects.setBounds(270, 120, 115, 15);
		
		countElements = new Label(composite, SWT.RIGHT);
		countElements.setText("");
		countElements.setBounds(10, 140, 75, 15);
		Label lblElements = new Label(composite, SWT.NONE);
		lblElements.setText("Elements");
		lblElements.setBounds(90, 141, 115, 15);
		
		countRelationships = new Label(composite, SWT.RIGHT);
		countRelationships.setText("");
		countRelationships.setBounds(10, 160, 75, 15);
		Label lblRelationships = new Label(composite, SWT.NONE);
		lblRelationships.setText("Relationships");
		lblRelationships.setBounds(90, 160, 115, 15);
		
		countProperties = new Label(composite, SWT.RIGHT);
		countProperties.setText("");
		countProperties.setBounds(10, 180, 75, 15);
		Label lblProperties = new Label(composite, SWT.NONE);
		lblProperties.setText("Properties");
		lblProperties.setBounds(90, 180, 115, 15);
		
		countDiagramObjects = new Label(composite, SWT.RIGHT);
		countDiagramObjects.setText("");
		countDiagramObjects.setBounds(190, 140, 75, 15);
		Label lblDiagramObjetcs = new Label(composite, SWT.NONE);
		lblDiagramObjetcs.setText("Diagram objects");
		lblDiagramObjetcs.setBounds(270, 140, 115, 15);
		
		countDiagramConnections = new Label(composite, SWT.RIGHT);
		countDiagramConnections.setText("");
		countDiagramConnections.setBounds(190, 160, 75, 15);
		Label lblDiagramConnections = new Label(composite, SWT.NONE);
		lblDiagramConnections.setText("Diagram connections");
		lblDiagramConnections.setBounds(270, 160, 115, 15);
		
		countDiagramArchimateConnections = new Label(composite, SWT.RIGHT);
		countDiagramArchimateConnections.setText("");
		countDiagramArchimateConnections.setBounds(190, 180, 75, 15);
		Label lblDiagramArchimateConnections = new Label(composite, SWT.NONE);
		lblDiagramArchimateConnections.setText("Diagram Archimate connections");
		lblDiagramArchimateConnections.setBounds(270, 180, 180, 15);
		
		countDiagramReferences = new Label(composite, SWT.RIGHT);
		countDiagramReferences.setText("");
		countDiagramReferences.setBounds(190, 200, 75, 15);
		Label lblDiagramReferences = new Label(composite, SWT.NONE);
		lblDiagramReferences.setText("Diagram references");
		lblDiagramReferences.setBounds(270, 200, 115, 15);
		
		countCanvas = new Label(composite, SWT.RIGHT);
		countCanvas.setText("");
		countCanvas.setBounds(410, 80, 75, 15);
		Label lblCanvas = new Label(composite, SWT.NONE);
		lblCanvas.setText("Canvas");
		lblCanvas.setBounds(490, 80, 115, 15);
		
		countCanvasBlocks = new Label(composite, SWT.RIGHT);
		countCanvasBlocks.setText("");
		countCanvasBlocks.setBounds(410, 100, 75, 15);
		Label lblCanvasBlocks = new Label(composite, SWT.NONE);
		lblCanvasBlocks.setText("Canvas blocks");
		lblCanvasBlocks.setBounds(490, 100, 115, 15);
		
		countCanvasSticky = new Label(composite, SWT.RIGHT);
		countCanvasSticky.setText("");
		countCanvasSticky.setBounds(410, 120, 75, 15);
		Label lblCanvasSticky = new Label(composite, SWT.NONE);
		lblCanvasSticky.setText("Canvas sticky");
		lblCanvasSticky.setBounds(490, 120, 115, 15);
		
		countCanvasImages = new Label(composite, SWT.RIGHT);
		countCanvasImages.setText("");
		countCanvasImages.setBounds(410, 140, 75, 15);
		Label lblCanvasImages = new Label(composite, SWT.NONE);
		lblCanvasImages.setText("Canvas images");
		lblCanvasImages.setBounds(490, 140, 115, 15);
		
		countCanvasModelConnections = new Label(composite, SWT.RIGHT);
		countCanvasModelConnections.setText("");
		countCanvasModelConnections.setBounds(410, 160, 75, 15);
		Label lblCanvasConnections = new Label(composite, SWT.NONE);
		lblCanvasConnections.setText("Canvas connections");
		lblCanvasConnections.setBounds(490, 160, 115, 15);
		
		countSketchs = new Label(composite, SWT.RIGHT);
		countSketchs.setText("");
		countSketchs.setBounds(612, 80, 75, 15);
		Label lblSketchs = new Label(composite, SWT.NONE);
		lblSketchs.setText("Sketchs");
		lblSketchs.setBounds(692, 80, 115, 15);
		
		countSketchActors = new Label(composite, SWT.RIGHT);
		countSketchActors.setText("");
		countSketchActors.setBounds(612, 100, 75, 15);
		Label lblSketchActors = new Label(composite, SWT.NONE);
		lblSketchActors.setText("Sketch actors");
		lblSketchActors.setBounds(692, 100, 115, 15);
		
		countSketchSticky = new Label(composite, SWT.RIGHT);
		countSketchSticky.setText("");
		countSketchSticky.setBounds(612, 120, 75, 15);
		Label lblSketchSticky = new Label(composite, SWT.NONE);
		lblSketchSticky.setText("Sketch sticky");
		lblSketchSticky.setBounds(692, 120, 115, 15);
		
		countSketchConnections = new Label(composite, SWT.RIGHT);
		countSketchConnections.setText("");
		countSketchConnections.setBounds(612, 140, 75, 15);
		Label lblSketchConnections = new Label(composite, SWT.NONE);
		lblSketchConnections.setText("Sketch connections");
		lblSketchConnections.setBounds(692, 140, 115, 15);
		
		countBendpoints = new Label(composite, SWT.RIGHT);
		countBendpoints.setText("");
		countBendpoints.setBounds(10, 200, 75, 15);
		Label lblBendpoints = new Label(composite, SWT.NONE);
		lblBendpoints.setText("Bendpoints");
		lblBendpoints.setBounds(90, 200, 115, 15);
		
		progressBar = new ProgressBar(composite, SWT.NONE); /*SWT.SMOOTH*/
		progressBar.setBounds(50, 237, 710, 17);
		progressBar.setMinimum(0);
		
		redraw();
	}
	
	private void redraw() {
		while( display.readAndDispatch() ) ;
	}
	
	public void setText(String _text) {
		text.setText(_text);
		redraw();
	}
	
	public void setMaximum(int _max) {
		progressBar.setMaximum(_max);
		redraw();
	}
	
	public void finish() {
		progressBar.setVisible(false);
		redraw();
	}
	
	public void setCountSketchModels(String _text) { countSketchs.setText(_text); countSketchs.setForeground(RED); redraw(); }
	public void setCountSketchModels(int _value) { countSketchs.setText(String.valueOf(_value)); redraw(); }
	
	public void setCountCanvasModels(String _text) { countCanvas.setText(_text); countCanvas.setForeground(RED); redraw(); }
	public void setCountCanvasModels(int _value) { countCanvas.setText(String.valueOf(_value)); redraw(); }
	
	public void setCountArchimateDiagramModels(String _text) { countDiagrams.setText(_text); countDiagrams.setForeground(RED); redraw(); }
	public void setCountArchimateDiagramModels(int _value) { countDiagrams.setText(String.valueOf(_value)); redraw(); }
	
	public void setCountElements(String _text) { countElements.setText(_text); countElements.setForeground(RED);  redraw(); }
	public void setCountElements(int _value) { countElements.setText(String.valueOf(_value)); redraw(); }
	
	public void setCountRelationships(String _text) { countRelationships.setText(_text); countRelationships.setForeground(RED);  redraw(); }
	public void setCountRelationships(int _value) { countRelationships.setText(String.valueOf(_value)); redraw(); }

	public void setCountMetadatas(String _text) { countMetadata.setText(_text); countMetadata.setForeground(RED);  redraw(); }
	public void setCountMetadatas(int _value) { countMetadata.setText(String.valueOf(_value)); redraw(); }
	
	public void setCountProperties(String _text) { countProperties.setText(_text); countProperties.setForeground(RED);  redraw(); }
	public void setCountProperties(int _value) { countProperties.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelArchimateObjects(String _text) { countDiagramObjects.setText(_text); countDiagramObjects.setForeground(RED);  redraw(); }
	public void setCountDiagramModelArchimateObjects(int _value) { countDiagramObjects.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelGroups(String _text) { countDiagramGroups.setText(_text); countDiagramGroups.setForeground(RED);  redraw(); }
	public void setCountDiagramModelGroups(int _value) { countDiagramGroups.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelNotes(String _text) { countDiagramNotes.setText(_text); countDiagramNotes.setForeground(RED);  redraw(); }
	public void setCountDiagramModelNotes(int _value) { countDiagramNotes.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelConnections(String _text) { countDiagramConnections.setText(_text); countDiagramConnections.setForeground(RED);  redraw(); }
	public void setCountDiagramModelConnections(int _value) { countDiagramConnections.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelArchimateConnections(String _text) { countDiagramArchimateConnections.setText(_text); countDiagramArchimateConnections.setForeground(RED);  redraw(); }
	public void setCountDiagramModelArchimateConnections(int _value) { countDiagramArchimateConnections.setText(String.valueOf(_value)); redraw(); }

	public void setCountCanvasModelConnections(String _text) { countCanvasModelConnections.setText(_text); countCanvasModelConnections.setForeground(RED);  redraw(); }
	public void setCountCanvasModelConnections(int _value) { countCanvasModelConnections.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelReferences(String _text) { countDiagramReferences.setText(_text); countDiagramReferences.setForeground(RED);  redraw(); }
	public void setCountDiagramModelReferences(int _value) { countDiagramReferences.setText(String.valueOf(_value)); redraw(); }

	public void setCountCanvasModelBlocks(String _text) { countCanvasBlocks.setText(_text); countCanvasBlocks.setForeground(RED);  redraw(); }
	public void setCountCanvasModelBlocks(int _value) { countCanvasBlocks.setText(String.valueOf(_value)); redraw(); }

	public void setCountCanvasModelStickys(String _text) { countCanvasSticky.setText(_text); countCanvasSticky.setForeground(RED);  redraw(); }
	public void setCountCanvasModelStickys(int _value) { countCanvasSticky.setText(String.valueOf(_value)); redraw(); }

	public void setCountCanvasModelImages(String _text) { countCanvasImages.setText(_text); countCanvasImages.setForeground(RED);  redraw(); }
	public void setCountCanvasModelImages(int _value) { countCanvasImages.setText(String.valueOf(_value)); redraw(); }

	public void setCountSketchModelStickys(String _text) { countSketchSticky.setText(_text); countSketchSticky.setForeground(RED);  redraw(); }
	public void setCountSketchModelStickys(int _value) { countSketchSticky.setText(String.valueOf(_value)); redraw(); }

	public void setCountSketchModelActors(String _text) { countSketchActors.setText(_text); countSketchActors.setForeground(RED);  redraw(); }
	public void setCountSketchModelActors(int _value) { countSketchActors.setText(String.valueOf(_value)); redraw(); }

	public void setCountDiagramModelBendpoints(String _text) { countBendpoints.setText(_text); countBendpoints.setForeground(RED);  redraw(); }
	public void setCountDiagramModelBendpoints(int _value) { countBendpoints.setText(String.valueOf(_value)); redraw(); }

	public void setCountFolders(String _text) { countFolders.setText(_text); countFolders.setForeground(RED);  redraw(); }
	public void setCountFolders(int _value) { countFolders.setText(String.valueOf(_value)); redraw(); }

	public void setCountImages(String _text) { countImages.setText(_text); countImages.setForeground(RED);  redraw(); }
	public void setCountImages(int _value) { countImages.setText(String.valueOf(_value)); redraw(); }
	
	public void setProgressBar(int _value) { progressBar.setSelection(_value); }
}
