CREATE TABLE public.archimatediagrammodel
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  connectionroutertype integer,
  documentation character varying(65535),
  folder character varying(255),
  name character varying(255) NOT NULL,
  type character varying(50),
  viewpoint integer,
  
  CONSTRAINT pk_archimatediagrammodel PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.archimateelement
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  documentation character varying(65535),
  folder character varying(255),
  name character varying(255) NOT NULL,
  type character varying(50),
  
  CONSTRAINT pk_archimateelement PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.canvasmodel
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  documentation character varying(65535),
  folder character varying(255),
  name character varying(255),
  hinttitle character varying(255),
  hintcontent character varying(4096),
  connectionroutertype integer,
  
  CONSTRAINT pk_canvasmodel PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.canvasmodelblock
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  parent character varying(255),
  bordercolor character varying(255),
  content character varying(4096),
  fillcolor character varying(255),
  font character varying(255),
  fontcolor character varying(255),
  hintcontent character varying(4096),
  hinttitle character varying(255),
  imagepath character varying(4096),
  imageposition integer,
  islocked boolean,
  linecolor character varying(255),
  linewidth integer,
  name character varying(255),
  textalignment integer,
  textposition integer,
  x integer,
  y integer,
  width integer,
  height integer,
  
  indent integer,
  rank integer,
  
  CONSTRAINT pk_canvasmodelblock PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.canvasmodelsticky
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  parent character varying(50),
  bordercolor character varying(255),
  content character varying(4096),
  fillcolor character varying(255),
  font character varying(255),
  fontcolor character varying(255),
  imagepath character varying(4096),
  imageposition integer,
  linecolor character varying(255),
  linewidth integer,
  notes character varying(4096),
  name character varying(255),
  source character varying(255),
  target character varying(255),
  textalignment integer,
  textposition integer,
  x integer,
  y integer,
  width integer,
  height integer,
  
  indent integer,
  rank integer,
  
  CONSTRAINT pk_canvasmodelsticky PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.diagrammodelarchimateconnection
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  class character varying(255),
  documentation character varying(65535),
  linewidth integer,
  font character varying(255),
  fontcolor character varying(255),
  linecolor character varying(255),
  parent character varying(255) NOT NULL,
  relationship character varying(255),
  source character varying(255),
  target character varying(255),
  text character varying(255),
  textposition character varying(255),
  type integer,
  
  indent integer,
  rank integer,
  
  CONSTRAINT pk_diagrammodelarchimateconnection PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.diagrammodelarchimateobject
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  archimateelementid character varying(255),
  archimateelementname character varying(255),
  archimateelementclass character varying(255),
  bordertype integer,
  class character varying(255),
  content character varying(65535),
  documentation character varying(65535),
  linecolor character varying(255),
  linewidth integer,
  font character varying(255),
  fontcolor character varying(255),
  fillcolor character varying(255),
  name character varying(255),
  parent character varying(255) NOT NULL,
  targetconnections character varying(255),
  textalignment integer,
  type integer,
  x integer,
  y integer,
  width integer,
  height integer,
  
  indent integer,
  rank integer,

  CONSTRAINT pk_diagrammodelarchimateobject PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.folder
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  documentation character varying(65535),
  parent character varying(50),
  type integer,
  name character varying(255),
  
  rank integer,
  
  CONSTRAINT pk_folder PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.model
(
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  name character varying(255) NOT NULL,
  
  note character varying(255),
  owner character varying(50),
  period character varying(50),
  purpose character varying(65535),
  
  CONSTRAINT pk_model PRIMARY KEY (model, version)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.bendpoint
(
  parent character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  startx integer,
  starty integer,
  endx integer,
  endy integer,
  
  rank integer,
  
  CONSTRAINT pk_point PRIMARY KEY (parent, model, version, rank)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.property
(
  id character varying(50) NOT NULL,
  parent character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  name character varying(4096) NOT NULL,
  value character varying(4096),

  CONSTRAINT pk_property PRIMARY KEY (id, model, version, parent)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE public.relationship
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  documentation character varying(65535),
  name character varying(255) NOT NULL,
  source character varying(50),
  target character varying(50),
  type character varying(50),
  folder character varying(255),
  
  CONSTRAINT pk_relationship PRIMARY KEY (id, model, version)
)
WITH (
  OIDS=FALSE
);

 