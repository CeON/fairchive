package edu.harvard.iq.dataverse.persistence.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;


public class MetadataBlockRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private MetadataBlockRepository repository;

    @Test
    public void findByName() {
        // when
        Optional<MetadataBlock> block = repository.findByName("citation");
        // then
        assertThat(block).isPresent();
        assertThat(block).map(MetadataBlock::getName).contains("citation");
    }

    @Test
    public void findByName__not_found() {
        // when
        Optional<MetadataBlock> block = repository.findByName("CitatioN");
        // then
        assertThat(block).isEmpty();
    }

    @Test
    public void findSystemMetadataBlocks() {
        // when
        List<MetadataBlock> blocks = repository.findSystemMetadataBlocks();
        // then
        assertThat(blocks).hasSize(6);
        assertThat(blocks).extracting(MetadataBlock::getName).containsExactly(
                "citation", "geospatial", "socialscience",
                "astrophysics", "biomedical", "journal");
    }

}
