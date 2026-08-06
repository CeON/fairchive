INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) 
    VALUES (nextval('dataverserole_id_seq'), 'collectionCustodian', 'Allows user to edit a collection, but not to publish it or to publish datasets deposited within this collection.', 'Collection Custodian', 2084, NULL);
