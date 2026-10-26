package edu.harvard.iq.dataverse.persistence.datafile.license;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;

/**
 * @author madryk
 */
public class LicenseRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private LicenseRepository repository;

    //-------------------- TESTS --------------------

	@Test
    public void findFirstActive() {
        assertThat(this.repository.findFirstActive())
            .extracting(License::getId, License::getName)
            .containsExactly(1L, "CC0 Creative Commons Zero 1.0 Waiver");
    }

    @Test
    public void findAllOrderedByPosition() {
        License licenseToMove = this.repository.getById(2L);
        licenseToMove.setPosition(40L);
        this.repository.save(licenseToMove);

        assertThat(this.repository.findAllOrderedByPosition()).hasSize(14)
            .extracting(License::getName)
            .containsExactly(
                "CC0 Creative Commons Zero 1.0 Waiver",
                "CC BY-SA Creative Commons Attribution - ShareAlike License 4.0",
                "CC BY-NC Creative Commons Attribution - NonCommercial License 4.0",
                "CC BY-ND Creative Commons Attribution - NoDerivs License 4.0",
                "CC BY-NC-SA Creative Commons Attribution - NonCommercial - ShareAlike License 4.0",
                "CC BY-NC-ND Creative Commons Attribution - NonCommercial - NoDerivs License 4.0",
                "Apache Software License 2.0",
                "BSD 3 – Clause “New” or “Revised” License",
                "GNU General Public License 3.0",
                "GNU Lesser General Public License 3.0",
                "GNU Affero General Public License version 3",
                "MIT License",
                "Open Data Commons Open Database License 1.0",
                "CC BY Creative Commons Attribution License 4.0");
    }

    @Test
    public void findLicenseByName() {
        String name = "CC0 Creative Commons Zero 1.0 Waiver";

        assertThat(this.repository.findLicenseByName(name).get().getName()).isEqualTo(name);
    }

    @Test
    public void findLicenseByName_not_existing() {
        assertThat(this.repository.findLicenseByName("not exists")).isNotPresent();
    }
}
