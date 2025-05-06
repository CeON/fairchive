package edu.harvard.iq.dataverse.search.advanced;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.search.advanced.field.PeriodoSearchField;

public class PeriodoSearchFieldTest {
    
    private final PeriodoSearchField searchField = new PeriodoSearchField("test", "test", "test");
    
    private AbstractStringAssert<?> assertThatQueryFragment() {
        return assertThat(this.searchField.getQueryPart().queryFragment);
    }
    
    @Test
    void getQueryPart_empty() {
        assertThatQueryFragment().isEmpty();
    }
    
    @Test
    void getQueryPart_idOnly() {
        this.searchField.setId("123");
        
        assertThatQueryFragment().isEqualTo("periodo_id_s:\"123\"");
    }
    
    @Test
    void getQueryPart_labelOnly() {
        this.searchField.setLabel("label1");
        
        assertThatQueryFragment().isEqualTo("periodo_label_txt:\"label1\"");
    }
    
    @Test
    void getQueryPart_coverageNameOnly() {
        this.searchField.setCoverageName("name1");
        
        assertThatQueryFragment().isEqualTo("periodo_coverage_name_txt:\"name1\"");
    }
    
    @Test
    void getQueryPart_oneLocationsOnly() {
        this.searchField.setLocations(singletonList("location1"));
        
        assertThatQueryFragment().isEqualTo("periodo_locations_ss:\"location1\"");
    }
    
    @Test
    void getQueryPart_startEarliest_andStartLatestOnly() {
        this.searchField.setStartEarliest("2000");
        this.searchField.setStartLatest("2020");
        
        assertThatQueryFragment().isEqualTo("periodo_start_l:[2000 TO 2020]");
    }
    
    @Test
    void getQueryPart_startEarliestOnly() {
        this.searchField.setStartEarliest("2000");
        
        assertThatQueryFragment().isEqualTo("periodo_start_l:[2000 TO *]");
    }
    
    @Test
    void getQueryPart_startLatestOnly() {
        this.searchField.setStartLatest("2020");
        
        assertThatQueryFragment().isEqualTo("periodo_start_l:[* TO 2020]");
    }
    
    @Test
    void getQueryPart_stopEarliest_andStopLatestOnly() {
        this.searchField.setStopEarliest("2100");
        this.searchField.setStopLatest("2120");
        
        assertThatQueryFragment().isEqualTo("periodo_stop_l:[2100 TO 2120]");
    }
    
    @Test
    void getQueryPart_stopEarliestOnly() {
        this.searchField.setStopEarliest("2100");
        
        assertThatQueryFragment().isEqualTo("periodo_stop_l:[2100 TO *]");
    }
    
    @Test
    void getQueryPart_stopLatestOnly() {
        this.searchField.setStopLatest("2120");
        
        assertThatQueryFragment().isEqualTo("periodo_stop_l:[* TO 2120]");
    }
    
    @Test
    void getQueryPart_autorityTitleOnly() {
        this.searchField.setAuthorityTitle("title1");
        
        assertThatQueryFragment().isEqualTo("periodo_authority_title_txt:\"title1\"");
    }
    
    @Test
    void getQueryPart_all() {
        this.searchField.setId("123");
        this.searchField.setLabel("label1");
        this.searchField.setCoverageName("name1");
        this.searchField.setLocations(singletonList("location1"));
        this.searchField.setStartEarliest("2000");
        this.searchField.setStartLatest("2020");
        this.searchField.setStopEarliest("2100");
        this.searchField.setStopLatest("2120");
        this.searchField.setAuthorityTitle("title1");
        
        assertThatQueryFragment().isEqualTo("periodo_id_s:\"123\" AND " +
                "periodo_label_txt:\"label1\" AND " + 
                "periodo_coverage_name_txt:\"name1\" AND " + 
                "periodo_locations_ss:\"location1\" AND " + 
                "periodo_start_l:[2000 TO 2020] AND " +
                "periodo_stop_l:[2100 TO 2120] AND " +
                "periodo_authority_title_txt:\"title1\"");
    }
    
    @Test
    void getQueryPart_allExceptLocations() {
        this.searchField.setId("123");
        this.searchField.setLabel("label1");
        this.searchField.setCoverageName("name1");
        this.searchField.setStartEarliest("2000");
        this.searchField.setStartLatest("2020");
        this.searchField.setStopEarliest("2100");
        this.searchField.setStopLatest("2120");
        this.searchField.setAuthorityTitle("title1");
        
        assertThatQueryFragment().isEqualTo("periodo_id_s:\"123\" AND " +
                "periodo_label_txt:\"label1\" AND " + 
                "periodo_coverage_name_txt:\"name1\" AND " + 
                "periodo_start_l:[2000 TO 2020] AND " +
                "periodo_stop_l:[2100 TO 2120] AND " +
                "periodo_authority_title_txt:\"title1\"");
    }
    
    
    @Test
    void getQueryPart_allExceptEarliest() {
        this.searchField.setId("123");
        this.searchField.setLabel("label1");
        this.searchField.setCoverageName("name1");
        this.searchField.setLocations(asList("location1", "location2"));
        this.searchField.setStartLatest("2020");
        this.searchField.setStopLatest("2120");
        this.searchField.setAuthorityTitle("title1");
        
        assertThatQueryFragment().isEqualTo("periodo_id_s:\"123\" AND " +
                "periodo_label_txt:\"label1\" AND " + 
                "periodo_coverage_name_txt:\"name1\" AND " + 
                "periodo_locations_ss:\"location1\" OR " + 
                "periodo_locations_ss:\"location2\" AND " + 
                "periodo_start_l:[* TO 2020] AND " +
                "periodo_stop_l:[* TO 2120] AND " +
                "periodo_authority_title_txt:\"title1\"");
    }
    
    @Test
    void getQueryPart_allExceptLatest() {
        this.searchField.setId("123");
        this.searchField.setLabel("label1");
        this.searchField.setCoverageName("name1");
        this.searchField.setLocations(singletonList("location1"));
        this.searchField.setStartEarliest("2000");
        this.searchField.setStopEarliest("2100");
        this.searchField.setAuthorityTitle("title1");
        
        assertThatQueryFragment().isEqualTo("periodo_id_s:\"123\" AND " +
                "periodo_label_txt:\"label1\" AND " + 
                "periodo_coverage_name_txt:\"name1\" AND " + 
                "periodo_locations_ss:\"location1\" AND " + 
                "periodo_start_l:[2000 TO *] AND " +
                "periodo_stop_l:[2100 TO *] AND " +
                "periodo_authority_title_txt:\"title1\"");
    }
}
