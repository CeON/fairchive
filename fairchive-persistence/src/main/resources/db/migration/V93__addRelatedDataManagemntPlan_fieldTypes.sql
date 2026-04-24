
-- move display orders forward to make place
update datasetfieldtype set displayorder = displayorder + 10  
    where displayorder >= (select max(displayorder) + 1  from datasetfieldtype 
                where parentdatasetfieldtype_id = (select id from datasetfieldtype where name = 'publication'));


-- inserd complex field
INSERT INTO datasetfieldtype (advancedsearchfieldtype,allowcontrolledvocabulary,allowmultiples,description,displayformat,
     displayoncreate,displayorder,facetable,fieldtype,name,required,title,uri,watermark,metadatablock_id,parentdatasetfieldtype_id,
     inputrenderertype,inputrendereroptions,validation,metadata,exporttofile,visiblethroughanonymizedurl,defaultvalue) 
VALUES (false,false,false,'Related Data Management Plan.','',true, (select max(displayorder) + 2  from datasetfieldtype 
                where parentdatasetfieldtype_id = (select id from datasetfieldtype where name = 'publication')) ,false,'NONE','dataManagementPlan',false,
        'Related Data Management Plan',NULL,'', (SELECT id  FROM metadatablock where name = 'citation'),NULL,'TEXT','{}','[{"name":"standard_input"}]','{}',false,false,NULL);
 
  
-- insert subfields
INSERT INTO datasetfieldtype (advancedsearchfieldtype, allowcontrolledvocabulary, 
      allowmultiples, description, displayformat, displayoncreate, displayorder, 
      facetable, fieldtype, name, required, title, uri, watermark, metadatablock_id, 
      parentdatasetfieldtype_id, inputrenderertype, inputrendereroptions, validation, 
      metadata, exporttofile, visiblethroughanonymizedurl, defaultvalue) 
VALUES(true, false, false, 'The full bibliographic citation for this related publication.', 
      '#VALUE', true, 
    (SELECT displayorder + 1 FROM datasetfieldtype where name = 'dataManagementPlan'), 
    false, 'TEXTBOX', 'managementPlanCitation', false, 'Citation', NULL, '', 
    (SELECT id  FROM metadatablock where name = 'citation'), 
    (SELECT id  FROM datasetfieldtype where name = 'dataManagementPlan'), 'TEXTBOX', 
    '{}', '[{\"name\":\"standard_input\"}]', '{}', false, true, NULL);
     

INSERT INTO datasetfieldtype (advancedsearchfieldtype,allowcontrolledvocabulary,
    allowmultiples,description,displayformat,displayoncreate,displayorder,facetable,
    fieldtype,name,required,title,uri,watermark,metadatablock_id,parentdatasetfieldtype_id,
    inputrenderertype,inputrendereroptions,validation,metadata,exporttofile,
    visiblethroughanonymizedurl,defaultvalue) 
VALUES (false,false,false,'Relation Type','#VALUE',true,
    (SELECT displayorder + 2 FROM datasetfieldtype where name = 'dataManagementPlan'),
    false,'TEXT', 'managementRelationType',false,'Relation Type',NULL,'',
    (SELECT id  FROM metadatablock where name = 'citation'),
    (SELECT id  FROM datasetfieldtype where name = 'dataManagementPlan'),'VOCABULARY_SELECT',
    '{"sortByLocalisedStringsOrder" : "true"}','[{"name":"standard_input"}]','{}',false,false,NULL);
    
    
INSERT INTO datasetfieldtype (advancedsearchfieldtype,allowcontrolledvocabulary,
    allowmultiples,description,displayformat,displayoncreate,displayorder,facetable,
    fieldtype,name,required,title,uri,watermark,metadatablock_id,parentdatasetfieldtype_id,
    inputrenderertype,inputrendereroptions,validation,metadata,exporttofile,
    visiblethroughanonymizedurl,defaultvalue)
VALUES (false,false,false,'ID Type','#VALUE',true,
    (SELECT displayorder + 3 FROM datasetfieldtype where name = 'dataManagementPlan'),
    false,'TEXT','managementRelationIDType', false,'ID Type',NULL,'',
    (SELECT id  FROM metadatablock where name = 'citation'),
    (SELECT id  FROM datasetfieldtype where name = 'dataManagementPlan'),'VOCABULARY_SELECT',
    '{"sortByLocalisedStringsOrder" : "true"}', '[{"name":"standard_input"}]','{}',false,false,NULL);
    

INSERT INTO datasetfieldtype (advancedsearchfieldtype,allowcontrolledvocabulary,
    allowmultiples,description,displayformat,displayoncreate,displayorder,facetable,
    fieldtype,name,required,title,uri,watermark,metadatablock_id,parentdatasetfieldtype_id,
    inputrenderertype,inputrendereroptions,validation,metadata,exporttofile,
    visiblethroughanonymizedurl,defaultvalue) 
VALUES (false,false,false,'ID Number.','#VALUE',true,
    (SELECT displayorder + 4 FROM datasetfieldtype where name = 'dataManagementPlan'),
     false,'TEXT', 'managementRelationID',false,'ID Number',NULL,'',
    (SELECT id  FROM metadatablock where name = 'citation'),
    (SELECT id  FROM datasetfieldtype where name = 'dataManagementPlan'),
    'TEXT','{}','[{"name":"standard_input"}]','{}',false,false,NULL);
     
    
    
INSERT INTO datasetfieldtype (advancedsearchfieldtype,allowcontrolledvocabulary,
    allowmultiples,description,displayformat,displayoncreate,displayorder,facetable,
    fieldtype,name,required,title,uri,watermark,metadatablock_id,parentdatasetfieldtype_id,
    inputrenderertype,inputrendereroptions,validation,metadata,exporttofile,
    visiblethroughanonymizedurl,defaultvalue) 
VALUES (false,false,false,'URL','"<a href="#VALUE" target="_blank">#VALUE</a>"', true,
    (SELECT displayorder + 5 FROM datasetfieldtype where name = 'dataManagementPlan'),
    false,'URL','managementRelationURL',false,'URL',NULL,'http://... or https://...',
    (SELECT id  FROM metadatablock where name = 'citation'),
    (SELECT id  FROM datasetfieldtype where name = 'dataManagementPlan'),
    'TEXT','{}','[{"name":"standard_url"}]','{}',false,false,NULL);
     

-- insert controlled vocabularies
INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id) 
VALUES(0, 'dmpDescribes', (select id from datasetfieldtype where name = 'managementRelationType')); 

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(1, 'dmpCites', (select id from datasetfieldtype where name = 'managementRelationType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id) 
VALUES(2, 'dmpReferences', (select id from datasetfieldtype where name = 'managementRelationType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id) 
VALUES(3, 'datasetCites', (select id from datasetfieldtype where name = 'managementRelationType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES (4 , 'datasetReferences', (select id from datasetfieldtype where name = 'managementRelationType'));


-- insert controlled vocabularies
INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(0, 'doi', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(1, 'ark', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(2, 'arXiv', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(3, 'bibcode', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(4, 'ean13', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(5, 'eissn', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(6, 'handle', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(7, 'isbn', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(8, 'issn', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(9, 'istc', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(10, 'lissn', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(11, 'lsid', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(12, 'pmid', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(13, 'purl', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(14, 'upc', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(15, 'url', (select id from datasetfieldtype where name = 'managementRelationIDType'));

INSERT INTO controlledvocabularyvalue (displayorder, strvalue, datasetfieldtype_id)
VALUES(16, 'urn', (select id from datasetfieldtype where name = 'managementRelationIDType'));
