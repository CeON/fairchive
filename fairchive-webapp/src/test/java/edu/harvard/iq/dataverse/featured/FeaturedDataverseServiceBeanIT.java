package edu.harvard.iq.dataverse.featured;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.link.DataverseLinkingDataverseRepository;
import edu.harvard.iq.dataverse.search.SolrIndexCleaner;
import org.apache.solr.client.solrj.SolrServerException;
import org.assertj.core.util.Lists;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(TransactionMode.ROLLBACK)
public class FeaturedDataverseServiceBeanIT extends WebappArquillianDeployment {

    @Inject
    private FeaturedDataverseServiceBean service;

    @Inject
    private DataverseRepository dataverseRepository;

    @Inject
    private DataverseLinkingDataverseRepository linkingDataverseRepository;

    @Inject
    private DataverseFeaturedDataverseRepository featuredDataverseRepository;

    @Inject
    private SolrIndexCleaner solrIndexCleaner;

    @BeforeEach
    public void init() throws SolrServerException, IOException {
        solrIndexCleaner.cleanupSolrIndex();
    }

    // -------------------- TESTS --------------------

    @Test
    public void refreshFeaturedDataversesAutomaticSorting() {
        // given
        Dataverse root = dataverseRepository.getById(1L);
        List<Dataverse> featuredDataverses = service.findByDataverseId(1L);
        assertThat(featuredDataverses.stream().map(Dataverse::getId)).containsExactly(19L, 23L, 20L, 21L);

        // when
        root.setFeaturedDataversesSorting(Dataverse.FeaturedDataversesSorting.BY_DATASET_COUNT);
        dataverseRepository.save(root);
        service.refreshFeaturedDataversesAutomaticSorting();

        // then
        assertThat(service.findByDataverseId(1L).stream().map(Dataverse::getId)).containsExactly(21L, 19L, 20L, 23L);
    }

    @Test
    public void sortFeaturedDataverses() {
        // given
        List<Dataverse> featuredDataverses = service.findByDataverseId(1L);
        assertThat(featuredDataverses.stream().map(Dataverse::getId)).containsExactly(19L, 23L, 20L, 21L);
        List<Dataverse> manualOrder = Lists.newArrayList(
                featuredDataverses.get(2), featuredDataverses.get(0), featuredDataverses.get(3), featuredDataverses.get(1));

        // when
        List<Dataverse> sortedManual = service.sortFeaturedDataverses(manualOrder, Dataverse.FeaturedDataversesSorting.BY_HAND);

        // then
        assertThat(sortedManual.stream().map(Dataverse::getId)).containsExactly(20L, 19L, 21L, 23L);

        // when
        List<Dataverse> sortedNameAsc = service.sortFeaturedDataverses(manualOrder, Dataverse.FeaturedDataversesSorting.BY_NAME_ASC);

        // then
        assertThat(sortedNameAsc.stream().map(Dataverse::getId)).containsExactly(21L, 19L, 23L, 20L);

        // when
        List<Dataverse> sortedNameDesc = service.sortFeaturedDataverses(manualOrder, Dataverse.FeaturedDataversesSorting.BY_NAME_DESC);

        // then
        assertThat(sortedNameDesc.stream().map(Dataverse::getId)).containsExactly(20L, 23L, 19L, 21L);

        // when
        List<Dataverse> sortedDatasetCount = service.sortFeaturedDataverses(manualOrder, Dataverse.FeaturedDataversesSorting.BY_DATASET_COUNT);

        // then
        assertThat(sortedDatasetCount.stream().map(Dataverse::getId)).containsExactly(21L, 19L, 20L, 23L);
    }

    @Test
    public void hasFeaturableOrFeaturedDataverses__trueForPublishedSubcollections() {
        // when & then
        assertThat(service.hasFeaturableOrFeaturedDataverses(1L)).isTrue();
    }

    @Test
    public void hasFeaturableOrFeaturedDataverses__falseForUnpublishedSubcollectionsOnly() {
        // when & then
        assertThat(service.hasFeaturableOrFeaturedDataverses(19L)).isFalse();
    }

    @Test
    public void hasFeaturableOrFeaturedDataverses__falseWithoutSubcollections() {
        // when & then
        assertThat(service.hasFeaturableOrFeaturedDataverses(20L)).isFalse();
    }

    @Test
    public void hasFeaturableOrFeaturedDataverses__trueForLinkedCollection() {
        // given
        DataverseLinkingDataverse link = new DataverseLinkingDataverse();
        link.setLinkingDataverse(dataverseRepository.getById(20L));
        link.setDataverse(dataverseRepository.getById(21L));
        link.setLinkCreateTime(new Date());
        linkingDataverseRepository.saveAndFlush(link);

        // when & then
        assertThat(service.hasFeaturableOrFeaturedDataverses(20L)).isTrue();
    }

    @Test
    public void hasFeaturableOrFeaturedDataverses__trueForFeaturedCollection() {
        // given
        DataverseFeaturedDataverse featured = new DataverseFeaturedDataverse();
        featured.setDataverse(dataverseRepository.getById(20L));
        featured.setFeaturedDataverse(dataverseRepository.getById(21L));
        featured.setDisplayOrder(0);
        featuredDataverseRepository.saveAndFlush(featured);

        // when & then
        assertThat(service.hasFeaturableOrFeaturedDataverses(20L)).isTrue();
    }
}
