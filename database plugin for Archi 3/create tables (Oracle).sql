CREATE TABLE archidatabaseplugin (
   version VARCHAR(7) NOT NULL,
   
   CONSTRAINT pk_archidatabaseplugin PRIMARY KEY (version)
);

INSERT INTO archidatabaseplugin values('3');

CREATE TABLE archimatediagrammodel (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   connectionroutertype INTEGER,
   documentation CLOB,
   folder VARCHAR(50) NOT NULL,
   name CLOB NOT NULL,
   type VARCHAR(50) NOT NULL,
   viewpoint INTEGER,
	
	CONSTRAINT pk_archimatediagrammodel PRIMARY KEY (id, model, version)
);

CREATE TABLE archimateelement (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   documentation CLOB,
   folder VARCHAR(50),
   name CLOB,
   type VARCHAR(30),
   interfacetype INTEGER,
	
	CONSTRAINT pk_archimateelement PRIMARY KEY (id, model, version)
);

CREATE TABLE bendpoint (
   parent VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   startx INTEGER,
   starty INTEGER,
   endx INTEGER,
   endy INTEGER,
   rank INTEGER,
	
	CONSTRAINT pk_bendpoint PRIMARY KEY (parent, model, version, rank)
);

CREATE TABLE canvasmodel (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   documentation CLOB,
   folder VARCHAR(50),
   name CLOB,
   hinttitle CLOB,
   hintcontent CLOB,
   connectionroutertype INTEGER,
	
	CONSTRAINT pk_canvasmodel PRIMARY KEY (id, model, version)
);

CREATE TABLE canvasmodelblock (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   parent VARCHAR(100),
   bordercolor VARCHAR(7),
   content CLOB,
   fillcolor VARCHAR(7),
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   hintcontent CLOB,
   hinttitle CLOB,
   imagepath VARCHAR(50),
   imageposition INTEGER,
   islocked CHAR,
   linecolor VARCHAR(7),
   linewidth INTEGER,
   name CLOB,
   textalignment INTEGER,
   textposition INTEGER,
   indent INTEGER,
   targetconnections CLOB,
   rank INTEGER,
   x INTEGER,
   y INTEGER,
   width INTEGER,
   height INTEGER,
	
	CONSTRAINT pk_canvasmodelblock PRIMARY KEY (id, model, version)
);

CREATE TABLE canvasmodelimage (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   parent VARCHAR(100),
   bordercolor VARCHAR(7),
   islocked CHAR,
   fillcolor VARCHAR(7),
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   imagepath VARCHAR(50),
   linecolor VARCHAR(7),
   linewidth INTEGER,
   name CLOB,
   textalignment INTEGER,
   indent INTEGER,
   targetconnections CLOB,
   rank INTEGER,
   x INTEGER,
   y INTEGER,
   width INTEGER,
   height INTEGER,
	
	CONSTRAINT pk_canvasmodelimage PRIMARY KEY (id, model, version)
);

CREATE TABLE canvasmodelsticky (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   parent VARCHAR(100),
   bordercolor VARCHAR(7),
   content CLOB,
   fillcolor VARCHAR(7),
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   imagepath VARCHAR(50),
   imageposition INTEGER,
   islocked CHAR,
   linecolor VARCHAR(7),
   linewidth INTEGER,
   notes CLOB,
   name CLOB,
   textalignment INTEGER,
   textposition INTEGER,
   indent INTEGER,
   targetconnections CLOB,
   rank INTEGER,
   x INTEGER,
   y INTEGER,
   width INTEGER,
   height INTEGER,
	
	CONSTRAINT pk_canvasmodelsticky PRIMARY KEY (id, model, version)
);

CREATE TABLE connection (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   class VARCHAR(50),
   documentation CLOB,
   islocked CHAR,
   linewidth INTEGER,
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   linecolor VARCHAR(7),
   parent VARCHAR(100) NOT NULL,
   relationship VARCHAR(100),
   source VARCHAR(100),
   target VARCHAR(100),
   text CLOB,
   textposition INTEGER,
   type INTEGER,
   indent INTEGER,
   rank INTEGER,
	
	CONSTRAINT pk_connection PRIMARY KEY (id, model, version)
);

