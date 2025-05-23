#!/bin/bash

echo "------------------------------------------------"
echo "Sourcing ~/.bashrc"
source ~/.bashrc
echo "------------------------------------------------"

if [ ! -d "/root/.dataverse" ]; then
  echo "Fairchive has not been installed yet."
  tail -f /dev/null
else
  echo "------------------------------------------------"
  echo "Starting glassfish..."
  echo "------------------------------------------------"
  /usr/local/glassfish4/bin/asadmin start-domain --debug
  tail -F /usr/local/glassfish4/glassfish/domains/domain1/logs/server.log
fi
