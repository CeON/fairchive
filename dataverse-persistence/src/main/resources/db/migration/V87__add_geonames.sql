CREATE TABLE public.geonames (
    id int4 NOT NULL,
    "name" varchar NOT NULL,
    alternate_names varchar NOT NULL,
    feature_code varchar NOT NULL,
    country_code bpchar(2) NOT NULL,
    admin1_code varchar NULL,
    admin2_code varchar NULL,
    admin3_code varchar NULL,
    admin4_code varchar NULL,
    CONSTRAINT geonames_pk PRIMARY KEY (id)
);