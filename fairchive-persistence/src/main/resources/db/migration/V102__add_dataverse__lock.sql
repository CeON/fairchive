CREATE TABLE dataverselock (
    id serial4 NOT NULL,
    info varchar(255) NULL,
    reason varchar(255) NOT NULL,
    starttime timestamp NULL,
    dataverse_id int8 NOT NULL,
    user_id int8 NOT NULL,
    CONSTRAINT dataverselock_pkey PRIMARY KEY (id),
    CONSTRAINT fk_dataverselock_datverse_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id),
    CONSTRAINT fk_dataverselock_user_id FOREIGN KEY (user_id) REFERENCES authenticateduser(id)
);