CREATE TABLE diagrammodelarchimateobject (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   archimateelementid VARCHAR(100),
   archimateelementname CLOB,
   archimateelementclass VARCHAR(50),
   bordertype INTEGER,
   class VARCHAR(50),
   content CLOB,
   documentation CLOB,
   linecolor VARCHAR(7),
   linewidth INTEGER,
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   fillcolor VARCHAR(7),
   name CLOB,
   parent VARCHAR(100) NOT NULL,
   textalignment INTEGER,
   targetconnections CLOB,
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
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   fillcolor VARCHAR(7),
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   linecolor VARCHAR(7),
   linewidth INTEGER,
   parent VARCHAR(100) NOT NULL,
   textalignment INTEGER,
   targetconnections CLOB,
   x integer,
   y integer,
   width integer,
   height integer,
   rank INTEGER,
   indent INTEGER,
	
	CONSTRAINT pk_diagrammodelreference PRIMARY KEY (id, model, version)
);

CREATE TABLE folder (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   documentation CLOB,
   parent VARCHAR(50),
   type INTEGER,
   name CLOB,
   rank INTEGER,
	
	CONSTRAINT pk_folder PRIMARY KEY (id, model, version)
);

CREATE TABLE images (
   path VARCHAR(50),
   md5 VARCHAR(32),
   image BLOB,
	
	CONSTRAINT pk_images PRIMARY KEY (path)
);

CREATE TABLE model (
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   name VARCHAR(255) NOT NULL,
   note CLOB,
   owner VARCHAR(30),
   period VARCHAR(30),
   purpose CLOB,
   countmetadatas INTEGER,
   countfolders INTEGER,
   countelements INTEGER,
   countrelationships INTEGER,
   countproperties INTEGER,
   countarchimatediagrammodels INTEGER,
   countdiagrammodelarchimateobje INTEGER,
   countdiagrammodelarchimateconn INTEGER,
   countdiagrammodelgroups INTEGER,
   countdiagrammodelnotes INTEGER,
   countcanvasmodels INTEGER,
   countcanvasmodelblocks INTEGER,
   countcanvasmodelstickys INTEGER,
   countcanvasmodelconnections INTEGER,
   countcanvasmodelimages INTEGER,
   countsketchmodels INTEGER,
   countsketchmodelactors INTEGER,
   countsketchmodelstickys INTEGER,
   countdiagrammodelconnections INTEGER,
   countdiagrammodelbendpoints INTEGER,
   countdiagrammodelreferences INTEGER,
   countimages INTEGER,
	
	CONSTRAINT pk_model PRIMARY KEY (model, version)
);

CREATE TABLE property (
   id VARCHAR(50) NOT NULL,
   parent VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   name CLOB NOT NULL,
   value CLOB,
	
	CONSTRAINT pk_property PRIMARY KEY (id, model, version, parent)
);

CREATE TABLE relationship (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   documentation CLOB,
   name CLOB NOT NULL,
   source VARCHAR(100),
   target VARCHAR(100),
   type VARCHAR(30),
   folder VARCHAR(50),
   accesstype INTEGER,
	
	CONSTRAINT pk_relationship PRIMARY KEY (id, model, version)
);

CREATE TABLE sketchmodel (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   name CLOB,
   documentation CLOB,
   connectionroutertype INTEGER,
   background INTEGER,
   folder VARCHAR(50),
	
	CONSTRAINT pk_sketchmodel PRIMARY KEY (id, model, version)
);

CREATE TABLE sketchmodelactor (
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   parent VARCHAR(100),
   fillcolor VARCHAR(7),
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   linecolor VARCHAR(7),
   linewidth INTEGER,
   name CLOB,
   targetconnections CLOB,
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
   id VARCHAR(50) NOT NULL,
   model VARCHAR(32) NOT NULL,
   version VARCHAR(11) NOT NULL,
   parent VARCHAR(100),
   content CLOB,
   fillcolor VARCHAR(7),
   font VARCHAR(150),
   fontcolor VARCHAR(7),
   linecolor VARCHAR(7),
   linewidth INTEGER,
   name CLOB,
   targetconnections CLOB,
   textalignment INTEGER,
   indent INTEGER,
   rank INTEGER,
   x INTEGER,
   y INTEGER,
   width INTEGER,
   height INTEGER,
	
	CONSTRAINT pk_sketchmodelsticky PRIMARY KEY (id, model, version)
);