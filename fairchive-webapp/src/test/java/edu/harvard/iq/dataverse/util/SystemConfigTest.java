package edu.harvard.iq.dataverse.util;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DownloadMethods;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.SiteUrl;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.UploadMethods;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(this.settings.getValueForKey(SiteUrl)).thenReturn("http://www.google.com:1234");
    }

    @Test
    void getDataverseSiteUrl() {
        // when
        String siteUrl = config.getDataverseSiteUrl();

        // then
        assertEquals(siteUrl, "http://www.google.com:1234");
    }

    @Test
    void getDataverseServer() {
        // when
        String siteUrl = config.getDataverseServer();

        // then
        assertEquals(siteUrl, "www.google.com");
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
        
        when(this.settings.getValueForKey(DownloadMethods)).thenReturn(downloadMethods);
        when(this.settings.getValueForKey(UploadMethods)).thenReturn(uploadMethods);
        
        assertEquals(result, this.config.isRsyncOnly());
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
        
        assertEquals(result, this.config.isRsyncDownload());
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
        
        assertEquals(result, this.config.isHTTPDownload());
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
        
        assertEquals(result, this.config.isRsyncUpload());
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
        
        assertEquals(result, this.config.isHTTPUpload());
    }
    
    @ParameterizedTest
    @CsvSource({  
        ", 0",
        "native/http, 1",
        "dcm/rsync+ssh native/http, 2",
    })
    void getUploadMethodCount(String methods, int result) {
        
        when(this.settings.getValueForKey(UploadMethods)).thenReturn(methods);
        
        assertEquals(result, this.config.getUploadMethodCount());
    }
    
}