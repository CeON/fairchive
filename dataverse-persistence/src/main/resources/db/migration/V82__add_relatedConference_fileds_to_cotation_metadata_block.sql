-- insert new fields
INSERT INTO public.datasetfieldtype
(advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, "name", required, title, uri, watermark, metadatablock_id, parentdatasetfieldtype_id, inputrenderertype, inputrendereroptions, validation, metadata, exporttofile, visiblethroughanonymizedurl)
VALUES(true, false, true, 'A conference that is related to this dataset.', '', false, 96, false, 'NONE', 'relatedConference', false, 'Related Conference', NULL, '', (SELECT id FROM public.metadatablock WHERE name = 'citation'), NULL, 'TEXT', '{}', '[{"name":"standard_input"}]', '{}', false, false);
INSERT INTO public.datasetfieldtype
(advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, "name", required, title, uri, watermark, metadatablock_id, parentdatasetfieldtype_id, inputrenderertype, inputrendereroptions, validation, metadata, exporttofile, visiblethroughanonymizedurl)
VALUES(true, false, false, 'Name of the related conference.', '#VALUE', false, 97, false, 'TEXT', 'relatedConferenceName', false, 'Name', NULL, '', (SELECT id FROM public.metadatablock WHERE name = 'citation'), (select id from datasetfieldtype where name = 'relatedConference'), 'TEXT', '{}', '[{"name":"standard_input"}]', '{}', false, false);
INSERT INTO public.datasetfieldtype
(advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, "name", required, title, uri, watermark, metadatablock_id, parentdatasetfieldtype_id, inputrenderertype, inputrendereroptions, validation, metadata, exporttofile, visiblethroughanonymizedurl)
VALUES(true, false, false, 'Date of the start of the conference.', '-#VALUE', false, 98, false, 'DATE', 'relatedConferenceStartDate', false, 'Start date', NULL, 'YYYY-MM-DD', (SELECT id FROM public.metadatablock WHERE name = 'citation'), (select id from datasetfieldtype where name = 'relatedConference'), 'TEXT', '{}', '[{"name":"standard_input"}]', '{}', false, false);
INSERT INTO public.datasetfieldtype
(advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, "name", required, title, uri, watermark, metadatablock_id, parentdatasetfieldtype_id, inputrenderertype, inputrendereroptions, validation, metadata, exporttofile, visiblethroughanonymizedurl)
VALUES(true, false, false, 'Date of the end of the conference.', '-#VALUE', false, 99, false, 'DATE', 'relatedConferenceEndDate', false, 'End date', NULL, 'YYYY-MM-DD', (SELECT id FROM public.metadatablock WHERE name = 'citation'), (select id from datasetfieldtype where name = 'relatedConference'), 'TEXT', '{}', '[{"name":"standard_input"}]', '{}', false, false);

-- reorder exisitng fields
update datasetfieldtype 
set displayorder = 100 
where name = 'otherReferences';

update datasetfieldtype 
set displayorder = 101
where name = 'dataSources';

update datasetfieldtype 
set displayorder = 102
where name = 'originOfSources';

update datasetfieldtype 
set displayorder = 103
where name = 'characteristicOfSources';

update datasetfieldtype 
set displayorder = 104
where name = 'accessToSources';