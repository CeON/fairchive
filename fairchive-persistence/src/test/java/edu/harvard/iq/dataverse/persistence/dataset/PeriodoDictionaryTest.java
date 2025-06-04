package edu.harvard.iq.dataverse.persistence.dataset;

import static edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary.find;
import static edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary.getByUrl;
import static edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary.locations;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PeriodoDictionaryTest {

    private final static String EARLY_BRONZE_URL = "http://n2t.net/ark:/99152/p0f65r2qmh2";
    private final static String EARLY_BRONZE_ID = "p0f65r2qmh2";
    
    private final static String PERIOD_STARTING_AT_MINUS0400 = "p0qhb66fwwp";
    private final static String PERIOD_STARTING_AT_0000 = "p0qhb66dbwb";
    private final static String PERIOD_STARTING_AT_EARLIEST_YEAR = "p0323gxds6f";
    
    private final static String PERIOD_STOPPING_AT_MINUS0052 = "p0f65r2v4dw";
    private final static String PERIOD_STOPPING_AT_0000 = "p04h98q5wn6";
    private final static String PERIOD_STOPPING_AT_LATEST_YEAR = "p06m4n4g3xs";
    private final static String PERIOD_STOPPING_AT_PRESENT_TIMES = "p0fbfthrjgj";

    @Test
    void findingByEmptyString_returnsEmptyList() {
        assertThat(find("")).isEmpty();
        assertThat(find("  ")).isEmpty();
    }

    @Test
    void gettingByEmptyUrl_returnsEmptyOptional() {
        assertThat(getByUrl("")).isEmpty();
        assertThat(getByUrl("  ")).isEmpty();
    }

    @Test
    void gettingByImproperUrl_returnsEmptyOptional() {
        // totally wrong url
        assertThat(getByUrl("http://google.com/xyz")).isEmpty();
        // url with wrong prefix but proper period identifier
        assertThat(getByUrl("http://google.com/p0f65r2")).isEmpty();
        // url with proper prefix but missing period identifier
        assertThat(getByUrl("http://google.com/")).isEmpty();
    }

    @Test
    void findingByImproperUrl_returnsEmptyList() {
        // totally wrong url
        assertThat(find("http://google.com/x1z")).isEmpty();
        // url with proper prefix but missing period identifier
        assertThat(find("http://google.com/")).isEmpty();
    }
    
    @Test
    void findingByLabel_returnsRasults_andIsCaseInsensitive() {
        assertThat(find("Romania")).isNotEmpty();
        assertThat(find("romania")).isNotEmpty();
    }
    
    @Test
    void findingBySeparateWords_returnsPeriodsContainingAllWords() {
        assertThat(find("Early\tMinoan")).isNotEmpty();

        List<PeriodoDictionary.Period> periods = find("Early\tMinoan III Period");

        assertThat(periods).hasSize(2);
        assertThat(periods.stream().map(PeriodoDictionary.Period::getId))
                .containsExactly("p0mn2ndsr5z", "p0ds9qjvfn5");

        assertThat(find("Minoan")).isNotEmpty();
        assertThat(find("Minoan Aegean")).isNotEmpty();
        assertThat(find("Minoan Poland")).isEmpty();
    }

    @Test
    void findingByUrlAndID_returnsTheSameResult() {
        List<PeriodoDictionary.Period> result1 = find(EARLY_BRONZE_URL);
        List<PeriodoDictionary.Period> result2 = find(EARLY_BRONZE_ID);

        assertThat(result1.size()).isEqualTo(1);
        assertThat(result2.size()).isEqualTo(1);
        assertThat(result1.get(0)).isEqualTo(result2.get(0));
    }

    @Test
    void findingAndGettingByTheSameUrl_returnsTheSameResult() {
        assertThat(assertThat(find(EARLY_BRONZE_URL).get(0))
                .isEqualTo(getByUrl(EARLY_BRONZE_URL).get()));
    }

    @Test
    void findingByProperId_retrunsActualData() {

        String result = find(EARLY_BRONZE_ID).get(0).getDetails();

        assertThat(result.contains("Name: Early Bronze")).isTrue();
        assertThat(result.contains(
                "Bronze Area: Israel, Egypt, Jordan, Lebanon, Syria, Cyprus, Turkey"))
                .isTrue();
        assertThat(result.contains("Area (description): Levant")).isTrue();
        assertThat(result.contains("Start: -3499")).isTrue();
        assertThat(result.contains("End: -2249")).isTrue();
        assertThat(result.contains("Source: Life in biblical Israel")).isTrue();
    }

    @Test
    void startDates_areProperlyNormalized() {
        assertThat(find(PERIOD_STARTING_AT_MINUS0400).get(0).getDetails()
                .contains("Start: -400")).isTrue();
        assertThat(find(PERIOD_STARTING_AT_0000).get(0).getDetails()
                .contains("Start: 0")).isTrue();
        assertThat(find(PERIOD_STARTING_AT_EARLIEST_YEAR).get(0).getDetails()
                .contains("Start: -1500000")).isTrue();
    }
    
    @Test
    void stopDates_areProperlyNormalized() {     
        assertThat(find(PERIOD_STOPPING_AT_MINUS0052).get(0).getDetails()
                .contains("End: -52")).isTrue();
        assertThat(find(PERIOD_STOPPING_AT_0000).get(0).getDetails()
                .contains("End: 0")).isTrue();
        assertThat(find(PERIOD_STOPPING_AT_LATEST_YEAR).get(0).getDetails()
                .contains("End: 33")).isTrue();
    }
    
    @Test
    void stopDatesAtPresentTimes_areInernationalized() {     
        assertThat(find(PERIOD_STOPPING_AT_PRESENT_TIMES).get(0).getDetails()
                .contains("End: Present times")).isTrue();
    }
    
    @Test
    void locationsAreGatheredDuringStartup_andAlphabeticallySorted() {
        assertThat(locations()).isNotEmpty();
        assertThat(locations()).isSorted();
    }
}