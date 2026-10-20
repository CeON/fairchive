package edu.harvard.iq.dataverse.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

public class PrivateUrlUserTest {

    @Test
    public void isAnonymizedViewerOf() {
        // given
        Dataset dataset = dataset(7L);

        // when
        boolean anonymizedViewer = PrivateUrlUser.isAnonymizedViewerOf(new PrivateUrlUser(7L, true), dataset);

        // then
        assertThat(anonymizedViewer).isTrue();
    }

    @Test
    public void isAnonymizedViewerOf__notAnonymized() {
        // given
        Dataset dataset = dataset(7L);

        // when
        boolean anonymizedViewer = PrivateUrlUser.isAnonymizedViewerOf(new PrivateUrlUser(7L, false), dataset);

        // then
        assertThat(anonymizedViewer).isFalse();
    }

    @Test
    public void isAnonymizedViewerOf__privateUrlOfOtherDataset() {
        // given
        Dataset dataset = dataset(7L);

        // when
        boolean anonymizedViewer = PrivateUrlUser.isAnonymizedViewerOf(new PrivateUrlUser(8L, true), dataset);

        // then
        assertThat(anonymizedViewer).isFalse();
    }

    @Test
    public void isAnonymizedViewerOf__notPrivateUrlUser() {
        // given
        Dataset dataset = dataset(7L);

        // when
        boolean anonymizedViewer = PrivateUrlUser.isAnonymizedViewerOf(GuestUser.get(), dataset);

        // then
        assertThat(anonymizedViewer).isFalse();
    }

    // -------------------- PRIVATE --------------------

    private static Dataset dataset(Long id) {
        Dataset dataset = new Dataset();
        dataset.setId(id);
        return dataset;
    }
}
