#!/bin/bash

set -e
. setenv.sh

echo "### Starting Solr ###"
$SERVICE_BIN start solr

echo "### Starting Glassfish ###"
$SERVICE_BIN start glassfish

echo "### Downloading Dataverse war ###"
. download_dataverse.sh

echo "### Deploying Dataverse ###"
$ASADMIN_BIN deploy --force $DATAVERSE_WAR

. reindex_solr.sh

