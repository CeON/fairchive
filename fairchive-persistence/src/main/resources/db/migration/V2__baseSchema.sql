create table workflow
(
	id serial not null
		constraint workflow_pkey
			primary key,
	name varchar(255)
);



create table oaiset
(
	id serial not null
		constraint oaiset_pkey
			primary key,
	definition text,
	deleted boolean,
	description text,
	name text,
	spec text,
	updateinprogress boolean,
	version bigint
);



create table storagesite
(
	id serial not null
		constraint storagesite_pkey
			primary key,
	hostname text,
	name text,
	primarystorage boolean not null,
	transferprotocols text
);



create table oairecord
(
	id serial not null
		constraint oairecord_pkey
			primary key,
	globalid varchar(255),
	lastupdatetime timestamp,
	removed boolean,
	setname varchar(255)
);



create table samlgroup
(
	id serial not null
		constraint samlgroup_pkey
			primary key,
	name varchar(255) not null,
	entityid varchar(255) not null
);



create table persistedglobalgroup
(
	id bigint not null
		constraint persistedglobalgroup_pkey
			primary key,
	dtype varchar(31),
	description varchar(255),
	displayname varchar(255),
	persistedgroupalias varchar(255)
		constraint persistedglobalgroup_persistedgroupalias_key
			unique
);



create table ipv6range
(
	id bigint not null
		constraint ipv6range_pkey
			primary key,
	bottoma bigint,
	bottomb bigint,
	bottomc bigint,
	bottomd bigint,
	topa bigint,
	topb bigint,
	topc bigint,
	topd bigint,
	owner_id bigint
		constraint fk_ipv6range_owner_id
			references persistedglobalgroup
);



create index index_ipv6range_owner_id
	on ipv6range (owner_id);

create index index_persistedglobalgroup_dtype
	on persistedglobalgroup (dtype);

create table ipv4range
(
	id bigint not null
		constraint ipv4range_pkey
			primary key,
	bottomaslong bigint,
	topaslong bigint,
	owner_id bigint
		constraint fk_ipv4range_owner_id
			references persistedglobalgroup
);



create index index_ipv4range_owner_id
	on ipv4range (owner_id);

create table metric
(
	id serial not null
		constraint metric_pkey
			primary key,
	lastcalleddate timestamp not null,
	name varchar(255) not null,
	valueJson text,
	dayString text,
	dataLocation text
);



create index index_metric_id
	on metric (id);

create table workflowstepdata
(
	id serial not null
		constraint workflowstepdata_pkey
			primary key,
	providerid varchar(255),
	steptype varchar(255),
	parent_id bigint
		constraint fk_workflowstepdata_parent_id
			references workflow,
	index integer
);



create table customfieldmap
(
	id serial not null
		constraint customfieldmap_pkey
			primary key,
	sourcedatasetfield varchar(255),
	sourcetemplate varchar(255),
	targetdatasetfield varchar(255)
);



create index index_customfieldmap_sourcedatasetfield
	on customfieldmap (sourcedatasetfield);

create index index_customfieldmap_sourcetemplate
	on customfieldmap (sourcetemplate);

create table actionlogrecord
(
	id varchar(36) not null
		constraint actionlogrecord_pkey
			primary key,
	actionresult varchar(255),
	actionsubtype varchar(255),
	actiontype varchar(255),
	endtime timestamp,
	info text,
	starttime timestamp,
	useridentifier varchar(255)
);



create index index_actionlogrecord_useridentifier
	on actionlogrecord (useridentifier);

create index index_actionlogrecord_actiontype
	on actionlogrecord (actiontype);

create index index_actionlogrecord_starttime
	on actionlogrecord (starttime);

create table foreignmetadataformatmapping
(
	id serial not null
		constraint foreignmetadataformatmapping_pkey
			primary key,
	displayname varchar(255) not null,
	name varchar(255) not null,
	schemalocation varchar(255),
	startelement varchar(255)
);



create index index_foreignmetadataformatmapping_name
	on foreignmetadataformatmapping (name);

create table externaltool
(
	id serial not null
		constraint externaltool_pkey
			primary key,
	description text,
	displayname varchar(255) not null,
	toolparameters varchar(255) not null,
	toolurl varchar(255) not null,
	type varchar(255) not null,
	contenttype text NOT NULL default 'text/tab-separated-values'
);



create table defaultvalueset
(
	id serial not null
		constraint defaultvalueset_pkey
			primary key,
	name varchar(255) not null
);



create table authenticateduser
(
	id serial not null
		constraint authenticateduser_pkey
			primary key,
	affiliation varchar(255),
	createdtime timestamp not null,
	email varchar(255) not null
		constraint authenticateduser_email_key
			unique,
	emailconfirmed timestamp,
	firstname varchar(255),
	lastapiusetime timestamp,
	lastlogintime timestamp,
	lastname varchar(255),
	position varchar(255),
	orcid text,
	affiliationror text,
	superuser boolean,
	notificationslanguage text NOT NULL DEFAULT 'en',
	useridentifier varchar(255) not null
		constraint authenticateduser_useridentifier_key
			unique
);



create table confirmemaildata
(
	id serial not null
		constraint confirmemaildata_pkey
			primary key,
	created timestamp not null,
	expires timestamp not null,
	token varchar(255),
	authenticateduser_id bigint not null
		constraint confirmemaildata_authenticateduser_id_key
			unique
		constraint fk_confirmemaildata_authenticateduser_id
			references authenticateduser
);



create index index_confirmemaildata_token
	on confirmemaildata (token);

create index index_confirmemaildata_authenticateduser_id
	on confirmemaildata (authenticateduser_id);

create table oauth2tokendata
(
	id serial not null
		constraint oauth2tokendata_pkey
			primary key,
	accesstoken text,
	expirydate timestamp,
	oauthproviderid varchar(255),
	rawresponse text,
	refreshtoken varchar(64),
	scope varchar(64),
	tokentype varchar(32),
	user_id bigint
		constraint fk_oauth2tokendata_user_id
			references authenticateduser
);



create table dvobject
(
	id serial not null
		constraint dvobject_pkey
			primary key,
	dtype varchar(31),
	authority varchar(255),
	createdate timestamp not null,
	globalidcreatetime timestamp,
	identifier varchar(255),
	identifierregistered boolean,
	indextime timestamp,
	modificationtime timestamp not null,
	permissionindextime timestamp,
	permissionmodificationtime timestamp,
	previewimageavailable boolean,
	protocol varchar(255),
	publicationdate timestamp,
	storageidentifier varchar(255),
	creator_id bigint
		constraint fk_dvobject_creator_id
			references authenticateduser,
	owner_id bigint
		constraint fk_dvobject_owner_id
			references dvobject,
	releaseuser_id bigint
		constraint fk_dvobject_releaseuser_id
			references authenticateduser,
	constraint unq_dvobject_0
		unique (authority, protocol, identifier)
);



create table dataversetheme
(
	id serial not null
		constraint dataversetheme_pkey
			primary key,
	backgroundcolor varchar(255),
	linkcolor varchar(255),
	linkurl varchar(255),
	logo varchar(255),
	logoalignment varchar(255),
	logobackgroundcolor varchar(255),
	logoformat varchar(255),
	tagline varchar(255),
	textcolor varchar(255),
	dataverse_id bigint
		constraint fk_dataversetheme_dataverse_id
			references dvobject
);



create index index_dataversetheme_dataverse_id
	on dataversetheme (dataverse_id);

create table datafilecategory
(
	id serial not null
		constraint datafilecategory_pkey
			primary key,
	name varchar(255) not null,
	dataset_id bigint not null
		constraint fk_datafilecategory_dataset_id
			references dvobject
);



create index index_datafilecategory_dataset_id
	on datafilecategory (dataset_id);

create table dataverselinkingdataverse
(
	id serial not null
		constraint dataverselinkingdataverse_pkey
			primary key,
	linkcreatetime timestamp,
	dataverse_id bigint not null
		constraint fk_dataverselinkingdataverse_dataverse_id
			references dvobject,
	linkingdataverse_id bigint not null
		constraint fk_dataverselinkingdataverse_linkingdataverse_id
			references dvobject
);



create index index_dataverselinkingdataverse_dataverse_id
	on dataverselinkingdataverse (dataverse_id);

create index index_dataverselinkingdataverse_linkingdataverse_id
	on dataverselinkingdataverse (linkingdataverse_id);

create table metadatablock
(
	id serial not null
		constraint metadatablock_pkey
			primary key,
	displayname varchar(255) not null,
	name varchar(255) not null,
	namespaceuri text,
	owner_id bigint
		constraint fk_metadatablock_owner_id
			references dvobject
);



create index index_metadatablock_name
	on metadatablock (name);

create index index_metadatablock_owner_id
	on metadatablock (owner_id);

create index index_dvobject_dtype
	on dvobject (dtype);

create index index_dvobject_owner_id
	on dvobject (owner_id);

create index index_dvobject_creator_id
	on dvobject (creator_id);

create index index_dvobject_releaseuser_id
	on dvobject (releaseuser_id);

create table dataversefeatureddataverse
(
	id serial not null
		constraint dataversefeatureddataverse_pkey
			primary key,
	displayorder integer,
	dataverse_id bigint
		constraint fk_dataversefeatureddataverse_dataverse_id
			references dvobject,
	featureddataverse_id bigint
		constraint fk_dataversefeatureddataverse_featureddataverse_id
			references dvobject
);



create index index_dataversefeatureddataverse_dataverse_id
	on dataversefeatureddataverse (dataverse_id);

create index index_dataversefeatureddataverse_featureddataverse_id
	on dataversefeatureddataverse (featureddataverse_id);

create index index_dataversefeatureddataverse_displayorder
	on dataversefeatureddataverse (displayorder);

create table harvestingclient
(
	id serial not null
		constraint harvestingclient_pkey
			primary key,
	archivedescription text,
	archiveurl varchar(255),
	deleted boolean,
	harveststyle varchar(255),
	harvesttype varchar(255),
	harvestingnow boolean,
	harvestingset varchar(255),
	harvestingurl varchar(255),
	metadataprefix varchar(255),
	name varchar(255) not null
		constraint harvestingclient_name_key
			unique,
	scheduledayofweek integer,
	schedulehourofday integer,
	scheduleperiod varchar(255),
	scheduled boolean,
	dataverse_id bigint
		constraint fk_harvestingclient_dataverse_id
			references dvobject
);



create index index_harvestingclient_dataverse_id
	on harvestingclient (dataverse_id);

create index index_harvestingclient_harvesttype
	on harvestingclient (harvesttype);

create index index_harvestingclient_harveststyle
	on harvestingclient (harveststyle);

create index index_harvestingclient_harvestingurl
	on harvestingclient (harvestingurl);

create table apitoken
(
	id serial not null
		constraint apitoken_pkey
			primary key,
	createtime timestamp not null,
	disabled boolean not null,
	expiretime timestamp not null,
	tokenstring varchar(255) not null
		constraint apitoken_tokenstring_key
			unique,
	authenticateduser_id bigint not null
		constraint fk_apitoken_authenticateduser_id
			references authenticateduser
);



create index index_apitoken_authenticateduser_id
	on apitoken (authenticateduser_id);

create table dataversetextmessage
(
	id serial not null
		constraint dataversetextmessage_pkey
			primary key,
	active boolean,
	fromtime timestamp,
	totime timestamp,
	version bigint,
	dataverse_id bigint
		constraint fk_dataversetextmessage_dataverse_id
			references dvobject
);



create index index_dataversetextmessage_dataverse_id
	on dataversetextmessage (dataverse_id);

create table usernotification
(
	id serial not null
		constraint usernotification_pkey
			primary key,
	emailed boolean,
	objectid bigint,
	readnotification boolean,
	additionalmessage varchar(5000),
	senddate timestamp,
	searchLabel TEXT,
	type varchar(255) not null,
	requestor_id bigint
		constraint fk_usernotification_requestor_id
			references authenticateduser,
	user_id bigint not null
		constraint fk_usernotification_user_id
			references authenticateduser
);


create index index_usernotification_user_id
	on usernotification (user_id);
	
CREATE INDEX index_usernotification_searchLabel on usernotification (searchLabel);

create table guestbook
(
	id serial not null
		constraint guestbook_pkey
			primary key,
	createtime timestamp not null,
	emailrequired boolean,
	enabled boolean,
	institutionrequired boolean,
	name varchar(255),
	namerequired boolean,
	positionrequired boolean,
	dataverse_id bigint
		constraint fk_guestbook_dataverse_id
			references dvobject
);



create table customquestion
(
	id serial not null
		constraint customquestion_pkey
			primary key,
	displayorder integer,
	hidden boolean,
	questionstring varchar(255) not null,
	questiontype varchar(255) not null,
	required boolean,
	guestbook_id bigint not null
		constraint fk_customquestion_guestbook_id
			references guestbook
);



create index index_customquestion_guestbook_id
	on customquestion (guestbook_id);

create table maplayermetadata
(
	id serial not null
		constraint maplayermetadata_pkey
			primary key,
	embedmaplink varchar(255) not null,
	isjoinlayer boolean,
	joindescription text,
	lastverifiedstatus integer,
	lastverifiedtime timestamp,
	layerlink varchar(255) not null,
	layername varchar(255) not null,
	mapimagelink varchar(255),
	maplayerlinks text,
	worldmapusername varchar(255) not null,
	dataset_id bigint not null
		constraint fk_maplayermetadata_dataset_id
			references dvobject,
	datafile_id bigint not null
		constraint maplayermetadata_datafile_id_key
			unique
		constraint fk_maplayermetadata_datafile_id
			references dvobject
);



create index index_maplayermetadata_dataset_id
	on maplayermetadata (dataset_id);

create table savedsearch
(
	id serial not null
		constraint savedsearch_pkey
			primary key,
	query text,
	creator_id bigint not null
		constraint fk_savedsearch_creator_id
			references authenticateduser,
	definitionpoint_id bigint not null
		constraint fk_savedsearch_definitionpoint_id
			references dvobject
);



create table savedsearchfilterquery
(
	id serial not null
		constraint savedsearchfilterquery_pkey
			primary key,
	filterquery text,
	savedsearch_id bigint not null
		constraint fk_savedsearchfilterquery_savedsearch_id
			references savedsearch
);



create index index_savedsearchfilterquery_savedsearch_id
	on savedsearchfilterquery (savedsearch_id);

create index index_savedsearch_definitionpoint_id
	on savedsearch (definitionpoint_id);

create index index_savedsearch_creator_id
	on savedsearch (creator_id);

create table explicitgroup
(
	id serial not null
		constraint explicitgroup_pkey
			primary key,
	description varchar(1024),
	displayname varchar(255),
	groupalias varchar(255)
		constraint explicitgroup_groupalias_key
			unique,
	groupaliasinowner varchar(255),
	owner_id bigint
		constraint fk_explicitgroup_owner_id
			references dvobject
);



create index index_explicitgroup_owner_id
	on explicitgroup (owner_id);

create index index_explicitgroup_groupaliasinowner
	on explicitgroup (groupaliasinowner);

create unique index index_authenticateduser_lower_email
	on authenticateduser (lower(email::text));

create table datatable
(
	id serial not null
		constraint datatable_pkey
			primary key,
	casequantity bigint,
	originalfileformat varchar(255),
	originalformatversion varchar(255),
	originalfilesize BIGINT,
	recordspercase bigint,
	unf varchar(255) not null,
	varquantity bigint,
	datafile_id bigint not null
		constraint fk_datatable_datafile_id
			references dvobject
);



create index index_datatable_datafile_id
	on datatable (datafile_id);

create table ingestreport
(
	id serial not null
		constraint ingestreport_pkey
			primary key,
	endtime timestamp,
	errorkey text default 'UNKNOWN_ERROR',
	starttime timestamp,
	status integer,
	type integer,
	datafile_id bigint not null
		constraint fk_ingestreport_datafile_id
			references dvobject
);

create index index_ingestreport_datafile_id
	on ingestreport (datafile_id);
	
CREATE TABLE ingestreport_errorarguments
(
    ingestreport_id bigint
        constraint fk_ingestreport_reportarguments_ingestreport_id
            references ingestreport,
    errorarguments varchar,
    errorarguments_order integer
);


create table authenticationproviderrow
(
	id varchar(255) not null
		constraint authenticationproviderrow_pkey
			primary key,
	enabled boolean,
	factoryalias varchar(255),
	factorydata text,
	subtitle varchar(255),
	title varchar(255)
);



create index index_authenticationproviderrow_enabled
	on authenticationproviderrow (enabled);

create table foreignmetadatafieldmapping
(
	id serial not null
		constraint foreignmetadatafieldmapping_pkey
			primary key,
	datasetfieldname text,
	foreignfieldxpath text,
	isattribute boolean,
	foreignmetadataformatmapping_id bigint
		constraint fk_foreignmetadatafieldmapping_foreignmetadataformatmapping_id
			references foreignmetadataformatmapping,
	parentfieldmapping_id bigint
		constraint fk_foreignmetadatafieldmapping_parentfieldmapping_id
			references foreignmetadatafieldmapping,
	constraint unq_foreignmetadatafieldmapping_0
		unique (foreignmetadataformatmapping_id, foreignfieldxpath)
);



create index index_foreignmetadatafieldmapping_foreignmetadataformatmapping_
	on foreignmetadatafieldmapping (foreignmetadataformatmapping_id);

create index index_foreignmetadatafieldmapping_foreignfieldxpath
	on foreignmetadatafieldmapping (foreignfieldxpath);

create index index_foreignmetadatafieldmapping_parentfieldmapping_id
	on foreignmetadatafieldmapping (parentfieldmapping_id);

create table dataverselocalizedmessage
(
	id serial not null
		constraint dataverselocalizedmessage_pkey
			primary key,
	locale varchar(255) not null,
	message text not null,
	dataversetextmessage_id bigint
		constraint fk_dataverselocalizedmessage_dataversetextmessage_id
			references dataversetextmessage
);



create index index_dataverselocalizedmessage_dataversetextmessage_id
	on dataverselocalizedmessage (dataversetextmessage_id);

create table customquestionvalue
(
	id serial not null
		constraint customquestionvalue_pkey
			primary key,
	displayorder integer,
	valuestring varchar(255) not null,
	customquestion_id bigint not null
		constraint fk_customquestionvalue_customquestion_id
			references customquestion
);



create table datasetlinkingdataverse
(
	id serial not null
		constraint datasetlinkingdataverse_pkey
			primary key,
	linkcreatetime timestamp not null,
	dataset_id bigint not null
		constraint fk_datasetlinkingdataverse_dataset_id
			references dvobject,
	linkingdataverse_id bigint not null
		constraint fk_datasetlinkingdataverse_linkingdataverse_id
			references dvobject
);



create index index_datasetlinkingdataverse_dataset_id
	on datasetlinkingdataverse (dataset_id);

create index index_datasetlinkingdataverse_linkingdataverse_id
	on datasetlinkingdataverse (linkingdataverse_id);

create table clientharvestrun
(
	id serial not null
		constraint clientharvestrun_pkey
			primary key,
	deleteddatasetcount bigint,
	faileddatasetcount bigint,
	finishtime timestamp,
	harvestresult integer,
	harvesteddatasetcount bigint,
	starttime timestamp,
	harvestingclient_id bigint not null
		constraint fk_clientharvestrun_harvestingclient_id
			references harvestingclient
);



create table worldmapauth_tokentype
(
	id serial not null
		constraint worldmapauth_tokentype_pkey
			primary key,
	contactemail varchar(255),
	created timestamp not null,
	hostname varchar(255),
	ipaddress varchar(255),
	mapitlink varchar(255) not null,
	md5 varchar(255) not null,
	modified timestamp not null,
	name varchar(255) not null,
	timelimitminutes integer default 30,
	timelimitseconds bigint default 1800
);



create table worldmapauth_token
(
	id serial not null
		constraint worldmapauth_token_pkey
			primary key,
	created timestamp not null,
	hasexpired boolean not null,
	lastrefreshtime timestamp not null,
	modified timestamp not null,
	token varchar(255),
	application_id bigint not null
		constraint fk_worldmapauth_token_application_id
			references worldmapauth_tokentype,
	datafile_id bigint not null
		constraint fk_worldmapauth_token_datafile_id
			references dvobject,
	dataverseuser_id bigint not null
		constraint fk_worldmapauth_token_dataverseuser_id
			references authenticateduser
);



create unique index token_value
	on worldmapauth_token (token);

create index index_worldmapauth_token_application_id
	on worldmapauth_token (application_id);

create index index_worldmapauth_token_datafile_id
	on worldmapauth_token (datafile_id);

create index index_worldmapauth_token_dataverseuser_id
	on worldmapauth_token (dataverseuser_id);

create unique index application_name
	on worldmapauth_tokentype (name);

create table datafiletag
(
	id serial not null
		constraint datafiletag_pkey
			primary key,
	type integer not null,
	datafile_id bigint not null
		constraint fk_datafiletag_datafile_id
			references dvobject
);



create index index_datafiletag_datafile_id
	on datafiletag (datafile_id);

create table authenticateduserlookup
(
	id serial not null
		constraint authenticateduserlookup_pkey
			primary key,
	authenticationproviderid varchar(255),
	persistentuserid varchar(255),
	authenticateduser_id bigint not null
		constraint authenticateduserlookup_authenticateduser_id_key
			unique
		constraint fk_authenticateduserlookup_authenticateduser_id
			references authenticateduser,
	constraint unq_authenticateduserlookup_0
		unique (persistentuserid, authenticationproviderid)
);



create table ingestrequest
(
	id serial not null
		constraint ingestrequest_pkey
			primary key,
	controlcard varchar(255),
	forcetypecheck boolean,
	labelsfile varchar(255),
	textencoding varchar(255),
	datafile_id bigint
		constraint fk_ingestrequest_datafile_id
			references dvobject
);



create index index_ingestrequest_datafile_id
	on ingestrequest (datafile_id);

create table setting
(
	name varchar(255) not null
		constraint setting_pkey
			primary key,
	content text
);



create table dataversecontact
(
	id serial not null
		constraint dataversecontact_pkey
			primary key,
	contactemail varchar(255) not null,
	displayorder integer,
	dataverse_id bigint
		constraint fk_dataversecontact_dataverse_id
			references dvobject
);



create index index_dataversecontact_dataverse_id
	on dataversecontact (dataverse_id);

create index index_dataversecontact_contactemail
	on dataversecontact (contactemail);

create index index_dataversecontact_displayorder
	on dataversecontact (displayorder);

create table datavariable
(
	id serial not null
		constraint datavariable_pkey
			primary key,
	factor boolean,
	fileendposition bigint,
	fileorder integer,
	filestartposition bigint,
	format varchar(255),
	formatcategory varchar(255),
	interval integer,
	label text,
	name varchar(255),
	numberofdecimalpoints bigint,
	orderedfactor boolean,
	recordsegmentnumber bigint,
	type integer,
	unf varchar(255),
	weighted boolean,
	datatable_id bigint not null
		constraint fk_datavariable_datatable_id
			references datatable
);



