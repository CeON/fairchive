package edu.harvard.iq.dataverse.search.periodo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.search.PeriodoSolrClient;

public class PeriodoIT extends WebappArquillianDeployment {

    @Inject
    private PeriodoIndexingService indexer;
    @Inject
    private PeriodoDataFinder finder;
    @Inject
    @PeriodoSolrClient
    private SolrClient solr;

    @BeforeEach
    void setUp() throws Exception {
        this.indexer.clear();
    }

    @Test
    void importingEmptyStream_doesNothing() throws Exception {
        // verify that core is empty
        assertThat(this.finder.findById("p0f65r29qvb")).isEmpty();
        assertThat(this.finder.find("Late Bronze", 50)).isEmpty();
        assertThat(this.finder.find("Early Bronze", 50)).isEmpty();

        this.indexer.importNames(new ByteArrayInputStream(new byte[0]));

        assertThat(this.finder.findById("p0f65r29qvb")).isEmpty();
        assertThat(this.finder.find("Late Bronze", 50)).isEmpty();
        assertThat(this.finder.find("Early Bronze", 50)).isEmpty();
    }

    @Test
    void importingNullStream_throwsException() throws Exception {
        assertThatThrownBy(() -> this.indexer.importNames(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    void searchingForNull_throwsreturnsEmptyAnswers() throws Exception {
        try (final InputStream in = openTextFile()) {
            this.indexer.importNames(in);
        }

        assertThat(this.finder.findById(null)).isEmpty();
        assertThat(this.finder.find(null, 50)).isEmpty();
    }

    @Test
    void searchingForEmptyValues_returnsEmptyAnswers() throws Exception {
        try (final InputStream in = openTextFile()) {
            this.indexer.importNames(in);
        }

        assertThat(this.finder.findById("")).isEmpty();
        assertThat(this.finder.findById("  ")).isEmpty();

        assertThat(this.finder.find("", 50)).isEmpty();
        assertThat(this.finder.find("  ", 50)).isEmpty();
    }
    
    @Test
    void searchingForSingleLetters_returnsEmptyAnswers() throws Exception {
        try (final InputStream in = openTextFile()) {
            this.indexer.importNames(in);
        }

        assertThat(this.finder.find("p", 50)).isEmpty();
        assertThat(this.finder.find("p  p", 50)).isEmpty();
        assertThat(this.finder.find("p  #p", 50)).isEmpty();
    }

 

    @Test
    void clearingCore_leavesEmptyCore() throws Exception {
        try (final InputStream in = openTextFile()) {
            this.indexer.importNames(in);
        }

        assertThat(this.finder.findById("p0f65r29qvb")).isNotEmpty();
        assertThat(this.finder.find("Late Bronze", 50)).isNotEmpty();
        assertThat(this.finder.find("Early Bronze", 50)).isNotEmpty();

        this.indexer.clear();

        assertThat(this.finder.findById("p0f65r29qvb")).isEmpty();
        assertThat(this.finder.find("Late Bronze", 50)).isEmpty();
        assertThat(this.finder.find("Early Bronze", 50)).isEmpty();
    }
    
    @Test
    void importingNamesIntoUnreachableSolr_throwsException() throws Exception {

        final SolrClient solr = Mockito.mock(SolrClient.class);
        when(solr.addBeans(anyCollection())).thenThrow(RuntimeException.class);
        this.indexer = new PeriodoIndexingService(solr);

        try (final InputStream in = openTextFile()) {
            try {
                this.indexer.importNames(in);
                fail("Exception expected");
            } catch (Exception e) {
                // pass
            }
        }
    }
    
    @Test
    void findingByEmptyString_returnsEmptyList() {
        assertThat(this.finder.find("", 50)).isEmpty();
        assertThat(this.finder.find("  ", 50)).isEmpty();
    }

    @Test
    void gettingByEmptyUrl_returnsEmptyOptional() {
        assertThat(this.finder.getByUrl("")).isEmpty();
        assertThat(this.finder.getByUrl("  ")).isEmpty();
    }

    @Test
    void gettingByImproperUrl_returnsEmptyOptional() {
        // totally wrong url
        assertThat(this.finder.getByUrl("http://google.com/xyz")).isEmpty();
        // url with wrong prefix but proper period identifier
        assertThat(this.finder.getByUrl("http://google.com/p0f65r2")).isEmpty();
        // url with proper prefix but missing period identifier
        assertThat(this.finder.getByUrl("http://google.com/")).isEmpty();
    }

    @Test
    void findingByImproperUrl_returnsEmptyList() {
        // totally wrong url
        assertThat(this.finder.find("http://google.com/x1z", 50)).isEmpty();
        // url with proper prefix but missing period identifier
        assertThat(this.finder.find("http://google.com/", 50)).isEmpty();
    }

    private InputStream openTextFile() {
        return getClass().getResourceAsStream("/json/periodo/periodo-dataset.json");
    }
}
