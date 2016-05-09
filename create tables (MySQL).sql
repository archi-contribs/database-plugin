CREATE TABLE archi.archimatediagrammodel
(
  id varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,

  connectionroutertype int,  
  documentation varchar(7500),
  name varchar(255) NOT NULL,
  type varchar(50),
  viewpoint int,
  
  CONSTRAINT pk_archimatediagrammodel PRIMARY KEY (id, model, version)
);

CREATE TABLE archi.archimateelement
(
  id varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,
  
  documentation varchar(7500),
  name varchar(255) NOT NULL,
  type varchar(50),
  
  CONSTRAINT pk_archimateelement PRIMARY KEY (id, model, version)
);

CREATE TABLE archi.diagrammodelarchimateconnection
(
  id varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,

  documentation varchar(7500),
  linewidth int,
  font varchar(255),
  fontcolor varchar(255),
  linecolor varchar(255),
  parent varchar(255) NOT NULL,
  relationship varchar(255),
  source varchar(255),
  target varchar(255),
  text varchar(255),
  textposition varchar(255),
  type int,
  
  indent int,
  rank int,
  class varchar(255),

  CONSTRAINT pk_diagrammodelarchimateconnection PRIMARY KEY (id, model, version)
);

CREATE TABLE archi.diagrammodelarchimateobject
(
  id varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,
  
  archimateelement varchar(255),
  bordertype int,
  content varchar(7500),
  documentation varchar(7500),
  linecolor varchar(255),
  linewidth int,
  font varchar(255),
  fontcolor varchar(255),
  fillcolor varchar(255),
  name varchar(255),
  parent varchar(255) NOT NULL,
  targetconnections varchar(255),
  textalignment int,
  type int,
	
  rank int,
  indent int,
  class varchar(255),

  CONSTRAINT pk_diagrammodelarchimateobject PRIMARY KEY (id, model, version)
);

CREATE TABLE archi.model
(
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,
  
  name varchar(255) NOT NULL,
  owner varchar(50),
  period varchar(50),
  purpose varchar(7500),
  note varchar(255),
  
  CONSTRAINT pk_model PRIMARY KEY (model, version)
);

CREATE TABLE archi.point
(
  parent varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,

  x int,
  y int,
  w int,
  h int,
  
  rank int NOT NULL,
  
  CONSTRAINT pk_point PRIMARY KEY (parent, model, version, rank)
);

CREATE TABLE archi.property
(
  parent varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,
  
  name varchar(50) NOT NULL,
  value varchar(255),
  
  CONSTRAINT pk_property PRIMARY KEY (parent, model, version, name)
);

CREATE TABLE archi.relationship
(
  id varchar(50) NOT NULL,
  model varchar(50) NOT NULL,
  version varchar(50) NOT NULL,
  
  documentation varchar(7500),
  name varchar(255) NOT NULL,
  source varchar(50),
  target varchar(50),
  type varchar(50),
  
  CONSTRAINT pk_relationship PRIMARY KEY (id, model, version)
);

 