create table variablerangeitem
(
	id serial not null
		constraint variablerangeitem_pkey
			primary key,
	value numeric(38),
	datavariable_id bigint not null
		constraint fk_variablerangeitem_datavariable_id
			references datavariable
);



create index index_variablerangeitem_datavariable_id
	on variablerangeitem (datavariable_id);

create table variablerange
(
	id serial not null
		constraint variablerange_pkey
			primary key,
	beginvalue varchar(255),
	beginvaluetype integer,
	endvalue varchar(255),
	endvaluetype integer,
	datavariable_id bigint not null
		constraint fk_variablerange_datavariable_id
			references datavariable
);



create index index_variablerange_datavariable_id
	on variablerange (datavariable_id);

create table summarystatistic
(
	id serial not null
		constraint summarystatistic_pkey
			primary key,
	type integer,
	value varchar(255),
	datavariable_id bigint not null
		constraint fk_summarystatistic_datavariable_id
			references datavariable
);



create index index_summarystatistic_datavariable_id
	on summarystatistic (datavariable_id);

create table variablecategory
(
	id serial not null
		constraint variablecategory_pkey
			primary key,
	catorder integer,
	frequency double precision,
	label varchar(255),
	missing boolean,
	value varchar(255),
	datavariable_id bigint not null
		constraint fk_variablecategory_datavariable_id
			references datavariable
);



create index index_variablecategory_datavariable_id
	on variablecategory (datavariable_id);

create index index_datavariable_datatable_id
	on datavariable (datatable_id);

create table datafile
(
	id bigint not null
		constraint datafile_pkey
			primary key
		constraint fk_datafile_id
			references dvobject,
	checksumtype varchar(255) not null,
	checksumvalue varchar(255) not null,
	contenttype varchar(255) not null,
	filesize bigint,
	uncompressedsize BIGINT NOT NULL DEFAULT 0,
	ingeststatus char,
	previousdatafileid bigint,
	prov_entityname text,
	restricted boolean,
	rootdatafileid bigint not null
);



create index index_datafile_ingeststatus
	on datafile (ingeststatus);

create index index_datafile_checksumvalue
	on datafile (checksumvalue);

create index index_datafile_contenttype
	on datafile (contenttype);

create index index_datafile_restricted
	on datafile (restricted);

create table builtinuser
(
	id serial not null
		constraint builtinuser_pkey
			primary key,
	encryptedpassword varchar(255),
	passwordencryptionversion integer,
	username varchar(255) not null
		constraint builtinuser_username_key
			unique
);



create table passwordresetdata
(
	id serial not null
		constraint passwordresetdata_pkey
			primary key,
	created timestamp not null,
	expires timestamp not null,
	reason varchar(255),
	token varchar(255),
	builtinuser_id bigint not null
		constraint fk_passwordresetdata_builtinuser_id
			references builtinuser
);



create index index_passwordresetdata_token
	on passwordresetdata (token);

create index index_passwordresetdata_builtinuser_id
	on passwordresetdata (builtinuser_id);

create index index_builtinuser_username
	on builtinuser (username);

create table datasetversion
(
	id serial not null
		constraint datasetversion_pkey
			primary key,
	unf varchar(255),
	archivenote varchar(1000),
	archivetime timestamp,
	createtime timestamp not null,
	deaccessionlink varchar(255),
	lastupdatetime timestamp not null,
	minorversionnumber bigint,
	releasetime timestamp,
	version bigint,
	versionnote varchar(1000),
	versionnumber bigint,
	versionstate varchar(255),
	archivalcopylocation text,
	dataset_id bigint
		constraint fk_datasetversion_dataset_id
			references dvobject,
	constraint unq_datasetversion_0
		unique (dataset_id, versionnumber, minorversionnumber)
);



create table workflowcomment
(
	id serial not null
		constraint workflowcomment_pkey
			primary key,
	created timestamp not null,
	message text,
	type varchar(255) not null,
	authenticateduser_id bigint
		constraint fk_workflowcomment_authenticateduser_id
			references authenticateduser,
	datasetversion_id bigint not null
		constraint fk_workflowcomment_datasetversion_id
			references datasetversion
);



create index index_datasetversion_dataset_id
	on datasetversion (dataset_id);

create unique index one_draft_version_per_dataset
	on datasetversion (dataset_id)
	where ((versionstate)::text = 'DRAFT'::text);

create table filemetadata
(
	id serial not null
		constraint filemetadata_pkey
			primary key,
	description text,
	directorylabel varchar(255),
	label varchar(255) not null,
	prov_freeform text,
	restricted boolean,
	version bigint,
	datafile_id bigint not null
		constraint fk_filemetadata_datafile_id
			references dvobject,
	datasetversion_id bigint not null
		constraint fk_filemetadata_datasetversion_id
			references datasetversion,
	displayorder integer
);



create index index_filemetadata_datafile_id
	on filemetadata (datafile_id);

create index index_filemetadata_datasetversion_id
	on filemetadata (datasetversion_id);

create table doidataciteregistercache
(
	id serial not null
		constraint doidataciteregistercache_pkey
			primary key,
	doi varchar(255)
		constraint doidataciteregistercache_doi_key
			unique,
	status varchar(255),
	url varchar(255),
	xml text
);



create table harvestingdataverseconfig
(
	id bigint not null
		constraint harvestingdataverseconfig_pkey
			primary key,
	archivedescription text,
	archiveurl varchar(255),
	harveststyle varchar(255),
	harvesttype varchar(255),
	harvestingset varchar(255),
	harvestingurl varchar(255),
	dataverse_id bigint
		constraint fk_harvestingdataverseconfig_dataverse_id
			references dvobject
);



create index index_harvestingdataverseconfig_dataverse_id
	on harvestingdataverseconfig (dataverse_id);

create index index_harvestingdataverseconfig_harvesttype
	on harvestingdataverseconfig (harvesttype);

create index index_harvestingdataverseconfig_harveststyle
	on harvestingdataverseconfig (harveststyle);

create index index_harvestingdataverseconfig_harvestingurl
	on harvestingdataverseconfig (harvestingurl);

create table alternativepersistentidentifier
(
	id serial not null
		constraint alternativepersistentidentifier_pkey
			primary key,
	authority varchar(255),
	globalidcreatetime timestamp,
	identifier varchar(255),
	identifierregistered boolean,
	protocol varchar(255),
	storagelocationdesignator boolean,
	dvobject_id bigint not null
		constraint fk_alternativepersistentidentifier_dvobject_id
			references dvobject
);



create table datasetversionuser
(
	id serial not null
		constraint datasetversionuser_pkey
			primary key,
	lastupdatedate timestamp not null,
	authenticateduser_id bigint
		constraint fk_datasetversionuser_authenticateduser_id
			references authenticateduser,
	datasetversion_id bigint
		constraint fk_datasetversionuser_datasetversion_id
			references datasetversion
);



create index index_datasetversionuser_authenticateduser_id
	on datasetversionuser (authenticateduser_id);

create index index_datasetversionuser_datasetversion_id
	on datasetversionuser (datasetversion_id);

create table guestbookresponse
(
	id serial not null
		constraint guestbookresponse_pkey
			primary key,
	downloadtype varchar(255),
	email varchar(255),
	institution varchar(255),
	name varchar(255),
	position varchar(255),
	responsetime timestamp,
	sessionid varchar(255),
	authenticateduser_id bigint
		constraint fk_guestbookresponse_authenticateduser_id
			references authenticateduser,
	datafile_id bigint not null
		constraint fk_guestbookresponse_datafile_id
			references dvobject,
	dataset_id bigint not null
		constraint fk_guestbookresponse_dataset_id
			references dvobject,
	datasetversion_id bigint
		constraint fk_guestbookresponse_datasetversion_id
			references datasetversion,
	guestbook_id bigint not null
		constraint fk_guestbookresponse_guestbook_id
			references guestbook
);



create index index_guestbookresponse_guestbook_id
	on guestbookresponse (guestbook_id);

create index index_guestbookresponse_datafile_id
	on guestbookresponse (datafile_id);

create index index_guestbookresponse_dataset_id
	on guestbookresponse (dataset_id);

create table customquestionresponse
(
	id serial not null
		constraint customquestionresponse_pkey
			primary key,
	response text,
	customquestion_id bigint not null
		constraint fk_customquestionresponse_customquestion_id
			references customquestion,
	guestbookresponse_id bigint not null
		constraint fk_customquestionresponse_guestbookresponse_id
			references guestbookresponse
);



create index index_customquestionresponse_guestbookresponse_id
	on customquestionresponse (guestbookresponse_id);

create table template
(
	id serial not null
		constraint template_pkey
			primary key,
	createtime timestamp not null,
	name varchar(255) not null,
	usagecount bigint,
	dataverse_id bigint
		constraint fk_template_dataverse_id
			references dvobject
);



create index index_template_dataverse_id
	on template (dataverse_id);

create table datasetlock
(
	id serial not null
		constraint datasetlock_pkey
			primary key,
	info varchar(255),
	reason varchar(255) not null,
	starttime timestamp,
	dataset_id bigint not null
		constraint fk_datasetlock_dataset_id
			references dvobject,
	user_id bigint not null
		constraint fk_datasetlock_user_id
			references authenticateduser
);



create index index_datasetlock_user_id
	on datasetlock (user_id);

create index index_datasetlock_dataset_id
	on datasetlock (dataset_id);

create table dataverserole
(
	id serial not null
		constraint dataverserole_pkey
			primary key,
	alias varchar(255) not null
		constraint dataverserole_alias_key
			unique,
	description varchar(255),
	name varchar(255) not null,
	permissionbits bigint,
	owner_id bigint
		constraint fk_dataverserole_owner_id
			references dvobject
);



create table roleassignment
(
	id serial not null
		constraint roleassignment_pkey
			primary key,
	assigneeidentifier varchar(255) not null,
	privateurltoken varchar(255),
	definitionpoint_id bigint not null
		constraint fk_roleassignment_definitionpoint_id
			references dvobject,
	role_id bigint not null
		constraint fk_roleassignment_role_id
			references dataverserole,
	constraint unq_roleassignment_0
		unique (assigneeidentifier, role_id, definitionpoint_id)
);



create index index_roleassignment_assigneeidentifier
	on roleassignment (assigneeidentifier);

create index index_roleassignment_definitionpoint_id
	on roleassignment (definitionpoint_id);

create index index_roleassignment_role_id
	on roleassignment (role_id);

create table dataverse
(
	id bigint not null
		constraint dataverse_pkey
			primary key
		constraint fk_dataverse_id
			references dvobject,
	affiliation varchar(255),
	alias varchar(255) not null
		constraint dataverse_alias_key
			unique,
	allowmessagesbanners boolean,
	dataversetype varchar(255) not null,
	description text,
	facetroot boolean,
	guestbookroot boolean,
	metadatablockroot boolean,
	name varchar(255) not null,
	permissionroot boolean,
	templateroot boolean,
	themeroot boolean,
	additional_description text,
	featuredDataversesSorting TEXT,
	defaultcontributorrole_id bigint
		constraint fk_dataverse_defaultcontributorrole_id
			references dataverserole,
	defaulttemplate_id bigint
		constraint fk_dataverse_defaulttemplate_id
			references template
);



create index index_dataverse_defaultcontributorrole_id
	on dataverse (defaultcontributorrole_id);

create index index_dataverse_defaulttemplate_id
	on dataverse (defaulttemplate_id);

create index index_dataverse_alias
	on dataverse (alias);

create index index_dataverse_affiliation
	on dataverse (affiliation);

create index index_dataverse_dataversetype
	on dataverse (dataversetype);

create index index_dataverse_facetroot
	on dataverse (facetroot);

create index index_dataverse_guestbookroot
	on dataverse (guestbookroot);

create index index_dataverse_metadatablockroot
	on dataverse (metadatablockroot);

create index index_dataverse_templateroot
	on dataverse (templateroot);

create index index_dataverse_permissionroot
	on dataverse (permissionroot);

create index index_dataverse_themeroot
	on dataverse (themeroot);

create unique index dataverse_alias_unique_idx
	on dataverse (lower(alias::text));

create index index_dataverserole_owner_id
	on dataverserole (owner_id);

create index index_dataverserole_name
	on dataverserole (name);

create index index_dataverserole_alias
	on dataverserole (alias);

create table datasetfieldtype
(
	id serial not null
		constraint datasetfieldtype_pkey
			primary key,
	advancedsearchfieldtype boolean,
	allowcontrolledvocabulary boolean,
	allowmultiples boolean,
	description text,
	displayformat varchar(255),
	displayoncreate boolean,
	displayorder integer,
	facetable boolean,
	fieldtype varchar(255) not null,
	name text,
	required boolean,
	title text,
	uri text,
	validationformat varchar(255),
	watermark varchar(255),
	metadatablock_id bigint
		constraint fk_datasetfieldtype_metadatablock_id
			references metadatablock,
	parentdatasetfieldtype_id bigint
		constraint fk_datasetfieldtype_parentdatasetfieldtype_id
			references datasetfieldtype
);



create table dataversefacet
(
	id serial not null
		constraint dataversefacet_pkey
			primary key,
	displayorder integer,
	datasetfieldtype_id bigint
		constraint fk_dataversefacet_datasetfieldtype_id
			references datasetfieldtype,
	dataverse_id bigint
		constraint fk_dataversefacet_dataverse_id
			references dvobject
);



create index index_dataversefacet_dataverse_id
	on dataversefacet (dataverse_id);

create index index_dataversefacet_datasetfieldtype_id
	on dataversefacet (datasetfieldtype_id);

create index index_dataversefacet_displayorder
	on dataversefacet (displayorder);

create table datasetfield
(
	id serial not null
		constraint datasetfield_pkey
			primary key,
	source VARCHAR(32) DEFAULT 'PRIMARY' NOT NULL,
	datasetfieldtype_id bigint not null
		constraint fk_datasetfield_datasetfieldtype_id
			references datasetfieldtype,
	datasetversion_id bigint
		constraint fk_datasetfield_datasetversion_id
			references datasetversion,
	parentdatasetfieldcompoundvalue_id bigint,
	template_id bigint
		constraint fk_datasetfield_template_id
			references template
);



create table datasetfieldvalue
(
	id serial not null
		constraint datasetfieldvalue_pkey
			primary key,
	displayorder integer,
	value text,
	datasetfield_id bigint not null
		constraint fk_datasetfieldvalue_datasetfield_id
			references datasetfield
);



create index index_datasetfieldvalue_datasetfield_id
	on datasetfieldvalue (datasetfield_id);

create index index_datasetfield_datasetfieldtype_id
	on datasetfield (datasetfieldtype_id);

create index index_datasetfield_datasetversion_id
	on datasetfield (datasetversion_id);

create index index_datasetfield_parentdatasetfieldcompoundvalue_id
	on datasetfield (parentdatasetfieldcompoundvalue_id);

create index index_datasetfield_template_id
	on datasetfield (template_id);

create table datasetfielddefaultvalue
(
	id serial not null
		constraint datasetfielddefaultvalue_pkey
			primary key,
	displayorder integer,
	strvalue text,
	datasetfield_id bigint not null
		constraint fk_datasetfielddefaultvalue_datasetfield_id
			references datasetfieldtype,
	defaultvalueset_id bigint not null
		constraint fk_datasetfielddefaultvalue_defaultvalueset_id
			references defaultvalueset,
	parentdatasetfielddefaultvalue_id bigint
		constraint fk_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_id
			references datasetfielddefaultvalue
);



create index index_datasetfielddefaultvalue_datasetfield_id
	on datasetfielddefaultvalue (datasetfield_id);

create index index_datasetfielddefaultvalue_defaultvalueset_id
	on datasetfielddefaultvalue (defaultvalueset_id);

create index index_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_i
	on datasetfielddefaultvalue (parentdatasetfielddefaultvalue_id);

create index index_datasetfielddefaultvalue_displayorder
	on datasetfielddefaultvalue (displayorder);

create table controlledvocabularyvalue
(
	id serial not null
		constraint controlledvocabularyvalue_pkey
			primary key,
	displayorder integer,
	identifier varchar(255),
	strvalue text,
	datasetfieldtype_id bigint
		constraint fk_controlledvocabularyvalue_datasetfieldtype_id
			references datasetfieldtype
);



create index index_controlledvocabularyvalue_datasetfieldtype_id
	on controlledvocabularyvalue (datasetfieldtype_id);

create index index_controlledvocabularyvalue_displayorder
	on controlledvocabularyvalue (displayorder);

create table dataset
(
	id bigint not null
		constraint dataset_pkey
			primary key
		constraint fk_dataset_id
			references dvobject,
	fileaccessrequest boolean,
	harvestidentifier varchar(255),
	usegenericthumbnail boolean,
	lastchangeforexportertime timestamp,
	embargodate timestamp,
	citationdatedatasetfieldtype_id bigint
		constraint fk_dataset_citationdatedatasetfieldtype_id
			references datasetfieldtype,
	harvestingclient_id bigint
		constraint fk_dataset_harvestingclient_id
			references harvestingclient,
	guestbook_id bigint
		constraint fk_dataset_guestbook_id
			references guestbook,
	thumbnailfile_id bigint
		constraint fk_dataset_thumbnailfile_id
			references dvobject
);

CREATE TABLE downloaddatasetlog (
    id bigint PRIMARY KEY,
    dataset_id bigint REFERENCES dataset NOT NULL,
    downloaddate timestamp NOT NULL
);

CREATE INDEX downloaddatasetlog_dataset_id_idx ON downloaddatasetlog (dataset_id);
CREATE SEQUENCE downloaddatasetlog_id_seq INCREMENT BY 50 MINVALUE 50;


create index index_dataset_guestbook_id
	on dataset (guestbook_id);

create index index_dataset_thumbnailfile_id
	on dataset (thumbnailfile_id);

create table controlledvocabalternate
(
	id serial not null
		constraint controlledvocabalternate_pkey
			primary key,
	strvalue text,
	controlledvocabularyvalue_id bigint not null
		constraint fk_controlledvocabalternate_controlledvocabularyvalue_id
			references controlledvocabularyvalue,
	datasetfieldtype_id bigint not null
		constraint fk_controlledvocabalternate_datasetfieldtype_id
			references datasetfieldtype
);



create index index_controlledvocabalternate_controlledvocabularyvalue_id
	on controlledvocabalternate (controlledvocabularyvalue_id);

create index index_controlledvocabalternate_datasetfieldtype_id
	on controlledvocabalternate (datasetfieldtype_id);

create table dataversefieldtypeinputlevel
(
	id serial not null
		constraint dataversefieldtypeinputlevel_pkey
			primary key,
	include boolean,
	required boolean,
	datasetfieldtype_id bigint
		constraint fk_dataversefieldtypeinputlevel_datasetfieldtype_id
			references datasetfieldtype,
	dataverse_id bigint
		constraint fk_dataversefieldtypeinputlevel_dataverse_id
			references dvobject,
	constraint unq_dataversefieldtypeinputlevel_0
		unique (dataverse_id, datasetfieldtype_id)
);



create index index_dataversefieldtypeinputlevel_dataverse_id
	on dataversefieldtypeinputlevel (dataverse_id);

create index index_dataversefieldtypeinputlevel_datasetfieldtype_id
	on dataversefieldtypeinputlevel (datasetfieldtype_id);

create index index_dataversefieldtypeinputlevel_required
	on dataversefieldtypeinputlevel (required);

create table datasetfieldcompoundvalue
(
	id serial not null
		constraint datasetfieldcompoundvalue_pkey
			primary key,
	displayorder integer,
	parentdatasetfield_id bigint
		constraint fk_datasetfieldcompoundvalue_parentdatasetfield_id
			references datasetfield
);



alter table datasetfield
	add constraint fk_datasetfield_parentdatasetfieldcompoundvalue_id
		foreign key (parentdatasetfieldcompoundvalue_id) references datasetfieldcompoundvalue;

create index index_datasetfieldcompoundvalue_parentdatasetfield_id
	on datasetfieldcompoundvalue (parentdatasetfield_id);

create index index_datasetfieldtype_metadatablock_id
	on datasetfieldtype (metadatablock_id);

create index index_datasetfieldtype_parentdatasetfieldtype_id
	on datasetfieldtype (parentdatasetfieldtype_id);

create table filemetadata_datafilecategory
(
	filecategories_id bigint not null
		constraint fk_filemetadata_datafilecategory_filecategories_id
			references datafilecategory,
	filemetadatas_id bigint not null
		constraint fk_filemetadata_datafilecategory_filemetadatas_id
			references filemetadata,
	constraint filemetadata_datafilecategory_pkey
		primary key (filecategories_id, filemetadatas_id)
);



create index index_filemetadata_datafilecategory_filecategories_id
	on filemetadata_datafilecategory (filecategories_id);

create index index_filemetadata_datafilecategory_filemetadatas_id
	on filemetadata_datafilecategory (filemetadatas_id);

create table dataverse_citationdatasetfieldtypes
(
	dataverse_id bigint not null
		constraint fk_dataverse_citationdatasetfieldtypes_dataverse_id
			references dvobject,
	citationdatasetfieldtype_id bigint not null
		constraint dataverse_citationdatasetfieldtypes_citationdatasetfieldtype_id
			references datasetfieldtype,
	constraint dataverse_citationdatasetfieldtypes_pkey
		primary key (dataverse_id, citationdatasetfieldtype_id)
);



create table dataversesubjects
(
	dataverse_id bigint not null
		constraint fk_dataversesubjects_dataverse_id
			references dvobject,
	controlledvocabularyvalue_id bigint not null
		constraint fk_dataversesubjects_controlledvocabularyvalue_id
			references controlledvocabularyvalue,
	constraint dataversesubjects_pkey
		primary key (dataverse_id, controlledvocabularyvalue_id)
);



create table dataverse_metadatablock
(
	dataverse_id bigint not null
		constraint fk_dataverse_metadatablock_dataverse_id
			references dvobject,
	metadatablocks_id bigint not null
		constraint fk_dataverse_metadatablock_metadatablocks_id
			references metadatablock,
	constraint dataverse_metadatablock_pkey
		primary key (dataverse_id, metadatablocks_id)
);



create table datasetfield_controlledvocabularyvalue
(
	datasetfield_id bigint not null
		constraint fk_datasetfield_controlledvocabularyvalue_datasetfield_id
			references datasetfield,
	controlledvocabularyvalues_id bigint not null
		constraint dtasetfieldcontrolledvocabularyvaluecntrolledvocabularyvaluesid
			references controlledvocabularyvalue,
	constraint datasetfield_controlledvocabularyvalue_pkey
		primary key (datasetfield_id, controlledvocabularyvalues_id)
);



create index index_datasetfield_controlledvocabularyvalue_datasetfield_id
	on datasetfield_controlledvocabularyvalue (datasetfield_id);

