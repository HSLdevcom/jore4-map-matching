CREATE SEQUENCE routing.infrastructure_link_vertices_pgr_id_seq;

CREATE TABLE routing.infrastructure_link_vertices_pgr
(
    id      bigint NOT NULL PRIMARY KEY DEFAULT nextval('routing.infrastructure_link_vertices_pgr_id_seq'),
    cnt     int,
    chk     int,
    ein     int,
    eout    int
);

COMMENT ON TABLE routing.infrastructure_link_vertices_pgr IS
    'Topology nodes created for infrastructure links by pgRougting';

ALTER SEQUENCE routing.infrastructure_link_vertices_pgr_id_seq OWNED BY routing.infrastructure_link_vertices_pgr.id;

SELECT AddGeometryColumn('routing', 'infrastructure_link_vertices_pgr', 'the_geom', 3067, 'POINT', 2);

CREATE INDEX infrastructure_link_vertices_pgr_the_geom_idx ON routing.infrastructure_link_vertices_pgr USING GIST(the_geom);
