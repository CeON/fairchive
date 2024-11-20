#!/bin/bash

if [ ! -d "/root/.dataverse" ]; then
  echo "Dataverse has not been installed yet."
  tail -f /dev/null
else
  echo "Starting glassfish..."
  /usr/local/glassfish4/bin/asadmin start-domain --debug
  tail -F /usr/local/glassfish4/glassfish/domains/domain1/logs/server.log
fi