create index index_datasetfield_controlledvocabularyvalue_controlledvocabula
	on datasetfield_controlledvocabularyvalue (controlledvocabularyvalues_id);

create table workflowstepdata_stepparameters
(
	workflowstepdata_id bigint
		constraint fk_workflowstepdata_stepparameters_workflowstepdata_id
			references workflowstepdata,
	stepparameters varchar(2048),
	stepparameters_key varchar(255)
);



create table workflowstepdata_stepsettings
(
	workflowstepdata_id bigint
		constraint fk_workflowstepdata_stepsettings_workflowstepdata_id
			references workflowstepdata,
	stepsettings varchar(2048),
	stepsettings_key varchar(255)
);



create table explicitgroup_containedroleassignees
(
	explicitgroup_id bigint
		constraint fk_explicitgroup_containedroleassignees_explicitgroup_id
			references explicitgroup,
	containedroleassignees varchar(255)
);



create table explicitgroup_authenticateduser
(
	explicitgroup_id bigint not null
		constraint fk_explicitgroup_authenticateduser_explicitgroup_id
			references explicitgroup,
	containedauthenticatedusers_id bigint not null
		constraint explicitgroup_authenticateduser_containedauthenticatedusers_id
			references authenticateduser,
	constraint explicitgroup_authenticateduser_pkey
		primary key (explicitgroup_id, containedauthenticatedusers_id)
);



create table explicitgroup_explicitgroup
(
	explicitgroup_id bigint not null
		constraint fk_explicitgroup_explicitgroup_explicitgroup_id
			references explicitgroup,
	containedexplicitgroups_id bigint not null
		constraint fk_explicitgroup_explicitgroup_containedexplicitgroups_id
			references explicitgroup,
	constraint explicitgroup_explicitgroup_pkey
		primary key (explicitgroup_id, containedexplicitgroups_id)
);


create table fileaccessrequests
(
	datafile_id bigint not null
		constraint fk_fileaccessrequests_datafile_id
			references dvobject,
	authenticated_user_id bigint not null
		constraint fk_fileaccessrequests_authenticated_user_id
			references authenticateduser,
	constraint fileaccessrequests_pkey
		primary key (datafile_id, authenticated_user_id)
);


create table sequence
(
	seq_name varchar(50) not null
		constraint sequence_pkey
			primary key,
	seq_count numeric(38)
);

create table dataversebanner
(
    id serial not null
        constraint dataversebanner_pkey
            primary key,
    active boolean,
    fromtime timestamp not null,
    totime timestamp not null,
    dataverse_id bigint
        constraint fk_dataversebanner_dataverse_id
            references dvobject
);

create table dataverselocalizedbanner
(
    id serial not null
        constraint dataverselocalizedbanner_pkey
            primary key,
    image bytea not null,
    imagelink varchar(255),
    contenttype varchar(255),
    imagename varchar(255),
    locale varchar(255) not null,
    dataversebanner_id bigint
        constraint fk_dataverselocalizedbanner_dataversebanner_id
            references dataversebanner
);



INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 0);

