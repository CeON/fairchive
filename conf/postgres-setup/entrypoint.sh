#!/bin/bash

if ! [ -f postgres-setup-done ]; then
  echo "Running db installer."

  echo "* Running the db installer"
  echo | /fairchive/scripts/installer/install --pg_only -y;

  touch postgres-setup-done
  echo "DB install done"
fi
