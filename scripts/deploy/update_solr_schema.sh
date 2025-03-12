#!/bin/bash

. setenv.sh

if [ "$(whoami)" != "solr" ]; then
        echo "Script must be run as user: solr"
        exit -1
fi

wget -O $SOLR_CONFIG_XML $SOLR_CONFIG_XML_HTTP_DOWNLOAD
wget -O $SCHEMA_XML $SCHEMA_XML_HTTP_DOWNLOAD

cp $SCHEMA_XML $SOLR_COLLECTION_DIR/conf/
cp $SOLR_CONFIG_XML $SOLR_COLLECTION_DIR/conf/

