package edu.harvard.iq.dataverse.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.common.files.mime.MimeTypes;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReader;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.SystemConfig;

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



    @Test
    public void getTabDataReaderByMimeType() {
        // given
        when(settingsService.getValueForKey(Key.RserveHost)).thenReturn(RSERVE_HOST);
        when(settingsService.getValueForKeyAsInt(Key.RservePort)).thenReturn(RSERVE_PORT);
        when(settingsService.getValueForKey(Key.RserveUser)).thenReturn(RSERVE_USER);
        when(settingsService.getValueForKey(Key.RservePassword)).thenReturn(RSERVE_PASSWORD);

        // when
        RDATAFileReader rdataFileReader =
                (RDATAFileReader) ingestServiceBean.getTabDataReaderByMimeType(MimeTypes.RDATA);

        // then
        assertThat(rdataFileReader)
                .extracting(RDATAFileReader::getRserveHost, RDATAFileReader::getRservePort,
                        RDATAFileReader::getRserveUser, RDATAFileReader::getRservePassword)
                .containsExactly(RSERVE_HOST, RSERVE_PORT,
                        RSERVE_USER, RSERVE_PASSWORD);
    }
}