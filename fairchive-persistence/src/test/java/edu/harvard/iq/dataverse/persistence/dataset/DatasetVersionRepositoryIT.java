package edu.harvard.iq.dataverse.persistence.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;

public class DatasetVersionRepositoryIT extends PersistenceArquillianDeployment {

	@Inject
	private DatasetVersionRepository repository;

	@Test
	public void getLatestReleasedByGlobalId() {

		Optional<DatasetVersion> version;
		
		// one version - released
		version = repository.findLatestReleasedByGlobalId("doi", "10.18153", "FK2/TTVQKZ");
		assertThat(version.get().getId()).isEqualTo(64L);
		
		// two versions - DRAFT and RELEASED
		version = repository.findLatestReleasedByGlobalId("doi", "10.18150", "FK2/QTVQXY");
		assertThat(version.get().getId()).isEqualTo(142L);
		
		// one version - DRAFT
		version = repository.findLatestReleasedByGlobalId("doi", "10.18150", "FK2/MLXK1N");
		assertThat(version).isEmpty();
	}
}
