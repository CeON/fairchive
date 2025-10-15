package edu.harvard.iq.dataverse.search.geonames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.search.GeoNameSolrClient;

public class GeoNamesIT extends WebappArquillianDeployment {

    @Inject
    private GeoNameIndexingService indexer;
    @Inject
    private GeoNameDataFinder finder;
    @Inject
    @GeoNameSolrClient
    private SolrClient solr;

    @BeforeEach
    void setUp() throws Exception {
        this.indexer.clear();
    }

    @Test
    void importingEmptyStream_doesNothing() throws Exception {
        // verify that core is empty
        assertThat(this.finder.findById("752942")).isEmpty();
        assertThat(this.finder.find("Poraj", 50)).isEmpty();
        assertThat(this.finder.find("Yorks", 50)).isEmpty();

        this.indexer.importNames(new ByteArrayInputStream(new byte[0]));

        assertThat(this.finder.findById("752942")).isEmpty();
        assertThat(this.finder.find("Poraj", 50)).isEmpty();
        assertThat(this.finder.find("Yorks", 50)).isEmpty();
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
    void importingDataIntoCore_allowsSearching() throws Exception {
        // verify that core is empty
        assertThat(this.finder.findById("752942")).isEmpty();

        try (final InputStream in = openTextFile()) {
            this.indexer.importNames(in);
        }

        Optional<GeoName> geoName = this.finder.findById("752942");
        assertThat(geoName).isNotEmpty();
        assertThat(geoName.get().getName()).isEqualTo("Poraj");
        assertThat(geoName.get().getAlternateNames()).isEqualTo("Kolonia Poraj, Poraj");
        assertThat(geoName.get().getHierarchy())
                .isEqualTo("PL - Lublin Voivodeship - Powiat hrubieszowski - Poraj");
        // search by name
        List<GeoName> geoNames = this.finder.find("Poraj", 50);
        assertThat(geoNames).isNotEmpty();
        assertThat(geoNames).anyMatch(gn -> gn.getName().equals("Poraj"));
        assertThat(this.finder.find("Yorks", 50)).isNotEmpty();
        // searhc by name - case insensitive
        geoNames = this.finder.find("poRaj", 50);
        assertThat(geoNames).isNotEmpty();
        assertThat(geoNames).anyMatch(gn -> gn.getName().equals("Poraj"));
        assertThat(this.finder.find("yorks", 50)).isNotEmpty();     
        // search by alternative name
        assertThat(this.finder.find("Predocin", 50))
                .anyMatch(gn -> gn.getName().equals("Prędocin"));
        // search by name prefix
        assertThat(this.finder.find("Predoc", 50))
            .anyMatch(gn -> gn.getName().equals("Prędocin"));
        // search by alternative name - case insensitive
        assertThat(this.finder.find("pRedocin", 50))
                .anyMatch(gn -> gn.getName().equals("Prędocin"));
        // search by alternate name prefix
        assertThat(this.finder.find("Prendo", 50))
            .anyMatch(gn -> gn.getName().equals("Prędocin"));
        //search multiple words
        assertThat(this.finder.find("Jezioro Zygmunta Augusta", 50))
                .anyMatch(gn -> gn.getName().equals("Jezioro Zygmunta Augusta"));
        //search multiple words - case insensitive
        assertThat(this.finder.find("jezioro zygmunta augusta", 50))
                .anyMatch(gn -> gn.getName().equals("Jezioro Zygmunta Augusta"));
        //search by name and proper featureCode
        assertThat(this.finder.find("Predocin #PPL", 50))
                .anyMatch(gn -> gn.getName().equals("Prędocin"));
        assertThat(this.finder.find("Predocin #ppl", 50))
            .anyMatch(gn -> gn.getName().equals("Prędocin"));
        // search by name and deatureCode prefix
        assertThat(this.finder.find("Predocin #pp", 50))
            .anyMatch(gn -> gn.getName().equals("Prędocin"));
        //search by name and inexistent featureCode
        assertThat(this.finder.find("Predocin #ADM3", 50)).isEmpty();
    }

    @Test
    void clearingCore_leavesEmptyCore() throws Exception {
        try (final InputStream in = openTextFile()) {
            this.indexer.importNames(in);
        }

        assertThat(this.finder.findById("752942")).isNotEmpty();
        assertThat(this.finder.find("Poraj", 50)).isNotEmpty();
        assertThat(this.finder.find("Yorks", 50)).isNotEmpty();

        this.indexer.clear();

        assertThat(this.finder.findById("752942")).isEmpty();
        assertThat(this.finder.find("Poraj", 50)).isEmpty();
        assertThat(this.finder.find("Yorks", 50)).isEmpty();
    }
    
    @Test
    void importingNamesIntoUnreachableSolr_throwsException() throws Exception {

        final SolrClient solr = Mockito.mock(SolrClient.class);
        when(solr.addBeans(anyCollection())).thenThrow(RuntimeException.class);
        this.indexer = new GeoNameIndexingService(solr);

        try (final InputStream in = openTextFile()) {
            try {
                this.indexer.importNames(in);
                fail("Exception expected");
            } catch (Exception e) {
                // pass
            }
        }
    }

    private InputStream openTextFile() {
        return getClass().getResourceAsStream("/geonames/testdata.txt");
    }
}
