--
-- Create table for vehicle modes.
--

CREATE TABLE routing.vehicle_mode (
    vehicle_mode text PRIMARY KEY
);

COMMENT ON TABLE routing.vehicle_mode IS
    'The vehicle modes from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:1:283';
COMMENT ON COLUMN routing.vehicle_mode.vehicle_mode IS
    'The vehicle mode from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:1:283';
