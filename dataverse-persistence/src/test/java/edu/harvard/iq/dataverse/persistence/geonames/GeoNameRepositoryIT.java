package edu.harvard.iq.dataverse.persistence.geonames;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
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

    @Test
    void find_byempty() {
        List<GeoName> results = this.repository.find("   ");

        assertThat(results).isEmpty();
    }

    @Test
    void find_byName() {
        List<GeoName> results = this.repository.find("Variaz");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(477032);
        assertThat(results.get(0).getName()).isEqualTo("Variazhanka");
    }

    @Test
    void find_byAlternateNames() {
        List<GeoName> results = this.repository.find("Mlynski");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(477032);
        assertThat(results.get(0).getName()).isEqualTo("Variazhanka");
    }

    @Test
    void findMultiple_byAlternateNames() {
        List<GeoName> results = this.repository.find("Powiat");

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(GeoName::getName).collect(toList()))
                .containsExactlyInAnyOrder("Powiat strzelecki", "Powiat krapkowicki");
    }

    @Test
    void importNames() throws Exception {
        this.repository.deleteAll();
        
        assertThat(this.repository.countAll()).isZero();
        
//        try (final InputStream in = new FileInputStream("C:\\prj\\dariah\\geonames\\PL.txt")) {
//            this.repository.importNames(in);
//        }
//        try (final InputStream in = new FileInputStream("C:\\prj\\dariah\\geonames\\allCountries.txt")) {
//            this.repository.importNames(in);
//        }
        
        try (final InputStream in = getClass().getResourceAsStream("/PL.txt")) {
            this.repository.importNames(in);
        }
        
        GeoName tr0 = this.repository.getById(6285565);
        assertThat(tr0.getName()).isEqualTo("Pilsko");
        assertThat(tr0.isTier0()).isTrue();
        assertThat(tr0.isTier1()).isFalse();
        assertThat(tr0.isTier2()).isFalse();
        assertThat(tr0.isTier3()).isFalse();
        assertThat(tr0.isTier4()).isFalse();
        assertThat(tr0.getHierarchy()).isEqualTo("PL");
        
        GeoName tr1 = this.repository.getById(858787);
        assertThat(tr1.getName()).isEqualTo("Masovian Voivodeship");
        assertThat(tr1.isAdm1()).isTrue();
        assertThat(tr1.isTier0()).isFalse();
        assertThat(tr1.isTier1()).isTrue();
        assertThat(tr1.isTier2()).isFalse();
        assertThat(tr1.isTier3()).isFalse();
        assertThat(tr1.isTier4()).isFalse();
        assertThat(tr1.getHierarchy()).isEqualTo("PL - Masovian Voivodeship");
        
        GeoName tr2 = this.repository.getById(3079850);
        assertThat(tr2.getName()).isEqualTo("Powiat będziński");
        assertThat(tr2.isAdm2()).isTrue();
        assertThat(tr2.isTier0()).isFalse();
        assertThat(tr2.isTier1()).isFalse();
        assertThat(tr2.isTier2()).isTrue();
        assertThat(tr2.isTier3()).isFalse();
        assertThat(tr2.isTier4()).isFalse();
        assertThat(tr2.getHierarchy()).isEqualTo("PL - Silesian Voivodeship - Powiat będziński");
        
        GeoName tr3 = this.repository.getById(7532923);
        assertThat(tr3.getName()).isEqualTo("Będzin");
        assertThat(tr3.isAdm3()).isTrue();
        assertThat(tr3.isTier0()).isFalse();
        assertThat(tr3.isTier1()).isFalse();
        assertThat(tr3.isTier2()).isFalse();
        assertThat(tr3.isTier3()).isTrue();
        assertThat(tr3.isTier4()).isFalse();
        assertThat(tr3.getHierarchy()).isEqualTo("PL - Silesian Voivodeship - Powiat będziński - Będzin");
        
        GeoName tr4 = this.repository.getById(8410917);
        assertThat(tr4.getName()).isEqualTo("Bemowo Airport");
        assertThat(tr4.isTier0()).isFalse();
        assertThat(tr4.isTier1()).isFalse();
        assertThat(tr4.isTier2()).isFalse();
        assertThat(tr4.isTier3()).isFalse();
        assertThat(tr4.isTier4()).isTrue();
        assertThat(tr4.getHierarchy()).isEqualTo("PL - Masovian Voivodeship - Warszawa - Warszawa - Bemowo Airport");
    }
}
