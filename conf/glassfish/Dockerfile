FROM rockylinux:9

RUN dnf -y --enablerepo=crb install perl-File-Copy-Recursive

RUN yum install -y \
    cronie \
    git \
    java-1.8.0-openjdk-devel \
    nc \
    perl \
    postgresql \
    unzip \
    wget \
    epel-release \
    jq

ENV GLASSFISH_DIRECTORY "/usr/local/glassfish4"
ENV HOST_DNS_ADDRESS "localhost"

RUN chmod g=u /etc/passwd && \
    mkdir -p /home/glassfish && \
    chgrp -R 0 /home/glassfish && \
    chmod -R g=u /home/glassfish

WORKDIR /home/glassfish

RUN wget https://download.oracle.com/glassfish/4.1.2/release/glassfish-4.1.2.zip \
    && echo "Downloaded glassfish zip"
RUN wget https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar \
    && echo "Downloaded postgresql lib"
RUN wget https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar \
    && echo "Downloaded commons-logging lib"
RUN wget https://github.com/prometheus/jmx_exporter/releases/download/1.1.0/jmx_prometheus_javaagent-1.1.0.jar \
    && echo "Downloaded prometheus javaagent"

# Install Glassfish
RUN unzip glassfish-*.zip \ 
    && cp postgresql-*.jar glassfish4/glassfish/domains/domain1/lib \
    && cp commons-logging-*.jar glassfish4/glassfish/modules \
    && mkdir -p /opt/prometheus_javagent \
    && cp jmx_prometheus_javaagent*.jar /opt/prometheus_javagent \
    && chgrp -R 0 glassfish4 \
    && chmod -R g=u glassfish4 \
    && mv glassfish4 /usr/local \
    && echo "Done installing and patching Glassfish"

EXPOSE      8080
EXPOSE      4848
EXPOSE      9009

ADD entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
