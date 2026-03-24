package edu.harvard.iq.dataverse.workflow.artifacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.io.InputSupplier;

import edu.harvard.iq.dataverse.persistence.StubJpaPersistence;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifact;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;
import edu.harvard.iq.dataverse.test.WithTestClock;

public class WorkflowArtifactServiceBeanTest implements WithTestClock {

    static final String ENCODING = "BINARY";
    static final String NAME = "TEST";
    static final Supplier<InputStream> DATA_SUPPLIER = () -> mock(InputStream.class);

    StubJpaPersistence persistence = new StubJpaPersistence();
    WorkflowArtifactRepository repository = persistence.stub(WorkflowArtifactRepository.class);
    WorkflowArtifactStorage storage = mock(WorkflowArtifactStorage.class);

    WorkflowArtifactServiceBean serviceBean = new WorkflowArtifactServiceBean(repository, storage, clock);

    WorkflowArtifactSource data = new WorkflowArtifactSource(NAME, ENCODING, DATA_SUPPLIER);

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should save artifact data and metadata")
    public void shouldSaveDataAndMetadata() throws IOException {
        // given
        doReturn("testLocation")
                .when(storage).write(any(Supplier.class));

        // when
        WorkflowArtifact artifact =
                serviceBean.create(1L,  data);

        // then
        assertThat(artifact.getId()).isNotNull();
        assertThat(artifact.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(artifact.getStorageLocation()).isEqualTo("testLocation");
        assertThat(artifact.getName()).isEqualTo(NAME);
        assertThat(artifact.getEncoding()).isEqualTo(ENCODING);
    }

    @Test
    @DisplayName("Should be able to retrieve stored artifact data")
    public void shouldRetrieveStoredData() {
        // given
        WorkflowArtifact artifact = new WorkflowArtifact(1L, NAME, ENCODING,
                "testLocation", clock);
        doReturn(Optional.of(DATA_SUPPLIER))
                .when(storage).read("testLocation");

        // when
        Optional<Supplier<InputStream>> streamSupplier = serviceBean.readAsStream(artifact.getStorageLocation());

        // then
        assertThat(streamSupplier.isPresent()).isTrue();
    }
}
