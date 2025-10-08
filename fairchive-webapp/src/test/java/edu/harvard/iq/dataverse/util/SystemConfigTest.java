package edu.harvard.iq.dataverse.util;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DownloadMethods;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.ReadonlyMode;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SiteUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.UploadMethods;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemConfigTest {

    @Mock
    private SettingsServiceBean settings;

    @InjectMocks
    private SystemConfig config;

    @BeforeEach
    public void setup() {
        when(this.settings.getValueForKey(SiteUrl))
                .thenReturn("http://www.google.com:1234");
    }

    @Test
    void getDataverseSiteUrl() {
        // when
        String siteUrl = config.getDataverseSiteUrl();

        // then
        assertThat(siteUrl).isEqualTo("http://www.google.com:1234");
    }

    @Test
    void getDataverseServer() {
        // when
        String siteUrl = config.getDataverseServer();

        // then
        assertThat(siteUrl).isEqualTo("www.google.com");
    }

    @ParameterizedTest
    @CsvSource({
            ", , false",
            "native/http, ,false",
            "native/http, native/http,false",
            "native/http, dcm/rsync+ssh,false",
            "native/http, native/http dcm/rsync+ssh,false",
            "rsal/rsync, ,false",
            "rsal/rsync, native/http, false",
            "rsal/rsync, native/http dcm/rsync+ssh, false",
            "rsal/rsync, dcm/rsync+ssh, true",
    })
    void isRsyncOnly(String downloadMethods, String uploadMethods,
            boolean result) {

        when(this.settings.getValueForKey(DownloadMethods))
                .thenReturn(downloadMethods);
        when(this.settings.getValueForKey(UploadMethods)).thenReturn(uploadMethods);

        assertThat(this.config.isRsyncOnly()).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource({
            ", false",
            "native/http, false",
            "rsal/rsync, true",
            "rsal/rsync native/http, true",
    })
    void isRsyncDownload(String methods, boolean result) {

        when(this.settings.getValueForKey(DownloadMethods)).thenReturn(methods);

        assertThat(this.config.isRsyncDownload()).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource({
            ", false",
            "native/http, true",
            "rsal/rsync, false",
            "rsal/rsync native/http, true",
    })
    void isHttpDownload(String methods, boolean result) {

        when(this.settings.getValueForKey(DownloadMethods)).thenReturn(methods);

        assertThat(this.config.isHTTPDownload()).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource({
            ", false",
            "native/http, false",
            "dcm/rsync+ssh, true",
            "dcm/rsync+ssh native/http, true",
    })
    void isRsyncUpload(String methods, boolean result) {

        when(this.settings.getValueForKey(UploadMethods)).thenReturn(methods);

        assertThat(this.config.isRsyncUpload()).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource({
            ", false",
            "native/http, true",
            "dcm/rsync+ssh, false",
            "dcm/rsync+ssh native/http, true",
    })
    void isHttpUpload(String methods, boolean result) {

        when(this.settings.getValueForKey(UploadMethods)).thenReturn(methods);

        assertThat(this.config.isHTTPUpload()).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource({
            ", 0",
            "native/http, 1",
            "dcm/rsync+ssh native/http, 2",
    })
    void getUploadMethodCount(String methods, int result) {

        when(this.settings.getValueForKey(UploadMethods)).thenReturn(methods);

        assertThat(this.config.getUploadMethodCount()).isEqualTo(result);
    }

    @Test
    void thumbnailSizeLimit_readOnly() {

        when(this.settings.isTrueForKey(ReadonlyMode)).thenReturn(true);
        System.setProperty("dataverse.dataAccess.thumbnail.image.limit", "");
        System.setProperty("dataverse.dataAccess.thumbnail.pdf.limit", "");

        assertThat(this.config.isReadonlyMode()).isTrue();

        assertThat(this.config.isThumbnailGenerationDisabledForImages()).isTrue();
        assertThat(this.config.isThumbnailGenerationDisabledForPDF()).isTrue();

        assertThat(this.config.getThumbnailSizeLimitImage()).isEqualTo(-1L);
        assertThat(this.config.getThumbnailSizeLimitPDF()).isEqualTo(-1L);
        
        System.setProperty("dataverse.dataAccess.thumbnail.image.limit", "2");
        System.setProperty("dataverse.dataAccess.thumbnail.pdf.limit", "3");
        
        assertThat(this.config.isReadonlyMode()).isTrue();

        assertThat(this.config.isThumbnailGenerationDisabledForImages()).isTrue();
        assertThat(this.config.isThumbnailGenerationDisabledForPDF()).isTrue();

        assertThat(this.config.getThumbnailSizeLimitImage()).isEqualTo(-1L);
        assertThat(this.config.getThumbnailSizeLimitPDF()).isEqualTo(-1L);
    }

    @Test
    void thumbnailSizeLimit_writeable() {

        when(this.settings.isTrueForKey(ReadonlyMode)).thenReturn(false);
        System.setProperty("dataverse.dataAccess.thumbnail.image.limit", "");
        System.setProperty("dataverse.dataAccess.thumbnail.pdf.limit", "");

        assertThat(this.config.isReadonlyMode()).isFalse();

        assertThat(this.config.isThumbnailGenerationDisabledForImages()).isFalse();
        assertThat(this.config.isThumbnailGenerationDisabledForPDF()).isFalse();

        assertThat(this.config.getThumbnailSizeLimitImage())
                .isEqualTo(this.config.getDefaultThumbnailSizeLimit());
        assertThat(this.config.getThumbnailSizeLimitPDF())
                .isEqualTo(this.config.getDefaultThumbnailSizeLimit());

        System.setProperty("dataverse.dataAccess.thumbnail.image.limit", "2");
        System.setProperty("dataverse.dataAccess.thumbnail.pdf.limit", "3");
        
        assertThat(this.config.isReadonlyMode()).isFalse();
        
        assertThat(this.config.isThumbnailGenerationDisabledForImages()).isFalse();
        assertThat(this.config.isThumbnailGenerationDisabledForPDF()).isFalse();

        assertThat(this.config.getThumbnailSizeLimitImage()).isEqualTo(2L);
        assertThat(this.config.getThumbnailSizeLimitPDF()).isEqualTo(3L);
    }

}