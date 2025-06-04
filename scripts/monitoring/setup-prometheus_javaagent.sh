#!/bin/bash

set -e

INSTALL_DIR="/opt/prometheus_javagent"
GLASSFISH_DIR="/opt/glassfish"
METRICS_EXPORT_PORT=9999
PROMETHEUS_JAVAAGENT_DOWNLOAD_URL=https://github.com/prometheus/jmx_exporter/releases/download/1.1.0/jmx_prometheus_javaagent-1.1.0.jar
PROMETHEUS_EXPORTER_CONFIG_URL=https://raw.githubusercontent.com/CeON/fairchive/refs/heads/develop/conf/prometheus/prometheus_exporter.yaml

GLASSFISH_JAVAAGENT="\-javaagent\:${INSTALL_DIR}/$(basename ${PROMETHEUS_JAVAAGENT_DOWNLOAD_URL})=${METRICS_EXPORT_PORT}\:${INSTALL_DIR}/$(basename ${PROMETHEUS_EXPORTER_CONFIG_URL})"

(

  usage() {
    echo "Usage: $(basename $0) [command]"
    echo "Commands: "
    echo " download                 Download the prometheus javaagent"
    echo " install                  Install the javaagent configuration"
    echo " uninstall                Uninstall the javaagent from glassfish"
    echo " show                     Display javaagent configuration"
    echo " -h|help"
    exit
  }

  download() {
    mkdir -p ${INSTALL_DIR}
    cd $INSTALL_DIR
    wget ${PROMETHEUS_JAVAAGENT_DOWNLOAD_URL} && echo "Downloaded prometheus javaagent"
    wget ${PROMETHEUS_EXPORTER_CONFIG_URL} && echo "Downloaded exporter configuration"
  }

  install() {
    ${GLASSFISH_DIR}/bin/asadmin create-jvm-options "$GLASSFISH_JAVAAGENT"
    echo "You need to restart glassfish for the changes to take effect."
  }

  uninstall() {
    ${GLASSFISH_DIR}/bin/asadmin delete-jvm-options "$GLASSFISH_JAVAAGENT"
    echo "You need to restart glassfish for the changes to take effect."
  }

  show() {
    echo "INSTALL_DIR: ${INSTALL_DIR}"
    echo "GLASSFISH_DIR: ${GLASSFISH_DIR}"
    echo "METRICS_EXPORT_PORT: ${METRICS_EXPORT_PORT}"
    echo "PROMETHEUS_JAVAAGENT_DOWNLOAD_URL: ${PROMETHEUS_JAVAAGENT_DOWNLOAD_URL}"
    echo "PROMETHEUS_EXPORTER_CONFIG_URL: ${PROMETHEUS_EXPORTER_CONFIG_URL}"
    echo "GLASSFISH_JAVAAGENT: ${GLASSFISH_JAVAAGENT}"
  }

  if [ $# -eq 0 ]; then
    usage
    exit
  fi


  while [[ $# -gt 0 ]]
  do
    key="$1"
    case $key in
      download)
        shift
        download
        exit
        ;;
      install)
        shift
        install
        exit
        ;;
      uninstall)
        shift
        uninstall
        exit
        ;;
      show)
        shift
        show
        exit
        ;;
      -h|--help)
        usage
        exit
        ;;
      *)
        usage
        exit
        ;;
    esac
  done
)