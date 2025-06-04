ALTER TABLE dvobject
ADD CONSTRAINT owner_not_null_for_files_andDatasets CHECK 
        (
            (dtype = 'DataFile' and owner_id is not null)
            or
            (dtype = 'Dataset' and owner_id is not null)
            or
            (dtype = 'Dataverse')
        );