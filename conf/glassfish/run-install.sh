#!/bin/bash

DATAVERSE_CONTAINER_WORKING_DIR="/dataverse/dataverse-dist/target/container"

echo "* Copying properties"
mkdir /root/.dataverse
cp ${DATAVERSE_CONTAINER_WORKING_DIR}/dataverse.properties /root/.dataverse/

cd /dataverse/scripts/installer
echo "* Copying default config"
cp ${DATAVERSE_CONTAINER_WORKING_DIR}/default.config ./

export WARFILE_LOCATION="${DATAVERSE_CONTAINER_WORKING_DIR}/dataverse.war"
echo "* Running the installer"
echo | ./install -y;

# During installation the generated folder might get corrupted
echo "* Clear generated state"
rm -rf /usr/local/glassfish4/glassfish/domains/domain1/generated

echo "* All done"