CREATE sequence if NOT EXISTS swf_sq increment BY 1 start 1;

CREATE TABLE if NOT EXISTS swf_workflow(
  id INTEGER PRIMARY KEY DEFAULT nextval('swf_sq'),
  email VARCHAR(200),
  phone VARCHAR(50),
  workflow_id VARCHAR(60)
);

