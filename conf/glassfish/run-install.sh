#!/bin/bash

echo "* Copying properties"
mkdir /root/.dataverse
cp /dataverse/conf/glassfish/dataverse.properties /root/.dataverse/

cd /dataverse/scripts/installer
echo "* Copying default config"
cp /dataverse/conf/glassfish/default.config ./

echo "* Running the installer"
echo | ./install -y;

# During installation the generated folder might get corrupted
echo "* Clear generated state"
rm -rf /usr/local/glassfish4/glassfish/domains/domain1/generated

echo "* All done"