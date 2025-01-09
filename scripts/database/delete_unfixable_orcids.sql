-- !!!!!!!  run fix_fixable_orcids.sql before this script,
-- !!!!!!!  otherwse fixable ORCIDs will be lost
DO $$
DECLARE
    ids numeric[];
    harvested_versions numeric [];
BEGIN
	-- get harvested dataset versions
    select array(select datasetversion.id as id
        from datasetversion join dataset on datasetversion.dataset_id = dataset.id
        where dataset.harvestingclient_id is not null)
    into harvested_versions;
	
	
    -- find indetifiers of all dataset fields with supposed ORCID values 
    -- that don't match ORCIS format
    SELECT ARRAY(select id
        from datasetfield 
         where 
         datasetfieldtype_id in (select id 
                        from datasetfieldtype 
                        where 
                        name = 'authorIdentifier')  
        and
        datasetfieldparent_id in 
            (select datasetfieldparent_id 
             from datasetfield  
             where 
                id in (select datasetfield_id 
                    from datasetfield_controlledvocabularyvalue 
                    where  
                    controlledvocabularyvalues_id in (select controlledvocabularyvalue.id   
                                                      from controlledvocabularyvalue join datasetfieldtype ON controlledvocabularyvalue.datasetfieldtype_id = datasetfieldtype.id
                                                      where controlledvocabularyvalue.strvalue = 'ORCID' and datasetfieldtype.name = 'authorIdentifierScheme'))) 
        and fieldvalue !~ '^(\d{4}-){3}\d{3}[\dX]$'
        and fieldvalue is not null)
    INTO ids;

    -- clear all unfixable orcid values in non-harvested datasets
    update datasetfield 
    set 
    fieldvalue = null
    where 
    id = any(ids) and datasetversion_id != all(harvested_versions);

    -- clear indetified schema selections for deleted ORCIDs
    delete 
    from datasetfield_controlledvocabularyvalue
    where datasetfield_id in (select id from datasetfield 
                              where
                              datasetfieldparent_id in (select datasetfieldparent_id 
                                                        from datasetfield 
                                                        where
                                                        id = any(ids))
                              and
                              datasetfieldtype_id in (select id 
                                                      from datasetfieldtype 
                                                      where
                                                      name = 'authorIdentifierScheme'));
END $$;;