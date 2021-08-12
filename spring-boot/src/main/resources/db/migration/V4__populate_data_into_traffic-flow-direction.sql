--
-- Populate data for directions of traffic flow.
--

INSERT INTO routing.traffic_flow_direction (traffic_flow_direction_type, traffic_flow_direction_name, description) VALUES
    (2, 'bidirectional', 'Bidirectional'),
    (3, 'backward', 'Against digitised direction'),
    (4, 'forward', 'Along digitised direction');
