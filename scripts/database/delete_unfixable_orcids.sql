-- !!!!!!!  run fix_fixable_orcids.sql before this script,
-- !!!!!!!  otherwse fixable ORCIDs will be lost
DO $$
DECLARE
    ids numeric[];
BEGIN
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
                    controlledvocabularyvalues_id in (select id  
                                                        from controlledvocabularyvalue 
                                                        where
                                                        strvalue = 'ORCID'))) 
        and fieldvalue !~ '^(\d{4}-){3}\d{3}[\dX]$'
        and fieldvalue is not null)
    INTO ids;

    -- clear all unfixable orcid values
    update datasetfield 
    set 
    fieldvalue = null
    where 
    id = any(ids);

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
END $$;