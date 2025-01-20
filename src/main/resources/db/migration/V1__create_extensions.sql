-- Create/recreate required extensions into schema named `extensions`.

DROP EXTENSION IF EXISTS pgrouting;
DROP EXTENSION IF EXISTS postgis;

CREATE EXTENSION postgis WITH SCHEMA extensions;
CREATE EXTENSION pgrouting WITH SCHEMA extensions;
