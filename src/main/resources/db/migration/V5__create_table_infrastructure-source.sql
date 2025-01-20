--
-- Create table for infrastructure element sources.
--

CREATE TABLE routing.infrastructure_source (
    infrastructure_source_id int PRIMARY KEY,
    infrastructure_source_name text NOT NULL,
    description text NOT NULL
);

COMMENT ON TABLE routing.infrastructure_source IS
    'The enumerated sources for infrastructure network entities';
COMMENT ON COLUMN routing.infrastructure_source.infrastructure_source_id IS
    'The numeric enum value for the infrastructure element source. This enum code is only local to this routing schema. ATM, it is not intended to be distributed to or shared across other JORE4 services.';
COMMENT ON COLUMN routing.infrastructure_source.infrastructure_source_name IS
    'The short name for the infrastructure element source';
