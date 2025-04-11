package edu.harvard.iq.dataverse.persistence.geonames;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    
    @Test
    void find_byempty() {      
        List<GeoName> results = this.repository.find("   ");
        
        assertThat(results).isEmpty();
    }
    
    @Test
    void find_byName() {
        List<GeoName> results = this.repository.find("Variaz");
        
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(477032);
        assertThat(results.get(0).getName()).isEqualTo("Variazhanka");
    }
    
    @Test
    void find_byAlternateNames() {
        List<GeoName> results = this.repository.find("Mlynski");
        
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getId()).isEqualTo(477032);
        assertThat(results.get(0).getName()).isEqualTo("Variazhanka");
    }
    
    @Test
    void findMultiple_byAlternateNames() {
        List<GeoName> results = this.repository.find("Powiat");

        assertThat(results.size()).isEqualTo(2);
        assertThat(results.stream().map(GeoName::getName).collect(toList()))
                .containsExactlyInAnyOrder("Powiat strzelecki", "Powiat krapkowicki");

    }
}
