--
-- Create table for public transport stops.
--

CREATE TABLE routing.public_transport_stop
(
    public_transport_stop_id                    bigint           NOT NULL,
    public_transport_stop_national_id           int,
    located_on_infrastructure_link_id           bigint           NOT NULL,
    infrastructure_source_id                    int              NOT NULL,
    is_on_direction_of_link_forward_traversal   boolean,
    distance_from_link_start_in_meters          double precision NOT NULL,
    municipality_code                           int,
    name                                        jsonb
);

SELECT AddGeometryColumn('routing', 'public_transport_stop', 'geom', 3067, 'POINT', 2);
ALTER TABLE routing.public_transport_stop ALTER COLUMN geom SET NOT NULL;

COMMENT ON TABLE routing.public_transport_stop IS
    'The public transport stops imported from Digiroad export';
COMMENT ON COLUMN routing.public_transport_stop.public_transport_stop_id IS
    'The local ID of the public transport stop';
COMMENT ON COLUMN routing.public_transport_stop.public_transport_stop_national_id IS
    'The national (persistent) ID for the public transport stop';
COMMENT ON COLUMN routing.public_transport_stop.located_on_infrastructure_link_id IS
    'The ID of the infrastructure link on which the stop is located';
COMMENT ON COLUMN routing.public_transport_stop.infrastructure_source_id IS
    'The ID of the external source system providing the stop data';
COMMENT ON COLUMN routing.public_transport_stop.is_on_direction_of_link_forward_traversal IS
    'Is the direction of traffic on this stop the same as the direction of the linestring describing the infrastructure link? If TRUE, the stop lies in the direction of the linestring. If FALSE, the stop lies in the reverse direction of the linestring. If NULL, the direction is undefined.';
COMMENT ON COLUMN routing.public_transport_stop.distance_from_link_start_in_meters IS
    'The measure or M value of the stop from the start of the linestring (linear geometry) describing the infrastructure link. The SI unit is the meter.';
COMMENT ON COLUMN routing.public_transport_stop.municipality_code IS
    'The official code of municipality in which the stop is located';
COMMENT ON COLUMN routing.public_transport_stop.name IS
    'JSON object containing name in different localisations';
COMMENT ON COLUMN routing.public_transport_stop.geom IS
    'The 2D point geometry describing the location of the public transport stop. The EPSG:3067 coordinate system applied is the same as is used in Digiroad.';

-- Add constraints.
ALTER TABLE routing.public_transport_stop
    ADD CONSTRAINT public_transport_stop_pkey PRIMARY KEY (public_transport_stop_id),
    ADD CONSTRAINT public_transport_stop_infrastructure_link_fkey FOREIGN KEY (located_on_infrastructure_link_id)
        REFERENCES routing.infrastructure_link (infrastructure_link_id),
    ADD CONSTRAINT public_transport_stop_infrastructure_source_fkey FOREIGN KEY (infrastructure_source_id)
        REFERENCES routing.infrastructure_source (infrastructure_source_id);

-- Create indices to improve performance of queries.
CREATE INDEX public_transport_stop_infrastructure_link_idx ON routing.public_transport_stop (located_on_infrastructure_link_id);
CREATE INDEX public_transport_stop_geom_idx ON routing.public_transport_stop USING GIST(geom);
