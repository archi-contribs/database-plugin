CREATE TABLE public.archimatediagrammodel
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,

  connectionroutertype integer,  
  documentation character varying(65535),
  name character varying(255) NOT NULL,
  type character varying(50),
  viewpoint integer,
  
  CONSTRAINT pk_archimatediagrammodel PRIMARY KEY (id, model, version)
);

CREATE TABLE public.archimateelement
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  documentation character varying(65535),
  name character varying(255) NOT NULL,
  type character varying(50),
  
  CONSTRAINT pk_archimateelement PRIMARY KEY (id, model, version)
);

CREATE TABLE public.diagrammodelarchimateconnection
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,

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
  class character varying(255),

  CONSTRAINT pk_diagrammodelarchimateconnection PRIMARY KEY (id, model, version)
);

CREATE TABLE public.diagrammodelarchimateobject
(
  id character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  archimateelement character varying(255),
  bordertype integer,
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
	
  rank integer,
  indent integer,
  class character varying(255),

  CONSTRAINT pk_diagrammodelarchimateobject PRIMARY KEY (id, model, version)
);

CREATE TABLE public.model
(
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  name character varying(255) NOT NULL,
  owner character varying(50),
  period character varying(50),
  purpose character varying(65535),
  note character varying(255),
  
  CONSTRAINT pk_model PRIMARY KEY (model, version)
);

CREATE TABLE public.point
(
  parent character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,

  x integer,
  y integer,
  w integer,
  h integer,
  
  rank integer NOT NULL,
  
  CONSTRAINT pk_point PRIMARY KEY (parent, model, version, rank)
);

CREATE TABLE public.property
(
  parent character varying(50) NOT NULL,
  model character varying(50) NOT NULL,
  version character varying(50) NOT NULL,
  
  name character varying(50) NOT NULL,
  value character varying(255),
  
  CONSTRAINT pk_property PRIMARY KEY (parent, model, version, name)
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
  
  CONSTRAINT pk_relationship PRIMARY KEY (id, model, version)
);



ALTER TABLE public.archimatediagrammodel			OWNER TO archi;
ALTER TABLE public.archimateelement					OWNER TO archi;
ALTER TABLE public.diagrammodelarchimateconnection	OWNER TO archi;
ALTER TABLE public.diagrammodelarchimateobject		OWNER TO archi;
ALTER TABLE public.model							OWNER TO archi;
ALTER TABLE public.point							OWNER TO archi;
ALTER TABLE public.property							OWNER TO archi;
ALTER TABLE public.relationship						OWNER TO archi;
 