-- fixes ORCID values
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'ORCID: ') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'ORCID: %';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'ORCID ') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'ORCID 0%';

update datasetfield set fieldvalue = LTRIM(fieldvalue, 'http://orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'http://orcid.org/%';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'https://orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'https://orcid.org/%';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'https://orcid.org/ ') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'https://orcid.org/ %';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'hhttps://orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'hhttps://orcid.org/%';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'Warsaw, Poland https://orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'Warsaw, Poland https://orcid.org/%';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'ORCID logo https://orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'ORCID logo https://orcid.org/%';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'orcid logo https://orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'orcid logo https://orcid.org/%';
update datasetfield set fieldvalue = LTRIM(fieldvalue, 'orcid.org/') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like 'orcid.org/%';

update datasetfield set fieldvalue = RTRIM(fieldvalue, 'Zbiór') 
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like '%Zbiór';

update datasetfield set fieldvalue = ('0' || fieldvalue)  
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like '000-%';

update datasetfield set fieldvalue = LTRIM(fieldvalue, '/')
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like '/0000-%';
    
update datasetfield set fieldvalue = RTRIM(fieldvalue, '.')
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like '%.';
    
update datasetfield set fieldvalue = RTRIM(fieldvalue, ',')
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier') and fieldvalue like '%,';

    
update datasetfield set fieldvalue = TRIM(fieldvalue)
    where datasetfieldtype_id  in (select id from datasetfieldtype where name = 'authorIdentifier');