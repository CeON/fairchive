package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReader;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IngestServiceBeanTest {

    private static final String RSERVE_HOST = "host";
    private static final int RSERVE_PORT = 633;
    private static final String RSERVE_USER = "user";
    private static final String RSERVE_PASSWORD = "password";

    @InjectMocks
    private IngestServiceBean ingestServiceBean;

    @Mock
    private SettingsServiceBean settingsService;
    @Mock
    private SystemConfig systemConfig;

    // -------------------- TESTS --------------------

    @Test
    public void getIngestSizeLimit() {
        // given
        DataFile dataFile = new DataFile("application/x-stata-13");
        when(systemConfig.getTabularIngestSizeLimit("dta")).thenReturn(14L);
        // when
        long limit = ingestServiceBean.getIngestSizeLimit(dataFile);
        // then
        assertThat(limit).isEqualTo(14L);
    }

    @Test
    public void exceedsIngestSizeLimit__true() {
        // given
        DataFile dataFile = new DataFile("application/x-stata-13");
        dataFile.setFilesize(1025L);
        when(systemConfig.getTabularIngestSizeLimit("dta")).thenReturn(1024L);
        // when
        boolean ret = ingestServiceBean.exceedsIngestSizeLimit(dataFile);
        // then
        assertThat(ret).isTrue();
    }

    @Test
    public void exceedsIngestSizeLimit__false() {
        // given
        DataFile dataFile = new DataFile("application/x-stata-13");
        dataFile.setFilesize(1024L);
        when(systemConfig.getTabularIngestSizeLimit("dta")).thenReturn(1024L);
        // when
        boolean ret = ingestServiceBean.exceedsIngestSizeLimit(dataFile);
        // then
        assertThat(ret).isFalse();
    }

    @Test
    public void exceedsIngestSizeLimit__no_limit() {
        // given
        DataFile dataFile = new DataFile("application/x-stata-13");
        dataFile.setFilesize(1024L);
        when(systemConfig.getTabularIngestSizeLimit("dta")).thenReturn(-1L);
        // when
        boolean ret = ingestServiceBean.exceedsIngestSizeLimit(dataFile);
        // then
        assertThat(ret).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, false",
            "text/csv, true",
            "text/comma-separated-values, true",
            "text/tsv, false",
            "application/octet-strean, false",
            "text/plain, false"
    })
    public void supportsPickingEncoding(String mimeType, boolean expectedSupportsPickingEncoding) {
        // given
        DataFile dataFile = new DataFile(mimeType);
        // when
        boolean ret = ingestServiceBean.supportsPickingEncoding(dataFile);
        // then
        assertThat(ret).isEqualTo(expectedSupportsPickingEncoding);
    }

    @ParameterizedTest
    @CsvSource({
            "application/x-spss-por, true",
            "text/tsv, false",
            "application/octet-strean, false",
            "text/plain, false"
    })
    public void supportsInclusionOfLabelsFile(String mimeType, boolean expectedSupportsInclusionOfLabelsFile) {
        // given
        DataFile dataFile = new DataFile(mimeType);
        // when
        boolean ret = ingestServiceBean.supportsInclusionOfLabelsFile(dataFile);
        // then
        assertThat(ret).isEqualTo(expectedSupportsInclusionOfLabelsFile);
    }

    @ParameterizedTest
    @CsvSource({
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, true",
            "text/tsv, true",
            "text/tab-separated-values, true",
            "application/octet-strean, false",
            "text/plain, false"
    })
    public void isSelectivelyIngestableFile(String mimeType, boolean expectedSelectivelyIngestable) {
        // given
        DataFile dataFile = new DataFile(mimeType);
        // when
        boolean ret = ingestServiceBean.isSelectivelyIngestableFile(dataFile);
        // then
        assertThat(ret).isEqualTo(expectedSelectivelyIngestable);
    }

    @Test
    public void getTabDataReaderByMimeType() {
        // given
        when(settingsService.getValueForKey(Key.RserveHost)).thenReturn(RSERVE_HOST);
        when(settingsService.getValueForKeyAsInt(Key.RservePort)).thenReturn(RSERVE_PORT);
        when(settingsService.getValueForKey(Key.RserveUser)).thenReturn(RSERVE_USER);
        when(settingsService.getValueForKey(Key.RservePassword)).thenReturn(RSERVE_PASSWORD);

        // when
        RDATAFileReader rdataFileReader =
                (RDATAFileReader) ingestServiceBean.getTabDataReaderByMimeType(ApplicationMimeType.RDATA.getMimeValue());

        // then
        assertThat(rdataFileReader)
                .extracting(RDATAFileReader::getRserveHost, RDATAFileReader::getRservePort,
                        RDATAFileReader::getRserveUser, RDATAFileReader::getRservePassword)
                .containsExactly(RSERVE_HOST, RSERVE_PORT,
                        RSERVE_USER, RSERVE_PASSWORD);
    }
}