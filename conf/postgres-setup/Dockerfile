FROM rockylinux:9

RUN yum install -y \
    cronie \
    git \
    nc \
    perl \
    postgresql \
    unzip \
    wget \
    epel-release \
    jq

WORKDIR /

ADD default.config /
ADD entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]