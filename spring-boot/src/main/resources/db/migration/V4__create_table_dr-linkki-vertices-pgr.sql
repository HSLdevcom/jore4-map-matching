CREATE SEQUENCE routing.dr_linkki_vertices_pgr_id_seq;

CREATE TABLE routing.dr_linkki_vertices_pgr
(
    id      BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('routing.dr_linkki_vertices_pgr_id_seq'),
    cnt     INT,
    chk     INT,
    ein     INT,
    eout    INT
);

ALTER SEQUENCE routing.dr_linkki_vertices_pgr_id_seq OWNED BY routing.dr_linkki_vertices_pgr.id;

SELECT AddGeometryColumn('routing', 'dr_linkki_vertices_pgr', 'the_geom', 3067, 'POINT', 2);

CREATE INDEX dr_linkki_vertices_pgr_the_geom_idx ON routing.dr_linkki_vertices_pgr USING GIST(the_geom);
