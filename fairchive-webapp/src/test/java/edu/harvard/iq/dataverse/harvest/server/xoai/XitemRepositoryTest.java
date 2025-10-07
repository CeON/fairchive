package edu.harvard.iq.dataverse.harvest.server.xoai;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class XitemRepositoryTest {

    @InjectMocks
    private XitemRepository xitemRepository;

    @Mock
    private OAIRecordServiceBean recordService;
    @Mock
    private DatasetService datasetService;
    @Mock 
    private SystemConfig systemConfig;
    // -------------------- TESTS --------------------

    @Test
    void getRecord() throws IdDoesNotExistException, OAIException {
        // given
        OAIRecord oaiRecord1 = new OAIRecord("", "id", dateFrom("1990-01-01T10:00:00.00Z"));
        OAIRecord oaiRecord2 = new OAIRecord("set2", "id", dateFrom("1990-01-01T11:00:00.00Z"));
        OAIRecord oaiRecord3 = new OAIRecord("set3", "id", dateFrom("1990-01-01T12:00:00.00Z"));
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        when(recordService.findOaiRecordsByGlobalId("id"))
            .thenReturn(asList(oaiRecord2, oaiRecord1, oaiRecord3));
        when(datasetService.findByGlobalId("id")).thenReturn(dataset);

        // when
        Item item = xitemRepository.getItem("id");

        // then
        assertThat(item).isInstanceOf(Xitem.class);
        assertThat(item.getIdentifier()).isEqualTo("id");
        assertThat(((Xitem)item).getDataset()).isEqualTo(dataset);
        assertThat(item.getDatestamp()).hasSameTimeAs("1990-01-01T10:00:00Z");
        assertThat(item.getSets()).extracting(Set::getSpec)
            .containsExactlyInAnyOrder("set2", "set3");
    }

    @Test
    void getRecord_no_oai_record_present() throws IdDoesNotExistException, OAIException {
        // given
        when(recordService.findOaiRecordsByGlobalId("id")).thenReturn(emptyList());

        // when & then
        assertThatThrownBy(() -> xitemRepository.getItem("id"))
            .isInstanceOf(IdDoesNotExistException.class);
    }

    @Test
    void getRecord_no_dataset() throws IdDoesNotExistException, OAIException {
        // given
        OAIRecord oaiRecord1 = new OAIRecord("", "id", dateFrom("1990-01-01T10:00:00.00Z"));
        when(recordService.findOaiRecordsByGlobalId("id")).thenReturn(asList(oaiRecord1));

        // when & then
        assertThatThrownBy(() -> xitemRepository.getItem("id"))
            .isInstanceOf(IdDoesNotExistException.class);
    }
    
    @Test
    void getItems_returnsItemsWithIdentifiersDependingOnSystemSetting()
            throws Exception {

        OAIRecord record = new OAIRecord("", "doi:10.5072/FK2/PWYXDG",
                dateFrom("1990-01-01T10:00:00.00Z"));
        when(this.recordService.findOaiRecordsBySetName(null, null, null))
                .thenReturn(asList(record));
        when(this.datasetService.findByGlobalId(any())).thenReturn(new Dataset());

        List<Item> items = this.xitemRepository.getItems(emptyList(), 0, 1)
                .getResults();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIdentifier()).isEqualTo(record.getGlobalId());

        // swich setting
        when(this.systemConfig.useOAIStrictIdentifierScheme()).thenReturn(true);
        when(this.systemConfig.getDataverseServer()).thenReturn("test.pl");

        items = this.xitemRepository.getItems(emptyList(), 0, 1).getResults();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getIdentifier())
            .isEqualTo("oai:test.pl:doi%3A10.5072%2FFK2%2FPWYXDG");
    }
    
    private static Date dateFrom(final String s) {
        return Date.from(Instant.parse(s));
    }
}
