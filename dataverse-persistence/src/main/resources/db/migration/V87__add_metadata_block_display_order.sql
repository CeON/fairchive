
alter table metadatablock add column displayorder int not null default 0;

update metadatablock set displayorder=id * 10;