package edu.harvard.iq.dataverse.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

public class PermissionTest {

	@Test 
	void appliesTo() {
		
		assertThrows(NullPointerException.class, () -> Permission.AddDataset.appliesTo(null));
		assertThat(Permission.AddDataset.appliesTo(Dataset.class)).isFalse();
		assertThat(Permission.AddDataset.appliesTo(Dataverse.class)).isTrue();
		
		assertThat(Permission.PublishDataset.appliesTo(Dataverse.class)).isTrue();
		assertThat(Permission.PublishDataset.appliesTo(Dataset.class)).isTrue();
		assertThat(Permission.PublishDataset.appliesTo(DataFile.class)).isFalse();
	}
}
