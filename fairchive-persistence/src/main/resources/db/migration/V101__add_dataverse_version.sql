-- collection versions table

CREATE TABLE dataverseversion (
    id serial4 primary key,
    createtime timestamp NOT NULL,
    name text NOT NULL,
    alias text NOT NULL,
    description text NULL,
    additionaldescription text NULL,
    dataversetype text NOT NULL,
    state text NULL,
    affiliation text NULL,
    messagesbannersallowed bool not null default false,
    dataverse_id int8 not null,
    CONSTRAINT unq_datverseversion_0 UNIQUE (dataverse_id, createtime),
    CONSTRAINT fk_dataverseversion_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id)
);

-- assign version to every existing collection

insert into dataverseversion (createtime, name, alias, description, 
                              additionaldescription, dataversetype, state, affiliation, 
                              dataverse_id)
    select now(), name, alias, description, 
       additional_description, dataversetype,'RELEASED', affiliation, 
       id
       from dataverse;

alter table dataverse drop column "name";       
alter table dataverse drop column alias;       
alter table dataverse drop column description;
alter table dataverse drop column additional_description;
alter table dataverse drop column dataversetype;
alter table dataverse drop column affiliation;   


-- reassign contacts from collections to collection versions
alter table dataversecontact add column version_id int8 references dataverseversion(id);

UPDATE dataversecontact dc
SET version_id = dv.id
FROM dataverseversion dv
WHERE dv.dataverse_id = dc.dataverse_id;

alter table dataversecontact ALTER COLUMN displayorder SET default 0;
alter table dataversecontact ALTER COLUMN version_id SET NOT NULL;
alter table dataversecontact drop column dataverse_id;
alter table dataversecontact rename contactemail to email;


-- tigthen definition fo facet
alter table dataversefacet ALTER COLUMN displayorder SET default 0;
alter table dataversefacet ALTER COLUMN displayorder SET not null;
alter table dataversefacet ALTER COLUMN datasetfieldtype_id SET NOT NULL;

-- tighten dataverse field type input levels definition

alter table dataversefieldtypeinputlevel ALTER COLUMN include SET default false;
alter table dataversefieldtypeinputlevel ALTER COLUMN include SET not null;
alter table dataversefieldtypeinputlevel ALTER COLUMN required SET default false;
alter table dataversefieldtypeinputlevel ALTER COLUMN required SET not null;

-- reassing dataverse themes from collections to collection versions
alter table dataversetheme add column version_id int8 references dataverseversion(id);

UPDATE dataversetheme dt
SET version_id = dv.id
FROM dataverseversion dv
WHERE dv.dataverse_id = dt.dataverse_id;


alter table dataversetheme drop column dataverse_id;
