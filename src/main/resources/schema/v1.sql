-- Table: region
CREATE TABLE region (
    id        INTEGER     PRIMARY KEY NOT NULL,
    name      TEXT        NOT NULL,
    world     TEXT        NOT NULL,
    enabled   INTEGER(1)  NOT NULL DEFAULT 1,
    hierarchy INTEGER     NOT NULL DEFAULT 0,
    min_x     REAL        NOT NULL,
    min_y     REAL        NOT NULL,
    min_z     REAL        NOT NULL,
    max_x     REAL        NOT NULL,
    max_y     REAL        NOT NULL,
    max_z     REAL        NOT NULL,
    destroyed INTEGER(1)  NOT NULL DEFAULT 0,
    CONSTRAINT min_max_check CHECK (
        min_x <= max_x AND
        min_y <= max_y AND
        min_z <= max_z
    )
);

-- Table: regionData
CREATE TABLE regionData (
    region_id INTEGER NOT NULL REFERENCES region(id),
    key       TEXT    NOT NULL,
    value     TEXT,
    PRIMARY KEY (region_id, key)
);

-- Table: regionPermission
CREATE TABLE regionPermission (
    region_id   INTEGER NOT NULL REFERENCES region(id),
    player_uuid TEXT    NOT NULL,
    level       INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (region_id, player_uuid)
);

-- Table: regionRule
CREATE TABLE regionRule (
    region_id INTEGER NOT NULL REFERENCES region(id),
    key       TEXT    NOT NULL,
    value     TEXT,
    PRIMARY KEY (region_id, key)
);
