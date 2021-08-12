--
-- Create link table between infrastructure links and vehicle types.
--

CREATE TABLE routing.infrastructure_link_safely_traversed_by_vehicle_type (
    infrastructure_link_id bigint REFERENCES routing.infrastructure_link (infrastructure_link_id),
    vehicle_type text REFERENCES routing.vehicle_type (vehicle_type),
    PRIMARY KEY (infrastructure_link_id, vehicle_type)
);

COMMENT ON TABLE routing.infrastructure_link_safely_traversed_by_vehicle_type IS
    'Which infrastructure links are safely traversed by which vehicle types?';
COMMENT ON COLUMN routing.infrastructure_link_safely_traversed_by_vehicle_type.infrastructure_link_id IS
    'The infrastructure link that can be safely traversed by the vehicle type';
COMMENT ON COLUMN routing.infrastructure_link_safely_traversed_by_vehicle_type.vehicle_type IS
    'The vehicle type that can safely traverse the infrastructure link';

CREATE INDEX ON routing.infrastructure_link_safely_traversed_by_vehicle_type (vehicle_type, infrastructure_link_id);
