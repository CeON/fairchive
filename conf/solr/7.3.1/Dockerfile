FROM solr:7.3.1

ADD --chmod=444 https://repo1.maven.org/maven2/org/locationtech/jts/jts-core/1.19.0/jts-core-1.19.0.jar /opt/solr/server/solr-webapp/webapp/WEB-INF/lib/

USER root

ADD schema.xml ./
ADD solrconfig.xml ./
RUN cp -r server/solr/configsets/_default server/solr/collection1 \
    && mv schema.xml server/solr/collection1/conf/ \
    && mv solrconfig.xml server/solr/collection1/conf/ \
    && echo "Configured collection1"

ADD ror/schema.xml ./
ADD ror/solrconfig.xml ./
RUN cp -r server/solr/configsets/_default server/solr/rorSuggestions \
    && mv schema.xml server/solr/rorSuggestions/conf/ \
    && mv solrconfig.xml server/solr/rorSuggestions/conf/ \
    && echo "Configured rorSuggestions"

USER solr

RUN ./bin/solr start \
    && ./bin/solr create_core -c collection1 -d server/solr/collection1/conf \
    && ./bin/solr create_core -c rorSuggestions -d server/solr/rorSuggestions/conf \
    && echo "Done creating collections"