-- using http://dublincore.org/schemas/xmls/qdc/dcterms.xsd because at http://dublincore.org/schemas/xmls/ it's the schema location for http://purl.org/dc/terms/ which is referenced in http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html
INSERT INTO foreignmetadataformatmapping(id, name, startelement, displayName, schemalocation) VALUES (1, 'http://purl.org/dc/terms/', 'entry', 'dcterms: DCMI Metadata Terms', 'http://dublincore.org/schemas/xmls/qdc/dcterms.xsd');
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (1, ':title', 'title', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (2, ':identifier', 'otherIdValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (3, ':creator', 'authorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (4, ':date', 'productionDate', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (5, ':subject', 'keywordValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (6, ':description', 'dsDescriptionValue', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (7, ':relation', 'relatedMaterial', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (8, ':isReferencedBy', 'publicationCitation', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (9, 'holdingsURI', 'publicationURL', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (10, 'agency', 'publicationIDType', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (11, 'IDNo', 'publicationIDNumber', TRUE, 8, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (12, ':coverage', 'otherGeographicCoverage', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (13, ':type', 'kindOfData', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (14, ':source', 'dataSources', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (15, 'affiliation', 'authorAffiliation', TRUE, 3, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (16, ':contributor', 'contributorName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (17, 'type', 'contributorType', TRUE, 16, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (18, ':publisher', 'producerName', FALSE, NULL, 1 );
INSERT INTO foreignmetadatafieldmapping (id, foreignfieldxpath, datasetfieldname, isattribute, parentfieldmapping_id, foreignmetadataformatmapping_id) VALUES (19, ':language', 'language', FALSE, NULL, 1 );

SELECT setval('foreignmetadataformatmapping_id_seq', COALESCE((SELECT MAX(id)+1 FROM foreignmetadataformatmapping), 1), false);
SELECT setval('foreignmetadatafieldmapping_id_seq', COALESCE((SELECT MAX(id)+1 FROM foreignmetadatafieldmapping), 1), false);


INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (1, 0, NULL, 'N/A', NULL);

INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (1, 'Citation Metadata', 'citation', 'https://dataverse.org/schema/citation/', NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (1, true, false, false, 'Full title by which the Dataset is known.', '', true, 0, false, 'TEXT', 'title', true, 'Title', 'http://purl.org/dc/terms/title', NULL, 'Enter title...', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (2, false, false, false, 'A secondary title used to amplify or state certain limitations on the main title.', '', false, 1, false, 'TEXT', 'subtitle', false, 'Subtitle', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (3, false, false, false, 'A title by which the work is commonly referred, or an abbreviation of the title.', '', false, 2, false, 'TEXT', 'alternativeTitle', false, 'Alternative Title', 'http://purl.org/dc/terms/alternative', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (4, false, false, false, 'A URL where the dataset can be viewed, such as a personal or project website.  ', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 3, false, 'URL', 'alternativeURL', false, 'Alternative URL', 'https://schema.org/distribution', NULL, 'Enter full URL, starting with http://', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (5, false, false, true, 'Another unique identifier that identifies this Dataset (e.g., producer''s or another repository''s number).', ':', false, 4, false, 'NONE', 'otherId', false, 'Other ID', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (6, false, false, false, 'Name of agency which generated this identifier.', '#VALUE', false, 5, false, 'TEXT', 'otherIdAgency', false, 'Agency', NULL, NULL, '', 1, 5);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (7, false, false, false, 'Other identifier that corresponds to this Dataset.', '#VALUE', false, 6, false, 'TEXT', 'otherIdValue', false, 'Identifier', NULL, NULL, '', 1, 5);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (8, false, false, true, 'The person(s), corporate body(ies), or agency(ies) responsible for creating the work.', '', true, 7, false, 'NONE', 'author', false, 'Author', 'http://purl.org/dc/terms/creator', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (9, true, false, false, 'The author''s Family Name, Given Name or the name of the organization responsible for this Dataset.', '#VALUE', true, 8, true, 'TEXT', 'authorName', true, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (10, true, false, false, 'The organization with which the author is affiliated.', '(#VALUE)', true, 9, true, 'TEXT', 'authorAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (11, false, true, false, 'Name of the identifier scheme (ORCID, ISNI).', '- #VALUE:', true, 10, false, 'TEXT', 'authorIdentifierScheme', false, 'Identifier Scheme', 'http://purl.org/spar/datacite/AgentIdentifierScheme', NULL, '', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (12, false, false, false, 'Uniquely identifies an individual author or organization, according to various schemes.', '#VALUE', true, 11, false, 'TEXT', 'authorIdentifier', false, 'Identifier', 'http://purl.org/spar/datacite/AgentIdentifier', NULL, '', 1, 8);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (13, false, false, true, 'The contact(s) for this Dataset.', '', true, 12, false, 'NONE', 'datasetContact', false, 'Contact', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (14, false, false, false, 'The contact''s Family Name, Given Name or the name of the organization.', '#VALUE', true, 13, false, 'TEXT', 'datasetContactName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 13);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (15, false, false, false, 'The organization with which the contact is affiliated.', '(#VALUE)', true, 14, false, 'TEXT', 'datasetContactAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 13);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (16, false, false, false, 'The e-mail address(es) of the contact(s) for the Dataset. This will not be displayed.', '#EMAIL', true, 15, false, 'EMAIL', 'datasetContactEmail', true, 'E-mail', NULL, NULL, '', 1, 13);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (17, false, false, true, 'A summary describing the purpose, nature, and scope of the Dataset.', '', true, 16, false, 'NONE', 'dsDescription', false, 'Description', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (18, true, false, false, 'A summary describing the purpose, nature, and scope of the Dataset.', '#VALUE', true, 17, false, 'TEXTBOX', 'dsDescriptionValue', true, 'Text', NULL, NULL, '', 1, 17);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (19, false, false, false, 'In cases where a Dataset contains more than one description (for example, one might be supplied by the data producer and another prepared by the data repository where the data are deposited), the date attribute is used to distinguish between the two descriptions. The date attribute follows the ISO convention of YYYY-MM-DD.', '(#VALUE)', true, 18, false, 'DATE', 'dsDescriptionDate', false, 'Date', NULL, NULL, 'YYYY-MM-DD', 1, 17);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (20, true, true, true, 'Domain-specific Subject Categories that are topically relevant to the Dataset.', '', true, 19, true, 'TEXT', 'subject', true, 'Subject', 'http://purl.org/dc/terms/subject', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (21, false, false, true, 'Key terms that describe important aspects of the Dataset.', '', true, 20, false, 'NONE', 'keyword', false, 'Keyword', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (22, true, false, false, 'Key terms that describe important aspects of the Dataset. Can be used for building keyword indexes and for classification and retrieval purposes. A controlled vocabulary can be employed. The vocab attribute is provided for specification of the controlled vocabulary in use, such as LCSH, MeSH, or others. The vocabURI attribute specifies the location for the full controlled vocabulary.', '#VALUE', true, 21, true, 'TEXT', 'keywordValue', false, 'Term', NULL, NULL, '', 1, 21);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (23, false, false, false, 'For the specification of the keyword controlled vocabulary in use, such as LCSH, MeSH, or others.', '(#VALUE)', true, 22, false, 'TEXT', 'keywordVocabulary', false, 'Vocabulary', NULL, NULL, '', 1, 21);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (24, false, false, false, 'Keyword vocabulary URL points to the web presence that describes the keyword vocabulary, if appropriate. Enter an absolute URL where the keyword vocabulary web site is found, such as http://www.my.org.', '<a href="#VALUE" target="_blank">#VALUE</a>', true, 23, false, 'URL', 'keywordVocabularyURI', false, 'Vocabulary URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 21);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (25, false, false, true, 'The classification field indicates the broad important topic(s) and subjects that the data cover. Library of Congress subject terms may be used here.  ', '', false, 24, false, 'NONE', 'topicClassification', false, 'Topic Classification', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (26, true, false, false, 'Topic or Subject term that is relevant to this Dataset.', '#VALUE', false, 25, true, 'TEXT', 'topicClassValue', false, 'Term', NULL, NULL, '', 1, 25);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (27, false, false, false, 'Provided for specification of the controlled vocabulary in use, e.g., LCSH, MeSH, etc.', '(#VALUE)', false, 26, false, 'TEXT', 'topicClassVocab', false, 'Vocabulary', NULL, NULL, '', 1, 25);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (28, false, false, false, 'Specifies the URL location for the full controlled vocabulary.', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 27, false, 'URL', 'topicClassVocabURI', false, 'Vocabulary URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 25);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (29, false, false, true, 'Publications that use the data from this Dataset.', '', true, 28, false, 'NONE', 'publication', false, 'Related Publication', 'http://purl.org/dc/terms/isReferencedBy', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (30, true, false, false, 'The full bibliographic citation for this related publication.', '#VALUE', true, 29, false, 'TEXTBOX', 'publicationCitation', false, 'Citation', 'http://purl.org/dc/terms/bibliographicCitation', NULL, '', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (31, true, true, false, 'The type of digital identifier used for this publication (e.g., Digital Object Identifier (DOI)).', '#VALUE: ', true, 30, false, 'TEXT', 'publicationIDType', false, 'ID Type', 'http://purl.org/spar/datacite/ResourceIdentifierScheme', NULL, '', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (32, true, false, false, 'The identifier for the selected ID type.', '#VALUE', true, 31, false, 'TEXT', 'publicationIDNumber', false, 'ID Number', NULL, NULL, '', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (33, false, false, false, 'Link to the publication web page (e.g., journal article page, archive record page, or other).', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 32, false, 'URL', 'publicationURL', false, 'URL', 'https://schema.org/distribution', NULL, 'Enter full URL, starting with http://', 1, 29);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (34, false, false, false, 'Additional important information about the Dataset.', '', true, 33, false, 'TEXTBOX', 'notesText', false, 'Notes', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (35, true, true, true, 'Language of the Dataset', '', false, 34, true, 'TEXT', 'language', false, 'Language', 'http://purl.org/dc/terms/language', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (36, false, false, true, 'Person or organization with the financial or administrative responsibility over this Dataset', '', false, 35, false, 'NONE', 'producer', false, 'Producer', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (37, true, false, false, 'Producer name', '#VALUE', false, 36, true, 'TEXT', 'producerName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (38, false, false, false, 'The organization with which the producer is affiliated.', '(#VALUE)', false, 37, false, 'TEXT', 'producerAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (39, false, false, false, 'The abbreviation by which the producer is commonly known. (ex. IQSS, ICPSR)', '(#VALUE)', false, 38, false, 'TEXT', 'producerAbbreviation', false, 'Abbreviation', NULL, NULL, '', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (40, false, false, false, 'Producer URL points to the producer''s web presence, if appropriate. Enter an absolute URL where the producer''s web site is found, such as http://www.my.org.  ', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 39, false, 'URL', 'producerURL', false, 'URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (41, false, false, false, 'URL for the producer''s logo, which points to this  producer''s web-accessible logo image. Enter an absolute URL where the producer''s logo image is found, such as http://www.my.org/images/logo.gif.', '<img src="#VALUE" alt="#NAME" class="metadata-logo"/><br/>', false, 40, false, 'URL', 'producerLogoURL', false, 'Logo URL', NULL, NULL, 'Enter full URL for image, starting with http://', 1, 36);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (42, true, false, false, 'Date when the data collection or other materials were produced (not distributed, published or archived).', '', false, 41, true, 'DATE', 'productionDate', false, 'Production Date', NULL, NULL, 'YYYY-MM-DD', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (43, false, false, false, 'The location where the data collection and any other related materials were produced.', '', false, 42, false, 'TEXT', 'productionPlace', false, 'Production Place', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (44, false, false, true, 'The organization or person responsible for either collecting, managing, or otherwise contributing in some form to the development of the resource.', ':', false, 43, false, 'NONE', 'contributor', false, 'Contributor', 'http://purl.org/dc/terms/contributor', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (45, true, true, false, 'The type of contributor of the  resource.  ', '#VALUE ', false, 44, true, 'TEXT', 'contributorType', false, 'Type', NULL, NULL, '', 1, 44);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (46, true, false, false, 'The Family Name, Given Name or organization name of the contributor.', '#VALUE', false, 45, true, 'TEXT', 'contributorName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 44);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (47, false, false, true, 'Grant Information', ':', false, 46, false, 'NONE', 'grantNumber', false, 'Grant Information', 'https://schema.org/sponsor', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (48, true, false, false, 'Grant Number Agency', '#VALUE', false, 47, true, 'TEXT', 'grantNumberAgency', false, 'Grant Agency', NULL, NULL, '', 1, 47);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (49, true, false, false, 'The grant or contract number of the project that  sponsored the effort.', '#VALUE', false, 48, true, 'TEXT', 'grantNumberValue', false, 'Grant Number', NULL, NULL, '', 1, 47);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (50, false, false, true, 'The organization designated by the author or producer to generate copies of the particular work including any necessary editions or revisions.', '', false, 49, false, 'NONE', 'distributor', false, 'Distributor', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (51, true, false, false, 'Distributor name', '#VALUE', false, 50, true, 'TEXT', 'distributorName', false, 'Name', NULL, NULL, 'FamilyName, GivenName or Organization', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (52, false, false, false, 'The organization with which the distributor contact is affiliated.', '(#VALUE)', false, 51, false, 'TEXT', 'distributorAffiliation', false, 'Affiliation', NULL, NULL, '', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (53, false, false, false, 'The abbreviation by which this distributor is commonly known (e.g., IQSS, ICPSR).', '(#VALUE)', false, 52, false, 'TEXT', 'distributorAbbreviation', false, 'Abbreviation', NULL, NULL, '', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (54, false, false, false, 'Distributor URL points to the distributor''s web presence, if appropriate. Enter an absolute URL where the distributor''s web site is found, such as http://www.my.org.', '<a href="#VALUE" target="_blank">#VALUE</a>', false, 53, false, 'URL', 'distributorURL', false, 'URL', NULL, NULL, 'Enter full URL, starting with http://', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (55, false, false, false, 'URL of the distributor''s logo, which points to this  distributor''s web-accessible logo image. Enter an absolute URL where the distributor''s logo image is found, such as http://www.my.org/images/logo.gif.', '<img src="#VALUE" alt="#NAME" class="metadata-logo"/><br/>', false, 54, false, 'URL', 'distributorLogoURL', false, 'Logo URL', NULL, NULL, 'Enter full URL for image, starting with http://', 1, 50);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (56, true, false, false, 'Date that the work was made available for distribution/presentation.', '', false, 55, true, 'DATE', 'distributionDate', false, 'Distribution Date', NULL, NULL, 'YYYY-MM-DD', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (57, false, false, false, 'The person (Family Name, Given Name) or the name of the organization that deposited this Dataset to the repository.', '', false, 56, false, 'TEXT', 'depositor', false, 'Depositor', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (58, false, false, false, 'Date that the Dataset was deposited into the repository.', '', false, 57, true, 'DATE', 'dateOfDeposit', false, 'Deposit Date', 'http://purl.org/dc/terms/dateSubmitted', NULL, 'YYYY-MM-DD', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (59, false, false, true, 'Time period to which the data refer. This item reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected. Also known as span.', ';', false, 58, false, 'NONE', 'timePeriodCovered', false, 'Time Period Covered', 'https://schema.org/temporalCoverage', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (60, true, false, false, 'Start date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected.', '#NAME: #VALUE ', false, 59, true, 'DATE', 'timePeriodCoveredStart', false, 'Start', NULL, NULL, 'YYYY-MM-DD', 1, 59);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (61, true, false, false, 'End date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected.', '#NAME: #VALUE ', false, 60, true, 'DATE', 'timePeriodCoveredEnd', false, 'End', NULL, NULL, 'YYYY-MM-DD', 1, 59);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (62, false, false, true, 'Contains the date(s) when the data were collected.', ';', false, 61, false, 'NONE', 'dateOfCollection', false, 'Date of Collection', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (63, false, false, false, 'Date when the data collection started.', '#NAME: #VALUE ', false, 62, false, 'DATE', 'dateOfCollectionStart', false, 'Start', NULL, NULL, 'YYYY-MM-DD', 1, 62);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (64, false, false, false, 'Date when the data collection ended.', '#NAME: #VALUE ', false, 63, false, 'DATE', 'dateOfCollectionEnd', false, 'End', NULL, NULL, 'YYYY-MM-DD', 1, 62);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (65, true, false, true, 'Type of data included in the file: survey data, census/enumeration data, aggregate data, clinical data, event/transaction data, program source code, machine-readable text, administrative records data, experimental data, psychological test, textual data, coded textual, coded documents, time budget diaries, observation data/ratings, process-produced data, or other.', '', false, 64, true, 'TEXT', 'kindOfData', false, 'Kind of Data', 'http://rdf-vocabulary.ddialliance.org/discovery#kindOfData', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (66, false, false, false, 'Information about the Dataset series.', ':', false, 65, false, 'NONE', 'series', false, 'Series', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (67, true, false, false, 'Name of the dataset series to which the Dataset belongs.', '#VALUE', false, 66, true, 'TEXT', 'seriesName', false, 'Name', NULL, NULL, '', 1, 66);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (68, false, false, false, 'History of the series and summary of those features that apply to the series as a whole.', '#VALUE', false, 67, false, 'TEXTBOX', 'seriesInformation', false, 'Information', NULL, NULL, '', 1, 66);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (69, false, false, true, 'Information about the software used to generate the Dataset.', ',', false, 68, false, 'NONE', 'software', false, 'Software', 'https://www.w3.org/TR/prov-o/#wasGeneratedBy', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (70, false, true, false, 'Name of software used to generate the Dataset.', '#VALUE', false, 69, false, 'TEXT', 'softwareName', false, 'Name', NULL, NULL, '', 1, 69);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (71, false, false, false, 'Version of the software used to generate the Dataset.', '#NAME: #VALUE', false, 70, false, 'TEXT', 'softwareVersion', false, 'Version', NULL, NULL, '', 1, 69);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (72, false, false, true, 'Any material related to this Dataset.', '', false, 71, false, 'TEXTBOX', 'relatedMaterial', false, 'Related Material', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (73, false, false, true, 'Any Datasets that are related to this Dataset, such as previous research on this subject.', '', false, 72, false, 'TEXTBOX', 'relatedDatasets', false, 'Related Datasets', 'http://purl.org/dc/terms/relation', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (74, false, false, true, 'Any references that would serve as background or supporting material to this Dataset.', '', false, 73, false, 'TEXT', 'otherReferences', false, 'Other References', 'http://purl.org/dc/terms/references', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (75, false, false, true, 'List of books, articles, serials, or machine-readable data files that served as the sources of the data collection.', '', false, 74, false, 'TEXTBOX', 'dataSources', false, 'Data Sources', 'https://www.w3.org/TR/prov-o/#wasDerivedFrom', NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (76, false, false, false, 'For historical materials, information about the origin of the sources and the rules followed in establishing the sources should be specified.', '', false, 75, false, 'TEXTBOX', 'originOfSources', false, 'Origin of Sources', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (77, false, false, false, 'Assessment of characteristics and source material.', '', false, 76, false, 'TEXTBOX', 'characteristicOfSources', false, 'Characteristic of Sources Noted', NULL, NULL, '', 1, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (78, false, false, false, 'Level of documentation of the original sources.', '', false, 77, false, 'TEXTBOX', 'accessToSources', false, 'Documentation and Access to Sources', NULL, NULL, '', 1, NULL);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (2, 0, 'D01', 'Agricultural Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (3, 1, 'D0', 'Arts and Humanities', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (4, 2, 'D1', 'Astronomy and Astrophysics', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (5, 3, 'D2', 'Business and Management', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (6, 4, 'D3', 'Chemistry', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (7, 5, 'D7', 'Computer and Information Science', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (8, 6, 'D4', 'Earth and Environmental Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (9, 7, 'D5', 'Engineering', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (10, 8, 'D8', 'Law', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (11, 9, 'D9', 'Mathematical Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (12, 10, 'D6', 'Medicine, Health and Life Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (13, 11, 'D10', 'Physics', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (14, 12, 'D11', 'Social Sciences', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (15, 13, 'D12', 'Other', 20);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (16, 0, '', 'ark', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (17, 1, '', 'arXiv', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (18, 2, '', 'bibcode', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (19, 3, '', 'doi', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (20, 4, '', 'ean13', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (21, 5, '', 'eissn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (22, 6, '', 'handle', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (23, 7, '', 'isbn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (24, 8, '', 'issn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (25, 9, '', 'istc', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (26, 10, '', 'lissn', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (27, 11, '', 'lsid', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (28, 12, '', 'pmid', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (29, 13, '', 'purl', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (30, 14, '', 'upc', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (31, 15, '', 'url', 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (32, 16, '', 'urn', 31);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (1, 'arxiv', 17, 31);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (33, 0, '', 'Data Collector', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (34, 1, '', 'Data Curator', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (35, 2, '', 'Data Manager', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (36, 3, '', 'Editor', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (37, 4, '', 'Funder', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (38, 5, '', 'Hosting Institution', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (39, 6, '', 'Project Leader', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (40, 7, '', 'Project Manager', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (41, 8, '', 'Project Member', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (42, 9, '', 'Related Person', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (43, 10, '', 'Researcher', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (44, 11, '', 'Research Group', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (45, 12, '', 'Rights Holder', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (46, 13, '', 'Sponsor', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (47, 14, '', 'Supervisor', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (48, 15, '', 'Work Package Leader', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (49, 16, '', 'Other', 45);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (50, 0, '', 'ORCID', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (51, 1, '', 'ISNI', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (52, 2, '', 'LCNA', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (53, 3, '', 'VIAF', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (54, 4, '', 'GND', 11);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (55, 0, '', 'Abkhaz', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (56, 1, '', 'Afar', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (57, 2, '', 'Afrikaans', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (58, 3, '', 'Akan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (59, 4, '', 'Albanian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (60, 5, '', 'Amharic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (61, 6, '', 'Arabic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (62, 7, '', 'Aragonese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (63, 8, '', 'Armenian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (64, 9, '', 'Assamese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (65, 10, '', 'Avaric', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (66, 11, '', 'Avestan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (67, 12, '', 'Aymara', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (68, 13, '', 'Azerbaijani', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (69, 14, '', 'Bambara', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (70, 15, '', 'Bashkir', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (71, 16, '', 'Basque', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (72, 17, '', 'Belarusian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (73, 18, '', 'Bengali, Bangla', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (74, 19, '', 'Bihari', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (75, 20, '', 'Bislama', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (76, 21, '', 'Bosnian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (77, 22, '', 'Breton', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (78, 23, '', 'Bulgarian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (79, 24, '', 'Burmese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (80, 25, '', 'Catalan,Valencian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (81, 26, '', 'Chamorro', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (82, 27, '', 'Chechen', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (83, 28, '', 'Chichewa, Chewa, Nyanja', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (84, 29, '', 'Chinese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (85, 30, '', 'Chuvash', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (86, 31, '', 'Cornish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (87, 32, '', 'Corsican', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (88, 33, '', 'Cree', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (89, 34, '', 'Croatian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (90, 35, '', 'Czech', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (91, 36, '', 'Danish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (92, 37, '', 'Divehi, Dhivehi, Maldivian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (93, 38, '', 'Dutch', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (94, 39, '', 'Dzongkha', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (95, 40, '', 'English', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (96, 41, '', 'Esperanto', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (97, 42, '', 'Estonian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (98, 43, '', 'Ewe', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (99, 44, '', 'Faroese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (100, 45, '', 'Fijian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (101, 46, '', 'Finnish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (102, 47, '', 'French', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (103, 48, '', 'Fula, Fulah, Pulaar, Pular', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (104, 49, '', 'Galician', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (105, 50, '', 'Georgian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (106, 51, '', 'German', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (107, 52, '', 'Greek (modern)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (108, 53, '', 'Guaraní', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (109, 54, '', 'Gujarati', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (110, 55, '', 'Haitian, Haitian Creole', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (111, 56, '', 'Hausa', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (112, 57, '', 'Hebrew (modern)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (113, 58, '', 'Herero', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (114, 59, '', 'Hindi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (115, 60, '', 'Hiri Motu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (116, 61, '', 'Hungarian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (117, 62, '', 'Interlingua', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (118, 63, '', 'Indonesian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (119, 64, '', 'Interlingue', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (120, 65, '', 'Irish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (121, 66, '', 'Igbo', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (122, 67, '', 'Inupiaq', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (123, 68, '', 'Ido', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (124, 69, '', 'Icelandic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (125, 70, '', 'Italian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (126, 71, '', 'Inuktitut', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (127, 72, '', 'Japanese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (128, 73, '', 'Javanese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (129, 74, '', 'Kalaallisut, Greenlandic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (130, 75, '', 'Kannada', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (131, 76, '', 'Kanuri', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (132, 77, '', 'Kashmiri', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (133, 78, '', 'Kazakh', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (134, 79, '', 'Khmer', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (135, 80, '', 'Kikuyu, Gikuyu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (136, 81, '', 'Kinyarwanda', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (137, 82, '', 'Kyrgyz', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (138, 83, '', 'Komi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (139, 84, '', 'Kongo', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (140, 85, '', 'Korean', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (141, 86, '', 'Kurdish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (142, 87, '', 'Kwanyama, Kuanyama', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (143, 88, '', 'Latin', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (144, 89, '', 'Luxembourgish, Letzeburgesch', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (145, 90, '', 'Ganda', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (146, 91, '', 'Limburgish, Limburgan, Limburger', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (147, 92, '', 'Lingala', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (148, 93, '', 'Lao', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (149, 94, '', 'Lithuanian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (150, 95, '', 'Luba-Katanga', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (151, 96, '', 'Latvian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (152, 97, '', 'Manx', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (153, 98, '', 'Macedonian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (154, 99, '', 'Malagasy', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (155, 100, '', 'Malay', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (156, 101, '', 'Malayalam', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (157, 102, '', 'Maltese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (158, 103, '', 'Māori', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (159, 104, '', 'Marathi (Marāṭhī)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (160, 105, '', 'Marshallese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (161, 106, '', 'Mixtepec Mixtec', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (162, 107, '', 'Mongolian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (163, 108, '', 'Nauru', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (164, 109, '', 'Navajo, Navaho', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (165, 110, '', 'Northern Ndebele', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (166, 111, '', 'Nepali', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (167, 112, '', 'Ndonga', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (168, 113, '', 'Norwegian Bokmål', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (169, 114, '', 'Norwegian Nynorsk', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (170, 115, '', 'Norwegian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (171, 116, '', 'Nuosu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (172, 117, '', 'Southern Ndebele', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (173, 118, '', 'Occitan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (174, 119, '', 'Ojibwe, Ojibwa', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (175, 120, '', 'Old Church Slavonic,Church Slavonic,Old Bulgarian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (176, 121, '', 'Oromo', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (177, 122, '', 'Oriya', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (178, 123, '', 'Ossetian, Ossetic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (179, 124, '', 'Panjabi, Punjabi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (180, 125, '', 'Pāli', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (181, 126, '', 'Persian (Farsi)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (182, 127, '', 'Polish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (183, 128, '', 'Pashto, Pushto', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (184, 129, '', 'Portuguese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (185, 130, '', 'Quechua', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (186, 131, '', 'Romansh', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (187, 132, '', 'Kirundi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (188, 133, '', 'Romanian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (189, 134, '', 'Russian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (190, 135, '', 'Sanskrit (Saṁskṛta)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (191, 136, '', 'Sardinian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (192, 137, '', 'Sindhi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (193, 138, '', 'Northern Sami', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (194, 139, '', 'Samoan', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (195, 140, '', 'Sango', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (196, 141, '', 'Serbian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (197, 142, '', 'Scottish Gaelic, Gaelic', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (198, 143, '', 'Shona', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (199, 144, '', 'Sinhala, Sinhalese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (200, 145, '', 'Slovak', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (201, 146, '', 'Slovene', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (202, 147, '', 'Somali', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (203, 148, '', 'Southern Sotho', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (204, 149, '', 'Spanish, Castilian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (205, 150, '', 'Sundanese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (206, 151, '', 'Swahili', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (207, 152, '', 'Swati', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (208, 153, '', 'Swedish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (209, 154, '', 'Tamil', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (210, 155, '', 'Telugu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (211, 156, '', 'Tajik', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (212, 157, '', 'Thai', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (213, 158, '', 'Tigrinya', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (214, 159, '', 'Tibetan Standard, Tibetan, Central', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (215, 160, '', 'Turkmen', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (216, 161, '', 'Tagalog', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (217, 162, '', 'Tswana', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (218, 163, '', 'Tonga (Tonga Islands)', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (219, 164, '', 'Turkish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (220, 165, '', 'Tsonga', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (221, 166, '', 'Tatar', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (222, 167, '', 'Twi', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (223, 168, '', 'Tahitian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (224, 169, '', 'Uyghur, Uighur', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (225, 170, '', 'Ukrainian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (226, 171, '', 'Urdu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (227, 172, '', 'Uzbek', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (228, 173, '', 'Venda', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (229, 174, '', 'Vietnamese', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (230, 175, '', 'Volapük', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (231, 176, '', 'Walloon', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (232, 177, '', 'Welsh', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (233, 178, '', 'Wolof', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (234, 179, '', 'Western Frisian', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (235, 180, '', 'Xhosa', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (236, 181, '', 'Yiddish', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (237, 182, '', 'Yoruba', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (238, 183, '', 'Zhuang, Chuang', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (239, 184, '', 'Zulu', 35);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (240, 185, '', 'Not applicable', 35);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (2, 'Geospatial Metadata', 'geospatial', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (79, false, false, true, 'Information on the geographic coverage of the data. Includes the total geographic scope of the data.', '', false, 0, false, 'NONE', 'geographicCoverage', false, 'Geographic Coverage', NULL, NULL, '', 2, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (80, true, true, false, 'The country or nation that the Dataset is about.', '', false, 1, true, 'TEXT', 'country', false, 'Country / Nation', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (81, true, false, false, 'The state or province that the Dataset is about. Use GeoNames for correct spelling and avoid abbreviations.', '', false, 2, true, 'TEXT', 'state', false, 'State / Province', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (82, true, false, false, 'The name of the city that the Dataset is about. Use GeoNames for correct spelling and avoid abbreviations.', '', false, 3, true, 'TEXT', 'city', false, 'City', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (83, false, false, false, 'Other information on the geographic coverage of the data.', '', false, 4, false, 'TEXT', 'otherGeographicCoverage', false, 'Other', NULL, NULL, '', 2, 79);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (84, true, false, true, 'Lowest level of geographic aggregation covered by the Dataset, e.g., village, county, region.', '', false, 5, true, 'TEXT', 'geographicUnit', false, 'Geographic Unit', NULL, NULL, '', 2, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (85, false, false, true, 'The fundamental geometric description for any Dataset that models geography is the geographic bounding box. It describes the minimum box, defined by west and east longitudes and north and south latitudes, which includes the largest geographic extent of the  Dataset''s geographic coverage. This element is used in the first pass of a coordinate-based search. Inclusion of this element in the codebook is recommended, but is required if the bound polygon box is included. ', '', false, 6, false, 'NONE', 'geographicBoundingBox', false, 'Geographic Bounding Box', NULL, NULL, '', 2, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (86, false, false, false, 'Westernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -180,0 <= West  Bounding Longitude Value <= 180,0.', '', false, 7, false, 'TEXT', 'westLongitude', false, 'West Longitude', NULL, NULL, '', 2, 85);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (87, false, false, false, 'Easternmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -180,0 <= East Bounding Longitude Value <= 180,0.', '', false, 8, false, 'TEXT', 'eastLongitude', false, 'East Longitude', NULL, NULL, '', 2, 85);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (88, false, false, false, 'Northernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -90,0 <= North Bounding Latitude Value <= 90,0.', '', false, 9, false, 'TEXT', 'northLongitude', false, 'North Latitude', NULL, NULL, '', 2, 85);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (89, false, false, false, 'Southernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -90,0 <= South Bounding Latitude Value <= 90,0.', '', false, 10, false, 'TEXT', 'southLongitude', false, 'South Latitude', NULL, NULL, '', 2, 85);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (241, 0, '', 'Afghanistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (242, 1, '', 'Albania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (243, 2, '', 'Algeria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (244, 3, '', 'American Samoa', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (245, 4, '', 'Andorra', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (246, 5, '', 'Angola', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (247, 6, '', 'Anguilla', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (248, 7, '', 'Antarctica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (249, 8, '', 'Antigua and Barbuda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (250, 9, '', 'Argentina', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (251, 10, '', 'Armenia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (252, 11, '', 'Aruba', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (253, 12, '', 'Australia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (254, 13, '', 'Austria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (255, 14, '', 'Azerbaijan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (256, 15, '', 'Bahamas', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (257, 16, '', 'Bahrain', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (258, 17, '', 'Bangladesh', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (259, 18, '', 'Barbados', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (260, 19, '', 'Belarus', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (261, 20, '', 'Belgium', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (262, 21, '', 'Belize', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (263, 22, '', 'Benin', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (264, 23, '', 'Bermuda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (265, 24, '', 'Bhutan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (266, 25, '', 'Bolivia, Plurinational State of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (267, 26, '', 'Bonaire, Sint Eustatius and Saba', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (268, 27, '', 'Bosnia and Herzegovina', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (269, 28, '', 'Botswana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (270, 29, '', 'Bouvet Island', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (271, 30, '', 'Brazil', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (272, 31, '', 'British Indian Ocean Territory', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (273, 32, '', 'Brunei Darussalam', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (274, 33, '', 'Bulgaria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (275, 34, '', 'Burkina Faso', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (276, 35, '', 'Burundi', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (277, 36, '', 'Cambodia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (278, 37, '', 'Cameroon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (279, 38, '', 'Canada', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (280, 39, '', 'Cape Verde', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (281, 40, '', 'Cayman Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (282, 41, '', 'Central African Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (283, 42, '', 'Chad', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (284, 43, '', 'Chile', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (285, 44, '', 'China', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (286, 45, '', 'Christmas Island', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (287, 46, '', 'Cocos (Keeling) Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (288, 47, '', 'Colombia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (289, 48, '', 'Comoros', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (290, 49, '', 'Congo', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (291, 50, '', 'Congo, the Democratic Republic of the', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (292, 51, '', 'Cook Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (293, 52, '', 'Costa Rica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (294, 53, '', 'Croatia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (295, 54, '', 'Cuba', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (296, 55, '', 'Curaçao', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (297, 56, '', 'Cyprus', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (298, 57, '', 'Czech Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (299, 58, '', 'Côte d''Ivoire', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (300, 59, '', 'Denmark', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (301, 60, '', 'Djibouti', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (302, 61, '', 'Dominica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (303, 62, '', 'Dominican Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (304, 63, '', 'Ecuador', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (305, 64, '', 'Egypt', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (306, 65, '', 'El Salvador', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (307, 66, '', 'Equatorial Guinea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (308, 67, '', 'Eritrea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (309, 68, '', 'Estonia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (310, 69, '', 'Ethiopia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (311, 70, '', 'Falkland Islands (Malvinas)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (312, 71, '', 'Faroe Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (313, 72, '', 'Fiji', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (314, 73, '', 'Finland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (315, 74, '', 'France', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (316, 75, '', 'French Guiana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (317, 76, '', 'French Polynesia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (318, 77, '', 'French Southern Territories', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (319, 78, '', 'Gabon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (320, 79, '', 'Gambia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (321, 80, '', 'Georgia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (322, 81, '', 'Germany', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (323, 82, '', 'Ghana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (324, 83, '', 'Gibraltar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (325, 84, '', 'Greece', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (326, 85, '', 'Greenland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (327, 86, '', 'Grenada', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (328, 87, '', 'Guadeloupe', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (329, 88, '', 'Guam', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (330, 89, '', 'Guatemala', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (331, 90, '', 'Guernsey', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (332, 91, '', 'Guinea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (333, 92, '', 'Guinea-Bissau', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (334, 93, '', 'Guyana', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (335, 94, '', 'Haiti', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (336, 95, '', 'Heard Island and Mcdonald Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (337, 96, '', 'Holy See (Vatican City State)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (338, 97, '', 'Honduras', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (339, 98, '', 'Hong Kong', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (340, 99, '', 'Hungary', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (341, 100, '', 'Iceland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (342, 101, '', 'India', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (343, 102, '', 'Indonesia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (344, 103, '', 'Iran, Islamic Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (345, 104, '', 'Iraq', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (346, 105, '', 'Ireland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (347, 106, '', 'Isle of Man', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (348, 107, '', 'Israel', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (349, 108, '', 'Italy', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (350, 109, '', 'Jamaica', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (351, 110, '', 'Japan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (352, 111, '', 'Jersey', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (353, 112, '', 'Jordan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (354, 113, '', 'Kazakhstan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (355, 114, '', 'Kenya', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (356, 115, '', 'Kiribati', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (357, 116, '', 'Korea, Democratic People''s Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (358, 117, '', 'Korea, Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (359, 118, '', 'Kuwait', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (360, 119, '', 'Kyrgyzstan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (361, 120, '', 'Lao People''s Democratic Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (362, 121, '', 'Latvia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (363, 122, '', 'Lebanon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (364, 123, '', 'Lesotho', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (365, 124, '', 'Liberia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (366, 125, '', 'Libya', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (367, 126, '', 'Liechtenstein', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (368, 127, '', 'Lithuania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (369, 128, '', 'Luxembourg', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (370, 129, '', 'Macao', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (371, 130, '', 'Macedonia, the Former Yugoslav Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (372, 131, '', 'Madagascar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (373, 132, '', 'Malawi', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (374, 133, '', 'Malaysia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (375, 134, '', 'Maldives', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (376, 135, '', 'Mali', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (377, 136, '', 'Malta', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (378, 137, '', 'Marshall Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (379, 138, '', 'Martinique', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (380, 139, '', 'Mauritania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (381, 140, '', 'Mauritius', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (382, 141, '', 'Mayotte', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (383, 142, '', 'Mexico', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (384, 143, '', 'Micronesia, Federated States of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (385, 144, '', 'Moldova, Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (386, 145, '', 'Monaco', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (387, 146, '', 'Mongolia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (388, 147, '', 'Montenegro', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (389, 148, '', 'Montserrat', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (390, 149, '', 'Morocco', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (391, 150, '', 'Mozambique', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (392, 151, '', 'Myanmar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (393, 152, '', 'Namibia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (394, 153, '', 'Nauru', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (395, 154, '', 'Nepal', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (396, 155, '', 'Netherlands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (397, 156, '', 'New Caledonia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (398, 157, '', 'New Zealand', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (399, 158, '', 'Nicaragua', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (400, 159, '', 'Niger', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (401, 160, '', 'Nigeria', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (402, 161, '', 'Niue', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (403, 162, '', 'Norfolk Island', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (404, 163, '', 'Northern Mariana Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (405, 164, '', 'Norway', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (406, 165, '', 'Oman', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (407, 166, '', 'Pakistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (408, 167, '', 'Palau', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (409, 168, '', 'Palestine, State of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (410, 169, '', 'Panama', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (411, 170, '', 'Papua New Guinea', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (412, 171, '', 'Paraguay', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (413, 172, '', 'Peru', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (414, 173, '', 'Philippines', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (415, 174, '', 'Pitcairn', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (416, 175, '', 'Poland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (417, 176, '', 'Portugal', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (418, 177, '', 'Puerto Rico', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (419, 178, '', 'Qatar', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (420, 179, '', 'Romania', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (421, 180, '', 'Russian Federation', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (422, 181, '', 'Rwanda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (423, 182, '', 'Réunion', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (424, 183, '', 'Saint Barthélemy', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (425, 184, '', 'Saint Helena, Ascension and Tristan da Cunha', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (426, 185, '', 'Saint Kitts and Nevis', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (427, 186, '', 'Saint Lucia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (428, 187, '', 'Saint Martin (French part)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (429, 188, '', 'Saint Pierre and Miquelon', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (430, 189, '', 'Saint Vincent and the Grenadines', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (431, 190, '', 'Samoa', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (432, 191, '', 'San Marino', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (433, 192, '', 'Sao Tome and Principe', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (434, 193, '', 'Saudi Arabia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (435, 194, '', 'Senegal', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (436, 195, '', 'Serbia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (437, 196, '', 'Seychelles', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (438, 197, '', 'Sierra Leone', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (439, 198, '', 'Singapore', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (440, 199, '', 'Sint Maarten (Dutch part)', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (441, 200, '', 'Slovakia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (442, 201, '', 'Slovenia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (443, 202, '', 'Solomon Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (444, 203, '', 'Somalia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (445, 204, '', 'South Africa', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (446, 205, '', 'South Georgia and the South Sandwich Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (447, 206, '', 'South Sudan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (448, 207, '', 'Spain', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (449, 208, '', 'Sri Lanka', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (450, 209, '', 'Sudan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (451, 210, '', 'Suriname', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (452, 211, '', 'Svalbard and Jan Mayen', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (453, 212, '', 'Swaziland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (454, 213, '', 'Sweden', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (455, 214, '', 'Switzerland', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (456, 215, '', 'Syrian Arab Republic', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (457, 216, '', 'Taiwan, Province of China', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (458, 217, '', 'Tajikistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (459, 218, '', 'Tanzania, United Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (460, 219, '', 'Thailand', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (461, 220, '', 'Timor-Leste', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (462, 221, '', 'Togo', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (463, 222, '', 'Tokelau', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (464, 223, '', 'Tonga', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (465, 224, '', 'Trinidad and Tobago', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (466, 225, '', 'Tunisia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (467, 226, '', 'Turkey', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (468, 227, '', 'Turkmenistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (469, 228, '', 'Turks and Caicos Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (470, 229, '', 'Tuvalu', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (471, 230, '', 'Uganda', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (472, 231, '', 'Ukraine', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (473, 232, '', 'United Arab Emirates', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (474, 233, '', 'United Kingdom', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (475, 234, '', 'United States', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (476, 235, '', 'United States Minor Outlying Islands', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (477, 236, '', 'Uruguay', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (478, 237, '', 'Uzbekistan', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (479, 238, '', 'Vanuatu', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (480, 239, '', 'Venezuela, Bolivarian Republic of', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (481, 240, '', 'Viet Nam', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (482, 241, '', 'Virgin Islands, British', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (483, 242, '', 'Virgin Islands, U.S.', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (484, 243, '', 'Wallis and Futuna', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (485, 244, '', 'Western Sahara', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (486, 245, '', 'Yemen', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (487, 246, '', 'Zambia', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (488, 247, '', 'Zimbabwe', 80);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (489, 248, '', 'Åland Islands', 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (2, 'BOTSWANA', 269, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (3, 'Brasil', 271, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (4, 'Gambia, The', 320, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (5, 'Germany (Federal Republic of)', 322, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (6, 'GHANA', 323, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (7, 'INDIA', 342, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (8, 'Sumatra', 343, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (9, 'Iran', 344, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (10, 'Iran (Islamic Republic of)', 344, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (11, 'IRAQ', 345, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (12, 'Laos', 361, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (13, 'LESOTHO', 364, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (14, 'MOZAMBIQUE', 391, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (15, 'NAMIBIA', 393, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (16, 'SWAZILAND', 453, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (17, 'Taiwan', 457, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (18, 'Tanzania', 459, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (19, 'UAE', 473, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (20, 'USA', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (21, 'U.S.A', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (22, 'U.S.A.', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (23, 'United States of America', 475, 80);
INSERT INTO controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) VALUES (24, 'YEMEN', 486, 80);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (3, 'Social Science and Humanities Metadata', 'socialscience', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (90, true, false, true, 'Basic unit of analysis or observation that this Dataset describes, such as individuals, families/households, groups, institutions/organizations, administrative units, and more. For information about the DDI''s controlled vocabulary for this element, please refer to the DDI web page at http://www.ddialliance.org/controlled-vocabularies.', '', false, 0, true, 'TEXTBOX', 'unitOfAnalysis', false, 'Unit of Analysis', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (91, true, false, true, 'Description of the population covered by the data in the file; the group of people or other elements that are the object of the study and to which the study results refer. Age, nationality, and residence commonly help to  delineate a given universe, but any number of other factors may be used, such as age limits, sex, marital status, race, ethnic group, nationality, income, veteran status, criminal convictions, and more. The universe may consist of elements other than persons, such as housing units, court cases, deaths, countries, and so on. In general, it should be possible to tell from the description of the universe whether a given individual or element is a member of the population under study. Also known as the universe of interest, population of interest, and target population.', '', false, 1, true, 'TEXTBOX', 'universe', false, 'Universe', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (92, true, false, false, 'The time method or time dimension of the data collection, such as panel, cross-sectional, trend, time- series, or other.', '', false, 2, true, 'TEXT', 'timeMethod', false, 'Time Method', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (93, false, false, false, 'Individual, agency or organization responsible for  administering the questionnaire or interview or compiling the data.', '', false, 3, false, 'TEXT', 'dataCollector', false, 'Data Collector', NULL, NULL, 'FamilyName, GivenName or Organization', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (94, false, false, false, 'Type of training provided to the data collector', '', false, 4, false, 'TEXT', 'collectorTraining', false, 'Collector Training', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (95, true, false, false, 'If the data collected includes more than one point in time, indicate the frequency with which the data was collected; that is, monthly, quarterly, or other.', '', false, 5, true, 'TEXT', 'frequencyOfDataCollection', false, 'Frequency', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (96, false, false, false, 'Type of sample and sample design used to select the survey respondents to represent the population. May include reference to the target sample size and the sampling fraction.', '', false, 6, false, 'TEXTBOX', 'samplingProcedure', false, 'Sampling Procedure', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (97, false, false, false, 'Specific information regarding the target sample size, actual  sample size, and the formula used to determine this.', '', false, 7, false, 'NONE', 'targetSampleSize', false, 'Target Sample Size', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (98, false, false, false, 'Actual sample size.', '', false, 8, false, 'INT', 'targetSampleActualSize', false, 'Actual', NULL, NULL, 'Enter an integer...', 3, 97);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (99, false, false, false, 'Formula used to determine target sample size.', '', false, 9, false, 'TEXT', 'targetSampleSizeFormula', false, 'Formula', NULL, NULL, '', 3, 97);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (100, false, false, false, 'Show correspondence as well as discrepancies between the sampled units (obtained) and available statistics for the population (age, sex-ratio, marital status, etc.) as a whole.', '', false, 10, false, 'TEXT', 'deviationsFromSampleDesign', false, 'Major Deviations for Sample Design', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (101, false, false, false, 'Method used to collect the data; instrumentation characteristics (e.g., telephone interview, mail questionnaire, or other).', '', false, 11, false, 'TEXTBOX', 'collectionMode', false, 'Collection Mode', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (102, false, false, false, 'Type of data collection instrument used. Structured indicates an instrument in which all respondents are asked the same questions/tests, possibly with precoded answers. If a small portion of such a questionnaire includes open-ended questions, provide appropriate comments. Semi-structured indicates that the research instrument contains mainly open-ended questions. Unstructured indicates that in-depth interviews were conducted.', '', false, 12, false, 'TEXT', 'researchInstrument', false, 'Type of Research Instrument', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (103, false, false, false, 'Description of noteworthy aspects of the data collection situation. Includes information on factors such as cooperativeness of respondents, duration of interviews, number of call backs, or similar.', '', false, 13, false, 'TEXTBOX', 'dataCollectionSituation', false, 'Characteristics of Data Collection Situation', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (104, false, false, false, 'Summary of actions taken to minimize data loss. Include information on actions such as follow-up visits, supervisory checks, historical matching, estimation, and so on.', '', false, 14, false, 'TEXT', 'actionsToMinimizeLoss', false, 'Actions to Minimize Losses', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (105, false, false, false, 'Control OperationsMethods to facilitate data control performed by the primary investigator or by the data archive.', '', false, 15, false, 'TEXT', 'controlOperations', false, 'Control Operations', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (106, false, false, false, 'The use of sampling procedures might make it necessary to apply weights to produce accurate statistical results. Describes the criteria for using weights in analysis of a collection. If a weighting formula or coefficient was developed, the formula is provided, its elements are defined, and it is indicated how the formula was applied to the data.', '', false, 16, false, 'TEXTBOX', 'weighting', false, 'Weighting', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (107, false, false, false, 'Methods used to clean the data collection, such as consistency checking, wildcode checking, or other.', '', false, 17, false, 'TEXT', 'cleaningOperations', false, 'Cleaning Operations', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (108, false, false, false, 'Note element used for any information annotating or clarifying the methodology and processing of the study. ', '', false, 18, false, 'TEXT', 'datasetLevelErrorNotes', false, 'Study Level Error Notes', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (109, true, false, false, 'Percentage of sample members who provided information.', '', false, 19, true, 'TEXTBOX', 'responseRate', false, 'Response Rate', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (110, false, false, false, 'Measure of how precisely one can estimate a population value from a given sample.', '', false, 20, false, 'TEXT', 'samplingErrorEstimates', false, 'Estimates of Sampling Error', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (111, false, false, false, 'Other issues pertaining to the data appraisal. Describe issues such as response variance, nonresponse rate  and testing for bias, interviewer and response bias, confidence levels, question bias, or similar.', '', false, 21, false, 'TEXT', 'otherDataAppraisal', false, 'Other Forms of Data Appraisal', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (112, false, false, false, 'General notes about this Dataset.', '', false, 22, false, 'NONE', 'socialScienceNotes', false, 'Notes', NULL, NULL, '', 3, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (113, false, false, false, 'Type of note.', '', false, 23, false, 'TEXT', 'socialScienceNotesType', false, 'Type', NULL, NULL, '', 3, 112);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (114, false, false, false, 'Note subject.', '', false, 24, false, 'TEXT', 'socialScienceNotesSubject', false, 'Subject', NULL, NULL, '', 3, 112);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (115, false, false, false, 'Text for this note.', '', false, 25, false, 'TEXTBOX', 'socialScienceNotesText', false, 'Text', NULL, NULL, '', 3, 112);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (4, 'Astronomy and Astrophysics Metadata', 'astrophysics', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (116, true, true, true, 'The nature or genre of the content of the files in the dataset.', '', false, 0, true, 'TEXT', 'astroType', false, 'Type', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (117, true, true, true, 'The observatory or facility where the data was obtained. ', '', false, 1, true, 'TEXT', 'astroFacility', false, 'Facility', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (118, true, true, true, 'The instrument used to collect the data.', '', false, 2, true, 'TEXT', 'astroInstrument', false, 'Instrument', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (119, true, false, true, 'Astronomical Objects represented in the data (Given as SIMBAD recognizable names preferred).', '', false, 3, true, 'TEXT', 'astroObject', false, 'Object', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (120, true, false, false, 'The spatial (angular) resolution that is typical of the observations, in decimal degrees.', '', false, 4, true, 'TEXT', 'resolution.Spatial', false, 'Spatial Resolution', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (121, true, false, false, 'The spectral resolution that is typical of the observations, given as the ratio λ/Δλ.', '', false, 5, true, 'TEXT', 'resolution.Spectral', false, 'Spectral Resolution', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (122, false, false, false, 'The temporal resolution that is typical of the observations, given in seconds.', '', false, 6, false, 'TEXT', 'resolution.Temporal', false, 'Time Resolution', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (123, true, true, true, 'Conventional bandpass name', '', false, 7, true, 'TEXT', 'coverage.Spectral.Bandpass', false, 'Bandpass', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (124, true, false, true, 'The central wavelength of the spectral bandpass, in meters.', '', false, 8, true, 'FLOAT', 'coverage.Spectral.CentralWavelength', false, 'Central Wavelength (m)', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (125, false, false, true, 'The minimum and maximum wavelength of the spectral bandpass.', '', false, 9, false, 'NONE', 'coverage.Spectral.Wavelength', false, 'Wavelength Range', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (126, true, false, false, 'The minimum wavelength of the spectral bandpass, in meters.', '', false, 10, true, 'FLOAT', 'coverage.Spectral.MinimumWavelength', false, 'Minimum (m)', NULL, NULL, 'Enter a floating-point number.', 4, 125);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (127, true, false, false, 'The maximum wavelength of the spectral bandpass, in meters.', '', false, 11, true, 'FLOAT', 'coverage.Spectral.MaximumWavelength', false, 'Maximum (m)', NULL, NULL, 'Enter a floating-point number.', 4, 125);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (128, false, false, true, ' Time period covered by the data.', '', false, 12, false, 'NONE', 'coverage.Temporal', false, 'Dataset Date Range', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (129, true, false, false, 'Dataset Start Date', '', false, 13, true, 'DATE', 'coverage.Temporal.StartTime', false, 'Start', NULL, NULL, 'YYYY-MM-DD', 4, 128);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (130, true, false, false, 'Dataset End Date', '', false, 14, true, 'DATE', 'coverage.Temporal.StopTime', false, 'End', NULL, NULL, 'YYYY-MM-DD', 4, 128);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (131, false, false, true, 'The sky coverage of the data object.', '', false, 15, false, 'TEXT', 'coverage.Spatial', false, 'Sky Coverage', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (132, false, false, false, 'The (typical) depth coverage, or sensitivity, of the data object in Jy.', '', false, 16, false, 'FLOAT', 'coverage.Depth', false, 'Depth Coverage', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (133, false, false, false, 'The (typical) density of objects, catalog entries, telescope pointings, etc., on the sky, in number per square degree.', '', false, 17, false, 'FLOAT', 'coverage.ObjectDensity', false, 'Object Density', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (134, false, false, false, 'The total number of objects, catalog entries, etc., in the data object.', '', false, 18, false, 'INT', 'coverage.ObjectCount', false, 'Object Count', NULL, NULL, 'Enter an integer.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (135, false, false, false, 'The fraction of the sky represented in the observations, ranging from 0 to 1.', '', false, 19, false, 'FLOAT', 'coverage.SkyFraction', false, 'Fraction of Sky', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (136, false, false, false, 'The polarization coverage', '', false, 20, false, 'TEXT', 'coverage.Polarization', false, 'Polarization', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (137, false, false, false, 'RedshiftType string C "Redshift"; or "Optical" or "Radio" definitions of Doppler velocity used in the data object.', '', false, 21, false, 'TEXT', 'redshiftType', false, 'RedshiftType', NULL, NULL, '', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (138, false, false, false, 'The resolution in redshift (unitless) or Doppler velocity (km/s) in the data object.', '', false, 22, false, 'FLOAT', 'resolution.Redshift', false, 'Redshift Resolution', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (139, false, false, true, 'The value of the redshift (unitless) or Doppler velocity (km/s in the data object.', '', false, 23, false, 'FLOAT', 'coverage.RedshiftValue', false, 'Redshift Value', NULL, NULL, 'Enter a floating-point number.', 4, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (140, false, false, false, 'The minimum value of the redshift (unitless) or Doppler velocity (km/s in the data object.', '', false, 24, false, 'FLOAT', 'coverage.Redshift.MinimumValue', false, 'Minimum', NULL, NULL, 'Enter a floating-point number.', 4, 139);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (141, false, false, false, 'The maximum value of the redshift (unitless) or Doppler velocity (km/s in the data object.', '', false, 25, false, 'FLOAT', 'coverage.Redshift.MaximumValue', false, 'Maximum', NULL, NULL, 'Enter a floating-point number.', 4, 139);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (490, 0, '', 'Image', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (491, 1, '', 'Mosaic', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (492, 2, '', 'EventList', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (493, 3, '', 'Spectrum', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (494, 4, '', 'Cube', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (495, 5, '', 'Table', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (496, 6, '', 'Catalog', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (497, 7, '', 'LightCurve', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (498, 8, '', 'Simulation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (499, 9, '', 'Figure', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (500, 10, '', 'Artwork', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (501, 11, '', 'Animation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (502, 12, '', 'PrettyPicture', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (503, 13, '', 'Documentation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (504, 14, '', 'Other', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (505, 15, '', 'Library', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (506, 16, '', 'Press Release', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (507, 17, '', 'Facsimile', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (508, 18, '', 'Historical', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (509, 19, '', 'Observation', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (510, 20, '', 'Object', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (511, 21, '', 'Value', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (512, 22, '', 'ValuePair', 116);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (513, 23, '', 'Survey', 116);


INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (5, 'Life Sciences Metadata', 'biomedical', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (142, true, true, true, 'Design types that are based on the overall experimental design.', '', false, 0, true, 'TEXT', 'studyDesignType', false, 'Design Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (143, true, true, true, 'Factors used in the Dataset. ', '', false, 1, true, 'TEXT', 'studyFactorType', false, 'Factor Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (144, true, true, true, 'The taxonomic name of the organism used in the Dataset or from which the  starting biological material derives.', '', false, 2, true, 'TEXT', 'studyAssayOrganism', false, 'Organism', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (145, true, false, true, 'If Other was selected in Organism, list any other organisms that were used in this Dataset. Terms from the NCBI Taxonomy are recommended.', '', false, 3, true, 'TEXT', 'studyAssayOtherOrganism', false, 'Other Organism', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (146, true, true, true, 'A term to qualify the endpoint, or what is being measured (e.g. gene expression profiling; protein identification). ', '', false, 4, true, 'TEXT', 'studyAssayMeasurementType', false, 'Measurement Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (147, true, false, true, 'If Other was selected in Measurement Type, list any other measurement types that were used. Terms from NCBO Bioportal are recommended.', '', false, 5, true, 'TEXT', 'studyAssayOtherMeasurmentType', false, 'Other Measurement Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (148, true, true, true, 'A term to identify the technology used to perform the measurement (e.g. DNA microarray; mass spectrometry).', '', false, 6, true, 'TEXT', 'studyAssayTechnologyType', false, 'Technology Type', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (149, true, true, true, 'The manufacturer and name of the technology platform used in the assay (e.g. Bruker AVANCE).', '', false, 7, true, 'TEXT', 'studyAssayPlatform', false, 'Technology Platform', NULL, NULL, '', 5, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (150, true, true, true, 'The name of the cell line from which the source or sample derives.', '', false, 8, true, 'TEXT', 'studyAssayCellType', false, 'Cell Type', NULL, NULL, '', 5, NULL);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (514, 0, 'EFO_0001427', 'Case Control', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (515, 1, 'EFO_0001428', 'Cross Sectional', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (516, 2, 'OCRE100078', 'Cohort Study', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (517, 3, 'NCI_C48202', 'Nested Case Control Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (518, 4, 'OTHER_DESIGN', 'Not Specified', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (519, 5, 'OBI_0500006', 'Parallel Group Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (520, 6, 'OBI_0001033', 'Perturbation Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (521, 7, 'MESH_D016449', 'Randomized Controlled Trial', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (522, 8, 'TECH_DESIGN', 'Technological Design', 142);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (523, 0, 'EFO_0000246', 'Age', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (524, 1, 'BIOMARKERS', 'Biomarkers', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (525, 2, 'CELL_SURFACE_M', 'Cell Surface Markers', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (526, 3, 'EFO_0000324;EFO_0000322', 'Cell Type/Cell Line', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (527, 4, 'EFO_0000399', 'Developmental Stage', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (528, 5, 'OBI_0001293', 'Disease State', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (529, 6, 'IDO_0000469', 'Drug Susceptibility', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (530, 7, 'FBcv_0010001', 'Extract Molecule', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (531, 8, 'OBI_0001404', 'Genetic Characteristics', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (532, 9, 'OBI_0000690', 'Immunoprecipitation Antibody', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (533, 10, 'OBI_0100026', 'Organism', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (534, 11, 'OTHER_FACTOR', 'Other', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (535, 12, 'PASSAGES_FACTOR', 'Passages', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (536, 13, 'OBI_0000050', 'Platform', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (537, 14, 'EFO_0000695', 'Sex', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (538, 15, 'EFO_0005135', 'Strain', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (539, 16, 'EFO_0000724', 'Time Point', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (540, 17, 'BTO_0001384', 'Tissue Type', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (541, 18, 'EFO_0000369', 'Treatment Compound', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (542, 19, 'EFO_0000727', 'Treatment Type', 143);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (543, 0, 'ERO_0001899', 'cell counting', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (544, 1, 'CHMO_0001085', 'cell sorting', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (545, 2, 'OBI_0000520', 'clinical chemistry analysis', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (546, 3, 'OBI_0000537', 'copy number variation profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (547, 4, 'OBI_0000634', 'DNA methylation profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (548, 5, 'OBI_0000748', 'DNA methylation profiling (Bisulfite-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (549, 6, '_OBI_0000634', 'DNA methylation profiling (MeDIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (550, 7, '_IDO_0000469', 'drug susceptibility', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (551, 8, 'ENV_GENE_SURVEY', 'environmental gene survey', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (552, 9, 'ERO_0001183', 'genome sequencing', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (553, 10, 'OBI_0000630', 'hematology', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (554, 11, 'OBI_0600020', 'histology', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (555, 12, 'OBI_0002017', 'Histone Modification (ChIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (556, 13, 'SO_0001786', 'loss of heterozygosity profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (557, 14, 'OBI_0000366', 'metabolite profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (558, 15, 'METAGENOME_SEQ', 'metagenome sequencing', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (559, 16, 'OBI_0000615', 'protein expression profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (560, 17, 'ERO_0000346', 'protein identification', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (561, 18, 'PROTEIN_DNA_BINDING', 'protein-DNA binding site identification', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (562, 19, 'OBI_0000288', 'protein-protein interaction detection', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (563, 20, 'PROTEIN_RNA_BINDING', 'protein-RNA binding (RIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (564, 21, 'OBI_0000435', 'SNP analysis', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (565, 22, 'TARGETED_SEQ', 'targeted sequencing', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (566, 23, 'OBI_0002018', 'transcription factor binding (ChIP-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (567, 24, 'OBI_0000291', 'transcription factor binding site identification', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (568, 26, 'EFO_0001032', 'transcription profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (569, 27, 'TRANSCRIPTION_PROF', 'transcription profiling (Microarray)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (570, 28, 'OBI_0001271', 'transcription profiling (RNA-Seq)', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (571, 29, 'TRAP_TRANS_PROF', 'TRAP translational profiling', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (572, 30, 'OTHER_MEASUREMENT', 'Other', 146);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (573, 0, 'NCBITaxon_3702', 'Arabidopsis thaliana', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (574, 1, 'NCBITaxon_9913', 'Bos taurus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (575, 2, 'NCBITaxon_6239', 'Caenorhabditis elegans', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (576, 3, 'NCBITaxon_3055', 'Chlamydomonas reinhardtii', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (577, 4, 'NCBITaxon_7955', 'Danio rerio (zebrafish)', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (578, 5, 'NCBITaxon_44689', 'Dictyostelium discoideum', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (579, 6, 'NCBITaxon_7227', 'Drosophila melanogaster', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (580, 7, 'NCBITaxon_562', 'Escherichia coli', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (581, 8, 'NCBITaxon_11103', 'Hepatitis C virus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (582, 9, 'NCBITaxon_9606', 'Homo sapiens', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (583, 10, 'NCBITaxon_10090', 'Mus musculus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (584, 11, 'NCBITaxon_33894', 'Mycobacterium africanum', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (585, 12, 'NCBITaxon_78331', 'Mycobacterium canetti', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (586, 13, 'NCBITaxon_1773', 'Mycobacterium tuberculosis', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (587, 14, 'NCBITaxon_2104', 'Mycoplasma pneumoniae', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (588, 15, 'NCBITaxon_4530', 'Oryza sativa', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (589, 16, 'NCBITaxon_5833', 'Plasmodium falciparum', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (590, 17, 'NCBITaxon_4754', 'Pneumocystis carinii', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (591, 18, 'NCBITaxon_10116', 'Rattus norvegicus', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (592, 19, 'NCBITaxon_4932', 'Saccharomyces cerevisiae (brewer''s yeast)', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (593, 20, 'NCBITaxon_4896', 'Schizosaccharomyces pombe', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (594, 21, 'NCBITaxon_31033', 'Takifugu rubripes', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (595, 22, 'NCBITaxon_8355', 'Xenopus laevis', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (596, 23, 'NCBITaxon_4577', 'Zea mays', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (597, 24, 'OTHER_TAXONOMY', 'Other', 144);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (598, 0, 'CULTURE_DRUG_TEST_SINGLE', 'culture based drug susceptibility testing, single concentration', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (599, 1, 'CULTURE_DRUG_TEST_TWO', 'culture based drug susceptibility testing, two concentrations', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (600, 2, 'CULTURE_DRUG_TEST_THREE', 'culture based drug susceptibility testing, three or more concentrations (minimium inhibitory concentration measurement)', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (601, 3, 'OBI_0400148', 'DNA microarray', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (602, 4, 'OBI_0000916', 'flow cytometry', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (603, 5, 'OBI_0600053', 'gel electrophoresis', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (604, 6, 'OBI_0000470', 'mass spectrometry', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (605, 7, 'OBI_0000623', 'NMR spectroscopy', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (606, 8, 'OBI_0000626', 'nucleotide sequencing', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (607, 9, 'OBI_0400149', 'protein microarray', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (608, 10, 'OBI_0000893', 'real time PCR', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (609, 11, 'NO_TECHNOLOGY', 'no technology required', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (610, 12, 'OTHER_TECHNOLOGY', 'Other', 148);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (611, 0, '210_MS_GC', '210-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (612, 1, '220_MS_GC', '220-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (613, 2, '225_MS_GC', '225-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (614, 3, '240_MS_GC', '240-MS GC Ion Trap (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (615, 4, '300_MS_GCMS', '300-MS quadrupole GC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (616, 5, '320_MS_LCMS', '320-MS LC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (617, 6, '325_MS_LCMS', '325-MS LC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (618, 7, '500_MS_GCMS', '320-MS GC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (619, 8, '500_MS_LCMS', '500-MS LC/MS (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (620, 9, '800D', '800D (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (621, 10, '910_MS_TQFT', '910-MS TQ-FT (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (622, 11, '920_MS_TQFT', '920-MS TQ-FT (Varian)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (623, 12, '3100_MASS_D', '3100 Mass Detector (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (624, 13, '6110_QUAD_LCMS', '6110 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (625, 14, '6120_QUAD_LCMS', '6120 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (626, 15, '6130_QUAD_LCMS', '6130 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (627, 16, '6140_QUAD_LCMS', '6140 Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (628, 17, '6310_ION_LCMS', '6310 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (629, 18, '6320_ION_LCMS', '6320 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (630, 19, '6330_ION_LCMS', '6330 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (631, 20, '6340_ION_LCMS', '6340 Ion Trap LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (632, 21, '6410_TRIPLE_LCMS', '6410 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (633, 22, '6430_TRIPLE_LCMS', '6430 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (634, 23, '6460_TRIPLE_LCMS', '6460 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (635, 24, '6490_TRIPLE_LCMS', '6490 Triple Quadrupole LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (636, 25, '6530_Q_TOF_LCMS', '6530 Q-TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (637, 26, '6540_Q_TOF_LCMS', '6540 Q-TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (638, 27, '6210_Q_TOF_LCMS', '6210 TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (639, 28, '6220_Q_TOF_LCMS', '6220 TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (640, 29, '6230_Q_TOF_LCMS', '6230 TOF LC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (641, 30, '700B_TRIPLE_GCMS', '7000B Triple Quadrupole GC/MS (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (642, 31, 'ACCUTO_DART', 'AccuTO DART (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (643, 32, 'ACCUTOF_GC', 'AccuTOF GC (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (644, 33, 'ACCUTOF_LC', 'AccuTOF LC (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (645, 34, 'ACQUITY_SQD', 'ACQUITY SQD (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (646, 35, 'ACQUITY_TQD', 'ACQUITY TQD (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (647, 36, 'AGILENT', 'Agilent', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (648, 37, 'AGILENT_ 5975E_GCMSD', 'Agilent 5975E GC/MSD (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (649, 38, 'AGILENT_5975T_LTM_GCMSD', 'Agilent 5975T LTM GC/MSD (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (650, 39, '5975C_GCMSD', '5975C Series GC/MSD (Agilent)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (651, 40, 'AFFYMETRIX', 'Affymetrix', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (652, 41, 'AMAZON_ETD_ESI', 'amaZon ETD ESI Ion Trap (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (653, 42, 'AMAZON_X_ESI', 'amaZon X ESI Ion Trap (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (654, 43, 'APEX_ULTRA_QQ_FTMS', 'apex-ultra hybrid Qq-FTMS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (655, 44, 'API_2000', 'API 2000 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (656, 45, 'API_3200', 'API 3200 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (657, 46, 'API_3200_QTRAP', 'API 3200 QTRAP (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (658, 47, 'API_4000', 'API 4000 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (659, 48, 'API_4000_QTRAP', 'API 4000 QTRAP (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (660, 49, 'API_5000', 'API 5000 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (661, 50, 'API_5500', 'API 5500 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (662, 51, 'API_5500_QTRAP', 'API 5500 QTRAP (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (663, 52, 'APPLIED_BIOSYSTEMS', 'Applied Biosystems Group (ABI)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (664, 53, 'AQI_BIOSCIENCES', 'AQI Biosciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (665, 54, 'ATMOS_GC', 'Atmospheric Pressure GC (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (666, 55, 'AUTOFLEX_III_MALDI_TOF_MS', 'autoflex III MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (667, 56, 'AUTOFLEX_SPEED', 'autoflex speed(Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (668, 57, 'AUTOSPEC_PREMIER', 'AutoSpec Premier (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (669, 58, 'AXIMA_MEGA_TOF', 'AXIMA Mega TOF (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (670, 59, 'AXIMA_PERF_MALDI_TOF', 'AXIMA Performance MALDI TOF/TOF (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (671, 60, 'A_10_ANALYZER', 'A-10 Analyzer (Apogee)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (672, 61, 'A_40_MINIFCM', 'A-40-MiniFCM (Apogee)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (673, 62, 'BACTIFLOW', 'Bactiflow (Chemunex SA)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (674, 63, 'BASE4INNOVATION', 'Base4innovation', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (675, 64, 'BD_BACTEC_MGIT_320', 'BD BACTEC MGIT 320', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (676, 65, 'BD_BACTEC_MGIT_960', 'BD BACTEC MGIT 960', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (677, 66, 'BD_RADIO_BACTEC_460TB', 'BD Radiometric BACTEC 460TB', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (678, 67, 'BIONANOMATRIX', 'BioNanomatrix', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (679, 68, 'CELL_LAB_QUANTA_SC', 'Cell Lab Quanta SC (Becman Coulter)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (680, 69, 'CLARUS_560_D_GCMS', 'Clarus 560 D GC/MS (PerkinElmer)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (681, 70, 'CLARUS_560_S_GCMS', 'Clarus 560 S GC/MS (PerkinElmer)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (682, 71, 'CLARUS_600_GCMS', 'Clarus 600 GC/MS (PerkinElmer)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (683, 72, 'COMPLETE_GENOMICS', 'Complete Genomics', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (684, 73, 'CYAN', 'Cyan (Dako Cytomation)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (685, 74, 'CYFLOW_ML', 'CyFlow ML (Partec)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (686, 75, 'CYFLOW_SL', 'Cyow SL (Partec)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (687, 76, 'CYFLOW_SL3', 'CyFlow SL3 (Partec)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (688, 77, 'CYTOBUOY', 'CytoBuoy (Cyto Buoy Inc)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (689, 78, 'CYTOSENCE', 'CytoSence (Cyto Buoy Inc)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (690, 79, 'CYTOSUB', 'CytoSub (Cyto Buoy Inc)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (691, 80, 'DANAHER', 'Danaher', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (692, 81, 'DFS', 'DFS (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (693, 82, 'EXACTIVE', 'Exactive(Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (694, 83, 'FACS_CANTO', 'FACS Canto (Becton Dickinson)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (695, 84, 'FACS_CANTO2', 'FACS Canto2 (Becton Dickinson)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (696, 85, 'FACS_SCAN', 'FACS Scan (Becton Dickinson)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (697, 86, 'FC_500', 'FC 500 (Becman Coulter)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (698, 87, 'GCMATE_II', 'GCmate II GC/MS (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (699, 88, 'GCMS_QP2010_PLUS', 'GCMS-QP2010 Plus (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (700, 89, 'GCMS_QP2010S_PLUS', 'GCMS-QP2010S Plus (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (701, 90, 'GCT_PREMIER', 'GCT Premier (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (702, 91, 'GENEQ', 'GENEQ', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (703, 92, 'GENOME_CORP', 'Genome Corp.', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (704, 93, 'GENOVOXX', 'GenoVoxx', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (705, 94, 'GNUBIO', 'GnuBio', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (706, 95, 'GUAVA_EASYCYTE_MINI', 'Guava EasyCyte Mini (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (707, 96, 'GUAVA_EASYCYTE_PLUS', 'Guava EasyCyte Plus (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (708, 97, 'GUAVA_PERSONAL_CELL', 'Guava Personal Cell Analysis (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (709, 98, 'GUAVA_PERSONAL_CELL_96', 'Guava Personal Cell Analysis-96 (Millipore)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (710, 99, 'HELICOS_BIO', 'Helicos BioSciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (711, 100, 'ILLUMINA', 'Illumina', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (712, 101, 'INDIRECT_LJ_MEDIUM', 'Indirect proportion method on LJ medium', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (713, 102, 'INDIRECT_AGAR_7H9', 'Indirect proportion method on Middlebrook Agar 7H9', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (714, 103, 'INDIRECT_AGAR_7H10', 'Indirect proportion method on Middlebrook Agar 7H10', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (715, 104, 'INDIRECT_AGAR_7H11', 'Indirect proportion method on Middlebrook Agar 7H11', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (716, 105, 'INFLUX_ANALYZER', 'inFlux Analyzer (Cytopeia)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (717, 106, 'INTELLIGENT_BIOSYSTEMS', 'Intelligent Bio-Systems', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (718, 107, 'ITQ_700', 'ITQ 700 (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (719, 108, 'ITQ_900', 'ITQ 900 (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (720, 109, 'ITQ_1100', 'ITQ 1100 (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (721, 110, 'JMS_53000_SPIRAL', 'JMS-53000 SpiralTOF (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (722, 111, 'LASERGEN', 'LaserGen', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (723, 112, 'LCMS_2020', 'LCMS-2020 (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (724, 113, 'LCMS_2010EV', 'LCMS-2010EV (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (725, 114, 'LCMS_IT_TOF', 'LCMS-IT-TOF (Shimadzu)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (726, 115, 'LI_COR', 'Li-Cor', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (727, 116, 'LIFE_TECH', 'Life Tech', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (728, 117, 'LIGHTSPEED_GENOMICS', 'LightSpeed Genomics', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (729, 118, 'LCT_PREMIER_XE', 'LCT Premier XE (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (730, 119, 'LCQ_DECA_XP_MAX', 'LCQ Deca XP MAX (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (731, 120, 'LCQ_FLEET', 'LCQ Fleet (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (732, 121, 'LXQ_THERMO', 'LXQ (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (733, 122, 'LTQ_CLASSIC', 'LTQ Classic (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (734, 123, 'LTQ_XL', 'LTQ XL (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (735, 124, 'LTQ_VELOS', 'LTQ Velos (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (736, 125, 'LTQ_ORBITRAP_CLASSIC', 'LTQ Orbitrap Classic (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (737, 126, 'LTQ_ORBITRAP_XL', 'LTQ Orbitrap XL (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (738, 127, 'LTQ_ORBITRAP_DISCOVERY', 'LTQ Orbitrap Discovery (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (739, 128, 'LTQ_ORBITRAP_VELOS', 'LTQ Orbitrap Velos (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (740, 129, 'LUMINEX_100', 'Luminex 100 (Luminex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (741, 130, 'LUMINEX_200', 'Luminex 200 (Luminex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (742, 131, 'MACS_QUANT', 'MACS Quant (Miltenyi)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (743, 132, 'MALDI_SYNAPT_G2_HDMS', 'MALDI SYNAPT G2 HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (744, 133, 'MALDI_SYNAPT_G2_MS', 'MALDI SYNAPT G2 MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (745, 134, 'MALDI_SYNAPT_HDMS', 'MALDI SYNAPT HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (746, 135, 'MALDI_SYNAPT_MS', 'MALDI SYNAPT MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (747, 136, 'MALDI_MICROMX', 'MALDI micro MX (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (748, 137, 'MAXIS', 'maXis (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (749, 138, 'MAXISG4', 'maXis G4 (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (750, 139, 'MICROFLEX_LT_MALDI_TOF_MS', 'microflex LT MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (751, 140, 'MICROFLEX_LRF_MALDI_TOF_MS', 'microflex LRF MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (752, 141, 'MICROFLEX_III_TOF_MS', 'microflex III MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (753, 142, 'MICROTOF_II_ESI_TOF', 'micrOTOF II ESI TOF (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (754, 143, 'MICROTOF_Q_II_ESI_QQ_TOF', 'micrOTOF-Q II ESI-Qq-TOF (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (755, 144, 'MICROPLATE_ALAMAR_BLUE_COLORIMETRIC', 'microplate Alamar Blue (resazurin) colorimetric method', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (756, 145, 'MSTATION', 'Mstation (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (757, 146, 'MSQ_PLUS', 'MSQ Plus (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (758, 147, 'NABSYS', 'NABsys', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (759, 148, 'NANOPHOTONICS_BIOSCIENCES', 'Nanophotonics Biosciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (760, 149, 'NETWORK_BIOSYSTEMS', 'Network Biosystems', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (761, 150, 'NIMBLEGEN', 'Nimblegen', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (762, 151, 'OXFORD_NANOPORE_TECHNOLOGIES', 'Oxford Nanopore Technologies', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (763, 152, 'PACIFIC_BIOSCIENCES', 'Pacific Biosciences', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (764, 153, 'POPULATION_GENETICS_TECHNOLOGIES', 'Population Genetics Technologies', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (765, 154, 'Q1000GC_ULTRAQUAD', 'Q1000GC UltraQuad (Jeol)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (766, 155, 'QUATTRO_MICRO_API', 'Quattro micro API (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (767, 156, 'QUATTRO_MICRO_GC', 'Quattro micro GC (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (768, 157, 'QUATTRO_PREMIER_XE', 'Quattro Premier XE (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (769, 158, 'QSTAR', 'QSTAR (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (770, 159, 'REVEO', 'Reveo', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (771, 160, 'ROCHE', 'Roche', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (772, 161, 'SEIRAD', 'Seirad', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (773, 162, 'SOLARIX_HYBRID_QQ_FTMS', 'solariX hybrid Qq-FTMS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (774, 163, 'SOMACOUNT', 'Somacount (Bently Instruments)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (775, 164, 'SOMASCOPE', 'SomaScope (Bently Instruments)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (776, 165, 'SYNAPT_G2_HDMS', 'SYNAPT G2 HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (777, 166, 'SYNAPT_G2_MS', 'SYNAPT G2 MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (778, 167, 'SYNAPT_HDMS', 'SYNAPT HDMS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (779, 168, 'SYNAPT_MS', 'SYNAPT MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (780, 169, 'TRIPLETOF_5600', 'TripleTOF 5600 (AB Sciex)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (781, 170, 'TSQ_QUANTUM_ULTRA', 'TSQ Quantum Ultra (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (782, 171, 'TSQ_QUANTUM_ACCESS', 'TSQ Quantum Access (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (783, 172, 'TSQ_QUANTUM_ACCESS_MAX', 'TSQ Quantum Access MAX (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (784, 173, 'TSQ_QUANTUM_DISCOVERY_MAX', 'TSQ Quantum Discovery MAX (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (785, 174, 'TSQ_QUANTUM_GC', 'TSQ Quantum GC (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (786, 175, 'TSQ_QUANTUM_XLS', 'TSQ Quantum XLS (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (787, 176, 'TSQ_VANTAGE', 'TSQ Vantage (Thermo Scientific)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (788, 177, 'ULTRAFLEXTREME_MALDI_TOF_MS', 'ultrafleXtreme MALDI-TOF MS (Bruker)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (789, 178, 'VISIGEN_BIO', 'VisiGen Biotechnologies', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (790, 179, 'XEVO_G2_QTOF', 'Xevo G2 QTOF (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (791, 180, 'XEVO_QTOF_MS', 'Xevo QTof MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (792, 181, 'XEVO_TQ_MS', 'Xevo TQ MS (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (793, 182, 'XEVO_TQ_S', 'Xevo TQ-S (Waters)', 149);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (794, 183, 'OTHER_PLATFORM', 'Other', 149);



INSERT INTO metadatablock (id, displayname, name, namespaceuri, owner_id) VALUES (6, 'Journal Metadata', 'journal', NULL, NULL);

INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (151, false, false, true, 'Indicates the volume, issue and date of a journal, which this Dataset is associated with.', '', false, 0, false, 'NONE', 'journalVolumeIssue', false, 'Journal', NULL, NULL, '', 6, NULL);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (152, true, false, false, 'The journal volume which this Dataset is associated with (e.g., Volume 4).', '', false, 1, true, 'TEXT', 'journalVolume', false, 'Volume', NULL, NULL, '', 6, 151);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (153, true, false, false, 'The journal issue number which this Dataset is associated with (e.g., Number 2, Autumn).', '', false, 2, true, 'TEXT', 'journalIssue', false, 'Issue', NULL, NULL, '', 6, 151);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (154, true, false, false, 'The publication date for this journal volume/issue, which this Dataset is associated with (e.g., 1999).', '', false, 3, true, 'DATE', 'journalPubDate', false, 'Publication Date', NULL, NULL, 'YYYY or YYYY-MM or YYYY-MM-DD', 6, 151);
INSERT INTO datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, uri, validationformat, watermark, metadatablock_id, parentdatasetfieldtype_id) VALUES (155, true, true, false, 'Indicates what kind of article this is, for example, a research article, a commentary, a book or product review, a case report, a calendar, etc (based on JATS). ', '', false, 4, true, 'TEXT', 'journalArticleType', false, 'Type of Article', NULL, NULL, '', 6, NULL);

INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (795, 0, '', 'abstract', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (796, 1, '', 'addendum', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (797, 2, '', 'announcement', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (798, 3, '', 'article-commentary', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (799, 4, '', 'book review', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (800, 5, '', 'books received', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (801, 6, '', 'brief report', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (802, 7, '', 'calendar', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (803, 8, '', 'case report', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (804, 9, '', 'collection', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (805, 10, '', 'correction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (806, 11, '', 'data paper', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (807, 12, '', 'discussion', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (808, 13, '', 'dissertation', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (809, 14, '', 'editorial', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (810, 15, '', 'in brief', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (811, 16, '', 'introduction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (812, 17, '', 'letter', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (813, 18, '', 'meeting report', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (814, 19, '', 'news', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (815, 20, '', 'obituary', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (816, 21, '', 'oration', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (817, 22, '', 'partial retraction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (818, 23, '', 'product review', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (819, 24, '', 'rapid communication', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (820, 25, '', 'reply', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (821, 26, '', 'reprint', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (822, 27, '', 'research article', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (823, 28, '', 'retraction', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (824, 29, '', 'review article', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (825, 30, '', 'translation', 155);
INSERT INTO controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) VALUES (826, 31, '', 'other', 155);

SELECT setval('metadatablock_id_seq', COALESCE((SELECT MAX(id)+1 FROM metadatablock), 1), false);
SELECT setval('datasetfieldtype_id_seq', COALESCE((SELECT MAX(id)+1 FROM datasetfieldtype), 1), false);
SELECT setval('controlledvocabularyvalue_id_seq', COALESCE((SELECT MAX(id)+1 FROM controlledvocabularyvalue), 1), false);
SELECT setval('controlledvocabalternate_id_seq', COALESCE((SELECT MAX(id)+1 FROM controlledvocabalternate), 1), false);


INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (1, 'admin', 'A person who has all permissions for dataverses, datasets, and files.', 'Admin', 8191, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (2, 'fileDownloader', 'A person who can download a published file.', 'File Downloader', 16, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (3, 'fullContributor', 'A person who can add subdataverses and datasets within a dataverse.', 'Dataverse + Dataset Creator', 3, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (4, 'dvContributor', 'A person who can add subdataverses within a dataverse.', 'Dataverse Creator', 1, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (5, 'dsContributor', 'A person who can add datasets within a dataverse.', 'Dataset Creator', 2, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (6, 'editor', 'For datasets, a person who can edit License + Terms, and then submit them for review.', 'Contributor', 4184, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (7, 'curator', 'For datasets, a person who can edit License + Terms, edit Permissions, and publish datasets.', 'Curator', 5471, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (8, 'member', 'A person who can view both unpublished dataverses and datasets.', 'Member', 28, NULL);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (9, 'depositor', 'A person who has all permissions for dataverses, datasets, and files.', 'Depositor', 12382, NULL);
        
SELECT setval('dataverserole_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataverserole), 1), false);


INSERT INTO authenticationproviderrow (id, enabled, factoryalias, factorydata, subtitle, title) VALUES ('builtin', true, 'BuiltinAuthenticationProvider', '', 'Datavers'' Internal Authentication provider', 'Dataverse Local');


INSERT INTO builtinuser (id, encryptedpassword, passwordencryptionversion, username) VALUES (1, '$2a$10$AW8rVR2emcYruz6g13oQ5uti4CkKcH7HBpzXXEpSdipIwyQaT0sWm', 1, 'dataverseAdmin'); -- default password: admin
INSERT INTO authenticateduser (id, affiliation, createdtime, email, emailconfirmed, firstname, lastapiusetime, lastlogintime, lastname, "position", superuser, useridentifier) VALUES (1, 'Dataverse.org', '2019-02-04 19:43:34.906', 'dataverse@mailinator.com', NULL, 'Dataverse', '2019-02-04 19:43:35.712', '2019-02-04 19:43:34.906', 'Admin', 'Admin', true, 'dataverseAdmin');
INSERT INTO authenticateduserlookup (id, authenticationproviderid, persistentuserid, authenticateduser_id) VALUES (1, 'builtin', 'dataverseAdmin', 1);

SELECT setval('builtinuser_id_seq', COALESCE((SELECT MAX(id)+1 FROM builtinuser), 1), false);
SELECT setval('authenticateduser_id_seq', COALESCE((SELECT MAX(id)+1 FROM authenticateduser), 1), false);
SELECT setval('authenticateduserlookup_id_seq', COALESCE((SELECT MAX(id)+1 FROM authenticateduserlookup), 1), false);


INSERT INTO dvobject (id, dtype, authority, createdate, globalidcreatetime, identifier, identifierregistered, indextime, modificationtime, permissionindextime, permissionmodificationtime, previewimageavailable, protocol, publicationdate, storageidentifier, creator_id, owner_id, releaseuser_id)
    VALUES (1, 'Dataverse', NULL, current_timestamp, NULL, NULL, false, NULL, current_timestamp, NULL, current_timestamp, false, NULL, NULL, NULL, 1, NULL, NULL);
INSERT INTO dataverse (id, affiliation, alias, allowmessagesbanners, dataversetype, description, facetroot, guestbookroot, metadatablockroot, name, permissionroot, templateroot, themeroot, defaultcontributorrole_id, defaulttemplate_id) 
    VALUES (1, NULL, 'root', false, 'UNCATEGORIZED', 'The root dataverse.', true, false, true, 'Root', true, false, true, 6, NULL);

INSERT INTO dataversecontact (id, contactemail, displayorder, dataverse_id) VALUES (1, 'root@mailinator.com', 0, 1);
INSERT INTO dataverse_metadatablock (dataverse_id, metadatablocks_id) VALUES (1, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (5, 0, 9, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (6, 3, 58, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (7, 1, 20, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (8, 2, 22, 1);

SELECT setval('dvobject_id_seq', COALESCE((SELECT MAX(id)+1 FROM dvobject), 1), false);
SELECT setval('dataversecontact_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversecontact), 1), false);
SELECT setval('dataversefacet_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversefacet), 1), false);


INSERT INTO guestbook (id, createtime, emailrequired, enabled, institutionrequired, name, namerequired, positionrequired, dataverse_id) VALUES (1, current_timestamp, false, true, false, 'Default', false, false, NULL);

SELECT setval('guestbook_id_seq', COALESCE((SELECT MAX(id)+1 FROM guestbook), 1), false);


--------------------------------------------------------------------------------
-- Licence model
--------------------------------------------------------------------------------
CREATE TABLE license (
    id SERIAL NOT NULL, 
    active BOOLEAN, 
    name VARCHAR(255) NOT NULL UNIQUE, 
    position BIGINT NOT NULL, 
    url VARCHAR(255) NOT NULL, 
    PRIMARY KEY (id)
);
CREATE TABLE licenseicon (
    id SERIAL NOT NULL, 
    content BYTEA NOT NULL, 
    contenttype VARCHAR(255) NOT NULL, 
    license_id BIGINT NOT NULL, 
    PRIMARY KEY (id)
);
CREATE TABLE license_localizedname (
    locale VARCHAR(255) NOT NULL,
    text VARCHAR(255) NOT NULL,
    license_id BIGINT
);

ALTER TABLE licenseicon ADD CONSTRAINT unq_licenseicon_0 UNIQUE (license_id);
ALTER TABLE licenseicon ADD CONSTRAINT fk_licenseicon_license_id FOREIGN KEY (license_id) REFERENCES license (ID);
ALTER TABLE license_localizedname ADD CONSTRAINT fk_license_localizedname_license_ID FOREIGN KEY (license_ID) REFERENCES LICENSE (id);


INSERT INTO license(id, active, name, "position", url) VALUES (1, true, 'CC0 Creative Commons Zero 1.0 Waiver', 1, 'https://creativecommons.org/publicdomain/zero/1.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (1, 'image/png', 1, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC0 Creative Commons Zero 1.0', 1);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC0 - Creative Commons Zero 1.0', 1);


INSERT INTO license(id, active, name, "position", url) VALUES (2, false, 'CC BY Creative Commons Attribution License 4.0', 2, 'https://creativecommons.org/licenses/by/4.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (2, 'image/png', 2, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC BY - Creative Commons Attribution 4.0', 2);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC BY - Creative Commons Uznanie Autorstwa 4.0', 2);


INSERT INTO license(id, active, name, "position", url) VALUES (3, false, 'CC BY-SA Creative Commons Attribution - ShareAlike License 4.0', 3, 'https://creativecommons.org/licenses/by-sa/4.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (3, 'image/png', 3, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC BY-SA Creative Commons Attribution - ShareAlike 4.0', 3);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC BY-SA Creative Commons Uznanie Autorstwa - Na tych samych warunkach 4.0', 3);


INSERT INTO license(id, active, name, "position", url) VALUES (4, false, 'CC BY-NC Creative Commons Attribution - NonCommercial License 4.0', 4, 'https://creativecommons.org/licenses/by-nc/4.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (4, 'image/png', 4, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC BY-NC Creative Commons Attribution - NonCommercial 4.0', 4);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC BY-NC Creative Commons Uznanie Autorstwa – Użycie niekomercyjne 4.0', 4);


INSERT INTO license(id, active, name, "position", url) VALUES (5, false, 'CC BY-ND Creative Commons Attribution - NoDerivs License 4.0', 5, 'https://creativecommons.org/licenses/by-nd/4.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (5, 'image/png', 5, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC BY-ND Creative Commons Attribution - NoDerivs 4.0', 5);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC BY-ND Creative Commons Uznanie autorstwa - Bez utworów zależnych 4.0', 5);


INSERT INTO license(id, active, name, "position", url) VALUES (6, false, 'CC BY-NC-SA Creative Commons Attribution - NonCommercial - ShareAlike License 4.0', 6, 'https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (6, 'image/png', 6, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC BY-NC-SA Creative Commons Attribution - NonCommercial - ShareAlike 4.0', 6);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC BY-NC-SA Creative Commons Uznanie autorstwa - Użycie niekomercyjne - Na tych samych warunkach 4.0', 6);


INSERT INTO license(id, active, name, "position", url) VALUES (7, false, 'CC BY-NC-ND Creative Commons Attribution - NonCommercial - NoDerivs License 4.0', 7, 'https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (7, 'image/png', 7, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'CC BY-NC-ND Creative Commons Attribution - NonCommercial - NoDerivs 4.0', 7);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'CC BY-NC-ND Creative Commons-Uznanie autorstwa - Użycie niekomercyjne - Bez utworów zależnych 4.0', 7);


INSERT INTO license(id, active, name, "position", url) VALUES (8, false, 'Apache Software License 2.0', 8, 'https://www.apache.org/licenses/LICENSE-2.0');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (8, 'image/gif', 8, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'Apache Software License 2.0', 8);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'Apache Software License 2.0', 8);


INSERT INTO license(id, active, name, "position", url) VALUES (9, false, 'BSD 3 – Clause “New” or “Revised” License', 9, 'https://opensource.org/licenses/BSD-3-Clause');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (9, 'image/png', 9, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'BSD 3 – Clause “New” or “Revised” License', 9);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'BSD 3-Clause “New” or “Revised” License', 9);


INSERT INTO license(id, active, name, "position", url) VALUES (10, false, 'GNU General Public License 3.0', 10, 'https://www.gnu.org/licenses/gpl-3.0.en.html');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (10, 'image/png', 10, decode('iVBORw0KGgoAAAANSUhEUgAAAMgAAABjCAYAAADeg0+zAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAABmJLR0QA/wD/AP+gvaeTAAAAB3RJTUUH4QgKAjACfM1KkwAAKExJREFUeNrtnXl8VNXZx7/nzoQsqGwi7guKWqxFBRUQ0MwEcG1dXlx5tWq1VWurttbW1hpb69LNqtW21lqtSy36qhWLApmEJaKiUlEErWtVXBAIICSEZO55/3jOJSc3987cmcwkQfN8PnxIMjN37j3n/J59UfRSL33OqA7KNIx04VBgTwW7I/92MG/RwIfAfxTMbYFHJ8P7QddSvcvZS1s6aVBz4FAXjgcmACOBPjlcogW4X8P3q2BVL0B66XNBKZig4RQlwNgx4C3rgGUK3tXwLtBgvTZYwQgN44AyA4b3FUyqhNeKDpAU/Bo4oXcbC0cKPtTBB+GLtAYK2EpDvwApkdaiOi2KwZMa5qyEd06GTWHXq4FBwPcUXA7EgbccGFUJa4oNkNeAfXqPdcFoPfAw8PUv6POngViwhsXbLjyk4dZJApCcqRa+quER8x3XJuGqogFkHgxugU96VbiCcs57gFYN536BHvt1A4ABwLY2QDSs13BfHK6qhJWF+LIa+IuCc4CV82FINbjxYjxVK4ztBUdhyYX7gNO+AIv6DPAWMBAYbf63loEU8Pcq+GsRjP17DUC2HQf7AkvjRdrMw3rRUVBa1x/mroUTP6fPt0bDAwpWaDhSwdSA9zym4cdVsLRYN+HAG9avOxYNIEokSC8VjmpGQUsKmj9nz7UA4drlwAXAMNXxLNVruCIp7y02xS2wtLT7Q6FoBpQCo3rPdEFF/wxzWNbpz8XjMF3D7wwjvUbDdgHvW63hsqTYXl11Y3urNi3o/aIApAxGaQFJLxWIYqJ3o+GjLduM4jENNygYpeABYPsQDeQfLny3Shw9XekIOdr8uKJe4iYUQ8XqVa8KSx9Wms1S8JHeMoExzYXrYrA34mzYO+xZFVyQgMe7+iarwQGmmHV+qFruu/AAyWKgLwLmK/gvsKEA36WAISbX5gSgf4jo/JUDbxZBJPdRsIMLIxQcic9Pr+ElB/4Q8LnhwHcjfscz1s9blARRUANckoYBCu7Q4pUKo7kaTkl2sdTwaAKcoGEns87TOhglBTowqjZYgtQpuDgBrxbrAWfCxXG4CTjPj6MYXOdFRotmRcNQBY8B+1uG3uMJuMP/3hR8P4dLbwZIK3wU3zKw8baG77XAvLgwp7PJ7Pa/yYEfVEJrd9xsHcRduNZjavVQb+1h4SgFw4DBvj8vXAVHFhMcAJNhQzNcDDT6Xnqt2OAAqIK3gSt8Eu6ZkLePyeHSi7wfBsHHRmXpqbRBw48d2E9BeQksNXEFleH9pyXhsu4CB0Bagq/7Gql3ZbW1xgVlSAoOC5AqtwflwtTAGAU75/lVq5JQ6/9jOaRdH+hVyCGtg71cODCfL3fgnUp4IeD5XctG0KXwXMglRufwdZv9/sbVu5Jgr093e6b+DvwgBiUaHkVUzox7qOGoKni+O2/cSP5fempeEp60Xy84QHRHD8zTIe/9k62O5LgbfyMAIK2wv2MyM7Nx8TRcrOA7edo+PyIAIMAhtuQa3z571JOyuxE94XB1gCfnrR4GkHc1nF8PqXFwqQvXAH2zfGa5hklRg36GmVUC+wG7mHP7tgt3TYRXOqla3QdsA2xw4Fv+9xTaBvFLkE+PkA1tRzPkhvbrBBADD30MxuiO3P7ZkGuMKfT320aoDn/PGBX9e4IO0BI6ce8F9k7d2go/VtBvAqQ0HBHhc8tbYfxkeCfTm2phBHCmhpNcYSpBkvysabB9pmzdLOf1RmstL7HT3AsOEJM27M/efVaJ+G1HfYTTOkU4oP6Ds2YeLPO/bwGUN8EBeX59a0uA9DAOitHZ7jGXw60DPG8KlnS3q1fBO2k4cyLU10hqyN90R9szUCIqmBwGjpnQNw5nAt/S8JUI1xuwDWwFrM7DXr5Yw2Xm14eTcGfQ++IFXLQOCYphh8QJ4PQ50PqVwkWz6vYanqsOMGobJVBVkuf3vzw5wEU9R5jDwAiSa3QOAHk/4G9LutnY+FszXFwOjSm4AfgB0RJTNzpwTGWAs2YmbBeXw3o+krkblZYdmR84jkM8ngAvGi8bRQUIAQZ6Ojqnz4UWniy1AR0WGdgrCkApgnrltj/4a+cGqEe5Si4HPvD/rQReaekebKxTcF4Sps2GXV0xZnMJCl9Y6WMa82DwJrhcwYUR7Jagc/SnPDSdY5E4R0zBOwqOrZRam7A9KBj5F6vVDfBQaOE2h3bigAZy5pIAzqyKYH+4Idf0gT5Qcm3IvVa6A0AmwKd0fTBtiYaDEzAtBcc58O8cwfHnpJWevgDKa+DKFnjLVPL1zeMcvLMJ/pij5DhNSVFUGbACOKpSXOcUFSAmQfFg359fiaKK5HFAo0olVxXGzRpJgvhAF6Za5vq9y0O+65UuBMffW2F0FfwnBT8E/pnj/r3bCJdYdtrpG+F1Bb8Ats73GKThnKNzyG5OiYfqXuGlfODA4QkpyKLoAOkDB+Fzr+pwVaQz6pWORdftlwUFCGfCHoQkyUWgFclwr9xw69mfLYRqp2BtCDPoCjukRcMlSTi9DzSnJCPgenIshFNw0XHQWAv71Iqn634trtrO0K8mwpyozLsGbkZSfmJIXtsRQR6rYtogQeK24Pq/ho817Fbrc/spUG5HCfZMiCrWGQfBsyEM4hDa8rDcsABhLi5eszlrQ15agOHKRaKPXZgyEerrYeuN8JCCyXlc57GNkEpBtRbpU4gs7xmr4MdR3jgTdolLHb8Xn/p3Go7NpW69IABR0jolkipCJ7J9Feygg12sdMX3E160Y18zMEBYB7u7bY3LIvGDj6RtTZDYn1/EfJM3HTgqCW/Ogh2bYboSDSFXagUeL5VUmeGFsoVcOD3ISROgUiWAB2lzP3/SCidNzrGpQ6GMdL9U+DQhuUlBqsiXukJxzmBMj+7ENZ8LAe6h2dSrdO7fuz7sIBjD8o1Cr5mC+hY4tBLerIURMVhIfuAAsQ/+XEBwvObAxInhUnWzE6hGXM+zaB+bGRKHl1LwjS4FyCxJUBziV29CAoSHFthzFkZr6gN0zOlQgURo86HWeEiA0Gf0Lwg5fLlKrqYsh3legdfs0TKYdCSsng0HainS2qkT1+tLcJuefOiNNCSzeZwAHoISB+YqOFGLYX4Nkhu2EWHQd9RKvKVrVCwnYOPD3KsOjO6KKHBYgLBCSoHzfeZXgvzlcyIGCPPwnGXTouZRuBZAN8+Hy6rBrYWRGmaTW8CumLQkDZOj2g0m7eS5EBX3EQT8v6mDx6MArtPcXHVdgDCXeyqGgyBSgDAstYXcU1syAqQF5heGl3B5Ei6pBrdOyqV7EjjmOjA+32ZwPrX0XdO7dxOwVRpO6SobxA+QogQIc6RiADTKNTMFCEtyBHlGQ9TkM33QyXW6LCktYqmDUW7PAse0ZphcyFqeifAeRsIr2ZPiAuQpUS38RndRAoS5cN4MxnTeBnosWlQ+NMs4D9YexVHVGTvk8iT8znh89nSlc0r/HgAMV8FPE3Dq0cVpc+SpVZEYQadskD4SU1ARVZFsh2Rdnp6Z/WgfpFwW5OmogaEBzoSolCltf3g21S5PyVUWQcqkNJyex7V/5EkOk4U9g2jZuEV3riiYmoB/FfE7PFf7mqIDJKhBg8oziqzg74mAgpVM9AKUrO0IhrA0j8EaHsrTpnklQ9p+zOJ8YQHC0Xl0miyPcF8ztNxXLpf/SVKycKmDMldSR/buAeBYmIapk4rgvt6ss8leHWTW7vmiAyTIdenkaSBnqN8Opc/EZVsexUBPyOE9ucDOAPt6gbXveQQIPaqIYHh+nJJA3MiI17w2KTlQXl7UPQE2ZFfTBuAnq+DWKAHAztC24kXta85bcQEyTbJSD8lHFQk5bMvmh+iF5bB+lGkFmcGDBIR70ApJ1eCMl7Lbc7MBsxO5Z/EXoCTouX3r9i8dDSD3JuCn3i8puFQVmGHkwWDmKDjb6/tVdOMGzjei9sP+wSXThQPItuJP9qsBz3YiQPhcWN1ks0iKlyNIpYanAzI0q8FJysCVvGkTVGjY1YFxGv4XXz19hizj0fk28m6SarmGLG+bgXXwQ2h+M5zn7U0NHKwk8bC7qEXB1fPgxuoQZ0Sd2GD7anEg7KagRMFnaaibGOBKz0Z1sK1rGIKCO7MxnkKoWEEBwoLHH4DPVoe0DFK+xEMV4mYdD+M2wdwCcDzCAp3FqH1vkpkYDVncWM+Pl9qGsEYObzlwoucRmg39lOQo9ekmcLwLnJKQNBYb5aVlUtM+ScMY16qdUW3MBkek34XJgIZ8WaTHlcbx0aokBYaiAiTIQE+H10qM7sSCBlYQ1sAQLanrUeyYYgcoi1H7TlwO/RtZ1D03JV6foLLRBkcq5lZaQP4j4tHrDqp14BTvfmZC3xgcr2SswyQtEjMKjSEHgNTBvi582/z658oc4kedaZyQS4CwMwCJfOhVtGq/gpMuYIDQd92o7X2CvHNpBVPsuocUHAWc2k3g+J0Dk+fC6ho4slamQ32ipO3OiUQHBwqeyGENVVriPSXAmhK4OkcmlTulYE86Fh0FBghrYF+nE9FZFV0qFSVA2Jl7jHWu9gQVMTaxClKDRBUbYB2M65KmK7ynwgC3dAMwNmlJDnxKw+Xj5efdO7EusypzcNfXwresWpZrTMlyZMpXgowNQGqhykzbXdYNL5v1S4WlRQgQRj3IRZFcUSWISdD7py11Y/Az+z2lUvu9VxeD4zPgagWTFLyn4Tqk0Xi+9HoJTFXhpmA7mi1ZHr82vz69Cm7NQ83NT73SEQ9JJ/X///gHuxudMu52HNLTXfZHoQOENu2aA5i2Nt+1thXOmGz1up0HO7SIW7qrqZUCecsUvKNhclQJMB0qzAySCmCtA1PzibPkJUECOihmChAW3P7Q4vatiALQYqtXhNS+18HuKr8AoX3ve0RUI05UcJL5zIX+xmytYqBWdANACpL4qOGlOByWlLEZWakanAq4RxkHiYaL8o215AyQOklo8wf9urTFaI4BwmJLkGI0p/AORlZv03SJz3hN0B5ICNfcTDOhr84xhaeH0WOb4PAJOcxGmQA/B/7HrOEtVXB/vl+eM0DMxvs/V5QWo2544VWHAGFV4SsIox7iZ4sIzF3rsqjBFeLf39WoEd8L0KHPpmuyqAtNaeBHCYnhrMvBKD9Hy5oAPLm6rb1o1wCEAPVKFSf+sS4sQBhQVx4I0L7S6aTYM2eKWRwWD2vcDJvLnb1hPNUhFXJnb4HgeNuFI5IyzzCywysFU6xui0tdOK2z+V05AySXCkI61+IzU4vRPaNw8S6oYGwIqn1fAOWqQJJLwZczbN5N4qBiiQO/Dzgwu5F/04Xuoj87MGKiNeUpCtWISvWAYYhvOzA5W4OHggPkBRmO4k9QzNTMIO8KQjdDX6uAPy/oDvsjLLWlSSRXSYG+JrDL+Ww40JvKquE7IROaJmxBwHgLmJyE8zP1yg0Dh2oDx3uONHj4oBA3lZP6sQYOUB29IYHNDGpgF0cQvDZP5M6NqF65Ojx1uagerAySa5gKaHsUgWL4VCodMmRISfM0BTxZBXUh1xuxBQBjEzLH8NpK6TySE6Wkgd5vDLNfDiQKmR2cE0BUsP0ROCXI1P/uWeADqWolXcKmwAChifYXexJToJSrgr8g/3IiUwC2wSd9OkgQk1t0gpG0v8xwyV16MjI0/NOFy/MpkqqWkoPf0jYt+K00HDUpwJvalTZIUAT9kLoI5aGdpWpwaiUSOyKikVzs+IebIcqfF5kUbP/QnGHGXW6rnz8we/d8ph61ujCtPosBjJeAZBUcnw84ZkDpBFGpPHAs1HBYMaoRcwVIUPXZMBfqa+GkBRHKRHOlOtg2BV8fJ7MOfxiVizvFN9CXFsIIDCC/0e/0sWy5OknqO8UctF9mkfif9TBsvAGcXg8jg4awRqGZsF0p1Oq2NfhnI1RWFWkkRDyHG9uD8OGTIzU83CSqzSoKc3AcYIhrQKfCudEvUvCTgL9vX+TNHpoqsDg3tG2Is2G2ea6vInbgu6ulY2AmTr1M9QxgvAf83IG7OzPuuRYO1dKMemdzJm5bBd8tZqluZIDEorfOHGT+dQl1Np2jE1RBF9VV2NJQtzU8uz/CwXgCU4PejarUb/rDP6JW8GUwxi/QkrbeB2jVcEVSbJCiUjyHg3gYPZe0qOYF6wXb0x5u9DSIDYatXZO6rWQueUaqgpdTMirtqC683Y3IuIQ/JUNGgOeoYpdp+L1uq/9fqeG0KqjpIuaUt/2xwYFhGvb0/wuxFYohPeZomOTCgITEaEbgGwSfhdYqOMmBckck0RolA17+1sMw0n8gHGLSfEqB9xIhWQZ+aoWLiNCDthA2GXBpC+yUhDMTBQCHmYU4zwOHGX0xqqvAERkgs6XhwZd9h/P5SngzBpscGGD/U3BgF9z7LAUT+8BLCo6qg6+uhtcaJaK6KuI1bkrAI2k40hV/+r0JmOoU3z2cDzOYhImK6xwOyGR4xxEJsrwIt/UqcI0D+ydhvyT8Lp+ps0FUA2c4sBgzGEnD7atzyOjtUhUrJv2E/GBagOg1tyJNgW1aqGVkV1xJ4+GvG8NqBfB/Gj5WUs9xrM/+XqxhhgJHw2SVuZ77GqCsBV5V0hSOQTKF9c4aeEMZO0jDS440WHOQoSpeJsCDDty/AMo3wpFIa1TP2H9ZSVO1/yj4GjBEQb0LS5WUrG6jYI55/XSk11ItkhOWNjO+v2oY0KcaHlWwlYLXNFQi/1qAx4DFCvbQAuxMnVcmeZIgatMzjyrhpTr4somZnEn+7t9G4BkNs2LSHf21Qh/Ip2BgH7jdsrUagW9VyXzB7mBMkdD8MwVX+fTi46rgiZQ0K7Ar9rYBpiQtD0sNPKygfx+YMg7WPAQlJ8MmMxTyenO9q+vh2uEQXwqtw0ENEs9NZdA9pWHvgfDuWrgCCZodpOB9LSAcjni/Lk3AzS9CfJTM3FO18A/gmH7Q3zMcn4KBcRhbZWqd66B/Gn5QBVfWwmwN40pg6AT4KCWqxPYODE3LvPVZGk6sgsc0KKu1zm1mvPF9SWkT5HliTtdwk4JJCVg8A0qPhuYa+IqCFzMwrVYjGYcgnHRBnpx5kILTgMMRJrV78PLyAfCWlrjMopjEfJZ0xgsV4d6qlEzD3dn86RUNU6uCWz71HICkpLY5YePDge3sbhnWez9xYEQTNJTBPSZYdUgf+HKruG1nAwPScGA5rNwkIvnhJEyphTNNNuYbSfhKSjbygZDbum8VnGvKTZktPavORlIwSoAHknCGuebtwOsJGJWCpIKZwBNJ+FotXKNFvVqehOE1cJaCWxQ8lIBvpGSmxPIkXGye70Xgofnwy/HwvIaXquDcGrhMCSN5JgETa+B7Cn6t4KIE3J6Cb2s4VsEIJWWjc03bz6OBC5Lwx5QE/Q6PYFfsOhneL5ADQNVD/2YY4MLGcmgan70XV0FpNvRzhFF+y5zJtIIbt4Hqznq/iq5imfJWf4LiG5WwslaKoWzjvR+wwbTEPM0K5lw/HhrqoK8y6piCFWugpULUtJvN51+05x0qKfgPo6mD4MiUFPA/1gxzj4ZrUhJI+76C2+fDgE0CuDLgwCehTx/ZAEcbI9eoPNsgI7tAEvy20W0NE1a2wi+nQ8VxIu7fboRbxgl4D1IC5P4KUi4c5Rpu58D+WljxMyYSfqPJY1uSgFSdqFw/BX6qDaNRIuWy0tYBjKkTHFIjgGjojgOYguMU3GZNvn0XOCtR+AlaxQGI8Qxt5VtUL3p9gRYviU0exz/WxtkMKLWr7BwYWiEjvj6rgqdND9vh1sINdYXj+qnFgbGmJv0PwAXABWViuE5EDmhjJSyoFTCWmedYbFSZ4d4zmNynUeb1Z8zfvXvc1vx+2yT4oFZUuRtc+GlMONy1xr18YhKmz4PBm2BkXIC11qS6rG+AlwfCcbQlec4xttshtMWLhqbgEJ0htd2m5s+BO7sOdk5Liv7XDFNwgdscuDLXbN7u9mJ1iH94qegh9RYeeEZbgFpbLnrtH4FpQELB2yZTtUmBTkukfpoBWF8H3lEdRzsDtH4K/9Zi47xr/takxbbwjNkmBVq3pVq0OPATYyOcCegSeHaNOAHKzbMsNN66fW2AJGBxnahB4xEELisT22J3oC4J01NwQQusUHBrWmpEtkaKmbyalrEWw/Hu6TzzvFcpeE/JfUSN/pdtwcAoS8EPjcPja5ZTZGxS0vbX96T7jQKQwBnoM8VzE1SrsKBGDMmhFkCSldDaKiO+DpkPF7ngGI/Q4FoYWQV1CsYZl+HdRvUJmohbPhCmVMEnSQHVzv2gn/FeTQXOAAbVwqFVUKNFZfpSAv6VEkfDQcCbE+BTX4T67QHQqCFpA8S8dpWX5l8H/XXbnO71xiZ4AphcCgdVwaqNAjLHYxb29yhIVoPjiORLNkrnk9c1fCeHfdt5SwRHCo5zRbW9XjRFmoBrVsHBiQInfnaZkZ4SY9DekFYN/+uIa/K6jsKFqcaLZOdHtWjpT1RnDkyVbt9pY7mG6zUsdkRFmYp028ugZXCrEgA0aNhDib1zvPdMGj5ScJ0SabOjhnOtBmLPINV4F3heMg13KZFCB2PUJwWnutKd5EYk0e7HCo7RcJb1vNe5MDMG27lwDLDQgVINNyv4rakZuYf2iZzTNfzNgY+0APYSckhbUfDNhLjRtxSpMVrDr7RlX2r4P8RT+HZPvneVBRy70UWt6Xspp037R6L7WohGploY6cI1ShiHR4uAS5M9xAjvlIrVw/OvvrCk4eSaIneL7AzViGfvUQ3PW+BYruDc+XDwlgKOrF6soA7uvdQzhEhMIsuTehgwJjpwmRZV1js6H2u4IQZ/yqektkerWDXwb9WJ9v29VPTNG1eIpMDOkBnxcApwqc9p86mGG5vgDyZ+tKWucTDNgG1KJcod6z2KPZaa4rDP4QWKqudoeB+g4RtavIb9rZfeVHBTC9wT1O1/S6NQFcuUefaCo2dTeQu8loLKpG9iUzFoFgyLSQ/gKW7HflvzNfy2Hh6vjjbjfcsGSK+BvsWoABXAsym4pxwuHCuxhUI5A1QdfEWL+/wkOrYgWgXcq+DOqPUpnxsVy2SxVvUewS2KWhQ86cANR+Qx7dfk3R2kYZySpMnD6Fg+3QTM1PCPTfCoN/vwc8yAQg30vyiJdvZSz6btkAI1uzWQ1pL68pYJlL5IQAqHqaPZUcFeWrIW9iG4VuQzZA7iI60w4/NgW3QaIL205dA0iA2E05SkixdCNV6PFEbVa5gXh2e3RBdtL0B6qQPNhF3iUiF5IJITNpzwoGIj8L6C911Y6sCrpgJzUTELo3oB0ks9iupgq7ivmfZnkM5l7kYv9VIv9VIv9VIv9VIv9VKxbJA66B/PYpsoaB3X8xoj9whKwW5KOpe4JXBXroPro9ALUNLkK4MOoo9gg9fUYkugWbBjzCocU9KnrFsDkO0i6TNgG1c6jWRLMXkAycHppfbMZXsXFmkzNLNFvEgFnxG4TjqkZO1LO0CKweZsKesXhyO11P5j1u9P3X1P7epByqQ0NEr+1TO9cOhIaWkAUfSJsm60WEfgaLyeTG77lkfL/fPeux0gERc+dKrtF51cqZbbrNLo8J5enaUonfYX97QGCBHOlQ2Qntf2x5eg+KYKKetU0jO1l3w0CT6shRM0nKrhkSoz06PAevowe+SDknai0wLA2rAlrZ2ZP2PPZ5zfowBiekSNtrjfvKTk8ORqpO6kYGQaXjJzCm0bp7QURirYPg2r0/BytmbHdbCzhv1cKI/Bu3Ph5eo80qnnweBW2FtL7lJcywF6Lyl9fNv1a9Og5sh3DjPvXZOGxZOlrWlGSsAMedTiUMxqfGDokUSO+2RasB6MdGJ/NMRY/rKGCg3/bYCXM80imQaxbeEAV5q/bXDh1UnSk5lMnxkMB2rYScvMj/18qk0gQEyd0gFIv+R1JbAwl06QpvfvGFeqMl+wZ8ubM3KIAcbCCfDpZm+Vmd5jT209Lwl3hn2R6fHq1SA0r4L9B8L1Sjp0lDhwYKUMUGEGbFMGV2s4h/bFNY0KbqiEa/2HNAVjFdyoRaq1a3ANnJMUdSbKQfgfZJbd6BD76tGk1UGlFk423Vr8A0ibHdi+EtaEALnMlXvzmM7vktLYmxo4XskkVoCXE3BirTT0vtgcjLSG2j5w3gT4KNMz1cKd1qyM5mbolymjtgYuUnCZ+fXeVrgnBtNM8/DnkhZTrIGDTQeXw33q96sOfKPSN9V3GvQZKPt9iW+QUauGuzbBd/z3NhP6xuAKBd8kvIv+qgQMts9EHezlyjCgr9E+obIFmTR1xcnt1VtVC//GJNw6kHSlk863rc83Kji5D8zbJF1XzqEt42CNA0fFLZE8TrU/XAuy6IsjaGtVs2wQ/AopuwRYNde04KyBoabBc9DE2woNP0tJS50HLXBcgrTMCXI3j1Dw2AuwZ6a+rfNhQK20JU1meY7nrcP3Hd3WBrWDFhAGDrNehwJ7W9ddav2csNbqRdNAe4rvPo5phZ8D38gCers16/PZ0s0dGK/NdytQcfFq7WpenmOB45sKbgthIvu58NgC2MOrNzFtXacTbLfGFZxfJtz5amtf90TmtwzLwtuetsFRA8e6cj76Bry3BLhkkDT9O8cC1N60DXzd4Eqbp+MDzt8vmsVW8z9HfxeuiluLPbb9PvD1lE+VMY2aH/QAYrH8gbRNHG0F6qrBnQ39TKNoDxxvAdOVzDY/C1Nr4Eib0gfNYpyBPIzBLU8gYBuBtPBEwy7rpP45ULWoh62bxcizW3k+h9gE29uH0IV6a8Ovt97/vIanFJQo2MGV3lYZPTAWmlta2nNbe22PQYqc0m1a0+Zr7JdFT9/OBqGGrVNwQwCI7vD6TWlrKrAL51mcfpMyfcpS0h3/D0ZSa3OIFxnpdoJ5/5BGkTrzp0Fsk/S18g7VCuT3tcDJHjPQsq9Xm30dYgC5s7W3/1TSwX1f87nN6r110Ee7MpfQ4/prTE+xmJGkXpfJr8+C671Jt76un32B4zXMVPAfpDOmt+7e+jyBeM0usqTn0LgljmzdVim4PIAbXWZzcuvnIch0posUTPvUoF+JqrKXdwP94MRR0FInashBGo4w3++axdjWlU7sHkiPTZqJUXUwyjUAMS7VUDukWVqceuDQCr6XMKCrFbVt81tjRoI0i21UYV3m9iq4OwcPzATr1xe8mgnTgdJeqwrgBgd+kYZSJXZahbnGZ1kMRr+6OYKOY7HTMQOa6XLdYdY97qBgThounAjLqsExjbX/7F1XwZSEHHZqYLhqAwiOWfNBcsC8sRSLHTjCk641MFDB+W3btPm777TA0Qyc4O1tDRypLIAoY3+YAq6/WuBo0DC6Sg45KXgduMX7WFzaw75hrjHa1wj8N1XwffO5Q2nfkP3GpJmKlhImMMYcwDVxSxxFmapUa6H8AN9rpyes8Wc1YkTZ6kK/tXB/Cvq6sqk7WQvylLnmxbQV/qwHzk6J2NzB12H+g4aQCKsB0mnWn+5JtEmkdpzFTMnaaDa/wbegf6iBHVfDr7NFo/0ODttFWSIbZXsL70vCj8y9bnStLFvD3TJRFDf8C95h7Ssd5m2V6fUyONpTk6rBrZEaEq9qsFHDKSnpUrK97xCtKINF0+R6V/ik5x0p4eT72BJO+IPYk1jNzDX8tqr9qDybuazvJ7YDWmzDfa3PXVtlrZELrzjt72Nr673tPLLNbe1ioX0h4JurpMu+R9tY+/FmPGjhFXxXBxiLlcauMFzHVl9mJH2zAZVw+z7Wn8aHbOh984x6ZaYs2Q8xJeD964Gzwg6tbpsxAdBcAlfaHhCshtiu5SlJwItmDopns5Qp+MUgOGUmHJtpHsdauWZf69nnWfcz3nd/P6dNFx3mWADR2b1RtpRfKme8g5R/J4gZmPu6wl+zrtqveUXImjcpOHssNNXCYdpibiFSDAX1xuBHy8yVzernJp9aaMc/FDzr2ZZa6uBtaX+X71n38v3+vuepwpoUoOAOz1YzUtX+3J3eWTItjGyAvxj3G37A2pVwWya3XqkAKmbdwF1BxnSGjV6lod6BvyZkiAwzRIzuG67i8zrwLwdurpTpR2FGrD3RdZHtFSoVD0iZtaBP+57rBKOenW79+StxMarHZgpwWdInrdo7OOy1XWRzQMfnsnUy+P7N5tmzH59MihMiE9mcecVKM0HLo2rRtffPsOZvAE+2wi1eVNuFAzIk6n0GLFTwwEq4xztDqv2UsCV2Hco82KHFklRu+zUYYe3rMr+TRItqtvmjrcbui8teORYjesz7uVzUK1tqb36tSRhdibUf9fGATZyfbf62hvE2RzCGuJ/sYv9FaTg1Bo19oDHIb10uE44cW4q5sqFrK6AxSrcOY5zvaC+qHYPxqwZxn6vYJGCekRKJdjdtaSNjZsGOGXz79kFc7G1kgOpVG/Y5DR9VyrizQDJd4Pv49fQsLu7x1vuf9O9rFfRtbj9K4YcaHorBuvXQGNLwzd7X1QrGpqGpDD5LwVp/jMqAcHfrPj6xX28Vt6utgtpZGttZh7Xd0KDZsL9tHwEpb398Ae/lnuEeoMl8nBDGG/TaeuAlx3gXhlkLmzWCqdpv7ItBKQ26PQj2icOODnzigUODmt3mbqRUPCBp6/OHu7CxClZ54KiD/rMzDLrc2F6lQ8GY2dCvBgaVwn2+YNQnnnSZDhWzYVcjfklK9/WHfc/TGmLzxHV76bJZvVonvaNs1ave9/Hx1gGYl8VdazMx7Xa8lv++htN+hMMc/3s+FADYteYTYrCpElZ64KiHrefDAOsZ7MDuQBdGNcDH46HBA0fKiohfLc6WmG0b1MDedRCvhfO1j2nF4BXr1099nxvjxWtionl4DLVVGbvO3OO49h7/9m7vsNd8psYzldDqOB0jsxkBskDa+I/KtrFO20gzgL5apsI2p2B1ClbXQpMj6oznB22i/aafGIPlKdhgPtPgQoMjIjKQkrJ5dseNLzkSDV2BGHxzrUVcZvkAv+nAfyvkuzakhDOeZweXw6LoaV9HEZuz++wPHbdUrzrx6OwWoloE6Tv25r1alWXUtW4v1QL31UgUW6od7cL7KWj09qkZ1jVbNoIjk7y09bz3DZJ9bTCf2WDHV0w8Y6ltWyp43YX1Zh6l7blbWdl+pvvjtpKhYEFK7KGFWlJTPC/ld71sghkyemJU0H4YZjY6iGEZSWfPi6kHmdVnL3zT6iyGYqN8QWm2jZ0nQaRHOuKGAeZfqT/w40iA0B+MqzDv72/5tMMkmzYuS38gCWSo5hJrAT62fj434Ps8LXJxOnPKun0QdUt7kNsAec2uDUn7hnXqDBJkGsSULw0ogjfLvq+PkxKDCgLe9+k487Dc2iewak/M6OcbAy7V37y/go61Kr8JMmWN9/Jua/+W+O7tWjpmTNgq4SoFpybaQgOUiB1RZq3zfOt6B9n3Zr82QWyx/n6GEjfeKE8dejqbS1PBSOv9ujSkeXI1uNUwZQJM1eIZ2VtBiRbRvRpJy6j1eclemg0jHHH3TjB9m7whk+8rWKSMCzCDKnK5hhVaAnJDtKRJ3JSAuTVwj2pT8d6xvBoLjW++vwLlwicmPvFEP3gwU8TeNPf21uMNT9IYG+BL1ms1vs8daL227ukMhUGD4UuuqC8NbcwwK23e1xAbEZCRcrNgREzW/HAF25s1X4M4QxbFfCkmSfhRrbQaPQsZX12qoVlLLGwJvjaoSbi7RmzVs5FhR+9peGAe/GW8SBFvT17w3dvaOjgsLVH+4xQMNd/zX+BJDXdPFNXcVtEO0uHrap/dDQ3WeGkzyMh7rbXJTLz6f9D5hRN+hTbvAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDE3LTA4LTEwVDAyOjQ4OjAyKzAwOjAwxWogXQAAACV0RVh0ZGF0ZTptb2RpZnkAMjAxNy0wOC0xMFQwMjo0ODowMiswMDowMLQ3mOEAAAAASUVORK5CYII=', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'GNU General Public License 3.0', 10);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'GNU General Public License 3.0', 10);


INSERT INTO license(id, active, name, "position", url) VALUES (11, false, 'GNU Lesser General Public License 3.0', 11, 'https://www.gnu.org/licenses/lgpl-3.0.en.html');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (11, 'image/png', 11, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'GNU Lesser General Public License 3.0', 11);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'GNU Lesser General Public License 3.0', 11);


INSERT INTO license(id, active, name, "position", url) VALUES (12, false, 'GNU Affero General Public License version 3', 12, 'https://www.gnu.org/licenses/agpl-3.0.en.html');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (12, 'image/png', 12, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'GNU Affero General Public License version 3', 12);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'GNU Affero General Public License version 3', 12);


INSERT INTO license(id, active, name, "position", url) VALUES (13, false, 'MIT License', 13, 'https://opensource.org/licenses/mit-license.php');
INSERT INTO licenseicon(id, contenttype, license_id, content)
VALUES (13, 'image/png', 13, decode('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', 'base64'));
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'MIT License', 13);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'MIT License', 13);


INSERT INTO license(id, active, name, "position", url) VALUES (14, false, 'Open Data Commons Open Database License 1.0', 14, 'https://opendatacommons.org/licenses/odbl/1.0/index.html');
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('en', 'Open Data Commons Open Database License 1.0', 14);
INSERT INTO license_localizedname(locale, text, license_id) VALUES ('pl', 'Open Data Commons Open Database License 1.0', 14);


SELECT setval('license_id_seq', COALESCE((SELECT MAX(id)+1 FROM license), 1), false);
SELECT setval('licenseicon_id_seq', COALESCE((SELECT MAX(id)+1 FROM licenseicon), 1), false);

--------------------------------------------------------------------------------
-- Consent model
--------------------------------------------------------------------------------
create table consent
(
    id serial
        constraint consent_pkey
            primary key,
    name varchar(255)
        constraint consent_name_key
            unique,
    displayorder integer not null default 0,
    hidden boolean not null default false,
    required boolean not null default false
);

create table consentaction
(
    id serial
        constraint consentaction_pkey
            primary key,
    actionoptions json,
    consentactiontype text not null,
    consent_id bigint
        constraint fk_consentaction_consent_id
            references consent
);

create table consentdetails
(
    id serial
        constraint consentdetails_pkey
            primary key,
    text text not null,
    language varchar(255) not null,
    consent_id bigint not null
        constraint fk_consentdetails_consent_id
            references consent
);

create table acceptedconsent
(
    id serial
        constraint acceptedconsent_pkey
            primary key,
    name varchar(255) not null,
    language varchar(255) not null,
    text text not null,
    required boolean not null,
    user_id bigint not null
        constraint fk_acceptedconsent_user_id
            references authenticateduser
);

--------------------------------------------------------------------------------
-- Grant suggestions model
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grantSuggestion (
    id SERIAL NOT NULL,
    grantAgency varchar(255) NOT NULL,
    grantAgencyAcronym varchar(255) NOT NULL,
    fundingProgram varchar(255),
    suggestionname varchar(255) NOT NULL,
    suggestionnamelocale varchar(255) NOT NULL
);

--------------------------------------------------------------------------------
-- Mail domain model
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS maildomainitem (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL,
    processingtype VARCHAR(31) NOT NULL,
    owner_id BIGINT
        CONSTRAINT fk_maildomainitem_owner_id REFERENCES persistedglobalgroup
);

CREATE INDEX index_maildomainitem_owner_id ON maildomainitem (owner_id);

--------------------------------------------------------------------------------
-- Citation model
--------------------------------------------------------------------------------
CREATE TABLE datasetcitationscount (
    id BIGSERIAL NOT NULL,
    dataset_id BIGINT NOT NULL
        CONSTRAINT fk_datasetcitationscount_dataset_id REFERENCES dataset,
    citationscount INT NOT NULL,
    PRIMARY KEY (ID)
);

CREATE INDEX index_datasetcitationscount_dataset_id ON datasetcitationscount (dataset_id);

--------------------------------------------------------------------------------
-- ROR model
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rordata (
    id BIGSERIAL PRIMARY KEY,
    rorid CHAR(9) UNIQUE,
    name VARCHAR,
    city VARCHAR,
    countryname VARCHAR,
    countrycode VARCHAR(16),
    website VARCHAR
);

CREATE TABLE IF NOT EXISTS rordata_namealias (
    rordata_id BIGINT
        CONSTRAINT fk_roralias_rordata_id REFERENCES rordata,
    namealias VARCHAR,
    PRIMARY KEY (rordata_id, namealias)
);

CREATE INDEX index_rordata_namealias_rordata_id ON rordata_namealias(rordata_id);

CREATE TABLE IF NOT EXISTS rordata_acronym (
    rordata_id BIGINT
        CONSTRAINT fk_roracronym_rordata_id REFERENCES rordata,
    acronym VARCHAR,
    PRIMARY KEY (rordata_id, acronym)
);

CREATE INDEX index_rordata_acronym_rordata_id ON rordata_acronym(rordata_id);

CREATE TABLE IF NOT EXISTS rordata_label (
    rordata_id BIGINT
        CONSTRAINT fk_roracronym_rordata_id REFERENCES rordata,
    label VARCHAR,
    code VARCHAR,
    PRIMARY KEY (rordata_id, label, code)
);

CREATE INDEX index_rordata_label_rordata_id ON rordata_label(rordata_id);

--------------------------------------------------------------------------------
-- SAML model
--------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS samlidentityprovider (
    id SERIAL PRIMARY KEY,
    entityid VARCHAR UNIQUE NOT NULL,
    metadataurl VARCHAR NOT NULL,
    displayname VARCHAR NOT NULL,
    configurationxml CHARACTER VARYING,
    lasttimeofxmldownload TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX index_samlidentityprovider_entityid ON samlidentityprovider (entityid);

--------------------------------------------------------------------------------
-- Workflow execution  model
--------------------------------------------------------------------------------
create table workflow_execution
(
    id serial not null
        constraint workflow_execution_pkey
            primary key,
    workflow_id bigint not null,
    trigger_type varchar(255) not null,
    invocation_id varchar(255) not null
        constraint workflow_execution_invocation_id_key
            unique,
    dataset_id bigint not null
        constraint fk_workflow_execution_dataset_id
            references dvobject,
    major_version_number bigint not null,
    minor_version_number bigint not null,
    dataset_externally_released boolean not null,
    description varchar(255) not null,
    started_at timestamp,
    user_id varchar(255),
    ip_address varchar(255),
    finished_at timestamp
);

create table workflow_execution_step
(
    id serial not null
        constraint workflow_execution_step_pkey
            primary key,
    workflow_execution_id bigint not null,
    index integer not null,
    provider_id varchar(255) not null,
    step_type varchar(255) not null,
    description varchar(255) not null,
    started_at timestamp,
    paused_at timestamp,
    resumed_at timestamp,
    resumed_data varchar(255),
    finished_at timestamp,
    finished_successfully boolean,
    rolled_back_at timestamp
);

create table workflow_execution_step_input_params
(
    workflow_execution_step_id bigint not null
        constraint fk_workflow_execution_step_input_params_execution_step_id
            references workflow_execution_step,
    param_key varchar(255) not null,
    param_value varchar(255) not null
);

create table workflow_execution_step_paused_data
(
    workflow_execution_step_id bigint not null
        constraint fk_workflow_execution_step_paused_data_execution_step_id
            references workflow_execution_step,
    param_key varchar(255) not null,
    param_value varchar(255) not null
);

create table workflow_execution_step_output_params
(
    workflow_execution_step_id bigint not null
        constraint fk_workflow_execution_step_output_params_execution_step_id
            references workflow_execution_step,
    param_key varchar(255) not null,
    param_value varchar(255) not null
);

CREATE TABLE  workflowartifact (
    id BIGSERIAL PRIMARY KEY,
    workflow_execution_id INTEGER REFERENCES workflow_execution(id) NOT NULL,
    created_at TIMESTAMP,
    name VARCHAR,
    encoding VARCHAR(64),
    storage_location VARCHAR
);

CREATE TABLE db_storage (
    id UUID PRIMARY KEY,
    stored_data BYTEA
);


