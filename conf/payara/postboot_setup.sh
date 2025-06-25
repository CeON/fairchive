#!/bin/bash

set -e

if [ -f "${HOME_DIR}/postboot-setup-done" ]; then
  echo "Postboot setup already done, skipping."
  exit 0
fi

(
  echo "List domains before setup:"
  asadmin list-domains

  while ! asadmin list-domains | grep -q "${DOMAIN_NAME} running"; do
    echo "Waiting for Payara domain to be available..."
    sleep 5
  done

  echo "${DOMAIN_NAME} is running."


  if [ -z "$CONTAINER_WORKING_DIR" ]
   then
    echo "You must specify container working directory."
    exit 1
  fi

  if [ -z "$DB_NAME" ]
   then
    echo "You must specify database name (DB_NAME)."
    echo "PLEASE NOTE THAT YOU (THE HUMAN USER) SHOULD NEVER RUN THIS SCRIPT DIRECTLY!"
    echo "IT SHOULD ONLY BE RUN BY OTHER SCRIPTS."
    exit 1
  fi

  if [ -z "$DB_PORT" ]
   then
    echo "You must specify database port (DB_PORT)."
    exit 1
  fi

  if [ -z "$DB_HOST" ]
   then
    echo "You must specify database host (DB_HOST)."
    exit 1
  fi

  if [ -z "$DB_USER" ]
   then
    echo "You must specify database user (DB_USER)."
    exit 1
  fi

  if [ -z "$DB_PASS" ]
   then
    echo "You must specify database password (DB_PASS)."
    exit 1
  fi

  if [ -z "$RSERVE_HOST" ]
   then
    echo "You must specify Rserve host (RSERVE_HOST)."
    exit 1
  fi

  if [ -z "$RSERVE_PORT" ]
   then
    echo "You must specify Rserve port (RSERVE_PORT)."
    exit 1
  fi

  if [ -z "$RSERVE_USER" ]
   then
    echo "You must specify Rserve user (RSERVE_USER)."
    exit 1
  fi

  if [ -z "$RSERVE_PASS" ]
   then
    echo "You must specify Rserve password (RSERVE_PASS)."
    exit 1
  fi

  if [ -z "$SMTP_SERVER" ]
   then
    echo "You must specify smtp server (SMTP_SERVER)."
    exit 1
  fi

  if [ -z "$HOST_ADDRESS" ]
   then
    echo "You must specify host address (HOST_ADDRESS)."
    exit 1
  fi

  if [ -z "$FILES_DIR" ]
   then
    echo "You must specify files directory (FILES_DIR)."
    exit 1
  fi

  # alias passwords
  for alias in "rserve_password_alias ${RSERVE_PASS}" "doi_password_alias ${DOI_PASSWORD}" "db_password_alias ${DB_PASS}"
  do
    set -- $alias
    echo "AS_ADMIN_ALIASPASSWORD=$2" > /tmp/$1.txt
    cat "${PASSWORD_FILE}" >> /tmp/$1.txt
    asadmin --user=${ADMIN_USER} --passwordfile /tmp/$1.txt create-password-alias $1
    rm /tmp/$1.txt
  done

  echo "* Setup of metrics exporter"
  cp "${CONTAINER_WORKING_DIR}/prometheus_exporter.yaml" ${PROMETHEUS_DIR}/
  asadmin $ASADMIN_OPTS create-jvm-options "\-javaagent\:${PROMETHEUS_DIR}/jmx_prometheus_javaagent-1.1.0.jar=9999\:${PROMETHEUS_DIR}/prometheus_exporter.yaml"

  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.directory=${FILES_DIR}"
  # Rserve-related JVM options:
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.host=${RSERVE_HOST}"
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.port=${RSERVE_PORT}"
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.user=${RSERVE_USER}"
  asadmin $ASADMIN_OPTS create-jvm-options '\-Ddataverse.rserve.password=${ALIAS=rserve_password_alias}'
  # Data Deposit API options
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.fqdn=${HOST_ADDRESS}"
  # password reset token timeout in minutes
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.auth.password-reset-timeout-in-minutes=60"

  # DataCite DOI Settings
  # (we can no longer offer EZID with their shared test account)
  # jvm-options use colons as separators, escape as literal
  DOI_BASEURL_ESC=`echo $DOI_BASEURL | sed -e 's/:/\\\:/'`
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddoi.username=${DOI_USERNAME}"
  asadmin $ASADMIN_OPTS create-jvm-options '\-Ddoi.password=${ALIAS=doi_password_alias}'
  asadmin $ASADMIN_OPTS create-jvm-options "\-Ddoi.baseurlstring=$DOI_BASEURL_ESC"

  asadmin $ASADMIN_OPTS create-jvm-options "-Ddataverse.timerServer=true"

  # enable comet support
  asadmin $ASADMIN_OPTS set \
    server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled="true"

  asadmin $ASADMIN_OPTS delete-connector-connection-pool \
    --cascade=true \
    jms/__defaultConnectionFactory-Connection-Pool

  # http://docs.oracle.com/cd/E19798-01/821-1751/gioce/index.html
  asadmin $ASADMIN_OPTS create-connector-connection-pool \
    --steadypoolsize 1 \
    --maxpoolsize 250 \
    --poolresize 2 \
    --maxwait 60000 \
    --raname jmsra \
    --transactionsupport "NoTransaction" \
    --connectiondefinition javax.jms.QueueConnectionFactory \
    jms/IngestQueueConnectionFactoryPool

  # http://docs.oracle.com/cd/E18930_01/html/821-2416/abllx.html#giogt
  asadmin $ASADMIN_OPTS create-connector-resource \
    --poolname jms/IngestQueueConnectionFactoryPool \
    --description "ingest connector resource" \
    jms/IngestQueueConnectionFactory

  # http://docs.oracle.com/cd/E18930_01/html/821-2416/ablmc.html#giolr
  asadmin $ASADMIN_OPTS create-admin-object \
    --restype javax.jms.Queue \
    --raname jmsra \
    --description "sample administered object" \
    --property Name=DataverseIngest \
    jms/DataverseIngest

  # so we can front with apache httpd ( ProxyPass / ajp://localhost:8009/ )
  asadmin $ASADMIN_OPTS create-network-listener \
    --protocol http-listener-1 \
    --listenerport 8009 \
    --jkenabled true \
    jk-connector

  # create ActiveMQ resource adapter thread pool
  asadmin $ASADMIN_OPTS create-threadpool \
    --minthreadpoolsize 2 \
    --maxthreadpoolsize 8 \
    "activemq-thread-pool"

  # deploy ActiveMQ resource adapter definition
  asadmin $ASADMIN_OPTS deploy \
    --name "activemq-rar" \
    --type rar \
    "${CONTAINER_WORKING_DIR}/activemq/activemq-rar-5.14.5.rar"

  # create ActiveMQ resource adapter
  asadmin $ASADMIN_OPTS create-resource-adapter-config \
    --threadpoolid "activemq-thread-pool" \
    --property ServerUrl="vm\://localhost\:61616":BrokerXmlConfig="xbean\:file\:${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/activemq.xml":UserName="${ACTIVEMQ_USER}":Password="{$ACTIVEMQ_PASS}" \
    "activemq-rar"

  # create ActiveMQ connection factory resource
  asadmin $ASADMIN_OPTS create-connector-connection-pool \
    --raname "activemq-rar" \
    --connectiondefinition "javax.jms.ConnectionFactory" \
    --ping true \
    --steadypoolsize 2 \
    --maxpoolsize 8 \
    --poolresize 1 \
    --idletimeout 300 \
    --maxwait 60000 \
    --isconnectvalidatereq true \
    --transactionsupport "NoTransaction" \
    "jms/activemqConnectionPool"

  # create ActiveMQ connection resource
  asadmin $ASADMIN_OPTS create-connector-resource \
    --poolname "jms/activemqConnectionPool" \
    --enabled true \
    "jms/activemqConnection"

  # create ActiveMQ queue resource
  asadmin $ASADMIN_OPTS create-admin-object \
    --raname "activemq-rar" \
    --restype "javax.jms.Queue" \
    --enabled true \
    --property PhysicalName="dataverseWorkflow" \
    "jms/queue/dataverseWorkflow"

  # allow ActiveMQ to serialize and deserialize data
  # http://activemq.apache.org/objectmessage.html
  asadmin $ASADMIN_OPTS create-jvm-options \
    -Dorg.apache.activemq.SERIALIZABLE_PACKAGES="*"

  # configure ActiveMQ web console
  # make ActiveMQ web console configurable by system properties
  asadmin $ASADMIN_OPTS create-jvm-options \
    -Dwebconsole.type="properties"
  # point ActiveMQ web console to the broker
  asadmin $ASADMIN_OPTS create-jvm-options \
    -Dwebconsole.jms.url="tcp\://localhost\:61616"
  # point ActiveMQ web console to application server JMX endpoint
  asadmin $ASADMIN_OPTS create-jvm-options \
    -Dwebconsole.jmx.url="service\:jmx\:rmi\:///jndi/rmi\://localhost\:8686/jmxrmi"

  # deploy ActiveMQ web console
  asadmin $ASADMIN_OPTS deploy \
    --name "activemq-console" \
    --type war \
    --contextroot "/activemq-console" \
    "${CONTAINER_WORKING_DIR}/activemq/activemq-web-console-5.14.5.war"

  asadmin $ASADMIN_OPTS create-jdbc-connection-pool \
    --restype javax.sql.DataSource \
    --datasourceclassname org.postgresql.ds.PGPoolingDataSource \
    --property create=true:User=$DB_USER:PortNumber=$DB_PORT:databaseName=$DB_NAME:ServerName=$DB_HOST \
    dvnDbPool

  asadmin $ASADMIN_OPTS set \
    resources.jdbc-connection-pool.dvnDbPool.property.password='${ALIAS=db_password_alias}'

  ###
  # Create data sources
  asadmin $ASADMIN_OPTS create-jdbc-resource --connectionpoolid dvnDbPool jdbc/VDCNetDS

  ###
  # Set up the data source for the timers
  asadmin $ASADMIN_OPTS set \
    configs.config.server-config.ejb-container.ejb-timer-service.timer-datasource=jdbc/VDCNetDS

  asadmin $ASADMIN_OPTS create-jvm-options \
    "\-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"

  asadmin $ASADMIN_OPTS create-javamail-resource \
    --mailhost "$SMTP_SERVER" \
    --mailuser "dataversenotify" \
    --fromaddress "do-not-reply@${HOST_ADDRESS}" \
    --property mail.smtp.port="${SMTP_SERVER_PORT}" \
    mail/notifyMailSession

  echo "SUCCESS" > "${HOME_DIR}/postboot-setup-done"
) > "${HOME_DIR}/postboot-setup.log" 2>&1

