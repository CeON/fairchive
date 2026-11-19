package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class DataciteDatasetMapperTest {

    private final DataciteDatasetMapper mapper = new DataciteDatasetMapper();

    // -------------------- TESTS --------------------

    @Test
    public void toDataset__identifierFromDoi() {
        // given
        // a search result carries the doi, but neither prefix nor suffix
        DataCiteFindDoiResponse response = response("10.5061/dryad.bf20k4q4");

        // when
        DatasetDTO dataset = mapper.toDataset(response);

        // then
        assertThat(dataset.getProtocol()).isEqualTo("doi");
        assertThat(dataset.getAuthority()).isEqualTo("10.5061");
        assertThat(dataset.getIdentifier()).isEqualTo("dryad.bf20k4q4");
    }

    @Test
    public void toDataset__identifierWithSlash() {
        // given
        DataCiteFindDoiResponse response = response("10.5072/fk2/abc");

        // when
        DatasetDTO dataset = mapper.toDataset(response);

        // then
        assertThat(dataset.getAuthority()).isEqualTo("10.5072");
        assertThat(dataset.getIdentifier()).isEqualTo("fk2/abc");
    }

    // -------------------- PRIVATE --------------------

    private static DataCiteFindDoiResponse response(String doi) {
        DataCiteFindDoiResponse response = new DataCiteFindDoiResponse();
        response.getAttributes().setDoi(doi);
        response.getAttributes().setContributors(emptyList());
        return response;
    }
}
