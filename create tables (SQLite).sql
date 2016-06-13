CREATE TABLE archimatediagrammodel (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	connectionroutertype INTEGER,
	documentation VARCHAR (65535),
	folder VARCHAR (255),
	name VARCHAR (255) NOT NULL,
	type VARCHAR (50),
	viewpoint INTEGER,
	
	CONSTRAINT pk_archimatediagrammodel PRIMARY KEY (id, model, version)
);

CREATE TABLE archimateelement (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	documentation VARCHAR (65535),
	folder VARCHAR (255),
	name VARCHAR (255) NOT NULL,
	type VARCHAR (50),
	
	CONSTRAINT pk_archimateelement PRIMARY KEY (id, model, version)
);

CREATE TABLE bendpoint (
parent VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	startx INTEGER,
	starty INTEGER,
	endx INTEGER,
	endy INTEGER,
	rank INTEGER,
	
	CONSTRAINT pk_bendpoint PRIMARY KEY (parent, model, version, rank)
);

CREATE TABLE canvasmodel (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	documentation VARCHAR (65535),
	folder VARCHAR (255),
	name VARCHAR (255),
	hinttitle VARCHAR (255),
	hintcontent VARCHAR (4096),
	connectionroutertype INTEGER,
	
	CONSTRAINT pk_canvasmodel PRIMARY KEY (id, model, version)
);

CREATE TABLE canvasmodelblock (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	parent VARCHAR (255),
	bordercolor VARCHAR (255),
	content VARCHAR (4096),
	fillcolor VARCHAR (255),
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	hintcontent VARCHAR (4096),
	hinttitle VARCHAR (255),
	imagepath VARCHAR (4096),
	imageposition INTEGER,
	islocked boolean,
	linecolor VARCHAR (255),
	linewidth INTEGER,
	name VARCHAR (255),
	textalignment INTEGER,
	textposition INTEGER,
	indent INTEGER,
	rank INTEGER,
	x INTEGER,
	y INTEGER,
	width INTEGER,
	height INTEGER,
	
	CONSTRAINT pk_canvasmodelblock PRIMARY KEY (id, model, version)
);

CREATE TABLE canvasmodelimage (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	parent VARCHAR (50),
	bordercolor VARCHAR (255),
	islocked BOOLEAN,
	fillcolor VARCHAR (255),
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	imagepath VARCHAR (4096),
	linecolor VARCHAR (255),
	linewidth INTEGER,
	name VARCHAR (255),
	targetconnections VARCHAR (255),
	textalignment INTEGER,
	indent INTEGER,
	rank INTEGER,
	x INTEGER,
	y INTEGER,
	width INTEGER,
	height INTEGER,
	
	CONSTRAINT pk_canvasmodelimage PRIMARY KEY (id, model, version)
);

CREATE TABLE canvasmodelsticky (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	parent VARCHAR (50),
	bordercolor VARCHAR (255),
	content VARCHAR (4096),
	fillcolor VARCHAR (255),
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	imagepath VARCHAR (4096),
	imageposition INTEGER,
	islocked BOOLEAN,
	linecolor VARCHAR (255),
	linewidth INTEGER,
	notes VARCHAR (4096),
	name VARCHAR (255),
	targetconnections VARCHAR (255),
	textalignment INTEGER,
	textposition INTEGER,
	indent INTEGER,
	rank INTEGER,
	x INTEGER,
	y INTEGER,
	width INTEGER,
	height INTEGER,
	
	CONSTRAINT pk_canvasmodelsticky PRIMARY KEY (id, model, version)
);

CREATE TABLE connection (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	class VARCHAR (255),
	documentation VARCHAR (65535),
	islocked BOOLEAN,
	linewidth INTEGER,
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	linecolor VARCHAR (255),
	parent VARCHAR (255) NOT NULL,
	relationship VARCHAR (255),
	source VARCHAR (255),
	target VARCHAR (255),
	text VARCHAR (255),
	textposition VARCHAR (255),
	type INTEGER,
	indent INTEGER,
	rank INTEGER,
	
	CONSTRAINT pk_connection PRIMARY KEY (id, model, version)
);

CREATE TABLE diagrammodelarchimateobject (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	archimateelementid VARCHAR (255),
	archimateelementname VARCHAR (255),
	archimateelementclass VARCHAR (255),
	bordertype INTEGER,
	class VARCHAR (255),
	content VARCHAR (65535),
	documentation VARCHAR (65535),
	linecolor VARCHAR (255),
	linewidth INTEGER,
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	fillcolor VARCHAR (255),
	name VARCHAR (255),
	parent VARCHAR (255) NOT NULL,
	targetconnections VARCHAR (255),
	textalignment INTEGER,
	type INTEGER,
	indent INTEGER,
	rank INTEGER,
	x INTEGER,
	y INTEGER,
	width INTEGER,
	height INTEGER,
	
	CONSTRAINT pk_diagrammodelarchimateobject PRIMARY KEY (id, model, version)
);

