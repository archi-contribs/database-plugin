package org.archicontribs.database;

import org.archicontribs.database.DBPlugin.DebugLevel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class DBProgressTabItem {
	private TabItem tabItem;
	private Text text;
	private Label countMetadatas;
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
	private Label countBendpoints;
	private ProgressBar progressBar;
	
	private static final Color COLOR_RED = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
	private static final Color COLOR_BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color COLOR_GREEN = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	private static final Color COLOR_ORANGE = new Color(Display.getCurrent(), 255, 140, 0);
	
	private Image IMAGE_OK = new Image(Display.getCurrent(), getClass().getResourceAsStream("/img/Ok.png"));
	private Image IMAGE_ERROR = new Image(Display.getCurrent(), getClass().getResourceAsStream("/img/Error.png"));
	private Image IMAGE_CLOCK = new Image(Display.getCurrent(), getClass().getResourceAsStream("/img/Clock.png"));
	private Image IMAGE_HAND = new Image(Display.getCurrent(), getClass().getResourceAsStream("/img/Hand.png"));
	
	DBProgressTabItem (TabFolder tabFolder, String _title) {
		DBPlugin.debug(DebugLevel.MainMethod, "+Entering DBProgressTabItem.DBProgressTabItem(\""+_title+"\")");

		tabItem = new TabItem(tabFolder, SWT.MULTI);
		tabItem.setText(_title);
		
		Composite composite = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(composite);
		
		text = DBPlugin.createText(composite, 10, 10, 787, 60, "Please wait ...", SWT.BORDER | SWT.CENTER | SWT.MULTI);
		text.setEditable(false);

		countMetadatas = DBPlugin.createLabel(composite, 10, 80, 75, 15, "", SWT.RIGHT);						countMetadatas.setData("name","Metadatas");
		countFolders = DBPlugin.createLabel(composite, 10, 100, 75, 15, "", SWT.RIGHT);							countFolders.setData("name","Folders");
		countImages = DBPlugin.createLabel(composite, 10, 120, 75, 15, "", SWT.RIGHT);							countImages.setData("name","Images");
		countDiagrams = DBPlugin.createLabel(composite, 190, 80, 75, 15, "", SWT.RIGHT);						countDiagrams.setData("name","Diagrams");
		countDiagramGroups = DBPlugin.createLabel(composite, 190, 100, 75, 15, "", SWT.RIGHT);					countDiagramGroups.setData("name","Diagram groups");
		countDiagramNotes = DBPlugin.createLabel(composite, 190, 120, 75, 15, "", SWT.RIGHT);					countDiagramNotes.setData("name","Diagram notes");
		countElements = DBPlugin.createLabel(composite, 10, 140, 75, 15, "", SWT.RIGHT);						countElements.setData("name","Elements");
		countRelationships = DBPlugin.createLabel(composite, 10, 160, 75, 15, "", SWT.RIGHT);					countRelationships.setData("name","Relationships");
		countProperties = DBPlugin.createLabel(composite, 10, 180, 75, 15, "", SWT.RIGHT);						countProperties.setData("name","Properties");
		countDiagramObjects = DBPlugin.createLabel(composite, 190, 140, 75, 15, "", SWT.RIGHT);					countDiagramObjects.setData("name","Diagram objects");
		countDiagramConnections = DBPlugin.createLabel(composite, 190, 160, 75, 15, "", SWT.RIGHT);				countDiagramConnections.setData("name","Diagram connections");
		countDiagramArchimateConnections = DBPlugin.createLabel(composite, 190, 180, 75, 15, "", SWT.RIGHT);	countDiagramArchimateConnections.setData("name","Diagram Archimate connections");
		countDiagramReferences = DBPlugin.createLabel(composite, 190, 200, 75, 15, "", SWT.RIGHT);				countDiagramReferences.setData("name","Diagram references");
		countCanvas = DBPlugin.createLabel(composite, 410, 80, 75, 15, "", SWT.RIGHT);							countCanvas.setData("name","Canvas");
		countCanvasBlocks = DBPlugin.createLabel(composite, 410, 100, 75, 15, "", SWT.RIGHT);					countCanvasBlocks.setData("name","Canvas blocks");
		countCanvasSticky = DBPlugin.createLabel(composite, 410, 120, 75, 15, "", SWT.RIGHT);					countCanvasSticky.setData("name","Canvas sticky");
		countCanvasImages = DBPlugin.createLabel(composite, 410, 140, 75, 15, "", SWT.RIGHT);					countCanvasImages.setData("name","Canvas images");
		countCanvasModelConnections = DBPlugin.createLabel(composite, 410, 160, 75, 15, "", SWT.RIGHT);			countCanvasModelConnections.setData("name","Canvas connections");
		countSketchActors = DBPlugin.createLabel(composite, 612, 100, 75, 15, "", SWT.RIGHT);					countSketchActors.setData("name","Sketch actors");
		countSketchs = DBPlugin.createLabel(composite, 612, 80, 75, 15, "", SWT.RIGHT);							countSketchs.setData("name","Sketchs");
		countSketchSticky = DBPlugin.createLabel(composite, 612, 120, 75, 15, "", SWT.RIGHT);					countSketchSticky.setData("name","Sketch sticky");
		countBendpoints = DBPlugin.createLabel(composite, 10, 200, 75, 15, "", SWT.RIGHT);						countBendpoints.setData("name","Bendpoints");
		
		DBPlugin.createLabel(composite, 90, 80, 115, 15, "Metadatas", SWT.NONE);
		DBPlugin.createLabel(composite, 90, 100, 115, 15, "Folders", SWT.NONE);
		DBPlugin.createLabel(composite, 90, 120, 115, 15, "Images", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 80, 115, 15, "Diagrams", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 100, 115, 15, "Diagram groups", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 120, 115, 15, "Diagram notes", SWT.NONE);
		DBPlugin.createLabel(composite, 90, 141, 115, 15, "Elements", SWT.NONE);
		DBPlugin.createLabel(composite, 90, 160, 115, 15, "Relationships", SWT.NONE);
		DBPlugin.createLabel(composite, 90, 180, 115, 15, "Properties", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 140, 115, 15, "Diagram objects", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 160, 115, 15, "Diagram connections", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 180, 180, 15, "Diagram Archimate connections", SWT.NONE);
		DBPlugin.createLabel(composite, 270, 200, 115, 15, "Diagram references", SWT.NONE);
		DBPlugin.createLabel(composite, 490, 80, 115, 15, "Canvas", SWT.NONE);
		DBPlugin.createLabel(composite, 490, 100, 115, 15, "Canvas blocks", SWT.NONE);
		DBPlugin.createLabel(composite, 490, 120, 115, 15, "Canvas sticky", SWT.NONE);
		DBPlugin.createLabel(composite, 490, 140, 115, 15, "Canvas images", SWT.NONE);
		DBPlugin.createLabel(composite, 490, 160, 115, 15, "Canvas connections", SWT.NONE);
		DBPlugin.createLabel(composite, 692, 80, 115, 15, "Sketchs", SWT.NONE);
		DBPlugin.createLabel(composite, 692, 100, 115, 15, "Sketch actors", SWT.NONE);
		DBPlugin.createLabel(composite, 692, 120, 115, 15, "Sketch sticky", SWT.NONE);
		DBPlugin.createLabel(composite, 90, 200, 115, 15, "Bendpoints", SWT.NONE);

		progressBar = new ProgressBar(composite, SWT.NONE); /*SWT.SMOOTH*/
		progressBar.setBounds(50, 237, 710, 17);
		progressBar.setMinimum(0);
		
		redraw();
		
		DBPlugin.debug(DebugLevel.MainMethod, "-Leaving DBProgressTabItem.DBProgressTabItem(\""+_title+"\")");
	}
	
	private void redraw() {
		while( Display.getCurrent().readAndDispatch() ) ;
	}
	
	public void setText(String _text) {
		DBPlugin.debug(DebugLevel.Variable, _text);
		text.setForeground(COLOR_BLACK);
		text.setText(_text);
		redraw();
	}
	
	public void setError(String _text) {
		DBPlugin.debug(DebugLevel.Variable, "ERROR " + _text);
		tabItem.setImage(IMAGE_ERROR);
		text.setForeground(COLOR_RED);
		text.setText(_text);
		redraw();
	}
	
	public void setWarning(String _text) {
		tabItem.setImage(IMAGE_HAND);
		text.setForeground(COLOR_ORANGE);
		text.setText(_text);
		redraw();
	}
	
	public void setSuccess(String _text) {
		DBPlugin.debug(DebugLevel.Variable, "SUCCESS : " + _text);
		tabItem.setImage(IMAGE_OK);
		text.setForeground(COLOR_GREEN);
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
	
	private void setValue(Label label, int _value) {
		label.setForeground(COLOR_BLACK);
		label.setText(String.valueOf(_value));
		redraw();
	}
	private void setValue(Label label, int _value, int _expected) {
		if ( _value == _expected) {
			DBPlugin.debug(DebugLevel.Variable, tabItem.getText() + " : " + label.getData("name") + " = " + _value);
			label.setText(String.valueOf(_value));
		}
		else {
			DBPlugin.debug(DebugLevel.Variable, tabItem.getText() + " : " + label.getData("name") + " = " + _value + " / " + _expected);
			label.setForeground(COLOR_RED);
			label.setText(String.valueOf(_value) + " / " + String.valueOf(_expected));
		}
		redraw();
	}
	
	public void setProgressBar(int _value) {
		if ( tabItem.getImage() != IMAGE_CLOCK )
			tabItem.setImage(IMAGE_CLOCK);
		progressBar.setSelection(_value);
	}
	
	public void setCountSketchModels(int _value)									{ setValue(countSketchs, _value); }
	public void setCountSketchModels(int _value, int _expected)						{ setValue(countSketchs, _value, _expected); }
	
	public void setCountCanvasModels(int _value)									{ setValue(countCanvas, _value); }
	public void setCountCanvasModels(int _value, int _expected)						{ setValue(countCanvas, _value, _expected); }
	
	public void setCountArchimateDiagramModels(int _value)							{ setValue(countDiagrams, _value); }
	public void setCountArchimateDiagramModels(int _value, int _expected)			{ setValue(countDiagrams, _value, _expected); }
	
	public void setCountElements(int _value)										{ setValue(countElements, _value); }
	public void setCountElements(int _value, int _expected)							{ setValue(countElements, _value, _expected); }
	
	public void setCountRelationships(int _value)									{ setValue(countRelationships, _value); }
	public void setCountRelationships(int _value, int _expected)					{ setValue(countRelationships, _value, _expected); }

	public void setCountMetadatas(int _value)										{ setValue(countMetadatas, _value); }
	public void setCountMetadatas(int _value, int _expected) 						{ setValue(countMetadatas, _value, _expected); }
	
	public void setCountProperties(int _value)										{ setValue(countProperties, _value); }
	public void setCountProperties(int _value, int _expected)						{ setValue(countProperties, _value, _expected); }

	public void setCountDiagramModelArchimateObjects(int _value)					{ setValue(countDiagramObjects, _value); }
	public void setCountDiagramModelArchimateObjects(int _value, int _expected)		{ setValue(countDiagramObjects, _value, _expected); }

	public void setCountDiagramModelGroups(int _value)								{ setValue(countDiagramGroups, _value); }
	public void setCountDiagramModelGroups(int _value, int _expected)				{ setValue(countDiagramGroups, _value, _expected); }

	public void setCountDiagramModelNotes(int _value)								{ setValue(countDiagramNotes, _value); }
	public void setCountDiagramModelNotes(int _value, int _expected)				{ setValue(countDiagramNotes, _value, _expected); }
	
	public void setCountDiagramModelConnections(int _value)							{ setValue(countDiagramConnections, _value); }
	public void setCountDiagramModelConnections(int _value, int _expected)			{ setValue(countDiagramConnections, _value, _expected); }

	public void setCountDiagramModelArchimateConnections(int _value)				{ setValue(countDiagramArchimateConnections, _value); }
	public void setCountDiagramModelArchimateConnections(int _value, int _expected)	{ setValue(countDiagramArchimateConnections, _value, _expected); }

	public void setCountCanvasModelConnections(int _value)							{ setValue(countCanvasModelConnections, _value); }
	public void setCountCanvasModelConnections(int _value, int _expected)			{ setValue(countCanvasModelConnections, _value, _expected); }

	public void setCountDiagramModelReferences(int _value)							{ setValue(countDiagramReferences, _value); }
	public void setCountDiagramModelReferences(int _value, int _expected)			{ setValue(countDiagramReferences, _value, _expected); }

	public void setCountCanvasModelBlocks(int _value)								{ setValue(countCanvasBlocks, _value); }
	public void setCountCanvasModelBlocks(int _value, int _expected)				{ setValue(countCanvasBlocks, _value, _expected); }

	public void setCountCanvasModelStickys(int _value)								{ setValue(countCanvasSticky, _value); }
	public void setCountCanvasModelStickys(int _value, int _expected)				{ setValue(countCanvasSticky, _value, _expected); }

	public void setCountCanvasModelImages(int _value)								{ setValue(countCanvasImages, _value); }
	public void setCountCanvasModelImages(int _value, int _expected)				{ setValue(countCanvasImages, _value, _expected); }

	public void setCountSketchModelStickys(int _value)								{ setValue(countSketchSticky, _value); }
	public void setCountSketchModelStickys(int _value, int _expected)				{ setValue(countSketchSticky, _value, _expected); }
	
	public void setCountSketchModelActors(int _value)								{ setValue(countSketchActors, _value); }
	public void setCountSketchModelActors(int _value, int _expected)				{ setValue(countSketchActors, _value, _expected); }

	public void setCountDiagramModelBendpoints(int _value)							{ setValue(countBendpoints, _value); }
	public void setCountDiagramModelBendpoints(int _value, int _expected)			{ setValue(countBendpoints, _value, _expected); }

	public void setCountFolders(int _value)											{ setValue(countFolders, _value); }
	public void setCountFolders(int _value, int _expected)							{ setValue(countFolders, _value, _expected); }

	public void setCountImages(int _value)											{ setValue(countImages, _value); }
	public void setCountImages(int _value, int _expected)							{ setValue(countImages, _value, _expected); }
}
