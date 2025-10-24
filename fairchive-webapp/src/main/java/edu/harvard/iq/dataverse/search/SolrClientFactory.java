package edu.harvard.iq.dataverse.search;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SolrHostColonPort;

import java.io.IOException;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * CDI compliant factory of {@link SolrClient} objects.
 * 
 * @author madryk
 */
public class SolrClientFactory {

    @Inject
    private SettingsServiceBean settings;

    @Produces
    public SolrClient produceSolrClient() throws IOException {
        return build("collection1");
    }

    @Produces
    @RorSolrClient
    public SolrClient produceRorSolrClient() {
        return build("rorSuggestions");
    }

    @Produces
    @GeoNameSolrClient
    public SolrClient produceGeoNameSolrClient() {
        return build("geonames");
    }

    @Produces
    @PeriodoSolrClient
    public SolrClient producePeriodoSolrClient() {
        return build("periodo");
    }

    public void disposeSolrClient(@Disposes SolrClient solrClient) throws IOException {
        solrClient.close();
    }

    private SolrClient build(final String path) {
        final String url = "http://" + this.settings.getValueForKey(SolrHostColonPort)
                + "/solr/" + path;
        return new HttpSolrClient.Builder(url).build();
    }
}
