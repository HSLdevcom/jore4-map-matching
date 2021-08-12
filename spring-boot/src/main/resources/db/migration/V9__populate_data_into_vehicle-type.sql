--
-- Populate data for vehicle types.
--

INSERT INTO routing.vehicle_type (vehicle_type, belonging_to_vehicle_mode) VALUES
    ('generic_bus', 'bus'),
    ('generic_tram', 'tram'),
    ('generic_train', 'train'),
    ('generic_metro', 'metro'),
    ('generic_ferry', 'ferry'),
    ('tall_electric_bus', 'bus');
