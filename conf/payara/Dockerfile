FROM payara/server-full:5.2022.5

ENV PAYARA_SETUP_DIR="/payara-setup" \
    PROMETHEUS_DIR="${HOME_DIR}/prometheus_javagent" \
    ASADMIN_OPTS="--user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE}" \
    ACTIVEMQ_USER="activemq" \
    ACTIVEMQ_PASS="activemq" \
    DB_PORT="5432" \
    DB_HOST="postgres" \
    DB_NAME="dataverse" \
    DB_USER="dataverse" \
    DB_PASS="dataverse" \
    RSERVE_HOST="rserve" \
    RSERVE_PORT="6311" \
    RSERVE_USER="rserve" \
    RSERVE_PASS="rserve" \
    DOI_USERNAME="doi" \
    DOI_PASSWORD="doi" \
    DOI_BASEURL= \
    HOST_ADDRESS="localhost" \
    SMTP_SERVER="smtp" \
    SMTP_SERVER_PORT="1025" \
    FILES_DIR="${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/files"

# Install wget and fetch dependencies
USER root
RUN apt-get update && apt-get install -y wget vim
RUN mkdir -p ${PAYARA_SETUP_DIR}
WORKDIR ${PAYARA_SETUP_DIR}
RUN wget https://github.com/prometheus/jmx_exporter/releases/download/1.1.0/jmx_prometheus_javaagent-1.1.0.jar \
    && echo "Downloaded prometheus javaagent"

# Payara setup
USER payara
WORKDIR ${HOME_DIR}


RUN mkdir -p ${HOME_DIR}/.dataverse \
    && mkdir -p ${PROMETHEUS_DIR} \
    && cp ${PAYARA_SETUP_DIR}/jmx_prometheus_javaagent*.jar ${PROMETHEUS_DIR}/

ADD dataverse.properties ${HOME_DIR}/.dataverse/
ADD init_0_preboot_setup.sh ${SCRIPT_DIR}/
ADD postboot_setup.sh ${SCRIPT_DIR}/

# Default payara ports to expose
# 4848: admin console
# 9009: debug port (JPDA)
# 8080: http
# 8181: https
EXPOSE 4848 9009 8080 8181

ENTRYPOINT ["/tini", "--"]
CMD "${SCRIPT_DIR}/entrypoint.sh"
