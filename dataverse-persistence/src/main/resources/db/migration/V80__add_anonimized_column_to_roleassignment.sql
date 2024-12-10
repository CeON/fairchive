alter table roleassignment add column if not exists anonymized boolean not null default false;
ALTER TABLE roleassignment DROP CONSTRAINT unq_roleassignment_0;
alter table roleassignment add CONSTRAINT unq_roleassignment_0 UNIQUE (assigneeidentifier, role_id, definitionpoint_id, anonymized);