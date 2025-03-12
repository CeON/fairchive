#!/bin/bash

export SCRIPTS_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export SCRIPTS_DOWNLOAD_DIR=$HOME/download

export DATAVERSE_MODULE="dataverse/dataverse-webapp"

# RELEASE VERSION
#export DATAVERSE_VERSION=1.0.0
#export DATAVERSE_WAR_NAME=dataverse-webapp-$DATAVERSE_VERSION.war
#export DATAVERSE_HTTP_DOWNLOAD=https://maven.ceon.pl/artifactory/drodb-releases/pl/edu/icm/$DATAVERSE_MODULE/$DATAVERSE_VERSION/$DATAVERSE_WAR_NAME

# SNAPSHOT VERSION
export DATAVERSE_VERSION=1.1.0-SNAPSHOT
export DATAVERSE_WAR_NAME=dataverse-webapp-1.1.0-20250312.014933-20.war
export DATAVERSE_HTTP_DOWNLOAD=https://maven.ceon.pl/artifactory/drodb-snapshots/pl/edu/icm/$DATAVERSE_MODULE/$DATAVERSE_VERSION/$DATAVERSE_WAR_NAME


export SCHEMA_XML_HTTP_DOWNLOAD=https://raw.githubusercontent.com/CeON/dataverse/develop/conf/solr/7.3.1/schema.xml
export SOLR_CONFIG_XML_HTTP_DOWNLOAD=https://raw.githubusercontent.com/CeON/dataverse/develop/conf/solr/7.3.1/solrconfig.xml

export DATAVERSE_ORIGINAL_WAR=$SCRIPTS_DOWNLOAD_DIR/$DATAVERSE_WAR_NAME
export DATAVERSE_WAR=$SCRIPTS_DOWNLOAD_DIR/dataverse.war
export SCHEMA_XML=$SCRIPTS_DOWNLOAD_DIR/schema.xml
export SOLR_CONFIG_XML=$SCRIPTS_DOWNLOAD_DIR/solrconfig.xml

export ASADMIN_BIN="/opt/glassfish/bin/asadmin"
export GLASSFISH_DOMAIN_DIR=/opt/glassfish/glassfish/domains/domain1
export SOLR_DIR=/opt/solr
export SOLR_COLLECTION_DIR=$SOLR_DIR/server/solr/collection1

export LOGIN_AS_GLASSFISH="sudo /bin/su - glassfish -s /bin/bash"
export LOGIN_AS_SOLR="sudo /bin/su - solr -s /bin/bash"
export SERVICE_BIN="sudo /bin/systemctl"

export DATAVERSE_API_URL=http://localhost:8080/api
export UNBLOCK_KEY=BYvB7FCE-TuzYdTAN

