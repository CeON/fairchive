package edu.harvard.iq.dataverse.persistence.geonames;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;

public class GeoNameRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private GeoNameRepository repository;
    
    @Test
    void findById() {
        
        Optional<GeoName> name = this.repository.findById(462259);
        
        assertThat(name).isNotEmpty();
        assertThat(name.get().getName()).isEqualTo("Zodenen");
    }
}
