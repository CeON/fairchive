#!/bin/bash

if [ -f "${HOME_DIR}/preboot-setup-done" ]; then
  echo "Preboot setup already done, skipping."
else
  set -e

  DOMAIN_FILES_SRC=${CONTAINER_WORKING_DIR}/glassfish/domain
  DOMAIN_FILES_DST=${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}
  echo "Copying domain files from ${DOMAIN_FILES_SRC} to ${DOMAIN_FILES_DST}..."

  cp -ur ${DOMAIN_FILES_SRC}/* ${DOMAIN_FILES_DST}/

  MODULES_SRC=${CONTAINER_WORKING_DIR}/glassfish/modules
  MODULES_DST=${PAYARA_DIR}/glassfish/modules
  # TODO: To be verified which modules need to be overridden on the new platform
  #echo "Copying module files from ${MODULES_SRC} to ${MODULES_DST}..."
  #cp -ur ${MODULES_SRC}/* ${MODULES_DST/

  echo "Done preboot setup."

  echo "SUCCESS" > "${HOME_DIR}/preboot-setup-done"
fi

