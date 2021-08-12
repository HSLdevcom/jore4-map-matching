CREATE TABLE routing.dr_linkki
(
    gid             INT NOT NULL PRIMARY KEY,
    link_id         VARCHAR(20) NOT NULL,
    kuntakoodi      INT NOT NULL,
    linkkityyp      INT NOT NULL,
    ajosuunta       INT NOT NULL,
    link_tila       INT,
    tienimi_su      VARCHAR(200),
    tienimi_ru      VARCHAR(200),
    tienimi_sa      VARCHAR(200),
    source          INT NOT NULL,
    target          INT NOT NULL,
    cost            DOUBLE PRECISION NOT NULL,
    reverse_cost    DOUBLE PRECISION NOT NULL
);

SELECT AddGeometryColumn('routing', 'dr_linkki', 'geom', 3067, 'LINESTRING', 2);
ALTER TABLE routing.dr_linkki ALTER COLUMN geom SET NOT NULL;

ALTER TABLE routing.dr_linkki ADD CONSTRAINT uk_dr_linkki_link_id UNIQUE (link_id);

CREATE INDEX dr_linkki_geom_idx ON routing.dr_linkki USING GIST(geom);
CREATE INDEX dr_linkki_source_idx ON routing.dr_linkki (source);
CREATE INDEX dr_linkki_target_idx ON routing.dr_linkki (target);
