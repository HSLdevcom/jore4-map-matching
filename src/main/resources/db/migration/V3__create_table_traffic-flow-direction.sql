--
-- Create table for directions of traffic flow.
--

CREATE TABLE routing.traffic_flow_direction (
    traffic_flow_direction_type int PRIMARY KEY,
    traffic_flow_direction_name text NOT NULL,
    description text NOT NULL
);

COMMENT ON TABLE routing.traffic_flow_direction IS
    'The possible directions of traffic flow on infrastructure links. Using code values from Digiroad codeset.';
COMMENT ON COLUMN routing.traffic_flow_direction.traffic_flow_direction_type IS
    'Numeric enum value for direction of traffic flow. The code value originates from Digiroad codeset.';
COMMENT ON COLUMN routing.traffic_flow_direction.traffic_flow_direction_name IS
    'The short name for direction of traffic flow. The text value originates from the JORE4 database schema.';
