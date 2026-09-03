#!/bin/bash

set -e
. setenv.sh

echo "### ReIndex Solr step 1 ###"
reindex_step1_resp=$(curl -X DELETE "$DATAVERSE_API_URL/admin/index/timestamps?unblock-key=$UNBLOCK_KEY")

echo "status: $reindex_step1_resp"
if [[ $reindex_step1_resp != *"\"status\":\"OK\","* ]]; then
        echo "### ReIndex step 1 failed!!! ###"
        exit 1
fi

echo "### ReIndex Solr step 2 ###"
reindex_step2_resp=$(curl "$DATAVERSE_API_URL/admin/index/continue?unblock-key=$UNBLOCK_KEY")
echo "status: $reindex_step2_resp"
if [[ $reindex_step2_resp != *"\"status\":\"OK\","* ]]; then
        echo "### ReIndex step 2 failed!!! ###"
        exit 1
fi
