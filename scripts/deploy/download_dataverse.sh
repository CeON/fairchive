#!/bin/bash

. setenv.sh

mkdir -p $SCRIPTS_DOWNLOAD_DIR

if [[ -f ${DATAVERSE_WAR} ]]; then
  rm "${DATAVERSE_WAR}"
fi

if [[ -f ${DATAVERSE_ORIGINAL_WAR} ]]; then
  echo "$DATAVERSE_HTTP_DOWNLOAD already downloaded to ${DATAVERSE_ORIGINAL_WAR}."
else
  echo "Downloading $DATAVERSE_HTTP_DOWNLOAD to ${DATAVERSE_ORIGINAL_WAR}."
  wget -O $DATAVERSE_ORIGINAL_WAR "$DATAVERSE_HTTP_DOWNLOAD"
fi

cp $DATAVERSE_ORIGINAL_WAR ${DATAVERSE_WAR}

