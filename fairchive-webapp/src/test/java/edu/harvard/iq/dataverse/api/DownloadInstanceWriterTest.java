package edu.harvard.iq.dataverse.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.dataaccess.DataConverter;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datafile.page.WholeDatasetDownloadLogger;
import edu.harvard.iq.dataverse.rserve.RemoteDataFrameService;
import edu.harvard.iq.dataverse.util.SystemConfig;

@ExtendWith(MockitoExtension.class)
public class DownloadInstanceWriterTest {

    @Mock 
    private SystemConfig sysConfig;
    @Mock 
    private WholeDatasetDownloadLogger datasetDownloadLogger;
    @Mock
    private RemoteDataFrameService remoteDataFrameService;
    @InjectMocks
    private ImageThumbConverter thumbConverter;
    @InjectMocks
    private DataConverter dataConverter;
    
    private DownloadInstanceWriter writer;
    
    @BeforeEach
    void setUp() {
        this.writer = new DownloadInstanceWriter(this.dataConverter, 
                    this.datasetDownloadLogger, this.thumbConverter);
    };
    
    @Test
    void isWriteable_returnsTrue_onlyForDownloadInstanceClass() {
        assertThat(this.writer.isWriteable(DownloadInstance.class, null, null, null)).isTrue();
        
        assertThat(this.writer.isWriteable(null, null, null, null)).isFalse();
        assertThat(this.writer.isWriteable(String.class, null, null, null)).isFalse();
    }
    
    @Test
    void writeTo() {
        
    }
    
    private DownloadInstance createDownloadInstance() {
        return new DownloadInstance();
    }
}
