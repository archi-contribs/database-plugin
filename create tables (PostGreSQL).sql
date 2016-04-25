CREATE TABLE public.archimatediagrammodel
(
  id character varying(50) NOT NULL,
  name character varying(255),
  documentation character varying(65535),
  type character varying(255),
  CONSTRAINT pk_archimatediagrammodel_id PRIMARY KEY (id)
);
ALTER TABLE archimatediagrammodel OWNER TO archi;

CREATE TABLE public.archimateelement
(
  id character varying(50) NOT NULL,
  name character varying(255),
  type character varying(50),
  documentation character varying(65535),
  CONSTRAINT pk_archimateelement_id PRIMARY KEY (id)
);
ALTER TABLE archimateelement OWNER TO archi;

CREATE TABLE public.diagrammodelarchimateobject
(
  id character varying(255) NOT NULL,
  font character varying(255),
  fontcolor character varying(255),
  fillcolor character varying(255),
  element character varying(255),
  targetconnections character varying(255),
  parent character varying(255),
  linecolor character varying(255),
  name character varying(255),
  linewidth integer,
  textalignment integer,
  rank integer,
  indent integer,
  type integer,
  CONSTRAINT pk_diagrammodelarchimateobject_id PRIMARY KEY (id)
);
ALTER TABLE diagrammodelarchimateobject OWNER TO archi;

CREATE TABLE public.diagrammodelgroup
(
  id character varying(255) NOT NULL,
  defaulttextalignment integer,
  documentation character varying(65535),
  fillcolor character varying(255),
  font character varying(255),
  fontcolor character varying(255),
  linecolor character varying(255),
  linewidth integer,
  name character varying(255),
  textalignment integer,
  rank integer,
  targetconnections character varying(255),
  parent character varying(255),
  CONSTRAINT pk_diagrammodelgroup_id PRIMARY KEY (id)
);
ALTER TABLE diagrammodelgroup OWNER TO archi;

CREATE TABLE public.diagrammodelarchimateconnection
(
  id character varying(255) NOT NULL,
  font character varying(255),
  fontcolor character varying(255),
  linecolor character varying(255),
  textposition character varying(255),
  source character varying(255),
  target character varying(255),
  relationship character varying(255),
  parent character varying(255),
  documentation character varying(65535),
  text character varying(255),
  name character varying(255),
  linewidth integer,
  type integer,
  rank integer,
  indent integer,
  CONSTRAINT pk_diagrammodelarchimateconnection_id PRIMARY KEY (id)
);
ALTER TABLE diagrammodelarchimateconnection OWNER TO archi;

CREATE TABLE public.model
(
  id character(50) NOT NULL,
  name character varying(255),
  purpose character varying(65535),
  owner character varying(50),
  creation date,
  checkin date,
  "user" character varying(50),
  version character varying(255)[],
  CONSTRAINT pk_model_id PRIMARY KEY (id)
);
ALTER TABLE model OWNER TO archi;

CREATE TABLE public.point
(
  id integer NOT NULL DEFAULT nextval('bounds_id_seq'::regclass),
  parent character varying(255),
  x integer,
  y integer,
  w integer,
  h integer,
  rank integer,
  CONSTRAINT pk_point_id PRIMARY KEY (id)
);
ALTER TABLE point OWNER TO archi;

CREATE TABLE public.property
(
  name character varying(50) NOT NULL,
  value character varying(255),
  parent character varying(255),
  id integer NOT NULL DEFAULT nextval('properties_id_seq'::regclass),
  CONSTRAINT pk_property_id PRIMARY KEY (id)
);
ALTER TABLE property OWNER TO archi;

CREATE TABLE public.relationship
(
  id character varying(50) NOT NULL,
  name character varying(255),
  source character varying(50),
  target character varying(50),
  type character varying(50),
  documentation character varying(65535),
  CONSTRAINT pk_relationship_id PRIMARY KEY (id)
);
ALTER TABLE relationship OWNER TO archi;