CREATE TABLE diagrammodelreference (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	fillcolor VARCHAR (255),
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	linecolor VARCHAR (255),
	linewidth INTEGER,
	parent VARCHAR (255) NOT NULL,
	targetconnections VARCHAR (255),
	textalignment INTEGER,
	rank INTEGER,
	indent INTEGER,
	
	CONSTRAINT pk_diagrammodelreference PRIMARY KEY (id, model, version)
);

CREATE TABLE folder (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	documentation VARCHAR (65535),
	parent VARCHAR (24),
	type INTEGER,
	name VARCHAR (255),
	rank INTEGER,
	
	CONSTRAINT pk_folder PRIMARY KEY (id, model, version)
);

CREATE TABLE images (
	path VARCHAR (50),
	md5 VARCHAR (32),
	image BLOB,
	
	CONSTRAINT pk_images PRIMARY KEY (path)
);

CREATE TABLE model (
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	name VARCHAR (255) NOT NULL,
	note VARCHAR (255),
	owner VARCHAR (50),
	period VARCHAR (50),
	purpose VARCHAR (65535),
	countMetadatas INTEGER,
	countFolders INTEGER,
	countElements INTEGER,
	countRelationships INTEGER,
	countProperties INTEGER,
	countArchimateDiagramModels INTEGER,
	countDiagramModelArchimateObjects INTEGER,
	countDiagramModelArchimateConnections INTEGER,
	countDiagramModelGroups INTEGER,
	countDiagramModelNotes INTEGER,
	countCanvasModels INTEGER,
	countCanvasModelBlocks INTEGER,
	countCanvasModelStickys INTEGER,
	countCanvasModelConnections INTEGER,
	countCanvasModelImages INTEGER,
	countSketchModels INTEGER,
	countSketchModelActors INTEGER,
	countSketchModelStickys INTEGER,
	countDiagramModelConnections INTEGER,
	countDiagramModelBendpoints INTEGER,
	countDiagramModelReferences INTEGER,
	countImages INTEGER,
	
	CONSTRAINT pk_model PRIMARY KEY (model, version)
);

CREATE TABLE property (
	id VARCHAR (24) NOT NULL,
	parent VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	name VARCHAR (4096) NOT NULL,
	value VARCHAR (4096),
	
	CONSTRAINT pk_property PRIMARY KEY (id, model, version, parent)
);

CREATE TABLE relationship (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	documentation VARCHAR (65535),
	name VARCHAR (255) NOT NULL,
	source VARCHAR (50),
	target VARCHAR (50),
	type VARCHAR (50),
	folder VARCHAR (255),
	
	CONSTRAINT pk_relationship PRIMARY KEY (id, model, version)
);

CREATE TABLE sketchmodel (
	id VARCHAR (24),
	model VARCHAR (24),
	version VARCHAR (11),
	name VARCHAR (255),
	documentation VARCHAR (65535),
	connectionroutertype INTEGER,
	background INTEGER,
	folder VARCHAR (255),
	
	CONSTRAINT pk_sketchmodel PRIMARY KEY (id, model, version)
);

CREATE TABLE sketchmodelactor (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	parent VARCHAR (255),
	fillcolor VARCHAR (255),
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	linecolor VARCHAR (255),
	linewidth INTEGER,
	name VARCHAR (255),
	targetconnections VARCHAR (255),
	textalignment INTEGER,
	indent INTEGER,
	rank INTEGER,
	x INTEGER,
	y INTEGER,
	width INTEGER,
	height INTEGER,
	
	CONSTRAINT pk_sketchmodelactor PRIMARY KEY (id, model, version)
);

CREATE TABLE sketchmodelsticky (
	id VARCHAR (24) NOT NULL,
	model VARCHAR (24) NOT NULL,
	version VARCHAR (11) NOT NULL,
	parent VARCHAR (255),
	content VARCHAR (4096),
	fillcolor VARCHAR (255),
	font VARCHAR (255),
	fontcolor VARCHAR (255),
	linecolor VARCHAR (255),
	linewidth INTEGER,
	name VARCHAR (255),
	targetconnections VARCHAR (255),
	textalignment INTEGER,
	indent INTEGER,
	rank INTEGER,
	x INTEGER,
	y INTEGER,
	width INTEGER,
	height INTEGER,
	
	CONSTRAINT pk_sketchmodelsticky PRIMARY KEY (id, model, version)
);