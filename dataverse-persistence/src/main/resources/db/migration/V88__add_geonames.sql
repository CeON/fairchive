CREATE TABLE IF NOT EXISTS geoname (
    id int4 NOT NULL,
    "name" varchar NOT NULL,
    alternatenames varchar NOT NULL,
    featurecode varchar NOT NULL,
    countrycode bpchar(2) NOT NULL,
    admin1code varchar NULL,
    admin2code varchar NULL,
    admin3code varchar NULL,
    admin4code varchar NULL,
    "hierarchy" varchar NULL,
    fullText tsvector NULL,
    CONSTRAINT geonames_pk PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_fullText_geoname ON geoname USING gin (fullText);

