CREATE TABLE public.archimatediagrammodel
(
  id character varying(50) NOT NULL,
  name character varying(255),
  documentation character varying(65535),
  type character varying(255),
  CONSTRAINT pk_dia_id PRIMARY KEY (id)
)

CREATE TABLE public.archimateelement
(
  id character varying(50) NOT NULL,
  name character varying(255),
  type character varying(50),
  documentation character varying(65535),
  CONSTRAINT pk_elm_id PRIMARY KEY (id)
)

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
  CONSTRAINT pk_diagram_objects_id PRIMARY KEY (id)
)

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
)

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
  CONSTRAINT pk_connections_id PRIMARY KEY (id)
)

CREATE TABLE public.model
(
  id character(50) NOT NULL,
  name character varying(255),
  purpose character varying(65535),
  owner character varying(50),
  creation date,
  checkin date,
  user character varying(50),
  version character varying(255)[],
  CONSTRAINT pk_models_id PRIMARY KEY (id)
)

CREATE TABLE public.point
(
  id integer NOT NULL DEFAULT nextval('bounds_id_seq'::regclass),
  parent character varying(255),
  x integer,
  y integer,
  w integer,
  h integer,
  rank integer,
  CONSTRAINT pk_bounds_id PRIMARY KEY (id)
)

CREATE TABLE public.property
(
  name character varying(50) NOT NULL,
  value character varying(255),
  parent character varying(255),
  id integer NOT NULL DEFAULT nextval('properties_id_seq'::regclass),
  CONSTRAINT pk_properties_id PRIMARY KEY (id)
)

CREATE TABLE public.relationship
(
  id character varying(50) NOT NULL,
  name character varying(255),
  source character varying(50),
  target character varying(50),
  type character varying(50),
  documentation character varying(65535),
  CONSTRAINT pk_rel_id PRIMARY KEY (id)
)