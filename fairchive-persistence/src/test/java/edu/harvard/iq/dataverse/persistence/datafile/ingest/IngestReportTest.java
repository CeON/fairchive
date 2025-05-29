package edu.harvard.iq.dataverse.persistence.datafile.ingest;

import static edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError.UNZIP_FAIL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

public class IngestReportTest {

    @Test
    public void getIngestErrorMessage_works() {
        
        IngestReport report = new IngestReport();
        
        assertThat(report.getIngestReportMessage()).isEqualTo("Unknown error occurred during ingest.");
        
        report.setErrorKey(UNZIP_FAIL);
        assertThat(report.getIngestReportMessage()).isEqualTo("Failed to unzip the file.");
    }
    
    @Test
    public void createIngestFailureReport_works() {
        
        IngestReport report = IngestReport.createIngestFailureReport(new DataFile(), UNZIP_FAIL, "arg1");
        
        assertThat(report.getErrorArguments()).containsExactly("arg1");
        
        report = IngestReport.createIngestFailureReport(new DataFile(), UNZIP_FAIL);
        
        assertThat(report.getErrorArguments()).isEmpty();
    }
    
    @Test
    public void createIngestFailureReport_fromException_works() {
        
        IngestException exception = new IngestException(UNZIP_FAIL, "arg1");
        IngestReport report = IngestReport.createIngestFailureReport(new DataFile(), exception);
        
        assertThat(report.getErrorArguments()).containsExactly("arg1");
    }
}
