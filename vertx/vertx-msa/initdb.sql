DROP DATABASE IF EXISTS sensorsdb;
DROP ROLE IF EXISTS sensorsus;

CREATE DATABASE sensorsdb WITH TEMPLATE template0 ENCODING UTF8 LC_CTYPE en_US;
\connect sensorsdb;

CREATE USER sensorsus WITH ENCRYPTED PASSWORD 'sensorsus';

CREATE SCHEMA sensorssc AUTHORIZATION sensorsus;
CREATE TABLE sensorssc.temperature_records
(
    uuid   VARCHAR,
    tstamp TIMESTAMPTZ,
    value  DOUBLE PRECISION,
    PRIMARY KEY (uuid, tstamp)
);

GRANT ALL PRIVILEGES ON DATABASE sensorsdb to sensorsus;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA sensorssc TO sensorsus;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA sensorssc TO sensorsus;
