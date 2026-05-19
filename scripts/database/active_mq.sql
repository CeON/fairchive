CREATE TABLE activemq_acks (
	container varchar(250) NOT NULL,
	sub_dest varchar(250) NULL,
	client_id varchar(250) NOT NULL,
	sub_name varchar(250) NOT NULL,
	selector varchar(250) NULL,
	last_acked_id int8 NULL,
	priority int8 DEFAULT 5 NOT NULL,
	xid varchar(250) NULL,
	CONSTRAINT activemq_acks_pkey PRIMARY KEY (container, client_id, sub_name, priority)
);
CREATE INDEX activemq_acks_xidx ON activemq_acks USING btree (xid);

CREATE TABLE activemq_lock (
	id int8 NOT NULL,
	"time" int8 NULL,
	broker_name varchar(250) NULL,
	CONSTRAINT activemq_lock_pkey PRIMARY KEY (id)
);

CREATE TABLE activemq_msgs (
	id int8 NOT NULL,
	container varchar(250) NULL,
	msgid_prod varchar(250) NULL,
	msgid_seq int8 NULL,
	expiration int8 NULL,
	msg bytea NULL,
	priority int8 NULL,
	xid varchar(250) NULL,
	CONSTRAINT activemq_msgs_pkey PRIMARY KEY (id)
);
CREATE INDEX activemq_msgs_cidx ON activemq_msgs USING btree (container);
CREATE INDEX activemq_msgs_eidx ON activemq_msgs USING btree (expiration);
CREATE INDEX activemq_msgs_midx ON activemq_msgs USING btree (msgid_prod, msgid_seq);
CREATE INDEX activemq_msgs_pidx ON activemq_msgs USING btree (priority);
CREATE INDEX activemq_msgs_xidx ON activemq_msgs USING btree (xid);
