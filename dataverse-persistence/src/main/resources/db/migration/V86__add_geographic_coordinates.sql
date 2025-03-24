create table gebox_coordinates_tmp as (
select d.*, 'westLongitude' as fieldname  from datasetfield d join datasetfieldtype dt on d.datasetfieldtype_id = dt.id where dt.name = 'westLongitude'
union
select d.*, 'eastLongitude' as fieldname from datasetfield d join datasetfieldtype dt on d.datasetfieldtype_id = dt.id where dt.name = 'eastLongitude'
union
select d.*, 'northLongitude' as fieldname from datasetfield d join datasetfieldtype dt on d.datasetfieldtype_id = dt.id where dt.name = 'northLongitude'
union
select d.*, 'southLongitude' as fieldname from datasetfield d join datasetfieldtype dt on d.datasetfieldtype_id = dt.id where dt.name = 'southLongitude'
);


INSERT INTO public.datasetfieldtype
(advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, "name", required, title, uri, watermark, metadatablock_id, parentdatasetfieldtype_id, inputrenderertype, inputrendereroptions, validation, metadata, exporttofile, visiblethroughanonymizedurl)
VALUES(true, false, false, 'Enter geographic coordinates to mark dataset location', '', true, 15, false, 'TEXTBOX', 'geographicCoordinates', true, 'Geographic coordinates', NULL, '', 2, 85, 'TEXTBOX', '{}', '[{"name":"geobox_polygon_component_validator", "parameters":{"runOnEmpty":"true"}}]', '{}', false, true);


insert into datasetfield (datasetfieldtype_id, datasetversion_id, template_id, datasetfieldparent_id, displayorder, "source", fieldvalue)
with coordinates as (
SELECT
    (select id from datasetfieldtype dt where dt.name = 'geographicCoordinates') as id,
    max(datasetversion_id) as datasetversion_id,
    max(template_id) as template_id,
    max(datasetfieldparent_id) as datasetfieldparent_id,
    0,
    MAX(CASE WHEN fieldname = 'westLongitude' THEN fieldvalue END) AS west,
    MAX(CASE WHEN fieldname = 'eastLongitude' THEN fieldvalue END) AS east,
    MAX(CASE WHEN fieldname = 'northLongitude' THEN fieldvalue END) AS north,
    MAX(CASE WHEN fieldname = 'southLongitude' THEN fieldvalue END) AS south
FROM gebox_coordinates_tmp
group by datasetfieldparent_id)
select
    id,
    datasetversion_id,
    template_id,
    datasetfieldparent_id,
    0,
    'PRIMARY' as "source" ,
    CASE
        WHEN south < north THEN
            CONCAT(west, ' ', south, E'\n', west, ' ', north, E'\n', east, ' ', north, E'\n', east, ' ', south, E'\n', west, ' ', south)
        ELSE
            CONCAT(west, ' ', north, E'\n', west, ' ', south, E'\n', east, ' ', south, E'\n', east, ' ', north, E'\n', west, ' ', north)
    END AS fieldvalue
from coordinates;

-- remove values connected with geo fields
delete from  datasetfield d where datasetfieldtype_id in (
  select id from datasetfieldtype dt where dt.name = 'westLongitude'
    or dt.name = 'eastLongitude'
    or dt.name = 'northLongitude'
    or dt.name = 'southLongitude'
);
-- remove datasetfieldstypes
delete from  public.datasetfieldtype where id in (
  select id from datasetfieldtype dt where dt.name = 'westLongitude'
    or dt.name = 'eastLongitude'
    or dt.name = 'northLongitude'
    or dt.name = 'southLongitude'
);