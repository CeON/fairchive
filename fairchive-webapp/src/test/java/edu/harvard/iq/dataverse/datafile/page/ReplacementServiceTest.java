package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ReplacementServiceTest {

    private final ReplacementService service = new ReplacementService();

    // -------------------- TESTS --------------------

    @Test
    void listReplacements() {

        // given
        List<DataFile> existingFiles = asList(
                fileOf("foo.csv", "111"),
                fileOf("bar.csv", "222"),
                fileOf("baz.csv", "333"),
                fileOf("data.csv", "444")
        );
        List<DataFile> newFiles = asList(
                fileOf("foo.csv", "111-new"),
                fileOf("bar.csv", "222"),
                fileOf("baz.csv", "333-new"),
                fileOf("data-new.csv", "555")
        );

        // when
        List<ReplacementService.ReplacementGroup> result = service.listReplacements(existingFiles, newFiles);

        // then
        assertThat(result)
                .extracting(r -> r.getExistingFile().getLabel())
                .containsExactlyInAnyOrder("foo.csv", "baz.csv");
    }

    @Test
    void listReplacements__empty() {

        // given
        List<DataFile> existingFiles = asList(
                fileOf("foo.csv", "111"),
                fileOf("bar.csv", "222"),
                fileOf("baz.csv", "333"),
                fileOf("data.csv", "444")
        );
        List<DataFile> newFiles = asList(
                fileOf("foo.csv", "111"),
                fileOf("bar.csv", "222"),
                fileOf("baz.csv", "333"),
                fileOf("data-new.csv", "555")
        );

        // when
        List<ReplacementService.ReplacementGroup> result = service.listReplacements(existingFiles, newFiles);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void listReplacements__no_files__empty() {

        // given
        List<DataFile> existingFiles = emptyList();
        List<DataFile> newFiles = emptyList();

        // when
        List<ReplacementService.ReplacementGroup> result = service.listReplacements(existingFiles, newFiles);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void listReplacements__no_new_files__empty() {

        // given
        List<DataFile> existingFiles = asList(
                fileOf("foo.csv", "111"),
                fileOf("bar.csv", "222"),
                fileOf("baz.csv", "333"),
                fileOf("data.csv", "444")
        );
        List<DataFile> newFiles = emptyList();

        // when
        List<ReplacementService.ReplacementGroup> result = service.listReplacements(existingFiles, newFiles);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void listReplacements__no_old_files__empty() {

        // given
        List<DataFile> existingFiles = emptyList();
        List<DataFile> newFiles = asList(
                fileOf("foo.csv", "111"),
                fileOf("bar.csv", "222"),
                fileOf("baz.csv", "333"),
                fileOf("data-new.csv", "555")
        );

        // when
        List<ReplacementService.ReplacementGroup> result = service.listReplacements(existingFiles, newFiles);

        // then
        assertThat(result).isEmpty();
    }

    // -------------------- PRIVATE --------------------

    private DataFile fileOf(String name, String checksum) {
        DataFile dataFile = new DataFile();
        dataFile.setId(1L);
        dataFile.setChecksumValue(checksum);
        FileMetadata metadata = new FileMetadata();
        metadata.setLabel(name);
        dataFile.setFileMetadatas(singletonList(metadata));
        return dataFile;
    }
}
