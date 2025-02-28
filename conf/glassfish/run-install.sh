#!/bin/bash

export DATAVERSE_CONTAINER_WORKING_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

echo "* Copying properties"
mkdir /root/.dataverse
cp "${DATAVERSE_CONTAINER_WORKING_DIR}/dataverse.properties" /root/.dataverse/

echo "* Copying default config"
cp "${DATAVERSE_CONTAINER_WORKING_DIR}/default.config" /dataverse/scripts/installer/

echo "* Configuring prometheus metrics export"
cp "${DATAVERSE_CONTAINER_WORKING_DIR}/prometheus_exporter.yaml" /opt/prometheus_javagent
export GLASSFISH_JAVAAGENT="/opt/prometheus_javagent/jmx_prometheus_javaagent-1.1.0.jar=9999\:/opt/prometheus_javagent/prometheus_exporter.yaml"

export WARFILE_LOCATION="${DATAVERSE_CONTAINER_WORKING_DIR}/dataverse.war"
echo "* Running the installer"
cd /dataverse/scripts/installer/
echo | ./install -y;

# During installation the generated folder might get corrupted
echo "* Clear generated state"
rm -rf /usr/local/glassfish4/glassfish/domains/domain1/generated

echo "* All done"