#!/bin/bash

. setenv.sh

echo "### Undeploying Dataverse ###"
$ASADMIN_BIN undeploy dataverse

echo "### Stopping Glassfish ###"
$SERVICE_BIN stop glassfish

echo "### Stopping Solr ###"
$SERVICE_BIN stop solr

