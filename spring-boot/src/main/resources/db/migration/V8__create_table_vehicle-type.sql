--
-- Create table for vehicle types.
--

CREATE TABLE routing.vehicle_type (
    vehicle_type text PRIMARY KEY,
    belonging_to_vehicle_mode text NOT NULL REFERENCES routing.vehicle_mode (vehicle_mode)
);

COMMENT ON TABLE routing.vehicle_type IS
    'The vehicle types from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:9:360';
COMMENT ON COLUMN routing.vehicle_type.vehicle_type IS
    'The vehicle type from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:9:360';
COMMENT ON COLUMN routing.vehicle_type.belonging_to_vehicle_mode IS
    'The vehicle mode the vehicle type belongs to: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:1:283';

CREATE INDEX ON routing.vehicle_type (belonging_to_vehicle_mode);
